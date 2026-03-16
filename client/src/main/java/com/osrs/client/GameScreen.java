package com.osrs.client;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.osrs.client.network.NettyClient;
import com.osrs.client.renderer.CoordinateConverter;
import com.osrs.client.renderer.IsometricRenderer;
import com.osrs.client.ui.CombatUI;
import com.osrs.client.ui.ContextMenu;
import com.osrs.client.ui.DialogueUI;
import com.osrs.client.ui.InventoryUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Main game screen (LibGDX ApplicationAdapter).
 * 
 * INPUT: Right-click for context menu (OSRS-style)
 * MOVEMENT: Click "Walk here" option → send pathfind request to server
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
    private CombatUI combatUI;
    private DialogueUI dialogueUI;
    private InventoryUI inventoryUI;
    
    private int playerX = 50;
    private int playerY = 50;
    private List<Integer> walkPath = new ArrayList<>(); // Current movement path
    private int pathIndex = 0;
    private boolean initialized = false;
    
    // HUD stats display
    private int playerHealth = 10;
    private int playerMaxHealth = 10;
    private int playerAttackLevel = 1;
    
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
        
        // Initialize UI systems
        contextMenu = new ContextMenu();
        combatUI = new CombatUI();
        dialogueUI = new DialogueUI();
        inventoryUI = new InventoryUI();
        
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
        
        // Update player position (follow path if walking)
        updateMovement();
        
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
        
        // Render debug info
        renderDebugInfo();
    }
    
    private void handleInput() {
        // RIGHT-CLICK opens context menu
        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            int screenX = Gdx.input.getX();
            int screenY = Gdx.input.getY();
            
            // Convert screen coords to world tile coords
            int clickedTileX = CoordinateConverter.screenToWorldX(screenX, Gdx.graphics.getHeight() - screenY);
            int clickedTileY = CoordinateConverter.screenToWorldY(screenX, Gdx.graphics.getHeight() - screenY);
            
            LOG.debug("Right-clicked at screen ({}, {}) → world tile ({}, {})", 
                screenX, screenY, clickedTileX, clickedTileY);
            
            // Generate context menu for this tile
            List<ContextMenu.MenuItem> options = generateContextMenu(clickedTileX, clickedTileY);
            
            if (!options.isEmpty()) {
                contextMenu.open(screenX, Gdx.graphics.getHeight() - screenY, options);
            }
        }
        
        // LEFT-CLICK selects menu option (or closes menu)
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            int screenX = Gdx.input.getX();
            int screenY = Gdx.input.getY();
            
            if (contextMenu.isVisible()) {
                ContextMenu.MenuItem clicked = contextMenu.getClickedItem(screenX, Gdx.graphics.getHeight() - screenY);
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
    
    private void updateMovement() {
        // TODO: Implement smooth pathfinding movement
        // For now, just move directly to target
    }
    
    private List<ContextMenu.MenuItem> generateContextMenu(int tileX, int tileY) {
        List<ContextMenu.MenuItem> options = new ArrayList<>();
        
        // Validate tile is in bounds
        if (!CoordinateConverter.isValidTile(tileX, tileY)) {
            return options;
        }
        
        // "Walk here" is always available (placeholder: assume all tiles walkable)
        options.add(new ContextMenu.MenuItem(
            "Walk here", 
            "walk", 
            new int[]{tileX, tileY}
        ));
        
        // Check if there's an NPC at this tile
        // TODO: Query world for entities at this tile
        // For demo, hardcode NPCs with their IDs
        if (tileX == 52 && tileY == 48) {  // Tutorial Guide
            options.add(new ContextMenu.MenuItem("Talk", "talk", 1));
            options.add(new ContextMenu.MenuItem("Attack", "attack", 1));
        } else if (tileX == 55 && tileY == 45) {  // Combat Instructor
            options.add(new ContextMenu.MenuItem("Talk", "talk", 2));
            options.add(new ContextMenu.MenuItem("Attack", "attack", 2));
        }
        
        return options;
    }
    
    private void handleContextMenuAction(ContextMenu.MenuItem item) {
        LOG.info("Menu action selected: {}", item.label);
        
        if ("walk".equals(item.action)) {
            int[] target = (int[]) item.target;
            int targetX = target[0];
            int targetY = target[1];
            
            LOG.info("Walk action: player at ({}, {}) → target ({}, {})", 
                playerX, playerY, targetX, targetY);
            
            // Send walk-to request to server (which will calculate pathfinding)
            nettyClient.sendWalkTo(targetX, targetY);
            
            // For now, move directly (server will validate + send back corrected path)
            playerX = targetX;
            playerY = targetY;
            
        } else if ("talk".equals(item.action)) {
            int npcId = (Integer) item.target;
            LOG.info("Talk action triggered on NPC {}", npcId);
            // Server will initiate dialogue
            // For now, just log it
            // TODO: Show dialogue UI when server sends DialogueMessage
            
        } else if ("attack".equals(item.action)) {
            int targetId = (Integer) item.target;
            LOG.info("Attack action triggered on entity {}", targetId);
            
            // Send attack command to server
            nettyClient.sendAttack(targetId);
        }
    }
    
    private void renderContextMenu() {
        // TODO: Render context menu as text list
        // For now, just log
    }
    
    private void renderDebugInfo() {
        // TODO: Render player position + debug info at top of screen
        // LOG.debug("Player: ({}, {}), Tiles: {}", playerX, playerY, contextMenu.getItems().size());
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
