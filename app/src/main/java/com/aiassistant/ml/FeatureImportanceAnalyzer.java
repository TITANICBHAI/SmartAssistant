package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * FeatureImportanceAnalyzer — identifies which state features matter most to the agent.
 *
 * Provides interpretability for the learned policy by estimating
 * how much each input feature dimension contributes to action selection.
 *
 * Methods:
 *
 *   PERMUTATION_IMPORTANCE:
 *     Shuffle feature j across a batch → measure drop in Q-value accuracy.
 *     importance_j = E[|Q(s,a) - Q(s_j_shuffled, a)|]
 *
 *   GRADIENT_SENSITIVITY:
 *     Compute ∂Q(s,a) / ∂s_j via finite differences.
 *     Importance = mean absolute gradient over the state distribution.
 *
 *   SALIENCY_MAP:
 *     For each state, compute |∂Q/∂s| → visual saliency of input features.
 *
 *   SHAPLEY_VALUES (approximated):
 *     Monte Carlo SHAP: average marginal contribution over random feature coalitions.
 *     Most theoretically principled but expensive.
 *
 *   MUTUAL_INFORMATION:
 *     I(s_j; A*) = Σ P(s_j, A*) log [P(s_j, A*) / P(s_j)P(A*)]
 *     where A* = argmax Q(s, ·).
 *
 * Thread-safe.
 */
public class FeatureImportanceAnalyzer {

    private static final String TAG = "FeatureImportance";

    public enum Method { PERMUTATION, GRADIENT_SENSITIVITY, SALIENCY, SHAPLEY, MUTUAL_INFO }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int    stateDim, actionDim;
    private final Method method;
    private final int    numSamples;   // for MC methods

    // Running importance estimates
    private final float[] importance;         // [stateDim] — current estimate
    private final float[] importanceEma;      // exponential moving average
    private final double[] mutuInfoAccum;     // for MI method

    // State sample bank (for permutation / SHAP)
    private final List<float[]> stateSamples = new ArrayList<>();
    private final List<Integer> actionSamples= new ArrayList<>();
    private static final int MAX_SAMPLES     = 2048;

    // Action count per feature bucket (for MI)
    private final int[][][] miCounts;        // [stateDim][numBins][actionDim]
    private static final int MI_BINS = 8;

    private final AtomicInteger analysisCount = new AtomicInteger(0);
    private float[] lastImportance = null;

