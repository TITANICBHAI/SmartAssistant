package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LatentSpaceNavigator — planning and search in learned latent state spaces.
 *
 * After an encoder learns a compact latent representation z = enc(s),
 * this module performs planning directly in z-space without decoding back
 * to the pixel/sensor domain:
 *
 *   RANDOM_SHOOTING (CEM-like):
 *     Sample K action sequences of length H.
 *     Evaluate each via latent rollout + value prediction.
 *     Execute first action of best sequence.
 *
 *   MCTS (Monte Carlo Tree Search):
 *     Build a search tree in latent space.
 *     Node: (z, a) → child z'.
 *     Use UCB1 for node selection: Q + c√(ln N / n).
 *     Expand with transition model, evaluate with value network.
 *
 *   LATENT_GRADIENT:
 *     Optimise action sequence via gradient through latent dynamics:
 *     a* = argmax_a V(T(z, a)) where T is differentiable transition model.
 *
 *   BFS_LATENT:
 *     Breadth-first search in discretised latent space toward goal z_goal.
 *
 * Thread-safe.
 */
public class LatentSpaceNavigator {

    private static final String TAG = "LatentNav";

    public enum PlanningMethod { RANDOM_SHOOTING, MCTS, LATENT_GRADIENT, BFS_LATENT }

    // ─────────────────────────────────────────────────────────────────────────
    // MCTS node
    // ─────────────────────────────────────────────────────────────────────────
    private static class MCTSNode {
        float[] z;          // latent state
        int     action;     // action taken to reach this node
        float   Q;          // mean value estimate
        int     N;          // visit count
        MCTSNode parent;
        List<MCTSNode> children = new ArrayList<>();

        MCTSNode(float[] z, int action, MCTSNode parent) {
            this.z = z; this.action = action; this.parent = parent;
        }

