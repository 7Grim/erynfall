package com.osrs.client.renderer;

import java.util.Arrays;
import java.util.List;

public final class RenderZone {
    public final String name;
    public final int minX;
    public final int minY;
    public final int maxX;
    public final int maxY;
    public final float tintR;
    public final float tintG;
    public final float tintB;
    public final float tintAlpha;
    public final float vignetteAlpha;

    public RenderZone(String name,
                      int minX,
                      int minY,
                      int maxX,
                      int maxY,
                      float tintR,
                      float tintG,
                      float tintB,
                      float tintAlpha,
                      float vignetteAlpha) {
        this.name = name;
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.tintR = tintR;
        this.tintG = tintG;
        this.tintB = tintB;
        this.tintAlpha = tintAlpha;
        this.vignetteAlpha = vignetteAlpha;
    }

    public boolean contains(int tileX, int tileY) {
        return tileX >= minX && tileX < maxX && tileY >= minY && tileY < maxY;
    }

    public static RenderZone findZone(List<RenderZone> zones, int tileX, int tileY) {
        for (RenderZone z : zones) {
            if (z.contains(tileX, tileY)) return z;
        }
        return null;
    }

    public static final List<RenderZone> TUTORIAL_ISLAND = Arrays.asList(
        new RenderZone("skill_north", 0, 8, 104, 23, 0.94f, 0.95f, 0.90f, 0.04f, 0.00f),
        new RenderZone("cooking", 0, 24, 104, 27, 1.00f, 0.90f, 0.82f, 0.05f, 0.00f),
        new RenderZone("fishing", 0, 28, 104, 31, 0.86f, 0.92f, 1.00f, 0.06f, 0.00f),
        new RenderZone("mining", 0, 32, 104, 35, 0.82f, 0.84f, 0.88f, 0.06f, 0.00f),
        new RenderZone("woodcutting", 0, 36, 104, 39, 0.90f, 0.98f, 0.88f, 0.05f, 0.00f),
        new RenderZone("town", 0, 40, 104, 60, 1.00f, 1.00f, 1.00f, 0.00f, 0.00f),
        new RenderZone("combat_low", 0, 61, 104, 66, 0.95f, 0.90f, 0.90f, 0.04f, 0.00f),
        new RenderZone("combat_mid", 0, 67, 104, 72, 0.92f, 0.86f, 0.86f, 0.05f, 0.00f),
        new RenderZone("combat_high", 0, 73, 104, 78, 0.90f, 0.84f, 0.84f, 0.06f, 0.00f),
        new RenderZone("combat_a", 0, 79, 104, 84, 0.88f, 0.82f, 0.82f, 0.07f, 0.00f),
        new RenderZone("combat_b", 0, 85, 104, 96, 0.86f, 0.80f, 0.80f, 0.08f, 0.00f)
    );
}
