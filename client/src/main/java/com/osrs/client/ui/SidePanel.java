package com.osrs.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;

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
    /** Total panel height including the tab bar. */
    public static final int TOTAL_H   = TAB_H + CONTENT_H;
    /** Gap between panel edge and screen edge. */
    public static final int MARGIN    = 8;

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
        COMBAT    (0, "Combat"),
        SKILLS    (1, "Skills"),
        INVENTORY (2, "Inventory");

        public final int    index;
        public final String label;
        Tab(int i, String l) { this.index = i; this.label = l; }
    }

    private static final Tab[] TABS = Tab.values();

    private Tab activeTab = Tab.INVENTORY;   // default open tab (OSRS default)

    // -----------------------------------------------------------------------
    // Sub-components / data
    // -----------------------------------------------------------------------

    private final InventoryUI inventoryUI = new InventoryUI();

    // Skills (synced from ClientPacketHandler each frame)
    private final int[]  skillLevels = new int[6];
    private final long[] skillXp     = new long[6];

    private static final String[] SKILL_NAMES = {
        "Attack", "Strength", "Defence", "Hitpoints", "Ranged", "Magic"
    };
    private static final Color[] SKILL_COLORS = {
        new Color(0.80f, 0.20f, 0.20f, 1f),   // Attack    – red
        new Color(0.80f, 0.50f, 0.10f, 1f),   // Strength  – orange
        new Color(0.20f, 0.55f, 0.85f, 1f),   // Defence   – blue
        new Color(0.55f, 0.85f, 0.25f, 1f),   // Hitpoints – green
        new Color(0.25f, 0.75f, 0.25f, 1f),   // Ranged    – lime
        new Color(0.45f, 0.30f, 0.90f, 1f),   // Magic     – purple
    };

    // Combat style (0=Accurate 1=Aggressive 2=Defensive 3=Controlled)
    private int combatStyle = 1;   // default: Aggressive

    private static final String[] STYLE_NAMES = {
        "Accurate", "Aggressive", "Defensive", "Controlled"
    };
    private static final String[] STYLE_XP = {
        "Attack XP", "Strength XP", "Defence XP", "Shared XP"
    };

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
        sr.setColor(0.10f, 0.09f, 0.08f, 0.96f);
        sr.rect(panelX, panelY, PANEL_W, TOTAL_H);
        sr.end();

        // --- Outer border (OSRS brown-gold) ---
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(0.55f, 0.46f, 0.28f, 1f);
        sr.rect(panelX, panelY, PANEL_W, TOTAL_H);
        sr.end();

        // --- Tab bar ---
        renderTabBar(sr, batch, font, proj);

        // --- Divider line between tab bar and content ---
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.55f, 0.46f, 0.28f, 1f);
        sr.rect(panelX, panelY + CONTENT_H, PANEL_W, 1);
        sr.end();

        // --- Active tab content ---
        switch (activeTab) {
            case COMBAT    -> renderCombatTab(sr, batch, font, proj);
            case SKILLS    -> renderSkillsTab(sr, batch, font, proj, screenW, screenH, mouseX, mouseY);
            case INVENTORY -> inventoryUI.render(sr, batch, font, panelX + 2, panelY, proj);
        }
    }

    // -----------------------------------------------------------------------
    // Tab bar
    // -----------------------------------------------------------------------

    private void renderTabBar(ShapeRenderer sr, SpriteBatch batch, BitmapFont font, Matrix4 proj) {
        int tabBarY = panelY + CONTENT_H;
        int tabW    = PANEL_W / TABS.length;
        int lastW   = PANEL_W - tabW * (TABS.length - 1);

        // ── Pass 1: all filled backgrounds + icons ───────────────────────────
        sr.setProjectionMatrix(proj);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < TABS.length; i++) {
            int     tx     = panelX + i * tabW;
            int     tw     = (i == TABS.length - 1) ? lastW : tabW;
            boolean active = (TABS[i] == activeTab);

            sr.setColor(active
                ? new Color(0.28f, 0.22f, 0.05f, 1f)
                : new Color(0.16f, 0.14f, 0.12f, 1f));
            sr.rect(tx + 1, tabBarY + 1, tw - 2, TAB_H - 2);

            // Icon
            int     cx = tx + tw / 2;
            int     cy = tabBarY + TAB_H / 2 + 5;
            Color   c  = active ? new Color(1f, 0.88f, 0.15f, 1f)
                                 : new Color(0.55f, 0.50f, 0.40f, 1f);
            sr.setColor(c);
            switch (i) {
                case 0 -> {
                    sr.rect(cx - 7, cy - 2, 14, 3);
                    sr.rect(cx - 2, cy - 6, 3, 12);
                    sr.setColor(c.r * 0.7f, c.g * 0.7f, c.b * 0.7f, 1f);
                    sr.rect(cx - 5, cy - 1, 10, 1);
                }
                case 1 -> {
                    sr.rect(cx - 7, cy - 6, 4, 4);
                    sr.rect(cx - 2, cy - 6, 4, 8);
                    sr.rect(cx + 3, cy - 6, 4, 12);
                }
                case 2 -> {
                    sr.rect(cx - 6, cy + 1, 5, 5);
                    sr.rect(cx + 1, cy + 1, 5, 5);
                    sr.rect(cx - 6, cy - 6, 5, 5);
                    sr.rect(cx + 1, cy - 6, 5, 5);
                }
            }
        }
        sr.end();

        // ── Pass 2: all borders ──────────────────────────────────────────────
        sr.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < TABS.length; i++) {
            int     tx     = panelX + i * tabW;
            int     tw     = (i == TABS.length - 1) ? lastW : tabW;
            boolean active = (TABS[i] == activeTab);
            sr.setColor(active
                ? new Color(1.0f, 0.85f, 0.10f, 1f)
                : new Color(0.42f, 0.36f, 0.22f, 1f));
            sr.rect(tx + 1, tabBarY + 1, tw - 2, TAB_H - 2);
        }
        sr.end();

        // ── Pass 3: labels ───────────────────────────────────────────────────
        batch.setProjectionMatrix(proj);
        batch.begin();
        font.getData().setScale(0.65f);
        for (int i = 0; i < TABS.length; i++) {
            int     tx     = panelX + i * tabW;
            int     tw     = (i == TABS.length - 1) ? lastW : tabW;
            boolean active = (TABS[i] == activeTab);
            font.setColor(active
                ? new Color(1f, 0.90f, 0.10f, 1f)
                : new Color(0.65f, 0.60f, 0.50f, 1f));
            int charW = (int) (TABS[i].label.length() * 7);
            font.draw(batch, TABS[i].label, tx + (tw - charW) / 2f, panelY + CONTENT_H + 13);
        }
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    // -----------------------------------------------------------------------
    // Combat tab
    // -----------------------------------------------------------------------

    private void renderCombatTab(ShapeRenderer sr, SpriteBatch batch, BitmapFont font, Matrix4 proj) {
        int pad  = 10;
        int btnW = (PANEL_W - pad * 3) / 2;   // ~80 px
        int btnH = 48;

        // Title
        batch.setProjectionMatrix(proj);
        batch.begin();
        font.getData().setScale(0.85f);
        font.setColor(new Color(0.9f, 0.80f, 0.50f, 1f));
        font.draw(batch, "Combat Options", panelX + pad, panelY + CONTENT_H - 8);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();

        // Title underline
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.45f, 0.38f, 0.22f, 1f);
        sr.rect(panelX + pad, panelY + CONTENT_H - 20, PANEL_W - pad * 2, 1);
        sr.end();

        // 2×2 button grid — top-left of the content area, below the title
        int gridTop = panelY + CONTENT_H - 28;
        for (int i = 0; i < 4; i++) {
            int col = i % 2;
            int row = i / 2;
            int bx  = panelX + pad + col * (btnW + pad);
            int by  = gridTop - (row + 1) * (btnH + pad);

            boolean sel = (i == combatStyle);

            // Button background
            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(sel
                ? new Color(0.35f, 0.27f, 0.04f, 1f)
                : new Color(0.17f, 0.15f, 0.13f, 1f));
            sr.rect(bx, by, btnW, btnH);
            sr.end();

            // Button border
            sr.begin(ShapeRenderer.ShapeType.Line);
            sr.setColor(sel
                ? new Color(1f, 0.85f, 0.10f, 1f)
                : new Color(0.40f, 0.36f, 0.26f, 1f));
            sr.rect(bx, by, btnW, btnH);
            sr.end();

            // Draw a small icon per style
            drawCombatStyleIcon(sr, i, bx + btnW / 2, by + btnH - 16, sel);
        }

        // Button labels
        batch.setProjectionMatrix(proj);
        batch.begin();
        for (int i = 0; i < 4; i++) {
            int col = i % 2;
            int row = i / 2;
            int bx  = panelX + pad + col * (btnW + pad);
            int by  = gridTop - (row + 1) * (btnH + pad);
            boolean sel = (i == combatStyle);

            font.getData().setScale(0.85f);
            font.setColor(sel ? new Color(1f, 0.90f, 0.10f, 1f) : Color.WHITE);
            font.draw(batch, STYLE_NAMES[i], bx + 6, by + btnH - 6);

            font.getData().setScale(0.70f);
            font.setColor(0.60f, 0.60f, 0.60f, 1f);
            font.draw(batch, STYLE_XP[i], bx + 6, by + 14);
        }
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    /** Small decorative icon inside each combat style button. */
    private void drawCombatStyleIcon(ShapeRenderer sr, int style, int cx, int cy, boolean active) {
        Color c = active ? new Color(1f, 0.85f, 0.15f, 1f) : new Color(0.50f, 0.48f, 0.38f, 1f);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(c);
        switch (style) {
            case 0 -> { // Accurate: target/aim — small diamond
                sr.rect(cx - 1, cy + 2, 3, 3);
                sr.rect(cx - 3, cy, 7, 3);
                sr.rect(cx - 1, cy - 2, 3, 3);
            }
            case 1 -> { // Aggressive: power — upward arrow
                sr.rect(cx - 4, cy - 3, 9, 4);
                sr.rect(cx - 1, cy + 1, 3, 4);
            }
            case 2 -> { // Defensive: shield — rectangle with rounded top suggestion
                sr.rect(cx - 4, cy - 3, 9, 7);
                sr.rect(cx - 3, cy + 4, 7, 2);
            }
            case 3 -> { // Controlled: balance — horizontal bar with two legs
                sr.rect(cx - 5, cy + 1, 11, 2);
                sr.rect(cx - 4, cy - 3, 2, 5);
                sr.rect(cx + 2, cy - 3, 2, 5);
            }
        }
        sr.end();
    }

    // -----------------------------------------------------------------------
    // Skills tab  —  OSRS-accurate layout
    //
    // OSRS skills tab uses a 3-column grid where each cell shows:
    //   [skill icon 32×32]  [level]
    // The level is displayed as a yellow number (current level).
    // Skill names are not shown as text — the icon identifies the skill.
    // XP is NOT shown in the cell; it appears only on hover.
    //
    // For our 6 MVP skills we use a 2-column, 3-row grid that closely
    // mirrors the left two columns of the real OSRS skills tab.
    // Column order matches OSRS: Attack/Strength/Defence on the left,
    // Hitpoints/Ranged/Magic on the right.
    // -----------------------------------------------------------------------

    /** Maps our skill array index to the OSRS two-column grid position.
     *  [col, row] — col 0 = left column, col 1 = right column. */
    private static final int[][] SKILL_GRID_POS = {
        {0, 0},  // Attack     — left col,  row 0
        {0, 1},  // Strength   — left col,  row 1
        {0, 2},  // Defence    — left col,  row 2
        {1, 0},  // Hitpoints  — right col, row 0
        {1, 1},  // Ranged     — right col, row 1
        {1, 2},  // Magic      — right col, row 2
    };

    private void renderSkillsTab(ShapeRenderer sr, SpriteBatch batch, BitmapFont font, Matrix4 proj,
                                  int screenW, int screenH, int mouseX, int mouseY) {
        final int COLS   = 2;
        final int ROWS   = 3;
        final int PAD    = 6;
        final int CELL_W = (PANEL_W - PAD * (COLS + 1)) / COLS;  // ~86 px
        final int CELL_H = (CONTENT_H - PAD * (ROWS + 1)) / ROWS; // ~96 px
        final int ICON_SZ = 22;

        // Precompute cell origins
        int[] cellX = new int[6], cellY = new int[6];
        for (int i = 0; i < 6; i++) {
            cellX[i] = panelX + PAD + SKILL_GRID_POS[i][0] * (CELL_W + PAD);
            cellY[i] = panelY + CONTENT_H - PAD - (SKILL_GRID_POS[i][1] + 1) * (CELL_H + PAD) + PAD;
        }

        // ── Pass 1: all filled shapes (backgrounds + icons) ─────────────────
        sr.setProjectionMatrix(proj);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < 6; i++) {
            int cx = cellX[i], cy = cellY[i];
            // Cell background
            sr.setColor(0.11f, 0.10f, 0.08f, 1f);
            sr.rect(cx, cy, CELL_W, CELL_H);
            // Skill icon
            int iconX = cx + (CELL_W - ICON_SZ) / 2;
            int iconY = cy + CELL_H - ICON_SZ - 8;
            Color ic  = SKILL_COLORS[i];
            sr.setColor(ic.r, ic.g, ic.b, 1f);
            sr.rect(iconX, iconY, ICON_SZ, ICON_SZ);
            // Highlight edges
            sr.setColor(Math.min(1f, ic.r + 0.35f), Math.min(1f, ic.g + 0.35f),
                        Math.min(1f, ic.b + 0.35f), 0.65f);
            sr.rect(iconX, iconY + ICON_SZ - 4, ICON_SZ, 4);
            sr.rect(iconX, iconY, 4, ICON_SZ);
        }
        sr.end();

        // ── Pass 2: all borders (Line mode) ─────────────────────────────────
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(0.48f, 0.40f, 0.18f, 1f);
        for (int i = 0; i < 6; i++) {
            sr.rect(cellX[i], cellY[i], CELL_W, CELL_H);
        }
        sr.end();

        // ── Pass 3: all text ─────────────────────────────────────────────────
        batch.setProjectionMatrix(proj);
        batch.begin();
        for (int i = 0; i < 6; i++) {
            int cx = cellX[i], cy = cellY[i];

            // Skill name — small grey text below icon
            font.getData().setScale(0.7f);
            font.setColor(0.75f, 0.70f, 0.60f, 1f);
            String name  = SKILL_NAMES[i];
            float  nameW = name.length() * 5.8f;
            font.draw(batch, name, cx + (CELL_W - nameW) / 2f, cy + CELL_H - 34);

            // Level — large yellow number
            font.getData().setScale(1.0f);
            font.setColor(1f, 0.85f, 0.10f, 1f);
            String lvl  = String.valueOf(skillLevels[i]);
            float  lvlW = lvl.length() * 8f;
            font.draw(batch, lvl, cx + (CELL_W - lvlW) / 2f, cy + 22);
        }
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();

        // ── Tooltip: show XP info when mouse hovers a skill cell ─────────────
        for (int i = 0; i < 6; i++) {
            if (mouseX >= cellX[i] && mouseX < cellX[i] + CELL_W
             && mouseY >= cellY[i] && mouseY < cellY[i] + CELL_H) {
                renderSkillTooltip(sr, batch, font, proj, i, mouseX, mouseY, screenW, screenH);
                break;
            }
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
        font.draw(batch, SKILL_NAMES[skillIdx], tipX + T_PAD, tipY + TIP_H - T_PAD);

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
            int tabW   = PANEL_W / TABS.length;
            int tabIdx = Math.min((mx - panelX) / tabW, TABS.length - 1);
            if (tabIdx >= 0) activeTab = TABS[tabIdx];
            return -1;
        }

        // ── Content area click ─────────────────────────────────────────────
        switch (activeTab) {
            case COMBAT -> {
                int pad  = 10;
                int btnW = (PANEL_W - pad * 3) / 2;
                int btnH = 48;
                int gridTop = panelY + CONTENT_H - 28;

                for (int i = 0; i < 4; i++) {
                    int col = i % 2;
                    int row = i / 2;
                    int bx  = panelX + pad + col * (btnW + pad);
                    int by  = gridTop - (row + 1) * (btnH + pad);

                    if (mx >= bx && mx <= bx + btnW && my >= by && my <= by + btnH) {
                        combatStyle = i;
                        return i;
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

    // -----------------------------------------------------------------------
    // Drag support (inventory)
    // -----------------------------------------------------------------------

    public void updateDrag(int mx, int my)    { inventoryUI.updateDrag(mx, my); }
    public boolean isInventoryDragging()      { return inventoryUI.isDragging(); }
    public int[]   handleInventoryMouseUp(int mx, int my) { return inventoryUI.handleMouseUp(mx, my); }
    public int     getInventoryRightClickSlot(int mx, int my) { return inventoryUI.getRightClickSlot(mx, my); }

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

    public void setCombatStyle(int style) {
        if (style >= 0 && style < 4) combatStyle = style;
    }

    // -----------------------------------------------------------------------
    // Accessors for GameScreen
    // -----------------------------------------------------------------------

    public int    getInventoryItemId(int slot)    { return inventoryUI.getItemId(slot); }
    public String getInventoryItemName(int slot)  { return inventoryUI.getName(slot); }
    public int    getInventoryItemFlags(int slot) { return inventoryUI.getFlags(slot); }
    public int    getCombatStyle()                { return combatStyle; }
}
