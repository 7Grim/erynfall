# OSRS-MMORP Documentation Index

Complete index of all design, architecture, and implementation documents.

---

## Quick Navigation

### By Phase

| Phase | Documents | Purpose |
|-------|-----------|---------|
| **Vision & Discovery** | VISION.md, SCOPE.md, QUESTIONS.md | Define project identity and scope |
| **Game Design** | OSRS_REFERENCE.md, EXACT_OSRS_PROGRESSION.md, COMBAT_FEEDBACK.md, GAMEPLAY.md, WORLD.md | Specify OSRS-aligned gameplay mechanics |
| **Systems Design** | INVENTORY_SYSTEM.md, LOOT_SYSTEM.md, QUEST_SYSTEM.md, SOCIAL_SYSTEMS.md | Detail each game system |
| **Technical** | ARCHITECTURE.md, TECH.md, DATABASE_SCHEMA_FINAL.md, DATABASE_IMPLEMENTATION_LOG.md | Infrastructure and database design |
| **Implementation** | IMPLEMENTATION_ROADMAP.md, EXHAUSTIVE_DEVELOPMENT_ROADMAP.md, PROGRESSION_PACING.md, SKILL_PROGRESSION.md | Step-by-step coding guides |
| **Reference** | OSRS_REFERENCE.md, QUICK_REFERENCE.md, PROJECT_SUMMARY.md | Lookup and quick access |
| **Tracking** | PROGRESS.md, S0_IMPLEMENTATION_SUMMARY.md, S1.5_DATABASE_INTEGRATION.md, CONTRIBUTING.md | Status and guidelines |

---

## By Audience

### **First-Time Developers**
1. [VISION.md](VISION.md) — What is this project?
2. [SCOPE.md](SCOPE.md) — What features are in MVP?
3. [ARCHITECTURE.md](ARCHITECTURE.md) — How does it work technically?
4. [QUICK_REFERENCE.md](QUICK_REFERENCE.md) — How do I get started?

### **Frontend/Client Developers**
- [ARCHITECTURE.md](ARCHITECTURE.md) — Client-side networking and authority validation
- [WORLD.md](WORLD.md) — Map layout and world structure
- [EXACT_OSRS_PROGRESSION.md](EXACT_OSRS_PROGRESSION.md) — UI needs for skill tracking
- [COMBAT_FEEDBACK.md](COMBAT_FEEDBACK.md) — Visual and audio feedback specs
- [QUICK_REFERENCE.md](QUICK_REFERENCE.md) — Netty message types and packet structure

### **Server/Backend Developers**
- [ARCHITECTURE.md](ARCHITECTURE.md) — 256 Hz tick loop and entity systems
- [EXHAUSTIVE_DEVELOPMENT_ROADMAP.md](EXHAUSTIVE_DEVELOPMENT_ROADMAP.md) — Code examples and task breakdown
- [DATABASE_SCHEMA_FINAL.md](DATABASE_SCHEMA_FINAL.md) — Table schema and constraints
- [DATABASE_IMPLEMENTATION_LOG.md](DATABASE_IMPLEMENTATION_LOG.md) — Setup and integration
- [TECH.md](TECH.md) — Technology stack and build process

### **Game Designers / Content Builders**
- [SCOPE.md](SCOPE.md) — What's in MVP?
- [EXACT_OSRS_PROGRESSION.md](EXACT_OSRS_PROGRESSION.md) — Skills and progression
- [PROGRESSION_PACING.md](PROGRESSION_PACING.md) — Time-to-level milestones
- [QUEST_SYSTEM.md](QUEST_SYSTEM.md) — Quest design template
- [LOOT_SYSTEM.md](LOOT_SYSTEM.md) — Drop tables and economy
- [OSRS_REFERENCE.md](OSRS_REFERENCE.md) — OSRS mechanics reference

### **QA / Testers**
- [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md) — Acceptance criteria checklist
- [EXHAUSTIVE_DEVELOPMENT_ROADMAP.md](EXHAUSTIVE_DEVELOPMENT_ROADMAP.md) — Test cases per system
- [COMBAT_FEEDBACK.md](COMBAT_FEEDBACK.md) — Combat bug detection
- [QUICK_REFERENCE.md](QUICK_REFERENCE.md) — Common queries and verification

---

## By Document Type

### **Canonical (Source of Truth)**
**These documents define the authoritative behavior. Code must match.**

- [EXACT_OSRS_PROGRESSION.md](EXACT_OSRS_PROGRESSION.md) — All 8 MVP skills, XP tables, exact levels
- [COMBAT_FEEDBACK.md](COMBAT_FEEDBACK.md) — Combat mechanics, hitsplats, timings
- [INVENTORY_SYSTEM.md](INVENTORY_SYSTEM.md) — 28-slot constraint, looting bag, banking
- [LOOT_SYSTEM.md](LOOT_SYSTEM.md) — Drop tables, raw meat requirement
- [QUEST_SYSTEM.md](QUEST_SYSTEM.md) — Quest structure, Dragon Slayer template
- [SOCIAL_SYSTEMS.md](SOCIAL_SYSTEMS.md) — Chat, GE matching, P2P trading
- [PLAYER_PSYCHOLOGY.md](PLAYER_PSYCHOLOGY.md) — Achievements, retention loops, seasonal events

