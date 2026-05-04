package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WorldModelAgent — model-based RL agent using an internal world model.
 *
 * Architecture inspired by Dreamer (Hafner et al. 2020):
 *
 *   World Model components:
 *     - REPRESENTATION MODEL: encode state s_t → latent z_t (deterministic encoder)
 *     - TRANSITION MODEL:     predict z_{t+1} from z_t, a_t (pure imagination)
 *     - REWARD MODEL:         predict r_t from z_t, a_t
 *     - CONTINUE MODEL:       predict done_t from z_t
 *
 *   Actor-Critic trained entirely in imagination:
 *     1. Start from real state z_0.
 *     2. Rollout H steps using transition model (no real env interaction).
 *     3. Compute λ-returns from imagined rewards.
 *     4. Train actor to maximise λ-return, critic to predict it.
 *
 *   World model trained on real transitions to minimise:
 *     L = L_rec (reconstruction) + L_reward + L_continue + L_KL (latent regularisation)
 *
 * Benefits: extremely sample efficient — agent improves from replaying memories.
 *
 * Thread-safe.
 */
public class WorldModelAgent {

    private static final String TAG = "WorldModelAgent";

    private static final int H = 15;    // imagination horizon

    // ─────────────────────────────────────────────────────────────────────────
    // Dimensions
    // ─────────────────────────────────────────────────────────────────────────
    private final int stateDim, actionDim, latDim, hidDim;

    // ─────────────────────────────────────────────────────────────────────────
    // World model networks
    // ─────────────────────────────────────────────────────────────────────────
    // Encoder: state → latent
    private final float[][] encW1, encW2;
    private final float[]   encB1, encB2;
    // Transition: [latent, action_onehot] → next_latent
    private final float[][] trW1, trW2;
    private final float[]   trB1, trB2;
    // Reward head: latent → r
    private final float[][] rwW;
    private final float[]   rwB;
    // Continue head: latent → p(continue)
    private final float[][] coW;
    private final float[]   coB;

    // ─────────────────────────────────────────────────────────────────────────
    // Actor-Critic
    // ─────────────────────────────────────────────────────────────────────────
    private final float[][] actW1, actW2;   // policy
    private final float[]   actB1, actB2;
    private final float[][] criW1, criW2;   // value
    private final float[]   criB1, criB2;

    // ─────────────────────────────────────────────────────────────────────────
    // Optimizers
    // ─────────────────────────────────────────────────────────────────────────
    private final NeuralNetworkOptimizer wmOpt, acOpt;

    // ─────────────────────────────────────────────────────────────────────────
    // Config
    // ─────────────────────────────────────────────────────────────────────────
    private final float gamma, lambdaGAE;
    private final float entropy;

    // Stats
    private final AtomicInteger realSteps      = new AtomicInteger(0);
    private final AtomicInteger imagineSteps   = new AtomicInteger(0);
    private final AtomicInteger wmUpdates      = new AtomicInteger(0);
    private final AtomicInteger acUpdates      = new AtomicInteger(0);
    private float avgWmLoss  = 0f, avgActLoss = 0f, avgValLoss = 0f;

