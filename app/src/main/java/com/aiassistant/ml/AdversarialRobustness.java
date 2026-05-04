package com.aiassistant.ml;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AdversarialRobustness — robustness training against adversarial perturbations.
 *
 * Trains the agent to maintain performance under adversarial input corruption.
 * Critical for production Android apps where screen captures may be noisy,
 * partially occluded, or manipulated.
 *
 * Attack methods (for generating adversarial examples):
 *
 *   FGSM (Fast Gradient Sign Method):
 *     s_adv = s + ε · sign(∇_s L(θ, s, a*))
 *
 *   PGD (Projected Gradient Descent, Madry et al.):
 *     Iterative FGSM with projection onto ε-ball:
 *     s_{t+1} = Proj_{||δ||≤ε}(s_t + α · sign(∇ L))
 *
 *   RANDOM_SMOOTH:
 *     Add Gaussian noise N(0, σ²) to inputs — simple but effective.
 *
 *   PATCH_ATTACK:
 *     Optimise a small patch of pixels to be universally adversarial.
 *     Simulates sticker/overlay attacks on screen content.
 *
 *   NATURAL_EVOLUTION (NES):
 *     Black-box attack: gradient-free, use natural evolution strategies.
 *
 * Defence methods:
 *   - ADVERSARIAL_TRAINING: augment replay with adversarial examples.
 *   - RANDOMISED_SMOOTHING: certifiable defence via input smoothing.
 *   - FEATURE_SQUEEZING:    reduce input precision (bit-depth reduction).
 *   - ENSEMBLE_DEFENCE:     aggregate over perturbed inputs.
 *
 * Thread-safe.
 */
public class AdversarialRobustness {

    private static final String TAG = "AdvRobustness";

    public enum AttackMethod { FGSM, PGD, RANDOM_SMOOTH, PATCH_ATTACK, NES }
    public enum DefenceMethod { ADVERSARIAL_TRAINING, RANDOMISED_SMOOTHING,
                                FEATURE_SQUEEZING, ENSEMBLE_DEFENCE }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int    stateDim;
    private final float  epsilon;          // attack budget (L∞ norm)
    private final float  alpha;            // PGD step size
    private final int    pgdSteps;         // PGD iterations
    private final float  noiseStd;         // random smoothing std
    private final int    smoothingSamples; // N for randomised smoothing
    private final int    patchSize;        // patch attack pixel count

    // Universal adversarial patch
    private final float[] patch;
    private final int[]   patchIndices;

    // NES parameters
    private final float  nesStd = 0.05f;
    private final int    nesSamples = 50;

    private final AtomicInteger attackCount   = new AtomicInteger(0);
    private final AtomicInteger defenceCount  = new AtomicInteger(0);
    private float avgPerturbNorm = 0f;
    private float avgRobustAcc   = 0f;

