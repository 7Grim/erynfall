package com.osrs.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;

/**
 * OSRS-style inventory panel.
 *
 * 28 slots arranged in a 4-column × 7-row grid.
 * Panel is anchored to the bottom-right corner of the screen.
 * Supports drag-and-drop slot swapping.
 *
 * All coordinates are in LibGDX screen-space (Y=0 at bottom).
 */
public class InventoryUI {

    // Layout constants
    private static final int COLS       = 4;
    private static final int ROWS       = 7;
    private static final int SLOTS      = COLS * ROWS;   // 28
    private static final int SLOT_SIZE  = 40;
    private static final int SLOT_GAP   = 2;
    private static final int PANEL_PAD  = 8;

    // Panel dimensions
    private static final int PANEL_W = COLS * SLOT_SIZE + (COLS + 1) * SLOT_GAP + PANEL_PAD * 2;
    private static final int PANEL_H = ROWS * SLOT_SIZE + (ROWS + 1) * SLOT_GAP + PANEL_PAD * 2;

    // Stack quantity colour thresholds (OSRS)
    private static final Color QTY_YELLOW = new Color(1f, 1f, 0f, 1f);   // < 100k
    private static final Color QTY_GREEN = new Color(0f, 0.80f, 0f, 1f); // 100k - 9.99M
    private static final Color QTY_CYAN = new Color(0f, 1f, 1f, 1f);     // >= 10M

    // Item data (synced from ClientPacketHandler)
    private final int[]    itemIds    = new int[SLOTS];
    private final int[]    quantities = new int[SLOTS];
    private final String[] names      = new String[SLOTS];
    private final int[]    flags      = new int[SLOTS];

    // Drag state
    private int dragSlot   = -1;
    private int selectedSlot = -1;
    private int dragMouseX = 0;
    private int dragMouseY = 0;

    // Cached panel origin (recomputed in render)
    private int panelX, panelY;

    // Tooltip hover state
    private int hoveredSlot = -1;
    private float hoverTimer = 0f;
    private final GlyphLayout tooltipLayout = new GlyphLayout();
    private final GlyphLayout qtyLayout = new GlyphLayout();

    // -----------------------------------------------------------------------
    // Data mutators (called from GameScreen after syncing handler data)
    // -----------------------------------------------------------------------

    public void setSlot(int slot, int itemId, int qty, String name, int itemFlags) {
        if (slot < 0 || slot >= SLOTS) return;
        itemIds[slot]    = itemId;
        quantities[slot] = qty;
        names[slot]      = name != null ? name : "";
        flags[slot]      = itemFlags;
    }

    // -----------------------------------------------------------------------
    // Update
    // -----------------------------------------------------------------------

    public void update(float delta) {
        if (hoveredSlot >= 0 && hoveredSlot < SLOTS && itemIds[hoveredSlot] > 0) {
            hoverTimer += delta;
        } else {
            hoverTimer = 0f;
            hoveredSlot = -1;
        }
    }

    // -----------------------------------------------------------------------
    // Rendering
    // -----------------------------------------------------------------------

    /**
     * Render the inventory panel in screen space.
     *
     * @param sr      ShapeRenderer (not currently in a begin/end block when called)
     * @param batch   SpriteBatch   (not currently in a begin/end block when called)
     * @param font    BitmapFont for stack counts and item names
     * @param screenW current screen width
     * @param screenH current screen height
     */
    /**
     * Render the inventory panel at the given origin with the given projection.
     * Called by SidePanel which controls the panel's screen position.
     */
    public void render(ShapeRenderer sr, SpriteBatch batch, BitmapFont font,
                       int panelX, int panelY, Matrix4 proj) {

        this.panelX = panelX;
        this.panelY = panelY;

        // ----- Background panel -----
        sr.setProjectionMatrix(proj);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.15f, 0.15f, 0.15f, 0.9f);
        sr.rect(panelX, panelY, PANEL_W, PANEL_H);
        sr.end();

        // Panel border
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(0.5f, 0.45f, 0.35f, 1f);
        sr.rect(panelX, panelY, PANEL_W, PANEL_H);
        sr.end();

