package com.aiassistant.ml;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * BatchNormalization — layer normalization and batch statistics for on-device RL.
 *
 * Provides three normalization modes suitable for mobile/edge inference:
 *
 *   BATCH_NORM     — Normalize by running batch mean/variance (train mode),
 *                    switch to running stats at inference.
 *   LAYER_NORM     — Normalize across features within a single sample (no batch).
 *                    More suitable for RL (variable-length episodes).
 *   INSTANCE_NORM  — Per-sample per-channel normalization (for 2D feature maps).
 *   RUNNING_NORM   — Online z-score normalization using Welford running stats.
 *                    The simplest, most robust choice for RL on-device.
 *
 * Learnable parameters: γ (scale) and β (shift) per feature dimension.
 *
 * Thread-safe.
 */
public class BatchNormalization {

    private static final String TAG = "BatchNorm";

    public enum Mode { BATCH_NORM, LAYER_NORM, INSTANCE_NORM, RUNNING_NORM }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int    dim;
    private final Mode   mode;
    private final float  epsilon;    // numerical stability
    private final float  momentum;   // for running mean/var update

    // Learnable parameters
    private final float[] gamma;     // scale [dim]
    private final float[] beta;      // shift [dim]
    private final float[] dGamma, dBeta; // accumulated gradients

    // Running statistics (Welford)
    private final double[] runMean, runM2;
    private long           runN = 0;

    // Training mode running stats
    private final double[] bnRunMean, bnRunVar;

    private final AtomicInteger forwardCount = new AtomicInteger(0);
    private boolean             trainMode    = true;

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public BatchNormalization(int dim, Mode mode, float epsilon, float momentum) {
        this.dim      = dim;
        this.mode     = mode;
        this.epsilon  = epsilon;
        this.momentum = momentum;

        gamma  = new float[dim]; java.util.Arrays.fill(gamma, 1f);
        beta   = new float[dim]; // zeros
        dGamma = new float[dim];
        dBeta  = new float[dim];

        runMean  = new double[dim];
        runM2    = new double[dim];
        bnRunMean= new double[dim];
        bnRunVar = new double[dim]; java.util.Arrays.fill(bnRunVar, 1.0);

        Log.i(TAG, "BatchNormalization: dim=" + dim + " mode=" + mode);
    }

    public BatchNormalization(int dim, Mode mode) {
        this(dim, mode, 1e-5f, 0.1f);
    }

    public BatchNormalization(int dim) {
        this(dim, Mode.RUNNING_NORM);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Normalisation (forward)
    // ─────────────────────────────────────────────────────────────────────────

    /** Normalize a single feature vector and apply learned affine transform. */
    public synchronized float[] forward(float[] x) {
        float[] out = new float[dim];
        int d = Math.min(x.length, dim);

        switch (mode) {
            case LAYER_NORM:    layerNorm(x, out, d);    break;
            case BATCH_NORM:    batchNorm(x, out, d);    break;
            case INSTANCE_NORM: instanceNorm(x, out, d); break;
            case RUNNING_NORM:
            default:            runningNorm(x, out, d);  break;
        }

        forwardCount.incrementAndGet();
        return out;
    }

    /** Normalize a batch of vectors (each row is one sample). */
    public synchronized float[][] forwardBatch(float[][] batch) {
        float[][] out = new float[batch.length][];
        for (int i = 0; i < batch.length; i++) out[i] = forward(batch[i]);
        return out;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Backward (gradient of affine params γ, β)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Compute gradient w.r.t. input x and update dGamma/dBeta accumulators.
     *
     * @param normX Normalized x (output of forward before affine).
     * @param dOut  Gradient from next layer [dim].
     * @return dX: gradient w.r.t. original input x.
     */
    public synchronized float[] backward(float[] normX, float[] dOut, float lr) {
        float[] dX = new float[dim];
        int d = Math.min(Math.min(normX.length, dOut.length), dim);
        for (int i = 0; i < d; i++) {
            dGamma[i] += dOut[i] * normX[i];
            dBeta[i]  += dOut[i];
            dX[i]      = dOut[i] * gamma[i];
        }
        // Apply accumulated grads
        for (int i = 0; i < dim; i++) {
            gamma[i] -= lr * dGamma[i]; dGamma[i] = 0f;
            beta[i]  -= lr * dBeta[i];  dBeta[i]  = 0f;
        }
        return dX;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mode implementations
    // ─────────────────────────────────────────────────────────────────────────

    private void layerNorm(float[] x, float[] out, int d) {
        double mean = 0, var = 0;
        for (int i = 0; i < d; i++) mean += x[i];
        mean /= d;
        for (int i = 0; i < d; i++) { double diff = x[i] - mean; var += diff * diff; }
        var = var / d + epsilon;
        float std = (float) Math.sqrt(var);
        for (int i = 0; i < d; i++) out[i] = gamma[i] * (x[i] - (float)mean) / std + beta[i];
    }

    private void runningNorm(float[] x, float[] out, int d) {
        runN++;
        for (int i = 0; i < d; i++) {
            double delta = x[i] - runMean[i];
            runMean[i] += delta / runN;
            runM2[i]   += delta * (x[i] - runMean[i]);
            double std  = runN < 2 ? 1.0 : Math.sqrt(runM2[i] / (runN - 1) + epsilon);
            out[i] = gamma[i] * (float)((x[i] - runMean[i]) / std) + beta[i];
        }
    }

    private void batchNorm(float[] x, float[] out, int d) {
        if (trainMode) {
            // Update running stats
            for (int i = 0; i < d; i++) {
                bnRunMean[i] = (1 - momentum) * bnRunMean[i] + momentum * x[i];
                bnRunVar[i]  = (1 - momentum) * bnRunVar[i]  + momentum * (x[i] - bnRunMean[i]) * (x[i] - bnRunMean[i]);
            }
        }
        for (int i = 0; i < d; i++) {
            float std = (float) Math.sqrt(bnRunVar[i] + epsilon);
            out[i] = gamma[i] * (x[i] - (float)bnRunMean[i]) / std + beta[i];
        }
    }

    private void instanceNorm(float[] x, float[] out, int d) {
        // Treat feature vector as single instance: same as layer norm here
        layerNorm(x, out, d);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Config
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized void setTrainMode(boolean train) { this.trainMode = train; }
    public synchronized void setGamma(float[] g) { System.arraycopy(g, 0, gamma, 0, Math.min(g.length, dim)); }
    public synchronized void setBeta(float[] b)  { System.arraycopy(b, 0, beta,  0, Math.min(b.length, dim)); }
    public synchronized float[] getGamma()        { return gamma.clone(); }
    public synchronized float[] getBeta()         { return beta.clone(); }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("mode",         mode.name());
        s.put("dim",          dim);
        s.put("forwardCount", forwardCount.get());
        s.put("runN",         runN);
        s.put("trainMode",    trainMode);
        return s;
    }
}
