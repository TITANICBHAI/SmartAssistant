package com.aiassistant.utils;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Efficient sliding-window statistics with O(1) amortised update time.
 *
 * Maintained statistics:
 *  • Mean          — running mean via Welford-style update over the window
 *  • Variance/StdDev — running variance using the corrected sum-of-squares trick
 *  • Min / Max     — monotonic deque (O(1) amortised per update)
 *  • Count         — total number of observations added (not just window size)
 *  • Sum           — sum of values in the current window
 *  • EMA           — exponential moving average with configurable alpha
 *
 * Usage:
 * <pre>
 *     SlidingWindowStats stats = new SlidingWindowStats(100);
 *     stats.add(value);
 *     double mean = stats.mean();
 *     double std  = stats.stdDev();
 *     boolean outlier = Math.abs(value - mean) > 3 * std;
 * </pre>
 *
 * Thread-safety: NOT thread-safe.  Wrap with external synchronisation if
 * used from multiple threads.
 */
public class SlidingWindowStats {

    private final int       capacity;
    private final double[]  window;
    private int             head     = 0;   // index of oldest element
    private int             size     = 0;   // current number of elements
    private double          sum      = 0;
    private double          sumSq    = 0;   // Σ(x²) for variance

    // Min-deque and max-deque (store indices into window[])
    private final Deque<Integer> minDeque = new ArrayDeque<>();
    private final Deque<Integer> maxDeque = new ArrayDeque<>();

    // EMA state
    private double ema          = Double.NaN;
    private double emaAlpha     = 0.1;

    // Global count (including evicted values)
    private long totalCount = 0;

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    public SlidingWindowStats(int windowSize) {
        if (windowSize < 1) throw new IllegalArgumentException("windowSize must be >= 1");
        this.capacity = windowSize;
        this.window   = new double[windowSize];
    }

    public SlidingWindowStats(int windowSize, double emaAlpha) {
        this(windowSize);
        setEmaAlpha(emaAlpha);
    }

    // -----------------------------------------------------------------------
    // Update
    // -----------------------------------------------------------------------

    /** Adds a new value, evicting the oldest if the window is full. */
    public void add(double value) {
        totalCount++;

        if (size == capacity) {
            // Evict the oldest value
            double evicted = window[head];
            sum   -= evicted;
            sumSq -= evicted * evicted;

            // Fix deques (remove evicted index if it's at the front)
            if (!minDeque.isEmpty() && minDeque.peekFirst() == head) minDeque.pollFirst();
            if (!maxDeque.isEmpty() && maxDeque.peekFirst() == head) maxDeque.pollFirst();

            // Overwrite slot
            window[head] = value;
            head = (head + 1) % capacity;
        } else {
            // Window not full yet
            int tail = (head + size) % capacity;
            window[tail] = value;
            size++;
        }

        sum   += value;
        sumSq += value * value;

        // Update min-deque
        int tail = (head + size - 1) % capacity;
        while (!minDeque.isEmpty() && window[minDeque.peekLast()] >= value) minDeque.pollLast();
        minDeque.addLast(tail);

        // Update max-deque
        while (!maxDeque.isEmpty() && window[maxDeque.peekLast()] <= value) maxDeque.pollLast();
        maxDeque.addLast(tail);

        // Update EMA
        if (Double.isNaN(ema)) ema = value;
        else                   ema = emaAlpha * value + (1 - emaAlpha) * ema;
    }

    // -----------------------------------------------------------------------
    // Computed statistics
    // -----------------------------------------------------------------------

    public double mean() {
        return size > 0 ? sum / size : 0.0;
    }

    /** Population variance of the current window. */
    public double variance() {
        if (size < 2) return 0.0;
        double m = mean();
        // Var = E[x²] - (E[x])²
        double v = (sumSq / size) - m * m;
        return Math.max(0.0, v); // guard against floating-point negatives
    }

    /** Population standard deviation. */
    public double stdDev() { return Math.sqrt(variance()); }

    /** Sample variance (Bessel-corrected, n-1 denominator). */
    public double sampleVariance() {
        if (size < 2) return 0.0;
        return variance() * size / (size - 1);
    }

    public double sampleStdDev() { return Math.sqrt(sampleVariance()); }

    /** Minimum value in the current window. */
    public double min() {
        if (size == 0) return Double.NaN;
        return window[minDeque.peekFirst()];
    }

    /** Maximum value in the current window. */
    public double max() {
        if (size == 0) return Double.NaN;
        return window[maxDeque.peekFirst()];
    }

    public double sum()           { return sum; }
    public double ema()           { return ema; }
    public int    windowSize()    { return size; }
    public int    capacity()      { return capacity; }
    public long   totalCount()    { return totalCount; }
    public boolean isFull()       { return size == capacity; }
    public boolean isEmpty()      { return size == 0; }

    /** Z-score of the given value relative to the current window. */
    public double zScore(double value) {
        double sd = stdDev();
        return sd > 1e-12 ? (value - mean()) / sd : 0.0;
    }

    /** Returns true if the value's z-score exceeds the threshold. */
    public boolean isOutlier(double value, double zThreshold) {
        return Math.abs(zScore(value)) > zThreshold;
    }

    // -----------------------------------------------------------------------
    // Configuration
    // -----------------------------------------------------------------------

    public void setEmaAlpha(double alpha) {
        if (alpha <= 0 || alpha > 1) throw new IllegalArgumentException("alpha must be in (0,1]");
        this.emaAlpha = alpha;
    }

    /** Resets all state. */
    public void reset() {
        head = 0; size = 0; sum = 0; sumSq = 0; totalCount = 0; ema = Double.NaN;
        minDeque.clear(); maxDeque.clear();
    }

    // -----------------------------------------------------------------------
    // String representation
    // -----------------------------------------------------------------------

    @Override
    public String toString() {
        if (size == 0) return "SlidingWindowStats{empty}";
        return "SlidingWindowStats{n=" + size
             + " mean=" + String.format("%.4f", mean())
             + " std=" + String.format("%.4f", stdDev())
             + " min=" + String.format("%.4f", min())
             + " max=" + String.format("%.4f", max())
             + " ema=" + String.format("%.4f", ema())
             + "}";
    }
}
