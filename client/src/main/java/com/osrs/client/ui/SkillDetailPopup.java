package com.osrs.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.Gdx;
import com.osrs.shared.XpTable;

/**
 * Floating popup shown when the player clicks a skill cell in the skills tab.
 *
 * Displays:
 *   - Skill name and coloured icon
 *   - Current level
 *   - Total XP (formatted with commas)
 *   - XP to next level and progress bar
 *   - Next milestone unlock (if any)
 *
 * Positioned to the left of the side panel; falls back to a centred position
 * if the screen is too narrow. Dismissed by clicking anywhere outside the popup.
 */
public class SkillDetailPopup {

    private static final int POPUP_W = 220;
    private static final int POPUP_H = 120;
    private static final int ICON_SZ = 24;
    private static final int MARGIN  = 8;

    /** All 23 skill names indexed by Player.SKILL_* constant. */
    private static final String[] SKILL_NAMES = {
        "Attack",       "Strength",     "Defence",      "Hitpoints",
        "Ranged",       "Magic",        "Prayer",       "Woodcutting",
        "Fishing",      "Cooking",      "Mining",       "Smithing",
        "Firemaking",   "Crafting",     "Runecrafting", "Fletching",
        "Agility",      "Herblore",     "Thieving",     "Slayer",
        "Farming",      "Hunter",       "Construction"
    };

    /** Per-skill icon RGB (matches SidePanel.SKILL_COLORS). */
    private static final float[][] ICON_COLORS = {
        {0.72f, 0.18f, 0.10f}, {0.52f, 0.38f, 0.26f}, {0.22f, 0.42f, 0.72f},
        {0.85f, 0.15f, 0.15f}, {0.40f, 0.58f, 0.20f}, {0.30f, 0.25f, 0.78f},
        {0.90f, 0.85f, 0.55f}, {0.55f, 0.32f, 0.12f}, {0.20f, 0.48f, 0.78f},
        {0.82f, 0.38f, 0.12f}, {0.52f, 0.52f, 0.55f}, {0.55f, 0.52f, 0.50f},
        {0.92f, 0.52f, 0.08f}, {0.75f, 0.68f, 0.28f}, {0.58f, 0.35f, 0.72f},
        {0.62f, 0.55f, 0.28f}, {0.35f, 0.58f, 0.78f}, {0.25f, 0.65f, 0.30f},
        {0.48f, 0.38f, 0.52f}, {0.78f, 0.14f, 0.14f}, {0.30f, 0.62f, 0.22f},
        {0.78f, 0.62f, 0.28f}, {0.80f, 0.68f, 0.38f},
    };

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------
    private boolean visible    = false;
    private int skillIdx       = -1;
    private int level          = 0;
    private long totalXp       = 0L;   // whole XP (not tenths)

    /** Last rendered bounding box for hit-testing. */
    private int lastX, lastY;

    private final GlyphLayout layout = new GlyphLayout();

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Show the popup for the given skill.
     *
     * @param skillIdx  Player.SKILL_* constant (0–22)
     * @param level     current skill level (1–99)
     * @param totalXp   total XP, whole number (server value already divided by 10)
     */
    public void show(int skillIdx, int level, long totalXp) {
        if (skillIdx < 0 || skillIdx >= SKILL_NAMES.length) return;
        this.skillIdx = skillIdx;
        this.level    = level;
        this.totalXp  = totalXp;
        this.visible  = true;
    }

    public void dismiss() {
        visible = false;
    }

    public boolean isVisible() {
        return visible;
    }

    /**
     * Consume a click. Returns true if the click was OUTSIDE the popup
     * (the popup should dismiss) and false if inside (click absorbed).
     * Caller should dismiss when true is returned.
     */
    public boolean handleClick(int screenX, int screenY) {
        if (!visible) return false;
        boolean inside = screenX >= lastX && screenX < lastX + POPUP_W
                      && screenY >= lastY && screenY < lastY + POPUP_H;
        if (!inside) {
            visible = false;
            return false;  // click not consumed — let it propagate
        }
        return true;  // click inside popup consumed
    }

    /**
     * Render the popup.
     *
     * @param panelX     x coordinate of the left edge of the side panel
     * @param screenW    screen width in pixels
     * @param screenH    screen height in pixels
     */
    public void render(ShapeRenderer sr, SpriteBatch batch, BitmapFont font,
                       int panelX, int screenW, int screenH, Matrix4 proj) {
        if (!visible) return;

        // Position: to the left of the side panel, vertically centred on screen
        int popX = panelX - POPUP_W - MARGIN;
        if (popX < MARGIN) popX = (screenW - POPUP_W) / 2;  // fallback: centre
        int popY = (screenH - POPUP_H) / 2;
        lastX = popX;
        lastY = popY;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        sr.setProjectionMatrix(proj);

        // Background
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.10f, 0.08f, 0.05f, 0.97f);
        sr.rect(popX, popY, POPUP_W, POPUP_H);
        sr.end();

