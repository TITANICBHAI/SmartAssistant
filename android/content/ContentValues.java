package android.content;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Mock implementation of Android ContentValues class for development outside of Android.
 * This class is used to store a set of values that can be included with rows in a database table.
 */
public class ContentValues implements Parcelable {
    private HashMap<String, Object> mValues;
    
    /**
     * Creates an empty set of values.
     */
    public ContentValues() {
        mValues = new HashMap<String, Object>();
    }
    
    /**
     * Creates a set of values copied from the given ContentValues.
     * 
     * @param from The ContentValues to copy.
     */
    public ContentValues(ContentValues from) {
        mValues = new HashMap<String, Object>(from.mValues);
    }
    
    /**
     * Creates a set of values using the given HashMap.
     * 
     * @param values The HashMap to copy.
     */
    public ContentValues(HashMap<String, Object> values) {
        mValues = new HashMap<String, Object>(values);
    }
    
    /**
     * Returns the number of values.
     * 
     * @return The number of values.
     */
    public int size() {
        return mValues.size();
    }
    
    /**
     * Returns true if this object has no values.
     * 
     * @return True if this object has no values.
     */
    public boolean isEmpty() {
        return mValues.isEmpty();
    }
    
    /**
     * Removes all values.
     */
    public void clear() {
        mValues.clear();
    }
    
    /**
     * Returns true if the specified key is in the ContentValues.
     * 
     * @param key The key to check.
     * @return True if the key exists.
     */
    public boolean containsKey(String key) {
        return mValues.containsKey(key);
    }
    
    /**
     * Gets a value. Returns null if the value is not found.
     * 
     * @param key The key to find.
     * @return The value or null.
     */
    public Object get(String key) {
        return mValues.get(key);
    }
    
    /**
     * Returns a set of all of the keys and values.
     * 
     * @return A set of all keys and values.
     */
    public Set<Map.Entry<String, Object>> valueSet() {
        return mValues.entrySet();
    }
    
    /**
     * Returns a set of all of the keys.
     * 
     * @return A set of all keys.
     */
    public Set<String> keySet() {
        return mValues.keySet();
    }
    
    /**
     * Adds a null value to the set.
     * 
     * @param key The key for the value.
     */
    public void putNull(String key) {
        mValues.put(key, null);
    }
    
    /**
     * Adds a value to the set.
     * 
     * @param key The key for the value.
     * @param value The value to add.
     */
    public void put(String key, String value) {
        mValues.put(key, value);
    }
    
    /**
     * Adds a value to the set.
     * 
     * @param key The key for the value.
     * @param value The value to add.
     */
    public void put(String key, Byte value) {
        mValues.put(key, value);
    }
    
    /**
     * Adds a value to the set.
     * 
     * @param key The key for the value.
     * @param value The value to add.
     */
    public void put(String key, Short value) {
        mValues.put(key, value);
    }
    
    /**
     * Adds a value to the set.
     * 
     * @param key The key for the value.
     * @param value The value to add.
     */
    public void put(String key, Integer value) {
        mValues.put(key, value);
    }
    
    /**
     * Adds a value to the set.
     * 
     * @param key The key for the value.
     * @param value The value to add.
     */
    public void put(String key, Long value) {
        mValues.put(key, value);
    }
    
    /**
     * Adds a value to the set.
     * 
     * @param key The key for the value.
     * @param value The value to add.
     */
    public void put(String key, Float value) {
        mValues.put(key, value);
    }
    
    /**
     * Adds a value to the set.
     * 
     * @param key The key for the value.
     * @param value The value to add.
     */
    public void put(String key, Double value) {
        mValues.put(key, value);
    }
    
    /**
     * Adds a value to the set.
     * 
     * @param key The key for the value.
     * @param value The value to add.
     */
    public void put(String key, Boolean value) {
        mValues.put(key, value);
    }
    
    /**
     * Adds a value to the set.
     * 
     * @param key The key for the value.
     * @param value The value to add.
     */
    public void put(String key, byte[] value) {
        mValues.put(key, value);
    }
    
    /**
     * Gets a value.
     * 
     * @param key The key for the value.
     * @return The value or null.
     */
    public String getAsString(String key) {
        Object value = mValues.get(key);
        if (value == null) return null;
        return value.toString();
    }
    
