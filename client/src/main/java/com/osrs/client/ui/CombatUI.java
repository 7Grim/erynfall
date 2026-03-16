package com.osrs.client.ui;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * UI for combat feedback: damage numbers, hit messages, XP drops.
 */
public class CombatUI {
    
    private static final Logger LOG = LoggerFactory.getLogger(CombatUI.class);
    
    public static class DamageNumber {
        public int x, y;
        public int damage;
        public int ticksRemaining;
        public boolean hit;
        
        public DamageNumber(int x, int y, int damage, boolean hit) {
            this.x = x;
            this.y = y;
            this.damage = damage;
            this.hit = hit;
            this.ticksRemaining = 60; // Display for 60 frames (~1 second at 60 FPS)
        }
        
        public boolean isExpired() {
            return ticksRemaining <= 0;
        }
    }
    
    private final List<DamageNumber> damageNumbers = new ArrayList<>();
    private final List<String> combatMessages = new ArrayList<>();
    private static final int MAX_MESSAGES = 5;
    
    /**
     * Add a damage number that floats above a position.
     */
    public void addDamageNumber(int worldX, int worldY, int damage, boolean hit) {
        DamageNumber dn = new DamageNumber(worldX, worldY, damage, hit);
        damageNumbers.add(dn);
        LOG.debug("Added damage number: {} {} at ({}, {})", 
            damage, (hit ? "HIT" : "MISS"), worldX, worldY);
    }
    
    /**
     * Add a combat message to the log.
     */
    public void addMessage(String message) {
        combatMessages.add(message);
        if (combatMessages.size() > MAX_MESSAGES) {
            combatMessages.remove(0);
        }
        LOG.debug("Combat message: {}", message);
    }
    
    /**
     * Update (remove expired damage numbers, etc).
     */
    public void update() {
        damageNumbers.removeIf(DamageNumber::isExpired);
        
        for (DamageNumber dn : damageNumbers) {
            dn.ticksRemaining--;
            dn.y++; // Float upward
        }
    }
    
    /**
     * Render damage numbers (placeholder).
     */
    public void render(ShapeRenderer renderer) {
        // TODO: Render floating damage numbers
        // For now, just update
    }
    
    /**
     * Render combat message log (placeholder).
     */
    public void renderMessages() {
        // TODO: Render message log at bottom left of screen
    }
    
    public List<DamageNumber> getDamageNumbers() {
        return damageNumbers;
    }
    
    public List<String> getMessages() {
        return combatMessages;
    }
}
