# INVENTORY_SYSTEM.md - Exact OSRS Inventory Mechanics

**Source:** Official OSRS Wiki  
**Status:** LOCKED IN - 28 slot system, looting bag, weight mechanics  
**Purpose:** Core gameplay constraint that drives economy and strategy

---

## CRITICAL GAME DESIGN PRINCIPLE (ULTRATHINK)

**The 28-slot inventory limit is NOT a limitation. It IS the feature.**

Why this matters:
```
28 Slots = Bank Trips Required
  ↓
Bank Trips = Time Spent Moving
  ↓
Time Spent Moving = Slower Resource Gathering
  ↓
Slower Resource Gathering = Economic Scarcity
  ↓
Economic Scarcity = Natural Supply/Demand
  ↓
Supply/Demand = Functioning Economy
```

Without the 28-slot limit:
- ❌ Players accumulate infinite loot per session
- ❌ Resources become abundant
- ❌ Prices crash (no scarcity)
- ❌ Economy breaks
- ❌ Grinding loses meaning

**The 28-slot constraint forces strategic gameplay.**

---

## PART 1: CORE INVENTORY SYSTEM (MVP)

### Fixed Slot Count

**28 slots maximum** - This is hardcoded, non-negotiable.

**Structure:**
```
Inventory = 28 slots
Each slot = 1 item stack (can hold multiple of same item)
Example:
  - Slot 1: 64 Logs (one stack)
  - Slot 2: 1 Iron sword (one stack)
  - Slot 3: 25 Raw chicken (one stack)
  - ... (25 more slots)
```

**No permanent expansion:** There is NO way to increase inventory above 28 slots.

### Stack Mechanics

**Stackable items** (infinite copies in 1 slot):
- Coins
- Bones
- Feathers
- Seeds
- Arrows
- Raw food
- Cooked food
- Potions

**Non-stackable items** (1 per slot):
- Weapons
- Armor
- Tools
- Shields
- Unique items

**Formula:** Each item type has a stack size (or unlimited for stackables).

### Item Actions (Right-Click Context Menu)

**For inventory items:**
```
Left-click: Use item / Equip item
Right-click: Show context menu
  - Drop
  - Examine
  - Use (if applicable)
  - Eat (if food)
  - Equip (if gear)
  - Bury (if bones)
  - Use with (if requires target)
```

**Drop mechanics:**
- Right-click item → "Drop"
- Item disappears from inventory
- Item appears on ground at player's feet
- Visible to all players
- Own items can be picked up instantly
- Other players' items require timer before pickup (later feature)
- Items disappear after 1 hour if not picked up

---

## PART 2: LOOTING BAG (Wilderness Special Item)

### What It Is

**A special inventory container (NOT permanent expansion).**

- Holds up to 28 items (separate from main inventory)
- Only works in Wilderness zones
- Cannot be banked
- Can only withdraw contents at a bank
- If opened ("Open" option), auto-deposits all tradeable loot

### Drop Rate (Exact OSRS Formula)

**Drop rate varies by monster level:**

```
Formula: 1 / (150 / monster_combat_level)

Examples:
- Level 10 monster: 1/15 (6.67% per kill)
- Level 30 monster: 1/5 (20% per kill)
- Level 50 monster: 1/3 (33% per kill)
- Level 100+ monster: 1/1 or close to guaranteed

Average (balanced): ~1/30 (3.3% per kill)
```

**In MVP (Simple Scaling):**
- Low-level Wilderness mobs (Level 10): 1/15 drop rate
- Mid-level Wilderness mobs (Level 30): 1/10 drop rate
- High-level Wilderness mobs (Level 50+): 1/3 drop rate

### How It Works

**When opened ("Open" option):**
- Auto-deposits any tradeable item picked up directly into looting bag
- Main inventory not affected
- Only works for tradeable items (untradeable items still go to main inventory)

