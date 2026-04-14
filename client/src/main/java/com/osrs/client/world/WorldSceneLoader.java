package com.osrs.client.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.osrs.client.LaunchOptions;

import java.util.List;
import java.util.Map;

public final class WorldSceneLoader {

    private static final String SCENE_RESOURCE_PREFIX = "worlds/";
    private static final String SCENE_SOURCE_PREFIX = "art/worlds/";

    private WorldSceneLoader() {}

    public record WorldSceneData(Map<String, Object> terrainHeight,
                                 Map<String, Object> terrainVisual,
                                 List<Map<String, Object>> staticProps,
                                 boolean repoBacked,
                                 String sourcePath) {}

    public static WorldSceneData load() {
        return load(LaunchOptions.normal());
    }

    public static WorldSceneData load(LaunchOptions launchOptions) {
        LaunchOptions options = launchOptions == null ? LaunchOptions.normal() : launchOptions;
        String worldId = options.worldId();
        boolean repoBacked = options.artistMode();
        String sceneResourcePath = SCENE_RESOURCE_PREFIX + worldId + "/scene.yaml";
        String sceneSourceRelativePath = SCENE_SOURCE_PREFIX + worldId + "/scene.yaml";
        FileHandle sceneHandle = repoBacked
            ? Gdx.files.absolute(options.repoRootPath().resolve(sceneSourceRelativePath).toString())
            : Gdx.files.internal(sceneResourcePath);

        if (!sceneHandle.exists()) {
            String source = repoBacked ? "repo" : "classpath";
            Gdx.app.log("WorldSceneLoader", "WARN: world scene file missing for world '" + worldId + "' in " + source + ": " + sceneHandle.path());
            return new WorldSceneData(Map.of(), Map.of(), List.of(), repoBacked, sceneHandle.path());
        }

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> root = mapper.readValue(sceneHandle.readString(), Map.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> terrainHeight = root.get("terrain_height") instanceof Map<?, ?>
                ? (Map<String, Object>) root.get("terrain_height")
                : Map.of();

            @SuppressWarnings("unchecked")
            Map<String, Object> terrainVisual = root.get("terrain_visual") instanceof Map<?, ?>
                ? (Map<String, Object>) root.get("terrain_visual")
                : Map.of();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> staticProps = root.get("static_props") instanceof List<?>
                ? (List<Map<String, Object>>) root.get("static_props")
                : List.of();

            Gdx.app.log("WorldSceneLoader", "Loaded world scene for world '" + worldId + "' from " + sceneHandle.path());
            return new WorldSceneData(terrainHeight, terrainVisual, staticProps, repoBacked, sceneHandle.path());
        } catch (Exception e) {
            Gdx.app.log("WorldSceneLoader", "WARN: failed to parse scene data from " + sceneHandle.path() + ": " + e.getMessage());
            return new WorldSceneData(Map.of(), Map.of(), List.of(), repoBacked, sceneHandle.path());
        }
    }
}
