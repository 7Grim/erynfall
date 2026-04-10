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

public class ShopUI {

    public static class StockRow {
        public final int itemId;
        public final String itemName;
        public final int quantity;
        public final int price;
        public final int flags;

        public StockRow(int itemId, String itemName, int quantity, int price, int flags) {
            this.itemId = itemId;
            this.itemName = itemName == null ? "" : itemName;
            this.quantity = quantity;
            this.price = price;
            this.flags = flags;
        }
    }

    public static class PendingPurchase {
        public final int npcId;
        public final int itemId;
        public final int quantity;

        public PendingPurchase(int npcId, int itemId, int quantity) {
            this.npcId = npcId;
            this.itemId = itemId;
            this.quantity = quantity;
        }
    }

    private static final int PANEL_W = 560;
    private static final int PANEL_H = 404;
    private static final int HEADER_H = 36;
    private static final int PAD = 12;
    private static final int CLOSE_W = 26;
    private static final int CLOSE_H = 22;
    private static final int ROW_H = 34;
    private static final int ROW_GAP = 6;

    private static final int[] QTY_PRESETS = {1, 5, 10, 50};

    private static final Color OVERLAY = new Color(0f, 0f, 0f, 0.62f);
    private static final Color PANEL_BG = new Color(0.14f, 0.12f, 0.09f, 0.98f);
    private static final Color PANEL_BORDER = new Color(0.78f, 0.67f, 0.38f, 1f);
    private static final Color HEADER_BG = new Color(0.24f, 0.19f, 0.12f, 1f);
    private static final Color CLOSE_BG = new Color(0.45f, 0.10f, 0.10f, 1f);

    private boolean visible;
    private int npcId = -1;
    private String shopName = "";
    private List<StockRow> stock = List.of();
    private int selectedItemId = -1;
    private int selectedQty = 1;
    private PendingPurchase pendingPurchase;

    private int panelX;
    private int panelY;
    private int closeX;
    private int closeY;
    private int listX;
    private int listY;
    private int listW;
    private int listH;

    public void show(int npcId, String shopName, List<StockRow> stock) {
        this.npcId = npcId;
        this.shopName = shopName == null ? "Shop" : shopName;
        this.stock = stock == null ? List.of() : new ArrayList<>(stock);
        this.visible = true;
        this.pendingPurchase = null;
        this.selectedQty = 1;
        if (this.stock.isEmpty()) {
            this.selectedItemId = -1;
        } else if (selectedItemId < 0 || this.stock.stream().noneMatch(row -> row.itemId == selectedItemId)) {
            this.selectedItemId = this.stock.get(0).itemId;
        }
    }

    public void dismiss() {
        visible = false;
        npcId = -1;
        shopName = "";
        stock = List.of();
        selectedItemId = -1;
        pendingPurchase = null;
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

        for (int i = 0; i < stock.size(); i++) {
            int rowY = listY + (stock.size() - 1 - i) * (ROW_H + ROW_GAP);
            if (rowY + ROW_H > listY + listH) {
                continue;
            }
            if (screenX >= listX && screenX <= listX + listW && screenY >= rowY && screenY <= rowY + ROW_H) {
                selectedItemId = stock.get(i).itemId;
                return;
            }
        }

        int qtyBarY = panelY + 48;
        int buttonW = 44;
        int buttonH = 22;
        int startX = listX;
        for (int i = 0; i < QTY_PRESETS.length; i++) {
            int bx = startX + i * (buttonW + 6);
            if (screenX >= bx && screenX <= bx + buttonW && screenY >= qtyBarY && screenY <= qtyBarY + buttonH) {
                selectedQty = QTY_PRESETS[i];
                return;
            }
        }

        int buyX = listX + listW - 102;
        int buyY = panelY + 44;
        int buyW = 96;
        int buyH = 28;
        if (screenX >= buyX && screenX <= buyX + buyW && screenY >= buyY && screenY <= buyY + buyH) {
            if (selectedItemId > 0 && selectedQty > 0 && npcId >= 0) {
                pendingPurchase = new PendingPurchase(npcId, selectedItemId, selectedQty);
            }
        }
    }

