package com.osrs.client.renderer;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Isometric tile renderer.
 * Converts world tile coordinates (x, y) to screen coordinates.
 */
public class IsometricRenderer {
    
    private static final Logger LOG = LoggerFactory.getLogger(IsometricRenderer.class);
    
    private static final int TILE_WIDTH = 32;   // Pixels
    private static final int TILE_HEIGHT = 16;  // Pixels
    private static final int MAP_WIDTH = 104;
    private static final int MAP_HEIGHT = 104;
    
    private final OrthographicCamera camera;
    private final SpriteBatch batch;
    private final ShapeRenderer shapeRenderer;
    
    public IsometricRenderer(OrthographicCamera camera, SpriteBatch batch, ShapeRenderer shapeRenderer) {
        this.camera = camera;
        this.batch = batch;
        this.shapeRenderer = shapeRenderer;
    }
    
    /**
     * Convert world tile position to screen position.
     */
    public float worldToScreenX(int tileX, int tileY) {
        return (tileX - tileY) * TILE_WIDTH / 2.0f;
    }
    
    public float worldToScreenY(int tileX, int tileY) {
        return (tileX + tileY) * TILE_HEIGHT / 2.0f;
    }
    
    /**
     * Render isometric tile grid.
     */
    public void renderTiles() {
        shapeRenderer.setColor(0.2f, 0.6f, 0.2f, 1.0f); // Green
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        
        // Draw visible tile grid
        for (int y = 0; y < MAP_HEIGHT; y++) {
            for (int x = 0; x < MAP_WIDTH; x++) {
                float screenX = worldToScreenX(x, y);
                float screenY = worldToScreenY(x, y);
                
                // Draw tile diamond outline
                float[] xPoints = {
                    screenX + TILE_WIDTH / 2,  // Right
                    screenX,                    // Center
                    screenX - TILE_WIDTH / 2,  // Left
                    screenX                     // Center
                };
                float[] yPoints = {
                    screenY,                    // Top
                    screenY + TILE_HEIGHT / 2, // Bottom
                    screenY,                    // Center
                    screenY - TILE_HEIGHT / 2  // Top
                };
                
                // Draw simplified: just corners
                shapeRenderer.line(xPoints[0], yPoints[0], xPoints[1], yPoints[1]);
                shapeRenderer.line(xPoints[1], yPoints[1], xPoints[2], yPoints[2]);
                shapeRenderer.line(xPoints[2], yPoints[2], xPoints[3], yPoints[3]);
                shapeRenderer.line(xPoints[3], yPoints[3], xPoints[0], yPoints[0]);
            }
        }
        
        shapeRenderer.end();
    }
    
    /**
     * Render player sprite (placeholder).
     */
    public void renderPlayer(int playerX, int playerY) {
        shapeRenderer.setColor(1.0f, 0.0f, 0.0f, 1.0f); // Red
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        float screenX = worldToScreenX(playerX, playerY);
        float screenY = worldToScreenY(playerX, playerY);
        
        // Draw red square at player position
        shapeRenderer.rect(screenX - 8, screenY - 8, 16, 16);
        
        shapeRenderer.end();
    }
    
    /**
     * Render NPC sprite (placeholder).
     */
    public void renderNPC(int npcX, int npcY, int npcId) {
        shapeRenderer.setColor(0.0f, 1.0f, 1.0f, 1.0f); // Cyan
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        float screenX = worldToScreenX(npcX, npcY);
        float screenY = worldToScreenY(npcX, npcY);
        
        // Draw cyan circle at NPC position
        shapeRenderer.circle(screenX, screenY, 6);
        
        shapeRenderer.end();
    }
}
