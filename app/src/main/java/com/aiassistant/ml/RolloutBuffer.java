package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;

/**
 * RolloutBuffer — on-policy trajectory buffer for PPO / A2C.
 *
 * Collects a fixed number of environment steps, then computes:
 *   • Monte-Carlo returns:   G_t = r_t + γ·r_{t+1} + … + γ^{T-t}·V(s_T)
 *   • GAE-λ advantages:      A_t = Σ_{l≥0} (γλ)^l δ_{t+l}   δ_t = r_t+γV(s')-V(s)
 *   • Log-probabilities stored alongside each transition for PPO ratio computation
 *
 * After {@link #isFull()} returns true, call {@link #computeReturnsAndAdvantages}
 * before sampling mini-batches with {@link #sampleMiniBatch}.
 *
 * Supports advantage normalisation (zero-mean, unit-variance) per batch.
 */
public class RolloutBuffer {

    private static final String TAG = "RolloutBuffer";

    // ─────────────────────────────────────────────────────────────────────────
    // Step record
    // ─────────────────────────────────────────────────────────────────────────
    public static class Step {
        public final float[] state;
        public final int     action;
        public final float   reward;
        public final float[] nextState;
        public final boolean done;
        public final float   logProb;   // log π_old(a|s)
        public final float   value;     // V(s) at collection time
        public float         advantage; // filled by computeReturnsAndAdvantages()
        public float         returnVal; // G_t

        public Step(float[] state, int action, float reward,
                    float[] nextState, boolean done, float logProb, float value) {
            this.state     = state.clone();
            this.action    = action;
            this.reward    = reward;
            this.nextState = nextState.clone();
            this.done      = done;
            this.logProb   = logProb;
            this.value     = value;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int     capacity;
    private final float   gamma;
    private final float   gaeLambda;
    private final boolean normaliseAdvantages;

    private final List<Step> steps  = new ArrayList<>();
    private boolean          ready  = false;   // returns/advantages computed
    private final Random     rng    = new Random(71L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public RolloutBuffer(int capacity, float gamma, float gaeLambda,
                         boolean normaliseAdvantages) {
        this.capacity            = capacity;
        this.gamma               = gamma;
        this.gaeLambda           = gaeLambda;
        this.normaliseAdvantages = normaliseAdvantages;
    }

    public RolloutBuffer(int capacity) {
        this(capacity, 0.99f, 0.95f, true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Collection
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Add one environment step.
     *
     * @param state     s_t
     * @param action    a_t
     * @param reward    r_t
     * @param nextState s_{t+1}
     * @param done      terminal flag
     * @param logProb   log π_old(a_t | s_t)
     * @param value     V(s_t) from the value network
     */
    public synchronized void add(float[] state, int action, float reward,
                                  float[] nextState, boolean done,
                                  float logProb, float value) {
        if (isFull()) { Log.w(TAG, "Buffer full — call reset() before adding."); return; }
        steps.add(new Step(state, action, reward, nextState, done, logProb, value));
        ready = false;
    }

    public synchronized boolean isFull()  { return steps.size() >= capacity; }
    public synchronized boolean isEmpty() { return steps.isEmpty(); }
    public synchronized int     size()    { return steps.size(); }

    // ─────────────────────────────────────────────────────────────────────────
    // Return & advantage computation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Compute GAE advantages and discounted returns.
     * Must be called before sampling.
     *
     * @param lastValue V(s_{T+1}) bootstrap value (0 if terminal).
     */
    public synchronized void computeReturnsAndAdvantages(float lastValue) {
        int T = steps.size();
        if (T == 0) return;

        float gae = 0f;
        for (int t = T - 1; t >= 0; t--) {
            Step s   = steps.get(t);
            float nextV = (t < T - 1) ? steps.get(t + 1).value : lastValue;
            float delta = s.reward + (s.done ? 0f : gamma * nextV) - s.value;
            gae         = delta + (s.done ? 0f : gamma * gaeLambda * gae);
            s.advantage = gae;
            s.returnVal = gae + s.value;
        }

        if (normaliseAdvantages) {
            double mean = 0, var = 0;
            for (Step s : steps) mean += s.advantage;
            mean /= T;
            for (Step s : steps) { double d = s.advantage - mean; var += d * d; }
            var /= T;
            float std = (float) Math.sqrt(var + 1e-8);
            for (Step s : steps) s.advantage = (float)((s.advantage - mean) / std);
        }

        ready = true;
        Log.d(TAG, "Returns/advantages computed for " + T + " steps.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sampling
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Randomly sample a mini-batch.
     * @throws IllegalStateException if {@link #computeReturnsAndAdvantages} hasn't been called.
     */
    public synchronized List<Step> sampleMiniBatch(int batchSize) {
        if (!ready) throw new IllegalStateException("Call computeReturnsAndAdvantages() first.");
        List<Step> shuffled = new ArrayList<>(steps);
        java.util.Collections.shuffle(shuffled, rng);
        return shuffled.subList(0, Math.min(batchSize, shuffled.size()));
    }

    /** Return all steps in order. */
    public synchronized List<Step> getAllSteps() { return new ArrayList<>(steps); }

    /** Clear buffer for reuse. */
    public synchronized void reset() { steps.clear(); ready = false; }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("capacity",  capacity);
        s.put("size",      steps.size());
        s.put("isFull",    isFull());
        s.put("ready",     ready);
        if (!steps.isEmpty() && ready) {
            float sumR = 0, sumA = 0;
            for (Step st : steps) { sumR += st.returnVal; sumA += st.advantage; }
            s.put("avgReturn",    sumR / steps.size());
            s.put("avgAdvantage", sumA / steps.size());
        }
        return s;
    }
}
