package com.osrs.client.ui;

/**
 * Maps skill index + level → what was unlocked at that level.
 *
 * Used by LevelUpOverlay to show the player what they can now do.
 * Sources: https://oldschool.runescape.wiki (skill requirement pages).
 *
 * Returns null when a level has no notable unlock so the overlay
 * can show a generic "keep training" message instead.
 */
public final class LevelUnlockRegistry {

    private LevelUnlockRegistry() {}

    /**
     * Text describing what was unlocked when the player JUST reached {@code level}
     * in the skill identified by {@code skillIdx} (Player.SKILL_* constants).
     * Returns null if this level has no specific unlock.
     */
    public static String getUnlockText(int skillIdx, int level) {
        return switch (skillIdx) {
            case 0  -> attackUnlock(level);
            case 1  -> strengthUnlock(level);
            case 2  -> defenceUnlock(level);
            case 3  -> hitpointsUnlock(level);
            case 4  -> rangedUnlock(level);
            case 5  -> magicUnlock(level);
            case 6  -> prayerUnlock(level);
            case 7  -> woodcuttingUnlock(level);
            case 8  -> fishingUnlock(level);
            case 9  -> cookingUnlock(level);
            case 10 -> miningUnlock(level);
            case 11 -> smithingUnlock(level);
            case 12 -> firemakingUnlock(level);
            case 13 -> craftingUnlock(level);
            case 14 -> runecraftingUnlock(level);
            case 15 -> fletchingUnlock(level);
            case 16 -> agilityUnlock(level);
            case 17 -> herbloreUnlock(level);
            case 18 -> thievingUnlock(level);
            case 19 -> slayerUnlock(level);
            case 20 -> farmingUnlock(level);
            case 21 -> hunterUnlock(level);
            case 22 -> constructionUnlock(level);
            default -> null;
        };
    }

    /**
     * Returns the next upcoming unlock ABOVE the given level, formatted as a hint.
     * e.g. "Next at level 15: Chop oak trees." Returns null at level 99.
     */
    public static String getNextMilestoneText(int skillIdx, int currentLevel) {
        for (int lvl = currentLevel + 1; lvl <= 99; lvl++) {
            String text = getUnlockText(skillIdx, lvl);
            if (text != null) {
                // Strip trailing period for the "Next at level N:" prefix
                String trimmed = text.endsWith(".") ? text.substring(0, text.length() - 1) : text;
                return "Next at level " + lvl + ": " + trimmed + ".";
            }
        }
        return null;
    }

    // -----------------------------------------------------------------------
    // Per-skill unlock tables (OSRS wiki sourced)
    // -----------------------------------------------------------------------

    private static String attackUnlock(int level) {
        return switch (level) {
            case 5  -> "You can now wield steel weapons, including the steel scimitar.";
            case 10 -> "You can now wield black weapons, including the black scimitar.";
            case 20 -> "You can now wield mithril weapons, including the mithril scimitar.";
            case 30 -> "You can now wield adamant weapons, including the adamant scimitar.";
            case 40 -> "You can now wield rune weapons, including the rune scimitar.";
            case 60 -> "You can now wield dragon weapons, including the dragon scimitar and longsword.";
            case 70 -> "You can now wield barrows weapons such as the abyssal whip.";
            case 99 -> "You have reached the maximum Attack level!";
            default -> null;
        };
    }

    private static String strengthUnlock(int level) {
        return switch (level) {
            case 10 -> "Your max hit is improving — train on goblins and cows.";
            case 20 -> "Your melee damage is noticeably stronger.";
            case 30 -> "Your attacks are hitting harder. Consider upgrading to mithril weapons.";
            case 40 -> "You can now wield a barrelchest anchor (Attack 40 also required).";
            case 50 -> "You can now wield a granite maul (Attack 50 also required).";
            case 60 -> "You can now wield a granite 2h sword (Strength 60 required).";
            case 70 -> "Your max hit with a rune scimitar reaches 16. Major melee milestone.";
            case 80 -> "Your max hit with a rune scimitar reaches 18. Consider Slayer training.";
            case 90 -> "Your max hit with a rune scimitar reaches 20. Near-elite damage output.";
            case 99 -> "You have reached the maximum Strength level! Max hit: 22 with rune scimitar.";
            default -> null;
        };
    }

