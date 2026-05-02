package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GameStateEncoder — converts raw game-state {@code Map<String, Object>} snapshots into
 * fixed-length normalized float feature vectors suitable for RL agents and ML models.
 *
 * Features:
 * - Configurable feature schema: callers register named feature slots with type and range.
 * - Running min/max normalization per feature (Welford-style, updated online).
 * - One-hot encoding for categorical string features.
 * - Missing-value imputation (mean or zero).
 * - Delta features: for each numeric feature the encoder optionally appends Δ = current − prev.
 * - Stacking: last N encoded vectors are concatenated into a single observation for
 *   frame-stacked inputs (useful for velocity / temporal context).
 * - Thread-safe encode/update via synchronized blocks.
 */
public class GameStateEncoder {

    private static final String TAG = "GameStateEncoder";

    // -------------------------------------------------------------------------
    // Feature descriptor
    // -------------------------------------------------------------------------

    public enum FeatureType { NUMERIC, BOOLEAN, CATEGORICAL }

    public static class FeatureSpec {
        final String      name;
        final FeatureType type;
        final float       min;      // for NUMERIC: expected min (used for init)
        final float       max;      // for NUMERIC: expected max (used for init)
        final String[]    categories; // for CATEGORICAL: all possible values

        /** Numeric feature constructor. */
        public FeatureSpec(String name, float min, float max) {
            this.name       = name;
            this.type       = FeatureType.NUMERIC;
            this.min        = min;
            this.max        = max;
            this.categories = null;
        }

        /** Boolean feature constructor (encoded as 0 / 1). */
        public FeatureSpec(String name) {
            this.name       = name;
            this.type       = FeatureType.BOOLEAN;
            this.min        = 0f;
            this.max        = 1f;
            this.categories = null;
        }

        /** Categorical feature constructor. */
        public FeatureSpec(String name, String[] categories) {
            this.name       = name;
            this.type       = FeatureType.CATEGORICAL;
            this.min        = 0f;
            this.max        = 1f;
            this.categories = categories;
        }

        int encodedSize() {
            if (type == FeatureType.CATEGORICAL) return categories.length;
            return 1;
        }
    }

    // -------------------------------------------------------------------------
    // Running normalization stats (Welford)
    // -------------------------------------------------------------------------

    private static class RunningStats {
        double mean   = 0.0;
        double m2     = 0.0;
        long   count  = 0;
        float  seenMin = Float.MAX_VALUE;
        float  seenMax = -Float.MAX_VALUE;

        void update(float value) {
            count++;
            double delta  = value - mean;
            mean += delta / count;
            double delta2 = value - mean;
            m2   += delta * delta2;
            if (value < seenMin) seenMin = value;
            if (value > seenMax) seenMax = value;
        }

        /** Normalize value to approximately [0, 1] using running min/max. */
        float normalize(float value) {
            float range = seenMax - seenMin;
            if (range < 1e-6f) return 0.5f;
            return Math.max(0f, Math.min(1f, (value - seenMin) / range));
        }

        float getMean() { return (float) mean; }
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final List<FeatureSpec>           specs        = new ArrayList<>();
    private final Map<String, RunningStats>   stats        = new HashMap<>();
    private final boolean                     deltaFeatures;
    private final int                         stackSize;   // frame stacking depth

    private float[] lastEncoded;   // previous frame's raw encoded vector (for delta)
    private final List<float[]> frameStack = new ArrayList<>();

    private int baseVectorSize  = 0;
    private int outputVectorSize = 0;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * @param deltaFeatures If true, appends Δ = current−prev for every numeric/bool feature.
     * @param stackSize     Number of consecutive frames to stack (1 = no stacking).
     */
    public GameStateEncoder(boolean deltaFeatures, int stackSize) {
        this.deltaFeatures = deltaFeatures;
        this.stackSize     = Math.max(1, stackSize);
    }

    /** Default: no delta, no stacking. */
    public GameStateEncoder() {
        this(false, 1);
    }

    // -------------------------------------------------------------------------
    // Schema builder
    // -------------------------------------------------------------------------

    public synchronized GameStateEncoder addNumeric(String name, float min, float max) {
        addSpec(new FeatureSpec(name, min, max));
        return this;
    }

    public synchronized GameStateEncoder addBoolean(String name) {
        addSpec(new FeatureSpec(name));
        return this;
    }

    public synchronized GameStateEncoder addCategorical(String name, String... categories) {
        addSpec(new FeatureSpec(name, categories));
        return this;
    }

    private void addSpec(FeatureSpec spec) {
        specs.add(spec);
        stats.put(spec.name, new RunningStats());
        recomputeSizes();
    }

