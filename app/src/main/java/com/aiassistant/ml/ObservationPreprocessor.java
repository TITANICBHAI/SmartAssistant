package com.aiassistant.ml;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ObservationPreprocessor — standardised observation preprocessing pipeline.
 *
 * Transforms raw environment observations into normalised feature vectors
 * suitable for neural network input.
 *
 * Pipeline stages (applied in order):
 *
 *   1. FRAME_STACK:    Concatenate last N frames → temporal context.
 *   2. CHANNEL_NORM:   Per-channel z-score normalisation (running stats).
 *   3. CLIP:           Clip to [-clip, +clip] after normalisation.
 *   4. PCA_WHITEN:     Optional dimensionality reduction via online PCA.
 *   5. DELTA:          Append Δ(obs) = obs_t - obs_{t-1} (velocity features).
 *   6. AUGMENT:        Random noise injection (training only, improves robustness).
 *
 * Also computes:
 *   - Observation novelty (deviation from running mean).
 *   - Observation variance (measure of how dynamic the environment is).
 *
 * Thread-safe.
 */
public class ObservationPreprocessor {

    private static final String TAG = "ObsPreproc";

    // ─────────────────────────────────────────────────────────────────────────
    // Configuration flags
    // ─────────────────────────────────────────────────────────────────────────
    public static class Config {
        public int     stackSize      = 4;
        public boolean channelNorm    = true;
        public float   clipValue      = 10f;
        public boolean appendDelta    = true;
        public boolean augment        = false;   // training only
        public float   noiseStd       = 0.01f;
        public boolean pcaWhiten      = false;
        public int     pcaDim         = 32;
        public int     rawDim         = 64;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final Config cfg;
    private final int    rawDim;

    // Frame stack
    private final float[][] stack;
    private int             stackHead = 0;
    private int             stackFilled = 0;

    // Previous observation (for delta)
    private float[] prevObs;

    // Running statistics (Welford)
    private final double[] runMean, runM2;
    private long           runN = 0;

    // Online PCA (simplified: random projection whitening)
    private final float[][] pcaMatrix;   // [pcaDim][rawDim]

    // Stats
    private final AtomicInteger processCount = new AtomicInteger(0);
    private float avgNovelty  = 0f;
    private float avgVariance = 0f;

