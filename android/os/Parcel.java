package android.os;

/**
 * Mock implementation of Android's Parcel class.
 */
public class Parcel {
    private int dataPosition;
    private int dataCapacity;
    private byte[] data;
    
    /**
     * Private constructor, use {@link #obtain()} to create a new instance.
     */
    private Parcel() {
        this.dataPosition = 0;
        this.dataCapacity = 0;
        this.data = new byte[1024]; // Initial capacity
    }
    
    /**
     * Obtain a new instance of a Parcel.
     * 
     * @return A new Parcel instance
     */
    public static Parcel obtain() {
        return new Parcel();
    }
    
    /**
     * Write a byte value to the parcel.
     * 
     * @param val The value to write
     */
    public void writeByte(byte val) {
        ensureCapacity(1);
        data[dataPosition++] = val;
    }
    
    /**
     * Write a byte array to the parcel.
     * 
     * @param b The byte array to write
     */
    public void writeByteArray(byte[] b) {
        if (b == null) {
            writeInt(-1);
            return;
        }
        writeInt(b.length);
        writeByteArray(b, 0, b.length);
    }
    
    /**
     * Write a portion of a byte array to the parcel.
     * 
     * @param b The byte array to write
     * @param offset The offset into the array
     * @param len The number of bytes to write
     */
    public void writeByteArray(byte[] b, int offset, int len) {
        if (b == null) {
            writeInt(-1);
            return;
        }
        ensureCapacity(len);
        System.arraycopy(b, offset, data, dataPosition, len);
        dataPosition += len;
    }
    
    /**
     * Write an int value to the parcel.
     * 
     * @param val The value to write
     */
    public void writeInt(int val) {
        ensureCapacity(4);
        data[dataPosition++] = (byte) (val & 0xff);
        data[dataPosition++] = (byte) ((val >> 8) & 0xff);
        data[dataPosition++] = (byte) ((val >> 16) & 0xff);
        data[dataPosition++] = (byte) ((val >> 24) & 0xff);
    }
    
    /**
     * Write a long value to the parcel.
     * 
     * @param val The value to write
     */
    public void writeLong(long val) {
        ensureCapacity(8);
        data[dataPosition++] = (byte) (val & 0xff);
        data[dataPosition++] = (byte) ((val >> 8) & 0xff);
        data[dataPosition++] = (byte) ((val >> 16) & 0xff);
        data[dataPosition++] = (byte) ((val >> 24) & 0xff);
        data[dataPosition++] = (byte) ((val >> 32) & 0xff);
        data[dataPosition++] = (byte) ((val >> 40) & 0xff);
        data[dataPosition++] = (byte) ((val >> 48) & 0xff);
        data[dataPosition++] = (byte) ((val >> 56) & 0xff);
    }
    
    /**
     * Write a float value to the parcel.
     * 
     * @param val The value to write
     */
    public void writeFloat(float val) {
        writeInt(Float.floatToIntBits(val));
    }
    
    /**
     * Write a double value to the parcel.
     * 
     * @param val The value to write
     */
    public void writeDouble(double val) {
        writeLong(Double.doubleToLongBits(val));
    }
    
    /**
     * Write a boolean value to the parcel.
     * 
     * @param val The value to write
     */
    public void writeBoolean(boolean val) {
        writeByte((byte) (val ? 1 : 0));
    }
    
    /**
     * Write a String value to the parcel.
     * 
     * @param val The value to write
     */
    public void writeString(String val) {
        if (val == null) {
            writeInt(-1);
            return;
        }
        byte[] bytes = val.getBytes();
        writeInt(bytes.length);
        writeByteArray(bytes);
    }
    
    /**
     * Write a String array to the parcel.
     * 
     * @param val The values to write
     */
    public void writeStringArray(String[] val) {
        if (val == null) {
            writeInt(-1);
            return;
        }
        writeInt(val.length);
        for (String s : val) {
            writeString(s);
        }
    }
    
    /**
     * Write a Parcelable value to the parcel.
     * 
     * @param val The value to write
     * @param flags The flags to use
     */
    public void writeParcelable(Parcelable val, int flags) {
        if (val == null) {
            writeInt(-1);
            return;
        }
        writeInt(1);
        writeString(val.getClass().getName());
        val.writeToParcel(this, flags);
    }
    
    /**
     * Write a Parcelable array to the parcel.
     * 
     * @param val The values to write
     * @param flags The flags to use
     */
    public void writeParcelableArray(Parcelable[] val, int flags) {
        if (val == null) {
            writeInt(-1);
            return;
        }
        writeInt(val.length);
        for (Parcelable p : val) {
            writeParcelable(p, flags);
        }
    }
    
    /**
     * Read a byte value from the parcel.
     * 
     * @return The byte value
     */
    public byte readByte() {
        return data[dataPosition++];
    }
    
    /**
     * Read a byte array from the parcel.
     * 
     * @return The byte array
     */
    public byte[] readByteArray() {
        int len = readInt();
        if (len < 0) {
            return null;
        }
        byte[] result = new byte[len];
        readByteArray(result, 0, len);
        return result;
    }
    
