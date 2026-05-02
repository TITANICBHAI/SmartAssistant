package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * EpisodeMemory — stores complete episode trajectories for offline / batch RL.
 *
 * An "episode" is a sequence of (state, action, reward, nextState, done) tuples
 * collected during one complete game run from start to terminal state.
 *
 * Features:
 *   - Configurable max capacity (number of stored episodes, not transitions).
 *   - LRU eviction: when full, the oldest episode is replaced.
 *   - Episode-level metadata: total return, episode length, success flag.
 *   - Trajectory replay: callers can iterate a stored episode in order.
 *   - Monte Carlo return computation: discounted G_t = Σ γ^k r_{t+k}.
 *   - GAE advantage computation: A_t = Σ (γλ)^k δ_{t+k}  (for PPO / actor-critic).
 *   - Batch sampling: randomly sample N complete episodes (for offline policy learning).
 *   - Filtering: retrieve only episodes with return above a threshold (curriculum).
 *
 * Thread-safe via synchronized methods.
 */
public class EpisodeMemory {

    private static final String TAG = "EpisodeMemory";

    // -------------------------------------------------------------------------
    // Transition within an episode
    // -------------------------------------------------------------------------
    public static class Transition {
        public final float[] state;
        public final int     action;
        public final float   reward;
        public final float[] nextState;
        public final boolean done;
        public float         advantage;    // computed by computeGAE()
        public float         returnValue;  // computed by computeReturns()

        public Transition(float[] state, int action, float reward,
                          float[] nextState, boolean done) {
            this.state     = state.clone();
            this.action    = action;
            this.reward    = reward;
            this.nextState = nextState.clone();
            this.done      = done;
        }
    }

    // -------------------------------------------------------------------------
    // Episode record
    // -------------------------------------------------------------------------
    public static class Episode {
        public final int             id;
        public final List<Transition> transitions = new ArrayList<>();
        public float                 totalReturn;
        public boolean               success;     // caller-set flag
        public final long            startMs;
        public long                  endMs;

        Episode(int id) {
            this.id      = id;
            this.startMs = System.currentTimeMillis();
        }

        public int    length()    { return transitions.size(); }
        public boolean isComplete() { return endMs > 0; }

        /**
         * Compute discounted Monte Carlo returns G_t = r_t + γ·r_{t+1} + γ²·r_{t+2} + …
         */
        public void computeReturns(float gamma) {
            float G = 0f;
            for (int t = transitions.size() - 1; t >= 0; t--) {
                G = transitions.get(t).reward + gamma * G;
                transitions.get(t).returnValue = G;
            }
        }

        /**
         * Compute GAE advantages: A_t = Σ_{l=0}^{T} (γλ)^l δ_{t+l}
         * where δ_t = r_t + γ·V(s_{t+1}) − V(s_t).
         *
         * @param values   Per-step V(s) estimates, length = transitions.size().
         * @param gamma    Discount factor.
         * @param lambda   GAE λ (0 = TD, 1 = MC).
         */
        public void computeGAE(float[] values, float gamma, float lambda) {
            int T = transitions.size();
            float lastGAE = 0f;
            for (int t = T - 1; t >= 0; t--) {
                float nextV = (t < T - 1) ? values[t + 1] : 0f;
                float delta  = transitions.get(t).reward + gamma * nextV - values[t];
                lastGAE = delta + gamma * lambda * lastGAE;
                transitions.get(t).advantage = lastGAE;
                transitions.get(t).returnValue = lastGAE + values[t]; // V + A = Q
            }
        }
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------
    private final int          maxEpisodes;
    private final List<Episode> episodes   = new ArrayList<>();
    private int                episodeIdCounter = 0;
    private Episode             currentEpisode  = null;

    // Stats
    private final AtomicInteger totalEpisodesCreated = new AtomicInteger(0);
    private final AtomicInteger totalTransitions     = new AtomicInteger(0);
    private float               bestReturn           = Float.NEGATIVE_INFINITY;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    public EpisodeMemory(int maxEpisodes) {
        this.maxEpisodes = Math.max(1, maxEpisodes);
    }

    public EpisodeMemory() {
        this(200);
    }

    // -------------------------------------------------------------------------
    // Episode lifecycle
    // -------------------------------------------------------------------------

