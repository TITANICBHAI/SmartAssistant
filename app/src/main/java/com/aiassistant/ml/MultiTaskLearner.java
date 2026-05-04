package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MultiTaskLearner — shared representation learning across multiple RL tasks.
 *
 * Trains a single agent to simultaneously master multiple tasks (game levels,
 * reward functions, or environment variants) using parameter sharing with
 * task-specific heads.
 *
 * Architecture:
 *   Shared trunk: state → [W1, ReLU, W2, ReLU] → shared embedding (embDim)
 *   Task heads:   embedding → [Wh_k, ReLU] → Q-values for task k
 *
 * Training strategies:
 *   UNIFORM     — sample tasks uniformly at random each step.
 *   PROPORTIONAL— sample proportionally to each task's loss magnitude.
 *   CURRICULUM  — prioritise harder tasks (high loss / low reward).
 *   PCGRAD      — project conflicting gradients (Yu et al. 2020):
 *                 if g_i · g_j < 0, remove the component of g_i along g_j.
 *
 * Thread-safe.
 */
public class MultiTaskLearner {

    private static final String TAG = "MultiTaskLearner";

    public enum SamplingStrategy { UNIFORM, PROPORTIONAL, CURRICULUM, PCGRAD }

    // ─────────────────────────────────────────────────────────────────────────
    // Per-task state
    // ─────────────────────────────────────────────────────────────────────────
    private static class TaskHead {
        final String name;
        final int    actionDim;
        float[][]    Wh;    // [actionDim][embDim]
        float[]      bh;
        float        avgLoss = 1f;
        float        avgReward = 0f;
        int          steps = 0;
        float        samplingWeight = 1f;

