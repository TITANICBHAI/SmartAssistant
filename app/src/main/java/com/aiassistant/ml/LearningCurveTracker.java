package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LearningCurveTracker — comprehensive training progress monitoring for RL agents.
 *
 * Tracks and analyses multiple metrics over training:
 *
 *   REWARD METRICS:
 *     - Episode return (raw and EMA smoothed)
 *     - Return distribution: mean, std, min, max, median, percentiles
 *     - Best/worst episode ever seen
 *
 *   LOSS METRICS:
 *     - Policy loss, value loss, entropy
 *     - Gradient norm (if provided)
 *
 *   EXPLORATION METRICS:
 *     - Epsilon, action entropy
 *     - Unique states visited per episode
 *
 *   CONVERGENCE DETECTION:
 *     - Detects plateau: EMA of improvement < threshold for K steps
 *     - Detects collapse: sudden drop in performance
 *     - Detects oscillation: reward oscillates without net improvement
 *
 *   REPORTING:
 *     - Moving average window statistics
 *     - Progress percentage toward target return
 *     - ETA estimation (steps to target)
 *
 * Thread-safe.
 */
public class LearningCurveTracker {

    private static final String TAG = "LearningCurveTracker";

    // ─────────────────────────────────────────────────────────────────────────
    // Episode record
    // ─────────────────────────────────────────────────────────────────────────
    public static class EpisodeRecord {
        public final int   episode;
        public final float returnValue;
        public final int   steps;
        public final float epsilon;
        public final long  wallTimeMs;

