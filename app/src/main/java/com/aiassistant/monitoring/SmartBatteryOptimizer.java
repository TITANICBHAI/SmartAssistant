package com.aiassistant.monitoring;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Battery-aware AI throttle controller.
 *
 * Registers a BroadcastReceiver for ACTION_BATTERY_CHANGED to track the
 * real-time battery level and charging status.  Based on configurable
 * thresholds it exposes {@link #shouldThrottle()} and
 * {@link #isCriticalBattery()} so callers can reduce AI workload or skip
 * expensive inference cycles when the device is low on power.
 *
 * A manual override flag ({@link #setLowPowerOverride(boolean)}) lets the
 * user or AIBackgroundService force the throttle regardless of actual level.
 */
public class SmartBatteryOptimizer {

    private static final String TAG = "SmartBatteryOptimizer";

    // -----------------------------------------------------------------------
    // Thresholds (all percentages, 0–100)
    // -----------------------------------------------------------------------
    private static final int CRITICAL_BATTERY = 10;
    private static final int LOW_BATTERY      = 20;
    private static final int WARN_BATTERY     = 30;

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    private final Context      context;
    private final AtomicBoolean registered = new AtomicBoolean(false);
    private volatile boolean    lowPowerOverride = false;

    private volatile int     batteryLevel   = 100;
    private volatile boolean isCharging     = false;
    private volatile boolean isPowerSaveMode = false;
    private volatile int     health         = BatteryManager.BATTERY_HEALTH_GOOD;

    private final CopyOnWriteArrayList<BatteryListener> listeners = new CopyOnWriteArrayList<>();

    // -----------------------------------------------------------------------
    // Listener interface
    // -----------------------------------------------------------------------

    public interface BatteryListener {
        void onBatteryStateChanged(int level, boolean charging, boolean throttle);
    }

    // -----------------------------------------------------------------------
    // BroadcastReceiver
    // -----------------------------------------------------------------------

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            if (!Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) return;
            int rawLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale    = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            batteryLevel  = scale > 0 ? (int) (100f * rawLevel / scale) : rawLevel;
            int status    = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                    BatteryManager.BATTERY_STATUS_UNKNOWN);
            isCharging    = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL;
            health        = intent.getIntExtra(BatteryManager.EXTRA_HEALTH,
                    BatteryManager.BATTERY_HEALTH_GOOD);
            checkPowerSaveMode();
            Log.d(TAG, "Battery: " + batteryLevel + "%, charging=" + isCharging +
                    ", powerSave=" + isPowerSaveMode);
            notifyListeners();
        }
    };

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public SmartBatteryOptimizer(Context context) {
        this.context = context.getApplicationContext();
        register();
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    private void register() {
        if (!registered.compareAndSet(false, true)) return;
        // Sticky broadcast — register and get last value immediately
        Intent sticky = context.registerReceiver(receiver,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (sticky != null) receiver.onReceive(context, sticky);
        Log.i(TAG, "SmartBatteryOptimizer registered (level=" + batteryLevel + "%)");
    }

    public void release() {
        if (!registered.compareAndSet(true, false)) return;
        try { context.unregisterReceiver(receiver); }
        catch (IllegalArgumentException ignored) {}
        Log.i(TAG, "SmartBatteryOptimizer released");
    }

    // -----------------------------------------------------------------------
    // Override
    // -----------------------------------------------------------------------

    /** When true, always throttle regardless of actual battery level. */
    public void setLowPowerOverride(boolean override) {
        this.lowPowerOverride = override;
    }

    // -----------------------------------------------------------------------
    // Public state API
    // -----------------------------------------------------------------------

    public int     getBatteryLevel()    { return batteryLevel; }
    public boolean isCharging()         { return isCharging; }
    public boolean isPowerSaveMode()    { return isPowerSaveMode; }

    /** Critical: ≤10 % and not charging — stop all non-essential AI work. */
    public boolean isCriticalBattery() {
        return !isCharging && batteryLevel <= CRITICAL_BATTERY;
    }

    /** Low battery: ≤20 % and not charging — reduce AI polling frequency. */
    public boolean isLowBattery() {
        return !isCharging && batteryLevel <= LOW_BATTERY;
    }

    /**
     * Returns true when the AI should throttle:
     *  - User has enabled low-power override, OR
     *  - Device is in system power-save mode, OR
     *  - Battery is at or below the WARN threshold and not charging.
     */
    public boolean shouldThrottle() {
        return lowPowerOverride
                || isPowerSaveMode
                || (!isCharging && batteryLevel <= WARN_BATTERY);
    }

    /**
     * Suggested polling multiplier based on battery state.
     * Callers can multiply their base interval by this value.
     *  1 = normal, 2 = half speed, 5 = very slow, 10 = almost stopped.
     */
    public int getThrottleMultiplier() {
        if (isCriticalBattery()) return 10;
        if (isLowBattery())      return 5;
        if (shouldThrottle())    return 2;
        return 1;
    }

    // -----------------------------------------------------------------------
    // Listener management
    // -----------------------------------------------------------------------

    public void addListener(BatteryListener l) {
        if (l != null && !listeners.contains(l)) listeners.add(l);
    }

    public void removeListener(BatteryListener l) {
        listeners.remove(l);
    }

    // -----------------------------------------------------------------------
    // Internal
    // -----------------------------------------------------------------------

    private void checkPowerSaveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            isPowerSaveMode = pm != null && pm.isPowerSaveMode();
        }
    }

    private void notifyListeners() {
        boolean throttle = shouldThrottle();
        for (BatteryListener l : listeners) {
            try { l.onBatteryStateChanged(batteryLevel, isCharging, throttle); }
            catch (Exception e) { Log.w(TAG, "Listener error", e); }
        }
    }
}
