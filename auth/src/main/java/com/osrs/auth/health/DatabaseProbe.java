package com.osrs.auth.health;

import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

@Component
public class DatabaseProbe {

    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final boolean requireDatabase;

    public DatabaseProbe() {
        this.dbUrl = env("DB_URL");
        this.dbUser = env("DB_USER");
        this.dbPassword = env("DB_PASSWORD");
        this.requireDatabase = "true".equalsIgnoreCase(env("AUTH_REQUIRE_DATABASE"));
    }

    public Result check() {
        if (dbUrl == null || dbUrl.isBlank()) {
            return new Result(false, requireDatabase, "DB_URL not configured");
        }

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT 1")) {
                ps.executeQuery();
            }
            return new Result(true, requireDatabase, "ok");
        } catch (Exception e) {
            return new Result(false, requireDatabase, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static String env(String key) {
        String val = System.getenv(key);
        return val == null ? "" : val;
    }

    public record Result(boolean up, boolean requireDatabase, String message) {
    }
}
