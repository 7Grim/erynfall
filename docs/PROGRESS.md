# PROGRESS.md - Sprint Tracking

**Last Updated:** 2026-02-26 (Project Kickoff)

**Current Sprint:** S1 (Foundation: Tick Loop & Networking)

**Team:** victorystyle (lead dev), game artist (TBD)

---

## Executive Summary

This document tracks:
1. **Completed work** — What's done and merged
2. **Current sprint** — What we're actively building
3. **Upcoming sprints** — What comes next
4. **Blockers** — Issues blocking progress

**Update Frequency:** Daily (end of work) or after PR merge

**Format:** Each task has:
- Status (🔲 Not started, 🟡 In progress, 🟢 In review, ✅ Complete)
- GitHub issue link (when relevant)
- Brief description
- Blocker notes (if any)

---

## Sprint S1: Foundation (Weeks 1-4)
**Goal:** Tick loop, networking, world state, basic rendering

### S1-001: Maven Project Setup ✅
- ✅ **Complete** — All modules created, dependencies resolved
- **Deliverable:** Multi-module pom.xml, clean build
- **Time:** ~1 hour
- **Merged:** Main branch ready

### S1-002: Server Tick Loop 🟡
- 🟡 **In progress**
- **Task:** Implement 256 ticks/sec loop in `com.osrs.server.Server`
- **What to build:**
  - `Server` class with main method
  - `GameLoop` class (runs on separate thread)
  - Tick counter + nanosecond timing
  - Graceful shutdown on SIGTERM
  - Log tick count every 256 ticks (1 sec)
- **Acceptance Criteria:**
  - Server runs for 10 seconds, prints "Tick N" once per second
  - No timing drift (nanosecond precision)
  - Shutdown exits cleanly
- **Estimated:** 4-6 hours
- **Blocker:** None
- **PR:** (pending)

### S1-003: Netty Server TCP Listener
- 🔲 **Not started**
- **Task:** Implement Netty server listening on port 43594
- **What to build:**
  - `NettyServer` class
  - `ChannelInitializer` for packet handling
  - `ConnectionHandler` (on connect/disconnect)
  - Graceful shutdown
- **Acceptance Criteria:**
  - Server binds to localhost:43594
  - Client connects without error
  - Server logs "Player connected" + "Player disconnected"
  - Can handle 10 concurrent connections
- **Estimated:** 4-6 hours
- **Depends on:** S1-002 (tick loop)
- **Blocker:** None

### S1-004: Protocol Buffers Schema (Network Packets)
- 🔲 **Not started**
- **Task:** Define initial network protocol
- **What to build:**
  - `shared/src/main/proto/network.proto`
  - Message types:
    - `PlayerMovement` (client → server): x, y, facing, sequence
    - `WorldState` (server → client): delta updates
    - `EntityUpdate`: position, animation, health
    - `Handshake`: login request/response
- **Acceptance Criteria:**
  - .proto file compiles via Maven
  - Generated Java classes usable in client + server
  - Can serialize + deserialize a PlayerMovement packet
- **Estimated:** 2-3 hours
- **Depends on:** S1-003 (Netty handlers need to deserialize these)
- **Blocker:** None

### S1-005: Client Handshake (Connect to Server)
- 🔲 **Not started**
- **Task:** Client connects to server, receives world state
- **What to build:**
  - `Client` class (LibGDX main)
  - `NettyClient` (connects to server)
  - `PacketHandler` (receives WorldState)
  - Simple console UI (log connection + tick count)
- **Acceptance Criteria:**
  - Client connects to localhost:43594
  - Receives initial world state from server
  - Logs "Connected to server" + world state
  - Can disconnect gracefully
- **Estimated:** 4-6 hours
- **Depends on:** S1-003, S1-004
- **Blocker:** None

### S1-006: Player Entity (Data Model)
- 🔲 **Not started**
- **Task:** Define Player, NPC, Entity classes
- **What to build:**
  - `shared/com.osrs.shared.data.Entity` (base class)
  - `shared/com.osrs.shared.data.Player` (extends Entity)
  - `shared/com.osrs.shared.data.NPC` (extends Entity)
  - Fields: position, facing, animation, sprite ID, etc.
- **Acceptance Criteria:**
  - Can create Player, NPC instances
  - Can serialize to Protocol Buffers
  - Entity ID is unique per instance
  - Can store in HashMap by ID
- **Estimated:** 2-3 hours
- **Depends on:** S1-004 (protocol needs to know about entities)
- **Blocker:** None

