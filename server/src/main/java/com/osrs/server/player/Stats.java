package com.osrs.server.player;

import java.util.HashMap;
import java.util.Map;

/**
 * Player combat and skill statistics.
 * Tracks experience and levels for each skill.
 */
public class Stats {
    
    public enum Skill {
        ATTACK("attack"),
        STRENGTH("strength"),
        DEFENCE("defence"),
        HITPOINTS("hitpoints"),
        RANGED("ranged"),
        MAGIC("magic");
        
        public final String name;
        
        Skill(String name) {
            this.name = name;
        }
    }
    
    private Map<Skill, Long> experience = new HashMap<>();
    private Map<Skill, Integer> levels = new HashMap<>();
    
    private static final long[] XP_TABLE = generateXPTable();
    private static final int MAX_LEVEL = 99;
    
    public Stats() {
        // Initialize all skills at level 1 with 0 XP
        for (Skill skill : Skill.values()) {
            experience.put(skill, 0L);
            levels.put(skill, 1);
        }
    }
    
    /**
     * Add experience to a skill.
     * Automatically updates level if XP crosses threshold.
     */
    public void addExperience(Skill skill, long xp) {
        long currentXP = experience.getOrDefault(skill, 0L);
        long newXP = currentXP + xp;
        experience.put(skill, newXP);
        
        updateLevel(skill);
    }
    
    /**
     * Get current experience for a skill.
     */
    public long getExperience(Skill skill) {
        return experience.getOrDefault(skill, 0L);
    }
    
    /**
     * Get current level for a skill.
     */
    public int getLevel(Skill skill) {
        return levels.getOrDefault(skill, 1);
    }
    
    /**
     * Calculate level from XP.
     */
    private void updateLevel(Skill skill) {
        long xp = experience.get(skill);
        int level = 1;
        
        for (int i = 1; i <= MAX_LEVEL; i++) {
            if (xp >= XP_TABLE[i - 1]) {
                level = i;
            } else {
                break;
            }
        }
        
        levels.put(skill, level);
    }
    
    /**
     * Generate XP table using the exact OSRS formula.
     * table[level-1] = XP required to be that level.
     * Level 1 = 0 XP, Level 99 = 13,034,431 XP.
     * Level 92 is exactly 50% of level 99 XP (exponential curve).
     */
    private static long[] generateXPTable() {
        long[] table = new long[MAX_LEVEL];
        table[0] = 0; // Level 1 requires 0 XP

        long points = 0;
        for (int level = 1; level < MAX_LEVEL; level++) {
            // OSRS formula: floor(level + 300 * 2^(level/7))
            points += (long) Math.floor(level + 300.0 * Math.pow(2.0, level / 7.0));
            table[level] = (long) Math.floor(points / 4.0);
        }
        return table;
    }
}
