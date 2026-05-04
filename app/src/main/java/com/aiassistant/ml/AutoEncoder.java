package com.aiassistant.ml;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AutoEncoder — unsupervised state-representation learning for RL agents.
 *
 * Architecture:
 *   Encoder: state(stateDim) → [W1,ReLU] → [W2,ReLU] → latent(latentDim)
 *   Decoder: latent(latentDim) → [W3,ReLU] → [W4] → reconstruction(stateDim)
 *
 * Loss: MSE(input, reconstruction) + β·KL(latent || N(0,1))  [VAE-style regularisation]
 *
 * Applications:
 *   - Compress high-dimensional game states before feeding to RL agent
 *   - Anomaly detection: high reconstruction error = novel/unusual state
 *   - Pre-train representations before RL fine-tuning
 *   - State clustering for hierarchical RL
 *
 * Usage:
 *   autoEncoder.train(stateVector);             // unsupervised update
 *   float[] latent = autoEncoder.encode(state); // compressed representation
 *   float[] recon  = autoEncoder.decode(latent);
 *   float   anomaly= autoEncoder.reconstructionError(state); // novelty score
 */
public class AutoEncoder {

    private static final String TAG = "AutoEncoder";

    // ── dimensions ────────────────────────────────────────────────────────────
    private final int stateDim;
    private final int hiddenDim;
    private final int latentDim;

    // ── encoder weights ───────────────────────────────────────────────────────
    private final float[][] E1, E2;    // [hidden][in], [latent][hidden]
    private final float[]   EB1, EB2;

    // ── decoder weights ───────────────────────────────────────────────────────
    private final float[][] D1, D2;    // [hidden][latent], [state][hidden]
    private final float[]   DB1, DB2;

    // ── VAE: mean & logvar heads from encoder ─────────────────────────────────
    private final float[][] Wmu, Wlv;  // [latent][hidden]
    private final float[]   Bmu, Blv;

    // ── optimizer ─────────────────────────────────────────────────────────────
    private final NeuralNetworkOptimizer optimiser;
    private final float betaKL;        // KL regularisation weight
    private final boolean vaeMode;     // true = VAE, false = plain AE

    // ── stats ──────────────────────────────────────────────────────────────────
    private final AtomicInteger updateCount  = new AtomicInteger(0);
    private float avgReconLoss = 0f;
    private float avgKlLoss    = 0f;
    private float maxReconError= 0f;

