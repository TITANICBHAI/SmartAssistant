package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * ExperienceAugmenter — synthetic data augmentation for experience replay buffers.
 *
 * Techniques implemented:
 *
 * 1. GAUSSIAN_NOISE — adds N(0, σ) noise to state and next-state features.
 *    Prevents over-fitting to exact pixel values.
 *
 * 2. MIXUP — interpolates two experiences: s̃ = λs_i + (1−λ)s_j,  r̃ = λr_i + (1−λ)r_j.
 *    λ drawn from Beta(α, α).  Action is inherited from the higher-weight experience.
 *
 * 3. HINDSIGHT_EXPERIENCE_REPLAY (HER) — relabels a failed trajectory:
 *    replaces the goal with the state actually reached, converting failure → success.
 *    Useful for sparse-reward environments.
 *
 * 4. REWARD_PERTURBATION — applies small uniform noise to the reward to smooth
 *    the reward landscape and reduce cliff effects.
 *
 * 5. STATE_DROPOUT — randomly zeros a fraction of state features (like MC dropout),
 *    creating robust representations for partially-observable environments.
 *
 * Each technique can be individually enabled/disabled and has configurable
 * hyper-parameters.  The augmenter can produce N synthetic experiences per
 * real experience.
 */
public class ExperienceAugmenter {

    private static final String TAG = "ExperienceAugmenter";

    // -------------------------------------------------------------------------
    // Technique enum
    // -------------------------------------------------------------------------
    public enum Technique {
        GAUSSIAN_NOISE,
        MIXUP,
        HINDSIGHT_EXPERIENCE_REPLAY,
        REWARD_PERTURBATION,
        STATE_DROPOUT
    }

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------
    private final Map<Technique, Boolean> enabled = new HashMap<>();
    private float gaussianSigma   = 0.02f;  // std for Gaussian noise
    private float mixupAlpha      = 0.2f;   // Beta(α,α) parameter for Mixup
    private float rewardNoise     = 0.05f;  // uniform noise range ±rewardNoise
    private float dropoutRate     = 0.10f;  // fraction of features zeroed in STATE_DROPOUT
    private int   augPerExperience = 2;     // synthetic experiences generated per real one

    private final Random rng;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    public ExperienceAugmenter(long seed) {
        this.rng = new Random(seed);
        // Enable all techniques by default
        for (Technique t : Technique.values()) enabled.put(t, true);
        // Disable HER by default (needs domain knowledge to be useful)
        enabled.put(Technique.HINDSIGHT_EXPERIENCE_REPLAY, false);
    }

    public ExperienceAugmenter() {
        this(42L);
    }

    // -------------------------------------------------------------------------
    // Configuration API
    // -------------------------------------------------------------------------

    public void setEnabled(Technique t, boolean on)  { enabled.put(t, on); }
    public void setGaussianSigma(float sigma)         { this.gaussianSigma = sigma; }
    public void setMixupAlpha(float alpha)            { this.mixupAlpha = alpha; }
    public void setRewardNoise(float noise)           { this.rewardNoise = noise; }
    public void setDropoutRate(float rate)            { this.dropoutRate = rate; }
    public void setAugPerExperience(int n)            { this.augPerExperience = Math.max(1, n); }

    // -------------------------------------------------------------------------
    // Main API
    // -------------------------------------------------------------------------

    /**
     * Augment a single experience, returning a list of synthetic variants.
     *
     * @param exp  The real experience to augment.
     * @return     List of synthetic experiences (length ≤ augPerExperience × enabled_techniques).
     */
    public List<MemoryReplayBuffer.Experience> augment(MemoryReplayBuffer.Experience exp) {
        List<MemoryReplayBuffer.Experience> result = new ArrayList<>();

        for (int n = 0; n < augPerExperience; n++) {
            // Pick one technique probabilistically
            Technique t = pickTechnique();
            if (t == null) continue;

            MemoryReplayBuffer.Experience augmented = applyTechnique(t, exp, null);
            if (augmented != null) result.add(augmented);
        }

        return result;
    }

    /**
     * Augment a batch of experiences, including cross-experience techniques (Mixup).
     *
     * @param batch Real experiences.
     * @return      All synthetic variants, may be larger than the input batch.
     */
    public List<MemoryReplayBuffer.Experience> augmentBatch(
            List<MemoryReplayBuffer.Experience> batch) {

        List<MemoryReplayBuffer.Experience> result = new ArrayList<>();
        if (batch.isEmpty()) return result;

        for (int i = 0; i < batch.size(); i++) {
            MemoryReplayBuffer.Experience expI = batch.get(i);

            for (int n = 0; n < augPerExperience; n++) {
                Technique t = pickTechnique();
                if (t == null) continue;

                MemoryReplayBuffer.Experience peer = null;
                if (t == Technique.MIXUP && batch.size() > 1) {
                    int j = rng.nextInt(batch.size());
                    if (j == i) j = (j + 1) % batch.size();
                    peer = batch.get(j);
                }

                MemoryReplayBuffer.Experience aug = applyTechnique(t, expI, peer);
                if (aug != null) result.add(aug);
            }
        }

        return result;
    }

    // -------------------------------------------------------------------------
    // Technique implementations
    // -------------------------------------------------------------------------

