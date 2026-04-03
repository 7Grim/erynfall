package com.osrs.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;

public class AdminToolsPopup {

    private static final int PANEL_W = 420;
    private static final int PANEL_H = 260;
    private static final int HEADER_H = 34;
    private static final int CLOSE_SIZE = 20;
    private static final int TAB_W = 94;
    private static final int TAB_H = 24;
    private static final int TAB_GAP = 8;

    private static final Color OVERLAY = new Color(0.05f, 0.04f, 0.03f, 0.72f);
    private static final Color PANEL_BG = new Color(0.79f, 0.70f, 0.52f, 0.98f);
    private static final Color PANEL_BORDER = new Color(0.42f, 0.30f, 0.15f, 1f);
    private static final Color HEADER_BG = new Color(0.62f, 0.48f, 0.28f, 1f);
    private static final Color CONTENT_BG = new Color(0.85f, 0.77f, 0.60f, 1f);
    private static final Color TAB_BG = new Color(0.66f, 0.54f, 0.35f, 1f);
    private static final Color TAB_SELECTED = new Color(0.86f, 0.72f, 0.35f, 1f);
    private static final Color CLOSE_BG = new Color(0.52f, 0.22f, 0.16f, 1f);

    private static final String[] TAB_NAMES = {"Skills", "Items", "Travel"};

    private boolean visible;
    private int selectedTabIdx;

    private int panelX;
    private int panelY;
    private int closeX;
    private int closeY;
    private int tabsX;
    private int tabsY;
    private int contentX;
    private int contentY;
    private int contentW;
    private int contentH;

    public void show() {
        visible = true;
        selectedTabIdx = Math.max(0, Math.min(selectedTabIdx, TAB_NAMES.length - 1));
    }

    public void dismiss() {
        visible = false;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean handleClick(int mouseX, int mouseY) {
        if (!visible) {
            return false;
        }
        boolean insidePanel = mouseX >= panelX && mouseX <= panelX + PANEL_W
            && mouseY >= panelY && mouseY <= panelY + PANEL_H;
        if (!insidePanel) {
            dismiss();
            return true;
        }
        if (mouseX >= closeX && mouseX <= closeX + CLOSE_SIZE
            && mouseY >= closeY && mouseY <= closeY + CLOSE_SIZE) {
            dismiss();
            return true;
        }
        for (int i = 0; i < TAB_NAMES.length; i++) {
            int tx = tabsX + i * (TAB_W + TAB_GAP);
            if (mouseX >= tx && mouseX <= tx + TAB_W && mouseY >= tabsY && mouseY <= tabsY + TAB_H) {
                selectedTabIdx = i;
                return true;
            }
        }
        return true;
    }

    public void render(ShapeRenderer shapeRenderer,
                       SpriteBatch batch,
                       BitmapFont font,
                       int screenW,
                       int screenH,
                       Matrix4 projection) {
        if (!visible) {
            return;
        }

        panelX = (screenW - PANEL_W) / 2;
        panelY = (screenH - PANEL_H) / 2;
        closeX = panelX + PANEL_W - CLOSE_SIZE - 8;
        closeY = panelY + PANEL_H - CLOSE_SIZE - 7;
        tabsX = panelX + 14;
        tabsY = panelY + PANEL_H - HEADER_H - TAB_H - 8;
        contentX = panelX + 12;
        contentY = panelY + 12;
        contentW = PANEL_W - 24;
        contentH = PANEL_H - HEADER_H - TAB_H - 30;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(projection);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(OVERLAY);
        shapeRenderer.rect(0, 0, screenW, screenH);
        shapeRenderer.setColor(PANEL_BG);
        shapeRenderer.rect(panelX, panelY, PANEL_W, PANEL_H);
        shapeRenderer.setColor(HEADER_BG);
        shapeRenderer.rect(panelX + 2, panelY + PANEL_H - HEADER_H - 2, PANEL_W - 4, HEADER_H);
        shapeRenderer.setColor(CONTENT_BG);
        shapeRenderer.rect(contentX, contentY, contentW, contentH);
        for (int i = 0; i < TAB_NAMES.length; i++) {
            shapeRenderer.setColor(i == selectedTabIdx ? TAB_SELECTED : TAB_BG);
            shapeRenderer.rect(tabsX + i * (TAB_W + TAB_GAP), tabsY, TAB_W, TAB_H);
        }
        shapeRenderer.setColor(CLOSE_BG);
        shapeRenderer.rect(closeX, closeY, CLOSE_SIZE, CLOSE_SIZE);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(PANEL_BORDER);
        shapeRenderer.rect(panelX, panelY, PANEL_W, PANEL_H);
        shapeRenderer.rect(panelX + 2, panelY + 2, PANEL_W - 4, PANEL_H - 4);
        shapeRenderer.rect(contentX, contentY, contentW, contentH);
        for (int i = 0; i < TAB_NAMES.length; i++) {
            shapeRenderer.rect(tabsX + i * (TAB_W + TAB_GAP), tabsY, TAB_W, TAB_H);
        }
        shapeRenderer.rect(closeX, closeY, CLOSE_SIZE, CLOSE_SIZE);
        shapeRenderer.line(closeX + 5, closeY + 5, closeX + CLOSE_SIZE - 5, closeY + CLOSE_SIZE - 5);
        shapeRenderer.line(closeX + CLOSE_SIZE - 5, closeY + 5, closeX + 5, closeY + CLOSE_SIZE - 5);
        shapeRenderer.end();

        batch.setProjectionMatrix(projection);
        batch.begin();
        font.getData().setScale(0.95f);
        font.setColor(0.20f, 0.12f, 0.04f, 1f);
        font.draw(batch, "Admin Tools", panelX + 12, panelY + PANEL_H - 9);
        font.getData().setScale(0.72f);
        for (int i = 0; i < TAB_NAMES.length; i++) {
            font.setColor(i == selectedTabIdx ? new Color(0.18f, 0.12f, 0.04f, 1f) : new Color(0.32f, 0.20f, 0.08f, 1f));
            font.draw(batch, TAB_NAMES[i], tabsX + i * (TAB_W + TAB_GAP) + 10, tabsY + 16);
        }

        font.setColor(0.24f, 0.16f, 0.06f, 1f);
        String placeholder = switch (selectedTabIdx) {
            case 0 -> "Skills tools coming next.";
            case 1 -> "Item tools coming next.";
            case 2 -> "Travel tools coming next.";
            default -> "Tools coming next.";
        };
        font.draw(batch, placeholder, contentX + 12, contentY + contentH - 16);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }
}
