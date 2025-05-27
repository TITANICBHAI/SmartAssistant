package com.aiassistant.utils;

import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Helper class for encryption and decryption operations
 */
public class EncryptionHelper {
    private static final String TAG = "EncryptionHelper";
    
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String AES_MODE = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String SHARED_PREFS_FILENAME = "encrypted_data";
    
    private static EncryptionHelper instance;
    private final Context context;
    
    /**
     * Get singleton instance of EncryptionHelper
     */
    public static synchronized EncryptionHelper getInstance(Context context) {
        if (instance == null) {
            instance = new EncryptionHelper(context.getApplicationContext());
        }
        return instance;
    }
    
    private EncryptionHelper(Context context) {
        this.context = context;
    }
    
    /**
     * Encrypt a string value
     */
    public String encrypt(String alias, String value) {
        try {
            if (value == null) {
                return null;
            }
            
            // Get or create key
            SecretKey secretKey = getOrCreateKey(alias);
            
            // Encrypt the value
            Cipher cipher = Cipher.getInstance(AES_MODE);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            // Get IV
            byte[] iv = cipher.getIV();
            
            // Encrypt
            byte[] encryptedBytes = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            
            // Combine IV and encrypted data
            byte[] combined = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);
            
            // Encode as Base64
            return Base64.encodeToString(combined, Base64.DEFAULT);
            
        } catch (Exception e) {
            Log.e(TAG, "Error encrypting data: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Decrypt an encrypted string value
     */
    public String decrypt(String alias, String encryptedValue) {
        try {
            if (encryptedValue == null) {
                return null;
            }
            
            // Get key
            SecretKey secretKey = getKey(alias);
            if (secretKey == null) {
                Log.e(TAG, "Key not found for alias: " + alias);
                return null;
            }
            
            // Decode from Base64
            byte[] combined = Base64.decode(encryptedValue, Base64.DEFAULT);
            
            // Extract IV and encrypted data
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encryptedBytes = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, encryptedBytes, 0, encryptedBytes.length);
            
            // Decrypt
            Cipher cipher = Cipher.getInstance(AES_MODE);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            
            // Return decrypted string
            return new String(cipher.doFinal(encryptedBytes), StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            Log.e(TAG, "Error decrypting data: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Store an encrypted value in SharedPreferences
     */
    public boolean store(String key, String value) {
        try {
            if (value == null) {
                context.getSharedPreferences(SHARED_PREFS_FILENAME, Context.MODE_PRIVATE)
                        .edit()
                        .remove(key)
                        .apply();
                return true;
            }
            
            String encryptedValue = encrypt(key, value);
            
            context.getSharedPreferences(SHARED_PREFS_FILENAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(key, encryptedValue)
                    .apply();
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error storing encrypted data: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Retrieve and decrypt a value from SharedPreferences
     */
    public String retrieve(String key) {
        try {
            String encryptedValue = context.getSharedPreferences(SHARED_PREFS_FILENAME, Context.MODE_PRIVATE)
                    .getString(key, null);
            
            if (encryptedValue == null) {
                return null;
            }
            
            return decrypt(key, encryptedValue);
            
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving encrypted data: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Store a token in encrypted storage
     */
    public boolean storeToken(String service, String token) {
        return store(service + "_token", token);
    }
    
    /**
     * Retrieve a token from encrypted storage
     */
    public String retrieveToken(String service) {
        return retrieve(service + "_token");
    }
    
    /**
     * Store credentials in encrypted storage
     */
    public boolean storeCredentials(String service, String username, String password) {
        boolean usernameStored = store(service + "_username", username);
        boolean passwordStored = store(service + "_password", password);
        return usernameStored && passwordStored;
    }
    
    /**
     * Retrieve username from encrypted storage
     */
    public String retrieveUsername(String service) {
        return retrieve(service + "_username");
    }
    
    /**
     * Retrieve password from encrypted storage
     */
    public String retrievePassword(String service) {
        return retrieve(service + "_password");
    }
    
    /**
     * Generate a random secure token
     */
    public String generateSecureToken(int byteLength) {
        SecureRandom secureRandom = new SecureRandom();
        byte[] token = new byte[byteLength];
        secureRandom.nextBytes(token);
        return Base64.encodeToString(token, Base64.URL_SAFE | Base64.NO_WRAP);
    }
    
    /**
     * Get or create encryption key for the given alias
     */
    private SecretKey getOrCreateKey(String alias) throws GeneralSecurityException, IOException {
        SecretKey key = getKey(alias);
        
        if (key == null) {
            key = createKey(alias);
        }
        
        return key;
    }
    
    /**
     * Get encryption key for the given alias
     */
    private SecretKey getKey(String alias) throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        
        if (!keyStore.containsAlias(alias)) {
            return null;
        }
        
        Key key = keyStore.getKey(alias, null);
        if (key instanceof SecretKey) {
            return (SecretKey) key;
        }
        
        return null;
    }
    
    /**
     * Create a new encryption key for the given alias
     */
    private SecretKey createKey(String alias) throws GeneralSecurityException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Use Android KeyStore on Android M and above
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
            
            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256);
            
            keyGenerator.init(builder.build());
            return keyGenerator.generateKey();
            
        } else {
            // For older Android versions, generate a key and store it encrypted
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256, new SecureRandom());
            SecretKey secretKey = keyGenerator.generateKey();
            
            // Store the key securely
            byte[] encoded = secretKey.getEncoded();
            String keyStr = Base64.encodeToString(encoded, Base64.DEFAULT);
            
            // Store the key in SharedPreferences (this isn't ideal, but it's better than nothing)
            context.getSharedPreferences(SHARED_PREFS_FILENAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(alias + "_key", keyStr)
                    .apply();
            
            return secretKey;
        }
    }
}