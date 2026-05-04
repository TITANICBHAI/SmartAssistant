package com.aiassistant.ml;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AttentionMechanism — scaled dot-product and multi-head self-attention for RL.
 *
 * Enables the agent to selectively focus on relevant parts of:
 *   - Multi-modal observations (screen regions, event history, state features)
 *   - Sequence of past states (temporal attention / sliding window)
 *   - Action history context
 *
 * Implements three attention variants:
 *
 *   SCALED_DOT      — Attention(Q,K,V) = softmax(Q·K^T / √d_k) · V
 *   MULTI_HEAD      — Concat of H parallel dot-product attention heads
 *   ADDITIVE        — Bahdanau: score = v^T tanh(W_1·q + W_2·k)
 *
 * For RL use:
 *   - Each past observation is a "token"
 *   - Query = current state embedding
 *   - Keys/Values = past state embeddings
 *   - Output = weighted aggregation of past information
 *
 * All variants are implemented without autograd: forward only (for inference),
 * with an optional supervised update via mean-attention regularisation.
 *
 * Thread-safe.
 */
public class AttentionMechanism {

    private static final String TAG = "AttentionMechanism";

    public enum AttentionType { SCALED_DOT, MULTI_HEAD, ADDITIVE }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int         dModel;    // input/output dimension
    private final int         dKey;      // key/query dimension
    private final int         dValue;    // value dimension
    private final int         numHeads;
    private final AttentionType type;

    // Projection matrices per head (for MULTI_HEAD)
    private final float[][][] Wq, Wk, Wv;   // [numHeads][dKey][dModel]
    private final float[][]   Wo;            // [dModel][numHeads*dValue]

    // Additive attention weights
    private final float[][] addW1, addW2;    // [dKey][dModel]
    private final float[]   addV;            // [dKey]

    // Stats
    private final AtomicInteger forwardCount = new AtomicInteger(0);
    private float avgMaxAttn = 0f;  // how focused attention is (max weight)
    private float avgEntropy = 0f;  // entropy of attention weights

