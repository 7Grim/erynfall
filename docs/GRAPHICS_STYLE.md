# GRAPHICS_STYLE.md

## Purpose

This is the authoritative visual and animation style guide for Erynfall.

Read this before authoring or reviewing any art, rig, animation, or rendering code.

Erynfall is a **Western/1800s cowboy world** with **OSRS-style gameplay feel and animation timing**. These are not in conflict — the aesthetic is cowboy, the feel and motion language are OSRS.

---

## Visual Aesthetic

### Core Principles

- **Low-poly, blocky proportions.** NOT anatomically realistic. NOT sculpted. Box modeling from primitive shapes.
- **Flat colors.** Vertex colors are preferred. If UV texture maps are needed, keep them simple (128×128 or 256×256 maximum, pixel-art style).
- **No PBR.** No normal maps, no roughness, no metallic, no ambient occlusion maps. Simple Lambert/diffuse shading only.
- **Minimal specularity.** Flat matte surfaces. No reflections.
- **Simple directional lighting.** One directional light. No dynamic shadows required.
- **OSRS animation feel.** Snappy, tick-locked motion. No floaty easing. Characters snap between poses.

### World Aesthetic

- Western frontier, 1800s cowboy era
- Dusty, sun-bleached color palette: tan, brown, terracotta, faded blue, sage green
- Characters wear cowboy hats, dusters, bandanas, spurs, holsters
- Weapons: revolvers, rifles, shotguns, knives, dynamite, lasso
- Structures: saloons, ranch buildings, mine shafts, general stores, sheriff's offices
- Nature: cacti, dead trees, tumbleweed, mesas, dry creek beds

---

## Character Proportions

### Standard (Adapted from OSRS)

| Feature | Rule |
|---|---|
| Height | 1.8–2.0 world units (standardize all player/NPC characters) |
| Head | Slightly oversized — 10–15% larger than realistic human ratio |
| Torso | Broad and square. Chunky shoulders. Short trunk. |
| Arms | Medium length. Slightly blocky. Not anatomically proportionate. |
| Legs | Shorter than realistic. Wide stance. Cowboy bow-legged stance is fine. |
| Overall silhouette | Compact, readable, geometric — recognizable at a distance as a low-poly humanoid |

### Modeling Technique

- **Box modeling only.** Start from a cube or cylinder primitive. Extrude faces, loop cuts, edge manipulation.
- No subdivision surface. No sculpting. Hard edges preferred over smooth shading on body parts.
- Each body part is a **separate mesh object** — head, torso, upper arms, lower arms, upper legs, lower legs, etc.
- This is the OSRS method. Characters are NOT single continuous meshes. Rigid separate meshes per segment.

### Poly Budget

| Model Type | Target Poly Count |
|---|---|
| Player base | 1,000–2,000 |
| Generic NPC | 300–800 |
| Named/important NPC | 500–1,500 |
| Small prop | up to 400 |
| Medium prop | up to 800 |
| Building shell | up to 2,000 |
| Equipment piece | 100–400 |

---

## Blender Rig Specification

### Method: Rigid Body Part Animation

**CRITICAL — read before rigging anything:**

OSRS characters do not use vertex-weight skinning. Each body part is a **separate mesh object parented directly to a bone as a rigid child**. No weight painting. No Skin modifier.

This is intentional:
- Matches OSRS visual style (body parts rotate as rigid units)
- Compatible with LibGDX `AnimationController` (node animations, not skin animations)
- Simpler Blender workflow — no rigging weight paint step
- Eliminates the need for GPU skinning shaders

If you weight-paint and skin your mesh, the LibGDX pipeline will not handle it correctly without a shader rewrite. Use rigid parenting.

### Armature Hierarchy

Bone names use `_l` / `_r` suffixes. **Names must match exactly** — the renderer's procedural animation fallback and anchor lookup reference these names directly.

```
root (at ground, world center)
└── hips
    ├── spine
    │   └── chest
    │       ├── shoulder_l
    │       │   └── upper_arm_l
    │       │       └── lower_arm_l
    │       │           └── hand_l
    │       ├── shoulder_r
    │       │   └── upper_arm_r
    │       │       └── lower_arm_r
    │       │           └── hand_r
    │       └── neck
    │           └── head
    ├── upper_leg_l
    │   └── lower_leg_l
    │       └── foot_l
    └── upper_leg_r
        └── lower_leg_r
            └── foot_r
```

