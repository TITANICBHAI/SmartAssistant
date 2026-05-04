package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ExperienceReplayOptimizer — manages and optimises the experience replay buffer.
 *
 * Combines five sampling strategies selectable at runtime:
 *
 *   UNIFORM                  — standard random sampling (iid baseline)
 *   PRIORITIZED_TD           — PER (Schaul 2015): P(i) ∝ |δ_i|^α
 *   STRATIFIED_RETURN        — stratify by episode return quartile (curriculum)
 *   MAX_DIVERSITY            — greedy max-coverage: prefer states far from already
 *                              sampled set in feature space (exploration diversity)
 *   RECENCY_WEIGHTED         — exponentially-decaying weight: recent > old
 *
 * Additional features:
 *   - IS (importance-sampling) weight correction for PER to debias gradients.
 *   - Automatic priority refresh: stale priorities decay every DECAY_STEPS calls.
 *   - Buffer analytics: returns mean/std TD error, age histogram, return distribution.
 *   - Thread-safe.
 */
public class ExperienceReplayOptimizer {

    private static final String TAG = "ExpReplayOpt";

    public enum Strategy {
        UNIFORM, PRIORITIZED_TD, STRATIFIED_RETURN, MAX_DIVERSITY, RECENCY_WEIGHTED
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Experience record
    // ─────────────────────────────────────────────────────────────────────────
    public static class Experience {
        public final float[] state;
        public final int     action;
        public final float   reward;
        public final float[] nextState;
        public final boolean done;
        public float         priority;
        public float         tdError;
        public float         episodeReturn;
        public final long    timestamp;
        public float         isWeight;   // importance sampling weight (PER)
        int                  bufferIdx;

