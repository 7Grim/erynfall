package com.osrs.client.renderer;

/**
 * Converts between world tile coordinates and isometric screen coordinates.
 */
public class CoordinateConverter {
    
    private static final int TILE_WIDTH = 32;   // Pixels
    private static final int TILE_HEIGHT = 16;  // Pixels
    
    /**
     * Convert world tile position to screen position.
     * Used for rendering.
     */
    public static float worldToScreenX(int tileX, int tileY) {
        return (tileX - tileY) * TILE_WIDTH / 2.0f;
    }
    
    public static float worldToScreenY(int tileX, int tileY) {
        return (tileX + tileY) * TILE_HEIGHT / 2.0f;
    }
    
    /**
     * Convert screen position to world tile position.
     * Used for input (mouse clicks).
     * 
     * Inverts the isometric projection:
     * screenX = (tileX - tileY) * 16
     * screenY = (tileX + tileY) * 8
     * 
     * Solving for tileX, tileY:
     * tileX = (screenX/16 + screenY/8) / 2
     * tileY = (screenY/8 - screenX/16) / 2
     */
    public static int screenToWorldX(float screenX, float screenY) {
        float x = (screenX / (TILE_WIDTH / 2.0f) + screenY / (TILE_HEIGHT / 2.0f)) / 2.0f;
        return Math.round(x);
    }

    public static int screenToWorldY(float screenX, float screenY) {
        float y = (screenY / (TILE_HEIGHT / 2.0f) - screenX / (TILE_WIDTH / 2.0f)) / 2.0f;
        return Math.round(y);
    }
    
    /**
     * Check if world coordinates are valid (within map bounds).
     */
    public static boolean isValidTile(int x, int y) {
        return x >= 0 && x < 104 && y >= 0 && y < 104;
    }
}
