package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ModelEnsemble — combines action-value predictions from multiple RL models using
 * confidence-weighted voting, then rebalances weights online based on observed outcomes.
 *
 * Design:
 *   - Any number of named "model sources" can be registered.  Each source provides
 *     a Q-value vector float[] for the current state.
 *   - Ensemble output = weighted sum of Q-vectors, normalised so weights sum to 1.
 *   - Online rebalancing: when an outcome is fed back, the source whose greedy action
 *     matched the eventually-rewarding action gets its weight increased; others decay.
 *   - A minimum weight floor (MIN_WEIGHT = 0.05) prevents any model from being
 *     permanently silenced.
 *   - Optional temperature-scaled softmax voting mode as an alternative to linear fusion.
 *   - Thread-safe via synchronized on the weight map.
 */
public class ModelEnsemble {

    private static final String TAG = "ModelEnsemble";

    private static final float MIN_WEIGHT  = 0.05f;
    private static final float WEIGHT_BUMP = 0.05f;  // reward bump for correct prediction
    private static final float DECAY       = 0.98f;  // per-outcome multiplicative decay for all

    // -------------------------------------------------------------------------
    // Inner types
    // -------------------------------------------------------------------------

    public interface ModelSource {
        /** Return Q-value estimates for each action given the current state. */
        float[] getQValues(float[] state);
        String  getName();
    }

    private static class SourceEntry {
        final ModelSource source;
        float weight;
        float totalCorrect;
        float totalPredictions;
        int   lastGreedyAction;

        SourceEntry(ModelSource src, float initialWeight) {
            this.source        = src;
            this.weight        = initialWeight;
            this.lastGreedyAction = -1;
        }

        float accuracy() {
            return totalPredictions > 0 ? totalCorrect / totalPredictions : 0.5f;
        }
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final List<SourceEntry>  sources   = new CopyOnWriteArrayList<>();
    private final int                actionDim;
    private final boolean            softmaxMode;   // true = softmax voting, false = linear
    private final float              temperature;   // for softmax mode

    private final AtomicInteger      callCount     = new AtomicInteger(0);
    private final AtomicInteger      outcomeCount  = new AtomicInteger(0);

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * @param actionDim    Number of discrete actions.
     * @param softmaxMode  If true, softmax-fuse Q-vectors rather than linear blend.
     * @param temperature  Softmax temperature (lower = more peaky).
     */
    public ModelEnsemble(int actionDim, boolean softmaxMode, float temperature) {
        this.actionDim   = actionDim;
        this.softmaxMode = softmaxMode;
        this.temperature = Math.max(0.01f, temperature);
    }

    public ModelEnsemble(int actionDim) {
        this(actionDim, false, 1.0f);
    }

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    /** Add a model source with an explicit initial weight. */
    public synchronized void addSource(ModelSource source, float initialWeight) {
        // Normalize so weights still sum to 1 after addition
        float totalBefore = 0;
        for (SourceEntry e : sources) totalBefore += e.weight;
        float newWeight = Math.max(MIN_WEIGHT, initialWeight);
        float scale = totalBefore > 0 ? (1f - newWeight) / totalBefore : 1f;
        for (SourceEntry e : sources) e.weight = Math.max(MIN_WEIGHT, e.weight * scale);
        sources.add(new SourceEntry(source, newWeight));
        Log.i(TAG, "Added source: " + source.getName() + " weight=" + newWeight
                + " total_sources=" + sources.size());
    }

    /** Add a model source with equal initial weight split. */
    public synchronized void addSource(ModelSource source) {
        float equalWeight = sources.isEmpty() ? 1.0f : 1.0f / (sources.size() + 1);
        addSource(source, equalWeight);
    }

    /** Remove a source by name. Remaining weights are renormalized. */
    public synchronized void removeSource(String name) {
        sources.removeIf(e -> e.source.getName().equals(name));
        renormalize();
    }

    // -------------------------------------------------------------------------
    // Ensemble prediction
    // -------------------------------------------------------------------------

