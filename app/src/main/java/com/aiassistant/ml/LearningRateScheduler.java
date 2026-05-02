package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LearningRateScheduler — adaptive learning-rate schedules for RL training.
 *
 * Supported schedules:
 *
 *   CONSTANT          — lr = lr_init  (no change)
 *   LINEAR_DECAY      — lr = lr_init · max(lr_min/lr_init, 1 − step/maxSteps)
 *   EXPONENTIAL_DECAY — lr = lr_init · decay^step  (clamped to lr_min)
 *   COSINE_ANNEALING  — lr = lr_min + 0.5·(lr_init−lr_min)·(1+cos(π·step/T_max))
 *   WARM_UP_COSINE    — linear warm-up for warmupSteps, then cosine annealing
 *   CYCLIC_TRIANGULAR — triangular cycle between lr_min and lr_init
 *   REDUCE_ON_PLATEAU — reduce lr by factor when a monitored metric stops improving
 *
 * Usage:
 *   LearningRateScheduler sched = new LearningRateScheduler(Schedule.COSINE_ANNEALING,
 *                                     1e-3f, 1e-5f, 100_000, ...);
 *   float lr = sched.getLr();          // at each step
 *   sched.step();                       // advance the scheduler
 *   sched.reportMetric(val_loss);       // only for REDUCE_ON_PLATEAU
 */
public class LearningRateScheduler {

    private static final String TAG = "LRScheduler";

    public enum Schedule {
        CONSTANT, LINEAR_DECAY, EXPONENTIAL_DECAY,
        COSINE_ANNEALING, WARM_UP_COSINE,
        CYCLIC_TRIANGULAR, REDUCE_ON_PLATEAU
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final Schedule schedule;
    private final float    lrInit;
    private final float    lrMin;
    private final int      maxSteps;
    private final float    decayFactor;   // for EXP_DECAY and REDUCE_ON_PLATEAU
    private final int      warmupSteps;   // for WARM_UP_COSINE
    private final int      cycleSteps;    // for CYCLIC_TRIANGULAR

    // REDUCE_ON_PLATEAU
    private final int   patience;
    private final float plateauThresh;
    private float       bestMetric      = Float.MAX_VALUE;
    private int         noImproveCnt    = 0;

    private volatile float currentLr;
    private final AtomicInteger step = new AtomicInteger(0);

    private final List<Float> lrHistory = new ArrayList<>();
    private static final int  HIST_MAX  = 500;

    // ─────────────────────────────────────────────────────────────────────────
    // Construction helpers
    // ─────────────────────────────────────────────────────────────────────────

    public static LearningRateScheduler constant(float lr) {
        return new LearningRateScheduler(Schedule.CONSTANT, lr, lr, 1, 1f, 0, 1);
    }

    public static LearningRateScheduler linearDecay(float lrInit, float lrMin, int maxSteps) {
        return new LearningRateScheduler(Schedule.LINEAR_DECAY, lrInit, lrMin, maxSteps, 1f, 0, 1);
    }

    public static LearningRateScheduler expDecay(float lrInit, float lrMin, float decay, int maxSteps) {
        return new LearningRateScheduler(Schedule.EXPONENTIAL_DECAY, lrInit, lrMin, maxSteps, decay, 0, 1);
    }

    public static LearningRateScheduler cosineAnnealing(float lrInit, float lrMin, int tMax) {
        return new LearningRateScheduler(Schedule.COSINE_ANNEALING, lrInit, lrMin, tMax, 1f, 0, 1);
    }

    public static LearningRateScheduler warmUpCosine(float lrInit, float lrMin, int warmup, int total) {
        return new LearningRateScheduler(Schedule.WARM_UP_COSINE, lrInit, lrMin, total, 1f, warmup, 1);
    }

    public static LearningRateScheduler cyclicTriangular(float lrInit, float lrMin, int cycleSteps) {
        return new LearningRateScheduler(Schedule.CYCLIC_TRIANGULAR, lrInit, lrMin, cycleSteps * 100, 1f, 0, cycleSteps);
    }

    public static LearningRateScheduler reduceOnPlateau(float lrInit, float lrMin, float factor, int patience) {
        LearningRateScheduler s = new LearningRateScheduler(Schedule.REDUCE_ON_PLATEAU, lrInit, lrMin,
                Integer.MAX_VALUE, factor, 0, 1);
        s.noImproveCnt = 0;
        return s;
    }

