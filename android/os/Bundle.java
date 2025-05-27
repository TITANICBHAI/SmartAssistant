package android.os;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Mock implementation of Android's Bundle class.
 * This class is used to pass data between components.
 */
public class Bundle implements Parcelable {
    private final Map<String, Object> map;
    
    /**
     * Create a new Bundle.
     */
    public Bundle() {
        this.map = new HashMap<>();
    }
    
    /**
     * Create a new Bundle with the given capacity.
     * 
     * @param capacity The initial capacity
     */
    public Bundle(int capacity) {
        this.map = new HashMap<>(capacity);
    }
    
    /**
     * Create a new Bundle from an existing one.
     * 
     * @param bundle The source bundle
     */
    public Bundle(Bundle bundle) {
        this.map = new HashMap<>(bundle.map);
    }
    
    /**
     * Clear the bundle.
     */
    public void clear() {
        map.clear();
    }
    
    /**
     * Check if the bundle contains a key.
     * 
     * @param key The key to check
     * @return True if the key exists, false otherwise
     */
    public boolean containsKey(String key) {
        return map.containsKey(key);
    }
    
    /**
     * Get a value from the bundle.
     * 
     * @param key The key to get
     * @return The value, or null if not found
     */
    public Object get(String key) {
        return map.get(key);
    }
    
    /**
     * Get a String value from the bundle.
     * 
     * @param key The key to get
     * @return The String value, or null if not found
     */
    public String getString(String key) {
        Object value = map.get(key);
        return value instanceof String ? (String) value : null;
    }
    