    private static String defenceUnlock(int level) {
        return switch (level) {
            case 10 -> "You can now wear black armour.";
            case 20 -> "You can now wear mithril armour.";
            case 30 -> "You can now wear adamant armour.";
            case 40 -> "You can now wear rune armour.";
            case 45 -> "You can now wear Bandos armour.";
            case 60 -> "You can now wear dragon armour.";
            case 65 -> "You can now wear barrows armour.";
            case 70 -> "You can now wear armadyl armour.";
            case 99 -> "You have reached the maximum Defence level!";
            default -> null;
        };
    }

    private static String hitpointsUnlock(int level) {
        return switch (level) {
            case 10 -> "Starting Hitpoints level — train combat skills to raise this further.";
            case 99 -> "You have reached the maximum Hitpoints level!";
            default -> null;
        };
    }

    private static String rangedUnlock(int level) {
        return switch (level) {
            case 20 -> "You can now use an oak shortbow and adamant arrows.";
            case 30 -> "You can now use a willow bow and mithril arrows.";
            case 40 -> "You can now use a maple bow, adamant arrows, and adamant bolts.";
            case 50 -> "You can now use a yew bow and rune arrows.";
            case 60 -> "You can now use a magic shortbow and rune arrows.";
            case 61 -> "You can now use a magic longbow.";
            case 70 -> "You can now wield a rune crossbow.";
            case 75 -> "You can now wield a dragon crossbow.";
            case 80 -> "You can now wield an armadyl crossbow.";
            case 99 -> "You have reached the maximum Ranged level!";
            default -> null;
        };
    }

    private static String magicUnlock(int level) {
        return switch (level) {
            case 13 -> "You can now cast Wind Bolt.";
            case 17 -> "You can now cast Confuse.";
            case 19 -> "You can now cast Water Bolt.";
            case 25 -> "You can now cast Fire Bolt.";
            case 35 -> "You can now cast Fire Blast.";
            case 43 -> "You can now cast Superheat Item.";
            case 59 -> "You can now cast Fire Wave.";
            case 72 -> "You can now cast Fire Surge.";
            case 75 -> "You can now cast Ice Barrage (Ancient Magicks).";
            case 99 -> "You have reached the maximum Magic level!";
            default -> null;
        };
    }

    private static String prayerUnlock(int level) {
        return switch (level) {
            case 4  -> "You can now use Burst of Strength (+5% Strength bonus).";
            case 7  -> "You can now use Clarity of Thought (+5% Attack bonus).";
            case 10 -> "You can now use Rock Skin (+10% Defence bonus).";
            case 13 -> "You can now use Superhuman Strength (+10% Strength bonus).";
            case 16 -> "You can now use Improved Reflexes (+10% Attack bonus).";
            case 19 -> "You can now use Protect from Magic.";
            case 22 -> "You can now use Protect from Missiles.";
            case 25 -> "You can now use Protect from Melee.";
            case 31 -> "You can now use Smite.";
            case 37 -> "You can now use Eagle Eye (+15% Ranged bonus).";
            case 43 -> "You can now use Mystic Might (+15% Magic bonus).";
            case 44 -> "You can now use Retribution.";
            case 46 -> "You can now use Redemption.";
            case 52 -> "You can now use Rigour (+20% Ranged bonus).";
            case 55 -> "You can now use Augury (+25% Magic bonus).";
            case 70 -> "You can now use Piety (+20% Attack, Strength and Defence bonus).";
            case 99 -> "You have reached the maximum Prayer level!";
            default -> null;
        };
    }

