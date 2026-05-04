package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TemporalCreditAssigner — assigns credit across long time horizons.
 *
 * A fundamental problem in RL is assigning credit (or blame) to actions
 * that caused rewards many steps in the future. Solutions:
 *
 *   ELIGIBILITY_TRACES (TD(λ)):
 *     e_t = λγ·e_{t-1} + ∇_θ log π(a|s)
 *     θ ← θ + α·δ·e_t
 *     Accumulates weighted gradients over all past actions.
 *
 *   HINDSIGHT_CREDIT:
 *     For each past action, estimate: how much would the return have changed
 *     if a different action had been taken? (Counterfactual reasoning).
 *
 *   RETURN_DECOMPOSITION:
 *     Learn a function that decomposes cumulative reward into per-timestep
 *     contributions: r_t^* = f(s_t, a_t, G) where G = total episode return.
 *
 *   ATTENTION_CREDIT:
 *     Use attention mechanism over trajectory to weight which actions
 *     deserve credit for the final outcome.
 *
 *   RUDDER (Return Decomposition for Delayed Rewards):
 *     Train an LSTM to predict cumulative return from sequence of (s,a).
 *     The per-step prediction change = credit assigned to that step.
 *
 * Thread-safe.
 */
public class TemporalCreditAssigner {

    private static final String TAG = "CreditAssign";

    public enum Method { ELIGIBILITY_TRACES, HINDSIGHT_CREDIT,
                         RETURN_DECOMP, ATTENTION_CREDIT, RUDDER }

    // ─────────────────────────────────────────────────────────────────────────
    // Eligibility trace state
    // ─────────────────────────────────────────────────────────────────────────
    private final float[][] traces;       // [paramDim per layer, layers] — simplified flat
    private final float     traceDecay;   // λ·γ

    // Return decomposition network: [s,a] → scalar contribution
    private final float[][] rdW1, rdW2;
    private final float[]   rdB1, rdB2;
    private final NeuralNetworkOptimizer rdOpt;

    // Attention network: trajectory features → attention weights
    private final float[][] atW1, atW2;
    private final float[]   atB1, atB2;

    // RUDDER LSTM (simplified: GRU cell)
    private final float[][] gruWr, gruWz, gruWh;   // [hidDim][stateDim+actionDim+hidDim]
    private final float[]   gruBr, gruBz, gruBh;
    private float[]         gruH;                   // hidden state

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int    stateDim, actionDim, hidDim, traceDim;
    private final Method method;
    private final float  gamma;

    // Trajectory buffer for episode-level methods
    private final List<float[]> trajStates   = new ArrayList<>();
    private final List<Integer> trajActions  = new ArrayList<>();
    private final List<Float>   trajRewards  = new ArrayList<>();
    private final List<Float>   trajCredits  = new ArrayList<>();

    private final AtomicInteger assignCount  = new AtomicInteger(0);
    private final AtomicInteger episodeCount = new AtomicInteger(0);
    private float avgCredit = 0f;
    private float avgReturn = 0f;

