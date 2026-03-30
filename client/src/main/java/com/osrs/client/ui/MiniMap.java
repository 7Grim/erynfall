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
                       int[][] tileMap, ClientPacketHandler h) {

        // -- Centre of the minimap circle --
        // Anchored: right edge aligns with left edge of side panel minus a margin
        int cx = screenW - SidePanel.PANEL_W - SidePanel.MARGIN - RADIUS - SidePanel.MARGIN;
        int cy = screenH - RADIUS - SidePanel.MARGIN;

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

                float mx = cx + dtx * TILE_PX;
                float my = cy + dty * TILE_PX;

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
                float mx = cx + (item[0] - playerTileX) * TILE_PX;
                float my = cy + (item[1] - playerTileY) * TILE_PX;
                float dd = (mx - cx) * (mx - cx) + (my - cy) * (my - cy);
                if (dd > (RADIUS - 4) * (RADIUS - 4)) continue;
                sr.circle(mx, my, 2.5f);
            }

            // NPCs -- red (hostile) or yellow (friendly/resource)
            for (Map.Entry<Integer, int[]> entry : h.getEntityPositions().entrySet()) {
                int id = entry.getKey();
                if (h.isPlayer(id)) continue;
                int[] pos = entry.getValue();
                float mx = cx + (pos[0] - playerTileX) * TILE_PX;
                float my = cy + (pos[1] - playerTileY) * TILE_PX;
                float dd = (mx - cx) * (mx - cx) + (my - cy) * (my - cy);
                if (dd > (RADIUS - 4) * (RADIUS - 4)) continue;
                if (h.isNpcHostile(id)) {
                    sr.setColor(1f, 0.15f, 0.15f, 1f);
                } else {
                    sr.setColor(1f, 0.95f, 0.10f, 1f);
                }
                sr.circle(mx, my, 3.5f);
            }
        }

        // -- Player dot (white, always at centre) --
        sr.setColor(1f, 1f, 1f, 1f);
        sr.circle(cx, cy, 3.5f);

        sr.end();

        // -- Compass ring border --
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(0.20f, 0.16f, 0.08f, 1f);
        sr.circle(cx, cy, RADIUS);
        // 1px gold outline for OSRS compass styling
        sr.setColor(0.80f, 0.68f, 0.28f, 1f);
        sr.circle(cx, cy, RADIUS + 1);
        sr.end();

        // -- "N" compass label --
        batch.setProjectionMatrix(proj);
        batch.begin();
        font.getData().setScale(0.75f);
        font.setColor(1f, 0.92f, 0.20f, 1f);     // gold
        GlyphLayout nGlyph = new GlyphLayout(font, "N");
        font.draw(batch, "N", cx - nGlyph.width / 2f, cy + RADIUS - 2);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }
}
