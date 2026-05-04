package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * GameStrategyAnalyzer — high-level game strategy reasoning for RL agents.
 *
 * Extracts and tracks strategic patterns from game play:
 *
 *   PATTERN_RECOGNITION:
 *     Identify recurring state-action sequences that lead to rewards.
 *     Cluster trajectories by outcome using k-means on feature vectors.
 *
 *   OPPONENT_MODELLING:
 *     Predict opponent's next action from their observed history.
 *     Maintain a model of opponent's policy to best respond.
 *
 *   MACRO_ACTION_DISCOVERY:
 *     Identify temporally extended actions (options) that recur across episodes.
 *     Use termination criteria to segment macro-actions.
 *
 *   STRATEGY_PORTFOLIO:
 *     Maintain K strategy templates; select best strategy per game state.
 *     Update strategy weights by win/loss outcomes.
 *
 *   GAME_PHASE_DETECTION:
 *     Classify current game phase (early/mid/late game, crisis, winning).
 *     Phase-conditioned policy selection.
 *
 * Thread-safe.
 */
public class GameStrategyAnalyzer {

    private static final String TAG = "GameStrategy";

    // ─────────────────────────────────────────────────────────────────────────
    // Game phase
    // ─────────────────────────────────────────────────────────────────────────
    public enum GamePhase { EARLY, MID, LATE, CRISIS, WINNING, LOSING, UNKNOWN }

    // ─────────────────────────────────────────────────────────────────────────
    // Strategy template
    // ─────────────────────────────────────────────────────────────────────────
    public static class Strategy {
        public final int    id;
        public final String name;
        public final float[] actionBias;   // prior over actions [actionDim]
        public       float   winRate    = 0.5f;
        public       float   weight     = 1f;
        public       int     uses       = 0;

