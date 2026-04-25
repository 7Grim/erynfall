# Visual Parity Checklist

**Scope:** Rules for Erynfall's low-poly OSRS-like visual style.  
**Audience:** Artists, engineers, and AI reviewers deciding whether an asset belongs in the game.  
**Authority:** Derived from `docs/GRAPHICS_STYLE.md`, `docs/SCALE_SPEC.md`, `ARTIST_GUIDE.md`, `art/models/manifest.yaml`, and `art/world/entity_visuals.yaml`. When this document conflicts with those, those win — update this document.

> **How to use:** Each section lists rules as PASS/FAIL criteria. An asset passes if every applicable rule in every relevant section is met. A single FAIL is grounds for revision.

---

## 1 — Player Proportions

### Rules

| # | Rule | Measurable |
|---|------|-----------|
| P1 | Height exactly **1.80 WU** (= 1.80 Blender units before export) | ±0.05 WU tolerance |
| P2 | Head volume **10–15% larger** than a realistic human-proportioned head at the same body height | Compare against `player_base.glb` reference |
| P3 | Torso is broad and square. Shoulder width ≥ 0.5 WU. | Measure in Blender or with F7 overlay |
| P4 | Legs shorter than realistic. Upper-leg length < half of total height. | — |
| P5 | Body is built from **separate mesh objects** per segment — head, torso, upper_arm_l/r, lower_arm_l/r, upper_leg_l/r, lower_leg_l/r, foot_l/r | No single-mesh body |
| P6 | **No vertex skinning.** Each mesh parented rigidly to a bone (Ctrl+P → Bone). | No Armature modifier with vertex groups |
| P7 | Box-modeled from primitive shapes. No subdivision surface, no sculpt data. | Check Blender modifiers stack |
| P8 | Hard edges preferred. Smooth shading on body parts is a FAIL. | Check Blender normal data |
| P9 | Poly count **1,000–2,000 triangles** total across all body part meshes. | Check in Blender statistics |
| P10 | Manifest entry: `category: actor`, `scale: 1.0`, `animated: true`, `source_blend` present. | Check `manifest.yaml` |

### Pass / Fail Examples

| Asset | Result | Note |
|-------|--------|------|
| `player_base.glb` | **Reference** | Canonical — all others measured against this |
| A character mesh at 2.0 WU | FAIL P1 | 11% too tall; rescale to 1.80 |
| A character with smooth-shaded curved limbs | FAIL P8 | Too anatomically soft |
| A single-mesh character with weight painting | FAIL P5, P6 | Rigid parenting required |
| A character with 3,500 triangles | FAIL P9 | Over budget by 75–250% |

### Bone Hierarchy (must match exactly)

```
root → hips → spine → chest → shoulder_l → upper_arm_l → lower_arm_l → hand_l
                             → shoulder_r → upper_arm_r → lower_arm_r → hand_r
                             → neck → head
            → upper_leg_l → lower_leg_l → foot_l
            → upper_leg_r → lower_leg_r → foot_r
```

Any deviation in bone names breaks the renderer's procedural fallback and anchor lookup. Names are case-sensitive.

---

## 2 — NPC Proportions

### Rules

| # | Rule | Measurable |
|---|------|-----------|
| N1 | Height **1.80 WU** for humanoid NPCs. Creature NPCs may differ but must be visually scaled relative to player in a readable, intentional way. | — |
| N2 | Same box-modeling, separate-mesh, rigid-parenting requirements as player (P5–P8). | — |
| N3 | Generic NPC poly count **300–800 triangles**. Named/important NPC **500–1,500**. | — |
| N4 | Three required animation clips: `idle` (2.4s), `walk` (1.2s), `action` (1.8s). | Check NLA tracks in Blender |
| N5 | `entity_visuals.yaml` entry must exist with `definition_id`, `model_key_3d`, and `animated_3d: true`. | — |
| N6 | Manifest entry: `category: actor`, `scale: 1.0`, `animated: true`, `source_blend` present. | — |
| N7 | Silhouette must be **immediately readable** at 2–4 tiles distance. The viewer should be able to identify NPC type (humanoid vs. beast) in under one second. | Manual test in `MODEL_PREVIEW` |

### Pass / Fail Examples

