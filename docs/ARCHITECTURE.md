# ARCHITECTURE.md

## Purpose

This file describes the current high-level architecture of Erynfall.

It is intentionally shorter and more current than the older design-heavy planning docs.

## Core Model

Erynfall is server-authoritative.

The client:
- renders state
- captures player input
- sends intent

The server:
- validates actions
- advances gameplay state
- calculates combat, loot, progression, and other results
- broadcasts state/results back to clients

## Modules

### `shared`

Purpose:
- Protocol Buffers schema
- shared models and registries

Key path:
- `shared/src/main/proto/network.proto`

### `server`

Purpose:
- authoritative game loop
- gameplay systems
- packet handling
- persistence integration

Key traits:
- tick-based game simulation
- effectively single-threaded gameplay logic by design
- Netty networking

### `client`

Purpose:
- world rendering
- UI
- local interaction handling
- artist mode and workbench

Current client rendering reality:
- 3D experimental renderer is an active primary path
- 2D is still a supported fallback/comparison path

## Client Rendering Architecture

Main runtime flow:
- `Client.java`
- `ErynfallGame.java`
- `GameScreen.java`

Key client systems:
- `Renderer3DExperimental`
- side-panel and gameplay UI
- workbench popup and artist tools
- packet handling for live gameplay state

### 3D Art Pipeline

Current source model:
- `.blend` source under `art/blender/`
- runtime exports under `art/models/`
- default runtime format `.glb`
- metadata in `art/models/manifest.yaml`
- visual scene source in `art/world/tutorial_island.scene.yaml`

### Artist Mode

Artist mode is an offline/source-backed client workflow that:
- bypasses normal login flow
- loads model metadata and model files from repo paths
- loads visual scene data from canonical source paths
- exposes the Art Workbench for:
  - model preview
  - equipment fit
  - world placement

## Visual Scene Ownership

Canonical visual scene source:
- `art/world/tutorial_island.scene.yaml`

This is distinct from gameplay authority.

The visual scene file owns things like:
- static prop placements
- terrain visual height data

It does not redefine:
- gameplay pathing authority
- server collision truth
- server simulation rules

## Runtime Asset Modes

### Packaged mode

Uses generated/copied runtime resources under the client module.

### Artist mode

Uses repo-backed source paths directly for current art workflow iteration.

This is what enables:
- direct hot reload
- workbench save-back
- iterative asset tuning without manual resource-copy steps on each change

## Current Hotkeys

Global implemented F-key controls:
- `F5` hot reload assets
- `F6` art workbench toggle in artist mode
- `F7` 3D bounds/axes debug
- `F8` 3D anchor debug
- `F9` 3D / 2D renderer toggle
- `F10` 3D pick-volume debug
- `F11` 3D render-budget debug

For the full artist workflow control map, see `../ARTIST_GUIDE.md`.

## For New Assistants

If you are a new LLM or contributor, read these next:

1. `../AGENTS.md`
2. `../ARTIST_GUIDE.md`
3. `PROGRESS.md`
4. `ART_PIPELINE_IMPLEMENTATION_CHECKLIST.md`
5. `ART_PIPELINE_IMPLEMENTATION_ORDER.md`
6. `CONTRIBUTING.md`
