# Graphics Fidelity & Pipeline Refactor — Atomic Plan

Status: draft, in-progress

This document is the authoritative plan for moving Erynfall's world-authoring and rendering pipeline from hand-edited `scene.yaml` + tile-quad terrain to **Blender as the map editor** and **gdx-gltf `SceneManager`** as the runtime scene graph.

It is deliberately atomic. Each phase can land on its own without breaking the client, and each has an explicit rollback.

---

## Constraints & Facts

- Engine: libGDX 1.13.0 + gdx-gltf 2.2.1. **Not changing engines.** Every fidelity and pipeline improvement below lives inside what you already ship.
- Asset format reality (as of this document): **159 of 175 manifest entries are G3DJ without `source_blend`; 16 are GLB with `source_blend`.** The G3DJ loader is not dead and cannot be removed. Plan is designed around this.
- Server deployment: DigitalOcean VM, CI/CD from GitHub on push to `server/`. **Nothing in this plan touches `server/` source code or `shared/` protocol**, so server redeploys are not triggered.
- Blender is the authoring tool. The user runs Blender locally; the build/runtime pipeline consumes its output files. CI does not run Blender.
- Style guide: `docs/GRAPHICS_STYLE.md`. Low-poly, flat colors, no PBR. This plan respects that — baked AO stays inside the vertex-color lane.

---

## The Core Architectural Shift

**Before.**
- `art/worlds/<world>/scene.yaml` is the source of truth for both terrain and static-prop placement.
- Static props are a YAML list of `(key, x, y, rotation, scale)` consumed by `StaticPropLoader`.
- Terrain is rendered as per-tile quads with hard edge walls in `Renderer3DExperimental.buildChunkModel`.

**After.**
- `art/worlds/<world>/world.blend` is the authored source of truth for visual world composition.
- On export, it produces `art/worlds/<world>/world.glb` containing:
  - a `terrain_mesh` root node (single subdivided mesh, sculpted to match elevation, vertex-painted for grass/sand/path, optionally with baked AO in vertex colors);
  - a tree of named transforms (`<manifest_key>.<instance_id>`) marking where each prop goes.
- `scene.yaml` keeps only **gameplay data** the client needs alongside: `terrain_height` regions drive walkability chunk bounds; `terrain_visual` (walkable/water classification) drives pathing; `static_props` section is **retired** once `world.glb` is in place.
- Props are still loaded from their own `.g3dj`/`.glb` files via `ModelLibrary`. The world scene just tells the renderer where to instantiate them.
- Runtime gets `SceneManager` for per-node frustum culling, a clean sRGB-correct vertex color path, and optional shadow-map support without rewriting the rendering code that works today.

**Why this shape.**
The world file is a *transform tree + terrain mesh only*. It does not embed prop geometry. That means:
- You don't have to migrate 159 G3DJ models to `.blend` to start using the new pipeline.
- `world.blend` stays small — scene composition, not asset library.
- As you migrate individual props from G3DJ to GLB-with-source-blend, the Blender `world.blend` can start showing real geometry via linked collections for WYSIWYG authoring. Until then, named empties stand in.

---

## Phase 0 — Scaffolding (this PR)

Goal: land the plan, the Blender-side scripts, and the runtime infrastructure without changing any visible behavior. Once Phase 0 is in, a user can run three scripts locally and produce a working `world.glb`.

### Deliverables

1. This document (`docs/FIDELITY_REFACTOR_PLAN.md`).
2. `scripts/blender/generate_world_blend.py` — run once per world. Reads `art/worlds/<world>/scene.yaml` + `art/models/manifest.yaml`, writes `art/worlds/<world>/world.blend` containing:
   - a `terrain_mesh` object: subdivided plane sized to the world bounds, Z-displaced per `terrain_height` regions, vertex-colored per `terrain_visual` types.
   - a `props` collection: one named empty per `static_props` entry, name format `<manifest_key>.<instance_id>` where instance_id is a zero-padded 3-digit sequence.
   - For manifest entries with `source_blend` that exist on disk, the empty is replaced with a linked collection instance (WYSIWYG).
   - Idempotent: re-running rebuilds from scratch, never drifts.
3. `scripts/blender/export_world_glb.py` — opens `world.blend`, exports `world.glb` via Blender's glTF 2.0 exporter with transform tree preserved and custom node names retained. Matches export settings in `GRAPHICS_STYLE.md`.
4. `scripts/blender/bake_world_lighting.py` — Phase 2's bake step, landed now but only used when the user pulls the trigger. Cycles bake: combined direct + AO + shadow from a sun lamp, output to the terrain mesh's vertex color layer. Saves `world.blend` in place.
5. Runtime `WorldSceneGlb` loader class — new, does nothing visible yet.
6. Phase 4 "kill list" in this document (see bottom) — evidence-backed inventory of code that becomes removable after each phase, NOT deleted yet.

