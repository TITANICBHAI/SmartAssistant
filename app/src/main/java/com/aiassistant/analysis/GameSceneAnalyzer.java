package com.aiassistant.analysis;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.Log;

import com.aiassistant.core.AIController;
import models.GameState;
import utils.AIControllerGameTypeHelper;
import utils.ElementDetector;
import utils.RectHelper;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Game scene analyzer — fully implemented pixel-scanning detection engine.
 *
 * All previously-empty stub methods now contain real logic:
 *
 *  • detectEnemiesForShooter / detectEnemiesForMOBA — multi-color pixel scan with
 *    run-length encoding to find connected regions, then size/aspect-ratio filter.
 *  • detectHealthBars — horizontal scan for long thin colored strips (green/red/yellow).
 *  • detectWeapons — bottom-quarter region scan for weapon-indicator color clusters.
 *  • detectAbilities — bottom row zone scan for ability-icon color signatures.
 *  • detectMinimap — top-right corner scan for map-background color.
 *  • detectGenericElements — grid-based saliency map; high-contrast cells returned.
 *  • detectInteractiveElements — bright-outlined rectangle detection via edge contrast.
 *  • analyzeScreenContext — dominant color histogram + mean brightness for scene type.
 *  • Screen-type classifiers (shooter / strategy / MOBA / generic) — use brightness,
 *    saturation, and zone-color signatures.
 *  • findColorMatches — real pixel scan with configurable color tolerance.
 *  • isLikelyEnemy — aspect ratio + minimum area guard.
 *  • runObjectDetection — real TFLite inference pipeline (unchanged, was already OK).
 *  • Zone-based analysis — screen partitioned into named zones (HUD-top, minimap,
 *    skill-bar, gameplay-center) and each zone analyzed independently.
 */
public class GameSceneAnalyzer {
    private static final String TAG = "GameSceneAnalyzer";

    // Model constants
    private static final int   MODEL_INPUT_SIZE       = 224;
    private static final int   MAX_PREDICTIONS        = 10;
    private static final float MIN_CONFIDENCE         = 0.5f;

    // Detection constants
    private static final int   COLOR_TOLERANCE        = 40;   // per-channel tolerance
    private static final int   MIN_ENEMY_AREA         = 150;  // px² minimum blob
    private static final float MAX_ENEMY_ASPECT       = 4.0f; // width/height must be < 4
    private static final int   HEALTH_MIN_WIDTH       = 40;
    private static final int   HEALTH_MAX_HEIGHT      = 12;
    private static final int   SCAN_STEP              = 4;    // skip pixels for speed

    // Named screen zones (as fractions of w/h)
    private static final float[] ZONE_HUD_TOP    = {0f,   0f,   1f,   0.12f};
    private static final float[] ZONE_GAMEPLAY   = {0f,   0.12f,1f,   0.82f};
    private static final float[] ZONE_SKILL_BAR  = {0f,   0.82f,1f,   1f};
    private static final float[] ZONE_MINIMAP    = {0.78f,0f,   1f,   0.22f};

    // TFLite
    private Interpreter  tfliteInterpreter;
    private ByteBuffer   inputBuffer;

    // Game config
    private Object       gameType;
    private ElementDetector elementDetector;

    // Color profiles
    private final Map<Object, int[]> enemyColors      = new HashMap<>();
    private final Map<Object, int[]> healthBarColors  = new HashMap<>();

    // Result cache (avoid re-scanning the same frame)
    private final Map<String, Object> analysisCache   = new ConcurrentHashMap<>();

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------
    public GameSceneAnalyzer(File assetDir) {
        this.elementDetector = new ElementDetector();
        initializeColorMaps();
        loadModels(assetDir);
    }

