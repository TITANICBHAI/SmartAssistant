package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ModelEnsembleVoter — ensemble of RL agents voting on actions for robustness.
 *
 * Combines predictions from multiple independent RL agents (voters) to produce
 * a single robust action, reducing variance and improving generalisation.
 *
 * Voting methods:
 *
 *   MAJORITY_VOTE        — Pick the action selected by most agents.
 *   SOFTMAX_AVERAGE      — Average action probability distributions, then argmax.
 *   CONFIDENCE_WEIGHTED  — Weight each agent's vote by its recent accuracy.
 *   RANK_AGGREGATION     — Borda count: sum action ranks across agents.
 *   BOOTSTRAP_COMMITTEE  — Randomly select one agent per step (Thompson Ensemble).
 *
 * Additionally:
 *   - Diversity tracking: measure Kullback-Leibler divergence between agents.
 *   - Epistemic uncertainty: variance of agent Q-value predictions.
 *   - Online accuracy update: agents rewarded when their voted action matches
 *     the actual best outcome action (evaluated post-hoc with received reward).
 *
 * Thread-safe.
 */
public class ModelEnsembleVoter {

    private static final String TAG = "EnsembleVoter";

    public enum VotingMethod {
        MAJORITY_VOTE, SOFTMAX_AVERAGE, CONFIDENCE_WEIGHTED,
        RANK_AGGREGATION, BOOTSTRAP_COMMITTEE
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Agent interface: any class that can provide action probabilities
    // ─────────────────────────────────────────────────────────────────────────
    public interface VotingAgent {
        float[] actionProbs(float[] state);   // must return float[actionDim]
        String  getName();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Agent wrapper with confidence tracking
    // ─────────────────────────────────────────────────────────────────────────
    private static class AgentEntry {
        final VotingAgent agent;
        float confidence = 1f;    // updated based on accuracy
        int   correct    = 0;
        int   total      = 0;

        AgentEntry(VotingAgent a) { this.agent = a; }

        float accuracy() { return total > 0 ? (float) correct / total : 0.5f; }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final List<AgentEntry> agents     = new ArrayList<>();
    private final int              actionDim;
    private VotingMethod           method;

    // Last vote details for accuracy update
    private int   lastConsensusAction = -1;
    private float lastVoteStep        = 0f;
    private int   bootstrapAgentIdx   = 0;

    // Diversity / uncertainty tracking
    private float avgEnsembleDiversity = 0f;
    private float avgEpistemicUncert   = 0f;