### **Critical (Impacts Multiple Systems)**
**Read before implementing anything that touches these areas.**

- [ARCHITECTURE.md](ARCHITECTURE.md) — 256 Hz tick loop, Netty, authority validation
- [DATABASE_SCHEMA_FINAL.md](DATABASE_SCHEMA_FINAL.md) — 14-table schema, 2 views, 4 procedures
- [DATABASE_IMPLEMENTATION_LOG.md](DATABASE_IMPLEMENTATION_LOG.md) — Setup, cascade strategy, Java integration
- [EXHAUSTIVE_DEVELOPMENT_ROADMAP.md](EXHAUSTIVE_DEVELOPMENT_ROADMAP.md) — Complete implementation blueprint with code examples

### **Reference (Lookup As Needed)**
**Consult when implementing specific features.**

- [OSRS_REFERENCE.md](OSRS_REFERENCE.md) — OSRS mechanics (skills, combat, economy)
- [QUICK_REFERENCE.md](QUICK_REFERENCE.md) — Connection strings, test queries, common code snippets
- [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md) — High-level project overview

### **Specification Documents**
**Detailed system design; less likely to change.**

- [VISION.md](VISION.md) — Project purpose and audience
- [SCOPE.md](SCOPE.md) — MVP definition and constraints
- [GAMEPLAY.md](GAMEPLAY.md) — Core loop and player fantasy
- [WORLD.md](WORLD.md) — World scope and map layout
- [TECH.md](TECH.md) — Technology stack and constraints

### **Implementation Guides**
**Step-by-step instructions for coding.**

- [IMPLEMENTATION_ROADMAP.md](IMPLEMENTATION_ROADMAP.md) — 8 sprints with task breakdown
- [EXHAUSTIVE_DEVELOPMENT_ROADMAP.md](EXHAUSTIVE_DEVELOPMENT_ROADMAP.md) — Complete guide with code examples, tests, edge cases
- [PROGRESSION_PACING.md](PROGRESSION_PACING.md) — Time-to-level milestones and enemy progression
- [SKILL_PROGRESSION.md](SKILL_PROGRESSION.md) — Skill leveling mechanics

### **Operational / Tracking**
**Current status and guidelines.**

- [PROGRESS.md](PROGRESS.md) — Current sprint status and blockers
- [S0_IMPLEMENTATION_SUMMARY.md](S0_IMPLEMENTATION_SUMMARY.md) — S0 (Database) completion status
- [S1.5_DATABASE_INTEGRATION.md](S1.5_DATABASE_INTEGRATION.md) — S1.5 milestone planning
- [CONTRIBUTING.md](CONTRIBUTING.md) — Development guidelines and commit conventions
- [QUESTIONS.md](QUESTIONS.md) — Discovery questionnaire (35 questions answered)

### **Post-Launch**
**Infrastructure and monetization (for reference; not MVP).**

- [POSTLAUNCH.md](POSTLAUNCH.md) — Monetization, scaling, operations

---

## Searching This Documentation

### **Find Info About...**

#### **Inventory System**
- Primary: [INVENTORY_SYSTEM.md](INVENTORY_SYSTEM.md) **CANONICAL**
- Reference: [QUICK_REFERENCE.md](QUICK_REFERENCE.md) (SQL queries)
- Implementation: [EXHAUSTIVE_DEVELOPMENT_ROADMAP.md](EXHAUSTIVE_DEVELOPMENT_ROADMAP.md) (code examples)

#### **Combat**
- Primary: [COMBAT_FEEDBACK.md](COMBAT_FEEDBACK.md) **CANONICAL**
- Reference: [OSRS_REFERENCE.md](OSRS_REFERENCE.md) (mechanics)
- Implementation: [EXHAUSTIVE_DEVELOPMENT_ROADMAP.md](EXHAUSTIVE_DEVELOPMENT_ROADMAP.md) (code examples)

#### **Skills & Progression**
- Primary: [EXACT_OSRS_PROGRESSION.md](EXACT_OSRS_PROGRESSION.md) **CANONICAL**
- Reference: [PROGRESSION_PACING.md](PROGRESSION_PACING.md) (milestones)
- Reference: [SKILL_PROGRESSION.md](SKILL_PROGRESSION.md) (leveling mechanics)
- Implementation: [EXHAUSTIVE_DEVELOPMENT_ROADMAP.md](EXHAUSTIVE_DEVELOPMENT_ROADMAP.md) (code examples)

#### **Economy & Loot**
- Primary: [LOOT_SYSTEM.md](LOOT_SYSTEM.md) **CANONICAL**
- Reference: [OSRS_REFERENCE.md](OSRS_REFERENCE.md) (items, prices)
- Implementation: [EXHAUSTIVE_DEVELOPMENT_ROADMAP.md](EXHAUSTIVE_DEVELOPMENT_ROADMAP.md) (code examples)

