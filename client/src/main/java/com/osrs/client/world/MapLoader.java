package com.osrs.client.world;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.osrs.client.LaunchOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads gameplay tile semantics from map.yaml (classpath resource).
 * Mirrors server-side TileMap.java — keep in sync when map.yaml format changes.
 *
 * This loader remains gameplay-authoritative for walkability/pathing semantics.
 * Visual terrain appearance may be overridden client-side by artist-owned sources.
 */
public class MapLoader {

    private static final Logger LOG = LoggerFactory.getLogger(MapLoader.class);

    public static final int DEFAULT_WIDTH = 104;
    public static final int DEFAULT_HEIGHT = 104;
    public static final int WIDTH = DEFAULT_WIDTH;
    public static final int HEIGHT = DEFAULT_HEIGHT;

    private final int[][] layout;
    private final Map<Integer, Boolean> walkableById;
    private final int width;
    private final int height;

    private MapLoader(int[][] layout, Map<Integer, Boolean> walkableById, int width, int height) {
        this.layout = layout;
        this.walkableById = walkableById;
        this.width = width;
        this.height = height;
    }

    /** Load world-specific map from the classpath. Throws RuntimeException on failure. */
    public static MapLoader load() {
        return load(LaunchOptions.normal());
    }

    public static MapLoader load(LaunchOptions launchOptions) {
        LaunchOptions options = launchOptions == null ? LaunchOptions.normal() : launchOptions;
        String worldId = options.worldId();
        String mapResourcePath = "/worlds/" + worldId + "/map.yaml";
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try (InputStream is = MapLoader.class.getResourceAsStream(mapResourcePath)) {
            if (is == null) throw new IllegalStateException("map not found in classpath at " + mapResourcePath);

            @SuppressWarnings("unchecked")
            Map<String, Object> data = mapper.readValue(is, Map.class);

            Map<Integer, Boolean> walkable = new HashMap<>();
            Object tilesObj = data.get("tiles");
            if (tilesObj instanceof List<?> tiles) {
                for (Object raw : tiles) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> tile = (Map<String, Object>) raw;
                    int id = ((Number) tile.getOrDefault("id", -1)).intValue();
                    boolean w = Boolean.TRUE.equals(tile.get("walkable"));
                    if (id >= 0) walkable.put(id, w);
                }
            }
            walkable.putIfAbsent(0, true); // grass default

            int w = ((Number) data.getOrDefault("width", DEFAULT_WIDTH)).intValue();
            int h = ((Number) data.getOrDefault("height", DEFAULT_HEIGHT)).intValue();
            if (w <= 0 || h <= 0) {
                throw new IllegalStateException("Invalid map dimensions: " + w + "x" + h);
            }
            int[][] layout = new int[w][h];

            Object layoutObj = data.get("layout");
            if (layoutObj instanceof String text) {
                String[] rows = text.split("\\R");
                int y = 0;
                for (String row : rows) {
                    String trimmed = row.trim();
                    if (trimmed.isEmpty() || y >= h) continue;
                    String[] cells = trimmed.split("\\s+");
                    int maxX = Math.min(w, cells.length);
                    for (int x = 0; x < maxX; x++) {
                        try {
                            layout[x][y] = Integer.parseInt(cells[x]);
                        } catch (NumberFormatException ignored) {
                            layout[x][y] = 0;
                        }
                    }
                    y++;
                }
            }

            LOG.info("Client gameplay map loaded for world '{}': {}x{} tiles, {} walkability types", worldId, w, h, walkable.size());
            return new MapLoader(layout, walkable, w, h);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load client map for world '" + worldId + "'", e);
        }
    }

    public int[][] getLayout() { return layout; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public boolean isWalkableTile(int tileId) {
        return walkableById.getOrDefault(tileId, false);
    }

    public boolean isWalkable(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return false;
        return isWalkableTile(layout[x][y]);
    }
}
