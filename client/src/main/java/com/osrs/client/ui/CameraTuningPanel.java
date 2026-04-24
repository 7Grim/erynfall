package com.osrs.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.osrs.client.CameraConfig;

/**
 * Developer-only overlay for live camera tuning.
 * Toggle with F9. Writes directly to CameraConfig mutable statics;
 * changes take effect the next frame without recompiling.
 *
 * To copy found values back to code: read the displayed numbers and paste
 * them into the _INITIAL constants in CameraConfig.java.
 *
 * Disabled entirely when CameraConfig.TUNING_PANEL_ENABLED == false.
 */
public class CameraTuningPanel {

    // --- Layout constants ---
    private static final int PANEL_W   = 238;
    private static final int HEADER_H  = 26;
    private static final int ROW_H     = 22;
    private static final int ROWS      = 8;
    private static final int SEP_H     = 6;
    private static final int FOOTER_H  = 22;
    private static final int PAD       = 8;
    private static final int BTN_W     = 20;
    private static final int BTN_H     = 15;
    private static final int PANEL_H   = HEADER_H + ROWS * ROW_H + SEP_H + FOOTER_H + PAD;

    // --- Dev-tool colour scheme: intentionally dark/blue, distinct from production OSRS gold UI ---
    private static final Color BG        = new Color(0.09f, 0.10f, 0.13f, 0.93f);
    private static final Color HDR_BG    = new Color(0.16f, 0.19f, 0.25f, 1.00f);
    private static final Color BORDER    = new Color(0.38f, 0.53f, 0.72f, 1.00f);
    private static final Color BTN_BG    = new Color(0.20f, 0.27f, 0.38f, 1.00f);
    private static final Color CLOSE_BG  = new Color(0.42f, 0.16f, 0.16f, 1.00f);
    private static final Color RESET_BG  = new Color(0.24f, 0.20f, 0.10f, 1.00f);
    private static final Color SEP_COL   = new Color(0.28f, 0.34f, 0.46f, 0.90f);
    private static final Color TXT_HDR   = new Color(0.72f, 0.87f, 1.00f, 1.00f);
    private static final Color TXT_LABEL = new Color(0.68f, 0.77f, 0.86f, 1.00f);
    private static final Color TXT_VAL   = new Color(1.00f, 0.88f, 0.44f, 1.00f);
    private static final Color TXT_HINT  = new Color(0.46f, 0.55f, 0.64f, 1.00f);

    // --- Row definitions ---
    private static final String[] LABELS = {
        "FOV",  "Pitch", "PitchMin", "PitchMax",
        "Dist", "DistMin", "DistMax", "LookAtH"
    };
    private static final float[] STEPS = {
        1.0f,   0.05f, 0.05f, 0.05f,
        0.5f,   0.5f,  0.5f,  0.05f
    };

    // --- Runtime state ---
    private boolean visible;
    private final GlyphLayout glyph = new GlyphLayout();

    // Hit-box positions computed each render, reused in handleClick.
    private int panelX, panelY;
    private final int[] decX  = new int[ROWS];
    private final int[] incX  = new int[ROWS];
    private final int[] btnY  = new int[ROWS];
    private int closeX, closeY, closeSz;
    private int resetX, resetY, resetW, resetH;

    // -------------------------------------------------------------------------

    public void toggle()     { visible = !visible; }
    public boolean isVisible() { return visible; }

    // -------------------------------------------------------------------------
    // Row value accessors — keep in sync with LABELS / STEPS order.

    private static float get(int row) {
        switch (row) {
            case 0: return CameraConfig.FOV;
            case 1: return CameraConfig.PITCH_DEFAULT;
            case 2: return CameraConfig.PITCH_MIN;
            case 3: return CameraConfig.PITCH_MAX;
            case 4: return CameraConfig.DISTANCE_DEFAULT;
            case 5: return CameraConfig.DISTANCE_MIN;
            case 6: return CameraConfig.DISTANCE_MAX;
            case 7: return CameraConfig.LOOKAT_HEIGHT;
            default: return 0f;
        }
    }

