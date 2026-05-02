package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MultiArmedBandit — flexible bandit implementation for hyperparameter tuning,
 * A/B testing, and online algorithm selection.
 *
 * Supported algorithms:
 *   EPSILON_GREEDY    — exploration rate ε with linear decay
 *   UCB1              — upper confidence bound (Auer et al., 2002)
 *   UCB1_TUNED        — UCB1 with variance estimate (slightly tighter bound)
 *   THOMPSON_SAMPLING — Beta-distributed Thompson Sampling
 *   EXP3              — adversarial setting, exponential weights (Auer et al., 2002)
 *
 * Typical use cases:
 *   - Selecting between DQN / PPO / Q-Learning at runtime based on per-episode reward
 *   - Online learning rate tuning (arm = lr bucket)
 *   - Choosing exploration strategy for the current game context
 *
 * All algorithms:
 *   - Thread-safe via synchronized methods
 *   - Expose getStats() with per-arm pull/reward info
 *   - Support regret tracking vs. the best arm
 */
public class MultiArmedBandit {

    private static final String TAG = "MultiArmedBandit";

    // -------------------------------------------------------------------------
    // Algorithm enum
    // -------------------------------------------------------------------------
    public enum Algorithm {
        EPSILON_GREEDY, UCB1, UCB1_TUNED, THOMPSON_SAMPLING, EXP3
    }

    // -------------------------------------------------------------------------
    // Arm state
    // -------------------------------------------------------------------------
    private static class Arm {
        final int    id;
        final String name;
        int          pulls       = 0;
        double       totalReward = 0.0;
        double       totalSqReward = 0.0; // for UCB1-Tuned variance

        // Thompson Sampling: Beta(α, β)
        double alpha = 1.0;  // successes + 1
        double beta  = 1.0;  // failures  + 1

        // EXP3: probability weight
        double weight = 1.0;

        Arm(int id, String name) {
            this.id   = id;
            this.name = name;
        }

        double mean() { return pulls > 0 ? totalReward / pulls : 0.0; }

        double variance() {
            if (pulls < 2) return 0.25; // max variance for Beta
            double m = mean();
            return totalSqReward / pulls - m * m;
        }
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------
    private final List<Arm>  arms;
    private final int        numArms;
    private final Algorithm  algorithm;

    // Epsilon-greedy
    private double epsilon     = 0.15;
    private final double epsilonMin   = 0.01;
    private final double epsilonDecay = 0.995;

    // EXP3
    private final double gamma3 = 0.07;  // exploration parameter

    // Global stats
    private final AtomicInteger totalPulls = new AtomicInteger(0);
    private double cumulativeRegret = 0.0;
    private int    lastChosenArm    = 0;

    private final Random rng;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    public MultiArmedBandit(int numArms, Algorithm algorithm, long seed) {
        this.numArms   = numArms;
        this.algorithm = algorithm;
        this.rng       = new Random(seed);
        this.arms      = new ArrayList<>(numArms);
        for (int i = 0; i < numArms; i++) {
            arms.add(new Arm(i, "arm_" + i));
        }
        Log.i(TAG, "MultiArmedBandit: arms=" + numArms + " algo=" + algorithm);
    }

    /** Constructor with custom arm names. */
    public MultiArmedBandit(String[] armNames, Algorithm algorithm) {
        this(armNames.length, algorithm, 7L);
        for (int i = 0; i < armNames.length && i < numArms; i++) {
            arms.get(i).name = armNames[i];
        }
    }

    public MultiArmedBandit(int numArms, Algorithm algorithm) {
        this(numArms, algorithm, 7L);
    }

    // -------------------------------------------------------------------------
    // Core API
    // -------------------------------------------------------------------------

    /**
     * Select the next arm to pull.
     * @return Selected arm index [0, numArms).
     */
    public synchronized int selectArm() {
        int chosen;
        switch (algorithm) {
            case UCB1:          chosen = selectUCB1();       break;
            case UCB1_TUNED:    chosen = selectUCB1Tuned();  break;
            case THOMPSON_SAMPLING: chosen = selectThompson(); break;
            case EXP3:          chosen = selectEXP3();       break;
            case EPSILON_GREEDY:
            default:            chosen = selectEpsilonGreedy(); break;
        }
        lastChosenArm = chosen;
        totalPulls.incrementAndGet();
        return chosen;
    }

