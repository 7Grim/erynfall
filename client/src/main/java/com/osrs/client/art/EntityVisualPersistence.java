package com.osrs.client.art;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EntityVisualPersistence {

    public record SaveResult(boolean success, String message) {}

    private EntityVisualPersistence() {}

    public static SaveResult saveModelBinding(Path visualsPath,
                                              int definitionId,
                                              String entityName,
                                              String modelKey3d,
                                              boolean animated3d) {
        if (visualsPath == null) {
            return new SaveResult(false, "Entity visuals path not configured");
        }
        if (!Files.exists(visualsPath)) {
            return new SaveResult(false, "Entity visuals file not found: " + visualsPath);
        }
        if (definitionId <= 0) {
            return new SaveResult(false, "Invalid definition_id: " + definitionId);
        }

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            @SuppressWarnings("unchecked")
            LinkedHashMap<String, Object> root = mapper.readValue(Files.readString(visualsPath), LinkedHashMap.class);

            List<Object> entityVisualsList = root.get("entity_visuals") instanceof List<?> list
                ? new ArrayList<>(list)
                : new ArrayList<>();

            LinkedHashMap<String, Object> target = null;
            int targetIndex = -1;
            for (int i = 0; i < entityVisualsList.size(); i++) {
                Object rowObj = entityVisualsList.get(i);
                if (!(rowObj instanceof Map<?, ?> rowMap)) {
                    continue;
                }
                Integer rowDefinitionId = asInt(rowMap.get("definition_id"));
                if (rowDefinitionId != null && rowDefinitionId == definitionId) {
                    @SuppressWarnings("unchecked")
                    LinkedHashMap<String, Object> row = new LinkedHashMap<>((Map<String, Object>) rowMap);
                    target = row;
                    targetIndex = i;
                    break;
                }
            }

            if (target == null) {
                target = new LinkedHashMap<>();
                target.put("definition_id", definitionId);
                String normalizedName = entityName == null ? "" : entityName.trim();
                if (!normalizedName.isBlank()) {
                    target.put("name", normalizedName);
                }
                entityVisualsList.add(target);
            }

            if ((target.get("name") == null || String.valueOf(target.get("name")).isBlank())
                && entityName != null
                && !entityName.trim().isBlank()) {
                target.put("name", entityName.trim());
            }

            String normalizedModelKey = modelKey3d == null ? "" : modelKey3d.trim();
            if (normalizedModelKey.isBlank()) {
                target.remove("model_key_3d");
            } else {
                target.put("model_key_3d", normalizedModelKey);
            }
            target.put("animated_3d", animated3d);

            if (targetIndex >= 0) {
                entityVisualsList.set(targetIndex, target);
            }

            root.put("entity_visuals", entityVisualsList);

            String yaml = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            Path temp = visualsPath.resolveSibling(visualsPath.getFileName() + ".tmp");
            Files.writeString(temp, yaml, StandardCharsets.UTF_8);
            try {
                Files.move(temp, visualsPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception atomicIgnored) {
                Files.move(temp, visualsPath, StandardCopyOption.REPLACE_EXISTING);
            }
            return new SaveResult(true, "Saved entity binding for definition_id=" + definitionId);
        } catch (Exception e) {
            return new SaveResult(false, "Entity visual save failed: " + e.getMessage());
        }
    }

    private static Integer asInt(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