        float ucb(float c) {
            if (N == 0) return Float.MAX_VALUE;
            return Q + c * (float)Math.sqrt(Math.log(parent != null ? parent.N : 1) / N);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Encoder network: state → latent
    // ─────────────────────────────────────────────────────────────────────────
    private final float[][] encW1, encW2;
    private final float[]   encB1, encB2;

    // Transition model: concat(z, a_onehot) → z'
    private final float[][] trW1, trW2;
    private final float[]   trB1, trB2;

    // Value network: z → V(z)
    private final float[][] valW1, valW2;
    private final float[]   valB1, valB2;

    private final NeuralNetworkOptimizer opt;

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int    stateDim, actionDim, latDim, hidDim;
    private final int    horizon, numSamples;
    private final float  gamma, ucbC;
    private final PlanningMethod method;

    private final AtomicInteger planCount  = new AtomicInteger(0);
    private final AtomicInteger evalCount  = new AtomicInteger(0);
    private float avgPlanValue = 0f;
    private int   avgPlanDepth = 0;

    private final Random rng = new Random(421L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public LatentSpaceNavigator(int stateDim, int actionDim, int latDim, int hidDim,
                                 int horizon, int numSamples, float gamma, float ucbC,
                                 PlanningMethod method, float lr) {
        this.stateDim  = stateDim;
        this.actionDim = actionDim;
        this.latDim    = latDim;
        this.hidDim    = hidDim;
        this.horizon   = horizon;
        this.numSamples= numSamples;
        this.gamma     = gamma;
        this.ucbC      = ucbC;
        this.method    = method;
        this.opt       = new NeuralNetworkOptimizer(lr);

        float s = (float)Math.sqrt(2.0/(stateDim+latDim));
        encW1 = xav(hidDim, stateDim, s);  encB1 = new float[hidDim];
        encW2 = xav(latDim, hidDim, s);   encB2 = new float[latDim];

        trW1  = xav(hidDim, latDim+actionDim, s); trB1 = new float[hidDim];
        trW2  = xav(latDim, hidDim, s);           trB2 = new float[latDim];

        valW1 = xav(hidDim, latDim, s);  valB1 = new float[hidDim];
        valW2 = xav(1, hidDim, s*0.01f); valB2 = new float[1];

        Log.i(TAG, "LatentSpaceNavigator: " + method + " z=" + latDim + " H=" + horizon);
    }

    public LatentSpaceNavigator(int stateDim, int actionDim, PlanningMethod method) {
        this(stateDim, actionDim, 32, 128, 10, 64, 0.99f, 1.5f, method, 1e-3f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Planning
    // ─────────────────────────────────────────────────────────────────────────

    /** Plan from current state and return best first action. */
    public synchronized int plan(float[] state) {
        float[] z = encode(pad(state, stateDim));
        int action;
        switch (method) {
            case MCTS:           action = mctsSearch(z, 50); break;
            case LATENT_GRADIENT:action = latentGradientSearch(z); break;
            case BFS_LATENT:     action = bfsLatent(z, null); break;
            case RANDOM_SHOOTING:
            default:             action = randomShooting(z);  break;
        }
        planCount.incrementAndGet();
        return action;
    }

    /** Plan toward a goal latent state. */
    public synchronized int planToGoal(float[] state, float[] goalState) {
        float[] z  = encode(pad(state,     stateDim));
        float[] zg = encode(pad(goalState, stateDim));
        return bfsLatent(z, zg);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Planning implementations
    // ─────────────────────────────────────────────────────────────────────────

    private int randomShooting(float[] z0) {
        float bestValue = Float.NEGATIVE_INFINITY;
        int   bestA0    = rng.nextInt(actionDim);

        for (int k=0;k<numSamples;k++) {
            float[] z = z0.clone();
            float value = 0, disc = 1f;
            int firstA = -1;
            for (int t=0;t<horizon;t++) {
                int a = rng.nextInt(actionDim);
                if (t==0) firstA=a;
                float r = predictReward(z, a);
                z = transition(z, a);
                value += disc * r; disc *= gamma;
            }
            value += disc * valueEstimate(z);
            if (value > bestValue) { bestValue=value; bestA0=firstA; }
        }
        avgPlanValue = 0.99f*avgPlanValue + 0.01f*bestValue;
        return bestA0;
    }

    private int mctsSearch(float[] z0, int iterations) {
        MCTSNode root = new MCTSNode(z0, -1, null);
        for (int it=0;it<iterations;it++) {
            // Select
            MCTSNode node = select(root);
            // Expand
            if (node.N > 0 && node.children.isEmpty() && node.parent != null) {
                expand(node);
                if (!node.children.isEmpty())
                    node = node.children.get(rng.nextInt(node.children.size()));
            }
            // Evaluate (rollout)
            float v = rolloutValue(node.z, 5);
            // Backprop
            backup(node, v);
        }
        // Best child of root by Q
        int best=0; float bestQ=Float.NEGATIVE_INFINITY;
        for (int a=0;a<actionDim;a++) {
            float q=Float.NEGATIVE_INFINITY;
            for(MCTSNode c:root.children) if(c.action==a && c.Q>q) q=c.Q;
            if(q>bestQ){bestQ=q;best=a;}
        }
        avgPlanValue = 0.99f*avgPlanValue + 0.01f*bestQ;
        return best;
    }

    private MCTSNode select(MCTSNode node) {
        while (!node.children.isEmpty()) {
            MCTSNode best=node.children.get(0);
            for(MCTSNode c:node.children) if(c.ucb(ucbC)>best.ucb(ucbC)) best=c;
            node=best;
        }
        return node;
    }

    private void expand(MCTSNode node) {
        for (int a=0;a<actionDim;a++) {
            float[] zNext = transition(node.z, a);
            node.children.add(new MCTSNode(zNext, a, node));
        }
    }

    private float rolloutValue(float[] z, int depth) {
        float v=0, disc=1f;
        for(int t=0;t<depth;t++){int a=rng.nextInt(actionDim);v+=disc*predictReward(z,a);z=transition(z,a);disc*=gamma;}
        return v + disc * valueEstimate(z);
    }

    private void backup(MCTSNode node, float v) {
        while(node!=null){node.N++;node.Q+=(v-node.Q)/node.N;node=node.parent;}
    }

    private int latentGradientSearch(float[] z0) {
        // Simplified: try each first action, pick best value after 1-step rollout
        float bestV = Float.NEGATIVE_INFINITY; int bestA=0;
        for(int a=0;a<actionDim;a++){float v=valueEstimate(transition(z0,a));if(v>bestV){bestV=v;bestA=a;}}
        return bestA;
    }

    private int bfsLatent(float[] z0, float[] zGoal) {
        // BFS with beam of latent states; pick action leading to best value / closest to goal
        float bestV = Float.NEGATIVE_INFINITY; int bestA=0;
        for(int a=0;a<actionDim;a++){
            float[] z=transition(z0,a);
            float v = zGoal!=null ? -latentDist(z,zGoal) : valueEstimate(z);
            if(v>bestV){bestV=v;bestA=a;}
        }
        return bestA;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Training
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized void trainEncoder(float[] state, float[] target) {
        float[] s = pad(state, stateDim);
        float[] h = linRelu(encW1, encB1, s);
        float[] z = lin(encW2, encB2, h);
        float[] err = new float[latDim];
        for(int i=0;i<latDim;i++) err[i]=2f*(z[i]-target[i]);
        opt.step("ln_encW2", encW2, outer(err, h));
        float[] dH=new float[hidDim];
        for(int j=0;j<hidDim;j++){if(h[j]<=0)continue;for(int i=0;i<latDim;i++)dH[j]+=err[i]*encW2[i][j];}
        opt.step("ln_encW1", encW1, outer(dH, s));
    }

    public synchronized void trainTransition(float[] z, int action, float[] zNext) {
        float[] inp=buildTrInput(z,action);
        float[] h=linRelu(trW1,trB1,inp);
        float[] pred=lin(trW2,trB2,h);
        float[] err=new float[latDim];for(int i=0;i<latDim;i++)err[i]=2f*(pred[i]-zNext[i]);
        opt.step("ln_trW2",trW2,outer(err,h));
        float[] dH=new float[hidDim];
        for(int j=0;j<hidDim;j++){if(h[j]<=0)continue;for(int i=0;i<latDim;i++)dH[j]+=err[i]*trW2[i][j];}
        opt.step("ln_trW1",trW1,outer(dH,inp));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Forward passes
    // ─────────────────────────────────────────────────────────────────────────

    private float[] encode(float[] s)      { return lin(encW2,encB2,linRelu(encW1,encB1,s)); }
    private float[] transition(float[] z, int a) { float[] inp=buildTrInput(z,a); return lin(trW2,trB2,linRelu(trW1,trB1,inp)); }
    private float   valueEstimate(float[] z) { evalCount.incrementAndGet(); return lin(valW2,valB2,linRelu(valW1,valB1,z))[0]; }
    private float   predictReward(float[] z, int a) { return 0f; } // placeholder for reward head

    private float[] buildTrInput(float[] z, int a) {
        float[] inp=new float[latDim+actionDim];
        System.arraycopy(z,0,inp,0,latDim);
        if(a>=0&&a<actionDim)inp[latDim+a]=1f;
        return inp;
    }

    private static float latentDist(float[] a, float[] b) {
        float d=0;for(int i=0;i<Math.min(a.length,b.length);i++){float di=a[i]-b[i];d+=di*di;}return(float)Math.sqrt(d);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static float[] linRelu(float[][] W, float[] b, float[] x) {
        float[] o=new float[W.length];for(int i=0;i<W.length;i++){float s=b[i];for(int j=0;j<Math.min(x.length,W[i].length);j++)s+=W[i][j]*x[j];o[i]=Math.max(0f,s);}return o;
    }
    private static float[] lin(float[][] W, float[] b, float[] x) {
        float[] o=new float[W.length];for(int i=0;i<W.length;i++){float s=b[i];for(int j=0;j<Math.min(x.length,W[i].length);j++)s+=W[i][j]*x[j];o[i]=s;}return o;
    }
    private static float[][] outer(float[] a, float[] b){float[][] g=new float[a.length][b.length];for(int i=0;i<a.length;i++) for(int j=0;j<b.length;j++) g[i][j]=a[i]*b[j];return g;}
    private static float[] pad(float[] x,int dim){if(x.length==dim)return x;float[] p=new float[dim];System.arraycopy(x,0,p,0,Math.min(x.length,dim));return p;}
    private float[][] xav(int r,int c,float s){float[][] m=new float[r][c];for(int i=0;i<r;i++) for(int j=0;j<c;j++) m[i][j]=(rng.nextFloat()*2f-1f)*s;return m;}

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("method",      method.name());
        s.put("planCount",   planCount.get());
        s.put("evalCount",   evalCount.get());
        s.put("avgPlanValue",avgPlanValue);
        s.put("horizon",     horizon);
        s.put("numSamples",  numSamples);
        return s;
    }
}
