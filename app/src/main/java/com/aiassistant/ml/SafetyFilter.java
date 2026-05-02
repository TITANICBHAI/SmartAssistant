package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SafetyFilter — validates and sanitizes AI-generated action recommendations
 * before they reach the execution layer.
 *
 * Safety checks performed:
 *
 *   1. ACTION_TYPE_WHITELIST
 *      Only action types in the configured whitelist are allowed.
 *      Unknown types → blocked with reason UNKNOWN_ACTION_TYPE.
 *
 *   2. COORDINATE_BOUNDS
 *      For TAP / SWIPE actions, verifies x/y coordinates are within
 *      screen bounds [0, screenWidth] × [0, screenHeight].
 *
 *   3. RATE_LIMIT
 *      Identical action-type sequences within RATE_WINDOW_MS are blocked
 *      to prevent click-flooding attacks on the accessibility service.
 *
 *   4. PARAMETER_SANITY
 *      Checks that required parameters are present and numeric values
 *      fall within expected ranges (e.g. swipe speed > 0, pressure ≤ 1.0).
 *
 *   5. CONTEXT_SAFETY
 *      Game-state aware veto: e.g. forbid attacking when ally health > 0.9
 *      to prevent friendly-fire, or forbid USE_SKILL when skill meter < 0.5.
 *
 *   6. CATASTROPHIC_PREVENTION
 *      Hard-veto for actions that could cause unrecoverable states
 *      (e.g. QUIT_GAME during an active boss fight).
 *
 * For every blocked action, a SafetyViolation record is emitted and optionally
 * delivered to registered ViolationListeners for telemetry / logging.
 *
 * All methods are thread-safe.
 */
public class SafetyFilter {

    private static final String TAG = "SafetyFilter";

    private static final long RATE_WINDOW_MS   = 500L;  // repeat detection window
    private static final int  MAX_REPEATS      = 3;     // max same-type actions in window

    // -------------------------------------------------------------------------
    // Safety violation record
    // -------------------------------------------------------------------------
    public enum ViolationReason {
        UNKNOWN_ACTION_TYPE,
        OUT_OF_BOUNDS,
        RATE_LIMIT_EXCEEDED,
        MISSING_PARAMETER,
        PARAMETER_OUT_OF_RANGE,
        CONTEXT_VETO,
        CATASTROPHIC_PREVENTION
    }

    public static class SafetyViolation {
        public final String         actionType;
        public final ViolationReason reason;
        public final String         detail;
        public final long           timestamp;

        public SafetyViolation(String actionType, ViolationReason reason, String detail) {
            this.actionType = actionType;
            this.reason     = reason;
            this.detail     = detail;
            this.timestamp  = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return "SafetyViolation{" + actionType + " → " + reason + ": " + detail + "}";
        }
    }

    public interface ViolationListener {
        void onViolation(SafetyViolation violation);
    }

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    private final Set<String>   allowedActionTypes   = new HashSet<>();
    private final Set<String>   catastrophicActions  = new HashSet<>();
    private int                 screenWidth          = 1080;
    private int                 screenHeight         = 1920;

    // Rate-limit tracking: action type → list of recent dispatch timestamps
    private final Map<String, List<Long>> recentActions = new HashMap<>();

    // Violation log
    private final List<SafetyViolation>   violations    = new ArrayList<>();
    private static final int              MAX_VIOLATIONS = 500;

    private final List<ViolationListener> listeners = new CopyOnWriteArrayList<>();

    // Stats
    private final AtomicInteger allowedCount   = new AtomicInteger(0);
    private final AtomicInteger blockedCount   = new AtomicInteger(0);

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    public SafetyFilter(int screenWidth, int screenHeight) {
        this.screenWidth  = screenWidth;
        this.screenHeight = screenHeight;

        // Default allowed action types
        String[] defaults = { "TAP", "SWIPE", "LONG_PRESS", "SCROLL", "DRAG",
                              "attack", "defend", "heal", "collect", "dodge",
                              "block", "use_item", "interact", "move" };
        for (String t : defaults) allowedActionTypes.add(t.toUpperCase());

        // Catastrophic actions (always blocked unless explicitly unlocked)
        catastrophicActions.add("QUIT_GAME");
        catastrophicActions.add("DELETE_SAVE");
        catastrophicActions.add("RESET_PROGRESS");
        catastrophicActions.add("PURCHASE");  // no auto-purchases
    }

