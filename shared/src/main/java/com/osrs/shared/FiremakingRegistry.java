package com.osrs.shared;

import java.util.List;
import java.util.Map;

/**
 * Shared F2P Firemaking progression data.
 *
 * XP values are stored in tenths to keep exact decimal values.
 */
public final class FiremakingRegistry {

    public record LogTier(int itemId,
                          String name,
                          int levelRequirement,
                          int xpTenths,
                          int successLow,
                          int successHigh) {}

    private static final List<LogTier> F2P_LOGS = List.of(
        new LogTier(1511, "Logs", 1, 400, 130, 245),
        new LogTier(1521, "Oak logs", 15, 600, 112, 232),
        new LogTier(1522, "Willow logs", 30, 900, 96, 220),
        new LogTier(1523, "Maple logs", 45, 1350, 82, 208),
        new LogTier(1524, "Yew logs", 60, 2025, 70, 195),
        new LogTier(1525, "Magic logs", 75, 3038, 58, 182)
    );

    private static final Map<Integer, LogTier> BY_ITEM_ID = Map.of(
        1511, F2P_LOGS.get(0),
        1521, F2P_LOGS.get(1),
        1522, F2P_LOGS.get(2),
        1523, F2P_LOGS.get(3),
        1524, F2P_LOGS.get(4),
        1525, F2P_LOGS.get(5)
    );

    private FiremakingRegistry() {
    }

    public static List<LogTier> f2pLogs() {
        return F2P_LOGS;
    }

    public static LogTier getByItemId(int itemId) {
        return BY_ITEM_ID.get(itemId);
    }

    public static int successThreshold255(LogTier tier, int firemakingLevel) {
        if (tier == null) {
            return 0;
        }
        int clampedLevel = Math.max(1, Math.min(99, firemakingLevel));
        int threshold = tier.successLow() + (int) Math.floor((tier.successHigh() - tier.successLow()) * (double) clampedLevel / 99.0);
        return Math.max(0, Math.min(255, threshold));
    }
}
