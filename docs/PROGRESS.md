# PROGRESS.md - Sprint Tracking

**Last Updated:** 2026-04-03 (schema cutover, XP tenths, full Woodcutting, skill guide shell, and bank tabs/search shipped)

**Current Sprint:** S4 — Woodcutting + banking + reusable skill-guide polish shipped; Fishing/Cooking next

**Team:** victorystyle (lead dev), game artist (TBD)

**Work Summary 2026-04-03 (S4 banking + Woodcutting systems polish):**
- ✅ Database schema cut over from `osrs` live objects to `erynfall`; runtime/config updated to use `erynfall`
- ✅ XP precision migrated to fixed-point tenths across the game so authentic fractional XP is possible
- ✅ Shared `WoodcuttingRegistry` added as the source of truth for standard axes, trees, XP, and success data
- ✅ Full pre-Forestry-style Woodcutting progression implemented:
  - standard tree progression
  - standard axe progression
  - correct item/log variants
  - tree-specific progression data
- ✅ Large skill guide modal added to replace the tiny click-popup; hover tooltip kept for quick XP/status
- ✅ Woodcutting guide now has OSRS-style sections:
  - Introduction
  - Trees
  - Axes
- ✅ Bank foundation implemented:
  - banker NPC access
  - character-specific persistent bank
  - deposit / withdraw
  - amount modes
  - bank drag/rearrange
- ✅ Bank UI improved:
  - item icons restored via shared item icon renderer
  - bank/inventory drag behavior fixed
  - deterministic deposit-by-item-id behavior for non-stackables
- ✅ Bank tabs foundation implemented:
  - All tab
  - custom tabs
  - drag item to tab
  - persistent tab metadata
- ✅ Client-side bank search implemented:
  - local filtering over bank snapshot
  - zero server overhead
  - safe filtered view (rearranging disabled while searching)
**Known remaining banking hardening work:**
- ⏳ Atomic inventory+bank persistence for dirty bank mutations
- ⏳ Force-close bank on movement/combat with guaranteed flush of dirty state
- ⏳ Final relog/disconnect/autosave safety sweep for bank state

**Work Summary 2026-03-25 (S3 account auth, persistence, equipment bonuses, OSRS combat):**
- ✅ LoginScreen: OSRS-style login UI (username/password fields, Tab to switch, Enter to submit, asterisk masking)
- ✅ ErynfallGame: LibGDX Game wrapper — starts with LoginScreen, transitions to GameScreen on submit
- ✅ PlayerRepository: full DB auth — BCrypt login, auto-register on first play, DB-offline fallback to in-memory
- ✅ Server saves player XP + position on disconnect and every 60 seconds (autosave)
- ✅ Handshake sends all 6 skill levels to client on login (skills tab populates immediately)
- ✅ Stats.java deleted (was vestigial/unused — Player.java canonical skill tracking)
- ✅ Hitpoints XP bug fixed: new players now start at 1,154 XP (level 10) consistently
- ✅ Password security fix: Azure SQL password removed from server.yml; reads from DB_PASSWORD env var
- ✅ ItemDefinition expanded: full 14-field OSRS bonus model (stab/slash/crush/magic/ranged atk+def, meleeStr, rangedStr, magicDmg, prayer, weaponType)
- ✅ items.yaml updated: OSRS-accurate stats for all equipment (bronze sword +6 str, scimitar +14, shortbow, bronze arrows, shield, helm, platebody, platelegs)
- ✅ NPC combat stats added: attackLevel, strengthLevel, defenceLevel, attackBonus, strengthBonus, defenceBonus (defaults from combatLevel)
- ✅ EquipmentBonusCalculator: sums all 11 equipment slots per player each combat tick
- ✅ CombatEngine rewritten: exact OSRS hit-chance formula, max-hit formula for melee and ranged; replaces old simplified 50±bonus% approximation
- ✅ SQL migration created: sql/migrations/001_add_missing_skill_columns.sql (hitpoints_xp + ranged_xp)

**Pending Azure setup (must do before auth works on prod):**
- Rotate Azure SQL password (old one was in git history — treat as compromised)
- Run sql/migrations/001_add_missing_skill_columns.sql on erynfall database
- Set DB_PASSWORD environment variable before starting server