### Equipment Anchor Nodes

These are bones (or empties) in the armature, added as children of the relevant parent bone. **Names must match exactly** — the renderer finds them by name.

| Anchor Name | Parent Bone | Purpose |
|---|---|---|
| `head_anchor` | `head` | Hats, bandanas, helmets |
| `cape_anchor` | `chest` | Capes, ponchos, dusters (back) |
| `ammo_anchor` | `chest` | Ammo belt, bullet pouches |
| `weapon_anchor` | `hand_r` | Revolver, rifle, axe (held in right hand) |
| `shield_anchor` | `hand_l` | Shield, offhand items (left hand) |
| `body_anchor` | `chest` | Body armor, vests, shirts |
| `legs_anchor` | `hips` | Pants, chaps |
| `hands_anchor` | `chest` | Gloves (attach at wrist area) |
| `feet_anchor` | `hips` | Boots, spurs |

### Rigid Parenting in Blender

For each body part mesh:
1. Select the mesh object
2. Select the bone it belongs to (in Pose mode)
3. Ctrl+P → "Bone" (not "Bone Relative")
4. The mesh will rotate rigidly with the bone

Do NOT use:
- Armature modifier with vertex groups
- Automatic weights
- Weight painting

### Blender Export Settings (GLB)

In File → Export → glTF 2.0 (.glb/.gltf):

| Setting | Value |
|---|---|
| Format | GLB (Binary) |
| Include → Selected Objects | OFF (export all unless intentional) |
| Include → Visible Objects | ON |
| Transform → Y Up | ON |
| Geometry → Apply Modifiers | ON |
| Geometry → UVs | ON (if UV maps present) |
| Geometry → Vertex Colors | ON |
| Armatures → Export Deform Bones Only | OFF |
| Armatures → Add Leaf Bones | OFF |
| Animation → Export | ON |
| Animation → Limit to Playback Range | OFF |
| Animation → Always Export Action | OFF (use NLA tracks) |
| Animation → Export NLA Strips | ON |
| Animation → Group by NLA Track | OFF |
| Animation → Skinning | OFF (rigid body part method — no skin data needed) |

**Output location:** `art/models/<filename>.glb`

**Register in manifest:** Update `art/models/manifest.yaml` after export (see Manifest section below).

---

## Animation Clip Standard

### Timing: OSRS Tick System

- 1 game tick = **600ms (0.6 seconds)**
- All animation durations must be **exact multiples of 600ms**
- This ensures animations stay phase-locked with game actions

### Required Clips — Player Character

Author all clips as **separate NLA tracks** in Blender. Track/action names must match the `Clip Name` column exactly.

| Clip Name | Duration | Ticks | Loop | Notes |
|---|---|---|---|---|
| `idle` | 2.4s | 4 | YES | Subtle breathing weight shift. Minimal. NOT dramatic. |
| `walk` | 1.2s | 2 | YES | Full stride cycle. Server handles direction via Y-rotation. |
| `run` | 0.6s | 1 | YES | Fast stride. Higher knee lift. |
| `pickup` | 0.6s | 1 | NO | Lean forward, grab, stand back. |
| `chop` | 1.2s | 2 | NO | Axe swing — woodcutting. Two-handed forward arc. |
| `mine` | 2.4s | 4 | NO | Pickaxe. Both arms. Overhead swing. |
| `fish` | 2.4s | 4 | NO | Fishing rod cast. |
| `smith` | 1.8s | 3 | NO | Hammer swing on anvil. |
| `cook` | 1.8s | 3 | NO | Stir/tend cooking fire. |
| `attack_slash` | 1.8s | 3 | NO | Standard melee — horizontal slash. |
| `attack_stab` | 1.8s | 3 | NO | Dagger/knife forward thrust. |
| `attack_shoot` | 1.8s | 3 | NO | Revolver/rifle draw-and-fire. |
| `attack_throw` | 1.8s | 3 | NO | Dynamite throw arc. |
| `block` | 0.6s | 1 | NO | Defensive raised-arm pose. |
| `death` | 1.8s | 3 | NO | Fall forward or backward. Does not loop. |

### Required Clips — Base NPC

