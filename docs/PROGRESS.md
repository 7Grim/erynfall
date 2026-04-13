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

### Current Major Gaps

- broader real-world `.blend -> .glb` usage still needs more proof assets
- validation/export tooling exists but is still early-stage
- docs still need continued pruning and lock-in
- world placement remains intentionally simple and tile-based
- no full undo/redo or terrain editing yet

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
