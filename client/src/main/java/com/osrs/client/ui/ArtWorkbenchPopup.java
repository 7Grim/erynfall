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

    private static final int PANEL_W = 760;
    private static final int PANEL_H = 320;
    private static final int HEADER_H = 42;
    private static final int STATUS_H = 56;
    private static final int FOOTER_H = 34;
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

    public void toggleWorldPlacementVisibilityGroup() {
        if (mode == Mode.WORLD_PLACEMENT && sceneEditState != null) {
            sceneEditState.toggleVisibilityGroupSelectedOrPreview();
        }
    }

    public void cycleWorldPlacementVisibilityFilter() {
        if (mode == Mode.WORLD_PLACEMENT && sceneEditState != null) {
            sceneEditState.cycleVisibilityFilter();
        }
    }

    public boolean duplicateWorldPlacementSelectedToPreview() {
        return mode == Mode.WORLD_PLACEMENT
            && sceneEditState != null
            && sceneEditState.duplicateSelectedToPreviewState();
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
        int headerY = y + PANEL_H - HEADER_H;
        int statusY = y + FOOTER_H;
        int bodyY = statusY + STATUS_H;
        int bodyH = headerY - bodyY;
        int splitX = x + (PANEL_W / 2);
        int leftX = x + 14;
        int rightX = splitX + 12;
        int leftW = splitX - x - 24;
        int rightW = x + PANEL_W - splitX - 24;

        shapeRenderer.setProjectionMatrix(projection);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(new Color(0.03f, 0.04f, 0.06f, 0.86f));
        shapeRenderer.rect(x, y, PANEL_W, PANEL_H);
        shapeRenderer.setColor(new Color(0.10f, 0.13f, 0.18f, 0.95f));
        shapeRenderer.rect(x + 2, headerY + 2, PANEL_W - 4, HEADER_H - 4);
        shapeRenderer.setColor(new Color(0.07f, 0.09f, 0.13f, 0.95f));
        shapeRenderer.rect(x + 2, statusY + 2, PANEL_W - 4, STATUS_H - 4);
        shapeRenderer.setColor(new Color(0.06f, 0.08f, 0.11f, 0.95f));
        shapeRenderer.rect(x + 2, y + 2, PANEL_W - 4, FOOTER_H - 4);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(new Color(0.74f, 0.80f, 0.92f, 1f));
        shapeRenderer.rect(x, y, PANEL_W, PANEL_H);
        shapeRenderer.rect(x + 2, headerY + 2, PANEL_W - 4, HEADER_H - 4);
        shapeRenderer.rect(x + 2, statusY + 2, PANEL_W - 4, STATUS_H - 4);
        shapeRenderer.rect(x + 2, y + 2, PANEL_W - 4, FOOTER_H - 4);
        shapeRenderer.line(splitX, bodyY + 6, splitX, headerY - 6);
        shapeRenderer.end();

        batch.setProjectionMatrix(projection);
        batch.begin();
        font.getData().setScale(0.90f);
        font.setColor(0.92f, 0.95f, 1f, 1f);
        font.draw(batch, "Art Workbench", x + 12, y + PANEL_H - 12);

        font.getData().setScale(0.70f);
        font.setColor(0.82f, 0.88f, 0.98f, 1f);
        font.draw(batch, "Mode:", x + 230, y + PANEL_H - 16);
        font.setColor(0.99f, 0.96f, 0.72f, 1f);
        font.draw(batch, mode.name(), x + 272, y + PANEL_H - 16);

        if (mode != Mode.WORLD_PLACEMENT) {
            font.setColor(0.82f, 0.88f, 0.98f, 1f);
            font.draw(batch, "Clip:", x + 520, y + PANEL_H - 16);
            font.setColor(0.96f, 0.96f, 0.96f, 1f);
            font.draw(batch, clipMode.name(), x + 558, y + PANEL_H - 16);
        } else {
            boolean dirty = sceneEditState != null && sceneEditState.dirty();
            font.setColor(dirty ? 1f : 0.72f, dirty ? 0.9f : 0.84f, 0.62f, 1f);
            font.draw(batch, dirty ? "DIRTY" : "CLEAN", x + PANEL_W - 78, y + PANEL_H - 16);
        }

        int topY = bodyY + bodyH - 12;
        if (mode == Mode.MODEL_PREVIEW) {
            renderModelPreviewText(batch, font, leftX, topY, rightX, topY, leftW, rightW);
        } else if (mode == Mode.EQUIPMENT_FIT) {
            renderEquipmentFitText(batch, font, leftX, topY, rightX, topY, leftW, rightW);
        } else {
            renderWorldPlacementText(batch, font, leftX, topY, rightX, topY, leftW, rightW);
        }

        String statusA = "";
        String statusB = "";
        if (mode == Mode.EQUIPMENT_FIT) {
            statusA = exportStatus;
            statusB = exportSnippet == null ? "" : exportSnippet.replace('\n', ' ').trim();
        } else if (mode == Mode.WORLD_PLACEMENT) {
            statusA = worldPlacementStatus;
        }
        font.setColor(0.93f, 0.95f, 0.84f, 1f);
        font.draw(batch, truncateToWidth(font, statusA.isBlank() ? "Status: -" : "Status: " + statusA, PANEL_W - 28), x + 14, statusY + STATUS_H - 16);
        if (!statusB.isBlank()) {
            font.setColor(0.72f, 0.78f, 0.90f, 1f);
            font.draw(batch, truncateToWidth(font, statusB, PANEL_W - 28), x + 14, statusY + STATUS_H - 36);
        }

        font.setColor(0.80f, 0.84f, 0.90f, 1f);
        font.draw(batch, "F6 close   TAB mode   LMB drag orbit   wheel zoom   MMB reset camera   F7/F8 debug", x + 14, y + 22);

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private void renderModelPreviewText(SpriteBatch batch,
                                        BitmapFont font,
                                        int leftX,
                                        int leftTopY,
                                        int rightX,
                                        int rightTopY,
                                        int leftWidth,
                                        int rightWidth) {
        int y = leftTopY;
        font.setColor(0.82f, 0.88f, 0.98f, 1f);
        font.draw(batch, "Selection", leftX, y);
        y -= 22;
        font.setColor(0.80f, 0.84f, 0.90f, 1f);
        font.draw(batch, "Model key:", leftX, y);
        String key = selectedModelKey();
        if (key.isBlank()) key = "(no loaded models)";
        font.setColor(0.99f, 0.96f, 0.72f, 1f);
        font.draw(batch, truncateToWidth(font, key, leftWidth - 88), leftX + 84, y);
        y -= 22;
        font.setColor(0.80f, 0.84f, 0.90f, 1f);
        font.draw(batch, "Neutral isolated preview", leftX, y);

        y = rightTopY;
        font.setColor(0.82f, 0.88f, 0.98f, 1f);
        font.draw(batch, "Actions", rightX, y);
        y -= 22;
        font.setColor(0.80f, 0.84f, 0.90f, 1f);
        font.draw(batch, truncateToWidth(font, "[ / ] previous/next model", rightWidth), rightX, y);
        y -= 20;
        font.draw(batch, truncateToWidth(font, "; cycle clip (AUTO/IDLE/WALK)", rightWidth), rightX, y);
    }

    private void renderEquipmentFitText(SpriteBatch batch,
                                        BitmapFont font,
                                        int leftX,
                                        int leftTopY,
                                        int rightX,
                                        int rightTopY,
                                        int leftWidth,
                                        int rightWidth) {
        int slot = activeSlot();
        ModelLibrary.EquipmentPreviewOption option = selectedOptionForSlot(slot);
        float[] override = transformOverridesBySlot.getOrDefault(slot, new float[6]);
        List<ModelLibrary.EquipmentPreviewOption> options = equipmentOptionsBySlot.getOrDefault(slot, List.of());
        int optionCount = options.size();
        int selected = equipmentSelectionIndexBySlot.getOrDefault(slot, -1);
        String progress = selected < 0 ? "empty" : (selected + 1) + " / " + optionCount;

        int y = leftTopY;
        font.setColor(0.82f, 0.88f, 0.98f, 1f);
        font.draw(batch, "Selection", leftX, y);
        y -= 22;
        font.setColor(0.80f, 0.84f, 0.90f, 1f);
        font.draw(batch, "Slot:", leftX, y);
        font.setColor(0.99f, 0.96f, 0.72f, 1f);
        font.draw(batch, activeSlotLabel(), leftX + 42, y);
        y -= 22;
        font.setColor(0.80f, 0.84f, 0.90f, 1f);
        font.draw(batch, "Item:", leftX, y);
        String itemText = "(empty)";
        if (option != null) {
            String name = option.itemName() == null || option.itemName().isBlank() ? "item " + option.itemId() : option.itemName();
            itemText = name + " [" + option.modelKey() + "]";
        }
        font.setColor(0.96f, 0.96f, 0.96f, 1f);
        font.draw(batch, truncateToWidth(font, itemText, leftWidth - 44), leftX + 42, y);
        y -= 24;
        font.setColor(0.82f, 0.88f, 0.98f, 1f);
        font.draw(batch, "Transform", leftX, y);
        y -= 22;
        font.setColor(0.80f, 0.84f, 0.90f, 1f);
        font.draw(batch, String.format("Offset: %.3f, %.3f, %.3f", override[0], override[1], override[2]), leftX, y);
        y -= 20;
        font.draw(batch, String.format("Rotate: %.2f, %.2f, %.2f", override[3], override[4], override[5]), leftX, y);
        y -= 20;
        font.draw(batch, "Slot options: " + optionCount + "  current: " + progress, leftX, y);

        y = rightTopY;
        font.setColor(0.82f, 0.88f, 0.98f, 1f);
        font.draw(batch, "Actions", rightX, y);
        y -= 22;
        font.setColor(0.80f, 0.84f, 0.90f, 1f);
        font.draw(batch, truncateToWidth(font, "< / > slot (comma/period)", rightWidth), rightX, y);
        y -= 20;
        font.draw(batch, truncateToWidth(font, "[ / ] item option   Backspace clear", rightWidth), rightX, y);
        y -= 20;
        font.draw(batch, truncateToWidth(font, "W/S Y  A/D X  Q/E Z", rightWidth), rightX, y);
        y -= 20;
        font.draw(batch, truncateToWidth(font, "I/K RX  J/L RY  U/O RZ", rightWidth), rightX, y);
        y -= 20;
        font.draw(batch, truncateToWidth(font, "Shift fine   Ctrl coarse   R reset", rightWidth), rightX, y);
        y -= 20;
        font.draw(batch, truncateToWidth(font, "C export snippet   P save manifest", rightWidth), rightX, y);
    }

    private void renderWorldPlacementText(SpriteBatch batch,
                                          BitmapFont font,
                                          int leftX,
                                          int leftTopY,
                                          int rightX,
                                          int rightTopY,
                                          int leftWidth,
                                          int rightWidth) {
        String key = sceneEditState == null ? "" : sceneEditState.selectedPlaceableKey();
        float rot = sceneEditState == null ? 0f : sceneEditState.previewRotationYDegrees();
        float scale = sceneEditState == null ? 1f : sceneEditState.previewScale();
        String vis = sceneEditState == null ? "base" : sceneEditState.previewVisibilityGroup();
        String filter = sceneEditState == null ? "ALL" : sceneEditState.visibilityFilter().name();
        int count = sceneEditState == null ? 0 : sceneEditState.placements().size();
        int selected = sceneEditState == null ? -1 : sceneEditState.selectedPlacementIndex();
        String selectedVis = (sceneEditState != null && sceneEditState.selectedPlacement() != null)
            ? sceneEditState.selectedPlacement().visibilityGroup
            : "none";
        boolean dirty = sceneEditState != null && sceneEditState.dirty();

        int y = leftTopY;
        font.setColor(0.82f, 0.88f, 0.98f, 1f);
        font.draw(batch, "Selection", leftX, y);
        y -= 22;
        font.setColor(0.80f, 0.84f, 0.90f, 1f);
        font.draw(batch, "Place key:", leftX, y);
        font.setColor(0.99f, 0.96f, 0.72f, 1f);
        font.draw(batch, truncateToWidth(font, key.isBlank() ? "(no placeable keys)" : key, leftWidth - 82), leftX + 80, y);
        y -= 22;
        font.setColor(0.80f, 0.84f, 0.90f, 1f);
        font.draw(batch, String.format("Preview rot_y: %.1f", rot), leftX, y);
        y -= 20;
        font.draw(batch, String.format("Preview scale: %.2f", scale), leftX, y);
        y -= 20;
        font.draw(batch, "Preview vis: " + vis, leftX, y);
        y -= 20;
        font.draw(batch, "Selected: " + selected + " (" + selectedVis + ")", leftX, y);
        y -= 20;
        font.draw(batch, "Filter: " + filter + "   dirty: " + dirty + "   count: " + count, leftX, y);

        y = rightTopY;
        font.setColor(0.82f, 0.88f, 0.98f, 1f);
        font.draw(batch, "Actions", rightX, y);
        y -= 22;
        font.setColor(0.80f, 0.84f, 0.90f, 1f);
        font.draw(batch, truncateToWidth(font, "[ / ] key", rightWidth), rightX, y);
        y -= 20;
        font.draw(batch, truncateToWidth(font, ", / . rotate   - / = scale", rightWidth), rightX, y);
        y -= 20;
        font.draw(batch, truncateToWidth(font, "V vis-group toggle   Y filter cycle", rightWidth), rightX, y);
        y -= 20;
        font.draw(batch, truncateToWidth(font, "LMB place/select (re-click cycles)", rightWidth), rightX, y);
        y -= 20;
        font.draw(batch, truncateToWidth(font, "N cycle hovered   D duplicate->preview", rightWidth), rightX, y);
        y -= 20;
        font.draw(batch, truncateToWidth(font, "Backspace delete   R reset preview", rightWidth), rightX, y);
        y -= 20;
        font.draw(batch, truncateToWidth(font, "ESC deselect first, then close", rightWidth), rightX, y);
        y -= 20;
        font.draw(batch, truncateToWidth(font, "P save scene", rightWidth), rightX, y);
    }

    private String truncateToWidth(BitmapFont font, String text, float maxWidth) {
        if (text == null) {
            return "";
        }
        glyph.setText(font, text);
        if (glyph.width <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int end = text.length();
        while (end > 0) {
            String candidate = text.substring(0, end) + ellipsis;
            glyph.setText(font, candidate);
            if (glyph.width <= maxWidth) {
                return candidate;
            }
            end--;
        }
        return ellipsis;
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
