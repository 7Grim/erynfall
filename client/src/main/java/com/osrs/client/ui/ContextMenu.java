package com.osrs.client.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Right-click context menu for OSRS-style interactions.
 */
public class ContextMenu {
    
    public static class MenuItem {
        public String label;
        public String action;
        public Object target;
        
        public MenuItem(String label, String action, Object target) {
            this.label = label;
            this.action = action;
            this.target = target;
        }
    }
    
    private List<MenuItem> items = new ArrayList<>();
    private int screenX, screenY;
    private boolean visible = false;
    
    /**
     * Open menu at screen position with options.
     */
    public void open(int x, int y, List<MenuItem> menuItems) {
        this.screenX = x;
        this.screenY = y;
        this.items = new ArrayList<>(menuItems);
        this.visible = true;
    }
    
    /**
     * Close menu.
     */
    public void close() {
        this.visible = false;
        this.items.clear();
    }
    
    /**
     * Get clicked menu item (if any).
     * Returns null if no item clicked.
     */
    public MenuItem getClickedItem(int mouseX, int mouseY) {
        if (!visible) return null;
        
        // Menu item height (pixels)
        int itemHeight = 16;
        int itemWidth = 120;
        
        // Check if click is within menu bounds
        if (mouseX < screenX || mouseX > screenX + itemWidth ||
            mouseY < screenY || mouseY > screenY + items.size() * itemHeight) {
            return null;
        }
        
        // Calculate which item was clicked
        int itemIndex = (mouseY - screenY) / itemHeight;
        if (itemIndex >= 0 && itemIndex < items.size()) {
            return items.get(itemIndex);
        }
        
        return null;
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    public List<MenuItem> getItems() {
        return items;
    }
    
    public int getScreenX() {
        return screenX;
    }
    
    public int getScreenY() {
        return screenY;
    }
}
