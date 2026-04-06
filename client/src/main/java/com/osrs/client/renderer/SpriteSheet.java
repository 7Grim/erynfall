package com.osrs.client.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    private static final String ATLAS_VALIDATION_PROPERTY = "erynfall.spriteAtlasValidation";
    private static final String EXPECTED_KEYS_RESOURCE = "sprite-manifest-keys.txt";
    private static final String RUNTIME_META_RESOURCE = "sprite-manifest-runtime.json";

    private final TextureAtlas atlas;
    private final Map<String, Animation<TextureRegion>> animCache = new HashMap<>();
    private final Map<String, SpriteMeta> metadataByKey = new HashMap<>();

    public static final class SpriteMeta {
        private final String key;
        private final String category;
        private final int canvasWidth;
        private final int canvasHeight;
        private final String pivot;
        private final boolean animated;
        private final Float shadowWidth;
        private final Float shadowHeight;
        private final Float shadowAlpha;

        private SpriteMeta(String key,
                           String category,
                           int canvasWidth,
                           int canvasHeight,
                           String pivot,
                           boolean animated,
                           Float shadowWidth,
                           Float shadowHeight,
                           Float shadowAlpha) {
            this.key = key;
            this.category = category;
            this.canvasWidth = canvasWidth;
            this.canvasHeight = canvasHeight;
            this.pivot = pivot;
            this.animated = animated;
            this.shadowWidth = shadowWidth;
            this.shadowHeight = shadowHeight;
            this.shadowAlpha = shadowAlpha;
        }

        public String key() { return key; }

        public String category() { return category; }

        public int canvasWidth() { return canvasWidth; }

        public int canvasHeight() { return canvasHeight; }

        public String pivot() { return pivot; }

        public boolean animated() { return animated; }

        public Float shadowWidth() { return shadowWidth; }

        public Float shadowHeight() { return shadowHeight; }

        public Float shadowAlpha() { return shadowAlpha; }
    }

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
        loadRuntimeMetadata();
        logMissingManifestSlots();
    }

    private void loadRuntimeMetadata() {
        if (!Gdx.files.internal(RUNTIME_META_RESOURCE).exists()) {
            Gdx.app.log("SpriteSheet", "WARN: runtime metadata resource missing: " + RUNTIME_META_RESOURCE);
            return;
        }
        try {
            String content = Gdx.files.internal(RUNTIME_META_RESOURCE).readString();
            JsonValue root = new JsonReader().parse(content);
            JsonValue assets = root.get("assets");
            if (assets == null || !assets.isArray()) {
                Gdx.app.log("SpriteSheet", "WARN: runtime metadata JSON has no assets array");
                return;
            }
            for (JsonValue asset = assets.child; asset != null; asset = asset.next) {
                String key = asset.getString("key", null);
                String category = asset.getString("category", "");
                int canvasWidth = asset.getInt("canvas_width", 0);
                int canvasHeight = asset.getInt("canvas_height", 0);
                String pivot = asset.getString("pivot", "bottom-center");
                boolean animated = asset.getBoolean("animated", false);
                if (key == null || key.isBlank() || canvasWidth <= 0 || canvasHeight <= 0) {
                    continue;
                }

                Float shadowWidth = asset.has("shadow_width") ? asset.getFloat("shadow_width") : null;
                Float shadowHeight = asset.has("shadow_height") ? asset.getFloat("shadow_height") : null;
                Float shadowAlpha = asset.has("shadow_alpha") ? asset.getFloat("shadow_alpha") : null;

                metadataByKey.put(key, new SpriteMeta(
                    key,
                    category,
                    canvasWidth,
                    canvasHeight,
                    pivot,
                    animated,
                    shadowWidth,
                    shadowHeight,
                    shadowAlpha
                ));
            }
        } catch (Exception e) {
            Gdx.app.log("SpriteSheet", "WARN: failed to parse runtime metadata: " + e.getMessage());
            metadataByKey.clear();
        }
    }

    private void logMissingManifestSlots() {
        if (!Boolean.parseBoolean(System.getProperty(ATLAS_VALIDATION_PROPERTY, "true"))) {
            return;
        }
        Set<String> expectedKeys = loadExpectedManifestKeys();
        for (String key : expectedKeys) {
            if (atlas.findRegion(key) == null) {
                Gdx.app.log("SpriteSheet", "WARN: manifest key missing from atlas: " + key);
            }
        }
    }

    private Set<String> loadExpectedManifestKeys() {
        Set<String> keys = new HashSet<>();
        if (!Gdx.files.internal(EXPECTED_KEYS_RESOURCE).exists()) {
            Gdx.app.log("SpriteSheet", "WARN: manifest key resource missing: " + EXPECTED_KEYS_RESOURCE);
            return keys;
        }
        String content = Gdx.files.internal(EXPECTED_KEYS_RESOURCE).readString();
        for (String rawLine : content.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            keys.add(line);
        }
        return keys;
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

    public SpriteMeta getMeta(String key) {
        return metadataByKey.get(key);
    }

    public boolean hasMeta(String key) {
        return metadataByKey.containsKey(key);
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
        metadataByKey.clear();
    }
}
