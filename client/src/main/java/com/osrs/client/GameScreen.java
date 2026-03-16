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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main game screen (LibGDX ApplicationAdapter).
 * Handles rendering, input, and networking.
 */
public class GameScreen extends ApplicationAdapter {
    
    private static final Logger LOG = LoggerFactory.getLogger(GameScreen.class);
    
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private OrthographicCamera camera;
    private IsometricRenderer renderer;
    private NettyClient nettyClient;
    
    private int playerX = 50;
    private int playerY = 50;
    private boolean initialized = false;
    
    @Override
    public void create() {
        LOG.info("Game screen created");
        LOG.info("Display: {} x {}", Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        LOG.info("Graphics: {}", Gdx.graphics.getGLVersion().getRendererString());
        
        // Initialize graphics
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0, 0, 0);
        camera.update();
        
        // Initialize renderer
        renderer = new IsometricRenderer(camera, batch, shapeRenderer);
        
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
    }
    
    private void handleInput() {
        // Arrow keys to move player
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            playerY++;
            nettyClient.sendPlayerMovement(playerX, playerY, 0);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            playerY--;
            nettyClient.sendPlayerMovement(playerX, playerY, 4);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            playerX--;
            nettyClient.sendPlayerMovement(playerX, playerY, 6);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            playerX++;
            nettyClient.sendPlayerMovement(playerX, playerY, 2);
        }
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
