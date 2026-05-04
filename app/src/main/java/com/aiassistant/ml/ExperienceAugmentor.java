package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ExperienceAugmentor — data augmentation for replay buffer transitions.
 *
 * Generates synthetic transitions from real ones to improve sample efficiency
 * and robustness:
 *
 *   HER (Hindsight Experience Replay, Andrychowicz et al. 2017):
 *     Re-label transitions with achieved goals instead of desired goals.
 *     Turns failed episodes into successful ones with different goals.
 *     Strategies: FINAL (use final state), FUTURE (use random future state),
 *                 EPISODE (use random state from episode).
 *
 *   C51_AUGMENT:
 *     Create multiple copies with different reward scales (reward noise).
 *
 *   MIXUP:
 *     Interpolate between two transitions: (λ·s1 + (1-λ)·s2, λ·r1 + (1-λ)·r2).
 *     Improves generalisation.
 *
 *   STATE_NOISE:
 *     Add Gaussian noise to state observations (robustness training).
 *
 *   DYNAMICS_NOISE:
 *     Add noise to transitions (simulates stochastic environment).
 *
 *   TEMPORAL_SHIFT:
 *     Create n-step returns from single transitions using reward accumulation.
 *
 * Thread-safe.
 */
public class ExperienceAugmentor {

    private static final String TAG = "ExpAugmentor";

    public enum AugmentType { HER_FINAL, HER_FUTURE, MIXUP, STATE_NOISE,
                              DYNAMICS_NOISE, TEMPORAL_SHIFT, REWARD_SCALE }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int   stateDim;
    private final float stateNoiseStd;
    private final float mixupAlpha;
    private final float rewardScale;
    private final float gamma;   // for temporal shift

    private final AtomicInteger augmentCount = new AtomicInteger(0);
    private final AtomicInteger herCount     = new AtomicInteger(0);

