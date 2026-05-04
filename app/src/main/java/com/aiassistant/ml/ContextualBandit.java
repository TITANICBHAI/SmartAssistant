package com.aiassistant.ml;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ContextualBandit — Thompson-sampling contextual bandit for online action
 * selection when full RL is too expensive or the episode is very short.
 *
 * Model: For each arm a, maintain a linear reward model:
 *   E[r | context, a] = context · θ_a
 * with a diagonal Bayesian posterior (LinUCB / LinThompson style):
 *   θ_a | data ~ N(μ_a, Σ_a)
 *
 * Thompson Sampling: sample θ̃_a ~ N(μ_a, v²·Σ_a), select argmax context·θ̃_a.
 *
 * LinUCB:  score_a = context·μ_a + α·sqrt(context^T·Σ_a·context)
 *
 * Features:
 *   - Online posterior update: O(d²) per step with Sherman-Morrison formula.
 *   - Both Thompson Sampling and LinUCB selection modes.
 *   - Context normalization via running mean/std.
 *   - Per-arm pull counts and empirical mean rewards.
 *   - Warm-start: force each arm to be pulled at least warmupPulls times.
 *   - Thread-safe.
 */
public class ContextualBandit {

    private static final String TAG = "ContextualBandit";

    public enum Mode { THOMPSON_SAMPLING, LIN_UCB }

    // ─────────────────────────────────────────────────────────────────────────
    // Per-arm Bayesian linear model
    // ─────────────────────────────────────────────────────────────────────────
    private static class ArmModel {
        final int d;
        final float[] mu;       // posterior mean [d]
        final float[][] A;      // precision matrix A = I + X^T X  [d][d]
        final float[] b;        // sufficient statistic b = X^T r   [d]
        int    pulls;
        double totalReward;

        ArmModel(int d, float lambda) {
            this.d  = d;
            this.mu = new float[d];
            this.b  = new float[d];
            // A = λ·I
            this.A  = new float[d][d];
            for (int i = 0; i < d; i++) A[i][i] = lambda;
        }

        /** Sherman–Morrison rank-1 update of A (online ridge regression). */
        void update(float[] ctx, float reward) {
            // A ← A + ctx·ctx^T
            for (int i = 0; i < d; i++)
                for (int j = 0; j < d; j++)
                    A[i][j] += ctx[i] * ctx[j];
            // b ← b + ctx·reward
            for (int i = 0; i < d; i++) b[i] += ctx[i] * reward;
            // mu = A^{-1} b  (cheap Cholesky or just use Jacobi since d is small)
            solveJacobi(A, b, mu, 10);
            pulls++;
            totalReward += reward;
        }

        double mean() { return pulls > 0 ? totalReward / pulls : 0; }

        // Jacobi iterations for A·mu = b (works when A is well-conditioned diag-dominant)
        static void solveJacobi(float[][] A, float[] b, float[] x, int iters) {
            int n = b.length;
            float[] xNew = new float[n];
            for (int it = 0; it < iters; it++) {
                for (int i = 0; i < n; i++) {
                    float sum = b[i];
                    for (int j = 0; j < n; j++) if (j != i) sum -= A[i][j] * x[j];
                    xNew[i] = A[i][i] != 0 ? sum / A[i][i] : 0f;
                }
                System.arraycopy(xNew, 0, x, 0, n);
            }
        }

