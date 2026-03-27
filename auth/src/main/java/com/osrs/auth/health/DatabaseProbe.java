package com.osrs.auth.health;

import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.Duration;

@Component
public class DatabaseProbe {

    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final boolean requireDatabase;
    private final int probeAttempts;
    private final long retryDelayMs;

    public DatabaseProbe() {
        this.dbUrl = env("DB_URL");
        this.dbUser = env("DB_USER");
        this.dbPassword = env("DB_PASSWORD");
        this.requireDatabase = "true".equalsIgnoreCase(env("AUTH_REQUIRE_DATABASE"));
        this.probeAttempts = parseInt(env("DB_PROBE_ATTEMPTS"), 3, 1, 10);
        this.retryDelayMs = parseLong(env("DB_PROBE_RETRY_DELAY_MS"), 1000L, 0L, 30000L);
    }

    public Result check() {
        if (dbUrl == null || dbUrl.isBlank()) {
            return new Result(false, requireDatabase, "DB_URL not configured");
        }

        String lastError = "unknown";
        for (int attempt = 1; attempt <= probeAttempts; attempt++) {
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
                try (PreparedStatement ps = conn.prepareStatement("SELECT 1")) {
                    ps.executeQuery();
                }
                return new Result(true, requireDatabase, "ok");
            } catch (Exception e) {
                lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
                if (attempt < probeAttempts && retryDelayMs > 0) {
                    sleepQuietly(retryDelayMs);
                }
            }
        }

        return new Result(
            false,
            requireDatabase,
            "failed after " + probeAttempts + " attempt(s) over "
                + Duration.ofMillis(retryDelayMs * Math.max(0, probeAttempts - 1))
                + ": " + lastError
        );
    }

    private static String env(String key) {
        String val = System.getenv(key);
        return val == null ? "" : val;
    }

    private static int parseInt(String value, int defaultValue, int min, int max) {
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(value)));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static long parseLong(String value, long defaultValue, long min, long max) {
        try {
            return Math.max(min, Math.min(max, Long.parseLong(value)));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    public record Result(boolean up, boolean requireDatabase, String message) {
    }
}
