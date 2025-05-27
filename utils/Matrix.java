package utils;

/**
 * The Matrix class holds a 3x3 matrix for transforming coordinates.
 */
public class Matrix {
    private final float[] mValues = new float[9];
    
    /**
     * Create a new identity matrix
     */
    public Matrix() {
        reset();
    }
    
    /**
     * Create a new matrix with the same values as src
     * @param src The source matrix
     */
    public Matrix(Matrix src) {
        if (src == null) {
            reset();
        } else {
            System.arraycopy(src.mValues, 0, mValues, 0, mValues.length);
        }
    }
    
    /**
     * Reset this matrix to the identity matrix
     */
    public void reset() {
        mValues[0] = 1;  // scale x
        mValues[1] = 0;  // skew y
        mValues[2] = 0;  // translate x
        mValues[3] = 0;  // skew x
        mValues[4] = 1;  // scale y
        mValues[5] = 0;  // translate y
        mValues[6] = 0;  // perspective 0
        mValues[7] = 0;  // perspective 1
        mValues[8] = 1;  // perspective 2
    }
    
    /**
     * Check if this matrix is the identity matrix
     * @return True if this matrix is identity
     */
    public boolean isIdentity() {
        return mValues[0] == 1 && mValues[1] == 0 && mValues[2] == 0 &&
               mValues[3] == 0 && mValues[4] == 1 && mValues[5] == 0 &&
               mValues[6] == 0 && mValues[7] == 0 && mValues[8] == 1;
    }
    
    /**
     * Set this matrix to translate by (dx, dy)
     * @param dx The x translation
     * @param dy The y translation
     */
    public void setTranslate(float dx, float dy) {
        reset();
        mValues[2] = dx;  // translate x
        mValues[5] = dy;  // translate y
    }
    
    /**
     * Set this matrix to scale by (sx, sy)
     * @param sx The x scaling factor
     * @param sy The y scaling factor
     */
    public void setScale(float sx, float sy) {
        reset();
        mValues[0] = sx;  // scale x
        mValues[4] = sy;  // scale y
    }
    
    /**
     * Set this matrix to rotate by degrees
     * @param degrees The angle of rotation in degrees
     */
    public void setRotate(float degrees) {
        reset();
        double radians = Math.toRadians(degrees);
        float sin = (float) Math.sin(radians);
        float cos = (float) Math.cos(radians);
        mValues[0] = cos;   // scale x
        mValues[1] = -sin;  // skew y
        mValues[3] = sin;   // skew x
        mValues[4] = cos;   // scale y
    }
    
    /**
     * Copy the 9 values from src into this matrix
     * @param src The source array
     */
    public void setValues(float[] src) {
        if (src == null || src.length < 9) {
            throw new IllegalArgumentException("Array must be at least 9 long");
        }
        System.arraycopy(src, 0, mValues, 0, 9);
    }
    
    /**
     * Copy the 9 values from this matrix into dst
     * @param dst The destination array
     */
    public void getValues(float[] dst) {
        if (dst == null || dst.length < 9) {
            throw new IllegalArgumentException("Array must be at least 9 long");
        }
        System.arraycopy(mValues, 0, dst, 0, 9);
    }
    
    /**
     * Apply this matrix to the array of 2D points specified by src, and write
     * the transformed points into the array of points specified by dst.
     * @param dst The destination array
     * @param dstIndex The index into dst where the first point is written
     * @param src The source array
     * @param srcIndex The index into src where the first point is read
     * @param pointCount The number of points to transform
     */
    public void mapPoints(float[] dst, int dstIndex, float[] src, int srcIndex, int pointCount) {
        if (pointCount < 0) {
            throw new IllegalArgumentException("pointCount < 0");
        }
        if (src == null || dst == null) {
            throw new NullPointerException("src or dst is null");
        }
        if (src.length - srcIndex < pointCount * 2 || dst.length - dstIndex < pointCount * 2) {
            throw new ArrayIndexOutOfBoundsException("src or dst too small");
        }
        
        for (int i = 0; i < pointCount; i++) {
            float x = src[srcIndex + i * 2];
            float y = src[srcIndex + i * 2 + 1];
            
            // Apply the transformation
            float tx = mValues[0] * x + mValues[1] * y + mValues[2];
            float ty = mValues[3] * x + mValues[4] * y + mValues[5];
            
            dst[dstIndex + i * 2] = tx;
            dst[dstIndex + i * 2 + 1] = ty;
        }
    }
    