    /**
     * Feed back the reward obtained by pulling the last selected arm.
     *
     * @param armIndex The arm that was pulled.
     * @param reward   Observed reward (should be in [0,1] for Thompson/EXP3).
     */
    public synchronized void update(int armIndex, double reward) {
        if (armIndex < 0 || armIndex >= numArms) return;
        Arm arm   = arms.get(armIndex);
        arm.pulls++;
        arm.totalReward   += reward;
        arm.totalSqReward += reward * reward;

        // Thompson update
        if (reward >= 0.5) { arm.alpha++; } else { arm.beta++; }

        // EXP3 weight update
        if (algorithm == Algorithm.EXP3) {
            double[] probs = exp3Probs();
            double xHat = reward / Math.max(probs[armIndex], 1e-10);
            arm.weight *= Math.exp(gamma3 * xHat / numArms);
        }

        // Epsilon decay
        if (algorithm == Algorithm.EPSILON_GREEDY) {
            epsilon = Math.max(epsilonMin, epsilon * epsilonDecay);
        }

        // Regret tracking vs. best empirical arm
        double bestMean = 0;
        for (Arm a : arms) bestMean = Math.max(bestMean, a.mean());
        cumulativeRegret += bestMean - reward;
    }

    // -------------------------------------------------------------------------
    // Algorithm implementations
    // -------------------------------------------------------------------------

    private int selectEpsilonGreedy() {
        if (rng.nextDouble() < epsilon) return rng.nextInt(numArms);
        return argmax();
    }

    private int selectUCB1() {
        int t = Math.max(1, totalPulls.get());
        double best = Double.NEGATIVE_INFINITY;
        int bestArm = 0;
        for (int i = 0; i < numArms; i++) {
            Arm a = arms.get(i);
            if (a.pulls == 0) return i; // try all arms at least once
            double score = a.mean() + Math.sqrt(2.0 * Math.log(t) / a.pulls);
            if (score > best) { best = score; bestArm = i; }
        }
        return bestArm;
    }

    private int selectUCB1Tuned() {
        int t = Math.max(1, totalPulls.get());
        double best = Double.NEGATIVE_INFINITY;
        int bestArm = 0;
        for (int i = 0; i < numArms; i++) {
            Arm a = arms.get(i);
            if (a.pulls == 0) return i;
            double logT  = Math.log(t);
            double V     = a.variance() + Math.sqrt(2.0 * logT / a.pulls);
            double score = a.mean() + Math.sqrt(logT / a.pulls * Math.min(0.25, V));
            if (score > best) { best = score; bestArm = i; }
        }
        return bestArm;
    }

    private int selectThompson() {
        double best = Double.NEGATIVE_INFINITY;
        int bestArm = 0;
        for (int i = 0; i < numArms; i++) {
            double sample = betaSample(arms.get(i).alpha, arms.get(i).beta);
            if (sample > best) { best = sample; bestArm = i; }
        }
        return bestArm;
    }

    private int selectEXP3() {
        double[] probs = exp3Probs();
        double r = rng.nextDouble();
        double cum = 0;
        for (int i = 0; i < numArms - 1; i++) {
            cum += probs[i];
            if (r <= cum) return i;
        }
        return numArms - 1;
    }

    private double[] exp3Probs() {
        double totalW = 0;
        for (Arm a : arms) totalW += a.weight;
        double[] probs = new double[numArms];
        for (int i = 0; i < numArms; i++) {
            probs[i] = (1 - gamma3) * arms.get(i).weight / totalW + gamma3 / numArms;
        }
        return probs;
    }

    private int argmax() {
        double bestMean = Double.NEGATIVE_INFINITY;
        int    bestArm  = 0;
        for (int i = 0; i < numArms; i++) {
            if (arms.get(i).mean() > bestMean) {
                bestMean = arms.get(i).mean();
                bestArm  = i;
            }
        }
        return bestArm;
    }

    // -------------------------------------------------------------------------
    // Monitoring
    // -------------------------------------------------------------------------

    public String getArmName(int index) {
        return (index >= 0 && index < numArms) ? arms.get(index).name : "unknown";
    }

    public double getCumulativeRegret() { return cumulativeRegret; }
    public int    getTotalPulls()       { return totalPulls.get(); }

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("algorithm",         algorithm.name());
        s.put("numArms",           numArms);
        s.put("totalPulls",        totalPulls.get());
        s.put("cumulativeRegret",  cumulativeRegret);
        s.put("lastChosenArm",     lastChosenArm);
        s.put("epsilon",           epsilon);

        List<Map<String, Object>> armStats = new ArrayList<>();
        for (Arm a : arms) {
            Map<String, Object> as = new HashMap<>();
            as.put("id",     a.id);
            as.put("name",   a.name);
            as.put("pulls",  a.pulls);
            as.put("mean",   a.mean());
            as.put("alpha",  a.alpha);
            as.put("beta",   a.beta);
            armStats.add(as);
        }
        s.put("arms", armStats);

        // Best arm by mean reward
        int   bestIdx = argmax();
        s.put("bestArm",     bestIdx);
        s.put("bestArmName", arms.get(bestIdx).name);
        s.put("bestMean",    arms.get(bestIdx).mean());
        return s;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

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
}
