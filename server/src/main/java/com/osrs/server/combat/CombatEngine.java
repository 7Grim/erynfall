package com.osrs.server.combat;

import com.osrs.server.combat.EquipmentBonusCalculator.BonusSet;
import com.osrs.shared.CombatStyle;
import com.osrs.shared.ItemDefinition;
import com.osrs.shared.NPC;
import com.osrs.shared.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * OSRS-accurate combat engine.
 *
 * Implements the exact hit-chance and max-hit formulas from the OSRS wiki:
 *   https://oldschool.runescape.wiki/w/Combat_Options
 *   https://oldschool.runescape.wiki/w/Maximum_hit
 *   https://oldschool.runescape.wiki/w/Hit_chance
 *
 * Combat style invisible stat bonuses (applied before the formula):
 *   Accurate   → +3 invisible Attack level
 *   Aggressive → +3 invisible Strength level
 *   Defensive  → +3 invisible Defence level (defender side)
 *   Controlled → +1 Attack, +1 Strength, +1 Defence
 *
 * The +8 constant in all effective-level formulas represents standing (always present).
 *
 * XP awarding is NOT done here — GameLoop.awardCombatXp() handles it.
 */
public class CombatEngine {

    private static final Logger LOG = LoggerFactory.getLogger(CombatEngine.class);

    private final Map<Integer, ItemDefinition> itemDefs;

