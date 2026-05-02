package com.aiassistant.ml;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ScreenRegionAnalyzer — partitions the screen into named zones and computes
 * per-zone pixel statistics that feed into downstream detection and decision systems.
 *
 * Built-in zone layout (normalized, based on typical mobile game HUDs):
 *   ┌───────────────────────────────────┐
 *   │          HUD_TOP  (0–12%)         │  health bars, score, minimap
 *   ├───────────────────────────────────┤
 *   │                                   │
 *   │       GAMEPLAY  (12–75%)          │  main play field
 *   │                                   │
 *   ├──────────────┬────────────────────┤
 *   │  HUD_BOTTOM_L│  HUD_BOTTOM_R      │  joystick / skill buttons (75–100%)
 *   └──────────────┴────────────────────┘
 *
 * Callers can also register custom zones with arbitrary screen-fraction bounds.
 *
 * Per-zone statistics computed:
 *   - Average R, G, B and brightness
 *   - Saturation (max−min channel / max channel)
 *   - Dominant hue bucket (16 buckets)
 *   - Activity score = mean absolute pixel difference from last frame
 *   - High-contrast pixel fraction (local variance proxy)
 *
 * Usage:
 *   ScreenRegionAnalyzer analyzer = new ScreenRegionAnalyzer();
 *   analyzer.initializeDefaultZones(screenWidth, screenHeight);
 *   Map<String, ZoneStats> stats = analyzer.analyze(currentFrame, previousFrame);
 */
public class ScreenRegionAnalyzer {

    private static final String TAG = "ScreenRegionAnalyzer";

    // -------------------------------------------------------------------------
    // Zone descriptor
    // -------------------------------------------------------------------------
    public static class Zone {
        public final String name;
        public final Rect   bounds; // pixel bounds in the input bitmap

        public Zone(String name, Rect bounds) {
            this.name   = name;
            this.bounds = bounds;
        }
    }

    // -------------------------------------------------------------------------
    // Per-zone stats
    // -------------------------------------------------------------------------
    public static class ZoneStats {
        public final String name;
        public final float  avgR, avgG, avgB;
        public final float  avgBrightness;
        public final float  avgSaturation;
        public final int    dominantHueBucket;   // 0–15 (22.5° each)
        public final float  activityScore;       // mean |current − previous| / 255
        public final float  contrastFraction;    // fraction of high-contrast pixels

        ZoneStats(String name, float r, float g, float b, float brightness,
                  float sat, int hue, float activity, float contrast) {
            this.name              = name;
            this.avgR              = r;
            this.avgG              = g;
            this.avgB              = b;
            this.avgBrightness     = brightness;
            this.avgSaturation     = sat;
            this.dominantHueBucket = hue;
            this.activityScore     = activity;
            this.contrastFraction  = contrast;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new HashMap<>();
            m.put("avgR",             avgR);
            m.put("avgG",             avgG);
            m.put("avgB",             avgB);
            m.put("avgBrightness",    avgBrightness);
            m.put("avgSaturation",    avgSaturation);
            m.put("dominantHueBucket", dominantHueBucket);
            m.put("activityScore",    activityScore);
            m.put("contrastFraction", contrastFraction);
            return m;
        }
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------
    private final List<Zone>             zones       = new ArrayList<>();
    private final Map<String, ZoneStats> lastStats   = new LinkedHashMap<>();
    private int                          sampleStep  = 4; // pixel sampling stride

    // -------------------------------------------------------------------------
    // Zone registration
    // -------------------------------------------------------------------------

    /**
     * Register the default HUD zones based on full screen dimensions.
     */
    public void initializeDefaultZones(int screenW, int screenH) {
        zones.clear();
        int hudH   = (int) (screenH * 0.12f);
        int skillH = (int) (screenH * 0.25f);
        int midW   = screenW / 2;

        zones.add(new Zone("HUD_TOP",       new Rect(0, 0, screenW, hudH)));
        zones.add(new Zone("GAMEPLAY",      new Rect(0, hudH, screenW, screenH - skillH)));
        zones.add(new Zone("HUD_BOTTOM_L",  new Rect(0, screenH - skillH, midW, screenH)));
        zones.add(new Zone("HUD_BOTTOM_R",  new Rect(midW, screenH - skillH, screenW, screenH)));
        zones.add(new Zone("FULL_SCREEN",   new Rect(0, 0, screenW, screenH)));
        Log.d(TAG, "Default zones initialized for " + screenW + "×" + screenH);
    }

    /** Add a custom zone defined by normalized fractions [0,1]. */
    public void addZone(String name, float leftFrac, float topFrac,
                        float rightFrac, float bottomFrac,
                        int screenW, int screenH) {
        zones.add(new Zone(name, new Rect(
                (int)(leftFrac   * screenW), (int)(topFrac    * screenH),
                (int)(rightFrac  * screenW), (int)(bottomFrac * screenH))));
    }

    public void clearZones() { zones.clear(); }