| Asset | Result | Note |
|-------|--------|------|
| `npc_goblin_base.glb` + `npc_goblin_idle.glb` + `npc_goblin_walk.glb` | **Reference** | Goblin archetype (def_id 62) |
| `npc_rat_base.glb` | **Reference** | Small creature |
| An NPC with 2,000 triangles, no `action` clip | FAIL N3, N4 | Over budget; missing required clip |
| An NPC without `entity_visuals.yaml` entry | FAIL N5 | Will not render in world |
| A humanoid NPC with no `idle` animation | FAIL N4 | T-poses in world |

### Relative Size Reference

| NPC type | Approximate height | Rule |
|----------|--------------------|------|
| Human NPC (Banker, Guide) | 1.80 WU | Same as player |
| Goblin | ~1.2–1.4 WU | Shorter, stockier than player |
| Rat | ~0.4–0.6 WU | Visibly much smaller |
| Cow | ~1.4–1.6 WU | Wider than tall |

A goblin that renders at 1.80 WU reads as a human — FAIL.

---

## 3 — Equipment Silhouette Rules

### Rules

| # | Rule | Measurable |
|---|------|-----------|
| E1 | Equipment is a **separate GLB file** per piece. Not embedded in `player_base.blend`. | — |
| E2 | Origin point set to the **anchor attachment point** (the contact face that connects to character). | Check in Blender |
| E3 | Poly count **100–400 triangles** per piece. | — |
| E4 | Manifest entry must include: `equip_slot`, `item_id`, `anchor_name`, all six offset/rotation fields. | Validated by `scripts/validate-models.py` |
| E5 | `anchor_name` must match an anchor node that exists in `player_base`. Valid names: `head_anchor`, `cape_anchor`, `ammo_anchor`, `weapon_anchor`, `shield_anchor`, `body_anchor`, `legs_anchor`, `hands_anchor`, `feet_anchor`. | — |
| E6 | Equipment must not protrude more than **0.5 WU beyond the player bounding box** when equipped at rest. | Check with F7 in `EQUIPMENT_FIT` |
| E7 | `hide_nodes` must list any `player_base` nodes that would visually conflict (z-fight or overlap). At minimum: helmets hide `head` + `hair`; body armor hides `torso_shirt`; boots hide bare-foot nodes. | Check in `EQUIPMENT_FIT` |
| E8 | Equipment silhouette must read clearly in the walk animation — no wild jitter or floating pieces. | Test with `;` → WALK in `EQUIPMENT_FIT` |
| E9 | Equipment naming convention: `equip_<slot>_<material>_<type>.glb`. Examples: `equip_head_iron_full_helm.glb`, `equip_weapon_iron_scimitar.glb`. | — |
| E10 | `source_blend` present in manifest. | — |

### Pass / Fail Examples

| Asset | Result | Note |
|-------|--------|------|
| `equip_head_iron_full_helm.glb` — `anchor_name: head_anchor`, hides `head`, `hair` | PASS | Canonical pattern |
| `equip_head_bronze_full_helm.glb` — `anchor_name: head_anchor`, `hide_nodes: [head, hair]` | PASS | Reference |
| Equipment piece at wrong origin (origin at model center, not attachment face) | FAIL E2 | Will float away from body |
| Equipment with 600 triangles | FAIL E3 | Double budget |
| Equipment manifest missing `anchor_name` | FAIL E4 | Renders at world origin |
| A sword that clips through the player torso at rest | FAIL E6 | Adjust offset in `EQUIPMENT_FIT` |

### Anchor Y Offsets (absolute world units above ground)

These are the expected world-space Y positions of each anchor on a standing player:

| Anchor | Y (WU) |
|--------|--------|
| `feet_anchor` | 0.08 |
| `legs_anchor` | 0.38 |
| `body_anchor`, `weapon_anchor`, `shield_anchor`, `hands_anchor` | 0.82–0.84 |
| `ammo_anchor` | 0.92 |
| `head_anchor` | 1.18 |

Equipment placed at `offset_y: 1.18` + `anchor_name: head_anchor` will render at ~1.18 WU above ground. Use `F8` in `EQUIPMENT_FIT` to verify.

---

## 4 — Item Icon / Model Rules

### Rules

