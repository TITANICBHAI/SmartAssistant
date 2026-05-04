package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MultiObjectiveRL — multi-objective reward management and Pareto-front tracking.
 *
 * In complex game environments, agents must balance multiple objectives
 * (e.g., score, health, efficiency, safety).  This class provides:
 *
 *   1. Scalarization methods to combine objective vectors into a single reward:
 *        WEIGHTED_SUM   — r = w · R  (linear, configurable weights)
 *        CHEBYSHEV      — r = -max_i w_i·|R_i - utopia_i|  (non-convex Pareto)
 *        ACHIEVEMENT    — r = min_i (R_i - ref_i)/w_i      (balanced achievement)
 *
 *   2. Pareto dominance: tracks a set of non-dominated policy snapshots.
 *
 *   3. Adaptive weight scheduling: increase weight of lagging objectives.
 *
 *   4. Per-objective running statistics (mean, std, min, max) for normalisation.
 *
 * Thread-safe.
 */
public class MultiObjectiveRL {

    private static final String TAG = "MultiObjectiveRL";

    public enum Scalarization { WEIGHTED_SUM, CHEBYSHEV, ACHIEVEMENT }

    // ─────────────────────────────────────────────────────────────────────────
    // Pareto solution snapshot
    // ─────────────────────────────────────────────────────────────────────────
    public static class Solution {
        public final float[] objectives;
        public final int     step;
        Solution(float[] obj, int step) {
            this.objectives = obj.clone();
            this.step = step;
        }

