package com.osrs.client;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

public record LaunchOptions(boolean artistMode, String repoRoot, String worldId) {

    private static final String ARTIST_MODE_PROPERTY = "erynfall.artistMode";
    private static final String REPO_ROOT_PROPERTY = "erynfall.repoRoot";
    private static final String WORLD_ID_PROPERTY = "erynfall.worldId";
    private static final String DEFAULT_WORLD_ID = "sandbox";

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
                        worldId = sanitizeWorldId(value);
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
        String worldId = sanitizeWorldId(System.getProperty(WORLD_ID_PROPERTY, DEFAULT_WORLD_ID));
        return new LaunchOptions(artistMode, repoRoot, worldId);
    }

    private static String sanitizeWorldId(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_WORLD_ID;
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
}
