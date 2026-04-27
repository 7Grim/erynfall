package com.osrs.client;

/**
 * Canonical stylized colour palette for the Erynfall world.
 *
 * Two roles:
 *
 *   1. CONSTRAINT — max saturation, min/max value limits applied to every
 *      static-prop/building material at model-instance-creation time.
 *      Prevents neon prototype colours from slipping through GLB exports
 *      without a full art review pass.
 *
 *   2. REFERENCE — named colour constants used for programmatic materials
 *      (player skin fallback, future procedural clutter) and as tuning
 *      anchors when authoring GLB exports.
 *
 * Usage in the renderer:
 *   WorldPalette.applyPaletteConstraints(modelInstance) is called once
 *   per static-prop ModelInstance at creation time (not per frame).
 *   It runs an HSV clamp: saturation ≤ MAX_SATURATION, value in [MIN_VALUE, MAX_VALUE].
 *   Near-white materials (r,g,b ≥ 0.95) are skipped — they are texture
 *   multipliers that must stay white to avoid tinting sprite-sheet tiles.
 *
 * Tuning notes (last tuned alongside WorldLighting, see git log):
 *   MAX_SATURATION 0.72 → allows vivid colours while blocking neon.
 *     OSRS palette sits around 0.55–0.80; 0.72 is the reasonable cap.
 *   MIN_VALUE 0.18 → prevents near-black materials that look like voids.
 *   MAX_VALUE 0.88 → prevents blown-out whites that appear to glow.
 */
public final class WorldPalette {

    private WorldPalette() {}

    // ── Global material constraints ───────────────────────────────────────────

    /** Maximum HSV saturation allowed on any static-prop material diffuse colour. */
    public static final float MAX_SATURATION = 0.72f;

    /** Minimum HSV value (brightness) allowed on any static-prop material diffuse colour. */
    public static final float MIN_VALUE = 0.18f;

    /** Maximum HSV value (brightness) allowed on any static-prop material diffuse colour. */
    public static final float MAX_VALUE = 0.88f;

    // ── Reference palette ─────────────────────────────────────────────────────
    //
    // Use these when creating programmatic materials or checking GLB exports.
    // Each entry is within the constraint envelope above.

    /** Oak wood — warm brown for beams, docks, furniture frames. */
    public static final float WOOD_R = 0.55f;
    public static final float WOOD_G = 0.38f;
    public static final float WOOD_B = 0.22f;

    /** Stone masonry — cool grey-beige for building walls and stonework. */
    public static final float STONE_R = 0.46f;
    public static final float STONE_G = 0.44f;
    public static final float STONE_B = 0.40f;

    /** Thatch / straw — warm yellow-brown for roof surfaces. */
    public static final float THATCH_R = 0.58f;
    public static final float THATCH_G = 0.50f;
    public static final float THATCH_B = 0.28f;

    /** Foliage — muted OSRS-style green for canopy and bushes. */
    public static final float FOLIAGE_R = 0.28f;
    public static final float FOLIAGE_G = 0.50f;
    public static final float FOLIAGE_B = 0.18f;

    /** Iron / metal — dark grey for fittings, anvils, tools. */
    public static final float METAL_R = 0.38f;
    public static final float METAL_G = 0.36f;
    public static final float METAL_B = 0.34f;

    /** Default player skin tone — skin zone fallback and DEV_FALLBACK diffuse. */
    public static final float PLAYER_SKIN_R = 0.76f;
    public static final float PLAYER_SKIN_G = 0.52f;
    public static final float PLAYER_SKIN_B = 0.33f;

    /** Default player hair — dark brown. */
    public static final float PLAYER_HAIR_R = 0.28f;
    public static final float PLAYER_HAIR_G = 0.18f;
    public static final float PLAYER_HAIR_B = 0.10f;

    /** Default player shirt — muted green tunic. */
    public static final float PLAYER_SHIRT_R = 0.28f;
    public static final float PLAYER_SHIRT_G = 0.42f;
    public static final float PLAYER_SHIRT_B = 0.22f;

    /** Default player pants — dark grey. */
    public static final float PLAYER_PANTS_R = 0.25f;
    public static final float PLAYER_PANTS_G = 0.24f;
    public static final float PLAYER_PANTS_B = 0.22f;

    /** Default player boots — worn leather brown. */
    public static final float PLAYER_BOOTS_R = 0.38f;
    public static final float PLAYER_BOOTS_G = 0.26f;
    public static final float PLAYER_BOOTS_B = 0.14f;

    /** Default player gloves — same leather as boots. */
    public static final float PLAYER_GLOVES_R = 0.40f;
    public static final float PLAYER_GLOVES_G = 0.28f;
    public static final float PLAYER_GLOVES_B = 0.16f;

    // ── Terrain base colours ──────────────────────────────────────────────────
    //
    // Canonical RGB for each of the five terrain tile types (tile types 0–4).
    // These are the BASE colours before any WorldTheme terrain_tint multiplier
    // is applied.  All values sit within the WorldPalette constraint envelope
    // (saturation ≤ 0.72, value 0.18–0.88).
    //
    // See docs/TERRAIN_PALETTE_SPEC.md for the full spec and OSRS rationale.
    // Validation of scene.yaml terrain types: scripts/validate-scene.py

    /** Tile type 0 — grass. Muted lowland green, slight yellow-warm bias. */
    public static final float TERRAIN_GRASS_R = 0.42f;
    public static final float TERRAIN_GRASS_G = 0.52f;
    public static final float TERRAIN_GRASS_B = 0.24f;

    /** Tile type 1 — water. Cold coastal blue, shallow-inlet tone. */
    public static final float TERRAIN_WATER_R = 0.22f;
    public static final float TERRAIN_WATER_G = 0.44f;
    public static final float TERRAIN_WATER_B = 0.62f;

    /** Tile type 2 — path / dirt. Worn dusty brown, well-trodden earth. */
    public static final float TERRAIN_PATH_R  = 0.58f;
    public static final float TERRAIN_PATH_G  = 0.48f;
    public static final float TERRAIN_PATH_B  = 0.28f;

    /** Tile type 3 — wall / rock. Cool grey stone, cliff or rubble face. */
    public static final float TERRAIN_WALL_R  = 0.46f;
    public static final float TERRAIN_WALL_G  = 0.44f;
    public static final float TERRAIN_WALL_B  = 0.38f;

    /** Tile type 4 — sand. Warm pale beige, beach and desert ground. */
    public static final float TERRAIN_SAND_R  = 0.76f;
    public static final float TERRAIN_SAND_G  = 0.66f;
    public static final float TERRAIN_SAND_B  = 0.38f;
}
