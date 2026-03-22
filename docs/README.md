# OSRS-MMORP Documentation

## 📋 Overview

This `docs/` directory contains the **canonical source of truth** for all OSRS-MMORP design, architecture, and implementation details. All files are considered **authoritative references** for development decisions.

**Alongside these docs, also consult:**
- **Old School RuneScape Wiki**: https://oldschool.runescape.wiki/ (gameplay mechanics, skill definitions, economy)
- **GitHub Issues & Project Board**: https://github.com/EarthDeparture/osrs-mmorp (sprint tracking, bug reports)

---

## 📁 Directory Structure

### **Phase 0: Vision & Discovery** (COMPLETE)
Planning documents that establish project identity, scope, and constraints.

- **[VISION.md](VISION.md)** — Project purpose, target audience, and long-term vision
- **[SCOPE.md](SCOPE.md)** — MVP definition, feature scope, concurrent player capacity, timeline estimate
- **[QUESTIONS.md](QUESTIONS.md)** — Full discovery questionnaire (35 questions answered)

### **Phase 1: Gameplay Design** (COMPLETE)
Canonical gameplay mechanics and system design aligned with OSRS.

- **[OSRS_REFERENCE.md](OSRS_REFERENCE.md)** — OSRS core mechanics (skills, combat, economy, progression)
- **[EXACT_OSRS_PROGRESSION.md](EXACT_OSRS_PROGRESSION.md)** — **CANONICAL** All 8 MVP skills with exact XP tables, level unlocks, and progression curves
- **[COMBAT_FEEDBACK.md](COMBAT_FEEDBACK.md)** — **CANONICAL** Hitsplats, combat log, animations, death sequences, sounds (exact OSRS)
- **[GAMEPLAY.md](GAMEPLAY.md)** — Core loop, progression paths, player fantasy, victory conditions
- **[WORLD.md](WORLD.md)** — World scope, map layout, NPC placement, POI locations
- **[PROGRESSION_PACING.md](PROGRESSION_PACING.md)** — Time-to-level milestones, enemy progression gates, gear progression
- **[PLAYER_PSYCHOLOGY.md](PLAYER_PSYCHOLOGY.md)** — **CRITICAL** Achievements, seasonal events, hiscores, retention mechanics, motivation loops

### **Phase 2: Systems Design** (COMPLETE)
Detailed specification for each game system.

- **[INVENTORY_SYSTEM.md](INVENTORY_SYSTEM.md)** — **CRITICAL** 28-slot hard limit, looting bag, weight mechanics, banking, exact OSRS
- **[LOOT_SYSTEM.md](LOOT_SYSTEM.md)** — **CRITICAL** Drop tables, raw meat requirement, economy interconnection, exact OSRS
- **[QUEST_SYSTEM.md](QUEST_SYSTEM.md)** — **CRITICAL** Quest structure, Dragon Slayer as MVP model, objectives, rewards, journal
- **[SOCIAL_SYSTEMS.md](SOCIAL_SYSTEMS.md)** — **CRITICAL** Chat (public/private), Grand Exchange (auto-matching, 2% fee), P2P trading (double-accept), exact OSRS

### **Phase 3: Technical Architecture** (COMPLETE)
Infrastructure, networking, and database design.

- **[ARCHITECTURE.md](ARCHITECTURE.md)** — Server-first MMO architecture, 256 Hz tick loop, Netty, authority validation, deterministic RNG
- **[TECH.md](TECH.md)** — Technology stack, constraints, build system, deployment strategy
- **[DATABASE_SCHEMA_FINAL.md](DATABASE_SCHEMA_FINAL.md)** — 14-table SQL Server schema with 2 views, 4 stored procedures, verification status
- **[DATABASE_IMPLEMENTATION_LOG.md](DATABASE_IMPLEMENTATION_LOG.md)** — Complete database implementation timeline, cascade delete strategy, initialization data

### **Phase 4: Implementation Roadmap** (COMPLETE)
Sprint-by-sprint development plan with tasks and acceptance criteria.

- **[IMPLEMENTATION_ROADMAP.md](IMPLEMENTATION_ROADMAP.md)** — 8 sprints (S0–S8) with task breakdown
- **[EXHAUSTIVE_DEVELOPMENT_ROADMAP.md](EXHAUSTIVE_DEVELOPMENT_ROADMAP.md)** — **MASTER GUIDE (20,000+ lines)** Complete implementation blueprint with:
  - Code examples for every system
  - Database schema and migrations
  - Unit test cases
  - Edge case handling
  - Acceptance criteria
  - **START HERE WHEN CODING**

### **Phase 5: Launch & Operations** (COMPLETE)
Post-launch infrastructure and monetization.

