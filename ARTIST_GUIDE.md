# ARTIST_GUIDE.md — Erynfall Sprite Pipeline

How to create game art in Aseprite, export it, and see it live in the client.

You do **not** need to understand the code. You need Aseprite, Java 21, and Maven.

---

## Quick start (first time)

1. Install **Aseprite** (aseprite.org — paid, ~$20; or free if self-compiled)
2. Install **Java 21 JDK** and **Maven 3.9+** (same environment as the devs)
3. Clone the repo; your working directory is the repo root

---

## Directory overview

```
art/
  aseprite/          ← Your source files (.aseprite / .ase) — COMMIT THESE
  sprites/           ← Exported flat PNGs — commit when ready
    manifest.yaml    ← Authoritative sprite slot spec (source of truth)
    pack.json        ← TexturePacker settings (do not delete or move)
    tile_grass.png.placeholder   ← Slot waiting for your PNG
    npc_guide.png.placeholder    ← (one .placeholder file per needed sprite)
    ...

scripts/
  export-art.sh      ← Batch export script (Mac / Linux)
  export-art.bat     ← Batch export script (Windows)

client/src/main/resources/
  sprites.atlas      ← Auto-generated — do NOT commit, do NOT edit
  sprites.png        ← Auto-generated — do NOT commit, do NOT edit
```

---

## Asset manifest (source of truth)

`art/sprites/manifest.yaml` is the canonical registry for sprite slots and requirements.
It defines key, expected file name, canvas size, pivot, and whether a slot is required.

Runtime now consumes generated manifest metadata (`sprite-manifest-runtime.json`) for draw alignment.
That means pivot values affect in-game sprite placement, not just offline validation.

Optional shadow tuning fields are also supported per asset:
- `shadow_width`
- `shadow_height`
- `shadow_alpha`

If shadow fields are omitted, the renderer keeps safe built-in defaults.

`*.placeholder` files are convenience labels for artists; they are not the canonical spec.
If docs and placeholders disagree with the manifest, follow the manifest.

---

## Sprite specifications

### Ground tiles

| File name | Canvas | Notes |
|---|---|---|
| `tile_grass.png` | **32 × 16 px** | Isometric diamond; transparent corners |
| `tile_water.png` | 32 × 16 px | |
| `tile_path.png`  | 32 × 16 px | Dirt/dust path |
| `tile_wall.png`  | 32 × 16 px | Stone wall top face |
| `tile_sand.png`  | 32 × 16 px | |

The tile canvas is exactly the diamond footprint. The four corner pixels outside the diamond must be **alpha 0** (transparent). Think top-down rhombus rotated 45°.

```
Tile canvas layout (32 × 16):
   ╱╲    ← transparent corners outside this shape
  ╱  ╲
 ╱    ╲
╲      ╱
 ╲    ╱
  ╲  ╱
   ╲╱
```

#### Optional base material variants

Some base materials support optional deterministic variants.

Naming convention:
- `tile_grass_0.png`, `tile_grass_1.png`, `tile_grass_2.png`
- `tile_path_0.png`, `tile_path_1.png`
- `tile_sand_0.png`, `tile_sand_1.png`
- `tile_wall_0.png`, `tile_wall_1.png`

These are optional. If only the base file exists (for example `tile_grass.png`), rendering still works.
When variants are present, the game picks one deterministically per tile location.

### Optional zone material profile overrides

Zones can optionally override terrain/overlay/clutter art by suffixing a material profile name:
- `tile_grass_fishing.png`
- `tile_grass_fishing_0.png`, `tile_grass_fishing_1.png`, ...
- `tile_path_town.png`
- `edge_wall_base_mining.png`
- `clutter_grass_1_woodcutting.png`

Lookup order is deterministic and always falls back safely:
1. profile-specific variant (for example `tile_grass_fishing_0`)
2. profile-specific base (for example `tile_grass_fishing`)
3. global variant (for example `tile_grass_0`)
4. global base (`tile_grass`)

These profile-specific assets are optional. If absent, the renderer uses current global assets automatically.

