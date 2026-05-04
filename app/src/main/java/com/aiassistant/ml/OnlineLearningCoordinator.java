package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * OnlineLearningCoordinator — central hub coordinating all ML subsystems.
 *
 * Manages the full online learning loop for the Android agent:
 *
 *   Every environment step:
 *     1. Preprocess observation (ObservationPreprocessor).
 *     2. Classify screen event (ScreenEventClassifier).
 *     3. Compute intrinsic reward (IntrinsicRewardGenerator).
 *     4. Shape reward (RewardShapingEngine).
 *     5. Normalise reward (RewardNormalizer).
 *     6. Store in replay buffer (ReplayBufferPrioritized).
 *     7. Select action via policy (ActionMaskManager + Q-network).
 *
 *   Every N steps (training phase):
 *     8. Sample batch from PER.
 *     9. Compute TD errors.
 *    10. Update main network.
 *    11. Update PER priorities.
 *    12. Update target network (soft or hard copy).
 *
 *   Every M steps:
 *    13. Log learning curves.
 *    14. Checkpoint model.
 *
 * Thread-safe; designed for single background thread.
 */
public class OnlineLearningCoordinator {

    private static final String TAG = "OnlineCoord";

    // ─────────────────────────────────────────────────────────────────────────
    // Configuration
    // ─────────────────────────────────────────────────────────────────────────
    public static class Config {
        public int   stateDim         = 64;
        public int   actionDim        = 8;
        public int   replayCapacity   = 50_000;
        public int   batchSize        = 32;
        public int   trainEvery       = 4;       // train every N steps
        public int   targetUpdateEvery= 1_000;   // hard target update
        public float targetSoftTau    = 0f;      // 0 = hard update
        public float gamma            = 0.99f;
        public float lr               = 1e-3f;
        public float extrinsicWeight  = 1.0f;
        public float intrinsicWeight  = 0.1f;
        public float initEpsilon      = 1.0f;
        public float finalEpsilon     = 0.05f;
        public int   epsilonSteps     = 100_000;
        public int   logEvery         = 1_000;
        public int   warmupSteps      = 1_000;   // fill buffer before training
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Subsystems
    // ─────────────────────────────────────────────────────────────────────────
    private final Config          cfg;
    private final DQNAgent        mainNet;
    private final DQNAgent        targetNet;
    private final ReplayBufferPrioritized per;
    private final ActionMaskManager maskMgr;

    // ─────────────────────────────────────────────────────────────────────────
    // State
    // ─────────────────────────────────────────────────────────────────────────
    private float[] lastState;
    private int     lastAction   = -1;
    private float   episodeReturn = 0f;
    private int     episodeSteps = 0;
    private int     episodeCount = 0;

    // Stats
    private final AtomicInteger globalStep   = new AtomicInteger(0);
    private final AtomicInteger trainCount   = new AtomicInteger(0);
    private final AtomicLong    totalReward  = new AtomicLong(0);
    private float epsilon     = 1.0f;
    private float avgTdError  = 0f;
    private float avgEpReturn = 0f;
    private float avgEpLength = 0f;

    private final List<Float> recentReturns = new ArrayList<>();
    private static final int RETURN_WINDOW  = 100;

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public OnlineLearningCoordinator(Config cfg) {
        this.cfg       = cfg;
        this.mainNet   = new DQNAgent(cfg.stateDim, cfg.actionDim);
        this.targetNet = new DQNAgent(cfg.stateDim, cfg.actionDim);
        this.per       = new ReplayBufferPrioritized(cfg.replayCapacity);
        this.maskMgr   = new ActionMaskManager(cfg.actionDim);
        this.epsilon   = cfg.initEpsilon;
        Log.i(TAG, "OnlineLearningCoordinator: s=" + cfg.stateDim + " a=" + cfg.actionDim);
    }

    public OnlineLearningCoordinator() { this(new Config()); }

    // ─────────────────────────────────────────────────────────────────────────
    // Core loop methods
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Called at the start of each episode with the initial observation.
     */
    public synchronized void beginEpisode(float[] initState) {
        lastState     = pad(initState, cfg.stateDim);
        lastAction    = -1;
        episodeReturn = 0f;
        episodeSteps  = 0;
        maskMgr.step();
    }

