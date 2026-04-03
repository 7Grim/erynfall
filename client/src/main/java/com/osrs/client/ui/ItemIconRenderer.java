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
            case 1511 -> drawLogsIcon(sr, iconX, iconY);
            case 1521 -> drawOakLogsIcon(sr, iconX, iconY);
            case 1522 -> drawWillowLogsIcon(sr, iconX, iconY);
            case 1523 -> drawMapleLogsIcon(sr, iconX, iconY);
            case 1524 -> drawYewLogsIcon(sr, iconX, iconY);
            case 1525 -> drawMagicLogsIcon(sr, iconX, iconY);
            case 1351 -> drawAxeIcon(sr, iconX, iconY);
            case 303 -> drawSmallNetIcon(sr, iconX, iconY);
            case 317 -> drawRawShrimpsIcon(sr, iconX, iconY);
            case 315 -> drawCookedShrimpsIcon(sr, iconX, iconY);
            case 7954 -> drawBurntShrimpsIcon(sr, iconX, iconY);
            case 526 -> drawBonesIcon(sr, iconX, iconY);
            case 995 -> drawCoinsIcon(sr, iconX, iconY);
            case 1000 -> drawCoinsIcon(sr, iconX, iconY);
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

    private static void drawAxeIcon(ShapeRenderer sr, float x, float y) {
        sr.setColor(0.58f, 0.36f, 0.18f, 1f);
        sr.rect(x + 11, y + 4, 3, 16);
        sr.setColor(0.75f, 0.78f, 0.82f, 1f);
        sr.rect(x + 6, y + 14, 10, 5);
        sr.setColor(0.55f, 0.59f, 0.64f, 1f);
        sr.rect(x + 6, y + 12, 6, 2);
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

    private static void drawRawShrimpsIcon(ShapeRenderer sr, float x, float y) {
        sr.setColor(0.45f, 0.62f, 0.78f, 1f);
        sr.rect(x + 5, y + 9, 13, 6);
        sr.setColor(0.35f, 0.50f, 0.66f, 1f);
        sr.rect(x + 3, y + 10, 3, 4);
        sr.rect(x + 17, y + 10, 3, 4);
        sr.setColor(0.92f, 0.95f, 0.98f, 1f);
        sr.rect(x + 7, y + 12, 2, 1);
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
