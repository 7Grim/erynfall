# SCOPE.md - MVP & Project Scope

*This file will be filled as we answer Category 2 questions.*

## Minimum Viable Product (MVP) - Locked In

### Content
- **Main quest line** (story-driven progression)
- **3 side quests** (optional content, XP/reward)
- **Tutorial Island** (starting zone, teaches mechanics)
- **Main game island** (part of it - enough to play after tutorial)

### Bosses & Dungeons
- **Bosses:** One boss per quest (main + side quests)
- **Dungeons:** At least 2-3 with regular monsters for grinding XP and loot
- **Open world mobs:** Regular enemies on both islands

### Combat Skills (8 skills in MVP)
1. **Attack** — Weapon accuracy, gates weapon access
2. **Strength** — Damage output, gates weapon access
3. **Defence** — Damage reduction, gates armor access
4. **Magic** — Spell damage and accuracy
5. **Prayer** — Passive buffs and bone burial for XP
6. **Woodcutting** — Harvesting logs (feeds Fletching)
7. **Fishing** — Harvesting fish (feeds Cooking)
8. **Cooking** — Preparing food for healing

### Economy (Full)
- **Grand Exchange** (trading hub)
- **NPC trading** (buy/sell from NPCs)
- **Drop system:** Enemies drop loot (gold + items)
- **Resource gathering:** Logs, fish, bones → trade
- **Crafting economy:** Fletching uses logs → arrows/bows → sell or use

### Skill Tier System (Critical)
**Core principle:** Each skill has tiers that unlock with level milestones. Higher tier = higher XP + better output.

**Woodcutting Example:**
- Level 1: Regular Logs (low XP, basic material)
- Level 30: Oak Logs (mid XP, better material)
- Level 60: Yew Logs (high XP, premium material)
- Higher logs unlock higher Fletching xp

**Fishing Example:**
- Level 1: Shrimp (low XP, low healing food)
- Level 30: Trout (mid XP, mid healing)
- Level 60: Salmon (high XP, high healing)

**Prayer Example:**
- Bury Bones from monsters killed
- Monster difficulty determines bone grade
- Higher grade bones = higher Prayer XP

**This system interconnects everything:**
- Woodcutting → Fletching (logs → arrows)
- Fishing → Cooking (fish → food)
- Combat → Prayer (bones from kills → bury for XP)
- All outputs feed economy (supply/demand)

### Economy Feedback Loop
```
Kill Enemy
  ↓
Get Loot (gold) + Bones (Prayer XP) + Rare Drops
  ↓
Sell Loot → Gold
  ↓
Buy Better Gear → Kill Harder Enemies
  ↓
Better Enemies Drop Better Loot
  ↓
Economy Grows
```

Also:
```
Woodcutting → Logs
  ↓
Fletching → Bows/Arrows (high XP if high-tier logs)
  ↓
Sell Arrows → Economy
  ↓
Other Players Buy Arrows → Use in Combat
  ↓
Arrows Fuel Combat Training
```

### What's NOT in MVP (Post-Launch)
- Ranged & Melee weapon attack styles (only Magic + basic melee)
- Advanced skills (Slayer, Agility, Thieving, Construction, etc.)
- Special attacks
- PvP zones (multiplayer required first)
- Multiple biomes/areas (start with 2 islands)
- Raids or advanced dungeons
- Questing system is basic (NPC dialogue + objectives, no complex branching)

## Concurrent Players & Multiplayer Timeline

### MVP Phase (Solo Development)
- **Players:** 1 (you, solo testing)
- **Architecture:** Local server (localhost:43594) + client connection
- **Duration:** 6+ months (until 99% game is complete)
- **Server location:** Your machine (running Server.java + Client.java together)

### Post-MVP Phase 1 (Multiplayer Ready)
- **Players:** 2-10 (friends testing)
- **Architecture:** Same server code, deploy to LAN or cloud
- **No refactoring required** (server-first design enables this)

### Post-MVP Phase 2 (Official Release)
- **Players per server:** 100-500 concurrent
- **Multiple servers:** Yes (if demand exists)
- **Total players:** Thousands across all servers (like official OSRS)

### Critical Architectural Decision (LOCKED IN)
**Option B: Server-First from Day One**
- Server runs immediately (Netty on port 43594)
- Client connects to localhost (even though solo)
- All game logic on server, validated
- Authority-server prevents exploits
- Zero refactoring needed to add multiplayer
- Multiplayer transition: just deploy server to cloud + allow multiple clients

## Multiplayer Strategy (MVP vs Post-Launch)
(To be filled)

## Essential Systems for MVP
(To be filled)

## Timeline
(To be filled)

---

**Status:** Pending answers to Q2.1–Q2.5
