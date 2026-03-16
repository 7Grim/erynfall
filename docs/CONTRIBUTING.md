# Contributing to OSRS MMORP

This document outlines how we work together asynchronously — minimal IDE chat, maximum clarity through documentation.

## Git Workflow

### Branch Naming

All branches off `main`. Use this format:

```
<type>/<sprint>-<task-id>-<description>
```

Examples:
- `feature/s1-001-tick-loop`
- `feature/s1-002-netty-server`
- `bugfix/s2-045-combat-desync`
- `docs/s1-999-architecture`

**Types:** `feature`, `bugfix`, `docs`, `test`, `refactor`

**Sprint:** Sprint number (s1, s2, etc.)

**Task ID:** From PROGRESS.md task list (3 digits, zero-padded)

**Description:** 2-3 words, kebab-case, what the work does

### Commits

Keep commits **atomic and descriptive**. One logical change per commit.

**Format:**
```
Add <what>

[Optional detailed explanation of why this change is needed,
what problem it solves, or implementation notes.]
```

**Examples:**
```
Add server tick loop at 256 ticks/sec

Tick loop runs continuously in main thread, processing input,
updating entities, and broadcasting deltas to clients. Uses
nanoTime() for sub-millisecond precision to avoid drift.

---

Add Protocol Buffers schema for PlayerMovement packet

Defines network packet for player movement (position + facing).
Serializes to ~10 bytes. Clients send once per move action.

---

Fix NPC pathfinding infinite loop

A* was not properly checking visited set, causing NPCs to
spin in place. Added HashSet<Tile> tracking.

---

Refactor combat hit calculation

Moved hardcoded constants into CombatConfig. Added unit tests.
No behavior change.
```

**DO NOT include:**
- "Co-authored-by" lines or any attribution
- IDE auto-generated headers ("// TODO: implement")
- Commit messages like "Update code" or "Work in progress"

### Pull Requests

**Before opening a PR:**
1. Ensure branch is up-to-date with `main`
2. Run `mvn clean test` locally (all tests pass)
3. Manually test your feature in IDE
4. Update PROGRESS.md with your changes
5. Add a brief description in the PR body

**PR Format:**
```
## Sprint S1, Task #002: Netty Server Setup

### What
Implements base Netty server infrastructure with TCP listening
on port 43594. Handles client connections, packet decoding,
and graceful shutdown.

### Why
Foundational networking layer required by all subsequent work.
Unblocks client development and protocol testing.

### Testing
- Server binds to port 43594 ✅
- Client connects successfully ✅
- Graceful shutdown on SIGTERM ✅
- Handles 10 concurrent connections ✅

### Related Files
- server/src/main/java/com/osrs/server/network/NettyServer.java
- shared/src/main/proto/network.proto

### Blockers
None
```

**Reviewer Checklist:**
- Does this match the task description in PROGRESS.md?
- Is the code readable and well-commented?
- Does it follow the architecture in ARCHITECTURE.md?
- Are there test cases for critical logic?
- Is the commit message clear?

**Approval & Merge:**
- One approval required (from main dev or reviewer)
- Squash and merge (keep main history clean)
- Close related issue in GitHub Projects

---

## Code Standards

### Java Style

**Naming:**
- Classes: `PascalCase` (`PlayerMovement`, `IsometricRenderer`)
- Methods: `camelCase` (`updatePlayerPosition`, `calculateHit`)
- Constants: `SCREAMING_SNAKE_CASE` (`TICK_RATE_HZ`, `MAX_INVENTORY_SLOTS`)
- Private fields: `camelCase`, prefix with underscore if confusing (`_cachedTile`)

**Structure:**
```java
public class EntityManager {
    
    private static final int MAX_ENTITIES = 10_000;
    private static final Logger LOG = LoggerFactory.getLogger(EntityManager.class);
    
    private final Map<Integer, Entity> entities = new HashMap<>();
    private int nextEntityId = 1;
    
    public void addEntity(Entity entity) {
        if (entities.size() >= MAX_ENTITIES) {
            throw new IllegalStateException("Entity limit exceeded");
        }
        entities.put(nextEntityId++, entity);
        LOG.info("Added entity: {}", entity.getName());
    }
    
    public Optional<Entity> getEntity(int id) {
        return Optional.ofNullable(entities.get(id));
    }
}
```

**Comments:**
- Comment *why*, not *what*
- Use block comments for complex sections
- Log important state transitions

```java
// WRONG:
int x = player.getX(); // Get player's X position
x += 1; // Add 1 to X

// RIGHT:
// Player moved east; update position
int x = player.getX() + 1;

// GOOD:
// Collision with walls requires checking all adjacent tiles,
// not just the target tile. This prevents clipping through
// corners where two walls meet at 45 degrees.
boolean canWalkTo(int targetX, int targetY) {
    // ...
}
```

**Logging:**
Use `slf4j` (already in pom.xml):
```java
private static final Logger LOG = LoggerFactory.getLogger(MyClass.class);

LOG.debug("Tick {}: processed {} input packets", tickNumber, count);
LOG.info("Player {} connected", playerId);
LOG.warn("Packet dropped due to invalid state: {}", packetType);
LOG.error("Failed to save player state", exception);
```

### Protocol Buffers (.proto files)