**When full (28 items):**
- Cannot add more items
- Must go to bank and withdraw to make space

**Withdrawal (Only at Bank):**
- Right-click looting bag at bank
- "Withdraw" option
- Items move from bag to bank inventory
- Cannot withdraw into main inventory directly

**Cannot be banked:**
- Looting bag itself cannot be put in bank
- Only the CONTENTS can be withdrawn at bank

### Why It's Rare & Valuable

**Risk/Reward mechanics:**
- Wilderness is HIGH RISK (other players can attack you)
- Looting bag is the REWARD for surviving Wilderness
- Only 1/3 to 1/15 drop rate (depends on level)
- Players must return to bank to access contents (time investment)
- Makes Wilderness grinding attractive but expensive/risky

**Economic implications:**
- Creates alternative farming locations (Wilderness)
- Looting bag itself becomes valuable (has trade value)
- Encourages high-risk content for rewards

---

## PART 3: WEIGHT SYSTEM (Affects Run Energy)

### How Weight Works

**Every item has weight (in kg):**

**Example weights:**
- Logs: 2.5 kg each
- Raw chicken: 0.1 kg
- Rune platebody: 24 kg
- Iron sword: 3.4 kg
- Feathers: 0.1 kg each (very light)

**Total weight = sum of all equipped + inventory items**

**Run energy drain:**
- Heavier inventory = faster run energy drain
- Example: 30 kg inventory drains run energy 2× faster than 0 kg
- Affects travel time to/from banks
- Strategic resource gathering consideration

**Negative weight items (optimization):**
- Light capes (spottier cape, graceful gear)
- Boots of lightness
- Can achieve negative weight (-27 kg)
- When wearing negative weight items, normal heavy inventory items carry no penalty
- Allows players carrying heavy resources to move efficiently

### MVP Implementation

**Simple approach:**
- Each item has weight value
- Total weight = sum of all item weights
- Run energy drain formula: base_drain × (1 + weight/100)
- Negative weight items possible but optional

---

## PART 4: BANK SYSTEM (MVP Scope)

### What It Is

**Unlimited storage** (in terms of capacity for MVP purposes).

For MVP: Just implement "infinite bank slots" or very large limit (1000+ slots).
(Post-MVP can implement 600-800 slot limit like real OSRS)

### How It Works

**Access:**
- Right-click banker NPC
- "Bank" option
- Opens bank interface
- Can withdraw/deposit items
- Only works at bank locations (Tutorial Island, main town, etc.)

**Bank interface:**
- Lists all banked items
- Can withdraw items to inventory
- Can deposit items from inventory
- Stackable items combine
- Non-stackable items stack by quantity

**Non-bankable items:**
- Some items cannot be banked
- Message: "A magical force prevents you from banking this item!"
- (Post-MVP feature for restricted items)

### Banking mechanics

**Deposit:**
- Right-click item in inventory while bank open
- Item moves to bank
- Inventory slot freed

**Withdraw:**
- Right-click item in bank
- Item moves to inventory
- Bank slot still shows item (unless empty)

**Stack combining:**
- Multiple stacks of same item merge into one
- Example: 50 logs + 30 logs = 80 logs (1 slot)

---

## PART 5: INVENTORY CONSTRAINT IMPLICATIONS (Critical for MVP)

### Why 28 Slots Matters for Economy

**Scenario 1: Training Woodcutting**
```
Player at log gathering spot:
  1. Inventory = 28 logs (full)
  2. Can't chop anymore (inventory full)
  3. Must run to bank
  4. Run time = 30-60 seconds
  5. Bank deposit = 5 seconds
  6. Run back = 30-60 seconds
  7. Total trip time = 1-2 minutes per 28 logs
  8. Result: Slower resource gathering = scarcity

This time investment makes logs valuable.
Chopping 100 logs/hour = natural supply constraint.
```

