package com.osrs.client.audio;

/**
 * All music tracks in the game.
 *
 * File names map to OGG files under client/src/main/resources/audio/music/.
 * The game runs silently if a file is absent — see AudioManager's null-safe loader.
 *
 * Track names match OSRS originals where possible so composers know the target
 * mood and style for each area.
 */
public enum MusicTrack {

    // Login / title screen
    SCAPE_MAIN        ("scape_main",         "Scape Main"),

    // Tutorial Island — town / spawn area
    NEWBIE_MELODY     ("newbie_melody",       "Newbie Melody"),

    // Skill zones
    COOKING_WITH_FIRE ("cooking_with_fire",   "Cooking With Fire"),
    SEA_SHANTY_2      ("sea_shanty_2",        "Sea Shanty 2"),
    PICK_AND_SHOVEL   ("pick_and_shovel",     "Pick and Shovel"),
    VILLAGE_OF_TREES  ("village_of_trees",    "Village of Trees"),

    // Combat
    BLOOD_MONEY       ("blood_money",         "Blood Money");

    /** Filename without extension (OGG only). */
    public final String fileName;

    /** Display name shown in the Music tab player list. */
    public final String displayName;

    MusicTrack(String fileName, String displayName) {
        this.fileName    = fileName;
        this.displayName = displayName;
    }

    public String filePath() {
        return "audio/music/" + fileName + ".ogg";
    }
}
