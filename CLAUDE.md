# CLAUDE.md

This file provides concise repository guidance for coding agents and onboarding.

## Active Source Of Truth

When building or reviewing changes, prefer this context set:

1. `AGENTS.md`
2. `ARTIST_GUIDE.md`
3. `docs/README.md`
4. `docs/ARCHITECTURE.md`
5. `docs/PROGRESS.md`
6. `docs/ART_PIPELINE_IMPLEMENTATION_CHECKLIST.md`
7. `docs/ART_PIPELINE_IMPLEMENTATION_ORDER.md`
8. `docs/CONTRIBUTING.md`

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

# Run a single test class
mvn test -pl server -Dtest=YourTestClass

# Run server
mvn -pl server exec:java -Dexec.mainClass="com.osrs.server.Server"

# Run client
mvn -pl client exec:exec

# Package artist-mode jar
mvn -pl client -am -DskipTests package
```

## Client / Art Workflow

Current client reality:
- 3D experimental renderer is a primary active path
- 2D is still a fallback/comparison path
- artist mode exists
- the Art Workbench supports:
  - model preview
  - equipment fit preview, tuning, snippet export, and manifest save-back
  - world placement, scene save-back, and lightweight search

Current art workflow:
- `.blend` source under `art/blender/`
- runtime model assets under `art/models/`
- default runtime format is `.glb`
- model metadata in `art/models/manifest.yaml`
- visual scene source in `art/world/tutorial_island.scene.yaml`

See `ARTIST_GUIDE.md` for the full current workflow and hotkeys.

## Architecture Snapshot

Repository modules:
- `client`
- `server`
- `shared`

High-level architecture:
- server-authoritative gameplay
- client renders and sends intent
- networking via Netty + Protocol Buffers
- shared schema in `shared/src/main/proto/network.proto`

## Documentation Discipline

Do not assume all docs are current.

If code and docs disagree:
- prefer current code plus the active source-of-truth docs listed above
- treat older planning docs as historical reference unless explicitly refreshed
