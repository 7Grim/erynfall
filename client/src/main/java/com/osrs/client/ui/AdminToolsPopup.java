package com.osrs.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.osrs.protocol.NetworkProto;
import com.osrs.client.network.ClientPacketHandler;

import java.util.List;

public class AdminToolsPopup {

    private static final int PANEL_W = 560;
    private static final int PANEL_H = 330;
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
    private static final String[] SKILL_NAMES = {
        "Attack", "Strength", "Defence", "Hitpoints", "Ranged", "Magic", "Prayer",
        "Woodcutting", "Fishing", "Cooking", "Mining", "Smithing", "Firemaking",
        "Crafting", "Runecrafting", "Fletching", "Agility", "Herblore", "Thieving",
        "Slayer", "Farming", "Hunter", "Construction"
    };

    private static final int[] LEVEL_DELTA_VALUES = {-10, -5, -1, 1, 5, 10};
    private static final int[] LEVEL_SET_VALUES = {1, 15, 30, 50, 75, 99};
    private static final long[] XP_DELTA_VALUES = {-1000L, -100L, 100L, 1000L};

    private boolean visible;
    private int selectedTabIdx;
    private int selectedSkillIdx;
    private AdminSkillAction pendingSkillAction;
    private AdminItemAction pendingItemAction;
    private AdminTravelAction pendingTravelAction;

    private String itemSearchQuery = "";
    private boolean itemSearchFocused = false;
    private int selectedAdminItemId = -1;
    private String selectedAdminItemName = "";
    private long selectedQuantity = 1;
    private boolean giveToBank = false;
    private boolean itemSearchDirty = false;

    private int itemSearchBoxX;
    private int itemSearchBoxY;
    private int itemSearchBoxW;
    private int itemSearchBoxH;
    private final int[] itemResultX = new int[20];
    private final int[] itemResultY = new int[20];
    private final int[] itemResultW = new int[20];
    private final int[] itemResultH = new int[20];
    private final boolean[] itemResultValid = new boolean[20];
    private final int[] itemResultItemId = new int[20];
    private final String[] itemResultItemName = new String[20];
    private final long[] ITEM_QTY_VALUES = {1L, 5L, 10L, 50L, 100L, 1000L};
    private final int[] itemQtyX = new int[6];
    private final int[] itemQtyY = new int[6];
    private final int[] itemQtyW = new int[6];
    private final int[] itemQtyH = new int[6];
    private int destInvX;
    private int destInvY;
    private int destInvW;
    private int destInvH;
    private int destBankX;
    private int destBankY;
    private int destBankW;
    private int destBankH;
    private int giveButtonX;
    private int giveButtonY;
    private int giveButtonW;
    private int giveButtonH;
    private int travelBtnX1;
    private int travelBtnY1;
    private int travelBtnW1;
    private int travelBtnH1;
    private int travelBtnX2;
    private int travelBtnY2;
    private int travelBtnW2;
    private int travelBtnH2;
    private int travelBtnX3;
    private int travelBtnY3;
    private int travelBtnW3;
    private int travelBtnH3;
    private int travelBtnX4;
    private int travelBtnY4;
    private int travelBtnW4;
    private int travelBtnH4;

    public static final class AdminSkillAction {
        public final boolean setLevel;
        public final int skillIdx;
        public final int levelValue;
        public final long xpDeltaWhole;

        public AdminSkillAction(boolean setLevel, int skillIdx, int levelValue, long xpDeltaWhole) {
            this.setLevel = setLevel;
            this.skillIdx = skillIdx;
            this.levelValue = levelValue;
            this.xpDeltaWhole = xpDeltaWhole;
        }
    }

    public static final class AdminItemAction {
        public final int itemId;
        public final long quantity;
        public final boolean toBank;

        public AdminItemAction(int itemId, long quantity, boolean toBank) {
            this.itemId = itemId;
            this.quantity = quantity;
            this.toBank = toBank;
        }
    }

    public static final class AdminTravelAction {
        public final NetworkProto.AdminTravelDestination destination;

        public AdminTravelAction(NetworkProto.AdminTravelDestination destination) {
            this.destination = destination;
        }
    }

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

    private int[] currentLevels = new int[SKILL_NAMES.length];
    private long[] currentXp = new long[SKILL_NAMES.length];

    private final int[] skillBtnX = new int[SKILL_NAMES.length];
    private final int[] skillBtnY = new int[SKILL_NAMES.length];
    private final int[] skillBtnW = new int[SKILL_NAMES.length];
    private final int[] skillBtnH = new int[SKILL_NAMES.length];
    private final boolean[] skillBtnValid = new boolean[SKILL_NAMES.length];

    private final int[] levelDeltaX = new int[LEVEL_DELTA_VALUES.length];
    private final int[] levelDeltaY = new int[LEVEL_DELTA_VALUES.length];
    private final int[] levelDeltaW = new int[LEVEL_DELTA_VALUES.length];
    private final int[] levelDeltaH = new int[LEVEL_DELTA_VALUES.length];

