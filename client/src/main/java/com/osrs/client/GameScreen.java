package com.osrs.client;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main game screen (LibGDX ApplicationAdapter).
 * Handles rendering, input, and networking.
 */
public class GameScreen extends ApplicationAdapter {
    
    private static final Logger LOG = LoggerFactory.getLogger(GameScreen.class);
    private ShapeRenderer shapeRenderer;
    private boolean initialized = false;
    
    @Override
    public void create() {
        LOG.info("Game screen created");
        LOG.info("Display: {} x {}", Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        LOG.info("Graphics: {}", Gdx.graphics.getGLVersion().getRendererString());
        shapeRenderer = new ShapeRenderer();
        initialized = true;
        
        // TODO: Initialize network client (S1-005)
        // TODO: Initialize isometric renderer (S1-008)
        // TODO: Initialize UI (S1-005)
    }
    
    @Override
    public void render() {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        if (!initialized) {
            return;
        }
        
        // TODO: Render game world
        // 1. Render tiles
        // 2. Render entities
        // 3. Render UI
        
        // Temporary placeholder: Draw a simple rectangle
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(1, 1, 1, 1);
        shapeRenderer.rect(100, 100, 200, 200);
        shapeRenderer.end();
    }
    
    @Override
    public void dispose() {
        LOG.info("Game screen disposed");
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
        
        // TODO: Close network connection
    }
    
    @Override
    public void resize(int width, int height) {
        LOG.debug("Window resized to {} x {}", width, height);
    }
}
