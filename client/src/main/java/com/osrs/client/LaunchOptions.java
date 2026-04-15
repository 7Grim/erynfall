package com.osrs.client;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public record LaunchOptions(boolean artistMode, String repoRoot, String worldId) {

    public record WorldOption(String worldId, String displayLabel) {}

    private static final String ARTIST_MODE_PROPERTY = "erynfall.artistMode";
    private static final String REPO_ROOT_PROPERTY = "erynfall.repoRoot";
    private static final String WORLD_ID_PROPERTY = "erynfall.worldId";
    private static final String GAME_SERVER_HOST_PROPERTY = "GAME_SERVER_HOST";
    private static final String DEFAULT_WORLD_ID = "sandbox";
    private static final String DEFAULT_LOGIN_WORLD_ID = "main_world";
    private static final Set<String> SUPPORTED_WORLD_IDS = Set.of(
        "sandbox",
        "main_world"
    );
    private static final List<WorldOption> SELECTABLE_WORLDS = List.of(
        new WorldOption("sandbox", "Sandbox"),
        new WorldOption("main_world", "Erynfall_001")
    );

    public static LaunchOptions normal() {
        return new LaunchOptions(false, detectDefaultRepoRoot().toString(), DEFAULT_WORLD_ID);
    }

    public static LaunchOptions fromArgs(String[] args) {
        LaunchOptions defaults = fromSystemProperties();
        boolean artistMode = defaults.artistMode();
        String repoRoot = defaults.repoRoot();
        String worldId = defaults.worldId();

        if (args != null) {
            for (String arg : args) {
                if (arg == null || arg.isBlank()) {
                    continue;
                }
                if ("--artist".equals(arg)) {
                    artistMode = true;
                } else if (arg.startsWith("--repo-root=")) {
                    String value = arg.substring("--repo-root=".length()).trim();
                    if (!value.isEmpty()) {
                        repoRoot = Paths.get(value).toAbsolutePath().normalize().toString();
                    }
                } else if (arg.startsWith("--world-id=")) {
                    String value = arg.substring("--world-id=".length()).trim();
                    if (!value.isEmpty()) {
                        worldId = normalizeWorldId(value, defaults.worldId());
                    }
                }
            }
        }

        return new LaunchOptions(artistMode, repoRoot, worldId);
    }

    private static LaunchOptions fromSystemProperties() {
        boolean artistMode = Boolean.parseBoolean(System.getProperty(ARTIST_MODE_PROPERTY, "false"));
        String defaultRepoRoot = detectDefaultRepoRoot().toString();
        String configuredRepoRoot = System.getProperty(REPO_ROOT_PROPERTY, "").trim();
        String repoRoot = configuredRepoRoot.isEmpty()
            ? defaultRepoRoot
            : Paths.get(configuredRepoRoot).toAbsolutePath().normalize().toString();
        String worldId = normalizeWorldId(System.getProperty(WORLD_ID_PROPERTY, DEFAULT_WORLD_ID), DEFAULT_WORLD_ID);
        return new LaunchOptions(artistMode, repoRoot, worldId);
    }

    public static List<WorldOption> supportedWorlds() {
        if (isRemoteServerTarget()) {
            return List.of(new WorldOption("main_world", "Erynfall_001"));
        }
        return SELECTABLE_WORLDS;
    }

    public static String defaultWorldId() {
        return DEFAULT_WORLD_ID;
    }

    public static String defaultLoginWorldId() {
        return isRemoteServerTarget() ? "main_world" : DEFAULT_LOGIN_WORLD_ID;
    }

    public static String normalizeWorldId(String raw, String fallback) {
        String sanitized = sanitizeWorldId(raw);
        if (sanitized.isEmpty()) {
            return normalizeFallback(fallback);
        }
        if (isSupportedWorldId(sanitized)) {
            return sanitized;
        }
        return normalizeFallback(fallback);
    }

    private static String normalizeFallback(String fallback) {
        String fallbackSanitized = sanitizeWorldId(fallback);
        if (fallbackSanitized.isEmpty() || !isSupportedWorldId(fallbackSanitized)) {
            return DEFAULT_WORLD_ID;
        }
        return fallbackSanitized;
    }

    public static boolean isSupportedWorldId(String worldId) {
        String candidate = sanitizeWorldId(worldId);
        if (candidate.isEmpty()) {
            return false;
        }
        return SUPPORTED_WORLD_IDS.contains(candidate);
    }

    public static String resolveConfiguredGameServerHost() {
        String host = System.getProperty(GAME_SERVER_HOST_PROPERTY);
        if (host != null && !host.isBlank()) {
            return host.trim();
        }

        host = System.getenv(GAME_SERVER_HOST_PROPERTY);
        if (host != null && !host.isBlank()) {
            return host.trim();
        }

        try (InputStream is = LaunchOptions.class.getResourceAsStream("/auth.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                String fromFile = props.getProperty("game.server.host");
                if (fromFile != null && !fromFile.isBlank()) {
                    return fromFile.trim();
                }
            }
        } catch (Exception ignored) {
        }

        return "localhost";
    }

    public static boolean isRemoteServerTarget() {
        String host = resolveConfiguredGameServerHost().trim().toLowerCase();
        return !(host.isEmpty()
            || "localhost".equals(host)
            || "127.0.0.1".equals(host)
            || "::1".equals(host));
    }

    private static String sanitizeWorldId(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw.trim().toLowerCase();
    }

    private static Path detectDefaultRepoRoot() {
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        Path cursor = cwd;
        for (int i = 0; i < 6 && cursor != null; i++) {
            if (Files.exists(cursor.resolve("art/models/manifest.yaml"))) {
                return cursor;
            }
            cursor = cursor.getParent();
        }
        return cwd;
    }

    public Path repoRootPath() {
        return Paths.get(repoRoot).toAbsolutePath().normalize();
    }

    public LaunchOptions withWorldId(String selectedWorldId) {
        return new LaunchOptions(artistMode, repoRoot, normalizeWorldId(selectedWorldId, worldId));
    }
}
