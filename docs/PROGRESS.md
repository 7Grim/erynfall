# PROGRESS.md

## Purpose

This file is a concise current-state snapshot.

It is not intended to remain a full historical sprint archive.
Older detailed planning and implementation notes still exist elsewhere in `docs/`, but this file should stay focused on what is true now.

## Current Snapshot

### Core Project State

- server-authoritative architecture is in place
- client is **3D-only** — IsometricRenderer removed from active path; billboard fallback for entities without 3D models
- gameplay breadth is well beyond initial foundation stage
- 3D art pipeline is fully spec'd and operational end-to-end

### 3D Art Pipeline (Complete)

- `docs/GRAPHICS_STYLE.md` created — authoritative visual/animation/rig style guide for artists and AI
- `IsometricRenderer` moved to `renderer/legacy/` package; no 2D rendering in hot path
- Legacy procedural generation scripts moved to `scripts/legacy/`
- `validate-models.py` updated: G3DJ → deprecation warnings; GLB actor `animated` flag check; source_blend enforcement
- `export-blender-models.py` updated: animation NLA strip export, no skinning, vertex colors
- `ARTIST_GUIDE.md` updated with Blender rig/export section
- `AGENTS.md` and `CLAUDE.md` updated to reference `docs/GRAPHICS_STYLE.md`
- Renderer animation: idle fallback on missing clip, blend time tightened to 0.08s (OSRS feel), `normalizePlayerClipName()` expanded to full clip set
- Two-world art structure in place: `art/worlds/sandbox/` (model staging) and `art/worlds/main_world/` (game world)

### 3D Art Workflow (Art Workbench)

- GLB-first runtime plumbing added
- artist mode added
- repo-backed source loading added
- Art Workbench: 5 modes (Model Preview, Equipment Fit, World Placement, Entity Binding, Terrain Paint)
- equipment fit preview + live transform tuning + manifest save-back
- world placement mode with `P` save-back to active world's scene.yaml
- searchable selection in workbench

### S5 Skilling and Systems (Complete)

- Full skilling suite implemented with guides: woodcutting, fishing, cooking, smithing, crafting, runecrafting, magic, ranged, firemaking
- Combat expanded to cover melee, ranged, magic with OSRS-accurate formulas
- Quest system, dialogue, NPC roster — all Tutorial Island content in place
- Bronze GLB reference set complete: helm, platebody, platelegs, sword, shield via Blender
- ArtWorkbench: 5 modes (Model Preview, Equipment Fit, World Placement, Entity Binding, Terrain Paint)
- Equipment hide_nodes corrected across all metal tiers
- Walking system: server-authoritative click-to-walk + client prediction
- Azure JWT auth deployed end-to-end; DigitalOcean VM + GitHub Actions CI/CD wired

### Tutorial Island Polish (Complete)

- Building interior visibility: `_base` (walls+floor) always renders; only `_roof` is skipped when player inside footprint (AUTO/ALWAYS_HIDE modes). Uses `NodePart.enabled = false` on prim[1] to cull the flat ceiling inside `_base.glb` — material alpha never works (LibGDX ModelBatch is deferred).
- Terrain patchiness fixed: simplified to 7 clean visual regions; skill zones sit on natural grass; only arrival path, hub square, and combat yard use path tiles.
- Skill areas moved outdoors: fishing cove, cooking fire/range, and grove are fully outdoor. Forge/smithing hut interior holds only anvil + furnace. Hub building holds tutorial guide, quest guide, banker.
- Fishing Supplier moved to (33,84), Cooking Fire (43,90), Cooking Range (45,90).
- Building door updated: forge has a 3-tile west door (y:87-89) so players walk from mine into forge.
- OSRS-style door system in world.yml; map.yaml regenerated via `python3 scripts/gen_tutorial_map.py`.
- Player character color: `player_base.glb` has no materials JSON so `instance.materials` is empty. Fix creates a default `Material(diffuse=0.76,0.52,0.33)` and assigns to all NodeParts via recursive traversal (`assignMaterialToNodePartsRecursive`).
- Combat de-aggro: `walkTo()` now sets `combatTargetId = -1` before `clearPendingAction()`, so explicit player walk breaks the `processCombatFollow()` re-queue loop.

### Current Major Gaps

- Prayer system (points, activation, drain) — not yet implemented
- Post-Tutorial Island content (mainland map, bank, follow-on quests)
- SQL persistence gaps: XP/inventory can reset on server restart (schema wired, integration incomplete)
- Weight stat: placeholder 0, not synced to client
- Bronze scimitar still G3DJ (only bronze GLB piece not yet Blender-authored)
- Player model colors are a flat single skin tone — no per-zone materials (skin, hair, shirt, pants, boots). Needs artist re-export with separate primitives per zone.

## Recommended Active Docs

For current work, use this context set first:

1. `../AGENTS.md`
2. `../ARTIST_GUIDE.md`
3. `GRAPHICS_STYLE.md`
4. `README.md`
5. `ARCHITECTURE.md`
6. `ART_PIPELINE_IMPLEMENTATION_CHECKLIST.md`
7. `ART_PIPELINE_IMPLEMENTATION_ORDER.md`
8. `CONTRIBUTING.md`

## Next High-Value Areas

1. **Player model per-zone materials** — re-export `player_base.glb` with separate mesh primitives (skin, hair, shirt, pants, boots) so each can be colored independently
2. **Prayer system** — points, activation, drain
3. **SQL persistence** — complete schema wiring so XP/inventory survive server restart
4. **Post-Tutorial Island** — mainland map, bank, follow-on quests
5. **Broader GLB asset adoption** — migrate remaining G3DJ models to Blender-authored GLB

## Notes

- Older sprint sections in previous versions of this file were useful historically, but had become misleading as a current-state source.
- If a detailed historical implementation record is needed, prefer git history or specific milestone docs instead of re-expanding this file into a giant stale log.
