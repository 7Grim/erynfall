package com.osrs.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * OSRS-accurate XP drop display.
 *
 * OSRS drops appear on the right side of the screen and float upward.
 * Each drop shows a small skill icon (coloured square) and the gained XP
 * in yellow text. Multiple drops from the same combat action (e.g. Attack
 * XP + Hitpoints XP) appear as separate entries stacked vertically with
 * ROW_H pixel separation — they never overlap.
 *
 * OSRS verified details:
 *   - Text colour: yellow (#FFFF00)
 *   - Format: [icon] +N
 *   - Float direction: upward at ~28 px/sec
 *   - Duration: ~3 seconds
 *   - Vanilla: no fade (drops scroll off top); we add a short fade
 *   - Simultaneous drops: stacked vertically, each offset by ROW_H
 */
public class XpDropOverlay {

    private static final float LIFETIME    = 3.0f;
    private static final float FLOAT_SPEED = 28f;
    /** When life fraction falls below this, fade out. */
    private static final float FADE_START  = 0.20f;
    /** Vertical spacing between stacked drops (pixels). */
    private static final float ROW_H       = 16f;
    private static final int   ICON_SZ     = 12;
    private static final int   ICON_GAP    = 4;

    private static final String[] SKILL_NAMES = {
        "Attack", "Strength", "Defence", "Hitpoints", "Ranged", "Magic",
        "Prayer", "Woodcutting", "Fishing", "Cooking", "Mining", "Smithing", "Firemaking",
        "Crafting", "Runecrafting", "Fletching", "Agility", "Herblore", "Thieving",
        "Slayer", "Farming", "Hunter", "Construction"
    };

    private static final Color[] SKILL_ICON_COLORS = {
        new Color(0.72f, 0.08f, 0.08f, 1f),  // Attack    — dark crimson
        new Color(0.18f, 0.48f, 0.11f, 1f),  // Strength  — dark green
        new Color(0.18f, 0.38f, 0.72f, 1f),  // Defence   — steel blue
        new Color(0.72f, 0.18f, 0.18f, 1f),  // Hitpoints — red
        new Color(0.12f, 0.52f, 0.18f, 1f),  // Ranged    — dark green
        new Color(0.28f, 0.20f, 0.75f, 1f),  // Magic     — indigo
        new Color(0.85f, 0.80f, 0.20f, 1f),  // Prayer
        new Color(0.30f, 0.60f, 0.15f, 1f),  // Woodcutting
        new Color(0.10f, 0.55f, 0.80f, 1f),  // Fishing
        new Color(0.85f, 0.40f, 0.10f, 1f),  // Cooking
        new Color(0.60f, 0.60f, 0.62f, 1f),  // Mining
        new Color(0.70f, 0.50f, 0.20f, 1f),  // Smithing
        new Color(0.95f, 0.55f, 0.05f, 1f),  // Firemaking
        new Color(0.65f, 0.50f, 0.30f, 1f),  // Crafting
        new Color(0.20f, 0.75f, 0.75f, 1f),  // Runecrafting
        new Color(0.35f, 0.55f, 0.15f, 1f),  // Fletching
        new Color(0.25f, 0.60f, 0.85f, 1f),  // Agility
        new Color(0.15f, 0.75f, 0.25f, 1f),  // Herblore
        new Color(0.55f, 0.15f, 0.65f, 1f),  // Thieving
        new Color(0.70f, 0.10f, 0.10f, 1f),  // Slayer
        new Color(0.40f, 0.65f, 0.15f, 1f),  // Farming
        new Color(0.55f, 0.38f, 0.12f, 1f),  // Hunter
        new Color(0.78f, 0.65f, 0.35f, 1f),  // Construction
    };

    // -----------------------------------------------------------------------
    // Drop model
    // -----------------------------------------------------------------------

    private static class Drop {
        final int  skillIndex;
        final long xpGained;
        float yOffset;  // current upward displacement from anchor (can start negative)
        float life;     // 1.0 → 0.0

        Drop(int skillIndex, long xpGained, float startYOffset) {
            this.skillIndex = skillIndex;
            this.xpGained   = xpGained;
            this.yOffset    = startYOffset;
            this.life       = 1.0f;
        }

        float alpha() { return life < FADE_START ? life / FADE_START : 1f; }
        boolean expired() { return life <= 0f; }
    }

    private final List<Drop> drops = new ArrayList<>();

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Queue a new XP drop for the given skill (0–5) and amount.
     *
     * Simultaneous drops (e.g. Attack + Hitpoints from one hit) are staggered
     * vertically so they never overlap. Each new drop starts at or below the
     * current lowest drop, maintaining ROW_H separation.
     */
    public void addDrop(int skillIndex, long xpGained) {
        if (skillIndex < 0 || skillIndex >= SKILL_NAMES.length) return;

        // Find the lowest yOffset among all currently active drops.
        // New drop starts just below it so they all maintain vertical separation
        // as they float upward at the same speed.
        float startOffset = 0f;
        if (!drops.isEmpty()) {
            float lowestOffset = Float.MAX_VALUE;
            for (Drop d : drops) {
                if (d.yOffset < lowestOffset) lowestOffset = d.yOffset;
            }
            // Only stack below if there's a drop near the anchor area
            if (lowestOffset < ROW_H) {
                startOffset = lowestOffset - ROW_H;
            }
        }

        drops.add(new Drop(skillIndex, xpGained, startOffset));
    }

    /** Advance all drops one frame. Call once per frame before render(). */
    public void update(float delta) {
        Iterator<Drop> it = drops.iterator();
        while (it.hasNext()) {
            Drop d = it.next();
            d.life    -= delta / LIFETIME;
            d.yOffset += FLOAT_SPEED * delta;
            if (d.expired()) it.remove();
        }
    }

    /**
     * Render all active drops in screen space (Y=0 at bottom).
     *
     * Uses a single ShapeRenderer pass and a single SpriteBatch pass across
     * all drops to avoid the GL state disruption of per-drop begin/end.
     */
    public void render(ShapeRenderer sr, SpriteBatch batch, BitmapFont font,
                       int screenW, int screenH, Matrix4 proj,
                       int sidePanelX, int sidePanelTotalH) {
        if (drops.isEmpty()) return;

        // Anchor: just left of the side panel, above the HUD area
        float anchorX = sidePanelX - 84f;
        float anchorY = (float) sidePanelTotalH + 80f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // ── One ShapeRenderer pass for ALL skill icons ───────────────────────
        sr.setProjectionMatrix(proj);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (Drop d : drops) {
            float dropY = anchorY + d.yOffset;
            if (dropY + ROW_H < 0 || dropY > screenH) continue; // skip off-screen
            float alpha = d.alpha();
            Color ic = SKILL_ICON_COLORS[d.skillIndex];
            sr.setColor(ic.r, ic.g, ic.b, alpha);
            sr.rect(anchorX, dropY, ICON_SZ, ICON_SZ);
        }
        sr.end();

        // ── One SpriteBatch pass for ALL labels ──────────────────────────────
        batch.setProjectionMatrix(proj);
        batch.begin();
        font.getData().setScale(0.85f);
        for (Drop d : drops) {
            float dropY = anchorY + d.yOffset;
            if (dropY + ROW_H < 0 || dropY > screenH) continue; // skip off-screen
            float alpha = d.alpha();
            String label = "+" + d.xpGained;
            font.setColor(1f, 1f, 0f, alpha);  // OSRS yellow #FFFF00
            font.draw(batch, label, anchorX + ICON_SZ + ICON_GAP, dropY + ICON_SZ + 2f);
        }
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }
}
