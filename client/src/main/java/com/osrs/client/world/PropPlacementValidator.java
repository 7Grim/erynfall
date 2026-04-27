package com.osrs.client.world;

import com.osrs.client.renderer.ModelLibrary;
import com.osrs.client.world.StaticPropLoader.StaticPropPlacement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dev-time sanity checks for static prop/building placements loaded from scene.yaml.
 * All issues are logged as WARN — nothing blocks runtime.
 *
 * Call once after StaticPropLoader.load() and ModelLibrary are both ready.
 */
public final class PropPlacementValidator {

    private static final Logger LOG = LoggerFactory.getLogger(PropPlacementValidator.class);

    // Scale thresholds
    private static final float SCALE_MIN          = 0.1f;   // below this: suspiciously tiny
    private static final float PROP_SCALE_MAX      = 3.0f;   // props larger than this need review
    private static final float SHELL_SCALE_MAX     = 4.5f;   // building shells may go up to ~2.5×

    // Building class scale bands (docs/BUILDING_CLASS_SPEC.md).
    // Each entry: {min_scale, max_scale}.  A shell must fall in at least one band.
    private static final float[][] SHELL_CLASS_BANDS = {
        { 1.40f, 1.65f },  // tiny_hut
        { 1.65f, 1.95f },  // small_service
        { 1.80f, 2.25f },  // forge_workshop
        { 2.00f, 2.55f },  // medium_hub
    };
    private static final String[] SHELL_CLASS_NAMES = {
        "tiny_hut", "small_service", "forge_workshop", "medium_hub",
    };
    // Hard limits for scale warnings specific to shells (separate from SHELL_SCALE_MAX).
    private static final float SHELL_OSRS_SCALE_MIN = 1.40f;
    private static final float SHELL_OSRS_SCALE_MAX = 2.50f;

    // Rotation tolerance: flag anything that isn't a multiple of 90° within this margin
    private static final float ROTATION_TOLERANCE_DEG = 1.0f;

    // Map bounds (tiles 0..WIDTH-1, 0..HEIGHT-1)
    private static final int MAP_MAX = MapLoader.WIDTH - 1;

    private static final List<String> VALID_VISIBILITY_GROUPS = List.of("base", "roof");

    private PropPlacementValidator() {}

