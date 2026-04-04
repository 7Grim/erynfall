package com.osrs.shared;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shared pre-Falling-Stars Mining progression data (post-2019 rework).
 *
 * This is intentionally the single source of truth for:
 * - standard pickaxe progression
 * - standard rock progression
 * - depletion behaviour per rock type
 * - exact OSRS mine-chance low/high constants for standard rocks
 *
 * XP is stored as fixed-point tenths so values like 17.5 and 35.0 remain exact
 * without introducing floating-point drift into later server/client prompts.
 */
public final class MiningRegistry {

    /**
     * One Mining roll is attempted every 3 OSRS ticks.
     */
    public static final int MINE_ATTEMPT_OSRS_TICKS = 3;

    public enum DepletionType {
        SINGLE_ORE,
        CHANCE_PER_SUCCESS
    }

    public record PickaxeTier(int itemId, String name, int miningLevel, int attackLevelToEquip) {}

    public record SuccessRate(int pickaxeItemId, int low, int high) {}

    public record RockTier(
        int definitionId,
        String name,
        int oreItemId,
        int levelRequirement,
        int xpTenths,
        DepletionType depletionType,
        int depletionChanceDenominator,
        int respawnMinOsrsTicks,
        int respawnMaxOsrsTicks,
        List<SuccessRate> successRates
    ) {}

    private static final PickaxeTier BRONZE_PICKAXE  = new PickaxeTier(1265,  "Bronze pickaxe",  1,  1);
    private static final PickaxeTier IRON_PICKAXE    = new PickaxeTier(1267,  "Iron pickaxe",    1,  1);
    private static final PickaxeTier STEEL_PICKAXE   = new PickaxeTier(1269,  "Steel pickaxe",   6,  5);
    private static final PickaxeTier BLACK_PICKAXE   = new PickaxeTier(12297, "Black pickaxe",  11, 10);
    private static final PickaxeTier MITHRIL_PICKAXE = new PickaxeTier(1273,  "Mithril pickaxe", 21, 20);
    private static final PickaxeTier ADAMANT_PICKAXE = new PickaxeTier(1271,  "Adamant pickaxe", 31, 30);
    private static final PickaxeTier RUNE_PICKAXE    = new PickaxeTier(1275,  "Rune pickaxe",   41, 40);
    private static final PickaxeTier DRAGON_PICKAXE  = new PickaxeTier(11920, "Dragon pickaxe", 61, 60);

    private static final List<PickaxeTier> PICKAXE_TIERS = List.of(
        BRONZE_PICKAXE,
        IRON_PICKAXE,
        STEEL_PICKAXE,
        BLACK_PICKAXE,
        MITHRIL_PICKAXE,
        ADAMANT_PICKAXE,
        RUNE_PICKAXE,
        DRAGON_PICKAXE
    );

    private static final Map<Integer, PickaxeTier> PICKAXES_BY_ITEM_ID = Map.of(
        BRONZE_PICKAXE.itemId(),  BRONZE_PICKAXE,
        IRON_PICKAXE.itemId(),    IRON_PICKAXE,
        STEEL_PICKAXE.itemId(),   STEEL_PICKAXE,
        BLACK_PICKAXE.itemId(),   BLACK_PICKAXE,
        MITHRIL_PICKAXE.itemId(), MITHRIL_PICKAXE,
        ADAMANT_PICKAXE.itemId(), ADAMANT_PICKAXE,
        RUNE_PICKAXE.itemId(),    RUNE_PICKAXE,
        DRAGON_PICKAXE.itemId(),  DRAGON_PICKAXE
    );

    // ──────────────────────────────────────────────────────────────────────────
    // Rock definitions  (definitionId 300–308)
    // ──────────────────────────────────────────────────────────────────────────

    private static final RockTier COPPER_ROCK = new RockTier(
        300,
        "Copper Rock",
        436,
        1,
        175,
        DepletionType.SINGLE_ORE,
        1,
        6, 6,
        List.of(
            new SuccessRate(1265,   32, 160),
            new SuccessRate(1267,   40, 185),
            new SuccessRate(1269,   52, 200),
            new SuccessRate(12297,  58, 212),
            new SuccessRate(1273,   65, 224),
            new SuccessRate(1271,   75, 236),
            new SuccessRate(1275,   88, 245),
            new SuccessRate(11920,  96, 250)
        )
    );

