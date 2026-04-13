package com.osrs.client.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.osrs.client.LaunchOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EntityVisualRegistry {

    private static final String VISUALS_RESOURCE_PATH = "world/entity_visuals.yaml";
    private static final String VISUALS_SOURCE_RELATIVE_PATH = "art/world/entity_visuals.yaml";

    public record EntityVisual(int definitionId,
                               String name,
                               String spriteKey2d,
                               String modelKey3d,
                               Boolean animated3d,
                               Boolean occludesWorld,
                               Float shadowWidth,
                               Float shadowHeight,
                               Float shadowAlpha,
                               Boolean actionAnimated) {}

    private final Map<Integer, List<EntityVisual>> byDefinitionId;

    private EntityVisualRegistry(Map<Integer, List<EntityVisual>> byDefinitionId) {
        this.byDefinitionId = byDefinitionId;
    }

    public static EntityVisualRegistry load() {
        return load(LaunchOptions.normal());
    }

    public static EntityVisualRegistry load(LaunchOptions launchOptions) {
        LaunchOptions options = launchOptions == null ? LaunchOptions.normal() : launchOptions;
        boolean repoBacked = options.artistMode();
        FileHandle source = repoBacked
            ? Gdx.files.absolute(options.repoRootPath().resolve(VISUALS_SOURCE_RELATIVE_PATH).toString())
            : Gdx.files.internal(VISUALS_RESOURCE_PATH);

        if (!source.exists()) {
            Gdx.app.log("EntityVisualRegistry", "WARN: visual binding file missing: " + source.path());
            return new EntityVisualRegistry(Map.of());
        }

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> root = mapper.readValue(source.readString(), Map.class);
            Object entriesRaw = root.get("entity_visuals");
            if (!(entriesRaw instanceof List<?> entries)) {
                Gdx.app.log("EntityVisualRegistry", "WARN: no entity_visuals list in " + source.path());
                return new EntityVisualRegistry(Map.of());
            }

            Map<Integer, List<EntityVisual>> byDefinitionId = new HashMap<>();
            for (Object raw : entries) {
                if (!(raw instanceof Map<?, ?> map)) {
                    continue;
                }
                Integer definitionId = asInt(map.get("definition_id"));
                if (definitionId == null || definitionId <= 0) {
                    continue;
                }
                EntityVisual visual = new EntityVisual(
                    definitionId,
                    asString(map.get("name")),
                    asString(map.get("sprite_key_2d")),
                    asString(map.get("model_key_3d")),
                    asBoolean(map.get("animated_3d")),
                    asBoolean(map.get("occludes_world")),
                    asFloat(map.get("shadow_width")),
                    asFloat(map.get("shadow_height")),
                    asFloat(map.get("shadow_alpha")),
                    asBoolean(map.get("action_animated"))
                );
                byDefinitionId.computeIfAbsent(definitionId, ignored -> new ArrayList<>()).add(visual);
            }

            return new EntityVisualRegistry(byDefinitionId);
        } catch (Exception e) {
            Gdx.app.log("EntityVisualRegistry", "WARN: failed to parse " + source.path() + ": " + e.getMessage());
            return new EntityVisualRegistry(Map.of());
        }
    }

    public EntityVisual resolve(int definitionId, String entityName) {
        if (definitionId <= 0) {
            return null;
        }
        List<EntityVisual> candidates = byDefinitionId.get(definitionId);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        String lookup = entityName == null ? "" : entityName.trim();
        if (!lookup.isEmpty()) {
            for (EntityVisual visual : candidates) {
                String name = visual.name();
                if (name != null && !name.isBlank() && name.equalsIgnoreCase(lookup)) {
                    return visual;
                }
            }
        }

        for (EntityVisual visual : candidates) {
            String name = visual.name();
            if (name == null || name.isBlank()) {
                return visual;
            }
        }

        return candidates.get(0);
    }

    private static String asString(Object value) {
        if (value == null) {
            return null;
        }
        String out = String.valueOf(value).trim();
        return out.isEmpty() ? null : out;
    }

    private static Integer asInt(Object value) {
        if (value instanceof Integer i) {
            return i;
        }
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

    private static Float asFloat(Object value) {
        if (value instanceof Float f) {
            return f;
        }
        if (value instanceof Number n) {
            return n.floatValue();
        }
        if (value instanceof String s) {
            try {
                return Float.parseFloat(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Boolean asBoolean(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String s) {
            if ("true".equalsIgnoreCase(s.trim())) {
                return true;
            }
            if ("false".equalsIgnoreCase(s.trim())) {
                return false;
            }
        }
        return null;
    }
}