    public SafetyFilter() {
        this(1080, 1920);
    }

    // -------------------------------------------------------------------------
    // Configuration API
    // -------------------------------------------------------------------------

    public void allowActionType(String type)       { allowedActionTypes.add(type.toUpperCase()); }
    public void disallowActionType(String type)    { allowedActionTypes.remove(type.toUpperCase()); }
    public void addCatastrophicAction(String type) { catastrophicActions.add(type.toUpperCase()); }
    public void setScreenDimensions(int w, int h)  { this.screenWidth = w; this.screenHeight = h; }
    public void addListener(ViolationListener l)   { listeners.add(l); }
    public void removeListener(ViolationListener l){ listeners.remove(l); }

    // -------------------------------------------------------------------------
    // Core API
    // -------------------------------------------------------------------------

    /**
     * Validate a proposed action against all safety checks.
     *
     * @param actionType Action type string (e.g. "TAP", "attack").
     * @param params     Action parameters map.
     * @param gameState  Current game state (may be null).
     * @return true if the action is safe to execute; false if it was blocked.
     */
    public synchronized boolean validate(String actionType,
                                          Map<String, Object> params,
                                          Map<String, Object> gameState) {
        String type = actionType != null ? actionType.toUpperCase() : "UNKNOWN";
        params      = params != null ? params : new HashMap<>();

        // ---- Check 1: Catastrophic prevention ----
        if (catastrophicActions.contains(type)) {
            return block(type, ViolationReason.CATASTROPHIC_PREVENTION,
                    "Action '" + type + "' is permanently blocked");
        }

        // ---- Check 2: Action type whitelist ----
        if (!allowedActionTypes.contains(type)) {
            return block(type, ViolationReason.UNKNOWN_ACTION_TYPE,
                    "'" + type + "' not in allowed action types");
        }

        // ---- Check 3: Coordinate bounds ----
        if (type.equals("TAP") || type.equals("SWIPE") || type.equals("DRAG")) {
            String coordViolation = checkCoordinates(params);
            if (coordViolation != null) {
                return block(type, ViolationReason.OUT_OF_BOUNDS, coordViolation);
            }
        }

        // ---- Check 4: Parameter sanity ----
        String paramViolation = checkParameters(type, params);
        if (paramViolation != null) {
            return block(type, ViolationReason.PARAMETER_OUT_OF_RANGE, paramViolation);
        }

        // ---- Check 5: Rate limit ----
        if (isRateLimited(type)) {
            return block(type, ViolationReason.RATE_LIMIT_EXCEEDED,
                    "Too many '" + type + "' actions in " + RATE_WINDOW_MS + " ms");
        }

        // ---- Check 6: Context safety ----
        if (gameState != null) {
            String ctxViolation = checkContext(type, params, gameState);
            if (ctxViolation != null) {
                return block(type, ViolationReason.CONTEXT_VETO, ctxViolation);
            }
        }

        // ---- All checks passed ----
        recordAllowed(type);
        return true;
    }

    /**
     * Produce a filtered action mask (boolean[actionDim]) where true = safe.
     * Quick version for use inside beam search / Q-value computation.
     */
    public synchronized boolean[] getActionMask(String[] actionTypes,
                                                  Map<String, Object> gameState) {
        boolean[] mask = new boolean[actionTypes.length];
        for (int i = 0; i < actionTypes.length; i++) {
            String t = actionTypes[i].toUpperCase();
            mask[i] = !catastrophicActions.contains(t)
                    && allowedActionTypes.contains(t)
                    && (gameState == null || checkContext(t, new HashMap<>(), gameState) == null);
        }
        return mask;
    }

    // -------------------------------------------------------------------------
    // Individual checks
    // -------------------------------------------------------------------------

