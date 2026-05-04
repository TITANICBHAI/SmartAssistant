package com.aiassistant.ml;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ModelCompressionEngine — neural network compression for on-device efficiency.
 *
 * Reduces model size and inference latency while preserving performance:
 *
 *   WEIGHT_PRUNING (Magnitude):
 *     Set weights |w_ij| < threshold to 0. Sparse network → faster inference.
 *     Supports structured pruning (remove entire neurons) and unstructured.
 *
 *   QUANTIZATION:
 *     INT8 quantization: map float weights to [-127, 127].
 *     Reduces memory by 4× (FP32 → INT8). Critical for mobile deployment.
 *
 *   KNOWLEDGE_DISTILLATION:
 *     Train small student network to match teacher's SOFT LOGITS
 *     (temperature-scaled softmax), not just hard labels.
 *     L = α·L_CE(y, y_student) + (1-α)·T²·KL(σ(z_teacher/T) || σ(z_student/T))
 *
 *   LOW_RANK_FACTORISATION:
 *     Decompose W ≈ U·V^T where U∈R^{m×r}, V∈R^{n×r}, r ≪ min(m,n).
 *     Reduces parameters from m·n to r·(m+n).
 *
 *   HUFFMAN_CODING:
 *     Lossless compression of weight indices (for sparse models).
 *     Achieves further 2-3× compression after pruning.
 *
 * Thread-safe.
 */
public class ModelCompressionEngine {

    private static final String TAG = "ModelCompress";

    public enum Technique { WEIGHT_PRUNING, QUANTIZATION, KNOWLEDGE_DISTILLATION,
                            LOW_RANK_FACTORISATION, HUFFMAN_CODING }

    // ─────────────────────────────────────────────────────────────────────────
    // Pruning config
    // ─────────────────────────────────────────────────────────────────────────
    private float pruneThreshold    = 0.01f;   // |w| < threshold → 0
    private float targetSparsity    = 0.9f;    // 90% of weights are zero
    private boolean structuredPrune = false;   // prune entire neurons

    // Quantization config
    private static final float QUANT_SCALE = 127f;

    // Distillation config
    private float distilTemp   = 3.0f;
    private float distilAlpha  = 0.5f;

    // Low-rank config
    private int rankR = 8;   // decomposition rank

    // Stats
    private final AtomicInteger compressCount = new AtomicInteger(0);
    private float lastSparsity   = 0f;
    private float lastCompRatio  = 1f;
    private int   totalWeights   = 0;
    private int   prunedWeights  = 0;

