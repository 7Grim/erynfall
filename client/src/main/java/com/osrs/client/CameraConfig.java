package com.osrs.client;

/**
 * Central camera tuning knobs. Change values here; nowhere else.
 *
 * Design target: OSRS-like isometric readability — top-down, minimal perspective
 * distortion on tiles and buildings, player easy to track.
 *
 * Fields split into two tiers:
 *   _INITIAL constants — immutable compile-time baselines; used by CameraTuningPanel.resetDefaults().
 *   Mutable statics    — live values; written by CameraTuningPanel at runtime, read every frame.
 */
public final class CameraConfig {

    private CameraConfig() {}

    // Set false to strip the tuning panel from production builds (one-line change).
    public static final boolean TUNING_PANEL_ENABLED = true;

    // --- Compile-time baselines (immutable) ---

    // 45° is standard 3D game FOV. Narrowing to 30° compresses the perspective
    // frustum so near and far tiles appear much closer in size (telephoto effect).
    // This removes the "buildings looming overhead" distortion.
    public static final float FOV_INITIAL              = 30f;

    // 0.9 rad (~52°) was too cinematic. 1.05 rad (~60°) reads closer to OSRS
    // and keeps the ground plane dominant in the viewport.
    public static final float PITCH_DEFAULT_INITIAL    = 1.05f;

    // Raised floor from 0.3 (17°) to 0.55 (31°) — prevents the camera from
    // dropping so low that buildings block the view and perspective distortion
    // dominates.
    public static final float PITCH_MIN_INITIAL        = 0.55f;
    public static final float PITCH_MAX_INITIAL        = 1.40f;   // near-vertical; unchanged

    // 14 tiles (was 12) compensates for the narrower FOV so the player still
    // fills a comfortable portion of the screen.
    public static final float DISTANCE_DEFAULT_INITIAL = 14f;
    public static final float DISTANCE_MIN_INITIAL     = 6f;
    public static final float DISTANCE_MAX_INITIAL     = 33f;    // 1.5× prior max (22) for wider view

    // Camera targets this Y above ground instead of ground level (0).
    // Centres the view on the player's torso so the player is not stuck at the
    // bottom of the screen and buildings behind them don't loom as tall.
    public static final float LOOKAT_HEIGHT_INITIAL    = 0.75f;

    // --- Live-tunable values (read every frame by GameScreen) ---

    public static float FOV             = FOV_INITIAL;
    public static float PITCH_DEFAULT   = PITCH_DEFAULT_INITIAL;
    public static float PITCH_MIN       = PITCH_MIN_INITIAL;
    public static float PITCH_MAX       = PITCH_MAX_INITIAL;
    public static float DISTANCE_DEFAULT = DISTANCE_DEFAULT_INITIAL;
    public static float DISTANCE_MIN    = DISTANCE_MIN_INITIAL;
    public static float DISTANCE_MAX    = DISTANCE_MAX_INITIAL;
    public static float LOOKAT_HEIGHT   = LOOKAT_HEIGHT_INITIAL;

    // --- Fixed (not tunable at runtime) ---

    public static final float YAW_DEFAULT             = 0f;
    public static final float NEAR                    = 0.1f;
    public static final float FAR                     = 600f;
    public static final float YAW_SPEED               = 1.8f;   // rad/sec
    public static final float PITCH_SPEED             = 1.2f;   // rad/sec
    public static final float SCROLL_ZOOM_SENSITIVITY = 0.4f;   // tiles per scroll notch
}