    /**
     * Get a String value from the bundle with a default.
     * 
     * @param key The key to get
     * @param defaultValue The default value
     * @return The String value, or defaultValue if not found
     */
    public String getString(String key, String defaultValue) {
        String value = getString(key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Get an int value from the bundle.
     * 
     * @param key The key to get
     * @param defaultValue The default value
     * @return The int value, or defaultValue if not found
     */
    public int getInt(String key, int defaultValue) {
        Object value = map.get(key);
        return value instanceof Integer ? (Integer) value : defaultValue;
    }
    
    /**
     * Get a long value from the bundle.
     * 
     * @param key The key to get
     * @param defaultValue The default value
     * @return The long value, or defaultValue if not found
     */
    public long getLong(String key, long defaultValue) {
        Object value = map.get(key);
        return value instanceof Long ? (Long) value : defaultValue;
    }
    
    /**
     * Get a boolean value from the bundle.
     * 
     * @param key The key to get
     * @param defaultValue The default value
     * @return The boolean value, or defaultValue if not found
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = map.get(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }
    
    /**
     * Get a float value from the bundle.
     * 
     * @param key The key to get
     * @param defaultValue The default value
     * @return The float value, or defaultValue if not found
     */
    public float getFloat(String key, float defaultValue) {
        Object value = map.get(key);
        return value instanceof Float ? (Float) value : defaultValue;
    }
    
    /**
     * Get a double value from the bundle.
     * 
     * @param key The key to get
     * @param defaultValue The default value
     * @return The double value, or defaultValue if not found
     */
    public double getDouble(String key, double defaultValue) {
        Object value = map.get(key);
        return value instanceof Double ? (Double) value : defaultValue;
    }
    
    /**
     * Get a String array value from the bundle.
     * 
     * @param key The key to get
     * @return The String array value, or null if not found
     */
    public String[] getStringArray(String key) {
        Object value = map.get(key);
        return value instanceof String[] ? (String[]) value : null;
    }
    
    /**
     * Get a Parcelable value from the bundle.
     * 
     * @param key The key to get
     * @return The Parcelable value, or null if not found
     */
    public <T extends Parcelable> T getParcelable(String key) {
        Object value = map.get(key);
        try {
            @SuppressWarnings("unchecked")
            T result = (T) value;
            return result;
        } catch (ClassCastException e) {
            return null;
        }
    }
    
    /**
     * Get a Bundle value from the bundle.
     * 
     * @param key The key to get
     * @return The Bundle value, or null if not found
     */
    public Bundle getBundle(String key) {
        Object value = map.get(key);
        return value instanceof Bundle ? (Bundle) value : null;
    }
    
    /**
     * Get a CharSequence value from the bundle.
     * 
     * @param key The key to get
     * @return The CharSequence value, or null if not found
     */
    public CharSequence getCharSequence(String key) {
        Object value = map.get(key);
        return value instanceof CharSequence ? (CharSequence) value : null;
    }
    
    /**
     * Get a CharSequence value from the bundle with a default.
     * 
     * @param key The key to get
     * @param defaultValue The default value
     * @return The CharSequence value, or defaultValue if not found
     */
    public CharSequence getCharSequence(String key, CharSequence defaultValue) {
        CharSequence value = getCharSequence(key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Put a value in the bundle.
     * 
     * @param key The key to put
     * @param value The value to put
     */
    public void putObject(String key, Object value) {
        map.put(key, value);
    }
    
    /**
     * Put a String value in the bundle.
     * 
     * @param key The key to put
     * @param value The value to put
     */
    public void putString(String key, String value) {
        map.put(key, value);
    }
    
    /**
     * Put an int value in the bundle.
     * 
     * @param key The key to put
     * @param value The value to put
     */
    public void putInt(String key, int value) {
        map.put(key, value);
    }
    
    /**
     * Put a long value in the bundle.
     * 
     * @param key The key to put
     * @param value The value to put
     */
    public void putLong(String key, long value) {
        map.put(key, value);
    }
    
    /**
     * Put a boolean value in the bundle.
     * 
     * @param key The key to put
     * @param value The value to put
     */
    public void putBoolean(String key, boolean value) {
        map.put(key, value);
    }
    
    /**
     * Put a float value in the bundle.
     * 
     * @param key The key to put
     * @param value The value to put
     */
    public void putFloat(String key, float value) {
        map.put(key, value);
    }
    
    /**
     * Put a double value in the bundle.
     * 
     * @param key The key to put
     * @param value The value to put
     */
    public void putDouble(String key, double value) {
        map.put(key, value);
    }
    
    /**
     * Put a String array value in the bundle.
     * 
     * @param key The key to put
     * @param value The value to put
     */
    public void putStringArray(String key, String[] value) {
        map.put(key, value);
    }
    
    /**
     * Put a Parcelable value in the bundle.
     * 
     * @param key The key to put
     * @param value The value to put
     */
    public void putParcelable(String key, Parcelable value) {
        map.put(key, value);
    }
    
    /**
     * Put a Bundle value in the bundle.
     * 
     * @param key The key to put
     * @param value The value to put
     */
    public void putBundle(String key, Bundle value) {
        map.put(key, value);
    }
    
    /**
     * Put a CharSequence value in the bundle.
     * 
     * @param key The key to put
     * @param value The value to put
     */
    public void putCharSequence(String key, CharSequence value) {
        map.put(key, value);
    }
    
    /**
     * Remove a value from the bundle.
     * 
     * @param key The key to remove
     */
    public void remove(String key) {
        map.remove(key);
    }
    
    /**
     * Get the size of the bundle.
     * 
     * @return The size
     */
    public int size() {
        return map.size();
    }
    
    /**
     * Get the key set of the bundle.
     * 
     * @return The key set
     */
    public Set<String> keySet() {
        return map.keySet();
    }
    
    /**
     * Check if the bundle is empty.
     * 
     * @return True if empty, false otherwise
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(map.size());
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            dest.writeString(entry.getKey());
            // For simplicity, we only support String values in this mock
            if (entry.getValue() instanceof String) {
                dest.writeInt(1); // String type
                dest.writeString((String) entry.getValue());
            } else if (entry.getValue() instanceof Integer) {
                dest.writeInt(2); // Integer type
                dest.writeInt((Integer) entry.getValue());
            } else if (entry.getValue() instanceof Boolean) {
                dest.writeInt(3); // Boolean type
                dest.writeBoolean((Boolean) entry.getValue());
            } else if (entry.getValue() instanceof Parcelable) {
                dest.writeInt(4); // Parcelable type
                dest.writeParcelable((Parcelable) entry.getValue(), flags);
            } else {
                dest.writeInt(0); // Unsupported type
            }
        }
    }
    
    public static final Parcelable.Creator<Bundle> CREATOR = new Parcelable.Creator<Bundle>() {
        @Override
        public Bundle createFromParcel(Parcel source) {
            Bundle bundle = new Bundle();
            int size = source.readInt();
            for (int i = 0; i < size; i++) {
                String key = source.readString();
                int type = source.readInt();
                
                switch (type) {
                    case 1: // String
                        bundle.putString(key, source.readString());
                        break;
                    case 2: // Integer
                        bundle.putInt(key, source.readInt());
                        break;
                    case 3: // Boolean
                        bundle.putBoolean(key, source.readBoolean());
                        break;
                    case 4: // Parcelable
                        bundle.putParcelable(key, source.readParcelable(Bundle.class.getClassLoader()));
                        break;
                    default:
                        // Skip unsupported types
                        break;
                }
            }
            return bundle;
        }
        
        @Override
        public Bundle[] newArray(int size) {
            return new Bundle[size];
        }
    };
    
    @Override
    public int describeContents() {
        return 0;
    }
}