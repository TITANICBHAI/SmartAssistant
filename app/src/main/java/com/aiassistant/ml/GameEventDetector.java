package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * GameEventDetector — detects significant game events by comparing consecutive
 * game-state snapshots.
 *
 * Events detected:
 *   SCORE_CHANGE       — score delta ≠ 0
 *   HEALTH_DECREASE    — health dropped (player was hit)
 *   HEALTH_INCREASE    — health increased (heal / pickup)
 *   LEVEL_UP           — level field increased
 *   ENEMY_DEFEATED     — enemy count decreased
 *   RESOURCE_COLLECTED — resource count decreased (collected) or score jumped
 *   GAME_OVER          — health ≤ 0 or explicit "gameOver" flag
 *   STAGE_COMPLETE     — level changed or "stageComplete" flag
 *   BOSS_APPEARED      — "bossPresent" flag newly true
 *   SPECIAL_CHARGED    — "specialReady" flag newly true
 *   IDLE_DETECTED      — no state change for N consecutive frames (stuck detector)
 *
 * Integration:
 *   Called from PerceptionEngine or AIController once per frame with the latest
 *   decoded game-state map.  Fires EventListener callbacks on each detected event.
 *   Events are also persisted in a rolling history for analysis.
 */
public class GameEventDetector {

    private static final String TAG = "GameEventDetector";

    // -------------------------------------------------------------------------
    // Event type enum
    // -------------------------------------------------------------------------
    public enum EventType {
        SCORE_CHANGE,
        HEALTH_DECREASE,
        HEALTH_INCREASE,
        LEVEL_UP,
        ENEMY_DEFEATED,
        RESOURCE_COLLECTED,
        GAME_OVER,
        STAGE_COMPLETE,
        BOSS_APPEARED,
        SPECIAL_CHARGED,
        IDLE_DETECTED,
        UNKNOWN
    }

    // -------------------------------------------------------------------------
    // Event record
    // -------------------------------------------------------------------------
    public static class GameEvent {
        public final EventType          type;
        public final long               timestamp;
        public final Map<String, Object> context; // relevant state fields at time of event
        public final float              magnitude; // how large the change was (normalized)

        public GameEvent(EventType type, Map<String, Object> context, float magnitude) {
            this.type      = type;
            this.timestamp = System.currentTimeMillis();
            this.context   = new HashMap<>(context);
            this.magnitude = magnitude;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new HashMap<>();
            m.put("type",      type.name());
            m.put("timestamp", timestamp);
            m.put("magnitude", magnitude);
            m.put("context",   new HashMap<>(context));
            return m;
        }

        @Override
        public String toString() {
            return "GameEvent{" + type + " @" + timestamp
                    + " mag=" + String.format("%.2f", magnitude) + "}";
        }
    }

    // -------------------------------------------------------------------------
    // Listener
    // -------------------------------------------------------------------------
    public interface EventListener {
        void onGameEvent(GameEvent event);
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final List<EventListener> listeners     = new CopyOnWriteArrayList<>();
    private final List<GameEvent>     eventHistory  = new ArrayList<>();
    private static final int          MAX_HISTORY   = 500;

    private Map<String, Object> previousState = null;
    private final AtomicInteger framesSinceChange = new AtomicInteger(0);
    private final int           idleThreshold;    // frames before IDLE_DETECTED fires

    private final AtomicInteger totalEvents = new AtomicInteger(0);
    private final AtomicLong    lastEventMs = new AtomicLong(0L);

    // Per-event counts
    private final Map<EventType, AtomicInteger> eventCounts = new HashMap<>();

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    public GameEventDetector(int idleThresholdFrames) {
        this.idleThreshold = idleThresholdFrames;
        for (EventType t : EventType.values()) {
            eventCounts.put(t, new AtomicInteger(0));
        }
    }

    public GameEventDetector() {
        this(30); // 30 frames ≈ 1 second at 30 fps
    }

    // -------------------------------------------------------------------------
    // Listener management
    // -------------------------------------------------------------------------

    public void addListener(EventListener l)    { listeners.add(l); }
    public void removeListener(EventListener l) { listeners.remove(l); }

    // -------------------------------------------------------------------------
    // Main API
    // -------------------------------------------------------------------------

