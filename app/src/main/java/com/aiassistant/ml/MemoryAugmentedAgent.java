package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MemoryAugmentedAgent — external memory for long-horizon game tasks.
 *
 * Combines a recurrent-style agent with a differentiable key-value memory:
 *
 *   MEMORY WRITE:  m_t = Memory.write(key=φ(s_t), value=a_t)
 *   MEMORY READ:   v_t = Memory.read(key=φ(s_t))   via softmax attention
 *   AUGMENTED STATE: ŝ_t = concat(s_t, v_t)        fed to policy
 *
 * Memory types:
 *   NEAREST_NEIGHBOUR  — retrieve the value associated with the nearest stored key.
 *   ATTENTION          — softmax attention over all stored keys: v = Σ_i α_i · val_i
 *   EPISODIC           — DNC-style: remember the most surprising (high-novelty) states.
 *
 * Use cases:
 *   - Sparse reward games: remember path to rewarding states across long episodes.
 *   - Navigation: store visited positions and their rewards.
 *   - Multi-episode meta-learning: persist knowledge between episodes.
 *
 * Thread-safe.
 */
public class MemoryAugmentedAgent {

    private static final String TAG = "MemoryAugmentedAgent";

    public enum MemoryType { NEAREST_NEIGHBOUR, ATTENTION, EPISODIC }

    // ─────────────────────────────────────────────────────────────────────────
    // Memory entry
    // ─────────────────────────────────────────────────────────────────────────
    private static class MemEntry {
        float[] key;    // compressed state key
        float[] value;  // stored value (action encoding or state embedding)
        float   reward; // reward at write time
        float   usage;  // read count (for LRU eviction)
        long    timestamp;

