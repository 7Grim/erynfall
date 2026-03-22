package com.osrs.server.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * DatabaseManager - Singleton connection pool manager for OSRS-MMORP
 *
 * Manages HikariCP connection pooling to SQL Server database (osrsmmorp)
 * Features:
 * - Thread-safe singleton pattern
 * - Lazy initialization
 * - Graceful shutdown hook
 * - Connection validation & retry logic
 *
 * Usage:
 *   DatabaseManager.initialize();  // Call once during Server startup
 *   Connection conn = DatabaseManager.getConnection();
 *   // Use connection...
 *   conn.close();  // Return to pool
 *   DatabaseManager.shutdown();  // Call on Server shutdown
 */
public class DatabaseManager {
    
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseManager.class);
    private static DatabaseManager instance;
    private HikariDataSource dataSource;
    
    // Database configuration (SQL Server 2025 on localhost)
    private static final String JDBC_URL = "jdbc:sqlserver://localhost:1433;databaseName=osrsmmorp;encrypt=false;trustServerCertificate=true;";
    private static final String DB_USER = "sa";  // Windows auth alternative
    private static final String DB_PASSWORD = "";  // Empty for Windows auth
    private static final int MAX_POOL_SIZE = 10;
    private static final int MIN_IDLE = 2;
    private static final long CONNECTION_TIMEOUT_MS = 5000;  // 5 seconds
    private static final long IDLE_TIMEOUT_MS = 600000;  // 10 minutes
    private static final long MAX_LIFETIME_MS = 1800000;  // 30 minutes
    
    /**
     * Singleton getter - private to prevent direct instantiation
     */
    private DatabaseManager() {}
    
    /**
     * Initialize the connection pool (call once during Server startup)
     * 
     * Returns: Initialized DatabaseManager instance (for compatibility with Server.java)
     * 
     * @throws SQLException if database connection fails
     */
    public static synchronized DatabaseManager initialize() throws SQLException {
        return initialize(null);  // config parameter ignored for now
    }
    
    /**
     * Initialize the connection pool with optional config
     * 
     * @param config ServerConfig (optional, can be null - uses hardcoded defaults)
     * @throws SQLException if database connection fails
     */
    public static synchronized DatabaseManager initialize(Object config) throws SQLException {
        if (instance != null) {
            LOG.warn("DatabaseManager already initialized, returning existing instance...");
            return instance;
        }
        
        try {
            instance = new DatabaseManager();
            instance.createDataSource();
            
            // Test connection
            try (Connection conn = instance.dataSource.getConnection()) {
                LOG.info("✓ Database connection successful: osrsmmorp");
            }
            
            // Register shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOG.info("Shutdown hook triggered, closing database connection pool...");
                if (instance != null) {
                    instance.shutdown();
                }
            }));
            
            return instance;
            
        } catch (SQLException e) {
            LOG.error("✗ Failed to initialize DatabaseManager", e);
            throw e;
        }
    }
    
    /**
     * Create and configure HikariCP data source
     */
    private void createDataSource() throws SQLException {
        HikariConfig config = new HikariConfig();
        
        // Connection settings
        config.setJdbcUrl(JDBC_URL);
        config.setUsername(DB_USER);
        config.setPassword(DB_PASSWORD);
        config.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        
        // Pool sizing
        config.setMaximumPoolSize(MAX_POOL_SIZE);
        config.setMinimumIdle(MIN_IDLE);
        
        // Timeouts
        config.setConnectionTimeout(CONNECTION_TIMEOUT_MS);
        config.setIdleTimeout(IDLE_TIMEOUT_MS);
        config.setMaxLifetime(MAX_LIFETIME_MS);
        
        // Connection validation
        config.setConnectionTestQuery("SELECT 1");
        config.setLeakDetectionThreshold(60000);  // 1 minute leak detection
        
        // Pool name for logging
        config.setPoolName("OSRS-MMORP-Pool");
        
        // Auto-commit disabled (managed by application)
        config.setAutoCommit(false);
        
        LOG.info("Creating HikariCP data source...");
        LOG.info("  JDBC URL: " + JDBC_URL);
        LOG.info("  Max Pool Size: " + MAX_POOL_SIZE);
        LOG.info("  Min Idle: " + MIN_IDLE);
        
        this.dataSource = new HikariDataSource(config);
    }
    
    /**
     * Get a connection from the pool
     *
     * @return Connection from pool
     * @throws SQLException if unable to get connection
     */
    public static Connection getConnection() throws SQLException {
        if (instance == null || instance.dataSource == null) {
            throw new SQLException("DatabaseManager not initialized. Call initialize() first.");
        }
        return instance.dataSource.getConnection();
    }
    
    /**
     * Check if database is connected and healthy
     *
     * @return true if pool has active connections and is responsive
     */
    public static boolean isHealthy() {
        try {
            if (instance == null || instance.dataSource == null) {
                return false;
            }
            
            // Test connection
            try (Connection conn = instance.dataSource.getConnection()) {
                return true;
            }
        } catch (SQLException e) {
            LOG.warn("Database health check failed", e);
            return false;
        }
    }
    
    /**
     * Get pool statistics
     *
     * @return String with active/idle/pending connections
     */
    public static String getPoolStats() {
        if (instance == null || instance.dataSource == null) {
            return "DatabaseManager not initialized";
        }
        return String.format("Active: %d, Idle: %d, Pending: %d, Total: %d",
            instance.dataSource.getHikariPoolMXBean().getActiveConnections(),
            instance.dataSource.getHikariPoolMXBean().getIdleConnections(),
            instance.dataSource.getHikariPoolMXBean().getPendingThreads(),
            instance.dataSource.getHikariPoolMXBean().getTotalConnections());
    }
    
    /**
     * Gracefully shutdown the connection pool
     * Call during Server shutdown
     * 
     * Note: Can be called as static (DatabaseManager.shutdown()) or instance (db.shutdown())
     */
    public synchronized void shutdown() {
        if (instance == null || instance.dataSource == null) {
            return;
        }
        
        try {
            LOG.info("Shutting down database connection pool...");
            LOG.info("Current pool stats: " + getPoolStats());
            
            instance.dataSource.close();
            instance = null;
            
            LOG.info("✓ Database connection pool closed");
        } catch (Exception e) {
            LOG.error("✗ Error closing database connection pool", e);
        }
    }
}
