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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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

    private static final int ALL_TAB = -1;
    private static final int MAX_CUSTOM_TABS = 9;
    private static final int TAB_W = 44;
    private static final int TAB_H = 26;
    private static final int TAB_GAP = 6;

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

    private int selectedTab = ALL_TAB;
    private int allTabX;
    private int allTabY;
    private int allTabW;
    private int allTabH;
    private final int[] customTabX = new int[MAX_CUSTOM_TABS];
    private final int[] customTabY = new int[MAX_CUSTOM_TABS];
    private final int[] customTabW = new int[MAX_CUSTOM_TABS];
    private final int[] customTabH = new int[MAX_CUSTOM_TABS];
    private int plusTabX;
    private int plusTabY;
    private int plusTabW;
    private int plusTabH;
    private int searchBoxX;
    private int searchBoxY;
    private int searchBoxW;
    private int searchBoxH;
    private boolean searchFocused = false;
    private String searchQuery = "";

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
        updateLayout(screenW, screenH, slots);
        clampSelectedTab(slots);

        List<ClientPacketHandler.BankSlotSnapshot> displaySlots = getDisplaySlots(slots);
        boolean packedDisplay = selectedTab != ALL_TAB || isSearchActive();
        int maxBankRow = getMaxBankRow(slots, displaySlots);
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

        shapeRenderer.setColor(0.18f, 0.14f, 0.10f, 1f);
        shapeRenderer.rect(allTabX, allTabY, allTabW, allTabH);

        for (int i = 0; i < MAX_CUSTOM_TABS; i++) {
            if (customTabW[i] <= 0) continue;
            shapeRenderer.setColor(0.18f, 0.14f, 0.10f, 1f);
            shapeRenderer.rect(customTabX[i], customTabY[i], customTabW[i], customTabH[i]);
        }
        if (plusTabW > 0) {
            shapeRenderer.setColor(0.16f, 0.14f, 0.11f, 1f);
            shapeRenderer.rect(plusTabX, plusTabY, plusTabW, plusTabH);
        }

        shapeRenderer.setColor(searchFocused ? 0.24f : 0.16f, searchFocused ? 0.20f : 0.14f,
            searchFocused ? 0.11f : 0.10f, 1f);
        shapeRenderer.rect(searchBoxX, searchBoxY, searchBoxW, searchBoxH);

        if (selectedTab == ALL_TAB) {
            shapeRenderer.setColor(0.86f, 0.72f, 0.34f, 1f);
            shapeRenderer.rect(allTabX + 3, allTabY + 3, allTabW - 6, allTabH - 6);
        } else if (selectedTab >= 1 && selectedTab <= MAX_CUSTOM_TABS) {
            int idx = selectedTab - 1;
            if (customTabW[idx] > 0) {
                shapeRenderer.setColor(0.86f, 0.72f, 0.34f, 1f);
                shapeRenderer.rect(customTabX[idx] + 3, customTabY[idx] + 3, customTabW[idx] - 6, customTabH[idx] - 6);
            }
        }

        List<Integer> visibleTabs = getVisibleCustomTabs(slots);
        for (int tab : visibleTabs) {
            int idx = tab - 1;
            ClientPacketHandler.BankSlotSnapshot iconSlot = getTabIconSlot(slots, tab);
            if (iconSlot != null && customTabW[idx] > 0) {
                ItemIconRenderer.drawItemIcon(shapeRenderer, customTabX[idx] + 4f, customTabY[idx] - 1f, iconSlot.itemId);
            }
        }

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

        if (!packedDisplay) {
            for (ClientPacketHandler.BankSlotSnapshot slot : displaySlots) {
                if (slot.slot == draggingBankSlot) continue;
                int slotIndex = slot.slot;
                int row = slotIndex / BANK_COLS;
                int col = slotIndex % BANK_COLS;
                int localRow = row - scrollRows;
                if (localRow < 0 || localRow >= bankVisibleRows) continue;
                int x = bankGridX + col * (CELL + CELL_GAP);
                int y = bankGridY + bankGridH - CELL - localRow * (CELL + CELL_GAP);
                ItemIconRenderer.drawItemIcon(shapeRenderer, x, y, slot.itemId);
            }
        } else {
            for (int i = 0; i < displaySlots.size(); i++) {
                ClientPacketHandler.BankSlotSnapshot slot = displaySlots.get(i);
                if (slot.slot == draggingBankSlot) continue;
                int packedRow = i / BANK_COLS;
                int col = i % BANK_COLS;
                int localRow = packedRow - scrollRows;
                if (localRow < 0 || localRow >= bankVisibleRows) continue;
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
                    ItemIconRenderer.drawItemIcon(shapeRenderer, dragMouseX - CELL / 2f, dragMouseY - CELL / 2f + 6f, draggingItemId);
                }
            }
        }

        if (slots != null && draggingBankSlot >= 0) {
            for (ClientPacketHandler.BankSlotSnapshot slot : slots) {
                if (slot.slot != draggingBankSlot) continue;
                if (slot.itemId <= 0 || slot.quantity <= 0) break;
                ItemIconRenderer.drawItemIcon(shapeRenderer, dragMouseX - CELL / 2f, dragMouseY - CELL / 2f + 6f, slot.itemId);
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
        shapeRenderer.rect(allTabX, allTabY, allTabW, allTabH);
        for (int i = 0; i < MAX_CUSTOM_TABS; i++) {
            if (customTabW[i] > 0) {
                shapeRenderer.rect(customTabX[i], customTabY[i], customTabW[i], customTabH[i]);
            }
        }
        if (plusTabW > 0) {
            shapeRenderer.rect(plusTabX, plusTabY, plusTabW, plusTabH);
        }
        shapeRenderer.rect(searchBoxX, searchBoxY, searchBoxW, searchBoxH);
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

        font.getData().setScale(0.76f);
        font.setColor(0.95f, 0.93f, 0.84f, 1f);
        glyph.setText(font, "∞");
        font.draw(batch, "∞", allTabX + (allTabW - glyph.width) / 2f, allTabY + TAB_H - 7f);

        if (plusTabW > 0) {
            font.getData().setScale(0.8f);
            glyph.setText(font, "+");
            font.setColor(0.94f, 0.92f, 0.84f, 1f);
            font.draw(batch, "+", plusTabX + (plusTabW - glyph.width) / 2f, plusTabY + TAB_H - 7f);
        }

        font.getData().setScale(0.62f);
        String searchLabel = searchQuery == null || searchQuery.isEmpty() ? "Search..." : searchQuery;
        font.setColor((searchQuery == null || searchQuery.isEmpty())
            ? new Color(0.70f, 0.66f, 0.58f, 1f)
            : new Color(0.94f, 0.92f, 0.84f, 1f));
        font.draw(batch, searchLabel, searchBoxX + 8, searchBoxY + searchBoxH - 6);

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

        font.getData().setScale(0.58f);
        if (!packedDisplay) {
            for (ClientPacketHandler.BankSlotSnapshot slot : displaySlots) {
                if (slot.slot == draggingBankSlot) continue;
                int row = slot.slot / BANK_COLS;
                int col = slot.slot % BANK_COLS;
                int localRow = row - scrollRows;
                if (localRow < 0 || localRow >= bankVisibleRows) continue;
                int x = bankGridX + col * (CELL + CELL_GAP);
                int y = bankGridY + bankGridH - CELL - localRow * (CELL + CELL_GAP);
                drawSlotText(batch, font, x, y, slot.itemName, slot.itemId, slot.quantity);
            }
        } else {
            for (int i = 0; i < displaySlots.size(); i++) {
                ClientPacketHandler.BankSlotSnapshot slot = displaySlots.get(i);
                if (slot.slot == draggingBankSlot) continue;
                int packedRow = i / BANK_COLS;
                int col = i % BANK_COLS;
                int localRow = packedRow - scrollRows;
                if (localRow < 0 || localRow >= bankVisibleRows) continue;
                int x = bankGridX + col * (CELL + CELL_GAP);
                int y = bankGridY + bankGridH - CELL - localRow * (CELL + CELL_GAP);
                drawSlotText(batch, font, x, y, slot.itemName, slot.itemId, slot.quantity);
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
                drawSlotText(batch, font, x, y, name, itemId, qty);
            }
        }

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    public int getSelectedAmount() {
        return selectedAmount;
    }

    public int getSelectedTab() {
        return selectedTab;
    }

    public void setSelectedTab(int tab) {
        if (tab < ALL_TAB) {
            selectedTab = ALL_TAB;
        } else if (tab > MAX_CUSTOM_TABS) {
            selectedTab = MAX_CUSTOM_TABS;
        } else {
            selectedTab = tab;
        }
        scrollRows = 0;
    }

    public void resetSelectedTab() {
        selectedTab = ALL_TAB;
        scrollRows = 0;
        searchQuery = "";
        searchFocused = false;
    }

    public boolean isAllTabSelected() {
        return selectedTab == ALL_TAB;
    }

    public boolean isSearchFocused() {
        return searchFocused;
    }

    public boolean isSearchActive() {
        return searchQuery != null && !searchQuery.isBlank();
    }

    public String getSearchQuery() {
        return searchQuery == null ? "" : searchQuery;
    }

    public void clearSearch() {
        searchQuery = "";
        searchFocused = false;
        scrollRows = 0;
    }

    public void unfocusSearch() {
        searchFocused = false;
    }

    public boolean isSearchBoxHit(int mouseX, int mouseY) {
        return mouseX >= searchBoxX && mouseX <= searchBoxX + searchBoxW
            && mouseY >= searchBoxY && mouseY <= searchBoxY + searchBoxH;
    }

    public void focusSearch() {
        searchFocused = true;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery == null ? "" : searchQuery;
        scrollRows = 0;
    }

    public boolean handleSearchKey(int keycode, boolean shiftDown) {
        if (!searchFocused) return false;
        if (keycode == com.badlogic.gdx.Input.Keys.BACKSPACE) {
            if (!searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
            }
            scrollRows = 0;
            return true;
        }
        if (keycode == com.badlogic.gdx.Input.Keys.ESCAPE) {
            if (!searchQuery.isEmpty()) {
                searchQuery = "";
            } else {
                searchFocused = false;
            }
            scrollRows = 0;
            return true;
        }
        if (keycode == com.badlogic.gdx.Input.Keys.SPACE) {
            searchQuery += " ";
            scrollRows = 0;
            return true;
        }
        for (int k = com.badlogic.gdx.Input.Keys.A; k <= com.badlogic.gdx.Input.Keys.Z; k++) {
            if (keycode == k) {
                char c = com.badlogic.gdx.Input.Keys.toString(k).charAt(0);
                c = shiftDown ? c : Character.toLowerCase(c);
                searchQuery += c;
                scrollRows = 0;
                return true;
            }
        }
        int[] numKeys = {
            com.badlogic.gdx.Input.Keys.NUM_0, com.badlogic.gdx.Input.Keys.NUM_1, com.badlogic.gdx.Input.Keys.NUM_2,
            com.badlogic.gdx.Input.Keys.NUM_3, com.badlogic.gdx.Input.Keys.NUM_4, com.badlogic.gdx.Input.Keys.NUM_5,
            com.badlogic.gdx.Input.Keys.NUM_6, com.badlogic.gdx.Input.Keys.NUM_7, com.badlogic.gdx.Input.Keys.NUM_8,
            com.badlogic.gdx.Input.Keys.NUM_9
        };
        for (int i = 0; i < numKeys.length; i++) {
            if (keycode == numKeys[i]) {
                searchQuery += (char) ('0' + i);
                scrollRows = 0;
                return true;
            }
        }
        if (keycode == com.badlogic.gdx.Input.Keys.MINUS) {
            searchQuery += "-";
            scrollRows = 0;
            return true;
        }
        if (keycode == com.badlogic.gdx.Input.Keys.APOSTROPHE) {
            searchQuery += "'";
            scrollRows = 0;
            return true;
        }
        return false;
    }

    public boolean clickTab(int mouseX, int mouseY, List<ClientPacketHandler.BankSlotSnapshot> slots) {
        if (mouseX >= allTabX && mouseX <= allTabX + allTabW && mouseY >= allTabY && mouseY <= allTabY + allTabH) {
            setSelectedTab(ALL_TAB);
            return true;
        }
        for (int tab : getVisibleCustomTabs(slots)) {
            int idx = tab - 1;
            if (mouseX >= customTabX[idx] && mouseX <= customTabX[idx] + customTabW[idx]
                && mouseY >= customTabY[idx] && mouseY <= customTabY[idx] + customTabH[idx]) {
                setSelectedTab(tab);
                return true;
            }
        }
        if (plusTabW > 0 && mouseX >= plusTabX && mouseX <= plusTabX + plusTabW
            && mouseY >= plusTabY && mouseY <= plusTabY + plusTabH) {
            return true;
        }
        return false;
    }

    public int getTabDropTarget(int mouseX, int mouseY, List<ClientPacketHandler.BankSlotSnapshot> slots) {
        if (isSearchActive()) return -1;
        if (mouseX >= allTabX && mouseX <= allTabX + allTabW && mouseY >= allTabY && mouseY <= allTabY + allTabH) {
            return 0;
        }
        for (int tab : getVisibleCustomTabs(slots)) {
            int idx = tab - 1;
            if (mouseX >= customTabX[idx] && mouseX <= customTabX[idx] + customTabW[idx]
                && mouseY >= customTabY[idx] && mouseY <= customTabY[idx] + customTabH[idx]) {
                return tab;
            }
        }
        if (plusTabW > 0 && mouseX >= plusTabX && mouseX <= plusTabX + plusTabW
            && mouseY >= plusTabY && mouseY <= plusTabY + plusTabH) {
            return getNextAvailableCustomTab(slots);
        }
        return -1;
    }

    public List<Integer> getVisibleCustomTabs(List<ClientPacketHandler.BankSlotSnapshot> slots) {
        if (slots == null || slots.isEmpty()) return Collections.emptyList();
        boolean[] present = new boolean[MAX_CUSTOM_TABS + 1];
        for (ClientPacketHandler.BankSlotSnapshot slot : slots) {
            if (slot == null || slot.itemId <= 0 || slot.quantity <= 0) continue;
            if (slot.tabIndex >= 1 && slot.tabIndex <= MAX_CUSTOM_TABS) {
                present[slot.tabIndex] = true;
            }
        }
        List<Integer> out = new ArrayList<>();
        for (int tab = 1; tab <= MAX_CUSTOM_TABS; tab++) {
            if (present[tab]) out.add(tab);
        }
        return out;
    }

    public int getNextAvailableCustomTab(List<ClientPacketHandler.BankSlotSnapshot> slots) {
        boolean[] present = new boolean[MAX_CUSTOM_TABS + 1];
        for (int tab : getVisibleCustomTabs(slots)) {
            present[tab] = true;
        }
        for (int tab = 1; tab <= MAX_CUSTOM_TABS; tab++) {
            if (!present[tab]) return tab;
        }
        return -1;
    }

    public int getBankSlotAt(int mouseX, int mouseY, List<ClientPacketHandler.BankSlotSnapshot> slots) {
        if (!isBankCellHit(mouseX, mouseY)) return -1;
        int col = (mouseX - bankGridX) / (CELL + CELL_GAP);
        int rowFromTop = (bankGridY + bankGridH - mouseY - 1) / (CELL + CELL_GAP);
        if (col < 0 || col >= BANK_COLS || rowFromTop < 0 || rowFromTop >= bankVisibleRows) return -1;
        if (selectedTab == ALL_TAB && !isSearchActive()) {
            int slotIndex = (scrollRows + rowFromTop) * BANK_COLS + col;
            if (slots == null) return -1;
            for (ClientPacketHandler.BankSlotSnapshot slot : slots) {
                if (slot.slot == slotIndex && slot.itemId > 0 && slot.quantity > 0) {
                    return slotIndex;
                }
            }
            return -1;
        }

        List<ClientPacketHandler.BankSlotSnapshot> displaySlots = getDisplaySlots(slots);
        int packedIndex = (scrollRows + rowFromTop) * BANK_COLS + col;
        if (packedIndex < 0 || packedIndex >= displaySlots.size()) return -1;
        ClientPacketHandler.BankSlotSnapshot slot = displaySlots.get(packedIndex);
        return (slot.itemId > 0 && slot.quantity > 0) ? slot.slot : -1;
    }

    public int getBankCellSlotAt(int mouseX, int mouseY) {
        if (isSearchActive()) return -1;
        if (selectedTab != ALL_TAB) return -1;
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
        List<ClientPacketHandler.BankSlotSnapshot> displaySlots = getDisplaySlots(slots);
        int maxBankRow = getMaxBankRow(slots, displaySlots);
        int maxScroll = Math.max(0, maxBankRow - bankVisibleRows + 1);
        if (maxScroll <= 0) {
            scrollRows = 0;
            return false;
        }
        int before = scrollRows;
        scrollRows = Math.max(0, Math.min(maxScroll, scrollRows + amount));
        return before != scrollRows;
    }

    private void drawSlotText(SpriteBatch batch, BitmapFont font, int x, int y, String itemName, int itemId, long quantity) {
        String resolvedName = (itemName == null || itemName.isBlank()) ? ("Item " + itemId) : itemName;
        glyph.setText(font, resolvedName);
        String truncated = resolvedName;
        if (glyph.width > CELL - 6) {
            int maxChars = Math.max(3, (int) ((CELL - 8) / 5f));
            truncated = resolvedName.substring(0, Math.min(resolvedName.length(), maxChars));
        }
        font.setColor(0.92f, 0.90f, 0.84f, 1f);
        font.draw(batch, truncated, x + 3, y + CELL - 4);
        font.setColor(0.72f, 0.87f, 0.78f, 1f);
        font.draw(batch, formatQuantity(quantity), x + 3, y + 11);
    }

    private void clampSelectedTab(List<ClientPacketHandler.BankSlotSnapshot> slots) {
        if (selectedTab == ALL_TAB) return;
        for (int tab : getVisibleCustomTabs(slots)) {
            if (tab == selectedTab) {
                return;
            }
        }
        selectedTab = ALL_TAB;
        scrollRows = 0;
    }

    private ClientPacketHandler.BankSlotSnapshot getTabIconSlot(List<ClientPacketHandler.BankSlotSnapshot> slots, int tabIndex) {
        if (slots == null) return null;
        ClientPacketHandler.BankSlotSnapshot best = null;
        for (ClientPacketHandler.BankSlotSnapshot slot : slots) {
            if (slot.tabIndex != tabIndex) continue;
            if (slot.itemId <= 0 || slot.quantity <= 0) continue;
            if (best == null || slot.slot < best.slot) {
                best = slot;
            }
        }
        return best;
    }

    private List<ClientPacketHandler.BankSlotSnapshot> getDisplaySlots(List<ClientPacketHandler.BankSlotSnapshot> slots) {
        if (slots == null || slots.isEmpty()) return Collections.emptyList();
        List<ClientPacketHandler.BankSlotSnapshot> out = new ArrayList<>();
        String query = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);
        if (!query.isEmpty()) {
            for (ClientPacketHandler.BankSlotSnapshot slot : slots) {
                String name = slot.itemName == null ? "" : slot.itemName.toLowerCase(Locale.ROOT);
                if (name.contains(query)) {
                    out.add(slot);
                }
            }
        } else if (selectedTab == ALL_TAB) {
            out.addAll(slots);
        } else {
            for (ClientPacketHandler.BankSlotSnapshot slot : slots) {
                if (slot.tabIndex == selectedTab) {
                    out.add(slot);
                }
            }
        }
        out.sort(Comparator.comparingInt(slot -> slot.slot));
        return out;
    }

    private int getMaxBankRow(List<ClientPacketHandler.BankSlotSnapshot> slots,
                              List<ClientPacketHandler.BankSlotSnapshot> displaySlots) {
        if (selectedTab == ALL_TAB && !isSearchActive()) {
            if (slots == null || slots.isEmpty()) return 0;
            int maxSlot = 0;
            for (ClientPacketHandler.BankSlotSnapshot slot : slots) {
                if (slot.slot > maxSlot) {
                    maxSlot = slot.slot;
                }
            }
            return maxSlot / BANK_COLS;
        }
        if (displaySlots == null || displaySlots.isEmpty()) return 0;
        return Math.max(0, (displaySlots.size() - 1) / BANK_COLS);
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

    private void updateLayout(int screenW, int screenH, List<ClientPacketHandler.BankSlotSnapshot> slots) {
        panelW = Math.min(PANEL_W, Math.max(580, screenW - 32));
        panelH = Math.min(PANEL_H, Math.max(340, screenH - 48));
        panelX = (screenW - panelW) / 2;
        panelY = (screenH - panelH) / 2;

        closeX = panelX + panelW - PADDING - CLOSE_W;
        closeY = panelY + panelH - 8 - CLOSE_H;

        int tabBandH = 30;
        int amountBandH = 28;
        int contentTop = panelY + panelH - HEADER_H - PADDING;
        int contentBottom = panelY + PADDING;

        allTabX = panelX + PADDING;
        allTabY = contentTop - tabBandH + 2;
        allTabW = TAB_W;
        allTabH = TAB_H;

        for (int i = 0; i < MAX_CUSTOM_TABS; i++) {
            customTabX[i] = 0;
            customTabY[i] = 0;
            customTabW[i] = 0;
            customTabH[i] = 0;
        }

        int tabCursorX = allTabX + TAB_W + TAB_GAP;
        for (int tab : getVisibleCustomTabs(slots)) {
            int idx = tab - 1;
            customTabX[idx] = tabCursorX;
            customTabY[idx] = allTabY;
            customTabW[idx] = TAB_W;
            customTabH[idx] = TAB_H;
            tabCursorX += TAB_W + TAB_GAP;
        }

        int nextTab = getNextAvailableCustomTab(slots);
        if (nextTab > 0) {
            plusTabX = tabCursorX;
            plusTabY = allTabY;
            plusTabW = TAB_W;
            plusTabH = TAB_H;
        } else {
            plusTabX = 0;
            plusTabY = 0;
            plusTabW = 0;
            plusTabH = 0;
        }

        searchBoxW = 180;
        searchBoxH = 22;
        searchBoxX = panelX + panelW - PADDING - CLOSE_W - 12 - searchBoxW;
        searchBoxY = allTabY + 2;

        int amountY = (contentTop - tabBandH) - amountBandH;
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