    /**
     * Produce an ensemble Q-value estimate by fusing all source predictions.
     *
     * @param state Current state feature vector.
     * @return Fused Q-values, length = actionDim.
     */
    public synchronized float[] getEnsembleQValues(float[] state) {
        if (sources.isEmpty()) return new float[actionDim];
        callCount.incrementAndGet();

        float[] ensemble = new float[actionDim];

        if (softmaxMode) {
            // Collect per-source Q-vectors and their weights
            List<float[]> qVecs   = new ArrayList<>();
            List<Float>   weights = new ArrayList<>();
            for (SourceEntry e : sources) {
                float[] q = safeGetQValues(e, state);
                qVecs.add(q);
                weights.add(e.weight);
            }
            // Softmax over weights
            float[] softWeights = softmax(toArray(weights), temperature);
            for (int i = 0; i < qVecs.size(); i++) {
                float w = softWeights[i];
                float[] q = qVecs.get(i);
                for (int a = 0; a < actionDim; a++) {
                    ensemble[a] += w * (a < q.length ? q[a] : 0f);
                }
                // Remember greedy action for outcome feedback
                sources.get(i).lastGreedyAction = argmax(q);
            }
        } else {
            // Linear weighted sum — normalize weights first
            float totalW = 0;
            for (SourceEntry e : sources) totalW += e.weight;
            if (totalW <= 0) totalW = 1;
            for (SourceEntry e : sources) {
                float[] q = safeGetQValues(e, state);
                float   w = e.weight / totalW;
                for (int a = 0; a < actionDim; a++) {
                    ensemble[a] += w * (a < q.length ? q[a] : 0f);
                }
                e.lastGreedyAction = argmax(q);
            }
        }

        return ensemble;
    }

    /**
     * Convenience: return the greedy action from the ensemble.
     */
    public synchronized int selectAction(float[] state) {
        return argmax(getEnsembleQValues(state));
    }

    // -------------------------------------------------------------------------
    // Online weight rebalancing
    // -------------------------------------------------------------------------

    /**
     * Feed back the outcome of the last step to rebalance source weights.
     *
     * @param rewardingAction The action that turned out to be correct / well-rewarded.
     * @param reward          Observed reward (positive = correct, negative = incorrect).
     */
    public synchronized void recordOutcome(int rewardingAction, float reward) {
        outcomeCount.incrementAndGet();
        boolean positive = reward > 0;

        for (SourceEntry e : sources) {
            e.totalPredictions++;
            boolean correct = (e.lastGreedyAction == rewardingAction);
            if (correct) e.totalCorrect++;

            // Decay all weights, then bump the correct one
            e.weight = Math.max(MIN_WEIGHT, e.weight * DECAY);
            if (correct && positive) {
                e.weight = Math.min(1.0f, e.weight + WEIGHT_BUMP);
            }
        }
        renormalize();
    }

    // -------------------------------------------------------------------------
    // Monitoring
    // -------------------------------------------------------------------------

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("sourceCount",  sources.size());
        s.put("callCount",    callCount.get());
        s.put("outcomeCount", outcomeCount.get());
        s.put("softmaxMode",  softmaxMode);

        List<Map<String, Object>> sourceStats = new ArrayList<>();
        for (SourceEntry e : sources) {
            Map<String, Object> ss = new HashMap<>();
            ss.put("name",     e.source.getName());
            ss.put("weight",   e.weight);
            ss.put("accuracy", e.accuracy());
            ss.put("predictions", (int) e.totalPredictions);
            sourceStats.add(ss);
        }
        s.put("sources", sourceStats);
        return s;
    }

    public synchronized List<String> getSourceNames() {
        List<String> names = new ArrayList<>();
        for (SourceEntry e : sources) names.add(e.source.getName());
        return names;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private float[] safeGetQValues(SourceEntry e, float[] state) {
        try {
            float[] q = e.source.getQValues(state);
            return q != null ? q : new float[actionDim];
        } catch (Exception ex) {
            Log.w(TAG, "Source " + e.source.getName() + " threw: " + ex.getMessage());
            return new float[actionDim];
        }
    }

    private void renormalize() {
        float total = 0;
        for (SourceEntry e : sources) total += e.weight;
        if (total <= 0) {
            float eq = 1f / Math.max(1, sources.size());
            for (SourceEntry e : sources) e.weight = eq;
        } else {
            for (SourceEntry e : sources) e.weight /= total;
        }
    }

    private static int argmax(float[] v) {
        int best = 0;
        for (int i = 1; i < v.length; i++) if (v[i] > v[best]) best = i;
        return best;
    }

    private static float[] softmax(float[] v, float temp) {
        float max = v[0];
        for (float x : v) if (x > max) max = x;
        float[] out = new float[v.length];
        float   sum = 0;
        for (int i = 0; i < v.length; i++) { out[i] = (float) Math.exp((v[i] - max) / temp); sum += out[i]; }
        for (int i = 0; i < v.length; i++) out[i] /= sum;
        return out;
    }

    private static float[] toArray(List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }
}
