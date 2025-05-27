package android.graphics;

/**
 * Mock implementation of Android ColorMatrix class for development outside of Android.
 * A 4x5 matrix for transforming colors that is useful for saturation, hue rotation, etc.
 */
public class ColorMatrix {
    private final float[] matrix = new float[20];
    
    /**
     * Create a new color matrix initialized to identity (no change)
     */
    public ColorMatrix() {
        reset();
    }
    
    /**
     * Create a new color matrix with the specified array of values
     * 
     * @param src The array of values to initialize the matrix (length must be 20)
     */
    public ColorMatrix(float[] src) {
        if (src == null || src.length != 20) {
            throw new IllegalArgumentException("Array must be 20 values");
        }
        System.arraycopy(src, 0, matrix, 0, 20);
    }
    
    /**
     * Create a new color matrix from another matrix
     * 
     * @param src The matrix to copy from
     */
    public ColorMatrix(ColorMatrix src) {
        if (src != null) {
            System.arraycopy(src.matrix, 0, matrix, 0, 20);
        } else {
            reset();
        }
    }
    
    /**
     * Get the underlying array of values
     * 
     * @return The array of values
     */
    public float[] getArray() {
        return matrix;
    }
    
    /**
     * Reset the matrix to identity
     */
    public void reset() {
        matrix[0] = 1;
        matrix[1] = 0;
        matrix[2] = 0;
        matrix[3] = 0;
        matrix[4] = 0;
        matrix[5] = 0;
        matrix[6] = 1;
        matrix[7] = 0;
        matrix[8] = 0;
        matrix[9] = 0;
        matrix[10] = 0;
        matrix[11] = 0;
        matrix[12] = 1;
        matrix[13] = 0;
        matrix[14] = 0;
        matrix[15] = 0;
        matrix[16] = 0;
        matrix[17] = 0;
        matrix[18] = 1;
        matrix[19] = 0;
    }
    
    /**
     * Set the matrix to another matrix
     * 
     * @param src The matrix to copy from
     */
    public void set(ColorMatrix src) {
        if (src != null) {
            System.arraycopy(src.matrix, 0, matrix, 0, 20);
        } else {
            reset();
        }
    }
    
    /**
     * Set the matrix values directly
     * 
     * @param src The array of values to set (length must be 20)
     */
    public void set(float[] src) {
        if (src == null || src.length != 20) {
            throw new IllegalArgumentException("Array must be 20 values");
        }
        System.arraycopy(src, 0, matrix, 0, 20);
    }
    
    /**
     * Set a specific element in the matrix
     * 
     * @param row The row index (0-3)
     * @param column The column index (0-4)
     * @param value The value to set
     */
    public void setElement(int row, int column, float value) {
        if (row < 0 || row > 3 || column < 0 || column > 4) {
            throw new IllegalArgumentException("Row/column out of range");
        }
        matrix[row * 5 + column] = value;
    }
    
    /**
     * Get a specific element from the matrix
     * 
     * @param row The row index (0-3)
     * @param column The column index (0-4)
     * @return The element value
     */
    public float getElement(int row, int column) {
        if (row < 0 || row > 3 || column < 0 || column > 4) {
            throw new IllegalArgumentException("Row/column out of range");
        }
        return matrix[row * 5 + column];
    }
    
    /**
     * Set the values in the matrix to change the saturation
     * 
     * @param sat The saturation value (0 = grayscale, 1 = identity, > 1 = increase saturation)
     */
    public void setSaturation(float sat) {
        reset();
        
        float[] mArray = matrix;
        
        final float invSat = 1 - sat;
        final float R = 0.213f * invSat;
        final float G = 0.715f * invSat;
        final float B = 0.072f * invSat;
        
        mArray[0] = R + sat;
        mArray[1] = G;
        mArray[2] = B;
        mArray[5] = R;
        mArray[6] = G + sat;
        mArray[7] = B;
        mArray[10] = R;
        mArray[11] = G;
        mArray[12] = B + sat;
    }
    
    /**
     * Set the values in the matrix to scale the colors
     * 
     * @param rScale The red scale factor
     * @param gScale The green scale factor
     * @param bScale The blue scale factor
     * @param aScale The alpha scale factor
     */
    public void setScale(float rScale, float gScale, float bScale, float aScale) {
        matrix[0] = rScale;
        matrix[6] = gScale;
        matrix[12] = bScale;
        matrix[18] = aScale;
        
        matrix[1] = matrix[2] = matrix[3] = matrix[4] = 0;
        matrix[5] = matrix[7] = matrix[8] = matrix[9] = 0;
        matrix[10] = matrix[11] = matrix[13] = matrix[14] = 0;
        matrix[15] = matrix[16] = matrix[17] = matrix[19] = 0;
    }
    
    /**
     * Post-concatenate two matrices
     * 
     * @param matrix The matrix to concatenate with this one
     */
    public void postConcat(ColorMatrix matrix) {
        if (matrix == null) {
            return;
        }
        
        float[] src = matrix.getArray();
        float[] dst = new float[20];
        
        for (int j = 0; j < 4; j++) {
            for (int i = 0; i < 5; i++) {
                dst[j * 5 + i] = this.matrix[j * 5] * src[i] +
                                this.matrix[j * 5 + 1] * src[i + 5] +
                                this.matrix[j * 5 + 2] * src[i + 10] +
                                this.matrix[j * 5 + 3] * src[i + 15] +
                                (i == 4 ? this.matrix[j * 5 + 4] : 0);
            }
        }
        
        System.arraycopy(dst, 0, this.matrix, 0, 20);
    }
    
    /**
     * Pre-concatenate two matrices
     * 
     * @param matrix The matrix to concatenate with this one
     */
    public void preConcat(ColorMatrix matrix) {
        if (matrix == null) {
            return;
        }
        
        float[] src = matrix.getArray();
        float[] dst = new float[20];
        
        for (int j = 0; j < 4; j++) {
            for (int i = 0; i < 5; i++) {
                dst[j * 5 + i] = src[j * 5] * this.matrix[i] +
                                src[j * 5 + 1] * this.matrix[i + 5] +
                                src[j * 5 + 2] * this.matrix[i + 10] +
                                src[j * 5 + 3] * this.matrix[i + 15] +
                                (i == 4 ? src[j * 5 + 4] : 0);
            }
        }
        
        System.arraycopy(dst, 0, this.matrix, 0, 20);
    }
}