    /**
     * Start a new episode.  Any previously open episode is automatically closed.
     */
    public synchronized void beginEpisode() {
        if (currentEpisode != null && !currentEpisode.isComplete()) {
            endEpisode(false);
        }
        currentEpisode = new Episode(episodeIdCounter++);
        totalEpisodesCreated.incrementAndGet();
    }

    /**
     * Add a transition to the current open episode.
     */
    public synchronized void record(float[] state, int action, float reward,
                                     float[] nextState, boolean done) {
        if (currentEpisode == null) beginEpisode();
        currentEpisode.transitions.add(new Transition(state, action, reward, nextState, done));
        totalTransitions.incrementAndGet();
        if (done) endEpisode(false);
    }

    /**
     * Close the current episode.
     *
     * @param success Whether the agent succeeded in the task.
     */
    public synchronized void endEpisode(boolean success) {
        if (currentEpisode == null) return;
        currentEpisode.endMs      = System.currentTimeMillis();
        currentEpisode.success    = success;
        currentEpisode.totalReturn = sumRewards(currentEpisode);

        if (currentEpisode.totalReturn > bestReturn) bestReturn = currentEpisode.totalReturn;

        // Evict oldest episode if at capacity
        if (episodes.size() >= maxEpisodes) episodes.remove(0);
        episodes.add(currentEpisode);
        Log.d(TAG, "Episode " + currentEpisode.id
                + " ended: len=" + currentEpisode.length()
                + " return=" + String.format("%.2f", currentEpisode.totalReturn)
                + " success=" + success);
        currentEpisode = null;
    }

    // -------------------------------------------------------------------------
    // Sampling
    // -------------------------------------------------------------------------

    /**
     * Randomly sample up to {@code n} complete episodes.
     */
    public synchronized List<Episode> sampleEpisodes(int n) {
        if (episodes.isEmpty()) return Collections.emptyList();
        List<Episode> pool = new ArrayList<>(episodes);
        Collections.shuffle(pool);
        return pool.subList(0, Math.min(n, pool.size()));
    }

    /**
     * Sample all transitions from all stored episodes as a flat list.
     */
    public synchronized List<Transition> getAllTransitions() {
        List<Transition> all = new ArrayList<>();
        for (Episode ep : episodes) all.addAll(ep.transitions);
        return all;
    }

    /**
     * Return only episodes whose total return exceeds a threshold (curriculum replay).
     */
    public synchronized List<Episode> getTopEpisodes(float minReturn) {
        List<Episode> result = new ArrayList<>();
        for (Episode ep : episodes) if (ep.totalReturn >= minReturn) result.add(ep);
        result.sort((a, b) -> Float.compare(b.totalReturn, a.totalReturn));
        return result;
    }

    /**
     * Return the top-K episodes by return.
     */
    public synchronized List<Episode> getTopK(int k) {
        List<Episode> sorted = new ArrayList<>(episodes);
        sorted.sort((a, b) -> Float.compare(b.totalReturn, a.totalReturn));
        return sorted.subList(0, Math.min(k, sorted.size()));
    }

    // -------------------------------------------------------------------------
    // Returns & advantages
    // -------------------------------------------------------------------------

    /**
     * Compute Monte Carlo returns for all stored episodes.
     */
    public synchronized void computeAllReturns(float gamma) {
        for (Episode ep : episodes) ep.computeReturns(gamma);
    }

    // -------------------------------------------------------------------------
    // Monitoring
    // -------------------------------------------------------------------------

    public synchronized int size() { return episodes.size(); }

    public synchronized boolean hasActiveEpisode() { return currentEpisode != null; }

    public Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("storedEpisodes",       episodes.size());
        s.put("totalEpisodesCreated", totalEpisodesCreated.get());
        s.put("totalTransitions",     totalTransitions.get());
        s.put("bestReturn",           bestReturn);
        s.put("hasActiveEpisode",     currentEpisode != null);
        if (!episodes.isEmpty()) {
            float sumR = 0;
            int successes = 0;
            for (Episode ep : episodes) {
                sumR += ep.totalReturn;
                if (ep.success) successes++;
            }
            s.put("avgReturn",    sumR / episodes.size());
            s.put("successRate",  (float) successes / episodes.size());
        }
        return s;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static float sumRewards(Episode ep) {
        float sum = 0f;
        for (Transition t : ep.transitions) sum += t.reward;
        return sum;
    }
}
