package com.osrs.shared;

/**
 * Canonical tick conversion constants for the Erynfall server.
 *
 * Server runs at 256 Hz. OSRS game tick = 0.6 s = 153.6 server ticks, rounded to 154.
 * All timing in gameplay systems should be expressed in multiples of OSRS_TICK.
 */
public final class TickConstants {

    private TickConstants() {}

    /** Server ticks per OSRS game tick (256 Hz × 0.6 s = 153.6 ≈ 154). */
    public static final int OSRS_TICK = WeaponRegistry.OSRS_TICKS_TO_SERVER;

    // -----------------------------------------------------------------------
    // Skilling action intervals (OSRS tick multiples)
    // -----------------------------------------------------------------------

    /** Woodcutting: 4 OSRS ticks per roll. */
    public static final int CHOP_ATTEMPT_INTERVAL = WoodcuttingRegistry.CHOP_ATTEMPT_OSRS_TICKS * OSRS_TICK;

    /** Mining: 3 OSRS ticks per roll. */
    public static final int MINE_ATTEMPT_INTERVAL = MiningRegistry.MINE_ATTEMPT_OSRS_TICKS * OSRS_TICK;

    /** Smithing: 5 OSRS ticks per item. */
    public static final int SMITH_ITEM_ATTEMPT_INTERVAL = 5 * OSRS_TICK;

    /** Firemaking: 2 OSRS ticks per log. */
    public static final int FIREMAKING_ATTEMPT_INTERVAL = 2 * OSRS_TICK;

    // -----------------------------------------------------------------------
    // NPC timing
    // -----------------------------------------------------------------------

    /** NPC movement: 1 tile per OSRS tick. */
    public static final int NPC_MOVE_SPEED = OSRS_TICK;

    /** NPC attack speed: 4 OSRS ticks (2.4 s). */
    public static final int NPC_ATTACK_SPEED = 4 * OSRS_TICK;

    /**
     * NPC wander minimum delay: ~1.3 OSRS ticks (200 server ticks).
     * Kept in server-tick units because it is tuned for visual feel, not OSRS lore.
     */
    public static final int WANDER_MIN = 200;

    /**
     * NPC wander maximum delay: ~2.9 OSRS ticks (450 server ticks).
     * Kept in server-tick units because it is tuned for visual feel, not OSRS lore.
     */
    public static final int WANDER_MAX = 450;

    // -----------------------------------------------------------------------
    // World / session timers
    // -----------------------------------------------------------------------

    /** Autosave interval: 60 s × 256 Hz = 15 360 server ticks. */
    public static final long AUTOSAVE_INTERVAL = 60L * 256L;

    // Prayer drain rate is now per-prayer (PrayerRegistry.drainRate) accumulated per OSRS tick.

    /** PID rotation minimum: 100 OSRS ticks. */
    public static final long PID_ROTATE_MIN = 100L * OSRS_TICK;

    /** PID rotation maximum: 150 OSRS ticks. */
    public static final long PID_ROTATE_MAX = 150L * OSRS_TICK;

    /** Temporary fire lifetime: 60 s × 256 Hz = 15 360 server ticks. */
    public static final long TEMP_FIRE_LIFETIME_TICKS = 60L * 256L;
}