    private final Random rng = new Random(31L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public AutoEncoder(int stateDim, int hiddenDim, int latentDim,
                       float lr, float betaKL, boolean vaeMode) {
        this.stateDim  = stateDim;
        this.hiddenDim = hiddenDim;
        this.latentDim = latentDim;
        this.betaKL    = betaKL;
        this.vaeMode   = vaeMode;
        this.optimiser = new NeuralNetworkOptimizer(lr);

        float s1 = sc(stateDim,  hiddenDim);
        float s2 = sc(hiddenDim, latentDim);
        float s3 = sc(latentDim, hiddenDim);
        float s4 = sc(hiddenDim, stateDim);

        E1  = xav(hiddenDim, stateDim,  s1); EB1 = new float[hiddenDim];
        E2  = xav(hiddenDim, hiddenDim, s1); EB2 = new float[hiddenDim];
        Wmu = xav(latentDim, hiddenDim, s2); Bmu = new float[latentDim];
        Wlv = xav(latentDim, hiddenDim, s2); Blv = new float[latentDim];
        D1  = xav(hiddenDim, latentDim, s3); DB1 = new float[hiddenDim];
        D2  = xav(stateDim,  hiddenDim, s4); DB2 = new float[stateDim];
    }

    public AutoEncoder(int stateDim, int latentDim) {
        this(stateDim, Math.max(latentDim * 2, 64), latentDim, 1e-3f, 0.001f, true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inference
    // ─────────────────────────────────────────────────────────────────────────

    /** Encode state → latent vector (deterministic mean for VAE). */
    public synchronized float[] encode(float[] state) {
        float[] h1 = lin(E1, EB1, pad(state, stateDim), true);
        float[] h2 = lin(E2, EB2, h1, true);
        // Return mu (mean) as the latent code
        return lin(Wmu, Bmu, h2, false);
    }

    /** Decode latent → reconstructed state. */
    public synchronized float[] decode(float[] latent) {
        float[] h1 = lin(D1, DB1, latent, true);
        return lin(D2, DB2, h1, false);
    }

    /** Reconstruction error (MSE) — use as anomaly/novelty score. */
    public synchronized float reconstructionError(float[] state) {
        float[] padded = pad(state, stateDim);
        float[] latent = encode(padded);
        float[] recon  = decode(latent);
        float   mse    = 0f;
        for (int i = 0; i < stateDim; i++) {
            float d = padded[i] - recon[i]; mse += d * d;
        }
        return mse / stateDim;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Training
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * One training step. Updates all encoder + decoder weights.
     * @return Total loss = recon_loss + β·KL_loss.
     */
    public synchronized float train(float[] state) {
        float[] x  = pad(state, stateDim);

        // ── Encoder forward ──────────────────────────────────────────────────
        float[] h1 = lin(E1, EB1, x,  true);
        float[] h2 = lin(E2, EB2, h1, true);
        float[] mu = lin(Wmu, Bmu, h2, false);
        float[] lv = lin(Wlv, Blv, h2, false);  // log-variance

        // ── Reparameterisation (VAE) ─────────────────────────────────────────
        float[] z  = new float[latentDim];
        float[] eps = new float[latentDim];
        for (int i = 0; i < latentDim; i++) {
            eps[i] = vaeMode ? (float) rng.nextGaussian() : 0f;
            float std = (float) Math.exp(0.5f * lv[i]);
            z[i] = mu[i] + std * eps[i];
        }

        // ── Decoder forward ──────────────────────────────────────────────────
        float[] dh1  = lin(D1, DB1, z,   true);
        float[] xHat = lin(D2, DB2, dh1, false);

        // ── Reconstruction loss (MSE) ────────────────────────────────────────
        float reconLoss = 0f;
        float[] dxHat   = new float[stateDim];
        for (int i = 0; i < stateDim; i++) {
            float d = xHat[i] - x[i];
            dxHat[i] = 2f * d;
            reconLoss += d * d;
        }
        reconLoss /= stateDim;

        // ── KL loss (VAE): 0.5 * Σ(exp(lv) + mu² - 1 - lv) ─────────────────
        float klLoss = 0f;
        for (int i = 0; i < latentDim; i++) {
            klLoss += 0.5f * ((float) Math.exp(lv[i]) + mu[i] * mu[i] - 1f - lv[i]);
        }

        // ── Backprop decoder ─────────────────────────────────────────────────
        float[][] dD2 = new float[stateDim][hiddenDim];
        float[]   dDB2= new float[stateDim];
        for (int i = 0; i < stateDim; i++) {
            dDB2[i] = dxHat[i];
            for (int j = 0; j < hiddenDim; j++) dD2[i][j] = dxHat[i] * dh1[j];
        }
        float[] dDh1 = new float[hiddenDim];
        for (int j = 0; j < hiddenDim; j++) {
            if (dh1[j] <= 0f) continue;
            for (int i = 0; i < stateDim; i++) dDh1[j] += dxHat[i] * D2[i][j];
        }
        float[][] dD1 = new float[hiddenDim][latentDim];
        float[]   dDB1= new float[hiddenDim];
        for (int j = 0; j < hiddenDim; j++) {
            dDB1[j] = dDh1[j];
            for (int k = 0; k < latentDim; k++) dD1[j][k] = dDh1[j] * z[k];
        }
        optimiser.step("ae_D2", D2, dD2);
        optimiser.step("ae_D1", D1, dD1);

        // ── Backprop through z → mu, lv ─────────────────────────────────────
        float[] dz  = new float[latentDim];
        float[] dmu = new float[latentDim];
        float[] dlv = new float[latentDim];
        for (int i = 0; i < latentDim; i++) {
            for (int j = 0; j < hiddenDim; j++) dz[i] += dDh1[j] * D1[j][i];
            // KL gradient for mu: mu + dRecon/dmu
            dmu[i] = betaKL * mu[i] + dz[i];
            // KL gradient for logvar: 0.5*(exp(lv)-1) + dRecon/dlv
            float std = (float) Math.exp(0.5f * lv[i]);
            dlv[i] = betaKL * 0.5f * ((float) Math.exp(lv[i]) - 1f)
                    + dz[i] * eps[i] * 0.5f * std;
        }

        // ── Backprop encoder ─────────────────────────────────────────────────
        float[][] dWmu = new float[latentDim][hiddenDim];
        float[][] dWlv = new float[latentDim][hiddenDim];
        for (int i = 0; i < latentDim; i++) {
            for (int j = 0; j < hiddenDim; j++) {
                dWmu[i][j] = dmu[i] * h2[j];
                dWlv[i][j] = dlv[i] * h2[j];
            }
        }
        optimiser.step("ae_Wmu", Wmu, dWmu);
        optimiser.step("ae_Wlv", Wlv, dWlv);

        float[] dH2 = new float[hiddenDim];
        for (int j = 0; j < hiddenDim; j++) {
            if (h2[j] <= 0f) continue;
            for (int i = 0; i < latentDim; i++)
                dH2[j] += dmu[i] * Wmu[i][j] + dlv[i] * Wlv[i][j];
        }
        float[][] dE2 = new float[hiddenDim][hiddenDim];
        for (int j = 0; j < hiddenDim; j++) {
            for (int k = 0; k < hiddenDim; k++) dE2[j][k] = dH2[j] * h1[k];
        }
        optimiser.step("ae_E2", E2, dE2);

        float[] dH1 = new float[hiddenDim];
        for (int k = 0; k < hiddenDim; k++) {
            if (h1[k] <= 0f) continue;
            for (int j = 0; j < hiddenDim; j++) dH1[k] += dH2[j] * E2[j][k];
        }
        float[][] dE1 = new float[hiddenDim][stateDim];
        for (int k = 0; k < hiddenDim; k++) {
            for (int s = 0; s < stateDim; s++) dE1[k][s] = dH1[k] * x[s];
        }
        optimiser.step("ae_E1", E1, dE1);

        float total = reconLoss + betaKL * klLoss;
        avgReconLoss = 0.95f * avgReconLoss + 0.05f * reconLoss;
        avgKlLoss    = 0.95f * avgKlLoss    + 0.05f * klLoss;
        if (reconLoss > maxReconError) maxReconError = reconLoss;
        updateCount.incrementAndGet();
        return total;
    }

    /** Batch train on multiple state vectors. */
    public synchronized float batchTrain(float[][] states) {
        float total = 0f;
        for (float[] s : states) total += train(s);
        return states.length > 0 ? total / states.length : 0f;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public int getDimension()  { return latentDim; }

    public Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("updateCount",  updateCount.get());
        s.put("avgReconLoss", avgReconLoss);
        s.put("avgKlLoss",    avgKlLoss);
        s.put("maxReconError",maxReconError);
        s.put("stateDim",     stateDim);
        s.put("latentDim",    latentDim);
        s.put("hiddenDim",    hiddenDim);
        s.put("vaeMode",      vaeMode);
        s.put("betaKL",       betaKL);
        return s;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static float[] lin(float[][] W, float[] b, float[] inp, boolean relu) {
        float[] o = new float[W.length];
        for (int i = 0; i < W.length; i++) {
            float sum = b[i];
            for (int j = 0; j < Math.min(inp.length, W[i].length); j++) sum += W[i][j] * inp[j];
            o[i] = relu ? Math.max(0f, sum) : sum;
        }
        return o;
    }

    private static float[] pad(float[] x, int dim) {
        if (x.length == dim) return x;
        float[] p = new float[dim];
        System.arraycopy(x, 0, p, 0, Math.min(x.length, dim));
        return p;
    }

    private static float sc(int a, int b) { return (float) Math.sqrt(2.0 / (a + b)); }

    private float[][] xav(int r, int c, float s) {
        float[][] m = new float[r][c];
        for (int i = 0; i < r; i++)
            for (int j = 0; j < c; j++) m[i][j] = (rng.nextFloat() * 2f - 1f) * s;
        return m;
    }
}
