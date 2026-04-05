package com.osrs.shared;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared OSRS ranged weapon and ammunition data.
 *
 * Single source of truth for:
 * - All standard bows / crossbows and their Ranged requirements, attack range, and speed
 * - All arrow tiers and their ranged-strength bonuses
 * - Attack-speed lookup with Rapid-style adjustment (-1 OSRS tick)
 *
 * Attack-roll and max-hit formulas live in CombatEngine; rangedAttack/rangedStrength
 * bonuses live in ItemDefinition / items.yaml.  This registry is the authority on:
 *   1. Whether an item is a recognised ranged weapon
 *   2. The per-weapon attack speed (OSRS ticks), before combat-style modifiers
 *   3. Whether an item is recognised ammo (arrow/bolt)
 *
 * Sources:
 *   https://oldschool.runescape.wiki/w/Bow
 *   https://oldschool.runescape.wiki/w/Arrows
 *   https://oldschool.runescape.wiki/w/Attack_speed
 */
public final class RangedRegistry {

    /** Re-use WeaponRegistry's server-tick conversion constant. */
    public static final int OSRS_TICKS_TO_SERVER = WeaponRegistry.OSRS_TICKS_TO_SERVER;

    /**
     * Default ranged attack speed (OSRS ticks) when the equipped bow is not in
     * the registry — same as a shortbow (5 ticks = 3.0 s).
     */
    public static final int DEFAULT_ATTACK_SPEED_OSRS_TICKS = 5;

    // -----------------------------------------------------------------------
    // Bow record
    // -----------------------------------------------------------------------

    /**
     * @param itemId               OSRS item ID
     * @param name                 Display name
     * @param rangedReq            Minimum Ranged level to wield
     * @param attackRange          Maximum attack distance in tiles
     * @param attackSpeedOsrsTicks Base attack interval in OSRS game ticks
     *                             (Rapid style subtracts 1 tick; minimum 3)
     * @param rangedAttack         Equipment ranged-attack bonus
     */
    public record BowTier(
        int    itemId,
        String name,
        int    rangedReq,
        int    attackRange,
        int    attackSpeedOsrsTicks,
        int    rangedAttack
    ) {}

    // -----------------------------------------------------------------------
    // Arrow record
    // -----------------------------------------------------------------------

    /**
     * @param itemId          OSRS item ID (stackable ammo)
     * @param name            Display name
     * @param rangedStrength  Ranged-strength bonus (determines max hit with bows)
     */
    public record ArrowTier(
        int    itemId,
        String name,
        int    rangedStrength
    ) {}

    // -----------------------------------------------------------------------
    // Shortbows — 5-tick, range 7
    // Source: https://oldschool.runescape.wiki/w/Shortbow
    // -----------------------------------------------------------------------
    private static final BowTier SHORTBOW        = new BowTier(841,  "Shortbow",         1,  7, 5,  8);
    private static final BowTier OAK_SHORTBOW    = new BowTier(843,  "Oak shortbow",     5,  7, 5, 14);
    private static final BowTier WILLOW_SHORTBOW = new BowTier(849,  "Willow shortbow", 20,  7, 5, 19);
    private static final BowTier MAPLE_SHORTBOW  = new BowTier(853,  "Maple shortbow",  30,  7, 5, 29);
    private static final BowTier YEW_SHORTBOW    = new BowTier(857,  "Yew shortbow",    40,  7, 5, 47);
    private static final BowTier MAGIC_SHORTBOW  = new BowTier(861,  "Magic shortbow",  50,  7, 5, 75);

    // -----------------------------------------------------------------------
    // Longbows — 6-tick, range 10 (trade speed for range)
    // Source: https://oldschool.runescape.wiki/w/Longbow
    // -----------------------------------------------------------------------
    private static final BowTier LONGBOW        = new BowTier(839,  "Longbow",          1, 10, 6, 10);
    private static final BowTier OAK_LONGBOW    = new BowTier(845,  "Oak longbow",      5, 10, 6, 14);
    private static final BowTier WILLOW_LONGBOW = new BowTier(847,  "Willow longbow",  20, 10, 6, 20);
    private static final BowTier MAPLE_LONGBOW  = new BowTier(851,  "Maple longbow",   30, 10, 6, 31);
    private static final BowTier YEW_LONGBOW    = new BowTier(855,  "Yew longbow",     40, 10, 6, 50);
    private static final BowTier MAGIC_LONGBOW  = new BowTier(859,  "Magic longbow",   50, 10, 6, 77);

    // -----------------------------------------------------------------------
    // Arrow tiers — ranged strength is the dominant max-hit factor
    // Source: https://oldschool.runescape.wiki/w/Arrows
    // -----------------------------------------------------------------------
    private static final ArrowTier BRONZE_ARROW  = new ArrowTier(882, "Bronze arrow",   7);
    private static final ArrowTier IRON_ARROW    = new ArrowTier(884, "Iron arrow",    10);
    private static final ArrowTier STEEL_ARROW   = new ArrowTier(886, "Steel arrow",   16);
    private static final ArrowTier MITHRIL_ARROW = new ArrowTier(888, "Mithril arrow", 22);
    private static final ArrowTier ADAMANT_ARROW = new ArrowTier(890, "Adamant arrow", 31);
    private static final ArrowTier RUNE_ARROW    = new ArrowTier(892, "Rune arrow",    49);