| # | Rule | Measurable |
|---|------|-----------|
| I1 | Every wearable item that can appear in the equipment panel must have a manifest entry with `category: equipment` and a matching `item_id`. | — |
| I2 | Manifest `equip_slot` values must use the canonical slot names: `HEAD`, `CAPE`, `NECK`, `AMMO`, `WEAPON`, `SHIELD`, `BODY`, `LEGS`, `HANDS`, `FEET`, `RING`. | — |
| I3 | The GLB filename must follow the naming convention in E9. No generic or ad-hoc names. | — |
| I4 | Item models must look recognizable as their item type from 1–2 tiles distance. A sword must read as a sword, not a generic stick. | Manual test |
| I5 | No item model should use PBR-style shading. All surfaces: flat vertex colors or a simple 128×128 diffuse texture. | — |
| I6 | Ground-drop items (loot on the floor) render as simple billboard icons in the 3D renderer. No separate ground-drop 3D models needed. | — |

### Pass / Fail Examples

| Asset | Result | Note |
|-------|--------|------|
| `equip_weapon_iron_scimitar.glb`, `equip_slot: WEAPON`, `item_id: 1323` | PASS | Correct naming + slot + id |
| A weapon GLB named `sword_v2_final.glb` | FAIL I3 | Non-standard name |
| Equipment with a 512×512 PBR texture | FAIL I5 | No PBR |

---

## 5 — Tree / Resource Model Rules

### Rules

| # | Rule | Measurable |
|---|------|-----------|
| T1 | Resource models (trees, rocks, fishing spots) use `category: resource` in manifest. | — |
| T2 | Origin at bottom-center of the model, at `(0, 0, 0)`. | Check in Blender |
| T3 | Scale `1.0` in manifest. A tree should occupy approximately **1 tile footprint** and stand **1.5–3.0 WU tall**. | — |
| T4 | Trees must have a visually distinct silhouette per species — `tree.glb` (normal), `tree_oak.glb`, `tree_willow.glb`, `tree_maple.glb`, `tree_mahogany.glb`, `tree_yew.glb`, `tree_magic.glb`. Same shape for all variants is a FAIL. | — |
| T5 | Rock models (`rock_copper`, `rock_tin`, `rock_iron`, `rock_coal`, `rock_gold`, `rock_silver`, `rock_mithril`, `rock_adamantite`, `rock_runite`) must use vertex color to indicate ore type. Color must be readable at 2–3 tile distance. | — |
| T6 | Resources are static (no animation). `animated: false` or field absent in manifest. | — |
| T7 | Resource entities use `animated_3d: false` in `entity_visuals.yaml`. | — |
| T8 | Poly count: trees **200–600 triangles**, rocks **100–300 triangles**. | — |
| T9 | Trees must have clearly distinct canopy from trunk — visible separation at top. Trunk and canopy are separate visual regions, even if one mesh. | — |

### Pass / Fail Examples

| Asset | Result | Note |
|-------|--------|------|
| `rock_iron.glb` at 1.0 scale, vertex colored rust-orange | PASS | Readable at distance |
| `tree_willow.glb` — drooping long canopy, green-grey, visually distinct from `tree_oak.glb` | PASS | Correct variety |
| `tree_oak.glb` and `tree_willow.glb` identical meshes, different colors only | FAIL T4 | Must have different silhouettes |
| A tree with 1,200 triangles | FAIL T8 | Double budget |
| Rock with a specular highlight shader | FAIL T5 + § Forbidden | No specularity |

### Ore Color Reference

| Rock | Vertex color hint |
|------|-----------------|
| Copper | Dull orange-red |
| Tin | Mid grey |
| Iron | Rust red-brown |
| Coal | Near-black |
| Gold | Dull yellow |
| Silver | Light grey |
| Mithril | Deep blue-grey |
| Adamantite | Dark green-grey |
| Runite | Deep teal |

---

## 6 — Building Footprint / Height / Roof Rules

### Rules

| # | Rule | Measurable |
|---|------|-----------|
| B1 | Every building shell must be split into **two GLB files**: `<key>_base.glb` (`visibility_group: base`) and `<key>_roof.glb` (`visibility_group: roof`). | Check manifest for paired entries |
| B2 | Both halves must have a `source_blend` entry in manifest. | — |
| B3 | Building wall height must be **taller than the player (1.80 WU)** when placed at the intended scale. At `scale 1.5×`, a `small` shell wall = 2.25 WU. | Calculate: model wall height × placement scale |
| B4 | Terrain procedural walls are 1.2 WU — **do not confuse** with building shell walls. | — |
| B5 | Building shell models are authored at ~2–2.5 BU half-extents and scaled up in `scene.yaml`. Valid placement scales: **1.4–2.5×**. Do not place a shell at `scale: 1.0` — walls will be shorter than the player. | Check `scene.yaml` scale values |
| B6 | Doors: door gaps in walls must align with a walkable tile transition. Door orientation (`rotation_y`) must face the approach direction. | Cross-check `world.yml` door entries |
| B7 | Roof geometry must conceal the interior floor from outside viewing angles. No visible "open ceiling" when looking through a window or door. | Manual test: orbit camera in `WORLD_PLACEMENT` |
| B8 | Prop origin at bottom-center. Buildings must not float above the terrain or sink into it by more than 0.1 WU. | Check with F7 in `WORLD_PLACEMENT` |
| B9 | Building shells use `category: shell` in manifest. General props use `category: prop`. Do not mix. | — |

