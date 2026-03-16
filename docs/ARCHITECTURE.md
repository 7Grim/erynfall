# OSRS MMORP Architecture

## System Overview

```
┌─────────────────────────────────────────────────────┐
│                   Client (LibGDX)                   │
│  ┌──────────────────────────────────────────────┐  │
│  │ Renderer (Isometric tiles, entities, UI)     │  │
│  │ Input Handler (keyboard, mouse)              │  │
│  │ Predictive State (player, inventory)         │  │
│  └──────────────────────────────────────────────┘  │
│  ▲                                                  │
│  │ Netty (TCP)                                      │
│  │ [Protocol Buffers serialization]                │
│  ▼                                                  │
│  ┌──────────────────────────────────────────────┐  │
│  │ Netty Handlers                               │  │
│  │ PacketDecoder → Game Logic → PacketEncoder   │  │
│  └──────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
                          │
                    (TCP Port 43594)
                          │
┌─────────────────────────────────────────────────────┐
│               Server (256-tick loop)                │
│  ┌──────────────────────────────────────────────┐  │
│  │ Tick Loop (runs 256x per second = 3.9ms)    │  │
│  │                                              │  │
│  │ Input Processing                            │  │
│  │  └─ Dequeue client packets                  │  │
│  │  └─ Validate moves, combat actions          │  │
│  │                                              │  │
│  │ Entity Updates                              │  │
│  │  └─ Update positions, animations            │  │
│  │  └─ NPC AI, pathfinding                     │  │
│  │  └─ Combat calculations                    │  │
│  │                                              │  │
│  │ State Synchronization                       │  │
│  │  └─ Delta updates to clients (only changed) │  │
│  │  └─ Queue outbound packets                  │  │
│  │                                              │  │
│  │ Memory & Database                           │  │
│  │  └─ Player state (HashMap, in-memory)       │  │
│  │  └─ World state (tiles, NPCs, items)        │  │
│  └──────────────────────────────────────────────┘  │
│  ▲                                                  │
│  │ Netty (TCP)                                      │
│  │ [Protocol Buffers serialization]                │
│  ▼                                                  │
│  ┌──────────────────────────────────────────────┐  │
│  │ Netty Handlers                               │  │
│  │ PacketDecoder → PlayerSession → PacketEncoder│  │
│  └──────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

## Tick Loop (Server-Side)

The server runs a deterministic tick-based loop at **256 ticks per second** (3.9ms per tick). This is non-negotiable — it matches OSRS and ensures consistent RNG seeding, combat calculations, and animation timing.

```
TICK 0: ─────────────────────────────────────────
  1. Dequeue all client input packets
  2. Validate player actions (move, attack, interact)
  3. Queue movement/action intents
  
TICK 1: ─────────────────────────────────────────
  1. Execute movement intents (update positions)
  2. Process NPC AI (pathfinding, idle actions)
  3. Update animations (frame counter)
  
TICK 2: ─────────────────────────────────────────
  1. Combat calculations (hit rolls, damage, XP)
  2. Skill progression (update XP totals)
  3. Apply environmental effects (projectiles, etc.)
  
TICK 3: ─────────────────────────────────────────
  1. Build delta updates (what changed since last broadcast)
  2. Serialize packets
  3. Send to all clients in viewport range
  4. Sleep remainder of 3.9ms
  
[repeat]
```

### Key Properties
- **Deterministic:** Same inputs → same outputs, every time
- **RNG seeded by tick:** Combat rolls use `tick % 256` as seed
- **No floating-point math:** All movement in tiles, not meters
- **Authority-server:** Client never trusts predicted damage or position without server validation

## Network Protocol

**Transport:** TCP over Netty (reliable, ordered delivery)

**Serialization:** Protocol Buffers 3 (compact, versioned, auto-generated)

### Packet Types

#### Client → Server
- `PlayerMovement` — Player position + direction (10 bytes)
- `PlayerAction` — Attack NPC, cast spell, pick up item, etc. (8 bytes)
- `DialogueResponse` — Player chose dialogue option (4 bytes)
- `InventoryAction` — Drop item, equip, etc. (12 bytes)

#### Server → Client
- `WorldState` — Delta update: entities added/removed/moved (variable)
- `EntityUpdate` — Single entity (position, animation, health) (16 bytes)
- `CombatHit` — Damage + XP feedback (12 bytes)
- `DialoguePrompt` — NPC dialogue + response options (variable)
- `InventoryUpdate` — Item added/removed (8 bytes)

**Typical update rate:** 10 packets/sec per client (once per 25 ticks)

## Entity Model (Server)

All entities inherit from `Entity`:

```
Entity
├── id (unique)
├── position (x, y, z)
├── facing (direction 0-7)
├── animation (current frame, total frames)
├── flags (visible, collidable, etc.)
└── ticksAlive (age in ticks)

Player extends Entity
├── stats (attack, strength, defence, hp, etc.)
├── inventory (20 slots, each holds item + quantity)
├── equipment (8 slots: head, body, legs, feet, hands, weapon, shield, ring)
├── quest state (map of quest ID → completion %)
├── experience (map of skill ID → total XP)
├── lastInput (tick N when last movement received)
└── sessionId (network connection ID)

