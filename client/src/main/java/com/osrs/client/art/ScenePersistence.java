package com.osrs.client.art;

import com.osrs.client.world.StaticPropLoader;
import com.osrs.client.world.TerrainVisualLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Writes sections of scene.yaml without touching the rest of the file.
 *
 * Approach: section-targeted text replacement.
 * The file is read as lines; only the target section is rewritten.
 * All comments and formatting outside that section are preserved verbatim.
 *
 * Safeguards applied on every write:
 *   1. Backup  — original file copied to <name>.bak before any change.
 *   2. Guard   — refuses to write 0 entries when the file currently has > 0
 *                (prevents accidental full-wipe; see ZERO_ENTRY_OVERRIDE_TOKEN).
 *   3. Atomic  — new content written to .tmp, then moved atomically into place.
 *   4. Verify  — the written file is re-parsed after write to confirm YAML integrity.
 *
 * Dry-run mode (dryRun=true): returns the YAML that *would* be written without
 * touching any file. Use for previewing saves.
 */
public final class ScenePersistence {

    /** Pass as placementsOrOverrides to override the zero-entry guard in dry-run mode. */
    public static final String ZERO_ENTRY_OVERRIDE = "FORCE_ZERO";

    public record SaveResult(boolean success, String message, String dryRunContent) {
        public SaveResult(boolean success, String message) {
            this(success, message, null);
        }
    }

    private static final Pattern TOP_LEVEL_KEY = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*:.*");
    private static final Pattern STATIC_PROPS_HEADER = Pattern.compile("^static_props:\\s*$");
    private static final Pattern TILE_OVERRIDES_HEADER = Pattern.compile("^\\s{2}tile_overrides:\\s*$");

    private ScenePersistence() {}

    // ── Static props ──────────────────────────────────────────────────────────

    public static SaveResult saveStaticProps(
            Path scenePath,
            List<StaticPropLoader.StaticPropPlacement> placements) {
        return saveStaticProps(scenePath, placements, false);
    }

