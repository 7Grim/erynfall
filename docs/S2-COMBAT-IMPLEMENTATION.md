# S2: Combat Basics - Implementation Plan

**Status:** Foundation systems implemented (CombatEngine, Stats, combat state)  
**Timeline:** Weeks 5-8 (Sprint S2)  
**Deliverable:** Tutorial Island fully playable with combat, XP, quests

---

## What's Implemented (Foundation)

✅ **CombatEngine** — Deterministic hit/miss/damage calculation  
✅ **Stats** — Experience tracking + level progression (1-99)  
✅ **Combat State** — Player tracks current combat target + last attack tick  
✅ **Right-Click System** — Ready for "Attack" context menu option  

---

## What's Next (S2 Tasks)

### S2-011: Combat Engine (Hit/Miss Calculation)
- [x] CombatEngine.calculateHit() — Returns HitResult (hit, damage, XP)
- [ ] Integrate into GameLoop.processTick()
- [ ] Server broadcasts CombatHit packets to clients
- [ ] Attack speed tied to weapon (2-6 ticks between attacks)

### S2-012: Combat UI (Damage Numbers, Messages)
- [ ] Damage numbers floating above target
- [ ] "Your attack hit!" / "Your attack missed!" in chat
- [ ] Health bars update visually
- [ ] XP drops show experience gained

### S2-013: Skill Progression (Attack, Strength, Defence)
- [ ] XP awarded to attacker (Strength) + defender (Defence)
- [ ] Levels tracked in Stats class
- [ ] HUD shows current levels
- [ ] Announce level-ups to player

### S2-014: Inventory System
- [ ] Pick up items from ground
- [ ] Drop items from inventory
- [ ] Equip/unequip weapons + armor
- [ ] Equipment affects combat stats

### S2-015: Quest System (State Machine)
- [ ] Load quests from YAML
- [ ] Track quest progress per player
- [ ] Tasks: speak, kill, collect, cook, equip
- [ ] Reward XP on completion

### S2-016: Dialogue System (NPC Conversations)
- [ ] Load dialogue trees from YAML
- [ ] Right-click NPC → "Talk" → dialogue menu
- [ ] Player selects response
- [ ] Dialogue progresses server-side

---

## Protocol Messages (Ready to Add)

```protobuf
// Server → Client (combat result)
message CombatHit {
    int32 attacker_id = 1;
    int32 target_id = 2;
    int32 damage = 3;
    bool hit = 4;
    int32 xp_awarded = 5;
}

// Server → Client (entity take damage)
message HealthUpdate {
    int32 entity_id = 1;
    int32 health = 2;
    int32 max_health = 3;
}

// Server → Client (skill XP gained)
message SkillXP {
    string skill = 1;
    int32 xp_amount = 2;
}

// Client → Server (initiate combat)
message Attack {
    int32 target_id = 1;
}
```

---

## Game Loop Integration (processTick)

Currently `GameLoop.processTick()` is stubbed. It needs to:

```java
private void processTick() {
    // 1. Process player input (movement, combat commands)
    processPlayerInput();
    
    // 2. Update entity positions
    updateEntityPositions();
    
    // 3. Process combat (every entity in combat)
    processCombat();
    
    // 4. Update skills/experience
    updateSkills();
    
    // 5. Send delta updates to clients
    broadcastWorldState();
}

private void processCombat() {
    for (Player player : world.getPlayers().values()) {
        if (!player.isInCombat()) continue;
        
        NPC target = world.getNPC(player.getCombatTarget());
        if (target == null) {
            player.setCombatTarget(-1);
            continue;
        }
        
        // Attack every N ticks (weapon-dependent)
        if (tickCount - player.getLastAttackTick() >= 4) {
            CombatEngine.HitResult result = combatEngine.calculateHit(player, target, tickCount);
            broadcastCombatHit(player, target, result);
            player.setLastAttackTick(tickCount);
        }
    }
}
```

---

## Right-Click Attack Flow

1. **Client:** Right-click on NPC
2. **Client:** Shows context menu with "Attack" option
3. **Player:** Clicks "Attack"
4. **Client:** Sends pathfind to NPC (if not in range) + "Attack" intent
5. **Server:** Receives WalkTo + Attack command
6. **Server:** Player pathfinds to NPC
7. **Server:** Once adjacent, combat starts
8. **Server:** Each tick (4+ ticks), roll hit/miss/damage
9. **Server:** Broadcasts CombatHit packet to all clients
10. **Client:** Renders damage number + health bar update

---

## Code Organization (S2)

```
server/src/main/java/com/osrs/server/
├── combat/
│   ├── CombatEngine.java       ✅ (done)
│   ├── CombatLoop.java         (new: integrated into processTick)
│   └── CombatResult.java       (new: data class)
├── player/
│   ├── Stats.java              ✅ (done)
│   └── SkillProgression.java   (new: XP thresholds, level-up events)
├── quest/
│   ├── Quest.java              (new: YAML-loaded quest definition)
│   ├── QuestManager.java       (new: per-player quest tracking)
│   └── DialogueEngine.java     (new: dialogue state machine)
└── (existing world/, network/, etc.)

client/src/main/java/com/osrs/client/
├── ui/
│   ├── ContextMenu.java        ✅ (done)
│   ├── CombatUI.java           (new: damage numbers, hit messages)
│   ├── DialogueUI.java         (new: dialogue menu)
│   └── InventoryUI.java        (new: inventory grid)
└── (existing network/, renderer/)
```

---

## Testing Checklist (S2-Complete)

- [ ] Right-click NPC → "Attack" option appears
- [ ] Click "Attack" → player pathfinds to NPC
- [ ] Adjacent to NPC → combat starts
- [ ] Every ~4 ticks → hit/miss roll
- [ ] Hit → damage number floats above target
- [ ] Attacker gains XP (Strength)
- [ ] Defender gains XP (Defence)
- [ ] Damage numbers accumulate in UI
- [ ] Right-click NPC → "Talk" option
- [ ] Talk → dialogue menu shows
- [ ] Select response → dialogue progresses
- [ ] Quests appear in UI
- [ ] Kill enemy → quest task marks complete
- [ ] Collect item → quest task marks complete

---

## Time Estimate

- S2-011 (Combat): 3-4 hours
- S2-012 (UI): 2-3 hours
- S2-013 (XP): 1-2 hours
- S2-014 (Inventory): 2-3 hours
- S2-015 (Quests): 2-3 hours
- S2-016 (Dialogue): 2-3 hours
- **Total: 12-18 hours** (2-3 weeks at 10 hrs/week)

---

## Non-Blocking Issues for S2

- Game artist not needed until S4 (can use placeholder sprites)
- Multi-player player interactions can wait (1v1 server-only for MVP)
- Advanced combat mechanics (special attacks, prayers) can wait for post-MVP

---

**Next:** Implement S2-011 (CombatEngine integration into game loop)