### Files touched

- New: `docs/FIDELITY_REFACTOR_PLAN.md`, `scripts/blender/*.py`, `client/src/main/java/com/osrs/client/world/WorldSceneGlb.java`, tests.
- Server: untouched.
- `shared/`: untouched.
- `client/pom.xml`: untouched in Phase 0.

### Validation

- Client compiles and runs identically to before.
- `mvn test -pl client` passes.
- `python3 scripts/blender/generate_world_blend.py --world=main_world --dry-run` prints the plan without running Blender.

### Rollback

Revert the PR. No runtime behavior changed.

---

## Phase 1 — World.glb as authoritative prop placement

Goal: switch static-prop placements to read from `world.glb` when present, while leaving `scene.yaml` as a fallback. At the end of Phase 1, moving a building in Blender shows up in-game.

### Deliverables

1. `StaticPropLoader` gets a `world.glb` fast path:
   - If `art/worlds/<world>/world.glb` exists, load its scene tree, walk nodes, extract placements, return.
   - Else, fall back to existing `scene.yaml` path exactly as today.
2. Convention parser: a node named `tree_oak.017` with world transform `T` becomes `StaticPropPlacement(key="tree_oak", x, y, rotationYDegrees, scale)` where `(x, y, rotation, scale)` are derived from `T`.
   - Translation XZ → tile (x, y), using `1 tile = 1 world unit` from the style guide.
   - Y-rotation (around world up) → `rotationYDegrees`.
   - Uniform scale → `scale`; non-uniform scale logs a warning and uses X.
3. `visibility_group` defaults to `"base"` when a node has no custom property. Future: Blender custom property `visibility_group = "roof"` or a `_roof` sub-collection → roof group.
4. User-facing migration guide in this document (below).

### Files touched

- `client/src/main/java/com/osrs/client/world/StaticPropLoader.java` — add world.glb path; preserve existing scene.yaml path.
- `client/src/main/java/com/osrs/client/world/WorldSceneGlb.java` — implementation (Phase 0 landed the stub).
- Tests.

### Validation

- If `world.glb` absent, client behaves identically to today.
- If `world.glb` present, prop placements come from it; `scene.yaml static_props` section is ignored.
- Unit tests cover: single prop, multiple instances of same key, rotation extraction, non-uniform scale warning, missing manifest key (logged + skipped), nested collections.
- In-engine: launch client, visually confirm props at same positions (diff test via screenshot or log of placements).

### Rollback

Delete `world.glb` from the repo. Client auto-falls-back to `scene.yaml`.

---

## Phase 2 — Authored terrain mesh + baked AO

Goal: replace the procedural tile-quad terrain with the terrain mesh authored in `world.blend`. Adds baked AO via Cycles vertex-color bake. This is the single biggest visual upgrade in the whole plan.

### Deliverables

1. `Renderer3DExperimental` gets a `terrain_mesh` fast path:
   - If the loaded `world.glb` contains a node named `terrain_mesh`, render it as the terrain. Skip `buildChunkModel`.
   - Else, build the tile-quad terrain as today.
2. Walkability (server-authoritative and client-side movement) **continues to come from `terrain_height` regions in `scene.yaml` + `terrain_visual.water` tiles**. Terrain mesh is visual-only.
3. `bake_world_lighting.py` run by user:
   - Opens `world.blend`.
   - Adds a `Sun` lamp with matching direction/intensity to the runtime `DirectionalLight`.
   - Bakes Combined (direct + indirect + AO + shadow) to the terrain mesh's `Col` vertex color layer.
   - Saves the file.
4. `export_world_glb.py` re-run by user — produces `world.glb` with baked vertex colors.
5. Shader change: nothing. The stock libGDX `DefaultShader` already reads vertex colors. This works on Day 1 with zero shader code.

### Files touched

- `client/src/main/java/com/osrs/client/renderer/Renderer3DExperimental.java` — terrain mesh fast path; `buildChunkModel` path preserved as fallback.
- No new Java files. Work is in the Blender pipeline.

### Validation

- Without `terrain_mesh` node: terrain renders as today (tile quads). No regression.
- With `terrain_mesh` node: render the authored mesh. Compare screenshot side-by-side.
- Walkability unchanged — server and client movement logic must not shift by even one tile. Regression tested by spawning at known tiles and walking along `terrain_height` boundaries.
- FPS at spawn: measure before/after. Authored mesh should be *cheaper* than per-tile quad generation since it's one mesh vs 30 chunks.

