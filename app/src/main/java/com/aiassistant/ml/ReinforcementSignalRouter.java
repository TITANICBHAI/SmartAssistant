package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ReinforcementSignalRouter — centralised reward routing and aggregation hub.
 *
 * Routes rewards from multiple sources to multiple learning modules.
 * Maintains a weighted reward pipeline with attribution tracking.
 *
 * Architecture:
 *   INPUT CHANNELS (reward sources):
 *     - Extrinsic:   raw environment reward.
 *     - Curiosity:   prediction error bonus (from IntrinsicRewardGenerator).
 *     - Shaping:     potential-based shaping (from RewardShapingEngine).
 *     - Social:      reward from multi-agent interaction.
 *     - Progress:    curriculum advancement bonus.
 *     - Safety:      constraint violation penalty.
 *
 *   OUTPUT SINKS (learning modules):
 *     - Main Q-network.
 *     - Auxiliary task heads.
 *     - Meta-learning signal.
 *     - Logging / telemetry.
 *
 *   ROUTING TABLE:
 *     source_weight[i][j] = weight of source i → sink j.
 *     Supports dynamic reweighting based on learning progress.
 *
 * Thread-safe.
 */
public class ReinforcementSignalRouter {

    private static final String TAG = "SignalRouter";

    // ─────────────────────────────────────────────────────────────────────────
    // Signal channels
    // ─────────────────────────────────────────────────────────────────────────
    public enum Source {
        EXTRINSIC, CURIOSITY, SHAPING, SOCIAL, PROGRESS, SAFETY
    }

    public enum Sink {
        MAIN_Q, AUXILIARY, META, LOGGING
    }

    private static final int NUM_SOURCES = Source.values().length;
    private static final int NUM_SINKS   = Sink.values().length;

    // ─────────────────────────────────────────────────────────────────────────
    // Routing table and channel state
    // ─────────────────────────────────────────────────────────────────────────
    private final float[][] routeWeights;   // [NUM_SOURCES][NUM_SINKS]
    private final float[]   sourceScale;    // per-source amplitude scaling
    private final float[]   sourceEma;      // running mean per source
    private final float[]   sinkOutput;     // aggregated reward per sink

    // Per-source normalizer
    private final double[] srcMean, srcM2;
    private final long[]   srcN;
    private float          clipValue = 10f;

    // Attribution tracking
    private final float[][] attribution;    // [NUM_SOURCES][NUM_SINKS] — cumulative contribution
    private final long      startTime;

    private final AtomicInteger stepCount  = new AtomicInteger(0);
    private float totalRoutedReward = 0f;

    // History for dynamic reweighting
    private final float[] learningProgress = new float[NUM_SOURCES]; // 0=no progress,1=max

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public ReinforcementSignalRouter() {
        routeWeights = new float[NUM_SOURCES][NUM_SINKS];
        sourceScale  = new float[NUM_SOURCES];
        sourceEma    = new float[NUM_SOURCES];
        sinkOutput   = new float[NUM_SINKS];
        attribution  = new float[NUM_SOURCES][NUM_SINKS];
        srcMean      = new double[NUM_SOURCES];
        srcM2        = new double[NUM_SOURCES];
        srcN         = new long[NUM_SOURCES];
        startTime    = System.currentTimeMillis();

        // Default routing: extrinsic → all sinks; others → MAIN_Q only
        setDefaultRouting();
        java.util.Arrays.fill(sourceScale, 1f);

        Log.i(TAG, "ReinforcementSignalRouter: sources=" + NUM_SOURCES + " sinks=" + NUM_SINKS);
    }

