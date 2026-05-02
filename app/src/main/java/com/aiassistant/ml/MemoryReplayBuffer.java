package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MemoryReplayBuffer — Prioritized Experience Replay (PER) buffer.
 *
 * Implements proportional prioritization as described in
 * "Prioritized Experience Replay" (Schaul et al., 2015).
 *
 * Key features:
 * - Sum-tree data structure for O(log N) priority sampling
 * - Importance-sampling weights to correct for sampling bias
 * - β annealing from β₀ → 1.0 over the training lifetime
 * - Configurable α (priority exponent) and capacity
 * - Thread-safe add/sample interface
 * - Experience stored as generic float arrays (state, action, reward, nextState, done)
 */
public class MemoryReplayBuffer {

    private static final String TAG = "MemoryReplayBuffer";

    // -------------------------------------------------------------------------
    // Hyper-parameters
    // -------------------------------------------------------------------------
    private final int    capacity;   // Maximum number of transitions
    private final float  alpha;      // Priority exponent (0 = uniform, 1 = full PER)
    private final float  betaStart;  // Initial IS-weight exponent
    private final float  betaEnd;    // Final IS-weight exponent (anneals to 1.0)
    private final float  betaFrames; // Steps over which β is annealed
    private final float  epsilon;    // Small constant to avoid zero priority

    // -------------------------------------------------------------------------
    // Sum-tree
    // -------------------------------------------------------------------------
    // The tree has `capacity` leaf nodes and (capacity - 1) internal nodes.
    // tree[i] = sum of priorities of its subtree.
    // Leaves occupy indices [capacity-1 .. 2*capacity-2].
    private final double[] tree;     // double for numerical stability
    private final Object[] data;     // Stored transition objects

    private int    writeIdx  = 0;    // Circular write pointer
    private int    size      = 0;    // Current fill level
    private final AtomicInteger totalAdded = new AtomicInteger(0);
    private int    betaStep  = 0;    // Used for β annealing

    private final Random rng = new Random(42L);

    // -------------------------------------------------------------------------
    // Inner type — one experience tuple
    // -------------------------------------------------------------------------
    public static class Experience {
        public final float[] state;
        public final int     action;
        public final float   reward;
        public final float[] nextState;
        public final boolean done;
        /** Index inside the replay buffer — used by updatePriorities(). */
        public int    bufferIdx;
        /** IS weight to multiply into the loss. */
        public float  isWeight;

        public Experience(float[] state, int action, float reward,
                          float[] nextState, boolean done) {
            this.state     = state;
            this.action    = action;
            this.reward    = reward;
            this.nextState = nextState;
            this.done      = done;
        }
    }

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * @param capacity    Maximum buffer size (must be a power of two for efficiency;
     *                    the constructor will round up automatically).
     * @param alpha       Priority exponent α ∈ [0, 1].
     * @param betaStart   Initial IS exponent β₀ (typically 0.4).
     * @param betaFrames  Number of steps for β to reach 1.0.
     */
    public MemoryReplayBuffer(int capacity, float alpha, float betaStart, float betaFrames) {
        // Round capacity up to the next power of two for the sum-tree
        int cap = 1;
        while (cap < capacity) cap <<= 1;
        this.capacity   = cap;
        this.alpha      = alpha;
        this.betaStart  = betaStart;
        this.betaEnd    = 1.0f;
        this.betaFrames = betaFrames;
        this.epsilon    = 1e-6f;

        this.tree = new double[2 * cap];
        this.data = new Object[cap];

        Log.i(TAG, "MemoryReplayBuffer created: capacity=" + cap
                + " alpha=" + alpha + " betaStart=" + betaStart);
    }

