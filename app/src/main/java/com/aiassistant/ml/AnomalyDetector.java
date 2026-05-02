package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Anomaly detector for game-state and system-metric monitoring.
 *
 * Detects unusual values in any named numeric stream using a combination of:
 *
 *  1. Z-score detection — value is flagged if |z| > Z_THRESHOLD where
 *     z = (x − μ) / σ, with μ and σ maintained via Welford's online
 *     algorithm (numerically stable, single-pass).
 *
 *  2. IQR detection — value is flagged if it falls below Q1 − 1.5·IQR or
 *     above Q3 + 1.5·IQR, computed from the sliding window buffer.
 *
 *  3. Consecutive deviation counter — an anomaly is only reported after
 *     MIN_CONSECUTIVE consecutive unusual values to suppress one-off noise.
 *
 *  4. Per-feature sliding window of configurable length (default 100 samples)
 *     so detection adapts to distributional shifts over time.
 *
 *  5. Anomaly event callbacks — register an {@link AnomalyListener} to receive
 *     notifications whenever an anomaly is confirmed.
 *
 *  6. Anomaly history — keeps the last MAX_HISTORY events per feature.
 *
 * Thread-safe via per-feature synchronisation.
 */
public class AnomalyDetector {
    private static final String TAG            = "AnomalyDetector";
    private static final double Z_THRESHOLD    = 3.0;
    private static final int    WINDOW_SIZE    = 100;
    private static final int    MIN_CONSECUTIVE= 2;
    private static final int    MIN_SAMPLES    = 5;   // need at least this many before flagging
    private static final int    MAX_HISTORY    = 50;

    // -----------------------------------------------------------------------
    // Anomaly event
    // -----------------------------------------------------------------------
    public static class AnomalyEvent {
        public final String  feature;
        public final double  value;
        public final double  zScore;
        public final double  mean;
        public final double  stdDev;
        public final long    timestamp;
        public final String  method; // "ZSCORE" or "IQR"

        AnomalyEvent(String feature, double value, double z,
                     double mean, double std, String method) {
            this.feature   = feature;
            this.value     = value;
            this.zScore    = z;
            this.mean      = mean;
            this.stdDev    = std;
            this.timestamp = System.currentTimeMillis();
            this.method    = method;
        }

        @Override public String toString() {
            return "AnomalyEvent{feature=" + feature + " value=" + String.format("%.3f",value)
                 + " z=" + String.format("%.2f",zScore) + " method=" + method + "}";
        }
    }

    // -----------------------------------------------------------------------
    // Listener
    // -----------------------------------------------------------------------
    public interface AnomalyListener {
        void onAnomaly(AnomalyEvent event);
    }

    // -----------------------------------------------------------------------
    // Per-feature stream state
    // -----------------------------------------------------------------------
    private static class FeatureStream {
        final String     name;
        final List<Double> window = new ArrayList<>();

        // Welford's online statistics
        long   count  = 0;
        double mean   = 0;
        double M2     = 0;          // sum of squared deviations

        int  consecutiveAnomalies = 0;
        final List<AnomalyEvent> history = new ArrayList<>();

        FeatureStream(String name) { this.name = name; }

        synchronized void add(double value) {
            // Update window
            window.add(value);
            if (window.size() > WINDOW_SIZE) window.remove(0);

            // Welford update
            count++;
            double delta = value - mean;
            mean  += delta / count;
            double delta2 = value - mean;
            M2    += delta * delta2;
        }

        synchronized double stdDev() {
            return count < 2 ? 0 : Math.sqrt(M2 / (count - 1));
        }

        synchronized double[] iqrBounds() {
            if (window.size() < MIN_SAMPLES) return null;
            List<Double> sorted = new ArrayList<>(window);
            Collections.sort(sorted);
            int n = sorted.size();
            double q1 = sorted.get(n / 4);
            double q3 = sorted.get(3 * n / 4);
            double iqr = q3 - q1;
            return new double[]{q1 - 1.5 * iqr, q3 + 1.5 * iqr};
        }

        synchronized void recordAnomaly(AnomalyEvent e) {
            history.add(e);
            if (history.size() > MAX_HISTORY) history.remove(0);
        }

        synchronized List<AnomalyEvent> getHistory() { return new ArrayList<>(history); }
    }

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------
    private final Map<String, FeatureStream>   streams   = new ConcurrentHashMap<>();
    private final List<AnomalyListener>        listeners = new ArrayList<>();
    private final Object                       listenerLock = new Object();

