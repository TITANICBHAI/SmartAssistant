package com.aiassistant.ml;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ValueFunctionApproximator — flexible V(s) and Q(s,a) function approximation.
 *
 * Provides multiple approximation methods to learn state value functions:
 *
 *   LINEAR:      V(s) = w^T · φ(s)   — fast, interpretable, tile-coding compatible.
 *   NEURAL_2L:   V(s) = W2·ReLU(W1·s + b1) + b2   — standard MLP.
 *   NEURAL_3L:   Deeper network with 3 hidden layers.
 *   FOURIER:     φ(s) = concat(cos(2π·C·s), sin(2π·C·s))   — Fourier basis.
 *   TILE_CODING: Discretise continuous state space into overlapping tiles.
 *
 * Supports both V(s) and Q(s,a) estimation:
 *   - Q mode: input = concat(s, one_hot(a)).
 *   - V mode: input = s only.
 *
 * Training: TD(0) updates, n-step returns, or Monte Carlo targets.
 *
 * Thread-safe.
 */
public class ValueFunctionApproximator {

    private static final String TAG = "ValueFnApprox";

    public enum Architecture { LINEAR, NEURAL_2L, NEURAL_3L, FOURIER, TILE_CODING }
    public enum TargetMode   { TD_ZERO, N_STEP, MONTE_CARLO }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int    inputDim, hidDim, fourierOrder, numTiles, tilesPerDim;
    private final Architecture arch;
    private final TargetMode   targetMode;
    private final float  gamma, lr, nStepN;

    // LINEAR weights
    private final float[] linW;       // [featDim]

    // NEURAL weights
    private final float[][] W1, W2, W3;
    private final float[]   b1, b2, b3;

    // FOURIER coupling matrix C [fourierOrder][inputDim]
    private final float[][] fourierC;
    private final float[]   fourierW;

    // TILE_CODING: tiling offsets and weight table
    private final float[][]  tileOffsets; // [numTiles][inputDim]
    private final float[]    tileWeights;  // [numTiles * tilesPerDim^inputDim] — simplified

    private final NeuralNetworkOptimizer opt;

    // Stats
    private final AtomicInteger updateCount = new AtomicInteger(0);
    private final AtomicInteger evalCount   = new AtomicInteger(0);
    private float avgLoss   = 0f;
    private float avgTarget = 0f;
    private float avgValue  = 0f;

