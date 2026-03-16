package com.osrs.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main server entry point.
 * Initializes Netty server and starts the 256-tick game loop.
 */
public class Server {
    
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);
    private static final int TICK_RATE_HZ = 256;
    private static final long TICK_INTERVAL_NS = 1_000_000_000 / TICK_RATE_HZ; // 3.9ms per tick
    
    private GameLoop gameLoop;
    
    public Server() {
        LOG.info("OSRS MMORP Server starting...");
    }
    
    public void start() throws Exception {
        LOG.info("Server tick rate: {} Hz ({}ms per tick)", TICK_RATE_HZ, TICK_INTERVAL_NS / 1_000_000.0);
        
        // TODO: Initialize Netty server (S1-003)
        // TODO: Load world map (S1-007)
        // TODO: Spawn NPCs (S1-010)
        
        // Start game loop
        gameLoop = new GameLoop(TICK_INTERVAL_NS);
        gameLoop.start();
        
        // Keep server running
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }
    
    public void shutdown() {
        LOG.info("Server shutting down...");
        if (gameLoop != null) {
            gameLoop.stop();
        }
        LOG.info("Server stopped");
    }
    
    public static void main(String[] args) {
        try {
            Server server = new Server();
            server.start();
        } catch (Exception e) {
            LOG.error("Failed to start server", e);
            System.exit(1);
        }
    }
}
