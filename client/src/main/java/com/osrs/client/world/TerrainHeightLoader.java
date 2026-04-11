package com.osrs.client.world;

import com.badlogic.gdx.Gdx;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.util.List;
import java.util.Map;

public final class TerrainHeightLoader {
    private static final float DEFAULT_HEIGHT_STEP = 0.6f;

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
        int[][] levels = new int[MapLoader.WIDTH][MapLoader.HEIGHT];
        float heightStep = DEFAULT_HEIGHT_STEP;

        try {
            if (!Gdx.files.internal("terrain_height.yaml").exists()) {
                Gdx.app.log("TerrainHeightLoader", "WARN: terrain_height.yaml not found; using flat terrain");
                return new TerrainHeightData(levels, heightStep);
            }

            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            String yaml = Gdx.files.internal("terrain_height.yaml").readString();
            @SuppressWarnings("unchecked")
            Map<String, Object> data = mapper.readValue(yaml, Map.class);

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

            Gdx.app.log("TerrainHeightLoader", "Loaded terrain heights with step=" + heightStep);
            return new TerrainHeightData(levels, heightStep);
        } catch (Exception e) {
            Gdx.app.log("TerrainHeightLoader", "WARN: failed to load terrain_height.yaml: " + e.getMessage());
            return new TerrainHeightData(levels, DEFAULT_HEIGHT_STEP);
        }
    }
}
