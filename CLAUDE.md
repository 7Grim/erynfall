# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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
cd server && mvn exec:java -Dexec.mainClass="com.osrs.server.Server"

# Run client
cd client && mvn exec:java -Dexec.mainClass="com.osrs.client.Client"
```

## Architecture

Three Maven modules with a one-way dependency: `client` → `shared` ← `server`.

### shared (`osrs-shared`)
- Defines `Entity`, `Player`, `NPC` data models used by both sides
- Contains the Protocol Buffers schema (`shared/src/main/proto/network.proto`) that generates all packet types
- All network packet definitions live here — add new packet types to the `.proto` file first

### server (`osrs-server`)
Single-threaded, tick-based game server (256 ticks/sec, ~3.9ms per tick).

**Startup chain:** `Server.main()` → loads `server.yml` → initializes `DatabaseManager` → loads YAML world data → starts `NettyServer` on port 43594 → starts `GameLoop`

**GameLoop.processTick() stages:**
1. Dequeue input packets (`ServerPacketHandler`)
2. Update entity positions
3. `CombatEngine.calculateHit()` — deterministic RNG seeded by tick number
4. Skill/XP progression
5. Loot generation
6. Broadcast deltas to all clients via `NettyServer`

**World state** is held in-memory in `World.java`: players (`HashMap<playerId, Player>`), NPCs, and a `TileMap` (104×104 walkability grid loaded from `assets/data/map.yaml`).

**Database:** `DatabaseManager` uses HikariCP pooling to SQL Server (`jdbc:sqlserver://localhost:1433;databaseName=osrsmmorp`). The schema (14 tables, 2 views, 4 stored procedures) is in `sql/osrs_mmorp_schema.sql`. Currently a placeholder — world state is in-memory only.

### client (`osrs-client`)
LibGDX 1.13 application rendering at 60 FPS with isometric projection (32×16 tile metrics).

**Flow:** `Client.main()` → LibGDX window → `GameScreen` → `NettyClient` connects → sends Handshake → receives `WorldState` → `IsometricRenderer` draws tiles and entities.

UI components (`CombatUI`, `DialogueUI`, `InventoryUI`, `ContextMenu`) are rendered by `GameScreen`.

### Network Protocol
TCP via Netty, serialized with Protocol Buffers. Server has authority — client sends intent (e.g., `PlayerMovement`), server validates and broadcasts `EntityUpdate` to all clients. Update rate ~10 packets/sec per client (delta only).

## Game Data (YAML)

All content configuration lives in `assets/data/`:
- `map.yaml` — 104×104 tile walkability grid
- `npcs.yaml` — NPC definitions (id, position, stats, dialogue refs)
- `dialogue.yaml` — Branching dialogue trees
- `quests.yaml` — Quest definitions with task state machines

The server loads these at startup via Jackson.

## Git Conventions

Branch naming: `<type>/<sprint>-<task-id>-<description>` (e.g., `feature/s2-011-combat-engine`)

Sprint progress is tracked in `docs/PROGRESS.md`. Update it when starting or completing tasks. Commits should be atomic with imperative messages.