    // -----------------------------------------------------------------------
    // Combined bow list (ordered by rangedReq)
    // -----------------------------------------------------------------------
    private static final List<BowTier> BOWS = List.of(
        SHORTBOW,
        LONGBOW,
        OAK_SHORTBOW,
        OAK_LONGBOW,
        WILLOW_SHORTBOW,
        WILLOW_LONGBOW,
        MAPLE_SHORTBOW,
        MAPLE_LONGBOW,
        YEW_SHORTBOW,
        YEW_LONGBOW,
        MAGIC_SHORTBOW,
        MAGIC_LONGBOW
    );

    /** Shortbows only, ordered by rangedReq — used for the skill guide Bows tab. */
    private static final List<BowTier> SHORTBOWS = List.of(
        SHORTBOW, OAK_SHORTBOW, WILLOW_SHORTBOW, MAPLE_SHORTBOW, YEW_SHORTBOW, MAGIC_SHORTBOW
    );

    /** All arrow tiers, ordered by rangedStrength. */
    private static final List<ArrowTier> ARROWS = List.of(
        BRONZE_ARROW, IRON_ARROW, STEEL_ARROW, MITHRIL_ARROW, ADAMANT_ARROW, RUNE_ARROW
    );

    // -----------------------------------------------------------------------
    // Speed lookup map: itemId → OSRS attack ticks (bows only)
    // -----------------------------------------------------------------------
    private static final Map<Integer, Integer> SPEED_BY_ID;
    static {
        Map<Integer, Integer> m = new HashMap<>();
        for (BowTier b : BOWS) m.put(b.itemId(), b.attackSpeedOsrsTicks());
        SPEED_BY_ID = Collections.unmodifiableMap(m);
    }

    // -----------------------------------------------------------------------
    // Bow itemId set — O(1) membership test
    // -----------------------------------------------------------------------
    private static final java.util.Set<Integer> BOW_IDS;
    static {
        java.util.Set<Integer> s = new java.util.HashSet<>();
        for (BowTier b : BOWS) s.add(b.itemId());
        BOW_IDS = Collections.unmodifiableSet(s);
    }

    // -----------------------------------------------------------------------
    // Arrow itemId set — O(1) membership test
    // -----------------------------------------------------------------------
    private static final java.util.Set<Integer> ARROW_IDS;
    static {
        java.util.Set<Integer> s = new java.util.HashSet<>();
        for (ArrowTier a : ARROWS) s.add(a.itemId());
        ARROW_IDS = Collections.unmodifiableSet(s);
    }

    private RangedRegistry() {}

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /** All bow tiers, ordered by Ranged requirement. */
    public static List<BowTier> bows() { return BOWS; }

    /** Shortbow tiers only (for skill guide). */
    public static List<BowTier> shortbows() { return SHORTBOWS; }

    /** All arrow tiers, ordered by ranged strength. */
    public static List<ArrowTier> arrows() { return ARROWS; }

    /** Returns true if the given item ID is a recognised bow or crossbow. */
    public static boolean isBow(int itemId) { return BOW_IDS.contains(itemId); }

    /** Returns true if the given item ID is a recognised arrow or bolt. */
    public static boolean isArrow(int itemId) { return ARROW_IDS.contains(itemId); }

    /**
     * Base attack speed in OSRS ticks for the given bow.
     * Returns {@link #DEFAULT_ATTACK_SPEED_OSRS_TICKS} for unknown bows.
     */
    public static int getAttackSpeedOsrsTicks(int itemId) {
        return SPEED_BY_ID.getOrDefault(itemId, DEFAULT_ATTACK_SPEED_OSRS_TICKS);
    }

    /**
     * Attack interval in server ticks for the given bow and combat style.
     *
     * Rapid (AGGRESSIVE) style subtracts 1 OSRS tick from the base speed
     * (minimum 3 ticks — matching OSRS mechanics).
     *
     * Use this value directly in GameLoop combat tick-gate comparisons.
     */
    public static int getAttackSpeedServerTicks(int itemId, CombatStyle style) {
        int osrsTicks = getAttackSpeedOsrsTicks(itemId);
        if (style == CombatStyle.AGGRESSIVE) {            // Rapid
            osrsTicks = Math.max(3, osrsTicks - 1);
        }
        return osrsTicks * OSRS_TICKS_TO_SERVER;
    }

    /**
     * Computes the ranged max hit for a given Ranged level and ranged-strength bonus,
     * without prayer or potion multipliers.
     *
     * Formula: floor(0.5 + effectiveRanged × (rangedStrength + 64) / 640)
     * where effectiveRanged = level + 8  (no style bonus — guide reference only).
     *
     * Source: https://oldschool.runescape.wiki/w/Maximum_ranged_hit
     */
    public static int maxHit(int rangedLevel, int rangedStrengthBonus) {
        int effectiveRanged = rangedLevel + 8;
        return Math.max(1, (int) Math.floor(0.5 + effectiveRanged * (rangedStrengthBonus + 64) / 640.0));
    }
}
