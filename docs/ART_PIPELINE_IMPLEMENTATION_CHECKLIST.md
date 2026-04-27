# ART_PIPELINE_IMPLEMENTATION_CHECKLIST.md
## Purpose
Major checklist for converting Erynfall's art workflow to a Blender-first, GLB-first 3D pipeline with direct repo loading, artist mode, and an in-client workbench.

## Current Status
- Completed blocks: `A`, `B`, `C`, `D1`, `D2a`, `D2b`, `D2c`, `D2d`, `D2e`, `D2f`, `D2g`, `D3a`, `D3b`, `D3c`, `E1`
- Runtime state today:
  - GLB plumbing is in place, but still lacks a real in-repo `.glb` proof asset
  - artist mode launches directly and uses repo-backed model + scene sources
  - canonical visual scene source lives under `art/world/`
  - workbench supports model preview, equipment fit preview, live preview-only tuning, and snippet export
- Most important remaining near-term gap:
  - searchable selection UX polish in workbench modes
- Important unfinished larger areas:
  - `art/blender/` source-of-truth folder
  - world prop placement tooling
  - stronger validation/export tooling
  - CI + docs lock-in

## Decisions Frozen
- [x] Blender (.blend) is the source of truth for 3D authored assets.
- [x] .glb is the default runtime/export format.
- [x] .gltf is allowed only as a secondary/debug/export variant when needed.
- [x] .g3dj / .g3db remain transitional legacy formats only during migration.
- [x] Aseprite remains for 2D fallback/support assets only, not the primary 3D workflow.
- [x] Artist iteration must not require auth, server login, or admin tools.
- [x] Artist iteration must not require mvn generate-resources for every model change.
- [x] Static prop placement and terrain visual data must move under artist-owned source, not stay hand-authored in runtime resources.
- [x] We will optimize for artist throughput over perfect incremental purity.

## End State
- [ ] Artist can open Blender, edit .blend, export .glb, reload in-client, preview in-context, and tune placement/attachment without being blocked by engineering.
- [ ] Client supports GLB/GLTF loading alongside temporary legacy G3D support.
- [x] Client has an explicit artist mode.
- [ ] Client has an art workbench for preview, equipment fit, and prop placement.
- [x] Runtime can load art directly from repo paths in artist mode.
- [x] Packaged build still works from generated/copied runtime resources.
- [ ] Docs reflect the real workflow.
## Critical Constraints
- [ ] Do not center the future pipeline on g3dj.
- [ ] Do not keep artist-owned scene data under client/src/main/resources/ as the canonical source.
- [ ] Do not rely on admin/gameplay item acquisition as the main art preview path.
- [ ] Do not promise full Blender material parity in the first GLB integration pass.
- [ ] Do not rewrite the full renderer into a PBR engine as part of this workflow unblock.
## Workstream 1: Launch, Artist Mode, Offline Workflow
- [x] Change client/src/main/java/com/osrs/client/Client.java
Reason: add launch flag parsing and a first-class artist boot path.
- [x] Add client/src/main/java/com/osrs/client/LaunchOptions.java
Reason: central place for artistMode, offlineMode, repoRoot, workbenchMode.
- [ ] Add client/src/main/java/com/osrs/client/ClientArtistMode.java
Reason: explicit artist entry point for frictionless local launch.
- [x] Change client/src/main/java/com/osrs/client/ErynfallGame.java
Reason: stop assuming login is always the first screen.
- [ ] Change client/src/main/java/com/osrs/client/ui/LoginScreen.java
Reason: expose Artist Mode / Workbench entry path without hiding normal auth flow.
- [x] Change client/src/main/java/com/osrs/client/GameScreen.java
Reason: split startup cleanly into normal auth mode, local dev bypass mode, and offline artist/workbench mode.
- [ ] Change client/src/main/java/com/osrs/client/auth/AuthApiClient.java
Reason: auth calls must be optional and mode-aware.
- [ ] Change client/src/main/java/com/osrs/client/network/NettyClient.java
Reason: connection/handshake must be optional in artist mode.

## Workstream 2: GLB / GLTF Runtime Support
- [ ] Change root pom.xml
Reason: add managed version/property for the GLTF runtime dependency.
- [x] Change client/pom.xml
Reason: add GLTF dependency, repo config if needed, and expand copied runtime asset types beyond .g3dj/.g3db.
- [x] Change client/src/main/java/com/osrs/client/renderer/ModelLibrary.java
Reason: replace G3D-only loading with format-aware loading for .glb, .gltf, .g3dj, .g3db.
- [ ] Add client/src/main/java/com/osrs/client/renderer/ArtAssetResolver.java
Reason: resolve classpath assets in packaged mode and direct repo files in artist mode.
- [ ] Add client/src/main/java/com/osrs/client/renderer/ModelTransformContract.java
Reason: formalize origin, unit scale, axis assumptions, tile snap, and anchor semantics instead of scattering them.
- [x] Keep G3D support during migration, but mark it as temporary in code/docs.