    private void setDefaultRouting() {
        // Extrinsic: full weight to MAIN_Q, half to META
        setRoute(Source.EXTRINSIC, Sink.MAIN_Q,   1.0f);
        setRoute(Source.EXTRINSIC, Sink.META,      0.5f);
        setRoute(Source.EXTRINSIC, Sink.LOGGING,   1.0f);
        // Curiosity: partial to MAIN_Q, full to AUXILIARY
        setRoute(Source.CURIOSITY, Sink.MAIN_Q,    0.1f);
        setRoute(Source.CURIOSITY, Sink.AUXILIARY, 1.0f);
        setRoute(Source.CURIOSITY, Sink.LOGGING,   1.0f);
        // Shaping: partial to MAIN_Q
        setRoute(Source.SHAPING,   Sink.MAIN_Q,    0.5f);
        setRoute(Source.SHAPING,   Sink.LOGGING,   1.0f);
        // Safety: penalty to MAIN_Q
        setRoute(Source.SAFETY,    Sink.MAIN_Q,    1.0f);
        setRoute(Source.SAFETY,    Sink.LOGGING,   1.0f);
        // Progress: to META and LOGGING
        setRoute(Source.PROGRESS,  Sink.META,      1.0f);
        setRoute(Source.PROGRESS,  Sink.LOGGING,   1.0f);
        // Social: MAIN_Q and AUXILIARY
        setRoute(Source.SOCIAL,    Sink.MAIN_Q,    0.3f);
        setRoute(Source.SOCIAL,    Sink.AUXILIARY, 0.7f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Route a batch of source signals to all sinks.
     * @param signals float[NUM_SOURCES] — reward value from each source.
     * @return float[NUM_SINKS] — aggregated reward for each sink.
     */
    public synchronized float[] route(float[] signals) {
        java.util.Arrays.fill(sinkOutput, 0f);

        for (int i=0;i<Math.min(signals.length, NUM_SOURCES);i++) {
            float raw = signals[i];
            updateSourceStats(i, raw);
            float normalised = normaliseSource(i, raw);
            float scaled     = Math.max(-clipValue, Math.min(clipValue,
                    normalised * sourceScale[i]));
            sourceEma[i] = 0.99f*sourceEma[i] + 0.01f*scaled;

            for (int j=0;j<NUM_SINKS;j++) {
                float contribution = routeWeights[i][j] * scaled;
                sinkOutput[j]  += contribution;
                attribution[i][j] += Math.abs(contribution);
            }
        }
        totalRoutedReward += sinkOutput[Sink.MAIN_Q.ordinal()];
        stepCount.incrementAndGet();
        return sinkOutput.clone();
    }

    /**
     * Convenience: route named source signals.
     */
    public synchronized float routeToMainQ(float extrinsic, float curiosity,
                                             float shaping, float safety) {
        float[] sigs = new float[NUM_SOURCES];
        sigs[Source.EXTRINSIC.ordinal()] = extrinsic;
        sigs[Source.CURIOSITY.ordinal()]  = curiosity;
        sigs[Source.SHAPING.ordinal()]    = shaping;
        sigs[Source.SAFETY.ordinal()]     = safety;
        return route(sigs)[Sink.MAIN_Q.ordinal()];
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Configuration
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized void setRoute(Source src, Sink sink, float weight) {
        routeWeights[src.ordinal()][sink.ordinal()] = weight;
    }

    public synchronized void setSourceScale(Source src, float scale) {
        sourceScale[src.ordinal()] = scale;
    }

    public synchronized void setClipValue(float clip) { clipValue = clip; }

    /**
     * Dynamic reweighting: reduce scale of sources that have converged
     * (low absolute signal), increase scale of sources driving progress.
     */
    public synchronized void adaptWeights() {
        float totalEma = 0;
        for (float e : sourceEma) totalEma += Math.abs(e);
        if (totalEma < 1e-6f) return;
        // Normalise source scales so total remains constant
        for (int i=0;i<NUM_SOURCES;i++) {
            float contrib = Math.abs(sourceEma[i]) / totalEma;
            sourceScale[i] = Math.max(0.01f, Math.min(10f,
                    sourceScale[i] * (1f + 0.01f * (learningProgress[i] - contrib))));
        }
    }

    public synchronized void updateLearningProgress(Source src, float progress) {
        learningProgress[src.ordinal()] = Math.max(0f, Math.min(1f, progress));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void updateSourceStats(int i, float v) {
        srcN[i]++;
        double d = v - srcMean[i];
        srcMean[i] += d / srcN[i];
        srcM2[i]   += d * (v - srcMean[i]);
    }

    private float normaliseSource(int i, float v) {
        double std = srcN[i] < 2 ? 1.0 : Math.sqrt(srcM2[i]/(srcN[i]-1)+1e-8);
        return (float)((v - srcMean[i]) / std);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Queries
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized float getSourceEma(Source src)  { return sourceEma[src.ordinal()]; }
    public synchronized float getSinkOutput(Sink sink)  { return sinkOutput[sink.ordinal()]; }

    /** Attribution: which source contributed most to MAIN_Q? */
    public synchronized Source dominantSource() {
        int mainQ = Sink.MAIN_Q.ordinal();
        int best=0;
        for(int i=1;i<NUM_SOURCES;i++) if(attribution[i][mainQ]>attribution[best][mainQ]) best=i;
        return Source.values()[best];
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("stepCount",         stepCount.get());
        s.put("totalRoutedReward", totalRoutedReward);
        s.put("dominantSource",    dominantSource().name());
        Source[] srcs = Source.values();
        Sink[]   snks = Sink.values();
        Map<String,Double> ema=new HashMap<>();
        for(Source src:srcs) ema.put(src.name(),(double)sourceEma[src.ordinal()]);
        s.put("sourceEma",    ema);
        Map<String,Double> out=new HashMap<>();
        for(Sink snk:snks) out.put(snk.name(),(double)sinkOutput[snk.ordinal()]);
        s.put("sinkOutput",   out);
        return s;
    }
}
