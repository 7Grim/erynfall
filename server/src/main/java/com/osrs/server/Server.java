package com.osrs.server;

import com.osrs.server.config.ServerConfig;
import com.osrs.server.database.DatabaseManager;
import com.osrs.server.network.NettyServer;
import com.osrs.server.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main server entry point.
 * Initializes:
 * 1. Configuration (server.yml)
 * 2. Database (PostgreSQL)
 * 3. World (YAML tiles + NPCs)
 * 4. Netty server (port 43594)
 * 5. GameLoop (256 Hz tick rate)
 */
public class Server {
    
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);
    
    private GameLoop gameLoop;
    private NettyServer nettyServer;
    private World world;
    private GameContent gameContent;
    private DatabaseManager database;
    private ServerConfig config;
    
    public Server() {
        LOG.info("OSRS MMORP Server initializing...");
    }
    
    public void start() throws Exception {
        // Stage 1: Load configuration
        LOG.info("Stage 1: Loading configuration");
        config = ServerConfig.load("server/src/main/resources/server.yml");
        LOG.info("✓ Configuration loaded");
        LOG.info("  Tick rate: {} Hz ({:.2f}ms per tick)", 
            config.tickRateHz, 1_000_000_000.0 / config.tickRateHz / 1_000_000.0);
        LOG.info("  Database: {}", config.dbUrl);
        
        // Stage 2: Initialize database (optional — world state is in-memory; DB used for persistence in S6+)
        LOG.info("Stage 2: Initializing database (optional)");
        try {
            database = DatabaseManager.initialize(config);
            LOG.info("✓ Database initialized");
        } catch (Exception e) {
            LOG.warn("⚠ Database unavailable ({}). Continuing without persistence — world state is in-memory only.",
                e.getMessage().split("\\.")[0]);
        }
        
        // Stage 3: Load world
        LOG.info("Stage 3: Loading world");
        world = new World();
        LOG.info("✓ World loaded (map: {} tiles)", world.getTileMap().getTotalTiles());
        
        // Stage 4: Initialize game content
        LOG.info("Stage 4: Loading game content");
        gameContent = new GameContent();
        gameContent.initializeTutorialIsland();
        LOG.info("✓ Game content loaded");
        
        // Stage 5: Spawn NPCs
        LOG.info("Stage 5: Spawning NPCs");
        spawnNPCs();
        LOG.info("✓ NPCs spawned");
        
        // Stage 6: Start Netty server
        LOG.info("Stage 6: Starting Netty server");
        nettyServer = new NettyServer(config.port, config.bossThreads, config.workerThreads, world, gameContent);
        nettyServer.start();
        LOG.info("✓ Netty server listening on port {}", config.port);
        
        // Stage 7: Start game loop
        LOG.info("Stage 7: Starting game loop");
        long tickIntervalNs = 1_000_000_000 / config.tickRateHz;
        gameLoop = new GameLoop(tickIntervalNs, world, nettyServer);
        gameLoop.start();
        LOG.info("✓ Game loop started");
        
        // Register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        
        LOG.info("\n" +
            "╔══════════════════════════════════════════════════════════════════╗\n" +
            "║          OSRS-MMORP SERVER STARTED SUCCESSFULLY                  ║\n" +
            "║                                                                  ║\n" +
            "║  Tick Rate: {} Hz                                               ║\n" +
            "║  Port: {}                                                        ║\n" +
            "║  Database: {}                                          ║\n" +
            "║  Max Players (MVP): {}                                            ║\n" +
            "║                                                                  ║\n" +
            "║  Waiting for client connections...                              ║\n" +
            "╚══════════════════════════════════════════════════════════════════╝",
            config.tickRateHz, config.port, config.dbUrl, config.maxPlayersLocal);
    }
    
    private void spawnNPCs() {
        // NPCs are already spawned from world.yml during World initialization
        // This method is kept for compatibility but is now a no-op
        LOG.info("NPCs pre-loaded from world.yml ({} total)", world.getNPCs().size());
    }
    
    public void shutdown() {
        LOG.info("Server shutting down...");
        
        try {
            if (gameLoop != null) {
                gameLoop.stop();
                LOG.info("✓ Game loop stopped");
            }
            
            if (nettyServer != null) {
                nettyServer.stop();
                LOG.info("✓ Netty server stopped");
            }
            
            if (database != null) {
                database.shutdown();
                LOG.info("✓ Database closed");
            }
        } catch (Exception e) {
            LOG.error("Error during shutdown", e);
        }
        
        LOG.info("Server stopped");
    }
    
    public static void main(String[] args) {
        try {
            Server server = new Server();
            server.start();
        } catch (Exception e) {
            LOG.error("Failed to start server", e);
            e.printStackTrace();
            System.exit(1);
        }
    }
}
