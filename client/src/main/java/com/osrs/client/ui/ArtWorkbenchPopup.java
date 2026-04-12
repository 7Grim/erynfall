package com.osrs.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ArtWorkbenchPopup {

    public enum ClipMode {
        AUTO,
        IDLE,
        WALK
    }

    private static final int PANEL_W = 540;
    private static final int PANEL_H = 190;

    private final GlyphLayout glyph = new GlyphLayout();
    private final ArrayList<String> modelKeys = new ArrayList<>();
    private boolean visible = false;
    private int selectedIndex = 0;
    private ClipMode clipMode = ClipMode.AUTO;

    public void setModelKeys(List<String> keys) {
        modelKeys.clear();
        if (keys != null) {
            modelKeys.addAll(keys);
        }
        Collections.sort(modelKeys);
        if (selectedIndex >= modelKeys.size()) {
            selectedIndex = Math.max(0, modelKeys.size() - 1);
        }
    }

    public void show() {
        visible = true;
    }

    public void dismiss() {
        visible = false;
    }

    public boolean isVisible() {
        return visible;
    }

    public void toggle() {
        visible = !visible;
    }

    public void selectNext() {
        if (modelKeys.isEmpty()) {
            selectedIndex = 0;
            return;
        }
        selectedIndex = (selectedIndex + 1) % modelKeys.size();
    }

    public void selectPrevious() {
        if (modelKeys.isEmpty()) {
            selectedIndex = 0;
            return;
        }
        selectedIndex--;
        if (selectedIndex < 0) {
            selectedIndex = modelKeys.size() - 1;
        }
    }

    public void cycleClipMode() {
        clipMode = switch (clipMode) {
            case AUTO -> ClipMode.IDLE;
            case IDLE -> ClipMode.WALK;
            case WALK -> ClipMode.AUTO;
        };
    }

    public String selectedModelKey() {
        if (modelKeys.isEmpty()) {
            return "";
        }
        return modelKeys.get(selectedIndex);
    }

    public String selectedClipName() {
        return switch (clipMode) {
            case IDLE -> "idle";
            case WALK -> "walk";
            case AUTO -> "";
        };
    }

    public ClipMode clipMode() {
        return clipMode;
    }

    public void render(ShapeRenderer shapeRenderer,
                       SpriteBatch batch,
                       BitmapFont font,
                       int screenW,
                       int screenH,
                       Matrix4 projection) {
        if (!visible) {
            return;
        }

        int x = (screenW - PANEL_W) / 2;
        int y = screenH - PANEL_H - 18;

        shapeRenderer.setProjectionMatrix(projection);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(new Color(0.03f, 0.04f, 0.06f, 0.86f));
        shapeRenderer.rect(x, y, PANEL_W, PANEL_H);
        shapeRenderer.setColor(new Color(0.10f, 0.13f, 0.18f, 0.95f));
        shapeRenderer.rect(x + 2, y + PANEL_H - 40, PANEL_W - 4, 38);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(new Color(0.74f, 0.80f, 0.92f, 1f));
        shapeRenderer.rect(x, y, PANEL_W, PANEL_H);
        shapeRenderer.rect(x + 2, y + PANEL_H - 40, PANEL_W - 4, 38);
        shapeRenderer.end();

        batch.setProjectionMatrix(projection);
        batch.begin();
        font.setColor(0.92f, 0.95f, 1f, 1f);
        font.getData().setScale(0.90f);
        font.draw(batch, "Art Workbench - Model Preview", x + 12, y + PANEL_H - 13);

        font.getData().setScale(0.70f);
        font.setColor(0.82f, 0.88f, 0.98f, 1f);
        font.draw(batch, "Model key:", x + 14, y + PANEL_H - 58);
        font.setColor(0.99f, 0.96f, 0.72f, 1f);
        String key = selectedModelKey();
        if (key.isBlank()) {
            key = "(no models loaded)";
        }
        font.draw(batch, key, x + 94, y + PANEL_H - 58);

        font.setColor(0.82f, 0.88f, 0.98f, 1f);
        font.draw(batch, "Clip mode:", x + 14, y + PANEL_H - 82);
        font.setColor(0.96f, 0.96f, 0.96f, 1f);
        font.draw(batch, clipMode.name(), x + 94, y + PANEL_H - 82);

        font.setColor(0.80f, 0.84f, 0.90f, 1f);
        font.draw(batch, "[ / ]  Previous / next model", x + 14, y + 66);
        font.draw(batch, ";  Cycle clip mode (AUTO/IDLE/WALK)", x + 14, y + 46);
        font.draw(batch, "F6  Close workbench", x + 14, y + 26);
        font.draw(batch, "F7/F8 remain available for bounds/axes and anchor debug", x + 14, y + 8);

        if (!modelKeys.isEmpty()) {
            String count = (selectedIndex + 1) + " / " + modelKeys.size();
            glyph.setText(font, count);
            font.setColor(0.68f, 0.74f, 0.84f, 1f);
            font.draw(batch, count, x + PANEL_W - glyph.width - 14, y + PANEL_H - 58);
        }

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }
}
