package com.osrs.client.renderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Isometric tile + entity renderer.
 *
 * Coordinate formula (32×16 tile metrics):
 *   screenX = (tileX - tileY) * 16
 *   screenY = (tileX + tileY) *  8
 */
public class IsometricRenderer {

    public static final int TILE_WIDTH  = 32;
    public static final int TILE_HEIGHT = 16;
    public static final int MAP_WIDTH   = 104;
    public static final int MAP_HEIGHT  = 104;

    /** Only render tiles within this radius of the camera centre (viewport culling). */
    private static final int RENDER_RADIUS = 36;

    // --- Tile colours: two variants per type for checkerboard micro-texture ---
    private static final Color GRASS_A = new Color(0.27f, 0.60f, 0.15f, 1f);
    private static final Color GRASS_B = new Color(0.22f, 0.50f, 0.11f, 1f);
    private static final Color PATH_A  = new Color(0.60f, 0.44f, 0.20f, 1f);
    private static final Color PATH_B  = new Color(0.50f, 0.36f, 0.15f, 1f);
    private static final Color WATER_A = new Color(0.12f, 0.44f, 0.82f, 1f);
    private static final Color WATER_B = new Color(0.09f, 0.34f, 0.68f, 1f);
    private static final Color SAND_A  = new Color(0.84f, 0.74f, 0.44f, 1f);
    private static final Color SAND_B  = new Color(0.72f, 0.62f, 0.34f, 1f);
    private static final Color WALL_A  = new Color(0.54f, 0.54f, 0.54f, 1f);
    private static final Color WALL_B  = new Color(0.40f, 0.40f, 0.40f, 1f);

    // --- Shared entity palette ---
    private static final Color SKIN    = new Color(0.92f, 0.72f, 0.52f, 1f);
    private static final Color HAIR    = new Color(0.28f, 0.17f, 0.07f, 1f);
    private static final Color PANTS   = new Color(0.34f, 0.21f, 0.09f, 1f);

    private final OrthographicCamera camera;
    private final SpriteBatch batch;
    private final ShapeRenderer sr;

    public IsometricRenderer(OrthographicCamera camera, SpriteBatch batch, ShapeRenderer shapeRenderer) {
        this.camera = camera;
        this.batch  = batch;
        this.sr     = shapeRenderer;
    }

    // -----------------------------------------------------------------------
    // Coordinate conversion
    // -----------------------------------------------------------------------

    public float worldToScreenX(int tileX, int tileY) {
        return (tileX - tileY) * (TILE_WIDTH / 2.0f);
    }

    public float worldToScreenY(int tileX, int tileY) {
        return (tileX + tileY) * (TILE_HEIGHT / 2.0f);
    }

    public float worldToScreenX(float tileX, float tileY) {
        return (tileX - tileY) * (TILE_WIDTH / 2.0f);
    }

    public float worldToScreenY(float tileX, float tileY) {
        return (tileX + tileY) * (TILE_HEIGHT / 2.0f);
    }

    // -----------------------------------------------------------------------
    // Tile map
    // -----------------------------------------------------------------------