        MemEntry(float[] k, float[] v, float r) {
            key = k.clone(); value = v.clone(); reward = r;
            timestamp = System.currentTimeMillis();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Key encoder: state → compressed key
    // ─────────────────────────────────────────────────────────────────────────
    private final float[][] keyW;  // [keyDim][stateDim]
    private final float[]   keyB;

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int    stateDim;
    private final int    keyDim;
    private final int    valueDim;
    private final int    capacity;
    private final MemoryType type;
    private final float  attentionBeta;   // softmax temperature for attention

    private final List<MemEntry> memory  = new ArrayList<>();
    private int writeHead = 0;

    private float avgReadSimilarity = 0f;
    private float avgMemUsage       = 0f;

    private final AtomicInteger writeCount = new AtomicInteger(0);
    private final AtomicInteger readCount  = new AtomicInteger(0);

    private final Random rng = new Random(163L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public MemoryAugmentedAgent(int stateDim, int keyDim, int valueDim,
                                 int capacity, MemoryType type, float attentionBeta) {
        this.stateDim      = stateDim;
        this.keyDim        = keyDim;
        this.valueDim      = valueDim;
        this.capacity      = capacity;
        this.type          = type;
        this.attentionBeta = attentionBeta;

        float s = (float) Math.sqrt(2.0 / (stateDim + keyDim));
        keyW = new float[keyDim][stateDim];
        keyB = new float[keyDim];
        for (int i = 0; i < keyDim; i++)
            for (int j = 0; j < stateDim; j++) keyW[i][j] = (rng.nextFloat()*2f-1f)*s;

        Log.i(TAG, "MemoryAugmentedAgent: cap=" + capacity + " key=" + keyDim
                + " val=" + valueDim + " type=" + type);
    }

    public MemoryAugmentedAgent(int stateDim, int capacity) {
        this(stateDim, Math.min(stateDim, 32), Math.min(stateDim, 32),
             capacity, MemoryType.ATTENTION, 1.0f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Write (state, value) to memory.
     * Evicts oldest entry when at capacity.
     */
    public synchronized void write(float[] state, float[] value, float reward) {
        float[] key = encode(state);
        MemEntry entry = new MemEntry(key, value, reward);

        if (memory.size() < capacity) {
            memory.add(entry);
        } else {
            // LRU-like: evict least-used or by write head
            int evict = lruIndex();
            memory.set(evict, entry);
        }
        writeHead = (writeHead + 1) % capacity;
        writeCount.incrementAndGet();
    }

    /**
     * Write action one-hot encoding as value.
     */
    public synchronized void writeAction(float[] state, int action, float reward) {
        float[] value = new float[valueDim];
        if (action >= 0 && action < valueDim) value[action] = 1f;
        write(state, value, reward);
    }

    /**
     * Read from memory using current state as query key.
     * @return Retrieved value vector [valueDim].
     */
    public synchronized float[] read(float[] state) {
        if (memory.isEmpty()) return new float[valueDim];
        float[] query = encode(state);

        switch (type) {
            case NEAREST_NEIGHBOUR: return readNN(query);
            case EPISODIC:          return readEpisodic(query, state);
            case ATTENTION:
            default:                return readAttention(query);
        }
    }

    /**
     * Augmented state = concat(state, read(state)) — feed this to the policy.
     */
    public synchronized float[] augmentedState(float[] state) {
        float[] mem = read(state);
        float[] out = new float[stateDim + valueDim];
        System.arraycopy(pad(state, stateDim), 0, out, 0, stateDim);
        System.arraycopy(mem, 0, out, stateDim, valueDim);
        return out;
    }

    public synchronized int augmentedDim() { return stateDim + valueDim; }

    /** Clear all memory entries (call at episode start for episodic memory). */
    public synchronized void clearEpisodic() { memory.clear(); writeHead = 0; }

    // ─────────────────────────────────────────────────────────────────────────
    // Read implementations
    // ─────────────────────────────────────────────────────────────────────────

    private float[] readNN(float[] query) {
        float bestSim = Float.NEGATIVE_INFINITY;
        MemEntry best = memory.get(0);
        for (MemEntry e : memory) {
            float sim = dot(query, e.key);
            if (sim > bestSim) { bestSim = sim; best = e; }
        }
        best.usage++;
        avgReadSimilarity = 0.99f * avgReadSimilarity + 0.01f * bestSim;
        readCount.incrementAndGet();
        return best.value.clone();
    }

    private float[] readAttention(float[] query) {
        // α_i = softmax(β · q·k_i)
        float[] scores = new float[memory.size()];
        float maxScore = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < memory.size(); i++) {
            scores[i] = attentionBeta * dot(query, memory.get(i).key);
            if (scores[i] > maxScore) maxScore = scores[i];
        }
        float sum = 0;
        for (int i = 0; i < scores.length; i++) { scores[i] = (float) Math.exp(scores[i]-maxScore); sum += scores[i]; }
        float[] out = new float[valueDim];
        for (int i = 0; i < memory.size(); i++) {
            float alpha = scores[i] / sum;
            float[] val = memory.get(i).value;
            for (int j = 0; j < Math.min(valueDim, val.length); j++) out[j] += alpha * val[j];
            memory.get(i).usage += alpha;
        }
        readCount.incrementAndGet();
        return out;
    }

    private float[] readEpisodic(float[] query, float[] state) {
        // Blend nearest-neighbour by reward: prefer high-reward memories
        float bestScore = Float.NEGATIVE_INFINITY;
        MemEntry best = memory.get(0);
        for (MemEntry e : memory) {
            float sim   = dot(query, e.key);
            float score = sim + 0.1f * e.reward;
            if (score > bestScore) { bestScore = score; best = e; }
        }
        best.usage++;
        readCount.incrementAndGet();
        return best.value.clone();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    private float[] encode(float[] state) {
        float[] s = pad(state, stateDim);
        float[] k = new float[keyDim];
        for (int i = 0; i < keyDim; i++) {
            k[i] = keyB[i];
            for (int j = 0; j < stateDim; j++) k[i] += keyW[i][j] * s[j];
            k[i] = (float) Math.tanh(k[i]);
        }
        return k;
    }

    private int lruIndex() {
        int idx = 0; float minUsage = memory.get(0).usage;
        for (int i = 1; i < memory.size(); i++)
            if (memory.get(i).usage < minUsage) { minUsage = memory.get(i).usage; idx = i; }
        return idx;
    }

    private static float dot(float[] a, float[] b) {
        float s = 0; for (int i = 0; i < Math.min(a.length, b.length); i++) s += a[i]*b[i];
        return s;
    }

    private static float[] pad(float[] x, int dim) {
        if (x.length == dim) return x;
        float[] p = new float[dim]; System.arraycopy(x, 0, p, 0, Math.min(x.length, dim));
        return p;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("memoryType",       type.name());
        s.put("memorySize",       memory.size());
        s.put("capacity",         capacity);
        s.put("writeCount",       writeCount.get());
        s.put("readCount",        readCount.get());
        s.put("avgReadSimilarity",avgReadSimilarity);
        return s;
    }
}
