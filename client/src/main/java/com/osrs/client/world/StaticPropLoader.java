package com.osrs.client.world;

import com.badlogic.gdx.Gdx;
import com.osrs.client.LaunchOptions;

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
        return load(LaunchOptions.normal());
    }

    public static List<StaticPropPlacement> load(LaunchOptions launchOptions) {
        try {
            WorldSceneLoader.WorldSceneData sceneData = WorldSceneLoader.load(launchOptions);
            List<Map<String, Object>> rows = sceneData.staticProps();
            if (rows.isEmpty()) {
                Gdx.app.log("StaticPropLoader", "WARN: no static props found in scene file: " + sceneData.sourcePath());
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

            String source = sceneData.repoBacked() ? "repo" : "classpath";
            Gdx.app.log("StaticPropLoader", "Loaded " + placements.size() + " placed static props from " + source + " scene");
            return Collections.unmodifiableList(placements);
        } catch (Exception e) {
            Gdx.app.log("StaticPropLoader", "WARN: failed to load static props from scene data: " + e.getMessage());
            return List.of();
        }
    }
}