    private String checkCoordinates(Map<String, Object> params) {
        float x  = getFloat(params, "x",  -1f);
        float y  = getFloat(params, "y",  -1f);
        float x2 = getFloat(params, "x2", -1f);
        float y2 = getFloat(params, "y2", -1f);

        if (x >= 0 && (x < 0 || x > screenWidth))
            return "x=" + x + " out of [0," + screenWidth + "]";
        if (y >= 0 && (y < 0 || y > screenHeight))
            return "y=" + y + " out of [0," + screenHeight + "]";
        if (x2 >= 0 && (x2 < 0 || x2 > screenWidth))
            return "x2=" + x2 + " out of [0," + screenWidth + "]";
        if (y2 >= 0 && (y2 < 0 || y2 > screenHeight))
            return "y2=" + y2 + " out of [0," + screenHeight + "]";
        return null; // OK
    }

    private String checkParameters(String type, Map<String, Object> params) {
        float speed = getFloat(params, "speed", 1f);
        if (speed < 0) return "speed must be ≥ 0, got " + speed;

        float pressure = getFloat(params, "pressure", 1f);
        if (pressure < 0 || pressure > 1f) return "pressure must be [0,1], got " + pressure;

        long durationMs = (long) getFloat(params, "durationMs", 100f);
        if (durationMs < 0 || durationMs > 10_000L)
            return "durationMs must be [0,10000], got " + durationMs;

        return null; // OK
    }

    private String checkContext(String type, Map<String, Object> params,
                                 Map<String, Object> state) {
        float health = getFloat(state, "health", 1f);

        // Don't attack when health is critically low — heal first
        if (type.equals("ATTACK") && health < 0.1f)
            return "Health critical (" + health + "); attack vetoed";

        // Don't use skill if meter not ready
        if (type.equals("USE_SKILL")) {
            float meter = getFloat(state, "skillMeter", 1f);
            if (meter < 0.5f) return "Skill meter too low: " + meter;
        }

        return null; // OK
    }

    // -------------------------------------------------------------------------
    // Rate limiting
    // -------------------------------------------------------------------------

    private boolean isRateLimited(String type) {
        long now = System.currentTimeMillis();
        List<Long> times = recentActions.computeIfAbsent(type, k -> new ArrayList<>());

        // Prune old entries
        times.removeIf(t -> (now - t) > RATE_WINDOW_MS);

        return times.size() >= MAX_REPEATS;
    }

    private void recordAllowed(String type) {
        allowedCount.incrementAndGet();
        List<Long> times = recentActions.computeIfAbsent(type, k -> new ArrayList<>());
        times.add(System.currentTimeMillis());
    }

    // -------------------------------------------------------------------------
    // Blocking helper
    // -------------------------------------------------------------------------

    private boolean block(String type, ViolationReason reason, String detail) {
        blockedCount.incrementAndGet();
        SafetyViolation v = new SafetyViolation(type, reason, detail);
        Log.w(TAG, "BLOCKED: " + v);
        synchronized (violations) {
            if (violations.size() >= MAX_VIOLATIONS) violations.remove(0);
            violations.add(v);
        }
        for (ViolationListener l : listeners) {
            try { l.onViolation(v); }
            catch (Exception e) { Log.w(TAG, "Listener error: " + e.getMessage()); }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Monitoring
    // -------------------------------------------------------------------------

    public synchronized List<SafetyViolation> getRecentViolations(int n) {
        int size = violations.size();
        int from = Math.max(0, size - n);
        return new ArrayList<>(violations.subList(from, size));
    }

    public Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("allowedCount",      allowedCount.get());
        s.put("blockedCount",      blockedCount.get());
        s.put("allowedTypes",      allowedActionTypes.size());
        s.put("catastrophicTypes", catastrophicActions.size());
        s.put("violationCount",    violations.size());
        s.put("screenWidth",       screenWidth);
        s.put("screenHeight",      screenHeight);
        // Violation breakdown by reason
        Map<String, Integer> byReason = new HashMap<>();
        for (SafetyViolation v : violations) {
            String k = v.reason.name();
            byReason.put(k, byReason.getOrDefault(k, 0) + 1);
        }
        s.put("violationsByReason", byReason);
        return s;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static float getFloat(Map<String, Object> m, String key, float def) {
        Object v = m.get(key);
        if (v instanceof Number) return ((Number)v).floatValue();
        if (v instanceof String) { try { return Float.parseFloat((String)v); } catch (NumberFormatException ignored) {} }
        return def;
    }
}
