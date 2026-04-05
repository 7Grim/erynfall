package com.osrs.client.audio;

import java.util.Arrays;
import java.util.List;

/**
 * A rectangular tile region with an associated music track and optional ambient layer.
 *
 * Zones are checked in priority order — first match wins. The list is ordered
 * most-specific (narrow skill zones) to least-specific (broad fallback zones).
 *
 * Single-tile path-separator rows between zones are intentionally excluded so
 * music does not re-trigger while the player walks through a transition row.
 * The player continues hearing the last matched zone until they fully cross into
 * a new one.
 *
 * Coordinate system: y=0 is the top of the map, y increases downward.
 * minY/maxY use half-open range [minY, maxY).
 */
public final class AudioZone {

    public final String       name;
    public final int          minX, minY, maxX, maxY;
    public final MusicTrack   music;
    public final AmbientLayer ambient;   // null = no ambient layer in this zone

    public AudioZone(String name, int minX, int minY, int maxX, int maxY,
                     MusicTrack music, AmbientLayer ambient) {
        this.name    = name;
        this.minX    = minX;
        this.minY    = minY;
        this.maxX    = maxX;
        this.maxY    = maxY;
        this.music   = music;
        this.ambient = ambient;
    }

    public boolean contains(int tileX, int tileY) {
        return tileX >= minX && tileX < maxX && tileY >= minY && tileY < maxY;
    }

    // -----------------------------------------------------------------------
    // Zone registry — Tutorial Island (104×104 grid)
    //
    // Zone rows from map.yaml / world.yml:
    //   y= 8–11  North road
    //   y=12–22  Future skill zones (C/B/A)
    //   y=24–26  Cooking + Services  (fires at y=25)
    //   y=28–30  Fishing             (spots at y=29)
    //   y=32–34  Mining              (rocks at y=33)
    //   y=36–38  Woodcutting         (trees at y=37)
    //   y=40–59  Open zone / Town    (spawn at y=50, banker at y=44)
    //   y=61–65  Combat Low          (rats/chickens at y=63)
    //   y=67–71  Combat Mid          (giant rats/goblins at y=69)
    //   y=73–77  Combat High         (cows at y=75)
    //   y=79–83  Future Combat A
    //   y=85–89  Future Combat B
    // -----------------------------------------------------------------------
    public static final List<AudioZone> TUTORIAL_ISLAND = Arrays.asList(
        // Skill zones north → south (checked before the broad town zone)
        new AudioZone("skill_north",  0,  8, 104, 23, MusicTrack.NEWBIE_MELODY,     AmbientLayer.FOREST_BIRDS),
        new AudioZone("cooking",      0, 24, 104, 27, MusicTrack.COOKING_WITH_FIRE,  AmbientLayer.FIRE_CRACKLE),
        new AudioZone("fishing",      0, 28, 104, 31, MusicTrack.SEA_SHANTY_2,      AmbientLayer.WATER_FLOWING),
        new AudioZone("mining",       0, 32, 104, 35, MusicTrack.PICK_AND_SHOVEL,   null),
        new AudioZone("woodcutting",  0, 36, 104, 39, MusicTrack.VILLAGE_OF_TREES,  AmbientLayer.FOREST_BIRDS),
        // Town / spawn area
        new AudioZone("town",         0, 40, 104, 60, MusicTrack.NEWBIE_MELODY,     null),
        // Combat zones south
        new AudioZone("combat_low",   0, 61, 104, 66, MusicTrack.BLOOD_MONEY,       null),
        new AudioZone("combat_mid",   0, 67, 104, 72, MusicTrack.BLOOD_MONEY,       null),
        new AudioZone("combat_high",  0, 73, 104, 78, MusicTrack.BLOOD_MONEY,       null),
        new AudioZone("combat_a",     0, 79, 104, 84, MusicTrack.BLOOD_MONEY,       null),
        new AudioZone("combat_b",     0, 85, 104, 96, MusicTrack.BLOOD_MONEY,       null)
    );

    /**
     * Returns the first zone in the list that contains (tileX, tileY),
     * or null if the player is on a path-separator or border tile.
     */
    public static AudioZone findZone(List<AudioZone> zones, int tileX, int tileY) {
        for (AudioZone z : zones) {
            if (z.contains(tileX, tileY)) return z;
        }
        return null;
    }
}