### S1-007: Tile Map (Tutorial Island)
- 🔲 **Not started**
- **Task:** Load tile map data from YAML
- **What to build:**
  - `assets/data/map.yaml` (104×104 tile layout)
  - `server/com.osrs.server.world.TileMap` class
  - Loader that reads YAML + validates collision rules
  - Tile spritesheet placeholder (temporary)
- **Acceptance Criteria:**
  - Load Tutorial Island map from YAML
  - Can query "is tile (x,y) walkable?" instantly
  - Log map dimensions on server start
- **Estimated:** 3-4 hours
- **Depends on:** S1-002 (server startup)
- **Blocker:** None

### S1-008: Isometric Renderer (LibGDX)
- 🔲 **Not started**
- **Task:** Render isometric tiles + player
- **What to build:**
  - `client/com.osrs.client.renderer.IsometricRenderer`
  - Tile rendering (world to screen projection)
  - Player sprite rendering
  - Simple placeholder sprites (white tiles, red player)
- **Acceptance Criteria:**
  - Render 104×104 tile grid on screen
  - Player visible at center
  - No texture errors (all rendered in white/red)
  - 60 FPS on modern hardware
- **Estimated:** 6-8 hours
- **Depends on:** S1-005 (client needs to render world state)
- **Blocker:** Game artist needed for sprites (use placeholders for now)

### S1-009: Player Movement (Validation + Sync)
- 🔲 **Not started**
- **Task:** Player can move, server validates, client renders
- **What to build:**
  - Client input handler (arrow keys → PlayerMovement packet)
  - Server validation (can player walk to target tile?)
  - Server broadcasts new position to client
  - Client updates player position on screen
- **Acceptance Criteria:**
  - Player moves 1 tile per keypress (right, left, up, down)
  - Cannot walk through walls
  - Movement feels responsive (<100ms latency)
  - Other players (if connected) see movement
- **Estimated:** 6-8 hours
- **Depends on:** S1-007 (collision rules), S1-008 (rendering)
- **Blocker:** None

### S1-010: NPC Spawning + Idle Animation
- 🔲 **Not started**
- **Task:** Spawn NPCs, render them, basic idle animation
- **What to build:**
  - `assets/data/npcs.yaml` (15 Tutorial Island NPCs)
  - `server/com.osrs.server.npc.NPCManager` (spawn + track)
  - Server sends NPC positions in WorldState packet
  - Client renders NPC sprites + idle animation
- **Acceptance Criteria:**
  - 15 NPCs spawn at Tutorial Island
  - Each NPC visible on screen
  - Idle animation loops (frame counter increments each tick)
  - NPCs position correctly with isometric projection
- **Estimated:** 4-6 hours
- **Depends on:** S1-008 (rendering), S1-007 (map data)
- **Blocker:** Game artist needed for NPC sprites

---

## Sprint S2: Combat Basics (Weeks 5-8)
**Goal:** Combat system, hit/miss, XP tracking, basic quests

### S2-011: Combat Engine (Hit/Miss Calculation)
- 🔲 **Not started**
- **Task:** Implement deterministic combat math
- **What to build:**
  - `server/com.osrs.server.combat.CombatEngine`
  - Hit roll (based on player attack vs NPC defence)
  - Damage calculation (0-max damage)
  - All RNG seeded by server tick
- **Acceptance Criteria:**
  - Same inputs + same tick = same result (deterministic)
  - Hit chance ~50% vs equal opponents
  - Damage varies 0 to max
  - Unit tests for all calculations
- **Estimated:** 4-6 hours
- **Depends on:** S1-002 (tick-based RNG)
- **Blocker:** None

### S2-012: Combat UI Feedback
- 🔲 **Not started**
- **Task:** Display hit/miss messages + damage numbers
- **What to build:**
  - `client/com.osrs.client.ui.CombatUI`
  - Damage numbers floating above entities
  - Hit/miss messages in chat/log
  - Health bar updates
- **Acceptance Criteria:**
  - Damage numbers visible above target
  - "Your attack hit!" or "Your attack missed!" in log
  - Health bars decrease visually
- **Estimated:** 3-4 hours
- **Depends on:** S2-011, S1-008 (rendering)
- **Blocker:** None

### S2-013: XP & Skill Progression
- 🔲 **Not started**
- **Task:** Track Attack, Strength, Defence XP + levels
- **What to build:**
  - `server/com.osrs.server.player.Stats` (XP tables)
  - XP award on hit/damage dealt
  - Level-up thresholds
  - Client displays current levels in HUD
- **Acceptance Criteria:**
  - 10 attacks → visible level increase
  - XP accumulates correctly
  - Levels display 1-99
  - Persist player stats (in-memory for now)
- **Estimated:** 4-6 hours
- **Depends on:** S2-011 (combat)
- **Blocker:** None

