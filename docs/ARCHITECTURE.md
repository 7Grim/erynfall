# ARCHITECTURE.md - System Design (Server-First)

**Decision Date:** 2026-03-17  
**Approach:** Authority-Server from Day One (Option B)  
**Rationale:** See ULTRATHINK analysis in session notes

---

## Core Philosophy

**OSRS is fundamentally a server-authoritative game.**

Every action goes through the server:
1. **Client:** Player clicks tile / attacks enemy / interacts with NPC
2. **Server:** Validates action, calculates results (hit/miss/damage/XP/loot)
3. **Server:** Broadcasts results to all clients
4. **Client:** Renders the result

**No client-side logic for:**
- Damage calculation
- XP awards
- Loot generation
- Item tracking
- Economy transactions
- Skill progression

---

## Architecture Layers

### Layer 1: Server (Authority & Game Logic)

**Component:** `server/src/main/java/com/osrs/server/`

```
Server.java
  ↓
NettyServer.java (port 43594)
  ↓
ServerPacketHandler.java
  ↓
GameLoop.java (256 Hz tick rate)
  ↓
Game Systems:
  - CombatEngine.java (hit/miss/damage)
  - World.java (entities, tile map, state)
  - Pathfinding.java (BFS movement)
  - GameContent.java (quests, NPCs, bosses)
  - economy/TradeManager.java (GE, drops)
  - skills/SkillManager.java (XP, leveling)
  - quest/QuestManager.java (quest state)
  - combat/CombatEngine.java (combat logic)
```

**Responsibilities:**
- ✅ Authority: Source of truth for all game state
- ✅ Validation: All actions server-side validated
- ✅ Tick processing: 256 Hz game loop
- ✅ State management: World, players, entities, loot
- ✅ Network: Netty packet handling + broadcasting

**What runs here:**
- Combat calculations (deterministic, server-seeded RNG)
- XP awards (4 per damage melee, 2 per damage magic)
- Loot generation (rarity tables, drop rates)
- Skill progression (XP thresholds, level-ups)
- Quest state (objectives, completion, rewards)
- Economy (trades, price discovery, GE)
- Pathfinding (BFS to destination)
- Collision detection

---

### Layer 2: Client (Rendering & Input)

**Component:** `client/src/main/java/com/osrs/client/`

```
Client.java (LibGDX)
  ↓
GameScreen.java
  ↓
NettyClient.java (connects to localhost:43594 or remote IP)
  ↓
UI Systems:
  - InventoryUI.java (display inventory)
  - CombatUI.java (damage numbers, log)
  - DialougeUI.java (quest/NPC dialogue)
  - StatsUI.java (stats panel)
  ↓
Rendering:
  - IsometricRenderer.java (tiles + entities)
  - CoordinateConverter.java (screen ↔ world)
  ↓
Input:
  - Right-click handling (context menu)
  - Left-click handling (default action)
```

**Responsibilities:**
- ✅ Rendering: Display server state
- ✅ Input handling: Capture right-click/left-click
- ✅ UI: Inventory, stats, chat, combat log
- ✅ Network: Send packets to server (WalkTo, Attack, etc.)
- ✅ Prediction: Optional client-side movement prediction (for smoothness, but server validates)

**What runs here:**
- Rendering (tiles, entities, UI elements)
- Input capture (mouse clicks)
- UI state (which panel is open, inventory slots)
- **Optional:** Smooth movement prediction (client guesses destination while waiting for server pathfind result)

**What DOES NOT run here:**
- Combat calculations
- XP awards
- Loot generation
- Skill progression
- Inventory validation
- Trade validation
- Quest state changes

---

### Layer 3: Shared (Serialization & Data Models)

**Component:** `shared/src/main/java/com/osrs/`

