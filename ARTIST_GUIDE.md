# Artist Guide

## Purpose

This guide defines the current art workflow for Erynfall.

The project is now 3D-first for authored:
- character models
- equipment models
- props and shells
- visual world-scene placement

Primary workflow:
1. Author in Blender.
2. Keep `.blend` source files under `art/blender/`.
3. Export runtime assets to `art/models/` as `.glb`.
4. Describe runtime metadata in `art/models/manifest.yaml`.
5. Use artist mode and the in-client Art Workbench to preview, fit, bind, paint, and place assets.
6. **Model work (MODEL_PREVIEW, EQUIPMENT_FIT, ENTITY_BINDING):** launch artist mode with `sandbox` world (default). Saves from ENTITY_BINDING go to `art/worlds/sandbox/scene.yaml` and `art/world/entity_visuals.yaml`.
7. **World building (WORLD_PLACEMENT, TERRAIN_PAINT):** launch artist mode with `main_world`. Saves go to `art/worlds/main_world/scene.yaml`.
8. Save entity/resource visual bindings to `art/world/entity_visuals.yaml`.

This file is the source of truth for the current art workflow.

## Pipeline Summary

### Source Of Truth

- 3D authored source: `art/blender/`
- Runtime model exports: `art/models/`
- Model metadata: `art/models/manifest.yaml`
- Visual world-scene source: `art/worlds/main_world/scene.yaml` (world building)
- Sandbox test scene: `art/worlds/sandbox/scene.yaml` (model staging only)
- Entity/resource visual bindings: `art/world/entity_visuals.yaml`

### Runtime Format

Default runtime/export target:
- `.glb`

Allowed but secondary:
- `.gltf`

Legacy transitional formats still supported:
- `.g3dj`
- `.g3db`

Important:
- Do not use `.g3dj` as the primary future authoring target.
- Do not treat exported runtime files as the only source of truth.
- Commit `.blend` source files.

## Directory Layout

```text
art/
  blender/                      # canonical Blender source (.blend)
  models/                       # runtime 3D exports (.glb primary)
    manifest.yaml               # model metadata / attachment metadata
  world/
    entity_visuals.yaml         # entity/resource archetype visual bindings
  worlds/
    main_world/
      scene.yaml                # canonical visual scene (terrain + props for tutorial island region + mainland)
    sandbox/
      scene.yaml                # sandbox scene (model staging, not world building)
scripts/
  export-blender-models.py      # export planner / optional Blender CLI runner
  validate-models.py            # model + manifest validation
  run-artist-client.ps1
  run-artist-client.bat
  run-artist-client.sh
```

## Tool Choice

### Use Blender For

- player and NPC base models
- equipment models
- props
- shells and buildings
- other authored 3D scene assets

### Use Aseprite Only For

- 2D fallback/support assets
- sprite or overlay support still in the project
- non-primary 3D workflow tasks

Aseprite is no longer the primary workflow for authored world or character art.

## Authoring Rules

### 1. Blender Source Files

Put `.blend` files under:
- `art/blender/`

Subfolders are allowed.

Example:
- `art/blender/player/player_base.blend`

### 2. Runtime Export Files

Export runtime files to:
- `art/models/`

Default export target:
- `.glb`

Example:
- `art/models/player_base.glb`

### 3. Manifest Metadata

Every runtime model used by the client should have a manifest entry in:
- `art/models/manifest.yaml`

For GLB-era assets, include `source_blend` when possible.

Example:

```yaml
- key: player_base
  file: player_base.glb
  category: actor
  format: glb
  source_blend: player/player_base.blend
```

### 4. Blender Rig and Export Standards

**Full spec:** `docs/GRAPHICS_STYLE.md` — read it before rigging or animating anything.

Key rules (summary only — GRAPHICS_STYLE.md is authoritative):

**Rig method — rigid body part, not vertex skinning:**
- Each body part (head, torso, arms, legs) is a **separate mesh object parented to a bone** via Ctrl+P → Bone.
- No Armature modifier, no weight painting, no automatic weights.
- LibGDX `AnimationController` handles node animations from GLB — this is why rigid parenting works.