    private static String woodcuttingUnlock(int level) {
        return switch (level) {
            case 6  -> "You can now use a steel axe.";
            case 11 -> "You can now use a black axe.";
            case 15 -> "You can now chop oak trees for oak logs.";
            case 21 -> "You can now use a mithril axe.";
            case 30 -> "You can now chop willow trees for willow logs.";
            case 31 -> "You can now use an adamant axe.";
            case 35 -> "You can now chop teak trees for teak logs.";
            case 41 -> "You can now use a rune axe.";
            case 45 -> "You can now chop maple trees for maple logs.";
            case 50 -> "You can now chop mahogany trees for mahogany logs.";
            case 60 -> "You can now chop yew trees for yew logs.";
            case 61 -> "You can now use a dragon axe.";
            case 75 -> "You can now chop magic trees for magic logs.";
            case 99 -> "You have reached the maximum Woodcutting level!";
            default -> null;
        };
    }

    private static String fishingUnlock(int level) {
        return switch (level) {
            case 5  -> "You can now use a fishing rod to catch sardines.";
            case 10 -> "You can now use a fly fishing rod.";
            case 16 -> "You can now catch herring.";
            case 20 -> "You can now catch pike.";
            case 25 -> "You can now use a big net.";
            case 35 -> "You can now catch salmon.";
            case 40 -> "You can now catch lobsters.";
            case 46 -> "You can now catch bass.";
            case 50 -> "You can now catch swordfish.";
            case 62 -> "You can now catch monkfish.";
            case 70 -> "You can now catch manta ray.";
            case 76 -> "You can now catch sharks.";
            case 82 -> "You can now catch anglerfish.";
            case 99 -> "You have reached the maximum Fishing level!";
            default -> null;
        };
    }

    private static String cookingUnlock(int level) {
        return switch (level) {
            case 7  -> "You can now cook sardines.";
            case 10 -> "You can now cook herring.";
            case 15 -> "You can now cook anchovies.";
            case 18 -> "You can now cook tuna.";
            case 20 -> "You can now cook pike.";
            case 25 -> "You can now bake bread.";
            case 28 -> "You can now cook trout.";
            case 30 -> "You can now cook lobster.";
            case 35 -> "You can now cook swordfish.";
            case 41 -> "You can now cook monkfish.";
            case 50 -> "You can now cook sea turtle.";
            case 62 -> "You can now cook manta ray.";
            case 80 -> "You can now cook sharks.";
            case 82 -> "You can now cook anglerfish.";
            case 99 -> "You have reached the maximum Cooking level!";
            default -> null;
        };
    }

    private static String miningUnlock(int level) {
        return switch (level) {
            case 15 -> "You can now mine iron ore.";
            case 20 -> "You can now mine silver ore.";
            case 30 -> "You can now mine coal.";
            case 40 -> "You can now mine gold ore.";
            case 55 -> "You can now mine mithril ore.";
            case 70 -> "You can now mine adamantite ore.";
            case 80 -> "You can now mine runite ore.";
            case 85 -> "You can now mine amethyst.";
            case 99 -> "You have reached the maximum Mining level!";
            default -> null;
        };
    }

    private static String smithingUnlock(int level) {
        return switch (level) {
            case 15 -> "You can now smelt iron bars.";
            case 20 -> "You can now smith iron equipment.";
            case 30 -> "You can now smelt steel bars.";
            case 40 -> "You can now smelt gold bars.";
            case 50 -> "You can now smelt mithril bars.";
            case 60 -> "You can now smelt adamantite bars.";
            case 70 -> "You can now smith rune equipment.";
            case 85 -> "You can now smelt runite bars.";
            case 99 -> "You have reached the maximum Smithing level!";
            default -> null;
        };
    }

    private static String firemakingUnlock(int level) {
        return switch (level) {
            case 15 -> "You can now light oak logs.";
            case 30 -> "You can now light willow logs.";
            case 35 -> "You can now light teak logs.";
            case 45 -> "You can now light maple logs.";
            case 50 -> "You can now light mahogany logs.";
            case 60 -> "You can now light yew logs.";
            case 75 -> "You can now light magic logs.";
            case 99 -> "You have reached the maximum Firemaking level!";
            default -> null;
        };
    }

