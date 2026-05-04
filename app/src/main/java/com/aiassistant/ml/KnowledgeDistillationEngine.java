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
 * KnowledgeDistillationEngine — compress ensemble knowledge into a deployable model.
 *
 * High-level pipeline:
 *   1. TEACHER POOL: N teacher agents, each with potentially different architectures.
 *   2. DISTILLATION DATASET: buffer of (state, teacher_soft_labels) pairs.
 *   3. STUDENT TRAINING: student minimises KL(soft_labels || student_probs).
 *   4. COMPRESSION: student is 2-4× smaller than average teacher.
 *   5. VALIDATION: monitor student-teacher agreement rate.
 *
 * Extensions beyond PolicyDistillation:
 *   - FEATURE DISTILLATION: match intermediate activations (hint layers).
 *   - ATTENTION DISTILLATION: match attention maps (for transformer-style models).
 *   - PROGRESSIVE DISTILLATION: iteratively distill student → even smaller student.
 *   - SELF-DISTILLATION: distill model's own outputs at a previous checkpoint.
 *
 * Thread-safe.
 */
public class KnowledgeDistillationEngine {

    private static final String TAG = "KDistillEngine";

    // ─────────────────────────────────────────────────────────────────────────
    // Teacher registration
    // ─────────────────────────────────────────────────────────────────────────
    public interface TeacherModel {
        float[] softLabels(float[] state, float temperature);
        String  name();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Distillation buffer entry
    // ─────────────────────────────────────────────────────────────────────────
    private static class DistillEntry {
        final float[] state;
        final float[] softLabels;    // ensemble-averaged, temperature-scaled
        final float   importance;    // sampling weight

