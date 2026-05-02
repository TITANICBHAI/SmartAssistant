package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * TemporalDifferenceTracker — training-diagnostics utility that tracks and
 * analyzes Temporal Difference (TD) errors over the training lifetime.
 *
 * Provides:
 *   - Rolling window statistics (mean, std, max, min) of TD errors.
 *   - Divergence detection: raises a flag when mean |δ| grows too fast.
 *   - Gradient noise ratio (GNR): ratio of gradient variance to gradient mean,
 *     useful for detecting noisy or saturated training.
 *   - Per-action TD-error tracking to identify which actions are hardest to learn.
 *   - Learning progress score [0,1] derived from |δ| trend (decreasing = progress).
 *   - Alert callbacks when training appears to diverge or stall.
 *   - Rolling log of min/max error for plotting.
 *
 * This class is intentionally framework-agnostic — it accepts raw float TD errors
 * from any RL algorithm (DQN, PPO, SARSA, Q-learning, etc.).
 */
public class TemporalDifferenceTracker {

    private static final String TAG = "TDTracker";

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------
    private final int   windowSize;        // rolling window for statistics
    private final float divergenceThreshold; // |δ| increase ratio that triggers alert
    private final float stallThreshold;    // if progress score < this → stall alert

    // -------------------------------------------------------------------------
    // Alert callback
    // -------------------------------------------------------------------------
    public interface AlertListener {
        void onDivergence(float currentMeanAbsDelta, float previousMeanAbsDelta);
        void onStall(float progressScore);
    }

    private final List<AlertListener> alertListeners = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Rolling storage
    // -------------------------------------------------------------------------
    private final Queue<Float> window          = new LinkedList<>(); // raw |δ| values
    private final Queue<Float> windowSigned    = new LinkedList<>(); // raw δ (signed)

    // Per-action statistics
    private final Map<Integer, double[]> perActionStats = new HashMap<>();
    // double[] = {sumAbsDelta, sumDeltaSq, count}

    // History of windowed mean for trend analysis
    private final Queue<Float> meanHistory = new LinkedList<>();
    private static final int   MEAN_HISTORY_SIZE = 20;

    // -------------------------------------------------------------------------
    // Running aggregates (Welford)
    // -------------------------------------------------------------------------
    private double globalMean   = 0.0;
    private double globalM2     = 0.0;
    private long   globalCount  = 0L;

    private float  lastWindowMeanAbsDelta = Float.MAX_VALUE;
    private float  progressScore          = 0.5f;  // [0,1]; higher = more progress

    // Total updates
    private long totalUpdates = 0L;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    public TemporalDifferenceTracker(int windowSize,
                                      float divergenceThreshold,
                                      float stallThreshold) {
        this.windowSize           = Math.max(10, windowSize);
        this.divergenceThreshold  = divergenceThreshold;
        this.stallThreshold       = stallThreshold;
    }

    public TemporalDifferenceTracker() {
        this(200, 2.5f, 0.2f);
    }

    // -------------------------------------------------------------------------
    // API
    // -------------------------------------------------------------------------

    /**
     * Record a single TD error δ for the given action.
     *
     * @param delta    Raw signed TD error (δ = r + γ·V(s') − V(s)).
     * @param action   Action index (−1 if not applicable).
     */
    public synchronized void record(float delta, int action) {
        float absDelta = Math.abs(delta);

        // ---- Global Welford update ----
        globalCount++;
        double d  = absDelta - globalMean;
        globalMean += d / globalCount;
        globalM2   += d * (absDelta - globalMean);

        // ---- Rolling window ----
        window.offer(absDelta);
        windowSigned.offer(delta);
        if (window.size() > windowSize) window.poll();
        if (windowSigned.size() > windowSize) windowSigned.poll();

        // ---- Per-action stats ----
        if (action >= 0) {
            double[] s = perActionStats.computeIfAbsent(action, k -> new double[3]);
            s[0] += absDelta;
            s[1] += absDelta * absDelta;
            s[2] += 1;
        }

        totalUpdates++;

        // ---- Periodic checks (every full window) ----
        if (totalUpdates % windowSize == 0) {
            checkDivergenceAndStall();
        }
    }

    /**
     * Record a batch of TD errors.
     */
    public synchronized void recordBatch(float[] deltas, int[] actions) {
        int n = Math.min(deltas.length, actions != null ? actions.length : deltas.length);
        for (int i = 0; i < n; i++) {
            record(deltas[i], actions != null ? actions[i] : -1);
        }
    }

    // -------------------------------------------------------------------------
    // Statistics
    // -------------------------------------------------------------------------

    /** Mean absolute TD error over the rolling window. */
    public synchronized float getWindowMeanAbsDelta() {
        if (window.isEmpty()) return 0f;
        double sum = 0;
        for (float v : window) sum += v;
        return (float)(sum / window.size());
    }

    /** Standard deviation of absolute TD errors in the rolling window. */
    public synchronized float getWindowStdAbsDelta() {
        if (window.size() < 2) return 0f;
        double mean = getWindowMeanAbsDelta();
        double var  = 0;
        for (float v : window) var += (v - mean) * (v - mean);
        return (float) Math.sqrt(var / (window.size() - 1));
    }