### S2-014: Player Inventory System
- 🔲 **Not started**
- **Task:** Inventory management (20 slots, equip/unequip)
- **What to build:**
  - `server/com.osrs.server.player.Inventory`
  - Client-side UI (grid display)
  - Equip/unequip items
  - Server validates all inventory changes
- **Acceptance Criteria:**
  - Inventory displays 20 slots
  - Can pick up items from world
  - Can equip weapon/armor from inventory
  - Equipment slot shows current item
  - Desync testing (spam equip/unequip)
- **Estimated:** 6-8 hours
- **Depends on:** S1-005 (client/server comms)
- **Blocker:** None

### S2-015: Quest System (State Machine)
- 🔲 **Not started**
- **Task:** Track quest progress (initial implementation)
- **What to build:**
  - `assets/data/quests.yaml` (Tutorial Island quest)
  - `server/com.osrs.server.quest.QuestManager`
  - Quest state tracking (not started, in progress, complete)
  - Client displays quest status
- **Acceptance Criteria:**
  - Tutorial Island quest loadable from YAML
  - Quest state persists per player
  - Server validates quest task completion
- **Estimated:** 4-6 hours
- **Depends on:** S1-005 (client/server)
- **Blocker:** None

### S2-016: Dialogue System (NPC Conversations)
- 🔲 **Not started**
- **Task:** Dialogue trees, branching responses
- **What to build:**
  - `assets/data/dialogue.yaml` (dialogue definitions)
  - `server/com.osrs.server.quest.DialogueEngine`
  - Client UI (NPC says X, player chooses response)
  - Server tracks dialogue state per player
- **Acceptance Criteria:**
  - Load dialogue tree from YAML
  - Can talk to NPC, see options
  - Choose response, dialogue progresses
  - Different dialogue for different players
- **Estimated:** 4-6 hours
- **Depends on:** S1-010 (NPCs), S2-015 (quest integration)
- **Blocker:** None

---

## Sprint S3: Polish & Testing (Weeks 9-14)
**Goal:** Tutorial Island complete, all systems working, balance

### S3-017: Tutorial Island Tutorial Tasks
- 🔲 **Not started**
- **Task:** Implement 5 tutorial mini-tasks
- **Deliverable:**
  - Speak to Guide
  - Fight tutorial rat
  - Collect logs
  - Cook food
  - Equip armor
- **Acceptance Criteria:**
  - All 5 tasks completable
  - Quest log updates on completion
  - Rewards (XP) awarded
- **Estimated:** 6-8 hours
- **Depends on:** S2-015 (quests), S2-016 (dialogue)
- **Blocker:** None

### S3-018: NPC AI Pathfinding
- 🔲 **Not started**
- **Task:** NPCs walk patrol routes
- **What to build:**
  - A* pathfinding over collision grid
  - Patrol path definition in YAML
  - Server calculates path once per N ticks
  - Client interpolates NPC movement
