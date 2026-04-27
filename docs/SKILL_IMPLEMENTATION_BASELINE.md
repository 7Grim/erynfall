# Skill Implementation Baseline

## Purpose

This document is for engineers and LLMs adding or updating a skill.

It defines:
- the correct OSRS free-to-play skill scope
- what must exist for a skill to be considered real, not partial or misleading
- the minimum implementation checklist across server, client, content, UI, and world support

Use this before proposing or implementing any skill work.

## Correct OSRS F2P Skill Scope

Free-to-play skills in OSRS are:

- Attack
- Strength
- Defence
- Hitpoints
- Ranged
- Prayer
- Magic
- Runecraft
- Woodcutting
- Fishing
- Cooking
- Mining
- Smithing
- Firemaking
- Crafting

Important:
- `Fletching` is **members-only**, not free-to-play.
- Do not include `Fletching` in the F2P completion plan.

## Rule Of Thumb

A skill is not "implemented" just because:
- XP can be granted
- a tab exists
- one interaction works
- guide text exists

A skill is only "complete enough" when a fresh player can actually start, train, and understand it using current world/content support.

## Minimum Skill Completeness Checklist

For any skill, check all of these.

### 1. Shared Data / Source Of Truth

There should be one shared source for the skill's progression/content data when the skill has tiered unlocks or recipes.

Examples:
- `WoodcuttingRegistry`
- `MiningRegistry`
- `CookingRegistry`
- `SmithingRegistry`
- `SpellRegistry`
- `PrayerRegistry`
- `FiremakingRegistry`
- `CraftingRegistry`

When to add a registry:
- tiered tools
- tiered resources
- recipes/products
- unlock tables
- XP values
- level requirements

Do not scatter those values across client and server separately.

### 2. Core Server Mechanics

The server must own the real skill loop.

Questions to answer:
- how does the player start the action?
- what validation occurs?
- what interrupts it?
- when does XP get awarded?
- how does the skill stop?
- is there a level requirement failure message?

Common places:
- `server/src/main/java/com/osrs/server/network/ServerPacketHandler.java`
- `server/src/main/java/com/osrs/server/GameLoop.java`

### 3. Items / Tools / Inputs / Outputs

All required items must exist in item data.

Typical categories:
- tools
- raw resources
- intermediate items
- finished outputs
- consumables needed by the loop

Primary source:
- `server/src/main/resources/items.yaml`

Examples:
- Fishing: rods, bait, fish
- Smithing: ores, bars, hammer, products
- Crafting: cowhide, leather, needle, thread, outputs
- Runecraft: essence, talismans, runes

If a skill needs an item chain and those items do not exist, the skill is not complete.

### 4. World Content / Stations / NPCs

Skills often need world support beyond item data.

Examples:
- trees
- rocks
- fishing spots
- altar
- furnace
- anvil
- range / fire
- tanner
- supplier NPCs
- rune altars

Primary source:
- `server/src/main/resources/world.yml`

Questions:
- can a fresh player find the required world entities?
- are they placed in reachable tutorial/F2P-accessible space?
- are they visually distinguishable?

### 5. Acquisition / Starter Loop

A skill is not finished if players cannot obtain the required starter tools/resources.

Check:
- shop support
- drop support
- world gathering support
- default starter inventory if relevant

Primary source:
- `assets/data/shops.yaml`

If the mechanic exists but the player cannot acquire the materials, the skill is still incomplete.

### 6. Client Interaction Path

The player must be able to actually trigger the skill from the client.

Typical areas:
- `client/src/main/java/com/osrs/client/GameScreen.java`
- side panel / inventory / context menu integration
- right-click actions / item-on-item / item-on-ground flows

Questions:
- is the action discoverable?
- does right-click text match the mechanic?
- does approach-and-act work correctly?
- are failure states visible in chat?

### 7. Visual / Art Support

If the skill depends on visible entities/stations/resources, make sure visual support exists.

Potential sources:
- `art/models/manifest.yaml`
- `art/world/entity_visuals.yaml`
- `art/world/tutorial_island.scene.yaml`

Check whether the player can visually identify:
- resources
- stations
- supplier NPCs
- temporary skill outputs such as fires

### 8. Skill Guide / Details Popup

If clicking the skill in the skills tab does not open an accurate guide, the skill is not finished.

Primary area:
- `client/src/main/java/com/osrs/client/ui/SkillGuideRegistry.java`

Every real skill should have a provider that matches actual gameplay.

At minimum the guide should cover:
- what the skill does
- how to start it
- tools/resources required
- level unlocks or progression tiers
- any important stations or world interactions

### 9. Player Feedback

The skill should communicate what is happening.

Check for:
- success chat
- failure chat
- insufficient level messages
- inventory full messages
- station/tool requirement messages
- level up feedback
- visible animation or world result when appropriate

If a skill works but feels invisible, it is not done enough.

### 10. Persistence / Save Behavior

If the skill affects persistent state, verify persistence.

Examples:
- inventory changes
- XP changes
- level changes
- produced items

Questions:
- does relog preserve results?
- do server saves occur where expected?

### 11. Quest / Tutorial Hooks

If the skill is involved in starter progression or quests, update the relevant hooks.

Examples:
- tutorial-like onboarding
- quest action tracking
- quest item requirements

Do not forget these or the skill will feel disconnected from the game loop.

### 12. Runtime Verification

Compile passing is not enough.

Every skill block should include a runtime sanity pass.

Minimum runtime checks:
- can start the skill
- can perform one successful action
- can hit at least one obvious failure case
- XP is granted correctly
- produced/consumed items are correct
- guide matches reality

## Skill-Type Specific Notes

### Combat-support skills

Examples:
- Attack
- Strength
- Defence
- Hitpoints
- Prayer
- Magic
- Ranged

Extra checks:
- formulas actually use the skill
- XP routing is correct
- guide reflects real formulas/requirements
- support items are obtainable

### Gathering skills

Examples:
- Woodcutting
- Fishing
- Mining

Extra checks:
- resource nodes exist in world
- depletion/respawn works
- inventory full stops correctly
- tool tiers matter

### Processing / utility skills

Examples:
- Cooking
- Firemaking
- Runecraft

Extra checks:
- stations exist and are visible
- temporary world outputs behave correctly if relevant
- item-on-item or item-on-ground flow is intuitive

### Production skills

Examples:
- Smithing
- Crafting

Extra checks:
- recipe chain exists end-to-end
- intermediate materials exist
- products are useful/obtainable through the intended loop

## Free-To-Play Completion Order

For this repo, the correct F2P finalize order is:

1. finish partial skills that already look present
   - Prayer
   - Firemaking
2. finish content/supply support for existing combat skills
   - Magic
   - Ranged
3. implement missing F2P production/utility skills
   - Crafting
   - Runecraft
4. broaden narrow but existing production content
   - Smithing breadth
5. finish any missing skill-guide coverage and correctness
6. run a full F2P skill verification pass

Important:
- `Fletching` is not part of the F2P completion order.

## Before Marking A Skill Done

Ask all of these:

1. Can a fresh player obtain what they need to start it?
2. Can they train it using current world content?
3. Does the server own the real mechanic?
4. Do all required items/stations/resources exist?
5. Does the skill guide open and match reality?
6. Does the player get clear feedback?
7. Was it runtime-tested, not just compiled?

If any answer is no, the skill is not finalized.

## For New LLMs

If you are asked to add or update a skill:

1. read this file first
2. identify whether the skill is F2P or members-only before planning
3. inspect current server, client, items, world, shop, and guide coverage
4. do not assume a skill is complete because one interaction exists
5. always include missing content/support loops in your plan