    /**
     * Process a new game-state snapshot and fire events for any detected changes.
     *
     * @param state Current game state map.
     * @return List of events detected in this frame (may be empty).
     */
    public List<GameEvent> processState(Map<String, Object> state) {
        List<GameEvent> detected = new ArrayList<>();
        if (state == null) return detected;

        if (previousState == null) {
            previousState = new HashMap<>(state);
            return detected;
        }

        boolean anyChange = false;

        // ---------- Score change ----------
        float scoreDelta = floatDelta(state, previousState, "score");
        if (Math.abs(scoreDelta) > 0.5f) {
            detected.add(fire(EventType.SCORE_CHANGE, state, Math.abs(scoreDelta) / 1000f));
            anyChange = true;
        }

        // ---------- Health changes ----------
        float healthDelta = floatDelta(state, previousState, "health");
        if (healthDelta < -0.01f) {
            detected.add(fire(EventType.HEALTH_DECREASE, state, -healthDelta));
            anyChange = true;
        } else if (healthDelta > 0.01f) {
            detected.add(fire(EventType.HEALTH_INCREASE, state, healthDelta));
            anyChange = true;
        }

        // ---------- Game over ----------
        boolean isGameOver = getBoolean(state, "gameOver")
                || getFloat(state, "health") <= 0f && previousState.containsKey("health");
        if (isGameOver && !getBoolean(previousState, "gameOver")) {
            detected.add(fire(EventType.GAME_OVER, state, 1.0f));
            anyChange = true;
        }

        // ---------- Level up ----------
        float levelDelta = floatDelta(state, previousState, "level");
        if (levelDelta > 0.5f) {
            detected.add(fire(EventType.LEVEL_UP, state, levelDelta));
            anyChange = true;
        }

        // ---------- Stage complete ----------
        boolean stageComplete = getBoolean(state, "stageComplete");
        if (stageComplete && !getBoolean(previousState, "stageComplete")) {
            detected.add(fire(EventType.STAGE_COMPLETE, state, 1.0f));
            anyChange = true;
        }

        // ---------- Enemy defeated ----------
        float enemyDelta = floatDelta(state, previousState, "enemyCount");
        if (enemyDelta < -0.5f) {
            detected.add(fire(EventType.ENEMY_DEFEATED, state, Math.abs(enemyDelta) / 10f));
            anyChange = true;
        }

        // ---------- Resource collected ----------
        float resDelta = floatDelta(state, previousState, "resourceCount");
        if (resDelta < -0.5f) {
            detected.add(fire(EventType.RESOURCE_COLLECTED, state, Math.abs(resDelta) / 5f));
            anyChange = true;
        }

        // ---------- Boss appeared ----------
        boolean bossNow  = getBoolean(state,         "bossPresent");
        boolean bossPrev = getBoolean(previousState, "bossPresent");
        if (bossNow && !bossPrev) {
            detected.add(fire(EventType.BOSS_APPEARED, state, 1.0f));
            anyChange = true;
        }

        // ---------- Special charged ----------
        boolean specialNow  = getBoolean(state,         "specialReady");
        boolean specialPrev = getBoolean(previousState, "specialReady");
        if (specialNow && !specialPrev) {
            detected.add(fire(EventType.SPECIAL_CHARGED, state, 1.0f));
            anyChange = true;
        }

        // ---------- Idle detection ----------
        if (anyChange) {
            framesSinceChange.set(0);
        } else {
            int idle = framesSinceChange.incrementAndGet();
            if (idle == idleThreshold) {
                detected.add(fire(EventType.IDLE_DETECTED, state, 1.0f));
            }
        }

        previousState = new HashMap<>(state);
        return detected;
    }

    /** Reset internal state (call on episode/level restart). */
    public synchronized void reset() {
        previousState = null;
        framesSinceChange.set(0);
    }

    // -------------------------------------------------------------------------
    // History / stats
    // -------------------------------------------------------------------------

    public synchronized List<GameEvent> getHistory() {
        return new ArrayList<>(eventHistory);
    }

    public synchronized List<GameEvent> getHistoryByType(EventType type) {
        List<GameEvent> result = new ArrayList<>();
        for (GameEvent e : eventHistory) if (e.type == type) result.add(e);
        return result;
    }

    public Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("totalEvents",     totalEvents.get());
        s.put("lastEventMs",     lastEventMs.get());
        s.put("historySize",     eventHistory.size());
        s.put("idleThreshold",   idleThreshold);
        Map<String, Integer> counts = new HashMap<>();
        for (Map.Entry<EventType, AtomicInteger> e : eventCounts.entrySet()) {
            counts.put(e.getKey().name(), e.getValue().get());
        }
        s.put("eventCounts", counts);
        return s;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private GameEvent fire(EventType type, Map<String, Object> state, float magnitude) {
        GameEvent event = new GameEvent(type, state, magnitude);

        // Update counters
        totalEvents.incrementAndGet();
        lastEventMs.set(event.timestamp);
        AtomicInteger cnt = eventCounts.get(type);
        if (cnt != null) cnt.incrementAndGet();

        // Persist to history
        synchronized (this) {
            if (eventHistory.size() >= MAX_HISTORY) eventHistory.remove(0);
            eventHistory.add(event);
        }

        // Notify listeners
        for (EventListener l : listeners) {
            try { l.onGameEvent(event); }
            catch (Exception ex) { Log.w(TAG, "Listener error: " + ex.getMessage()); }
        }

        Log.d(TAG, "Event: " + event);
        return event;
    }

    private static float floatDelta(Map<String, Object> curr, Map<String, Object> prev, String key) {
        return getFloat(curr, key) - getFloat(prev, key);
    }

    private static float getFloat(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof Number) return ((Number) v).floatValue();
        if (v instanceof String) { try { return Float.parseFloat((String)v); } catch (NumberFormatException e) {} }
        return 0f;
    }

    private static boolean getBoolean(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof Number)  return ((Number)v).floatValue() > 0f;
        if (v instanceof String)  return "true".equalsIgnoreCase((String)v) || "1".equals(v);
        return false;
    }
}
