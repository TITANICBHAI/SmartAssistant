package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * StateAbstraction — learned state aggregation and compression for scalable RL.
 *
 * Raw game states can be very high-dimensional (pixel images, full game state vectors).
 * This module compresses states into compact abstract representations via:
 *
 *   1. AUTOENCODER:
 *      Encoder φ: R^n → R^k   (k << n)
 *      Decoder ψ: R^k → R^n
 *      Trained via reconstruction loss: ||ψ(φ(s)) - s||²
 *
 *   2. BISIMULATION:
 *      Two states s, s' are bisimilar if they have identical transition distributions
 *      and immediate rewards. Abstract: group bisimilar states.
 *      Approximated via metric learning: ||φ(s) - φ(s')||₂ ≈ bisim_distance(s, s')
 *
 *   3. VQ-VAE (Vector Quantised):
 *      Discrete codebook of K centroids. Encode state to nearest centroid.
 *      Enables discrete state spaces for tabular RL methods.
 *
 *   4. RANDOM_PROJECTION:
 *      φ(s) = W·s  (random Gaussian matrix, fixed at init).
 *      Fast, dimension-reducing, no training required.
 *
 * Thread-safe.
 */
public class StateAbstraction {

    private static final String TAG = "StateAbstraction";

    public enum Mode { AUTOENCODER, BISIMULATION, VQ_VAE, RANDOM_PROJECTION }

    // ─────────────────────────────────────────────────────────────────────────
    // Autoencoder
    // ─────────────────────────────────────────────────────────────────────────
    private final float[][] encW1, encW2;   // [hidDim][stateDim], [latDim][hidDim]
    private final float[]   encB1, encB2;
    private final float[][] decW1, decW2;   // [hidDim][latDim],   [stateDim][hidDim]
    private final float[]   decB1, decB2;
    private final NeuralNetworkOptimizer aeOpt;

    // VQ-VAE codebook
    private final float[][] codebook;       // [numCodes][latDim]
    private final int[]     codeUsage;

    // Random projection matrix (fixed)
    private final float[][] rProjW;         // [latDim][stateDim]

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int    stateDim, latDim, hidDim, numCodes;
    private final Mode   mode;
    private final float  commitment;     // VQ-VAE commitment loss weight
    private final float  vqLr;          // codebook update LR

    private final AtomicInteger encodeCount = new AtomicInteger(0);
    private final AtomicInteger trainCount  = new AtomicInteger(0);
    private float avgRecLoss = 0f;
    private float avgCbUsage = 0f;   // fraction of codebook actually used