        // Gold border
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(0.82f, 0.68f, 0.10f, 1f);
        sr.rect(popX, popY, POPUP_W, POPUP_H);
        sr.end();
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(0.50f, 0.40f, 0.06f, 0.7f);
        sr.rect(popX + 2, popY + 2, POPUP_W - 4, POPUP_H - 4);
        sr.end();

        // Skill icon
        float[] ic = ICON_COLORS[skillIdx];
        int iconX = popX + 8;
        int iconY = popY + POPUP_H - 8 - ICON_SZ;
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(ic[0], ic[1], ic[2], 1f);
        sr.rect(iconX, iconY, ICON_SZ, ICON_SZ);
        sr.setColor(Math.min(1f, ic[0] + 0.3f), Math.min(1f, ic[1] + 0.3f),
                    Math.min(1f, ic[2] + 0.3f), 0.5f);
        sr.rect(iconX, iconY + ICON_SZ - 3, ICON_SZ, 3);
        sr.rect(iconX, iconY, 3, ICON_SZ);

        // XP progress bar
        long xpThisLevel = XpTable.xpForLevel(level);
        long xpNextLevel = XpTable.xpForNextLevel(level);
        float progress;
        long xpToNext;
        if (level >= 99) {
            progress  = 1f;
            xpToNext  = 0L;
        } else {
            long span = xpNextLevel - xpThisLevel;
            long done = totalXp - xpThisLevel;
            progress  = span > 0 ? Math.max(0f, Math.min(1f, (float) done / span)) : 1f;
            xpToNext  = Math.max(0L, xpNextLevel - totalXp);
        }

        int barX = popX + 8;
        int barY = popY + 22;
        int barW = POPUP_W - 16;
        int barH = 8;
        // Track
        sr.setColor(0.20f, 0.16f, 0.08f, 1f);
        sr.rect(barX, barY, barW, barH);
        // Fill
        sr.setColor(0.15f, 0.72f, 0.15f, 1f);
        sr.rect(barX, barY, (int)(barW * progress), barH);
        sr.end();

        // Bar border
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(0.40f, 0.34f, 0.14f, 1f);
        sr.rect(barX, barY, barW, barH);
        sr.end();

        // Text
        batch.setProjectionMatrix(proj);
        batch.begin();

        int tx = iconX + ICON_SZ + 8;

        // Skill name + level
        font.getData().setScale(0.84f);
        font.setColor(1f, 0.92f, 0.10f, 1f);
        font.draw(batch, SKILL_NAMES[skillIdx], tx, popY + POPUP_H - 10);
        font.getData().setScale(0.78f);
        font.setColor(1f, 1f, 1f, 1f);
        font.draw(batch, "Level " + level, tx, popY + POPUP_H - 26);

        // Total XP
        font.getData().setScale(0.72f);
        font.setColor(0.85f, 0.85f, 0.85f, 1f);
        font.draw(batch, "XP: " + formatXp(totalXp), popX + 8, popY + POPUP_H - 46);

        // XP to next level
        if (level < 99) {
            font.setColor(0.75f, 0.75f, 0.75f, 1f);
            font.draw(batch, "Remaining: " + formatXp(xpToNext), popX + 8, popY + POPUP_H - 60);
        } else {
            font.setColor(0.90f, 0.80f, 0.10f, 1f);
            font.draw(batch, "Maximum level achieved!", popX + 8, popY + POPUP_H - 60);
        }

        // Progress percent below bar
        font.getData().setScale(0.66f);
        font.setColor(0.65f, 0.65f, 0.65f, 1f);
        font.draw(batch, String.format("%.1f%%", progress * 100f), popX + 8, barY + barH + 13);

        // Next milestone
        String next = LevelUnlockRegistry.getNextMilestoneText(skillIdx, level);
        if (next != null) {
            font.getData().setScale(0.62f);
            font.setColor(0.60f, 0.90f, 1.0f, 1f);
            // Truncate to fit popup width (rough 6px/char at this scale)
            if (next.length() > 46) next = next.substring(0, 43) + "...";
            font.draw(batch, next, popX + 8, popY + 12);
        }

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static String formatXp(long xp) {
        // Insert commas every 3 digits
        String s = Long.toString(xp);
        StringBuilder sb = new StringBuilder();
        int start = s.length() % 3;
        if (start > 0) sb.append(s, 0, start);
        for (int i = start; i < s.length(); i += 3) {
            if (sb.length() > 0) sb.append(',');
            sb.append(s, i, i + 3);
        }
        return sb.toString();
    }
}
