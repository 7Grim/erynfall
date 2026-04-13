package com.osrs.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.osrs.client.art.SceneEditState;
import com.osrs.client.renderer.ModelLibrary;
import com.osrs.shared.EquipmentSlot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArtWorkbenchPopup {

    public enum ClipMode {
        AUTO,
        IDLE,
        WALK
    }

    public enum Mode {
        MODEL_PREVIEW,
        EQUIPMENT_FIT,
        WORLD_PLACEMENT
    }

    private static final int PANEL_W = 580;
    private static final int PANEL_H = 220;
    private static final int[] VISIBLE_SLOTS = {
        EquipmentSlot.HEAD,
        EquipmentSlot.CAPE,
        EquipmentSlot.AMMO,
        EquipmentSlot.WEAPON,
        EquipmentSlot.SHIELD,
        EquipmentSlot.BODY,
        EquipmentSlot.LEGS,
        EquipmentSlot.HANDS,
        EquipmentSlot.FEET
    };
    private static final String[] VISIBLE_SLOT_LABELS = {
        "HEAD",
        "CAPE",
        "AMMO",
        "WEAPON",
        "SHIELD",
        "BODY",
        "LEGS",
        "HANDS",
        "FEET"
    };

    private final GlyphLayout glyph = new GlyphLayout();
    private final ArrayList<String> modelKeys = new ArrayList<>();
    private final Map<Integer, List<ModelLibrary.EquipmentPreviewOption>> equipmentOptionsBySlot = new HashMap<>();
    private final Map<Integer, Integer> equipmentSelectionIndexBySlot = new HashMap<>();
    private final Map<Integer, float[]> transformOverridesBySlot = new HashMap<>();
    private boolean visible = false;
    private int selectedIndex = 0;
    private int activeSlotIndex = 0;
    private ClipMode clipMode = ClipMode.AUTO;
    private Mode mode = Mode.MODEL_PREVIEW;
    private String exportStatus = "";
    private String exportSnippet = "";
    private String worldPlacementStatus = "";
    private SceneEditState sceneEditState;

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

    public void setEquipmentOptions(Map<Integer, List<ModelLibrary.EquipmentPreviewOption>> optionsBySlot) {
        equipmentOptionsBySlot.clear();
        if (optionsBySlot != null) {
            equipmentOptionsBySlot.putAll(optionsBySlot);
        }
        for (int slot : VISIBLE_SLOTS) {
            equipmentSelectionIndexBySlot.putIfAbsent(slot, -1);
            List<ModelLibrary.EquipmentPreviewOption> options = equipmentOptionsBySlot.getOrDefault(slot, List.of());
            int current = equipmentSelectionIndexBySlot.getOrDefault(slot, -1);
            if (current >= options.size()) {
                equipmentSelectionIndexBySlot.put(slot, -1);
            }
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
        if (mode == Mode.EQUIPMENT_FIT) {
            cycleEquipmentOption(1);
            return;
        }
        if (modelKeys.isEmpty()) {
            selectedIndex = 0;
            return;
        }
        selectedIndex = (selectedIndex + 1) % modelKeys.size();
    }

    public void selectPrevious() {
        if (mode == Mode.EQUIPMENT_FIT) {
            cycleEquipmentOption(-1);
            return;
        }
        if (modelKeys.isEmpty()) {
            selectedIndex = 0;
            return;
        }
        selectedIndex--;
        if (selectedIndex < 0) {
            selectedIndex = modelKeys.size() - 1;
        }
    }

    public void cycleMode() {
        mode = switch (mode) {
            case MODEL_PREVIEW -> Mode.EQUIPMENT_FIT;
            case EQUIPMENT_FIT -> Mode.WORLD_PLACEMENT;
            case WORLD_PLACEMENT -> Mode.MODEL_PREVIEW;
        };
    }

    public void cycleClipMode() {
        clipMode = switch (clipMode) {
            case AUTO -> ClipMode.IDLE;
            case IDLE -> ClipMode.WALK;
            case WALK -> ClipMode.AUTO;
        };
    }

    public void cycleActiveSlot(int direction) {
        if (mode != Mode.EQUIPMENT_FIT) {
            return;
        }
        activeSlotIndex += direction;
        while (activeSlotIndex < 0) {
            activeSlotIndex += VISIBLE_SLOTS.length;
        }
        activeSlotIndex = activeSlotIndex % VISIBLE_SLOTS.length;
    }

    public void clearActiveSlot() {
        if (mode != Mode.EQUIPMENT_FIT) {
            return;
        }
        equipmentSelectionIndexBySlot.put(activeSlot(), -1);
    }

    public int activeSlot() {
        return VISIBLE_SLOTS[Math.max(0, Math.min(activeSlotIndex, VISIBLE_SLOTS.length - 1))];
    }

    public String activeSlotLabel() {
        return VISIBLE_SLOT_LABELS[Math.max(0, Math.min(activeSlotIndex, VISIBLE_SLOTS.length - 1))];
    }

    public ModelLibrary.EquipmentPreviewOption activeEquipmentOption() {
        return selectedOptionForSlot(activeSlot());
    }

    public float[] activeSlotTransformOverride() {
        float[] values = transformOverridesBySlot.get(activeSlot());
        if (values == null || values.length < 6) {
            return new float[6];
        }
        return new float[]{values[0], values[1], values[2], values[3], values[4], values[5]};
    }

    public void setExportResult(String status, String snippet) {
        exportStatus = status == null ? "" : status;
        exportSnippet = snippet == null ? "" : snippet;
    }

    public void setSceneEditState(SceneEditState sceneEditState) {
        this.sceneEditState = sceneEditState;
    }

    public void setWorldPlacementStatus(String status) {
        this.worldPlacementStatus = status == null ? "" : status;
    }

    public void cycleWorldPlacementProp(int direction) {
        if (mode == Mode.WORLD_PLACEMENT && sceneEditState != null) {
            sceneEditState.cyclePlaceableKey(direction);
        }
    }

    public void rotateWorldPlacement(float deltaDegrees) {
        if (mode == Mode.WORLD_PLACEMENT && sceneEditState != null) {
            sceneEditState.rotateSelectedOrPreview(deltaDegrees);
        }
    }

    public void scaleWorldPlacement(float deltaScale) {
        if (mode == Mode.WORLD_PLACEMENT && sceneEditState != null) {
            sceneEditState.scaleSelectedOrPreview(deltaScale);
        }
    }

    public void resetWorldPlacementTransform() {
        if (mode == Mode.WORLD_PLACEMENT && sceneEditState != null) {
            sceneEditState.resetPreviewTransform();
        }
    }

    public void adjustActiveSlotOffset(float dx, float dy, float dz) {
        if (mode != Mode.EQUIPMENT_FIT) {
            return;
        }
        float[] values = transformOverridesBySlot.computeIfAbsent(activeSlot(), ignored -> new float[6]);
        values[0] += dx;
        values[1] += dy;
        values[2] += dz;
    }

    public void adjustActiveSlotRotation(float dRx, float dRy, float dRz) {
        if (mode != Mode.EQUIPMENT_FIT) {
            return;
        }
        float[] values = transformOverridesBySlot.computeIfAbsent(activeSlot(), ignored -> new float[6]);
        values[3] += dRx;
        values[4] += dRy;
        values[5] += dRz;
    }

    public void resetActiveSlotTransformOverrides() {
        if (mode != Mode.EQUIPMENT_FIT) {
            return;
        }
        transformOverridesBySlot.remove(activeSlot());
    }

    public Map<Integer, float[]> equipmentTransformOverrides() {
        Map<Integer, float[]> copy = new HashMap<>();
        for (Map.Entry<Integer, float[]> entry : transformOverridesBySlot.entrySet()) {
            float[] source = entry.getValue();
            if (source == null || source.length < 6) {
                continue;
            }
            copy.put(entry.getKey(), new float[]{source[0], source[1], source[2], source[3], source[4], source[5]});
        }
        return copy;
    }

    private void cycleEquipmentOption(int direction) {
        int slot = activeSlot();
        List<ModelLibrary.EquipmentPreviewOption> options = equipmentOptionsBySlot.getOrDefault(slot, List.of());
        if (options.isEmpty()) {
            equipmentSelectionIndexBySlot.put(slot, -1);
            return;
        }

        int current = equipmentSelectionIndexBySlot.getOrDefault(slot, -1);
        if (direction > 0) {
            current++;
            if (current >= options.size()) {
                current = -1;
            }
        } else {
            current--;
            if (current < -1) {
                current = options.size() - 1;
            }
        }
        equipmentSelectionIndexBySlot.put(slot, current);
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

    public Mode mode() {
        return mode;
    }

    public int[] selectedEquipmentItemIds() {
        int[] equipped = new int[EquipmentSlot.RING + 1];
        for (int slot : VISIBLE_SLOTS) {
            int index = equipmentSelectionIndexBySlot.getOrDefault(slot, -1);
            List<ModelLibrary.EquipmentPreviewOption> options = equipmentOptionsBySlot.getOrDefault(slot, List.of());
            if (index >= 0 && index < options.size()) {
                equipped[slot] = options.get(index).itemId();
            }
        }
        return equipped;
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
        font.draw(batch, "Art Workbench", x + 12, y + PANEL_H - 13);

        font.getData().setScale(0.70f);
        font.setColor(0.82f, 0.88f, 0.98f, 1f);
        font.draw(batch, "Mode:", x + 14, y + PANEL_H - 58);
        font.setColor(0.99f, 0.96f, 0.72f, 1f);
        font.draw(batch, mode == Mode.MODEL_PREVIEW ? "MODEL_PREVIEW" : "EQUIPMENT_FIT", x + 70, y + PANEL_H - 58);

        font.setColor(0.82f, 0.88f, 0.98f, 1f);
        font.draw(batch, "Clip:", x + 300, y + PANEL_H - 58);
        font.setColor(0.96f, 0.96f, 0.96f, 1f);
        font.draw(batch, clipMode.name(), x + 338, y + PANEL_H - 58);

        if (mode == Mode.MODEL_PREVIEW) {
            renderModelPreviewText(batch, font, x, y);
        } else if (mode == Mode.EQUIPMENT_FIT) {
            renderEquipmentFitText(batch, font, x, y);
        } else {
            renderWorldPlacementText(batch, font, x, y);
        }

        font.setColor(0.80f, 0.84f, 0.90f, 1f);
        font.draw(batch, "TAB mode  [ / ] cycle  ; clip  F6 close", x + 14, y + 46);
        font.draw(batch, "LMB drag orbit  wheel zoom  MMB reset camera", x + 14, y + 26);
        font.draw(batch, "F7/F8 debug overlays remain active", x + 14, y + 8);

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private void renderModelPreviewText(SpriteBatch batch, BitmapFont font, int x, int y) {
        font.setColor(0.82f, 0.88f, 0.98f, 1f);
        font.draw(batch, "Model key:", x + 14, y + PANEL_H - 86);
        font.setColor(0.99f, 0.96f, 0.72f, 1f);
        String key = selectedModelKey();
        if (key.isBlank()) {
            key = "(no loaded models)";
        }
        font.draw(batch, key, x + 94, y + PANEL_H - 86);
        font.setColor(0.80f, 0.84f, 0.90f, 1f);
        font.draw(batch, "Mode: isolated model preview in neutral scene", x + 14, y + 64);
    }

    private void renderEquipmentFitText(SpriteBatch batch, BitmapFont font, int x, int y) {
        int slot = activeSlot();
        String slotLabel = activeSlotLabel();
        ModelLibrary.EquipmentPreviewOption option = selectedOptionForSlot(slot);

        font.setColor(0.82f, 0.88f, 0.98f, 1f);
        font.draw(batch, "Slot:", x + 14, y + PANEL_H - 86);
        font.setColor(0.99f, 0.96f, 0.72f, 1f);
        font.draw(batch, slotLabel, x + 54, y + PANEL_H - 86);

        font.setColor(0.82f, 0.88f, 0.98f, 1f);
        font.draw(batch, "Selection:", x + 14, y + PANEL_H - 110);
        font.setColor(0.96f, 0.96f, 0.96f, 1f);
        if (option == null) {
            font.draw(batch, "(empty)", x + 94, y + PANEL_H - 110);
        } else {
            String name = option.itemName() == null || option.itemName().isBlank()
                ? "item " + option.itemId()
                : option.itemName();
            font.draw(batch, name + " [" + option.modelKey() + "]", x + 94, y + PANEL_H - 110);
        }

        List<ModelLibrary.EquipmentPreviewOption> options = equipmentOptionsBySlot.getOrDefault(slot, List.of());
        int optionCount = options.size();
        int selected = equipmentSelectionIndexBySlot.getOrDefault(slot, -1);
        String progress = selected < 0 ? "empty" : (selected + 1) + " / " + optionCount;
        float[] override = transformOverridesBySlot.getOrDefault(slot, new float[6]);

        font.setColor(0.80f, 0.84f, 0.90f, 1f);
        font.draw(batch, "< / > slot (comma/period), [ / ] option, backspace clear", x + 14, y + 84);
        font.draw(batch, "W/S Y  A/D X  Q/E Z  |  I/K RX  J/L RY  U/O RZ  |  R reset", x + 14, y + 64);
        font.draw(batch, "Shift fine, Ctrl coarse", x + 14, y + 44);
        font.draw(batch, String.format("OVR off(%.3f, %.3f, %.3f) rot(%.2f, %.2f, %.2f)",
            override[0], override[1], override[2], override[3], override[4], override[5]), x + 14, y + 24);
        font.draw(batch, "Slot options: " + optionCount + "  current: " + progress + "  |  C export  P save", x + 14, y + 6);

        if (!exportStatus.isBlank()) {
            font.setColor(0.93f, 0.95f, 0.84f, 1f);
            font.draw(batch, "Export: " + exportStatus, x + 290, y + PANEL_H - 86);
        }
        if (!exportSnippet.isBlank()) {
            String compact = exportSnippet.replace('\n', ' ').trim();
            if (compact.length() > 68) {
                compact = compact.substring(0, 68) + "...";
            }
            font.setColor(0.72f, 0.78f, 0.90f, 1f);
            font.draw(batch, compact, x + 290, y + PANEL_H - 110);
        }
    }

    private void renderWorldPlacementText(SpriteBatch batch, BitmapFont font, int x, int y) {
        String key = sceneEditState == null ? "" : sceneEditState.selectedPlaceableKey();
        float rot = sceneEditState == null ? 0f : sceneEditState.previewRotationYDegrees();
        float scale = sceneEditState == null ? 1f : sceneEditState.previewScale();
        int count = sceneEditState == null ? 0 : sceneEditState.placements().size();
        int selected = sceneEditState == null ? -1 : sceneEditState.selectedPlacementIndex();
        boolean dirty = sceneEditState != null && sceneEditState.dirty();

        font.setColor(0.82f, 0.88f, 0.98f, 1f);
        font.draw(batch, "Place key:", x + 14, y + PANEL_H - 86);
        font.setColor(0.99f, 0.96f, 0.72f, 1f);
        font.draw(batch, key.isBlank() ? "(no placeable keys)" : key, x + 84, y + PANEL_H - 86);

        font.setColor(0.80f, 0.84f, 0.90f, 1f);
        font.draw(batch, String.format("Preview rot_y: %.1f   scale: %.2f", rot, scale), x + 14, y + 64);
        font.draw(batch, "[ / ] key   , / . rotate   - / = scale", x + 14, y + 44);
        font.draw(batch, "LMB place/select   Backspace delete selected   R reset preview", x + 14, y + 24);
        font.draw(batch, "P save scene   placements: " + count + "   selected: " + selected + "   dirty: " + dirty, x + 14, y + 6);

        if (!worldPlacementStatus.isBlank()) {
            font.setColor(0.93f, 0.95f, 0.84f, 1f);
            font.draw(batch, "Status: " + worldPlacementStatus, x + 300, y + PANEL_H - 86);
        }
    }

    private ModelLibrary.EquipmentPreviewOption selectedOptionForSlot(int slot) {
        int index = equipmentSelectionIndexBySlot.getOrDefault(slot, -1);
        List<ModelLibrary.EquipmentPreviewOption> options = equipmentOptionsBySlot.getOrDefault(slot, List.of());
        if (index < 0 || index >= options.size()) {
            return null;
        }
        return options.get(index);
    }
}