    private final int[] levelSetX = new int[LEVEL_SET_VALUES.length];
    private final int[] levelSetY = new int[LEVEL_SET_VALUES.length];
    private final int[] levelSetW = new int[LEVEL_SET_VALUES.length];
    private final int[] levelSetH = new int[LEVEL_SET_VALUES.length];

    private final int[] xpDeltaX = new int[XP_DELTA_VALUES.length];
    private final int[] xpDeltaY = new int[XP_DELTA_VALUES.length];
    private final int[] xpDeltaW = new int[XP_DELTA_VALUES.length];
    private final int[] xpDeltaH = new int[XP_DELTA_VALUES.length];

    private int skillsHeaderX;
    private int skillsHeaderY;
    private int skillsLevelLineY;
    private int skillsXpLineY;
    private int levelDeltaLabelY;
    private int levelSetLabelY;
    private int xpDeltaLabelY;

    private final GlyphLayout glyph = new GlyphLayout();

    public void show() {
        visible = true;
        selectedTabIdx = Math.max(0, Math.min(selectedTabIdx, TAB_NAMES.length - 1));
        selectedSkillIdx = Math.max(0, Math.min(selectedSkillIdx, SKILL_NAMES.length - 1));
        pendingSkillAction = null;
        pendingItemAction = null;
        pendingTravelAction = null;
        itemSearchDirty = true;
    }

    public void dismiss() {
        visible = false;
        pendingSkillAction = null;
        pendingItemAction = null;
        pendingTravelAction = null;
        itemSearchFocused = false;
    }

    public boolean isVisible() {
        return visible;
    }

    public AdminSkillAction consumePendingSkillAction() {
        AdminSkillAction action = pendingSkillAction;
        pendingSkillAction = null;
        return action;
    }

    public AdminItemAction consumePendingItemAction() {
        AdminItemAction action = pendingItemAction;
        pendingItemAction = null;
        return action;
    }

    public AdminTravelAction consumePendingTravelAction() {
        AdminTravelAction action = pendingTravelAction;
        pendingTravelAction = null;
        return action;
    }

    public boolean isItemSearchFocused() {
        return itemSearchFocused;
    }

    public boolean handleItemSearchKey(int keycode, boolean shiftDown) {
        if (!visible || selectedTabIdx != 1 || !itemSearchFocused) {
            return false;
        }
        if (keycode == Input.Keys.BACKSPACE) {
            if (!itemSearchQuery.isEmpty()) {
                itemSearchQuery = itemSearchQuery.substring(0, itemSearchQuery.length() - 1);
                itemSearchDirty = true;
            }
            return true;
        }
        if (keycode == Input.Keys.SPACE) {
            itemSearchQuery += " ";
            itemSearchDirty = true;
            return true;
        }
        if (keycode == Input.Keys.APOSTROPHE) {
            itemSearchQuery += '\'';
            itemSearchDirty = true;
            return true;
        }
        if (keycode == Input.Keys.MINUS) {
            itemSearchQuery += '-';
            itemSearchDirty = true;
            return true;
        }
        if (keycode >= Input.Keys.A && keycode <= Input.Keys.Z) {
            char c = Input.Keys.toString(keycode).charAt(0);
            if (!shiftDown) {
                c = Character.toLowerCase(c);
            }
            itemSearchQuery += c;
            itemSearchDirty = true;
            return true;
        }
        if (keycode >= Input.Keys.NUM_0 && keycode <= Input.Keys.NUM_9) {
            itemSearchQuery += (char) ('0' + (keycode - Input.Keys.NUM_0));
            itemSearchDirty = true;
            return true;
        }
        return false;
    }

    public String consumeItemSearchQueryIfDirty() {
        if (!itemSearchDirty) {
            return null;
        }
        itemSearchDirty = false;
        return itemSearchQuery;
    }

