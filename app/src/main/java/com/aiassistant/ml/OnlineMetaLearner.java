package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * OnlineMetaLearner — MAML-inspired few-shot fast adaptation for on-device RL.
 *
 * Core idea (simplified MAML for production):
 *   1. Maintain a shared "meta" parameter set θ (the "meta-weights").
 *   2. For each game context (level, game-mode, enemy-type …) keep a local parameter
 *      set φ_c = θ − α · ∇L_c(θ) (one gradient step from meta).
 *   3. Local parameters φ_c improve rapidly on context-specific data via a fast
 *      inner-loop learning rate α_inner (typically 10× the outer rate).
 *   4. After K context steps, the outer-loop updates θ toward the average of all
 *      local gradients, so meta-parameters generalize better.
 *
 * Simplifications for Android (no autograd):
 *   - Parameters are 1-D float[] (can represent flattened Q-table rows or linear heads).
 *   - Gradient = (predicted − target) * feature (MSE loss, linear model).
 *   - Inner loop: 1 gradient step per incoming experience.
 *   - Outer loop: every OUTER_INTERVAL inner steps, average local gradients → update meta.
 *
 * Features:
 *   - Up to MAX_CONTEXTS separate context-parameter sets stored (LRU eviction).
 *   - Context switching triggers a parameter reset from meta (fast warm-start).
 *   - Thread-safe.
 *   - Exposes meta-parameters and per-context stats via getStats().
 */
public class OnlineMetaLearner {

    private static final String TAG = "OnlineMetaLearner";

    private static final int   MAX_CONTEXTS    = 20;
    private static final int   OUTER_INTERVAL  = 50;   // inner steps per outer update
    private static final float ALPHA_INNER     = 0.05f; // fast adaptation rate
    private static final float ALPHA_OUTER     = 0.005f; // slow meta update rate
    private static final float GRAD_CLIP       = 1.0f;

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final int paramDim;   // dimension of parameter vector

    /** Meta (global) parameters θ — the initialization shared across contexts. */
    private final float[] metaParams;

    /** Per-context local parameters φ_c. LRU-ordered. */
    private final LinkedHashMap<String, float[]> contextParams;

    /** Accumulated outer-loop gradient per context. */
    private final Map<String, float[]> outerGrads = new ConcurrentHashMap<>();

    private String currentContext = "default";
    private final AtomicInteger innerStep = new AtomicInteger(0);
    private final AtomicInteger outerStep = new AtomicInteger(0);

    // Per-context step counters and loss history
    private final Map<String, AtomicInteger> contextSteps   = new ConcurrentHashMap<>();
    private final Map<String, Double>        contextAvgLoss = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    public OnlineMetaLearner(int paramDim) {
        this.paramDim   = paramDim;
        this.metaParams = new float[paramDim];
        // Xavier initialization
        float scale = (float) Math.sqrt(2.0 / paramDim);
        java.util.Random rng = new java.util.Random(13L);
        for (int i = 0; i < paramDim; i++) {
            metaParams[i] = (rng.nextFloat() * 2f - 1f) * scale;
        }

        // LRU map: eldest entry evicted when > MAX_CONTEXTS
        this.contextParams = new LinkedHashMap<String, float[]>(
                MAX_CONTEXTS + 1, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, float[]> eldest) {
                return size() > MAX_CONTEXTS;
            }
        };

