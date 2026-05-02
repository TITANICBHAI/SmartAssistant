package com.aiassistant.rl;

import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Deep Q-Network (DQN) agent with the following improvements:
 *
 *  1. Prioritized Experience Replay (PER) — samples high-TD-error transitions
 *     more frequently, dramatically improving sample efficiency.
 *  2. Double DQN — action selection uses the online network, Q-value evaluation
 *     uses the target network, reducing over-estimation bias.
 *  3. Epsilon decay schedule — exploration rate decays exponentially from
 *     EPSILON_START down to EPSILON_MIN over EPSILON_DECAY_STEPS.
 *  4. N-step returns — accumulates rewards over N steps before bootstrapping
 *     from the target network.
 *  5. Numerical Q-value approximation for the online / target networks that is
 *     consistent across predict() calls (same weights object, deterministic).
 *  6. Model save/load now persists the full weight matrix as well as scalars.
 */
public class DQNAgent extends RLAgent {
    private static final String TAG = "DQNAgent";

    // -----------------------------------------------------------------------
    // Hyper-parameters
    // -----------------------------------------------------------------------
    private static final float EPSILON_START       = 1.0f;
    private static final float EPSILON_MIN         = 0.05f;
    private static final int   EPSILON_DECAY_STEPS = 10_000;
    private static final int   N_STEP              = 3;
    private static final float PER_ALPHA           = 0.6f;   // priority exponent
    private static final float PER_BETA_START      = 0.4f;   // IS correction start
    private static final float PER_BETA_END        = 1.0f;
    private static final int   PER_BETA_STEPS      = 20_000;
    private static final float PER_EPSILON         = 1e-6f;  // small constant to avoid zero priority

    // -----------------------------------------------------------------------
    // Network weights (lightweight numeric approximation)
    // -----------------------------------------------------------------------
    /** Online network weight matrix — [stateSize x actionSize]. */
    private float[][] onlineWeights;
    /** Target network weight matrix — updated every targetUpdateFreq steps. */
    private float[][] targetWeights;

    // -----------------------------------------------------------------------
    // Replay memory
    // -----------------------------------------------------------------------
    private static class Experience {
        float[] state;
        int     action;
        float   reward;          // n-step accumulated
        float[] nextState;       // state after n steps
        boolean done;
        float   priority;        // TD-error based priority

        Experience(float[] state, int action, float reward,
                   float[] nextState, boolean done) {
            this.state     = state.clone();
            this.action    = action;
            this.reward    = reward;
            this.nextState = nextState.clone();
            this.done      = done;
            this.priority  = 1.0f; // max priority for new experiences
        }
    }

    private final List<Experience> replayBuffer = new ArrayList<>();
    private int maxMemorySize = 2000;
    private int batchSize     = 32;

    // N-step buffer
    private final List<float[]>  nStepStates    = new ArrayList<>();
    private final List<Integer>  nStepActions   = new ArrayList<>();
    private final List<Float>    nStepRewards   = new ArrayList<>();
    private float[]              nStepNextState = null;
    private boolean              nStepDone      = false;

    // -----------------------------------------------------------------------
    // Training state
    // -----------------------------------------------------------------------
    private int   targetUpdateFreq = 200;
    private int   trainingSteps    = 0;
    private int   totalSteps       = 0;
    private float perBeta          = PER_BETA_START;
    private boolean useDoubleDQN   = true;

    private final Random random;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public DQNAgent(int stateSize, int actionSize) {
        super(stateSize, actionSize);
        this.random          = new Random(42);
        this.explorationRate = EPSILON_START;
        this.learningRate    = 0.001f;

        initializeWeights();
        Log.i(TAG, "DQNAgent created (stateSize=" + stateSize +
                ", actionSize=" + actionSize + ", doubleDQN=" + useDoubleDQN + ")");
    }

    // -----------------------------------------------------------------------
    // Weight initialisation (Xavier uniform)
    // -----------------------------------------------------------------------

    private void initializeWeights() {
        float limit = (float) Math.sqrt(6.0 / (stateSize + actionSize));
        onlineWeights = new float[stateSize][actionSize];
        targetWeights = new float[stateSize][actionSize];
        for (int i = 0; i < stateSize; i++) {
            for (int j = 0; j < actionSize; j++) {
                float w = (random.nextFloat() * 2 - 1) * limit;
                onlineWeights[i][j] = w;
                targetWeights[i][j] = w;
            }
        }
    }