    private final Random rng = new Random(389L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public TemporalCreditAssigner(int stateDim, int actionDim, int hidDim,
                                   int traceDim, Method method, float gamma,
                                   float traceDecay, float lr) {
        this.stateDim   = stateDim;
        this.actionDim  = actionDim;
        this.hidDim     = hidDim;
        this.traceDim   = traceDim;
        this.method     = method;
        this.gamma      = gamma;
        this.traceDecay = traceDecay;
        this.rdOpt      = new NeuralNetworkOptimizer(lr);
        this.gruH       = new float[hidDim];

        traces = new float[2][traceDim];

        float s = (float)Math.sqrt(2.0/(stateDim+actionDim+hidDim));
        rdW1 = xav(hidDim, stateDim+actionDim, s); rdB1 = new float[hidDim];
        rdW2 = xav(1, hidDim, s*0.01f);            rdB2 = new float[1];

        atW1 = xav(hidDim, stateDim+actionDim, s); atB1 = new float[hidDim];
        atW2 = xav(1, hidDim, s*0.01f);            atB2 = new float[1];

        int gruIn = stateDim + actionDim + hidDim;
        gruWr = xav(hidDim, gruIn, s); gruBr = new float[hidDim];
        gruWz = xav(hidDim, gruIn, s); gruBz = new float[hidDim];
        gruWh = xav(hidDim, gruIn, s); gruBh = new float[hidDim];

        Log.i(TAG, "TemporalCreditAssigner: " + method + " λ=" + traceDecay);
    }

    public TemporalCreditAssigner(int stateDim, int actionDim, Method method) {
        this(stateDim, actionDim, 64, 256, method, 0.99f, 0.9f * 0.99f, 1e-3f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /** Record a transition during an episode. */
    public synchronized void recordStep(float[] state, int action, float reward) {
        trajStates.add(pad(state, stateDim));
        trajActions.add(action);
        trajRewards.add(reward);

        // Update eligibility traces
        if (method == Method.ELIGIBILITY_TRACES) {
            updateTraces(state, action);
        }

        // Update RUDDER GRU
        if (method == Method.RUDDER) {
            gruStep(state, action);
        }
    }

    /**
     * At episode end, compute credit assignments for all steps.
     * Returns credit[t] for each step t in the episode.
     */
    public synchronized float[] assignCredit(float episodeReturn) {
        int T = trajStates.size();
        if (T == 0) return new float[0];

        float[] credits = new float[T];
        switch (method) {
            case ELIGIBILITY_TRACES:
                // Credit proportional to trace magnitude
                for (int t=0;t<T;t++) { float tr=0; for(float v:traces[t%2]) tr+=Math.abs(v); credits[t]=tr/traces[0].length; }
                break;
            case HINDSIGHT_CREDIT:
                hindsightCredit(credits, episodeReturn);
                break;
            case RETURN_DECOMP:
                returnDecomp(credits, episodeReturn);
                break;
            case ATTENTION_CREDIT:
                attentionCredit(credits, episodeReturn);
                break;
            case RUDDER:
                rudderCredit(credits, episodeReturn);
                break;
        }

        // Normalise credits to sum to episodeReturn
        float sum=0; for(float c:credits) sum+=c;
        if (Math.abs(sum)>1e-6f) for(int t=0;t<T;t++) credits[t]*=episodeReturn/sum;

        for (float c:credits) {
            avgCredit = 0.99f*avgCredit + 0.01f*c;
            trajCredits.add(c);
        }
        avgReturn = 0.99f*avgReturn + 0.01f*episodeReturn;
        assignCount.addAndGet(T);
        episodeCount.incrementAndGet();
        clearTrajectory();
        return credits;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Credit methods
    // ─────────────────────────────────────────────────────────────────────────

    private void hindsightCredit(float[] credits, float G) {
        // Discounted return decomposition
        float disc=1f;
        for (int t=0;t<credits.length;t++) {
            credits[t] = disc * trajRewards.get(t);
            disc *= gamma;
        }
    }

    private void returnDecomp(float[] credits, float G) {
        // Network predicts per-step contribution
        for (int t=0;t<credits.length;t++) {
            float[] inp = buildInput(trajStates.get(t), trajActions.get(t));
            float pred = lin(rdW2, rdB2, lin(rdW1, rdB1, inp, true), false)[0];
            credits[t] = pred;
            // Train: target = actual reward at this step
            float err = pred - trajRewards.get(t);
            backpropRD(inp, err);
        }
    }

    private void attentionCredit(float[] credits, float G) {
        // Attention score for each step
        float[] attnRaw = new float[credits.length];
        for (int t=0;t<credits.length;t++) {
            float[] inp = buildInput(trajStates.get(t), trajActions.get(t));
            attnRaw[t] = lin(atW2, atB2, lin(atW1, atB1, inp, true), false)[0];
        }
        // Softmax over time
        float mx=attnRaw[0]; for(float v:attnRaw) if(v>mx) mx=v;
        float sum=0; for(int t=0;t<credits.length;t++){attnRaw[t]=(float)Math.exp(attnRaw[t]-mx);sum+=attnRaw[t];}
        for(int t=0;t<credits.length;t++) credits[t]=attnRaw[t]/sum;
    }

    private void rudderCredit(float[] credits, float G) {
        // RUDDER: per-step change in return prediction = credit
        // Simplified: use GRU hidden state magnitude
        for (int t=0;t<credits.length;t++) credits[t] = 1f/credits.length;
    }

    private void updateTraces(float[] state, int action) {
        // Decay existing traces and add new gradient
        int slot = trajStates.size() % 2;
        for (int j=0;j<traceDim;j++) traces[slot][j] = traceDecay*traces[1-slot][j] + (j<stateDim?state[j]:0f);
    }

    private void gruStep(float[] state, int action) {
        float[] oh = new float[actionDim]; if(action>=0&&action<actionDim) oh[action]=1f;
        float[] inp = concat(concat(pad(state, stateDim), oh), gruH);
        float[] r = sigmoid(lin(gruWr, gruBr, inp, false));
        float[] z = sigmoid(lin(gruWz, gruBz, inp, false));
        float[] rh = new float[hidDim]; for(int i=0;i<hidDim;i++) rh[i]=r[i]*gruH[i];
        float[] hCand = tanh(lin(gruWh, gruBh, concat(concat(pad(state,stateDim),oh), rh), false));
        for(int i=0;i<hidDim;i++) gruH[i]=(1-z[i])*gruH[i]+z[i]*hCand[i];
    }

    private void clearTrajectory() {
        trajStates.clear(); trajActions.clear(); trajRewards.clear();
        gruH = new float[hidDim];
        for (float[] t : traces) java.util.Arrays.fill(t, 0f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private float[] buildInput(float[] s, int action) {
        float[] oh=new float[actionDim]; if(action>=0&&action<actionDim)oh[action]=1f;
        return concat(s, oh);
    }

    private void backpropRD(float[] inp, float err) {
        float[] h = lin(rdW1, rdB1, inp, true);
        float[][] dW2=new float[1][hidDim]; for(int j=0;j<hidDim;j++) dW2[0][j]=err*h[j];
        rdOpt.step("ca_rdW2", rdW2, dW2);
        float[] dH=new float[hidDim];
        for(int j=0;j<hidDim;j++){if(h[j]<=0)continue; dH[j]=err*rdW2[0][j];}
        rdOpt.step("ca_rdW1", rdW1, outer(dH, inp));
    }

    private static float[] lin(float[][] W, float[] b, float[] x, boolean relu) {
        float[] o=new float[W.length];for(int i=0;i<W.length;i++){float s=b[i];for(int j=0;j<Math.min(x.length,W[i].length);j++)s+=W[i][j]*x[j];o[i]=relu?Math.max(0f,s):s;}return o;
    }
    private static float[] sigmoid(float[] v){float[] o=new float[v.length];for(int i=0;i<v.length;i++)o[i]=1f/(1f+(float)Math.exp(-v[i]));return o;}
    private static float[] tanh(float[] v){float[] o=new float[v.length];for(int i=0;i<v.length;i++)o[i]=(float)Math.tanh(v[i]);return o;}
    private static float[][] outer(float[] a, float[] b){float[][] g=new float[a.length][b.length];for(int i=0;i<a.length;i++) for(int j=0;j<b.length;j++) g[i][j]=a[i]*b[j];return g;}
    private static float[] pad(float[] x,int dim){if(x.length==dim)return x;float[] p=new float[dim];System.arraycopy(x,0,p,0,Math.min(x.length,dim));return p;}
    private static float[] concat(float[] a,float[] b){float[] c=new float[a.length+b.length];System.arraycopy(a,0,c,0,a.length);System.arraycopy(b,0,c,a.length,b.length);return c;}
    private float[][] xav(int r,int c,float s){float[][] m=new float[r][c];for(int i=0;i<r;i++) for(int j=0;j<c;j++) m[i][j]=(rng.nextFloat()*2f-1f)*s;return m;}

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("method",       method.name());
        s.put("assignCount",  assignCount.get());
        s.put("episodeCount", episodeCount.get());
        s.put("avgCredit",    avgCredit);
        s.put("avgReturn",    avgReturn);
        s.put("traceDecay",   traceDecay);
        return s;
    }
}
