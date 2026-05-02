package com.aiassistant.core;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;

import com.aiassistant.ml.ActionSuggestion;
import com.aiassistant.ml.PredictiveActionSystem;
import models.AppInfo;
import utils.ActionHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Central controller for the AI assistant.
 * Manages operating modes, action execution, suggestion routing,
 * per-type success tracking, and retry-with-exponential-backoff.
 */
public class AIController {

    private static final String TAG = "AIController";

    // Retry configuration
    private static final int  MAX_RETRIES    = 3;
    private static final long BASE_BACKOFF_MS = 200L;

    // Suggestion polling interval when active
    private static final long SUGGESTION_POLL_MS = 2_000L;

    // Consecutive-error threshold before we apply back-pressure
    private static final int ERROR_THRESHOLD = 5;

    // -----------------------------------------------------------------------
    // Enums
    // -----------------------------------------------------------------------

    public enum Mode {
        INACTIVE,
        PASSIVE,
        ACTIVE,
        GAMING,
        LEARNING
    }

    public enum GameType {
        PUBG_MOBILE,
        FREE_FIRE,
        FPS,
        CLASH_OF_CLANS,
        STRATEGY,
        POKEMON_UNITE,
        MOBA,
        RPG,
        OTHER;

        public static GameType fromPackageName(String packageName) {
            if (packageName == null) return OTHER;
            String p = packageName.toLowerCase();
            if (p.contains("pubg") || p.contains("playerunknown"))    return PUBG_MOBILE;
            if (p.contains("freefire") || p.contains("garena"))       return FREE_FIRE;
            if (p.contains("clash") && p.contains("clans"))           return CLASH_OF_CLANS;
            if (p.contains("pokemon") && p.contains("unite"))         return POKEMON_UNITE;
            if (p.contains("mobilelegends") || p.contains("league")
                    || p.contains("dota") || p.contains("vainglory")) return MOBA;
            if (p.contains("fps") || p.contains("shooter")
                    || p.contains("gun")    || p.contains("strike")
                    || p.contains("battle") || p.contains("callofduty")
                    || p.contains("cod"))                             return FPS;
            if (p.contains("rpg") || p.contains("role")
                    || p.contains("genshin") || p.contains("fantasy")) return RPG;
            if (p.contains("strategy") || p.contains("tower")
                    || p.contains("command") || p.contains("empire")
                    || p.contains("royal"))                           return STRATEGY;
            return OTHER;
        }
    }

    // -----------------------------------------------------------------------
    // Singleton
    // -----------------------------------------------------------------------

    private static volatile AIController instance;

