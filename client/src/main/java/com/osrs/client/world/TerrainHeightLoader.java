package com.osrs.client.world;

import com.badlogic.gdx.Gdx;
import com.osrs.client.LaunchOptions;
import com.osrs.client.WorldScale;

import java.util.List;
import java.util.Map;

public final class TerrainHeightLoader {
    private static final float DEFAULT_HEIGHT_STEP = WorldScale.TERRAIN_HEIGHT_STEP_DEFAULT;

    private TerrainHeightLoader() {}

    public static final class TerrainHeightData {
        public final int[][] levels;
        public final float heightStep;

        public TerrainHeightData(int[][] levels, float heightStep) {
            this.levels = levels;
            this.heightStep = heightStep;
        }
    }

    public static TerrainHeightData load() {
        return load(LaunchOptions.normal());
    }

    public static TerrainHeightData load(LaunchOptions launchOptions) {
        int[][] levels = new int[MapLoader.WIDTH][MapLoader.HEIGHT];
        float heightStep = DEFAULT_HEIGHT_STEP;

        try {
            WorldSceneLoader.WorldSceneData sceneData = WorldSceneLoader.load(launchOptions);
            Map<String, Object> data = sceneData.terrainHeight();
            if (data.isEmpty()) {
                Gdx.app.log("TerrainHeightLoader", "WARN: terrain_height block missing; using flat terrain");
                return new TerrainHeightData(levels, heightStep);
            }

            Object stepValue = data.get("height_step");
            if (stepValue instanceof Number n && n.floatValue() > 0f) {
                heightStep = n.floatValue();
            }

            Object regionsObj = data.get("regions");
            if (regionsObj instanceof List<?> regions) {
                for (Object row : regions) {
                    if (!(row instanceof Map<?, ?> regionRow)) {
                        continue;
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> region = (Map<String, Object>) regionRow;
                    int minX = region.get("min_x") instanceof Number n ? n.intValue() : 0;
                    int minY = region.get("min_y") instanceof Number n ? n.intValue() : 0;
                    int maxX = region.get("max_x") instanceof Number n ? n.intValue() : 0;
                    int maxY = region.get("max_y") instanceof Number n ? n.intValue() : 0;
                    int level = region.get("level") instanceof Number n ? n.intValue() : 0;

                    if (maxX < minX || maxY < minY || level < 0) {
                        continue;
                    }

                    int clampedMinX = Math.max(0, minX);
                    int clampedMinY = Math.max(0, minY);
                    int clampedMaxX = Math.min(MapLoader.WIDTH - 1, maxX);
                    int clampedMaxY = Math.min(MapLoader.HEIGHT - 1, maxY);
                    for (int x = clampedMinX; x <= clampedMaxX; x++) {
                        for (int y = clampedMinY; y <= clampedMaxY; y++) {
                            levels[x][y] = level;
                        }
                    }
                }
            }

            Object overridesObj = data.get("tile_overrides");
            if (overridesObj instanceof List<?> overrides) {
                for (Object row : overrides) {
                    if (!(row instanceof Map<?, ?> overrideRow)) {
                        continue;
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> override = (Map<String, Object>) overrideRow;
                    int x = override.get("x") instanceof Number n ? n.intValue() : -1;
                    int y = override.get("y") instanceof Number n ? n.intValue() : -1;
                    int level = override.get("level") instanceof Number n ? n.intValue() : 0;
                    if (x < 0 || y < 0 || x >= MapLoader.WIDTH || y >= MapLoader.HEIGHT || level < 0) {
                        continue;
                    }
                    levels[x][y] = level;
                }
            }

            String source = sceneData.repoBacked() ? "repo" : "classpath";
            Gdx.app.log("TerrainHeightLoader", "Loaded terrain heights with step=" + heightStep + " from " + source + " scene");
            return new TerrainHeightData(levels, heightStep);
        } catch (Exception e) {
            Gdx.app.log("TerrainHeightLoader", "WARN: failed to load terrain height scene data: " + e.getMessage());
            return new TerrainHeightData(levels, DEFAULT_HEIGHT_STEP);
        }
    }
}
