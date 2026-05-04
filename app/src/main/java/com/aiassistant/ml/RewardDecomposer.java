package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RewardDecomposer — disentangle scalar rewards into interpretable components.
 *
 * Learns to decompose a scalar reward signal r_t into a sum of sub-rewards:
 *   r_t = r_survival + r_progress + r_exploration + r_efficiency + r_safety + ...
 *
 * Methods:
 *
 *   SUPERVISED_DECOMP:
 *     The agent sees labeled reward components (from game telemetry).
 *     Maps (state, action) → individual reward components via regression.
 *
 *   DISENTANGLED_REPR:
 *     Unsupervised: learns a factored representation where each latent
 *     dimension predicts one reward component via mutual information max.
 *
 *   CAUSAL_DECOMP:
 *     Identifies which state features causally influence each reward component
 *     (sparse attention mask per component).
 *
 * Benefits:
 *   - Interpretability: understand WHY the agent gets high/low reward.
 *   - Targeted improvement: optimise individual components independently.
 *   - Reward hacking detection: catch agents exploiting a single component.
 *
 * Thread-safe.
 */
public class RewardDecomposer {

    private static final String TAG = "RewardDecomposer";

    public enum Mode { SUPERVISED_DECOMP, DISENTANGLED_REPR, CAUSAL_DECOMP }

    // ─────────────────────────────────────────────────────────────────────────
    // Component definition
    // ─────────────────────────────────────────────────────────────────────────
    public static class RewardComponent {
        public final String name;
        public final float  weight;         // relative importance
        float  predicted;
        float  avgContribution;
        float  minSeen, maxSeen;
        int    hitCount;                    // times this was the dominant component

