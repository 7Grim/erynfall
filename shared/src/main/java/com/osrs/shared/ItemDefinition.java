package com.osrs.shared;

/**
 * Full OSRS equipment bonus model.
 *
 * Attack bonuses: stab, slash, crush, magic, ranged
 * Defence bonuses: stab, slash, crush, magic, ranged
 * Other: meleeStrength, rangedStrength, magicDamage (%), prayer
 *
 * Source: https://oldschool.runescape.wiki/w/Equipment_bonuses
 */
public class ItemDefinition {
    public int     id;
    public String  name      = "Unknown item";
    public String  examine   = "";
    public boolean stackable;
    public boolean equipable;
    public int     equipSlot = -1;   // -1 = not equipable; maps to EquipmentSlot constants
    public boolean consumable;
    public int     consumeHeal;      // HP restored (food/potion)

    /** Tiles away the player can be when attacking with this weapon. 1 = melee. */
    public int attackRange = 1;

    /**
     * Primary weapon attack type determines which attack bonus to use in combat.
     * Values: "stab", "slash", "crush", "ranged", "magic"
     */
    public String weaponType = "slash";

    // -----------------------------------------------------------------------
    // Attack bonuses (positive = bonus, negative = penalty)
    // -----------------------------------------------------------------------
    public int stabAttack;
    public int slashAttack;
    public int crushAttack;
    public int magicAttack;
    public int rangedAttack;

    // -----------------------------------------------------------------------
    // Defence bonuses
    // -----------------------------------------------------------------------
    public int stabDefence;
    public int slashDefence;
    public int crushDefence;
    public int magicDefence;
    public int rangedDefence;

    // -----------------------------------------------------------------------
    // Other bonuses
    // -----------------------------------------------------------------------
    /** Melee strength bonus — contributes to max hit. */
    public int meleeStrength;
    /** Ranged strength bonus — contributes to ranged max hit (e.g. arrows). */
    public int rangedStrength;
    /** Magic damage bonus in whole-percentage points (e.g. 10 = +10%). */
    public int magicDamage;
    public int prayer;
    /** Minimum Defence level required to wear this item. */
    public int defenceReq = 1;
    /** Minimum Attack level required to wield this weapon. */
    public int attackReq  = 1;

    // -----------------------------------------------------------------------
    // Utility
    // -----------------------------------------------------------------------

    /** Bit flags for client display: 0x1=equippable 0x2=consumable 0x4=stackable */
    public int getFlags() {
        int f = 0;
        if (equipable)  f |= 0x1;
        if (consumable) f |= 0x2;
        if (stackable)  f |= 0x4;
        return f;
    }

    /**
     * Returns the relevant melee attack bonus based on weapon type.
     * CombatEngine uses this when calculating the player's attack roll.
     */
    public int getMeleeAttackBonus() {
        return switch (weaponType) {
            case "stab"  -> stabAttack;
            case "crush" -> crushAttack;
            default      -> slashAttack;  // "slash" is the default
        };
    }
}