    private LearningRateScheduler(Schedule schedule, float lrInit, float lrMin, int maxSteps,
                                   float decayFactor, int warmupSteps, int cycleSteps) {
        this.schedule    = schedule;
        this.lrInit      = lrInit;
        this.lrMin       = lrMin;
        this.maxSteps    = maxSteps;
        this.decayFactor = decayFactor;
        this.warmupSteps = warmupSteps;
        this.cycleSteps  = Math.max(1, cycleSteps);
        this.patience    = 10;
        this.plateauThresh = 1e-4f;
        this.currentLr   = lrInit;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /** Return the current learning rate. */
    public float getLr() { return currentLr; }

    /** Advance scheduler by one step and recompute LR. */
    public synchronized float step() {
        int t = step.incrementAndGet();
        float newLr = compute(t);
        currentLr   = Math.max(lrMin, newLr);
        if (lrHistory.size() >= HIST_MAX) lrHistory.remove(0);
        lrHistory.add(currentLr);
        return currentLr;
    }

    /**
     * For REDUCE_ON_PLATEAU: report the monitored metric value.
     * LR is reduced if no improvement is seen for `patience` calls.
     */
    public synchronized void reportMetric(float metricValue) {
        if (schedule != Schedule.REDUCE_ON_PLATEAU) return;
        if (metricValue < bestMetric * (1f - plateauThresh)) {
            bestMetric   = metricValue;
            noImproveCnt = 0;
        } else {
            noImproveCnt++;
            if (noImproveCnt >= patience) {
                float newLr = Math.max(lrMin, currentLr * decayFactor);
                Log.i(TAG, "Plateau — reducing LR " + currentLr + " → " + newLr);
                currentLr    = newLr;
                noImproveCnt = 0;
            }
        }
    }

    public int  getStep()     { return step.get(); }
    public void resetStep()   { step.set(0); currentLr = lrInit; }

    // ─────────────────────────────────────────────────────────────────────────
    // Schedule implementations
    // ─────────────────────────────────────────────────────────────────────────

    private float compute(int t) {
        switch (schedule) {
            case CONSTANT:
                return lrInit;

            case LINEAR_DECAY: {
                float frac = (float) t / maxSteps;
                return lrInit * Math.max(lrMin / lrInit, 1f - frac);
            }

            case EXPONENTIAL_DECAY:
                return (float)(lrInit * Math.pow(decayFactor, t));

            case COSINE_ANNEALING: {
                double cos = Math.cos(Math.PI * (t % maxSteps) / (double) maxSteps);
                return (float)(lrMin + 0.5 * (lrInit - lrMin) * (1.0 + cos));
            }

            case WARM_UP_COSINE: {
                if (t < warmupSteps) return lrInit * t / (float) Math.max(1, warmupSteps);
                int tAdj = t - warmupSteps;
                int tMax = Math.max(1, maxSteps - warmupSteps);
                double cos = Math.cos(Math.PI * tAdj / (double) tMax);
                return (float)(lrMin + 0.5 * (lrInit - lrMin) * (1.0 + cos));
            }

            case CYCLIC_TRIANGULAR: {
                int cycle  = t / cycleSteps;
                int pos    = t % cycleSteps;
                int half   = cycleSteps / 2;
                // Scale decreases with each cycle
                float scale = (float) Math.pow(0.99, cycle);
                float frac  = pos < half
                        ? (float) pos / half
                        : 1f - (float)(pos - half) / half;
                return lrMin + (lrInit - lrMin) * frac * scale;
            }

            case REDUCE_ON_PLATEAU:
                return currentLr; // controlled by reportMetric()

            default:
                return lrInit;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("schedule",    schedule.name());
        s.put("currentLr",   currentLr);
        s.put("step",        step.get());
        s.put("lrInit",      lrInit);
        s.put("lrMin",       lrMin);
        s.put("maxSteps",    maxSteps);
        if (schedule == Schedule.REDUCE_ON_PLATEAU) {
            s.put("bestMetric",   bestMetric);
            s.put("noImproveCnt", noImproveCnt);
            s.put("patience",     patience);
        }
        // Compact history: last 10 values
        int hn = lrHistory.size();
        s.put("recentLrs", new ArrayList<>(lrHistory.subList(Math.max(0, hn - 10), hn)));
        return s;
    }
}