NPC extends Entity
├── definition (template: sprites, dialogue, drops, behavior)
├── pathTarget (where NPC is walking to, if any)
├── currentDialogue (active dialogue state)
├── combatTarget (if in combat, who they're fighting)
└── respawnTick (when to respawn, if dead)
```

## Rendering Pipeline (Client)

### Isometric Projection

Each tile at world position `(x, y)` is rendered at screen position:
```
screenX = (x - y) * TILE_WIDTH / 2
screenY = (x + y) * TILE_HEIGHT / 2
```

Where `TILE_WIDTH = 32` and `TILE_HEIGHT = 16` pixels (standard isometric).

### Render Order (painter's algorithm)
1. Draw tiles (background-to-foreground, by Y then X)
2. Draw shadows
3. Draw entities (sorted by Y position for correct depth)
4. Draw UI overlays (dialogue, inventory, HUD)

### Camera
Fixed isometric view centered on player. No smooth camera tracking (too slow for sync issues).

## Data-Driven Systems

### World Map (`assets/data/map.yaml`)
```yaml
width: 104
height: 104
tiles:
  - id: 0
    name: Grass
    walkable: true
    spriteIndex: 42
  - id: 1
    name: Water
    walkable: false
    spriteIndex: 0
mapLayout: |
  0 0 0 1 1 0 0 0
  0 2 2 1 0 0 0 0
  ...
```

### NPC Definitions (`assets/data/npcs.yaml`)
```yaml
npcs:
  - id: 1
    name: Tutorial Guide
    spriteIndex: 512
    walkable: true
    dialogue:
      initial: "dialogue_tutorial_intro"
      responses:
        - "dialogue_tutorial_q1"
        - "dialogue_tutorial_q2"
    walkPath: [[50, 50], [52, 50], [54, 50]]
    walkSpeed: 2 tiles per tick
```

### Dialogue Trees (`assets/data/dialogue.yaml`)
```yaml
dialogues:
  dialogue_tutorial_intro:
    npcSays: "Hello adventurer! Welcome to Tutorial Island."
    options:
      - text: "Tell me about combat"
        next: "dialogue_combat_intro"
      - text: "How do I gain experience?"
        next: "dialogue_xp_intro"
  dialogue_combat_intro:
    npcSays: "In combat, you'll gain Attack, Strength, and Defence experience..."
    options:
      - text: "[Back]"
        next: "dialogue_tutorial_intro"
```

### Quests (`assets/data/quests.yaml`)
```yaml
quests:
  - id: 1
    name: Tutorial Quest
    tasks:
      - id: "speak_guide"
        type: "dialogue"
        npcId: 1
        dialogueId: "dialogue_tutorial_intro"
        reward: 10 exp
      - id: "kill_rat"
        type: "kill"
        npcId: 3
        count: 5
        reward: 50 exp
      - id: "collect_logs"
        type: "collect"
        itemId: 7
        count: 10
        reward: 100 exp
```

## Authority & Validation

**Golden Rule:** Server is always right. Client can predict for smoothness, but server has final say.

### Movement Validation
- Client sends intended position
- Server checks if path is walkable + not blocked by entities
- If invalid, server sends corrected position (client rewinds)
- If valid, server broadcasts to other players

### Combat Validation
- Client sends "attack NPC" intent
- Server verifies NPC is in range + attack target is valid
- Server rolls hit/miss using deterministic RNG (seeded by tick)
- Server calculates damage, updates XP
- Server broadcasts result to client

### Inventory Validation
- Client sends "drop item from slot 3"
- Server verifies item exists at slot 3
- Server creates dropped item in world
- Server sends acknowledgement (client UI updates)

## Performance Targets

### Server
- **CPU:** <5% on modern hardware (single-threaded)
- **Memory:** ~500 MB (player state + world state in-memory)
- **Network:** ~50 KB/sec outbound (256 ticks × ~200 bytes per packet)

### Client
- **Rendering:** 60 FPS (decoupled from server tick)
- **Memory:** ~200 MB (textures, UI, entity cache)
- **Network:** ~10 KB/sec inbound (delta updates + gameplay feedback)

## Scaling Path (Post-MVP)

| Phase | Bottleneck | Solution |
|-------|-----------|----------|
| **MVP** | Single world, all in-memory | Works fine for Tutorial Island |
| **v1.0** | Persistence | PostgreSQL, Redis cache for player state |
| **v1.5** | Concurrent players | World sharding (multiple servers, player routing) |
| **v2.0** | Instanced content | Separate server per instance (dungeon copies) |
| **v2.5** | GC pauses | ZGC (low-latency GC), off-heap data structures |

## Code Organization (Java Packages)

```
com.osrs.shared
├── protocol (generated from .proto files)
│   ├── PlayerMovement
│   ├── WorldState
│   └── ...
└── data
    ├── Entity
    ├── Player
    ├── NPC
    └── Item

com.osrs.server
├── Server (main + tick loop)
├── network
│   ├── NettyServer
│   ├── PacketHandler
│   └── PlayerSession
├── world
│   ├── World
│   ├── TileMap
│   └── EntityManager
├── player
│   ├── PlayerManager
│   ├── Stats
│   └── Inventory
├── npc
│   ├── NPCDefinition
│   ├── NPCManager
│   └── Pathfinding
├── quest
│   ├── Quest
│   ├── QuestManager
│   └── DialogueEngine
└── combat
    ├── CombatEngine
    ├── HitCalculator
    └── XPCalculator

com.osrs.client
├── Client (main + LibGDX)
├── network
│   ├── NettyClient
│   └── PacketHandler
├── renderer
│   ├── IsometricRenderer
│   ├── TextureAtlas
│   └── AnimationFrame
├── ui
│   ├── HUD
│   ├── DialogueUI
│   ├── InventoryUI
│   └── ChatBox
└── state
    ├── LocalPlayerState
    ├── WorldCache
    └── CameraController
```

---

**See also:** PROGRESS.md for sprint planning and task breakdown.