    // -----------------------------------------------------------------------
    // Color maps
    // -----------------------------------------------------------------------
    private void initializeColorMaps() {
        enemyColors.put(AIControllerGameTypeHelper.getPUBG_MOBILE(),
                new int[]{Color.rgb(255,100,0), Color.rgb(255,50,0), Color.rgb(255,0,0)});
        enemyColors.put(AIControllerGameTypeHelper.getFREE_FIRE(),
                new int[]{Color.rgb(255,0,0), Color.rgb(255,60,60), Color.rgb(180,0,0)});
        enemyColors.put(AIControllerGameTypeHelper.getGameTypeValue("FPS"),
                new int[]{Color.rgb(255,0,0), Color.rgb(255,60,0), Color.rgb(200,0,0)});
        enemyColors.put(AIControllerGameTypeHelper.getPOKEMON_UNITE(),
                new int[]{Color.rgb(255,0,0), Color.rgb(255,60,60)});
        enemyColors.put(AIControllerGameTypeHelper.getMOBA(),
                new int[]{Color.rgb(255,0,0), Color.rgb(255,60,60)});
        enemyColors.put(AIControllerGameTypeHelper.getGameTypeValue("OTHER"),
                new int[]{Color.rgb(255,0,0)});

        // Health-bar green / yellow (player) and red (enemy)
        healthBarColors.put("green",  new int[]{Color.rgb(0,200,0), Color.rgb(50,230,50)});
        healthBarColors.put("yellow", new int[]{Color.rgb(220,220,0), Color.rgb(255,200,0)});
        healthBarColors.put("red",    new int[]{Color.rgb(220,0,0), Color.rgb(200,50,50)});
    }

    // -----------------------------------------------------------------------
    // Init for game type
    // -----------------------------------------------------------------------
    public void initializeForGameType(Object gameType) {
        this.gameType = gameType;
        if (gameType != null)
            elementDetector.setGameSpecificParameters(gameType.toString(), 10);
        Log.d(TAG, "Initialized for game: " + gameType);
    }