| Clip Name | Duration | Ticks | Loop | Notes |
|---|---|---|---|---|
| `idle` | 2.4s | 4 | YES | Subtle bob or sway |
| `walk` | 1.2s | 2 | YES | Standard stride |
| `action` | 1.8s | 3 | NO | Generic action/attack |

### OSRS Animation Feel Rules

These rules define the motion quality that makes animations feel OSRS-like:

1. **Snappy transitions.** Blend time between clips: ≤ 0.1 seconds. OSRS animations don't blend — they cut. Keep blending minimal.
2. **No easing.** Animations snap to tick boundaries. Do not add ease-in/ease-out curves at clip start/end in Blender's graph editor. Linear or stepped interpolation.
3. **Walk: opposite limb swing.** Right arm swings forward when left leg steps, and vice versa.
4. **Combat wind-up.** First 1–2 frames of attack clips show the wind-up (weapon pulls back). Frame 3+ is the forward strike. Hold on impact.
5. **Idle: very subtle.** The OSRS idle is minimal — a slight breathing rise/fall on the torso, maybe a head bob. Do not over-animate idle.
6. **No footslide.** Walk cycle must not have the feet sliding on the ground when the character is in place. Adjust stride length to match ground contact.
7. **Hard stop.** When walking stops, character snaps immediately to idle — no deceleration slide.

---

## Equipment System

### Overview

Equipment pieces are **separate GLB files** attached to the player_base model at runtime via named anchor nodes.

The renderer looks up `anchor_name` from the manifest, finds that node in the player_base model hierarchy, and places the equipment mesh at that node's world transform.

### Authoring Equipment in Blender

1. Model the equipment piece in a **separate .blend file** (e.g., `art/blender/equipment/equip_head_cowboy_hat.blend`)
2. Set the **origin point** to the anchor attachment point (where it connects to the character)
3. Export as GLB to `art/models/equip_<slot>_<name>.glb`
4. Register in manifest (see Manifest section)

### hide_nodes

When a piece of equipment is worn, certain base character nodes should be hidden to avoid z-fighting or visual overlap.

Examples:
- Cowboy hat → hide `head_hair` (if present)
- Full body armor → hide `torso_shirt`
- Boots → hide `left_foot_bare`, `right_foot_bare`

List exact node names from the Blender armature hierarchy in the manifest `hide_nodes` field.

### Manifest Entry for Equipment

```yaml
- key: equip_head_cowboy_hat
  file: equip_head_cowboy_hat.glb
  category: equipment
  format: glb
  equip_slot: HEAD
  item_id: 1001
  anchor_name: head_anchor
  offset_x: 0.0
  offset_y: 0.05
  offset_z: 0.0
  rot_x: 0.0
  rot_y: 0.0
  rot_z: 0.0
  hide_nodes: []
  source_blend: equipment/equip_head_cowboy_hat.blend
```

---

## Props and World Assets

### Static Props

- Format: GLB, no animation
- Origin: tile-center, bottom of object at Y=0
- Scale: 1 tile = 1 world unit (see `docs/SCALE_SPEC.md` for the full scale specification)
- Rotation: Y-axis only (placed at tile grid positions)

> **Note:** Building shells are an exception — they use placement scale multipliers (1.4–2.5×).
> See `docs/SCALE_SPEC.md §Building Shells` for details and in-game dimensions.

### Manifest Entry for Props

```yaml
- key: saloon_barrel
  file: saloon_barrel.glb
  category: prop
  format: glb
  scale: 1.0
  origin: tile-center
  source_blend: props/saloon_barrel.blend
```

### Building Shells

- Split into base + roof variants for visibility group support
- `visibility_group: base` — always visible
- `visibility_group: roof` — hidden when player is inside
- Both are separate GLB files

---

## Texturing Rules

### Preference Order

1. **Vertex colors** — preferred for all models. Simple to set up in Blender, no UV unwrap needed, renders correctly in LibGDX.
2. **Simple UV-mapped diffuse texture** — if more detail needed. Max 256×256. Pixel-art style. No gradients.
3. **No PBR** — never use metallic/roughness/specular/normal maps. LibGDX doesn't support PBR without custom shaders.

### Vertex Color Workflow in Blender

1. In Edit Mode → select all faces
2. Add a Color Attribute (vertex color layer)
3. Use Face Map or Paint mode to assign flat colors per face/vertex
4. In Material: use an Attribute node → connect to Principled BSDF Base Color
5. On export: Geometry → Vertex Colors = ON in glTF settings

