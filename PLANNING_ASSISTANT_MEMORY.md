# PLANNING_ASSISTANT_MEMORY.md

**Purpose:** Memory file documenting my role as a Planning Assistant for the Erynfall project.

## My Role

I am a **Planning Assistant** whose primary purpose is to:

1. **Research OSRS mechanics** in depth (specifically OSRS 2007, never RS3)
2. **Understand current implementation status** by reading PROGRESS.md and codebase
3. **Create perfect, detailed prompts** for a Codex LLM to implement coding tasks
4. **Maintain full context** of all planning documents and architecture decisions
5. **Identify gaps** between current state and target implementation
6. **Synthesize requirements** from EXHAUSTIVE_DEVELOPMENT_ROADMAP.md, EXACT_OSRS_PROGRESSION.md, OSRS_REFERENCE.md

## Critical Distinction: OSRS vs RS3

**NON-NEGOTIABLE:** This project models **OSRS (Old School RuneScape, 2007 era)**, NOT RS3 (modern RuneScape).

**Key Differences to Maintain:**
- Simpler combat mechanics (no Evolution of Combat)
- No squeal of fortune, microtransactions, MTX
- Lower poly count, simpler art style
- Original skill formulas and XP curves
- No EoC abilities, action bar system
- Click-based gameplay, not keyboard-driven
- Economy: raw material drops (not cooked), forcing skill interdependencies
- Prayer: overhead prayers provide immunity, not damage reduction
- Combat styles: Accurate/Aggressive/Defensive/Controlled (not legacy ability system)
- No "lifepoints" beyond standard HP
- No "divination," "invention," or RS3-only skills

## Project Snapshot

**Name:** Erynfall
**Reference:** Old School RuneScape (2007)
**Tech Stack:**
- Java 21
- Maven (multi-module)
- Netty (networking)
- Protocol Buffers (serialization)
- LibGDX (client rendering)
- HikariCP + SQL Server (persistence)

**Modules:**
- `shared` - Protocol Buffers schema, shared data models
- `server` - Authoritative game loop, Netty server (port 43594)
- `client` - LibGDX UI/rendering
- `auth` - Spring Boot auth service

**Architecture:**
- Server-authoritative (client sends intent, server validates and calculates)
- 256 Hz tick loop (deterministic game logic)
- Netty TCP networking

## Current Implementation Status

**Last Updated:** 2026-03-25

**Current Sprint:** S3 — Account auth + persistence shipped; dialogue/quest wiring remains

**Completed S3 Work:**
- ✅ LoginScreen: OSRS-style login UI
- ✅ PlayerRepository: DB auth, BCrypt login, auto-register, offline fallback
- ✅ Autosave: Every 60 seconds (player XP + position)
- ✅ Full skill sync on login (all 6 skills to client)
- ✅ Hitpoints XP fix (new players start at 1,154 XP, level 10)
- ✅ ItemDefinition: 14-field OSRS bonus model
- ✅ EquipmentBonusCalculator: Sums 11 equipment slots per tick
- ✅ CombatEngine: Exact OSRS hit-chance formula, max-hit formula (melee + ranged)
- ✅ SQL migration: hitpoints_xp + ranged_xp columns

**Completed S2 Work:**
- ✅ CombatEngine (hit/miss/damage, deterministic RNG)
- ✅ CombatUI (hitsplat circles, damage numbers, HP bars)
- ✅ Skill progression (exact OSRS XP table)
- ✅ Inventory system (20-slot grid, ground items, equip/unequip)
- ✅ Quest system (Quest class, QuestManager)
- ✅ Dialogue system (DialogueUI, DialogueEngine)
- ✅ XpDropOverlay (OSRS yellow drops, stacked)
- ✅ LevelUpOverlay (golden banner, OSRS wording)
- ✅ Skills tab (2-column grid, hover tooltips)
- ✅ Chat system (OSRS filters, overhead text)

**Pending:**
- Dialogue/quest wiring not complete
- Quest loading from YAML not wired
- NPC AI pathfinding (planned S3)
- Extended content (more quests, areas, skills)

## MVP Skills (8 Total, OSRS-Exact Progression)

1. **Attack** - Weapon accuracy, weapon access gates
2. **Strength** - Max damage, weapon gates
3. **Defence** - Damage reduction, armor gates
4. **Hitpoints** - Health pool
5. **Ranged** - Ranged combat (basic implementation)
6. **Magic** - Spell damage, accuracy
7. **Prayer** - Bury bones → XP, unlock buffs
8. **Woodcutting** - Harvest logs (feeds Fletching)
9. **Fishing** - Catch fish (feeds Cooking)
10. **Cooking** - Cook fish → healing food

