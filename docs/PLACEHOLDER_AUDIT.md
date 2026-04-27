# Placeholder & Tutorial Content Audit

Audit of all Tutorial Island / OSRS-verbatim / Lumbridge references remaining in the codebase.
Items are categorized by risk. Safe renames are applied in the same commit; larger migrations have plans.

---

## Category 1 — World Region & Spawn Naming (Low Risk — Renamed)

These are internal constant/config names with no player-visible text. Renamed in this pass.

| Location | Old | New |
|---|---|---|
| `World.java` | `MAIN_WORLD_TUTORIAL_REGION_ID = "tutorial_island_region"` | `MAIN_WORLD_STARTER_REGION_ID = "starter_island_region"` |
| `WorldLoader.java` config keys | `tutorial_spawn_x / tutorial_spawn_y` | `starter_spawn_x / starter_spawn_y` |
| `server/src/main/resources/worlds/main_world/world.yml` | `tutorial_island_region:` map key, `tutorial_spawn_x/y:` keys | `starter_island_region:`, `starter_spawn_x/y:` |
| `server/src/main/resources/worlds/sandbox/world.yml` | `goblin_spawn:` region key | `creature_spawn:` |
| `server/src/main/resources/world.yml` (root copy) | same as main_world/world.yml | same renames |
| `RenderZone.java` | `TUTORIAL_ISLAND` list constant | `STARTER_ISLAND` |
| `AudioZone.java` | `TUTORIAL_ISLAND` list constant | `STARTER_ISLAND` |
| `QuestManager.java` | `TUTORIAL_ISLAND_QUEST_ID = 1` | `STARTER_QUEST_ID = 1` |
| `art/worlds/main_world/scene.yaml` | `tutorial_*` region prefix (8 entries) | `starter_*` prefix |
| `art/worlds/sandbox/scene.yaml` | `"tutorial_paths"` region | `"starter_paths"` |

**Pass 2 additions (same commit):**

| Location | Old | New |
|---|---|---|
| `QuestManager.java` | `isTutorialComplete()` | `isStarterComplete()` |
| `World.java` | `getMainWorldSpawnForTutorialState()` | `getMainWorldSpawnForStarterState()` |
| `ServerPacketHandler.java` | `applyMainWorldTutorialSpawnGate()` | `applyMainWorldStarterSpawnGate()` |
| `ServerPacketHandler.java` | `handleMainWorldTutorialCompletionTransition()` | `handleMainWorldStarterCompletionTransition()` |
| `ServerPacketHandler.java` | local `tutorialComplete` / `inTutorialRegion` | `starterComplete` / `inStarterRegion` |
| `GameContent.java` | `tutorialContentEnabled` / `shouldEnableTutorialContent()` | `starterContentEnabled` / `shouldEnableStarterContent()` |
| `network.proto` | `ADMIN_TRAVEL_MAIN_WORLD_TUTORIAL = 4` | `ADMIN_TRAVEL_MAIN_WORLD_STARTER = 4` |
| `AdminToolsPopup.java` | `ADMIN_TRAVEL_MAIN_WORLD_TUTORIAL` ref + UI label "Main: Tutorial" | `ADMIN_TRAVEL_MAIN_WORLD_STARTER` + "Main: Starter" |
| `TutorialHintOverlay.java` | field `tutorialComplete`, javadoc, UI string `"TUTORIAL"` | `starterComplete`, updated javadoc, `"ARRIVAL"` |
| `FishingRegistry.java` | `TUTORIAL_NET_SPOT` / `"Tutorial Fishing Spot"` | `STARTER_NET_SPOT` / `"Starter Fishing Spot"` |
| `IsometricRenderer.java` | `"Tutorial Guide"` switch cases + Javadoc | added `"Arrival Guide"` cases; kept legacy aliases |
| `GameScreen.java` | `"Tutorial Guide"` NPC name checks (2 sites), vignette comment | added `"Arrival Guide"` checks; legacy aliases kept |
| `MiniMap.java` | OSRS tutorial island comment | generic comment |
| `PlayerRepository.java` | "Tutorial Island center" comment | "starter island center" |

---

## Category 2 — Player-Visible Text (Low Risk — Renamed)

These strings appear in-game. Swapped to Erynfall-appropriate phrasing.

| Location | Old text | New text |
|---|---|---|
| `GameScreen.java` | `"You have been teleported back to Lumbridge."` | `"You have been teleported to the settlement."` |
| `npcs.yaml` examine text | `"The first person you should talk to on Tutorial Island."` | `"The first mentor you meet in Erynfall."` |

