package com.osrs.client.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import java.util.HashMap;
import java.util.Map;

/**
 * Wraps the packed sprites.atlas and provides quick region / animation lookup.
 *
 * Usage pattern:
 *   SpriteSheet sheet = SpriteSheet.load(); // null if atlas absent
 *   TextureRegion r = sheet.getTile("tile_grass"); // null if not in atlas
 *
 * Callers treat null from any method as "not available — use ShapeRenderer fallback".
 */
public class SpriteSheet {

    /** Seconds per animation frame. Matches OSRS ~6 fps walk cycle. */
    private static final float FRAME_DURATION = 0.167f;

    private final TextureAtlas atlas;
    private final Map<String, Animation<TextureRegion>> animCache = new HashMap<>();

    /**
     * Loads sprites.atlas from the classpath resources.
     * Returns null (no exception) when the file is absent so the game falls
     * back to ShapeRenderer rendering for everything.
     */
    public static SpriteSheet load() {
        try {
            if (!Gdx.files.internal("sprites.atlas").exists()) {
                Gdx.app.log("SpriteSheet", "sprites.atlas not found — using ShapeRenderer fallback");
                return null;
            }
            return new SpriteSheet(new TextureAtlas(Gdx.files.internal("sprites.atlas")));
        } catch (Exception e) {
            Gdx.app.log("SpriteSheet", "Failed to load sprites.atlas: " + e.getMessage());
            return null;
        }
    }

    private SpriteSheet(TextureAtlas atlas) {
        this.atlas = atlas;
        // Nearest filter is mandatory for pixel art — prevents bilinear blurring
        atlas.getTextures().forEach(t ->
            t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest));
    }

    /**
     * Returns the TextureRegion for the given atlas key, or null if absent.
     * Used for static sprites (tiles, idle entities).
     */
    public TextureRegion getTile(String name) {
        return atlas.findRegion(name);
    }

    /** True when the atlas contains a region with the given name. */
    public boolean hasTile(String name) {
        return atlas.findRegion(name) != null;
    }

    /**
     * Returns a looping Animation built from all regions whose names start with
     * {@code name} followed by an underscore and a frame index (e.g. "walk_n_0",
     * "walk_n_1" for base name "walk_n").
     *
     * Returns null if no frames exist in the atlas.
     */
    public Animation<TextureRegion> getAnimation(String name) {
        return animCache.computeIfAbsent(name, k -> {
            Array<TextureAtlas.AtlasRegion> frames = atlas.findRegions(k);
            if (frames.isEmpty()) return null;
            return new Animation<>(FRAME_DURATION, frames, Animation.PlayMode.LOOP);
        });
    }

    /** True when the atlas has at least one animation frame for this base name. */
    public boolean hasAnimation(String name) {
        return !atlas.findRegions(name).isEmpty();
    }

    /**
     * Reloads the atlas from disk.  Call this after TexturePacker has run
     * (e.g. on F5 hot-reload) to pick up newly exported sprites without
     * restarting the client.
     *
     * Returns a new SpriteSheet (or null on failure) — callers must replace
     * their reference.
     */
    public SpriteSheet reload() {
        dispose();
        return load();
    }

    public void dispose() {
        atlas.dispose();
        animCache.clear();
    }
}