    private MemoryReplayBuffer.Experience applyTechnique(
            Technique t, MemoryReplayBuffer.Experience exp,
            MemoryReplayBuffer.Experience peer) {
        switch (t) {
            case GAUSSIAN_NOISE:    return gaussianNoise(exp);
            case MIXUP:             return peer != null ? mixup(exp, peer) : gaussianNoise(exp);
            case REWARD_PERTURBATION: return rewardPerturbation(exp);
            case STATE_DROPOUT:     return stateDropout(exp);
            case HINDSIGHT_EXPERIENCE_REPLAY: return her(exp);
            default:                return null;
        }
    }

    /** Add Gaussian noise to state and next-state. */
    private MemoryReplayBuffer.Experience gaussianNoise(MemoryReplayBuffer.Experience exp) {
        float[] s  = addNoise(exp.state,     gaussianSigma);
        float[] ns = addNoise(exp.nextState, gaussianSigma);
        return new MemoryReplayBuffer.Experience(s, exp.action, exp.reward, ns, exp.done);
    }

    /** Mixup: linear interpolation of two experiences. */
    private MemoryReplayBuffer.Experience mixup(
            MemoryReplayBuffer.Experience a, MemoryReplayBuffer.Experience b) {
        float lambda = (float) betaSample(mixupAlpha, mixupAlpha);
        float[] s    = interpolate(a.state,     b.state,     lambda);
        float[] ns   = interpolate(a.nextState, b.nextState, lambda);
        float   r    = lambda * a.reward + (1f - lambda) * b.reward;
        int     act  = lambda >= 0.5f ? a.action : b.action;
        boolean done = lambda >= 0.5f ? a.done   : b.done;
        return new MemoryReplayBuffer.Experience(s, act, r, ns, done);
    }

    /** Perturb reward with small uniform noise. */
    private MemoryReplayBuffer.Experience rewardPerturbation(MemoryReplayBuffer.Experience exp) {
        float r = exp.reward + (rng.nextFloat() * 2f - 1f) * rewardNoise;
        return new MemoryReplayBuffer.Experience(
                exp.state.clone(), exp.action, r, exp.nextState.clone(), exp.done);
    }

    /** Randomly zero a fraction of state features. */
    private MemoryReplayBuffer.Experience stateDropout(MemoryReplayBuffer.Experience exp) {
        float[] s  = exp.state.clone();
        float[] ns = exp.nextState.clone();
        for (int i = 0; i < s.length;  i++) if (rng.nextFloat() < dropoutRate) s[i]  = 0f;
        for (int i = 0; i < ns.length; i++) if (rng.nextFloat() < dropoutRate) ns[i] = 0f;
        return new MemoryReplayBuffer.Experience(s, exp.action, exp.reward, ns, exp.done);
    }

    /**
     * Hindsight Experience Replay: if done=false, pretend the next-state WAS the goal.
     * Sets reward = +1.0 and done = true.
     */
    private MemoryReplayBuffer.Experience her(MemoryReplayBuffer.Experience exp) {
        if (exp.done) return null; // already terminal
        // Relabel: the "goal" is wherever we actually ended up
        return new MemoryReplayBuffer.Experience(
                exp.state.clone(), exp.action, 1.0f, exp.nextState.clone(), true);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private float[] addNoise(float[] v, float sigma) {
        float[] out = v.clone();
        for (int i = 0; i < out.length; i++) {
            out[i] = Math.max(0f, Math.min(1f, out[i] + (float)(rng.nextGaussian() * sigma)));
        }
        return out;
    }

    private static float[] interpolate(float[] a, float[] b, float lambda) {
        int len = Math.min(a.length, b.length);
        float[] out = new float[len];
        for (int i = 0; i < len; i++) out[i] = lambda * a[i] + (1f - lambda) * b[i];
        return out;
    }

    private Technique pickTechnique() {
        List<Technique> active = new ArrayList<>();
        for (Map.Entry<Technique, Boolean> e : enabled.entrySet()) {
            if (e.getValue()) active.add(e.getKey());
        }
        if (active.isEmpty()) return null;
        return active.get(rng.nextInt(active.size()));
    }

    private double betaSample(double a, double b) {
        double ga = gammaSample(a);
        double gb = gammaSample(b);
        double sum = ga + gb;
        return sum <= 0 ? 0.5 : ga / sum;
    }

    private double gammaSample(double shape) {
        if (shape < 1.0) return gammaSample(1.0 + shape) * Math.pow(rng.nextDouble(), 1.0 / shape);
        double d = shape - 1.0 / 3.0;
        double c = 1.0 / Math.sqrt(9.0 * d);
        while (true) {
            double x, v;
            do { x = rng.nextGaussian(); v = 1.0 + c * x; } while (v <= 0);
            v = v * v * v;
            double u = rng.nextDouble();
            if (u < 1.0 - 0.0331 * x * x * x * x) return d * v;
            if (Math.log(u) < 0.5 * x * x + d * (1.0 - v + Math.log(v))) return d * v;
        }
    }

    public Map<String, Object> getConfig() {
        Map<String, Object> c = new HashMap<>();
        for (Map.Entry<Technique, Boolean> e : enabled.entrySet()) {
            c.put(e.getKey().name(), e.getValue());
        }
        c.put("gaussianSigma",    gaussianSigma);
        c.put("mixupAlpha",       mixupAlpha);
        c.put("rewardNoise",      rewardNoise);
        c.put("dropoutRate",      dropoutRate);
        c.put("augPerExperience", augPerExperience);
        return c;
    }
}