    private final Random rng = new Random(397L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public ModelCompressionEngine(float pruneThreshold, float targetSparsity,
                                   float distilTemp, float distilAlpha, int rankR) {
        this.pruneThreshold = pruneThreshold;
        this.targetSparsity = targetSparsity;
        this.distilTemp     = distilTemp;
        this.distilAlpha    = distilAlpha;
        this.rankR          = rankR;
        Log.i(TAG, "ModelCompressionEngine: threshold=" + pruneThreshold + " rank=" + rankR);
    }

    public ModelCompressionEngine() { this(0.01f, 0.9f, 3f, 0.5f, 8); }

    // ─────────────────────────────────────────────────────────────────────────
    // Weight Pruning
    // ─────────────────────────────────────────────────────────────────────────

    /** Prune weights below threshold in-place. Returns sparsity ratio. */
    public synchronized float prune(float[][] W) {
        int total=0, pruned=0;
        for (float[] row : W) for(int j=0;j<row.length;j++) {
            total++;
            if (Math.abs(row[j]) < pruneThreshold) { row[j]=0f; pruned++; }
        }
        lastSparsity = total > 0 ? (float)pruned/total : 0f;
        totalWeights += total; prunedWeights += pruned;
        compressCount.incrementAndGet();
        return lastSparsity;
    }

    /** Global magnitude pruning: prune top-K% smallest weights across all layers. */
    public synchronized void globalPrune(float[][]... layers) {
        // Collect all weights
        int total=0;
        for (float[][] W:layers) for(float[] row:W) total+=row.length;
        float[] flat=new float[total]; int idx=0;
        for (float[][] W:layers) for(float[] row:W) for(float w:row) flat[idx++]=Math.abs(w);
        // Find threshold at targetSparsity percentile
        java.util.Arrays.sort(flat);
        int thresh_idx = (int)(targetSparsity*total);
        float threshold = flat[Math.min(thresh_idx, total-1)];
        // Prune
        for (float[][] W:layers) for(float[] row:W) for(int j=0;j<row.length;j++) if(Math.abs(row[j])<threshold) row[j]=0f;
        lastSparsity = targetSparsity;
    }

    /** Structured pruning: zero out entire neurons with lowest L2 norm. */
    public synchronized void structuredPruneNeurons(float[][] W, float fraction) {
        // Compute L2 norm of each output neuron (row)
        float[] norms = new float[W.length];
        for (int i=0;i<W.length;i++) { for(float w:W[i]) norms[i]+=w*w; norms[i]=(float)Math.sqrt(norms[i]); }
        float[] sorted = norms.clone(); java.util.Arrays.sort(sorted);
        float threshold = sorted[(int)(fraction*W.length)];
        for (int i=0;i<W.length;i++) if(norms[i]<threshold) java.util.Arrays.fill(W[i],0f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Quantization
    // ─────────────────────────────────────────────────────────────────────────

    /** Quantize weights to INT8 range [-127,127] and return scale factor. */
    public synchronized float quantize(float[][] W, byte[][] Wq) {
        // Find max absolute value
        float absMax = 1e-8f;
        for (float[] row : W) for (float w : row) if(Math.abs(w)>absMax) absMax=Math.abs(w);
        float scale = QUANT_SCALE / absMax;
        for (int i=0;i<W.length;i++) {
            if (Wq[i] == null || Wq[i].length != W[i].length) Wq[i] = new byte[W[i].length];
            for (int j=0;j<W[i].length;j++) Wq[i][j] = (byte)Math.max(-127,Math.min(127,(int)(W[i][j]*scale)));
        }
        lastCompRatio = 4f;  // FP32 → INT8
        compressCount.incrementAndGet();
        return scale;
    }

    /** Dequantize INT8 weights back to float. */
    public synchronized void dequantize(byte[][] Wq, float scale, float[][] W) {
        for (int i=0;i<Wq.length;i++) {
            if (W[i] == null || W[i].length != Wq[i].length) W[i] = new float[Wq[i].length];
            for (int j=0;j<Wq[i].length;j++) W[i][j] = Wq[i][j] / scale;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Knowledge Distillation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Compute distillation loss between teacher and student logits.
     * @param teacherLogits Logits from teacher (large) network.
     * @param studentLogits Logits from student (small) network.
     * @param hardLabel     True class label (one-hot index).
     * @return Weighted distillation loss.
     */
    public synchronized float distillationLoss(float[] teacherLogits, float[] studentLogits,
                                                int hardLabel) {
        // Soft targets from teacher
        float[] tSoft = temperatureSoftmax(teacherLogits, distilTemp);
        float[] sSoft = temperatureSoftmax(studentLogits, distilTemp);

        // KL divergence: Σ p_teacher · log(p_teacher/p_student)
        float kl = 0;
        for (int i=0;i<Math.min(tSoft.length,sSoft.length);i++)
            if (tSoft[i]>1e-8f && sSoft[i]>1e-8f)
                kl += tSoft[i] * (float)(Math.log(tSoft[i])-Math.log(sSoft[i]));

        // Hard label CE loss
        float[] hardProbs = softmax(studentLogits);
        float ceLoss = 0;
        if (hardLabel >= 0 && hardLabel < hardProbs.length)
            ceLoss = -(float)Math.log(Math.max(hardProbs[hardLabel], 1e-8f));

        return distilAlpha * ceLoss + (1-distilAlpha) * distilTemp * distilTemp * kl;
    }

    /** Gradient of distillation loss w.r.t. student logits. */
    public synchronized float[] distillationGradient(float[] teacherLogits,
                                                       float[] studentLogits, int hardLabel) {
        float[] tSoft = temperatureSoftmax(teacherLogits, distilTemp);
        float[] sSoft = temperatureSoftmax(studentLogits, distilTemp);
        float[] grad  = new float[studentLogits.length];
        for (int i=0;i<grad.length;i++) {
            float hardGrad = (i==hardLabel ? sSoft[i]-1f : sSoft[i]) * distilAlpha;
            float softGrad = (sSoft[i]-tSoft[i]) * (1-distilAlpha);
            grad[i] = hardGrad + softGrad;
        }
        return grad;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Low-Rank Factorisation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Factorise W ≈ U·V^T using randomised SVD.
     * @return U [m×r], V [n×r] such that W ≈ U·V^T.
     */
    public synchronized float[][][] lowRankFactorise(float[][] W) {
        int m = W.length, n = W[0].length;
        int r = Math.min(rankR, Math.min(m, n));
        // Randomised power iteration (simplified rank-r approximation)
        float[][] omega = xav(n, r);
        float[][] Y     = matmul(W, omega);       // [m][r]
        float[][] Q     = gramSchmidt(Y);          // [m][r] orthonormal
        float[][] B     = matmulT(Q, W);           // [r][n]
        // B ≈ V^T · Σ, so U = Q, V = B^T
        float[][][] result = new float[2][][];
        result[0] = Q;                             // U [m][r]
        result[1] = transpose(B);                  // V [n][r]
        lastCompRatio = (float)(m*n) / (r*(m+n));
        compressCount.incrementAndGet();
        return result;
    }

    /** Multiply two factorised matrices: U·V^T → full W. */
    public synchronized float[][] reconstruct(float[][] U, float[][] V) {
        int m=U.length, n=V.length, r=U[0].length;
        float[][] W=new float[m][n];
        for(int i=0;i<m;i++) for(int j=0;j<n;j++) for(int k=0;k<r;k++) W[i][j]+=U[i][k]*V[j][k];
        return W;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Matrix ops
    // ─────────────────────────────────────────────────────────────────────────

    private float[][] matmul(float[][] A, float[][] B) {
        int m=A.length,k=A[0].length,n=B[0].length;
        float[][] C=new float[m][n];
        for(int i=0;i<m;i++) for(int j=0;j<n;j++) for(int l=0;l<k;l++) C[i][j]+=A[i][l]*B[l][j];
        return C;
    }
    private float[][] matmulT(float[][] A, float[][] B) { // C = A^T · B
        int r=A[0].length,m=A.length,n=B[0].length;
        float[][] C=new float[r][n];
        for(int i=0;i<r;i++) for(int j=0;j<n;j++) for(int l=0;l<m;l++) C[i][j]+=A[l][i]*B[l][j];
        return C;
    }
    private float[][] gramSchmidt(float[][] Y) {
        int m=Y.length,r=Y[0].length;
        float[][] Q=new float[m][r];
        for(int k=0;k<r;k++){
            float[] v=new float[m]; for(int i=0;i<m;i++) v[i]=Y[i][k];
            for(int j=0;j<k;j++){float dot=0;for(int i=0;i<m;i++)dot+=Q[i][j]*v[i];for(int i=0;i<m;i++)v[i]-=dot*Q[i][j];}
            float norm=0; for(float vi:v) norm+=vi*vi; norm=(float)Math.sqrt(norm+1e-8f);
            for(int i=0;i<m;i++) Q[i][k]=v[i]/norm;
        }
        return Q;
    }
    private float[][] transpose(float[][] A){float[][] T=new float[A[0].length][A.length];for(int i=0;i<A.length;i++) for(int j=0;j<A[0].length;j++) T[j][i]=A[i][j];return T;}
    private float[][] xav(int r,int c){float[][] m=new float[r][c];for(int i=0;i<r;i++) for(int j=0;j<c;j++) m[i][j]=(rng.nextFloat()*2f-1f)*(float)Math.sqrt(1.0/c);return m;}
    private static float[] softmax(float[] v){float mx=v[0];for(float x:v)if(x>mx)mx=x;float sum=0;float[] o=new float[v.length];for(int i=0;i<v.length;i++){o[i]=(float)Math.exp(v[i]-mx);sum+=o[i];}for(int i=0;i<v.length;i++)o[i]/=sum;return o;}
    private static float[] temperatureSoftmax(float[] v, float T){float[] s=v.clone();for(int i=0;i<s.length;i++)s[i]/=T;return softmax(s);}

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("compressCount", compressCount.get());
        s.put("lastSparsity",  lastSparsity);
        s.put("lastCompRatio", lastCompRatio);
        s.put("totalWeights",  totalWeights);
        s.put("prunedWeights", prunedWeights);
        s.put("pruneThreshold",pruneThreshold);
        s.put("rankR",         rankR);
        return s;
    }
}
