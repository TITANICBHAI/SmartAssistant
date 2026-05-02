package com.aiassistant.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.aiassistant.detection.EnemyDetector;
import com.aiassistant.ml.GamePatternRecognizer;
import com.aiassistant.utils.ElementDetector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/**
 * PerceptionEngine — unified, multi-channel screen perception layer.
 *
 * Responsibility:
 *   Accept raw screen bitmaps from AIAccessibilityService (or any source) and
 *   fan-out analysis to all relevant sub-detectors in parallel.  Merge results
 *   into a single {@link PerceptionResult} that downstream decision-making
 *   components (DecisionEngine, PredictiveActionSystem, etc.) consume.
 *
 * Design:
 *   - Three parallel perception channels:
 *       1. EnemyDetector      — enemy positions, threat levels
 *       2. ElementDetector    — UI element detection (buttons, toggles, text)
 *       3. GamePatternRecognizer — temporal pattern matching
 *   - Results merged with timestamps so consumers can discard stale data.
 *   - Adaptive frame-skip: if perception takes longer than the deadline,
 *     subsequent frames are dropped until the pipeline catches up.
 *   - PerceptionListener callback for push-based delivery.
 *   - Singleton with lazy initialisation.
 */
public class PerceptionEngine {

    private static final String TAG = "PerceptionEngine";

    /** Soft deadline for one full perception pass in milliseconds. */
    private static final long FRAME_DEADLINE_MS = 80L;

    // -------------------------------------------------------------------------
    // Singleton
    // -------------------------------------------------------------------------
    private static volatile PerceptionEngine instance;

