package com.aiassistant.ml;

import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PPOTrainer — full Proximal Policy Optimization training loop.
 *
 * Uses:
 *   PolicyNetwork  — actor  π_θ(a|s)
 *   ValueNetwork   — critic V_φ(s)
 *   RolloutBuffer  — on-policy trajectory storage with GAE
 *   GradientClipper — norm clipping (maxNorm = 0.5)
 *   LearningRateScheduler — warm-up + cosine annealing
 *
 * PPO clipped objective:
 *   L_clip = E[min(r_t·A_t, clip(r_t, 1−ε, 1+ε)·A_t)]
 *   where r_t = π_θ(a_t|s_t) / π_θ_old(a_t|s_t)
 *
 * Approximated on-device via importance-weighted policy gradient:
 *   ∇θ L ≈ Σ clip(ratio, 1−ε, 1+ε) · A_t · ∇θ log π(a|s)
 *
 * Value loss (clipped PPO2-style):
 *   L_v = max((V(s)−G_t)², (clip(V,V_old±ε)−G_t)²)
 *
 * Early stopping: skip epoch if KL divergence > targetKL.
 */
public class PPOTrainer {

    private static final String TAG = "PPOTrainer";

    // ── sub-systems ───────────────────────────────────────────────────────────
    private final PolicyNetwork  actor;
    private final ValueNetwork   critic;
    private final RolloutBuffer  rollout;
    private final GradientClipper clipper;
    private final LearningRateScheduler lrSched;

    // ── PPO hyper-params ──────────────────────────────────────────────────────
    private final float clipEps;      // ε in clip(r, 1−ε, 1+ε)
    private final float targetKL;     // early-stop KL threshold
    private final float valueCoeff;   // weight of value loss
    private final float entropyCoeff; // weight of entropy bonus
    private final int   epochs;       // update epochs per rollout
    private final int   miniBatch;
    private final int   rolloutSteps;
    private final float gamma;
    private final float gaeLambda;

    // ── stats ──────────────────────────────────────────────────────────────────
    private final AtomicInteger updateCount     = new AtomicInteger(0);
    private final AtomicInteger episodeCount    = new AtomicInteger(0);
    private float avgPolicyLoss  = 0f;
    private float avgValueLoss   = 0f;
    private float avgEntropy     = 0f;
    private float avgEpReturn    = 0f;
    private float episodeReturn  = 0f;
    private float lastValue      = 0f;
    private boolean lastDone     = false;

    // ─────────────────────────────────────────────────────────────────────────
    public PPOTrainer(int stateDim, int actionDim, int rolloutSteps,
                      float gamma, float gaeLambda, float lr,
                      float clipEps, float targetKL, float valueCoeff,
                      float entropyCoeff, int epochs, int miniBatch) {
        this.rolloutSteps  = rolloutSteps;
        this.gamma         = gamma;
        this.gaeLambda     = gaeLambda;
        this.clipEps       = clipEps;
        this.targetKL      = targetKL;
        this.valueCoeff    = valueCoeff;
        this.entropyCoeff  = entropyCoeff;
        this.epochs        = epochs;
        this.miniBatch     = miniBatch;

        actor   = new PolicyNetwork(stateDim, 128, actionDim, lr,
                                     entropyCoeff, entropyCoeff * 0.1f, 0.9999f);
        critic  = new ValueNetwork(stateDim, 128, lr * 2f, clipEps, true);
        rollout = new RolloutBuffer(rolloutSteps, gamma, gaeLambda, true);
        clipper = new GradientClipper(GradientClipper.Strategy.GLOBAL_NORM, 0.5f);
        lrSched = LearningRateScheduler.warmUpCosine(lr, lr * 0.01f, 2000, 1_000_000);

        Log.i(TAG, "PPOTrainer: stateDim=" + stateDim + " actionDim=" + actionDim
                + " rolloutSteps=" + rolloutSteps + " clipEps=" + clipEps);
    }

