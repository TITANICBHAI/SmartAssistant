package com.aiassistant.ml;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AdaptiveLearningRate — per-parameter adaptive learning rate algorithms.
 *
 * Provides standalone implementations of the most important adaptive optimizers
 * for use anywhere in the ML pipeline (not just NeuralNetworkOptimizer):
 *
 *   ADAM:     m_t = β1·m_{t-1} + (1-β1)·g_t
 *             v_t = β2·v_{t-1} + (1-β2)·g_t²
 *             θ_t ← θ_{t-1} - α·m̂_t/(√v̂_t + ε)
 *
 *   ADAMW:    ADAM + weight decay (decoupled regularisation).
 *
 *   RMSPROP:  v_t = ρ·v_{t-1} + (1-ρ)·g_t²
 *             θ_t ← θ_{t-1} - α·g_t/√(v_t + ε)
 *
 *   ADAGRAD:  G_t += g_t²
 *             θ_t ← θ_{t-1} - α·g_t/√(G_t + ε)
 *
 *   SGD_MOMENTUM: m_t = μ·m_{t-1} + g_t;  θ ← θ - α·m_t
 *
 *   WARMUP_COSINE: learning rate warm-up followed by cosine annealing.
 *     - Linearly increases lr from 0 → peak over warmupSteps.
 *     - Then cosine decays from peak → minLr.
 *
 * Works on flat parameter vectors (compatible with all weight matrices via flatten/reshape).
 * Thread-safe.
 */
public class AdaptiveLearningRate {

    private static final String TAG = "AdaptiveLR";

    public enum Optimizer { ADAM, ADAMW, RMSPROP, ADAGRAD, SGD_MOMENTUM, WARMUP_COSINE }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final Optimizer optimizer;
    private final int       paramDim;
    private       float     lr;
    private final float     peakLr;
    private final float     minLr;
    private final float     beta1, beta2;  // ADAM
    private final float     rho;           // RMSProp
    private final float     mu;            // momentum
    private final float     weightDecay;   // AdamW
    private final float     epsilon;
    private final int       warmupSteps, totalSteps;

    // State
    private final float[] m;     // first moment / momentum
    private final float[] v;     // second moment
    private final float[] G;     // AdaGrad sum
    private int   step = 0;

    private final AtomicInteger updateCount = new AtomicInteger(0);
    private float avgGradNorm   = 0f;
    private float avgParamNorm  = 0f;
    private float effectiveLr   = 0f;

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public AdaptiveLearningRate(int paramDim, Optimizer optimizer,
                                 float lr, float peakLr, float minLr,
                                 float beta1, float beta2, float rho, float mu,
                                 float weightDecay, float epsilon,
                                 int warmupSteps, int totalSteps) {
        this.paramDim   = paramDim;
        this.optimizer  = optimizer;
        this.lr         = lr;
        this.peakLr     = peakLr;
        this.minLr      = minLr;
        this.beta1      = beta1;
        this.beta2      = beta2;
        this.rho        = rho;
        this.mu         = mu;
        this.weightDecay= weightDecay;
        this.epsilon    = epsilon;
        this.warmupSteps= warmupSteps;
        this.totalSteps = totalSteps;

        m = new float[paramDim];
        v = new float[paramDim];
        G = new float[paramDim];
        java.util.Arrays.fill(v, 1f);

        Log.i(TAG, "AdaptiveLearningRate: " + optimizer + " lr=" + lr + " dim=" + paramDim);
    }

    public static AdaptiveLearningRate adam(int paramDim, float lr) {
        return new AdaptiveLearningRate(paramDim, Optimizer.ADAM, lr, lr, lr*0.01f,
                0.9f, 0.999f, 0.99f, 0.9f, 0f, 1e-8f, 0, 0);
    }

    public static AdaptiveLearningRate adamW(int paramDim, float lr, float wd) {
        return new AdaptiveLearningRate(paramDim, Optimizer.ADAMW, lr, lr, lr*0.01f,
                0.9f, 0.999f, 0.99f, 0.9f, wd, 1e-8f, 0, 0);
    }

