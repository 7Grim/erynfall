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
        public final int levelRequirement;
        public final int smithingXpTenths;
        public final int successPercent;
        public final int oreItemIdA;
        public final int oreQtyA;
        public final int oreItemIdB;
        public final int oreQtyB;
        public final int coalRequired;

        public Option(int barItemId,
                      String barName,
                      int levelRequirement,
                      int smithingXpTenths,
                      int successPercent,
                      int oreItemIdA,
                      int oreQtyA,
                      int oreItemIdB,
                      int oreQtyB,
                      int coalRequired) {
            this.barItemId = barItemId;
            this.barName = barName == null ? "" : barName;
            this.levelRequirement = levelRequirement;
            this.smithingXpTenths = smithingXpTenths;
            this.successPercent = successPercent;
            this.oreItemIdA = oreItemIdA;
            this.oreQtyA = oreQtyA;
            this.oreItemIdB = oreItemIdB;
            this.oreQtyB = oreQtyB;
            this.coalRequired = coalRequired;
        }
    }

    private static final int PANEL_W = 500;
    private static final int PANEL_H = 360;
    private static final int HEADER_H = 36;
    private static final int PAD = 12;
    private static final int CLOSE_W = 26;
    private static final int CLOSE_H = 22;
    private static final int ROW_H = 42;
    private static final int ROW_GAP = 6;

    private static final Color OVERLAY = new Color(0f, 0f, 0f, 0.62f);
    private static final Color PANEL_BG = new Color(0.14f, 0.12f, 0.09f, 0.98f);
    private static final Color PANEL_BORDER = new Color(0.78f, 0.67f, 0.38f, 1f);
    private static final Color HEADER_BG = new Color(0.24f, 0.19f, 0.12f, 1f);
    private static final Color CLOSE_BG = new Color(0.45f, 0.10f, 0.10f, 1f);

    private boolean visible;
    private int npcId = -1;
    private List<Option> options = List.of();
    private int selectedBarItemId = -1;

    private int panelX;
    private int panelY;
    private int closeX;
    private int closeY;
    private int listX;
    private int listY;
    private int listW;
    private int listH;

    public void show(int npcId, List<Option> options) {
        this.npcId = npcId;
        this.options = options == null ? List.of() : new ArrayList<>(options);
        this.visible = true;
        this.selectedBarItemId = -1;
    }

    public void dismiss() {
        this.visible = false;
        this.npcId = -1;
        this.options = List.of();
        this.selectedBarItemId = -1;
    }

    public boolean isVisible() {
        return visible;
    }

    public int getNpcId() {
        return npcId;
    }

    public boolean isOver(int screenX, int screenY) {
        if (!visible) {
            return false;
        }
        return screenX >= panelX && screenX <= panelX + PANEL_W
            && screenY >= panelY && screenY <= panelY + PANEL_H;
    }

    public void handleClick(int screenX, int screenY) {
        if (!visible) {
            return;
        }
        if (!isOver(screenX, screenY)
            || (screenX >= closeX && screenX <= closeX + CLOSE_W && screenY >= closeY && screenY <= closeY + CLOSE_H)) {
            dismiss();
            return;
        }
        if (screenX < listX || screenX > listX + listW || screenY < listY || screenY > listY + listH) {
            return;
        }

        int row = options.size() - 1 - ((screenY - listY) / (ROW_H + ROW_GAP));
        if (row < 0 || row >= options.size()) {
            return;
        }

        int rowY = listY + (options.size() - 1 - row) * (ROW_H + ROW_GAP);
        if (screenY < rowY || screenY > rowY + ROW_H) {
            return;
        }
        selectedBarItemId = options.get(row).barItemId;
        visible = false;
    }

    public int consumeSelectedBarItemId() {
        int selected = selectedBarItemId;
        selectedBarItemId = -1;
        return selected;
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
        font.draw(batch, "Smelt", panelX + PAD, panelY + PANEL_H - 12);

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
                String right = "Lv " + option.levelRequirement
                    + "  XP " + formatTenths(option.smithingXpTenths)
                    + "  " + option.successPercent + "%";
                String ingredients = formatIngredients(option);
                font.setColor(0.96f, 0.94f, 0.90f, 1f);
                font.draw(batch, option.barName, listX + 8, rowY + 29);
                font.setColor(0.84f, 0.82f, 0.74f, 1f);
                font.draw(batch, ingredients, listX + 8, rowY + 14);
                float textX = Math.max(listX + 240f, listX + listW - right.length() * 5.4f);
                font.draw(batch, right, textX, rowY + 29);
            }
        }
        font.setColor(Color.WHITE);
        font.getData().setScale(1f);
        batch.end();
    }

    private String formatIngredients(Option option) {
        List<String> pieces = new ArrayList<>();
        if (option.oreItemIdA > 0 && option.oreQtyA > 0) {
            pieces.add(option.oreQtyA + "x " + oreName(option.oreItemIdA));
        }
        if (option.oreItemIdB > 0 && option.oreQtyB > 0) {
            pieces.add(option.oreQtyB + "x " + oreName(option.oreItemIdB));
        }
        if (option.coalRequired > 0) {
            pieces.add(option.coalRequired + "x Coal");
        }
        if (pieces.isEmpty()) {
            return "No ingredients";
        }
        return String.join(" + ", pieces);
    }

    private String oreName(int itemId) {
        return switch (itemId) {
            case 436 -> "Copper ore";
            case 438 -> "Tin ore";
            case 440 -> "Iron ore";
            case 442 -> "Silver ore";
            case 444 -> "Gold ore";
            case 447 -> "Mithril ore";
            case 449 -> "Adamantite ore";
            case 451 -> "Runite ore";
            default -> "Ore " + itemId;
        };
    }

    private String formatTenths(int tenths) {
        int whole = Math.max(0, tenths / 10);
        int frac = Math.max(0, tenths % 10);
        return whole + "." + frac;
    }
}
