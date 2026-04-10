package com.osrs.client.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.UBJsonReader;

import java.util.HashMap;
import java.util.Map;

public class ModelLibrary {

    private static final String RUNTIME_META_RESOURCE = "model-manifest-runtime.json";
    private static final String MODELS_RESOURCE_DIR = "models";

    public record ModelMeta(String key,
                            String file,
                            String category,
                            String format,
                            float scale,
                            String origin,
                            boolean required) {}

    private final Map<String, ModelMeta> metaByKey = new HashMap<>();
    private final Map<String, Model> modelByKey = new HashMap<>();

    public static ModelLibrary load() {
        ModelLibrary library = new ModelLibrary();
        library.loadRuntimeMetadata();
        library.loadModels();
        return library;
    }

    public boolean hasModel(String key) {
        return key != null && modelByKey.containsKey(key);
    }

    public Model getModel(String key) {
        return key == null ? null : modelByKey.get(key);
    }

    public ModelMeta getMeta(String key) {
        return key == null ? null : metaByKey.get(key);
    }

    public void dispose() {
        for (Model model : modelByKey.values()) {
            model.dispose();
        }
        modelByKey.clear();
        metaByKey.clear();
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
                    asset.getBoolean("required", false)
                );
                metaByKey.put(key, meta);
            }
        } catch (Exception e) {
            Gdx.app.log("ModelLibrary", "WARN: failed parsing model metadata: " + e.getMessage());
            metaByKey.clear();
        }
    }

    private void loadModels() {
        G3dModelLoader g3djLoader = new G3dModelLoader(new JsonReader());
        G3dModelLoader g3dbLoader = new G3dModelLoader(new UBJsonReader());

        for (ModelMeta meta : metaByKey.values()) {
            String resourcePath = MODELS_RESOURCE_DIR + "/" + meta.file();
            FileHandle handle = Gdx.files.internal(resourcePath);
            if (!handle.exists()) {
                String level = meta.required() ? "ERROR" : "WARN";
                Gdx.app.log("ModelLibrary", level + ": missing model file for key '" + meta.key() + "': " + resourcePath);
                continue;
            }
            try {
                Model model;
                String lowerFile = meta.file().toLowerCase();
                if (lowerFile.endsWith(".g3db")) {
                    model = g3dbLoader.loadModel(handle);
                } else {
                    model = g3djLoader.loadModel(handle);
                }
                modelByKey.put(meta.key(), model);
            } catch (Exception e) {
                String level = meta.required() ? "ERROR" : "WARN";
                Gdx.app.log("ModelLibrary", level + ": failed to load model for key '" + meta.key() + "': " + e.getMessage());
            }
        }
    }
}
