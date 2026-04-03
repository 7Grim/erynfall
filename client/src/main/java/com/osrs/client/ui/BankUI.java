package com.osrs.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.osrs.client.network.ClientPacketHandler;

import java.util.List;

public class BankUI {

    private static final int PANEL_W = 760;
    private static final int PANEL_H = 500;
    private static final int HEADER_H = 40;
    private static final int PADDING = 12;
    private static final int CLOSE_W = 26;
    private static final int CLOSE_H = 22;

    private static final int BANK_COLS = 8;
    private static final int INV_COLS = 4;
    private static final int INV_ROWS = 7;
    private static final int CELL = 42;
    private static final int CELL_GAP = 4;

    private static final int AMOUNT_MODE_1 = 1;
    private static final int AMOUNT_MODE_5 = 5;
    private static final int AMOUNT_MODE_10 = 10;
    private static final int AMOUNT_MODE_ALL = Integer.MAX_VALUE;

    private final GlyphLayout glyph = new GlyphLayout();

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    private int closeX;
    private int closeY;

    private int bankGridX;
    private int bankGridY;
    private int bankGridW;
    private int bankGridH;
    private int bankVisibleRows;

    private int invGridX;
    private int invGridY;
    private int invGridW;
    private int invGridH;

    private final int[] amountValues = {AMOUNT_MODE_1, AMOUNT_MODE_5, AMOUNT_MODE_10, AMOUNT_MODE_ALL};
    private final String[] amountLabels = {"1", "5", "10", "All"};
    private final int[] amountButtonX = new int[4];
    private final int[] amountButtonY = new int[4];
    private final int[] amountButtonW = new int[4];
    private final int[] amountButtonH = new int[4];

    private int selectedAmount = AMOUNT_MODE_1;
    private int scrollRows = 0;

    public void render(ShapeRenderer shapeRenderer,
                       SpriteBatch batch,
                       BitmapFont font,
                       int screenW,
                       int screenH,
                       Matrix4 projection,
                       int mouseX,
                       int mouseY,
                       int capacity,
                       List<ClientPacketHandler.BankSlotSnapshot> slots,
                       ClientPacketHandler handler,
                       int draggingInventorySlot,
                       int draggingBankSlot,
                       int dragMouseX,
                       int dragMouseY) {
        updateLayout(screenW, screenH);

        int maxBankRow = getMaxBankRow(slots);
        int maxScroll = Math.max(0, maxBankRow - bankVisibleRows + 1);
        if (scrollRows > maxScroll) {
            scrollRows = maxScroll;
        }

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(projection);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.60f);
        shapeRenderer.rect(0, 0, screenW, screenH);

        shapeRenderer.setColor(0.15f, 0.12f, 0.09f, 0.98f);
        shapeRenderer.rect(panelX, panelY, panelW, panelH);
        shapeRenderer.setColor(0.21f, 0.16f, 0.10f, 1f);
        shapeRenderer.rect(panelX + 1, panelY + panelH - HEADER_H, panelW - 2, HEADER_H - 1);

        shapeRenderer.setColor(0.45f, 0.08f, 0.08f, 1f);
        shapeRenderer.rect(closeX, closeY, CLOSE_W, CLOSE_H);

        shapeRenderer.setColor(0.10f, 0.09f, 0.07f, 1f);
        shapeRenderer.rect(bankGridX, bankGridY, bankGridW, bankGridH);
        shapeRenderer.rect(invGridX, invGridY, invGridW, invGridH);

        for (int i = 0; i < amountValues.length; i++) {
            boolean selected = selectedAmount == amountValues[i];
            shapeRenderer.setColor(selected ? 0.32f : 0.20f, selected ? 0.28f : 0.16f, selected ? 0.13f : 0.10f, 1f);
            shapeRenderer.rect(amountButtonX[i], amountButtonY[i], amountButtonW[i], amountButtonH[i]);
        }

        for (int row = 0; row < bankVisibleRows; row++) {
            for (int col = 0; col < BANK_COLS; col++) {
                int x = bankGridX + col * (CELL + CELL_GAP);
                int y = bankGridY + bankGridH - CELL - row * (CELL + CELL_GAP);
                boolean hover = mouseX >= x && mouseX <= x + CELL && mouseY >= y && mouseY <= y + CELL;
                shapeRenderer.setColor(hover ? 0.24f : 0.19f, hover ? 0.20f : 0.16f, hover ? 0.12f : 0.10f, 1f);
                shapeRenderer.rect(x, y, CELL, CELL);
            }
        }

