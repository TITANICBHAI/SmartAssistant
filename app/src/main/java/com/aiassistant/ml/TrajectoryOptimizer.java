package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TrajectoryOptimizer — cross-entropy method (CEM) and model-based trajectory search.
 *
 * Used in conjunction with a learned world model (TransitionModel) to plan optimal
 * action sequences WITHOUT further environment interaction (planning at inference time).
 *
 * Two optimization backends:
 *
 *   CROSS_ENTROPY_METHOD (CEM):
 *     1. Sample N candidate action sequences from N(μ, σ²) distribution.
 *     2. Simulate each sequence through the world model.
 *     3. Keep top-K trajectories (elite set).
 *     4. Update μ, σ from elite set.
 *     5. Repeat for nIter iterations.
 *     6. Execute first action of best trajectory (MPC: receding horizon).
 *
 *   RANDOM_SHOOTING (RS):
 *     Sample M random sequences, score via world model, pick best.
 *     Faster than CEM, worse quality. Good warm-start for CEM.
 *
 *   MPPI (Model Predictive Path Integral):
 *     Importance-weighted Monte Carlo: sample trajectories, weight by
 *     exp(λ⁻¹ · return), update nominal sequence as weighted mean.
 *
 * Thread-safe.
 */
public class TrajectoryOptimizer {

    private static final String TAG = "TrajectoryOptimizer";

    public enum Backend { CROSS_ENTROPY_METHOD, RANDOM_SHOOTING, MPPI }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int      horizon;       // planning horizon H
    private final int      actionDim;
    private final int      numSamples;    // population size N
    private final int      numElites;     // top-K for CEM
    private final int      numIter;       // CEM iterations
    private final float    mppiLambda;    // MPPI temperature
    private final Backend  backend;

    // CEM distribution parameters (per timestep, per action)
    private final float[]  mu;    // [H] — nominal action (continuous, clipped to [0, actionDim-1])
    private final float[]  sigma; // [H]

    // World model reference
    private TransitionModel worldModel;   // optional; enables model-based scoring

    private final AtomicInteger optimizeCount = new AtomicInteger(0);
    private float avgBestReturn = 0f;
    private float avgPlanTime   = 0f;

