package com.osrs.client;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
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
        
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("OSRS MMORP");
        config.setWindowedMode(1024, 768);
        config.useVsync(true);
        config.setForegroundFPS(60);
        config.setIdleFPS(60);
        
        LOG.info("Creating LibGDX application");
        new Lwjgl3Application(new GameScreen(), config);
    }
}
