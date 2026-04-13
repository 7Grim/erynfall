package com.osrs.client.art;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.osrs.client.world.StaticPropLoader;
import com.osrs.client.world.TerrainVisualLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ScenePersistence {

    public record SaveResult(boolean success, String message) {}

    private ScenePersistence() {}

    public static SaveResult saveStaticProps(Path scenePath, List<StaticPropLoader.StaticPropPlacement> placements) {
        if (scenePath == null) {
            return new SaveResult(false, "Scene path not configured");
        }
        if (!Files.exists(scenePath)) {
            return new SaveResult(false, "Scene file not found: " + scenePath);
        }

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            @SuppressWarnings("unchecked")
            LinkedHashMap<String, Object> root = mapper.readValue(Files.readString(scenePath), LinkedHashMap.class);
            ArrayList<Map<String, Object>> serialized = new ArrayList<>();
            if (placements != null) {
                for (StaticPropLoader.StaticPropPlacement p : placements) {
                    LinkedHashMap<String, Object> row = new LinkedHashMap<>();
                    row.put("key", p.key);
                    row.put("x", p.x);
                    row.put("y", p.y);
                    row.put("rotation_y_degrees", p.rotationYDegrees);
                    row.put("scale", p.scale);
                    row.put("visibility_group", p.visibilityGroup);
                    serialized.add(row);
                }
            }
            root.put("static_props", serialized);

            String yaml = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            Path temp = scenePath.resolveSibling(scenePath.getFileName() + ".tmp");
            Files.writeString(temp, yaml, StandardCharsets.UTF_8);
            try {
                Files.move(temp, scenePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception atomicIgnored) {
                Files.move(temp, scenePath, StandardCopyOption.REPLACE_EXISTING);
            }
            return new SaveResult(true, "Saved scene static props to " + scenePath.getFileName());
        } catch (Exception e) {
            return new SaveResult(false, "Scene save failed: " + e.getMessage());
        }
    }

    public static SaveResult saveTerrainVisualOverrides(Path scenePath, Map<Integer, Integer> overrides) {
        if (scenePath == null) {
            return new SaveResult(false, "Scene path not configured");
        }
        if (!Files.exists(scenePath)) {
            return new SaveResult(false, "Scene file not found: " + scenePath);
        }

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            @SuppressWarnings("unchecked")
            LinkedHashMap<String, Object> root = mapper.readValue(Files.readString(scenePath), LinkedHashMap.class);
            @SuppressWarnings("unchecked")
            LinkedHashMap<String, Object> terrainVisual = root.get("terrain_visual") instanceof Map<?, ?> m
                ? new LinkedHashMap<>((Map<String, Object>) m)
                : new LinkedHashMap<>();

            ArrayList<Map<String, Object>> serialized = new ArrayList<>();
            if (overrides != null && !overrides.isEmpty()) {
                ArrayList<Integer> keys = new ArrayList<>(overrides.keySet());
                keys.sort(Integer::compareTo);
                for (Integer index : keys) {
                    if (index == null) {
                        continue;
                    }
                    Integer type = overrides.get(index);
                    if (type == null || type < 0 || type > 4) {
                        continue;
                    }
                    int x = index % com.osrs.client.world.MapLoader.WIDTH;
                    int y = index / com.osrs.client.world.MapLoader.WIDTH;
                    LinkedHashMap<String, Object> row = new LinkedHashMap<>();
                    row.put("x", x);
                    row.put("y", y);
                    row.put("type", TerrainVisualLoader.terrainTypeLabel(type));
                    serialized.add(row);
                }
            }

            terrainVisual.put("tile_overrides", serialized);
            root.put("terrain_visual", terrainVisual);

            String yaml = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            Path temp = scenePath.resolveSibling(scenePath.getFileName() + ".tmp");
            Files.writeString(temp, yaml, StandardCharsets.UTF_8);
            try {
                Files.move(temp, scenePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception atomicIgnored) {
                Files.move(temp, scenePath, StandardCopyOption.REPLACE_EXISTING);
            }
            return new SaveResult(true, "Saved terrain_visual.tile_overrides to " + scenePath.getFileName());
        } catch (Exception e) {
            return new SaveResult(false, "Terrain visual save failed: " + e.getMessage());
        }
    }
}
