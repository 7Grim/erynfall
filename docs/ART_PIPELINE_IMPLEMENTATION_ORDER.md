# ART_PIPELINE_IMPLEMENTATION_ORDER.md
## Purpose
Execution order and dependency graph for the Blender-first, GLB-first art pipeline migration.
Use this document together with:
- `docs/ART_PIPELINE_IMPLEMENTATION_CHECKLIST.md`
This file answers:
- what must happen first
- what can happen in parallel
- what blocks what
- what “done enough” means before moving on
## How To Use This With The Checklist
- The checklist is the master completeness tracker.
- This document is the implementation sequence.
- Each phase below references checklist workstreams by number:
  - `W1` Launch / Artist Mode / Offline Workflow
  - `W2` GLB / GLTF Runtime Support
  - `W3` Direct Repo Loading
  - `W4` Canonical Source Ownership
  - `W5` Scene Loaders / Ownership
  - `W6` Renderer Refactor For 3D Authoring
  - `W7` Model Manifest Schema Upgrade
  - `W8` Validation / Export Tooling
  - `W9` In-Client Art Workbench
  - `W10` Build / Packaging / CI
  - `W11` Docs
## Critical Strategy
We are not doing a perfect slow migration.
We are doing the shortest path to an unblocked artist workflow.
That means:
1. make the client capable of loading the right format
2. make the client capable of loading directly from repo paths
3. make the client launch without gameplay/auth friction
4. make the artist able to preview and place assets
5. then harden validation, packaging, and docs

## Current Progress Snapshot
- Completed blocks:
  - `A` GLB-first runtime plumbing
  - `B` artist mode + repo-backed model loading
  - `C` canonical visual scene source + scene loaders
  - `D1` manifest transforms applied more consistently to props/actors
  - `D2a` bounds / axes / anchor debug overlays
  - `D2b` minimal workbench shell + model preview
  - `D2c` equipment fit preview
  - `D2d` interactive preview camera
  - `D2e` live preview-only equipment transform tuning
  - `D2f` manifest-ready snippet export
- Current next target:
  - `E1` Blender source workspace + export/validation workflow foundations
- Biggest unresolved work after `D2g`:
  - world prop placement tooling
  - stronger validation/export tooling
  - CI hardening
  - doc lock-in
## Hard Blockers
These are true blockers. Do not skip them.
- `B1` Runtime still only supports `.g3dj/.g3db`
  - blocks Blender-native workflow
  - refs: `W2`, `W8`, `W10`
- `B2` Art iteration currently depends on `mvn generate-resources`
  - blocks fast iteration
  - refs: `W3`, `W10`
- `B3` Client assumes login/game flow first
  - blocks easy artist onboarding
  - refs: `W1`
- `B4` Visual world source ownership is wrong
  - `static_props.yaml` and `terrain_height.yaml` are hand-authored under runtime resources
  - blocks clean authoring workflow
  - refs: `W4`, `W5`
- `B5` No in-client tooling for fit/placement
  - blocks efficient use of the new pipeline
  - refs: `W9`

Current blocker status:
- `B1` partially resolved
  - GLB plumbing is implemented, but still lacks a real in-repo `.glb` proof asset
- `B2` resolved for model/scene iteration
- `B3` resolved
- `B4` mostly resolved for visual scene data
- `B5` partially resolved
  - model preview + equipment fit exist
  - world placement tooling still missing