---

## Category 3 — Quest & Dialogue Content (Migration Plan — Not Changed Here)

Full quest/dialogue content requires writing new Erynfall lore. Scope is too large for a mechanical rename.

**Quest 1 (quests.yaml):** Named `"Tutorial Island"`, uses OSRS-copied task sequence. Needs full narrative rewrite → rename to `"Erynfall Arrival"` or similar. Assign to lore/content work.

**dialogue.yaml:** All keys use `dialogue_tutorial_*` prefix. NPC says `"Welcome to Tutorial Island!"`. Rewrite to Erynfall setting. Key IDs can be migrated by grep-replace once new content is drafted; the content itself needs authoring.

**NPC names in npcs.yaml:** `"Tutorial Guide"`, `"Combat Instructor"`. These are also used in world.yml and entity_visuals.yaml. Rename once Erynfall NPC names are decided.

---

## Category 4 — Creature & Item Names (Migration Plan — OSRS-Verbatim)

These are OSRS-verbatim names. Changing them requires consistent replacement across items.yaml, FishingRegistry, CookingRegistry, PlayerRepository, GameLoop, and art model/sprite keys.

**Creatures:**
- `"Goblin"` / `goblin_basic` loot / `npc_goblin*` art keys → needs Erynfall creature name decision first
- `"Giant Rat"` / `npc_rat*` / `npc_giant_rat*` art keys → same
- sandbox/world.yml: `"Goblin"` NPC entry; examine text `"A goblin."`

**Fish items:**
- `"Raw shrimps"`, `"Shrimps"`, `"Burnt shrimps"` in items.yaml
- `RAW_SHRIMPS` in FishingRegistry / CookingRegistry
- `PlayerRepository.java:978-980`: hardcoded item name strings
- `GameLoop.java:2046,2361`: article logic for shrimps/anchovies
- sandbox/world.yml comment: `# Tutorial net-only spot — shrimps only`
- items.yaml source comments citing oldschool.runescape.wiki (internal only, low priority)

**Migration path:** Decide new Erynfall names for starter fish + starter creatures, then do a single grep-replace pass across all sites. Art keys (`npc_goblin`, `fish_shrimps_raw`) can be aliased at first (old key → new key in manifest.yaml) then hard-replaced once no references remain.

---

## Category 5 — Code & Class Names (Migration Plan)

**`TutorialIslandMap.java`:** Entire class named after tutorial island. Rename to `StarterIslandMap.java` when the starter zone layout is confirmed (class rename + package reference update). Low risk once done, but confirming the layout is non-trivial.

**`TutorialHintOverlay.java`:** Class name still tutorial-derived. Internal only (never serialized). Rename to `StarterQuestOverlay.java` — update `GameScreen.java` import + field. Trivial once the quest title is settled (currently using `"ARRIVAL"` as the in-panel title).

**`assets/data/worlds/tutorial_island/`:** Directory containing `map.yaml`. Not referenced by any loaded code path — safe to rename to `starter_island/` at any time. No code changes needed.

**`WorldLoader.java:123`:** Default location fallback string `"lumbridge"`. This fallback is used when no explicit spawn config is set. Rename to `"settlement"` or the Erynfall settlement name once decided.

---

## Category 6 — Comments & Proto (Cosmetic)

These are internal-only. No functional impact. Clean up opportunistically.

- `shared/src/main/proto/network.proto:159`: comment `// Tile X of respawn point (Lumbridge)` → `// Tile X of respawn point`
- `GameLoop.java:968`: comment `// Teleport the player to the world spawn point (Lumbridge)` → drop `(Lumbridge)`
- `MusicTrack.java`: comment `// Tutorial Island — town / spawn area` on `NEWBIE_MELODY` → `// Starter island — town / spawn area`
- items.yaml: `# source: oldschool.runescape.wiki` comments → remove once items are renamed

---

## Summary

| Category | Count | Status |
|---|---|---|
| World region / spawn naming | 10 sites | **Renamed** |
| Method / field / constant names | 14 sites | **Renamed** |
| Player-visible text | 3 sites | **Renamed** |
| NPC name string dispatch | 4 sites | **Renamed** (legacy aliases kept until world.yml NPC names land) |
| Quest & dialogue content | ~40 files/keys | Migration plan — needs lore authoring |
| Creature & item names | ~15 sites | Migration plan — needs name decisions |
| Class names | 2 classes | Migration plan — `TutorialIslandMap`, `TutorialHintOverlay` |
| Comments & proto | 6 sites | **Renamed** |
