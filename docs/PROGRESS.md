# PROGRESS.md

## Purpose

This file is a concise current-state snapshot.

It is not intended to remain a full historical sprint archive.
Older detailed planning and implementation notes still exist elsewhere in `docs/`, but this file should stay focused on what is true now.

## Current Snapshot

### Core Project State

- server-authoritative architecture is in place
- client supports both 3D experimental and 2D fallback rendering paths
- gameplay breadth is well beyond initial foundation stage
- 3D art workflow is now an active workstream, not an idea

### 3D Art Workflow State

Completed major work:
- GLB-first runtime plumbing added
- artist mode added
- repo-backed source loading added for current art workflow
- canonical visual scene source moved to `art/world/tutorial_island.scene.yaml`
- Art Workbench added
- model preview added
- equipment fit preview added
- live preview-only equipment transform tuning added
- manifest snippet export added
- manifest save-back for active equipment fit preview added
- world placement mode added
- searchable selection added to the workbench
- workbench readability/layout improved

### S5 Skilling and Systems (Complete)

- Full skilling suite implemented with guides: woodcutting, fishing, cooking, smithing, crafting, runecrafting, magic, ranged, firemaking
- Combat expanded to cover melee, ranged, magic with OSRS-accurate formulas
- Quest system, dialogue, NPC roster — all Tutorial Island content in place
- Bronze GLB reference set complete: helm, platebody, platelegs, sword, shield via Blender
- ArtWorkbench: 5 modes (Model Preview, Equipment Fit, World Placement, Entity Binding, Terrain Paint)
- Equipment hide_nodes corrected across all metal tiers
- Walking system: server-authoritative click-to-walk + client prediction
- Azure JWT auth deployed end-to-end; DigitalOcean VM + GitHub Actions CI/CD wired

### Current Major Gaps

- Prayer system (points, activation, drain) — not yet implemented
- Post-Tutorial Island content (mainland map, bank, follow-on quests)
- SQL persistence gaps: XP/inventory can reset on server restart (schema wired, integration incomplete)
- Weight stat: placeholder 0, not synced to client
- Bronze scimitar still G3DJ (only bronze GLB piece not yet Blender-authored)

## Recommended Active Docs

For current work, use this context set first:

1. `../AGENTS.md`
2. `../ARTIST_GUIDE.md`
3. `README.md`
4. `ARCHITECTURE.md`
5. `ART_PIPELINE_IMPLEMENTATION_CHECKLIST.md`
6. `ART_PIPELINE_IMPLEMENTATION_ORDER.md`
7. `CONTRIBUTING.md`

## Next High-Value Areas

The biggest remaining workflow/value areas are:

1. stronger export + validation hardening
2. doc cleanup / source-of-truth lock-in
3. broader real GLB asset adoption
4. further world-placement quality-of-life only if artists still need it

## Notes

- Older sprint sections in previous versions of this file were useful historically, but had become misleading as a current-state source.
- If a detailed historical implementation record is needed, prefer git history or specific milestone docs instead of re-expanding this file into a giant stale log.
