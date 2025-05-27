package android.graphics;

/**
 * Mock implementation of Android Matrix class for development outside of Android.
 * The Matrix class holds a 3x3 matrix for transforming coordinates.
 */
public class Matrix {
    /**
     * Constant that can be passed to setScaleType.
     */
    public static final int MSCALE_X = 0;
    
    /**
     * Constant that can be passed to setScaleType.
     */
    public static final int MSKEW_X = 1;
    
    /**
     * Constant that can be passed to setScaleType.
     */
    public static final int MTRANS_X = 2;
    
    /**
     * Constant that can be passed to setScaleType.
     */
    public static final int MSKEW_Y = 3;
    
    /**
     * Constant that can be passed to setScaleType.
     */
    public static final int MSCALE_Y = 4;
    
    /**
     * Constant that can be passed to setScaleType.
     */
    public static final int MTRANS_Y = 5;
    
    /**
     * Constant that can be passed to setScaleType.
     */
    public static final int MPERSP_0 = 6;
    
    /**
     * Constant that can be passed to setScaleType.
     */
    public static final int MPERSP_1 = 7;
    
    /**
     * Constant that can be passed to setScaleType.
     */
    public static final int MPERSP_2 = 8;
    
    private final float[] mValues = new float[9];
    
    /**
     * Create an identity matrix
     */
    public Matrix() {
        reset();
    }
    
    /**
     * Create a matrix that is a copy of src
     * 
     * @param src The matrix to copy into this matrix
     */
    public Matrix(Matrix src) {
        if (src == null) {
            reset();
        } else {
            System.arraycopy(src.mValues, 0, mValues, 0, mValues.length);
        }
    }
    
    /**
     * Copy the values from src into this matrix
     * 
     * @param src The matrix to copy from
     */
    public void set(Matrix src) {
        if (src == null) {
            reset();
        } else {
            System.arraycopy(src.mValues, 0, mValues, 0, mValues.length);
        }
    }
    
    /**
     * Set this matrix to identity
     */
    public void reset() {
        mValues[MSCALE_X] = 1;
        mValues[MSKEW_X] = 0;
        mValues[MTRANS_X] = 0;
        mValues[MSKEW_Y] = 0;
        mValues[MSCALE_Y] = 1;
        mValues[MTRANS_Y] = 0;
        mValues[MPERSP_0] = 0;
        mValues[MPERSP_1] = 0;
        mValues[MPERSP_2] = 1;
    }
    
    /**
     * Set the matrix to translate by (dx, dy)
     * 
     * @param dx The translation in X
     * @param dy The translation in Y
     */
    public void setTranslate(float dx, float dy) {
        reset();
        mValues[MTRANS_X] = dx;
        mValues[MTRANS_Y] = dy;
    }
    
    /**
     * Set the matrix to scale by (sx, sy)
     * 
     * @param sx The scale in X
     * @param sy The scale in Y
     */
    public void setScale(float sx, float sy) {
        reset();
        mValues[MSCALE_X] = sx;
        mValues[MSCALE_Y] = sy;
    }
    
    /**
     * Set the matrix to scale by (sx, sy, px, py)
     * 
     * @param sx The scale in X
     * @param sy The scale in Y
     * @param px The pivot point X for scaling
     * @param py The pivot point Y for scaling
     */
    public void setScale(float sx, float sy, float px, float py) {
        reset();
        mValues[MSCALE_X] = sx;
        mValues[MSCALE_Y] = sy;
        mValues[MTRANS_X] = px - sx * px;
        mValues[MTRANS_Y] = py - sy * py;
    }
    
    /**
     * Set the matrix to rotate by degrees
     * 
     * @param degrees The angle in degrees
     */
    public void setRotate(float degrees) {
        reset();
        double radians = Math.toRadians(degrees);
        float sin = (float) Math.sin(radians);
        float cos = (float) Math.cos(radians);
        mValues[MSCALE_X] = cos;
        mValues[MSKEW_X] = -sin;
        mValues[MSKEW_Y] = sin;
        mValues[MSCALE_Y] = cos;
    }
    
    /**
     * Set the matrix to rotate by degrees and a pivot point at (px, py)
     * 
     * @param degrees The angle in degrees
     * @param px The pivot point X for rotation
     * @param py The pivot point Y for rotation
     */
    public void setRotate(float degrees, float px, float py) {
        reset();
        double radians = Math.toRadians(degrees);
        float sin = (float) Math.sin(radians);
        float cos = (float) Math.cos(radians);
        mValues[MSCALE_X] = cos;
        mValues[MSKEW_X] = -sin;
        mValues[MSKEW_Y] = sin;
        mValues[MSCALE_Y] = cos;
        mValues[MTRANS_X] = px - cos * px + sin * py;
        mValues[MTRANS_Y] = py - sin * px - cos * py;
    }
    