**Bone naming (must match exactly):**
```
root → hips → spine → chest → shoulder_l → upper_arm_l → lower_arm_l → hand_l
                             → shoulder_r → upper_arm_r → lower_arm_r → hand_r
                             → neck → head
            → upper_leg_l → lower_leg_l → foot_l
            → upper_leg_r → lower_leg_r → foot_r
```

**Equipment anchor nodes (child bones, exact names):**
`head_anchor`, `weapon_anchor` (child of hand_r), `shield_anchor` (child of hand_l),
`cape_anchor`, `ammo_anchor`, `body_anchor`, `hands_anchor` (chest),
`legs_anchor`, `feet_anchor` (hips)

**Animation clips (NLA tracks, exact names, OSRS tick timing = 600ms/tick):**

| Clip | Duration | Loop |
|---|---|---|
| `idle` | 2.4s | YES |
| `walk` | 1.2s | YES |
| `run` | 0.6s | YES |
| `attack_slash` | 1.8s | NO |
| `attack_shoot` | 1.8s | NO |
| `death` | 1.8s | NO |
| (full table in GRAPHICS_STYLE.md) | | |

**Blender GLB export settings:**
- Format: GLB
- Geometry → Apply Modifiers: ON
- Geometry → Vertex Colors: ON
- Animation → Export NLA Strips: ON
- Armatures → Export Deform Bones Only: OFF (needed for anchor nodes)
- Skinning: OFF

Run `python3 scripts/export-blender-models.py --dry-run` to preview planned exports.
Run `python3 scripts/export-blender-models.py --run` to execute (requires Blender in PATH).

### 5. World Scene Source

Static prop placement and terrain visual scene data belong in the active world's scene file:
- `art/worlds/main_world/scene.yaml` — tutorial island region + mainland (primary world building target)
- `art/worlds/sandbox/scene.yaml` — sandbox staging scene

Each scene file owns:
- `terrain_height`
- `terrain_visual`
- `static_props`

The artist workbench saves to whichever world is currently loaded. Use `main_world` for real world building. Use `sandbox` for isolated model staging and testing.

### 6. Entity Visual Binding Source

Entity and resource archetype visual bindings belong in:
- `art/world/entity_visuals.yaml`

This file is shared across all worlds (entity visuals are not world-specific).

This file maps runtime entity `definition_id` values to visual behavior such as:
- `sprite_key_2d`
- `model_key_3d`
- `animated_3d`
- optional visual metadata like shadow/occlusion/action flags

This is the canonical artist-owned source for which in-game entity archetype uses which visual model.

Do not hand-author:
- `client/src/main/resources/static_props.yaml`
- `client/src/main/resources/terrain_height.yaml`

Those are no longer the canonical source.

## Launching Artist Mode

### Package First

Before launching the packaged client jar, build it:

```bash
mvn -pl client -am -DskipTests package
```

### Two Artist Launch Modes

#### Model Work (default — sandbox world)

Use this for MODEL_PREVIEW, EQUIPMENT_FIT, ENTITY_BINDING.
The sandbox server must be running locally.

macOS / Linux:
```bash
./scripts/run-artist-client.sh
```

Windows PowerShell:
```powershell
.\scripts\run-artist-client.ps1
```

Windows cmd:
```cmd
scripts\run-artist-client.bat
```

This launches in sandbox world (default). Saves from ENTITY_BINDING go to `art/worlds/sandbox/scene.yaml` and `art/world/entity_visuals.yaml`.

#### World Building (main_world)

Use this for WORLD_PLACEMENT and TERRAIN_PAINT on the actual game world.
The main_world server must be running locally.

macOS / Linux:
```bash
./scripts/run-artist-client.sh --world-id=main_world
```

Windows PowerShell:
```powershell
.\scripts\run-artist-client.ps1 --world-id=main_world
```

Windows cmd:
```cmd
scripts\run-artist-client.bat --world-id=main_world
```

WORLD_PLACEMENT and TERRAIN_PAINT saves go directly to `art/worlds/main_world/scene.yaml`. After saving, the client hot-reloads the scene immediately via `reloadRuntimeAssets()`.

These scripts:
- resolve repo root automatically
- launch the client in artist mode
- use the safer `java -jar ... --artist --repo-root=...` path
- fail clearly if the jar is missing

