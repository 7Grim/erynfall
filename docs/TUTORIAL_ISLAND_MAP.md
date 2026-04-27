# Tutorial Island — Map Reference for Developers & AI Agents

> **Read this first** when adding a new skill, placing new NPCs, or modifying
> the tutorial island. It describes the tile layout, zone purpose, NPC
> positions, and the exact steps to extend the map for a new skill.

---

## Files

| File | Purpose |
|------|---------|
| `client/src/main/resources/map.yaml` | Tile grid used by the client renderer |
| `assets/data/map.yaml` | Identical copy used by the server's walkability checks |
| `server/src/main/resources/world.yml` | All NPC/object spawn positions + loot tables |

Both `map.yaml` files must always be identical. When you change one, copy it to
the other. The server validates walkability against `assets/data/map.yaml`; the
client renders from `client/src/main/resources/map.yaml`.

---

## Coordinate System

```
(0,0) ─────────────────────────► x (east)
  │
  │  x=0-7   : water border (not walkable)
  │  x=8-11  : west side path (walkable)
  │  x=12-91 : interior (grass or path)
  │  x=92-95 : east side path (walkable)
  │  x=96-103: water border (not walkable)
  │
  ▼ y (south / downward)
```

- **y=0** is the **top** row of the map
- **y increases downward**
- Player **spawn point**: **(50, 50)** — do not change without also updating `world.yml`

---

## Tile Types

| ID | Name | Walkable | Used in map? |
|----|------|----------|--------------|
| 0 | Grass | ✅ | Yes — all zone interiors |
| 1 | Water | ❌ | Yes — border rows |
| 2 | Path | ✅ | Yes — roads and zone separators |
| 3 | Rock Wall | ❌ | Defined for compatibility; **not placed** |
| 4 | Tree Marker | ❌ | Defined for compatibility; **not placed** |

> **Rule:** Never place tile 3 or 4 in the interior. All obstacles are NPC objects
> (trees, rocks, fishing spots). This guarantees the interior is always fully walkable.

---

## Zone Layout  (104 × 104 grid)

Each zone is a flat grass area separated by full-width path rows. Resource
objects within each zone are placed as NPC entries in `world.yml` — no tile
changes are needed to add new objects.

```
 y  0─  7  │ Water border
 y  8─ 11  │ North road  ────────────────────── full-width path
            │
 y 12─ 14  │ FUTURE ZONE C   (Agility / Thieving / Hunter)
 y 15       │ ── path ──
 y 16─ 18  │ FUTURE ZONE B   (Herblore / Crafting / Runecrafting)
 y 19       │ ── path ──
 y 20─ 22  │ FUTURE ZONE A   (Smithing / Fletching / Construction)
 y 23       │ ── path ──
 y 24─ 26  │ COOKING + SERVICES   fires at y=25 x=14,18
 y 27       │ ── path ──
 y 28─ 30  │ FISHING   spots at y=29  x=14,18,22,26
 y 31       │ ── path ──
 y 32─ 34  │ MINING   rocks at y=33  x=14,18,22,26,30,34,38,42,46
 y 35       │ ── path ──
 y 36─ 38  │ WOODCUTTING   trees at y=37  x=14,18,22,26,30,34,38,42
 y 39       │ ── path ──
 y 40─ 59  │ OPEN ZONE  (spawn, town, tutorial NPCs)
            │   Banker NPC:            x=50  y=44
            │   Tutorial Guide NPC:    x=50  y=48
            │   Combat Instructor NPC: x=55  y=48
            │   Player spawn:          x=50  y=50  ← fixed
 y 60       │ ── path ──
 y 61─ 65  │ COMBAT LOW   Rats y=63 x=45,48,51 │ Chickens y=63 x=30,33,36
 y 66       │ ── path ──
 y 67─ 71  │ COMBAT MID   Giant Rats y=69 x=44,48 │ Goblin y=69 x=58
 y 72       │ ── path ──
 y 73─ 77  │ COMBAT HIGH  Cows y=75 x=44,48
 y 78       │ ── path ──
 y 79─ 83  │ FUTURE COMBAT A   (stronger monsters — dragons, demons, etc.)
 y 84       │ ── path ──
 y 85─ 89  │ FUTURE COMBAT B   (end-game monsters)
 y 90─ 95  │ South road  ────────────────────── full-width path
 y 96─103  │ Water border
```

---

## Resource Node Placement Convention

Within any resource zone, objects are placed **west → east in ascending level
order**, spaced **4 tiles apart starting at x=14**:

