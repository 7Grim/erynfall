package com.osrs.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/**
 * Persistent client-side preferences backed by LibGDX {@link Preferences}.
 * On desktop: ~/.prefs/erynfall_client (platform-dependent).
 *
 * Only store UI/visual preferences here — not gameplay state.
 * Instantiate once in {@link GameScreen} after Gdx is initialised.
 */
public class ClientPreferences {

    // ── Enums ─────────────────────────────────────────────────────────────────

    /**
     * Controls when building roofs are rendered.
     *
     * AUTO        — hide roof when player is physically inside the building footprint.
     *               Matches original OSRS "roof-fading" behaviour, but uses full hide.
     * ALWAYS_SHOW — roofs always rendered.  Useful for top-down screenshots / art review.
     * ALWAYS_HIDE — roofs never rendered.   Useful for players who find them distracting.
     */
    public enum RoofVisibility {
        AUTO,
        ALWAYS_SHOW,
        ALWAYS_HIDE
    }

    // ── Keys + defaults ───────────────────────────────────────────────────────

    private static final String PREF_FILE    = "erynfall_client";
    private static final String KEY_ROOF_VIS = "roof_visibility";

    // ── State ─────────────────────────────────────────────────────────────────

    private final Preferences prefs;
    private RoofVisibility    roofVisibility;

    // ── Construction ──────────────────────────────────────────────────────────

    public ClientPreferences() {
        prefs = Gdx.app.getPreferences(PREF_FILE);
        roofVisibility = parse(prefs.getString(KEY_ROOF_VIS, ""), RoofVisibility.AUTO);
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public RoofVisibility getRoofVisibility() {
        return roofVisibility;
    }

    public void setRoofVisibility(RoofVisibility v) {
        if (v == null) v = RoofVisibility.AUTO;
        roofVisibility = v;
        prefs.putString(KEY_ROOF_VIS, v.name());
        prefs.flush();
    }

    /**
     * Cycle AUTO → ALWAYS_SHOW → ALWAYS_HIDE → AUTO.
     * Called by the Options panel toggle button.
     */
    public void cycleRoofVisibility() {
        RoofVisibility[] vals = RoofVisibility.values();
        setRoofVisibility(vals[(roofVisibility.ordinal() + 1) % vals.length]);
    }

    /** Human-readable label for the current setting (used in Options UI). */
    public String roofVisibilityLabel() {
        return switch (roofVisibility) {
            case AUTO        -> "Roofs: Auto";
            case ALWAYS_SHOW -> "Roofs: Always On";
            case ALWAYS_HIDE -> "Roofs: Always Off";
        };
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private static <T extends Enum<T>> T parse(String raw, T fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            @SuppressWarnings("unchecked")
            T val = (T) Enum.valueOf(fallback.getClass(), raw.trim().toUpperCase());
            return val;
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
