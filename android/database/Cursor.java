package android.database;

/**
 * Mock implementation of Android Cursor interface for development outside of Android.
 * This interface provides random read-write access to the result set returned
 * by a database query.
 */
public interface Cursor {
    /**
     * Returns the numbers of rows in the cursor.
     * 
     * @return the number of rows in the cursor.
     */
    int getCount();
    
    /**
     * Returns the current position of the cursor in the row set.
     * The value is zero-based. When the cursor is at the first row, the position is 0,
     * at the second row the position is 1, and so on.
     * 
     * @return The current cursor position.
     */
    int getPosition();
    
    /**
     * Move the cursor to an absolute position.
     * 
     * @param position The position to move to.
     * @return true if the requested position is valid, false otherwise.
     */
    boolean moveToPosition(int position);
    
    /**
     * Move the cursor to the first row.
     * 
     * @return true if the cursor is not empty, false otherwise.
     */
    boolean moveToFirst();
    
    /**
     * Move the cursor to the last row.
     * 
     * @return true if the cursor is not empty, false otherwise.
     */
    boolean moveToLast();
    
    /**
     * Move the cursor to the next row.
     * 
     * @return true if the new position is valid, false otherwise.
     */
    boolean moveToNext();
    
    /**
     * Move the cursor to the previous row.
     * 
     * @return true if the new position is valid, false otherwise.
     */
    boolean moveToPrevious();
    
    /**
     * Returns whether the cursor is pointing to the first row.
     * 
     * @return true if the cursor is pointing to the first row, false otherwise.
     */
    boolean isFirst();
    
    /**
     * Returns whether the cursor is pointing to the last row.
     * 
     * @return true if the cursor is pointing to the last row, false otherwise.
     */
    boolean isLast();
    
    /**
     * Returns whether the cursor is pointing to the position before the first row.
     * 
     * @return true if the cursor is before the first row, false otherwise.
     */
    boolean isBeforeFirst();
    
    /**
     * Returns whether the cursor is pointing to the position after the last row.
     * 
     * @return true if the cursor is after the last row, false otherwise.
     */
    boolean isAfterLast();
    
    /**
     * Returns the column names for the cursor.
     * 
     * @return the column names.
     */
    String[] getColumnNames();
    
    /**
     * Returns the column index for the given column name.
     * 
     * @param columnName The name of the column.
     * @return The column index, or -1 if the column doesn't exist.
     */
    int getColumnIndex(String columnName);
    
    /**
     * Returns the column index for the given column name.
     * 
     * @param columnName The name of the column.
     * @return The column index, or -1.
     * @throws IllegalArgumentException if the column doesn't exist.
     */
    int getColumnIndexOrThrow(String columnName);
    
    /**
     * Returns the column name at the given column index.
     * 
     * @param columnIndex The index of the column.
     * @return The column name.
     */
    String getColumnName(int columnIndex);
    
    /**
     * Returns the zero-based index for the given column name, or -1 if the column doesn't exist.
     * 
     * @param columnName The column name.
     * @return The column index, or -1 if the column doesn't exist.
     */
    int getColumnCount();
    
    /**
     * Returns the value of the requested column as a String.
     * 
     * @param columnIndex The zero-based index of the target column.
     * @return The value of that column as a String.
     */
    String getString(int columnIndex);
    
    /**
     * Returns the value of the requested column as a short.
     * 
     * @param columnIndex The zero-based index of the target column.
     * @return The value of that column as a short.
     */
    short getShort(int columnIndex);
    
    /**
     * Returns the value of the requested column as an int.
     * 
     * @param columnIndex The zero-based index of the target column.
     * @return The value of that column as an int.
     */
    int getInt(int columnIndex);
    
    /**
     * Returns the value of the requested column as a long.
     * 
     * @param columnIndex The zero-based index of the target column.
     * @return The value of that column as a long.
     */
    long getLong(int columnIndex);
    
    /**
     * Returns the value of the requested column as a float.
     * 
     * @param columnIndex The zero-based index of the target column.
     * @return The value of that column as a float.
     */
    float getFloat(int columnIndex);
    
    /**
     * Returns the value of the requested column as a double.
     * 
     * @param columnIndex The zero-based index of the target column.
     * @return The value of that column as a double.
     */
    double getDouble(int columnIndex);
    
    /**
     * Returns the value of the requested column as a byte array.
     * 
     * @param columnIndex The zero-based index of the target column.
     * @return The value of that column as a byte array.
     */
    byte[] getBlob(int columnIndex);
    
    /**
     * Returns true if the value in the indicated column is null.
     * 
     * @param columnIndex The zero-based index of the target column.
     * @return true if the value in the indicated column is null, false otherwise.
     */
    boolean isNull(int columnIndex);
    
    /**
     * Close the Cursor, releasing all of its resources and making it completely invalid.
     */
    void close();
    
    /**
     * return true if the cursor is closed
     * @return true if the cursor is closed.
     */
    boolean isClosed();
}