        // Initialize default context
        switchContext("default");
    }

    // -------------------------------------------------------------------------
    // Context management
    // -------------------------------------------------------------------------

    /**
     * Switch to a new context.  If the context has been seen before its local
     * parameters are restored; otherwise a fresh copy of meta-params is used.
     */
    public synchronized void switchContext(String context) {
        if (!contextParams.containsKey(context)) {
            // New context: warm-start from meta
            contextParams.put(context, Arrays.copyOf(metaParams, paramDim));
            outerGrads.put(context, new float[paramDim]);
            contextSteps.put(context, new AtomicInteger(0));
            contextAvgLoss.put(context, 0.0);
            Log.d(TAG, "New context: " + context + " (warm-started from meta)");
        }
        this.currentContext = context;
    }

    // -------------------------------------------------------------------------
    // Inner-loop update (fast adaptation)
    // -------------------------------------------------------------------------

    /**
     * Process one (feature, target) pair for the current context.
     *
     * @param features Input feature vector (length = paramDim).
     * @param target   Scalar regression target (e.g. TD target).
     * @return MSE loss before this update.
     */
    public synchronized float innerUpdate(float[] features, float target) {
        float[] phi = getCurrentParams();

        // Prediction: dot(phi, features)
        float pred = dot(phi, features);
        float err  = pred - target;
        float loss = err * err;

        // Gradient: dL/dφ = 2 * err * features
        float[] grad = new float[paramDim];
        int dim = Math.min(features.length, paramDim);
        for (int i = 0; i < dim; i++) {
            grad[i] = clip(2f * err * features[i]);
        }

        // Inner-loop SGD step
        for (int i = 0; i < paramDim; i++) {
            phi[i] -= ALPHA_INNER * grad[i];
        }

        // Accumulate gradient for outer loop
        float[] og = outerGrads.getOrDefault(currentContext, new float[paramDim]);
        for (int i = 0; i < paramDim; i++) og[i] += grad[i];
        outerGrads.put(currentContext, og);

        // Update context stats
        contextSteps.getOrDefault(currentContext, new AtomicInteger(0)).incrementAndGet();
        double prevLoss = contextAvgLoss.getOrDefault(currentContext, 0.0);
        contextAvgLoss.put(currentContext, prevLoss * 0.95 + loss * 0.05);

        // Outer-loop update every OUTER_INTERVAL inner steps
        int step = innerStep.incrementAndGet();
        if (step % OUTER_INTERVAL == 0) {
            outerUpdate();
        }

        return loss;
    }

    // -------------------------------------------------------------------------
    // Outer-loop update (meta-parameter update)
    // -------------------------------------------------------------------------

    private void outerUpdate() {
        // Average outer gradients across all active contexts
        float[] avgGrad = new float[paramDim];
        int count = 0;
        for (float[] og : outerGrads.values()) {
            for (int i = 0; i < paramDim; i++) avgGrad[i] += og[i];
            count++;
        }
        if (count == 0) return;
        for (int i = 0; i < paramDim; i++) avgGrad[i] /= count;

        // Update meta-parameters
        for (int i = 0; i < paramDim; i++) {
            metaParams[i] -= ALPHA_OUTER * clip(avgGrad[i]);
        }

        // Reset outer gradients
        for (String ctx : outerGrads.keySet()) {
            outerGrads.put(ctx, new float[paramDim]);
        }

        outerStep.incrementAndGet();
        if (outerStep.get() % 10 == 0) {
            Log.d(TAG, "Outer step " + outerStep.get()
                    + " active contexts=" + contextParams.size());
        }
    }

    // -------------------------------------------------------------------------
    // Inference
    // -------------------------------------------------------------------------

    /**
     * Predict using current context's local parameters.
     */
    public synchronized float predict(float[] features) {
        return dot(getCurrentParams(), features);
    }

    /**
     * Predict using meta-parameters (context-agnostic baseline).
     */
    public synchronized float predictMeta(float[] features) {
        return dot(metaParams, features);
    }

    // -------------------------------------------------------------------------
    // Monitoring
    // -------------------------------------------------------------------------

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("paramDim",        paramDim);
        s.put("innerStep",       innerStep.get());
        s.put("outerStep",       outerStep.get());
        s.put("currentContext",  currentContext);
        s.put("activeContexts",  contextParams.size());

        // Per-context summary
        List<Map<String, Object>> ctxList = new ArrayList<>();
        for (String ctx : contextSteps.keySet()) {
            Map<String, Object> cm = new HashMap<>();
            cm.put("context",  ctx);
            cm.put("steps",    contextSteps.get(ctx).get());
            cm.put("avgLoss",  contextAvgLoss.getOrDefault(ctx, 0.0));
            ctxList.add(cm);
        }
        s.put("contexts", ctxList);

        // Meta-param norm
        float norm = 0;
        for (float p : metaParams) norm += p * p;
        s.put("metaParamNorm", Math.sqrt(norm));

        return s;
    }

    public synchronized String getCurrentContext() { return currentContext; }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private float[] getCurrentParams() {
        return contextParams.computeIfAbsent(currentContext,
                k -> Arrays.copyOf(metaParams, paramDim));
    }

    private float dot(float[] a, float[] b) {
        float sum = 0;
        int   dim = Math.min(Math.min(a.length, b.length), paramDim);
        for (int i = 0; i < dim; i++) sum += a[i] * b[i];
        return sum;
    }

    private float clip(float v) {
        return Math.max(-GRAD_CLIP, Math.min(GRAD_CLIP, v));
    }
}
