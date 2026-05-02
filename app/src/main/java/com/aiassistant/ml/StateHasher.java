package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * StateHasher — efficient state deduplication and novelty detection.
 *
 * Implements Locality-Sensitive Hashing (LSH) for float feature vectors:
 *
 *   1. SimHash / Random Projection LSH:
 *      For each of K hash functions h_k, project the state onto a random
 *      hyperplane and take the sign.  The K-bit binary code is the LSH fingerprint.
 *      States with similar fingerprints have high cosine similarity.
 *
 *   2. Count-Min Sketch for visit counting:
 *      Approximate visit count per unique state hash using a memory-efficient
 *      count-min sketch (d arrays of width w). O(1) update and query.
 *
 *   3. Novelty score:  1 / sqrt(visitCount(hash)) — same as CuriosityModule bonus
 *      but computed entirely from the hash without storing a full state map.
 *
 *   4. State deduplication:  two states are considered "identical" if their LSH
 *      fingerprints match exactly — useful for loop detection.
 *
 * Memory: O(d × w) for the sketch + O(K/8) per hash computation.
 * Time:   O(K × stateDim) per hash computation.
 *
 * Thread-safe.
 */
public class StateHasher {

    private static final String TAG = "StateHasher";

    // -------------------------------------------------------------------------
    // LSH parameters
    // -------------------------------------------------------------------------
    private final int stateDim;
    private final int numHyperplanes;   // K — bits per hash code
    private final float[][] hyperplanes; // [K][stateDim] — random unit vectors

    // -------------------------------------------------------------------------
    // Count-Min Sketch
    // -------------------------------------------------------------------------
    private final int   cmDepth = 5;    // d — number of hash functions
    private final int   cmWidth = 4096; // w — sketch width (power of 2 for fast mod)
    private final int[][] cmSketch;     // [d][w] — count table
    private final long[] cmSeeds;       // seeds for each CMS hash function

    // -------------------------------------------------------------------------
    // Exact count for known hashes (small map for frequently-seen states)
    // -------------------------------------------------------------------------
    private final ConcurrentHashMap<String, Integer> exactCounts = new ConcurrentHashMap<>();
    private static final int EXACT_CACHE_MAX = 2000;

    // -------------------------------------------------------------------------
    // Stats
    // -------------------------------------------------------------------------
    private final AtomicLong   totalHashes   = new AtomicLong(0);
    private final AtomicInteger uniqueHashes = new AtomicInteger(0);
    private volatile float     avgNovelty    = 0f;

    private final Random rng;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * @param stateDim        Dimension of input state feature vectors.
     * @param numHyperplanes  Number of LSH bits (32–128 recommended). More bits = finer granularity.
     * @param seed            Random seed for reproducibility.
     */
    public StateHasher(int stateDim, int numHyperplanes, long seed) {
        this.stateDim       = stateDim;
        this.numHyperplanes = numHyperplanes;
        this.rng            = new Random(seed);

        // Generate random hyperplanes (unit vectors)
        hyperplanes = new float[numHyperplanes][stateDim];
        for (int k = 0; k < numHyperplanes; k++) {
            float norm = 0f;
            for (int d = 0; d < stateDim; d++) {
                hyperplanes[k][d] = (float) rng.nextGaussian();
                norm += hyperplanes[k][d] * hyperplanes[k][d];
            }
            norm = (float) Math.sqrt(norm);
            if (norm > 1e-8f) for (int d = 0; d < stateDim; d++) hyperplanes[k][d] /= norm;
        }

        // Count-Min Sketch setup
        cmSketch = new int[cmDepth][cmWidth];
        cmSeeds  = new long[cmDepth];
        for (int i = 0; i < cmDepth; i++) cmSeeds[i] = rng.nextLong();

        Log.i(TAG, "StateHasher: stateDim=" + stateDim
                + " numHyperplanes=" + numHyperplanes
                + " cmDepth=" + cmDepth + " cmWidth=" + cmWidth);
    }

    public StateHasher(int stateDim) {
        this(stateDim, 64, 31L);
    }

    // -------------------------------------------------------------------------
    // Core API
    // -------------------------------------------------------------------------

    /**
     * Compute the LSH fingerprint of a state vector.
     * @return String of '0'/'1' characters, length = numHyperplanes.
     */
    public String computeHash(float[] state) {
        int dim = Math.min(state.length, stateDim);
        char[] bits = new char[numHyperplanes];
        for (int k = 0; k < numHyperplanes; k++) {
            float dot = 0;
            for (int d = 0; d < dim; d++) dot += hyperplanes[k][d] * state[d];
            bits[k] = dot >= 0 ? '1' : '0';
        }
        return new String(bits);
    }

