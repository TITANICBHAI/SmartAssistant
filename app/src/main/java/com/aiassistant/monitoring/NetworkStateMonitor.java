package com.aiassistant.monitoring;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Monitors network connectivity so AI subsystems can adapt their behaviour
 * when the device goes offline or switches between WiFi and mobile data.
 *
 * Uses ConnectivityManager.NetworkCallback on API ≥ 21 for accurate,
 * real-time notifications. Falls back to a legacy BroadcastReceiver for
 * older devices.
 *
 * Usage:
 * <pre>
 *   NetworkStateMonitor m = new NetworkStateMonitor(context);
 *   m.addListener(state -> { if (!state.isConnected()) pauseCloudSync(); });
 *   m.start();
 *   // ... later ...
 *   m.stop();
 * </pre>
 */
public class NetworkStateMonitor {

    private static final String TAG = "NetworkStateMonitor";

    // -----------------------------------------------------------------------
    // State model
    // -----------------------------------------------------------------------

    public enum ConnectionType { NONE, WIFI, CELLULAR, ETHERNET, OTHER }

    public static class NetworkState {
        public final boolean        isConnected;
        public final ConnectionType type;
        public final boolean        isMetered;
        public final boolean        hasInternet;

        public NetworkState(boolean isConnected, ConnectionType type,
                            boolean isMetered, boolean hasInternet) {
            this.isConnected = isConnected;
            this.type        = type;
            this.isMetered   = isMetered;
            this.hasInternet = hasInternet;
        }

        @Override public String toString() {
            return "NetworkState{connected=" + isConnected +
                    ", type=" + type + ", metered=" + isMetered + "}";
        }
    }

    // -----------------------------------------------------------------------
    // Listener
    // -----------------------------------------------------------------------

    public interface NetworkStateListener {
        void onNetworkStateChanged(NetworkState state);
    }

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    private final Context              context;
    private final ConnectivityManager  cm;
    private final AtomicBoolean        running = new AtomicBoolean(false);
    private final CopyOnWriteArrayList<NetworkStateListener> listeners = new CopyOnWriteArrayList<>();

    private volatile NetworkState currentState =
            new NetworkState(false, ConnectionType.NONE, false, false);

    // Modern callback (API 21+)
    private ConnectivityManager.NetworkCallback networkCallback;

    // Legacy fallback receiver (API < 21)
    private BroadcastReceiver legacyReceiver;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public NetworkStateMonitor(Context context) {
        this.context = context.getApplicationContext();
        this.cm      = (ConnectivityManager)
                this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    public void start() {
        if (!running.compareAndSet(false, true)) return;

        // Take initial snapshot
        currentState = buildState();
        Log.i(TAG, "Initial network state: " + currentState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override public void onAvailable(Network network) {
                    update();
                }
                @Override public void onLost(Network network) {
                    update();
                }
                @Override public void onCapabilitiesChanged(Network network,
                                                            NetworkCapabilities caps) {
                    update();
                }
            };
            NetworkRequest req = new NetworkRequest.Builder().build();
            cm.registerNetworkCallback(req, networkCallback);
        } else {
            // Legacy BroadcastReceiver
            legacyReceiver = new BroadcastReceiver() {
                @Override public void onReceive(Context ctx, Intent intent) { update(); }
            };
            context.registerReceiver(legacyReceiver,
                    new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }

        Log.i(TAG, "NetworkStateMonitor started");
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                    && networkCallback != null) {
                cm.unregisterNetworkCallback(networkCallback);
                networkCallback = null;
            } else if (legacyReceiver != null) {
                context.unregisterReceiver(legacyReceiver);
                legacyReceiver = null;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error stopping NetworkStateMonitor", e);
        }
        Log.i(TAG, "NetworkStateMonitor stopped");
    }

    // -----------------------------------------------------------------------
    // Listener management
    // -----------------------------------------------------------------------

    public void addListener(NetworkStateListener l) {
        if (l != null && !listeners.contains(l)) listeners.add(l);
    }

    public void removeListener(NetworkStateListener l) {
        listeners.remove(l);
    }

    // -----------------------------------------------------------------------
    // State read
    // -----------------------------------------------------------------------

    public NetworkState getCurrentState() { return currentState; }
    public boolean isConnected()          { return currentState.isConnected; }
    public boolean isWifi()               { return currentState.type == ConnectionType.WIFI; }
    public boolean isCellular()           { return currentState.type == ConnectionType.CELLULAR; }
    public boolean isMetered()            { return currentState.isMetered; }

    // -----------------------------------------------------------------------
    // Internal
    // -----------------------------------------------------------------------

    private void update() {
        NetworkState newState = buildState();
        if (newState.isConnected != currentState.isConnected ||
                newState.type != currentState.type ||
                newState.isMetered != currentState.isMetered) {
            currentState = newState;
            Log.d(TAG, "Network state changed: " + newState);
            notifyListeners(newState);
        }
    }

    private NetworkState buildState() {
        if (cm == null)
            return new NetworkState(false, ConnectionType.NONE, false, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network active = cm.getActiveNetwork();
            if (active == null)
                return new NetworkState(false, ConnectionType.NONE, false, false);

            NetworkCapabilities caps = cm.getNetworkCapabilities(active);
            if (caps == null)
                return new NetworkState(false, ConnectionType.NONE, false, false);

            boolean internet  = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            boolean validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            boolean metered   = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);

            ConnectionType type = ConnectionType.OTHER;
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
                type = ConnectionType.WIFI;
            else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                type = ConnectionType.CELLULAR;
            else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
                type = ConnectionType.ETHERNET;

            return new NetworkState(internet, type, metered, validated);
        } else {
            // Legacy
            android.net.NetworkInfo info = cm.getActiveNetworkInfo();
            if (info == null || !info.isConnected())
                return new NetworkState(false, ConnectionType.NONE, false, false);
            ConnectionType type;
            switch (info.getType()) {
                case ConnectivityManager.TYPE_WIFI:   type = ConnectionType.WIFI;     break;
                case ConnectivityManager.TYPE_MOBILE: type = ConnectionType.CELLULAR; break;
                default:                              type = ConnectionType.OTHER;     break;
            }
            return new NetworkState(true, type, type == ConnectionType.CELLULAR, true);
        }
    }

    private void notifyListeners(NetworkState state) {
        for (NetworkStateListener l : listeners) {
            try { l.onNetworkStateChanged(state); }
            catch (Exception e) { Log.w(TAG, "Listener error", e); }
        }
    }
}
