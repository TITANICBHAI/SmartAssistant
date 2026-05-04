package com.aiassistant.ml;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * NoisyNetworkExplorer — parametric noise for exploration (NoisyNet, Fortunato et al. 2017).
 *
 * Replaces ε-greedy exploration with learned stochastic weights:
 *
 *   y = (μ_w + σ_w ⊙ ε_w) · x + (μ_b + σ_b ⊙ ε_b)
 *
 * where ε_w, ε_b are noise variables sampled per forward pass.
 *
 * Two noise types:
 *   INDEPENDENT:  ε ∼ N(0,1) per weight — expensive but unbiased.
 *   FACTORISED:   ε_{ij} = f(ε_i) · f(ε_j),  f(x)=sgn(x)·√|x|  — O(p+q) vs O(p·q).
 *
 * The network learns WHEN to be noisy (σ → 0 when exploitation is best)
 * and WHEN to explore (high σ in uncertain states).
 *
 * Thread-safe.
 */
public class NoisyNetworkExplorer {

    private static final String TAG = "NoisyNet";

    public enum NoiseType { INDEPENDENT, FACTORISED }

    // ─────────────────────────────────────────────────────────────────────────
    // Noisy linear layer
    // ─────────────────────────────────────────────────────────────────────────
    private static class NoisyLinear {
        final int inDim, outDim;
        final float[][] muW,  sigW;    // mean & sigma weights
        final float[]   muB,  sigB;    // mean & sigma biases
        final float[][] epsW;          // sampled noise (refreshed each forward)
        final float[]   epsB;
        float sigma0;                  // initial sigma

        NoisyLinear(int inDim, int outDim, float sigma0, Random rng) {
            this.inDim  = inDim;
            this.outDim = outDim;
            this.sigma0 = sigma0;
            float s = (float)Math.sqrt(1.0/inDim);
            muW  = new float[outDim][inDim]; sigW = new float[outDim][inDim];
            muB  = new float[outDim];        sigB = new float[outDim];
            epsW = new float[outDim][inDim]; epsB = new float[outDim];
            for (int i=0;i<outDim;i++) {
                for (int j=0;j<inDim;j++) {
                    muW[i][j]  = (rng.nextFloat()*2f-1f)*s;
                    sigW[i][j] = sigma0 * s;
                }
                muB[i]  = (rng.nextFloat()*2f-1f)*s;
                sigB[i] = sigma0 * s;
            }
        }

        /** Sample new noise (call once per step, not per forward pass). */
        void sampleNoise(Random rng, NoiseType type) {
            if (type == NoiseType.FACTORISED) {
                float[] eps_in  = new float[inDim];
                float[] eps_out = new float[outDim];
                for (int j=0;j<inDim;j++)  eps_in[j]  = f(rng.nextGaussian());
                for (int i=0;i<outDim;i++) eps_out[i] = f(rng.nextGaussian());
                for (int i=0;i<outDim;i++) {
                    for (int j=0;j<inDim;j++) epsW[i][j] = eps_out[i] * eps_in[j];
                    epsB[i] = eps_out[i];
                }
            } else {
                for (int i=0;i<outDim;i++) {
                    for (int j=0;j<inDim;j++) epsW[i][j] = (float)rng.nextGaussian();
                    epsB[i] = (float)rng.nextGaussian();
                }
            }
        }

        /** Forward pass using noisy weights. */
        float[] forward(float[] x, boolean noisy) {
            float[] o = new float[outDim];
            for (int i=0;i<outDim;i++) {
                float v = noisy ? muB[i] + sigB[i]*epsB[i] : muB[i];
                for (int j=0;j<Math.min(x.length,inDim);j++)
                    v += (muW[i][j] + (noisy ? sigW[i][j]*epsW[i][j] : 0f)) * x[j];
                o[i] = v;
            }
            return o;
        }

        static float f(double x) { return (float)(Math.signum(x)*Math.sqrt(Math.abs(x))); }

