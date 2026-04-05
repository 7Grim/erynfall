package com.osrs.shared;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Standard Spellbook combat spells.
 *
 * Single source of truth for:
 * - All Standard Spellbook combat spells, their rune costs, XP, and level requirements
 * - Elemental staff auto-rune provision
 * - Attack speed and range for magic combat
 *
 * Sources:
 *   https://oldschool.runescape.wiki/w/Wind_Strike
 *   https://oldschool.runescape.wiki/w/Magic/Spells#Standard_spellbook
 */
public final class SpellRegistry {

    // Rune item IDs (OSRS-canonical)
    public static final int RUNE_AIR   = 556;
    public static final int RUNE_WATER = 555;
    public static final int RUNE_EARTH = 557;
    public static final int RUNE_FIRE  = 554;
    public static final int RUNE_MIND  = 558;
    public static final int RUNE_CHAOS = 562;

    // Elemental staff item IDs — each auto-provides one rune type per cast
    public static final int STAFF_AIR   = 1381;
    public static final int STAFF_WATER = 1383;
    public static final int STAFF_EARTH = 1385;
    public static final int STAFF_FIRE  = 1387;

    /** Element type — determines the client-side projectile colour. */
    public enum Element { WIND, WATER, EARTH, FIRE }

    /**
     * Maps a spell element to the {@code projectile_type} field value used in
     * the {@code CombatHit} proto message.
     * Matches the comment in network.proto: 3=spell_wind, 4=spell_water,
     * 5=spell_fire, 6=spell_earth.
     */
    public static int projectileType(Element element) {
        return switch (element) {
            case WIND  -> 3;
            case WATER -> 4;
            case FIRE  -> 5;
            case EARTH -> 6;
        };
    }

    /**
     * Returns the rune ID that the given staff automatically provides (unlimited),
     * or -1 if the item is not a recognised elemental staff.
     */
    public static int staffAutoRuneId(int staffItemId) {
        return switch (staffItemId) {
            case STAFF_AIR   -> RUNE_AIR;
            case STAFF_WATER -> RUNE_WATER;
            case STAFF_EARTH -> RUNE_EARTH;
            case STAFF_FIRE  -> RUNE_FIRE;
            default          -> -1;
        };
    }

    /** Base attack speed for all Standard Spellbook combat spells (5 OSRS ticks = 3.0 s). */
    public static final int ATTACK_SPEED_OSRS_TICKS  = 5;
    /** Converted to server ticks using WeaponRegistry's OSRS → server conversion factor. */
    public static final int ATTACK_SPEED_SERVER_TICKS = ATTACK_SPEED_OSRS_TICKS * WeaponRegistry.OSRS_TICKS_TO_SERVER;
    /** Attack range in tiles for all Standard Spellbook combat spells. */
    public static final int ATTACK_RANGE = 7;

    /** A single rune cost entry: item ID and quantity consumed per cast. */
    public record RuneCost(int runeId, int quantity) {}

    /**
     * Immutable spell definition.
     *
     * @param id            Unique spell ID (used in the SetSpell proto and Player.selectedSpellId)
     * @param name          Display name shown in the spellbook tab
     * @param element       Element type (wind/water/earth/fire)
     * @param magicLevel    Minimum Magic level required to cast
     * @param baseMaxHit    Maximum hit before magic-damage-bonus equipment scaling
     * @param castXpTenths  Magic XP awarded per cast regardless of hit/miss, stored as tenths
     *                      (e.g. 55 = 5.5 XP per cast).  Source: OSRS wiki spell XP column.
     * @param runes         Rune costs consumed on each cast (staff auto-rune skips matched IDs)
     */
    public record Spell(
        int id,
        String name,
        Element element,
        int magicLevel,
        int baseMaxHit,
        long castXpTenths,
        List<RuneCost> runes
    ) {}

    // -----------------------------------------------------------------------
    // Strike tier
    // Source: https://oldschool.runescape.wiki/w/Wind_Strike (and siblings)
    // -----------------------------------------------------------------------
    private static final Spell WIND_STRIKE  = new Spell(1, "Wind Strike",  Element.WIND,  1,  2,  55L,
        List.of(new RuneCost(RUNE_AIR, 1), new RuneCost(RUNE_MIND, 1)));
    private static final Spell WATER_STRIKE = new Spell(2, "Water Strike", Element.WATER, 5,  4,  75L,
        List.of(new RuneCost(RUNE_AIR, 1), new RuneCost(RUNE_MIND, 1), new RuneCost(RUNE_WATER, 1)));
    private static final Spell EARTH_STRIKE = new Spell(3, "Earth Strike", Element.EARTH, 9,  6,  95L,
        List.of(new RuneCost(RUNE_AIR, 2), new RuneCost(RUNE_MIND, 1), new RuneCost(RUNE_EARTH, 1)));
    private static final Spell FIRE_STRIKE  = new Spell(4, "Fire Strike",  Element.FIRE,  13, 8,  115L,
        List.of(new RuneCost(RUNE_AIR, 3), new RuneCost(RUNE_MIND, 1), new RuneCost(RUNE_FIRE, 1)));

    // -----------------------------------------------------------------------
    // Bolt tier
    // Source: https://oldschool.runescape.wiki/w/Wind_Bolt (and siblings)
    // -----------------------------------------------------------------------
    private static final Spell WIND_BOLT  = new Spell(5, "Wind Bolt",  Element.WIND,  17, 9,  135L,
        List.of(new RuneCost(RUNE_AIR, 2), new RuneCost(RUNE_CHAOS, 1)));
    private static final Spell WATER_BOLT = new Spell(6, "Water Bolt", Element.WATER, 23, 10, 165L,
        List.of(new RuneCost(RUNE_AIR, 2), new RuneCost(RUNE_CHAOS, 1), new RuneCost(RUNE_WATER, 2)));
    private static final Spell EARTH_BOLT = new Spell(7, "Earth Bolt", Element.EARTH, 29, 11, 195L,
        List.of(new RuneCost(RUNE_AIR, 3), new RuneCost(RUNE_CHAOS, 1), new RuneCost(RUNE_EARTH, 3)));
    private static final Spell FIRE_BOLT  = new Spell(8, "Fire Bolt",  Element.FIRE,  35, 12, 225L,
        List.of(new RuneCost(RUNE_AIR, 4), new RuneCost(RUNE_CHAOS, 1), new RuneCost(RUNE_FIRE, 3)));

    private static final List<Spell> ALL_SPELLS = List.of(
        WIND_STRIKE, WATER_STRIKE, EARTH_STRIKE, FIRE_STRIKE,
        WIND_BOLT,   WATER_BOLT,   EARTH_BOLT,   FIRE_BOLT
    );

    private static final Map<Integer, Spell> BY_ID;
    static {
        Map<Integer, Spell> m = new HashMap<>();
        for (Spell s : ALL_SPELLS) m.put(s.id(), s);
        BY_ID = Collections.unmodifiableMap(m);
    }

    private SpellRegistry() {}

    /** All combat spells in Standard Spellbook order (Strike tier then Bolt tier). */
    public static List<Spell> allSpells() { return ALL_SPELLS; }

    /** Returns the spell for the given ID, or {@code null} if not recognised. */
    public static Spell getById(int id) { return BY_ID.get(id); }
}
