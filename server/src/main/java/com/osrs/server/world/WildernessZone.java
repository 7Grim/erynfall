package com.osrs.server.world;

/**
 * Wilderness PvP zone logic.
 *
 * The wilderness region is defined in world.yml as "wilderness_region".
 * Depth increases with distance from the southern border.
 * Combat level range = wilderness depth level (minimum 1).
 */
public final class WildernessZone {

    public static final String REGION_ID = "wilderness_region";

    /** Tiles per wilderness level. Each 4 tiles north = +1 level. */
    private static final int TILES_PER_LEVEL = 4;

    private WildernessZone() {}

    /** True if the coordinate is inside the named wilderness map region. */
    public static boolean isInWilderness(World world, int x, int y) {
        return world.isPointInMapRegion(REGION_ID, x, y);
    }

    /**
     * Returns the wilderness level (1–max) based on Y position within the zone.
     * Level 1 at the south border, increases north.
     * Returns 0 if the coordinate is not in the wilderness.
     */
    public static int wildernessLevel(World world, int x, int y) {
        if (!isInWilderness(world, x, y)) return 0;
        WorldData.MapInfo mapInfo = world.getMapInfo(REGION_ID);
        if (mapInfo == null) return 1;
        int depth = y - mapInfo.minY;
        return Math.max(1, depth / TILES_PER_LEVEL + 1);
    }

    /**
     * Returns true if two players can attack each other based on combat level difference.
     * In OSRS: attackable if |levelA - levelB| ≤ wildernessLevel.
     */
    public static boolean canAttack(int attackerCombatLevel, int defenderCombatLevel, int wildernessLevel) {
        return Math.abs(attackerCombatLevel - defenderCombatLevel) <= wildernessLevel;
    }
}