        for (int row = 0; row < INV_ROWS; row++) {
            for (int col = 0; col < INV_COLS; col++) {
                int x = invGridX + col * (CELL + CELL_GAP);
                int y = invGridY + invGridH - CELL - row * (CELL + CELL_GAP);
                boolean hover = mouseX >= x && mouseX <= x + CELL && mouseY >= y && mouseY <= y + CELL;
                shapeRenderer.setColor(hover ? 0.23f : 0.18f, hover ? 0.19f : 0.15f, hover ? 0.11f : 0.09f, 1f);
                shapeRenderer.rect(x, y, CELL, CELL);
            }
        }
        if (slots != null) {
            for (ClientPacketHandler.BankSlotSnapshot slot : slots) {
                if (slot.slot == draggingBankSlot) continue;
                int slotIndex = slot.slot;
                int row = slotIndex / BANK_COLS;
                int col = slotIndex % BANK_COLS;
                int localRow = row - scrollRows;
                if (localRow < 0 || localRow >= bankVisibleRows) {
                    continue;
                }
                int x = bankGridX + col * (CELL + CELL_GAP);
                int y = bankGridY + bankGridH - CELL - localRow * (CELL + CELL_GAP);
                ItemIconRenderer.drawItemIcon(shapeRenderer, x, y, slot.itemId);
            }
        }
        if (handler != null) {
            for (int i = 0; i < 28; i++) {
                if (i == draggingInventorySlot) continue;
                int itemId = handler.getInventoryItemId(i);
                if (itemId <= 0) continue;
                int row = i / INV_COLS;
                int col = i % INV_COLS;
                int x = invGridX + col * (CELL + CELL_GAP);
                int y = invGridY + invGridH - CELL - row * (CELL + CELL_GAP);
                ItemIconRenderer.drawItemIcon(shapeRenderer, x, y, itemId);
            }
            if (draggingInventorySlot >= 0 && draggingInventorySlot < 28) {
                int draggingItemId = handler.getInventoryItemId(draggingInventorySlot);
                if (draggingItemId > 0) {
                    float dragX = dragMouseX - CELL / 2f;
                    float dragY = dragMouseY - CELL / 2f + 6f;
                    ItemIconRenderer.drawItemIcon(shapeRenderer, dragX, dragY, draggingItemId);
                }
            }
        }
        if (slots != null && draggingBankSlot >= 0) {
            for (ClientPacketHandler.BankSlotSnapshot slot : slots) {
                if (slot.slot != draggingBankSlot) continue;
                if (slot.itemId <= 0 || slot.quantity <= 0) break;
                float dragX = dragMouseX - CELL / 2f;
                float dragY = dragMouseY - CELL / 2f + 6f;
                ItemIconRenderer.drawItemIcon(shapeRenderer, dragX, dragY, slot.itemId);
                break;
            }
        }
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.78f, 0.67f, 0.38f, 1f);
        shapeRenderer.rect(panelX, panelY, panelW, panelH);
        shapeRenderer.rect(closeX, closeY, CLOSE_W, CLOSE_H);
        shapeRenderer.rect(bankGridX, bankGridY, bankGridW, bankGridH);
        shapeRenderer.rect(invGridX, invGridY, invGridW, invGridH);
        for (int i = 0; i < amountValues.length; i++) {
            shapeRenderer.rect(amountButtonX[i], amountButtonY[i], amountButtonW[i], amountButtonH[i]);
        }
        shapeRenderer.end();

        batch.setProjectionMatrix(projection);
        batch.begin();
        font.getData().setScale(0.9f);
        font.setColor(1f, 0.92f, 0.58f, 1f);
        font.draw(batch, "The Bank of Erynfall", panelX + PADDING, panelY + panelH - 12);
        font.getData().setScale(0.7f);
        font.setColor(0.82f, 0.82f, 0.82f, 1f);
        font.draw(batch, "Used: " + (slots == null ? 0 : slots.size()) + " / " + capacity,
            panelX + panelW - 180, panelY + panelH - 13);

        font.getData().setScale(0.86f);
        glyph.setText(font, "X");
        font.setColor(Color.WHITE);
        font.draw(batch, "X", closeX + (CLOSE_W - glyph.width) / 2f, closeY + CLOSE_H - 5);

        font.getData().setScale(0.66f);
        font.setColor(0.95f, 0.93f, 0.84f, 1f);
        font.draw(batch, "Bank", bankGridX, bankGridY + bankGridH + 13);
        font.draw(batch, "Inventory", invGridX, invGridY + invGridH + 13);

        for (int i = 0; i < amountValues.length; i++) {
            font.setColor(selectedAmount == amountValues[i]
                ? new Color(1f, 0.95f, 0.68f, 1f)
                : new Color(0.82f, 0.80f, 0.75f, 1f));
            glyph.setText(font, amountLabels[i]);
            font.draw(batch, amountLabels[i],
                amountButtonX[i] + (amountButtonW[i] - glyph.width) / 2f,
                amountButtonY[i] + amountButtonH[i] - 5f);
        }

        if (slots != null) {
            font.getData().setScale(0.58f);
            for (ClientPacketHandler.BankSlotSnapshot slot : slots) {
                if (slot.slot == draggingBankSlot) continue;
                int slotIndex = slot.slot;
                int row = slotIndex / BANK_COLS;
                int col = slotIndex % BANK_COLS;
                int localRow = row - scrollRows;
                if (localRow < 0 || localRow >= bankVisibleRows) {
                    continue;
                }
                int x = bankGridX + col * (CELL + CELL_GAP);
                int y = bankGridY + bankGridH - CELL - localRow * (CELL + CELL_GAP);

                String itemName = (slot.itemName == null || slot.itemName.isBlank())
                    ? ("Item " + slot.itemId)
                    : slot.itemName;
                glyph.setText(font, itemName);
                String truncated = itemName;
                if (glyph.width > CELL - 6) {
                    int maxChars = Math.max(3, (int) ((CELL - 8) / 5f));
                    truncated = itemName.substring(0, Math.min(itemName.length(), maxChars));
                }
                font.setColor(0.92f, 0.90f, 0.84f, 1f);
                font.draw(batch, truncated, x + 3, y + CELL - 4);
                font.setColor(0.72f, 0.87f, 0.78f, 1f);
                font.draw(batch, formatQuantity(slot.quantity), x + 3, y + 11);
            }
        }

        if (handler != null) {
            font.getData().setScale(0.62f);
            for (int i = 0; i < 28; i++) {
                if (i == draggingInventorySlot) continue;
                int itemId = handler.getInventoryItemId(i);
                if (itemId <= 0) continue;
                int qty = handler.getInventoryQuantity(i);
                String name = handler.getInventoryName(i);
                if (name == null || name.isBlank()) name = "Item " + itemId;

                int row = i / INV_COLS;
                int col = i % INV_COLS;
                int x = invGridX + col * (CELL + CELL_GAP);
                int y = invGridY + invGridH - CELL - row * (CELL + CELL_GAP);

                glyph.setText(font, name);
                String truncated = name;
                if (glyph.width > CELL - 6) {
                    int maxChars = Math.max(3, (int) ((CELL - 8) / 5f));
                    truncated = name.substring(0, Math.min(name.length(), maxChars));
                }
                font.setColor(0.92f, 0.90f, 0.84f, 1f);
                font.draw(batch, truncated, x + 3, y + CELL - 4);
                font.setColor(0.79f, 0.86f, 0.97f, 1f);
                font.draw(batch, formatQuantity(qty), x + 3, y + 11);
            }
        }

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    public int getSelectedAmount() {
        return selectedAmount;
    }

    public int getBankSlotAt(int mouseX, int mouseY, List<ClientPacketHandler.BankSlotSnapshot> slots) {
        if (!isBankCellHit(mouseX, mouseY)) return -1;
        int col = (mouseX - bankGridX) / (CELL + CELL_GAP);
        int rowFromTop = (bankGridY + bankGridH - mouseY - 1) / (CELL + CELL_GAP);
        if (col < 0 || col >= BANK_COLS || rowFromTop < 0 || rowFromTop >= bankVisibleRows) return -1;
        int slotIndex = (scrollRows + rowFromTop) * BANK_COLS + col;
        if (slots == null) return -1;
        for (ClientPacketHandler.BankSlotSnapshot slot : slots) {
            if (slot.slot == slotIndex && slot.itemId > 0 && slot.quantity > 0) {
                return slotIndex;
            }
        }
        return -1;
    }

    public int getBankCellSlotAt(int mouseX, int mouseY) {
        if (!isBankCellHit(mouseX, mouseY)) return -1;
        int col = (mouseX - bankGridX) / (CELL + CELL_GAP);
        int rowFromTop = (bankGridY + bankGridH - mouseY - 1) / (CELL + CELL_GAP);
        if (col < 0 || col >= BANK_COLS || rowFromTop < 0 || rowFromTop >= bankVisibleRows) return -1;
        return (scrollRows + rowFromTop) * BANK_COLS + col;
    }

    public int getInventorySlotAt(int mouseX, int mouseY, ClientPacketHandler handler) {
        if (!isInventoryCellHit(mouseX, mouseY) || handler == null) return -1;
        int col = (mouseX - invGridX) / (CELL + CELL_GAP);
        int rowFromTop = (invGridY + invGridH - mouseY - 1) / (CELL + CELL_GAP);
        if (col < 0 || col >= INV_COLS || rowFromTop < 0 || rowFromTop >= INV_ROWS) return -1;
        int slot = rowFromTop * INV_COLS + col;
        if (slot < 0 || slot >= 28) return -1;
        return handler.getInventoryItemId(slot) > 0 ? slot : -1;
    }

    public int getInventoryCellSlotAt(int mouseX, int mouseY) {
        if (!isOverInventoryGrid(mouseX, mouseY)) return -1;
        int localX = mouseX - invGridX;
        int localY = invGridY + invGridH - mouseY - 1;
        int col = localX / (CELL + CELL_GAP);
        int rowFromTop = localY / (CELL + CELL_GAP);
        if (col < 0 || col >= INV_COLS || rowFromTop < 0 || rowFromTop >= INV_ROWS) return -1;
        int cellLocalX = localX - col * (CELL + CELL_GAP);
        int cellLocalY = localY - rowFromTop * (CELL + CELL_GAP);
        if (cellLocalX >= CELL || cellLocalY >= CELL) return -1;
        int slot = rowFromTop * INV_COLS + col;
        return (slot >= 0 && slot < 28) ? slot : -1;
    }

    public boolean clickAmountButton(int mouseX, int mouseY) {
        for (int i = 0; i < amountValues.length; i++) {
            if (mouseX >= amountButtonX[i] && mouseX <= amountButtonX[i] + amountButtonW[i]
                && mouseY >= amountButtonY[i] && mouseY <= amountButtonY[i] + amountButtonH[i]) {
                selectedAmount = amountValues[i];
                return true;
            }
        }
        return false;
    }

    public boolean isOverBankGrid(int mouseX, int mouseY) {
        return mouseX >= bankGridX && mouseX <= bankGridX + bankGridW
            && mouseY >= bankGridY && mouseY <= bankGridY + bankGridH;
    }

    public boolean isOverInventoryGrid(int mouseX, int mouseY) {
        return mouseX >= invGridX && mouseX <= invGridX + invGridW
            && mouseY >= invGridY && mouseY <= invGridY + invGridH;
    }

    public boolean isInventoryCellHit(int mouseX, int mouseY) {
        if (!isOverInventoryGrid(mouseX, mouseY)) return false;
        int localX = mouseX - invGridX;
        int localY = invGridY + invGridH - mouseY - 1;
        int col = localX / (CELL + CELL_GAP);
        int row = localY / (CELL + CELL_GAP);
        if (col < 0 || col >= INV_COLS || row < 0 || row >= INV_ROWS) return false;
        int cellLocalX = localX - col * (CELL + CELL_GAP);
        int cellLocalY = localY - row * (CELL + CELL_GAP);
        return cellLocalX < CELL && cellLocalY < CELL;
    }

    public boolean isBankCellHit(int mouseX, int mouseY) {
        if (!isOverBankGrid(mouseX, mouseY)) return false;
        int localX = mouseX - bankGridX;
        int localY = bankGridY + bankGridH - mouseY - 1;
        int col = localX / (CELL + CELL_GAP);
        int row = localY / (CELL + CELL_GAP);
        if (col < 0 || col >= BANK_COLS || row < 0 || row >= bankVisibleRows) return false;
        int cellLocalX = localX - col * (CELL + CELL_GAP);
        int cellLocalY = localY - row * (CELL + CELL_GAP);
        return cellLocalX < CELL && cellLocalY < CELL;
    }

    public boolean isOver(int mouseX, int mouseY) {
        return mouseX >= panelX && mouseX <= panelX + panelW
            && mouseY >= panelY && mouseY <= panelY + panelH;
    }

    public boolean isCloseButtonHit(int mouseX, int mouseY) {
        return mouseX >= closeX && mouseX <= closeX + CLOSE_W && mouseY >= closeY && mouseY <= closeY + CLOSE_H;
    }

    public boolean handleScroll(int amount, List<ClientPacketHandler.BankSlotSnapshot> slots) {
        int maxBankRow = getMaxBankRow(slots);
        int maxScroll = Math.max(0, maxBankRow - bankVisibleRows + 1);
        if (maxScroll <= 0) {
            scrollRows = 0;
            return false;
        }
        int before = scrollRows;
        scrollRows = Math.max(0, Math.min(maxScroll, scrollRows + amount));
        return before != scrollRows;
    }

    private int getMaxBankRow(List<ClientPacketHandler.BankSlotSnapshot> slots) {
        if (slots == null || slots.isEmpty()) return 0;
        int maxSlot = 0;
        for (ClientPacketHandler.BankSlotSnapshot slot : slots) {
            if (slot.slot > maxSlot) {
                maxSlot = slot.slot;
            }
        }
        return maxSlot / BANK_COLS;
    }

    private String formatQuantity(long quantity) {
        if (quantity >= 1_000_000_000L) {
            return String.format("%.1fb", quantity / 1_000_000_000f);
        }
        if (quantity >= 1_000_000L) {
            return String.format("%.1fm", quantity / 1_000_000f);
        }
        if (quantity >= 1_000L) {
            return String.format("%.1fk", quantity / 1_000f);
        }
        return Long.toString(quantity);
    }

    private void updateLayout(int screenW, int screenH) {
        panelW = Math.min(PANEL_W, Math.max(580, screenW - 32));
        panelH = Math.min(PANEL_H, Math.max(340, screenH - 48));
        panelX = (screenW - panelW) / 2;
        panelY = (screenH - panelH) / 2;

        closeX = panelX + panelW - PADDING - CLOSE_W;
        closeY = panelY + panelH - 8 - CLOSE_H;

        int amountBandH = 28;
        int contentTop = panelY + panelH - HEADER_H - PADDING;
        int contentBottom = panelY + PADDING;

        int amountY = contentTop - amountBandH;
        int buttonW = 48;
        int buttonH = 20;
        int btnX = panelX + PADDING;
        for (int i = 0; i < amountValues.length; i++) {
            amountButtonX[i] = btnX;
            amountButtonY[i] = amountY;
            amountButtonW[i] = buttonW;
            amountButtonH[i] = buttonH;
            btnX += buttonW + 6;
        }

        int gridsTop = amountY - 8;
        int gridsBottom = contentBottom;
        int gridsHeight = Math.max(120, gridsTop - gridsBottom);

        int rightPaneW = INV_COLS * (CELL + CELL_GAP) - CELL_GAP;
        int gridGap = 14;
        bankGridX = panelX + PADDING;
        invGridX = panelX + panelW - PADDING - rightPaneW;
        bankGridY = gridsBottom;
        invGridY = gridsBottom;
        invGridW = rightPaneW;
        invGridH = Math.min(gridsHeight, INV_ROWS * (CELL + CELL_GAP) - CELL_GAP);
        bankGridW = Math.max(120, invGridX - bankGridX - gridGap);
        bankGridH = gridsHeight;
        bankVisibleRows = Math.max(1, bankGridH / (CELL + CELL_GAP));
    }
}
