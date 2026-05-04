package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SelfPlayManager — self-play training for competitive and cooperative tasks.
 *
 * Self-play (Silver et al. AlphaGo, OpenAI Five) enables an agent to learn
 * without a fixed opponent by playing against past versions of itself.
 *
 * Strategies:
 *
 *   LATEST (SP):
 *     Always play against the current policy. Fast but unstable (cycles).
 *
 *   FICTITIOUS_PLAY (FP):
 *     Play against a uniform mixture of all past policies.
 *     Converges to Nash equilibrium in two-player zero-sum games.
 *
 *   PSRO (Policy Space Response Oracles):
 *     Maintain a meta-game payoff matrix; compute Nash equilibrium over
 *     it to weight opponent sampling.
 *
 *   LEAGUE (AlphaStar-style):
 *     Three agent types: Main (exploits all), Main Exploiter (beats main),
 *     League Exploiter (beats everyone). Sampling weights tuned by wins.
 *
 * Population management:
 *   - Add checkpoint every N episodes.
 *   - Maintain Elo ratings for all population members.
 *   - Sample opponents proportionally to Elo distance from current agent.
 *
 * Thread-safe.
 */
public class SelfPlayManager {

    private static final String TAG = "SelfPlay";

    public enum Strategy { LATEST, FICTITIOUS_PLAY, PSRO, LEAGUE }
    public enum Outcome  { WIN, LOSS, DRAW }

    // ─────────────────────────────────────────────────────────────────────────
    // Population member (past policy checkpoint)
    // ─────────────────────────────────────────────────────────────────────────
    public static class PolicyCheckpoint {
        public final int     id;
        public final int     episode;
        public final float[] weights;   // flattened policy weights
        public       float   elo;
        public       int     wins, losses, draws;
        public       float   samplingWeight = 1f;