```protobuf
syntax = "proto3";
package com.osrs.protocol;

// PlayerMovement: Client → Server
message PlayerMovement {
    int32 x = 1;           // World tile X coordinate
    int32 y = 2;           // World tile Y coordinate
    int32 facing = 3;      // Direction 0-7 (N, NE, E, SE, S, SW, W, NW)
    int64 sequence = 4;    // Packet sequence number (for reordering)
}
```

- Use meaningful field names
- Add comments explaining ranges (e.g., `// facing: 0-7`)
- Increment field numbers sequentially; never reuse old numbers
- Use `int32`, `int64`, `string`, `bool`, `bytes` only (no `float` for game data)

### No Magic Numbers

**BAD:**
```java
if (player.getX() == 50 && player.getY() == 50) {
    // Tutorial Island starting position
}
```

**GOOD:**
```java
private static final int TUTORIAL_START_X = 50;
private static final int TUTORIAL_START_Y = 50;

if (player.getX() == TUTORIAL_START_X && player.getY() == TUTORIAL_START_Y) {
    // Player at Tutorial Island starting position
}
```

### Testing

Write tests for:
- Combat calculations (hit/miss, XP)
- Path validation (can player walk here?)
- Inventory operations (add/remove items)
- Protocol serialization (packets round-trip correctly)

```java
class CombatEngineTest {
    
    @Test
    void testHitRollDeterministic() {
        // Same inputs + same tick seed = same result
        CombatEngine engine = new CombatEngine();
        boolean hit1 = engine.rollHit(player, target, 100); // tick 100
        boolean hit2 = engine.rollHit(player, target, 100); // same inputs
        
        assertEquals(hit1, hit2, "Hit roll must be deterministic");
    }
}
```

---

## Async Collaboration Protocol

**Goal:** Minimize back-and-forth. Maximum information density.

### Before Starting Work
1. Create a task in GitHub Projects (if not already there)
2. Move task to "In Progress"
3. Assign yourself
4. Create feature branch from task name

### During Work
1. **Do NOT chat about the work.** Just do it.
2. Commit frequently (5-10 commits per task is normal)
3. Update PROGRESS.md as you go (mark tests passing, note blockers)
4. If you hit a blocker, **document it in the PR description**, don't DM

### When Work is Done
1. Update PROGRESS.md: mark task as "✅ Complete"
2. Commit: `Update PROGRESS.md`
3. Push feature branch
4. Open PR with description (see PR Format above)
5. Request review

### Code Review
- Reviewer reads PR description first (understand the context)
- Reviewer checks code + tests
- If changes needed, reviewer adds comments inline
- If approved, reviewer merges (squash + merge)

**Timeline:** 24-48 hours for review (async, no chat needed)

---

## Workspace Updates

When you **pull changes** from main:

```bash
git checkout main
git pull origin main

# If you have local changes:
git checkout your-branch
git rebase main       # or git merge main

# Rebuild
mvn clean install
```

**After merging a PR:**
```bash
git checkout main
git pull origin main
mvn clean install
```

---

## Assets (Art & Data)

### Sprite Contributions (Game Artist)

When adding new sprites:

1. **Coordinate format:** Isometric tiles, 32×32 pixels per tile (width), 16×16 pixels per half-height
2. **Save as:** PNG, indexed color (256 colors max)
3. **Organize in:** `assets/sprites/<category>/<name>.png`
4. **Document in:** `assets/data/sprites.yaml`

Example `sprites.yaml`:
```yaml
sprites:
  - id: 1000
    name: PlayerHead
    file: "sprites/player/head_male.png"
    frames: 4              # Animation frames
    width: 32
    height: 32
  - id: 1001
    name: NPCGuide
    file: "sprites/npc/tutorial_guide.png"
    frames: 2
    width: 32
    height: 32
```

### Data Files (Dev)

When updating YAML configs:

1. **Format:** YAML (not JSON, not XML)
2. **Validate:** Ensure syntax is valid (test load in Java)
3. **Document:** Comments in YAML for any complex sections
4. **Push:** Commit to feature branch, include in PR

---

## Documentation Updates

If you:
- Add a new major system → update ARCHITECTURE.md
- Add a new config file → document in ARCHITECTURE.md (data section)
- Complete a sprint → update PROGRESS.md

**Always include documentation in your PR.**

---

## GitHub Projects Setup

Columns (automated with labels):
- **Backlog** — Not started
- **In Progress** — Claimed by someone
- **In Review** — PR opened
- **Done** — Merged to main

Use issue labels:
- `s1`, `s2`, `s3`, ... (sprint)
- `server`, `client`, `shared`, `docs` (component)
- `blocker` (critical path)
- `quick-win` (small, high-value)

---

## Final Checklist Before Pushing

- [ ] Branch name follows format: `<type>/<sprint>-<id>-<description>`
- [ ] Commits are atomic + have clear messages (no "WIP" or "test")
- [ ] `mvn clean test` passes locally
- [ ] Code follows style guide (naming, no magic numbers)
- [ ] PROGRESS.md updated with task status
- [ ] PR description is detailed (what, why, testing)
- [ ] No debug logging left in code (`System.out.println`, `e.printStackTrace()`)
- [ ] No commented-out code (if not needed, delete it)

---

## Questions?

If something is unclear, **add a comment in the PR or update this document.** Async collaboration gets better with every iteration.