#### **Trading & Social**
- Primary: [SOCIAL_SYSTEMS.md](SOCIAL_SYSTEMS.md) **CANONICAL**
- Reference: [OSRS_REFERENCE.md](OSRS_REFERENCE.md) (Grand Exchange mechanics)
- Implementation: [EXHAUSTIVE_DEVELOPMENT_ROADMAP.md](EXHAUSTIVE_DEVELOPMENT_ROADMAP.md) (code examples)

#### **Quests**
- Primary: [QUEST_SYSTEM.md](QUEST_SYSTEM.md) **CANONICAL**
- Template: [OSRS_REFERENCE.md](OSRS_REFERENCE.md) (Dragon Slayer example)
- Implementation: [EXHAUSTIVE_DEVELOPMENT_ROADMAP.md](EXHAUSTIVE_DEVELOPMENT_ROADMAP.md) (code examples)

#### **Player Retention & Engagement**
- Primary: [PLAYER_PSYCHOLOGY.md](PLAYER_PSYCHOLOGY.md) **CANONICAL**
- Reference: [SCOPE.md](SCOPE.md) (success metrics)
- Implementation: [EXHAUSTIVE_DEVELOPMENT_ROADMAP.md](EXHAUSTIVE_DEVELOPMENT_ROADMAP.md) (code examples)

#### **Database**
- Schema: [DATABASE_SCHEMA_FINAL.md](DATABASE_SCHEMA_FINAL.md) **CRITICAL**
- Setup: [DATABASE_IMPLEMENTATION_LOG.md](DATABASE_IMPLEMENTATION_LOG.md) **CRITICAL**
- Queries: [QUICK_REFERENCE.md](QUICK_REFERENCE.md) (common SQL + Java code)

#### **Networking & Architecture**
- Primary: [ARCHITECTURE.md](ARCHITECTURE.md) **CRITICAL**
- Reference: [TECH.md](TECH.md) (technology stack)
- Implementation: [EXHAUSTIVE_DEVELOPMENT_ROADMAP.md](EXHAUSTIVE_DEVELOPMENT_ROADMAP.md) (code examples)

---

## Document Criticality Matrix

| Document | Criticality | Frequency Referenced | Last Updated |
|----------|------------|---------------------|----|
| ARCHITECTURE.md | CRITICAL | Daily | 2026-03-21 |
| EXACT_OSRS_PROGRESSION.md | CANONICAL | Daily | 2026-03-21 |
| COMBAT_FEEDBACK.md | CANONICAL | Daily | 2026-03-21 |
| INVENTORY_SYSTEM.md | CANONICAL | Daily | 2026-03-21 |
| LOOT_SYSTEM.md | CANONICAL | Daily | 2026-03-21 |
| EXHAUSTIVE_DEVELOPMENT_ROADMAP.md | CRITICAL | Continuous | 2026-03-21 |
| DATABASE_SCHEMA_FINAL.md | CRITICAL | Daily | 2026-03-22 |
| SOCIAL_SYSTEMS.md | CANONICAL | Weekly | 2026-03-21 |
| QUEST_SYSTEM.md | CANONICAL | Weekly | 2026-03-21 |
| PLAYER_PSYCHOLOGY.md | CANONICAL | Periodic | 2026-03-21 |
| PROGRESSION_PACING.md | Reference | Periodic | 2026-03-21 |
| QUICK_REFERENCE.md | Reference | Often | 2026-03-22 |
| OSRS_REFERENCE.md | Reference | Often | 2026-03-21 |

---

## Update Protocol

### **When to Update Documents**

- **Gameplay mechanics change** → Update CANONICAL doc (e.g., COMBAT_FEEDBACK.md)
- **System design change** → Update CRITICAL doc (e.g., ARCHITECTURE.md)
- **New discovery/clarification** → Update relevant docs + EXHAUSTIVE_DEVELOPMENT_ROADMAP.md
- **Code implementation differs from docs** → **Update docs, not code** (docs are source of truth)
- **New feature added to MVP** → Update SCOPE.md, IMPLEMENTATION_ROADMAP.md, EXHAUSTIVE_DEVELOPMENT_ROADMAP.md

### **Document Review Cycle**

- **Weekly**: Review PROGRESS.md for blockers; update if needed
- **Bi-weekly**: Review CANONICAL docs for consistency with code
- **Monthly**: Update EXHAUSTIVE_DEVELOPMENT_ROADMAP.md with new learnings

---

## Version Control

**All docs are version-controlled in Git.**

```bash
# View history of a document
git log -- docs/COMBAT_FEEDBACK.md

# See who changed what
git blame docs/COMBAT_FEEDBACK.md

# Revert a change
git checkout <commit> -- docs/COMBAT_FEEDBACK.md
```

---

**Last Updated:** 2026-03-22  
**Maintained By:** Chip (AI Assistant)  
**Status:** ✅ CANONICAL
