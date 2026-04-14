package com.osrs.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.Input;
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
        WORLD_PLACEMENT,
        ENTITY_BINDING,
        TERRAIN_PAINT
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
    private String entityBindingStatus = "";
    private String terrainPaintStatus = "";
    private SceneEditState sceneEditState;
    private String modelSearchQuery = "";
    private final Map<Integer, String> equipmentSearchBySlot = new HashMap<>();
    private String worldSearchQuery = "";
    private String entityBindingSearchQuery = "";
    private boolean inWorldFreeCameraEnabled = false;
    private boolean selectionSearchActive = false;
    private int declaredModelCount = 0;
    private int loadedModelCount = 0;
    private int failedModelCount = 0;
    private String firstFailedModelSummary = "";
    private int entityBindingSelectedEntityId = -1;
    private int entityBindingSelectedDefinitionId = -1;
    private String entityBindingSelectedName = "";
    private String entityBindingCurrentModelKey = "";
    private boolean entityBindingCurrentAnimated3d = false;
    private String entityBindingCandidateModelKey = "";
    private int entityBindingCandidateModelIndex = -1;
    private boolean entityBindingCandidateAnimated3d = false;
    private boolean entityBindingDirty = false;

    public void setModelKeys(List<String> keys) {
        modelKeys.clear();
        if (keys != null) {
            modelKeys.addAll(keys);
        }
        Collections.sort(modelKeys);
        if (modelKeys.isEmpty()) {
            selectedIndex = -1;
        } else if (selectedIndex >= modelKeys.size()) {
            selectedIndex = modelKeys.size() - 1;
        }
        applySearchSelection();
    }

    public void setModelLoadSummary(int declared, int loaded, int failed, String firstFailureSummary) {
        declaredModelCount = Math.max(0, declared);
        loadedModelCount = Math.max(0, loaded);
        failedModelCount = Math.max(0, failed);
        firstFailedModelSummary = firstFailureSummary == null ? "" : firstFailureSummary;
    }

    public void setEquipmentOptions(Map<Integer, List<ModelLibrary.EquipmentPreviewOption>> optionsBySlot) {
        equipmentOptionsBySlot.clear();
        if (optionsBySlot != null) {
            equipmentOptionsBySlot.putAll(optionsBySlot);
        }
        for (int slot : VISIBLE_SLOTS) {
            equipmentSelectionIndexBySlot.putIfAbsent(slot, -1);
            equipmentSearchBySlot.putIfAbsent(slot, "");
            List<ModelLibrary.EquipmentPreviewOption> options = equipmentOptionsBySlot.getOrDefault(slot, List.of());
            int current = equipmentSelectionIndexBySlot.getOrDefault(slot, -1);
            if (current >= options.size()) {
                equipmentSelectionIndexBySlot.put(slot, -1);
            }
        }
        applySearchSelection();
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

    public boolean isSelectionSearchActive() {
        return selectionSearchActive;
    }

    public void activateSelectionSearch() {
        selectionSearchActive = true;
        applySearchSelection();
    }

    public void cancelSelectionSearch() {
        selectionSearchActive = false;
    }

    public void confirmSelectionSearch() {
        applySearchSelection();
        selectionSearchActive = false;
    }

    public String selectionSearchQuery() {
        return activeSearchQuery();
    }

    public int selectionSearchMatchCount() {
        return switch (mode) {
            case MODEL_PREVIEW -> filteredModelIndices().size();
            case EQUIPMENT_FIT -> filteredEquipmentIndices(activeSlot()).size();
            case WORLD_PLACEMENT -> filteredWorldIndices().size();
            case ENTITY_BINDING -> filteredEntityBindingModelIndices().size();
            case TERRAIN_PAINT -> 0;
        };
    }

    public boolean handleSelectionSearchKey(int keycode, boolean shiftDown) {
        String query = activeSearchQuery();
        if (keycode == Input.Keys.BACKSPACE) {
            if (!query.isEmpty()) {
                updateActiveSearchQuery(query.substring(0, query.length() - 1));
            }
            return true;
        }
        if (keycode == Input.Keys.SPACE) {
            updateActiveSearchQuery(query + " ");
            return true;
        }
        if (keycode == Input.Keys.APOSTROPHE) {
            updateActiveSearchQuery(query + '\'');
            return true;
        }
        if (keycode == Input.Keys.MINUS) {
            updateActiveSearchQuery(query + '-');
            return true;
        }
        if (keycode >= Input.Keys.A && keycode <= Input.Keys.Z) {
            char c = Input.Keys.toString(keycode).charAt(0);
            if (!shiftDown) {
                c = Character.toLowerCase(c);
            }
            updateActiveSearchQuery(query + c);
            return true;
        }
        if (keycode >= Input.Keys.NUM_0 && keycode <= Input.Keys.NUM_9) {
            updateActiveSearchQuery(query + (char) ('0' + (keycode - Input.Keys.NUM_0)));
            return true;
        }
        return false;
    }

    public void selectNext() {
        if (mode == Mode.EQUIPMENT_FIT) {
            cycleEquipmentOption(1);
            return;
        }
        if (mode == Mode.WORLD_PLACEMENT) {
            cycleWorldPlacementProp(1);
            return;
        }
        if (mode == Mode.ENTITY_BINDING) {
            cycleEntityBindingModel(1);
            return;
        }
        if (mode == Mode.TERRAIN_PAINT) {
            cycleTerrainPaintType(1);
            return;
        }
        List<Integer> filtered = filteredModelIndices();
        if (filtered.isEmpty()) {
            selectedIndex = -1;
            return;
        }
        int current = filtered.indexOf(selectedIndex);
        selectedIndex = current < 0
            ? filtered.get(0)
            : filtered.get((current + 1) % filtered.size());
    }

    public void selectPrevious() {
        if (mode == Mode.EQUIPMENT_FIT) {
            cycleEquipmentOption(-1);
            return;
        }
        if (mode == Mode.WORLD_PLACEMENT) {
            cycleWorldPlacementProp(-1);
            return;
        }
        if (mode == Mode.ENTITY_BINDING) {
            cycleEntityBindingModel(-1);
            return;
        }
        if (mode == Mode.TERRAIN_PAINT) {
            cycleTerrainPaintType(-1);
            return;
        }
        List<Integer> filtered = filteredModelIndices();
        if (filtered.isEmpty()) {
            selectedIndex = -1;
            return;
        }
        int current = filtered.indexOf(selectedIndex);
        selectedIndex = current < 0
            ? filtered.get(0)
            : filtered.get((current - 1 + filtered.size()) % filtered.size());
    }

    public void cycleMode() {
        mode = switch (mode) {
            case MODEL_PREVIEW -> Mode.EQUIPMENT_FIT;
            case EQUIPMENT_FIT -> Mode.WORLD_PLACEMENT;
            case WORLD_PLACEMENT -> Mode.ENTITY_BINDING;
            case ENTITY_BINDING -> Mode.TERRAIN_PAINT;
            case TERRAIN_PAINT -> Mode.MODEL_PREVIEW;
        };
        selectionSearchActive = false;
        applySearchSelection();
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
        applySearchSelection();
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
        applySearchSelection();
    }

    public void setWorldPlacementStatus(String status) {
        this.worldPlacementStatus = status == null ? "" : status;
    }

    public void setEntityBindingStatus(String status) {
        this.entityBindingStatus = status == null ? "" : status;
    }

    public void setEntityBindingSelection(int entityId,
                                          int definitionId,
                                          String entityName,
                                          String currentModelKey,
                                          boolean currentAnimated3d) {
        entityBindingSelectedEntityId = entityId;
        entityBindingSelectedDefinitionId = definitionId;
        entityBindingSelectedName = entityName == null ? "" : entityName;
        entityBindingCurrentModelKey = currentModelKey == null ? "" : currentModelKey;
        entityBindingCurrentAnimated3d = currentAnimated3d;
        entityBindingCandidateAnimated3d = currentAnimated3d;
        setEntityBindingCandidateModelKey(entityBindingCurrentModelKey);
        entityBindingDirty = false;
        applySearchSelection();
    }

    public void clearEntityBindingSelection() {
        entityBindingSelectedEntityId = -1;
        entityBindingSelectedDefinitionId = -1;
        entityBindingSelectedName = "";
        entityBindingCurrentModelKey = "";
        entityBindingCurrentAnimated3d = false;
        entityBindingCandidateModelKey = "";
        entityBindingCandidateModelIndex = -1;
        entityBindingCandidateAnimated3d = false;
        entityBindingDirty = false;
    }

    public boolean hasEntityBindingSelection() {
        return entityBindingSelectedEntityId >= 0 && entityBindingSelectedDefinitionId > 0;
    }

    public int selectedEntityBindingDefinitionId() {
        return entityBindingSelectedDefinitionId;
    }

    public String selectedEntityBindingName() {
        return entityBindingSelectedName;
    }

    public String entityBindingCandidateModelKey() {
        return entityBindingCandidateModelKey;
    }

    public boolean entityBindingCandidateAnimated3d() {
        return entityBindingCandidateAnimated3d;
    }

    public void toggleEntityBindingAnimated3d() {
        if (!hasEntityBindingSelection()) {
            return;
        }
        entityBindingCandidateAnimated3d = !entityBindingCandidateAnimated3d;
        entityBindingDirty = true;
    }

    public void clearEntityBindingCandidateModelKey() {
        if (!hasEntityBindingSelection()) {
            return;
        }
        entityBindingCandidateModelKey = "";
        entityBindingCandidateModelIndex = -1;
        entityBindingDirty = true;
    }

    public void markEntityBindingSaved() {
        entityBindingCurrentModelKey = entityBindingCandidateModelKey;
        entityBindingCurrentAnimated3d = entityBindingCandidateAnimated3d;
        entityBindingDirty = false;
    }

    public void setTerrainPaintStatus(String status) {
        this.terrainPaintStatus = status == null ? "" : status;
    }

    public void setInWorldFreeCameraEnabled(boolean enabled) {
        this.inWorldFreeCameraEnabled = enabled;
    }

    public void cycleTerrainPaintType(int direction) {
        if (mode == Mode.TERRAIN_PAINT && sceneEditState != null) {
            sceneEditState.cycleSelectedTerrainType(direction);
        }
    }

    public void cycleEntityBindingModel(int direction) {
        if (mode != Mode.ENTITY_BINDING || !hasEntityBindingSelection()) {
            return;
        }
        List<Integer> filtered = filteredEntityBindingModelIndices();
        if (filtered.isEmpty()) {
            return;
        }
        int current = filtered.indexOf(entityBindingCandidateModelIndex);
        int next = current < 0
            ? filtered.get(0)
            : filtered.get((current + (direction >= 0 ? 1 : -1) + filtered.size()) % filtered.size());
        entityBindingCandidateModelIndex = next;
        entityBindingCandidateModelKey = modelKeys.get(next);
        entityBindingDirty = true;
    }

    public void cycleWorldPlacementProp(int direction) {
        if (mode == Mode.WORLD_PLACEMENT && sceneEditState != null) {
            List<Integer> filtered = filteredWorldIndices();
            if (filtered.isEmpty()) {
                return;
            }
            int current = filtered.indexOf(sceneEditState.selectedPlaceableKeyIndex());
            if (current < 0) {
                sceneEditState.setSelectedPlaceableKeyIndex(filtered.get(0));
                return;
            }
            int next = (current + (direction >= 0 ? 1 : -1) + filtered.size()) % filtered.size();
            sceneEditState.setSelectedPlaceableKeyIndex(filtered.get(next));
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
        List<Integer> filtered = filteredEquipmentIndices(slot);
        if (filtered.isEmpty()) {
            equipmentSelectionIndexBySlot.put(slot, -1);
            return;
        }
        int current = equipmentSelectionIndexBySlot.getOrDefault(slot, -1);
        if (activeSearchQuery().isBlank()) {
            List<ModelLibrary.EquipmentPreviewOption> options = equipmentOptionsBySlot.getOrDefault(slot, List.of());
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
            return;
        }

        int pos = filtered.indexOf(current);
        if (pos < 0) {
            equipmentSelectionIndexBySlot.put(slot, filtered.get(0));
            return;
        }
        if (direction > 0) {
            pos = (pos + 1) % filtered.size();
        } else {
            pos = (pos - 1 + filtered.size()) % filtered.size();
        }
        equipmentSelectionIndexBySlot.put(slot, filtered.get(pos));
    }

    private void applySearchSelection() {
        switch (mode) {
            case MODEL_PREVIEW -> {
                List<Integer> filtered = filteredModelIndices();
                if (filtered.isEmpty()) {
                    selectedIndex = -1;
                } else if (!filtered.contains(selectedIndex)) {
                    selectedIndex = filtered.get(0);
                }
            }
            case EQUIPMENT_FIT -> {
                int slot = activeSlot();
                List<Integer> filtered = filteredEquipmentIndices(slot);
                if (filtered.isEmpty()) {
                    equipmentSelectionIndexBySlot.put(slot, -1);
                } else {
                    int selected = equipmentSelectionIndexBySlot.getOrDefault(slot, -1);
                    if (!filtered.contains(selected)) {
                        equipmentSelectionIndexBySlot.put(slot, filtered.get(0));
                    }
                }
            }
            case WORLD_PLACEMENT -> {
                List<Integer> filtered = filteredWorldIndices();
                if (sceneEditState == null || filtered.isEmpty()) {
                    return;
                }
                if (!filtered.contains(sceneEditState.selectedPlaceableKeyIndex())) {
                    sceneEditState.setSelectedPlaceableKeyIndex(filtered.get(0));
                }
            }
            case ENTITY_BINDING -> {
                if (!hasEntityBindingSelection()) {
                    return;
                }
                List<Integer> filtered = filteredEntityBindingModelIndices();
                if (filtered.isEmpty()) {
                    entityBindingCandidateModelIndex = -1;
                    entityBindingCandidateModelKey = "";
                    return;
                }
                if (!filtered.contains(entityBindingCandidateModelIndex)) {
                    entityBindingCandidateModelIndex = filtered.get(0);
                    entityBindingCandidateModelKey = modelKeys.get(entityBindingCandidateModelIndex);
                }
            }
            case TERRAIN_PAINT -> {
                // no searchable list in terrain paint mode
            }
        }
    }

    private String activeSearchQuery() {
        return switch (mode) {
            case MODEL_PREVIEW -> modelSearchQuery;
            case EQUIPMENT_FIT -> equipmentSearchBySlot.getOrDefault(activeSlot(), "");
            case WORLD_PLACEMENT -> worldSearchQuery;
            case ENTITY_BINDING -> entityBindingSearchQuery;
            case TERRAIN_PAINT -> "";
        };
    }

    private void updateActiveSearchQuery(String query) {
        String normalized = query == null ? "" : query;
        switch (mode) {
            case MODEL_PREVIEW -> modelSearchQuery = normalized;
            case EQUIPMENT_FIT -> equipmentSearchBySlot.put(activeSlot(), normalized);
            case WORLD_PLACEMENT -> worldSearchQuery = normalized;
            case ENTITY_BINDING -> entityBindingSearchQuery = normalized;
            case TERRAIN_PAINT -> {
            }
        }
        applySearchSelection();
    }

    private List<Integer> filteredModelIndices() {
        ArrayList<Integer> out = new ArrayList<>();
        String query = modelSearchQuery.trim().toLowerCase();
        for (int i = 0; i < modelKeys.size(); i++) {
            if (query.isEmpty() || modelKeys.get(i).toLowerCase().contains(query)) {
                out.add(i);
            }
        }
        return out;
    }

    private List<Integer> filteredEquipmentIndices(int slot) {
        ArrayList<Integer> out = new ArrayList<>();
        List<ModelLibrary.EquipmentPreviewOption> options = equipmentOptionsBySlot.getOrDefault(slot, List.of());
        String query = equipmentSearchBySlot.getOrDefault(slot, "").trim().toLowerCase();
        for (int i = 0; i < options.size(); i++) {
            ModelLibrary.EquipmentPreviewOption option = options.get(i);
            if (query.isEmpty()) {
                out.add(i);
                continue;
            }
            String name = option.itemName() == null ? "" : option.itemName();
            String haystack = (name + " " + option.modelKey() + " " + option.itemId()).toLowerCase();
            if (haystack.contains(query)) {
                out.add(i);
            }
        }
        return out;
    }

    private List<Integer> filteredWorldIndices() {
        if (sceneEditState == null) {
            return List.of();
        }
        ArrayList<Integer> out = new ArrayList<>();
        List<String> keys = sceneEditState.placeableKeys();
        String query = worldSearchQuery.trim().toLowerCase();
        for (int i = 0; i < keys.size(); i++) {
            if (query.isEmpty() || keys.get(i).toLowerCase().contains(query)) {
                out.add(i);
            }
        }
        return out;
    }

    private List<Integer> filteredEntityBindingModelIndices() {
        ArrayList<Integer> out = new ArrayList<>();
        String query = entityBindingSearchQuery.trim().toLowerCase();
        for (int i = 0; i < modelKeys.size(); i++) {
            if (query.isEmpty() || modelKeys.get(i).toLowerCase().contains(query)) {
                out.add(i);
            }
        }
        return out;
    }

    private void setEntityBindingCandidateModelKey(String modelKey) {
        String normalized = modelKey == null ? "" : modelKey.trim();
        entityBindingCandidateModelKey = normalized;
        entityBindingCandidateModelIndex = -1;
        if (normalized.isBlank()) {
            return;
        }
        for (int i = 0; i < modelKeys.size(); i++) {
            if (normalized.equals(modelKeys.get(i))) {
                entityBindingCandidateModelIndex = i;
                break;
            }
        }
    }

    public String selectedModelKey() {
        if (selectedIndex < 0 || selectedIndex >= modelKeys.size()) {
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

        if (mode == Mode.MODEL_PREVIEW || mode == Mode.EQUIPMENT_FIT) {
            font.setColor(0.82f, 0.88f, 0.98f, 1f);
            font.draw(batch, "Clip:", x + 520, y + PANEL_H - 16);
            font.setColor(0.96f, 0.96f, 0.96f, 1f);
            font.draw(batch, clipMode.name(), x + 558, y + PANEL_H - 16);
        } else {
            boolean dirty = switch (mode) {
                case WORLD_PLACEMENT -> sceneEditState != null && sceneEditState.dirty();
                case ENTITY_BINDING -> entityBindingDirty;
                case TERRAIN_PAINT -> sceneEditState != null && sceneEditState.terrainDirty();
                default -> false;
            };
            font.setColor(0.82f, 0.88f, 0.98f, 1f);
            font.draw(batch, "Free cam: " + (inWorldFreeCameraEnabled ? "ON" : "OFF"), x + PANEL_W - 218, y + PANEL_H - 16);
            font.setColor(dirty ? 1f : 0.72f, dirty ? 0.9f : 0.84f, 0.62f, 1f);
            font.draw(batch, dirty ? "DIRTY" : "CLEAN", x + PANEL_W - 78, y + PANEL_H - 16);
        }

        int topY = bodyY + bodyH - 12;
        if (mode == Mode.MODEL_PREVIEW) {
            renderModelPreviewText(batch, font, leftX, topY, rightX, topY, leftW, rightW);
        } else if (mode == Mode.EQUIPMENT_FIT) {
            renderEquipmentFitText(batch, font, leftX, topY, rightX, topY, leftW, rightW);
        } else if (mode == Mode.WORLD_PLACEMENT) {
            renderWorldPlacementText(batch, font, leftX, topY, rightX, topY, leftW, rightW);
        } else if (mode == Mode.ENTITY_BINDING) {
            renderEntityBindingText(batch, font, leftX, topY, rightX, topY, leftW, rightW);
        } else {
            renderTerrainPaintText(batch, font, leftX, topY, rightX, topY, leftW, rightW);
        }

        String statusA = "";
        String statusB = "";
        if (mode == Mode.EQUIPMENT_FIT) {
            statusA = exportStatus;
            statusB = exportSnippet == null ? "" : exportSnippet.replace('\n', ' ').trim();
        } else if (mode == Mode.WORLD_PLACEMENT) {
            statusA = worldPlacementStatus;
        } else if (mode == Mode.ENTITY_BINDING) {
            statusA = entityBindingStatus;
        } else if (mode == Mode.TERRAIN_PAINT) {
            statusA = terrainPaintStatus;
        }
        font.setColor(0.93f, 0.95f, 0.84f, 1f);
        font.draw(batch, truncateToWidth(font, statusA.isBlank() ? "Status: -" : "Status: " + statusA, PANEL_W - 28), x + 14, statusY + STATUS_H - 16);
        if (!statusB.isBlank()) {
            font.setColor(0.72f, 0.78f, 0.90f, 1f);
            font.draw(batch, truncateToWidth(font, statusB, PANEL_W - 28), x + 14, statusY + STATUS_H - 36);
        }

        String footer;
        if (mode == Mode.WORLD_PLACEMENT || mode == Mode.ENTITY_BINDING || mode == Mode.TERRAIN_PAINT) {
            footer = "F6 close   TAB mode   F free camera   WASD move   Q/E vertical   G reset free camera   F7/F8 debug";
        } else {
            footer = "F6 close   TAB mode   / search   LMB drag orbit   wheel zoom   MMB reset camera   F7/F8 debug";
        }
        font.setColor(0.80f, 0.84f, 0.90f, 1f);
        font.draw(batch, footer, x + 14, y + 22);

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
        String query = modelSearchQuery;
        int matches = filteredModelIndices().size();
        font.setColor(0.80f, 0.84f, 0.90f, 1f);
        font.draw(batch, "Search:", leftX, y);
        font.setColor(selectionSearchActive && mode == Mode.MODEL_PREVIEW ? 0.99f : 0.94f,
            selectionSearchActive && mode == Mode.MODEL_PREVIEW ? 0.88f : 0.94f,
            selectionSearchActive && mode == Mode.MODEL_PREVIEW ? 0.66f : 0.94f,
            1f);
        String queryText = query.isBlank() ? "(type to filter)" : query;
        font.draw(batch, truncateToWidth(font, queryText + "  [" + matches + "]", leftWidth - 60), leftX + 58, y);
        y -= 22;
        font.setColor(0.80f, 0.84f, 0.90f, 1f);
        font.draw(batch, "Models:", leftX, y);
        font.setColor(0.96f, 0.96f, 0.96f, 1f);
        font.draw(batch,
            "declared " + declaredModelCount + "   loaded " + loadedModelCount + "   failed " + failedModelCount,
            leftX + 54,
            y);
        y -= 22;
        if (matches == 0) {
            font.setColor(0.96f, 0.80f, 0.70f, 1f);
            String noMatch = query.isBlank()
                ? "No loaded models available for preview"
                : "No loaded model matches \"" + query + "\"";
            font.draw(batch, truncateToWidth(font, noMatch, leftWidth), leftX, y);
            y -= 20;
        }
        if (!firstFailedModelSummary.isBlank()) {
            font.setColor(0.96f, 0.80f, 0.70f, 1f);
            font.draw(batch, truncateToWidth(font, "First failure: " + firstFailedModelSummary, leftWidth), leftX, y);
            y -= 20;
        }
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
        y -= 20;
        font.draw(batch, truncateToWidth(font, "/ search   Enter apply   Esc exit search", rightWidth), rightX, y);
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
        String slotQuery = equipmentSearchBySlot.getOrDefault(slot, "");
        int matchCount = filteredEquipmentIndices(slot).size();
        font.setColor(0.80f, 0.84f, 0.90f, 1f);
        font.draw(batch, "Search:", leftX, y);
        font.setColor(selectionSearchActive && mode == Mode.EQUIPMENT_FIT ? 0.99f : 0.94f,
            selectionSearchActive && mode == Mode.EQUIPMENT_FIT ? 0.88f : 0.94f,
            selectionSearchActive && mode == Mode.EQUIPMENT_FIT ? 0.66f : 0.94f,
            1f);
        String slotQueryText = slotQuery.isBlank() ? "(type to filter slot items)" : slotQuery;
        font.draw(batch, truncateToWidth(font, slotQueryText + "  [" + matchCount + "]", leftWidth - 60), leftX + 58, y);
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
        y -= 20;
        font.draw(batch, truncateToWidth(font, "/ search   Enter apply   Esc exit search", rightWidth), rightX, y);
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
        font.draw(batch, "Free camera: " + (inWorldFreeCameraEnabled ? "ON" : "OFF"), leftX, y);
        y -= 22;
        int matches = filteredWorldIndices().size();
        font.setColor(0.80f, 0.84f, 0.90f, 1f);
        font.draw(batch, "Search:", leftX, y);
        font.setColor(selectionSearchActive && mode == Mode.WORLD_PLACEMENT ? 0.99f : 0.94f,
            selectionSearchActive && mode == Mode.WORLD_PLACEMENT ? 0.88f : 0.94f,
            selectionSearchActive && mode == Mode.WORLD_PLACEMENT ? 0.66f : 0.94f,
            1f);
        String worldQueryText = worldSearchQuery.isBlank() ? "(type to filter place keys)" : worldSearchQuery;
        font.draw(batch, truncateToWidth(font, worldQueryText + "  [" + matches + "]", leftWidth - 60), leftX + 58, y);
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
        y -= 20;
        font.draw(batch, truncateToWidth(font, "/ search   Enter apply   Esc exit search", rightWidth), rightX, y);
        y -= 20;
        font.draw(batch, truncateToWidth(font, "F toggle free camera   G reset camera", rightWidth), rightX, y);
    }

    private void renderTerrainPaintText(SpriteBatch batch,
                                        BitmapFont font,
                                        int leftX,
                                        int leftTopY,
                                        int rightX,
                                        int rightTopY,
                                        int leftWidth,
                                        int rightWidth) {
        int y = leftTopY;
        int terrainType = sceneEditState == null ? 0 : sceneEditState.selectedTerrainType();
        String terrainTypeLabel = sceneEditState == null ? "grass" : sceneEditState.selectedTerrainTypeLabel();
        boolean dirty = sceneEditState != null && sceneEditState.terrainDirty();
        int overrideCount = sceneEditState == null ? 0 : sceneEditState.terrainTileOverrides().size();

        font.setColor(0.82f, 0.88f, 0.98f, 1f);
        font.draw(batch, "Paint", leftX, y);
        y -= 22;
        font.setColor(0.80f, 0.84f, 0.90f, 1f);
        font.draw(batch, "Type:", leftX, y);
        font.setColor(0.99f, 0.96f, 0.72f, 1f);
        font.draw(batch, terrainTypeLabel + " (" + terrainType + ")", leftX + 42, y);
        y -= 22;
        font.setColor(0.80f, 0.84f, 0.90f, 1f);
        font.draw(batch, "Dirty: " + dirty, leftX, y);
        y -= 20;
        font.draw(batch, "Free camera: " + (inWorldFreeCameraEnabled ? "ON" : "OFF"), leftX, y);
        y -= 20;
        font.draw(batch, "Tile overrides: " + overrideCount, leftX, y);
        y -= 20;
        font.draw(batch, "Hover tile: see in-world highlight", leftX, y);
        y -= 20;
        font.draw(batch, "Writes terrain_visual.tile_overrides", leftX, y);

        y = rightTopY;
        font.setColor(0.82f, 0.88f, 0.98f, 1f);
        font.draw(batch, "Actions", rightX, y);
        y -= 22;
        font.setColor(0.80f, 0.84f, 0.90f, 1f);
        font.draw(batch, truncateToWidth(font, "[ / ] cycle terrain type", rightWidth), rightX, y);
        y -= 20;
        font.draw(batch, truncateToWidth(font, "LMB paint hovered tile", rightWidth), rightX, y);
        y -= 20;
        font.draw(batch, truncateToWidth(font, "E erase override on hovered tile", rightWidth), rightX, y);
        y -= 20;
        font.draw(batch, truncateToWidth(font, "P save scene terrain visuals", rightWidth), rightX, y);
        y -= 20;
        font.draw(batch, truncateToWidth(font, "ESC closes (or exits search)", rightWidth), rightX, y);
        y -= 20;
        font.draw(batch, truncateToWidth(font, "F toggle free camera   G reset camera", rightWidth), rightX, y);
    }

    private void renderEntityBindingText(SpriteBatch batch,
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

        if (!hasEntityBindingSelection()) {
            font.setColor(0.80f, 0.84f, 0.90f, 1f);
            font.draw(batch, "LMB a visible entity/resource to bind", leftX, y);
            y -= 22;
            font.draw(batch, "Binding is saved by definition_id archetype", leftX, y);
        } else {
            font.setColor(0.80f, 0.84f, 0.90f, 1f);
            font.draw(batch, "Entity:", leftX, y);
            font.setColor(0.99f, 0.96f, 0.72f, 1f);
            String name = entityBindingSelectedName.isBlank() ? "(unnamed)" : entityBindingSelectedName;
            font.draw(batch, truncateToWidth(font, name, leftWidth - 54), leftX + 52, y);
            y -= 22;

            font.setColor(0.80f, 0.84f, 0.90f, 1f);
            font.draw(batch, "Entity ID:", leftX, y);
            font.setColor(0.96f, 0.96f, 0.96f, 1f);
            font.draw(batch, String.valueOf(entityBindingSelectedEntityId), leftX + 66, y);
            y -= 20;

            font.setColor(0.80f, 0.84f, 0.90f, 1f);
            font.draw(batch, "Definition ID:", leftX, y);
            font.setColor(0.96f, 0.96f, 0.96f, 1f);
            font.draw(batch, String.valueOf(entityBindingSelectedDefinitionId), leftX + 88, y);
            y -= 20;

            font.setColor(0.80f, 0.84f, 0.90f, 1f);
            font.draw(batch, "Current 3D:", leftX, y);
            font.setColor(0.96f, 0.96f, 0.96f, 1f);
            String currentModel = entityBindingCurrentModelKey.isBlank() ? "(fallback)" : entityBindingCurrentModelKey;
            font.draw(batch, truncateToWidth(font, currentModel, leftWidth - 74), leftX + 70, y);
            y -= 20;

            font.setColor(0.80f, 0.84f, 0.90f, 1f);
            font.draw(batch, "Current anim:", leftX, y);
            font.setColor(0.96f, 0.96f, 0.96f, 1f);
            font.draw(batch, entityBindingCurrentAnimated3d ? "true" : "false", leftX + 78, y);
            y -= 22;

            int matches = filteredEntityBindingModelIndices().size();
            font.setColor(0.80f, 0.84f, 0.90f, 1f);
            font.draw(batch, "Search:", leftX, y);
            font.setColor(selectionSearchActive && mode == Mode.ENTITY_BINDING ? 0.99f : 0.94f,
                selectionSearchActive && mode == Mode.ENTITY_BINDING ? 0.88f : 0.94f,
                selectionSearchActive && mode == Mode.ENTITY_BINDING ? 0.66f : 0.94f,
                1f);
            String queryText = entityBindingSearchQuery.isBlank() ? "(type to filter model keys)" : entityBindingSearchQuery;
            font.draw(batch, truncateToWidth(font, queryText + "  [" + matches + "]", leftWidth - 60), leftX + 58, y);
            y -= 22;

            font.setColor(0.80f, 0.84f, 0.90f, 1f);
            font.draw(batch, "Candidate 3D:", leftX, y);
            font.setColor(0.99f, 0.96f, 0.72f, 1f);
            String candidateModel = entityBindingCandidateModelKey.isBlank() ? "(fallback)" : entityBindingCandidateModelKey;
            font.draw(batch, truncateToWidth(font, candidateModel, leftWidth - 86), leftX + 82, y);
            y -= 20;

            font.setColor(0.80f, 0.84f, 0.90f, 1f);
            font.draw(batch, "Candidate anim:", leftX, y);
            font.setColor(0.99f, 0.96f, 0.72f, 1f);
            font.draw(batch, entityBindingCandidateAnimated3d ? "true" : "false", leftX + 92, y);
            y -= 22;
            font.setColor(0.80f, 0.84f, 0.90f, 1f);
            font.draw(batch, "Free camera: " + (inWorldFreeCameraEnabled ? "ON" : "OFF"), leftX, y);
        }

        y = rightTopY;
        font.setColor(0.82f, 0.88f, 0.98f, 1f);
        font.draw(batch, "Actions", rightX, y);
        y -= 22;
        font.setColor(0.80f, 0.84f, 0.90f, 1f);
        font.draw(batch, truncateToWidth(font, "LMB select visible entity/resource", rightWidth), rightX, y);
        y -= 20;
        font.draw(batch, truncateToWidth(font, "[ / ] cycle candidate model key", rightWidth), rightX, y);
        y -= 20;
        font.draw(batch, truncateToWidth(font, "/ search   Enter apply   Esc exit search", rightWidth), rightX, y);
        y -= 20;
        font.draw(batch, truncateToWidth(font, "V toggle animated_3d", rightWidth), rightX, y);
        y -= 20;
        font.draw(batch, truncateToWidth(font, "Backspace clear model key (fallback)", rightWidth), rightX, y);
        y -= 20;
        font.draw(batch, truncateToWidth(font, "P save binding to entity_visuals.yaml", rightWidth), rightX, y);
        y -= 20;
        font.draw(batch, truncateToWidth(font, "ESC deselect first, then close", rightWidth), rightX, y);
        y -= 20;
        font.draw(batch, truncateToWidth(font, "F toggle free camera   G reset camera", rightWidth), rightX, y);
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