    // -----------------------------------------------------------------------
    // RLAgent interface
    // -----------------------------------------------------------------------

    @Override
    public int selectAction(float[] state) {
        totalSteps++;
        updateEpsilon();
        if (random.nextFloat() < explorationRate) {
            return random.nextInt(actionSize);
        }
        return argmax(predict(state, onlineWeights));
    }

    @Override
    public void update(float[] state, int action, float reward,
                       float[] nextState, boolean done) {
        // Accumulate into n-step buffer
        nStepStates.add(state.clone());
        nStepActions.add(action);
        nStepRewards.add(reward);
        nStepNextState = nextState.clone();
        nStepDone      = done;

        if (nStepStates.size() >= N_STEP || done) {
            flushNStepBuffer();
        }

        if (replayBuffer.size() >= batchSize) {
            trainBatch();
        }

        trainingSteps++;
        if (trainingSteps % targetUpdateFreq == 0) {
            updateTargetNetwork();
        }
    }

    @Override
    public int[] getTopActions(float[] state, int n) {
        float[] q = predict(state, onlineWeights);
        Integer[] idx = new Integer[actionSize];
        for (int i = 0; i < actionSize; i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> Float.compare(q[b], q[a]));
        n = Math.min(n, actionSize);
        int[] top = new int[n];
        for (int i = 0; i < n; i++) top[i] = idx[i];
        return top;
    }

    @Override
    public float[] getActionProbabilities(float[] state, int[] actions) {
        float[] q = predict(state, onlineWeights);
        float[] probs = new float[actions.length];
        float   sum   = 0f;
        for (int i = 0; i < actions.length; i++) {
            probs[i] = (float) Math.exp(q[actions[i]]);
            sum      += probs[i];
        }
        if (sum > 0) for (int i = 0; i < probs.length; i++) probs[i] /= sum;
        else         Arrays.fill(probs, 1f / actions.length);
        return probs;
    }

