package com.aiassistant.ml;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ExplorationScheduler — adaptive epsilon and exploration rate management.
 *
 * Provides multiple epsilon annealing schedules and switches dynamically
 * based on learning progress:
 *
 *   LINEAR      — ε decreases linearly from εStart to εEnd over N steps.
 *   EXPONENTIAL — ε *= decay at each step (most common).
 *   COSINE      — ε follows a cosine curve (smooth, avoids abrupt cutoff).
 *   CYCLIC      — ε oscillates with a sawtooth pattern (re-explore periodically).
 *   ADAPTIVE    — ε increases when learning plateaus (detected by reward EMA stall).
 *   STEP        — ε drops at milestone steps (curriculum style).
 *
 * Additional features:
 *   - Per-action epsilon: UCB-style per-action confidence intervals.
 *   - Boltzmann exploration: ε → temperature τ for softmax action selection.
 *   - Entropy regularisation bonus: reward for exploring.
 *
 * Thread-safe.
 */
public class ExplorationScheduler {

    private static final String TAG = "ExplorationScheduler";

    public enum Schedule { LINEAR, EXPONENTIAL, COSINE, CYCLIC, ADAPTIVE, STEP }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final Schedule schedule;
    private final float    epsStart;
    private final float    epsEnd;
    private final float    decay;         // for EXPONENTIAL
    private final int      totalSteps;    // for LINEAR / COSINE
    private final int      cycleLen;      // for CYCLIC
    private final int[]    stepMilestones;// for STEP
    private final float[]  stepValues;    // for STEP

    private float   epsilon;
    private float   temperature;      // Boltzmann temperature (linked to epsilon)
    private int     step         = 0;

    // Adaptive schedule state
    private float   rewardEma    = 0f;
    private float   prevRewardEma= 0f;
    private int     stallSteps   = 0;
    private final int     stallThreshold;
    private final float   boostAmount;

    // Per-action UCB counts
    private final int[]   actionCounts;
    private final int     actionDim;

    private final AtomicInteger resetCount  = new AtomicInteger(0);
    private float avgEpsilon = 0f;

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public ExplorationScheduler(int actionDim, Schedule schedule,
                                 float epsStart, float epsEnd, float decay,
                                 int totalSteps, int cycleLen,
                                 int[] stepMilestones, float[] stepValues,
                                 int stallThreshold, float boostAmount) {
        this.actionDim      = actionDim;
        this.schedule       = schedule;
        this.epsStart       = epsStart;
        this.epsEnd         = epsEnd;
        this.decay          = decay;
        this.totalSteps     = totalSteps;
        this.cycleLen       = cycleLen;
        this.stepMilestones = stepMilestones != null ? stepMilestones : new int[0];
        this.stepValues     = stepValues    != null ? stepValues    : new float[0];
        this.stallThreshold = stallThreshold;
        this.boostAmount    = boostAmount;

        this.epsilon     = epsStart;
        this.temperature = epsToTemp(epsStart);
        this.actionCounts= new int[actionDim];

        Log.i(TAG, "ExplorationScheduler: " + schedule + " ε=" + epsStart + "→" + epsEnd);
    }

    /** Convenience constructors. */
    public static ExplorationScheduler exponential(int actionDim, float epsStart, float epsEnd, float decay) {
        return new ExplorationScheduler(actionDim, Schedule.EXPONENTIAL,
                epsStart, epsEnd, decay, 0, 0, null, null, 500, 0.1f);
    }

    public static ExplorationScheduler linear(int actionDim, float epsStart, float epsEnd, int steps) {
        return new ExplorationScheduler(actionDim, Schedule.LINEAR,
                epsStart, epsEnd, 0f, steps, 0, null, null, 500, 0.1f);
    }

