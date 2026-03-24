package com.osrs.shared;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a player character.
 * Extends Entity with player-specific properties like stats, inventory, quests.
 */
public class Player extends Entity {

    // Equipment (11 slots using EquipmentSlot constants)
    private int[] equipment = new int[EquipmentSlot.COUNT];

    // Inventory (28 slots — OSRS standard)
    private int[] inventoryItemIds = new int[28];
    private int[] inventoryQuantities = new int[28];
    
    // Combat state
    private int combatTarget = -1; // Entity ID of current combat target
    private long lastAttackTick = 0;

    // Dialogue state
    private int dialogueTarget = -1; // NPC ID this player is currently talking to, or -1
    
    // Skills (stored server-side in Stats, cached here for quick access)
    private int attackLevel = 1;
    private int strengthLevel = 1;
    private int defenceLevel = 1;
    private int rangedLevel = 1;
    private int magicLevel = 1;
    
    // Experience (stored server-side in Stats)
    private long attackXp = 0;
    private long strengthXp = 0;
    private long defenceXp = 0;
    
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
        if (slot >= 0 && slot < 28) {
            inventoryItemIds[slot] = itemId;
            inventoryQuantities[slot] = quantity;
        }
    }

    public int getInventoryItemId(int slot) {
        return (slot >= 0 && slot < 28) ? inventoryItemIds[slot] : 0;
    }

    public int getInventoryQuantity(int slot) {
        return (slot >= 0 && slot < 28) ? inventoryQuantities[slot] : 0;
    }

    /**
     * Returns the index of the first empty inventory slot, or -1 if full.
     */
    public int getFirstEmptySlot() {
        for (int i = 0; i < 28; i++) {
            if (inventoryItemIds[i] == 0) return i;
        }
        return -1;
    }

    /**
     * Returns true if every inventory slot is occupied.
     */
    public boolean isInventoryFull() {
        return getFirstEmptySlot() < 0;
    }
    
    // Skill getters/setters
    public int getAttackLevel() { return attackLevel; }
    public void setAttackLevel(int level) { this.attackLevel = level; }
    
    public int getStrengthLevel() { return strengthLevel; }
    public void setStrengthLevel(int level) { this.strengthLevel = level; }
    
    public int getDefenceLevel() { return defenceLevel; }
    public void setDefenceLevel(int level) { this.defenceLevel = level; }
    
    public int getRangedLevel() { return rangedLevel; }
    public void setRangedLevel(int level) { this.rangedLevel = level; }
    
    public int getMagicLevel() { return magicLevel; }
    public void setMagicLevel(int level) { this.magicLevel = level; }
    
    public int getDialogueTarget() { return dialogueTarget; }
    public void setDialogueTarget(int npcId) { this.dialogueTarget = npcId; }
    public boolean isInDialogue() { return dialogueTarget >= 0; }

    // Experience getters/setters
    public long getAttackXp() { return attackXp; }
    public void setAttackXp(long xp) { this.attackXp = xp; }
    public void addAttackXp(long xp) { this.attackXp += xp; }
    
    public long getStrengthXp() { return strengthXp; }
    public void setStrengthXp(long xp) { this.strengthXp = xp; }
    public void addStrengthXp(long xp) { this.strengthXp += xp; }
    
    public long getDefenceXp() { return defenceXp; }
    public void setDefenceXp(long xp) { this.defenceXp = xp; }
    public void addDefenceXp(long xp) { this.defenceXp += xp; }
}