**Work Summary 2026-03-24 (S3 HUD + XP UI polish):**
- ✅ XpDropOverlay: OSRS-accurate yellow XP drops float upward on right side; simultaneous drops (e.g. Attack + Hitpoints) stacked vertically so they never overlap
- ✅ LevelUpOverlay: golden banner popup above chat box with queued display (5s, 0.8s fade); exact OSRS text wording
- ✅ Skills tab overhauled: 2-column grid (Attack/Str/Def left, Hitpoints/Ranged/Magic right); large yellow level numbers; renders in 3 GL passes (no per-cell begin/end spam)
- ✅ Skill XP tooltip on hover: shows current XP (formatted with commas), remaining XP to next level, gold progress bar — rendered as floating panel left of side panel
- ✅ Chat fully fixed: key range bug fixed (SPACE > Z), click-to-activate, own messages displayed immediately (optimistic), overhead text shows, server echo skipped to prevent duplicates
- ✅ Combat messages now route to chat box Game filter (removed separate renderMessages overlay that was overlapping chat box at y=80)
- ✅ HUD cleaned up: removed redundant Atk/Str/Def text (in skills tab), opponent info panel repositioned to h-105 with clear gap from HP bar
- ✅ Side panel widened to 240px (OSRS-authentic 249px); combat buttons no longer truncate "Aggressive"/"Controlled"

**Work Summary 2026-03-23 (S2 completion):**
- ✅ Fixed packet routing (ClientMessage/ServerMessage proto wrappers — all packets now routed correctly)
- ✅ Fixed coordinate conversion (camera.unproject + correct inverse isometric formula)
- ✅ Left-click-to-walk working with correct tile coordinates
- ✅ Smooth tile-by-tile movement (1 tile/600ms interpolation, OSRS accurate)
- ✅ Context menu rendered OSRS-style (dark navy panel, yellow "Choose Option" header, white options)
- ✅ HUD: HP bar (red fill), Attack/Strength/Defence levels, tile coords (bottom-right)
- ✅ Hitsplats: red/white floating damage numbers above entities, fade + float over 1.5s
- ✅ GameLoop broadcasts CombatHit + HealthUpdate packets to clients
- ✅ NPC health bars visible above NPCs when health < max
- ✅ XP table replaced with exact OSRS formula (Level 99 = 13,034,431 XP)
- ✅ Server connected to Azure SQL (erynfall.database.windows.net)

---

## 🚀 Your Next Move (Right Now)

```bash
# 1. Pull latest from main
git pull origin main

# 2. Create feature branch for S1-002
git checkout -b feature/s1-002-tick-loop

# 3. Edit GameLoop.java (see S1-002 section below for details)
# - Improve timing precision
# - Implement processTick() stub
# - Add better logging

# 4. Test: Run Server.java, watch for tick output for 30+ seconds
# Should see: Tick 256 (uptime: 1 sec), Tick 512 (uptime: 2 sec), etc.

# 5. If working: Commit + push + create PR (see docs/CONTRIBUTING.md)
```

**Estimated time:** 2-4 hours

---

---

## Executive Summary

**Sprint S1: COMPLETE** ✅

All 10 core systems implemented and integrated:
- ✅ 256-tick server loop (nanosecond precision)
- ✅ Netty TCP server (port 43594, packet framing)
- ✅ Client-server handshake + authentication
- ✅ Tile map loading from YAML
- ✅ Isometric renderer (104×104 grid visible)
- ✅ Player movement (arrow keys → server validation → network sync)
- ✅ NPC spawning + rendering

**What Works:**
- Server runs continuously, logs ticks
- Client connects, authenticates, receives world state
- Player moves with arrow keys; position synced to server
- NPCs visible on screen
- All systems networked via Protocol Buffers

**What's Next:** Finish bank hardening (atomic persistence + forced close safety), then build Fishing followed by Cooking using the same registry + guide-popup architecture proven by Woodcutting.

---

This document tracks:
1. **Completed work** — What's done and merged
2. **Current sprint** — What we're actively building
3. **Upcoming sprints** — What comes next
4. **Blockers** — Issues blocking progress

**Update Frequency:** Daily (end of work) or after PR merge

**Format:** Each task has:
- Status (🔲 Not started, 🟡 In progress, 🟢 In review, ✅ Complete)
- Deliverable summary
- Time taken
- Notes

---

## Sprint S1: Foundation (Weeks 1-4)
**Goal:** Tick loop, networking, world state, basic rendering

### S1-001: Maven Project Setup ✅
- ✅ **Complete** — All modules created, dependencies resolved
- **Deliverable:** Multi-module pom.xml, clean build
- **Time:** 1 hour
- **Merged:** Main branch (commit 42ebe00)
- **Verified:** Windows 11 + IntelliJ (server + client both run)
- **Notes:** Maven wrapper added for Windows support; LibGDX GUI works on Windows, headless mode for M2 Mac

### S1-002: Server Tick Loop (Proper Implementation) ✅
- ✅ **Complete** — Merged to main (commit b5a909b)
- **Deliverable:** 256-tick/sec loop with nanosecond timing, logging, documentation
- **Time:** 2 hours
- **Notes:** processTick() framework added for future stages (input, entity updates, combat)