- **Acceptance Criteria:**
  - NPCs walk patrol routes smoothly
  - No clipping through walls
  - Efficient (doesn't spike CPU)
- **Estimated:** 6-8 hours
- **Depends on:** S1-010 (NPCs), S1-007 (collision)
- **Blocker:** None

### S3-019: Sound Effects & Music (Optional)
- 🔲 **Not started**
- **Task:** Add audio (SFX + background music)
- **Deliverable:**
  - Combat hit sound
  - Level-up fanfare
  - Background music (loop)
  - UI click sounds
- **Acceptance Criteria:**
  - Sounds play on events
  - No audio clipping
  - Volume control in settings
- **Estimated:** 4-6 hours
- **Depends on:** All rendering/gameplay systems
- **Blocker:** None (optional; can skip if time-bound)

### S3-020: Client-Side Prediction (Smoothing)
- 🔲 **Not started**
- **Task:** Predict player movement for smooth feel
- **What to build:**
  - Client predicts next move (render at predicted position)
  - Server sends correction (if prediction wrong)
  - Rewind animation if needed
- **Acceptance Criteria:**
  - Movement feels instant (no 50ms latency feel)
  - No visual popping on correction
  - Tests for prediction logic
- **Estimated:** 4-6 hours
- **Depends on:** S1-009 (movement)
- **Blocker:** None

### S3-021: Load Testing (100+ Player Simulation)
- 🔲 **Not started**
- **Task:** Stress-test server with bot players
- **What to build:**
  - Bot client (auto-connects, random moves)
  - Metrics (CPU, memory, tick time)
  - Bottleneck identification
- **Acceptance Criteria:**
  - Server handles 100 bots connected
  - Tick loop under 3.9ms (no drops)
  - Memory usage < 1 GB
  - Can identify bottlenecks
- **Estimated:** 4-6 hours
- **Depends on:** S1-009 (movement)
- **Blocker:** None

### S3-022: Performance Optimization
- 🔲 **Not started**
- **Task:** Profile + optimize identified bottlenecks
- **Deliverable:**
  - Profiling results (JProfiler or JMH)
  - Code optimizations
  - Benchmarks before/after
- **Acceptance Criteria:**
  - Identified bottleneck(s)
  - At least 20% improvement in worst-case
  - Tick loop stable at 256 ticks/sec
- **Estimated:** 6-8 hours
- **Depends on:** S3-021 (load testing)
- **Blocker:** Might find unknown issues

### S3-023: Documentation (Code + Game Design)
- 🔲 **Not started**
- **Task:** Write comprehensive docs
- **Deliverable:**
  - Code documentation (javadoc)
  - Game design document (mechanics, economy, progression)
  - Deployment guide
- **Acceptance Criteria:**
  - All public methods have javadoc
  - Game design doc complete
  - New dev can understand codebase in 1 day
- **Estimated:** 4-6 hours
- **Depends on:** Everything complete
- **Blocker:** None

---

## Sprint S4: Alpha Release Prep (Weeks 15-24)
**Goal:** Tutorial Island complete, ready for public testing

### S4-024: Bug Fixes & Polish (Ongoing)
- 🔲 **Not started**
- **Task:** Address bugs found during S3 testing
- **Deliverable:**
  - Fix all critical bugs (crashes, desync)
  - Polish UI (colors, spacing, fonts)
  - Optimize remaining bottlenecks
- **Estimated:** 8-12 hours
- **Depends on:** S3 completion
- **Blocker:** Depends on what bugs emerge

### S4-025: Asset Pipeline (Game Artist Work)
- 🔲 **Not started**
- **Task:** Create custom art for Tutorial Island
- **Deliverable:**
  - Tile sprites (104×104 unique variations)
  - NPC sprites (15 NPCs × idle animation)
  - Player sprites (male/female, idle animation)
  - UI elements (buttons, inventory slots, etc.)
- **Estimated:** 40-60 hours (artist time)
- **Depends on:** S1-008 (renderer ready to display)
- **Blocker:** Requires game artist

### S4-026: Deployment & Release
- 🔲 **Not started**
- **Task:** Package JAR + create installer
- **Deliverable:**
  - `osrs-server.jar` (runnable server)
  - `osrs-client.jar` (runnable client)
  - Release notes
  - Installation guide
- **Acceptance Criteria:**
  - Can run server + client from command line
  - No unhandled exceptions
  - Logs are clean and informative
- **Estimated:** 3-4 hours
- **Depends on:** S4-024 (everything stable)
- **Blocker:** None

---

## Post-MVP: Multi-Player & Scaling (Sprints S5+)

### S5: Multiplayer (Multiple players per world)
- Shared world state
- Entity interpolation
- Player proximity broadcasting
- Test with 10+ concurrent players

### S6: Persistence (Database)
- PostgreSQL schema
- Player save/load
- Quest state persistence
- Equipment + inventory persistence

### S7: Instanced Content
- Separate dungeon copies per player/group
- Dynamic instance spawning
- Server-side instance management

### S8: Extended Content
- More NPCs, quests, areas
- Additional skills
- Economy (item drops, trading)

---

## Blockers & Issues

| Issue | Severity | Status | Notes |
|-------|----------|--------|-------|
| Game artist availability | HIGH | 🟡 Pending | Needed for S4-025 (sprite work). Can proceed with placeholders. |
| Netty NIO complexity | MEDIUM | 🔲 Not started | May encounter Netty edge cases during S1-003. Mitigation: reference official examples. |
| GC pauses (later) | MEDIUM | 🟡 Planning | At scale (100+ players), GC pauses break 256-tick timing. Solution: ZGC (Java 21 feature). Not critical for MVP. |

---

## How to Update This File

**End of each work session:**
1. Mark completed tasks as ✅
2. Move in-progress tasks to 🟡 or 🟢 (review)
3. Add new blockers if discovered
4. Update time estimates if more accurate data available
5. Commit: `Update PROGRESS.md`

**Example:**
```
### S1-002: Server Tick Loop 🟡 → ✅
- ✅ **Complete** — Merged to main (commit abc1234)
- Time taken: 5 hours (estimate was 4-6)
- Blocker resolved: None
```

---

**Last check-in:** Project kickoff (2026-02-26)  
**Next check-in:** After S1-002 + S1-003 complete
