package com.osrs.client.art;

import com.osrs.client.world.StaticPropLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SceneEditState {

    public enum VisibilityFilter {
        ALL,
        BASE_ONLY,
        ROOF_ONLY
    }

    private final ArrayList<StaticPropLoader.StaticPropPlacement> placements = new ArrayList<>();
    private final ArrayList<String> placeableKeys = new ArrayList<>();
    private int selectedPlaceableKeyIndex = 0;
    private int selectedPlacementIndex = -1;
    private float previewRotationYDegrees = 0f;
    private float previewScale = 1f;
    private String previewVisibilityGroup = "base";
    private VisibilityFilter visibilityFilter = VisibilityFilter.ALL;
    private boolean dirty = false;

    public void setPlacements(List<StaticPropLoader.StaticPropPlacement> source) {
        placements.clear();
        if (source != null) {
            placements.addAll(source);
        }
        selectedPlacementIndex = -1;
        dirty = false;
    }

    public List<StaticPropLoader.StaticPropPlacement> placements() {
        return Collections.unmodifiableList(placements);
    }

    public void setPlaceableKeys(List<String> keys) {
        placeableKeys.clear();
        if (keys != null) {
            placeableKeys.addAll(keys);
        }
        Collections.sort(placeableKeys);
        if (selectedPlaceableKeyIndex >= placeableKeys.size()) {
            selectedPlaceableKeyIndex = Math.max(0, placeableKeys.size() - 1);
        }
    }

    public String selectedPlaceableKey() {
        if (placeableKeys.isEmpty()) {
            return "";
        }
        return placeableKeys.get(selectedPlaceableKeyIndex);
    }

    public void cyclePlaceableKey(int direction) {
        if (placeableKeys.isEmpty()) {
            selectedPlaceableKeyIndex = 0;
            return;
        }
        selectedPlaceableKeyIndex += direction;
        while (selectedPlaceableKeyIndex < 0) {
            selectedPlaceableKeyIndex += placeableKeys.size();
        }
        selectedPlaceableKeyIndex = selectedPlaceableKeyIndex % placeableKeys.size();
    }

    public float previewRotationYDegrees() {
        return previewRotationYDegrees;
    }

    public float previewScale() {
        return previewScale;
    }

    public void adjustPreviewRotation(float deltaDegrees) {
        previewRotationYDegrees += deltaDegrees;
    }

    public void adjustPreviewScale(float deltaScale) {
        previewScale = Math.max(0.2f, Math.min(4.0f, previewScale + deltaScale));
    }

    public void resetPreviewTransform() {
        previewRotationYDegrees = 0f;
        previewScale = 1f;
    }

    public String previewVisibilityGroup() {
        return previewVisibilityGroup;
    }

    public VisibilityFilter visibilityFilter() {
        return visibilityFilter;
    }

    public void cycleVisibilityFilter() {
        visibilityFilter = switch (visibilityFilter) {
            case ALL -> VisibilityFilter.BASE_ONLY;
            case BASE_ONLY -> VisibilityFilter.ROOF_ONLY;
            case ROOF_ONLY -> VisibilityFilter.ALL;
        };
    }

    public boolean dirty() {
        return dirty;
    }

    public int selectedPlacementIndex() {
        return selectedPlacementIndex;
    }

    public StaticPropLoader.StaticPropPlacement selectedPlacement() {
        if (selectedPlacementIndex < 0 || selectedPlacementIndex >= placements.size()) {
            return null;
        }
        return placements.get(selectedPlacementIndex);
    }

    public int findPlacementIndexOnTile(int tileX, int tileY, String preferredKey) {
        int fallback = -1;
        for (int i = 0; i < placements.size(); i++) {
            StaticPropLoader.StaticPropPlacement p = placements.get(i);
            if (p.x != tileX || p.y != tileY) {
                continue;
            }
            if (preferredKey != null && !preferredKey.isBlank() && preferredKey.equals(p.key)) {
                return i;
            }
            if (fallback < 0) {
                fallback = i;
            }
        }
        return fallback;
    }

    public void selectPlacement(int index) {
        if (index < 0 || index >= placements.size()) {
            selectedPlacementIndex = -1;
        } else {
            selectedPlacementIndex = index;
        }
    }

    public void clearSelection() {
        selectedPlacementIndex = -1;
    }

    public boolean hasSelection() {
        return selectedPlacementIndex >= 0 && selectedPlacementIndex < placements.size();
    }

    public void placeAt(int tileX, int tileY, String visibilityGroup) {
        String key = selectedPlaceableKey();
        if (key.isBlank()) {
            return;
        }
        StaticPropLoader.StaticPropPlacement placement = new StaticPropLoader.StaticPropPlacement(
            key,
            tileX,
            tileY,
            previewRotationYDegrees,
            previewScale,
            visibilityGroup == null || visibilityGroup.isBlank() ? previewVisibilityGroup : normalizeVisibilityGroup(visibilityGroup)
        );
        placements.add(placement);
        selectedPlacementIndex = placements.size() - 1;
        dirty = true;
    }

    public void toggleVisibilityGroupSelectedOrPreview() {
        StaticPropLoader.StaticPropPlacement selected = selectedPlacement();
        if (selected == null) {
            previewVisibilityGroup = toggleVisibility(previewVisibilityGroup);
            return;
        }
        String next = toggleVisibility(selected.visibilityGroup);
        StaticPropLoader.StaticPropPlacement updated = new StaticPropLoader.StaticPropPlacement(
            selected.key,
            selected.x,
            selected.y,
            selected.rotationYDegrees,
            selected.scale,
            next
        );
        placements.set(selectedPlacementIndex, updated);
        dirty = true;
    }

    public boolean duplicateSelectedToPreviewState() {
        StaticPropLoader.StaticPropPlacement selected = selectedPlacement();
        if (selected == null) {
            return false;
        }
        int idx = placeableKeys.indexOf(selected.key);
        if (idx >= 0) {
            selectedPlaceableKeyIndex = idx;
        }
        previewRotationYDegrees = selected.rotationYDegrees;
        previewScale = selected.scale;
        previewVisibilityGroup = normalizeVisibilityGroup(selected.visibilityGroup);
        selectedPlacementIndex = -1;
        return true;
    }

    public int cycleSelectionOnTile(int tileX, int tileY, String preferredKey) {
        ArrayList<Integer> matches = new ArrayList<>();
        for (int i = 0; i < placements.size(); i++) {
            StaticPropLoader.StaticPropPlacement p = placements.get(i);
            if (p.x == tileX && p.y == tileY) {
                matches.add(i);
            }
        }
        if (matches.isEmpty()) {
            selectedPlacementIndex = -1;
            return -1;
        }

        int selectedMatch = -1;
        for (int i = 0; i < matches.size(); i++) {
            if (matches.get(i) == selectedPlacementIndex) {
                selectedMatch = i;
                break;
            }
        }

        int next;
        if (selectedMatch >= 0) {
            next = matches.get((selectedMatch + 1) % matches.size());
        } else {
            next = matches.get(0);
            if (preferredKey != null && !preferredKey.isBlank()) {
                for (int idx : matches) {
                    if (preferredKey.equals(placements.get(idx).key)) {
                        next = idx;
                        break;
                    }
                }
            }
        }

        selectedPlacementIndex = next;
        return next;
    }

    public boolean shouldRenderByVisibilityFilter(StaticPropLoader.StaticPropPlacement placement) {
        if (placement == null) {
            return false;
        }
        String group = normalizeVisibilityGroup(placement.visibilityGroup);
        return switch (visibilityFilter) {
            case ALL -> true;
            case BASE_ONLY -> "base".equals(group);
            case ROOF_ONLY -> "roof".equals(group);
        };
    }

    public void rotateSelectedOrPreview(float deltaDegrees) {
        StaticPropLoader.StaticPropPlacement selected = selectedPlacement();
        if (selected == null) {
            adjustPreviewRotation(deltaDegrees);
            return;
        }
        StaticPropLoader.StaticPropPlacement updated = new StaticPropLoader.StaticPropPlacement(
            selected.key,
            selected.x,
            selected.y,
            selected.rotationYDegrees + deltaDegrees,
            selected.scale,
            selected.visibilityGroup
        );
        placements.set(selectedPlacementIndex, updated);
        dirty = true;
    }

    public void scaleSelectedOrPreview(float deltaScale) {
        StaticPropLoader.StaticPropPlacement selected = selectedPlacement();
        if (selected == null) {
            adjustPreviewScale(deltaScale);
            return;
        }
        float nextScale = Math.max(0.2f, Math.min(4.0f, selected.scale + deltaScale));
        StaticPropLoader.StaticPropPlacement updated = new StaticPropLoader.StaticPropPlacement(
            selected.key,
            selected.x,
            selected.y,
            selected.rotationYDegrees,
            nextScale,
            selected.visibilityGroup
        );
        placements.set(selectedPlacementIndex, updated);
        dirty = true;
    }

    public boolean deleteSelected() {
        if (selectedPlacementIndex < 0 || selectedPlacementIndex >= placements.size()) {
            return false;
        }
        placements.remove(selectedPlacementIndex);
        selectedPlacementIndex = -1;
        dirty = true;
        return true;
    }

    public void markSaved() {
        dirty = false;
    }

    private String toggleVisibility(String group) {
        return "roof".equals(normalizeVisibilityGroup(group)) ? "base" : "roof";
    }

    private String normalizeVisibilityGroup(String group) {
        String normalized = group == null ? "base" : group.trim().toLowerCase();
        if ("roof".equals(normalized)) {
            return "roof";
        }
        return "base";
    }
}
