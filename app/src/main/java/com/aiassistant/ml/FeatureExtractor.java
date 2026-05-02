package com.aiassistant.ml;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FeatureExtractor — extracts domain-specific numeric feature vectors from
 * multiple input modalities (game state maps, screen bitmaps, event histories)
 * and combines them into a single concatenated float[] for RL agents.
 *
 * Feature groups:
 *
 *   1. STATE_MAP   — extracts registered keys from a Map<String, Object>, normalizes
 *                    numeric values, one-hot encodes categoricals, converts booleans.
 *
 *   2. SCREEN_GRID — divides a Bitmap into an (cols × rows) grid and computes
 *                    mean RGB brightness per cell (compact visual representation).
 *
 *   3. EVENT_HIST  — encodes the last N game events as a fixed-size binary feature
 *                    window (which event types occurred in the last N steps).
 *
 *   4. TEMPORAL    — appends delta features (change since last frame) for the
 *                    numeric state-map group.
 *
 * A "feature plan" is configured at construction time; subsequent calls to
 * {@link #extract(Map, Bitmap, List)} return the same-length vector on every call.
 * Thread-safe.
 */
public class FeatureExtractor {

    private static final String TAG = "FeatureExtractor";

    // -------------------------------------------------------------------------
    // Feature group enum
    // -------------------------------------------------------------------------
    public enum Group { STATE_MAP, SCREEN_GRID, EVENT_HIST, TEMPORAL }

    // -------------------------------------------------------------------------
    // State-map feature spec
    // -------------------------------------------------------------------------
    public static class MapFeatureSpec {
        final String   key;
        final float    min, max;      // for normalization
        final String[] categories;   // null for numeric/bool, non-null for categorical

        /** Numeric feature. */
        public MapFeatureSpec(String key, float min, float max) {
            this.key = key; this.min = min; this.max = max; this.categories = null;
        }
        /** Boolean feature (encoded 0/1). */
        public MapFeatureSpec(String key) {
            this.key = key; this.min = 0; this.max = 1; this.categories = null;
        }
        /** Categorical feature (one-hot). */
        public MapFeatureSpec(String key, String[] categories) {
            this.key = key; this.min = 0; this.max = 1; this.categories = categories;
        }
        int size() { return categories != null ? categories.length : 1; }
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final List<MapFeatureSpec>  mapSpecs       = new ArrayList<>();
    private final Set<Group>            enabledGroups  = new java.util.HashSet<>();

    // Screen grid settings
    private int gridCols = 8;
    private int gridRows = 8;

    // Event history settings
    private int  eventWindowSize = 10;  // last N events encoded
    private String[] eventTypes  = {}; // event type names to track

    // Temporal delta settings
    private boolean temporalEnabled = false;
    private float[] lastMapFeatures = null;

    // Computed output dimension
    private int outputDim = 0;

    private final Object lock = new Object();

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    public FeatureExtractor() {}

    // -------------------------------------------------------------------------
    // Configuration API (fluent builder pattern)
    // -------------------------------------------------------------------------

    /** Enable the game-state map feature group. */
    public FeatureExtractor withMapFeatures(MapFeatureSpec... specs) {
        mapSpecs.clear();
        mapSpecs.addAll(Arrays.asList(specs));
        enabledGroups.add(Group.STATE_MAP);
        recomputeDim();
        return this;
    }

    /** Enable the screen-grid visual feature group. */
    public FeatureExtractor withScreenGrid(int cols, int rows) {
        this.gridCols = cols;
        this.gridRows = rows;
        enabledGroups.add(Group.SCREEN_GRID);
        recomputeDim();
        return this;
    }

    /** Enable the event-history feature group. */
    public FeatureExtractor withEventHistory(int windowSize, String... eventTypes) {
        this.eventWindowSize = windowSize;
        this.eventTypes      = eventTypes;
        enabledGroups.add(Group.EVENT_HIST);
        recomputeDim();
        return this;
    }

    /** Enable temporal delta features for the state-map group. */
    public FeatureExtractor withTemporalDeltas() {
        temporalEnabled = true;
        enabledGroups.add(Group.TEMPORAL);
        recomputeDim();
        return this;
    }

    public int getOutputDim() { return outputDim; }

    // -------------------------------------------------------------------------
    // Extraction API
    // -------------------------------------------------------------------------

    /**
     * Extract combined feature vector from all configured groups.
     *
     * @param stateMap     Game state map (may be null if STATE_MAP not used).
     * @param screen       Screen bitmap (may be null if SCREEN_GRID not used).
     * @param recentEvents List of recent event type strings (may be null).
     * @return float[] of length {@link #getOutputDim()}.
     */
    public float[] extract(Map<String, Object> stateMap,
                            Bitmap screen,
                            List<String> recentEvents) {
        synchronized (lock) {
            List<float[]> parts = new ArrayList<>();

            if (enabledGroups.contains(Group.STATE_MAP) && stateMap != null) {
                float[] mapF = extractMapFeatures(stateMap);
                parts.add(mapF);

                if (enabledGroups.contains(Group.TEMPORAL)) {
                    float[] delta = new float[mapF.length];
                    if (lastMapFeatures != null && lastMapFeatures.length == mapF.length) {
                        for (int i = 0; i < mapF.length; i++) delta[i] = mapF[i] - lastMapFeatures[i];
                    }
                    lastMapFeatures = mapF.clone();
                    parts.add(delta);
                }
            }

            if (enabledGroups.contains(Group.SCREEN_GRID) && screen != null) {
                parts.add(extractScreenGrid(screen));
            }

            if (enabledGroups.contains(Group.EVENT_HIST)) {
                parts.add(extractEventHistory(recentEvents));
            }

            return concat(parts);
        }
    }

    /** Convenience: extract from state map only. */
    public float[] extractFromMap(Map<String, Object> stateMap) {
        return extract(stateMap, null, null);
    }

    /** Convenience: extract from bitmap only. */
    public float[] extractFromScreen(Bitmap screen) {
        return extract(null, screen, null);
    }

    // -------------------------------------------------------------------------
    // Group implementations
    // -------------------------------------------------------------------------

    private float[] extractMapFeatures(Map<String, Object> state) {
        int dim = 0;
        for (MapFeatureSpec s : mapSpecs) dim += s.size();
        float[] vec = new float[dim];
        int idx = 0;
        for (MapFeatureSpec spec : mapSpecs) {
            Object val = state.get(spec.key);
            if (spec.categories != null) {
                // One-hot
                String cat = val != null ? val.toString() : "";
                for (String c : spec.categories) vec[idx++] = c.equals(cat) ? 1f : 0f;
            } else if (val instanceof Boolean) {
                vec[idx++] = ((Boolean) val) ? 1f : 0f;
            } else {
                float v = toFloat(val, (spec.min + spec.max) / 2f);
                float range = spec.max - spec.min;
                vec[idx++] = range > 1e-6f ? Math.max(0f, Math.min(1f, (v - spec.min) / range)) : 0.5f;
            }
        }
        return vec;
    }

    private float[] extractScreenGrid(Bitmap bmp) {
        int w = bmp.getWidth(), h = bmp.getHeight();
        int cW = Math.max(1, w / gridCols), cH = Math.max(1, h / gridRows);
        float[] vec = new float[gridCols * gridRows * 3];
        int idx = 0;
        for (int gy = 0; gy < gridRows; gy++) {
            for (int gx = 0; gx < gridCols; gx++) {
                long sr = 0, sg = 0, sb = 0;
                int count = 0;
                int x0 = gx * cW, y0 = gy * cH;
                for (int y = y0; y < Math.min(y0 + cH, h); y += 2) {
                    for (int x = x0; x < Math.min(x0 + cW, w); x += 2) {
                        int px = bmp.getPixel(x, y);
                        sr += Color.red(px); sg += Color.green(px); sb += Color.blue(px);
                        count++;
                    }
                }
                if (count > 0) {
                    vec[idx++] = sr / (255f * count);
                    vec[idx++] = sg / (255f * count);
                    vec[idx++] = sb / (255f * count);
                } else idx += 3;
            }
        }
        return vec;
    }

    private float[] extractEventHistory(List<String> events) {
        float[] vec = new float[eventTypes.length * eventWindowSize];
        if (events == null || eventTypes.length == 0) return vec;

        // Fill from most-recent end of events
        int start = Math.max(0, events.size() - eventWindowSize);
        for (int t = 0; t < eventWindowSize && start + t < events.size(); t++) {
            String evt = events.get(start + t);
            for (int e = 0; e < eventTypes.length; e++) {
                if (eventTypes[e].equals(evt)) {
                    vec[t * eventTypes.length + e] = 1f;
                }
            }
        }
        return vec;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void recomputeDim() {
        int dim = 0;
        if (enabledGroups.contains(Group.STATE_MAP)) {
            for (MapFeatureSpec s : mapSpecs) dim += s.size();
            if (enabledGroups.contains(Group.TEMPORAL)) dim += mapSpecs.stream().mapToInt(MapFeatureSpec::size).sum();
        }
        if (enabledGroups.contains(Group.SCREEN_GRID)) dim += gridCols * gridRows * 3;
        if (enabledGroups.contains(Group.EVENT_HIST))  dim += eventTypes.length * eventWindowSize;
        outputDim = dim;
    }

    private static float[] concat(List<float[]> parts) {
        int total = 0;
        for (float[] p : parts) total += p.length;
        float[] out = new float[total];
        int off = 0;
        for (float[] p : parts) { System.arraycopy(p, 0, out, off, p.length); off += p.length; }
        return out;
    }

    private static float toFloat(Object v, float def) {
        if (v instanceof Number) return ((Number) v).floatValue();
        if (v instanceof Boolean) return ((Boolean) v) ? 1f : 0f;
        if (v instanceof String) { try { return Float.parseFloat((String)v); } catch (NumberFormatException ignored) {} }
        return def;
    }

    public Map<String, Object> getInfo() {
        Map<String, Object> m = new HashMap<>();
        m.put("outputDim",    outputDim);
        m.put("mapSpecs",     mapSpecs.size());
        m.put("gridCols",     gridCols);
        m.put("gridRows",     gridRows);
        m.put("eventWindow",  eventWindowSize);
        m.put("temporal",     temporalEnabled);
        return m;
    }
}
