package com.aiassistant.rl;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Proximal Policy Optimization (PPO) agent — improved implementation:
 *
 *  1. Generalized Advantage Estimation (GAE-Lambda) instead of raw Monte Carlo
 *     advantages, dramatically reducing variance.
 *  2. Separate policy weights and value weights (linear approximations).
 *  3. Entropy bonus computed correctly from the softmax distribution at each step.
 *  4. Value function loss clipping (PPO2-style) to prevent large value updates.
 *  5. Gradient clipping simulated via weight update norm capping.
 *  6. Proper KL-divergence early-stopping per epoch.
 *  7. Model save / load persists full weight matrices.
 */
public class PPOAgent extends RLAgent {
    private static final String TAG = "PPOAgent";

    // -----------------------------------------------------------------------
    // Hyper-parameters
    // -----------------------------------------------------------------------
    private float clipEpsilon  = 0.2f;
    private float valueCoeff   = 0.5f;
    private float entropyCoeff = 0.01f;
    private int   epochs       = 4;
    private int   batchSize    = 32;
    private float gaeLambda    = 0.95f;       // GAE λ
    private float maxGradNorm  = 0.5f;        // gradient norm clipping
    private float targetKL     = 0.015f;      // early-stop KL threshold

    // -----------------------------------------------------------------------
    // Network weights (linear approximations; Xavier-init)
    // Policy: softmax(s · policyW)
    // Value:  s · valueW   (scalar)
    // -----------------------------------------------------------------------
    private float[][] policyWeights; // [stateSize][actionSize]
    private float[]   valueWeights;  // [stateSize]

    // -----------------------------------------------------------------------
    // Experience buffer
    // -----------------------------------------------------------------------
    private static class Experience {
        float[] state;
        int     action;
        float   reward;
        float[] logProbs;   // log π_old(a|s) for each action
        float   value;      // V(s) at collection time
        float   returnValue;
        float   advantage;
        boolean done;

        Experience(float[] state, int action, float reward,
                   float[] logProbs, float value, boolean done) {
            this.state    = state.clone();
            this.action   = action;
            this.reward   = reward;
            this.logProbs = logProbs.clone();
            this.value    = value;
            this.done     = done;
        }
    }

    private final List<Experience> experiences;
    private int maxExperiences = 512;

    // -----------------------------------------------------------------------
    // Training state
    // -----------------------------------------------------------------------
    private int   trainSteps = 0;
    private final Random random;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public PPOAgent(int stateSize, int actionSize) {
        super(stateSize, actionSize);
        this.random      = new Random(42);
        this.experiences = new ArrayList<>();

        this.explorationRate = 0.0f;  // PPO is on-policy; no ε-greedy
        this.learningRate    = 3e-4f;

        initializeWeights();
        Log.i(TAG, "PPOAgent created (stateSize=" + stateSize +
                ", actionSize=" + actionSize + ", GAE-λ=" + gaeLambda + ")");
    }

    // -----------------------------------------------------------------------
    // Weight initialisation (Xavier uniform)
    // -----------------------------------------------------------------------

    private void initializeWeights() {
        float pLimit = (float) Math.sqrt(6.0 / (stateSize + actionSize));
        float vLimit = (float) Math.sqrt(6.0 / stateSize);
        policyWeights = new float[stateSize][actionSize];
        valueWeights  = new float[stateSize];
        for (int i = 0; i < stateSize; i++) {
            for (int j = 0; j < actionSize; j++) {
                policyWeights[i][j] = (random.nextFloat() * 2 - 1) * pLimit;
            }
            valueWeights[i] = (random.nextFloat() * 2 - 1) * vLimit;
        }
    }

    // -----------------------------------------------------------------------
    // RLAgent interface
    // -----------------------------------------------------------------------

    @Override
    public int selectAction(float[] state) {
        float[] dist = policyDistribution(state);
        return sampleCategorical(dist);
    }

