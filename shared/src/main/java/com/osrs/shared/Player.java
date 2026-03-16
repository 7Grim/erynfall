package com.osrs.shared;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a player character.
 * Extends Entity with player-specific properties like stats, inventory, quests.
 */
public class Player extends Entity {
    
    // Equipment (8 slots)
    private int[] equipment = new int[8];
    
    // Inventory (20 slots)
    private int[] inventoryItemIds = new int[20];
    private int[] inventoryQuantities = new int[20];
    
    // Combat state
    private int combatTarget = -1; // Entity ID of current combat target
    private long lastAttackTick = 0;
    
    public Player(int id, String name, int x, int y) {
        super(id, name, x, y);
        this.health = 10; // Initial hitpoints
        this.maxHealth = 10;
    }
    
    public int getCombatTarget() {
        return combatTarget;
    }
    
    public void setCombatTarget(int entityId) {
        this.combatTarget = entityId;
    }
    
    public boolean isInCombat() {
        return combatTarget >= 0;
    }
    
    public long getLastAttackTick() {
        return lastAttackTick;
    }
    
    public void setLastAttackTick(long tick) {
        this.lastAttackTick = tick;
    }
    
    public void setEquipment(int slot, int itemId) {
        if (slot >= 0 && slot < equipment.length) {
            equipment[slot] = itemId;
        }
    }
    
    public int getEquipment(int slot) {
        return (slot >= 0 && slot < equipment.length) ? equipment[slot] : 0;
    }
    
    public void setInventoryItem(int slot, int itemId, int quantity) {
        if (slot >= 0 && slot < 20) {
            inventoryItemIds[slot] = itemId;
            inventoryQuantities[slot] = quantity;
        }
    }
    
    public int getInventoryItemId(int slot) {
        return (slot >= 0 && slot < 20) ? inventoryItemIds[slot] : 0;
    }
    
    public int getInventoryQuantity(int slot) {
        return (slot >= 0 && slot < 20) ? inventoryQuantities[slot] : 0;
    }
}