## Workstream 3: Direct Repo Loading, No Maven-on-Every-Change
- [x] Implement repo-backed loading for art/models/manifest.yaml
- [x] Implement repo-backed loading for art/models/*.glb
- [x] Implement repo-backed loading for artist-owned world scene YAML
- [x] Change client/src/main/java/com/osrs/client/GameScreen.java
Reason: F5 must reload from repo-backed sources in artist mode, not just classpath-generated resources.
- [x] Fix reload logging in GameScreen.java
Reason: current message says sprite atlas reloaded, but models/static props/terrain are also reloaded.

## Workstream 4: Canonical Source Ownership
- [x] Add art/blender/
Reason: commit .blend source files in-repo.
- [x] Add art/world/
Reason: artist-owned visual scene source belongs under art/.
- [x] Add art/world/tutorial_island.scene.yaml
Reason: canonical source for static props, terrain visual heights, and workbench scene metadata.
- [x] Demote client/src/main/resources/static_props.yaml to generated/runtime copy only.
- [x] Demote client/src/main/resources/terrain_height.yaml to generated/runtime copy only.
- [ ] Review assets/data/map.yaml
Reason: keep gameplay map authority separate from visual scene authority.
- [ ] Review client/src/main/resources/map.yaml
Reason: either generate it from the gameplay-owned source or eliminate duplicate ownership.

## Workstream 5: Scene Loaders Must Follow New Ownership
- [x] Change client/src/main/java/com/osrs/client/world/StaticPropLoader.java
Reason: load from canonical artist-owned scene schema, not client-only authored resources.
- [x] Change client/src/main/java/com/osrs/client/world/TerrainHeightLoader.java
Reason: same ownership fix and better validation.
- [ ] Change client/src/main/java/com/osrs/client/world/MapLoader.java
Reason: clarify what remains gameplay-owned vs what is visual-only.
- [ ] Review server/src/main/java/com/osrs/server/world/TileMap.java
Reason: server remains authoritative for gameplay collision/walkability; visual scene data must not silently become gameplay truth.

## Workstream 6: Renderer Refactor for Real 3D Authoring
- [x] Change client/src/main/java/com/osrs/client/renderer/Renderer3DExperimental.java
Reason: current prop/actor transform assumptions are too rigid for authored 3D workflows.
- [x] Remove or refactor hardcoded assumptions that every prop/actor is just translated to tile center with Y-only rotation.
- [x] Make manifest transform fields usable consistently for:
  - props
  - resources
  - actor bases
  - equipment
- [ ] Support authored origin semantics beyond the current effectively-no-op tile-center handling.
- [x] Expose bounds/axes/anchor draw hooks for editor/workbench use.
- [x] Keep current equipment anchor pipeline intact while generalizing the transform contract.

## Workstream 7: Model Manifest Schema Upgrade
- [ ] Change art/models/manifest.yaml
Reason: become the canonical runtime metadata schema for GLB-era assets.
- [ ] Keep existing useful fields:
  - key
  - file
  - category
  - format
  - scale
  - origin
  - equip_slot
  - item_id
  - anchor_name
  - offset_x/y/z
  - rot_x/y/z
  - hide_nodes
- [ ] Add source ownership fields, eg source_blend
- [ ] Add clearer category typing if needed, eg actor_base, prop, resource, equipment, shell
- [ ] Add optional artist/editor metadata if useful, eg preview_clip, artist_notes, tags
- [ ] Audit weak/dead fields like attach_to_state
Reason: do not carry metadata that runtime does not meaningfully use.
## Workstream 8: Validation and Export Tooling
- [x] Change scripts/validate-models.py
Reason: extend accepted formats to .glb/.gltf, validate schema additions, sidecar dependencies, and transform rules.
- [ ] Add loader-backed validation path
Reason: Python-only validation is too shallow for binary GLB workflows.
- [x] Add scripts/export-blender-models.py
Reason: define a canonical export step from .blend to runtime files.
- [ ] Review scripts/audit-3d-equipment-coverage.py
Reason: keep coverage audit compatible with schema/format changes.
- [ ] Review scripts/generate-character-models.py
Reason: keep as placeholder/prototyping utility only, not the primary artist workflow.
- [ ] Review scripts/generate-equipment-models.py
Reason: same as above.
- [ ] Review scripts/export-art.sh
Reason: either retire it from primary docs or clearly mark it sprite-only.
- [ ] Review scripts/export-art.bat
Reason: same.
## Workstream 9: In-Client Art Workbench
- [x] Add client/src/main/java/com/osrs/client/ui/ArtWorkbenchPopup.java
Reason: main local authoring UI.
- [ ] Add client/src/main/java/com/osrs/client/art/SceneEditState.java
Reason: hold current selection, active tool, unsaved edits, preview state.
- [ ] Add client/src/main/java/com/osrs/client/art/ScenePersistence.java
Reason: save/load artist-owned scene YAML safely.
- [x] Change client/src/main/java/com/osrs/client/GameScreen.java
Reason: host workbench lifecycle, toggles, input routing, reload/save actions.
- [x] Change client/src/main/java/com/osrs/client/renderer/Renderer3DExperimental.java
Reason: expose pick helpers, anchor markers, gizmo hooks, and debug drawing for workbench.

## Workbench Features: Model Preview
- [x] Preview any model by manifest key
- [x] Neutral ground plane / tile center marker
- [x] Orbit camera / turntable
- [x] Bounds and axis visualization
- [x] Clip selector for actor models
- [ ] Node and anchor listing
- [ ] Clear warnings for missing clips / anchors / files

## Workbench Features: Equipment Fit
- [x] Spawn player_base
- [x] Preview equipped attachment by manifest entry or equip_slot + item_id
- [x] Preview idle / walk / action clips
- [x] Show anchor gizmos
- [x] Live-edit offset_x/y/z
- [x] Live-edit rot_x/y/z
- [ ] Live-edit scale
- [x] Copy or save final manifest snippet

## Workbench Features: World Placement
- [ ] Pick tiles in-world
- [ ] Place selected static prop
- [ ] Rotate prop
- [ ] Scale prop
- [ ] Delete prop
- [ ] Duplicate prop
- [ ] Toggle roof/base visibility preview
- [ ] Save placement back to canonical scene YAML
## Workstream 10: Build, Packaging, CI
- [x] Change client/pom.xml
Reason: copy .glb, .gltf, optional .bin, and dependent textures into packaged runtime resources.
- [x] Ensure packaged build still emits:
  - runtime model metadata
  - copied runtime model assets
  - any generated scene/runtime files
- [ ] Change .github/workflows/compile-check.yml
Reason: CI must validate the new asset pipeline, not only old G3D assumptions.
- [ ] Keep packaged-mode resource generation working even after artist-mode direct loading exists.
## Workstream 11: Docs
- [ ] Add docs/ART_PIPELINE.md
Reason: new canonical artist/dev pipeline doc.
- [ ] Rewrite ARTIST_GUIDE.md
Reason: make it 3D-first, Blender-first, GLB-first, artist-mode-first.
- [ ] Change docs/ARCHITECTURE.md
Reason: document packaged mode vs artist mode, and gameplay-map vs visual-scene ownership.
- [ ] Change docs/TUTORIAL_ISLAND_MAP.md
Reason: current ownership story is stale.
- [ ] Change docs/README.md
Reason: link new art pipeline docs.
- [ ] Change docs/PROGRESS.md
Reason: track migration as a dedicated workstream.
- [ ] Change CLAUDE.md
Reason: update run commands / architecture notes for art workflow.
- [ ] Change AGENTS.md
Reason: future agents must know the new art pipeline rules.
## Critical Acceptance Criteria
- [ ] Artist can work from Blender without learning Maven internals.
- [x] Artist can preview models without auth/login friction.
- [ ] Artist can test prop placement without hand-editing YAML blindly.
- [x] Artist can fit equipment without repeated blind manifest guesswork.
- [ ] Client can load GLB assets successfully in development and packaged modes.
- [ ] Docs no longer describe the project as sprite-first.
- [ ] No remaining hidden ownership conflict between gameplay map data and visual world-art data.
- [x] Legacy G3D assets can coexist temporarily without blocking the new workflow.

## Explicit Non-Goals For First Completion
- [ ] Not required: full PBR renderer rewrite
- [ ] Not required: perfect Blender material parity
- [ ] Not required: removing all legacy G3D assets immediately
- [ ] Not required: eliminating 2D fallback pipeline
- [ ] Not required: solving every aesthetic problem before the workflow unblock lands
## Final Definition Of Done
- [x] Blender source path exists and is documented.
- [ ] GLB/GLTF runtime support is live.
- [x] Artist mode launches directly.
- [x] Repo-backed direct loading works.
- [x] F5 or auto-reload refreshes artist assets correctly.
- [ ] Workbench supports model preview, equipment fit, and prop placement.
- [x] Canonical artist-owned scene files live under art/.
- [ ] Validation reflects the new pipeline and CI is updated.
- [ ] ARTIST_GUIDE.md and new docs match reality.