    private static String craftingUnlock(int level) {
        return switch (level) {
            case 5  -> "You can now craft leather gloves.";
            case 9  -> "You can now craft leather vambraces.";
            case 11 -> "You can now craft a leather body.";
            case 14 -> "You can now craft leather chaps.";
            case 28 -> "You can now craft a hard leather body.";
            case 40 -> "You can now craft green dragonhide armour.";
            case 50 -> "You can now cut rubies.";
            case 55 -> "You can now craft blue dragonhide armour.";
            case 63 -> "You can now cut diamonds.";
            case 70 -> "You can now craft red dragonhide armour.";
            case 84 -> "You can now craft black dragonhide armour.";
            case 99 -> "You have reached the maximum Crafting level!";
            default -> null;
        };
    }

    private static String runecraftingUnlock(int level) {
        return switch (level) {
            case 2  -> "You can now craft Mind runes.";
            case 5  -> "You can now craft Water runes.";
            case 9  -> "You can now craft Earth runes.";
            case 14 -> "You can now craft Fire runes.";
            case 20 -> "You can now craft Body runes.";
            case 27 -> "You can now craft Cosmic runes.";
            case 35 -> "You can now craft Chaos runes.";
            case 44 -> "You can now craft Nature runes.";
            case 54 -> "You can now craft Law runes.";
            case 65 -> "You can now craft Death runes.";
            case 77 -> "You can now craft Blood runes.";
            case 91 -> "You can now craft Soul runes.";
            case 99 -> "You have reached the maximum Runecrafting level!";
            default -> null;
        };
    }

    private static String fletchingUnlock(int level) {
        return switch (level) {
            case 5  -> "You can now make arrow shafts.";
            case 10 -> "You can now make headless arrows.";
            case 20 -> "You can now make an oak shortbow.";
            case 25 -> "You can now make an oak longbow.";
            case 35 -> "You can now make a willow shortbow.";
            case 40 -> "You can now make a willow longbow.";
            case 50 -> "You can now make a maple shortbow.";
            case 55 -> "You can now make a maple longbow.";
            case 60 -> "You can now make a yew shortbow.";
            case 65 -> "You can now fletch broad bolts.";
            case 70 -> "You can now make a yew longbow.";
            case 80 -> "You can now make a magic shortbow.";
            case 85 -> "You can now make a magic longbow.";
            case 99 -> "You have reached the maximum Fletching level!";
            default -> null;
        };
    }

    private static String agilityUnlock(int level) {
        return switch (level) {
            case 10 -> "You can now use the Draynor Village Rooftop Course.";
            case 20 -> "You can now use the Al-Kharid Rooftop Course.";
            case 30 -> "You can now use the Varrock Rooftop Course.";
            case 40 -> "You can now use the Canifis Rooftop Course.";
            case 50 -> "You can now use the Falador Rooftop Course.";
            case 60 -> "You can now use the Seers' Village Rooftop Course.";
            case 70 -> "You can now use the Pollnivneach Rooftop Course.";
            case 80 -> "You can now use the Rellekka Rooftop Course.";
            case 90 -> "You can now use the Ardougne Rooftop Course.";
            case 99 -> "You have reached the maximum Agility level!";
            default -> null;
        };
    }

    private static String herbloreUnlock(int level) {
        return switch (level) {
            case 3  -> "You can now make Attack potions.";
            case 5  -> "You can now make Antipoison potions.";
            case 9  -> "You can now make Strength potions.";
            case 15 -> "You can now make Restore potions.";
            case 22 -> "You can now make Energy potions.";
            case 26 -> "You can now make Defence potions.";
            case 38 -> "You can now make Prayer potions.";
            case 45 -> "You can now make Super Attack potions.";
            case 50 -> "You can now make Fishing potions.";
            case 55 -> "You can now make Super Energy potions.";
            case 63 -> "You can now make Super Restore potions.";
            case 66 -> "You can now make Super Defence potions.";
            case 76 -> "You can now make Super Combat potions.";
            case 99 -> "You have reached the maximum Herblore level!";
            default -> null;
        };
    }