| Slot | x  | Notes |
|------|----|-------|
| 1    | 14 | Level 1 (always start here) |
| 2    | 18 | |
| 3    | 22 | |
| 4    | 26 | |
| 5    | 30 | |
| 6    | 34 | |
| 7    | 38 | |
| 8    | 42 | |
| 9    | 46 | |
| 10+  | 50, 54, … | Still within walkable interior (x ≤ 91) |

The y-value for objects should be the **middle row** of the zone (e.g., y=33
for the 3-row zone at y=32–34).

---

## Current NPC Inventory

### Resource nodes

| ID | Type | Definition ID | x | y | Level req |
|----|------|--------------|---|---|-----------|
| 10 | Tree | 100 | 14 | 37 | 1 |
| 11 | Oak Tree | 101 | 18 | 37 | 15 |
| 12 | Willow Tree | 102 | 22 | 37 | 30 |
| 13 | Teak Tree | 106 | 26 | 37 | 35 |
| 14 | Maple Tree | 103 | 30 | 37 | 45 |
| 15 | Mahogany Tree | 107 | 34 | 37 | 50 |
| 16 | Yew Tree | 104 | 38 | 37 | 60 |
| 17 | Magic Tree | 105 | 42 | 37 | 75 |
| 20 | Copper Rock | 300 | 14 | 33 | 1 |
| 21 | Tin Rock | 301 | 18 | 33 | 1 |
| 22 | Iron Rock | 302 | 22 | 33 | 15 |
| 23 | Silver Rock | 303 | 26 | 33 | 20 |
| 24 | Coal Rock | 304 | 30 | 33 | 30 |
| 25 | Gold Rock | 305 | 34 | 33 | 40 |
| 26 | Mithril Rock | 306 | 38 | 33 | 55 |
| 27 | Adamantite Rock | 307 | 42 | 33 | 70 |
| 28 | Runite Rock | 308 | 46 | 33 | 85 |
| 30 | Fishing Spot (net) | 200 | 14 | 29 | 1 |
| 31 | Fishing Spot (net/bait) | 201 | 18 | 29 | 1 |
| 32 | Fishing Spot (lure/bait) | 202 | 20 | 29 | 20 |
| 33 | Fishing Spot (cage/harpoon) | 203 | 26 | 29 | 40 |
| 35 | Cooking Fire | — | 14 | 25 | 1 |
| 36 | Cooking Fire | — | 18 | 25 | 1 |

### Town / services

| ID | Name | x | y |
|----|------|---|---|
| 1  | Tutorial Guide | 50 | 48 |
| 2  | Combat Instructor | 55 | 48 |
| 40 | Banker | 50 | 44 |

### Combat NPCs

| ID | Name | Combat Lv | x | y | Zone |
|----|------|-----------|---|---|------|
| 50 | Rat | 1 | 45 | 63 | Low |
| 51 | Rat | 1 | 48 | 63 | Low |
| 52 | Rat | 1 | 51 | 63 | Low |
| 53 | Chicken | 1 | 30 | 63 | Low |
| 54 | Chicken | 1 | 33 | 63 | Low |
| 55 | Chicken | 1 | 36 | 63 | Low |
| 60 | Giant Rat | 3 | 44 | 69 | Mid |
| 61 | Giant Rat | 3 | 48 | 69 | Mid |
| 62 | Goblin | 2 | 58 | 69 | Mid |
| 70 | Cow | 2 | 44 | 75 | High |
| 71 | Cow | 2 | 48 | 75 | High |

---

## How to Add a New Skill

### Step 1 — Pick the right zone

| Skill type | Use zone |
|------------|----------|
| Resource gathering (chopping, mining, etc.) | Assign one of the Future Zones A/B/C |
| Processing (smithing, crafting, etc.) | Assign one of the Future Zones A/B/C |
| Combat-adjacent (slayer, ranged targets) | Future Combat Zone A or B (y=79–89) |

Recommended assignment order (first-in, inner zone):
- **Future Zone A** (y=20–22): Smithing, Fletching, Construction
- **Future Zone B** (y=16–18): Herblore, Crafting, Runecrafting
- **Future Zone C** (y=12–14): Agility, Thieving, Hunter, Farming

### Step 2 — Place NPC objects in world.yml

```yaml
# Example: Smithing furnace + anvil in Future Zone A (y=20-22, objects at y=21)
- id: 80
  name: "Furnace"
  definition_id: 400        # register SmithingRegistry definition 400
  combat_level: 0
  max_hp: 1
  max_hit: 0
  respawn_delay_ticks: 0
  location: "tutorial_island"
  x: 14
  y: 21
  examine: "A furnace for smelting ores."
  wander_radius: 0
  is_aggressive: false

- id: 81
  name: "Anvil"
  definition_id: 401
  ...
  x: 18
  y: 21
```

