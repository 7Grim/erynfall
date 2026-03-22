# QUEST_SYSTEM.md - OSRS Quest Mechanics (Main Line Example: Dragon Slayer)

**Source:** Official OSRS Wiki - Dragon Slayer quest (iconic F2P main quest)  
**Status:** LOCKED IN - Quest structure, mechanics, objectives, rewards  
**Purpose:** Design MVP main quest line modeled after Dragon Slayer

---

## CRITICAL UNDERSTANDING: Dragon Slayer as MVP Model

**Dragon Slayer** is the iconic OSRS F2P main quest. It's the perfect model for your MVP main quest line because:

- **Iconic boss fight** (Elvarg, level 83 dragon)
- **Multiple objectives** (gather items, complete tasks, defeat enemies, defeat final boss)
- **Prerequisite requirements** (32 quest points, combat level recommendations)
- **Skill level gates** (Magic 33+, Prayer 37+, Combat 45+)
- **Equipment requirements** (specific items must be crafted/obtained)
- **NPC interactions** (Guildmaster, Oziach)
- **Rewards that unlock gear** (ability to equip Rune platebody, Green d'hide body)
- **Experience rewards** (18,650 Strength, 18,650 Defence XP)

---

## PART 1: DRAGON SLAYER QUEST STRUCTURE (MVP TEMPLATE)

### Quest Overview

**Name:** Dragon Slayer I  
**Difficulty:** Experienced (mid-level difficulty)  
**Series:** Dragonkin (first quest in series)  
**Status:** Available to F2P, no membership required  
**Quest Points:** 2 points upon completion

### Quest Start

**Location:** Champions' Guild (south of Varrock)  
**NPC:** Guildmaster  
**Action:** Right-click "Talk-to" Guildmaster  
**Dialogue:** 
```
Guildmaster: "Greetings, adventurer. I am the Guildmaster of the Champions Guild.
Only the strongest and most worthy of champions may enter our guild.
Prove yourself by completing a quest for me..."
```
**Objective:** Find Oziach in Edgeville and ask about the dragon task

### Main Quest Objective

**Go to Edgeville and speak to Oziach**

**Oziach dialogue:**
```
Oziach: "To prove yourself a true champion, you must slay Elvarg, the dragon of Crandor.
But first, you must gather these items and complete these tasks..."
```

---

## PART 2: QUEST REQUIREMENTS (MVP Implementation Model)

### Quest Prerequisite (Gate Access)

**Must have:** 32 Quest Points (completed 32 quest points worth of other quests)
- Forces players to do content before main quest
- Creates progression gate
- Rewards players for exploring

**Cannot proceed if:** <32 quest points
- UI prevents talking to Guildmaster
- Message: "You must complete more quests to enter the Champions Guild!"

### Skill Level Requirements (Recommended, not mandatory)

**Combat Level:** 45+ recommended
- Can attempt with lower, but will be significantly harder
- Level 83 Elvarg is dangerous

**Magic:** 33+ recommended  
- Allows using Telekinetic Grab spell (saves 10,000 coins)
- Without it, must pay fee to proceed

**Prayer:** 37+ recommended
- Unlock Protect from Magic prayer
- Significantly reduces Elvarg's magic damage

**Note:** In MVP, keep skill requirements simple. Just Combat level 40+ minimum to start.

### Item Requirements (Must Gather)

These items **must be collected** before proceeding:

| Item | How to Get | Purpose |
|------|-----------|---------|
| Unfired Bowl | Craft with Crafting 8 | Quest objective |
| Wizard's Mind Bomb | Buy from Rising Sun Inn (30 coins) | Quest objective |
| Lobster Pot | Buy from fishing shops | Quest objective |
| Silk | Buy (30 coins) or steal (Thieving 20) | Quest objective |
| Magic/Ranged Weapon | Obtain or craft | Fight enemies |
| Law Rune + Air Rune | Gather/buy | Telekinetic Grab spell OR pay 10,000 coins |
| Hammer | Get from anywhere | Quest objective |
| Anti-dragon Shield | Obtain during quest | Fight Elvarg |
| 90 Steel Nails | Craft/buy from sawmill | Build boat |
| 3 Planks | Gather/buy | Build boat |
| 2,000 Coins | Gold | Boat passage to Crandor |

**Mechanic:** Quest progress **cannot advance** until all items are collected.

---

## PART 3: QUEST OBJECTIVES (Multi-Stage)

### Stage 1: Gather Items (Fetch Quest)

**Objectives:**
1. Obtain unfired bowl from pottery station
2. Obtain wizard's mind bomb from inn
3. Obtain lobster pot from fishing shop
4. Obtain silk from merchant
5. Obtain law rune and air rune (for spell or pay fee)
6. Obtain hammer
7. Obtain 90 steel nails
8. Obtain 3 planks
9. Gather 2,000 coins

**Journal entry:** "Collect all required items for Oziach"  
**Progress indicator:** "Items collected: 6/9" (displays in quest journal)

### Stage 2: Complete Intermediate Objectives

**Sub-objectives:**
1. Visit Melzar's Maze and defeat lock-guarded enemies
   - Kill Zombie Rat → get key from corpse
   - Kill Ghost → get key
   - Kill Skeleton → get key
   - Kill Zombie → get key
   - Defeat Melzar the Mad (level 43 boss)
   - Collect items from chests within maze

2. Travel to Ice Mountain
   - Collect Anti-dragon Shield (unique item)

3. Gather magical components
   - Obtain components for protection spell

### Stage 3: Build Boat & Travel to Crandor

**Objective:** Travel to Crandor Island

**Mechanics:**
- Use planks + nails to build boat (specific construction task)
- Pay ferryman 2,000 coins to sail to Crandor
- Only way to reach Crandor (blocks access without quest completion)

### Stage 4: Defeat Elvarg (Final Boss)

**Boss:** Elvarg, Green Dragon (Level 83)  
**Location:** Crandor Island  
**Mechanics:**
- Melee attack (physical damage)
- Magic attack (magical damage)
- Dragonfire attack (special - can be blocked with Anti-dragon Shield)
- HP: ~200 (scales with difficulty)
- Drops loot upon defeat

**Combat encounter:**
- Players must use combat skills to defeat
- Prayer provides damage reduction
- Food heals damage taken
- Death = respawn at last bank (items lost if not protected)

---

## PART 4: QUEST JOURNAL & TRACKING SYSTEM

### Quest Journal UI

**Appearance:**
- Panel/window listing all quests
- Each quest color-coded by status

**Color Coding:**
```
Red = Not started (quest available)
Yellow = In progress (quest active)
Green = Completed (quest finished)
```

**For each quest, display:**
- Quest name
- Difficulty rating (Novice, Intermediate, Experienced, Master)
- Current objectives (bulleted list)
- Progress indicator ("3/5 objectives complete")
- Quest points earned upon completion

### Current Objectives Display

**Example for Dragon Slayer:**
```
Quest: Dragon Slayer I
Difficulty: Experienced
Progress: Stage 2/4

Current Objectives:
☐ Collect unfired bowl
☐ Collect wizard's mind bomb
☐ Collect lobster pot
☑ Collect silk (COMPLETED)
☐ Obtain anti-dragon shield
☐ Build boat
☐ Defeat Elvarg

Quest Points Earned: 2
```

### Quest Points System

**What are quest points?**
- Currency earned for completing quests
- 1-3 points per quest (based on difficulty)
- Dragon Slayer = 2 quest points
- Cumulative total shown in quest journal header

**Usage:**
- Quest prerequisites gate other quests (e.g., "32 quest points required")
- Show player progress toward "Quest Cape" (all quests completed = special cape)

---

## PART 5: QUEST REWARDS (MVP Model)

### Upon Completion: Dragon Slayer Rewards

**XP Awards:**
- 18,650 Strength XP
- 18,650 Defence XP
- Player chooses which skill receives the XP (one-time choice)

**Equipment Unlocks:**
- Rune Platebody (can now equip/wear)
- Green D'hide Body (can now equip/wear)
- Dragon Platebody (can now equip/wear)

**Quest Points:** +2 quest points to total

**Special Unlock:**
- Ability to slay dragons on Slayer tasks (post-MVP)
- Dragonfire protection mechanics unlocked (spell resistance)

### Why These Rewards Work

**XP is valuable:**
- 18,650 XP is significant at mid-levels
- Players can choose skill (customization)
- Feels like achievement

**Equipment unlocks are progression:**
- Rune gear is a major power spike
- Creates visible progression
- Enables new content (wearing rune gear unlocks tougher enemies)

**Quest points matter:**
- Gate other quests
- Create long-term goal (collect all quest points)

---

## PART 6: OSRS QUEST STRUCTURE MECHANICS (Core System)

### Quest Types (By Difficulty)

```
Novice (Easy):
- Few prerequisites
- Simple tasks (kill 5 enemies, collect items)
- Low stat requirements
- Quick completion (5-15 minutes)
- 1-2 quest points

Intermediate:
- Some prerequisites  
- Multiple objectives
- Moderate stat requirements
- Medium completion (30-60 minutes)
- 2-3 quest points

Experienced:
- Quest prerequisites required (like Dragon Slayer)
- Complex objectives with sub-tasks
- Recommended high stats
- Lengthy completion (1-3 hours)
- 3-5 quest points

Master:
- Major prerequisites
- Very complex objectives
- High stat requirements
- Very lengthy (3+ hours)
- 5+ quest points
```

**Dragon Slayer = Experienced difficulty**

### Quest Dialogue System (MVP Scope)

**Dialogue flow:**
1. NPC says text (one message box)
2. Player clicks "Continue" or "OK"
3. Next NPC text displays
4. Repeat until dialogue tree complete

**No dialogue choices in MVP** (simple linear dialogue)

**Example dialogue sequence:**
```
[NPC] "Greetings, adventurer."
[Player clicks Continue]

[NPC] "I have a task for you. Defeat the dragon Elvarg."
[Player clicks Continue]

[NPC] "You must gather these items first..."
[NPC lists all required items]
[Player clicks Continue]

[NPC] "Return when you have gathered all items and completed the task."
[Quest: Dragon Slayer ADDED TO JOURNAL]
```

---

## PART 7: QUEST PREREQUISITES & GATING SYSTEM

### How Prerequisites Work

**Soft gating (Recommended):**
- UI warns player: "This quest recommends Combat 40+. You are level 30."
- Player can still attempt, but will struggle

**Hard gating (Required):**
- UI blocks player: "You must have 32 quest points to start this quest."
- Cannot talk to NPC until requirement met
- NPC dialogue: "You are not ready. Prove yourself with other quests first."

### Quest Chains (Sequential)

Example chain:
```
Cook's Assistant (Novice, 0 prerequisites)
  ↓ (0 quest points)
Tutorial Island Quest
  ↓ (2 quest points total)
Dragon Slayer I (requires 32 quest points)
  ↓ (34 quest points total)
Dragon Slayer II (requires 200 quest points + completion of DS1)
```

**In MVP:**
- Main quest line = Dragon Slayer-like quest (requires 32 quest points from other activities)
- Side quests = Novice/Intermediate quests (no prerequisites)
- Completion of main quest allows access to "endgame" quests (post-MVP)

---

## PART 8: MVP QUEST IMPLEMENTATION CHECKLIST

### Quest Journal System
- ✅ Quest journal UI (list all available/in-progress/completed quests)
- ✅ Color coding (red/yellow/green)
- ✅ Current objectives display (bulleted list)
- ✅ Progress tracker ("3/5 objectives complete")
- ✅ Quest points display
- ✅ Difficulty rating display

### Quest Start Mechanics
- ✅ NPC "Talk-to" option in context menu
- ✅ First dialogue initiates quest
- ✅ Quest added to journal immediately
- ✅ Objectives displayed in journal

### Quest Requirements
- ✅ Skill level checks (soft gate: warnings)
- ✅ Quest prerequisite checks (hard gate: blocks start)
- ✅ Item collection tracking (displays in journal)
- ✅ Server validates items before progression

### Quest Objectives
- ✅ Multiple objectives per quest
- ✅ Sub-objectives (kill 5 goblins = 5 steps)
- ✅ Progress tracking (1/5 kills, 2/5 kills, etc.)
- ✅ Automatic completion when objective met

### Quest Combat Encounters
- ✅ Boss encounter mechanics (Elvarg-like)
- ✅ Damage calculation (authority-server validated)
- ✅ Boss health tracking
- ✅ Loot drops on defeat
- ✅ Quest completion trigger on final boss defeat

### Quest Dialogue
- ✅ Simple linear dialogue (no branching)
- ✅ "Continue" button to advance
- ✅ NPC text display
- ✅ Dialogue triggers quest updates

### Quest Rewards
- ✅ XP awards (skill selection by player)
- ✅ Item unlocks (equipment gating)
- ✅ Quest points awarded
- ✅ Achievement/completion message

---

## PART 9: MVP MAIN QUEST LINE DESIGN (Recommended)

### Main Quest: "The Dragon's Curse" (Dragon Slayer Equivalent)

**Setup:**
- Player completes side quests to earn 32 quest points (progression gate)
- Visits Guildmaster at guild (iconic NPC)
- Receives task: "Defeat Elvarg, the dragon of Crandor"
- Must gather items, complete sub-objectives, travel to island, defeat dragon
- Rewards: Major XP, equipment unlock, progression

**Side Quests (3 total):**
1. **The Goblin Problem** (Novice, 5 points)
   - Kill 5 goblins
   - Rewards: 500 XP (choice), 2 quest points

2. **The Lost Artifact** (Intermediate, 10 points)
   - Gather artifact pieces from enemies
   - Rewards: 2,000 XP (choice), 2 quest points

3. **Rescue the Villager** (Intermediate, 12 points)
   - Defeat bandits holding villager
   - Rewards: 2,500 XP (choice), 2 quest points

**After 32 quest points from side quests:**
- Main quest "The Dragon's Curse" unlocks
- 4-stage progression (gather items, complete tasks, travel, defeat dragon)
- Final reward: Major XP + equipment unlock

---

**Status:** LOCKED IN - Quest structure, objectives, rewards, journal system, Dragon Slayer as MVP model

