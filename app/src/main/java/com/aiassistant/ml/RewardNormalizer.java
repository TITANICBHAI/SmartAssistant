package com.aiassistant.ml;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RewardNormalizer — online reward normalization for stable RL training.
 *
 * Implements three strategies selectable per instance:
 *
 *   RUNNING_MEAN_STD  — z-score: r̃ = (r − μ) / (σ + ε)
 *                       Uses Welford's online algorithm for numerical stability.
 *
 *   RUNNING_MIN_MAX   — min-max: r̃ = (r − min) / (max − min + ε)
 *                       Running min/max tracked over all observed rewards.
 *
 *   RETURN_WHITENING  — normalize episode returns: r̃ = r / std(G)
 *                       where G is accumulated per-episode return.
 *                       Keeps a buffer of recent episode returns.
 *
 * All strategies:
 *   - Clip final output to [-clipRange, +clipRange]
 *   - Expose running statistics for monitoring
 *   - Thread-safe
 */
public class RewardNormalizer {

    private static final String TAG = "RewardNormalizer";

    public enum Strategy { RUNNING_MEAN_STD, RUNNING_MIN_MAX, RETURN_WHITENING }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final Strategy strategy;
    private final float    clipRange;
    private final float    epsilon;

    // Welford stats
    private double   wMean = 0.0, wM2 = 0.0;
    private final AtomicLong wCount = new AtomicLong(0);

    // Min-max stats
    private double   runMin = Double.MAX_VALUE;
    private double   runMax = -Double.MAX_VALUE;

    // Return whitening
    private final int     returnBufSize;
    private final double[] returnBuf;
    private int            returnBufHead = 0;
    private int            returnBufFill = 0;
    private double   episodeReturn = 0.0;
    private double   returnStd     = 1.0;

    // EMA of normalised reward (for monitoring)
    private float lastNormalized = 0f;
    private float emaOutput      = 0f;

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public RewardNormalizer(Strategy strategy, float clipRange,
                            float epsilon, int returnBufSize) {
        this.strategy       = strategy;
        this.clipRange      = clipRange;
        this.epsilon        = epsilon;
        this.returnBufSize  = returnBufSize;
        this.returnBuf      = new double[returnBufSize];
    }

    public RewardNormalizer(Strategy strategy) {
        this(strategy, 10f, 1e-8f, 100);
    }

    public RewardNormalizer() {
        this(Strategy.RUNNING_MEAN_STD, 10f, 1e-8f, 100);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Normalize a single reward value.
     * Updates running statistics as a side effect.
     *
     * @param reward Raw environment reward.
     * @param done   True if this is the last step of an episode (for RETURN_WHITENING).
     * @return Normalised, clipped reward.
     */
    public synchronized float normalize(float reward, boolean done) {
        float out;
        switch (strategy) {
            case RUNNING_MEAN_STD:
                updateWelford(reward);
                out = welfordNormalize(reward);
                break;
            case RUNNING_MIN_MAX:
                updateMinMax(reward);
                out = minMaxNormalize(reward);
                break;
            case RETURN_WHITENING:
                episodeReturn += reward;
                if (done) {
                    pushReturn(episodeReturn);
                    returnStd     = computeReturnStd();
                    episodeReturn = 0.0;
                }
                out = (float)(reward / (returnStd + epsilon));
                break;
            default:
                out = reward;
        }
        out = Math.max(-clipRange, Math.min(clipRange, out));
        lastNormalized = out;
        emaOutput      = 0.95f * emaOutput + 0.05f * out;
        return out;
    }

    /** Normalize without updating statistics (inference-only). */
    public synchronized float normalizeNoUpdate(float reward) {
        float out;
        switch (strategy) {
            case RUNNING_MEAN_STD: out = welfordNormalize(reward); break;
            case RUNNING_MIN_MAX:  out = minMaxNormalize(reward);  break;
            default:               out = (float)(reward / (returnStd + epsilon)); break;
        }
        return Math.max(-clipRange, Math.min(clipRange, out));
    }

    /** Denormalize: convert a normalised reward back to the original scale. */
    public synchronized float denormalize(float normReward) {
        if (strategy == Strategy.RUNNING_MEAN_STD) {
            double std = wCount.get() < 2 ? 1.0 : Math.sqrt(wM2 / (wCount.get() - 1));
            return (float)(normReward * (std + epsilon) + wMean);
        }
        if (strategy == Strategy.RUNNING_MIN_MAX) {
            return (float)(normReward * (runMax - runMin + epsilon) + runMin);
        }
        return (float)(normReward * (returnStd + epsilon));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized double getMean()   { return wMean; }
    public synchronized double getStd()    { return wCount.get() < 2 ? 1.0 : Math.sqrt(wM2 / (wCount.get() - 1)); }
    public synchronized double getMin()    { return runMin == Double.MAX_VALUE  ? 0.0 : runMin; }
    public synchronized double getMax()    { return runMax == -Double.MAX_VALUE ? 0.0 : runMax; }
    public long   getSampleCount()         { return wCount.get(); }

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("strategy",       strategy.name());
        s.put("sampleCount",    wCount.get());
        s.put("mean",           wMean);
        s.put("std",            getStd());
        s.put("min",            getMin());
        s.put("max",            getMax());
        s.put("returnStd",      returnStd);
        s.put("clipRange",      clipRange);
        s.put("lastNormalized", lastNormalized);
        s.put("emaOutput",      emaOutput);
        return s;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal
    // ─────────────────────────────────────────────────────────────────────────

    private void updateWelford(double x) {
        long n    = wCount.incrementAndGet();
        double d  = x - wMean;
        wMean    += d / n;
        wM2      += d * (x - wMean);
    }

    private float welfordNormalize(double x) {
        double std = wCount.get() < 2 ? 1.0 : Math.sqrt(wM2 / (wCount.get() - 1));
        return (float)((x - wMean) / (std + epsilon));
    }

    private void updateMinMax(double x) {
        if (x < runMin) runMin = x;
        if (x > runMax) runMax = x;
    }

    private float minMaxNormalize(double x) {
        double range = runMax - runMin + epsilon;
        return (float)((x - runMin) / range);
    }

    private void pushReturn(double G) {
        returnBuf[returnBufHead] = G;
        returnBufHead = (returnBufHead + 1) % returnBufSize;
        if (returnBufFill < returnBufSize) returnBufFill++;
    }

    private double computeReturnStd() {
        if (returnBufFill < 2) return 1.0;
        double mean = 0;
        for (int i = 0; i < returnBufFill; i++) mean += returnBuf[i];
        mean /= returnBufFill;
        double var = 0;
        for (int i = 0; i < returnBufFill; i++) { double d = returnBuf[i] - mean; var += d * d; }
        var /= returnBufFill;
        return Math.sqrt(var + epsilon);
    }
}
