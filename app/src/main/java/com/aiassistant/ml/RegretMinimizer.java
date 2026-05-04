package com.aiassistant.ml;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RegretMinimizer — online learning with regret minimization guarantees.
 *
 * Implements three classic no-regret algorithms for sequential decision making:
 *
 *   HEDGE (Exponential Weights):
 *     w_i ← w_i · exp(-η · loss_i)
 *     p_i = w_i / Σw_j
 *     Regret bound: O(√(T log K))
 *
 *   FOLLOW_THE_REGULARIZED_LEADER (FTRL):
 *     π_t = argmin_p { Σ_{τ<t} L_τ · p + R(p)/η }
 *     With entropy regularisation: p_i ∝ exp(-η · Σ_τ L_{τ,i})
 *
 *   REGRET_MATCHING (RM+):
 *     Used in CFR (Counterfactual Regret Minimisation) for games.
 *     Tracks counterfactual regret per action; distributes probability
 *     proportionally to positive regrets.
 *
 * Use cases:
 *   - Selecting exploration strategies (action selectors).
 *   - Online hyperparameter selection.
 *   - Game-theoretic reasoning (approximate Nash equilibria).
 *   - Meta-learning: which algorithm to use next step.
 *
 * Thread-safe.
 */
public class RegretMinimizer {

    private static final String TAG = "RegretMinimizer";

    public enum Algorithm { HEDGE, FTRL, REGRET_MATCHING }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int       numActions;
    private final Algorithm algo;
    private final float     eta;         // learning rate / temperature

    // HEDGE: weights
    private final float[] weights;
    private final float[] cumulLoss;    // for FTRL

    // RM+: cumulative regrets
    private final float[] cumRegret;
    private final float[] cumStrategy;  // for average strategy

    // Statistics
    private final AtomicInteger stepCount    = new AtomicInteger(0);
    private float avgLoss       = 0f;
    private float avgRegret     = 0f;
    private float[] lastProbs;

    private final Random rng = new Random(269L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public RegretMinimizer(int numActions, Algorithm algo, float eta) {
        this.numActions = numActions;
        this.algo       = algo;
        this.eta        = eta;

        weights      = new float[numActions]; java.util.Arrays.fill(weights, 1f / numActions);
        cumulLoss    = new float[numActions];
        cumRegret    = new float[numActions];
        cumStrategy  = new float[numActions];
        lastProbs    = new float[numActions]; java.util.Arrays.fill(lastProbs, 1f/numActions);

        Log.i(TAG, "RegretMinimizer: K=" + numActions + " algo=" + algo + " η=" + eta);
    }

    public RegretMinimizer(int numActions, Algorithm algo) {
        this(numActions, algo, (float) Math.sqrt(Math.log(numActions) / 1000.0));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /** Get current mixed strategy (probability distribution over actions). */
    public synchronized float[] getStrategy() {
        return lastProbs.clone();
    }

    /**
     * Sample action according to current mixed strategy.
     */
    public synchronized int sampleAction() {
        float r = rng.nextFloat(), cum = 0;
        for (int a = 0; a < numActions - 1; a++) {
            cum += lastProbs[a]; if (r < cum) return a;
        }
        return numActions - 1;
    }

    /**
     * Observe losses and update strategy.
     *
     * @param losses float[numActions] — loss for each action this round.
     *               For reward: pass negative reward as loss.
     */
    public synchronized void update(float[] losses) {
        if (losses.length < numActions) return;
        int t = stepCount.incrementAndGet();

        switch (algo) {
            case HEDGE:    updateHedge(losses); break;
            case FTRL:     updateFTRL(losses);  break;
            case REGRET_MATCHING: updateRM(losses, t); break;
        }

        // Track average loss
        float expLoss = 0;
        for (int a = 0; a < numActions; a++) expLoss += lastProbs[a] * losses[a];
        avgLoss   = 0.99f * avgLoss + 0.01f * expLoss;
        float minLoss = losses[0]; for (float l : losses) if (l < minLoss) minLoss = l;
        avgRegret = 0.99f * avgRegret + 0.01f * (expLoss - minLoss);
    }

    /**
     * Convenience: update with reward (pass negative reward as loss).
     * @param rewards float[numActions]
     */
    public synchronized void updateRewards(float[] rewards) {
        float[] losses = new float[numActions];
        for (int a = 0; a < numActions; a++) losses[a] = -rewards[a];
        update(losses);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Algorithm implementations
    // ─────────────────────────────────────────────────────────────────────────

    private void updateHedge(float[] losses) {
        float sum = 0;
        for (int a = 0; a < numActions; a++) {
            weights[a] *= (float) Math.exp(-eta * losses[a]);
            weights[a]  = Math.max(weights[a], 1e-10f);
            sum        += weights[a];
        }
        for (int a = 0; a < numActions; a++) lastProbs[a] = weights[a] / sum;
    }

    private void updateFTRL(float[] losses) {
        for (int a = 0; a < numActions; a++) cumulLoss[a] += losses[a];
        // p_a ∝ exp(-η · cumLoss_a)
        float max = cumulLoss[0]; for (float l : cumulLoss) if (l < max) max = l;
        float sum = 0;
        for (int a = 0; a < numActions; a++) {
            lastProbs[a] = (float) Math.exp(-eta * (cumulLoss[a] - max));
            sum += lastProbs[a];
        }
        for (int a = 0; a < numActions; a++) lastProbs[a] /= sum;
    }

    private void updateRM(float[] losses, int t) {
        // Expected loss under current strategy
        float expLoss = 0;
        for (int a = 0; a < numActions; a++) expLoss += lastProbs[a] * losses[a];

        // Counterfactual regrets
        for (int a = 0; a < numActions; a++) {
            float regret = expLoss - losses[a];   // positive = wish we had played a
            cumRegret[a] = Math.max(0, cumRegret[a] + regret);  // RM+: clip to 0
        }

        // Update strategy: proportional to positive regrets
        float sumPos = 0; for (float r : cumRegret) sumPos += r;
        if (sumPos > 0) {
            for (int a = 0; a < numActions; a++) lastProbs[a] = cumRegret[a] / sumPos;
        } else {
            java.util.Arrays.fill(lastProbs, 1f / numActions);
        }

        // Track average strategy (for Nash approximation)
        for (int a = 0; a < numActions; a++) cumStrategy[a] += lastProbs[a];
    }

    /** Average strategy (RM+ converges to Nash in two-player zero-sum). */
    public synchronized float[] averageStrategy() {
        int t = stepCount.get();
        if (t == 0) return lastProbs.clone();
        float[] avg = cumStrategy.clone();
        float sum = 0; for (float v : avg) sum += v;
        if (sum > 0) for (int a = 0; a < numActions; a++) avg[a] /= sum;
        return avg;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("algorithm",  algo.name());
        s.put("numActions", numActions);
        s.put("stepCount",  stepCount.get());
        s.put("avgLoss",    avgLoss);
        s.put("avgRegret",  avgRegret);
        // Entropy of current strategy
        float H = 0;
        for (float p : lastProbs) if (p > 1e-8f) H -= p*(float)Math.log(p);
        s.put("strategyEntropy", H);
        s.put("currentStrategy", lastProbs.clone());
        return s;
    }
}
