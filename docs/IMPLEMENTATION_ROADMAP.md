# IMPLEMENTATION_ROADMAP.md - Complete Development Plan

**Status:** LOCKED IN - Ready to begin development  
**Last Updated:** 2026-03-17  
**Total Discovery Documents:** 12 (VISION → PLAYER_PSYCHOLOGY)

---

## EXECUTIVE SUMMARY

**Project:** OSRS-MMORP (Old School RuneScape MMO Clone)  
**MVP Scope:** 99% feature-complete single-player game  
**Timeline:** Open-ended (quality > speed)  
**Architecture:** Server-first (Netty + Protocol Buffers), authority-validated, scalable to thousands of players  
**Tech Stack:** Java 21, Maven, LibGDX, PostgreSQL

**MVP Feature Set:**
- 8 core skills (Attack, Strength, Defence, Magic, Prayer, Woodcutting, Fishing, Cooking)
- Full economy (GE + direct trading)
- Main quest + 3 side quests
- Chat (public + private) + trading (GE + P2P)
- Achievements + seasonal events
- Stats lookup (hiscores)

---

## SPRINT BREAKDOWN (Estimated)

### S0: Infrastructure (Week 1-2)
**Goal:** Set up server, client, and database

**Tasks:**
- [ ] Verify Maven multi-module builds (clean compile on Windows)
- [ ] Regenerate Protocol Buffers (network.proto → NetworkProto.java)
- [ ] Set up PostgreSQL schema (players, items, NPCs, quests, inventory, GE orders)
- [ ] Implement base Entity system (Player, NPC classes)
- [ ] Verify GameLoop runs at 256 Hz locally
- [ ] Implement login/handshake (client connects to localhost)
- [ ] Create basic Isometric renderer test
- [ ] Git workflow verification

**Completion:** Server starts, client connects, world loads empty map

---

### S1: Core Movement & Rendering (Week 2-3)
**Goal:** Players can move around the world and see the environment

**Tasks:**
- [ ] Load map from YAML (Lumbridge + Tutorial Island layouts)
- [ ] Implement right-click pathfinding (BFS algorithm)
- [ ] Broadcast PlayerMovement packets
- [ ] Render tiles + entities in isometric view
- [ ] Test NPC spawning
- [ ] Implement basic context menu (right-click on entities)
- [ ] Verify 28-slot inventory UI

**Completion:** Walk around world, see NPCs, right-click to move

---

### S2: Combat System (Week 3-5)
**Goal:** Kill enemies, receive loot, gain XP

**Tasks:**
- [ ] Integrate CombatEngine into GameLoop.processTick()
- [ ] Implement hitsplat display (red/white damage numbers, fade animation)
- [ ] Add attack/miss sound effects
- [ ] Implement enemy death animations (crouch/fade)
- [ ] Broadcast CombatHit packets
- [ ] Implement loot drops (bones + meat + feathers, RAW ONLY)
- [ ] Test loot pickup (right-click ground)
- [ ] Implement XP awards (4 per damage)
- [ ] Update Stats class with XP tracking
- [ ] Test level-ups

**Completion:** Kill chicken → get bones + raw chicken + feathers, gain XP, level up

---

### S3: Skills (Week 5-7)
**Goal:** All 8 skills trainable, level progression working

**Tasks:**
- [ ] Implement Woodcutting (chop trees → logs)
- [ ] Implement Fishing (catch fish at spots)
- [ ] Implement Cooking (cook raw fish → cooked food)
- [ ] Implement Prayer (bury bones → XP)
- [ ] Implement attack style selection (UI menu)
- [ ] Add Prayer points system (max = level, drain per tick)
- [ ] Add prayer restoration (altars + potions)
- [ ] Test all 8 skills leveling together
- [ ] Verify XP curve (level 92 = 50% to 99)

**Completion:** All skills trainable, progression working correctly

---

### S4: Economy & Banking (Week 7-9)
**Goal:** Grand Exchange working, banking functional, items tradeable