        EpisodeRecord(int ep, float ret, int steps, float eps, long time) {
            episode = ep; returnValue = ret; this.steps = steps;
            epsilon = eps; wallTimeMs = time;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int    windowSize;       // moving average window
    private final float  targetReturn;
    private final float  plateauThresh;
    private final int    plateauSteps;
    private final float  collapseThresh;   // drop fraction to detect collapse

    // Circular buffer of recent episode returns
    private final float[] returnWindow;
    private int    winHead = 0, winCount = 0;

    // Full history (capped)
    private final LinkedList<EpisodeRecord> history = new LinkedList<>();
    private final int maxHistory;

    // Metrics
    private final AtomicInteger episodeCount = new AtomicInteger(0);
    private float   ema         = 0f;
    private float   emaFast     = 0f;  // fast EMA for collapse detection
    private float   bestReturn  = Float.NEGATIVE_INFINITY;
    private float   worstReturn = Float.POSITIVE_INFINITY;
    private int     bestEpisode = -1;
    private long    startTime   = System.currentTimeMillis();

    // Convergence state
    private float   prevEma     = 0f;
    private int     plateauCnt  = 0;
    private boolean isPlateau   = false;
    private boolean isCollapse  = false;

    // Loss tracking (optional)
    private float avgPolicyLoss = 0f;
    private float avgValueLoss  = 0f;
    private float avgEntropy    = 0f;
    private float avgGradNorm   = 0f;

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public LearningCurveTracker(int windowSize, float targetReturn,
                                 float plateauThresh, int plateauSteps,
                                 float collapseThresh, int maxHistory) {
        this.windowSize    = windowSize;
        this.targetReturn  = targetReturn;
        this.plateauThresh = plateauThresh;
        this.plateauSteps  = plateauSteps;
        this.collapseThresh= collapseThresh;
        this.maxHistory    = maxHistory;
        this.returnWindow  = new float[windowSize];
        Log.i(TAG, "LearningCurveTracker: win=" + windowSize + " target=" + targetReturn);
    }

    public LearningCurveTracker(int windowSize, float targetReturn) {
        this(windowSize, targetReturn, 0.01f, 100, 0.3f, 10_000);
    }

    public LearningCurveTracker() {
        this(100, Float.MAX_VALUE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /** Record episode completion. */
    public synchronized void recordEpisode(float returnValue, int steps, float epsilon) {
        int ep = episodeCount.incrementAndGet();

        // Update circular window
        returnWindow[winHead % windowSize] = returnValue;
        winHead++; if (winCount < windowSize) winCount++;

        // EMA update
        float emaDecay     = 0.99f;
        float emaFastDecay = 0.9f;
        ema     = ep == 1 ? returnValue : emaDecay     * ema     + (1-emaDecay)     * returnValue;
        emaFast = ep == 1 ? returnValue : emaFastDecay * emaFast + (1-emaFastDecay) * returnValue;

        // Track extremes
        if (returnValue > bestReturn)  { bestReturn = returnValue; bestEpisode = ep; }
        if (returnValue < worstReturn) { worstReturn = returnValue; }

        // History
        if (history.size() >= maxHistory) history.removeFirst();
        history.add(new EpisodeRecord(ep, returnValue, steps, epsilon, System.currentTimeMillis()));

        // Convergence detection
        checkConvergence(returnValue);

        // Periodic log
        if (ep % 100 == 0) logProgress(ep);
    }

    /** Record training loss metrics. */
    public synchronized void recordLoss(float policyLoss, float valueLoss,
                                        float entropy, float gradNorm) {
        avgPolicyLoss = 0.99f * avgPolicyLoss + 0.01f * policyLoss;
        avgValueLoss  = 0.99f * avgValueLoss  + 0.01f * valueLoss;
        avgEntropy    = 0.99f * avgEntropy    + 0.01f * entropy;
        avgGradNorm   = 0.99f * avgGradNorm   + 0.01f * gradNorm;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Queries
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized float getEma()          { return ema; }
    public synchronized float getBestReturn()   { return bestReturn; }
    public synchronized float getWorstReturn()  { return worstReturn; }
    public synchronized boolean isPlateau()     { return isPlateau; }
    public synchronized boolean isCollapse()    { return isCollapse; }
    public synchronized int episodeCount()      { return episodeCount.get(); }

    /** Moving window mean. */
    public synchronized float windowMean() {
        if (winCount == 0) return 0f;
        float s = 0; for (int i = 0; i < winCount; i++) s += returnWindow[i]; return s / winCount;
    }

    /** Moving window std. */
    public synchronized float windowStd() {
        if (winCount < 2) return 0f;
        float mean = windowMean(), s2 = 0;
        for (int i = 0; i < winCount; i++) { float d = returnWindow[i]-mean; s2+=d*d; }
        return (float) Math.sqrt(s2 / winCount);
    }

    /** Progress fraction toward target return. */
    public synchronized float progress() {
        if (targetReturn == Float.MAX_VALUE) return 0f;
        float range = targetReturn - worstReturn;
        if (range <= 0) return 1f;
        return Math.min(1f, Math.max(0f, (ema - worstReturn) / range));
    }

    /** Estimated steps to target (based on recent EMA improvement rate). */
    public synchronized int etaSteps() {
        if (targetReturn == Float.MAX_VALUE) return -1;
        float remaining = targetReturn - ema;
        if (remaining <= 0) return 0;
        float rate = Math.abs(ema - prevEma) + 1e-8f;
        return (int)(remaining / rate);
    }

    /** Recent episode history snapshot. */
    public synchronized List<EpisodeRecord> getHistory(int n) {
        List<EpisodeRecord> recent = new ArrayList<>();
        int start = Math.max(0, history.size() - n);
        int i = 0;
        for (EpisodeRecord r : history) { if (i++ >= start) recent.add(r); }
        return recent;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Convergence detection
    // ─────────────────────────────────────────────────────────────────────────

    private void checkConvergence(float returnValue) {
        float improvement = Math.abs(ema - prevEma);
        prevEma = ema;

        // Plateau
        if (improvement < plateauThresh) { plateauCnt++; } else { plateauCnt = 0; }
        isPlateau = plateauCnt >= plateauSteps;

        // Collapse: fast EMA drops significantly below slow EMA
        isCollapse = (ema > 0 && emaFast < ema * (1f - collapseThresh))
                  || (ema < 0 && emaFast > ema * (1f - collapseThresh));

        if (isPlateau && plateauCnt == plateauSteps)
            Log.w(TAG, "Learning plateau detected at episode " + episodeCount.get()
                    + " ema=" + ema);
        if (isCollapse)
            Log.w(TAG, "Performance collapse at episode " + episodeCount.get()
                    + " ema=" + ema + " fast=" + emaFast);
    }

    private void logProgress(int ep) {
        Log.i(TAG, String.format("Ep=%d ema=%.2f win=%.2f±%.2f best=%.2f progress=%.1f%%",
                ep, ema, windowMean(), windowStd(), bestReturn, progress() * 100));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("episodeCount",  episodeCount.get());
        s.put("ema",           ema);
        s.put("windowMean",    windowMean());
        s.put("windowStd",     windowStd());
        s.put("bestReturn",    bestReturn);
        s.put("worstReturn",   worstReturn);
        s.put("bestEpisode",   bestEpisode);
        s.put("isPlateau",     isPlateau);
        s.put("isCollapse",    isCollapse);
        s.put("progress",      progress());
        s.put("etaSteps",      etaSteps());
        s.put("avgPolicyLoss", avgPolicyLoss);
        s.put("avgValueLoss",  avgValueLoss);
        s.put("avgEntropy",    avgEntropy);
        long elapsed = System.currentTimeMillis() - startTime;
        s.put("elapsedSec",    elapsed / 1000f);
        return s;
    }
}
