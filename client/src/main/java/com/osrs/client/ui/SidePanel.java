package com.osrs.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OSRS-style unified side panel with a tab bar at the top.
 *
 * All game UI panels share a single anchored panel in the bottom-right corner.
 * The tab row at the top switches between Combat Options, Skills, and Inventory —
 * matching how OSRS uses F1-F6 (and bottom row F7-F12) to switch panel content.
 *
 *  ┌───────────────────────────────┐  ← TAB_H: tab icon row
 *  │  [CMB]     [SKL]     [INV]   │
 *  ├───────────────────────────────┤  ← divider
 *  │                               │
 *  │        active tab content     │  ← CONTENT_H
 *  │                               │
 *  └───────────────────────────────┘
 *
 * Panel is always anchored: right edge = screenW - MARGIN, bottom = MARGIN.
 */
public class SidePanel {

    // -----------------------------------------------------------------------
    // Layout
    // -----------------------------------------------------------------------

    /** Width of the side panel in pixels. Matches InventoryUI's internal width. */
    public static final int PANEL_W   = 240;
    /** Height of the tab icon row at the top of the panel. */
    public static final int TAB_H     = 36;
    /** Height of the content area (sized to fit 7-row inventory). */
    public static final int CONTENT_H = 312;
    /** Total panel height: content + two tab rows (top + bottom). */
    public static final int TOTAL_H   = TAB_H * 2 + CONTENT_H;
    /** Gap between panel edge and screen edge. */
    public static final int MARGIN    = 8;

    private static final Color BORDER_COLOR = new Color(0.55f, 0.46f, 0.28f, 1f);  // Brown-gold
    private static final int BORDER_THICKNESS = 2;
    // Stat pillar bar colours (pre-allocated — never allocate Color in render)
    private static final Color HP_BAR_BG = new Color(0.08f, 0.02f, 0.02f, 1f);
    private static final Color HP_BAR_FILL = new Color(0.82f, 0.08f, 0.08f, 1f);
    private static final Color HP_BAR_LOW = new Color(1.00f, 0.14f, 0.08f, 1f); // flashes brighter when <=25%
    private static final Color HP_BAR_DARK = new Color(0.50f, 0.04f, 0.04f, 1f); // right-edge shadow strip
    private static final Color PR_BAR_BG = new Color(0.02f, 0.02f, 0.10f, 1f);
    private static final Color PR_BAR_FILL = new Color(0.15f, 0.45f, 0.90f, 1f); // OSRS prayer blue
    private static final Color PR_BAR_DARK = new Color(0.08f, 0.24f, 0.58f, 1f); // right-edge shadow strip
    private static final Color BG_COLOR = new Color(0.10f, 0.09f, 0.08f, 1f);  // Fully opaque
    private static final int FRIEND_ROW_H = 14;
    private static final int FRIEND_REMOVE_W = 42;

    // -----------------------------------------------------------------------
    // OSRS XP table — exact formula: floor(level + 300 * 2^(level/7)) / 4
    // -----------------------------------------------------------------------

    /** XP required to reach each level. Index = level (1-99). */
    private static final long[] XP_TABLE = buildXpTable();

    private static long[] buildXpTable() {
        long[] table = new long[100];  // index = level
        long   points = 0;
        for (int level = 1; level < 99; level++) {
            points += (long) Math.floor(level + 300.0 * Math.pow(2.0, level / 7.0));
            table[level + 1] = (long) Math.floor(points / 4.0);
        }
        return table;
    }

    /** XP required to reach {@code level} (1–99). Returns 0 for level 1, caps at 13,034,431. */
    private static long xpForLevel(int level) {
        if (level <= 1)  return 0L;
        if (level >= 99) return 13_034_431L;
        return XP_TABLE[level];
    }

    // -----------------------------------------------------------------------
    // Tab definitions  — ordered left to right in the tab bar
    // -----------------------------------------------------------------------

    public enum Tab {
        // Top row — F1–F7 (OSRS order)
        COMBAT    (0,  "Combat"),
        SKILLS    (1,  "Skills"),
        QUESTS    (2,  "Quests"),
        INVENTORY (3,  "Inventory"),
        EQUIPMENT (4,  "Equipment"),
        PRAYER    (5,  "Prayer"),
        MAGIC     (6,  "Magic"),
        // Bottom row — F8–F13
        FRIENDS   (7,  "Friends"),
        IGNORE    (8,  "Ignore"),
        CLAN      (9,  "Clan"),
        SETTINGS  (10, "Settings"),
        EMOTES    (11, "Emotes"),
        MUSIC     (12, "Music");

        public final int    index;
        public final String label;
        Tab(int i, String l) { this.index = i; this.label = l; }
    }

    private static final Tab[] TOP_TABS = {
        Tab.COMBAT, Tab.SKILLS, Tab.QUESTS, Tab.INVENTORY, Tab.EQUIPMENT, Tab.PRAYER, Tab.MAGIC
    };
    private static final Tab[] BOTTOM_TABS = {
        Tab.FRIENDS, Tab.IGNORE, Tab.CLAN, Tab.SETTINGS, Tab.EMOTES, Tab.MUSIC
    };

    private Tab activeTab = Tab.INVENTORY;   // default open tab (OSRS default)

    // -----------------------------------------------------------------------
    // Sub-components / data
    // -----------------------------------------------------------------------

    private final InventoryUI inventoryUI = new InventoryUI();

    private enum CharacterPage {
        SUMMARY,
        QUEST_LIST,
        GEAR,
        FRIENDS_LIST
    }

    public enum QuestStatus {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED
    }

    public static class QuestTaskView {
        public final String taskId;
        public final String description;
        public final int currentCount;
        public final int requiredCount;
        public final boolean completed;

        public QuestTaskView(String taskId, String description, int currentCount, int requiredCount, boolean completed) {
            this.taskId = taskId;
            this.description = description;
            this.currentCount = currentCount;
            this.requiredCount = requiredCount;
            this.completed = completed;
        }
    }

    public static class QuestView {
        public final int questId;
        public final String questName;
        public final String description;
        public final boolean miniquest;
        public final int questPointsReward;
        public final QuestStatus status;
        public final List<QuestTaskView> tasks;

        public QuestView(int questId, String questName, String description,
                         boolean miniquest, int questPointsReward,
                         QuestStatus status, List<QuestTaskView> tasks) {
            this.questId = questId;
            this.questName = questName;
            this.description = description;
            this.miniquest = miniquest;
            this.questPointsReward = questPointsReward;
            this.status = status;
            this.tasks = tasks;
        }
    }

    // Skills (synced from ClientPacketHandler each frame)
    private final int[]  skillLevels = new int[23];
    private final long[] skillXp     = new long[23];
    private final int[]    equippedIds   = new int[11];
    private final String[] equippedNames = new String[11];
    private int[]   equipBonuses  = new int[14]; // indices 0-13: stab_attack...prayer
    private boolean gearShowStats = false;        // false=slot grid, true=bonus stats
    private boolean memberPlayer = false;

    private static final String[] SLOT_LABELS = {
        "Head","Cape","Neck","Ammo","Weapon","Shield","Body","Legs","Hands","Feet","Ring"
    };

    private static final String[] SKILL_NAMES = {
        "Attack", "Strength", "Defence", "Hitpoints", "Ranged", "Magic",
        "Prayer", "Woodcutting", "Fishing", "Cooking", "Mining", "Smithing", "Firemaking",
        "Crafting", "Runecrafting", "Fletching", "Agility", "Herblore", "Thieving",
        "Slayer", "Farming", "Hunter", "Construction"
    };
    private static final Color[] SKILL_COLORS = {
        new Color(0.72f, 0.18f, 0.10f, 1f),   // Attack       – dark-red steel sword
        new Color(0.52f, 0.38f, 0.26f, 1f),   // Strength     – grey-brown fist
        new Color(0.22f, 0.42f, 0.72f, 1f),   // Defence      – steel blue shield
        new Color(0.85f, 0.15f, 0.15f, 1f),   // Hitpoints    – bright red heart
        new Color(0.40f, 0.58f, 0.20f, 1f),   // Ranged       – olive-green bow
        new Color(0.30f, 0.25f, 0.78f, 1f),   // Magic        – deep blue wand
        new Color(0.90f, 0.85f, 0.55f, 1f),   // Prayer       – cream/pale yellow
        new Color(0.55f, 0.32f, 0.12f, 1f),   // Woodcutting  – brown axe
        new Color(0.20f, 0.48f, 0.78f, 1f),   // Fishing      – mid blue
        new Color(0.82f, 0.38f, 0.12f, 1f),   // Cooking      – flame orange
        new Color(0.52f, 0.52f, 0.55f, 1f),   // Mining       – stone grey
        new Color(0.55f, 0.52f, 0.50f, 1f),   // Smithing     – grey anvil
        new Color(0.92f, 0.52f, 0.08f, 1f),   // Firemaking   – bright orange flame
        new Color(0.75f, 0.68f, 0.28f, 1f),   // Crafting     – golden tools
        new Color(0.58f, 0.35f, 0.72f, 1f),   // Runecrafting – purple rune
        new Color(0.62f, 0.55f, 0.28f, 1f),   // Fletching    – olive arrow shaft
        new Color(0.35f, 0.58f, 0.78f, 1f),   // Agility      – sky-blue figure
        new Color(0.25f, 0.65f, 0.30f, 1f),   // Herblore     – herb green
        new Color(0.48f, 0.38f, 0.52f, 1f),   // Thieving     – grey-purple
        new Color(0.78f, 0.14f, 0.14f, 1f),   // Slayer       – dark-red skull
        new Color(0.30f, 0.62f, 0.22f, 1f),   // Farming      – leaf green
        new Color(0.78f, 0.62f, 0.28f, 1f),   // Hunter       – tan/earth
        new Color(0.80f, 0.68f, 0.38f, 1f),   // Construction – sandstone
    };

    // Combat style: server index (0=Accurate 1=Aggressive 2=Defensive 3=Controlled)
    private int combatStyle = 1;   // default: Aggressive
    private boolean autoRetaliate = true;