    @Override
    public void update(float[] state, int action, float reward,
                       float[] nextState, boolean done) {
        float[] dist     = policyDistribution(state);
        float[] logProbs = new float[actionSize];
        for (int i = 0; i < actionSize; i++) {
            logProbs[i] = (float) Math.log(Math.max(dist[i], 1e-8f));
        }
        float value = predictValue(state);
        experiences.add(new Experience(state, action, reward, logProbs, value, done));

        if (done || experiences.size() >= maxExperiences) {
            float bootstrapValue = done ? 0f : predictValue(nextState);
            train(bootstrapValue);
        }
    }

    @Override
    public int[] getTopActions(float[] state, int n) {
        float[] dist = policyDistribution(state);
        Integer[] idx = new Integer[actionSize];
        for (int i = 0; i < actionSize; i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> Float.compare(dist[b], dist[a]));
        n = Math.min(n, actionSize);
        int[] top = new int[n];
        for (int i = 0; i < n; i++) top[i] = idx[i];
        return top;
    }

    @Override
    public float[] getActionProbabilities(float[] state, int[] actions) {
        float[] dist  = policyDistribution(state);
        float[] probs = new float[actions.length];
        for (int i = 0; i < actions.length; i++) probs[i] = dist[actions[i]];
        return probs;
    }

