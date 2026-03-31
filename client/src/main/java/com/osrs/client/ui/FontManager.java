package com.osrs.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.osrs.client.util.FontGenerator;

public final class FontManager {
    public static final Color TEXT_WHITE = new Color(1f, 1f, 1f, 1f);
    public static final Color TEXT_CYAN = new Color(0f, 0.38f, 1f, 1f);
    public static final Color TEXT_YELLOW = new Color(1f, 0.85f, 0f, 1f);
    public static final Color TEXT_GOLD = new Color(0.75f, 0.60f, 0.10f, 1f);

    public enum FontContext {
        BASE_UI,
        SKILL,
        TOOLTIP,
        SMALL_LABEL
    }

    public enum OsrsColor {
        WHITE,
        CYAN,
        YELLOW,
        GOLD
    }

    private static BitmapFont osrsRegular;
    private static BitmapFont osrsBold;
    private static BitmapFont osrsSmall;
    private static BitmapFont osrsTooltip;

    private FontManager() {
    }

    public static void initialize() {
        if (osrsRegular != null) {
            return;
        }
        osrsRegular = FontGenerator.generateOsrsFont("regular", TEXT_WHITE);
        osrsBold = FontGenerator.generateOsrsFont("bold", TEXT_WHITE);
        osrsSmall = FontGenerator.generateOsrsFont("small", TEXT_WHITE);
        osrsTooltip = FontGenerator.generateOsrsFont("tooltip", TEXT_WHITE);

        osrsRegular.getData().markupEnabled = true;
        osrsBold.getData().markupEnabled = true;
        osrsSmall.getData().markupEnabled = true;
        osrsTooltip.getData().markupEnabled = true;
    }

    public static BitmapFont regular() {
        initialize();
        return osrsRegular;
    }

    public static BitmapFont bold() {
        initialize();
        return osrsBold;
    }

    public static BitmapFont small() {
        initialize();
        return osrsSmall;
    }

    public static BitmapFont tooltip() {
        initialize();
        return osrsTooltip;
    }

    public static float getScale(FontContext context) {
        return switch (context) {
            case BASE_UI -> 1.0f;
            case SKILL -> 0.85f;
            case TOOLTIP, SMALL_LABEL -> 0.75f;
        };
    }

    public static Color getOsrsColor(OsrsColor colorType) {
        return switch (colorType) {
            case WHITE -> TEXT_WHITE;
            case CYAN -> TEXT_CYAN;
            case YELLOW -> TEXT_YELLOW;
            case GOLD -> TEXT_GOLD;
        };
    }

    public static void dispose() {
        if (osrsRegular != null) osrsRegular.dispose();
        if (osrsBold != null) osrsBold.dispose();
        if (osrsSmall != null) osrsSmall.dispose();
        if (osrsTooltip != null) osrsTooltip.dispose();
        osrsRegular = null;
        osrsBold = null;
        osrsSmall = null;
        osrsTooltip = null;
    }
}
