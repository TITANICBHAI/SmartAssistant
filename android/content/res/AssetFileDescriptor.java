package android.content.res;

import android.os.Parcel;
import android.os.Parcelable;
import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;

/**
 * File descriptor of an entry in the AssetManager. This provides your own
 * opened FileDescriptor that can be used to read the data, as well as the
 * offset and length of that entry's data in the file.
 * 
 * This is a simple mock implementation for the purposes of simulating
 * the Android SDK AssetFileDescriptor class.
 */
public class AssetFileDescriptor implements Parcelable, Closeable {
    /**
     * The file descriptor that this object manages.
     */
    private final FileDescriptor mFd;
    
    /**
     * The offset within the file that the entry starts at.
     */
    private final long mStartOffset;
    
    /**
     * The length of the entry.
     */
    private final long mLength;
    
    /**
     * Construct a new AssetFileDescriptor based on given attributes.
     * 
     * @param fd The underlying file descriptor.
     * @param startOffset The offset into the file descriptor where the data
     *   starts.
     * @param length The number of bytes of the data, or -1 if unknown.
     */
    public AssetFileDescriptor(FileDescriptor fd, long startOffset, long length) {
        mFd = fd;
        mStartOffset = startOffset;
        mLength = length;
    }
    
    /**
     * Return the underlying file descriptor.
     */
    public FileDescriptor getFileDescriptor() {
        return mFd;
    }
    
    /**
     * Return the offset within the file descriptor of the asset's content.
     */
    public long getStartOffset() {
        return mStartOffset;
    }
    
    /**
     * Return the known length of the asset, or -1 if unknown.
     */
    public long getLength() {
        return mLength;
    }
    
    /**
     * Return a new auto-closeable that provides access to the data associated
     * with the file descriptor.
     */
    public java.io.FileInputStream createInputStream() throws IOException {
        return new java.io.FileInputStream(mFd);
    }
    
    /**
     * Create an auto-closeable that provides access to the data associated
     * with the file descriptor. The stream can be used for reading or writing.
     */
    public java.io.FileOutputStream createOutputStream() throws IOException {
        return new java.io.FileOutputStream(mFd);
    }
    
    /**
     * Close the underlying file descriptor.
     */
    public void close() throws IOException {
        // In a real implementation, would close the file descriptor
    }
    
    /**
     * Return the message as a string.
     */
    @Override
    public String toString() {
        return "AssetFileDescriptor: " + mFd;
    }
    
    @Override
    public int describeContents() {
        return mFd != null ? 1 : 0;
    }
    
    @Override
    public void writeToParcel(Parcel out, int flags) {
        // Mock implementation
    }
    
    public static final Parcelable.Creator<AssetFileDescriptor> CREATOR
            = new Parcelable.Creator<AssetFileDescriptor>() {
        public AssetFileDescriptor createFromParcel(Parcel in) {
            return new AssetFileDescriptor(null, 0, 0);
        }
        
        public AssetFileDescriptor[] newArray(int size) {
            return new AssetFileDescriptor[size];
        }
    };
}