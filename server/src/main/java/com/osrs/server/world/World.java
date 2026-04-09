package com.osrs.server.world;

import com.osrs.shared.Entity;
import com.osrs.shared.ItemDefinition;
import com.osrs.shared.NPC;
import com.osrs.shared.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;

import java.util.*;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

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
    private final Map<Integer, String> npcExamineTexts = new HashMap<>();
    private final Map<String, WorldData.LootTable> lootTables;
    private final WorldData worldData;
    private int nextEntityId = 1;

    // Ground items
    private final Map<Integer, GroundItem> groundItems = new HashMap<>();
    private int nextGroundItemId = 1;

    // -----------------------------------------------------------------------
    // Pending item pickups (cross-tick scheduling)
    // -----------------------------------------------------------------------

    /**
     * A pickup that has been validated and is waiting for the 3-OSRS-tick
     * animation delay before the item enters the player's inventory.
     */
    public static class PendingPickup {
        public final Player            player;
        public final int               groundItemId;
        public final long              executeTick;
        public final ChannelHandlerContext ctx;

        public PendingPickup(Player player, int groundItemId,
                             long executeTick, ChannelHandlerContext ctx) {
            this.player       = player;
            this.groundItemId = groundItemId;
            this.executeTick  = executeTick;
            this.ctx          = ctx;
        }
    }

    private final ConcurrentLinkedQueue<PendingPickup> pendingPickups =
        new ConcurrentLinkedQueue<>();

    /** Schedule a validated pickup to execute after the animation delay. */
    public void schedulePendingPickup(Player player, int groundItemId,
                                      long executeTick, ChannelHandlerContext ctx) {
        pendingPickups.add(new PendingPickup(player, groundItemId, executeTick, ctx));
    }

    /** Drain all pickups whose execute tick has arrived. */
    public List<PendingPickup> drainDuePickups(long currentTick) {
        List<PendingPickup> due = new ArrayList<>();
        // Peek-and-remove those that are due; leave future ones in the queue
        pendingPickups.removeIf(p -> {
            if (p.executeTick <= currentTick) { due.add(p); return true; }
            return false;
        });
        return due;
    }

    // Item definitions (loaded from items.yaml)
    private final Map<Integer, ItemDefinition> itemDefs;
    private static final ItemDefinition DEFAULT_ITEM_DEF = new ItemDefinition();

    public World() throws Exception {
        LOG.info("Initializing World...");

        // Load item definitions from items.yaml
        Map<Integer, ItemDefinition> loadedItems;
        try {
            loadedItems = ItemLoader.loadItems();
        } catch (Exception e) {
            LOG.warn("Failed to load items.yaml, using empty item definitions: {}", e.getMessage());
            loadedItems = new HashMap<>();
        }
        this.itemDefs = loadedItems;

        // Load world configuration from world.yml
        this.worldData = WorldLoader.loadWorld();
        LOG.info("World configuration loaded");
        LOG.info("  Spawn: ({}, {})", worldData.spawnX, worldData.spawnY);
        LOG.info("  Maps: {}", worldData.maps.keySet());
        
        // Initialize tile map
        this.tileMap = new TileMap();
        try {
            tileMap.load("map.yaml");
        } catch (Exception e) {
            LOG.warn("Failed to load map.yaml; using default walkable map: {}", e.getMessage());
            tileMap.initializeDefaultMap(104, 104);  // OSRS default size
        }
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
            NPC npc = new NPC(npcDef.id, npcDef.name, npcDef.definitionId, npcDef.x, npcDef.y);
            npc.setMaxHealth(npcDef.maxHp);
            npc.setMaxHit(npcDef.maxHit);
            npc.setRespawnDelayTicks(npcDef.respawnDelayTicks);
            npc.setLootTable(npcDef.lootTable);
            npc.setAggressive(npcDef.isAggressive);
            npc.setWanderRadius(npcDef.wanderRadius);
            npc.setAttackRange(npcDef.attackRange);
            // Set combat stats; -1 means "derive from combatLevel"
            npc.setCombatLevel(npcDef.combatLevel);
            if (npcDef.attackLevel   >= 0) npc.setAttackLevel(npcDef.attackLevel);
            if (npcDef.strengthLevel >= 0) npc.setStrengthLevel(npcDef.strengthLevel);
            if (npcDef.defenceLevel  >= 0) npc.setDefenceLevel(npcDef.defenceLevel);
            npc.setAttackBonus(npcDef.attackBonus);
            npc.setStrengthBonus(npcDef.strengthBonus);
            npc.setDefenceBonus(npcDef.defenceBonus);
            npc.setBanker(npcDef.isBanker);
            npc.setFishingSupplier(npcDef.isFishingSupplier);
            npc.setSmithingSupplier(npcDef.isSmithingSupplier);
            npcs.put(npcDef.id, npc);
            npcExamineTexts.put(npcDef.id, npcDef.examine);
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
        NPC npc = new NPC(npcId, npcName, npcId, x, y);
        npc.setCombatLevel(combatLevel);
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

    public String getNpcExamineText(int npcId) {
        String text = npcExamineTexts.get(npcId);
        if (text == null || text.isBlank()) {
            NPC npc = npcs.get(npcId);
            String name = npc != null ? npc.getName() : "creature";
            return "It's a " + name + ".";
        }
        return text;
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

    // -----------------------------------------------------------------------
    // Item definitions
    // -----------------------------------------------------------------------

    public Map<Integer, ItemDefinition> getItemDefs() { return itemDefs; }

    public ItemDefinition getItemDef(int itemId) {
        return itemDefs.getOrDefault(itemId, DEFAULT_ITEM_DEF);
    }

    // -----------------------------------------------------------------------
    // Ground items
    // -----------------------------------------------------------------------

    public GroundItem spawnGroundItem(int itemId, int quantity, int x, int y,
                                      int ownerPlayerId, long tick) {
        int id = nextGroundItemId++;
        GroundItem gi = new GroundItem(id, itemId, quantity, x, y, ownerPlayerId, tick);
        groundItems.put(id, gi);
        LOG.debug("Spawned ground item: id={} itemId={} qty={} at ({},{}) owner={}",
            id, itemId, quantity, x, y, ownerPlayerId);
        return gi;
    }

    public Map<Integer, GroundItem> getGroundItems() {
        return groundItems;
    }

    public GroundItem getGroundItem(int id) {
        return groundItems.get(id);
    }

    public void removeGroundItem(int id) {
        groundItems.remove(id);
    }

    // -----------------------------------------------------------------------
    // Loot generation (kept here to access package-private WorldData)
    // -----------------------------------------------------------------------

    /**
     * Roll loot drops from the NPC's loot table and spawn them as ground items.
     * Returns the list of spawned GroundItem objects so the caller can notify clients.
     *
     * @param npc    the NPC that just died
     * @param owner  the player who gets owner-only visibility
     * @param tick   current server tick
     * @param rng    shared Random instance from GameLoop
     */
    public List<GroundItem> rollAndSpawnLoot(NPC npc, Player owner, long tick, Random rng) {
        List<GroundItem> spawned = new ArrayList<>();
        WorldData.LootTable table = lootTables.get(npc.getLootTable());
        if (table == null) return spawned;

        List<WorldData.LootDrop> drops = new ArrayList<>(table.alwaysDrops);
        for (WorldData.LootDrop d : table.commonDrops) {
            if (rng.nextInt(100) < d.chance) drops.add(d);
        }
        for (WorldData.LootDrop d : table.rareDrops) {
            if (rng.nextInt(10000) < d.chance) drops.add(d);
        }
        for (WorldData.LootDrop d : drops) {
            int qty = d.minQuantity + (d.maxQuantity > d.minQuantity
                ? rng.nextInt(d.maxQuantity - d.minQuantity + 1) : 0);
            GroundItem gi = spawnGroundItem(d.itemId, qty,
                npc.getX(), npc.getY(), owner.getId(), tick);
            spawned.add(gi);
        }
        return spawned;
    }
}