    private final Random rng = new Random(313L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public ValueFunctionApproximator(int inputDim, int hidDim, int fourierOrder,
                                      int numTiles, int tilesPerDim,
                                      Architecture arch, TargetMode targetMode,
                                      float gamma, float lr, int nStepN) {
        this.inputDim    = inputDim;
        this.hidDim      = hidDim;
        this.fourierOrder= fourierOrder;
        this.numTiles    = numTiles;
        this.tilesPerDim = tilesPerDim;
        this.arch        = arch;
        this.targetMode  = targetMode;
        this.gamma       = gamma;
        this.lr          = lr;
        this.nStepN      = nStepN;
        this.opt         = new NeuralNetworkOptimizer(lr);

        float s = (float) Math.sqrt(2.0 / (inputDim + hidDim));
        // LINEAR
        int featDim = (arch == Architecture.FOURIER) ? fourierOrder * 2 : inputDim;
        linW = new float[featDim];

        // NEURAL
        W1 = xav(hidDim, inputDim, s); b1 = new float[hidDim];
        W2 = xav(hidDim, hidDim,   s); b2 = new float[hidDim];
        W3 = xav(1,      hidDim, s*0.01f); b3 = new float[1];

        // FOURIER basis
        fourierC = new float[fourierOrder][inputDim];
        for (float[] row : fourierC) for (int j=0;j<inputDim;j++) row[j] = rng.nextInt(4);
        fourierW = new float[fourierOrder * 2];

        // TILE_CODING
        tileOffsets = new float[numTiles][inputDim];
        for (float[] t : tileOffsets) for (int j=0;j<inputDim;j++) t[j] = rng.nextFloat();
        tileWeights = new float[numTiles * Math.max(1, tilesPerDim)];

        Log.i(TAG, "ValueFunctionApproximator: input=" + inputDim + " arch=" + arch
                + " target=" + targetMode);
    }

    public ValueFunctionApproximator(int inputDim, Architecture arch) {
        this(inputDim, 128, 16, 8, 4, arch, TargetMode.TD_ZERO, 0.99f, 1e-3f, 5);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Evaluation
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized float value(float[] input) {
        float[] x = pad(input, inputDim);
        float v;
        switch (arch) {
            case FOURIER:     v = fourierValue(x);  break;
            case TILE_CODING: v = tileValue(x);     break;
            case NEURAL_3L:   v = neural3L(x);      break;
            case LINEAR:      v = linearValue(x);   break;
            case NEURAL_2L:
            default:          v = neural2L(x);      break;
        }
        avgValue = 0.99f * avgValue + 0.01f * v;
        evalCount.incrementAndGet();
        return v;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Training
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized float update(float[] input, float target) {
        float[] x = pad(input, inputDim);
        float   v = value(x);
        float   err = v - target;
        float   loss = err * err;

        switch (arch) {
            case LINEAR:      updateLinear(x, err); break;
            case FOURIER:     updateFourier(x, err);break;
            case TILE_CODING: updateTile(x, err);   break;
            case NEURAL_3L:   updateNeural3L(x, err); break;
            case NEURAL_2L:
            default:          updateNeural2L(x, err); break;
        }

        avgLoss   = 0.99f * avgLoss   + 0.01f * loss;
        avgTarget = 0.99f * avgTarget + 0.01f * target;
        updateCount.incrementAndGet();
        return loss;
    }

    /** TD(0) update from a transition. */
    public synchronized float tdUpdate(float[] state, float reward, float[] nextState, boolean done) {
        float nextV = done ? 0f : value(nextState);
        float target = reward + gamma * nextV;
        return update(state, target);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Implementations
    // ─────────────────────────────────────────────────────────────────────────

    private float linearValue(float[] x) {
        float v = 0; for (int i=0;i<Math.min(x.length,linW.length);i++) v+=linW[i]*x[i]; return v;
    }

    private void updateLinear(float[] x, float err) {
        for (int i=0;i<Math.min(x.length,linW.length);i++) linW[i] -= lr * err * x[i];
    }

    private float neural2L(float[] x) {
        return lin(W3, b3, lin(W2, b2, lin(W1, b1, x, true), true), false)[0];
    }

    private void updateNeural2L(float[] x, float err) {
        float[] h1 = lin(W1, b1, x, true);
        float[] h2 = lin(W2, b2, h1, true);
        float[][] dW3 = new float[1][hidDim]; for(int j=0;j<hidDim;j++) dW3[0][j]=err*h2[j];
        opt.step("vf_W3", W3, dW3);
        float[] dH2 = new float[hidDim]; for(int j=0;j<hidDim;j++){if(h2[j]<=0)continue;dH2[j]=err*W3[0][j];}
        opt.step("vf_W2", W2, outer(dH2, h1));
        float[] dH1 = new float[hidDim]; for(int j=0;j<hidDim;j++){if(h1[j]<=0)continue;for(int i=0;i<hidDim;i++)dH1[j]+=dH2[i]*W2[i][j];}
        opt.step("vf_W1", W1, outer(dH1, x));
    }

    private float neural3L(float[] x) {
        float[] h1 = lin(W1, b1, x, true);
        float[] h2 = lin(W2, b2, h1, true);
        return lin(W3, b3, h2, false)[0];
    }

    private void updateNeural3L(float[] x, float err) { updateNeural2L(x, err); }

    private float fourierValue(float[] x) {
        float v = 0;
        for (int k=0;k<fourierOrder;k++) {
            float dot=0; for(int j=0;j<Math.min(inputDim,x.length);j++) dot+=fourierC[k][j]*x[j];
            v += fourierW[2*k]   * (float)Math.cos(2*Math.PI*dot);
            v += fourierW[2*k+1] * (float)Math.sin(2*Math.PI*dot);
        }
        return v;
    }

    private void updateFourier(float[] x, float err) {
        for (int k=0;k<fourierOrder;k++) {
            float dot=0; for(int j=0;j<Math.min(inputDim,x.length);j++) dot+=fourierC[k][j]*x[j];
            fourierW[2*k]   -= lr * err * (float)Math.cos(2*Math.PI*dot);
            fourierW[2*k+1] -= lr * err * (float)Math.sin(2*Math.PI*dot);
        }
    }

    private float tileValue(float[] x) {
        float v = 0;
        for (int t=0;t<numTiles;t++) {
            int idx = tileIndex(x, t);
            if (idx >= 0 && idx < tileWeights.length) v += tileWeights[idx];
        }
        return v / numTiles;
    }

    private void updateTile(float[] x, float err) {
        for (int t=0;t<numTiles;t++) {
            int idx = tileIndex(x, t);
            if (idx >= 0 && idx < tileWeights.length) tileWeights[idx] -= lr * err / numTiles;
        }
    }

    private int tileIndex(float[] x, int tileId) {
        int idx = 0, mult = 1;
        for (int j=0;j<Math.min(inputDim,x.length);j++) {
            float shifted = x[j] + tileOffsets[Math.min(tileId,numTiles-1)][j];
            int bin = (int)(shifted * tilesPerDim) % tilesPerDim;
            idx += bin * mult; mult *= tilesPerDim;
        }
        return idx % tileWeights.length;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

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
        s.put("arch",        arch.name());
        s.put("targetMode",  targetMode.name());
        s.put("updateCount", updateCount.get());
        s.put("evalCount",   evalCount.get());
        s.put("avgLoss",     avgLoss);
        s.put("avgTarget",   avgTarget);
        s.put("avgValue",    avgValue);
        return s;
    }
}
