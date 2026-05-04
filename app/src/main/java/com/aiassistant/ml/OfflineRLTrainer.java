package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * OfflineRLTrainer — batch offline RL training from a fixed dataset.
 *
 * Offline RL learns a policy from a static dataset of logged interactions
 * (state, action, reward, nextState, done) WITHOUT any further environment
 * interaction. This is critical for safety-sensitive applications.
 *
 * Supported algorithms:
 *
 *   BEHAVIOUR_CLONING (BC):
 *     Supervised imitation: minimise cross-entropy loss between policy and logged actions.
 *     Simple, fast, but ignores reward signal.
 *
 *   CONSERVATIVE_Q_LEARNING (CQL):
 *     CQL loss = standard Q loss + α·E_{s~D}[log Σ_a exp(Q(s,a)) - Q(s, a_D)]
 *     Penalises Q-values for OOD (out-of-distribution) actions.
 *
 *   IMPLICIT_Q_LEARNING (IQL):
 *     Expectile regression on values; avoids querying OOD actions at all.
 *     V̂(s) = expectile_τ loss on min(Q1, Q2)
 *     Q̂(s,a) = r + γ·V̂(s')
 *
 *   DECISION_TRANSFORMER (DT):
 *     Treat trajectory as sequence: (G_t, s_t, a_t, G_{t+1}, s_{t+1}, ...)
 *     Simplified: linear regression from (G, s) → a (without full transformer).
 *
 * Thread-safe.
 */
public class OfflineRLTrainer {

    private static final String TAG = "OfflineRL";

    public enum Algorithm { BEHAVIOUR_CLONING, CONSERVATIVE_Q_LEARNING,
                            IMPLICIT_Q_LEARNING, DECISION_TRANSFORMER }

    // ─────────────────────────────────────────────────────────────────────────
    // Dataset
    // ─────────────────────────────────────────────────────────────────────────
    public static class Transition {
        public final float[]  state, nextState;
        public final int      action;
        public final float    reward;
        public final boolean  done;
        public final float    returnToGo; // for DT

        public Transition(float[] state, int action, float reward,
                          float[] nextState, boolean done, float returnToGo) {
            this.state      = state;
            this.action     = action;
            this.reward     = reward;
            this.nextState  = nextState;
            this.done       = done;
            this.returnToGo = returnToGo;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Policy network (shared across algorithms)
    // ─────────────────────────────────────────────────────────────────────────
    private final int         stateDim, actionDim, hidDim;
    private final float[][]   W1, W2;      // [hidDim][stateDim], [actionDim][hidDim]
    private final float[]     b1, b2;
    // Q-network 1 and 2 (for CQL / IQL twin critics)
    private final float[][]   QW1a, QW2a, QW1b, QW2b;
    private final float[]     Qb1a, Qb2a, Qb1b, Qb2b;
    // Value network (IQL)
    private final float[][]   VW1, VW2;
    private final float[]     Vb1, Vb2;

    private final NeuralNetworkOptimizer polOpt, qOpt, vOpt;
    private final Algorithm   algorithm;
    private final float       gamma, alpha, tau, expectileTau;
    private final int         batchSize;

    private final List<Transition> dataset = new ArrayList<>();
    private final int              maxDataset;

    private final AtomicInteger trainSteps = new AtomicInteger(0);
    private float avgPolicyLoss = 0f;
    private float avgQLoss      = 0f;
    private float avgVLoss      = 0f;

    private final Random rng;

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public OfflineRLTrainer(int stateDim, int actionDim, int hidDim,
                             Algorithm algorithm, float gamma, float alpha,
                             float tau, float expectileTau, int batchSize,
                             int maxDataset, float lr, long seed) {
        this.stateDim     = stateDim;
        this.actionDim    = actionDim;
        this.hidDim       = hidDim;
        this.algorithm    = algorithm;
        this.gamma        = gamma;
        this.alpha        = alpha;
        this.tau          = tau;
        this.expectileTau = expectileTau;
        this.batchSize    = batchSize;
        this.maxDataset   = maxDataset;
        this.rng          = new Random(seed);

        float s = (float) Math.sqrt(2.0 / (stateDim + hidDim));
        W1   = xav(hidDim, stateDim, s); b1  = new float[hidDim];
        W2   = xav(actionDim, hidDim, s);b2  = new float[actionDim];

        QW1a = xav(hidDim, stateDim+actionDim, s); Qb1a = new float[hidDim];
        QW2a = xav(1,      hidDim,  s); Qb2a = new float[1];
        QW1b = xav(hidDim, stateDim+actionDim, s); Qb1b = new float[hidDim];
        QW2b = xav(1,      hidDim,  s); Qb2b = new float[1];

        VW1  = xav(hidDim, stateDim, s); Vb1  = new float[hidDim];
        VW2  = xav(1,      hidDim,  s); Vb2  = new float[1];

        polOpt = new NeuralNetworkOptimizer(lr);
        qOpt   = new NeuralNetworkOptimizer(lr);
        vOpt   = new NeuralNetworkOptimizer(lr * 0.5f);

        Log.i(TAG, "OfflineRLTrainer: algo=" + algorithm + " state=" + stateDim
                + " actions=" + actionDim);
    }

    public OfflineRLTrainer(int stateDim, int actionDim, Algorithm algorithm) {
        this(stateDim, actionDim, 128, algorithm, 0.99f, 0.1f, 0.005f, 0.7f,
             256, 100_000, 3e-4f, 211L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Dataset management
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized void addTransition(Transition t) {
        if (dataset.size() >= maxDataset) dataset.remove(rng.nextInt(dataset.size()));
        dataset.add(t);
    }

    public synchronized void addTransitions(List<Transition> transitions) {
        for (Transition t : transitions) addTransition(t);
    }

    public synchronized int datasetSize() { return dataset.size(); }

    // ─────────────────────────────────────────────────────────────────────────
    // Training step
    // ─────────────────────────────────────────────────────────────────────────

    /** Sample a batch and perform one gradient update. */
    public synchronized float trainStep() {
        if (dataset.size() < batchSize) return 0f;
        List<Transition> batch = sampleBatch();
        float loss;
        switch (algorithm) {
            case CONSERVATIVE_Q_LEARNING: loss = trainCQL(batch);   break;
            case IMPLICIT_Q_LEARNING:     loss = trainIQL(batch);   break;
            case DECISION_TRANSFORMER:    loss = trainDT(batch);    break;
            case BEHAVIOUR_CLONING:
            default:                      loss = trainBC(batch);    break;
        }
        trainSteps.incrementAndGet();
        return loss;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Algorithm implementations
    // ─────────────────────────────────────────────────────────────────────────

    private float trainBC(List<Transition> batch) {
        float loss = 0;
        for (Transition t : batch) {
            float[] s = pad(t.state, stateDim);
            float[] h = lin(W1, b1, s, true);
            float[] logits = lin(W2, b2, h, false);
            float[] probs  = softmax(logits);
            int a = t.action;
            float l = -(float) Math.log(Math.max(probs[a], 1e-8f));
            loss += l;

            float[] dL = probs.clone(); dL[a] -= 1f;
            float[][] dW2 = outer(dL, h, hidDim, actionDim);
            polOpt.step("bc_W2", W2, dW2);
            float[] dH = new float[hidDim];
            for (int j = 0; j < hidDim; j++) {
                if (h[j] <= 0) continue;
                for (int i = 0; i < actionDim; i++) dH[j] += dL[i] * W2[i][j];
            }
            float[][] dW1 = outer(dH, s, stateDim, hidDim);
            polOpt.step("bc_W1", W1, dW1);
        }
        avgPolicyLoss = 0.99f * avgPolicyLoss + 0.01f * loss / batch.size();
        return loss / batch.size();
    }

    private float trainCQL(List<Transition> batch) {
        float ql = trainBC(batch);  // also update policy via BC
        float cql = 0;
        for (Transition t : batch) {
            float[] s  = pad(t.state,     stateDim);
            float[] sp = pad(t.nextState, stateDim);
            float[] qa_in = saInput(sp, bestAction(sp));
            float   nextQ = Math.min(qForward(QW1a, Qb1a, QW2a, Qb2a, qa_in),
                                     qForward(QW1b, Qb1b, QW2b, Qb2b, qa_in));
            float   target = t.reward + (t.done ? 0f : gamma * nextQ);
            float[] sa_in  = saInput(s, t.action);
            float   qa = qForward(QW1a, Qb1a, QW2a, Qb2a, sa_in);
            float   err = qa - target;

            // CQL penalty: E[log Σ exp Q] - Q(s, a_data)
            float   logsumexpQ = logSumExpQ(s);
            float   cqlPenalty = alpha * (logsumexpQ - qa);
            float   totalGrad  = err + alpha * cqlPenalty;
            simpleQBackprop(QW1a, Qb1a, QW2a, Qb2a, sa_in, totalGrad, "cql_a");
            simpleQBackprop(QW1b, Qb1b, QW2b, Qb2b, sa_in, err,       "cql_b");
            cql += Math.abs(err);
        }
        avgQLoss = 0.99f * avgQLoss + 0.01f * cql / batch.size();
        return (ql + cql / batch.size()) / 2f;
    }

    private float trainIQL(List<Transition> batch) {
        float ql = 0, vl = 0;
        for (Transition t : batch) {
            float[] s  = pad(t.state,     stateDim);
            float[] sp = pad(t.nextState, stateDim);
            // Value update: V(s) via expectile regression on min(Q1, Q2)
            float[] sa_in = saInput(s, t.action);
            float q1 = qForward(QW1a, Qb1a, QW2a, Qb2a, sa_in);
            float q2 = qForward(QW1b, Qb1b, QW2b, Qb2b, sa_in);
            float minQ = Math.min(q1, q2);
            float vs   = vForward(s);
            float diff = minQ - vs;
            float weight = diff < 0 ? (1f - expectileTau) : expectileTau;
            float vGrad  = -weight * diff;
            vl += diff * diff;
            simpleVBackprop(s, vGrad);

            // Q update: r + γ·V(s')
            float vsp    = t.done ? 0f : vForward(sp);
            float target = t.reward + gamma * vsp;
            float qErr1  = q1 - target, qErr2 = q2 - target;
            simpleQBackprop(QW1a, Qb1a, QW2a, Qb2a, sa_in, qErr1, "iql_q1");
            simpleQBackprop(QW1b, Qb1b, QW2b, Qb2b, sa_in, qErr2, "iql_q2");
            ql += qErr1 * qErr1;
        }
        avgQLoss = 0.99f * avgQLoss + 0.01f * ql / batch.size();
        avgVLoss = 0.99f * avgVLoss + 0.01f * vl / batch.size();
        return (ql + vl) / batch.size() / 2f;
    }

    private float trainDT(List<Transition> batch) {
        // Simplified DT: predict action from (returnToGo, state) via BC
        float loss = 0;
        for (Transition t : batch) {
            float[] dtState = new float[stateDim + 1];
            dtState[0] = t.returnToGo;
            System.arraycopy(pad(t.state, stateDim), 0, dtState, 1, stateDim);
            float[] h = lin(W1, b1, pad(dtState, stateDim), true);
            float[] logits = lin(W2, b2, h, false);
            float[] probs  = softmax(logits);
            int a = Math.min(t.action, actionDim - 1);
            loss += -(float) Math.log(Math.max(probs[a], 1e-8f));

            float[] dL = probs.clone(); dL[a] -= 1f;
            float[][] dW2 = outer(dL, h, hidDim, actionDim);
            polOpt.step("dt_W2", W2, dW2);
        }
        avgPolicyLoss = 0.99f * avgPolicyLoss + 0.01f * loss / batch.size();
        return loss / batch.size();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inference
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized int selectAction(float[] state) {
        float[] h = lin(W1, b1, pad(state, stateDim), true);
        float[] logits = lin(W2, b2, h, false);
        return argmax(logits);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private List<Transition> sampleBatch() {
        List<Transition> batch = new ArrayList<>(batchSize);
        for (int i = 0; i < batchSize; i++) batch.add(dataset.get(rng.nextInt(dataset.size())));
        return batch;
    }

    private float[] saInput(float[] s, int a) {
        float[] inp = new float[stateDim + actionDim];
        System.arraycopy(pad(s, stateDim), 0, inp, 0, stateDim);
        if (a >= 0 && a < actionDim) inp[stateDim + a] = 1f;
        return inp;
    }

    private float qForward(float[][] W1q, float[] b1q, float[][] W2q, float[] b2q, float[] inp) {
        return lin(W2q, b2q, lin(W1q, b1q, inp, true), false)[0];
    }

    private float vForward(float[] s) {
        return lin(VW2, Vb2, lin(VW1, Vb1, pad(s, stateDim), true), false)[0];
    }

    private int bestAction(float[] s) {
        float[] h = lin(W1, b1, pad(s, stateDim), true);
        return argmax(lin(W2, b2, h, false));
    }

    private float logSumExpQ(float[] s) {
        float[] qs = new float[actionDim];
        for (int a = 0; a < actionDim; a++) qs[a] = qForward(QW1a, Qb1a, QW2a, Qb2a, saInput(s, a));
        float mx = qs[0]; for (float q : qs) if (q > mx) mx = q;
        float sum = 0; for (float q : qs) sum += (float) Math.exp(q - mx);
        return (float) Math.log(sum) + mx;
    }

    private void simpleQBackprop(float[][] W1q, float[] b1q, float[][] W2q, float[] b2q,
                                  float[] inp, float err, String key) {
        float[] h = lin(W1q, b1q, inp, true);
        float[][] dW2 = new float[1][h.length];
        for (int j = 0; j < h.length; j++) dW2[0][j] = err * h[j];
        qOpt.step(key + "_W2", W2q, dW2);
        float[] dH = new float[h.length];
        for (int j = 0; j < h.length; j++) { if (h[j] <= 0) continue; dH[j] = err * W2q[0][j]; }
        float[][] dW1 = outer(dH, inp, inp.length, h.length);
        qOpt.step(key + "_W1", W1q, dW1);
    }

    private void simpleVBackprop(float[] s, float grad) {
        float[] h = lin(VW1, Vb1, pad(s, stateDim), true);
        float[][] dW2 = new float[1][h.length];
        for (int j = 0; j < h.length; j++) dW2[0][j] = grad * h[j];
        vOpt.step("v_W2", VW2, dW2);
        float[] dH = new float[h.length];
        for (int j = 0; j < h.length; j++) { if (h[j] <= 0) continue; dH[j] = grad * VW2[0][j]; }
        float[][] dW1 = outer(dH, pad(s, stateDim), stateDim, h.length);
        vOpt.step("v_W1", VW1, dW1);
    }

    private static float[] lin(float[][] W, float[] b, float[] x, boolean relu) {
        float[] o = new float[W.length];
        for (int i = 0; i < W.length; i++) {
            float s = b[i];
            for (int j = 0; j < Math.min(x.length, W[i].length); j++) s += W[i][j] * x[j];
            o[i] = relu ? Math.max(0f, s) : s;
        }
        return o;
    }

    private static float[] softmax(float[] v) {
        float mx = v[0]; for (float x : v) if (x > mx) mx = x;
        float sum = 0; float[] o = new float[v.length];
        for (int i=0;i<v.length;i++){o[i]=(float)Math.exp(v[i]-mx);sum+=o[i];}
        for (int i=0;i<v.length;i++) o[i]/=sum;
        return o;
    }

    private static int argmax(float[] v) {
        int b=0; for(int i=1;i<v.length;i++) if(v[i]>v[b]) b=i; return b;
    }

    private static float[][] outer(float[] a, float[] b, int bLen, int aLen) {
        float[][] m = new float[aLen][bLen];
        for (int i = 0; i < Math.min(aLen, a.length); i++)
            for (int j = 0; j < Math.min(bLen, b.length); j++) m[i][j] = a[i] * b[j];
        return m;
    }

    private static float[] pad(float[] x, int dim) {
        if (x.length == dim) return x;
        float[] p = new float[dim]; System.arraycopy(x, 0, p, 0, Math.min(x.length, dim));
        return p;
    }

    private float[][] xav(int r, int c, float s) {
        float[][] m = new float[r][c];
        for (int i=0;i<r;i++) for(int j=0;j<c;j++) m[i][j]=(rng.nextFloat()*2f-1f)*s;
        return m;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("algorithm",    algorithm.name());
        s.put("trainSteps",   trainSteps.get());
        s.put("datasetSize",  dataset.size());
        s.put("avgPolicyLoss",avgPolicyLoss);
        s.put("avgQLoss",     avgQLoss);
        s.put("avgVLoss",     avgVLoss);
        return s;
    }
}
