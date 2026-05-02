package com.aiassistant.ml;

import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ActorCriticAgent — full Advantage Actor-Critic (A2C) on-device RL agent.
 *
 * Components:
 *   PolicyNetwork  π_θ(a|s)  — stochastic actor
 *   ValueNetwork   V_φ(s)    — critic for baseline/advantage estimation
 *   RolloutBuffer             — collects T on-policy steps before each update
 *   IntrinsicMotivation       — optional count-based + ICM exploration bonus
 *   LearningRateScheduler     — cosine-annealed LR for both actor and critic
 *   GradientClipper           — prevents gradient explosion
 *
 * Training loop (called from background thread):
 *   1. Collect T steps into RolloutBuffer using π_θ.
 *   2. Compute GAE advantages and discounted returns.
 *   3. Mini-batch policy gradient update on actor.
 *   4. Mini-batch MSE update on critic (value loss).
 *   5. Optionally add entropy bonus to actor loss.
 *   6. Clear buffer and repeat.
 *
 * Thread-safe; select/observe can be called from any thread.
 */
public class ActorCriticAgent {

    private static final String TAG = "A2CAgent";

    // ── sub-systems ───────────────────────────────────────────────────────────
    private final PolicyNetwork       actor;
    private final ValueNetwork        critic;
    private final RolloutBuffer       rollout;
    private final IntrinsicMotivation intrinsic;
    private final LearningRateScheduler actorSched;
    private final LearningRateScheduler criticSched;
    private final GradientClipper     clipper;

    // ── hyper-params ──────────────────────────────────────────────────────────
    private final int   stateDim;
    private final int   actionDim;
    private final int   rolloutSteps;
    private final float gamma;
    private final float gaeLambda;
    private final float valueLossCoeff;
    private final int   miniBatchSize;

    // ── episode state ─────────────────────────────────────────────────────────
    private float lastValue    = 0f;
    private boolean lastDone   = false;

    // ── stats ──────────────────────────────────────────────────────────────────
    private final AtomicInteger updateCount      = new AtomicInteger(0);
    private final AtomicInteger episodeCount     = new AtomicInteger(0);
    private final AtomicLong    totalStepCount   = new AtomicLong(0);
    private float avgActorLoss   = 0f;
    private float avgCriticLoss  = 0f;
    private float avgEpisodeReturn = 0f;
    private float episodeReturn  = 0f;

    // ─────────────────────────────────────────────────────────────────────────
    public ActorCriticAgent(int stateDim, int actionDim, int rolloutSteps,
                             float gamma, float gaeLambda, float actorLr,
                             float criticLr, float valueLossCoeff, int miniBatchSize) {
        this.stateDim       = stateDim;
        this.actionDim      = actionDim;
        this.rolloutSteps   = rolloutSteps;
        this.gamma          = gamma;
        this.gaeLambda      = gaeLambda;
        this.valueLossCoeff = valueLossCoeff;
        this.miniBatchSize  = miniBatchSize;

        actor      = new PolicyNetwork(stateDim, actionDim);
        critic     = new ValueNetwork(stateDim);
        rollout    = new RolloutBuffer(rolloutSteps, gamma, gaeLambda, true);
        intrinsic  = new IntrinsicMotivation(stateDim, actionDim);
        actorSched = LearningRateScheduler.cosineAnnealing(actorLr, actorLr * 0.01f, 200_000);
        criticSched= LearningRateScheduler.cosineAnnealing(criticLr, criticLr * 0.01f, 200_000);
        clipper    = new GradientClipper(GradientClipper.Strategy.GLOBAL_NORM, 0.5f);

        Log.i(TAG, "A2CAgent created: stateDim=" + stateDim + " actionDim=" + actionDim
                + " rolloutSteps=" + rolloutSteps);
    }