**Progression Formula (All Skills):**
- Level 99 = 13,034,431 XP total
- Level 92 = 6,517,253 XP = 50% of total
- Exponential grind: levels 1-30 fast, 60-85 slow, 85-92 very grindy, 92-99 extreme

## Planning Documents (Read These for Context)

**Primary Sources:**
1. `docs/VISION.md` - Project identity, goals, success metrics
2. `docs/SCOPE.md` - MVP definition, skill tiers, economy flow
3. `docs/EXACT_OSRS_PROGRESSION.md` - Complete skill formulas, unlocks, XP rates
4. `docs/OSRS_REFERENCE.md` - Canonical OSRS design, mechanics, "feel"
5. `docs/EXHAUSTIVE_DEVELOPMENT_ROADMAP.md` - ULTRATHINK implementation blueprint
6. `docs/ARCHITECTURE.md` - Server-first design decisions
7. `docs/PROGRESS.md` - Sprint tracking, completed work
8. `docs/QUEST_SYSTEM.md` - Quest structure, objectives, rewards
9. `docs/LOOT_SYSTEM.md` - Drop tables, rarity, mechanics
10. `docs/COMBAT_FEEDBACK.md` - Hitsplat, HP bar, UI feedback

**For Research Tasks:**
- OSRS Wiki (oldschool.runescape.wiki) - Mechanics, formulas, exact values
- OSRS gameplay footage - Understand "feel" and pacing
- Item databases - OSRS item IDs, stats, requirements

## My Prompt Creation Workflow

When creating prompts for Codex LLM:

1. **Analyze the task** - Read relevant planning docs (EXACT_OSRS_PROGRESSION.md, OSRS_REFERENCE.md)
2. **Identify current state** - Check PROGRESS.md for what's already built
3. **Research OSRS mechanics** - Find exact formulas, values, behaviors from OSRS Wiki
4. **Review codebase** - Understand existing patterns (GameLoop, CombatEngine, packet handlers)
5. **Create comprehensive prompt** including:
   - Clear objective
   - Current implementation state
   - OSRS reference (exact mechanics to replicate)
   - Code location/context
   - Acceptance criteria
   - Test requirements
   - Dependencies on other systems

6. **Format for Codex:** Structured, context-rich, with clear deliverables

## Key OSRS Mechanics to Reference

**Combat:**
- Attack styles: Accurate (Attack XP), Aggressive (Strength XP), Defensive (Attack+Strength+Defence XP), Controlled (1.33 each)
- Hit chance formula: (attack_roll > defence_roll) = hit
- Max hit formula: floor((max_hit * strength_bonus) / 256) + damage_bonus
- Attack speed: weapons have attack speed in ticks (0.6s = 1 OSRS tick)
- Equipment bonuses: stab/slash/crush attack/defence, magic attack/defence, ranged attack/defence

**Skills:**
- Woodcutting: Trees have respawn timers, axes provide speed bonus
- Fishing: Fish caught based on level, success rate varies
- Cooking: Burn rate decreases with level, some food burns even at 99
- Prayer: Max PP = Prayer level, overhead prayers block damage type completely

**Economy:**
- NPCs drop raw materials (bones, logs, raw fish), not finished goods
- Forces interdependency: Woodcutting → Fletching, Fishing → Cooking, Combat → Prayer
- Grand Exchange for trading, Ironman mode blocks it

**UI/UX:**
- Right-click context menus for all actions
- Left-click performs default action
- No keyboard movement
- XP drops float up on right side
- Level-up banners above chat box
- HP bars above damaged entities

## Current Architecture Understanding

**Server Side:**
- `GameLoop.java` - 256 Hz tick processing, 6 stages (input → movement → combat → skills → loot → sync)
- `CombatEngine.java` - Hit/miss calculation, damage formulas, deterministic RNG
- `ServerPacketHandler.java` - Packet routing, validation, game state changes
- `World.java` - Entity management, tile map, collision
- `PlayerRepository.java` - Database persistence

**Client Side:**
- `GameScreen.java` - Main render loop, UI coordination
- `IsometricRenderer.java` - Tile + entity rendering
- `ClientPacketHandler.java` - Packet processing, entity state tracking
- UI components: CombatUI, DialogueUI, InventoryUI, XpDropOverlay, LevelUpOverlay

**Protocol:**
- `shared/src/main/proto/network.proto` - All packet definitions
- Protocol Buffers serialization
- Server authority (client sends intent, server sends results)

## Mode Recommendation

**Operating in PLAN MODE** is optimal for my role as a Planning Assistant because:

1. You want me to explore and research before creating outputs
2. I need to read multiple planning documents to synthesize context
3. Creating prompts is an analytical, not exploratory task
4. Build mode is for executing tool calls and creating files
5. Plan mode allows me to think through requirements before presenting solutions

---

**Last Updated:** 2026-03-30
**Next Action:** Wait for user to request research or prompt creation for specific feature
