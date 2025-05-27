package com.aiassistant.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import android.util.LruCache;

import models.AppInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Detects apps from screenshots or images
 * Implementation of AppDetector from app_detector.py
 */
public class AppDetector {
    private static final String TAG = "AppDetector";
    
    private Context context;
    private PackageManager packageManager;
    private float confidenceThreshold;
    private Map<String, AppSignature> appSignatures;
    private List<AppInfo> installedApps;
    private LruCache<String, AppDetectionResult> detectionCache;
    
    // Default detection cache size (items)
    private static final int DEFAULT_CACHE_SIZE = 100;
    
    /**
     * Initialize app detector
     */
    public AppDetector(Context context) {
        this(context, 0.6f);
    }
    
    /**
     * Initialize app detector with custom confidence threshold
     */
    public AppDetector(Context context, float confidenceThreshold) {
        this.context = context;
        this.packageManager = context.getPackageManager();
        this.confidenceThreshold = confidenceThreshold;
        this.appSignatures = new HashMap<>();
        this.installedApps = new ArrayList<>();
        
        // Initialize detection cache
        detectionCache = new LruCache<>(DEFAULT_CACHE_SIZE);
        
        // Load installed apps and their signatures
        loadInstalledApps();
    }
    
    /**
     * Detect app from bitmap
     * 
     * @param bitmap Bitmap to analyze for app detection
     * @return Detected app info or null if no app is detected with sufficient confidence
     */
    public AppInfo detectApp(Bitmap bitmap) {
        if (bitmap == null) {
            Log.e(TAG, "Cannot detect app from null bitmap");
            return null;
        }
        
        // Create a downsized bitmap for faster processing
        Bitmap processedBitmap = Bitmap.createScaledBitmap(
                bitmap, 
                Math.min(bitmap.getWidth(), 300), 
                Math.min(bitmap.getHeight(), 300), 
                true);
        
        // Generate a simple hash of the bitmap for caching
        String bitmapHash = generateBitmapHash(processedBitmap);
        
        // Check cache first
        AppDetectionResult cachedResult = detectionCache.get(bitmapHash);
        if (cachedResult != null) {
            return cachedResult.confidence >= confidenceThreshold ? 
                   cachedResult.appInfo : null;
        }
        
        // Extract bitmap features
        BitmapFeatures features = extractFeatures(processedBitmap);
        
        // Find the best matching app
        AppDetectionResult bestMatch = findBestMatch(features);
        
        // Cache the result
        detectionCache.put(bitmapHash, bestMatch);
        
        // Return result if confidence is above threshold
        return bestMatch.confidence >= confidenceThreshold ? 
               bestMatch.appInfo : null;
    }
    
    /**
     * Detect app with detailed results
     * 
     * @param bitmap Bitmap to analyze
     * @return Detection result with confidence score
     */
    public AppDetectionResult detectAppWithConfidence(Bitmap bitmap) {
        if (bitmap == null) {
            Log.e(TAG, "Cannot detect app from null bitmap");
            return new AppDetectionResult(null, 0.0f);
        }
        
        // Create a downsized bitmap for faster processing
        Bitmap processedBitmap = Bitmap.createScaledBitmap(
                bitmap, 
                Math.min(bitmap.getWidth(), 300), 
                Math.min(bitmap.getHeight(), 300), 
                true);
        
        // Generate a simple hash of the bitmap for caching
        String bitmapHash = generateBitmapHash(processedBitmap);
        
        // Check cache first
        AppDetectionResult cachedResult = detectionCache.get(bitmapHash);
        if (cachedResult != null) {
            return cachedResult;
        }
        
        // Extract bitmap features
        BitmapFeatures features = extractFeatures(processedBitmap);
        
        // Find the best matching app
        AppDetectionResult bestMatch = findBestMatch(features);
        
        // Cache the result
        detectionCache.put(bitmapHash, bestMatch);
        
        return bestMatch;
    }
    
    /**
     * Get installed apps
     */
    public List<AppInfo> getInstalledApps() {
        return installedApps;
    }
    