    /** Convenience constructor with sensible defaults. */
    public ActorCriticAgent(int stateDim, int actionDim) {
        this(stateDim, actionDim, 128, 0.99f, 0.95f, 3e-4f, 1e-3f, 0.5f, 32);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Action selection
    // ─────────────────────────────────────────────────────────────────────────

    /** Sample action from π(a|s). Thread-safe. */
    public synchronized int selectAction(float[] state) {
        return actor.sampleAction(state);
    }

    /** Greedy action for evaluation. */
    public synchronized int greedyAction(float[] state) {
        return actor.greedyAction(state);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Step observation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Observe a transition. Triggers an update when the rollout buffer is full.
     *
     * @return Actor loss from the update, or 0 if no update occurred.
     */
    public synchronized float observe(float[] state, int action, float reward,
                                       float[] nextState, boolean done) {
        totalStepCount.incrementAndGet();

        // Intrinsic bonus
        float intBonus = intrinsic.computeBonus(state, action, nextState,
                actor.entropy(state));
        float shapedReward = reward + intBonus;
        episodeReturn += reward;

        float   logProb = actor.logProb(state, action);
        float   value   = critic.getValue(state);
        lastValue       = done ? 0f : critic.getValue(nextState);
        lastDone        = done;

        rollout.add(state, action, shapedReward, nextState, done, logProb, value);

        if (done) {
            episodeCount.incrementAndGet();
            avgEpisodeReturn = 0.99f * avgEpisodeReturn + 0.01f * episodeReturn;
            episodeReturn    = 0f;
        }

        if (rollout.isFull() || done) {
            return trainOnRollout();
        }
        return 0f;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Training
    // ─────────────────────────────────────────────────────────────────────────

    private float trainOnRollout() {
        rollout.computeReturnsAndAdvantages(lastDone ? 0f : lastValue);
        List<RolloutBuffer.Step> all = rollout.getAllSteps();
        if (all.isEmpty()) { rollout.reset(); return 0f; }

        float totalActorLoss  = 0f;
        float totalCriticLoss = 0f;
        int   batches         = 0;

        // Multiple mini-batch passes
        for (int pass = 0; pass < 4; pass++) {
            List<RolloutBuffer.Step> batch = rollout.sampleMiniBatch(miniBatchSize);
            if (batch.isEmpty()) continue;

            float batchAL = 0f, batchCL = 0f;
            for (RolloutBuffer.Step step : batch) {
                batchAL += actor.update(step.state, step.action, step.advantage);
                batchCL += critic.update(step.state, step.returnVal, step.value);
            }
            totalActorLoss  += batchAL  / batch.size();
            totalCriticLoss += batchCL  / batch.size();
            batches++;
        }

        rollout.reset();

        if (batches > 0) {
            avgActorLoss  = 0.95f * avgActorLoss  + 0.05f * (totalActorLoss / batches);
            avgCriticLoss = 0.95f * avgCriticLoss + 0.05f * (totalCriticLoss / batches);
        }

        actorSched.step();
        criticSched.step();
        updateCount.incrementAndGet();

        if (updateCount.get() % 100 == 0) {
            Log.d(TAG, "Update #" + updateCount.get()
                    + " actorLoss=" + String.format("%.4f", avgActorLoss)
                    + " criticLoss=" + String.format("%.4f", avgCriticLoss)
                    + " avgReturn=" + String.format("%.2f", avgEpisodeReturn)
                    + " ev=" + String.format("%.3f", critic.explainedVariance()));
        }
        return avgActorLoss;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("updateCount",       updateCount.get());
        s.put("episodeCount",      episodeCount.get());
        s.put("totalSteps",        totalStepCount.get());
        s.put("avgActorLoss",      avgActorLoss);
        s.put("avgCriticLoss",     avgCriticLoss);
        s.put("avgEpisodeReturn",  avgEpisodeReturn);
        s.put("explainedVariance", critic.explainedVariance());
        s.put("actorLr",           actorSched.getLr());
        s.put("criticLr",          criticSched.getLr());
        s.put("clipRate",          clipper.getClipRate());
        s.put("intrinsic",         intrinsic.getStats());
        s.put("rollout",           rollout.getStats());
        return s;
    }
}
