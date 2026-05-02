package com.aiassistant.core;

import android.util.Log;

import com.aiassistant.ml.ActionPrioritization;
import com.aiassistant.ml.AnomalyDetector;
import com.aiassistant.ml.GameRuleUnderstanding;
import com.aiassistant.ml.PredictiveActionSystem;
import com.aiassistant.ml.RuleExtractionSystem;
import com.aiassistant.rl.AlgorithmSelector;
import com.aiassistant.rl.RLAgent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Unified Decision Engine — aggregates signals from all AI subsystems and
 * returns a single recommended action index.
 *
 * Architecture:
 *  • Four signal sources with configurable weights:
 *      1. RL agent (AlgorithmSelector-chosen DQN/PPO/Q-Learning/SARSA)
 *      2. PredictiveActionSystem (Thompson-Sampling contextual bandit)
 *      3. RuleExtractionSystem (IF-THEN matched rules)
 *      4. GameRuleUnderstanding (causal rule confidence)
 *  • Weighted-voting fusion: each source votes for its preferred action
 *    with a weight equal to its configured weight × its internal confidence.
 *  • Emergency override: if the AnomalyDetector reports an active anomaly
 *    in any critical feature, a pre-configured safety action is injected.
 *  • Outcome feedback loop: the caller reports the actual reward after each
 *    action via {@link #recordOutcome}; this updates source weights through
 *    an exponential moving average of per-source correctness.
 *  • Decision logging: last N decisions are kept for introspection.
 *  • Thread-safe; can be called from any thread.
 */
public class DecisionEngine {
    private static final String TAG           = "DecisionEngine";
    private static final int    MAX_LOG       = 100;
    private static final float  EMA_ALPHA     = 0.1f;   // weight-update learning rate
    private static final float  MIN_WEIGHT    = 0.05f;

    // -----------------------------------------------------------------------
    // Decision record
    // -----------------------------------------------------------------------
    public static class DecisionRecord {
        public final int              chosenAction;
        public final Map<String, Integer> sourceVotes;    // source → voted action
        public final Map<String, Float>   sourceWeights;  // source → effective weight
        public final float            confidence;          // 0-1
        public final long             timestamp;
        public final boolean          emergencyOverride;
        public       float            actualReward = Float.NaN;

        DecisionRecord(int action, Map<String, Integer> votes,
                       Map<String, Float> weights, float conf, boolean emergency) {
            this.chosenAction    = action;
            this.sourceVotes     = new HashMap<>(votes);
            this.sourceWeights   = new HashMap<>(weights);
            this.confidence      = conf;
            this.timestamp       = System.currentTimeMillis();
            this.emergencyOverride = emergency;
        }
    }

    // -----------------------------------------------------------------------
    // Signal source descriptor
    // -----------------------------------------------------------------------
    private static class Source {
        final String name;
        volatile float weight;
        volatile float emaCorrectness = 0.5f; // starts neutral

        Source(String name, float initialWeight) {
            this.name   = name;
            this.weight = initialWeight;
        }
    }

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------
    private final RLAgent                   rlAgent;
    private final PredictiveActionSystem    predictive;
    private final RuleExtractionSystem      ruleSystem;
    private final GameRuleUnderstanding     ruleUnderstanding;
    private final AnomalyDetector           anomalyDetector;

    private final Source srcRL    = new Source("RL",   0.40f);
    private final Source srcPred  = new Source("PRED", 0.30f);
    private final Source srcRule  = new Source("RULE", 0.20f);
    private final Source srcGame  = new Source("GAME", 0.10f);

    private final int                       actionSize;
    private       int                       safetyAction = 0;  // fallback action on anomaly
    private final List<String>              criticalFeatures   = new ArrayList<>();

    private final List<DecisionRecord>      log            = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger             decisionCount  = new AtomicInteger(0);
    private final AtomicLong                lastDecisionMs = new AtomicLong(0);

    // Map decision index → source that recommended it (for feedback)
    private final ConcurrentHashMap<Integer, String> pendingFeedback = new ConcurrentHashMap<>();

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------
    public DecisionEngine(RLAgent rlAgent,
                          PredictiveActionSystem predictive,
                          RuleExtractionSystem ruleSystem,
                          GameRuleUnderstanding ruleUnderstanding,
                          int actionSize) {
        this.rlAgent          = rlAgent;
        this.predictive       = predictive;
        this.ruleSystem       = ruleSystem;
        this.ruleUnderstanding = ruleUnderstanding;
        this.anomalyDetector  = AnomalyDetector.getInstance();
        this.actionSize       = Math.max(1, actionSize);
        Log.i(TAG, "DecisionEngine created actionSize=" + actionSize);
    }

    // -----------------------------------------------------------------------
    // Primary API
    // -----------------------------------------------------------------------

    /**
     * Returns the recommended action index for the given state.
     *
     * @param state        Numeric state features (also used by AnomalyDetector)
     * @param stateMap     Richer state map for rule/predictive systems
     * @return             Best action index in [0, actionSize)
     */
    public int decide(float[] state, Map<String, Object> stateMap) {
        lastDecisionMs.set(System.currentTimeMillis());

        // 1. Anomaly check — safety override
        boolean emergency = false;
        if (!criticalFeatures.isEmpty()) {
            Map<String, Boolean> anomalies = anomalyDetector.observeAll(stateMap);
            for (String cf : criticalFeatures) {
                if (Boolean.TRUE.equals(anomalies.get(cf))) { emergency = true; break; }
            }
        }
        if (emergency) {
            Log.w(TAG, "Anomaly detected — emergency action=" + safetyAction);
            recordDecision(safetyAction, Collections.emptyMap(), Collections.emptyMap(),
                           0f, true);
            return safetyAction;
        }

        // 2. Gather votes from each source
        float[] votes = new float[actionSize]; // accumulated weighted score per action
        Map<String, Integer> sourceVotes   = new HashMap<>();
        Map<String, Float>   sourceWeights = new HashMap<>();

        // RL agent
        if (rlAgent != null && state != null) {
            int a = clamp(rlAgent.selectAction(state));
            float w = srcRL.weight;
            votes[a] += w; sourceVotes.put("RL", a); sourceWeights.put("RL", w);
        }

        // PredictiveActionSystem
        if (predictive != null && stateMap != null) {
            int a = clamp(predictive.selectAction(stateMap));
            float w = srcPred.weight;
            votes[a] += w; sourceVotes.put("PRED", a); sourceWeights.put("PRED", w);
        }

        // RuleExtractionSystem
        if (ruleSystem != null && stateMap != null) {
            int a = ruleActionFromSystem(stateMap);
            if (a >= 0) {
                float w = srcRule.weight;
                votes[a] += w; sourceVotes.put("RULE", a); sourceWeights.put("RULE", w);
            }
        }

        // GameRuleUnderstanding — boost RL choice if it matches a confident rule
        if (ruleUnderstanding != null) {
            int a = sourceVotes.getOrDefault("RL", 0);
            List<GameRuleUnderstanding.GameRule> rules =
                    ruleUnderstanding.getAllRules();
            if (!rules.isEmpty()) {
                float conf = rules.get(0).getConfidence();
                votes[clamp(a)] += srcGame.weight * conf;
                sourceVotes.put("GAME", a); sourceWeights.put("GAME", srcGame.weight * conf);
            }
        }

        // 3. Pick highest-scored action
        int   best = 0;
        float bv   = votes[0];
        for (int i = 1; i < votes.length; i++) if (votes[i] > bv) { bv = votes[i]; best = i; }

        // 4. Confidence = winning score / total score
        float total = 0; for (float v : votes) total += v;
        float conf  = total > 0 ? bv / total : 0f;

        decisionCount.incrementAndGet();
        recordDecision(best, sourceVotes, sourceWeights, conf, false);

        Log.d(TAG, "Decision #" + decisionCount.get() + " action=" + best
                + " conf=" + String.format("%.2f", conf)
                + " votes=" + formatVotes(votes));
        return best;
    }

    // -----------------------------------------------------------------------
    // Outcome feedback
    // -----------------------------------------------------------------------

    /**
     * Reports the actual reward for the most recent decision at the given index.
     * Updates per-source EMA correctness scores and adjusts weights.
     *
     * @param decisionIndex Zero-based index into the decision log (0 = most recent)
     * @param reward        Actual reward received in [0, 1]
     */
    public void recordOutcome(int decisionIndex, float reward) {
        synchronized (log) {
            int idx = log.size() - 1 - decisionIndex;
            if (idx < 0 || idx >= log.size()) return;
            DecisionRecord rec = log.get(idx);
            rec.actualReward = reward;

            // Update per-source correctness based on whether source voted for the chosen action
            for (Source src : new Source[]{srcRL, srcPred, srcRule, srcGame}) {
                Integer voted = rec.sourceVotes.get(src.name);
                if (voted == null) continue;
                // Correctness: was this source's vote the same as the chosen action that got reward?
                float correct = (voted == rec.chosenAction) ? reward : 1f - reward;
                src.emaCorrectness = (1 - EMA_ALPHA) * src.emaCorrectness + EMA_ALPHA * correct;
            }
            rebalanceWeights();
        }

        // Also notify predictive system
        if (predictive != null) {
            Map<String, Object> emptyCtx = Collections.emptyMap();
            predictive.recordOutcome(emptyCtx, decisionIndex % actionSize, reward);
        }
    }

    // -----------------------------------------------------------------------
    // Weight rebalancing
    // -----------------------------------------------------------------------
    private void rebalanceWeights() {
        Source[] sources = {srcRL, srcPred, srcRule, srcGame};
        float total = 0;
        float[] raw = new float[sources.length];
        for (int i = 0; i < sources.length; i++) {
            raw[i] = Math.max(MIN_WEIGHT, sources[i].emaCorrectness);
            total += raw[i];
        }
        for (int i = 0; i < sources.length; i++) sources[i].weight = raw[i] / total;
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private int ruleActionFromSystem(Map<String, Object> stateMap) {
        if (ruleSystem == null) return -1;
        List<RuleExtractionSystem.GameRule> matched = ruleSystem.findRelevantRules(stateMap, null);
        if (matched.isEmpty()) return -1;
        String actionStr = matched.get(0).getAction();
        // Simple heuristic: hash the action name to an index
        return Math.abs(actionStr.hashCode()) % actionSize;
    }

    private int clamp(int a) { return Math.max(0, Math.min(actionSize - 1, a)); }

    private void recordDecision(int action,
                                Map<String, Integer> votes,
                                Map<String, Float> weights,
                                float conf, boolean emergency) {
        DecisionRecord rec = new DecisionRecord(action, votes, weights, conf, emergency);
        synchronized (log) {
            log.add(rec);
            if (log.size() > MAX_LOG) log.remove(0);
        }
    }

    private String formatVotes(float[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(String.format("%.2f", v[i]));
        }
        return sb.append(']').toString();
    }

    // -----------------------------------------------------------------------
    // Configuration
    // -----------------------------------------------------------------------
    public void setSafetyAction(int action) { safetyAction = clamp(action); }
    public void addCriticalFeature(String f) { criticalFeatures.add(f); }
    public void clearCriticalFeatures()      { criticalFeatures.clear(); }

    public void setSourceWeights(float wRL, float wPred, float wRule, float wGame) {
        float s = wRL + wPred + wRule + wGame;
        if (s <= 0) return;
        srcRL.weight   = wRL   / s;
        srcPred.weight = wPred / s;
        srcRule.weight = wRule / s;
        srcGame.weight = wGame / s;
    }

    // -----------------------------------------------------------------------
    // Stats
    // -----------------------------------------------------------------------
    public Map<String, Object> getStats() {
        Map<String, Object> m = new HashMap<>();
        m.put("decisions",    decisionCount.get());
        m.put("lastDecisionMs", lastDecisionMs.get());
        m.put("weights", Map.of(
            "RL",   srcRL.weight,
            "PRED", srcPred.weight,
            "RULE", srcRule.weight,
            "GAME", srcGame.weight));
        m.put("emaCorrectness", Map.of(
            "RL",   srcRL.emaCorrectness,
            "PRED", srcPred.emaCorrectness,
            "RULE", srcRule.emaCorrectness,
            "GAME", srcGame.emaCorrectness));
        m.put("logSize", log.size());
        return m;
    }

    public DecisionRecord getLastDecision() {
        synchronized (log) {
            return log.isEmpty() ? null : log.get(log.size() - 1);
        }
    }

    public List<DecisionRecord> getDecisionLog(int n) {
        synchronized (log) {
            int from = Math.max(0, log.size() - n);
            return new ArrayList<>(log.subList(from, log.size()));
        }
    }
}
