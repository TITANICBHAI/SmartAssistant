package com.aiassistant.ml;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Game Rule Understanding System — significantly improved over the original stub.
 *
 * The original had empty start/stop methods and no real logic.
 * This rewrite implements:
 *
 *  1. Causal relationship discovery — calling {@link #observeTransition} records
 *     (state, action, nextState, reward) tuples.  A background miner groups
 *     pairs by action and detects consistent cause→effect patterns using
 *     conditional probability: P(effect | cause, action).
 *
 *  2. Rule confidence using Bayesian counts — each candidate rule tracks
 *     hits (cause + effect observed together) and misses (cause observed
 *     without effect).  Confidence = hits / (hits + misses).
 *
 *  3. Rule type classification — after discovery rules are tagged as
 *     OBJECTIVE, CONSTRAINT, MECHANICS, SCORING, TEMPORAL, or SPATIAL
 *     based on which features changed.
 *
 *  4. Query API — {@link #getRulesFor(String)} returns all high-confidence
 *     rules for a given action, sorted by confidence descending.
 *
 *  5. Automatic pruning — rules with confidence < MIN_CONF or age > MAX_AGE
 *     are removed periodically.
 *
 *  6. Singleton with double-checked locking.
 */
public class GameRuleUnderstanding {
    private static final String TAG = "GameRuleUnderstanding";

    private static final float  MIN_CONF          = 0.45f;
    private static final int    MIN_HITS          = 4;
    private static final int    MAX_RULES         = 300;
    private static final long   MAX_AGE_MS        = 7L * 24 * 3_600_000; // 7 days
    private static final int    MAX_OBSERVATIONS  = 3_000;
    private static final int    MINE_INTERVAL_SEC = 30;

    // -----------------------------------------------------------------------
    // Rule types
    // -----------------------------------------------------------------------
    public enum RuleType {
        OBJECTIVE, CONSTRAINT, MECHANICS, SCORING, PROGRESSION,
        INTERACTION, STATE_CHANGE, TEMPORAL, SPATIAL
    }

    // -----------------------------------------------------------------------
    // Rule model
    // -----------------------------------------------------------------------
    public static class GameRule {
        private final String id;
        private final RuleType type;
        private final String description;
        private final String action;        // action that triggers this rule
        private final String causeFeature;  // state feature that must be present
        private final String effectFeature; // state feature that changes
        private float confidence;
        private int   hits;
        private int   misses;
        private int   observationCount;
        private long  createdAt;
        private long  lastMatchedAt;
        private final List<String>         examples   = new ArrayList<>();
        private final Map<String, Object>  parameters = new HashMap<>();

        GameRule(String id, RuleType type, String action,
                 String causeFeature, String effectFeature) {
            this.id            = id;
            this.type          = type;
            this.description   = action + ": " + causeFeature + " → " + effectFeature;
            this.action        = action;
            this.causeFeature  = causeFeature;
            this.effectFeature = effectFeature;
            this.confidence    = 0.5f;
            this.hits          = 0;
            this.misses        = 0;
            this.observationCount = 0;
            this.createdAt     = System.currentTimeMillis();
            this.lastMatchedAt = this.createdAt;
        }

        void recordHit()  {
            hits++;   observationCount++;
            updateConfidence();
            lastMatchedAt = System.currentTimeMillis();
        }
        void recordMiss() { misses++; observationCount++; updateConfidence(); }

        private void updateConfidence() {
            // Laplace-smoothed posterior: (hits+1) / (hits+misses+2)
            confidence = (hits + 1f) / (hits + misses + 2f);
        }

        public float getConfidence()        { return confidence; }
        public String getId()               { return id; }
        public RuleType getType()           { return type; }
        public String getDescription()      { return description; }
        public String getAction()           { return action; }
        public int getObservationCount()    { return observationCount; }
        public void addExample(String ex)   { if (!examples.contains(ex)) examples.add(ex); }
        public List<String> getExamples()   { return examples; }
        public void addParameter(String k, Object v) { parameters.put(k, v); }
        public Map<String, Object> getParameters()   { return parameters; }
        public void incrementObservationCount() { observationCount++; }
        public void setConfidence(float c)      { confidence = Math.max(0f, Math.min(1f, c)); }

        @Override public String toString() {
            return "GameRule{action=" + action + " cause=" + causeFeature
                + " effect=" + effectFeature + " conf=" + String.format("%.2f", confidence) + "}";
        }
    }

    // -----------------------------------------------------------------------
    // Transition record
    // -----------------------------------------------------------------------
    private static class Transition {
        final Map<String, Object> state;
        final String              action;
        final Map<String, Object> nextState;
        final float               reward;
        final long                timestamp;

        Transition(Map<String, Object> s, String a, Map<String, Object> ns, float r) {
            this.state     = new HashMap<>(s);
            this.action    = a;
            this.nextState = new HashMap<>(ns);
            this.reward    = r;
            this.timestamp = System.currentTimeMillis();
        }
    }

    // -----------------------------------------------------------------------
    // Singleton
    // -----------------------------------------------------------------------
    private static volatile GameRuleUnderstanding instance;

    public static GameRuleUnderstanding getInstance(Context context) {
        if (instance == null) {
            synchronized (GameRuleUnderstanding.class) {
                if (instance == null) instance = new GameRuleUnderstanding(context);
            }
        }
        return instance;
    }

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------
    private final Context                      context;
    private final AtomicBoolean                running     = new AtomicBoolean(false);
    private final Map<String, GameRule>        rules       = new ConcurrentHashMap<>();
    private final List<Transition>             transitions = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger                mineCount   = new AtomicInteger(0);
    private ScheduledExecutorService           scheduler;
    private String                             gameType    = "unknown";

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------
    public GameRuleUnderstanding() { this.context = null; }

    public GameRuleUnderstanding(Context context) {
        this.context = context;
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------
    public void start() {
        if (!running.compareAndSet(false, true)) return;
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(
                this::mineRules, MINE_INTERVAL_SEC, MINE_INTERVAL_SEC, TimeUnit.SECONDS);
        Log.i(TAG, "GameRuleUnderstanding started for game=" + gameType);
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) return;
        if (scheduler != null) scheduler.shutdown();
        Log.i(TAG, "GameRuleUnderstanding stopped. rules=" + rules.size());
    }

    public void setGameType(String gameType) {
        this.gameType = gameType != null ? gameType : "unknown";
        Log.d(TAG, "Game type: " + this.gameType);
    }

    // -----------------------------------------------------------------------
    // Observation API
    // -----------------------------------------------------------------------

    /**
     * Records a (state, action, nextState, reward) transition for later analysis.
     */
    public void observeTransition(@NonNull Map<String, Object> state,
                                  @NonNull String action,
                                  @NonNull Map<String, Object> nextState,
                                  float reward) {
        synchronized (transitions) {
            if (transitions.size() >= MAX_OBSERVATIONS) transitions.remove(0);
            transitions.add(new Transition(state, action, nextState, reward));
        }
    }

    // -----------------------------------------------------------------------
    // Query API
    // -----------------------------------------------------------------------

    /**
     * Returns all high-confidence rules for the given action, sorted by
     * confidence descending.
     */
    public List<GameRule> getRulesFor(String action) {
        List<GameRule> result = new ArrayList<>();
        for (GameRule r : rules.values()) {
            if (r.action.equals(action) && r.confidence >= MIN_CONF && r.hits >= MIN_HITS)
                result.add(r);
        }
        Collections.sort(result, (a, b) -> Float.compare(b.confidence, a.confidence));
        return result;
    }

    /** Returns all rules above the confidence threshold, sorted by confidence. */
    public List<GameRule> getAllRules() {
        List<GameRule> list = new ArrayList<>();
        for (GameRule r : rules.values())
            if (r.confidence >= MIN_CONF && r.hits >= MIN_HITS) list.add(r);
        Collections.sort(list, (a, b) -> Float.compare(b.confidence, a.confidence));
        return list;
    }

    /** Returns confidence for a specific (cause → action → effect) triplet, or -1. */
    public float getConfidence(String causeFeature, String action, String effectFeature) {
        for (GameRule r : rules.values()) {
            if (r.action.equals(action)
             && r.causeFeature.equals(causeFeature)
             && r.effectFeature.equals(effectFeature)) return r.confidence;
        }
        return -1f;
    }

    // -----------------------------------------------------------------------
    // Rule mining (background thread)
    // -----------------------------------------------------------------------

    private void mineRules() {
        List<Transition> snap;
        synchronized (transitions) { snap = new ArrayList<>(transitions); }
        if (snap.size() < MIN_HITS * 2) return;

        // Group by action
        Map<String, List<Transition>> byAction = new HashMap<>();
        for (Transition t : snap)
            byAction.computeIfAbsent(t.action, k -> new ArrayList<>()).add(t);

        for (Map.Entry<String, List<Transition>> entry : byAction.entrySet()) {
            String action = entry.getKey();
            List<Transition> ts = entry.getValue();

            // Find features that changed in the next state
            Map<String, Integer> changedCount = new HashMap<>();
            for (Transition t : ts) {
                for (String k : t.nextState.keySet()) {
                    Object before = t.state.get(k);
                    Object after  = t.nextState.get(k);
                    if (before != null && !before.equals(after))
                        changedCount.merge(k, 1, Integer::sum);
                }
            }

            // For each feature pair (causeFeature, changedFeature), compute confidence
            for (Map.Entry<String, Integer> ce : changedCount.entrySet()) {
                String effectFeature = ce.getKey();
                int    changeCount   = ce.getValue();
                if (changeCount < MIN_HITS) continue;

                // Find the most discriminating cause feature
                Map<String, Integer> causeHits   = new HashMap<>();
                Map<String, Integer> causeMisses  = new HashMap<>();
                for (Transition t : ts) {
                    boolean changed = !equals(t.state.get(effectFeature),
                                              t.nextState.get(effectFeature));
                    for (String sf : t.state.keySet()) {
                        if (sf.equals(effectFeature)) continue;
                        Object val = t.state.get(sf);
                        if (!(val instanceof Number)) continue;
                        double v = ((Number) val).doubleValue();
                        // Binarize: feature is "high" if above 0.5
                        if (v >= 0.5) {
                            if (changed) causeHits.merge(sf, 1, Integer::sum);
                            else         causeMisses.merge(sf, 1, Integer::sum);
                        }
                    }
                }

                // Pick best cause feature by Fisher score proxy
                String bestCause = null; float bestConf = MIN_CONF;
                for (String cf : causeHits.keySet()) {
                    int h = causeHits.getOrDefault(cf, 0);
                    int m = causeMisses.getOrDefault(cf, 0);
                    float conf = (h + 1f) / (h + m + 2f);
                    if (conf > bestConf && h >= MIN_HITS) { bestConf = conf; bestCause = cf; }
                }
                if (bestCause == null) continue;

                // Create or update rule
                String ruleKey = action + ":" + bestCause + "→" + effectFeature;
                GameRule rule  = rules.get(ruleKey);
                if (rule == null && rules.size() < MAX_RULES) {
                    RuleType type = classifyRule(effectFeature, changeCount, ts);
                    rule = new GameRule(ruleKey, type, action, bestCause, effectFeature);
                    rules.put(ruleKey, rule);
                    Log.d(TAG, "New rule: " + rule);
                }
                if (rule != null) {
                    int h = causeHits.getOrDefault(bestCause, 0);
                    int m = causeMisses.getOrDefault(bestCause, 0);
                    rule.hits   = h;
                    rule.misses = m;
                    rule.updateConfidence();
                }
            }
        }

        pruneRules();
        mineCount.incrementAndGet();
        Log.d(TAG, "Mining round " + mineCount.get() + ": " + rules.size() + " rules");
    }

    private RuleType classifyRule(String effectFeature, int changeCount,
                                   List<Transition> ts) {
        if (effectFeature.contains("score") || effectFeature.contains("point")) return RuleType.SCORING;
        if (effectFeature.contains("health") || effectFeature.contains("hp"))   return RuleType.CONSTRAINT;
        if (effectFeature.contains("pos") || effectFeature.contains("x")
         || effectFeature.contains("y"))                                         return RuleType.SPATIAL;
        if (effectFeature.contains("time") || effectFeature.contains("timer"))  return RuleType.TEMPORAL;
        return RuleType.MECHANICS;
    }

    private boolean equals(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a instanceof Number && b instanceof Number)
            return Math.abs(((Number)a).doubleValue() - ((Number)b).doubleValue()) < 0.05;
        return a.equals(b);
    }

    private void pruneRules() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, GameRule>> it = rules.entrySet().iterator();
        int removed = 0;
        while (it.hasNext()) {
            GameRule r = it.next().getValue();
            if (r.confidence < MIN_CONF * 0.8f) { it.remove(); removed++; }
            else if (now - r.createdAt > MAX_AGE_MS) { it.remove(); removed++; }
        }
        if (removed > 0) Log.d(TAG, "Pruned " + removed + " rules");
    }

    // -----------------------------------------------------------------------
    // Stats
    // -----------------------------------------------------------------------
    public Map<String, Object> getStats() {
        Map<String, Object> m = new HashMap<>();
        m.put("totalRules",    rules.size());
        m.put("transitions",   transitions.size());
        m.put("miningRounds",  mineCount.get());
        m.put("isRunning",     running.get());
        m.put("gameType",      gameType);
        return m;
    }

    public boolean isRunning() { return running.get(); }
}