    // Pre-allocated colors to avoid per-frame GC in renderCombatTab
    private static final Color COLOR_BTN_SEL_BG = new Color(0.35f, 0.27f, 0.04f, 1f);
    private static final Color COLOR_BTN_IDLE_BG = new Color(0.17f, 0.15f, 0.13f, 1f);
    private static final Color COLOR_BTN_SEL_BORDER = new Color(1f, 0.85f, 0.10f, 1f);
    private static final Color COLOR_BTN_IDLE_BORDER = new Color(0.40f, 0.36f, 0.26f, 1f);
    private static final Color COLOR_BTN_SEL_TEXT = new Color(1f, 0.90f, 0.10f, 1f);
    private static final Color COLOR_TITLE_GOLD = new Color(0.9f, 0.80f, 0.50f, 1f);
    private static final Color COLOR_ICON_ACTIVE = new Color(1f, 0.85f, 0.15f, 1f);
    private static final Color COLOR_ICON_IDLE = new Color(0.50f, 0.48f, 0.38f, 1f);
    private static final Color COLOR_TOGGLE_ON = new Color(0.18f, 0.55f, 0.22f, 1f);
    private static final Color COLOR_BTN_DISABLED_BORDER = new Color(0.25f, 0.23f, 0.18f, 1f);
    private CharacterPage characterPage = CharacterPage.SUMMARY;
    private final Map<Integer, QuestView> quests = new HashMap<>();
    private int selectedQuestId = -1;
    private int playerQuestPoints = 0;
    private boolean logoutRequested = false;
    private final List<FriendEntryView> friendEntries = new ArrayList<>();
    private long removeFriendRequestedId = -1L;

    public static class FriendEntryView {
        public final long playerId;
        public final String name;
        public final boolean online;

        public FriendEntryView(long playerId, String name, boolean online) {
            this.playerId = playerId;
            this.name = name;
            this.online = online;
        }
    }
    // HP state (synced from GameScreen each frame)
    private int currentHp = 0;
    private int maxHp = 0;

    // Prayer tab state (synced from GameScreen)
    private int currentPrayerPoints = 0;
    private int maxPrayerPoints     = 0;
    private final java.util.Set<Integer> activePrayerIds = new java.util.HashSet<>();
    public void setHpState(int current, int max) {
        this.currentHp = current;
        this.maxHp = max;
    }

    public void setPrayerState(int current, int max, java.util.Set<Integer> active) {
        this.currentPrayerPoints = current;
        this.maxPrayerPoints     = max;
        this.activePrayerIds.clear();
        if (active != null) this.activePrayerIds.addAll(active);
    }

    // Cached panel origin (set each render call)
    private int panelX, panelY;

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    public void update(float delta) {
        inventoryUI.update(delta);
    }

    // -----------------------------------------------------------------------
    // Rendering
    // -----------------------------------------------------------------------

    public void render(ShapeRenderer sr, SpriteBatch batch, BitmapFont font,
                       int screenW, int screenH, Matrix4 proj, int mouseX, int mouseY) {
        panelX = screenW - PANEL_W - MARGIN;
        panelY = MARGIN;

        // --- Panel background ---
        sr.setProjectionMatrix(proj);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(BG_COLOR);
        sr.rect(panelX, panelY, PANEL_W, TOTAL_H);

        // Outer border (2px)
        sr.setColor(BORDER_COLOR);
        sr.rect(panelX, panelY, PANEL_W, BORDER_THICKNESS); // bottom
        sr.rect(panelX, panelY + TOTAL_H - BORDER_THICKNESS, PANEL_W, BORDER_THICKNESS); // top
        sr.rect(panelX, panelY, BORDER_THICKNESS, TOTAL_H); // left
        sr.rect(panelX + PANEL_W - BORDER_THICKNESS, panelY, BORDER_THICKNESS, TOTAL_H); // right
        sr.end();

        // --- Tab bars (two rows) ---
        renderTabBar(sr, batch, font, proj, mouseX, mouseY);

        // --- Dividers: content ↔ bottom row, and between the two rows ---
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(BORDER_COLOR);
        sr.rect(panelX, panelY + CONTENT_H, PANEL_W, BORDER_THICKNESS);
        sr.rect(panelX, panelY + CONTENT_H + TAB_H, PANEL_W, BORDER_THICKNESS);
        sr.end();

        // --- Active tab content ---
        switch (activeTab) {
            case COMBAT    -> renderCombatTab(sr, batch, font, proj);
            case SKILLS    -> renderSkillsTab(sr, batch, font, proj, screenW, screenH, mouseX, mouseY);
            case QUESTS    -> {
                characterPage = CharacterPage.QUEST_LIST;
                renderCharacterTab(sr, batch, font, proj);
            }
            case INVENTORY -> {
                int invX = panelX + (PANEL_W - inventoryUI.getPanelWidth()) / 2;
                renderStatPillars(sr, proj, invX);
                inventoryUI.render(sr, batch, font, invX, panelY, proj);
            }
            case EQUIPMENT -> {
                characterPage = CharacterPage.GEAR;
                renderCharacterTab(sr, batch, font, proj);
            }
            case PRAYER    -> renderPrayerTab(sr, batch, font, proj, mouseX, mouseY);
            case FRIENDS   -> {
                characterPage = CharacterPage.FRIENDS_LIST;
                renderCharacterTab(sr, batch, font, proj);
            }
            case SETTINGS  -> {
                characterPage = CharacterPage.SUMMARY;
                renderCharacterTab(sr, batch, font, proj);
            }
            default        -> renderStubTab(sr, batch, font, proj, activeTab.label);
        }
    }

    // -----------------------------------------------------------------------
    // Tab bar
    // -----------------------------------------------------------------------

    private void renderTabBar(ShapeRenderer sr, SpriteBatch batch, BitmapFont font, Matrix4 proj,
                              int mouseX, int mouseY) {
        sr.setProjectionMatrix(proj);
        // Bottom row rendered first (lower Y), then top row above it
        renderTabRow(sr, mouseX, mouseY, BOTTOM_TABS, panelY + CONTENT_H);
        renderTabRow(sr, mouseX, mouseY, TOP_TABS, panelY + CONTENT_H + TAB_H);
    }

    private void renderTabRow(ShapeRenderer sr, int mouseX, int mouseY,
                              Tab[] rowTabs, int rowY) {
        int tabW = PANEL_W / rowTabs.length;
        int lastW = PANEL_W - tabW * (rowTabs.length - 1);

        // Strip background
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.09f, 0.08f, 0.07f, 1f);
        sr.rect(panelX + 1, rowY + 1, PANEL_W - 2, TAB_H - 2);

        // Button backgrounds + icons
        for (int i = 0; i < rowTabs.length; i++) {
            int tx = panelX + i * tabW;
            int tw = (i == rowTabs.length - 1) ? lastW : tabW;
            boolean active = (rowTabs[i] == activeTab);
            boolean hover = mouseX >= tx && mouseX < tx + tw
                && mouseY >= rowY && mouseY <= rowY + TAB_H;

            if (active) sr.setColor(0.34f, 0.28f, 0.08f, 1f);
            else if (hover) sr.setColor(0.22f, 0.18f, 0.14f, 1f);
            else sr.setColor(0.15f, 0.13f, 0.11f, 1f);
            sr.rect(tx + 1, rowY + 1, tw - 2, TAB_H - 2);

            int cx = tx + tw / 2;
            int cy = rowY + TAB_H / 2;
            float r = active ? 1.00f : hover ? 0.80f : 0.55f;
            float g = active ? 0.88f : hover ? 0.72f : 0.50f;
            float b = active ? 0.15f : hover ? 0.50f : 0.40f;
            sr.setColor(r, g, b, 1f);
            drawTabIcon(sr, rowTabs[i], cx, cy);
        }
        sr.end();

