package com.osrs.client.world;

import com.badlogic.gdx.Gdx;
import com.osrs.client.LaunchOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TerrainVisualLoader {

    public record TerrainVisualRegion(int minX, int minY, int maxX, int maxY, int type, String themeId) {}

    /** Polygon region — vertices in tile coords, filled via scanline. Min 3 vertices required. */
    public record TerrainVisualPolygon(int[] xs, int[] ys, int type) {}

    public record TerrainVisualData(Integer defaultType,
                                    List<TerrainVisualRegion> regions,
                                    List<TerrainVisualPolygon> polygons,
                                    Map<Integer, Integer> tileOverrides,
                                    WorldTheme[] tileThemeMap,
                                    boolean repoBacked,
                                    String sourcePath) {}

    private TerrainVisualLoader() {}

    public static int[][] load(int[][] gameplayTileMap) {
        return load(LaunchOptions.normal(), gameplayTileMap);
    }

    public static int[][] load(LaunchOptions launchOptions, int[][] gameplayTileMap) {
        TerrainVisualData data = loadTerrainVisualData(launchOptions);
        return composeVisualTileMap(gameplayTileMap, data, data.tileOverrides());
    }

    public static int[][] loadWithOverrides(LaunchOptions launchOptions,
                                            int[][] gameplayTileMap,
                                            Map<Integer, Integer> tileOverrides) {
        TerrainVisualData data = loadTerrainVisualData(launchOptions);
        return composeVisualTileMap(gameplayTileMap, data, tileOverrides);
    }

    public static Map<Integer, Integer> loadTileOverrides(LaunchOptions launchOptions) {
        return new HashMap<>(loadTerrainVisualData(launchOptions).tileOverrides());
    }

    public static TerrainVisualData loadTerrainVisualData(LaunchOptions launchOptions) {
        LaunchOptions options = launchOptions == null ? LaunchOptions.normal() : launchOptions;
        ArrayList<TerrainVisualRegion> regions = new ArrayList<>();
        ArrayList<TerrainVisualPolygon> polygons = new ArrayList<>();
        HashMap<Integer, Integer> overrides = new HashMap<>();

        try {
            WorldSceneLoader.WorldSceneData sceneData = WorldSceneLoader.load(options);
            Map<String, Object> data = sceneData.terrainVisual();
            if (data.isEmpty()) {
                Gdx.app.log("TerrainVisualLoader", "No terrain_visual block found; using gameplay map visuals");
                return new TerrainVisualData(null, List.of(), List.of(), Map.of(), null, sceneData.repoBacked(), sceneData.sourcePath());
            }

            Integer defaultType = parseTileType(data.get("default_type"));

            Object regionsObj = data.get("regions");
            if (regionsObj instanceof List<?> regionList) {
                for (Object row : regionList) {
                    if (!(row instanceof Map<?, ?> regionRow)) {
                        continue;
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> region = (Map<String, Object>) regionRow;
                    Integer type = parseTileType(region.get("type"));
                    if (type == null) {
                        continue;
                    }
                    if (region.containsKey("polygon")) {
                        TerrainVisualPolygon polygon = parsePolygon(region.get("polygon"), type);
                        if (polygon != null) {
                            polygons.add(polygon);
                        }
                        continue;
                    }
                    int minX = region.get("min_x") instanceof Number n ? n.intValue() : 0;
                    int minY = region.get("min_y") instanceof Number n ? n.intValue() : 0;
                    int maxX = region.get("max_x") instanceof Number n ? n.intValue() : -1;
                    int maxY = region.get("max_y") instanceof Number n ? n.intValue() : -1;
                    if (maxX < minX || maxY < minY) {
                        continue;
                    }
                    int clampedMinX = Math.max(0, minX);
                    int clampedMinY = Math.max(0, minY);
                    int clampedMaxX = Math.min(MapLoader.WIDTH - 1, maxX);
                    int clampedMaxY = Math.min(MapLoader.HEIGHT - 1, maxY);
                    String themeId = region.get("theme") instanceof String s ? s : null;
                    regions.add(new TerrainVisualRegion(clampedMinX, clampedMinY, clampedMaxX, clampedMaxY, type, themeId));
                }
            }

            Object overridesObj = data.get("tile_overrides");
            if (overridesObj instanceof List<?> overrideList) {
                for (Object row : overrideList) {
                    if (!(row instanceof Map<?, ?> overrideRow)) {
                        continue;
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> override = (Map<String, Object>) overrideRow;
                    Integer type = parseTileType(override.get("type"));
                    if (type == null) {
                        continue;
                    }
                    int x = override.get("x") instanceof Number n ? n.intValue() : -1;
                    int y = override.get("y") instanceof Number n ? n.intValue() : -1;
                    if (x < 0 || y < 0 || x >= MapLoader.WIDTH || y >= MapLoader.HEIGHT) {
                        continue;
                    }
                    overrides.put(tileIndex(x, y), type);
                }
            }

            Map<String, WorldTheme> themes = WorldThemeLoader.load(sceneData.rawThemes());
            WorldTheme[] tileThemeMap = buildTileThemeMap(regions, themes);

            String source = sceneData.repoBacked() ? "repo" : "classpath";
            int polyCount = polygons.size();
            int themeCount = themes.size();
            Gdx.app.log("TerrainVisualLoader", "Loaded terrain visual map from " + source + " scene source"
                + (polyCount > 0 ? " (" + polyCount + " polygon region(s))" : "")
                + (themeCount > 0 ? " [" + themeCount + " theme(s)]" : ""));
            return new TerrainVisualData(defaultType, regions, polygons, overrides, tileThemeMap, sceneData.repoBacked(), sceneData.sourcePath());
        } catch (Exception e) {
            Gdx.app.log("TerrainVisualLoader", "WARN: failed to load terrain visuals: " + e.getMessage());
            return new TerrainVisualData(null, List.of(), List.of(), Map.of(), null, options.artistMode(), "");
        }
    }

    public static int[][] composeVisualTileMap(int[][] gameplayTileMap,
                                               TerrainVisualData terrainVisualData,
                                               Map<Integer, Integer> tileOverrides) {
        int[][] visual = copyTileMap(gameplayTileMap);
        if (visual.length == 0) {
            return visual;
        }
        if (terrainVisualData == null) {
            return visual;
        }

        if (terrainVisualData.defaultType() != null) {
            int defaultType = terrainVisualData.defaultType();
            for (int x = 0; x < visual.length; x++) {
                for (int y = 0; y < visual[x].length; y++) {
                    visual[x][y] = defaultType;
                }
            }
        }

        for (TerrainVisualRegion region : terrainVisualData.regions()) {
            for (int x = region.minX(); x <= region.maxX(); x++) {
                for (int y = region.minY(); y <= region.maxY(); y++) {
                    visual[x][y] = region.type();
                }
            }
        }

        for (TerrainVisualPolygon polygon : terrainVisualData.polygons()) {
            fillPolygon(visual, polygon.xs(), polygon.ys(), polygon.type());
        }

        Map<Integer, Integer> effectiveOverrides = tileOverrides == null
            ? terrainVisualData.tileOverrides()
            : tileOverrides;
        for (Map.Entry<Integer, Integer> entry : effectiveOverrides.entrySet()) {
            int index = entry.getKey();
            int x = index % MapLoader.WIDTH;
            int y = index / MapLoader.WIDTH;
            if (x < 0 || y < 0 || x >= MapLoader.WIDTH || y >= MapLoader.HEIGHT) {
                continue;
            }
            Integer type = entry.getValue();
            if (type == null || type < 0 || type > 4) {
                continue;
            }
            visual[x][y] = type;
        }
        return visual;
    }

    public static int tileIndex(int x, int y) {
        return y * MapLoader.WIDTH + x;
    }

    public static String terrainTypeLabel(int type) {
        return switch (type) {
            case 1 -> "water";
            case 2 -> "path";
            case 3 -> "wall";
            case 4 -> "sand";
            default -> "grass";
        };
    }

    private static int[][] copyTileMap(int[][] source) {
        if (source == null || source.length == 0) {
            return new int[0][0];
        }
        int[][] copy = new int[source.length][];
        for (int x = 0; x < source.length; x++) {
            if (source[x] == null) {
                copy[x] = new int[0];
                continue;
            }
            copy[x] = source[x].clone();
        }
        return copy;
    }

    /**
     * Parses a polygon vertex list from YAML. Supports two vertex formats:
     *   - sequence: [[x, y], [x, y], ...]
     *   - map list: [{x: N, y: N}, ...]
     * Returns null if fewer than 3 vertices are parseable.
     */
    private static TerrainVisualPolygon parsePolygon(Object polygonObj, int type) {
        if (!(polygonObj instanceof List<?> vertexList) || vertexList.size() < 3) {
            return null;
        }
        int n = vertexList.size();
        int[] xs = new int[n];
        int[] ys = new int[n];
        int count = 0;
        for (Object v : vertexList) {
            if (v instanceof List<?> pair && pair.size() >= 2
                    && pair.get(0) instanceof Number vx && pair.get(1) instanceof Number vy) {
                xs[count] = vx.intValue();
                ys[count] = vy.intValue();
                count++;
            } else if (v instanceof Map<?, ?> vmap) {
                Object vx = vmap.get("x");
                Object vy = vmap.get("y");
                if (vx instanceof Number nx && vy instanceof Number ny) {
                    xs[count] = nx.intValue();
                    ys[count] = ny.intValue();
                    count++;
                }
            }
        }
        if (count < 3) {
            return null;
        }
        if (count < n) {
            xs = Arrays.copyOf(xs, count);
            ys = Arrays.copyOf(ys, count);
        }
        return new TerrainVisualPolygon(xs, ys, type);
    }

    /**
     * Scanline-fills a polygon onto the visual tile map.
     * Uses the standard edge-crossing rule: edge (y1..y2) contributes to scanline y
     * when y1 <= y < y2 (or y2 <= y < y1), preventing double-counting at vertices.
     */
    private static void fillPolygon(int[][] visual, int[] xs, int[] ys, int type) {
        int n = xs.length;
        int minY = ys[0], maxY = ys[0];
        for (int i = 1; i < n; i++) {
            if (ys[i] < minY) minY = ys[i];
            if (ys[i] > maxY) maxY = ys[i];
        }
        minY = Math.max(0, minY);
        maxY = Math.min(MapLoader.HEIGHT - 1, maxY);

        List<Integer> crossings = new ArrayList<>();
        for (int scanY = minY; scanY <= maxY; scanY++) {
            crossings.clear();
            for (int i = 0; i < n; i++) {
                int x1 = xs[i], y1 = ys[i];
                int x2 = xs[(i + 1) % n], y2 = ys[(i + 1) % n];
                if ((y1 <= scanY && y2 > scanY) || (y2 <= scanY && y1 > scanY)) {
                    // Integer intersection: x1 + (scanY - y1) * (x2 - x1) / (y2 - y1)
                    int crossX = x1 + (scanY - y1) * (x2 - x1) / (y2 - y1);
                    crossings.add(crossX);
                }
            }
            crossings.sort(Integer::compareTo);
            for (int i = 0; i + 1 < crossings.size(); i += 2) {
                int startX = Math.max(0, crossings.get(i));
                int endX = Math.min(MapLoader.WIDTH - 1, crossings.get(i + 1));
                for (int x = startX; x <= endX; x++) {
                    if (x < visual.length && scanY < visual[x].length) {
                        visual[x][scanY] = type;
                    }
                }
            }
        }
    }

    /**
     * Builds a flat per-tile theme array (stride = MapLoader.WIDTH).
     * Regions are applied in list order; later regions overwrite earlier ones.
     * Returns null if there are no themed regions.
     */
    private static WorldTheme[] buildTileThemeMap(List<TerrainVisualRegion> regions,
                                                   Map<String, WorldTheme> themes) {
        if (themes.isEmpty()) {
            return null;
        }
        boolean anyTheme = false;
        for (TerrainVisualRegion r : regions) {
            if (r.themeId() != null && themes.containsKey(r.themeId())) {
                anyTheme = true;
                break;
            }
        }
        if (!anyTheme) {
            return null;
        }
        WorldTheme[] map = new WorldTheme[MapLoader.WIDTH * MapLoader.HEIGHT];
        for (TerrainVisualRegion r : regions) {
            if (r.themeId() == null) {
                continue;
            }
            WorldTheme theme = themes.get(r.themeId());
            if (theme == null) {
                continue;
            }
            for (int x = r.minX(); x <= r.maxX(); x++) {
                for (int y = r.minY(); y <= r.maxY(); y++) {
                    map[x + y * MapLoader.WIDTH] = theme;
                }
            }
        }
        return map;
    }

    private static Integer parseTileType(Object value) {
        if (value instanceof Number n) {
            int type = n.intValue();
            return type >= 0 && type <= 4 ? type : null;
        }
        if (!(value instanceof String raw)) {
            return null;
        }
        String normalized = raw.trim().toLowerCase();
        if (normalized.isEmpty()) {
            return null;
        }
        return switch (normalized) {
            case "0", "grass" -> 0;
            case "1", "water" -> 1;
            case "2", "path", "dirt" -> 2;
            case "3", "wall", "rock" -> 3;
            case "4", "sand" -> 4;
            default -> null;
        };
    }
}
