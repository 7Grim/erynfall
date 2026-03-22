# OSRS_REFERENCE.md - Canonical OSRS Design & Mechanics

**⚠️ CRITICAL:** This is the single source of truth for what we're building.  
**Version:** 2007 OSRS (Old School RuneScape), NOT RS3 (modern RuneScape)

---

## 1. Gameplay Loop (Core Feel)

### The Core Mechanic: Mouse Clicking
**THIS IS THE FOUNDATION.** Everything in OSRS uses mouse clicking. Nothing else.

- **Left-click:** Perform default action on any entity/object
  - Left-click tile: Walk there
  - Left-click NPC: Talk to them
  - Left-click item on ground: Pick it up
  - Left-click enemy: Attack them

- **Right-click:** Open context menu with available actions
  - Right-click tile: "Walk here" option
  - Right-click NPC: "Talk to", "Attack", "Trade" options depending on NPC
  - Right-click item: Options like "Drop", "Use", etc.
  - Right-click enemy: "Attack" with different combat styles

**NO KEYBOARD MOVEMENT.** Ever. This is non-negotiable and core to OSRS feel.

### Player-Driven Progression Paths (Choose Your Own Adventure)
Players have complete freedom to pursue different progression paths:

1. **Quest Path:** Follow main quest line + side quests
   - Story-driven progression
   - Can be combined with other paths

2. **Skilling Path:** Focus on non-combat skills
   - Woodcutting, Fishing, Mining, Crafting, Smithing, Cooking, etc.
   - Can be purely PvE
   - Contributes to economy via resources/crafted items

3. **Combat/PvP Path:** Focus on combat skills and fighting
   - Can do dungeons, bosses, or engage in PvP zones
   - Contributes to economy via loot drops

All paths contribute to the **shared economy** via drops, resources, and loot.

### What Makes OSRS Feel Like OSRS
- **Grindy progression:** Leveling skills takes TIME. Lots of it.
- **Simplicity:** Click, wait for action to complete, repeat
- **Freedom:** Do what you want, grind what matters to you
- **Tangible progression:** Every level feels earned
- **Community through economy:** Players depend on each other's loot/resources
- **Ironman alternative:** For players who want hardcore solo mode (no GE interaction)

---

## 2. Combat System

### Core Philosophy
**Simple and grindy.** Combat is not tactical or complex. It's:
- Click enemy
- Select combat style from menu
- Watch number go up (damage numbers)
- Repeat
- Progressively upgrade equipment and stats

### Combat Styles & Skills
Each weapon has multiple attack styles. Selecting a style determines which skill levels up:

**OSRS Attack Types (7 types total):**
1. **Stab** → Levels Attack skill
2. **Slash** → Levels Strength skill
3. **Crush** → Levels Strength skill
4. **Ranged Light** → Levels Ranged skill (faster attack speed)
5. **Ranged Standard** → Levels Ranged skill (balanced)
6. **Ranged Heavy** → Levels Ranged skill (slower, more damage)
7. **Magic** → Levels Magic skill (spell-based)

**Additional styles with Defence:**
- Most weapons can be wielded in "Defensive" mode → Levels both offensive skill + Defence

**Key mechanic:** Attack type selection = skill progression path
- Players choose their combat evolution via attack style selection
- Want high Strength? Use Crush/Slash attacks
- Want balanced Attack/Strength? Use defensive stance
- Want pure Attack? Use Stab attacks

### Combat Skills (The Trinity)
- **Attack:** Improves accuracy (hit chance), gates weapon access
- **Strength:** Improves maximum damage per hit, gates weapon access
- **Defence:** Reduces incoming damage, gates armor access, provides survivability
- **Hitpoints:** Total health pool; increases slowly with damage taken

**XP Gain Per Hit:**
- Melee: 4 XP per damage dealt (Attack or Strength based on style)
- Defensive mode: 1.33 XP per damage to both offensive skill + Defence
- Ranged: 4 XP per damage (Ranged skill) + 2 XP per damage if "long range" style (+3 Defence bonus)
- Magic: 2 XP per damage (Magic skill) + 1.33 XP per damage if defensive
- Hitpoints: 1.33 XP per damage dealt