    private static void set(int row, float v) {
        switch (row) {
            case 0:
                CameraConfig.FOV = clamp(v, 10f, 90f);
                break;
            case 1:
                CameraConfig.PITCH_DEFAULT = clamp(v, CameraConfig.PITCH_MIN, CameraConfig.PITCH_MAX);
                break;
            case 2:
                CameraConfig.PITCH_MIN = clamp(v, 0.10f, CameraConfig.PITCH_MAX - 0.05f);
                break;
            case 3:
                CameraConfig.PITCH_MAX = clamp(v, CameraConfig.PITCH_MIN + 0.05f, 1.57f);
                break;
            case 4:
                CameraConfig.DISTANCE_DEFAULT = clamp(v, CameraConfig.DISTANCE_MIN, CameraConfig.DISTANCE_MAX);
                break;
            case 5:
                CameraConfig.DISTANCE_MIN = clamp(v, 1f, CameraConfig.DISTANCE_MAX - 0.5f);
                break;
            case 6:
                CameraConfig.DISTANCE_MAX = clamp(v, CameraConfig.DISTANCE_MIN + 0.5f, 60f);
                break;
            case 7:
                CameraConfig.LOOKAT_HEIGHT = clamp(v, 0f, 3f);
                break;
            default:
                break;
        }
    }

    private static void resetDefaults() {
        CameraConfig.FOV              = CameraConfig.FOV_INITIAL;
        CameraConfig.PITCH_DEFAULT    = CameraConfig.PITCH_DEFAULT_INITIAL;
        CameraConfig.PITCH_MIN        = CameraConfig.PITCH_MIN_INITIAL;
        CameraConfig.PITCH_MAX        = CameraConfig.PITCH_MAX_INITIAL;
        CameraConfig.DISTANCE_DEFAULT = CameraConfig.DISTANCE_DEFAULT_INITIAL;
        CameraConfig.DISTANCE_MIN     = CameraConfig.DISTANCE_MIN_INITIAL;
        CameraConfig.DISTANCE_MAX     = CameraConfig.DISTANCE_MAX_INITIAL;
        CameraConfig.LOOKAT_HEIGHT    = CameraConfig.LOOKAT_HEIGHT_INITIAL;
    }

    // -------------------------------------------------------------------------
    // Render

