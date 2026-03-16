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
     * Generate XP table (levels 1-99).
     * Simplified: each level requires 1000 * level XP.
     */
    private static long[] generateXPTable() {
        long[] table = new long[MAX_LEVEL];
        long totalXP = 0;
        
        for (int i = 1; i <= MAX_LEVEL; i++) {
            totalXP += i * 1000L;
            table[i - 1] = totalXP;
        }
        
        return table;
    }
}
