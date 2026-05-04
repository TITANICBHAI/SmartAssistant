package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CurriculumLearner — automatic curriculum generation for progressive RL training.
 *
 * Implements multiple curriculum strategies for gradually increasing task difficulty:
 *
 *   FIXED_SEQUENCE:
 *     Pre-defined difficulty levels, advance when mastery criterion met.
 *
 *   SELF_PACED (SPL):
 *     Agent selects tasks it finds neither too easy nor too hard
 *     (optimal learning zone: ~70-80% success rate).
 *
 *   AUTOMATIC_CURRICULUM (ALP-GMM):
 *     Track absolute learning progress (ALP = |Δ success rate|).
 *     Sample tasks from regions of high ALP (most learnable).
 *
 *   GOAL_GENERATION (GoalGAN):
 *     Train a generator to produce goals at the frontier of competence:
 *     - Too easy (>80% success): discard.
 *     - Too hard (<20% success): discard.
 *     - Intermediate: keep for training.
 *
 *   REVERSE_CURRICULUM:
 *     Start from near-goal states, gradually back away (Florensa et al.).
 *
 * Difficulty parameters: episode length limit, enemy count, obstacle density, etc.
 * Thread-safe.
 */
public class CurriculumLearner {

    private static final String TAG = "CurriculumLearner";

    public enum Strategy { FIXED_SEQUENCE, SELF_PACED, AUTOMATIC_CURRICULUM,
                           GOAL_GENERATION, REVERSE_CURRICULUM }

    // ─────────────────────────────────────────────────────────────────────────
    // Difficulty level
    // ─────────────────────────────────────────────────────────────────────────
    public static class Level {
        public final int    id;
        public final String name;
        public final float  difficulty;  // [0,1] normalized difficulty
        public final Map<String, Float> params;  // task parameters

        float successRate   = 0f;
        float prevSuccess   = 0f;
        float alp           = 0f;   // absolute learning progress
        int   attempts      = 0;