    public void render(ShapeRenderer sr, SpriteBatch batch, BitmapFont font,
                       int screenW, int screenH, Matrix4 proj) {
        if (!visible) return;

        // Anchor to top-left corner (10px margin) — away from minimap and side panel.
        panelX = 10;
        panelY = screenH - 10 - PANEL_H;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        sr.setProjectionMatrix(proj);

        // --- filled shapes ---
        sr.begin(ShapeRenderer.ShapeType.Filled);

        sr.setColor(BG);
        sr.rect(panelX, panelY, PANEL_W, PANEL_H);

        sr.setColor(HDR_BG);
        sr.rect(panelX, panelY + PANEL_H - HEADER_H, PANEL_W, HEADER_H);

        closeSz = HEADER_H - 6;
        closeX  = panelX + PANEL_W - closeSz - 3;
        closeY  = panelY + PANEL_H - HEADER_H + 3;
        sr.setColor(CLOSE_BG);
        sr.rect(closeX, closeY, closeSz, closeSz);

        for (int i = 0; i < ROWS; i++) {
            int ry = rowBottomY(i);
            btnY[i] = ry + (ROW_H - BTN_H) / 2;
            decX[i] = panelX + PANEL_W - PAD - BTN_W * 2 - 3;
            incX[i] = panelX + PANEL_W - PAD - BTN_W;
            sr.setColor(BTN_BG);
            sr.rect(decX[i], btnY[i], BTN_W, BTN_H);
            sr.rect(incX[i], btnY[i], BTN_W, BTN_H);
        }

        // separator line above footer
        int sepY = panelY + FOOTER_H + PAD / 2;
        sr.setColor(SEP_COL);
        sr.rect(panelX + PAD, sepY, PANEL_W - PAD * 2, 1);

        // reset button
        resetW = PANEL_W - PAD * 2;
        resetH = 14;
        resetX = panelX + PAD;
        resetY = panelY + (FOOTER_H - resetH) / 2;
        sr.setColor(RESET_BG);
        sr.rect(resetX, resetY, resetW, resetH);

        sr.end();

        // --- line borders ---
        sr.begin(ShapeRenderer.ShapeType.Line);

        sr.setColor(BORDER);
        sr.rect(panelX, panelY, PANEL_W, PANEL_H);

        // close X lines
        sr.setColor(BORDER);
        sr.rect(closeX, closeY, closeSz, closeSz);
        int cx1 = closeX + 4, cy1 = closeY + 4;
        int cx2 = closeX + closeSz - 4, cy2 = closeY + closeSz - 4;
        sr.line(cx1, cy1, cx2, cy2);
        sr.line(cx2, cy1, cx1, cy2);

        sr.setColor(BORDER);
        sr.rect(resetX, resetY, resetW, resetH);

        sr.end();

        // --- text ---
        batch.setProjectionMatrix(proj);
        batch.begin();

        font.getData().setScale(0.70f);
        font.setColor(TXT_HDR);
        font.draw(batch, "CAM TUNING  [F9]", panelX + PAD, panelY + PANEL_H - 7);

        font.getData().setScale(0.65f);
        for (int i = 0; i < ROWS; i++) {
            float textY = rowBottomY(i) + ROW_H - 4;

            font.setColor(TXT_LABEL);
            font.draw(batch, LABELS[i], panelX + PAD, textY);

            String val = String.format("%.2f", get(i));
            font.setColor(TXT_VAL);
            glyph.setText(font, val);
            font.draw(batch, val, decX[i] - 5 - glyph.width, textY);

            font.setColor(TXT_HDR);
            // '-' centred in dec button
            glyph.setText(font, "-");
            font.draw(batch, "-",
                decX[i] + (BTN_W - glyph.width) / 2f,
                btnY[i] + BTN_H - 2);
            // '+' centred in inc button
            glyph.setText(font, "+");
            font.draw(batch, "+",
                incX[i] + (BTN_W - glyph.width) / 2f,
                btnY[i] + BTN_H - 2);
        }

        // reset button label
        font.getData().setScale(0.60f);
        font.setColor(TXT_HINT);
        String rl = "RESET TO DEFAULTS";
        glyph.setText(font, rl);
        font.draw(batch, rl,
            resetX + (resetW - glyph.width) / 2f,
            resetY + resetH - 2);

        // footer hint: remind the developer where to paste found values
        font.getData().setScale(0.58f);
        font.setColor(TXT_HINT);
        font.draw(batch, "values → CameraConfig.java _INITIAL",
            panelX + PAD, panelY + FOOTER_H + PAD / 2f - 2);

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    // -------------------------------------------------------------------------
    // Input

    /**
     * Call on every left-click frame.
     * Returns true if the click was consumed (inside the panel).
     */
    public boolean handleClick(int mx, int my) {
        if (!visible) return false;

        if (hit(mx, my, closeX, closeY, closeSz, closeSz)) {
            visible = false;
            return true;
        }

        if (hit(mx, my, resetX, resetY, resetW, resetH)) {
            resetDefaults();
            return true;
        }

        for (int i = 0; i < ROWS; i++) {
            if (hit(mx, my, decX[i], btnY[i], BTN_W, BTN_H)) {
                set(i, get(i) - STEPS[i]);
                return true;
            }
            if (hit(mx, my, incX[i], btnY[i], BTN_W, BTN_H)) {
                set(i, get(i) + STEPS[i]);
                return true;
            }
        }

        // Absorb any click within the panel bounds so it doesn't pathfind the character.
        return hit(mx, my, panelX, panelY, PANEL_W, PANEL_H);
    }

    // -------------------------------------------------------------------------
    // Helpers

    private int rowBottomY(int i) {
        // Row 0 is directly below the header; rows increase downward.
        return panelY + PANEL_H - HEADER_H - (i + 1) * ROW_H;
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static boolean hit(int mx, int my, int rx, int ry, int rw, int rh) {
        return mx >= rx && mx < rx + rw && my >= ry && my < ry + rh;
    }
}
