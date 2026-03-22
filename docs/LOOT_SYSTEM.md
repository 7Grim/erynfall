# LOOT_SYSTEM.md - Exact OSRS Drop Tables & Economy Interconnection

**Source:** Official OSRS Wiki drop tables  
**Status:** LOCKED IN - Drop table structure and economy loop  
**Purpose:** Understand how killing → looting → crafting → using → grinding forms complete economy

---

## CRITICAL SYSTEM UNDERSTANDING (ULTRATHINK ANALYSIS)

### The Core Interconnection (Why This Matters)

**Combat kills drop RAW MEAT → Cooking transforms RAW MEAT to COOKED FOOD → Players use COOKED FOOD to sustain combat training → This creates the FULL LOOP.**

This is NOT just "getting loot." This is building an **interdependent economy where all 8 MVP skills reinforce each other.**

```
Combat Training Loop:
  1. Player fights Chicken
  2. Kills drop: Bones + Raw Chicken + Feathers
  3. Raw Chicken goes to Cooking inventory (separate from combat)
  4. Player cooks Raw Chicken → Cooked Chicken
  5. Cooked Chicken heals HP
  6. Player uses Cooked Chicken to heal during extended combat training
  7. More training = more raw meat drops = more cooking XP
  8. More cooking = ability to cook higher-level fish from Fishing
  9. Better food = longer combat sessions = faster training

Resource Loop:
  - Woodcutting: Logs (raw material)
  - Fishing: Fish (raw material)
  - Cooking: Transforms fish to food
  - Combat: Drops meat + bones
  - Prayer: Bury bones for XP
  - All paths feed back into Economy (sell excess to GE)
```

**This is the genius of OSRS design:** No skill is standalone. Each feeds others.

---

## PART 1: DROP TABLE STRUCTURE (OSRS Standard)

### Drop Table Tiers (Every Enemy Has This Structure)

**GUARANTEED DROPS (Always Table):**
Items the enemy **ALWAYS** drops when killed. Examples:
- Bones (for Prayer)
- Meat/Hide (specific to animal type)
- Ashes (for undead)

**Common Drops (75%+ frequency):**
Items dropped frequently. Examples:
- Currency (coins)
- Basic resources (feathers, hides)
- Food byproducts

**Uncommon Drops (10-50% frequency):**
Items dropped occasionally. Examples:
- Weapons/armor drops
- Special resources
- Value varies

**Rare Drops (<10% frequency):**
Rare/unique drops. Examples:
- Boss unique items
- Special event drops
- High-value items

### For MVP (Simple Structure)

Implement 3 tiers for simplicity:
1. **Always (Guaranteed)**
2. **Common (High frequency, ~50-75%)**
3. **Rare (Low frequency, ~5-25%)**

---

## PART 2: MVP ENEMY LOOT TABLES (EXACT OSRS)

### TIER 1: Weakest Enemies (Level 1-5)

#### Chicken (Level 1 Combat)
**Always Drops:**
- Bones (1x) — Prayer XP
- Raw Chicken (1x) — Cooking material, ~30 seconds to cook
- Feathers (5-15x) — Fletching material (future), also sells for coins

**Example drop:** "You loot 1 bones, 1 raw chicken, 12 feathers"

**Economy Value:**
- Bones: ~5-10 gp per bone (low demand early)
- Raw Chicken: ~40-50 gp per meat (after cooking)
- Feathers: ~3 gp per feather (low value, bulk trades)
- **Total per kill:** ~150-200 gp

**Interconnection:**
- Feathers → Fletching (future)
- Raw Chicken → Cooking → Food for other players
- Bones → Prayer XP

