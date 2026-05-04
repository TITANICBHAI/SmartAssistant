package com.aiassistant.ml;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RewardShapingEngine — potential-based reward shaping for faster learning.
 *
 * Reward shaping adds a shaping term F(s, a, s') to the extrinsic reward
 * without changing the optimal policy (potential-based shaping theorem,
 * Ng et al. 1999):
 *
 *   r_shaped = r + γ·Φ(s') - Φ(s)
 *
 * where Φ: S → ℝ is a potential function. The shaped MDP has the same
 * optimal policy as the original MDP.
 *
 * Shaping strategies:
 *
 *   DISTANCE_TO_GOAL:
 *     Φ(s) = -||s - s_goal||   — continuous goal-directed guidance.
 *
 *   LEARNED_POTENTIAL:
 *     Φ(s) = V_θ(s)            — value function as potential (bootstrapping).
 *
 *   PROGRESS:
 *     Φ(s) = similarity(s, desired_direction) — reward moving toward goal region.
 *
 *   CURIOSITY_BONUS:
 *     F(s,a,s') = η·||f_θ(s,a) - s'||²   — not potential-based but widely used.
 *
 *   MILESTONE:
 *     F(s) = +bonus when s crosses a milestone threshold (one-time reward).
 *
 *   COMPOSITE:
 *     Weighted combination of any of the above.
 *
 * Thread-safe.
 */
public class RewardShapingEngine {

    private static final String TAG = "RewardShaping";

    public enum Strategy {
        DISTANCE_TO_GOAL, LEARNED_POTENTIAL, PROGRESS,
        CURIOSITY_BONUS, MILESTONE, COMPOSITE
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Learned potential network: state → V(s)
    // ─────────────────────────────────────────────────────────────────────────
    private final float[][] potW1, potW2;
    private final float[]   potB1, potB2;
    private final NeuralNetworkOptimizer potOpt;

    // Curiosity forward model
    private final float[][] curW1, curW2;
    private final float[]   curB1, curB2;
    private final NeuralNetworkOptimizer curOpt;

    // ─────────────────────────────────────────────────────────────────────────
    // Config
    // ─────────────────────────────────────────────────────────────────────────
    private final int      stateDim, actionDim, hidDim;
    private final Strategy strategy;
    private final float    gamma;
    private final float    shapingScale;
    private final float    curiosityScale;

    // Goal state (for DISTANCE_TO_GOAL / PROGRESS)
    private float[] goalState;
    private float[] desiredDirection;

    // Milestones: [threshold_dim, threshold_val, bonus, triggered?]
    private final float[]   milestoneThresh;
    private final float[]   milestoneBonus;
    private final boolean[] milestoneDone;

    // Stats
    private final AtomicInteger shapeCount = new AtomicInteger(0);
    private float avgShaping  = 0f;
    private float avgExtrinsic= 0f;
    private float avgTotal    = 0f;

    private final Random rng = new Random(353L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public RewardShapingEngine(int stateDim, int actionDim, int hidDim,
                                Strategy strategy, float gamma,
                                float shapingScale, float curiosityScale,
                                float potLr, float curLr) {
        this.stateDim      = stateDim;
        this.actionDim     = actionDim;
        this.hidDim        = hidDim;
        this.strategy      = strategy;
        this.gamma         = gamma;
        this.shapingScale  = shapingScale;
        this.curiosityScale= curiosityScale;
        this.potOpt        = new NeuralNetworkOptimizer(potLr);
        this.curOpt        = new NeuralNetworkOptimizer(curLr);

        float s = (float)Math.sqrt(2.0/(stateDim+hidDim));
        potW1 = xav(hidDim, stateDim, s); potB1 = new float[hidDim];
        potW2 = xav(1,      hidDim, s*0.01f); potB2= new float[1];

        curW1 = xav(hidDim, stateDim+actionDim, s); curB1 = new float[hidDim];
        curW2 = xav(stateDim, hidDim, s*0.01f);     curB2 = new float[stateDim];

        goalState       = new float[stateDim];
        desiredDirection= new float[stateDim];
        milestoneThresh = new float[8];
        milestoneBonus  = new float[8];
        milestoneDone   = new boolean[8];

        Log.i(TAG, "RewardShapingEngine: " + strategy + " scale=" + shapingScale);
    }

    public RewardShapingEngine(int stateDim, int actionDim, Strategy strategy) {
        this(stateDim, actionDim, 64, strategy, 0.99f, 0.1f, 0.01f, 1e-3f, 3e-4f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Shape the reward for a transition.
     * @return total shaped reward
     */
    public synchronized float shape(float[] state, int action, float extrinsic,
                                     float[] nextState, boolean done) {
        float[] s  = pad(state,     stateDim);
        float[] sp = pad(nextState, stateDim);

        float shaping = computeShaping(s, action, sp, done);
        float total   = extrinsic + shapingScale * shaping;

        avgShaping   = 0.99f * avgShaping   + 0.01f * shaping;
        avgExtrinsic = 0.99f * avgExtrinsic + 0.01f * extrinsic;
        avgTotal     = 0.99f * avgTotal     + 0.01f * total;
        shapeCount.incrementAndGet();
        return total;
    }

    /** Update learned components with TD target. */
    public synchronized void update(float[] state, float[] nextState, float reward, boolean done) {
        float[] s  = pad(state,     stateDim);
        float[] sp = pad(nextState, stateDim);
        float   nextV = done ? 0f : potential(sp);
        float   target= reward + gamma * nextV;
        float   V     = potential(s);
        float   err   = V - target;

        float[] h = linRelu(potW1, potB1, s);
        float[][] dW2 = new float[1][hidDim]; for(int j=0;j<hidDim;j++) dW2[0][j]=err*h[j];
        potOpt.step("rs_potW2", potW2, dW2);
        float[] dH = new float[hidDim];
        for(int j=0;j<hidDim;j++){if(h[j]<=0)continue; dH[j]=err*potW2[0][j];}
        potOpt.step("rs_potW1", potW1, outer(dH, s));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Shaping implementations
    // ─────────────────────────────────────────────────────────────────────────

    private float computeShaping(float[] s, int action, float[] sp, boolean done) {
        switch (strategy) {
            case DISTANCE_TO_GOAL:
                return gamma * (-distance(sp, goalState)) - (-distance(s, goalState));
            case LEARNED_POTENTIAL:
                return gamma * (done ? 0f : potential(sp)) - potential(s);
            case PROGRESS:
                return progress(s, sp);
            case CURIOSITY_BONUS:
                return curiosityBonus(s, action, sp);
            case MILESTONE:
                return milestoneBonus(sp);
            case COMPOSITE:
                return 0.4f * (gamma*(done?0f:potential(sp))-potential(s))
                     + 0.3f * curiosityBonus(s, action, sp)
                     + 0.2f * progress(s, sp)
                     + 0.1f * milestoneBonus(sp);
            default: return 0f;
        }
    }

    private float potential(float[] s) {
        return linRelu_to_lin(potW1, potB1, potW2, potB2, s)[0];
    }

    private float distance(float[] a, float[] b) {
        float d = 0;
        for (int i=0;i<Math.min(a.length,b.length);i++) { float diff=a[i]-b[i]; d+=diff*diff; }
        return (float)Math.sqrt(d);
    }

    private float progress(float[] s, float[] sp) {
        float dotS=0, dotSp=0;
        for(int i=0;i<Math.min(stateDim, desiredDirection.length);i++) {
            dotS  += s[i]*desiredDirection[i];
            dotSp += sp[i]*desiredDirection[i];
        }
        return dotSp - dotS;
    }

    private float curiosityBonus(float[] s, int action, float[] sp) {
        float[] inp = concat(s, oneHot(action, actionDim));
        float[] h   = linRelu(curW1, curB1, inp);
        float[] pred= linRelu_to_lin_f(curW2, curB2, h);
        float err = 0;
        for(int i=0;i<Math.min(stateDim,sp.length);i++){float d=pred[i]-sp[i];err+=d*d;}
        // Train curiosity model
        float[] errVec = new float[stateDim];
        for(int i=0;i<stateDim;i++) errVec[i]=2f*(pred[i]-sp[i]);
        curOpt.step("rs_curW2", curW2, outer(errVec, h));
        return curiosityScale * err;
    }

    private float milestoneBonus(float[] sp) {
        float bonus = 0;
        for (int m=0;m<milestoneThresh.length;m++) {
            if (!milestoneDone[m] && milestoneThresh[m] > 0) {
                // Check if any state dimension crosses threshold
                for (float v : sp) {
                    if (v >= milestoneThresh[m]) {
                        bonus += milestoneBonus[m];
                        milestoneDone[m] = true;
                        break;
                    }
                }
            }
        }
        return bonus;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Configuration setters
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized void setGoal(float[] goal) {
        goalState = pad(goal, stateDim);
    }

    public synchronized void setDirection(float[] dir) {
        desiredDirection = pad(dir, stateDim);
        float norm=0; for(float v:desiredDirection)norm+=v*v; norm=(float)Math.sqrt(norm+1e-8f);
        for(int i=0;i<stateDim;i++) desiredDirection[i]/=norm;
    }

    public synchronized void addMilestone(int idx, float threshold, float bonus) {
        if (idx >= 0 && idx < milestoneThresh.length) {
            milestoneThresh[idx] = threshold; milestoneBonus[idx] = bonus; milestoneDone[idx] = false;
        }
    }

    public synchronized void resetMilestones() { java.util.Arrays.fill(milestoneDone, false); }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static float[] linRelu(float[][] W, float[] b, float[] x) {
        float[] o=new float[W.length];for(int i=0;i<W.length;i++){float s=b[i];for(int j=0;j<Math.min(x.length,W[i].length);j++)s+=W[i][j]*x[j];o[i]=Math.max(0f,s);}return o;
    }
    private static float[] linRelu_to_lin(float[][] W1, float[] b1, float[][] W2, float[] b2, float[] x) {
        float[] h=linRelu(W1,b1,x);float[] o=new float[W2.length];for(int i=0;i<W2.length;i++){float s=b2[i];for(int j=0;j<Math.min(h.length,W2[i].length);j++)s+=W2[i][j]*h[j];o[i]=s;}return o;
    }
    private static float[] linRelu_to_lin_f(float[][] W, float[] b, float[] x) {
        float[] o=new float[W.length];for(int i=0;i<W.length;i++){float s=b[i];for(int j=0;j<Math.min(x.length,W[i].length);j++)s+=W[i][j]*x[j];o[i]=s;}return o;
    }
    private static float[][] outer(float[] a, float[] b){float[][] g=new float[a.length][b.length];for(int i=0;i<a.length;i++) for(int j=0;j<b.length;j++) g[i][j]=a[i]*b[j];return g;}
    private static float[] pad(float[] x,int dim){if(x.length==dim)return x;float[] p=new float[dim];System.arraycopy(x,0,p,0,Math.min(x.length,dim));return p;}
    private static float[] concat(float[] a, float[] b){float[] c=new float[a.length+b.length];System.arraycopy(a,0,c,0,a.length);System.arraycopy(b,0,c,a.length,b.length);return c;}
    private static float[] oneHot(int idx, int dim){float[] o=new float[dim];if(idx>=0&&idx<dim)o[idx]=1f;return o;}
    private float[][] xav(int r,int c,float s){float[][] m=new float[r][c];for(int i=0;i<r;i++) for(int j=0;j<c;j++) m[i][j]=(rng.nextFloat()*2f-1f)*s;return m;}

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("strategy",    strategy.name());
        s.put("shapeCount",  shapeCount.get());
        s.put("avgShaping",  avgShaping);
        s.put("avgExtrinsic",avgExtrinsic);
        s.put("avgTotal",    avgTotal);
        return s;
    }
}
