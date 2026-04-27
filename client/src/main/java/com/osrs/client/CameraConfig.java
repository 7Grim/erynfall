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

    // 1.05 rad (~60°) was too low vs real OSRS which sits ~65-68°.
    // 1.15 rad (~66°) keeps the ground dominant and reduces building loom.
    public static final float PITCH_DEFAULT_INITIAL    = 1.15f;

    // Raised floor to 0.70 (40°) — prevents the camera from dropping so low
    // that buildings block the view and perspective distortion dominates.
    public static final float PITCH_MIN_INITIAL        = 0.70f;
    public static final float PITCH_MAX_INITIAL        = 1.40f;   // near-vertical; unchanged

    // 16 tiles: slightly pulled back from 14 to compensate for the steeper pitch
    // and keep world context visible (OSRS shows roughly a 13x13 tile window).
    public static final float DISTANCE_DEFAULT_INITIAL = 16f;
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