## Hot Reload

### F5

`F5` hot reloads runtime art data in the running client.

Current reload covers:
- sprites
- model metadata
- model assets
- entity visual bindings
- terrain visual data
- static props
- terrain heights

In artist mode, model and scene reload come from repo-backed source paths.

Important:
- hot reload works from repo-backed source for the current artist workflow
- if runtime behavior looks stale, verify the asset actually exported to the expected file

## Global F-Key Hotkeys

These are the currently implemented global F-key bindings in the client.

### F5
- hot reload assets
- reloads sprites, models, static props, and terrain

### F6
- open/close Art Workbench
- artist mode only

### F7
- toggle 3D artist bounds + axes debug overlay

### F8
- toggle 3D artist anchor debug overlay

### F9
- toggle renderer mode
- switches between 3D experimental and 2D isometric

### F10
- toggle 3D pick-volume debug

### F11
- toggle 3D render-budget debug

### F4
- toggle character proportion reference gizmo
- artist mode only
- draws the canonical player/NPC silhouette wireframe in MODEL_PREVIEW
- white = outer bounding box, magenta = zone dividers, cyan = head sub-box
- see docs/CHARACTER_PROPORTION_SPEC.md

### F3 — Batch screenshot capture (MODEL_PREVIEW only)
Captures front/right/rear/iso views of the selected model and saves them to `review/screenshots/{category}/{key}_{view}.png` plus a `{key}.json` sidecar. Camera is held still (delta=0) for each view to produce a clean frozen frame. The `review/` directory is git-ignored.

### F4 — Character proportion gizmo (artist mode)
Toggles a wireframe overlay of the canonical player proportion box (white outer silhouette, magenta zone dividers at shoulder/hip/knee, cyan head sub-box). Use to verify models fit within spec. See `docs/CHARACTER_PROPORTION_SPEC.md`.

### F12 — Building reference gizmo (artist mode)
Toggles a wireframe overlay of the canonical small-building footprint (grey walls, yellow door opening, cyan player-height reference line).

Important note:
- the side panel/tab system uses OSRS-style tab ordering in UI design
- those F-key tab shortcuts are not currently the main artist workflow controls

## Art Workbench Overview

Open the workbench with:
- `F6`

Current workbench modes:
- `MODEL_PREVIEW`
- `EQUIPMENT_FIT`
- `WORLD_PLACEMENT`
- `ENTITY_BINDING`
- `TERRAIN_PAINT`

## Shared Workbench Controls

Available across workbench use:
- `F6` close/open workbench
- `TAB` switch workbench mode
- `/` enter search
- `ESC` exit search
- `ENTER` apply current search result
- `LMB drag` orbit preview camera
- `Mouse wheel` zoom preview camera
- `MMB click` reset preview camera
- `F7` bounds/axes overlay
- `F8` anchor overlay

## Search Behavior

Search is lightweight and mode-aware.

Enter search:
- `/`

While search is active:
- type letters, numbers, space, `-`, or `'`
- `BACKSPACE` deletes one character
- `ENTER` applies the current search result
- `ESC` exits search mode

Search targets:
- `MODEL_PREVIEW`: model keys
- `EQUIPMENT_FIT`: loaded equipment options for the active slot
- `WORLD_PLACEMENT`: placeable prop keys
- `ENTITY_BINDING`: loaded model keys for the selected entity archetype

Search is intentionally disabled in `TERRAIN_PAINT`.

## Mode: MODEL_PREVIEW

Purpose:
- inspect a model in a neutral isolated preview setup
- inspect scale, silhouette, orientation, and clip behavior

### Controls

- `[` / `]` previous/next model
- `;` cycle clip:
  - `AUTO`
  - `IDLE`
  - `WALK`
- `/` search model keys
- `F3` batch screenshot (front/right/rear/iso) → `review/screenshots/`

### Best Use

Use this mode when:
- validating a newly exported model
- checking base silhouette
- checking animation existence at a high level
- checking bounds/orientation with `F7`
- capturing objective review screenshots for async feedback (`F3`)

## Mode: EQUIPMENT_FIT

