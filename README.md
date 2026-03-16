# OSRS MMORP

Old School RuneScape-inspired MMORP in Java 21. 

**Status:** Foundation phase (Weeks 1-4) — Tick loop, networking, world state synchronization.

## Quick Start

### Prerequisites
- Java 21+ (JDK)
- Maven 3.8+
- Git

### Build
```bash
mvn clean install
```

### Run Server
```bash
cd server
mvn exec:java -Dexec.mainClass="com.osrs.server.Server"
```

### Run Client
```bash
cd client
mvn exec:java -Dexec.mainClass="com.osrs.client.Client"
```

## Project Structure

```
osrs-mmorp/
├── shared/              # Network protocol (Protocol Buffers) + shared classes
├── server/              # Game server (256-tick loop, Netty, world state)
├── client/              # Game client (LibGDX, rendering, UI)
├── assets/
│   ├── tilesets/        # Tile spritesheet
│   ├── sprites/         # NPC/player sprites
│   ├── ui/              # UI elements (buttons, icons)
│   └── data/            # YAML/JSON configs (map, quests, NPCs, dialogue)
├── docs/
│   ├── ARCHITECTURE.md  # System design, data flow, protocols
│   ├── CONTRIBUTING.md  # Git workflow, code standards, async collab
│   └── PROGRESS.md      # Running log of completed + upcoming work
└── README.md            # This file
```

## Key Documentation

- **[ARCHITECTURE.md](docs/ARCHITECTURE.md)** — System design, tick loop, networking, entity synchronization
- **[PROGRESS.md](docs/PROGRESS.md)** — Sprint tracking, completed tasks, blockers, upcoming work
- **[CONTRIBUTING.md](docs/CONTRIBUTING.md)** — Git workflow, code style, async collaboration

## Development Workflow

1. **Tasks tracked in GitHub Projects** (linked in PROGRESS.md)
2. **Feature branches** — One feature per branch, clear naming
3. **Commits** — Short, imperative messages (no author attributions)
4. **PRs** — Reviewed before merge; see CONTRIBUTING.md

## Technologies

| Layer | Tech |
|-------|------|
| **Language** | Java 21+ |
| **Networking** | Netty 4.1 |
| **Serialization** | Protocol Buffers 3 |
| **Server** | Custom tick-based game loop (256 ticks/sec) |
| **Client Rendering** | LibGDX 1.13 (isometric 2D) |
| **Config/Data** | YAML/JSON (Jackson) |
| **Build** | Maven |

## Contact & Collaboration

- **Lead Dev:** victorystyle
- **Game Artist:** TBD
- **Repository:** https://github.com/EarthDeparture/osrs-mmorp (public)
- **Discord:** #parsundra

---

**Next:** See [PROGRESS.md](docs/PROGRESS.md) for current sprint and blockers.
