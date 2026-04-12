package com.osrs.client;

import java.nio.file.Path;
import java.nio.file.Paths;

public record LaunchOptions(boolean artistMode, String repoRoot) {

    public static LaunchOptions normal() {
        return new LaunchOptions(false, Paths.get("").toAbsolutePath().normalize().toString());
    }

    public static LaunchOptions fromArgs(String[] args) {
        boolean artistMode = false;
        String repoRoot = Paths.get("").toAbsolutePath().normalize().toString();

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
                }
            }
        }

        return new LaunchOptions(artistMode, repoRoot);
    }

    public Path repoRootPath() {
        return Paths.get(repoRoot).toAbsolutePath().normalize();
    }
}
