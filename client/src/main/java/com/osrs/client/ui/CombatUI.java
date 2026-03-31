package com.osrs.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
    private static final float STACK_SPACING = 12f;      // vertical spacing for overlapping hitsplats

    public enum HpBarState { HEALTHY, POISONED, VENOMED, DISEASED, NEAR_DEATH }

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
    private HpBarState hpBarState = HpBarState.HEALTHY;
    private String opponentName = null;
    private int opponentLevel = 0;
    private int opponentHp = 0;
    private int opponentMaxHp = 0;
    private boolean showOpponentInfo = false;

    private final Matrix4 screenProjection = new Matrix4();
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

    public void setHpBarState(HpBarState state) {
        this.hpBarState = state != null ? state : HpBarState.HEALTHY;
    }

    public void setOpponentInfo(String npcName, int npcLevel, int npcHp, int npcMaxHp) {
        this.opponentName = npcName;
        this.opponentLevel = Math.max(0, npcLevel);
        this.opponentHp = Math.max(0, npcHp);
        this.opponentMaxHp = Math.max(1, npcMaxHp);
        this.showOpponentInfo = npcName != null && !npcName.isEmpty();
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
        if (damageNumbers.isEmpty() && !showOpponentInfo) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        if (!damageNumbers.isEmpty()) {
            // --- Pass 1: filled circles (ShapeRenderer) ---
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            Map<Long, Integer> stacksByTile = new HashMap<>();
            for (DamageNumber dn : damageNumbers) {
                float sx = (dn.tileX - dn.tileY) * 16f;
                int stackIndex = stacksByTile.getOrDefault(tileKey(dn.tileX, dn.tileY), 0);
                stacksByTile.put(tileKey(dn.tileX, dn.tileY), stackIndex + 1);
                float sy = (dn.tileX + dn.tileY) * 8f + 20 + dn.yOffset + stackIndex * STACK_SPACING; // +20 above entity
                float alpha = dn.getAlpha();
                if (!dn.hit) {
                    shapeRenderer.setColor(0.85f, 0.85f, 0.85f, alpha); // light grey (miss)
                } else if (dn.damage == 0) {
                    shapeRenderer.setColor(0.40f, 0.60f, 1.0f, alpha); // blue (blocked/zero damage)
                } else {
                    shapeRenderer.setColor(0.75f, 0.0f, 0.0f, alpha); // red (hit)
                }
                shapeRenderer.circle(sx, sy, SPLAT_RADIUS, 12);
            }
            shapeRenderer.end();

            // --- Pass 2: number text (SpriteBatch) ---
            batch.setProjectionMatrix(camera.combined);
            batch.begin();
            stacksByTile.clear();
            for (DamageNumber dn : damageNumbers) {
                float sx = (dn.tileX - dn.tileY) * 16f;
                int stackIndex = stacksByTile.getOrDefault(tileKey(dn.tileX, dn.tileY), 0);
                stacksByTile.put(tileKey(dn.tileX, dn.tileY), stackIndex + 1);
                float sy = (dn.tileX + dn.tileY) * 8f + 20 + dn.yOffset + stackIndex * STACK_SPACING;
                float alpha = dn.getAlpha();
                String text = String.valueOf(dn.damage);
                font.setColor(1f, 1f, 1f, alpha);
                // Centre the number on the circle
                font.draw(batch, text, sx - text.length() * 3f, sy + 5f);
            }
            batch.end();
        }

        if (showOpponentInfo) {
            int screenW = Gdx.graphics.getWidth();
            int screenH = Gdx.graphics.getHeight();
            screenProjection.setToOrtho2D(0, 0, screenW, screenH);
            renderOpponentInfo(shapeRenderer, batch, font, screenW, screenH,
                opponentName, opponentLevel, opponentHp, opponentMaxHp);
        }

        font.setColor(Color.WHITE);
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private Color hpColorForState() {
        return switch (hpBarState) {
            case HEALTHY, NEAR_DEATH -> new Color(0.22f, 0.78f, 0.28f, 1f); // green
            case POISONED -> new Color(0.90f, 0.80f, 0.15f, 1f);             // yellow
            case VENOMED -> new Color(0.12f, 0.42f, 0.38f, 1f);              // dark green-blue
            case DISEASED -> new Color(0.90f, 0.80f, 0.15f, 1f);             // yellow
        };
    }

    private void renderHpBar(ShapeRenderer sr, float screenX, float screenY,
                             float hpPercent, Color barColor, Color textColor) {
        int barWidth = 100;
        int barHeight = 8;
        int barX = (int) screenX + 10;
        int barY = (int) screenY - 25;

        float clamped = Math.max(0f, Math.min(1f, hpPercent));
        float innerW = (barWidth - 2) * clamped;

        // Background fill
        sr.setColor(Color.BLACK);
        sr.rect(barX, barY, barWidth, barHeight);

        // HP fill
        sr.setColor(barColor);
        sr.rect(barX + 1, barY + 1, innerW, barHeight - 2);

        // Red overlay for near-death state
        if (hpBarState == HpBarState.NEAR_DEATH) {
            sr.setColor(new Color(1f, 0f, 0f, 0.5f));
            sr.rect(barX, barY, barWidth, barHeight);
        }
    }

    private void renderOpponentInfo(ShapeRenderer sr, SpriteBatch batch, BitmapFont font,
                                    int screenX, int screenY,
                                    String npcName, int npcLevel, int npcHp, int npcMaxHp) {
        int panelX = 10;
        int panelY = screenY - 200;
        int panelW = 170;
        int panelH = 42;

        float hpPercent = npcMaxHp > 0 ? (float) npcHp / npcMaxHp : 0f;
        Color hpColor = hpColorForState();

        sr.setProjectionMatrix(screenProjection);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.08f, 0.07f, 0.06f, 0.88f);
        sr.rect(panelX, panelY - panelH + 8, panelW, panelH);
        renderHpBar(sr, panelX + 10, panelY - 8, hpPercent, hpColor, Color.WHITE);
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(0.55f, 0.46f, 0.28f, 1f);
        sr.rect(panelX, panelY - panelH + 8, panelW, panelH);
        sr.end();

        batch.setProjectionMatrix(screenProjection);
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, npcName + " (Level " + npcLevel + ")", panelX + 10, panelY);
        font.setColor(0.88f, 0.82f, 0.72f, 1f);
        font.draw(batch, "HP: " + npcHp + "/" + npcMaxHp, panelX + 10, panelY - 16);
        batch.end();
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

    private long tileKey(float tileX, float tileY) {
        long x = (int) tileX;
        long y = (int) tileY;
        return (x << 32) | (y & 0xffffffffL);
    }
}
