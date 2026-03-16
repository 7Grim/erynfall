package com.osrs.client;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import org.lwjgl.system.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main client entry point.
 * Initializes LibGDX and launches the game.
 */
public class Client {
    
    private static final Logger LOG = LoggerFactory.getLogger(Client.class);
    
    public static void main(String[] args) {
        LOG.info("OSRS MMORP Client starting...");
        LOG.info("OS: {}", System.getProperty("os.name"));
        LOG.info("Arch: {}", System.getProperty("os.arch"));
        
        // Disable GLFW thread check (allows running from Maven on non-macOS)
        Configuration.GLFW_CHECK_THREAD0.set(false);
        LOG.info("GLFW thread check disabled");
        
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("OSRS MMORP");
        config.setWindowedMode(1024, 768);
        config.useVsync(true);
        config.setForegroundFPS(60);
        config.setIdleFPS(60);
        
        LOG.info("Creating LibGDX application...");
        try {
            new Lwjgl3Application(new GameScreen(), config);
            LOG.info("LibGDX application created successfully");
        } catch (Exception e) {
            LOG.error("Failed to create LibGDX application", e);
            throw e;
        }
    }
}
