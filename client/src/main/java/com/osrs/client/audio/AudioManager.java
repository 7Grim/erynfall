package com.osrs.client.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.EnumSet;

/**
 * Central audio controller.
 *
 * <h3>Music system</h3>
 * <p>At most two Music objects are open at once (outgoing + incoming) to keep
 * memory low.  When a new track is requested:
 * <ol>
 *   <li>The current track fades out over {@value #FADE_OUT_SEC} s.</li>
 *   <li>Once silent it is stopped and disposed.</li>
 *   <li>The new track loads, starts at volume 0, and fades in over
 *       {@value #FADE_IN_SEC} s.</li>
 * </ol>
 * If another request arrives while fading, the pending track is updated;
 * the current fade-out is not interrupted.</p>
 *
 * <h3>Ambient layer</h3>
 * <p>One ambient loop per zone fades in/out alongside the music but independently,
 * at a lower volume so it reads as environmental texture.</p>
 *
 * <h3>Sound effects</h3>
 * <p>All SFX are loaded eagerly on startup and played one-shot.  Missing files
 * produce silence without crashing.</p>
 *
 * <h3>Volume</h3>
 * <p>Five levels (0 = off, 1–4 = low → full) for music, effects, and ambient.
 * Settings are persisted via LibGDX Preferences.</p>
 *
 * <h3>Thread safety</h3>
 * <p>All public methods must be called from the GL/render thread.</p>
 */
public class AudioManager {

    private static final Logger LOG = LoggerFactory.getLogger(AudioManager.class);

    // -----------------------------------------------------------------------
    // Volume constants
    // -----------------------------------------------------------------------
    public static final int MAX_VOLUME_LEVEL = 4;
    private static final float[] VOLUME_TABLE = { 0f, 0.25f, 0.50f, 0.75f, 1.00f };

    // -----------------------------------------------------------------------
    // Music fade timings (seconds)
    // -----------------------------------------------------------------------
    private static final float FADE_OUT_SEC    = 2.0f;
    private static final float FADE_IN_SEC     = 1.5f;
    private static final float AMBIENT_FADE_SEC = 3.0f;

    // -----------------------------------------------------------------------
    // Skill → SoundEffect mapping  (index = Player.SKILL_* constants)
    // -----------------------------------------------------------------------
    private static final SoundEffect[] SKILL_SOUNDS = {
        SoundEffect.COMBAT_HIT,           //  0 Attack
        SoundEffect.COMBAT_HIT,           //  1 Strength
        SoundEffect.COMBAT_HIT,           //  2 Defence
        null,                              //  3 Hitpoints   (no XP-drop sound)
        SoundEffect.RANGED_FIRE,          //  4 Ranged
        SoundEffect.SPELL_CAST,           //  5 Magic
        SoundEffect.PRAYER_ACTIVATE,      //  6 Prayer
        SoundEffect.WOODCUT_CHOP,         //  7 Woodcutting
        SoundEffect.FISH_REEL,            //  8 Fishing
        SoundEffect.COOK_SIZZLE,          //  9 Cooking
        SoundEffect.MINE_PICK,            // 10 Mining
        SoundEffect.SMITHING_ANVIL,       // 11 Smithing
        SoundEffect.FIREMAKING_LIGHT,     // 12 Firemaking
        SoundEffect.CRAFT_CHISEL,         // 13 Crafting
        SoundEffect.RUNECRAFT_ALTAR,      // 14 Runecrafting
        SoundEffect.FLETCH_ARROW,         // 15 Fletching
        SoundEffect.AGILITY_STEP,         // 16 Agility
        SoundEffect.HERBLORE_MIX,         // 17 Herblore
        SoundEffect.THIEVING_PICKPOCKET,  // 18 Thieving
        SoundEffect.SLAYER_HIT,           // 19 Slayer
        SoundEffect.FARMING_RAKE,         // 20 Farming
        SoundEffect.HUNTER_TRAP,          // 21 Hunter
        SoundEffect.CONSTRUCT_SAW,        // 22 Construction
    };

    // -----------------------------------------------------------------------
    // Volume state
    // -----------------------------------------------------------------------
    private int musicLevel   = 4;
    private int sfxLevel     = 4;
    private int ambientLevel = 3;

    // -----------------------------------------------------------------------
    // Music fade state
    // Invariant: at most one of (outgoingMusic, activeMusic) is non-null during
    // steady playback; during a fade both may be non-null simultaneously.
    // -----------------------------------------------------------------------
    private Music   outgoingMusic = null;
    private float   outgoingProg  = 1f;   // fade progress: 1=full volume, 0=silent
    private Music   activeMusic   = null;
    private float   activeProg    = 0f;   // fade progress: 0=silent, 1=full volume
    private MusicTrack activeTrack  = null;
    private MusicTrack pendingTrack = null;   // queued; starts after outgoing finishes

