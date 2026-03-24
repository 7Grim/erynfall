package com.osrs.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * OSRS-style hitsplat rendering.
 *
 * Hitsplats appear above the target entity in world space, float upward
 * over ~1.5 seconds, then fade out — exactly matching OSRS combat feedback.
 *
 * Red splat with white number = damage dealt.
 * White splat with "0" = miss.
 */
public class CombatUI {

    private static final Logger LOG = LoggerFactory.getLogger(CombatUI.class);

    // Hitsplat appearance constants (OSRS-accurate)
    private static final float SPLAT_LIFETIME = 1.5f;   // seconds visible
    private static final float SPLAT_RADIUS = 9f;        // pixels
    private static final float FLOAT_SPEED = 25f;        // screen pixels per second upward
    private static final float FADE_START = 0.6f;        // start fading at 60% of life remaining

    public static class DamageNumber {
        public final float tileX;   // world tile X of the entity hit
        public final float tileY;   // world tile Y of the entity hit
        public final int damage;
        public final boolean hit;
        float yOffset = 0;          // screen-space upward offset (accumulated)
        float lifeRemaining;        // 1.0 → 0.0

        public DamageNumber(float tileX, float tileY, int damage, boolean hit) {
            this.tileX = tileX;
            this.tileY = tileY;
            this.damage = damage;
            this.hit = hit;
            this.lifeRemaining = 1.0f;
        }

        public boolean isExpired() { return lifeRemaining <= 0; }

        public float getAlpha() {
            return (lifeRemaining < FADE_START) ? lifeRemaining / FADE_START : 1.0f;
        }
    }

    private final List<DamageNumber> damageNumbers = new ArrayList<>();
    private final List<String> combatMessages = new ArrayList<>();
    private static final int MAX_MESSAGES = 5;

    /** Add a hitsplat above the given world tile. */
    public void addDamageNumber(float tileX, float tileY, int damage, boolean hit) {
        damageNumbers.add(new DamageNumber(tileX, tileY, damage, hit));
    }

    /** Add a line to the combat message log (bottom of screen). */
    public void addMessage(String message) {
        combatMessages.add(message);
        if (combatMessages.size() > MAX_MESSAGES) {
            combatMessages.remove(0);
        }
    }

    /** Call every frame with elapsed delta (seconds). */
    public void update(float delta) {
        Iterator<DamageNumber> it = damageNumbers.iterator();
        while (it.hasNext()) {
            DamageNumber dn = it.next();
            dn.lifeRemaining -= delta / SPLAT_LIFETIME;
            dn.yOffset += FLOAT_SPEED * delta;
            if (dn.isExpired()) it.remove();
        }
    }

    /**
     * Render all active hitsplats in WORLD SPACE (uses the game camera transform).
     * The isometric formula: screenX = (tileX - tileY) * 16, screenY = (tileX + tileY) * 8
     */
    public void render(ShapeRenderer shapeRenderer, SpriteBatch batch, BitmapFont font, OrthographicCamera camera) {
        if (damageNumbers.isEmpty()) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // --- Pass 1: filled circles (ShapeRenderer) ---
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (DamageNumber dn : damageNumbers) {
            float sx = (dn.tileX - dn.tileY) * 16f;
            float sy = (dn.tileX + dn.tileY) * 8f + 20 + dn.yOffset; // +20 above entity
            float alpha = dn.getAlpha();
            if (dn.hit) {
                shapeRenderer.setColor(0.75f, 0.0f, 0.0f, alpha); // red
            } else {
                shapeRenderer.setColor(0.85f, 0.85f, 0.85f, alpha); // light grey (miss)
            }
            shapeRenderer.circle(sx, sy, SPLAT_RADIUS, 12);
        }
        shapeRenderer.end();

        // --- Pass 2: number text (SpriteBatch) ---
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (DamageNumber dn : damageNumbers) {
            float sx = (dn.tileX - dn.tileY) * 16f;
            float sy = (dn.tileX + dn.tileY) * 8f + 20 + dn.yOffset;
            float alpha = dn.getAlpha();
            String text = String.valueOf(dn.damage);
            font.setColor(1f, 1f, 1f, alpha);
            // Centre the number on the circle
            font.draw(batch, text, sx - text.length() * 3f, sy + 5f);
        }
        batch.end();

        font.setColor(Color.WHITE);
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    /**
     * Render the combat message log in SCREEN SPACE (no camera).
     * Call with a screen-space SpriteBatch.
     */
    public void renderMessages(SpriteBatch screenBatch, BitmapFont font, int screenHeight) {
        if (combatMessages.isEmpty()) return;
        screenBatch.begin();
        font.setColor(Color.WHITE);
        int y = 80; // bottom-left area, above any bottom HUD
        for (int i = 0; i < combatMessages.size(); i++) {
            font.draw(screenBatch, combatMessages.get(i), 10, y + i * 15);
        }
        screenBatch.end();
        font.setColor(Color.WHITE);
    }

    public List<DamageNumber> getDamageNumbers() { return damageNumbers; }
    public List<String> getMessages() { return combatMessages; }
}
