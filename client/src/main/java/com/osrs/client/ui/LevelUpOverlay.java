package com.osrs.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.Gdx;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * OSRS-accurate level-up congratulations overlay.
 *
 * Matches the in-game banner behaviour exactly:
 *   Phase 1 — BANNER: shows "Congratulations / Your level is now N /
 *                      Click here to continue". Does NOT auto-dismiss —
 *                      waits for player click or Enter key.
 *   Phase 2 — UNLOCKS: shows what the new level unlocked. Auto-dismisses
 *                       after 8 s (with fade) or on any click within the box.
 *
 * Multiple level-ups queue and play sequentially — only one banner visible
 * at a time so nothing gets lost.
 *
 * Supports all 23 skills.
 */
public class LevelUpOverlay {

    // -----------------------------------------------------------------------
    // Layout constants
    // -----------------------------------------------------------------------
    private static final int BOX_W  = 420;
    private static final int BOX_H  = 82;
    private static final int ICON_SZ = 30;
    /** Vertical gap from the top of the chat box. */
    private static final int ABOVE_CHAT_GAP = 14;

    /** Duration of Phase 2 (unlocks screen) before auto-dismiss. */
    private static final float UNLOCK_DISPLAY_TIME = 8.0f;
    /** Fade-out starts this many seconds before Phase 2 expires. */
    private static final float FADE_TIME = 1.0f;

    // -----------------------------------------------------------------------
    // All 23 skill names (Player.SKILL_* order)
    // -----------------------------------------------------------------------
    private static final String[] SKILL_NAMES = {
        "Attack",       "Strength",     "Defence",      "Hitpoints",
        "Ranged",       "Magic",        "Prayer",       "Woodcutting",
        "Fishing",      "Cooking",      "Mining",       "Smithing",
        "Firemaking",   "Crafting",     "Runecrafting", "Fletching",
        "Agility",      "Herblore",     "Thieving",     "Slayer",
        "Farming",      "Hunter",       "Construction"
    };

    // -----------------------------------------------------------------------
    // Per-skill icon colours — matches SidePanel.SKILL_COLORS
    // -----------------------------------------------------------------------
    private static final float[][] ICON_COLORS = {
        {0.72f, 0.18f, 0.10f},  // Attack       – dark red
        {0.52f, 0.38f, 0.26f},  // Strength     – brown-grey
        {0.22f, 0.42f, 0.72f},  // Defence      – steel blue
        {0.85f, 0.15f, 0.15f},  // Hitpoints    – bright red
        {0.40f, 0.58f, 0.20f},  // Ranged       – olive green
        {0.30f, 0.25f, 0.78f},  // Magic        – deep blue
        {0.90f, 0.85f, 0.55f},  // Prayer       – pale gold
        {0.55f, 0.32f, 0.12f},  // Woodcutting  – brown
        {0.20f, 0.48f, 0.78f},  // Fishing      – mid blue
        {0.82f, 0.38f, 0.12f},  // Cooking      – flame orange
        {0.52f, 0.52f, 0.55f},  // Mining       – stone grey
        {0.55f, 0.52f, 0.50f},  // Smithing     – grey
        {0.92f, 0.52f, 0.08f},  // Firemaking   – bright orange
        {0.75f, 0.68f, 0.28f},  // Crafting     – gold
        {0.58f, 0.35f, 0.72f},  // Runecrafting – purple
        {0.62f, 0.55f, 0.28f},  // Fletching    – olive
        {0.35f, 0.58f, 0.78f},  // Agility      – sky blue
        {0.25f, 0.65f, 0.30f},  // Herblore     – herb green
        {0.48f, 0.38f, 0.52f},  // Thieving     – grey-purple
        {0.78f, 0.14f, 0.14f},  // Slayer       – dark red
        {0.30f, 0.62f, 0.22f},  // Farming      – leaf green
        {0.78f, 0.62f, 0.28f},  // Hunter       – tan
        {0.80f, 0.68f, 0.38f},  // Construction – sandstone
    };

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------
    private enum Phase { BANNER, UNLOCKS }

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
    private Phase      phase   = Phase.BANNER;
    private float      unlockTimer = 0f;

    /** Last rendered bounding box — used for click hit-testing. */
    private int lastBoxX, lastBoxY;

    private final GlyphLayout glyphLayout = new GlyphLayout();

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    public void addLevelUp(int skillIndex, int newLevel) {
        if (skillIndex < 0 || skillIndex >= SKILL_NAMES.length) return;
        queue.add(new LevelEvent(skillIndex, newLevel));
    }

    public boolean isActive() {
        return current != null;
    }

    public void update(float delta) {
        // Advance queue only when no current item
        if (current == null && !queue.isEmpty()) {
            current = queue.poll();
            phase   = Phase.BANNER;
            unlockTimer = 0f;
        }

        if (current != null && phase == Phase.UNLOCKS) {
            unlockTimer -= delta;
            if (unlockTimer <= 0f) {
                // Auto-dismiss; pick up next queued item if any
                current = queue.isEmpty() ? null : queue.poll();
                phase   = Phase.BANNER;
                unlockTimer = 0f;
            }
        }
        // Phase.BANNER never auto-dismisses — waits for handleClick/handleEnter
    }

    /**
     * Call when the player clicks anywhere.
     * @return true if the click was consumed by the overlay (caller should stop propagation)
     */
    public boolean handleClick(int screenX, int screenY) {
        if (current == null) return false;
        // Only consume clicks inside the banner box
        if (screenX < lastBoxX || screenX >= lastBoxX + BOX_W) return false;
        if (screenY < lastBoxY || screenY >= lastBoxY + BOX_H) return false;

        advancePhase();
        return true;
    }