    public static AdaptiveLearningRate warmupCosine(int paramDim, float peakLr,
                                                     float minLr, int warmup, int total) {
        return new AdaptiveLearningRate(paramDim, Optimizer.WARMUP_COSINE, 0f, peakLr, minLr,
                0.9f, 0.999f, 0.99f, 0.9f, 0f, 1e-8f, warmup, total);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /** Apply one gradient step. Returns updated parameters. */
    public synchronized float[] update(float[] params, float[] grads) {
        if (params.length != paramDim || grads.length != paramDim) {
            Log.w(TAG, "Dimension mismatch: expected " + paramDim);
            return params;
        }
        step++;
        float currentLr = computeLr();
        float[] updated = params.clone();

        switch (optimizer) {
            case ADAM:          applyAdam(updated, grads, currentLr, false); break;
            case ADAMW:         applyAdam(updated, grads, currentLr, true);  break;
            case RMSPROP:       applyRmsProp(updated, grads, currentLr);     break;
            case ADAGRAD:       applyAdaGrad(updated, grads, currentLr);     break;
            case SGD_MOMENTUM:  applySgdMomentum(updated, grads, currentLr); break;
            case WARMUP_COSINE: applyAdam(updated, grads, currentLr, false); break;
        }

        float gn = 0, pn = 0;
        for (int i=0;i<paramDim;i++) { gn+=grads[i]*grads[i]; pn+=updated[i]*updated[i]; }
        avgGradNorm  = 0.99f * avgGradNorm  + 0.01f * (float)Math.sqrt(gn);
        avgParamNorm = 0.99f * avgParamNorm + 0.01f * (float)Math.sqrt(pn);
        effectiveLr  = currentLr;
        updateCount.incrementAndGet();
        return updated;
    }

    /** Flatten 2D weight matrix and apply update, then reshape. */
    public synchronized float[][] updateMatrix(float[][] W, float[][] dW) {
        int r = W.length, c = W[0].length;
        float[] flat = new float[r*c], dFlat = new float[r*c];
        for (int i=0;i<r;i++) for(int j=0;j<c;j++) { flat[i*c+j]=W[i][j]; dFlat[i*c+j]=dW[i][j]; }

        if (flat.length != paramDim) { Log.w(TAG, "Matrix dim mismatch"); return W; }
        float[] updated = update(flat, dFlat);

        float[][] result = new float[r][c];
        for (int i=0;i<r;i++) for(int j=0;j<c;j++) result[i][j]=updated[i*c+j];
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Optimizer implementations
    // ─────────────────────────────────────────────────────────────────────────

    private void applyAdam(float[] params, float[] grads, float currentLr, boolean decoupled) {
        float b1c = (float)(1 - Math.pow(beta1, step));
        float b2c = (float)(1 - Math.pow(beta2, step));
        for (int i=0;i<paramDim;i++) {
            m[i] = beta1 * m[i] + (1-beta1) * grads[i];
            v[i] = beta2 * v[i] + (1-beta2) * grads[i] * grads[i];
            float mHat = m[i] / b1c;
            float vHat = v[i] / b2c;
            float step_size = currentLr / ((float)Math.sqrt(vHat) + epsilon);
            params[i] -= step_size * mHat;
            if (decoupled) params[i] -= currentLr * weightDecay * params[i];
        }
    }

    private void applyRmsProp(float[] params, float[] grads, float currentLr) {
        for (int i=0;i<paramDim;i++) {
            v[i] = rho * v[i] + (1-rho) * grads[i] * grads[i];
            params[i] -= currentLr * grads[i] / ((float)Math.sqrt(v[i]) + epsilon);
        }
    }

    private void applyAdaGrad(float[] params, float[] grads, float currentLr) {
        for (int i=0;i<paramDim;i++) {
            G[i] += grads[i] * grads[i];
            params[i] -= currentLr * grads[i] / ((float)Math.sqrt(G[i]) + epsilon);
        }
    }

    private void applySgdMomentum(float[] params, float[] grads, float currentLr) {
        for (int i=0;i<paramDim;i++) {
            m[i] = mu * m[i] + grads[i];
            params[i] -= currentLr * m[i];
        }
    }

    private float computeLr() {
        if (optimizer != Optimizer.WARMUP_COSINE) return lr;
        if (step <= warmupSteps) return peakLr * step / Math.max(1, warmupSteps);
        float progress = (float)(step - warmupSteps) / Math.max(1, totalSteps - warmupSteps);
        return minLr + (peakLr - minLr) * 0.5f * (1f + (float)Math.cos(Math.PI * progress));
    }

    public synchronized void setLr(float newLr) { this.lr = newLr; }
    public synchronized float getLr()           { return effectiveLr; }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("optimizer",   optimizer.name());
        s.put("step",        step);
        s.put("effectiveLr", effectiveLr);
        s.put("updateCount", updateCount.get());
        s.put("avgGradNorm", avgGradNorm);
        s.put("avgParamNorm",avgParamNorm);
        return s;
    }
}
