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
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

    private final PerspectiveCamera camera;
    private final ModelBatch modelBatch;
    private final DecalBatch decalBatch;
    private final Environment environment;
    private final ModelBuilder modelBuilder = new ModelBuilder();
    private final Plane groundPlane = new Plane(new Vector3(0f, 1f, 0f), 0f);
    private final ArrayList<Decal> decalPool = new ArrayList<>();
    private int decalPoolCursor = 0;
    private final Texture fallbackBillboardTexture;
    private final TextureRegion fallbackBillboardRegion;

    private SpriteSheet spriteSheet;
    private float stateTime = 0f;

    private final Map<Long, ModelInstance> terrainChunks = new HashMap<>();
    private final Map<Long, Model> terrainChunkModels = new HashMap<>();
    private final Map<Long, ArrayList<WallMaterialBinding>> wallMaterialsByChunk = new HashMap<>();

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
    }

    public void update(float delta) {
        stateTime += delta;
    }

    public void rebuildTerrain(int[][] tileMap) {
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
                Model model = buildChunkModel(tileMap, chunkX, chunkY, wallBindings);
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
        if (terrainChunks.isEmpty() && tileMap != null) {
            rebuildTerrain(tileMap);
        }
        if (terrainChunks.isEmpty()) {
            return;
        }

        updateWallOcclusion(localPlayerX, localPlayerY);

        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);

        modelBatch.begin(camera);
        for (ModelInstance instance : terrainChunks.values()) {
            modelBatch.render(instance, environment);
        }
        modelBatch.end();

        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
    }

    public void renderEntityBillboard(float tileX,
                                      float tileY,
                                      TextureRegion region,
                                      float width,
                                      float height,
                                      float alpha) {
        renderEntityBillboard(tileX, tileY, region, width, height, 1f, 1f, 1f, alpha);
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
        TextureRegion effectiveRegion = region != null ? region : fallbackBillboardRegion;
        if (effectiveRegion == null) {
            return;
        }
        Decal decal = obtainDecal(effectiveRegion, width, height);
        decal.setPosition(tileX + 0.5f, height * 0.5f, tileY + 0.5f);
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
        for (Model model : terrainChunkModels.values()) {
            model.dispose();
        }
        terrainChunkModels.clear();
        terrainChunks.clear();
        wallMaterialsByChunk.clear();
        fallbackBillboardTexture.dispose();
        decalBatch.dispose();
        modelBatch.dispose();
    }

    private Model buildChunkModel(int[][] tileMap,
                                  int chunkX,
                                  int chunkY,
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
                TextureRegion region = resolveTileRegion(type, x, y);
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

    private TextureRegion resolveTileRegion(int type, int tileX, int tileY) {
        if (spriteSheet == null) {
            return null;
        }
        String key = switch (type) {
            case 1 -> "tile_water";
            case 2 -> "tile_path";
            case 3 -> "tile_wall";
            case 4 -> "tile_sand";
            default -> "tile_grass";
        };

        if (type == 1) {
            com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> anim = spriteSheet.getAnimation(key);
            if (anim != null) {
                return anim.getKeyFrame(stateTime, true);
            }
            return spriteSheet.getTile(key);
        }

        SpriteSheet.SpriteMeta meta = spriteSheet.getMeta(key);
        if (meta != null && meta.variantCount() > 0) {
            int seed = tileVariantSeed(tileX, tileY, type);
            return spriteSheet.getDeterministicVariantTile(key, meta.variantCount(), seed);
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
}
