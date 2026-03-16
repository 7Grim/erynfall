package com.osrs.shared;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a player character.
 * Extends Entity with player-specific properties like stats, inventory, quests.
 */
public class Player extends Entity {
    
    // Stats (experience values)
    private final Map<String, Long> experience = new HashMap<>();
    private final Map<String, Integer> levels = new HashMap<>();
    
    // Equipment (8 slots)
    private int[] equipment = new int[8];
    
    // Inventory (20 slots)
    private int[] inventoryItemIds = new int[20];
    private int[] inventoryQuantities = new int[20];
    
    public Player(int id, String name, int x, int y) {
        super(id, name, x, y);
        initializeStats();
    }
    
    private void initializeStats() {
        String[] skills = {"attack", "strength", "defence", "hitpoints"};
        for (String skill : skills) {
            experience.put(skill, 0L);
            levels.put(skill, 1);
        }
    }
    
    public long getExperience(String skill) {
        return experience.getOrDefault(skill, 0L);
    }
    
    public void addExperience(String skill, long amount) {
        experience.put(skill, experience.getOrDefault(skill, 0L) + amount);
        updateLevel(skill);
    }
    
    public int getLevel(String skill) {
        return levels.getOrDefault(skill, 1);
    }
    
    private void updateLevel(String skill) {
        long xp = experience.get(skill);
        int level = calculateLevel(xp);
        levels.put(skill, level);
    }
    
    private int calculateLevel(long xp) {
        // Simplified: 1 XP per level, max level 99
        return Math.min(99, 1 + (int) (xp / 1000L));
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
