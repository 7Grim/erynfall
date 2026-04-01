package com.osrs.client.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FontGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(FontGenerator.class);

    private static final String DEFAULT_TTF_PATH = "fonts/osrs.ttf";

    private FontGenerator() {
    }

    public static BitmapFont generateOsrsFont(String ttfPath, int size, Color color) {
        FileHandle handle = Gdx.files.internal(ttfPath);
        if (!handle.exists()) {
            LOG.warn("TTF font '{}' not found; using BitmapFont fallback", ttfPath);
            BitmapFont fallback = new BitmapFont();
            fallback.setUseIntegerPositions(true);
            fallback.setColor(color);
            return fallback;
        }

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(handle);
        try {
            FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
            p.size = size;
            p.color = color;
            p.magFilter = Texture.TextureFilter.Linear;
            p.minFilter = Texture.TextureFilter.Linear;
            p.hinting = FreeTypeFontGenerator.Hinting.Full;
            BitmapFont font = generator.generateFont(p);
            font.setUseIntegerPositions(true);
            return font;
        } finally {
            generator.dispose();
        }
    }

    public static BitmapFont generateOsrsFont(String variant, Color color) {
        int size = switch (variant) {
            case "bold"             -> 15;
            case "small", "tooltip" -> 13;
            default                 -> 14;
        };
        return generateOsrsFont(DEFAULT_TTF_PATH, size, color);
    }
}
