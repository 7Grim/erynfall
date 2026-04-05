package com.osrs.client.audio;

/**
 * Looping ambient sound layers that play underneath the music track.
 *
 * Each zone may specify one ambient layer (or none). The layer fades in
 * when entering the zone and fades out when leaving, independently of the
 * music crossfade so they can transition at different rates.
 *
 * baseVolume is the target volume relative to the ambient master slider —
 * water sounds are louder than wind so they read clearly as "you are near water".
 */
public enum AmbientLayer {

    FOREST_BIRDS  ("ambient_forest_birds",   0.25f),
    WATER_FLOWING ("ambient_water_flowing",  0.35f),
    FIRE_CRACKLE  ("ambient_fire_crackle",   0.30f),
    WIND          ("ambient_wind",           0.20f);

    public final String fileName;

    /** Target volume when fully faded in (relative to ambientMaster, 0..1). */
    public final float baseVolume;

    AmbientLayer(String fileName, float baseVolume) {
        this.fileName   = fileName;
        this.baseVolume = baseVolume;
    }

    public String filePath() {
        return "audio/ambient/" + fileName + ".ogg";
    }
}
