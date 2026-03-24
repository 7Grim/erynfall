package com.osrs.server.combat;

import com.osrs.shared.Entity;
import com.osrs.shared.NPC;
import com.osrs.shared.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSRS combat engine.
 * All calculations are deterministic, seeded by server tick.
 */
public class CombatEngine {
    
    private static final Logger LOG = LoggerFactory.getLogger(CombatEngine.class);
    
    // Combat stats
    private static final int ATTACK_LEVEL = 10;
    private static final int STRENGTH_LEVEL = 10;
    private static final int DEFENCE_LEVEL = 10;
    
    // XP tables
    private static final long[] XP_TABLE = new long[100];
    
    static {
        // Initialize XP table (simplified: linear progression)
        for (int i = 0; i < 100; i++) {
            XP_TABLE[i] = i * 1000L; // 0 XP at level 1, 1000 per level
        }
    }
    
    public CombatEngine() {
        LOG.info("Combat engine initialized");
    }
    
    /**
     * Calculate hit/miss for an attack.
     * Deterministic: same tick + attacker + target = same result
     */
    public HitResult calculateHit(Entity attacker, Entity target, long serverTick) {
        // RNG seeded by server tick
        int seed = (int) (serverTick % 256);
        
        // Simplified hit calculation:
        // 50% base hit chance, modified by attack/defence
        int attackBonus = getAttackBonus(attacker);
        int defenceBonus = getDefenceBonus(target);
        
        int hitChance = 50 + (attackBonus / 2) - (defenceBonus / 2);
        hitChance = Math.max(5, Math.min(95, hitChance)); // Clamp to 5-95%
        
        boolean hit = (seed % 100) < hitChance;
        
        if (!hit) {
            return new HitResult(false, 0, 0);
        }
        
        // Calculate damage
        int maxDamage = getMaxDamage(attacker);
        int damage = seed % (maxDamage + 1);
        
        // Award XP: attacker gets Strength XP, defender gets Defence XP
        int xpAwarded = calculateXP(attacker, target, damage);
        
        // Apply XP to player
        if (attacker instanceof Player) {
            Player p = (Player) attacker;
            p.addStrengthXp(xpAwarded);
        }
        if (target instanceof Player) {
            Player p = (Player) target;
            p.addDefenceXp(xpAwarded / 2); // Defender gets half XP
        }
        
        return new HitResult(true, damage, xpAwarded);
    }
    
    /**
     * Attack bonus: player uses Attack skill level; NPCs use combat level as proxy.
     */
    private int getAttackBonus(Entity attacker) {
        if (attacker instanceof Player) {
            return ((Player) attacker).getAttackLevel();
        } else if (attacker instanceof NPC) {
            // NPC combat level ≈ attack/defence level (simplified OSRS formula)
            return Math.max(1, ((NPC) attacker).getDefinitionId());
        }
        return 1;
    }

    /**
     * Defence bonus: player uses Defence skill level; NPCs use combat level as proxy.
     */
    private int getDefenceBonus(Entity target) {
        if (target instanceof Player) {
            return ((Player) target).getDefenceLevel();
        } else if (target instanceof NPC) {
            return Math.max(1, ((NPC) target).getDefinitionId());
        }
        return 1;
    }

    /**
     * Max hit: players scale with Strength level + equipped weapon;
     * NPCs use their definition's max_hit from world.yml.
     */
    private int getMaxDamage(Entity attacker) {
        if (attacker instanceof Player) {
            // OSRS simplified: floor(0.5 + strengthLevel * (strengthBonus + 64) / 640)
            // For now: base 1 + 1 per 10 Strength levels above 1
            int str = ((Player) attacker).getStrengthLevel();
            return Math.max(1, 1 + (str - 1) / 5);
        } else if (attacker instanceof NPC) {
            return ((NPC) attacker).getMaxHit();
        }
        return 1;
    }
    
    /**
     * Calculate XP awarded for a hit.
     */
    private int calculateXP(Entity attacker, Entity target, int damage) {
        // Simplified: 4 XP per damage point to Strength
        // Defender gets Defence XP
        return damage * 4;
    }
    
    /**
     * Result of a single hit in combat.
     */
    public static class HitResult {
        public boolean hit;
        public int damage;
        public int xpAwarded;
        
        public HitResult(boolean hit, int damage, int xpAwarded) {
            this.hit = hit;
            this.damage = damage;
            this.xpAwarded = xpAwarded;
        }
    }
}
