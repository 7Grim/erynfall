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

    public static final int ITEM_HEIGHT = 18;
    public static final int HEADER_HEIGHT = 19;
    public static final int MIN_MENU_WIDTH = 148;
    public static final int MAX_MENU_WIDTH = 320;
    public static final int H_PAD = 6;
    public static final int V_PAD = 4;

    public enum Action {
        WALK_HERE("walk", "Walk here"),
        EXAMINE_NPC("examine_npc", "Examine"),
        EXAMINE_ITEM("inv_examine", "Examine"),
        EXAMINE_GROUND_ITEM("examine_ground_item", "Examine"),
        TRADE("trade_player", "Trade with"),
        FOLLOW("follow_player", "Follow"),
        CHALLENGE("challenge_player", "Challenge"),
        ADD_FRIEND("friend_add", "Add friend"),
        REMOVE_FRIEND("friend_remove", "Remove friend"),
        ATTACK("attack", "Attack"),
        BANK("bank", "Bank"),
        TALK_TO("talk", "Talk-to"),
        CHOP("chop", "Chop down"),
        FISH("fish", "Net"),
        COOK_AT("cook_at", "Cook-at"),
        TAKE("take", "Take"),
        EAT("inv_eat", "Eat"),
        WIELD("inv_wield", "Wield"),
        BURY("inv_bury", "Bury"),
        USE("inv_use", "Use"),
        DROP("inv_drop", "Drop");

        public final String id;
        public final String defaultLabel;

        Action(String id, String defaultLabel) {
            this.id = id;
            this.defaultLabel = defaultLabel;
        }
    }

    public static class MenuItem {
        public String label;
        public String action;
        public Object target;

        public MenuItem(String label, String action, Object target) {
            this.label = label;
            this.action = action;
            this.target = target;
        }

        public MenuItem(Action action, String subject, Object target) {
            this.label = subject == null || subject.isEmpty()
                ? action.defaultLabel
                : action.defaultLabel + " " + subject;
            this.action = action.id;
            this.target = target;
        }
    }

    private List<MenuItem> items = new ArrayList<>();
    private int screenX, screenY; // bottom-left of the menu (LibGDX Y-up coords)
    private int menuWidth = MIN_MENU_WIDTH;
    private boolean visible = false;

    /**
     * Open menu at the given position (LibGDX screen coords, Y from bottom).
     * The menu grows upward from (x, y).
     */
    public void open(int x, int y, List<MenuItem> menuItems, int screenW, int screenH) {
        this.items = new ArrayList<>(menuItems);
        this.menuWidth = computeMenuWidth(menuItems);

        int totalHeight = totalHeight();
        this.screenX = clamp(x, 0, Math.max(0, screenW - menuWidth));
        this.screenY = clamp(y, 0, Math.max(0, screenH - totalHeight));
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
        int totalHeight = totalHeight();
        int itemsBottomY = screenY + V_PAD;
        int itemsTopY = itemsBottomY + n * ITEM_HEIGHT;

        if (mouseX < screenX || mouseX > screenX + menuWidth
                || mouseY < screenY || mouseY > screenY + totalHeight) {
            return null;
        }

        // Clicked in header area?
        if (mouseY >= itemsTopY) {
            return null; // "Choose Option" label — dismiss, no action
        }

        // Item area: item 0 is at top (highest Y), item N-1 is at bottom (lowest Y)
        int relY = mouseY - itemsBottomY;               // 0 = bottom of menu items area
        int indexFromBottom = relY / ITEM_HEIGHT;       // 0 = bottom item
        int itemIndex = n - 1 - indexFromBottom;        // 0 = top item
        if (itemIndex >= 0 && itemIndex < n) {
            return items.get(itemIndex);
        }
        return null;
    }

    public int getHoveredItemIndex(int mouseX, int mouseY) {
        if (!visible || items.isEmpty()) return -1;

        int n = items.size();
        int totalHeight = totalHeight();
        int itemsBottomY = screenY + V_PAD;
        int itemsTopY = itemsBottomY + n * ITEM_HEIGHT;

        if (mouseX < screenX || mouseX > screenX + menuWidth
            || mouseY < itemsBottomY || mouseY > itemsTopY
            || mouseY >= itemsTopY) {
            return -1;
        }

        int relY = mouseY - itemsBottomY;
        int indexFromBottom = relY / ITEM_HEIGHT;
        int itemIndex = n - 1 - indexFromBottom;
        return (itemIndex >= 0 && itemIndex < n) ? itemIndex : -1;
    }

    public boolean isVisible() { return visible; }
    public List<MenuItem> getItems() { return items; }
    public int getScreenX() { return screenX; }
    public int getScreenY() { return screenY; }
    public int getMenuWidth() { return menuWidth; }
    public int getTotalHeight() { return totalHeight(); }

    private int totalHeight() {
        return HEADER_HEIGHT + items.size() * ITEM_HEIGHT + V_PAD * 2;
    }

    private int computeMenuWidth(List<MenuItem> menuItems) {
        int maxChars = "Choose Option".length();
        for (MenuItem item : menuItems) {
            maxChars = Math.max(maxChars, plainLength(item.label));
        }
        int estimated = H_PAD * 2 + maxChars * 7 + 10;
        return clamp(estimated, MIN_MENU_WIDTH, MAX_MENU_WIDTH);
    }

    private int plainLength(String text) {
        if (text == null) return 0;
        String noColor = text.replaceAll("\\[#([0-9a-fA-F]{6})]", "").replace("[]", "");
        return noColor.length();
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
