package com.osrs.server.world;

import com.osrs.shared.Entity;
import com.osrs.shared.NPC;
import com.osrs.shared.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * World state: all entities, tile map, and player sessions.
 * Loads NPCs, maps, and loot tables from world.yml
 */
public class World {
    
    private static final Logger LOG = LoggerFactory.getLogger(World.class);
    
    private final TileMap tileMap;
    private final Pathfinding pathfinding;
    private final Map<Integer, Player> players = new HashMap<>();
    private final Map<Integer, NPC> npcs = new HashMap<>();
    private final Map<String, WorldData.LootTable> lootTables;
    private final WorldData worldData;
    private int nextEntityId = 1;
    
    public World() throws Exception {
        LOG.info("Initializing World...");
        
        // Load world configuration from world.yml
        this.worldData = WorldLoader.loadWorld();
        LOG.info("World configuration loaded");
        LOG.info("  Spawn: ({}, {})", worldData.spawnX, worldData.spawnY);
        LOG.info("  Maps: {}", worldData.maps.keySet());
        
        // Initialize tile map
        this.tileMap = new TileMap();
        tileMap.initializeDefaultMap(104, 104);  // OSRS default size
        this.pathfinding = new Pathfinding(tileMap);
        LOG.info("Tile map initialized: {} x {} tiles", tileMap.getWidth(), tileMap.getHeight());
        
        // Store loot tables
        this.lootTables = worldData.lootTables;
        LOG.info("Loot tables loaded: {}", lootTables.keySet());
        
        // Spawn NPCs from world configuration (uses wander_radius from world.yml)
        spawnConfiguredNPCs();
        
        LOG.info("✓ World initialized successfully with {} NPCs", npcs.size());
    }
    
    /**
     * Spawn NPCs from world.yml configuration
     */
    private void spawnConfiguredNPCs() {
        for (WorldData.NPCDefinition npcDef : worldData.npcs) {
            NPC npc = new NPC(npcDef.id, npcDef.name, npcDef.combatLevel, npcDef.x, npcDef.y);
            npc.setLootTable(npcDef.lootTable);
            npc.setAggressive(npcDef.isAggressive);
            npc.setWanderRadius(npcDef.wanderRadius);
            npcs.put(npcDef.id, npc);
            LOG.debug("Spawned NPC: {} (id={}, level={}, pos=({}, {}))", 
                npcDef.name, npcDef.id, npcDef.combatLevel, npcDef.x, npcDef.y);
        }
    }
    
    /**
     * Spawn a player into the world
     */
    public void spawnPlayer(int playerId, String playerName) {
        Player player = new Player(playerId, playerName, worldData.spawnX, worldData.spawnY);
        players.put(playerId, player);
        LOG.info("Player {} spawned at ({}, {})", playerName, worldData.spawnX, worldData.spawnY);
    }
    
    /**
     * Spawn an NPC (for dynamic spawning, not from config)
     */
    public void spawnNPC(int npcId, String npcName, int combatLevel, int x, int y) {
        NPC npc = new NPC(npcId, npcName, combatLevel, x, y);
        npcs.put(npcId, npc);
        LOG.debug("NPC {} spawned at ({}, {})", npcName, x, y);
    }
    
    /**
     * Get player by ID
     */
    public Player getPlayer(int playerId) {
        return players.get(playerId);
    }
    
    /**
     * Get NPC by ID
     */
    public NPC getNPC(int npcId) {
        return npcs.get(npcId);
    }
    
    /**
     * Get NPC at specific coordinates
     */
    public NPC getNPCAt(int x, int y) {
        for (NPC npc : npcs.values()) {
            if (npc.getX() == x && npc.getY() == y) {
                return npc;
            }
        }
        return null;
    }
    
    /**
     * Check if a tile is walkable
     */
    public boolean canWalkTo(int x, int y) {
        return tileMap.isWalkable(x, y);
    }
    
    /**
     * Find a path using BFS pathfinding
     */
    public List<Pathfinding.Tile> findPath(int startX, int startY, int targetX, int targetY) {
        return pathfinding.findPath(startX, startY, targetX, targetY);
    }
    
    /**
     * Check if a location is reachable
     */
    public boolean canReach(int startX, int startY, int targetX, int targetY) {
        return pathfinding.canReach(startX, startY, targetX, targetY);
    }
    
    /**
     * Get all players
     */
    public Map<Integer, Player> getPlayers() {
        return players;
    }
    
    /**
     * Get all NPCs
     */
    public Map<Integer, NPC> getNPCs() {
        return npcs;
    }
    
    /**
     * Get all players as list (for iteration)
     */
    public Collection<Player> getAllPlayers() {
        return players.values();
    }
    
    /**
     * Get all NPCs as list (for iteration)
     */
    public Collection<NPC> getAllNPCs() {
        return npcs.values();
    }
    
    /**
     * Get tile map
     */
    public TileMap getTileMap() {
        return tileMap;
    }

    public int getSpawnX() { return worldData.spawnX; }
    public int getSpawnY() { return worldData.spawnY; }
    
    /**
     * Get loot table by ID
     */
    public WorldData.LootTable getLootTable(String tableId) {
        return lootTables.get(tableId);
    }
}