    private static final RockTier TIN_ROCK = new RockTier(
        301,
        "Tin Rock",
        438,
        1,
        175,
        DepletionType.SINGLE_ORE,
        1,
        6, 6,
        List.of(
            new SuccessRate(1265,   32, 160),
            new SuccessRate(1267,   40, 185),
            new SuccessRate(1269,   52, 200),
            new SuccessRate(12297,  58, 212),
            new SuccessRate(1273,   65, 224),
            new SuccessRate(1271,   75, 236),
            new SuccessRate(1275,   88, 245),
            new SuccessRate(11920,  96, 250)
        )
    );

    private static final RockTier IRON_ROCK = new RockTier(
        302,
        "Iron Rock",
        440,
        15,
        350,
        DepletionType.CHANCE_PER_SUCCESS,
        3,
        9, 9,
        List.of(
            new SuccessRate(1265,   20,  88),
            new SuccessRate(1267,   26, 110),
            new SuccessRate(1269,   34, 136),
            new SuccessRate(12297,  38, 148),
            new SuccessRate(1273,   42, 162),
            new SuccessRate(1271,   50, 176),
            new SuccessRate(1275,   60, 192),
            new SuccessRate(11920,  66, 200)
        )
    );

    private static final RockTier SILVER_ROCK = new RockTier(
        303,
        "Silver Rock",
        442,
        20,
        400,
        DepletionType.SINGLE_ORE,
        1,
        100, 100,
        List.of(
            new SuccessRate(1265,   18,  80),
            new SuccessRate(1267,   23, 100),
            new SuccessRate(1269,   30, 124),
            new SuccessRate(12297,  34, 136),
            new SuccessRate(1273,   38, 148),
            new SuccessRate(1271,   45, 162),
            new SuccessRate(1275,   55, 176),
            new SuccessRate(11920,  60, 185)
        )
    );

    private static final RockTier COAL_ROCK = new RockTier(
        304,
        "Coal Rock",
        453,
        30,
        500,
        DepletionType.CHANCE_PER_SUCCESS,
        5,
        50, 50,
        List.of(
            new SuccessRate(1265,   14,  64),
            new SuccessRate(1267,   18,  82),
            new SuccessRate(1269,   24, 100),
            new SuccessRate(12297,  27, 110),
            new SuccessRate(1273,   30, 120),
            new SuccessRate(1271,   36, 132),
            new SuccessRate(1275,   44, 146),
            new SuccessRate(11920,  48, 154)
        )
    );

    private static final RockTier GOLD_ROCK = new RockTier(
        305,
        "Gold Rock",
        444,
        40,
        650,
        DepletionType.SINGLE_ORE,
        1,
        100, 100,
        List.of(
            new SuccessRate(1265,   10,  50),
            new SuccessRate(1267,   13,  64),
            new SuccessRate(1269,   17,  80),
            new SuccessRate(12297,  20,  88),
            new SuccessRate(1273,   22,  96),
            new SuccessRate(1271,   27, 108),
            new SuccessRate(1275,   33, 120),
            new SuccessRate(11920,  37, 128)
        )
    );

    private static final RockTier MITHRIL_ROCK = new RockTier(
        306,
        "Mithril Rock",
        447,
        55,
        800,
        DepletionType.CHANCE_PER_SUCCESS,
        2,
        150, 150,
        List.of(
            new SuccessRate(1265,    6,  32),
            new SuccessRate(1267,    8,  42),
            new SuccessRate(1269,   11,  54),
            new SuccessRate(12297,  12,  60),
            new SuccessRate(1273,   14,  66),
            new SuccessRate(1271,   17,  74),
            new SuccessRate(1275,   21,  84),
            new SuccessRate(11920,  23,  90)
        )
    );

    private static final RockTier ADAMANTITE_ROCK = new RockTier(
        307,
        "Adamantite Rock",
        449,
        70,
        950,
        DepletionType.CHANCE_PER_SUCCESS,
        2,
        250, 250,
        List.of(
            new SuccessRate(1265,    3,  18),
            new SuccessRate(1267,    4,  24),
            new SuccessRate(1269,    6,  32),
            new SuccessRate(12297,   7,  36),
            new SuccessRate(1273,    8,  40),
            new SuccessRate(1271,   10,  48),
            new SuccessRate(1275,   12,  56),
            new SuccessRate(11920,  14,  62)
        )
    );

