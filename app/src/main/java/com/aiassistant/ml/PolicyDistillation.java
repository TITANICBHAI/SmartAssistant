package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PolicyDistillation — compress a large/complex teacher policy into a smaller student.
 *
 * Applications:
 *   1. KNOWLEDGE DISTILLATION: Transfer knowledge from an ensemble of agents (teacher)
 *      into a single compact network (student). Reduces inference time on-device.
 *
 *   2. POLICY COMPRESSION: Replace a deep 3-layer teacher with a shallow 1-layer student,
 *      guided by KL-divergence minimization.
 *
 *   3. MULTI-TASK DISTILLATION: Distill multiple task-specific policies into one
 *      universal policy that handles all tasks.
 *
 *   4. ONLINE DISTILLATION: Run teacher and student concurrently; student learns from
 *      teacher's soft labels while also receiving its own RL gradient.
 *
 * Loss functions:
 *   KL_DIVERGENCE   — KL(π_teacher || π_student)  (standard Hinton distillation)
 *   MSE_LOGITS      — ||logits_teacher - logits_student||²
 *   SYMMETRIC_KL    — 0.5·KL(T||S) + 0.5·KL(S||T)
 *   TEMPERATURE_KL  — KL with temperature τ: softer distributions → more transfer
 *
 * Thread-safe.
 */
public class PolicyDistillation {

    private static final String TAG = "PolicyDistillation";

    public enum DistillLoss { KL_DIVERGENCE, MSE_LOGITS, SYMMETRIC_KL, TEMPERATURE_KL }

    // ─────────────────────────────────────────────────────────────────────────
    // Teacher interface
    // ─────────────────────────────────────────────────────────────────────────
    public interface Teacher {
        float[] actionLogits(float[] state);    // raw logits (pre-softmax)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Student network (2-layer MLP)
    // ─────────────────────────────────────────────────────────────────────────
    private final int stateDim, hidDim, actionDim;
    private final float[][] W1, W2;
    private final float[]   b1, b2;
    private final NeuralNetworkOptimizer opt;

    // ─────────────────────────────────────────────────────────────────────────
    // Config
    // ─────────────────────────────────────────────────────────────────────────
    private final List<Teacher> teachers     = new ArrayList<>();
    private final DistillLoss   lossType;
    private final float         temperature; // for TEMPERATURE_KL
    private final float         alpha;       // blend: α·distill + (1-α)·task_loss

    // Replay buffer for offline distillation
    private final List<float[]>   replayStates = new ArrayList<>();
    private final List<float[]>   replayLogits = new ArrayList<>();
    private final int             maxReplay;

    private final AtomicInteger updateCount   = new AtomicInteger(0);
    private final AtomicInteger distillSteps  = new AtomicInteger(0);
    private float avgDistillLoss = 0f;
    private float avgTaskLoss    = 0f;

