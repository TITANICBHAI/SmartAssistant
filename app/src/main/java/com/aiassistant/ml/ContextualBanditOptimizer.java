package com.aiassistant.ml;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ContextualBanditOptimizer — LinUCB and Thompson Sampling for contextual bandits.
 *
 * When full RL is too expensive or the episode length is 1 (single-shot decisions),
 * contextual bandits offer an efficient alternative:
 *
 *   Given context x ∈ R^d, choose arm a to maximise expected reward:
 *     r = θ_a^T · x + noise
 *
 * Algorithms:
 *
 *   LIN_UCB (Li et al. 2010):
 *     UCB score_a = θ_a^T·x + α·sqrt(x^T·A_a^{-1}·x)
 *     A_a updated: A_a += x·x^T;  b_a += r·x
 *     θ_a = A_a^{-1}·b_a
 *
 *   THOMPSON_SAMPLING:
 *     Sample θ̃_a ~ N(μ_a, σ_a²·A_a^{-1})
 *     Choose a = argmax θ̃_a^T·x
 *
 *   EPSILON_GREEDY:
 *     With prob ε choose random arm; else argmax θ_a^T·x
 *
 * Per-arm diagonal approximation A_a^{-1} (no matrix inversion needed).
 * Thread-safe.
 */
public class ContextualBanditOptimizer {

    private static final String TAG = "ContextualBandit";

    public enum Algorithm { LIN_UCB, THOMPSON_SAMPLING, EPSILON_GREEDY }

    // ─────────────────────────────────────────────────────────────────────────
    // Per-arm state
    // ─────────────────────────────────────────────────────────────────────────
    private static class Arm {
        final int id;
        float[]  theta;       // [contextDim] — estimated reward weight
        float[]  A_diag;      // diagonal of A_a (approx. A_a = diag)
        float[]  b;           // [contextDim] — reward-weighted context sum
        int      pulls;
        float    totalReward;
        float    avgReward;

        Arm(int id, int contextDim) {
            this.id    = id;
            theta      = new float[contextDim];
            A_diag     = new float[contextDim];
            b          = new float[contextDim];
            java.util.Arrays.fill(A_diag, 1f);  // A_a = I initially
        }

        float predict(float[] x) {
            float s = 0; for (int i = 0; i < Math.min(theta.length, x.length); i++) s += theta[i]*x[i];
            return s;
        }

        float ucbBonus(float[] x, float alpha) {
            float s = 0;
            for (int i = 0; i < Math.min(A_diag.length, x.length); i++)
                s += x[i]*x[i] / (A_diag[i] + 1e-8f);
            return alpha * (float) Math.sqrt(s);
        }

        float thompsonSample(float[] x, float sigmaScale, Random rng) {
            float s = 0;
            for (int i = 0; i < Math.min(theta.length, x.length); i++) {
                float std = sigmaScale / (float) Math.sqrt(A_diag[i] + 1e-8f);
                s += (theta[i] + std * (float) rng.nextGaussian()) * x[i];
            }
            return s;
        }

