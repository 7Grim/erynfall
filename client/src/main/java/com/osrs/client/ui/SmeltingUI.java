package com.osrs.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;

import java.util.ArrayList;
import java.util.List;

public class SmeltingUI {

    public static class Option {
        public final int barItemId;
        public final String barName;
        public final int levelRequired;
        public final int xpTenths;
        public final int successPercent;

        public Option(int barItemId,
                      String barName,
                      int levelRequired,
                      int xpTenths,
                      int successPercent) {
            this.barItemId = barItemId;
            this.barName = barName == null ? "" : barName;
            this.levelRequired = levelRequired;
            this.xpTenths = xpTenths;
            this.successPercent = successPercent;
        }
    }

    private static final int PANEL_W = 500;
    private static final int PANEL_H = 360;
    private static final int HEADER_H = 36;
    private static final int PAD = 12;
    private static final int CLOSE_W = 26;
    private static final int CLOSE_H = 22;
    private static final int ROW_H = 36;
    private static final int ROW_GAP = 6;

    private static final Color OVERLAY = new Color(0f, 0f, 0f, 0.62f);
    private static final Color PANEL_BG = new Color(0.14f, 0.12f, 0.09f, 0.98f);
    private static final Color PANEL_BORDER = new Color(0.78f, 0.67f, 0.38f, 1f);
    private static final Color HEADER_BG = new Color(0.24f, 0.19f, 0.12f, 1f);
    private static final Color CLOSE_BG = new Color(0.45f, 0.10f, 0.10f, 1f);

    private boolean visible;
    private int furnaceNpcId = -1;
    private List<Option> options = List.of();

    private int panelX;
    private int panelY;
    private int closeX;
    private int closeY;
    private int listX;
    private int listY;
    private int listW;
    private int listH;

    public void open(int furnaceNpcId, List<Option> options) {
        this.furnaceNpcId = furnaceNpcId;
        this.options = options == null ? List.of() : new ArrayList<>(options);
        this.visible = true;
    }

    public void close() {
        this.visible = false;
        this.furnaceNpcId = -1;
        this.options = List.of();
    }

    public boolean isVisible() {
        return visible;
    }

    public int getFurnaceNpcId() {
        return furnaceNpcId;
    }

    public boolean isOver(int screenX, int screenY) {
        if (!visible) {
            return false;
        }
        return screenX >= panelX && screenX <= panelX + PANEL_W
            && screenY >= panelY && screenY <= panelY + PANEL_H;
    }

    public boolean isCloseButtonHit(int screenX, int screenY) {
        if (!visible) {
            return false;
        }
        return screenX >= closeX && screenX <= closeX + CLOSE_W
            && screenY >= closeY && screenY <= closeY + CLOSE_H;
    }

    public int getClickedBarItemId(int screenX, int screenY) {
        if (!visible) {
            return -1;
        }
        if (screenX < listX || screenX > listX + listW || screenY < listY || screenY > listY + listH) {
            return -1;
        }

        int row = options.size() - 1 - ((screenY - listY) / (ROW_H + ROW_GAP));
        if (row < 0 || row >= options.size()) {
            return -1;
        }

        int rowY = listY + (options.size() - 1 - row) * (ROW_H + ROW_GAP);
        if (screenY < rowY || screenY > rowY + ROW_H) {
            return -1;
        }
        return options.get(row).barItemId;
    }

    public void render(ShapeRenderer shapeRenderer,
                       SpriteBatch batch,
                       BitmapFont font,
                       int screenW,
                       int screenH,
                       Matrix4 projection,
                       int mouseX,
                       int mouseY) {
        if (!visible) {
            return;
        }

        panelX = (screenW - PANEL_W) / 2;
        panelY = (screenH - PANEL_H) / 2;
        closeX = panelX + PANEL_W - CLOSE_W - 8;
        closeY = panelY + PANEL_H - CLOSE_H - 8;
        listX = panelX + PAD;
        listY = panelY + PAD;
        listW = PANEL_W - PAD * 2;
        listH = PANEL_H - HEADER_H - PAD * 2;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(projection);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(OVERLAY);
        shapeRenderer.rect(0, 0, screenW, screenH);
        shapeRenderer.setColor(PANEL_BG);
        shapeRenderer.rect(panelX, panelY, PANEL_W, PANEL_H);
        shapeRenderer.setColor(HEADER_BG);
        shapeRenderer.rect(panelX + 1, panelY + PANEL_H - HEADER_H - 1, PANEL_W - 2, HEADER_H);
        shapeRenderer.setColor(CLOSE_BG);
        shapeRenderer.rect(closeX, closeY, CLOSE_W, CLOSE_H);

        for (int i = 0; i < options.size(); i++) {
            int rowY = listY + (options.size() - 1 - i) * (ROW_H + ROW_GAP);
            if (rowY + ROW_H > listY + listH) {
                continue;
            }
            boolean hover = mouseX >= listX && mouseX <= listX + listW
                && mouseY >= rowY && mouseY <= rowY + ROW_H;
            shapeRenderer.setColor(hover ? 0.26f : 0.20f, hover ? 0.21f : 0.16f, hover ? 0.12f : 0.10f, 1f);
            shapeRenderer.rect(listX, rowY, listW, ROW_H);
        }
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(PANEL_BORDER);
        shapeRenderer.rect(panelX, panelY, PANEL_W, PANEL_H);
        shapeRenderer.rect(closeX, closeY, CLOSE_W, CLOSE_H);
        for (int i = 0; i < options.size(); i++) {
            int rowY = listY + (options.size() - 1 - i) * (ROW_H + ROW_GAP);
            if (rowY + ROW_H > listY + listH) {
                continue;
            }
            shapeRenderer.rect(listX, rowY, listW, ROW_H);
        }
        shapeRenderer.line(closeX + 5, closeY + 5, closeX + CLOSE_W - 5, closeY + CLOSE_H - 5);
        shapeRenderer.line(closeX + CLOSE_W - 5, closeY + 5, closeX + 5, closeY + CLOSE_H - 5);
        shapeRenderer.end();

        batch.setProjectionMatrix(projection);
        batch.begin();
        font.getData().setScale(0.92f);
        font.setColor(1f, 0.92f, 0.58f, 1f);
        font.draw(batch, "Smelt bars", panelX + PAD, panelY + PANEL_H - 12);

        font.getData().setScale(0.74f);
        if (options.isEmpty()) {
            font.setColor(0.88f, 0.86f, 0.82f, 1f);
            font.draw(batch, "You do not have ores for any bars.", listX + 8, listY + listH - 14);
        } else {
            for (int i = 0; i < options.size(); i++) {
                int rowY = listY + (options.size() - 1 - i) * (ROW_H + ROW_GAP);
                if (rowY + ROW_H > listY + listH) {
                    continue;
                }
                Option option = options.get(i);
                String right = "Lv " + option.levelRequired
                    + "  XP " + formatTenths(option.xpTenths)
                    + "  " + option.successPercent + "%";
                font.setColor(0.96f, 0.94f, 0.90f, 1f);
                font.draw(batch, option.barName, listX + 8, rowY + 24);
                font.setColor(0.84f, 0.82f, 0.74f, 1f);
                float textX = Math.max(listX + 180f, listX + listW - right.length() * 5.4f);
                font.draw(batch, right, textX, rowY + 24);
            }
        }
        font.setColor(Color.WHITE);
        font.getData().setScale(1f);
        batch.end();
    }

    private String formatTenths(int tenths) {
        int whole = Math.max(0, tenths / 10);
        int frac = Math.max(0, tenths % 10);
        return whole + "." + frac;
    }
}
