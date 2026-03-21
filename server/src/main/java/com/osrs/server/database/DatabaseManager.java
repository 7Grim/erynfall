package com.osrs.server.database;

import com.osrs.server.config.ServerConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Statement;

/**
 * Database manager for connection pooling and migrations.
 * Handles PostgreSQL connection pool and schema initialization.
 */
public class DatabaseManager {
    
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseManager.class);
    
    private static DatabaseManager instance;
    private HikariDataSource dataSource;
    
    /**
     * Initialize database connection pool and run migrations
     */
    public static DatabaseManager initialize(ServerConfig config) throws Exception {
        LOG.info("Initializing database: {}", config.dbUrl);
        
        instance = new DatabaseManager();
        instance.initializeConnectionPool(config);
        instance.runMigrations();
        
        LOG.info("Database initialization complete");
        return instance;
    }
    
    /**
     * Get singleton instance
     */
    public static DatabaseManager get() {
        if (instance == null) {
            throw new RuntimeException("DatabaseManager not initialized. Call initialize() first.");
        }
        return instance;
    }
    
    /**
     * Initialize HikariCP connection pool
     */
    private void initializeConnectionPool(ServerConfig config) throws Exception {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.dbUrl);
        hikariConfig.setUsername(config.dbUser);
        hikariConfig.setPassword(config.dbPassword);
        hikariConfig.setMaximumPoolSize(config.maxConnections);
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setConnectionTimeout(10000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);
        hikariConfig.setPoolName("OSRS-Pool");
        
        dataSource = new HikariDataSource(hikariConfig);
        LOG.info("Connection pool created (max: {})", config.maxConnections);
    }
    
    /**
     * Run database migrations
     */
    private void runMigrations() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                // Create schema
                LOG.info("Creating schema osrs");
                stmt.execute("CREATE SCHEMA IF NOT EXISTS osrs");
                
                // Players table
                LOG.info("Creating table osrs.players");
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS osrs.players (
                        id SERIAL PRIMARY KEY,
                        username VARCHAR(12) UNIQUE NOT NULL,
                        password_hash VARCHAR(255) NOT NULL,
                        created_at TIMESTAMP DEFAULT NOW(),
                        last_login TIMESTAMP,
                        x INT DEFAULT 3222,
                        y INT DEFAULT 3218,
                        attack_xp BIGINT DEFAULT 0,
                        strength_xp BIGINT DEFAULT 0,
                        defence_xp BIGINT DEFAULT 0,
                        magic_xp BIGINT DEFAULT 0,
                        prayer_xp BIGINT DEFAULT 0,
                        prayer_points INT DEFAULT 10,
                        woodcutting_xp BIGINT DEFAULT 0,
                        fishing_xp BIGINT DEFAULT 0,
                        cooking_xp BIGINT DEFAULT 0,
                        total_gold BIGINT DEFAULT 0,
                        total_questpoints INT DEFAULT 0,
                        INDEX (username),
                        INDEX (created_at)
                    )
                    """);
                
                // Inventory table
                LOG.info("Creating table osrs.inventory");
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS osrs.inventory (
                        id SERIAL PRIMARY KEY,
                        player_id INT NOT NULL REFERENCES osrs.players(id) ON DELETE CASCADE,
                        slot_index INT NOT NULL CHECK (slot_index >= 0 AND slot_index < 28),
                        item_id INT NOT NULL,
                        quantity INT DEFAULT 1,
                        UNIQUE (player_id, slot_index),
                        INDEX (player_id)
                    )
                    """);
                
                // Quest progress table
                LOG.info("Creating table osrs.player_quests");
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS osrs.player_quests (
                        id SERIAL PRIMARY KEY,
                        player_id INT NOT NULL REFERENCES osrs.players(id) ON DELETE CASCADE,
                        quest_id INT NOT NULL,
                        status INT DEFAULT 0,
                        completed_objectives INT DEFAULT 0,
                        started_at TIMESTAMP DEFAULT NOW(),
                        completed_at TIMESTAMP,
                        UNIQUE (player_id, quest_id),
                        INDEX (player_id, quest_id)
                    )
                    """);
                
                // Grand Exchange orders table
                LOG.info("Creating table osrs.ge_orders");
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS osrs.ge_orders (
                        id SERIAL PRIMARY KEY,
                        player_id INT NOT NULL REFERENCES osrs.players(id) ON DELETE CASCADE,
                        item_id INT NOT NULL,
                        quantity INT NOT NULL,
                        price_per_unit INT NOT NULL,
                        is_buy BOOLEAN NOT NULL,
                        filled_quantity INT DEFAULT 0,
                        created_at TIMESTAMP DEFAULT NOW(),
                        completed_at TIMESTAMP,
                        INDEX (item_id, is_buy, price_per_unit),
                        INDEX (player_id, completed_at),
                        INDEX (created_at)
                    )
                    """);
                
                // Achievements table
                LOG.info("Creating table osrs.player_achievements");
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS osrs.player_achievements (
                        id SERIAL PRIMARY KEY,
                        player_id INT NOT NULL REFERENCES osrs.players(id) ON DELETE CASCADE,
                        achievement_id INT NOT NULL,
                        unlocked_at TIMESTAMP DEFAULT NOW(),
                        progress INT DEFAULT 0,
                        UNIQUE (player_id, achievement_id),
                        INDEX (player_id)
                    )
                    """);
                
                // Chat messages table (audit log)
                LOG.info("Creating table osrs.chat_messages");
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS osrs.chat_messages (
                        id SERIAL PRIMARY KEY,
                        sender_id INT REFERENCES osrs.players(id) ON DELETE SET NULL,
                        sender_name VARCHAR(12),
                        message_text VARCHAR(255) NOT NULL,
                        chat_type INT DEFAULT 0,
                        recipient_id INT REFERENCES osrs.players(id) ON DELETE SET NULL,
                        created_at TIMESTAMP DEFAULT NOW(),
                        INDEX (sender_id, created_at),
                        INDEX (recipient_id, created_at),
                        INDEX (created_at)
                    )
                    """);
                
                // Hiscores cache table (denormalized for fast lookups)
                LOG.info("Creating table osrs.hiscores_cache");
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS osrs.hiscores_cache (
                        player_id INT PRIMARY KEY REFERENCES osrs.players(id) ON DELETE CASCADE,
                        overall_rank INT,
                        overall_level INT,
                        attack_level INT,
                        strength_level INT,
                        defence_level INT,
                        magic_level INT,
                        prayer_level INT,
                        woodcutting_level INT,
                        fishing_level INT,
                        cooking_level INT,
                        updated_at TIMESTAMP DEFAULT NOW(),
                        INDEX (overall_rank),
                        INDEX (attack_level),
                        INDEX (updated_at)
                    )
                    """);
                
                LOG.info("All migrations completed successfully");
            }
        }
    }
    
    /**
     * Get a database connection from the pool
     */
    public Connection getConnection() throws Exception {
        return dataSource.getConnection();
    }
    
    /**
     * Close the connection pool
     */
    public void shutdown() {
        if (dataSource != null) {
            dataSource.close();
            LOG.info("Database connection pool closed");
        }
    }
}