    /**
     * Call when the player presses Enter.
     * @return true if the key was consumed by the overlay.
     */
    public boolean handleEnter() {
        if (current == null) return false;
        advancePhase();
        return true;
    }

    /**
     * Render in screen space (Y=0 at bottom), positioned just above the chat box.
     */
    public void render(ShapeRenderer sr, SpriteBatch batch, BitmapFont font,
                       int screenW, int screenH, Matrix4 proj) {
        if (current == null) return;

        float alpha = 1f;
        if (phase == Phase.UNLOCKS) {
            alpha = (unlockTimer < FADE_TIME) ? Math.max(0f, unlockTimer / FADE_TIME) : 1f;
        }

        // Position: horizontally centred, just above chat box
        int boxX = (screenW - BOX_W) / 2;
        int boxY = ChatBox.TOTAL_H + ABOVE_CHAT_GAP;
        lastBoxX = boxX;
        lastBoxY = boxY;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        sr.setProjectionMatrix(proj);

        // Background — dark parchment-brown (matches OSRS level-up banner)
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.12f, 0.09f, 0.05f, 0.95f * alpha);
        sr.rect(boxX, boxY, BOX_W, BOX_H);
        sr.end();

        // Outer gold border
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(0.85f, 0.70f, 0.10f, alpha);
        sr.rect(boxX, boxY, BOX_W, BOX_H);
        sr.end();

        // Inner border (slightly inset, darker gold)
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(0.55f, 0.44f, 0.06f, alpha * 0.7f);
        sr.rect(boxX + 2, boxY + 2, BOX_W - 4, BOX_H - 4);
        sr.end();

        // Skill icon (coloured square with highlight)
        float[] ic = ICON_COLORS[current.skillIndex];
        int iconX = boxX + 12;
        int iconY = boxY + (BOX_H - ICON_SZ) / 2;
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(ic[0], ic[1], ic[2], alpha);
        sr.rect(iconX, iconY, ICON_SZ, ICON_SZ);
        // Top-left highlight edge (OSRS pixel-art brightness)
        sr.setColor(Math.min(1f, ic[0] + 0.35f),
                    Math.min(1f, ic[1] + 0.35f),
                    Math.min(1f, ic[2] + 0.35f), alpha * 0.55f);
        sr.rect(iconX, iconY + ICON_SZ - 4, ICON_SZ, 4);
        sr.rect(iconX, iconY, 4, ICON_SZ);
        sr.end();

        // Text rendering
        batch.setProjectionMatrix(proj);
        batch.begin();

        String skillName = SKILL_NAMES[current.skillIndex];
        int textX = iconX + ICON_SZ + 12;

        if (phase == Phase.BANNER) {
            // Line 1: yellow "Congratulations, you just advanced a [Skill] level."
            font.getData().setScale(0.82f);
            font.setColor(1f, 0.92f, 0.10f, alpha);
            font.draw(batch,
                "Congratulations, you just advanced a " + skillName + " level.",
                textX, boxY + BOX_H - 12);

            // Line 2: white "Your [Skill] level is now N."
            font.setColor(1f, 1f, 1f, alpha);
            font.draw(batch,
                "Your " + skillName + " level is now " + current.newLevel + ".",
                textX, boxY + BOX_H - 30);

            // Line 3: cyan "Click here to continue" — centred across entire box
            font.getData().setScale(0.78f);
            font.setColor(0.50f, 0.88f, 1.0f, alpha);
            glyphLayout.setText(font, "Click here to continue");
            float continueX = boxX + (BOX_W - glyphLayout.width) / 2f;
            font.draw(batch, "Click here to continue", continueX, boxY + 18);

        } else {
            // Phase UNLOCKS — show what the level unlocked
            String unlockText = LevelUnlockRegistry.getUnlockText(current.skillIndex, current.newLevel);
            String nextText   = LevelUnlockRegistry.getNextMilestoneText(current.skillIndex, current.newLevel);

            // Header: "Level [N] [Skill] unlocked:"
            font.getData().setScale(0.78f);
            font.setColor(1f, 0.92f, 0.10f, alpha);
            font.draw(batch, skillName + " level " + current.newLevel + " unlocked:", textX, boxY + BOX_H - 12);

            font.getData().setScale(0.74f);
            if (unlockText != null) {
                font.setColor(0.90f, 1f, 0.90f, alpha);
                font.draw(batch, unlockText, textX, boxY + BOX_H - 29);
            } else {
                font.setColor(0.80f, 0.80f, 0.80f, alpha);
                font.draw(batch, "Keep training " + skillName + " to unlock new content.", textX, boxY + BOX_H - 29);
            }

            if (nextText != null) {
                font.getData().setScale(0.68f);
                font.setColor(0.70f, 0.70f, 0.70f, alpha);
                font.draw(batch, nextText, textX, boxY + BOX_H - 46);
            }

            // "Click to dismiss" at the bottom — cyan, centred
            font.getData().setScale(0.72f);
            font.setColor(0.50f, 0.88f, 1.0f, alpha * 0.8f);
            glyphLayout.setText(font, "Click to dismiss");
            float dismissX = boxX + (BOX_W - glyphLayout.width) / 2f;
            font.draw(batch, "Click to dismiss", dismissX, boxY + 14);
        }

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void advancePhase() {
        if (phase == Phase.BANNER) {
            phase       = Phase.UNLOCKS;
            unlockTimer = UNLOCK_DISPLAY_TIME;
        } else {
            // Dismiss current; pick up next queued item
            current = queue.isEmpty() ? null : queue.poll();
            phase   = Phase.BANNER;
            unlockTimer = 0f;
        }
    }
}
