package com.osrs.client.audio;

/**
 * All in-game sound effects.
 *
 * Files live under client/src/main/resources/audio/sfx/ as OGG.
 * Missing files produce silence — AudioManager loads null-safely.
 *
 * baseVolume is a 0..1 multiplier applied before the master SFX volume,
 * letting quieter effects (UI clicks) sit below louder ones (combat hits)
 * even when both sliders are at maximum.
 */
public enum SoundEffect {

    // ---- Combat ----
    COMBAT_HIT            ("combat_hit",              1.00f),
    COMBAT_MISS           ("combat_miss",             0.80f),
    COMBAT_DEATH          ("combat_death",            1.00f),

    // ---- Skilling ----
    WOODCUT_CHOP          ("woodcut_chop",            0.90f),
    MINE_PICK             ("mine_pick",               1.00f),
    FISH_REEL             ("fish_reel",               0.80f),
    COOK_SIZZLE           ("cook_sizzle",             0.70f),
    FIREMAKING_LIGHT      ("firemaking_light",        1.00f),
    SMITHING_ANVIL        ("smithing_anvil",          1.00f),
    CRAFT_CHISEL          ("craft_chisel",            0.80f),
    RUNECRAFT_ALTAR       ("runecraft_altar",         1.00f),
    FLETCH_ARROW          ("fletch_arrow",            0.70f),
    AGILITY_STEP          ("agility_step",            0.90f),
    HERBLORE_MIX          ("herblore_mix",            0.80f),
    THIEVING_PICKPOCKET   ("thieving_pickpocket",     0.90f),
    SLAYER_HIT            ("slayer_hit",              1.00f),
    FARMING_RAKE          ("farming_rake",            0.70f),
    HUNTER_TRAP           ("hunter_trap",             0.80f),
    CONSTRUCT_SAW         ("construct_saw",           0.90f),
    RANGED_FIRE           ("ranged_fire",             0.90f),
    SPELL_CAST            ("spell_cast",              1.00f),
    PRAYER_ACTIVATE       ("prayer_activate",         0.80f),

    // ---- Progression ----
    LEVEL_UP              ("level_up_fanfare",        1.00f),

    // ---- UI ----
    UI_CLICK              ("ui_click",                0.55f),
    INVENTORY_PICKUP      ("inventory_pickup",        0.70f),
    INVENTORY_DROP        ("inventory_drop",          0.70f);

    public final String fileName;

    /**
     * Base volume multiplier (0..1).
     * Final volume = baseVolume × sfxMasterVolume.
     */
    public final float baseVolume;

    SoundEffect(String fileName, float baseVolume) {
        this.fileName   = fileName;
        this.baseVolume = baseVolume;
    }

    public String filePath() {
        return "audio/sfx/" + fileName + ".ogg";
    }
}
