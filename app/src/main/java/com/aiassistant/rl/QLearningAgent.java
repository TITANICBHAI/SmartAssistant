package com.aiassistant.rl;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Q-Learning agent — significantly improved over the original tabular stub:
 *
 *  1. Q(λ) — replacing-trace eligibility traces so credit is correctly propagated
 *     back through a trajectory (the original replay called update() recursively,
 *     causing an infinite loop; that bug is fixed here).
 *  2. LRU Q-table pruning — capped at MAX_TABLE_SIZE states; least-recently-used
 *     states are evicted automatically so memory stays bounded.
 *  3. Epsilon-decay schedule — exploration decays linearly from 1.0 → 0.05 over
 *     DECAY_STEPS total environment steps.
 *  4. Adaptive learning rate — α(s,a) = α₀ / (1 + visits(s,a) × 0.1) gives
 *     larger updates to rarely-visited state-action pairs.
 *  5. Optimistic initialisation — new Q-values start at +0.1 to encourage
 *     exploration of unseen regions without any hardcoded bias.
 *  6. Safe experience replay — mini-batch is applied without a recursive call;
 *     eligibility traces are NOT polluted by replay updates.
 *  7. Better state hashing — integer arithmetic avoids floating-point edge cases.
 *  8. Save / load persists visit counts, epsilon, λ, and bins.
 */
public class QLearningAgent extends RLAgent {
    private static final String TAG = "QLearningAgent";

    // -----------------------------------------------------------------------
    // Hyper-parameters
    // -----------------------------------------------------------------------
    private static final float EPSILON_START  = 1.0f;
    private static final float EPSILON_MIN    = 0.05f;
    private static final int   DECAY_STEPS    = 5_000;
    private static final float OPTIMISTIC_Q   = 0.1f;
    private static final int   MAX_TABLE_SIZE = 50_000;
    private static final int   MAX_MEMORY     = 2_000;
    private static final int   BATCH_SIZE     = 32;
    private static final float LAMBDA         = 0.9f;
    private static final float TRACE_MIN      = 0.01f;

    // -----------------------------------------------------------------------
    // LRU Q-table
    // -----------------------------------------------------------------------
    private final LinkedHashMap<String, float[]> qTable;

    // Per-state-action visit counts for adaptive α
    private final Map<String, int[]> visits = new HashMap<>();

    // Eligibility traces for Q(λ)
    private final Map<String, float[]> traces = new HashMap<>();

    // -----------------------------------------------------------------------
    // Replay buffer
    // -----------------------------------------------------------------------
    private static class Exp {
        final float[] s, ns;
        final int a;
        final float r;
        final boolean done;
        Exp(float[] s, int a, float r, float[] ns, boolean done) {
            this.s = s.clone(); this.a = a; this.r = r;
            this.ns = ns.clone(); this.done = done;
        }
    }
    private final List<Exp> memory = new ArrayList<>();