```
protocol/network.proto (Protocol Buffers)
  ↓ (auto-generates NetworkProto.java)
  ↓
Packet types:
  - Handshake / HandshakeResponse
  - PlayerMovement
  - WalkTo
  - Attack
  - CombatHit
  - HealthUpdate
  - SkillXP
  - QuestUpdate
  - DialogueMessage / DialogueResponse
  - InventoryUpdate
  - TradeOffer
  - ... (more as features added)
  ↓
Data Models:
  - Entity.java (base entity)
  - Player.java (extends Entity)
  - NPC.java (extends Entity)
  - Item.java (item definition)
  - Stats.java (skill levels + XP)
  - Inventory.java (item slots)
```

---

## Data Flow Diagram

### Example: Combat Attack Flow

```
Client (LibGDX)                    Server (Netty + GameLoop)
     ↓                                       ↓
Player right-clicks NPC                     ↓
     ↓                                       ↓
ContextMenu shows "Attack"                  ↓
     ↓                                       ↓
Player clicks "Attack"                      ↓
     ↓                                       ↓
Client sends Attack(targetId) packet →→→→→ Server receives in tick N
     ↓                                       ↓
                                      GameLoop.processTick(N):
                                        1. Dequeue Attack packet
                                        2. Validate: attacker in range?
                                        3. CombatEngine.calculateHit()
                                           - RNG seeded by (tick + attacker + target)
                                           - Deterministic result
                                        4. Award XP if hit
                                        5. Award loot if kill
                                        6. Create CombatHit packet
     ↓                                       ↓
Client receives CombatHit packet ←←←← Server broadcasts CombatHit packet
     ↓                                       ↓
CombatUI displays damage number             ↓
     ↓                                       ↓
Player sees "-47" floating above NPC        ↓
     ↓                                       ↓
```

**Key principle:** Server does all calculations. Client just displays results.

---

## Network Protocol Design

### Message Flow (Request-Response Pattern)

**Type 1: Client → Server (Request)**
```protobuf
WalkTo { target_x, target_y }
Attack { target_id }
DialogueResponse { npc_id, option_id }
```

**Type 2: Server → Client (Broadcast)**
```protobuf
CombatHit { attacker_id, target_id, damage, hit }
HealthUpdate { entity_id, health, max_health }
SkillXP { skill_id, xp_awarded, new_level? }
EntityUpdate { entity_id, x, y, animation_frame, health }
DialogueMessage { npc_id, npc_text, options[] }
```

**Type 3: Bi-Directional (State Sync)**
```protobuf
WorldState { entities[], updates[] }
InventoryUpdate { slot, item_id, quantity }
```

---

## Solo vs Multiplayer Transition

### Current State (Solo Development)
```
Player's Machine:
  ├─ Server.java (Netty, localhost:43594)
  │  └─ GameLoop (256 Hz)
  │  └─ CombatEngine, World, etc.
  └─ Client.java (LibGDX)
     └─ Connects to localhost:43594
     └─ Tests all game systems
```

### Multiplayer State (No Changes to Code)
```
Cloud Server (AWS/Digital Ocean):
  └─ Server.java (Netty, public IP)
     └─ GameLoop (256 Hz)
     └─ CombatEngine, World, etc.

Player 1's Machine:
  └─ Client.java
     └─ Connects to cloud IP

Player 2's Machine:
  └─ Client.java
     └─ Connects to cloud IP
```

**What changes?**
- Server IP/port config (from localhost to public IP)
- Database connection (SQLite → PostgreSQL, eventually)
- Load balancing (if 1000+ players, use multiple servers)

**What stays the same?**
- All game logic (CombatEngine, World, etc.)
- All protocol messages
- All client rendering
- All validation

---

## Tick-Based Authority Pattern

### Why 256 Hz?

1. **OSRS uses 600ms ticks = ~1.67 Hz.** We use 256 Hz for finer granularity.
2. **More ticks = smoother feel, better lag compensation.**
3. **Matches modern MMO standards** (typical: 60-256 Hz).

### Tick Loop Structure