    private void recomputeSizes() {
        int base = 0;
        for (FeatureSpec s : specs) base += s.encodedSize();
        if (deltaFeatures) base += base; // append delta for every slot
        baseVectorSize  = base;
        outputVectorSize = base * stackSize;
    }

    /** Total length of the vector returned by {@link #encode}. */
    public int getOutputSize() { return outputVectorSize; }

    // -------------------------------------------------------------------------
    // Encoding
    // -------------------------------------------------------------------------

    /**
     * Encode a raw game-state map into a fixed-length float vector.
     *
     * @param state Key-value game state snapshot.
     * @return Normalized float[] of length {@link #getOutputSize()}.
     */
    public synchronized float[] encode(Map<String, Object> state) {
        if (specs.isEmpty()) return new float[0];

        float[] vec = new float[baseVectorSize / (deltaFeatures ? 2 : 1)];
        int idx = 0;

        for (FeatureSpec spec : specs) {
            Object rawVal = state.get(spec.name);

            switch (spec.type) {
                case NUMERIC: {
                    float v = toFloat(rawVal, stats.get(spec.name).getMean());
                    stats.get(spec.name).update(v);
                    vec[idx++] = stats.get(spec.name).normalize(v);
                    break;
                }
                case BOOLEAN: {
                    boolean b = toBoolean(rawVal);
                    vec[idx++] = b ? 1f : 0f;
                    break;
                }
                case CATEGORICAL: {
                    // One-hot over spec.categories — leaves all zeros if unknown category
                    String cat = rawVal != null ? rawVal.toString() : "";
                    for (String c : spec.categories) {
                        vec[idx++] = c.equals(cat) ? 1f : 0f;
                    }
                    break;
                }
            }
        }

        // Append delta features
        float[] base;
        if (deltaFeatures) {
            float[] delta = new float[vec.length];
            if (lastEncoded != null && lastEncoded.length == vec.length) {
                for (int i = 0; i < vec.length; i++) delta[i] = vec[i] - lastEncoded[i];
            }
            base = concat(vec, delta);
        } else {
            base = vec;
        }

        lastEncoded = vec.clone();

        // Frame stacking
        frameStack.add(base);
        while (frameStack.size() > stackSize) frameStack.remove(0);

        float[] output = new float[outputVectorSize];
        int outIdx = 0;
        for (int f = 0; f < stackSize; f++) {
            float[] frame = f < frameStack.size()
                    ? frameStack.get(frameStack.size() - 1 - f)
                    : new float[base.length];
            System.arraycopy(frame, 0, output, outIdx, Math.min(frame.length, base.length));
            outIdx += base.length;
        }
        return output;
    }

    /** Reset the frame stack and normalization statistics. */
    public synchronized void reset() {
        lastEncoded = null;
        frameStack.clear();
        for (RunningStats s : stats.values()) {
            s.mean = 0; s.m2 = 0; s.count = 0;
            s.seenMin = Float.MAX_VALUE; s.seenMax = -Float.MAX_VALUE;
        }
    }

    /** Return a descriptive snapshot of the feature schema and stats. */
    public synchronized Map<String, Object> getSchemaInfo() {
        Map<String, Object> info = new HashMap<>();
        List<Map<String, Object>> featureList = new ArrayList<>();
        for (FeatureSpec spec : specs) {
            Map<String, Object> fd = new HashMap<>();
            fd.put("name",        spec.name);
            fd.put("type",        spec.type.name());
            fd.put("encodedSize", spec.encodedSize());
            RunningStats rs = stats.get(spec.name);
            if (rs != null && rs.count > 0) {
                fd.put("seenMin", rs.seenMin);
                fd.put("seenMax", rs.seenMax);
                fd.put("mean",    rs.getMean());
                fd.put("samples", rs.count);
            }
            featureList.add(fd);
        }
        info.put("features",       featureList);
        info.put("baseVectorSize", baseVectorSize);
        info.put("outputSize",     outputVectorSize);
        info.put("deltaFeatures",  deltaFeatures);
        info.put("stackSize",      stackSize);
        return info;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static float toFloat(Object v, float defaultVal) {
        if (v == null)             return defaultVal;
        if (v instanceof Number)   return ((Number) v).floatValue();
        if (v instanceof Boolean)  return ((Boolean) v) ? 1f : 0f;
        try { return Float.parseFloat(v.toString()); }
        catch (NumberFormatException e) { return defaultVal; }
    }

    private static boolean toBoolean(Object v) {
        if (v == null)            return false;
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof Number)  return ((Number) v).floatValue() > 0f;
        String s = v.toString().toLowerCase();
        return "true".equals(s) || "1".equals(s) || "yes".equals(s) || "on".equals(s);
    }

    private static float[] concat(float[] a, float[] b) {
        float[] out = new float[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
