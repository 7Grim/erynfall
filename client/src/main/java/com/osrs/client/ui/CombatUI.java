package com.osrs.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.osrs.client.renderer.Renderer3DExperimental;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * OSRS-style hitsplat rendering.
 *
 * Hitsplats are static circular icons anchored to the target entity's world position.
 * They do NOT float upward. Multiple hitsplats on the same entity stack horizontally.
 * Duration: 1.8s (3 OSRS ticks), fade during last 0.3s.
 *
 * Colours by hit type (OSRS convention):
 *   MELEE  → red     RANGED → green   MAGIC → blue
 *   POISON → yellow  BLOCK  → grey    MISS  → grey
 */
public class CombatUI {

    private static final Logger LOG = LoggerFactory.getLogger(CombatUI.class);

    // Hitsplat constants
    private static final float SPLAT_LIFETIME  = 1.8f;   // seconds (3 OSRS ticks)
    private static final float FADE_DURATION   = 0.3f;   // fade during last 0.3s
    private static final float SPLAT_RADIUS    = 11f;    // screen pixels (22px diameter)
    private static final float STACK_OFFSET_X  = 24f;   // px between horizontal splats
    // Height above ground in world units at which the splat anchor is projected
    private static final float SPLAT_WORLD_HEIGHT = 2.2f;

    /** Attack type that determines the splat colour. */
    public enum HitType { MELEE, RANGED, MAGIC, POISON, BLOCK, MISS }

    public enum HpBarState { HEALTHY, POISONED, VENOMED, DISEASED, NEAR_DEATH }

    public static class DamageNumber {
        public final float tileX;
        public final float tileY;
        public final int damage;
        public final HitType hitType;
        float age = 0f;

        DamageNumber(float tileX, float tileY, int damage, HitType hitType) {
            this.tileX   = tileX;
            this.tileY   = tileY;
            this.damage  = damage;
            this.hitType = hitType;
        }

        boolean isExpired() { return age >= SPLAT_LIFETIME; }

        float getAlpha() {
            float remaining = SPLAT_LIFETIME - age;
            return remaining < FADE_DURATION ? remaining / FADE_DURATION : 1f;
        }
    }

    private final List<DamageNumber> damageNumbers = new ArrayList<>();
    private final List<String> combatMessages = new ArrayList<>();
    private HpBarState hpBarState = HpBarState.HEALTHY;
    private String opponentName = null;
    private int opponentLevel = 0;
    private int opponentHp = 0;
    private int opponentMaxHp = 0;
    private boolean showOpponentInfo = false;

    private final Matrix4 screenProjection = new Matrix4();
    private final Vector3 tmpVec3 = new Vector3();

    private static final int MAX_MESSAGES = 5;

    /**
     * Add a hitsplat above the given world tile.
     *
     * If {@code hit} is false the splat becomes MISS regardless of {@code baseType}.
     * If {@code damage == 0} and {@code hit} is true the splat becomes BLOCK.
     */
    public void addDamageNumber(float tileX, float tileY, int damage, boolean hit, HitType baseType) {
        HitType effective;
        if (!hit) {
            effective = HitType.MISS;
        } else if (damage == 0) {
            effective = HitType.BLOCK;
        } else {
            effective = baseType != null ? baseType : HitType.MELEE;
        }
        damageNumbers.add(new DamageNumber(tileX, tileY, damage, effective));
    }

    /** Convenience overload when hit type is not known (defaults to MELEE). */
    public void addDamageNumber(float tileX, float tileY, int damage, boolean hit) {
        addDamageNumber(tileX, tileY, damage, hit, HitType.MELEE);
    }

    /** Add a line to the combat message log (bottom of screen). */
    public void addMessage(String message) {
        combatMessages.add(message);
        if (combatMessages.size() > MAX_MESSAGES) {
            combatMessages.remove(0);
        }
    }

    public void setHpBarState(HpBarState state) {
        this.hpBarState = state != null ? state : HpBarState.HEALTHY;
    }

