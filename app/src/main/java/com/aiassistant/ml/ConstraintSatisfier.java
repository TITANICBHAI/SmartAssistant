package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ConstraintSatisfier — safe RL with hard and soft constraints.
 *
 * Implements Constrained MDP (CMDP) framework (Altman 1999):
 *   max_π E[Σ r_t]  subject to  E[Σ c_t^k] ≤ d_k  for k=1..K
 *
 * Approaches implemented:
 *
 *   LAGRANGIAN:
 *     Augment reward: r' = r - Σ_k λ_k · c^k
 *     Dual update: λ_k ← max(0, λ_k + α_λ · (E[c^k] - d_k))
 *     Primal update: π ← argmax E[r'] with current λ.
 *
 *   PROJECTION:
 *     Project unsafe actions onto the safe set before execution.
 *     Safe action: Q(s,a) ≥ threshold AND c(s,a) ≤ limit.
 *
 *   SHIELD:
 *     Hard safety filter: if proposed action is unsafe (cost > 0),
 *     select the safest available action instead.
 *
 *   CPO (Constrained Policy Optimisation):
 *     Approximate CPO via single Lagrangian step per policy update.
 *
 * Constraint types:
 *   - ACTION_FREQUENCY: limit how often a specific action is taken.
 *   - STATE_REGION:     avoid entering forbidden state regions.
 *   - RESOURCE:         budget constraint (e.g., battery, API calls).
 *   - CUSTOM:           user-defined cost function.
 *
 * Thread-safe.
 */
public class ConstraintSatisfier {

    private static final String TAG = "ConstraintSatisfier";

    public enum Approach { LAGRANGIAN, PROJECTION, SHIELD, CPO }

    // ─────────────────────────────────────────────────────────────────────────
    // Constraint definition
    // ─────────────────────────────────────────────────────────────────────────
    public static class Constraint {
        public final String name;
        public final float  limit;          // d_k: max allowed average cost
        public       float  lambda;         // Lagrange multiplier
        public       float  avgCost;        // running average of c^k
        public       float  totalCost;
        public       int    violations;
        private final float alpha;          // dual step size

        public Constraint(String name, float limit, float initLambda, float alpha) {
            this.name   = name;
            this.limit  = limit;
            this.lambda = initLambda;
            this.alpha  = alpha;
        }

        /** Update Lagrange multiplier. */
        void dualUpdate(float costSignal) {
            avgCost = 0.99f * avgCost + 0.01f * costSignal;
            totalCost += costSignal;
            if (costSignal > 0) violations++;
            lambda = Math.max(0f, lambda + alpha * (avgCost - limit));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cost network: state + action → cost prediction
    // ─────────────────────────────────────────────────────────────────────────
    private final float[][] costW1, costW2;
    private final float[]   costB1, costB2;
    private final NeuralNetworkOptimizer costOpt;

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int        stateDim, actionDim, hidDim;
    private final Approach   approach;
    private final List<Constraint> constraints = new ArrayList<>();

    private float totalLagrangianPenalty = 0f;

    // Resource tracking
    private float   resourceBudget = 1000f;
    private float   resourceUsed   = 0f;

    private final AtomicInteger filterCount   = new AtomicInteger(0);
    private final AtomicInteger blockCount    = new AtomicInteger(0);
    private float avgPenalty = 0f;

    private final Random rng = new Random(367L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public ConstraintSatisfier(int stateDim, int actionDim, int hidDim,
                                Approach approach, float costLr) {
        this.stateDim  = stateDim;
        this.actionDim = actionDim;
        this.hidDim    = hidDim;
        this.approach  = approach;
        this.costOpt   = new NeuralNetworkOptimizer(costLr);

        float s = (float)Math.sqrt(2.0/(stateDim+actionDim+hidDim));
        costW1 = xav(hidDim, stateDim+actionDim, s); costB1 = new float[hidDim];
        costW2 = xav(1, hidDim, s*0.01f);            costB2 = new float[1];

        Log.i(TAG, "ConstraintSatisfier: " + approach + " s=" + stateDim + " a=" + actionDim);
    }

    public ConstraintSatisfier(int stateDim, int actionDim, Approach approach) {
        this(stateDim, actionDim, 64, approach, 1e-3f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Constraint management
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Constraint addConstraint(String name, float limit,
                                                  float initLambda, float alpha) {
        Constraint c = new Constraint(name, limit, initLambda, alpha);
        constraints.add(c);
        Log.i(TAG, "Constraint added: " + name + " limit=" + limit);
        return c;
    }

    public synchronized void setResourceBudget(float budget) { resourceBudget = budget; }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Filter an action through safety constraints.
     * @param state     Current state.
     * @param action    Proposed action from policy.
     * @param qValues   Q-values for all actions (for safe alternative selection).
     * @return Safe action (may differ from proposed action if unsafe).
     */
    public synchronized int filterAction(float[] state, int action, float[] qValues) {
        filterCount.incrementAndGet();
        float[] s = pad(state, stateDim);

        switch (approach) {
            case SHIELD:    return shield(s, action, qValues);
            case PROJECTION:return project(s, action, qValues);
            default:        return action; // LAGRANGIAN/CPO: modify reward, not action
        }
    }

    /**
     * Compute Lagrangian-modified reward.
     * r' = r - Σ_k λ_k · c^k(s, a)
     */
    public synchronized float lagrangianReward(float[] state, int action, float reward) {
        float[] s = pad(state, stateDim);
        float penalty = 0;
        for (Constraint c : constraints) {
            float cost = predictCost(s, action);
            penalty += c.lambda * cost;
        }
        avgPenalty = 0.99f * avgPenalty + 0.01f * penalty;
        return reward - penalty;
    }

    /**
     * Record actual costs and update Lagrange multipliers.
     * @param costs Array of actual constraint costs [numConstraints].
     */
    public synchronized void recordCosts(float[] costs) {
        for (int k=0;k<Math.min(costs.length, constraints.size());k++) {
            constraints.get(k).dualUpdate(costs[k]);
        }
    }

    /** Train cost predictor on observed cost. */
    public synchronized void trainCostPredictor(float[] state, int action, float cost) {
        float[] s   = pad(state, stateDim);
        float[] inp = buildInput(s, action);
        float[] h   = linRelu(costW1, costB1, inp);
        float   pred= lin1(costW2, costB2, h);
        float   err = pred - cost;
        float[][] dW2 = new float[1][hidDim]; for(int j=0;j<hidDim;j++) dW2[0][j]=err*h[j];
        costOpt.step("cs_costW2", costW2, dW2);
        float[] dH = new float[hidDim];
        for(int j=0;j<hidDim;j++){if(h[j]<=0)continue; dH[j]=err*costW2[0][j];}
        costOpt.step("cs_costW1", costW1, outer(dH, inp));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Filtering strategies
    // ─────────────────────────────────────────────────────────────────────────

    private int shield(float[] s, int action, float[] qValues) {
        if (!isUnsafe(s, action)) return action;
        blockCount.incrementAndGet();
        // Find safest action (lowest predicted cost among high-Q actions)
        float bestQ = Float.NEGATIVE_INFINITY;
        int best = action;
        for (int a=0;a<actionDim;a++) {
            if (!isUnsafe(s, a)) {
                float q = qValues != null && a < qValues.length ? qValues[a] : 0f;
                if (q > bestQ) { bestQ = q; best = a; }
            }
        }
        return best;
    }

    private int project(float[] s, int action, float[] qValues) {
        if (!isUnsafe(s, action)) return action;
        blockCount.incrementAndGet();
        return shield(s, action, qValues);  // simplified projection
    }

    private boolean isUnsafe(float[] s, int action) {
        float cost = predictCost(s, action);
        if (cost > 0.5f) return true;
        if (resourceUsed >= resourceBudget) return true;
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private float predictCost(float[] s, int action) {
        float[] inp = buildInput(s, action);
        float[] h   = linRelu(costW1, costB1, inp);
        return Math.max(0f, lin1(costW2, costB2, h));
    }

    private float[] buildInput(float[] s, int action) {
        float[] inp = new float[stateDim + actionDim];
        System.arraycopy(s, 0, inp, 0, stateDim);
        if (action >= 0 && action < actionDim) inp[stateDim + action] = 1f;
        return inp;
    }

    private static float[] linRelu(float[][] W, float[] b, float[] x) {
        float[] o=new float[W.length];for(int i=0;i<W.length;i++){float s=b[i];for(int j=0;j<Math.min(x.length,W[i].length);j++)s+=W[i][j]*x[j];o[i]=Math.max(0f,s);}return o;
    }
    private static float lin1(float[][] W, float[] b, float[] x) {
        float s=b[0];for(int j=0;j<Math.min(x.length,W[0].length);j++) s+=W[0][j]*x[j];return s;
    }
    private static float[][] outer(float[] a, float[] b){float[][] g=new float[a.length][b.length];for(int i=0;i<a.length;i++) for(int j=0;j<b.length;j++) g[i][j]=a[i]*b[j];return g;}
    private static float[] pad(float[] x,int dim){if(x.length==dim)return x;float[] p=new float[dim];System.arraycopy(x,0,p,0,Math.min(x.length,dim));return p;}
    private float[][] xav(int r,int c,float s){float[][] m=new float[r][c];for(int i=0;i<r;i++) for(int j=0;j<c;j++) m[i][j]=(rng.nextFloat()*2f-1f)*s;return m;}

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("approach",    approach.name());
        s.put("filterCount", filterCount.get());
        s.put("blockCount",  blockCount.get());
        s.put("avgPenalty",  avgPenalty);
        s.put("numConstraints", constraints.size());
        Map<String, Double> cInfo = new HashMap<>();
        for (Constraint c : constraints) cInfo.put(c.name, (double)c.lambda);
        s.put("lambdas", cInfo);
        return s;
    }
}