        public Level(int id, String name, float difficulty, Map<String, Float> params) {
            this.id = id; this.name = name;
            this.difficulty = difficulty;
            this.params = params != null ? params : new HashMap<String, Float>();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final List<Level>  levels;
    private final Strategy     strategy;
    private final float        masteryThresh;  // success rate for advancement
    private final float        easyThresh, hardThresh;  // GoalGAN filtering
    private       int          currentLevel = 0;

    // ALP-GMM: Gaussian mixture model over difficulty space (simplified: bins)
    private final float[] alpBins;
    private static final int NUM_BINS = 20;

    // Reverse curriculum: start state distance from goal
    private float reverseDistance = 0.1f;   // starts close, increases
    private final float reverseStep = 0.05f;

    // Stats
    private final AtomicInteger levelAdvances = new AtomicInteger(0);
    private final AtomicInteger episodeCount  = new AtomicInteger(0);
    private float avgSuccessRate = 0f;
    private float avgAlp         = 0f;

    private final Random rng = new Random(337L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public CurriculumLearner(List<Level> levels, Strategy strategy,
                              float masteryThresh, float easyThresh, float hardThresh) {
        this.levels        = new ArrayList<>(levels);
        this.strategy      = strategy;
        this.masteryThresh = masteryThresh;
        this.easyThresh    = easyThresh;
        this.hardThresh    = hardThresh;
        this.alpBins       = new float[NUM_BINS];

        Log.i(TAG, "CurriculumLearner: " + strategy + " levels=" + levels.size()
                + " mastery=" + masteryThresh);
    }

    /** Factory: create a simple N-level curriculum. */
    public static CurriculumLearner createSimple(int numLevels, Strategy strategy) {
        List<Level> lvls = new ArrayList<>();
        for (int i=0;i<numLevels;i++) {
            Map<String, Float> params = new HashMap<>();
            params.put("difficulty", (float)i / (numLevels - 1));
            params.put("maxSteps",   50f + 50f * i);
            params.put("enemies",    (float) i);
            lvls.add(new Level(i, "Level " + (i+1), (float)i/(numLevels-1), params));
        }
        return new CurriculumLearner(lvls, strategy, 0.75f, 0.8f, 0.2f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /** Select the next level/task for the agent to train on. */
    public synchronized Level selectLevel() {
        if (levels.isEmpty()) return null;
        switch (strategy) {
            case SELF_PACED:          return selfPacedLevel();
            case AUTOMATIC_CURRICULUM:return alpLevel();
            case GOAL_GENERATION:     return goalGenLevel();
            case REVERSE_CURRICULUM:  return levels.get(0);  // always start; distance varies
            case FIXED_SEQUENCE:
            default:                  return levels.get(Math.min(currentLevel, levels.size()-1));
        }
    }

    /** Record episode outcome. */
    public synchronized void recordEpisode(int levelId, boolean success, float episodeReturn) {
        if (levelId < 0 || levelId >= levels.size()) return;
        Level lvl = levels.get(levelId);
        lvl.prevSuccess  = lvl.successRate;
        lvl.successRate  = 0.9f * lvl.successRate + 0.1f * (success ? 1f : 0f);
        lvl.alp          = Math.abs(lvl.successRate - lvl.prevSuccess);
        lvl.attempts++;
        episodeCount.incrementAndGet();
        avgSuccessRate = 0.99f * avgSuccessRate + 0.01f * lvl.successRate;
        avgAlp         = 0.99f * avgAlp         + 0.01f * lvl.alp;

        // Update ALP bin
        int bin = Math.min(NUM_BINS-1, (int)(lvl.difficulty * NUM_BINS));
        alpBins[bin] = 0.9f * alpBins[bin] + 0.1f * lvl.alp;

        // Fixed sequence: check mastery
        if (strategy == Strategy.FIXED_SEQUENCE) {
            if (lvl.successRate >= masteryThresh && currentLevel < levels.size()-1) {
                currentLevel++;
                levelAdvances.incrementAndGet();
                Log.i(TAG, "Advanced to level " + currentLevel + ": " + levels.get(currentLevel).name);
            }
        }
        // Reverse curriculum: expand distance when succeeding
        if (strategy == Strategy.REVERSE_CURRICULUM && success) {
            reverseDistance = Math.min(1f, reverseDistance + reverseStep);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Selection strategies
    // ─────────────────────────────────────────────────────────────────────────

    private Level selfPacedLevel() {
        // Find levels with success rate in [0.2, 0.8] (optimal zone)
        List<Level> optimal = new ArrayList<>();
        for (Level l : levels) if (l.successRate >= 0.2f && l.successRate <= 0.8f) optimal.add(l);
        if (!optimal.isEmpty()) return optimal.get(rng.nextInt(optimal.size()));
        // Fallback: lowest difficulty not yet mastered
        for (Level l : levels) if (l.successRate < masteryThresh) return l;
        return levels.get(levels.size()-1);
    }

    private Level alpLevel() {
        // Sample bin proportionally to ALP
        float total = 0; for (float a : alpBins) total += a;
        if (total <= 0) return levels.get(rng.nextInt(levels.size()));
        float r = rng.nextFloat() * total, cum = 0;
        int targetBin = NUM_BINS - 1;
        for (int i=0;i<NUM_BINS-1;i++) { cum += alpBins[i]; if (r < cum) { targetBin = i; break; } }
        float targetDiff = (targetBin + 0.5f) / NUM_BINS;
        // Find closest level
        Level best = levels.get(0); float bestD = Float.MAX_VALUE;
        for (Level l : levels) { float d=Math.abs(l.difficulty-targetDiff); if(d<bestD){bestD=d;best=l;} }
        return best;
    }

    private Level goalGenLevel() {
        // GoalGAN: sample only intermediate success rate levels
        List<Level> frontier = new ArrayList<>();
        for (Level l : levels)
            if (l.successRate >= hardThresh && l.successRate <= easyThresh) frontier.add(l);
        if (!frontier.isEmpty()) return frontier.get(rng.nextInt(frontier.size()));
        return levels.get(rng.nextInt(levels.size()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Queries
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized int getCurrentLevelIndex()  { return currentLevel; }
    public synchronized float getReverseDistance()  { return reverseDistance; }
    public synchronized List<Level> getLevels()     { return new ArrayList<>(levels); }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("strategy",      strategy.name());
        s.put("numLevels",     levels.size());
        s.put("currentLevel",  currentLevel);
        s.put("levelAdvances", levelAdvances.get());
        s.put("episodeCount",  episodeCount.get());
        s.put("avgSuccessRate",avgSuccessRate);
        s.put("avgAlp",        avgAlp);
        s.put("reverseDistance",reverseDistance);
        return s;
    }
}
