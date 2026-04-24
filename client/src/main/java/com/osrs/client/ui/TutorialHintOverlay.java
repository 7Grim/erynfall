package com.osrs.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;

/**
 * Top-right corner hint panel shown during the starter quest (quest ID 1).
 *
 * Stone aesthetic: dark bg + outer border + inner highlight + gold title bar.
 * Dims:  270 x 54 px, 14 px margin from top-right corner.
 * Shows: quest title row, current incomplete task description below.
 * Hidden once starter quest is marked complete.
 */
public class TutorialHintOverlay {

    private static final int   PANEL_W       = 270;
    private static final int   PANEL_H       = 54;
    private static final int   MARGIN        = 14;
    private static final int   TITLE_ROW_H   = 18;
    private static final float TITLE_SCALE   = 0.72f;
    private static final float BODY_SCALE    = 0.80f;
    private static final int   TEXT_PAD_X    = 8;

    // Stone colour palette (matches LoginScreen)
    private static final Color PANEL_BG    = new Color(0.07f, 0.07f, 0.08f, 0.88f);
    private static final Color BORDER_OUT  = new Color(0.18f, 0.16f, 0.13f, 1f);
    private static final Color BORDER_IN   = new Color(0.32f, 0.28f, 0.20f, 1f);
    private static final Color TITLE_BG    = new Color(0.12f, 0.10f, 0.08f, 1f);
    private static final Color GOLD        = new Color(0.72f, 0.58f, 0.18f, 1f);
    private static final Color WHITE       = new Color(1f, 1f, 1f, 0.92f);
    private static final Color DIM_GREY    = new Color(0.55f, 0.55f, 0.55f, 1f);

    private String  currentTask      = null;
    private boolean starterComplete = false;

    /** Call whenever a QuestUpdate for quest 1 arrives. */
    public void onQuestUpdate(boolean complete, String firstIncompleteTaskDescription) {
        starterComplete = complete;
        currentTask = firstIncompleteTaskDescription;
    }

    public void render(ShapeRenderer sr, SpriteBatch batch, BitmapFont font,
                       int screenW, int screenH, Matrix4 projection) {
        if (starterComplete || currentTask == null || currentTask.isBlank()) return;

        int panelX = (screenW - PANEL_W) / 2;   // horizontally centered
        int panelY = screenH - PANEL_H - MARGIN; // top edge with margin

        // ── shapes ──────────────────────────────────────────────────────────
        sr.setProjectionMatrix(projection);
        sr.begin(ShapeRenderer.ShapeType.Filled);

        // outer border
        sr.setColor(BORDER_OUT);
        sr.rect(panelX - 2, panelY - 2, PANEL_W + 4, PANEL_H + 4);

        // inner highlight
        sr.setColor(BORDER_IN);
        sr.rect(panelX - 1, panelY - 1, PANEL_W + 2, PANEL_H + 2);

        // panel bg
        sr.setColor(PANEL_BG);
        sr.rect(panelX, panelY, PANEL_W, PANEL_H);

        // title bar bg
        sr.setColor(TITLE_BG);
        sr.rect(panelX, panelY + PANEL_H - TITLE_ROW_H, PANEL_W, TITLE_ROW_H);

        // divider line between title and body
        sr.setColor(BORDER_IN);
        sr.rect(panelX, panelY + PANEL_H - TITLE_ROW_H - 1, PANEL_W, 1);

        sr.end();

        // ── alpha blend for text ─────────────────────────────────────────────
        com.badlogic.gdx.Gdx.gl.glEnable(GL20.GL_BLEND);

        batch.setProjectionMatrix(projection);
        batch.begin();

        float savedScaleX = font.getData().scaleX;
        float savedScaleY = font.getData().scaleY;

        // title
        font.getData().setScale(TITLE_SCALE);
        font.setColor(GOLD);
        String title = "ARRIVAL";
        GlyphLayout titleLayout = new GlyphLayout(font, title);
        float titleX = panelX + (PANEL_W - titleLayout.width) * 0.5f;
        float titleY = panelY + PANEL_H - (TITLE_ROW_H - titleLayout.height) * 0.5f;
        font.draw(batch, title, titleX, titleY);

        // body task description (truncate if too wide)
        font.getData().setScale(BODY_SCALE);
        font.setColor(WHITE);
        float maxBodyW = PANEL_W - TEXT_PAD_X * 2;
        String body = truncateToFit(font, currentTask, maxBodyW);
        GlyphLayout bodyLayout = new GlyphLayout(font, body);
        float bodyAreaH = PANEL_H - TITLE_ROW_H;
        float bodyX = panelX + TEXT_PAD_X;
        float bodyY = panelY + PANEL_H - TITLE_ROW_H - (bodyAreaH - bodyLayout.height) * 0.5f;
        font.draw(batch, body, bodyX, bodyY);

        // restore font scale
        font.getData().setScale(savedScaleX, savedScaleY);
        font.setColor(Color.WHITE);

        batch.end();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String truncateToFit(BitmapFont font, String text, float maxWidth) {
        GlyphLayout gl = new GlyphLayout(font, text);
        if (gl.width <= maxWidth) return text;
        // Binary-search the longest prefix that fits with "..."
        String ellipsis = "...";
        int lo = 0, hi = text.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            gl.setText(font, text.substring(0, mid) + ellipsis);
            if (gl.width <= maxWidth) lo = mid;
            else hi = mid - 1;
        }
        return text.substring(0, lo) + ellipsis;
    }
}