    private static String thievingUnlock(int level) {
        return switch (level) {
            case 5  -> "You can now pickpocket farmers.";
            case 10 -> "You can now pickpocket HAM members.";
            case 20 -> "You can now pickpocket warriors.";
            case 25 -> "You can now pickpocket rogues.";
            case 32 -> "You can now pickpocket master farmers.";
            case 38 -> "You can now pickpocket guards.";
            case 45 -> "You can now pickpocket knights.";
            case 55 -> "You can now pickpocket paladins.";
            case 65 -> "You can now pickpocket heroes.";
            case 75 -> "You can now pickpocket elves.";
            case 99 -> "You have reached the maximum Thieving level!";
            default -> null;
        };
    }

    private static String slayerUnlock(int level) {
        return switch (level) {
            case 7  -> "You can now slay Cave Crawlers.";
            case 10 -> "You can now slay Banshees.";
            case 15 -> "You can now slay Cockatrices.";
            case 20 -> "You can now slay Pyrefiends.";
            case 28 -> "You can now slay Rockslugs.";
            case 30 -> "You can now slay Jellies.";
            case 40 -> "You can now slay Turoth.";
            case 55 -> "You can now slay Gargoyles.";
            case 60 -> "You can now slay Nechryaels.";
            case 65 -> "You can now slay Dark Beasts.";
            case 75 -> "You can now slay Cave Horrors.";
            case 85 -> "You can now slay Abyssal Demons.";
            case 90 -> "You can now slay Dust Devils and Smoke Devils.";
            case 99 -> "You have reached the maximum Slayer level!";
            default -> null;
        };
    }

    private static String farmingUnlock(int level) {
        return switch (level) {
            case 5  -> "You can now plant potatoes.";
            case 7  -> "You can now plant onions.";
            case 9  -> "You can now plant cabbages.";
            case 15 -> "You can now grow sweetcorn.";
            case 20 -> "You can now grow strawberries.";
            case 26 -> "You can now grow watermelons.";
            case 32 -> "You can now grow oak trees.";
            case 35 -> "You can now grow willow trees.";
            case 45 -> "You can now grow maple trees.";
            case 60 -> "You can now grow yew trees.";
            case 75 -> "You can now grow magic trees.";
            case 99 -> "You have reached the maximum Farming level!";
            default -> null;
        };
    }

    private static String hunterUnlock(int level) {
        return switch (level) {
            case 9  -> "You can now catch cerulean twitches.";
            case 15 -> "You can now catch tropical wagtails.";
            case 19 -> "You can now catch swamp lizards.";
            case 27 -> "You can now catch orange salamanders.";
            case 43 -> "You can now catch red salamanders.";
            case 53 -> "You can now catch black salamanders.";
            case 60 -> "You can now catch black chinchompas.";
            case 63 -> "You can now catch red chinchompas.";
            case 99 -> "You have reached the maximum Hunter level!";
            default -> null;
        };
    }

    private static String constructionUnlock(int level) {
        return switch (level) {
            case 2  -> "You can now build basic furniture in your house.";
            case 5  -> "You can now build a bookcase.";
            case 10 -> "You can now build a workshop.";
            case 20 -> "You can now build a kitchen.";
            case 30 -> "You can now build a skill hall.";
            case 40 -> "You can now build a games room.";
            case 50 -> "You can now build a combat room.";
            case 60 -> "You can now build a quest hall.";
            case 70 -> "You can now build a menagerie.";
            case 80 -> "You can now build a formal garden.";
            case 90 -> "You can now build an oubliette.";
            case 99 -> "You have reached the maximum Construction level!";
            default -> null;
        };
    }
}
