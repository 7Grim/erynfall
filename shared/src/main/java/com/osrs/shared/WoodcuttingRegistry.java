package com.osrs.shared;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shared pre-Forestry Woodcutting progression data.
 *
 * This is intentionally the single source of truth for:
 * - standard axe progression
 * - standard tree progression
 * - pre-Forestry depletion behaviour
 * - exact OSRS cut-chance low/high constants for standard trees
 *
 * XP is stored as fixed-point tenths so values like 37.5 and 67.5 remain exact
 * without introducing floating-point drift into later server/client prompts.
 */
public final class WoodcuttingRegistry {

    /**
     * One Woodcutting roll is attempted every 4 OSRS ticks.
     */
    public static final int CHOP_ATTEMPT_OSRS_TICKS = 4;

    public enum DepletionType {
        SINGLE_LOG,
        CHANCE_PER_SUCCESS
    }

    public record AxeTier(int itemId, String name, int woodcuttingLevel, int attackLevelToEquip) {}

    public record SuccessRate(int axeItemId, int low, int high) {}

    public record TreeTier(
        int definitionId,
        String name,
        int logItemId,
        int levelRequirement,
        int xpTenths,
        DepletionType depletionType,
        int depletionChanceDenominator,
        int respawnMinOsrsTicks,
        int respawnMaxOsrsTicks,
        List<SuccessRate> successRates
    ) {}

    private static final AxeTier BRONZE_AXE = new AxeTier(1351, "Bronze axe", 1, 1);
    private static final AxeTier IRON_AXE = new AxeTier(1349, "Iron axe", 1, 1);
    private static final AxeTier STEEL_AXE = new AxeTier(1353, "Steel axe", 6, 5);
    private static final AxeTier BLACK_AXE = new AxeTier(1361, "Black axe", 11, 10);
    private static final AxeTier MITHRIL_AXE = new AxeTier(1355, "Mithril axe", 21, 20);
    private static final AxeTier ADAMANT_AXE = new AxeTier(1357, "Adamant axe", 31, 30);
    private static final AxeTier RUNE_AXE = new AxeTier(1359, "Rune axe", 41, 40);
    private static final AxeTier DRAGON_AXE = new AxeTier(6739, "Dragon axe", 61, 60);

    private static final List<AxeTier> AXE_TIERS = List.of(
        BRONZE_AXE,
        IRON_AXE,
        STEEL_AXE,
        BLACK_AXE,
        MITHRIL_AXE,
        ADAMANT_AXE,
        RUNE_AXE,
        DRAGON_AXE
    );

    private static final Map<Integer, AxeTier> AXES_BY_ITEM_ID = Map.of(
        BRONZE_AXE.itemId(), BRONZE_AXE,
        IRON_AXE.itemId(), IRON_AXE,
        STEEL_AXE.itemId(), STEEL_AXE,
        BLACK_AXE.itemId(), BLACK_AXE,
        MITHRIL_AXE.itemId(), MITHRIL_AXE,
        ADAMANT_AXE.itemId(), ADAMANT_AXE,
        RUNE_AXE.itemId(), RUNE_AXE,
        DRAGON_AXE.itemId(), DRAGON_AXE
    );

    // Definition IDs intentionally preserve existing live mappings where possible:
    // 101-105 already back oak/willow/maple/yew/magic content in the current repo.
    // Standard tree is added at 100, with teak/mahogany appended at 106/107.
    private static final TreeTier TREE = new TreeTier(
        100,
        "Tree",
        1511,
        1,
        250,
        DepletionType.SINGLE_LOG,
        1,
        60,
        100,
        List.of(
            new SuccessRate(1351, 64, 200),
            new SuccessRate(1349, 96, 300),
            new SuccessRate(1353, 128, 400),
            new SuccessRate(1361, 144, 450),
            new SuccessRate(1355, 160, 500),
            new SuccessRate(1357, 192, 600),
            new SuccessRate(1359, 224, 700),
            new SuccessRate(6739, 240, 750)
        )
    );

    private static final TreeTier OAK_TREE = new TreeTier(
        101,
        "Oak Tree",
        1521,
        15,
        375,
        DepletionType.CHANCE_PER_SUCCESS,
        8,
        14,
        14,
        List.of(
            new SuccessRate(1351, 32, 100),
            new SuccessRate(1349, 48, 150),
            new SuccessRate(1353, 64, 200),
            new SuccessRate(1361, 72, 225),
            new SuccessRate(1355, 80, 250),
            new SuccessRate(1357, 96, 300),
            new SuccessRate(1359, 112, 350),
            new SuccessRate(6739, 120, 375)
        )
    );

    private static final TreeTier WILLOW_TREE = new TreeTier(
        102,
        "Willow Tree",
        1522,
        30,
        675,
        DepletionType.CHANCE_PER_SUCCESS,
        8,
        14,
        14,
        List.of(
            new SuccessRate(1351, 16, 50),
            new SuccessRate(1349, 24, 75),
            new SuccessRate(1353, 32, 100),
            new SuccessRate(1361, 36, 112),
            new SuccessRate(1355, 40, 125),
            new SuccessRate(1357, 48, 150),
            new SuccessRate(1359, 56, 175),
            new SuccessRate(6739, 60, 187)
        )
    );

    private static final TreeTier TEAK_TREE = new TreeTier(
        106,
        "Teak Tree",
        1526,
        35,
        850,
        DepletionType.CHANCE_PER_SUCCESS,
        8,
        15,
        15,
        List.of(
            new SuccessRate(1351, 15, 46),
            new SuccessRate(1349, 23, 70),
            new SuccessRate(1353, 31, 93),
            new SuccessRate(1361, 35, 102),
            new SuccessRate(1355, 39, 117),
            new SuccessRate(1357, 47, 140),
            new SuccessRate(1359, 55, 164),
            new SuccessRate(6739, 60, 190)
        )
    );