    public PendingPurchase consumePendingPurchase() {
        PendingPurchase out = pendingPurchase;
        pendingPurchase = null;
        return out;
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
        listY = panelY + 84;
        listW = PANEL_W - PAD * 2;
        listH = PANEL_H - HEADER_H - PAD - 82;

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

        int qtyBarY = panelY + 48;
        int buttonW = 44;
        int buttonH = 22;
        for (int i = 0; i < QTY_PRESETS.length; i++) {
            int bx = listX + i * (buttonW + 6);
            boolean active = selectedQty == QTY_PRESETS[i];
            shapeRenderer.setColor(active ? 0.45f : 0.26f, active ? 0.34f : 0.22f, active ? 0.14f : 0.12f, 1f);
            shapeRenderer.rect(bx, qtyBarY, buttonW, buttonH);
        }

        int buyX = listX + listW - 102;
        int buyY = panelY + 44;
        int buyW = 96;
        int buyH = 28;
        shapeRenderer.setColor(0.22f, 0.42f, 0.22f, 1f);
        shapeRenderer.rect(buyX, buyY, buyW, buyH);

        for (int i = 0; i < stock.size(); i++) {
            int rowY = listY + (stock.size() - 1 - i) * (ROW_H + ROW_GAP);
            if (rowY + ROW_H > listY + listH) {
                continue;
            }
            StockRow row = stock.get(i);
            boolean selected = row.itemId == selectedItemId;
            boolean hover = mouseX >= listX && mouseX <= listX + listW
                && mouseY >= rowY && mouseY <= rowY + ROW_H;
            shapeRenderer.setColor(selected ? 0.34f : hover ? 0.26f : 0.20f,
                selected ? 0.27f : hover ? 0.21f : 0.16f,
                selected ? 0.14f : hover ? 0.12f : 0.10f,
                1f);
            shapeRenderer.rect(listX, rowY, listW, ROW_H);
            ItemIconRenderer.drawItemIcon(shapeRenderer, listX + 8, rowY + 3, row.itemId);
        }
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(PANEL_BORDER);
        shapeRenderer.rect(panelX, panelY, PANEL_W, PANEL_H);
        shapeRenderer.rect(closeX, closeY, CLOSE_W, CLOSE_H);
        for (int i = 0; i < stock.size(); i++) {
            int rowY = listY + (stock.size() - 1 - i) * (ROW_H + ROW_GAP);
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
        font.draw(batch, shopName, panelX + PAD, panelY + PANEL_H - 12);

        font.getData().setScale(0.66f);
        font.setColor(0.86f, 0.84f, 0.78f, 1f);
        font.draw(batch, "Buy quantity:", listX, qtyBarY + 34);
        for (int i = 0; i < QTY_PRESETS.length; i++) {
            int bx = listX + i * (buttonW + 6);
            font.setColor(selectedQty == QTY_PRESETS[i] ? 1f : 0.82f,
                selectedQty == QTY_PRESETS[i] ? 0.92f : 0.78f,
                selectedQty == QTY_PRESETS[i] ? 0.58f : 0.70f,
                1f);
            font.draw(batch, Integer.toString(QTY_PRESETS[i]), bx + 16, qtyBarY + 15);
        }

        font.setColor(0.92f, 0.98f, 0.92f, 1f);
        font.draw(batch, "Buy", buyX + 34, buyY + 19);

        for (int i = 0; i < stock.size(); i++) {
            int rowY = listY + (stock.size() - 1 - i) * (ROW_H + ROW_GAP);
            if (rowY + ROW_H > listY + listH) {
                continue;
            }
            StockRow row = stock.get(i);
            String right = "Stock " + row.quantity + "  Price " + row.price;
            font.setColor(0.96f, 0.94f, 0.90f, 1f);
            font.draw(batch, row.itemName, listX + 44, rowY + 22);
            font.setColor(0.84f, 0.82f, 0.74f, 1f);
            font.draw(batch, right, listX + listW - 170, rowY + 22);
        }

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }
}
