# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Active Source Of Truth

When building or reviewing changes, prefer this context set:

1. `AGENTS.md`
2. `ARTIST_GUIDE.md`
3. `docs/GRAPHICS_STYLE.md` — authoritative visual/animation style guide (read before any art or rendering work)
4. `docs/SCALE_SPEC.md` — canonical world scale (1 tile = 1 WU, player height, terrain, buildings); read before any geometry or placement work
5. `docs/README.md`
6. `docs/ARCHITECTURE.md`
7. `docs/PROGRESS.md`
8. `docs/ART_PIPELINE_IMPLEMENTATION_CHECKLIST.md`
9. `docs/ART_PIPELINE_IMPLEMENTATION_ORDER.md`
10. `docs/SKILL_IMPLEMENTATION_BASELINE.md`
11. `docs/CONTRIBUTING.md`

Important:
- many older docs remain useful as historical design reference
- they are not all equally current
- cross-check them against code and the active docs above before treating them as authoritative

## Commands

```bash
# Build all modules
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Run tests
mvn clean test

# Run tests in one module
mvn test -pl server
mvn test -pl client
mvn test -pl shared

# Run a single test class
mvn test -pl server -Dtest=YourTestClass

# Run a single test method
mvn test -pl server -Dtest=CombatEngineTest#testHitRollDeterministic

# Run server
mvn -pl server exec:java -Dexec.mainClass="com.osrs.server.Server"

# Run client
mvn -pl client exec:java -Dexec.mainClass="com.osrs.client.Client"

# Run auth service
mvn -pl auth spring-boot:run

# Package artist-mode jar (required before running artist scripts)
mvn -pl client -am -DskipTests package

# Launch artist mode — model/entity work (default: sandbox world)
./scripts/run-artist-client.sh

# Launch artist mode — world building (main_world)
./scripts/run-artist-client.sh --world-id=main_world

# Validate models against manifest
python3 scripts/validate-models.py

# Preview what GLB exports would run (dry-run)
python3 scripts/export-blender-models.py --dry-run

# Run GLB exports (requires Blender CLI)
python3 scripts/export-blender-models.py --run
```

## Architecture Snapshot

Modules:
- `shared` — Protocol Buffers schema + shared models/registries. Key path: `shared/src/main/proto/network.proto`
- `server` — authoritative game loop, gameplay systems, packet handling, persistence integration
- `client` — world rendering, UI, local interaction handling, artist mode and workbench
- `auth` — Azure JWT auth service (Spring Boot)

High-level architecture:
- server-authoritative gameplay; client renders and sends intent only
- tick-based server simulation; effectively single-threaded gameplay logic by design
- networking via Netty + Protocol Buffers
- protocol changes: edit `network.proto` first, regenerate via Maven build (never hand-edit generated classes)

Client rendering paths:
- 3D experimental renderer (`Renderer3DExperimental`) is the primary active path
- 2D isometric is still a supported fallback (`F9` toggles between them)
- main runtime flow: `Client.java` → `ErynfallGame.java` → `GameScreen.java`

## Client / Art Workflow

Art directory layout:
- `.blend` source: `art/blender/`
- runtime exports: `art/models/` (`.glb` primary; `.g3dj`/`.g3db` legacy still supported)
- model metadata: `art/models/manifest.yaml`
- entity/resource visual bindings: `art/world/entity_visuals.yaml`
- world scene files:
  - `art/worlds/main_world/scene.yaml` — canonical visual scene for world building (tutorial island + mainland)
  - `art/worlds/sandbox/scene.yaml` — isolated staging scene for model work

The old `art/world/tutorial_island.scene.yaml` path is gone. Do not reference it.

### Two Artist Launch Modes

**Model/entity work (sandbox, default)** — use for `MODEL_PREVIEW`, `EQUIPMENT_FIT`, `ENTITY_BINDING`:
```bash
./scripts/run-artist-client.sh
```
Saves from ENTITY_BINDING go to `art/worlds/sandbox/scene.yaml` and `art/world/entity_visuals.yaml`.

**World building (main_world)** — use for `WORLD_PLACEMENT`, `TERRAIN_PAINT`:
```bash
./scripts/run-artist-client.sh --world-id=main_world
```
Saves go to `art/worlds/main_world/scene.yaml`.

### Art Workbench Modes (F6)

- `MODEL_PREVIEW` — inspect model scale, silhouette, clips
- `EQUIPMENT_FIT` — fit armor/weapons to player_base via real attachment path
- `WORLD_PLACEMENT` — place/edit static props; `P` saves to active world's `scene.yaml`
- `ENTITY_BINDING` — bind NPC/resource archetypes to 3D model keys
- `TERRAIN_PAINT` — paint client-side visual terrain layer

Key global hotkeys: `F5` hot reload, `F6` workbench toggle, `F7` bounds/axes, `F8` anchor debug, `F9` renderer toggle, `F10` pick-volume debug, `F11` render-budget debug.

See `ARTIST_GUIDE.md` for full workbench controls and per-mode hotkeys.

## Code Style

- Java 21; 4-space indent, opening braces same line
- Field order: static constants → instance fields → constructors → methods
- Logger: `private static final Logger LOG = LoggerFactory.getLogger(MyClass.class);`
- Use primitives for hot-path values; tick/time values are `long`
- Prefer explicit imports (no wildcards, no static imports)
- No magic numbers — use named constants
- No `System.out.println` or `printStackTrace` — use SLF4J

See `AGENTS.md` for full style and domain rules.

## Documentation Discipline

Do not assume all docs are current.

If code and docs disagree:
- prefer current code plus the active source-of-truth docs listed above
- treat older planning docs as historical reference unless explicitly refreshed
