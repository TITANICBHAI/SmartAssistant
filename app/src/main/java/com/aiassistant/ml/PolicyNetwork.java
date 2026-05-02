package com.aiassistant.ml;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PolicyNetwork — 3-layer softmax policy network for actor-critic algorithms.
 *
 * Architecture:  state → [W1,b1,ReLU] → [W2,b2,ReLU] → [W3,b3] → softmax → π(a|s)
 *
 * Features:
 *   - Entropy regularisation: H(π) = -Σ π(a)·log π(a)  encourages exploration
 *   - Policy gradient update: ∇θ J = E[∇θ log π(a|s) · A(s,a)]
 *   - Manual backprop through softmax cross-entropy
 *   - Adam optimiser via NeuralNetworkOptimizer
 *   - Deterministic (greedy) mode for exploitation
 *   - Stochastic mode: sample action from π(a|s) for on-policy learning
 *   - Entropy coefficient scheduling: anneal from entropyStart→entropyEnd
 */
public class PolicyNetwork {

    private static final String TAG = "PolicyNetwork";

    // ── dimensions ────────────────────────────────────────────────────────────
    private final int stateDim;
    private final int hiddenDim;
    private final int actionDim;

    // ── weights ───────────────────────────────────────────────────────────────
    private final float[][] W1; // [hiddenDim][stateDim]
    private final float[]   B1; // [hiddenDim]
    private final float[][] W2; // [hiddenDim][hiddenDim]
    private final float[]   B2; // [hiddenDim]
    private final float[][] W3; // [actionDim][hiddenDim]
    private final float[]   B3; // [actionDim]

    // ── optimiser ─────────────────────────────────────────────────────────────
    private final NeuralNetworkOptimizer optimiser;

    // ── entropy annealing ─────────────────────────────────────────────────────
    private float entropyCoeff;
    private final float entropyMin;
    private final float entropyDecay;

    // ── stats ──────────────────────────────────────────────────────────────────
    private final AtomicInteger updateCount = new AtomicInteger(0);
    private float avgEntropy  = 0f;
    private float avgPgLoss   = 0f;

