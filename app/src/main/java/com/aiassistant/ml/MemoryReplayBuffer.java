package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * MemoryReplayBuffer — Prioritized Experience Replay (PER) buffer shared across RL agents.
 *
 * Implements proportional-priority sampling using a binary segment-tree so that
 * sampling is O(log N) and priority updates are O(log N).
 *
 * Based on:
 *   Schaul et al., "Prioritized Experience Replay" (ICLR 2016).
 *
 * Usage:
 *   MemoryReplayBuffer buf = new MemoryReplayBuffer(50_000, 0.6f, 0.4f);
 *   buf.add(state, action, reward, nextState, done);
 *   List<MemoryReplayBuffer.Sample> batch = buf.sample(32);
 *   // train on batch …
 *   buf.updatePriorities(indices, tdErrors);
 */
public class MemoryReplayBuffer {

    private static final String TAG = "MemoryReplayBuffer";

    // -------------------------------------------------------------------------
    // Experience record
    // -------------------------------------------------------------------------

    public static class Experience {
        public final float[] state;
        public final int     action;
        public final float   reward;
        public final float[] nextState;
        public final boolean done;

        public Experience(float[] state, int action, float reward,
                          float[] nextState, boolean done) {
            this.state     = state;
            this.action    = action;
            this.reward    = reward;
            this.nextState = nextState;
            this.done      = done;
        }
    }

    /** Returned from {@link #sample(int)}. */
    public static class Sample {
        public final int        bufferIndex;    // needed for priority update
        public final Experience experience;
        public final float      importanceWeight; // IS correction weight (normalized)

        public Sample(int bufferIndex, Experience experience, float importanceWeight) {
            this.bufferIndex       = bufferIndex;
            this.experience        = experience;
            this.importanceWeight  = importanceWeight;
        }
    }

    // -------------------------------------------------------------------------
    // Segment tree (sum-tree + min-tree)
    // -------------------------------------------------------------------------

    /**
     * Sum segment-tree supporting O(log N) point-update and prefix-sum query.
     */
    private static class SumTree {
        private final float[] tree;
        private final int     capacity;

        SumTree(int capacity) {
            this.capacity = capacity;
            this.tree     = new float[2 * capacity];
        }

        /** Update leaf at position {@code idx} (0-indexed) to {@code value}. */
        void update(int idx, float value) {
            int pos = idx + capacity;
            tree[pos] = value;
            pos >>= 1;
            while (pos >= 1) {
                tree[pos] = tree[2 * pos] + tree[2 * pos + 1];
                pos >>= 1;
            }
        }

        /** Total sum of all priorities. */
        float total() {
            return tree[1];
        }

        /**
         * Find the leaf whose prefix-sum first reaches {@code value}.
         * Returns a 0-indexed leaf position.
         */
        int find(float value) {
            int pos = 1;
            while (pos < capacity) {
                int left = 2 * pos;
                if (value <= tree[left]) {
                    pos = left;
                } else {
                    value -= tree[left];
                    pos    = left + 1;
                }
            }
            return pos - capacity; // back to 0-indexed
        }

        float get(int idx) {
            return tree[idx + capacity];
        }
    }

    /**
     * Min segment-tree supporting O(log N) point-update and global minimum query.
     */
    private static class MinTree {
        private final float[] tree;
        private final int     capacity;

        MinTree(int capacity) {
            this.capacity = capacity;
            this.tree     = new float[2 * capacity];
            Arrays.fill(tree, Float.MAX_VALUE);
        }

        void update(int idx, float value) {
            int pos = idx + capacity;
            tree[pos] = value;
            pos >>= 1;
            while (pos >= 1) {
                tree[pos] = Math.min(tree[2 * pos], tree[2 * pos + 1]);
                pos >>= 1;
            }
        }

        float min() {
            return tree[1];
        }
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final int      capacity;
    private final float    alpha;      // priority exponent  (0 = uniform, 1 = full PER)
    private final float    betaStart;  // IS-weight exponent start (annealed to 1.0)
    private       float    beta;       // current beta (annealed each sample call)
    private final float    betaIncrement;

    private final Experience[] buffer;
    private final SumTree      sumTree;
    private final MinTree      minTree;

    private int   writePos = 0;
    private int   size     = 0;
    private float maxPriority = 1.0f;  // new experiences get max priority

    private final Random rng;