### Pass / Fail Examples

| Asset | Result | Note |
|-------|--------|------|
| `building_shell_small_base.glb` + `building_shell_small_roof.glb` at `scale: 1.5` | PASS | Wall = 2.25 WU, taller than player |
| `building_shell_service_base.glb` placed at `scale: 1.0` | FAIL B5 | Wall ~1.55 WU — shorter than player |
| A building shell with no `_roof` variant | FAIL B1 | Interior not culled when inside |
| A building whose roof floats 1.0 WU above the walls | FAIL B7 | Visible gap |

### Current Shell Variants

| Key prefix | Notes |
|------------|-------|
| `building_shell_small` | General purpose, most buildings |
| `building_shell_service` | Forge, workshop — slightly taller |
| `building_shell_coastal` | Shorter walls, seaside/dock feel |

---

## 7 — Terrain Color / Material Rules

### Rules

| # | Rule | Measurable |
|---|------|-----------|
| M1 | **No PBR.** No normal maps, no metallic, no roughness, no ambient occlusion maps in any model. | Check Blender material nodes |
| M2 | **Vertex colors preferred** over texture maps for all models. | — |
| M3 | If a UV texture is used: max **256×256 pixels**, pixel-art style, no gradients, no photographic content. | Check texture resolution |
| M4 | Materials use **Lambert/diffuse shading only**. Principled BSDF is acceptable if Metallic = 0, Specular = 0, Roughness = 1. | — |
| M5 | No reflections, no specularity, no emission (except for very specific stylized effects like magic). | — |
| M6 | The visual terrain vocabulary is: `grass`, `water`, `path`, `wall`, `sand`. No other terrain paint types unless added to the system. | Check `terrain_visual` in `scene.yaml` |
| M7 | **Palette:** Skin = warm tan/sand (no pink). Hair = brown/black/grey. Leather = mid-to-dark brown. Metal = grey or dark grey. Wood = tan or medium brown. Rope = straw yellow. Vegetation = desaturated sage or forest green. | — |
| M8 | No white or pure-black (#000000 / #FFFFFF) vertex colors. Ground colors must be muted: greys, tans, dusty browns. | — |
| M9 | Terrain tiles rendered via `TERRAIN_PAINT` use client-side visual data from `scene.yaml`, not `map.yaml`. Do not edit `map.yaml` for visual effect. | — |

### Pass / Fail Examples

| Asset | Result | Note |
|-------|--------|------|
| Model with vertex colors, no texture, Lambert material | PASS | Ideal workflow |
| Model with a 128×128 pixel-art texture, diffuse only | PASS | Acceptable fallback |
| Model with a 512×512 photographic texture | FAIL M3 | Too detailed, wrong aesthetic |
| Model with Roughness map or Normal map | FAIL M1 | No PBR |
| Player skin using pink (#FF99AA) | FAIL M7 | Too saturated/realistic |
| Building with specular highlights | FAIL M5 | Flat matte only |

---

## 8 — Animation Timing / Amplitude Rules

### Rules

| # | Rule | Measurable |
|---|------|-----------|
| A1 | All animation clip durations must be **exact multiples of 600ms (0.6s)**. | Check NLA track duration in Blender |
| A2 | Interpolation: **linear or stepped only** at clip boundaries. No ease-in/ease-out curves at the start or end of a clip. | Check Blender graph editor |
| A3 | Blend time between clips must be **≤ 0.1 seconds** in the runtime config. Animations cut, they do not blend. | — |
| A4 | **Walk clip:** right arm swings forward when left leg steps. Stride must not cause foot-slide. | Visual test at normal walk speed |
| A5 | **Idle clip:** minimal amplitude — torso rise/fall ≤ 0.02 WU, head bob optional and tiny. Do not over-animate. | — |
| A6 | **Attack clips:** first 1–2 frames show wind-up (weapon pulls back). Strike is on frame 3+. Impact frame holds briefly. | — |
| A7 | When the walk clip stops, the character must snap immediately to idle. No deceleration or overshoot. | Test in client |
| A8 | All required player clips must be present: `idle`, `walk`, `run`, `pickup`, `chop`, `mine`, `fish`, `smith`, `cook`, `attack_slash`, `attack_stab`, `attack_shoot`, `attack_throw`, `block`, `death`. Missing clips show as T-pose. | Run `python3 scripts/validate-models.py` |
| A9 | All required NPC clips must be present: `idle`, `walk`, `action`. | — |
| A10 | Clip names must match **exactly** (lowercase, underscore-separated). `Attack_Slash` is not the same as `attack_slash`. | — |

### Duration Reference

| Clip | Duration | Ticks |
|------|----------|-------|
| `idle` | 2.4s | 4 |
| `walk` | 1.2s | 2 |
| `run` | 0.6s | 1 |
| `pickup`, `block` | 0.6s | 1 |
| `attack_slash`, `attack_stab`, `attack_shoot`, `attack_throw`, `smith`, `cook`, `death` | 1.8s | 3 |
| `chop` | 1.2s | 2 |
| `mine`, `fish` | 2.4s | 4 |

A clip at 1.0s (not a 0.6× multiple) is a FAIL. A clip at 1.7s is a FAIL.

### Pass / Fail Examples

| Case | Result | Note |
|------|--------|------|
| `attack_slash` at exactly 1.8s, linear interpolation | PASS | |
| `idle` at 2.4s with dramatic torso sway of 0.3 WU | FAIL A5 | Too animated for OSRS feel |
| `walk` at 1.0s | FAIL A1 | Not a tick multiple |
| Missing `mine` clip in player_base | FAIL A8 | Pickaxe action shows T-pose |
| Smooth ease-out at end of `attack_slash` | FAIL A2 | Kills the snappy OSRS feel |

---

## 9 — Camera Readability Rules

### Rules

| # | Rule | Measurable |
|---|------|-----------|
| C1 | Characters must be clearly readable at **3–5 tiles camera distance** (typical combat distance). At that distance the head, body, and legs are distinct regions. | Orbit camera in `MODEL_PREVIEW` |
| C2 | Resources (rocks, trees) must be distinguishable from each other and from characters when both are in frame. Silhouette is more important than color alone. | Manual test in world |
| C3 | Props must not occlude more than **one tile** of walkable space visually. Tall thin props (fence posts, signposts) are acceptable. | — |
| C4 | No asset should require the player to read fine texture detail to understand what it is. All identification is from silhouette + broad color region. | — |
| C5 | Equipment worn on a character must not dominate the character's silhouette. A full set of plate armor changes the outline but the character remains recognizable as a humanoid. | Test in `EQUIPMENT_FIT` |
| C6 | The 3D camera is a top-down isometric perspective. Assets must look correct from **above and slightly in front**, not optimized for a side or first-person view. | — |
| C7 | For multi-story buildings: the roof/ceiling must be culled when the player is inside (visibility_group: roof hidden). Interior floor and walls must remain visible. | Test with `Y` filter in `WORLD_PLACEMENT` |

### Pass / Fail Examples

| Case | Result | Note |
|------|--------|------|
| `npc_goblin_base.glb` — squat, wide silhouette, clearly not player shape | PASS | |
| A building whose `_base` interior is invisible when roof is culled | FAIL C7 | Interior must always render |
| Fine line-art detail on a 128×128 texture that is unreadable at 3 tiles | FAIL C4 | Texture too detailed for resolution budget |
| Equipment armor so large it hides both arms | FAIL C5 | Silhouette unreadable |

---

## 10 — Forbidden Anti-Style Patterns

These are automatic FAIL conditions regardless of other rule scores. Any one of these disqualifies an asset from inclusion.

| # | Forbidden | Why |
|---|-----------|-----|
| F1 | **Vertex skinning (Armature modifier with vertex groups / weight painting)** | Breaks LibGDX rigid-node animation pipeline. No skinning shaders exist. |
| F2 | **PBR materials** (normal map, roughness map, metallic map, AO map) | LibGDX has no PBR shader. These render flat or broken. Wrong aesthetic. |
| F3 | **Textures larger than 256×256** | Wrong aesthetic. Use vertex colors. |
| F4 | **Photographic textures** | Wrong aesthetic. Erynfall is hand-authored low-poly art. |
| F5 | **Subdiv surface or sculpt data on exported mesh** | Bloats poly count, breaks style. Apply or delete before export. |
| F6 | **`.g3dj` / `.g3db` as primary format for new models** | Legacy transitional only. All new models: `.glb`. |
| F7 | **Equipment authored inside `player_base.blend`** | Must be separate file. Cannot be independently loaded/swapped. |
| F8 | **Animation durations not a multiple of 600ms** | Phase-locks break. Tick-based game actions will visually de-sync. |
| F9 | **Ease curves at clip start/end** | Kills the OSRS snap feel. Linear/stepped interpolation only. |
| F10 | **Copyrighted OSRS asset geometry** (traced or extracted from the real game) | Legal. Erynfall matches visual principles, not copied geometry. |
| F11 | **Skinning: ON in glTF export settings** | Exports incorrect skin data. Must be OFF (see `GRAPHICS_STYLE.md §Blender Export Settings`). |
| F12 | **Models without `source_blend` in manifest** | Future artists cannot modify the source. Required for new models. |
| F13 | **Building shell placed at `scale: 1.0`** | Walls shorter than player. Gameplay-breaking visual error. |
| F14 | **Hand-editing `client/src/main/resources/worlds/`** | Canonical scene source is `art/worlds/`. Use Art Workbench save-back. |
| F15 | **Emissive/glow materials on non-magic assets** | Breaks flat-matte palette. Only acceptable on intentional magical/fire effects. |

---

## 11 — Manifest Completeness Checklist

Every entry in `art/models/manifest.yaml` must satisfy:

| Field | Actor | Equipment | Prop/Shell | Resource |
|-------|-------|-----------|------------|----------|
| `key` | ✓ | ✓ | ✓ | ✓ |
| `file` | ✓ | ✓ | ✓ | ✓ |
| `category` | `actor` | `equipment` | `prop`/`shell` | `resource` |
| `format: glb` | ✓ | ✓ | ✓ | ✓ |
| `scale: 1.0` | ✓ | ✓ | ✓ | ✓ |
| `animated: true` | ✓ | — | — | — |
| `source_blend` | ✓ required | ✓ required | ✓ required | ✓ required |
| `equip_slot` | — | ✓ required | — | — |
| `item_id` | — | ✓ required | — | — |
| `anchor_name` | — | ✓ required | — | — |
| `offset_x/y/z` | — | ✓ required | — | — |
| `rot_x/y/z` | — | ✓ required | — | — |
| `hide_nodes` | — | ✓ required | — | — |

Run `python3 scripts/validate-models.py` to auto-check most of these fields.

---

## 12 — Pre-Commit Art Review Checklist

Before committing any art file, confirm:

- [ ] `python3 scripts/validate-models.py` — zero errors (warnings reviewed)
- [ ] Asset inspected in `MODEL_PREVIEW` — correct scale, orientation, clip behavior
- [ ] If equipment: inspected in `EQUIPMENT_FIT` — no float/clip, correct hide_nodes
- [ ] If NPC/resource: bound in `ENTITY_BINDING` — model_key visible in world
- [ ] If prop/shell: placed in `WORLD_PLACEMENT` — correct footprint and height
- [ ] `git diff art/` reviewed — no unintended scene.yaml or manifest.yaml mutations
- [ ] `.blend` source committed alongside `.glb`
- [ ] Poly count within budget (Section 1, 2, 3, 5 tables)
- [ ] Palette within muted cowboy/western range (Section 7)
- [ ] No forbidden patterns from Section 10 triggered

---

## Quick Reference: Key Numbers

| Measurement | Value |
|-------------|-------|
| 1 tile | 1.0 WU |
| Player/humanoid NPC height | 1.80 WU |
| Terrain procedural wall height | 1.20 WU |
| Building shell wall height (typical 1.5× scale) | 2.25 WU |
| Head anchor Y | 1.18 WU |
| Body/weapon anchor Y | 0.82–0.84 WU |
| Tick duration | 600ms = 0.6s |
| Minimum animation duration | 600ms (1 tick) |
| Max texture size | 256×256 px |
| Player poly budget | 1,000–2,000 tri |
| Generic NPC poly budget | 300–800 tri |
| Equipment poly budget | 100–400 tri |
| Prop poly budget | up to 400 tri (small), 800 tri (medium) |
| Building shell poly budget | up to 2,000 tri |
| Tree poly budget | 200–600 tri |
| Rock poly budget | 100–300 tri |
