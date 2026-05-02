package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AdaptiveExplorationStrategy — dynamically switches between exploration policies
 * (ε-greedy, UCB1, Boltzmann/softmax, Thompson Sampling) based on observed
 * performance, learning progress, and game context.
 *
 * Algorithm overview:
 *   1. Each exploration method is itself treated as an arm of a bandit problem.
 *   2. UCB1 scores are computed over the meta-bandit to select which exploration
 *      strategy to use for the current step.
 *   3. After each episode, performance feedback (episode return) is fed back to
 *      update the meta-bandit's running reward statistics.
 *   4. Configurable "exploration budget" forces uniform strategy sampling for the
 *      first N steps to ensure all strategies are tried.
 *
 * Supported base strategies:
 *   - EPSILON_GREEDY  — classic ε-greedy with linear decay
 *   - UCB1            — upper confidence bound on per-action reward estimates
 *   - BOLTZMANN       — softmax with temperature τ (decaying)
 *   - THOMPSON        — Beta-distribution Thompson Sampling per action
 *   - RANDOM          — pure random (baseline)
 */
public class AdaptiveExplorationStrategy {

    private static final String TAG = "AdaptiveExplorationStrategy";

    // -------------------------------------------------------------------------
    // Strategy enum
    // -------------------------------------------------------------------------
    public enum Strategy {
        EPSILON_GREEDY, UCB1, BOLTZMANN, THOMPSON, RANDOM
    }

    // -------------------------------------------------------------------------
    // Per-strategy meta-bandit state
    // -------------------------------------------------------------------------
    private static class StrategyArm {
        final Strategy strategy;
        double totalReward  = 0.0;
        int    pullCount    = 0;
        double meanReward   = 0.0;

        StrategyArm(Strategy s) { this.strategy = s; }

        void update(double reward) {
            pullCount++;
            totalReward += reward;
            meanReward   = totalReward / pullCount;
        }

        /** UCB1 score for meta-selection. */
        double ucb1Score(int totalPulls, double explorationC) {
            if (pullCount == 0) return Double.MAX_VALUE;
            return meanReward + explorationC * Math.sqrt(Math.log(totalPulls) / pullCount);
        }
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final int      actionDim;
    private final int      warmupSteps;    // Force uniform meta-sampling for this many episodes
    private final double   metaC;          // UCB1 exploration coefficient for meta-bandit

    // Base strategy parameters
    private double epsilon;         // ε-greedy current epsilon
    private final double epsilonMin;
    private final double epsilonDecay;
    private double temperature;     // Boltzmann current temperature
    private final double tempMin;
    private final double tempDecay;

    // Per-action statistics for UCB1 and Thompson
    private final int[]    actionCounts;   // times each action was chosen
    private final double[] actionRewards;  // cumulative reward per action
    private final double[] alphaThompson;  // Beta α for Thompson
    private final double[] betaThompson;   // Beta β for Thompson

    // Meta-bandit arms
    private final List<StrategyArm> arms = new ArrayList<>();
    private final AtomicInteger     totalPulls = new AtomicInteger(0);
    private final AtomicInteger     episodeCount = new AtomicInteger(0);

    private Strategy currentStrategy = Strategy.EPSILON_GREEDY;
    private final Random rng;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    public AdaptiveExplorationStrategy(int actionDim, int warmupSteps,
                                        double metaC, long seed) {
        this.actionDim    = actionDim;
        this.warmupSteps  = warmupSteps;
        this.metaC        = metaC;
        this.rng          = new Random(seed);

        this.epsilon      = 1.0;
        this.epsilonMin   = 0.05;
        this.epsilonDecay = 0.995;
        this.temperature  = 1.0;
        this.tempMin      = 0.1;
        this.tempDecay    = 0.998;

        this.actionCounts  = new int[actionDim];
        this.actionRewards = new double[actionDim];
        this.alphaThompson = new double[actionDim];
        this.betaThompson  = new double[actionDim];
        for (int i = 0; i < actionDim; i++) {
            alphaThompson[i] = 1.0;   // uniform Beta(1,1) prior
            betaThompson[i]  = 1.0;
        }

        // Register all strategy arms
        for (Strategy s : Strategy.values()) arms.add(new StrategyArm(s));
    }

    public AdaptiveExplorationStrategy(int actionDim) {
        this(actionDim, 5, 1.0, 42L);
    }

    // -------------------------------------------------------------------------
    // Core API
    // -------------------------------------------------------------------------

    /**
     * Select an action given Q-value estimates.
     *
     * @param qValues Q(s,·) estimates; length must be ≥ actionDim.
     * @return Chosen action index.
     */
    public synchronized int selectAction(float[] qValues) {
        // Pick strategy via meta-bandit
        currentStrategy = selectMetaStrategy();
        int action;
        switch (currentStrategy) {
            case UCB1:       action = ucb1Action(qValues);       break;
            case BOLTZMANN:  action = boltzmannAction(qValues);  break;
            case THOMPSON:   action = thompsonAction(qValues);   break;
            case RANDOM:     action = rng.nextInt(actionDim);    break;
            case EPSILON_GREEDY:
            default:         action = epsilonGreedyAction(qValues); break;
        }
        actionCounts[action]++;
        totalPulls.incrementAndGet();
        return action;
    }

    /**
     * Feed back reward for the last action to update per-action statistics.
     *
     * @param action Chosen action.
     * @param reward Observed reward.
     * @param success True when the action achieved its goal (for Thompson).
     */
    public synchronized void recordActionOutcome(int action, float reward, boolean success) {
        if (action < 0 || action >= actionDim) return;
        actionRewards[action] += reward;
        // Thompson Bayesian update
        if (success) {
            alphaThompson[action]++;
        } else {
            betaThompson[action]++;
        }
    }

