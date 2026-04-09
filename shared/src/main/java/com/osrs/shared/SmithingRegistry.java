package com.osrs.shared;

import java.util.List;
import java.util.Map;

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
        new ProductTier(1321, "Bronze scimitar", 5, 2349, 2, ProductCategory.WEAPON),
        new ProductTier(1323, "Iron scimitar", 20, 2351, 2, ProductCategory.WEAPON),
        new ProductTier(1325, "Steel scimitar", 35, 2353, 2, ProductCategory.WEAPON),
        new ProductTier(1329, "Mithril scimitar", 55, 2359, 2, ProductCategory.WEAPON),
        new ProductTier(1331, "Adamant scimitar", 75, 2361, 2, ProductCategory.WEAPON),
        new ProductTier(1333, "Rune scimitar", 90, 2363, 2, ProductCategory.WEAPON),
        new ProductTier(1115, "Bronze full helm", 7, 2349, 2, ProductCategory.ARMOUR),
        new ProductTier(1153, "Iron full helm", 22, 2351, 2, ProductCategory.ARMOUR),
        new ProductTier(1157, "Steel full helm", 37, 2353, 2, ProductCategory.ARMOUR),
        new ProductTier(1163, "Mithril full helm", 57, 2359, 2, ProductCategory.ARMOUR),
        new ProductTier(1161, "Adamant full helm", 77, 2361, 2, ProductCategory.ARMOUR),
        new ProductTier(1165, "Rune full helm", 92, 2363, 2, ProductCategory.ARMOUR)
    );

    private static final Map<Integer, ProductTier> PRODUCTS_BY_ITEM_ID = Map.ofEntries(
        Map.entry(1351, PRODUCTS.get(0)),
        Map.entry(1349, PRODUCTS.get(1)),
        Map.entry(1353, PRODUCTS.get(2)),
        Map.entry(1355, PRODUCTS.get(3)),
        Map.entry(1357, PRODUCTS.get(4)),
        Map.entry(1359, PRODUCTS.get(5)),
        Map.entry(1321, PRODUCTS.get(6)),
        Map.entry(1323, PRODUCTS.get(7)),
        Map.entry(1325, PRODUCTS.get(8)),
        Map.entry(1329, PRODUCTS.get(9)),
        Map.entry(1331, PRODUCTS.get(10)),
        Map.entry(1333, PRODUCTS.get(11)),
        Map.entry(1115, PRODUCTS.get(12)),
        Map.entry(1153, PRODUCTS.get(13)),
        Map.entry(1157, PRODUCTS.get(14)),
        Map.entry(1163, PRODUCTS.get(15)),
        Map.entry(1161, PRODUCTS.get(16)),
        Map.entry(1165, PRODUCTS.get(17))
    );

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