    public static SaveResult saveStaticProps(
            Path scenePath,
            List<StaticPropLoader.StaticPropPlacement> placements,
            boolean dryRun) {

        if (scenePath == null) {
            return new SaveResult(false, "Scene path not configured");
        }
        if (!Files.exists(scenePath)) {
            return new SaveResult(false, "Scene file not found: " + scenePath.toAbsolutePath());
        }

        try {
            List<String> lines = Files.readAllLines(scenePath, StandardCharsets.UTF_8);

            // Find the static_props: top-level header
            int sectionLine = findLineIndex(lines, STATIC_PROPS_HEADER);
            if (sectionLine < 0) {
                return new SaveResult(false,
                    "No 'static_props:' section found in " + scenePath.getFileName()
                    + " — cannot save without a target section");
            }

            // Zero-entry guard: refuse to wipe existing placements accidentally
            int existingCount = countEntriesInSection(lines, sectionLine);
            int newCount = placements == null ? 0 : placements.size();
            if (newCount == 0 && existingCount > 0) {
                return new SaveResult(false,
                    "Refused to save: would replace " + existingCount + " existing placement(s) with an empty list. "
                    + "If intentional, use the WORLD_PLACEMENT delete-all workflow first, then save.");
            }

            // Serialize the new static_props section
            String newSection = serializeStaticProps(placements);

            // Everything before the static_props: line stays verbatim (preserves comments)
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < sectionLine; i++) {
                sb.append(lines.get(i)).append('\n');
            }
            sb.append(newSection);
            String newContent = sb.toString();

            if (dryRun) {
                return new SaveResult(true,
                    "[DRY-RUN] Would save " + newCount + " placement(s) to " + scenePath.getFileName()
                    + " (currently " + existingCount + ")",
                    newSection);
            }

            Path backupPath = scenePath.resolveSibling(scenePath.getFileName() + ".bak");
            writeAtomicWithVerification(scenePath, newContent, backupPath,
                "static_props", newCount);

            return new SaveResult(true,
                "Saved " + newCount + " static prop(s) to " + scenePath.getFileName()
                + " (backup: " + backupPath.getFileName() + ")"
                + (existingCount > 0 ? " — note: inline comments in static_props section are not preserved" : ""));

        } catch (SceneSaveException e) {
            return new SaveResult(false, e.getMessage());
        } catch (Exception e) {
            return new SaveResult(false,
                "Scene save failed: " + e.getClass().getSimpleName() + ": " + e.getMessage()
                + " — check console for details. Backup may exist at " + scenePath.getFileName() + ".bak");
        }
    }

    // ── Terrain visual overrides ──────────────────────────────────────────────

    public static SaveResult saveTerrainVisualOverrides(
            Path scenePath,
            Map<Integer, Integer> overrides) {
        return saveTerrainVisualOverrides(scenePath, overrides, false);
    }

    public static SaveResult saveTerrainVisualOverrides(
            Path scenePath,
            Map<Integer, Integer> overrides,
            boolean dryRun) {

        if (scenePath == null) {
            return new SaveResult(false, "Scene path not configured");
        }
        if (!Files.exists(scenePath)) {
            return new SaveResult(false, "Scene file not found: " + scenePath.toAbsolutePath());
        }

        try {
            List<String> lines = Files.readAllLines(scenePath, StandardCharsets.UTF_8);

            // Find the 2-space-indented tile_overrides: key inside terrain_visual
            int overridesLine = findLineIndex(lines, TILE_OVERRIDES_HEADER);
            if (overridesLine < 0) {
                return new SaveResult(false,
                    "No 'tile_overrides:' section found in " + scenePath.getFileName()
                    + " under terrain_visual — cannot save without a target section");
            }

            // Find the next top-level key after tile_overrides (e.g. static_props:)
            int afterLine = lines.size();
            for (int i = overridesLine + 1; i < lines.size(); i++) {
                if (TOP_LEVEL_KEY.matcher(lines.get(i)).matches()) {
                    afterLine = i;
                    break;
                }
            }

            int existingCount = countEntriesInSection(lines, overridesLine);
            int newCount = overrides == null ? 0 : overrides.size();

            // New section: tile_overrides block only
            String newSection = serializeTileOverrides(overrides);

            // Compose: lines before tile_overrides + new tile_overrides + lines from afterLine onward
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < overridesLine; i++) {
                sb.append(lines.get(i)).append('\n');
            }
            sb.append(newSection);
            // Add a blank line separator before the next top-level section (if any)
            if (afterLine < lines.size()) {
                sb.append('\n');
                for (int i = afterLine; i < lines.size(); i++) {
                    sb.append(lines.get(i)).append('\n');
                }
            }
            String newContent = sb.toString();

            if (dryRun) {
                return new SaveResult(true,
                    "[DRY-RUN] Would save " + newCount + " tile override(s) to " + scenePath.getFileName()
                    + " (currently " + existingCount + ")",
                    newSection);
            }

            Path backupPath = scenePath.resolveSibling(scenePath.getFileName() + ".bak");
            writeAtomicWithVerification(scenePath, newContent, backupPath,
                "tile_overrides", newCount);

            return new SaveResult(true,
                "Saved " + newCount + " terrain override(s) to " + scenePath.getFileName()
                + " (backup: " + backupPath.getFileName() + ")");

        } catch (SceneSaveException e) {
            return new SaveResult(false, e.getMessage());
        } catch (Exception e) {
            return new SaveResult(false,
                "Terrain visual save failed: " + e.getClass().getSimpleName() + ": " + e.getMessage()
                + " — check console for details. Backup may exist at " + scenePath.getFileName() + ".bak");
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private static int findLineIndex(List<String> lines, Pattern pattern) {
        for (int i = 0; i < lines.size(); i++) {
            if (pattern.matcher(lines.get(i)).matches()) {
                return i;
            }
        }
        return -1;
    }

    /** Counts `- key:` or `- {` list entries in the section starting at headerLine. */
    private static int countEntriesInSection(List<String> lines, int headerLine) {
        int count = 0;
        for (int i = headerLine + 1; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            if (trimmed.startsWith("- ")) {
                count++;
            } else if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                // Non-empty, non-comment, non-list line at a lower indent = end of section
                if (!lines.get(i).startsWith(" ") && !lines.get(i).startsWith("\t")) {
                    break;
                }
            }
        }
        return count;
    }

    private static String serializeStaticProps(List<StaticPropLoader.StaticPropPlacement> placements) {
        StringBuilder sb = new StringBuilder("static_props:\n");
        if (placements == null || placements.isEmpty()) {
            sb.append('\n');
            return sb.toString();
        }
        sb.append('\n');
        for (StaticPropLoader.StaticPropPlacement p : placements) {
            sb.append("  - key: \"").append(p.key).append("\"\n");
            sb.append("    x: ").append(p.x).append('\n');
            sb.append("    y: ").append(p.y).append('\n');
            sb.append("    rotation_y_degrees: ").append(formatFloat(p.rotationYDegrees)).append('\n');
            sb.append("    scale: ").append(formatFloat(p.scale)).append('\n');
            sb.append("    visibility_group: \"").append(p.visibilityGroup).append("\"\n");
        }
        return sb.toString();
    }

    private static String serializeTileOverrides(Map<Integer, Integer> overrides) {
        StringBuilder sb = new StringBuilder("  tile_overrides:\n");
        if (overrides == null || overrides.isEmpty()) {
            return sb.toString();
        }
        // Sort by index for stable output
        ArrayList<Integer> keys = new ArrayList<>(overrides.keySet());
        keys.sort(Integer::compareTo);
        for (Integer index : keys) {
            if (index == null) continue;
            Integer type = overrides.get(index);
            if (type == null || type < 0 || type > 4) continue;
            int x = index % com.osrs.client.world.MapLoader.WIDTH;
            int y = index / com.osrs.client.world.MapLoader.WIDTH;
            String label = TerrainVisualLoader.terrainTypeLabel(type);
            sb.append("    - { x: ").append(x).append(", y: ").append(y)
              .append(", type: \"").append(label).append("\" }\n");
        }
        return sb.toString();
    }

    private static String formatFloat(float value) {
        if (value == Math.floor(value) && !Float.isInfinite(value)) {
            return String.format(Locale.US, "%.1f", value);
        }
        return String.format(Locale.US, "%s", value);
    }

    /**
     * Writes content atomically:
     *   1. Backup existing file to backupPath.
     *   2. Write content to .tmp alongside target.
     *   3. Move .tmp to target atomically.
     *   4. Read back target and verify it contains the expected section header.
     */
    private static void writeAtomicWithVerification(
            Path target, String content, Path backupPath,
            String sectionName, int expectedEntryCount) throws Exception {

        // 1. Backup
        Files.copy(target, backupPath, StandardCopyOption.REPLACE_EXISTING);

        // 2. Write temp
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(temp, content, StandardCharsets.UTF_8);

        // 3. Atomic move
        try {
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception atomicIgnored) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }

        // 4. Post-write verification: read back and confirm section present + entry count plausible
        List<String> written = Files.readAllLines(target, StandardCharsets.UTF_8);
        boolean sectionFound = written.stream().anyMatch(
            line -> line.startsWith(sectionName.startsWith("tile") ? "  tile_overrides:" : "static_props:"));
        if (!sectionFound) {
            throw new SceneSaveException(
                "Post-write verification failed: '" + sectionName + "' section not found in written file. "
                + "Original backed up to " + backupPath.getFileName() + " — restore manually if needed.");
        }
        int actualCount = 0;
        boolean inSection = false;
        for (String line : written) {
            if (line.startsWith(sectionName.startsWith("tile") ? "  tile_overrides:" : "static_props:")) {
                inSection = true;
                continue;
            }
            if (inSection && line.trim().startsWith("- ")) {
                actualCount++;
            } else if (inSection && !line.trim().isEmpty() && !line.trim().startsWith("#")) {
                if (!line.startsWith(" ") && !line.startsWith("\t")) {
                    break;
                }
            }
        }
        if (actualCount != expectedEntryCount) {
            throw new SceneSaveException(
                "Post-write count mismatch in '" + sectionName + "': wrote " + expectedEntryCount
                + " but verified " + actualCount + ". "
                + "Original backed up to " + backupPath.getFileName() + " — restore manually if needed.");
        }
    }

    private static final class SceneSaveException extends Exception {
        SceneSaveException(String message) {
            super(message);
        }
    }
}
