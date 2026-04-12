package com.osrs.client.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.UBJsonReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.osrs.client.LaunchOptions;
import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelLibrary {

    private static final String RUNTIME_META_RESOURCE = "model-manifest-runtime.json";
    private static final String COVERAGE_REPORT_RESOURCE = "equipment-coverage-report.json";
    private static final String MODELS_RESOURCE_DIR = "models";
    private static final String SOURCE_MANIFEST = "art/models/manifest.yaml";

    private record LoadedAsset(Model model, Disposable owner) {}

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
    private final Map<Long, ModelMeta> equipmentMetaBySlotItem = new HashMap<>();
    private final Map<Integer, String> knownItemNamesById = new HashMap<>();
    private final boolean repoBacked;
    private final String repoRoot;

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
        metaByKey.clear();
        equipmentMetaBySlotItem.clear();
        knownItemNamesById.clear();
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
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            JsonNode root = mapper.readTree(manifestHandle.readString());
            JsonNode assets = root.get("assets");
            if (assets == null || !assets.isArray()) {
                Gdx.app.log("ModelLibrary", "WARN: no assets array in source model manifest");
                return;
            }
            for (JsonNode asset : assets) {
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

    private void loadModels() {
        G3dModelLoader g3djLoader = new G3dModelLoader(new JsonReader());
        G3dModelLoader g3dbLoader = new G3dModelLoader(new UBJsonReader());
        GLBLoader glbLoader = new GLBLoader();

        for (ModelMeta meta : metaByKey.values()) {
            FileHandle handle = resolveModelHandle(meta.file());
            if (!handle.exists()) {
                String level = meta.required() ? "ERROR" : "WARN";
                Gdx.app.log("ModelLibrary", level + ": missing model file for key '" + meta.key() + "': " + handle.path());
                continue;
            }
            try {
                LoadedAsset loadedAsset;
                String lowerFile = meta.file().toLowerCase();
                if (lowerFile.endsWith(".g3db")) {
                    Model model = g3dbLoader.loadModel(handle);
                    loadedAsset = new LoadedAsset(model, model);
                } else if (lowerFile.endsWith(".glb")) {
                    SceneAsset sceneAsset = glbLoader.load(handle);
                    if (sceneAsset == null || sceneAsset.scene == null || sceneAsset.scene.model == null) {
                        throw new IllegalStateException("GLB scene asset missing root model");
                    }
                    loadedAsset = new LoadedAsset(sceneAsset.scene.model, sceneAsset);
                } else {
                    Model model = g3djLoader.loadModel(handle);
                    loadedAsset = new LoadedAsset(model, model);
                }
                loadedAssetByKey.put(meta.key(), loadedAsset);
            } catch (Exception e) {
                String level = meta.required() ? "ERROR" : "WARN";
                Gdx.app.log("ModelLibrary", level + ": failed to load model for key '" + meta.key() + "': " + e.getMessage());
            }
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

    private List<String> readStringList(JsonNode value) {
        if (value == null || !value.isArray()) {
            return List.of();
        }
        java.util.ArrayList<String> strings = new java.util.ArrayList<>();
        for (JsonNode entry : value) {
            if (entry != null && entry.isValueNode()) {
                String text = entry.asText("").trim();
                if (!text.isEmpty()) {
                    strings.add(text);
                }
            }
        }
        return List.copyOf(strings);
    }

    private String text(JsonNode node, String field, String fallback) {
        if (node == null) {
            return fallback;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return fallback;
        }
        String text = value.asText(fallback);
        return text == null ? fallback : text;
    }

    private float floatValue(JsonNode node, String field, float fallback) {
        if (node == null) {
            return fallback;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return fallback;
        }
        return (float) value.asDouble(fallback);
    }

    private int intValue(JsonNode node, String field, int fallback) {
        if (node == null) {
            return fallback;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return fallback;
        }
        return value.asInt(fallback);
    }

    private boolean boolValue(JsonNode node, String field, boolean fallback) {
        if (node == null) {
            return fallback;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return fallback;
        }
        return value.asBoolean(fallback);
    }

    private int equipSlotValue(JsonNode value) {
        if (value == null || value.isNull()) {
            return -1;
        }
        if (value.isInt()) {
            return value.asInt(-1);
        }
        String slot = value.asText("").trim().toUpperCase();
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