    // -----------------------------------------------------------------------
    // Runtime state
    // -----------------------------------------------------------------------
    private int   totalSteps       = 0;
    private int   binsPerDimension = 10;
    private final Random rng;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------
    public QLearningAgent(int stateSize, int actionSize) {
        super(stateSize, actionSize);
        this.rng             = new Random(42);
        this.explorationRate = EPSILON_START;
        this.learningRate    = 0.1f;
        this.discountFactor  = 0.99f;

        // Access-ordered LinkedHashMap = LRU eviction when size > MAX_TABLE_SIZE
        qTable = new LinkedHashMap<String, float[]>(MAX_TABLE_SIZE + 1, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, float[]> e) {
                if (size() > MAX_TABLE_SIZE) { visits.remove(e.getKey()); return true; }
                return false;
            }
        };
        Log.i(TAG, "QLearning(λ) created stateSize=" + stateSize + " actionSize=" + actionSize);
    }

    // -----------------------------------------------------------------------
    // RLAgent interface
    // -----------------------------------------------------------------------

    @Override
    public int selectAction(float[] state) {
        totalSteps++;
        decayEpsilon();
        if (rng.nextFloat() < explorationRate) return rng.nextInt(actionSize);
        return greedyAction(key(state));
    }

    @Override
    public void update(float[] state, int action, float reward, float[] nextState, boolean done) {
        // --- Store in replay buffer (non-recursive) ---
        if (memory.size() >= MAX_MEMORY) memory.remove(0);
        memory.add(new Exp(state, action, reward, nextState, done));

        // --- Online Q(λ) update ---
        String sk  = key(state);
        String nsk = key(nextState);
        float[] qS = qGet(sk);
        float[] qN = qGet(nsk);

        float maxN    = done ? 0f : max(qN);
        float tdError = reward + discountFactor * maxN - qS[action];
        float alpha   = adaptiveAlpha(sk, action);

        // Replacing traces: set current (s,a) to 1, zero all others in s
        float[] e = traceGet(sk);
        Arrays.fill(e, 0f);
        e[action] = 1f;

        // Update all active traces
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, float[]> entry : traces.entrySet()) {
            float[] qt = qGet(entry.getKey());
            float[] et = entry.getValue();
            boolean tiny = true;
            for (int a = 0; a < actionSize; a++) {
                qt[a] += alpha * tdError * et[a];
                et[a] *= discountFactor * LAMBDA;
                if (Math.abs(et[a]) >= TRACE_MIN) tiny = false;
            }
            if (tiny) toRemove.add(entry.getKey());
        }
        for (String k : toRemove) traces.remove(k);

        // Increment visit counter
        visits.computeIfAbsent(sk, x -> new int[actionSize])[action]++;

        if (done) traces.clear();

        // --- Mini-batch replay every 8 steps (no recursion) ---
        if (totalSteps % 8 == 0 && memory.size() >= BATCH_SIZE) replayBatch();
    }

    @Override
    public int[] getTopActions(float[] state, int n) {
        float[]   q   = qGet(key(state));
        Integer[] idx = new Integer[actionSize];
        for (int i = 0; i < actionSize; i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> Float.compare(q[b], q[a]));
        n = Math.min(n, actionSize);
        int[] top = new int[n];
        for (int i = 0; i < n; i++) top[i] = idx[i];
        return top;
    }

    @Override
    public float[] getActionProbabilities(float[] state, int[] actions) {
        float[] q     = qGet(key(state));
        float[] probs = new float[actions.length];
        float   maxQ  = Float.NEGATIVE_INFINITY;
        for (int a : actions) if (q[a] > maxQ) maxQ = q[a];
        float sum = 0f;
        for (int i = 0; i < actions.length; i++) {
            probs[i] = (float) Math.exp(q[actions[i]] - maxQ);
            sum      += probs[i];
        }
        if (sum > 0) for (int i = 0; i < probs.length; i++) probs[i] /= sum;
        else         Arrays.fill(probs, 1f / actions.length);
        return probs;
    }

    @Override
    public boolean saveModel(String filePath) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(new HashMap<>(qTable));
            oos.writeObject(visits);
            oos.writeFloat(explorationRate);
            oos.writeFloat(learningRate);
            oos.writeFloat(discountFactor);
            oos.writeInt(totalSteps);
            oos.writeInt(binsPerDimension);
            Log.d(TAG, "Saved " + qTable.size() + " states to " + filePath);
            return true;
        } catch (Exception e) { Log.e(TAG, "Save error", e); return false; }
    }

    @Override @SuppressWarnings("unchecked")
    public boolean loadModel(String filePath) {
        File f = new File(filePath);
        if (!f.exists()) { Log.e(TAG, "Not found: " + filePath); return false; }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            qTable.putAll((Map<String, float[]>) ois.readObject());
            visits.putAll((Map<String, int[]>) ois.readObject());
            explorationRate  = ois.readFloat();
            learningRate     = ois.readFloat();
            discountFactor   = ois.readFloat();
            totalSteps       = ois.readInt();
            binsPerDimension = ois.readInt();
            traces.clear();
            Log.d(TAG, "Loaded " + qTable.size() + " states from " + filePath);
            return true;
        } catch (Exception e) { Log.e(TAG, "Load error", e); return false; }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private float[] qGet(String k) {
        float[] q = qTable.get(k);
        if (q == null) {
            q = new float[actionSize];
            Arrays.fill(q, OPTIMISTIC_Q);
            qTable.put(k, q);
        }
        return q;
    }

    private float[] traceGet(String k) {
        return traces.computeIfAbsent(k, x -> new float[actionSize]);
    }

    private int greedyAction(String k) {
        float[] q = qGet(k); int best = 0; float bv = q[0];
        for (int i = 1; i < q.length; i++) if (q[i] > bv) { bv = q[i]; best = i; }
        return best;
    }

    /** Adaptive α: larger for rarely-visited state-action pairs. */
    private float adaptiveAlpha(String k, int a) {
        int[] vc = visits.get(k);
        int   n  = vc != null ? vc[a] : 0;
        return learningRate / (1f + n * 0.1f);
    }

    private void decayEpsilon() {
        explorationRate = EPSILON_START +
                (EPSILON_MIN - EPSILON_START) * Math.min(1f, (float) totalSteps / DECAY_STEPS);
    }

    /** Non-recursive mini-batch replay — no eligibility trace pollution. */
    private void replayBatch() {
        int n = Math.min(BATCH_SIZE, memory.size());
        for (int i = 0; i < n; i++) {
            Exp exp = memory.get(rng.nextInt(memory.size()));
            String sk   = key(exp.s);
            String nsk  = key(exp.ns);
            float[] qS  = qGet(sk);
            float maxN  = exp.done ? 0f : max(qGet(nsk));
            float target = exp.r + discountFactor * maxN;
            float alpha  = adaptiveAlpha(sk, exp.a);
            qS[exp.a]   += alpha * (target - qS[exp.a]);
            visits.computeIfAbsent(sk, x -> new int[actionSize])[exp.a]++;
        }
    }

    private String key(float[] state) {
        if (state == null || state.length == 0) return "empty";
        StringBuilder sb = new StringBuilder(state.length * 3);
        for (int i = 0; i < state.length; i++) {
            int bin = (int) (Math.max(0f, Math.min(1f, state[i])) * binsPerDimension);
            if (bin >= binsPerDimension) bin = binsPerDimension - 1;
            if (i > 0) sb.append('_');
            sb.append(bin);
        }
        return sb.toString();
    }

    private float max(float[] v) {
        float m = v[0]; for (float x : v) if (x > m) m = x; return m;
    }

    // -----------------------------------------------------------------------
    // Configuration
    // -----------------------------------------------------------------------
    public void setBinsPerDimension(int bins) { binsPerDimension = Math.max(2, bins); }
    public void setMaxMemorySize(int size)    {
        while (memory.size() > size) memory.remove(0);
    }
    public int getTableSize()   { return qTable.size(); }
    public int getTotalSteps()  { return totalSteps; }
}
