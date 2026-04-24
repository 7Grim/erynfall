package com.osrs.client.world;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses the {@code themes:} block from a world {@code scene.yaml} into
 * a map of {@link WorldTheme} instances keyed by theme id.
 *
 * YAML structure expected under the {@code themes:} key:
 * <pre>
 * themes:
 *   frontier_settlement:
 *     terrain_tint: [1.05, 0.92, 0.80]    # R G B multipliers (default 1.0)
 *     lighting_bias: [0.03, 0.01, -0.02]  # R G B additive (default 0.0)
 *     clutter_tags: [crate_small, barrel_small]
 *     foliage_tags: [tree_oak]
 *     rock_tags: [rock_copper, rock_tin]
 *     ambient_layer: FOREST_BIRDS
 *
 *   hell_scarred_badlands:
 *     terrain_tint: [1.20, 0.65, 0.45]
 *     lighting_bias: [0.06, -0.03, -0.06]
 *     clutter_tags: [rock_copper]
 *     foliage_tags: [tree_dead]
 *     rock_tags: [rock_coal, rock_iron]
 * </pre>
 */
public final class WorldThemeLoader {

    private WorldThemeLoader() {}

    /**
     * Converts a raw parsed YAML map (the value of the {@code themes:} root key)
     * into typed {@link WorldTheme} instances.
     *
     * @param rawThemes the {@code Map<String,Object>} produced by SnakeYAML for
     *                  the {@code themes:} block, or {@code null} / empty map if absent
     * @return map from theme id → WorldTheme; empty map if input is null or empty
     */
    @SuppressWarnings("unchecked")
    public static Map<String, WorldTheme> load(Map<String, Object> rawThemes) {
        Map<String, WorldTheme> result = new HashMap<>();
        if (rawThemes == null || rawThemes.isEmpty()) {
            return result;
        }
        for (Map.Entry<String, Object> entry : rawThemes.entrySet()) {
            String id = entry.getKey();
            if (!(entry.getValue() instanceof Map<?, ?> rawDef)) {
                continue;
            }
            Map<String, Object> def = (Map<String, Object>) rawDef;

            float tintR = 1f, tintG = 1f, tintB = 1f;
            float biasR = 0f, biasG = 0f, biasB = 0f;

            if (def.get("terrain_tint") instanceof List<?> tint && tint.size() >= 3) {
                tintR = toFloat(tint.get(0), 1f);
                tintG = toFloat(tint.get(1), 1f);
                tintB = toFloat(tint.get(2), 1f);
            }
            if (def.get("lighting_bias") instanceof List<?> bias && bias.size() >= 3) {
                biasR = toFloat(bias.get(0), 0f);
                biasG = toFloat(bias.get(1), 0f);
                biasB = toFloat(bias.get(2), 0f);
            }

            List<String> clutterTags = toStringList(def.get("clutter_tags"));
            List<String> foliageTags = toStringList(def.get("foliage_tags"));
            List<String> rockTags    = toStringList(def.get("rock_tags"));
            String ambientLayer = def.get("ambient_layer") instanceof String s ? s : null;

            result.put(id, new WorldTheme(
                id,
                tintR, tintG, tintB,
                biasR, biasG, biasB,
                clutterTags, foliageTags, rockTags,
                ambientLayer
            ));
        }
        return result;
    }

    private static float toFloat(Object o, float defaultValue) {
        return o instanceof Number n ? n.floatValue() : defaultValue;
    }

    @SuppressWarnings("unchecked")
    private static List<String> toStringList(Object o) {
        if (!(o instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
            .filter(v -> v instanceof String)
            .map(v -> (String) v)
            .toList();
    }
}
