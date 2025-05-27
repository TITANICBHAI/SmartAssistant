package android.content;

/**
 * Mock implementation of Android's ClipData
 */
public class ClipData {
    private final String label;
    private final ClipDescription description;
    private final Item item;
    
    /**
     * Private constructor
     * @param label The label
     * @param description The description
     * @param item The item
     */
    private ClipData(String label, ClipDescription description, Item item) {
        this.label = label;
        this.description = description;
        this.item = item;
    }
    
    /**
     * Create a new ClipData with a plain text item
     * @param label The label
     * @param text The text
     * @return The new ClipData
     */
    public static ClipData newPlainText(String label, CharSequence text) {
        ClipDescription description = new ClipDescription(label, new String[] {"text/plain"});
        Item item = new Item(text);
        return new ClipData(label, description, item);
    }
    
    /**
     * Get the label
     * @return The label
     */
    public String getLabel() {
        return label;
    }
    
    /**
     * Get the description
     * @return The description
     */
    public ClipDescription getDescription() {
        return description;
    }
    
    /**
     * Get the item at the specified index
     * @param index The index
     * @return The item
     */
    public Item getItemAt(int index) {
        if (index != 0) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length 1");
        }
        return item;
    }
    
    /**
     * Get the number of items
     * @return The number of items
     */
    public int getItemCount() {
        return 1;
    }
    
    /**
     * Item in a ClipData
     */
    public static class Item {
        private final CharSequence text;
        
        /**
         * Constructor
         * @param text The text
         */
        public Item(CharSequence text) {
            this.text = text;
        }
        
        /**
         * Get the text
         * @return The text
         */
        public CharSequence getText() {
            return text;
        }
    }
    
    /**
     * Description of a ClipData
     */
    public static class ClipDescription {
        private final String label;
        private final String[] mimeTypes;
        
        /**
         * Constructor
         * @param label The label
         * @param mimeTypes The MIME types
         */
        public ClipDescription(String label, String[] mimeTypes) {
            this.label = label;
            this.mimeTypes = mimeTypes;
        }
        
        /**
         * Get the label
         * @return The label
         */
        public String getLabel() {
            return label;
        }
        
        /**
         * Check if a MIME type is supported
         * @param mimeType The MIME type
         * @return True if supported, false otherwise
         */
        public boolean hasMimeType(String mimeType) {
            for (String supportedType : mimeTypes) {
                if (supportedType.equals(mimeType)) {
                    return true;
                }
            }
            return false;
        }
    }
}