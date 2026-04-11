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
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.osrs.shared.EquipmentSlot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Renderer3DExperimental {
    private static final int MAP_WIDTH = 104;
    private static final int MAP_HEIGHT = 104;
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
    private static final int MIN_OVERLAY_RADIUS = 34;
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

    private final PerspectiveCamera camera;
    private final ModelBatch modelBatch;
    private final DecalBatch decalBatch;
    private final DecalBatch overlayDecalBatch;
    private final Environment environment;
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
    private boolean staticPropPassActive = false;
    private boolean actorModelPassActive = false;

    private ModelLibrary modelLibrary;
    private ModelInstance localPlayerAnimatedInstance;
    private AnimationController localPlayerAnimationController;
    private String currentLocalPlayerClip = "";
    private final Map<Integer, ModelInstance> npcAnimatedInstances = new HashMap<>();
    private final Map<Integer, AnimationController> npcAnimationControllers = new HashMap<>();
    private final Map<Integer, String> npcBaseKeyByEntityId = new HashMap<>();
    private final Map<Integer, String> currentNpcClipByEntityId = new HashMap<>();
    private final Set<Long> missingEquipmentWarnings = new HashSet<>();

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
        camera = new PerspectiveCamera(67f, Math.max(1, screenW), Math.max(1, screenH));
        camera.near = 0.1f;
        camera.far = 600f;
        camera.position.set(52f, 12f, 62f);
        camera.lookAt(52f, 0f, 52f);
        camera.up.set(0f, 1f, 0f);
        camera.update();

        modelBatch = new ModelBatch();
        decalBatch = new DecalBatch(new CameraGroupStrategy(camera));
        overlayDecalBatch = new DecalBatch(new CameraGroupStrategy(camera));

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 1f, 1f, 1f, 1f));

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
        localPlayerAnimatedInstance = null;
        localPlayerAnimationController = null;
        currentLocalPlayerClip = "";
        npcAnimatedInstances.clear();
        npcAnimationControllers.clear();
        npcBaseKeyByEntityId.clear();
        currentNpcClipByEntityId.clear();
        missingEquipmentWarnings.clear();
    }

    public boolean hasStaticPropModel(String key) {
        return modelLibrary != null && modelLibrary.hasModel(key);
    }

    public void update(float delta) {
        stateTime += delta;
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
            return;
        }

        int chunksX = (MAP_WIDTH + CHUNK_SIZE - 1) / CHUNK_SIZE;
        int chunksY = (MAP_HEIGHT + CHUNK_SIZE - 1) / CHUNK_SIZE;
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

    public void renderTerrain(int[][] tileMap, float localPlayerX, float localPlayerY) {
        renderTerrain(tileMap, localPlayerX, localPlayerY, activeMaterialProfile);
    }

    public void renderTerrain(int[][] tileMap,
                              float localPlayerX,
                              float localPlayerY,
                              String materialProfile) {
        String normalizedProfile = normalizeMaterialProfile(materialProfile);
        if (!normalizedProfile.equals(activeMaterialProfile)) {
            rebuildTerrain(tileMap, normalizedProfile);
        } else if (terrainChunks.isEmpty() && tileMap != null) {
            rebuildTerrain(tileMap, normalizedProfile);
        }
        if (terrainChunks.isEmpty()) {
            return;
        }

        updateWallOcclusion(localPlayerX, localPlayerY);

        int centerTileX = clampTile((int) Math.floor(localPlayerX));
        int centerTileY = clampTile((int) Math.floor(localPlayerY));
        int overlayRadius = computeOverlayRadius();
        int minTileX = Math.max(0, centerTileX - overlayRadius);
        int maxTileX = Math.min(MAP_WIDTH - 1, centerTileX + overlayRadius);
        int minTileY = Math.max(0, centerTileY - overlayRadius);
        int maxTileY = Math.min(MAP_HEIGHT - 1, centerTileY + overlayRadius);

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
                }
            }
        }
        modelBatch.end();

        renderGroundOverlayPass(tileMap, minTileX, maxTileX, minTileY, maxTileY, localPlayerX, localPlayerY, normalizedProfile);

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
        decal.setPosition(tileX + 0.5f, baseY + height * 0.5f, tileY + 0.5f);
        decal.lookAt(camera.position, camera.up);
        decal.setColor(
            Math.max(0f, Math.min(1f, tintR)),
            Math.max(0f, Math.min(1f, tintG)),
            Math.max(0f, Math.min(1f, tintB)),
            Math.max(0f, Math.min(1f, alpha))
        );
        decalBatch.add(decal);
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
        instance.transform.idt();
        instance.transform.translate(tileX + 0.5f, 0f, tileY + 0.5f);
        float effectiveScale = scaleOverride > 0f ? scaleOverride : meta.scale();
        if (effectiveScale > 0f && Math.abs(effectiveScale - 1f) > 0.0001f) {
            instance.transform.scale(effectiveScale, effectiveScale, effectiveScale);
        }
        if (Math.abs(rotationYDegrees) > 0.0001f) {
            instance.transform.rotate(Vector3.Y, rotationYDegrees);
        }
        if (!"tile-center".equals(meta.origin())) {
            instance.transform.translate(0f, 0f, 0f);
        }

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
        if (clampedAlpha < 0.999f) {
            Gdx.gl.glDepthMask(true);
        }

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
        instance.transform.idt();
        instance.transform.translate(tileX + 0.5f, 0f, tileY + 0.5f);
        if (meta.scale() > 0f && Math.abs(meta.scale() - 1f) > 0.0001f) {
            instance.transform.scale(meta.scale(), meta.scale(), meta.scale());
        }
        if (Math.abs(rotationYDegrees) > 0.0001f) {
            instance.transform.rotate(Vector3.Y, rotationYDegrees);
        }

        modelBatch.render(instance, environment);

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
        baseInstance.transform.idt();
        baseInstance.transform.translate(tileX + 0.5f, 0f, tileY + 0.5f);
        if (Math.abs(rotationYDegrees) > 0.0001f) {
            baseInstance.transform.rotate(Vector3.Y, rotationYDegrees);
        }
        if (Math.abs(baseScale - 1f) > 0.0001f) {
            baseInstance.transform.scale(baseScale, baseScale, baseScale);
        }
        baseInstance.calculateTransforms();
        modelBatch.render(baseInstance, environment);

        renderPlayerEquipmentAttachments(baseInstance, tileX, tileY, rotationYDegrees, baseScale, equippedItemIds);

        if (ownsPass) {
            endActorModelPass();
        }
        return true;
    }

    public boolean renderAnimatedLocalPlayer(String stateKey,
                                             float tileX,
                                             float tileY,
                                             float rotationYDegrees,
                                             int[] equippedItemIds,
                                             float delta) {
        if (!ensureLocalPlayerAnimatedModelLoaded()) {
            return false;
        }

        String clipName = normalizePlayerClipName(stateKey);
        if (localPlayerAnimatedInstance.getAnimation(clipName) == null) {
            return false;
        }

        boolean ownsPass = !actorModelPassActive;
        if (ownsPass) {
            beginActorModelPass();
        }

        ModelLibrary.ModelMeta baseMeta = modelLibrary.getMeta("player_base");
        float baseScale = baseMeta != null && baseMeta.scale() > 0f ? baseMeta.scale() : 1f;
        try {
            if (!clipName.equals(currentLocalPlayerClip)) {
                localPlayerAnimationController.setAnimation(clipName, -1);
                currentLocalPlayerClip = clipName;
            }
            localPlayerAnimationController.update(Math.max(0f, delta));

            localPlayerAnimatedInstance.transform.idt();
            localPlayerAnimatedInstance.transform.translate(tileX + 0.5f, 0f, tileY + 0.5f);
            if (Math.abs(rotationYDegrees) > 0.0001f) {
                localPlayerAnimatedInstance.transform.rotate(Vector3.Y, rotationYDegrees);
            }
            if (Math.abs(baseScale - 1f) > 0.0001f) {
                localPlayerAnimatedInstance.transform.scale(baseScale, baseScale, baseScale);
            }
            localPlayerAnimatedInstance.calculateTransforms();
            modelBatch.render(localPlayerAnimatedInstance, environment);

            renderPlayerEquipmentAttachments(localPlayerAnimatedInstance, tileX, tileY, rotationYDegrees, baseScale, equippedItemIds);
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
        if (instance.getAnimation(clipName) == null) {
            return false;
        }

        boolean ownsPass = !actorModelPassActive;
        if (ownsPass) {
            beginActorModelPass();
        }

        ModelLibrary.ModelMeta baseMeta = modelLibrary.getMeta(baseKey);
        float baseScale = baseMeta != null && baseMeta.scale() > 0f ? baseMeta.scale() : 1f;
        try {
            String currentClip = currentNpcClipByEntityId.getOrDefault(entityId, "");
            if (!clipName.equals(currentClip)) {
                controller.setAnimation(clipName, -1);
                currentNpcClipByEntityId.put(entityId, clipName);
            }
            controller.update(Math.max(0f, delta));

            instance.transform.idt();
            instance.transform.translate(tileX + 0.5f, 0f, tileY + 0.5f);
            if (Math.abs(rotationYDegrees) > 0.0001f) {
                instance.transform.rotate(Vector3.Y, rotationYDegrees);
            }
            if (Math.abs(baseScale - 1f) > 0.0001f) {
                instance.transform.scale(baseScale, baseScale, baseScale);
            }
            instance.calculateTransforms();

            renderInstanceWithOptionalAlpha(instance, alpha);
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
            return;
        }
        npcAnimatedInstances.keySet().retainAll(activeEntityIds);
        npcAnimationControllers.keySet().retainAll(activeEntityIds);
        npcBaseKeyByEntityId.keySet().retainAll(activeEntityIds);
        currentNpcClipByEntityId.keySet().retainAll(activeEntityIds);
    }

    private void renderPlayerEquipmentAttachments(ModelInstance baseInstance,
                                                  float tileX,
                                                  float tileY,
                                                  float rotationYDegrees,
                                                  float baseScale,
                                                  int[] equippedItemIds) {
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
            Matrix4 anchorTransform = findActorAnchorTransform(baseInstance, equipMeta.anchorName());
            if (anchorTransform != null) {
                equipInstance.transform.set(anchorTransform);
                equipInstance.transform.translate(
                    equipMeta.offsetX() - defaultAnchorOffsetXForSlot(slot),
                    equipMeta.offsetY() - defaultAnchorOffsetYForSlot(slot),
                    equipMeta.offsetZ() - defaultAnchorOffsetZForSlot(slot)
                );
            } else {
                equipInstance.transform.translate(tileX + 0.5f, 0f, tileY + 0.5f);
                if (Math.abs(rotationYDegrees) > 0.0001f) {
                    equipInstance.transform.rotate(Vector3.Y, rotationYDegrees);
                }
                if (Math.abs(baseScale - 1f) > 0.0001f) {
                    equipInstance.transform.scale(baseScale, baseScale, baseScale);
                }
                equipInstance.transform.translate(equipMeta.offsetX(), equipMeta.offsetY(), equipMeta.offsetZ());
            }
            if (Math.abs(equipMeta.rotX()) > 0.0001f) {
                equipInstance.transform.rotate(Vector3.X, equipMeta.rotX());
            }
            if (Math.abs(equipMeta.rotY()) > 0.0001f) {
                equipInstance.transform.rotate(Vector3.Y, equipMeta.rotY());
            }
            if (Math.abs(equipMeta.rotZ()) > 0.0001f) {
                equipInstance.transform.rotate(Vector3.Z, equipMeta.rotZ());
            }
            if (equipMeta.scale() > 0f && Math.abs(equipMeta.scale() - 1f) > 0.0001f) {
                equipInstance.transform.scale(equipMeta.scale(), equipMeta.scale(), equipMeta.scale());
            }
            modelBatch.render(equipInstance, environment);
        }
    }

    private boolean ensureLocalPlayerAnimatedModelLoaded() {
        if (modelLibrary == null || !modelLibrary.hasModel("player_base")) {
            localPlayerAnimatedInstance = null;
            localPlayerAnimationController = null;
            currentLocalPlayerClip = "";
            return false;
        }
        if (localPlayerAnimatedInstance != null && localPlayerAnimationController != null) {
            return true;
        }

        Model model = modelLibrary.getModel("player_base");
        if (model == null) {
            return false;
        }
        try {
            localPlayerAnimatedInstance = new ModelInstance(model);
            localPlayerAnimationController = new AnimationController(localPlayerAnimatedInstance);
            currentLocalPlayerClip = "";
            return true;
        } catch (Exception e) {
            localPlayerAnimatedInstance = null;
            localPlayerAnimationController = null;
            currentLocalPlayerClip = "";
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

    private void renderInstanceWithOptionalAlpha(ModelInstance instance, float alpha) {
        float clampedAlpha = Math.max(0f, Math.min(1f, alpha));
        if (clampedAlpha >= 0.999f) {
            modelBatch.render(instance, environment);
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
        modelBatch.render(instance, environment);
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
        Vector3 intersection = new Vector3();
        if (!Intersector.intersectRayPlane(ray, groundPlane, intersection)) {
            return new int[]{-1, -1};
        }
        int tileX = (int) Math.floor(intersection.x);
        int tileY = (int) Math.floor(intersection.z);
        if (tileX < 0 || tileY < 0 || tileX >= MAP_WIDTH || tileY >= MAP_HEIGHT) {
            return new int[]{-1, -1};
        }
        return new int[]{tileX, tileY};
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
        localPlayerAnimatedInstance = null;
        localPlayerAnimationController = null;
        currentLocalPlayerClip = "";
        npcAnimatedInstances.clear();
        npcAnimationControllers.clear();
        npcBaseKeyByEntityId.clear();
        currentNpcClipByEntityId.clear();
        missingEquipmentWarnings.clear();
        fallbackBillboardTexture.dispose();
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
        int endX = Math.min(MAP_WIDTH, startX + CHUNK_SIZE);
        int endY = Math.min(MAP_HEIGHT, startY + CHUNK_SIZE);

        modelBuilder.begin();
        boolean hasGeometry = false;

        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                int type = tileMap[x][y];
                TextureRegion region = resolveTileRegion(type, x, y, materialProfile);
                if (region == null) {
                    continue;
                }

                float x0 = x;
                float z0 = y;
                float x1 = x + 1f;
                float z1 = y + 1f;

                if (type == 3) {
                    Material topMaterial = createWallTopMaterial(region);
                    MeshPartBuilder topPart = modelBuilder.part(
                        "wall_top_" + x + "_" + y,
                        GL20.GL_TRIANGLES,
                        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates,
                        topMaterial
                    );
                    addTopQuad(topPart, x0, z0, x1, z1, WALL_TOP_Y, region);
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
                            x0, GROUND_Y, z0,
                            x1, GROUND_Y, z0,
                            x1, WALL_TOP_Y, z0,
                            x0, WALL_TOP_Y, z0,
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
                            x1, GROUND_Y, z1,
                            x0, GROUND_Y, z1,
                            x0, WALL_TOP_Y, z1,
                            x1, WALL_TOP_Y, z1,
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
                            x0, GROUND_Y, z1,
                            x0, GROUND_Y, z0,
                            x0, WALL_TOP_Y, z0,
                            x0, WALL_TOP_Y, z1,
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
                            x1, GROUND_Y, z0,
                            x1, GROUND_Y, z1,
                            x1, WALL_TOP_Y, z1,
                            x1, WALL_TOP_Y, z0,
                            region,
                            1f, 0f, 0f
                        );
                        wallBindings.add(new WallMaterialBinding(eastFaceMaterial, x, y, WALL_FACE_SHADE));
                    }
                } else {
                    Material material = new Material(TextureAttribute.createDiffuse(region.getTexture()));
                    MeshPartBuilder part = modelBuilder.part(
                        "tile_" + x + "_" + y,
                        GL20.GL_TRIANGLES,
                        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates,
                        material
                    );
                    addTopQuad(part, x0, z0, x1, z1, GROUND_Y, region);
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
        MeshPartBuilder.VertexInfo v00 = vertex(x0, y, z0, 0f, 1f, 0f, region.getU(), region.getV2());
        MeshPartBuilder.VertexInfo v10 = vertex(x1, y, z0, 0f, 1f, 0f, region.getU2(), region.getV2());
        MeshPartBuilder.VertexInfo v11 = vertex(x1, y, z1, 0f, 1f, 0f, region.getU2(), region.getV());
        MeshPartBuilder.VertexInfo v01 = vertex(x0, y, z1, 0f, 1f, 0f, region.getU(), region.getV());
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
        MeshPartBuilder.VertexInfo v0 = vertex(x0, y0, z0, nx, ny, nz, region.getU(), region.getV2());
        MeshPartBuilder.VertexInfo v1 = vertex(x1, y1, z1, nx, ny, nz, region.getU2(), region.getV2());
        MeshPartBuilder.VertexInfo v2 = vertex(x2, y2, z2, nx, ny, nz, region.getU2(), region.getV());
        MeshPartBuilder.VertexInfo v3 = vertex(x3, y3, z3, nx, ny, nz, region.getU(), region.getV());
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
        if (x < 0 || y < 0 || x >= MAP_WIDTH || y >= MAP_HEIGHT) {
            return false;
        }
        return tileMap[x][y] == 3;
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

        int playerTileX = clampTile((int) Math.floor(localPlayerX));
        int playerTileY = clampTile((int) Math.floor(localPlayerY));
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
                int type = tileMap[x][y];
                boolean waterNorth = isTile(tileMap, x, y - 1, 1);
                boolean waterSouth = isTile(tileMap, x, y + 1, 1);
                boolean waterEast = isTile(tileMap, x + 1, y, 1);
                boolean waterWest = isTile(tileMap, x - 1, y, 1);
                float baseOverlayY = type == 3 ? WALL_TOP_Y + WALL_OVERLAY_Y : GROUND_Y + OVERLAY_Y;

                if (type == 1) {
                    TextureRegion shimmer = resolveAnimatedOverlayRegion("water_shimmer", materialProfile);
                    renderGroundOverlayDecal(x, y, GROUND_Y + WATER_OVERLAY_Y, shimmer, 0.18f);

                    if (waterSeed(x, y) % 7 == 0) {
                        TextureRegion sparkle = resolveAnimatedOverlayRegion("water_sparkle", materialProfile);
                        renderGroundOverlayDecal(x, y, GROUND_Y + WATER_OVERLAY_Y + 0.001f, sparkle, 0.10f);
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

                if (type == 2) {
                    if (isTile(tileMap, x, y - 1, 0)) {
                        renderGroundOverlayDecal(x, y, baseOverlayY, resolveOverlayRegion("edge_path_grass_n", materialProfile), 1f);
                        renderGroundOverlayDecal(x, y, baseOverlayY + 0.001f, resolveOverlayRegion("ao_path_grass_n", materialProfile), 0.20f);
                    }
                    if (isTile(tileMap, x, y + 1, 0)) {
                        renderGroundOverlayDecal(x, y, baseOverlayY, resolveOverlayRegion("edge_path_grass_s", materialProfile), 1f);
                        renderGroundOverlayDecal(x, y, baseOverlayY + 0.001f, resolveOverlayRegion("ao_path_grass_s", materialProfile), 0.20f);
                    }
                    if (isTile(tileMap, x + 1, y, 0)) {
                        renderGroundOverlayDecal(x, y, baseOverlayY, resolveOverlayRegion("edge_path_grass_e", materialProfile), 1f);
                        renderGroundOverlayDecal(x, y, baseOverlayY + 0.001f, resolveOverlayRegion("ao_path_grass_e", materialProfile), 0.20f);
                    }
                    if (isTile(tileMap, x - 1, y, 0)) {
                        renderGroundOverlayDecal(x, y, baseOverlayY, resolveOverlayRegion("edge_path_grass_w", materialProfile), 1f);
                        renderGroundOverlayDecal(x, y, baseOverlayY + 0.001f, resolveOverlayRegion("ao_path_grass_w", materialProfile), 0.20f);
                    }
                }

                if (type == 3) {
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

                    boolean wallNorth = isTile(tileMap, x, y - 1, 3);
                    boolean wallSouth = isTile(tileMap, x, y + 1, 3);
                    boolean wallEast = isTile(tileMap, x + 1, y, 3);
                    boolean wallWest = isTile(tileMap, x - 1, y, 3);
                    boolean wallNE = isTile(tileMap, x + 1, y - 1, 3);
                    boolean wallNW = isTile(tileMap, x - 1, y - 1, 3);
                    boolean wallSE = isTile(tileMap, x + 1, y + 1, 3);
                    boolean wallSW = isTile(tileMap, x - 1, y + 1, 3);

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
                boolean nearWall = isTile(tileMap, x, y - 1, 3)
                    || isTile(tileMap, x, y + 1, 3)
                    || isTile(tileMap, x + 1, y, 3)
                    || isTile(tileMap, x - 1, y, 3);

                TextureRegion clutterRegion = null;
                if (type == 0 && !nearWall && !nearWater && clutterSeed % 8 == 0) {
                    int variant = (clutterSeed % 3) + 1;
                    clutterRegion = resolveClutterRegion("clutter_grass_" + variant, materialProfile);
                } else if (type == 2 && clutterSeed % 14 == 0) {
                    int variant = (clutterSeed % 2) + 1;
                    clutterRegion = resolveClutterRegion("clutter_path_" + variant, materialProfile);
                } else if (type == 4 && clutterSeed % 12 == 0) {
                    int variant = (clutterSeed % 2) + 1;
                    clutterRegion = resolveClutterRegion("clutter_sand_" + variant, materialProfile);
                } else if (type != 1 && type != 3 && nearWater && clutterSeed % 18 == 0) {
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
        return type == 0 || type == 2 || type == 4;
    }

    private boolean isTile(int[][] tileMap, int x, int y, int expected) {
        if (tileMap == null) {
            return expected == 0;
        }
        if (x < 0 || y < 0 || x >= MAP_WIDTH || y >= MAP_HEIGHT) {
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

    private int computeOverlayRadius() {
        float estimate = camera.position.y * 2.4f + 22f;
        return Math.max(MIN_OVERLAY_RADIUS, (int) Math.ceil(estimate));
    }

    private int clampTile(int value) {
        if (value < 0) {
            return 0;
        }
        if (value >= MAP_WIDTH) {
            return MAP_WIDTH - 1;
        }
        return value;
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
            pool.add(instance);
        }
        actorModelInstanceCursor.put(key, cursor + 1);
        return instance;
    }
}