    public void setOpponentInfo(String npcName, int npcLevel, int npcHp, int npcMaxHp) {
        this.opponentName  = npcName;
        this.opponentLevel = Math.max(0, npcLevel);
        this.opponentHp    = Math.max(0, npcHp);
        this.opponentMaxHp = Math.max(1, npcMaxHp);
        this.showOpponentInfo = npcName != null && !npcName.isEmpty();
    }

    /** Call every frame with elapsed delta (seconds). */
    public void update(float delta) {
        Iterator<DamageNumber> it = damageNumbers.iterator();
        while (it.hasNext()) {
            DamageNumber dn = it.next();
            dn.age += delta;
            if (dn.isExpired()) it.remove();
        }
    }

    /**
     * Render all active hitsplats in SCREEN SPACE using 3D camera projection.
     * Hitsplats are static — they do not move upward.
     */
    public void render(ShapeRenderer shapeRenderer, SpriteBatch batch, BitmapFont font,
                       PerspectiveCamera camera3d, Renderer3DExperimental renderer3d) {
        if (damageNumbers.isEmpty() && !showOpponentInfo) return;

        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();
        screenProjection.setToOrtho2D(0, 0, screenW, screenH);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        if (!damageNumbers.isEmpty() && camera3d != null) {
            // Map from tile key → horizontal stack index (for simultaneous splats on same tile)
            Map<Long, Integer> stackByTile = new HashMap<>();

            // --- Pass 1: filled circles ---
            shapeRenderer.setProjectionMatrix(screenProjection);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            for (DamageNumber dn : damageNumbers) {
                float[] screen = projectToScreen(dn.tileX, dn.tileY, camera3d, renderer3d);
                if (screen == null) continue;
                long key = tileKey(dn.tileX, dn.tileY);
                int stackIdx = stackByTile.getOrDefault(key, 0);
                stackByTile.put(key, stackIdx + 1);
                float sx = screen[0] + stackIdx * STACK_OFFSET_X;
                float sy = screen[1];
                float alpha = dn.getAlpha();
                float[] col = splatColor(dn.hitType);
                // Dark border ring
                shapeRenderer.setColor(col[0] * 0.35f, col[1] * 0.35f, col[2] * 0.35f, alpha);
                shapeRenderer.circle(sx, sy, SPLAT_RADIUS + 1.5f, 14);
                // Fill
                shapeRenderer.setColor(col[0], col[1], col[2], alpha);
                shapeRenderer.circle(sx, sy, SPLAT_RADIUS, 14);
            }
            shapeRenderer.end();

            // --- Pass 2: damage number text ---
            batch.setProjectionMatrix(screenProjection);
            batch.begin();
            stackByTile.clear();
            for (DamageNumber dn : damageNumbers) {
                float[] screen = projectToScreen(dn.tileX, dn.tileY, camera3d, renderer3d);
                if (screen == null) continue;
                long key = tileKey(dn.tileX, dn.tileY);
                int stackIdx = stackByTile.getOrDefault(key, 0);
                stackByTile.put(key, stackIdx + 1);
                float sx = screen[0] + stackIdx * STACK_OFFSET_X;
                float sy = screen[1];
                float alpha = dn.getAlpha();
                String text = String.valueOf(dn.damage);
                float textColor = dn.hitType == HitType.BLOCK || dn.hitType == HitType.MISS ? 0.2f : 1f;
                font.setColor(textColor, textColor, textColor, alpha);
                font.draw(batch, text, sx - text.length() * 3f, sy + 5f);
            }
            batch.end();
        }

        if (showOpponentInfo) {
            renderOpponentInfo(shapeRenderer, batch, font, screenW, screenH,
                opponentName, opponentLevel, opponentHp, opponentMaxHp);
        }

        font.setColor(Color.WHITE);
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private float[] projectToScreen(float tileX, float tileY,
                                     PerspectiveCamera camera3d,
                                     Renderer3DExperimental renderer3d) {
        float terrainY = renderer3d != null ? renderer3d.getTileTopY(tileX, tileY) : 0f;
        tmpVec3.set(tileX + 0.5f, terrainY + SPLAT_WORLD_HEIGHT, tileY + 0.5f);
        camera3d.project(tmpVec3);
        if (tmpVec3.z < 0f || tmpVec3.z > 1f) return null; // behind camera / clipped
        return new float[]{ tmpVec3.x, tmpVec3.y };
    }

    /** Returns (r, g, b) for the splat background colour. */
    private static float[] splatColor(HitType type) {
        return switch (type) {
            case MELEE  -> new float[]{ 0.80f, 0.05f, 0.05f };
            case RANGED -> new float[]{ 0.18f, 0.72f, 0.18f };
            case MAGIC  -> new float[]{ 0.18f, 0.42f, 1.00f };
            case POISON -> new float[]{ 0.88f, 0.74f, 0.05f };
            case BLOCK  -> new float[]{ 0.72f, 0.72f, 0.72f };
            case MISS   -> new float[]{ 0.55f, 0.55f, 0.55f };
        };
    }

    private Color hpColorForState() {
        return switch (hpBarState) {
            case HEALTHY, NEAR_DEATH -> new Color(0.22f, 0.78f, 0.28f, 1f);
            case POISONED            -> new Color(0.90f, 0.80f, 0.15f, 1f);
            case VENOMED             -> new Color(0.12f, 0.42f, 0.38f, 1f);
            case DISEASED            -> new Color(0.90f, 0.80f, 0.15f, 1f);
        };
    }

    private void renderHpBar(ShapeRenderer sr, float screenX, float screenY,
                             float hpPercent, Color barColor) {
        int barWidth  = 100;
        int barHeight = 8;
        int barX = (int) screenX + 10;
        int barY = (int) screenY - 25;
        float clamped = Math.max(0f, Math.min(1f, hpPercent));
        float innerW  = (barWidth - 2) * clamped;

        sr.setColor(Color.BLACK);
        sr.rect(barX, barY, barWidth, barHeight);

        sr.setColor(barColor);
        sr.rect(barX + 1, barY + 1, innerW, barHeight - 2);

        if (hpBarState == HpBarState.NEAR_DEATH) {
            sr.setColor(1f, 0f, 0f, 0.5f);
            sr.rect(barX, barY, barWidth, barHeight);
        }
    }

    private void renderOpponentInfo(ShapeRenderer sr, SpriteBatch batch, BitmapFont font,
                                    int screenW, int screenH,
                                    String npcName, int npcLevel, int npcHp, int npcMaxHp) {
        int panelX = 10;
        int panelY = screenH - 200;
        int panelW = 170;
        int panelH = 42;

        float hpPercent = npcMaxHp > 0 ? (float) npcHp / npcMaxHp : 0f;
        Color hpColor = hpColorForState();

        sr.setProjectionMatrix(screenProjection);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.08f, 0.07f, 0.06f, 0.88f);
        sr.rect(panelX, panelY - panelH + 8, panelW, panelH);
        renderHpBar(sr, panelX + 10, panelY - 8, hpPercent, hpColor);
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(0.55f, 0.46f, 0.28f, 1f);
        sr.rect(panelX, panelY - panelH + 8, panelW, panelH);
        sr.end();

        batch.setProjectionMatrix(screenProjection);
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, npcName + " (Level " + npcLevel + ")", panelX + 10, panelY);
        font.setColor(0.88f, 0.82f, 0.72f, 1f);
        font.draw(batch, "HP: " + npcHp + "/" + npcMaxHp, panelX + 10, panelY - 16);
        batch.end();
    }

    /**
     * Render the combat message log in SCREEN SPACE (no camera).
     * Call with a screen-space SpriteBatch.
     */
    public void renderMessages(SpriteBatch screenBatch, BitmapFont font, int screenHeight) {
        if (combatMessages.isEmpty()) return;
        screenBatch.begin();
        font.setColor(Color.WHITE);
        int y = 80;
        for (int i = 0; i < combatMessages.size(); i++) {
            font.draw(screenBatch, combatMessages.get(i), 10, y + i * 15);
        }
        screenBatch.end();
        font.setColor(Color.WHITE);
    }

    public List<DamageNumber> getDamageNumbers() { return damageNumbers; }
    public List<String> getMessages() { return combatMessages; }

    private long tileKey(float tileX, float tileY) {
        long x = (int) tileX;
        long y = (int) tileY;
        return (x << 32) | (y & 0xffffffffL);
    }
}