**Tasks:**
- [ ] Implement bank system (deposit/withdraw)
- [ ] Implement GE order book (buy/sell offers)
- [ ] Implement auto-matching algorithm
- [ ] Implement 2% fee calculation
- [ ] Implement quantity limits (prevent manipulation)
- [ ] Implement direct P2P trading (double-accept)
- [ ] Implement inventory space management (28-slot hard limit)
- [ ] Test trading scenarios (GE + P2P)
- [ ] Add trade notifications to chat

**Completion:** Buy/sell items via GE or direct trading, banking works

---

### S5: Quests & Content (Week 9-12)
**Goal:** Main quest + 3 side quests playable, boss encounters working

**Tasks:**
- [ ] Design quest structure (YAML config files)
- [ ] Implement quest journal UI
- [ ] Implement objective tracking
- [ ] Create Tutorial Island quest
- [ ] Create main quest (Dragon Slayer equivalent)
- [ ] Create 3 side quests (Novice/Intermediate)
- [ ] Implement boss encounters (Elvarg, quest bosses)
- [ ] Implement quest completion + rewards
- [ ] Test quest progression (32 quest points requirement)

**Completion:** Complete all quests, defeat final boss

---

### S6: Social Systems (Week 12-13)
**Goal:** Chat + trading functional, stats lookup working

**Tasks:**
- [ ] Implement public chat (broadcast to all players)
- [ ] Implement private chat (DMs)
- [ ] Add ignore list (mute players)
- [ ] Implement hiscores database (player stats)
- [ ] Implement stats lookup command (`/stats PlayerName`)
- [ ] Test chat messages + trading notifications
- [ ] Add leaderboards (optional, basic top 100)

**Completion:** Chat with other solo players, look up stats

---

### S7: Achievements & Events (Week 13-14)
**Goal:** Achievement system + seasonal events framework

**Tasks:**
- [ ] Create achievement log UI
- [ ] Implement achievement tracking (5-6 per skill, 2-3 economy achievements)
- [ ] Create seasonal event framework (YAML-driven)
- [ ] Implement first seasonal event (Christmas)
- [ ] Add event quest mechanics
- [ ] Add event cosmetic rewards
- [ ] Test achievement unlock notifications

**Completion:** Earn achievements, seasonal events work

---

### S8: Polish & Testing (Week 14-16)
**Goal:** Everything working, optimized, tested

**Tasks:**
- [ ] Performance profiling (256 Hz tick loop optimization)
- [ ] Fix all edge cases in combat (multiple hits, simultaneous deaths)
- [ ] Fix inventory edge cases (full inventory, item overflow)
- [ ] Test full quest progression (1-99 grinding)
- [ ] Test economy edge cases (GE limits, fee calculations)
- [ ] Balance enemy XP rates (verify progression pacing)
- [ ] Final UI polish
- [ ] Documentation (CONTRIBUTING.md, setup guides)
- [ ] Tag v1.0 on GitHub

**Completion:** MVP complete, ready for long-term solo testing

---

## DEPENDENCY MAP (What Must Come First)

```
S0 (Infrastructure)
  ↓
S1 (Movement)
  ↓
S2 (Combat) ← depends on S1 (entities)
  ↓
S3 (Skills) ← depends on S2 (XP system)
  ↓
S4 (Economy) ← depends on S3 (items to trade)
  ↓
S5 (Quests) ← depends on S4 (rewards), S2 (bosses), S3 (XP)
  ↓
S6 (Social) ← independent, can run in parallel
  ↓
S7 (Achievements) ← depends on S3 (achievements tracked)
  ↓
S8 (Polish) ← final integration testing
```

---

## CORE SYSTEMS (By Implementation Order)

### 1. GameLoop & Tick System (S0)
**File:** `server/src/main/java/com/osrs/server/GameLoop.java`

**Responsibilities:**
- 256 Hz tick rate (3.9ms per tick)
- Dequeue packets from clients
- Process movement (BFS pathfinding)
- Process combat (CombatEngine)
- Process skills (XP awards)
- Broadcast world state

**Key Methods:**
```java
public void processTick(long tickNumber) {
  // Stage 1: Dequeue client packets
  dequeuePackets();
  
  // Stage 2: Update movement
  updateMovement();
  
  // Stage 3: Process combat
  processCombat();
  
  // Stage 4: Award XP / skill progression
  processSkills();
  
  // Stage 5: Loot + drops
  processLoot();
  
  // Stage 6: Broadcast state
  broadcastWorldState();
}
```

