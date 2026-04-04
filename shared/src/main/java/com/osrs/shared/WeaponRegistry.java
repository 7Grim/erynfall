package com.osrs.shared;

import java.util.List;
import java.util.Map;

/**
 * Shared OSRS melee weapon progression data.
 *
 * Single source of truth for:
 * - All standard melee weapons and their attack level requirements
 * - Attack speed in OSRS ticks (1 tick = 0.6 s; 1 OSRS tick ≈ 154 server ticks at 256 Hz)
 * - Weapon type (slash / stab / crush) for the combat engine
 * - Strength bonus for the skill guide
 *
 * Source: https://oldschool.runescape.wiki/w/Weapon
 */
public final class WeaponRegistry {

    /**
     * 256 Hz server ticks per OSRS game tick (0.6 s × 256 = 153.6 ≈ 154).
     */
    public static final int OSRS_TICKS_TO_SERVER = 154;

    /**
     * Fallback attack speed used when the equipped item is not in the registry
     * (bare fists in OSRS = 4 ticks).
     */
    public static final int DEFAULT_ATTACK_SPEED_OSRS_TICKS = 4;

    /**
     * A single weapon entry.
     *
     * @param itemId               OSRS item ID
     * @param name                 Display name
     * @param attackReq            Minimum Attack level required to wield
     * @param weaponType           Primary attack type: "slash", "stab", or "crush"
     * @param attackSpeedOsrsTicks Attack interval in OSRS game ticks (4 = fast, 5 = average, 6 = slow)
     * @param strengthBonus        Melee strength bonus (contributes to max hit)
     */
    public record WeaponTier(
        int    itemId,
        String name,
        int    attackReq,
        String weaponType,
        int    attackSpeedOsrsTicks,
        int    strengthBonus
    ) {}

    // -----------------------------------------------------------------------
    // Scimitars — 4-tick, best DPS per tier in OSRS F2P
    // Source: https://oldschool.runescape.wiki/w/Scimitar
    // -----------------------------------------------------------------------
    private static final WeaponTier BRONZE_SCIMITAR  = new WeaponTier(1321,  "Bronze scimitar",   1, "slash", 4, 14);
    private static final WeaponTier IRON_SCIMITAR    = new WeaponTier(1323,  "Iron scimitar",     1, "slash", 4, 21);
    private static final WeaponTier STEEL_SCIMITAR   = new WeaponTier(1325,  "Steel scimitar",    5, "slash", 4, 28);
    private static final WeaponTier BLACK_SCIMITAR   = new WeaponTier(1327,  "Black scimitar",   10, "slash", 4, 35);
    private static final WeaponTier MITHRIL_SCIMITAR = new WeaponTier(1329,  "Mithril scimitar", 20, "slash", 4, 40);
    private static final WeaponTier ADAMANT_SCIMITAR = new WeaponTier(1331,  "Adamant scimitar", 30, "slash", 4, 46);
    private static final WeaponTier RUNE_SCIMITAR    = new WeaponTier(1333,  "Rune scimitar",    40, "slash", 4, 67);
    private static final WeaponTier DRAGON_SCIMITAR  = new WeaponTier(4587,  "Dragon scimitar",  60, "slash", 4, 66);

    // -----------------------------------------------------------------------
    // Longswords — 5-tick, higher strength bonus than scimitars
    // Source: https://oldschool.runescape.wiki/w/Longsword
    // -----------------------------------------------------------------------
    private static final WeaponTier BRONZE_LONGSWORD  = new WeaponTier(1291,  "Bronze longsword",   1, "slash", 5,  9);
    private static final WeaponTier IRON_LONGSWORD    = new WeaponTier(1293,  "Iron longsword",     1, "slash", 5, 14);
    private static final WeaponTier STEEL_LONGSWORD   = new WeaponTier(1295,  "Steel longsword",    5, "slash", 5, 23);
    private static final WeaponTier BLACK_LONGSWORD   = new WeaponTier(1297,  "Black longsword",   10, "slash", 5, 29);
    private static final WeaponTier MITHRIL_LONGSWORD = new WeaponTier(1299,  "Mithril longsword", 20, "slash", 5, 34);
    private static final WeaponTier ADAMANT_LONGSWORD = new WeaponTier(1301,  "Adamant longsword", 30, "slash", 5, 44);
    private static final WeaponTier RUNE_LONGSWORD    = new WeaponTier(1303,  "Rune longsword",    40, "slash", 5, 53);
    private static final WeaponTier DRAGON_LONGSWORD  = new WeaponTier(1305,  "Dragon longsword",  60, "slash", 5, 60);

