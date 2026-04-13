package com.osrs.client.art;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.osrs.client.world.StaticPropLoader;

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
}