    /**
     * Set confidence threshold for app detection
     * 
     * @param threshold Threshold value (0.0-1.0)
     */
    public void setConfidenceThreshold(float threshold) {
        this.confidenceThreshold = Math.max(0.0f, Math.min(1.0f, threshold));
    }
    
    /**
     * Check if app detector is ready
     */
    public boolean isReady() {
        return !appSignatures.isEmpty();
    }
    
    /**
     * Load installed apps and extract their signatures
     */
    private void loadInstalledApps() {
        Log.d(TAG, "Loading installed apps...");
        
        List<ApplicationInfo> applications = packageManager.getInstalledApplications(
                PackageManager.GET_META_DATA);
        
        for (ApplicationInfo appInfo : applications) {
            try {
                // Skip system apps that aren't in launcher
                if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                    // Unless they have a launcher intent
                    if (packageManager.getLaunchIntentForPackage(appInfo.packageName) == null) {
                        continue;
                    }
                }
                
                String packageName = appInfo.packageName;
                String appName = packageManager.getApplicationLabel(appInfo).toString();
                
                // Create app info object
                AppInfo app = new AppInfo(
                        packageName,
                        appName,
                        packageManager.getApplicationIcon(appInfo),
                        getAppCategory(appInfo),
                        (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0,
                        0,  // Will be set later if needed
                        0   // Will be set later if needed
                );
                
                // Add to installed apps list
                installedApps.add(app);
                
                // Extract app signature (not implemented in this simplified version)
                appSignatures.put(packageName, new AppSignature(packageName, appName));
                
            } catch (Exception e) {
                Log.e(TAG, "Error loading app: " + appInfo.packageName, e);
            }
        }
        
        // Sort apps by name
        Collections.sort(installedApps, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo a1, AppInfo a2) {
                return a1.getAppName().compareToIgnoreCase(a2.getAppName());
            }
        });
        
        Log.d(TAG, "Loaded " + installedApps.size() + " apps");
    }
    
    /**
     * Get app category (simplified version)
     */
    private String getAppCategory(ApplicationInfo appInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int category = appInfo.category;
            switch (category) {
                case ApplicationInfo.CATEGORY_GAME:
                    return "Game";
                case ApplicationInfo.CATEGORY_AUDIO:
                    return "Audio";
                case ApplicationInfo.CATEGORY_VIDEO:
                    return "Video";
                case ApplicationInfo.CATEGORY_IMAGE:
                    return "Image";
                case ApplicationInfo.CATEGORY_SOCIAL:
                    return "Social";
                case ApplicationInfo.CATEGORY_NEWS:
                    return "News";
                case ApplicationInfo.CATEGORY_MAPS:
                    return "Maps";
                case ApplicationInfo.CATEGORY_PRODUCTIVITY:
                    return "Productivity";
                default:
                    return "Other";
            }
        }
        return "Unknown";
    }
    
    /**
     * Extract features from bitmap for app detection
     */
    private BitmapFeatures extractFeatures(Bitmap bitmap) {
        // This is a simplified implementation that focuses on color patterns
        // A real implementation would use more sophisticated app detection algorithms
        
        BitmapFeatures features = new BitmapFeatures();
        
        // Extract color histogram
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        // Sample grid of 10x10 points across the image
        int gridSize = 10;
        int xStep = width / gridSize;
        int yStep = height / gridSize;
        
        for (int y = 0; y < gridSize; y++) {
            for (int x = 0; x < gridSize; x++) {
                int pixelX = x * xStep + xStep / 2;
                int pixelY = y * yStep + yStep / 2;
                
                if (pixelX < width && pixelY < height) {
                    int pixel = bitmap.getPixel(pixelX, pixelY);
                    features.colorSamples.add(pixel);
                    
                    // Update color statistics
                    features.redSum += Color.red(pixel);
                    features.greenSum += Color.green(pixel);
                    features.blueSum += Color.blue(pixel);
                    
                    // Track dominant colors
                    int r = Color.red(pixel);
                    int g = Color.green(pixel);
                    int b = Color.blue(pixel);
                    
                    String colorBin = getColorBin(r, g, b);
                    Integer count = features.colorHistogram.get(colorBin);
                    features.colorHistogram.put(colorBin, count != null ? count + 1 : 1);
                }
            }
        }
        
        // Calculate average colors
        int sampleCount = features.colorSamples.size();
        if (sampleCount > 0) {
            features.avgRed = features.redSum / sampleCount;
            features.avgGreen = features.greenSum / sampleCount;
            features.avgBlue = features.blueSum / sampleCount;
        }
        
        return features;
    }
    
    /**
     * Generate a color bin key for histogram
     */
    private String getColorBin(int r, int g, int b) {
        // Simplify color to 5 levels per channel
        int rBin = r / 51;  // 51 = 255/5
        int gBin = g / 51;
        int bBin = b / 51;
        
        return rBin + "," + gBin + "," + bBin;
    }
    
    /**
     * Find best matching app for bitmap features
     */
    private AppDetectionResult findBestMatch(BitmapFeatures features) {
        // In a real implementation, this would compare the extracted features
        // against known app signatures. This is a simplified placeholder.
        
        // For now, return a random app with random confidence (for testing only)
        if (!installedApps.isEmpty()) {
            // Select random app for testing 
            // THIS IS JUST A PLACEHOLDER - would need real implementation
            int randomIndex = (int) (Math.random() * installedApps.size());
            AppInfo randomApp = installedApps.get(randomIndex);
            
            // Random confidence between 0.4 and 0.9
            float randomConfidence = 0.4f + (float) Math.random() * 0.5f;
            
            return new AppDetectionResult(randomApp, randomConfidence);
        }
        
        return new AppDetectionResult(null, 0.0f);
    }
    
    /**
     * Generate a simple hash for a bitmap for caching
     */
    private String generateBitmapHash(Bitmap bitmap) {
        // Simple hash based on sampling a few pixels and average color
        // A real implementation would use a more robust hashing algorithm
        
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        int[] samplePoints = {
            bitmap.getPixel(width / 4, height / 4),
            bitmap.getPixel(width / 2, height / 2),
            bitmap.getPixel(3 * width / 4, 3 * height / 4),
            bitmap.getPixel(width / 4, 3 * height / 4),
            bitmap.getPixel(3 * width / 4, height / 4)
        };
        
        StringBuilder hash = new StringBuilder();
        hash.append(width).append("x").append(height).append("_");
        
        int redSum = 0, greenSum = 0, blueSum = 0;
        for (int pixel : samplePoints) {
            redSum += Color.red(pixel);
            greenSum += Color.green(pixel);
            blueSum += Color.blue(pixel);
        }
        
        int avgRed = redSum / samplePoints.length;
        int avgGreen = greenSum / samplePoints.length;
        int avgBlue = blueSum / samplePoints.length;
        
        hash.append(avgRed).append("_")
            .append(avgGreen).append("_")
            .append(avgBlue);
        
        return hash.toString();
    }
    
    /**
     * Features extracted from a bitmap
     */
    private static class BitmapFeatures {
        List<Integer> colorSamples = new ArrayList<>();
        Map<String, Integer> colorHistogram = new HashMap<>();
        
        int redSum = 0;
        int greenSum = 0;
        int blueSum = 0;
        
        int avgRed = 0;
        int avgGreen = 0;
        int avgBlue = 0;
    }
    
    /**
     * App signature for detection
     */
    private static class AppSignature {
        String packageName;
        String appName;
        
        AppSignature(String packageName, String appName) {
            this.packageName = packageName;
            this.appName = appName;
        }
    }
    
    /**
     * Result of app detection
     */
    public static class AppDetectionResult {
        public final AppInfo appInfo;
        public final float confidence;
        
        public AppDetectionResult(AppInfo appInfo, float confidence) {
            this.appInfo = appInfo;
            this.confidence = confidence;
        }
    }
}