Pick the next available NPC `id` value. IDs 1–79 are taken (see table above).
Use 80+ for new objects to avoid collisions.

### Step 3 — Register the definition

Create or extend the appropriate shared registry (e.g.,
`shared/src/main/java/com/osrs/shared/SmithingRegistry.java`) following the
pattern of `WoodcuttingRegistry`, `MiningRegistry`, `FishingRegistry`.

### Step 4 — Wire up the skill guide

In `client/src/main/java/com/osrs/client/ui/SkillGuideRegistry.java`:
1. Add a `SKILL_<NAME> = <index>` constant
2. Implement a `<Name>GuideProvider` inner class (Introduction + resource tab + optional scaling tab)
3. Register it in the `static {}` block

In `client/src/main/java/com/osrs/client/ui/LevelUnlockRegistry.java`:
- Add a `<name>Unlock(int level)` method with milestone messages at every 10 levels

### Step 5 — No tile changes needed

Unless the skill requires special terrain (e.g., water for fishing already
exists conceptually, farming patches need soil tiles), the map tiles do not
need to change. All resource objects render as NPC entities, not tiles.

If special terrain IS needed, only modify the **3 grass rows of that specific
zone** in both `map.yaml` files. Never change path separator rows.

---

## Skill Guide Pattern (quick reference)

All skills follow this code pattern:

```
SkillGuideRegistry
  └─ <Name>GuideProvider
       ├─ getSections()      → ["Introduction", "<Tab1>", "<Tab2>"]
       ├─ renderSectionContent(sr, batch, font, proj, skillIdx, level, totalXp,
       │                       sectionIdx, x, y, w, h, scrollOffset)
       └─ getSectionContentHeight(skillIdx, level, sectionIdx, contentW)

LevelUnlockRegistry.get(skillIdx, level)
  └─ returns a String milestone message, or null if no unlock at this level

ItemIconRenderer.drawItemIcon(sr, x, y, itemId)
  └─ renders a geometric icon for any item ID used in skill guides
```

The Introduction section always has **3 info blocks** (82px tall each), each
with an icon on the left (x+20) and wrapped text on the right (x+62).

Resource list sections use scrollable rows (32px tall), rendered in two passes:
1. ShapeRenderer pass — background rect + item icon
2. SpriteBatch pass — level label, name, stat label, sub-label

Row colours: `ROW_UNLOCKED` (tan) / `ROW_NEXT` (gold) / `ROW_LOCKED` (grey).

---

## Skills Implementation Status (as of 2026-04-04)

| # | Skill | Guide | Unlock msgs | Notes |
|---|-------|-------|-------------|-------|
| 0 | Attack | ✅ | ✅ | AttackGuideProvider, WeaponRegistry |
| 1 | Strength | ✅ | ✅ | StrengthGuideProvider, max-hit table |
| 2 | Defence | ✅ | ✅ | DefenceGuideProvider, ArmourTier |
| 3 | Hitpoints | ✅ | ✅ | HitpointsGuideProvider, food table, HP bar number |
| 4 | Ranged | ❌ | partial | No guide yet |
| 5 | Prayer | ❌ | partial | No guide, no point pool |
| 6 | Magic | ❌ | partial | No guide |
| 7 | Woodcutting | ✅ | ✅ | WoodcuttingRegistry, 8 tree tiers |
| 8 | Fishing | ✅ | ✅ | FishingRegistry, 4 spot types |
| 9 | Cooking | partial | partial | XP awards, no guide |
| 10 | Mining | ✅ | ✅ | MiningRegistry, 9 rock tiers |
| 11 | Smithing | ❌ | ❌ | Future Zone A |
| 12 | Firemaking | partial | partial | XP only, no client animation |
| 13 | Crafting | ❌ | ❌ | Future Zone B |
| 14 | Fletching | ❌ | ❌ | Future Zone A |
| 15 | Agility | ❌ | ❌ | Future Zone C |
| 16 | Herblore | ❌ | ❌ | Future Zone B |
| 17 | Thieving | ❌ | ❌ | Future Zone C |
| 18 | Slayer | ❌ | ❌ | Future Combat zones |
| 19 | Farming | ❌ | ❌ | Future Zone C |
| 20 | Runecrafting | ❌ | ❌ | Future Zone B |
| 21 | Hunter | ❌ | ❌ | Future Zone C |
| 22 | Construction | ❌ | ❌ | Future Zone A |