### Rollback

Remove the `terrain_mesh` node from `world.blend` and re-export. Client auto-falls-back to tile-quad path.

### Dependencies on user Blender work

User must:
1. Open `world.blend`, sculpt/paint the terrain mesh to their satisfaction.
2. Run `scripts/blender/bake_world_lighting.py --world=main_world` (Blender CLI).
3. Run `scripts/blender/export_world_glb.py --world=main_world` (Blender CLI).
4. Commit the updated `world.glb`.

These are GUI + CLI steps the assistant cannot perform inside a sandbox. The scripts land in Phase 0.

---

## Phase 3 — SceneManager, shadows, fog, sRGB

Goal: unlock the parts of gdx-gltf already on the classpath. Add directional shadow map. Add exponential fog. Confirm sRGB-correct vertex colors.

### Deliverables

1. Replace the hand-rolled `ModelBatch` path for **static props only** with `net.mgsx.gltf.scene3d.scene.SceneManager`:
   - One `SceneManager` owns all static prop `Scene` objects.
   - Frustum culling is handled by `SceneManager` per-frame, replacing the custom logic.
   - Actor / animated model rendering stays on the existing path for now — animation integration with `SceneManager` is a follow-up.
2. Directional shadow map:
   - Add `DirectionalShadowLight` (or gdx-gltf's `CascadeShadowMap` if warranted by map scale) configured to match the world's sun.
   - One cascade, 2048² depth map. Props + terrain cast shadows; actors receive.
   - Performance target: < 1 ms extra frame time on a modest GPU.
3. Exponential distance fog:
   - Small shader modification (or gdx-gltf built-in fog if the version supports it) — 20-line change.
   - Color from scene's sky color. Density configured per world.
4. sRGB-correct vertex colors:
   - gdx-gltf's `PBRShaderConfig` handles this correctly when using its shader; stock `DefaultShader` does not. Switching to `SceneManager` gets this for free.
   - Validate: ramp from black-to-white in a test prop should look linear, not muddy.

### Files touched

- `client/src/main/java/com/osrs/client/renderer/Renderer3DExperimental.java` — introduce `SceneManager` for prop pass; add shadow light config; add fog uniform wiring.
- `client/pom.xml` — no new deps (all inside gdx-gltf 2.2.1).
- Tests — `SceneManager` integration smoke test.

### Validation

- Visual regression: before/after screenshots. Expected: cleaner colors, soft shadows on terrain and buildings, distance fade instead of hard cutoff.
- Performance: frame time must not increase by more than 1.5 ms average in the spawn view.
- No test regressions in `mvn test -pl client`.

### Rollback

Feature-flag the `SceneManager` path via a `LaunchOptions` boolean. Disable flag, revert to hand-rolled `ModelBatch` path.

---

## Phase 4 — Dead code sweep

Goal: remove the code that Phase 1–3 make unreachable. This is the phase where "ensure a clean codebase" pays off.

### Removal candidates (evidence required per entry)

Only remove once Phase N's validation passes AND `world.glb` is committed AND there is no fallback need. Each entry lists the *evidence required* to prove it dead.

| Candidate | Evidence required to remove |
|-----------|-----------------------------|
| `static_props:` section in `scene.yaml` (both worlds) | `world.glb` committed for both `main_world` and `sandbox`, Phase 1 landed, no code path reads `staticProps()` from `WorldSceneData` |
| `StaticPropLoader` YAML parse branch | world.glb branch is the only path; all tests pass |
| `ScenePersistence.saveStaticProps(...)` | `WORLD_PLACEMENT` workbench mode removed or rewritten to save to `world.blend` (not YAML) |
| `Renderer3DExperimental.buildChunkModel` + `rebuildTerrain` | Phase 2 `terrain_mesh` path is default AND committed for all worlds |
| `WORLD_PLACEMENT` workbench mode | User confirms Blender fully replaces in-engine placement workflow |
| `TERRAIN_PAINT` workbench mode | Phase 2 terrain mesh baked; `TERRAIN_PAINT` output no longer drives visuals |
| `client/src/main/java/com/osrs/client/renderer/legacy/IsometricRenderer.java` | Confirm `F9` toggle is no longer desired; no code paths enter it by default |
| `SceneEditState` visual placement fields | If WORLD_PLACEMENT removed, the placement editing state is dead |
| `art/world/tutorial_island.scene.yaml` | Already gone per CLAUDE.md; verify no references |

### Anti-removal (DO NOT delete — evidence these are live)

- **G3DJ loader** (`G3dModelLoader` + `UBJsonReader` paths in `ModelLibrary`): 159 of 175 manifest entries are format `g3dj` without source `.blend`. Removing this code breaks 91% of assets.
- `sandbox` world (`art/worlds/sandbox/`): still referenced by `CLAUDE.md` as the default artist-mode launch for `MODEL_PREVIEW`, `EQUIPMENT_FIT`, `ENTITY_BINDING`.
- `entity_visuals.yaml`: not superseded by this plan.

### Validation

- After each phase's removals: `mvn clean install` green.
- No references to removed classes/methods anywhere in `client/`, `shared/`, `server/`.
- `git grep` on removed symbol names returns empty.
- Runtime smoke: artist-mode launches, main-world launches, prop interactions render.

### Rollback

Standard git revert per removal commit.

---

## Migration Runbook (for the user, once Phase 0 lands)

This is the step-by-step the user runs on their workstation to bring up `world.glb` for the first time.

Prereqs: Blender 4.x in PATH. Python 3.10+. Repo cloned.

```bash
# 1. Generate the initial world.blend from existing scene.yaml + manifest.
#    Produces: art/worlds/main_world/world.blend
#    Contains: terrain_mesh object, props collection with named empties/instances.
blender --background --python scripts/blender/generate_world_blend.py -- --world=main_world

# 2. (Optional) Open world.blend in Blender GUI and tweak.
#    Adjust terrain sculpt, move props around, add new ones by duplicating existing
#    named empties and renaming to <manifest_key>.NNN.
blender art/worlds/main_world/world.blend

# 3. Bake lighting (Cycles). Produces updated vertex colors on terrain_mesh.
blender --background --python scripts/blender/bake_world_lighting.py -- --world=main_world

# 4. Export world.glb. Runtime picks this up automatically.
blender --background --python scripts/blender/export_world_glb.py -- --world=main_world

# 5. Launch client to verify.
./scripts/run-artist-client.sh --world-id=main_world

# 6. Commit.
git add art/worlds/main_world/world.blend art/worlds/main_world/world.glb
git commit -m "Introduce world.glb for main_world"
git push
```

From this point on, world authoring happens in Blender, not YAML.

---

## What CI/CD needs to know

- CI/CD triggers on push to `server/`. This plan does not touch `server/`, so server does not rebuild on Phase 0–4 merges.
- Blender CLI is **not** available on the CI runner, and this plan does not require it to be. `world.glb` is committed to the repo as a binary artifact.
- The existing `validate-models.py` and `validate-art.py` Maven hooks remain unchanged. They validate the manifest and art directory; they do not validate `world.glb`.

---

## Risks, honestly

1. **159 G3DJ models without source .blend.** Addressed by the transform-tree design — world.glb does not embed prop geometry. But WYSIWYG authoring is degraded for G3DJ-backed props until each one gets a source `.blend`. Mitigation: the `generate_world_blend.py` script uses sized proxy cubes colored by category so empties are at least visible.
2. **Blender export determinism.** Blender's glTF exporter can reorder nodes in subtle ways. The runtime parser must not rely on ordering — it keys off node names only. Tests verify this.
3. **Vertex color sRGB.** Blender exports linear vertex colors; the stock libGDX `DefaultShader` samples them in linear space. `SceneManager` handles sRGB conversion correctly. If Phase 2 lands before Phase 3, colors may look darker than authored. Mitigation: land Phase 3 quickly after Phase 2, or accept temporarily.
4. **Shadow-map performance on low-end hardware.** 2048² single cascade is cheap but not free. Feature-flag-gated in `LaunchOptions` so it can be disabled.
5. **Dev ergonomics.** Everyone who touches the world now needs Blender installed. That's a hard requirement this plan formalizes.

---

## Done-means-done criteria

- [x] Phase 0: docs + scripts + stubs landed; nothing breaks.
- [ ] Phase 1: `world.glb` in repo for `main_world`; props come from glb; `scene.yaml static_props` section deleted from `main_world/scene.yaml`.
- [ ] Phase 2: `terrain_mesh` node in `world.glb`; `buildChunkModel` no longer runs for `main_world`; AO visibly baked.
- [ ] Phase 3: `SceneManager` owns the prop render pass; directional shadow map enabled; distance fog enabled; sRGB confirmed.
- [ ] Phase 4: kill list executed; `mvn clean install` green; `git grep` on removed symbols empty.
