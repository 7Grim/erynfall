package com.osrs.client.world;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads the tile map from map.yaml (classpath resource).
 * Mirrors server-side TileMap.java — keep in sync when map.yaml format changes.
 */
public class MapLoader {

    private static final Logger LOG = LoggerFactory.getLogger(MapLoader.class);

    public static final int WIDTH = 104;
    public static final int HEIGHT = 104;

    private final int[][] layout;
    private final Map<Integer, Boolean> walkableById;

    private MapLoader(int[][] layout, Map<Integer, Boolean> walkableById) {
        this.layout = layout;
        this.walkableById = walkableById;
    }

    /** Load map.yaml from the classpath. Throws RuntimeException on failure. */
    public static MapLoader load() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try (InputStream is = MapLoader.class.getResourceAsStream("/map.yaml")) {
            if (is == null) throw new IllegalStateException("map.yaml not found in classpath");

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

            int w = ((Number) data.getOrDefault("width", WIDTH)).intValue();
            int h = ((Number) data.getOrDefault("height", HEIGHT)).intValue();
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

            LOG.info("Client map loaded: {}x{} tiles, {} walkability types", w, h, walkable.size());
            return new MapLoader(layout, walkable);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load client map.yaml", e);
        }
    }

    public int[][] getLayout() { return layout; }

    public boolean isWalkableTile(int tileId) {
        return walkableById.getOrDefault(tileId, false);
    }

    public boolean isWalkable(int x, int y) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return false;
        return isWalkableTile(layout[x][y]);
    }
}