### Optional transition and clutter sprites

The renderer supports optional environment richness keys (shore/path/wall overlays and sparse clutter).
These are **not required** slots and are skipped silently if missing, so gameplay and map behavior are unchanged.

Examples:
- Transition overlays: `edge_shore_n`, `edge_shore_s`, `edge_shore_e`, `edge_shore_w`, `edge_path_grass_n`, `edge_wall_base`
- Clutter: `clutter_grass_1`, `clutter_path_1`, `clutter_sand_1`, `clutter_reeds_1`

All optional overlay/clutter canvases use `32 x 16` with `tile-diamond` pivot in `manifest.yaml`.

### Optional water and coastline assets

Water/coastline enrichment supports optional layered assets:
- `water_shimmer_0.png`, `water_shimmer_1.png`, ...
- `water_sparkle_0.png`, `water_sparkle_1.png`, ...
- `shore_foam_n.png`, `shore_foam_s.png`, `shore_foam_e.png`, `shore_foam_w.png`
- `shore_wet_n.png`, `shore_wet_s.png`, `shore_wet_e.png`, `shore_wet_w.png`

Intent:
- Shimmer: subtle animated water movement on top of base water
- Sparkle: very sparse highlight accents (not every water tile)
- Foam: light coast accent where water meets land
- Wet edge: darker/moister shoreline transition on adjacent land tiles

All of these slots are optional. If they are absent, current water rendering still works.

### Optional fake AO / crevice overlays

The renderer also supports an optional authored fake ambient-occlusion layer for local depth:
- Wall/base creases: `ao_wall_base`, `ao_wall_inner_ne`, `ao_wall_inner_nw`, `ao_wall_inner_se`, `ao_wall_inner_sw`
- Shoreline creases: `ao_shore_n`, `ao_shore_s`, `ao_shore_e`, `ao_shore_w`
- Path/grass seams: `ao_path_grass_n`, `ao_path_grass_s`, `ao_path_grass_e`, `ao_path_grass_w`

Important intent:
- These are **not** dynamic shadows and **not** screen-space AO.
- Treat them as baked-looking material depth and contact darkening only.
- Keep values subtle; avoid heavy black outlines.
- Use the same `32 x 16` tile-diamond alignment rules as other overlays.
- All AO keys are optional; missing keys are skipped gracefully.

### Humanoid NPCs and player

| File | Canvas | Anchor |
|---|---|---|
| `player.png` | **16 × 24 px** | Bottom-center |
| `npc_guide.png` | 16 × 24 px | Bottom-center |
| `npc_instructor.png` | 16 × 24 px | Bottom-center |
| `npc_goblin.png` | 16 × 24 px | Bottom-center |
| `npc_banker.png` | 16 × 24 px | Bottom-center |

Bottom-center anchor means: the **bottom row of pixels** sits at ground level (the tile origin). Horizontally, the sprite is centred on that point.

### Creatures

| File | Canvas | Anchor |
|---|---|---|
| `npc_rat.png` | **16 × 12 px** | Bottom-center |
| `npc_chicken.png` | 16 × 12 px | Bottom-center |
| `npc_giant_rat.png` | **24 × 20 px** | Bottom-center |
| `npc_cow.png` | 24 × 20 px | Bottom-center |

### Trees and objects

| File | Canvas | Anchor |
|---|---|---|
| `tree.png` | **16 × 32 px** | Bottom-center |
| `tree_oak.png` | 16 × 32 px | Bottom-center |
| `tree_willow.png` | 16 × 40 px | Bottom-center (willow is taller) |
| `tree_teak.png` | 16 × 32 px | Bottom-center |
| `tree_maple.png` | 16 × 36 px | Bottom-center |
| `tree_mahogany.png` | 16 × 36 px | Bottom-center |
| `tree_yew.png` | 16 × 40 px | Bottom-center |
| `tree_magic.png` | 16 × 40 px | Bottom-center |
| `fishing_spot.png` | 32 × 16 px | Bottom-center |
| `fire.png` | 16 × 20 px | Bottom-center |
| `rock_copper.png` | 16 × 12 px | Bottom-center |
| `rock_tin.png` | 16 × 12 px | Bottom-center |
| `rock_iron.png` | 16 × 12 px | Bottom-center |
| `rock_silver.png` | 16 × 12 px | Bottom-center |
| `rock_coal.png` | 16 × 12 px | Bottom-center |
| `rock_gold.png` | 16 × 12 px | Bottom-center |
| `rock_mithril.png` | 16 × 12 px | Bottom-center |
| `rock_adamantite.png` | 16 × 12 px | Bottom-center |
| `rock_runite.png` | 16 × 12 px | Bottom-center |