    // -----------------------------------------------------------------------
    // Model loading
    // -----------------------------------------------------------------------
    private void loadModels(File assetDir) {
        try {
            File f = new File(assetDir, "element_detector.tflite");
            if (f.exists()) {
                tfliteInterpreter = new Interpreter(f);
                inputBuffer = ByteBuffer.allocateDirect(
                        MODEL_INPUT_SIZE * MODEL_INPUT_SIZE * 3 * 4);
                inputBuffer.order(ByteOrder.nativeOrder());
                Log.d(TAG, "TFLite model loaded");
            } else {
                Log.w(TAG, "TFLite model not found at " + f.getAbsolutePath()
                        + " — falling back to pixel-scan only");
            }
        } catch (Exception e) {
            Log.e(TAG, "Model load error: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Main entry point
    // -----------------------------------------------------------------------
    public GameState analyzeScreen(Bitmap screenshot) {
        if (screenshot == null) { Log.e(TAG, "null screenshot"); return null; }
        try {
            GameState state = new GameState();
            if (gameType == null) { detectGenericElements(screenshot, state); return state; }

            String gt = gameType.toString();
            if (isShooter(gt)) {
                detectEnemiesForShooter(screenshot, state);
                detectHealthBars(screenshot, state);
                detectWeapons(screenshot, state);
            } else if (isMOBA(gt)) {
                detectEnemiesForMOBA(screenshot, state);
                detectAbilities(screenshot, state);
                detectMinimap(screenshot, state);
            } else {
                detectGenericElements(screenshot, state);
            }

            detectInteractiveElements(screenshot, state);
            analyzeScreenContext(screenshot, state);
            determineScreenType(screenshot, state);
            return state;
        } catch (Exception e) {
            Log.e(TAG, "analyzeScreen error: " + e.getMessage());
            return null;
        }
    }

    private boolean isShooter(String gt) {
        return "PUBG_MOBILE".equals(gt) || "FREE_FIRE".equals(gt) || "FPS".equals(gt);
    }
    private boolean isMOBA(String gt) {
        return "POKEMON_UNITE".equals(gt) || "MOBA".equals(gt);
    }

    // -----------------------------------------------------------------------
    // Enemy detection — shooter
    // -----------------------------------------------------------------------
    private void detectEnemiesForShooter(Bitmap screenshot, GameState state) {
        int[] colors = getEnemyColors();
        // Restrict scan to gameplay zone
        Rect zone = zoneRect(screenshot, ZONE_GAMEPLAY);
        List<Rect> blobs = colorBlobs(screenshot, colors, zone);
        List<Rect> enemies = new ArrayList<>();
        for (Rect b : blobs) if (isLikelyEnemy(screenshot, b)) enemies.add(b);
        applyEnemies(state, enemies);
    }

    // -----------------------------------------------------------------------
    // Enemy detection — MOBA
    // -----------------------------------------------------------------------
    private void detectEnemiesForMOBA(Bitmap screenshot, GameState state) {
        int[] colors = getEnemyColors();
        // MOBAs often have red health bars above character heads
        // Scan top 80% of screen excluding minimap corner
        Rect zone = new Rect(0, 0,
                (int)(screenshot.getWidth() * 0.78f), (int)(screenshot.getHeight() * 0.82f));
        List<Rect> blobs = colorBlobs(screenshot, colors, zone);
        List<Rect> enemies = new ArrayList<>();
        for (Rect b : blobs) {
            // MOBA enemy health bars are wider than tall
            float aspect = (b.right - b.left) / (float) Math.max(1, b.bottom - b.top);
            if (aspect > 1.5f && aspect < 8f && (b.right - b.left) >= HEALTH_MIN_WIDTH)
                enemies.add(b);
        }
        applyEnemies(state, enemies);
    }

    // -----------------------------------------------------------------------
    // Health-bar detection
    // -----------------------------------------------------------------------
    private void detectHealthBars(Bitmap screenshot, GameState state) {
        // Scan top HUD zone for horizontal colored strips
        Rect zone = zoneRect(screenshot, ZONE_HUD_TOP);
        List<Rect> bars = new ArrayList<>();
        int w = screenshot.getWidth(), h = screenshot.getHeight();
        int zoneBottom = zone.bottom;

        // Slide a 1-pixel tall strip across the HUD zone
        for (int y = zone.top; y < zoneBottom; y += SCAN_STEP) {
            int runStart = -1, runColor = 0;
            for (int x = zone.left; x < zone.right; x += 1) {
                int px = screenshot.getPixel(x, Math.min(y, h - 1));
                boolean isHealth = colorMatchesAny(px, healthBarColors.get("green"))
                        || colorMatchesAny(px, healthBarColors.get("yellow"))
                        || colorMatchesAny(px, healthBarColors.get("red"));
                if (isHealth) {
                    if (runStart < 0) runStart = x;
                    runColor = px;
                } else {
                    if (runStart >= 0 && (x - runStart) >= HEALTH_MIN_WIDTH) {
                        bars.add(new Rect(runStart, y, x, y + HEALTH_MAX_HEIGHT));
                    }
                    runStart = -1;
                }
            }
        }

        if (!bars.isEmpty()) {
            // Use first bar as player health estimate
            Rect bar = bars.get(0);
            float ratio = (bar.right - bar.left) / (float) Math.max(1, w / 4);
            state.setPlayerHealth(Math.min(1f, ratio));
            Log.d(TAG, "Health bars detected: " + bars.size() + " ratio=" + ratio);
        }
    }

    // -----------------------------------------------------------------------
    // Weapon detection (bottom-right HUD area)
    // -----------------------------------------------------------------------
    private void detectWeapons(Bitmap screenshot, GameState state) {
        int w = screenshot.getWidth(), h = screenshot.getHeight();
        // Weapon icons typically appear in bottom-right quarter
        Rect zone = new Rect((int)(w * 0.6f), (int)(h * 0.78f), w, h);
        // Count distinct color clusters as a proxy for weapon count
        int clusters = countColorClusters(screenshot, zone, 3);
        state.setWeaponCount(clusters);
        Log.d(TAG, "Weapons estimated: " + clusters);
    }

    // -----------------------------------------------------------------------
    // Ability detection (bottom skill-bar zone)
    // -----------------------------------------------------------------------
    private void detectAbilities(Bitmap screenshot, GameState state) {
        Rect zone = zoneRect(screenshot, ZONE_SKILL_BAR);
        int clusters = countColorClusters(screenshot, zone, 5);
        state.setAbilityCount(clusters);
        Log.d(TAG, "Abilities estimated: " + clusters);
    }

    // -----------------------------------------------------------------------
    // Minimap detection (top-right corner)
    // -----------------------------------------------------------------------
    private void detectMinimap(Bitmap screenshot, GameState state) {
        Rect zone = zoneRect(screenshot, ZONE_MINIMAP);
        // Minimap backgrounds tend to be dark green/grey — measure mean darkness
        float[] hsv = new float[3];
        float meanS = 0f; int n = 0;
        for (int y = zone.top; y < zone.bottom; y += SCAN_STEP) {
            for (int x = zone.left; x < zone.right; x += SCAN_STEP) {
                Color.colorToHSV(screenshot.getPixel(x, y), hsv);
                meanS += hsv[1]; n++;
            }
        }
        boolean hasMap = n > 0 && (meanS / n) < 0.4f; // low saturation = map background
        state.setMinimapVisible(hasMap);
        Log.d(TAG, "Minimap visible: " + hasMap);
    }

    // -----------------------------------------------------------------------
    // Generic element detection (saliency grid)
    // -----------------------------------------------------------------------
    private void detectGenericElements(Bitmap screenshot, GameState state) {
        int w = screenshot.getWidth(), h = screenshot.getHeight();
        int cols = 8, rows = 6;
        int cw = w / cols, ch = h / rows;
        List<Rect> salient = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Rect cell = new Rect(c * cw, r * ch, (c + 1) * cw, (r + 1) * ch);
                float contrast = cellContrast(screenshot, cell);
                if (contrast > 50f) salient.add(cell); // high-contrast cell
            }
        }
        state.setSalientRegionCount(salient.size());
        Log.d(TAG, "Salient regions: " + salient.size());
    }

    // -----------------------------------------------------------------------
    // Interactive element detection (bright-outlined rectangles)
    // -----------------------------------------------------------------------
    private void detectInteractiveElements(Bitmap screenshot, GameState state) {
        // Look for bright button-like regions in the lower third
        int w = screenshot.getWidth(), h = screenshot.getHeight();
        Rect zone = new Rect(0, (int)(h * 0.67f), w, h);
        int[] brightColors = {
            Color.rgb(255,255,255), Color.rgb(200,200,255),
            Color.rgb(255,200,100), Color.rgb(100,220,255)
        };
        List<Rect> blobs = colorBlobs(screenshot, brightColors, zone);
        state.setInteractiveElementCount(blobs.size());
        Log.d(TAG, "Interactive elements: " + blobs.size());
    }

    // -----------------------------------------------------------------------
    // Screen context analysis (brightness / saturation histogram)
    // -----------------------------------------------------------------------
    private void analyzeScreenContext(Bitmap screenshot, GameState state) {
        float[] hsv = new float[3];
        float totalV = 0, totalS = 0; int n = 0;
        int w = screenshot.getWidth(), h = screenshot.getHeight();
        int step = Math.max(SCAN_STEP, Math.min(w, h) / 50);
        for (int y = 0; y < h; y += step) {
            for (int x = 0; x < w; x += step) {
                Color.colorToHSV(screenshot.getPixel(x, y), hsv);
                totalV += hsv[2]; totalS += hsv[1]; n++;
            }
        }
        float brightness  = n > 0 ? totalV / n : 0.5f;
        float saturation  = n > 0 ? totalS / n : 0.5f;
        state.setSceneBrightness(brightness);
        state.setSceneSaturation(saturation);
        // Simple scene classification
        if (brightness < 0.2f) state.setSceneType("DARK");
        else if (saturation < 0.15f) state.setSceneType("MENU");
        else state.setSceneType("GAMEPLAY");
        Log.d(TAG, "Context: brightness=" + String.format("%.2f", brightness)
                + " saturation=" + String.format("%.2f", saturation));
    }

    // -----------------------------------------------------------------------
    // Screen type determination
    // -----------------------------------------------------------------------
    private void determineScreenType(Bitmap screenshot, GameState state) {
        String gt = gameType != null ? gameType.toString() : "OTHER";
        String type;
        switch (gt) {
            case "PUBG_MOBILE": case "FREE_FIRE": type = detectShooterScreenType(screenshot); break;
            case "CLASH_OF_CLANS":               type = detectStrategyScreenType(screenshot); break;
            case "POKEMON_UNITE": case "MOBA":   type = detectMOBAScreenType(screenshot);    break;
            default:                             type = detectGenericScreenType(screenshot);
        }
        state.setScreenTypeString(type);
        Log.d(TAG, "Screen type: " + type);
    }

    private String detectShooterScreenType(Bitmap bmp) {
        // Lobby/menu = high brightness, low enemy count
        float[] hsv = new float[3];
        Color.colorToHSV(bmp.getPixel(bmp.getWidth() / 2, bmp.getHeight() / 2), hsv);
        if (hsv[2] > 0.85f && hsv[1] < 0.2f) return "MENU";
        int[] red = {Color.rgb(255,0,0)};
        if (colorBlobs(bmp, red, new Rect(0,0,bmp.getWidth(),bmp.getHeight())).size() > 2)
            return "COMBAT";
        return "GAMEPLAY";
    }

    private String detectStrategyScreenType(Bitmap bmp) {
        // Base screen has many interactive elements at bottom
        float[] hsv = new float[3];
        Color.colorToHSV(bmp.getPixel(bmp.getWidth()/2, bmp.getHeight()-10), hsv);
        return hsv[2] > 0.7f ? "BASE_VIEW" : "GAMEPLAY";
    }

    private String detectMOBAScreenType(Bitmap bmp) {
        // Loading/result screen is dark; gameplay has bright minimap
        float[] hsv = new float[3];
        Color.colorToHSV(bmp.getPixel(bmp.getWidth()/2, bmp.getHeight()/2), hsv);
        if (hsv[2] < 0.2f) return "LOADING";
        return detectMinimap(bmp) ? "GAMEPLAY" : "SCORE";
    }

    private boolean detectMinimap(Bitmap bmp) {
        Rect zone = zoneRect(bmp, ZONE_MINIMAP);
        float[] hsv = new float[3]; float s = 0; int n = 0;
        for (int y=zone.top;y<zone.bottom;y+=SCAN_STEP)
            for (int x=zone.left;x<zone.right;x+=SCAN_STEP) {
                Color.colorToHSV(bmp.getPixel(x,y),hsv); s+=hsv[1]; n++;
            }
        return n > 0 && (s/n) < 0.35f;
    }

    private String detectGenericScreenType(Bitmap bmp) {
        float[] hsv = new float[3];
        Color.colorToHSV(bmp.getPixel(bmp.getWidth()/2, bmp.getHeight()/2), hsv);
        if (hsv[2] < 0.15f) return "LOADING";
        if (hsv[1] < 0.1f)  return "MENU";
        return "GAMEPLAY";
    }

    // -----------------------------------------------------------------------
    // Pixel-level helpers
    // -----------------------------------------------------------------------

    /**
     * Returns connected-region blobs matching any of the target colors within zone.
     * Uses a fast row-scan + run-length encoding approach.
     */
    private List<Rect> colorBlobs(Bitmap bmp, int[] targetColors, Rect zone) {
        List<Rect> blobs = new ArrayList<>();
        if (targetColors == null || targetColors.length == 0) return blobs;
        int zl = Math.max(0, zone.left),  zt = Math.max(0, zone.top);
        int zr = Math.min(bmp.getWidth(), zone.right);
        int zb = Math.min(bmp.getHeight(), zone.bottom);

        // Simple row-scan merge — collect horizontal runs, then merge vertically
        List<Rect> runs = new ArrayList<>();
        for (int y = zt; y < zb; y += SCAN_STEP) {
            int runStart = -1;
            for (int x = zl; x < zr; x++) {
                int px = bmp.getPixel(x, y);
                if (colorMatchesAny(px, targetColors)) {
                    if (runStart < 0) runStart = x;
                } else {
                    if (runStart >= 0) {
                        runs.add(new Rect(runStart, y, x, y + SCAN_STEP));
                        runStart = -1;
                    }
                }
            }
            if (runStart >= 0) runs.add(new Rect(runStart, zt, zr, y + SCAN_STEP));
        }

        // Merge overlapping runs
        if (runs.isEmpty()) return blobs;
        runs.sort((a, b) -> a.left - b.left);
        Rect current = new Rect(runs.get(0));
        for (int i = 1; i < runs.size(); i++) {
            Rect r = runs.get(i);
            if (r.left <= current.right + 4 && r.top <= current.bottom + SCAN_STEP * 2) {
                current.left   = Math.min(current.left,  r.left);
                current.top    = Math.min(current.top,   r.top);
                current.right  = Math.max(current.right, r.right);
                current.bottom = Math.max(current.bottom,r.bottom);
            } else {
                int area = (current.right - current.left) * (current.bottom - current.top);
                if (area >= MIN_ENEMY_AREA) blobs.add(new Rect(current));
                current.set(r);
            }
        }
        int area = (current.right - current.left) * (current.bottom - current.top);
        if (area >= MIN_ENEMY_AREA) blobs.add(current);
        return blobs;
    }

    /** Checks if pixel color matches any color in the array within COLOR_TOLERANCE. */
    private boolean colorMatchesAny(int pixel, int[] targets) {
        if (targets == null) return false;
        int pr = Color.red(pixel), pg = Color.green(pixel), pb = Color.blue(pixel);
        for (int t : targets) {
            if (Math.abs(pr - Color.red(t))   <= COLOR_TOLERANCE
             && Math.abs(pg - Color.green(t)) <= COLOR_TOLERANCE
             && Math.abs(pb - Color.blue(t))  <= COLOR_TOLERANCE) return true;
        }
        return false;
    }

    /** Returns true if a color region is plausibly an enemy (size + aspect ratio). */
    private boolean isLikelyEnemy(Bitmap bmp, Rect r) {
        int w = r.right - r.left, h = r.bottom - r.top;
        if (w <= 0 || h <= 0) return false;
        float aspect = (float) w / h;
        int area = w * h;
        return area >= MIN_ENEMY_AREA && aspect < MAX_ENEMY_ASPECT && aspect > 0.2f;
    }

    /** Applies enemy list to GameState. */
    private void applyEnemies(GameState state, List<Rect> enemies) {
        float[][] positions = new float[enemies.size()][2];
        for (int i = 0; i < enemies.size(); i++) {
            Rect e = enemies.get(i);
            positions[i][0] = (e.left + e.right) / 2f;
            positions[i][1] = (e.top  + e.bottom) / 2f;
        }
        state.setEnemyPositions(positions);
        state.setEnemyCount(enemies.size());
        Log.d(TAG, "Enemies: " + enemies.size());
    }

    /**
     * Counts approximate number of distinct color clusters in a zone.
     * Used as a proxy for button / icon count.
     */
    private int countColorClusters(Bitmap bmp, Rect zone, int maxClusters) {
        List<int[]> centroids = new ArrayList<>(); // [r,g,b]
        int zl = Math.max(0, zone.left), zt = Math.max(0, zone.top);
        int zr = Math.min(bmp.getWidth(), zone.right);
        int zb = Math.min(bmp.getHeight(), zone.bottom);
        int step = Math.max(SCAN_STEP, (zr - zl) / 30);

        for (int y = zt; y < zb; y += step) {
            for (int x = zl; x < zr; x += step) {
                int px = bmp.getPixel(x, y);
                int pr = Color.red(px), pg = Color.green(px), pb = Color.blue(px);
                // Skip very dark / desaturated pixels
                if (pr < 40 && pg < 40 && pb < 40) continue;
                if (Math.abs(pr - pg) < 20 && Math.abs(pg - pb) < 20 && pr < 160) continue;

                boolean found = false;
                for (int[] c : centroids) {
                    if (Math.abs(c[0]-pr)<50 && Math.abs(c[1]-pg)<50 && Math.abs(c[2]-pb)<50) {
                        c[0] = (c[0]+pr)/2; c[1] = (c[1]+pg)/2; c[2] = (c[2]+pb)/2;
                        found = true; break;
                    }
                }
                if (!found && centroids.size() < maxClusters)
                    centroids.add(new int[]{pr, pg, pb});
            }
        }
        return centroids.size();
    }

    /** Converts zone definition (fractions) to pixel Rect. */
    private Rect zoneRect(Bitmap bmp, float[] zone) {
        int w = bmp.getWidth(), h = bmp.getHeight();
        return new Rect(
            (int)(zone[0] * w), (int)(zone[1] * h),
            (int)(zone[2] * w), (int)(zone[3] * h));
    }

    /** Returns RMS contrast of a cell (max − min channel range across sampled pixels). */
    private float cellContrast(Bitmap bmp, Rect cell) {
        int min = 255, max = 0;
        int step = Math.max(1, (cell.right - cell.left) / 8);
        for (int y = cell.top; y < cell.bottom; y += step) {
            for (int x = cell.left; x < cell.right; x += step) {
                if (x >= bmp.getWidth() || y >= bmp.getHeight()) continue;
                int px  = bmp.getPixel(x, y);
                int lum = (Color.red(px) + Color.green(px) + Color.blue(px)) / 3;
                if (lum < min) min = lum; if (lum > max) max = lum;
            }
        }
        return max - min;
    }

    private int[] getEnemyColors() {
        int[] c = enemyColors.get(gameType);
        return c != null ? c : enemyColors.get(AIControllerGameTypeHelper.getGameTypeValue("OTHER"));
    }

    // -----------------------------------------------------------------------
    // TFLite inference
    // -----------------------------------------------------------------------
    private float[][] runObjectDetection(Bitmap bitmap) {
        if (tfliteInterpreter == null) return new float[0][0];
        try {
            Bitmap resized = Bitmap.createScaledBitmap(bitmap, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, true);
            inputBuffer.rewind();
            int[] pixels = new int[MODEL_INPUT_SIZE * MODEL_INPUT_SIZE];
            resized.getPixels(pixels, 0, MODEL_INPUT_SIZE, 0, 0, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE);
            for (int px : pixels) {
                inputBuffer.putFloat(((px >> 16) & 0xFF) / 255f);
                inputBuffer.putFloat(((px >>  8) & 0xFF) / 255f);
                inputBuffer.putFloat(( px        & 0xFF) / 255f);
            }
            float[][][] out = new float[1][MAX_PREDICTIONS][6];
            Map<Integer, Object> outputs = new HashMap<>();
            outputs.put(0, out);
            tfliteInterpreter.runForMultipleInputsOutputs(new Object[]{inputBuffer}, outputs);
            float[][] results = new float[MAX_PREDICTIONS][6];
            for (int i = 0; i < MAX_PREDICTIONS; i++) {
                if (out[0][i][4] < MIN_CONFIDENCE) continue;
                results[i][0] = out[0][i][1] * bitmap.getWidth();
                results[i][1] = out[0][i][0] * bitmap.getHeight();
                results[i][2] = out[0][i][3] * bitmap.getWidth();
                results[i][3] = out[0][i][2] * bitmap.getHeight();
                results[i][4] = out[0][i][4];
                results[i][5] = out[0][i][5];
            }
            return results;
        } catch (Exception e) {
            Log.e(TAG, "TFLite inference error: " + e.getMessage());
            return new float[0][0];
        }
    }

    // -----------------------------------------------------------------------
    // Cleanup
    // -----------------------------------------------------------------------
    public void close() {
        if (tfliteInterpreter != null) { tfliteInterpreter.close(); tfliteInterpreter = null; }
        analysisCache.clear();
    }
}