    /** Defaults matching common PPO hyperparameters. */
    public PPOTrainer(int stateDim, int actionDim) {
        this(stateDim, actionDim, 256, 0.99f, 0.95f, 3e-4f,
             0.2f, 0.015f, 0.5f, 0.01f, 4, 64);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Action selection
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized int selectAction(float[] state) {
        return actor.sampleAction(state);
    }

    public synchronized int greedyAction(float[] state) {
        return actor.greedyAction(state);
    }

    public synchronized float[] getActionProbs(float[] state) {
        return actor.getProbs(state);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Observation & buffering
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized float observe(float[] state, int action, float reward,
                                       float[] nextState, boolean done) {
        float logProb = actor.logProb(state, action);
        float value   = critic.getValue(state);
        lastValue     = done ? 0f : critic.getValue(nextState);
        lastDone      = done;

        rollout.add(state, action, reward, nextState, done, logProb, value);
        episodeReturn += reward;

        if (done) {
            episodeCount.incrementAndGet();
            avgEpReturn  = 0.99f * avgEpReturn + 0.01f * episodeReturn;
            episodeReturn = 0f;
        }

        if (rollout.isFull() || (done && rollout.size() > 0)) {
            return runPPOUpdate();
        }
        return 0f;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PPO update
    // ─────────────────────────────────────────────────────────────────────────

    private float runPPOUpdate() {
        rollout.computeReturnsAndAdvantages(lastDone ? 0f : lastValue);
        List<RolloutBuffer.Step> all = rollout.getAllSteps();
        if (all.isEmpty()) { rollout.reset(); return 0f; }

        float totPL = 0f, totVL = 0f, totEnt = 0f;
        int   batches = 0;
        boolean earlyStop = false;

        for (int ep = 0; ep < epochs && !earlyStop; ep++) {
            List<RolloutBuffer.Step> batch = rollout.sampleMiniBatch(miniBatch);
            if (batch.isEmpty()) continue;

            float bPL = 0f, bVL = 0f, bEnt = 0f;
            float kl  = 0f;

            for (RolloutBuffer.Step step : batch) {
                // Importance ratio r_t = π_new(a|s) / π_old(a|s)
                float newLogProb = actor.logProb(step.state, step.action);
                float ratio      = (float) Math.exp(newLogProb - step.logProb);
                float clipped    = Math.max(1f - clipEps, Math.min(1f + clipEps, ratio));
                float clippedAdv = Math.min(ratio * step.advantage, clipped * step.advantage);

                // Approx: use clipped advantage as the gradient weight in policy update
                float actorLoss = actor.update(step.state, step.action, clippedAdv);
                float valueLoss = critic.update(step.state, step.returnVal, step.value);

                bPL  += actorLoss;
                bVL  += valueLoss * valueCoeff;
                bEnt += actor.entropy(step.state);
                kl   += Math.abs(newLogProb - step.logProb);
            }
            bPL  /= batch.size(); bVL /= batch.size();
            bEnt /= batch.size(); kl  /= batch.size();

            totPL += bPL; totVL += bVL; totEnt += bEnt;
            batches++;

            if (kl > targetKL) {
                Log.d(TAG, "KL=" + String.format("%.4f", kl)
                        + " > targetKL=" + targetKL + " early-stop at epoch=" + ep);
                earlyStop = true;
            }
        }

        rollout.reset();
        lrSched.step();
        updateCount.incrementAndGet();

        if (batches > 0) {
            avgPolicyLoss = 0.95f * avgPolicyLoss + 0.05f * (totPL / batches);
            avgValueLoss  = 0.95f * avgValueLoss  + 0.05f * (totVL / batches);
            avgEntropy    = 0.95f * avgEntropy     + 0.05f * (totEnt / batches);
        }

        if (updateCount.get() % 50 == 0) {
            Log.d(TAG, "PPO update #" + updateCount.get()
                    + " policyLoss=" + String.format("%.4f", avgPolicyLoss)
                    + " valueLoss="  + String.format("%.4f", avgValueLoss)
                    + " entropy="    + String.format("%.3f", avgEntropy)
                    + " avgReturn="  + String.format("%.2f", avgEpReturn)
                    + " ev="         + String.format("%.3f", critic.explainedVariance()));
        }
        return avgPolicyLoss;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("updateCount",      updateCount.get());
        s.put("episodeCount",     episodeCount.get());
        s.put("avgPolicyLoss",    avgPolicyLoss);
        s.put("avgValueLoss",     avgValueLoss);
        s.put("avgEntropy",       avgEntropy);
        s.put("avgEpisodeReturn", avgEpReturn);
        s.put("explainedVariance", critic.explainedVariance());
        s.put("currentLr",        lrSched.getLr());
        s.put("clipRate",         clipper.getClipRate());
        s.put("rollout",          rollout.getStats());
        return s;
    }
}