    private final Random rng = new Random(13L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public PolicyNetwork(int stateDim, int hiddenDim, int actionDim,
                         float lr, float entropyCoeff, float entropyMin, float entropyDecay) {
        this.stateDim     = stateDim;
        this.hiddenDim    = hiddenDim;
        this.actionDim    = actionDim;
        this.entropyCoeff = entropyCoeff;
        this.entropyMin   = entropyMin;
        this.entropyDecay = entropyDecay;
        this.optimiser    = new NeuralNetworkOptimizer(lr);

        float s1 = scale(stateDim,  hiddenDim);
        float s2 = scale(hiddenDim, hiddenDim);
        float s3 = scale(hiddenDim, actionDim);

        W1 = xavierMat(hiddenDim,  stateDim,  s1);  B1 = new float[hiddenDim];
        W2 = xavierMat(hiddenDim,  hiddenDim, s2);  B2 = new float[hiddenDim];
        W3 = xavierMat(actionDim,  hiddenDim, s3);  B3 = new float[actionDim];
    }

    public PolicyNetwork(int stateDim, int actionDim) {
        this(stateDim, 128, actionDim, 3e-4f, 0.01f, 1e-4f, 0.9999f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inference
    // ─────────────────────────────────────────────────────────────────────────

    /** Return the full action probability distribution π(a|s). */
    public synchronized float[] getProbs(float[] state) {
        float[] h1 = linear(W1, B1, state,  true);
        float[] h2 = linear(W2, B2, h1,     true);
        return softmax(linear(W3, B3, h2, false));
    }

    /** Sample an action from π(a|s) — stochastic (on-policy). */
    public synchronized int sampleAction(float[] state) {
        return categoricalSample(getProbs(state));
    }

    /** Return argmax_a π(a|s) — deterministic (exploitation). */
    public synchronized int greedyAction(float[] state) {
        float[] p = getProbs(state);
        int best = 0;
        for (int a = 1; a < actionDim; a++) if (p[a] > p[best]) best = a;
        return best;
    }

    /** Compute log π(a|s) for a specific action. */
    public synchronized float logProb(float[] state, int action) {
        float[] p = getProbs(state);
        return (float) Math.log(Math.max(p[action], 1e-8f));
    }

    /** Entropy H(π(·|s)) = -Σ_a π(a|s) log π(a|s). */
    public synchronized float entropy(float[] state) {
        float[] p = getProbs(state);
        float H = 0f;
        for (float pi : p) if (pi > 1e-10f) H -= pi * (float) Math.log(pi);
        return H;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Training — Policy Gradient
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * One policy-gradient step: ∇θ J = ∇θ log π(a|s) · advantage − entropyCoeff · ∇θ H(π)
     *
     * @param state      State at which action was taken.
     * @param action     Action taken.
     * @param advantage  Advantage estimate A(s,a) (can be GAE, TD, MC).
     * @return Policy gradient loss (negative because we ascend).
     */
    public synchronized float update(float[] state, int action, float advantage) {
        // ── Forward ──────────────────────────────────────────────────────────
        float[] h1   = linear(W1, B1, state, true);
        float[] h2   = linear(W2, B2, h1,    true);
        float[] logits = linear(W3, B3, h2,  false);
        float[] probs  = softmax(logits);

        float H   = 0f;
        for (float p : probs) if (p > 1e-10f) H -= p * (float) Math.log(p);

        float pgLoss = -(float) Math.log(Math.max(probs[action], 1e-8f)) * advantage;

        // ── Gradient of (pg_loss − entropyCoeff·H) w.r.t. logits ─────────────
        // d(pg)/d(logit_a) = probs[a] − 1{a==action} (from REINFORCE)
        // d(-H)/d(logit_a) = probs[a]·(log(probs[a]) + H)  (via softmax)
        float[] dLogits = new float[actionDim];
        for (int a = 0; a < actionDim; a++) {
            float dPg = (probs[a] - (a == action ? 1f : 0f)) * advantage;
            float dEnt = -entropyCoeff * probs[a] * ((float) Math.log(Math.max(probs[a], 1e-8f)) + H);
            dLogits[a] = dPg + dEnt;
        }

        // ── Backprop W3 ───────────────────────────────────────────────────────
        float[][] dW3 = new float[actionDim][hiddenDim];
        float[]   dB3 = new float[actionDim];
        for (int a = 0; a < actionDim; a++) {
            dB3[a] = dLogits[a];
            for (int j = 0; j < hiddenDim; j++) dW3[a][j] = dLogits[a] * h2[j];
        }

        // ── Backprop h2 ───────────────────────────────────────────────────────
        float[] dh2 = new float[hiddenDim];
        for (int j = 0; j < hiddenDim; j++) {
            if (h2[j] <= 0f) continue;
            for (int a = 0; a < actionDim; a++) dh2[j] += dLogits[a] * W3[a][j];
        }

        // ── Backprop W2 ───────────────────────────────────────────────────────
        float[][] dW2 = new float[hiddenDim][hiddenDim];
        float[]   dB2 = new float[hiddenDim];
        for (int j = 0; j < hiddenDim; j++) {
            dB2[j] = dh2[j];
            for (int k = 0; k < hiddenDim; k++) dW2[j][k] = dh2[j] * h1[k];
        }

        // ── Backprop h1 ───────────────────────────────────────────────────────
        float[] dh1 = new float[hiddenDim];
        for (int k = 0; k < hiddenDim; k++) {
            if (h1[k] <= 0f) continue;
            for (int j = 0; j < hiddenDim; j++) dh1[k] += dh2[j] * W2[j][k];
        }

        // ── Backprop W1 ───────────────────────────────────────────────────────
        float[][] dW1 = new float[hiddenDim][stateDim];
        float[]   dB1 = new float[hiddenDim];
        int sdim = Math.min(state.length, stateDim);
        for (int k = 0; k < hiddenDim; k++) {
            dB1[k] = dh1[k];
            for (int s = 0; s < sdim; s++) dW1[k][s] = dh1[k] * state[s];
        }

        // ── Adam steps ────────────────────────────────────────────────────────
        optimiser.step("pi_W3", W3, dW3);
        optimiser.step("pi_W2", W2, dW2);
        optimiser.step("pi_W1", W1, dW1);

        // ── Entropy annealing ─────────────────────────────────────────────────
        entropyCoeff = Math.max(entropyMin, entropyCoeff * entropyDecay);

        avgEntropy = 0.95f * avgEntropy + 0.05f * H;
        avgPgLoss  = 0.95f * avgPgLoss  + 0.05f * pgLoss;
        updateCount.incrementAndGet();
        return pgLoss;
    }

    /**
     * Batch update: average gradients over a mini-batch of (state, action, advantage) tuples.
     */
    public synchronized float batchUpdate(float[][] states, int[] actions, float[] advantages) {
        int n = Math.min(states.length, Math.min(actions.length, advantages.length));
        float totalLoss = 0f;
        for (int i = 0; i < n; i++) totalLoss += update(states[i], actions[i], advantages[i]);
        return n > 0 ? totalLoss / n : 0f;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("updateCount",  updateCount.get());
        s.put("avgEntropy",   avgEntropy);
        s.put("avgPgLoss",    avgPgLoss);
        s.put("entropyCoeff", entropyCoeff);
        s.put("stateDim",     stateDim);
        s.put("hiddenDim",    hiddenDim);
        s.put("actionDim",    actionDim);
        return s;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private float[] linear(float[][] W, float[] b, float[] inp, boolean relu) {
        float[] out = new float[W.length];
        for (int i = 0; i < W.length; i++) {
            float sum = b[i];
            int dim = Math.min(inp.length, W[i].length);
            for (int j = 0; j < dim; j++) sum += W[i][j] * inp[j];
            out[i] = relu ? Math.max(0f, sum) : sum;
        }
        return out;
    }

    private static float[] softmax(float[] v) {
        float max = v[0]; for (float x : v) if (x > max) max = x;
        float sum = 0f;
        float[] out = new float[v.length];
        for (int i = 0; i < v.length; i++) { out[i] = (float) Math.exp(v[i] - max); sum += out[i]; }
        for (int i = 0; i < v.length; i++) out[i] /= sum;
        return out;
    }

    private int categoricalSample(float[] probs) {
        float r = rng.nextFloat(), cum = 0f;
        for (int i = 0; i < probs.length - 1; i++) { cum += probs[i]; if (r < cum) return i; }
        return probs.length - 1;
    }

    private static float scale(int in, int out) { return (float) Math.sqrt(2.0 / (in + out)); }

    private float[][] xavierMat(int rows, int cols, float s) {
        float[][] m = new float[rows][cols];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++) m[i][j] = (rng.nextFloat() * 2f - 1f) * s;
        return m;
    }
}
