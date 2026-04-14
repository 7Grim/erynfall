package com.osrs.client.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.osrs.shared.EquipmentSlot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class Renderer3DExperimental {
    public record PickHit(int entityId, float distance) {}

    private static final int CHUNK_SIZE = 16;
    private static final float GROUND_Y = 0f;
    private static final float WALL_TOP_Y = 1.2f;
    private static final float WALL_FACE_SHADE = 0.55f;
    private static final float WALL_FADE_ALPHA = 0.40f;
    private static final float WALL_FADE_LINE_DISTANCE = 1.15f;
    private static final float WALL_FADE_PLAYER_RADIUS = 6.5f;
    private static final float OVERLAY_Y = 0.018f;
    private static final float WATER_OVERLAY_Y = 0.022f;
    private static final float WALL_OVERLAY_Y = 0.018f;
    private static final int MIN_OVERLAY_RADIUS = 20;
    private static final int MAX_OVERLAY_RADIUS = 30;
    private static final int MAX_VARIANTS_SCAN = 16;
    private static final int[] PLAYER_EQUIPMENT_VISIBLE_SLOTS = {
        EquipmentSlot.HEAD,
        EquipmentSlot.CAPE,
        EquipmentSlot.AMMO,
        EquipmentSlot.WEAPON,
        EquipmentSlot.SHIELD,
        EquipmentSlot.BODY,
        EquipmentSlot.LEGS,
        EquipmentSlot.HANDS,
        EquipmentSlot.FEET,
    };
    private static final float DEFAULT_HEAD_ANCHOR_X = 0f;
    private static final float DEFAULT_HEAD_ANCHOR_Y = 1.18f;
    private static final float DEFAULT_HEAD_ANCHOR_Z = 0f;
    private static final float DEFAULT_CAPE_ANCHOR_X = 0f;
    private static final float DEFAULT_CAPE_ANCHOR_Y = 0.82f;
    private static final float DEFAULT_CAPE_ANCHOR_Z = 0.08f;
    private static final float DEFAULT_AMMO_ANCHOR_X = -0.12f;
    private static final float DEFAULT_AMMO_ANCHOR_Y = 0.92f;
    private static final float DEFAULT_AMMO_ANCHOR_Z = 0.10f;
    private static final float DEFAULT_WEAPON_ANCHOR_X = 0.24f;
    private static final float DEFAULT_WEAPON_ANCHOR_Y = 0.84f;
    private static final float DEFAULT_WEAPON_ANCHOR_Z = -0.02f;
    private static final float DEFAULT_SHIELD_ANCHOR_X = -0.24f;
    private static final float DEFAULT_SHIELD_ANCHOR_Y = 0.84f;
    private static final float DEFAULT_SHIELD_ANCHOR_Z = 0.02f;
    private static final float DEFAULT_BODY_ANCHOR_X = 0f;
    private static final float DEFAULT_BODY_ANCHOR_Y = 0.84f;
    private static final float DEFAULT_BODY_ANCHOR_Z = 0f;
    private static final float DEFAULT_LEGS_ANCHOR_X = 0f;
    private static final float DEFAULT_LEGS_ANCHOR_Y = 0.38f;
    private static final float DEFAULT_LEGS_ANCHOR_Z = 0f;
    private static final float DEFAULT_HANDS_ANCHOR_X = 0f;
    private static final float DEFAULT_HANDS_ANCHOR_Y = 0.82f;
    private static final float DEFAULT_HANDS_ANCHOR_Z = 0f;
    private static final float DEFAULT_FEET_ANCHOR_X = 0f;
    private static final float DEFAULT_FEET_ANCHOR_Y = 0.08f;
    private static final float DEFAULT_FEET_ANCHOR_Z = 0f;
    private static final float DEFAULT_TERRAIN_HEIGHT_STEP = 0.6f;
    private static final int TILE_GRASS = 0;
    private static final int TILE_WATER = 1;
    private static final int TILE_PATH = 2;
    private static final int TILE_WALL = 3;
    private static final int TILE_SAND = 4;
    private static final float DEBUG_AXIS_LENGTH = 0.35f;
    private static final float DEBUG_ANCHOR_CROSS_SIZE = 0.10f;
    private static final String[] DEBUG_PLAYER_ANCHOR_NAMES = {
        "weapon_anchor",
        "shield_anchor",
        "head_anchor",
        "cape_anchor",
        "ammo_anchor",
        "body_anchor",
        "legs_anchor",
        "hands_anchor",
        "feet_anchor"
    };
    private static final float PREVIEW_DEFAULT_YAW_DEGREES = 210f;
    private static final float PREVIEW_DEFAULT_PITCH_DEGREES = 18f;
    private static final float PREVIEW_DEFAULT_DISTANCE = 3.35f;
    private static final float PREVIEW_MIN_DISTANCE = 1.1f;
    private static final float PREVIEW_MAX_DISTANCE = 7.5f;
    private static final float PREVIEW_MIN_PITCH = -15f;
    private static final float PREVIEW_MAX_PITCH = 70f;

    private static final class DebugModelMarker {
        private final Matrix4 transform;
        private final BoundingBox localBounds;

        private DebugModelMarker(Matrix4 transform, BoundingBox localBounds) {
            this.transform = transform;
            this.localBounds = localBounds;
        }
    }

    private final PerspectiveCamera camera;
    private final ModelBatch modelBatch;
    private final ShapeRenderer debugShapeRenderer;
    private final DecalBatch decalBatch;
    private final DecalBatch overlayDecalBatch;
    private final Environment environment;
    private final Environment characterEnvironment;
    private final ModelBuilder modelBuilder = new ModelBuilder();
    private final Plane groundPlane = new Plane(new Vector3(0f, 1f, 0f), 0f);
    private final ArrayList<Decal> decalPool = new ArrayList<>();
    private int decalPoolCursor = 0;
    private final ArrayList<Decal> overlayDecalPool = new ArrayList<>();
    private int overlayDecalPoolCursor = 0;
    private final Texture fallbackBillboardTexture;
    private final TextureRegion fallbackBillboardRegion;
    private final Map<String, Integer> variantCountCache = new HashMap<>();

    private SpriteSheet spriteSheet;
    private float stateTime = 0f;
    private String activeMaterialProfile = "neutral";

    private final Map<Long, ModelInstance> terrainChunks = new HashMap<>();
    private final Map<Long, Model> terrainChunkModels = new HashMap<>();
    private final Map<Long, ArrayList<WallMaterialBinding>> wallMaterialsByChunk = new HashMap<>();
    private final Map<String, ArrayList<ModelInstance>> staticPropInstancePool = new HashMap<>();
    private final Map<String, Integer> staticPropInstanceCursor = new HashMap<>();
    private final Map<String, ArrayList<ModelInstance>> actorModelInstancePool = new HashMap<>();
    private final Map<String, Integer> actorModelInstanceCursor = new HashMap<>();
    private final Map<String, BoundingBox> modelBoundsCache = new HashMap<>();
    private final ArrayList<DebugModelMarker> debugModelMarkers = new ArrayList<>();
    private final ArrayList<Vector3> debugAnchorPoints = new ArrayList<>();
    private final Vector3 debugTmpA = new Vector3();
    private final Vector3 debugTmpB = new Vector3();
    private final Vector3 debugTmpC = new Vector3();
    private boolean debugModelAxesAndBoundsEnabled = false;
    private boolean debugAnchorMarkersEnabled = false;
    private boolean staticPropPassActive = false;
    private boolean actorModelPassActive = false;

    private ModelLibrary modelLibrary;
    private final Map<Integer, ModelInstance> animatedPlayerInstances = new HashMap<>();
    private final Map<Integer, AnimationController> playerAnimationControllers = new HashMap<>();
    private final Map<Integer, String> currentPlayerClipByEntityId = new HashMap<>();
    private final Map<Integer, Float> playerAnimTimes = new HashMap<>();
    private final Map<Integer, ModelInstance> npcAnimatedInstances = new HashMap<>();
    private final Map<Integer, AnimationController> npcAnimationControllers = new HashMap<>();
    private final Map<Integer, String> npcBaseKeyByEntityId = new HashMap<>();
    private final Map<Integer, String> currentNpcClipByEntityId = new HashMap<>();
    private final Map<Integer, Float> npcAnimTimes = new HashMap<>();
    private final Map<String, ModelInstance> previewInstanceByKey = new HashMap<>();
    private final Map<String, AnimationController> previewAnimationControllerByKey = new HashMap<>();
    private final Map<String, String> previewClipByKey = new HashMap<>();
    private String previewLoggedModelKey = "";
    private int previewLoggedModelIdentity = 0;
    private ModelInstance previewEquipmentFitPlayerInstance;
    private AnimationController previewEquipmentFitPlayerController;
    private String previewEquipmentFitPlayerClip = "";
    private float previewCameraYawDegrees = PREVIEW_DEFAULT_YAW_DEGREES;
    private float previewCameraPitchDegrees = PREVIEW_DEFAULT_PITCH_DEGREES;
    private float previewCameraDistance = PREVIEW_DEFAULT_DISTANCE;
    private final Set<Long> missingEquipmentWarnings = new HashSet<>();
    private final Vector3 frustumQueryPoint = new Vector3();
    private int terrainChunksRenderedLastFrame = 0;
    private int overlayTilesProcessedLastFrame = 0;
    private int staticPropsRenderedLastFrame = 0;
    private int actorModelsRenderedLastFrame = 0;
    private int entityBillboardsRenderedLastFrame = 0;
    private int[][] terrainHeightLevels;
    private float terrainHeightStep = DEFAULT_TERRAIN_HEIGHT_STEP;
    private int[][] lastTileMap;
    private int mapWidth = 0;
    private int mapHeight = 0;

    private static final class WallMaterialBinding {
        private final Material material;
        private final int tileX;
        private final int tileY;
        private final float shade;

        private WallMaterialBinding(Material material, int tileX, int tileY, float shade) {
            this.material = material;
            this.tileX = tileX;
            this.tileY = tileY;
            this.shade = shade;
        }
    }

    public Renderer3DExperimental(int screenW, int screenH) {
        camera = new PerspectiveCamera(45f, Math.max(1, screenW), Math.max(1, screenH));
        camera.near = 0.1f;
        camera.far = 600f;
        camera.position.set(52f, 26f, 74f);
        camera.lookAt(52f, 0f, 52f);
        camera.up.set(0f, 1f, 0f);
        camera.update();

        modelBatch = new ModelBatch();
        debugShapeRenderer = new ShapeRenderer();
        decalBatch = new DecalBatch(new CameraGroupStrategy(camera));
        overlayDecalBatch = new DecalBatch(new CameraGroupStrategy(camera));

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.50f, 0.48f, 0.42f, 1f));
        environment.add(new DirectionalLight().set(0.45f, 0.42f, 0.36f, -0.5f, -0.85f, -0.2f));

        // Character models use pre-baked vertex colors — full ambient, no directional light.
        characterEnvironment = new Environment();
        characterEnvironment.set(new ColorAttribute(ColorAttribute.AmbientLight, 1f, 1f, 1f, 1f));

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        fallbackBillboardTexture = new Texture(pixmap);
        pixmap.dispose();
        fallbackBillboardRegion = new TextureRegion(fallbackBillboardTexture);
    }

    public PerspectiveCamera getCamera() {
        return camera;
    }

    public void setSpriteSheet(SpriteSheet sheet) {
        this.spriteSheet = sheet;
        this.variantCountCache.clear();
    }

    public void setModelLibrary(ModelLibrary library) {
        this.modelLibrary = library;
        staticPropInstancePool.clear();
        staticPropInstanceCursor.clear();
        actorModelInstancePool.clear();
        actorModelInstanceCursor.clear();
        animatedPlayerInstances.clear();
        playerAnimationControllers.clear();
        currentPlayerClipByEntityId.clear();
        playerAnimTimes.clear();
        npcAnimatedInstances.clear();
        npcAnimationControllers.clear();
        npcBaseKeyByEntityId.clear();
        currentNpcClipByEntityId.clear();
        npcAnimTimes.clear();
        previewInstanceByKey.clear();
        previewAnimationControllerByKey.clear();
        previewClipByKey.clear();
        previewEquipmentFitPlayerInstance = null;
        previewEquipmentFitPlayerController = null;
        previewEquipmentFitPlayerClip = "";
        missingEquipmentWarnings.clear();
        modelBoundsCache.clear();
        debugModelMarkers.clear();
        debugAnchorPoints.clear();
    }

    public void setArtistDebugVisualization(boolean showModelAxesAndBounds, boolean showAnchorMarkers) {
        this.debugModelAxesAndBoundsEnabled = showModelAxesAndBounds;
        this.debugAnchorMarkersEnabled = showAnchorMarkers;
    }

    public void orbitWorkbenchPreviewCamera(float yawDeltaDegrees, float pitchDeltaDegrees) {
        previewCameraYawDegrees = (previewCameraYawDegrees + yawDeltaDegrees) % 360f;
        if (previewCameraYawDegrees < 0f) {
            previewCameraYawDegrees += 360f;
        }
        previewCameraPitchDegrees = Math.max(PREVIEW_MIN_PITCH, Math.min(PREVIEW_MAX_PITCH,
            previewCameraPitchDegrees + pitchDeltaDegrees));
    }

    public void zoomWorkbenchPreviewCamera(float distanceDelta) {
        previewCameraDistance = Math.max(PREVIEW_MIN_DISTANCE, Math.min(PREVIEW_MAX_DISTANCE,
            previewCameraDistance + distanceDelta));
    }

    public void resetWorkbenchPreviewCamera() {
        previewCameraYawDegrees = PREVIEW_DEFAULT_YAW_DEGREES;
        previewCameraPitchDegrees = PREVIEW_DEFAULT_PITCH_DEGREES;
        previewCameraDistance = PREVIEW_DEFAULT_DISTANCE;
    }

    public boolean hasStaticPropModel(String key) {
        return modelLibrary != null && modelLibrary.hasModel(key);
    }

    public void update(float delta) {
        stateTime += delta;
    }

    public void setTerrainHeightData(int[][] levels, float step) {
        this.terrainHeightLevels = levels;
        if (step > 0f) {
            this.terrainHeightStep = step;
        } else {
            this.terrainHeightStep = DEFAULT_TERRAIN_HEIGHT_STEP;
        }
    }

    public void rebuildTerrain(int[][] tileMap) {
        rebuildTerrain(tileMap, activeMaterialProfile);
    }

    public void rebuildTerrain(int[][] tileMap, String materialProfile) {
        activeMaterialProfile = normalizeMaterialProfile(materialProfile);
        for (Model model : terrainChunkModels.values()) {
            model.dispose();
        }
        terrainChunkModels.clear();
        terrainChunks.clear();
        wallMaterialsByChunk.clear();

        if (tileMap == null || spriteSheet == null) {
            mapWidth = 0;
            mapHeight = 0;
            return;
        }

        updateMapBounds(tileMap);
        if (mapWidth <= 0 || mapHeight <= 0) {
            return;
        }

        int chunksX = (mapWidth + CHUNK_SIZE - 1) / CHUNK_SIZE;
        int chunksY = (mapHeight + CHUNK_SIZE - 1) / CHUNK_SIZE;
        for (int chunkY = 0; chunkY < chunksY; chunkY++) {
            for (int chunkX = 0; chunkX < chunksX; chunkX++) {
                ArrayList<WallMaterialBinding> wallBindings = new ArrayList<>();
                Model model = buildChunkModel(tileMap, chunkX, chunkY, activeMaterialProfile, wallBindings);
                if (model == null) {
                    continue;
                }
                long key = chunkKey(chunkX, chunkY);
                terrainChunkModels.put(key, model);
                terrainChunks.put(key, new ModelInstance(model));
                wallMaterialsByChunk.put(key, wallBindings);
            }
        }
    }

    public void renderTerrain(int[][] tileMap, float centerX, float centerY) {
        renderTerrain(tileMap, centerX, centerY, activeMaterialProfile);
    }

    public void renderTerrain(int[][] tileMap,
                              float centerX,
                              float centerY,
                              String materialProfile) {
        terrainChunksRenderedLastFrame = 0;
        overlayTilesProcessedLastFrame = 0;
        staticPropsRenderedLastFrame = 0;
        actorModelsRenderedLastFrame = 0;
        entityBillboardsRenderedLastFrame = 0;
        resetDebugCapture();

        String normalizedProfile = normalizeMaterialProfile(materialProfile);
        lastTileMap = tileMap;
        updateMapBounds(tileMap);
        if (mapWidth <= 0 || mapHeight <= 0) {
            return;
        }
        if (!normalizedProfile.equals(activeMaterialProfile)) {
            rebuildTerrain(tileMap, normalizedProfile);
        } else if (terrainChunks.isEmpty() && tileMap != null) {
            rebuildTerrain(tileMap, normalizedProfile);
        }
        if (terrainChunks.isEmpty()) {
            return;
        }

        updateWallOcclusion(centerX, centerY);

        int centerTileX = clampTileX((int) Math.floor(centerX));
        int centerTileY = clampTileY((int) Math.floor(centerY));
        int overlayRadius = computeOverlayRadius();
        int minTileX = Math.max(0, centerTileX - overlayRadius);
        int maxTileX = Math.min(mapWidth - 1, centerTileX + overlayRadius);
        int minTileY = Math.max(0, centerTileY - overlayRadius);
        int maxTileY = Math.min(mapHeight - 1, centerTileY + overlayRadius);

        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);

        modelBatch.begin(camera);
        int minChunkX = minTileX / CHUNK_SIZE;
        int maxChunkX = maxTileX / CHUNK_SIZE;
        int minChunkY = minTileY / CHUNK_SIZE;
        int maxChunkY = maxTileY / CHUNK_SIZE;
        for (int chunkY = minChunkY; chunkY <= maxChunkY; chunkY++) {
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                ModelInstance instance = terrainChunks.get(chunkKey(chunkX, chunkY));
                if (instance != null) {
                    modelBatch.render(instance, environment);
                    terrainChunksRenderedLastFrame++;
                }
            }
        }
        modelBatch.end();

        renderGroundOverlayPass(tileMap, minTileX, maxTileX, minTileY, maxTileY, centerX, centerY, normalizedProfile);

        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
    }

    public void renderEntityBillboard(float tileX,
                                      float tileY,
                                      TextureRegion region,
                                      float width,
                                      float height,
                                      float alpha) {
        renderEntityBillboard(tileX, tileY, 0f, region, width, height, 1f, 1f, 1f, alpha);
    }

    public void renderEntityBillboardAtHeight(float tileX,
                                              float tileY,
                                              float baseY,
                                              TextureRegion region,
                                              float width,
                                              float height,
                                              float alpha) {
        renderEntityBillboard(tileX, tileY, baseY, region, width, height, 1f, 1f, 1f, alpha);
    }

    public void renderEntityBillboard(float tileX,
                                      float tileY,
                                      TextureRegion region,
                                      float width,
                                      float height,
                                      float tintR,
                                      float tintG,
                                      float tintB,
                                      float alpha) {
        renderEntityBillboard(tileX, tileY, 0f, region, width, height, tintR, tintG, tintB, alpha);
    }

    public void renderEntityBillboard(float tileX,
                                      float tileY,
                                      float baseY,
                                      TextureRegion region,
                                      float width,
                                      float height,
                                      float tintR,
                                      float tintG,
                                      float tintB,
                                      float alpha) {
        TextureRegion effectiveRegion = region != null ? region : fallbackBillboardRegion;
        if (effectiveRegion == null) {
            return;
        }
        Decal decal = obtainDecal(effectiveRegion, width, height);
        float tileBaseY = getTileTopY(tileX, tileY);
        decal.setPosition(tileX + 0.5f, tileBaseY + baseY + height * 0.5f, tileY + 0.5f);
        decal.lookAt(camera.position, camera.up);
        decal.setColor(
            Math.max(0f, Math.min(1f, tintR)),
            Math.max(0f, Math.min(1f, tintG)),
            Math.max(0f, Math.min(1f, tintB)),
            Math.max(0f, Math.min(1f, alpha))
        );
        decalBatch.add(decal);
        entityBillboardsRenderedLastFrame++;
    }

    public void beginEntityPass() {
        decalPoolCursor = 0;
    }

    public void endEntityPass() {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        decalBatch.flush();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
    }

    public void flushEntityBillboards() {
        endEntityPass();
    }

    public boolean renderWorkbenchModelPreview(String modelKey, String requestedClip, float delta) {
        resetDebugCapture();
        if (modelLibrary == null || modelKey == null || modelKey.isBlank() || !modelLibrary.hasModel(modelKey)) {
            return false;
        }

        Model model = modelLibrary.getModel(modelKey);
        ModelLibrary.ModelMeta meta = modelLibrary.getMeta(modelKey);
        if (model == null || meta == null) {
            return false;
        }

        int modelIdentity = System.identityHashCode(model);
        if (!modelKey.equals(previewLoggedModelKey) || modelIdentity != previewLoggedModelIdentity) {
            Gdx.app.log("Renderer3DExperimental", "Workbench preview key='" + modelKey
                + "' model_id=" + modelIdentity
                + " meshes=" + model.meshes.size
                + " materials=" + model.materials.size);
            previewLoggedModelKey = modelKey;
            previewLoggedModelIdentity = modelIdentity;
        }

        ModelInstance instance = previewInstanceByKey.get(modelKey);
        if (instance == null) {
            instance = new ModelInstance(model);
            for (Material m : instance.materials) {
                m.set(IntAttribute.createCullFace(GL20.GL_BACK));
            }
            previewInstanceByKey.put(modelKey, instance);
        }

        if (instance.model != model) {
            instance = new ModelInstance(model);
            for (Material m : instance.materials) {
                m.set(IntAttribute.createCullFace(GL20.GL_BACK));
            }
            previewInstanceByKey.put(modelKey, instance);
            previewAnimationControllerByKey.remove(modelKey);
            previewClipByKey.remove(modelKey);
        }

        applyPreviewCamera();
        drawPreviewGroundMarkers();

        String clipToPlay = resolvePreviewClip(instance, requestedClip);
        if (!clipToPlay.isBlank()) {
            AnimationController controller = previewAnimationControllerByKey.get(modelKey);
            if (controller == null) {
                controller = new AnimationController(instance);
                previewAnimationControllerByKey.put(modelKey, controller);
            }
            String current = previewClipByKey.getOrDefault(modelKey, "");
            if (!clipToPlay.equals(current)) {
                controller.setAnimation(clipToPlay, -1);
                previewClipByKey.put(modelKey, clipToPlay);
            }
            controller.update(Math.max(0f, delta));
        }

        applyManifestModelTransform(instance, meta, -0.5f, -0.5f, 0f, 0f, 1f);
        instance.calculateTransforms();

        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(true);
        modelBatch.begin(camera);
        modelBatch.render(instance, characterEnvironment);
        modelBatch.end();
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);

        captureDebugModelMarker(modelKey, model, instance);
        capturePlayerAnchorMarkers(instance);
        return true;
    }

    public boolean renderWorkbenchEquipmentFitPreview(String requestedClip,
                                                      int[] equippedItemIds,
                                                      Map<Integer, float[]> previewOverridesBySlot,
                                                      float delta) {
        resetDebugCapture();
        if (modelLibrary == null || !modelLibrary.hasModel("player_base")) {
            return false;
        }

        Model baseModel = modelLibrary.getModel("player_base");
        ModelLibrary.ModelMeta baseMeta = modelLibrary.getMeta("player_base");
        if (baseModel == null || baseMeta == null) {
            return false;
        }

        if (previewEquipmentFitPlayerInstance == null || previewEquipmentFitPlayerInstance.model != baseModel) {
            previewEquipmentFitPlayerInstance = new ModelInstance(baseModel);
            for (Material m : previewEquipmentFitPlayerInstance.materials) {
                m.set(IntAttribute.createCullFace(GL20.GL_BACK));
            }
            previewEquipmentFitPlayerController = new AnimationController(previewEquipmentFitPlayerInstance);
            previewEquipmentFitPlayerClip = "";
        }

        applyPreviewCamera();
        drawPreviewGroundMarkers();

        String clipToPlay = resolvePreviewClip(previewEquipmentFitPlayerInstance, requestedClip);
        if (!clipToPlay.isBlank() && previewEquipmentFitPlayerController != null) {
            if (!clipToPlay.equals(previewEquipmentFitPlayerClip)) {
                previewEquipmentFitPlayerController.setAnimation(clipToPlay, -1);
                previewEquipmentFitPlayerClip = clipToPlay;
            }
            previewEquipmentFitPlayerController.update(Math.max(0f, delta));
        }

        applyManifestModelTransform(previewEquipmentFitPlayerInstance, baseMeta, -0.5f, -0.5f, 0f, 0f, 1f);
        previewEquipmentFitPlayerInstance.calculateTransforms();
        float baseScale = baseMeta.scale() > 0f ? baseMeta.scale() : 1f;

        java.util.Map<Node, Boolean> previousNodeVisibility = applyHiddenBaseNodes(previewEquipmentFitPlayerInstance, equippedItemIds);
        boolean batchOpen = false;
        try {
            Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
            Gdx.gl.glDepthMask(true);
            modelBatch.begin(camera);
            batchOpen = true;
            modelBatch.render(previewEquipmentFitPlayerInstance, characterEnvironment);
            actorModelsRenderedLastFrame++;
            captureDebugModelMarker("player_base", baseModel, previewEquipmentFitPlayerInstance);
            capturePlayerAnchorMarkers(previewEquipmentFitPlayerInstance);
            renderPlayerEquipmentAttachments(previewEquipmentFitPlayerInstance,
                -0.5f,
                -0.5f,
                0f,
                0f,
                baseScale,
                equippedItemIds,
                previewOverridesBySlot);
            modelBatch.end();
            batchOpen = false;
            Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
            return true;
        } finally {
            if (batchOpen) {
                modelBatch.end();
            }
            Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
            restoreHiddenBaseNodes(previousNodeVisibility);
        }
    }

    private void resetDebugCapture() {
        debugModelMarkers.clear();
        debugAnchorPoints.clear();
    }

    public void renderArtistDebugOverlays() {
        if (!debugModelAxesAndBoundsEnabled && !debugAnchorMarkersEnabled) {
            return;
        }

        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(false);
        debugShapeRenderer.setProjectionMatrix(camera.combined);
        debugShapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        if (debugModelAxesAndBoundsEnabled) {
            for (DebugModelMarker marker : debugModelMarkers) {
                drawAxes(marker.transform);
                drawBounds(marker.transform, marker.localBounds);
            }
        }

        if (debugAnchorMarkersEnabled) {
            debugShapeRenderer.setColor(1f, 1f, 0.2f, 1f);
            for (Vector3 point : debugAnchorPoints) {
                drawCross(point, DEBUG_ANCHOR_CROSS_SIZE);
            }
        }

        debugShapeRenderer.end();
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
    }

    public void beginStaticPropPass() {
        if (staticPropPassActive) {
            return;
        }
        staticPropPassActive = true;
        for (String key : staticPropInstanceCursor.keySet()) {
            staticPropInstanceCursor.put(key, 0);
        }

        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);
        modelBatch.begin(camera);
    }

    public void endStaticPropPass() {
        if (!staticPropPassActive) {
            return;
        }
        modelBatch.end();
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        staticPropPassActive = false;
    }

    public void beginActorModelPass() {
        if (actorModelPassActive) {
            return;
        }
        actorModelPassActive = true;
        for (String key : actorModelInstanceCursor.keySet()) {
            actorModelInstanceCursor.put(key, 0);
        }

        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);
        modelBatch.begin(camera);
    }

    public void endActorModelPass() {
        if (!actorModelPassActive) {
            return;
        }
        modelBatch.end();
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        actorModelPassActive = false;
    }

    public boolean renderStaticPropModel(String key, float tileX, float tileY, float alpha) {
        return renderPlacedStaticPropModel(key, tileX, tileY, 0f, -1f, alpha);
    }

    public boolean renderPlacedStaticPropModel(String key,
                                               float tileX,
                                               float tileY,
                                               float rotationYDegrees,
                                               float scaleOverride) {
        return renderPlacedStaticPropModel(key, tileX, tileY, rotationYDegrees, scaleOverride, 1f);
    }

    public boolean renderPlacedStaticPropModel(String key,
                                               float tileX,
                                               float tileY,
                                               float rotationYDegrees,
                                               float scaleOverride,
                                               float alpha) {
        if (key == null || key.isBlank() || modelLibrary == null || !modelLibrary.hasModel(key)) {
            return false;
        }

        Model model = modelLibrary.getModel(key);
        ModelLibrary.ModelMeta meta = modelLibrary.getMeta(key);
        if (model == null || meta == null) {
            return false;
        }

        boolean ownsPass = !staticPropPassActive;
        if (ownsPass) {
            beginStaticPropPass();
        }

        ModelInstance instance = obtainStaticPropInstance(key, model);
        float tileBaseY = getTileTopY(tileX, tileY);
        float placementScale = scaleOverride > 0f ? scaleOverride : 1f;
        applyManifestModelTransform(instance, meta, tileX, tileY, tileBaseY, rotationYDegrees, placementScale);

        float clampedAlpha = Math.max(0f, Math.min(1f, alpha));
        float[] previousDiffuseAlpha = null;
        float[] previousBlendAlpha = null;
        int[] previousBlendSource = null;
        int[] previousBlendDest = null;
        boolean[] hadBlend = null;
        if (clampedAlpha < 0.999f) {
            int materialCount = instance.materials.size;
            previousDiffuseAlpha = new float[materialCount];
            previousBlendAlpha = new float[materialCount];
            previousBlendSource = new int[materialCount];
            previousBlendDest = new int[materialCount];
            hadBlend = new boolean[materialCount];
            for (int i = 0; i < materialCount; i++) {
                Material material = instance.materials.get(i);
                ColorAttribute diffuse = (ColorAttribute) material.get(ColorAttribute.Diffuse);
                previousDiffuseAlpha[i] = diffuse != null ? diffuse.color.a : 1f;

                BlendingAttribute blend = (BlendingAttribute) material.get(BlendingAttribute.Type);
                if (blend != null) {
                    hadBlend[i] = true;
                    previousBlendAlpha[i] = blend.opacity;
                    previousBlendSource[i] = blend.sourceFunction;
                    previousBlendDest[i] = blend.destFunction;
                }

                if (diffuse != null) {
                    diffuse.color.a = clampedAlpha;
                }
                material.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, clampedAlpha));
            }
        }

        if (clampedAlpha < 0.999f) {
            Gdx.gl.glDepthMask(false);
        }
        modelBatch.render(instance, environment);
        staticPropsRenderedLastFrame++;
        if (clampedAlpha < 0.999f) {
            Gdx.gl.glDepthMask(true);
        }

        captureDebugModelMarker(key, model, instance);

        if (clampedAlpha < 0.999f) {
            for (int i = 0; i < instance.materials.size; i++) {
                Material material = instance.materials.get(i);
                ColorAttribute diffuse = (ColorAttribute) material.get(ColorAttribute.Diffuse);
                if (diffuse != null && previousDiffuseAlpha != null) {
                    diffuse.color.a = previousDiffuseAlpha[i];
                }
                if (hadBlend != null && hadBlend[i] && previousBlendAlpha != null
                    && previousBlendSource != null && previousBlendDest != null) {
                    material.set(new BlendingAttribute(previousBlendSource[i], previousBlendDest[i], previousBlendAlpha[i]));
                } else {
                    material.remove(BlendingAttribute.Type);
                }
            }
        }

        if (ownsPass) {
            endStaticPropPass();
        }
        return true;
    }

    public boolean renderActorModel(String key,
                                    float tileX,
                                    float tileY,
                                    float rotationYDegrees,
                                    float alpha) {
        if (key == null || key.isBlank() || modelLibrary == null || !modelLibrary.hasModel(key)) {
            return false;
        }

        Model model = modelLibrary.getModel(key);
        ModelLibrary.ModelMeta meta = modelLibrary.getMeta(key);
        if (model == null || meta == null) {
            return false;
        }

        boolean ownsPass = !actorModelPassActive;
        if (ownsPass) {
            beginActorModelPass();
        }

        ModelInstance instance = obtainActorModelInstance(key, model);
        float tileBaseY = getTileTopY(tileX, tileY);
        applyManifestModelTransform(instance, meta, tileX, tileY, tileBaseY, rotationYDegrees, 1f);

        modelBatch.render(instance, characterEnvironment);
        actorModelsRenderedLastFrame++;
        captureDebugModelMarker(key, model, instance);

        if (ownsPass) {
            endActorModelPass();
        }
        return true;
    }

    public boolean renderPlayerModelComposed(String baseKey,
                                             float tileX,
                                             float tileY,
                                             float rotationYDegrees,
                                             int[] equippedItemIds) {
        if (baseKey == null || baseKey.isBlank() || modelLibrary == null || !modelLibrary.hasModel(baseKey)) {
            return false;
        }

        Model baseModel = modelLibrary.getModel(baseKey);
        ModelLibrary.ModelMeta baseMeta = modelLibrary.getMeta(baseKey);
        if (baseModel == null || baseMeta == null) {
            return false;
        }

        boolean ownsPass = !actorModelPassActive;
        if (ownsPass) {
            beginActorModelPass();
        }

        float baseScale = baseMeta.scale() > 0f ? baseMeta.scale() : 1f;

        ModelInstance baseInstance = obtainActorModelInstance(baseKey, baseModel);
        float tileBaseY = getTileTopY(tileX, tileY);
        applyManifestModelTransform(baseInstance, baseMeta, tileX, tileY, tileBaseY, rotationYDegrees, 1f);
        baseInstance.calculateTransforms();
        java.util.Map<Node, Boolean> previousNodeVisibility = applyHiddenBaseNodes(baseInstance, equippedItemIds);
        try {
            modelBatch.render(baseInstance, characterEnvironment);
            actorModelsRenderedLastFrame++;
            captureDebugModelMarker(baseKey, baseModel, baseInstance);
            capturePlayerAnchorMarkers(baseInstance);

            renderPlayerEquipmentAttachments(baseInstance, tileX, tileY, tileBaseY, rotationYDegrees, baseScale, equippedItemIds, null);
        } finally {
            restoreHiddenBaseNodes(previousNodeVisibility);
        }

        if (ownsPass) {
            endActorModelPass();
        }
        return true;
    }

    public boolean renderAnimatedPlayer(String stateKey,
                                        int entityId,
                                        float tileX,
                                        float tileY,
                                        float rotationYDegrees,
                                        int[] equippedItemIds,
                                        float delta) {
        if (!ensureAnimatedPlayerModelLoaded(entityId)) {
            return false;
        }

        ModelInstance animatedInstance = animatedPlayerInstances.get(entityId);
        AnimationController animationController = playerAnimationControllers.get(entityId);
        if (animatedInstance == null || animationController == null) {
            return false;
        }

        String clipName = normalizePlayerClipName(stateKey);
        boolean hasClips = animatedInstance.animations != null
                           && !animatedInstance.animations.isEmpty();
        if (hasClips && animatedInstance.getAnimation(clipName) == null) {
            return false;
        }

        boolean ownsPass = !actorModelPassActive;
        if (ownsPass) {
            beginActorModelPass();
        }

        ModelLibrary.ModelMeta baseMeta = modelLibrary.getMeta("player_base");
        float baseScale = baseMeta != null && baseMeta.scale() > 0f ? baseMeta.scale() : 1f;
        try {
            if (hasClips) {
                String currentClip = currentPlayerClipByEntityId.getOrDefault(entityId, "");
                if (!clipName.equals(currentClip)) {
                    animationController.setAnimation(clipName, -1);
                    currentPlayerClipByEntityId.put(entityId, clipName);
                }
                animationController.update(Math.max(0f, delta));
            } else {
                float animTime = playerAnimTimes.getOrDefault(entityId, 0f) + Math.max(0f, delta);
                playerAnimTimes.put(entityId, animTime);
                applyCharacterAnimation(animatedInstance, clipName, animTime);
            }

            animatedInstance.transform.idt();
            float tileBaseY = getTileTopY(tileX, tileY);
            applyManifestModelTransform(animatedInstance, baseMeta, tileX, tileY, tileBaseY, rotationYDegrees, 1f);
            animatedInstance.calculateTransforms();
            java.util.Map<Node, Boolean> previousNodeVisibility = applyHiddenBaseNodes(animatedInstance, equippedItemIds);
            try {
            modelBatch.render(animatedInstance, characterEnvironment);
            actorModelsRenderedLastFrame++;
                captureDebugModelMarker("player_base", animatedInstance.model, animatedInstance);
                capturePlayerAnchorMarkers(animatedInstance);

                renderPlayerEquipmentAttachments(animatedInstance, tileX, tileY, tileBaseY, rotationYDegrees, baseScale, equippedItemIds, null);
            } finally {
                restoreHiddenBaseNodes(previousNodeVisibility);
            }
        } catch (Exception e) {
            if (ownsPass) {
                endActorModelPass();
            }
            return false;
        }

        if (ownsPass) {
            endActorModelPass();
        }
        return true;
    }

    public void retainAnimatedPlayerEntities(Set<Integer> activePlayerEntityIds) {
        if (activePlayerEntityIds == null) {
            animatedPlayerInstances.clear();
            playerAnimationControllers.clear();
            currentPlayerClipByEntityId.clear();
            playerAnimTimes.clear();
            return;
        }
        animatedPlayerInstances.keySet().retainAll(activePlayerEntityIds);
        playerAnimationControllers.keySet().retainAll(activePlayerEntityIds);
        currentPlayerClipByEntityId.keySet().retainAll(activePlayerEntityIds);
        playerAnimTimes.keySet().retainAll(activePlayerEntityIds);
    }

    public boolean renderAnimatedNpc(String baseKey,
                                     int entityId,
                                     String stateKey,
                                     float tileX,
                                     float tileY,
                                     float rotationYDegrees,
                                     float alpha,
                                     float delta) {
        if (!ensureAnimatedNpcModelLoaded(entityId, baseKey)) {
            return false;
        }

        ModelInstance instance = npcAnimatedInstances.get(entityId);
        AnimationController controller = npcAnimationControllers.get(entityId);
        if (instance == null || controller == null) {
            return false;
        }

        String clipName = normalizeNpcClipName(stateKey);
        boolean hasClips = instance.animations != null && !instance.animations.isEmpty();
        if (hasClips && instance.getAnimation(clipName) == null) {
            return false;
        }

        boolean ownsPass = !actorModelPassActive;
        if (ownsPass) {
            beginActorModelPass();
        }

        ModelLibrary.ModelMeta baseMeta = modelLibrary.getMeta(baseKey);
        float baseScale = baseMeta != null && baseMeta.scale() > 0f ? baseMeta.scale() : 1f;
        try {
            if (hasClips) {
                String currentClip = currentNpcClipByEntityId.getOrDefault(entityId, "");
                if (!clipName.equals(currentClip)) {
                    controller.setAnimation(clipName, -1);
                    currentNpcClipByEntityId.put(entityId, clipName);
                }
                controller.update(Math.max(0f, delta));
            } else {
                float npcTime = npcAnimTimes.getOrDefault(entityId, 0f) + Math.max(0f, delta);
                npcAnimTimes.put(entityId, npcTime);
                applyCharacterAnimation(instance, clipName, npcTime);
            }

            instance.transform.idt();
            float tileBaseY = getTileTopY(tileX, tileY);
            applyManifestModelTransform(instance, baseMeta, tileX, tileY, tileBaseY, rotationYDegrees, 1f);
            instance.calculateTransforms();

            renderInstanceWithOptionalAlpha(instance, alpha, characterEnvironment);
            actorModelsRenderedLastFrame++;
            captureDebugModelMarker(baseKey, instance.model, instance);
        } catch (Exception e) {
            if (ownsPass) {
                endActorModelPass();
            }
            return false;
        }

        if (ownsPass) {
            endActorModelPass();
        }
        return true;
    }

    public void retainAnimatedNpcEntities(Set<Integer> activeEntityIds) {
        if (activeEntityIds == null) {
            npcAnimatedInstances.clear();
            npcAnimationControllers.clear();
            npcBaseKeyByEntityId.clear();
            currentNpcClipByEntityId.clear();
            npcAnimTimes.clear();
            return;
        }
        npcAnimatedInstances.keySet().retainAll(activeEntityIds);
        npcAnimationControllers.keySet().retainAll(activeEntityIds);
        npcBaseKeyByEntityId.keySet().retainAll(activeEntityIds);
        currentNpcClipByEntityId.keySet().retainAll(activeEntityIds);
        npcAnimTimes.keySet().retainAll(activeEntityIds);
    }

    private void renderPlayerEquipmentAttachments(ModelInstance baseInstance,
                                                  float tileX,
                                                  float tileY,
                                                  float tileBaseY,
                                                  float rotationYDegrees,
                                                  float baseScale,
                                                  int[] equippedItemIds,
                                                  Map<Integer, float[]> previewOverridesBySlot) {
        if (equippedItemIds == null) {
            return;
        }
        for (int slot : PLAYER_EQUIPMENT_VISIBLE_SLOTS) {
            if (slot < 0 || slot >= equippedItemIds.length) {
                continue;
            }
            int itemId = equippedItemIds[slot];
            if (itemId <= 0) {
                continue;
            }

            if (!modelLibrary.hasEquipmentCoverage(slot, itemId)) {
                warnMissingEquipmentCoverageOnce(slot, itemId);
                continue;
            }

            ModelLibrary.ModelMeta equipMeta = modelLibrary.getEquipmentMeta(slot, itemId);
            Model equipModel = modelLibrary.getEquipmentModel(slot, itemId);
            if (equipMeta == null || equipModel == null) {
                continue;
            }

            ModelInstance equipInstance = obtainActorModelInstance(equipMeta.key(), equipModel);
            equipInstance.transform.idt();

            float offsetX = equipMeta.offsetX();
            float offsetY = equipMeta.offsetY();
            float offsetZ = equipMeta.offsetZ();
            float rotX = equipMeta.rotX();
            float rotY = equipMeta.rotY();
            float rotZ = equipMeta.rotZ();
            if (previewOverridesBySlot != null) {
                float[] override = previewOverridesBySlot.get(slot);
                if (override != null && override.length >= 6) {
                    offsetX += override[0];
                    offsetY += override[1];
                    offsetZ += override[2];
                    rotX += override[3];
                    rotY += override[4];
                    rotZ += override[5];
                }
            }

            Matrix4 anchorTransform = findActorAnchorTransform(baseInstance, equipMeta.anchorName());
            if (anchorTransform != null) {
                equipInstance.transform.set(anchorTransform);
                equipInstance.transform.translate(
                    offsetX - defaultAnchorOffsetXForSlot(slot),
                    offsetY - defaultAnchorOffsetYForSlot(slot),
                    offsetZ - defaultAnchorOffsetZForSlot(slot)
                );
            } else {
                equipInstance.transform.translate(tileX + 0.5f, tileBaseY, tileY + 0.5f);
                if (Math.abs(rotationYDegrees) > 0.0001f) {
                    equipInstance.transform.rotate(Vector3.Y, rotationYDegrees);
                }
                if (Math.abs(baseScale - 1f) > 0.0001f) {
                    equipInstance.transform.scale(baseScale, baseScale, baseScale);
                }
                equipInstance.transform.translate(offsetX, offsetY, offsetZ);
            }
            if (Math.abs(rotX) > 0.0001f) {
                equipInstance.transform.rotate(Vector3.X, rotX);
            }
            if (Math.abs(rotY) > 0.0001f) {
                equipInstance.transform.rotate(Vector3.Y, rotY);
            }
            if (Math.abs(rotZ) > 0.0001f) {
                equipInstance.transform.rotate(Vector3.Z, rotZ);
            }
            if (equipMeta.scale() > 0f && Math.abs(equipMeta.scale() - 1f) > 0.0001f) {
                equipInstance.transform.scale(equipMeta.scale(), equipMeta.scale(), equipMeta.scale());
            }
            modelBatch.render(equipInstance, environment);
        }
    }

    private boolean ensureAnimatedPlayerModelLoaded(int entityId) {
        if (entityId <= 0 || modelLibrary == null || !modelLibrary.hasModel("player_base")) {
            animatedPlayerInstances.remove(entityId);
            playerAnimationControllers.remove(entityId);
            currentPlayerClipByEntityId.remove(entityId);
            playerAnimTimes.remove(entityId);
            return false;
        }
        if (animatedPlayerInstances.containsKey(entityId) && playerAnimationControllers.containsKey(entityId)) {
            return true;
        }

        Model model = modelLibrary.getModel("player_base");
        if (model == null) {
            return false;
        }
        try {
            ModelInstance instance = new ModelInstance(model);
            for (Material m : instance.materials) m.set(IntAttribute.createCullFace(GL20.GL_BACK));
            AnimationController controller = new AnimationController(instance);
            animatedPlayerInstances.put(entityId, instance);
            playerAnimationControllers.put(entityId, controller);
            currentPlayerClipByEntityId.put(entityId, "");
            playerAnimTimes.put(entityId, 0f);
            return true;
        } catch (Exception e) {
            animatedPlayerInstances.remove(entityId);
            playerAnimationControllers.remove(entityId);
            currentPlayerClipByEntityId.remove(entityId);
            playerAnimTimes.remove(entityId);
            return false;
        }
    }

    private boolean ensureAnimatedNpcModelLoaded(int entityId, String baseKey) {
        if (entityId <= 0 || baseKey == null || baseKey.isBlank() || modelLibrary == null || !modelLibrary.hasModel(baseKey)) {
            npcAnimatedInstances.remove(entityId);
            npcAnimationControllers.remove(entityId);
            npcBaseKeyByEntityId.remove(entityId);
            currentNpcClipByEntityId.remove(entityId);
            return false;
        }

        String currentBaseKey = npcBaseKeyByEntityId.get(entityId);
        if (baseKey.equals(currentBaseKey)
            && npcAnimatedInstances.containsKey(entityId)
            && npcAnimationControllers.containsKey(entityId)) {
            return true;
        }

        Model model = modelLibrary.getModel(baseKey);
        if (model == null) {
            return false;
        }

        try {
            ModelInstance instance = new ModelInstance(model);
            for (Material m : instance.materials) m.set(IntAttribute.createCullFace(GL20.GL_BACK));
            AnimationController controller = new AnimationController(instance);
            npcAnimatedInstances.put(entityId, instance);
            npcAnimationControllers.put(entityId, controller);
            npcBaseKeyByEntityId.put(entityId, baseKey);
            currentNpcClipByEntityId.put(entityId, "");
            return true;
        } catch (Exception e) {
            npcAnimatedInstances.remove(entityId);
            npcAnimationControllers.remove(entityId);
            npcBaseKeyByEntityId.remove(entityId);
            currentNpcClipByEntityId.remove(entityId);
            return false;
        }
    }

    private String normalizePlayerClipName(String stateKey) {
        if (stateKey == null || stateKey.isBlank()) {
            return "idle";
        }
        return switch (stateKey) {
            case "player_idle" -> "idle";
            case "player_walk" -> "walk";
            case "player_pickup" -> "pickup";
            case "player_chop" -> "chop";
            case "player_mine" -> "mine";
            case "player_fish" -> "fish";
            case "player_sword" -> "sword";
            case "player_spear" -> "spear";
            default -> "idle";
        };
    }

    private String normalizeNpcClipName(String stateKey) {
        if (stateKey == null || stateKey.isBlank()) {
            return "idle";
        }
        if (stateKey.endsWith("_walk")) {
            return "walk";
        }
        if (stateKey.endsWith("_action")) {
            return "action";
        }
        return "idle";
    }

    // ── Programmatic skeletal animation ─────────────────────────────────────

    private static final float WALK_PERIOD    = 0.6f;
    private static final float WALK_LEG_ANG   = 28f;
    private static final float WALK_ARM_ANG   = 20f;
    private static final float WALK_KNEE_F    = 0.45f;
    private static final float WALK_ELBOW_F   = 0.30f;

    /** Dispatch to walk or idle based on clip name. No-op if model lacks multi-bone structure. */
    private void applyCharacterAnimation(ModelInstance instance, String clipName, float animTime) {
        if (instance.getNode("upper_arm_l", true) == null) return;
        if ("walk".equals(clipName)) {
            applyWalkAnimation(instance, animTime);
        } else {
            applyIdleAnimation(instance, animTime);
        }
    }

    private void applyWalkAnimation(ModelInstance instance, float animTime) {
        float phase   = (animTime % WALK_PERIOD) / WALK_PERIOD * MathUtils.PI2;
        float legAng  =  MathUtils.sin(phase) * WALK_LEG_ANG;
        float armAng  = -MathUtils.sin(phase) * WALK_ARM_ANG;
        float kneeL   = Math.max(0f,  legAng) * WALK_KNEE_F;
        float kneeR   = Math.max(0f, -legAng) * WALK_KNEE_F;
        float elbowL  = Math.max(0f, -armAng) * WALK_ELBOW_F;
        float elbowR  = Math.max(0f,  armAng) * WALK_ELBOW_F;

        setNodeRotX(instance, "upper_leg_l",  legAng);
        setNodeRotX(instance, "upper_leg_r", -legAng);
        setNodeRotX(instance, "lower_leg_l",  kneeL);
        setNodeRotX(instance, "lower_leg_r",  kneeR);
        setNodeRotX(instance, "upper_arm_l",  armAng);
        setNodeRotX(instance, "upper_arm_r", -armAng);
        setNodeRotX(instance, "lower_arm_l",  elbowL);
        setNodeRotX(instance, "lower_arm_r",  elbowR);
    }

    private void applyIdleAnimation(ModelInstance instance, float animTime) {
        setNodeRotX(instance, "upper_leg_l", 0f);
        setNodeRotX(instance, "upper_leg_r", 0f);
        setNodeRotX(instance, "lower_leg_l", 0f);
        setNodeRotX(instance, "lower_leg_r", 0f);
        setNodeRotX(instance, "upper_arm_l", -5f);
        setNodeRotX(instance, "upper_arm_r", -5f);
        setNodeRotX(instance, "lower_arm_l", 0f);
        setNodeRotX(instance, "lower_arm_r", 0f);
    }

    /** Set a bone's rotation about world X axis (does not modify translation/scale). */
    private void setNodeRotX(ModelInstance inst, String nodeId, float degrees) {
        Node node = inst.getNode(nodeId, true);
        if (node == null) return;
        node.rotation.setFromAxis(Vector3.X, degrees);
    }

    // ────────────────────────────────────────────────────────────────────────

    private void renderInstanceWithOptionalAlpha(ModelInstance instance, float alpha) {
        renderInstanceWithOptionalAlpha(instance, alpha, environment);
    }

    private void renderInstanceWithOptionalAlpha(ModelInstance instance, float alpha, Environment env) {
        float clampedAlpha = Math.max(0f, Math.min(1f, alpha));
        if (clampedAlpha >= 0.999f) {
            modelBatch.render(instance, env);
            return;
        }

        float[] previousDiffuseAlpha = new float[instance.materials.size];
        float[] previousBlendAlpha = new float[instance.materials.size];
        int[] previousBlendSource = new int[instance.materials.size];
        int[] previousBlendDest = new int[instance.materials.size];
        boolean[] hadBlend = new boolean[instance.materials.size];

        for (int i = 0; i < instance.materials.size; i++) {
            Material material = instance.materials.get(i);
            ColorAttribute diffuse = (ColorAttribute) material.get(ColorAttribute.Diffuse);
            previousDiffuseAlpha[i] = diffuse != null ? diffuse.color.a : 1f;

            BlendingAttribute blend = (BlendingAttribute) material.get(BlendingAttribute.Type);
            if (blend != null) {
                hadBlend[i] = true;
                previousBlendAlpha[i] = blend.opacity;
                previousBlendSource[i] = blend.sourceFunction;
                previousBlendDest[i] = blend.destFunction;
            }

            if (diffuse != null) {
                diffuse.color.a = clampedAlpha;
            }
            material.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, clampedAlpha));
        }

        Gdx.gl.glDepthMask(false);
        modelBatch.render(instance, env);
        Gdx.gl.glDepthMask(true);

        for (int i = 0; i < instance.materials.size; i++) {
            Material material = instance.materials.get(i);
            ColorAttribute diffuse = (ColorAttribute) material.get(ColorAttribute.Diffuse);
            if (diffuse != null) {
                diffuse.color.a = previousDiffuseAlpha[i];
            }
            if (hadBlend[i]) {
                material.set(new BlendingAttribute(previousBlendSource[i], previousBlendDest[i], previousBlendAlpha[i]));
            } else {
                material.remove(BlendingAttribute.Type);
            }
        }
    }

    private void warnMissingEquipmentCoverageOnce(int equipSlot, int itemId) {
        long key = (((long) equipSlot) << 32) | (itemId & 0xffffffffL);
        if (!missingEquipmentWarnings.add(key)) {
            return;
        }
        String slotName = switch (equipSlot) {
            case EquipmentSlot.HEAD -> "HEAD";
            case EquipmentSlot.CAPE -> "CAPE";
            case EquipmentSlot.AMMO -> "AMMO";
            case EquipmentSlot.WEAPON -> "WEAPON";
            case EquipmentSlot.SHIELD -> "SHIELD";
            case EquipmentSlot.BODY -> "BODY";
            case EquipmentSlot.LEGS -> "LEGS";
            case EquipmentSlot.HANDS -> "HANDS";
            case EquipmentSlot.FEET -> "FEET";
            default -> "SLOT_" + equipSlot;
        };
        String itemName = modelLibrary == null ? "" : modelLibrary.getKnownItemName(itemId);
        String nameSuffix = itemName == null || itemName.isBlank()
            ? ""
            : " name='" + itemName + "'";
        Gdx.app.log(
            "Renderer3DExperimental",
            "WARN: missing 3D equipment coverage for slot=" + slotName + " itemId=" + itemId + nameSuffix
        );
    }

    private Matrix4 findActorAnchorTransform(ModelInstance baseInstance, String anchorName) {
        if (baseInstance == null || anchorName == null || anchorName.isBlank()) {
            return null;
        }
        Node anchorNode = baseInstance.getNode(anchorName, true);
        if (anchorNode == null) {
            return null;
        }
        return new Matrix4(baseInstance.transform).mul(anchorNode.globalTransform);
    }

    private void captureDebugModelMarker(String modelKey, Model model, ModelInstance instance) {
        if (!debugModelAxesAndBoundsEnabled || instance == null || model == null) {
            return;
        }
        BoundingBox cachedBounds = getCachedModelBounds(modelKey, model);
        if (cachedBounds == null) {
            return;
        }
        debugModelMarkers.add(new DebugModelMarker(new Matrix4(instance.transform), new BoundingBox(cachedBounds)));
    }

    private BoundingBox getCachedModelBounds(String modelKey, Model model) {
        if (model == null) {
            return null;
        }
        String key = modelKey == null || modelKey.isBlank() ? "__anon__" + System.identityHashCode(model) : modelKey;
        BoundingBox cached = modelBoundsCache.get(key);
        if (cached != null) {
            return cached;
        }
        BoundingBox bounds = new BoundingBox();
        model.calculateBoundingBox(bounds);
        modelBoundsCache.put(key, bounds);
        return bounds;
    }

    private void capturePlayerAnchorMarkers(ModelInstance baseInstance) {
        if (!debugAnchorMarkersEnabled || baseInstance == null) {
            return;
        }
        for (String anchorName : DEBUG_PLAYER_ANCHOR_NAMES) {
            Matrix4 anchorTransform = findActorAnchorTransform(baseInstance, anchorName);
            if (anchorTransform == null) {
                continue;
            }
            Vector3 point = new Vector3();
            anchorTransform.getTranslation(point);
            debugAnchorPoints.add(point);
        }
    }

    private void drawAxes(Matrix4 transform) {
        Vector3 origin = debugTmpA.set(0f, 0f, 0f).mul(transform);
        Vector3 x = debugTmpB.set(DEBUG_AXIS_LENGTH, 0f, 0f).mul(transform);
        Vector3 y = debugTmpC.set(0f, DEBUG_AXIS_LENGTH, 0f).mul(transform);
        Vector3 z = new Vector3(0f, 0f, DEBUG_AXIS_LENGTH).mul(transform);

        debugShapeRenderer.setColor(1f, 0f, 0f, 1f);
        debugShapeRenderer.line(origin, x);
        debugShapeRenderer.setColor(0f, 1f, 0f, 1f);
        debugShapeRenderer.line(origin, y);
        debugShapeRenderer.setColor(0.2f, 0.6f, 1f, 1f);
        debugShapeRenderer.line(origin, z);
    }

    private void drawBounds(Matrix4 transform, BoundingBox localBounds) {
        if (localBounds == null) {
            return;
        }
        float minX = localBounds.min.x;
        float minY = localBounds.min.y;
        float minZ = localBounds.min.z;
        float maxX = localBounds.max.x;
        float maxY = localBounds.max.y;
        float maxZ = localBounds.max.z;

        Vector3 p000 = new Vector3(minX, minY, minZ).mul(transform);
        Vector3 p100 = new Vector3(maxX, minY, minZ).mul(transform);
        Vector3 p010 = new Vector3(minX, maxY, minZ).mul(transform);
        Vector3 p110 = new Vector3(maxX, maxY, minZ).mul(transform);
        Vector3 p001 = new Vector3(minX, minY, maxZ).mul(transform);
        Vector3 p101 = new Vector3(maxX, minY, maxZ).mul(transform);
        Vector3 p011 = new Vector3(minX, maxY, maxZ).mul(transform);
        Vector3 p111 = new Vector3(maxX, maxY, maxZ).mul(transform);

        debugShapeRenderer.setColor(0.9f, 0.85f, 0.2f, 1f);
        debugShapeRenderer.line(p000, p100);
        debugShapeRenderer.line(p100, p110);
        debugShapeRenderer.line(p110, p010);
        debugShapeRenderer.line(p010, p000);
        debugShapeRenderer.line(p001, p101);
        debugShapeRenderer.line(p101, p111);
        debugShapeRenderer.line(p111, p011);
        debugShapeRenderer.line(p011, p001);
        debugShapeRenderer.line(p000, p001);
        debugShapeRenderer.line(p100, p101);
        debugShapeRenderer.line(p110, p111);
        debugShapeRenderer.line(p010, p011);
    }

    private void drawCross(Vector3 point, float size) {
        Vector3 x1 = new Vector3(point.x - size, point.y, point.z);
        Vector3 x2 = new Vector3(point.x + size, point.y, point.z);
        Vector3 y1 = new Vector3(point.x, point.y - size, point.z);
        Vector3 y2 = new Vector3(point.x, point.y + size, point.z);
        Vector3 z1 = new Vector3(point.x, point.y, point.z - size);
        Vector3 z2 = new Vector3(point.x, point.y, point.z + size);
        debugShapeRenderer.line(x1, x2);
        debugShapeRenderer.line(y1, y2);
        debugShapeRenderer.line(z1, z2);
    }

    private void applyPreviewCamera() {
        float yawRad = previewCameraYawDegrees * MathUtils.degreesToRadians;
        float pitchRad = previewCameraPitchDegrees * MathUtils.degreesToRadians;
        float cosPitch = (float) Math.cos(pitchRad);
        float sinPitch = (float) Math.sin(pitchRad);
        float sinYaw = (float) Math.sin(yawRad);
        float cosYaw = (float) Math.cos(yawRad);

        float targetX = 0f;
        float targetY = 0.85f;
        float targetZ = 0f;

        float offsetX = sinYaw * cosPitch * previewCameraDistance;
        float offsetY = sinPitch * previewCameraDistance;
        float offsetZ = cosYaw * cosPitch * previewCameraDistance;

        camera.position.set(targetX + offsetX, targetY + offsetY, targetZ + offsetZ);
        camera.lookAt(targetX, targetY, targetZ);
        camera.up.set(0f, 1f, 0f);
        camera.update();
    }

    private void drawPreviewGroundMarkers() {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(false);
        debugShapeRenderer.setProjectionMatrix(camera.combined);
        debugShapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        debugShapeRenderer.setColor(0.26f, 0.30f, 0.34f, 0.95f);
        for (int i = -4; i <= 4; i++) {
            debugShapeRenderer.line(i, 0f, -4f, i, 0f, 4f);
            debugShapeRenderer.line(-4f, 0f, i, 4f, 0f, i);
        }
        debugShapeRenderer.setColor(0.80f, 0.20f, 0.20f, 1f);
        debugShapeRenderer.line(-0.18f, 0.001f, 0f, 0.18f, 0.001f, 0f);
        debugShapeRenderer.setColor(0.20f, 0.84f, 0.24f, 1f);
        debugShapeRenderer.line(0f, 0.001f, -0.18f, 0f, 0.001f, 0.18f);
        debugShapeRenderer.end();
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
    }

    private String resolvePreviewClip(ModelInstance instance, String requestedClip) {
        if (instance == null || instance.animations == null || instance.animations.isEmpty()) {
            return "";
        }
        if (requestedClip != null && !requestedClip.isBlank() && instance.getAnimation(requestedClip) != null) {
            return requestedClip;
        }
        if (instance.getAnimation("idle") != null) {
            return "idle";
        }
        return instance.animations.get(0).id;
    }

    private void applyManifestModelTransform(ModelInstance instance,
                                             ModelLibrary.ModelMeta meta,
                                             float tileX,
                                             float tileY,
                                             float tileBaseY,
                                             float placementRotationYDegrees,
                                             float placementScale) {
        instance.transform.idt();
        instance.transform.translate(tileX + 0.5f, tileBaseY, tileY + 0.5f);

        if (Math.abs(placementRotationYDegrees) > 0.0001f) {
            instance.transform.rotate(Vector3.Y, placementRotationYDegrees);
        }

        if (meta != null) {
            if (Math.abs(meta.offsetX()) > 0.0001f || Math.abs(meta.offsetY()) > 0.0001f || Math.abs(meta.offsetZ()) > 0.0001f) {
                instance.transform.translate(meta.offsetX(), meta.offsetY(), meta.offsetZ());
            }
            if (Math.abs(meta.rotX()) > 0.0001f) {
                instance.transform.rotate(Vector3.X, meta.rotX());
            }
            if (Math.abs(meta.rotY()) > 0.0001f) {
                instance.transform.rotate(Vector3.Y, meta.rotY());
            }
            if (Math.abs(meta.rotZ()) > 0.0001f) {
                instance.transform.rotate(Vector3.Z, meta.rotZ());
            }
        }

        float effectiveScale = 1f;
        if (meta != null && meta.scale() > 0f) {
            effectiveScale *= meta.scale();
        }
        if (placementScale > 0f) {
            effectiveScale *= placementScale;
        }
        if (Math.abs(effectiveScale - 1f) > 0.0001f) {
            instance.transform.scale(effectiveScale, effectiveScale, effectiveScale);
        }
    }

    private java.util.Map<Node, Boolean> applyHiddenBaseNodes(ModelInstance baseInstance, int[] equippedItemIds) {
        java.util.Map<Node, Boolean> previous = new LinkedHashMap<>();
        if (baseInstance == null || equippedItemIds == null || modelLibrary == null) {
            return previous;
        }

        LinkedHashSet<String> hideNodeNames = new LinkedHashSet<>();
        for (int slot : PLAYER_EQUIPMENT_VISIBLE_SLOTS) {
            if (slot < 0 || slot >= equippedItemIds.length) {
                continue;
            }
            int itemId = equippedItemIds[slot];
            if (itemId <= 0) {
                continue;
            }
            ModelLibrary.ModelMeta equipMeta = modelLibrary.getEquipmentMeta(slot, itemId);
            if (equipMeta == null || equipMeta.hideNodes() == null || equipMeta.hideNodes().isEmpty()) {
                continue;
            }
            for (String nodeName : equipMeta.hideNodes()) {
                if (nodeName != null && !nodeName.isBlank()) {
                    hideNodeNames.add(nodeName);
                }
            }
        }

        for (String nodeName : hideNodeNames) {
            Node node = baseInstance.getNode(nodeName, true);
            if (node == null) {
                continue;
            }
            captureNodeVisibility(node, previous);
            setNodeVisibilityRecursive(node, false);
        }

        return previous;
    }

    private void restoreHiddenBaseNodes(java.util.Map<Node, Boolean> previous) {
        if (previous == null || previous.isEmpty()) {
            return;
        }
        for (Map.Entry<Node, Boolean> entry : previous.entrySet()) {
            Node node = entry.getKey();
            if (node == null || node.parts == null) {
                continue;
            }
            boolean enabled = Boolean.TRUE.equals(entry.getValue());
            for (int i = 0; i < node.parts.size; i++) {
                node.parts.get(i).enabled = enabled;
            }
        }
    }

    private void captureNodeVisibility(Node node, java.util.Map<Node, Boolean> previous) {
        if (node == null || previous.containsKey(node)) {
            return;
        }
        previous.put(node, areNodePartsVisible(node));
        if (node.getChildren() == null) {
            return;
        }
        for (Node child : node.getChildren()) {
            captureNodeVisibility(child, previous);
        }
    }

    private void setNodeVisibilityRecursive(Node node, boolean enabled) {
        if (node == null) {
            return;
        }
        if (node.parts != null) {
            for (int i = 0; i < node.parts.size; i++) {
                node.parts.get(i).enabled = enabled;
            }
        }
        if (node.getChildren() == null) {
            return;
        }
        for (Node child : node.getChildren()) {
            setNodeVisibilityRecursive(child, enabled);
        }
    }

    private boolean areNodePartsVisible(Node node) {
        if (node == null || node.parts == null || node.parts.size == 0) {
            return true;
        }
        for (int i = 0; i < node.parts.size; i++) {
            if (node.parts.get(i).enabled) {
                return true;
            }
        }
        return false;
    }

    private float defaultAnchorOffsetXForSlot(int slot) {
        return switch (slot) {
            case EquipmentSlot.AMMO -> DEFAULT_AMMO_ANCHOR_X;
            case EquipmentSlot.WEAPON -> DEFAULT_WEAPON_ANCHOR_X;
            case EquipmentSlot.SHIELD -> DEFAULT_SHIELD_ANCHOR_X;
            default -> 0f;
        };
    }

    private float defaultAnchorOffsetYForSlot(int slot) {
        return switch (slot) {
            case EquipmentSlot.HEAD -> DEFAULT_HEAD_ANCHOR_Y;
            case EquipmentSlot.CAPE -> DEFAULT_CAPE_ANCHOR_Y;
            case EquipmentSlot.AMMO -> DEFAULT_AMMO_ANCHOR_Y;
            case EquipmentSlot.WEAPON -> DEFAULT_WEAPON_ANCHOR_Y;
            case EquipmentSlot.SHIELD -> DEFAULT_SHIELD_ANCHOR_Y;
            case EquipmentSlot.BODY -> DEFAULT_BODY_ANCHOR_Y;
            case EquipmentSlot.LEGS -> DEFAULT_LEGS_ANCHOR_Y;
            case EquipmentSlot.HANDS -> DEFAULT_HANDS_ANCHOR_Y;
            case EquipmentSlot.FEET -> DEFAULT_FEET_ANCHOR_Y;
            default -> 0f;
        };
    }

    private float defaultAnchorOffsetZForSlot(int slot) {
        return switch (slot) {
            case EquipmentSlot.CAPE -> DEFAULT_CAPE_ANCHOR_Z;
            case EquipmentSlot.AMMO -> DEFAULT_AMMO_ANCHOR_Z;
            case EquipmentSlot.WEAPON -> DEFAULT_WEAPON_ANCHOR_Z;
            case EquipmentSlot.SHIELD -> DEFAULT_SHIELD_ANCHOR_Z;
            default -> 0f;
        };
    }

    public int[] pickTile(int screenX, int screenY) {
        Ray ray = camera.getPickRay(screenX, screenY);
        int centerTileX = clampTileX((int) Math.floor(camera.position.x));
        int centerTileY = clampTileY((int) Math.floor(camera.position.z));
        int radius = Math.min(40, Math.max(20, (int) Math.ceil(camera.position.y * 2f + 10f)));

        int minTileX = Math.max(0, centerTileX - radius);
        int maxTileX = Math.min(mapWidth - 1, centerTileX + radius);
        int minTileY = Math.max(0, centerTileY - radius);
        int maxTileY = Math.min(mapHeight - 1, centerTileY + radius);

        float bestT = Float.POSITIVE_INFINITY;
        int bestX = -1;
        int bestY = -1;
        if (Math.abs(ray.direction.y) > 0.000001f) {
            for (int y = minTileY; y <= maxTileY; y++) {
                for (int x = minTileX; x <= maxTileX; x++) {
                    float tileTopY = getTileTopY(lastTileMap, x, y);
                    float t = (tileTopY - ray.origin.y) / ray.direction.y;
                    if (t <= 0.0001f || t >= bestT) {
                        continue;
                    }
                    float hitX = ray.origin.x + ray.direction.x * t;
                    float hitZ = ray.origin.z + ray.direction.z * t;
                    if (hitX >= x && hitX <= x + 1f && hitZ >= y && hitZ <= y + 1f) {
                        bestT = t;
                        bestX = x;
                        bestY = y;
                    }
                }
            }
        }

        if (bestX >= 0 && bestY >= 0) {
            return new int[]{bestX, bestY};
        }

        Vector3 intersection = new Vector3();
        if (!Intersector.intersectRayPlane(ray, groundPlane, intersection)) {
            return new int[]{-1, -1};
        }
        int fallbackX = (int) Math.floor(intersection.x);
        int fallbackY = (int) Math.floor(intersection.z);
        if (fallbackX < 0 || fallbackY < 0 || fallbackX >= mapWidth || fallbackY >= mapHeight) {
            return new int[]{-1, -1};
        }
        return new int[]{fallbackX, fallbackY};
    }

    public Float intersectRayCylinder(Ray ray,
                                      float centerX,
                                      float centerZ,
                                      float radius,
                                      float minY,
                                      float maxY) {
        if (ray == null || radius <= 0f || maxY <= minY) {
            return null;
        }

        float ox = ray.origin.x - centerX;
        float oz = ray.origin.z - centerZ;
        float dx = ray.direction.x;
        float dz = ray.direction.z;

        float bestT = Float.POSITIVE_INFINITY;
        float a = dx * dx + dz * dz;
        if (a > 0.000001f) {
            float b = 2f * (ox * dx + oz * dz);
            float c = ox * ox + oz * oz - radius * radius;
            float discriminant = b * b - 4f * a * c;
            if (discriminant >= 0f) {
                float sqrtDisc = (float) Math.sqrt(discriminant);
                float invDenominator = 1f / (2f * a);
                float t0 = (-b - sqrtDisc) * invDenominator;
                float t1 = (-b + sqrtDisc) * invDenominator;
                if (t0 > 0.0001f) {
                    float y0 = ray.origin.y + ray.direction.y * t0;
                    if (y0 >= minY && y0 <= maxY) {
                        bestT = Math.min(bestT, t0);
                    }
                }
                if (t1 > 0.0001f) {
                    float y1 = ray.origin.y + ray.direction.y * t1;
                    if (y1 >= minY && y1 <= maxY) {
                        bestT = Math.min(bestT, t1);
                    }
                }
            }
        }

        float dy = ray.direction.y;
        if (Math.abs(dy) > 0.000001f) {
            float tMinCap = (minY - ray.origin.y) / dy;
            if (tMinCap > 0.0001f) {
                float x = ray.origin.x + ray.direction.x * tMinCap - centerX;
                float z = ray.origin.z + ray.direction.z * tMinCap - centerZ;
                if (x * x + z * z <= radius * radius) {
                    bestT = Math.min(bestT, tMinCap);
                }
            }

            float tMaxCap = (maxY - ray.origin.y) / dy;
            if (tMaxCap > 0.0001f) {
                float x = ray.origin.x + ray.direction.x * tMaxCap - centerX;
                float z = ray.origin.z + ray.direction.z * tMaxCap - centerZ;
                if (x * x + z * z <= radius * radius) {
                    bestT = Math.min(bestT, tMaxCap);
                }
            }
        }

        return bestT == Float.POSITIVE_INFINITY ? null : bestT;
    }

    public float getTileTopY(float tileX, float tileY) {
        int x = (int) Math.floor(tileX);
        int y = (int) Math.floor(tileY);
        return getTileTopY(lastTileMap, x, y);
    }

    public boolean isPointNearCamera(float worldX, float worldY, float worldZ, float maxDistance) {
        if (maxDistance <= 0f) {
            return false;
        }
        return camera.position.dst2(worldX, worldY, worldZ) <= maxDistance * maxDistance;
    }

    public boolean isTileCenterNearCamera(float tileX, float tileY, float maxDistance) {
        return isPointNearCamera(tileX + 0.5f, 0f, tileY + 0.5f, maxDistance);
    }

    public boolean isPointInCameraFrustum(float worldX, float worldY, float worldZ) {
        frustumQueryPoint.set(worldX, worldY, worldZ);
        return camera.frustum.pointInFrustum(frustumQueryPoint);
    }

    public boolean isTileCenterVisible(float tileX, float tileY) {
        return isPointInCameraFrustum(tileX + 0.5f, 0f, tileY + 0.5f);
    }

    public int getTerrainChunksRenderedLastFrame() {
        return terrainChunksRenderedLastFrame;
    }

    public int getOverlayTilesProcessedLastFrame() {
        return overlayTilesProcessedLastFrame;
    }

    public int getStaticPropsRenderedLastFrame() {
        return staticPropsRenderedLastFrame;
    }

    public int getActorModelsRenderedLastFrame() {
        return actorModelsRenderedLastFrame;
    }

    public int getEntityBillboardsRenderedLastFrame() {
        return entityBillboardsRenderedLastFrame;
    }

    public void resize(int width, int height) {
        camera.viewportWidth = Math.max(1, width);
        camera.viewportHeight = Math.max(1, height);
        camera.update();
    }

    public void dispose() {
        endStaticPropPass();
        endActorModelPass();
        for (Model model : terrainChunkModels.values()) {
            model.dispose();
        }
        terrainChunkModels.clear();
        terrainChunks.clear();
        wallMaterialsByChunk.clear();
        animatedPlayerInstances.clear();
        playerAnimationControllers.clear();
        currentPlayerClipByEntityId.clear();
        playerAnimTimes.clear();
        npcAnimatedInstances.clear();
        npcAnimationControllers.clear();
        npcBaseKeyByEntityId.clear();
        currentNpcClipByEntityId.clear();
        npcAnimTimes.clear();
        previewInstanceByKey.clear();
        previewAnimationControllerByKey.clear();
        previewClipByKey.clear();
        previewEquipmentFitPlayerInstance = null;
        previewEquipmentFitPlayerController = null;
        previewEquipmentFitPlayerClip = "";
        missingEquipmentWarnings.clear();
        fallbackBillboardTexture.dispose();
        debugShapeRenderer.dispose();
        decalBatch.dispose();
        overlayDecalBatch.dispose();
        modelBatch.dispose();
    }

    private Model buildChunkModel(int[][] tileMap,
                                  int chunkX,
                                  int chunkY,
                                  String materialProfile,
                                  ArrayList<WallMaterialBinding> wallBindings) {
        int startX = chunkX * CHUNK_SIZE;
        int startY = chunkY * CHUNK_SIZE;
        int endX = Math.min(mapWidth, startX + CHUNK_SIZE);
        int endY = Math.min(mapHeight, startY + CHUNK_SIZE);

        modelBuilder.begin();
        boolean hasGeometry = false;

        // One shared vertex-colored part for all ground tiles (grass/path/water/sand).
        // Vertex colors blend at shared corners → eliminates visible tile seams.
        Material colorMat = new Material(ColorAttribute.createDiffuse(Color.WHITE));
        int colorAttrs = VertexAttributes.Usage.Position
                       | VertexAttributes.Usage.Normal
                       | VertexAttributes.Usage.ColorUnpacked;
        MeshPartBuilder coloredPart = modelBuilder.part(
            "ground_" + chunkX + "_" + chunkY, GL20.GL_TRIANGLES, colorAttrs, colorMat);

        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                int type = tileMap[x][y];
                TextureRegion region = resolveTileRegion(type, x, y, materialProfile);
                boolean useFallback = (region == null);

                float x0 = x;
                float z0 = y;
                float x1 = x + 1f;
                float z1 = y + 1f;
                float tileTopY = getTileTopY(tileMap, x, y);

                if (type == TILE_WALL && !useFallback) {
                    Material baseTileMaterial = new Material(TextureAttribute.createDiffuse(region.getTexture()));
                    MeshPartBuilder baseTilePart = modelBuilder.part(
                        "wall_tile_base_" + x + "_" + y,
                        GL20.GL_TRIANGLES,
                        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates,
                        baseTileMaterial
                    );
                    addTopQuad(baseTilePart, x0, z0, x1, z1, tileTopY, region);

                    float wallTopY = tileTopY + WALL_TOP_Y;
                    Material topMaterial = createWallTopMaterial(region);
                    MeshPartBuilder topPart = modelBuilder.part(
                        "wall_top_" + x + "_" + y,
                        GL20.GL_TRIANGLES,
                        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates,
                        topMaterial
                    );
                    addTopQuad(topPart, x0, z0, x1, z1, wallTopY, region);
                    wallBindings.add(new WallMaterialBinding(topMaterial, x, y, 1f));

                    if (!isWallTile(tileMap, x, y - 1)) {
                        Material northFaceMaterial = createWallFaceMaterial(region);
                        MeshPartBuilder northFacePart = modelBuilder.part(
                            "wall_face_n_" + x + "_" + y,
                            GL20.GL_TRIANGLES,
                            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates,
                            northFaceMaterial
                        );
                        addVerticalFace(
                            northFacePart,
                            x0, tileTopY, z0,
                            x1, tileTopY, z0,
                            x1, wallTopY, z0,
                            x0, wallTopY, z0,
                            region,
                            0f, 0f, -1f
                        );
                        wallBindings.add(new WallMaterialBinding(northFaceMaterial, x, y, WALL_FACE_SHADE));
                    }

                    if (!isWallTile(tileMap, x, y + 1)) {
                        Material southFaceMaterial = createWallFaceMaterial(region);
                        MeshPartBuilder southFacePart = modelBuilder.part(
                            "wall_face_s_" + x + "_" + y,
                            GL20.GL_TRIANGLES,
                            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates,
                            southFaceMaterial
                        );
                        addVerticalFace(
                            southFacePart,
                            x1, tileTopY, z1,
                            x0, tileTopY, z1,
                            x0, wallTopY, z1,
                            x1, wallTopY, z1,
                            region,
                            0f, 0f, 1f
                        );
                        wallBindings.add(new WallMaterialBinding(southFaceMaterial, x, y, WALL_FACE_SHADE));
                    }

                    if (!isWallTile(tileMap, x - 1, y)) {
                        Material westFaceMaterial = createWallFaceMaterial(region);
                        MeshPartBuilder westFacePart = modelBuilder.part(
                            "wall_face_w_" + x + "_" + y,
                            GL20.GL_TRIANGLES,
                            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates,
                            westFaceMaterial
                        );
                        addVerticalFace(
                            westFacePart,
                            x0, tileTopY, z1,
                            x0, tileTopY, z0,
                            x0, wallTopY, z0,
                            x0, wallTopY, z1,
                            region,
                            -1f, 0f, 0f
                        );
                        wallBindings.add(new WallMaterialBinding(westFaceMaterial, x, y, WALL_FACE_SHADE));
                    }

                    if (!isWallTile(tileMap, x + 1, y)) {
                        Material eastFaceMaterial = createWallFaceMaterial(region);
                        MeshPartBuilder eastFacePart = modelBuilder.part(
                            "wall_face_e_" + x + "_" + y,
                            GL20.GL_TRIANGLES,
                            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates,
                            eastFaceMaterial
                        );
                        addVerticalFace(
                            eastFacePart,
                            x1, tileTopY, z0,
                            x1, tileTopY, z1,
                            x1, wallTopY, z1,
                            x1, wallTopY, z0,
                            region,
                            1f, 0f, 0f
                        );
                        wallBindings.add(new WallMaterialBinding(eastFaceMaterial, x, y, WALL_FACE_SHADE));
                    }
                } else {
                    // Ground tiles use the shared vertex-colored part for seamless blending.
                    addColoredTopQuad(coloredPart, x0, z0, x1, z1, tileTopY, tileMap, x, y);
                    hasGeometry = true;
                }

                float northTop = getTileTopY(tileMap, x, y - 1);
                float southTop = getTileTopY(tileMap, x, y + 1);
                float eastTop = getTileTopY(tileMap, x + 1, y);
                float westTop = getTileTopY(tileMap, x - 1, y);
                String cliffFaceKey = cliffFaceKeyForTileType(type, materialProfile);
                TextureRegion cliffFaceRegion = cliffFaceKey == null ? null : spriteSheet.getTile(cliffFaceKey);
                if (cliffFaceRegion == null) {
                    cliffFaceRegion = region;
                }

                if (northTop + 0.0001f < tileTopY) {
                    addTerrainStepFace(cliffFaceRegion, "step_n_" + x + "_" + y,
                        x0, northTop, z0,
                        x1, northTop, z0,
                        x1, tileTopY, z0,
                        x0, tileTopY, z0,
                        0f, 0f, -1f);
                }
                if (southTop + 0.0001f < tileTopY) {
                    addTerrainStepFace(cliffFaceRegion, "step_s_" + x + "_" + y,
                        x1, southTop, z1,
                        x0, southTop, z1,
                        x0, tileTopY, z1,
                        x1, tileTopY, z1,
                        0f, 0f, 1f);
                }
                if (westTop + 0.0001f < tileTopY) {
                    addTerrainStepFace(cliffFaceRegion, "step_w_" + x + "_" + y,
                        x0, westTop, z1,
                        x0, westTop, z0,
                        x0, tileTopY, z0,
                        x0, tileTopY, z1,
                        -1f, 0f, 0f);
                }
                if (eastTop + 0.0001f < tileTopY) {
                    addTerrainStepFace(cliffFaceRegion, "step_e_" + x + "_" + y,
                        x1, eastTop, z0,
                        x1, eastTop, z1,
                        x1, tileTopY, z1,
                        x1, tileTopY, z0,
                        1f, 0f, 0f);
                }
                hasGeometry = true;
            }
        }

        if (!hasGeometry) {
            modelBuilder.end().dispose();
            return null;
        }
        return modelBuilder.end();
    }

    private MeshPartBuilder.VertexInfo vertex(float x,
                                              float y,
                                              float z,
                                              float nx,
                                              float ny,
                                              float nz,
                                              float u,
                                              float v) {
        MeshPartBuilder.VertexInfo vertex = new MeshPartBuilder.VertexInfo();
        vertex.setPos(x, y, z);
        vertex.setNor(nx, ny, nz);
        vertex.setUV(u, v);
        return vertex;
    }

    private void addTopQuad(MeshPartBuilder part,
                            float x0,
                            float z0,
                            float x1,
                            float z1,
                            float y,
                            TextureRegion region) {
        float ui = 0.5f / region.getTexture().getWidth();
        float vi = 0.5f / region.getTexture().getHeight();
        MeshPartBuilder.VertexInfo v00 = vertex(x0, y, z0, 0f, 1f, 0f, region.getU() + ui,  region.getV2() - vi);
        MeshPartBuilder.VertexInfo v10 = vertex(x1, y, z0, 0f, 1f, 0f, region.getU2() - ui, region.getV2() - vi);
        MeshPartBuilder.VertexInfo v11 = vertex(x1, y, z1, 0f, 1f, 0f, region.getU2() - ui, region.getV() + vi);
        MeshPartBuilder.VertexInfo v01 = vertex(x0, y, z1, 0f, 1f, 0f, region.getU() + ui,  region.getV() + vi);
        part.rect(v01, v11, v10, v00);
    }

    private void addTopQuadNoUV(MeshPartBuilder part,
                                float x0,
                                float z0,
                                float x1,
                                float z1,
                                float y) {
        MeshPartBuilder.VertexInfo v00 = new MeshPartBuilder.VertexInfo().setPos(x0, y, z0).setNor(0f, 1f, 0f);
        MeshPartBuilder.VertexInfo v10 = new MeshPartBuilder.VertexInfo().setPos(x1, y, z0).setNor(0f, 1f, 0f);
        MeshPartBuilder.VertexInfo v11 = new MeshPartBuilder.VertexInfo().setPos(x1, y, z1).setNor(0f, 1f, 0f);
        MeshPartBuilder.VertexInfo v01 = new MeshPartBuilder.VertexInfo().setPos(x0, y, z1).setNor(0f, 1f, 0f);
        part.rect(v01, v11, v10, v00);
    }

    /** OSRS-accurate base colors for each tile type (pre-lighting). */
    private static Color tileBaseColor(int type) {
        return switch (type) {
            case TILE_WATER -> new Color(0.20f, 0.36f, 0.48f, 1f); // water: slate blue
            case TILE_PATH -> new Color(0.40f, 0.36f, 0.28f, 1f); // path: warm gray-brown
            case TILE_WALL -> new Color(0.36f, 0.34f, 0.32f, 1f); // wall top: stone gray
            case TILE_SAND -> new Color(0.56f, 0.48f, 0.28f, 1f); // sand: warm tan
            default -> new Color(0.22f, 0.42f, 0.13f, 1f); // grass: dark natural green
        };
    }

    /**
     * Per-corner vertex color: average of up to 4 neighboring tile base colors
     * plus ±5% seeded noise to break up uniformity (texture-like variation).
     * cx, cz are tile-corner world coordinates (0..mapW, 0..mapH).
     */
    private Color tileCornerColor(int[][] tileMap, int cx, int cz) {
        float r = 0, g = 0, b = 0;
        int n = 0;
        int[] dxs = {-1, 0, -1, 0};
        int[] dzs = {-1, -1, 0, 0};
        for (int i = 0; i < 4; i++) {
            int tx = cx + dxs[i], tz = cz + dzs[i];
            if (tx >= 0 && tx < tileMap.length && tz >= 0 && tz < tileMap[0].length) {
                Color c = tileBaseColor(tileMap[tx][tz]);
                r += c.r; g += c.g; b += c.b; n++;
            }
        }
        if (n == 0) return tileBaseColor(0);
        r /= n; g /= n; b /= n;
        // Deterministic ±5% per-corner noise
        int h = (cx * 374761393) ^ (cz * 668265263);
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= (h >>> 16);
        float noise = ((h & 0xFF) / 255f - 0.5f) * 0.10f;
        return new Color(
            Math.max(0f, Math.min(1f, r + noise)),
            Math.max(0f, Math.min(1f, g + noise * 0.7f)),
            Math.max(0f, Math.min(1f, b + noise * 0.4f)),
            1f);
    }

    /**
     * Adds a ground tile quad to the shared vertex-colored part.
     * Each corner gets a color averaged from neighboring tile types,
     * producing seamless blending across tile boundaries.
     */
    private void addColoredTopQuad(MeshPartBuilder part,
                                   float x0, float z0, float x1, float z1, float y,
                                   int[][] tileMap, int tx, int tz) {
        Color c00 = tileCornerColor(tileMap, tx,   tz  );
        Color c10 = tileCornerColor(tileMap, tx+1, tz  );
        Color c11 = tileCornerColor(tileMap, tx+1, tz+1);
        Color c01 = tileCornerColor(tileMap, tx,   tz+1);
        MeshPartBuilder.VertexInfo v00 = new MeshPartBuilder.VertexInfo().setPos(x0,y,z0).setNor(0f,1f,0f).setCol(c00);
        MeshPartBuilder.VertexInfo v10 = new MeshPartBuilder.VertexInfo().setPos(x1,y,z0).setNor(0f,1f,0f).setCol(c10);
        MeshPartBuilder.VertexInfo v11 = new MeshPartBuilder.VertexInfo().setPos(x1,y,z1).setNor(0f,1f,0f).setCol(c11);
        MeshPartBuilder.VertexInfo v01 = new MeshPartBuilder.VertexInfo().setPos(x0,y,z1).setNor(0f,1f,0f).setCol(c01);
        part.rect(v01, v11, v10, v00);
    }

    private void addVerticalFace(MeshPartBuilder part,
                                 float x0,
                                 float y0,
                                 float z0,
                                 float x1,
                                 float y1,
                                 float z1,
                                 float x2,
                                 float y2,
                                 float z2,
                                 float x3,
                                 float y3,
                                 float z3,
                                 TextureRegion region,
                                 float nx,
                                 float ny,
                                 float nz) {
        float ui = 0.5f / region.getTexture().getWidth();
        float vi = 0.5f / region.getTexture().getHeight();
        MeshPartBuilder.VertexInfo v0 = vertex(x0, y0, z0, nx, ny, nz, region.getU() + ui,  region.getV2() - vi);
        MeshPartBuilder.VertexInfo v1 = vertex(x1, y1, z1, nx, ny, nz, region.getU2() - ui, region.getV2() - vi);
        MeshPartBuilder.VertexInfo v2 = vertex(x2, y2, z2, nx, ny, nz, region.getU2() - ui, region.getV() + vi);
        MeshPartBuilder.VertexInfo v3 = vertex(x3, y3, z3, nx, ny, nz, region.getU() + ui,  region.getV() + vi);
        part.rect(v0, v1, v2, v3);
    }

    private Material createWallTopMaterial(TextureRegion region) {
        return new Material(
            TextureAttribute.createDiffuse(region.getTexture()),
            ColorAttribute.createDiffuse(1f, 1f, 1f, 1f)
        );
    }

    private Material createWallFaceMaterial(TextureRegion region) {
        return new Material(
            TextureAttribute.createDiffuse(region.getTexture()),
            ColorAttribute.createDiffuse(WALL_FACE_SHADE, WALL_FACE_SHADE, WALL_FACE_SHADE, 1f)
        );
    }

    private boolean isWallTile(int[][] tileMap, int x, int y) {
        if (tileMap == null) {
            return false;
        }
        if (!isInBounds(x, y)) {
            return false;
        }
        return tileMap[x][y] == TILE_WALL;
    }

    private void updateWallOcclusion(float localPlayerX, float localPlayerY) {
        float cameraX = camera.position.x;
        float cameraZ = camera.position.z;
        float playerX = localPlayerX + 0.5f;
        float playerZ = localPlayerY + 0.5f;

        float dirX = playerX - cameraX;
        float dirZ = playerZ - cameraZ;
        float dirLen2 = dirX * dirX + dirZ * dirZ;
        if (dirLen2 < 0.0001f) {
            dirLen2 = 0.0001f;
        }

        for (ArrayList<WallMaterialBinding> wallBindings : wallMaterialsByChunk.values()) {
            for (WallMaterialBinding binding : wallBindings) {
                float wallCenterX = binding.tileX + 0.5f;
                float wallCenterZ = binding.tileY + 0.5f;
                boolean fade = shouldFadeWall(
                    wallCenterX,
                    wallCenterZ,
                    cameraX,
                    cameraZ,
                    playerX,
                    playerZ,
                    dirX,
                    dirZ,
                    dirLen2
                );
                float alpha = fade ? WALL_FADE_ALPHA : 1f;
                applyMaterialAlpha(binding.material, binding.shade, alpha);
            }
        }
    }

    private boolean shouldFadeWall(float wallX,
                                   float wallZ,
                                   float cameraX,
                                   float cameraZ,
                                   float playerX,
                                   float playerZ,
                                   float dirX,
                                   float dirZ,
                                   float dirLen2) {
        float toWallX = wallX - cameraX;
        float toWallZ = wallZ - cameraZ;
        float t = (toWallX * dirX + toWallZ * dirZ) / dirLen2;
        if (t <= 0.08f || t >= 1.02f) {
            return false;
        }

        float closestX = cameraX + dirX * t;
        float closestZ = cameraZ + dirZ * t;
        float offLineDx = wallX - closestX;
        float offLineDz = wallZ - closestZ;
        float lineDist = (float) Math.sqrt(offLineDx * offLineDx + offLineDz * offLineDz);
        if (lineDist > WALL_FADE_LINE_DISTANCE) {
            return false;
        }

        float toPlayerX = playerX - wallX;
        float toPlayerZ = playerZ - wallZ;
        float playerDist = (float) Math.sqrt(toPlayerX * toPlayerX + toPlayerZ * toPlayerZ);
        return playerDist <= WALL_FADE_PLAYER_RADIUS;
    }

    private void applyMaterialAlpha(Material material, float shade, float alpha) {
        ColorAttribute diffuse = (ColorAttribute) material.get(ColorAttribute.Diffuse);
        if (diffuse != null) {
            diffuse.color.set(shade, shade, shade, alpha);
        }
        if (alpha < 0.999f) {
            material.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, alpha));
        } else {
            material.remove(BlendingAttribute.Type);
        }
    }

    private void renderGroundOverlayPass(int[][] tileMap,
                                         int minTileX,
                                         int maxTileX,
                                         int minTileY,
                                         int maxTileY,
                                         float localPlayerX,
                                         float localPlayerY,
                                         String materialProfile) {
        if (tileMap == null || spriteSheet == null) {
            return;
        }

        overlayDecalPoolCursor = 0;
        float cameraX = camera.position.x;
        float cameraZ = camera.position.z;

        int playerTileX = clampTileX((int) Math.floor(localPlayerX));
        int playerTileY = clampTileY((int) Math.floor(localPlayerY));
        float playerCenterX = playerTileX + 0.5f;
        float playerCenterZ = playerTileY + 0.5f;
        float dirX = playerCenterX - cameraX;
        float dirZ = playerCenterZ - cameraZ;
        float dirLen2 = dirX * dirX + dirZ * dirZ;
        if (dirLen2 < 0.0001f) {
            dirLen2 = 0.0001f;
        }

        for (int y = minTileY; y <= maxTileY; y++) {
            for (int x = minTileX; x <= maxTileX; x++) {
                overlayTilesProcessedLastFrame++;
                int type = tileMap[x][y];
                boolean waterNorth = isTile(tileMap, x, y - 1, TILE_WATER);
                boolean waterSouth = isTile(tileMap, x, y + 1, TILE_WATER);
                boolean waterEast = isTile(tileMap, x + 1, y, TILE_WATER);
                boolean waterWest = isTile(tileMap, x - 1, y, TILE_WATER);
                float tileTopY = getTileTopY(tileMap, x, y);
                float baseOverlayY = type == TILE_WALL ? tileTopY + WALL_TOP_Y + WALL_OVERLAY_Y : tileTopY + OVERLAY_Y;

                if (type == TILE_WATER) {
                    TextureRegion shimmer = resolveAnimatedOverlayRegion("water_shimmer", materialProfile);
                    renderGroundOverlayDecal(x, y, tileTopY + WATER_OVERLAY_Y, shimmer, 0.18f);

                    if (waterSeed(x, y) % 7 == 0) {
                        TextureRegion sparkle = resolveAnimatedOverlayRegion("water_sparkle", materialProfile);
                        renderGroundOverlayDecal(x, y, tileTopY + WATER_OVERLAY_Y + 0.001f, sparkle, 0.10f);
                    }
                }

                if (isLandTile(type)) {
                    if (waterNorth) {
                        renderGroundOverlayDecal(x, y, baseOverlayY, resolveOverlayRegion("shore_wet_n", materialProfile), 0.15f);
                        renderGroundOverlayDecal(x, y, baseOverlayY + 0.001f, resolveOverlayRegion("shore_foam_n", materialProfile), 0.18f);
                        renderGroundOverlayDecal(x, y, baseOverlayY + 0.002f, resolveOverlayRegion("edge_shore_n", materialProfile), 1f);
                        renderGroundOverlayDecal(x, y, baseOverlayY + 0.003f, resolveOverlayRegion("ao_shore_n", materialProfile), 0.22f);
                    }
                    if (waterSouth) {
                        renderGroundOverlayDecal(x, y, baseOverlayY, resolveOverlayRegion("shore_wet_s", materialProfile), 0.15f);
                        renderGroundOverlayDecal(x, y, baseOverlayY + 0.001f, resolveOverlayRegion("shore_foam_s", materialProfile), 0.18f);
                        renderGroundOverlayDecal(x, y, baseOverlayY + 0.002f, resolveOverlayRegion("edge_shore_s", materialProfile), 1f);
                        renderGroundOverlayDecal(x, y, baseOverlayY + 0.003f, resolveOverlayRegion("ao_shore_s", materialProfile), 0.22f);
                    }
                    if (waterEast) {
                        renderGroundOverlayDecal(x, y, baseOverlayY, resolveOverlayRegion("shore_wet_e", materialProfile), 0.15f);
                        renderGroundOverlayDecal(x, y, baseOverlayY + 0.001f, resolveOverlayRegion("shore_foam_e", materialProfile), 0.18f);
                        renderGroundOverlayDecal(x, y, baseOverlayY + 0.002f, resolveOverlayRegion("edge_shore_e", materialProfile), 1f);
                        renderGroundOverlayDecal(x, y, baseOverlayY + 0.003f, resolveOverlayRegion("ao_shore_e", materialProfile), 0.22f);
                    }
                    if (waterWest) {
                        renderGroundOverlayDecal(x, y, baseOverlayY, resolveOverlayRegion("shore_wet_w", materialProfile), 0.15f);
                        renderGroundOverlayDecal(x, y, baseOverlayY + 0.001f, resolveOverlayRegion("shore_foam_w", materialProfile), 0.18f);
                        renderGroundOverlayDecal(x, y, baseOverlayY + 0.002f, resolveOverlayRegion("edge_shore_w", materialProfile), 1f);
                        renderGroundOverlayDecal(x, y, baseOverlayY + 0.003f, resolveOverlayRegion("ao_shore_w", materialProfile), 0.22f);
                    }
                }

                if (type == TILE_PATH) {
                    if (isTile(tileMap, x, y - 1, TILE_GRASS)) {
                        renderGroundOverlayDecal(x, y, baseOverlayY, resolveOverlayRegion("edge_path_grass_n", materialProfile), 1f);
                        renderGroundOverlayDecal(x, y, baseOverlayY + 0.001f, resolveOverlayRegion("ao_path_grass_n", materialProfile), 0.20f);
                    }
                    if (isTile(tileMap, x, y + 1, TILE_GRASS)) {
                        renderGroundOverlayDecal(x, y, baseOverlayY, resolveOverlayRegion("edge_path_grass_s", materialProfile), 1f);
                        renderGroundOverlayDecal(x, y, baseOverlayY + 0.001f, resolveOverlayRegion("ao_path_grass_s", materialProfile), 0.20f);
                    }
                    if (isTile(tileMap, x + 1, y, TILE_GRASS)) {
                        renderGroundOverlayDecal(x, y, baseOverlayY, resolveOverlayRegion("edge_path_grass_e", materialProfile), 1f);
                        renderGroundOverlayDecal(x, y, baseOverlayY + 0.001f, resolveOverlayRegion("ao_path_grass_e", materialProfile), 0.20f);
                    }
                    if (isTile(tileMap, x - 1, y, TILE_GRASS)) {
                        renderGroundOverlayDecal(x, y, baseOverlayY, resolveOverlayRegion("edge_path_grass_w", materialProfile), 1f);
                        renderGroundOverlayDecal(x, y, baseOverlayY + 0.001f, resolveOverlayRegion("ao_path_grass_w", materialProfile), 0.20f);
                    }
                }

                if (type == TILE_WALL) {
                    boolean wallFaded = shouldFadeWall(
                        x + 0.5f,
                        y + 0.5f,
                        cameraX,
                        cameraZ,
                        playerCenterX,
                        playerCenterZ,
                        dirX,
                        dirZ,
                        dirLen2
                    );
                    float wallAlpha = wallFaded ? WALL_FADE_ALPHA : 1f;
                    float aoAlpha = 0.24f * wallAlpha;
                    renderGroundOverlayDecal(x, y, baseOverlayY, resolveOverlayRegion("edge_wall_base", materialProfile), wallAlpha);
                    renderGroundOverlayDecal(x, y, baseOverlayY + 0.001f, resolveOverlayRegion("ao_wall_base", materialProfile), aoAlpha);

                    boolean wallNorth = isTile(tileMap, x, y - 1, TILE_WALL);
                    boolean wallSouth = isTile(tileMap, x, y + 1, TILE_WALL);
                    boolean wallEast = isTile(tileMap, x + 1, y, TILE_WALL);
                    boolean wallWest = isTile(tileMap, x - 1, y, TILE_WALL);
                    boolean wallNE = isTile(tileMap, x + 1, y - 1, TILE_WALL);
                    boolean wallNW = isTile(tileMap, x - 1, y - 1, TILE_WALL);
                    boolean wallSE = isTile(tileMap, x + 1, y + 1, TILE_WALL);
                    boolean wallSW = isTile(tileMap, x - 1, y + 1, TILE_WALL);

                    if (wallNorth && wallEast && !wallNE) {
                        renderGroundOverlayDecal(x, y, baseOverlayY + 0.002f, resolveOverlayRegion("ao_wall_inner_ne", materialProfile), aoAlpha);
                    }
                    if (wallNorth && wallWest && !wallNW) {
                        renderGroundOverlayDecal(x, y, baseOverlayY + 0.002f, resolveOverlayRegion("ao_wall_inner_nw", materialProfile), aoAlpha);
                    }
                    if (wallSouth && wallEast && !wallSE) {
                        renderGroundOverlayDecal(x, y, baseOverlayY + 0.002f, resolveOverlayRegion("ao_wall_inner_se", materialProfile), aoAlpha);
                    }
                    if (wallSouth && wallWest && !wallSW) {
                        renderGroundOverlayDecal(x, y, baseOverlayY + 0.002f, resolveOverlayRegion("ao_wall_inner_sw", materialProfile), aoAlpha);
                    }
                }

                int clutterSeed = clutterSeed(x, y);
                boolean nearWater = waterNorth || waterSouth || waterEast || waterWest;
                boolean nearWall = isTile(tileMap, x, y - 1, TILE_WALL)
                    || isTile(tileMap, x, y + 1, TILE_WALL)
                    || isTile(tileMap, x + 1, y, TILE_WALL)
                    || isTile(tileMap, x - 1, y, TILE_WALL);

                TextureRegion clutterRegion = null;
                if (type == TILE_GRASS && !nearWall && !nearWater && clutterSeed % 8 == 0) {
                    int variant = (clutterSeed % 3) + 1;
                    clutterRegion = resolveClutterRegion("clutter_grass_" + variant, materialProfile);
                } else if (type == TILE_PATH && clutterSeed % 14 == 0) {
                    int variant = (clutterSeed % 2) + 1;
                    clutterRegion = resolveClutterRegion("clutter_path_" + variant, materialProfile);
                } else if (type == TILE_SAND && clutterSeed % 12 == 0) {
                    int variant = (clutterSeed % 2) + 1;
                    clutterRegion = resolveClutterRegion("clutter_sand_" + variant, materialProfile);
                } else if (type != TILE_WATER && type != TILE_WALL && nearWater && clutterSeed % 18 == 0) {
                    clutterRegion = resolveClutterRegion("clutter_reeds_1", materialProfile);
                }
                renderGroundOverlayDecal(x, y, baseOverlayY + 0.004f, clutterRegion, 1f);
            }
        }

        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        overlayDecalBatch.flush();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void renderGroundOverlayDecal(int tileX,
                                          int tileY,
                                          float y,
                                          TextureRegion region,
                                          float alpha) {
        if (region == null || alpha <= 0f) {
            return;
        }
        Decal decal = obtainOverlayDecal(region, 1f, 1f);
        decal.setPosition(tileX + 0.5f, y, tileY + 0.5f);
        decal.setRotationX(-90f);
        decal.setRotationY(0f);
        decal.setRotationZ(0f);
        decal.setColor(1f, 1f, 1f, Math.max(0f, Math.min(1f, alpha)));
        overlayDecalBatch.add(decal);
    }

    private TextureRegion resolveOverlayRegion(String key, String materialProfile) {
        if (spriteSheet == null || key == null || key.isEmpty()) {
            return null;
        }
        String profileKey = profiledKey(key, materialProfile);
        if (profileKey != null) {
            TextureRegion region = spriteSheet.getTile(profileKey);
            if (region != null) {
                return region;
            }
        }
        return spriteSheet.getTile(key);
    }

    private TextureRegion resolveAnimatedOverlayRegion(String key, String materialProfile) {
        if (spriteSheet == null || key == null || key.isEmpty()) {
            return null;
        }
        String profileKey = profiledKey(key, materialProfile);
        if (profileKey != null) {
            com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> profileAnim = spriteSheet.getAnimation(profileKey);
            if (profileAnim != null) {
                return profileAnim.getKeyFrame(stateTime, true);
            }
            TextureRegion profileTile = spriteSheet.getTile(profileKey);
            if (profileTile != null) {
                return profileTile;
            }
        }

        com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> anim = spriteSheet.getAnimation(key);
        if (anim != null) {
            return anim.getKeyFrame(stateTime, true);
        }
        return spriteSheet.getTile(key);
    }

    private TextureRegion resolveClutterRegion(String key, String materialProfile) {
        if (spriteSheet == null || key == null || key.isEmpty()) {
            return null;
        }
        String profileKey = profiledKey(key, materialProfile);
        if (profileKey != null) {
            com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> profileAnim = spriteSheet.getAnimation(profileKey + "_idle");
            if (profileAnim != null) {
                return profileAnim.getKeyFrame(stateTime, true);
            }
            TextureRegion profileTile = spriteSheet.getTile(profileKey);
            if (profileTile != null) {
                return profileTile;
            }
        }

        com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> anim = spriteSheet.getAnimation(key + "_idle");
        if (anim != null) {
            return anim.getKeyFrame(stateTime, true);
        }
        return spriteSheet.getTile(key);
    }

    private boolean isLandTile(int type) {
        return type == TILE_GRASS || type == TILE_PATH || type == TILE_SAND;
    }

    private boolean isTile(int[][] tileMap, int x, int y, int expected) {
        if (tileMap == null) {
            return expected == 0;
        }
        if (!isInBounds(x, y)) {
            return false;
        }
        return tileMap[x][y] == expected;
    }

    private int clutterSeed(int x, int y) {
        int h = (x * 73856093) ^ (y * 19349663) ^ 0x9e3779b9;
        h ^= (h >>> 13);
        h *= 1274126177;
        h ^= (h >>> 16);
        return h & Integer.MAX_VALUE;
    }

    private int waterSeed(int x, int y) {
        int h = (x * 83492791) ^ (y * 19351301) ^ 0x51f2e3d7;
        h ^= (h >>> 13);
        h *= 1274126177;
        h ^= (h >>> 16);
        return h & Integer.MAX_VALUE;
    }

    private int countAtlasVariants(String baseKey) {
        Integer cached = variantCountCache.get(baseKey);
        if (cached != null) {
            return cached;
        }
        int count = 0;
        while (count < MAX_VARIANTS_SCAN) {
            if (spriteSheet.getVariantTile(baseKey, count) == null) {
                break;
            }
            count++;
        }
        variantCountCache.put(baseKey, count);
        return count;
    }

    private String normalizeMaterialProfile(String materialProfile) {
        if (materialProfile == null) {
            return "neutral";
        }
        String trimmed = materialProfile.trim();
        if (trimmed.isEmpty()) {
            return "neutral";
        }
        return trimmed.toLowerCase();
    }

    private String profiledKey(String key, String materialProfile) {
        String profile = normalizeMaterialProfile(materialProfile);
        if ("neutral".equals(profile)) {
            return null;
        }
        return key + "_" + profile;
    }

    private String cliffFaceKeyForTileType(int type, String materialProfile) {
        if (spriteSheet == null) {
            return null;
        }

        String preferredBase = switch (type) {
            case 0 -> "cliff_face_grass";
            case 2 -> "cliff_face_path";
            case 4 -> "cliff_face_sand";
            default -> "cliff_face_neutral";
        };

        String preferredProfiled = profiledKey(preferredBase, materialProfile);
        if (preferredProfiled != null && spriteSheet.getTile(preferredProfiled) != null) {
            return preferredProfiled;
        }
        if (spriteSheet.getTile(preferredBase) != null) {
            return preferredBase;
        }

        String neutralProfiled = profiledKey("cliff_face_neutral", materialProfile);
        if (neutralProfiled != null && spriteSheet.getTile(neutralProfiled) != null) {
            return neutralProfiled;
        }
        if (spriteSheet.getTile("cliff_face_neutral") != null) {
            return "cliff_face_neutral";
        }
        return null;
    }

    private int computeOverlayRadius() {
        float estimate = camera.position.y * 1.8f + 14f;
        int radius = (int) Math.ceil(estimate);
        radius = Math.max(MIN_OVERLAY_RADIUS, radius);
        return Math.min(MAX_OVERLAY_RADIUS, radius);
    }

    private float getTileTopY(int[][] tileMap, int tileX, int tileY) {
        int level = getHeightLevel(tileMap, tileX, tileY);
        return level * terrainHeightStep;
    }

    private int getHeightLevel(int[][] tileMap, int tileX, int tileY) {
        if (!isInBounds(tileX, tileY)) {
            return 0;
        }
        if (tileMap != null && isTile(tileMap, tileX, tileY, 1)) {
            return 0;
        }
        if (terrainHeightLevels == null
            || tileX >= terrainHeightLevels.length
            || terrainHeightLevels[tileX] == null
            || tileY >= terrainHeightLevels[tileX].length) {
            return 0;
        }
        return Math.max(0, terrainHeightLevels[tileX][tileY]);
    }

    private void addTerrainStepFace(TextureRegion region,
                                    String partId,
                                    float x0,
                                    float y0,
                                    float z0,
                                    float x1,
                                    float y1,
                                    float z1,
                                    float x2,
                                    float y2,
                                    float z2,
                                    float x3,
                                    float y3,
                                    float z3,
                                    float nx,
                                    float ny,
                                    float nz) {
        Material material = createWallFaceMaterial(region);
        MeshPartBuilder part = modelBuilder.part(
            partId,
            GL20.GL_TRIANGLES,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates,
            material
        );
        addVerticalFace(part, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, region, nx, ny, nz);
    }

    private int clampTileX(int value) {
        if (value < 0) {
            return 0;
        }
        if (value >= mapWidth) {
            return mapWidth - 1;
        }
        return value;
    }

    private int clampTileY(int value) {
        if (value < 0) {
            return 0;
        }
        if (value >= mapHeight) {
            return mapHeight - 1;
        }
        return value;
    }

    private void updateMapBounds(int[][] tileMap) {
        if (tileMap == null || tileMap.length == 0) {
            mapWidth = 0;
            mapHeight = 0;
            return;
        }
        mapWidth = tileMap.length;
        mapHeight = tileMap[0] == null ? 0 : tileMap[0].length;
    }

    private boolean isInBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < mapWidth && y < mapHeight;
    }

    private Color tileTypeFallbackColor(int type) {
        return switch (type) {
            case 1 -> new Color(0.20f, 0.40f, 0.66f, 1f); // water
            case 2 -> new Color(0.68f, 0.58f, 0.42f, 1f); // path
            case 3 -> new Color(0.56f, 0.52f, 0.48f, 1f); // wall
            case 4 -> new Color(0.90f, 0.79f, 0.52f, 1f); // sand
            default -> new Color(0.27f, 0.60f, 0.13f, 1f); // grass
        };
    }

    private static String tileKey(int type) {
        return switch (type) {
            case 1 -> "tile_water";
            case 2 -> "tile_path";
            case 3 -> "tile_wall";
            case 4 -> "tile_sand";
            default -> "tile_grass";
        };
    }

    private TextureRegion resolveTileRegion(int type, int tileX, int tileY, String materialProfile) {
        if (spriteSheet == null) {
            return null;
        }
        String key = tileKey(type);

        if (type == 1) {
            com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> anim = spriteSheet.getAnimation("tile_water");
            if (anim != null) {
                return anim.getKeyFrame(stateTime, true);
            }
            return spriteSheet.getTile("tile_water");
        }

        String profile = normalizeMaterialProfile(materialProfile);
        if (profile != null) {
            TextureRegion profileVariant = resolveVariantAwareTile(key + "_" + profile, tileX, tileY, type);
            if (profileVariant != null) {
                return profileVariant;
            }
        }

        return resolveVariantAwareTile(key, tileX, tileY, type);
    }

    private TextureRegion resolveVariantAwareTile(String key, int x, int y, int type) {
        if (spriteSheet == null || key == null || key.isEmpty()) {
            return null;
        }

        SpriteSheet.SpriteMeta meta = spriteSheet.getMeta(key);
        int seed = tileVariantSeed(x, y, type);
        if (meta != null && meta.variantCount() > 0) {
            return spriteSheet.getDeterministicVariantTile(key, meta.variantCount(), seed);
        }

        int variantCount = countAtlasVariants(key);
        if (variantCount > 0) {
            return spriteSheet.getDeterministicVariantTile(key, variantCount, seed);
        }

        return spriteSheet.getTile(key);
    }

    private int tileVariantSeed(int x, int y, int type) {
        int h = (x * 92837111) ^ (y * 689287499) ^ (type * 1237);
        h ^= (h >>> 13);
        h *= 1274126177;
        h ^= (h >>> 16);
        return h & Integer.MAX_VALUE;
    }

    private long chunkKey(int chunkX, int chunkY) {
        return ((long) chunkX << 32) | (chunkY & 0xffffffffL);
    }

    private Decal obtainDecal(TextureRegion region, float width, float height) {
        Decal decal;
        if (decalPoolCursor < decalPool.size()) {
            decal = decalPool.get(decalPoolCursor);
            decal.setTextureRegion(region);
            decal.setDimensions(width, height);
            decal.setColor(1f, 1f, 1f, 1f);
        } else {
            decal = Decal.newDecal(width, height, region, true);
            decalPool.add(decal);
        }
        decalPoolCursor++;
        return decal;
    }

    private Decal obtainOverlayDecal(TextureRegion region, float width, float height) {
        Decal decal;
        if (overlayDecalPoolCursor < overlayDecalPool.size()) {
            decal = overlayDecalPool.get(overlayDecalPoolCursor);
            decal.setTextureRegion(region);
            decal.setDimensions(width, height);
            decal.setColor(1f, 1f, 1f, 1f);
        } else {
            decal = Decal.newDecal(width, height, region, true);
            overlayDecalPool.add(decal);
        }
        overlayDecalPoolCursor++;
        return decal;
    }

    private ModelInstance obtainStaticPropInstance(String key, Model model) {
        ArrayList<ModelInstance> pool = staticPropInstancePool.computeIfAbsent(key, k -> new ArrayList<>());
        int cursor = staticPropInstanceCursor.getOrDefault(key, 0);
        ModelInstance instance;
        if (cursor < pool.size()) {
            instance = pool.get(cursor);
        } else {
            instance = new ModelInstance(model);
            pool.add(instance);
        }
        staticPropInstanceCursor.put(key, cursor + 1);
        return instance;
    }

    private ModelInstance obtainActorModelInstance(String key, Model model) {
        ArrayList<ModelInstance> pool = actorModelInstancePool.computeIfAbsent(key, k -> new ArrayList<>());
        int cursor = actorModelInstanceCursor.getOrDefault(key, 0);
        ModelInstance instance;
        if (cursor < pool.size()) {
            instance = pool.get(cursor);
        } else {
            instance = new ModelInstance(model);
            for (Material m : instance.materials) m.set(IntAttribute.createCullFace(GL20.GL_BACK));
            pool.add(instance);
        }
        actorModelInstanceCursor.put(key, cursor + 1);
        return instance;
    }
}