    private final java.util.Random rng = new java.util.Random(349L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public ObservationPreprocessor(Config cfg) {
        this.cfg    = cfg;
        this.rawDim = cfg.rawDim;
        this.stack  = new float[cfg.stackSize][rawDim];
        this.prevObs= new float[rawDim];
        this.runMean= new double[rawDim];
        this.runM2  = new double[rawDim];
        // Random projection PCA matrix
        this.pcaMatrix = new float[cfg.pcaDim][rawDim];
        float s = 1f / (float)Math.sqrt(rawDim);
        for (float[] row : pcaMatrix) for(int j=0;j<rawDim;j++) row[j]=(rng.nextFloat()*2f-1f)*s;

        Log.i(TAG, "ObsPreproc: rawDim=" + rawDim + " stack=" + cfg.stackSize
                + " delta=" + cfg.appendDelta + " pca=" + cfg.pcaWhiten);
    }

    public ObservationPreprocessor(int rawDim) {
        this(defaultConfig(rawDim));
    }

    private static Config defaultConfig(int rawDim) {
        Config c = new Config(); c.rawDim = rawDim; return c;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Process a raw observation vector.
     * @param rawObs Raw observation [rawDim].
     * @param training Whether augmentation should be applied.
     * @return Preprocessed feature vector.
     */
    public synchronized float[] process(float[] rawObs, boolean training) {
        float[] obs = pad(rawObs, rawDim);

        // Update running stats
        updateStats(obs);

        // Channel normalisation
        float[] normed = cfg.channelNorm ? normalise(obs) : obs.clone();

        // Clip
        for (int i=0;i<rawDim;i++) normed[i] = Math.max(-cfg.clipValue, Math.min(cfg.clipValue, normed[i]));

        // Augment (training only)
        if (training && cfg.augment) {
            for (int i=0;i<rawDim;i++) normed[i] += (float)(rng.nextGaussian() * cfg.noiseStd);
        }

        // Compute novelty before PCA
        float novelty = novelty(normed);
        avgNovelty  = 0.99f * avgNovelty  + 0.01f * novelty;

        // PCA whiten (optional)
        float[] projected = cfg.pcaWhiten ? pcaProject(normed) : normed;

        // Push to stack
        System.arraycopy(projected, 0, stack[stackHead], 0, Math.min(projected.length, stack[stackHead].length));
        stackHead   = (stackHead + 1) % cfg.stackSize;
        stackFilled = Math.min(stackFilled + 1, cfg.stackSize);

        // Compute delta
        float[] delta = new float[rawDim];
        for (int i=0;i<rawDim;i++) delta[i] = normed[i] - prevObs[i];
        prevObs = normed.clone();
        float var = 0; for(float d:delta) var+=d*d; avgVariance=0.99f*avgVariance+0.01f*var/rawDim;

        // Build output
        float[] out = buildOutput(projected, delta);
        processCount.incrementAndGet();
        return out;
    }

    /** Returns the expected output dimension for this preprocessor. */
    public int outputDim() {
        int base = cfg.pcaWhiten ? cfg.pcaDim : rawDim;
        int stacked = base * cfg.stackSize;
        return cfg.appendDelta ? stacked + rawDim : stacked;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private float[] buildOutput(float[] latest, float[] delta) {
        int base = latest.length;
        int outDim = base * cfg.stackSize + (cfg.appendDelta ? rawDim : 0);
        float[] out = new float[outDim];
        // Stack (oldest → newest)
        for (int k=0;k<cfg.stackSize;k++) {
            int idx = (stackHead - cfg.stackSize + k + cfg.stackSize * 100) % cfg.stackSize;
            float[] frame = stack[idx];
            System.arraycopy(frame, 0, out, k * base, Math.min(base, frame.length));
        }
        if (cfg.appendDelta) {
            System.arraycopy(delta, 0, out, base * cfg.stackSize, Math.min(rawDim, delta.length));
        }
        return out;
    }

    private float[] normalise(float[] obs) {
        float[] n = new float[rawDim];
        for (int i=0;i<rawDim;i++) {
            double std = runN < 2 ? 1.0 : Math.sqrt(runM2[i]/(runN-1)+1e-8);
            n[i] = (float)((obs[i] - runMean[i]) / std);
        }
        return n;
    }

    private void updateStats(float[] obs) {
        runN++;
        for (int i=0;i<rawDim;i++) {
            double d = obs[i] - runMean[i];
            runMean[i] += d / runN;
            runM2[i]   += d * (obs[i] - runMean[i]);
        }
    }

    private float novelty(float[] obs) {
        float d = 0;
        for (int i=0;i<rawDim;i++) { float diff=(float)(obs[i]-runMean[i]); d+=diff*diff; }
        return (float)Math.sqrt(d/rawDim);
    }

    private float[] pcaProject(float[] obs) {
        float[] proj = new float[cfg.pcaDim];
        for (int i=0;i<cfg.pcaDim;i++)
            for (int j=0;j<Math.min(rawDim,obs.length);j++) proj[i]+=pcaMatrix[i][j]*obs[j];
        return proj;
    }

    private static float[] pad(float[] x, int dim) {
        if(x.length==dim)return x;float[] p=new float[dim];System.arraycopy(x,0,p,0,Math.min(x.length,dim));return p;
    }

    /** Reset frame stack (call at episode start). */
    public synchronized void reset() {
        stackHead = 0; stackFilled = 0;
        for (float[] f : stack) java.util.Arrays.fill(f, 0f);
        java.util.Arrays.fill(prevObs, 0f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("processCount", processCount.get());
        s.put("avgNovelty",   avgNovelty);
        s.put("avgVariance",  avgVariance);
        s.put("outputDim",    outputDim());
        s.put("stackFilled",  stackFilled);
        s.put("runN",         runN);
        return s;
    }
}
