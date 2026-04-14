package com.osrs.shared;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class SmithingRegistry {

    public enum StationType {
        FURNACE,
        ANVIL
    }

    public enum ProductCategory {
        TOOL,
        WEAPON,
        ARMOUR
    }

    public record BarTier(
        int itemId,
        String name,
        int levelRequirement,
        int xpTenths,
        int oreItemIdA,
        int oreQtyA,
        int oreItemIdB,
        int oreQtyB,
        int coalRequired,
        int successPercent
    ) {}

    public record ProductTier(
        int itemId,
        String name,
        int levelRequirement,
        int barItemId,
        int barsRequired,
        ProductCategory category
    ) {}

    public static final int FURNACE_DEFINITION_ID = 500;
    public static final int ANVIL_DEFINITION_ID = 501;
    public static final int HAMMER_ITEM_ID = 2347;

    private static final BarTier BRONZE_BAR = new BarTier(
        2349, "Bronze bar", 1, 62,
        436, 1,
        438, 1,
        0, 100
    );

    private static final BarTier IRON_BAR = new BarTier(
        2351, "Iron bar", 15, 125,
        440, 1,
        0, 0,
        0, 50
    );

    private static final BarTier SILVER_BAR = new BarTier(
        2355, "Silver bar", 20, 137,
        442, 1,
        0, 0,
        0, 100
    );

    private static final BarTier STEEL_BAR = new BarTier(
        2353, "Steel bar", 30, 175,
        440, 1,
        0, 0,
        2, 100
    );

    private static final BarTier GOLD_BAR = new BarTier(
        2357, "Gold bar", 40, 225,
        444, 1,
        0, 0,
        0, 100
    );

    private static final BarTier MITHRIL_BAR = new BarTier(
        2359, "Mithril bar", 50, 300,
        447, 1,
        0, 0,
        4, 100
    );

    private static final BarTier ADAMANTITE_BAR = new BarTier(
        2361, "Adamantite bar", 70, 375,
        449, 1,
        0, 0,
        6, 100
    );

    private static final BarTier RUNITE_BAR = new BarTier(
        2363, "Runite bar", 85, 500,
        451, 1,
        0, 0,
        8, 100
    );

    private static final List<BarTier> BARS = List.of(
        BRONZE_BAR,
        IRON_BAR,
        SILVER_BAR,
        STEEL_BAR,
        GOLD_BAR,
        MITHRIL_BAR,
        ADAMANTITE_BAR,
        RUNITE_BAR
    );

    private static final Map<Integer, BarTier> BARS_BY_ITEM_ID = Map.of(
        BRONZE_BAR.itemId(), BRONZE_BAR,
        IRON_BAR.itemId(), IRON_BAR,
        SILVER_BAR.itemId(), SILVER_BAR,
        STEEL_BAR.itemId(), STEEL_BAR,
        GOLD_BAR.itemId(), GOLD_BAR,
        MITHRIL_BAR.itemId(), MITHRIL_BAR,
        ADAMANTITE_BAR.itemId(), ADAMANTITE_BAR,
        RUNITE_BAR.itemId(), RUNITE_BAR
    );

    private static final List<ProductTier> PRODUCTS = List.of(
        new ProductTier(1351, "Bronze axe", 1, 2349, 1, ProductCategory.TOOL),
        new ProductTier(1349, "Iron axe", 16, 2351, 1, ProductCategory.TOOL),
        new ProductTier(1353, "Steel axe", 31, 2353, 1, ProductCategory.TOOL),
        new ProductTier(1355, "Mithril axe", 51, 2359, 1, ProductCategory.TOOL),
        new ProductTier(1357, "Adamant axe", 71, 2361, 1, ProductCategory.TOOL),
        new ProductTier(1359, "Rune axe", 86, 2363, 1, ProductCategory.TOOL),

        new ProductTier(1205, "Bronze dagger", 1, 2349, 1, ProductCategory.WEAPON),
        new ProductTier(1203, "Iron dagger", 15, 2351, 1, ProductCategory.WEAPON),
        new ProductTier(1207, "Steel dagger", 30, 2353, 1, ProductCategory.WEAPON),
        new ProductTier(1209, "Mithril dagger", 50, 2359, 1, ProductCategory.WEAPON),
        new ProductTier(1211, "Adamant dagger", 70, 2361, 1, ProductCategory.WEAPON),
        new ProductTier(1213, "Rune dagger", 85, 2363, 1, ProductCategory.WEAPON),

        new ProductTier(1422, "Bronze mace", 4, 2349, 1, ProductCategory.WEAPON),
        new ProductTier(1420, "Iron mace", 19, 2351, 1, ProductCategory.WEAPON),
        new ProductTier(1424, "Steel mace", 34, 2353, 1, ProductCategory.WEAPON),
        new ProductTier(1428, "Mithril mace", 54, 2359, 1, ProductCategory.WEAPON),
        new ProductTier(1430, "Adamant mace", 74, 2361, 1, ProductCategory.WEAPON),
        new ProductTier(1432, "Rune mace", 89, 2363, 1, ProductCategory.WEAPON),

        new ProductTier(1321, "Bronze scimitar", 5, 2349, 2, ProductCategory.WEAPON),
        new ProductTier(1323, "Iron scimitar", 20, 2351, 2, ProductCategory.WEAPON),
        new ProductTier(1325, "Steel scimitar", 35, 2353, 2, ProductCategory.WEAPON),
        new ProductTier(1329, "Mithril scimitar", 55, 2359, 2, ProductCategory.WEAPON),
        new ProductTier(1331, "Adamant scimitar", 75, 2361, 2, ProductCategory.WEAPON),
        new ProductTier(1333, "Rune scimitar", 90, 2363, 2, ProductCategory.WEAPON),

        new ProductTier(1139, "Bronze med helm", 3, 2349, 1, ProductCategory.ARMOUR),
        new ProductTier(1137, "Iron med helm", 18, 2351, 1, ProductCategory.ARMOUR),
        new ProductTier(1141, "Steel med helm", 33, 2353, 1, ProductCategory.ARMOUR),
        new ProductTier(1143, "Mithril med helm", 53, 2359, 1, ProductCategory.ARMOUR),
        new ProductTier(1145, "Adamant med helm", 73, 2361, 1, ProductCategory.ARMOUR),
        new ProductTier(1147, "Rune med helm", 88, 2363, 1, ProductCategory.ARMOUR),

        new ProductTier(1115, "Bronze full helm", 7, 2349, 2, ProductCategory.ARMOUR),
        new ProductTier(1153, "Iron full helm", 22, 2351, 2, ProductCategory.ARMOUR),
        new ProductTier(1157, "Steel full helm", 37, 2353, 2, ProductCategory.ARMOUR),
        new ProductTier(1163, "Mithril full helm", 57, 2359, 2, ProductCategory.ARMOUR),
        new ProductTier(1161, "Adamant full helm", 77, 2361, 2, ProductCategory.ARMOUR),
        new ProductTier(1165, "Rune full helm", 92, 2363, 2, ProductCategory.ARMOUR),

        new ProductTier(1103, "Bronze chainbody", 11, 2349, 3, ProductCategory.ARMOUR),
        new ProductTier(1101, "Iron chainbody", 26, 2351, 3, ProductCategory.ARMOUR),
        new ProductTier(1105, "Steel chainbody", 41, 2353, 3, ProductCategory.ARMOUR),
        new ProductTier(1109, "Mithril chainbody", 61, 2359, 3, ProductCategory.ARMOUR),
        new ProductTier(1111, "Adamant chainbody", 81, 2361, 3, ProductCategory.ARMOUR),
        new ProductTier(1113, "Rune chainbody", 96, 2363, 3, ProductCategory.ARMOUR),

        new ProductTier(1175, "Bronze sq shield", 8, 2349, 2, ProductCategory.ARMOUR),
        new ProductTier(1177, "Iron sq shield", 23, 2351, 2, ProductCategory.ARMOUR),
        new ProductTier(1193, "Steel sq shield", 38, 2353, 2, ProductCategory.ARMOUR),
        new ProductTier(1197, "Mithril sq shield", 58, 2359, 2, ProductCategory.ARMOUR),
        new ProductTier(1199, "Adamant sq shield", 78, 2361, 2, ProductCategory.ARMOUR),
        new ProductTier(1185, "Rune sq shield", 93, 2363, 2, ProductCategory.ARMOUR),

        new ProductTier(1067, "Bronze platelegs", 16, 2349, 3, ProductCategory.ARMOUR),
        new ProductTier(1069, "Iron platelegs", 31, 2351, 3, ProductCategory.ARMOUR),
        new ProductTier(1071, "Steel platelegs", 46, 2353, 3, ProductCategory.ARMOUR),
        new ProductTier(1075, "Mithril platelegs", 66, 2359, 3, ProductCategory.ARMOUR),
        new ProductTier(1077, "Adamant platelegs", 86, 2361, 3, ProductCategory.ARMOUR),
        new ProductTier(1079, "Rune platelegs", 99, 2363, 3, ProductCategory.ARMOUR),

        new ProductTier(1119, "Bronze platebody", 18, 2349, 5, ProductCategory.ARMOUR),
        new ProductTier(2000, "Iron platebody", 33, 2351, 5, ProductCategory.ARMOUR),
        new ProductTier(1085, "Steel platebody", 48, 2353, 5, ProductCategory.ARMOUR),
        new ProductTier(1129, "Mithril platebody", 68, 2359, 5, ProductCategory.ARMOUR),
        new ProductTier(1133, "Adamant platebody", 88, 2361, 5, ProductCategory.ARMOUR),
        new ProductTier(1127, "Rune platebody", 99, 2363, 5, ProductCategory.ARMOUR)
    );

    private static final Map<Integer, ProductTier> PRODUCTS_BY_ITEM_ID = PRODUCTS.stream()
        .collect(Collectors.toUnmodifiableMap(ProductTier::itemId, Function.identity()));

    private SmithingRegistry() {}

    public static List<BarTier> bars() {
        return BARS;
    }

    public static List<ProductTier> products() {
        return PRODUCTS;
    }

    public static BarTier getBarByItemId(int itemId) {
        return BARS_BY_ITEM_ID.get(itemId);
    }

    public static ProductTier getProductByItemId(int itemId) {
        return PRODUCTS_BY_ITEM_ID.get(itemId);
    }

    public static int smithingXpTenths(ProductTier product) {
        if (product == null) {
            return 0;
        }
        BarTier bar = getBarByItemId(product.barItemId());
        return bar == null ? 0 : bar.xpTenths() * product.barsRequired();
    }
}
