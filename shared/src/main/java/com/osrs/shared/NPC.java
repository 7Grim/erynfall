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
    private String lootTable;
    private boolean aggressive;

    /** Spawn-point: NPC returns/wanders within wanderRadius of this tile. */
    private int spawnX, spawnY;
    private int wanderRadius;

    /** Maximum damage this NPC can deal per hit (from world.yml). */
    private int maxHit = 1;

    /** true while this NPC is dead and waiting to respawn. */
    private boolean dead = false;
    /** Server tick at which this NPC will respawn (-1 = not scheduled). */
    private long respawnAtTick = -1;
    /** How many server ticks to wait before respawning (set from world.yml). */
    private int respawnDelayTicks = 3850; // default ~25 OSRS ticks

    /** true while the NPC is in an active dialogue — wander and combat movement are frozen. */
    private boolean inDialogue = false;
    /** Player ID currently in dialogue with this NPC, or -1 if none. */
    private int dialoguePlayer = -1;
    /** Server tick of this NPC's last attack (for attack speed gating). */
    private long lastAttackTick = 0;

    public NPC(int id, String name, int definitionId, int x, int y) {
        super(id, name, x, y);
        this.definitionId = definitionId;
        this.combatTarget = -1;
        this.currentDialogueId = null;
        this.lastPathfindTick = 0;
        this.spawnX = x;
        this.spawnY = y;
        this.wanderRadius = 0;
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

    public String getLootTable() {
        return lootTable;
    }

    public void setLootTable(String lootTable) {
        this.lootTable = lootTable;
    }

    public boolean isAggressive() {
        return aggressive;
    }

    public void setAggressive(boolean aggressive) {
        this.aggressive = aggressive;
    }

    public int getSpawnX() { return spawnX; }
    public int getSpawnY() { return spawnY; }
    public int getWanderRadius() { return wanderRadius; }
    public void setWanderRadius(int radius) { this.wanderRadius = radius; }

    public boolean isInDialogue() { return inDialogue; }
    public void setInDialogue(boolean v) { this.inDialogue = v; }
    public int getDialoguePlayer() { return dialoguePlayer; }
    public void setDialoguePlayer(int id) { this.dialoguePlayer = id; }
    public long getLastAttackTick() { return lastAttackTick; }
    public void setLastAttackTick(long tick) { this.lastAttackTick = tick; }

    public int getMaxHit() { return maxHit; }
    public void setMaxHit(int maxHit) { this.maxHit = Math.max(0, maxHit); }

    public boolean isDead() { return dead; }
    public void setDead(boolean dead) { this.dead = dead; }
    public long getRespawnAtTick() { return respawnAtTick; }
    public void setRespawnAtTick(long tick) { this.respawnAtTick = tick; }
    public int getRespawnDelayTicks() { return respawnDelayTicks; }
    public void setRespawnDelayTicks(int ticks) { this.respawnDelayTicks = ticks; }
}
