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

    /** Default player skin tone — used as the player_base fallback diffuse. */
    public static final float PLAYER_SKIN_R = 0.76f;
    public static final float PLAYER_SKIN_G = 0.52f;
    public static final float PLAYER_SKIN_B = 0.33f;
}
