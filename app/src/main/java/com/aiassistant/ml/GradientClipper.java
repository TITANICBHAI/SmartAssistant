package com.aiassistant.ml;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * GradientClipper — prevents gradient explosion during RL training.
 *
 * Implements three clipping strategies:
 *
 *   GLOBAL_NORM   — clip all gradients so their global L2 norm ≤ maxNorm.
 *                   This is the standard method used in PyTorch clip_grad_norm_.
 *                   scale = min(1, maxNorm / global_norm)
 *
 *   PER_LAYER_NORM — independently clip each weight matrix by its own L2 norm.
 *
 *   VALUE_CLIP     — element-wise clip: g ← clip(g, −clipValue, +clipValue).
 *                   Fast but ignores inter-gradient relationships.
 *
 * Additional features:
 *   - Records how many times clipping was active (clip events) vs. pass-through.
 *   - Running statistics on gradient norms: mean, max, last.
 *   - Can be attached to any float[][] gradient array returned by the optimiser.
 *
 * Usage:
 *   GradientClipper gc = new GradientClipper(GradientClipper.Strategy.GLOBAL_NORM, 0.5f);
 *   gc.clip(gradients);   // modifies in place
 */
public class GradientClipper {

    private static final String TAG = "GradientClipper";

    public enum Strategy { GLOBAL_NORM, PER_LAYER_NORM, VALUE_CLIP }

    // ─────────────────────────────────────────────────────────────────────────
    // Config
    // ─────────────────────────────────────────────────────────────────────────
    private final Strategy strategy;
    private final float    threshold;    // maxNorm or clipValue

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────
    private final AtomicInteger clipEvents  = new AtomicInteger(0);
    private final AtomicInteger totalCalls  = new AtomicInteger(0);
    private final AtomicLong    sumNorm     = new AtomicLong(0);  // stored as float bits
    private volatile float      lastNorm    = 0f;
    private volatile float      maxObsNorm  = 0f;

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public GradientClipper(Strategy strategy, float threshold) {
        this.strategy  = strategy;
        this.threshold = threshold;
    }

    /** Convenience: global-norm clip with maxNorm = 0.5 (typical PPO default). */
    public GradientClipper() {
        this(Strategy.GLOBAL_NORM, 0.5f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API — single matrix
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Clip a single gradient matrix in place.
     *
     * @param grad Gradient matrix [rows][cols] — modified in place.
     * @return Effective L2 norm before clipping.
     */
    public float clip(float[][] grad) {
        if (grad == null || grad.length == 0) return 0f;
        totalCalls.incrementAndGet();
        switch (strategy) {
            case GLOBAL_NORM:     return clipGlobalNorm(new float[][][]{grad});
            case PER_LAYER_NORM:  return clipPerLayerNorm(grad);
            case VALUE_CLIP:      return clipValues(grad);
            default:              return 0f;
        }
    }

    /**
     * Clip a group of gradient matrices together (true global-norm clip).
     *
     * @param grads Array of gradient matrices — all modified in place.
     * @return Global L2 norm before clipping.
     */
    public float clipAll(float[][]... grads) {
        if (grads == null || grads.length == 0) return 0f;
        totalCalls.incrementAndGet();
        if (strategy == Strategy.VALUE_CLIP) {
            float maxNorm = 0f;
            for (float[][] g : grads) maxNorm = Math.max(maxNorm, clipValues(g));
            return maxNorm;
        }
        return clipGlobalNorm(grads);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Strategy implementations
    // ─────────────────────────────────────────────────────────────────────────

    private float clipGlobalNorm(float[][][] grads) {
        // Compute global L2 norm
        double sumSq = 0.0;
        for (float[][] g : grads)
            for (float[] row : g)
                for (float v : row) sumSq += v * v;

        float globalNorm = (float) Math.sqrt(sumSq);
        lastNorm  = globalNorm;
        if (globalNorm > maxObsNorm) maxObsNorm = globalNorm;
        sumNorm.set(Float.floatToIntBits(
                Float.intBitsToFloat((int) sumNorm.get()) * 0.99f + globalNorm * 0.01f));

        if (globalNorm > threshold) {
            float scale = threshold / (globalNorm + 1e-8f);
            for (float[][] g : grads)
                for (float[] row : g)
                    for (int j = 0; j < row.length; j++) row[j] *= scale;
            clipEvents.incrementAndGet();
        }
        return globalNorm;
    }

    private float clipPerLayerNorm(float[][] grad) {
        double sumSq = 0.0;
        for (float[] row : grad) for (float v : row) sumSq += v * v;
        float norm = (float) Math.sqrt(sumSq);
        lastNorm = norm;
        if (norm > maxObsNorm) maxObsNorm = norm;

        if (norm > threshold) {
            float scale = threshold / (norm + 1e-8f);
            for (float[] row : grad) for (int j = 0; j < row.length; j++) row[j] *= scale;
            clipEvents.incrementAndGet();
        }
        return norm;
    }

    private float clipValues(float[][] grad) {
        float maxAbs = 0f;
        for (float[] row : grad) {
            for (int j = 0; j < row.length; j++) {
                if (Math.abs(row[j]) > maxAbs) maxAbs = Math.abs(row[j]);
                row[j] = Math.max(-threshold, Math.min(threshold, row[j]));
            }
        }
        lastNorm = maxAbs;
        if (maxAbs > threshold) clipEvents.incrementAndGet();
        return maxAbs;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public float getLastNorm()    { return lastNorm; }
    public float getMaxObsNorm()  { return maxObsNorm; }
    public int   getClipEvents()  { return clipEvents.get(); }
    public int   getTotalCalls()  { return totalCalls.get(); }

    public float getClipRate() {
        int t = totalCalls.get();
        return t > 0 ? (float) clipEvents.get() / t : 0f;
    }

    public Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("strategy",   strategy.name());
        s.put("threshold",  threshold);
        s.put("clipEvents", clipEvents.get());
        s.put("totalCalls", totalCalls.get());
        s.put("clipRate",   getClipRate());
        s.put("lastNorm",   lastNorm);
        s.put("maxObsNorm", maxObsNorm);
        return s;
    }

    public void resetStats() {
        clipEvents.set(0);
        totalCalls.set(0);
        lastNorm   = 0f;
        maxObsNorm = 0f;
    }
}