### Equipment Gates
- Higher skill = access to higher-tier equipment
- A level 40 attack player cannot equip a level 60 sword
- Equipment progression is GATED by skill level
- This creates the grind: level up skill → unlock better gear → kill harder enemies → level faster

### Armor & Protection
- Higher Defence skill = higher-grade armor you can wear
- Armor reduces damage taken
- No armor = low Defence requirement but low protection

### Ranged & Magic
(To be researched for specific mechanics)

### PvP vs PvE Mechanics
- Both use same combat system
- PvE: Simpler AI, predictable damage
- PvP: Players adapt, unpredictable, higher stakes
- Risk/reward in PvP zones (can lose items on death)

---

## 3. Skills & Progression

### All 23 Skills
**Combat Skills:**
- Attack, Strength, Defence, Hitpoints, Ranged, Magic, Prayer

**Gathering Skills:**
- Woodcutting, Fishing, Mining, Hunter, Farming

**Crafting Skills:**
- Smithing, Cooking, Crafting, Herblore, Fletching, Runecrafting

**Utility Skills:**
- Agility, Slayer, Thieving, Construction

### Skill Progression Formula
**XP Requirement Formula:**
- Approximately 10% more XP per level
- Level 2: 83 XP
- Level 3: 174 XP
- By level 30+, doubles every 7 levels
- **Level 92 = 6,517,253 XP = 50% of total XP to 99**
- **Level 99 = 13,034,431 XP total**

### XP Doubling Every 7 Levels
- Level 1-30: Quick progression
- Level 30-85: Noticeable exponential curve
- Level 85-92: Seriously grindy (25% of total XP)
- Level 92-99: Extremely grindy (another 50% of total XP)

### Training Methods (Vary by Skill)
- **Combat:** Fight enemies (different difficulty = different XP rates)
- **Gathering:** Harvest resources (wood, fish, ore, etc.)
- **Crafting:** Combine materials
- **Utility:** Complete activities (run agility courses, lockpick, etc.)

### Key Milestones
- Level 40: Access mid-tier gear
- Level 60: Access higher-tier gear
- Level 70: Access mid-endgame gear
- Level 85: Access near-endgame gear
- Level 99: Max level, prestige achievement

---

## 4. Economy

### Grand Exchange (GE) - Optional
- Central trading hub for buying/selling items
- **NOT mandatory** to participate in
- Ironman mode deliberately blocks GE access (hardcore mode)
- Players can ignore GE and still have fun via direct NPC trading

### Free Market Principle
- **Essential:** Players must ALWAYS be able to make money via trading/flipping
- Buy low, sell high should be a viable path
- No restrictions on player-to-player trading (except Ironman mode)
- Price discovery happens organically via supply/demand

### Gold Sources
- **PvE Loot Drops:** Enemies drop gold + items
- **Skilling:** Selling gathered resources (wood, ore, fish, etc.)
- **Crafting:** Creating items to sell for profit
- **Questing:** Quest rewards include gold
- **PvP Drops:** Defeating players in PvP zones yields loot

### Item Sources
- All items come from:
  - Killing monsters (random drops)
  - Crafting (combining resources)
  - Questing (quest rewards)
  - NPCs (buying/trading)
  - PvP (dropping from defeated players)

### Economic Contribution via All Paths
- **Quest players:** Get loot from bosses/enemies in quests, sell to GE
- **Skilling players:** Harvest resources, sell to crafters and others
- **Combat/PvP players:** Get rare drops, fight for high-value items
- All feed the same economy

### Ironman Mode
- Hardcore mode: Cannot use GE or trade with players
- Must obtain all items solo
- Self-sufficient gameplay
- Still has fun via self-imposed challenges

---

## 5. Items & Equipment

### Equipment Tiers
- Early game (F2P equivalent)
- Mid game
- Late game
- Endgame BiS (Best in Slot)
(To be filled)

### Key Weapons
- Melee
- Ranged
- Magic
- Special weapons
(To be filled)

### Armor Sets
(To be filled)

### Consumables
- Food (healing, effects)
- Potions
- Buffs
(To be filled)

---

## 6. Questing & Story