    private final Random rng = new Random(151L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public PolicyDistillation(int stateDim, int hidDim, int actionDim, float lr,
                               DistillLoss lossType, float temperature, float alpha,
                               int maxReplay) {
        this.stateDim    = stateDim;
        this.hidDim      = hidDim;
        this.actionDim   = actionDim;
        this.lossType    = lossType;
        this.temperature = temperature;
        this.alpha       = alpha;
        this.maxReplay   = maxReplay;

        float s = (float) Math.sqrt(2.0 / (stateDim + hidDim));
        W1 = xav(hidDim, stateDim, s);   b1 = new float[hidDim];
        W2 = xav(actionDim, hidDim, s);  b2 = new float[actionDim];
        opt= new NeuralNetworkOptimizer(lr);

        Log.i(TAG, "PolicyDistillation: s=" + stateDim + " h=" + hidDim
                + " a=" + actionDim + " loss=" + lossType);
    }

    public PolicyDistillation(int stateDim, int actionDim) {
        this(stateDim, 128, actionDim, 3e-4f, DistillLoss.KL_DIVERGENCE, 3f, 0.5f, 10_000);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Teacher management
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized void addTeacher(Teacher teacher) { teachers.add(teacher); }

    // ─────────────────────────────────────────────────────────────────────────
    // Student inference
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized float[] studentLogits(float[] state) {
        float[] h = lin(W1, b1, pad(state, stateDim), true);
        return lin(W2, b2, h, false);
    }

    public synchronized int studentAction(float[] state) {
        return argmax(softmax(studentLogits(state)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Distillation step
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * One online distillation step.
     * @param state         Current state.
     * @param taskAction    Action selected by task RL loss (for blending).
     * @param taskAdvantage Advantage estimate from RL (for task loss gradient).
     * @return Total loss = α·distill + (1-α)·task.
     */
    public synchronized float distillStep(float[] state, int taskAction, float taskAdvantage) {
        float[] s = pad(state, stateDim);

        // ── Teacher ensemble: average soft labels ─────────────────────────
        float[] tLogits = ensembleLogits(s);

        // ── Student forward ────────────────────────────────────────────────
        float[] h       = lin(W1, b1, s, true);
        float[] sLogits = lin(W2, b2, h, false);

        // ── Distillation loss ──────────────────────────────────────────────
        float[] dGrad = distillGradient(tLogits, sLogits);
        float distillLoss = distillLossValue(tLogits, sLogits);

        // ── Task loss (policy gradient) ────────────────────────────────────
        float[] tProbs = softmax(sLogits);
        float[] taskGrad = new float[actionDim];
        float taskLoss = 0f;
        if (taskAction >= 0 && taskAction < actionDim) {
            taskLoss = -(float) Math.log(Math.max(tProbs[taskAction], 1e-8f)) * taskAdvantage;
            for (int a = 0; a < actionDim; a++)
                taskGrad[a] = (tProbs[a] - (a == taskAction ? 1f : 0f)) * taskAdvantage;
        }

        // ── Combined gradient ──────────────────────────────────────────────
        float[] grad = new float[actionDim];
        for (int a = 0; a < actionDim; a++)
            grad[a] = alpha * dGrad[a] + (1f - alpha) * taskGrad[a];

        // ── Backprop ───────────────────────────────────────────────────────
        float[][] dW2 = bp(hidDim, actionDim, h, grad);
        opt.step("pd_W2", W2, dW2);
        float[] dH = new float[hidDim];
        for (int j = 0; j < hidDim; j++) {
            if (h[j] <= 0) continue;
            for (int a = 0; a < actionDim; a++) dH[j] += grad[a] * W2[a][j];
        }
        float[][] dW1 = bp(stateDim, hidDim, s, dH);
        opt.step("pd_W1", W1, dW1);

        // ── Cache for offline replay ───────────────────────────────────────
        if (replayStates.size() < maxReplay) {
            replayStates.add(s.clone());
            replayLogits.add(tLogits.clone());
        }

        avgDistillLoss = 0.99f * avgDistillLoss + 0.01f * distillLoss;
        avgTaskLoss    = 0.99f * avgTaskLoss    + 0.01f * taskLoss;
        distillSteps.incrementAndGet();
        return alpha * distillLoss + (1f - alpha) * taskLoss;
    }

    /** Offline replay distillation from stored (state, teacherLogits) pairs. */
    public synchronized float replayDistill(int batchSize) {
        if (replayStates.isEmpty()) return 0f;
        float totalLoss = 0;
        for (int i = 0; i < batchSize; i++) {
            int idx = rng.nextInt(replayStates.size());
            totalLoss += distillStep(replayStates.get(idx), -1, 0f);
        }
        updateCount.incrementAndGet();
        return totalLoss / batchSize;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Loss implementations
    // ─────────────────────────────────────────────────────────────────────────

    private float[] ensembleLogits(float[] s) {
        if (teachers.isEmpty()) return new float[actionDim];
        float[] avg = new float[actionDim];
        for (Teacher t : teachers) {
            float[] lg = t.actionLogits(s);
            for (int a = 0; a < Math.min(actionDim, lg.length); a++) avg[a] += lg[a];
        }
        for (int a = 0; a < actionDim; a++) avg[a] /= teachers.size();
        return avg;
    }

    private float[] distillGradient(float[] tLogits, float[] sLogits) {
        float τ = temperature;
        float[] tProbs = softmax(scale(tLogits, 1f/τ));
        float[] sProbs = softmax(scale(sLogits, 1f/τ));
        float[] grad   = new float[actionDim];
        switch (lossType) {
            case MSE_LOGITS:
                for (int a=0;a<actionDim;a++) grad[a]=2f*(sLogits[a]-tLogits[a]);
                break;
            case SYMMETRIC_KL:
                for (int a=0;a<actionDim;a++)
                    grad[a]=0.5f*(sProbs[a]-tProbs[a])+0.5f*(tProbs[a]/(sProbs[a]+1e-8f)-1f)*sProbs[a];
                break;
            case KL_DIVERGENCE:
            case TEMPERATURE_KL:
            default:
                // KL(T||S): grad = -(T_a / S_a) * dS_a/dlogit
                for (int a=0;a<actionDim;a++) grad[a]=(sProbs[a]-tProbs[a])*(τ*τ);
                break;
        }
        return grad;
    }

    private float distillLossValue(float[] tLogits, float[] sLogits) {
        float[] tProbs = softmax(scale(tLogits, 1f/temperature));
        float[] sProbs = softmax(scale(sLogits, 1f/temperature));
        float kl = 0;
        for (int a=0;a<actionDim;a++) kl += tProbs[a]*(float)(Math.log(tProbs[a]+1e-8f)-Math.log(sProbs[a]+1e-8f));
        return kl;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("distillSteps",  distillSteps.get());
        s.put("updateCount",   updateCount.get());
        s.put("avgDistillLoss",avgDistillLoss);
        s.put("avgTaskLoss",   avgTaskLoss);
        s.put("teacherCount",  teachers.size());
        s.put("replaySize",    replayStates.size());
        s.put("lossType",      lossType.name());
        s.put("temperature",   temperature);
        return s;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static float[] lin(float[][] W, float[] b, float[] x, boolean relu) {
        float[] o = new float[W.length];
        for (int i = 0; i < W.length; i++) {
            float s = b[i];
            for (int j = 0; j < Math.min(x.length, W[i].length); j++) s += W[i][j] * x[j];
            o[i] = relu ? Math.max(0f, s) : s;
        }
        return o;
    }

    private static float[][] bp(int inD, int outD, float[] inp, float[] dout) {
        float[][] g = new float[outD][inD];
        for (int i = 0; i < outD; i++)
            for (int j = 0; j < Math.min(inD, inp.length); j++) g[i][j] = dout[i] * inp[j];
        return g;
    }

    private static float[] softmax(float[] v) {
        float mx = v[0]; for (float x : v) if (x > mx) mx = x;
        float sum = 0; float[] o = new float[v.length];
        for (int i=0;i<v.length;i++){o[i]=(float)Math.exp(v[i]-mx);sum+=o[i];}
        for (int i=0;i<v.length;i++) o[i]/=sum;
        return o;
    }

    private static float[] scale(float[] v, float s) {
        float[] o = new float[v.length];
        for (int i = 0; i < v.length; i++) o[i] = v[i] * s;
        return o;
    }

    private static int argmax(float[] v) { int b=0; for(int i=1;i<v.length;i++) if(v[i]>v[b]) b=i; return b; }

    private static float[] pad(float[] x, int dim) {
        if (x.length == dim) return x;
        float[] p = new float[dim]; System.arraycopy(x, 0, p, 0, Math.min(x.length, dim));
        return p;
    }

    private float[][] xav(int r, int c, float s) {
        float[][] m = new float[r][c];
        for (int i = 0; i < r; i++)
            for (int j = 0; j < c; j++) m[i][j] = (rng.nextFloat()*2f-1f)*s;
        return m;
    }
}
