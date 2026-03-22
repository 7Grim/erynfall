# COMBAT_FEEDBACK.md - Exact OSRS Combat Feel & Feedback

**Source:** Official OSRS Wiki + community feedback  
**Status:** LOCKED IN - All combat feedback mechanics  
**Purpose:** Ensure combat feels exactly like OSRS

---

## HITSPLAT SYSTEM (Damage Numbers)

**What is a Hitsplat?**
A hitsplat is the damage indicator that appears above an entity (player, NPC, monster) in combat. It displays:
- Colored background (indicates damage type)
- Numerical value (amount of damage)
- Animation (floats up, fades, disappears)

### Hitsplat Colors (MVP Scope - Melee/Magic Only)

| Hitsplat Type | Color | Damage Type | Used For | Notes |
|---|---|---|---|---|
| Regular Damage | Red | Melee/Ranged/Magic | Standard hits | Most common |
| Zero Damage | White | N/A | Misses/Blocks | No damage dealt |
| Prayer Drain | Purple | Prayer | Phantom Muspah | Drains prayer points |
| Poison | Green | Poison | Poison effects | Damage over time |
| Healing | Blue | Healing | Rapid heal prayer | Health restoration |
| Shield | Gold | Shield | Boss shields | Damaging shields (Verzik, etc.) |

**For MVP:** Focus on Red (melee hit), White (miss), Green (poison). Other types can be added post-MVP.

### Hitsplat Display Properties

**Appearance:**
- Shape: Rounded rectangular splat with number in center
- Other players' hitsplats: Tinted/darker (less obtrusive)
- Max hit hitsplat: "Trimmed" appearance (special visual indicator)
- Can toggle off in settings (optional feature, post-MVP)

**Animation Timeline:**
- Tick 0: Hitsplat appears above entity's head
- Ticks 0-30 (1.8 seconds): Float upward slowly
- Ticks 20-35: Fade out (opacity decreases)
- Tick 35+: Disappear completely

**Positioning:**
- Appears at entity's head position
- Floats upward during display time
- If entity is surrounded by other hitsplats, may overlap (acceptable)
- Should be visible from game perspective (not hidden behind entities)

### Combat Log (Text-Based History)

**In-Game Chat:**
- Separate "Combat" tab in chat interface
- Shows all combat events in chronological order
- Example: "You hit the Goblin for 47 damage."
- Example: "The Goblin missed you."
- Example: "You gained 150 Attack XP."

**Retention:**
- Last 100 combat messages visible
- Scrollable history
- Cleared on logout or world hop

---

## ATTACK SPEED & ANIMATION FEEDBACK

**Tick Rate: 0.6 seconds per tick**

### Weapon Speed Tiers (MVP Weapons Only)

| Weapon Type | Example | Speed | Ticks | Seconds | Feel |
|---|---|---|---|---|---|
| Fastest | Dagger, Claws | 4 tick | 4 | 2.4s | Very rapid, high click |
| Fast | Longsword, Scimitar | 5 tick | 5 | 3.0s | Balanced, standard |
| Medium | Battleaxe | 6 tick | 6 | 3.6s | Slower, more powerful |
| Slow | Halberd, 2H Sword | 7 tick | 7 | 4.2s | Very slow, massive hits |
| Magic | All combat spells | 5 tick | 5 | 3.0s | Same as longsword |

### Attack Animation Behavior

**Player Character Attack Animation:**
- Played every swing (regardless of hit/miss)
- Synced to weapon speed
- Example: Dagger at 4 ticks = animation plays every 2.4 seconds
- Example: Halberd at 7 ticks = animation plays every 4.2 seconds

**Animation Variations (Post-MVP):**
- Different animations per weapon type
- Slash vs Stab vs Crush animations different (future)
- 2H weapons swing differently than 1H (future)

**For MVP:**
- Single generic melee attack animation
- Play animation every swing (every 4-7 ticks depending on weapon)
- No animation variation (all weapons look similar during MVP)

