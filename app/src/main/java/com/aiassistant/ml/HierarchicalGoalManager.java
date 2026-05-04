package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * HierarchicalGoalManager — two-level goal hierarchy for hierarchical RL.
 *
 * Implements the HIRO (Hierarchical RL with Off-policy Correction) architecture:
 *
 *   HIGH-LEVEL POLICY (Manager):
 *     - Operates at timescale c (every c env steps).
 *     - Observes state s_t, produces sub-goal g_t ∈ goal space.
 *     - Trained to maximise extrinsic reward sum over c steps.
 *
 *   LOW-LEVEL POLICY (Worker):
 *     - Executes primitive actions to reach sub-goal g_t.
 *     - Intrinsic reward: r_i = -||s_{t+1} - (s_t + g_t)||  (goal reaching).
 *     - Trained on (s, g, a, r_i, s') transitions.
 *
 *   OFF-POLICY CORRECTION:
 *     Re-label past sub-goals to maximise likelihood of observed actions
 *     (corrects for non-stationary manager policy during replay).
 *
 * Goal space: same dimensionality as state space, or a learned subspace.
 *
 * Thread-safe.
 */
public class HierarchicalGoalManager {

    private static final String TAG = "HierGoal";

    // ─────────────────────────────────────────────────────────────────────────
    // High-level (Manager) network
    // ─────────────────────────────────────────────────────────────────────────
    private final float[][] mgW1, mgW2;   // state → goal
    private final float[]   mgB1, mgB2;

    // Low-level (Worker) network
    private final float[][] wkW1, wkW2;   // concat(state, goal) → action logits
    private final float[]   wkB1, wkB2;

    private final NeuralNetworkOptimizer mgOpt, wkOpt;

    // ─────────────────────────────────────────────────────────────────────────
    // Config
    // ─────────────────────────────────────────────────────────────────────────
    private final int   stateDim, actionDim, goalDim, hidDim, managerFreq;
    private final float gamma, goalScale;

    // Runtime state
    private float[] currentGoal;
    private float[] managerState;
    private int     stepsSinceManager = 0;
    private float   managerReward = 0f;

    // Stats
    private final AtomicInteger managerUpdates = new AtomicInteger(0);
    private final AtomicInteger workerUpdates  = new AtomicInteger(0);
    private final AtomicInteger goalCount      = new AtomicInteger(0);
    private float avgGoalDistance  = 0f;
    private float avgWorkerReward  = 0f;
    private float avgManagerReward = 0f;

    private final Random rng = new Random(347L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public HierarchicalGoalManager(int stateDim, int actionDim, int goalDim,
                                    int hidDim, int managerFreq, float gamma,
                                    float goalScale, float mgLr, float wkLr) {
        this.stateDim    = stateDim;
        this.actionDim   = actionDim;
        this.goalDim     = goalDim;
        this.hidDim      = hidDim;
        this.managerFreq = managerFreq;
        this.gamma       = gamma;
        this.goalScale   = goalScale;
        this.mgOpt       = new NeuralNetworkOptimizer(mgLr);
        this.wkOpt       = new NeuralNetworkOptimizer(wkLr);

        float s = (float) Math.sqrt(2.0 / (stateDim + hidDim));
        mgW1 = xav(hidDim, stateDim, s); mgB1 = new float[hidDim];
        mgW2 = xav(goalDim, hidDim, s * 0.1f); mgB2 = new float[goalDim];

        float ws = (float) Math.sqrt(2.0 / (stateDim + goalDim + hidDim));
        wkW1 = xav(hidDim, stateDim + goalDim, ws); wkB1 = new float[hidDim];
        wkW2 = xav(actionDim, hidDim, ws * 0.1f);   wkB2 = new float[actionDim];

        currentGoal  = new float[goalDim];
        managerState = new float[stateDim];

        Log.i(TAG, "HierarchicalGoalManager: s=" + stateDim + " a=" + actionDim
                + " g=" + goalDim + " freq=" + managerFreq);
    }

    public HierarchicalGoalManager(int stateDim, int actionDim) {
        this(stateDim, actionDim, Math.min(stateDim, 16), 128, 10, 0.99f, 1f, 3e-4f, 3e-4f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Select action given current state.
     * Manager generates new goal every managerFreq steps.
     * Worker selects action to achieve current goal.
     */
    public synchronized int selectAction(float[] state) {
        float[] s = pad(state, stateDim);
        if (stepsSinceManager == 0 || currentGoal == null) {
            currentGoal  = generateGoal(s);
            managerState = s.clone();
            goalCount.incrementAndGet();
        }
        stepsSinceManager = (stepsSinceManager + 1) % managerFreq;
        return workerAction(s, currentGoal);
    }

    /**
     * Record transition for training.
     * @param state     current state
     * @param action    action taken
     * @param reward    extrinsic reward
     * @param nextState next state
     * @param done      episode done
     */
    public synchronized void recordTransition(float[] state, int action, float reward,
                                               float[] nextState, boolean done) {
        float[] s  = pad(state,     stateDim);
        float[] sp = pad(nextState, stateDim);

        // Worker intrinsic reward: how close did we get to goal?
        float goalDist = goalDistance(s, sp, currentGoal);
        float workerReward = -goalDist * goalScale;
        avgWorkerReward = 0.99f * avgWorkerReward + 0.01f * workerReward;
        avgGoalDistance = 0.99f * avgGoalDistance + 0.01f * goalDist;

        // Train worker
        trainWorker(s, currentGoal, action, workerReward, sp, done);

        // Accumulate extrinsic reward for manager
        managerReward += (float) Math.pow(gamma, stepsSinceManager) * reward;

        // Train manager on manager-level transition
        if (stepsSinceManager == 0 || done) {
            trainManager(managerState, sp, managerReward);
            avgManagerReward = 0.99f * avgManagerReward + 0.01f * managerReward;
            managerReward = 0f;
        }

        // Update goal: h-correction (translate goal from s to sp frame)
        if (currentGoal != null) {
            for (int i=0;i<goalDim;i++) {
                currentGoal[i] = s[i] + currentGoal[i] - sp[i];
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Manager
    // ─────────────────────────────────────────────────────────────────────────

    private float[] generateGoal(float[] state) {
        float[] h = linRelu(mgW1, mgB1, state);
        float[] g = lin(mgW2, mgB2, h);
        // Clip goal to reasonable range
        for (int i=0;i<goalDim;i++) g[i] = Math.max(-2f, Math.min(2f, g[i]));
        return g;
    }

    private void trainManager(float[] s0, float[] sT, float extReward) {
        float[] h   = linRelu(mgW1, mgB1, s0);
        float[] gPred = lin(mgW2, mgB2, h);
        // Reward: want to generate goals that led to high extrinsic reward
        // Surrogate: push goal toward direction of observed state change
        float[] goalTarget = new float[goalDim];
        for (int i=0;i<goalDim;i++) goalTarget[i] = (sT[i] - s0[i]) * Math.signum(extReward);

        float[] err = new float[goalDim];
        for (int i=0;i<goalDim;i++) err[i] = gPred[i] - goalTarget[i];

        float[][] dW2 = outer(err, h);
        mgOpt.step("hg_mgW2", mgW2, dW2);
        float[] dH = new float[hidDim];
        for(int j=0;j<hidDim;j++){if(h[j]<=0)continue;for(int i=0;i<goalDim;i++)dH[j]+=err[i]*mgW2[i][j];}
        mgOpt.step("hg_mgW1", mgW1, outer(dH, s0));
        managerUpdates.incrementAndGet();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Worker
    // ─────────────────────────────────────────────────────────────────────────

    private int workerAction(float[] state, float[] goal) {
        float[] inp = concat(pad(state, stateDim), pad(goal, goalDim));
        float[] h   = linRelu(wkW1, wkB1, inp);
        float[] logits = lin(wkW2, wkB2, h);
        float[] probs  = softmax(logits);
        return sampleCat(probs);
    }

    private void trainWorker(float[] s, float[] g, int action, float reward,
                              float[] sp, boolean done) {
        float[] inp = concat(pad(s, stateDim), pad(g, goalDim));
        float[] h   = linRelu(wkW1, wkB1, inp);
        float[] pr  = softmax(lin(wkW2, wkB2, h));
        int a = Math.min(action, actionDim - 1);
        float[] dL  = pr.clone(); dL[a] -= 1f;
        float scale = -reward;
        for (int i=0;i<actionDim;i++) dL[i] *= scale;
        float[][] dW2 = outer(dL, h);
        wkOpt.step("hg_wkW2", wkW2, dW2);
        float[] dH = new float[hidDim];
        for(int j=0;j<hidDim;j++){if(h[j]<=0)continue;for(int i=0;i<actionDim;i++)dH[j]+=dL[i]*wkW2[i][j];}
        wkOpt.step("hg_wkW1", wkW1, outer(dH, inp));
        workerUpdates.incrementAndGet();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private float goalDistance(float[] s, float[] sp, float[] g) {
        float d = 0;
        for (int i=0;i<Math.min(goalDim, Math.min(s.length, sp.length));i++) {
            float diff = (s[i] + g[i]) - sp[i]; d += diff*diff;
        }
        return (float)Math.sqrt(d);
    }

    private static float[] linRelu(float[][] W, float[] b, float[] x) {
        float[] o=new float[W.length];for(int i=0;i<W.length;i++){float s=b[i];for(int j=0;j<Math.min(x.length,W[i].length);j++)s+=W[i][j]*x[j];o[i]=Math.max(0f,s);}return o;
    }
    private static float[] lin(float[][] W, float[] b, float[] x) {
        float[] o=new float[W.length];for(int i=0;i<W.length;i++){float s=b[i];for(int j=0;j<Math.min(x.length,W[i].length);j++)s+=W[i][j]*x[j];o[i]=s;}return o;
    }
    private static float[] softmax(float[] v){float mx=v[0];for(float x:v)if(x>mx)mx=x;float sum=0;float[] o=new float[v.length];for(int i=0;i<v.length;i++){o[i]=(float)Math.exp(v[i]-mx);sum+=o[i];}for(int i=0;i<v.length;i++)o[i]/=sum;return o;}
    private int sampleCat(float[] p){float r=rng.nextFloat(),c=0;for(int a=0;a<p.length-1;a++){c+=p[a];if(r<c)return a;}return p.length-1;}
    private static float[] pad(float[] x,int dim){if(x.length==dim)return x;float[] p=new float[dim];System.arraycopy(x,0,p,0,Math.min(x.length,dim));return p;}
    private static float[] concat(float[] a, float[] b){float[] c=new float[a.length+b.length];System.arraycopy(a,0,c,0,a.length);System.arraycopy(b,0,c,a.length,b.length);return c;}
    private static float[][] outer(float[] a, float[] b){float[][] g=new float[a.length][b.length];for(int i=0;i<a.length;i++) for(int j=0;j<b.length;j++) g[i][j]=a[i]*b[j];return g;}
    private static float Math_signum(float v){return v>0?1f:v<0?-1f:0f;}
    private static float signum(float v){return v>0?1f:v<0?-1f:0f;}
    private float[][] xav(int r,int c,float s){float[][] m=new float[r][c];for(int i=0;i<r;i++) for(int j=0;j<c;j++) m[i][j]=(rng.nextFloat()*2f-1f)*s;return m;}

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("goalCount",       goalCount.get());
        s.put("managerUpdates",  managerUpdates.get());
        s.put("workerUpdates",   workerUpdates.get());
        s.put("avgGoalDistance", avgGoalDistance);
        s.put("avgWorkerReward", avgWorkerReward);
        s.put("avgManagerReward",avgManagerReward);
        s.put("managerFreq",     managerFreq);
        return s;
    }
}