---

## Animated sprites

For anything with movement (player walk, NPC walk), use **Aseprite tags**:

| Tag name | Recommended frames | What it is |
|---|---|---|
| `idle` | 1–2 | Standing still |
| `walk_n` | 4 | Walking north (away from camera) |
| `walk_s` | 4 | Walking south (toward camera) |
| `walk_e` | 4 | Walking east |
| `walk_w` | 4 | Walking west |
| `pickup` | 1–2 | Item pickup action (optional) |
| `chop` | 2–4 | Woodcutting action (optional) |
| `mine` | 2–4 | Mining action (optional) |
| `fish` | 2–4 | Fishing action (optional) |
| `sword` | 2–4 | Basic melee sword action (optional) |
| `spear` | 2–4 | Basic melee spear action (optional) |

The export script converts tags to numbered PNGs:
`player_walk_n_0.png`, `player_walk_n_1.png`, `player_walk_n_2.png`, `player_walk_n_3.png`

Action tags are optional for this pass and do not require directional variants.
If action sprites are missing, the game automatically falls back to the current
ShapeRenderer action poses.

Examples:
- `player_pickup_0.png`
- `player_chop_0.png`, `player_chop_1.png`
- `player_mine_0.png`
- `player_fish_0.png`
- `player_sword_0.png`
- `player_spear_0.png`

### NPC action tags (optional first pass)

NPC action loops are also supported with a single `action` tag per NPC key:
- `npc_rat_action_*`
- `npc_giant_rat_action_*`
- `npc_goblin_action_*`
- `npc_chicken_action_*`
- `npc_cow_action_*`
- `npc_banker_action_*`
- `npc_guide_action_*`
- `npc_instructor_action_*`

Examples:
- `npc_goblin_action_0.png`, `npc_goblin_action_1.png`
- `npc_rat_action_0.png`
- `npc_banker_action_0.png`

These are optional. If action sprites are absent, NPCs continue using existing walk/idle/static fallback behavior.
Directional action variants are not required in this first pass.
These `npc_*_action_*` sequences are now consumed at runtime when NPC action windows are active.

### Ambient environment animation (optional)

Resources and ambient props can also use optional `idle` loops for subtle world motion.

Examples:
- `fishing_spot_idle_0.png`, `fishing_spot_idle_1.png`
- `fire_idle_0.png`
- `tree_oak_idle_0.png`
- `rock_gold_idle_0.png`
- `clutter_reeds_1_idle_0.png`

Style guidance:
- Fishing spots: gentle bobbing/ripple disturbance
- Fires: restrained flicker
- Trees: very light canopy sway only
- Ore rocks: subtle glint (especially higher-tier ores)
- Reeds/clutter: minimal sway

If idle frames are absent, the renderer uses the base static sprite automatically.

---

## Step-by-step workflow

### Step 1 — Create your PNG

**The only requirement is a PNG file in `art/sprites/` with the correct name.**
How you produce that PNG is up to you:

**Option A — Save directly as PNG (simplest)**
Use any tool (Aseprite, Photoshop, Krita, Piskel, etc.) and save straight to `art/sprites/tile_grass.png`. Done — skip to Step 2.

