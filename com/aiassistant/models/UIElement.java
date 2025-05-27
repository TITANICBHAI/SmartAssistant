package com.aiassistant.models;

import android.graphics.Rect;
import utils.UIElementInterface;

/**
 * Class representing a UI element.
 */
public class UIElement implements UIElementInterface {
    private String id;
    private String text;
    private ElementType elementType;
    private Rect bounds;
    private boolean clickable;
    private boolean visible;
    private boolean enabled;
    private String resourceId;
    private String contentDescription;
    private String packageName;
    
    /**
     * Create a new UIElement
     */
    public UIElement() {
        // Default constructor
        this.enabled = true;
    }
    
    /**
     * Create a new UIElement
     * @param id The ID
     * @param text The text
     * @param elementType The element type
     * @param bounds The bounds
     */
    public UIElement(String id, String text, ElementType elementType, Rect bounds) {
        this.id = id;
        this.text = text;
        this.elementType = elementType;
        this.bounds = bounds;
        this.clickable = true;
        this.visible = true;
        this.enabled = true;
    }
    
    /**
     * Get the ID as an integer for UIElementInterface compatibility
     * @return The ID as an integer
     */
    @Override
    public int getId() {
        if (id == null) {
            return 0;
        }
        try {
            return Integer.parseInt(id);
        } catch (NumberFormatException e) {
            // If the ID is not a number, use its hashcode
            return id.hashCode();
        }
    }
    
    /**
     * Get the original string ID
     * @return The string ID
     */
    public String getStringId() {
        return id;
    }
    
    /**
     * Set the ID
     * @param id The ID
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Get the text
     * @return The text
     */
    @Override
    public String getText() {
        return text;
    }
    
    /**
     * Set the text
     * @param text The text
     */
    public void setText(String text) {
        this.text = text;
    }
    
    /**
     * Get the element type
     * @return The element type
     */
    public ElementType getElementType() {
        return elementType;
    }
    
    /**
     * Get the element type as an integer for UIElementInterface compatibility
     * @return The element type as an integer
     */
    @Override
    public int getElementType() {
        if (elementType == null) {
            return 0;
        }
        return elementType.ordinal();
    }
    
    /**
     * Set the element type
     * @param elementType The element type
     */
    public void setElementType(ElementType elementType) {
        this.elementType = elementType;
    }
    
    /**
     * Get the bounds
     * @return The bounds
     */
    @Override
    public Object getBounds() {
        return bounds;
    }
    
    /**
     * Get the specific Rect bounds
     * @return The Rect bounds
     */
    public Rect getRectBounds() {
        return bounds;
    }
    
    /**
     * Set the bounds
     * @param bounds The bounds
     */
    public void setBounds(Rect bounds) {
        this.bounds = bounds;
    }
    
    /**
     * Is the element clickable
     * @return True if clickable, false otherwise
     */
    @Override
    public boolean isClickable() {
        return clickable;
    }
    
    /**
     * Set clickable
     * @param clickable True if clickable, false otherwise
     */
    public void setClickable(boolean clickable) {
        this.clickable = clickable;
    }
    
    /**
     * Is the element visible
     * @return True if visible, false otherwise
     */
    @Override
    public boolean isVisible() {
        return visible;
    }
    
    /**
     * Set visible
     * @param visible True if visible, false otherwise
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    /**
     * Is the element enabled
     * @return True if enabled, false otherwise
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Set enabled
     * @param enabled True if enabled, false otherwise
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Get the resource ID
     * @return The resource ID
     */
    public String getResourceId() {
        return resourceId;
    }
    
