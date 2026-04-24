package com.osrs.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.osrs.client.network.ClientPacketHandler;

import java.util.Map;

/**
 * Circular minimap overlay rendered in the top-right of the game-world area.
 *
 * Tile colours match the visual terrain type system (from scene.yaml / TerrainVisualLoader):
 *   0 = grass, 1 = water, 2 = path, 3 = wall, 4 = sand
 *
 * Features:
 *   - Mouse-wheel over minimap zooms in/out (3–16 px per tile)
 *   - Left-click anywhere inside the circle walks the player to that tile
 */
public class MiniMap {

    // ── Layout ────────────────────────────────────────────────────────────────
    public static final int RADIUS        = 60;
    private static final int TILE_PX_DEFAULT = 8;
    private static final int TILE_PX_MIN  = 3;
    private static final int TILE_PX_MAX  = 16;

    // ── Visual tile type IDs (must match TerrainVisualLoader / scene.yaml) ───
    private static final int VT_GRASS = 0;
    private static final int VT_WATER = 1;
    private static final int VT_PATH  = 2;
    private static final int VT_WALL  = 3;
    private static final int VT_SAND  = 4;

    // ── OSRS minimap colour palette ───────────────────────────────────────────
    // Reference: flat, saturated, clearly distinct colour palette
    private static final Color COLOR_GRASS = new Color(0.31f, 0.57f, 0.17f, 1f); // #4F9130 med green
    private static final Color COLOR_PATH  = new Color(0.53f, 0.43f, 0.25f, 1f); // #876E40 dirt brown
    private static final Color COLOR_WATER = new Color(0.11f, 0.40f, 0.68f, 1f); // #1C66AD OSRS blue
    private static final Color COLOR_WALL  = new Color(0.28f, 0.28f, 0.28f, 1f); // #474747 stone gray
    private static final Color COLOR_SAND  = new Color(0.76f, 0.66f, 0.38f, 1f); // #C2A861 sandy yellow

    // ── State ─────────────────────────────────────────────────────────────────
    private int tilePx = TILE_PX_DEFAULT;

    private final GlyphLayout compassGlyph = new GlyphLayout();

    // ── Position helpers ──────────────────────────────────────────────────────

    public static int getCenterX(int screenW) {
        return screenW - RADIUS - SidePanel.MARGIN;
    }

    public static int getCenterY(int screenH) {
        return screenH - RADIUS - SidePanel.MARGIN;
    }

    public static int getLeftX(int screenW) {
        return getCenterX(screenW) - RADIUS;
    }

    /** True if screen point (sx, sy) is inside the minimap circle (with 4px hit padding). */
    public static boolean isOverMinimap(int sx, int sy, int screenW, int screenH) {
        int cx = getCenterX(screenW);
        int cy = getCenterY(screenH);
        float dx = sx - cx, dy = sy - cy;
        float r = RADIUS + 4f;
        return dx * dx + dy * dy <= r * r;
    }

    // ── Interaction ───────────────────────────────────────────────────────────

    /**
     * Adjust zoom level by one step in the given scroll direction.
     * amountY > 0 = scroll down = zoom out; < 0 = scroll up = zoom in.
     */
    public void handleScroll(float amountY) {
        int delta = amountY > 0 ? -1 : 1;   // scroll up = more px per tile = zoom in
        tilePx = Math.max(TILE_PX_MIN, Math.min(TILE_PX_MAX, tilePx + delta));
    }

    /**
     * Returns the world tile [x, y] for a screen click inside the minimap, or null
     * if the click is outside the circle.
     *
     * Inverse of the render projection:
     *   screen_offset = rotate(-cameraYaw) * [dtx, -dty] * tilePx
     * Solved for (dtx, dty) given screen offset.
     */
    public int[] getTileAtScreenPos(int screenX, int screenY,
                                     int screenW, int screenH,
                                     float cameraYaw,
                                     int playerTileX, int playerTileY) {
        int cx = getCenterX(screenW);
        int cy = getCenterY(screenH);
        float dx = screenX - cx;
        float dy = screenY - cy;
        if (dx * dx + dy * dy > (float) RADIUS * RADIUS) return null;

        // Screen-pixel offset → rotated tile offset
        float rx = dx / tilePx;
        float ry = dy / tilePx;

        // Inverse rotation: apply +cameraYaw to recover [dtx, dty]
        float cos = (float) Math.cos(cameraYaw);
        float sin = (float) Math.sin(cameraYaw);
        float dtx =  rx * cos - ry * sin;
        float dty = -(rx * sin + ry * cos);   // negate because forward pass used -dty

        int tx = Math.round(playerTileX + dtx);
        int ty = Math.round(playerTileY + dty);
        return new int[]{tx, ty};
    }

    // ── Render ────────────────────────────────────────────────────────────────

    /**
     * Render the minimap.
     *
     * @param visualTileMap  Client visual tile map (TerrainVisualLoader IDs: 0=grass 1=water
     *                       2=path 3=wall 4=sand).  May be null; falls back to empty.
     */
    public void render(ShapeRenderer sr, SpriteBatch batch, BitmapFont font,
                       Matrix4 proj, int screenW, int screenH,
                       int playerTileX, int playerTileY,
                       int walkDestX, int walkDestY,
                       float cameraYaw,
                       int[][] visualTileMap, ClientPacketHandler h) {

        int cx = getCenterX(screenW);
        int cy = getCenterY(screenH);
        int halfTiles = RADIUS / tilePx + 2;

        int mapW = (visualTileMap != null && visualTileMap.length > 0) ? visualTileMap.length : 0;
        int mapH = (mapW > 0 && visualTileMap[0] != null) ? visualTileMap[0].length : 0;

        sr.setProjectionMatrix(proj);

        // ── Dark background fill ─────────────────────────────────────────────
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.06f, 0.08f, 0.06f, 0.92f);
        sr.circle(cx, cy, RADIUS);
        sr.end();