    private final Random rng = new Random(359L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public ExperienceAugmentor(int stateDim, float stateNoiseStd,
                                float mixupAlpha, float rewardScale, float gamma) {
        this.stateDim      = stateDim;
        this.stateNoiseStd = stateNoiseStd;
        this.mixupAlpha    = mixupAlpha;
        this.rewardScale   = rewardScale;
        this.gamma         = gamma;
        Log.i(TAG, "ExperienceAugmentor: dim=" + stateDim + " noiseStd=" + stateNoiseStd);
    }

    public ExperienceAugmentor(int stateDim) {
        this(stateDim, 0.02f, 0.2f, 0.5f, 0.99f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Augment a single transition.
     * @return List of augmented transitions (may be empty if augmentation fails).
     */
    public synchronized List<ReplayBuffer.Experience> augment(
            ReplayBuffer.Experience exp, AugmentType type) {
        List<ReplayBuffer.Experience> result = new ArrayList<>();
        switch (type) {
            case STATE_NOISE:    result.add(stateNoise(exp));     break;
            case DYNAMICS_NOISE: result.add(dynamicsNoise(exp));  break;
            case REWARD_SCALE:   result.add(rewardScale(exp));    break;
            default:             result.add(exp);                 break;
        }
        augmentCount.addAndGet(result.size());
        return result;
    }

    /** Mixup between two transitions. */
    public synchronized ReplayBuffer.Experience mixup(ReplayBuffer.Experience e1,
                                                       ReplayBuffer.Experience e2) {
        float lam = (float) betaSample(mixupAlpha);
        float[] s  = interpolate(e1.state,     e2.state,     lam, stateDim);
        float[] sp = interpolate(e1.nextState, e2.nextState, lam, stateDim);
        float   r  = lam * e1.reward + (1-lam) * e2.reward;
        int     a  = lam > 0.5f ? e1.action : e2.action;
        augmentCount.incrementAndGet();
        return new ReplayBuffer.Experience(s, a, r, sp, e1.done && e2.done);
    }

    /**
     * HER: Hindsight Experience Replay (FINAL strategy).
     * Takes an episode trajectory and re-labels transitions with the FINAL
     * achieved state as the goal.
     *
     * @param episode   Sequence of transitions in episode order.
     * @param goalDim   Number of goal dimensions (appended to state).
     * @return List of HER-relabeled transitions.
     */
    public synchronized List<ReplayBuffer.Experience> applyHER_FINAL(
            List<ReplayBuffer.Experience> episode, int goalDim) {
        if (episode.isEmpty()) return new ArrayList<>();
        List<ReplayBuffer.Experience> her = new ArrayList<>(episode.size());
        ReplayBuffer.Experience last = episode.get(episode.size()-1);
        float[] achievedGoal = head(last.nextState, goalDim);

        for (ReplayBuffer.Experience e : episode) {
            // Append achieved goal to state
            float[] s  = appendGoal(pad(e.state,     stateDim), achievedGoal);
            float[] sp = appendGoal(pad(e.nextState, stateDim), achievedGoal);
            // New reward: 1 if we reached the goal, else 0
            float herReward = goalReached(e.nextState, achievedGoal) ? 1f : 0f;
            her.add(new ReplayBuffer.Experience(s, e.action, herReward, sp, e.done));
        }
        herCount.addAndGet(her.size());
        return her;
    }

    /**
     * HER FUTURE strategy: for each transition, sample K goals from future states.
     */
    public synchronized List<ReplayBuffer.Experience> applyHER_FUTURE(
            List<ReplayBuffer.Experience> episode, int goalDim, int K) {
        List<ReplayBuffer.Experience> her = new ArrayList<>();
        for (int t=0;t<episode.size();t++) {
            ReplayBuffer.Experience e = episode.get(t);
            for (int k=0;k<K;k++) {
                int futureIdx = t + rng.nextInt(episode.size()-t);
                ReplayBuffer.Experience futureE = episode.get(futureIdx);
                float[] achievedGoal = head(futureE.nextState, goalDim);
                float[] s  = appendGoal(pad(e.state,     stateDim), achievedGoal);
                float[] sp = appendGoal(pad(e.nextState, stateDim), achievedGoal);
                float herReward = goalReached(e.nextState, achievedGoal) ? 1f : 0f;
                her.add(new ReplayBuffer.Experience(s, e.action, herReward, sp, e.done));
            }
        }
        herCount.addAndGet(her.size());
        return her;
    }

    /** N-step return: collapse N transitions into one with accumulated reward. */
    public synchronized ReplayBuffer.Experience nStep(
            List<ReplayBuffer.Experience> window, int n) {
        if (window.isEmpty()) return null;
        float reward = 0;
        float discount = 1f;
        for (int t=0;t<Math.min(n, window.size());t++) {
            reward  += discount * window.get(t).reward;
            discount*= gamma;
            if (window.get(t).done) break;
        }
        ReplayBuffer.Experience first = window.get(0);
        ReplayBuffer.Experience last  = window.get(Math.min(n-1, window.size()-1));
        augmentCount.incrementAndGet();
        return new ReplayBuffer.Experience(first.state, first.action, reward,
                last.nextState, last.done);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Augmentation implementations
    // ─────────────────────────────────────────────────────────────────────────

    private ReplayBuffer.Experience stateNoise(ReplayBuffer.Experience e) {
        float[] s = addNoise(pad(e.state, stateDim), stateNoiseStd);
        return new ReplayBuffer.Experience(s, e.action, e.reward, e.nextState, e.done);
    }

    private ReplayBuffer.Experience dynamicsNoise(ReplayBuffer.Experience e) {
        float[] sp = addNoise(pad(e.nextState, stateDim), stateNoiseStd);
        float   r  = e.reward + (float)(rng.nextGaussian() * 0.01);
        return new ReplayBuffer.Experience(e.state, e.action, r, sp, e.done);
    }

    private ReplayBuffer.Experience rewardScale(ReplayBuffer.Experience e) {
        float scale = 1f + (rng.nextFloat() - 0.5f) * rewardScale;
        return new ReplayBuffer.Experience(e.state, e.action, e.reward * scale,
                e.nextState, e.done);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private float[] addNoise(float[] x, float std) {
        float[] n = x.clone();
        for (int i=0;i<n.length;i++) n[i] += (float)(rng.nextGaussian() * std);
        return n;
    }

    private static float[] interpolate(float[] a, float[] b, float lam, int dim) {
        float[] r = new float[dim];
        float[] pa = pad(a,dim), pb = pad(b,dim);
        for(int i=0;i<dim;i++) r[i]=lam*pa[i]+(1f-lam)*pb[i];
        return r;
    }

    private static float[] appendGoal(float[] s, float[] g) {
        float[] r=new float[s.length+g.length];
        System.arraycopy(s,0,r,0,s.length);System.arraycopy(g,0,r,s.length,g.length);
        return r;
    }

    private static float[] head(float[] x, int n) {
        float[] h=new float[Math.min(n,x.length)];System.arraycopy(x,0,h,0,h.length);return h;
    }

    private static boolean goalReached(float[] state, float[] goal) {
        float d=0; for(int i=0;i<Math.min(state.length,goal.length);i++){float diff=state[i]-goal[i];d+=diff*diff;}
        return Math.sqrt(d) < 0.05;
    }

    private static float[] pad(float[] x,int dim){if(x.length==dim)return x;float[] p=new float[dim];System.arraycopy(x,0,p,0,Math.min(x.length,dim));return p;}

    private double betaSample(float alpha) {
        // Approximation: use ratio of two gamma samples
        double a = gammaSample(alpha), b = gammaSample(alpha);
        return a / (a + b + 1e-8);
    }

    private double gammaSample(float shape) {
        // Marsaglia-Tsang method (simplified for shape < 1: use shape+1 then scale)
        if (shape < 1) return gammaSample(shape+1) * Math.pow(rng.nextDouble(), 1.0/shape);
        double d = shape - 1.0/3.0, c = 1.0/Math.sqrt(9*d);
        while (true) {
            double x = rng.nextGaussian(), v = Math.pow(1+c*x, 3);
            if (v > 0 && Math.log(rng.nextDouble()) < 0.5*x*x + d - d*v + d*Math.log(v)) return d*v;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("augmentCount", augmentCount.get());
        s.put("herCount",     herCount.get());
        return s;
    }
}