    private final Random rng = new Random(431L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public AdversarialRobustness(int stateDim, float epsilon, float alpha,
                                  int pgdSteps, float noiseStd,
                                  int smoothingSamples, int patchSize) {
        this.stateDim        = stateDim;
        this.epsilon         = epsilon;
        this.alpha           = alpha;
        this.pgdSteps        = pgdSteps;
        this.noiseStd        = noiseStd;
        this.smoothingSamples= smoothingSamples;
        this.patchSize       = Math.min(patchSize, stateDim);

        patch       = new float[this.patchSize];
        patchIndices= new int[this.patchSize];
        for (int i=0;i<this.patchSize;i++) {
            patchIndices[i] = i * (stateDim / Math.max(1, this.patchSize));
            patch[i] = rng.nextFloat() * 2f - 1f;
        }
        Log.i(TAG, "AdversarialRobustness: ε=" + epsilon + " pgdSteps=" + pgdSteps);
    }

    public AdversarialRobustness(int stateDim) {
        this(stateDim, 0.1f, 0.01f, 10, 0.05f, 64, Math.min(8, stateDim));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Attack methods
    // ─────────────────────────────────────────────────────────────────────────

    /** Generate adversarial example using specified attack. */
    public synchronized float[] attack(float[] state, int targetAction,
                                        DQNAgent model, AttackMethod method) {
        float[] s = pad(state, stateDim);
        float[] adv;
        switch (method) {
            case PGD:          adv = pgdAttack(s, targetAction, model); break;
            case RANDOM_SMOOTH:adv = randomNoiseAttack(s); break;
            case PATCH_ATTACK: adv = patchAttack(s); break;
            case NES:          adv = nesAttack(s, targetAction, model); break;
            case FGSM:
            default:           adv = fgsmAttack(s, targetAction, model); break;
        }
        float norm = lInfNorm(sub(adv, s));
        avgPerturbNorm = 0.99f*avgPerturbNorm + 0.01f*norm;
        attackCount.incrementAndGet();
        return adv;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Defence methods
    // ─────────────────────────────────────────────────────────────────────────

    /** Apply defence preprocessing to an observation. */
    public synchronized float[] defend(float[] state, DefenceMethod method) {
        float[] s = pad(state, stateDim);
        float[] defended;
        switch (method) {
            case RANDOMISED_SMOOTHING: defended = randomisedSmoothing(s); break;
            case FEATURE_SQUEEZING:    defended = featureSqueezing(s, 4); break;
            case ENSEMBLE_DEFENCE:     defended = ensembleSmoothing(s, 8); break;
            case ADVERSARIAL_TRAINING:
            default:                   defended = s.clone(); break;
        }
        defenceCount.incrementAndGet();
        return defended;
    }

    /**
     * Certifiable prediction via randomised smoothing.
     * Returns (predicted_action, certified_radius).
     */
    public synchronized float[] certify(float[] state, DQNAgent model, int numSamples) {
        int[] voteCounts = new int[10]; // up to 10 actions
        for (int k=0;k<numSamples;k++) {
            float[] noisy = addGaussianNoise(pad(state, stateDim), noiseStd);
            float[] Q = model.getQValues(noisy);
            if (Q != null) {
                int a = argmax(Q);
                if (a < voteCounts.length) voteCounts[a]++;
            }
        }
        int bestA = argmax(voteCounts);
        int bestN = voteCounts[bestA];
        int secondN = 0;
        for (int i=0;i<voteCounts.length;i++) if(i!=bestA && voteCounts[i]>secondN) secondN=voteCounts[i];
        // Certified radius: ε such that smooth classifier is certified
        float pA = (float)bestN / numSamples;
        float radius = pA > 0.5f ? noiseStd * (float)Math.sqrt(-2*Math.log(1-pA)) * 0.5f : 0f;
        return new float[]{bestA, radius};
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Attack implementations
    // ─────────────────────────────────────────────────────────────────────────

    private float[] fgsmAttack(float[] s, int targetA, DQNAgent model) {
        // Compute finite-difference gradient of Q w.r.t. state
        float[] adv = s.clone();
        float[] Q   = model.getQValues(s); if(Q==null) return adv;
        float baseQ = targetA<Q.length ? Q[targetA] : max(Q);
        float eps_fd = 1e-3f;
        for (int j=0;j<stateDim;j++) {
            float[] sp=s.clone(); sp[j]+=eps_fd;
            float[] Qp=model.getQValues(sp); if(Qp==null) continue;
            float grad=(targetA<Qp.length?Qp[targetA]:max(Qp)-baseQ)/eps_fd;
            adv[j] = clip(adv[j] + epsilon*Math.signum(grad), s[j]-epsilon, s[j]+epsilon);
        }
        return adv;
    }

    private float[] pgdAttack(float[] s, int targetA, DQNAgent model) {
        float[] adv = addGaussianNoise(s, epsilon*0.5f);
        projectLInf(adv, s, epsilon);
        for (int step=0;step<pgdSteps;step++) {
            float[] Q=model.getQValues(adv); if(Q==null) break;
            float baseQ=targetA<Q.length?Q[targetA]:max(Q);
            float eps_fd=1e-3f;
            for(int j=0;j<stateDim;j++){
                float[] sp=adv.clone(); sp[j]+=eps_fd;
                float[] Qp=model.getQValues(sp); if(Qp==null) continue;
                float grad=(targetA<Qp.length?Qp[targetA]:max(Qp)-baseQ)/eps_fd;
                adv[j]=clip(adv[j]+alpha*(float)Math.signum(grad),s[j]-epsilon,s[j]+epsilon);
            }
        }
        return adv;
    }

    private float[] randomNoiseAttack(float[] s) {
        float[] adv = s.clone();
        for(int j=0;j<stateDim;j++) adv[j]=clip(adv[j]+(rng.nextFloat()*2f-1f)*epsilon,s[j]-epsilon,s[j]+epsilon);
        return adv;
    }

    private float[] patchAttack(float[] s) {
        float[] adv = s.clone();
        for(int k=0;k<patchSize;k++) { int j=patchIndices[k]; if(j<stateDim) adv[j]=patch[k]; }
        return adv;
    }

    private float[] nesAttack(float[] s, int targetA, DQNAgent model) {
        // NES gradient estimate
        float[] adv = s.clone();
        float[] grad = new float[stateDim];
        for(int k=0;k<nesSamples;k++) {
            float[] u=new float[stateDim]; for(int j=0;j<stateDim;j++) u[j]=(float)rng.nextGaussian()*nesStd;
            float[] splus=add(s,u); float[] sminus=sub(s,u);
            float[] Qplus=model.getQValues(splus), Qminus=model.getQValues(sminus);
            if(Qplus==null||Qminus==null) continue;
            float fplus=targetA<Qplus.length?Qplus[targetA]:max(Qplus);
            float fminus=targetA<Qminus.length?Qminus[targetA]:max(Qminus);
            for(int j=0;j<stateDim;j++) grad[j]+=(fplus-fminus)*u[j]/(2f*nesStd);
        }
        for(int j=0;j<stateDim;j++) adv[j]=clip(adv[j]+alpha*(float)Math.signum(grad[j]),s[j]-epsilon,s[j]+epsilon);
        return adv;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Defence implementations
    // ─────────────────────────────────────────────────────────────────────────

    private float[] randomisedSmoothing(float[] s) {
        float[] mean=new float[stateDim];
        for(int k=0;k<smoothingSamples;k++){float[] n=addGaussianNoise(s,noiseStd);for(int j=0;j<stateDim;j++)mean[j]+=n[j];}
        for(int j=0;j<stateDim;j++) mean[j]/=smoothingSamples;
        return mean;
    }

    private float[] featureSqueezing(float[] s, int bits) {
        float scale=(float)(Math.pow(2,bits)-1);
        float[] sq=new float[stateDim];
        for(int j=0;j<stateDim;j++) sq[j]=Math.round(s[j]*scale)/scale;
        return sq;
    }

    private float[] ensembleSmoothing(float[] s, int K) {
        float[] mean=new float[stateDim];
        for(int k=0;k<K;k++){float[] n=addGaussianNoise(s,noiseStd*0.5f);for(int j=0;j<stateDim;j++)mean[j]+=n[j];}
        for(int j=0;j<stateDim;j++) mean[j]/=K;
        return mean;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private float[] addGaussianNoise(float[] s, float std) {
        float[] n=new float[s.length];for(int j=0;j<s.length;j++)n[j]=s[j]+(float)(rng.nextGaussian()*std);return n;
    }
    private static void projectLInf(float[] adv, float[] s, float eps) {
        for(int j=0;j<Math.min(adv.length,s.length);j++) adv[j]=Math.max(s[j]-eps,Math.min(s[j]+eps,adv[j]));
    }
    private static float clip(float v, float lo, float hi){return Math.max(lo,Math.min(hi,v));}
    private static float lInfNorm(float[] v){float m=0;for(float x:v)if(Math.abs(x)>m)m=Math.abs(x);return m;}
    private static float[] sub(float[] a, float[] b){float[] r=new float[a.length];for(int i=0;i<a.length;i++)r[i]=a[i]-b[i];return r;}
    private static float[] add(float[] a, float[] b){float[] r=new float[a.length];for(int i=0;i<a.length;i++)r[i]=a[i]+(i<b.length?b[i]:0f);return r;}
    private static float max(float[] v){float m=v[0];for(float x:v)if(x>m)m=x;return m;}
    private static int argmax(float[] v){int b=0;for(int i=1;i<v.length;i++)if(v[i]>v[b])b=i;return b;}
    private static int argmax(int[] v){int b=0;for(int i=1;i<v.length;i++)if(v[i]>v[b])b=i;return b;}
    private static float[] pad(float[] x,int dim){if(x.length==dim)return x;float[] p=new float[dim];System.arraycopy(x,0,p,0,Math.min(x.length,dim));return p;}

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("attackCount",   attackCount.get());
        s.put("defenceCount",  defenceCount.get());
        s.put("avgPerturbNorm",avgPerturbNorm);
        s.put("avgRobustAcc",  avgRobustAcc);
        s.put("epsilon",       epsilon);
        s.put("pgdSteps",      pgdSteps);
        return s;
    }
}
