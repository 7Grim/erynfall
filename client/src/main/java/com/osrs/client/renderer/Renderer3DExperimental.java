package com.osrs.client.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
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

import java.util.HashMap;
import java.util.Map;

public class Renderer3DExperimental {
    private static final int MAP_WIDTH = 104;
    private static final int MAP_HEIGHT = 104;
    private static final int CHUNK_SIZE = 16;

    private final PerspectiveCamera camera;
    private final ModelBatch modelBatch;
    private final DecalBatch decalBatch;
    private final Environment environment;
    private final ModelBuilder modelBuilder = new ModelBuilder();
    private final Plane groundPlane = new Plane(new Vector3(0f, 1f, 0f), 0f);

    private SpriteSheet spriteSheet;
    private float stateTime = 0f;

    private final Map<Long, ModelInstance> terrainChunks = new HashMap<>();
    private final Map<Long, Model> terrainChunkModels = new HashMap<>();

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

        if (tileMap == null || spriteSheet == null) {
            return;
        }

        int chunksX = (MAP_WIDTH + CHUNK_SIZE - 1) / CHUNK_SIZE;
        int chunksY = (MAP_HEIGHT + CHUNK_SIZE - 1) / CHUNK_SIZE;
        for (int chunkY = 0; chunkY < chunksY; chunkY++) {
            for (int chunkX = 0; chunkX < chunksX; chunkX++) {
                Model model = buildChunkModel(tileMap, chunkX, chunkY);
                if (model == null) {
                    continue;
                }
                long key = chunkKey(chunkX, chunkY);
                terrainChunkModels.put(key, model);
                terrainChunks.put(key, new ModelInstance(model));
            }
        }
    }

    public void renderTerrain(int[][] tileMap) {
        if (terrainChunks.isEmpty() && tileMap != null) {
            rebuildTerrain(tileMap);
        }
        if (terrainChunks.isEmpty()) {
            return;
        }

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
        if (region == null) {
            return;
        }
        Decal decal = Decal.newDecal(width, height, region, true);
        decal.setPosition(tileX + 0.5f, height * 0.5f, tileY + 0.5f);
        decal.lookAt(camera.position, camera.up);
        Color c = decal.getColor();
        decal.setColor(c.r, c.g, c.b, Math.max(0f, Math.min(1f, alpha)));
        decalBatch.add(decal);
    }

    public void flushEntityBillboards() {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        decalBatch.flush();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
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
        decalBatch.dispose();
        modelBatch.dispose();
    }

    private Model buildChunkModel(int[][] tileMap, int chunkX, int chunkY) {
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

                Material material = new Material(TextureAttribute.createDiffuse(region.getTexture()));
                MeshPartBuilder part = modelBuilder.part(
                    "tile_" + x + "_" + y,
                    GL20.GL_TRIANGLES,
                    VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates,
                    material
                );

                float x0 = x;
                float z0 = y;
                float x1 = x + 1f;
                float z1 = y + 1f;
                float y0 = 0f;

                MeshPartBuilder.VertexInfo v00 = vertex(x0, y0, z0, 0f, 1f, 0f, region.getU(), region.getV2());
                MeshPartBuilder.VertexInfo v10 = vertex(x1, y0, z0, 0f, 1f, 0f, region.getU2(), region.getV2());
                MeshPartBuilder.VertexInfo v11 = vertex(x1, y0, z1, 0f, 1f, 0f, region.getU2(), region.getV());
                MeshPartBuilder.VertexInfo v01 = vertex(x0, y0, z1, 0f, 1f, 0f, region.getU(), region.getV());
                part.rect(v01, v11, v10, v00);
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
}