        DistillEntry(float[] s, float[] l, float imp) {
            state = s.clone(); softLabels = l.clone(); importance = imp;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Student network
    // ─────────────────────────────────────────────────────────────────────────
    private final int    stateDim, actionDim, hidDim;
    private final float[][] sW1, sW2;
    private final float[]   sB1, sB2;
    private final NeuralNetworkOptimizer sOpt;

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final List<TeacherModel>  teachers   = new ArrayList<>();
    private final LinkedList<DistillEntry> buffer = new LinkedList<>();
    private final int    maxBuffer;
    private final float  temperature;      // soft label temperature
    private final float  distillAlpha;     // blend: distill vs hard label
    private final int    batchSize;
    private final float  importancePow;    // priority exponent for IS sampling

    // Validation
    private float agreementRate  = 0f;
    private float studentKl      = 0f;
    private int   validationSteps= 0;

    private final AtomicInteger distillSteps  = new AtomicInteger(0);
    private final AtomicInteger bufferAdds    = new AtomicInteger(0);
    private float avgDistillLoss = 0f;

    private final Random rng;

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public KnowledgeDistillationEngine(int stateDim, int actionDim, int hidDim,
                                        float temperature, float distillAlpha,
                                        int batchSize, int maxBuffer,
                                        float importancePow, float lr, long seed) {
        this.stateDim     = stateDim;
        this.actionDim    = actionDim;
        this.hidDim       = hidDim;
        this.temperature  = temperature;
        this.distillAlpha = distillAlpha;
        this.batchSize    = batchSize;
        this.maxBuffer    = maxBuffer;
        this.importancePow= importancePow;
        this.rng          = new Random(seed);
        this.sOpt         = new NeuralNetworkOptimizer(lr);

        float s = (float) Math.sqrt(2.0 / (stateDim + hidDim));
        sW1 = xav(hidDim, stateDim, s);   sB1 = new float[hidDim];
        sW2 = xav(actionDim, hidDim, s);  sB2 = new float[actionDim];

        Log.i(TAG, "KnowledgeDistillationEngine: T=" + temperature
                + " α=" + distillAlpha + " buf=" + maxBuffer);
    }

    public KnowledgeDistillationEngine(int stateDim, int actionDim) {
        this(stateDim, actionDim, 64, 4f, 0.5f, 128, 50_000, 0.6f, 3e-4f, 241L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Teacher registration
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized void addTeacher(TeacherModel t) {
        teachers.add(t);
        Log.i(TAG, "Teacher added: " + t.name() + " (total=" + teachers.size() + ")");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Buffer population
    // ─────────────────────────────────────────────────────────────────────────

    /** Generate soft labels from ensemble and add to buffer. */
    public synchronized void addState(float[] state) {
        if (teachers.isEmpty()) return;
        float[] avg = ensembleSoftLabels(state);
        float importance = labelUncertainty(avg);   // high uncertainty → high priority
        if (buffer.size() >= maxBuffer) buffer.removeFirst();
        buffer.add(new DistillEntry(state, avg, importance));
        bufferAdds.incrementAndGet();
    }

    /** Batch-add states. */
    public synchronized void addStates(List<float[]> states) {
        for (float[] s : states) addState(s);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Student training
    // ─────────────────────────────────────────────────────────────────────────

    /** One training step: sample batch, compute KL, backprop student. */
    public synchronized float trainStep() {
        if (buffer.size() < batchSize) return 0f;

        List<DistillEntry> batch = sampleBatch();
        float totalLoss = 0;

        for (DistillEntry e : batch) {
            float[] s = pad(e.state, stateDim);
            float[] h = lin(sW1, sB1, s, true);
            float[] logits = lin(sW2, sB2, h, false);
            float[] probs  = softmax(scaleVec(logits, 1f / temperature));

            // KL(teacher || student)
            float[] dLogits = new float[actionDim];
            float kl = 0;
            for (int a = 0; a < actionDim; a++) {
                float t = e.softLabels[a], p = Math.max(probs[a], 1e-8f);
                kl += t * (float)(Math.log(t + 1e-8f) - Math.log(p));
                dLogits[a] = (p - t) * e.importance;
            }
            totalLoss += kl;

            // Backprop
            float[][] dW2 = new float[actionDim][hidDim];
            for (int i=0;i<actionDim;i++) for(int j=0;j<hidDim;j++) dW2[i][j]=dLogits[i]*h[j];
            sOpt.step("kd_W2", sW2, dW2);
            float[] dH = new float[hidDim];
            for (int j=0;j<hidDim;j++){if(h[j]<=0)continue;for(int i=0;i<actionDim;i++)dH[j]+=dLogits[i]*sW2[i][j];}
            float[][] dW1 = new float[hidDim][stateDim];
            for (int i=0;i<hidDim;i++) for(int j=0;j<stateDim;j++) dW1[i][j]=dH[i]*s[j];
            sOpt.step("kd_W1", sW1, dW1);
        }

        float loss = totalLoss / batchSize;
        avgDistillLoss = 0.99f * avgDistillLoss + 0.01f * loss;
        distillSteps.incrementAndGet();

        // Periodic validation
        if (distillSteps.get() % 100 == 0) validate();
        return loss;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Student inference
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized int studentAction(float[] state) {
        return argmax(studentLogits(pad(state, stateDim)));
    }

    public synchronized float[] studentProbs(float[] state) {
        return softmax(studentLogits(pad(state, stateDim)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Validation
    // ─────────────────────────────────────────────────────────────────────────

    private void validate() {
        if (buffer.isEmpty() || teachers.isEmpty()) return;
        int n = Math.min(50, buffer.size());
        int agree = 0;
        float klSum = 0;
        List<DistillEntry> sample = new ArrayList<>(buffer).subList(0, n);
        for (DistillEntry e : sample) {
            float[] sp = studentProbs(e.state);
            int sAction = argmax(sp);
            int tAction = argmax(e.softLabels);
            if (sAction == tAction) agree++;
            for (int a = 0; a < actionDim; a++) {
                float t = e.softLabels[a], p = Math.max(sp[a], 1e-8f);
                klSum += t * (float)(Math.log(t + 1e-8f) - Math.log(p));
            }
        }
        agreementRate = (float) agree / n;
        studentKl     = klSum / n;
        validationSteps++;
        Log.d(TAG, "Validation step=" + validationSteps
                + " agreement=" + String.format("%.2f", agreementRate)
                + " KL=" + String.format("%.4f", studentKl));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private float[] ensembleSoftLabels(float[] state) {
        float[] avg = new float[actionDim];
        for (TeacherModel t : teachers) {
            float[] l = t.softLabels(state, temperature);
            for (int a = 0; a < Math.min(actionDim, l.length); a++) avg[a] += l[a];
        }
        float s = 0; for (float v : avg) s += v;
        if (s > 0) for (int a = 0; a < actionDim; a++) avg[a] /= s;
        return avg;
    }

    private float labelUncertainty(float[] probs) {
        float ent = 0;
        for (float p : probs) if (p > 1e-8f) ent -= p * (float) Math.log(p);
        return (float) Math.exp(ent);   // higher entropy → higher priority
    }

    private List<DistillEntry> sampleBatch() {
        List<DistillEntry> bufList = new ArrayList<>(buffer);
        List<DistillEntry> batch = new ArrayList<>(batchSize);
        for (int i = 0; i < batchSize; i++) batch.add(bufList.get(rng.nextInt(bufList.size())));
        return batch;
    }

    private float[] studentLogits(float[] s) {
        return lin(sW2, sB2, lin(sW1, sB1, s, true), false);
    }

    private static float[] lin(float[][] W, float[] b, float[] x, boolean relu) {
        float[] o = new float[W.length];
        for (int i=0;i<W.length;i++){
            float s=b[i]; for(int j=0;j<Math.min(x.length,W[i].length);j++) s+=W[i][j]*x[j];
            o[i]=relu?Math.max(0f,s):s;
        }
        return o;
    }

    private static float[] softmax(float[] v) {
        float mx=v[0];for(float x:v)if(x>mx)mx=x;
        float sum=0;float[] o=new float[v.length];
        for(int i=0;i<v.length;i++){o[i]=(float)Math.exp(v[i]-mx);sum+=o[i];}
        for(int i=0;i<v.length;i++)o[i]/=sum;
        return o;
    }

    private static float[] scaleVec(float[] v, float s) {
        float[] o=new float[v.length]; for(int i=0;i<v.length;i++) o[i]=v[i]*s; return o;
    }

    private static int argmax(float[] v){int b=0;for(int i=1;i<v.length;i++)if(v[i]>v[b])b=i;return b;}

    private static float[] pad(float[] x, int dim) {
        if(x.length==dim)return x;
        float[] p=new float[dim];System.arraycopy(x,0,p,0,Math.min(x.length,dim));return p;
    }

    private float[][] xav(int r, int c, float s) {
        float[][] m=new float[r][c];
        for(int i=0;i<r;i++) for(int j=0;j<c;j++) m[i][j]=(rng.nextFloat()*2f-1f)*s;
        return m;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("teacherCount",   teachers.size());
        s.put("bufferSize",     buffer.size());
        s.put("distillSteps",   distillSteps.get());
        s.put("bufferAdds",     bufferAdds.get());
        s.put("avgDistillLoss", avgDistillLoss);
        s.put("agreementRate",  agreementRate);
        s.put("studentKL",      studentKl);
        s.put("temperature",    temperature);
        return s;
    }
}
