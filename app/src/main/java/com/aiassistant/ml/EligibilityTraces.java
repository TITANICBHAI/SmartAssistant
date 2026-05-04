package com.aiassistant.ml;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * EligibilityTraces — TD(λ) eligibility trace algorithms for accelerated RL.
 *
 * Eligibility traces bridge one-step TD and Monte-Carlo returns, allowing credit
 * assignment to spread backwards in time.
 *
 * Supported algorithms:
 *
 *   ACCUMULATING  — e(s) ← γλe(s); e(s_t) ← e(s_t) + 1   (classic Sutton & Barto)
 *   REPLACING     — e(s) ← γλe(s); e(s_t) ← 1             (avoids runaway traces)
 *   DUTCH         — e(s) ← (1-α)γλe(s); e(s_t) ← e(s_t)+1 (prevents double update)
 *   TRUE_ONLINE   — TD(λ) with Seijen & Sutton 2014 correction
 *
 * Works with a hash-based linear function approximator (tile coding / feature hash).
 * Each feature key stores a scalar trace and weight.
 *
 * Usage with Sarsa(λ):
 *   traces.beginEpisode();
 *   // At each step:
 *   float delta = r + γ·Q(s',a') - Q(s,a)
 *   traces.update(featureKeys(s,a), delta, lr);
 *   float q = traces.value(featureKeys(s,a));
 *
 * Thread-safe.
 */
public class EligibilityTraces {

    private static final String TAG = "EligibilityTraces";

    public enum Mode { ACCUMULATING, REPLACING, DUTCH, TRUE_ONLINE }

    // ─────────────────────────────────────────────────────────────────────────
    // Per-feature entry
    // ─────────────────────────────────────────────────────────────────────────
    private static class Feature {
        float weight = 0f;
        float trace  = 0f;
        float oldVal = 0f;  // for TRUE_ONLINE
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final HashMap<String, Feature> features = new HashMap<>();
    private final Mode   mode;
    private final float  lambda;
    private final float  gamma;
    private final float  alpha;    // learning rate (used for DUTCH)

    private int   episodeStep = 0;
    private float lastOldQ    = 0f; // for TRUE_ONLINE

    private final AtomicInteger episodeCount = new AtomicInteger(0);
    private final AtomicInteger stepCount    = new AtomicInteger(0);
    private float avgDelta = 0f;

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public EligibilityTraces(Mode mode, float lambda, float gamma, float alpha) {
        this.mode   = mode;
        this.lambda = lambda;
        this.gamma  = gamma;
        this.alpha  = alpha;
        Log.i(TAG, "EligibilityTraces mode=" + mode + " λ=" + lambda + " γ=" + gamma);
    }

    public EligibilityTraces(float lambda, float gamma) {
        this(Mode.REPLACING, lambda, gamma, 0.1f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Episode management
    // ─────────────────────────────────────────────────────────────────────────

    /** Must be called at the start of each episode to zero all traces. */
    public synchronized void beginEpisode() {
        for (Feature f : features.values()) { f.trace = 0f; f.oldVal = 0f; }
        episodeStep = 0;
        lastOldQ    = 0f;
        episodeCount.incrementAndGet();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Step update: call once per (s,a,r,s',a') step
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * TD(λ) update: backpropagates delta through all active traces.
     *
     * @param activeKeys  Feature keys for the current (s, a) pair.
     * @param delta       TD error: r + γ·V(s') - V(s)
     * @param lr          Learning rate override (pass -1 to use stored alpha).
     */
    public synchronized void update(String[] activeKeys, float delta, float lr) {
        if (lr < 0) lr = alpha;
        stepCount.incrementAndGet();
        episodeStep++;
        avgDelta = 0.99f * avgDelta + 0.01f * Math.abs(delta);

        // Decay all existing traces
        float decay = gamma * lambda;
        for (Feature f : features.values()) {
            if (mode == Mode.DUTCH)
                f.trace *= (1f - lr) * decay;
            else
                f.trace *= decay;
        }

        // Update traces for active features
        for (String key : activeKeys) {
            Feature f = features.computeIfAbsent(key, k -> new Feature());
            switch (mode) {
                case REPLACING:    f.trace  = 1f;           break;
                case ACCUMULATING: f.trace += 1f;           break;
                case DUTCH:        f.trace += 1f;           break;
                case TRUE_ONLINE:
                    float oldQ = f.weight;
                    f.trace += 1f - lr * f.trace; // Dutch-style with correction
                    f.oldVal  = oldQ;
                    break;
            }
        }

        // Apply weight updates proportional to trace
        for (Map.Entry<String, Feature> entry : features.entrySet()) {
            Feature f = entry.getValue();
            if (Math.abs(f.trace) < 1e-8f) continue;
            if (mode == Mode.TRUE_ONLINE) {
                f.weight += lr * (delta + f.weight - f.oldVal) * f.trace - lr * (f.weight - f.oldVal);
            } else {
                f.weight += lr * delta * f.trace;
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Value query
    // ─────────────────────────────────────────────────────────────────────────

    /** Estimate Q(s,a) or V(s) as sum of active feature weights. */
    public synchronized float value(String[] activeKeys) {
        float sum = 0f;
        for (String key : activeKeys) {
            Feature f = features.get(key);
            if (f != null) sum += f.weight;
        }
        return sum;
    }

    /** Get trace strength for a feature key. */
    public synchronized float trace(String key) {
        Feature f = features.get(key);
        return f != null ? f.trace : 0f;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Convenience: hash-based feature generation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generate feature keys from a continuous state vector via tile coding hash.
     * Creates numTilings offset hashes, each quantizing state into numTiles bins.
     */
    public static String[] tileKeys(float[] state, int action,
                                     int numTilings, int numTiles) {
        String[] keys = new String[numTilings];
        for (int tiling = 0; tiling < numTilings; tiling++) {
            StringBuilder sb = new StringBuilder("t").append(tiling).append("a").append(action);
            for (float v : state) {
                int bin = (int)((v + tiling * 0.1f) * numTiles);
                sb.append('_').append(bin);
            }
            keys[tiling] = sb.toString();
        }
        return keys;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("mode",         mode.name());
        s.put("lambda",       lambda);
        s.put("gamma",        gamma);
        s.put("featureCount", features.size());
        s.put("episodeCount", episodeCount.get());
        s.put("stepCount",    stepCount.get());
        s.put("avgDelta",     avgDelta);
        s.put("episodeStep",  episodeStep);
        float maxTrace = 0f;
        for (Feature f : features.values()) if (f.trace > maxTrace) maxTrace = f.trace;
        s.put("maxTrace",     maxTrace);
        return s;
    }

    /** Remove features with negligible weights to control memory. */
    public synchronized void prune(float threshold) {
        features.entrySet().removeIf(e -> Math.abs(e.getValue().weight) < threshold
                && Math.abs(e.getValue().trace) < threshold);
    }
}
