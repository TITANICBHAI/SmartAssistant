package com.aiassistant.ml;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * NoveltyDetector — multi-method novelty / surprise detection for RL exploration.
 *
 * Combines four complementary novelty signals into a single weighted score:
 *
 *   1. K-NEAREST NEIGHBOUR DISTANCE
 *      Novelty = average L2 distance to the k nearest states in a sliding-window buffer.
 *      High distance → rare region of state space.
 *
 *   2. RANDOM NETWORK DISTILLATION (RND)
 *      Target network φ_target (frozen), predictor φ_pred (trained).
 *      Novelty = ||φ_pred(s) - φ_target(s)||²
 *      Prediction error is high for unseen states.
 *
 *   3. HASH COUNT SURPRISE
 *      Novelty = 1 / log(N(s) + e)   (information-theoretic surprise)
 *
 *   4. PREDICTION ERROR DELTA
 *      Novelty = |model.error(s) - avg_error|
 *      Unusual states have prediction error far from the mean.
 *
 * Combined score = Σ_i w_i · normalize_i(signal_i)
 *
 * Thread-safe.
 */
public class NoveltyDetector {

    private static final String TAG = "NoveltyDetector";

    // ─────────────────────────────────────────────────────────────────────────
    // Config
    // ─────────────────────────────────────────────────────────────────────────
    private final int   stateDim;
    private final int   knn;           // k for KNN distance
    private final int   bufferSize;    // rolling state buffer for KNN
    private final int   rndDim;        // RND embedding dimension
    private final float rndLr;         // RND predictor LR

    private final float wKnn;  // weight for KNN
    private final float wRnd;  // weight for RND
    private final float wHash; // weight for hash count
    private final float wErr;  // weight for model error delta

    // ─────────────────────────────────────────────────────────────────────────
    // KNN buffer
    // ─────────────────────────────────────────────────────────────────────────
    private final float[][] knnBuffer;
    private int    knnHead  = 0;
    private int    knnCount = 0;

    // ─────────────────────────────────────────────────────────────────────────
    // RND networks: target (frozen) and predictor (trained)
    // ─────────────────────────────────────────────────────────────────────────
    private final float[][] targetW1, targetW2; // [rndDim][stateDim], [rndDim][rndDim]
    private final float[]   targetB1, targetB2;
    private final float[][] predW1,   predW2;
    private final float[]   predB1,   predB2;
    private final NeuralNetworkOptimizer rndOpt;

    // ─────────────────────────────────────────────────────────────────────────
    // Hash count
    // ─────────────────────────────────────────────────────────────────────────
    private final HashMap<Long, Integer> hashCount = new HashMap<>();

    // ─────────────────────────────────────────────────────────────────────────
    // Model error tracking
    // ─────────────────────────────────────────────────────────────────────────
    private float avgModelError = 0f;

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────
    private final AtomicInteger visitCount  = new AtomicInteger(0);
    private float avgNovelty = 0f;
    private float maxNovelty = 0f;

    // Running normalisation (Welford) for each signal
    private final double[] sigMean = new double[4];
    private final double[] sigM2   = new double[4];
    private long sigN = 0;