    private final Random rng = new Random(251L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public StateAbstraction(int stateDim, int latDim, int hidDim, int numCodes,
                             Mode mode, float commitment, float vqLr, float aeLr) {
        this.stateDim   = stateDim;
        this.latDim     = latDim;
        this.hidDim     = hidDim;
        this.numCodes   = numCodes;
        this.mode       = mode;
        this.commitment = commitment;
        this.vqLr       = vqLr;
        this.aeOpt      = new NeuralNetworkOptimizer(aeLr);

        float s = (float) Math.sqrt(2.0 / (stateDim + latDim));
        encW1 = xav(hidDim, stateDim, s); encB1 = new float[hidDim];
        encW2 = xav(latDim, hidDim,   s); encB2 = new float[latDim];
        decW1 = xav(hidDim, latDim,   s); decB1 = new float[hidDim];
        decW2 = xav(stateDim, hidDim, s); decB2 = new float[stateDim];

        codebook  = xav(numCodes, latDim, s);
        codeUsage = new int[numCodes];

        rProjW = new float[latDim][stateDim];
        for (int i = 0; i < latDim; i++)
            for (int j = 0; j < stateDim; j++) rProjW[i][j] = (float) rng.nextGaussian() / (float) Math.sqrt(latDim);

        Log.i(TAG, "StateAbstraction: s=" + stateDim + "→z=" + latDim + " mode=" + mode);
    }

    public StateAbstraction(int stateDim, int latDim, Mode mode) {
        this(stateDim, latDim, 128, 64, mode, 0.25f, 0.01f, 1e-3f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Encoding (inference)
    // ─────────────────────────────────────────────────────────────────────────

    /** Encode state to abstract representation. Returns float[latDim]. */
    public synchronized float[] encode(float[] state) {
        float[] s = pad(state, stateDim);
        encodeCount.incrementAndGet();
        switch (mode) {
            case VQ_VAE:            return vqEncode(s);
            case RANDOM_PROJECTION: return projEncode(s);
            case BISIMULATION:
            case AUTOENCODER:
            default:                return aeEncode(s);
        }
    }

    /** One-hot discrete code index (VQ-VAE mode). */
    public synchronized int discreteCode(float[] state) {
        float[] z = aeEncode(pad(state, stateDim));
        return nearestCode(z);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Decoding (reconstruction)
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized float[] decode(float[] z) {
        float[] h = lin(decW1, decB1, z, true);
        return lin(decW2, decB2, h, false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Training (reconstruction)
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized float train(float[] state) {
        float[] s = pad(state, stateDim);
        float loss;
        switch (mode) {
            case VQ_VAE: loss = trainVQVAE(s); break;
            default:     loss = trainAE(s);    break;
        }
        avgRecLoss = 0.99f * avgRecLoss + 0.01f * loss;
        trainCount.incrementAndGet();
        return loss;
    }

    public synchronized float trainBatch(List<float[]> batch) {
        float total = 0;
        for (float[] s : batch) total += train(s);
        return batch.isEmpty() ? 0f : total / batch.size();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bisimulation metric
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Bisimulation distance between two states.
     * d(s, s') = ||φ(s) - φ(s')||₂
     */
    public synchronized float bisimDistance(float[] stateA, float[] stateB) {
        float[] zA = encode(stateA);
        float[] zB = encode(stateB);
        float d = 0;
        for (int i = 0; i < latDim; i++) { float diff = zA[i]-zB[i]; d+=diff*diff; }
        return (float) Math.sqrt(d);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal: AE
    // ─────────────────────────────────────────────────────────────────────────

    private float[] aeEncode(float[] s) {
        float[] h = lin(encW1, encB1, s, true);
        return lin(encW2, encB2, h, false);
    }

    private float trainAE(float[] s) {
        // Forward
        float[] h1 = lin(encW1, encB1, s, true);
        float[] z  = lin(encW2, encB2, h1, false);
        float[] h2 = lin(decW1, decB1, z, true);
        float[] sHat = lin(decW2, decB2, h2, false);

        // Reconstruction loss
        float[] dOut = new float[stateDim];
        float loss = 0;
        for (int i = 0; i < stateDim; i++) {
            dOut[i] = 2f * (sHat[i] - s[i]); loss += (sHat[i]-s[i])*(sHat[i]-s[i]);
        }
        loss /= stateDim;

        // Backprop decoder
        float[][] dDecW2 = outerGrad(dOut, h2);
        aeOpt.step("sa_decW2", decW2, dDecW2);
        float[] dH2 = new float[hidDim];
        for (int j=0;j<hidDim;j++){if(h2[j]<=0)continue;for(int i=0;i<stateDim;i++)dH2[j]+=dOut[i]*decW2[i][j];}
        float[][] dDecW1 = outerGrad(dH2, z);
        aeOpt.step("sa_decW1", decW1, dDecW1);

        // Backprop encoder
        float[] dZ = new float[latDim];
        for (int i=0;i<latDim;i++) for(int j=0;j<hidDim;j++) if(h2[j]>0) dZ[i]+=dH2[j]*decW1[j][i];
        float[][] dEncW2 = outerGrad(dZ, h1);
        aeOpt.step("sa_encW2", encW2, dEncW2);
        float[] dH1 = new float[hidDim];
        for (int j=0;j<hidDim;j++){if(h1[j]<=0)continue;for(int i=0;i<latDim;i++)dH1[j]+=dZ[i]*encW2[i][j];}
        float[][] dEncW1 = outerGrad(dH1, s);
        aeOpt.step("sa_encW1", encW1, dEncW1);

        return loss;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal: VQ-VAE
    // ─────────────────────────────────────────────────────────────────────────

    private float[] vqEncode(float[] s) {
        float[] ze = aeEncode(s);
        int idx = nearestCode(ze);
        codeUsage[idx]++;
        return codebook[idx].clone();
    }

    private float trainVQVAE(float[] s) {
        float[] ze   = aeEncode(s);
        int     idx  = nearestCode(ze);
        float[] code = codebook[idx];

        // Decoder takes quantized code
        float[] h2   = lin(decW1, decB1, code, true);
        float[] sHat = lin(decW2, decB2, h2,   false);

        // Reconstruction loss
        float[] dOut = new float[stateDim];
        float loss = 0;
        for (int i=0;i<stateDim;i++){dOut[i]=2f*(sHat[i]-s[i]);loss+=(sHat[i]-s[i])*(sHat[i]-s[i]);}

        // Codebook update via EMA: code ← (1-lr)·code + lr·ze
        for (int j=0;j<latDim;j++) codebook[idx][j]=(1f-vqLr)*code[j]+vqLr*ze[j];

        // Commitment loss gradient on encoder
        float[] dZe = new float[latDim];
        for (int j=0;j<latDim;j++) dZe[j]=commitment*2f*(ze[j]-code[j]);
        float[] h1 = lin(encW1, encB1, s, true);
        float[][] dEncW2 = outerGrad(dZe, h1);
        aeOpt.step("vq_encW2", encW2, dEncW2);

        // Codebook utilisation
        int used = 0; for (int c : codeUsage) if (c > 0) used++;
        avgCbUsage = 0.99f * avgCbUsage + 0.01f * (float)used/numCodes;

        return loss / stateDim;
    }

    private int nearestCode(float[] z) {
        int best = 0; float bestD = Float.MAX_VALUE;
        for (int i = 0; i < numCodes; i++) {
            float d = 0;
            for (int j=0;j<latDim;j++){float diff=z[j]-codebook[i][j];d+=diff*diff;}
            if (d < bestD) { bestD = d; best = i; }
        }
        return best;
    }

    private float[] projEncode(float[] s) {
        float[] o = new float[latDim];
        for (int i=0;i<latDim;i++) for(int j=0;j<stateDim;j++) o[i]+=rProjW[i][j]*s[j];
        return o;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static float[] lin(float[][] W, float[] b, float[] x, boolean relu) {
        float[] o=new float[W.length];
        for(int i=0;i<W.length;i++){float s=b[i];for(int j=0;j<Math.min(x.length,W[i].length);j++)s+=W[i][j]*x[j];o[i]=relu?Math.max(0f,s):s;}
        return o;
    }

    private static float[][] outerGrad(float[] a, float[] b) {
        float[][] g=new float[a.length][b.length];
        for(int i=0;i<a.length;i++) for(int j=0;j<b.length;j++) g[i][j]=a[i]*b[j];
        return g;
    }

    private static float[] pad(float[] x, int dim) {
        if(x.length==dim)return x;
        float[] p=new float[dim];System.arraycopy(x,0,p,0,Math.min(x.length,dim));return p;
    }

    private float[][] xav(int r, int c, float s) {
        float[][] m=new float[r][c];
        for(int i=0;i<r;i++) for(int j=0;j<c;j++) m[i][j]=(rng.nextFloat()*2f-1f)*s;
        return m;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("mode",        mode.name());
        s.put("stateDim",    stateDim);
        s.put("latDim",      latDim);
        s.put("encodeCount", encodeCount.get());
        s.put("trainCount",  trainCount.get());
        s.put("avgRecLoss",  avgRecLoss);
        s.put("avgCbUsage",  avgCbUsage);
        return s;
    }
}