        // Borders
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < rowTabs.length; i++) {
            int tx = panelX + i * tabW;
            int tw = (i == rowTabs.length - 1) ? lastW : tabW;
            boolean active = (rowTabs[i] == activeTab);
            boolean hover = mouseX >= tx && mouseX < tx + tw
                && mouseY >= rowY && mouseY <= rowY + TAB_H;
            if (active) sr.setColor(1.0f, 0.85f, 0.10f, 1f);
            else if (hover) sr.setColor(0.65f, 0.57f, 0.35f, 1f);
            else sr.setColor(0.38f, 0.32f, 0.18f, 1f);
            int bw = active ? 2 : 1;
            int x = tx + 1;
            int y = rowY + 1;
            int w = tw - 2;
            int h = TAB_H - 2;
            sr.rect(x, y, w, bw);
            sr.rect(x, y + h - bw, w, bw);
            sr.rect(x, y, bw, h);
            sr.rect(x + w - bw, y, bw, h);
        }
        // Vertical separators
        sr.setColor(0.30f, 0.26f, 0.16f, 1f);
        for (int i = 1; i < rowTabs.length; i++) {
            sr.rect(panelX + i * tabW, rowY + 3, 1, TAB_H - 6);
        }
        sr.end();
    }

    /** Draw the icon for a given tab at pixel center (cx, cy). ShapeRenderer must be in Filled mode. */
    private void drawTabIcon(ShapeRenderer sr, Tab tab, int cx, int cy) {
        switch (tab) {
            case COMBAT -> {
                sr.rect(cx - 7, cy - 1, 14, 2);
                sr.rect(cx - 1, cy - 7, 2, 14);
            }
            case SKILLS -> {
                sr.rect(cx - 6, cy - 5, 3, 3);
                sr.rect(cx - 2, cy - 5, 3, 7);
                sr.rect(cx + 2, cy - 5, 3, 11);
            }
            case QUESTS -> {
                sr.rect(cx - 5, cy - 6, 10, 12);
                sr.rect(cx - 3, cy - 1, 6, 2);
                sr.rect(cx - 3, cy + 3, 5, 2);
            }
            case INVENTORY -> {
                sr.rect(cx - 5, cy - 6, 10, 12);
                sr.rect(cx - 2, cy + 5, 4, 2);
                sr.rect(cx - 1, cy - 1, 2, 3);
            }
            case EQUIPMENT -> {
                sr.rect(cx - 5, cy + 2, 10, 4);
                sr.rect(cx - 4, cy - 3, 8, 6);
                sr.rect(cx - 2, cy - 6, 4, 4);
            }
            case PRAYER -> {
                sr.circle(cx, cy + 3, 3);
                sr.rect(cx - 1, cy - 5, 2, 8);
                sr.rect(cx - 4, cy - 1, 8, 2);
            }
            case MAGIC -> {
                sr.circle(cx, cy + 4, 3);
                sr.rect(cx - 1, cy - 6, 2, 11);
                sr.rect(cx + 1, cy - 3, 4, 2);
            }
            case FRIENDS -> {
                sr.circle(cx, cy + 4, 3, 8);
                sr.rect(cx - 4, cy - 6, 8, 8);
            }
            case IGNORE -> {
                sr.circle(cx - 2, cy + 4, 3, 8);
                sr.rect(cx - 5, cy - 5, 7, 7);
                sr.rect(cx + 2, cy + 2, 5, 2);
                sr.rect(cx + 4, cy - 1, 2, 4);
            }
            case CLAN -> {
                sr.rect(cx - 2, cy - 6, 3, 12);
                sr.rect(cx + 1, cy + 2, 6, 3);
                sr.rect(cx + 1, cy - 2, 5, 3);
            }
            case SETTINGS -> {
                sr.circle(cx, cy, 5, 12);
                sr.rect(cx - 1, cy + 5, 2, 3);
                sr.rect(cx - 1, cy - 7, 2, 3);
                sr.rect(cx + 5, cy - 1, 3, 2);
                sr.rect(cx - 8, cy - 1, 3, 2);
            }
            case EMOTES -> {
                sr.circle(cx - 3, cy + 2, 2, 8);
                sr.circle(cx + 3, cy + 2, 2, 8);
                sr.rect(cx - 4, cy - 4, 8, 2);
            }
            case MUSIC -> {
                sr.rect(cx - 5, cy - 4, 3, 10);
                sr.rect(cx, cy - 2, 3, 8);
                sr.rect(cx - 5, cy + 4, 8, 2);
            }
        }
    }

    private void renderStubTab(ShapeRenderer sr, SpriteBatch batch, BitmapFont font, Matrix4 proj, String tabName) {
        batch.setProjectionMatrix(proj);
        batch.begin();
        font.getData().setScale(0.80f);
        font.setColor(0.55f, 0.52f, 0.42f, 1f);
        font.draw(batch, tabName + " - Coming Soon", panelX + 10, panelY + CONTENT_H / 2 + 8);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    // -----------------------------------------------------------------------
    // Combat tab
    // -----------------------------------------------------------------------

    /**
     * Draws vertical HP (left) and prayer (right) bars inside the two dark pillars
     * that flank the inventory grid. Bars fill from the bottom; drain from the top.
     */
    private void renderStatPillars(ShapeRenderer sr, Matrix4 proj, int invX) {
        int leftX = panelX + BORDER_THICKNESS;
        int rightX = invX + inventoryUI.getPanelWidth();
        int pillarW = invX - panelX - BORDER_THICKNESS; // 25px — same on both sides (centred)
        int pillarH = CONTENT_H;

        sr.setProjectionMatrix(proj);
        sr.begin(ShapeRenderer.ShapeType.Filled);

        // --- Left pillar: HP bar ---
        sr.setColor(HP_BAR_BG);
        sr.rect(leftX, panelY, pillarW, pillarH);

        if (maxHp > 0 && currentHp > 0) {
            float fraction = Math.min(1f, (float) currentHp / maxHp);
            int fillH = Math.max(1, (int) (pillarH * fraction) - 1);
            sr.setColor(fraction <= 0.25f ? HP_BAR_LOW : HP_BAR_FILL);
            sr.rect(leftX + 2, panelY + 1, pillarW - 4, fillH);
            // Right-edge shadow strip (gives a 3-D depth feel)
            sr.setColor(HP_BAR_DARK);
            sr.rect(leftX + pillarW - 4, panelY + 1, 2, fillH);
        }

        // --- Right pillar: Prayer bar ---
        sr.setColor(PR_BAR_BG);
        sr.rect(rightX, panelY, pillarW, pillarH);

        if (maxPrayerPoints > 0 && currentPrayerPoints > 0) {
            float fraction = Math.min(1f, (float) currentPrayerPoints / maxPrayerPoints);
            int fillH = Math.max(1, (int) (pillarH * fraction) - 1);
            sr.setColor(PR_BAR_FILL);
            sr.rect(rightX + 2, panelY + 1, pillarW - 4, fillH);
            // Right-edge shadow strip
            sr.setColor(PR_BAR_DARK);
            sr.rect(rightX + pillarW - 4, panelY + 1, 2, fillH);
        }

        sr.end();

        // Thin gold border around each pillar
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(BORDER_COLOR);
        sr.rect(leftX, panelY, pillarW, pillarH);
        sr.rect(rightX, panelY, pillarW, pillarH);
        sr.end();
    }

    private void renderCombatTab(ShapeRenderer sr, SpriteBatch batch, BitmapFont font, Matrix4 proj) {
        int pad  = 10;
        int btnW = (PANEL_W - pad * 3) / 2;
        int btnH = 56;

        String[] names = styleNames();
        String[] xpLabels = styleXp();
        String weaponName = (equippedNames[4] != null && !equippedNames[4].isEmpty())
            ? equippedNames[4] : "Unarmed";

        // Title + weapon label
        batch.setProjectionMatrix(proj);
        batch.begin();
        font.getData().setScale(0.85f);
        font.setColor(COLOR_TITLE_GOLD);
        font.draw(batch, "Combat Options", panelX + pad, panelY + CONTENT_H - 8);
        font.getData().setScale(0.75f);
        font.setColor(0.70f, 0.65f, 0.50f, 1f);
        font.draw(batch, weaponName, panelX + pad, panelY + CONTENT_H - 24);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();

        // Title underline
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.45f, 0.38f, 0.22f, 1f);
        sr.rect(panelX + pad, panelY + CONTENT_H - 34, PANEL_W - pad * 2, 1);
        sr.end();

        // 2x2 button grid
        int gridTop = panelY + CONTENT_H - 40;
        for (int i = 0; i < 4; i++) {
            int col = i % 2;
            int row = i / 2;
            int bx  = panelX + pad + col * (btnW + pad);
            int by  = gridTop - (row + 1) * (btnH + pad);

            boolean disabled = names[i] == null;
            boolean sel = !disabled && i == combatStyle;

            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(sel ? COLOR_BTN_SEL_BG : COLOR_BTN_IDLE_BG);
            sr.rect(bx, by, btnW, btnH);
            sr.end();

            sr.begin(ShapeRenderer.ShapeType.Line);
            if (disabled) sr.setColor(COLOR_BTN_DISABLED_BORDER);
            else sr.setColor(sel ? COLOR_BTN_SEL_BORDER : COLOR_BTN_IDLE_BORDER);
            sr.rect(bx, by, btnW, btnH);
            sr.end();

            if (!disabled) {
                drawCombatStyleIcon(sr, i, bx + btnW / 2, by + btnH - 20, sel);
            }
        }

        // Button labels
        batch.setProjectionMatrix(proj);
        batch.begin();
        for (int i = 0; i < 4; i++) {
            if (names[i] == null) continue;
            int col = i % 2;
            int row = i / 2;
            int bx  = panelX + pad + col * (btnW + pad);
            int by  = gridTop - (row + 1) * (btnH + pad);
            boolean sel = i == combatStyle;

            font.getData().setScale(0.85f);
            font.setColor(sel ? COLOR_BTN_SEL_TEXT : Color.WHITE);
            font.draw(batch, names[i], bx + 6, by + btnH - 6);

            font.getData().setScale(0.70f);
            font.setColor(0.60f, 0.60f, 0.60f, 1f);
            font.draw(batch, xpLabels[i], bx + 6, by + 14);
        }
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();

        // Auto Retaliate toggle
        int toggleY  = gridTop - 2 * (btnH + pad) - 6;
        int toggleSz = 14;
        int toggleX  = panelX + pad;

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(autoRetaliate ? COLOR_TOGGLE_ON : COLOR_BTN_IDLE_BG);
        sr.rect(toggleX, toggleY, toggleSz, toggleSz);
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(0.55f, 0.46f, 0.28f, 1f);
        sr.rect(toggleX, toggleY, toggleSz, toggleSz);
        sr.end();

        if (autoRetaliate) {
            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(Color.WHITE);
            sr.rect(toggleX + 2, toggleY + 3, 3, 2);
            sr.rect(toggleX + 4, toggleY + 2, 2, 6);
            sr.end();
        }

        batch.setProjectionMatrix(proj);
        batch.begin();
        font.getData().setScale(0.85f);
        font.setColor(Color.WHITE);
        font.draw(batch, "Auto Retaliate", toggleX + toggleSz + 6, toggleY + toggleSz - 1);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    /**
     * Combat style icon drawn in the upper portion of each button.
     * style = visual grid position (0=TL, 1=TR, 2=BL, 3=BR).
     */
    private void drawCombatStyleIcon(ShapeRenderer sr, int style, int cx, int cy, boolean active) {
        Color c = active ? COLOR_ICON_ACTIVE : COLOR_ICON_IDLE;
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(c);
        switch (style) {
            case 0 -> {
                sr.rect(cx - 1, cy + 4, 3, 3);
                sr.rect(cx + 3, cy, 3, 3);
                sr.rect(cx - 1, cy - 4, 3, 3);
                sr.rect(cx - 5, cy, 3, 3);
                sr.rect(cx - 1, cy, 3, 3);
            }
            case 1 -> {
                sr.rect(cx - 5, cy - 4, 11, 5);
                sr.rect(cx - 2, cy + 1, 5, 5);
                sr.rect(cx - 4, cy + 3, 9, 2);
            }
            case 2 -> {
                sr.rect(cx - 7, cy + 1, 15, 2);
                sr.rect(cx - 6, cy - 4, 4, 6);
                sr.rect(cx + 2, cy - 4, 4, 6);
                sr.rect(cx - 1, cy + 3, 3, 2);
            }
            case 3 -> {
                sr.rect(cx - 5, cy + 1, 11, 5);
                sr.rect(cx - 4, cy - 2, 9, 4);
                sr.rect(cx - 3, cy - 5, 7, 4);
                sr.rect(cx - 1, cy - 7, 3, 3);
            }
        }
        sr.end();
    }

    /** Returns style names in visual grid order [TL, TR, BL, BR]. */
    private String[] styleNames() {
        String weapon = equippedNames[4];
        if (weapon == null || weapon.isEmpty()) {
            return new String[]{"Punch", "Kick", "Block", null};
        }
        return new String[]{"Accurate", "Aggressive", "Controlled", "Defensive"};
    }

    private String[] styleXp() {
        String weapon = equippedNames[4];
        if (weapon == null || weapon.isEmpty()) {
            return new String[]{"Attack XP", "Strength XP", "Defence XP", null};
        }
        return new String[]{"Attack XP", "Strength XP", "Shared XP", "Defence XP"};
    }

    public boolean isAutoRetaliate() { return autoRetaliate; }

    /**
     * OSRS skills-tab grid: 3 columns × 8 rows, OSRS canonical ordering.
     * Each row = {col0 skillIdx, col1 skillIdx, col2 skillIdx}, -1 = Total Level cell.
     */
    private static final int[][] SKILL_GRID = {
        { 0,  3, 10},  // Attack,       Hitpoints,   Mining
        { 1, 16, 11},  // Strength,     Agility,     Smithing
        { 2, 17,  8},  // Defence,      Herblore,    Fishing
        { 4, 18,  9},  // Ranged,       Thieving,    Cooking
        { 6, 13, 12},  // Prayer,       Crafting,    Firemaking
        { 5, 15,  7},  // Magic,        Fletching,   Woodcutting
        {14, 19, 20},  // Runecrafting, Slayer,      Farming
        {22, 21, -1},  // Construction, Hunter,      [Total Level]
    };

    /** F2P prayer definitions: {prayerId, levelRequired, name}. */
    private static final Object[][] PRAYERS = {
        {1,  1, "Thick Skin"},
        {2,  4, "Burst of Strength"},
        {3,  7, "Clarity of Thought"},
        {4, 10, "Rock Skin"},
        {5, 13, "Superhuman Strength"},
        {6, 16, "Improved Reflexes"},
    };

    private void renderPrayerTab(ShapeRenderer sr, SpriteBatch batch, BitmapFont font,
                                  Matrix4 proj, int mouseX, int mouseY) {
        final int PAD     = 8;
        final int ROW_H   = 36;
        final int BAR_H   = 20;
        final int DOT_SZ  = 20;

        // -- Header --
        batch.setProjectionMatrix(proj);
        batch.begin();
        font.getData().setScale(0.85f);
        font.setColor(0.90f, 0.80f, 0.50f, 1f);
        font.draw(batch, "Prayer", panelX + PAD, panelY + CONTENT_H - 8);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();

        sr.setProjectionMatrix(proj);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.45f, 0.38f, 0.22f, 1f);
        sr.rect(panelX + PAD, panelY + CONTENT_H - 20, PANEL_W - PAD * 2, 1);
        sr.end();

        // -- Prayer rows --
        int prayerLevel = skillLevels[6]; // SKILL_PRAYER index
        int startY = panelY + CONTENT_H - 28;

        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < PRAYERS.length; i++) {
            int    prayerId  = (int) PRAYERS[i][0];
            int    levelReq  = (int) PRAYERS[i][1];
            boolean active   = activePrayerIds.contains(prayerId);
            boolean canUse   = prayerLevel >= levelReq && currentPrayerPoints > 0;
            boolean hovering = mouseX >= panelX + PAD && mouseX <= panelX + PANEL_W - PAD
                            && mouseY >= startY - (i + 1) * ROW_H
                            && mouseY <= startY - i * ROW_H - 2;

            int rowY = startY - (i + 1) * ROW_H + 2;

            // Row background
            if (active) {
                sr.setColor(0.20f, 0.28f, 0.10f, 1f);
            } else if (hovering && canUse) {
                sr.setColor(0.16f, 0.14f, 0.10f, 1f);
            } else {
                sr.setColor(0.10f, 0.09f, 0.07f, 1f);
            }
            sr.rect(panelX + PAD, rowY, PANEL_W - PAD * 2, ROW_H - 2);

            // Active/inactive indicator dot
            if (active) {
                sr.setColor(0.55f, 0.88f, 0.20f, 1f);
            } else if (!canUse) {
                sr.setColor(0.28f, 0.24f, 0.18f, 1f);
            } else {
                sr.setColor(0.38f, 0.34f, 0.22f, 1f);
            }
            sr.rect(panelX + PAD + 2, rowY + (ROW_H - 2 - DOT_SZ) / 2f, DOT_SZ, DOT_SZ);

            // Row border
            sr.setColor(0.38f, 0.32f, 0.18f, 0.70f);
            sr.rect(panelX + PAD, rowY,            PANEL_W - PAD * 2, 1);
            sr.rect(panelX + PAD, rowY + ROW_H - 3, PANEL_W - PAD * 2, 1);
        }
        sr.end();

        // -- Prayer point bar (bottom) --
        int barY  = panelY + PAD + 2;
        int barW  = PANEL_W - PAD * 2;
        float ratio = maxPrayerPoints > 0 ? (float) currentPrayerPoints / maxPrayerPoints : 0f;

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.12f, 0.10f, 0.08f, 1f);
        sr.rect(panelX + PAD, barY, barW, BAR_H);
        sr.setColor(0.22f, 0.50f, 0.82f, 1f);
        sr.rect(panelX + PAD, barY, (int) (barW * ratio), BAR_H);
        sr.setColor(0.38f, 0.32f, 0.18f, 0.80f);
        sr.rect(panelX + PAD, barY,       barW, 1);
        sr.rect(panelX + PAD, barY + BAR_H - 1, barW, 1);
        sr.end();

        // -- Text pass --
        batch.setProjectionMatrix(proj);
        batch.begin();
        for (int i = 0; i < PRAYERS.length; i++) {
            int    prayerId = (int) PRAYERS[i][0];
            int    levelReq = (int) PRAYERS[i][1];
            String name     = (String) PRAYERS[i][2];
            boolean active  = activePrayerIds.contains(prayerId);
            boolean canUse  = prayerLevel >= levelReq;
            int rowY = startY - (i + 1) * ROW_H + 2;

            font.getData().setScale(0.78f);
            font.setColor(active ? new Color(0.75f, 1.00f, 0.35f, 1f)
                        : canUse ? new Color(0.88f, 0.84f, 0.72f, 1f)
                                 : new Color(0.45f, 0.42f, 0.35f, 1f));
            font.draw(batch, name, panelX + PAD + DOT_SZ + 8, rowY + ROW_H - 8);

            font.getData().setScale(0.62f);
            font.setColor(canUse ? new Color(0.65f, 0.90f, 0.25f, 1f)
                                 : new Color(0.75f, 0.58f, 0.18f, 1f));
            font.draw(batch, "Lv " + levelReq, panelX + PANEL_W - PAD - 26, rowY + ROW_H - 8);
        }

        // Prayer points bar label
        font.getData().setScale(0.72f);
        font.setColor(0.78f, 0.88f, 1.00f, 1f);
        font.draw(batch, currentPrayerPoints + " / " + maxPrayerPoints,
            panelX + PAD + 4, barY + BAR_H - 3);

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private void renderSkillsTab(ShapeRenderer sr, SpriteBatch batch, BitmapFont font, Matrix4 proj,
                                  int screenW, int screenH, int mouseX, int mouseY) {
        final int COLS    = 3;
        final int ROWS    = SKILL_GRID.length;  // 8
        final int PAD_X   = 3;
        final int PAD_Y   = 2;
        final int COL_GAP = 2;
        final int ROW_GAP = 1;
        final int CELL_W  = (PANEL_W - PAD_X * 2 - COL_GAP * (COLS - 1)) / COLS;  // ~76
        final int CELL_H  = (CONTENT_H - PAD_Y * 2 - ROW_GAP * (ROWS - 1)) / ROWS; // ~37
        final int ICON_SZ = Math.min(CELL_H - 4, 32);

        // Compute cell origins for all 23 skills + total-level cell.
        // cellX/cellY indexed by skillIdx; -2 = total-level cell stored at index 23.
        int[] cellX = new int[24]; int[] cellY = new int[24];
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int idx = SKILL_GRID[r][c];
                int storeIdx = (idx == -1) ? 23 : idx;
                cellX[storeIdx] = panelX + PAD_X + c * (CELL_W + COL_GAP);
                cellY[storeIdx] = panelY + CONTENT_H - PAD_Y - (r + 1) * CELL_H - r * ROW_GAP;
            }
        }

        // -- Pass 1: cell backgrounds --
        sr.setProjectionMatrix(proj);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < 24; i++) {
            int cx = cellX[i]; int cy = cellY[i];
            boolean isTotalCell = (i == 23);
            boolean membersLocked = !isTotalCell && i >= 15 && !memberPlayer;
            sr.setColor(membersLocked ? 0.07f : 0.10f,
                        membersLocked ? 0.06f : 0.09f,
                        membersLocked ? 0.05f : 0.07f, 1f);
            sr.rect(cx, cy, CELL_W, CELL_H);
        }

        // -- Pass 2: skill icon shapes --
        for (int i = 0; i < 23; i++) {
            int cx = cellX[i]; int cy = cellY[i];
            boolean membersLocked = i >= 15 && !memberPlayer;
            float iconX = cx + 2;
            float iconY = cy + (CELL_H - ICON_SZ) / 2f;
            Color ic = SKILL_COLORS[i];
            float dim = membersLocked ? 0.35f : 1.0f;

            // Icon background tile
            sr.setColor(ic.r * dim * 0.45f, ic.g * dim * 0.45f, ic.b * dim * 0.45f, 1f);
            sr.rect(iconX, iconY, ICON_SZ, ICON_SZ);

            // Icon symbol (brighter, drawn over background)
            sr.setColor(ic.r * dim, ic.g * dim, ic.b * dim, 1f);
            drawSkillIcon(sr, i, iconX, iconY, ICON_SZ);
        }
        sr.end();

        // -- Pass 3: cell borders (filled 1px rects – more reliable than Line mode) --
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.38f, 0.32f, 0.16f, 0.80f);
        for (int i = 0; i < 24; i++) {
            int cx = cellX[i]; int cy = cellY[i];
            sr.rect(cx, cy,            CELL_W, 1);
            sr.rect(cx, cy + CELL_H - 1, CELL_W, 1);
            sr.rect(cx, cy,            1, CELL_H);
            sr.rect(cx + CELL_W - 1, cy, 1, CELL_H);
        }
        sr.end();

        // -- Pass 4: level numbers + Total Level cell --
        batch.setProjectionMatrix(proj);
        batch.begin();
        int textX = (int)(cellX[0] + 2 + ICON_SZ + 3); // X relative to each cell below
        for (int i = 0; i < 23; i++) {
            int cx = cellX[i]; int cy = cellY[i];
            boolean membersLocked = i >= 15 && !memberPlayer;
            int lvlX = cx + 2 + ICON_SZ + 3;
            int lvlY = cy + CELL_H - 4;

            // Level number — large, prominent
            font.getData().setScale(0.92f);
            font.setColor(membersLocked ? 0.45f : 1.0f,
                          membersLocked ? 0.45f : 0.88f,
                          membersLocked ? 0.45f : 0.10f, 1f);
            font.draw(batch, String.valueOf(skillLevels[i]), lvlX, lvlY);

            if (membersLocked) {
                // Subtle "M" badge for members-locked skills
                font.getData().setScale(0.50f);
                font.setColor(0.75f, 0.62f, 0.18f, 0.80f);
                font.draw(batch, "M", cx + CELL_W - 9, cy + CELL_H - 2);
            }
        }

        // Total Level cell (bottom-right)
        {
            int totalLevel = 0;
            for (int lvl : skillLevels) totalLevel += lvl;
            int cx = cellX[23]; int cy = cellY[23];
            font.getData().setScale(0.58f);
            font.setColor(0.82f, 0.75f, 0.45f, 1f);
            font.draw(batch, "Total", cx + 4, cy + CELL_H - 3);
            font.getData().setScale(0.80f);
            font.setColor(1f, 0.88f, 0.10f, 1f);
            font.draw(batch, String.valueOf(totalLevel), cx + 4, cy + CELL_H - 16);
        }

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();

        // -- Tooltip on hover --
        for (int i = 0; i < 23; i++) {
            if (mouseX >= cellX[i] && mouseX < cellX[i] + CELL_W
             && mouseY >= cellY[i] && mouseY < cellY[i] + CELL_H) {
                renderSkillTooltip(sr, batch, font, proj, i, mouseX, mouseY, screenW, screenH);
                break;
            }
        }
    }

    /**
     * Draws a per-skill icon symbol inside a sz×sz box at (x, y).
     * The icon background is drawn by the caller; this draws the symbol on top.
     * All coordinates are relative to the box's bottom-left corner.
     */
    private void drawSkillIcon(ShapeRenderer sr, int skillIdx, float x, float y, float sz) {
        float h = sz, w = sz;
        switch (skillIdx) {
            case 0: // Attack — sword: blade + crossguard + pommel
                sr.rect(x + w*0.41f, y + h*0.06f, w*0.18f, h*0.74f);
                sr.rect(x + w*0.10f, y + h*0.52f, w*0.80f, h*0.14f);
                sr.rect(x + w*0.36f, y + h*0.80f, w*0.28f, h*0.14f);
                break;
            case 1: // Strength — dumbbell
                sr.circle(x + w*0.22f, y + h*0.50f, h*0.19f);
                sr.rect(x + w*0.20f, y + h*0.43f, w*0.60f, h*0.14f);
                sr.circle(x + w*0.78f, y + h*0.50f, h*0.19f);
                break;
            case 2: // Defence — shield body + point
                sr.rect(x + w*0.18f, y + h*0.28f, w*0.64f, h*0.50f);
                sr.rect(x + w*0.28f, y + h*0.10f, w*0.44f, h*0.22f);
                sr.rect(x + w*0.38f, y + h*0.75f, w*0.24f, h*0.15f);
                sr.rect(x + w*0.45f, y + h*0.88f, w*0.10f, h*0.10f);
                break;
            case 3: // Hitpoints — medical cross
                sr.rect(x + w*0.38f, y + h*0.06f, w*0.24f, h*0.88f);
                sr.rect(x + w*0.06f, y + h*0.38f, w*0.88f, h*0.24f);
                break;
            case 4: // Ranged — arrow pointing right
                sr.rect(x + w*0.06f, y + h*0.44f, w*0.60f, h*0.12f);
                sr.rect(x + w*0.06f, y + h*0.28f, w*0.14f, h*0.14f); // top feather
                sr.rect(x + w*0.06f, y + h*0.58f, w*0.14f, h*0.14f); // bot feather
                sr.rect(x + w*0.62f, y + h*0.30f, w*0.18f, h*0.40f); // arrowhead back
                sr.rect(x + w*0.76f, y + h*0.37f, w*0.18f, h*0.26f); // arrowhead mid
                break;
            case 5: // Magic — wand with star rays
                sr.rect(x + w*0.44f, y + h*0.08f, w*0.12f, h*0.58f);
                sr.rect(x + w*0.08f, y + h*0.44f, w*0.84f, h*0.12f);
                sr.circle(x + w*0.50f, y + h*0.72f, h*0.16f);
                break;
            case 6: // Prayer — cross
                sr.rect(x + w*0.41f, y + h*0.06f, w*0.18f, h*0.88f);
                sr.rect(x + w*0.14f, y + h*0.30f, w*0.72f, h*0.18f);
                break;
            case 7: // Woodcutting — axe head + handle
                sr.rect(x + w*0.44f, y + h*0.06f, w*0.12f, h*0.88f); // handle
                sr.rect(x + w*0.18f, y + h*0.12f, w*0.42f, h*0.44f); // blade body
                sr.rect(x + w*0.48f, y + h*0.06f, w*0.16f, h*0.14f); // blade hook
                break;
            case 8: // Fishing — rod + vertical line + hook
                sr.rect(x + w*0.16f, y + h*0.10f, w*0.10f, h*0.78f); // rod
                sr.rect(x + w*0.22f, y + h*0.82f, w*0.58f, h*0.08f); // rod tip
                sr.rect(x + w*0.74f, y + h*0.12f, w*0.08f, h*0.72f); // line
                sr.circle(x + w*0.78f, y + h*0.15f, h*0.09f);         // lure
                break;
            case 9: // Cooking — flame + base
                sr.rect(x + w*0.30f, y + h*0.04f, w*0.40f, h*0.62f); // center flame
                sr.rect(x + w*0.14f, y + h*0.20f, w*0.24f, h*0.46f); // left flame
                sr.rect(x + w*0.62f, y + h*0.24f, w*0.24f, h*0.42f); // right flame
                sr.rect(x + w*0.12f, y + h*0.66f, w*0.76f, h*0.18f); // base/pot
                break;
            case 10: // Mining — pickaxe head + handle
                sr.rect(x + w*0.42f, y + h*0.22f, w*0.10f, h*0.72f); // handle
                sr.rect(x + w*0.06f, y + h*0.10f, w*0.66f, h*0.18f); // pick head
                sr.rect(x + w*0.06f, y + h*0.10f, w*0.18f, h*0.36f); // sharp pick point
                break;
            case 11: // Smithing — hammer
                sr.rect(x + w*0.43f, y + h*0.32f, w*0.14f, h*0.62f); // handle
                sr.rect(x + w*0.14f, y + h*0.06f, w*0.72f, h*0.30f); // hammer head
                break;
            case 12: // Firemaking — three flame tips + log
                sr.rect(x + w*0.32f, y + h*0.06f, w*0.36f, h*0.58f); // center flame
                sr.rect(x + w*0.12f, y + h*0.22f, w*0.24f, h*0.42f); // left flame
                sr.rect(x + w*0.64f, y + h*0.26f, w*0.24f, h*0.38f); // right flame
                sr.rect(x + w*0.08f, y + h*0.66f, w*0.84f, h*0.16f); // log
                break;
            case 13: // Crafting — needle body + eye
                sr.rect(x + w*0.64f, y + h*0.08f, w*0.12f, h*0.78f); // needle
                sr.circle(x + w*0.70f, y + h*0.82f, h*0.09f);         // eye hole
                sr.rect(x + w*0.14f, y + h*0.75f, w*0.54f, h*0.10f); // thread
                break;
            case 14: // Runecrafting — stylised rune (diamond)
                sr.rect(x + w*0.20f, y + h*0.38f, w*0.60f, h*0.24f); // horiz
                sr.rect(x + w*0.38f, y + h*0.20f, w*0.24f, h*0.60f); // vert
                sr.rect(x + w*0.28f, y + h*0.28f, w*0.44f, h*0.44f); // fill diamond
                break;
            case 15: // Fletching — arrow + large feathers
                sr.rect(x + w*0.10f, y + h*0.46f, w*0.68f, h*0.08f); // shaft
                sr.rect(x + w*0.70f, y + h*0.32f, w*0.20f, h*0.36f); // arrowhead
                sr.rect(x + w*0.06f, y + h*0.24f, w*0.16f, h*0.20f); // feather top
                sr.rect(x + w*0.06f, y + h*0.56f, w*0.16f, h*0.20f); // feather bot
                sr.rect(x + w*0.04f, y + h*0.30f, w*0.08f, h*0.40f); // feather center
                break;
            case 16: // Agility — stylised running figure
                sr.circle(x + w*0.60f, y + h*0.76f, h*0.14f);         // head
                sr.rect(x + w*0.30f, y + h*0.32f, w*0.40f, h*0.36f); // torso
                sr.rect(x + w*0.08f, y + h*0.48f, w*0.24f, h*0.30f); // back arm
                sr.rect(x + w*0.66f, y + h*0.22f, w*0.24f, h*0.28f); // fwd arm
                sr.rect(x + w*0.14f, y + h*0.06f, w*0.20f, h*0.28f); // back leg
                sr.rect(x + w*0.62f, y + h*0.06f, w*0.20f, h*0.28f); // fwd leg
                break;
            case 17: // Herblore — flask: cap + neck + body
                sr.rect(x + w*0.35f, y + h*0.04f, w*0.30f, h*0.10f); // cap
                sr.rect(x + w*0.40f, y + h*0.12f, w*0.20f, h*0.22f); // neck
                sr.rect(x + w*0.22f, y + h*0.32f, w*0.56f, h*0.58f); // body
                break;
            case 18: // Thieving — coin bag
                sr.circle(x + w*0.50f, y + h*0.38f, h*0.30f);
                sr.rect(x + w*0.36f, y + h*0.64f, w*0.28f, h*0.14f);
                sr.rect(x + w*0.30f, y + h*0.75f, w*0.40f, h*0.12f);
                break;
            case 19: // Slayer — simplified skull
                sr.circle(x + w*0.50f, y + h*0.56f, h*0.30f);
                sr.rect(x + w*0.22f, y + h*0.40f, w*0.16f, h*0.20f); // left eye dark
                sr.rect(x + w*0.62f, y + h*0.40f, w*0.16f, h*0.20f); // right eye dark
                sr.rect(x + w*0.22f, y + h*0.06f, w*0.56f, h*0.18f); // jaw
                break;
            case 20: // Farming — plant stem + two leaves
                sr.rect(x + w*0.46f, y + h*0.04f, w*0.08f, h*0.68f); // stem
                sr.rect(x + w*0.18f, y + h*0.44f, w*0.30f, h*0.16f); // leaf left
                sr.rect(x + w*0.52f, y + h*0.28f, w*0.30f, h*0.16f); // leaf right
                sr.rect(x + w*0.28f, y + h*0.68f, w*0.44f, h*0.20f); // pot/ground
                break;
            case 21: // Hunter — paw print
                sr.circle(x + w*0.50f, y + h*0.32f, h*0.22f);
                sr.circle(x + w*0.26f, y + h*0.60f, h*0.11f);
                sr.circle(x + w*0.42f, y + h*0.68f, h*0.10f);
                sr.circle(x + w*0.58f, y + h*0.68f, h*0.10f);
                sr.circle(x + w*0.74f, y + h*0.60f, h*0.11f);
                break;
            case 22: // Construction — brick wall (2×2 bricks)
                sr.rect(x + w*0.06f, y + h*0.52f, w*0.88f, h*0.08f); // mortar
                sr.rect(x + w*0.06f, y + h*0.06f, w*0.40f, h*0.44f); // top-left brick
                sr.rect(x + w*0.54f, y + h*0.06f, w*0.40f, h*0.44f); // top-right brick
                sr.rect(x + w*0.06f, y + h*0.62f, w*0.40f, h*0.32f); // bot-left brick
                sr.rect(x + w*0.54f, y + h*0.62f, w*0.40f, h*0.32f); // bot-right brick
                break;
            default:
                sr.rect(x + w*0.20f, y + h*0.20f, w*0.60f, h*0.60f);
                break;
        }
    }

    /**
     * Renders an OSRS-style tooltip to the left of the side panel showing:
     *  - Skill name
     *  - Current XP (formatted with commas)
     *  - Remaining XP to next level + progress bar
     */
    private void renderSkillTooltip(ShapeRenderer sr, SpriteBatch batch, BitmapFont font,
                                     Matrix4 proj, int skillIdx, int mouseX, int mouseY,
                                     int screenW, int screenH) {
        int  level       = skillLevels[skillIdx];
        long currentXp   = skillXp[skillIdx];
        long xpThisLevel = xpForLevel(level);
        long xpNextLevel = xpForLevel(level + 1);
        long remaining   = (level >= 99) ? 0L : Math.max(0L, xpNextLevel - currentXp);
        float progress   = (level >= 99) ? 1f
                         : (xpNextLevel <= xpThisLevel) ? 1f
                         : (float)(currentXp - xpThisLevel) / (float)(xpNextLevel - xpThisLevel);
        progress = Math.min(1f, Math.max(0f, progress));

        final int TIP_W = 215;
        final int TIP_H = 80;
        final int T_PAD = 8;
        final int BAR_H = 6;

        int tipX = panelX - TIP_W - 10;
        int tipY = mouseY - TIP_H / 2;
        if (tipX < 4)                    tipX = 4;
        if (tipY < 4)                    tipY = 4;
        if (tipY + TIP_H > screenH - 4)  tipY = screenH - TIP_H - 4;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Background + progress bar fill
        sr.setProjectionMatrix(proj);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.07f, 0.06f, 0.05f, 0.96f);
        sr.rect(tipX, tipY, TIP_W, TIP_H);
        // Progress bar track
        sr.setColor(0.18f, 0.14f, 0.04f, 1f);
        sr.rect(tipX + T_PAD, tipY + T_PAD, TIP_W - T_PAD * 2, BAR_H);
        // Progress bar fill (gold)
        sr.setColor(0.85f, 0.70f, 0.10f, 1f);
        sr.rect(tipX + T_PAD, tipY + T_PAD, (TIP_W - T_PAD * 2) * progress, BAR_H);
        sr.end();

        // Border
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(0.75f, 0.62f, 0.10f, 1f);
        sr.rect(tipX, tipY, TIP_W, TIP_H);
        sr.end();

        batch.setProjectionMatrix(proj);
        batch.begin();
        font.getData().setScale(0.85f);

        // Skill name (gold, bold appearance via colour)
        font.setColor(1f, 0.85f, 0.10f, 1f);
        String skillName = SKILL_NAMES[skillIdx] + (skillIdx >= 15 ? " (Members)" : "");
        font.draw(batch, skillName, tipX + T_PAD, tipY + TIP_H - T_PAD);

        // Current XP
        font.setColor(Color.WHITE);
        font.draw(batch, "XP: " + String.format("%,d", currentXp),
                  tipX + T_PAD, tipY + TIP_H - T_PAD - 18);

        // Remaining XP / max level
        if (level >= 99) {
            font.setColor(1f, 0.85f, 0.10f, 1f);
            font.draw(batch, "MAX LEVEL", tipX + T_PAD, tipY + TIP_H - T_PAD - 36);
        } else {
            font.setColor(0.75f, 0.75f, 0.75f, 1f);
            font.draw(batch, "Remaining XP: " + String.format("%,d", remaining),
                      tipX + T_PAD, tipY + TIP_H - T_PAD - 36);
        }

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private void renderCharacterTab(ShapeRenderer sr, SpriteBatch batch, BitmapFont font, Matrix4 proj) {
        final int pad = 8;
        final int headerY = panelY + CONTENT_H - 8;
        final int subY = panelY + CONTENT_H - 34;
        final int subW = (PANEL_W - pad * 5) / 4;
        final int subH = 18;

        // Title
        batch.setProjectionMatrix(proj);
        batch.begin();
        font.getData().setScale(0.85f);
        font.setColor(new Color(0.9f, 0.80f, 0.50f, 1f));
        font.draw(batch, "Character Summary", panelX + pad, headerY);
        font.getData().setScale(1f);
        batch.end();

        // Sub-nav buttons
        sr.setProjectionMatrix(proj);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < 4; i++) {
            int bx = panelX + pad + i * (subW + pad);
            boolean active = characterPage.ordinal() == i;
            sr.setColor(active ? new Color(0.32f, 0.25f, 0.06f, 1f) : new Color(0.13f, 0.12f, 0.10f, 1f));
            sr.rect(bx, subY, subW, subH);
        }
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < 4; i++) {
            int bx = panelX + pad + i * (subW + pad);
            boolean active = characterPage.ordinal() == i;
            sr.setColor(active ? new Color(1f, 0.85f, 0.10f, 1f) : new Color(0.40f, 0.36f, 0.26f, 1f));
            sr.rect(bx, subY, subW, subH);
        }
        sr.end();

        batch.begin();
        font.getData().setScale(0.72f);
        String[] labels = {"Summary", "Quests", "Gear", "Friends"};
        for (int i = 0; i < 4; i++) {
            int bx = panelX + pad + i * (subW + pad);
            font.setColor(characterPage.ordinal() == i ? new Color(1f, 0.90f, 0.10f, 1f) : Color.WHITE);
            font.draw(batch, labels[i], bx + 3, subY + 12);
        }

        int bodyTop = subY - 8;
        if (characterPage == CharacterPage.SUMMARY) {
            font.getData().setScale(0.8f);
            font.setColor(Color.WHITE);
            font.draw(batch, "Quest points: " + playerQuestPoints, panelX + pad, bodyTop);
            font.draw(batch, "Completed quests: " + countCompletedQuests(), panelX + pad, bodyTop - 18);
            font.draw(batch, "Total tracked quests: " + quests.size(), panelX + pad, bodyTop - 36);
            font.setColor(0.8f, 0.75f, 0.65f, 1f);
            font.getData().setScale(0.7f);
            font.draw(batch, "Open Quests to see progress.", panelX + pad, bodyTop - 60);

            // Logout button
            int bx = logoutButtonX();
            int by = logoutButtonY();
            int bw = logoutButtonW();
            int bh = logoutButtonH();

            batch.end();
            sr.setProjectionMatrix(proj);
            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(0.22f, 0.10f, 0.08f, 1f);
            sr.rect(bx, by, bw, bh);
            sr.end();

            sr.begin(ShapeRenderer.ShapeType.Line);
            sr.setColor(0.85f, 0.35f, 0.25f, 1f);
            sr.rect(bx, by, bw, bh);
            sr.end();

            batch.begin();
            font.getData().setScale(0.75f);
            font.setColor(1f, 0.8f, 0.6f, 1f);
            font.draw(batch, "Logout", bx + 10, by + 14);
        } else if (characterPage == CharacterPage.QUEST_LIST) {
            QuestView sel = selectedQuestId != -1 ? quests.get(selectedQuestId) : null;
            if (sel == null) {
                // -- Quest list --
                List<QuestView> list = sortedQuests();
                int y = bodyTop;
                font.getData().setScale(0.78f);
                for (QuestView q : list) {
                    if (y < panelY + 18) break;
                    font.setColor(colorForQuest(q.status));
                    font.draw(batch, q.questName, panelX + pad, y);
                    y -= 16;
                }
                font.setColor(0.75f, 0.75f, 0.75f, 1f);
                font.getData().setScale(0.65f);
                font.draw(batch, "Red: not started  Yellow: in progress  Green: complete", panelX + pad, panelY + 10);
            } else {
                // -- Quest detail --
                int y = bodyTop;

                // Back button
                font.getData().setScale(0.75f);
                font.setColor(0.85f, 0.75f, 0.40f, 1f);
                font.draw(batch, "< Back", panelX + pad, y);
                y -= 22;

                // Quest name
                font.getData().setScale(0.85f);
                font.setColor(colorForQuest(sel.status));
                font.draw(batch, sel.questName, panelX + pad, y);
                y -= 20;

                // Description (up to 2 lines, ~32 chars each)
                font.getData().setScale(0.70f);
                font.setColor(0.85f, 0.82f, 0.72f, 1f);
                String desc = sel.description != null ? sel.description : "";
                if (desc.length() > 32) {
                    int cut = desc.lastIndexOf(' ', 32);
                    if (cut < 1) cut = 32;
                    font.draw(batch, desc.substring(0, cut), panelX + pad, y);
                    y -= 14;
                    String rest = desc.substring(cut).trim();
                    if (!rest.isEmpty() && y >= panelY + 18) {
                        String line2 = rest.length() > 32 ? rest.substring(0, 32) + "..." : rest;
                        font.draw(batch, line2, panelX + pad, y);
                        y -= 14;
                    }
                } else if (!desc.isEmpty()) {
                    font.draw(batch, desc, panelX + pad, y);
                    y -= 14;
                }

                // Tasks header
                y -= 4;
                font.getData().setScale(0.72f);
                font.setColor(0.90f, 0.85f, 0.55f, 1f);
                font.draw(batch, "Tasks:", panelX + pad, y);
                y -= 16;

                // Task rows
                for (QuestTaskView t : sel.tasks) {
                    if (y < panelY + 18) break;
                    String prefix = t.completed ? "[x] " : "[ ] ";
                    String progress = t.requiredCount > 1
                        ? " (" + t.currentCount + "/" + t.requiredCount + ")" : "";
                    font.setColor(t.completed
                        ? new Color(0.45f, 0.85f, 0.45f, 1f) : Color.WHITE);
                    font.draw(batch, prefix + t.description + progress, panelX + pad, y);
                    y -= 16;
                }

                // Quest point reward at bottom
                font.getData().setScale(0.65f);
                font.setColor(0.75f, 0.75f, 0.75f, 1f);
                font.draw(batch, "Reward: " + sel.questPointsReward + " Quest Point(s)",
                    panelX + pad, panelY + 10);
            }
        } else if (characterPage == CharacterPage.GEAR) {
            final int SLOT_SIZE = 36;
            final int toggleY   = panelY + 22;
            final int toggleH   = 14;
            final int half      = PANEL_W / 2;

            // -- Slots / Stats toggle --
            batch.end();
            sr.setProjectionMatrix(proj);
            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(!gearShowStats
                ? new Color(0.32f, 0.25f, 0.06f, 1f)
                : new Color(0.13f, 0.12f, 0.10f, 1f));
            sr.rect(panelX, toggleY, half - 1, toggleH);
            sr.setColor(gearShowStats
                ? new Color(0.32f, 0.25f, 0.06f, 1f)
                : new Color(0.13f, 0.12f, 0.10f, 1f));
            sr.rect(panelX + half, toggleY, half, toggleH);
            sr.end();
            sr.begin(ShapeRenderer.ShapeType.Line);
            sr.setColor(new Color(0.40f, 0.36f, 0.26f, 1f));
            sr.rect(panelX, toggleY, half - 1, toggleH);
            sr.rect(panelX + half, toggleY, half, toggleH);
            sr.end();
            batch.setProjectionMatrix(proj);
            batch.begin();
            font.getData().setScale(0.65f);
            font.setColor(!gearShowStats ? new Color(1f, 0.90f, 0.10f, 1f) : Color.WHITE);
            font.draw(batch, "Equipment", panelX + 18, toggleY + 11);
            font.setColor(gearShowStats ? new Color(1f, 0.90f, 0.10f, 1f) : Color.WHITE);
            font.draw(batch, "Bonuses", panelX + half + 22, toggleY + 11);

            if (!gearShowStats) {
                // -- Visual slot grid --
                String[] slotAbbrev = {
                    "Head","Cape","Neck","Ammo","Weapon","Shield",
                    "Body","Legs","Hands","Feet","Ring"
                };
                int[][] slotPos = getGearSlotPositions();

                batch.end();
                sr.begin(ShapeRenderer.ShapeType.Filled);
                for (int i = 0; i < 11; i++) {
                    int sx = slotPos[i][0], sy = slotPos[i][1];
                    boolean filled = equippedIds[i] > 0;
                    sr.setColor(filled
                        ? new Color(0.18f, 0.15f, 0.09f, 1f)
                        : new Color(0.10f, 0.09f, 0.07f, 1f));
                    sr.rect(sx, sy, SLOT_SIZE, SLOT_SIZE);
                    if (filled) {
                        // tier-colour swatch in centre of slot
                        Color ic = itemTierColor(equippedNames[i]);
                        sr.setColor(ic);
                        sr.rect(sx + 13, sy + 13, 10, 10);
                    }
                }
                sr.end();
                sr.begin(ShapeRenderer.ShapeType.Line);
                for (int i = 0; i < 11; i++) {
                    int sx = slotPos[i][0], sy = slotPos[i][1];
                    boolean filled = equippedIds[i] > 0;
                    sr.setColor(filled
                        ? new Color(0.80f, 0.65f, 0.20f, 1f)
                        : new Color(0.35f, 0.32f, 0.22f, 1f));
                    sr.rect(sx, sy, SLOT_SIZE, SLOT_SIZE);
                }
                sr.end();
                batch.setProjectionMatrix(proj);
                batch.begin();
                font.getData().setScale(0.58f);
                for (int i = 0; i < 11; i++) {
                    int sx = slotPos[i][0], sy = slotPos[i][1];
                    if (equippedIds[i] > 0) {
                        font.setColor(Color.WHITE);
                        String n = equippedNames[i];
                        if (n.length() > 11) n = n.substring(0, 11);
                        font.draw(batch, n, sx + 2, sy + 10);
                    } else {
                        font.setColor(new Color(0.40f, 0.38f, 0.28f, 1f));
                        font.draw(batch, slotAbbrev[i], sx + 3, sy + 22);
                    }
                }
                font.getData().setScale(0.58f);
                font.setColor(new Color(0.60f, 0.55f, 0.35f, 1f));
                font.draw(batch, "Click slot to unequip", panelX + 8, panelY + 14);

            } else {
                // -- Bonus stats view --
                int sy2 = bodyTop - 4;
                final int LH = 14;
                final int col2 = panelX + 8 + 108;

                batch.end();
                batch.setProjectionMatrix(proj);
                batch.begin();
                String[] atkLbls   = {"Stab",   "Slash",  "Crush",  "Magic",  "Ranged"};
                String[] defLbls   = {"Stab",   "Slash",  "Crush",  "Magic",  "Ranged"};
                String[] otherLbls = {"Melee Str", "Range Str", "Magic Dmg", "Prayer"};
                int[] atkIdx   = {0, 1, 2, 3, 4};
                int[] defIdx   = {5, 6, 7, 8, 9};
                int[] otherIdx = {10, 11, 12, 13};

                // Attack bonuses
                font.getData().setScale(0.68f);
                font.setColor(new Color(1f, 0.75f, 0.20f, 1f));
                font.draw(batch, "Attack Bonuses", panelX + 8, sy2);
                sy2 -= LH;
                font.getData().setScale(0.63f);
                for (int i = 0; i < 5; i++) {
                    int v = equipBonuses[atkIdx[i]];
                    font.setColor(new Color(0.75f, 0.70f, 0.50f, 1f));
                    font.draw(batch, atkLbls[i], panelX + 8, sy2);
                    font.setColor(v >= 0
                        ? new Color(0.40f, 0.85f, 0.40f, 1f)
                        : new Color(0.85f, 0.40f, 0.40f, 1f));
                    font.draw(batch, (v >= 0 ? "+" : "") + v, col2, sy2);
                    sy2 -= LH;
                }
                sy2 -= 4;
                // Defence bonuses
                font.getData().setScale(0.68f);
                font.setColor(new Color(1f, 0.75f, 0.20f, 1f));
                font.draw(batch, "Defence Bonuses", panelX + 8, sy2);
                sy2 -= LH;
                font.getData().setScale(0.63f);
                for (int i = 0; i < 5; i++) {
                    int v = equipBonuses[defIdx[i]];
                    font.setColor(new Color(0.75f, 0.70f, 0.50f, 1f));
                    font.draw(batch, defLbls[i], panelX + 8, sy2);
                    font.setColor(v >= 0
                        ? new Color(0.40f, 0.85f, 0.40f, 1f)
                        : new Color(0.85f, 0.40f, 0.40f, 1f));
                    font.draw(batch, (v >= 0 ? "+" : "") + v, col2, sy2);
                    sy2 -= LH;
                }
                sy2 -= 4;
                // Other bonuses
                font.getData().setScale(0.68f);
                font.setColor(new Color(1f, 0.75f, 0.20f, 1f));
                font.draw(batch, "Other Bonuses", panelX + 8, sy2);
                sy2 -= LH;
                font.getData().setScale(0.63f);
                for (int i = 0; i < 4; i++) {
                    int v = equipBonuses[otherIdx[i]];
                    font.setColor(new Color(0.75f, 0.70f, 0.50f, 1f));
                    font.draw(batch, otherLbls[i], panelX + 8, sy2);
                    font.setColor(v >= 0
                        ? new Color(0.40f, 0.85f, 0.40f, 1f)
                        : new Color(0.85f, 0.40f, 0.40f, 1f));
                    font.draw(batch, (v >= 0 ? "+" : "") + v, col2, sy2);
                    sy2 -= LH;
                }
            }
        } else if (characterPage == CharacterPage.FRIENDS_LIST) {
            renderFriendsList(batch, font, panelX + pad, bodyTop);
        }
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private List<QuestView> sortedQuests() {
        List<QuestView> list = new ArrayList<>(quests.values());
        list.sort(Comparator.comparing(q -> q.questName));
        return list;
    }

    private void renderFriendsList(SpriteBatch batch, BitmapFont font, int leftX, int topY) {
        font.getData().setScale(0.78f);
        font.setColor(new Color(0.90f, 0.85f, 0.55f, 1f));
        font.draw(batch, "Friends List", leftX, topY);

        int y = topY - 18;
        font.getData().setScale(0.68f);
        if (friendEntries.isEmpty()) {
            font.setColor(Color.WHITE);
            font.draw(batch, "No friends added yet.", leftX, y);
            font.draw(batch, "Right-click players to add.", leftX, y - FRIEND_ROW_H);
            return;
        }

        for (FriendEntryView entry : friendEntries) {
            if (y < panelY + 16) break;
            font.setColor(entry.online ? new Color(0.50f, 0.95f, 0.50f, 1f) : new Color(0.72f, 0.72f, 0.72f, 1f));
            String status = entry.online ? "Online" : "Offline";
            font.draw(batch, entry.name + " - " + status, leftX, y);
            font.setColor(new Color(0.95f, 0.45f, 0.45f, 1f));
            int removeX = panelX + PANEL_W - 8 - FRIEND_REMOVE_W;
            font.draw(batch, "Remove", removeX, y);
            y -= FRIEND_ROW_H;
        }
    }

    private int countCompletedQuests() {
        int done = 0;
        for (QuestView q : quests.values()) {
            if (q.status == QuestStatus.COMPLETED) done++;
        }
        return done;
    }

    private Color colorForQuest(QuestStatus status) {
        return switch (status) {
            case COMPLETED -> new Color(0.30f, 0.90f, 0.30f, 1f);
            case IN_PROGRESS -> new Color(0.95f, 0.85f, 0.20f, 1f);
            case NOT_STARTED -> new Color(0.92f, 0.33f, 0.33f, 1f);
        };
    }

    // -----------------------------------------------------------------------
    // Input handling
    // -----------------------------------------------------------------------

    /**
     * Handle a left-click.
     * Returns the new combat style index (0–3) if a combat style button was
     * clicked, or -1 for everything else (tab switches, inventory clicks, etc.).
     */
    public int handleLeftClick(int mx, int my) {
        if (!isOverPanel(mx, my)) return -1;

        // ── Tab bar click ──────────────────────────────────────────────────
        if (my >= panelY + CONTENT_H) {
            Tab[] row = my >= panelY + CONTENT_H + TAB_H ? TOP_TABS : BOTTOM_TABS;
            int tW = PANEL_W / row.length;
            int idx = Math.min((mx - panelX) / tW, row.length - 1);
            if (idx >= 0) activeTab = row[idx];
            return -1;
        }

        // ── Content area click ─────────────────────────────────────────────
        switch (activeTab) {
            case COMBAT -> {
                int pad  = 10;
                int btnW = (PANEL_W - pad * 3) / 2;
                int btnH = 70;
                int gridTop = panelY + CONTENT_H - 38;

                String[] names = styleNames();

                for (int i = 0; i < 4; i++) {
                    if (names[i] == null) continue;
                    int col = i % 2;
                    int row = i / 2;
                    int bx  = panelX + pad + col * (btnW + pad);
                    int by  = gridTop - (row + 1) * (btnH + pad);

                    if (mx >= bx && mx <= bx + btnW && my >= by && my <= by + btnH) {
                        combatStyle = i;
                        return i;
                    }
                }

                int toggleY  = gridTop - 2 * (btnH + pad) - 6;
                int toggleSz = 14;
                int toggleX  = panelX + pad;
                if (mx >= toggleX && mx <= panelX + PANEL_W - pad
                    && my >= toggleY && my <= toggleY + toggleSz + 4) {
                    autoRetaliate = !autoRetaliate;
                    return -50;
                }
            }
            case EQUIPMENT -> {
                int toggleY = panelY + 22;
                int toggleH = 14;
                if (my >= toggleY && my <= toggleY + toggleH) {
                    gearShowStats = mx >= panelX + PANEL_W / 2;
                    return -1;
                }
                if (!gearShowStats) {
                    int[][] slotPos = getGearSlotPositions();
                    final int SLOT_SIZE = 36;
                    for (int i = 0; i < 11; i++) {
                        int sx = slotPos[i][0], sy = slotPos[i][1];
                        if (mx >= sx && mx <= sx + SLOT_SIZE && my >= sy && my <= sy + SLOT_SIZE) {
                            return -(100 + i);
                        }
                    }
                }
            }
            case QUESTS -> {
                int pad = 8;
                int subY = panelY + CONTENT_H - 34;
                int y = subY - 8;
                if (selectedQuestId != -1) {
                    if (mx >= panelX + pad && mx <= panelX + PANEL_W - pad
                        && my <= y && my >= y - 16) {
                        selectedQuestId = -1;
                        return -1;
                    }
                } else {
                    List<QuestView> list = sortedQuests();
                    for (QuestView q : list) {
                        if (mx >= panelX + pad && mx <= panelX + PANEL_W - pad
                            && my <= y && my >= y - 16) {
                            selectedQuestId = q.questId;
                            return -1;
                        }
                        y -= 16;
                        if (y < panelY + 18) break;
                    }
                }
            }
            case FRIENDS -> {
                int subY = panelY + CONTENT_H - 34;
                int bodyTop = subY - 8;
                int y = bodyTop - 18;
                int removeX = panelX + PANEL_W - 8 - FRIEND_REMOVE_W;
                for (FriendEntryView entry : friendEntries) {
                    if (y < panelY + 16) break;
                    if (mx >= removeX && mx <= removeX + FRIEND_REMOVE_W
                        && my >= y - FRIEND_ROW_H + 2 && my <= y + 2) {
                        removeFriendRequestedId = entry.playerId;
                        return -1;
                    }
                    y -= FRIEND_ROW_H;
                }
            }
            case SETTINGS -> {
                if (mx >= logoutButtonX() && mx <= logoutButtonX() + logoutButtonW()
                    && my >= logoutButtonY() && my <= logoutButtonY() + logoutButtonH()) {
                    logoutRequested = true;
                    return -1;
                }
            }
            case PRAYER -> {
                int prayerLevel = skillLevels[6];
                int startY = panelY + CONTENT_H - 28;
                final int ROW_H = 36;
                for (int i = 0; i < PRAYERS.length; i++) {
                    int prayerId = (int) PRAYERS[i][0];
                    int rowY = startY - (i + 1) * ROW_H + 2;
                    if (mx >= panelX + 8 && mx <= panelX + PANEL_W - 8
                     && my >= rowY && my <= rowY + ROW_H - 2) {
                        return -(200 + prayerId);   // GameScreen decodes: prayerId = -(ret+200)
                    }
                }
            }
            case INVENTORY -> inventoryUI.handleMouseDown(mx, my, 0);
            default -> { /* Skills tab: no clicks */ }
        }
        return -1;
    }

    /** Returns true if (mx, my) is anywhere within the side panel. */
    public boolean isOverPanel(int mx, int my) {
        return mx >= panelX && mx <= panelX + PANEL_W
            && my >= panelY && my <= panelY + TOTAL_H;
    }

    public boolean isInventoryTabActive() { return activeTab == Tab.INVENTORY; }
    public int     getPanelX()            { return panelX; }
    public boolean consumeLogoutRequested() {
        boolean out = logoutRequested;
        logoutRequested = false;
        return out;
    }

    // -----------------------------------------------------------------------
    // Drag support (inventory)
    // -----------------------------------------------------------------------

    public void updateDrag(int mx, int my)    { inventoryUI.updateDrag(mx, my); }
    public boolean isInventoryDragging()      { return inventoryUI.isDragging(); }
    public int[]   handleInventoryMouseUp(int mx, int my) { return inventoryUI.handleMouseUp(mx, my); }
    public int     getInventoryRightClickSlot(int mx, int my) { return inventoryUI.getRightClickSlot(mx, my); }
    public void setSelectedInventorySlot(int slot) { inventoryUI.setSelectedSlot(slot); }
    public void setInventoryHoveredSlot(int slot) { inventoryUI.setHoveredSlot(slot); }
    public int  getInventorySlotAt(int mx, int my)  { return inventoryUI.getSlotAt(mx, my); }

    // -----------------------------------------------------------------------
    // Data setters
    // -----------------------------------------------------------------------

    public void setInventorySlot(int slot, int itemId, int qty, String name, int flags) {
        inventoryUI.setSlot(slot, itemId, qty, name, flags);
    }

    public void setSkillData(int[] levels, long[] xp) {
        int n = Math.min(levels.length, skillLevels.length);
        System.arraycopy(levels, 0, skillLevels, 0, n);
        System.arraycopy(xp, 0, skillXp, 0, n);
    }

    public void setEquipmentSlot(int slot, int itemId, String name) {
        if (slot >= 0 && slot < 11) {
            equippedIds[slot] = itemId;
            equippedNames[slot] = name != null ? name : "";
        }
    }

    public void setEquipBonuses(int[] bonuses) {
        if (bonuses != null && bonuses.length == 14)
            System.arraycopy(bonuses, 0, this.equipBonuses, 0, 14);
    }

    public void setMember(boolean isMember) { this.memberPlayer = isMember; }

    public void setCombatStyle(int style) {
        if (style >= 0 && style < 4) combatStyle = style;
    }

    public void setQuestState(QuestView questView, int totalQuestPoints) {
        quests.put(questView.questId, questView);
        playerQuestPoints = Math.max(0, totalQuestPoints);
    }

    public void setFriendsList(List<FriendEntryView> entries) {
        friendEntries.clear();
        if (entries != null) friendEntries.addAll(entries);
    }

    public long consumeRemoveFriendRequestedId() {
        long id = removeFriendRequestedId;
        removeFriendRequestedId = -1L;
        return id;
    }

    // -----------------------------------------------------------------------
    // Accessors for GameScreen
    // -----------------------------------------------------------------------

    public int    getInventoryItemId(int slot)    { return inventoryUI.getItemId(slot); }
    public String getInventoryItemName(int slot)  { return inventoryUI.getName(slot); }
    public int    getInventoryItemFlags(int slot) { return inventoryUI.getFlags(slot); }
    public int    getCombatStyle()                { return combatStyle; }

    private int logoutButtonX() { return panelX + 8; }
    private int logoutButtonY() { return panelY + 8; }
    private int logoutButtonW() { return 72; }
    private int logoutButtonH() { return 20; }

    /**
     * Returns screen [x, y] (bottom-left corner of each 36x36 slot box) for all
     * 11 equipment slots in EquipmentSlot order (HEAD=0 ... RING=10).
     * Mirrors the OSRS worn-equipment layout.
     */
    private int[][] getGearSlotPositions() {
        final int SLOT_SIZE = 36;
        final int GAP       = 8;
        final int leftX     = panelX + 8;
        final int centerX   = panelX + (PANEL_W - SLOT_SIZE) / 2;  // 102
        final int rightX    = panelX + PANEL_W - SLOT_SIZE - 8;    // 196

        // bodyTop = panelY + CONTENT_H - 34 - 8 (matches renderCharacterTab's bodyTop)
        int bt    = panelY + CONTENT_H - 42;
        int row0Y = bt - SLOT_SIZE;                       // HEAD / CAPE / NECK
        int row1Y = row0Y - SLOT_SIZE - GAP;              // AMMO (right col only)
        int row2Y = row1Y - SLOT_SIZE - GAP;              // WEAPON / BODY / SHIELD
        int row3Y = row2Y - SLOT_SIZE - GAP;              // LEGS (centre only)
        int row4Y = row3Y - SLOT_SIZE - GAP;              // HANDS / FEET / RING

        return new int[][] {
            {centerX, row0Y},   // 0 HEAD
            {leftX,   row0Y},   // 1 CAPE
            {rightX,  row0Y},   // 2 NECK
            {rightX,  row1Y},   // 3 AMMO
            {leftX,   row2Y},   // 4 WEAPON
            {rightX,  row2Y},   // 5 SHIELD
            {centerX, row2Y},   // 6 BODY
            {centerX, row3Y},   // 7 LEGS
            {leftX,   row4Y},   // 8 HANDS
            {centerX, row4Y},   // 9 FEET
            {rightX,  row4Y},   // 10 RING
        };
    }

    private Color itemTierColor(String name) {
        if (name == null || name.isEmpty()) return new Color(0.75f, 0.78f, 0.82f, 1f);
        String n = name.toLowerCase();
        if (n.contains("dragon"))  return new Color(0.80f, 0.18f, 0.10f, 1f);
        if (n.contains("rune"))    return new Color(0.10f, 0.62f, 0.85f, 1f);
        if (n.contains("adamant")) return new Color(0.15f, 0.58f, 0.28f, 1f);
        if (n.contains("mithril")) return new Color(0.35f, 0.48f, 0.82f, 1f);
        if (n.contains("black"))   return new Color(0.22f, 0.20f, 0.25f, 1f);
        if (n.contains("steel"))   return new Color(0.60f, 0.62f, 0.72f, 1f);
        if (n.contains("iron"))    return new Color(0.52f, 0.52f, 0.54f, 1f);
        if (n.contains("bronze"))  return new Color(0.72f, 0.42f, 0.10f, 1f);
        return new Color(0.75f, 0.78f, 0.82f, 1f);
    }
}