    // -----------------------------------------------------------------------
    // Ambient fade state  (same pattern as music)
    // -----------------------------------------------------------------------
    private Music        outAmbient    = null;
    private float        outAmbProg    = 1f;
    private Music        activeAmbient = null;
    private float        activeAmbProg = 0f;
    private AmbientLayer activeAmbLayer  = null;
    private AmbientLayer pendingAmbLayer = null;

    // -----------------------------------------------------------------------
    // Sound effects
    // -----------------------------------------------------------------------
    private final EnumMap<SoundEffect, Sound> sounds = new EnumMap<>(SoundEffect.class);

    // -----------------------------------------------------------------------
    // Zone / unlock tracking
    // -----------------------------------------------------------------------
    private String  currentZoneName = null;
    /** true = user manually picked a track; zone changes won't override music */
    private boolean manualMode = false;
    private final EnumSet<MusicTrack> unlockedTracks = EnumSet.noneOf(MusicTrack.class);

    // -----------------------------------------------------------------------
    // Preferences key
    // -----------------------------------------------------------------------
    private static final String PREF_FILE = "erynfall.audio";

    // =======================================================================
    // Construction / lifecycle
    // =======================================================================

    public AudioManager() {
        loadPreferences();
        loadAllSounds();
        // Login screen tracks are unlocked by default
        unlockedTracks.add(MusicTrack.SCAPE_MAIN);
    }

    /** Must be called once per frame from the GL thread. */
    public void update(float delta) {
        updateMusicFade(delta);
        updateAmbientFade(delta);
    }

    /** Release all audio resources. Call from dispose(). */
    public void dispose() {
        stopAll();
        for (Sound s : sounds.values()) s.dispose();
        sounds.clear();
        savePreferences();
    }

    // =======================================================================
    // Music API
    // =======================================================================

    /**
     * Start a crossfade to the given track.
     * No-op if the track is already playing and fully faded in.
     * If the same track is pending, no-op.
     * Safe to call every frame — internal dedup prevents unnecessary fades.
     */
    public void requestMusic(MusicTrack track) {
        if (track == activeTrack && outgoingMusic == null && pendingTrack == null) return;
        if (track == pendingTrack) return;

        pendingTrack = track;

        if (outgoingMusic == null) {
            // Begin fading out whatever is currently active (may be null)
            startMusicFadeOut();
        }
        // If outgoingMusic != null: already fading — pendingTrack is queued for after
    }

    /** Fade out current music without starting a replacement. */
    public void stopMusic() {
        pendingTrack = null;
        if (outgoingMusic == null) startMusicFadeOut();
    }

    /** True if the given track is currently the active (or incoming) track. */
    public boolean isPlaying(MusicTrack track) {
        return track == activeTrack && activeMusic != null;
    }

    public MusicTrack getActiveTrack() { return activeTrack; }

    // =======================================================================
    // Zone / in-game music
    // =======================================================================

    /**
     * Call each frame with the player's current tile position.
     * In manual mode (user picked a track) zone changes are ignored.
     * The login track (SCAPE_MAIN) always yields to zone music on first call.
     */
    public void onPlayerMoved(int tileX, int tileY) {
        if (manualMode) return;

        AudioZone zone = AudioZone.findZone(AudioZone.TUTORIAL_ISLAND, tileX, tileY);
        String zoneName = zone != null ? zone.name : null;

        if (java.util.Objects.equals(zoneName, currentZoneName)) return;
        currentZoneName = zoneName;

        if (zone != null) {
            requestMusic(zone.music);
            requestAmbient(zone.ambient);
        }
    }

    /**
     * Play the login-screen music.  Does not set manualMode — zone music will
     * take over automatically once the player enters the game world.
     */
    public void playLoginMusic() {
        manualMode = false;
        currentZoneName = null;
        requestMusic(MusicTrack.SCAPE_MAIN);
        requestAmbient(null);
    }

    /**
     * Let the user manually pick a track from the music tab.
     * Sets manualMode = true so zone transitions don't interrupt it.
     */
    public void playManualTrack(MusicTrack track) {
        if (!unlockedTracks.contains(track)) return;
        manualMode = true;
        requestMusic(track);
    }

    /** Return to automatic zone-based music. */
    public void setAutoMode() {
        manualMode = false;
        currentZoneName = null;   // force re-evaluation on next onPlayerMoved()
    }

    public boolean isManualMode() { return manualMode; }

    // =======================================================================
    // Ambient API
    // =======================================================================

