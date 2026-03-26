package com.osrs.server.world;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
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
    private Map<Integer, Boolean> walkableCache = new HashMap<>();
    
    public void load(String filePath) throws Exception {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        File file = resolveMapFile(filePath);
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("Map file not found: " + filePath);
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> data = mapper.readValue(file, Map.class);
        
        width = ((Number) data.getOrDefault("width", 104)).intValue();
        height = ((Number) data.getOrDefault("height", 104)).intValue();

        walkableCache.clear();
        Object tilesObj = data.get("tiles");
        if (tilesObj instanceof List<?> tiles) {
            for (Object tileObj : tiles) {
                if (!(tileObj instanceof Map<?, ?> raw)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> tile = (Map<String, Object>) raw;
                int id = ((Number) tile.getOrDefault("id", -1)).intValue();
                boolean walkable = Boolean.TRUE.equals(tile.get("walkable"));
                if (id >= 0) {
                    walkableCache.put(id, walkable);
                }
            }
        }

        // Safe default for grass-like tile when metadata is missing.
        walkableCache.putIfAbsent(0, true);

        // Default all cells to tile id 0, then overlay parsed layout rows.
        layout = new int[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                layout[y][x] = 0;
            }
        }

        int parsedRows = parseLayout(data.get("layout"));
        if (parsedRows == 0) {
            LOG.warn("Map file '{}' has empty or invalid layout; using all-default tiles", file.getPath());
        }

        LOG.info("Loaded map: {} x {} tiles ({} walkability types, {} layout rows)",
            width, height, walkableCache.size(), parsedRows);
    }
    
    public boolean isWalkable(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return false;
        }
        
        int tileId = layout[y][x];
        return walkableCache.getOrDefault(tileId, false);
    }
    
    /**
     * Initialize a default walkable map (all tiles walkable)
     * Used when no YAML map file is loaded
     */
    public void initializeDefaultMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.layout = new int[height][width];
        this.walkableCache = new HashMap<>();
        this.walkableCache.put(0, true);
        
        // Initialize all tiles as walkable (grass)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                layout[y][x] = 0;  // 0 = grass (walkable)
            }
        }
        
        LOG.info("Initialized default map: {} x {} tiles", width, height);
    }

    private int parseLayout(Object layoutObj) {
        if (!(layoutObj instanceof String layoutText)) {
            return 0;
        }

        String[] rows = layoutText.split("\\R");
        int y = 0;
        for (String row : rows) {
            String trimmed = row.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (y >= height) {
                break;
            }

            String[] cells = trimmed.split("\\s+");
            int maxX = Math.min(width, cells.length);
            for (int x = 0; x < maxX; x++) {
                try {
                    layout[y][x] = Integer.parseInt(cells[x]);
                } catch (NumberFormatException ignored) {
                    layout[y][x] = 0;
                }
            }
            y++;
        }
        return y;
    }

    private File resolveMapFile(String filePath) {
        File direct = new File(filePath);
        if (direct.exists()) return direct;

        Path cwdAssets = Paths.get("assets", "data", filePath);
        if (Files.exists(cwdAssets)) return cwdAssets.toFile();

        Path serverAssets = Paths.get("..", "assets", "data", filePath);
        if (Files.exists(serverAssets)) return serverAssets.toFile();

        return direct;
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
