package com.osrs.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.osrs.client.network.ClientPacketHandler;
import com.osrs.client.world.TutorialIslandMap;

import java.util.Map;

/**
 * Circular minimap overlay rendered in the top-right of the game-world area.
 * Tile colours, entity dots and ground-item dots are drawn using ShapeRenderer;
 * the compass "N" label uses SpriteBatch + BitmapFont.
 */
public class MiniMap {

    public static final int RADIUS   = 60;   // circle radius in screen pixels
    public static final int TILE_PX  = 8;    // pixels per world tile on the minimap
    // How many tiles are visible in each direction from the player
    private static final int HALF_TILES = RADIUS / TILE_PX + 2;

    private static final Color COLOR_GRASS = new Color(0.27f, 0.53f, 0.15f, 1f); // Light green
    private static final Color COLOR_PATH = new Color(0.86f, 0.70f, 0.35f, 1f);  // Beige-tan
    private static final Color COLOR_WATER = new Color(0.18f, 0.70f, 0.60f, 1f); // Light blue
    private static final Color COLOR_WALL = new Color(0.31f, 0.31f, 0.31f, 1f);  // Dark gray

    private final GlyphLayout compassGlyph = new GlyphLayout();

    public static int getCenterX(int screenW) {
        return screenW - RADIUS - SidePanel.MARGIN;
    }

    public static int getCenterY(int screenH) {
        return screenH - RADIUS - SidePanel.MARGIN;
    }

    public static int getLeftX(int screenW) {
        return getCenterX(screenW) - RADIUS;
    }

    /** Render the minimap.
     *
     * @param sr          ShapeRenderer (caller must not have an active begin/end)
     * @param batch       SpriteBatch   (caller must not have an active begin)
     * @param font        BitmapFont for the "N" compass label
     * @param proj        screen projection matrix (Y=0 at bottom)
     * @param screenW     current screen width
     * @param screenH     current screen height
     * @param playerTileX player logical tile X
     * @param playerTileY player logical tile Y
     * @param tileMap     world tile data [x][y]
     * @param h           ClientPacketHandler for entity/ground-item data (may be null)
     */
    public void render(ShapeRenderer sr, SpriteBatch batch, BitmapFont font,
                       Matrix4 proj, int screenW, int screenH,
                       int playerTileX, int playerTileY,
                       int walkDestX, int walkDestY,
                       float cameraYaw,
                       int[][] tileMap, ClientPacketHandler h) {

        // -- Centre of the minimap circle --
        // Anchored directly to the screen's top-right corner.
        int cx = getCenterX(screenW);
        int cy = getCenterY(screenH);

        sr.setProjectionMatrix(proj);

        // -- Dark background fill --
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.06f, 0.08f, 0.06f, 0.92f);
        sr.circle(cx, cy, RADIUS);
        sr.end();

        // -- Terrain tiles --
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (int dtx = -HALF_TILES; dtx <= HALF_TILES; dtx++) {
            for (int dty = -HALF_TILES; dty <= HALF_TILES; dty++) {
                int tx = playerTileX + dtx;
                int ty = playerTileY + dty;
                if (tx < 0 || tx >= TutorialIslandMap.WIDTH) continue;
                if (ty < 0 || ty >= TutorialIslandMap.HEIGHT) continue;

                float[] rotated = rotateMinimapOffset(dtx, -dty, cameraYaw);
                float mx = cx + rotated[0] * TILE_PX;
                float my = cy + rotated[1] * TILE_PX;

                // Skip pixels outside the circle
                float dx = mx - cx, dy = my - cy;
                if (dx * dx + dy * dy > (RADIUS - 1) * (RADIUS - 1)) continue;

                switch (tileMap[tx][ty]) {
                    case TutorialIslandMap.GRASS -> sr.setColor(COLOR_GRASS);
                    case TutorialIslandMap.PATH, TutorialIslandMap.SAND -> sr.setColor(COLOR_PATH);
                    case TutorialIslandMap.WATER -> sr.setColor(COLOR_WATER);
                    case TutorialIslandMap.WALL  -> sr.setColor(COLOR_WALL);
                    default                      -> sr.setColor(COLOR_GRASS);
                }
                sr.rect(mx - TILE_PX / 2f, my - TILE_PX / 2f, TILE_PX, TILE_PX);
            }
        }

