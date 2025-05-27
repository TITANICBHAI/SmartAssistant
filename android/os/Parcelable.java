package android.os;

/**
 * Mock implementation of Android Parcelable interface for development outside of Android.
 * Interface for classes whose instances can be written to and restored from a Parcel.
 */
public interface Parcelable {
    /**
     * Describe the kinds of special objects contained in this Parcelable instance's marshaled representation.
     * 
     * @return A bitmask indicating the set of special object types marshaled by this Parcelable object.
     */
    int describeContents();
    
    /**
     * Flatten this object in to a Parcel.
     * 
     * @param dest The Parcel in which the object should be written.
     * @param flags Additional flags about how the object should be written.
     */
    void writeToParcel(Parcel dest, int flags);
    
    /**
     * Interface that must be implemented and provided as a public CREATOR field
     * that generates instances of your Parcelable class from a Parcel.
     * 
     * @param <T> The type of the Parcelable object.
     */
    interface Creator<T> {
        /**
         * Create a new instance of the Parcelable class, instantiating it from the given Parcel.
         * 
         * @param source The Parcel to read the object's data from.
         * @return A new instance of the Parcelable class.
         */
        T createFromParcel(Parcel source);
        
        /**
         * Create a new array of the Parcelable class.
         * 
         * @param size Size of the array to create.
         * @return An array of the Parcelable class.
         */
        T[] newArray(int size);
    }
    
    /**
     * Flag for use with writeToParcel: the object being written is a return value.
     */
    int PARCELABLE_WRITE_RETURN_VALUE = 0x0001;
    
    /**
     * Flag for use with writeToParcel: the object being written is a file descriptor.
     */
    int CONTENTS_FILE_DESCRIPTOR = 0x0001;
}