### Sound Effects

**Hit Sound:**
- Plays when attack CONNECTS (damage > 0)
- Sound effect: "Melee hit" sound (distinctive "thunk" noise)
- Timingplayed immediately when hit lands (same tick as hitsplat appears)
- Example: Dagger swing lands every 2.4 seconds → hit sound plays 2.4 seconds

**Miss Sound:**
- Plays when attack FAILS (damage = 0)
- Sound effect: "Whoosh" or "Swoosh" sound (air swing)
- Timing: Played immediately when hit misses (same tick as hitsplat appears)
- Lower volume than hit sound (less jarring)

**Volume & Mixing:**
- Hit sounds: Loud, satisfying, encouraging
- Miss sounds: Quieter, subtle feedback
- Both should be toggleable in settings (future)

---

## ENEMY DEATH ANIMATIONS (Type-Based)

**Critical Mechanic:** Different enemy types have different death animations in OSRS. This must be replicated exactly.

### Death Animation Types (MVP Scope)

#### Type 1: Humanoid Crouching (Humans, Goblins, Guards, etc.)

**Animation Sequence:**
1. Character receives fatal damage (HP ≤ 0)
2. Character plays "crouch" or "kneel" animation
3. Character drops to knees/crouches (animation takes ~0.6-1.2 seconds)
4. Character disappears/vanishes (fade to transparent, then gone)
5. Items drop on ground (loot appears)

**Timeline:**
- Tick 0: Fatal hit
- Ticks 0-3: Crouch animation plays
- Ticks 3-5: Character fades from view
- Tick 5: Character gone, loot visible

**Visual Effect:**
- Smooth fade-out (opacity goes from 100% → 0%)
- No ragdoll physics
- Clean disappearance

#### Type 2: Monster Fading (Spiders, Dragons, Slimes, etc.)

**Animation Sequence:**
1. Monster receives fatal damage (HP ≤ 0)
2. Monster plays death animation (varies per monster type)
3. Monster fades away (transparency increases)
4. Monster disappears
5. Items drop on ground

**Timeline:**
- Tick 0: Fatal hit
- Ticks 0-3: Death animation (creature-specific, e.g., dragon collapses, spider curls up)
- Ticks 3-6: Fade out
- Tick 6: Monster gone, loot visible

**Visual Effect:**
- Gradual fade-out (opacity decreases over 0.6-1.8 seconds)
- Creature-specific animation before fade
- No ragdoll

#### Type 3: Special Case (Undead - Skeletons, Zombies)

**Animation Sequence:**
1. Undead receives fatal damage
2. Undead collapses/falls apart
3. Undead fades away
4. Items drop

**Special Notes:**
- Skeletons might collapse into bones (visual effect)
- Zombies might fall/crumble
- Differences are cosmetic, all follow fade pattern

### MVP Death Animation Scope

**For MVP, implement:**
- Type 1: Humanoid crouch/fade (for all NPCs, guards, goblins, etc.)
- Type 2: Generic monster fade (all creatures use same fade, no type-specific animation)
- Special animations can be added per-monster post-MVP