    public void setSampleStep(int step) { this.sampleStep = Math.max(1, step); }

    // -------------------------------------------------------------------------
    // Analysis
    // -------------------------------------------------------------------------

    /**
     * Analyze the current frame, optionally comparing against the previous frame
     * to produce an activity score.
     *
     * @param current  Current screen bitmap.
     * @param previous Previous frame (may be null — activity score will be 0).
     * @return Map of zone name → ZoneStats.
     */
    public Map<String, ZoneStats> analyze(Bitmap current, Bitmap previous) {
        Map<String, ZoneStats> result = new LinkedHashMap<>();
        if (current == null) return result;

        int imgW = current.getWidth();
        int imgH = current.getHeight();

        for (Zone zone : zones) {
            Rect b = clampRect(zone.bounds, imgW, imgH);
            if (b.width() <= 0 || b.height() <= 0) continue;

            ZoneStats stats = analyzeZone(zone.name, current, previous, b);
            result.put(zone.name, stats);
        }

        lastStats.clear();
        lastStats.putAll(result);
        return result;
    }

    /** Return the most recent stats without re-analyzing. */
    public Map<String, ZoneStats> getLastStats() {
        return new LinkedHashMap<>(lastStats);
    }

    // -------------------------------------------------------------------------
    // Per-zone computation
    // -------------------------------------------------------------------------

    private ZoneStats analyzeZone(String name, Bitmap cur, Bitmap prev, Rect b) {
        long sumR = 0, sumG = 0, sumB = 0;
        long sumActivity = 0;
        int  highContrastCount = 0;
        int  sampleCount = 0;
        int[] hueHistogram = new int[16];
        int   prevBrightness = -1;

        for (int y = b.top; y < b.bottom; y += sampleStep) {
            for (int x = b.left; x < b.right; x += sampleStep) {
                int px  = cur.getPixel(x, y);
                int r   = Color.red(px);
                int g   = Color.green(px);
                int bl  = Color.blue(px);

                sumR += r; sumG += g; sumB += bl;

                // Activity = |current − previous|
                if (prev != null) {
                    int ppx   = prev.getPixel(x, y);
                    int diff  = Math.abs(r - Color.red(ppx))
                              + Math.abs(g - Color.green(ppx))
                              + Math.abs(bl - Color.blue(ppx));
                    sumActivity += diff;
                }

                // Contrast: compare with pixel above
                if (y > b.top) {
                    int abovePx  = cur.getPixel(x, y - sampleStep);
                    int aboveBri = (Color.red(abovePx) + Color.green(abovePx) + Color.blue(abovePx)) / 3;
                    int curBri   = (r + g + bl) / 3;
                    if (Math.abs(curBri - aboveBri) > 50) highContrastCount++;
                }

                // Hue bucket (0–15)
                int maxC = Math.max(r, Math.max(g, bl));
                int minC = Math.min(r, Math.min(g, bl));
                if (maxC > 30) { // skip near-black pixels
                    float hue = computeHue(r, g, bl, maxC, minC);
                    int bucket = (int)(hue / 22.5f) % 16;
                    hueHistogram[bucket]++;
                }

                sampleCount++;
            }
        }

        if (sampleCount == 0) {
            return new ZoneStats(name, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        float avgR   = sumR   / (float) sampleCount;
        float avgG   = sumG   / (float) sampleCount;
        float avgB   = sumB   / (float) sampleCount;
        float avgBri = (avgR + avgG + avgB) / 3f / 255f;

        // Saturation = (max − min) / max
        float maxC = Math.max(avgR, Math.max(avgG, avgB));
        float minC = Math.min(avgR, Math.min(avgG, avgB));
        float sat  = maxC > 0 ? (maxC - minC) / maxC : 0f;

        // Dominant hue bucket
        int domHue = 0;
        for (int i = 1; i < 16; i++) if (hueHistogram[i] > hueHistogram[domHue]) domHue = i;

        float activity = prev != null ? (sumActivity / (float)(sampleCount * 3 * 255)) : 0f;
        float contrast = (float) highContrastCount / sampleCount;

        return new ZoneStats(name, avgR/255f, avgG/255f, avgB/255f,
                avgBri, sat, domHue, activity, contrast);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static float computeHue(int r, int g, int b, int max, int min) {
        if (max == min) return 0f;
        float range = max - min;
        float hue;
        if (max == r)      hue = (g - b) / range % 6f;
        else if (max == g) hue = (b - r) / range + 2f;
        else               hue = (r - g) / range + 4f;
        hue *= 60f;
        if (hue < 0) hue += 360f;
        return hue;
    }

    private static Rect clampRect(Rect r, int w, int h) {
        return new Rect(
            Math.max(0, r.left),
            Math.max(0, r.top),
            Math.min(w, r.right),
            Math.min(h, r.bottom));
    }

    public List<String> getZoneNames() {
        List<String> names = new ArrayList<>();
        for (Zone z : zones) names.add(z.name);
        return names;
    }
}
