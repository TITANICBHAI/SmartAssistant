package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * StateClusterer — online k-means clustering of state vectors for:
 *   - Hierarchical RL (abstract state representations)
 *   - Exploration bonus via cluster novelty
 *   - State-space coverage monitoring
 *   - Reward function shaping (encourage visiting new clusters)
 *
 * Algorithm:
 *   Online mini-batch k-means with:
 *     1. Forgy initialisation (k random seed states from first N observations)
 *     2. Soft-update: centroid ← (1−lr)·centroid + lr·x  when x is assigned to it
 *     3. Centroid refresh: re-initialise dead centroids (not updated in patience steps)
 *     4. Cluster novelty score = 1 / √(visits[cluster] + 1)
 *
 * Thread-safe.
 */
public class StateClusterer {

    private static final String TAG = "StateClusterer";

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int k;          // number of clusters
    private final int stateDim;
    private final float lr;       // centroid soft-update rate
    private final int refreshPatience; // steps before dead centroid is reset

    private final float[][] centroids;  // [k][stateDim]
    private final int[]     visits;     // cluster visit count
    private final int[]     lastUpdate; // step at which centroid was last moved
    private int             stepCount   = 0;
    private boolean         initialized = false;

    // Initialisation buffer
    private final List<float[]> initBuffer;
    private final int initBufferSize;

    // Stats
    private final AtomicInteger totalAssignments = new AtomicInteger(0);
    private final AtomicInteger refreshCount     = new AtomicInteger(0);
    private float avgInertia = 0f;

    private final Random rng = new Random(53L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public StateClusterer(int k, int stateDim, float lr, int refreshPatience) {
        this.k               = k;
        this.stateDim        = stateDim;
        this.lr              = lr;
        this.refreshPatience = refreshPatience;
        this.centroids       = new float[k][stateDim];
        this.visits          = new int[k];
        this.lastUpdate      = new int[k];
        this.initBufferSize  = k * 3;
        this.initBuffer      = new ArrayList<>(initBufferSize);
    }

    public StateClusterer(int k, int stateDim) {
        this(k, stateDim, 0.05f, 500);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Assign state to its nearest cluster and update centroid.
     *
     * @param state State feature vector.
     * @return Cluster index [0, k).
     */
    public synchronized int assign(float[] state) {
        if (!initialized) {
            initBuffer.add(pad(state));
            if (initBuffer.size() >= initBufferSize) initialize();
            else return 0;
        }

        int cluster = nearest(state);
        visits[cluster]++;
        lastUpdate[cluster] = stepCount;
        stepCount++;

        // Soft centroid update
        float[] c = centroids[cluster];
        float[] s = pad(state);
        for (int i = 0; i < stateDim; i++) c[i] = (1f - lr) * c[i] + lr * s[i];

        // Inertia tracking
        float dist = l2sq(s, c);
        avgInertia = 0.99f * avgInertia + 0.01f * dist;
        totalAssignments.incrementAndGet();

        // Refresh dead centroids
        refreshDeadCentroids(s);

        return cluster;
    }

    /**
     * Cluster novelty score — higher = less visited cluster.
     * @return 1 / √(visits[nearest_cluster] + 1)
     */
    public synchronized float noveltyScore(float[] state) {
        if (!initialized) return 1f;
        int c = nearest(state);
        return 1f / (float) Math.sqrt(visits[c] + 1);
    }

    /**
     * Get centroid vector for cluster c.
     */
    public synchronized float[] getCentroid(int c) {
        return centroids[c].clone();
    }

    /**
     * Get cluster assignment for state without updating the model.
     */
    public synchronized int predict(float[] state) {
        if (!initialized) return 0;
        return nearest(state);
    }

    /**
     * Cluster coverage: fraction of clusters visited at least once.
     */
    public synchronized float coverage() {
        int visited = 0;
        for (int v : visits) if (v > 0) visited++;
        return (float) visited / k;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("k",               k);
        s.put("stateDim",        stateDim);
        s.put("initialized",     initialized);
        s.put("totalAssignments",totalAssignments.get());
        s.put("refreshCount",    refreshCount.get());
        s.put("avgInertia",      avgInertia);
        s.put("coverage",        coverage());
        // Cluster visit distribution
        int maxV = 0; for (int v : visits) if (v > maxV) maxV = v;
        s.put("maxClusterVisits", maxV);
        int deadCount = 0;
        for (int v : visits) if (v == 0) deadCount++;
        s.put("deadClusters", deadCount);
        return s;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal
    // ─────────────────────────────────────────────────────────────────────────

    private void initialize() {
        // Forgy: pick k random states from initBuffer
        List<float[]> seeds = new ArrayList<>(initBuffer);
        for (int c = 0; c < k; c++) {
            int idx = rng.nextInt(seeds.size());
            centroids[c] = seeds.get(idx).clone();
        }
        initialized = true;
        Log.i(TAG, "Initialized " + k + " centroids from " + initBuffer.size() + " samples.");
        initBuffer.clear();
    }

    private int nearest(float[] state) {
        float[] s    = pad(state);
        int     best = 0;
        float   bestD= Float.MAX_VALUE;
        for (int c = 0; c < k; c++) {
            float d = l2sq(s, centroids[c]);
            if (d < bestD) { bestD = d; best = c; }
        }
        return best;
    }

    private void refreshDeadCentroids(float[] currentState) {
        for (int c = 0; c < k; c++) {
            if (stepCount - lastUpdate[c] > refreshPatience && visits[c] == 0) {
                // Re-initialise dead centroid near a random live centroid + noise
                int liveSeed = rng.nextInt(k);
                for (int i = 0; i < stateDim; i++)
                    centroids[c][i] = centroids[liveSeed][i] + (rng.nextFloat() - 0.5f) * 0.1f;
                lastUpdate[c] = stepCount;
                refreshCount.incrementAndGet();
            }
        }
    }

    private float[] pad(float[] x) {
        if (x.length == stateDim) return x;
        float[] p = new float[stateDim];
        System.arraycopy(x, 0, p, 0, Math.min(x.length, stateDim));
        return p;
    }

    private static float l2sq(float[] a, float[] b) {
        float s = 0f;
        for (int i = 0; i < a.length; i++) { float d = a[i] - b[i]; s += d * d; }
        return s;
    }
}
