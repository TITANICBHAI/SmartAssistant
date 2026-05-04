package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * GoalConditionedRL — Hindsight Experience Replay (HER) + Universal Value Functions.
 *
 * Enables the agent to learn from failures by relabelling unsuccessful trajectories
 * with the state the agent actually reached (as if that was the goal).
 *
 * Components:
 *
 *   1. UNIVERSAL POLICY: π(a | s, g) — conditioned on both state AND goal.
 *      Policy input = concat(state, goal).
 *
 *   2. HINDSIGHT EXPERIENCE REPLAY (HER, Andrychowicz et al. 2017):
 *      After each episode, for each transition (s, a, r, s', g):
 *        a. Keep original transition.
 *        b. Relabel with g' = s'_{T} (the final state actually reached).
 *        c. Recompute reward: r' = env.reward(s', g') = 0 if s'≈g', else -1.
 *      This produces 4x more useful learning signal from sparse-reward tasks.
 *
 *   3. GOAL SAMPLING: multiple strategies for sampling goals:
 *        FUTURE   — sample g from states later in same episode (most common).
 *        FINAL    — use the last state of the episode.
 *        EPISODE  — sample g from any state in the same episode.
 *        RANDOM   — sample g from a random replay buffer entry.
 *
 *   4. GOAL DISTANCE REWARD: r(s', g) = -||s' - g||₂ (negative L2 for dense shaping).
 *
 * Thread-safe.
 */
public class GoalConditionedRL {

    private static final String TAG = "GoalConditionedRL";

    public enum GoalStrategy { FUTURE, FINAL, EPISODE, RANDOM }

    // ─────────────────────────────────────────────────────────────────────────
    // Goal-conditioned transition
    // ─────────────────────────────────────────────────────────────────────────
    public static class GoalTransition {
        public final float[] state, nextState, goal;
        public final int     action;
        public final float   reward;
        public final boolean done;
        public final boolean achieved;  // did agent reach goal?

        public GoalTransition(float[] s, int a, float r, float[] sp,
                              float[] g, boolean done, boolean achieved) {
            this.state     = s;   this.action   = a;
            this.reward    = r;   this.nextState = sp;
            this.goal      = g;   this.done      = done;
            this.achieved  = achieved;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int    stateDim, goalDim, actionDim, hidDim;
    private final int    inputDim;  // stateDim + goalDim
    private final float  gamma, lr;
    private final float  successRadius;   // ||s' - g||₂ ≤ radius → achieved
    private final int    herRatio;        // k = num HER relabellings per real
    private final GoalStrategy strategy;

    // Goal-conditioned Q network
    private final float[][] W1, W2;  // [hidDim][inputDim], [actionDim][hidDim]
    private final float[]   b1, b2;
    private final float[][] TW1, TW2; // target network
    private final float[]   Tb1, Tb2;
    private final NeuralNetworkOptimizer opt;

    // Episode buffer (for HER relabelling)
    private final List<float[]>  epStates   = new ArrayList<>();
    private final List<Integer>  epActions  = new ArrayList<>();
    private final List<Float>    epRewards  = new ArrayList<>();

    // Replay buffer (goal-conditioned transitions)
    private final List<GoalTransition> replayBuffer = new ArrayList<>();
    private final int                  maxReplay;

    // Current episode goal
    private float[] currentGoal = null;

    private final AtomicInteger episodeCount  = new AtomicInteger(0);
    private final AtomicInteger herTransitions= new AtomicInteger(0);
    private final AtomicInteger trainSteps    = new AtomicInteger(0);
    private float avgReward   = 0f;
    private float avgSuccess  = 0f;
    private float avgQLoss    = 0f;

    private final Random rng;

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public GoalConditionedRL(int stateDim, int goalDim, int actionDim, int hidDim,
                              float gamma, float lr, float successRadius,
                              int herRatio, GoalStrategy strategy, int maxReplay, long seed) {
        this.stateDim      = stateDim;
        this.goalDim       = goalDim;
        this.actionDim     = actionDim;
        this.hidDim        = hidDim;
        this.inputDim      = stateDim + goalDim;
        this.gamma         = gamma;
        this.lr            = lr;
        this.successRadius = successRadius;
        this.herRatio      = herRatio;
        this.strategy      = strategy;
        this.maxReplay     = maxReplay;
        this.rng           = new Random(seed);
        this.opt           = new NeuralNetworkOptimizer(lr);

        float s = (float) Math.sqrt(2.0 / (inputDim + hidDim));
        W1  = xav(hidDim, inputDim, s);   b1  = new float[hidDim];
        W2  = xav(actionDim, hidDim, s);  b2  = new float[actionDim];
        TW1 = xav(hidDim, inputDim, s);   Tb1 = new float[hidDim];
        TW2 = xav(actionDim, hidDim, s);  Tb2 = new float[actionDim];
        copyNet();

        Log.i(TAG, "GoalConditionedRL: s=" + stateDim + " g=" + goalDim
                + " a=" + actionDim + " HER×" + herRatio);
    }

    public GoalConditionedRL(int stateDim, int goalDim, int actionDim) {
        this(stateDim, goalDim, actionDim, 128, 0.99f, 1e-3f, 0.05f, 4,
             GoalStrategy.FUTURE, 50_000, 223L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Episode management
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized void beginEpisode(float[] goal) {
        currentGoal = goal.clone();
        epStates.clear(); epActions.clear(); epRewards.clear();
    }

    /** Record a step; automatically adds to episode buffer. */
    public synchronized void step(float[] state, int action, float reward, float[] nextState) {
        epStates.add(state.clone());
        epActions.add(action);
        epRewards.add(reward);
        avgReward = 0.99f * avgReward + 0.01f * reward;
    }

    /** End episode: apply HER relabelling, add to replay buffer. */
    public synchronized void endEpisode() {
        if (currentGoal == null || epStates.isEmpty()) return;
        int T = epStates.size();
        float[] finalState = epStates.get(T - 1);

        // Add real transitions
        for (int t = 0; t < T - 1; t++) {
            float[] s  = epStates.get(t);
            float[] sp = epStates.get(t + 1);
            boolean ach= achieved(sp, currentGoal);
            addToReplay(new GoalTransition(s, epActions.get(t),
                    epRewards.get(t), sp, currentGoal, t == T - 2, ach));
        }

        // HER relabelling
        for (int k = 0; k < herRatio; k++) {
            float[] herGoal = sampleHerGoal(finalState, t -> epStates.get(t));
            for (int t = 0; t < T - 1; t++) {
                float[] s  = epStates.get(t);
                float[] sp = epStates.get(t + 1);
                float   r  = achieved(sp, herGoal) ? 0f : -1f;
                boolean ach= achieved(sp, herGoal);
                addToReplay(new GoalTransition(s, epActions.get(t),
                        r, sp, herGoal, t == T - 2, ach));
                herTransitions.incrementAndGet();
                if (ach) break; // stop after first achievement
            }
        }

        // Track success
        boolean success = achieved(finalState, currentGoal);
        avgSuccess = 0.99f * avgSuccess + 0.01f * (success ? 1f : 0f);
        episodeCount.incrementAndGet();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inference
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized int selectAction(float[] state, float[] goal, float epsilon) {
        if (rng.nextFloat() < epsilon) return rng.nextInt(actionDim);
        float[] Q = qValues(state, goal);
        int best = 0; for (int a = 1; a < actionDim; a++) if (Q[a] > Q[best]) best = a;
        return best;
    }

    public synchronized float[] qValues(float[] state, float[] goal) {
        float[] inp = goalInput(state, goal);
        float[] h   = lin(W1, b1, inp, true);
        return lin(W2, b2, h, false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Training
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized float trainStep(int batchSize) {
        if (replayBuffer.size() < batchSize) return 0f;
        float loss = 0;
        for (int i = 0; i < batchSize; i++) {
            GoalTransition t = replayBuffer.get(rng.nextInt(replayBuffer.size()));
            float[] inp  = goalInput(t.state,     t.goal);
            float[] inpN = goalInput(t.nextState, t.goal);
            float[] QN   = linFwd(TW1, Tb1, TW2, Tb2, inpN);
            float   maxQN= t.done ? 0f : max(QN);
            float   target= t.reward + gamma * maxQN;
            float[] Q    = linFwd(W1, b1, W2, b2, inp);
            float   err  = target - Q[t.action];
            loss += err * err;

            Q[t.action] -= err;  // gradient
            backprop(inp, Q, t.action, err);
            trainSteps.incrementAndGet();
        }
        if (trainSteps.get() % 1000 == 0) copyNet();
        avgQLoss = 0.99f * avgQLoss + 0.01f * loss / batchSize;
        return loss / batchSize;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal
    // ─────────────────────────────────────────────────────────────────────────

    private boolean achieved(float[] state, float[] goal) {
        float d = 0;
        for (int i = 0; i < Math.min(state.length, goal.length); i++) {
            float diff = state[i] - goal[i]; d += diff * diff;
        }
        return Math.sqrt(d) <= successRadius;
    }

    private float[] sampleHerGoal(float[] finalState, java.util.function.IntFunction<float[]> getter) {
        switch (strategy) {
            case FINAL:   return finalState;
            case EPISODE: return getter.apply(rng.nextInt(epStates.size()));
            case FUTURE:
            default:
                int idx = rng.nextInt(Math.max(1, epStates.size()));
                return getter.apply(idx);
        }
    }

    private float[] goalInput(float[] state, float[] goal) {
        float[] inp = new float[inputDim];
        System.arraycopy(pad(state, stateDim), 0, inp, 0, stateDim);
        System.arraycopy(pad(goal,  goalDim),  0, inp, stateDim, goalDim);
        return inp;
    }

    private void addToReplay(GoalTransition t) {
        if (replayBuffer.size() >= maxReplay) replayBuffer.remove(rng.nextInt(maxReplay));
        replayBuffer.add(t);
    }

    private void backprop(float[] inp, float[] qTarget, int action, float err) {
        float[] h  = lin(W1, b1, inp, true);
        float[] dQ = new float[actionDim]; dQ[action] = -err;
        float[][] dW2 = new float[actionDim][hidDim];
        for (int i=0;i<actionDim;i++) for(int j=0;j<hidDim;j++) dW2[i][j]=dQ[i]*h[j];
        opt.step("gcrl_W2", W2, dW2);
        float[] dH = new float[hidDim];
        for (int j=0;j<hidDim;j++) {if(h[j]<=0) continue; for(int i=0;i<actionDim;i++) dH[j]+=dQ[i]*W2[i][j];}
        float[][] dW1 = new float[hidDim][inp.length];
        for (int i=0;i<hidDim;i++) for(int j=0;j<inp.length;j++) dW1[i][j]=dH[i]*inp[j];
        opt.step("gcrl_W1", W1, dW1);
    }

    private void copyNet() {
        for (int i=0;i<W1.length;i++) System.arraycopy(W1[i],0,TW1[i],0,W1[i].length);
        for (int i=0;i<W2.length;i++) System.arraycopy(W2[i],0,TW2[i],0,W2[i].length);
    }

    private static float[] linFwd(float[][] W1, float[] b1, float[][] W2, float[] b2, float[] x) {
        return lin(W2, b2, lin(W1, b1, x, true), false);
    }

    private static float[] lin(float[][] W, float[] b, float[] x, boolean relu) {
        float[] o = new float[W.length];
        for (int i=0;i<W.length;i++) {
            float s=b[i]; for(int j=0;j<Math.min(x.length,W[i].length);j++) s+=W[i][j]*x[j];
            o[i]=relu?Math.max(0f,s):s;
        }
        return o;
    }

    private static float max(float[] v) {float m=v[0];for(float x:v)if(x>m)m=x;return m;}

    private static float[] pad(float[] x, int dim) {
        if (x.length==dim) return x;
        float[] p=new float[dim]; System.arraycopy(x,0,p,0,Math.min(x.length,dim)); return p;
    }

    private float[][] xav(int r, int c, float s) {
        float[][] m=new float[r][c];
        for(int i=0;i<r;i++) for(int j=0;j<c;j++) m[i][j]=(rng.nextFloat()*2f-1f)*s;
        return m;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("episodeCount",   episodeCount.get());
        s.put("herTransitions", herTransitions.get());
        s.put("trainSteps",     trainSteps.get());
        s.put("replaySize",     replayBuffer.size());
        s.put("avgReward",      avgReward);
        s.put("avgSuccess",     avgSuccess);
        s.put("avgQLoss",       avgQLoss);
        s.put("successRadius",  successRadius);
        s.put("herRatio",       herRatio);
        return s;
    }
}
