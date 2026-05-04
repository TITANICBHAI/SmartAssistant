package com.aiassistant.ml;

import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DualNetworkDQN — Double DQN + Dueling Networks (Wang et al. 2016).
 *
 * Combines two key improvements over vanilla DQN:
 *
 *   DOUBLE DQN (van Hasselt et al. 2016):
 *     Q_target(s,a) = r + γ · Q_target(s', argmax_{a'} Q_online(s', a'))
 *     Decouples action selection from evaluation, reducing overestimation bias.
 *
 *   DUELING ARCHITECTURE:
 *     Q(s,a) = V(s) + A(s,a) - mean_a A(s,a)
 *     Separate streams for state value V(s) and action advantage A(s,a).
 *     Stabilises learning by factoring out state-independent information.
 *
 * Architecture:
 *   Shared trunk:  state → [W1, ReLU] → [W2, ReLU] → embedding (hidDim)
 *   Value stream:  embedding → [Wv, ReLU] → [Wv2] → scalar V(s)
 *   Advantage:     embedding → [Wa, ReLU] → [Wa2] → vector A(s,a) [actionDim]
 *   Q(s,a) = V(s) + A(s,a) - mean(A)
 *
 * Thread-safe.
 */
public class DualNetworkDQN {

    private static final String TAG = "DualNetworkDQN";

    // ─────────────────────────────────────────────────────────────────────────
    // Network parameters (one copy = online; two copies = +target)
    // ─────────────────────────────────────────────────────────────────────────
    private static class Net {
        // Shared trunk
        float[][] W1, W2;      // [hidDim][stateDim], [hidDim][hidDim]
        float[]   b1, b2;
        // Value stream
        float[][] Wv1, Wv2;    // [valDim][hidDim], [1][valDim]
        float[]   bv1, bv2;
        // Advantage stream
        float[][] Wa1, Wa2;    // [advDim][hidDim], [actionDim][advDim]
        float[]   ba1, ba2;

        Net(int stateDim, int hidDim, int actionDim, int valDim, int advDim, Random rng) {
            float s = (float) Math.sqrt(2.0 / (stateDim + hidDim));
            W1 = xav(hidDim, stateDim, s, rng); b1 = new float[hidDim];
            W2 = xav(hidDim, hidDim,   s, rng); b2 = new float[hidDim];
            Wv1= xav(valDim, hidDim,   s, rng); bv1= new float[valDim];
            Wv2= xav(1,      valDim,   s, rng); bv2= new float[1];
            Wa1= xav(advDim, hidDim,   s, rng); ba1= new float[advDim];
            Wa2= xav(actionDim, advDim, s, rng);ba2= new float[actionDim];
        }

        float[] forward(float[] state) {
            float[] h1  = lin(W1, b1, state, true);
            float[] h2  = lin(W2, b2, h1,   true);
            float   V   = lin(Wv2, bv2, lin(Wv1, bv1, h2, true), false)[0];
            float[] A   = lin(Wa2, ba2, lin(Wa1, ba1, h2, true), false);
            float   mA  = 0; for (float a : A) mA += a; mA /= A.length;
            float[] Q   = new float[A.length];
            for (int i = 0; i < A.length; i++) Q[i] = V + A[i] - mA;
            return Q;
        }

        static float[] lin(float[][] W, float[] b, float[] x, boolean relu) {
            float[] o = new float[W.length];
            for (int i = 0; i < W.length; i++) {
                float s = b[i];
                for (int j = 0; j < Math.min(x.length, W[i].length); j++) s += W[i][j] * x[j];
                o[i] = relu ? Math.max(0f, s) : s;
            }
            return o;
        }

