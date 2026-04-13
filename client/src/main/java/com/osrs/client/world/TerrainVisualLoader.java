package com.osrs.client.world;

import com.badlogic.gdx.Gdx;
import com.osrs.client.LaunchOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TerrainVisualLoader {

    public record TerrainVisualRegion(int minX, int minY, int maxX, int maxY, int type) {}

    public record TerrainVisualData(Integer defaultType,
                                    List<TerrainVisualRegion> regions,
                                    Map<Integer, Integer> tileOverrides,
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
        HashMap<Integer, Integer> overrides = new HashMap<>();

        try {
            WorldSceneLoader.WorldSceneData sceneData = WorldSceneLoader.load(options);
            Map<String, Object> data = sceneData.terrainVisual();
            if (data.isEmpty()) {
                Gdx.app.log("TerrainVisualLoader", "No terrain_visual block found; using gameplay map visuals");
                return new TerrainVisualData(null, List.of(), Map.of(), sceneData.repoBacked(), sceneData.sourcePath());
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
                    regions.add(new TerrainVisualRegion(clampedMinX, clampedMinY, clampedMaxX, clampedMaxY, type));
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

            String source = sceneData.repoBacked() ? "repo" : "classpath";
            Gdx.app.log("TerrainVisualLoader", "Loaded terrain visual map from " + source + " scene source");
            return new TerrainVisualData(defaultType, regions, overrides, sceneData.repoBacked(), sceneData.sourcePath());
        } catch (Exception e) {
            Gdx.app.log("TerrainVisualLoader", "WARN: failed to load terrain visuals: " + e.getMessage());
            return new TerrainVisualData(null, List.of(), Map.of(), options.artistMode(), "");
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
