package com.osrs.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;
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
    public static final int PANEL_W       = 240;
    /** Width of the inner content column shared by ALL tabs (matches InventoryUI slot grid). */
    public static final int CONTENT_W     = 186;
    /** Left inset from panel edge to content column — symmetric on both sides. */
    public static final int CONTENT_INSET = (PANEL_W - CONTENT_W) / 2;
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
    private static final int FRIEND_ROW_H       = 18;   // height per friend row
    private static final int FRIEND_BTN_H       = 22;   // Add/Del button height
    private static final int FRIEND_LIST_PAD    = 34;   // reserved bottom space (buttons)
    private static final int FRIEND_SCROLLBAR_W = 7;
    private static final int FRIEND_REMOVE_W    = 46;   // pixel width of old "Remove" label (kept for compat)

    // Friends tab colours
    private static final Color FRIEND_TITLE   = new Color(0.90f, 0.82f, 0.48f, 1f);
    private static final Color FRIEND_ONLINE  = new Color(1.00f, 0.85f, 0.10f, 1f);
    private static final Color FRIEND_OFFLINE = new Color(0.75f, 0.22f, 0.22f, 1f);
    private static final Color FRIEND_SEL_BG  = new Color(0.28f, 0.22f, 0.08f, 1f);
    private static final Color FRIEND_BTN_BG  = new Color(0.16f, 0.14f, 0.10f, 1f);
    private static final Color FRIEND_BTN_BR  = new Color(0.52f, 0.44f, 0.20f, 1f);
    private static final Color FRIEND_BTN_LBL = new Color(0.88f, 0.82f, 0.58f, 1f);
    private static final Color FRIEND_BTN_DIS = new Color(0.36f, 0.34f, 0.28f, 1f);
    private static final Color OVERLAY_BG     = new Color(0.04f, 0.04f, 0.04f, 0.92f);
    private static final Color OVERLAY_BORDER = new Color(0.60f, 0.50f, 0.22f, 1f);
    private static final Color OVERLAY_INPUT  = new Color(0.10f, 0.10f, 0.08f, 1f);

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
        IGNORE    (8,  "Options"),
        CLAN      (9,  "Clan"),
        SETTINGS  (10, "Account"),
        EMOTES    (11, "Emotes"),
        MUSIC     (12, "Music"),
        LOGOUT    (13, "Logout");

        public final int    index;
        public final String label;
        Tab(int i, String l) { this.index = i; this.label = l; }
    }

    private static final Tab[] TOP_TABS = {
        Tab.COMBAT, Tab.SKILLS, Tab.QUESTS, Tab.INVENTORY, Tab.EQUIPMENT, Tab.PRAYER, Tab.MAGIC
    };
    private static final Tab[] BOTTOM_TABS = {
        Tab.CLAN, Tab.FRIENDS, Tab.SETTINGS, Tab.LOGOUT, Tab.IGNORE, Tab.EMOTES, Tab.MUSIC
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
    private boolean gearShowStats  = false;        // false=slot grid, true=bonus stats
    private float   playerWeight   = 0f;           // carried weight in kg (synced from server)
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

    // ── Weapon attack-style tables (OSRS-accurate, per weapon category) ──────
    // Category indices: 0=Unarmed 1=Axe 2=Sword/Scimitar 3=Bow 4=Default-Melee
    private static final String[][] WEAPON_STYLE_NAMES = {
        {"Punch",    "Kick",       "Block",     null},          // 0 Unarmed
        {"Chop",     "Hack",       "Smash",     "Block"},       // 1 Axe
        {"Chop",     "Slash",      "Lunge",     "Block"},       // 2 Sword/Scimitar
        {"Accurate", "Rapid",      "Longrange", null},          // 3 Bow
        {"Accurate", "Aggressive", "Controlled","Defensive"},   // 4 Default melee
    };
    private static final String[][] WEAPON_STYLE_XP = {
        {"Attack XP",  "Strength XP", "Defence XP",  null},         // 0 Unarmed
        {"Attack XP",  "Strength XP", "Strength XP", "Defence XP"}, // 1 Axe
        {"Attack XP",  "Strength XP", "Shared XP",   "Defence XP"}, // 2 Sword
        {"Ranged XP",  "Ranged XP",   "Rng+Def XP",  null},         // 3 Bow
        {"Attack XP",  "Strength XP", "Shared XP",   "Defence XP"}, // 4 Default
    };
    private static final String[] WEAPON_CATEGORY_NAMES = {
        "Unarmed", "Axe", "Sword", "Ranged", "Melee"
    };

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
    // Equipment tab slot colours
    private static final Color COLOR_SLOT_EMPTY_BG     = new Color(0.10f, 0.09f, 0.07f, 1f);
    private static final Color COLOR_SLOT_FILLED_BG    = new Color(0.17f, 0.15f, 0.10f, 1f);
    private static final Color COLOR_SLOT_EMPTY_BORDER = new Color(0.28f, 0.25f, 0.17f, 1f);
    private static final Color COLOR_SLOT_FILLED_BORDER= new Color(0.78f, 0.62f, 0.20f, 1f);
    private static final Color COLOR_CONNECTOR         = new Color(0.30f, 0.27f, 0.18f, 1f);
    private static final Color COLOR_SLOT_ICON         = new Color(0.24f, 0.22f, 0.15f, 1f);
    private static final Color COLOR_WEIGHT_TEXT       = new Color(0.70f, 0.68f, 0.55f, 1f);

    private static final Color COLOR_AR_ON_TEXT   = new Color(0.72f, 1.00f, 0.72f, 1f); // bright green-white
    private static final Color COLOR_AR_OFF_TEXT  = new Color(0.65f, 0.25f, 0.25f, 1f); // muted red
    private static final Color COLOR_AR_LABEL_OFF  = new Color(0.65f, 0.62f, 0.58f, 1f); // dimmed label when off
    private static final Color COLOR_LOGOUT_BTN_BG    = new Color(0.35f, 0.10f, 0.08f, 1f);
    private static final Color COLOR_LOGOUT_BTN_BR    = new Color(0.75f, 0.20f, 0.15f, 1f);
    private static final Color COLOR_LOGOUT_CONFIRM   = new Color(1.00f, 0.50f, 0.45f, 1f);
    // Quest status colours (pre-allocated — never use new Color in colorForQuest)
    private static final Color COLOR_QUEST_COMPLETE    = new Color(0.30f, 0.90f, 0.30f, 1f);
    private static final Color COLOR_QUEST_IN_PROGRESS = new Color(0.95f, 0.85f, 0.20f, 1f);
    private static final Color COLOR_QUEST_NOT_STARTED = new Color(0.92f, 0.33f, 0.33f, 1f);
    private static final Color COLOR_QUEST_TASK_DONE   = new Color(0.30f, 0.90f, 0.30f, 1f);
    private static final Color COLOR_QUEST_TASK_PEND   = new Color(0.80f, 0.80f, 0.78f, 1f);
    private CharacterPage characterPage = CharacterPage.SUMMARY;
    private final Map<Integer, QuestView> quests = new HashMap<>();
    private int selectedQuestId = -1;
    private int playerQuestPoints = 0;
    private boolean logoutRequested  = false;
    private boolean logoutConfirmed  = false;
    private final List<FriendEntryView> friendEntries = new ArrayList<>();
    private long    removeFriendRequestedId = -1L;

    // Quest tab state
    private int     questScrollOffset       = 0;  // list view: row index
    private int     questDetailScrollOffset = 0;  // detail view: pixel offset from top

    // Friends tab state
    private int     friendScrollOffset   = 0;
    private int     selectedFriendIdx    = -1;
    private boolean addFriendOverlay     = false;
    private String  addFriendInput       = "";
    private String  pendingAddFriendName = null;
    private float   overlayCursorTimer   = 0f;
    private boolean overlayCursorVis     = true;

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
    private int cY; // bottom of content area = panelY + TAB_H

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    public void update(float delta) {
        inventoryUI.update(delta);
        if (addFriendOverlay) {
            overlayCursorTimer += delta;
            if (overlayCursorTimer >= 0.5f) {
                overlayCursorTimer -= 0.5f;
                overlayCursorVis = !overlayCursorVis;
            }
        }
    }

    // -----------------------------------------------------------------------
    // Rendering
    // -----------------------------------------------------------------------

    public void render(ShapeRenderer sr, SpriteBatch batch, BitmapFont font,
                       int screenW, int screenH, Matrix4 proj, int mouseX, int mouseY) {
        panelX = screenW - PANEL_W - MARGIN;
        panelY = MARGIN;
        cY = panelY + TAB_H;

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

        // --- Dividers: bottom tab ↔ content, and content ↔ top tab ---
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(BORDER_COLOR);
        sr.rect(panelX, cY, PANEL_W, BORDER_THICKNESS);             // bottom tab ↔ content
        sr.rect(panelX, cY + CONTENT_H, PANEL_W, BORDER_THICKNESS); // content ↔ top tab
        sr.end();

        // --- Active tab content ---
        switch (activeTab) {
            case COMBAT    -> renderCombatTab(sr, batch, font, proj);
            case SKILLS    -> renderSkillsTab(sr, batch, font, proj, screenW, screenH, mouseX, mouseY);
            case QUESTS    -> renderQuestTab(sr, batch, font, proj);
            case INVENTORY -> inventoryUI.render(sr, batch, font, panelX + CONTENT_INSET, cY, proj);
            case EQUIPMENT -> renderEquipmentTab(sr, batch, font, proj);
            case PRAYER    -> renderPrayerTab(sr, batch, font, proj, mouseX, mouseY);
            case FRIENDS   -> renderFriendsTab(sr, batch, font, proj);
            case SETTINGS  -> {
                characterPage = CharacterPage.SUMMARY;
                renderCharacterTab(sr, batch, font, proj);
            }
            case LOGOUT    -> renderLogoutTab(sr, batch, font, proj);
            default        -> renderStubTab(sr, batch, font, proj, activeTab.label);
        }

        // Stat pillars always last — drawn on top of every tab so they are always visible
        renderStatPillars(sr, proj);
    }

    // -----------------------------------------------------------------------
    // Tab bar
    // -----------------------------------------------------------------------

    private void renderTabBar(ShapeRenderer sr, SpriteBatch batch, BitmapFont font, Matrix4 proj,
                              int mouseX, int mouseY) {
        sr.setProjectionMatrix(proj);
        // Bottom row rendered first (at panel bottom), then top row above content
        renderTabRow(sr, mouseX, mouseY, BOTTOM_TABS, panelY);
        renderTabRow(sr, mouseX, mouseY, TOP_TABS, cY + CONTENT_H);
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
            case LOGOUT -> {
                // Door: rectangle with small arch, arrow pointing right (exit)
                sr.rect(cx - 5, cy - 7, 8, 14);      // door frame
                sr.rect(cx + 3, cy - 3, 5, 2);        // arrow shaft
                sr.rect(cx + 5, cy - 5, 2, 2);        // arrow head up
                sr.rect(cx + 5, cy + 1, 2, 2);        // arrow head down
            }
        }
    }

    private void renderStubTab(ShapeRenderer sr, SpriteBatch batch, BitmapFont font, Matrix4 proj, String tabName) {
        batch.setProjectionMatrix(proj);
        batch.begin();
        font.getData().setScale(0.80f);
        font.setColor(0.55f, 0.52f, 0.42f, 1f);
        font.draw(batch, tabName + " - Coming Soon", panelX + CONTENT_INSET + 10, cY + CONTENT_H / 2 + 8);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private void renderLogoutTab(ShapeRenderer sr, SpriteBatch batch, BitmapFont font, Matrix4 proj) {
        int contentX = panelX + CONTENT_INSET;
        int pad = 12;
        int midY = cY + CONTENT_H / 2; // vertical centre of content area

        // Header
        batch.setProjectionMatrix(proj);
        batch.begin();
        font.getData().setScale(0.85f);
        font.setColor(COLOR_TITLE_GOLD);
        font.draw(batch, "Logout", contentX + pad, cY + CONTENT_H - 10);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();

        sr.setProjectionMatrix(proj);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.45f, 0.38f, 0.22f, 1f);
        sr.rect(contentX + pad, cY + CONTENT_H - 22, CONTENT_W - pad * 2, 1);
        sr.end();

        // Question text — centred just above the button row
        batch.setProjectionMatrix(proj);
        batch.begin();
        font.getData().setScale(0.80f);
        font.setColor(0.88f, 0.84f, 0.72f, 1f);
        font.draw(batch, "Are you sure you want", contentX + pad, midY + 44);
        font.draw(batch, "to logout?",            contentX + pad, midY + 26);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();

        // Confirm / Cancel buttons
        int btnH = 28;
        int btnW = (CONTENT_W - pad * 3) / 2;  // ~75px each
        int confirmX = contentX + pad;
        int cancelX  = contentX + pad * 2 + btnW;
        int btnY     = midY - 20;

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(COLOR_LOGOUT_BTN_BG);
        sr.rect(confirmX, btnY, btnW, btnH);
        sr.setColor(COLOR_BTN_IDLE_BG);
        sr.rect(cancelX,  btnY, btnW, btnH);
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(COLOR_LOGOUT_BTN_BR);
        sr.rect(confirmX, btnY, btnW, btnH);
        sr.setColor(BORDER_COLOR);
        sr.rect(cancelX,  btnY, btnW, btnH);
        sr.end();

        batch.setProjectionMatrix(proj);
        batch.begin();
        font.getData().setScale(0.78f);
        font.setColor(COLOR_LOGOUT_CONFIRM);
        font.draw(batch, "Confirm", confirmX + 8, btnY + btnH - 8);
        font.setColor(Color.WHITE);
        font.draw(batch, "Cancel",  cancelX  + 8, btnY + btnH - 8);
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
    private void renderStatPillars(ShapeRenderer sr, Matrix4 proj) {
        int leftX = panelX + BORDER_THICKNESS;
        int rightX = panelX + CONTENT_INSET + CONTENT_W;
        int pillarW = CONTENT_INSET - BORDER_THICKNESS;
        int pillarH = CONTENT_H;

        sr.setProjectionMatrix(proj);
        sr.begin(ShapeRenderer.ShapeType.Filled);

        // --- Left pillar: HP bar ---
        sr.setColor(HP_BAR_BG);
        sr.rect(leftX, cY, pillarW, pillarH);

        if (maxHp > 0 && currentHp > 0) {
            float fraction = Math.min(1f, (float) currentHp / maxHp);
            int fillH = Math.max(1, (int) (pillarH * fraction) - 1);
            sr.setColor(fraction <= 0.25f ? HP_BAR_LOW : HP_BAR_FILL);
            sr.rect(leftX + 2, cY + 1, pillarW - 4, fillH);
            // Right-edge shadow strip (gives a 3-D depth feel)
            sr.setColor(HP_BAR_DARK);
            sr.rect(leftX + pillarW - 4, cY + 1, 2, fillH);
        }

        // --- Right pillar: Prayer bar ---
        sr.setColor(PR_BAR_BG);
        sr.rect(rightX, cY, pillarW, pillarH);

        if (maxPrayerPoints > 0 && currentPrayerPoints > 0) {
            float fraction = Math.min(1f, (float) currentPrayerPoints / maxPrayerPoints);
            int fillH = Math.max(1, (int) (pillarH * fraction) - 1);
            sr.setColor(PR_BAR_FILL);
            sr.rect(rightX + 2, cY + 1, pillarW - 4, fillH);
            // Right-edge shadow strip
            sr.setColor(PR_BAR_DARK);
            sr.rect(rightX + pillarW - 4, cY + 1, 2, fillH);
        }

        sr.end();

        // Thin gold border around each pillar
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(BORDER_COLOR);
        sr.rect(leftX, cY, pillarW, pillarH);
        sr.rect(rightX, cY, pillarW, pillarH);
        sr.end();
    }

    private void renderCombatTab(ShapeRenderer sr, SpriteBatch batch, BitmapFont font, Matrix4 proj) {
        int contentX = panelX + CONTENT_INSET;
        int pad  = 8;
        int btnW = (CONTENT_W - pad * 3) / 2;
        int btnH = 60;

        int     wepCat    = weaponCategory();
        String[] names    = WEAPON_STYLE_NAMES[wepCat];
        String[] xpLabels = WEAPON_STYLE_XP[wepCat];
        String weaponName = (equippedNames[4] != null && !equippedNames[4].isEmpty())
            ? equippedNames[4] : "Unarmed";
        int combatLvl = combatLevel();

        // ── Header (title / weapon / combat level) ────────────────────────
        batch.setProjectionMatrix(proj);
        batch.begin();
        font.getData().setScale(0.85f);
        font.setColor(COLOR_TITLE_GOLD);
        // Draw each word separately to avoid space-glyph rendering artefact
        font.draw(batch, "Combat", contentX + pad, cY + CONTENT_H - 6);
        font.draw(batch, "Options", contentX + pad + 55, cY + CONTENT_H - 6);

        font.getData().setScale(0.75f);
        font.setColor(0.75f, 0.68f, 0.48f, 1f);
        font.draw(batch, weaponName, contentX + pad, cY + CONTENT_H - 20);

        font.getData().setScale(0.70f);
        font.setColor(0.82f, 0.62f, 0.22f, 1f);
        font.draw(batch, "Combat Lvl:", contentX + pad, cY + CONTENT_H - 33);
        font.setColor(Color.WHITE);
        font.draw(batch, String.valueOf(combatLvl), contentX + pad + 66, cY + CONTENT_H - 33);

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();

        // Header underline
        sr.setProjectionMatrix(proj);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.45f, 0.38f, 0.22f, 1f);
        sr.rect(contentX + pad, cY + CONTENT_H - 42, CONTENT_W - pad * 2, 1);
        sr.end();

        // ── 2×2 button grid ───────────────────────────────────────────────
        // gridTop is the y-coordinate just below the header underline
        int gridTop = cY + CONTENT_H - 48;

        // Pass 1: fills and borders
        for (int i = 0; i < 4; i++) {
            int col = i % 2;
            int row = i / 2;
            int bx  = contentX + pad + col * (btnW + pad);
            int by  = gridTop - (row + 1) * (btnH + pad);

            boolean disabled = names[i] == null;
            boolean sel      = !disabled && i == combatStyle;

            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(sel ? COLOR_BTN_SEL_BG : COLOR_BTN_IDLE_BG);
            sr.rect(bx, by, btnW, btnH);
            sr.end();

            sr.begin(ShapeRenderer.ShapeType.Line);
            sr.setColor(disabled ? COLOR_BTN_DISABLED_BORDER
                      : sel      ? COLOR_BTN_SEL_BORDER
                                 : COLOR_BTN_IDLE_BORDER);
            sr.rect(bx, by, btnW, btnH);
            sr.end();

            // Icon centred in the button (upper half so text doesn't overlap)
            if (!disabled) {
                drawCombatStyleIcon(sr, i, bx + btnW / 2, by + btnH / 2 + 4, sel);
            }
        }

        // Pass 2: text labels
        batch.setProjectionMatrix(proj);
        batch.begin();
        for (int i = 0; i < 4; i++) {
            if (names[i] == null) continue;
            int col = i % 2;
            int row = i / 2;
            int bx  = contentX + pad + col * (btnW + pad);
            int by  = gridTop - (row + 1) * (btnH + pad);
            boolean sel = i == combatStyle;

            // Style name — top of button
            font.getData().setScale(0.78f);
            font.setColor(sel ? COLOR_BTN_SEL_TEXT : Color.WHITE);
            font.draw(batch, names[i], bx + 5, by + btnH - 5);

            // XP label — bottom of button
            font.getData().setScale(0.62f);
            font.setColor(0.55f, 0.55f, 0.55f, 1f);
            font.draw(batch, xpLabels[i], bx + 5, by + 11);
        }
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();

        // ── Category line (above Auto Retaliate button) ───────────────────
        int arBtnH = 26;
        int arBtnY = cY + 6;
        int arBtnX = contentX + pad;
        int arBtnW = CONTENT_W - pad * 2;
        int catLineY = arBtnY + arBtnH + 14;

        batch.setProjectionMatrix(proj);
        batch.begin();
        font.getData().setScale(0.62f);
        font.setColor(0.50f, 0.48f, 0.38f, 1f);
        font.draw(batch, "Category: " + WEAPON_CATEGORY_NAMES[wepCat],
                  contentX + pad, catLineY);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();

        sr.setProjectionMatrix(proj);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.35f, 0.30f, 0.20f, 1f);
        sr.rect(contentX + pad, catLineY - 4, CONTENT_W - pad * 2, 1);
        sr.end();

        // ── Auto Retaliate full-width toggle button (anchored at bottom) ──
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(autoRetaliate ? COLOR_TOGGLE_ON : COLOR_BTN_IDLE_BG);
        sr.rect(arBtnX, arBtnY, arBtnW, arBtnH);
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(BORDER_COLOR);
        sr.rect(arBtnX, arBtnY, arBtnW, arBtnH);
        sr.end();

        batch.setProjectionMatrix(proj);
        batch.begin();
        font.getData().setScale(0.78f);
        font.setColor(autoRetaliate ? Color.WHITE : COLOR_AR_LABEL_OFF);
        font.draw(batch, "Auto Retaliate", arBtnX + 6, arBtnY + arBtnH - 7);
        font.setColor(autoRetaliate ? COLOR_AR_ON_TEXT : COLOR_AR_OFF_TEXT);
        font.draw(batch, autoRetaliate ? "(On)" : "(Off)", arBtnX + arBtnW - 38, arBtnY + arBtnH - 7);
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

    /**
     * Returns a category index into WEAPON_STYLE_NAMES / WEAPON_STYLE_XP based on
     * the name of the equipped weapon. Keyword matching is intentionally broad so
     * future weapons slot in automatically.
     */
    private int weaponCategory() {
        String w = equippedNames[4];
        if (w == null || w.isEmpty()) return 0; // Unarmed
        String l = w.toLowerCase();
        if (l.contains("axe"))                              return 1; // Axe
        if (l.contains("scimitar") || l.contains("sword")) return 2; // Sword
        if (l.contains("bow"))                              return 3; // Bow/Ranged
        return 4; // Generic melee fallback
    }

    /**
     * Player combat level — OSRS formula (from wiki):
     *   base   = 0.25 × (Defence + Hitpoints + ⌊Prayer ÷ 2⌋)
     *   melee  = 0.325 × (Attack + Strength)
     *   magic  = 0.325 × ⌊Magic  × 1.5⌋
     *   ranged = 0.325 × ⌊Ranged × 1.5⌋
     *   level  = ⌊ base + max(melee, magic, ranged) ⌋   (minimum 3)
     */
    private int combatLevel() {
        int atk  = Math.max(1,  skillLevels[0]);  // SKILL_ATTACK
        int str  = Math.max(1,  skillLevels[1]);  // SKILL_STRENGTH
        int def  = Math.max(1,  skillLevels[2]);  // SKILL_DEFENCE
        int hp   = Math.max(10, skillLevels[3]);  // SKILL_HITPOINTS (minimum 10 in OSRS)
        int rang = Math.max(1,  skillLevels[4]);  // SKILL_RANGED
        int mag  = Math.max(1,  skillLevels[5]);  // SKILL_MAGIC
        int pray = Math.max(1,  skillLevels[6]);  // SKILL_PRAYER
        double base   = 0.25  * (def + hp + Math.floor(pray / 2.0));
        double melee  = 0.325 * (atk + str);
        double magic  = 0.325 * Math.floor(mag  * 1.5);
        double ranged = 0.325 * Math.floor(rang * 1.5);
        return Math.max(3, (int) Math.floor(base + Math.max(melee, Math.max(magic, ranged))));
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
        int contentX = panelX + CONTENT_INSET;
        final int PAD = 8;
        final int ROW_H = 36;
        final int BAR_H = 20;
        final int DOT_SZ = 20;

        // -- Header --
        batch.setProjectionMatrix(proj);
        batch.begin();
        font.getData().setScale(0.85f);
        font.setColor(0.90f, 0.80f, 0.50f, 1f);
        font.draw(batch, "Prayer", contentX + PAD, cY + CONTENT_H - 8);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();

        sr.setProjectionMatrix(proj);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.45f, 0.38f, 0.22f, 1f);
        sr.rect(contentX + PAD, cY + CONTENT_H - 20, CONTENT_W - PAD * 2, 1);
        sr.end();

        // -- Prayer rows --
        int prayerLevel = skillLevels[6];
        int startY = cY + CONTENT_H - 28;

        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < PRAYERS.length; i++) {
            int prayerId = (int) PRAYERS[i][0];
            int levelReq = (int) PRAYERS[i][1];
            boolean active = activePrayerIds.contains(prayerId);
            boolean canUse = prayerLevel >= levelReq && currentPrayerPoints > 0;
            boolean hovering = mouseX >= contentX + PAD && mouseX <= contentX + CONTENT_W - PAD
                && mouseY >= startY - (i + 1) * ROW_H
                && mouseY <= startY - i * ROW_H - 2;

            int rowY = startY - (i + 1) * ROW_H + 2;

            if (active) {
                sr.setColor(0.20f, 0.28f, 0.10f, 1f);
            } else if (hovering && canUse) {
                sr.setColor(0.16f, 0.14f, 0.10f, 1f);
            } else {
                sr.setColor(0.10f, 0.09f, 0.07f, 1f);
            }
            sr.rect(contentX + PAD, rowY, CONTENT_W - PAD * 2, ROW_H - 2);

            if (active) {
                sr.setColor(0.55f, 0.88f, 0.20f, 1f);
            } else if (!canUse) {
                sr.setColor(0.28f, 0.24f, 0.18f, 1f);
            } else {
                sr.setColor(0.38f, 0.34f, 0.22f, 1f);
            }
            sr.rect(contentX + PAD + 2, rowY + (ROW_H - 2 - DOT_SZ) / 2f, DOT_SZ, DOT_SZ);

            sr.setColor(0.38f, 0.32f, 0.18f, 0.70f);
            sr.rect(contentX + PAD, rowY, CONTENT_W - PAD * 2, 1);
            sr.rect(contentX + PAD, rowY + ROW_H - 3, CONTENT_W - PAD * 2, 1);
        }
        sr.end();

        // -- Prayer point bar (bottom) --
        int barY = cY + PAD + 2;
        int barW = CONTENT_W - PAD * 2;
        float ratio = maxPrayerPoints > 0 ? (float) currentPrayerPoints / maxPrayerPoints : 0f;

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.12f, 0.10f, 0.08f, 1f);
        sr.rect(contentX + PAD, barY, barW, BAR_H);
        sr.setColor(0.22f, 0.50f, 0.82f, 1f);
        sr.rect(contentX + PAD, barY, (int) (barW * ratio), BAR_H);
        sr.setColor(0.38f, 0.32f, 0.18f, 0.80f);
        sr.rect(contentX + PAD, barY, barW, 1);
        sr.rect(contentX + PAD, barY + BAR_H - 1, barW, 1);
        sr.end();

        // -- Text pass --
        batch.setProjectionMatrix(proj);
        batch.begin();
        for (int i = 0; i < PRAYERS.length; i++) {
            int prayerId = (int) PRAYERS[i][0];
            int levelReq = (int) PRAYERS[i][1];
            String name = (String) PRAYERS[i][2];
            boolean active = activePrayerIds.contains(prayerId);
            boolean canUse = prayerLevel >= levelReq;
            int rowY = startY - (i + 1) * ROW_H + 2;

            font.getData().setScale(0.78f);
            font.setColor(active ? new Color(0.75f, 1.00f, 0.35f, 1f)
                : canUse ? new Color(0.88f, 0.84f, 0.72f, 1f)
                : new Color(0.45f, 0.42f, 0.35f, 1f));
            font.draw(batch, name, contentX + PAD + DOT_SZ + 8, rowY + ROW_H - 8);

            font.getData().setScale(0.62f);
            font.setColor(canUse ? new Color(0.65f, 0.90f, 0.25f, 1f)
                : new Color(0.75f, 0.58f, 0.18f, 1f));
            font.draw(batch, "Lv " + levelReq, contentX + CONTENT_W - PAD - 26, rowY + ROW_H - 8);
        }

        font.getData().setScale(0.72f);
        font.setColor(0.78f, 0.88f, 1.00f, 1f);
        font.draw(batch, currentPrayerPoints + " / " + maxPrayerPoints,
            contentX + PAD + 4, barY + BAR_H - 3);

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
        final int CELL_W  = (CONTENT_W - PAD_X * 2 - COL_GAP * (COLS - 1)) / COLS;
        final int CELL_H  = (CONTENT_H - PAD_Y * 2 - ROW_GAP * (ROWS - 1)) / ROWS; // ~37
        final int ICON_SZ = Math.min(CELL_H - 4, 32);

        // Compute cell origins for all 23 skills + total-level cell.
        // cellX/cellY indexed by skillIdx; -2 = total-level cell stored at index 23.
        int[] cellX = new int[24]; int[] cellY = new int[24];
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int idx = SKILL_GRID[r][c];
                int storeIdx = (idx == -1) ? 23 : idx;
                cellX[storeIdx] = panelX + CONTENT_INSET + PAD_X + c * (CELL_W + COL_GAP);
                cellY[storeIdx] = cY + CONTENT_H - PAD_Y - (r + 1) * CELL_H - r * ROW_GAP;
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
        int contentX = panelX + CONTENT_INSET;
        final int pad = 8;
        final int headerY = cY + CONTENT_H - 8;
        final int subY = cY + CONTENT_H - 34;
        final int subW = (CONTENT_W - pad * 5) / 4;
        final int subH = 18;

        // Title
        batch.setProjectionMatrix(proj);
        batch.begin();
        font.getData().setScale(0.85f);
        font.setColor(new Color(0.9f, 0.80f, 0.50f, 1f));
        font.draw(batch, "Character Summary", contentX + pad, headerY);
        font.getData().setScale(1f);
        batch.end();

        // Sub-nav buttons
        sr.setProjectionMatrix(proj);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < 4; i++) {
            int bx = contentX + pad + i * (subW + pad);
            boolean active = characterPage.ordinal() == i;
            sr.setColor(active ? new Color(0.32f, 0.25f, 0.06f, 1f) : new Color(0.13f, 0.12f, 0.10f, 1f));
            sr.rect(bx, subY, subW, subH);
        }
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < 4; i++) {
            int bx = contentX + pad + i * (subW + pad);
            boolean active = characterPage.ordinal() == i;
            sr.setColor(active ? new Color(1f, 0.85f, 0.10f, 1f) : new Color(0.40f, 0.36f, 0.26f, 1f));
            sr.rect(bx, subY, subW, subH);
        }
        sr.end();

        batch.begin();
        font.getData().setScale(0.72f);
        String[] labels = {"Summary", "Quests", "Gear", "Friends"};
        for (int i = 0; i < 4; i++) {
            int bx = contentX + pad + i * (subW + pad);
            font.setColor(characterPage.ordinal() == i ? new Color(1f, 0.90f, 0.10f, 1f) : Color.WHITE);
            font.draw(batch, labels[i], bx + 3, subY + 12);
        }

        int bodyTop = subY - 8;
        if (characterPage == CharacterPage.SUMMARY) {
            font.getData().setScale(0.8f);
            font.setColor(Color.WHITE);
            font.draw(batch, "Quest points: " + playerQuestPoints, contentX + pad, bodyTop);
            font.draw(batch, "Completed quests: " + countCompletedQuests(), contentX + pad, bodyTop - 18);
            font.draw(batch, "Total tracked quests: " + quests.size(), contentX + pad, bodyTop - 36);
            font.setColor(0.8f, 0.75f, 0.65f, 1f);
            font.getData().setScale(0.7f);
            font.draw(batch, "Open Quests to see progress.", contentX + pad, bodyTop - 60);

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
                    if (y < cY + 18) break;
                    font.setColor(colorForQuest(q.status));
                    font.draw(batch, q.questName, contentX + pad, y);
                    y -= 16;
                }
                font.setColor(0.75f, 0.75f, 0.75f, 1f);
                font.getData().setScale(0.65f);
                font.draw(batch, "Red: not started  Yellow: in progress  Green: complete", contentX + pad, cY + 10);
            } else {
                // -- Quest detail --
                int y = bodyTop;

                // Back button
                font.getData().setScale(0.75f);
                font.setColor(0.85f, 0.75f, 0.40f, 1f);
                font.draw(batch, "< Back", contentX + pad, y);
                y -= 22;

                // Quest name
                font.getData().setScale(0.85f);
                font.setColor(colorForQuest(sel.status));
                font.draw(batch, sel.questName, contentX + pad, y);
                y -= 20;

                // Description (up to 2 lines, ~32 chars each)
                font.getData().setScale(0.70f);
                font.setColor(0.85f, 0.82f, 0.72f, 1f);
                String desc = sel.description != null ? sel.description : "";
                if (desc.length() > 32) {
                    int cut = desc.lastIndexOf(' ', 32);
                    if (cut < 1) cut = 32;
                    font.draw(batch, desc.substring(0, cut), contentX + pad, y);
                    y -= 14;
                    String rest = desc.substring(cut).trim();
                    if (!rest.isEmpty() && y >= cY + 18) {
                        String line2 = rest.length() > 32 ? rest.substring(0, 32) + "..." : rest;
                        font.draw(batch, line2, contentX + pad, y);
                        y -= 14;
                    }
                } else if (!desc.isEmpty()) {
                    font.draw(batch, desc, contentX + pad, y);
                    y -= 14;
                }

                // Tasks header
                y -= 4;
                font.getData().setScale(0.72f);
                font.setColor(0.90f, 0.85f, 0.55f, 1f);
                font.draw(batch, "Tasks:", contentX + pad, y);
                y -= 16;

                // Task rows
                for (QuestTaskView t : sel.tasks) {
                    if (y < cY + 18) break;
                    String prefix = t.completed ? "[x] " : "[ ] ";
                    String progress = t.requiredCount > 1
                        ? " (" + t.currentCount + "/" + t.requiredCount + ")" : "";
                    font.setColor(t.completed
                        ? new Color(0.45f, 0.85f, 0.45f, 1f) : Color.WHITE);
                    font.draw(batch, prefix + t.description + progress, contentX + pad, y);
                    y -= 16;
                }

                // Quest point reward at bottom
                font.getData().setScale(0.65f);
                font.setColor(0.75f, 0.75f, 0.75f, 1f);
                font.draw(batch, "Reward: " + sel.questPointsReward + " Quest Point(s)",
                    contentX + pad, cY + 10);
            }
        } else if (characterPage == CharacterPage.GEAR) {
            final int SLOT_SIZE = 36;
            final int toggleY   = cY + 22;
            final int toggleH   = 14;
            final int half      = CONTENT_W / 2;

            // -- Slots / Stats toggle --
            batch.end();
            sr.setProjectionMatrix(proj);
            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(!gearShowStats
                ? new Color(0.32f, 0.25f, 0.06f, 1f)
                : new Color(0.13f, 0.12f, 0.10f, 1f));
            sr.rect(contentX, toggleY, half - 1, toggleH);
            sr.setColor(gearShowStats
                ? new Color(0.32f, 0.25f, 0.06f, 1f)
                : new Color(0.13f, 0.12f, 0.10f, 1f));
            sr.rect(contentX + half, toggleY, half, toggleH);
            sr.end();
            sr.begin(ShapeRenderer.ShapeType.Line);
            sr.setColor(new Color(0.40f, 0.36f, 0.26f, 1f));
            sr.rect(contentX, toggleY, half - 1, toggleH);
            sr.rect(contentX + half, toggleY, half, toggleH);
            sr.end();
            batch.setProjectionMatrix(proj);
            batch.begin();
            font.getData().setScale(0.65f);
            font.setColor(!gearShowStats ? new Color(1f, 0.90f, 0.10f, 1f) : Color.WHITE);
            font.draw(batch, "Equipment", contentX + 18, toggleY + 11);
            font.setColor(gearShowStats ? new Color(1f, 0.90f, 0.10f, 1f) : Color.WHITE);
            font.draw(batch, "Bonuses", contentX + half + 22, toggleY + 11);

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
                font.draw(batch, "Click slot to unequip", contentX + 8, cY + 14);

            } else {
                // -- Bonus stats view --
                int sy2 = bodyTop - 4;
                final int LH = 14;
                final int col2 = contentX + 8 + 108;

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
                font.draw(batch, "Attack Bonuses", contentX + 8, sy2);
                sy2 -= LH;
                font.getData().setScale(0.63f);
                for (int i = 0; i < 5; i++) {
                    int v = equipBonuses[atkIdx[i]];
                    font.setColor(new Color(0.75f, 0.70f, 0.50f, 1f));
                    font.draw(batch, atkLbls[i], contentX + 8, sy2);
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
                font.draw(batch, "Defence Bonuses", contentX + 8, sy2);
                sy2 -= LH;
                font.getData().setScale(0.63f);
                for (int i = 0; i < 5; i++) {
                    int v = equipBonuses[defIdx[i]];
                    font.setColor(new Color(0.75f, 0.70f, 0.50f, 1f));
                    font.draw(batch, defLbls[i], contentX + 8, sy2);
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
                font.draw(batch, "Other Bonuses", contentX + 8, sy2);
                sy2 -= LH;
                font.getData().setScale(0.63f);
                for (int i = 0; i < 4; i++) {
                    int v = equipBonuses[otherIdx[i]];
                    font.setColor(new Color(0.75f, 0.70f, 0.50f, 1f));
                    font.draw(batch, otherLbls[i], contentX + 8, sy2);
                    font.setColor(v >= 0
                        ? new Color(0.40f, 0.85f, 0.40f, 1f)
                        : new Color(0.85f, 0.40f, 0.40f, 1f));
                    font.draw(batch, (v >= 0 ? "+" : "") + v, col2, sy2);
                    sy2 -= LH;
                }
            }
        }
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    // -----------------------------------------------------------------------
    // Equipment tab (standalone OSRS-style paperdoll)
    // -----------------------------------------------------------------------

    public void setPlayerWeight(float kg) { playerWeight = kg; }

    private void renderEquipmentTab(ShapeRenderer sr, SpriteBatch batch, BitmapFont font, Matrix4 proj) {
        final int contentX  = panelX + CONTENT_INSET;
        final int SS        = 38;    // slot size
        final int GAP       = 14;    // gap between rows
        final int toggleH   = 22;
        final int toggleY   = cY + 4;
        final int half      = CONTENT_W / 2;

        // ── Equipment / Bonuses toggle (kept) ─────────────────────────────
        sr.setProjectionMatrix(proj);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(!gearShowStats ? COLOR_BTN_SEL_BG : COLOR_BTN_IDLE_BG);
        sr.rect(contentX, toggleY, half - 1, toggleH);
        sr.setColor(gearShowStats ? COLOR_BTN_SEL_BG : COLOR_BTN_IDLE_BG);
        sr.rect(contentX + half, toggleY, half, toggleH);
        sr.end();
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(!gearShowStats ? COLOR_BTN_SEL_BORDER : COLOR_BTN_IDLE_BORDER);
        sr.rect(contentX, toggleY, half - 1, toggleH);
        sr.setColor(gearShowStats ? COLOR_BTN_SEL_BORDER : COLOR_BTN_IDLE_BORDER);
        sr.rect(contentX + half, toggleY, half, toggleH);
        sr.end();

        batch.setProjectionMatrix(proj);
        batch.begin();
        font.getData().setScale(0.70f);
        font.setColor(!gearShowStats ? COLOR_BTN_SEL_TEXT : Color.WHITE);
        font.draw(batch, "Equipment", contentX, toggleY + 16, half - 1, Align.center, false);
        font.setColor(gearShowStats ? COLOR_BTN_SEL_TEXT : Color.WHITE);
        font.draw(batch, "Bonuses", contentX + half, toggleY + 16, half, Align.center, false);

        // ── Weight line (just above toggle) ──────────────────────────────
        font.getData().setScale(0.65f);
        font.setColor(COLOR_WEIGHT_TEXT);
        String wText = String.format("Weight: %.1f kg", playerWeight);
        font.draw(batch, wText, contentX, toggleY + toggleH + 17, CONTENT_W, Align.center, false);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();

        if (!gearShowStats) {
            // ── Connector lines ───────────────────────────────────────────
            int[][] sp    = getGearSlotPositions();
            int leftX     = contentX + 6;
            int centerX   = contentX + (CONTENT_W - SS) / 2;
            int rightX    = contentX + CONTENT_W - SS - 6;
            int sy0 = sp[0][1], sy1 = sp[1][1], sy2 = sp[4][1], sy3 = sp[7][1], sy4 = sp[8][1];
            int midH = SS / 2;  // half-slot height (center of slot on Y axis)

            sr.setProjectionMatrix(proj);
            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(COLOR_CONNECTOR);
            // Vertical: HEAD→NECK (center col, between rows 0 and 1)
            sr.rect(centerX + midH - 1, sy1 + SS, 2, GAP);
            // Vertical: NECK→BODY (center col, between rows 1 and 2)
            sr.rect(centerX + midH - 1, sy2 + SS, 2, GAP);
            // Vertical: BODY→LEGS (center col, between rows 2 and 3)
            sr.rect(centerX + midH - 1, sy3 + SS, 2, GAP);
            // Vertical: LEGS→FEET (center col, between rows 3 and 4)
            sr.rect(centerX + midH - 1, sy4 + SS, 2, GAP);
            // Horizontal row 1: CAPE—NECK and NECK—AMMO (at slot mid-height)
            sr.rect(leftX + SS,         sy1 + midH - 1, centerX - leftX - SS, 2);
            sr.rect(centerX + SS,       sy1 + midH - 1, rightX - centerX - SS, 2);
            // Horizontal row 2: WEAPON—BODY and BODY—SHIELD
            sr.rect(leftX + SS,         sy2 + midH - 1, centerX - leftX - SS, 2);
            sr.rect(centerX + SS,       sy2 + midH - 1, rightX - centerX - SS, 2);
            // Horizontal row 4: HANDS—FEET and FEET—RING
            sr.rect(leftX + SS,         sy4 + midH - 1, centerX - leftX - SS, 2);
            sr.rect(centerX + SS,       sy4 + midH - 1, rightX - centerX - SS, 2);
            sr.end();

            // ── Slot backgrounds (filled) ─────────────────────────────────
            sr.begin(ShapeRenderer.ShapeType.Filled);
            for (int i = 0; i < 11; i++) {
                int sx = sp[i][0], sy = sp[i][1];
                boolean filled = equippedIds[i] > 0;
                sr.setColor(filled ? COLOR_SLOT_FILLED_BG : COLOR_SLOT_EMPTY_BG);
                sr.rect(sx, sy, SS, SS);
                // Inset bevel: lighter top/left edge, darker bottom/right
                sr.setColor(0.18f, 0.16f, 0.11f, 1f);
                sr.rect(sx + 1, sy + SS - 2, SS - 2, 1); // top inner edge (lighter)
                sr.rect(sx + 1, sy + 1,      1, SS - 2); // left inner edge
                sr.setColor(0.07f, 0.06f, 0.04f, 1f);
                sr.rect(sx + 2, sy,          SS - 2, 1); // bottom inner edge (darker)
                sr.rect(sx + SS - 2, sy + 1, 1, SS - 2); // right inner edge
            }
            sr.end();

            // ── Slot borders ─────────────────────────────────────────────
            sr.begin(ShapeRenderer.ShapeType.Line);
            for (int i = 0; i < 11; i++) {
                int sx = sp[i][0], sy = sp[i][1];
                sr.setColor(equippedIds[i] > 0 ? COLOR_SLOT_FILLED_BORDER : COLOR_SLOT_EMPTY_BORDER);
                sr.rect(sx, sy, SS, SS);
            }
            sr.end();

            // ── Slot icons (empty) + tier-color dots (filled) ────────────
            sr.begin(ShapeRenderer.ShapeType.Filled);
            for (int i = 0; i < 11; i++) {
                int cx = sp[i][0] + SS / 2;
                int cy = sp[i][1] + SS / 2 + 3; // shift icon up slightly to leave room for name
                if (equippedIds[i] > 0) {
                    // Tier-colour indicator dot (center of slot, top area)
                    Color tc = itemTierColor(equippedNames[i]);
                    sr.setColor(tc);
                    sr.rect(cx - 5, cy + 3, 10, 10);
                } else {
                    drawSlotIcon(sr, i, cx, cy);
                }
            }
            sr.end();

            // ── Item names (inside filled slots, bottom area) ─────────────
            batch.setProjectionMatrix(proj);
            batch.begin();
            font.getData().setScale(0.52f);
            for (int i = 0; i < 11; i++) {
                if (equippedIds[i] > 0) {
                    int sx = sp[i][0], sy = sp[i][1];
                    String n = equippedNames[i] != null ? equippedNames[i] : "";
                    if (n.length() > 9) n = n.substring(0, 9);
                    font.setColor(Color.WHITE);
                    font.draw(batch, n, sx, sy + 11, SS, Align.center, false);
                }
            }
            font.getData().setScale(1f);
            font.setColor(Color.WHITE);
            batch.end();

        } else {
            // ── Bonuses view ──────────────────────────────────────────────
            final int bodyTop = cY + CONTENT_H - 8;
            int sy2 = bodyTop - 4;
            final int LH   = 14;
            final int col2 = contentX + 8 + 108;

            batch.setProjectionMatrix(proj);
            batch.begin();
            String[] atkLbls   = {"Stab", "Slash", "Crush", "Magic", "Ranged"};
            String[] defLbls   = {"Stab", "Slash", "Crush", "Magic", "Ranged"};
            String[] otherLbls = {"Melee Str", "Range Str", "Magic Dmg", "Prayer"};
            int[] atkIdx   = {0, 1, 2, 3, 4};
            int[] defIdx   = {5, 6, 7, 8, 9};
            int[] otherIdx = {10, 11, 12, 13};

            font.getData().setScale(0.68f);
            font.setColor(COLOR_TITLE_GOLD);
            font.draw(batch, "Attack Bonuses", contentX + 8, sy2);
            sy2 -= LH;
            font.getData().setScale(0.63f);
            for (int i = 0; i < 5; i++) {
                int v = equipBonuses[atkIdx[i]];
                font.setColor(COLOR_WEIGHT_TEXT);
                font.draw(batch, atkLbls[i], contentX + 8, sy2);
                font.setColor(v >= 0 ? COLOR_QUEST_COMPLETE : COLOR_QUEST_NOT_STARTED);
                font.draw(batch, (v >= 0 ? "+" : "") + v, col2, sy2);
                sy2 -= LH;
            }
            sy2 -= 4;
            font.getData().setScale(0.68f);
            font.setColor(COLOR_TITLE_GOLD);
            font.draw(batch, "Defence Bonuses", contentX + 8, sy2);
            sy2 -= LH;
            font.getData().setScale(0.63f);
            for (int i = 0; i < 5; i++) {
                int v = equipBonuses[defIdx[i]];
                font.setColor(COLOR_WEIGHT_TEXT);
                font.draw(batch, defLbls[i], contentX + 8, sy2);
                font.setColor(v >= 0 ? COLOR_QUEST_COMPLETE : COLOR_QUEST_NOT_STARTED);
                font.draw(batch, (v >= 0 ? "+" : "") + v, col2, sy2);
                sy2 -= LH;
            }
            sy2 -= 4;
            font.getData().setScale(0.68f);
            font.setColor(COLOR_TITLE_GOLD);
            font.draw(batch, "Other Bonuses", contentX + 8, sy2);
            sy2 -= LH;
            font.getData().setScale(0.63f);
            for (int i = 0; i < 4; i++) {
                int v = equipBonuses[otherIdx[i]];
                font.setColor(COLOR_WEIGHT_TEXT);
                font.draw(batch, otherLbls[i], contentX + 8, sy2);
                font.setColor(v >= 0 ? COLOR_QUEST_COMPLETE : COLOR_QUEST_NOT_STARTED);
                font.draw(batch, (v >= 0 ? "+" : "") + v, col2, sy2);
                sy2 -= LH;
            }
            font.getData().setScale(0.65f);
            font.setColor(COLOR_WEIGHT_TEXT);
            font.draw(batch, String.format("Weight: %.1f kg", playerWeight),
                contentX, toggleY + toggleH + 17, CONTENT_W, Align.center, false);
            font.getData().setScale(1f);
            font.setColor(Color.WHITE);
            batch.end();
        }
    }

    private List<QuestView> sortedQuests() {
        List<QuestView> list = new ArrayList<>(quests.values());
        list.sort(Comparator.comparing(q -> q.questName));
        return list;
    }

    // -----------------------------------------------------------------------
    // Quest tab
    // -----------------------------------------------------------------------

    private void renderQuestTab(ShapeRenderer sr, SpriteBatch batch, BitmapFont font, Matrix4 proj) {
        final int contentX   = panelX + CONTENT_INSET;
        final int pad        = 8;
        final int ROW_H      = 15;
        final int DESC_CHARS = 26;   // max chars per description wrap line
        // Bottom zone: 3 legend lines × 14px each + separator + margins
        final int LEGEND_H   = 46;
        final int sepY       = cY + 4 + LEGEND_H;   // = cY + 50 — separator above legend
        final int listFloor  = sepY + 14;            // = cY + 64 — list/content must stay above
        final int listTop    = cY + CONTENT_H - 40; // = cY + 272 — below the header

        // ── Header (always rendered) ────────────────────────────────────────
        batch.setProjectionMatrix(proj);
        batch.begin();
        font.getData().setScale(0.85f);
        font.setColor(COLOR_TITLE_GOLD);
        font.draw(batch, "Quest Journal", contentX + pad, cY + CONTENT_H - 8);
        font.getData().setScale(0.78f);
        font.setColor(COLOR_QUEST_COMPLETE);
        font.draw(batch, "Quest Points: " + playerQuestPoints, contentX + pad, cY + CONTENT_H - 24);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();

        sr.setProjectionMatrix(proj);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.45f, 0.38f, 0.22f, 1f);
        sr.rect(contentX + pad, cY + CONTENT_H - 34, CONTENT_W - pad * 2, 1); // header underline
        sr.setColor(0.30f, 0.28f, 0.22f, 1f);
        sr.rect(contentX + pad, sepY, CONTENT_W - pad * 2, 1);               // legend separator
        sr.end();

        // ── Legend (always visible, pinned at bottom, centered) ─────────────
        batch.setProjectionMatrix(proj);
        batch.begin();
        font.getData().setScale(0.72f);
        int ly = sepY - 4;
        font.setColor(COLOR_QUEST_NOT_STARTED);
        font.draw(batch, "Not started", contentX, ly, CONTENT_W, Align.center, false);
        ly -= 14;
        font.setColor(COLOR_QUEST_IN_PROGRESS);
        font.draw(batch, "In progress", contentX, ly, CONTENT_W, Align.center, false);
        ly -= 14;
        font.setColor(COLOR_QUEST_COMPLETE);
        font.draw(batch, "Complete", contentX, ly, CONTENT_W, Align.center, false);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();

        // ── Content ──────────────────────────────────────────────────────────
        if (selectedQuestId != -1) {
            QuestView sel = quests.get(selectedQuestId);
            if (sel == null) {
                selectedQuestId = -1;
            } else {
                // "< Back" is fixed at listTop and not part of the scrollable measurement.
                final int scrollAreaTop = listTop - 22;
                final int scrollAreaH   = scrollAreaTop - listFloor;

                // ── Measure total scrollable height (must exactly match render order) ──
                // Order: name(18) → qp reward(16) → gap(8) → desc(13/line+gap) → tasks
                int descLines = countDescLines(sel.description, DESC_CHARS);
                int totalH = 18                                          // quest name
                    + 16 + 8                                             // quest points row + gap
                    + (descLines > 0 ? descLines * 13 + 4 : 0)          // description + gap
                    + (!sel.tasks.isEmpty() ? ROW_H : 0)                // "Tasks:" header
                    + sel.tasks.size() * ROW_H;                         // task rows
                int maxScroll = Math.max(0, totalH - scrollAreaH);
                questDetailScrollOffset = Math.max(0, Math.min(questDetailScrollOffset, maxScroll));

                batch.setProjectionMatrix(proj);
                batch.begin();

                // ── "< Back" — fixed, always visible, never scrolls ──────
                font.getData().setScale(0.75f);
                font.setColor(0.85f, 0.75f, 0.40f, 1f);
                font.draw(batch, "< Back", contentX + pad, listTop);

                // ── Scrollable body ───────────────────────────────────────
                int y = scrollAreaTop - questDetailScrollOffset;

                // Quest name (status-coloured)
                font.getData().setScale(0.85f);
                font.setColor(colorForQuest(sel.status));
                if (y <= scrollAreaTop && y > listFloor) {
                    font.draw(batch, sel.questName, contentX + pad, y);
                }
                y -= 18;

                // Quest point reward — gold, prominent, always the second thing you read
                font.getData().setScale(0.78f);
                font.setColor(COLOR_TITLE_GOLD);
                if (y <= scrollAreaTop && y > listFloor) {
                    String qpText = sel.questPointsReward == 1
                        ? "Quest Points: 1"
                        : "Quest Points: " + sel.questPointsReward;
                    font.draw(batch, qpText, contentX + pad, y);
                }
                y -= 16 + 8; // row height + gap before description

                // Description (word-wrapped at DESC_CHARS)
                font.getData().setScale(0.70f);
                font.setColor(0.85f, 0.82f, 0.72f, 1f);
                String desc = sel.description != null ? sel.description : "";
                while (!desc.isEmpty()) {
                    int cut = desc.length() > DESC_CHARS
                        ? desc.lastIndexOf(' ', DESC_CHARS) : desc.length();
                    if (cut < 1) cut = Math.min(DESC_CHARS, desc.length());
                    if (y <= scrollAreaTop && y > listFloor) {
                        font.draw(batch, desc.substring(0, cut), contentX + pad, y);
                    }
                    desc = desc.substring(cut).trim();
                    y -= 13;
                    if (y < listFloor - 200) break;
                }
                if (descLines > 0) y -= 4; // gap after description block

                // Tasks header
                if (!sel.tasks.isEmpty()) {
                    font.getData().setScale(0.72f);
                    font.setColor(0.90f, 0.85f, 0.55f, 1f);
                    if (y <= scrollAreaTop && y > listFloor) {
                        font.draw(batch, "Tasks:", contentX + pad, y);
                    }
                    y -= ROW_H;
                }

                // Task rows
                font.getData().setScale(0.70f);
                for (QuestTaskView t : sel.tasks) {
                    if (y <= scrollAreaTop && y > listFloor) {
                        String prefix = t.completed ? "[x] " : "[ ] ";
                        String progress = t.requiredCount > 1
                            ? " (" + t.currentCount + "/" + t.requiredCount + ")" : "";
                        font.setColor(t.completed ? COLOR_QUEST_TASK_DONE : COLOR_QUEST_TASK_PEND);
                        font.draw(batch, prefix + t.description + progress, contentX + pad, y);
                    }
                    y -= ROW_H;
                    if (y < listFloor - 200) break;
                }

                font.getData().setScale(1f);
                font.setColor(Color.WHITE);
                batch.end();

                // Detail scrollbar (only when content overflows)
                if (maxScroll > 0) {
                    int sbX    = contentX + CONTENT_W - 6;
                    int sbY    = listFloor;
                    int sbH    = scrollAreaH;
                    int thumbH = Math.max(14, sbH * scrollAreaH / totalH);
                    int thumbY = sbY + (sbH - thumbH)
                        * (maxScroll - questDetailScrollOffset) / maxScroll;
                    sr.setProjectionMatrix(proj);
                    sr.begin(ShapeRenderer.ShapeType.Filled);
                    sr.setColor(0.18f, 0.17f, 0.14f, 1f);
                    sr.rect(sbX, sbY, 4, sbH);
                    sr.setColor(0.55f, 0.50f, 0.38f, 1f);
                    sr.rect(sbX, thumbY, 4, thumbH);
                    sr.end();
                }
            }
        } else {
            // ── Quest list ───────────────────────────────────────────────────
            List<QuestView> list = sortedQuests();
            int totalRows = list.size();
            int visRows   = (listTop - listFloor) / ROW_H;
            questScrollOffset = Math.max(0, Math.min(questScrollOffset, Math.max(0, totalRows - visRows)));

            batch.setProjectionMatrix(proj);
            batch.begin();
            font.getData().setScale(0.78f);

            int y = listTop;
            for (int i = questScrollOffset; i < totalRows && y > listFloor; i++) {
                QuestView q = list.get(i);
                font.setColor(colorForQuest(q.status));
                font.draw(batch, q.questName, contentX + pad, y);
                y -= ROW_H;
            }

            font.getData().setScale(1f);
            font.setColor(Color.WHITE);
            batch.end();

            // List scrollbar
            if (totalRows > visRows) {
                int sbH    = listTop - listFloor;
                int sbY    = listFloor;
                int sbX    = contentX + CONTENT_W - 6;
                int thumbH = Math.max(14, sbH * visRows / totalRows);
                int thumbY = sbY + (sbH - thumbH)
                    * (totalRows - visRows - questScrollOffset)
                    / Math.max(1, totalRows - visRows);
                sr.setProjectionMatrix(proj);
                sr.begin(ShapeRenderer.ShapeType.Filled);
                sr.setColor(0.18f, 0.17f, 0.14f, 1f);
                sr.rect(sbX, sbY, 4, sbH);
                sr.setColor(0.55f, 0.50f, 0.38f, 1f);
                sr.rect(sbX, thumbY, 4, thumbH);
                sr.end();
            }
        }
    }

    /** Count how many word-wrapped lines a description produces at the given char width. */
    private int countDescLines(String desc, int maxChars) {
        if (desc == null || desc.isEmpty()) return 0;
        int lines = 0;
        while (!desc.isEmpty()) {
            int cut = desc.length() > maxChars ? desc.lastIndexOf(' ', maxChars) : desc.length();
            if (cut < 1) cut = Math.min(maxChars, desc.length());
            desc = desc.substring(cut).trim();
            lines++;
        }
        return lines;
    }

    // -----------------------------------------------------------------------
    // Friends tab (full OSRS-style implementation)
    // -----------------------------------------------------------------------

    private void renderFriendsTab(ShapeRenderer sr, SpriteBatch batch, BitmapFont font, Matrix4 proj) {
        final int contentX   = panelX + CONTENT_INSET;
        final int contentW   = CONTENT_W;
        final int pad        = 6;
        final int listTop    = cY + CONTENT_H - 26;        // Y just below the title underline
        final int btnAreaH   = FRIEND_BTN_H + pad * 2;    // reserved at bottom for Add/Del
        final int listBottom = cY + btnAreaH;              // list ends here
        final int listH      = listTop - listBottom;
        final int visRows    = Math.max(1, listH / FRIEND_ROW_H);
        final int totalRows  = friendEntries.size();
        final int maxScroll  = Math.max(0, totalRows - visRows);

        // Clamp scroll
        if (friendScrollOffset > maxScroll) friendScrollOffset = maxScroll;
        if (friendScrollOffset < 0)         friendScrollOffset = 0;

        // ── Title bar ──────────────────────────────────────────────────────
        sr.setProjectionMatrix(proj);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(FRIEND_TITLE.r * 0.25f, FRIEND_TITLE.g * 0.25f, FRIEND_TITLE.b * 0.25f, 1f);
        sr.rect(contentX, cY + CONTENT_H - 22, contentW, 20);
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.38f, 0.32f, 0.18f, 1f);
        sr.rect(contentX, cY + CONTENT_H - 23, contentW, 1);
        sr.end();

        batch.setProjectionMatrix(proj);
        batch.begin();
        font.getData().setScale(0.82f);
        font.setColor(FRIEND_TITLE);
        font.draw(batch, "Friends List", contentX + 6, cY + CONTENT_H - 6);
        font.getData().setScale(0.68f);
        font.setColor(0.55f, 0.52f, 0.42f, 1f);
        String onlineCount = friendEntries.stream().filter(e -> e.online).count() + "/" + friendEntries.size();
        font.draw(batch, onlineCount + " online", contentX + contentW - 50, cY + CONTENT_H - 8);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();

        // ── List background ────────────────────────────────────────────────
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.08f, 0.07f, 0.06f, 1f);
        sr.rect(contentX, listBottom, contentW - FRIEND_SCROLLBAR_W - 1, listH);
        sr.end();

        // ── Friend rows ────────────────────────────────────────────────────
        if (friendEntries.isEmpty()) {
            batch.setProjectionMatrix(proj);
            batch.begin();
            font.getData().setScale(0.72f);
            font.setColor(0.55f, 0.52f, 0.42f, 1f);
            font.draw(batch, "No friends added yet.", contentX + pad, listTop - FRIEND_ROW_H);
            font.getData().setScale(1f);
            font.setColor(Color.WHITE);
            batch.end();
        } else {
            // Selection highlight pass (ShapeRenderer)
            sr.setProjectionMatrix(proj);
            sr.begin(ShapeRenderer.ShapeType.Filled);
            for (int i = 0; i < visRows; i++) {
                int entryIdx = i + friendScrollOffset;
                if (entryIdx >= totalRows) break;
                int rowY = listTop - (i + 1) * FRIEND_ROW_H;
                if (entryIdx == selectedFriendIdx) {
                    sr.setColor(FRIEND_SEL_BG);
                    sr.rect(contentX, rowY, contentW - FRIEND_SCROLLBAR_W - 1, FRIEND_ROW_H);
                }
                // Subtle alternating row tint
                else if (i % 2 == 1) {
                    sr.setColor(0.10f, 0.09f, 0.08f, 1f);
                    sr.rect(contentX, rowY, contentW - FRIEND_SCROLLBAR_W - 1, FRIEND_ROW_H);
                }
            }
            sr.end();

            // Text pass
            batch.setProjectionMatrix(proj);
            batch.begin();
            font.getData().setScale(0.75f);
            for (int i = 0; i < visRows; i++) {
                int entryIdx = i + friendScrollOffset;
                if (entryIdx >= totalRows) break;
                FriendEntryView entry = friendEntries.get(entryIdx);
                int rowY = listTop - (i + 1) * FRIEND_ROW_H + FRIEND_ROW_H - 4;

                font.setColor(entry.online ? FRIEND_ONLINE : FRIEND_OFFLINE);
                font.draw(batch, entry.name, contentX + pad, rowY);

                String status = entry.online ? "Online" : "Offline";
                font.setColor(entry.online ? FRIEND_ONLINE : FRIEND_OFFLINE);
                // right-align status
                float statusW = status.length() * 5.5f;
                font.draw(batch, status, contentX + contentW - FRIEND_SCROLLBAR_W - pad - (int) statusW, rowY);
            }
            font.getData().setScale(1f);
            font.setColor(Color.WHITE);
            batch.end();

            // Thin separator lines between rows
            sr.setProjectionMatrix(proj);
            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(0.18f, 0.16f, 0.12f, 1f);
            for (int i = 1; i <= visRows; i++) {
                int rowY = listTop - i * FRIEND_ROW_H;
                sr.rect(contentX, rowY, contentW - FRIEND_SCROLLBAR_W - 1, 1);
            }
            sr.end();
        }

        // ── Scrollbar ──────────────────────────────────────────────────────
        if (totalRows > visRows) {
            int sbX   = contentX + contentW - FRIEND_SCROLLBAR_W;
            int sbH   = listH;
            int sbY   = listBottom;
            int handleH = Math.max(16, sbH * visRows / Math.max(1, totalRows));
            int handleY = sbY + sbH - handleH - (int) ((sbH - handleH) * (float) friendScrollOffset / maxScroll);

            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(0.12f, 0.11f, 0.09f, 1f);
            sr.rect(sbX, sbY, FRIEND_SCROLLBAR_W, sbH);
            sr.setColor(0.45f, 0.40f, 0.26f, 1f);
            sr.rect(sbX + 1, handleY, FRIEND_SCROLLBAR_W - 2, handleH);
            sr.end();
        }

        // ── Add Friend / Del Friend buttons ────────────────────────────────
        final int btnW    = (contentW - pad * 3) / 2;
        final int btnAddX = contentX + pad;
        final int btnDelX = contentX + pad * 2 + btnW;
        final int btnY    = cY + pad;

        boolean canDel = (selectedFriendIdx >= 0 && selectedFriendIdx < totalRows);

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(FRIEND_BTN_BG);
        sr.rect(btnAddX, btnY, btnW, FRIEND_BTN_H);
        sr.setColor(canDel ? FRIEND_BTN_BG : new Color(0.08f, 0.07f, 0.06f, 1f));
        sr.rect(btnDelX, btnY, btnW, FRIEND_BTN_H);
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(FRIEND_BTN_BR);
        sr.rect(btnAddX, btnY, btnW, FRIEND_BTN_H);
        sr.setColor(canDel ? FRIEND_BTN_BR : FRIEND_BTN_DIS);
        sr.rect(btnDelX, btnY, btnW, FRIEND_BTN_H);
        sr.end();

        batch.begin();
        font.getData().setScale(0.78f);
        font.setColor(FRIEND_BTN_LBL);
        font.draw(batch, "Add Friend", btnAddX + 5, btnY + FRIEND_BTN_H - 5);
        font.setColor(canDel ? FRIEND_BTN_LBL : FRIEND_BTN_DIS);
        font.draw(batch, "Del Friend", btnDelX + 5, btnY + FRIEND_BTN_H - 5);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();

        // ── Add Friend overlay ─────────────────────────────────────────────
        if (addFriendOverlay) {
            renderAddFriendOverlay(sr, batch, font, proj);
        }
    }

    private void renderAddFriendOverlay(ShapeRenderer sr, SpriteBatch batch, BitmapFont font, Matrix4 proj) {
        final int contentX = panelX + CONTENT_INSET;
        final int overlayW = CONTENT_W;
        final int overlayH = 80;
        final int overlayX = contentX;
        final int overlayY = cY + (CONTENT_H - overlayH) / 2;

        // Dim background
        sr.setProjectionMatrix(proj);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(OVERLAY_BG);
        sr.rect(panelX, cY, PANEL_W, CONTENT_H);
        sr.end();

        // Overlay box
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.08f, 0.07f, 0.06f, 1f);
        sr.rect(overlayX, overlayY, overlayW, overlayH);
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(OVERLAY_BORDER);
        sr.rect(overlayX, overlayY, overlayW, overlayH);
        sr.end();

        // Title
        batch.setProjectionMatrix(proj);
        batch.begin();
        font.getData().setScale(0.82f);
        font.setColor(FRIEND_TITLE);
        font.draw(batch, "Add Friend", overlayX + 6, overlayY + overlayH - 6);

        // "Friend name:" label
        font.getData().setScale(0.72f);
        font.setColor(Color.WHITE);
        font.draw(batch, "Friend name:", overlayX + 6, overlayY + overlayH - 24);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();

        // Input box
        final int inputX = overlayX + 6;
        final int inputY = overlayY + overlayH - 52;
        final int inputW = overlayW - 12;
        final int inputH = 18;

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(OVERLAY_INPUT);
        sr.rect(inputX, inputY, inputW, inputH);
        sr.end();
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(OVERLAY_BORDER);
        sr.rect(inputX, inputY, inputW, inputH);
        sr.end();

        String displayText = addFriendInput + (overlayCursorVis ? "|" : "");
        batch.begin();
        font.getData().setScale(0.75f);
        font.setColor(Color.WHITE);
        font.draw(batch, displayText, inputX + 4, inputY + inputH - 3);

        // OK / Cancel buttons
        final int okW   = 40;
        final int btnH  = 16;
        final int okX   = overlayX + overlayW / 2 - okW - 4;
        final int canX  = overlayX + overlayW / 2 + 4;
        final int bBtnY = overlayY + 6;

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(FRIEND_BTN_BG);
        sr.rect(okX,  bBtnY, okW, btnH);
        sr.rect(canX, bBtnY, okW, btnH);
        sr.end();
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(FRIEND_BTN_BR);
        sr.rect(okX,  bBtnY, okW, btnH);
        sr.rect(canX, bBtnY, okW, btnH);
        sr.end();

        batch.begin();
        font.getData().setScale(0.72f);
        font.setColor(FRIEND_BTN_LBL);
        font.draw(batch, "OK",     okX  + 10, bBtnY + btnH - 2);
        font.draw(batch, "Cancel", canX + 4,  bBtnY + btnH - 2);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
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
            case COMPLETED   -> COLOR_QUEST_COMPLETE;
            case IN_PROGRESS -> COLOR_QUEST_IN_PROGRESS;
            case NOT_STARTED -> COLOR_QUEST_NOT_STARTED;
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
        // Top tab row click
        if (my >= cY + CONTENT_H && my < panelY + TOTAL_H) {
            Tab[] row = TOP_TABS;
            int tabW = PANEL_W / row.length;
            int lastW = PANEL_W - tabW * (row.length - 1);
            for (int i = 0; i < row.length; i++) {
                int tw = (i == row.length - 1) ? lastW : tabW;
                int tx = panelX + i * tabW;
                if (mx >= tx && mx <= tx + tw) {
                    activeTab = row[i];
                    return -1;
                }
            }
            return -1;
        }
        // Bottom tab row click
        if (my >= panelY && my < cY) {
            Tab[] row = BOTTOM_TABS;
            int tabW = PANEL_W / row.length;
            int lastW = PANEL_W - tabW * (row.length - 1);
            for (int i = 0; i < row.length; i++) {
                int tw = (i == row.length - 1) ? lastW : tabW;
                int tx = panelX + i * tabW;
                if (mx >= tx && mx <= tx + tw) {
                    activeTab = row[i];
                    return -1;
                }
            }
            return -1;
        }

        // ── Content area click ─────────────────────────────────────────────
        switch (activeTab) {
            case COMBAT -> {
                int contentX = panelX + CONTENT_INSET;
                int pad  = 8;
                int btnW = (CONTENT_W - pad * 3) / 2;
                int btnH = 60;
                int gridTop = cY + CONTENT_H - 48;

                String[] names = WEAPON_STYLE_NAMES[weaponCategory()];

                for (int i = 0; i < 4; i++) {
                    if (names[i] == null) continue;
                    int col = i % 2;
                    int row = i / 2;
                    int bx  = contentX + pad + col * (btnW + pad);
                    int by  = gridTop - (row + 1) * (btnH + pad);

                    if (mx >= bx && mx <= bx + btnW && my >= by && my <= by + btnH) {
                        combatStyle = i;
                        return i;
                    }
                }

                // Auto Retaliate button — fixed at bottom of content area
                int arBtnH = 26;
                int arBtnY = cY + 6;
                int arBtnX = contentX + pad;
                int arBtnW = CONTENT_W - pad * 2;
                if (mx >= arBtnX && mx <= arBtnX + arBtnW
                    && my >= arBtnY && my <= arBtnY + arBtnH) {
                    autoRetaliate = !autoRetaliate;
                    return -50;
                }
            }
            case EQUIPMENT -> {
                final int contentX = panelX + CONTENT_INSET;
                final int toggleY  = cY + 4;
                final int toggleH  = 22;
                if (my >= toggleY && my <= toggleY + toggleH) {
                    gearShowStats = mx >= contentX + CONTENT_W / 2;
                    return -1;
                }
                if (!gearShowStats) {
                    int[][] slotPos = getGearSlotPositions();
                    final int SS = 38;
                    for (int i = 0; i < 11; i++) {
                        int sx = slotPos[i][0], sy = slotPos[i][1];
                        if (mx >= sx && mx <= sx + SS && my >= sy && my <= sy + SS) {
                            return -(100 + i);
                        }
                    }
                }
            }
            case QUESTS -> {
                final int pad        = 8;
                final int ROW_H      = 15;
                final int LEGEND_H   = 46;
                final int sepY       = cY + 4 + LEGEND_H;   // = cY + 50
                final int listFloor  = sepY + 14;            // = cY + 64
                final int listTop    = cY + CONTENT_H - 40;
                final int contentX   = panelX + CONTENT_INSET;
                if (selectedQuestId != -1) {
                    // "< Back" is fixed at listTop (not scrolled)
                    if (mx >= contentX + pad && mx <= contentX + CONTENT_W - pad
                        && my <= listTop && my >= listTop - ROW_H) {
                        selectedQuestId = -1;
                        questDetailScrollOffset = 0;
                        return -1;
                    }
                } else {
                    List<QuestView> list = sortedQuests();
                    int y = listTop;
                    for (int i = questScrollOffset; i < list.size() && y > listFloor; i++) {
                        if (mx >= contentX + pad && mx <= contentX + CONTENT_W - pad
                            && my <= y && my >= y - ROW_H) {
                            selectedQuestId = list.get(i).questId;
                            questDetailScrollOffset = 0; // reset detail scroll on entry
                            return -1;
                        }
                        y -= ROW_H;
                    }
                }
            }
            case FRIENDS -> {
                final int contentX   = panelX + CONTENT_INSET;
                final int contentW   = CONTENT_W;
                final int pad        = 6;
                final int listTop    = cY + CONTENT_H - 26;
                final int btnAreaH   = FRIEND_BTN_H + pad * 2;
                final int listBottom = cY + btnAreaH;
                final int listH      = listTop - listBottom;
                final int visRows    = Math.max(1, listH / FRIEND_ROW_H);
                final int totalRows  = friendEntries.size();

                // Handle Add Friend overlay clicks first
                if (addFriendOverlay) {
                    final int overlayW = contentW;
                    final int overlayH = 80;
                    final int overlayX = contentX;
                    final int overlayY = cY + (CONTENT_H - overlayH) / 2;
                    final int okW      = 40;
                    final int btnH     = 16;
                    final int okX      = overlayX + overlayW / 2 - okW - 4;
                    final int canX     = overlayX + overlayW / 2 + 4;
                    final int bBtnY    = overlayY + 6;

                    if (mx >= okX && mx <= okX + okW && my >= bBtnY && my <= bBtnY + btnH) {
                        // OK clicked
                        if (!addFriendInput.trim().isEmpty()) {
                            pendingAddFriendName = addFriendInput.trim();
                        }
                        addFriendInput = "";
                        addFriendOverlay = false;
                        return -1;
                    }
                    // Any click that isn't on OK closes the overlay (cancel / click-outside)
                    addFriendInput = "";
                    addFriendOverlay = false;
                    return -1;
                }

                // Buttons at bottom
                final int btnW    = (contentW - pad * 3) / 2;
                final int btnAddX = contentX + pad;
                final int btnDelX = contentX + pad * 2 + btnW;
                final int btnY    = cY + pad;

                if (mx >= btnAddX && mx <= btnAddX + btnW && my >= btnY && my <= btnY + FRIEND_BTN_H) {
                    addFriendInput = "";
                    addFriendOverlay = true;
                    overlayCursorTimer = 0f;
                    overlayCursorVis = true;
                    return -1;
                }
                boolean canDel = selectedFriendIdx >= 0 && selectedFriendIdx < totalRows;
                if (canDel && mx >= btnDelX && mx <= btnDelX + btnW && my >= btnY && my <= btnY + FRIEND_BTN_H) {
                    FriendEntryView toRemove = friendEntries.get(selectedFriendIdx);
                    removeFriendRequestedId = toRemove.playerId;
                    selectedFriendIdx = -1;
                    return -1;
                }

                // Friend row clicks
                for (int i = 0; i < visRows; i++) {
                    int entryIdx = i + friendScrollOffset;
                    if (entryIdx >= totalRows) break;
                    int rowY = listTop - (i + 1) * FRIEND_ROW_H;
                    if (mx >= contentX && mx <= contentX + contentW - FRIEND_SCROLLBAR_W - 1
                        && my >= rowY && my <= rowY + FRIEND_ROW_H) {
                        selectedFriendIdx = (selectedFriendIdx == entryIdx) ? -1 : entryIdx;
                        return -1;
                    }
                }
            }
            case SETTINGS -> {
                if (mx >= logoutButtonX() && mx <= logoutButtonX() + logoutButtonW()
                    && my >= logoutButtonY() && my <= logoutButtonY() + logoutButtonH()) {
                    activeTab = Tab.LOGOUT;  // route to in-panel confirmation
                    return -1;
                }
            }
            case LOGOUT -> {
                int contentX = panelX + CONTENT_INSET;
                int pad = 12;
                int midY = cY + CONTENT_H / 2;
                int btnH = 28;
                int btnW = (CONTENT_W - pad * 3) / 2;
                int confirmX = contentX + pad;
                int cancelX  = contentX + pad * 2 + btnW;
                int btnY     = midY - 20;
                if (mx >= confirmX && mx <= confirmX + btnW && my >= btnY && my <= btnY + btnH) {
                    logoutConfirmed = true;
                    activeTab = Tab.INVENTORY;
                    return -1;
                }
                if (mx >= cancelX && mx <= cancelX + btnW && my >= btnY && my <= btnY + btnH) {
                    activeTab = Tab.INVENTORY;
                    return -1;
                }
            }
            case PRAYER -> {
                int prayerLevel = skillLevels[6];
                int startY = cY + CONTENT_H - 28;
                final int ROW_H = 36;
                int contentX = panelX + CONTENT_INSET;
                for (int i = 0; i < PRAYERS.length; i++) {
                    int prayerId = (int) PRAYERS[i][0];
                    int rowY = startY - (i + 1) * ROW_H + 2;
                    if (mx >= contentX + 8 && mx <= contentX + CONTENT_W - 8
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
        boolean out = logoutRequested || logoutConfirmed;
        logoutRequested = false;
        logoutConfirmed = false;
        return out;
    }

    // Logout tab public API (called from GameScreen)
    public void    showLogoutTab()       { activeTab = Tab.LOGOUT; }
    public boolean isLogoutTabActive()   { return activeTab == Tab.LOGOUT; }
    public void    confirmLogout()       { logoutConfirmed = true; activeTab = Tab.INVENTORY; }
    public void    cancelLogout()        { if (activeTab == Tab.LOGOUT) activeTab = Tab.INVENTORY; }

    // Friends tab public API (called from GameScreen)
    public boolean isAddFriendOverlayActive() { return addFriendOverlay; }
    public boolean isFriendsTabActive()       { return activeTab == Tab.FRIENDS; }
    public boolean isQuestsTabActive()        { return activeTab == Tab.QUESTS; }

    public void scrollFriendsList(int amount) {
        friendScrollOffset = Math.max(0, friendScrollOffset + amount);
    }

    public void scrollQuestList(int amount) {
        if (selectedQuestId != -1) {
            // Detail view: pixel scroll (15px per wheel notch)
            questDetailScrollOffset = Math.max(0, questDetailScrollOffset + amount * 15);
        } else {
            // List view: row scroll
            questScrollOffset = Math.max(0, questScrollOffset + amount);
        }
    }

    public void typeAddFriendChar(char c) {
        if (addFriendInput.length() < 32) addFriendInput += c;
    }

    public void handleAddFriendKey(int keycode) {
        switch (keycode) {
            case com.badlogic.gdx.Input.Keys.ENTER -> {
                if (!addFriendInput.trim().isEmpty()) pendingAddFriendName = addFriendInput.trim();
                addFriendInput = "";
                addFriendOverlay = false;
            }
            case com.badlogic.gdx.Input.Keys.ESCAPE -> {
                addFriendInput = "";
                addFriendOverlay = false;
            }
            case com.badlogic.gdx.Input.Keys.BACKSPACE -> {
                if (!addFriendInput.isEmpty())
                    addFriendInput = addFriendInput.substring(0, addFriendInput.length() - 1);
            }
        }
    }

    public String consumeAddFriendRequested() {
        String name = pendingAddFriendName;
        pendingAddFriendName = null;
        return name;
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

    private int logoutButtonX() { return panelX + CONTENT_INSET + 8; }
    private int logoutButtonY() { return cY + 8; }
    private int logoutButtonW() { return 72; }
    private int logoutButtonH() { return 20; }

    /**
     * Returns screen [x, y] (bottom-left corner of each 38x38 slot box) for all
     * 11 equipment slots in EquipmentSlot order (HEAD=0 … RING=10).
     * Correct OSRS paperdoll layout: HEAD alone top, CAPE/NECK/AMMO row 2,
     * WEAPON/BODY/SHIELD row 3, LEGS alone, HANDS/FEET/RING bottom.
     */
    private int[][] getGearSlotPositions() {
        final int SS        = 38;   // slot size
        final int GAP       = 14;   // vertical gap between rows
        final int contentX  = panelX + CONTENT_INSET;
        final int leftX     = contentX + 6;
        final int centerX   = contentX + (CONTENT_W - SS) / 2;   // contentX + 74
        final int rightX    = contentX + CONTENT_W - SS - 6;      // contentX + 142

        // Grid starts 8px from top of content area, rows go downward
        int sy0 = cY + CONTENT_H - 8  - SS;         // HEAD row
        int sy1 = sy0 - GAP - SS;                    // CAPE / NECK / AMMO
        int sy2 = sy1 - GAP - SS;                    // WEAPON / BODY / SHIELD
        int sy3 = sy2 - GAP - SS;                    // LEGS
        int sy4 = sy3 - GAP - SS;                    // HANDS / FEET / RING

        return new int[][] {
            {centerX, sy0},  // 0 HEAD
            {leftX,   sy1},  // 1 CAPE
            {centerX, sy1},  // 2 NECK
            {rightX,  sy1},  // 3 AMMO
            {leftX,   sy2},  // 4 WEAPON
            {rightX,  sy2},  // 5 SHIELD
            {centerX, sy2},  // 6 BODY
            {centerX, sy3},  // 7 LEGS
            {leftX,   sy4},  // 8 HANDS
            {centerX, sy4},  // 9 FEET
            {rightX,  sy4},  // 10 RING
        };
    }

    /**
     * Draw a simple geometric icon representing each equipment slot type.
     * cx, cy = centre of the slot box in screen coords.
     * Drawn using whichever ShapeRenderer.Filled pass is currently active.
     */
    private void drawSlotIcon(ShapeRenderer sr, int slot, int cx, int cy) {
        sr.setColor(COLOR_SLOT_ICON);
        switch (slot) {
            case 0 -> { // HEAD — stacked dome
                sr.rect(cx - 5, cy + 5, 10, 4);
                sr.rect(cx - 7, cy + 1, 14, 4);
                sr.rect(cx - 5, cy - 3, 10, 4);
            }
            case 1 -> { // CAPE — downward trapezoid
                sr.rect(cx - 8, cy + 6, 16, 3);
                sr.rect(cx - 6, cy + 3, 12, 3);
                sr.rect(cx - 4, cy,      8, 3);
                sr.rect(cx - 2, cy - 3,  4, 3);
            }
            case 2 -> { // NECK/AMULET — diamond pendant
                sr.rect(cx - 1, cy + 7,  2, 2);
                sr.rect(cx - 3, cy + 5,  6, 2);
                sr.rect(cx - 4, cy + 3,  8, 2);
                sr.rect(cx - 3, cy + 1,  6, 2);
                sr.rect(cx - 1, cy - 1,  2, 2);
                sr.rect(cx - 1, cy + 9,  2, 3); // chain
            }
            case 3 -> { // AMMO — two arrow shafts with tips
                sr.rect(cx - 4, cy - 7, 3, 13);
                sr.rect(cx + 1, cy - 7, 3, 13);
                sr.rect(cx - 5, cy + 6, 5, 2);  // left tip
                sr.rect(cx,     cy + 6, 5, 2);  // right tip
            }
            case 4 -> { // WEAPON — vertical sword
                sr.rect(cx - 1, cy - 7, 3, 14); // blade
                sr.rect(cx - 6, cy + 1, 12, 3); // cross-guard
                sr.rect(cx - 2, cy - 9, 4, 3);  // pommel
            }
            case 5 -> { // SHIELD — kite shape
                sr.rect(cx - 6, cy + 4, 12, 5);
                sr.rect(cx - 6, cy + 0, 12, 4);
                sr.rect(cx - 4, cy - 4,  8, 4);
                sr.rect(cx - 2, cy - 7,  4, 3);
                sr.rect(cx - 1, cy - 9,  2, 2);
            }
            case 6 -> { // BODY — chest plate
                sr.rect(cx - 8, cy + 5, 16, 3);  // shoulders
                sr.rect(cx - 7, cy - 6, 14, 11); // torso
                sr.rect(cx - 4, cy - 8,  8, 2);  // bottom hem
            }
            case 7 -> { // LEGS — trousers
                sr.rect(cx - 7, cy + 7, 14, 3);  // waistband
                sr.rect(cx - 7, cy - 8,  6, 15); // left leg
                sr.rect(cx + 1, cy - 8,  6, 15); // right leg
            }
            case 8 -> { // HANDS — gauntlets (two rectangles)
                sr.rect(cx - 9, cy - 4, 8, 8);
                sr.rect(cx + 1, cy - 4, 8, 8);
            }
            case 9 -> { // FEET — boot silhouette
                sr.rect(cx - 4, cy + 1,  5, 8); // shaft
                sr.rect(cx - 4, cy - 3, 10, 4); // foot/sole
            }
            case 10 -> { // RING — circle
                sr.circle(cx, cy, 6, 10);
            }
        }
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