    /**
     * Set the matrix to skew
     * 
     * @param kx The skew factor in X
     * @param ky The skew factor in Y
     */
    public void setSkew(float kx, float ky) {
        reset();
        mValues[MSKEW_X] = kx;
        mValues[MSKEW_Y] = ky;
    }
    
    /**
     * Set the matrix to skew with pivot point
     * 
     * @param kx The skew factor in X
     * @param ky The skew factor in Y
     * @param px The pivot point X for skew
     * @param py The pivot point Y for skew
     */
    public void setSkew(float kx, float ky, float px, float py) {
        reset();
        mValues[MSKEW_X] = kx;
        mValues[MSKEW_Y] = ky;
        mValues[MTRANS_X] = -kx * py;
        mValues[MTRANS_Y] = -ky * px;
    }
    
    /**
     * Set this matrix to the concat of the two specified matrices, a and b.
     * In this case a is drawn before b.
     * 
     * @param a The first matrix
     * @param b The second matrix
     */
    public void setConcat(Matrix a, Matrix b) {
        // Mock implementation
        // In a real implementation, this would do matrix multiplication
        set(b);
    }
    
    /**
     * Preconcats the matrix with the specified translation.
     * M' = M * T(dx, dy)
     * 
     * @param dx The distance to translate in X
     * @param dy The distance to translate in Y
     */
    public boolean preTranslate(float dx, float dy) {
        mValues[MTRANS_X] += dx;
        mValues[MTRANS_Y] += dy;
        return true;
    }
    
    /**
     * Postconcats the matrix with the specified translation.
     * M' = T(dx, dy) * M
     * 
     * @param dx The distance to translate in X
     * @param dy The distance to translate in Y
     */
    public boolean postTranslate(float dx, float dy) {
        mValues[MTRANS_X] += dx;
        mValues[MTRANS_Y] += dy;
        return true;
    }
    
    /**
     * Preconcats the matrix with the specified scale.
     * M' = M * S(sx, sy)
     * 
     * @param sx The scale factor in X
     * @param sy The scale factor in Y
     */
    public boolean preScale(float sx, float sy) {
        mValues[MSCALE_X] *= sx;
        mValues[MSCALE_Y] *= sy;
        return true;
    }
    
    /**
     * Postconcats the matrix with the specified scale.
     * M' = S(sx, sy) * M
     * 
     * @param sx The scale factor in X
     * @param sy The scale factor in Y
     */
    public boolean postScale(float sx, float sy) {
        mValues[MSCALE_X] *= sx;
        mValues[MSCALE_Y] *= sy;
        return true;
    }
    
    /**
     * Preconcats the matrix with the specified rotation.
     * M' = M * R(degrees)
     * 
     * @param degrees The angle in degrees
     */
    public boolean preRotate(float degrees) {
        // Mock implementation
        return true;
    }
    
    /**
     * Postconcats the matrix with the specified rotation.
     * M' = R(degrees) * M
     * 
     * @param degrees The angle in degrees
     */
    public boolean postRotate(float degrees) {
        // Mock implementation
        return true;
    }
    
    /**
     * Copy 9 values from the matrix to the array
     * 
     * @param values The array to fill with the values from this matrix
     */
    public void getValues(float[] values) {
        if (values.length >= 9) {
            System.arraycopy(mValues, 0, values, 0, 9);
        }
    }
    
    /**
     * Set the matrix to the specified values
     * 
     * @param values The values to set in the matrix
     */
    public void setValues(float[] values) {
        if (values.length >= 9) {
            System.arraycopy(values, 0, mValues, 0, 9);
        }
    }
    
    /**
     * Returns true if the matrix is identity
     */
    public boolean isIdentity() {
        return mValues[MSCALE_X] == 1 && mValues[MSKEW_X] == 0 && mValues[MTRANS_X] == 0 &&
               mValues[MSKEW_Y] == 0 && mValues[MSCALE_Y] == 1 && mValues[MTRANS_Y] == 0 &&
               mValues[MPERSP_0] == 0 && mValues[MPERSP_1] == 0 && mValues[MPERSP_2] == 1;
    }
    
    /**
     * Returns true if the matrix is a simple translation
     */
    public boolean isTranslate() {
        return mValues[MSCALE_X] == 1 && mValues[MSKEW_X] == 0 &&
               mValues[MSKEW_Y] == 0 && mValues[MSCALE_Y] == 1 &&
               mValues[MPERSP_0] == 0 && mValues[MPERSP_1] == 0 && mValues[MPERSP_2] == 1;
    }
    