        // ----- Slots -----
        int hoverSlot = (dragSlot >= 0) ? getSlotAt(dragMouseX, dragMouseY) : -1;

        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (int slot = 0; slot < SLOTS; slot++) {
            if (slot == dragSlot) continue;  // render dragged slot last / as floating
            float[] pos = slotPos(slot);
            boolean occupied = itemIds[slot] > 0;

            sr.setColor(occupied
                ? new Color(0.28f, 0.25f, 0.20f, 1f)
                : new Color(0.20f, 0.20f, 0.20f, 1f));
            sr.rect(pos[0], pos[1], SLOT_SIZE, SLOT_SIZE);

            // Drop target feedback while dragging
            if (dragSlot >= 0 && hoverSlot == slot && slot != dragSlot) {
                sr.setColor(1f, 0.85f, 0.1f, 0.18f);
                sr.rect(pos[0] + 1, pos[1] + 1, SLOT_SIZE - 2, SLOT_SIZE - 2);
            }
        }
        sr.end();

        // Slot borders
        sr.begin(ShapeRenderer.ShapeType.Line);
        for (int slot = 0; slot < SLOTS; slot++) {
            if (slot == dragSlot) continue;
            float[] pos = slotPos(slot);
            if (selectedSlot == slot) {
                // Double gold border for selected slot
                sr.setColor(1f, 0.85f, 0.1f, 1f);
                sr.rect(pos[0] + 1, pos[1] + 1, SLOT_SIZE - 2, SLOT_SIZE - 2);
                sr.rect(pos[0] + 2, pos[1] + 2, SLOT_SIZE - 4, SLOT_SIZE - 4);
            } else {
                // Single gray border for unselected slots
                sr.setColor(0.40f, 0.36f, 0.28f, 1f);
                sr.rect(pos[0], pos[1], SLOT_SIZE, SLOT_SIZE);
            }

            if (dragSlot >= 0 && hoverSlot == slot && slot != dragSlot) {
                sr.setColor(1f, 0.85f, 0.1f, 1f);
                sr.rect(pos[0] + 1, pos[1] + 1, SLOT_SIZE - 2, SLOT_SIZE - 2);
            }
        }
        sr.end();

        // ----- Item icons (coloured squares inside each slot) -----
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (int slot = 0; slot < SLOTS; slot++) {
            if (slot == dragSlot) continue;
            if (itemIds[slot] == 0) continue;
            float[] pos = slotPos(slot);
            ItemIconRenderer.drawItemIcon(sr, pos[0], pos[1], itemIds[slot]);
        }
        sr.end();

        // ----- Tooltip on hover (300ms delay) -----
        if (hoverTimer >= 0.3f && hoveredSlot >= 0 && hoveredSlot < SLOTS && !names[hoveredSlot].isEmpty()) {
            String itemName = names[hoveredSlot];

            font.getData().setScale(FontManager.getScale(FontManager.FontContext.TOOLTIP));
            tooltipLayout.setText(font, itemName);

            float[] slotPos = slotPos(hoveredSlot);
            float tooltipW = tooltipLayout.width + 16f;
            float tooltipH = 20f;
            float tooltipX = slotPos[0] + SLOT_SIZE * 0.5f - tooltipW * 0.5f;
            float tooltipY = slotPos[1] - tooltipH - 6f;

            sr.setProjectionMatrix(proj);
            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(0.15f, 0.12f, 0.08f, 0.95f);
            sr.rect(tooltipX, tooltipY, tooltipW, tooltipH);
            sr.end();

            sr.begin(ShapeRenderer.ShapeType.Line);
            sr.setColor(0.55f, 0.42f, 0.28f, 1f);
            sr.rect(tooltipX, tooltipY, tooltipW, tooltipH);
            sr.end();

            batch.setProjectionMatrix(proj);
            batch.begin();
            font.setColor(1f, 0.90f, 0.45f, 1f);
            font.draw(batch, itemName, tooltipX + 8f, tooltipY + 14f);
            batch.end();

            font.getData().setScale(FontManager.getScale(FontManager.FontContext.BASE_UI));
            font.setColor(Color.WHITE);
        }