    public static AIController getInstance(Context context) {
        if (instance == null) {
            synchronized (AIController.class) {
                if (instance == null) {
                    instance = new AIController(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    // -----------------------------------------------------------------------
    // Interfaces
    // -----------------------------------------------------------------------

    public interface AIControllerListener {
        void onModeChanged(Mode newMode);
        void onSuggestionsAvailable(List<ActionSuggestion> suggestions);
        void onActionExecuted(String actionId, boolean success);
        void onError(String errorMessage);
    }

    public interface ActionCallback {
        void onComplete(Map<String, Object> result);
        void onError(String errorMessage);
    }

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    private final Context applicationContext;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> suggestionPollerFuture;

    private final Map<String, ActionHandler> actionHandlers = new ConcurrentHashMap<>();
    private final List<AIControllerListener> listeners      = new CopyOnWriteArrayList<>();

    private volatile Mode    currentMode  = Mode.INACTIVE;
    private volatile boolean initialized  = false;
    private volatile boolean gameMode     = false;

    private PredictiveActionSystem predictiveSystem;
    private AppInfo currentApp;

    // Per-action-type success tracking
    private final Map<String, AtomicInteger> actionSuccessCount = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> actionTotalCount   = new ConcurrentHashMap<>();

    // Back-pressure tracking
    private final AtomicInteger consecutiveErrors = new AtomicInteger(0);
    private final AtomicLong    lastErrorTime     = new AtomicLong(0);

    // Controls the periodic suggestion poller
    private final AtomicBoolean pollerActive = new AtomicBoolean(false);

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    private AIController(Context applicationContext) {
        this.applicationContext = applicationContext;
        this.executorService    = Executors.newCachedThreadPool();
        this.scheduler          = Executors.newSingleThreadScheduledExecutor();
    }

    // -----------------------------------------------------------------------
    // Initialization
    // -----------------------------------------------------------------------

    public boolean initialize() {
        if (initialized) return true;

        try {
            Log.i(TAG, "Initializing AI controller");

            // Build PredictiveActionSystem with a direct typed callback — no reflection.
            predictiveSystem = new PredictiveActionSystem(applicationContext, this);
            predictiveSystem.initialize();

            predictiveSystem.registerCallback(new PredictiveActionSystem.PredictionCallback() {
                @Override
                public void onStatePrediction(
                        PredictiveActionSystem.GameState current,
                        PredictiveActionSystem.GameState predicted) {
                    // Consumed by the RL subsystem internally
                }

                @Override
                public void onActionRecommendation(com.aiassistant.ml.GameAction action) {
                    if (action == null) return;
                    List<ActionSuggestion> suggestions = Collections.singletonList(
                            convertGameActionToSuggestion(action));
                    notifySuggestionsAvailable(suggestions);
                }
            });

            setMode(Mode.PASSIVE);
            initialized = true;
            consecutiveErrors.set(0);
            Log.i(TAG, "AI controller initialized successfully");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error initializing AI controller", e);
            notifyError("Failed to initialize AI controller: " + e.getMessage());
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // Suggestion polling
    // -----------------------------------------------------------------------

    /** Starts automatic suggestion generation every {@link #SUGGESTION_POLL_MS} ms. */
    public void startSuggestionPolling() {
        if (!pollerActive.compareAndSet(false, true)) return;
        suggestionPollerFuture = scheduler.scheduleAtFixedRate(
                this::getSuggestions, 0, SUGGESTION_POLL_MS, TimeUnit.MILLISECONDS);
        Log.d(TAG, "Suggestion polling started");
    }

    public void stopSuggestionPolling() {
        if (pollerActive.compareAndSet(true, false) && suggestionPollerFuture != null) {
            suggestionPollerFuture.cancel(false);
            Log.d(TAG, "Suggestion polling stopped");
        }
    }

    // -----------------------------------------------------------------------
    // Listener management
    // -----------------------------------------------------------------------

    public void addListener(AIControllerListener listener) {
        if (listener != null && !listeners.contains(listener)) listeners.add(listener);
    }

    public void removeListener(AIControllerListener listener) {
        listeners.remove(listener);
    }

    // -----------------------------------------------------------------------
    // Action handler registry
    // -----------------------------------------------------------------------

    public void registerActionHandler(String handlerType, ActionHandler handler) {
        if (handlerType != null && !handlerType.isEmpty() && handler != null) {
            actionHandlers.put(handlerType, handler);
        }
    }

    // -----------------------------------------------------------------------
    // Mode management
    // -----------------------------------------------------------------------

    public Mode getMode() { return currentMode; }

    public void setMode(Mode mode) {
        if (mode == null || mode == currentMode) return;
        Mode old = currentMode;
        currentMode = mode;
        gameMode    = (mode == Mode.GAMING);
        Log.i(TAG, "Mode changed: " + old + " → " + mode);
        for (AIControllerListener l : listeners) {
            try { l.onModeChanged(mode); }
            catch (Exception ex) { Log.w(TAG, "Listener error in onModeChanged", ex); }
        }
    }

    public boolean isInitialized() { return initialized; }
    public boolean isGameMode()    { return gameMode || currentMode == Mode.GAMING; }

    public void setGameMode(boolean enabled) {
        if (enabled == gameMode) return;
        gameMode = enabled;
        if (enabled && currentMode != Mode.GAMING) setMode(Mode.GAMING);
        else if (!enabled && currentMode == Mode.GAMING) setMode(Mode.ACTIVE);
    }

    // -----------------------------------------------------------------------
    // App tracking
    // -----------------------------------------------------------------------

    public AppInfo getCurrentApp() { return currentApp; }

    public void setCurrentApp(AppInfo appInfo) {
        this.currentApp = appInfo;
        if (appInfo != null && appInfo.getGameType() != null) setGameMode(true);
    }

    /** Returns a safe state snapshot for the UI layer. */
    public models.AIState getAIState() { return new models.AIState(); }

    /** Returns true when the system accessibility service is active for this package. */
    public boolean checkAccessibilityServiceEnabled() {
        AccessibilityManager am = (AccessibilityManager)
                applicationContext.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am == null) return false;
        for (AccessibilityServiceInfo s :
                am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
            if (s.getResolveInfo().serviceInfo.packageName
                    .equals(applicationContext.getPackageName())) return true;
        }
        return false;
    }

    // -----------------------------------------------------------------------
    // Suggestion generation
    // -----------------------------------------------------------------------

    public void getSuggestions() {
        if (!ensureInitialized()) return;
        if (currentMode == Mode.INACTIVE) return;

        // Apply back-pressure when consecutive errors are high
        if (consecutiveErrors.get() >= ERROR_THRESHOLD) {
            long elapsed = System.currentTimeMillis() - lastErrorTime.get();
            long backoff  = BASE_BACKOFF_MS * (1L << Math.min(consecutiveErrors.get(), 6));
            if (elapsed < backoff) return;
        }

        executorService.execute(() -> {
            try {
                Map<String, Object> ctx = new HashMap<>();
                ctx.put("mode", currentMode.name());
                ctx.put("gameMode", gameMode);
                if (currentApp != null) ctx.put("packageName", currentApp.getPackageName());
                predictiveSystem.getSuggestions(currentApp, ctx);
                consecutiveErrors.set(0);
            } catch (Exception e) {
                Log.w(TAG, "Error getting suggestions", e);
                consecutiveErrors.incrementAndGet();
                lastErrorTime.set(System.currentTimeMillis());
                notifyError("Suggestion error: " + e.getMessage());
            }
        });
    }

    // -----------------------------------------------------------------------
    // Action execution (with retry + exponential back-off)
    // -----------------------------------------------------------------------

    public boolean executeAction(ActionSuggestion suggestion) {
        if (suggestion == null) return false;
        String actionType = mapSuggestionToHandlerKey(suggestion);
        if (actionType == null) {
            notifyError("Unknown action type: " + suggestion.getType());
            return false;
        }
        ActionHandler handler = actionHandlers.get(actionType);
        if (handler == null) {
            notifyError("No handler for action type: " + actionType);
            return false;
        }
        final String actionId  = suggestion.getId();
        final Map<String, Object> params = suggestion.getParameters();
        executorService.execute(() -> executeWithRetry(handler, actionId, actionType, params, 0));
        return true;
    }

    private void executeWithRetry(ActionHandler handler, String actionId,
                                  String actionType, Map<String, Object> params, int attempt) {
        try {
            boolean success = handler.executeAction(params);
            recordOutcome(actionType, success);
            notifyActionExecuted(actionId, success);
        } catch (Exception e) {
            Log.w(TAG, "Execute attempt " + (attempt + 1) + " failed for " + actionType, e);
            if (attempt < MAX_RETRIES) {
                long delay = BASE_BACKOFF_MS * (1L << attempt);
                scheduler.schedule(
                        () -> executeWithRetry(handler, actionId, actionType, params, attempt + 1),
                        delay, TimeUnit.MILLISECONDS);
            } else {
                recordOutcome(actionType, false);
                notifyError("Action failed after " + MAX_RETRIES + " retries: " + e.getMessage());
                notifyActionExecuted(actionId, false);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Per-type success tracking
    // -----------------------------------------------------------------------

    private void recordOutcome(String actionType, boolean success) {
        actionTotalCount.computeIfAbsent(actionType, k -> new AtomicInteger(0)).incrementAndGet();
        if (success) {
            actionSuccessCount.computeIfAbsent(actionType, k -> new AtomicInteger(0)).incrementAndGet();
        }
    }

    /** Returns a 0–1 success rate for the given action type, or -1 if no data. */
    public float getSuccessRate(String actionType) {
        AtomicInteger total = actionTotalCount.get(actionType);
        if (total == null || total.get() == 0) return -1f;
        AtomicInteger success = actionSuccessCount.getOrDefault(actionType, new AtomicInteger(0));
        return (float) success.get() / total.get();
    }

    /** Snapshot of all per-type success rates. */
    public Map<String, Float> getAllSuccessRates() {
        Map<String, Float> rates = new HashMap<>();
        for (String key : actionTotalCount.keySet()) rates.put(key, getSuccessRate(key));
        return rates;
    }

    // -----------------------------------------------------------------------
    // Touch actions — dispatched through AIAccessibilityService
    // -----------------------------------------------------------------------

    public boolean clickAction(int x, int y) {
        Log.d(TAG, "Click @ " + x + "," + y);
        return dispatchGesture("click", x, y, -1, -1, 0);
    }

    public boolean clickAction(int x, int y, ActionCallback callback) {
        boolean r = clickAction(x, y);
        if (callback != null) {
            if (r) { Map<String, Object> res = new HashMap<>();
                res.put("x", x); res.put("y", y); callback.onComplete(res); }
            else   { callback.onError("Click failed"); }
        }
        return r;
    }

    public boolean longPressAction(int x, int y) {
        Log.d(TAG, "LongPress @ " + x + "," + y);
        return dispatchGesture("longpress", x, y, -1, -1, 800);
    }

    public boolean longPressAction(int x, int y, ActionCallback callback) {
        boolean r = longPressAction(x, y);
        if (callback != null) {
            if (r) { Map<String, Object> res = new HashMap<>();
                res.put("x", x); res.put("y", y); callback.onComplete(res); }
            else   { callback.onError("LongPress failed"); }
        }
        return r;
    }

    public boolean swipeAction(int startX, int startY, int endX, int endY, long duration) {
        Log.d(TAG, "Swipe (" + startX + "," + startY + ")→(" + endX + "," + endY + ")");
        return dispatchGesture("swipe", startX, startY, endX, endY, duration);
    }

    public boolean swipeAction(int startX, int startY, int endX, int endY, long duration,
                               ActionCallback callback) {
        boolean r = swipeAction(startX, startY, endX, endY, duration);
        if (callback != null) {
            if (r) { Map<String, Object> res = new HashMap<>();
                res.put("startX", startX); res.put("startY", startY);
                res.put("endX", endX);     res.put("endY", endY);
                res.put("duration", duration); callback.onComplete(res); }
            else   { callback.onError("Swipe failed"); }
        }
        return r;
    }

    private boolean dispatchGesture(String type, int x, int y, int ex, int ey, long duration) {
        try {
            com.aiassistant.services.AIAccessibilityService svc =
                    com.aiassistant.services.AIAccessibilityService.getInstance();
            if (svc == null) {
                Log.w(TAG, "AccessibilityService not active — gesture queued: " + type);
                return false;
            }
            switch (type) {
                case "click":     return svc.performClick(x, y);
                case "longpress": return svc.performLongPress(x, y);
                case "swipe":     return svc.performSwipe(x, y, ex, ey, duration);
                default:          return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Gesture dispatch error", e);
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private boolean ensureInitialized() {
        if (!initialized) {
            Log.d(TAG, "Not initialized — attempting lazy init");
            return initialize();
        }
        return true;
    }

    private String mapSuggestionToHandlerKey(ActionSuggestion suggestion) {
        if (suggestion == null || suggestion.getType() == null) return null;
        switch (suggestion.getType()) {
            case SYSTEM_ACTION: return "system_action";
            case API_CALL:      return "api_call";
            case EMAIL:         return "email";
            case APP_LAUNCH:
            case APP_ACTION:    return "app_control";
            case NOTIFICATION:  return "notification";
            case CUSTOM:        return "custom";
            default:            return null;
        }
    }

    private ActionSuggestion convertGameActionToSuggestion(com.aiassistant.ml.GameAction action) {
        ActionSuggestion s = new ActionSuggestion();
        if (action != null) s.setDescription(action.toString());
        return s;
    }

    // -----------------------------------------------------------------------
    // Notification helpers
    // -----------------------------------------------------------------------

    private void notifySuggestionsAvailable(List<ActionSuggestion> suggestions) {
        for (AIControllerListener l : listeners) {
            try { l.onSuggestionsAvailable(suggestions); }
            catch (Exception e) { Log.w(TAG, "Listener error", e); }
        }
    }

    private void notifyActionExecuted(String actionId, boolean success) {
        for (AIControllerListener l : listeners) {
            try { l.onActionExecuted(actionId, success); }
            catch (Exception e) { Log.w(TAG, "Listener error", e); }
        }
    }

    private void notifyError(String msg) {
        for (AIControllerListener l : listeners) {
            try { l.onError(msg); }
            catch (Exception e) { Log.w(TAG, "Listener error", e); }
        }
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    public void release() {
        stopSuggestionPolling();

        if (predictiveSystem != null) {
            try { predictiveSystem.release(); }
            catch (Exception e) { Log.w(TAG, "Error releasing predictive system", e); }
            predictiveSystem = null;
        }

        executorService.shutdownNow();
        scheduler.shutdownNow();
        listeners.clear();
        actionHandlers.clear();
        actionSuccessCount.clear();
        actionTotalCount.clear();

        initialized = false;
        gameMode    = false;
        currentMode = Mode.INACTIVE;

        synchronized (AIController.class) { instance = null; }
        Log.i(TAG, "AIController released");
    }
}