    /**
     * Read a byte array from the parcel into a given array.
     * 
     * @param dest The array to read into
     * @param offset The offset into the array
     * @param len The number of bytes to read
     */
    public void readByteArray(byte[] dest, int offset, int len) {
        System.arraycopy(data, dataPosition, dest, offset, len);
        dataPosition += len;
    }
    
    /**
     * Read an int value from the parcel.
     * 
     * @return The int value
     */
    public int readInt() {
        int result = ((data[dataPosition] & 0xff)) |
                    ((data[dataPosition + 1] & 0xff) << 8) |
                    ((data[dataPosition + 2] & 0xff) << 16) |
                    ((data[dataPosition + 3]) << 24);
        dataPosition += 4;
        return result;
    }
    
    /**
     * Read a long value from the parcel.
     * 
     * @return The long value
     */
    public long readLong() {
        long result = ((long) (data[dataPosition] & 0xff)) |
                    ((long) (data[dataPosition + 1] & 0xff) << 8) |
                    ((long) (data[dataPosition + 2] & 0xff) << 16) |
                    ((long) (data[dataPosition + 3] & 0xff) << 24) |
                    ((long) (data[dataPosition + 4] & 0xff) << 32) |
                    ((long) (data[dataPosition + 5] & 0xff) << 40) |
                    ((long) (data[dataPosition + 6] & 0xff) << 48) |
                    ((long) (data[dataPosition + 7]) << 56);
        dataPosition += 8;
        return result;
    }
    
