package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TransitionPredictor — learned dynamics model for model-based RL planning.
 *
 * Learns the environment's state transition function:
 *   s_{t+1} = f_θ(s_t, a_t) + noise
 *
 * Architecture:
 *   Input: concat(s_t, one_hot(a_t))  →  [hidDim, ReLU] x 2  →  Δs (residual)
 *   Prediction: s_{t+1} = s_t + Δs   (residual connections improve accuracy)
 *
 * Also predicts:
 *   - Expected reward: r̂ = r_head(s, a)
 *   - Done probability: p̂_done = sigmoid(d_head(s, a))
 *
 * Extensions:
 *   PROBABILISTIC: output mean AND log-variance → sample from N(μ, σ²).
 *   ENSEMBLE:      maintain K models, use disagreement as uncertainty signal.
 *
 * Use cases:
 *   - Dyna-style planning: generate synthetic transitions for training.
 *   - Model-predictive control: rollout K steps and pick best first action.
 *   - Uncertainty estimation: avoid OOD states where models disagree.
 *
 * Thread-safe.
 */
public class TransitionPredictor {

    private static final String TAG = "TransitionPredictor";

    public enum Mode { DETERMINISTIC, PROBABILISTIC, ENSEMBLE }

    // ─────────────────────────────────────────────────────────────────────────
    // Single model
    // ─────────────────────────────────────────────────────────────────────────
    private static class DynModel {
        float[][] W1, W2, W3;   // [hidDim][stateDim+actionDim], [hidDim][hidDim], [stateDim][hidDim]
        float[]   b1, b2, b3;
        // Reward head: [1][hidDim]
        float[][] Wr; float[] br;
        // Done head:   [1][hidDim]
        float[][] Wd; float[] bd;
        // Log-var head (PROBABILISTIC): [stateDim][hidDim]
        float[][] Wlv; float[] blv;

        DynModel(int stateDim, int actionDim, int hidDim, Random rng) {
            int inDim = stateDim + actionDim;
            float s = (float) Math.sqrt(2.0/(inDim+hidDim));
            W1 = xav(hidDim, inDim,    s, rng); b1 = new float[hidDim];
            W2 = xav(hidDim, hidDim,   s, rng); b2 = new float[hidDim];
            W3 = xav(stateDim, hidDim, s*0.01f, rng); b3 = new float[stateDim];
            Wr = xav(1, hidDim, s*0.01f, rng); br = new float[1];
            Wd = xav(1, hidDim, s*0.01f, rng); bd = new float[1];
            Wlv= xav(stateDim, hidDim, s*0.01f, rng); blv= new float[stateDim];
        }

        float[] predict(float[] inp) {
            float[] h1 = linRelu(W1, b1, inp);
            float[] h2 = linRelu(W2, b2, h1);
            return lin(W3, b3, h2);
        }

        float predictReward(float[] inp) {
            float[] h1=linRelu(W1,b1,inp);float[] h2=linRelu(W2,b2,h1);
            return lin(Wr,br,h2)[0];
        }

        float predictDone(float[] inp) {
            float[] h1=linRelu(W1,b1,inp);float[] h2=linRelu(W2,b2,h1);
            return sigmoid(lin(Wd,bd,h2)[0]);
        }

        float[] predictLogVar(float[] inp) {
            float[] h1=linRelu(W1,b1,inp);float[] h2=linRelu(W2,b2,h1);
            return lin(Wlv,blv,h2);
        }

        static float[] linRelu(float[][] W, float[] b, float[] x) {
            float[] o=new float[W.length];
            for(int i=0;i<W.length;i++){float s=b[i];for(int j=0;j<Math.min(x.length,W[i].length);j++)s+=W[i][j]*x[j];o[i]=Math.max(0f,s);}
            return o;
        }
        static float[] lin(float[][] W, float[] b, float[] x) {
            float[] o=new float[W.length];
            for(int i=0;i<W.length;i++){float s=b[i];for(int j=0;j<Math.min(x.length,W[i].length);j++)s+=W[i][j]*x[j];o[i]=s;}
            return o;
        }
        static float sigmoid(float x){return 1f/(1f+(float)Math.exp(-x));}
        static float[][] xav(int r,int c,float s,Random rng){float[][] m=new float[r][c];for(int i=0;i<r;i++) for(int j=0;j<c;j++) m[i][j]=(rng.nextFloat()*2f-1f)*s;return m;}
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int    stateDim, actionDim, hidDim, ensembleSize;
    private final Mode   mode;
    private final DynModel[] ensemble;
    private final NeuralNetworkOptimizer opt;