    // -----------------------------------------------------------------------
    // Swords — 5-tick, lower bonuses than longswords but good stab attack
    // Source: https://oldschool.runescape.wiki/w/Sword
    // -----------------------------------------------------------------------
    private static final WeaponTier BRONZE_SWORD = new WeaponTier(1277, "Bronze sword", 1, "slash", 5, 6);

    // -----------------------------------------------------------------------
    // Combined list (ordered by attackReq then strength bonus descending)
    // Used by the skill guide for the Weapons tab
    // -----------------------------------------------------------------------
    private static final List<WeaponTier> WEAPONS = List.of(
        BRONZE_SCIMITAR,
        BRONZE_LONGSWORD,
        BRONZE_SWORD,
        IRON_SCIMITAR,
        IRON_LONGSWORD,
        STEEL_SCIMITAR,
        STEEL_LONGSWORD,
        BLACK_SCIMITAR,
        BLACK_LONGSWORD,
        MITHRIL_SCIMITAR,
        MITHRIL_LONGSWORD,
        ADAMANT_SCIMITAR,
        ADAMANT_LONGSWORD,
        RUNE_SCIMITAR,
        RUNE_LONGSWORD,
        DRAGON_SCIMITAR,
        DRAGON_LONGSWORD
    );

    // -----------------------------------------------------------------------
    // Scimitar-only list — used for "best weapon per tier" in the intro
    // -----------------------------------------------------------------------
    private static final List<WeaponTier> SCIMITARS = List.of(
        BRONZE_SCIMITAR,
        IRON_SCIMITAR,
        STEEL_SCIMITAR,
        BLACK_SCIMITAR,
        MITHRIL_SCIMITAR,
        ADAMANT_SCIMITAR,
        RUNE_SCIMITAR,
        DRAGON_SCIMITAR
    );

    /**
     * Speed lookup map: itemId → OSRS attack speed ticks.
     * Built once at class-init to keep GameLoop lookups O(1).
     */
    private static final Map<Integer, Integer> SPEED_BY_ID;
    static {
        Map<Integer, Integer> m = new java.util.HashMap<>();
        for (WeaponTier w : WEAPONS) {
            m.put(w.itemId(), w.attackSpeedOsrsTicks());
        }
        SPEED_BY_ID = Map.copyOf(m);
    }

    private WeaponRegistry() {}

    /** All melee weapon tiers, ordered by attack requirement then strength bonus. */
    public static List<WeaponTier> weapons() {
        return WEAPONS;
    }

    /** Scimitar tiers only, ordered by attack requirement. */
    public static List<WeaponTier> scimitars() {
        return SCIMITARS;
    }

    /**
     * Returns the OSRS attack speed (in game ticks) for the given item ID.
     * Returns {@link #DEFAULT_ATTACK_SPEED_OSRS_TICKS} for unknown items (fists).
     */
    public static int getAttackSpeedOsrsTicks(int itemId) {
        return SPEED_BY_ID.getOrDefault(itemId, DEFAULT_ATTACK_SPEED_OSRS_TICKS);
    }

    /**
     * Returns the attack interval in server ticks for the given weapon item ID.
     * Use this value directly in GameLoop combat tick-gate comparisons.
     */
    public static int getAttackSpeedServerTicks(int itemId) {
        return getAttackSpeedOsrsTicks(itemId) * OSRS_TICKS_TO_SERVER;
    }
}