        public Experience(float[] state, int action, float reward,
                          float[] nextState, boolean done) {
            this.state         = state.clone();
            this.action        = action;
            this.reward        = reward;
            this.nextState     = nextState.clone();
            this.done          = done;
            this.priority      = 1.0f;
            this.tdError       = 1.0f;
            this.timestamp     = System.currentTimeMillis();
            this.isWeight      = 1.0f;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final List<Experience> buffer      = new ArrayList<>();
    private final int              capacity;
    private       int              head        = 0;  // circular write pointer

    private Strategy   strategy;
    private float      perAlpha    = 0.6f;   // priority exponent
    private float      perBeta     = 0.4f;   // IS correction exponent
    private final float perBetaEnd = 1.0f;
    private final int  perBetaSteps= 50_000;
    private int        addCount    = 0;

    private static final int    DECAY_STEPS  = 5_000;
    private static final float  PRIORITY_EPS = 1e-6f;

    private final AtomicInteger totalAdded   = new AtomicInteger(0);
    private final AtomicInteger totalSampled = new AtomicInteger(0);

    private final Random rng = new Random(55L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public ExperienceReplayOptimizer(int capacity, Strategy strategy) {
        this.capacity = capacity;
        this.strategy = strategy;
    }

    public ExperienceReplayOptimizer(int capacity) {
        this(capacity, Strategy.PRIORITIZED_TD);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Adding experiences
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized void add(float[] state, int action, float reward,
                                  float[] nextState, boolean done) {
        Experience e = new Experience(state, action, reward, nextState, done);
        e.priority = maxPriority();
        e.bufferIdx = totalAdded.get();

        if (buffer.size() < capacity) {
            buffer.add(e);
        } else {
            buffer.set(head % capacity, e);
        }
        head++;
        addCount++;
        totalAdded.incrementAndGet();

        // Periodic priority decay for stale entries
        if (addCount % DECAY_STEPS == 0) decayPriorities(0.99f);

        // Anneal beta toward 1
        perBeta = Math.min(perBetaEnd, PER_BETA_START + addCount * (perBetaEnd - PER_BETA_START) / perBetaSteps);
    }

    private static final float PER_BETA_START = 0.4f;

    // ─────────────────────────────────────────────────────────────────────────
    // Sampling
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized List<Experience> sample(int batchSize) {
        if (buffer.isEmpty()) return new ArrayList<>();
        batchSize = Math.min(batchSize, buffer.size());
        totalSampled.addAndGet(batchSize);

        switch (strategy) {
            case PRIORITIZED_TD:      return samplePER(batchSize);
            case STRATIFIED_RETURN:   return sampleStratified(batchSize);
            case MAX_DIVERSITY:       return sampleMaxDiversity(batchSize);
            case RECENCY_WEIGHTED:    return sampleRecency(batchSize);
            case UNIFORM:
            default:                  return sampleUniform(batchSize);
        }
    }

    /** Update TD errors and priorities after a training step. */
    public synchronized void updatePriorities(List<Experience> batch, float[] tdErrors) {
        for (int i = 0; i < Math.min(batch.size(), tdErrors.length); i++) {
            Experience e  = batch.get(i);
            int idx = buffer.indexOf(e);
            if (idx < 0) continue;
            e.tdError  = Math.abs(tdErrors[i]);
            e.priority = (float) Math.pow(e.tdError + PRIORITY_EPS, perAlpha);
            buffer.set(idx, e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Strategy implementations
    // ─────────────────────────────────────────────────────────────────────────

    private List<Experience> sampleUniform(int n) {
        List<Experience> result = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            result.add(buffer.get(rng.nextInt(buffer.size())));
        }
        return result;
    }

    private List<Experience> samplePER(int n) {
        double total = 0;
        for (Experience e : buffer) total += e.priority;
        double maxW = Double.NEGATIVE_INFINITY;
        for (Experience e : buffer)
            maxW = Math.max(maxW, Math.pow(buffer.size() * e.priority / total, -perBeta));

        List<Experience> result = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            double r   = rng.nextDouble() * total;
            double cum = 0;
            Experience chosen = buffer.get(buffer.size() - 1);
            for (Experience e : buffer) {
                cum += e.priority;
                if (r <= cum) { chosen = e; break; }
            }
            // IS weight
            double w = Math.pow(buffer.size() * chosen.priority / total, -perBeta);
            chosen.isWeight = (float)(w / maxW);
            result.add(chosen);
        }
        return result;
    }

    private List<Experience> sampleStratified(int n) {
        // Sort by episodeReturn, split into 4 quartiles, sample equally from each
        List<Experience> sorted = new ArrayList<>(buffer);
        sorted.sort((a, b) -> Float.compare(a.episodeReturn, b.episodeReturn));
        List<Experience> result = new ArrayList<>(n);
        int qSize = Math.max(1, sorted.size() / 4);
        for (int q = 0; q < 4 && result.size() < n; q++) {
            int start = q * qSize;
            int end   = Math.min(start + qSize, sorted.size());
            int take  = Math.max(1, n / 4);
            for (int i = 0; i < take && result.size() < n; i++) {
                result.add(sorted.get(start + rng.nextInt(end - start)));
            }
        }
        return result;
    }

    private List<Experience> sampleMaxDiversity(int n) {
        // Greedy: pick first randomly, then pick samples maximally far from chosen set
        List<Experience> pool   = new ArrayList<>(buffer);
        List<Experience> result = new ArrayList<>(n);
        result.add(pool.remove(rng.nextInt(pool.size())));
        while (result.size() < n && !pool.isEmpty()) {
            float maxMinDist = -1f;
            Experience best  = pool.get(0);
            for (Experience cand : pool) {
                float minDist = Float.MAX_VALUE;
                for (Experience chosen : result) {
                    float d = l2sq(cand.state, chosen.state);
                    if (d < minDist) minDist = d;
                }
                if (minDist > maxMinDist) { maxMinDist = minDist; best = cand; }
            }
            result.add(best);
            pool.remove(best);
        }
        return result;
    }

    private List<Experience> sampleRecency(int n) {
        // Weight = exp(-λ·age_rank), rank 0 = newest
        int sz = buffer.size();
        float lambda = 3f / sz;
        double total = 0;
        double[] weights = new double[sz];
        for (int i = 0; i < sz; i++) {
            int rank = sz - 1 - i; // i=sz-1 is newest
            weights[i] = Math.exp(-lambda * rank);
            total += weights[i];
        }
        List<Experience> result = new ArrayList<>(n);
        for (int k = 0; k < n; k++) {
            double r = rng.nextDouble() * total, cum = 0;
            for (int i = 0; i < sz; i++) {
                cum += weights[i];
                if (r <= cum) { result.add(buffer.get(i)); break; }
            }
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats & utilities
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized void setStrategy(Strategy s)  { this.strategy = s; }
    public synchronized int   size()                  { return buffer.size(); }
    public synchronized boolean isFull()              { return buffer.size() >= capacity; }

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("size",        buffer.size());
        s.put("capacity",    capacity);
        s.put("strategy",    strategy.name());
        s.put("totalAdded",  totalAdded.get());
        s.put("totalSampled",totalSampled.get());
        s.put("perBeta",     perBeta);
        if (!buffer.isEmpty()) {
            float sumP = 0, maxP = 0, sumTD = 0;
            for (Experience e : buffer) {
                sumP  += e.priority;
                sumTD += e.tdError;
                if (e.priority > maxP) maxP = e.priority;
            }
            s.put("avgPriority", sumP / buffer.size());
            s.put("maxPriority", maxP);
            s.put("avgTdError",  sumTD / buffer.size());
        }
        return s;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private float maxPriority() {
        float max = 1.0f;
        for (Experience e : buffer) if (e.priority > max) max = e.priority;
        return max;
    }

    private void decayPriorities(float factor) {
        for (Experience e : buffer) e.priority *= factor;
    }

    private static float l2sq(float[] a, float[] b) {
        float s = 0f;
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++) { float d = a[i] - b[i]; s += d * d; }
        return s;
    }
}
