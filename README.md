# Erynfall

Old School RuneScape-inspired MMO project in Java 21.

## Current State

The project is no longer in a pure foundation phase.

Current reality:
- server-authoritative gameplay architecture is in place
- client supports both 2D fallback and 3D experimental rendering
- 3D art workflow is now Blender-first and artist-mode driven
- artist tooling exists for:
  - model preview
  - equipment fit preview and save-back
  - static prop world placement and save-back

## Project Structure

```text
client/   LibGDX client, renderer, UI, artist mode, art workbench
server/   authoritative game loop and gameplay systems
shared/   protocol and shared data models
art/      authored 3D source, runtime model assets, world scene source
docs/     architecture, progress, art pipeline, and project docs
scripts/  validation, export helpers, artist launch scripts
```

## Quick Start

### Build

```bash
mvn clean install
```

### Run Server

```bash
mvn -pl shared,server -am -DskipTests package
./run-server.sh --world sandbox
```

Other local world targets:

```bash
./run-server.sh --world main_world
```

Recommended workflow:
- `sandbox` for local/dev system and content testing
- `main_world` for testing the real live-world layout, including Tutorial Island as a region inside it

The launcher also accepts the JVM property form if you prefer:

```bash
./run-server.sh -Derynfall.worldId=main_world
```

For a local server that still uses the shared Azure auth/database stack, create:

```bash
.env.server.local
```

with at minimum:

```bash
DB_PASSWORD=...
JWT_SIGNING_KEY=...
JWT_ISSUER=...
JWT_AUDIENCE=erynfall-game
```

Then launch normally with `./run-server.sh --world main_world` or `./run-server.sh --world sandbox`.

If these values are missing, the local server will still start, but:
- DB persistence will fall back to in-memory mode if `DB_PASSWORD` is missing
- token-auth login from the Azure auth service will be rejected if `JWT_SIGNING_KEY` is missing

### Run Client

```bash
mvn -pl shared,client -am -DskipTests package
./run-client.sh
```

Connect to a remote server explicitly:

```bash
GAME_SERVER_HOST=165.22.37.200 ./run-client.sh
```

### Run Artist Mode

Build the client jar first:

```bash
mvn -pl client -am -DskipTests package
```

Then use a launch script:

Windows PowerShell:

```powershell
.\scripts\run-artist-client.ps1
```

Windows cmd:

```cmd
scripts\run-artist-client.bat
```

macOS / Linux:

```bash
./scripts/run-artist-client.sh
```

## Read These First

For a new engineer or LLM, the active context set is:

1. `AGENTS.md`
2. `CLAUDE.md`
3. `ARTIST_GUIDE.md`
4. `docs/README.md`
5. `docs/ARCHITECTURE.md`
6. `docs/PROGRESS.md`
7. `docs/ART_PIPELINE_IMPLEMENTATION_CHECKLIST.md`
8. `docs/ART_PIPELINE_IMPLEMENTATION_ORDER.md`
9. `docs/CONTRIBUTING.md`

Important:
- not every markdown file in the repo is equally current
- older large planning docs should be treated as historical reference unless cross-checked with code and the active docs above

## Key Hotkeys

Global:
- `F5` hot reload assets
- `F6` open/close Art Workbench in artist mode
- `F7` 3D bounds/axes debug
- `F8` 3D anchor debug
- `F9` toggle 3D / 2D renderer
- `F10` 3D pick-volume debug
- `F11` 3D render-budget debug

For the full current art workflow and workbench controls, see `ARTIST_GUIDE.md`.