    public static PerceptionEngine getInstance(Context context) {
        if (instance == null) {
            synchronized (PerceptionEngine.class) {
                if (instance == null) {
                    instance = new PerceptionEngine(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    // Inner types
    // -------------------------------------------------------------------------

    /** Merged output of one perception cycle. */
    public static class PerceptionResult {
        public final long   timestamp;
        public final long   processingMs;

        /** Enemy detector output — list of enemy attribute maps. */
        public final List<Map<String, Object>> enemies;

        /** UI element detector output — raw element list from detectElements(). */
        public final Map<String, Object> uiElements;

        /** Game pattern recognizer stats snapshot. */
        public final Map<String, Object> patternStats;

        /** Combined confidence [0, 1] — geometric mean of sub-detector confidences. */
        public final float confidence;

        /** True when at least one channel detected something actionable. */
        public final boolean hasDetections;

        PerceptionResult(long ts, long ms,
                         List<Map<String, Object>> enemies,
                         Map<String, Object> uiElements,
                         Map<String, Object> patternStats,
                         float confidence) {
            this.timestamp    = ts;
            this.processingMs = ms;
            this.enemies      = enemies;
            this.uiElements   = uiElements;
            this.patternStats = patternStats;
            this.confidence   = confidence;
            this.hasDetections = (enemies != null && !enemies.isEmpty())
                    || (uiElements != null && !uiElements.isEmpty());
        }
    }

    /** Callback delivered on the perception thread. */
    public interface PerceptionListener {
        void onPerceptionResult(PerceptionResult result);
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------
    private final Context context;

    private EnemyDetector       enemyDetector;
    private ElementDetector     elementDetector;
    private GamePatternRecognizer patternRecognizer;

    private final ExecutorService pool =
            Executors.newFixedThreadPool(3); // one thread per channel

    private final List<PerceptionListener> listeners = new CopyOnWriteArrayList<>();

    private final AtomicLong frameCount    = new AtomicLong(0);
    private final AtomicLong droppedFrames = new AtomicLong(0);
    private final AtomicLong totalMs       = new AtomicLong(0);

    private volatile boolean   initialized   = false;
    private volatile long      lastFrameStart = 0L;

    // Per-channel enable flags — allow callers to disable channels they don't need
    private volatile boolean enemyChannel   = true;
    private volatile boolean elementChannel = true;
    private volatile boolean patternChannel = true;

    // -------------------------------------------------------------------------
    // Construction / initialisation
    // -------------------------------------------------------------------------

    private PerceptionEngine(Context context) {
        this.context = context;
    }

    /**
     * Initialise all sub-detectors.  Must be called once before processFrame().
     * Safe to call multiple times (idempotent).
     */
    public synchronized void initialize(String gameType) {
        if (initialized) return;
        try {
            enemyDetector     = EnemyDetector.getInstance(context);
            enemyDetector.setGameType(gameType);

            elementDetector   = new ElementDetector(context);
            patternRecognizer = GamePatternRecognizer.getInstance(context);
            patternRecognizer.start();

            initialized = true;
            Log.i(TAG, "PerceptionEngine initialized for gameType=" + gameType);
        } catch (Exception e) {
            Log.e(TAG, "Initialization error: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Main API
    // -------------------------------------------------------------------------

    /**
     * Submit a screen frame for parallel analysis.
     *
     * @param screen Raw screen bitmap (will NOT be recycled by this method).
     * @return PerceptionResult, or null if the frame was dropped due to overload.
     */
    public PerceptionResult processFrame(Bitmap screen) {
        if (!initialized || screen == null) return null;

        long now = System.currentTimeMillis();

        // Adaptive frame-skip: drop frame if last one is still processing
        if (lastFrameStart > 0 && (now - lastFrameStart) < FRAME_DEADLINE_MS / 2) {
            droppedFrames.incrementAndGet();
            return null;
        }
        lastFrameStart = now;
        frameCount.incrementAndGet();

        long t0 = System.currentTimeMillis();

        // ---- Channel 1: Enemy detection ----
        Future<List<Map<String, Object>>> enemyFuture = null;
        if (enemyChannel) {
            enemyFuture = pool.submit(() -> {
                try {
                    return enemyDetector.detectEnemies(screen);
                } catch (Exception e) {
                    Log.w(TAG, "Enemy channel error: " + e.getMessage());
                    return new ArrayList<>();
                }
            });
        }

        // ---- Channel 2: UI element detection ----
        Future<Map<String, Object>> elementFuture = null;
        if (elementChannel) {
            elementFuture = pool.submit(() -> {
                try {
                    return elementDetector.detectElements(screen);
                } catch (Exception e) {
                    Log.w(TAG, "Element channel error: " + e.getMessage());
                    return new HashMap<>();
                }
            });
        }

        // ---- Channel 3: Pattern stats (non-blocking snapshot) ----
        Map<String, Object> patternStats = new HashMap<>();
        if (patternChannel && patternRecognizer != null) {
            try {
                patternStats = patternRecognizer.getStats();
            } catch (Exception e) {
                Log.w(TAG, "Pattern channel error: " + e.getMessage());
            }
        }

        // ---- Collect futures ----
        List<Map<String, Object>> enemies = new ArrayList<>();
        Map<String, Object>       uiElems = new HashMap<>();

        try {
            if (enemyFuture  != null) enemies = enemyFuture.get();
        } catch (Exception e) { Log.w(TAG, "Enemy future error: " + e.getMessage()); }

        try {
            if (elementFuture != null) uiElems = elementFuture.get();
        } catch (Exception e) { Log.w(TAG, "Element future error: " + e.getMessage()); }

        long processingMs = System.currentTimeMillis() - t0;
        totalMs.addAndGet(processingMs);

        // ---- Compute composite confidence ----
        float conf = computeConfidence(enemies, uiElems);

        PerceptionResult result = new PerceptionResult(
                now, processingMs, enemies, uiElems, patternStats, conf);

        // ---- Notify listeners ----
        for (PerceptionListener l : listeners) {
            try { l.onPerceptionResult(result); }
            catch (Exception e) { Log.w(TAG, "Listener error: " + e.getMessage()); }
        }

        return result;
    }

    // -------------------------------------------------------------------------
    // Channel control
    // -------------------------------------------------------------------------

    public void setEnemyChannelEnabled(boolean enabled)   { this.enemyChannel   = enabled; }
    public void setElementChannelEnabled(boolean enabled) { this.elementChannel = enabled; }
    public void setPatternChannelEnabled(boolean enabled) { this.patternChannel = enabled; }

    // -------------------------------------------------------------------------
    // Listener management
    // -------------------------------------------------------------------------

    public void addListener(PerceptionListener l)    { listeners.add(l); }
    public void removeListener(PerceptionListener l) { listeners.remove(l); }

    // -------------------------------------------------------------------------
    // Stats
    // -------------------------------------------------------------------------

    public Map<String, Object> getStats() {
        long fc = frameCount.get();
        Map<String, Object> s = new HashMap<>();
        s.put("frameCount",    fc);
        s.put("droppedFrames", droppedFrames.get());
        s.put("avgProcessingMs", fc > 0 ? (double) totalMs.get() / fc : 0.0);
        s.put("enemyChannel",   enemyChannel);
        s.put("elementChannel", elementChannel);
        s.put("patternChannel", patternChannel);
        s.put("listenerCount",  listeners.size());
        return s;
    }

    // -------------------------------------------------------------------------
    // Shutdown
    // -------------------------------------------------------------------------

    public void shutdown() {
        pool.shutdownNow();
        if (patternRecognizer != null) patternRecognizer.stop();
        initialized = false;
        Log.i(TAG, "PerceptionEngine shut down.");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private float computeConfidence(List<Map<String, Object>> enemies,
                                     Map<String, Object> uiElems) {
        float enemyConf   = enemies.isEmpty() ? 0.5f : Math.min(1f, 0.5f + 0.1f * enemies.size());
        float elementConf = uiElems.isEmpty() ? 0.5f : 0.75f;
        // Geometric mean of the two channel confidences
        return (float) Math.sqrt(enemyConf * elementConf);
    }
}
