package com.osrs.client.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.UBJsonReader;
import com.osrs.client.LaunchOptions;
import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModelLibrary {

    private static final String RUNTIME_META_RESOURCE = "model-manifest-runtime.json";
    private static final String COVERAGE_REPORT_RESOURCE = "equipment-coverage-report.json";
    private static final String MODELS_RESOURCE_DIR = "models";
    private static final String SOURCE_MANIFEST = "art/models/manifest.yaml";
    private static final Pattern ENTRY_START_PATTERN = Pattern.compile("^\\s*-\\s+key:\\s*(.+)$");
    private static final Pattern LIST_ITEM_PATTERN = Pattern.compile("^\\s{6}-\\s*(.+)$");
    private static final Pattern FIELD_PATTERN = Pattern.compile("^\\s{4}([a-z_]+):\\s*(.*)$");

    private record LoadedAsset(Model model, Disposable owner) {}

    public record EquipmentPreviewOption(int equipSlot,
                                         int itemId,
                                         String modelKey,
                                         String itemName) {}

    public record ModelMeta(String key,
                            String file,
                            String category,
                            String format,
                            float scale,
                            String origin,
                            boolean required,
                            int equipSlot,
                            int itemId,
                            String attachToState,
                            float offsetX,
                            float offsetY,
                            float offsetZ,
                            float rotX,
                            float rotY,
                            float rotZ,
                            String anchorName,
                            List<String> hideNodes) {}

    private final Map<String, ModelMeta> metaByKey = new HashMap<>();
    private final Map<String, LoadedAsset> loadedAssetByKey = new HashMap<>();
    private final Map<String, String> failedModelSummaryByKey = new LinkedHashMap<>();
    private final Map<Long, ModelMeta> equipmentMetaBySlotItem = new HashMap<>();
    private final Map<Integer, String> knownItemNamesById = new HashMap<>();
    private final boolean repoBacked;
    private final String repoRoot;
    private int declaredModelCount;
    private int loadedModelCount;
    private int failedModelCount;

    private ModelLibrary(boolean repoBacked, String repoRoot) {
        this.repoBacked = repoBacked;
        this.repoRoot = repoRoot == null ? "" : repoRoot;
    }

    public static ModelLibrary load() {
        return load(LaunchOptions.normal());
    }

    public static ModelLibrary load(LaunchOptions launchOptions) {
        LaunchOptions resolved = launchOptions == null ? LaunchOptions.normal() : launchOptions;
        ModelLibrary library = new ModelLibrary(resolved.artistMode(), resolved.repoRootPath().toString());
        if (library.repoBacked) {
            library.loadSourceMetadata();
        } else {
            library.loadRuntimeMetadata();
        }
        library.loadModels();
        library.loadCoverageReport();
        return library;
    }

    public boolean hasModel(String key) {
        return key != null && loadedAssetByKey.containsKey(key);
    }

    public Model getModel(String key) {
        LoadedAsset asset = key == null ? null : loadedAssetByKey.get(key);
        return asset == null ? null : asset.model();
    }

    public ModelMeta getMeta(String key) {
        return key == null ? null : metaByKey.get(key);
    }

    public void dispose() {
        for (LoadedAsset asset : loadedAssetByKey.values()) {
            asset.owner().dispose();
        }
        loadedAssetByKey.clear();
        failedModelSummaryByKey.clear();
        metaByKey.clear();
        equipmentMetaBySlotItem.clear();
        knownItemNamesById.clear();
        declaredModelCount = 0;
        loadedModelCount = 0;
        failedModelCount = 0;
    }

    public int getDeclaredModelCount() {
        return declaredModelCount;
    }

    public int getLoadedModelCount() {
        return loadedModelCount;
    }

    public int getFailedModelCount() {
        return failedModelCount;
    }

    public Map<String, String> getFailedModelSummaries() {
        return Collections.unmodifiableMap(failedModelSummaryByKey);
    }

    public String getFirstFailedModelSummary() {
        if (failedModelSummaryByKey.isEmpty()) {
            return "";
        }
        Map.Entry<String, String> entry = failedModelSummaryByKey.entrySet().iterator().next();
        return entry.getKey() + ": " + entry.getValue();
    }

    public boolean hasEquipmentModel(int equipSlot, int itemId) {
        ModelMeta meta = getEquipmentMeta(equipSlot, itemId);
        return meta != null && loadedAssetByKey.containsKey(meta.key());
    }

    public Model getEquipmentModel(int equipSlot, int itemId) {
        ModelMeta meta = getEquipmentMeta(equipSlot, itemId);
        if (meta == null) {
            return null;
        }
        LoadedAsset asset = loadedAssetByKey.get(meta.key());
        return asset == null ? null : asset.model();
    }

    public ModelMeta getEquipmentMeta(int equipSlot, int itemId) {
        if (equipSlot < 0 || itemId <= 0) {
            return null;
        }
        return equipmentMetaBySlotItem.get(slotItemKey(equipSlot, itemId));
    }

    public boolean hasEquipmentCoverage(int equipSlot, int itemId) {
        return getEquipmentMeta(equipSlot, itemId) != null && getEquipmentModel(equipSlot, itemId) != null;
    }

    public String getKnownItemName(int itemId) {
        if (itemId <= 0) {
            return "";
        }
        return knownItemNamesById.getOrDefault(itemId, "");
    }

    public List<String> getModelKeys() {
        ArrayList<String> keys = new ArrayList<>(loadedAssetByKey.keySet());
        Collections.sort(keys);
        return List.copyOf(keys);
    }

    public Map<Integer, List<EquipmentPreviewOption>> getLoadedEquipmentOptionsBySlot() {
        Map<Integer, ArrayList<EquipmentPreviewOption>> bySlot = new HashMap<>();
        for (Map.Entry<Long, ModelMeta> entry : equipmentMetaBySlotItem.entrySet()) {
            ModelMeta meta = entry.getValue();
            if (meta == null || meta.equipSlot() < 0 || meta.itemId() <= 0) {
                continue;
            }
            if (!loadedAssetByKey.containsKey(meta.key())) {
                continue;
            }
            String itemName = getKnownItemName(meta.itemId());
            EquipmentPreviewOption option = new EquipmentPreviewOption(
                meta.equipSlot(),
                meta.itemId(),
                meta.key(),
                itemName
            );
            bySlot.computeIfAbsent(meta.equipSlot(), ignored -> new ArrayList<>()).add(option);
        }

        Map<Integer, List<EquipmentPreviewOption>> result = new HashMap<>();
        for (Map.Entry<Integer, ArrayList<EquipmentPreviewOption>> entry : bySlot.entrySet()) {
            ArrayList<EquipmentPreviewOption> options = entry.getValue();
            options.sort((a, b) -> {
                String aName = a.itemName() == null ? "" : a.itemName();
                String bName = b.itemName() == null ? "" : b.itemName();
                int cmp = aName.compareToIgnoreCase(bName);
                if (cmp != 0) {
                    return cmp;
                }
                return Integer.compare(a.itemId(), b.itemId());
            });
            result.put(entry.getKey(), List.copyOf(options));
        }
        return result;
    }

    public String equipSlotName(int slot) {
        return switch (slot) {
            case 0 -> "HEAD";
            case 1 -> "CAPE";
            case 2 -> "NECK";
            case 3 -> "AMMO";
            case 4 -> "WEAPON";
            case 5 -> "SHIELD";
            case 6 -> "BODY";
            case 7 -> "LEGS";
            case 8 -> "HANDS";
            case 9 -> "FEET";
            case 10 -> "RING";
            default -> "UNKNOWN";
        };
    }

    private void loadRuntimeMetadata() {
        if (!Gdx.files.internal(RUNTIME_META_RESOURCE).exists()) {
            Gdx.app.log("ModelLibrary", "WARN: runtime metadata missing: " + RUNTIME_META_RESOURCE);
            return;
        }
        try {
            String content = Gdx.files.internal(RUNTIME_META_RESOURCE).readString();
            JsonValue root = new JsonReader().parse(content);
            JsonValue assets = root.get("assets");
            if (assets == null || !assets.isArray()) {
                Gdx.app.log("ModelLibrary", "WARN: no assets array in model metadata");
                return;
            }
            for (JsonValue asset = assets.child; asset != null; asset = asset.next) {
                String key = asset.getString("key", null);
                String file = asset.getString("file", null);
                if (key == null || key.isBlank() || file == null || file.isBlank()) {
                    continue;
                }
                ModelMeta meta = new ModelMeta(
                    key,
                    file,
                    asset.getString("category", ""),
                    asset.getString("format", "g3dj"),
                    asset.getFloat("scale", 1f),
                    asset.getString("origin", "tile-center"),
                    asset.getBoolean("required", false),
                    asset.getInt("equip_slot", -1),
                    asset.getInt("item_id", -1),
                    asset.getString("attach_to_state", ""),
                    asset.getFloat("offset_x", 0f),
                    asset.getFloat("offset_y", 0f),
                    asset.getFloat("offset_z", 0f),
                    asset.getFloat("rot_x", 0f),
                    asset.getFloat("rot_y", 0f),
                    asset.getFloat("rot_z", 0f),
                    asset.getString("anchor_name", ""),
                    readStringList(asset.get("hide_nodes"))
                );
                metaByKey.put(key, meta);
                if ("equipment".equals(meta.category()) && meta.equipSlot() >= 0 && meta.itemId() > 0) {
                    equipmentMetaBySlotItem.put(slotItemKey(meta.equipSlot(), meta.itemId()), meta);
                }
            }
        } catch (Exception e) {
            Gdx.app.log("ModelLibrary", "WARN: failed parsing model metadata: " + e.getMessage());
            metaByKey.clear();
            equipmentMetaBySlotItem.clear();
        }
    }

    private void loadSourceMetadata() {
        FileHandle manifestHandle = resolveSourceManifestHandle();
        if (manifestHandle == null || !manifestHandle.exists()) {
            Gdx.app.log("ModelLibrary", "WARN: source manifest missing: " + SOURCE_MANIFEST);
            return;
        }
        try {
            List<Map<String, Object>> items = parseSourceManifest(manifestHandle.readString());
            for (Map<String, Object> asset : items) {
                String key = text(asset, "key", "");
                String file = text(asset, "file", "");
                if (key.isBlank() || file.isBlank()) {
                    continue;
                }
                ModelMeta meta = new ModelMeta(
                    key,
                    file,
                    text(asset, "category", ""),
                    text(asset, "format", "g3dj"),
                    floatValue(asset, "scale", 1f),
                    text(asset, "origin", "tile-center"),
                    boolValue(asset, "required", false),
                    equipSlotValue(asset.get("equip_slot")),
                    intValue(asset, "item_id", -1),
                    text(asset, "attach_to_state", ""),
                    floatValue(asset, "offset_x", 0f),
                    floatValue(asset, "offset_y", 0f),
                    floatValue(asset, "offset_z", 0f),
                    floatValue(asset, "rot_x", 0f),
                    floatValue(asset, "rot_y", 0f),
                    floatValue(asset, "rot_z", 0f),
                    text(asset, "anchor_name", ""),
                    readStringList(asset.get("hide_nodes"))
                );
                metaByKey.put(meta.key(), meta);
                if ("equipment".equals(meta.category()) && meta.equipSlot() >= 0 && meta.itemId() > 0) {
                    equipmentMetaBySlotItem.put(slotItemKey(meta.equipSlot(), meta.itemId()), meta);
                }
            }
        } catch (Exception e) {
            Gdx.app.log("ModelLibrary", "WARN: failed parsing source model manifest: " + e.getMessage());
            metaByKey.clear();
            equipmentMetaBySlotItem.clear();
        }
    }

    private List<Map<String, Object>> parseSourceManifest(String content) {
        String[] lines = content.split("\\R");
        boolean inAssets = false;
        String activeListField = null;
        ArrayList<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> current = null;

        for (String rawLine : lines) {
            String line = rawLine;
            int comment = line.indexOf('#');
            if (comment >= 0) {
                line = line.substring(0, comment);
            }
            line = rstrip(line);
            if (line.trim().isEmpty()) {
                continue;
            }
            if ("assets:".equals(line.trim())) {
                inAssets = true;
                continue;
            }
            if (!inAssets) {
                continue;
            }

            Matcher entryStart = ENTRY_START_PATTERN.matcher(line);
            if (entryStart.matches()) {
                activeListField = null;
                if (current != null) {
                    items.add(current);
                }
                current = new HashMap<>();
                current.put("key", parseScalar(entryStart.group(1)));
                continue;
            }

            Matcher listItem = LIST_ITEM_PATTERN.matcher(line);
            if (activeListField != null && listItem.matches()) {
                if (current == null) {
                    continue;
                }
                Object existing = current.get(activeListField);
                if (existing instanceof List<?> list) {
                    @SuppressWarnings("unchecked")
                    List<Object> values = (List<Object>) list;
                    values.add(parseScalar(listItem.group(1)));
                }
                continue;
            }

            activeListField = null;
            Matcher pair = FIELD_PATTERN.matcher(line);
            if (pair.matches() && current != null) {
                String fieldName = pair.group(1);
                String fieldValue = pair.group(2);
                if (fieldValue.isEmpty()) {
                    current.put(fieldName, new ArrayList<>());
                    activeListField = fieldName;
                } else {
                    current.put(fieldName, parseScalar(fieldValue));
                }
            }
        }

        if (current != null) {
            items.add(current);
        }
        return items;
    }

    private String rstrip(String text) {
        int end = text.length();
        while (end > 0 && Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(0, end);
    }

    private Object parseScalar(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return "";
        }
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.length() >= 2 ? value.substring(1, value.length() - 1) : "";
        }
        if ("true".equals(value)) {
            return true;
        }
        if ("false".equals(value)) {
            return false;
        }
        if (value.matches("-?\\d+")) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
                return value;
            }
        }
        if (value.matches("-?\\d+\\.\\d+")) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException ignored) {
                return value;
            }
        }
        return value;
    }

    private void loadModels() {
        G3dModelLoader g3djLoader = new G3dModelLoader(new JsonReader());
        G3dModelLoader g3dbLoader = new G3dModelLoader(new UBJsonReader());

        loadedAssetByKey.clear();
        failedModelSummaryByKey.clear();
        declaredModelCount = metaByKey.size();

        for (ModelMeta meta : metaByKey.values()) {
            FileHandle handle = resolveModelHandle(meta.file());
            String resolvedPath = handle.path();
            if (!handle.exists()) {
                String level = meta.required() ? "ERROR" : "WARN";
                String summary = "missing file (format=" + meta.format() + ", path=" + resolvedPath + ")";
                failedModelSummaryByKey.put(meta.key(), summary);
                Gdx.app.log("ModelLibrary", level + ": model load failed for key='" + meta.key()
                    + "' format='" + meta.format() + "' path='" + resolvedPath + "' error='missing file'");
                continue;
            }
            try {
                LoadedAsset loadedAsset;
                String lowerFile = meta.file().toLowerCase();
                if (lowerFile.endsWith(".g3db")) {
                    Model model = g3dbLoader.loadModel(handle);
                    loadedAsset = new LoadedAsset(model, model);
                } else if (lowerFile.endsWith(".glb")) {
                    GLBLoader glbLoader = new GLBLoader();
                    SceneAsset sceneAsset = glbLoader.load(handle);
                    if (sceneAsset == null || sceneAsset.scene == null || sceneAsset.scene.model == null) {
                        throw new IllegalStateException("GLB scene asset missing root model");
                    }
                    normalizeGlbMaterials(sceneAsset.scene.model, meta, resolvedPath);
                    loadedAsset = new LoadedAsset(sceneAsset.scene.model, sceneAsset);
                    Model model = loadedAsset.model();
                    Gdx.app.log("ModelLibrary", "GLB loaded key='" + meta.key()
                        + "' path='" + resolvedPath
                        + "' model_id=" + System.identityHashCode(model)
                        + " meshes=" + model.meshes.size
                        + " materials=" + model.materials.size);
                } else {
                    Model model = g3djLoader.loadModel(handle);
                    loadedAsset = new LoadedAsset(model, model);
                }
                loadedAssetByKey.put(meta.key(), loadedAsset);
            } catch (Exception e) {
                String level = meta.required() ? "ERROR" : "WARN";
                String errorMessage = e.getMessage() == null || e.getMessage().isBlank()
                    ? e.getClass().getSimpleName()
                    : e.getMessage();
                String summary = "load error (format=" + meta.format() + ", path=" + resolvedPath + ", error=" + errorMessage + ")";
                failedModelSummaryByKey.put(meta.key(), summary);
                Gdx.app.log("ModelLibrary", level + ": model load failed for key='" + meta.key()
                    + "' format='" + meta.format() + "' path='" + resolvedPath + "' error='" + errorMessage + "'");
            }
        }

        loadedModelCount = loadedAssetByKey.size();
        failedModelCount = failedModelSummaryByKey.size();
        Gdx.app.log("ModelLibrary", "Model load summary: declared=" + declaredModelCount
            + " loaded=" + loadedModelCount
            + " failed=" + failedModelCount);
    }

    private void normalizeGlbMaterials(Model model, ModelMeta meta, String resolvedPath) {
        if (model == null || model.materials == null) {
            return;
        }

        int normalized = 0;
        for (Material material : model.materials) {
            if (material == null) {
                continue;
            }

            PBRColorAttribute baseColorFactor = (PBRColorAttribute) material.get(PBRColorAttribute.BaseColorFactor);
            TextureAttribute diffuseTexture = (TextureAttribute) material.get(TextureAttribute.Diffuse);
            PBRTextureAttribute baseColorTexture = (PBRTextureAttribute) material.get(PBRTextureAttribute.BaseColorTexture);
            ColorAttribute diffuseColor = (ColorAttribute) material.get(ColorAttribute.Diffuse);

            boolean touched = false;

            if (baseColorTexture != null && diffuseTexture == null && baseColorTexture.textureDescription != null) {
                material.set(new TextureAttribute(TextureAttribute.Diffuse, baseColorTexture.textureDescription));
                touched = true;
            }

            if (baseColorFactor != null) {
                Color base = baseColorFactor.color == null ? Color.WHITE : baseColorFactor.color;
                if (diffuseColor == null) {
                    material.set(ColorAttribute.createDiffuse(base));
                } else {
                    diffuseColor.color.set(base);
                }
                touched = true;
            } else if (diffuseColor == null && diffuseTexture == null && baseColorTexture == null) {
                material.set(ColorAttribute.createDiffuse(Color.LIGHT_GRAY));
                touched = true;
            }

            if (touched) {
                normalized++;
            }
        }

        if (normalized > 0) {
            Gdx.app.log("ModelLibrary", "GLB material compatibility normalized for key='" + meta.key()
                + "' path='" + resolvedPath + "' materials=" + normalized);
        }
    }

    private FileHandle resolveModelHandle(String fileName) {
        if (repoBacked) {
            return Gdx.files.absolute(repoRoot + "/art/models/" + fileName);
        }
        return Gdx.files.internal(MODELS_RESOURCE_DIR + "/" + fileName);
    }

    private FileHandle resolveSourceManifestHandle() {
        if (!repoBacked) {
            return null;
        }
        return Gdx.files.absolute(repoRoot + "/" + SOURCE_MANIFEST);
    }

    private void loadCoverageReport() {
        if (!Gdx.files.internal(COVERAGE_REPORT_RESOURCE).exists()) {
            return;
        }
        try {
            String content = Gdx.files.internal(COVERAGE_REPORT_RESOURCE).readString();
            JsonValue root = new JsonReader().parse(content);
            JsonValue itemNames = root.get("item_names");
            if (itemNames == null || !itemNames.isObject()) {
                return;
            }
            for (JsonValue entry = itemNames.child; entry != null; entry = entry.next) {
                int itemId;
                try {
                    itemId = Integer.parseInt(entry.name);
                } catch (NumberFormatException ignored) {
                    continue;
                }
                String name = entry.asString();
                if (itemId > 0 && name != null && !name.isBlank()) {
                    knownItemNamesById.put(itemId, name);
                }
            }
        } catch (Exception e) {
            Gdx.app.log("ModelLibrary", "WARN: failed parsing coverage report: " + e.getMessage());
            knownItemNamesById.clear();
        }
    }

    private long slotItemKey(int slot, int itemId) {
        return ((long) slot << 32) | (itemId & 0xffffffffL);
    }

    private List<String> readStringList(JsonValue value) {
        if (value == null || !value.isArray()) {
            return List.of();
        }
        java.util.ArrayList<String> strings = new java.util.ArrayList<>();
        for (JsonValue entry = value.child; entry != null; entry = entry.next) {
            String text = entry.asString();
            if (text != null && !text.isBlank()) {
                strings.add(text);
            }
        }
        return List.copyOf(strings);
    }

    private List<String> readStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        java.util.ArrayList<String> strings = new java.util.ArrayList<>();
        for (Object entry : list) {
            if (entry == null) {
                continue;
            }
            String text = String.valueOf(entry).trim();
            if (!text.isEmpty()) {
                strings.add(text);
            }
        }
        return List.copyOf(strings);
    }

    private String text(Map<String, Object> node, String field, String fallback) {
        if (node == null) {
            return fallback;
        }
        Object value = node.get(field);
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value);
        return text == null ? fallback : text;
    }

    private float floatValue(Map<String, Object> node, String field, float fallback) {
        if (node == null) {
            return fallback;
        }
        Object value = node.get(field);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.floatValue();
        }
        try {
            return Float.parseFloat(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private int intValue(Map<String, Object> node, String field, int fallback) {
        if (node == null) {
            return fallback;
        }
        Object value = node.get(field);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private boolean boolValue(Map<String, Object> node, String field, boolean fallback) {
        if (node == null) {
            return fallback;
        }
        Object value = node.get(field);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private int equipSlotValue(Object value) {
        if (value == null) {
            return -1;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String slot = String.valueOf(value).trim().toUpperCase();
        return switch (slot) {
            case "HEAD" -> 0;
            case "CAPE" -> 1;
            case "NECK" -> 2;
            case "AMMO" -> 3;
            case "WEAPON" -> 4;
            case "SHIELD" -> 5;
            case "BODY" -> 6;
            case "LEGS" -> 7;
            case "HANDS" -> 8;
            case "FEET" -> 9;
            case "RING" -> 10;
            default -> -1;
        };
    }
}
