package com.osrs.server.combat;

import com.osrs.shared.CombatStyle;
import com.osrs.shared.Entity;
import com.osrs.shared.NPC;
import com.osrs.shared.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSRS-accurate combat engine.
 *
 * Accuracy and max-hit scale with combat style invisible bonuses:
 *   Accurate   → +3 invisible Attack  (better accuracy)
 *   Aggressive → +3 invisible Strength (higher max hit)
 *   Defensive  → +3 invisible Defence  (no damage/accuracy benefit for attacker)
 *   Controlled → +1 Attack, +1 Strength, +1 Defence
 *
 * XP awarding is NOT done here — GameLoop.awardCombatXp() handles it so
 * the correct XP per combat style can be applied after the hit is resolved.
 *
 * Source: https://oldschool.runescape.wiki/w/Combat_Options
 */
public class CombatEngine {

    private static final Logger LOG = LoggerFactory.getLogger(CombatEngine.class);

    public CombatEngine() {
        LOG.info("Combat engine initialized");
    }

    /**
     * Resolve a single melee attack.
     * Returns a HitResult containing whether the attack landed and how much
     * damage was dealt.  XP is awarded by the caller.
     *
     * Deterministic per tick: same tick + same attacker + same target = same result.
     */
    public HitResult calculateHit(Entity attacker, Entity target, long serverTick) {
        // Simple LCG seeded by tick so every tick yields a different roll
        int seed = (int) ((serverTick * 6364136223846793005L + 1442695040888963407L) >>> 32);
        seed = Math.abs(seed) % 100;

        int attackBonus  = getAttackBonus(attacker);
        int defenceBonus = getDefenceBonus(target);

        // Hit-chance clamped to 5–95 % so combat is never fully trivial or fully guaranteed
        int hitChance = 50 + (attackBonus / 2) - (defenceBonus / 2);
        hitChance = Math.max(5, Math.min(95, hitChance));

        if (seed >= hitChance) {
            return new HitResult(false, 0);
        }

        int maxDmg = getMaxDamage(attacker);
        int damage = 1 + (seed % Math.max(1, maxDmg));

        return new HitResult(true, damage);
    }

    // -----------------------------------------------------------------------
    // Attack bonus — includes invisible style boost
    // -----------------------------------------------------------------------

    private int getAttackBonus(Entity attacker) {
        if (attacker instanceof Player p) {
            int base = p.getAttackLevel();
            return switch (p.getCombatStyle()) {
                case ACCURATE   -> base + 3;
                case CONTROLLED -> base + 1;
                default         -> base;
            };
        }
        if (attacker instanceof NPC npc) {
            return Math.max(1, npc.getDefinitionId()); // combat level ≈ attack
        }
        return 1;
    }

    // -----------------------------------------------------------------------
    // Defence bonus
    // -----------------------------------------------------------------------

    private int getDefenceBonus(Entity target) {
        if (target instanceof Player p) {
            int base = p.getDefenceLevel();
            return switch (p.getCombatStyle()) {
                case DEFENSIVE  -> base + 3;
                case CONTROLLED -> base + 1;
                default         -> base;
            };
        }
        if (target instanceof NPC npc) {
            return Math.max(1, npc.getDefinitionId());
        }
        return 1;
    }

    // -----------------------------------------------------------------------
    // Max hit — includes invisible Strength boost
    // -----------------------------------------------------------------------

    private int getMaxDamage(Entity attacker) {
        if (attacker instanceof Player p) {
            int str = p.getStrengthLevel();
            // Apply Aggressive/Controlled strength bonus
            int effectiveStr = switch (p.getCombatStyle()) {
                case AGGRESSIVE -> str + 3;
                case CONTROLLED -> str + 1;
                default         -> str;
            };
            // Simplified OSRS formula: 1 + floor(effectiveStr / 5)
            return Math.max(1, 1 + effectiveStr / 5);
        }
        if (attacker instanceof NPC npc) {
            return Math.max(1, npc.getMaxHit());
        }
        return 1;
    }

    // -----------------------------------------------------------------------
    // Result
    // -----------------------------------------------------------------------

    public static class HitResult {
        public final boolean hit;
        public final int damage;

        public HitResult(boolean hit, int damage) {
            this.hit    = hit;
            this.damage = damage;
        }
    }
}
