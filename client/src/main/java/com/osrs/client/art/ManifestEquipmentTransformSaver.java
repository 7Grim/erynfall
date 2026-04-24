package com.osrs.client.art;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ManifestEquipmentTransformSaver {

    private static final Pattern ENTRY_START = Pattern.compile("^\\s*-\\s+key:\\s*(.+?)\\s*$");
    private static final Pattern FIELD_LINE = Pattern.compile("^(\\s+)([a-z_]+):\\s*(.*)$");

    public record EffectiveTransform(float offsetX,
                                     float offsetY,
                                     float offsetZ,
                                     float rotX,
                                     float rotY,
                                     float rotZ) {}

    public record SaveResult(boolean success, String message) {}

    private ManifestEquipmentTransformSaver() {}

    public static SaveResult save(Path manifestPath, String key, EffectiveTransform transform) {
        if (manifestPath == null || key == null || key.isBlank() || transform == null) {
            return new SaveResult(false, "Invalid save request");
        }
        if (!Files.exists(manifestPath)) {
            return new SaveResult(false, "Manifest file not found: " + manifestPath);
        }

        try {
            List<String> lines = Files.readAllLines(manifestPath, StandardCharsets.UTF_8);
            int start = -1;
            int endExclusive = lines.size();

            for (int i = 0; i < lines.size(); i++) {
                Matcher m = ENTRY_START.matcher(lines.get(i));
                if (!m.matches()) {
                    continue;
                }
                String entryKey = parseScalar(m.group(1));
                if (key.equals(entryKey)) {
                    start = i;
                    for (int j = i + 1; j < lines.size(); j++) {
                        if (ENTRY_START.matcher(lines.get(j)).matches()) {
                            endExclusive = j;
                            break;
                        }
                    }
                    break;
                }
            }

            if (start < 0) {
                return new SaveResult(false, "Manifest entry not found for key: " + key);
            }

            upsertField(lines, start, endExclusive, "offset_x", formatOffset(transform.offsetX()));
            upsertField(lines, start, endExclusive, "offset_y", formatOffset(transform.offsetY()));
            upsertField(lines, start, endExclusive, "offset_z", formatOffset(transform.offsetZ()));
            upsertField(lines, start, endExclusive, "rot_x", formatRotation(transform.rotX()));
            upsertField(lines, start, endExclusive, "rot_y", formatRotation(transform.rotY()));
            upsertField(lines, start, endExclusive, "rot_z", formatRotation(transform.rotZ()));

            Path backup = manifestPath.resolveSibling(manifestPath.getFileName() + ".bak");
            Files.copy(manifestPath, backup, StandardCopyOption.REPLACE_EXISTING);

            Path temp = manifestPath.resolveSibling(manifestPath.getFileName() + ".tmp");
            Files.write(temp, lines, StandardCharsets.UTF_8);
            try {
                Files.move(temp, manifestPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException atomicIgnored) {
                Files.move(temp, manifestPath, StandardCopyOption.REPLACE_EXISTING);
            }
            return new SaveResult(true, "Saved manifest entry for key " + key
                + " (backup: " + backup.getFileName() + ")");
        } catch (Exception e) {
            return new SaveResult(false, "Manifest save failed: " + e.getMessage());
        }
    }

    private static void upsertField(List<String> lines, int start, int endExclusive, String field, String value) {
        for (int i = start; i < endExclusive && i < lines.size(); i++) {
            Matcher matcher = FIELD_LINE.matcher(lines.get(i));
            if (!matcher.matches()) {
                continue;
            }
            String fieldName = matcher.group(2);
            if (field.equals(fieldName)) {
                String indent = matcher.group(1);
                lines.set(i, indent + field + ": " + value);
                return;
            }
        }

        int insertAt = Math.min(endExclusive, lines.size());
        for (int i = start; i < endExclusive && i < lines.size(); i++) {
            Matcher matcher = FIELD_LINE.matcher(lines.get(i));
            if (!matcher.matches()) {
                continue;
            }
            String fieldName = matcher.group(2);
            if ("anchor_name".equals(fieldName) || "item_id".equals(fieldName) || "equip_slot".equals(fieldName)) {
                insertAt = i + 1;
            }
        }
        lines.add(insertAt, "    " + field + ": " + value);
    }

    private static String parseScalar(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String formatOffset(float value) {
        return String.format(Locale.US, "%.3f", value);
    }

    private static String formatRotation(float value) {
        return String.format(Locale.US, "%.2f", value);
    }
}
