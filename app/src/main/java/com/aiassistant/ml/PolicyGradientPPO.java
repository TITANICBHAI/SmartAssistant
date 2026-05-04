package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PolicyGradientPPO — full Proximal Policy Optimisation (Schulman et al. 2017).
 *
 * PPO is one of the most widely used deep RL algorithms. It improves on
 * vanilla policy gradient by clipping the probability ratio to prevent
 * destructively large updates:
 *
 *   L_CLIP = E[ min( r_t(θ)·A_t, clip(r_t(θ), 1-ε, 1+ε)·A_t ) ]
 *   r_t(θ) = π_θ(a|s) / π_θ_old(a|s)
 *
 * Combined loss:
 *   L = -L_CLIP + c1·L_VF - c2·H(π)
 *   where L_VF = (V(s) - V_target)²  and  H(π) = entropy bonus
 *
 * This implementation:
 *   - Maintains an actor (policy) and critic (value function) network.
 *   - Collects a rollout buffer of T steps.
 *   - Computes generalised advantage estimates (GAE, λ-returns).
 *   - Runs K epochs of mini-batch gradient updates.
 *   - Tracks KL divergence to detect policy instability.
 *
 * Thread-safe.
 */
public class PolicyGradientPPO {

    private static final String TAG = "PPO";

    // ─────────────────────────────────────────────────────────────────────────
    // Rollout buffer entry
    // ─────────────────────────────────────────────────────────────────────────
    private static class RolloutEntry {
        float[] state;
        int     action;
        float   reward, value, logProb;
        boolean done;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Actor network
    // ─────────────────────────────────────────────────────────────────────────
    private final float[][] actW1, actW2;
    private final float[]   actB1, actB2;
    // Critic network
    private final float[][] criW1, criW2;
    private final float[]   criB1, criB2;

    private final NeuralNetworkOptimizer actOpt, criOpt;

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int   stateDim, actionDim, hidDim;
    private final float gamma, gae_lambda;
    private final float clipEpsilon;
    private final float c1, c2;         // value loss coeff, entropy coeff
    private final int   rolloutLen, ppoEpochs, miniBatchSize;

    private final List<RolloutEntry> rollout = new ArrayList<>();
    private final AtomicInteger      updateCount = new AtomicInteger(0);
    private float avgPolicyLoss = 0f, avgValueLoss = 0f, avgEntropy = 0f;
    private float avgKL = 0f, avgClipFrac = 0f;