- **[POSTLAUNCH.md](POSTLAUNCH.md)** — Monetization model, infrastructure scaling, operational procedures

### **Phase 6: Reference & Quick Lookup** (COMPLETE)
Quick-access guides for common tasks.

- **[QUICK_REFERENCE.md](QUICK_REFERENCE.md)** — Database connection, common queries, Java integration code, verification checklists
- **[PROJECT_SUMMARY.md](PROJECT_SUMMARY.md)** — High-level project manifest and completion checklist

### **Phase 7: Progress Tracking**
Real-time implementation status.

- **[S0_IMPLEMENTATION_SUMMARY.md](S0_IMPLEMENTATION_SUMMARY.md)** — S0 (Database Schema) completion status and verification
- **[S1.5_DATABASE_INTEGRATION.md](S1.5_DATABASE_INTEGRATION.md)** — S1.5 milestone planning (HikariCP, JDBC integration)
- **[PROGRESS.md](PROGRESS.md)** — Current development status, blockers, next steps
- **[CONTRIBUTING.md](CONTRIBUTING.md)** — Development guidelines, code style, commit conventions

### **Session Logs**
Dated session notes for debugging and context recovery.

- **[sessions/](sessions/)** — Session logs organized by date

---

## 🔑 Key Documents for Development

### **Start Here (First-Time Developers)**
1. **[VISION.md](VISION.md)** — Understand the project
2. **[SCOPE.md](SCOPE.md)** — Know what's in/out for MVP
3. **[ARCHITECTURE.md](ARCHITECTURE.md)** — Understand the technical approach
4. **[QUICK_REFERENCE.md](QUICK_REFERENCE.md)** — Get connection strings and setup

### **When Implementing a Feature**
1. **[EXHAUSTIVE_DEVELOPMENT_ROADMAP.md](EXHAUSTIVE_DEVELOPMENT_ROADMAP.md)** — Find the task with code examples
2. Consult relevant system doc (e.g., [INVENTORY_SYSTEM.md](INVENTORY_SYSTEM.md) for inventory)
3. Check [OSRS_REFERENCE.md](OSRS_REFERENCE.md) for exact OSRS behavior
4. Write tests per [EXHAUSTIVE_DEVELOPMENT_ROADMAP.md](EXHAUSTIVE_DEVELOPMENT_ROADMAP.md) spec

### **When Debugging Combat or Skills**
1. **[EXACT_OSRS_PROGRESSION.md](EXACT_OSRS_PROGRESSION.md)** — Verify XP tables and level calculations
2. **[COMBAT_FEEDBACK.md](COMBAT_FEEDBACK.md)** — Check combat mechanics, damage formulas, hitsplat timing
3. **[PROGRESSION_PACING.md](PROGRESSION_PACING.md)** — Verify enemy difficulty gates

### **When Building Quests or Dialogue**
1. **[QUEST_SYSTEM.md](QUEST_SYSTEM.md)** — Follow Dragon Slayer template
2. **[OSRS_REFERENCE.md](OSRS_REFERENCE.md)** — Match exact quest design from OSRS

### **When Integrating with Database**
1. **[DATABASE_SCHEMA_FINAL.md](DATABASE_SCHEMA_FINAL.md)** — Schema reference
2. **[DATABASE_IMPLEMENTATION_LOG.md](DATABASE_IMPLEMENTATION_LOG.md)** — Setup instructions and cascade strategy
3. **[QUICK_REFERENCE.md](QUICK_REFERENCE.md)** — Connection strings and test queries

---

## 🎯 Critical Design Constraints

### **Non-Negotiable Gameplay**
- ✅ **Right-click tile movement ONLY** — No keyboard movement, ever
- ✅ **256 Hz tick rate (3.9ms/tick)** — Deterministic, matches OSRS
- ✅ **Authority-server architecture** — Client predicts, server validates; client cannot cheat
- ✅ **Raw meat drops only** — Forces Cooking dependency; prevents commodity trading
- ✅ **28-slot inventory (hard constraint)** — Enforced at database level
- ✅ **Chat + Trading MVP** — Public chat, private messages, Grand Exchange, P2P trading

### **Technical Architecture**
- **Java 21 LTS** with Maven 3.8+
- **Netty 4.1** for networking (port 43594)
- **LibGDX 1.14.0** for isometric client rendering
- **Protocol Buffers 3.25.1** for serialization
- **SQL Server 2025 Express** for persistence
- **Custom 256 Hz tick loop** (not request/response)

### **Data-Driven Design**
- All NPCs, items, quests, dialogue in YAML config files
- No hardcoded game content
- Configuration loading from `server/src/main/resources/`