    private static final RockTier RUNITE_ROCK = new RockTier(
        308,
        "Runite Rock",
        451,
        85,
        1250,
        DepletionType.SINGLE_ORE,
        1,
        500, 500,
        List.of(
            new SuccessRate(1265,    1,   8),
            new SuccessRate(1267,    2,  11),
            new SuccessRate(1269,    3,  16),
            new SuccessRate(12297,   3,  18),
            new SuccessRate(1273,    4,  20),
            new SuccessRate(1271,    5,  24),
            new SuccessRate(1275,    6,  30),
            new SuccessRate(11920,   7,  34)
        )
    );

    private static final List<RockTier> ROCK_TIERS = List.of(
        COPPER_ROCK,
        TIN_ROCK,
        IRON_ROCK,
        SILVER_ROCK,
        COAL_ROCK,
        GOLD_ROCK,
        MITHRIL_ROCK,
        ADAMANTITE_ROCK,
        RUNITE_ROCK
    );

    private static final Map<Integer, RockTier> ROCKS_BY_DEFINITION_ID = Map.of(
        COPPER_ROCK.definitionId(),    COPPER_ROCK,
        TIN_ROCK.definitionId(),       TIN_ROCK,
        IRON_ROCK.definitionId(),      IRON_ROCK,
        SILVER_ROCK.definitionId(),    SILVER_ROCK,
        COAL_ROCK.definitionId(),      COAL_ROCK,
        GOLD_ROCK.definitionId(),      GOLD_ROCK,
        MITHRIL_ROCK.definitionId(),   MITHRIL_ROCK,
        ADAMANTITE_ROCK.definitionId(), ADAMANTITE_ROCK,
        RUNITE_ROCK.definitionId(),    RUNITE_ROCK
    );

    private static final Map<String, RockTier> ROCKS_BY_NAME = Map.ofEntries(
        Map.entry(normalize("copper rock"),    COPPER_ROCK),
        Map.entry(normalize("copper"),         COPPER_ROCK),
        Map.entry(normalize("tin rock"),       TIN_ROCK),
        Map.entry(normalize("tin"),            TIN_ROCK),
        Map.entry(normalize("iron rock"),      IRON_ROCK),
        Map.entry(normalize("iron"),           IRON_ROCK),
        Map.entry(normalize("silver rock"),    SILVER_ROCK),
        Map.entry(normalize("silver"),         SILVER_ROCK),
        Map.entry(normalize("coal rock"),      COAL_ROCK),
        Map.entry(normalize("coal"),           COAL_ROCK),
        Map.entry(normalize("gold rock"),      GOLD_ROCK),
        Map.entry(normalize("gold"),           GOLD_ROCK),
        Map.entry(normalize("mithril rock"),   MITHRIL_ROCK),
        Map.entry(normalize("mithril"),        MITHRIL_ROCK),
        Map.entry(normalize("adamantite rock"), ADAMANTITE_ROCK),
        Map.entry(normalize("adamantite"),     ADAMANTITE_ROCK),
        Map.entry(normalize("runite rock"),    RUNITE_ROCK),
        Map.entry(normalize("runite"),         RUNITE_ROCK)
    );

    private MiningRegistry() {}

    public static List<PickaxeTier> pickaxes() {
        return PICKAXE_TIERS;
    }

    public static List<RockTier> rocks() {
        return ROCK_TIERS;
    }

    public static PickaxeTier getPickaxeByItemId(int itemId) {
        return PICKAXES_BY_ITEM_ID.get(itemId);
    }

    public static RockTier getRockByDefinitionId(int definitionId) {
        return ROCKS_BY_DEFINITION_ID.get(definitionId);
    }

    public static RockTier getRockByName(String name) {
        return ROCKS_BY_NAME.get(normalize(name));
    }

    public static SuccessRate getSuccessRate(int rockDefinitionId, int pickaxeItemId) {
        RockTier rock = getRockByDefinitionId(rockDefinitionId);
        if (rock == null) return null;
        for (SuccessRate rate : rock.successRates()) {
            if (rate.pickaxeItemId() == pickaxeItemId) {
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