        public Strategy(int id, String name, float[] actionBias) {
            this.id = id; this.name = name; this.actionBias = actionBias;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pattern cluster
    // ─────────────────────────────────────────────────────────────────────────
    private static class Cluster {
        float[] centroid;   // [stateDim]
        float   avgReward;
        int     count;
        int     actionMode; // most common action in this cluster

        Cluster(float[] centroid, int actionMode) {
            this.centroid = centroid.clone();
            this.actionMode = actionMode;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int    stateDim, actionDim;
    private final int    numStrategies, numClusters;
    private final List<Strategy> strategies  = new ArrayList<>();
    private final List<Cluster>  clusters    = new ArrayList<>();

    // Opponent model: state → opponent action probabilities
    private final float[][] oppW1, oppW2;
    private final float[]   oppB1, oppB2;
    private final NeuralNetworkOptimizer oppOpt;

    // Phase classifier network: state + meta-features → phase
    private final float[][] phaseW1, phaseW2;
    private final float[]   phaseB1, phaseB2;

    // Macro-action tracking
    private int       macroStart = -1;
    private float[]   macroState = null;
    private float     macroReward= 0f;
    private int       macroAction= 0;
    private final List<float[]> macroLib = new ArrayList<>(); // discovered macro-actions

    // Stats
    private final AtomicInteger analysisCount  = new AtomicInteger(0);
    private final AtomicInteger patternMatches = new AtomicInteger(0);
    private GamePhase currentPhase   = GamePhase.UNKNOWN;
    private int       currentStrategy= 0;

    private final Random rng = new Random(401L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public GameStrategyAnalyzer(int stateDim, int actionDim,
                                 int numStrategies, int numClusters,
                                 float oppLr) {
        this.stateDim      = stateDim;
        this.actionDim     = actionDim;
        this.numStrategies = numStrategies;
        this.numClusters   = numClusters;
        this.oppOpt        = new NeuralNetworkOptimizer(oppLr);

        float s = (float)Math.sqrt(2.0/(stateDim+64));
        oppW1   = xav(64, stateDim, s); oppB1  = new float[64];
        oppW2   = xav(actionDim, 64, s*0.1f); oppB2 = new float[actionDim];
        phaseW1 = xav(32, stateDim+4, s); phaseB1= new float[32];
        phaseW2 = xav(GamePhase.values().length, 32, s*0.1f); phaseB2= new float[GamePhase.values().length];

        // Create default strategies
        for (int k=0;k<numStrategies;k++) {
            float[] bias = new float[actionDim];
            // Slightly different biases per strategy
            for (int a=0;a<actionDim;a++) bias[a] = 1f/actionDim + (rng.nextFloat()-0.5f)*0.1f;
            strategies.add(new Strategy(k, "Strategy_" + k, bias));
        }

        Log.i(TAG, "GameStrategyAnalyzer: s=" + stateDim + " a=" + actionDim
                + " strategies=" + numStrategies);
    }

    public GameStrategyAnalyzer(int stateDim, int actionDim) {
        this(stateDim, actionDim, 5, 16, 1e-3f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Analyse current state and return strategic action biases.
     * Combines strategy portfolio weights with pattern-matched recommendations.
     */
    public synchronized float[] getActionBias(float[] state) {
        float[] s = pad(state, stateDim);
        analysisCount.incrementAndGet();

        // Detect game phase
        currentPhase = detectPhase(s);

        // Select best strategy
        currentStrategy = selectStrategy(s);
        Strategy strat = strategies.get(currentStrategy);
        strat.uses++;

        // Pattern match
        int clusterId = nearestCluster(s);
        float[] bias = strat.actionBias.clone();
        if (clusterId >= 0) {
            Cluster cl = clusters.get(clusterId);
            if (cl.avgReward > 0 && cl.actionMode >= 0 && cl.actionMode < actionDim) {
                bias[cl.actionMode] *= 1.5f;  // boost pattern-matched action
                patternMatches.incrementAndGet();
            }
        }

        // Normalise
        float sum=0; for(float b:bias) sum+=b;
        if(sum>0) for(int i=0;i<bias.length;i++) bias[i]/=sum;
        return bias;
    }

    /** Predict opponent's next action. */
    public synchronized int predictOpponentAction(float[] state) {
        float[] s = pad(state, stateDim);
        float[] h = linRelu(oppW1, oppB1, s);
        float[] probs = softmax(lin(oppW2, oppB2, h));
        return argmax(probs);
    }

    /** Train opponent model on observed opponent action. */
    public synchronized void observeOpponent(float[] state, int oppAction) {
        float[] s = pad(state, stateDim);
        float[] h = linRelu(oppW1, oppB1, s);
        float[] probs = softmax(lin(oppW2, oppB2, h));
        int a = Math.min(oppAction, actionDim-1);
        float[] dL = probs.clone(); dL[a] -= 1f;
        float[][] dW2 = outer(dL, h);
        oppOpt.step("gs_oppW2", oppW2, dW2);
        float[] dH = new float[64];
        for(int j=0;j<64;j++){if(h[j]<=0)continue;for(int i=0;i<actionDim;i++)dH[j]+=dL[i]*oppW2[i][j];}
        oppOpt.step("gs_oppW1", oppW1, outer(dH, s));
    }

    /** Record step for macro-action tracking. */
    public synchronized void recordMacroStep(float[] state, int action, float reward, boolean done) {
        if (macroStart < 0) { macroStart=0; macroState=pad(state,stateDim); macroAction=action; }
        macroReward += reward;
        if (done || macroReward > 5f || macroReward < -5f) {
            // End of macro-action
            if (macroState != null) macroLib.add(new float[]{macroReward, macroAction});
            macroStart=-1; macroState=null; macroReward=0f;
        }
    }

    /** Update strategy win rate after game outcome. */
    public synchronized void recordOutcome(int strategyId, boolean win) {
        if (strategyId >= 0 && strategyId < strategies.size()) {
            Strategy s = strategies.get(strategyId);
            s.winRate = 0.9f * s.winRate + 0.1f * (win ? 1f : 0f);
            s.weight  = (float)Math.exp(s.winRate * 2f);
        }
        normaliseStrategyWeights();
    }

    /** Add a state→reward observation to cluster pool. */
    public synchronized void addToCluster(float[] state, int action, float reward) {
        float[] s = pad(state, stateDim);
        int nearest = nearestCluster(s);
        if (nearest >= 0) {
            Cluster c = clusters.get(nearest);
            c.count++;
            c.avgReward = 0.9f * c.avgReward + 0.1f * reward;
            float alpha = 1f / c.count;
            for (int i=0;i<stateDim;i++) c.centroid[i] = (1-alpha)*c.centroid[i]+alpha*s[i];
        } else if (clusters.size() < numClusters) {
            Cluster c = new Cluster(s, action); c.avgReward = reward; c.count = 1;
            clusters.add(c);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private GamePhase detectPhase(float[] s) {
        // Simple heuristic: use first state feature as game progress proxy
        float prog = s.length > 0 ? s[0] : 0.5f;
        if (prog < 0.2f)      return GamePhase.EARLY;
        else if (prog < 0.6f) return GamePhase.MID;
        else if (prog < 0.8f) return GamePhase.LATE;
        else                  return GamePhase.WINNING;
    }

    private int selectStrategy(float[] s) {
        float total=0; for(Strategy st:strategies) total+=st.weight;
        float r=rng.nextFloat()*total, cum=0;
        for (int k=0;k<strategies.size()-1;k++) { cum+=strategies.get(k).weight; if(r<cum)return k; }
        return strategies.size()-1;
    }

    private int nearestCluster(float[] s) {
        if (clusters.isEmpty()) return -1;
        int best=-1; float bestD=Float.MAX_VALUE;
        for (int c=0;c<clusters.size();c++) {
            float d=0; for(int i=0;i<Math.min(stateDim,clusters.get(c).centroid.length);i++){float di=s[i]-clusters.get(c).centroid[i];d+=di*di;}
            if(d<bestD){bestD=d;best=c;}
        }
        return bestD < 10f ? best : -1;
    }

    private void normaliseStrategyWeights() {
        float total=0; for(Strategy s:strategies) total+=s.weight;
        if(total>0) for(Strategy s:strategies) s.weight/=total;
    }

    private static float[] linRelu(float[][] W, float[] b, float[] x) {
        float[] o=new float[W.length];for(int i=0;i<W.length;i++){float s=b[i];for(int j=0;j<Math.min(x.length,W[i].length);j++)s+=W[i][j]*x[j];o[i]=Math.max(0f,s);}return o;
    }
    private static float[] lin(float[][] W, float[] b, float[] x) {
        float[] o=new float[W.length];for(int i=0;i<W.length;i++){float s=b[i];for(int j=0;j<Math.min(x.length,W[i].length);j++)s+=W[i][j]*x[j];o[i]=s;}return o;
    }
    private static float[] softmax(float[] v){float mx=v[0];for(float x:v)if(x>mx)mx=x;float sum=0;float[] o=new float[v.length];for(int i=0;i<v.length;i++){o[i]=(float)Math.exp(v[i]-mx);sum+=o[i];}for(int i=0;i<v.length;i++)o[i]/=sum;return o;}
    private static int argmax(float[] v){int b=0;for(int i=1;i<v.length;i++)if(v[i]>v[b])b=i;return b;}
    private static float[] pad(float[] x,int dim){if(x.length==dim)return x;float[] p=new float[dim];System.arraycopy(x,0,p,0,Math.min(x.length,dim));return p;}
    private static float[][] outer(float[] a, float[] b){float[][] g=new float[a.length][b.length];for(int i=0;i<a.length;i++) for(int j=0;j<b.length;j++) g[i][j]=a[i]*b[j];return g;}
    private float[][] xav(int r,int c,float s){float[][] m=new float[r][c];for(int i=0;i<r;i++) for(int j=0;j<c;j++) m[i][j]=(rng.nextFloat()*2f-1f)*s;return m;}

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("analysisCount",  analysisCount.get());
        s.put("patternMatches", patternMatches.get());
        s.put("currentPhase",   currentPhase.name());
        s.put("currentStrategy",currentStrategy);
        s.put("clusterCount",   clusters.size());
        s.put("macroLibSize",   macroLib.size());
        s.put("numStrategies",  strategies.size());
        Map<String,Double> wr=new HashMap<>(); for(Strategy st:strategies) wr.put(st.name,(double)st.winRate);
        s.put("winRates",       wr);
        return s;
    }
}