        static float[][] xav(int r, int c, float s, Random rng) {
            float[][] m = new float[r][c];
            for (int i=0;i<r;i++) for(int j=0;j<c;j++) m[i][j]=(rng.nextFloat()*2f-1f)*s;
            return m;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final Net   online, target;
    private final int   stateDim, actionDim, hidDim;
    private final float gamma, lr;
    private final int   targetUpdateFreq;
    private final float epsilonStart, epsilonEnd, epsilonDecay;
    private       float epsilon;
    private final NeuralNetworkOptimizer opt;

    private final AtomicInteger stepCount   = new AtomicInteger(0);
    private final AtomicInteger updateCount = new AtomicInteger(0);
    private float avgLoss = 0f;
    private float avgQ    = 0f;

    private final Random rng;

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public DualNetworkDQN(int stateDim, int actionDim, int hidDim,
                          float gamma, float lr, int targetUpdateFreq,
                          float epsilonStart, float epsilonEnd, float epsilonDecay) {
        this.stateDim       = stateDim;
        this.actionDim      = actionDim;
        this.hidDim         = hidDim;
        this.gamma          = gamma;
        this.lr             = lr;
        this.targetUpdateFreq = targetUpdateFreq;
        this.epsilonStart   = epsilonStart;
        this.epsilonEnd     = epsilonEnd;
        this.epsilonDecay   = epsilonDecay;
        this.epsilon        = epsilonStart;
        this.opt            = new NeuralNetworkOptimizer(lr);
        this.rng            = new Random(191L);

        int valDim = hidDim / 2, advDim = hidDim / 2;
        online = new Net(stateDim, hidDim, actionDim, valDim, advDim, rng);
        target = new Net(stateDim, hidDim, actionDim, valDim, advDim, rng);
        syncTarget();

        Log.i(TAG, "DualNetworkDQN: s=" + stateDim + " a=" + actionDim
                + " h=" + hidDim + " γ=" + gamma);
    }

    public DualNetworkDQN(int stateDim, int actionDim) {
        this(stateDim, actionDim, 128, 0.99f, 1e-3f, 500, 1.0f, 0.05f, 0.995f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Action selection
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized int selectAction(float[] state) {
        epsilon = Math.max(epsilonEnd, epsilon * epsilonDecay);
        if (rng.nextFloat() < epsilon) return rng.nextInt(actionDim);
        float[] Q = online.forward(pad(state, stateDim));
        int best = 0; for (int a = 1; a < actionDim; a++) if (Q[a] > Q[best]) best = a;
        avgQ = 0.99f * avgQ + 0.01f * Q[best];
        stepCount.incrementAndGet();
        return best;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Training
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized float update(float[] state, int action, float reward,
                                     float[] nextState, boolean done) {
        float[] s  = pad(state, stateDim);
        float[] sp = pad(nextState, stateDim);

        // DOUBLE DQN target:
        // a* = argmax_a Q_online(s', a)
        float[] onlineQN = online.forward(sp);
        int aStar = 0; for (int a = 1; a < actionDim; a++) if (onlineQN[a] > onlineQN[aStar]) aStar = a;
        float[] targetQN = target.forward(sp);
        float td_target  = reward + (done ? 0f : gamma * targetQN[aStar]);

        // Online Q(s,a)
        float[] Q    = online.forward(s);
        float   tdErr= td_target - Q[action];

        // Backprop through dueling heads
        // Simplified: treat as plain 2-layer + Q output, gradient via chain rule
        backpropDueling(s, action, tdErr);
        float loss = tdErr * tdErr;
        avgLoss = 0.99f * avgLoss + 0.01f * loss;
        updateCount.incrementAndGet();

        // Periodic target sync
        if (updateCount.get() % targetUpdateFreq == 0) syncTarget();
        return loss;
    }

    /** Batch update on a list of experiences. */
    public synchronized float batchUpdate(List<ReplayBuffer.Experience> batch) {
        float total = 0;
        for (ReplayBuffer.Experience e : batch)
            total += update(e.state, e.action, e.reward, e.nextState, e.done);
        return batch.isEmpty() ? 0f : total / batch.size();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal
    // ─────────────────────────────────────────────────────────────────────────

    private void backpropDueling(float[] s, int action, float tdErr) {
        // Shared trunk forward
        float[] h1 = Net.lin(online.W1, online.b1, s, true);
        float[] h2 = Net.lin(online.W2, online.b2, h1, true);

        // Value stream
        float[] hv = Net.lin(online.Wv1, online.bv1, h2, true);
        // Advantage stream
        float[] ha = Net.lin(online.Wa1, online.ba1, h2, true);
        float[] A  = Net.lin(online.Wa2, online.ba2, ha, false);
        float   mA = 0; for (float a : A) mA += a; mA /= actionDim;

        // dQ/dA[action] = 1 - 1/n;  dQ/dA[k≠action] = -1/n;  dQ/dV = 1
        float[] dA = new float[actionDim];
        for (int a = 0; a < actionDim; a++)
            dA[a] = -tdErr * (a == action ? (1f - 1f/actionDim) : (-1f/actionDim));
        float dV = -tdErr;

        // Backprop advantage head
        float[][] dWa2 = new float[actionDim][online.Wa1.length];
        for (int i = 0; i < actionDim; i++)
            for (int j = 0; j < ha.length; j++) dWa2[i][j] = dA[i] * ha[j];
        opt.step("dqn_Wa2", online.Wa2, dWa2);

        float[] dHa = new float[ha.length];
        for (int j = 0; j < ha.length; j++) {
            if (ha[j] <= 0) continue;
            for (int i = 0; i < actionDim; i++) dHa[j] += dA[i] * online.Wa2[i][j];
        }
        float[][] dWa1 = new float[online.Wa1.length][h2.length];
        for (int i = 0; i < online.Wa1.length; i++)
            for (int j = 0; j < h2.length; j++) dWa1[i][j] = dHa[i] * h2[j];
        opt.step("dqn_Wa1", online.Wa1, dWa1);

        // Backprop value head
        float[][] dWv2 = new float[1][hv.length];
        for (int j = 0; j < hv.length; j++) dWv2[0][j] = dV * hv[j];
        opt.step("dqn_Wv2", online.Wv2, dWv2);

        float[] dHv = new float[hv.length];
        for (int j = 0; j < hv.length; j++) {
            if (hv[j] <= 0) continue;
            dHv[j] = dV * online.Wv2[0][j];
        }
        float[][] dWv1 = new float[online.Wv1.length][h2.length];
        for (int i = 0; i < online.Wv1.length; i++)
            for (int j = 0; j < h2.length; j++) dWv1[i][j] = dHv[i] * h2[j];
        opt.step("dqn_Wv1", online.Wv1, dWv1);

        // Aggregate gradient into h2
        float[] dH2 = new float[h2.length];
        for (int j = 0; j < h2.length; j++) {
            if (h2[j] <= 0) continue;
            for (int k = 0; k < online.Wa1.length; k++) dH2[j] += dHa[k] * online.Wa1[k][j];
            for (int k = 0; k < online.Wv1.length; k++) dH2[j] += dHv[k] * online.Wv1[k][j];
        }
        float[][] dW2 = new float[h2.length][h1.length];
        for (int i = 0; i < h2.length; i++)
            for (int j = 0; j < h1.length; j++) dW2[i][j] = dH2[i] * h1[j];
        opt.step("dqn_W2", online.W2, dW2);

        float[] dH1 = new float[h1.length];
        for (int j = 0; j < h1.length; j++) {
            if (h1[j] <= 0) continue;
            for (int k = 0; k < h2.length; k++) dH1[j] += dH2[k] * online.W2[k][j];
        }
        float[][] dW1 = new float[h1.length][s.length];
        for (int i = 0; i < h1.length; i++)
            for (int j = 0; j < s.length; j++) dW1[i][j] = dH1[i] * s[j];
        opt.step("dqn_W1", online.W1, dW1);
    }

    private void syncTarget() {
        copyNet(online, target);
        Log.d(TAG, "Target network synced at step " + updateCount.get());
    }

    private static void copyNet(Net src, Net dst) {
        copyMat(src.W1, dst.W1); copyMat(src.W2, dst.W2);
        copyMat(src.Wv1,dst.Wv1);copyMat(src.Wv2,dst.Wv2);
        copyMat(src.Wa1,dst.Wa1);copyMat(src.Wa2,dst.Wa2);
    }

    private static void copyMat(float[][] src, float[][] dst) {
        for (int i = 0; i < Math.min(src.length, dst.length); i++)
            System.arraycopy(src[i], 0, dst[i], 0, Math.min(src[i].length, dst[i].length));
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
        s.put("stepCount",   stepCount.get());
        s.put("updateCount", updateCount.get());
        s.put("epsilon",     epsilon);
        s.put("avgLoss",     avgLoss);
        s.put("avgQ",        avgQ);
        s.put("gamma",       gamma);
        return s;
    }
}