---

### 2. Combat Engine (S2)
**File:** `server/src/main/java/com/osrs/server/combat/CombatEngine.java`

**Responsibilities:**
- Calculate hit/miss (server-side RNG)
- Calculate damage (Strength + equipment)
- Award XP (4 per damage)
- Generate loot (bones + meat)
- Handle enemy death

**Key Methods:**
```java
public HitResult calculateHit(Entity attacker, Entity target, int tick) {
  // Server-seeded RNG: seed = tick + attacker_id + target_id
  // Deterministic result
  
  if (isHit) {
    int damage = calculateDamage(attacker, target);
    return new HitResult(damage, true);
  }
  return new HitResult(0, false);
}

public void handleKill(Entity victim) {
  // Drop loot (bones + meat)
  // Create loot items on ground
  // Broadcast to clients
}
```

---

### 3. Skill System (S3)
**File:** `server/src/main/java/com/osrs/server/skills/Stats.java`

**Responsibilities:**
- Track XP per skill (8 skills)
- Calculate levels from XP (formula: exponential)
- Award XP on actions
- Trigger level-up events

**Key Methods:**
```java
public void addExperience(SkillType skill, int xp) {
  skills[skill].addXP(xp);
  
  if (shouldLevelUp(skill)) {
    levelUp(skill);
    broadcastLevelUpNotification();
  }
}

public int getLevel(SkillType skill) {
  return skills[skill].getLevel(); // 1-99
}
```

---

### 4. Inventory System (S4)
**File:** `server/src/main/java/com/osrs/server/inventory/Inventory.java`

**Responsibilities:**
- 28 slots (hard limit)
- Stack management (stackable items)
- Weight calculation
- Validation (server-side, not client)

**Key Methods:**
```java
public void add(Item item) throws InventoryFullException {
  if (isFull()) throw new InventoryFullException();
  slots[nextFree] = item;
}

public void remove(int slotIndex) {
  slots[slotIndex] = null;
}

public int getTotalWeight() {
  // Sum of all item weights
}
```

---

### 5. Grand Exchange (S4)
**File:** `server/src/main/java/com/osrs/server/economy/GrandExchange.java`

**Responsibilities:**
- Manage buy/sell order book
- Auto-match orders
- Calculate fees (2%)
- Enforce quantity limits

**Key Methods:**
```java
public void placeOffer(GEOffer offer) {
  // Validate: item exists, quantity ≤ limit, player has resources
  // Add to order book
  // Attempt matches
  // Remove offer if fully filled
}

private void attemptMatches(Item item) {
  // Sort buy orders (highest price first)
  // Sort sell orders (lowest price first)
  // Match when buy ≥ sell
  // Execute trades, apply 2% fee
}
```

---

### 6. Quest System (S5)
**File:** `server/src/main/java/com/osrs/server/quest/QuestManager.java`

**Responsibilities:**
- Track quest progress per player
- Validate requirements (32 quest points, skill levels)
- Process quest objectives
- Award quest rewards

**Key Methods:**
```java
public void startQuest(Player player, QuestId questId) {
  // Validate player meets requirements
  // Create quest session
  // Add to quest journal
}

public void completeQuest(Player player, QuestId questId) {
  // Award XP
  // Award quest points
  // Update player stats
  // Unlock equipment
}
```

---

### 7. Chat System (S6)
**File:** `server/src/main/java/com/osrs/server/social/ChatManager.java`

**Responsibilities:**
- Broadcast public messages
- Route private messages
- Manage ignore lists
- Validate message content

**Key Methods:**
```java
public void broadcastPublic(Player sender, String message) {
  // Validate message
  // Send to all online players except those who ignore sender
}

public void sendPrivate(Player sender, Player target, String message) {
  // Validate both players online
  // Route to target
  // Log message
}
```

---

### 8. Hiscores (S6)
**File:** `server/src/main/java/com/osrs/server/social/Hiscores.java`

**Responsibilities:**
- Track player stats
- Calculate rankings
- Provide stats lookup

