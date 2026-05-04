package com.aiassistant.ml;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ScreenEventClassifier — lightweight on-device classifier for Android UI events.
 *
 * Classifies accessibility events and screen changes into semantic categories
 * to provide richer state representation for the RL agent.
 *
 * Event categories:
 *   - REWARD_SIGNAL:   score increase, achievement, positive feedback.
 *   - PENALTY_SIGNAL:  life lost, game over, negative feedback.
 *   - PROGRESS:        level advancement, stage completion.
 *   - UI_TRANSITION:   menu open, dialog appear, screen change.
 *   - GAME_ACTION:     enemy movement, power-up spawn, obstacle.
 *   - IDLE:            no significant event.
 *
 * Features extracted from screen state:
 *   - Text content hash (numeric approximation of visible text).
 *   - Element count delta.
 *   - Bounding box centroid shift.
 *   - Color histogram change (brightness delta proxy).
 *   - View hierarchy depth change.
 *
 * Classifier: softmax output from a 2-layer MLP trained online
 * via labeled transitions (reward +/-, manual labels).
 *
 * Thread-safe.
 */
public class ScreenEventClassifier {

    private static final String TAG = "ScreenEventClassifier";

    public enum EventType {
        REWARD_SIGNAL, PENALTY_SIGNAL, PROGRESS, UI_TRANSITION, GAME_ACTION, IDLE
    }

    private static final int NUM_CLASSES = EventType.values().length;

    // ─────────────────────────────────────────────────────────────────────────
    // Feature vector
    // ─────────────────────────────────────────────────────────────────────────
    public static class ScreenFeatures {
        public float textHashDelta;
        public float elementCountDelta;
        public float centroidShiftX, centroidShiftY;
        public float brightnessDelta;
        public float hierarchyDepthDelta;
        public float rewardSignalMagnitude;  // from RL reward tracker
        public float timeSinceLastEvent;

        public float[] toVector() {
            return new float[]{
                textHashDelta, elementCountDelta,
                centroidShiftX, centroidShiftY,
                brightnessDelta, hierarchyDepthDelta,
                rewardSignalMagnitude, timeSinceLastEvent
            };
        }
    }

    private static final int FEAT_DIM = 8;
    private static final int HID_DIM  = 32;

    // ─────────────────────────────────────────────────────────────────────────
    // Network
    // ─────────────────────────────────────────────────────────────────────────
    private final float[][] W1, W2;   // [HID_DIM][FEAT_DIM], [NUM_CLASSES][HID_DIM]
    private final float[]   b1, b2;
    private final NeuralNetworkOptimizer opt;

    // Class statistics
    private final int[]   classCounts = new int[NUM_CLASSES];
    private final float[] classWeights= new float[NUM_CLASSES];  // inverse frequency

    // Running feature normalisation
    private final double[] featMean = new double[FEAT_DIM];
    private final double[] featM2   = new double[FEAT_DIM];
    private long           featN    = 0;

    private final AtomicInteger classifyCount = new AtomicInteger(0);
    private final AtomicInteger trainCount    = new AtomicInteger(0);
    private float avgLoss = 0f;
    private float avgAcc  = 0f;

