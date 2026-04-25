package com.osrs.shared;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Shared F2P prayer definitions used by both server and client.
 *
 * Drain rate unit: drain points per OSRS game tick (0.6 s).
 * A prayer point is lost each time the player's drain accumulator reaches
 * DRAIN_THRESHOLD (100). Summing all active prayers' drainRate values
 * per tick gives the correct OSRS drain speed without per-tick floating-point math.
 *
 *   Tier-1 prayers (drainRate 3):   100/3  = 33.3 ticks ≈ 20 s/pt = 3 pts/min
 *   Tier-2 prayers (drainRate 6):   100/6  = 16.7 ticks ≈ 10 s/pt = 6 pts/min
 *   Protect prayers (drainRate 18): 100/18 =  5.6 ticks ≈  3.3 s/pt = 18 pts/min
 */
public final class PrayerRegistry {

    /** Drain accumulator threshold — losing 1 prayer point when reached. */
    public static final int DRAIN_THRESHOLD = 100;

    public enum BonusType {
        ATTACK,
        STRENGTH,
        DEFENCE,
        NONE
    }

    public enum ProtectionType {
        NONE,
        MELEE,
        RANGED,
        MAGIC
    }

    public record PrayerDef(
        int id,
        String name,
        int levelRequirement,
        BonusType bonusType,
        double multiplier,
        ProtectionType protectionType,
        int drainRate,
        String effectSummary
    ) {}

    // Convenience factory for stat-boosting prayers (no protection)
    private static PrayerDef boost(int id, String name, int lvl, BonusType bt, double mult, int drain, String summary) {
        return new PrayerDef(id, name, lvl, bt, mult, ProtectionType.NONE, drain, summary);
    }

    // Convenience factory for protection prayers (no stat boost)
    private static PrayerDef protect(int id, String name, int lvl, ProtectionType pt, int drain, String summary) {
        return new PrayerDef(id, name, lvl, BonusType.NONE, 1.0, pt, drain, summary);
    }

    private static final List<PrayerDef> F2P_PRAYERS = List.of(
        boost(1, "Thick Skin",           1,  BonusType.DEFENCE,  1.05, 3,  "+5% Defence"),
        boost(2, "Burst of Strength",    4,  BonusType.STRENGTH, 1.05, 3,  "+5% Strength"),
        boost(3, "Clarity of Thought",   7,  BonusType.ATTACK,   1.05, 3,  "+5% Attack"),
        boost(4, "Rock Skin",            10, BonusType.DEFENCE,  1.10, 6,  "+10% Defence"),
        boost(5, "Superhuman Strength",  13, BonusType.STRENGTH, 1.10, 6,  "+10% Strength"),
        boost(6, "Improved Reflexes",    16, BonusType.ATTACK,   1.10, 6,  "+10% Attack"),
        protect(7, "Protect from Magic",    37, ProtectionType.MAGIC,  18, "Block magic damage"),
        protect(8, "Protect from Missiles", 40, ProtectionType.RANGED, 18, "Block ranged damage"),
        protect(9, "Protect from Melee",    43, ProtectionType.MELEE,  18, "Block melee damage")
    );

    private static final Map<Integer, PrayerDef> BY_ID;

    static {
        Map<Integer, PrayerDef> map = new HashMap<>();
        for (PrayerDef prayer : F2P_PRAYERS) {
            map.put(prayer.id(), prayer);
        }
        BY_ID = Collections.unmodifiableMap(map);
    }

    private PrayerRegistry() {}

    public static List<PrayerDef> f2pPrayers() {
        return F2P_PRAYERS;
    }

    public static Optional<PrayerDef> byId(int prayerId) {
        return Optional.ofNullable(BY_ID.get(prayerId));
    }

    /** Returns the highest active stat multiplier for the given bonus type. */
    public static double highestActiveMultiplier(Set<Integer> activePrayerIds, BonusType bonusType) {
        if (activePrayerIds == null || activePrayerIds.isEmpty()) {
            return 1.0;
        }
        double highest = 1.0;
        for (int prayerId : activePrayerIds) {
            PrayerDef prayer = BY_ID.get(prayerId);
            if (prayer == null || prayer.bonusType() != bonusType) {
                continue;
            }
            if (prayer.multiplier() > highest) {
                highest = prayer.multiplier();
            }
        }
        return highest;
    }

    /** Returns true if the player has an active prayer that protects against the given attack type. */
    public static boolean isProtected(Set<Integer> activePrayerIds, ProtectionType attackType) {
        if (activePrayerIds == null || activePrayerIds.isEmpty()
                || attackType == null || attackType == ProtectionType.NONE) {
            return false;
        }
        for (int prayerId : activePrayerIds) {
            PrayerDef prayer = BY_ID.get(prayerId);
            if (prayer != null && prayer.protectionType() == attackType) {
                return true;
            }
        }
        return false;
    }

    /**
     * Computes total drain accumulator increment for one OSRS tick
     * given the set of active prayer IDs.
     */
    public static int totalDrainRate(Set<Integer> activePrayerIds) {
        if (activePrayerIds == null || activePrayerIds.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (int prayerId : activePrayerIds) {
            PrayerDef prayer = BY_ID.get(prayerId);
            if (prayer != null) {
                total += prayer.drainRate();
            }
        }
        return total;
    }
}
