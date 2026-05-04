package com.aiassistant.ml;

import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ActionMasking — invalid action filtering and safe exploration for RL agents.
 *
 * In many game contexts certain actions are invalid (e.g., moving into a wall,
 * using a skill on cooldown, buying when broke).  Without masking, the policy
 * wastes exploration budget on illegal moves and receives misleading feedback.
 *
 * This class provides:
 *
 *   HARD_MASK     — zero out logits / probabilities of invalid actions before
 *                   softmax; guaranteed to never select an invalid action.
 *
 *   SOFT_PENALTY  — add a large negative penalty (-1e9) to invalid action logits;
 *                   effectively the same as hard masking but differentiable.
 *
 *   REWARD_SHAPING— do not block invalid actions; instead apply a configurable
 *                   negative shaped reward when one is taken, letting the agent
 *                   learn naturally.
 *
 * Also tracks:
 *   - Masked action histogram: counts how many times each action was masked.
 *   - Invalid action rate: fraction of steps where the chosen action was invalid.
 *   - Dynamic masks: callers register a {@link MaskProvider} called each step.
 *
 * Thread-safe.
 */
public class ActionMasking {

    private static final String TAG = "ActionMasking";

    public enum Strategy { HARD_MASK, SOFT_PENALTY, REWARD_SHAPING }

    /** Functional interface: returns a boolean[] mask (true = valid) for the current state. */
    public interface MaskProvider {
        boolean[] getMask(float[] state);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int      actionDim;
    private Strategy       strategy;
    private float          invalidPenalty;   // for REWARD_SHAPING
    private float          softNegInf;       // for SOFT_PENALTY (default -1e9)

    private MaskProvider   maskProvider = null;
    private boolean[]      staticMask   = null; // fallback if no provider

    private final int[]    maskedCount;        // per-action mask count
    private final AtomicInteger totalSteps     = new AtomicInteger(0);
    private final AtomicInteger invalidChosen  = new AtomicInteger(0);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public ActionMasking(int actionDim, Strategy strategy,
                          float invalidPenalty, float softNegInf) {
        this.actionDim     = actionDim;
        this.strategy      = strategy;
        this.invalidPenalty= invalidPenalty;
        this.softNegInf    = softNegInf;
        this.maskedCount   = new int[actionDim];
        this.staticMask    = allValid();
        Log.i(TAG, "ActionMasking: dim=" + actionDim + " strategy=" + strategy);
    }

    public ActionMasking(int actionDim) {
        this(actionDim, Strategy.HARD_MASK, -1.0f, -1e9f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Configuration
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized void setMaskProvider(MaskProvider provider) {
        this.maskProvider = provider;
    }

    /** Set a fixed mask: true = valid, false = invalid. */
    public synchronized void setStaticMask(boolean[] mask) {
        this.staticMask = Arrays.copyOf(mask, actionDim);
    }

    /** Re-enable all actions. */
    public synchronized void clearMask() {
        this.staticMask = allValid();
        this.maskProvider = null;
    }

    public synchronized void setStrategy(Strategy s) { this.strategy = s; }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Apply masking to raw logits before softmax.
     * Returns a new float[] with invalid logits set to softNegInf (HARD/SOFT)
     * or unchanged (REWARD_SHAPING).
     */
    public synchronized float[] applyToLogits(float[] logits, float[] state) {
        boolean[] mask = getMask(state);
        float[] out = logits.clone();

        if (strategy == Strategy.REWARD_SHAPING) return out; // no logit change

        for (int a = 0; a < Math.min(actionDim, out.length); a++) {
            if (!mask[a]) {
                out[a] = softNegInf;
                maskedCount[a]++;
            }
        }
        return out;
    }

    /**
     * Apply masking to a probability distribution (post-softmax).
     * Zeroes invalid probabilities and renormalises.
     */
    public synchronized float[] applyToProbs(float[] probs, float[] state) {
        boolean[] mask = getMask(state);
        float[] out = probs.clone();

        if (strategy == Strategy.REWARD_SHAPING) return out;

        float sum = 0f;
        for (int a = 0; a < Math.min(actionDim, out.length); a++) {
            if (!mask[a]) { out[a] = 0f; maskedCount[a]++; }
            else sum += out[a];
        }
        // Renormalise
        if (sum > 1e-8f) for (int a = 0; a < out.length; a++) out[a] /= sum;
        return out;
    }

    /**
     * Check if the chosen action is valid.
     * For REWARD_SHAPING: returns the penalty to add to reward (0 or invalidPenalty).
     * For HARD/SOFT: returns 0 (action was guaranteed valid by masking).
     */
    public synchronized float checkAction(int action, float[] state) {
        totalSteps.incrementAndGet();
        boolean[] mask = getMask(state);
        boolean valid  = action >= 0 && action < actionDim && mask[action];
        if (!valid) {
            invalidChosen.incrementAndGet();
            return strategy == Strategy.REWARD_SHAPING ? invalidPenalty : 0f;
        }
        return 0f;
    }

    /**
     * Remap a chosen action to the nearest valid alternative if invalid.
     * Useful as a safety fallback when hard-masking was bypassed.
     */
    public synchronized int remapIfInvalid(int action, float[] state) {
        boolean[] mask = getMask(state);
        if (action >= 0 && action < actionDim && mask[action]) return action;
        // Find nearest valid action (wrap-around search)
        for (int delta = 1; delta < actionDim; delta++) {
            int candidate = (action + delta) % actionDim;
            if (mask[candidate]) return candidate;
        }
        return action; // no valid action found (shouldn't happen)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized int[] getMaskedCounts() { return maskedCount.clone(); }

    public float getInvalidRate() {
        int t = totalSteps.get();
        return t > 0 ? (float) invalidChosen.get() / t : 0f;
    }

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("strategy",      strategy.name());
        s.put("actionDim",     actionDim);
        s.put("totalSteps",    totalSteps.get());
        s.put("invalidChosen", invalidChosen.get());
        s.put("invalidRate",   getInvalidRate());
        s.put("maskedCounts",  maskedCount.clone());
        int totalMasked = 0;
        for (int c : maskedCount) totalMasked += c;
        s.put("totalMasked",   totalMasked);
        return s;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private boolean[] getMask(float[] state) {
        if (maskProvider != null) {
            boolean[] m = maskProvider.getMask(state);
            return m != null ? m : allValid();
        }
        return staticMask != null ? staticMask : allValid();
    }

    private boolean[] allValid() {
        boolean[] m = new boolean[actionDim];
        Arrays.fill(m, true);
        return m;
    }
}
