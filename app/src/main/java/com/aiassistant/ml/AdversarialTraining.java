package com.aiassistant.ml;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AdversarialTraining — robustness training via adversarial perturbation and self-play.
 *
 * Provides two adversarial training paradigms:
 *
 *   1. OBSERVATION PERTURBATION (FGSM / PGD style):
 *      Perturb state observations adversarially to maximize policy loss.
 *      Train the agent on perturbed states → policy becomes robust to noisy inputs.
 *
 *      FGSM:  s̃ = s + ε·sign(∇_s L(π(s), a))
 *      PGD:   s̃_0 = s;  s̃_{k+1} = clip(s̃_k + α·sign(∇_s L), s-ε, s+ε)
 *
 *   2. SELF-PLAY ADVERSARY:
 *      Maintain an adversary agent that selects environment perturbations
 *      (state noise, action delays, reward negation) to minimise protagonist reward.
 *      Protagonist and adversary alternate training.
 *
 * Both methods are optional and configured at construction time.
 * The class is used as a wrapper — it transforms the state before the agent sees it.
 *
 * Thread-safe.
 */
public class AdversarialTraining {

    private static final String TAG = "AdversarialTraining";

    public enum Mode { FGSM, PGD, SELF_PLAY, RANDOM_NOISE }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int   stateDim;
    private final int   actionDim;
    private final Mode  mode;
    private float       epsilon;       // perturbation budget
    private float       pgdAlpha;      // PGD step size
    private final int   pgdSteps;
    private final float noiseStd;      // for RANDOM_NOISE fallback

    // Self-play adversary: simple linear policy (adversary selects noise direction)
    private final float[][] advW;  // [stateDim][stateDim]
    private final float[]   advB;
    private final NeuralNetworkOptimizer advOpt;
    private float advReturn = 0f;

    // Stats
    private final AtomicInteger perturbCount = new AtomicInteger(0);
    private final AtomicInteger advUpdates   = new AtomicInteger(0);
    private float avgPerturbNorm = 0f;
    private float avgProtReturn  = 0f;

