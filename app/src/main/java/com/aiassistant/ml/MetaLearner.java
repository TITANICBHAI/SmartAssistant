package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MetaLearner — MAML-inspired fast adaptation for RL on new game contexts.
 *
 * Implements a simplified Model-Agnostic Meta-Learning (MAML) approach:
 *   - Maintains a set of meta-parameters θ (shared initialisation)
 *   - For each task (game context), computes adapted parameters θ' in K gradient steps
 *   - Meta-update: θ ← θ − β·∇θ Σ_tasks L(θ')
 *
 * On-device simplification:
 *   - Single linear layer policy (too heavy to do full 3-layer MAML on-device)
 *   - Inner loop: K=5 steps of policy gradient on task-specific transitions
 *   - Outer loop: meta-gradient = average of inner-adapted gradients
 *   - Task buffer stores K recent tasks for meta-update
 *
 * Benefits:
 *   - Rapidly adapts to new game genres/modes with only a few real transitions
 *   - Shared initialisation transfers knowledge across game contexts
 *   - Outperforms training from scratch on every new context
 *
 * Thread-safe.
 */
public class MetaLearner {

    private static final String TAG = "MetaLearner";

    // ─────────────────────────────────────────────────────────────────────────
    // Task record: stores K transitions from one game context
    // ─────────────────────────────────────────────────────────────────────────
    public static class Task {
        public final String  taskId;
        public final List<float[]>  states      = new ArrayList<>();
        public final List<Integer>  actions     = new ArrayList<>();
        public final List<Float>    advantages  = new ArrayList<>();

        public Task(String taskId) { this.taskId = taskId; }

        public void addTransition(float[] state, int action, float advantage) {
            states.add(state.clone());
            actions.add(action);
            advantages.add(advantage);
        }
        public int size() { return states.size(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int stateDim;
    private final int actionDim;
    private final int innerSteps;      // K
    private final float innerLr;       // α (inner loop LR)
    private final float metaLr;        // β (outer loop LR)
    private final int taskBufferSize;

    // Meta-parameters: linear policy W[actionDim][stateDim]
    private final float[][] metaW;
    private final float[]   metaB;

    // Adam state for meta-update
    private final float[][] mW, vW;   // first/second moments
    private final float[]   mB, vB;
    private int metaT = 0;
    private static final float ADAM_BETA1 = 0.9f, ADAM_BETA2 = 0.999f, ADAM_EPS = 1e-8f;

    // Task buffer
    private final List<Task> taskBuffer = new ArrayList<>();

    // Stats
    private final AtomicInteger metaUpdates = new AtomicInteger(0);
    private final AtomicInteger innerUpdates= new AtomicInteger(0);
    private float avgMetaLoss  = 0f;
    private float avgInnerLoss = 0f;

    private final Random rng = new Random(97L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public MetaLearner(int stateDim, int actionDim, int innerSteps,
                        float innerLr, float metaLr, int taskBufferSize) {
        this.stateDim      = stateDim;
        this.actionDim     = actionDim;
        this.innerSteps    = innerSteps;
        this.innerLr       = innerLr;
        this.metaLr        = metaLr;
        this.taskBufferSize= taskBufferSize;

        float s = (float) Math.sqrt(2.0 / (stateDim + actionDim));
        metaW = new float[actionDim][stateDim];
        metaB = new float[actionDim];
        mW    = new float[actionDim][stateDim];
        vW    = new float[actionDim][stateDim];
        mB    = new float[actionDim];
        vB    = new float[actionDim];
        for (int a = 0; a < actionDim; a++)
            for (int i = 0; i < stateDim; i++)
                metaW[a][i] = (rng.nextFloat() * 2f - 1f) * s;

        Log.i(TAG, "MetaLearner: stateDim=" + stateDim + " actionDim=" + actionDim
                + " K=" + innerSteps + " α=" + innerLr + " β=" + metaLr);
    }

    public MetaLearner(int stateDim, int actionDim) {
        this(stateDim, actionDim, 5, 0.01f, 3e-4f, 8);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Adapt meta-parameters to a new task with K inner gradient steps.
     * @return Adapted weight matrix θ' (does NOT modify meta-params).
     */
    public synchronized float[][] adapt(Task task) {
        float[][] W = copyW(metaW);
        float[]   b = metaB.clone();

        for (int step = 0; step < innerSteps && task.size() > 0; step++) {
            float[][] dW = new float[actionDim][stateDim];
            float[]   db = new float[actionDim];
            float loss = 0f;

            for (int t = 0; t < task.size(); t++) {
                float[] state = task.states.get(t);
                int     act   = task.actions.get(t);
                float   adv   = task.advantages.get(t);

                float[] logits = forward(W, b, state);
                float[] probs  = softmax(logits);
                float   logP   = (float) Math.log(Math.max(probs[act], 1e-8f));
                loss -= logP * adv;

                // Policy gradient gradient
                for (int a = 0; a < actionDim; a++) {
                    float g = (probs[a] - (a == act ? 1f : 0f)) * adv;
                    db[a]  += g;
                    for (int i = 0; i < Math.min(state.length, stateDim); i++)
                        dW[a][i] += g * state[i];
                }
            }

            // Normalise by batch size
            int n = task.size();
            // SGD inner step: θ' ← θ − α·∇θ L
            for (int a = 0; a < actionDim; a++) {
                b[a] -= innerLr * db[a] / n;
                for (int i = 0; i < stateDim; i++)
                    W[a][i] -= innerLr * dW[a][i] / n;
            }
            avgInnerLoss = 0.95f * avgInnerLoss + 0.05f * (loss / n);
            innerUpdates.incrementAndGet();
        }
        return W;
    }

    /**
     * Register a completed task and trigger a meta-update when buffer is full.
     */
    public synchronized void registerTask(Task task) {
        if (task.size() < 2) return;
        if (taskBuffer.size() >= taskBufferSize) taskBuffer.remove(0);
        taskBuffer.add(task);
        if (taskBuffer.size() >= taskBufferSize) metaUpdate();
    }

    /**
     * Select action using adapted weights for a given context.
     */
    public synchronized int selectAction(float[][] adaptedW, float[] state) {
        float[] logits = forward(adaptedW, metaB, state);
        float[] probs  = softmax(logits);
        // Sample from distribution
        float r = rng.nextFloat(), cum = 0f;
        for (int a = 0; a < actionDim - 1; a++) {
            cum += probs[a]; if (r < cum) return a;
        }
        return actionDim - 1;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Meta-update (MAML outer loop)
    // ─────────────────────────────────────────────────────────────────────────

    private void metaUpdate() {
        float[][] sumDW = new float[actionDim][stateDim];
        float[]   sumDb = new float[actionDim];
        float totalLoss = 0f;

        for (Task task : taskBuffer) {
            // Compute adapted weights
            float[][] thetaPrime = adapt(task);

            // Compute meta-gradient: ∇θ L(θ') evaluated at θ' on the same task
            for (int t = 0; t < task.size(); t++) {
                float[] state = task.states.get(t);
                int     act   = task.actions.get(t);
                float   adv   = task.advantages.get(t);

                float[] logits = forward(thetaPrime, metaB, state);
                float[] probs  = softmax(logits);
                totalLoss -= (float) Math.log(Math.max(probs[act], 1e-8f)) * adv;

                for (int a = 0; a < actionDim; a++) {
                    float g = (probs[a] - (a == act ? 1f : 0f)) * adv;
                    sumDb[a] += g;
                    for (int i = 0; i < Math.min(state.length, stateDim); i++)
                        sumDW[a][i] += g * state[i];
                }
            }
        }

        // Normalise over tasks
        int nTasks = taskBuffer.size();
        int nTrans = taskBuffer.stream().mapToInt(Task::size).sum();
        if (nTrans == 0) return;

        // Adam meta-update
        metaT++;
        float bc1 = 1f - (float) Math.pow(ADAM_BETA1, metaT);
        float bc2 = 1f - (float) Math.pow(ADAM_BETA2, metaT);

        for (int a = 0; a < actionDim; a++) {
            float gb = sumDb[a] / nTrans;
            mB[a] = ADAM_BETA1 * mB[a] + (1 - ADAM_BETA1) * gb;
            vB[a] = ADAM_BETA2 * vB[a] + (1 - ADAM_BETA2) * gb * gb;
            metaB[a] -= metaLr * (mB[a] / bc1) / ((float) Math.sqrt(vB[a] / bc2) + ADAM_EPS);

            for (int i = 0; i < stateDim; i++) {
                float gw = sumDW[a][i] / nTrans;
                mW[a][i] = ADAM_BETA1 * mW[a][i] + (1 - ADAM_BETA1) * gw;
                vW[a][i] = ADAM_BETA2 * vW[a][i] + (1 - ADAM_BETA2) * gw * gw;
                metaW[a][i] -= metaLr * (mW[a][i] / bc1) / ((float) Math.sqrt(vW[a][i] / bc2) + ADAM_EPS);
            }
        }

        avgMetaLoss = 0.9f * avgMetaLoss + 0.1f * (totalLoss / nTrans);
        metaUpdates.incrementAndGet();
        taskBuffer.clear();

        Log.d(TAG, "Meta-update #" + metaUpdates.get()
                + " tasks=" + nTasks + " loss=" + String.format("%.4f", avgMetaLoss));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("metaUpdates",  metaUpdates.get());
        s.put("innerUpdates", innerUpdates.get());
        s.put("avgMetaLoss",  avgMetaLoss);
        s.put("avgInnerLoss", avgInnerLoss);
        s.put("taskBufferSize", taskBuffer.size());
        s.put("metaT",        metaT);
        return s;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private float[] forward(float[][] W, float[] b, float[] state) {
        float[] out = new float[actionDim];
        int sdim = Math.min(state.length, stateDim);
        for (int a = 0; a < actionDim; a++) {
            out[a] = b[a];
            for (int i = 0; i < sdim; i++) out[a] += W[a][i] * state[i];
        }
        return out;
    }

    private static float[] softmax(float[] v) {
        float max = v[0]; for (float x : v) if (x > max) max = x;
        float sum = 0f;
        float[] o = new float[v.length];
        for (int i = 0; i < v.length; i++) { o[i] = (float) Math.exp(v[i] - max); sum += o[i]; }
        for (int i = 0; i < v.length; i++) o[i] /= sum;
        return o;
    }

    private float[][] copyW(float[][] src) {
        float[][] dst = new float[src.length][];
        for (int i = 0; i < src.length; i++) dst[i] = src[i].clone();
        return dst;
    }
}
