package com.aiassistant.learning;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Extracts contextual information from the device
 * This is used by the learning system to capture the state
 * during which actions are performed.
 */
public class ContextExtractor implements SensorEventListener {
    private static final String TAG = "ContextExtractor";
    
    private Context context;
    private SensorManager sensorManager;
    private LocationManager locationManager;
    private PowerManager powerManager;
    private ConnectivityManager connectivityManager;
    
    // Sensor data
    private float[] accelerometerValues = new float[3];
    private float[] gyroscopeValues = new float[3];
    private float[] lightValues = new float[1];
    private Location lastLocation;
    
    public ContextExtractor(Context context) {
        this.context = context;
        
        // Initialize managers
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        // Register sensor listeners if available
        registerSensors();
        
        // Register location listener if available
        registerLocationListener();
    }
    
    /**
     * Register sensor listeners
     */
    private void registerSensors() {
        try {
            // Accelerometer
            Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            }
            
            // Gyroscope
            Sensor gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            if (gyroscope != null) {
                sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
            }
            
            // Light sensor
            Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            if (lightSensor != null) {
                sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error registering sensors", e);
        }
    }
    
    /**
     * Register location listener
     */
    private void registerLocationListener() {
        try {
            // Location updates
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        5 * 60 * 1000, // 5 minutes
                        100, // 100 meters
                        new LocationListener() {
                            @Override
                            public void onLocationChanged(Location location) {
                                lastLocation = location;
                            }
                            
                            @Override
                            public void onStatusChanged(String provider, int status, Bundle extras) {}
                            
                            @Override
                            public void onProviderEnabled(String provider) {}
                            
                            @Override
                            public void onProviderDisabled(String provider) {}
                        }
                );
            }
        } catch (SecurityException e) {
            Log.e(TAG, "No permission for location", e);
        } catch (Exception e) {
            Log.e(TAG, "Error registering location listener", e);
        }
    }
    
    /**
     * Extract current context from the device
     */
    public Map<String, Object> extractContext() {
        Map<String, Object> context = new HashMap<>();
        
        // Time context
        addTimeContext(context);
        
        // Device context
        addDeviceContext(context);
        
        // Activity context
        addActivityContext(context);
        
        // Location context (if available)
        addLocationContext(context);
        
        return context;
    }
    
    /**
     * Add time-related context
     */
    private void addTimeContext(Map<String, Object> context) {
        Calendar calendar = Calendar.getInstance();
        
        // Current time
        context.put("hour", calendar.get(Calendar.HOUR_OF_DAY));
        context.put("minute", calendar.get(Calendar.MINUTE));
        context.put("dayOfWeek", calendar.get(Calendar.DAY_OF_WEEK));
        
        // Time categories
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        String timeOfDay;
        
        if (hour >= 5 && hour < 12) {
            timeOfDay = "morning";
        } else if (hour >= 12 && hour < 17) {
            timeOfDay = "afternoon";
        } else if (hour >= 17 && hour < 21) {
            timeOfDay = "evening";
        } else {
            timeOfDay = "night";
        }
        
        context.put("timeOfDay", timeOfDay);
        
        // Weekend or weekday
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        boolean isWeekend = (day == Calendar.SATURDAY || day == Calendar.SUNDAY);
        context.put("isWeekend", isWeekend);
    }
    
    /**
     * Add device-related context
     */
    private void addDeviceContext(Map<String, Object> context) {
        // Battery level
        try {
            BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            if (batteryManager != null) {
                int batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                context.put("batteryLevel", batteryLevel);
                
                if (batteryLevel <= 20) {
                    context.put("batteryState", "low");
                } else if (batteryLevel <= 40) {
                    context.put("batteryState", "medium");
                } else {
                    context.put("batteryState", "high");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting battery info", e);
        }
        
        // Screen state
        try {
            boolean isScreenOn = powerManager.isInteractive();
            context.put("screenState", isScreenOn ? "on" : "off");
        } catch (Exception e) {
            Log.e(TAG, "Error getting screen state", e);
        }
        
        // Network connectivity
        try {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            String networkType = "none";
            
            if (isConnected) {
                networkType = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI ? "wifi" : "mobile";
            }
            
            context.put("networkConnected", isConnected);
            context.put("networkType", networkType);
        } catch (Exception e) {
            Log.e(TAG, "Error getting network state", e);
        }
        
        // Light level (from sensor)
        if (lightValues[0] > 0) {
            float light = lightValues[0];
            String lightLevel;
            
            if (light < 50) {
                lightLevel = "dark";
            } else if (light < 5000) {
                lightLevel = "indoor";
            } else {
                lightLevel = "bright";
            }
            
            context.put("lightLevel", lightLevel);
        }
    }
    
    /**
     * Add activity-related context
     */
    private void addActivityContext(Map<String, Object> context) {
        // Basic activity detection based on accelerometer
        float magnitude = getMagnitude(accelerometerValues);
        String activityType;
        
        if (magnitude < 0.5) {
            activityType = "still";
        } else if (magnitude < 2.0) {
            activityType = "walking";
        } else {
            activityType = "active";
        }
        
        context.put("activityType", activityType);
        context.put("deviceMotion", magnitude);
    }
    
    /**
     * Add location-related context
     */
    private void addLocationContext(Map<String, Object> context) {
        if (lastLocation != null) {
            context.put("latitude", lastLocation.getLatitude());
            context.put("longitude", lastLocation.getLongitude());
            
            // In a real app, would resolve to place names, etc.
        }
    }
    
    /**
     * Calculate magnitude of vector
     */
    private float getMagnitude(float[] vector) {
        return (float) Math.sqrt(
                vector[0] * vector[0] +
                vector[1] * vector[1] +
                vector[2] * vector[2]
        );
    }
    
    /**
     * Clean up resources
     */
    public void destroy() {
        // Unregister sensor listener
        sensorManager.unregisterListener(this);
        
        // Unregister location listener
        try {
            locationManager.removeUpdates(new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {}
                
                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}
                
                @Override
                public void onProviderEnabled(String provider) {}
                
                @Override
                public void onProviderDisabled(String provider) {}
            });
        } catch (SecurityException e) {
            Log.e(TAG, "No permission to remove location updates", e);
        } catch (Exception e) {
            Log.e(TAG, "Error removing location updates", e);
        }
    }
    
    // SensorEventListener implementation
    
    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(event.values, 0, accelerometerValues, 0, 3);
                break;
            case Sensor.TYPE_GYROSCOPE:
                System.arraycopy(event.values, 0, gyroscopeValues, 0, 3);
                break;
            case Sensor.TYPE_LIGHT:
                System.arraycopy(event.values, 0, lightValues, 0, 1);
                break;
        }
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }
}