```java
while (running) {
  long startTime = System.nanoTime();
  
  // Stage 1: Dequeue input packets (from clients)
  dequeueAndProcessPackets();
  
  // Stage 2: Update entity positions (pathfinding)
  updateMovement();
  
  // Stage 3: Combat calculations
  processCombat();
  
  // Stage 4: Skill progression (XP awards)
  processSkills();
  
  // Stage 5: Loot generation (drop items)
  processLoot();
  
  // Stage 6: Broadcast state to all clients
  broadcastWorldState();
  
  // Sleep until next tick (3.9ms)
  long elapsed = System.nanoTime() - startTime;
  sleep(TICK_INTERVAL_NS - elapsed);
}
```

**Every calculation happens once per tick, deterministically.**

---

## Authority Validation Checklist

**For every action, server must validate:**

- ✅ Is attacker in range of target? (prevent attacking across map)
- ✅ Is target alive? (prevent double-killing)
- ✅ Does attacker have required Attack level? (prevent low-level using high-level weapons)
- ✅ Does attacker have the weapon equipped? (prevent naked attacks)
- ✅ Is target a valid entity? (prevent attacking objects)
- ✅ Did the attack land? (RNG check, server-side)
- ✅ How much damage? (Strength + equipment + RNG)
- ✅ Grant XP? (4 per damage, skill-based)
- ✅ Did target die? (health ≤ 0)
- ✅ Drop loot? (rarity table check)
- ✅ Award bones for Prayer? (higher-level enemies = better bones)

**If ANY check fails, request is rejected. Server is source of truth.**

---

## Scalability Path

### Phase 1: Solo (Current)
- 1 server, 1 client
- Game logic complete
- Runs on single machine

### Phase 2: Friends (Post-MVP)
- 1 server, 4-8 clients
- Deploy to LAN or cloud VPS
- Single database (PostgreSQL)
- No changes to code

### Phase 3: Community (1000+ players)
- Multiple servers (shards)
- Each server: 100-500 players
- Shared database or per-server DB
- Server selection UI (pick which server to join)
- Minimal changes to code

### Phase 4: Enterprise (Official OSRS-Scale)
- Load balancers
- Replication
- Caching layer (Redis)
- CDN for assets
- Trading between servers (if desired)

**The key: Phase 1 architecture supports all future phases without major refactors.**

---

## File Structure (Multi-Module Maven)

```
osrs-mmorp/
├── pom.xml (parent)
├── server/
│   ├── pom.xml
│   └── src/main/java/com/osrs/server/
│       ├── Server.java
│       ├── GameLoop.java
│       ├── World.java
│       ├── combat/CombatEngine.java
│       ├── skills/SkillManager.java
│       ├── quest/QuestManager.java
│       ├── network/ServerPacketHandler.java
│       └── ... (game systems)
├── client/
│   ├── pom.xml
│   └── src/main/java/com/osrs/client/
│       ├── Client.java
│       ├── GameScreen.java
│       ├── ui/InventoryUI.java
│       ├── renderer/IsometricRenderer.java
│       ├── network/NettyClient.java
│       └── ... (rendering systems)
├── shared/
│   ├── pom.xml
│   ├── src/main/proto/network.proto
│   └── src/main/java/com/osrs/
│       ├── Entity.java
│       ├── Player.java
│       ├── NPC.java
│       └── ... (shared models)
└── docs/
    ├── ARCHITECTURE.md (this file)
    ├── S2-COMBAT-IMPLEMENTATION.md
    ├── PROGRESS.md
    └── CONTRIBUTING.md
```

---

## Next Steps

1. ✅ Verify Server + Client can run together (localhost connection)
2. ✅ Finalize all MVP systems (combat, skills, economy)
3. ✅ Continue discovery questions (Q2.3, Q2.4, Q2.5)
4. ✅ Create detailed tech stack & database schema
5. ✅ Start implementation with server-first mindset

---

**Status:** LOCKED IN - Server-First Architecture, Authority-Validated, Scalable to MMO