    private final Random rng = new Random(271L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public PolicyGradientPPO(int stateDim, int actionDim, int hidDim,
                              float gamma, float gae_lambda, float clipEpsilon,
                              float c1, float c2, int rolloutLen,
                              int ppoEpochs, int miniBatchSize,
                              float actLr, float criLr) {
        this.stateDim     = stateDim;
        this.actionDim    = actionDim;
        this.hidDim       = hidDim;
        this.gamma        = gamma;
        this.gae_lambda   = gae_lambda;
        this.clipEpsilon  = clipEpsilon;
        this.c1           = c1;
        this.c2           = c2;
        this.rolloutLen   = rolloutLen;
        this.ppoEpochs    = ppoEpochs;
        this.miniBatchSize= miniBatchSize;
        this.actOpt       = new NeuralNetworkOptimizer(actLr);
        this.criOpt       = new NeuralNetworkOptimizer(criLr);

        float s = (float) Math.sqrt(2.0 / (stateDim + hidDim));
        actW1 = xav(hidDim, stateDim, s); actB1 = new float[hidDim];
        actW2 = xav(actionDim, hidDim, s * 0.01f); actB2 = new float[actionDim];
        criW1 = xav(hidDim, stateDim, s); criB1 = new float[hidDim];
        criW2 = xav(1, hidDim, s * 0.01f);         criB2 = new float[1];

        Log.i(TAG, "PPO: s=" + stateDim + " a=" + actionDim + " h=" + hidDim
                + " ε=" + clipEpsilon + " rollout=" + rolloutLen);
    }

    public PolicyGradientPPO(int stateDim, int actionDim) {
        this(stateDim, actionDim, 256, 0.99f, 0.95f, 0.2f, 0.5f, 0.01f,
             2048, 10, 64, 3e-4f, 1e-3f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Action selection
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized int selectAction(float[] state) {
        float[] logits = actorForward(pad(state, stateDim));
        float[] probs  = softmax(logits);
        return sampleCategorical(probs);
    }

    public synchronized float[] actionProbs(float[] state) {
        return softmax(actorForward(pad(state, stateDim)));
    }

    public synchronized float value(float[] state) {
        return criticForward(pad(state, stateDim));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rollout collection
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized void addStep(float[] state, int action, float reward,
                                     boolean done) {
        float[] s   = pad(state, stateDim);
        float[] pr  = actionProbs(state);
        float logP  = (float) Math.log(Math.max(pr[Math.min(action, actionDim-1)], 1e-8f));
        float V     = criticForward(s);

        RolloutEntry e = new RolloutEntry();
        e.state = s; e.action = action; e.reward = reward;
        e.value = V; e.logProb = logP; e.done = done;
        rollout.add(e);

        if (rollout.size() >= rolloutLen) update();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PPO update
    // ─────────────────────────────────────────────────────────────────────────

    private void update() {
        int T = rollout.size();
        // Compute GAE advantages
        float[] advantages = new float[T];
        float[] returns    = new float[T];
        float gae = 0f, nextVal = 0f;
        for (int t = T - 1; t >= 0; t--) {
            RolloutEntry e = rollout.get(t);
            float delta = e.reward + (e.done ? 0f : gamma * nextVal) - e.value;
            gae = delta + (e.done ? 0f : gamma * gae_lambda * gae);
            advantages[t] = gae;
            returns[t]    = gae + e.value;
            nextVal       = e.value;
        }
        // Normalise advantages
        float advMean = 0, advStd = 0;
        for (float a : advantages) advMean += a; advMean /= T;
        for (float a : advantages) { float d = a - advMean; advStd += d*d; } advStd = (float)Math.sqrt(advStd/T + 1e-8f);
        for (int t = 0; t < T; t++) advantages[t] = (advantages[t] - advMean) / advStd;

        // Store old log probs
        float[] oldLogProbs = new float[T];
        for (int t = 0; t < T; t++) oldLogProbs[t] = rollout.get(t).logProb;

        // PPO epochs
        float totPL = 0, totVL = 0, totH = 0, totKL = 0, totClip = 0;
        for (int epoch = 0; epoch < ppoEpochs; epoch++) {
            for (int start = 0; start < T; start += miniBatchSize) {
                int end = Math.min(start + miniBatchSize, T);
                for (int t = start; t < end; t++) {
                    RolloutEntry e = rollout.get(t);
                    float[] probs  = softmax(actorForward(e.state));
                    int     a      = Math.min(e.action, actionDim - 1);
                    float   logP   = (float) Math.log(Math.max(probs[a], 1e-8f));
                    float   ratio  = (float) Math.exp(logP - oldLogProbs[t]);
                    float   clpRat = Math.max(1-clipEpsilon, Math.min(1+clipEpsilon, ratio));
                    float   adv    = advantages[t];
                    float   pLoss  = -Math.min(ratio*adv, clpRat*adv);
                    totPL   += pLoss;
                    totClip += (ratio != clpRat ? 1 : 0);
                    // KL
                    totKL   += logP - oldLogProbs[t];
                    // Entropy
                    float H = 0; for (float p : probs) if (p > 1e-8f) H -= p*(float)Math.log(p);
                    totH += H;
                    // Value loss
                    float V    = criticForward(e.state);
                    float vLoss= (V - returns[t]) * (V - returns[t]);
                    totVL += vLoss;
                    // Backprop actor
                    float[] dLogits = probs.clone();
                    dLogits[a] -= 1f;
                    float scale = pLoss - c2 * H;
                    for (int i = 0; i < actionDim; i++) dLogits[i] *= scale;
                    actorBackprop(e.state, dLogits);
                    // Backprop critic
                    criticBackprop(e.state, 2f * (V - returns[t]));
                }
            }
        }
        int n = T * ppoEpochs;
        avgPolicyLoss = 0.99f * avgPolicyLoss + 0.01f * totPL / n;
        avgValueLoss  = 0.99f * avgValueLoss  + 0.01f * totVL / n;
        avgEntropy    = 0.99f * avgEntropy    + 0.01f * totH  / n;
        avgKL         = 0.99f * avgKL         + 0.01f * Math.abs(totKL / n);
        avgClipFrac   = 0.99f * avgClipFrac   + 0.01f * totClip / n;
        updateCount.incrementAndGet();
        rollout.clear();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Network forward / backward
    // ─────────────────────────────────────────────────────────────────────────

    private float[] actorForward(float[] s) {
        return lin(actW2, actB2, lin(actW1, actB1, s, true), false);
    }

    private float criticForward(float[] s) {
        return lin(criW2, criB2, lin(criW1, criB1, s, true), false)[0];
    }

    private void actorBackprop(float[] s, float[] dLogits) {
        float[] h = lin(actW1, actB1, s, true);
        float[][] dW2 = new float[actionDim][hidDim];
        for (int i=0;i<actionDim;i++) for(int j=0;j<hidDim;j++) dW2[i][j]=dLogits[i]*h[j];
        actOpt.step("ppo_actW2", actW2, dW2);
        float[] dH = new float[hidDim];
        for (int j=0;j<hidDim;j++){if(h[j]<=0)continue;for(int i=0;i<actionDim;i++)dH[j]+=dLogits[i]*actW2[i][j];}
        float[][] dW1 = new float[hidDim][stateDim];
        for (int i=0;i<hidDim;i++) for(int j=0;j<stateDim;j++) dW1[i][j]=dH[i]*s[j];
        actOpt.step("ppo_actW1", actW1, dW1);
    }

    private void criticBackprop(float[] s, float dV) {
        float[] h = lin(criW1, criB1, s, true);
        float[][] dW2 = new float[1][hidDim];
        for (int j=0;j<hidDim;j++) dW2[0][j]=dV*h[j];
        criOpt.step("ppo_criW2", criW2, dW2);
        float[] dH = new float[hidDim];
        for (int j=0;j<hidDim;j++){if(h[j]<=0)continue; dH[j]=dV*criW2[0][j];}
        float[][] dW1 = new float[hidDim][stateDim];
        for (int i=0;i<hidDim;i++) for(int j=0;j<stateDim;j++) dW1[i][j]=dH[i]*s[j];
        criOpt.step("ppo_criW1", criW1, dW1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static float[] lin(float[][] W, float[] b, float[] x, boolean relu) {
        float[] o=new float[W.length];
        for(int i=0;i<W.length;i++){float s=b[i];for(int j=0;j<Math.min(x.length,W[i].length);j++)s+=W[i][j]*x[j];o[i]=relu?Math.max(0f,s):s;}
        return o;
    }

    private static float[] softmax(float[] v) {
        float mx=v[0];for(float x:v)if(x>mx)mx=x;
        float sum=0;float[] o=new float[v.length];
        for(int i=0;i<v.length;i++){o[i]=(float)Math.exp(v[i]-mx);sum+=o[i];}
        for(int i=0;i<v.length;i++)o[i]/=sum;
        return o;
    }

    private int sampleCategorical(float[] probs) {
        float r=rng.nextFloat(),cum=0;
        for(int a=0;a<probs.length-1;a++){cum+=probs[a];if(r<cum)return a;}
        return probs.length-1;
    }

    private static float[] pad(float[] x, int dim) {
        if(x.length==dim)return x;
        float[] p=new float[dim];System.arraycopy(x,0,p,0,Math.min(x.length,dim));return p;
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
        s.put("updateCount",   updateCount.get());
        s.put("rolloutFill",   rollout.size());
        s.put("avgPolicyLoss", avgPolicyLoss);
        s.put("avgValueLoss",  avgValueLoss);
        s.put("avgEntropy",    avgEntropy);
        s.put("avgKL",         avgKL);
        s.put("avgClipFrac",   avgClipFrac);
        s.put("clipEpsilon",   clipEpsilon);
        return s;
    }
}
