package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PopulationBasedTraining (PBT) — parallel hyperparameter evolution for RL.
 *
 * PBT (Jaderberg et al. 2017) maintains a population of agents with different
 * hyperparameters. Periodically:
 *   1. EXPLOIT: Replace bottom-20% agents with copies of top-20% agents.
 *   2. EXPLORE:  Perturb hyperparameters of replaced agents (±20% or resample).
 *
 * Hyperparameters managed: learning rate, discount factor, entropy coefficient,
 * clipping epsilon (PPO), reward shaping weight.
 *
 * On-device simplification: agents share the same weights but maintain independent
 * hyperparameter vectors and score tracking. The "agent" is a thin config wrapper.
 *
 * Thread-safe.
 */
public class PopulationBasedTraining {

    private static final String TAG = "PBT";

    // ─────────────────────────────────────────────────────────────────────────
    // Hyperparameter config per agent
    // ─────────────────────────────────────────────────────────────────────────
    public static class AgentConfig {
        public final int   id;
        public float learningRate;
        public float gamma;
        public float entropyCoeff;
        public float clipEpsilon;   // PPO ε
        public float shapingWeight;

        // Performance tracking
        public float score      = 0f;
        public float avgScore   = 0f;
        public int   generation = 0;
        public int   steps      = 0;

        public AgentConfig(int id, float lr, float gamma, float entropy,
                           float clip, float shaping) {
            this.id           = id;
            this.learningRate = lr;
            this.gamma        = gamma;
            this.entropyCoeff = entropy;
            this.clipEpsilon  = clip;
            this.shapingWeight= shaping;
        }

        public AgentConfig copy(int newId) {
            AgentConfig c = new AgentConfig(newId, learningRate, gamma,
                    entropyCoeff, clipEpsilon, shapingWeight);
            c.score = score; c.avgScore = avgScore;
            return c;
        }

        @Override public String toString() {
            return String.format("Agent[%d] lr=%.4f γ=%.3f ε=%.3f score=%.2f gen=%d",
                    id, learningRate, gamma, clipEpsilon, avgScore, generation);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final List<AgentConfig> population;
    private final int    popSize;
    private final int    exploitInterval;   // steps between exploit/explore
    private final float  exploitFraction;   // bottom fraction to replace
    private final float  perturbFactor;     // ±factor for hyperparams

    private final AtomicInteger globalStep   = new AtomicInteger(0);
    private final AtomicInteger exploitCount = new AtomicInteger(0);
    private float avgPopScore = 0f;
    private float bestScore   = Float.NEGATIVE_INFINITY;
    private AgentConfig bestConfig = null;

    private final Random rng;

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public PopulationBasedTraining(int popSize, int exploitInterval,
                                    float exploitFraction, float perturbFactor,
                                    long seed) {
        this.popSize         = popSize;
        this.exploitInterval = exploitInterval;
        this.exploitFraction = exploitFraction;
        this.perturbFactor   = perturbFactor;
        this.rng             = new Random(seed);

        population = new ArrayList<>(popSize);
        for (int i = 0; i < popSize; i++) {
            population.add(new AgentConfig(i,
                    sampleLr(),
                    0.95f + rng.nextFloat() * 0.04f,   // gamma in [0.95, 0.99]
                    rng.nextFloat() * 0.05f,             // entropy [0, 0.05]
                    0.1f + rng.nextFloat() * 0.1f,      // clip [0.1, 0.2]
                    rng.nextFloat() * 0.5f));            // shaping [0, 0.5]
        }
        Log.i(TAG, "PBT initialized: pop=" + popSize + " interval=" + exploitInterval);
    }

    public PopulationBasedTraining(int popSize) {
        this(popSize, 1000, 0.2f, 0.2f, 73L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /** Record a score for a specific agent and trigger exploit/explore if due. */
    public synchronized void recordScore(int agentId, float score) {
        if (agentId < 0 || agentId >= population.size()) return;
        AgentConfig cfg = population.get(agentId);
        cfg.score   = score;
        cfg.avgScore= 0.9f * cfg.avgScore + 0.1f * score;
        cfg.steps++;
        globalStep.incrementAndGet();
        avgPopScore = 0.99f * avgPopScore + 0.01f * score;

        if (score > bestScore) { bestScore = score; bestConfig = cfg.copy(-1); }

        if (cfg.steps % exploitInterval == 0) exploitExplore();
    }

    /** Get the current config for an agent. */
    public synchronized AgentConfig getConfig(int agentId) {
        return population.get(agentId);
    }

    /** Get the best-performing config found so far. */
    public synchronized AgentConfig getBestConfig() { return bestConfig; }

    /** All agent configs (read-only snapshot). */
    public synchronized List<AgentConfig> getPopulation() {
        return new ArrayList<>(population);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Exploit / Explore
    // ─────────────────────────────────────────────────────────────────────────

    private void exploitExplore() {
        // Sort by avgScore descending
        population.sort((a, b) -> Float.compare(b.avgScore, a.avgScore));

        int nExploit = Math.max(1, (int)(popSize * exploitFraction));
        int nTop     = Math.max(1, (int)(popSize * exploitFraction));

        for (int i = popSize - nExploit; i < popSize; i++) {
            // Replace with copy of top agent
            int    topIdx = rng.nextInt(nTop);
            AgentConfig src = population.get(topIdx);
            AgentConfig dst = population.get(i);

            dst.learningRate  = perturb(src.learningRate,  true);
            dst.gamma         = clamp(perturb(src.gamma,         false), 0.9f, 0.999f);
            dst.entropyCoeff  = clamp(perturb(src.entropyCoeff,  false), 0f,   0.1f);
            dst.clipEpsilon   = clamp(perturb(src.clipEpsilon,   false), 0.05f,0.3f);
            dst.shapingWeight = clamp(perturb(src.shapingWeight, false), 0f,   1f);
            dst.generation    = src.generation + 1;
            dst.score         = 0f;
            dst.avgScore      = src.avgScore * 0.5f;

            Log.d(TAG, "Exploit: agent[" + dst.id + "] ← agent[" + src.id + "] "
                    + "lr=" + String.format("%.5f", dst.learningRate));
        }
        exploitCount.incrementAndGet();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private float perturb(float val, boolean logScale) {
        float factor = 1f + perturbFactor * (rng.nextFloat() * 2f - 1f);
        return logScale ? val * factor : val * factor;
    }

    private float sampleLr() {
        return (float) Math.pow(10, -(3 + rng.nextFloat() * 2)); // [1e-5, 1e-3]
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("popSize",      popSize);
        s.put("globalStep",   globalStep.get());
        s.put("exploitCount", exploitCount.get());
        s.put("avgPopScore",  avgPopScore);
        s.put("bestScore",    bestScore);
        if (bestConfig != null) s.put("bestConfig", bestConfig.toString());
        return s;
    }
}