    private final Random rng = new Random(181L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public AttentionMechanism(int dModel, int dKey, int dValue,
                               int numHeads, AttentionType type) {
        this.dModel   = dModel;
        this.dKey     = dKey;
        this.dValue   = dValue;
        this.numHeads = numHeads;
        this.type     = type;

        float s = (float) Math.sqrt(2.0 / (dModel + dKey));
        Wq = new float[numHeads][dKey][dModel];
        Wk = new float[numHeads][dKey][dModel];
        Wv = new float[numHeads][dValue][dModel];
        Wo = new float[dModel][numHeads * dValue];
        for (int h = 0; h < numHeads; h++) {
            xavInit(Wq[h], s); xavInit(Wk[h], s); xavInit(Wv[h], s);
        }
        xavInit(Wo, s);

        addW1 = new float[dKey][dModel]; xavInit(addW1, s);
        addW2 = new float[dKey][dModel]; xavInit(addW2, s);
        addV  = new float[dKey];

        Log.i(TAG, "AttentionMechanism: dModel=" + dModel + " heads=" + numHeads + " type=" + type);
    }

    public AttentionMechanism(int dModel, int numHeads) {
        this(dModel, dModel / numHeads, dModel / numHeads, numHeads, AttentionType.MULTI_HEAD);
    }

    public AttentionMechanism(int dModel) {
        this(dModel, 1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Attention forward pass
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Compute attention output given a query and sequence of key-value pairs.
     *
     * @param query  Query vector [dModel].
     * @param keys   Key matrix [seqLen][dModel].
     * @param values Value matrix [seqLen][dModel].
     * @return Attended output [dModel].
     */
    public synchronized float[] attend(float[] query, float[][] keys, float[][] values) {
        int seqLen = keys.length;
        if (seqLen == 0) return new float[dModel];

        float[] out;
        switch (type) {
            case ADDITIVE:   out = additiveAttend(query, keys, values);  break;
            case SCALED_DOT: out = scaledDotAttend(query, keys, values, 0); break;
            case MULTI_HEAD:
            default:         out = multiHeadAttend(query, keys, values); break;
        }

        forwardCount.incrementAndGet();
        return out;
    }

    /**
     * Self-attention over a sequence of vectors.
     * @return float[seqLen][dModel] — attended representation for each position.
     */
    public synchronized float[][] selfAttend(float[][] sequence) {
        float[][] out = new float[sequence.length][];
        for (int i = 0; i < sequence.length; i++) {
            out[i] = attend(sequence[i], sequence, sequence);
        }
        return out;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Attention implementations
    // ─────────────────────────────────────────────────────────────────────────

    private float[] scaledDotAttend(float[] query, float[][] keys, float[][] values, int head) {
        int seqLen = keys.length;
        float scale = 1f / (float) Math.sqrt(dKey);

        float[][] Wqh = head < numHeads ? Wq[head] : Wq[0];
        float[][] Wkh = head < numHeads ? Wk[head] : Wk[0];
        float[][] Wvh = head < numHeads ? Wv[head] : Wv[0];

        float[] q = project(Wqh, query);
        float[] scores = new float[seqLen];
        float maxScore = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < seqLen; i++) {
            float[] k = project(Wkh, keys[i]);
            scores[i] = dot(q, k) * scale;
            if (scores[i] > maxScore) maxScore = scores[i];
        }
        float[] attn = softmax(scores, maxScore);
        updateAttnStats(attn);

        float[] out = new float[dValue];
        for (int i = 0; i < seqLen; i++) {
            float[] v = project(Wvh, values[i]);
            for (int j = 0; j < Math.min(dValue, v.length); j++) out[j] += attn[i] * v[j];
        }
        return out;
    }

    private float[] multiHeadAttend(float[] query, float[][] keys, float[][] values) {
        int seqLen = keys.length;
        float[] concat = new float[numHeads * dValue];
        for (int h = 0; h < numHeads; h++) {
            float[] headOut = scaledDotAttend(query, keys, values, h);
            System.arraycopy(headOut, 0, concat, h * dValue, dValue);
        }
        // Project: Wo [dModel][numHeads*dValue]
        return project(Wo, concat);
    }

    private float[] additiveAttend(float[] query, float[][] keys, float[][] values) {
        int seqLen = keys.length;
        float[] scores = new float[seqLen];
        float maxScore = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < seqLen; i++) {
            float[] w1q = project(addW1, query);
            float[] w2k = project(addW2, keys[i]);
            float score = 0;
            for (int j = 0; j < dKey; j++) score += addV[j] * (float) Math.tanh(w1q[j] + w2k[j]);
            scores[i] = score;
            if (score > maxScore) maxScore = score;
        }
        float[] attn = softmax(scores, maxScore);
        updateAttnStats(attn);

        float[] out = new float[dModel];
        for (int i = 0; i < seqLen; i++) {
            float[] v = values[i];
            for (int j = 0; j < Math.min(dModel, v.length); j++) out[j] += attn[i] * v[j];
        }
        return out;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static float[] project(float[][] W, float[] x) {
        float[] o = new float[W.length];
        for (int i = 0; i < W.length; i++)
            for (int j = 0; j < Math.min(x.length, W[i].length); j++) o[i] += W[i][j] * x[j];
        return o;
    }

    private static float dot(float[] a, float[] b) {
        float s = 0; for (int i = 0; i < Math.min(a.length, b.length); i++) s += a[i]*b[i];
        return s;
    }

    private static float[] softmax(float[] v, float max) {
        float sum = 0; float[] o = new float[v.length];
        for (int i = 0; i < v.length; i++) { o[i] = (float) Math.exp(v[i] - max); sum += o[i]; }
        for (int i = 0; i < v.length; i++) o[i] /= sum;
        return o;
    }

    private void updateAttnStats(float[] attn) {
        float max = 0, ent = 0;
        for (float a : attn) {
            if (a > max) max = a;
            if (a > 1e-8f) ent -= a * (float) Math.log(a);
        }
        avgMaxAttn = 0.99f * avgMaxAttn + 0.01f * max;
        avgEntropy = 0.99f * avgEntropy + 0.01f * ent;
    }

    private void xavInit(float[][] m, float s) {
        for (float[] row : m) for (int j = 0; j < row.length; j++) row[j] = (rng.nextFloat()*2f-1f)*s;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("type",         type.name());
        s.put("dModel",       dModel);
        s.put("numHeads",     numHeads);
        s.put("forwardCount", forwardCount.get());
        s.put("avgMaxAttn",   avgMaxAttn);
        s.put("avgEntropy",   avgEntropy);
        return s;
    }
}
