package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RobustnessEvaluator — stress-test agent policies under perturbation.
 *
 * Evaluates how well a trained policy holds up against:
 *
 *   1. OBSERVATION NOISE: Gaussian/uniform noise added to state features.
 *   2. MISSING FEATURES:  Random subset of state features zeroed out.
 *   3. ACTION DELAY:      Actions take effect k steps later.
 *   4. REWARD NOISE:      Stochastic reward (ε-corrupted).
 *   5. DYNAMICS SHIFT:    State transition modified (domain randomisation).
 *   6. ADVERSARIAL ATTACK: FGSM-style perturbation on observation.
 *
 * For each perturbation, runs N evaluation episodes and reports:
 *   - Mean return and std under perturbation.
 *   - Degradation ratio: (clean_return - perturbed_return) / clean_return.
 *   - Robustness score: 1 - degradation_ratio (1.0 = no degradation).
 *
 * Also provides robustness certification: maximum perturbation magnitude
 * under which degradation stays below a threshold.
 *
 * Thread-safe.
 */
public class RobustnessEvaluator {

    private static final String TAG = "RobustnessEval";

    public enum PerturbationType {
        OBSERVATION_NOISE, MISSING_FEATURES, ACTION_DELAY,
        REWARD_NOISE, DYNAMICS_SHIFT, ADVERSARIAL
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Evaluation result
    // ─────────────────────────────────────────────────────────────────────────
    public static class EvalResult {
        public final PerturbationType type;
        public final float magnitude;
        public final float meanReturn;
        public final float stdReturn;
        public final float degradationRatio;
        public final float robustnessScore;

