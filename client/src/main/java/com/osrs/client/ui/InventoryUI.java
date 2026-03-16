package com.osrs.client.ui;

/**
 * UI for inventory management (items, equipment).
 */
public class InventoryUI {
    
    private static final int INVENTORY_SLOTS = 20;
    private static final int EQUIPMENT_SLOTS = 8;
    private static final int SLOT_WIDTH = 32;
    private static final int SLOT_HEIGHT = 32;
    private static final int SLOT_SPACING = 4;
    
    // Inventory grid position (top-right of screen)
    private static final int INVENTORY_X = 400;
    private static final int INVENTORY_Y = 100;
    
    // Equipment panel position (below inventory)
    private static final int EQUIPMENT_X = 400;
    private static final int EQUIPMENT_Y = 300;
    
    private int[] inventoryItemIds = new int[INVENTORY_SLOTS];
    private int[] inventoryQuantities = new int[INVENTORY_SLOTS];
    
    private int[] equipmentItemIds = new int[EQUIPMENT_SLOTS];
    
    private boolean visible = true;
    
    /**
     * Update inventory item.
     */
    public void setInventoryItem(int slot, int itemId, int quantity) {
        if (slot >= 0 && slot < INVENTORY_SLOTS) {
            inventoryItemIds[slot] = itemId;
            inventoryQuantities[slot] = quantity;
        }
    }
    
    /**
     * Update equipment item.
     */
    public void setEquipment(int slot, int itemId) {
        if (slot >= 0 && slot < EQUIPMENT_SLOTS) {
            equipmentItemIds[slot] = itemId;
        }
    }
    
    /**
     * Render inventory (placeholder).
     */
    public void render() {
        // TODO: Render inventory grid and equipment panel
    }
    
    /**
     * Handle inventory click.
     */
    public int getClickedSlot(int mouseX, int mouseY) {
        // Check if click is within inventory bounds
        if (mouseX < INVENTORY_X || mouseX > INVENTORY_X + (SLOT_WIDTH * 4) + (SLOT_SPACING * 4)) {
            return -1;
        }
        
        if (mouseY < INVENTORY_Y || mouseY > INVENTORY_Y + (SLOT_HEIGHT * 5) + (SLOT_SPACING * 5)) {
            return -1;
        }
        
        // Calculate which slot was clicked
        int relX = mouseX - INVENTORY_X;
        int relY = mouseY - INVENTORY_Y;
        
        int col = relX / (SLOT_WIDTH + SLOT_SPACING);
        int row = relY / (SLOT_HEIGHT + SLOT_SPACING);
        
        if (col >= 4 || row >= 5) {
            return -1;
        }
        
        return row * 4 + col;
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    public int getInventoryItemId(int slot) {
        return (slot >= 0 && slot < INVENTORY_SLOTS) ? inventoryItemIds[slot] : 0;
    }
    
    public int getInventoryQuantity(int slot) {
        return (slot >= 0 && slot < INVENTORY_SLOTS) ? inventoryQuantities[slot] : 0;
    }
    
    public int getEquipment(int slot) {
        return (slot >= 0 && slot < EQUIPMENT_SLOTS) ? equipmentItemIds[slot] : 0;
    }
}