    private final Random rng = new Random(127L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public NoveltyDetector(int stateDim, int knn, int bufferSize, int rndDim, float rndLr,
                            float wKnn, float wRnd, float wHash, float wErr) {
        this.stateDim   = stateDim;
        this.knn        = knn;
        this.bufferSize = bufferSize;
        this.rndDim     = rndDim;
        this.rndLr      = rndLr;
        this.wKnn       = wKnn;
        this.wRnd       = wRnd;
        this.wHash      = wHash;
        this.wErr       = wErr;

        knnBuffer = new float[bufferSize][stateDim];
        rndOpt    = new NeuralNetworkOptimizer(rndLr);

        float s = (float) Math.sqrt(2.0 / (stateDim + rndDim));
        targetW1 = xav(rndDim, stateDim, s); targetB1 = new float[rndDim];
        targetW2 = xav(rndDim, rndDim,   s); targetB2 = new float[rndDim];
        predW1   = xav(rndDim, stateDim, s); predB1   = new float[rndDim];
        predW2   = xav(rndDim, rndDim,   s); predB2   = new float[rndDim];

        Log.i(TAG, "NoveltyDetector: state=" + stateDim + " RND=" + rndDim
                + " KNN=" + knn + " buf=" + bufferSize);
    }

    public NoveltyDetector(int stateDim) {
        this(stateDim, 10, 1000, 64, 1e-3f, 0.25f, 0.35f, 0.20f, 0.20f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Compute novelty score for state, update internal models.
     * @param modelError Optional prediction error from world model (pass 0 if unavailable).
     */
    public synchronized float novelty(float[] state, float modelError) {
        float[] s = pad(state, stateDim);
        visitCount.incrementAndGet();

        // ── Signal 1: KNN distance ─────────────────────────────────────────
        float knnDist = knnDistance(s);

        // ── Signal 2: RND error ────────────────────────────────────────────
        float rndErr = rndError(s);
        updateRnd(s, rndErr);

        // ── Signal 3: hash count surprise ─────────────────────────────────
        long key = simHash(s);
        int  n   = hashCount.merge(key, 1, Integer::sum);
        float hashSurprise = 1f / (float) Math.log(n + Math.E);

        // ── Signal 4: model error delta ────────────────────────────────────
        avgModelError = 0.99f * avgModelError + 0.01f * modelError;
        float errDelta = Math.abs(modelError - avgModelError);

        // ── Update KNN buffer ──────────────────────────────────────────────
        knnBuffer[knnHead % bufferSize] = s.clone();
        knnHead++; if (knnCount < bufferSize) knnCount++;

        // ── Normalise signals via Welford ──────────────────────────────────
        float[] raw = {knnDist, rndErr, hashSurprise, errDelta};
        updateSigStats(raw);
        float[] norm = normaliseSigs(raw);

        // ── Combine ────────────────────────────────────────────────────────
        float score = wKnn * norm[0] + wRnd * norm[1] + wHash * norm[2] + wErr * norm[3];
        avgNovelty = 0.99f * avgNovelty + 0.01f * score;
        if (score > maxNovelty) maxNovelty = score;
        return score;
    }

    public synchronized float novelty(float[] state) {
        return novelty(state, 0f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal
    // ─────────────────────────────────────────────────────────────────────────

    private float knnDistance(float[] s) {
        if (knnCount == 0) return 1f;
        // Partial heap for k smallest
        float[] heap = new float[knn];
        java.util.Arrays.fill(heap, Float.MAX_VALUE);
        for (int i = 0; i < knnCount; i++) {
            float d = l2sq(s, knnBuffer[i % bufferSize]);
            if (d < heap[0]) { heap[0] = d; heapify(heap); }
        }
        float sum = 0; for (float h : heap) if (h < Float.MAX_VALUE) sum += (float) Math.sqrt(h);
        return sum / knn;
    }

    private float rndError(float[] s) {
        float[] t = rndForward(targetW1, targetB1, targetW2, targetB2, s);
        float[] p = rndForward(predW1,   predB1,   predW2,   predB2,   s);
        float err = 0;
        for (int i = 0; i < rndDim; i++) { float d = t[i] - p[i]; err += d * d; }
        return err / rndDim;
    }

    private void updateRnd(float[] s, float err) {
        // One step of gradient descent: minimise ||pred(s) - target(s)||²
        float[] t  = rndForward(targetW1, targetB1, targetW2, targetB2, s);
        float[] h1 = lin(predW1, predB1, s, true);
        float[] p  = lin(predW2, predB2, h1, false);
        float[] dP = new float[rndDim];
        for (int i = 0; i < rndDim; i++) dP[i] = 2f * (p[i] - t[i]);

        float[][] dW2 = new float[rndDim][rndDim];
        for (int i = 0; i < rndDim; i++)
            for (int j = 0; j < rndDim; j++) dW2[i][j] = dP[i] * h1[j];
        rndOpt.step("rnd_W2", predW2, dW2);

        float[] dH1 = new float[rndDim];
        for (int j = 0; j < rndDim; j++) {
            if (h1[j] <= 0) continue;
            for (int i = 0; i < rndDim; i++) dH1[j] += dP[i] * predW2[i][j];
        }
        float[][] dW1 = new float[rndDim][stateDim];
        for (int i = 0; i < rndDim; i++)
            for (int j = 0; j < Math.min(s.length, stateDim); j++) dW1[i][j] = dH1[i] * s[j];
        rndOpt.step("rnd_W1", predW1, dW1);
    }

    private float[] rndForward(float[][] W1, float[] b1, float[][] W2, float[] b2, float[] s) {
        return lin(W2, b2, lin(W1, b1, s, true), false);
    }

    private long simHash(float[] s) {
        long h = 0;
        for (int i = 0; i < Math.min(s.length, 32); i++) {
            int b = (int)(s[i] * 8f) & 0xFF;
            h = h * 31L + b;
        }
        return h;
    }

    private void updateSigStats(float[] raw) {
        sigN++;
        for (int i = 0; i < 4; i++) {
            double delta = raw[i] - sigMean[i];
            sigMean[i] += delta / sigN;
            sigM2[i]   += delta * (raw[i] - sigMean[i]);
        }
    }

    private float[] normaliseSigs(float[] raw) {
        float[] norm = new float[4];
        for (int i = 0; i < 4; i++) {
            double std = sigN < 2 ? 1.0 : Math.sqrt(sigM2[i] / (sigN - 1));
            norm[i] = std < 1e-8 ? 0f : (float)((raw[i] - sigMean[i]) / std);
            norm[i] = Math.max(0f, norm[i]);  // clip to non-negative
        }
        return norm;
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

    private static float l2sq(float[] a, float[] b) {
        float s = 0f;
        for (int i = 0; i < a.length; i++) { float d = a[i] - b[i]; s += d * d; }
        return s;
    }

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

    private static void heapify(float[] heap) {
        // Max-heap of size k (keep k smallest)
        int n = heap.length;
        for (int i = n / 2 - 1; i >= 0; i--) {
            int p = i;
            while (true) {
                int l = 2*p+1, r = 2*p+2, largest = p;
                if (l < n && heap[l] > heap[largest]) largest = l;
                if (r < n && heap[r] > heap[largest]) largest = r;
                if (largest == p) break;
                float t = heap[p]; heap[p] = heap[largest]; heap[largest] = t;
                p = largest;
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("visitCount",    visitCount.get());
        s.put("uniqueStates",  hashCount.size());
        s.put("avgNovelty",    avgNovelty);
        s.put("maxNovelty",    maxNovelty);
        s.put("knnBufFill",    knnCount);
        s.put("avgModelError", avgModelError);
        return s;
    }
}
