package com.osrs.server.world;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.osrs.shared.NPC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.*;

/**
 * Loads world definition from world.yml
 * Initializes NPCs, maps, loot tables, and spawn points
 */
public class WorldLoader {
    
    private static final Logger LOG = LoggerFactory.getLogger(WorldLoader.class);
    
    /**
     * Load world from world.yml resource
     */
    public static WorldData loadWorld() throws Exception {
        LOG.info("Loading world configuration from world.yml");
        
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            InputStream is = WorldLoader.class.getClassLoader().getResourceAsStream("world.yml");
            
            if (is == null) {
                throw new Exception("world.yml not found in resources");
            }
            
            Map<String, Object> yaml = mapper.readValue(is, Map.class);
            is.close();
            
            WorldData worldData = new WorldData();
            
            // Load world info
            if (yaml.containsKey("world")) {
                Map<String, Object> world = (Map<String, Object>) yaml.get("world");
                worldData.spawnX = getInt(world, "spawn_x", 3222);
                worldData.spawnY = getInt(world, "spawn_y", 3218);
            }
            
            // Load maps
            if (yaml.containsKey("maps")) {
                Map<String, Object> maps = (Map<String, Object>) yaml.get("maps");
                for (String mapName : maps.keySet()) {
                    Map<String, Object> mapDef = (Map<String, Object>) maps.get(mapName);
                    WorldData.MapInfo mapInfo = new WorldData.MapInfo();
                    mapInfo.name = mapName;
                    mapInfo.displayName = getString(mapDef, "name", mapName);
                    mapInfo.minX = getInt(mapDef, "min_x", 0);
                    mapInfo.minY = getInt(mapDef, "min_y", 0);
                    mapInfo.maxX = getInt(mapDef, "max_x", 128);
                    mapInfo.maxY = getInt(mapDef, "max_y", 128);
                    worldData.maps.put(mapName, mapInfo);
                }
            }
            
            // Load loot tables
            if (yaml.containsKey("loot_tables")) {
                Map<String, Object> lootTables = (Map<String, Object>) yaml.get("loot_tables");
                for (String tableId : lootTables.keySet()) {
                    Map<String, Object> tableDef = (Map<String, Object>) lootTables.get(tableId);
                    WorldData.LootTable table = new WorldData.LootTable();
                    table.id = tableId;
                    
                    // Always drops
                    if (tableDef.containsKey("always")) {
                        List<Map<String, Object>> always = (List<Map<String, Object>>) tableDef.get("always");
                        for (Map<String, Object> drop : always) {
                            table.alwaysDrops.add(parseLootDrop(drop));
                        }
                    }
                    
                    // Common drops
                    if (tableDef.containsKey("common")) {
                        List<Map<String, Object>> common = (List<Map<String, Object>>) tableDef.get("common");
                        for (Map<String, Object> drop : common) {
                            table.commonDrops.add(parseLootDrop(drop));
                        }
                    }
                    
                    // Rare drops
                    if (tableDef.containsKey("rare")) {
                        List<Map<String, Object>> rare = (List<Map<String, Object>>) tableDef.get("rare");
                        for (Map<String, Object> drop : rare) {
                            table.rareDrops.add(parseLootDrop(drop));
                        }
                    }
                    
                    worldData.lootTables.put(tableId, table);
                }
            }
            
            // Load NPCs
            if (yaml.containsKey("npcs")) {
                List<Map<String, Object>> npcs = (List<Map<String, Object>>) yaml.get("npcs");
                for (Map<String, Object> npcDef : npcs) {
                    WorldData.NPCDefinition npcDef2 = new WorldData.NPCDefinition();
                    npcDef2.id = getInt(npcDef, "id", 0);
                    npcDef2.definitionId = getInt(npcDef, "definition_id", npcDef2.id);
                    npcDef2.name = getString(npcDef, "name", "Unknown");
                    npcDef2.combatLevel = getInt(npcDef, "combat_level", 0);
                    npcDef2.maxHp = getInt(npcDef, "max_hp", 100);
                    npcDef2.maxHit = getInt(npcDef, "max_hit", 1);
                    npcDef2.respawnDelayTicks = getInt(npcDef, "respawn_delay_ticks", 3850);
                    npcDef2.location = getString(npcDef, "location", "lumbridge");
                    npcDef2.x = getInt(npcDef, "x", 0);
                    npcDef2.y = getInt(npcDef, "y", 0);
                    npcDef2.isAggressive = getBoolean(npcDef, "is_aggressive", false);
                    npcDef2.lootTable = getString(npcDef, "loot_table", "default");
                    npcDef2.examine = getString(npcDef, "examine", "It's a " + npcDef2.name + ".");
                    npcDef2.wanderRadius    = getInt(npcDef, "wander_radius", 0);
                    npcDef2.attackRange     = getInt(npcDef, "attack_range", 1);
                    npcDef2.attackLevel     = getInt(npcDef, "attack_level",   -1);
                    npcDef2.strengthLevel   = getInt(npcDef, "strength_level", -1);
                    npcDef2.defenceLevel    = getInt(npcDef, "defence_level",  -1);
                    npcDef2.attackBonus     = getInt(npcDef, "attack_bonus",   0);
                    npcDef2.strengthBonus   = getInt(npcDef, "strength_bonus", 0);
                    npcDef2.defenceBonus    = getInt(npcDef, "defence_bonus",  0);

                    worldData.npcs.add(npcDef2);
                }
                LOG.info("Loaded {} NPCs", worldData.npcs.size());
            }
            
            return worldData;
            
        } catch (Exception e) {
            LOG.error("Failed to load world configuration", e);
            throw e;
        }
    }
    
    // Helper methods
    private static int getInt(Map<String, Object> map, String key, int defaultValue) {
        if (!map.containsKey(key)) return defaultValue;
        Object val = map.get(key);
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof Number) return ((Number) val).intValue();
        return defaultValue;
    }
    
    private static String getString(Map<String, Object> map, String key, String defaultValue) {
        if (!map.containsKey(key)) return defaultValue;
        Object val = map.get(key);
        return val != null ? val.toString() : defaultValue;
    }
    
    private static boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        if (!map.containsKey(key)) return defaultValue;
        Object val = map.get(key);
        if (val instanceof Boolean) return (Boolean) val;
        return defaultValue;
    }
    
    private static WorldData.LootDrop parseLootDrop(Map<String, Object> drop) {
        WorldData.LootDrop lootDrop = new WorldData.LootDrop();
        lootDrop.itemId = getInt(drop, "item_id", 0);
        lootDrop.minQuantity = getInt(drop, "quantity", 1);
        lootDrop.maxQuantity = getInt(drop, "max_quantity", lootDrop.minQuantity);
        lootDrop.chance = getInt(drop, "chance", 100);  // Default: always
        return lootDrop;
    }
}

