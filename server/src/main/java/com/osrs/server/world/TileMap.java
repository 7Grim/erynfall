package com.osrs.server.world;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Tutorial Island tile map (104x104).
 * Loads from YAML, provides collision queries.
 */
public class TileMap {
    
    private static final Logger LOG = LoggerFactory.getLogger(TileMap.class);
    
    private int width;
    private int height;
    private int[][] layout;
    private Map<Integer, Boolean> walkableCache;
    
    public void load(String filePath) throws Exception {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        
        // Load YAML
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("Map file not found: " + filePath);
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> data = mapper.readValue(file, Map.class);
        
        width = (Integer) data.get("width");
        height = (Integer) data.get("height");
        
        // For now, default all tiles to walkable (placeholder)
        // In production, parse layout from YAML
        layout = new int[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                layout[y][x] = 0; // Grass (walkable)
            }
        }
        
        LOG.info("Loaded map: {} x {} tiles", width, height);
    }
    
    public boolean isWalkable(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return false;
        }
        
        int tileId = layout[y][x];
        // Tile 0 (grass) = walkable, all others = not walkable (for now)
        return tileId == 0;
    }
    
    /**
     * Initialize a default walkable map (all tiles walkable)
     * Used when no YAML map file is loaded
     */
    public void initializeDefaultMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.layout = new int[height][width];
        
        // Initialize all tiles as walkable (grass)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                layout[y][x] = 0;  // 0 = grass (walkable)
            }
        }
        
        LOG.info("Initialized default map: {} x {} tiles", width, height);
    }
    
    /**
     * Get total number of tiles
     */
    public int getTotalTiles() {
        return width * height;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
}
