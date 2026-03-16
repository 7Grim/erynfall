package com.osrs.client;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.osrs.client.network.NettyClient;
import com.osrs.client.renderer.IsometricRenderer;
import com.osrs.client.ui.ContextMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Main game screen (LibGDX ApplicationAdapter).
 * 
 * INPUT: Right-click for context menu (OSRS-style)
 * MOVEMENT: Click "Walk here" option → pathfind to tile
 * COMBAT: Right-click NPC → select "Attack"
 */
public class GameScreen extends ApplicationAdapter {
    
    private static final Logger LOG = LoggerFactory.getLogger(GameScreen.class);
    
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private OrthographicCamera camera;
    private IsometricRenderer renderer;
    private NettyClient nettyClient;
    private ContextMenu contextMenu;
    
    private int playerX = 50;
    private int playerY = 50;
    private boolean initialized = false;
    
    @Override
    public void create() {
        LOG.info("Game screen created");
        LOG.info("Display: {} x {}", Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        
        // Initialize graphics
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0, 0, 0);
        camera.update();
        
        // Initialize renderer
        renderer = new IsometricRenderer(camera, batch, shapeRenderer);
        
        // Initialize context menu
        contextMenu = new ContextMenu();
        
        // Initialize network
        try {
            nettyClient = new NettyClient();
            nettyClient.connect();
            nettyClient.sendHandshake("TestPlayer");
            LOG.info("Connected to server");
        } catch (Exception e) {
            LOG.error("Failed to connect to server", e);
        }
        
        initialized = true;
    }
    
    @Override
    public void render() {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.15f, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        if (!initialized) {
            return;
        }
        
        // Handle input
        handleInput();
        
        // Update camera
        camera.update();
        
        // Render world
        shapeRenderer.setProjectionMatrix(camera.combined);
        renderer.renderTiles();
        renderer.renderPlayer(playerX, playerY);
        
        // TODO: Render NPCs from world state
        // Placeholder: render a few NPCs
        renderer.renderNPC(52, 48, 1);
        renderer.renderNPC(55, 45, 2);
        
        // Render context menu if visible
        if (contextMenu.isVisible()) {
            renderContextMenu();
        }
    }
    
    private void handleInput() {
        // RIGHT-CLICK opens context menu
        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            int mouseX = Gdx.input.getX();
            int mouseY = Gdx.input.getY();
            
            // Get clicked tile
            int clickedTileX = screenToWorldX(mouseX, mouseY);
            int clickedTileY = screenToWorldY(mouseX, mouseY);
            
            // Generate context menu for this tile
            List<ContextMenu.MenuItem> options = generateContextMenu(clickedTileX, clickedTileY);
            
            if (!options.isEmpty()) {
                contextMenu.open(mouseX, Gdx.graphics.getHeight() - mouseY, options);
                LOG.debug("Context menu opened at ({}, {}) for tile ({}, {})", 
                    mouseX, mouseY, clickedTileX, clickedTileY);
            }
        }
        
        // LEFT-CLICK selects menu option (or closes menu)
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            int mouseX = Gdx.input.getX();
            int mouseY = Gdx.input.getY();
            
            if (contextMenu.isVisible()) {
                ContextMenu.MenuItem clicked = contextMenu.getClickedItem(mouseX, Gdx.graphics.getHeight() - mouseY);
                if (clicked != null) {
                    handleContextMenuAction(clicked);
                }
                contextMenu.close();
            }
        }
        
        // ESC closes context menu
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            contextMenu.close();
        }
    }
    
    private int screenToWorldX(int screenX, int screenY) {
        // TODO: Implement proper screen-to-world conversion for isometric
        // For now, approximate
        return (int) ((screenX / 32.0f) + (screenY / 16.0f) / 2);
    }
    
    private int screenToWorldY(int screenX, int screenY) {
        // TODO: Implement proper screen-to-world conversion for isometric
        // For now, approximate
        return (int) ((screenY / 16.0f) - (screenX / 32.0f) / 2);
    }
    
    private List<ContextMenu.MenuItem> generateContextMenu(int tileX, int tileY) {
        List<ContextMenu.MenuItem> options = new ArrayList<>();
        
        // "Walk here" is always available
        options.add(new ContextMenu.MenuItem(
            "Walk here", 
            "walk", 
            new int[]{tileX, tileY}
        ));
        
        // Check if there's an NPC at this tile
        // TODO: Query world for entities at this tile
        // For demo, hardcode NPCs
        if ((tileX == 52 && tileY == 48) || (tileX == 55 && tileY == 45)) {
            options.add(new ContextMenu.MenuItem("Talk", "talk", null));
            options.add(new ContextMenu.MenuItem("Attack", "attack", null));
        }
        
        return options;
    }
    
    private void handleContextMenuAction(ContextMenu.MenuItem item) {
        LOG.info("Menu action selected: {}", item.label);
        
        if ("walk".equals(item.action)) {
            int[] target = (int[]) item.target;
            int targetX = target[0];
            int targetY = target[1];
            
            // TODO: Calculate path using BFS
            // For now, just move directly
            if (canWalkTo(targetX, targetY)) {
                playerX = targetX;
                playerY = targetY;
                nettyClient.sendPlayerMovement(playerX, playerY, 0);
                LOG.info("Player moved to ({}, {})", targetX, targetY);
            } else {
                LOG.warn("Cannot walk to ({}, {}): blocked or out of bounds", targetX, targetY);
            }
        } else if ("talk".equals(item.action)) {
            LOG.info("Talk action triggered");
            // TODO: Open dialogue UI
        } else if ("attack".equals(item.action)) {
            LOG.info("Attack action triggered");
            // TODO: Initiate combat
        }
    }
    
    private boolean canWalkTo(int x, int y) {
        // TODO: Query server for walkability
        // For now, allow anywhere on map
        return x >= 0 && x < 104 && y >= 0 && y < 104;
    }
    
    private void renderContextMenu() {
        // TODO: Render context menu as text list
        // For now, just log that it would render
        LOG.debug("Would render context menu with {} options", contextMenu.getItems().size());
    }
    
    @Override
    public void dispose() {
        LOG.info("Game screen disposed");
        
        if (nettyClient != null) {
            try {
                nettyClient.disconnect();
            } catch (Exception e) {
                LOG.error("Error disconnecting", e);
            }
        }
        
        if (batch != null) batch.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
    }
    
    @Override
    public void resize(int width, int height) {
        LOG.debug("Window resized to {} x {}", width, height);
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }
}
