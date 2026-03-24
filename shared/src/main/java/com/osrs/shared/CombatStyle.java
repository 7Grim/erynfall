package com.osrs.shared;

/**
 * OSRS melee attack styles.
 * Each style determines which skill receives combat XP and grants an invisible stat boost.
 *
 * Source: https://oldschool.runescape.wiki/w/Combat_Options
 */
public enum CombatStyle {

    /**
     * Accurate — +3 invisible Attack level bonus.
     * XP per damage: 4 Attack XP + 1.33 Hitpoints XP.
     */
    ACCURATE(0, "Accurate", "Attack XP"),

    /**
     * Aggressive — +3 invisible Strength level bonus.
     * XP per damage: 4 Strength XP + 1.33 Hitpoints XP.
     */
    AGGRESSIVE(1, "Aggressive", "Strength XP"),

    /**
     * Defensive — +3 invisible Defence level bonus.
     * XP per damage: 4 Defence XP + 1.33 Hitpoints XP.
     */
    DEFENSIVE(2, "Defensive", "Defence XP"),

    /**
     * Controlled — +1 invisible Attack, Strength, and Defence.
     * XP per damage: 1.33 Attack + 1.33 Strength + 1.33 Defence + 1.33 Hitpoints XP.
     */
    CONTROLLED(3, "Controlled", "Shared XP");

    public final int index;
    public final String displayName;
    public final String xpLabel;

    CombatStyle(int index, String displayName, String xpLabel) {
        this.index = index;
        this.displayName = displayName;
        this.xpLabel = xpLabel;
    }

    public static CombatStyle fromIndex(int index) {
        for (CombatStyle s : values()) {
            if (s.index == index) return s;
        }
        return AGGRESSIVE; // default
    }
}