    public static ExplorationScheduler adaptive(int actionDim) {
        return new ExplorationScheduler(actionDim, Schedule.ADAPTIVE,
                1.0f, 0.05f, 0.999f, 0, 0, null, null, 200, 0.15f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /** Advance one step and return current epsilon. */
    public synchronized float step(float rewardSignal) {
        updateEpsilon(rewardSignal);
        step++;
        avgEpsilon = 0.99f * avgEpsilon + 0.01f * epsilon;
        return epsilon;
    }

    public synchronized float getEpsilon() { return epsilon; }
    public synchronized float getTemperature() { return temperature; }

    /** UCB-style action selection bonus for under-explored actions. */
    public synchronized float ucbBonus(int action, float cUCB) {
        int total = 0; for (int c : actionCounts) total += c;
        if (total == 0 || actionCounts[action] == 0) return Float.MAX_VALUE;
        return cUCB * (float) Math.sqrt(Math.log(total + 1) / actionCounts[action]);
    }

    /** Record that action was taken (for UCB tracking). */
    public synchronized void recordAction(int action) {
        if (action >= 0 && action < actionDim) actionCounts[action]++;
    }

    /** Boltzmann action sampling: returns softmax probabilities using temperature τ. */
    public synchronized float[] boltzmannProbs(float[] qValues) {
        float[] out = new float[qValues.length];
        float τ = Math.max(temperature, 0.01f);
        float mx = qValues[0]; for (float q : qValues) if (q > mx) mx = q;
        float sum = 0;
        for (int a = 0; a < qValues.length; a++) {
            out[a] = (float) Math.exp((qValues[a] - mx) / τ); sum += out[a];
        }
        for (int a = 0; a < qValues.length; a++) out[a] /= sum;
        return out;
    }

    /** Entropy regularisation bonus: H(π) = -Σ p(a) log p(a). */
    public synchronized float entropyBonus(float[] probs, float coeff) {
        float H = 0;
        for (float p : probs) if (p > 1e-8f) H -= p * (float) Math.log(p);
        return coeff * H;
    }

    /** Reset epsilon to epsStart (e.g., at curriculum level change). */
    public synchronized void reset() {
        epsilon     = epsStart;
        temperature = epsToTemp(epsStart);
        step        = 0;
        resetCount.incrementAndGet();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Schedule implementations
    // ─────────────────────────────────────────────────────────────────────────

    private void updateEpsilon(float reward) {
        switch (schedule) {
            case LINEAR:
                float frac = Math.min(1f, (float) step / Math.max(totalSteps, 1));
                epsilon = epsStart + (epsEnd - epsStart) * frac;
                break;

            case EXPONENTIAL:
                epsilon = Math.max(epsEnd, epsilon * decay);
                break;

            case COSINE:
                float cos = (1f + (float) Math.cos(Math.PI * step / Math.max(totalSteps, 1))) / 2f;
                epsilon = epsEnd + (epsStart - epsEnd) * cos;
                break;

            case CYCLIC:
                float phase = (step % cycleLen) / (float) cycleLen;
                epsilon = epsEnd + (epsStart - epsEnd) * (1f - phase);
                break;

            case ADAPTIVE:
                epsilon = Math.max(epsEnd, epsilon * decay);
                // Detect plateau and boost epsilon
                rewardEma = 0.99f * rewardEma + 0.01f * reward;
                if (Math.abs(rewardEma - prevRewardEma) < 1e-4f) stallSteps++;
                else stallSteps = 0;
                prevRewardEma = rewardEma;
                if (stallSteps >= stallThreshold) {
                    epsilon = Math.min(epsStart, epsilon + boostAmount);
                    stallSteps = 0;
                    Log.d(TAG, "Adaptive boost: ε=" + epsilon + " (plateau detected)");
                }
                break;

            case STEP:
                for (int i = 0; i < stepMilestones.length; i++) {
                    if (step >= stepMilestones[i] && i < stepValues.length)
                        epsilon = stepValues[i];
                }
                epsilon = Math.max(epsEnd, epsilon);
                break;
        }
        epsilon = Math.max(epsEnd, Math.min(epsStart, epsilon));
        temperature = epsToTemp(epsilon);
    }

    private static float epsToTemp(float eps) {
        // Map epsilon [epsEnd, epsStart] → temperature [0.01, 5.0]
        return Math.max(0.01f, eps * 5f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("schedule",    schedule.name());
        s.put("epsilon",     epsilon);
        s.put("temperature", temperature);
        s.put("step",        step);
        s.put("avgEpsilon",  avgEpsilon);
        s.put("resetCount",  resetCount.get());
        s.put("stallSteps",  stallSteps);
        // Most/least explored actions
        int maxA = 0, minA = 0;
        for (int a = 1; a < actionDim; a++) {
            if (actionCounts[a] > actionCounts[maxA]) maxA = a;
            if (actionCounts[a] < actionCounts[minA]) minA = a;
        }
        s.put("mostExploredAction",  maxA);
        s.put("leastExploredAction", minA);
        return s;
    }
}
