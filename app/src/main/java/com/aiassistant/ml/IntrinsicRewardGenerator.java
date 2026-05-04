package com.aiassistant.ml;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * IntrinsicRewardGenerator — composite intrinsic motivation system.
 *
 * Generates bonus intrinsic rewards to drive exploration and learning:
 *
 *   CURIOSITY (ICM-style):
 *     r_i = η · ||f(s, a) - s'||²   (surprise / prediction error)
 *
 *   NOVELTY (count-based):
 *     r_i = β / √N(s)   (inverse visit count — need StateHasher)
 *
 *   EMPOWERMENT:
 *     r_i = mutual information between actions and future states
 *     (simplified: variance of predicted next states over random actions)
 *
 *   PROGRESS:
 *     r_i = Δ learning_rate — reward for improvement in world model
 *
 *   DISAGREEMENT (ensemble):
 *     r_i = variance of ensemble predictions — uncertainty bonus
 *
 *   ENTROPY:
 *     r_i = H(π(·|s)) — entropy of policy distribution
 *
 * Rewards are combined as a weighted sum and normalised via running stats.
 *
 * Thread-safe.
 */
public class IntrinsicRewardGenerator {

    private static final String TAG = "IntrinsicReward";

    public enum Component {
        CURIOSITY, NOVELTY, EMPOWERMENT, PROGRESS, DISAGREEMENT, ENTROPY
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int    stateDim, actionDim;
    private final float[] weights;   // per-component weight

    // Curiosity model: predicts s_{t+1} from (s_t, a_t)
    private final float[][] curW1, curW2;
    private final float[]   curB1, curB2;
    private final NeuralNetworkOptimizer curOpt;

    // Running stats for normalisation
    private final double[] compMean, compM2;
    private final long[]   compN;

    // Progress: track learning error EMA
    private float prevCuriosityLoss = 0f;
    private float curCuriosityLoss  = 0f;

    // Visit counts (simplified: hash → count)
    private final int[] visitBuckets;
    private static final int NUM_BUCKETS = 4096;

    private final AtomicInteger stepCount = new AtomicInteger(0);
    private float[] lastComponents;
    private float lastIntrinsic = 0f;