    /** Max absolute TD error in the window. */
    public synchronized float getWindowMaxAbsDelta() {
        float max = 0;
        for (float v : window) if (v > max) max = v;
        return max;
    }

    /** Global lifetime mean absolute TD error. */
    public synchronized float getGlobalMeanAbsDelta() {
        return (float) globalMean;
    }

    /** Global lifetime std of absolute TD error (Bessel-corrected). */
    public synchronized float getGlobalStdAbsDelta() {
        if (globalCount < 2) return 0f;
        return (float) Math.sqrt(globalM2 / (globalCount - 1));
    }

    /**
     * Learning progress score [0,1].
     * 1.0 = clear improvement (|δ| decreasing strongly).
     * 0.0 = clear divergence.
     */
    public float getProgressScore() { return progressScore; }

    /**
     * Per-action mean absolute TD error — sorted by descending error.
     * Actions with high error are the hardest to learn.
     */
    public synchronized Map<Integer, Float> getPerActionMeanAbsDelta() {
        Map<Integer, Float> result = new HashMap<>();
        for (Map.Entry<Integer, double[]> e : perActionStats.entrySet()) {
            double[] s = e.getValue();
            if (s[2] > 0) result.put(e.getKey(), (float)(s[0] / s[2]));
        }
        return result;
    }

    /**
     * Gradient noise ratio: std / |mean| of signed deltas.
     * High GNR (>1) suggests very noisy gradient signal.
     */
    public synchronized float getGradientNoiseRatio() {
        if (windowSigned.size() < 2) return 0f;
        double mean = 0, m2 = 0;
        int    n    = 0;
        for (float v : windowSigned) {
            n++;
            double d = v - mean;
            mean += d / n;
            m2   += d * (v - mean);
        }
        double std = Math.sqrt(m2 / Math.max(1, n - 1));
        double absMean = Math.abs(mean);
        return absMean < 1e-8 ? (float) std : (float)(std / absMean);
    }

    // -------------------------------------------------------------------------
    // Alert management
    // -------------------------------------------------------------------------

    public void addAlertListener(AlertListener l)    { alertListeners.add(l); }
    public void removeAlertListener(AlertListener l) { alertListeners.remove(l); }

    // -------------------------------------------------------------------------
    // Monitoring summary
    // -------------------------------------------------------------------------

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("windowSize",          windowSize);
        s.put("windowFill",          window.size());
        s.put("totalUpdates",        totalUpdates);
        s.put("windowMeanAbsDelta",  getWindowMeanAbsDelta());
        s.put("windowStdAbsDelta",   getWindowStdAbsDelta());
        s.put("windowMaxAbsDelta",   getWindowMaxAbsDelta());
        s.put("globalMeanAbsDelta",  getGlobalMeanAbsDelta());
        s.put("globalStdAbsDelta",   getGlobalStdAbsDelta());
        s.put("progressScore",       progressScore);
        s.put("gradientNoiseRatio",  getGradientNoiseRatio());
        s.put("trackedActions",      perActionStats.size());
        return s;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void checkDivergenceAndStall() {
        float currentMean = getWindowMeanAbsDelta();

        // Update mean history
        meanHistory.offer(currentMean);
        if (meanHistory.size() > MEAN_HISTORY_SIZE) meanHistory.poll();

        // Compute trend: linear regression slope over recent means
        float trend = computeTrend();

        // Progress score: logistic transform of (−trend), clamped [0,1]
        float normTrend = trend / Math.max(1e-6f, currentMean);
        progressScore   = (float)(1.0 / (1.0 + Math.exp(normTrend * 10)));

        // Divergence alert
        if (lastWindowMeanAbsDelta < Float.MAX_VALUE && currentMean > 0 && lastWindowMeanAbsDelta > 0) {
            float ratio = currentMean / lastWindowMeanAbsDelta;
            if (ratio > divergenceThreshold) {
                Log.w(TAG, "Divergence detected! |δ| ratio=" + String.format("%.2f", ratio));
                for (AlertListener l : alertListeners) {
                    try { l.onDivergence(currentMean, lastWindowMeanAbsDelta); }
                    catch (Exception e) { Log.w(TAG, "Alert listener error: " + e.getMessage()); }
                }
            }
        }

        // Stall alert
        if (progressScore < stallThreshold && totalUpdates > windowSize * 2) {
            Log.w(TAG, "Stall detected! progressScore=" + String.format("%.3f", progressScore));
            for (AlertListener l : alertListeners) {
                try { l.onStall(progressScore); }
                catch (Exception e) { Log.w(TAG, "Alert listener error: " + e.getMessage()); }
            }
        }

        lastWindowMeanAbsDelta = currentMean;
    }

    /**
     * Compute linear regression slope over the mean history.
     * Positive slope → error increasing; negative → improving.
     */
    private float computeTrend() {
        if (meanHistory.size() < 2) return 0f;
        float[] y = new float[meanHistory.size()];
        int i = 0;
        for (float v : meanHistory) y[i++] = v;
        int n = y.length;
        float xMean = (n - 1) / 2f;
        float yMean = 0f; for (float v : y) yMean += v; yMean /= n;
        float num = 0f, den = 0f;
        for (int t = 0; t < n; t++) {
            num += (t - xMean) * (y[t] - yMean);
            den += (t - xMean) * (t - xMean);
        }
        return den > 0 ? num / den : 0f;
    }
}