**Option B — Export from Aseprite source files**
Save your `.aseprite` source in `art/aseprite/`, then either:
- Inside Aseprite: **File → Export As** → save to `art/sprites/tile_grass.png`
- Or run the batch export script (useful when you have many files):

  Mac / Linux: `./scripts/export-art.sh`
  Windows: `scripts\export-art.bat`

The export script is only needed for **animated sprites** (Aseprite tagged frames → numbered PNGs). For any static single-image sprite, just save the PNG directly.

**For animated sprites** the required naming format is `{name}_{tag}_{frame}.png`:
- `player_walk_n_0.png`, `player_walk_n_1.png`, `player_walk_n_2.png`, `player_walk_n_3.png`

The export script produces this naming automatically from Aseprite tags.

### Step 3 — Pack the atlas

```bash
mvn generate-resources -pl client -am
```

This first validates `art/sprites/manifest.yaml` against files in `art/sprites/`, then runs LibGDX TexturePacker.
TexturePacker reads every `.png` in `art/sprites/` (ignoring `.placeholder` files) and writes:
- `client/src/main/resources/sprites.atlas`
- `client/src/main/resources/sprites.png`

To bypass validation in an emergency:
```bash
mvn generate-resources -pl client -am -DskipArtValidation=true
```

If `art/sprites/` contains **no PNGs yet** (only `.placeholder` files), TexturePacker produces no output. The game uses ShapeRenderer placeholder graphics. This is expected and the game is still fully playable.

You can skip texture packing during regular Java compilation with:
```bash
mvn compile -pl client -am -DskipTexturePack=true
```

### Step 4 — Run the client

**Mac:**
```bash
mvn exec:java -pl client
```

**Windows / Linux:**
```bash
mvn exec:java -pl client -P non-mac
```

Your sprite should appear in the game in place of the ShapeRenderer shape.

### Step 5 — Iterate fast with F5 hot-reload

While the game is running, you can update sprites without restarting:

1. Edit your sprite in Aseprite and save
2. Re-export: `./scripts/export-art.sh art/aseprite/your_file.aseprite`
3. Repack: `mvn generate-resources -pl client -am`
4. **Press F5 in the game window**

The atlas reloads immediately. You'll see your changes in the running game.

---

## 3D Models (Experimental 3D renderer branch)

The experimental 3D renderer now has an optional static-prop model pipeline.

- Manifest location: `art/models/manifest.yaml`
- Model files directory: `art/models/`
- Runtime model resource directory: `client/src/main/resources/models/`

Current supported model formats for this pipeline:
- `.g3dj`
- `.g3db`

Validation and runtime metadata generation:

```bash
python3 scripts/validate-models.py
```

or as part of Maven resources generation:

```bash
mvn generate-resources -pl client -am
```

During `generate-resources`, model files are automatically copied from
`art/models/` into `client/src/main/resources/models/`.
Do not copy models manually into runtime resources.

This generates:
- `client/src/main/resources/model-manifest-keys.txt`
- `client/src/main/resources/model-manifest-runtime.json`
- copied runtime `.g3dj`/`.g3db` files in `client/src/main/resources/models/`

Important migration note:
- Static prop models are first-pass, optional upgrades.
- If a model file is missing, the renderer falls back to the existing sprite billboard path.
- Sprite impostors remain the default safety net until full 3D migration is complete.
- Some shell/decorative 3D props are placed through `client/src/main/resources/static_props.yaml`.
- Those placements are visual-only for the experimental 3D renderer and currently do not add gameplay collision or interaction.

---

## Replacing a placeholder

Each `.png.placeholder` file in `art/sprites/` is a labelled slot for a sprite that still needs to be made. To fill a slot:

1. Make `art/aseprite/tile_grass.aseprite` (matching the placeholder name minus `.placeholder`)
2. Export: `./scripts/export-art.sh`  → creates `art/sprites/tile_grass.png`
3. You don't need to delete the `.placeholder` file — TexturePacker ignores non-PNG files

---

## What happens when a sprite is missing

The game always falls back gracefully:

