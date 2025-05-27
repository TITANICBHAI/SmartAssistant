package android.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Arrays;

/**
 * Mock implementation of Android SparseArray class for development outside of Android.
 * This class provides a specialized map from int to Object that is designed to be more efficient
 * than using a HashMap to map Integers to Objects.
 *
 * @param <E> The type of values stored in the array
 */
public class SparseArray<E> implements Cloneable {
    private int[] mKeys;
    private Object[] mValues;
    private int mSize;
    
    /**
     * Creates a new SparseArray containing no mappings
     */
    public SparseArray() {
        this(10);
    }
    
    /**
     * Creates a new SparseArray containing no mappings that will not require any
     * additional memory allocation to store the specified number of mappings.
     * 
     * @param initialCapacity The initial capacity of the array
     */
    public SparseArray(int initialCapacity) {
        if (initialCapacity == 0) {
            mKeys = new int[0];
            mValues = new Object[0];
        } else {
            mKeys = new int[initialCapacity];
            mValues = new Object[initialCapacity];
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
    @SuppressWarnings("unchecked")
    public E valueAt(int index) {
        if (index >= mSize) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return (E) mValues[index];
    }
    
    /**
     * Sets the value at the given index
     * 
     * @param index The index
     * @param value The value
     */
    public void setValueAt(int index, E value) {
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
    public int indexOfValue(E value) {
        for (int i = 0; i < mSize; i++) {
            if (mValues[i] == value || (value != null && value.equals(mValues[i]))) {
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
     * Alias for {@link #delete(int)}
     * 
     * @param key The key
     */
    public void remove(int key) {
        delete(key);
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
    public void put(int key, E value) {
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
     * Adds a key-value mapping to the array if the key does not exist yet
     * 
     * @param key The key
     * @param value The value
     * @return The previous value, or null
     */
    @SuppressWarnings("unchecked")
    public E putIfAbsent(int key, E value) {
        E oldValue = get(key);
        if (oldValue == null) {
            put(key, value);
        }
        return oldValue;
    }
    
    /**
     * Finds the key that is associated with the specified value
     * 
     * @param value The value
     * @return The key, or 0 if the value is not in the array
     */
    public int keyOf(E value) {
        for (int i = 0; i < mSize; i++) {
            if (mValues[i] == value || (value != null && value.equals(mValues[i]))) {
                return mKeys[i];
            }
        }
        return 0;
    }
    
    /**
     * Adds a key-value mapping to the array
     * 
     * @param key The key
     * @param value The value
     */
    public void append(int key, E value) {
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
     * @return The value, or null if the key is not in the array
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public E get(int key) {
        return get(key, null);
    }
    
    /**
     * Gets the value for the given key, or the given default if the key is not in the array
     * 
     * @param key The key
     * @param valueIfKeyNotFound The default value
     * @return The value, or the default if the key is not in the array
     */
    @SuppressWarnings("unchecked")
    public E get(int key, E valueIfKeyNotFound) {
        int i = indexOfKey(key);
        return i >= 0 ? (E) mValues[i] : valueIfKeyNotFound;
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
    @SuppressWarnings("unchecked")
    public SparseArray<E> clone() {
        SparseArray<E> clone;
        try {
            clone = (SparseArray<E>) super.clone();
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
            Object value = mValues[i];
            if (value != this) {
                buffer.append(value);
            } else {
                buffer.append("(this SparseArray)");
            }
        }
        buffer.append('}');
        return buffer.toString();
    }
}