package android.content.res;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

/**
 * Mock implementation of Android Configuration class for development outside of Android.
 * Describes various configuration information about the device.
 */
public final class Configuration implements Parcelable, Comparable<Configuration> {
    /**
     * SCREENLAYOUT_SIZE_MASK: bits defining the screen size.
     * SCREENLAYOUT_SIZE_UNDEFINED, SCREENLAYOUT_SIZE_SMALL, SCREENLAYOUT_SIZE_NORMAL,
     * SCREENLAYOUT_SIZE_LARGE, or SCREENLAYOUT_SIZE_XLARGE.
     */
    public static final int SCREENLAYOUT_SIZE_MASK = 0x0f;
    public static final int SCREENLAYOUT_SIZE_UNDEFINED = 0x00;
    public static final int SCREENLAYOUT_SIZE_SMALL = 0x01;
    public static final int SCREENLAYOUT_SIZE_NORMAL = 0x02;
    public static final int SCREENLAYOUT_SIZE_LARGE = 0x03;
    public static final int SCREENLAYOUT_SIZE_XLARGE = 0x04;
    
    /**
     * SCREENLAYOUT_LONG_MASK: bits defining whether the screen is long.
     * SCREENLAYOUT_LONG_UNDEFINED, SCREENLAYOUT_LONG_NO, or SCREENLAYOUT_LONG_YES.
     */
    public static final int SCREENLAYOUT_LONG_MASK = 0x30;
    public static final int SCREENLAYOUT_LONG_UNDEFINED = 0x00;
    public static final int SCREENLAYOUT_LONG_NO = 0x10;
    public static final int SCREENLAYOUT_LONG_YES = 0x20;
    
    /**
     * SCREENLAYOUT_LAYOUTDIR_MASK: bits defining the layout direction.
     * SCREENLAYOUT_LAYOUTDIR_UNDEFINED, SCREENLAYOUT_LAYOUTDIR_LTR, or SCREENLAYOUT_LAYOUTDIR_RTL.
     */
    public static final int SCREENLAYOUT_LAYOUTDIR_MASK = 0xC0;
    public static final int SCREENLAYOUT_LAYOUTDIR_UNDEFINED = 0x00;
    public static final int SCREENLAYOUT_LAYOUTDIR_LTR = 0x40;
    public static final int SCREENLAYOUT_LAYOUTDIR_RTL = 0x80;
    
    /**
     * Describes the overall layout of the screen. Currently, the following
     * values are recognized:
     * <ul>
     *   <li>{@link #SCREENLAYOUT_SIZE_MASK} defines the size of the screen,
     *       which is one of
     *       {@link #SCREENLAYOUT_SIZE_UNDEFINED}, {@link #SCREENLAYOUT_SIZE_SMALL},
     *       {@link #SCREENLAYOUT_SIZE_NORMAL}, {@link #SCREENLAYOUT_SIZE_LARGE}, or
     *       {@link #SCREENLAYOUT_SIZE_XLARGE}.
     *   <li>{@link #SCREENLAYOUT_LONG_MASK} defines whether the screen is
     *       of a longer more landscape-style form factor, which is one of
     *       {@link #SCREENLAYOUT_LONG_UNDEFINED}, {@link #SCREENLAYOUT_LONG_NO},
     *       or {@link #SCREENLAYOUT_LONG_YES}.
     *   <li>{@link #SCREENLAYOUT_LAYOUTDIR_MASK} defines whether the screen layout
     *       direction is either left-to-right or right-to-left, which is one of
     *       {@link #SCREENLAYOUT_LAYOUTDIR_UNDEFINED}, {@link #SCREENLAYOUT_LAYOUTDIR_LTR},
     *       or {@link #SCREENLAYOUT_LAYOUTDIR_RTL}.
     * </ul>
     */
    public int screenLayout;
    
    /**
     * Bit mask of UI Mode: bits for the ui mode type.
     */
    public static final int UI_MODE_TYPE_MASK = 0x0f;
    public static final int UI_MODE_TYPE_UNDEFINED = 0x00;
    public static final int UI_MODE_TYPE_NORMAL = 0x01;
    public static final int UI_MODE_TYPE_DESK = 0x02;
    public static final int UI_MODE_TYPE_CAR = 0x03;
    public static final int UI_MODE_TYPE_TELEVISION = 0x04;
    public static final int UI_MODE_TYPE_APPLIANCE = 0x05;
    public static final int UI_MODE_TYPE_WATCH = 0x06;
    public static final int UI_MODE_TYPE_VR_HEADSET = 0x07;
    
    /**
     * Bit mask of UI Mode: bits for the ui night mode.
     */
    public static final int UI_MODE_NIGHT_MASK = 0x30;
    public static final int UI_MODE_NIGHT_UNDEFINED = 0x00;
    public static final int UI_MODE_NIGHT_NO = 0x10;
    public static final int UI_MODE_NIGHT_YES = 0x20;
    