        // ── Terrain tiles ────────────────────────────────────────────────────
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (int dtx = -halfTiles; dtx <= halfTiles; dtx++) {
            for (int dty = -halfTiles; dty <= halfTiles; dty++) {
                int tx = playerTileX + dtx;
                int ty = playerTileY + dty;
                if (tx < 0 || tx >= mapW || ty < 0 || ty >= mapH) continue;

                float[] rot = rotateOffset(dtx, -dty, cameraYaw);
                float mx = cx + rot[0] * tilePx;
                float my = cy + rot[1] * tilePx;

                // Skip outside circle
                float ox = mx - cx, oy = my - cy;
                if (ox * ox + oy * oy > (RADIUS - 1f) * (RADIUS - 1f)) continue;

                int vt = visualTileMap[tx][ty];
                switch (vt) {
                    case VT_WATER -> sr.setColor(COLOR_WATER);
                    case VT_PATH  -> sr.setColor(COLOR_PATH);
                    case VT_WALL  -> sr.setColor(COLOR_WALL);
                    case VT_SAND  -> sr.setColor(COLOR_SAND);
                    default       -> sr.setColor(COLOR_GRASS);
                }
                sr.rect(mx - tilePx * 0.5f, my - tilePx * 0.5f, tilePx, tilePx);
            }
        }

        // ── Entity dots ──────────────────────────────────────────────────────
        if (h != null) {
            // Ground items — yellow dot
            sr.setColor(1f, 0.92f, 0.20f, 1f);
            for (int[] item : h.getGroundItemPositions()) {
                float[] rot = rotateOffset(item[0] - playerTileX, -(item[1] - playerTileY), cameraYaw);
                float mx = cx + rot[0] * tilePx;
                float my = cy + rot[1] * tilePx;
                float ox = mx - cx, oy = my - cy;
                if (ox * ox + oy * oy > (RADIUS - 4f) * (RADIUS - 4f)) continue;
                sr.circle(mx, my, 2.5f);
            }

            // NPCs — red (hostile) / yellow (friendly)
            for (Map.Entry<Integer, int[]> entry : h.getEntityPositions().entrySet()) {
                int id = entry.getKey();
                if (h.isPlayer(id)) continue;
                int[] pos = entry.getValue();
                float[] rot = rotateOffset(pos[0] - playerTileX, -(pos[1] - playerTileY), cameraYaw);
                float mx = cx + rot[0] * tilePx;
                float my = cy + rot[1] * tilePx;
                float ox = mx - cx, oy = my - cy;
                if (ox * ox + oy * oy > (RADIUS - 4f) * (RADIUS - 4f)) continue;
                sr.setColor(h.isNpcHostile(id) ? new Color(1f, 0.15f, 0.15f, 1f)
                                                : new Color(1f, 0.95f, 0.10f, 1f));
                sr.circle(mx, my, 3.5f);
            }

            // Walk destination marker (red pin)
            if (walkDestX >= 0 && walkDestY >= 0) {
                float[] rot = rotateOffset(walkDestX - playerTileX, -(walkDestY - playerTileY), cameraYaw);
                float fx = rot[0] * tilePx;
                float fy = rot[1] * tilePx;
                float limit = RADIUS - 6f;
                float len2  = fx * fx + fy * fy;
                float markerX = cx + fx;
                float markerY = cy + fy;
                if (len2 > limit * limit) {
                    float len = (float) Math.sqrt(len2);
                    if (len > 0.0001f) { markerX = cx + fx / len * limit; markerY = cy + fy / len * limit; }
                }
                sr.setColor(0.90f, 0.10f, 0.10f, 1f);
                sr.triangle(markerX, markerY + 5f, markerX - 3f, markerY - 3f, markerX + 3f, markerY - 3f);
                sr.rect(markerX - 1f, markerY - 5f, 2f, 4f);
            }
        }

        // ── Player dot (white, always at centre) ─────────────────────────────
        sr.setColor(1f, 1f, 1f, 1f);
        sr.circle(cx, cy, 3.5f);

        sr.end();

        // ── Compass border: 4-ring stone frame ───────────────────────────────
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(0.04f, 0.03f, 0.02f, 1f);
        sr.circle(cx, cy, RADIUS + 3);
        sr.setColor(0.18f, 0.14f, 0.06f, 1f);
        sr.circle(cx, cy, RADIUS + 2);
        sr.setColor(0.40f, 0.32f, 0.12f, 1f);
        sr.circle(cx, cy, RADIUS + 1);
        sr.setColor(0.80f, 0.68f, 0.28f, 1f);
        sr.circle(cx, cy, RADIUS);
        sr.end();

        // ── "N" compass label ─────────────────────────────────────────────────
        batch.setProjectionMatrix(proj);
        batch.begin();
        font.getData().setScale(0.75f);
        font.setColor(1f, 0.92f, 0.20f, 1f);
        compassGlyph.setText(font, "N");
        float[] north = rotateOffset(0f, 1f, cameraYaw);
        float nx = cx + north[0] * (RADIUS - 2f);
        float ny = cy + north[1] * (RADIUS - 2f);
        font.draw(batch, "N", nx - compassGlyph.width / 2f, ny + compassGlyph.height / 2f);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /** Rotate a 2D tile offset by -cameraYaw (maps tile-space to screen-space). */
    private static float[] rotateOffset(float dx, float dy, float cameraYaw) {
        float cos = (float) Math.cos(-cameraYaw);
        float sin = (float) Math.sin(-cameraYaw);
        return new float[]{dx * cos - dy * sin, dx * sin + dy * cos};
    }
}