    /**
     * Read a float value from the parcel.
     * 
     * @return The float value
     */
    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }
    
    /**
     * Read a double value from the parcel.
     * 
     * @return The double value
     */
    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }
    
    /**
     * Read a boolean value from the parcel.
     * 
     * @return The boolean value
     */
    public boolean readBoolean() {
        return readByte() != 0;
    }
    
    /**
     * Read a String value from the parcel.
     * 
     * @return The String value
     */
    public String readString() {
        int len = readInt();
        if (len < 0) {
            return null;
        }
        byte[] bytes = new byte[len];
        readByteArray(bytes, 0, len);
        return new String(bytes);
    }
    
    /**
     * Read a String array from the parcel.
     * 
     * @return The String array
     */
    public String[] createStringArray() {
        int len = readInt();
        if (len < 0) {
            return null;
        }
        String[] result = new String[len];
        for (int i = 0; i < len; i++) {
            result[i] = readString();
        }
        return result;
    }
    
    /**
     * Read a String array from the parcel.
     *
     * @return The String array
     */
    public String[] readStringArray() {
        return createStringArray();
    }
    
    /**
     * Read a typed array from the parcel, using the given creator.
     *
     * @param creator The creator to use
     * @return The typed array
     */
    public <T> T[] createTypedArray(Parcelable.Creator<T> creator) {
        int length = readInt();
        if (length == -1) {
            return null;
        }
        
        @SuppressWarnings("unchecked")
        T[] result = (T[]) java.lang.reflect.Array.newInstance(
                creator.getClass().getDeclaringClass(), length);
        
        for (int i = 0; i < length; i++) {
            result[i] = creator.createFromParcel(this);
        }
        return result;
    }
    
    /**
     * Write a typed array to the parcel.
     *
     * @param val The array to write
     * @param flags Flags for writing
     */
    public <T extends Parcelable> void writeTypedArray(T[] val, int flags) {
        if (val == null) {
            writeInt(-1);
            return;
        }
        
        writeInt(val.length);
        for (T item : val) {
            if (item == null) {
                writeInt(0);
            } else {
                writeInt(1);
                item.writeToParcel(this, flags);
            }
        }
    }
    
    /**
     * Read a Bundle from the parcel.
     *
     * @return The Bundle
     */
    public Bundle readBundle() {
        return readBundle(null);
    }
    
    /**
     * Read a Bundle from the parcel.
     *
     * @param loader The ClassLoader to use
     * @return The Bundle
     */
    public Bundle readBundle(ClassLoader loader) {
        int marker = readInt();
        if (marker < 0) {
            return null;
        }
        Bundle bundle = new Bundle();
        // Mock implementation just returns an empty bundle
        return bundle;
    }
    
    /**
     * Read a Parcelable value from the parcel.
     * 
     * @param loader The ClassLoader to use
     * @return The Parcelable value
     */
    public <T extends Parcelable> T readParcelable(ClassLoader loader) {
        int marker = readInt();
        if (marker < 0) {
            return null;
        }
        String className = readString();
        try {
            Class<?> clazz = loader.loadClass(className);
            Parcelable.Creator<?> creator = (Parcelable.Creator<?>) clazz.getField("CREATOR").get(null);
            return (T) creator.createFromParcel(this);
        } catch (Exception e) {
            throw new RuntimeException("Could not create Parcelable: " + e.getMessage());
        }
    }
    
    /**
     * Read a Parcelable array from the parcel.
     * 
     * @param loader The ClassLoader to use
     * @return The Parcelable array
     */
    public Parcelable[] readParcelableArray(ClassLoader loader) {
        int len = readInt();
        if (len < 0) {
            return null;
        }
        Parcelable[] result = new Parcelable[len];
        for (int i = 0; i < len; i++) {
            result[i] = readParcelable(loader);
        }
        return result;
    }
    
    /**
     * Read a String list from the parcel.
     * 
     * @param list The list to read into, or null to create a new list
     * @return The list
     */
    public java.util.ArrayList<String> readStringList(java.util.ArrayList<String> list) {
        int len = readInt();
        if (len < 0) {
            return null;
        }
        
        java.util.ArrayList<String> result = list;
        if (result == null) {
            result = new java.util.ArrayList<String>(len);
        } else {
            result.clear();
        }
        
        for (int i = 0; i < len; i++) {
            result.add(readString());
        }
        
        return result;
    }
    
    /**
     * Write a String list to the parcel.
     * 
     * @param list The list to write
     */
    public void writeStringList(java.util.ArrayList<String> list) {
        if (list == null) {
            writeInt(-1);
            return;
        }
        
        int len = list.size();
        writeInt(len);
        
        for (int i = 0; i < len; i++) {
            writeString(list.get(i));
        }
    }
    
    /**
     * Write a generic List to the parcel.
     * 
     * @param list The list to write
     */
    public void writeList(java.util.List<?> list) {
        if (list == null) {
            writeInt(-1);
            return;
        }
        
        int len = list.size();
        writeInt(len);
        
        for (int i = 0; i < len; i++) {
            writeValue(list.get(i));
        }
    }
    
    /**
     * Read a generic List from the parcel.
     * 
     * @param list The list to read into, or null to create a new list
     * @param loader The ClassLoader to use
     * @return The list
     */
    public void readList(java.util.List list, ClassLoader loader) {
        int len = readInt();
        if (len < 0) {
            return;
        }
        
        list.clear();
        
        for (int i = 0; i < len; i++) {
            list.add(readValue(loader));
        }
    }
    
    /**
     * Write a generic value to the parcel.
     * 
     * @param value The value to write
     */
    public void writeValue(Object value) {
        if (value == null) {
            writeInt(-1);
            return;
        }
        
        if (value instanceof String) {
            writeInt(1); // String marker
            writeString((String) value);
        } else if (value instanceof Integer) {
            writeInt(2); // Integer marker
            writeInt((Integer) value);
        } else if (value instanceof Boolean) {
            writeInt(3); // Boolean marker
            writeBoolean((Boolean) value);
        } else if (value instanceof Parcelable) {
            writeInt(4); // Parcelable marker
            writeParcelable((Parcelable) value, 0);
        } else if (value instanceof CharSequence) {
            writeInt(5); // CharSequence marker
            writeString(value.toString());
        } else {
            // For unsupported types, write a placeholder
            writeInt(-1);
        }
    }
    
    /**
     * Read a generic value from the parcel.
     * 
     * @param loader The ClassLoader to use
     * @return The value
     */
    public Object readValue(ClassLoader loader) {
        int type = readInt();
        
        switch (type) {
            case -1:
                return null;
            case 1:
                return readString();
            case 2:
                return readInt();
            case 3:
                return readBoolean();
            case 4:
                return readParcelable(loader);
            case 5:
                return readString(); // CharSequence as String
            default:
                return null;
        }
    }
    
    /**
     * Write a Bundle to the parcel.
     * 
     * @param bundle The bundle to write
     */
    public void writeBundle(Bundle bundle) {
        if (bundle == null) {
            writeInt(-1);
            return;
        }
        
        writeInt(1); // Non-null marker
        // In a real implementation, this would write all bundle contents
        // For our mock, we'll just write an empty bundle marker
    }
    
    /**
     * Get the current data position in the parcel.
     * 
     * @return The data position
     */
    public int dataPosition() {
        return dataPosition;
    }
    
    /**
     * Set the data position in the parcel.
     * 
     * @param pos The data position
     */
    public void setDataPosition(int pos) {
        dataPosition = pos;
    }
    
    /**
     * Get the data size of the parcel.
     * 
     * @return The data size
     */
    public int dataSize() {
        return dataCapacity;
    }
    
    /**
     * Recycle this parcel.
     */
    public void recycle() {
        dataPosition = 0;
        dataCapacity = 0;
    }
    
    /**
     * Ensure the capacity of the parcel.
     * 
     * @param size The size to ensure
     */
    private void ensureCapacity(int size) {
        if (dataPosition + size > data.length) {
            byte[] newData = new byte[data.length * 2];
            System.arraycopy(data, 0, newData, 0, dataPosition);
            data = newData;
        }
        if (dataPosition + size > dataCapacity) {
            dataCapacity = dataPosition + size;
        }
    }
    
    /**
     * Creator for Parcel.
     */
    public static final Parcelable.Creator<Parcel> CREATOR = new Parcelable.Creator<Parcel>() {
        @Override
        public Parcel createFromParcel(Parcel source) {
            return new Parcel();
        }
        
        @Override
        public Parcel[] newArray(int size) {
            return new Parcel[size];
        }
    };
}