    /**
     * Apply this matrix to the array of 2D points, and write the transformed
     * points back into the array
     * @param pts The points to transform
     */
    public void mapPoints(float[] pts) {
        mapPoints(pts, 0, pts, 0, pts.length / 2);
    }
    
    /**
     * Pre-scale this matrix by (sx, sy)
     * @param sx The x scaling factor
     * @param sy The y scaling factor
     * @return This matrix for chaining
     */
    public Matrix preScale(float sx, float sy) {
        mValues[0] *= sx;  // scale x
        mValues[1] *= sx;  // skew y
        mValues[2] *= sx;  // translate x
        mValues[3] *= sy;  // skew x
        mValues[4] *= sy;  // scale y
        mValues[5] *= sy;  // translate y
        return this;
    }
    
    /**
     * Pre-translate this matrix by (dx, dy)
     * @param dx The x translation
     * @param dy The y translation
     * @return This matrix for chaining
     */
    public Matrix preTranslate(float dx, float dy) {
        mValues[2] += dx * mValues[0] + dy * mValues[1];  // translate x
        mValues[5] += dx * mValues[3] + dy * mValues[4];  // translate y
        mValues[8] += dx * mValues[6] + dy * mValues[7];  // perspective 2
        return this;
    }
    
    /**
     * Post-scale this matrix by (sx, sy)
     * @param sx The x scaling factor
     * @param sy The y scaling factor
     * @return This matrix for chaining
     */
    public Matrix postScale(float sx, float sy) {
        mValues[0] *= sx;  // scale x
        mValues[3] *= sx;  // skew x
        mValues[6] *= sx;  // perspective 0
        mValues[1] *= sy;  // skew y
        mValues[4] *= sy;  // scale y
        mValues[7] *= sy;  // perspective 1
        return this;
    }
    
    /**
     * Post-translate this matrix by (dx, dy)
     * @param dx The x translation
     * @param dy The y translation
     * @return This matrix for chaining
     */
    public Matrix postTranslate(float dx, float dy) {
        mValues[2] += dx;  // translate x
        mValues[5] += dy;  // translate y
        return this;
    }
    
    /**
     * Post-rotate this matrix by degrees
     * @param degrees The angle of rotation in degrees
     * @return This matrix for chaining
     */
    public Matrix postRotate(float degrees) {
        Matrix temp = new Matrix();
        temp.setRotate(degrees);
        postConcat(temp);
        return this;
    }
    
    /**
     * Post-concatenate this matrix with the specified matrix.
     * This matrix = this matrix * matrix
     * @param matrix The matrix to post-concatenate with this matrix
     * @return This matrix for chaining
     */
    public Matrix postConcat(Matrix matrix) {
        float[] a = mValues;
        float[] b = new float[9];
        matrix.getValues(b);
        
        final float[] result = new float[9];
        
        result[0] = a[0] * b[0] + a[1] * b[3] + a[2] * b[6];
        result[1] = a[0] * b[1] + a[1] * b[4] + a[2] * b[7];
        result[2] = a[0] * b[2] + a[1] * b[5] + a[2] * b[8];
        
        result[3] = a[3] * b[0] + a[4] * b[3] + a[5] * b[6];
        result[4] = a[3] * b[1] + a[4] * b[4] + a[5] * b[7];
        result[5] = a[3] * b[2] + a[4] * b[5] + a[5] * b[8];
        
        result[6] = a[6] * b[0] + a[7] * b[3] + a[8] * b[6];
        result[7] = a[6] * b[1] + a[7] * b[4] + a[8] * b[7];
        result[8] = a[6] * b[2] + a[7] * b[5] + a[8] * b[8];
        
        System.arraycopy(result, 0, mValues, 0, 9);
        return this;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Matrix) {
            Matrix other = (Matrix) obj;
            for (int i = 0; i < 9; i++) {
                if (mValues[i] != other.mValues[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        // Simple hash code implementation
        int hash = 0;
        for (float v : mValues) {
            hash = 31 * hash + Float.floatToIntBits(v);
        }
        return hash;
    }
    
    @Override
    public String toString() {
        return "Matrix{" +
               "[" + mValues[0] + ", " + mValues[1] + ", " + mValues[2] + "], " +
               "[" + mValues[3] + ", " + mValues[4] + ", " + mValues[5] + "], " +
               "[" + mValues[6] + ", " + mValues[7] + ", " + mValues[8] + "]" +
               "}";
    }
}