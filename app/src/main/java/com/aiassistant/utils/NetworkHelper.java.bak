package com.aiassistant.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * Helper class for network operations
 */
public class NetworkHelper {
    private static final String TAG = "NetworkHelper";
    
    /**
     * Check if network is available
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    
    /**
     * Make HTTP request with custom method, headers, and body
     */
    public static String makeHttpRequest(String urlStr, String method, Map<String, String> headers, String body) 
            throws IOException {
        HttpURLConnection connection = null;
        StringBuilder response = new StringBuilder();
        
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            
            // Set default timeouts
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            
            // Set headers
            if (headers != null) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    connection.setRequestProperty(header.getKey(), header.getValue());
                }
            }
            
            // Set body for POST, PUT, PATCH
            if (body != null && (method.equals("POST") || method.equals("PUT") || method.equals("PATCH"))) {
                connection.setDoOutput(true);
                
                try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                    wr.write(body.getBytes("UTF-8"));
                }
            }
            
            // Get response code
            int responseCode = connection.getResponseCode();
            
            // Read response
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(responseCode >= 400 ? connection.getErrorStream() : connection.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }
            
            // Check for error
            if (responseCode >= 400) {
                Log.e(TAG, "HTTP error code: " + responseCode + ", response: " + response.toString());
                throw new IOException("HTTP error code: " + responseCode);
            }
            
            return response.toString();
            
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Make a GET request
     */
    public static String get(String url, Map<String, String> headers) throws IOException {
        return makeHttpRequest(url, "GET", headers, null);
    }
    
    /**
     * Make a POST request
     */
    public static String post(String url, Map<String, String> headers, String body) throws IOException {
        return makeHttpRequest(url, "POST", headers, body);
    }
    
    /**
     * Make a PUT request
     */
    public static String put(String url, Map<String, String> headers, String body) throws IOException {
        return makeHttpRequest(url, "PUT", headers, body);
    }
    
    /**
     * Make a DELETE request
     */
    public static String delete(String url, Map<String, String> headers) throws IOException {
        return makeHttpRequest(url, "DELETE", headers, null);
    }
}