    /**
     * Set the resource ID
     * @param resourceId The resource ID
     */
    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }
    
    /**
     * Get the content description
     * @return The content description
     */
    public String getContentDescription() {
        return contentDescription;
    }
    
    /**
     * Set the content description
     * @param contentDescription The content description
     */
    public void setContentDescription(String contentDescription) {
        this.contentDescription = contentDescription;
    }
    
    /**
     * Get the package name
     * @return The package name
     */
    @Override
    public String getPackageName() {
        return packageName;
    }
    
    /**
     * Set the package name
     * @param packageName The package name
     */
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
    
    /**
     * Get the element width
     * @return The element width
     */
    @Override
    public int getWidth() {
        if (bounds == null) {
            return 0;
        }
        return bounds.width();
    }
    
    /**
     * Get the element height
     * @return The element height
     */
    @Override
    public int getHeight() {
        if (bounds == null) {
            return 0;
        }
        return bounds.height();
    }
    
    /**
     * Get the element left position
     * @return The element left position
     */
    @Override
    public int getLeft() {
        if (bounds == null) {
            return 0;
        }
        return bounds.left;
    }
    
    /**
     * Get the element top position
     * @return The element top position
     */
    @Override
    public int getTop() {
        if (bounds == null) {
            return 0;
        }
        return bounds.top;
    }
    
    /**
     * Get the element right position
     * @return The element right position
     */
    @Override
    public int getRight() {
        if (bounds == null) {
            return 0;
        }
        return bounds.right;
    }
    
    /**
     * Get the element bottom position
     * @return The element bottom position
     */
    @Override
    public int getBottom() {
        if (bounds == null) {
            return 0;
        }
        return bounds.bottom;
    }
    
    /**
     * Get the center X coordinate
     * @return The center X coordinate
     */
    @Override
    public int getCenterX() {
        if (bounds == null) {
            return 0;
        }
        return (bounds.left + bounds.right) / 2;
    }
    
    /**
     * Get the center X coordinate as float
     * @return The center X coordinate
     */
    public float getCenterXFloat() {
        if (bounds == null) {
            return 0;
        }
        return bounds.exactCenterX();
    }
    
    /**
     * Get the center Y coordinate
     * @return The center Y coordinate
     */
    @Override
    public int getCenterY() {
        if (bounds == null) {
            return 0;
        }
        return (bounds.top + bounds.bottom) / 2;
    }
    
    /**
     * Get the center Y coordinate as float
     * @return The center Y coordinate
     */
    public float getCenterYFloat() {
        if (bounds == null) {
            return 0;
        }
        return bounds.exactCenterY();
    }
    
    /**
     * Check if the element contains a point
     * @param x The x coordinate
     * @param y The y coordinate
     * @return True if the element contains the point, false otherwise
     */
    public boolean containsPoint(float x, float y) {
        if (bounds == null) {
            return false;
        }
        return bounds.contains((int)x, (int)y);
    }
    
    /**
     * Check if the element contains coordinates
     * @param x The x coordinate
     * @param y The y coordinate
     * @return True if the element contains the coordinates
     */
    @Override
    public boolean contains(int x, int y) {
        if (bounds == null) {
            return false;
        }
        return bounds.contains(x, y);
    }
    
    /**
     * Check if this element intersects with another element
     * @param other The other element
     * @return True if the elements intersect
     */
    @Override
    public boolean intersects(UIElementInterface other) {
        if (bounds == null || other == null) {
            return false;
        }
        
        // Calculate intersection
        int left = Math.max(getLeft(), other.getLeft());
        int top = Math.max(getTop(), other.getTop());
        int right = Math.min(getRight(), other.getRight());
        int bottom = Math.min(getBottom(), other.getBottom());
        
        return (left < right && top < bottom);
    }
    
    /**
     * Get the element's standardized type
     * @return The standardized element type
     */
    @Override
    public int getStandardizedElementType() {
        if (elementType == null) {
            return 0;
        }
        // Map ElementType to standardized type
        switch (elementType) {
            case BUTTON:
                return 1; // Assuming 1 is BUTTON in standardized type
            case TEXT:
                return 2; // Assuming 2 is TEXT in standardized type
            case IMAGE:
                return 3; // Assuming 3 is IMAGE in standardized type
            case CHECKBOX:
                return 4; // Assuming 4 is CHECKBOX in standardized type
            default:
                return 0; // UNKNOWN
        }
    }
}