    public void requestAmbient(AmbientLayer layer) {
        if (layer == activeAmbLayer && outAmbient == null && pendingAmbLayer == null) return;
        if (layer == pendingAmbLayer) return;

        pendingAmbLayer = layer;
        if (outAmbient == null) startAmbientFadeOut();
    }

    // =======================================================================
    // Sound effects API
    // =======================================================================

    /** Play a one-shot sound effect. Safe to call when sfxLevel == 0. */
    public void playSfx(SoundEffect sfx) {
        if (sfxLevel == 0) return;
        Sound s = sounds.get(sfx);
        if (s == null) return;
        float vol = sfx.baseVolume * VOLUME_TABLE[sfxLevel];
        s.play(vol);
    }

    /**
     * Play the appropriate skill sound for an XP-drop event.
     * skillIndex must match Player.SKILL_* constants (0–22).
     */
    public void playSfxForSkill(int skillIndex) {
        if (skillIndex < 0 || skillIndex >= SKILL_SOUNDS.length) return;
        SoundEffect sfx = SKILL_SOUNDS[skillIndex];
        if (sfx != null) playSfx(sfx);
    }

    // =======================================================================
    // Volume control
    // =======================================================================

    public int  getMusicLevel()   { return musicLevel; }
    public int  getSfxLevel()     { return sfxLevel; }
    public int  getAmbientLevel() { return ambientLevel; }

    public void setMusicLevel(int level) {
        musicLevel = clampLevel(level);
        applyMusicVolume();
        savePreferences();
    }

    public void setSfxLevel(int level) {
        sfxLevel = clampLevel(level);
        savePreferences();
    }

    public void setAmbientLevel(int level) {
        ambientLevel = clampLevel(level);
        applyAmbientVolume();
        savePreferences();
    }

    // =======================================================================
    // Music unlock registry
    // =======================================================================

    public boolean isUnlocked(MusicTrack track) {
        return unlockedTracks.contains(track);
    }

    /** Returns a snapshot for the music tab to iterate. */
    public MusicTrack[] allTracks() {
        return MusicTrack.values();
    }

    // =======================================================================
    // Internal — music fade
    // =======================================================================

    private void startMusicFadeOut() {
        // Move active → outgoing
        if (outgoingMusic != null) {
            // Already fading something out — dispose immediately, start new fade
            outgoingMusic.stop();
            outgoingMusic.dispose();
        }
        outgoingMusic = activeMusic;
        outgoingProg  = activeProg;
        activeMusic   = null;
        activeProg    = 0f;
        activeTrack   = null;
        // If nothing was playing, outgoingMusic is null → startPendingMusic next frame
    }

    private void startPendingMusic() {
        MusicTrack track = pendingTrack;
        pendingTrack = null;
        if (track == null) return;

        Music m = loadMusic(track.filePath());
        if (m != null) {
            m.setLooping(true);
            m.setVolume(0f);
            m.play();
        }
        activeMusic  = m;
        activeProg   = 0f;
        activeTrack  = track;
        // Unlock when first heard
        unlockedTracks.add(track);
    }

    private void updateMusicFade(float delta) {
        float masterVol = VOLUME_TABLE[musicLevel];

        // Step 1: Fade out outgoing track
        if (outgoingMusic != null) {
            outgoingProg -= delta / FADE_OUT_SEC;
            if (outgoingProg <= 0f) {
                outgoingMusic.stop();
                outgoingMusic.dispose();
                outgoingMusic = null;
                outgoingProg  = 0f;
                // Outgoing finished — start the pending track now
                startPendingMusic();
            } else {
                outgoingMusic.setVolume(outgoingProg * masterVol);
            }
        }

        // Step 2: Fade in active track
        if (activeMusic != null) {
            if (activeProg < 1f) {
                activeProg = Math.min(1f, activeProg + delta / FADE_IN_SEC);
            }
            activeMusic.setVolume(activeProg * masterVol);
        }
    }

    private void applyMusicVolume() {
        float masterVol = VOLUME_TABLE[musicLevel];
        if (activeMusic   != null) activeMusic.setVolume(activeProg * masterVol);
        if (outgoingMusic != null) outgoingMusic.setVolume(outgoingProg * masterVol);
    }

    // =======================================================================
    // Internal — ambient fade
    // =======================================================================

    private void startAmbientFadeOut() {
        if (outAmbient != null) {
            outAmbient.stop();
            outAmbient.dispose();
        }
        outAmbient    = activeAmbient;
        outAmbProg    = activeAmbProg;
        activeAmbient = null;
        activeAmbProg = 0f;
        activeAmbLayer = null;
    }