    public static void validate(List<StaticPropPlacement> placements, ModelLibrary modelLibrary) {
        if (placements == null || placements.isEmpty()) return;

        int warnings = 0;

        // Build index for unpaired-shell detection: (x,y,scale,rot) → present groups
        Map<String, List<String>> shellIndex = new HashMap<>();

        for (StaticPropPlacement p : placements) {

            // 1 — key exists in manifest
            if (modelLibrary.getMeta(p.key) == null) {
                LOG.warn("PropValidator: unknown key '{}' at ({},{}) — not in manifest", p.key, p.x, p.y);
                warnings++;
            }

            // 2 — bounds
            if (p.x < 0 || p.x > MAP_MAX || p.y < 0 || p.y > MAP_MAX) {
                LOG.warn("PropValidator: '{}' out of bounds at ({},{}) — map is 0–{}", p.key, p.x, p.y, MAP_MAX);
                warnings++;
            }

            // 3 — scale
            String category = categoryOf(p.key, modelLibrary);
            float scaleMax = "shell".equals(category) ? SHELL_SCALE_MAX : PROP_SCALE_MAX;
            if (p.scale < SCALE_MIN) {
                LOG.warn("PropValidator: '{}' at ({},{}) scale={} — below minimum {}", p.key, p.x, p.y, p.scale, SCALE_MIN);
                warnings++;
            } else if (p.scale > scaleMax) {
                LOG.warn("PropValidator: '{}' at ({},{}) scale={} — exceeds {} limit {} for category '{}'",
                        p.key, p.x, p.y, p.scale, "shell".equals(category) ? "shell" : "prop", scaleMax, category);
                warnings++;
            }

            // 3b — building shell class-band check (docs/BUILDING_CLASS_SPEC.md)
            if ("shell".equals(category)) {
                if (p.scale < SHELL_OSRS_SCALE_MIN) {
                    LOG.warn("PropValidator: shell '{}' at ({},{}) scale={} — below OSRS minimum {}"
                            + " (walls may not clear the 1.80 WU player). See docs/BUILDING_CLASS_SPEC.md",
                            p.key, p.x, p.y, p.scale, SHELL_OSRS_SCALE_MIN);
                    warnings++;
                } else if (p.scale > SHELL_OSRS_SCALE_MAX) {
                    LOG.warn("PropValidator: shell '{}' at ({},{}) scale={} — above recommended OSRS max {}"
                            + ". Building will dominate the zone. See docs/BUILDING_CLASS_SPEC.md",
                            p.key, p.x, p.y, p.scale, SHELL_OSRS_SCALE_MAX);
                    warnings++;
                } else {
                    boolean inAnyBand = false;
                    for (float[] band : SHELL_CLASS_BANDS) {
                        if (p.scale >= band[0] && p.scale <= band[1]) {
                            inAnyBand = true;
                            break;
                        }
                    }
                    if (!inAnyBand) {
                        StringBuilder bands = new StringBuilder();
                        for (int b = 0; b < SHELL_CLASS_BANDS.length; b++) {
                            if (b > 0) bands.append(", ");
                            bands.append(SHELL_CLASS_NAMES[b])
                                 .append(" (").append(SHELL_CLASS_BANDS[b][0])
                                 .append("–").append(SHELL_CLASS_BANDS[b][1]).append(")");
                        }
                        LOG.warn("PropValidator: shell '{}' at ({},{}) scale={} not in any class band: {}."
                                + " See docs/BUILDING_CLASS_SPEC.md",
                                p.key, p.x, p.y, p.scale, bands);
                        warnings++;
                    }
                }
            }

            // 4 — non-cardinal rotation
            float rot = ((p.rotationYDegrees % 360f) + 360f) % 360f;
            float nearestCard = Math.round(rot / 90f) * 90f;
            if (Math.abs(rot - nearestCard) > ROTATION_TOLERANCE_DEG) {
                LOG.warn("PropValidator: '{}' at ({},{}) rotation={}° — not a cardinal multiple of 90°", p.key, p.x, p.y, p.rotationYDegrees);
                warnings++;
            }

            // 5 — visibility group
            if (!VALID_VISIBILITY_GROUPS.contains(p.visibilityGroup)) {
                LOG.warn("PropValidator: '{}' at ({},{}) visibility_group='{}' — expected one of {}", p.key, p.x, p.y, p.visibilityGroup, VALID_VISIBILITY_GROUPS);
                warnings++;
            }

            // 6 — collect shells for unpaired check
            if ("shell".equals(category)) {
                String posKey = p.x + "," + p.y + "," + p.scale + "," + p.rotationYDegrees;
                shellIndex.computeIfAbsent(posKey, k -> new ArrayList<>()).add(p.visibilityGroup);
            }
        }

        // 7 — unpaired building shells (base without roof or vice-versa)
        for (Map.Entry<String, List<String>> entry : shellIndex.entrySet()) {
            List<String> groups = entry.getValue();
            boolean hasBase = groups.contains("base");
            boolean hasRoof = groups.contains("roof");
            if (hasBase && !hasRoof) {
                LOG.warn("PropValidator: building shell at [{}] has 'base' but no matching 'roof'", entry.getKey());
                warnings++;
            } else if (hasRoof && !hasBase) {
                LOG.warn("PropValidator: building shell at [{}] has 'roof' but no matching 'base'", entry.getKey());
                warnings++;
            }
        }

        if (warnings == 0) {
            LOG.info("PropValidator: {} placements — all clear", placements.size());
        } else {
            LOG.warn("PropValidator: {} placement warning(s) across {} props — check scene.yaml", warnings, placements.size());
        }
    }

    private static String categoryOf(String key, ModelLibrary modelLibrary) {
        ModelLibrary.ModelMeta meta = modelLibrary.getMeta(key);
        return meta != null ? meta.category() : "";
    }
}
