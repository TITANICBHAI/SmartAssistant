package com.aiassistant.learning;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Episodic contextual memory for the AI assistant.
 *
 * Stores a rolling window of (context → action → outcome) episodes and
 * allows fast retrieval of the most similar past episodes via cosine
 * similarity on a compact numeric feature vector.
 *
 * Features:
 *  1. Rolling buffer — oldest entries evicted when capacity is full.
 *  2. Cosine-similarity retrieval — returns the top-K most similar past
 *     episodes for a given query context.
 *  3. Feature normalisation — each numeric feature is tracked min/max so
 *     similarity is not dominated by large-magnitude dimensions.
 *  4. JSON persistence — survives app restarts.
 *  5. Thread-safe via ReadWriteLock.
 *  6. Recency boost — similarity score is scaled by a decay factor based on
 *     how recently the episode was recorded.
 */
public class ContextualMemory {
    private static final String TAG = "ContextualMemory";

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------
    private static final int   MAX_CAPACITY   = 1_000;
    private static final float RECENCY_HALF_LIFE_MS = 7 * 24 * 3_600_000f; // 7 days
    private static final String MEMORY_FILE   = "contextual_memory.json";

    // -----------------------------------------------------------------------
    // Episode model
    // -----------------------------------------------------------------------
    public static class Episode {
        public final String              id;
        public final Map<String, Object> context;
        public final String              action;
        public final float               reward;
        public final long                timestamp;
        final float[] featureVector;          // pre-computed for fast similarity

        Episode(String id, Map<String, Object> context, String action,
                float reward, float[] featureVector) {
            this.id            = id;
            this.context       = new HashMap<>(context);
            this.action        = action;
            this.reward        = reward;
            this.timestamp     = System.currentTimeMillis();
            this.featureVector = featureVector.clone();
        }
    }

    // -----------------------------------------------------------------------
    // Retrieval result
    // -----------------------------------------------------------------------
    public static class MemoryMatch {
        public final Episode episode;
        public final float   similarity;
        MemoryMatch(Episode e, float s) { episode = e; similarity = s; }
    }

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------
    private final List<Episode>      episodes = new ArrayList<>();
    private final ReadWriteLock      lock     = new ReentrantReadWriteLock();
    private final File               storageFile;

    // Feature key registry — maps feature name → index in vector
    private final Map<String, Integer> featureIndex = new HashMap<>();
    private       int                  featureDim   = 0;

    // Running min/max for normalisation
    private final Map<String, float[]> featureRange = new HashMap<>(); // key → [min, max]

    private int episodeCounter = 0;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------
    public ContextualMemory(File storageDir) {
        storageFile = new File(storageDir, MEMORY_FILE);
        load();
    }

    // -----------------------------------------------------------------------
    // Store
    // -----------------------------------------------------------------------

    /**
     * Records a new episode.
     * @param context  Feature map (numeric values preferred)
     * @param action   Action that was taken
     * @param reward   Outcome reward
     */
    public void store(Map<String, Object> context, String action, float reward) {
        float[] vec = toVector(context);

        lock.writeLock().lock();
        try {
            if (episodes.size() >= MAX_CAPACITY) episodes.remove(0);
            String id = "ep_" + (++episodeCounter);
            episodes.add(new Episode(id, context, action, reward, vec));
        } finally {
            lock.writeLock().unlock();
        }
    }

    // -----------------------------------------------------------------------
    // Retrieve
    // -----------------------------------------------------------------------

    /**
     * Returns the top-K episodes most similar to the query context.
     * @param queryContext  Feature map of the current state
     * @param topK          Number of results to return
     */
    public List<MemoryMatch> retrieve(Map<String, Object> queryContext, int topK) {
        float[] qVec = toVector(queryContext);
        long    now  = System.currentTimeMillis();

        lock.readLock().lock();
        List<Episode> snap;
        try { snap = new ArrayList<>(episodes); }
        finally { lock.readLock().unlock(); }

        List<MemoryMatch> matches = new ArrayList<>(snap.size());
        for (Episode ep : snap) {
            float cos     = cosineSimilarity(qVec, ep.featureVector);
            float recency = recencyFactor(ep.timestamp, now);
            matches.add(new MemoryMatch(ep, cos * recency));
        }

        // Sort descending by similarity
        Collections.sort(matches, (a, b) -> Float.compare(b.similarity, a.similarity));

        int limit = Math.min(topK, matches.size());
        return matches.subList(0, limit);
    }