    // -----------------------------------------------------------------------
    // Singleton
    // -----------------------------------------------------------------------
    private static volatile AnomalyDetector instance;
    public static AnomalyDetector getInstance() {
        if (instance == null) {
            synchronized (AnomalyDetector.class) {
                if (instance == null) instance = new AnomalyDetector();
            }
        }
        return instance;
    }

    // -----------------------------------------------------------------------
    // API
    // -----------------------------------------------------------------------

    /**
     * Records a new value for the named feature and checks for anomalies.
     *
     * @param feature Name of the metric (e.g., "health", "fps", "reward")
     * @param value   Current observed value
     * @return true if an anomaly was detected and confirmed
     */
    public boolean observe(String feature, double value) {
        FeatureStream stream = streams.computeIfAbsent(feature, FeatureStream::new);
        stream.add(value);

        if (stream.count < MIN_SAMPLES) return false;

        double std = stream.stdDev();
        boolean anomaly = false;
        AnomalyEvent event = null;

        // Z-score check
        if (std > 1e-9) {
            double z = (value - stream.mean) / std;
            if (Math.abs(z) > Z_THRESHOLD) {
                stream.consecutiveAnomalies++;
                if (stream.consecutiveAnomalies >= MIN_CONSECUTIVE) {
                    event = new AnomalyEvent(feature, value, z, stream.mean, std, "ZSCORE");
                }
            } else {
                stream.consecutiveAnomalies = 0;
            }
        }

        // IQR check (only if Z-score didn't already fire)
        if (event == null) {
            double[] bounds = stream.iqrBounds();
            if (bounds != null && (value < bounds[0] || value > bounds[1])) {
                double z = std > 1e-9 ? (value - stream.mean) / std : 0;
                stream.consecutiveAnomalies++;
                if (stream.consecutiveAnomalies >= MIN_CONSECUTIVE) {
                    event = new AnomalyEvent(feature, value, z, stream.mean, std, "IQR");
                }
            } else if (bounds != null) {
                stream.consecutiveAnomalies = 0;
            }
        }

        if (event != null) {
            anomaly = true;
            stream.recordAnomaly(event);
            Log.w(TAG, event.toString());
            notifyListeners(event);
        }

        return anomaly;
    }

    /**
     * Convenience overload — observes all features in a state map.
     * Returns a map of feature → anomaly detected.
     */
    public Map<String, Boolean> observeAll(Map<String, Object> state) {
        Map<String, Boolean> result = new HashMap<>();
        for (Map.Entry<String, Object> e : state.entrySet()) {
            if (e.getValue() instanceof Number) {
                result.put(e.getKey(),
                        observe(e.getKey(), ((Number)e.getValue()).doubleValue()));
            }
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // Listeners
    // -----------------------------------------------------------------------
    public void addListener(AnomalyListener l) {
        synchronized (listenerLock) { if (l != null) listeners.add(l); }
    }

    public void removeListener(AnomalyListener l) {
        synchronized (listenerLock) { listeners.remove(l); }
    }

    private void notifyListeners(AnomalyEvent event) {
        List<AnomalyListener> snap;
        synchronized (listenerLock) { snap = new ArrayList<>(listeners); }
        for (AnomalyListener l : snap) {
            try { l.onAnomaly(event); }
            catch (Exception e) { Log.e(TAG, "Listener error", e); }
        }
    }

    // -----------------------------------------------------------------------
    // Query
    // -----------------------------------------------------------------------

    public List<AnomalyEvent> getHistory(String feature) {
        FeatureStream s = streams.get(feature);
        return s != null ? s.getHistory() : Collections.emptyList();
    }

    public double getMean(String feature) {
        FeatureStream s = streams.get(feature);
        return s != null ? s.mean : 0;
    }

    public double getStdDev(String feature) {
        FeatureStream s = streams.get(feature);
        return s != null ? s.stdDev() : 0;
    }

    public int getFeatureCount()      { return streams.size(); }
    public void resetFeature(String f) { streams.remove(f); }
    public void resetAll()             { streams.clear(); }

    public Map<String, Object> getStats() {
        Map<String, Object> m = new HashMap<>();
        m.put("featureCount", streams.size());
        Map<String, Object> perFeature = new HashMap<>();
        for (Map.Entry<String, FeatureStream> e : streams.entrySet()) {
            FeatureStream s = e.getValue();
            Map<String, Object> fs = new HashMap<>();
            fs.put("count",    s.count);
            fs.put("mean",     s.mean);
            fs.put("stdDev",   s.stdDev());
            fs.put("anomalies",s.history.size());
            perFeature.put(e.getKey(), fs);
        }
        m.put("features", perFeature);
        return m;
    }
}