    /**
     * Record a visit to this state and return its updated visit count.
     */
    public int recordVisit(float[] state) {
        String hash = computeHash(state);
        totalHashes.incrementAndGet();

        // Count-Min Sketch update
        cmIncrement(hash);

        // Exact count update (bounded cache)
        int exactCount;
        Integer prev = exactCounts.get(hash);
        if (prev == null) {
            if (exactCounts.size() < EXACT_CACHE_MAX) {
                exactCounts.put(hash, 1);
                uniqueHashes.incrementAndGet();
            }
            exactCount = 1;
        } else {
            exactCount = prev + 1;
            exactCounts.put(hash, exactCount);
        }

        // Update avg novelty EMA
        float novelty = 1f / (float) Math.sqrt(exactCount);
        avgNovelty = 0.99f * avgNovelty + 0.01f * novelty;

        return exactCount;
    }

    /**
     * Get visit count for a state (uses exact cache when available, CMS otherwise).
     */
    public int getVisitCount(float[] state) {
        String hash = computeHash(state);
        Integer exact = exactCounts.get(hash);
        if (exact != null) return exact;
        return Math.max(1, cmQuery(hash));
    }

    /**
     * Compute novelty score:  1 / sqrt(visitCount).  Returns 1.0 for never-seen states.
     */
    public float computeNovelty(float[] state) {
        int visits = getVisitCount(state);
        return 1f / (float) Math.sqrt(visits);
    }

    /**
     * Check whether two states are considered "identical" by their LSH fingerprints.
     */
    public boolean areIdentical(float[] stateA, float[] stateB) {
        return computeHash(stateA).equals(computeHash(stateB));
    }

    /**
     * Hamming distance between two state hashes (number of differing bits).
     * Lower distance → more similar states.
     */
    public int hammingDistance(float[] stateA, float[] stateB) {
        String hA = computeHash(stateA);
        String hB = computeHash(stateB);
        int dist = 0;
        for (int i = 0; i < numHyperplanes; i++) if (hA.charAt(i) != hB.charAt(i)) dist++;
        return dist;
    }

    /**
     * Cosine similarity approximation from Hamming distance.
     * cos θ ≈ cos(π · hammingDist / numHyperplanes)
     */
    public float cosineSimilarity(float[] stateA, float[] stateB) {
        int hd = hammingDistance(stateA, stateB);
        return (float) Math.cos(Math.PI * hd / numHyperplanes);
    }

    /**
     * Reset all visit counts and statistics.
     */
    public void reset() {
        exactCounts.clear();
        for (int[] row : cmSketch) Arrays.fill(row, 0);
        totalHashes.set(0);
        uniqueHashes.set(0);
        avgNovelty = 0f;
    }

    // -------------------------------------------------------------------------
    // Count-Min Sketch internals
    // -------------------------------------------------------------------------

    private void cmIncrement(String hash) {
        int code = hash.hashCode();
        for (int i = 0; i < cmDepth; i++) {
            int col = (int)((code ^ cmSeeds[i]) & (cmWidth - 1));
            if (col < 0) col += cmWidth;
            cmSketch[i][col]++;
        }
    }

    private int cmQuery(String hash) {
        int code = hash.hashCode();
        int minCount = Integer.MAX_VALUE;
        for (int i = 0; i < cmDepth; i++) {
            int col = (int)((code ^ cmSeeds[i]) & (cmWidth - 1));
            if (col < 0) col += cmWidth;
            if (cmSketch[i][col] < minCount) minCount = cmSketch[i][col];
        }
        return Math.max(0, minCount);
    }

    // -------------------------------------------------------------------------
    // Monitoring
    // -------------------------------------------------------------------------

    public Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("totalHashes",    totalHashes.get());
        s.put("uniqueHashes",   uniqueHashes.get());
        s.put("avgNovelty",     avgNovelty);
        s.put("cacheSize",      exactCounts.size());
        s.put("numHyperplanes", numHyperplanes);
        s.put("stateDim",       stateDim);
        // Top-5 most visited hashes
        List<Map<String, Object>> top = new ArrayList<>();
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(exactCounts.entrySet());
        entries.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        for (int i = 0; i < Math.min(5, entries.size()); i++) {
            Map<String, Object> e = new HashMap<>();
            e.put("hash",   entries.get(i).getKey().substring(0, Math.min(16, numHyperplanes)) + "…");
            e.put("visits", entries.get(i).getValue());
            top.add(e);
        }
        s.put("topVisitedStates", top);
        return s;
    }
}
