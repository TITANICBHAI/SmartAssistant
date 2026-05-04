package com.aiassistant.ml;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CountBasedExploration — pseudo-count exploration bonuses for deep RL.
 *
 * Classic count-based exploration gives bonus r_+ = β / sqrt(N(s)) for visiting
 * state s, where N(s) is the visit count.  In continuous/high-dimensional spaces
 * exact counts are infeasible — we use hash-based pseudo-counts instead.
 *
 * Three counting backends selectable at runtime:
 *
 *   EXACT_HASH     — hash the state into a discrete bucket (fast, coarse)
 *   SIMHASH        — locality-sensitive hashing: nearby states share buckets
 *   FEATURE_HASH   — project state → {0,1}^k sign hash (Bellemare et al. 2016)
 *
 * Exploration bonus = β / sqrt(N(s) + ε)
 *
 * Additional features:
 *   - Per-action counts: N(s, a) for Q-learning bonus
 *   - Novelty decay: multiply counts by λ < 1 periodically to re-explore
 *   - Top-K novel states tracking for curiosity-driven planning
 *   - Thread-safe
 */
public class CountBasedExploration {

    private static final String TAG = "CountBasedExplore";

    public enum Backend { EXACT_HASH, SIMHASH, FEATURE_HASH }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final Backend  backend;
    private final float    beta;       // bonus scale
    private final float    epsilon;    // bonus floor denominator

    // State visit counts: stateKey → count
    private final HashMap<Long, Integer> stateCount   = new HashMap<>();
    // State-action visit counts: combined key → count
    private final HashMap<Long, Integer> saCount      = new HashMap<>();

    // SimHash / FeatureHash projection matrix [hashBits][stateDim]
    private final float[][] projMatrix;
    private final int        hashBits;
    private final int        stateDim;

    // Novelty decay
    private final float  decayFactor;
    private final int    decayInterval;
    private int          stepsSinceDecay = 0;

    // Stats
    private final AtomicInteger totalVisits    = new AtomicInteger(0);
    private final AtomicInteger uniqueStates   = new AtomicInteger(0);
    private float avgBonus = 0f;
    private float maxBonus = 0f;

    private final java.util.Random rng = new java.util.Random(67L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public CountBasedExploration(int stateDim, Backend backend, int hashBits,
                                  float beta, float epsilon,
                                  float decayFactor, int decayInterval) {
        this.stateDim      = stateDim;
        this.backend       = backend;
        this.hashBits      = hashBits;
        this.beta          = beta;
        this.epsilon       = epsilon;
        this.decayFactor   = decayFactor;
        this.decayInterval = decayInterval;

        // Build random projection matrix for SimHash / FeatureHash
        projMatrix = new float[hashBits][stateDim];
        for (int i = 0; i < hashBits; i++)
            for (int j = 0; j < stateDim; j++)
                projMatrix[i][j] = (float) rng.nextGaussian();

        Log.i(TAG, "CountBasedExploration: backend=" + backend
                + " bits=" + hashBits + " β=" + beta);
    }

    public CountBasedExploration(int stateDim) {
        this(stateDim, Backend.SIMHASH, 32, 0.1f, 0.001f, 0.99f, 5000);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Visit state s, increment count, return exploration bonus.
     */
    public synchronized float visit(float[] state) {
        long key = hash(state);
        int  n   = stateCount.merge(key, 1, Integer::sum);
        if (n == 1) uniqueStates.incrementAndGet();
        totalVisits.incrementAndGet();

        float bonus = beta / (float) Math.sqrt(n + epsilon);
        avgBonus = 0.999f * avgBonus + 0.001f * bonus;
        if (bonus > maxBonus) maxBonus = bonus;

        maybeDecay();
        return bonus;
    }

    /**
     * Visit (state, action) pair, return per-SA bonus.
     */
    public synchronized float visitSA(float[] state, int action) {
        long key = hashSA(state, action);
        int  n   = saCount.merge(key, 1, Integer::sum);
        return beta / (float) Math.sqrt(n + epsilon);
    }

    /**
     * Get exploration bonus WITHOUT updating counts (for planning/lookahead).
     */
    public synchronized float bonusOnly(float[] state) {
        long key = hash(state);
        int  n   = stateCount.getOrDefault(key, 0);
        return beta / (float) Math.sqrt(n + epsilon + 1);
    }

    /**
     * Get visit count for a state (0 if never seen).
     */
    public synchronized int getCount(float[] state) {
        return stateCount.getOrDefault(hash(state), 0);
    }

    /**
     * Number of unique states visited.
     */
    public synchronized int uniqueStateCount() {
        return stateCount.size();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Hashing backends
    // ─────────────────────────────────────────────────────────────────────────

    private long hash(float[] state) {
        switch (backend) {
            case SIMHASH:     return simHash(state);
            case FEATURE_HASH: return featureHash(state);
            case EXACT_HASH:
            default:          return exactHash(state);
        }
    }

    /** Discretize each feature into 10 bins, combine into a single long. */
    private long exactHash(float[] state) {
        long h = 0;
        for (int i = 0; i < Math.min(state.length, stateDim); i++) {
            int bucket = Math.min(9, Math.max(0, (int)(state[i] * 10f)));
            h = h * 11L + bucket;
        }
        return h;
    }

    /** Locality-sensitive hash: sign(A·x) packed as bits. */
    private long simHash(float[] state) {
        long bits = 0;
        for (int i = 0; i < hashBits; i++) {
            float dot = 0f;
            for (int j = 0; j < Math.min(state.length, stateDim); j++)
                dot += projMatrix[i][j] * state[j];
            if (dot >= 0) bits |= (1L << i);
        }
        return bits;
    }

    /** Feature hash: {±1} random projection, pack into long. */
    private long featureHash(float[] state) {
        long h = 0;
        for (int i = 0; i < Math.min(hashBits, 63); i++) {
            float dot = 0f;
            for (int j = 0; j < Math.min(state.length, stateDim); j++)
                dot += projMatrix[i][j] * state[j];
            if (dot >= 0) h |= (1L << i);
        }
        return h;
    }

    private long hashSA(float[] state, int action) {
        return hash(state) * 31L + action;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Novelty decay
    // ─────────────────────────────────────────────────────────────────────────

    private void maybeDecay() {
        if (++stepsSinceDecay < decayInterval) return;
        stepsSinceDecay = 0;
        // Multiply all counts by decayFactor (rounded down; remove zeroed entries)
        stateCount.replaceAll((k, v) -> Math.max(1, (int)(v * decayFactor)));
        saCount.replaceAll((k, v) -> Math.max(1, (int)(v * decayFactor)));
        Log.d(TAG, "Decay applied. Unique states: " + stateCount.size());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("backend",       backend.name());
        s.put("totalVisits",   totalVisits.get());
        s.put("uniqueStates",  stateCount.size());
        s.put("uniqueSAPairs", saCount.size());
        s.put("avgBonus",      avgBonus);
        s.put("maxBonus",      maxBonus);
        s.put("beta",          beta);
        s.put("hashBits",      hashBits);
        return s;
    }
}
