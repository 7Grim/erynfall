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

### Local Main World

Build once:

macOS / Linux:

```bash
mvn -pl shared,server,client -am -DskipTests package
```

Windows:

```cmd
mvn -pl shared,server,client -am -DskipTests package
```

Run local `main_world` server:

macOS / Linux:

```bash
./run-server.sh --world main_world
```

Windows:

```cmd
run-server.bat --world main_world
```

Run local client:

macOS / Linux:

```bash
./run-client.sh
```

Windows:

```cmd
run-client.bat
```

Then on the login screen choose:
- `Erynfall_001`

When connecting to a remote/live server target, the login screen intentionally exposes only:
- `Erynfall_001`

`Sandbox` is only intended for local/dev testing against localhost.

### Local Sandbox

Run local `sandbox` server:

macOS / Linux:

```bash
./run-server.sh --world sandbox
```

Windows:

```cmd
run-server.bat --world sandbox
```

Run local client:

macOS / Linux:

```bash
./run-client.sh
```

Windows:

```cmd
run-client.bat
```

Then on the login screen choose:
- `Sandbox`

Important:
- `sandbox` should primarily be used for local/dev testing
- `main_world` is the real world containing Tutorial Island and mainland

### Local Server Auth / DB Setup

For a local server that still uses the shared Azure auth/database stack, create:

```bash
server/.env.server.local
```

with at minimum:

```bash
DB_PASSWORD=...
JWT_SIGNING_KEY=...
JWT_ISSUER=...
JWT_AUDIENCE=erynfall-game
```

Then launch normally with the server launcher for either `main_world` or `sandbox`.

If these values are missing, the local server will still start, but:
- DB persistence will fall back to in-memory mode if `DB_PASSWORD` is missing
- token-auth login from the Azure auth service will be rejected if `JWT_SIGNING_KEY` is missing

### Remote Live Server Client

macOS / Linux:

```bash
GAME_SERVER_HOST=165.22.37.200 ./run-client.sh
```

Windows:

```cmd
set GAME_SERVER_HOST=165.22.37.200 && run-client.bat
```

Then on the login screen choose:
- `Erynfall_001`

### Production Server World Selection

The deployed live server should explicitly launch:

```bash
java -Derynfall.worldId=main_world -jar /opt/erynfall/server.jar
```

Do not rely on the server's default world selection for production.

Recommended `systemd` service command:

```ini
ExecStart=/usr/bin/java -Derynfall.worldId=main_world -jar /opt/erynfall/server.jar
```

This keeps:
- `main_world` = live deployed world
- `sandbox` = local/dev testing world only

### Run Artist Mode

Build the client jar first:

```bash
mvn -pl shared,client -am -DskipTests package
```

### Artist Mode On macOS / Linux

Client:

```bash
./scripts/run-artist-client.sh
```

This runs artist mode against the local repo and defaults to the sandbox-style workflow.

If the artist/client also needs a local server running for non-offline checks, use:

```bash
./run-server.sh --world sandbox
```

### Artist Mode On Windows

Windows PowerShell:

```powershell
.\scripts\run-artist-client.ps1
```

Windows cmd:

```cmd
scripts\run-artist-client.bat
```

If the artist/client also needs a local server running for non-offline checks, use:

```cmd
run-server.bat --world sandbox
```

Important:
- artist mode is primarily intended for the local sandbox/testing workflow
- the artist should generally not use `main_world` for everyday model iteration
- use the normal client path with `Erynfall_001` when validating the real integrated world

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
