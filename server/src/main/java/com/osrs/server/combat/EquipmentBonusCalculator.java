package com.osrs.server.combat;

import com.osrs.shared.ItemDefinition;
import com.osrs.shared.Player;

import java.util.Map;

/**
 * Sums all worn equipment bonuses for a player.
 *
 * In OSRS, every equipped item contributes its bonuses additively to the
 * player's total bonuses used in combat calculations.
 *
 * Source: https://oldschool.runescape.wiki/w/Equipment_bonuses
 */
public class EquipmentBonusCalculator {

    /** Immutable snapshot of a player's total equipment bonuses. */
    public static class BonusSet {
        public final int stabAttack;
        public final int slashAttack;
        public final int crushAttack;
        public final int magicAttack;
        public final int rangedAttack;
        public final int stabDefence;
        public final int slashDefence;
        public final int crushDefence;
        public final int magicDefence;
        public final int rangedDefence;
        public final int meleeStrength;
        public final int rangedStrength;
        public final int magicDamage;
        public final int prayer;

        BonusSet(int stabAtk, int slashAtk, int crushAtk, int magicAtk, int rangedAtk,
                 int stabDef, int slashDef, int crushDef, int magicDef, int rangedDef,
                 int meleeStr, int rangedStr, int magicDmg, int prayer) {
            this.stabAttack    = stabAtk;
            this.slashAttack   = slashAtk;
            this.crushAttack   = crushAtk;
            this.magicAttack   = magicAtk;
            this.rangedAttack  = rangedAtk;
            this.stabDefence   = stabDef;
            this.slashDefence  = slashDef;
            this.crushDefence  = crushDef;
            this.magicDefence  = magicDef;
            this.rangedDefence = rangedDef;
            this.meleeStrength = meleeStr;
            this.rangedStrength = rangedStr;
            this.magicDamage   = magicDmg;
            this.prayer        = prayer;
        }

        /** Melee attack bonus for the equipped weapon's primary weapon type. */
        public int getMeleeAttackBonus(String weaponType) {
            return switch (weaponType) {
                case "stab"  -> stabAttack;
                case "crush" -> crushAttack;
                default      -> slashAttack;  // slash is most common
            };
        }

        /** Sum of all melee defence bonuses (used for simplified defence roll). */
        public int getTotalMeleeDefence() {
            return stabDefence + slashDefence + crushDefence;
        }
    }

    private static final BonusSet EMPTY = new BonusSet(
        0,0,0,0,0, 0,0,0,0,0, 0,0,0,0
    );

    /**
     * Calculates the total equipment bonuses for a player given the item definition map.
     */
    public static BonusSet calculate(Player player, Map<Integer, ItemDefinition> itemDefs) {
        int stabAtk = 0, slashAtk = 0, crushAtk = 0, magicAtk = 0, rangedAtk = 0;
        int stabDef = 0, slashDef = 0, crushDef = 0, magicDef = 0, rangedDef = 0;
        int meleeStr = 0, rangedStr = 0, magicDmg = 0, prayer = 0;

        for (int slot = 0; slot < 11; slot++) {
            int itemId = player.getEquipment(slot);
            if (itemId == 0) continue;
            ItemDefinition def = itemDefs.get(itemId);
            if (def == null) continue;

            stabAtk   += def.stabAttack;
            slashAtk  += def.slashAttack;
            crushAtk  += def.crushAttack;
            magicAtk  += def.magicAttack;
            rangedAtk += def.rangedAttack;
            stabDef   += def.stabDefence;
            slashDef  += def.slashDefence;
            crushDef  += def.crushDefence;
            magicDef  += def.magicDefence;
            rangedDef += def.rangedDefence;
            meleeStr  += def.meleeStrength;
            rangedStr += def.rangedStrength;
            magicDmg  += def.magicDamage;
            prayer    += def.prayer;
        }

        return new BonusSet(stabAtk, slashAtk, crushAtk, magicAtk, rangedAtk,
                            stabDef, slashDef, crushDef, magicDef, rangedDef,
                            meleeStr, rangedStr, magicDmg, prayer);
    }

    /** Returns an all-zero BonusSet (for NPCs or unequipped players). */
    public static BonusSet empty() {
        return EMPTY;
    }

    /**
     * Returns the weapon type string for the item currently in the weapon slot.
     * Defaults to "slash" if no weapon equipped.
     */
    public static String getWeaponType(Player player, Map<Integer, ItemDefinition> itemDefs) {
        int weaponId = player.getEquipment(4); // WEAPON slot = 4
        if (weaponId == 0) return "slash";
        ItemDefinition def = itemDefs.get(weaponId);
        return (def != null) ? def.weaponType : "slash";
    }
}
