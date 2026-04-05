package com.osrs.client.renderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Isometric tile + entity renderer.
 *
 * Coordinate formula (32×16 tile metrics):
 *   screenX = (tileX - tileY) * 16
 *   screenY = (tileX + tileY) *  8
 *
 * Sprite pipeline: call {@link #setSpriteSheet(SpriteSheet)} after loading the
 * atlas.  Any entity or tile with a matching atlas region is drawn via
 * SpriteBatch; everything else falls back to ShapeRenderer geometry so the game
 * stays fully playable while art is being added incrementally.
 *
 * Sprite naming conventions:
 *   Tiles   — "tile_grass", "tile_water", "tile_path", "tile_wall", "tile_sand"
 *   Player  — "player"
 *   NPCs    — "npc_guide", "npc_instructor", "npc_rat", "npc_giant_rat",
 *              "npc_chicken", "npc_cow", "npc_goblin"
 *   Objects — "tree", "tree_oak", "tree_willow", "tree_maple", "tree_yew",
 *              "tree_magic", "fishing_spot", "fire"
 */
public class IsometricRenderer {

    public static final int TILE_WIDTH  = 32;
    public static final int TILE_HEIGHT = 16;
    public static final int MAP_WIDTH   = 104;
    public static final int MAP_HEIGHT  = 104;

    /**
     * Y-offset (in screen pixels) from the tile origin to the bottom of
     * entity sprites.  Matches the ShapeRenderer leg bottoms so sprites and
     * shape entities sit on the same visual ground plane.
     */
    private static final float ENTITY_FOOT_OFFSET = -8f;

    /** Minimum tile culling radius so we never under-draw at tight zoom. */
    private static final int MIN_RENDER_RADIUS = 36;

    // --- Tile colours: two variants per type for checkerboard micro-texture ---
    private static final Color GRASS_A = new Color(0.27f, 0.60f, 0.15f, 1f);
    private static final Color GRASS_B = new Color(0.22f, 0.50f, 0.11f, 1f);
    private static final Color PATH_A  = new Color(0.60f, 0.44f, 0.20f, 1f);
    private static final Color PATH_B  = new Color(0.50f, 0.36f, 0.15f, 1f);
    private static final Color WATER_A = new Color(0.12f, 0.44f, 0.82f, 1f);
    private static final Color WATER_B = new Color(0.09f, 0.34f, 0.68f, 1f);
    private static final Color SAND_A  = new Color(0.84f, 0.74f, 0.44f, 1f);
    private static final Color SAND_B  = new Color(0.72f, 0.62f, 0.34f, 1f);
    private static final Color WALL_A  = new Color(0.54f, 0.54f, 0.54f, 1f);
    private static final Color WALL_B  = new Color(0.40f, 0.40f, 0.40f, 1f);

    // --- Shared entity palette ---
    private static final Color SKIN    = new Color(0.92f, 0.72f, 0.52f, 1f);
    private static final Color HAIR    = new Color(0.28f, 0.17f, 0.07f, 1f);
    private static final Color PANTS   = new Color(0.34f, 0.21f, 0.09f, 1f);

    private final OrthographicCamera camera;
    private final SpriteBatch batch;
    private final ShapeRenderer sr;

    /** Nullable — null means no atlas loaded, fall back to ShapeRenderer for everything. */
    private SpriteSheet spriteSheet;

    public IsometricRenderer(OrthographicCamera camera, SpriteBatch batch, ShapeRenderer shapeRenderer) {
        this.camera = camera;
        this.batch  = batch;
        this.sr     = shapeRenderer;
    }

    /**
     * Injects the loaded sprite atlas.  Pass null to revert to ShapeRenderer-only
     * rendering (e.g. while a reload is in progress).
     */
    public void setSpriteSheet(SpriteSheet sheet) {
        this.spriteSheet = sheet;
    }

    // -----------------------------------------------------------------------
    // Coordinate conversion
    // -----------------------------------------------------------------------

    public float worldToScreenX(int tileX, int tileY) {
        return (tileX - tileY) * (TILE_WIDTH / 2.0f);
    }

    public float worldToScreenY(int tileX, int tileY) {
        return (tileX + tileY) * (TILE_HEIGHT / 2.0f);
    }

    public float worldToScreenX(float tileX, float tileY) {
        return (tileX - tileY) * (TILE_WIDTH / 2.0f);
    }

    public float worldToScreenY(float tileX, float tileY) {
        return (tileX + tileY) * (TILE_HEIGHT / 2.0f);
    }

    // -----------------------------------------------------------------------
    // Tile map
    // -----------------------------------------------------------------------

    /**
     * Render the visible portion of the tile map as filled coloured diamonds.
     * Only tiles within RENDER_RADIUS of (centerX, centerY) are drawn.
     *
     * @param tileMap  104×104 array of tile type ints (0=grass … 4=wall)
     * @param centerX  visual player X (used for viewport culling)
     * @param centerY  visual player Y
     */
    public void renderWorld(int[][] tileMap, float centerX, float centerY) {
        float zoom = camera.zoom;
        float scaledViewportWidth = camera.viewportWidth * zoom;
        float scaledViewportHeight = camera.viewportHeight * zoom;

        int cx = CoordinateConverter.screenToWorldX(camera.position.x, camera.position.y);
        int cy = CoordinateConverter.screenToWorldY(camera.position.x, camera.position.y);

        int radiusX = (int) Math.ceil((scaledViewportWidth * 0.5f) / (TILE_WIDTH / 2.0f)) + 4;
        int radiusY = (int) Math.ceil((scaledViewportHeight * 0.5f) / (TILE_HEIGHT / 2.0f)) + 4;
        int renderRadius = Math.max(MIN_RENDER_RADIUS, Math.max(radiusX, radiusY));

        int minX = Math.max(0, cx - renderRadius);
        int maxX = Math.min(MAP_WIDTH  - 1, cx + renderRadius);
        int minY = Math.max(0, cy - renderRadius);
        int maxY = Math.min(MAP_HEIGHT - 1, cy + renderRadius);

        sr.setProjectionMatrix(camera.combined);
        sr.begin(ShapeRenderer.ShapeType.Filled);

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                int type = (tileMap != null) ? tileMap[x][y] : 0;
                float sx = worldToScreenX(x, y);
                float sy = worldToScreenY(x, y);
                fillDiamond(sx, sy, tileColor(type, x, y));
            }
        }

        sr.end();

        // Sprite pass — draws atlas tiles on top of ShapeRenderer tiles.
        // Only runs when the atlas is loaded and contains the relevant region.
        if (spriteSheet != null) {
            batch.setProjectionMatrix(camera.combined);
            batch.begin();
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    int type = (tileMap != null) ? tileMap[x][y] : 0;
                    String key = tileKey(type);
                    TextureRegion region = spriteSheet.getTile(key);
                    if (region != null) {
                        float sx = worldToScreenX(x, y);
                        float sy = worldToScreenY(x, y);
                        // Draw tile sprite centred on the diamond centre point
                        batch.draw(region,
                            sx - TILE_WIDTH  / 2f,
                            sy - TILE_HEIGHT / 2f,
                            TILE_WIDTH, TILE_HEIGHT);
                    }
                }
            }
            batch.end();
        }
    }

    /** Atlas key for a tile type int. */
    private static String tileKey(int type) {
        return switch (type) {
            case 1  -> "tile_water";
            case 2  -> "tile_path";
            case 3  -> "tile_wall";
            case 4  -> "tile_sand";
            default -> "tile_grass";
        };
    }

    /** Draw a filled isometric diamond centred at (sx, sy). */
    private void fillDiamond(float sx, float sy, Color c) {
        float hw = TILE_WIDTH  / 2f;  // 16
        float hh = TILE_HEIGHT / 2f;  //  8
        sr.setColor(c);
        sr.triangle(sx,      sy - hh,   sx + hw, sy,   sx - hw, sy);  // upper half
        sr.triangle(sx,      sy + hh,   sx + hw, sy,   sx - hw, sy);  // lower half
    }

    /** Two-variant checkerboard colour per tile type. */
    private Color tileColor(int type, int x, int y) {
        boolean a = (x + y) % 2 == 0;
        switch (type) {
            case 1:  return a ? WATER_A : WATER_B;  // Water
            case 2:  return a ? PATH_A  : PATH_B;   // Dust Path
            case 3:  return a ? WALL_A  : WALL_B;   // Rock Wall
            case 4:  return a ? SAND_A  : SAND_B;   // Tree / Fence (light wood tone)
            default: return a ? GRASS_A : GRASS_B;  // Prairie Grass
        }
    }

    // -----------------------------------------------------------------------
    // Entity sprites
    // -----------------------------------------------------------------------

    /**
     * Player — blue shirt, brown trousers, skin head, dark hair.
     * Accepts float coords for smooth interpolated movement.
     */
    public void renderPlayer(float playerX, float playerY) {
        renderPlayer(playerX, playerY, false, null);
    }

    public void renderPlayer(float playerX, float playerY, boolean pickingUp) {
        renderPlayer(playerX, playerY, pickingUp, null);
    }

    /**
     * Renders the player sprite.
     *
     * @param pickingUp  true while the 3-tick pickup animation is playing —
     *                   shifts the body down and bends the torso to suggest
     *                   kneeling to pick up an item (OSRS take animation).
     */
    public void renderPlayer(float playerX, float playerY, boolean pickingUp, String pendingAction) {
        float sx = worldToScreenX(playerX, playerY);
        float sy = worldToScreenY(playerX, playerY);

        // Sprite-first: draw atlas sprite if available, skip ShapeRenderer geometry
        if (spriteSheet != null) {
            TextureRegion region = spriteSheet.getTile("player");
            if (region != null) {
                batch.setProjectionMatrix(camera.combined);
                batch.begin();
                batch.draw(region,
                    sx - region.getRegionWidth() / 2f,
                    sy + ENTITY_FOOT_OFFSET);
                batch.end();
                return;
            }
        }

        // Pickup animation: body crouches (all parts shift down 4px, torso squashes)
        float bodyY   = pickingUp ? sy - 4 : sy;
        float bodyH   = pickingUp ? 5 : 8;
        float headY   = pickingUp ? sy + 1 : sy + 6;
        float headH   = pickingUp ? 5 : 6;
        float hairY   = pickingUp ? headY + headH - 2 : sy + 10;

        sr.begin(ShapeRenderer.ShapeType.Filled);

        // Legs
        sr.setColor(PANTS);
        sr.rect(sx - 4, sy - 8, 3, pickingUp ? 4 : 6);
        sr.rect(sx + 1, sy - 8, 3, pickingUp ? 4 : 6);

        // Shirt
        sr.setColor(0.18f, 0.38f, 0.76f, 1f);
        sr.rect(sx - 5, bodyY - 2, 10, bodyH);

        // Head
        sr.setColor(SKIN);
        sr.rect(sx - 3, headY, 6, headH);

        // Hair
        sr.setColor(HAIR);
        sr.rect(sx - 3, hairY, 6, 2);

        // Arm extended downward during pickup
        if (pickingUp) {
            sr.setColor(SKIN);
            sr.rect(sx - 6, sy - 6, 3, 6);  // left arm reaching down
        }

        // Action arm/weapon poses
        if ("chop".equals(pendingAction)) {
            // Right arm raised -- axe-swing overhead
            sr.setColor(SKIN);
            sr.rect(sx + 5, bodyY + bodyH, 3, 7);   // upper arm raised
            // Axe head: small brown rectangle at tip of arm
            sr.setColor(0.40f, 0.26f, 0.10f, 1f);
            sr.rect(sx + 4, bodyY + bodyH + 7, 5, 3);
        } else if (pendingAction != null && pendingAction.startsWith("fish_")) {
            // Right arm extended forward -- rod cast
            sr.setColor(SKIN);
            sr.rect(sx + 5, bodyY + 2, 3, 6);       // upper arm out to side
            // Rod: thin tan line at angle
            sr.setColor(0.75f, 0.60f, 0.35f, 1f);
            sr.rect(sx + 8, bodyY + 3, 2, 8);       // vertical rod shaft
        } else if ("sword".equals(pendingAction)) {
            // Right arm forward with short blade slash pose
            sr.setColor(SKIN);
            sr.rect(sx + 5, bodyY + 1, 3, 7);
            sr.setColor(0.82f, 0.84f, 0.88f, 1f);
            sr.rect(sx + 8, bodyY + 4, 7, 2);
            sr.setColor(0.58f, 0.48f, 0.20f, 1f);
            sr.rect(sx + 7, bodyY + 3, 2, 4);
        } else if ("spear".equals(pendingAction)) {
            // Two-hand thrust pose with long shaft
            sr.setColor(SKIN);
            sr.rect(sx - 7, bodyY + 1, 3, 6);
            sr.rect(sx + 5, bodyY + 1, 3, 6);
            sr.setColor(0.70f, 0.55f, 0.30f, 1f);
            sr.rect(sx - 7, bodyY + 5, 16, 2);
            sr.setColor(0.85f, 0.86f, 0.92f, 1f);
            sr.triangle(sx + 9, bodyY + 4, sx + 13, bodyY + 6, sx + 9, bodyY + 8);
        }

        sr.end();
    }

    /**
     * NPC sprite dispatcher — dispatches by name so any future NPC with the same
     * name automatically gets the correct model regardless of its database ID.
     *
     * @param npcName  entity name from the server (e.g. "Rat", "Chicken")
     */
    public void renderNPC(int npcX, int npcY, int npcId, String npcName) {
        renderNPC((float) npcX, (float) npcY, npcId, npcName);
    }

    /** Maps an NPC display name to its atlas key (e.g. "Giant Rat" → "npc_giant_rat"). */
    private static String npcSpriteKey(String npcName) {
        if (npcName == null) return "npc_unknown";
        return switch (npcName) {
            case "Tutorial Guide"    -> "npc_guide";
            case "Combat Instructor" -> "npc_instructor";
            case "Rat"               -> "npc_rat";
            case "Giant Rat"         -> "npc_giant_rat";
            case "Chicken"           -> "npc_chicken";
            case "Cow"               -> "npc_cow";
            case "Goblin"            -> "npc_goblin";
            case "Tree"              -> "tree";
            case "Oak Tree"          -> "tree_oak";
            case "Willow Tree"       -> "tree_willow";
            case "Maple Tree"        -> "tree_maple";
            case "Yew Tree"          -> "tree_yew";
            case "Magic Tree"        -> "tree_magic";
            case "Fishing Spot"      -> "fishing_spot";
            case "Cooking Fire"      -> "fire";
            default                  -> "npc_unknown";
        };
    }

    public void renderNPC(float npcX, float npcY, int npcId, String npcName) {
        float sx = worldToScreenX(npcX, npcY);
        float sy = worldToScreenY(npcX, npcY);

        // Sprite-first: draw atlas sprite if available, skip ShapeRenderer geometry
        if (spriteSheet != null) {
            TextureRegion region = spriteSheet.getTile(npcSpriteKey(npcName));
            if (region != null) {
                batch.setProjectionMatrix(camera.combined);
                batch.begin();
                batch.draw(region,
                    sx - region.getRegionWidth() / 2f,
                    sy + ENTITY_FOOT_OFFSET);
                batch.end();
                return;
            }
        }

        sr.begin(ShapeRenderer.ShapeType.Filled);
        switch (npcName == null ? "" : npcName) {
            case "Tutorial Guide"    -> drawGuide(sx, sy);
            case "Combat Instructor" -> drawInstructor(sx, sy);
            case "Rat"               -> drawRat(sx, sy);
            case "Giant Rat"         -> drawGiantRat(sx, sy);
            case "Chicken"           -> drawChicken(sx, sy);
            case "Cow"               -> drawCow(sx, sy);
            case "Goblin"            -> drawGoblin(sx, sy);
            case "Tree"              -> drawDecorativeTree(sx, sy);
            case "Oak Tree"          -> drawOakTree(sx, sy);
            case "Willow Tree"       -> drawWillowTree(sx, sy);
            case "Maple Tree"        -> drawMapleTree(sx, sy);
            case "Yew Tree"          -> drawYewTree(sx, sy);
            case "Magic Tree"        -> drawMagicTree(sx, sy);
            case "Fishing Spot"      -> drawFishingSpot(sx, sy);
            case "Cooking Fire"      -> drawFire(sx, sy);
            default                  -> drawUnknownEntity(sx, sy);
        }
        sr.end();
    }

    // -----------------------------------------------------------------------
    // NPC sprite helpers
    // -----------------------------------------------------------------------

    /** Tutorial Guide: green robe, brown hood. */
    private void drawGuide(float sx, float sy) {
        sr.setColor(PANTS);
        sr.rect(sx - 4, sy - 8, 3, 6);
        sr.rect(sx + 1, sy - 8, 3, 6);

        sr.setColor(0.18f, 0.56f, 0.22f, 1f); // green robe
        sr.rect(sx - 5, sy - 2, 10, 8);

        sr.setColor(SKIN);
        sr.rect(sx - 3, sy + 6, 6, 6);

        sr.setColor(0.48f, 0.30f, 0.12f, 1f); // brown hood
        sr.rect(sx - 4, sy + 9, 8, 4);
    }

    /** Combat Instructor: red plate armour, steel helmet. */
    private void drawInstructor(float sx, float sy) {
        sr.setColor(0.55f, 0.55f, 0.58f, 1f); // chainmail legs
        sr.rect(sx - 4, sy - 8, 3, 6);
        sr.rect(sx + 1, sy - 8, 3, 6);

        sr.setColor(0.72f, 0.14f, 0.12f, 1f); // red plate body
        sr.rect(sx - 5, sy - 2, 10, 8);

        sr.setColor(SKIN);
        sr.rect(sx - 2, sy + 7, 4, 4);

        sr.setColor(0.62f, 0.62f, 0.65f, 1f); // steel helmet
        sr.rect(sx - 4, sy + 8, 8, 5);

        sr.setColor(0.12f, 0.12f, 0.12f, 1f); // visor slit
        sr.rect(sx - 3, sy + 9, 6, 1);
    }

    /**
     * Rat (level 1): small grey-brown creature, thin tail, pink ears.
     * Smaller than Giant Rat to distinguish visually.
     */
    private void drawRat(float sx, float sy) {
        // Body — narrow, low to ground
        sr.setColor(0.52f, 0.44f, 0.36f, 1f);
        sr.rect(sx - 4, sy - 2, 8, 4);

        // Head — small, pointed snout
        sr.setColor(0.48f, 0.40f, 0.32f, 1f);
        sr.rect(sx - 2, sy + 2, 5, 3);
        sr.rect(sx + 2, sy + 1, 3, 2); // snout

        // Ears — small pink
        sr.setColor(0.88f, 0.62f, 0.62f, 1f);
        sr.rect(sx - 3, sy + 3, 2, 2);
        sr.rect(sx,     sy + 3, 2, 2);

        // Eyes — tiny black dots
        sr.setColor(0.05f, 0.05f, 0.05f, 1f);
        sr.rect(sx - 1, sy + 2, 1, 1);
        sr.rect(sx + 2, sy + 2, 1, 1);

        // Tail — thin, curving right
        sr.setColor(0.75f, 0.62f, 0.52f, 1f);
        sr.rect(sx + 4, sy,     3, 1);
        sr.rect(sx + 6, sy - 1, 2, 1);
    }

    /**
     * Giant Rat (level 3): noticeably larger and darker than the regular Rat.
     * Broader body, bigger head, thicker tail.
     */
    private void drawGiantRat(float sx, float sy) {
        // Body — wide, dark brown
        sr.setColor(0.40f, 0.26f, 0.14f, 1f);
        sr.rect(sx - 6, sy - 3, 12, 6);

        // Head — large, blocky
        sr.setColor(0.36f, 0.22f, 0.10f, 1f);
        sr.rect(sx - 3, sy + 3, 7, 5);
        sr.rect(sx + 3, sy + 1, 4, 3); // wide snout

        // Ears — larger pink patches
        sr.setColor(0.82f, 0.52f, 0.52f, 1f);
        sr.rect(sx - 5, sy + 5, 3, 3);
        sr.rect(sx + 1, sy + 5, 3, 3);

        // Eyes — small dark red (menacing)
        sr.setColor(0.55f, 0.05f, 0.05f, 1f);
        sr.rect(sx - 1, sy + 3, 2, 2);
        sr.rect(sx + 3, sy + 3, 2, 2);

        // Tail — thick
        sr.setColor(0.62f, 0.45f, 0.30f, 1f);
        sr.rect(sx + 6, sy,     4, 2);
        sr.rect(sx + 9, sy - 2, 3, 2);
    }

    /**
     * Chicken (level 1): white body, red comb, orange beak and feet.
     * Rounded fluffy silhouette.
     */
    private void drawChicken(float sx, float sy) {
        // Legs — thin orange sticks
        sr.setColor(0.90f, 0.55f, 0.10f, 1f);
        sr.rect(sx - 3, sy - 7, 2, 5);
        sr.rect(sx + 1, sy - 7, 2, 5);

        // Body — large white oval (approximated with rects)
        sr.setColor(0.95f, 0.93f, 0.88f, 1f);
        sr.rect(sx - 6, sy - 4, 12, 7);  // main body
        sr.rect(sx - 5, sy + 3, 10, 3);  // rounded top

        // Wing accent — light grey
        sr.setColor(0.78f, 0.76f, 0.72f, 1f);
        sr.rect(sx - 5, sy - 2, 4, 4);
        sr.rect(sx + 1, sy - 2, 4, 4);

        // Head — white circle
        sr.setColor(0.95f, 0.93f, 0.88f, 1f);
        sr.rect(sx - 2, sy + 6, 6, 5);

        // Comb — bright red
        sr.setColor(0.88f, 0.10f, 0.10f, 1f);
        sr.rect(sx - 1, sy + 10, 2, 3);
        sr.rect(sx + 1, sy + 11, 2, 2);
        sr.rect(sx + 3, sy + 10, 2, 3);

        // Beak — orange triangle (rect approximation)
        sr.setColor(0.92f, 0.58f, 0.08f, 1f);
        sr.rect(sx + 3, sy + 7, 3, 2);

        // Eye — small black
        sr.setColor(0.05f, 0.05f, 0.05f, 1f);
        sr.rect(sx + 2, sy + 8, 1, 1);
    }

    /**
     * Cow (level 2): large black-and-white patched body, prominent horns.
     * Noticeably bigger than all other mobs.
     */
    private void drawCow(float sx, float sy) {
        // Legs — thick, black
        sr.setColor(0.15f, 0.15f, 0.15f, 1f);
        sr.rect(sx - 6, sy - 10, 3, 8);
        sr.rect(sx - 1, sy - 10, 3, 8);
        sr.rect(sx + 3, sy - 10, 3, 8);

        // Body — large white base
        sr.setColor(0.92f, 0.92f, 0.90f, 1f);
        sr.rect(sx - 8, sy - 4, 16, 10);

        // Black patches (markings)
        sr.setColor(0.12f, 0.12f, 0.12f, 1f);
        sr.rect(sx - 7, sy - 2, 5, 6);
        sr.rect(sx + 3, sy,     4, 5);
        sr.rect(sx - 2, sy + 3, 4, 3);

        // Head — white, broad
        sr.setColor(0.88f, 0.88f, 0.85f, 1f);
        sr.rect(sx - 4, sy + 6, 10, 7);

        // Black nose patch
        sr.setColor(0.30f, 0.20f, 0.20f, 1f);
        sr.rect(sx + 1, sy + 6, 5, 3);
        // Nostrils
        sr.setColor(0.10f, 0.05f, 0.05f, 1f);
        sr.rect(sx + 2, sy + 7, 1, 1);
        sr.rect(sx + 4, sy + 7, 1, 1);

        // Eyes — dark
        sr.setColor(0.10f, 0.08f, 0.05f, 1f);
        sr.rect(sx - 2, sy + 9, 2, 2);
        sr.rect(sx + 3, sy + 9, 2, 2);

        // Horns — cream/ivory
        sr.setColor(0.88f, 0.82f, 0.62f, 1f);
        sr.rect(sx - 5, sy + 11, 3, 2);
        sr.rect(sx - 6, sy + 12, 2, 3);
        sr.rect(sx + 5, sy + 11, 3, 2);
        sr.rect(sx + 7, sy + 12, 2, 3);

        // Ear — left, pinkish
        sr.setColor(0.82f, 0.60f, 0.58f, 1f);
        sr.rect(sx - 7, sy + 9, 3, 3);
    }

    /**
     * Goblin (level 2): hunched green-skinned creature, big ears, crude armour.
     * Wider stance and squatter proportions than a human.
     */
    private void drawGoblin(float sx, float sy) {
        // Legs — dark leather
        sr.setColor(0.28f, 0.16f, 0.06f, 1f);
        sr.rect(sx - 4, sy - 8, 3, 7);
        sr.rect(sx + 1, sy - 8, 3, 7);

        // Body — dark brown crude armour
        sr.setColor(0.38f, 0.22f, 0.08f, 1f);
        sr.rect(sx - 5, sy - 2, 10, 7);

        // Green belly/skin showing through
        sr.setColor(0.28f, 0.52f, 0.16f, 1f);
        sr.rect(sx - 2, sy,     4, 4);

        // Head — green skin, slightly hunched forward
        sr.setColor(0.30f, 0.55f, 0.18f, 1f);
        sr.rect(sx - 4, sy + 5, 8, 7);

        // Big protruding ears
        sr.setColor(0.24f, 0.46f, 0.14f, 1f);
        sr.rect(sx - 7, sy + 7, 4, 5);
        sr.rect(sx + 3, sy + 7, 4, 5);
        // Inner ear
        sr.setColor(0.40f, 0.65f, 0.25f, 1f);
        sr.rect(sx - 6, sy + 8, 2, 3);
        sr.rect(sx + 4, sy + 8, 2, 3);

        // Eyes — beady yellow
        sr.setColor(0.90f, 0.80f, 0.05f, 1f);
        sr.rect(sx - 2, sy + 8, 2, 2);
        sr.rect(sx + 1, sy + 8, 2, 2);
        // Pupils — black
        sr.setColor(0.02f, 0.02f, 0.02f, 1f);
        sr.rect(sx - 1, sy + 8, 1, 1);
        sr.rect(sx + 2, sy + 8, 1, 1);

        // Crude helmet/hood
        sr.setColor(0.28f, 0.16f, 0.06f, 1f);
        sr.rect(sx - 4, sy + 10, 8, 3);

        // Weapon stub (club/sword held to side)
        sr.setColor(0.55f, 0.55f, 0.58f, 1f);
        sr.rect(sx + 5, sy - 2, 2, 9);
    }

    /** Decorative non-choppable tree used as world dressing. */
    private void drawDecorativeTree(float sx, float sy) {
        sr.setColor(0.34f, 0.22f, 0.11f, 1f);
        sr.rect(sx - 3, sy - 6, 6, 12);

        sr.setColor(0.10f, 0.34f, 0.13f, 1f);
        sr.rect(sx - 13, sy + 2, 26, 8);
        sr.rect(sx - 10, sy + 9, 20, 7);
        sr.rect(sx - 6, sy + 15, 12, 5);

        sr.setColor(0.14f, 0.44f, 0.18f, 1f);
        sr.rect(sx - 9, sy + 4, 18, 4);
    }

    /**
     * Oak tree resource node used by Woodcutting.
     */
    private void drawOakTree(float sx, float sy) {
        // Trunk
        sr.setColor(0.42f, 0.25f, 0.12f, 1f);
        sr.rect(sx - 4, sy - 6, 8, 14);

        // Leaves (simple layered canopy)
        sr.setColor(0.12f, 0.45f, 0.16f, 1f);
        sr.rect(sx - 14, sy + 4, 28, 8);
        sr.rect(sx - 11, sy + 11, 22, 7);
        sr.rect(sx - 7, sy + 17, 14, 6);

        sr.setColor(0.20f, 0.60f, 0.24f, 1f);
        sr.rect(sx - 10, sy + 6, 20, 5);
    }

    private void drawWillowTree(float sx, float sy) {
        sr.setColor(0.38f, 0.24f, 0.11f, 1f);
        sr.rect(sx - 4, sy - 6, 8, 14);
        sr.setColor(0.18f, 0.48f, 0.24f, 1f);
        sr.rect(sx - 15, sy + 3, 30, 7);
        sr.rect(sx - 12, sy + 10, 24, 7);
        sr.rect(sx - 9, sy + 17, 18, 5);
    }

    private void drawMapleTree(float sx, float sy) {
        sr.setColor(0.45f, 0.28f, 0.14f, 1f);
        sr.rect(sx - 4, sy - 6, 8, 14);
        sr.setColor(0.70f, 0.36f, 0.12f, 1f);
        sr.rect(sx - 14, sy + 4, 28, 8);
        sr.rect(sx - 10, sy + 12, 20, 6);
        sr.rect(sx - 6, sy + 18, 12, 4);
    }

    private void drawYewTree(float sx, float sy) {
        sr.setColor(0.30f, 0.20f, 0.11f, 1f);
        sr.rect(sx - 4, sy - 6, 8, 14);
        sr.setColor(0.08f, 0.30f, 0.12f, 1f);
        sr.rect(sx - 13, sy + 4, 26, 9);
        sr.rect(sx - 10, sy + 13, 20, 7);
        sr.rect(sx - 6, sy + 20, 12, 5);
    }

    private void drawMagicTree(float sx, float sy) {
        sr.setColor(0.22f, 0.17f, 0.22f, 1f);
        sr.rect(sx - 4, sy - 6, 8, 14);
        sr.setColor(0.20f, 0.08f, 0.34f, 1f);
        sr.rect(sx - 15, sy + 4, 30, 8);
        sr.rect(sx - 11, sy + 12, 22, 7);
        sr.rect(sx - 7, sy + 19, 14, 5);
    }

    private void drawFishingSpot(float sx, float sy) {
        sr.setColor(0.10f, 0.45f, 0.75f, 1f);
        sr.rect(sx - 10, sy - 5, 20, 8);
        sr.setColor(0.70f, 0.90f, 1f, 0.9f);
        sr.rect(sx - 6, sy + 1, 12, 2);
        sr.setColor(0.95f, 0.95f, 0.95f, 1f);
        sr.rect(sx - 2, sy + 4, 4, 2);
    }

    private void drawFire(float sx, float sy) {
        sr.setColor(0.40f, 0.22f, 0.08f, 1f);
        sr.rect(sx - 8, sy - 5, 16, 4);
        sr.setColor(0.95f, 0.45f, 0.05f, 1f);
        sr.rect(sx - 4, sy - 1, 8, 6);
        sr.setColor(1.00f, 0.80f, 0.25f, 1f);
        sr.rect(sx - 2, sy + 1, 4, 4);
    }

    /** Unknown entity marker used to make missing renderer cases obvious in-game. */
    private void drawUnknownEntity(float sx, float sy) {
        sr.setColor(1.00f, 0.00f, 1.00f, 1f);
        sr.circle(sx, sy, 10f);
        sr.setColor(0.80f, 0.00f, 0.80f, 1f);
        sr.circle(sx, sy, 6f);
    }

    // -----------------------------------------------------------------------
    // Ground items
    // -----------------------------------------------------------------------

    /**
     * Render a ground item as a small coloured diamond slightly above the tile.
     * Must be called outside any begin/end block — opens and closes its own.
     */
    public void renderGroundItem(int tileX, int tileY, int itemId, int quantity) {
        float sx = worldToScreenX(tileX, tileY);
        float sy = worldToScreenY(tileX, tileY) + 4f;  // slightly above ground

        final Color c;
        switch (itemId) {
            case 995  -> c = Color.YELLOW;                           // Coins -- gold
            case 526  -> c = new Color(0.88f, 0.80f, 0.62f, 1f);   // Bones -- off-white tan
            case 314  -> c = new Color(0.88f, 0.90f, 0.96f, 1f);   // Feathers -- pale silver-white
            case 1000 -> c = new Color(0.50f, 0.18f, 0.18f, 1f);   // Rat's tail -- dark maroon
            case 2134 -> c = new Color(0.80f, 0.28f, 0.28f, 1f);   // Raw rat meat -- raw-meat red
            case 2138 -> c = new Color(0.90f, 0.65f, 0.58f, 1f);   // Raw chicken -- pale flesh pink
            case 2142 -> c = new Color(0.42f, 0.26f, 0.10f, 1f);   // Cowhide -- dark leather brown
            default   -> c = new Color(0.80f, 0.50f, 0.20f, 1f);   // generic brown
        }

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(c);
        // Small diamond: two triangles forming a 8-wide × 8-tall diamond
        sr.triangle(sx,      sy + 4,  sx + 4f, sy,  sx - 4f, sy);   // upper half
        sr.triangle(sx,      sy - 4,  sx + 4f, sy,  sx - 4f, sy);   // lower half
        sr.end();
    }

    // -----------------------------------------------------------------------
    // Health bar
    // -----------------------------------------------------------------------

    /**
     * Thin red/green bar above an entity — call when HP < max.
     */
    public void renderHealthBar(int tileX, int tileY, int health, int maxHealth) {
        renderHealthBar((float) tileX, (float) tileY, health, maxHealth);
    }

    public void renderHealthBar(float tileX, float tileY, int health, int maxHealth) {
        if (maxHealth <= 0) return;
        float sx = worldToScreenX(tileX, tileY);
        float sy = worldToScreenY(tileX, tileY);
        float barY  = sy + 16;
        float barW  = 20f;
        float barH  = 3f;
        float fill  = (float) health / maxHealth;

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.2f, 0.0f, 0.0f, 1f);
        sr.rect(sx - barW / 2, barY, barW, barH);
        sr.setColor(0.0f, 0.8f, 0.0f, 1f);
        sr.rect(sx - barW / 2, barY, barW * fill, barH);
        sr.end();
    }
}