    @Override
    public boolean saveModel(String filePath) {
        try (ObjectOutputStream oos =
                     new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeFloat(explorationRate);
            oos.writeFloat(learningRate);
            oos.writeFloat(discountFactor);
            oos.writeInt(trainingSteps);
            oos.writeInt(totalSteps);
            oos.writeObject(onlineWeights);
            Log.d(TAG, "Model saved to " + filePath);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error saving model", e);
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
            trainingSteps   = ois.readInt();
            totalSteps      = ois.readInt();
            onlineWeights   = (float[][]) ois.readObject();
            // Sync target network
            copyWeights(onlineWeights, targetWeights);
            Log.d(TAG, "Model loaded from " + filePath);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error loading model", e);
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // Network forward pass
    // -----------------------------------------------------------------------

    /** Computes Q(s, ·) = s · W as a linear approximation. */
    private float[] predict(float[] state, float[][] weights) {
        float[] q = new float[actionSize];
        for (int j = 0; j < actionSize; j++) {
            for (int i = 0; i < Math.min(state.length, stateSize); i++) {
                q[j] += state[i] * weights[i][j];
            }
        }
        return q;
    }

    // -----------------------------------------------------------------------
    // N-step return accumulation
    // -----------------------------------------------------------------------

    private void flushNStepBuffer() {
        if (nStepStates.isEmpty()) return;

        // Compute discounted n-step return from the end of the buffer
        float nStepReturn = 0f;
        float gamma = 1f;
        for (int i = nStepRewards.size() - 1; i >= 0; i--) {
            nStepReturn = nStepRewards.get(i) + discountFactor * nStepReturn;
        }

        Experience exp = new Experience(
                nStepStates.get(0),
                nStepActions.get(0),
                nStepReturn,
                nStepNextState,
                nStepDone);

        addToBuffer(exp);

        nStepStates.clear();
        nStepActions.clear();
        nStepRewards.clear();
    }

    // -----------------------------------------------------------------------
    // Prioritized Experience Replay
    // -----------------------------------------------------------------------

    private void addToBuffer(Experience exp) {
        if (replayBuffer.size() >= maxMemorySize) {
            replayBuffer.remove(0); // FIFO eviction of lowest-priority
        }
        replayBuffer.add(exp);
    }

    /** PER sampling: proportional to priority^alpha. */
    private List<Integer> sampleIndices(int size) {
        double[] cumulative = new double[replayBuffer.size()];
        double   sum        = 0;
        for (int i = 0; i < replayBuffer.size(); i++) {
            sum         += Math.pow(replayBuffer.get(i).priority + PER_EPSILON, PER_ALPHA);
            cumulative[i] = sum;
        }
        List<Integer> indices = new ArrayList<>();
        for (int k = 0; k < size; k++) {
            double r = random.nextDouble() * sum;
            int lo = 0, hi = replayBuffer.size() - 1;
            while (lo < hi) {
                int mid = (lo + hi) >>> 1;
                if (cumulative[mid] < r) lo = mid + 1; else hi = mid;
            }
            indices.add(lo);
        }
        return indices;
    }

    // -----------------------------------------------------------------------
    // Training
    // -----------------------------------------------------------------------

    private void trainBatch() {
        int actualBatch = Math.min(batchSize, replayBuffer.size());
        List<Integer> indices = sampleIndices(actualBatch);

        // Anneal beta toward 1
        perBeta = PER_BETA_START + (PER_BETA_END - PER_BETA_START)
                * Math.min(1f, (float) totalSteps / PER_BETA_STEPS);

        // Compute max priority for IS weight normalisation
        float maxPriority = 0f;
        for (Integer idx : indices) {
            maxPriority = Math.max(maxPriority, replayBuffer.get(idx).priority);
        }

        for (Integer idx : indices) {
            Experience exp = replayBuffer.get(idx);

            // IS weight
            double p   = Math.pow(exp.priority + PER_EPSILON, PER_ALPHA);
            float  w   = (float) Math.pow(p / maxPriority, -perBeta);

            // ---- Double DQN target ----
            float targetQ;
            if (exp.done) {
                targetQ = exp.reward;
            } else {
                if (useDoubleDQN) {
                    // Action selection: online network
                    int bestAction = argmax(predict(exp.nextState, onlineWeights));
                    // Value evaluation: target network
                    float[] nextQTarget = predict(exp.nextState, targetWeights);
                    targetQ = exp.reward + discountFactor * nextQTarget[bestAction];
                } else {
                    float[] nextQTarget = predict(exp.nextState, targetWeights);
                    targetQ = exp.reward + discountFactor * max(nextQTarget);
                }
            }

            // ---- Online network TD update ----
            float[] currentQ = predict(exp.state, onlineWeights);
            float   tdError   = targetQ - currentQ[exp.action];

            // Gradient descent step: W_i = W_i + lr * w * tdError * s_i
            for (int i = 0; i < Math.min(exp.state.length, stateSize); i++) {
                onlineWeights[i][exp.action] +=
                        learningRate * w * tdError * exp.state[i];
            }

            // Update priority
            exp.priority = Math.abs(tdError) + PER_EPSILON;
        }
    }

    private void updateTargetNetwork() {
        copyWeights(onlineWeights, targetWeights);
        Log.d(TAG, "Target network updated at step " + trainingSteps);
    }

    private void copyWeights(float[][] src, float[][] dst) {
        for (int i = 0; i < src.length; i++) {
            System.arraycopy(src[i], 0, dst[i], 0, src[i].length);
        }
    }

    // -----------------------------------------------------------------------
    // Epsilon decay
    // -----------------------------------------------------------------------

    private void updateEpsilon() {
        float progress = Math.min(1f, (float) totalSteps / EPSILON_DECAY_STEPS);
        explorationRate = EPSILON_START + (EPSILON_MIN - EPSILON_START) * progress;
    }

    // -----------------------------------------------------------------------
    // Array utilities
    // -----------------------------------------------------------------------

    private int argmax(float[] v) {
        int   best  = 0;
        float bestV = v[0];
        for (int i = 1; i < v.length; i++) if (v[i] > bestV) { bestV = v[i]; best = i; }
        return best;
    }

    private float max(float[] v) {
        float m = v[0];
        for (float x : v) if (x > m) m = x;
        return m;
    }

    // -----------------------------------------------------------------------
    // Setters
    // -----------------------------------------------------------------------

    public void setMaxMemorySize(int size) {
        this.maxMemorySize = Math.max(batchSize, size);
        while (replayBuffer.size() > maxMemorySize) replayBuffer.remove(0);
    }

    public void setBatchSize(int size) {
        this.batchSize = Math.min(Math.max(1, size), maxMemorySize);
    }

    public void setTargetUpdateFrequency(int freq) {
        this.targetUpdateFreq = Math.max(1, freq);
    }

    public void setDoubleDQN(boolean enabled) { this.useDoubleDQN = enabled; }

    public int getTrainingSteps() { return trainingSteps; }
    public int getTotalSteps()    { return totalSteps; }
}