**Scenario 2: Looting from Combat**
```
Player killing Chickens:
  1. Inventory fills with bones + raw chicken + feathers
  2. After ~13-15 kills, inventory full
  3. Cannot pick up more loot
  4. Must run to bank OR continue fighting (drop items)
  5. If drop items: ground becomes loot pile (other players can steal)
  6. Risk/reward: Leave loot unguarded vs. return to bank
  7. Result: Inventory management becomes strategic gameplay
```

### Strategic Depth

**Players must make real decisions:**
- "Do I bring more food (takes slots) or more armor (takes slots)?"
- "Do I make bank trip now or continue grinding?"
- "Should I drop bones here (risk theft) to pick up rare drops?"
- "Is the looting bag worth the Wilderness risk?"

**These constraints create gameplay, not frustration.**

---

## PART 6: SPECIAL INVENTORY CONTAINERS (Post-MVP)

Note: These are NOT permanent inventory expansion. They are special items.

### Looting Bag (MVP)
- 28 extra slots in Wilderness only
- Auto-deposits loot
- Cannot be banked
- Only withdraw at bank

### Rune Pouch (Post-MVP)
- Holds runes (magic resources)
- Doesn't count as inventory slot
- Saves 2-3 inventory slots for mages
- Can be used while equipped

### Seed Box (Post-MVP)
- Holds seeds (farming resources)
- Doesn't count as inventory slot
- Saves inventory space

### Herb Sack (Post-MVP)
- Holds herbs (herblore resources)
- Doesn't count as inventory slot
- Saves inventory space

**Key rule:** These containers don't increase inventory slots. They're specialty containers for specific item types.

---

## PART 7: IMPLEMENTATION CHECKLIST (MVP)

### Core Inventory
- ✅ 28-slot hard limit
- ✅ Stack mechanics for stackable items
- ✅ Drop/pickup system
- ✅ Right-click context menu
- ✅ "Drop" removes from inventory, places on ground
- ✅ Dropped items visible to all players
- ✅ Own items can be picked up instantly
- ✅ Other players' items have pickup timer (post-MVP)

### Weight System
- ✅ Each item has weight value
- ✅ Total weight = sum of all items
- ✅ Run energy drain formula uses weight
- ✅ Display total weight in character panel
- ✅ Optional: Light gear can achieve negative weight

### Bank System
- ✅ Bank NPCs have "Bank" option
- ✅ Bank interface lists items
- ✅ Deposit items from inventory
- ✅ Withdraw items to inventory
- ✅ Stackable items merge in bank
- ✅ Large capacity (1000+ slots for MVP)
- ❌ Item-specific banking restrictions (post-MVP)

### Looting Bag (MVP)
- ✅ Wilderness mob drops (1/30 average)
- ✅ Auto-deposit feature (if "Open")
- ✅ 28-item capacity
- ✅ Cannot be banked
- ✅ Withdraw only at bank
- ✅ Right-click "Withdraw" at bank

### Optional (Post-MVP)
- ❌ Rune pouch, seed box, herb sack
- ❌ Non-bankable item restrictions
- ❌ Pickup timers for other players' items
- ❌ Item decay on ground (1-hour despawn)

---

## CRITICAL MEMORY NOTES (FOR IMPLEMENTATION)

**Must remember:**
1. **28 is a hard limit.** No exceptions, no permanent expansion.
2. **Looting bag is a REWARD for Wilderness risk**, not a standard item.
3. **Weight affects run energy**, making strategic loadout choices matter.
4. **Banking is required for extended farming**, which creates natural pacing.
5. **Inventory management IS the game.** Don't trivialize it.
6. **Drop mechanic creates risk** — players must choose: bank trip vs. ground risk.

**Economy implications:**
- 28 slots = resource scarcity
- Scarcity = prices
- Prices = functional economy
- **Remove the 28-slot constraint and the economy breaks.**

---

**Status:** LOCKED IN - 28-slot system, looting bag mechanics, weight system, banking