    private final Random rng = new Random(293L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public ScreenEventClassifier(float lr) {
        this.opt = new NeuralNetworkOptimizer(lr);
        float s = (float) Math.sqrt(2.0 / (FEAT_DIM + HID_DIM));
        W1 = xav(HID_DIM, FEAT_DIM, s); b1 = new float[HID_DIM];
        W2 = xav(NUM_CLASSES, HID_DIM, s * 0.1f); b2 = new float[NUM_CLASSES];
        java.util.Arrays.fill(classWeights, 1f);
        Log.i(TAG, "ScreenEventClassifier: classes=" + NUM_CLASSES);
    }

    public ScreenEventClassifier() { this(1e-3f); }

    // ─────────────────────────────────────────────────────────────────────────
    // Heuristic classification (rule-based, no training required)
    // ─────────────────────────────────────────────────────────────────────────

    /** Fast rule-based classification without neural network. */
    public EventType classifyHeuristic(ScreenFeatures f) {
        if (f.rewardSignalMagnitude > 0.5f) return EventType.REWARD_SIGNAL;
        if (f.rewardSignalMagnitude < -0.5f) return EventType.PENALTY_SIGNAL;
        if (Math.abs(f.brightnessDelta) > 0.3f || Math.abs(f.elementCountDelta) > 5)
            return EventType.UI_TRANSITION;
        if (Math.abs(f.centroidShiftX) + Math.abs(f.centroidShiftY) > 50f)
            return EventType.GAME_ACTION;
        if (f.textHashDelta != 0 && Math.abs(f.textHashDelta) > 100)
            return EventType.PROGRESS;
        return EventType.IDLE;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Neural classification
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized EventType classify(ScreenFeatures features) {
        float[] f    = normalise(features.toVector());
        float[] h    = lin(W1, b1, f, true);
        float[] logits = lin(W2, b2, h, false);
        float[] probs  = softmax(logits);
        int cls = argmax(probs);
        classifyCount.incrementAndGet();
        return EventType.values()[cls];
    }

    public synchronized float[] classProbs(ScreenFeatures features) {
        float[] f = normalise(features.toVector());
        return softmax(lin(W2, b2, lin(W1, b1, f, true), false));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Training
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized void train(ScreenFeatures features, EventType label) {
        int y = label.ordinal();
        classCounts[y]++;
        updateClassWeights();
        updateFeatureStats(features.toVector());

        float[] f = normalise(features.toVector());
        float[] h = lin(W1, b1, f, true);
        float[] logits = lin(W2, b2, h, false);
        float[] probs  = softmax(logits);
        float   loss   = -(float) Math.log(Math.max(probs[y], 1e-8f)) * classWeights[y];
        float   acc    = argmax(probs) == y ? 1f : 0f;

        float[] dL = probs.clone(); dL[y] -= 1f;
        for (int i=0;i<NUM_CLASSES;i++) dL[i] *= classWeights[y];
        float[][] dW2 = new float[NUM_CLASSES][HID_DIM];
        for (int i=0;i<NUM_CLASSES;i++) for(int j=0;j<HID_DIM;j++) dW2[i][j]=dL[i]*h[j];
        opt.step("sec_W2", W2, dW2);
        float[] dH = new float[HID_DIM];
        for(int j=0;j<HID_DIM;j++){if(h[j]<=0)continue;for(int i=0;i<NUM_CLASSES;i++)dH[j]+=dL[i]*W2[i][j];}
        float[][] dW1 = new float[HID_DIM][FEAT_DIM];
        for(int i=0;i<HID_DIM;i++) for(int j=0;j<FEAT_DIM;j++) dW1[i][j]=dH[i]*f[j];
        opt.step("sec_W1", W1, dW1);

        avgLoss = 0.99f * avgLoss + 0.01f * loss;
        avgAcc  = 0.99f * avgAcc  + 0.01f * acc;
        trainCount.incrementAndGet();
    }

    /** Auto-label from reward signal and train. */
    public synchronized void trainFromReward(ScreenFeatures features, float reward) {
        EventType label;
        if (reward > 0.5f)       label = EventType.REWARD_SIGNAL;
        else if (reward < -0.5f) label = EventType.PENALTY_SIGNAL;
        else                     label = classifyHeuristic(features);
        train(features, label);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private float[] normalise(float[] x) {
        featN++;
        float[] n = new float[FEAT_DIM];
        for (int i=0;i<FEAT_DIM;i++) {
            double d = x[i] - featMean[i];
            featMean[i] += d / featN;
            featM2[i]   += d * (x[i] - featMean[i]);
            double std = featN < 2 ? 1.0 : Math.sqrt(featM2[i]/(featN-1)+1e-8);
            n[i] = (float)((x[i] - featMean[i]) / std);
        }
        return n;
    }

    private void updateFeatureStats(float[] x) { /* stats updated in normalise */ }

    private void updateClassWeights() {
        long total = 0; for (int c : classCounts) total += c;
        if (total == 0) return;
        for (int i=0;i<NUM_CLASSES;i++)
            classWeights[i] = classCounts[i] > 0 ? (float)total/(NUM_CLASSES*classCounts[i]) : 1f;
    }

    private static float[] lin(float[][] W, float[] b, float[] x, boolean relu) {
        float[] o=new float[W.length];
        for(int i=0;i<W.length;i++){float s=b[i];for(int j=0;j<Math.min(x.length,W[i].length);j++)s+=W[i][j]*x[j];o[i]=relu?Math.max(0f,s):s;}
        return o;
    }

    private static float[] softmax(float[] v){float mx=v[0];for(float x:v)if(x>mx)mx=x;float sum=0;float[] o=new float[v.length];for(int i=0;i<v.length;i++){o[i]=(float)Math.exp(v[i]-mx);sum+=o[i];}for(int i=0;i<v.length;i++)o[i]/=sum;return o;}
    private static int argmax(float[] v){int b=0;for(int i=1;i<v.length;i++)if(v[i]>v[b])b=i;return b;}

    private float[][] xav(int r,int c,float s){float[][] m=new float[r][c];for(int i=0;i<r;i++) for(int j=0;j<c;j++) m[i][j]=(rng.nextFloat()*2f-1f)*s;return m;}

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("classifyCount", classifyCount.get());
        s.put("trainCount",    trainCount.get());
        s.put("avgLoss",       avgLoss);
        s.put("avgAcc",        avgAcc);
        Map<String, Integer> cc = new HashMap<>();
        EventType[] vals = EventType.values();
        for (int i=0;i<NUM_CLASSES;i++) cc.put(vals[i].name(), classCounts[i]);
        s.put("classCounts",   cc);
        return s;
    }
}
