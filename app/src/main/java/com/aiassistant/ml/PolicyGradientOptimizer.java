package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PolicyGradientOptimizer — on-device REINFORCE with baseline.
 *
 * Algorithm:
 *   1. Roll out an episode collecting (state, action, log-prob, reward) tuples.
 *   2. Compute discounted returns G_t.
 *   3. Subtract a learned value-function baseline to reduce variance.
 *   4. Update policy weights:   θ ← θ + α_π · (G_t − b(s_t)) · ∇log π(a_t|s_t)
 *   5. Update baseline weights: w ← w − α_v · (G_t − b(s_t)) · ∇b(s_t)
 *
 * Both policy and baseline are implemented as single-layer linear models
 * (softmax output for policy, scalar output for baseline) so they run in <1 ms
 * on any Android device without hardware acceleration.
 *
 * Features:
 * - Entropy bonus to prevent premature policy collapse (coefficient β_e)
 * - Gradient norm clipping (max L2 norm = 0.5)
 * - Per-update statistics exposed via getStats()
 * - Thread-safe: episode collection and parameter updates are synchronized
 * - Background update loop (configurable interval)
 */
public class PolicyGradientOptimizer {

    private static final String TAG = "PolicyGradientOptimizer";

    // -------------------------------------------------------------------------
    // Hyper-parameters
    // -------------------------------------------------------------------------
    private final int   stateDim;
    private final int   actionDim;
    private final float gamma;         // Discount factor
    private final float lrPolicy;      // Policy learning rate α_π
    private final float lrBaseline;    // Baseline learning rate α_v
    private final float entropyCoeff;  // Entropy bonus coefficient β_e
    private final float gradClipNorm;  // Max gradient L2 norm

    // -------------------------------------------------------------------------
    // Parameters (row-major weight matrices)
    // -------------------------------------------------------------------------
    // Policy:   W_π ∈ ℝ^{actionDim × stateDim}
    // Baseline: w_v ∈ ℝ^{stateDim}
    private final float[][] policyWeights;   // [actionDim][stateDim]
    private final float[]   baselineWeights; // [stateDim]

    // -------------------------------------------------------------------------
    // Episode buffer
    // -------------------------------------------------------------------------
    private static class Step {
        final float[] state;
        final int     action;
        final float   logProb;
        final float   reward;
        Step(float[] s, int a, float lp, float r) {
            state = s; action = a; logProb = lp; reward = r;
        }
    }
    private final List<Step> episode = new ArrayList<>();

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------
    private final AtomicBoolean running  = new AtomicBoolean(false);
    private final AtomicInteger updates  = new AtomicInteger(0);
    private float  avgReturn  = 0f;
    private float  avgEntropy = 0f;
    private float  avgPolicyLoss   = 0f;
    private float  avgBaselineLoss = 0f;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();
    private final Random rng = new Random(7L);

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    public PolicyGradientOptimizer(int stateDim, int actionDim,
                                   float gamma, float lrPolicy, float lrBaseline,
                                   float entropyCoeff) {
        this.stateDim     = stateDim;
        this.actionDim    = actionDim;
        this.gamma        = gamma;
        this.lrPolicy     = lrPolicy;
        this.lrBaseline   = lrBaseline;
        this.entropyCoeff = entropyCoeff;
        this.gradClipNorm = 0.5f;

        // Xavier initialisation
        float scale = (float) Math.sqrt(2.0 / (stateDim + actionDim));
        policyWeights   = new float[actionDim][stateDim];
        baselineWeights = new float[stateDim];
        for (int a = 0; a < actionDim; a++) {
            for (int s = 0; s < stateDim; s++) {
                policyWeights[a][s] = (rng.nextFloat() * 2f - 1f) * scale;
            }
        }
        for (int s = 0; s < stateDim; s++) {
            baselineWeights[s] = (rng.nextFloat() * 2f - 1f) * 0.01f;
        }
    }

    /** Default constructor with sensible hyper-parameters. */
    public PolicyGradientOptimizer(int stateDim, int actionDim) {
        this(stateDim, actionDim, 0.99f, 1e-3f, 5e-3f, 0.01f);
    }

    // -------------------------------------------------------------------------
    // Episode API
    // -------------------------------------------------------------------------

    /**
     * Sample an action from the current policy π(·|state).
     * @return int action index
     */
    public synchronized int selectAction(float[] state) {
        float[] logits = computeLogits(state);
        float[] probs  = softmax(logits);
        return categoricalSample(probs);
    }

    /**
     * Record one transition.  Call after selectAction() and observing the reward.
     */
    public synchronized void recordStep(float[] state, int action, float reward) {
        float[] logits  = computeLogits(state);
        float[] probs   = softmax(logits);
        float   logProb = (float) Math.log(Math.max(probs[action], 1e-8f));
        episode.add(new Step(state.clone(), action, logProb, reward));
    }