**Implementation:**
- Fade-out duration: 1.0-1.5 seconds (matches OSRS)
- Opacity decreases linearly
- Loot appears on ground immediately (doesn't fade in, just appears)

### Loot Display

**When Enemy Dies:**
- Items appear on ground automatically
- Location: Where monster died
- Timing: Immediately after death (doesn't wait for animation to finish)
- Visibility: Always visible (not transparent or faded)
- Pickup: Player can right-click ground loot to pick up (via context menu)

---

## COMBAT FEEDBACK LOOP (MVP Complete Package)

### Moment-by-Moment (Every Attack Cycle)

```
Tick 0: Player attacks enemy
  ├─ Attack animation plays (weapon-specific timing)
  ├─ Server calculates hit/miss + damage
  └─ Packet sent to client

Ticks 1-4: Waiting for hit resolution (weapon-dependent)
  └─ Client prediction: movement, animations play

Tick 4 (Example - 4-tick weapon): Hit resolves
  ├─ Hitsplat appears above enemy head
  ├─ Hitsplat floats upward
  ├─ Sound plays (hit or miss sound)
  ├─ Combat log updates: "You hit Goblin for 47 damage"
  ├─ Enemy health bar decreases (if applicable)
  └─ XP indicator: "+150 Attack XP" (optional, chat message)

Ticks 4-6: Hitsplat display continues
  ├─ Hitsplat fades
  └─ Audio continues

Tick 6: Hitsplat disappears
  └─ System ready for next attack

REPEAT every 2.4-4.2 seconds (depending on weapon)
```

### Enemy Death Sequence

```
Before Death:
- Enemy HP bar shows decreasing health
- Combat continues normally

Moment of Death (HP = 0):
  ├─ Hitsplat appears with final damage
  ├─ Enemy plays death animation
  ├─ Combat log: "You defeated Goblin!"
  ├─ XP award: "+600 Attack XP" (total from kill)
  └─ Sound effect: Death sound (distinctive audio)

Post-Death (1-2 seconds):
  ├─ Enemy fades away
  ├─ Loot items appear on ground
  ├─ Enemy is no longer targetable
  └─ Player can proceed to next enemy

Loot Phase:
  ├─ Right-click on loot to pick up
  ├─ Items go to inventory
  └─ Can leave items on ground (appear for 1 hour, then despawn)
```

---

## IMPLEMENTATION CHECKLIST (MVP)

### Hitsplat System
- ✅ Hitsplat UI element (sprite/texture for damage indicator)
- ✅ Color: Red for hits, White for misses
- ✅ Animation: Float upward + fade out over ~1.5 seconds
- ✅ Display: Above entity head, synced to world position
- ✅ Multiple hitsplats: Can overlap if rapid hits

### Sound Effects
- ✅ Hit sound effect (play on successful damage)
- ✅ Miss sound effect (play on failed attack)
- ✅ Sound timing: Play immediately on hitsplat appearance
- ✅ Both toggleable (settings, post-MVP)

### Attack Animation
- ✅ Play attack animation every swing
- ✅ Timing synced to weapon speed (4-7 ticks)
- ✅ Generic animation for all weapons (MVP)
- ✅ Per-weapon animation (post-MVP)

### Enemy Death
- ✅ Humanoid crouch animation (player-like NPCs)
- ✅ Monster fade animation (creatures)
- ✅ Fade-out duration: 1.0-1.5 seconds
- ✅ Loot appears immediately after death
- ✅ Enemy becomes non-targetable post-death

### Combat Log
- ✅ Text log entry for each hit/miss
- ✅ XP awards logged
- ✅ Enemy death logged
- ✅ Scrollable history (last 100 messages)

### Optional (Post-MVP)
- ❌ Hitsplat color customization
- ❌ Per-weapon attack animations
- ❌ Special death animations per monster type
- ❌ Sound effect customization
- ❌ Floating XP numbers (nice-to-have)

---

## CRITICAL DETAILS

**Timing Precision:**
- Attack speeds must match OSRS ticks exactly (0.6s per tick)
- Hitsplat animation must be smooth, not jerky
- Sounds must play at exact moment of hit

**Feel & Feedback:**
- Hit sounds should be satisfying and encouraging
- Miss sounds should be subtle and non-jarring
- Death animations should be quick (not overly dramatic)
- Loot should appear immediately (no waiting for animations)

**Player Experience:**
- Feedback is instant (no lag between action and feedback)
- Multiple hitsplats visible simultaneously (rapid hits)
- Combat is responsive and snappy

---

**Status:** LOCKED IN - All combat feedback mechanics defined  
**Next:** Implement hitsplat system, attack animations, sound effects

