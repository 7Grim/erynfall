package com.osrs.client.world;

import java.util.List;

/**
 * Defines the visual identity of a world region — terrain tint, lighting bias,
 * and preferred prop/foliage/rock sets for authoring density zones.
 *
 * Themes are declared in the {@code themes:} block of {@code scene.yaml} and
 * referenced by name on individual {@code terrain_visual} regions.
 *
 * terrain_tint: per-channel multiplier applied to tile base colors after
 *   corner blending. Values > 1 push warm; < 1 pull cool or darken.
 *
 * lighting_bias: additive offsets applied to the ambient light for this
 *   region. Lets a hellscape feel ember-lit while the rest of the world
 *   uses the global sky fill.
 *
 * clutter/foliage/rock tags: informational — used by the density-zone
 *   overlay and future procedural dressing to know which props fit here.
 *
 * ambient_layer: string key matching an AmbientLayer enum value; advisory
 *   input for audio zone authoring.
 */
public record WorldTheme(
    String id,
    float terrainTintR,
    float terrainTintG,
    float terrainTintB,
    float lightingBiasR,
    float lightingBiasG,
    float lightingBiasB,
    List<String> clutterTags,
    List<String> foliageTags,
    List<String> rockTags,
    String ambientLayer
) {
    /** A theme that changes nothing — tint multipliers are 1, biases are 0. */
    public static WorldTheme neutral(String id) {
        return new WorldTheme(id, 1f, 1f, 1f, 0f, 0f, 0f, List.of(), List.of(), List.of(), null);
    }

    public boolean hasTint() {
        return terrainTintR != 1f || terrainTintG != 1f || terrainTintB != 1f;
    }

    public boolean hasLightingBias() {
        return lightingBiasR != 0f || lightingBiasG != 0f || lightingBiasB != 0f;
    }
}
