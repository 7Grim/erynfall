package com.osrs.server.world;

import com.osrs.shared.Entity;
import com.osrs.shared.NPC;
import com.osrs.shared.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * World state: all entities, tile map, and player sessions.
 */
public class World {
    
    private static final Logger LOG = LoggerFactory.getLogger(World.class);
    
    private final TileMap tileMap;
    private final Map<Integer, Player> players = new HashMap<>();
    private final Map<Integer, NPC> npcs = new HashMap<>();
    private int nextEntityId = 1;
    
    public World() throws Exception {
        this.tileMap = new TileMap();
        tileMap.load("assets/data/map.yaml");
        LOG.info("World initialized: {} x {} tiles", tileMap.getWidth(), tileMap.getHeight());
    }
    
    public void spawnPlayer(int playerId, String playerName, int x, int y) {
        Player player = new Player(playerId, playerName, x, y);
        players.put(playerId, player);
        LOG.info("Player {} spawned at ({}, {})", playerName, x, y);
    }
    
    public void spawnNPC(int npcId, String npcName, int definitionId, int x, int y) {
        NPC npc = new NPC(npcId, npcName, definitionId, x, y);
        npcs.put(npcId, npc);
        LOG.debug("NPC {} spawned at ({}, {})", npcName, x, y);
    }
    
    public Player getPlayer(int playerId) {
        return players.get(playerId);
    }
    
    public NPC getNPC(int npcId) {
        return npcs.get(npcId);
    }
    
    public boolean canWalkTo(int x, int y) {
        return tileMap.isWalkable(x, y);
    }
    
    public Map<Integer, Player> getPlayers() {
        return players;
    }
    
    public Map<Integer, NPC> getNPCs() {
        return npcs;
    }
    
    public TileMap getTileMap() {
        return tileMap;
    }
}