    private final AtomicInteger trainCount  = new AtomicInteger(0);
    private final AtomicInteger predictCount= new AtomicInteger(0);
    private float avgStateLoss  = 0f;
    private float avgRewardLoss = 0f;
    private float avgDoneLoss   = 0f;
    private float avgDisagreement=0f;

    private final Random rng = new Random(283L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public TransitionPredictor(int stateDim, int actionDim, int hidDim,
                                int ensembleSize, Mode mode, float lr) {
        this.stateDim    = stateDim;
        this.actionDim   = actionDim;
        this.hidDim      = hidDim;
        this.ensembleSize= ensembleSize;
        this.mode        = mode;
        this.opt         = new NeuralNetworkOptimizer(lr);

        ensemble = new DynModel[ensembleSize];
        for (int k=0;k<ensembleSize;k++) ensemble[k] = new DynModel(stateDim, actionDim, hidDim, rng);

        Log.i(TAG, "TransitionPredictor: s=" + stateDim + " a=" + actionDim
                + " ensemble=" + ensembleSize + " mode=" + mode);
    }

    public TransitionPredictor(int stateDim, int actionDim) {
        this(stateDim, actionDim, 128, 3, Mode.ENSEMBLE, 1e-3f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Prediction
    // ─────────────────────────────────────────────────────────────────────────

    /** Predict next state (mean over ensemble). */
    public synchronized float[] predictNextState(float[] state, int action) {
        float[] inp = buildInput(state, action);
        float[] mean = new float[stateDim];
        for (DynModel m : ensemble) {
            float[] dS = m.predict(inp);
            for (int i=0;i<stateDim;i++) mean[i]+=dS[i];
        }
        for (int i=0;i<stateDim;i++) mean[i] = pad(state,stateDim)[i] + mean[i]/ensembleSize;
        predictCount.incrementAndGet();
        return mean;
    }

    /** Predict reward. */
    public synchronized float predictReward(float[] state, int action) {
        float[] inp = buildInput(state, action);
        float sum = 0;
        for (DynModel m : ensemble) sum += m.predictReward(inp);
        return sum / ensembleSize;
    }

    /** Predict done probability. */
    public synchronized float predictDone(float[] state, int action) {
        float[] inp = buildInput(state, action);
        float sum = 0;
        for (DynModel m : ensemble) sum += m.predictDone(inp);
        return sum / ensembleSize;
    }

    /**
     * Ensemble disagreement = mean pairwise distance of predictions.
     * High disagreement → high uncertainty → avoid this (s,a) pair.
     */
    public synchronized float disagreement(float[] state, int action) {
        float[] inp = buildInput(state, action);
        float[][] preds = new float[ensembleSize][];
        for (int k=0;k<ensembleSize;k++) preds[k] = ensemble[k].predict(inp);
        float[] mean = new float[stateDim];
        for (float[] p : preds) for(int i=0;i<stateDim;i++) mean[i]+=p[i];
        for(int i=0;i<stateDim;i++) mean[i]/=ensembleSize;
        float d=0;
        for (float[] p : preds) { float v=0; for(int i=0;i<stateDim;i++){float diff=p[i]-mean[i];v+=diff*diff;} d+=v; }
        return d / ensembleSize;
    }

    /**
     * Roll out K steps from a starting state and return all predicted states.
     */
    public synchronized List<float[]> rollout(float[] startState, int[] actions) {
        List<float[]> traj = new ArrayList<>();
        float[] s = startState.clone();
        for (int action : actions) {
            s = predictNextState(s, action);
            traj.add(s.clone());
        }
        return traj;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Training
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized void train(float[] state, int action, float reward,
                                   float[] nextState, boolean done) {
        float[] inp    = buildInput(state, action);
        float[] s      = pad(state,     stateDim);
        float[] target = pad(nextState, stateDim);

        for (int k=0;k<ensembleSize;k++) {
            // Only train subset of ensemble per step (prevents co-adaptation)
            if (ensembleSize > 1 && rng.nextFloat() < 0.3f) continue;
            DynModel m = ensemble[k];

            float[] h1  = DynModel.linRelu(m.W1, m.b1, inp);
            float[] h2  = DynModel.linRelu(m.W2, m.b2, h1);
            float[] dS  = DynModel.lin(m.W3, m.b3, h2);

            // State loss: residual
            float[] err3 = new float[stateDim];
            float sLoss = 0;
            for (int i=0;i<stateDim;i++) {
                err3[i] = (s[i]+dS[i]) - target[i];
                sLoss  += err3[i]*err3[i];
            }
            opt.step("tp_W3_"+k, m.W3, outer(err3, h2));
            float[] dH2a = backFromHead(m.W3, h2, err3, hidDim);

            // Reward loss
            float rPred = m.predictReward(inp);
            float rErr  = rPred - reward;
            opt.step("tp_Wr_"+k, m.Wr, outer(new float[]{rErr}, h2));
            float[] dH2b = new float[hidDim];
            for(int j=0;j<hidDim;j++){if(h2[j]>0) dH2b[j]=rErr*m.Wr[0][j];}

            // Done loss (BCE)
            float dPred = m.predictDone(inp);
            float dTarget = done ? 1f : 0f;
            float dErr  = dPred - dTarget;
            opt.step("tp_Wd_"+k, m.Wd, outer(new float[]{dErr}, h2));
            float[] dH2c = new float[hidDim];
            for(int j=0;j<hidDim;j++){if(h2[j]>0) dH2c[j]=dErr*m.Wd[0][j];}

            // Combined h2 gradient
            float[] dH2 = new float[hidDim];
            for(int j=0;j<hidDim;j++) dH2[j]=dH2a[j]+dH2b[j]+dH2c[j];
            if(h2!=null) for(int j=0;j<hidDim;j++) if(h2[j]<=0) dH2[j]=0;
            opt.step("tp_W2_"+k, m.W2, outer(dH2, h1));

            float[] dH1 = backFromHead(m.W2, h1, dH2, hidDim);
            for(int j=0;j<hidDim;j++) if(h1[j]<=0) dH1[j]=0;
            opt.step("tp_W1_"+k, m.W1, outer(dH1, inp));

            avgStateLoss  = 0.99f*avgStateLoss  + 0.01f*sLoss/stateDim;
            avgRewardLoss = 0.99f*avgRewardLoss  + 0.01f*rErr*rErr;
            avgDoneLoss   = 0.99f*avgDoneLoss    + 0.01f*dErr*dErr;
        }
        float dis = disagreement(state, action);
        avgDisagreement = 0.99f*avgDisagreement + 0.01f*dis;
        trainCount.incrementAndGet();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private float[] buildInput(float[] state, int action) {
        float[] s = pad(state, stateDim);
        float[] inp = new float[stateDim + actionDim];
        System.arraycopy(s, 0, inp, 0, stateDim);
        if (action >= 0 && action < actionDim) inp[stateDim + action] = 1f;
        return inp;
    }

    private static float[][] outer(float[] a, float[] b) {
        float[][] g=new float[a.length][b.length];
        for(int i=0;i<a.length;i++) for(int j=0;j<b.length;j++) g[i][j]=a[i]*b[j];
        return g;
    }

    private static float[] backFromHead(float[][] W, float[] h, float[] dOut, int dim) {
        float[] dH=new float[dim];
        for(int j=0;j<dim;j++){if(h!=null&&j<h.length&&h[j]<=0)continue;for(int i=0;i<dOut.length;i++)if(j<W[i].length)dH[j]+=dOut[i]*W[i][j];}
        return dH;
    }

    private static float[] pad(float[] x, int dim) {
        if(x.length==dim)return x;
        float[] p=new float[dim];System.arraycopy(x,0,p,0,Math.min(x.length,dim));return p;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("mode",           mode.name());
        s.put("ensembleSize",   ensembleSize);
        s.put("trainCount",     trainCount.get());
        s.put("predictCount",   predictCount.get());
        s.put("avgStateLoss",   avgStateLoss);
        s.put("avgRewardLoss",  avgRewardLoss);
        s.put("avgDoneLoss",    avgDoneLoss);
        s.put("avgDisagreement",avgDisagreement);
        return s;
    }
}
