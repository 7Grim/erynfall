package com.osrs.client.world;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class StaticPropLoader {

    private static final Logger LOG = LoggerFactory.getLogger(StaticPropLoader.class);

    private StaticPropLoader() {}

    public static final class StaticPropPlacement {
        public final String key;
        public final int x;
        public final int y;
        public final float rotationYDegrees;
        public final float scale;

        public StaticPropPlacement(String key, int x, int y, float rotationYDegrees, float scale) {
            this.key = key;
            this.x = x;
            this.y = y;
            this.rotationYDegrees = rotationYDegrees;
            this.scale = scale;
        }
    }

    public static List<StaticPropPlacement> load() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try (InputStream is = StaticPropLoader.class.getResourceAsStream("/static_props.yaml")) {
            if (is == null) {
                LOG.warn("static_props.yaml not found in classpath; no placed static props loaded");
                return List.of();
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = mapper.readValue(is, Map.class);
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
                if (scale <= 0f) {
                    scale = 1f;
                }

                placements.add(new StaticPropPlacement(key, x, y, rotationY, scale));
            }

            LOG.info("Loaded {} placed static props", placements.size());
            return Collections.unmodifiableList(placements);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load static_props.yaml", e);
        }
    }
}
