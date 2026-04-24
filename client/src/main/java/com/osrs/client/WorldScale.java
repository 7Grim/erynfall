package com.osrs.client;

/**
 * Canonical world-geometry scale constants for the Erynfall 3D pipeline.
 *
 * Core identity: 1 gameplay tile == 1.0 world unit.
 *
 * All 3D model positions, distances, heights, and camera values use world units.
 * The server sends integer tile coordinates; the client maps them 1:1 to world
 * coordinates (no multiplier). See docs/SCALE_SPEC.md for full rationale.
 */
public final class WorldScale {

    private WorldScale() {}

    // -------------------------------------------------------------------------
    // Core identity

    /** One gameplay tile equals this many world units. The entire pipeline depends on this. */
    public static final float TILE_SIZE = 1.0f;

    // -------------------------------------------------------------------------
    // Actor / character

    /**
     * Canonical player and NPC height in world units.
     * All character models must be normalised to this height before export.
     * Matches the target in scripts/legacy/convert-glb-to-player.py
     * (normalise_to_180cm) and the lower bound in docs/GRAPHICS_STYLE.md.
     *
     * The building-shell generator (scripts/gen_osrs_buildings.py) was written
     * assuming 2.0 WU — a known discrepancy. Building wall proportions are
     * therefore 11% taller relative to the player than intended.
     */
    public static final float PLAYER_HEIGHT = 1.80f;

    // -------------------------------------------------------------------------
    // Terrain geometry

    /** Y coordinate of the ground plane. All terrain and actors sit at or above this. */
    public static final float GROUND_Y = 0.0f;

    /**
     * World-unit Y increment per terrain elevation level.
     * worldY = level * TERRAIN_HEIGHT_STEP_DEFAULT.
     * Can be overridden per-scene via terrain_height.height_step in scene.yaml.
     *
     * IMPORTANT: this value is 0.6 — the same number as OSRS_TICK_SECONDS.
     * They are unrelated concepts (world units vs. time). Do not conflate.
     */
    public static final float TERRAIN_HEIGHT_STEP_DEFAULT = 0.6f;

    /**
     * Height of procedural terrain walls generated from the tile map.
     * Intentionally shorter than PLAYER_HEIGHT (1.2 < 1.8) so players can
     * see over low walls — consistent with OSRS visibility rules.
     *
     * Building shell model walls are a separate system; their rendered height
     * depends on model geometry × placement scale and is NOT this constant.
     * See docs/SCALE_SPEC.md §Building Shells.
     */
    public static final float TERRAIN_WALL_HEIGHT = 1.2f;
}
