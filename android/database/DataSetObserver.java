package android.database;

/**
 * Mock implementation of Android DataSetObserver class for development outside of Android.
 * Receives callbacks when a data set has been changed, or made invalid.
 * The callback methods are typically called in the thread that
 * makes the changes, and implementations of these methods should be as
 * lightweight as possible to avoid blocking the thread.
 */
public abstract class DataSetObserver {
    /**
     * This method is called when the entire data set has changed, such as
     * when a new set of data is added to it.
     */
    public void onChanged() {
        // Do nothing by default
    }
    
    /**
     * This method is called when the entire data set has been invalidated,
     * such as when a new set of data will be loaded.
     */
    public void onInvalidated() {
        // Do nothing by default
    }
}