        public PolicyCheckpoint(int id, int episode, float[] weights, float elo) {
            this.id = id; this.episode = episode;
            this.weights = weights; this.elo = elo;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final List<PolicyCheckpoint> population = new ArrayList<>();
    private final Strategy   strategy;
    private final int        maxPopSize;
    private final int        checkpointEvery;
    private final float      eloK;          // Elo update constant
    private final float      initElo;

    private int   episodeCount     = 0;
    private int   nextId           = 0;
    private float currentAgentElo  = 1500f;

    // PSRO payoff matrix (simplified: last N×N win rates)
    private static final int MAX_PAYOFF_SIZE = 32;
    private final float[][] payoff = new float[MAX_PAYOFF_SIZE][MAX_PAYOFF_SIZE];

    // Stats
    private final AtomicInteger totalGames    = new AtomicInteger(0);
    private final AtomicInteger selfPlayWins  = new AtomicInteger(0);
    private float avgEloGain = 0f;
    private float winRate    = 0f;

    private final Random rng = new Random(373L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public SelfPlayManager(Strategy strategy, int maxPopSize, int checkpointEvery,
                            float eloK, float initElo) {
        this.strategy        = strategy;
        this.maxPopSize      = maxPopSize;
        this.checkpointEvery = checkpointEvery;
        this.eloK            = eloK;
        this.initElo         = initElo;
        this.currentAgentElo = initElo;
        Log.i(TAG, "SelfPlayManager: " + strategy + " maxPop=" + maxPopSize);
    }

    public SelfPlayManager(Strategy strategy) {
        this(strategy, 50, 1000, 32f, 1500f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Add a checkpoint of the current policy to the population.
     * @param weights Flattened policy weights.
     */
    public synchronized PolicyCheckpoint addCheckpoint(float[] weights) {
        PolicyCheckpoint cp = new PolicyCheckpoint(nextId++, episodeCount,
                weights.clone(), currentAgentElo);
        population.add(cp);
        // Trim if over max size (remove oldest / lowest Elo)
        while (population.size() > maxPopSize) {
            population.remove(selectWorst());
        }
        updateSamplingWeights();
        Log.i(TAG, "Checkpoint " + cp.id + " added. Population: " + population.size());
        return cp;
    }

    /**
     * Should we add a checkpoint this episode?
     */
    public synchronized boolean shouldCheckpoint() {
        episodeCount++;
        return episodeCount % checkpointEvery == 0;
    }

    /**
     * Sample an opponent from the population.
     * @return Sampled checkpoint, or null if population is empty.
     */
    public synchronized PolicyCheckpoint sampleOpponent() {
        if (population.isEmpty()) return null;
        switch (strategy) {
            case LATEST:         return population.get(population.size()-1);
            case FICTITIOUS_PLAY:return sampleUniform();
            case PSRO:           return sampleNash();
            case LEAGUE:         return sampleLeague();
            default:             return sampleUniform();
        }
    }

    /**
     * Record game outcome vs a checkpoint opponent and update Elo.
     */
    public synchronized void recordOutcome(int opponentId, Outcome outcome) {
        PolicyCheckpoint opp = findById(opponentId);
        if (opp == null) return;

        float expected = eloExpected(currentAgentElo, opp.elo);
        float score    = outcome == Outcome.WIN ? 1f : outcome == Outcome.DRAW ? 0.5f : 0f;

        float prevElo = currentAgentElo;
        currentAgentElo += eloK * (score - expected);
        opp.elo         -= eloK * (score - expected);

        switch (outcome) {
            case WIN:  opp.losses++;  selfPlayWins.incrementAndGet(); break;
            case LOSS: opp.wins++;    break;
            case DRAW: opp.draws++;   break;
        }

        avgEloGain = 0.99f * avgEloGain + 0.01f * (currentAgentElo - prevElo);
        winRate    = 0.99f * winRate    + 0.01f * score;
        totalGames.incrementAndGet();
        updateSamplingWeights();

        // Update payoff matrix
        int oppIdx = Math.min(population.indexOf(opp), MAX_PAYOFF_SIZE-1);
        if (oppIdx >= 0) payoff[0][oppIdx] = 0.99f*payoff[0][oppIdx]+0.01f*score;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sampling
    // ─────────────────────────────────────────────────────────────────────────

    private PolicyCheckpoint sampleUniform() {
        return population.get(rng.nextInt(population.size()));
    }

    private PolicyCheckpoint sampleNash() {
        // Simplified: sample proportional to sampling weights
        float total=0; for(PolicyCheckpoint p:population) total+=p.samplingWeight;
        float r=rng.nextFloat()*total, cum=0;
        for(PolicyCheckpoint p:population){cum+=p.samplingWeight;if(r<cum)return p;}
        return population.get(population.size()-1);
    }

    private PolicyCheckpoint sampleLeague() {
        // LEAGUE: prefer opponents close to current Elo (challenging but beatable)
        float bestScore = Float.NEGATIVE_INFINITY;
        PolicyCheckpoint best = population.get(0);
        for (PolicyCheckpoint p : population) {
            float eloDiff = Math.abs(currentAgentElo - p.elo);
            float score = -(eloDiff - 200f)*(eloDiff - 200f) + rng.nextFloat()*50f;
            if (score > bestScore) { bestScore=score; best=p; }
        }
        return best;
    }

    private int selectWorst() {
        int worst=0;
        for(int i=1;i<population.size();i++)
            if(population.get(i).elo<population.get(worst).elo) worst=i;
        return worst;
    }

    private void updateSamplingWeights() {
        // PSRO: Nash weights; simplified: weight by Elo proximity to current agent
        float total=0;
        for(PolicyCheckpoint p:population){
            p.samplingWeight=1f/(1f+Math.abs(currentAgentElo-p.elo)*0.01f);
            total+=p.samplingWeight;
        }
        if(total>0) for(PolicyCheckpoint p:population) p.samplingWeight/=total;
    }

    private static float eloExpected(float r1, float r2) {
        return 1f/(1f+(float)Math.pow(10f,(r2-r1)/400f));
    }

    private PolicyCheckpoint findById(int id) {
        for(PolicyCheckpoint p:population) if(p.id==id) return p;
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Queries
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized float getCurrentElo()     { return currentAgentElo; }
    public synchronized int   getPopulationSize()  { return population.size(); }
    public synchronized List<PolicyCheckpoint> getPopulation() { return new ArrayList<>(population); }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("strategy",      strategy.name());
        s.put("populationSize",population.size());
        s.put("totalGames",    totalGames.get());
        s.put("selfPlayWins",  selfPlayWins.get());
        s.put("currentElo",    currentAgentElo);
        s.put("avgEloGain",    avgEloGain);
        s.put("winRate",       winRate);
        s.put("episodeCount",  episodeCount);
        return s;
    }
}
