package com.osrs.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;

/**
 * OSRS-style Combat Options panel.
 *
 * Rendered in the bottom-right corner as a 2×2 grid of four buttons:
 *   Accurate  | Aggressive
 *   Defensive | Controlled
 *
 * The selected style is highlighted in gold. Clicking a button returns the
 * new style index (0–3) so the caller can send SetCombatStyle to the server.
 */
public class CombatStyleUI {

    // Panel layout
    public static final int BUTTON_W  = 88;
    public static final int BUTTON_H  = 36;
    public static final int PADDING   = 4;
    public static final int PANEL_W   = BUTTON_W * 2 + PADDING * 3;
    public static final int PANEL_H   = BUTTON_H * 2 + PADDING * 3;

    private static final String[] STYLE_NAMES = {"Accurate", "Aggressive", "Defensive", "Controlled"};
    /** OSRS XP label shown under each style name. */
    private static final String[] XP_LABELS   = {
        "Attack XP",
        "Strength XP",
        "Defence XP",
        "Shared XP"
    };

    private int selectedStyle = 1; // Default: Aggressive (matches Player.java default)

    // -----------------------------------------------------------------------
    // Rendering
    // -----------------------------------------------------------------------

    /**
     * Renders the panel in the bottom-right corner.
     *
     * @param shapeRenderer active ShapeRenderer (uses screenProjection)
     * @param batch         active SpriteBatch   (uses screenProjection)
     * @param font          font to use
     * @param screenW       full screen width
     * @param screenH       full screen height
     * @param proj          screen-space projection matrix
     */
    public void render(ShapeRenderer shapeRenderer, SpriteBatch batch,
                       BitmapFont font, int screenW, int screenH, Matrix4 proj) {
        int panelX = screenW - PANEL_W - 8;
        int panelY = 8;

        shapeRenderer.setProjectionMatrix(proj);

        // Background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.06f, 0.06f, 0.15f, 0.92f);
        shapeRenderer.rect(panelX, panelY, PANEL_W, PANEL_H);
        shapeRenderer.end();

        // Border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.5f, 0.4f, 0.1f, 1f);
        shapeRenderer.rect(panelX, panelY, PANEL_W, PANEL_H);
        shapeRenderer.end();

        // Buttons
        for (int i = 0; i < 4; i++) {
            int col = i % 2;
            int row = 1 - i / 2; // row 1 = top row
            int bx = panelX + PADDING + col * (BUTTON_W + PADDING);
            int by = panelY + PADDING + row * (BUTTON_H + PADDING);

            boolean selected = (i == selectedStyle);

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            if (selected) {
                shapeRenderer.setColor(0.4f, 0.32f, 0.05f, 1f); // gold highlight
            } else {
                shapeRenderer.setColor(0.15f, 0.15f, 0.25f, 1f); // dark blue-grey
            }
            shapeRenderer.rect(bx, by, BUTTON_W, BUTTON_H);
            shapeRenderer.end();

            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(selected ? new Color(1f, 0.85f, 0.1f, 1f) : new Color(0.4f, 0.4f, 0.5f, 1f));
            shapeRenderer.rect(bx, by, BUTTON_W, BUTTON_H);
            shapeRenderer.end();
        }

        // Labels
        batch.setProjectionMatrix(proj);
        batch.begin();
        font.getData().setScale(0.85f);
        for (int i = 0; i < 4; i++) {
            int col = i % 2;
            int row = 1 - i / 2;
            int bx = panelX + PADDING + col * (BUTTON_W + PADDING);
            int by = panelY + PADDING + row * (BUTTON_H + PADDING);

            boolean selected = (i == selectedStyle);
            font.setColor(selected ? new Color(1f, 0.9f, 0.1f, 1f) : Color.WHITE);
            font.draw(batch, STYLE_NAMES[i], bx + 6, by + BUTTON_H - 8);

            font.setColor(0.65f, 0.65f, 0.65f, 1f);
            font.draw(batch, XP_LABELS[i], bx + 6, by + 12);
        }
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    // -----------------------------------------------------------------------
    // Input
    // -----------------------------------------------------------------------

    /**
     * Check if the given screen-space coordinates hit a button.
     *
     * @return style index (0–3) if clicked, or -1 if not over the panel.
     */
    public int handleClick(int mx, int my, int screenW, int screenH) {
        int panelX = screenW - PANEL_W - 8;
        int panelY = 8;

        if (mx < panelX || mx > panelX + PANEL_W || my < panelY || my > panelY + PANEL_H) {
            return -1;
        }

        for (int i = 0; i < 4; i++) {
            int col = i % 2;
            int row = 1 - i / 2;
            int bx = panelX + PADDING + col * (BUTTON_W + PADDING);
            int by = panelY + PADDING + row * (BUTTON_H + PADDING);

            if (mx >= bx && mx <= bx + BUTTON_W && my >= by && my <= by + BUTTON_H) {
                selectedStyle = i;
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns true if the given screen coordinates are within this panel.
     * Used to block pass-through clicks on the world.
     */
    public boolean isOverPanel(int mx, int my, int screenW, int screenH) {
        int panelX = screenW - PANEL_W - 8;
        int panelY = 8;
        return mx >= panelX && mx <= panelX + PANEL_W && my >= panelY && my <= panelY + PANEL_H;
    }

    public void setSelectedStyle(int style) {
        if (style >= 0 && style < 4) selectedStyle = style;
    }

    public int getSelectedStyle() { return selectedStyle; }
}