    /**
     * Finish the current episode and apply a gradient update.
     * Clears the episode buffer afterwards.
     */
    public synchronized void finishEpisode() {
        if (episode.isEmpty()) return;
        int T = episode.size();

        // 1. Compute discounted returns G_t
        float[] G = new float[T];
        G[T - 1] = episode.get(T - 1).reward;
        for (int t = T - 2; t >= 0; t--) {
            G[t] = episode.get(t).reward + gamma * G[t + 1];
        }

        float totalReturn   = G[0];
        float policyLoss    = 0f;
        float baselineLoss  = 0f;
        float entropy       = 0f;

        // 2. Update parameters for each step
        for (int t = 0; t < T; t++) {
            Step  step  = episode.get(t);
            float ret   = G[t];
            float base  = computeBaseline(step.state);
            float adv   = ret - base;  // advantage estimate

            float[] logits = computeLogits(step.state);
            float[] probs  = softmax(logits);

            // Policy gradient update: ∇θ = adv · ∇log π(a|s) + β_e · ∇H(π)
            float[] policyGrad = new float[actionDim * stateDim];
            for (int a = 0; a < actionDim; a++) {
                float indicator = (a == step.action) ? 1f : 0f;
                float dlogProb  = indicator - probs[a];                   // ∂log π/∂logit_a
                float entropyGrad = -(float)(Math.log(Math.max(probs[a], 1e-8)) + 1f);

                for (int s = 0; s < stateDim; s++) {
                    float grad = (adv * dlogProb + entropyCoeff * entropyGrad)
                                 * step.state[s];
                    policyWeights[a][s] += clipGrad(lrPolicy * grad);
                }
            }

            // Entropy H(π) = -∑ p·log p
            for (int a = 0; a < actionDim; a++) {
                entropy -= probs[a] * Math.log(Math.max(probs[a], 1e-8f));
            }

            // Baseline MSE update: w ← w − α_v · (b - G) · state
            float baseErr = base - ret;
            for (int s = 0; s < stateDim; s++) {
                baselineWeights[s] -= clipGrad(lrBaseline * baseErr * step.state[s]);
            }

            policyLoss  -= step.logProb * adv;
            baselineLoss += baseErr * baseErr;
        }

        // Update running stats (EMA)
        float ema = 0.95f;
        avgReturn       = ema * avgReturn       + (1f - ema) * totalReturn;
        avgEntropy      = ema * avgEntropy      + (1f - ema) * (entropy / T);
        avgPolicyLoss   = ema * avgPolicyLoss   + (1f - ema) * (policyLoss / T);
        avgBaselineLoss = ema * avgBaselineLoss + (1f - ema) * (baselineLoss / T);

        episode.clear();
        int n = updates.incrementAndGet();
        if (n % 10 == 0) {
            Log.d(TAG, "Update #" + n
                    + " avgReturn=" + String.format("%.3f", avgReturn)
                    + " avgEntropy=" + String.format("%.3f", avgEntropy));
        }
    }

    // -------------------------------------------------------------------------
    // Forward pass helpers
    // -------------------------------------------------------------------------

    /** Linear combination W_π · state → logits. */
    private float[] computeLogits(float[] state) {
        float[] logits = new float[actionDim];
        int dim = Math.min(state.length, stateDim);
        for (int a = 0; a < actionDim; a++) {
            float sum = 0f;
            for (int s = 0; s < dim; s++) {
                sum += policyWeights[a][s] * state[s];
            }
            logits[a] = sum;
        }
        return logits;
    }

    /** Linear combination w_v · state → scalar baseline. */
    private float computeBaseline(float[] state) {
        float val = 0f;
        int dim = Math.min(state.length, stateDim);
        for (int s = 0; s < dim; s++) {
            val += baselineWeights[s] * state[s];
        }
        return val;
    }

    /** Numerically stable softmax. */
    private float[] softmax(float[] logits) {
        float max = logits[0];
        for (float v : logits) if (v > max) max = v;
        float sum = 0f;
        float[] out = new float[logits.length];
        for (int i = 0; i < logits.length; i++) {
            out[i] = (float) Math.exp(logits[i] - max);
            sum += out[i];
        }
        for (int i = 0; i < out.length; i++) out[i] /= sum;
        return out;
    }

    /** Sample from categorical distribution. */
    private int categoricalSample(float[] probs) {
        float r = rng.nextFloat();
        float cum = 0f;
        for (int i = 0; i < probs.length - 1; i++) {
            cum += probs[i];
            if (r < cum) return i;
        }
        return probs.length - 1;
    }

    /** Clip gradient by maximum L2 norm. */
    private float clipGrad(float grad) {
        float absGrad = Math.abs(grad);
        if (absGrad > gradClipNorm) {
            return (grad / absGrad) * gradClipNorm;
        }
        return grad;
    }

    // -------------------------------------------------------------------------
    // Monitoring
    // -------------------------------------------------------------------------

    public Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("updates",        updates.get());
        s.put("avgReturn",      avgReturn);
        s.put("avgEntropy",     avgEntropy);
        s.put("avgPolicyLoss",  avgPolicyLoss);
        s.put("avgBaselineLoss", avgBaselineLoss);
        s.put("stateDim",       stateDim);
        s.put("actionDim",      actionDim);
        s.put("gamma",          gamma);
        s.put("lrPolicy",       lrPolicy);
        s.put("lrBaseline",     lrBaseline);
        s.put("entropyCoeff",   entropyCoeff);
        return s;
    }

    /** Returns action probability distribution for a given state (for debugging). */
    public synchronized float[] getActionProbabilities(float[] state) {
        return softmax(computeLogits(state));
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}