    private final Random rng = new Random(419L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public FeatureImportanceAnalyzer(int stateDim, int actionDim,
                                      Method method, int numSamples) {
        this.stateDim   = stateDim;
        this.actionDim  = actionDim;
        this.method     = method;
        this.numSamples = numSamples;

        importance     = new float[stateDim];
        importanceEma  = new float[stateDim];
        mutuInfoAccum  = new double[stateDim];
        miCounts       = new int[stateDim][MI_BINS][actionDim];
        lastImportance = new float[stateDim];

        Log.i(TAG, "FeatureImportanceAnalyzer: dim=" + stateDim + " method=" + method);
    }

    public FeatureImportanceAnalyzer(int stateDim, int actionDim) {
        this(stateDim, actionDim, Method.GRADIENT_SENSITIVITY, 512);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Record a (state, action) observation for later analysis.
     */
    public synchronized void observe(float[] state, int action) {
        float[] s = pad(state, stateDim);
        if (stateSamples.size() < MAX_SAMPLES) {
            stateSamples.add(s.clone());
            actionSamples.add(Math.min(action, actionDim-1));
        } else {
            int idx = rng.nextInt(MAX_SAMPLES);
            stateSamples.set(idx, s.clone());
            actionSamples.set(idx, Math.min(action, actionDim-1));
        }
        // Update MI counts
        for (int j=0;j<stateDim;j++) {
            int bin = Math.max(0, Math.min(MI_BINS-1, (int)((s[j]+3f)/6f * MI_BINS)));
            miCounts[j][bin][Math.min(action, actionDim-1)]++;
        }
    }

    /**
     * Run importance analysis using the provided Q-function proxy.
     * @param qProxy A simple closure-equivalent: we use DQNAgent for Q values.
     * @return Importance scores [stateDim], higher = more important.
     */
    public synchronized float[] analyze(DQNAgent qProxy) {
        float[] scores;
        switch (method) {
            case PERMUTATION:        scores = permutationImportance(qProxy); break;
            case GRADIENT_SENSITIVITY: scores = gradientSensitivity(qProxy); break;
            case SHAPLEY:            scores = shapleyValues(qProxy);         break;
            case MUTUAL_INFO:        scores = mutualInfo();                  break;
            case SALIENCY:
            default:                 scores = saliencyMap(qProxy);           break;
        }
        for (int j=0;j<stateDim;j++) {
            importance[j]    = scores[j];
            importanceEma[j] = 0.9f * importanceEma[j] + 0.1f * scores[j];
        }
        lastImportance = scores.clone();
        analysisCount.incrementAndGet();
        return scores;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Importance methods
    // ─────────────────────────────────────────────────────────────────────────

    private float[] permutationImportance(DQNAgent q) {
        float[] scores = new float[stateDim];
        int N = Math.min(numSamples, stateSamples.size());
        if (N == 0) return scores;

        // Baseline Q-values
        float[] baseQ = new float[N];
        for (int i=0;i<N;i++) {
            float[] Q = q.getQValues(stateSamples.get(i));
            if (Q != null) baseQ[i] = max(Q);
        }

        for (int j=0;j<stateDim;j++) {
            float drop = 0;
            for (int i=0;i<N;i++) {
                // Shuffle feature j: replace with random sample
                float[] s = stateSamples.get(i).clone();
                s[j] = stateSamples.get(rng.nextInt(N))[j];
                float[] Q = q.getQValues(s);
                float   dQ = Q != null ? Math.abs(max(Q) - baseQ[i]) : 0f;
                drop += dQ;
            }
            scores[j] = drop / N;
        }
        return normalise(scores);
    }

    private float[] gradientSensitivity(DQNAgent q) {
        float[] scores = new float[stateDim];
        int N = Math.min(numSamples, stateSamples.size());
        if (N == 0) return scores;
        float eps = 1e-3f;
        for (int i=0;i<N;i++) {
            float[] s = stateSamples.get(i);
            float[] Q = q.getQValues(s); if(Q==null)continue;
            float baseVal = max(Q);
            for (int j=0;j<stateDim;j++) {
                float[] sp = s.clone(); sp[j] += eps;
                float[] Qp = q.getQValues(sp); if(Qp==null)continue;
                scores[j] += Math.abs(max(Qp)-baseVal)/eps;
            }
        }
        for (int j=0;j<stateDim;j++) scores[j] /= N;
        return normalise(scores);
    }

    private float[] saliencyMap(DQNAgent q) {
        return gradientSensitivity(q);  // same computation
    }

    private float[] shapleyValues(DQNAgent q) {
        float[] scores = new float[stateDim];
        int N = Math.min(64, stateSamples.size());
        if (N == 0) return scores;
        // MC SHAP: for each feature j, average marginal contribution
        for (int j=0;j<stateDim;j++) {
            float shap = 0;
            int iter = Math.min(numSamples, 50);
            for (int t=0;t<iter;t++) {
                float[] s   = stateSamples.get(rng.nextInt(N));
                float[] ref = stateSamples.get(rng.nextInt(N));
                // Random subset of features
                float[] with    = s.clone();
                float[] without = s.clone();
                for (int k=0;k<stateDim;k++) {
                    if (k==j) continue;
                    if (rng.nextFloat() < 0.5f) { with[k]=ref[k]; without[k]=ref[k]; }
                }
                without[j] = ref[j];
                float[] Qw  = q.getQValues(with);    if(Qw==null)  continue;
                float[] Qwo = q.getQValues(without); if(Qwo==null) continue;
                shap += max(Qw) - max(Qwo);
            }
            scores[j] = shap / iter;
        }
        return normalise(scores);
    }

    private float[] mutualInfo() {
        float[] scores = new float[stateDim];
        // Total action distribution
        int[] totalActionCount = new int[actionDim];
        for (int a : actionSamples) totalActionCount[Math.min(a,actionDim-1)]++;
        int total = actionSamples.size();
        if (total == 0) return scores;

        for (int j=0;j<stateDim;j++) {
            double mi = 0;
            for (int b=0;b<MI_BINS;b++) {
                int binTotal = 0; for (int a=0;a<actionDim;a++) binTotal+=miCounts[j][b][a];
                if (binTotal == 0) continue;
                double pBin = (double)binTotal/total;
                for (int a=0;a<actionDim;a++) {
                    if (miCounts[j][b][a]==0) continue;
                    double pJoint = (double)miCounts[j][b][a]/total;
                    double pA     = (double)totalActionCount[a]/total;
                    if (pJoint>0 && pBin>0 && pA>0)
                        mi += pJoint * Math.log(pJoint/(pBin*pA));
                }
            }
            scores[j] = (float)mi;
        }
        return normalise(scores);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Queries
    // ─────────────────────────────────────────────────────────────────────────

    /** Get top-K most important feature indices. */
    public synchronized int[] topKFeatures(int K) {
        float[] imp = importanceEma.clone();
        int[] indices = new int[stateDim];
        for(int i=0;i<stateDim;i++) indices[i]=i;
        // Partial sort (top-K)
        for(int k=0;k<Math.min(K,stateDim);k++){
            int maxIdx=k;
            for(int i=k+1;i<stateDim;i++) if(imp[indices[i]]>imp[indices[maxIdx]]) maxIdx=i;
            int tmp=indices[k]; indices[k]=indices[maxIdx]; indices[maxIdx]=tmp;
        }
        int[] top=new int[Math.min(K,stateDim)];
        System.arraycopy(indices,0,top,0,top.length);
        return top;
    }

    public synchronized float[] getImportanceEma() { return importanceEma.clone(); }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static float[] normalise(float[] v) {
        float max=0; for(float x:v) if(Math.abs(x)>max) max=Math.abs(x);
        if(max<1e-8f) return v;
        float[] n=new float[v.length]; for(int i=0;i<v.length;i++) n[i]=v[i]/max;
        return n;
    }

    private static float max(float[] v){float m=v[0];for(float x:v)if(x>m)m=x;return m;}
    private static float[] pad(float[] x,int dim){if(x.length==dim)return x;float[] p=new float[dim];System.arraycopy(x,0,p,0,Math.min(x.length,dim));return p;}

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("method",       method.name());
        s.put("analysisCount",analysisCount.get());
        s.put("sampleCount",  stateSamples.size());
        if (lastImportance != null) {
            float max=0; int topJ=0;
            for(int j=0;j<stateDim;j++) if(lastImportance[j]>max){max=lastImportance[j];topJ=j;}
            s.put("topFeature",   topJ);
            s.put("topImportance",max);
        }
        return s;
    }
}