### Quest Structure
- How do quests work?
- Dialogue system?
- Objectives?
(To be filled)

### Main Quest Line
- What's the story?
- Key quest arcs?
(To be filled)

### Side Quests
- How many?
- What do they offer?
(To be filled)

### Lore & World
(To be filled)

---

## 7. NPCs & World

### Tutorial Island
(To be filled)

### Key Locations
(To be filled)

### NPCs
- Merchants
- Trainers
- Quest givers
- Bosses
(To be filled)

### Monsters & Enemies
- Common enemies
- Boss fights
- Difficulty scaling
(To be filled)

---

## 8. UI & Presentation

### Menu Systems
(To be filled)

### Right-Click Context Menu
(To be filled)

### Inventory System
- Slots
- Mechanics
(To be filled)

### Chat & Communication
(To be filled)

---

## 9. Art Style & Aesthetics

### Graphics
- Isometric perspective?
- Polygon count / fidelity?
- Color palette?
(To be filled)

### Animation Style
(To be filled)

### Sound Design
(To be filled)

---

## 10. Progression Pacing & Grind

### The Exponential XP Curve (CRITICAL)
- Max level: **99 for every skill**
- XP requirement is **exponential**
- **Level 92 = 50% of total XP to reach 99**
- This means: Levels 1-92 = Same XP as 92-99
- The grind DRAMATICALLY ramps up at higher levels

### Early Game (Levels 1-30)
- Quick progression, feels rewarding
- Players unlock new equipment frequently
- New skills feel accessible
- Takes days to weeks per skill
- Gets players invested fast

### Mid Game (Levels 30-60)
- Progression slows noticeably
- Requires more grinding
- Good gear unlocks, meaningful progression
- Takes weeks to months per skill
- Players start specializing in certain skills

### Late Game (Levels 60-85)
- Grinding is real
- Each level takes significantly longer
- Only dedicated players push this far
- Weeks to months per level at the top end
- Players are serious about their path (combat, skilling, etc.)

### Endgame (Levels 85-99)
- Extremely grindy
- Can take months of dedicated grinding to reach 99 in a single skill
- The payoff is prestige and optimization
- Only hardcore/dedicated players reach 99
- This creates a badge of honor

### What Makes the Grind Feel Good?
- **Tangible progression:** Every level increases stats/unlocks gear
- **Milestone achievement:** Level milestones feel earned
- **Next-best-thing:** Always working toward next equipment tier
- **Player autonomy:** Choose what to grind, not forced path
- **Community:** Comparing progress with other players
- **Grindy = rewarding:** The time investment makes achievement meaningful
- **No level cap frustration:** 99 is the goal, but you're never "done" (other skills to level, optimization to pursue)

---

## 11. Prayer System (DETAILED - MVP)

### Prayer Mechanics

**Prayer Points (PP):**
- Maximum PP = Prayer level (e.g., level 50 Prayer = 50 max PP)
- PP regenerates at altars (full restore to max)
- PP consumed by active prayers (1-3 per tick depending on prayer)
- When PP reaches 0, active prayers deactivate (player must restore)

### Gaining Prayer XP

**Method 1: Bury Bones**
- Simple: Bury bone at gravestone or on ground
- Scales with bone quality:
  - Regular Bones (level 1): 4.5 XP
  - Big Bones (level 15): 15 XP
  - Dragon Bones (level 40): 72 XP
  - Ourg Bones (level 50): 140 XP
- Higher-difficulty enemies = better bones = faster Prayer XP

**Method 2: Use Gilded Altar (Future, not MVP)**
- Requires construction skill
- 3.5× XP multiplier
- Requires incense burners

**Method 3: Ectofuntus (Future, not MVP)**
- Special location-based training
- 4× XP multiplier

### Prayer Levels & Unlocks

**Early Game (1-15):**
| Level | Prayer | Effect | Cost/Tick |
|-------|--------|--------|-----------|
| 4 | Thick Skin | +1 Defence bonus | 0.33 |
| 7 | Burst of Strength | +3 Strength bonus | 0.33 |
| 10 | Clarity of Thought | +3 Attack bonus | 0.33 |
| 13 | Sharp Eye | +3 Ranged bonus (future) | 0.33 |
| 15 | Mystic Will | +1 Magic bonus | 0.33 |