**Key Methods:**
```java
public PlayerStats getStats(String playerName) {
  // Return all skill levels + overall rank
  // Can be null if player not found
}

public int getRank(SkillType skill, int level) {
  // Return rank (1 = best) for skill at level
}
```

---

## DATABASE SCHEMA (PostgreSQL)

### Players Table
```sql
CREATE TABLE players (
  id SERIAL PRIMARY KEY,
  username VARCHAR(12) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  created_at TIMESTAMP DEFAULT NOW(),
  
  -- Stats (8 skills)
  attack_xp BIGINT DEFAULT 0,
  strength_xp BIGINT DEFAULT 0,
  defence_xp BIGINT DEFAULT 0,
  magic_xp BIGINT DEFAULT 0,
  prayer_xp BIGINT DEFAULT 0,
  woodcutting_xp BIGINT DEFAULT 0,
  fishing_xp BIGINT DEFAULT 0,
  cooking_xp BIGINT DEFAULT 0,
  
  -- Other
  gold BIGINT DEFAULT 0,
  questpoints INT DEFAULT 0,
  prayer_points INT DEFAULT 0
);
```

### Inventory Table
```sql
CREATE TABLE inventory (
  id SERIAL PRIMARY KEY,
  player_id INT NOT NULL REFERENCES players(id) ON DELETE CASCADE,
  slot_index INT (0-27),
  item_id INT NOT NULL,
  quantity INT DEFAULT 1,
  
  UNIQUE(player_id, slot_index)
);
```

### Grand Exchange Table
```sql
CREATE TABLE ge_orders (
  id SERIAL PRIMARY KEY,
  player_id INT NOT NULL REFERENCES players(id) ON DELETE CASCADE,
  item_id INT NOT NULL,
  quantity INT NOT NULL,
  price_per_unit INT NOT NULL,
  is_buy BOOLEAN NOT NULL,
  created_at TIMESTAMP DEFAULT NOW(),
  completed_at TIMESTAMP,
  
  INDEX (item_id, is_buy)
);
```

### Quests Table
```sql
CREATE TABLE player_quests (
  id SERIAL PRIMARY KEY,
  player_id INT NOT NULL REFERENCES players(id) ON DELETE CASCADE,
  quest_id INT NOT NULL,
  started_at TIMESTAMP DEFAULT NOW(),
  completed_at TIMESTAMP,
  
  UNIQUE(player_id, quest_id)
);
```

---

## PROTOCOL UPDATES (network.proto)

**New messages to add:**

```protobuf
message CombatHit {
  int32 attacker_id = 1;
  int32 target_id = 2;
  int32 damage = 3;
  bool is_hit = 4;
  int64 timestamp = 5;
}

message HealthUpdate {
  int32 entity_id = 1;
  int32 current_health = 2;
  int32 max_health = 3;
}

message SkillXP {
  enum Skill {
    ATTACK = 0;
    STRENGTH = 1;
    DEFENCE = 2;
    MAGIC = 3;
    PRAYER = 4;
    WOODCUTTING = 5;
    FISHING = 6;
    COOKING = 7;
  }
  Skill skill = 1;
  int32 xp_gained = 2;
  int32 new_level = 3;
}

message ChatMessage {
  int32 sender_id = 1;
  string sender_name = 2;
  string message = 3;
  enum ChatType {
    PUBLIC = 0;
    PRIVATE = 1;
  }
  ChatType type = 4;
  int32 recipient_id = 5; // For PRIVATE
}

message LootDrop {
  int32 item_id = 1;
  int32 quantity = 2;
  int32 x = 3;
  int32 y = 4;
  int64 timestamp = 5;
  // Items disappear from ground after 1 hour
}

message GEOrder {
  int32 order_id = 1;
  int32 item_id = 2;
  int32 quantity = 3;
  int32 price = 4;
  bool is_buy = 5;
  int64 created_at = 6;
}

message QuestUpdate {
  int32 quest_id = 1;
  enum QuestStatus {
    NOT_STARTED = 0;
    IN_PROGRESS = 1;
    COMPLETED = 2;
  }
  QuestStatus status = 2;
  repeated int32 objective_indices = 3; // Which objectives completed
}
```

---

## GIT WORKFLOW