    private void startPendingAmbient() {
        AmbientLayer layer = pendingAmbLayer;
        pendingAmbLayer = null;
        if (layer == null) return;

        Music m = loadMusic(layer.filePath());
        if (m != null) {
            m.setLooping(true);
            m.setVolume(0f);
            m.play();
        }
        activeAmbient  = m;
        activeAmbProg  = 0f;
        activeAmbLayer = layer;
    }

    private void updateAmbientFade(float delta) {
        if (outAmbient != null) {
            outAmbProg -= delta / AMBIENT_FADE_SEC;
            if (outAmbProg <= 0f) {
                outAmbient.stop();
                outAmbient.dispose();
                outAmbient = null;
                outAmbProg = 0f;
                startPendingAmbient();
            } else {
                float targetVol = activeAmbLayer != null
                    ? activeAmbLayer.baseVolume * VOLUME_TABLE[ambientLevel] : 0f;
                outAmbient.setVolume(outAmbProg * targetVol);
            }
        }

        if (activeAmbient != null && activeAmbLayer != null) {
            float targetVol = activeAmbLayer.baseVolume * VOLUME_TABLE[ambientLevel];
            if (activeAmbProg < 1f) {
                activeAmbProg = Math.min(1f, activeAmbProg + delta / AMBIENT_FADE_SEC);
            }
            activeAmbient.setVolume(activeAmbProg * targetVol);
        }
    }

    private void applyAmbientVolume() {
        if (activeAmbient != null && activeAmbLayer != null) {
            activeAmbient.setVolume(activeAmbProg * activeAmbLayer.baseVolume * VOLUME_TABLE[ambientLevel]);
        }
    }

    // =======================================================================
    // Internal — stop / load / save
    // =======================================================================

    private void stopAll() {
        if (outgoingMusic != null) { outgoingMusic.stop(); outgoingMusic.dispose(); outgoingMusic = null; }
        if (activeMusic   != null) { activeMusic.stop();   activeMusic.dispose();   activeMusic   = null; }
        if (outAmbient    != null) { outAmbient.stop();    outAmbient.dispose();    outAmbient    = null; }
        if (activeAmbient != null) { activeAmbient.stop(); activeAmbient.dispose(); activeAmbient = null; }
        activeTrack  = null;
        pendingTrack = null;
        activeAmbLayer  = null;
        pendingAmbLayer = null;
    }

    /**
     * Load a Music file null-safely.
     * Returns null if the file does not exist or the platform has no audio.
     */
    private static Music loadMusic(String path) {
        try {
            FileHandle fh = Gdx.files.internal(path);
            if (!fh.exists()) return null;
            return Gdx.audio.newMusic(fh);
        } catch (Exception e) {
            LOG.debug("Audio file not available: {}", path);
            return null;
        }
    }

    /**
     * Load a Sound file null-safely.
     * Returns null if the file does not exist or the platform has no audio.
     */
    private static Sound loadSound(String path) {
        try {
            FileHandle fh = Gdx.files.internal(path);
            if (!fh.exists()) return null;
            return Gdx.audio.newSound(fh);
        } catch (Exception e) {
            LOG.debug("Audio file not available: {}", path);
            return null;
        }
    }

    private void loadAllSounds() {
        for (SoundEffect sfx : SoundEffect.values()) {
            Sound s = loadSound(sfx.filePath());
            if (s != null) sounds.put(sfx, s);
        }
    }

    private static int clampLevel(int level) {
        return Math.max(0, Math.min(MAX_VOLUME_LEVEL, level));
    }

    // -----------------------------------------------------------------------
    // Preferences persistence
    // -----------------------------------------------------------------------

    private void loadPreferences() {
        try {
            Preferences p = Gdx.app.getPreferences(PREF_FILE);
            musicLevel   = clampLevel(p.getInteger("music_level",   4));
            sfxLevel     = clampLevel(p.getInteger("sfx_level",     4));
            ambientLevel = clampLevel(p.getInteger("ambient_level", 3));
            for (MusicTrack t : MusicTrack.values()) {
                if (p.getBoolean("unlocked_" + t.name(), false)) {
                    unlockedTracks.add(t);
                }
            }
        } catch (Exception e) {
            LOG.debug("Could not read audio preferences: {}", e.getMessage());
        }
    }

    private void savePreferences() {
        try {
            Preferences p = Gdx.app.getPreferences(PREF_FILE);
            p.putInteger("music_level",   musicLevel);
            p.putInteger("sfx_level",     sfxLevel);
            p.putInteger("ambient_level", ambientLevel);
            for (MusicTrack t : unlockedTracks) {
                p.putBoolean("unlocked_" + t.name(), true);
            }
            p.flush();
        } catch (Exception e) {
            LOG.debug("Could not save audio preferences: {}", e.getMessage());
        }
    }
}