**Mid Game (20-40):**
| Level | Prayer | Effect | Cost/Tick |
|-------|--------|--------|-----------|
| 25 | Rock Skin | +2 Defence | 0.5 |
| 28 | Superhuman Strength | +5 Strength | 0.5 |
| 31 | Improved Reflexes | +5 Attack | 0.5 |
| 34 | Rapid Restore | +2 to all combat stats | 0.5 |
| 37 | Protect from Ranged (Overhead) | Immunity to ranged | 1.0 |
| 40 | Protect from Magic (Overhead) | Immunity to magic | 1.0 |
| 40 | Protect from Melee (Overhead) | Immunity to melee | 1.0 |

**Late Game (70+):**
| Level | Prayer | Effect | Cost/Tick |
|-------|--------|--------|-----------|
| 70 | Piety | +8 Attack, +8 Strength, +8 Defence | 2.0 |
| 95 | Rigour | +20% Ranged damage (future) | 2.0 |
| 99 | Augury | +20% Magic damage (future) | 2.0 |

### Overhead Prayers (Protection Prayers)

**Key Mechanic:**
- Only ONE overhead prayer can be active
- Provides complete immunity to that damage type
- Used strategically in combat
- High PP drain (1-2 PP per tick)
- Examples: Protect from Melee, Protect from Magic, Protect from Missiles

### Restoring Prayer Points

**At Altar (Full Restore):**
- Click altar → Fully restore PP to max
- Found in various locations
- No cost, instant, requires priest approval
- **MVP locations:** Tutorial Island altar, starting area altar

**With Prayer Potion:**
- Use prayer potion (crafted from Herblore, future)
- Restores: (Prayer level × 0.25) + 7 PP per dose
- Example: Level 50 Prayer = +19 PP per dose
- Portable, can use mid-combat

**At Home Rejuvenation Pool (Future):**
- Construction skill building
- Fully restores prayer + hitpoints

### Prayer Drain & Sustainability

**Drain Rate:**
- Most prayers: 0.33-2.0 PP per tick
- Overhead prayers: 1-2 PP per tick (expensive)
- Passive prayers: 0.33-0.5 PP per tick (cheap)
- Player can manage PP by toggling prayers on/off

**Example Sustainability:**
- Level 50 Prayer = 50 max PP
- Piety costs 2 PP/tick = 25 ticks = 3.9 seconds of continuous use
- Must toggle off or restore with potions

### Strategic Prayer Usage (Combat)

**Defensive Strategy:**
- Use passive prayer (Thick Skin) = cheap defense
- Swap to overhead prayer (Protect from Melee) when low on HP
- Bury bones after combat to train Prayer naturally

**Offensive Strategy:**
- Use offensive prayer (Burst of Strength) to maximize damage
- Accept PP drain cost
- Manage PP by toggling prayers mid-combat

**Boss Mechanics:**
- Overhead prayers block entire damage type
- No healing on Protect from Melee
- Creates tactical depth (can you tank this attack?)

### Prayer Bar & Management

**UI Display:**
- Prayer bar visible at all times
- Shows current PP / max PP
- Prayers toggle with right-click (quick prayers)
- Can set up custom "quick prayer" bar

---

## 12. PvP (Future - Not MVP)

### Wilderness PvP
(Post-MVP feature)

### Rewards & Risk
(Post-MVP feature)

---

## 12. Special Systems

### Prayer & Buffs
(To be filled)

### Spellcasting
(To be filled)

### Slayer System
(To be filled)

### Minigames
(To be filled)

---

## 13. Key Differences: OSRS vs RS3

### What Changed Between 2007 and Modern RS3?
(To be filled)

### What We're Keeping (2007 OSRS)
(To be filled)

### What We're NOT Doing (RS3 features)
(To be filled)

---

## 14. The "Feel" of OSRS

### What Makes It Special?
(To be filled)

### Tone & Vibe
(To be filled)

### Player Psychology
- Why do people play OSRS?
- What keeps them engaged?
(To be filled)

---

**Last Updated:** 2026-03-17  
**Status:** Awaiting Troy's detailed input to populate all sections