        // -- Entity dots --
        if (h != null) {
            // Ground items -- yellow
            sr.setColor(1f, 0.92f, 0.20f, 1f);
            for (int[] item : h.getGroundItemPositions()) {
                // item = {x, y}
                float[] rotated = rotateMinimapOffset(item[0] - playerTileX, -(item[1] - playerTileY), cameraYaw);
                float mx = cx + rotated[0] * TILE_PX;
                float my = cy + rotated[1] * TILE_PX;
                float dd = (mx - cx) * (mx - cx) + (my - cy) * (my - cy);
                if (dd > (RADIUS - 4) * (RADIUS - 4)) continue;
                sr.circle(mx, my, 2.5f);
            }

            // NPCs -- red (hostile) or yellow (friendly/resource)
            for (Map.Entry<Integer, int[]> entry : h.getEntityPositions().entrySet()) {
                int id = entry.getKey();
                if (h.isPlayer(id)) continue;
                int[] pos = entry.getValue();
                float[] rotated = rotateMinimapOffset(pos[0] - playerTileX, -(pos[1] - playerTileY), cameraYaw);
                float mx = cx + rotated[0] * TILE_PX;
                float my = cy + rotated[1] * TILE_PX;
                float dd = (mx - cx) * (mx - cx) + (my - cy) * (my - cy);
                if (dd > (RADIUS - 4) * (RADIUS - 4)) continue;
                if (h.isNpcHostile(id)) {
                    sr.setColor(1f, 0.15f, 0.15f, 1f);
                } else {
                    sr.setColor(1f, 0.95f, 0.10f, 1f);
                }
                sr.circle(mx, my, 3.5f);
            }

            if (walkDestX >= 0 && walkDestY >= 0) {
                float[] rotated = rotateMinimapOffset(walkDestX - playerTileX, -(walkDestY - playerTileY), cameraYaw);
                float fx = rotated[0] * TILE_PX;
                float fy = rotated[1] * TILE_PX;
                float len2 = fx * fx + fy * fy;
                float limit = (RADIUS - 6f);
                float markerX = cx + fx;
                float markerY = cy + fy;
                if (len2 > limit * limit) {
                    float len = (float) Math.sqrt(len2);
                    if (len > 0.0001f) {
                        markerX = cx + fx / len * limit;
                        markerY = cy + fy / len * limit;
                    }
                }
                sr.setColor(0.90f, 0.10f, 0.10f, 1f);
                sr.triangle(markerX, markerY + 5f, markerX - 3f, markerY - 3f, markerX + 3f, markerY - 3f);
                sr.rect(markerX - 1f, markerY - 5f, 2f, 4f);
            }
        }

        // -- Player dot (white, always at centre) --
        sr.setColor(1f, 1f, 1f, 1f);
        sr.circle(cx, cy, 3.5f);

        sr.end();

        // -- Compass ring border: 3-ring stone frame --
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(0.04f, 0.03f, 0.02f, 1f);     // outermost dark
        sr.circle(cx, cy, RADIUS + 3);
        sr.setColor(0.18f, 0.14f, 0.06f, 1f);     // dark brown mid
        sr.circle(cx, cy, RADIUS + 2);
        sr.setColor(0.40f, 0.32f, 0.12f, 1f);     // brown
        sr.circle(cx, cy, RADIUS + 1);
        sr.setColor(0.80f, 0.68f, 0.28f, 1f);     // gold inner edge
        sr.circle(cx, cy, RADIUS);
        sr.end();

        // -- "N" compass label --
        batch.setProjectionMatrix(proj);
        batch.begin();
        font.getData().setScale(0.75f);
        font.setColor(1f, 0.92f, 0.20f, 1f);     // gold
        compassGlyph.setText(font, "N");
        float[] north = rotateMinimapOffset(0f, 1f, cameraYaw);
        float nx = cx + north[0] * (RADIUS - 2f);
        float ny = cy + north[1] * (RADIUS - 2f);
        font.draw(batch, "N", nx - compassGlyph.width / 2f, ny + compassGlyph.height / 2f);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private float[] rotateMinimapOffset(float dxTiles, float dyTiles, float cameraYaw) {
        float cos = (float) Math.cos(-cameraYaw);
        float sin = (float) Math.sin(-cameraYaw);
        float rx = dxTiles * cos - dyTiles * sin;
        float ry = dxTiles * sin + dyTiles * cos;
        return new float[]{rx, ry};
    }
}
