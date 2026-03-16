package com.osrs.server;

import com.osrs.server.network.NettyServer;
import com.osrs.server.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main server entry point.
 * Initializes world, Netty server, and starts the 256-tick game loop.
 */
public class Server {
    
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);
    private static final int TICK_RATE_HZ = 256;
    private static final long TICK_INTERVAL_NS = 1_000_000_000 / TICK_RATE_HZ; // 3.9ms per tick
    
    private GameLoop gameLoop;
    private NettyServer nettyServer;
    private World world;
    
    public Server() {
        LOG.info("OSRS MMORP Server starting...");
    }
    
    public void start() throws Exception {
        LOG.info("Server tick rate: {} Hz ({:.2f}ms per tick)", TICK_RATE_HZ, TICK_INTERVAL_NS / 1_000_000.0);
        
        // Initialize world
        world = new World();
        LOG.info("World loaded");
        
        // Spawn NPCs (S1-010)
        spawnNPCs();
        
        // Start Netty server (S1-003)
        nettyServer = new NettyServer();
        nettyServer.start();
        
        // Start game loop
        gameLoop = new GameLoop(TICK_INTERVAL_NS);
        gameLoop.start();
        
        // Keep server running
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }
    
    private void spawnNPCs() {
        // Spawn Tutorial Island NPCs
        world.spawnNPC(1, "Tutorial Guide", 1, 50, 50);
        world.spawnNPC(2, "Combat Instructor", 2, 55, 45);
        world.spawnNPC(3, "Rat", 3, 45, 50);
        world.spawnNPC(4, "Rat", 3, 47, 50);
        world.spawnNPC(5, "Rat", 3, 49, 50);
        LOG.info("Spawned {} NPCs", 5);
    }
    
    public void shutdown() {
        LOG.info("Server shutting down...");
        
        if (nettyServer != null) {
            try {
                nettyServer.stop();
            } catch (Exception e) {
                LOG.error("Error stopping Netty server", e);
            }
        }
        
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
