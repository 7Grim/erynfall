package com.osrs.server.auth;

public class AuthTokenSettings {

    private final String jwtSigningKey = env("JWT_SIGNING_KEY");
    private final String jwtIssuer = defaultIfBlank(env("JWT_ISSUER"), "http://localhost:8080");
    private final String jwtAudience = defaultIfBlank(env("JWT_AUDIENCE"), "erynfall-game");
    private final String authDbSchema = sanitizeSchema(defaultIfBlank(env("AUTH_DB_SCHEMA"), "erynfall"));
    private final boolean allowLegacyLogin = bool(env("AUTH_ALLOW_LEGACY_LOGIN"), false);

    public String jwtSigningKey() {
        return jwtSigningKey;
    }

    public String jwtIssuer() {
        return jwtIssuer;
    }

    public String jwtAudience() {
        return jwtAudience;
    }

    public String authDbSchema() {
        return authDbSchema;
    }

    public boolean allowLegacyLogin() {
        return allowLegacyLogin;
    }

    private static String env(String key) {
        String value = System.getenv(key);
        return value == null ? "" : value.trim();
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static boolean bool(String value, boolean defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    private static String sanitizeSchema(String schema) {
        if (schema == null || schema.isBlank()) {
            return "erynfall";
        }
        if (!schema.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Invalid AUTH_DB_SCHEMA value");
        }
        return schema;
    }
}
