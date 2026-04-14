package com.osrs.shared;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class CraftingRegistry {

    public static final int COWHIDE_ITEM_ID = 2142;
    public static final int LEATHER_ITEM_ID = 1739;
    public static final int NEEDLE_ITEM_ID = 1733;
    public static final int THREAD_ITEM_ID = 1734;

    public record LeatherRecipe(int productItemId,
                                String productName,
                                int levelRequirement,
                                int leatherRequired,
                                int xpTenths) {}

    private static final List<LeatherRecipe> LEATHER_RECIPES = List.of(
        new LeatherRecipe(1059, "Leather gloves", 1, 1, 138),
        new LeatherRecipe(1061, "Leather boots", 7, 1, 162),
        new LeatherRecipe(1167, "Leather cowl", 9, 1, 185),
        new LeatherRecipe(2129, "Leather body", 14, 3, 250),
        new LeatherRecipe(1095, "Leather chaps", 18, 2, 275)
    );

    private static final Map<Integer, LeatherRecipe> BY_PRODUCT_ITEM_ID =
        LEATHER_RECIPES.stream().collect(Collectors.toMap(LeatherRecipe::productItemId, r -> r));

    private CraftingRegistry() {
    }

    public static List<LeatherRecipe> leatherRecipes() {
        return LEATHER_RECIPES;
    }

    public static LeatherRecipe getLeatherRecipeByProduct(int productItemId) {
        return BY_PRODUCT_ITEM_ID.get(productItemId);
    }

    public static LeatherRecipe bestCraftableLeatherRecipe(int craftingLevel, int leatherCount) {
        if (leatherCount <= 0) {
            return null;
        }
        List<LeatherRecipe> unlocked = new ArrayList<>();
        for (LeatherRecipe recipe : LEATHER_RECIPES) {
            if (craftingLevel >= recipe.levelRequirement() && leatherCount >= recipe.leatherRequired()) {
                unlocked.add(recipe);
            }
        }
        if (unlocked.isEmpty()) {
            return null;
        }
        unlocked.sort(Comparator
            .comparingInt(LeatherRecipe::levelRequirement)
            .thenComparingInt(LeatherRecipe::leatherRequired));
        return unlocked.get(unlocked.size() - 1);
    }
}