    /** Convenience constructor with sensible defaults. */
    public MemoryReplayBuffer(int capacity) {
        this(capacity, 0.6f, 0.4f, 100_000f);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Add a new experience with maximum current priority so it is sampled soon.
     */
    public synchronized void add(float[] state, int action, float reward,
                                  float[] nextState, boolean done) {
        Experience exp = new Experience(state, action, reward, nextState, done);

        // Use max current priority for new transitions (ensures all are sampled at least once)
        double maxPriority = tree[0] > 0 ? getMaxLeafPriority() : 1.0;
        int leafIdx = writeIdx + capacity - 1;
        data[writeIdx] = exp;
        updateTree(leafIdx, maxPriority);

        writeIdx = (writeIdx + 1) % capacity;
        size     = Math.min(size + 1, capacity);
        totalAdded.incrementAndGet();
    }

    /**
     * Sample a batch of {@code batchSize} prioritized experiences.
     *
     * @return List of experiences with bufferIdx and isWeight filled in.
     */
    public synchronized List<Experience> sample(int batchSize) {
        List<Experience> batch = new ArrayList<>(batchSize);
        if (size < batchSize) return batch;

        float beta      = currentBeta();
        double totalPri = tree[0];
        double segment  = totalPri / batchSize;

        // Compute max IS weight for normalization
        double minPriority = getMinLeafPriority();
        float  maxWeight   = (float) Math.pow(size * (minPriority / totalPri), -beta);
        maxWeight = Math.max(maxWeight, 1e-8f);

        for (int i = 0; i < batchSize; i++) {
            double lo  = i * segment;
            double hi  = (i + 1) * segment;
            double val = lo + rng.nextDouble() * (hi - lo);

            int treeIdx  = retrieveLeaf(val);
            int dataIdx  = treeIdx - (capacity - 1);

            if (dataIdx < 0 || dataIdx >= capacity || data[dataIdx] == null) continue;

            Experience exp = (Experience) data[dataIdx];
            double priority = tree[treeIdx];

            exp.bufferIdx = dataIdx;
            exp.isWeight  = (float) Math.pow(size * (priority / totalPri), -beta)
                            / maxWeight;
            batch.add(exp);
        }

        betaStep++;
        return batch;
    }

    /**
     * Update priorities of previously sampled experiences using new TD errors.
     *
     * @param indices   Buffer indices (from exp.bufferIdx).
     * @param tdErrors  Absolute TD errors parallel to indices.
     */
    public synchronized void updatePriorities(int[] indices, float[] tdErrors) {
        for (int i = 0; i < indices.length; i++) {
            int    idx      = indices[i];
            double priority = Math.pow(Math.abs(tdErrors[i]) + epsilon, alpha);
            int    treeIdx  = idx + capacity - 1;
            updateTree(treeIdx, priority);
        }
    }

    /** Current fill level. */
    public int size() { return size; }

    /** Total experiences ever added. */
    public int totalAdded() { return totalAdded.get(); }

    /** Whether the buffer contains at least {@code n} experiences. */
    public boolean isReady(int n) { return size >= n; }

    /** Export summary stats. */
    public Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("capacity",    capacity);
        s.put("size",        size);
        s.put("totalAdded",  totalAdded.get());
        s.put("beta",        currentBeta());
        s.put("alpha",       alpha);
        s.put("totalPriority", tree[0]);
        return s;
    }

    // -------------------------------------------------------------------------
    // Sum-tree internals
    // -------------------------------------------------------------------------

    /** Propagate a leaf priority change up to the root. */
    private void updateTree(int treeIdx, double priority) {
        double delta = priority - tree[treeIdx];
        tree[treeIdx] = priority;
        // Walk up
        while (treeIdx > 0) {
            treeIdx = (treeIdx - 1) / 2;
            tree[treeIdx] += delta;
        }
    }

    /** Find the leaf whose cumulative priority covers {@code value}. */
    private int retrieveLeaf(double value) {
        int idx = 0;
        while (true) {
            int left  = 2 * idx + 1;
            int right = left + 1;
            if (left >= tree.length) return idx; // leaf
            if (value <= tree[left]) {
                idx = left;
            } else {
                value -= tree[left];
                idx    = right;
            }
        }
    }

    /** Maximum priority among current leaves. */
    private double getMaxLeafPriority() {
        double max = 0.0;
        int start  = capacity - 1;
        for (int i = start; i < start + size; i++) {
            if (tree[i] > max) max = tree[i];
        }
        return max == 0.0 ? 1.0 : max;
    }

    /** Minimum non-zero priority among current leaves. */
    private double getMinLeafPriority() {
        double min = Double.MAX_VALUE;
        int start  = capacity - 1;
        for (int i = start; i < start + size; i++) {
            if (tree[i] > 0 && tree[i] < min) min = tree[i];
        }
        return min == Double.MAX_VALUE ? epsilon : min;
    }

    /** Current β value, annealed linearly from betaStart → betaEnd. */
    private float currentBeta() {
        float progress = Math.min(1.0f, betaStep / betaFrames);
        return betaStart + progress * (betaEnd - betaStart);
    }
}
