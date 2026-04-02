package com.osrs.shared;

/**
 * OSRS XP table — authoritative source for both server and client.
 *
 * Extracts the same formula used in Player.java so that the client
 * can compute level-to-XP and XP-to-next-level without duplicating logic.
 *
 * All XP values here are WHOLE numbers (not tenths).
 * Server stores XP as tenths internally; divide by 10 before calling these methods.
 */
public final class XpTable {

    // TABLE[level-1] = total whole XP needed to reach 'level'.
    // TABLE[0] = 0 (no XP needed to be level 1).
    private static final long[] TABLE = buildTable();

    private XpTable() {}

    /**
     * Total whole XP required to reach the given level (clamped 1–99).
     * Returns 0 for level 1.
     */
    public static long xpForLevel(int level) {
        return TABLE[Math.max(1, Math.min(99, level)) - 1];
    }

    /**
     * Total whole XP required to reach the level AFTER the given level.
     * Returns Long.MAX_VALUE at level 99 (no next level).
     */
    public static long xpForNextLevel(int currentLevel) {
        if (currentLevel >= 99) return Long.MAX_VALUE;
        return xpForLevel(currentLevel + 1);
    }

    /**
     * Level corresponding to the given whole-XP total (1–99).
     */
    public static int levelForXp(long wholeXp) {
        int level = 1;
        for (int i = 1; i < 99; i++) {
            if (wholeXp >= TABLE[i]) level = i + 1;
            else break;
        }
        return Math.min(level, 99);
    }

    private static long[] buildTable() {
        long[] table = new long[99];
        table[0] = 0;
        long points = 0;
        for (int lvl = 1; lvl < 99; lvl++) {
            points += (long) Math.floor(lvl + 300.0 * Math.pow(2.0, lvl / 7.0));
            table[lvl] = (long) Math.floor(points / 4.0);
        }
        return table;
    }
}
