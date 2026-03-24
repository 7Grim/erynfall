package com.osrs.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
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

    // Item data (synced from ClientPacketHandler)
    private final int[]    itemIds    = new int[SLOTS];
    private final int[]    quantities = new int[SLOTS];
    private final String[] names      = new String[SLOTS];
    private final int[]    flags      = new int[SLOTS];

    // Drag state
    private int dragSlot   = -1;
    private int dragMouseX = 0;
    private int dragMouseY = 0;

    // Cached panel origin (recomputed in render)
    private int panelX, panelY;

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
        // No animations currently; reserved for future use.
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
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (int slot = 0; slot < SLOTS; slot++) {
            if (slot == dragSlot) continue;  // render dragged slot last / as floating
            float[] pos = slotPos(slot);
            boolean occupied = itemIds[slot] > 0;

            sr.setColor(occupied
                ? new Color(0.28f, 0.25f, 0.20f, 1f)
                : new Color(0.20f, 0.20f, 0.20f, 1f));
            sr.rect(pos[0], pos[1], SLOT_SIZE, SLOT_SIZE);
        }
        sr.end();

        // Slot borders
        sr.begin(ShapeRenderer.ShapeType.Line);
        for (int slot = 0; slot < SLOTS; slot++) {
            if (slot == dragSlot) continue;
            float[] pos = slotPos(slot);
            sr.setColor(0.40f, 0.36f, 0.28f, 1f);
            sr.rect(pos[0], pos[1], SLOT_SIZE, SLOT_SIZE);
        }
        sr.end();

        // ----- Item icons (coloured squares inside each slot) -----
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (int slot = 0; slot < SLOTS; slot++) {
            if (slot == dragSlot) continue;
            if (itemIds[slot] == 0) continue;
            float[] pos = slotPos(slot);
            drawItemIcon(sr, pos[0], pos[1], itemIds[slot], flags[slot]);
        }
        sr.end();

        // ----- Stack counts & names (batch text) -----
        batch.setProjectionMatrix(proj);
        batch.begin();
        font.getData().setScale(0.75f);
        for (int slot = 0; slot < SLOTS; slot++) {
            if (slot == dragSlot) continue;
            if (itemIds[slot] == 0) continue;
            float[] pos = slotPos(slot);
            if (quantities[slot] > 1) {
                font.setColor(1f, 0.9f, 0.1f, 1f);
                font.draw(batch, String.valueOf(quantities[slot]),
                    pos[0] + 3, pos[1] + 12);
            }
        }

        // ----- Floating dragged item -----
        if (dragSlot >= 0 && itemIds[dragSlot] > 0) {
            font.getData().setScale(0.75f);
            // Drawn after loop — just text; shape drawn separately below
        }
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();

        // ----- Floating dragged item icon -----
        if (dragSlot >= 0 && itemIds[dragSlot] > 0) {
            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(0.28f, 0.25f, 0.20f, 0.85f);
            float fx = dragMouseX - SLOT_SIZE / 2f;
            float fy = dragMouseY - SLOT_SIZE / 2f;
            sr.rect(fx, fy, SLOT_SIZE, SLOT_SIZE);
            drawItemIcon(sr, fx, fy, itemIds[dragSlot], flags[dragSlot]);
            sr.end();
        }
    }

    /**
     * Draw a small coloured item icon inside a slot rectangle.
     * Icon occupies the central 24×24 area of a 40×40 slot.
     */
    private void drawItemIcon(ShapeRenderer sr, float slotLeft, float slotBottom,
                               int itemId, int itemFlags) {
        Color c = itemColor(itemId, itemFlags);
        float iconX = slotLeft   + 8;
        float iconY = slotBottom + 8;
        float iconW = 24f;
        float iconH = 24f;
        sr.setColor(c);
        sr.rect(iconX, iconY, iconW, iconH);

        // Simple highlight (top-left corner lighter)
        sr.setColor(c.r * 1.3f, c.g * 1.3f, c.b * 1.3f, 1f);
        sr.rect(iconX, iconY + iconH - 4, iconW, 4);
        sr.rect(iconX, iconY, 4, iconH);
    }

    private Color itemColor(int itemId, int itemFlags) {
        if (itemId == 995) return new Color(1f, 0.9f, 0.1f, 1f);           // Coins: yellow
        if ((itemFlags & 0x1) != 0) return new Color(0.75f, 0.78f, 0.82f, 1f); // Equipable: steel blue-grey
        if ((itemFlags & 0x2) != 0) return new Color(1f, 0.6f, 0.2f, 1f);  // Consumable: orange
        if (itemId == 526)  return new Color(0.85f, 0.75f, 0.55f, 1f);     // Bones: tan
        return new Color(0.65f, 0.55f, 0.45f, 1f);                         // Default: brownish
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
        int row = ry / (SLOT_SIZE + SLOT_GAP);
        if (col >= COLS || row >= ROWS) return -1;

        // Check that the click is within the slot area (not the gap)
        int localX = rx - col * (SLOT_SIZE + SLOT_GAP);
        int localY = ry - row * (SLOT_SIZE + SLOT_GAP);
        if (localX >= SLOT_SIZE || localY >= SLOT_SIZE) return -1;

        return row * COLS + col;
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

    public boolean isDragging()  { return dragSlot >= 0; }
    public int     getDragSlot() { return dragSlot; }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Bottom-left corner of a slot in screen space (Y=0 at bottom).
     */
    private float[] slotPos(int slot) {
        int col = slot % COLS;
        int row = slot / COLS;
        float x = panelX + PANEL_PAD + SLOT_GAP + col * (SLOT_SIZE + SLOT_GAP);
        float y = panelY + PANEL_PAD + SLOT_GAP + row * (SLOT_SIZE + SLOT_GAP);
        return new float[]{x, y};
    }
}