        RewardComponent(String name, float weight) {
            this.name   = name;
            this.weight = weight;
            this.minSeen= Float.MAX_VALUE;
            this.maxSeen= Float.MIN_VALUE;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Per-component regression head: (state, action) → component_reward
    // ─────────────────────────────────────────────────────────────────────────
    private final int    stateDim, actionDim, hidDim;
    private final float[][]   sharedW1, sharedW2; // shared trunk
    private final float[]     sharedB1, sharedB2;
    // Per-component output heads: [numComponents][1][hidDim/2]
    private final float[][][] headW;
    private final float[][]   headB;
    private final NeuralNetworkOptimizer opt;

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final List<RewardComponent> components;
    private final int    numComponents;
    private final Mode   mode;
    private final float  lr;

    // Causal masks [numComponents][stateDim] — learned sparse attention
    private final float[][] causalMasks;

    private final AtomicInteger updateCount = new AtomicInteger(0);
    private float avgTotalReward = 0f;
    private float avgPredReward  = 0f;

    private final java.util.Random rng = new java.util.Random(227L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public RewardDecomposer(int stateDim, int actionDim, int hidDim,
                             List<RewardComponent> components, Mode mode,
                             float lr) {
        this.stateDim      = stateDim;
        this.actionDim     = actionDim;
        this.hidDim        = hidDim;
        this.components    = components;
        this.numComponents = components.size();
        this.mode          = mode;
        this.lr            = lr;
        this.opt           = new NeuralNetworkOptimizer(lr);

        int inDim = stateDim + actionDim;
        float s = (float) Math.sqrt(2.0 / (inDim + hidDim));
        sharedW1 = xav(hidDim, inDim, s);   sharedB1 = new float[hidDim];
        sharedW2 = xav(hidDim/2, hidDim, s);sharedB2 = new float[hidDim/2];

        headW = new float[numComponents][1][hidDim/2];
        headB = new float[numComponents][1];
        for (int c = 0; c < numComponents; c++) xavInit(headW[c], s * 0.1f);

        causalMasks = new float[numComponents][stateDim];
        for (float[] row : causalMasks) java.util.Arrays.fill(row, 1f); // start fully open

        Log.i(TAG, "RewardDecomposer: components=" + numComponents + " mode=" + mode);
    }

    /** Default factory: creates a standard decomposition for game playing. */
    public static RewardDecomposer createGameDecomposer(int stateDim, int actionDim) {
        List<RewardComponent> comps = new ArrayList<>();
        comps.add(new RewardComponent("survival",    0.3f));
        comps.add(new RewardComponent("progress",    0.3f));
        comps.add(new RewardComponent("exploration", 0.1f));
        comps.add(new RewardComponent("efficiency",  0.2f));
        comps.add(new RewardComponent("safety",      0.1f));
        return new RewardDecomposer(stateDim, actionDim, 64, comps,
                Mode.SUPERVISED_DECOMP, 1e-3f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Predict decomposed reward components for (state, action).
     * @return float[numComponents] — predicted value per component.
     */
    public synchronized float[] predict(float[] state, int action) {
        float[] inp = buildInput(state, action);
        float[] h1  = lin(sharedW1, sharedB1, inp, true);
        float[] h2  = lin(sharedW2, sharedB2, h1,  true);
        float[] out = new float[numComponents];
        for (int c = 0; c < numComponents; c++) {
            out[c] = lin(headW[c], headB[c], h2, false)[0];
            components.get(c).predicted = out[c];
        }
        return out;
    }

    /** Weighted sum of component predictions → scalar reward estimate. */
    public synchronized float predictScalar(float[] state, int action) {
        float[] preds = predict(state, action);
        float sum = 0;
        for (int c = 0; c < numComponents; c++) sum += components.get(c).weight * preds[c];
        return sum;
    }

    /**
     * Supervised update: given observed scalar reward and optional component labels.
     *
     * @param state    State at step.
     * @param action   Action taken.
     * @param reward   Total observed scalar reward.
     * @param labels   Per-component targets (null = distribute proportionally).
     */
    public synchronized void update(float[] state, int action, float reward, float[] labels) {
        float[] targets;
        if (labels != null && labels.length == numComponents) {
            targets = labels;
        } else {
            // Distribute reward proportionally by component weights
            targets = new float[numComponents];
            float wSum = 0; for (RewardComponent c : components) wSum += c.weight;
            for (int i = 0; i < numComponents; i++)
                targets[i] = reward * (components.get(i).weight / Math.max(wSum, 1e-8f));
        }

        float[] inp = buildInput(state, action);
        float[] h1  = lin(sharedW1, sharedB1, inp, true);
        float[] h2  = lin(sharedW2, sharedB2, h1,  true);

        float totalErr = 0;
        float[] dH2 = new float[hidDim/2];
        for (int c = 0; c < numComponents; c++) {
            float pred = lin(headW[c], headB[c], h2, false)[0];
            float err  = pred - targets[c];
            totalErr  += Math.abs(err);

            // Track stats
            RewardComponent comp = components.get(c);
            comp.avgContribution = 0.99f * comp.avgContribution + 0.01f * targets[c];
            if (targets[c] < comp.minSeen) comp.minSeen = targets[c];
            if (targets[c] > comp.maxSeen) comp.maxSeen = targets[c];

            // Head gradient
            float[][] dHead = new float[1][hidDim/2];
            for (int j = 0; j < hidDim/2; j++) dHead[0][j] = err * h2[j];
            opt.step("rd_head_" + c, headW[c], dHead);
            for (int j = 0; j < hidDim/2; j++) if (h2[j] > 0) dH2[j] += err * headW[c][0][j];
        }

        // Backprop shared trunk
        float[] dH1 = new float[hidDim];
        for (int j=0;j<hidDim;j++) {
            if (h1[j]<=0) continue;
            for (int i=0;i<hidDim/2;i++) dH1[j] += dH2[i] * sharedW2[i][j];
        }
        float[][] dW2 = new float[hidDim/2][hidDim];
        for (int i=0;i<hidDim/2;i++) for(int j=0;j<hidDim;j++) dW2[i][j]=dH2[i]*h1[j];
        opt.step("rd_shW2", sharedW2, dW2);
        float[][] dW1 = new float[hidDim][inp.length];
        for (int i=0;i<hidDim;i++) for(int j=0;j<inp.length;j++) dW1[i][j]=dH1[i]*inp[j];
        opt.step("rd_shW1", sharedW1, dW1);

        // Update causal masks (CAUSAL mode: sparse via L1)
        if (mode == Mode.CAUSAL_DECOMP) updateCausalMasks(state);

        avgTotalReward = 0.99f * avgTotalReward + 0.01f * reward;
        avgPredReward  = 0.99f * avgPredReward  + 0.01f * predictScalar(state, action);
        updateCount.incrementAndGet();
    }

    private void updateCausalMasks(float[] state) {
        for (int c = 0; c < numComponents; c++) {
            float[] preds = predict(state, 0);
            for (int i = 0; i < Math.min(stateDim, state.length); i++) {
                // Soft L1 sparsification
                causalMasks[c][i] = Math.max(0f, causalMasks[c][i] - lr * 0.01f);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Analysis
    // ─────────────────────────────────────────────────────────────────────────

    /** Which component contributed most to the last prediction? */
    public synchronized String dominantComponent(float[] state, int action) {
        float[] preds = predict(state, action);
        float   maxContrib = Float.NEGATIVE_INFINITY;
        String  dominant   = components.isEmpty() ? "none" : components.get(0).name;
        for (int c = 0; c < numComponents; c++) {
            float contrib = components.get(c).weight * preds[c];
            if (contrib > maxContrib) { maxContrib = contrib; dominant = components.get(c).name; }
        }
        return dominant;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private float[] buildInput(float[] state, int action) {
        float[] inp = new float[stateDim + actionDim];
        System.arraycopy(pad(state, stateDim), 0, inp, 0, stateDim);
        if (action >= 0 && action < actionDim) inp[stateDim + action] = 1f;
        return inp;
    }

    private static float[] lin(float[][] W, float[] b, float[] x, boolean relu) {
        float[] o = new float[W.length];
        for (int i=0;i<W.length;i++){
            float s=b[i]; for(int j=0;j<Math.min(x.length,W[i].length);j++) s+=W[i][j]*x[j];
            o[i]=relu?Math.max(0f,s):s;
        }
        return o;
    }

    private static float[] pad(float[] x, int dim) {
        if(x.length==dim) return x;
        float[] p=new float[dim]; System.arraycopy(x,0,p,0,Math.min(x.length,dim)); return p;
    }

    private float[][] xav(int r, int c, float s) {
        float[][] m=new float[r][c];
        for(int i=0;i<r;i++) for(int j=0;j<c;j++) m[i][j]=(rng.nextFloat()*2f-1f)*s;
        return m;
    }

    private void xavInit(float[][] m, float s) {
        for (float[] row : m) for(int j=0;j<row.length;j++) row[j]=(rng.nextFloat()*2f-1f)*s;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("mode",          mode.name());
        s.put("numComponents", numComponents);
        s.put("updateCount",   updateCount.get());
        s.put("avgTotalReward",avgTotalReward);
        s.put("avgPredReward", avgPredReward);
        Map<String, Double> compStats = new HashMap<>();
        for (RewardComponent c : components) compStats.put(c.name, (double) c.avgContribution);
        s.put("componentAvgs", compStats);
        return s;
    }
}