    private final Random rng = new Random(139L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public TrajectoryOptimizer(int horizon, int actionDim, int numSamples,
                                int numElites, int numIter, float mppiLambda,
                                Backend backend) {
        this.horizon     = horizon;
        this.actionDim   = actionDim;
        this.numSamples  = numSamples;
        this.numElites   = numElites;
        this.numIter     = numIter;
        this.mppiLambda  = mppiLambda;
        this.backend     = backend;

        mu    = new float[horizon];
        sigma = new float[horizon];
        java.util.Arrays.fill(mu,    actionDim / 2f);
        java.util.Arrays.fill(sigma, actionDim / 2f);

        Log.i(TAG, "TrajectoryOptimizer: H=" + horizon + " N=" + numSamples
                + " backend=" + backend);
    }

    public TrajectoryOptimizer(int horizon, int actionDim) {
        this(horizon, actionDim, 64, 16, 5, 0.1f, Backend.CROSS_ENTROPY_METHOD);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // World model injection
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized void setWorldModel(TransitionModel model) {
        this.worldModel = model;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Planning
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Plan an optimal action sequence from the current state.
     *
     * @param state           Current state vector.
     * @param rewardFn        Scoring function: (state, action) → reward.
     * @return Best action sequence int[horizon].
     */
    public synchronized int[] plan(float[] state, RewardFunction rewardFn) {
        long t0 = System.currentTimeMillis();

        int[] best;
        switch (backend) {
            case RANDOM_SHOOTING:    best = randomShooting(state, rewardFn);      break;
            case MPPI:               best = mppi(state, rewardFn);                break;
            case CROSS_ENTROPY_METHOD:
            default:                 best = cem(state, rewardFn);                 break;
        }

        avgPlanTime = 0.9f * avgPlanTime + 0.1f * (System.currentTimeMillis() - t0);
        optimizeCount.incrementAndGet();
        return best;
    }

    /** Functional interface for scoring (state, action) → reward. */
    public interface RewardFunction {
        float reward(float[] state, int action);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Backend implementations
    // ─────────────────────────────────────────────────────────────────────────

    private int[] cem(float[] initState, RewardFunction rewardFn) {
        float[] bestSeqF = mu.clone();
        float   bestReturn = Float.NEGATIVE_INFINITY;

        for (int iter = 0; iter < numIter; iter++) {
            // Sample population
            float[][] samples  = new float[numSamples][horizon];
            float[]   returns  = new float[numSamples];

            for (int i = 0; i < numSamples; i++) {
                for (int h = 0; h < horizon; h++) {
                    samples[i][h] = mu[h] + sigma[h] * (float) rng.nextGaussian();
                    samples[i][h] = Math.max(0, Math.min(actionDim - 1, samples[i][h]));
                }
                returns[i] = rollout(initState, toIntSeq(samples[i]), rewardFn);
            }

            // Elite set: top-K by return
            int[] eliteIdx = topKIndices(returns, numElites);
            float[] newMu  = new float[horizon];
            float[] newSig = new float[horizon];

            for (int idx : eliteIdx) {
                for (int h = 0; h < horizon; h++) newMu[h] += samples[idx][h];
            }
            for (int h = 0; h < horizon; h++) newMu[h] /= numElites;
            for (int idx : eliteIdx) {
                for (int h = 0; h < horizon; h++) {
                    float d = samples[idx][h] - newMu[h]; newSig[h] += d * d;
                }
            }
            for (int h = 0; h < horizon; h++)
                newSig[h] = (float) Math.sqrt(newSig[h] / numElites + 1e-6f);

            System.arraycopy(newMu,  0, mu,    0, horizon);
            System.arraycopy(newSig, 0, sigma, 0, horizon);

            // Track best
            for (int i = 0; i < numSamples; i++) {
                if (returns[i] > bestReturn) { bestReturn = returns[i]; bestSeqF = samples[i].clone(); }
            }
        }

        avgBestReturn = 0.99f * avgBestReturn + 0.01f * bestReturn;
        // Shift mu for receding horizon (MPC)
        System.arraycopy(mu, 1, mu, 0, horizon - 1);
        mu[horizon - 1] = actionDim / 2f;
        return toIntSeq(bestSeqF);
    }

    private int[] randomShooting(float[] initState, RewardFunction rewardFn) {
        float bestReturn = Float.NEGATIVE_INFINITY;
        int[] bestSeq    = new int[horizon];

        for (int i = 0; i < numSamples; i++) {
            int[] seq = randomSeq();
            float ret = rollout(initState, seq, rewardFn);
            if (ret > bestReturn) { bestReturn = ret; bestSeq = seq; }
        }
        avgBestReturn = 0.99f * avgBestReturn + 0.01f * bestReturn;
        return bestSeq;
    }

    private int[] mppi(float[] initState, RewardFunction rewardFn) {
        float[][] seqs    = new float[numSamples][horizon];
        float[]   returns = new float[numSamples];

        for (int i = 0; i < numSamples; i++) {
            for (int h = 0; h < horizon; h++) {
                seqs[i][h] = mu[h] + sigma[h] * (float) rng.nextGaussian();
                seqs[i][h] = Math.max(0, Math.min(actionDim - 1, seqs[i][h]));
            }
            returns[i] = rollout(initState, toIntSeq(seqs[i]), rewardFn);
        }

        // Importance weights: w_i = exp((G_i - G_max) / λ)
        float gMax = returns[0]; for (float g : returns) if (g > gMax) gMax = g;
        float[] weights = new float[numSamples];
        float wSum = 0;
        for (int i = 0; i < numSamples; i++) {
            weights[i] = (float) Math.exp((returns[i] - gMax) / mppiLambda);
            wSum += weights[i];
        }

        // Weighted mean update of mu
        float[] newMu = new float[horizon];
        for (int i = 0; i < numSamples; i++) {
            float w = weights[i] / wSum;
            for (int h = 0; h < horizon; h++) newMu[h] += w * seqs[i][h];
        }
        System.arraycopy(newMu, 0, mu, 0, horizon);

        float bestReturn = gMax;
        avgBestReturn = 0.99f * avgBestReturn + 0.01f * bestReturn;
        return toIntSeq(mu);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rollout simulation
    // ─────────────────────────────────────────────────────────────────────────

    private float rollout(float[] initState, int[] actions, RewardFunction rewardFn) {
        float[] state = initState.clone();
        float G = 0, disc = 1f;
        for (int h = 0; h < horizon && h < actions.length; h++) {
            G    += disc * rewardFn.reward(state, actions[h]);
            disc *= 0.99f;
            if (worldModel != null) {
                float[] pred = worldModel.predictNextState(state, actions[h]);
                state = pred;
            }
        }
        return G;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private int[] randomSeq() {
        int[] s = new int[horizon];
        for (int h = 0; h < horizon; h++) s[h] = rng.nextInt(actionDim);
        return s;
    }

    private static int[] toIntSeq(float[] f) {
        int[] out = new int[f.length];
        for (int i = 0; i < f.length; i++) out[i] = Math.max(0, Math.min(Integer.MAX_VALUE, Math.round(f[i])));
        return out;
    }

    private static int[] topKIndices(float[] vals, int k) {
        int[] idx = new int[vals.length];
        for (int i = 0; i < idx.length; i++) idx[i] = i;
        // partial selection sort
        for (int i = 0; i < k; i++) {
            int best = i;
            for (int j = i + 1; j < idx.length; j++)
                if (vals[idx[j]] > vals[idx[best]]) best = j;
            int tmp = idx[i]; idx[i] = idx[best]; idx[best] = tmp;
        }
        int[] top = new int[k];
        System.arraycopy(idx, 0, top, 0, k);
        return top;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("backend",       backend.name());
        s.put("horizon",       horizon);
        s.put("numSamples",    numSamples);
        s.put("optimizeCount", optimizeCount.get());
        s.put("avgBestReturn", avgBestReturn);
        s.put("avgPlanTimeMs", avgPlanTime);
        return s;
    }
}