### Color Palette Guidance

- Skin: warm tan/sand tones (no pink — blocky, desaturated)
- Hair: brown, black, grey
- Denim/pants: faded blue
- Leather: mid brown, dark brown
- Metal: grey, dark grey
- Wood: tan, medium brown
- Rope/hemp: straw yellow

---

## Renderer Notes (For Engineers)

### Animation Playback (LibGDX)

- Models with node animations (rigid body part method) are played back via `AnimationController` from `com.badlogic.gdx.graphics.g3d.utils`
- Clip names in the GLB must match exactly what `normalizePlayerClipName()` maps to
- GLB models are loaded via mgsx gltf (`GLTFLoader`) into a standard LibGDX `Model`
- `model.getAnimation(clipName)` must return non-null for clips to play — verify export includes NLA tracks

### Billboard Fallback

Entities with `sprite_key_2d` but no `model_key_3d` in `entity_visuals.yaml` render as camera-facing billboard quads using the 2D sprite PNG as texture. This is a temporary placeholder, not a supported permanent state.

When a 3D model is authored for an entity, add `model_key_3d` to its entry in `entity_visuals.yaml` and the billboard automatically stops rendering.

### Equipment Anchor Lookup

The renderer finds anchor nodes in player_base by name via `findActorAnchorTransform()`. Node names in the exported GLB must match exactly (see anchor table above). If anchors are not found, equipment will render at world origin.

---

## Manifest Fields Reference

```yaml
- key: <string>              # Unique identifier used in code/YAML references
  file: <filename.glb>       # File in art/models/
  category: <category>       # actor | equipment | prop | shell | resource
  format: glb                # glb (required for new models). g3dj = legacy.
  scale: 1.0                 # World scale multiplier
  origin: tile-center        # tile-center (most models)
  animated: true             # true if model has embedded animation clips
  required: false            # true = hard error on missing file
  source_blend: <path>       # Relative path under art/blender/ — REQUIRED for new models
  notes: "<string>"          # Optional notes

  # Equipment only:
  equip_slot: HEAD           # HEAD|CAPE|NECK|AMMO|WEAPON|SHIELD|BODY|LEGS|HANDS|FEET|RING
  item_id: 1001              # Numeric item ID
  anchor_name: head_anchor   # Must match node name in player_base
  offset_x: 0.0
  offset_y: 0.0
  offset_z: 0.0
  rot_x: 0.0
  rot_y: 0.0
  rot_z: 0.0
  hide_nodes: []             # player_base node names to hide when equipped
```

---

## Adding a New Model — End-to-End Checklist

1. **Author** in Blender under `art/blender/<category>/<name>.blend`
2. **Rig** using rigid body part method (no weight painting) if animated
3. **Add NLA tracks** for all required clips (see Animation Clip Standard above)
4. **Export GLB** to `art/models/<name>.glb` with correct export settings
5. **Register** in `art/models/manifest.yaml` with all required fields
6. **Validate:** `python3 scripts/validate-models.py` — must pass clean
7. **Build:** `mvn -pl client -am -DskipTests package`
8. **Launch artist mode:** `./scripts/run-artist-client.sh`
9. **Inspect** in Art Workbench:
   - `MODEL_PREVIEW` — check scale, silhouette, animation clips
   - `EQUIPMENT_FIT` — if equipment, verify attachment and hide_nodes
   - `ENTITY_BINDING` — if NPC/resource, bind model key to definition_id
   - `WORLD_PLACEMENT` — if prop, verify placement in sandbox world
10. **Review diff** before committing: `git diff art/`

---

## What NOT To Do

- Do not use vertex skinning (Armature modifier with vertex weights). Use rigid bone parenting.
- Do not author equipment inside player_base.blend. Keep equipment as separate files.
- Do not use PBR materials, normal maps, or metallic/roughness.
- Do not commit G3DJ files for new models. GLB only.
- Do not skip `source_blend` for new models — always commit the .blend source.
- Do not use textures larger than 256×256. Prefer vertex colors.
- Do not animate outside tick-multiple durations (must be 0.6s × N).
- Do not use easing curves at clip boundaries — linear/stepped interpolation only.
- Do not hand-edit `client/src/main/resources/worlds/` — use Art Workbench save-back.