        // σ²(ctx) = ctx^T A^{-1} ctx (approximated as ||ctx||² / diag(A))
        float uncertainty(float[] ctx) {
            float sum = 0f;
            for (int i = 0; i < d; i++) {
                float diag = A[i][i];
                if (diag > 0) sum += ctx[i] * ctx[i] / diag;
            }
            return sum;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int        numArms;
    private final int        contextDim;
    private final Mode       mode;
    private final float      alpha;        // LinUCB exploration width
    private final float      v;            // Thompson sampling noise scale
    private final int        warmupPulls;
    private final ArmModel[] arms;

    // Context normalisation (Welford)
    private final double[] ctxMean, ctxM2;
    private long ctxCount = 0;

    private final AtomicInteger totalPulls   = new AtomicInteger(0);
    private final AtomicInteger warmupRemain;
    private final Random        rng          = new Random(43L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public ContextualBandit(int numArms, int contextDim, Mode mode,
                             float alpha, float lambda, float v, int warmupPulls) {
        this.numArms     = numArms;
        this.contextDim  = contextDim;
        this.mode        = mode;
        this.alpha       = alpha;
        this.v           = v;
        this.warmupPulls = warmupPulls;
        this.warmupRemain= new AtomicInteger(numArms * warmupPulls);

        arms    = new ArmModel[numArms];
        for (int a = 0; a < numArms; a++) arms[a] = new ArmModel(contextDim, lambda);
        ctxMean = new double[contextDim];
        ctxM2   = new double[contextDim];

        Log.i(TAG, "ContextualBandit: arms=" + numArms + " ctx=" + contextDim + " mode=" + mode);
    }

    public ContextualBandit(int numArms, int contextDim) {
        this(numArms, contextDim, Mode.THOMPSON_SAMPLING, 1.0f, 1.0f, 0.5f, 2);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /** Select an arm given the current context vector. */
    public synchronized int selectArm(float[] context) {
        float[] normCtx = normalise(context);

        // Warmup phase: round-robin
        if (warmupRemain.get() > 0) {
            int arm = totalPulls.get() % numArms;
            totalPulls.incrementAndGet();
            warmupRemain.decrementAndGet();
            return arm;
        }

        switch (mode) {
            case LIN_UCB:          return selectLinUCB(normCtx);
            case THOMPSON_SAMPLING:
            default:               return selectThompson(normCtx);
        }
    }

    /** Update the selected arm's posterior with the observed reward. */
    public synchronized void update(int arm, float[] context, float reward) {
        if (arm < 0 || arm >= numArms) return;
        float[] normCtx = normalise(context);
        arms[arm].update(normCtx, reward);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Selection implementations
    // ─────────────────────────────────────────────────────────────────────────

    private int selectThompson(float[] ctx) {
        float bestScore = Float.NEGATIVE_INFINITY;
        int   bestArm   = 0;
        for (int a = 0; a < numArms; a++) {
            float score = 0f;
            // θ̃ ~ N(μ, v²·A^{-1}): approximate as μ + v·noise/sqrt(diag(A))
            for (int i = 0; i < contextDim; i++) {
                float diag = arms[a].A[i][i];
                float std  = diag > 0 ? v / (float) Math.sqrt(diag) : v;
                score += ctx[i] * (arms[a].mu[i] + std * (float) rng.nextGaussian());
            }
            if (score > bestScore) { bestScore = score; bestArm = a; }
        }
        totalPulls.incrementAndGet();
        return bestArm;
    }

    private int selectLinUCB(float[] ctx) {
        float bestScore = Float.NEGATIVE_INFINITY;
        int   bestArm   = 0;
        for (int a = 0; a < numArms; a++) {
            float mean = dot(arms[a].mu, ctx);
            float ucb  = alpha * (float) Math.sqrt(arms[a].uncertainty(ctx));
            float score= mean + ucb;
            if (score > bestScore) { bestScore = score; bestArm = a; }
        }
        totalPulls.incrementAndGet();
        return bestArm;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("numArms",     numArms);
        s.put("contextDim",  contextDim);
        s.put("mode",        mode.name());
        s.put("totalPulls",  totalPulls.get());
        s.put("warmupLeft",  warmupRemain.get());
        double[] means = new double[numArms];
        int[] pulls = new int[numArms];
        for (int a = 0; a < numArms; a++) {
            means[a] = arms[a].mean();
            pulls[a] = arms[a].pulls;
        }
        s.put("armMeans", means);
        s.put("armPulls", pulls);
        return s;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private float[] normalise(float[] ctx) {
        ctxCount++;
        float[] norm = new float[contextDim];
        int d = Math.min(ctx.length, contextDim);
        for (int i = 0; i < d; i++) {
            double delta = ctx[i] - ctxMean[i];
            ctxMean[i] += delta / ctxCount;
            ctxM2[i]   += delta * (ctx[i] - ctxMean[i]);
            double std  = ctxCount < 2 ? 1.0 : Math.sqrt(ctxM2[i] / (ctxCount - 1));
            norm[i] = std < 1e-8 ? 0f : (float)((ctx[i] - ctxMean[i]) / std);
        }
        return norm;
    }

    private static float dot(float[] a, float[] b) {
        float s = 0f;
        for (int i = 0; i < Math.min(a.length, b.length); i++) s += a[i] * b[i];
        return s;
    }
}
