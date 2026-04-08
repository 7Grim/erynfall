package com.osrs.shared;

import java.util.List;
import java.util.Map;

/**
 * Shared standard-first Cooking progression data.
 *
 * This is a pure data source and is intentionally not wired into gameplay yet.
 */
public final class CookingRegistry {

    public enum StationType {
        FIRE,
        RANGE
    }

    public record FoodTier(
        int rawItemId,
        int cookedItemId,
        int burntItemId,
        String cookedName,
        int levelRequirement,
        int xpTenths,
        int healAmount,
        int noBurnFireLevel,
        int noBurnRangeLevel,
        int fireLow,
        int fireHigh,
        int rangeLow,
        int rangeHigh
    ) {}

    private static final FoodTier COOKED_CHICKEN = new FoodTier(
        2138, 2140, 2144, "Cooked chicken",
        1, 300, 3,
        34, 31,
        128, 512, 138, 532
    );

    // 2148 is intentionally custom in this repo because 2142 is already used by Cowhide.
    private static final FoodTier COOKED_MEAT = new FoodTier(
        2132, 2148, 2146, "Cooked meat",
        1, 300, 3,
        34, 31,
        128, 512, 138, 532
    );

    private static final FoodTier COOKED_RAT_MEAT = new FoodTier(
        2134, 2148, 2146, "Cooked meat",
        1, 300, 3,
        34, 31,
        128, 512, 138, 532
    );

    private static final FoodTier SHRIMPS = new FoodTier(
        317, 315, 7954, "Shrimps",
        1, 300, 3,
        34, 31,
        128, 512, 138, 532
    );

    private static final FoodTier ANCHOVIES = new FoodTier(
        321, 319, 323, "Anchovies",
        1, 300, 1,
        34, 31,
        128, 512, 138, 532
    );

    private static final FoodTier SARDINE = new FoodTier(
        327, 325, 369, "Sardine",
        1, 400, 4,
        38, 34,
        118, 492, 128, 512
    );

    private static final FoodTier HERRING = new FoodTier(
        345, 347, 357, "Herring",
        5, 500, 5,
        41, 38,
        108, 472, 118, 492
    );

    private static final FoodTier TROUT = new FoodTier(
        335, 333, 343, "Trout",
        15, 700, 7,
        49, 45,
        88, 432, 98, 452
    );

    private static final FoodTier PIKE = new FoodTier(
        349, 351, 343, "Pike",
        20, 800, 8,
        54, 49,
        78, 412, 88, 432
    );

    private static final FoodTier SALMON = new FoodTier(
        331, 329, 343, "Salmon",
        25, 900, 9,
        58, 55,
        68, 392, 78, 402
    );

    private static final FoodTier TUNA = new FoodTier(
        359, 361, 367, "Tuna",
        30, 1000, 10,
        63, 63,
        58, 372, 58, 372
    );

    private static final FoodTier LOBSTER = new FoodTier(
        377, 379, 381, "Lobster",
        40, 1200, 12,
        74, 74,
        38, 332, 38, 332
    );

    private static final FoodTier SWORDFISH = new FoodTier(
        371, 373, 375, "Swordfish",
        45, 1400, 14,
        86, 80,
        18, 292, 30, 310
    );

    private static final List<FoodTier> FOODS = List.of(
        COOKED_CHICKEN,
        COOKED_MEAT,
        COOKED_RAT_MEAT,
        SHRIMPS,
        ANCHOVIES,
        SARDINE,
        HERRING,
        TROUT,
        PIKE,
        SALMON,
        TUNA,
        LOBSTER,
        SWORDFISH
    );

    private static final Map<Integer, FoodTier> BY_RAW_ITEM_ID = Map.ofEntries(
        Map.entry(COOKED_CHICKEN.rawItemId(), COOKED_CHICKEN),
        Map.entry(COOKED_MEAT.rawItemId(), COOKED_MEAT),
        Map.entry(COOKED_RAT_MEAT.rawItemId(), COOKED_RAT_MEAT),
        Map.entry(SHRIMPS.rawItemId(), SHRIMPS),
        Map.entry(ANCHOVIES.rawItemId(), ANCHOVIES),
        Map.entry(SARDINE.rawItemId(), SARDINE),
        Map.entry(HERRING.rawItemId(), HERRING),
        Map.entry(TROUT.rawItemId(), TROUT),
        Map.entry(PIKE.rawItemId(), PIKE),
        Map.entry(SALMON.rawItemId(), SALMON),
        Map.entry(TUNA.rawItemId(), TUNA),
        Map.entry(LOBSTER.rawItemId(), LOBSTER),
        Map.entry(SWORDFISH.rawItemId(), SWORDFISH)
    );

    private CookingRegistry() {}

    public static List<FoodTier> foods() {
        return FOODS;
    }

    public static FoodTier getByRawItemId(int rawItemId) {
        return BY_RAW_ITEM_ID.get(rawItemId);
    }

    public static int noBurnLevel(FoodTier food, StationType stationType) {
        if (food == null || stationType == null) {
            return Integer.MAX_VALUE;
        }
        return stationType == StationType.RANGE ? food.noBurnRangeLevel() : food.noBurnFireLevel();
    }

    public static int burnLow(FoodTier food, StationType stationType) {
        if (food == null || stationType == null) {
            return 0;
        }
        return stationType == StationType.RANGE ? food.rangeLow() : food.fireLow();
    }

    public static int burnHigh(FoodTier food, StationType stationType) {
        if (food == null || stationType == null) {
            return 0;
        }
        return stationType == StationType.RANGE ? food.rangeHigh() : food.fireHigh();
    }
}