        float avgSigma() {
            float s=0,n=0;
            for(float[] row:sigW) for(float v:row){s+=Math.abs(v);n++;}
            return n>0?s/n:0f;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int stateDim, actionDim, hidDim;
    private final NoiseType noiseType;
    private final NoisyLinear layer1, layer2;
    private final NeuralNetworkOptimizer opt;
    private final float gamma;

    private final AtomicInteger actCount    = new AtomicInteger(0);
    private final AtomicInteger updateCount = new AtomicInteger(0);
    private float avgLoss   = 0f;
    private float avgSigma  = 0f;

    private final Random rng = new Random(379L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public NoisyNetworkExplorer(int stateDim, int actionDim, int hidDim,
                                 NoiseType noiseType, float sigma0,
                                 float gamma, float lr) {
        this.stateDim  = stateDim;
        this.actionDim = actionDim;
        this.hidDim    = hidDim;
        this.noiseType = noiseType;
        this.gamma     = gamma;
        this.opt       = new NeuralNetworkOptimizer(lr);
        this.layer1    = new NoisyLinear(stateDim, hidDim, sigma0, rng);
        this.layer2    = new NoisyLinear(hidDim, actionDim, sigma0, rng);
        Log.i(TAG, "NoisyNet: type=" + noiseType + " σ0=" + sigma0);
    }

    public NoisyNetworkExplorer(int stateDim, int actionDim) {
        this(stateDim, actionDim, 128, NoiseType.FACTORISED, 0.5f, 0.99f, 1e-3f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Action selection (no epsilon needed — noise handles exploration)
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized int selectAction(float[] state) {
        layer1.sampleNoise(rng, noiseType);
        layer2.sampleNoise(rng, noiseType);
        float[] h = relu(layer1.forward(pad(state, stateDim), true));
        float[] Q = layer2.forward(h, true);
        actCount.incrementAndGet();
        avgSigma = 0.99f*avgSigma + 0.01f*(layer1.avgSigma()+layer2.avgSigma())*0.5f;
        return argmax(Q);
    }

    /** Greedy Q-values (no noise — for evaluation). */
    public synchronized float[] greedyQValues(float[] state) {
        float[] h = relu(layer1.forward(pad(state, stateDim), false));
        return layer2.forward(h, false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Training (DQN-style TD update on μ parameters)
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized float update(float[] state, int action, float reward,
                                      float[] nextState, boolean done) {
        float[] s   = pad(state,     stateDim);
        float[] sp  = pad(nextState, stateDim);
        float[] Qsp = greedyQValues(sp);
        float   target = reward + (done ? 0f : gamma * max(Qsp));
        float[] Qs  = greedyQValues(s);
        int a = Math.min(action, actionDim-1);
        float err  = Qs[a] - target;
        float loss = err * err;

        // Backprop through μ weights
        float[] h = relu(layer1.forward(s, false));
        float[] dQ = new float[actionDim]; dQ[a] = err;
        // Layer 2 gradients
        for (int i=0;i<actionDim;i++) for(int j=0;j<hidDim;j++) layer2.muW[i][j]-=1e-3f*dQ[i]*h[j];
        for (int i=0;i<actionDim;i++) layer2.muB[i]-=1e-3f*dQ[i];
        // Sigma gradients (encourage uncertainty where TD error is high)
        float sigGrad = Math.abs(err) * 0.001f;
        for (int i=0;i<actionDim;i++) for(int j=0;j<hidDim;j++) layer2.sigW[i][j] = Math.max(0.001f, layer2.sigW[i][j]-sigGrad*(layer2.sigW[i][j]-0.01f));
        // Layer 1 backprop
        float[] dH = new float[hidDim];
        for(int j=0;j<hidDim;j++){if(h[j]<=0)continue;for(int i=0;i<actionDim;i++)dH[j]+=dQ[i]*layer1.muW[i%hidDim][j%stateDim];}
        for(int i=0;i<hidDim;i++) for(int j=0;j<Math.min(stateDim,s.length);j++) layer1.muW[i][j]-=1e-3f*dH[i]*s[j];

        avgLoss = 0.99f*avgLoss + 0.01f*loss;
        updateCount.incrementAndGet();
        return loss;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static float[] relu(float[] v){float[] o=v.clone();for(int i=0;i<o.length;i++)if(o[i]<0)o[i]=0;return o;}
    private static int argmax(float[] v){int b=0;for(int i=1;i<v.length;i++)if(v[i]>v[b])b=i;return b;}
    private static float max(float[] v){float m=v[0];for(float x:v)if(x>m)m=x;return m;}
    private static float[] pad(float[] x,int dim){if(x.length==dim)return x;float[] p=new float[dim];System.arraycopy(x,0,p,0,Math.min(x.length,dim));return p;}

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("noiseType",   noiseType.name());
        s.put("actCount",    actCount.get());
        s.put("updateCount", updateCount.get());
        s.put("avgLoss",     avgLoss);
        s.put("avgSigma",    avgSigma);
        s.put("layer1Sigma", layer1.avgSigma());
        s.put("layer2Sigma", layer2.avgSigma());
        return s;
    }
}