### Branch Strategy
- **main:** Production code (stable builds)
- **develop:** Integration branch (all sprints merge here)
- Feature branches: `feature/S2-combat`, `feature/S3-skills`, etc.

### Commit Style
```
Format: `[S#] Brief description`
Examples:
  [S1] Add right-click pathfinding
  [S2] Implement CombatEngine integration
  [S3] Add Prayer system
  
No Co-authored-by footers.
No Claude attribution.
Keep commits atomic (one feature per commit).
```

### Code Organization
```
osrs-mmorp/
├── server/
│   ├── src/main/java/com/osrs/server/
│   │   ├── Server.java (entry point)
│   │   ├── GameLoop.java (tick processor)
│   │   ├── World.java (entity manager)
│   │   ├── combat/
│   │   │   └── CombatEngine.java
│   │   ├── skills/
│   │   │   └── Stats.java
│   │   ├── inventory/
│   │   │   └── Inventory.java
│   │   ├── economy/
│   │   │   ├── GrandExchange.java
│   │   │   └── TradeManager.java
│   │   ├── quest/
│   │   │   └── QuestManager.java
│   │   ├── social/
│   │   │   ├── ChatManager.java
│   │   │   └── Hiscores.java
│   │   └── network/
│   │       └── ServerPacketHandler.java
│   └── pom.xml
├── client/
│   ├── src/main/java/com/osrs/client/
│   │   ├── Client.java (entry point)
│   │   ├── GameScreen.java (game loop)
│   │   ├── ui/
│   │   │   ├── InventoryUI.java
│   │   │   ├── ChatUI.java
│   │   │   └── QuestJournalUI.java
│   │   ├── renderer/
│   │   │   └── IsometricRenderer.java
│   │   └── network/
│   │       └── NettyClient.java
│   └── pom.xml
├── shared/
│   ├── src/main/proto/
│   │   └── network.proto
│   └── pom.xml
├── docs/
│   ├── ARCHITECTURE.md
│   ├── CONTRIBUTING.md
│   └── FIRST_PLAYTEST_SETUP.md
└── pom.xml (parent)
```

---

## TESTING STRATEGY

### Unit Tests (Per Sprint)
- CombatEngine: Hit calculation determinism
- Stats: XP to level conversion
- Inventory: Stack management
- GrandExchange: Order matching logic

### Integration Tests (After Each Sprint)
- Server + Client communication
- GameLoop tick accuracy
- Full combat flow (kill → loot → XP)
- Skill progression end-to-end

### Manual Testing (S8 Polish)
- Solo play: Level multiple skills to 99
- Economy: Buy/sell via GE and direct trading
- Quests: Complete all quests
- Social: Chat + hiscores lookup

---

## SUCCESS CRITERIA

### MVP Completion
- [ ] All 8 skills trainable to level 99
- [ ] Main quest + 3 side quests completable
- [ ] Combat: Kill enemies → loot + XP
- [ ] Economy: Trade via GE + direct P2P
- [ ] Chat: Send public + private messages
- [ ] Hiscores: Lookup any player's stats
- [ ] Achievements: Earn 10+ achievements
- [ ] Seasonal: 1 event playable (Christmas)
- [ ] Zero crashes in 8-hour solo session

### Performance Targets
- 256 Hz tick loop maintained (99%+ uptime)
- Client FPS: 60+ fps on mid-range laptop
- Network latency: <100ms command→response
- Memory usage: Server <2GB, Client <1GB

---

## NEXT IMMEDIATE ACTIONS

1. **Verify Windows Build**
   - `mvnw.cmd clean compile` must succeed
   - Protobuf regeneration must complete

2. **Create Initial World YAML**
   - Tutorial Island map layout
   - Lumbridge map layout
   - Basic NPCs

3. **Begin S0 Tasks**
   - Database schema creation
   - Schema migrations setup
   - GameLoop verification at 256 Hz

4. **Git Setup**
   - Create `develop` branch
   - Start feature branch for S0

---

**Status:** READY TO BEGIN DEVELOPMENT  
**Target Start:** Immediately after Windows build verification  
**Estimated MVP Completion:** 12-16 weeks (depends on available hours/week)