    // Statistics
    private long totalAdded   = 0;
    private long totalSampled = 0;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * @param capacity   Maximum number of experiences stored.
     * @param alpha      Priority exponent (recommended: 0.6).
     * @param betaStart  IS-weight starting exponent (recommended: 0.4, annealed to 1.0).
     */
    public MemoryReplayBuffer(int capacity, float alpha, float betaStart) {
        // Round capacity up to next power of 2 so segment trees work cleanly
        int cap = 1;
        while (cap < capacity) cap <<= 1;

        this.capacity      = cap;
        this.alpha         = Math.max(0f, alpha);
        this.betaStart     = Math.max(0f, Math.min(1f, betaStart));
        this.beta          = this.betaStart;
        // Anneal beta from betaStart to 1.0 over 100,000 sample calls
        this.betaIncrement = (1.0f - this.betaStart) / 100_000f;

        this.buffer  = new Experience[cap];
        this.sumTree = new SumTree(cap);
        this.minTree = new MinTree(cap);
        this.rng     = new Random();

        Log.i(TAG, "MemoryReplayBuffer created: capacity=" + cap
                + " alpha=" + alpha + " beta=" + betaStart);
    }

    /** Convenience constructor with recommended PER hyper-parameters. */
    public MemoryReplayBuffer(int capacity) {
        this(capacity, 0.6f, 0.4f);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Add a transition to the buffer.  New transitions receive the current maximum
     * priority so they are sampled at least once.
     */
    public synchronized void add(float[] state, int action, float reward,
                                  float[] nextState, boolean done) {
        float priority = (float) Math.pow(maxPriority + 1e-6f, alpha);

        buffer[writePos]  = new Experience(state, action, reward, nextState, done);
        sumTree.update(writePos, priority);
        minTree.update(writePos, priority);

        writePos = (writePos + 1) % capacity;
        size     = Math.min(size + 1, capacity);
        totalAdded++;
    }

    /**
     * Sample {@code batchSize} experiences using proportional-priority sampling.
     * Returns an empty list when the buffer has fewer than {@code batchSize} entries.
     */
    public synchronized List<Sample> sample(int batchSize) {
        List<Sample> batch = new ArrayList<>(batchSize);
        if (size < batchSize) return batch;

        // Anneal beta
        beta = Math.min(1.0f, beta + betaIncrement);

        float total    = sumTree.total();
        float minPrio  = minTree.min();
        float maxWeight = (float) Math.pow(size * minPrio / total, -beta);

        float segment = total / batchSize;

        for (int i = 0; i < batchSize; i++) {
            // Stratified sampling: sample uniformly from [i*segment, (i+1)*segment)
            float lower  = segment * i;
            float upper  = segment * (i + 1);
            float value  = lower + rng.nextFloat() * (upper - lower);

            int idx = sumTree.find(value);
            idx = Math.max(0, Math.min(idx, size - 1)); // clamp for safety

            Experience exp = buffer[idx];
            if (exp == null) continue; // slot not yet written

            float prio   = sumTree.get(idx);
            float weight = (float) Math.pow(size * prio / total, -beta) / maxWeight;

            batch.add(new Sample(idx, exp, weight));
        }

        totalSampled += batch.size();
        return batch;
    }

    /**
     * Update priorities after computing new TD-errors for a sampled batch.
     *
     * @param indices  Buffer indices returned in {@link Sample#bufferIndex}.
     * @param tdErrors Absolute TD-errors for each sample.
     */
    public synchronized void updatePriorities(int[] indices, float[] tdErrors) {
        if (indices == null || tdErrors == null) return;
        int n = Math.min(indices.length, tdErrors.length);
        for (int i = 0; i < n; i++) {
            float prio = (float) Math.pow(Math.abs(tdErrors[i]) + 1e-6f, alpha);
            sumTree.update(indices[i], prio);
            minTree.update(indices[i], prio);
            if (prio > maxPriority) maxPriority = prio;
        }
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public synchronized int size() {
        return size;
    }

    public int capacity() {
        return capacity;
    }

    public synchronized boolean isReady(int batchSize) {
        return size >= batchSize;
    }

    public float getBeta() {
        return beta;
    }

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> m = new HashMap<>();
        m.put("size",         size);
        m.put("capacity",     capacity);
        m.put("writePos",     writePos);
        m.put("totalAdded",   totalAdded);
        m.put("totalSampled", totalSampled);
        m.put("maxPriority",  maxPriority);
        m.put("beta",         beta);
        m.put("alpha",        alpha);
        m.put("fillRatio",    (float) size / capacity);
        return m;
    }

    /**
     * Clear all experiences and reset tree state.
     */
    public synchronized void clear() {
        Arrays.fill(buffer, null);
        Arrays.fill(sumTree.tree, 0f);
        Arrays.fill(minTree.tree, Float.MAX_VALUE);
        writePos    = 0;
        size        = 0;
        maxPriority = 1.0f;
        totalAdded  = 0;
        totalSampled = 0;
        Log.i(TAG, "MemoryReplayBuffer cleared");
    }
}