    public CombatEngine(Map<Integer, ItemDefinition> itemDefs) {
        this.itemDefs = itemDefs;
        LOG.info("Combat engine initialized (OSRS-accurate formulas)");
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Resolve a single attack tick.  Returns a HitResult with whether the
     * attack landed and how much damage was dealt.
     *
     * Deterministic per tick: same tickCounter + same attacker + same target = same roll.
     */
    public HitResult calculateHit(Player attacker, NPC target, long serverTick) {
        BonusSet bonuses = EquipmentBonusCalculator.calculate(attacker, itemDefs);
        String weaponType = EquipmentBonusCalculator.getWeaponType(attacker, itemDefs);

        int attackRoll  = playerAttackRoll(attacker, bonuses, weaponType);
        int defenceRoll = npcDefenceRoll(target);
        double hitChance = hitChance(attackRoll, defenceRoll);

        int maxHit = playerMaxHit(attacker, bonuses);

        return resolve(hitChance, maxHit, serverTick, attacker.getId(), target.getId());
    }

    public HitResult calculateHit(NPC attacker, Player target, long serverTick) {
        int attackRoll  = npcAttackRoll(attacker);
        int defenceRoll = playerDefenceRoll(target);
        double hitChance = hitChance(attackRoll, defenceRoll);

        int maxHit = attacker.getMaxHit();

        return resolve(hitChance, maxHit, serverTick, attacker.getId(), target.getId());
    }

    // -----------------------------------------------------------------------
    // Attack rolls
    // -----------------------------------------------------------------------

    /**
     * Player melee/ranged attack roll.
     *
     * effectiveLevel = floor(level × prayerMult) + styleBonus + 8
     * attackRoll     = effectiveLevel × (equipmentBonus + 64)
     */
    private int playerAttackRoll(Player p, BonusSet bonuses, String weaponType) {
        boolean isRanged = "ranged".equals(weaponType);

        int level = isRanged
            ? p.getSkillLevel(Player.SKILL_RANGED)
            : p.getAttackLevel();

        // Style bonuses (no prayer implemented yet → multiplier = 1.0)
        int styleBonus = switch (p.getCombatStyle()) {
            case ACCURATE   -> 3;   // +3 to attack/ranged
            case CONTROLLED -> 1;   // +1 to both
            default         -> 0;
        };

        int effectiveLevel = level + styleBonus + 8;

        int equipBonus = isRanged
            ? bonuses.rangedAttack
            : bonuses.getMeleeAttackBonus(weaponType);

        return effectiveLevel * (equipBonus + 64);
    }

    /**
     * NPC attack roll.
     * effectiveAttack = npcAttackLevel + 9  (the +9 is NPC's implicit stance bonus)
     * attackRoll      = effectiveAttack × (npcAttackBonus + 64)
     */
    private int npcAttackRoll(NPC npc) {
        int effectiveAttack = npc.getAttackLevel() + 9;
        return effectiveAttack * (npc.getAttackBonus() + 64);
    }

    // -----------------------------------------------------------------------
    // Defence rolls
    // -----------------------------------------------------------------------

    /**
     * Player defence roll.
     * effectiveDefence = floor(defenceLevel × prayerMult) + styleBonus + 8
     * defenceRoll      = effectiveDefence × (sumOfMeleeDefenceBonuses + 64)
     *
     * For simplicity, uses the sum of stab+slash+crush defence bonuses divided by 3
     * (average). This is a minor approximation; a full implementation would use
     * the attacker's weapon type to pick the matching defence bonus.
     */
    private int playerDefenceRoll(Player p) {
        int styleBonus = switch (p.getCombatStyle()) {
            case DEFENSIVE  -> 3;
            case CONTROLLED -> 1;
            default         -> 0;
        };
        int effectiveDefence = p.getDefenceLevel() + styleBonus + 8;

        // Sum all melee defence bonuses (conservative — OSRS picks based on attack type,
        // but we don't know the attacker's type here; using average is a fair proxy)
        BonusSet bonuses = EquipmentBonusCalculator.calculate(p, itemDefs);
        int defBonus = (bonuses.stabDefence + bonuses.slashDefence + bonuses.crushDefence) / 3;

        return effectiveDefence * (defBonus + 64);
    }

    /**
     * NPC defence roll.
     * effectiveDefence = npcDefenceLevel + 9
     * defenceRoll      = effectiveDefence × (npcDefenceBonus + 64)
     */
    private int npcDefenceRoll(NPC npc) {
        int effectiveDefence = npc.getDefenceLevel() + 9;
        return effectiveDefence * (npc.getDefenceBonus() + 64);
    }

    // -----------------------------------------------------------------------
    // Hit chance
    // -----------------------------------------------------------------------

    /**
     * OSRS hit-chance formula.
     *
     * If attackRoll > defenceRoll:  1 - (defRoll + 2) / (2 × (atkRoll + 1))
     * Else:                          atkRoll / (2 × (defRoll + 1))
     */
    private double hitChance(int attackRoll, int defenceRoll) {
        if (attackRoll > defenceRoll) {
            return 1.0 - (defenceRoll + 2.0) / (2.0 * (attackRoll + 1.0));
        } else {
            return (double) attackRoll / (2.0 * (defenceRoll + 1.0));
        }
    }

    // -----------------------------------------------------------------------
    // Max hit
    // -----------------------------------------------------------------------

    /**
     * Player max hit.
     *
     * effectiveStrength = floor(strLevel × prayerMult) + styleBonus + 8
     * maxHit = floor(0.5 + effectiveStrength × (meleeStrengthBonus + 64) / 640)
     *
     * Source: https://oldschool.runescape.wiki/w/Maximum_hit
     */
    private int playerMaxHit(Player p, BonusSet bonuses) {
        boolean isRanged = "ranged".equals(
            EquipmentBonusCalculator.getWeaponType(p, itemDefs));

        if (isRanged) {
            int rangedLevel = p.getSkillLevel(Player.SKILL_RANGED);
            // Rapid style: no bonus; Accurate: +3; Longrange: +0
            int styleBonus = (p.getCombatStyle() == CombatStyle.ACCURATE) ? 3 : 0;
            int effectiveRanged = rangedLevel + styleBonus + 8;
            int rangedStrBonus = bonuses.rangedStrength;
            return (int) Math.floor(0.5 + effectiveRanged * (rangedStrBonus + 64) / 640.0);
        }

        int strLevel = p.getStrengthLevel();
        int styleBonus = switch (p.getCombatStyle()) {
            case AGGRESSIVE -> 3;
            case CONTROLLED -> 1;
            default         -> 0;
        };
        int effectiveStr = strLevel + styleBonus + 8;
        int strBonus     = bonuses.meleeStrength;
        return Math.max(1, (int) Math.floor(0.5 + effectiveStr * (strBonus + 64) / 640.0));
    }

    // -----------------------------------------------------------------------
    // Resolution
    // -----------------------------------------------------------------------

    /**
     * Uses a deterministic LCG seeded by tick + attacker + defender IDs to
     * produce a pseudo-random roll. This ensures the same inputs always produce
     * the same result (useful for replay/testing), while varying between ticks.
     */
    private HitResult resolve(double hitChance, int maxHit, long serverTick,
                              int attackerId, int targetId) {
        // Knuth LCG (64-bit) → fold to [0, 10000) for 0.01% precision
        long seed = serverTick * 6364136223846793005L
                  + attackerId * 1442695040888963407L
                  + targetId;
        int roll = (int) (Math.abs(seed >>> 33) % 10000);

        int threshold = (int) (hitChance * 10000);
        if (roll >= threshold) {
            return new HitResult(false, 0);
        }

        // Damage uniformly distributed [0, maxHit]
        long dmgSeed = seed * 6364136223846793005L + 1L;
        int damage = (int) (Math.abs(dmgSeed >>> 33) % (maxHit + 1));
        return new HitResult(true, damage);
    }

    // -----------------------------------------------------------------------
    // Result
    // -----------------------------------------------------------------------

    public static class HitResult {
        public final boolean hit;
        public final int     damage;

        public HitResult(boolean hit, int damage) {
            this.hit    = hit;
            this.damage = damage;
        }
    }
}
