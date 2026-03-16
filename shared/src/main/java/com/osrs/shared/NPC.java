package com.osrs.shared;

/**
 * Represents a non-player character (NPC).
 * Extends Entity with NPC-specific properties like definition, dialogue state, combat target.
 */
public class NPC extends Entity {
    
    private int definitionId;
    private int combatTarget;     // Entity ID of current combat target, or -1 if none
    private String currentDialogueId;
    private long lastPathfindTick;
    
    public NPC(int id, String name, int definitionId, int x, int y) {
        super(id, name, x, y);
        this.definitionId = definitionId;
        this.combatTarget = -1;
        this.currentDialogueId = null;
        this.lastPathfindTick = 0;
    }
    
    public int getDefinitionId() {
        return definitionId;
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
    
    public String getCurrentDialogueId() {
        return currentDialogueId;
    }
    
    public void setCurrentDialogueId(String dialogueId) {
        this.currentDialogueId = dialogueId;
    }
    
    public long getLastPathfindTick() {
        return lastPathfindTick;
    }
    
    public void setLastPathfindTick(long tick) {
        this.lastPathfindTick = tick;
    }
}