    /**
     * Render the visible portion of the tile map as filled coloured diamonds.
     * Only tiles within RENDER_RADIUS of (centerX, centerY) are drawn.
     *
     * @param tileMap  104×104 array of tile type ints (0=grass … 4=wall)
     * @param centerX  visual player X (used for viewport culling)
     * @param centerY  visual player Y
     */
    public void renderWorld(int[][] tileMap, float centerX, float centerY) {
        int cx = (int) centerX;
        int cy = (int) centerY;

        int minX = Math.max(0, cx - RENDER_RADIUS);
        int maxX = Math.min(MAP_WIDTH  - 1, cx + RENDER_RADIUS);
        int minY = Math.max(0, cy - RENDER_RADIUS);
        int maxY = Math.min(MAP_HEIGHT - 1, cy + RENDER_RADIUS);

        sr.setProjectionMatrix(camera.combined);
        sr.begin(ShapeRenderer.ShapeType.Filled);

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                int type = (tileMap != null) ? tileMap[x][y] : 0;
                float sx = worldToScreenX(x, y);
                float sy = worldToScreenY(x, y);
                fillDiamond(sx, sy, tileColor(type, x, y));
            }
        }

        sr.end();
    }

    /** Draw a filled isometric diamond centred at (sx, sy). */
    private void fillDiamond(float sx, float sy, Color c) {
        float hw = TILE_WIDTH  / 2f;  // 16
        float hh = TILE_HEIGHT / 2f;  //  8
        sr.setColor(c);
        sr.triangle(sx,      sy - hh,   sx + hw, sy,   sx - hw, sy);  // upper half
        sr.triangle(sx,      sy + hh,   sx + hw, sy,   sx - hw, sy);  // lower half
    }

    /** Two-variant checkerboard colour per tile type. */
    private Color tileColor(int type, int x, int y) {
        boolean a = (x + y) % 2 == 0;
        switch (type) {
            case 1:  return a ? PATH_A  : PATH_B;
            case 2:  return a ? WATER_A : WATER_B;
            case 3:  return a ? SAND_A  : SAND_B;
            case 4:  return a ? WALL_A  : WALL_B;
            default: return a ? GRASS_A : GRASS_B;
        }
    }

    // -----------------------------------------------------------------------
    // Entity sprites
    // -----------------------------------------------------------------------

    /**
     * Player — blue shirt, brown trousers, skin head, dark hair.
     * Accepts float coords for smooth interpolated movement.
     */
    public void renderPlayer(float playerX, float playerY) {
        float sx = worldToScreenX(playerX, playerY);
        float sy = worldToScreenY(playerX, playerY);

        sr.begin(ShapeRenderer.ShapeType.Filled);

        // Legs
        sr.setColor(PANTS);
        sr.rect(sx - 4, sy - 8, 3, 6);
        sr.rect(sx + 1, sy - 8, 3, 6);

        // Shirt (OSRS blue)
        sr.setColor(0.18f, 0.38f, 0.76f, 1f);
        sr.rect(sx - 5, sy - 2, 10, 8);

        // Head
        sr.setColor(SKIN);
        sr.rect(sx - 3, sy + 6, 6, 6);

        // Hair
        sr.setColor(HAIR);
        sr.rect(sx - 3, sy + 10, 6, 2);

        sr.end();
    }

    /**
     * NPC sprite dispatcher.
     * npcId 1 = Tutorial Guide (green robe)
     * npcId 2 = Combat Instructor (red armour)
     * other   = Rat
     */
    public void renderNPC(int npcX, int npcY, int npcId) {
        float sx = worldToScreenX(npcX, npcY);
        float sy = worldToScreenY(npcX, npcY);

        sr.begin(ShapeRenderer.ShapeType.Filled);
        switch (npcId) {
            case 1  -> drawGuide(sx, sy);
            case 2  -> drawInstructor(sx, sy);
            default -> drawRat(sx, sy);
        }
        sr.end();
    }

    /** Tutorial Guide: green robe, brown hood. */
    private void drawGuide(float sx, float sy) {
        sr.setColor(PANTS);
        sr.rect(sx - 4, sy - 8, 3, 6);
        sr.rect(sx + 1, sy - 8, 3, 6);

        sr.setColor(0.18f, 0.56f, 0.22f, 1f); // green robe
        sr.rect(sx - 5, sy - 2, 10, 8);

        sr.setColor(SKIN);
        sr.rect(sx - 3, sy + 6, 6, 6);

        sr.setColor(0.48f, 0.30f, 0.12f, 1f); // brown hood
        sr.rect(sx - 4, sy + 9, 8, 4);
    }

    /** Combat Instructor: red plate armour, steel helmet. */
    private void drawInstructor(float sx, float sy) {
        sr.setColor(0.55f, 0.55f, 0.58f, 1f); // chainmail legs
        sr.rect(sx - 4, sy - 8, 3, 6);
        sr.rect(sx + 1, sy - 8, 3, 6);

        sr.setColor(0.72f, 0.14f, 0.12f, 1f); // red plate body
        sr.rect(sx - 5, sy - 2, 10, 8);

        sr.setColor(SKIN);
        sr.rect(sx - 2, sy + 7, 4, 4);

        sr.setColor(0.62f, 0.62f, 0.65f, 1f); // steel helmet
        sr.rect(sx - 4, sy + 8, 8, 5);

        sr.setColor(0.12f, 0.12f, 0.12f, 1f); // visor slit
        sr.rect(sx - 3, sy + 9, 6, 1);
    }

    /** Rat: small brown creature with pink ears, dark eyes. */
    private void drawRat(float sx, float sy) {
        sr.setColor(0.50f, 0.32f, 0.18f, 1f); // body
        sr.rect(sx - 4, sy - 3, 8, 5);

        sr.setColor(0.58f, 0.38f, 0.22f, 1f); // head
        sr.rect(sx - 3, sy + 2, 6, 4);

        sr.setColor(0.82f, 0.58f, 0.58f, 1f); // ears
        sr.rect(sx - 4, sy + 4, 2, 2);
        sr.rect(sx + 2, sy + 4, 2, 2);

        sr.setColor(0.05f, 0.05f, 0.05f, 1f); // eyes
        sr.rect(sx - 2, sy + 3, 1, 1);
        sr.rect(sx + 1, sy + 3, 1, 1);

        sr.setColor(0.72f, 0.55f, 0.45f, 1f); // tail
        sr.rect(sx + 4, sy - 1, 3, 1);
    }

    // -----------------------------------------------------------------------
    // Health bar
    // -----------------------------------------------------------------------

    /**
     * Thin red/green bar above an entity — call when HP < max.
     */
    public void renderHealthBar(int tileX, int tileY, int health, int maxHealth) {
        if (maxHealth <= 0) return;
        float sx = worldToScreenX(tileX, tileY);
        float sy = worldToScreenY(tileX, tileY);
        float barY  = sy + 16;
        float barW  = 20f;
        float barH  = 3f;
        float fill  = (float) health / maxHealth;

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.2f, 0.0f, 0.0f, 1f);
        sr.rect(sx - barW / 2, barY, barW, barH);
        sr.setColor(0.0f, 0.8f, 0.0f, 1f);
        sr.rect(sx - barW / 2, barY, barW * fill, barH);
        sr.end();
    }
}
