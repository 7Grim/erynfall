package com.osrs.client;

/**
 * Canonical world-lighting constants for the Erynfall 3D renderer.
 *
 * All scene-level lighting values live here so they can be understood and
 * tuned in one place without touching renderer internals.
 *
 * Design intent:
 *   - One clear directional "sun" light with warm afternoon colour.
 *   - Cool ambient "sky fill" kept well below sun intensity so form reads.
 *   - Ambient : directional ratio ≈ 1 : 2.5 — directional dominates.
 *     (Old ratio was ≈ 1 : 0.9 — ambient nearly matched directional → flat.)
 *   - Sun angle is more horizontal than straight-down so walls and building
 *     sides are lit at visible contrast vs. top faces.
 *   - Terrain tile palette included here — in a stylized renderer tile colours
 *     are inseparable from lighting intent; they must be tuned together.
 *
 * Old values recorded inline for reference so diffs are self-documenting.
 */
public final class WorldLighting {

    private WorldLighting() {}

    // ── Key light (sun) ───────────────────────────────────────────────────────
    //
    // Warm, slightly de-saturated afternoon sun.
    // Intensity driven high enough to cast readable shadows on walls/buildings.
    //
    // Old: R=0.45  G=0.42  B=0.36   (cooler, lower intensity)

    /** Sun colour — red channel. */
    public static final float SUN_R = 0.78f;

    /** Sun colour — green channel. */
    public static final float SUN_G = 0.74f;

    /** Sun colour — blue channel. */
    public static final float SUN_B = 0.56f;

    // Direction (unit vector pointing FROM sun TO scene, i.e. light travels this way):
    //   X: left-of-centre — sun is slightly to the right of camera
    //   Y: -0.70 (was -0.85) — less vertical; better side-wall hit
    //   Z: forward — sun slightly in front, casting back-wall shadows
    //
    // Old: X=-0.50  Y=-0.85  Z=-0.20   (near-vertical, walls barely lit)

    /** Sun direction X (left of camera). */
    public static final float SUN_DIR_X = -0.55f;

    /** Sun direction Y (down). Shallower than old value; improves form on walls. */
    public static final float SUN_DIR_Y = -0.70f;

    /** Sun direction Z (forward angle). */
    public static final float SUN_DIR_Z = -0.40f;

    // ── Sky fill (ambient) ────────────────────────────────────────────────────
    //
    // Cool blue-grey sky bounce. Kept intentionally low so the directional
    // light has clear contrast for depth and form reading.
    //
    // Old: R=0.50  G=0.48  B=0.42   (warm, too bright — washed out directional)

    /** Ambient colour — red channel. */
    public static final float AMB_R = 0.28f;

    /** Ambient colour — green channel. */
    public static final float AMB_G = 0.30f;

    /** Ambient colour — blue channel. */
    public static final float AMB_B = 0.38f;

    // ── Terrain wall vertex shade ─────────────────────────────────────────────
    //
    // Multiplied into vertical terrain-wall face vertex colours before any
    // environmental lighting. Raised slightly from 0.55 → 0.62 so walls have
    // a brighter base that reads against ground tiles even on shadowed faces.
    //
    // Old: 0.55f

    /** Base brightness applied to all vertical terrain-wall mesh faces. */
    public static final float WALL_FACE_SHADE = 0.62f;

    // ── Tile base colours ─────────────────────────────────────────────────────
    //
    // Pre-lighting vertex colours for each tile type.
    // These are the starting point before per-corner blending and seeded noise.
    // Adjust these AND the sun/ambient together — they are co-dependent.
    //
    // With the new lower ambient the slightly richer values below compensate:
    // tiles stay vivid at the new lighting level.

    /** Grass tile — rich deeper OSRS green (was 0.32/0.56/0.18 — too washed). */
    public static final float TILE_GRASS_R = 0.24f;
    public static final float TILE_GRASS_G = 0.50f;
    public static final float TILE_GRASS_B = 0.14f;

    /** Water tile — deep ocean blue. */
    public static final float TILE_WATER_R = 0.10f;  // unchanged
    public static final float TILE_WATER_G = 0.27f;  // was 0.26
    public static final float TILE_WATER_B = 0.46f;  // was 0.44

    /** Path tile — warm packed earth (darker, more OSRS-earthy; was 0.48/0.40/0.24). */
    public static final float TILE_PATH_R = 0.44f;
    public static final float TILE_PATH_G = 0.36f;
    public static final float TILE_PATH_B = 0.20f;

    /** Wall/rock tile — stone grey. */
    public static final float TILE_WALL_R = 0.36f;  // was 0.34
    public static final float TILE_WALL_G = 0.34f;  // was 0.32
    public static final float TILE_WALL_B = 0.32f;  // was 0.30

    /** Sand tile — warm golden yellow. */
    public static final float TILE_SAND_R = 0.62f;  // was 0.60
    public static final float TILE_SAND_G = 0.54f;  // was 0.52
    public static final float TILE_SAND_B = 0.30f;  // was 0.28

    // ── Derived ratios (informational) ────────────────────────────────────────
    // Approximate sun luma  ≈ 0.73  (0.2126*SUN_R + 0.7152*SUN_G + 0.0722*SUN_B)
    // Approximate amb luma  ≈ 0.30  (same formula on AMB_*)
    // Ratio: 0.73 / 0.30  ≈  2.4 : 1  (directional strongly dominates)
    //
    // Old ratio: ~0.44 / ~0.47 ≈ 0.9 : 1  (near-flat)
}
