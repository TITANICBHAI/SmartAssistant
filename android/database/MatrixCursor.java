package android.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Mock implementation of Android MatrixCursor class for development outside of Android.
 * A convenience class that implements a cursor populated with data provided
 * as arrays of objects. Commonly used for testing.
 */
public class MatrixCursor extends AbstractCursor {
    private final String[] mColumnNames;
    private final List<Object[]> mRows = new ArrayList<>();
    private int mRowCount = 0;
    
    /**
     * Constructs a new cursor with the given column names.
     *
     * @param columnNames The column names for this cursor.
     */
    public MatrixCursor(@NonNull String[] columnNames) {
        this(columnNames, 16);
    }
    
    /**
     * Constructs a new cursor with the given column names and initial capacity.
     *
     * @param columnNames The column names for this cursor.
     * @param initialCapacity The initial capacity of the cursor.
     */
    public MatrixCursor(@NonNull String[] columnNames, int initialCapacity) {
        if (columnNames == null) {
            throw new NullPointerException("columnNames cannot be null");
        }
        mColumnNames = columnNames;
    }
    
    /**
     * Gets the cursor's column count.
     *
     * @return The number of columns in the cursor.
     */
    @Override
    public int getColumnCount() {
        return mColumnNames.length;
    }
    
    /**
     * Gets the cursor's column names.
     *
     * @return The names of all columns in the cursor.
     */
    @Override
    @NonNull
    public String[] getColumnNames() {
        return mColumnNames;
    }
    
    /**
     * Gets the value of the requested column as a byte array.
     *
     * @param columnIndex The zero-based index of the target column.
     * @return The value of that column as a byte array.
     */
    @Override
    @Nullable
    public byte[] getBlob(int columnIndex) {
        Object value = getValue(columnIndex);
        return (byte[]) value;
    }
    
    /**
     * Gets the value of the requested column as a string.
     *
     * @param columnIndex The zero-based index of the target column.
     * @return The value of that column as a string.
     */
    @Override
    @Nullable
    public String getString(int columnIndex) {
        Object value = getValue(columnIndex);
        return value == null ? null : value.toString();
    }
    
    /**
     * Gets the value of the requested column as a short.
     *
     * @param columnIndex The zero-based index of the target column.
     * @return The value of that column as a short.
     */
    @Override
    public short getShort(int columnIndex) {
        Object value = getValue(columnIndex);
        if (value instanceof Number) {
            return ((Number) value).shortValue();
        }
        return (short) 0;
    }
    
    /**
     * Gets the value of the requested column as an int.
     *
     * @param columnIndex The zero-based index of the target column.
     * @return The value of that column as an int.
     */
    @Override
    public int getInt(int columnIndex) {
        Object value = getValue(columnIndex);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }
    
    /**
     * Gets the value of the requested column as a long.
     *
     * @param columnIndex The zero-based index of the target column.
     * @return The value of that column as a long.
     */
    @Override
    public long getLong(int columnIndex) {
        Object value = getValue(columnIndex);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }
    
    /**
     * Gets the value of the requested column as a float.
     *
     * @param columnIndex The zero-based index of the target column.
     * @return The value of that column as a float.
     */
    @Override
    public float getFloat(int columnIndex) {
        Object value = getValue(columnIndex);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return 0.0f;
    }
    
    /**
     * Gets the value of the requested column as a double.
     *
     * @param columnIndex The zero-based index of the target column.
     * @return The value of that column as a double.
     */
    @Override
    public double getDouble(int columnIndex) {
        Object value = getValue(columnIndex);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }
    
    /**
     * Gets the type of the given column's value.
     *
     * @param columnIndex The zero-based index of the target column.
     * @return The type of the column's value.
     */
    @Override
    public int getType(int columnIndex) {
        Object value = getValue(columnIndex);
        if (value == null) {
            return FIELD_TYPE_NULL;
        } else if (value instanceof byte[]) {
            return FIELD_TYPE_BLOB;
        } else if (value instanceof Float || value instanceof Double) {
            return FIELD_TYPE_FLOAT;
        } else if (value instanceof Long || value instanceof Integer
                   || value instanceof Short || value instanceof Byte) {
            return FIELD_TYPE_INTEGER;
        } else {
            return FIELD_TYPE_STRING;
        }
    }
    
    /**
     * Returns TRUE if the value in the indicated column is null.
     *
     * @param columnIndex The zero-based index of the target column.
     * @return TRUE if the column value is null.
     */
    @Override
    public boolean isNull(int columnIndex) {
        return getValue(columnIndex) == null;
    }
    
    /**
     * Gets the total number of rows in the cursor.
     *
     * @return The number of rows in the cursor.
     */
    @Override
    public int getCount() {
        return mRowCount;
    }
    
    /**
     * Adds a new row to the cursor with the given values.
     *
     * @param values The values to add to the row. Must have same length
     *               as the column names array.
     * @return The index of the newly added row.
     */
    public int addRow(@NonNull Object[] values) {
        if (values.length != mColumnNames.length) {
            throw new IllegalArgumentException("Values array length must match column names array length");
        }
        
        int rowId = mRowCount;
        mRows.add(Arrays.copyOf(values, values.length));
        mRowCount++;
        return rowId;
    }
    
    /**
     * Adds a new row to the cursor with the given values list.
     *
     * @param columnValues The values to add to the row. Must have same
     *                    length as the column names array.
     * @return The index of the newly added row.
     */
    public int addRow(@NonNull Iterable<?> columnValues) {
        Object[] values = new Object[mColumnNames.length];
        int i = 0;
        for (Object value : columnValues) {
            if (i >= mColumnNames.length) {
                throw new IllegalArgumentException("More values than column names");
            }
            values[i++] = value;
        }
        
        if (i != mColumnNames.length) {
            throw new IllegalArgumentException("Not enough values for all columns");
        }
        
        return addRow(values);
    }
    
    /**
     * Clears all rows from the cursor.
     */
    public void clear() {
        mRows.clear();
        mRowCount = 0;
    }
    
    /**
     * Gets the value at the given column for the current row.
     *
     * @param columnIndex The zero-based index of the target column.
     * @return The value at the given column.
     * @throws CursorIndexOutOfBoundsException If the cursor is not positioned on
     *         a valid row, or the column index is invalid.
     */
    @Nullable
    private Object getValue(int columnIndex) {
        if (mPos < 0 || mPos >= mRowCount) {
            throw new CursorIndexOutOfBoundsException("Invalid cursor position: " + mPos);
        }
        
        if (columnIndex < 0 || columnIndex >= mColumnNames.length) {
            throw new CursorIndexOutOfBoundsException("Invalid column index: " + columnIndex);
        }
        
        return mRows.get(mPos)[columnIndex];
    }
}