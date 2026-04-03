package com.osrs.shared;

import java.util.List;
import java.util.Map;

/**
 * Shared standard-first Fishing progression data.
 *
 * This intentionally mirrors WoodcuttingRegistry as a pure data source and is
 * not wired into gameplay logic in this prompt.
 */
public final class FishingRegistry {

    public enum ActionType {
        NET,
        BAIT,
        LURE,
        CAGE,
        HARPOON
    }

    public record Tool(int itemId, String name, int unlockLevel, int consumableItemId) {}

    public record CatchTier(int itemId, String name, int levelRequirement, int xpTenths, int low, int high) {}

    public record SpotAction(ActionType type, int toolItemId, int consumableItemId, List<CatchTier> catches) {}

    public record SpotType(int definitionId, String name, int moveMinTicks, int moveMaxTicks, boolean movable,
                           List<SpotAction> actions) {}

    private static final Tool SMALL_NET = new Tool(303, "Small fishing net", 1, 0);
    private static final Tool FISHING_ROD = new Tool(307, "Fishing rod", 5, 313);
    private static final Tool FLY_FISHING_ROD = new Tool(309, "Fly fishing rod", 20, 314);
    private static final Tool LOBSTER_POT = new Tool(301, "Lobster pot", 40, 0);
    private static final Tool HARPOON = new Tool(311, "Harpoon", 35, 0);

    private static final List<Tool> TOOLS = List.of(
        SMALL_NET,
        FISHING_ROD,
        FLY_FISHING_ROD,
        LOBSTER_POT,
        HARPOON
    );

    private static final Map<Integer, Tool> TOOLS_BY_ITEM_ID = Map.of(
        SMALL_NET.itemId(), SMALL_NET,
        FISHING_ROD.itemId(), FISHING_ROD,
        FLY_FISHING_ROD.itemId(), FLY_FISHING_ROD,
        LOBSTER_POT.itemId(), LOBSTER_POT,
        HARPOON.itemId(), HARPOON
    );

    private static final CatchTier RAW_SHRIMPS = new CatchTier(317, "Raw shrimps", 1, 100, 48, 256);
    private static final CatchTier RAW_ANCHOVIES = new CatchTier(321, "Raw anchovies", 15, 400, 24, 128);
    private static final CatchTier RAW_SARDINE = new CatchTier(327, "Raw sardine", 5, 200, 32, 192);
    private static final CatchTier RAW_HERRING = new CatchTier(345, "Raw herring", 10, 300, 24, 128);
    private static final CatchTier RAW_TROUT = new CatchTier(335, "Raw trout", 20, 500, 32, 192);
    private static final CatchTier RAW_SALMON = new CatchTier(331, "Raw salmon", 30, 700, 16, 96);
    private static final CatchTier RAW_PIKE = new CatchTier(349, "Raw pike", 25, 600, 16, 96);
    private static final CatchTier RAW_LOBSTER = new CatchTier(377, "Raw lobster", 40, 900, 6, 95);
    private static final CatchTier RAW_TUNA = new CatchTier(359, "Raw tuna", 35, 800, 8, 64);
    private static final CatchTier RAW_SWORDFISH = new CatchTier(371, "Raw swordfish", 50, 1000, 4, 48);

    private static final SpotType TUTORIAL_NET_SPOT = new SpotType(
        200,
        "Tutorial Fishing Spot",
        0,
        0,
        false,
        List.of(
            new SpotAction(ActionType.NET, 303, 0, List.of(RAW_SHRIMPS))
        )
    );

    private static final SpotType NET_BAIT_SPOT = new SpotType(
        201,
        "Net/Bait Fishing Spot",
        250,
        530,
        true,
        List.of(
            new SpotAction(ActionType.NET, 303, 0, List.of(RAW_ANCHOVIES, RAW_SHRIMPS)),
            new SpotAction(ActionType.BAIT, 307, 313, List.of(RAW_HERRING, RAW_SARDINE))
        )
    );

    private static final SpotType LURE_BAIT_SPOT = new SpotType(
        202,
        "Lure/Bait Fishing Spot",
        250,
        530,
        true,
        List.of(
            new SpotAction(ActionType.LURE, 309, 314, List.of(RAW_SALMON, RAW_TROUT)),
            new SpotAction(ActionType.BAIT, 307, 313, List.of(RAW_PIKE))
        )
    );

    private static final SpotType CAGE_HARPOON_SPOT = new SpotType(
        203,
        "Cage/Harpoon Fishing Spot",
        250,
        530,
        true,
        List.of(
            new SpotAction(ActionType.CAGE, 301, 0, List.of(RAW_LOBSTER)),
            new SpotAction(ActionType.HARPOON, 311, 0, List.of(RAW_SWORDFISH, RAW_TUNA))
        )
    );

    private static final List<SpotType> SPOT_TYPES = List.of(
        TUTORIAL_NET_SPOT,
        NET_BAIT_SPOT,
        LURE_BAIT_SPOT,
        CAGE_HARPOON_SPOT
    );

    private static final Map<Integer, SpotType> SPOTS_BY_DEFINITION_ID = Map.of(
        TUTORIAL_NET_SPOT.definitionId(), TUTORIAL_NET_SPOT,
        NET_BAIT_SPOT.definitionId(), NET_BAIT_SPOT,
        LURE_BAIT_SPOT.definitionId(), LURE_BAIT_SPOT,
        CAGE_HARPOON_SPOT.definitionId(), CAGE_HARPOON_SPOT
    );

    private FishingRegistry() {}

    public static List<Tool> tools() {
        return TOOLS;
    }

    public static List<SpotType> spotTypes() {
        return SPOT_TYPES;
    }

    public static Tool getToolByItemId(int itemId) {
        return TOOLS_BY_ITEM_ID.get(itemId);
    }

    public static SpotType getSpotByDefinitionId(int definitionId) {
        return SPOTS_BY_DEFINITION_ID.get(definitionId);
    }
}