## Dependency Graph
High-level graph:
```text
W2 GLB/GLTF support
  -> W3 direct repo loading
  -> W8 validator/tooling update
  -> W10 packaging/CI update
W1 artist mode / launch path
  -> W9 art workbench
W4 canonical source ownership
  -> W5 scene loaders
  -> W9 prop placement persistence
  -> W10 packaged runtime copies
  -> W11 docs
W7 manifest schema upgrade
  -> W2 loader behavior
  -> W6 renderer transform contract
  -> W8 validation
  -> W9 equipment fit UI
W6 renderer transform refactor
  -> W9 meaningful preview/fitting
  -> better real-world 3D authoring
W2 + W3 + W1 + partial W7
  -> first usable artist workflow
W8 + W10 + W11
  -> stabilization and team-wide adoption
Implementation Order
## Phase 0: Freeze Decisions
Goal: prevent scope drift before code starts.
Checklist refs:
- W2
- W4
- W7
- W11
Tasks:
- [x] Confirm Blender (.blend) is committed source-of-truth for 3D assets.
- [x] Confirm .glb is default runtime/export format.
- [x] Confirm .gltf is optional, not primary.
- [x] Confirm .g3dj/.g3db are transitional only.
- [x] Confirm Aseprite becomes secondary pipeline, not primary.
- [x] Confirm visual scene source moves under art/.
- [x] Confirm artist mode does not require auth/server/admin flow.
Exit criteria:
- [x] No ambiguity remains about source format, runtime format, or source ownership.
Why first:
- if this is not frozen, later implementation thrashes badly.
---
## Phase 1: Establish Core Runtime Capability
Goal: make the client capable of loading modern 3D assets at all.
Checklist refs:
- W2
- W7
- W8
- W10
Tasks:
- [x] Add GLB/GLTF dependency support in Maven.
- [x] Extend runtime model loading beyond .g3dj/.g3db.
- [ ] Update ModelLibrary to load .glb/.gltf alongside legacy formats.
- [x] Update manifest schema so format can represent glTF-era assets cleanly.
- [x] Update model validation so .glb/.gltf are accepted.
- [x] Keep legacy G3D loading alive during migration.
Files most likely touched:
- pom.xml
- client/pom.xml
- client/src/main/java/com/osrs/client/renderer/ModelLibrary.java
- art/models/manifest.yaml
- scripts/validate-models.py
Exit criteria:
- [ ] Client can load at least one .glb model successfully.
- [x] Existing G3D assets still load.
- [x] Model manifest can declare GLB/GLTF assets without hacks.
Why before everything else:
- nothing else matters if runtime cannot load the target format.
---
## Phase 2: Remove Maven From The Tight Artist Loop
Goal: direct repo-backed loading in artist mode.
Checklist refs:
- W3
- W2
- W10
Tasks:
- [x] Introduce classpath-vs-filesystem asset resolution.
- [x] Add repo-root aware loading for model manifest.
- [x] Add repo-root aware loading for model files.
- [x] Make F5 reload from repo-backed sources in artist mode.
- [x] Improve reload logs so artists know what actually refreshed.
Files most likely touched:
- client/src/main/java/com/osrs/client/renderer/ArtAssetResolver.java
- client/src/main/java/com/osrs/client/renderer/ModelLibrary.java
- client/src/main/java/com/osrs/client/GameScreen.java
Exit criteria:
- [ ] Artist can export a .glb into repo and reload it without running Maven.
- [x] Packaged/classpath mode still works.
Why now:
- this is the biggest real workflow unlock after format support.
---
## Phase 3: Add Explicit Artist Launch Path
Goal: artist can boot the client without login/auth friction.
Checklist refs:
- W1
Tasks:
- [x] Add launch options / flags.
- [ ] Add explicit artist-mode entry point.
- [x] Route ErynfallGame to login or workbench/artist flow based on launch mode.
- [x] Make auth/network optional in artist mode.
- [ ] Optionally expose Artist Mode from LoginScreen.
Files most likely touched:
- client/src/main/java/com/osrs/client/Client.java
- client/src/main/java/com/osrs/client/LaunchOptions.java
- client/src/main/java/com/osrs/client/ClientArtistMode.java
- client/src/main/java/com/osrs/client/ErynfallGame.java
- client/src/main/java/com/osrs/client/ui/LoginScreen.java
- client/src/main/java/com/osrs/client/GameScreen.java
- client/src/main/java/com/osrs/client/auth/AuthApiClient.java
- client/src/main/java/com/osrs/client/network/NettyClient.java
Exit criteria:
- [x] Artist can launch directly into an offline art-safe client path.
- [x] Normal login flow still works.
Why after Phase 2:
- artist mode is much more valuable once it loads repo assets directly.
---
## Phase 4: Fix Source Ownership
Goal: move authored visual scene data out of runtime resources.
Checklist refs:
- W4
- W5
Tasks:
- [x] Create art/blender/
- [x] Create art/world/
- [x] Add canonical scene file, eg art/world/tutorial_island.scene.yaml
- [x] Demote client/src/main/resources/static_props.yaml to generated/runtime copy
- [x] Demote client/src/main/resources/terrain_height.yaml to generated/runtime copy
- [ ] Clarify gameplay map vs visual scene ownership
Files most likely touched:
- new art/blender/
- new art/world/tutorial_island.scene.yaml
- client/src/main/resources/static_props.yaml
- client/src/main/resources/terrain_height.yaml
- assets/data/map.yaml
- client/src/main/resources/map.yaml
Exit criteria:
- [x] Artists have a proper source-owned location for visual scene data.
- [x] Runtime resources are no longer pretending to be the source of truth.
Why before workbench save-back:
- the workbench needs a correct place to read/write authoring data.
---
## Phase 5: Update Loaders To Follow New Ownership
Goal: runtime reads canonical scene sources correctly.
Checklist refs:
- W5
Tasks:
- [x] Update static prop loading to consume canonical scene schema.
- [x] Update terrain height loading to consume canonical scene schema.
- [ ] Clarify map loading boundaries.
- [ ] Keep server gameplay authority intact.
Files most likely touched:
- client/src/main/java/com/osrs/client/world/StaticPropLoader.java
- client/src/main/java/com/osrs/client/world/TerrainHeightLoader.java
- client/src/main/java/com/osrs/client/world/MapLoader.java
- server/src/main/java/com/osrs/server/world/TileMap.java
Exit criteria:
- [x] Scene loading works from canonical artist-owned source.
- [ ] No silent ownership ambiguity remains.
Why before editor persistence:
- save/load tools are dangerous if ownership is still fuzzy.
---
## Phase 6: Refactor Renderer Assumptions
Goal: stop fighting authored 3D data.
Checklist refs:
- W6
- W7
Tasks:
- [x] Formalize transform contract for props/resources/actors/equipment.
- [x] Stop relying on effectively hardcoded tile-center-only assumptions.
- [x] Support manifest transform usage consistently across categories.
- [x] Expose anchor/bounds/debug draw hooks needed by the workbench.
Files most likely touched:
- client/src/main/java/com/osrs/client/renderer/Renderer3DExperimental.java
- client/src/main/java/com/osrs/client/renderer/ModelTransformContract.java
- art/models/manifest.yaml
Exit criteria:
- [x] Artist can fix small positioning/orientation issues via metadata rather than forced re-export.
- [x] Props, actors, and equipment behave under one coherent transform contract.
Why before fit/placement tooling:
- otherwise workbench tuning will be built on bad assumptions.
---
## Phase 7: Build The Art Workbench
Goal: real artist productivity.
Checklist refs:
- W9
Tasks:
- [x] Add model preview mode
- [x] Add equipment fit mode
- [ ] Add static prop placement mode
- [x] Add save/copy workflow for manifest transforms
- [ ] Add save/copy workflow for scene placements
- [x] Add anchor, axis, and bounds visualization
- [x] Add clip selection for actor bases
Files most likely touched:
- client/src/main/java/com/osrs/client/ui/ArtWorkbenchPopup.java
- client/src/main/java/com/osrs/client/art/SceneEditState.java
- client/src/main/java/com/osrs/client/art/ScenePersistence.java
- client/src/main/java/com/osrs/client/GameScreen.java
- client/src/main/java/com/osrs/client/renderer/Renderer3DExperimental.java
Exit criteria:
- [x] Artist can preview a model by key
- [x] Artist can fit equipment on player_base
- [ ] Artist can place and rotate props in-world
- [x] Artist can save or at least copy the resulting metadata cleanly.
Why now:
- this is the moment the pipeline becomes truly usable.
---
## Phase 8: Harden Validation And Export Tooling
Goal: stop runtime surprises.
Checklist refs:
- W8
Tasks:
- [x] Upgrade validation to match actual runtime rules.
- [ ] Validate clips, anchors, and missing dependencies.
- [x] Add canonical Blender export helper script.
- [ ] Keep equipment coverage audit working after migration.
Files most likely touched:
- scripts/validate-models.py
- scripts/export-blender-models.py
- scripts/audit-3d-equipment-coverage.py
Exit criteria:
- [ ] Broken asset exports fail fast before the artist wastes time in-client.
- [ ] Validation understands GLB-era reality, not just old G3D file existence.
Why not earlier:
- first get the path working, then harden it.
---
## Phase 9: Packaging And CI
Goal: make the new pipeline safe for the full team.
Checklist refs:
- W10
Tasks:
- [ ] Update resource copy rules for .glb, .gltf, .bin, textures
- [ ] Keep packaged build path healthy
- [ ] Update CI to reflect new asset pipeline
- [ ] Ensure artist-mode direct loading does not break normal packaged runs
Files most likely touched:
- client/pom.xml
- pom.xml
- .github/workflows/compile-check.yml
Exit criteria:
- [ ] CI still builds/tests
- [ ] packaged runtime still works
- [ ] direct artist mode and packaged mode both remain supported
Why near the end:
- CI should reflect the settled architecture, not a half-migrated intermediate state.
---
## Phase 10: Documentation Lock-In
Goal: make the new workflow understandable and durable.
Checklist refs:
- W11
Tasks:
- [ ] Add docs/ART_PIPELINE.md
- [ ] Rewrite ARTIST_GUIDE.md
- [ ] Update architecture docs
- [ ] Update map/source-ownership docs
- [ ] Update repo agent guidance
Files most likely touched:
- docs/ART_PIPELINE.md
- ARTIST_GUIDE.md
- docs/ARCHITECTURE.md
- docs/TUTORIAL_ISLAND_MAP.md
- docs/README.md
- docs/PROGRESS.md
- CLAUDE.md
- AGENTS.md
Exit criteria:
- [ ] docs describe the real 3D workflow
- [ ] no more sprite-first framing at the top of the artist guide
- [ ] future contributors do not regress into the old pipeline assumptions
---
## Parallel Work Opportunities
These can run in parallel once dependencies allow.
Parallel Set A
Can start once Phase 1 is stable:
- W1 artist launch path
- W3 repo-backed loading
- partial W7 manifest schema cleanup
Parallel Set B
Can start once Phase 4 is stable:
- W5 scene loaders
- W9 scene persistence/data model scaffolding
- W11 doc outline drafting
Parallel Set C
Can start once Phase 6 is stable:
- W9 equipment fit tooling
- W8 stronger transform/anchor validation
## Must Not Run In Parallel
These pairings are high-risk if done at the same time.
- Do not fully rewrite renderer materials while integrating GLB loading.
- Do not finalize workbench save-back before source ownership is fixed.
- Do not remove G3D support before at least one successful GLB path is stable.
- Do not update docs as if the migration is complete before the actual launch/load/workbench path exists.
## Minimum Viable Artist Unblock
If we need a practical “usable early” milestone, it is this:
- [ ] GLB loads in client
- [x] artist mode launches directly
- [x] repo-backed direct loading works
- [x] F5 reload works without Maven
- [x] one simple model preview tool exists
- [x] one simple equipment fit preview exists
- [x] one canonical artist-owned scene file exists
If these are true, the artist is no longer fundamentally blocked.
## Recommended Execution Cadence
Order for active implementation sessions:
1. Phase 1
2. Phase 2
3. Phase 3
4. Phase 4
5. Phase 5
6. Phase 6
7. Phase 7
8. Phase 8
9. Phase 9
10. Phase 10
## Session Rule
For each implementation session:
- pick exactly one phase or one tightly-coupled sub-block
- verify compile/load behavior before moving on
- update both:
  - checklist status
  - this implementation-order doc if dependencies change
## Suggested First Working Block
Start here first:
Block A
- W2 core GLB/GLTF runtime support
- W7 minimum manifest changes required for GLB
- W8 minimum validator acceptance for GLB
- W10 minimum Maven support for GLB assets
Why:
- this is the foundation for everything else
Block B
Immediately after:
- W3 direct repo loading
- W1 artist mode entry path
Why:
- this produces the first actually useful iteration loop
Block C
Then:
- W4 + W5 source ownership + scene loaders
Why:
- this gives the workbench somewhere correct to persist data
Block D
Then:
- W6 renderer transform contract
- W9 art workbench
Why:
- this is the real productivity unlock

Current implementation note:
- Block D is now materially underway.
- Completed inside Block D so far:
  - D1 manifest transforms for props/actors
  - D2a debug overlays
  - D2b model preview shell
  - D2c equipment fit preview
  - D2d interactive preview camera
  - D2e preview-only live tuning
  - D2f manifest-ready snippet export
- Next sub-block:
  - E2 additional validation depth and CI hardening

## Definition Of Progress
We are making real progress only if one of these becomes true:
- artist needs fewer external steps
- artist needs fewer blind edits
- artist needs less gameplay/auth setup
- artist can preview more directly in-client
- source ownership gets cleaner
- runtime truth and docs get closer together
If a change does not improve one of those, it is probably scope noise.
## Final Readiness State
We are done when:
- the artist can work from Blender with .blend source and .glb runtime export
- the client supports direct repo-backed art iteration
- the workbench handles preview, fit, and placement
- packaging and CI still work
- docs match reality
