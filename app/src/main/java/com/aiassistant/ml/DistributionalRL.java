package com.aiassistant.ml;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DistributionalRL — distributional value function learning (C51 / QR-DQN / IQN).
 *
 * Instead of learning E[G] (expected return), distributional RL learns the full
 * return distribution Z(s,a) using multiple atoms/quantiles.
 *
 * Methods implemented:
 *
 *   C51 (Bellemare et al. 2017):
 *     Fixed atoms {z_0, ..., z_{N-1}} on a fixed support [V_min, V_max].
 *     Network outputs p_i = P(Z=z_i | s, a).
 *     Target: project Bellman-updated distribution onto fixed support.
 *
 *   QR-DQN (Dabney et al. 2018):
 *     N quantiles τ_i = (2i-1)/(2N)  i=1..N.
 *     Network outputs N quantile values θ(s,a).
 *     Loss: Huber quantile regression loss.
 *
 *   IQN (Dabney et al. 2019):
 *     Sample τ ~ Uniform[0,1] at each step.
 *     Encode τ via cosine embedding → state-conditioned quantile network.
 *
 * Benefits:
 *   - Richer gradient signal (learns the entire distribution, not just mean).
 *   - Risk-sensitive action selection (CVaR, worst-case, optimistic).
 *   - Better performance on tasks with multimodal return distributions.
 *
 * Thread-safe.
 */
public class DistributionalRL {

    private static final String TAG = "DistribRL";

    public enum Method { C51, QR_DQN, IQN }
    public enum RiskMeasure { MEAN, CVaR_PESSIMISTIC, CVaR_OPTIMISTIC, WORST_CASE, BEST_CASE }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int    stateDim, actionDim, hidDim, numAtoms;
    private final float  vMin, vMax;
    private final Method method;
    private final float  gamma;
    private final int    iqnCosineOrder;

    // Network: state → [hidDim] → [actionDim × numAtoms]
    private final float[][] W1, W2;
    private final float[]   b1, b2;   // b2 size = actionDim * numAtoms

    // IQN: quantile embedding → [hidDim]
    private final float[][] cosW;
    private final float[]   cosB;

    private final NeuralNetworkOptimizer opt;

    // C51 atom support
    private final float[] atoms;        // [numAtoms]

    private final AtomicInteger actCount    = new AtomicInteger(0);
    private final AtomicInteger updateCount = new AtomicInteger(0);
    private float avgLoss  = 0f;
    private float avgMeanQ = 0f;