Purpose:
- preview equipment on `player_base`
- inspect armor fit
- inspect weapon/shield placement
- inspect anchor behavior on the real mannequin composition path

This uses the real attachment path, not fake preview transforms.

Fit warnings are computed each frame and shown at the bottom of the left panel. Red = critical (missing anchor, no meta), yellow = advisory (suspicious scale, large offset, extreme rotation). These are display-only and have no gameplay effect.

### Selection Controls

- `,` / `.` cycle visible equipment slot
- `[` / `]` cycle loaded equipment option for active slot
- `BACKSPACE` clear current slot
- `;` cycle clip:
  - `AUTO`
  - `IDLE`
  - `WALK`
- `/` search loaded equipment options for the active slot

### Live Preview-Only Tuning Controls

Offsets:
- `A` / `D` = offset X - / +
- `W` / `S` = offset Y + / -
- `Q` / `E` = offset Z - / +

Rotations:
- `I` / `K` = rot X - / +
- `J` / `L` = rot Y - / +
- `U` / `O` = rot Z - / +

Reset:
- `R` = reset active slot preview override

Step sizes:
- default: normal step
- `Shift`: fine step
- `Ctrl`: coarse step

### Export / Save

- `C` = export manifest-ready snippet for the active equipment selection
- `P` = save current effective transform values back to `art/models/manifest.yaml`

Important:
- preview overrides are temporary until saved
- `P` writes effective values into the matching manifest entry
- after save, the current preview override is cleared and runtime assets reload

### Best Use

Use this mode when:
- fitting armor to the player base
- fitting held items to `weapon_anchor` / `shield_anchor`
- checking clip behavior in idle/walk
- using `F8` to inspect anchors directly

## Mode: WORLD_PLACEMENT