    private static final TreeTier MAPLE_TREE = new TreeTier(
        103,
        "Maple Tree",
        1523,
        45,
        1000,
        DepletionType.CHANCE_PER_SUCCESS,
        8,
        59,
        59,
        List.of(
            new SuccessRate(1351, 8, 25),
            new SuccessRate(1349, 12, 37),
            new SuccessRate(1353, 16, 50),
            new SuccessRate(1361, 18, 56),
            new SuccessRate(1355, 20, 62),
            new SuccessRate(1357, 24, 75),
            new SuccessRate(1359, 28, 87),
            new SuccessRate(6739, 30, 93)
        )
    );

    private static final TreeTier MAHOGANY_TREE = new TreeTier(
        107,
        "Mahogany Tree",
        1527,
        50,
        1250,
        DepletionType.CHANCE_PER_SUCCESS,
        8,
        14,
        14,
        List.of(
            new SuccessRate(1351, 8, 25),
            new SuccessRate(1349, 12, 38),
            new SuccessRate(1353, 16, 50),
            new SuccessRate(1361, 18, 54),
            new SuccessRate(1355, 20, 63),
            new SuccessRate(1357, 25, 75),
            new SuccessRate(1359, 29, 88),
            new SuccessRate(6739, 34, 94)
        )
    );

    private static final TreeTier YEW_TREE = new TreeTier(
        104,
        "Yew Tree",
        1524,
        60,
        1750,
        DepletionType.CHANCE_PER_SUCCESS,
        8,
        99,
        99,
        List.of(
            new SuccessRate(1351, 4, 12),
            new SuccessRate(1349, 6, 19),
            new SuccessRate(1353, 8, 25),
            new SuccessRate(1361, 9, 28),
            new SuccessRate(1355, 10, 31),
            new SuccessRate(1357, 12, 37),
            new SuccessRate(1359, 14, 44),
            new SuccessRate(6739, 15, 47)
        )
    );

    private static final TreeTier MAGIC_TREE = new TreeTier(
        105,
        "Magic Tree",
        1525,
        75,
        2500,
        DepletionType.CHANCE_PER_SUCCESS,
        8,
        199,
        199,
        List.of(
            new SuccessRate(1351, 2, 6),
            new SuccessRate(1349, 3, 9),
            new SuccessRate(1353, 4, 12),
            new SuccessRate(1361, 5, 13),
            new SuccessRate(1355, 5, 15),
            new SuccessRate(1357, 6, 18),
            new SuccessRate(1359, 7, 21),
            new SuccessRate(6739, 7, 22)
        )
    );

    private static final List<TreeTier> TREE_TIERS = List.of(
        TREE,
        OAK_TREE,
        WILLOW_TREE,
        TEAK_TREE,
        MAPLE_TREE,
        MAHOGANY_TREE,
        YEW_TREE,
        MAGIC_TREE
    );

    private static final Map<Integer, TreeTier> TREES_BY_DEFINITION_ID = Map.of(
        TREE.definitionId(), TREE,
        OAK_TREE.definitionId(), OAK_TREE,
        WILLOW_TREE.definitionId(), WILLOW_TREE,
        TEAK_TREE.definitionId(), TEAK_TREE,
        MAPLE_TREE.definitionId(), MAPLE_TREE,
        MAHOGANY_TREE.definitionId(), MAHOGANY_TREE,
        YEW_TREE.definitionId(), YEW_TREE,
        MAGIC_TREE.definitionId(), MAGIC_TREE
    );

    private static final Map<String, TreeTier> TREES_BY_NAME = Map.ofEntries(
        Map.entry(normalize("tree"), TREE),
        Map.entry(normalize("normal tree"), TREE),
        Map.entry(normalize("oak"), OAK_TREE),
        Map.entry(normalize("oak tree"), OAK_TREE),
        Map.entry(normalize("willow"), WILLOW_TREE),
        Map.entry(normalize("willow tree"), WILLOW_TREE),
        Map.entry(normalize("teak"), TEAK_TREE),
        Map.entry(normalize("teak tree"), TEAK_TREE),
        Map.entry(normalize("maple"), MAPLE_TREE),
        Map.entry(normalize("maple tree"), MAPLE_TREE),
        Map.entry(normalize("mahogany"), MAHOGANY_TREE),
        Map.entry(normalize("mahogany tree"), MAHOGANY_TREE),
        Map.entry(normalize("yew"), YEW_TREE),
        Map.entry(normalize("yew tree"), YEW_TREE),
        Map.entry(normalize("magic"), MAGIC_TREE),
        Map.entry(normalize("magic tree"), MAGIC_TREE)
    );

    private WoodcuttingRegistry() {}

    public static List<AxeTier> axes() {
        return AXE_TIERS;
    }

    public static List<TreeTier> trees() {
        return TREE_TIERS;
    }

    public static AxeTier getAxeByItemId(int itemId) {
        return AXES_BY_ITEM_ID.get(itemId);
    }

    public static TreeTier getTreeByDefinitionId(int definitionId) {
        return TREES_BY_DEFINITION_ID.get(definitionId);
    }

    public static TreeTier getTreeByName(String name) {
        return TREES_BY_NAME.get(normalize(name));
    }

    public static SuccessRate getSuccessRate(int treeDefinitionId, int axeItemId) {
        TreeTier tree = getTreeByDefinitionId(treeDefinitionId);
        if (tree == null) return null;
        for (SuccessRate rate : tree.successRates()) {
            if (rate.axeItemId() == axeItemId) {
                return rate;
            }
        }
        return null;
    }

    private static String normalize(String name) {
        if (name == null) return "";
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