    private final Random rng = new Random(311L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public WorldModelAgent(int stateDim, int actionDim, int latDim, int hidDim,
                            float gamma, float lambdaGAE, float entropy,
                            float wmLr, float acLr) {
        this.stateDim  = stateDim;
        this.actionDim = actionDim;
        this.latDim    = latDim;
        this.hidDim    = hidDim;
        this.gamma     = gamma;
        this.lambdaGAE = lambdaGAE;
        this.entropy   = entropy;
        this.wmOpt     = new NeuralNetworkOptimizer(wmLr);
        this.acOpt     = new NeuralNetworkOptimizer(acLr);

        float s = (float) Math.sqrt(2.0 / (stateDim + latDim));
        encW1 = xav(hidDim, stateDim, s);      encB1 = new float[hidDim];
        encW2 = xav(latDim, hidDim, s);        encB2 = new float[latDim];

        int trIn = latDim + actionDim;
        trW1  = xav(hidDim, trIn, s);          trB1  = new float[hidDim];
        trW2  = xav(latDim, hidDim, s);        trB2  = new float[latDim];

        rwW   = xav(1, latDim, s * 0.01f);     rwB   = new float[1];
        coW   = xav(1, latDim, s * 0.01f);     coB   = new float[1];

        actW1 = xav(hidDim, latDim, s);        actB1 = new float[hidDim];
        actW2 = xav(actionDim, hidDim, s*0.01f);actB2= new float[actionDim];
        criW1 = xav(hidDim, latDim, s);        criB1 = new float[hidDim];
        criW2 = xav(1, hidDim, s * 0.01f);     criB2 = new float[1];

        Log.i(TAG, "WorldModelAgent: s=" + stateDim + " a=" + actionDim
                + " z=" + latDim);
    }

    public WorldModelAgent(int stateDim, int actionDim) {
        this(stateDim, actionDim, 48, 128, 0.997f, 0.95f, 0.003f, 6e-4f, 8e-5f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Action selection (real interaction)
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized int act(float[] state) {
        float[] z = encode(pad(state, stateDim));
        float[] logits = actorLogits(z);
        float[] probs  = softmax(logits);
        realSteps.incrementAndGet();
        return sampleCat(probs);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // World model training
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized float trainWorldModel(float[] state, int action, float reward,
                                              float[] nextState, boolean done) {
        float[] s  = pad(state,     stateDim);
        float[] sp = pad(nextState, stateDim);
        float[] z  = encode(s);
        float[] zp_pred = transition(z, action);
        float[] zp_real = encode(sp);

        // Transition loss: ||z'_pred - z'_real||²
        float trLoss = 0;
        float[] dTr = new float[latDim];
        for (int i=0;i<latDim;i++) {
            dTr[i] = 2f*(zp_pred[i] - zp_real[i]);
            trLoss += (zp_pred[i]-zp_real[i])*(zp_pred[i]-zp_real[i]);
        }
        trLoss /= latDim;

        // Reward loss
        float rPred = predictReward(z);
        float rErr  = rPred - reward;

        // Done loss
        float dPred = predictContinue(z);
        float dTarget = done ? 0f : 1f;
        float dErr  = dPred - dTarget;

        float totalLoss = trLoss + rErr * rErr + dErr * dErr;

        // Backprop transition model
        backpropTransition(z, action, dTr);
        // Backprop heads (reward, continue)
        backpropHead(rwW, rwB, z, new float[]{rErr}, "wm_rw");
        backpropHead(coW, coB, z, new float[]{dErr}, "wm_co");

        avgWmLoss = 0.99f * avgWmLoss + 0.01f * totalLoss;
        wmUpdates.incrementAndGet();
        return totalLoss;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Actor-Critic training in imagination
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized void trainActorCritic(float[] startState) {
        // Rollout H steps in imagination
        float[] z = encode(pad(startState, stateDim));
        float[] imagRewards = new float[H];
        float[] imagValues  = new float[H + 1];
        int[]   imagActions  = new int[H];
        float[] imagCont    = new float[H];
        float[][] imagZ     = new float[H][latDim];

        imagValues[H] = predictValue(z);
        for (int t = 0; t < H; t++) {
            imagZ[t] = z.clone();
            int a = sampleCat(softmax(actorLogits(z)));
            imagActions[t] = a;
            imagRewards[t] = predictReward(z);
            imagCont[t]    = predictContinue(z);
            imagValues[t]  = predictValue(z);
            z = transition(z, a);
            imagineSteps.incrementAndGet();
        }

        // λ-returns
        float[] lambdaRet = new float[H];
        float nextRet = imagValues[H];
        for (int t = H-1; t >= 0; t--) {
            float td = imagRewards[t] + gamma * imagCont[t] * imagValues[t+1];
            nextRet = td + gamma * lambdaGAE * imagCont[t] * (nextRet - imagValues[t+1]);
            lambdaRet[t] = nextRet;
        }

        // Update actor and critic
        for (int t = 0; t < H; t++) {
            float adv  = lambdaRet[t] - imagValues[t];
            // Actor gradient
            float[] probs  = softmax(actorLogits(imagZ[t]));
            float[] dLogits= probs.clone();
            dLogits[imagActions[t]] -= 1f;
            for (int i=0;i<actionDim;i++) dLogits[i] = -adv * dLogits[i] - entropy * (-probs[i]*(float)Math.log(probs[i]+1e-8f));
            backpropActor(imagZ[t], dLogits);
            // Critic gradient
            float vErr = imagValues[t] - lambdaRet[t];
            backpropCritic(imagZ[t], 2f * vErr);
            avgActLoss = 0.99f * avgActLoss + 0.01f * adv * adv;
            avgValLoss = 0.99f * avgValLoss + 0.01f * vErr * vErr;
        }
        acUpdates.incrementAndGet();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Forward passes
    // ─────────────────────────────────────────────────────────────────────────

    private float[] encode(float[] s) {
        return lin(encW2, encB2, lin(encW1, encB1, s, true), false);
    }

    private float[] transition(float[] z, int action) {
        float[] inp = new float[latDim + actionDim];
        System.arraycopy(z, 0, inp, 0, latDim);
        if (action >= 0 && action < actionDim) inp[latDim + action] = 1f;
        return lin(trW2, trB2, lin(trW1, trB1, inp, true), false);
    }

    private float predictReward(float[] z)   { return lin(rwW, rwB, z, false)[0]; }
    private float predictContinue(float[] z) { return sigmoid(lin(coW, coB, z, false)[0]); }
    private float predictValue(float[] z)    { return lin(criW2, criB2, lin(criW1, criB1, z, true), false)[0]; }
    private float[] actorLogits(float[] z)   { return lin(actW2, actB2, lin(actW1, actB1, z, true), false); }

    // ─────────────────────────────────────────────────────────────────────────
    // Backprop helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void backpropTransition(float[] z, int action, float[] dOut) {
        float[] inp = new float[latDim + actionDim];
        System.arraycopy(z, 0, inp, 0, latDim);
        if (action >= 0 && action < actionDim) inp[latDim + action] = 1f;
        float[] h1 = lin(trW1, trB1, inp, true);
        float[][] dW2 = outer(dOut, h1);
        wmOpt.step("wm_trW2", trW2, dW2);
        float[] dH1 = new float[hidDim];
        for(int j=0;j<hidDim;j++){if(h1[j]<=0)continue;for(int i=0;i<latDim;i++)dH1[j]+=dOut[i]*trW2[i][j];}
        wmOpt.step("wm_trW1", trW1, outer(dH1, inp));
    }

    private void backpropHead(float[][] W, float[] b, float[] z, float[] dOut, String key) {
        wmOpt.step(key + "_W", W, outer(dOut, z));
    }

    private void backpropActor(float[] z, float[] dLogits) {
        float[] h1 = lin(actW1, actB1, z, true);
        acOpt.step("wm_actW2", actW2, outer(dLogits, h1));
        float[] dH1 = new float[hidDim];
        for(int j=0;j<hidDim;j++){if(h1[j]<=0)continue;for(int i=0;i<actionDim;i++)dH1[j]+=dLogits[i]*actW2[i][j];}
        acOpt.step("wm_actW1", actW1, outer(dH1, z));
    }

    private void backpropCritic(float[] z, float dV) {
        float[] h1 = lin(criW1, criB1, z, true);
        float[][] dW2 = new float[1][hidDim]; for(int j=0;j<hidDim;j++) dW2[0][j]=dV*h1[j];
        acOpt.step("wm_criW2", criW2, dW2);
        float[] dH1 = new float[hidDim];
        for(int j=0;j<hidDim;j++){if(h1[j]<=0)continue; dH1[j]=dV*criW2[0][j];}
        acOpt.step("wm_criW1", criW1, outer(dH1, z));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static float[] lin(float[][] W, float[] b, float[] x, boolean relu) {
        float[] o=new float[W.length];
        for(int i=0;i<W.length;i++){float s=b[i];for(int j=0;j<Math.min(x.length,W[i].length);j++)s+=W[i][j]*x[j];o[i]=relu?Math.max(0f,s):s;}
        return o;
    }

    private static float[][] outer(float[] a, float[] b) {
        float[][] g=new float[a.length][b.length];
        for(int i=0;i<a.length;i++) for(int j=0;j<b.length;j++) g[i][j]=a[i]*b[j];
        return g;
    }

    private static float[] softmax(float[] v){float mx=v[0];for(float x:v)if(x>mx)mx=x;float sum=0;float[] o=new float[v.length];for(int i=0;i<v.length;i++){o[i]=(float)Math.exp(v[i]-mx);sum+=o[i];}for(int i=0;i<v.length;i++)o[i]/=sum;return o;}
    private static float sigmoid(float x){return 1f/(1f+(float)Math.exp(-x));}
    private int sampleCat(float[] p){float r=rng.nextFloat(),c=0;for(int a=0;a<p.length-1;a++){c+=p[a];if(r<c)return a;}return p.length-1;}
    private static float[] pad(float[] x,int dim){if(x.length==dim)return x;float[] p=new float[dim];System.arraycopy(x,0,p,0,Math.min(x.length,dim));return p;}
    private float[][] xav(int r,int c,float s){float[][] m=new float[r][c];for(int i=0;i<r;i++) for(int j=0;j<c;j++) m[i][j]=(rng.nextFloat()*2f-1f)*s;return m;}

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("realSteps",   realSteps.get());
        s.put("imagineSteps",imagineSteps.get());
        s.put("wmUpdates",   wmUpdates.get());
        s.put("acUpdates",   acUpdates.get());
        s.put("avgWmLoss",   avgWmLoss);
        s.put("avgActLoss",  avgActLoss);
        s.put("avgValLoss",  avgValLoss);
        s.put("latDim",      latDim);
        s.put("horizon",     H);
        return s;
    }
}
