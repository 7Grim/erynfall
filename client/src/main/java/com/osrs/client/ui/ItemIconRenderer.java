package com.osrs.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public final class ItemIconRenderer {

    private static final Color BRONZE_COLOR = new Color(0.55f, 0.46f, 0.28f, 1f);
    private static final Color IRON_COLOR = new Color(0.30f, 0.30f, 0.30f, 1f);
    private static final Color STEEL_COLOR = new Color(0.60f, 0.60f, 0.60f, 1f);
    private static final Color MITHRIL_COLOR = new Color(0.20f, 0.55f, 0.85f, 1f);
    private static final Color ADAMANT_COLOR = new Color(0.20f, 0.60f, 0.20f, 1f);
    private static final Color RUNE_COLOR = new Color(0.40f, 0.65f, 0.75f, 1f);
    private static final Color DRAGON_COLOR = new Color(1.00f, 0.85f, 0.00f, 1f);
    private static final Color DEFAULT_ITEM_COLOR = new Color(0.60f, 0.60f, 0.60f, 1f);

    private ItemIconRenderer() {
    }

    public static void drawItemIcon(ShapeRenderer sr, float slotLeft, float slotBottom, int itemId) {
        float iconX = slotLeft + 4;
        float iconY = slotBottom + 4;

        switch (itemId) {
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

    private static void drawBurntShrimpsIcon(ShapeRenderer sr, float x, float y) {
        sr.setColor(0.20f, 0.18f, 0.16f, 1f);
        sr.rect(x + 5, y + 9, 13, 6);
        sr.setColor(0.12f, 0.11f, 0.10f, 1f);
        sr.rect(x + 3, y + 10, 3, 4);
        sr.rect(x + 17, y + 10, 3, 4);
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
            case 882, 1277, 1321, 1175, 1115, 1119, 1067, 1351 -> BRONZE_COLOR;
            case 1349, 1323, 1153, 2000, 1069, 1177 -> IRON_COLOR;
            case 1353, 1325, 1157, 1085, 1071, 1193 -> STEEL_COLOR;
            case 1355, 1329, 1163, 1129, 1075, 1197 -> MITHRIL_COLOR;
            case 1357, 1331, 1161, 1133, 1077, 1199 -> ADAMANT_COLOR;
            case 1359, 1333, 1165, 1127, 1079, 1185 -> RUNE_COLOR;
            case 4587, 11335, 3140, 4087, 11286 -> DRAGON_COLOR;
            case 1327, 1159, 1125, 1073, 1195 -> IRON_COLOR;
            case 995 -> new Color(1f, 0.90f, 0.10f, 1f);
            case 526 -> new Color(0.85f, 0.75f, 0.55f, 1f);
            default -> DEFAULT_ITEM_COLOR;
        };
    }
}