        void update(float[] x, float reward) {
            for (int i = 0; i < Math.min(A_diag.length, x.length); i++) {
                A_diag[i] += x[i] * x[i];
                b[i]      += reward * x[i];
                theta[i]   = b[i] / (A_diag[i] + 1e-8f);
            }
            pulls++;
            totalReward += reward;
            avgReward    = totalReward / pulls;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final Arm[]     arms;
    private final int       numArms;
    private final int       contextDim;
    private final Algorithm algo;
    private final float     alpha;        // LinUCB exploration param
    private final float     sigmaScale;   // Thompson sigma scale
    private       float     epsilon;      // ε-greedy

    private final AtomicInteger selectCount = new AtomicInteger(0);
    private float avgReward = 0f;
    private int   bestArm   = 0;

    private final Random rng;

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public ContextualBanditOptimizer(int numArms, int contextDim, Algorithm algo,
                                      float alpha, float sigmaScale, float epsilon, long seed) {
        this.numArms    = numArms;
        this.contextDim = contextDim;
        this.algo       = algo;
        this.alpha      = alpha;
        this.sigmaScale = sigmaScale;
        this.epsilon    = epsilon;
        this.rng        = new Random(seed);

        arms = new Arm[numArms];
        for (int a = 0; a < numArms; a++) arms[a] = new Arm(a, contextDim);

        Log.i(TAG, "ContextualBanditOptimizer: arms=" + numArms
                + " ctx=" + contextDim + " algo=" + algo);
    }

    public ContextualBanditOptimizer(int numArms, int contextDim) {
        this(numArms, contextDim, Algorithm.LIN_UCB, 1.0f, 1.0f, 0.1f, 199L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /** Select an arm given the current context. */
    public synchronized int selectArm(float[] context) {
        float[] ctx = pad(context, contextDim);
        int arm;

        switch (algo) {
            case THOMPSON_SAMPLING: arm = thompsonSelect(ctx); break;
            case EPSILON_GREEDY:    arm = epsilonGreedy(ctx);  break;
            case LIN_UCB:
            default:                arm = linUcbSelect(ctx);   break;
        }

        selectCount.incrementAndGet();
        return arm;
    }

    /** Update arm with observed reward. */
    public synchronized void update(int armIdx, float[] context, float reward) {
        if (armIdx < 0 || armIdx >= numArms) return;
        float[] ctx = pad(context, contextDim);
        arms[armIdx].update(ctx, reward);
        avgReward = 0.99f * avgReward + 0.01f * reward;

        // Track best arm by avgReward
        bestArm = 0;
        for (int a = 1; a < numArms; a++)
            if (arms[a].avgReward > arms[bestArm].avgReward) bestArm = a;
    }

    /** Predicted reward for arm in context. */
    public synchronized float predictReward(int armIdx, float[] context) {
        if (armIdx < 0 || armIdx >= numArms) return 0f;
        return arms[armIdx].predict(pad(context, contextDim));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Algorithms
    // ─────────────────────────────────────────────────────────────────────────

    private int linUcbSelect(float[] ctx) {
        float bestScore = Float.NEGATIVE_INFINITY; int best = 0;
        for (int a = 0; a < numArms; a++) {
            float score = arms[a].predict(ctx) + arms[a].ucbBonus(ctx, alpha);
            if (score > bestScore) { bestScore = score; best = a; }
        }
        return best;
    }

    private int thompsonSelect(float[] ctx) {
        float bestScore = Float.NEGATIVE_INFINITY; int best = 0;
        for (int a = 0; a < numArms; a++) {
            float score = arms[a].thompsonSample(ctx, sigmaScale, rng);
            if (score > bestScore) { bestScore = score; best = a; }
        }
        return best;
    }

    private int epsilonGreedy(float[] ctx) {
        if (rng.nextFloat() < epsilon) return rng.nextInt(numArms);
        float bestScore = Float.NEGATIVE_INFINITY; int best = 0;
        for (int a = 0; a < numArms; a++) {
            float score = arms[a].predict(ctx);
            if (score > bestScore) { bestScore = score; best = a; }
        }
        return best;
    }

    private static float[] pad(float[] x, int dim) {
        if (x.length == dim) return x;
        float[] p = new float[dim]; System.arraycopy(x, 0, p, 0, Math.min(x.length, dim));
        return p;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("numArms",     numArms);
        s.put("algo",        algo.name());
        s.put("selectCount", selectCount.get());
        s.put("avgReward",   avgReward);
        s.put("bestArm",     bestArm);
        int[] pulls = new int[numArms];
        double[] avgs = new double[numArms];
        for (int a = 0; a < numArms; a++) { pulls[a] = arms[a].pulls; avgs[a] = arms[a].avgReward; }
        s.put("armPulls", pulls);
        s.put("armAvgRewards", avgs);
        return s;
    }
}