#### Rat (Level 2 Combat)
**Always Drops:**
- Bones (1x)
- (No meat - rats don't have meat drops in OSRS)
- Cheese (1x) — Alternate food, healable item

**Example:** "You loot 1 bones, 1 cheese"

**Economy Value:**
- Bones: ~5-10 gp
- Cheese: ~30-40 gp
- **Total per kill:** ~50-60 gp (low profit)

#### Cow (Level 5 Combat)
**Always Drops:**
- Bones (1x) — Prayer XP
- Raw Beef (1x) — Cooking material, heals ~12 HP when cooked
- Cowhide (1x) — Crafting material (not MVP, but always drop)

**Example:** "You loot 1 bones, 1 raw beef, 1 cowhide"

**Economy Value:**
- Bones: ~5-10 gp
- Raw Beef: ~60-80 gp (higher value than chicken - heals more)
- Cowhide: ~100-150 gp (can be tanned → leather → profit)
- **Total per kill:** ~200-250 gp (better profit than chicken)

**Why This Matters:**
- Cowhide future-proofs for Crafting skill
- Raw Beef is MORE VALUABLE than Chicken (heals more)
- Creates incentive to kill cows for profit + training

#### Goblin (Level 5 Combat)
**Always Drops:**
- Bones (1x) — Prayer XP

**Common Drops (~70% chance):**
- Coins (5-25x) — Direct currency

**Economy Value:**
- Bones: ~5-10 gp
- Coins: 5-25 gp (direct currency)
- **Total per kill:** ~15-40 gp

**Why This Matters:**
- Goblins give **coins directly**
- Coins are the primary currency (more valuable per kill than animals)
- Creates incentive to kill goblins for profit over training

---

### TIER 2: Early Mid-Game (Level 6-20)

#### Spider (Level 3 Combat)
**Always Drops:**
- Bones (1x)

**Rare/Uncommon Drops:**
- Spider Web (rare) — Crafting material (future)
- Nothing else usually

**Economy Value:**
- Bones: ~5-10 gp
- Spider Web (rare): ~50-100 gp
- **Total per kill:** ~10-20 gp average (not profitable, just XP)

**Why:** Early training spot, not for profit.

#### Giant Spider (Level 6 Combat)
**Always Drops:**
- Bones (1x)

**Uncommon Drops (~25% chance):**
- Spider Web (1x)
- Various drops (rare)

**Economy Value:**
- Bones: ~5-10 gp
- Spider Web: ~50-100 gp
- **Total per kill:** ~20-40 gp (slightly better)

#### Flesh Crawler (Level 15 Combat)
**Always Drops:**
- Bones (1x)

**Common Drops (~50% chance):**
- Coins (5-20x)
- Raw meat (variable)

**Economy Value:**
- Bones: ~10-20 gp (higher level bones worth more? [future])
- Coins: 5-20 gp
- **Total per kill:** ~20-40 gp

---

### TIER 3: Mid-Game (Level 20+)

#### Giant (Level 43 Combat)
**Always Drops:**
- Bones (1x) — High level bones, worth more

**Common Drops (~60% chance):**
- Coins (50-100x) — Significant currency
- Various drops (armor, weapons - rare)

**Economy Value:**
- Bones: ~50-100 gp (much higher level = more Prayer XP)
- Coins: 50-100 gp
- **Total per kill:** ~150-200 gp (good profit tier)

---

## PART 3: THE INTERCONNECTION FLOW (CRITICAL)

### Why RAW Meat Matters

**Chicken Scenario:**
```
Player Level 1-10: Trains on chickens
  ↓
Kill Chicken → Get Raw Chicken (must cook)
  ↓
Take Raw Chicken to fire/range
  ↓
Cook Raw Chicken → Cooked Chicken (heals 3 HP)
  ↓
Use Cooked Chicken to heal during extended training
  ↓
Extended training = more Cooking XP = faster level-ups
  ↓
Level up Cooking → Can cook higher-level foods
  ↓
Fishing level up → Can catch higher-level fish
  ↓
Catch better fish → Cook for higher heals → Longer training sessions
```

**Why This Works:**
- Raw meat from Combat is **unusable without Cooking**
- Players MUST train Cooking to progress efficiently
- Cooking becomes essential, not optional
- Creates natural skill interconnection

### The Full 8-Skill Loop (MVP)

```
Combat (Melee)
  ├→ Kills drop bones
  │   └→ Bury bones → Prayer XP
  └→ Kills drop raw meat
      └→ Cook meat → Food for healing
          └→ Enables longer combat sessions

Combat (Magic)
  └→ Same bones/meat mechanics

Fishing
  └→ Catch raw fish
      └→ Cook fish → Food for healing
          └→ Enables longer combat sessions
          └→ Drives Cooking demand

Cooking
  ├→ Turns raw meat → usable food
  ├→ Turns raw fish → usable food
  └→ Creates profit (sell cooked food to GE)

Woodcutting
  └→ Harvest logs (future for Fletching)
      └→ Logs → Fletching (future)
          └→ Arrows/bows
              └→ Ranged training (future)

Prayer
  ├→ Bury bones from kills
  ├→ Unlock prayers (buffs)
  └→ Enable longer training with bonuses

Economy (GE)
  ├→ Sell excess meat → profit
  ├→ Sell excess fish → profit
  ├→ Buy meat/fish expensive (high demand)
  ├→ Sell cooked food → high profit
  └→ Buy gear → more damage → better drops
```

**The Genius:** Every system feeds others. No skill is isolated.

---

## PART 4: LOOT DROP RATES (Exact OSRS Mechanics)

### Bones Drop Mechanics

**All animals drop bones.** No exceptions.

**Bone Types by Enemy Level (scales with difficulty):**
```
Level 1-5 enemies: Bones (basic, ~5 XP each when buried)
Level 6-15 enemies: Bones (same type, ~5 XP)
Level 20-40 enemies: Higher quality bones (~15-30 XP when buried)
Level 50+ enemies: Dragon/superior bones (50-200+ XP)
```

**Implementation Note:** Bone type doesn't matter for MVP. Just "Bones" = 1 item drop, always.

### Meat/Hide Drop Mechanics

**All animals drop their specific meat/hide:**
```
Chickens: Raw Chicken (always)
Cows: Raw Beef (always)
Goblins: No meat (humanoid, not animal)
Spiders: No meat (insects don't have meat)
Rats: No meat (OSRS rats don't drop meat)
```

**Raw vs Cooked (CRITICAL):**
- Dropped meat is always RAW
- Players must cook to make usable
- Raw meat CANNOT be eaten (healing = 0)
- Cooked meat heals based on Cooking level

**Cooking Formula:**
- Raw Chicken cooks to Cooked Chicken (heals ~3 HP)
- Raw Beef cooks to Cooked Beef (heals ~6 HP)
- Raw Fish cooks to Cooked Fish (heals variable)
- Higher Cooking level = faster cook time, fewer burns

---

## PART 5: ECONOMY IMPLICATIONS (MVP)

### Money Sinks (What Removes Currency)

**Food Consumption (Daily Cost):**
- Players train combat for hours
- Need food every session
- Cooked food from players is consumed/deleted
- Natural money sink: raw resources → cooked food → eaten

**Gear Upgrades (One-time Cost):**
- Bronze set: cheap
- Iron → Steel → Mithril → Adamant → Rune (escalating cost)
- Worn by many players, replaced at level gates
- One-time major currency sinks

**Prayer Restoration (Repeating Cost):**
- Prayer potions (future) needed to restore PP
- Costs currency
- Repeating money sink for serious prayer users

### Money Sources (What Adds Currency)

**Coin Drops from Enemies:**
- Goblins drop coins
- Giants drop coins
- Guards drop coins
- Direct currency generation

**Resource Sales (Excess to GE):**
- Cooked food surplus
- Excess bones (prayer has overflow)
- Excess feathers (fletching future)
- Excess hides (crafting future)

### Price Discovery (Free Market)

**Example Economy Scenarios:**

**Scenario 1: High demand for food (many players grinding combat)**
- Cooked food price goes UP
- Fish price goes UP
- Players increase Fishing to profit
- Supply increases until equilibrium

**Scenario 2: Many players grinding Prayer**
- Bones demand high
- Bones price increases
- Encourages more combat training
- Combat drop supply increases

**This is organic economy, not balanced by Jagex.** Players decide prices via supply/demand.

---

## PART 6: IMPLEMENTATION CHECKLIST

### Drop Table Structure (MVP Code)

Each enemy should have:
```
{
  name: "Chicken",
  guaranteedDrops: [
    { item: "Bones", quantity: 1, weight: 1.0 },
    { item: "Raw Chicken", quantity: 1, weight: 1.0 },
    { item: "Feathers", quantity: 5-15, weight: 1.0 }
  ],
  commonDrops: [
    // Usually empty for animals, handled by always table
  ],
  rareDrops: [
    // Usually empty for level 1-5 enemies
  ]
}
```

### Raw vs Cooked Handling (CRITICAL)

```
Inventory Item:
  - "Raw Chicken": Cannot be eaten, healing = 0, must cook
  - "Cooked Chicken": Can be eaten, healing = 3 HP
  
When player uses "Eat Raw Chicken":
  - Message: "You try to eat it, but it's raw."
  - No healing
  - Item not consumed

When player cooks "Raw Chicken":
  - Cooking skill used
  - 30 second delay (cooking time)
  - Transforms to "Cooked Chicken"
  - Random chance to burn (decreases with skill)
```

### Prayer Integration

```
When player buries "Bones":
  - Right-click "Bury" on bones
  - Animation plays
  - 1x Bones consumed
  - Prayer XP awarded (4.5 × bone quality)
  - Never fails (unlike Cooking)
```

### Economy GE Integration

```
GE Pricing (Player-Driven):
  - Raw Chicken: ~40-50 gp (variable)
  - Cooked Chicken: ~50-80 gp (usually higher than raw)
  - Feathers: ~3 gp each
  - Raw Beef: ~60-100 gp
  - Bones: ~5-10 gp (variable)
  
Why Cooked > Raw:
  - Cooked requires Cooking effort
  - Cooked is immediately usable
  - Raw requires Cooking training to use
  - Supply is fixed (you only get what you cook)
```

---

## CRITICAL DESIGN RULE (NON-NEGOTIABLE)

**ALL raw meat/fish drops must be RAW. No exceptions.**

If you add "Cooked Chicken" as a direct drop:
- ❌ Players skip Cooking training
- ❌ Food becomes abundant/cheap
- ❌ Cooking skill becomes pointless
- ❌ Economy breaks

**The constraint (raw → requires cooking) is the feature that forces skill interconnection.**

---

**Status:** LOCKED IN - Drop table structure, raw meat mechanics, economy loops