    private final Random rng = new Random(157L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public AdversarialTraining(int stateDim, int actionDim, Mode mode,
                                float epsilon, float pgdAlpha, int pgdSteps,
                                float noiseStd, float advLr) {
        this.stateDim  = stateDim;
        this.actionDim = actionDim;
        this.mode      = mode;
        this.epsilon   = epsilon;
        this.pgdAlpha  = pgdAlpha;
        this.pgdSteps  = pgdSteps;
        this.noiseStd  = noiseStd;
        this.advOpt    = new NeuralNetworkOptimizer(advLr);

        float s = (float) Math.sqrt(2.0 / (stateDim * 2));
        advW = new float[stateDim][stateDim];
        advB = new float[stateDim];
        for (int i = 0; i < stateDim; i++)
            for (int j = 0; j < stateDim; j++) advW[i][j] = (rng.nextFloat()*2f-1f)*s;

        Log.i(TAG, "AdversarialTraining: mode=" + mode + " ε=" + epsilon);
    }

    public AdversarialTraining(int stateDim, int actionDim, Mode mode) {
        this(stateDim, actionDim, mode, 0.05f, 0.01f, 7, 0.01f, 1e-4f);
    }

    public AdversarialTraining(int stateDim, int actionDim) {
        this(stateDim, actionDim, Mode.RANDOM_NOISE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Perturb the observation to challenge the agent.
     *
     * @param state      Original state.
     * @param policyGrad ∇_s L (gradient of policy loss w.r.t. state), length=stateDim.
     *                   Pass null for RANDOM_NOISE or SELF_PLAY.
     * @return Perturbed state s̃ clipped to reasonable bounds.
     */
    public synchronized float[] perturb(float[] state, float[] policyGrad) {
        float[] s = pad(state, stateDim);
        float[] pert;

        switch (mode) {
            case FGSM:       pert = fgsm(s, policyGrad);       break;
            case PGD:        pert = pgd(s, policyGrad);         break;
            case SELF_PLAY:  pert = selfPlayPert(s);            break;
            case RANDOM_NOISE:
            default:         pert = randomNoise(s);             break;
        }

        // Track perturbation norm
        float norm = 0;
        for (int i = 0; i < stateDim; i++) { float d = pert[i]-s[i]; norm += d*d; }
        avgPerturbNorm = 0.99f * avgPerturbNorm + 0.01f * (float) Math.sqrt(norm);
        perturbCount.incrementAndGet();
        return pert;
    }

    /** Convenience overload — no gradient (uses RANDOM_NOISE internally). */
    public synchronized float[] perturb(float[] state) {
        return perturb(state, null);
    }

    /**
     * Update the self-play adversary to maximize damage to protagonist.
     * Call after observing the protagonist's reward on the perturbed state.
     *
     * @param state           State at which perturbation was applied.
     * @param protReward      Protagonist reward (adversary wants to minimize this).
     */
    public synchronized void updateAdversary(float[] state, float protReward) {
        if (mode != Mode.SELF_PLAY) return;
        float[] s = pad(state, stateDim);
        // Adversary wants to maximise -protReward → gradient ascent on -reward
        float[] h   = linAdv(s);
        float   loss = protReward;          // adversary maximises -protReward

        // Simple gradient: push perturbation in direction of -reward gradient
        float gScale = -0.1f * protReward;
        float[][] dW = new float[stateDim][stateDim];
        for (int i = 0; i < stateDim; i++)
            for (int j = 0; j < stateDim; j++) dW[i][j] = gScale * s[j];
        advOpt.step("adv_W", advW, dW);

        advReturn    = 0.99f * advReturn    + 0.01f * (-protReward);
        avgProtReturn= 0.99f * avgProtReturn + 0.01f * protReward;
        advUpdates.incrementAndGet();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Perturbation implementations
    // ─────────────────────────────────────────────────────────────────────────

    private float[] fgsm(float[] s, float[] grad) {
        if (grad == null || grad.length == 0) return randomNoise(s);
        float[] pert = s.clone();
        for (int i = 0; i < stateDim; i++) {
            int gi = i < grad.length ? (grad[i] >= 0 ? 1 : -1) : 0;
            pert[i] = s[i] + epsilon * gi;
        }
        return pert;
    }

    private float[] pgd(float[] s, float[] grad) {
        if (grad == null || grad.length == 0) return randomNoise(s);
        float[] pert = s.clone();
        for (int step = 0; step < pgdSteps; step++) {
            for (int i = 0; i < stateDim; i++) {
                int gi = i < grad.length ? (grad[i] >= 0 ? 1 : -1) : 0;
                pert[i] = pert[i] + pgdAlpha * gi;
                // Project back into ε-ball
                pert[i] = Math.max(s[i] - epsilon, Math.min(s[i] + epsilon, pert[i]));
            }
        }
        return pert;
    }

    private float[] selfPlayPert(float[] s) {
        float[] noise = linAdv(s);
        float[] pert  = s.clone();
        // Scale noise to ε-ball
        float norm = 0; for (float n : noise) norm += n*n;
        norm = (float) Math.sqrt(norm + 1e-8f);
        for (int i = 0; i < stateDim; i++) pert[i] = s[i] + epsilon * noise[i] / norm;
        return pert;
    }

    private float[] randomNoise(float[] s) {
        float[] pert = s.clone();
        for (int i = 0; i < stateDim; i++)
            pert[i] = s[i] + noiseStd * (float) rng.nextGaussian();
        return pert;
    }

    private float[] linAdv(float[] s) {
        float[] o = new float[stateDim];
        for (int i = 0; i < stateDim; i++) {
            float v = advB[i];
            for (int j = 0; j < stateDim; j++) v += advW[i][j] * s[j];
            o[i] = (float) Math.tanh(v);
        }
        return o;
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
        s.put("mode",           mode.name());
        s.put("epsilon",        epsilon);
        s.put("perturbCount",   perturbCount.get());
        s.put("avgPerturbNorm", avgPerturbNorm);
        s.put("avgProtReturn",  avgProtReturn);
        s.put("advUpdates",     advUpdates.get());
        s.put("advReturn",      advReturn);
        return s;
    }

    public synchronized void setEpsilon(float eps) { this.epsilon = Math.max(0f, eps); }
}