---

## 📊 Project Status

| Phase | Milestone | Status | Date |
|-------|-----------|--------|------|
| **Planning** | Categories 1-7 (15 documents) | ✅ COMPLETE | 2026-03-21 |
| **S0** | Database Schema (14 tables, 2 views, 4 procedures) | ✅ COMPLETE & VERIFIED | 2026-03-22 |
| **S1** | Infrastructure (256 Hz tick, Netty, rendering, movement) | ✅ COMPLETE | 2026-03-22 |
| **S1.5** | Database Integration (HikariCP, JDBC wiring) | 🔄 DEFERRED TO S2 | — |
| **S2** | Core Systems (combat, skills, inventory, quests) | 🚀 READY TO BEGIN | 2026-03-22 |
| **S3–S8** | Advanced features, content, optimization | 📋 PLANNED | — |

---

## 🔗 External References

### **OSRS Game Design** (Canonical for Mechanics)
- https://oldschool.runescape.wiki/ — All gameplay mechanics
- https://oldschool.runescape.wiki/w/Skill — All 23 skills
- https://oldschool.runescape.wiki/w/Experience — XP tables and formulas
- https://oldschool.runescape.wiki/w/Combat — Combat mechanics
- https://oldschool.runescape.wiki/w/Grand_Exchange — Trading system

### **Project Repository**
- https://github.com/EarthDeparture/osrs-mmorp
- **Main branch**: All work; feature branches for major features
- **Issues**: Bug reports, task tracking
- **Project Board**: Sprint planning

### **Technology References**
- **Java 21**: https://docs.oracle.com/en/java/javase/21/
- **Netty 4.1**: https://netty.io/wiki/
- **LibGDX**: https://libgdx.com/wiki/
- **Protocol Buffers**: https://developers.google.com/protocol-buffers
- **SQL Server 2025**: https://docs.microsoft.com/en-us/sql/

---

## 📝 Document Conventions

### **Canonical Documents** (Source of Truth)
These are marked with **CANONICAL** in their headers and should not be contradicted by code. If code differs, update the code.

- `EXACT_OSRS_PROGRESSION.md` — Authoritative XP tables and level definitions
- `COMBAT_FEEDBACK.md` — Authoritative combat mechanics and timings
- `INVENTORY_SYSTEM.md` — Authoritative inventory constraints
- `LOOT_SYSTEM.md` — Authoritative drop tables and economy
- `QUEST_SYSTEM.md` — Authoritative quest structure
- `SOCIAL_SYSTEMS.md` — Authoritative chat and trading mechanics
- `PLAYER_PSYCHOLOGY.md` — Authoritative retention and engagement mechanics

### **Critical Documents** (Read Before Implementing)
Marked with **CRITICAL** — these impact multiple systems; changes require careful consideration.

- `DATABASE_SCHEMA_FINAL.md`
- `EXHAUSTIVE_DEVELOPMENT_ROADMAP.md`
- All **CANONICAL** documents above

### **Reference Documents** (Look Up As Needed)
Marked with **REFERENCE** — these are lookup guides, not design spec.

- `OSRS_REFERENCE.md`
- `QUICK_REFERENCE.md`

---

## 🚀 Next Steps

### **Immediate (S2 – Start Now)**
1. **DatabaseManager Integration** (HikariCP + JDBC)
   - See: [EXHAUSTIVE_DEVELOPMENT_ROADMAP.md](EXHAUSTIVE_DEVELOPMENT_ROADMAP.md#s15-database-integration)
2. **Combat Engine Integration** (wire into game loop)
   - See: [COMBAT_FEEDBACK.md](COMBAT_FEEDBACK.md)
3. **Inventory System** (28-slot validation)
   - See: [INVENTORY_SYSTEM.md](INVENTORY_SYSTEM.md)
4. **Skill Progression** (XP tracking)
   - See: [EXACT_OSRS_PROGRESSION.md](EXACT_OSRS_PROGRESSION.md)

### **Quality Gates Before Each Sprint**
- All docs updated
- Code follows architecture
- Mechanics match OSRS exactly
- Tests pass (per EXHAUSTIVE_DEVELOPMENT_ROADMAP.md)

---

## 📞 Questions or Clarifications?

If any doc contradicts another, or you find an error:
1. **Check OSRS wiki first** — primary source
2. **Check CANONICAL docs** — secondary source
3. **Consult [QUESTIONS.md](QUESTIONS.md)** — may have been addressed
4. **Raise an issue** — document the contradiction

---

**Last Updated:** 2026-03-22
**Maintained By:** Chip (AI Assistant)
**Source of Truth Status:** ✅ CANONICAL
