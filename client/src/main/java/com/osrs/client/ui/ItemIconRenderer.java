package com.osrs.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public final class ItemIconRenderer {

    private static final Color BRONZE_COLOR  = new Color(0.55f, 0.46f, 0.28f, 1f);
    private static final Color IRON_COLOR    = new Color(0.30f, 0.30f, 0.30f, 1f);
    private static final Color STEEL_COLOR   = new Color(0.60f, 0.60f, 0.60f, 1f);
    private static final Color BLACK_COLOR   = new Color(0.16f, 0.16f, 0.16f, 1f);
    private static final Color MITHRIL_COLOR = new Color(0.20f, 0.55f, 0.85f, 1f);
    private static final Color ADAMANT_COLOR = new Color(0.20f, 0.60f, 0.20f, 1f);
    private static final Color RUNE_COLOR    = new Color(0.40f, 0.65f, 0.75f, 1f);
    private static final Color DRAGON_COLOR  = new Color(1.00f, 0.85f, 0.00f, 1f);
    private static final Color WOOD_COLOR    = new Color(0.48f, 0.30f, 0.12f, 1f);
    private static final Color DEFAULT_ITEM_COLOR = new Color(0.60f, 0.60f, 0.60f, 1f);

    private ItemIconRenderer() {
    }

    public static void drawItemIcon(ShapeRenderer sr, float slotLeft, float slotBottom, int itemId) {
        float iconX = slotLeft + 4;
        float iconY = slotBottom + 4;

        switch (itemId) {
            // ── Swords ────────────────────────────────────────────────────
            case 1277 -> drawSwordIcon(sr, iconX, iconY, BRONZE_COLOR);
            // ── Scimitars ─────────────────────────────────────────────────
            case 1321 -> drawScimitarIcon(sr, iconX, iconY, BRONZE_COLOR);
            case 1323 -> drawScimitarIcon(sr, iconX, iconY, IRON_COLOR);
            case 1325 -> drawScimitarIcon(sr, iconX, iconY, STEEL_COLOR);
            case 1327 -> drawScimitarIcon(sr, iconX, iconY, BLACK_COLOR);
            case 1329 -> drawScimitarIcon(sr, iconX, iconY, MITHRIL_COLOR);
            case 1331 -> drawScimitarIcon(sr, iconX, iconY, ADAMANT_COLOR);
            case 1333 -> drawScimitarIcon(sr, iconX, iconY, RUNE_COLOR);
            case 4587 -> drawScimitarIcon(sr, iconX, iconY, DRAGON_COLOR);
            // ── Longswords ────────────────────────────────────────────────
            case 1291 -> drawLongswordIcon(sr, iconX, iconY, BRONZE_COLOR);
            case 1293 -> drawLongswordIcon(sr, iconX, iconY, IRON_COLOR);
            case 1295 -> drawLongswordIcon(sr, iconX, iconY, STEEL_COLOR);
            case 1297 -> drawLongswordIcon(sr, iconX, iconY, BLACK_COLOR);
            case 1299 -> drawLongswordIcon(sr, iconX, iconY, MITHRIL_COLOR);
            case 1301 -> drawLongswordIcon(sr, iconX, iconY, ADAMANT_COLOR);
            case 1303 -> drawLongswordIcon(sr, iconX, iconY, RUNE_COLOR);
            case 1305 -> drawLongswordIcon(sr, iconX, iconY, DRAGON_COLOR);
            // ── Full helms ────────────────────────────────────────────────
            case 1115  -> drawFullHelmIcon(sr, iconX, iconY, BRONZE_COLOR);
            case 1153  -> drawFullHelmIcon(sr, iconX, iconY, IRON_COLOR);
            case 1157  -> drawFullHelmIcon(sr, iconX, iconY, STEEL_COLOR);
            case 1159  -> drawFullHelmIcon(sr, iconX, iconY, BLACK_COLOR);
            case 1163  -> drawFullHelmIcon(sr, iconX, iconY, MITHRIL_COLOR);
            case 1161  -> drawFullHelmIcon(sr, iconX, iconY, ADAMANT_COLOR);
            case 1165  -> drawFullHelmIcon(sr, iconX, iconY, RUNE_COLOR);
            case 11335 -> drawFullHelmIcon(sr, iconX, iconY, DRAGON_COLOR);
            // ── Platebodies ───────────────────────────────────────────────
            case 1119  -> drawPlatebodyIcon(sr, iconX, iconY, BRONZE_COLOR);
            case 2000  -> drawPlatebodyIcon(sr, iconX, iconY, IRON_COLOR);
            case 1085  -> drawPlatebodyIcon(sr, iconX, iconY, STEEL_COLOR);
            case 1125  -> drawPlatebodyIcon(sr, iconX, iconY, BLACK_COLOR);
            case 1129  -> drawPlatebodyIcon(sr, iconX, iconY, MITHRIL_COLOR);
            case 1133  -> drawPlatebodyIcon(sr, iconX, iconY, ADAMANT_COLOR);
            case 1127  -> drawPlatebodyIcon(sr, iconX, iconY, RUNE_COLOR);
            case 3140  -> drawPlatebodyIcon(sr, iconX, iconY, DRAGON_COLOR);
            // ── Platelegs ─────────────────────────────────────────────────
            case 1067  -> drawPlatelegsIcon(sr, iconX, iconY, BRONZE_COLOR);
            case 1069  -> drawPlatelegsIcon(sr, iconX, iconY, IRON_COLOR);
            case 1071  -> drawPlatelegsIcon(sr, iconX, iconY, STEEL_COLOR);
            case 1073  -> drawPlatelegsIcon(sr, iconX, iconY, BLACK_COLOR);
            case 1075  -> drawPlatelegsIcon(sr, iconX, iconY, MITHRIL_COLOR);
            case 1077  -> drawPlatelegsIcon(sr, iconX, iconY, ADAMANT_COLOR);
            case 1079  -> drawPlatelegsIcon(sr, iconX, iconY, RUNE_COLOR);
            case 4087  -> drawPlatelegsIcon(sr, iconX, iconY, DRAGON_COLOR);
            // ── Sq shields ────────────────────────────────────────────────
            case 1173  -> drawSqShieldIcon(sr, iconX, iconY, WOOD_COLOR);
            case 1175  -> drawSqShieldIcon(sr, iconX, iconY, BRONZE_COLOR);
            case 1177  -> drawSqShieldIcon(sr, iconX, iconY, IRON_COLOR);
            case 1193  -> drawSqShieldIcon(sr, iconX, iconY, STEEL_COLOR);
            case 1195  -> drawSqShieldIcon(sr, iconX, iconY, BLACK_COLOR);
            case 1197  -> drawSqShieldIcon(sr, iconX, iconY, MITHRIL_COLOR);
            case 1199  -> drawSqShieldIcon(sr, iconX, iconY, ADAMANT_COLOR);
            case 1185  -> drawSqShieldIcon(sr, iconX, iconY, RUNE_COLOR);
            case 11286 -> drawSqShieldIcon(sr, iconX, iconY, DRAGON_COLOR);
            // ── Woodcutting logs ──────────────────────────────────────────
            case 1511 -> drawLogsIcon(sr, iconX, iconY);
            case 1521 -> drawOakLogsIcon(sr, iconX, iconY);
            case 1522 -> drawWillowLogsIcon(sr, iconX, iconY);
            case 1523 -> drawMapleLogsIcon(sr, iconX, iconY);
            case 1524 -> drawYewLogsIcon(sr, iconX, iconY);
            case 1525 -> drawMagicLogsIcon(sr, iconX, iconY);
            case 1526 -> drawTeakLogsIcon(sr, iconX, iconY);
            case 1527 -> drawMahoganyLogsIcon(sr, iconX, iconY);
            // ── Axes (WC tool) ────────────────────────────────────────────
            case 1351 -> drawAxeIcon(sr, iconX, iconY, BRONZE_COLOR);
            case 1349 -> drawAxeIcon(sr, iconX, iconY, IRON_COLOR);
            case 1353 -> drawAxeIcon(sr, iconX, iconY, STEEL_COLOR);
            case 1361 -> drawAxeIcon(sr, iconX, iconY, new Color(0.16f, 0.16f, 0.16f, 1f));
            case 1355 -> drawAxeIcon(sr, iconX, iconY, MITHRIL_COLOR);
            case 1357 -> drawAxeIcon(sr, iconX, iconY, ADAMANT_COLOR);
            case 1359 -> drawAxeIcon(sr, iconX, iconY, RUNE_COLOR);
            case 6739 -> drawAxeIcon(sr, iconX, iconY, DRAGON_COLOR);
            // ── Mining ores ───────────────────────────────────────────────
            case 436  -> drawOreIcon(sr, iconX, iconY, new Color(0.78f, 0.42f, 0.20f, 1f), new Color(0.94f, 0.64f, 0.38f, 1f)); // Copper
            case 438  -> drawOreIcon(sr, iconX, iconY, new Color(0.60f, 0.64f, 0.68f, 1f), new Color(0.84f, 0.86f, 0.90f, 1f)); // Tin
            case 440  -> drawOreIcon(sr, iconX, iconY, new Color(0.60f, 0.42f, 0.36f, 1f), new Color(0.78f, 0.58f, 0.48f, 1f)); // Iron
            case 442  -> drawOreIcon(sr, iconX, iconY, new Color(0.72f, 0.76f, 0.82f, 1f), new Color(0.94f, 0.96f, 1.00f, 1f)); // Silver
            case 453  -> drawOreIcon(sr, iconX, iconY, new Color(0.20f, 0.20f, 0.22f, 1f), new Color(0.38f, 0.38f, 0.42f, 1f)); // Coal
            case 444  -> drawOreIcon(sr, iconX, iconY, new Color(0.90f, 0.74f, 0.08f, 1f), new Color(1.00f, 0.95f, 0.48f, 1f)); // Gold
            case 447  -> drawOreIcon(sr, iconX, iconY, new Color(0.20f, 0.48f, 0.88f, 1f), new Color(0.50f, 0.76f, 1.00f, 1f)); // Mithril
            case 449  -> drawOreIcon(sr, iconX, iconY, new Color(0.20f, 0.62f, 0.28f, 1f), new Color(0.46f, 0.86f, 0.54f, 1f)); // Adamantite
            case 451  -> drawOreIcon(sr, iconX, iconY, new Color(0.26f, 0.72f, 0.82f, 1f), new Color(0.56f, 0.92f, 0.98f, 1f)); // Runite
            // ── Pickaxes ──────────────────────────────────────────────────
            case 1265  -> drawPickaxeIcon(sr, iconX, iconY, BRONZE_COLOR);
            case 1267  -> drawPickaxeIcon(sr, iconX, iconY, IRON_COLOR);
            case 1269  -> drawPickaxeIcon(sr, iconX, iconY, STEEL_COLOR);
            case 12297 -> drawPickaxeIcon(sr, iconX, iconY, new Color(0.16f, 0.16f, 0.16f, 1f));
            case 1273  -> drawPickaxeIcon(sr, iconX, iconY, MITHRIL_COLOR);
            case 1271  -> drawPickaxeIcon(sr, iconX, iconY, ADAMANT_COLOR);
            case 1275  -> drawPickaxeIcon(sr, iconX, iconY, RUNE_COLOR);
            case 11920 -> drawPickaxeIcon(sr, iconX, iconY, DRAGON_COLOR);
            // ── Fishing tools and fish ────────────────────────────────────
            case 303 -> drawSmallNetIcon(sr, iconX, iconY);
            case 307 -> drawFishingRodIcon(sr, iconX, iconY);
            case 313 -> drawFishingBaitIcon(sr, iconX, iconY);
            case 309 -> drawFlyFishingRodIcon(sr, iconX, iconY);
            case 301 -> drawLobsterPotIcon(sr, iconX, iconY);
            case 311 -> drawHarpoonIcon(sr, iconX, iconY);
            case 590 -> drawFireIcon(sr, iconX, iconY);
            case 2138 -> drawRawChickenIcon(sr, iconX, iconY);
            case 2132, 2134 -> drawRawMeatIcon(sr, iconX, iconY);
            case 2140 -> drawCookedChickenIcon(sr, iconX, iconY);
            case 2148 -> drawCookedMeatIcon(sr, iconX, iconY);
            case 317 -> drawRawShrimpsIcon(sr, iconX, iconY);
            case 321 -> drawAnchoviesIcon(sr, iconX, iconY);
            case 327 -> drawSardineIcon(sr, iconX, iconY);
            case 345 -> drawHerringIcon(sr, iconX, iconY);
            case 335 -> drawTroutIcon(sr, iconX, iconY);
            case 331 -> drawSalmonIcon(sr, iconX, iconY);
            case 349 -> drawPikeIcon(sr, iconX, iconY);
            case 377 -> drawLobsterIcon(sr, iconX, iconY);
            case 359 -> drawTunaIcon(sr, iconX, iconY);
            case 371 -> drawSwordfishIcon(sr, iconX, iconY);
            case 315 -> drawCookedShrimpsIcon(sr, iconX, iconY);
            case 319 -> drawCookedAnchoviesIcon(sr, iconX, iconY);
            case 325 -> drawCookedSardineIcon(sr, iconX, iconY);
            case 347 -> drawCookedHerringIcon(sr, iconX, iconY);
            case 333 -> drawCookedTroutIcon(sr, iconX, iconY);
            case 351 -> drawCookedPikeIcon(sr, iconX, iconY);
            case 329 -> drawCookedSalmonIcon(sr, iconX, iconY);
            case 361 -> drawCookedTunaIcon(sr, iconX, iconY);
            case 379 -> drawCookedLobsterIcon(sr, iconX, iconY);
            case 373 -> drawCookedSwordfishIcon(sr, iconX, iconY);
            case 385 -> drawSharkIcon(sr, iconX, iconY);
            case 7954 -> drawBurntShrimpsIcon(sr, iconX, iconY);
            case 526  -> drawBonesIcon(sr, iconX, iconY);
            case 995, 1000 -> drawCoinsIcon(sr, iconX, iconY);
            default -> drawGenericIcon(sr, iconX, iconY, getItemIconColor(itemId));
        }
    }

    private static void drawGenericIcon(ShapeRenderer sr, float x, float y, Color c) {
        sr.setColor(c);
        sr.rect(x + 3, y + 3, 26, 26);
        float hi = Math.min(c.r * 1.3f, 1f);
        float hig = Math.min(c.g * 1.3f, 1f);
        float hib = Math.min(c.b * 1.3f, 1f);
        sr.setColor(hi, hig, hib, 1f);
        sr.rect(x + 3, y + 23, 26, 6);
        sr.rect(x + 3, y + 3, 6, 26);
    }

    private static void drawLogsIcon(ShapeRenderer sr, float x, float y) {
        sr.setColor(0.48f, 0.28f, 0.14f, 1f);
        sr.rect(x + 4, y + 5, 16, 4);
        sr.rect(x + 3, y + 10, 18, 4);
        sr.rect(x + 5, y + 15, 14, 4);
        sr.setColor(0.66f, 0.45f, 0.28f, 1f);
        sr.rect(x + 18, y + 5, 2, 4);
        sr.rect(x + 19, y + 10, 2, 4);
        sr.rect(x + 17, y + 15, 2, 4);
    }

    private static void drawAxeIcon(ShapeRenderer sr, float x, float y, Color headColor) {
        // Handle
        sr.setColor(0.58f, 0.36f, 0.18f, 1f);
        sr.rect(x + 11, y + 4, 3, 16);
        // Blade
        sr.setColor(headColor);
        sr.rect(x + 6, y + 14, 10, 5);
        // Blade back
        float hi = Math.min(headColor.r * 0.8f, 1f);
        float hig = Math.min(headColor.g * 0.8f, 1f);
        float hib = Math.min(headColor.b * 0.8f, 1f);
        sr.setColor(hi, hig, hib, 1f);
        sr.rect(x + 6, y + 12, 6, 2);
    }

    /** Ore nugget: asymmetric rock shape with highlight, used for all mining ores. */
    private static void drawOreIcon(ShapeRenderer sr, float x, float y, Color body, Color highlight) {
        // Main ore body
        sr.setColor(body);
        sr.rect(x + 4, y + 6, 18, 12);
        sr.rect(x + 8, y + 4, 10, 4);   // upper peak
        sr.rect(x + 6, y + 16, 12, 4);  // lower ledge
        // Shadow edge
        float dr = Math.max(body.r - 0.18f, 0f);
        float dg = Math.max(body.g - 0.18f, 0f);
        float db = Math.max(body.b - 0.18f, 0f);
        sr.setColor(dr, dg, db, 1f);
        sr.rect(x + 4, y + 6, 4, 4);    // bottom-left shadow notch
        // Highlight facet
        sr.setColor(highlight);
        sr.rect(x + 9, y + 9, 5, 5);
        sr.rect(x + 12, y + 6, 3, 3);
    }

    /** Pickaxe: horizontal pick head on a vertical handle. Different shape from axe. */
    private static void drawPickaxeIcon(ShapeRenderer sr, float x, float y, Color headColor) {
        // Handle
        sr.setColor(0.58f, 0.36f, 0.18f, 1f);
        sr.rect(x + 12, y + 3, 3, 17);
        // Horizontal pick head
        sr.setColor(headColor);
        sr.rect(x + 4, y + 15, 18, 4);
        // Left tapered pick tip
        sr.rect(x + 2, y + 14, 4, 3);
        // Right adze end (slightly wider)
        sr.rect(x + 20, y + 13, 4, 5);
        // Highlight on pick head
        float hi = Math.min(headColor.r + 0.22f, 1f);
        float hig = Math.min(headColor.g + 0.22f, 1f);
        float hib = Math.min(headColor.b + 0.22f, 1f);
        sr.setColor(hi, hig, hib, 1f);
        sr.rect(x + 6, y + 17, 6, 2);
    }

    /**
     * Sword icon: straight diagonal blade pointing top-right, with a crossguard.
     * Distinct from the scimitar (curved blade) and longsword (longer blade).
     */
    private static void drawSwordIcon(ShapeRenderer sr, float x, float y, Color bladeColor) {
        // Grip (handle)
        sr.setColor(0.48f, 0.28f, 0.12f, 1f);
        sr.rect(x + 3,  y + 3,  4, 4);
        // Crossguard
        sr.setColor(bladeColor);
        sr.rect(x + 5,  y + 6,  14, 3);
        // Blade — straight, running bottom-left to top-right
        sr.rect(x + 8,  y + 8,  3, 14);
        // Highlight on blade
        float hi = Math.min(bladeColor.r + 0.25f, 1f);
        float hig = Math.min(bladeColor.g + 0.25f, 1f);
        float hib = Math.min(bladeColor.b + 0.25f, 1f);
        sr.setColor(hi, hig, hib, 1f);
        sr.rect(x + 9,  y + 9,  1, 12);
    }

    /**
     * Scimitar icon: curved blade — C-shaped arc suggesting the classic OSRS curved sword.
     * Uses filled rectangles in a staircase to approximate a curve.
     */
    private static void drawScimitarIcon(ShapeRenderer sr, float x, float y, Color bladeColor) {
        // Grip
        sr.setColor(0.48f, 0.28f, 0.12f, 1f);
        sr.rect(x + 3, y + 3, 4, 4);
        // Crossguard
        sr.setColor(bladeColor);
        sr.rect(x + 4, y + 6, 12, 3);
        // Blade: staircase approximating the scimitar's back-curve
        sr.rect(x + 13, y + 7, 5, 3);   // horizontal sweep start
        sr.rect(x + 16, y + 9, 4, 3);   // curving upward
        sr.rect(x + 17, y + 11, 4, 3);
        sr.rect(x + 16, y + 13, 4, 3);
        sr.rect(x + 14, y + 15, 4, 3);  // curving back
        sr.rect(x + 11, y + 17, 4, 3);
        sr.rect(x + 8,  y + 18, 4, 3);  // tip pointing left
        // Highlight
        float hi = Math.min(bladeColor.r + 0.25f, 1f);
        float hig = Math.min(bladeColor.g + 0.25f, 1f);
        float hib = Math.min(bladeColor.b + 0.25f, 1f);
        sr.setColor(hi, hig, hib, 1f);
        sr.rect(x + 14, y + 9, 1, 2);
        sr.rect(x + 13, y + 16, 1, 2);
    }

    /**
     * Longsword icon: taller than the sword, same shape but with a longer blade running
     * vertically. Distinguished from the sword by blade length.
     */
    private static void drawLongswordIcon(ShapeRenderer sr, float x, float y, Color bladeColor) {
        // Grip
        sr.setColor(0.48f, 0.28f, 0.12f, 1f);
        sr.rect(x + 3, y + 2, 4, 5);
        // Crossguard — wider than sword's
        sr.setColor(bladeColor);
        sr.rect(x + 3, y + 6, 17, 3);
        // Blade — longer vertical blade
        sr.rect(x + 7, y + 8, 4, 18);
        // Highlight
        float hi = Math.min(bladeColor.r + 0.25f, 1f);
        float hig = Math.min(bladeColor.g + 0.25f, 1f);
        float hib = Math.min(bladeColor.b + 0.25f, 1f);
        sr.setColor(hi, hig, hib, 1f);
        sr.rect(x + 8, y + 9, 1, 16);
    }

    /**
     * Full helm: bucket-shaped helm with a visor slit.
     * Distinct from the platebody (T-shape) and platelegs (trousers shape).
     */
    private static void drawFullHelmIcon(ShapeRenderer sr, float x, float y, Color c) {
        float hi = Math.min(c.r + 0.22f, 1f);
        float hig = Math.min(c.g + 0.22f, 1f);
        float hib = Math.min(c.b + 0.22f, 1f);
        float dr = Math.max(c.r - 0.18f, 0f);
        float dg = Math.max(c.g - 0.18f, 0f);
        float db = Math.max(c.b - 0.18f, 0f);
        // Main face plate
        sr.setColor(c);
        sr.rect(x + 4, y + 5, 20, 14);
        // Dome (top, slightly narrower)
        sr.rect(x + 6, y + 18, 16, 6);
        // Left and right cheek guards
        sr.rect(x + 2, y + 8, 4, 11);
        sr.rect(x + 22, y + 8, 4, 11);
        // Chin guard (bottom rim, slightly wider)
        sr.rect(x + 3, y + 3, 22, 4);
        // Dome highlight
        sr.setColor(hi, hig, hib, 1f);
        sr.rect(x + 8, y + 20, 12, 3);
        // Visor slit (darker, over face plate)
        sr.setColor(dr, dg, db, 1f);
        sr.rect(x + 5, y + 13, 18, 3);
    }

    /**
     * Platebody: T-shaped chest silhouette with shoulder extensions.
     * Distinct from the helm (dome) and legs (trouser shape).
     */
    private static void drawPlatebodyIcon(ShapeRenderer sr, float x, float y, Color c) {
        float hi = Math.min(c.r + 0.22f, 1f);
        float hig = Math.min(c.g + 0.22f, 1f);
        float hib = Math.min(c.b + 0.22f, 1f);
        // Main torso
        sr.setColor(c);
        sr.rect(x + 5, y + 4, 18, 16);
        // Shoulder extensions (left and right arms at top of torso)
        sr.rect(x + 1, y + 18, 6, 6);
        sr.rect(x + 21, y + 18, 6, 6);
        // Collar area (top center, slightly narrower)
        sr.rect(x + 8, y + 20, 12, 4);
        // Chest highlight stripe
        sr.setColor(hi, hig, hib, 1f);
        sr.rect(x + 7, y + 16, 14, 3);
        sr.rect(x + 3, y + 20, 4, 3);
        sr.rect(x + 21, y + 20, 4, 3);
    }

    /**
     * Platelegs: trouser silhouette with a wide hip band and two leg columns.
     * The gap between legs is left implicit by not drawing that region.
     */
    private static void drawPlatelegsIcon(ShapeRenderer sr, float x, float y, Color c) {
        float hi = Math.min(c.r + 0.22f, 1f);
        float hig = Math.min(c.g + 0.22f, 1f);
        float hib = Math.min(c.b + 0.22f, 1f);
        // Hip/waist band
        sr.setColor(c);
        sr.rect(x + 3, y + 17, 22, 7);
        // Left leg column
        sr.rect(x + 3, y + 4, 9, 15);
        // Right leg column
        sr.rect(x + 16, y + 4, 9, 15);
        // Waist highlight
        sr.setColor(hi, hig, hib, 1f);
        sr.rect(x + 5, y + 19, 8, 3);
        // Knee highlights
        sr.rect(x + 5, y + 14, 4, 2);
        sr.rect(x + 18, y + 14, 4, 2);
    }

    /**
     * Sq shield: square shield face with a central boss and highlight quad.
     * Distinct from all weapons (which are elongated) and armour (which have limb shapes).
     */
    private static void drawSqShieldIcon(ShapeRenderer sr, float x, float y, Color c) {
        float hi = Math.min(c.r + 0.22f, 1f);
        float hig = Math.min(c.g + 0.22f, 1f);
        float hib = Math.min(c.b + 0.22f, 1f);
        float dr = Math.max(c.r - 0.15f, 0f);
        float dg = Math.max(c.g - 0.15f, 0f);
        float db = Math.max(c.b - 0.15f, 0f);
        // Main shield face
        sr.setColor(c);
        sr.rect(x + 3, y + 3, 22, 22);
        // Upper-left highlight quadrant
        sr.setColor(hi, hig, hib, 1f);
        sr.rect(x + 4, y + 16, 10, 7);
        // Central boss stud (slightly darker, square)
        sr.setColor(dr, dg, db, 1f);
        sr.rect(x + 10, y + 10, 8, 8);
        // Boss highlight
        sr.setColor(hi, hig, hib, 1f);
        sr.rect(x + 12, y + 14, 4, 2);
    }

    private static void drawTeakLogsIcon(ShapeRenderer sr, float x, float y) {
        sr.setColor(0.62f, 0.38f, 0.18f, 1f);
        sr.rect(x + 4, y + 5, 16, 4);
        sr.rect(x + 3, y + 10, 18, 4);
        sr.rect(x + 5, y + 15, 14, 4);
        sr.setColor(0.82f, 0.58f, 0.32f, 1f);
        sr.rect(x + 18, y + 5, 2, 4);
        sr.rect(x + 19, y + 10, 2, 4);
        sr.rect(x + 17, y + 15, 2, 4);
    }

    private static void drawMahoganyLogsIcon(ShapeRenderer sr, float x, float y) {
        sr.setColor(0.48f, 0.22f, 0.12f, 1f);
        sr.rect(x + 4, y + 5, 16, 4);
        sr.rect(x + 3, y + 10, 18, 4);
        sr.rect(x + 5, y + 15, 14, 4);
        sr.setColor(0.68f, 0.36f, 0.22f, 1f);
        sr.rect(x + 18, y + 5, 2, 4);
        sr.rect(x + 19, y + 10, 2, 4);
        sr.rect(x + 17, y + 15, 2, 4);
    }

    private static void drawOakLogsIcon(ShapeRenderer sr, float x, float y) {
        sr.setColor(0.58f, 0.34f, 0.16f, 1f);
        sr.rect(x + 4, y + 5, 16, 4);
        sr.rect(x + 3, y + 10, 18, 4);
        sr.rect(x + 5, y + 15, 14, 4);
        sr.setColor(0.74f, 0.52f, 0.30f, 1f);
        sr.rect(x + 18, y + 5, 2, 4);
        sr.rect(x + 19, y + 10, 2, 4);
        sr.rect(x + 17, y + 15, 2, 4);
    }

    private static void drawWillowLogsIcon(ShapeRenderer sr, float x, float y) {
        sr.setColor(0.46f, 0.32f, 0.20f, 1f);
        sr.rect(x + 4, y + 5, 16, 4);
        sr.rect(x + 3, y + 10, 18, 4);
        sr.rect(x + 5, y + 15, 14, 4);
        sr.setColor(0.60f, 0.48f, 0.34f, 1f);
        sr.rect(x + 18, y + 5, 2, 4);
        sr.rect(x + 19, y + 10, 2, 4);
        sr.rect(x + 17, y + 15, 2, 4);
    }

    private static void drawMapleLogsIcon(ShapeRenderer sr, float x, float y) {
        sr.setColor(0.68f, 0.36f, 0.14f, 1f);
        sr.rect(x + 4, y + 5, 16, 4);
        sr.rect(x + 3, y + 10, 18, 4);
        sr.rect(x + 5, y + 15, 14, 4);
        sr.setColor(0.84f, 0.56f, 0.28f, 1f);
        sr.rect(x + 18, y + 5, 2, 4);
        sr.rect(x + 19, y + 10, 2, 4);
        sr.rect(x + 17, y + 15, 2, 4);
    }

    private static void drawYewLogsIcon(ShapeRenderer sr, float x, float y) {
        sr.setColor(0.30f, 0.26f, 0.16f, 1f);
        sr.rect(x + 4, y + 5, 16, 4);
        sr.rect(x + 3, y + 10, 18, 4);
        sr.rect(x + 5, y + 15, 14, 4);
        sr.setColor(0.44f, 0.38f, 0.24f, 1f);
        sr.rect(x + 18, y + 5, 2, 4);
        sr.rect(x + 19, y + 10, 2, 4);
        sr.rect(x + 17, y + 15, 2, 4);
    }

    private static void drawMagicLogsIcon(ShapeRenderer sr, float x, float y) {
        sr.setColor(0.36f, 0.20f, 0.42f, 1f);
        sr.rect(x + 4, y + 5, 16, 4);
        sr.rect(x + 3, y + 10, 18, 4);
        sr.rect(x + 5, y + 15, 14, 4);
        sr.setColor(0.54f, 0.32f, 0.62f, 1f);
        sr.rect(x + 18, y + 5, 2, 4);
        sr.rect(x + 19, y + 10, 2, 4);
        sr.rect(x + 17, y + 15, 2, 4);
    }

    private static void drawSmallNetIcon(ShapeRenderer sr, float x, float y) {
        sr.setColor(0.70f, 0.62f, 0.45f, 1f);
        sr.rect(x + 4, y + 4, 2, 16);
        sr.setColor(0.82f, 0.76f, 0.60f, 1f);
        sr.rect(x + 8, y + 6, 12, 2);
        sr.rect(x + 8, y + 10, 12, 2);
        sr.rect(x + 8, y + 14, 12, 2);
        sr.rect(x + 10, y + 6, 2, 10);
        sr.rect(x + 14, y + 6, 2, 10);
        sr.rect(x + 18, y + 6, 2, 10);
    }

    private static void drawFishingRodIcon(ShapeRenderer sr, float x, float y) {
        sr.setColor(0.58f, 0.36f, 0.16f, 1f);
        sr.rect(x + 8, y + 4, 2, 19);
        sr.setColor(0.75f, 0.56f, 0.28f, 1f);
        sr.rect(x + 10, y + 18, 11, 2);
        sr.setColor(0.90f, 0.90f, 0.85f, 1f);
        sr.rect(x + 20, y + 11, 1, 7);
    }

    private static void drawFishingBaitIcon(ShapeRenderer sr, float x, float y) {
        sr.setColor(0.62f, 0.30f, 0.20f, 1f);
        sr.rect(x + 8, y + 8, 5, 4);
        sr.rect(x + 13, y + 10, 4, 3);
        sr.setColor(0.44f, 0.22f, 0.14f, 1f);
        sr.rect(x + 10, y + 13, 5, 2);
        sr.setColor(0.74f, 0.46f, 0.28f, 1f);
        sr.rect(x + 15, y + 7, 3, 2);
    }

    private static void drawFlyFishingRodIcon(ShapeRenderer sr, float x, float y) {
        drawFishingRodIcon(sr, x, y);
        sr.setColor(0.92f, 0.88f, 0.42f, 1f);
        sr.rect(x + 18, y + 10, 2, 2);
        sr.setColor(0.80f, 0.94f, 0.96f, 1f);
        sr.rect(x + 20, y + 9, 2, 1);
    }

    private static void drawLobsterPotIcon(ShapeRenderer sr, float x, float y) {
        sr.setColor(0.50f, 0.34f, 0.18f, 1f);
        sr.rect(x + 7, y + 6, 14, 12);
        sr.setColor(0.66f, 0.48f, 0.28f, 1f);
        sr.rect(x + 7, y + 16, 14, 2);
        sr.rect(x + 7, y + 10, 14, 1);
        sr.rect(x + 11, y + 6, 1, 12);
        sr.rect(x + 16, y + 6, 1, 12);
    }

    private static void drawHarpoonIcon(ShapeRenderer sr, float x, float y) {
        sr.setColor(0.66f, 0.44f, 0.22f, 1f);
        sr.rect(x + 7, y + 5, 2, 17);
        sr.setColor(0.78f, 0.82f, 0.88f, 1f);
        sr.rect(x + 8, y + 18, 11, 2);
        sr.rect(x + 18, y + 16, 2, 6);
    }

    private static void drawFireIcon(ShapeRenderer sr, float x, float y) {
        sr.setColor(0.30f, 0.22f, 0.14f, 1f);
        sr.rect(x + 4, y + 6, 18, 3);
        sr.rect(x + 5, y + 10, 16, 2);
        sr.setColor(0.92f, 0.34f, 0.10f, 1f);
        sr.rect(x + 10, y + 12, 6, 10);
        sr.setColor(1f, 0.66f, 0.14f, 1f);
        sr.rect(x + 11, y + 14, 4, 6);
    }

    private static void drawRawChickenIcon(ShapeRenderer sr, float x, float y) {
        sr.setColor(0.90f, 0.78f, 0.68f, 1f);
        sr.rect(x + 7, y + 8, 12, 10);
        sr.setColor(0.80f, 0.64f, 0.54f, 1f);
        sr.rect(x + 18, y + 10, 4, 3);
        sr.setColor(0.96f, 0.88f, 0.80f, 1f);
        sr.rect(x + 10, y + 14, 5, 2);
    }

    private static void drawRawMeatIcon(ShapeRenderer sr, float x, float y) {
        sr.setColor(0.82f, 0.18f, 0.20f, 1f);
        sr.rect(x + 7, y + 8, 13, 10);
        sr.setColor(0.62f, 0.10f, 0.12f, 1f);
        sr.rect(x + 8, y + 9, 11, 3);
        sr.setColor(0.96f, 0.78f, 0.72f, 1f);
        sr.rect(x + 11, y + 13, 5, 2);
    }

    private static void drawCookedChickenIcon(ShapeRenderer sr, float x, float y) {
        sr.setColor(0.82f, 0.50f, 0.18f, 1f);
        sr.rect(x + 7, y + 8, 12, 10);
        sr.setColor(0.62f, 0.34f, 0.10f, 1f);
        sr.rect(x + 18, y + 10, 4, 3);
        sr.setColor(0.98f, 0.74f, 0.40f, 1f);
        sr.rect(x + 10, y + 14, 5, 2);
    }

    private static void drawCookedMeatIcon(ShapeRenderer sr, float x, float y) {
        sr.setColor(0.72f, 0.34f, 0.14f, 1f);
        sr.rect(x + 7, y + 8, 13, 10);
        sr.setColor(0.52f, 0.22f, 0.08f, 1f);
        sr.rect(x + 8, y + 9, 11, 3);
        sr.setColor(0.94f, 0.66f, 0.36f, 1f);
        sr.rect(x + 11, y + 13, 5, 2);
    }

    private static void drawRawShrimpsIcon(ShapeRenderer sr, float x, float y) {
        sr.setColor(0.45f, 0.62f, 0.78f, 1f);
        sr.rect(x + 5, y + 9, 13, 6);
        sr.setColor(0.35f, 0.50f, 0.66f, 1f);
        sr.rect(x + 3, y + 10, 3, 4);
        sr.rect(x + 17, y + 10, 3, 4);
        sr.setColor(0.92f, 0.95f, 0.98f, 1f);
        sr.rect(x + 7, y + 12, 2, 1);
    }

    private static void drawAnchoviesIcon(ShapeRenderer sr, float x, float y) {
        drawFishBody(sr, x, y, new Color(0.56f, 0.70f, 0.86f, 1f), new Color(0.40f, 0.54f, 0.70f, 1f));
    }

    private static void drawSardineIcon(ShapeRenderer sr, float x, float y) {
        drawFishBody(sr, x, y, new Color(0.52f, 0.64f, 0.80f, 1f), new Color(0.36f, 0.48f, 0.64f, 1f));
    }

    private static void drawHerringIcon(ShapeRenderer sr, float x, float y) {
        drawFishBody(sr, x, y, new Color(0.48f, 0.60f, 0.76f, 1f), new Color(0.30f, 0.42f, 0.58f, 1f));
    }

    private static void drawTroutIcon(ShapeRenderer sr, float x, float y) {
        drawFishBody(sr, x, y, new Color(0.58f, 0.72f, 0.58f, 1f), new Color(0.40f, 0.56f, 0.40f, 1f));
    }

    private static void drawSalmonIcon(ShapeRenderer sr, float x, float y) {
        drawFishBody(sr, x, y, new Color(0.88f, 0.54f, 0.44f, 1f), new Color(0.70f, 0.38f, 0.30f, 1f));
    }

    private static void drawPikeIcon(ShapeRenderer sr, float x, float y) {
        drawFishBody(sr, x, y, new Color(0.52f, 0.68f, 0.52f, 1f), new Color(0.34f, 0.50f, 0.34f, 1f));
    }

    private static void drawLobsterIcon(ShapeRenderer sr, float x, float y) {
        sr.setColor(0.78f, 0.20f, 0.16f, 1f);
        sr.rect(x + 8, y + 8, 10, 9);
        sr.setColor(0.92f, 0.35f, 0.26f, 1f);
        sr.rect(x + 9, y + 15, 8, 2);
        sr.setColor(0.70f, 0.18f, 0.14f, 1f);
        sr.rect(x + 5, y + 10, 3, 3);
        sr.rect(x + 18, y + 10, 3, 3);
    }

    private static void drawTunaIcon(ShapeRenderer sr, float x, float y) {
        drawFishBody(sr, x, y, new Color(0.34f, 0.56f, 0.86f, 1f), new Color(0.22f, 0.40f, 0.66f, 1f));
    }

    private static void drawSwordfishIcon(ShapeRenderer sr, float x, float y) {
        drawFishBody(sr, x, y, new Color(0.30f, 0.52f, 0.82f, 1f), new Color(0.20f, 0.36f, 0.60f, 1f));
        sr.setColor(0.86f, 0.92f, 0.98f, 1f);
        sr.rect(x + 18, y + 12, 5, 1);
    }

    private static void drawFishBody(ShapeRenderer sr, float x, float y, Color body, Color tail) {
        sr.setColor(body);
        sr.rect(x + 6, y + 10, 12, 5);
        sr.setColor(tail);
        sr.rect(x + 4, y + 11, 2, 3);
        sr.rect(x + 18, y + 11, 2, 3);
        sr.setColor(0.94f, 0.97f, 1f, 1f);
        sr.rect(x + 9, y + 13, 2, 1);
    }

    private static void drawCookedShrimpsIcon(ShapeRenderer sr, float x, float y) {
        sr.setColor(0.93f, 0.52f, 0.20f, 1f);
        sr.rect(x + 5, y + 9, 13, 6);
        sr.setColor(0.74f, 0.31f, 0.10f, 1f);
        sr.rect(x + 3, y + 10, 3, 4);
        sr.rect(x + 17, y + 10, 3, 4);
        sr.setColor(1f, 0.75f, 0.40f, 1f);
        sr.rect(x + 8, y + 12, 2, 1);
    }

    private static void drawCookedAnchoviesIcon(ShapeRenderer sr, float x, float y) {
        drawCookedFishBody(sr, x, y, new Color(0.86f, 0.60f, 0.30f, 1f), new Color(0.66f, 0.40f, 0.18f, 1f));
    }

    private static void drawCookedSardineIcon(ShapeRenderer sr, float x, float y) {
        drawCookedFishBody(sr, x, y, new Color(0.82f, 0.56f, 0.28f, 1f), new Color(0.62f, 0.36f, 0.16f, 1f));
    }

    private static void drawCookedHerringIcon(ShapeRenderer sr, float x, float y) {
        drawCookedFishBody(sr, x, y, new Color(0.78f, 0.52f, 0.26f, 1f), new Color(0.58f, 0.32f, 0.14f, 1f));
    }

    private static void drawCookedTroutIcon(ShapeRenderer sr, float x, float y) {
        drawCookedFishBody(sr, x, y, new Color(0.86f, 0.58f, 0.24f, 1f), new Color(0.68f, 0.38f, 0.12f, 1f));
    }

    private static void drawCookedPikeIcon(ShapeRenderer sr, float x, float y) {
        drawCookedFishBody(sr, x, y, new Color(0.84f, 0.56f, 0.24f, 1f), new Color(0.64f, 0.36f, 0.12f, 1f));
    }

    private static void drawCookedSalmonIcon(ShapeRenderer sr, float x, float y) {
        drawCookedFishBody(sr, x, y, new Color(0.90f, 0.52f, 0.22f, 1f), new Color(0.70f, 0.32f, 0.12f, 1f));
    }

    private static void drawCookedTunaIcon(ShapeRenderer sr, float x, float y) {
        // Cooked tuna — warm golden-brown tones
        sr.setColor(0.78f, 0.52f, 0.20f, 1f);
        sr.rect(x + 6, y + 10, 12, 5);
        sr.setColor(0.56f, 0.34f, 0.10f, 1f);
        sr.rect(x + 4, y + 11, 2, 3);
        sr.rect(x + 18, y + 11, 2, 3);
        sr.setColor(0.98f, 0.80f, 0.50f, 1f);
        sr.rect(x + 9, y + 13, 2, 1);
    }

    private static void drawCookedLobsterIcon(ShapeRenderer sr, float x, float y) {
        sr.setColor(0.82f, 0.44f, 0.16f, 1f);
        sr.rect(x + 8, y + 8, 10, 9);
        sr.setColor(0.94f, 0.58f, 0.24f, 1f);
        sr.rect(x + 9, y + 15, 8, 2);
        sr.setColor(0.62f, 0.28f, 0.10f, 1f);
        sr.rect(x + 5, y + 10, 3, 3);
        sr.rect(x + 18, y + 10, 3, 3);
    }

    private static void drawCookedSwordfishIcon(ShapeRenderer sr, float x, float y) {
        drawCookedFishBody(sr, x, y, new Color(0.80f, 0.50f, 0.20f, 1f), new Color(0.58f, 0.30f, 0.10f, 1f));
        sr.setColor(0.98f, 0.86f, 0.56f, 1f);
        sr.rect(x + 18, y + 12, 5, 1);
    }

    private static void drawSharkIcon(ShapeRenderer sr, float x, float y) {
        // Cooked shark — larger body, dark grey-blue tones
        sr.setColor(0.42f, 0.44f, 0.48f, 1f);
        sr.rect(x + 5, y + 9, 14, 7);
        sr.setColor(0.28f, 0.30f, 0.34f, 1f);
        sr.rect(x + 3, y + 10, 2, 5);
        sr.rect(x + 19, y + 10, 2, 5);
        // Dorsal fin hint
        sr.setColor(0.50f, 0.52f, 0.56f, 1f);
        sr.rect(x + 10, y + 16, 3, 2);
        sr.setColor(0.80f, 0.84f, 0.88f, 1f);
        sr.rect(x + 9, y + 12, 2, 1);
    }

    private static void drawBurntShrimpsIcon(ShapeRenderer sr, float x, float y) {
        sr.setColor(0.20f, 0.18f, 0.16f, 1f);
        sr.rect(x + 5, y + 9, 13, 6);
        sr.setColor(0.12f, 0.11f, 0.10f, 1f);
        sr.rect(x + 3, y + 10, 3, 4);
        sr.rect(x + 17, y + 10, 3, 4);
    }

    private static void drawCookedFishBody(ShapeRenderer sr, float x, float y, Color body, Color tail) {
        sr.setColor(body);
        sr.rect(x + 6, y + 10, 12, 5);
        sr.setColor(tail);
        sr.rect(x + 4, y + 11, 2, 3);
        sr.rect(x + 18, y + 11, 2, 3);
        sr.setColor(0.98f, 0.86f, 0.56f, 1f);
        sr.rect(x + 9, y + 13, 2, 1);
    }

    private static void drawBonesIcon(ShapeRenderer sr, float x, float y) {
        sr.setColor(0.86f, 0.80f, 0.66f, 1f);
        sr.rect(x + 9, y + 6, 6, 12);
        sr.rect(x + 6, y + 8, 3, 3);
        sr.rect(x + 6, y + 13, 3, 3);
        sr.rect(x + 15, y + 8, 3, 3);
        sr.rect(x + 15, y + 13, 3, 3);
    }

    private static void drawCoinsIcon(ShapeRenderer sr, float x, float y) {
        sr.setColor(0.72f, 0.52f, 0.02f, 1f);
        sr.circle(x + 18, y + 14, 9f, 14);
        sr.setColor(1f, 0.85f, 0.05f, 1f);
        sr.circle(x + 16, y + 16, 9f, 14);
        sr.setColor(1f, 0.97f, 0.60f, 1f);
        sr.circle(x + 13, y + 19, 3.5f, 10);
    }

    private static Color getItemIconColor(int itemId) {
        return switch (itemId) {
            // Weapons / tools without dedicated draw methods
            case 882, 1277, 1351 -> BRONZE_COLOR;
            case 1349 -> IRON_COLOR;
            case 1353 -> STEEL_COLOR;
            case 1355 -> MITHRIL_COLOR;
            case 1357 -> ADAMANT_COLOR;
            case 1359 -> RUNE_COLOR;
            case 995 -> new Color(1f, 0.90f, 0.10f, 1f);
            case 526 -> new Color(0.85f, 0.75f, 0.55f, 1f);
            default -> DEFAULT_ITEM_COLOR;
        };
    }
}
