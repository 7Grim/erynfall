package com.osrs.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.Gdx;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * OSRS-accurate level-up congratulations popup.
 *
 * When a level up occurs, OSRS displays a small golden banner popup
 * near the bottom-center of the screen with:
 *   "Congratulations, you've just advanced your [Skill] level.
 *    You are now level N."
 *
 * The popup has a dark background with a golden border, a coloured skill
 * icon square on the left, and auto-dismisses after ~5 seconds.
 * Multiple level-ups queue and display one at a time.
 */
public class LevelUpOverlay {

    /** How long each popup stays visible (seconds). */
    private static final float DISPLAY_TIME = 5.0f;
    /** Fade-out begins this many seconds before expiry. */
    private static final float FADE_TIME    = 0.8f;

    private static final int BOX_W  = 340;
    private static final int BOX_H  = 60;
    private static final int ICON_SZ = 28;

    private static final String[] SKILL_NAMES = {
        "Attack", "Strength", "Defence", "Hitpoints", "Ranged", "Magic"
    };

    private static final Color[] SKILL_ICON_COLORS = {
        new Color(0.72f, 0.08f, 0.08f, 1f),  // Attack    — dark crimson
        new Color(0.18f, 0.48f, 0.11f, 1f),  // Strength  — dark green
        new Color(0.18f, 0.38f, 0.72f, 1f),  // Defence   — steel blue
        new Color(0.72f, 0.18f, 0.18f, 1f),  // Hitpoints — red
        new Color(0.12f, 0.52f, 0.18f, 1f),  // Ranged    — dark green
        new Color(0.28f, 0.20f, 0.75f, 1f),  // Magic     — indigo
    };

    private static class LevelEvent {
        final int skillIndex;
        final int newLevel;
        LevelEvent(int skillIndex, int newLevel) {
            this.skillIndex = skillIndex;
            this.newLevel   = newLevel;
        }
    }

    private final Deque<LevelEvent> queue = new ArrayDeque<>();
    private LevelEvent current = null;
    private float      timer   = 0f;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    public void addLevelUp(int skillIndex, int newLevel) {
        if (skillIndex < 0 || skillIndex >= SKILL_NAMES.length) return;
        queue.add(new LevelEvent(skillIndex, newLevel));
    }

    public void update(float delta) {
        if (current == null && !queue.isEmpty()) {
            current = queue.poll();
            timer   = DISPLAY_TIME;
        }
        if (current != null) {
            timer -= delta;
            if (timer <= 0f) current = null;
        }
    }

    /**
     * Render in screen space (Y=0 at bottom).
     * Positioned just above the chat box, horizontally centered.
     */
    public void render(ShapeRenderer sr, SpriteBatch batch, BitmapFont font,
                       int screenW, int screenH, Matrix4 proj) {
        if (current == null) return;

        float alpha = (timer < FADE_TIME) ? (timer / FADE_TIME) : 1f;

        // Position: centered horizontally, just above chat box
        int chatBoxH = ChatBox.TOTAL_H;
        int boxX = (screenW - BOX_W) / 2;
        int boxY = chatBoxH + 12;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // --- Background ---
        sr.setProjectionMatrix(proj);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.06f, 0.05f, 0.04f, 0.92f * alpha);
        sr.rect(boxX, boxY, BOX_W, BOX_H);
        sr.end();

        // --- Gold border (outer) ---
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(0.85f, 0.70f, 0.10f, alpha);
        sr.rect(boxX, boxY, BOX_W, BOX_H);
        sr.end();

        // --- Inner border (slightly inset) ---
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(0.55f, 0.44f, 0.06f, alpha * 0.7f);
        sr.rect(boxX + 2, boxY + 2, BOX_W - 4, BOX_H - 4);
        sr.end();

        // --- Skill icon (coloured square) ---
        int iconX = boxX + 10;
        int iconY = boxY + (BOX_H - ICON_SZ) / 2;
        Color ic = SKILL_ICON_COLORS[current.skillIndex];
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(ic.r, ic.g, ic.b, alpha);
        sr.rect(iconX, iconY, ICON_SZ, ICON_SZ);
        // Highlight top-left of icon (OSRS pixel art brightness)
        sr.setColor(
            Math.min(1f, ic.r + 0.3f),
            Math.min(1f, ic.g + 0.3f),
            Math.min(1f, ic.b + 0.3f),
            alpha * 0.6f);
        sr.rect(iconX, iconY + ICON_SZ - 4, ICON_SZ, 4);
        sr.rect(iconX, iconY, 4, ICON_SZ);
        sr.end();

        // --- Text ---
        int textX = iconX + ICON_SZ + 10;
        String skillName = SKILL_NAMES[current.skillIndex];

        batch.setProjectionMatrix(proj);
        batch.begin();

        // Line 1: "Congratulations, you just advanced a [Skill] level."  (exact OSRS wording)
        font.getData().setScale(0.82f);
        font.setColor(1f, 1f, 0f, alpha);  // yellow — matches OSRS game message color
        font.draw(batch,
            "Congratulations, you just advanced a " + skillName + " level.",
            textX, boxY + BOX_H - 10);

        // Line 2: "Your [Skill] level is now N."  (exact OSRS wording)
        font.setColor(Color.WHITE.r, Color.WHITE.g, Color.WHITE.b, alpha);
        font.draw(batch,
            "Your " + skillName + " level is now " + current.newLevel + ".",
            textX, boxY + BOX_H - 26);

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }
}