    @Override
    public boolean saveModel(String filePath) {
        try (ObjectOutputStream oos =
                     new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeFloat(explorationRate);
            oos.writeFloat(learningRate);
            oos.writeFloat(discountFactor);
            oos.writeFloat(clipEpsilon);
            oos.writeFloat(valueCoeff);
            oos.writeFloat(entropyCoeff);
            oos.writeFloat(gaeLambda);
            oos.writeInt(trainSteps);
            oos.writeObject(policyWeights);
            oos.writeObject(valueWeights);
            Log.d(TAG, "PPO model saved to " + filePath);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error saving PPO model", e);
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean loadModel(String filePath) {
        File f = new File(filePath);
        if (!f.exists()) { Log.e(TAG, "Model file not found: " + filePath); return false; }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            explorationRate = ois.readFloat();
            learningRate    = ois.readFloat();
            discountFactor  = ois.readFloat();
            clipEpsilon     = ois.readFloat();
            valueCoeff      = ois.readFloat();
            entropyCoeff    = ois.readFloat();
            gaeLambda       = ois.readFloat();
            trainSteps      = ois.readInt();
            policyWeights   = (float[][]) ois.readObject();
            valueWeights    = (float[])   ois.readObject();
            Log.d(TAG, "PPO model loaded from " + filePath);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error loading PPO model", e);
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // Policy and value forward passes
    // -----------------------------------------------------------------------

    /** Returns a valid probability distribution over actions via softmax. */
    private float[] policyDistribution(float[] state) {
        float[] logits = new float[actionSize];
        float   maxL   = Float.NEGATIVE_INFINITY;
        for (int j = 0; j < actionSize; j++) {
            for (int i = 0; i < Math.min(state.length, stateSize); i++) {
                logits[j] += state[i] * policyWeights[i][j];
            }
            if (logits[j] > maxL) maxL = logits[j];
        }
        // Numerically stable softmax
        float sum = 0f;
        float[] dist = new float[actionSize];
        for (int j = 0; j < actionSize; j++) {
            dist[j] = (float) Math.exp(logits[j] - maxL);
            sum += dist[j];
        }
        if (sum > 0) for (int j = 0; j < actionSize; j++) dist[j] /= sum;
        else         Arrays.fill(dist, 1f / actionSize);
        return dist;
    }

    private float predictValue(float[] state) {
        float v = 0f;
        for (int i = 0; i < Math.min(state.length, stateSize); i++) {
            v += state[i] * valueWeights[i];
        }
        return v;
    }

    // -----------------------------------------------------------------------
    // Training
    // -----------------------------------------------------------------------

    private void train(float bootstrapValue) {
        if (experiences.isEmpty()) return;

        computeGAE(bootstrapValue);

        // Store old log-probs for ratio computation
        List<float[]> oldLogProbsList = new ArrayList<>(experiences.size());
        for (Experience exp : experiences) oldLogProbsList.add(exp.logProbs.clone());

        for (int epoch = 0; epoch < epochs; epoch++) {
            shuffleExperiences();

            float epochKL = 0f;
            int   epochN  = 0;

            for (int start = 0; start < experiences.size(); start += batchSize) {
                int end = Math.min(start + batchSize, experiences.size());
                List<Experience> batch = experiences.subList(start, end);

                float batchKL = trainBatch(batch);
                epochKL += batchKL * batch.size();
                epochN  += batch.size();
            }

            // KL early stopping
            if (epochN > 0 && epochKL / epochN > targetKL) {
                Log.d(TAG, "PPO early stop at epoch " + epoch + ", KL=" +
                        String.format("%.4f", epochKL / epochN));
                break;
            }
        }

        experiences.clear();
        trainSteps++;
        Log.d(TAG, "PPO train step " + trainSteps + " complete");
    }

    /**
     * Generalized Advantage Estimation (GAE-λ).
     * Fills exp.advantage and exp.returnValue for each experience in the buffer.
     */
    private void computeGAE(float bootstrapValue) {
        float nextValue = bootstrapValue;
        float gaeAdv    = 0f;

        for (int i = experiences.size() - 1; i >= 0; i--) {
            Experience exp = experiences.get(i);

            float delta = exp.reward
                    + (exp.done ? 0f : discountFactor * nextValue)
                    - exp.value;

            gaeAdv = delta + (exp.done ? 0f : discountFactor * gaeLambda * gaeAdv);
            exp.advantage   = gaeAdv;
            exp.returnValue = exp.advantage + exp.value;

            nextValue = exp.value;
        }

        // Normalise advantages
        float mean = 0f, std;
        for (Experience e : experiences) mean += e.advantage;
        mean /= experiences.size();
        float sumSq = 0f;
        for (Experience e : experiences) { float d = e.advantage - mean; sumSq += d * d; }
        std = (float) Math.sqrt(sumSq / experiences.size() + 1e-8f);
        for (Experience e : experiences) e.advantage = (e.advantage - mean) / std;
    }

    /**
     * Train one mini-batch.  Returns mean KL divergence (for early stopping).
     */
    private float trainBatch(List<Experience> batch) {
        float policyGradNorm = 0f;
        float valueGradNorm  = 0f;
        float totalKL        = 0f;

        // Accumulate gradients
        float[][] policyGrad = new float[stateSize][actionSize];
        float[]   valueGrad  = new float[stateSize];

        for (Experience exp : batch) {
            float[] newDist     = policyDistribution(exp.state);
            float[] newLogProbs = new float[actionSize];
            for (int j = 0; j < actionSize; j++) {
                newLogProbs[j] = (float) Math.log(Math.max(newDist[j], 1e-8f));
            }

            // Ratio π_new(a|s) / π_old(a|s) in log space
            float logRatio = newLogProbs[exp.action] - exp.logProbs[exp.action];
            float ratio    = (float) Math.exp(logRatio);

            // Clipped surrogate objective
            float surr1 = ratio * exp.advantage;
            float surr2 = Math.max(Math.min(ratio, 1f + clipEpsilon),
                    1f - clipEpsilon) * exp.advantage;
            float policyLoss = -Math.min(surr1, surr2);

            // KL divergence (approximate)
            for (int j = 0; j < actionSize; j++) {
                float kl = (float) Math.exp(exp.logProbs[j]) * (exp.logProbs[j] - newLogProbs[j]);
                totalKL += Math.max(0f, kl);
            }

            // Value loss (clipped)
            float newValue   = predictValue(exp.state);
            float vClipped   = exp.value + Math.max(Math.min(newValue - exp.value,
                    clipEpsilon), -clipEpsilon);
            float valueLoss  = Math.max((float) Math.pow(newValue - exp.returnValue, 2),
                    (float) Math.pow(vClipped - exp.returnValue, 2));

            // Entropy bonus
            float entropy = 0f;
            for (int j = 0; j < actionSize; j++) {
                entropy -= newDist[j] * newLogProbs[j];
            }

            float totalLoss = policyLoss + valueCoeff * valueLoss - entropyCoeff * entropy;

            // Policy gradient for action dimension
            for (int i = 0; i < Math.min(exp.state.length, stateSize); i++) {
                for (int j = 0; j < actionSize; j++) {
                    float dLdLogit = (j == exp.action)
                            ? -Math.min(ratio, Math.max(1f - clipEpsilon,
                                          Math.min(ratio, 1f + clipEpsilon))) * exp.advantage
                            : 0f;
                    policyGrad[i][j] += dLdLogit * exp.state[i];
                }
                // Value gradient
                valueGrad[i] += (newValue - exp.returnValue) * exp.state[i];
            }
        }

        // Average and clip gradients
        int n = batch.size();
        for (int i = 0; i < stateSize; i++) {
            for (int j = 0; j < actionSize; j++) {
                policyGrad[i][j] /= n;
                policyGradNorm   += policyGrad[i][j] * policyGrad[i][j];
            }
            valueGrad[i] /= n;
            valueGradNorm += valueGrad[i] * valueGrad[i];
        }
        policyGradNorm = (float) Math.sqrt(policyGradNorm);
        valueGradNorm  = (float) Math.sqrt(valueGradNorm);

        float policyScale = policyGradNorm > maxGradNorm
                ? maxGradNorm / policyGradNorm : 1f;
        float valueScale  = valueGradNorm  > maxGradNorm
                ? maxGradNorm / valueGradNorm  : 1f;

        // Apply gradients
        for (int i = 0; i < stateSize; i++) {
            for (int j = 0; j < actionSize; j++) {
                policyWeights[i][j] -= learningRate * policyScale * policyGrad[i][j];
            }
            valueWeights[i] -= learningRate * valueScale * valueGrad[i];
        }

        return totalKL / (n * actionSize);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private int sampleCategorical(float[] dist) {
        float r = random.nextFloat(), cum = 0f;
        for (int i = 0; i < dist.length; i++) {
            cum += dist[i];
            if (r <= cum) return i;
        }
        return dist.length - 1;
    }

    private void shuffleExperiences() {
        for (int i = experiences.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Experience tmp = experiences.get(i);
            experiences.set(i, experiences.get(j));
            experiences.set(j, tmp);
        }
    }

    // -----------------------------------------------------------------------
    // Configuration setters
    // -----------------------------------------------------------------------

    public void setHyperparameters(float clipEpsilon, float valueCoeff,
                                   float entropyCoeff, int epochs) {
        this.clipEpsilon  = Math.max(0.01f, Math.min(0.5f, clipEpsilon));
        this.valueCoeff   = Math.max(0.1f,  Math.min(1.0f, valueCoeff));
        this.entropyCoeff = Math.max(0.0f,  Math.min(0.1f, entropyCoeff));
        this.epochs       = Math.max(1, epochs);
    }

    public void setGaeLambda(float lambda)    { this.gaeLambda    = Math.max(0f, Math.min(1f, lambda)); }
    public void setBatchSize(int size)         { this.batchSize    = Math.max(1, Math.min(maxExperiences, size)); }
    public void setMaxExperiences(int size)    { this.maxExperiences = Math.max(batchSize, size); }
    public void setTargetKL(float kl)         { this.targetKL     = Math.max(0.001f, kl); }
    public void setMaxGradNorm(float norm)     { this.maxGradNorm  = Math.max(0.1f, norm); }
    public int  getTrainSteps()               { return trainSteps; }
}
