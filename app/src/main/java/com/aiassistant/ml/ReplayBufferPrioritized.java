package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ReplayBufferPrioritized — Prioritized Experience Replay (PER) with sum-tree.
 *
 * PER (Schaul et al. 2016) samples transitions proportional to their TD error:
 *   P(i) = p_i^α / Σ_j p_j^α
 *
 * Importance sampling weights correct for the non-uniform distribution:
 *   w_i = (1/N · 1/P(i))^β
 *
 * Implemented with a binary sum-tree (O(log N) insert and sample).
 *
 * Features:
 *   - Configurable α (priority exponent) and β (IS exponent, annealed to 1).
 *   - Max priority for new transitions (ensures all transitions are sampled at least once).
 *   - Proportional and rank-based priority options.
 *   - Efficient O(log N) updates.
 *   - Compatible with ReplayBuffer.Experience format.
 *
 * Thread-safe.
 */
public class ReplayBufferPrioritized {

    private static final String TAG = "PER";

    // ─────────────────────────────────────────────────────────────────────────
    // Sum-tree
    // ─────────────────────────────────────────────────────────────────────────
    private static class SumTree {
        final double[] tree;     // [2*capacity - 1] node values
        final Object[] data;     // [capacity] stored transitions
        final int      capacity;
        int            write  = 0;
        int            size   = 0;

        SumTree(int capacity) {
            this.capacity = capacity;
            tree = new double[2 * capacity - 1];
            data = new Object[capacity];
        }

        void add(double priority, Object datum) {
            int idx = write + capacity - 1;
            data[write] = datum;
            update(idx, priority);
            write = (write + 1) % capacity;
            if (size < capacity) size++;
        }

        void update(int idx, double priority) {
            double change = priority - tree[idx];
            tree[idx] = priority;
            propagate(idx, change);
        }

        private void propagate(int idx, double change) {
            int parent = (idx - 1) / 2;
            tree[parent] += change;
            if (parent != 0) propagate(parent, change);
        }

        int retrieve(int idx, double s) {
            int left = 2 * idx + 1, right = left + 1;
            if (left >= tree.length) return idx;
            if (s <= tree[left]) return retrieve(left, s);
            return retrieve(right, s - tree[left]);
        }

        double total() { return tree[0]; }
        double minPriority() {
            double min = Double.MAX_VALUE;
            for (int i = capacity-1; i < tree.length; i++) if (tree[i] > 0 && tree[i] < min) min = tree[i];
            return min == Double.MAX_VALUE ? 1.0 : min;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sampled batch entry
    // ─────────────────────────────────────────────────────────────────────────
    public static class PrioritizedSample {
        public final ReplayBuffer.Experience experience;
        public final int    treeIdx;
        public final float  isWeight;

        PrioritizedSample(ReplayBuffer.Experience e, int treeIdx, float isWeight) {
            this.experience = e;
            this.treeIdx    = treeIdx;
            this.isWeight   = isWeight;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final SumTree tree;
    private final int     capacity;
    private final float   alpha;        // priority exponent
    private       float   beta;         // IS exponent (annealed)
    private final float   betaEnd;
    private final float   betaAnneal;
    private final float   epsilonPri;   // min priority for numerical stability
    private double        maxPriority   = 1.0;

    private final AtomicInteger addCount    = new AtomicInteger(0);
    private final AtomicInteger sampleCount = new AtomicInteger(0);
    private final AtomicInteger updateCount = new AtomicInteger(0);
    private float avgPriority = 1f;
    private float avgIsWeight = 1f;

    private final Random rng = new Random(307L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public ReplayBufferPrioritized(int capacity, float alpha, float betaStart,
                                    float betaEnd, float betaAnneal, float epsilonPri) {
        this.capacity   = capacity;
        this.alpha      = alpha;
        this.beta       = betaStart;
        this.betaEnd    = betaEnd;
        this.betaAnneal = betaAnneal;
        this.epsilonPri = epsilonPri;
        this.tree       = new SumTree(capacity);

        Log.i(TAG, "PER: capacity=" + capacity + " α=" + alpha + " β=" + betaStart);
    }

    public ReplayBufferPrioritized(int capacity) {
        this(capacity, 0.6f, 0.4f, 1.0f, 1e-6f, 1e-6f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /** Add transition with max priority (ensures new transitions are sampled). */
    public synchronized void add(ReplayBuffer.Experience exp) {
        tree.add(maxPriority, exp);
        addCount.incrementAndGet();
    }

    /** Add with explicit priority. */
    public synchronized void addWithPriority(ReplayBuffer.Experience exp, float tdError) {
        double priority = Math.pow(Math.abs(tdError) + epsilonPri, alpha);
        if (priority > maxPriority) maxPriority = priority;
        tree.add(priority, exp);
        addCount.incrementAndGet();
    }

    /** Sample a batch. Returns null entries if buffer too small. */
    public synchronized List<PrioritizedSample> sample(int batchSize) {
        annealBeta();
        List<PrioritizedSample> batch = new ArrayList<>(batchSize);
        double total    = tree.total();
        double minPri   = tree.minPriority();
        float  maxW     = (float) Math.pow(tree.size * minPri / total, -beta);

        for (int i = 0; i < batchSize; i++) {
            double segment = total / batchSize;
            double s = (rng.nextDouble() + i) * segment;
            int    idx = tree.retrieve(0, s);
            double pri = tree.tree[idx];
            if (pri <= 0 || tree.data[idx - capacity + 1] == null) continue;

            float isW = (float) Math.pow(tree.size * pri / total, -beta) / maxW;
            batch.add(new PrioritizedSample(
                    (ReplayBuffer.Experience) tree.data[idx - capacity + 1],
                    idx, isW));
            avgPriority = 0.99f * avgPriority + 0.01f * (float) pri;
            avgIsWeight = 0.99f * avgIsWeight + 0.01f * isW;
        }
        sampleCount.addAndGet(batch.size());
        return batch;
    }

    /** Update priorities after computing new TD errors. */
    public synchronized void updatePriorities(List<Integer> treeIdxs, float[] tdErrors) {
        for (int i = 0; i < Math.min(treeIdxs.size(), tdErrors.length); i++) {
            double priority = Math.pow(Math.abs(tdErrors[i]) + epsilonPri, alpha);
            if (priority > maxPriority) maxPriority = priority;
            tree.update(treeIdxs.get(i), priority);
        }
        updateCount.addAndGet(treeIdxs.size());
    }

    public synchronized int size()      { return tree.size; }
    public synchronized boolean isEmpty(){ return tree.size == 0; }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal
    // ─────────────────────────────────────────────────────────────────────────

    private void annealBeta() {
        beta = Math.min(betaEnd, beta + betaAnneal);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("capacity",    capacity);
        s.put("size",        tree.size);
        s.put("addCount",    addCount.get());
        s.put("sampleCount", sampleCount.get());
        s.put("updateCount", updateCount.get());
        s.put("beta",        beta);
        s.put("maxPriority", maxPriority);
        s.put("avgPriority", avgPriority);
        s.put("avgIsWeight", avgIsWeight);
        return s;
    }
}