        boolean dominates(Solution other) {
            boolean anyBetter = false;
            for (int i = 0; i < objectives.length; i++) {
                if (objectives[i] < other.objectives[i]) return false;
                if (objectives[i] > other.objectives[i]) anyBetter = true;
            }
            return anyBetter;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int            numObj;
    private final float[]        weights;          // scalarization weights
    private final float[]        utopia;           // reference point (max per obj)
    private final float[]        reference;        // reference point (min per obj)
    private Scalarization        method;

    // Running stats (Welford) per objective
    private final double[]       objMean, objM2;
    private long                 objCount = 0;

    // Pareto front
    private final List<Solution> paretoFront = new ArrayList<>();
    private final int            maxParetoSize;

    // Adaptive weight config
    private final boolean adaptWeights;
    private final float   adaptLr;          // how fast weights shift
    private final float[] objProgress;      // smoothed per-obj progress

    private final AtomicInteger stepCount = new AtomicInteger(0);
    private final Random        rng       = new Random(101L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public MultiObjectiveRL(int numObj, float[] weights, Scalarization method,
                             boolean adaptWeights, float adaptLr, int maxParetoSize) {
        this.numObj       = numObj;
        this.weights      = Arrays.copyOf(weights, numObj);
        this.method       = method;
        this.adaptWeights = adaptWeights;
        this.adaptLr      = adaptLr;
        this.maxParetoSize= maxParetoSize;
        this.utopia       = new float[numObj];
        this.reference    = new float[numObj];
        this.objMean      = new double[numObj];
        this.objM2        = new double[numObj];
        this.objProgress  = new float[numObj];
        Arrays.fill(utopia,    Float.NEGATIVE_INFINITY);
        Arrays.fill(reference, Float.POSITIVE_INFINITY);
        normaliseWeights();
        Log.i(TAG, "MultiObjectiveRL: obj=" + numObj + " method=" + method);
    }

    public MultiObjectiveRL(int numObj) {
        float[] w = new float[numObj];
        Arrays.fill(w, 1f / numObj);
        this(numObj, w, Scalarization.WEIGHTED_SUM, true, 0.01f, 100);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scalarize a vector of objective values into a single scalar reward.
     * Also updates running stats and attempts to add solution to Pareto front.
     */
    public synchronized float scalarize(float[] objectives) {
        if (objectives.length < numObj) objectives = Arrays.copyOf(objectives, numObj);
        updateStats(objectives);
        float[] norm = normalise(objectives);

        // Update utopia/reference
        for (int i = 0; i < numObj; i++) {
            if (objectives[i] > utopia[i])    utopia[i]    = objectives[i];
            if (objectives[i] < reference[i]) reference[i] = objectives[i];
        }

        float scalar;
        switch (method) {
            case CHEBYSHEV:   scalar = chebyshev(norm);   break;
            case ACHIEVEMENT: scalar = achievement(norm);  break;
            case WEIGHTED_SUM:
            default:          scalar = weightedSum(norm);  break;
        }

        // Update adaptive weights
        if (adaptWeights) adaptWeights(norm);

        // Pareto update (use raw objectives for dominance)
        maybeAddToPareto(objectives);
        stepCount.incrementAndGet();
        return scalar;
    }

    /** Get the current weight for each objective. */
    public synchronized float[] getWeights() { return weights.clone(); }

    /** Override scalarization method at runtime. */
    public synchronized void setMethod(Scalarization m) { this.method = m; }

    /** Set a specific objective weight (re-normalises). */
    public synchronized void setWeight(int objIdx, float w) {
        if (objIdx < 0 || objIdx >= numObj) return;
        weights[objIdx] = Math.max(0f, w);
        normaliseWeights();
    }

    /** Current Pareto front (non-dominated solutions). */
    public synchronized List<Solution> getParetoFront() { return new ArrayList<>(paretoFront); }

    // ─────────────────────────────────────────────────────────────────────────
    // Scalarization implementations
    // ─────────────────────────────────────────────────────────────────────────

    private float weightedSum(float[] norm) {
        float sum = 0f;
        for (int i = 0; i < numObj; i++) sum += weights[i] * norm[i];
        return sum;
    }

    private float chebyshev(float[] norm) {
        float max = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < numObj; i++) {
            float d = weights[i] * Math.abs(norm[i] - 1f); // 1 = utopia direction
            if (d > max) max = d;
        }
        return -max;  // maximise → minimise max deviation
    }

    private float achievement(float[] norm) {
        float min = Float.POSITIVE_INFINITY;
        for (int i = 0; i < numObj; i++) {
            float v = weights[i] > 0 ? (norm[i] - 0f) / weights[i] : Float.MAX_VALUE;
            if (v < min) min = v;
        }
        return min;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Adaptive weights
    // ─────────────────────────────────────────────────────────────────────────

    private void adaptWeights(float[] norm) {
        // Smooth progress per objective
        for (int i = 0; i < numObj; i++)
            objProgress[i] = (1f - adaptLr) * objProgress[i] + adaptLr * norm[i];
        // Increase weight for lagging objectives
        float minProgress = objProgress[0];
        for (float p : objProgress) if (p < minProgress) minProgress = p;
        for (int i = 0; i < numObj; i++) {
            float lag = Math.max(0f, 1f - objProgress[i]);
            weights[i] = Math.max(0.01f, weights[i] + adaptLr * 0.1f * lag);
        }
        normaliseWeights();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pareto front management
    // ─────────────────────────────────────────────────────────────────────────

    private void maybeAddToPareto(float[] objectives) {
        Solution candidate = new Solution(objectives, stepCount.get());
        // Check if dominated by any existing solution
        for (Solution sol : paretoFront) if (sol.dominates(candidate)) return;
        // Remove solutions dominated by candidate
        paretoFront.removeIf(candidate::dominates);
        paretoFront.add(candidate);
        // Trim if over capacity (remove oldest)
        if (paretoFront.size() > maxParetoSize)
            paretoFront.remove(0);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats & normalisation helpers
    // ─────────────────────────────────────────────────────────────────────────

    private float[] normalise(float[] obj) {
        float[] norm = new float[numObj];
        for (int i = 0; i < numObj; i++) {
            double std = objCount < 2 ? 1.0 : Math.sqrt(objM2[i] / (objCount - 1));
            norm[i] = std < 1e-8 ? 0f : (float)((obj[i] - objMean[i]) / std);
        }
        return norm;
    }

    private void updateStats(float[] obj) {
        objCount++;
        for (int i = 0; i < numObj; i++) {
            double delta = obj[i] - objMean[i];
            objMean[i] += delta / objCount;
            objM2[i]   += delta * (obj[i] - objMean[i]);
        }
    }

    private void normaliseWeights() {
        float sum = 0f; for (float w : weights) sum += w;
        if (sum > 0) for (int i = 0; i < numObj; i++) weights[i] /= sum;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("numObj",       numObj);
        s.put("method",       method.name());
        s.put("weights",      weights.clone());
        s.put("stepCount",    stepCount.get());
        s.put("paretoSize",   paretoFront.size());
        s.put("utopia",       utopia.clone());
        s.put("reference",    reference.clone());
        double[] means = new double[numObj];
        for (int i = 0; i < numObj; i++) means[i] = objMean[i];
        s.put("objMeans",     means);
        return s;
    }
}
