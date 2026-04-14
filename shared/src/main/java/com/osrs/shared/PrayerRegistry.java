package com.osrs.shared;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Shared F2P prayer definitions used by both server and client.
 */
public final class PrayerRegistry {

    public enum BonusType {
        ATTACK,
        STRENGTH,
        DEFENCE
    }

    public record PrayerDef(
        int id,
        String name,
        int levelRequirement,
        BonusType bonusType,
        double multiplier,
        String effectSummary
    ) {}

    private static final List<PrayerDef> F2P_PRAYERS = List.of(
        new PrayerDef(1, "Thick Skin", 1, BonusType.DEFENCE, 1.05, "+5% Defence"),
        new PrayerDef(2, "Burst of Strength", 4, BonusType.STRENGTH, 1.05, "+5% Strength"),
        new PrayerDef(3, "Clarity of Thought", 7, BonusType.ATTACK, 1.05, "+5% Attack"),
        new PrayerDef(4, "Rock Skin", 10, BonusType.DEFENCE, 1.10, "+10% Defence"),
        new PrayerDef(5, "Superhuman Strength", 13, BonusType.STRENGTH, 1.10, "+10% Strength"),
        new PrayerDef(6, "Improved Reflexes", 16, BonusType.ATTACK, 1.10, "+10% Attack")
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
}
