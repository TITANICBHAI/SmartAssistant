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
 * SARSA(λ) agent — on-policy temporal-difference control.
 *
 * Key improvements over the original:
 *
 *  1. Bug-fix: the original recursively called update() inside the eligibility-
 *     trace loop, causing a StackOverflow and incorrect double-counting.
 *     This implementation applies TD updates inline over the trace map.
 *  2. Replacing traces (not accumulating) — when we revisit (s,a) the trace is
 *     reset to 1 rather than incremented, preventing runaway trace values.
 *  3. Separate next-action selection — nextAction is selected once before the
 *     trace update loop (on-policy correctness).
 *  4. Epsilon decay schedule — linear decay from 1.0 → 0.05 over DECAY_STEPS.
 *  5. LRU Q-table capped at MAX_TABLE_SIZE entries.
 *  6. Adaptive learning rate α(s,a) = α₀ / (1 + visits × 0.1).
 *  7. Optimistic initial Q-values to encourage exploration.
 *  8. Save / load persists all hyper-parameters and visit counts.
 */
public class SARSAAgent extends RLAgent {
    private static final String TAG = "SARSAAgent";

    // -----------------------------------------------------------------------
    // Hyper-parameters
    // -----------------------------------------------------------------------
    private static final float EPSILON_START  = 1.0f;
    private static final float EPSILON_MIN    = 0.05f;
    private static final int   DECAY_STEPS    = 5_000;
    private static final float OPTIMISTIC_Q   = 0.1f;
    private static final int   MAX_TABLE_SIZE = 50_000;
    private static final float TRACE_MIN      = 0.01f;

    // -----------------------------------------------------------------------
    // Q-table (LRU)
    // -----------------------------------------------------------------------
    private final LinkedHashMap<String, float[]> qTable;
    private final Map<String, int[]>             visits = new HashMap<>();
    private final Map<String, float[]>           traces = new HashMap<>();

    // -----------------------------------------------------------------------
    // Episode state
    // -----------------------------------------------------------------------
    private String  prevKey       = null;
    private int     prevAction    = -1;
    private boolean hasEpisodeStep = false;

    // -----------------------------------------------------------------------
    // Runtime
    // -----------------------------------------------------------------------
    private float lambda          = 0.9f;
    private boolean useLambda     = true;
    private int   binsPerDimension = 10;
    private int   totalSteps      = 0;
    private final Random rng;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------
    public SARSAAgent(int stateSize, int actionSize) {
        super(stateSize, actionSize);
        this.rng             = new Random(42);
        this.explorationRate = EPSILON_START;
        this.learningRate    = 0.1f;
        this.discountFactor  = 0.99f;

        qTable = new LinkedHashMap<String, float[]>(MAX_TABLE_SIZE + 1, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, float[]> e) {
                if (size() > MAX_TABLE_SIZE) { visits.remove(e.getKey()); return true; }
                return false;
            }
        };
        Log.i(TAG, "SARSAAgent(λ) created stateSize=" + stateSize + " actionSize=" + actionSize);
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

    /**
     * SARSA on-policy update — receives (s, a, r, s', done).
     * The on-policy next action a' is selected here before the trace update.
     */
    @Override
    public void update(float[] state, int action, float reward, float[] nextState, boolean done) {
        String sk  = key(state);
        String nsk = key(nextState);

        // First call in episode — no previous (s,a) yet
        if (!hasEpisodeStep) {
            prevKey        = sk;
            prevAction     = action;
            hasEpisodeStep = true;
            // Update trace for first step
            applyReplacingTrace(sk, action);
            return;
        }

        // Select on-policy next action a' (not used if done)
        int nextAction = done ? -1 : selectAction(nextState);

        // TD error: Q(s,a) - [r + γ·Q(s',a')]
        float[] qS   = qGet(sk);
        float   nextQ = (!done && nextAction >= 0) ? qGet(nsk)[nextAction] : 0f;
        float   tdErr = reward + discountFactor * nextQ - qS[action];
        float   alpha = adaptiveAlpha(sk, action);

        // Set replacing trace for current (s,a)
        applyReplacingTrace(sk, action);
        visits.computeIfAbsent(sk, x -> new int[actionSize])[action]++;

        // Update all active traces
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, float[]> entry : traces.entrySet()) {
            float[] qt  = qGet(entry.getKey());
            float[] et  = entry.getValue();
            boolean tiny = true;
            for (int a = 0; a < actionSize; a++) {
                qt[a] += alpha * tdErr * et[a];
                et[a] *= discountFactor * lambda;
                if (Math.abs(et[a]) >= TRACE_MIN) tiny = false;
            }
            if (tiny) toRemove.add(entry.getKey());
        }
        for (String k : toRemove) traces.remove(k);

        prevKey    = nsk;
        prevAction = nextAction;

        if (done) {
            traces.clear();
            hasEpisodeStep = false;
            prevKey        = null;
            prevAction     = -1;
        }
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
            oos.writeFloat(lambda);
            oos.writeBoolean(useLambda);
            oos.writeInt(binsPerDimension);
            oos.writeInt(totalSteps);
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
            visits.putAll((Map<String, int[]>)   ois.readObject());
            explorationRate  = ois.readFloat();
            learningRate     = ois.readFloat();
            discountFactor   = ois.readFloat();
            lambda           = ois.readFloat();
            useLambda        = ois.readBoolean();
            binsPerDimension = ois.readInt();
            totalSteps       = ois.readInt();
            traces.clear();
            hasEpisodeStep = false;
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

    /** Replacing trace: set e(s,a)=1 and zero all others in that state. */
    private void applyReplacingTrace(String k, int action) {
        float[] e = traces.computeIfAbsent(k, x -> new float[actionSize]);
        Arrays.fill(e, 0f);
        e[action] = 1f;
    }

    private int greedyAction(String k) {
        float[] q = qGet(k); int best = 0; float bv = q[0];
        for (int i = 1; i < q.length; i++) if (q[i] > bv) { bv = q[i]; best = i; }
        return best;
    }

    private float adaptiveAlpha(String k, int a) {
        int[] vc = visits.get(k);
        int   n  = vc != null ? vc[a] : 0;
        return learningRate / (1f + n * 0.1f);
    }

    private void decayEpsilon() {
        explorationRate = EPSILON_START +
                (EPSILON_MIN - EPSILON_START) * Math.min(1f, (float) totalSteps / DECAY_STEPS);
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

    // -----------------------------------------------------------------------
    // Configuration
    // -----------------------------------------------------------------------
    public void setLambda(float l)            { lambda = Math.max(0f, Math.min(1f, l)); }
    public void setUseLambda(boolean use)     { if (!(useLambda = use)) traces.clear(); }
    public void setBinsPerDimension(int bins) { binsPerDimension = Math.max(2, bins); }
    public int  getTableSize()                { return qTable.size(); }
    public int  getTotalSteps()               { return totalSteps; }
}
