package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * EpisodeMemoryBuffer — full-episode storage for sequence-based RL methods.
 *
 * Unlike transition-level replay buffers, stores entire episodes as sequences.
 * Essential for:
 *   - LSTM / recurrent policy training (need contiguous sequences).
 *   - Monte Carlo return estimation (need complete returns).
 *   - HER (Hindsight Experience Replay) with FUTURE strategy.
 *   - Trajectory-level optimisation (TRPO, natural gradient).
 *   - Imitation learning from demonstration episodes.
 *
 * Storage model:
 *   - Each episode is a list of (s, a, r, s', done) transitions.
 *   - Episodes indexed by episode ID and length.
 *   - Supports sampling full episodes or fixed-length subsequences.
 *   - Maintains episodic return distribution for curriculum selection.
 *
 * Thread-safe.
 */
public class EpisodeMemoryBuffer {

    private static final String TAG = "EpisodeMemory";

    // ─────────────────────────────────────────────────────────────────────────
    // Episode record
    // ─────────────────────────────────────────────────────────────────────────
    public static class Episode {
        public final int             id;
        public final List<float[]>   states     = new ArrayList<>();
        public final List<Integer>   actions    = new ArrayList<>();
        public final List<Float>     rewards    = new ArrayList<>();
        public final List<float[]>   nextStates = new ArrayList<>();
        public final List<Boolean>   dones      = new ArrayList<>();
        public       float           totalReturn = 0f;
        public       int             length      = 0;
        public       float           priority    = 1f;

        public Episode(int id) { this.id = id; }

        public void addStep(float[] s, int a, float r, float[] sp, boolean done) {
            states.add(s); actions.add(a); rewards.add(r);
            nextStates.add(sp); dones.add(done);
            totalReturn += r; length++;
        }

        public ReplayBuffer.Experience getStep(int t) {
            return new ReplayBuffer.Experience(states.get(t), actions.get(t),
                    rewards.get(t), nextStates.get(t), dones.get(t));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int            capacity;      // max episodes
    private final int            maxEpLen;      // max steps per episode
    private final List<Episode>  episodes       = new ArrayList<>();
    private       Episode        currentEp      = null;
    private       int            nextId         = 0;
    private       int            writePtr       = 0;

    // Return statistics (for curriculum / sampling)
    private double returnMean = 0, returnM2 = 0;
    private long   returnN    = 0;
    private float  minReturn  = Float.MAX_VALUE, maxReturn = Float.MIN_VALUE;

    private final AtomicInteger addCount    = new AtomicInteger(0);
    private final AtomicInteger sampleCount = new AtomicInteger(0);
    private final AtomicInteger totalSteps  = new AtomicInteger(0);

    private final Random rng = new Random(409L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public EpisodeMemoryBuffer(int capacity, int maxEpLen) {
        this.capacity = capacity;
        this.maxEpLen = maxEpLen;
        Log.i(TAG, "EpisodeMemoryBuffer: cap=" + capacity + " maxLen=" + maxEpLen);
    }

    public EpisodeMemoryBuffer() { this(1000, 10000); }

    // ─────────────────────────────────────────────────────────────────────────
    // Episode building
    // ─────────────────────────────────────────────────────────────────────────

    /** Start a new episode. */
    public synchronized void beginEpisode() {
        currentEp = new Episode(nextId++);
    }

    /** Add a step to the current episode. */
    public synchronized void addStep(float[] state, int action, float reward,
                                      float[] nextState, boolean done) {
        if (currentEp == null) beginEpisode();
        if (currentEp.length < maxEpLen) {
            currentEp.addStep(state.clone(), action, reward, nextState.clone(), done);
            totalSteps.incrementAndGet();
        }
        if (done) endEpisode();
    }

    /** Finalise the current episode and add to buffer. */
    public synchronized Episode endEpisode() {
        if (currentEp == null || currentEp.length == 0) return null;
        Episode ep = currentEp;
        currentEp = null;

        // Compute discounted return (already accumulated as total)
        updateReturnStats(ep.totalReturn);
        ep.priority = returnPriority(ep.totalReturn);

        // Add to circular buffer
        if (episodes.size() < capacity) {
            episodes.add(ep);
        } else {
            episodes.set(writePtr, ep);
        }
        writePtr = (writePtr + 1) % capacity;
        addCount.incrementAndGet();
        return ep;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sampling
    // ─────────────────────────────────────────────────────────────────────────

    /** Sample a random episode. */
    public synchronized Episode sampleEpisode() {
        if (episodes.isEmpty()) return null;
        sampleCount.incrementAndGet();
        return episodes.get(rng.nextInt(episodes.size()));
    }

    /** Sample N episodes uniformly. */
    public synchronized List<Episode> sampleEpisodes(int n) {
        List<Episode> batch = new ArrayList<>(n);
        if (episodes.isEmpty()) return batch;
        for (int i=0;i<n;i++) batch.add(episodes.get(rng.nextInt(episodes.size())));
        sampleCount.addAndGet(n);
        return batch;
    }

    /**
     * Sample a contiguous subsequence of length seqLen from a random episode.
     * Returns list of transitions.
     */
    public synchronized List<ReplayBuffer.Experience> sampleSequence(int seqLen) {
        if (episodes.isEmpty()) return new ArrayList<>();
        Episode ep = episodes.get(rng.nextInt(episodes.size()));
        if (ep.length < seqLen) seqLen = ep.length;
        int start = rng.nextInt(Math.max(1, ep.length - seqLen + 1));
        List<ReplayBuffer.Experience> seq = new ArrayList<>(seqLen);
        for (int t=start;t<start+seqLen&&t<ep.length;t++) seq.add(ep.getStep(t));
        sampleCount.incrementAndGet();
        return seq;
    }

    /** Sample episode prioritised by return (high-return episodes more likely). */
    public synchronized Episode samplePrioritized() {
        if (episodes.isEmpty()) return null;
        float total=0; for(Episode e:episodes) total+=e.priority;
        float r=rng.nextFloat()*total, cum=0;
        for (Episode e:episodes) { cum+=e.priority; if(r<cum) { sampleCount.incrementAndGet(); return e; } }
        return episodes.get(episodes.size()-1);
    }

    /** Best episode by return (for imitation). */
    public synchronized Episode bestEpisode() {
        if (episodes.isEmpty()) return null;
        Episode best=episodes.get(0);
        for (Episode e:episodes) if(e.totalReturn>best.totalReturn) best=e;
        return best;
    }

    /** Sample diverse batch: K episodes spanning return distribution. */
    public synchronized List<Episode> sampleDiverse(int K) {
        if (episodes.isEmpty()) return new ArrayList<>();
        // Sort by return, pick K evenly spaced
        List<Episode> sorted = new ArrayList<>(episodes);
        sorted.sort((a,b)->Float.compare(a.totalReturn,b.totalReturn));
        List<Episode> diverse=new ArrayList<>(K);
        for(int k=0;k<K;k++){int idx=(int)(1.0*k/K*sorted.size());diverse.add(sorted.get(idx));}
        return diverse;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Monte Carlo returns
    // ─────────────────────────────────────────────────────────────────────────

    /** Compute discounted returns for all steps in an episode. */
    public synchronized float[] computeReturns(Episode ep, float gamma) {
        float[] G = new float[ep.length];
        float acc = 0;
        for (int t=ep.length-1;t>=0;t--) {
            acc = ep.rewards.get(t) + gamma * acc;
            G[t] = acc;
        }
        return G;
    }

    /** Compute GAE advantages for an episode using value function. */
    public synchronized float[] computeGAE(Episode ep, float gamma, float lambda,
                                            float[] values) {
        int T = ep.length;
        float[] adv = new float[T];
        float gae = 0;
        for (int t=T-1;t>=0;t--) {
            float nextV = (t+1<T&&!ep.dones.get(t)) ? (t+1<values.length?values[t+1]:0f) : 0f;
            float delta = ep.rewards.get(t) + gamma*nextV - (t<values.length?values[t]:0f);
            gae = delta + gamma*lambda*(ep.dones.get(t)?0f:gae);
            adv[t] = gae;
        }
        return adv;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void updateReturnStats(float ret) {
        returnN++;
        double d = ret - returnMean;
        returnMean += d / returnN;
        returnM2   += d * (ret - returnMean);
        if (ret < minReturn) minReturn = ret;
        if (ret > maxReturn) maxReturn = ret;
    }

    private float returnPriority(float ret) {
        // Prioritise episodes with above-average return
        double std = returnN<2?1.0:Math.sqrt(returnM2/(returnN-1)+1e-8);
        double z = (ret - returnMean) / std;
        return (float)Math.exp(Math.max(-3.0, Math.min(3.0, z)));
    }

    public synchronized int size()      { return episodes.size(); }
    public synchronized int stepCount() { return totalSteps.get(); }
    public synchronized boolean isEmpty(){ return episodes.isEmpty(); }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("capacity",    capacity);
        s.put("size",        episodes.size());
        s.put("addCount",    addCount.get());
        s.put("sampleCount", sampleCount.get());
        s.put("totalSteps",  totalSteps.get());
        s.put("returnMean",  returnMean);
        s.put("minReturn",   minReturn == Float.MAX_VALUE ? 0f : minReturn);
        s.put("maxReturn",   maxReturn == Float.MIN_VALUE ? 0f : maxReturn);
        double std=returnN<2?1.0:Math.sqrt(returnM2/(returnN-1)+1e-8);
        s.put("returnStd",   std);
        return s;
    }
}
