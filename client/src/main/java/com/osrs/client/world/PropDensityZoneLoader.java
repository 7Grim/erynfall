package com.osrs.client.world;

import com.badlogic.gdx.Gdx;
import com.osrs.client.LaunchOptions;
import com.osrs.client.world.StaticPropLoader.StaticPropPlacement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads density_zones from scene.yaml — authoring-only metadata used to
 * drive the WORLD_PLACEMENT density overlay (Z key).
 *
 * Zones have no gameplay effect and are never loaded in production builds.
 */
public final class PropDensityZoneLoader {

    public enum Density { SPARSE, MEDIUM, DENSE }

    /**
     * An authoring zone. Tile-space bounds inclusive.
     * propTarget is derived from density and area; used only for the overlay HUD.
     */
    public record PropDensityZone(
            String name,
            int minX, int minY, int maxX, int maxY,
            Density density,
            String note) {

        public int area() {
            return Math.max(0, (maxX - minX + 1)) * Math.max(0, (maxY - minY + 1));
        }

        /** Rough target prop count based on density and tile area. */
        public int propTarget() {
            if (area() <= 0) return 0;
            return switch (density) {
                case SPARSE -> Math.max(1, area() / 8);
                case MEDIUM -> Math.max(1, area() / 4);
                case DENSE  -> Math.max(1, area() / 2);
            };
        }

        public int countPropsInside(List<StaticPropPlacement> placements) {
            int count = 0;
            for (StaticPropPlacement p : placements) {
                if (p.x >= minX && p.x <= maxX && p.y >= minY && p.y <= maxY) {
                    count++;
                }
            }
            return count;
        }

        public boolean contains(int tileX, int tileY) {
            return tileX >= minX && tileX <= maxX && tileY >= minY && tileY <= maxY;
        }
    }

    private PropDensityZoneLoader() {}

    public static List<PropDensityZone> load(LaunchOptions launchOptions) {
        try {
            WorldSceneLoader.WorldSceneData sceneData = WorldSceneLoader.load(launchOptions);
            List<Map<String, Object>> rows = sceneData.densityZones();
            if (rows.isEmpty()) {
                return List.of();
            }
            List<PropDensityZone> zones = new ArrayList<>(rows.size());
            for (Map<String, Object> row : rows) {
                PropDensityZone zone = parseZone(row);
                if (zone != null) {
                    zones.add(zone);
                }
            }
            Gdx.app.log("PropDensityZoneLoader", "Loaded " + zones.size() + " density zone(s)");
            return List.copyOf(zones);
        } catch (Exception e) {
            Gdx.app.log("PropDensityZoneLoader", "WARN: failed to load density zones: " + e.getMessage());
            return List.of();
        }
    }

    private static PropDensityZone parseZone(Map<String, Object> row) {
        String name = row.get("name") instanceof String s ? s.trim() : "unnamed";
        int minX = intOrDefault(row.get("min_x"), 0);
        int minY = intOrDefault(row.get("min_y"), 0);
        int maxX = intOrDefault(row.get("max_x"), -1);
        int maxY = intOrDefault(row.get("max_y"), -1);
        if (maxX < minX || maxY < minY) {
            return null;
        }
        Density density = parseDensity(row.get("density"));
        String note = row.get("note") instanceof String n ? n.trim() : "";

        int clampedMinX = Math.max(0, minX);
        int clampedMinY = Math.max(0, minY);
        int clampedMaxX = Math.min(MapLoader.WIDTH - 1, maxX);
        int clampedMaxY = Math.min(MapLoader.HEIGHT - 1, maxY);
        return new PropDensityZone(name, clampedMinX, clampedMinY, clampedMaxX, clampedMaxY, density, note);
    }

    private static Density parseDensity(Object value) {
        if (!(value instanceof String s)) return Density.MEDIUM;
        return switch (s.trim().toLowerCase()) {
            case "sparse" -> Density.SPARSE;
            case "dense"  -> Density.DENSE;
            default       -> Density.MEDIUM;
        };
    }

    private static int intOrDefault(Object value, int def) {
        return value instanceof Number n ? n.intValue() : def;
    }
}