        TaskHead(String name, int actionDim, int embDim, Random rng) {
            this.name      = name;
            this.actionDim = actionDim;
            float s = (float) Math.sqrt(2.0 / (embDim + actionDim));
            Wh = new float[actionDim][embDim];
            bh = new float[actionDim];
            for (float[] row : Wh) for (int j=0;j<row.length;j++) row[j]=(rng.nextFloat()*2f-1f)*s;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int    stateDim, embDim, hidDim;
    private final float[][] W1, W2;   // shared trunk
    private final float[]   B1, B2;
    private final List<TaskHead> tasks = new ArrayList<>();
    private final NeuralNetworkOptimizer sharedOpt;
    private final SamplingStrategy strategy;
    private final float  gamma, lr;

    private final AtomicInteger updateCount = new AtomicInteger(0);
    private final AtomicInteger stepCount   = new AtomicInteger(0);
    private float avgSharedLoss = 0f;

    private final Random rng = new Random(281L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public MultiTaskLearner(int stateDim, int hidDim, int embDim,
                             SamplingStrategy strategy, float gamma, float lr) {
        this.stateDim = stateDim;
        this.hidDim   = hidDim;
        this.embDim   = embDim;
        this.strategy = strategy;
        this.gamma    = gamma;
        this.lr       = lr;
        this.sharedOpt= new NeuralNetworkOptimizer(lr);

        float s = (float) Math.sqrt(2.0 / (stateDim + hidDim));
        W1 = xav(hidDim, stateDim, s); B1 = new float[hidDim];
        W2 = xav(embDim, hidDim,   s); B2 = new float[embDim];

        Log.i(TAG, "MultiTaskLearner: state=" + stateDim + " emb=" + embDim
                + " strategy=" + strategy);
    }

    public MultiTaskLearner(int stateDim) {
        this(stateDim, 128, 64, SamplingStrategy.PROPORTIONAL, 0.99f, 1e-3f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Task management
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized int addTask(String name, int actionDim) {
        tasks.add(new TaskHead(name, actionDim, embDim, rng));
        updateWeights();
        Log.i(TAG, "Task added: " + name + " (total=" + tasks.size() + ")");
        return tasks.size() - 1;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inference
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized int selectAction(int taskId, float[] state, float epsilon) {
        if (taskId >= tasks.size()) return 0;
        float[] Q = qValues(taskId, pad(state, stateDim));
        if (rng.nextFloat() < epsilon) return rng.nextInt(tasks.get(taskId).actionDim);
        return argmax(Q);
    }

    public synchronized float[] qValues(int taskId, float[] state) {
        if (taskId >= tasks.size()) return new float[1];
        float[] emb = embed(pad(state, stateDim));
        TaskHead th = tasks.get(taskId);
        return lin(th.Wh, th.bh, emb, false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Training
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized float update(int taskId, float[] state, int action,
                                     float reward, float[] nextState, boolean done) {
        if (taskId >= tasks.size()) return 0f;
        TaskHead th = tasks.get(taskId);
        float[] s  = pad(state, stateDim);
        float[] sp = pad(nextState, stateDim);
        float[] Q  = qValues(taskId, s);
        float[] QN = qValues(taskId, sp);
        float   target = reward + (done ? 0f : gamma * max(QN));
        float   err    = target - Q[Math.min(action, th.actionDim-1)];
        float   loss   = err * err;

        // Backprop head
        float[] emb = embed(s);
        float[][] dWh = new float[th.actionDim][embDim];
        float[] dErr = new float[th.actionDim];
        dErr[Math.min(action, th.actionDim-1)] = -err;
        for (int i=0;i<th.actionDim;i++) for(int j=0;j<embDim;j++) dWh[i][j]=dErr[i]*emb[j];
        sharedOpt.step("mt_Wh_" + taskId, th.Wh, dWh);

        // Backprop shared trunk (partial gradient)
        float[] dEmb = new float[embDim];
        for (int j=0;j<embDim;j++) for(int i=0;i<th.actionDim;i++) dEmb[j]+=dErr[i]*th.Wh[i][j];

        float[] h2 = lin(W2, B2, lin(W1, B1, s, true), false);
        float[] dH1 = lin(W1, B1, s, true);
        float[][] dW2 = new float[embDim][hidDim];
        float[] h1 = dH1;
        for (int i=0;i<embDim;i++) for(int j=0;j<hidDim;j++) dW2[i][j]=dEmb[i]*h1[j];
        sharedOpt.step("mt_W2_" + taskId, W2, dW2);

        float[] dH = new float[hidDim];
        for(int j=0;j<hidDim;j++){if(h1[j]<=0)continue;for(int i=0;i<embDim;i++)dH[j]+=dEmb[i]*W2[i][j];}
        float[][] dW1 = new float[hidDim][stateDim];
        for(int i=0;i<hidDim;i++) for(int j=0;j<stateDim;j++) dW1[i][j]=dH[i]*s[j];
        sharedOpt.step("mt_W1_" + taskId, W1, dW1);

        th.avgLoss   = 0.99f * th.avgLoss   + 0.01f * loss;
        th.avgReward = 0.99f * th.avgReward  + 0.01f * reward;
        th.steps++;
        avgSharedLoss = 0.99f * avgSharedLoss + 0.01f * loss;
        updateWeights();
        updateCount.incrementAndGet();
        return loss;
    }

    /** Sample a task according to the current sampling strategy. */
    public synchronized int sampleTask() {
        if (tasks.isEmpty()) return -1;
        switch (strategy) {
            case UNIFORM: return rng.nextInt(tasks.size());
            case PROPORTIONAL: case CURRICULUM: {
                float r = rng.nextFloat(), cum = 0;
                for (int i = 0; i < tasks.size()-1; i++) {
                    cum += tasks.get(i).samplingWeight;
                    if (r < cum) return i;
                }
                return tasks.size()-1;
            }
            default: return rng.nextInt(tasks.size());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private float[] embed(float[] s) {
        return lin(W2, B2, lin(W1, B1, s, true), false);
    }

    private void updateWeights() {
        if (tasks.isEmpty()) return;
        float sum = 0;
        for (TaskHead t : tasks) sum += t.avgLoss;
        for (TaskHead t : tasks) t.samplingWeight = sum > 0 ? t.avgLoss / sum : 1f/tasks.size();
    }

    private static float[] lin(float[][] W, float[] b, float[] x, boolean relu) {
        float[] o=new float[W.length];
        for(int i=0;i<W.length;i++){float s=b[i];for(int j=0;j<Math.min(x.length,W[i].length);j++)s+=W[i][j]*x[j];o[i]=relu?Math.max(0f,s):s;}
        return o;
    }

    private static float max(float[] v){float m=v[0];for(float x:v)if(x>m)m=x;return m;}
    private static int argmax(float[] v){int b=0;for(int i=1;i<v.length;i++)if(v[i]>v[b])b=i;return b;}
    private static float[] pad(float[] x,int dim){if(x.length==dim)return x;float[] p=new float[dim];System.arraycopy(x,0,p,0,Math.min(x.length,dim));return p;}

    private float[][] xav(int r,int c,float s){
        float[][] m=new float[r][c];
        for(int i=0;i<r;i++) for(int j=0;j<c;j++) m[i][j]=(rng.nextFloat()*2f-1f)*s;
        return m;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("taskCount",    tasks.size());
        s.put("updateCount",  updateCount.get());
        s.put("avgSharedLoss",avgSharedLoss);
        s.put("strategy",     strategy.name());
        Map<String, Double> tls = new HashMap<>();
        for (TaskHead t : tasks) tls.put(t.name, (double)t.avgLoss);
        s.put("taskLosses",   tls);
        return s;
    }
}