    public void clearItemSearchFocus() {
        itemSearchFocused = false;
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
                pendingSkillAction = null;
                pendingItemAction = null;
                pendingTravelAction = null;
                itemSearchFocused = false;
                if (selectedTabIdx == 1) {
                    itemSearchDirty = true;
                }
                return true;
            }
        }
        if (selectedTabIdx == 0) {
            return handleSkillsClick(mouseX, mouseY);
        }
        if (selectedTabIdx == 1) {
            return handleItemsClick(mouseX, mouseY);
        }
        if (selectedTabIdx == 2) {
            return handleTravelClick(mouseX, mouseY);
        }
        return true;
    }

    private boolean handleSkillsClick(int mouseX, int mouseY) {
        for (int i = 0; i < SKILL_NAMES.length; i++) {
            if (!skillBtnValid[i]) {
                continue;
            }
            if (mouseX >= skillBtnX[i] && mouseX <= skillBtnX[i] + skillBtnW[i]
                && mouseY >= skillBtnY[i] && mouseY <= skillBtnY[i] + skillBtnH[i]) {
                selectedSkillIdx = i;
                return true;
            }
        }

        int currentLevel = getCurrentLevel(selectedSkillIdx);
        for (int i = 0; i < LEVEL_DELTA_VALUES.length; i++) {
            if (mouseX >= levelDeltaX[i] && mouseX <= levelDeltaX[i] + levelDeltaW[i]
                && mouseY >= levelDeltaY[i] && mouseY <= levelDeltaY[i] + levelDeltaH[i]) {
                int target = Math.max(1, Math.min(99, currentLevel + LEVEL_DELTA_VALUES[i]));
                pendingSkillAction = new AdminSkillAction(true, selectedSkillIdx, target, 0L);
                return true;
            }
        }

        for (int i = 0; i < LEVEL_SET_VALUES.length; i++) {
            if (mouseX >= levelSetX[i] && mouseX <= levelSetX[i] + levelSetW[i]
                && mouseY >= levelSetY[i] && mouseY <= levelSetY[i] + levelSetH[i]) {
                pendingSkillAction = new AdminSkillAction(true, selectedSkillIdx, LEVEL_SET_VALUES[i], 0L);
                return true;
            }
        }

        for (int i = 0; i < XP_DELTA_VALUES.length; i++) {
            if (mouseX >= xpDeltaX[i] && mouseX <= xpDeltaX[i] + xpDeltaW[i]
                && mouseY >= xpDeltaY[i] && mouseY <= xpDeltaY[i] + xpDeltaH[i]) {
                pendingSkillAction = new AdminSkillAction(false, selectedSkillIdx, 0, XP_DELTA_VALUES[i]);
                return true;
            }
        }
        return true;
    }

    private boolean handleItemsClick(int mouseX, int mouseY) {
        itemSearchFocused = mouseX >= itemSearchBoxX && mouseX <= itemSearchBoxX + itemSearchBoxW
            && mouseY >= itemSearchBoxY && mouseY <= itemSearchBoxY + itemSearchBoxH;

        for (int i = 0; i < itemResultValid.length; i++) {
            if (!itemResultValid[i]) {
                continue;
            }
            if (mouseX >= itemResultX[i] && mouseX <= itemResultX[i] + itemResultW[i]
                && mouseY >= itemResultY[i] && mouseY <= itemResultY[i] + itemResultH[i]) {
                selectedAdminItemId = itemResultItemId[i];
                selectedAdminItemName = itemResultItemName[i] == null ? "" : itemResultItemName[i];
                return true;
            }
        }

        for (int i = 0; i < ITEM_QTY_VALUES.length; i++) {
            if (mouseX >= itemQtyX[i] && mouseX <= itemQtyX[i] + itemQtyW[i]
                && mouseY >= itemQtyY[i] && mouseY <= itemQtyY[i] + itemQtyH[i]) {
                selectedQuantity = ITEM_QTY_VALUES[i];
                return true;
            }
        }

        if (mouseX >= destInvX && mouseX <= destInvX + destInvW && mouseY >= destInvY && mouseY <= destInvY + destInvH) {
            giveToBank = false;
            return true;
        }
        if (mouseX >= destBankX && mouseX <= destBankX + destBankW && mouseY >= destBankY && mouseY <= destBankY + destBankH) {
            giveToBank = true;
            return true;
        }
        if (mouseX >= giveButtonX && mouseX <= giveButtonX + giveButtonW
            && mouseY >= giveButtonY && mouseY <= giveButtonY + giveButtonH) {
            if (selectedAdminItemId > 0 && selectedQuantity > 0) {
                pendingItemAction = new AdminItemAction(selectedAdminItemId, selectedQuantity, giveToBank);
            }
            return true;
        }

        return true;
    }

    private boolean handleTravelClick(int mouseX, int mouseY) {
        if (mouseX >= travelBtnX1 && mouseX <= travelBtnX1 + travelBtnW1
            && mouseY >= travelBtnY1 && mouseY <= travelBtnY1 + travelBtnH1) {
            pendingTravelAction = new AdminTravelAction(NetworkProto.AdminTravelDestination.ADMIN_TRAVEL_SPAWN);
            return true;
        }
        if (mouseX >= travelBtnX2 && mouseX <= travelBtnX2 + travelBtnW2
            && mouseY >= travelBtnY2 && mouseY <= travelBtnY2 + travelBtnH2) {
            pendingTravelAction = new AdminTravelAction(NetworkProto.AdminTravelDestination.ADMIN_TRAVEL_SANDBOX_GROVE);
            return true;
        }
        if (mouseX >= travelBtnX3 && mouseX <= travelBtnX3 + travelBtnW3
            && mouseY >= travelBtnY3 && mouseY <= travelBtnY3 + travelBtnH3) {
            pendingTravelAction = new AdminTravelAction(NetworkProto.AdminTravelDestination.ADMIN_TRAVEL_SANDBOX_FISHING_COVE);
            return true;
        }
        if (mouseX >= travelBtnX4 && mouseX <= travelBtnX4 + travelBtnW4
            && mouseY >= travelBtnY4 && mouseY <= travelBtnY4 + travelBtnH4) {
            pendingTravelAction = new AdminTravelAction(NetworkProto.AdminTravelDestination.ADMIN_TRAVEL_SANDBOX_MINING_COVE);
            return true;
        }
        return true;
    }

    public void render(ShapeRenderer shapeRenderer,
                       SpriteBatch batch,
                       BitmapFont font,
                       int screenW,
                       int screenH,
                       Matrix4 projection,
                       ClientPacketHandler handler) {
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

        refreshSkillSnapshot(handler);

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

        if (selectedTabIdx == 0) {
            renderSkillsBackground(shapeRenderer);
        } else if (selectedTabIdx == 1) {
            renderItemsBackground(shapeRenderer, handler);
        } else if (selectedTabIdx == 2) {
            renderTravelBackground(shapeRenderer);
        }

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

        if (selectedTabIdx == 0) {
            renderSkillsBorders(shapeRenderer);
        } else if (selectedTabIdx == 1) {
            renderItemsBorders(shapeRenderer);
        } else if (selectedTabIdx == 2) {
            renderTravelBorders(shapeRenderer);
        }

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

        if (selectedTabIdx == 0) {
            renderSkillsText(batch, font, handler);
        } else if (selectedTabIdx == 1) {
            renderItemsText(batch, font, handler);
        } else if (selectedTabIdx == 2) {
            renderTravelText(batch, font, handler);
        }

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private void refreshSkillSnapshot(ClientPacketHandler handler) {
        for (int i = 0; i < SKILL_NAMES.length; i++) {
            currentLevels[i] = handler == null ? 1 : handler.getSkillLevel(i);
            currentXp[i] = handler == null ? 0L : handler.getSkillTotalXp(i);
        }
        selectedSkillIdx = Math.max(0, Math.min(selectedSkillIdx, SKILL_NAMES.length - 1));
    }

    private int getCurrentLevel(int skillIdx) {
        if (skillIdx < 0 || skillIdx >= currentLevels.length) {
            return 1;
        }
        return currentLevels[skillIdx];
    }

    private long getCurrentXp(int skillIdx) {
        if (skillIdx < 0 || skillIdx >= currentXp.length) {
            return 0L;
        }
        return currentXp[skillIdx];
    }

    private void renderSkillsBackground(ShapeRenderer shapeRenderer) {
        float leftX = contentX + 8;
        float leftY = contentY + 8;
        float leftW = 248;
        float leftH = contentH - 16;
        float rightX = leftX + leftW + 8;
        float rightY = leftY;
        float rightW = contentW - (rightX - contentX) - 8;
        float rightH = leftH;

        shapeRenderer.setColor(0.90f, 0.82f, 0.66f, 1f);
        shapeRenderer.rect(leftX, leftY, leftW, leftH);
        shapeRenderer.rect(rightX, rightY, rightW, rightH);

        int cols = 2;
        int btnW = 114;
        int btnH = 16;
        int gapX = 10;
        int gapY = 2;
        int startX = (int) leftX + 8;
        int startY = (int) (leftY + leftH - btnH - 8);

        for (int i = 0; i < SKILL_NAMES.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int x = startX + col * (btnW + gapX);
            int y = startY - row * (btnH + gapY);
            skillBtnX[i] = x;
            skillBtnY[i] = y;
            skillBtnW[i] = btnW;
            skillBtnH[i] = btnH;
            skillBtnValid[i] = true;
            boolean selected = i == selectedSkillIdx;
            shapeRenderer.setColor(selected ? 0.94f : 0.82f, selected ? 0.78f : 0.70f, selected ? 0.48f : 0.56f, 1f);
            shapeRenderer.rect(x, y, btnW, btnH);
        }

        int selectedLevel = getCurrentLevel(selectedSkillIdx);
        long selectedXp = getCurrentXp(selectedSkillIdx);
        int innerX = (int) rightX + 10;
        int innerY = (int) rightY + 10;
        int innerW = (int) rightW - 20;
        int topY = (int) (rightY + rightH - 12);
        skillsHeaderX = innerX;
        skillsHeaderY = topY;
        skillsLevelLineY = topY - 22;
        skillsXpLineY = topY - 40;
        int btnH2 = 18;
        int gapSmall = 6;
        int deltaBtnW = (innerW - gapSmall * (LEVEL_DELTA_VALUES.length - 1)) / LEVEL_DELTA_VALUES.length;
        int setBtnW = (innerW - gapSmall * (LEVEL_SET_VALUES.length - 1)) / LEVEL_SET_VALUES.length;
        int xpBtnW = (innerW - gapSmall * (XP_DELTA_VALUES.length - 1)) / XP_DELTA_VALUES.length;
        levelDeltaLabelY = topY - 58;
        int levelDeltaRowY = levelDeltaLabelY - 18;
        for (int i = 0; i < LEVEL_DELTA_VALUES.length; i++) {
            int x = innerX + i * (deltaBtnW + gapSmall);
            int y = levelDeltaRowY;
            levelDeltaX[i] = x;
            levelDeltaY[i] = y;
            levelDeltaW[i] = deltaBtnW;
            levelDeltaH[i] = btnH2;
            shapeRenderer.setColor(0.80f, 0.73f, 0.58f, 1f);
            shapeRenderer.rect(x, y, deltaBtnW, btnH2);
        }
        levelSetLabelY = levelDeltaRowY - 18;
        int levelSetRowY = levelSetLabelY - 18;
        for (int i = 0; i < LEVEL_SET_VALUES.length; i++) {
            int x = innerX + i * (setBtnW + gapSmall);
            int y = levelSetRowY;
            levelSetX[i] = x;
            levelSetY[i] = y;
            levelSetW[i] = setBtnW;
            levelSetH[i] = btnH2;
            shapeRenderer.setColor(0.80f, 0.73f, 0.58f, 1f);
            shapeRenderer.rect(x, y, setBtnW, btnH2);
        }
        xpDeltaLabelY = levelSetRowY - 18;
        int xpDeltaRowY = xpDeltaLabelY - 18;
        for (int i = 0; i < XP_DELTA_VALUES.length; i++) {
            int x = innerX + i * (xpBtnW + gapSmall);
            int y = xpDeltaRowY;
            xpDeltaX[i] = x;
            xpDeltaY[i] = y;
            xpDeltaW[i] = xpBtnW;
            xpDeltaH[i] = btnH2;
            shapeRenderer.setColor(0.80f, 0.73f, 0.58f, 1f);
            shapeRenderer.rect(x, y, xpBtnW, btnH2);
        }

        if (selectedLevel < 1 || selectedXp < 0 || innerY < 0) {
            // no-op guard to keep locals used coherently for static analysis
        }
    }

    private void renderSkillsBorders(ShapeRenderer shapeRenderer) {
        float leftX = contentX + 8;
        float leftY = contentY + 8;
        float leftW = 248;
        float leftH = contentH - 16;
        float rightX = leftX + leftW + 8;
        float rightY = leftY;
        float rightW = contentW - (rightX - contentX) - 8;
        float rightH = leftH;

        shapeRenderer.setColor(PANEL_BORDER);
        shapeRenderer.rect(leftX, leftY, leftW, leftH);
        shapeRenderer.rect(rightX, rightY, rightW, rightH);

        for (int i = 0; i < SKILL_NAMES.length; i++) {
            if (!skillBtnValid[i]) {
                continue;
            }
            shapeRenderer.rect(skillBtnX[i], skillBtnY[i], skillBtnW[i], skillBtnH[i]);
        }
        for (int i = 0; i < LEVEL_DELTA_VALUES.length; i++) {
            shapeRenderer.rect(levelDeltaX[i], levelDeltaY[i], levelDeltaW[i], levelDeltaH[i]);
            shapeRenderer.rect(levelSetX[i], levelSetY[i], levelSetW[i], levelSetH[i]);
        }
        for (int i = 0; i < XP_DELTA_VALUES.length; i++) {
            shapeRenderer.rect(xpDeltaX[i], xpDeltaY[i], xpDeltaW[i], xpDeltaH[i]);
        }
    }

    private void renderSkillsText(SpriteBatch batch, BitmapFont font, ClientPacketHandler handler) {
        float leftX = contentX + 8;
        float leftY = contentY + 8;
        float leftW = 248;
        float leftH = contentH - 16;
        float rightX = leftX + leftW + 8;
        float rightY = leftY;
        float rightW = contentW - (rightX - contentX) - 8;
        float rightH = leftH;

        font.getData().setScale(0.62f);
        for (int i = 0; i < SKILL_NAMES.length; i++) {
            if (!skillBtnValid[i]) {
                continue;
            }
            boolean selected = i == selectedSkillIdx;
            font.setColor(selected ? new Color(0.12f, 0.08f, 0.03f, 1f) : new Color(0.25f, 0.16f, 0.06f, 1f));
            font.draw(batch, SKILL_NAMES[i], skillBtnX[i] + 6, skillBtnY[i] + 12);
            font.setColor(0.20f, 0.14f, 0.06f, 1f);
            font.draw(batch, Integer.toString(getCurrentLevel(i)), skillBtnX[i] + skillBtnW[i] - 18, skillBtnY[i] + 12);
        }

        int selectedLevel = getCurrentLevel(selectedSkillIdx);
        long selectedXp = getCurrentXp(selectedSkillIdx);

        font.getData().setScale(0.74f);
        font.setColor(0.20f, 0.12f, 0.04f, 1f);
        font.draw(batch, SKILL_NAMES[selectedSkillIdx], skillsHeaderX, skillsHeaderY);

        font.getData().setScale(0.66f);
        font.setColor(0.24f, 0.16f, 0.06f, 1f);
        font.draw(batch, "Level: " + selectedLevel, skillsHeaderX, skillsLevelLineY);
        font.draw(batch, "XP: " + selectedXp, skillsHeaderX, skillsXpLineY);

        font.draw(batch, "Level +/-", skillsHeaderX, levelDeltaLabelY);
        for (int i = 0; i < LEVEL_DELTA_VALUES.length; i++) {
            String label = LEVEL_DELTA_VALUES[i] > 0 ? "+" + LEVEL_DELTA_VALUES[i] : Integer.toString(LEVEL_DELTA_VALUES[i]);
            glyph.setText(font, label);
            font.draw(batch, label,
                levelDeltaX[i] + (levelDeltaW[i] - glyph.width) / 2f,
                levelDeltaY[i] + 13);
        }

        font.draw(batch, "Set level", skillsHeaderX, levelSetLabelY);
        for (int i = 0; i < LEVEL_SET_VALUES.length; i++) {
            String label = Integer.toString(LEVEL_SET_VALUES[i]);
            glyph.setText(font, label);
            font.draw(batch, label,
                levelSetX[i] + (levelSetW[i] - glyph.width) / 2f,
                levelSetY[i] + 13);
        }

        font.draw(batch, "XP delta (whole XP)", skillsHeaderX, xpDeltaLabelY);
        for (int i = 0; i < XP_DELTA_VALUES.length; i++) {
            String label = XP_DELTA_VALUES[i] > 0 ? "+" + XP_DELTA_VALUES[i] : Long.toString(XP_DELTA_VALUES[i]);
            glyph.setText(font, label);
            font.draw(batch, label,
                xpDeltaX[i] + (xpDeltaW[i] - glyph.width) / 2f,
                xpDeltaY[i] + 13);
        }

        if (handler != null) {
            String msg = handler.getLastAdminActionMessage();
            if (msg != null && !msg.isBlank()) {
                font.getData().setScale(0.64f);
                font.setColor(handler.wasLastAdminActionSuccessful()
                    ? new Color(0.14f, 0.42f, 0.16f, 1f)
                    : new Color(0.52f, 0.14f, 0.14f, 1f));
                font.draw(batch, msg, rightX + 10, rightY + 14);
            }
        }
    }

    private void renderItemsBackground(ShapeRenderer shapeRenderer, ClientPacketHandler handler) {
        int leftX = contentX + 8;
        int leftY = contentY + 8;
        int leftW = 290;
        int leftH = contentH - 16;
        int rightX = leftX + leftW + 8;
        int rightY = leftY;
        int rightW = contentW - (rightX - contentX) - 8;
        int rightH = leftH;

        shapeRenderer.setColor(0.90f, 0.82f, 0.66f, 1f);
        shapeRenderer.rect(leftX, leftY, leftW, leftH);
        shapeRenderer.rect(rightX, rightY, rightW, rightH);

        itemSearchBoxX = leftX + 8;
        itemSearchBoxY = leftY + leftH - 28;
        itemSearchBoxW = leftW - 16;
        itemSearchBoxH = 18;
        shapeRenderer.setColor(itemSearchFocused ? 0.95f : 0.84f, itemSearchFocused ? 0.88f : 0.76f, 0.62f, 1f);
        shapeRenderer.rect(itemSearchBoxX, itemSearchBoxY, itemSearchBoxW, itemSearchBoxH);

        for (int i = 0; i < itemResultValid.length; i++) {
            itemResultValid[i] = false;
            itemResultItemId[i] = 0;
            itemResultItemName[i] = "";
        }

        List<ClientPacketHandler.AdminItemSearchEntry> entries = handler == null
            ? List.of()
            : handler.getAdminItemSearchResults();
        int rowH = 12;
        int listTopY = itemSearchBoxY - 6;
        int maxRows = Math.min(20, entries.size());
        for (int i = 0; i < maxRows; i++) {
            int rowY = listTopY - (i + 1) * rowH;
            if (rowY < leftY + 8) {
                break;
            }
            ClientPacketHandler.AdminItemSearchEntry entry = entries.get(i);
            itemResultX[i] = leftX + 8;
            itemResultY[i] = rowY;
            itemResultW[i] = leftW - 16;
            itemResultH[i] = rowH;
            itemResultValid[i] = true;
            itemResultItemId[i] = entry.itemId;
            itemResultItemName[i] = entry.itemName;
            boolean selected = entry.itemId == selectedAdminItemId;
            shapeRenderer.setColor(selected ? 0.95f : 0.82f, selected ? 0.79f : 0.72f, selected ? 0.48f : 0.56f, 1f);
            shapeRenderer.rect(itemResultX[i], itemResultY[i], itemResultW[i], itemResultH[i]);
        }

        int innerX = rightX + 10;
        int innerW = rightW - 20;
        int rowY = rightY + rightH - 84;
        int btnH = 18;
        int gap = 6;
        int qtyW = (innerW - gap * (ITEM_QTY_VALUES.length - 1)) / ITEM_QTY_VALUES.length;
        for (int i = 0; i < ITEM_QTY_VALUES.length; i++) {
            itemQtyX[i] = innerX + i * (qtyW + gap);
            itemQtyY[i] = rowY;
            itemQtyW[i] = qtyW;
            itemQtyH[i] = btnH;
            shapeRenderer.setColor(selectedQuantity == ITEM_QTY_VALUES[i] ? 0.95f : 0.80f,
                selectedQuantity == ITEM_QTY_VALUES[i] ? 0.79f : 0.73f,
                selectedQuantity == ITEM_QTY_VALUES[i] ? 0.48f : 0.58f, 1f);
            shapeRenderer.rect(itemQtyX[i], itemQtyY[i], itemQtyW[i], itemQtyH[i]);
        }

        int destY = rowY - 34;
        destInvW = (innerW - gap) / 2;
        destBankW = destInvW;
        destInvH = 18;
        destBankH = 18;
        destInvX = innerX;
        destInvY = destY;
        destBankX = innerX + destInvW + gap;
        destBankY = destY;
        shapeRenderer.setColor(!giveToBank ? 0.95f : 0.80f, !giveToBank ? 0.79f : 0.73f, !giveToBank ? 0.48f : 0.58f, 1f);
        shapeRenderer.rect(destInvX, destInvY, destInvW, destInvH);
        shapeRenderer.setColor(giveToBank ? 0.95f : 0.80f, giveToBank ? 0.79f : 0.73f, giveToBank ? 0.48f : 0.58f, 1f);
        shapeRenderer.rect(destBankX, destBankY, destBankW, destBankH);

        giveButtonX = innerX;
        giveButtonY = destY - 34;
        giveButtonW = innerW;
        giveButtonH = 22;
        shapeRenderer.setColor(selectedAdminItemId > 0 ? 0.70f : 0.60f,
            selectedAdminItemId > 0 ? 0.30f : 0.28f,
            selectedAdminItemId > 0 ? 0.18f : 0.24f,
            1f);
        shapeRenderer.rect(giveButtonX, giveButtonY, giveButtonW, giveButtonH);
    }

    private void renderItemsBorders(ShapeRenderer shapeRenderer) {
        int leftX = contentX + 8;
        int leftY = contentY + 8;
        int leftW = 290;
        int leftH = contentH - 16;
        int rightX = leftX + leftW + 8;
        int rightY = leftY;
        int rightW = contentW - (rightX - contentX) - 8;
        int rightH = leftH;
        shapeRenderer.setColor(PANEL_BORDER);
        shapeRenderer.rect(leftX, leftY, leftW, leftH);
        shapeRenderer.rect(rightX, rightY, rightW, rightH);
        shapeRenderer.rect(itemSearchBoxX, itemSearchBoxY, itemSearchBoxW, itemSearchBoxH);
        for (int i = 0; i < itemResultValid.length; i++) {
            if (itemResultValid[i]) {
                shapeRenderer.rect(itemResultX[i], itemResultY[i], itemResultW[i], itemResultH[i]);
            }
        }
        for (int i = 0; i < ITEM_QTY_VALUES.length; i++) {
            shapeRenderer.rect(itemQtyX[i], itemQtyY[i], itemQtyW[i], itemQtyH[i]);
        }
        shapeRenderer.rect(destInvX, destInvY, destInvW, destInvH);
        shapeRenderer.rect(destBankX, destBankY, destBankW, destBankH);
        shapeRenderer.rect(giveButtonX, giveButtonY, giveButtonW, giveButtonH);
    }

    private void renderItemsText(SpriteBatch batch, BitmapFont font, ClientPacketHandler handler) {
        int leftX = contentX + 8;
        int leftY = contentY + 8;
        int leftW = 290;
        int rightX = leftX + leftW + 8;

        font.getData().setScale(0.66f);
        font.setColor(0.24f, 0.16f, 0.06f, 1f);
        font.draw(batch, "Search items", itemSearchBoxX, itemSearchBoxY + itemSearchBoxH + 11);
        String shownQuery = itemSearchQuery.isBlank() ? "Type to search..." : itemSearchQuery;
        font.setColor(itemSearchQuery.isBlank() ? new Color(0.42f, 0.32f, 0.18f, 1f) : new Color(0.20f, 0.12f, 0.04f, 1f));
        font.draw(batch, shownQuery, itemSearchBoxX + 4, itemSearchBoxY + 13);

        List<ClientPacketHandler.AdminItemSearchEntry> entries = handler == null
            ? List.of()
            : handler.getAdminItemSearchResults();
        font.getData().setScale(0.60f);
        for (int i = 0; i < itemResultValid.length; i++) {
            if (!itemResultValid[i]) {
                continue;
            }
            ClientPacketHandler.AdminItemSearchEntry entry = i < entries.size() ? entries.get(i) : null;
            if (entry == null) {
                continue;
            }
            String label = entry.itemName + " [" + entry.itemId + "]";
            font.setColor(entry.itemId == selectedAdminItemId ? new Color(0.12f, 0.08f, 0.03f, 1f)
                : new Color(0.25f, 0.16f, 0.06f, 1f));
            font.draw(batch, label, itemResultX[i] + 4, itemResultY[i] + 10);
        }

        int infoX = rightX + 10;
        int infoTop = contentY + contentH - 12;
        font.getData().setScale(0.70f);
        font.setColor(0.20f, 0.12f, 0.04f, 1f);
        font.draw(batch, "Selected item", infoX, infoTop);
        font.getData().setScale(0.64f);
        font.setColor(0.24f, 0.16f, 0.06f, 1f);
        String selection = selectedAdminItemId > 0
            ? selectedAdminItemName + " [" + selectedAdminItemId + "]"
            : "None";
        font.draw(batch, selection, infoX, infoTop - 18);

        font.draw(batch, "Quantity", infoX, itemQtyY[0] + itemQtyH[0] + 12);
        for (int i = 0; i < ITEM_QTY_VALUES.length; i++) {
            String label = Long.toString(ITEM_QTY_VALUES[i]);
            glyph.setText(font, label);
            font.draw(batch, label,
                itemQtyX[i] + (itemQtyW[i] - glyph.width) / 2f,
                itemQtyY[i] + 13);
        }

        font.draw(batch, "Destination", infoX, destInvY + destInvH + 12);
        glyph.setText(font, "Inventory");
        font.draw(batch, "Inventory", destInvX + (destInvW - glyph.width) / 2f, destInvY + 13);
        glyph.setText(font, "Bank");
        font.draw(batch, "Bank", destBankX + (destBankW - glyph.width) / 2f, destBankY + 13);

        font.getData().setScale(0.70f);
        font.setColor(selectedAdminItemId > 0 ? new Color(0.96f, 0.92f, 0.84f, 1f) : new Color(0.86f, 0.82f, 0.74f, 1f));
        glyph.setText(font, "Give Item");
        font.draw(batch, "Give Item", giveButtonX + (giveButtonW - glyph.width) / 2f, giveButtonY + 15);

        if (handler != null) {
            String msg = handler.getLastAdminActionMessage();
            if (msg != null && !msg.isBlank()) {
                font.getData().setScale(0.62f);
                font.setColor(handler.wasLastAdminActionSuccessful()
                    ? new Color(0.14f, 0.42f, 0.16f, 1f)
                    : new Color(0.52f, 0.14f, 0.14f, 1f));
                font.draw(batch, msg, infoX, contentY + 20);
            }
        }
    }

    private void renderTravelBackground(ShapeRenderer shapeRenderer) {
        int innerX = contentX + 16;
        int innerY = contentY + 16;
        int innerW = contentW - 32;
        int innerH = contentH - 32;
        shapeRenderer.setColor(0.90f, 0.82f, 0.66f, 1f);
        shapeRenderer.rect(innerX, innerY, innerW, innerH);

        int btnW = Math.min(260, innerW - 24);
        int btnH = 26;
        int startX = innerX + 12;
        int startY = innerY + innerH - 68;

        travelBtnX1 = startX;
        travelBtnY1 = startY;
        travelBtnW1 = btnW;
        travelBtnH1 = btnH;
        travelBtnX2 = startX;
        travelBtnY2 = startY - 38;
        travelBtnW2 = btnW;
        travelBtnH2 = btnH;
        travelBtnX3 = startX;
        travelBtnY3 = startY - 76;
        travelBtnW3 = btnW;
        travelBtnH3 = btnH;
        travelBtnX4 = startX;
        travelBtnY4 = startY - 114;
        travelBtnW4 = btnW;
        travelBtnH4 = btnH;

        shapeRenderer.setColor(0.80f, 0.73f, 0.58f, 1f);
        shapeRenderer.rect(travelBtnX1, travelBtnY1, travelBtnW1, travelBtnH1);
        shapeRenderer.rect(travelBtnX2, travelBtnY2, travelBtnW2, travelBtnH2);
        shapeRenderer.rect(travelBtnX3, travelBtnY3, travelBtnW3, travelBtnH3);
        shapeRenderer.rect(travelBtnX4, travelBtnY4, travelBtnW4, travelBtnH4);
    }

    private void renderTravelBorders(ShapeRenderer shapeRenderer) {
        int innerX = contentX + 16;
        int innerY = contentY + 16;
        int innerW = contentW - 32;
        int innerH = contentH - 32;
        shapeRenderer.setColor(PANEL_BORDER);
        shapeRenderer.rect(innerX, innerY, innerW, innerH);
        shapeRenderer.rect(travelBtnX1, travelBtnY1, travelBtnW1, travelBtnH1);
        shapeRenderer.rect(travelBtnX2, travelBtnY2, travelBtnW2, travelBtnH2);
        shapeRenderer.rect(travelBtnX3, travelBtnY3, travelBtnW3, travelBtnH3);
        shapeRenderer.rect(travelBtnX4, travelBtnY4, travelBtnW4, travelBtnH4);
    }

    private void renderTravelText(SpriteBatch batch, BitmapFont font, ClientPacketHandler handler) {
        int titleX = contentX + 28;
        int titleY = contentY + contentH - 28;

        font.getData().setScale(0.72f);
        font.setColor(0.20f, 0.12f, 0.04f, 1f);
        font.draw(batch, "Safe fixed test teleports only.", titleX, titleY);

        font.getData().setScale(0.68f);
        font.setColor(0.24f, 0.16f, 0.06f, 1f);
        font.draw(batch, "Spawn", travelBtnX1 + 10, travelBtnY1 + 18);
        font.draw(batch, "Sandbox Grove", travelBtnX2 + 10, travelBtnY2 + 18);
        font.draw(batch, "Fishing Cove", travelBtnX3 + 10, travelBtnY3 + 18);
        font.draw(batch, "Mining Cove", travelBtnX4 + 10, travelBtnY4 + 18);

        if (handler != null) {
            String msg = handler.getLastAdminActionMessage();
            if (msg != null && !msg.isBlank()) {
                font.getData().setScale(0.62f);
                font.setColor(handler.wasLastAdminActionSuccessful()
                    ? new Color(0.14f, 0.42f, 0.16f, 1f)
                    : new Color(0.52f, 0.14f, 0.14f, 1f));
                font.draw(batch, msg, titleX, contentY + 28);
            }
        }
    }
}