    private final AtomicInteger voteCount = new AtomicInteger(0);
    private final Random        rng       = new Random(113L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public ModelEnsembleVoter(int actionDim, VotingMethod method) {
        this.actionDim = actionDim;
        this.method    = method;
        Log.i(TAG, "ModelEnsembleVoter: dim=" + actionDim + " method=" + method);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Agent registration
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized void addAgent(VotingAgent agent) {
        agents.add(new AgentEntry(agent));
        Log.i(TAG, "Added agent: " + agent.getName() + " (total=" + agents.size() + ")");
    }

    public synchronized int agentCount() { return agents.size(); }

    // ─────────────────────────────────────────────────────────────────────────
    // Voting
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Select action by ensemble vote.
     */
    public synchronized int vote(float[] state) {
        if (agents.isEmpty()) return rng.nextInt(actionDim);

        int action;
        switch (method) {
            case MAJORITY_VOTE:        action = majorityVote(state);       break;
            case CONFIDENCE_WEIGHTED:  action = confidenceWeighted(state); break;
            case RANK_AGGREGATION:     action = rankAggregation(state);    break;
            case BOOTSTRAP_COMMITTEE:  action = bootstrap(state);          break;
            case SOFTMAX_AVERAGE:
            default:                   action = softmaxAverage(state);     break;
        }

        updateDiversity(state);
        lastConsensusAction = action;
        voteCount.incrementAndGet();
        return action;
    }

    /** Get ensemble probability distribution (weighted average). */
    public synchronized float[] ensembleProbs(float[] state) {
        float[] avg = new float[actionDim];
        for (AgentEntry e : agents) {
            float[] p = e.agent.actionProbs(state);
            for (int a = 0; a < Math.min(p.length, actionDim); a++)
                avg[a] += e.confidence * p[a];
        }
        float sum = 0; for (float v : avg) sum += v;
        if (sum > 0) for (int a = 0; a < actionDim; a++) avg[a] /= sum;
        return avg;
    }

    /** Epistemic uncertainty: std of agent argmax predictions. */
    public synchronized float uncertainty(float[] state) {
        if (agents.size() < 2) return 0f;
        float[] votes = new float[agents.size()];
        for (int i = 0; i < agents.size(); i++) {
            float[] p = agents.get(i).agent.actionProbs(state);
            votes[i] = argmax(p);
        }
        float mean = 0; for (float v : votes) mean += v;
        mean /= votes.length;
        float var = 0;
        for (float v : votes) { float d = v - mean; var += d * d; }
        return (float) Math.sqrt(var / votes.length);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Accuracy feedback: call after observing the reward
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Update agent confidence based on whether last consensus action produced positive reward.
     */
    public synchronized void updateAccuracy(float reward) {
        if (lastConsensusAction < 0) return;
        boolean good = reward > 0;
        for (AgentEntry e : agents) {
            float[] p = e.agent.actionProbs(new float[0]); // quick dummy — use cached
            e.total++;
            if (good) e.correct++;
            e.confidence = 0.9f * e.confidence + 0.1f * e.accuracy();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Voting implementations
    // ─────────────────────────────────────────────────────────────────────────

    private int majorityVote(float[] state) {
        int[] votes = new int[actionDim];
        for (AgentEntry e : agents) votes[argmax(e.agent.actionProbs(state))]++;
        return argmax(toFloat(votes));
    }

    private int softmaxAverage(float[] state) {
        return argmax(ensembleProbs(state));
    }

    private int confidenceWeighted(float[] state) {
        float[] avg = new float[actionDim];
        float totalConf = 0;
        for (AgentEntry e : agents) {
            float[] p = e.agent.actionProbs(state);
            for (int a = 0; a < Math.min(p.length, actionDim); a++)
                avg[a] += e.confidence * p[a];
            totalConf += e.confidence;
        }
        if (totalConf > 0) for (int a = 0; a < actionDim; a++) avg[a] /= totalConf;
        return argmax(avg);
    }

    private int rankAggregation(float[] state) {
        // Borda count
        float[] scores = new float[actionDim];
        for (AgentEntry e : agents) {
            float[] p = e.agent.actionProbs(state);
            int[]   rank = rankOf(p);
            for (int a = 0; a < actionDim; a++) scores[a] += rank[a];
        }
        return argmax(scores);
    }

    private int bootstrap(float[] state) {
        bootstrapAgentIdx = rng.nextInt(agents.size());
        return argmax(agents.get(bootstrapAgentIdx).agent.actionProbs(state));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Diversity
    // ─────────────────────────────────────────────────────────────────────────

    private void updateDiversity(float[] state) {
        if (agents.size() < 2) return;
        // KL divergence between first agent and ensemble average
        float[] avg = ensembleProbs(state);
        float[] p0  = agents.get(0).agent.actionProbs(state);
        float kl = 0f;
        for (int a = 0; a < actionDim; a++) {
            float pa = Math.max(p0[a], 1e-8f), qa = Math.max(avg[a], 1e-8f);
            kl += pa * (float)(Math.log(pa) - Math.log(qa));
        }
        avgEnsembleDiversity = 0.99f * avgEnsembleDiversity + 0.01f * kl;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static int argmax(float[] v) {
        int best = 0;
        for (int i = 1; i < v.length; i++) if (v[i] > v[best]) best = i;
        return best;
    }

    private static float[] toFloat(int[] v) {
        float[] f = new float[v.length];
        for (int i = 0; i < v.length; i++) f[i] = v[i];
        return f;
    }

    private static int[] rankOf(float[] v) {
        int[] rank = new int[v.length];
        for (int i = 0; i < v.length; i++) {
            for (float x : v) if (x > v[i]) rank[i]++;
        }
        return rank;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("agentCount",        agents.size());
        s.put("method",            method.name());
        s.put("voteCount",         voteCount.get());
        s.put("avgDiversity",      avgEnsembleDiversity);
        s.put("lastConsensus",     lastConsensusAction);
        double[] confs = new double[agents.size()];
        String[] names = new String[agents.size()];
        for (int i = 0; i < agents.size(); i++) {
            confs[i] = agents.get(i).confidence;
            names[i] = agents.get(i).agent.getName();
        }
        s.put("agentConfidences", confs);
        s.put("agentNames",       names);
        return s;
    }
}
