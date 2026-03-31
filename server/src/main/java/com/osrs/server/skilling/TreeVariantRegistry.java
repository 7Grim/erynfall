package com.osrs.server.skilling;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;

/**
 * Central registry for choppable tree variants and their progression data.
 */
public final class TreeVariantRegistry {

    private static final TreeVariant OAK_TREE = new TreeVariant(1, "Oak Tree", 1521, 1, 25L);
    private static final TreeVariant WILLOW_TREE = new TreeVariant(2, "Willow Tree", 1522, 15, 37L);
    private static final TreeVariant MAPLE_TREE = new TreeVariant(3, "Maple Tree", 1523, 30, 67L);
    private static final TreeVariant YEW_TREE = new TreeVariant(4, "Yew Tree", 1524, 45, 100L);
    private static final TreeVariant MAGIC_TREE = new TreeVariant(5, "Magic Tree", 1525, 60, 150L);

    private static final Map<Integer, TreeVariant> BY_DEFINITION_ID = Map.of(
        OAK_TREE.definitionId(), OAK_TREE,
        WILLOW_TREE.definitionId(), WILLOW_TREE,
        MAPLE_TREE.definitionId(), MAPLE_TREE,
        YEW_TREE.definitionId(), YEW_TREE,
        MAGIC_TREE.definitionId(), MAGIC_TREE
    );

    private static final Map<String, TreeVariant> BY_NAME = Map.of(
        normalize(OAK_TREE.name()), OAK_TREE,
        normalize(WILLOW_TREE.name()), WILLOW_TREE,
        normalize(MAPLE_TREE.name()), MAPLE_TREE,
        normalize(YEW_TREE.name()), YEW_TREE,
        normalize(MAGIC_TREE.name()), MAGIC_TREE
    );

    private TreeVariantRegistry() {
    }

    public static TreeVariant getByDefinitionId(int definitionId) {
        return BY_DEFINITION_ID.get(definitionId);
    }

    public static TreeVariant getByName(String name) {
        return BY_NAME.get(normalize(name));
    }

    public static boolean isChoppableDefinitionId(int definitionId) {
        return BY_DEFINITION_ID.containsKey(definitionId);
    }

    public static Collection<TreeVariant> all() {
        return BY_DEFINITION_ID.values();
    }

    private static String normalize(String name) {
        if (name == null) return "";
        return name.trim().toLowerCase(Locale.ROOT);
    }

    public record TreeVariant(int definitionId, String name, int logItemId, int levelRequirement, long xp) {
    }
}
