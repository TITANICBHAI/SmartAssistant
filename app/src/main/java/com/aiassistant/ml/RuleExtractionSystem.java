package com.aiassistant.ml;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aiassistant.core.AIController.GameType;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rule Extraction System — replaces the near-empty original with a real
 * IF-THEN rule learner driven by observation history.
 *
 * Core features:
 *  1. Observation ingestion — {@link #observe(Map, String, float)} stores
 *     (state, action, reward) tuples in a rolling buffer.
 *  2. Rule induction — background thread mines frequent (condition → action)
 *     pairs from the buffer using a simplified APRIORI-style count.
 *  3. Rule scoring — each rule tracks support (observation frequency),
 *     confidence (action success rate), and a combined score.
 *  4. JSON persistence — rules survive app restarts; loaded on init.
 *  5. Rule query — {@link #findRelevantRules(Map, Object)} returns all rules
 *     whose conditions match the current state, sorted by score descending.
 *  6. Rule pruning — rules below MIN_CONFIDENCE or MAX_AGE_DAYS are removed.
 *  7. Adaptive context keys — only numeric and boolean state values are used
 *     as rule conditions to avoid high-cardinality string pollution.
 */
public class RuleExtractionSystem {
    private static final String TAG = "RuleExtractionSystem";

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------
    private static final int    MAX_OBSERVATIONS  = 2_000;
    private static final int    MAX_RULES         = 500;
    private static final float  MIN_CONFIDENCE    = 0.40f;
    private static final float  MIN_SUPPORT       = 3;       // minimum hits to form a rule
    private static final int    MINE_INTERVAL_SEC = 60;      // mine every 60 s
    private static final int    MAX_AGE_DAYS      = 30;
    private static final String RULES_FILE        = "rules.json";
    private static final String PREFS_NAME        = "rule_extraction";

    // -----------------------------------------------------------------------
    // Singleton
    // -----------------------------------------------------------------------
    private static volatile RuleExtractionSystem instance;

    public static RuleExtractionSystem getInstance(Context context) {
        if (instance == null) {
            synchronized (RuleExtractionSystem.class) {
                if (instance == null) instance = new RuleExtractionSystem(context);
            }
        }
        return instance;
    }

    // -----------------------------------------------------------------------
    // Models
    // -----------------------------------------------------------------------

    /** A single observation: state features + chosen action + reward signal. */
    private static class Observation {
        final Map<String, Object> state;
        final String              action;
        final float               reward;
        final long                timestamp;

        Observation(Map<String, Object> state, String action, float reward) {
            this.state     = new HashMap<>(state);
            this.action    = action;
            this.reward    = reward;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /** An IF (conditions) THEN (action) rule. */
    public static class GameRule {
        public enum RuleType { PATTERN, CONTEXTUAL, STRATEGY, GOAL, RESOURCE, CAUSAL, FEEDBACK, CUSTOM }

        final String              id;
        final RuleType            type;
        final GameType            gameType;
        final Map<String, Object> conditions;  // feature → threshold (Number) or value (Boolean)
        final String              action;
        float   confidence;
        float   importance;
        int     support;
        long    createdAt;
        long    lastMatchedAt;
        String  description;

        GameRule(RuleType type, GameType gameType, Map<String, Object> conditions,
                 String action, float confidence, float importance) {
            this.id           = UUID.randomUUID().toString();
            this.type         = type;
            this.gameType     = gameType;
            this.conditions   = new HashMap<>(conditions);
            this.action       = action;
            this.confidence   = confidence;
            this.importance   = importance;
            this.support      = 1;
            this.createdAt    = System.currentTimeMillis();
            this.lastMatchedAt = this.createdAt;
            this.description  = buildDescription();
        }

        /** Returns true if all conditions match the given state. */
        public boolean matches(Map<String, Object> state) {
            for (Map.Entry<String, Object> e : conditions.entrySet()) {
                Object sv = state.get(e.getKey());
                if (sv == null) return false;
                Object cond = e.getValue();
                if (cond instanceof Number && sv instanceof Number) {
                    // Condition stores a threshold; match if state value >= threshold
                    if (((Number) sv).doubleValue() < ((Number) cond).doubleValue()) return false;
                } else if (cond instanceof Boolean) {
                    if (!cond.equals(sv)) return false;
                } else {
                    if (!cond.toString().equals(sv.toString())) return false;
                }
            }
            lastMatchedAt = System.currentTimeMillis();
            return true;
        }

        /** Combined score: 60% confidence + 40% importance, boosted by support. */
        public float score() {
            float supportBonus = Math.min(1f, support / 10f) * 0.1f;
            return 0.6f * confidence + 0.4f * importance + supportBonus;
        }

        private String buildDescription() {
            StringBuilder sb = new StringBuilder("IF ");
            int i = 0;
            for (Map.Entry<String, Object> e : conditions.entrySet()) {
                if (i++ > 0) sb.append(" AND ");
                sb.append(e.getKey()).append('≥').append(e.getValue());
            }
            sb.append(" THEN ").append(action);
            return sb.toString();
        }

        public String getId()          { return id; }
        public RuleType getType()      { return type; }
        public GameType getGameType()  { return gameType; }
        public String getAction()      { return action; }
        public float getConfidence()   { return confidence; }
        public float getImportance()   { return importance; }
        public String getDescription() { return description; }
        public Map<String, Object> getParameters() { return conditions; }
    }

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------
    private final Context                      context;
    private final AtomicBoolean                running = new AtomicBoolean(false);
    private final AtomicInteger                totalRulesExtracted = new AtomicInteger(0);
    private final AtomicInteger                patternMatches      = new AtomicInteger(0);
    private final Map<String, GameRule>        rules               = new ConcurrentHashMap<>();
    private final List<Observation>            observations        = Collections.synchronizedList(new ArrayList<>());
    private ScheduledExecutorService           scheduler;
    private String                             gameType            = "unknown";
    private GameType                           currentGameType     = GameType.OTHER;

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------
    public RuleExtractionSystem(Context context) {
        this.context = context != null ? context.getApplicationContext() : null;
        if (context != null) loadRules();
    }

    public RuleExtractionSystem() { this.context = null; }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------
    public void start() {
        if (!running.compareAndSet(false, true)) return;
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::mineRules,
                MINE_INTERVAL_SEC, MINE_INTERVAL_SEC, TimeUnit.SECONDS);
        Log.i(TAG, "RuleExtractionSystem started for game=" + gameType);
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) return;
        if (scheduler != null) scheduler.shutdown();
        saveRules();
        Log.i(TAG, "RuleExtractionSystem stopped. rules=" + rules.size());
    }

    // -----------------------------------------------------------------------
    // Observation API
    // -----------------------------------------------------------------------

    /**
     * Record a new observation.
     * @param state  Numeric/boolean features of the current state
     * @param action Action name that was taken
     * @param reward Reward received (can be normalised 0-1 or raw)
     */
    public void observe(@NonNull Map<String, Object> state,
                        @NonNull String action,
                        float reward) {
        synchronized (observations) {
            if (observations.size() >= MAX_OBSERVATIONS) observations.remove(0);
            observations.add(new Observation(state, action, reward));
        }
    }

    // -----------------------------------------------------------------------
    // Rule query
    // -----------------------------------------------------------------------

    /**
     * Returns rules that match the given state, sorted by score descending.
     * @param state  Current state features
     * @param filter Ignored (kept for API compatibility)
     */
    public List<GameRule> findRelevantRules(@NonNull Map<String, Object> state,
                                            @Nullable Object filter) {
        List<GameRule> matched = new ArrayList<>();
        for (GameRule rule : rules.values()) {
            if (rule.matches(state)) {
                matched.add(rule);
                patternMatches.incrementAndGet();
            }
        }
        Collections.sort(matched, (a, b) -> Float.compare(b.score(), a.score()));
        return matched;
    }

    /** Returns the single best rule for the state, or null if none match. */
    @Nullable
    public GameRule getBestRule(Map<String, Object> state) {
        List<GameRule> matched = findRelevantRules(state, null);
        return matched.isEmpty() ? null : matched.get(0);
    }

    // -----------------------------------------------------------------------
    // Rule induction (background)
    // -----------------------------------------------------------------------

    /**
     * Simple rule miner: groups observations by action, then for each action
     * finds feature thresholds that predict positive reward.
     */
    private void mineRules() {
        List<Observation> snap;
        synchronized (observations) { snap = new ArrayList<>(observations); }
        if (snap.size() < MIN_SUPPORT * 2) return;

        // Group by action
        Map<String, List<Observation>> byAction = new HashMap<>();
        for (Observation o : snap) {
            byAction.computeIfAbsent(o.action, k -> new ArrayList<>()).add(o);
        }

        for (Map.Entry<String, List<Observation>> entry : byAction.entrySet()) {
            String         action = entry.getKey();
            List<Observation> obs = entry.getValue();
            if (obs.size() < MIN_SUPPORT) continue;

            // Collect numeric/boolean features
            Map<String, List<Double>> numeric  = new HashMap<>();
            Map<String, Integer>      boolTrue = new HashMap<>();
            Map<String, Integer>      boolCnt  = new HashMap<>();
            int positiveCount = 0;

            for (Observation o : obs) {
                if (o.reward > 0) positiveCount++;
                for (Map.Entry<String, Object> fe : o.state.entrySet()) {
                    Object v = fe.getValue();
                    if (v instanceof Number) {
                        numeric.computeIfAbsent(fe.getKey(), k -> new ArrayList<>())
                               .add(((Number) v).doubleValue());
                    } else if (v instanceof Boolean) {
                        boolCnt.merge(fe.getKey(),  1, Integer::sum);
                        if ((Boolean) v) boolTrue.merge(fe.getKey(), 1, Integer::sum);
                    }
                }
            }

            float baseConf = (float) positiveCount / obs.size();
            if (baseConf < MIN_CONFIDENCE) continue;

            // Build conditions: use median value of positive-reward obs as threshold
            Map<String, Object> conditions = new HashMap<>();
            for (Map.Entry<String, List<Double>> ne : numeric.entrySet()) {
                List<Double> vals = ne.getValue();
                Collections.sort(vals);
                double median = vals.get(vals.size() / 2);
                conditions.put(ne.getKey(), median);
            }
            for (Map.Entry<String, Integer> be : boolCnt.entrySet()) {
                int trueN = boolTrue.getOrDefault(be.getKey(), 0);
                if ((float) trueN / be.getValue() > 0.6f) {
                    conditions.put(be.getKey(), true);
                }
            }
            if (conditions.isEmpty()) continue;

            // Check if a matching rule already exists; update it, else create
            boolean found = false;
            for (GameRule r : rules.values()) {
                if (r.action.equals(action) && r.conditions.equals(conditions)) {
                    r.support++;
                    r.confidence = Math.max(r.confidence, baseConf);
                    found = true;
                    break;
                }
            }
            if (!found && rules.size() < MAX_RULES) {
                GameRule rule = new GameRule(
                        GameRule.RuleType.CAUSAL,
                        currentGameType,
                        conditions, action,
                        baseConf,
                        Math.min(1f, obs.size() / 20f));
                rule.support = obs.size();
                rules.put(rule.id, rule);
                totalRulesExtracted.incrementAndGet();
                Log.d(TAG, "New rule: " + rule.description);
            }
        }

        pruneRules();
        saveRules();
    }

    /** Remove expired or low-quality rules. */
    private void pruneRules() {
        long now     = System.currentTimeMillis();
        long maxAge  = MAX_AGE_DAYS * 86_400_000L;
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, GameRule> e : rules.entrySet()) {
            GameRule r = e.getValue();
            if (r.confidence < MIN_CONFIDENCE) toRemove.add(e.getKey());
            if (now - r.createdAt > maxAge)    toRemove.add(e.getKey());
        }
        for (String k : toRemove) rules.remove(k);
        if (!toRemove.isEmpty()) Log.d(TAG, "Pruned " + toRemove.size() + " rules");
    }

    // -----------------------------------------------------------------------
    // Persistence
    // -----------------------------------------------------------------------

    private void saveRules() {
        if (context == null) return;
        try {
            JSONArray arr = new JSONArray();
            for (GameRule r : rules.values()) {
                JSONObject obj = new JSONObject();
                obj.put("id",         r.id);
                obj.put("action",     r.action);
                obj.put("confidence", r.confidence);
                obj.put("importance", r.importance);
                obj.put("support",    r.support);
                obj.put("createdAt",  r.createdAt);
                obj.put("type",       r.type.name());
                obj.put("gameType",   r.gameType.name());
                JSONObject conds = new JSONObject();
                for (Map.Entry<String, Object> ce : r.conditions.entrySet())
                    conds.put(ce.getKey(), ce.getValue().toString());
                obj.put("conditions", conds);
                arr.put(obj);
            }
            File f = new File(context.getFilesDir(), RULES_FILE);
            try (FileWriter fw = new FileWriter(f)) { fw.write(arr.toString(2)); }
            Log.d(TAG, "Saved " + rules.size() + " rules");
        } catch (Exception e) {
            Log.e(TAG, "Error saving rules", e);
        }
    }

    private void loadRules() {
        if (context == null) return;
        File f = new File(context.getFilesDir(), RULES_FILE);
        if (!f.exists()) return;
        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line; while ((line = br.readLine()) != null) sb.append(line);
            }
            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj     = arr.getJSONObject(i);
                JSONObject condsJ  = obj.getJSONObject("conditions");
                Map<String, Object> conds = new HashMap<>();
                Iterator<String> keys = condsJ.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    String v = condsJ.getString(k);
                    try { conds.put(k, Double.parseDouble(v)); }
                    catch (NumberFormatException ex) {
                        if ("true".equalsIgnoreCase(v) || "false".equalsIgnoreCase(v))
                             conds.put(k, Boolean.parseBoolean(v));
                        else conds.put(k, v);
                    }
                }
                GameType gt; try { gt = GameType.valueOf(obj.optString("gameType", "OTHER")); }
                catch (Exception ex) { gt = GameType.OTHER; }
                GameRule.RuleType rt; try { rt = GameRule.RuleType.valueOf(obj.optString("type", "CAUSAL")); }
                catch (Exception ex) { rt = GameRule.RuleType.CAUSAL; }

                GameRule rule = new GameRule(rt, gt, conds,
                        obj.getString("action"),
                        (float) obj.optDouble("confidence", 0.5),
                        (float) obj.optDouble("importance", 0.5));
                rule.support   = obj.optInt("support", 1);
                rule.createdAt = obj.optLong("createdAt", System.currentTimeMillis());
                rules.put(rule.id, rule);
            }
            Log.d(TAG, "Loaded " + rules.size() + " rules from disk");
        } catch (Exception e) {
            Log.e(TAG, "Error loading rules", e);
        }
    }

    // -----------------------------------------------------------------------
    // Configuration & stats
    // -----------------------------------------------------------------------

    public void setGameType(String gameType) {
        this.gameType = gameType;
        try { currentGameType = GameType.valueOf(gameType.toUpperCase()); }
        catch (Exception ignored) { currentGameType = GameType.OTHER; }
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("rulesExtracted",      totalRulesExtracted.get());
        stats.put("activeRules",         rules.size());
        stats.put("patternMatches",      patternMatches.get());
        stats.put("observations",        observations.size());
        stats.put("isRunning",           running.get());
        stats.put("gameType",            gameType);
        return stats;
    }

    public int  getRuleCount()            { return rules.size(); }
    public boolean isRunning()            { return running.get(); }
}