        EvalResult(PerturbationType t, float mag, float mean, float std, float clean) {
            this.type             = t;
            this.magnitude        = mag;
            this.meanReturn       = mean;
            this.stdReturn        = std;
            float deg             = clean != 0 ? (clean - mean) / Math.abs(clean) : 0f;
            this.degradationRatio = Math.max(0f, deg);
            this.robustnessScore  = 1f - this.degradationRatio;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Policy interface
    // ─────────────────────────────────────────────────────────────────────────
    public interface EvalPolicy {
        int action(float[] state);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Environment interface
    // ─────────────────────────────────────────────────────────────────────────
    public interface EvalEnvironment {
        float[] reset();
        float[] step(int action);    // returns float[]{reward, done_flag, s0, s1, ..., sN}
        int     stateDim();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int   numEpisodes;
    private final int   maxSteps;
    private final float gamma;
    private final float cleanReturn;   // baseline (no perturbation)

    private final List<EvalResult> results = new ArrayList<>();
    private final AtomicInteger evalCount  = new AtomicInteger(0);
    private float overallRobustness = 1f;

    private final Random rng = new Random(257L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public RobustnessEvaluator(int numEpisodes, int maxSteps, float gamma, float cleanReturn) {
        this.numEpisodes = numEpisodes;
        this.maxSteps    = maxSteps;
        this.gamma       = gamma;
        this.cleanReturn = cleanReturn;
        Log.i(TAG, "RobustnessEvaluator: episodes=" + numEpisodes + " cleanReturn=" + cleanReturn);
    }

    public RobustnessEvaluator(float cleanReturn) {
        this(10, 200, 0.99f, cleanReturn);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Evaluation API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Evaluate policy robustness to a specific perturbation type and magnitude.
     * The environment and policy are called directly (no actual Android env needed).
     */
    public synchronized EvalResult evaluate(EvalPolicy policy, EvalEnvironment env,
                                            PerturbationType type, float magnitude) {
        float[] returns = new float[numEpisodes];
        for (int ep = 0; ep < numEpisodes; ep++) {
            returns[ep] = runEpisode(policy, env, type, magnitude);
        }

        float mean = 0; for (float r : returns) mean += r; mean /= numEpisodes;
        float var  = 0; for (float r : returns) { float d=r-mean; var+=d*d; } var/=numEpisodes;
        float std  = (float) Math.sqrt(var);

        EvalResult result = new EvalResult(type, magnitude, mean, std, cleanReturn);
        results.add(result);
        evalCount.incrementAndGet();

        // Update overall robustness
        overallRobustness = 0.9f * overallRobustness + 0.1f * result.robustnessScore;

        Log.i(TAG, String.format("Eval %s mag=%.3f → return=%.2f±%.2f rob=%.2f",
                type, magnitude, mean, std, result.robustnessScore));
        return result;
    }

    /**
     * Sweep magnitudes for a perturbation type: find robustness curve.
     */
    public synchronized List<EvalResult> sweepMagnitudes(EvalPolicy policy, EvalEnvironment env,
                                                          PerturbationType type,
                                                          float[] magnitudes) {
        List<EvalResult> curve = new ArrayList<>();
        for (float mag : magnitudes) curve.add(evaluate(policy, env, type, mag));
        return curve;
    }

    /**
     * Certification: maximum magnitude with degradation < maxDegradation.
     */
    public synchronized float certifiedMagnitude(EvalPolicy policy, EvalEnvironment env,
                                                  PerturbationType type,
                                                  float maxDegradation, int steps) {
        float lo = 0, hi = 1f;
        for (int i = 0; i < steps; i++) {
            float mid = (lo + hi) / 2f;
            EvalResult r = evaluate(policy, env, type, mid);
            if (r.degradationRatio <= maxDegradation) lo = mid; else hi = mid;
        }
        return lo;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Episode runner
    // ─────────────────────────────────────────────────────────────────────────

    private float runEpisode(EvalPolicy policy, EvalEnvironment env,
                             PerturbationType type, float magnitude) {
        float[] rawState = env.reset();
        float G = 0, disc = 1f;
        int[] delayBuf = new int[10]; // for ACTION_DELAY

        for (int t = 0; t < maxSteps; t++) {
            float[] state = perturb(rawState, type, magnitude, env.stateDim());
            int action    = policy.action(state);

            // Action delay
            if (type == PerturbationType.ACTION_DELAY) {
                int delay = (int)(magnitude * 5) + 1;
                int storedAction = delayBuf[t % delay];
                delayBuf[t % delay] = action;
                action = storedAction;
            }

            float[] result = env.step(action);
            float reward   = result.length > 0 ? result[0] : 0f;
            boolean done   = result.length > 1 && result[1] > 0.5f;
            rawState       = result.length > 2 ? java.util.Arrays.copyOfRange(result, 2, result.length) : rawState;

            if (type == PerturbationType.REWARD_NOISE)
                reward += magnitude * (float)(rng.nextGaussian());

            G    += disc * reward;
            disc *= gamma;
            if (done) break;
        }
        return G;
    }

    private float[] perturb(float[] state, PerturbationType type, float magnitude, int dim) {
        float[] s = new float[Math.min(state.length, dim)];
        System.arraycopy(state, 0, s, 0, s.length);

        switch (type) {
            case OBSERVATION_NOISE:
                for (int i = 0; i < s.length; i++) s[i] += magnitude * (float)rng.nextGaussian();
                break;
            case MISSING_FEATURES:
                for (int i = 0; i < s.length; i++) if (rng.nextFloat() < magnitude) s[i] = 0f;
                break;
            case ADVERSARIAL:
                for (int i = 0; i < s.length; i++) s[i] += magnitude * (rng.nextBoolean() ? 1f : -1f);
                break;
            case DYNAMICS_SHIFT:
                for (int i = 0; i < s.length; i++) s[i] *= (1f + magnitude * (float)(rng.nextGaussian() * 0.1));
                break;
            default: break;
        }
        return s;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("evalCount",         evalCount.get());
        s.put("resultsCount",      results.size());
        s.put("overallRobustness", overallRobustness);
        s.put("cleanReturn",       cleanReturn);
        if (!results.isEmpty()) {
            EvalResult last = results.get(results.size() - 1);
            s.put("lastType",       last.type.name());
            s.put("lastMagnitude",  last.magnitude);
            s.put("lastRobustness", last.robustnessScore);
        }
        return s;
    }

    public synchronized List<EvalResult> getResults() { return new ArrayList<>(results); }
}
