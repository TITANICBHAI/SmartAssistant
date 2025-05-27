package android.util;

import androidx.annotation.NonNull;
import java.util.Arrays;

/**
 * Mock implementation of Android SparseBooleanArray class for development outside of Android.
 * This class provides a specialized map from int to boolean that is designed to be more efficient
 * than using a HashMap to map Integers to Booleans.
 */
public class SparseBooleanArray implements Cloneable {
    private int[] mKeys;
    private boolean[] mValues;
    private int mSize;
    
    /**
     * Creates a new SparseBooleanArray containing no mappings
     */
    public SparseBooleanArray() {
        this(10);
    }
    
    /**
     * Creates a new SparseBooleanArray containing no mappings that will not require any
     * additional memory allocation to store the specified number of mappings.
     * 
     * @param initialCapacity The initial capacity of the array
     */
    public SparseBooleanArray(int initialCapacity) {
        if (initialCapacity == 0) {
            mKeys = new int[0];
            mValues = new boolean[0];
        } else {
            mKeys = new int[initialCapacity];
            mValues = new boolean[initialCapacity];
        }
        mSize = 0;
    }
    
    /**
     * Gets the size of the array
     * 
     * @return The number of key-value mappings
     */
    public int size() {
        return mSize;
    }
    
    /**
     * Returns the key at the given index
     * 
     * @param index The index
     * @return The key
     */
    public int keyAt(int index) {
        if (index >= mSize) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return mKeys[index];
    }
    
    /**
     * Returns the value at the given index
     * 
     * @param index The index
     * @return The value
     */
    public boolean valueAt(int index) {
        if (index >= mSize) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return mValues[index];
    }
    
    /**
     * Sets the value at the given index
     * 
     * @param index The index
     * @param value The value
     */
    public void setValueAt(int index, boolean value) {
        if (index >= mSize) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        mValues[index] = value;
    }
    
    /**
     * Returns the index for the given key, or a negative number if the key is not present
     * 
     * @param key The key
     * @return The index, or a negative number
     */
    public int indexOfKey(int key) {
        for (int i = 0; i < mSize; i++) {
            if (mKeys[i] == key) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Returns the index for the given value, or a negative number if the value is not present
     * 
     * @param value The value
     * @return The index, or a negative number
     */
    public int indexOfValue(boolean value) {
        for (int i = 0; i < mSize; i++) {
            if (mValues[i] == value) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Removes the mapping from the specified key, if there was any
     * 
     * @param key The key
     */
    public void delete(int key) {
        int i = indexOfKey(key);
        if (i >= 0) {
            System.arraycopy(mKeys, i + 1, mKeys, i, mSize - i - 1);
            System.arraycopy(mValues, i + 1, mValues, i, mSize - i - 1);
            mSize--;
        }
    }
    
    /**
     * Removes the mapping at the specified index
     * 
     * @param index The index
     */
    public void removeAt(int index) {
        if (index >= mSize) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        System.arraycopy(mKeys, index + 1, mKeys, index, mSize - index - 1);
        System.arraycopy(mValues, index + 1, mValues, index, mSize - index - 1);
        mSize--;
    }
    
    /**
     * Removes all key-value mappings
     */
    public void clear() {
        mSize = 0;
    }
    
    /**
     * Puts a key-value mapping into the array
     * 
     * @param key The key
     * @param value The value
     */
    public void put(int key, boolean value) {
        int i = indexOfKey(key);
        if (i >= 0) {
            mValues[i] = value;
        } else {
            i = ~i;
            if (mSize >= mKeys.length) {
                grow(mSize + 1);
            }
            System.arraycopy(mKeys, i, mKeys, i + 1, mSize - i);
            System.arraycopy(mValues, i, mValues, i + 1, mSize - i);
            mKeys[i] = key;
            mValues[i] = value;
            mSize++;
        }
    }
    
    /**
     * Adds a key-value mapping to the array
     * 
     * @param key The key
     * @param value The value
     */
    public void append(int key, boolean value) {
        if (mSize != 0 && key <= mKeys[mSize - 1]) {
            put(key, value);
            return;
        }
        
        if (mSize >= mKeys.length) {
            grow(mSize + 1);
        }
        
        mKeys[mSize] = key;
        mValues[mSize] = value;
        mSize++;
    }
    
    /**
     * Gets the value for the given key
     * 
     * @param key The key
     * @return The value, or false if the key is not in the array
     */
    public boolean get(int key) {
        return get(key, false);
    }
    
    /**
     * Gets the value for the given key, or the given default if the key is not in the array
     * 
     * @param key The key
     * @param valueIfKeyNotFound The default value
     * @return The value, or the default if the key is not in the array
     */
    public boolean get(int key, boolean valueIfKeyNotFound) {
        int i = indexOfKey(key);
        return i >= 0 ? mValues[i] : valueIfKeyNotFound;
    }
    
    /**
     * Grows the capacity of the array
     * 
     * @param minCapacity The minimum capacity required
     */
    private void grow(int minCapacity) {
        int oldCapacity = mKeys.length;
        int newCapacity = oldCapacity + (oldCapacity < 4 ? 4 : oldCapacity >> 1);
        if (newCapacity < minCapacity) {
            newCapacity = minCapacity;
        }
        mKeys = Arrays.copyOf(mKeys, newCapacity);
        mValues = Arrays.copyOf(mValues, newCapacity);
    }
    
    /**
     * Creates a copy of the array
     * 
     * @return A copy of the array
     */
    @Override
    @NonNull
    public SparseBooleanArray clone() {
        SparseBooleanArray clone;
        try {
            clone = (SparseBooleanArray) super.clone();
            clone.mKeys = mKeys.clone();
            clone.mValues = mValues.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
    
    /**
     * Returns a string representation of the array
     * 
     * @return A string representation
     */
    @Override
    public String toString() {
        if (size() <= 0) {
            return "{}";
        }
        
        StringBuilder buffer = new StringBuilder(mSize * 28);
        buffer.append('{');
        for (int i = 0; i < mSize; i++) {
            if (i > 0) {
                buffer.append(", ");
            }
            buffer.append(mKeys[i]);
            buffer.append('=');
            buffer.append(mValues[i]);
        }
        buffer.append('}');
        return buffer.toString();
    }
}