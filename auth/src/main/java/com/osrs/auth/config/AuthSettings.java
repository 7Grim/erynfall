package com.osrs.auth.config;

import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AuthSettings {

    private final String dbUrl = env("DB_URL");
    private final String dbUser = env("DB_USER");
    private final String dbPassword = env("DB_PASSWORD");
    private final String jwtIssuer = defaultIfBlank(env("JWT_ISSUER"), "http://localhost:8080");
    private final String jwtAudience = defaultIfBlank(env("JWT_AUDIENCE"), "erynfall-game");
    private final String jwtSigningKey = env("JWT_SIGNING_KEY");
    private final boolean requireDatabase = bool(env("AUTH_REQUIRE_DATABASE"), true);
    private final boolean bypassEmailVerification = bool(env("AUTH_BYPASS_EMAIL_VERIFICATION"), false);
    private final Duration accessTokenTtl = Duration.ofMinutes(parseLong(env("AUTH_ACCESS_TTL_MINUTES"), 15));
    private final Duration refreshTokenTtl = Duration.ofDays(parseLong(env("AUTH_REFRESH_TTL_DAYS"), 30));

    public String dbUrl() { return dbUrl; }
    public String dbUser() { return dbUser; }
    public String dbPassword() { return dbPassword; }
    public String jwtIssuer() { return jwtIssuer; }
    public String jwtAudience() { return jwtAudience; }
    public String jwtSigningKey() { return jwtSigningKey; }
    public boolean requireDatabase() { return requireDatabase; }
    public boolean bypassEmailVerification() { return bypassEmailVerification; }
    public Duration accessTokenTtl() { return accessTokenTtl; }
    public Duration refreshTokenTtl() { return refreshTokenTtl; }

    private static String env(String key) {
        String v = System.getenv(key);
        return v == null ? "" : v.trim();
    }

    private static boolean bool(String value, boolean defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    private static long parseLong(String value, long defaultValue) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