    private final Random rng = new Random(383L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public DistributionalRL(int stateDim, int actionDim, int hidDim,
                             int numAtoms, float vMin, float vMax,
                             float gamma, Method method, int iqnCosineOrder,
                             float lr) {
        this.stateDim      = stateDim;
        this.actionDim     = actionDim;
        this.hidDim        = hidDim;
        this.numAtoms      = numAtoms;
        this.vMin          = vMin;
        this.vMax          = vMax;
        this.gamma         = gamma;
        this.method        = method;
        this.iqnCosineOrder= iqnCosineOrder;
        this.opt           = new NeuralNetworkOptimizer(lr);

        float s = (float)Math.sqrt(2.0/(stateDim+hidDim));
        W1 = xav(hidDim, stateDim, s);         b1 = new float[hidDim];
        W2 = xav(actionDim*numAtoms, hidDim, s*0.01f); b2 = new float[actionDim*numAtoms];

        // IQN cosine embedding
        cosW = xav(hidDim, iqnCosineOrder, s); cosB = new float[hidDim];

        // C51 atoms
        atoms = new float[numAtoms];
        for (int i=0;i<numAtoms;i++) atoms[i] = vMin + i*(vMax-vMin)/(numAtoms-1);

        Log.i(TAG, "DistribRL: " + method + " N=" + numAtoms + " [" + vMin + "," + vMax + "]");
    }

    public DistributionalRL(int stateDim, int actionDim, Method method) {
        this(stateDim, actionDim, 128, 51, -10f, 10f, 0.99f, method, 64, 1e-3f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Action selection
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized int selectAction(float[] state, RiskMeasure risk) {
        float[] values = stateActionValues(state, risk);
        actCount.incrementAndGet();
        avgMeanQ = 0.99f*avgMeanQ + 0.01f*max(values);
        return argmax(values);
    }

    public synchronized int selectAction(float[] state) {
        return selectAction(state, RiskMeasure.MEAN);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Distributional forward
    // ─────────────────────────────────────────────────────────────────────────

    /** Returns Z distribution for all actions: [actionDim][numAtoms]. */
    public synchronized float[][] distribution(float[] state) {
        float[] s = pad(state, stateDim);
        float[] h = lin(W1, b1, s, true);
        if (method == Method.IQN) {
            float tau = rng.nextFloat();
            h = elementwiseMul(h, iqnEmbedding(tau));
        }
        float[] out = lin(W2, b2, h, false);
        float[][] dist = new float[actionDim][numAtoms];
        for (int a=0;a<actionDim;a++) {
            for (int n=0;n<numAtoms;n++) dist[a][n] = out[a*numAtoms+n];
            if (method == Method.C51) dist[a] = softmax(dist[a]);
            // QR-DQN / IQN: quantile values directly
        }
        return dist;
    }

    /** Expected value for each action under given risk measure. */
    public synchronized float[] stateActionValues(float[] state, RiskMeasure risk) {
        float[][] dist = distribution(state);
        float[] values = new float[actionDim];
        for (int a=0;a<actionDim;a++) {
            values[a] = applyRisk(dist[a], risk);
        }
        return values;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Training
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized float update(float[] state, int action, float reward,
                                      float[] nextState, boolean done) {
        float loss;
        switch (method) {
            case C51:    loss = updateC51(state, action, reward, nextState, done); break;
            case QR_DQN: loss = updateQRDQN(state, action, reward, nextState, done); break;
            case IQN:    loss = updateIQN(state, action, reward, nextState, done); break;
            default:     loss = updateC51(state, action, reward, nextState, done);
        }
        avgLoss = 0.99f*avgLoss + 0.01f*loss;
        updateCount.incrementAndGet();
        return loss;
    }

    private float updateC51(float[] s, int action, float reward, float[] sp, boolean done) {
        float[][] distSp = distribution(sp);
        int bestA = argmax(stateActionValues(sp, RiskMeasure.MEAN));
        float[] pNext = distSp[Math.min(bestA, actionDim-1)];

        // Project onto support
        float[] target = new float[numAtoms];
        for (int j=0;j<numAtoms;j++) {
            float Tz = Math.max(vMin, Math.min(vMax, reward + (done?0f:gamma*atoms[j])));
            float b  = (Tz - vMin) / ((vMax - vMin) / (numAtoms - 1));
            int lo = (int)Math.floor(b), hi = (int)Math.ceil(b);
            lo = Math.max(0, Math.min(numAtoms-1, lo));
            hi = Math.max(0, Math.min(numAtoms-1, hi));
            target[lo] += pNext[j] * (hi - b);
            target[hi] += pNext[j] * (b  - lo);
        }

        // Cross-entropy loss
        float[][] dist = distribution(s);
        float[] pPred  = dist[Math.min(action, actionDim-1)];
        float   loss   = 0;
        float[] grad   = new float[numAtoms];
        for (int i=0;i<numAtoms;i++) {
            loss    -= target[i] * (float)Math.log(Math.max(pPred[i], 1e-8f));
            grad[i]  = pPred[i] - target[i];
        }

        backpropDistrib(s, action, grad);
        return loss;
    }

    private float updateQRDQN(float[] s, int action, float reward, float[] sp, boolean done) {
        float[][] distSp = distribution(sp);
        int bestA = argmax(stateActionValues(sp, RiskMeasure.MEAN));
        float[] thetaNext = distSp[Math.min(bestA, actionDim-1)];
        float[] targets   = new float[numAtoms];
        for (int j=0;j<numAtoms;j++) targets[j] = reward + (done?0f:gamma*thetaNext[j]);

        float[][] distS = distribution(s);
        float[] theta   = distS[Math.min(action, actionDim-1)];
        // Quantile Huber loss
        float loss = 0;
        float[] grad = new float[numAtoms];
        for (int i=0;i<numAtoms;i++) {
            float tau = (2f*i+1f)/(2f*numAtoms);
            for (int j=0;j<numAtoms;j++) {
                float u = targets[j] - theta[i];
                float rho = Math.abs(tau - (u<0?1f:0f));
                float huber = Math.abs(u) < 1f ? 0.5f*u*u : Math.abs(u)-0.5f;
                loss   += rho * huber / numAtoms;
                grad[i]+= rho * (Math.abs(u)<1f ? u : Math.signum(u)) / numAtoms;
            }
        }
        backpropDistrib(s, action, grad);
        return loss;
    }

    private float updateIQN(float[] s, int action, float reward, float[] sp, boolean done) {
        return updateQRDQN(s, action, reward, sp, done); // IQN uses same loss
    }

    private void backpropDistrib(float[] state, int action, float[] gradAtoms) {
        float[] s = pad(state, stateDim);
        float[] h = lin(W1, b1, s, true);

        float[] dOut = new float[actionDim * numAtoms];
        int aOff = Math.min(action, actionDim-1) * numAtoms;
        System.arraycopy(gradAtoms, 0, dOut, aOff, numAtoms);

        float[][] dW2 = new float[actionDim*numAtoms][hidDim];
        for (int i=0;i<actionDim*numAtoms;i++) for(int j=0;j<hidDim;j++) dW2[i][j]=dOut[i]*h[j];
        opt.step("dr_W2", W2, dW2);

        float[] dH = new float[hidDim];
        for(int j=0;j<hidDim;j++){if(h[j]<=0)continue;for(int i=0;i<actionDim*numAtoms;i++)dH[j]+=dOut[i]*W2[i][j];}
        float[][] dW1 = new float[hidDim][stateDim];
        for(int i=0;i<hidDim;i++) for(int j=0;j<stateDim;j++) dW1[i][j]=dH[i]*s[j];
        opt.step("dr_W1", W1, dW1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Risk measures
    // ─────────────────────────────────────────────────────────────────────────

    private float applyRisk(float[] dist, RiskMeasure risk) {
        switch (risk) {
            case CVaR_PESSIMISTIC: {
                // CVaR at 10%: mean of bottom 10% atoms
                int k = Math.max(1, numAtoms/10);
                float s=0,w=0;
                for(int i=0;i<k;i++){float p=method==Method.C51?dist[i]:1f/numAtoms;s+=p*atomValue(dist,i);w+=p;}
                return w>0?s/w:s;
            }
            case CVaR_OPTIMISTIC: {
                int k = Math.max(1, numAtoms-numAtoms/10);
                float s=0,w=0;
                for(int i=k;i<numAtoms;i++){float p=method==Method.C51?dist[i]:1f/numAtoms;s+=p*atomValue(dist,i);w+=p;}
                return w>0?s/w:s;
            }
            case WORST_CASE: return atomValue(dist, 0);
            case BEST_CASE:  return atomValue(dist, numAtoms-1);
            case MEAN: default: {
                float s=0;
                for(int i=0;i<numAtoms;i++) s+=atomValue(dist,i)*(method==Method.C51?dist[i]:1f/numAtoms);
                return s;
            }
        }
    }

    private float atomValue(float[] dist, int i) {
        return method == Method.C51 ? atoms[i] : dist[i];
    }

    private float[] iqnEmbedding(float tau) {
        float[] cos = new float[iqnCosineOrder];
        for (int n=0;n<iqnCosineOrder;n++) cos[n] = (float)Math.cos(Math.PI*(n+1)*tau);
        return lin(cosW, cosB, cos, true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static float[] lin(float[][] W, float[] b, float[] x, boolean relu) {
        float[] o=new float[W.length];for(int i=0;i<W.length;i++){float s=b[i];for(int j=0;j<Math.min(x.length,W[i].length);j++)s+=W[i][j]*x[j];o[i]=relu?Math.max(0f,s):s;}return o;
    }
    private static float[] elementwiseMul(float[] a, float[] b){float[] r=new float[Math.min(a.length,b.length)];for(int i=0;i<r.length;i++)r[i]=a[i]*b[i];return r;}
    private static float[] softmax(float[] v){float mx=v[0];for(float x:v)if(x>mx)mx=x;float sum=0;float[] o=new float[v.length];for(int i=0;i<v.length;i++){o[i]=(float)Math.exp(v[i]-mx);sum+=o[i];}for(int i=0;i<v.length;i++)o[i]/=sum;return o;}
    private static int argmax(float[] v){int b=0;for(int i=1;i<v.length;i++)if(v[i]>v[b])b=i;return b;}
    private static float max(float[] v){float m=v[0];for(float x:v)if(x>m)m=x;return m;}
    private static float[] pad(float[] x,int dim){if(x.length==dim)return x;float[] p=new float[dim];System.arraycopy(x,0,p,0,Math.min(x.length,dim));return p;}
    private float[][] xav(int r,int c,float s){float[][] m=new float[r][c];for(int i=0;i<r;i++) for(int j=0;j<c;j++) m[i][j]=(rng.nextFloat()*2f-1f)*s;return m;}

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("method",      method.name());
        s.put("numAtoms",    numAtoms);
        s.put("actCount",    actCount.get());
        s.put("updateCount", updateCount.get());
        s.put("avgLoss",     avgLoss);
        s.put("avgMeanQ",    avgMeanQ);
        s.put("vMin",        vMin);
        s.put("vMax",        vMax);
        return s;
    }
}