#### What Needs Implementation

**In `GameLoop.java`:**

1. **Tick Processing Method**
   ```java
   private void processTick() {
       // Called once per tick (3.9ms interval)
       // TODO: Implement stages:
       // 1. Process input (placeholder for now)
       // 2. Update entities (placeholder for now)
       // 3. Calculate collisions (placeholder for now)
       // 4. Send deltas to clients (placeholder for now)
   }
   ```

2. **Timing Precision**
   - Use `System.nanoTime()` (not `System.currentTimeMillis()`)
   - Calculate sleep time: `long sleepNs = TICK_INTERVAL_NS - (now - lastTickNs)`
   - Handle tick overruns gracefully (log warning, don't skip)

3. **Logging**
   - Log tick count every 256 ticks (once per second)
   - Format: `"Tick {} (uptime: {} sec)"`
   - Log any tick overruns >10% (>4.3ms)

#### Acceptance Criteria

- [ ] Server runs continuously for ≥30 seconds without stopping
- [ ] Prints tick count every 1 second: `Tick 256 (uptime: 1 sec)` → `Tick 512 (uptime: 2 sec)` etc.
- [ ] No timing drift: Last tick at 30 sec ≈ 7680 ticks (≤1% variance)
- [ ] Shutdown on Ctrl+C exits cleanly within 2 seconds
- [ ] No exceptions in logs (only INFO + WARN)
- [ ] processTick() method exists (can be stubbed, will fill in later)

#### Testing Steps

```bash
git checkout -b feature/s1-002-tick-loop

# Edit GameLoop.java:
# 1. Implement processTick() method (can be empty or just comment)
# 2. Improve timing precision if needed

# Run server
# Open Server.java → click green play button

# Watch output for 30+ seconds:
# [INFO] Tick 256 (uptime: 1 sec)
# [INFO] Tick 512 (uptime: 2 sec)
# ... (should be consistent)

# Press Ctrl+C to stop
# Should see: [INFO] Game loop exited at tick XXXX
```

#### Success Indicators

✅ Ticks increment smoothly  
✅ One log line per second (no gaps, no doubles)  
✅ Shutdown is clean (no hanging)  
✅ No exceptions or errors in console  

#### Implementation Notes

- The stub `GameLoop.java` already exists (commit eac91f2)
- `processTick()` is a placeholder — you just need to ensure the loop runs correctly
- Focus on **timing precision** — this is the foundation
- No entity logic needed yet (that's S1-010)

#### Time Estimate: 2-4 hours
- 30 min: Understand current code
- 1 hour: Improve timing precision + logging
- 30 min: Testing + debugging
- 30 min: Clean up + document + commit

#### Blocker: None

#### Related Files
- `server/src/main/java/com/osrs/server/GameLoop.java` (main work)
- `server/src/main/java/com/osrs/server/Server.java` (initialization)

#### PR Checklist
- [ ] Branch name: `feature/s1-002-tick-loop`
- [ ] 3-5 commits (one per logical step)
- [ ] Tested for 30+ seconds (tick count verified)
- [ ] PROGRESS.md updated (mark S1-002 complete)
- [ ] PR description includes test results

### S1-003: Netty Server TCP Listener ✅
- ✅ **Complete** — Merged to main (commit b5a909b)
- **Deliverable:** Netty server listening on port 43594, packet framing, Protocol Buffers integration
- **Time:** 2 hours
- **Notes:** NettyServer, PlayerSession, ServerPacketHandler implemented; handles client connections + packet routing

### S1-004: Protocol Buffers Schema (Network Packets) ✅
- ✅ **Complete** — Merged to main (commit b5a909b)
- **Deliverable:** network.proto with Handshake, PlayerMovement, WorldState, EntityUpdate, etc.
- **Time:** 1 hour
- **Notes:** Schema supports 10+ packet types; auto-generates Java classes

### S1-005: Client Handshake (Connect to Server) ✅
- ✅ **Complete** — Merged to main (commit b5a909b)
- **Deliverable:** NettyClient, ClientPacketHandler; client connects, authenticates, receives world state
- **Time:** 2 hours
- **Notes:** Handshake protocol implemented; ready for entity sync

### S1-006: Player Entity (Data Model) ✅
- ✅ **Complete** — Merged to main (commit b5a909b)
- **Deliverable:** Entity, Player, NPC classes (shared); full stat/inventory/quest support
- **Time:** 1 hour
- **Notes:** Models extend Entity with player-specific properties (skills, equipment, inventory)

### S1-007: Tile Map (Tutorial Island) ✅
- ✅ **Complete** — Merged to main (commit b5a909b)
- **Deliverable:** TileMap class, YAML loading, collision queries (isWalkable)
- **Time:** 1.5 hours
- **Notes:** Supports 104×104 grid; tile data-driven from YAML

### S1-008: Isometric Renderer (LibGDX) ✅
- ✅ **Complete** — Merged to main (commit b5a909b)
- **Deliverable:** IsometricRenderer; tile grid (green outline), player (red square), NPCs (cyan circle)
- **Time:** 2 hours
- **Notes:** World-to-screen projection with 32×16 tile metrics; 60 FPS target

### S1-009: Player Movement (Validation + Sync) ✅
- ✅ **Complete** — Merged to main (commit b5a909b)
- **Deliverable:** Arrow key input → PlayerMovement packets → server validation + broadcast
- **Time:** 2 hours
- **Notes:** Client-side input handling; server-side movement validation; network sync

### S1-010: NPC Spawning + Idle Animation ✅
- ✅ **Complete** — Merged to main (commit b5a909b)
- **Deliverable:** Server spawns 5 NPCs; client renders placeholders; NPC data in YAML
- **Time:** 1.5 hours
- **Notes:** World.spawnNPC() method; NPCs rendered as cyan circles; ready for animation frames (S2+)

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

---

## Sprint S2: Combat Basics — COMPLETE ✅

### S2-011: Combat Engine ✅
- ✅ CombatEngine class (hit/miss/damage, deterministic RNG)
- ✅ Integrated into GameLoop.processTick() (attack speed, per-tick rolling)
- ✅ XP awards (Attack/Strength/Defence/Hitpoints per combat style)
- ✅ Server-side validation (authority-server model)

### S2-012: Combat UI ✅
- ✅ CombatUI: hitsplat circles (red/white) with floating damage numbers
- ✅ Combat messages → chat box system messages (Game filter)
- ✅ HP bar above NPCs (shows after first hit)
- ✅ Opponent info panel (top-left, name + level + HP bar)

### S2-013: Skill Progression ✅
- ✅ Stats class with exact OSRS XP table (level 99 = 13,034,431)
- ✅ Player skill tracking: Attack, Strength, Defence, Hitpoints, Ranged, Magic
- ✅ SkillUpdate packet: sends new_level + total_xp + leveled_up flag
- ✅ XpDropOverlay: OSRS yellow drops, right side, float up, no overlap
- ✅ LevelUpOverlay: golden banner with exact OSRS wording, queued, 5s display
- ✅ Skills tab: 2-column grid with hover tooltip (XP + remaining XP + progress bar)
- ⚠️ XP persists in-memory only — lost on server restart (needs account/persistence)

### S2-014: Inventory System ✅
- ✅ InventoryUI: 20-slot inventory grid, item pickup from world, right-click use/drop
- ✅ Ground items: server spawns + despawns; client shows name label
- ✅ Equipment slots (stubbed — no stat bonuses applied yet)

### S2-015: Quest System ✅
- ✅ Quest class + QuestManager (per-player quest state machine)
- ⚠️ Quest loading from YAML not wired (QuestManager exists, no YAML state tracked)

### S2-016: Dialogue System ✅
- ✅ DialogueUI renders NPC text + player response options
- ✅ TalkToNpc packet flow: approach → server validates → DialoguePrompt
- ⚠️ DialogueEngine per-player state not fully wired (placeholder responses)

---

## 🎉 S1 Complete Summary

**Timeline:** 2026-02-26 (kickoff) → 2026-03-16 (S1 complete)  
**Total Work:** ~17 hours of implementation + architecture  
**Commits:** 9 (setup + S1 systems)  

### What You Can Do Right Now

1. **Run server:** `Server.java` ▶️ (tick loop prints every second)
2. **Run client:** `Client.java` ▶️ (connects to server, shows tile grid)
3. **Move player:** Arrow keys (UP/DOWN/LEFT/RIGHT)
4. **See NPCs:** Cyan circles on screen (Tutorial Island NPCs)

### Code Quality

- ✅ No magic numbers (all in constants/YAML)
- ✅ Data-driven (quests, NPCs, dialogue in YAML)
- ✅ Logging throughout (easy to debug)
- ✅ Network protocol versioned (Protocol Buffers)
- ✅ Authority-server validation (client can't cheat)

### Ready for S2 (Combat Basics)

S2 implementation will hook into:
- `GameLoop.processTick()` → combat calculations
- `ServerPacketHandler.handlePlayerMovement()` → validate combat range
- `ClientPacketHandler` → display damage numbers
- YAML configs → load combat formulas

**Total work so far:** ~17 hours (solo dev pace: ~2 weeks at 10 hours/week)

---

**Last check-in:** S1 complete (2026-03-16)  
**Next sprint:** S2 (Combat Basics) — Weeks 5-8