    /**
     * Returns true if the matrix only scales
     */
    public boolean isScale() {
        return mValues[MSKEW_X] == 0 && mValues[MTRANS_X] == 0 &&
               mValues[MSKEW_Y] == 0 && mValues[MTRANS_Y] == 0 &&
               mValues[MPERSP_0] == 0 && mValues[MPERSP_1] == 0 && mValues[MPERSP_2] == 1;
    }
    
    /**
     * Map points through this matrix and return the result
     * 
     * @param dst The array of destination points
     * @param dstIndex The index of the first destination point
     * @param src The array of source points
     * @param srcIndex The index of the first source point
     * @param pointCount The number of points to transform
     */
    public void mapPoints(float[] dst, int dstIndex, float[] src, int srcIndex, int pointCount) {
        // Mock implementation
        for (int i = 0; i < pointCount; i++) {
            float x = src[srcIndex + i * 2];
            float y = src[srcIndex + i * 2 + 1];
            float newX = x * mValues[MSCALE_X] + y * mValues[MSKEW_X] + mValues[MTRANS_X];
            float newY = x * mValues[MSKEW_Y] + y * mValues[MSCALE_Y] + mValues[MTRANS_Y];
            dst[dstIndex + i * 2] = newX;
            dst[dstIndex + i * 2 + 1] = newY;
        }
    }
    
    /**
     * Map points through this matrix and return the result
     * 
     * @param dst The array of points for both input and output
     */
    public void mapPoints(float[] dst) {
        mapPoints(dst, 0, dst, 0, dst.length / 2);
    }
    
    /**
     * Map a rect through this matrix and return the result
     * 
     * @param dst The resulting rect after applying the matrix
     * @param src The original rect
     */
    public boolean mapRect(RectF dst, RectF src) {
        float[] src4 = {src.left, src.top, src.right, src.top, src.right, src.bottom, src.left, src.bottom};
        float[] dst4 = new float[8];
        mapPoints(dst4, 0, src4, 0, 4);
        
        dst.left = Math.min(Math.min(dst4[0], dst4[2]), Math.min(dst4[4], dst4[6]));
        dst.top = Math.min(Math.min(dst4[1], dst4[3]), Math.min(dst4[5], dst4[7]));
        dst.right = Math.max(Math.max(dst4[0], dst4[2]), Math.max(dst4[4], dst4[6]));
        dst.bottom = Math.max(Math.max(dst4[1], dst4[3]), Math.max(dst4[5], dst4[7]));
        return true;
    }
    
    /**
     * Map a rect through this matrix and return the result
     * 
     * @param rect The rect to map and store the result
     */
    public boolean mapRect(RectF rect) {
        return mapRect(rect, rect);
    }
    
    /**
     * Return the mean radius of a circle after mapping through the matrix.
     * 
     * @param radius The original radius of the circle
     */
    public float mapRadius(float radius) {
        return radius * (float) Math.sqrt((mValues[MSCALE_X] * mValues[MSCALE_X] + mValues[MSKEW_Y] * mValues[MSKEW_Y] +
                                          mValues[MSKEW_X] * mValues[MSKEW_X] + mValues[MSCALE_Y] * mValues[MSCALE_Y]) / 2);
    }
    
    /**
     * Invert this matrix and return success
     */
    public boolean invert(Matrix inverse) {
        // Mock implementation
        if (inverse != null) {
            inverse.set(this);
            inverse.mValues[MTRANS_X] = -mValues[MTRANS_X];
            inverse.mValues[MTRANS_Y] = -mValues[MTRANS_Y];
        }
        return true;
    }
    
    @Override
    public String toString() {
        return "Matrix{" +
                "MSCALE_X=" + mValues[MSCALE_X] +
                ", MSKEW_X=" + mValues[MSKEW_X] +
                ", MTRANS_X=" + mValues[MTRANS_X] +
                ", MSKEW_Y=" + mValues[MSKEW_Y] +
                ", MSCALE_Y=" + mValues[MSCALE_Y] +
                ", MTRANS_Y=" + mValues[MTRANS_Y] +
                ", MPERSP_0=" + mValues[MPERSP_0] +
                ", MPERSP_1=" + mValues[MPERSP_1] +
                ", MPERSP_2=" + mValues[MPERSP_2] +
                "}";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        
        Matrix other = (Matrix) obj;
        for (int i = 0; i < 9; i++) {
            if (Float.compare(mValues[i], other.mValues[i]) != 0) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public int hashCode() {
        int result = 0;
        for (int i = 0; i < 9; i++) {
            result = 31 * result + Float.floatToIntBits(mValues[i]);
        }
        return result;
    }
}