Purpose:
- place and edit static props in the canonical visual scene
- save scene placement to `art/worlds/{worldId}/scene.yaml` (the active world's scene file)

This is visual scene editing only.
It does not change gameplay pathing or collision authority.

### Prop Selection

- `[` / `]` cycle placeable prop key
- `/` search placeable prop keys

### Placement / Selection

- `LMB` on tile:
  - select or cycle placement on hovered tile if one exists
  - otherwise place new prop on tile
- `N` cycle placements on hovered tile
- `ESC`:
  - if a placement is selected: deselect
  - otherwise: close workbench

### Transform

- `,` / `.` rotate preview or selected placement
- `-` / `=` scale preview or selected placement
- `R` reset preview transform

### Placement Workflow Helpers

- `D` duplicate selected placement into preview state
  - copies key, rotation, scale, and visibility group
  - does not place immediately
  - lets you place the clone elsewhere with `LMB`

### Visibility Controls

- `V` toggle `visibility_group`:
  - `base`
  - `roof`
- `Y` cycle visibility preview filter:
  - `ALL`
  - `BASE_ONLY`
  - `ROOF_ONLY`

### Delete / Save

- `BACKSPACE` delete selected placement
- `P` save scene to `art/worlds/{worldId}/scene.yaml` (active world)

### Best Use

Use this mode when:
- placing buildings, shells, and clutter
- adjusting roof/base scene composition
- quickly repeating props with duplicate-to-preview
- isolating roof-only or base-only visibility with `Y`

## Mode: ENTITY_BINDING

Purpose:
- bind a visible in-world NPC or resource-like archetype to a 3D model key
- inspect and edit the visual binding for its `definition_id`
- save the binding back to `art/world/entity_visuals.yaml`

This mode is archetype-based.
Saving changes all entities that share the selected `definition_id`.

### Selection And Search

- `LMB` select visible NPC/resource-like entity
- `[` / `]` cycle candidate `model_key_3d`
- `/` search loaded model keys for the selected archetype
- `ESC`:
  - if an entity binding target is selected: deselect
  - otherwise: close workbench

### Binding Controls

- `V` toggle `animated_3d`
- `BACKSPACE` clear candidate model key and fall back to legacy/default resolution
- `P` save binding to `art/world/entity_visuals.yaml`

### Best Use

Use this mode when:
- a Blender-exported asset should be assigned to a real world entity archetype
- testing resource/entity model binding without hand-editing YAML
- checking whether a resource should behave as static 3D or animated 3D

## Mode: TERRAIN_PAINT

Purpose:
- paint the client-side visual terrain layer used by `terrain_visual`
- control static ground appearance without changing gameplay walkability

This is visual-only terrain authoring.
Gameplay map authority remains in `map.yaml`.

### Paint Controls

- `[` / `]` cycle terrain type
- `LMB` paint hovered tile
- `E` erase/reset hovered tile override back to fallback
- `P` save terrain visual changes to `art/worlds/{worldId}/scene.yaml` (active world)
- `ESC` close workbench or exit search in other modes

Current terrain visual vocabulary:
- `grass`
- `water`
- `path`
- `wall`
- `sand`

### Best Use

Use this mode when:
- shaping the static ground surface of the map
- painting paths, beaches, water pockets, and wall/rock visual zones
- refining the world-of-nature look without touching gameplay tile semantics

## Debug Overlays

### F7: Bounds + Axes

Use this when:
- checking prop footprint
- checking actor orientation
- checking whether manifest transforms are doing what you expect

Color convention:
- X = red
- Y = green
- Z = blue

### F8: Anchors

Use this when:
- fitting weapon, shield, or body equipment
- checking `weapon_anchor`
- checking `shield_anchor`
- inspecting player-base anchor alignment

## Blender Export Workflow

### Recommended Normal Loop

1. Edit `.blend` under `art/blender/`.
2. Export `.glb` into `art/models/`.
3. Update or create manifest entry in `art/models/manifest.yaml`.
4. Run validation.
5. Launch artist mode.
6. Press `F5` after re-export if the client is already running.
7. Inspect in:
   - `MODEL_PREVIEW`
   - `EQUIPMENT_FIT`
   - `WORLD_PLACEMENT`

## Export Helper

Script:
- `scripts/export-blender-models.py`

### What It Does Now

- scans `art/blender/`
- parses `art/models/manifest.yaml`
- builds an export plan for `format: glb` entries
- prefers explicit `source_blend`
- can do dry-run planning
- can optionally invoke Blender headless to export `.glb`

### What It Does Not Claim

- it is not a full Blender plugin
- it is not guaranteed to be turnkey on every machine without Blender CLI configured
- it is a workflow helper/orchestrator, not the entire pipeline

### Recommended Commands

See what would export:

```bash
python3 scripts/export-blender-models.py --dry-run
```

Show CLI help:

```bash
python3 scripts/export-blender-models.py --help
```

If Blender CLI is configured and you want to run exports:

```bash
python3 scripts/export-blender-models.py --run
```

If Blender is not on PATH:

```bash
python3 scripts/export-blender-models.py --run --blender-exe "/path/to/blender"
```

## Validation

Validation script:
- `scripts/validate-models.py`

Run:

```bash
python3 scripts/validate-models.py
```

Current validation checks include:
- missing model files
- unsupported formats
- extension mismatch
- duplicate keys / duplicate files
- equipment entries missing `anchor_name`
- suspicious equipment origin
- `source_blend` warnings/errors
- duplicate keys/files differing only by case
- anchor integrity against `player_base` where feasible
- some actor/base clip sanity checks
- equipment anchor references against `player_base` when feasible

Important:
- some clip warnings are still heuristic/noisy
- treat warnings as signals to inspect, not always as hard truth

## Manifest Guidance

Current important fields include:
- `key`
- `file`
- `category`
- `format`
- `scale`
- `origin`
- `equip_slot`
- `item_id`
- `anchor_name`
- `offset_x`
- `offset_y`
- `offset_z`
- `rot_x`
- `rot_y`
- `rot_z`
- `hide_nodes`
- `source_blend`

### For Equipment Entries

Make sure equipment entries define:
- `equip_slot`
- `item_id`
- `anchor_name`

### For GLB/GLTF-Era Assets

Prefer including:
- `source_blend`

### For Entity/Resource Visual Binding

Entity/resource binding is now definition-driven.

The canonical file is:
- `art/world/entity_visuals.yaml`

Use the workbench `ENTITY_BINDING` mode when possible instead of hand-editing the file.

### For Terrain Visual Authoring

Static visual ground authoring is now owned by:
- `terrain_visual` inside `art/worlds/main_world/scene.yaml` (for the actual game world)
- `terrain_visual` inside `art/worlds/sandbox/scene.yaml` (for sandbox staging)

Use `TERRAIN_PAINT` in the appropriate world mode instead of trying to repurpose gameplay `map.yaml` for visual art.

## Current Workflow Limitations

These are real current limitations:
- not every workflow has full automation yet
- `.glb` runtime plumbing exists and is now partially proven, but broader real asset-family migration is still needed
- validator clip checks are not yet perfectly trustworthy for all actor assets
- workbench search is lightweight, not a full asset browser
- world placement is tile-based and intentionally simple
- no full undo/redo system yet
- terrain painting is tile-based, not a rich terrain brush/sculpt system
- no full drag-gizmo manipulation yet
- `entity_visuals.yaml` save-back updates the first matching `definition_id` row, so duplicate-row content should be avoided
- this guide describes the workflow, not a complete OSRS fidelity art bible

## What Artists Should Do Right Now

### For Equipment Fit

Use:
- `F6` -> `EQUIPMENT_FIT`
- search with `/`
- tune with `WASD/QE + IJKLUO`
- inspect with `F7` / `F8`
- export with `C`
- save with `P`

### For Prop Placement

Use:
- `F6` -> `WORLD_PLACEMENT`
- search with `/`
- place with `LMB`
- rotate/scale with `, . - =`
- duplicate with `D`
- roof/base group with `V`
- visibility filter with `Y`
- save with `P`

### For Entity / Resource Binding

Use:
- `F6` -> `ENTITY_BINDING`
- click visible entity/resource with `LMB`
- search with `/`
- cycle candidate model key with `[ / ]`
- toggle animated state with `V`
- clear candidate model key with `BACKSPACE`
- save binding with `P`

### For Terrain Painting

Use:
- `F6` -> `TERRAIN_PAINT`
- cycle terrain type with `[ / ]`
- paint with `LMB`
- erase override with `E`
- save with `P`

### For General Model Inspection

Use:
- `F6` -> `MODEL_PREVIEW`
- search with `/`
- cycle clip with `;`
- orbit with mouse
- inspect bounds with `F7`
- toggle character proportion reference with `F4` (player/NPC models)

## Practical Safety Rules

- always inspect diffs after save-back the first few times
- prefer `source_blend` in manifest instead of relying on filename guessing
- keep `.blend` source committed
- treat validator warnings seriously, but not all warnings are fatal
- if a workbench save/export result seems wrong, check:
  - popup status
  - client logs
  - `git diff`

## Current Recommended Artist Checklist

For a new model:
1. create/update `.blend` in `art/blender/`
2. export `.glb` to `art/models/`
3. add/update manifest entry
4. run `python3 scripts/validate-models.py`
5. launch artist mode
6. inspect in appropriate workbench mode
7. save-back if needed
8. review diff before commit

## First Real Asset Test Checklist

For a first end-to-end Blender asset test, use this order:

1. Pick one narrow asset family already used in the game.
2. Create or update the `.blend` under `art/blender/`.
3. Export the runtime `.glb` into `art/models/`.
4. Update manifest entry:
   - `file: *.glb`
   - `format: glb`
   - `source_blend: ...`
5. Run:

```bash
python3 scripts/validate-models.py
python3 scripts/export-blender-models.py --dry-run
```

6. Build/package if needed:

```bash
mvn -pl client -am -DskipTests package
```

7. Launch artist mode.
8. Press `F5` after re-export if the client is already running.
9. Verify in the correct workbench mode:
   - `MODEL_PREVIEW` for raw model inspection
   - `EQUIPMENT_FIT` for wearable/held assets
   - `WORLD_PLACEMENT` for static world props/shells
   - `ENTITY_BINDING` for NPC/resource archetype binding
   - `TERRAIN_PAINT` for static ground visual edits
10. Save from the workbench if needed.
11. Review `git diff` before commit.

## Final Note

This workflow is now centered on:
- Blender for source authoring
- GLB for runtime export
- repo-backed artist mode for iteration
- in-client workbench for preview, fit, binding, terrain paint, and placement

That is the intended path going forward.
