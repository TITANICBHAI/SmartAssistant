package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PolicyEnsemble — committee of policies for robust action selection.
 *
 * Maintains K policy networks with different random initialisations.
 * Combines their votes using:
 *
 *   MAJORITY_VOTE:    Most common action across all policies.
 *   WEIGHTED_VOTE:    Weight each policy by its recent performance.
 *   SOFT_ENSEMBLE:    Average action probability distributions, then argmax.
 *   UNCERTAINTY:      Select action with lowest disagreement across policies.
 *   UCBE:             Upper confidence bound on ensemble predictions.
 *
 * Benefits:
 *   - Reduced variance in action selection.
 *   - Natural uncertainty estimation (disagreement between policies).
 *   - Better exploration: policies disagree on uncertain regions.
 *   - Robust to individual policy failures.
 *
 * Each member policy is a lightweight 2-layer MLP.
 * Thread-safe.
 */
public class PolicyEnsemble {

    private static final String TAG = "PolicyEnsemble";

    public enum AggregationMode { MAJORITY_VOTE, WEIGHTED_VOTE, SOFT_ENSEMBLE,
                                  UNCERTAINTY, UCBE }

    // ─────────────────────────────────────────────────────────────────────────
    // Member policy
    // ─────────────────────────────────────────────────────────────────────────
    private static class MemberPolicy {
        final int id;
        float[][] W1, W2;
        float[]   b1, b2;
        float     weight  = 1f;
        float     avgReward = 0f;
        int       steps   = 0;

        MemberPolicy(int id, int stateDim, int actionDim, int hidDim, Random rng) {
            this.id = id;
            float s = (float) Math.sqrt(2.0 / (stateDim + hidDim));
            W1 = xav(hidDim, stateDim, s, rng); b1 = new float[hidDim];
            W2 = xav(actionDim, hidDim, s * 0.1f, rng); b2 = new float[actionDim];
        }

        float[] actionProbs(float[] state) {
            float[] h = linRelu(W1, b1, state);
            return softmax(lin(W2, b2, h));
        }

        static float[] linRelu(float[][] W, float[] b, float[] x) {
            float[] o=new float[W.length];for(int i=0;i<W.length;i++){float s=b[i];for(int j=0;j<Math.min(x.length,W[i].length);j++)s+=W[i][j]*x[j];o[i]=Math.max(0f,s);}return o;
        }
        static float[] lin(float[][] W, float[] b, float[] x) {
            float[] o=new float[W.length];for(int i=0;i<W.length;i++){float s=b[i];for(int j=0;j<Math.min(x.length,W[i].length);j++)s+=W[i][j]*x[j];o[i]=s;}return o;
        }
        static float[] softmax(float[] v){float mx=v[0];for(float x:v)if(x>mx)mx=x;float sum=0;float[] o=new float[v.length];for(int i=0;i<v.length;i++){o[i]=(float)Math.exp(v[i]-mx);sum+=o[i];}for(int i=0;i<v.length;i++)o[i]/=sum;return o;}
        static float[][] xav(int r,int c,float s,Random rng){float[][] m=new float[r][c];for(int i=0;i<r;i++) for(int j=0;j<c;j++) m[i][j]=(rng.nextFloat()*2f-1f)*s;return m;}
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final List<MemberPolicy>  members;
    private final int    K;
    private final int    stateDim, actionDim, hidDim;
    private final AggregationMode mode;
    private final float  ucbC;
    private final NeuralNetworkOptimizer opt;

    private final AtomicInteger selectCount  = new AtomicInteger(0);
    private final AtomicInteger updateCount  = new AtomicInteger(0);
    private float avgAgreement = 0f;