| Situation | What you see in-game |
|---|---|
| No atlas at all | ShapeRenderer coloured diamonds for tiles, stick figures for entities |
| Atlas exists, tile sprite missing | ShapeRenderer coloured diamond for that tile only |
| Atlas exists, NPC sprite missing | ShapeRenderer stick figure for that NPC only |

Sprites replace shapes as you add them. The game is always playable.

---

## File naming reference

All file names are **lowercase with underscores**. The atlas key = file name without `.png`.
The manifest is authoritative for this list.

| File | Atlas key | What it replaces |
|---|---|---|
| `tile_grass.png` | `tile_grass` | Green grass tiles |
| `tile_water.png` | `tile_water` | Blue water tiles |
| `tile_path.png` | `tile_path` | Brown dirt path tiles |
| `tile_wall.png` | `tile_wall` | Grey stone wall tiles |
| `tile_sand.png` | `tile_sand` | Sandy/desert tiles |
| `player.png` | `player` | Local player character |
| `npc_guide.png` | `npc_guide` | Tutorial Guide NPC |
| `npc_instructor.png` | `npc_instructor` | Combat Instructor NPC |
| `npc_banker.png` | `npc_banker` | Banker NPC |
| `npc_goblin.png` | `npc_goblin` | Goblin NPC |
| `npc_rat.png` | `npc_rat` | Rat (level 1) |
| `npc_giant_rat.png` | `npc_giant_rat` | Giant Rat (level 3) |
| `npc_chicken.png` | `npc_chicken` | Chicken |
| `npc_cow.png` | `npc_cow` | Cow |
| `tree.png` | `tree` | Standard tree |
| `tree_oak.png` | `tree_oak` | Oak tree |
| `tree_willow.png` | `tree_willow` | Willow tree |
| `tree_teak.png` | `tree_teak` | Teak tree |
| `tree_maple.png` | `tree_maple` | Maple tree |
| `tree_mahogany.png` | `tree_mahogany` | Mahogany tree |
| `tree_yew.png` | `tree_yew` | Yew tree |
| `tree_magic.png` | `tree_magic` | Magic tree (glowing blue) |
| `fishing_spot.png` | `fishing_spot` | Fishing spot |
| `fire.png` | `fire` | Cooking fire |
| `rock_copper.png` | `rock_copper` | Copper rock |
| `rock_tin.png` | `rock_tin` | Tin rock |
| `rock_iron.png` | `rock_iron` | Iron rock |
| `rock_silver.png` | `rock_silver` | Silver rock |
| `rock_coal.png` | `rock_coal` | Coal rock |
| `rock_gold.png` | `rock_gold` | Gold rock |
| `rock_mithril.png` | `rock_mithril` | Mithril rock |
| `rock_adamantite.png` | `rock_adamantite` | Adamantite rock |
| `rock_runite.png` | `rock_runite` | Runite rock |

---

## Colour palette reference

Match these approximate colours when shading tiles to blend with existing UI:

| Tile | Dark variant | Light variant |
|---|---|---|
| Grass | `#3D9927` | `#5B9A1C` |
| Water | `#1F71D1` | `#1657AD` |
| Path  | `#997033` | `#806026` |
| Wall  | `#8A8A8A` | `#666666` |
| Sand  | `#D6BD70` | `#B89E57` |

---

## Common issues

**Sprite looks blurry** — The pipeline uses Nearest filter (no bilinear interpolation), so sprites should stay crisp. If you see blurring, confirm you exported at the correct canvas size (not scaled up).

**Sprite draws at the wrong position** — Check the anchor. Bottom-center means the sprite bottom row must visually be at floor level. If the character floats, add more transparent pixels below the feet.

**Tile has a white/black border** — Make the corner pixels outside the diamond shape fully transparent (alpha 0), not a near-transparent colour.

**`mvn generate-resources` fails** — If `art/sprites/` is completely empty (no `.png` files), that's fine — TexturePacker skips quietly. If it fails for another reason, check that Java 21 is active (`java -version`) and Maven can resolve the `gdx-tools` dependency (requires internet on first run).

---

Questions? Ask in Discord or open a GitHub issue tagged `art`.