    /**
     * Called at the end of each episode to update the meta-bandit and decay parameters.
     *
     * @param episodeReturn Total undiscounted return for the episode.
     */
    public synchronized void onEpisodeEnd(double episodeReturn) {
        // Update the arm for the strategy used in this episode
        for (StrategyArm arm : arms) {
            if (arm.strategy == currentStrategy) {
                arm.update(episodeReturn);
                break;
            }
        }
        // Decay ε and temperature
        epsilon     = Math.max(epsilonMin,  epsilon     * epsilonDecay);
        temperature = Math.max(tempMin,     temperature * tempDecay);
        episodeCount.incrementAndGet();

        if (episodeCount.get() % 20 == 0) {
            logMetaBanditState();
        }
    }

    // -------------------------------------------------------------------------
    // Strategy implementations
    // -------------------------------------------------------------------------

    private int epsilonGreedyAction(float[] qValues) {
        if (rng.nextDouble() < epsilon) return rng.nextInt(actionDim);
        return argmax(qValues);
    }

    private int ucb1Action(float[] qValues) {
        int t = totalPulls.get() + 1;
        double bestScore = Double.NEGATIVE_INFINITY;
        int    bestAction = 0;
        for (int a = 0; a < actionDim; a++) {
            double q = qValues.length > a ? qValues[a] : 0.0;
            double bonus = actionCounts[a] == 0
                    ? Double.MAX_VALUE
                    : Math.sqrt(2.0 * Math.log(t) / actionCounts[a]);
            double score = q + bonus;
            if (score > bestScore) { bestScore = score; bestAction = a; }
        }
        return bestAction;
    }

    private int boltzmannAction(float[] qValues) {
        double[] probs = new double[actionDim];
        double max = Double.NEGATIVE_INFINITY;
        for (int a = 0; a < actionDim; a++) if (qValues[a] > max) max = qValues[a];
        double sum = 0.0;
        for (int a = 0; a < actionDim; a++) {
            probs[a] = Math.exp((qValues[a] - max) / Math.max(temperature, 1e-6));
            sum += probs[a];
        }
        double r   = rng.nextDouble() * sum;
        double cum = 0.0;
        for (int a = 0; a < actionDim - 1; a++) {
            cum += probs[a];
            if (r <= cum) return a;
        }
        return actionDim - 1;
    }

    private int thompsonAction(float[] qValues) {
        double bestSample = Double.NEGATIVE_INFINITY;
        int    bestAction = 0;
        for (int a = 0; a < actionDim; a++) {
            // Sample from Beta(α, β) using the ratio of two Gamma samples (Marsaglia)
            double sample = betaSample(alphaThompson[a], betaThompson[a]);
            // Blend with Q-value for better stability
            double score  = 0.7 * sample + 0.3 * (qValues.length > a ? qValues[a] : 0.0);
            if (score > bestSample) { bestSample = score; bestAction = a; }
        }
        return bestAction;
    }

    // -------------------------------------------------------------------------
    // Meta-bandit selection
    // -------------------------------------------------------------------------

    private Strategy selectMetaStrategy() {
        int ep = episodeCount.get();
        // Warm-up: cycle through strategies uniformly
        if (ep < warmupSteps * arms.size()) {
            return arms.get(ep % arms.size()).strategy;
        }
        // UCB1 over strategy arms
        int totalMeta = 0;
        for (StrategyArm arm : arms) totalMeta += arm.pullCount;
        totalMeta = Math.max(1, totalMeta);

        double bestScore = Double.NEGATIVE_INFINITY;
        Strategy best   = Strategy.EPSILON_GREEDY;
        for (StrategyArm arm : arms) {
            double score = arm.ucb1Score(totalMeta, metaC);
            if (score > bestScore) { bestScore = score; best = arm.strategy; }
        }
        return best;
    }

    // -------------------------------------------------------------------------
    // Monitoring
    // -------------------------------------------------------------------------

    public Strategy getCurrentStrategy() { return currentStrategy; }
    public double   getCurrentEpsilon()  { return epsilon; }
    public double   getCurrentTemp()     { return temperature; }

    public Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("currentStrategy", currentStrategy.name());
        s.put("epsilon",         epsilon);
        s.put("temperature",     temperature);
        s.put("totalPulls",      totalPulls.get());
        s.put("episodeCount",    episodeCount.get());
        Map<String, Object> metaStats = new HashMap<>();
        for (StrategyArm arm : arms) {
            Map<String, Object> as = new HashMap<>();
            as.put("pulls",      arm.pullCount);
            as.put("meanReward", arm.meanReward);
            metaStats.put(arm.strategy.name(), as);
        }
        s.put("metaBandit", metaStats);
        return s;
    }

    private void logMetaBanditState() {
        StringBuilder sb = new StringBuilder("Meta-bandit state: ");
        for (StrategyArm arm : arms) {
            sb.append(arm.strategy.name()).append("(pulls=").append(arm.pullCount)
              .append(",r=").append(String.format("%.2f", arm.meanReward)).append(") ");
        }
        Log.d(TAG, sb.toString());
    }

    // -------------------------------------------------------------------------
    // Math helpers
    // -------------------------------------------------------------------------

    private static int argmax(float[] v) {
        int best = 0;
        for (int i = 1; i < v.length; i++) if (v[i] > v[best]) best = i;
        return best;
    }

    /** Sample from Beta(a,b) via ratio of two Gamma samples (Marsaglia & Tsang, 2000). */
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
            if (u < 1.0 - 0.0331 * (x * x) * (x * x)) return d * v;
            if (Math.log(u) < 0.5 * x * x + d * (1.0 - v + Math.log(v))) return d * v;
        }
    }
}