/**
 * Parsed world data structure
 */
class WorldData {
    public int spawnX = 3222;
    public int spawnY = 3218;
    public Map<String, MapInfo> maps = new HashMap<>();
    public List<NPCDefinition> npcs = new ArrayList<>();
    public Map<String, LootTable> lootTables = new HashMap<>();
    
    static class MapInfo {
        public String name;
        public String displayName;
        public int minX, minY, maxX, maxY;
    }
    
    static class NPCDefinition {
        public int id;
        public int definitionId;
        public String name;
        public int combatLevel;
        public int maxHp = 100;
        public int maxHit = 1;
        public int respawnDelayTicks = 3850; // server ticks (~25 OSRS ticks)
        public String location;
        public int x, y;
        public boolean isAggressive;
        public String lootTable;
        public String examine;
        public int wanderRadius;
        public int attackRange = 1;
        // OSRS combat stats (defaults to combatLevel if not set in world.yml)
        public int attackLevel   = -1;
        public int strengthLevel = -1;
        public int defenceLevel  = -1;
        public int attackBonus   = 0;
        public int strengthBonus = 0;
        public int defenceBonus  = 0;
    }
    
    static class LootTable {
        public String id;
        public List<LootDrop> alwaysDrops = new ArrayList<>();
        public List<LootDrop> commonDrops = new ArrayList<>();
        public List<LootDrop> rareDrops = new ArrayList<>();
    }
    
    static class LootDrop {
        public int itemId;
        public int minQuantity;
        public int maxQuantity;
        public int chance;  // 0-100, 100 = always
    }
}
