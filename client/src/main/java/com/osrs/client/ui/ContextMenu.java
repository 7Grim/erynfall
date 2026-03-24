package com.osrs.client.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Right-click context menu — OSRS style.
 *
 * Coordinate system: raw LibGDX screen coords (Y=0 at bottom).
 * The menu opens at the cursor position and grows UPWARD.
 * Item 0 is rendered just below the "Choose Option" header (visually at top).
 */
public class ContextMenu {

    public static final int ITEM_HEIGHT = 16;
    public static final int HEADER_HEIGHT = 20;
    public static final int MENU_WIDTH = 150;

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
    private int screenX, screenY; // bottom-left of the menu (LibGDX Y-up coords)
    private boolean visible = false;

    /**
     * Open menu at the given position (LibGDX screen coords, Y from bottom).
     * The menu grows upward from (x, y).
     */
    public void open(int x, int y, List<MenuItem> menuItems) {
        this.screenX = x;
        this.screenY = y;
        this.items = new ArrayList<>(menuItems);
        this.visible = true;
    }

    public void close() {
        this.visible = false;
        this.items.clear();
    }

    /**
     * Returns the clicked item, or null.
     *
     * Layout (Y-up, menu grows upward from screenY):
     *   [screenY + N*ITEM_HEIGHT .. screenY + totalHeight]  → header (no action)
     *   item 0 (top): [screenY + (N-1)*ITEM_HEIGHT .. screenY + N*ITEM_HEIGHT]
     *   item 1:       [screenY + (N-2)*ITEM_HEIGHT .. screenY + (N-1)*ITEM_HEIGHT]
     *   ...
     *   item N-1 (bottom): [screenY .. screenY + ITEM_HEIGHT]
     */
    public MenuItem getClickedItem(int mouseX, int mouseY) {
        if (!visible || items.isEmpty()) return null;

        int n = items.size();
        int totalHeight = HEADER_HEIGHT + n * ITEM_HEIGHT;

        if (mouseX < screenX || mouseX > screenX + MENU_WIDTH
                || mouseY < screenY || mouseY > screenY + totalHeight) {
            return null;
        }

        // Clicked in header area?
        if (mouseY >= screenY + n * ITEM_HEIGHT) {
            return null; // "Choose Option" label — dismiss, no action
        }

        // Item area: item 0 is at top (highest Y), item N-1 is at bottom (lowest Y)
        int relY = mouseY - screenY;                   // 0 = bottom of menu
        int indexFromBottom = relY / ITEM_HEIGHT;       // 0 = bottom item
        int itemIndex = n - 1 - indexFromBottom;        // 0 = top item
        if (itemIndex >= 0 && itemIndex < n) {
            return items.get(itemIndex);
        }
        return null;
    }

    public boolean isVisible() { return visible; }
    public List<MenuItem> getItems() { return items; }
    public int getScreenX() { return screenX; }
    public int getScreenY() { return screenY; }
}