    private final Random rng = new Random(317L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public PolicyEnsemble(int K, int stateDim, int actionDim, int hidDim,
                           AggregationMode mode, float ucbC, float lr) {
        this.K         = K;
        this.stateDim  = stateDim;
        this.actionDim = actionDim;
        this.hidDim    = hidDim;
        this.mode      = mode;
        this.ucbC      = ucbC;
        this.opt       = new NeuralNetworkOptimizer(lr);

        members = new ArrayList<>(K);
        for (int k=0;k<K;k++) members.add(new MemberPolicy(k, stateDim, actionDim, hidDim, rng));

        Log.i(TAG, "PolicyEnsemble: K=" + K + " mode=" + mode + " s=" + stateDim + " a=" + actionDim);
    }

    public PolicyEnsemble(int K, int stateDim, int actionDim) {
        this(K, stateDim, actionDim, 64, AggregationMode.SOFT_ENSEMBLE, 1f, 1e-3f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Action selection
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized int selectAction(float[] state) {
        float[] s = pad(state, stateDim);
        int action;
        switch (mode) {
            case MAJORITY_VOTE: action = majorityVote(s); break;
            case WEIGHTED_VOTE: action = weightedVote(s); break;
            case UNCERTAINTY:   action = uncertaintyAction(s); break;
            case UCBE:          action = ucbeAction(s); break;
            case SOFT_ENSEMBLE:
            default:            action = softEnsemble(s); break;
        }
        selectCount.incrementAndGet();
        return action;
    }

    /** Get ensemble averaged action probabilities. */
    public synchronized float[] ensembleProbs(float[] state) {
        float[] s = pad(state, stateDim);
        float[] avg = new float[actionDim];
        float totalW = 0;
        for (MemberPolicy m : members) {
            float[] p = m.actionProbs(s);
            for (int a=0;a<actionDim;a++) avg[a] += m.weight * p[a];
            totalW += m.weight;
        }
        for (int a=0;a<actionDim;a++) avg[a] /= totalW;
        return avg;
    }

    /** Uncertainty: entropy of ensemble averaged distribution. */
    public synchronized float uncertainty(float[] state) {
        float[] probs = ensembleProbs(state);
        float H = 0; for (float p : probs) if (p > 1e-8f) H -= p*(float)Math.log(p);
        return H;
    }

    /** Disagreement: average pairwise KL divergence between member distributions. */
    public synchronized float disagreement(float[] state) {
        float[] s = pad(state, stateDim);
        float[] avg = ensembleProbs(s);
        float kl = 0;
        for (MemberPolicy m : members) {
            float[] p = m.actionProbs(s);
            for (int a=0;a<actionDim;a++) if (avg[a]>1e-8f && p[a]>1e-8f)
                kl += p[a] * (float)(Math.log(p[a]) - Math.log(avg[a]));
        }
        avgAgreement = 0.99f * avgAgreement + 0.01f * (1f - Math.min(1f, kl / K));
        return kl / K;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Training
    // ─────────────────────────────────────────────────────────────────────────

    /** Train all ensemble members on a transition (behaviour cloning style). */
    public synchronized void trainAll(float[] state, int action, float reward) {
        float[] s = pad(state, stateDim);
        for (int k=0;k<K;k++) {
            // Train only a random subset to diversify (bagging effect)
            if (K > 1 && rng.nextFloat() > 0.7f) continue;
            MemberPolicy m = members.get(k);
            float[] h  = MemberPolicy.linRelu(m.W1, m.b1, s);
            float[] pr = MemberPolicy.softmax(MemberPolicy.lin(m.W2, m.b2, h));
            int a = Math.min(action, actionDim - 1);
            float[] dL = pr.clone(); dL[a] -= 1f;
            // Backprop W2
            float[][] dW2 = new float[actionDim][hidDim];
            for (int i=0;i<actionDim;i++) for(int j=0;j<hidDim;j++) dW2[i][j]=dL[i]*h[j];
            opt.step("pe_W2_" + k, m.W2, dW2);
            float[] dH = new float[hidDim];
            for(int j=0;j<hidDim;j++){if(h[j]<=0)continue;for(int i=0;i<actionDim;i++)dH[j]+=dL[i]*m.W2[i][j];}
            float[][] dW1 = new float[hidDim][stateDim];
            for(int i=0;i<hidDim;i++) for(int j=0;j<stateDim;j++) dW1[i][j]=dH[i]*s[j];
            opt.step("pe_W1_" + k, m.W1, dW1);
            m.avgReward = 0.99f * m.avgReward + 0.01f * reward;
            m.steps++;
            m.weight = (float) Math.exp(m.avgReward);
        }
        updateWeight();
        updateCount.incrementAndGet();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Aggregation
    // ─────────────────────────────────────────────────────────────────────────

    private int majorityVote(float[] s) {
        int[] votes = new int[actionDim];
        for (MemberPolicy m : members) votes[argmax(m.actionProbs(s))]++;
        return argmax(votes);
    }

    private int weightedVote(float[] s) {
        float[] weighted = new float[actionDim];
        for (MemberPolicy m : members) {
            float[] p = m.actionProbs(s);
            for (int a=0;a<actionDim;a++) weighted[a] += m.weight * p[a];
        }
        return argmax(weighted);
    }

    private int softEnsemble(float[] s) { return argmax(ensembleProbs(s)); }

    private int uncertaintyAction(float[] s) {
        // Choose action with lowest per-action variance across members
        float[] variance = new float[actionDim];
        float[] mean = ensembleProbs(s);
        for (MemberPolicy m : members) {
            float[] p = m.actionProbs(s);
            for (int a=0;a<actionDim;a++) { float d=p[a]-mean[a]; variance[a]+=d*d; }
        }
        int best=0; for(int a=1;a<actionDim;a++) if(variance[a]<variance[best]) best=a;
        return best;
    }

    private int ucbeAction(float[] s) {
        float[] avg = ensembleProbs(s);
        float[] ucb = new float[actionDim];
        for (int a=0;a<actionDim;a++) {
            float var=0;
            for (MemberPolicy m : members){float d=m.actionProbs(s)[a]-avg[a];var+=d*d;}
            ucb[a] = avg[a] + ucbC * (float)Math.sqrt(var / K);
        }
        return argmax(ucb);
    }

    private void updateWeight() {
        float total = 0;
        for (MemberPolicy m : members) total += m.weight;
        if (total > 0) for (MemberPolicy m : members) m.weight /= total;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static int argmax(float[] v){int b=0;for(int i=1;i<v.length;i++)if(v[i]>v[b])b=i;return b;}
    private static int argmax(int[] v){int b=0;for(int i=1;i<v.length;i++)if(v[i]>v[b])b=i;return b;}
    private static float[] pad(float[] x,int dim){if(x.length==dim)return x;float[] p=new float[dim];System.arraycopy(x,0,p,0,Math.min(x.length,dim));return p;}

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("K",            K);
        s.put("mode",         mode.name());
        s.put("selectCount",  selectCount.get());
        s.put("updateCount",  updateCount.get());
        s.put("avgAgreement", avgAgreement);
        return s;
    }
}