    /**
     * Returns the single most similar episode, or null if memory is empty.
     */
    public Episode retrieveBest(Map<String, Object> queryContext) {
        List<MemoryMatch> top = retrieve(queryContext, 1);
        return top.isEmpty() ? null : top.get(0).episode;
    }

    // -----------------------------------------------------------------------
    // Stats / utility
    // -----------------------------------------------------------------------
    public int size()  { lock.readLock().lock(); try { return episodes.size(); } finally { lock.readLock().unlock(); } }
    public void clear() { lock.writeLock().lock(); try { episodes.clear(); } finally { lock.writeLock().unlock(); } }

    // -----------------------------------------------------------------------
    // Persistence
    // -----------------------------------------------------------------------

    public void save() {
        try {
            lock.readLock().lock();
            List<Episode> snap;
            try { snap = new ArrayList<>(episodes); }
            finally { lock.readLock().unlock(); }

            JSONArray arr = new JSONArray();
            for (Episode ep : snap) {
                JSONObject obj = new JSONObject();
                obj.put("id",        ep.id);
                obj.put("action",    ep.action);
                obj.put("reward",    ep.reward);
                obj.put("timestamp", ep.timestamp);
                JSONObject ctx = new JSONObject();
                for (Map.Entry<String, Object> e : ep.context.entrySet())
                    ctx.put(e.getKey(), e.getValue().toString());
                obj.put("context", ctx);
                arr.put(obj);
            }
            if (!storageFile.getParentFile().exists()) storageFile.getParentFile().mkdirs();
            try (FileWriter fw = new FileWriter(storageFile)) { fw.write(arr.toString(2)); }
            Log.d(TAG, "Saved " + snap.size() + " episodes");
        } catch (Exception e) {
            Log.e(TAG, "Save error", e);
        }
    }

    private void load() {
        if (!storageFile.exists()) return;
        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new FileReader(storageFile))) {
                String line; while ((line = br.readLine()) != null) sb.append(line);
            }
            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                JSONObject ctx = obj.getJSONObject("context");
                Map<String, Object> context = new HashMap<>();
                Iterator<String> keys = ctx.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    String v = ctx.getString(k);
                    try { context.put(k, Double.parseDouble(v)); }
                    catch (NumberFormatException ex) { context.put(k, v); }
                }
                float[] vec = toVector(context);
                episodes.add(new Episode(
                        obj.getString("id"), context,
                        obj.getString("action"),
                        (float) obj.optDouble("reward", 0.0), vec));
            }
            Log.d(TAG, "Loaded " + episodes.size() + " episodes");
        } catch (Exception e) {
            Log.e(TAG, "Load error", e);
        }
    }

    // -----------------------------------------------------------------------
    // Feature vector helpers
    // -----------------------------------------------------------------------

    private synchronized float[] toVector(Map<String, Object> ctx) {
        // Register new feature keys
        for (String k : ctx.keySet()) {
            if (!featureIndex.containsKey(k) && ctx.get(k) instanceof Number) {
                featureIndex.put(k, featureDim++);
            }
        }

        float[] vec = new float[featureDim];
        for (Map.Entry<String, Object> e : ctx.entrySet()) {
            Integer idx = featureIndex.get(e.getKey());
            if (idx == null || !(e.getValue() instanceof Number)) continue;
            double raw = ((Number) e.getValue()).doubleValue();

            // Update running range
            float[] range = featureRange.computeIfAbsent(e.getKey(), k -> new float[]{Float.MAX_VALUE, Float.MIN_VALUE});
            range[0] = (float) Math.min(range[0], raw);
            range[1] = (float) Math.max(range[1], raw);

            // Normalise to [0, 1]
            float span = range[1] - range[0];
            vec[idx] = span > 0 ? (float) ((raw - range[0]) / span) : 0f;
        }
        return vec;
    }

    private static float cosineSimilarity(float[] a, float[] b) {
        int len = Math.min(a.length, b.length);
        if (len == 0) return 0f;
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < len; i++) {
            dot   += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom > 0 ? (float) (dot / denom) : 0f;
    }

    /** Exponential recency decay: 1.0 → 0.5 over RECENCY_HALF_LIFE_MS. */
    private static float recencyFactor(long episodeTime, long now) {
        float ageDays = (now - episodeTime) / RECENCY_HALF_LIFE_MS;
        return (float) Math.pow(0.5, ageDays);
    }
}
