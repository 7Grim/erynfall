package com.osrs.client.world;

import com.badlogic.gdx.Gdx;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class StaticPropLoader {

    private StaticPropLoader() {}

    public static final class StaticPropPlacement {
        public final String key;
        public final int x;
        public final int y;
        public final float rotationYDegrees;
        public final float scale;
        public final String visibility_group;
        public final String visibilityGroup;

        public StaticPropPlacement(String key,
                                   int x,
                                   int y,
                                   float rotationYDegrees,
                                   float scale,
                                   String visibilityGroup) {
            this.key = key;
            this.x = x;
            this.y = y;
            this.rotationYDegrees = rotationYDegrees;
            this.scale = scale;
            String group = visibilityGroup == null ? "base" : visibilityGroup.trim().toLowerCase();
            this.visibilityGroup = group.isBlank() ? "base" : group;
            this.visibility_group = this.visibilityGroup;
        }
    }

    public static List<StaticPropPlacement> load() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            if (!Gdx.files.internal("static_props.yaml").exists()) {
                Gdx.app.log("StaticPropLoader", "WARN: static_props.yaml not found in classpath");
                return List.of();
            }

            String yaml = Gdx.files.internal("static_props.yaml").readString();
            @SuppressWarnings("unchecked")
            Map<String, Object> data = mapper.readValue(yaml, Map.class);
            Object propsObj = data.get("props");
            if (!(propsObj instanceof List<?> rows)) {
                return List.of();
            }

            List<StaticPropPlacement> placements = new ArrayList<>(rows.size());
            for (Object row : rows) {
                if (!(row instanceof Map<?, ?> item)) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> prop = (Map<String, Object>) item;

                String key = String.valueOf(prop.getOrDefault("key", "")).trim();
                if (key.isBlank()) {
                    continue;
                }

                int x = prop.get("x") instanceof Number n ? n.intValue() : 0;
                int y = prop.get("y") instanceof Number n ? n.intValue() : 0;
                float rotationY = prop.get("rotation_y_degrees") instanceof Number n ? n.floatValue() : 0f;
                float scale = prop.get("scale") instanceof Number n ? n.floatValue() : 1f;
                String visibilityGroup = String.valueOf(prop.getOrDefault("visibility_group", "base"));
                if (scale <= 0f) {
                    scale = 1f;
                }

                placements.add(new StaticPropPlacement(key, x, y, rotationY, scale, visibilityGroup));
            }

            Gdx.app.log("StaticPropLoader", "Loaded " + placements.size() + " placed static props");
            return Collections.unmodifiableList(placements);
        } catch (Exception e) {
            Gdx.app.log("StaticPropLoader", "WARN: failed to load static_props.yaml: " + e.getMessage());
            return List.of();
        }
    }
}