        // ----- Stack counts (top-left, OSRS colours: yellow/green/cyan) -----
        batch.setProjectionMatrix(proj);
        batch.begin();
        font.getData().setScale(0.75f);
        for (int slot = 0; slot < SLOTS; slot++) {
            if (slot == dragSlot) continue;
            if (itemIds[slot] == 0) continue;
            float[] pos = slotPos(slot);
            if (quantities[slot] > 1) {
                String qty = formatStackCount(quantities[slot]);
                qtyLayout.setText(font, qty);
                // 1px black drop-shadow
                font.setColor(0f, 0f, 0f, 1f);
                font.draw(batch, qty, pos[0] + 3, pos[1] + SLOT_SIZE - 2);
                // Colour-coded text
                font.setColor(qtyColor(quantities[slot]));
                font.draw(batch, qty, pos[0] + 2, pos[1] + SLOT_SIZE - 1);
            }
        }

        if (dragSlot >= 0 && itemIds[dragSlot] > 0) {
            if (quantities[dragSlot] > 1) {
                float fx = dragMouseX - SLOT_SIZE / 2f;
                float fy = dragMouseY - SLOT_SIZE / 2f + 6f;
                String qty = formatStackCount(quantities[dragSlot]);
                font.setColor(0f, 0f, 0f, 1f);
                font.draw(batch, qty, fx + 3, fy + SLOT_SIZE - 2);
                font.setColor(qtyColor(quantities[dragSlot]));
                font.draw(batch, qty, fx + 2, fy + SLOT_SIZE - 1);
            }
        }
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();

