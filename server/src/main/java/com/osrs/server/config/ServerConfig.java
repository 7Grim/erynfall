package com.osrs.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Server configuration loader.
 * Loads from resources/server.yml
 */
public class ServerConfig {
    
    private static final Logger LOG = LoggerFactory.getLogger(ServerConfig.class);
    private static ServerConfig instance;
    
    // Server config
    public int port = 43594;
    public int tickRateHz = 256;
    public int maxPlayersLocal = 1;
    public int maxPlayersFuture = 1000;
    
    // Database config
    public String dbUrl = "jdbc:postgresql://localhost:5432/osrs_mmorp";
    public String dbUser = "postgres";
    public String dbPassword = "password";
    public int maxConnections = 10;
    
    // Network config
    public int bossThreads = 1;
    public int workerThreads = 4;
    public int readTimeoutMs = 30000;
    public int writeTimeoutMs = 30000;
    
    // Logging config
    public String logLevel = "INFO";
    public String logFile = "logs/server.log";
    public int maxFileSizeMb = 100;
    
    // Game config
    public int combatRange = 2;
    public int npcRespawnTicks = 600;
    public int itemDespawnHours = 1;
    public int idleDisconnectMinutes = 30;
    
    /**
     * Load configuration from YAML file
     */
    public static ServerConfig load(String path) throws IOException {
        LOG.info("Loading server configuration from classpath: server.yml");

        try {
            InputStream is = ServerConfig.class.getClassLoader().getResourceAsStream("server.yml");
            if (is == null) {
                throw new IOException("server.yml not found on classpath");
            }
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            Map<String, Object> yaml = mapper.readValue(is, Map.class);
            
            ServerConfig config = new ServerConfig();
            
            // Load server section
            if (yaml.containsKey("server")) {
                Map<String, Object> server = (Map<String, Object>) yaml.get("server");
                config.port = getInt(server, "port", 43594);
                config.tickRateHz = getInt(server, "tick_rate_hz", 256);
                config.maxPlayersLocal = getInt(server, "max_players_local", 1);
                config.maxPlayersFuture = getInt(server, "max_players_future", 1000);
            }
            
            // Load database section
            if (yaml.containsKey("database")) {
                Map<String, Object> db = (Map<String, Object>) yaml.get("database");
                config.dbUrl = getString(db, "url", "jdbc:postgresql://localhost:5432/osrs_mmorp");
                config.dbUser = getString(db, "user", "postgres");
                config.dbPassword = getString(db, "password", "password");
                config.maxConnections = getInt(db, "max_connections", 10);
            }
            
            // Load network section
            if (yaml.containsKey("network")) {
                Map<String, Object> network = (Map<String, Object>) yaml.get("network");
                config.bossThreads = getInt(network, "boss_threads", 1);
                config.workerThreads = getInt(network, "worker_threads", 4);
                config.readTimeoutMs = getInt(network, "read_timeout_ms", 30000);
                config.writeTimeoutMs = getInt(network, "write_timeout_ms", 30000);
            }
            
            // Load logging section
            if (yaml.containsKey("logging")) {
                Map<String, Object> logging = (Map<String, Object>) yaml.get("logging");
                config.logLevel = getString(logging, "level", "INFO");
                config.logFile = getString(logging, "file", "logs/server.log");
                config.maxFileSizeMb = getInt(logging, "max_file_size_mb", 100);
            }
            
            // Load game section
            if (yaml.containsKey("game")) {
                Map<String, Object> game = (Map<String, Object>) yaml.get("game");
                config.combatRange = getInt(game, "combat_range", 2);
                config.npcRespawnTicks = getInt(game, "npc_respawn_ticks", 600);
                config.itemDespawnHours = getInt(game, "item_despawn_hours", 1);
                config.idleDisconnectMinutes = getInt(game, "idle_disconnect_minutes", 30);
            }
            
            instance = config;
            LOG.info("Configuration loaded successfully");
            return config;
        } catch (IOException e) {
            LOG.error("Failed to load server configuration", e);
            throw e;
        }
    }
    
    /**
     * Get singleton instance
     */
    public static ServerConfig get() {
        if (instance == null) {
            throw new RuntimeException("Configuration not loaded. Call load() first.");
        }
        return instance;
    }
    
    // Helper methods
    private static int getInt(Map<String, Object> map, String key, int defaultValue) {
        if (!map.containsKey(key)) return defaultValue;
        Object val = map.get(key);
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof Number) return ((Number) val).intValue();
        return defaultValue;
    }
    
    private static String getString(Map<String, Object> map, String key, String defaultValue) {
        if (!map.containsKey(key)) return defaultValue;
        Object val = map.get(key);
        return val != null ? val.toString() : defaultValue;
    }
}
