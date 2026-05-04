package com.aiassistant.ml;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * GradientEstimator — variance reduction and gradient clipping for RL training.
 *
 * Provides four gradient estimation methods:
 *
 *   REINFORCE:
 *     ∇J = E[∇logπ(a|s) · G_t]   (high variance, unbiased)
 *
 *   REINFORCE_BASELINE:
 *     ∇J = E[∇logπ(a|s) · (G_t - b(s))]   (baseline b(s) reduces variance)
 *
 *   ADVANTAGE_AC:
 *     ∇J = E[∇logπ(a|s) · A(s,a)]   (actor-critic advantage estimate)
 *
 *   NATURAL_GRADIENT:
 *     θ ← θ + α · F⁻¹ · ∇J   (F = Fisher information matrix, diagonal approx.)
 *     Scales gradient by inverse curvature → better conditioned updates.
 *
 * Additional utilities:
 *   - Gradient clipping: clip-by-norm and clip-by-value.
 *   - Gradient accumulation: average over multiple mini-batches before update.
 *   - Gradient noise injection: for better exploration (SGLD style).
 *   - EMA of gradient norms for adaptive learning rate warnings.
 *
 * Thread-safe.
 */
public class GradientEstimator {

    private static final String TAG = "GradientEstimator";

    public enum Method { REINFORCE, REINFORCE_BASELINE, ADVANTAGE_AC, NATURAL_GRADIENT }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final Method  method;
    private final int     paramDim;         // number of parameters
    private final float   clipNorm;         // max gradient norm (0 = no clip)
    private final float   clipValue;        // max gradient element (0 = no clip)
    private final float   noiseStd;         // gradient noise std (0 = no noise)
    private final int     accumSteps;       // accumulation steps

    // Baseline (REINFORCE_BASELINE): running mean of returns
    private double  baselineMean = 0, baselineM2 = 0;
    private long    baselineN    = 0;

    // Fisher information diagonal (NATURAL_GRADIENT)
    private final float[] fisherDiag;
    private final float   fisherDecay;    // EMA decay for Fisher update

    // Accumulation buffer
    private final float[][] accumBuffer;
    private int    accumCount = 0;

    // Stats
    private final AtomicInteger estimateCount = new AtomicInteger(0);
    private float avgGradNorm = 0f;
    private float avgVariance = 0f;
    private int   clipEvents  = 0;

    private final Random rng = new Random(263L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public GradientEstimator(int paramDim, Method method, float clipNorm,
                              float clipValue, float noiseStd, int accumSteps,
                              float fisherDecay) {
        this.paramDim   = paramDim;
        this.method     = method;
        this.clipNorm   = clipNorm;
        this.clipValue  = clipValue;
        this.noiseStd   = noiseStd;
        this.accumSteps = Math.max(1, accumSteps);
        this.fisherDecay= fisherDecay;

        fisherDiag  = new float[paramDim];
        java.util.Arrays.fill(fisherDiag, 1f);   // start as identity
        accumBuffer = new float[accumSteps][paramDim];

        Log.i(TAG, "GradientEstimator: method=" + method + " dim=" + paramDim
                + " clipNorm=" + clipNorm);
    }

    public GradientEstimator(int paramDim, Method method) {
        this(paramDim, method, 0.5f, 0f, 0f, 1, 0.99f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Estimate policy gradient for a single trajectory step.
     *
     * @param logProbGrad  ∇logπ(a|s) w.r.t. parameters [paramDim].
     * @param returnValue  G_t or A(s,a) depending on method.
     * @param value        V(s) estimate for baseline (ignored for REINFORCE).
     * @return Processed gradient estimate [paramDim].
     */
    public synchronized float[] estimate(float[] logProbGrad, float returnValue, float value) {
        float[] raw = new float[paramDim];
        int d = Math.min(logProbGrad.length, paramDim);

        float weight;
        switch (method) {
            case REINFORCE_BASELINE:
                float baseline = updateBaseline(returnValue);
                weight = returnValue - baseline;
                break;
            case ADVANTAGE_AC:
                weight = returnValue - value;   // returnValue = TD target, value = V(s)
                break;
            case NATURAL_GRADIENT:
                weight = returnValue;
                break;
            case REINFORCE:
            default:
                weight = returnValue;
                break;
        }

        for (int i = 0; i < d; i++) raw[i] = logProbGrad[i] * weight;

        // Natural gradient: scale by inverse Fisher
        if (method == Method.NATURAL_GRADIENT) {
            for (int i = 0; i < paramDim; i++)
                raw[i] /= (fisherDiag[i] + 1e-8f);
            updateFisher(logProbGrad, weight);
        }

        // Accumulation
        System.arraycopy(raw, 0, accumBuffer[accumCount % accumSteps], 0, paramDim);
        accumCount++;
        float[] accumulated = new float[paramDim];
        int n = Math.min(accumCount, accumSteps);
        for (int k = 0; k < n; k++)
            for (int i = 0; i < paramDim; i++) accumulated[i] += accumBuffer[k][i];
        for (int i = 0; i < paramDim; i++) accumulated[i] /= n;

        // Clip
        accumulated = clip(accumulated);

        // Noise injection
        if (noiseStd > 0) {
            for (int i = 0; i < paramDim; i++)
                accumulated[i] += noiseStd * (float) rng.nextGaussian();
        }

        // Track stats
        float norm = norm(accumulated);
        avgGradNorm = 0.99f * avgGradNorm + 0.01f * norm;
        estimateCount.incrementAndGet();
        return accumulated;
    }

    /** Clip-by-norm only (for convenience). */
    public synchronized float[] clipByNorm(float[] grad, float maxNorm) {
        float n = norm(grad);
        if (n > maxNorm) {
            float scale = maxNorm / n;
            float[] out = new float[grad.length];
            for (int i = 0; i < grad.length; i++) out[i] = grad[i] * scale;
            clipEvents++;
            return out;
        }
        return grad;
    }

    /** Update Fisher diagonal EMA: F_i ← decay·F_i + (1-decay)·(∂logπ/∂θ_i)² */
    public synchronized void updateFisher(float[] logProbGrad, float weight) {
        for (int i = 0; i < Math.min(logProbGrad.length, paramDim); i++) {
            float g2 = logProbGrad[i] * logProbGrad[i] * weight * weight;
            fisherDiag[i] = fisherDecay * fisherDiag[i] + (1f - fisherDecay) * g2;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private float[] clip(float[] g) {
        if (clipNorm > 0) { float n = norm(g); if (n > clipNorm) { clipEvents++; float s=clipNorm/n; for(int i=0;i<g.length;i++)g[i]*=s; } }
        if (clipValue > 0) for (int i = 0; i < g.length; i++) g[i]=Math.max(-clipValue,Math.min(clipValue,g[i]));
        return g;
    }

    private float updateBaseline(float G) {
        baselineN++;
        double delta = G - baselineMean;
        baselineMean += delta / baselineN;
        baselineM2   += delta * (G - baselineMean);
        return (float) baselineMean;
    }

    private static float norm(float[] v) {
        float s = 0; for (float x : v) s += x*x; return (float)Math.sqrt(s);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("method",        method.name());
        s.put("estimateCount", estimateCount.get());
        s.put("avgGradNorm",   avgGradNorm);
        s.put("clipEvents",    clipEvents);
        s.put("baselineMean",  baselineMean);
        s.put("accumSteps",    accumSteps);
        return s;
    }
}