        // ----- Floating dragged item icon -----
        if (dragSlot >= 0 && itemIds[dragSlot] > 0) {
            sr.begin(ShapeRenderer.ShapeType.Filled);
            float fx = dragMouseX - SLOT_SIZE / 2f;
            float fy = dragMouseY - SLOT_SIZE / 2f + 6f;

            // Semi-transparent shadow below dragged slot
            sr.setColor(0f, 0f, 0f, 0.28f);
            sr.rect(fx + 3, fy - 4, SLOT_SIZE, SLOT_SIZE);

            sr.setColor(0.28f, 0.25f, 0.20f, 0.88f);
            sr.rect(fx, fy, SLOT_SIZE, SLOT_SIZE);

            if (selectedSlot == dragSlot) {
                sr.setColor(1f, 0.85f, 0.1f, 0.85f);
                sr.rect(fx + 1, fy + 1, SLOT_SIZE - 2, 1);
                sr.rect(fx + 1, fy + SLOT_SIZE - 2, SLOT_SIZE - 2, 1);
                sr.rect(fx + 1, fy + 1, 1, SLOT_SIZE - 2);
                sr.rect(fx + SLOT_SIZE - 2, fy + 1, 1, SLOT_SIZE - 2);
            }
            ItemIconRenderer.drawItemIcon(sr, fx, fy, itemIds[dragSlot]);
            sr.end();
        }
    }

    private String formatStackCount(int qty) {
        if (qty >= 1_000_000) {
            return (qty / 1_000_000) + "M";
        }
        if (qty >= 1_000) {
            return (qty / 1_000) + "k";
        }
        return String.valueOf(qty);
    }

    private Color qtyColor(int qty) {
        if (qty >= 10_000_000) return QTY_CYAN;
        if (qty >= 100_000) return QTY_GREEN;
        return QTY_YELLOW;
    }

    // -----------------------------------------------------------------------
    // Hit-testing
    // -----------------------------------------------------------------------

    /**
     * Returns the slot at (mx, my) in screen coords (Y=0 at bottom), or -1 if none.
     */
    public int getSlotAt(int mx, int my) {
        int rx = mx - panelX - PANEL_PAD;
        int ry = my - panelY - PANEL_PAD;
        if (rx < 0 || ry < 0) return -1;

        int col = rx / (SLOT_SIZE + SLOT_GAP);
        int rowFromBottom = ry / (SLOT_SIZE + SLOT_GAP);
        if (col >= COLS || rowFromBottom >= ROWS) return -1;

        // Check that the click is within the slot area (not the gap)
        int localX = rx - col * (SLOT_SIZE + SLOT_GAP);
        int localY = ry - rowFromBottom * (SLOT_SIZE + SLOT_GAP);
        if (localX >= SLOT_SIZE || localY >= SLOT_SIZE) return -1;

        // Inventory indices are read top-left -> bottom-right.
        int rowFromTop = (ROWS - 1) - rowFromBottom;
        return rowFromTop * COLS + col;
    }

    /** Returns true if (mx, my) is over the inventory panel. */
    public boolean isOverPanel(int mx, int my) {
        return mx >= panelX && mx <= panelX + PANEL_W
            && my >= panelY && my <= panelY + PANEL_H;
    }

    /**
     * Mouse-down handler.  Starts a drag if over an occupied slot.
     * Returns the slot index, or -1.
     */
    public int handleMouseDown(int mx, int my, int button) {
        int slot = getSlotAt(mx, my);
        if (slot >= 0 && itemIds[slot] > 0 && button == 0) {
            dragSlot   = slot;
            dragMouseX = mx;
            dragMouseY = my;
        }
        return slot;
    }

    /**
     * Update drag position every frame while dragging.
     */
    public void updateDrag(int mx, int my) {
        if (dragSlot >= 0) {
            dragMouseX = mx;
            dragMouseY = my;
        }
    }

    /**
     * Mouse-up handler.
     * Returns int[]{fromSlot, toSlot} if a drag-swap occurred, null otherwise.
     */
    public int[] handleMouseUp(int mx, int my) {
        if (dragSlot < 0) return null;
        int from = dragSlot;
        dragSlot = -1;

        int to = getSlotAt(mx, my);
        if (to >= 0 && to != from) {
            return new int[]{from, to};
        }
        return null;
    }

    /**
     * Returns the slot under the cursor for right-click, or -1.
     */
    public int getRightClickSlot(int mx, int my) {
        return getSlotAt(mx, my);
    }

    /**
     * Returns the centre of a slot in screen coords.
     */
    public float[] getSlotScreenPos(int slot) {
        float[] pos = slotPos(slot);
        return new float[]{pos[0] + SLOT_SIZE / 2f, pos[1] + SLOT_SIZE / 2f};
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    public int    getItemId(int slot)   { return (slot >= 0 && slot < SLOTS) ? itemIds[slot]    : 0; }
    public int    getQuantity(int slot) { return (slot >= 0 && slot < SLOTS) ? quantities[slot] : 0; }
    public String getName(int slot)     { return (slot >= 0 && slot < SLOTS) ? names[slot]      : ""; }
    public int    getFlags(int slot)    { return (slot >= 0 && slot < SLOTS) ? flags[slot]      : 0; }
    public void setSelectedSlot(int slot) { this.selectedSlot = slot; }

    public boolean isDragging()  { return dragSlot >= 0; }
    public int     getDragSlot() { return dragSlot; }
    public void setHoveredSlot(int slot) {
        if (slot != hoveredSlot) {
            hoverTimer = 0f;
        }
        hoveredSlot = slot;
        if (slot < 0 || slot >= SLOTS) {
            hoverTimer = 0f;
        }
    }
    public int     getPanelWidth() { return PANEL_W; }
    public int     getPanelHeight() { return PANEL_H; }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Bottom-left corner of a slot in screen space (Y=0 at bottom).
     * Slot indexing follows top-left -> bottom-right reading order.
     */
    private float[] slotPos(int slot) {
        int col = slot % COLS;
        int rowFromTop = slot / COLS;
        int rowFromBottom = (ROWS - 1) - rowFromTop;
        float x = panelX + PANEL_PAD + SLOT_GAP + col * (SLOT_SIZE + SLOT_GAP);
        float y = panelY + PANEL_PAD + SLOT_GAP + rowFromBottom * (SLOT_SIZE + SLOT_GAP);
        return new float[]{x, y};
    }
}