    private final Random rng = new Random(331L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public IntrinsicRewardGenerator(int stateDim, int actionDim, float[] weights,
                                     float curLr) {
        this.stateDim  = stateDim;
        this.actionDim = actionDim;
        this.weights   = weights.length == Component.values().length ? weights : defaultWeights();

        float s = (float) Math.sqrt(2.0 / (stateDim + actionDim));
        int hid = 64;
        curW1 = xav(hid, stateDim + actionDim, s); curB1 = new float[hid];
        curW2 = xav(stateDim, hid, s * 0.01f);     curB2 = new float[stateDim];
        curOpt = new NeuralNetworkOptimizer(curLr);

        int n = Component.values().length;
        compMean = new double[n]; compM2 = new double[n]; compN = new long[n];
        visitBuckets = new int[NUM_BUCKETS];
        lastComponents = new float[n];

        Log.i(TAG, "IntrinsicRewardGenerator: state=" + stateDim + " actions=" + actionDim);
    }

    public IntrinsicRewardGenerator(int stateDim, int actionDim) {
        this(stateDim, actionDim, defaultWeights(), 3e-4f);
    }

    private static float[] defaultWeights() {
        return new float[]{0.3f, 0.2f, 0.1f, 0.2f, 0.1f, 0.1f};
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Compute composite intrinsic reward for a transition.
     * Also trains the curiosity model on this transition.
     *
     * @param state     s_t
     * @param action    a_t
     * @param nextState s_{t+1}
     * @param policyProbs π(·|s_t) for entropy bonus
     * @return intrinsic reward scalar
     */
    public synchronized float compute(float[] state, int action, float[] nextState,
                                       float[] policyProbs) {
        float[] s  = pad(state,     stateDim);
        float[] sp = pad(nextState, stateDim);
        float[] inp = buildInput(s, action);

        float total = 0;
        Component[] comps = Component.values();

        for (int c = 0; c < comps.length; c++) {
            float r = 0;
            switch (comps[c]) {
                case CURIOSITY:     r = curiosityReward(inp, sp);   break;
                case NOVELTY:       r = noveltyReward(s);           break;
                case PROGRESS:      r = progressReward();           break;
                case DISAGREEMENT:  r = disagreementReward(s, action); break;
                case ENTROPY:       r = entropyReward(policyProbs); break;
                case EMPOWERMENT:   r = empowermentReward(s);       break;
            }
            r = normalise(r, c);
            lastComponents[c] = r;
            total += weights[c] * r;
        }

        // Train curiosity model
        trainCuriosity(inp, sp);
        lastIntrinsic = total;
        stepCount.incrementAndGet();
        return total;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Component implementations
    // ─────────────────────────────────────────────────────────────────────────

    private float curiosityReward(float[] inp, float[] nextState) {
        float[] pred = lin(curW2, curB2, lin(curW1, curB1, inp, true), false);
        float err = 0;
        for (int i=0;i<stateDim;i++) { float d=pred[i]-nextState[i]; err+=d*d; }
        curCuriosityLoss = 0.99f * curCuriosityLoss + 0.01f * err;
        return err;
    }

    private void trainCuriosity(float[] inp, float[] target) {
        float[] h    = lin(curW1, curB1, inp, true);
        float[] pred = lin(curW2, curB2, h,   false);
        float[] err  = new float[stateDim];
        for (int i=0;i<stateDim;i++) err[i] = 2f*(pred[i]-target[i]);
        float[][] dW2 = outer(err, h);
        curOpt.step("ir_curW2", curW2, dW2);
        float[] dH = new float[curW1.length];
        for(int j=0;j<dH.length;j++){if(h[j]<=0)continue;for(int i=0;i<stateDim;i++) dH[j]+=err[i]*curW2[i][j];}
        curOpt.step("ir_curW1", curW1, outer(dH, inp));
    }

    private float noveltyReward(float[] s) {
        int bucket = stateHash(s) % NUM_BUCKETS;
        visitBuckets[bucket]++;
        return 1f / (float) Math.sqrt(visitBuckets[bucket]);
    }

    private float progressReward() {
        float progress = prevCuriosityLoss - curCuriosityLoss;
        prevCuriosityLoss = curCuriosityLoss;
        return Math.max(0f, progress);
    }

    private float disagreementReward(float[] s, int action) {
        float[] inp = buildInput(s, action);
        float[] h = lin(curW1, curB1, inp, true);
        float var = 0;
        for (float v : h) { float d = v - 0.5f; var += d*d; }
        return var / h.length;
    }

    private float entropyReward(float[] probs) {
        if (probs == null || probs.length == 0) return 0f;
        float H = 0;
        for (float p : probs) if (p > 1e-8f) H -= p * (float) Math.log(p);
        return H;
    }

    private float empowermentReward(float[] s) {
        // Simplified: variance of predicted next states over K random actions
        float var = 0;
        float[] mean = new float[stateDim];
        int K = Math.min(4, actionDim);
        float[][] preds = new float[K][];
        for (int k=0;k<K;k++) {
            preds[k] = lin(curW2, curB2, lin(curW1, curB1, buildInput(s, k), true), false);
            for (int i=0;i<stateDim;i++) mean[i] += preds[k][i];
        }
        for (int i=0;i<stateDim;i++) mean[i] /= K;
        for (float[] p : preds) for(int i=0;i<stateDim;i++){float d=p[i]-mean[i];var+=d*d;}
        return var / (K * stateDim);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private float normalise(float r, int c) {
        compN[c]++;
        double delta = r - compMean[c];
        compMean[c] += delta / compN[c];
        compM2[c]   += delta * (r - compMean[c]);
        double std = compN[c] < 2 ? 1.0 : Math.sqrt(compM2[c]/(compN[c]-1)+1e-8);
        return (float)((r - compMean[c]) / std);
    }

    private float[] buildInput(float[] s, int action) {
        float[] inp = new float[stateDim + actionDim];
        System.arraycopy(s, 0, inp, 0, stateDim);
        if (action >= 0 && action < actionDim) inp[stateDim + action] = 1f;
        return inp;
    }

    private static int stateHash(float[] s) {
        int h = 0;
        for (float v : s) h = (int)(h * 31 + Float.floatToIntBits(v));
        return Math.abs(h);
    }

    private static float[] lin(float[][] W, float[] b, float[] x, boolean relu) {
        float[] o=new float[W.length];for(int i=0;i<W.length;i++){float s=b[i];for(int j=0;j<Math.min(x.length,W[i].length);j++)s+=W[i][j]*x[j];o[i]=relu?Math.max(0f,s):s;}return o;
    }
    private static float[][] outer(float[] a, float[] b){float[][] g=new float[a.length][b.length];for(int i=0;i<a.length;i++) for(int j=0;j<b.length;j++) g[i][j]=a[i]*b[j];return g;}
    private static float[] pad(float[] x,int dim){if(x.length==dim)return x;float[] p=new float[dim];System.arraycopy(x,0,p,0,Math.min(x.length,dim));return p;}
    private float[][] xav(int r,int c,float s){float[][] m=new float[r][c];for(int i=0;i<r;i++) for(int j=0;j<c;j++) m[i][j]=(rng.nextFloat()*2f-1f)*s;return m;}

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("stepCount",    stepCount.get());
        s.put("lastIntrinsic",lastIntrinsic);
        Component[] comps = Component.values();
        for (int c=0;c<comps.length;c++) s.put(comps[c].name().toLowerCase(), lastComponents[c]);
        s.put("curLoss",      curCuriosityLoss);
        return s;
    }
}