    /**
     * Defines the current UI mode, as per {@link #UI_MODE_TYPE_MASK} and
     * {@link #UI_MODE_NIGHT_MASK}.
     */
    public int uiMode;
    
    /**
     * Current user preference for the direction of the layout.
     */
    public int layoutDirection;
    
    /**
     * Current device orientation.
     */
    public int orientation;
    
    /**
     * The primary language/locale of the user interface.
     */
    public String locale;
    
    /**
     * Construct an invalid Configuration, which must be filled in using
     * the various set methods.
     */
    public Configuration() {
        setToDefaults();
    }
    
    /**
     * Set to the default configuration.
     */
    public void setToDefaults() {
        screenLayout = SCREENLAYOUT_SIZE_NORMAL | SCREENLAYOUT_LONG_NO | SCREENLAYOUT_LAYOUTDIR_LTR;
        uiMode = UI_MODE_TYPE_NORMAL | UI_MODE_NIGHT_NO;
        layoutDirection = 0;
        orientation = 0;
        locale = "en-US";
    }
    
    /**
     * Copy the fields from config into this Configuration object.
     * 
     * @param config The Configuration object to copy.
     */
    public void setTo(@NonNull Configuration config) {
        if (config == null) {
            return;
        }
        
        screenLayout = config.screenLayout;
        uiMode = config.uiMode;
        layoutDirection = config.layoutDirection;
        orientation = config.orientation;
        locale = config.locale;
    }
    
    /**
     * Return a nice readable representation of this object.
     */
    @Override
    public String toString() {
        return "Configuration{" +
               "screenLayout=" + screenLayout +
               ", uiMode=" + uiMode +
               ", layoutDirection=" + layoutDirection +
               ", orientation=" + orientation +
               ", locale='" + locale + '\'' +
               '}';
    }
    
    /**
     * Compares this Configuration to the specified object.
     * 
     * @param o The object to compare to this instance.
     * @return true if the specified object is equal to this Configuration, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Configuration that = (Configuration) o;
        
        if (screenLayout != that.screenLayout) return false;
        if (uiMode != that.uiMode) return false;
        if (layoutDirection != that.layoutDirection) return false;
        if (orientation != that.orientation) return false;
        return locale != null ? locale.equals(that.locale) : that.locale == null;
    }
    
    /**
     * Returns a hash code for this Configuration.
     * 
     * @return A hash code for this Configuration.
     */
    @Override
    public int hashCode() {
        int result = screenLayout;
        result = 31 * result + uiMode;
        result = 31 * result + layoutDirection;
        result = 31 * result + orientation;
        result = 31 * result + (locale != null ? locale.hashCode() : 0);
        return result;
    }
    
    /**
     * Compares this object with the specified object for order. Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     * 
     * @param other The object to compare to this instance.
     * @return A negative integer, zero, or a positive integer as this object
     *         is less than, equal to, or greater than the specified object.
     */
    @Override
    public int compareTo(@NonNull Configuration other) {
        int result = screenLayout - other.screenLayout;
        if (result != 0) return result;
        result = uiMode - other.uiMode;
        if (result != 0) return result;
        result = layoutDirection - other.layoutDirection;
        if (result != 0) return result;
        result = orientation - other.orientation;
        if (result != 0) return result;
        if (locale == null) {
            if (other.locale != null) return -1;
        } else if (other.locale == null) {
            return 1;
        } else {
            result = locale.compareTo(other.locale);
            if (result != 0) return result;
        }
        return 0;
    }
    
    /**
     * Writes the Configuration to a Parcel.
     * 
     * @param dest The Parcel to write to.
     * @param flags Additional flags about how the object should be written.
     */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        // Mock implementation, does nothing
    }
    
    /**
     * Describe the kinds of special objects contained in this object.
     * 
     * @return A bitmask indicating the set of special object types in this Parcelable.
     */
    @Override
    public int describeContents() {
        return 0;
    }
    
    /**
     * Indicates whether the current Configuration is different from the specified one.
     * 
     * @param other The Configuration to compare with this instance.
     * @return True if the specified configuration differs from the current one.
     */
    public boolean diff(@NonNull Configuration other) {
        return !equals(other);
    }
    
    /**
     * Parcelable creator for Configuration.
     */
    public static final Parcelable.Creator<Configuration> CREATOR =
            new Parcelable.Creator<Configuration>() {
        @Override
        @NonNull
        public Configuration createFromParcel(Parcel source) {
            return new Configuration();
        }
        
        @Override
        @NonNull
        public Configuration[] newArray(int size) {
            return new Configuration[size];
        }
    };
}