    /**
     * Gets a value.
     * 
     * @param key The key for the value.
     * @return The value or null.
     */
    public Long getAsLong(String key) {
        Object value = mValues.get(key);
        if (value == null) return null;
        
        try {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * Gets a value.
     * 
     * @param key The key for the value.
     * @return The value or null.
     */
    public Integer getAsInteger(String key) {
        Object value = mValues.get(key);
        if (value == null) return null;
        
        try {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * Gets a value.
     * 
     * @param key The key for the value.
     * @return The value or null.
     */
    public Short getAsShort(String key) {
        Object value = mValues.get(key);
        if (value == null) return null;
        
        try {
            if (value instanceof Number) {
                return ((Number) value).shortValue();
            }
            return Short.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * Gets a value.
     * 
     * @param key The key for the value.
     * @return The value or null.
     */
    public Byte getAsByte(String key) {
        Object value = mValues.get(key);
        if (value == null) return null;
        
        try {
            if (value instanceof Number) {
                return ((Number) value).byteValue();
            }
            return Byte.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * Gets a value.
     * 
     * @param key The key for the value.
     * @return The value or null.
     */
    public Double getAsDouble(String key) {
        Object value = mValues.get(key);
        if (value == null) return null;
        
        try {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return Double.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * Gets a value.
     * 
     * @param key The key for the value.
     * @return The value or null.
     */
    public Float getAsFloat(String key) {
        Object value = mValues.get(key);
        if (value == null) return null;
        
        try {
            if (value instanceof Number) {
                return ((Number) value).floatValue();
            }
            return Float.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * Gets a value.
     * 
     * @param key The key for the value.
     * @return The value or null.
     */
    public Boolean getAsBoolean(String key) {
        Object value = mValues.get(key);
        if (value == null) return null;
        
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        
        try {
            String str = value.toString();
            return str.equals("1") || str.equalsIgnoreCase("true");
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Gets a value.
     * 
     * @param key The key for the value.
     * @return The value or null.
     */
    public byte[] getAsByteArray(String key) {
        Object value = mValues.get(key);
        if (value == null) return null;
        
        if (value instanceof byte[]) {
            return (byte[]) value;
        }
        
        return null;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : mValues.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            sb.append(key).append("=").append(value).append(" ");
        }
        return sb.toString();
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mValues.size());
        
        for (Map.Entry<String, Object> entry : mValues.entrySet()) {
            parcel.writeString(entry.getKey());
            
            Object value = entry.getValue();
            if (value == null) {
                parcel.writeInt(0); // null marker
            } else if (value instanceof String) {
                parcel.writeInt(1); // string marker
                parcel.writeString((String) value);
            } else if (value instanceof Long) {
                parcel.writeInt(2); // long marker
                parcel.writeLong((Long) value);
            } else if (value instanceof Integer) {
                parcel.writeInt(3); // int marker
                parcel.writeInt((Integer) value);
            } else if (value instanceof Float) {
                parcel.writeInt(4); // float marker
                parcel.writeFloat((Float) value);
            } else if (value instanceof Double) {
                parcel.writeInt(5); // double marker
                parcel.writeDouble((Double) value);
            } else if (value instanceof Boolean) {
                parcel.writeInt(6); // boolean marker
                parcel.writeInt(((Boolean) value) ? 1 : 0);
            } else if (value instanceof byte[]) {
                parcel.writeInt(7); // byte array marker
                parcel.writeInt(((byte[]) value).length);
                parcel.writeByteArray((byte[]) value);
            } else {
                parcel.writeInt(0); // unknown type treated as null
            }
        }
    }
    
    public static final Parcelable.Creator<ContentValues> CREATOR =
            new Parcelable.Creator<ContentValues>() {
        @Override
        public ContentValues createFromParcel(Parcel source) {
            ContentValues values = new ContentValues();
            
            int size = source.readInt();
            
            for (int i = 0; i < size; i++) {
                String key = source.readString();
                int type = source.readInt();
                
                switch (type) {
                    case 0: // null
                        values.putNull(key);
                        break;
                    case 1: // string
                        values.put(key, source.readString());
                        break;
                    case 2: // long
                        values.put(key, source.readLong());
                        break;
                    case 3: // int
                        values.put(key, source.readInt());
                        break;
                    case 4: // float
                        values.put(key, source.readFloat());
                        break;
                    case 5: // double
                        values.put(key, source.readDouble());
                        break;
                    case 6: // boolean
                        values.put(key, source.readInt() == 1);
                        break;
                    case 7: // byte array
                        int len = source.readInt();
                        byte[] arr = new byte[len];
                        source.readByteArray(arr);
                        values.put(key, arr);
                        break;
                }
            }
            
            return values;
        }
        
        @Override
        public ContentValues[] newArray(int size) {
            return new ContentValues[size];
        }
    };
}