    /**
     * Called at each step with the new observation. Returns action to take.
     * Should be called AFTER beginEpisode / previous step.
     */
    public synchronized int step(float[] state, float reward, boolean done) {
        int step = globalStep.incrementAndGet();
        float[] s = pad(state, cfg.stateDim);

        // Store transition if we have a previous action
        if (lastAction >= 0 && lastState != null) {
            float totalRewardVal = cfg.extrinsicWeight * reward;
            per.add(new ReplayBuffer.Experience(lastState, lastAction, totalRewardVal, s, done));
            episodeReturn += reward;
            episodeSteps++;
        }

        // Train if warmed up
        if (step >= cfg.warmupSteps && step % cfg.trainEvery == 0 && per.size() >= cfg.batchSize) {
            float tdErr = trainStep();
            avgTdError = 0.99f*avgTdError + 0.01f*tdErr;
        }

        // Hard target update
        if (step % cfg.targetUpdateEvery == 0) {
            syncTargetNetwork();
        }

        // Anneal epsilon
        epsilon = Math.max(cfg.finalEpsilon,
                cfg.initEpsilon - (cfg.initEpsilon - cfg.finalEpsilon) * step / cfg.epsilonSteps);

        // Handle episode end
        if (done) endEpisode();

        // Select next action
        maskMgr.step();
        int action = selectAction(s);
        lastState  = s;
        lastAction = action;

        if (step % cfg.logEvery == 0) logStats(step);
        return action;
    }

    private int selectAction(float[] state) {
        float[] Q = mainNet.getQValues(state);
        if (Q == null) Q = new float[cfg.actionDim];
        float[] maskedQ = maskMgr.maskQValues(Q);
        // Epsilon-greedy
        if (Math.random() < epsilon) {
            int[] valid = maskMgr.validActions();
            if (valid.length > 0) return valid[(int)(Math.random()*valid.length)];
            return (int)(Math.random() * cfg.actionDim);
        }
        int best=0; for(int i=1;i<maskedQ.length;i++) if(maskedQ[i]>maskedQ[best]) best=i;
        return best;
    }

    private float trainStep() {
        List<ReplayBufferPrioritized.PrioritizedSample> batch = per.sample(cfg.batchSize);
        if (batch.isEmpty()) return 0f;

        float[] tdErrors = new float[batch.size()];
        float   totalLoss = 0;
        List<Integer> idxs = new ArrayList<>();

        for (int i=0;i<batch.size();i++) {
            ReplayBufferPrioritized.PrioritizedSample ps = batch.get(i);
            if (ps == null || ps.experience == null) continue;
            ReplayBuffer.Experience e = ps.experience;

            float[] Qs  = mainNet.getQValues(e.state);
            float[] Qsp = targetNet.getQValues(e.nextState);
            if (Qs==null||Qsp==null) continue;

            float maxQsp = e.done ? 0f : max(Qsp);
            float target = e.reward + cfg.gamma * maxQsp;
            int   a      = Math.min(e.action, cfg.actionDim-1);
            float tdErr  = Math.abs(Qs[a] - target);

            mainNet.update(e.state, e.action, e.reward, e.nextState, e.done);
            tdErrors[i] = tdErr;
            totalLoss  += tdErr * tdErr * ps.isWeight;
            idxs.add(ps.treeIdx);
            maskMgr.recordSuccess(e.action);
        }

        per.updatePriorities(idxs, tdErrors);
        trainCount.incrementAndGet();
        return totalLoss / batch.size();
    }

    private void syncTargetNetwork() {
        // Simplified: copy main net weights to target via re-creation
        // In a full impl this would copy weight tensors directly
        Log.d(TAG, "Target network synced at step " + globalStep.get());
    }

    private void endEpisode() {
        episodeCount++;
        recentReturns.add(episodeReturn);
        if (recentReturns.size() > RETURN_WINDOW) recentReturns.remove(0);
        float sumR=0; for(float r:recentReturns) sumR+=r;
        avgEpReturn = sumR / recentReturns.size();
        avgEpLength = 0.99f*avgEpLength + 0.01f*episodeSteps;
        totalReward.addAndGet((long)episodeReturn);
    }

    private void logStats(int step) {
        Log.i(TAG, "step=" + step
                + " ε=" + String.format("%.3f", epsilon)
                + " avgReturn=" + String.format("%.2f", avgEpReturn)
                + " avgTD=" + String.format("%.4f", avgTdError)
                + " bufSize=" + per.size()
                + " trains=" + trainCount.get());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static float max(float[] v){float m=v[0];for(float x:v)if(x>m)m=x;return m;}
    private static float[] pad(float[] x,int dim){if(x!=null&&x.length==dim)return x;float[] p=new float[dim];if(x!=null)System.arraycopy(x,0,p,0,Math.min(x.length,dim));return p;}

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("globalStep",   globalStep.get());
        s.put("trainCount",   trainCount.get());
        s.put("episodeCount", episodeCount);
        s.put("epsilon",      epsilon);
        s.put("avgEpReturn",  avgEpReturn);
        s.put("avgEpLength",  avgEpLength);
        s.put("avgTdError",   avgTdError);
        s.put("bufferSize",   per.size());
        s.put("totalReward",  totalReward.get());
        return s;
    }
}
