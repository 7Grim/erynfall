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

### Humanoid NPCs and player

| File | Canvas | Anchor |
|---|---|---|
| `player.png` | **16 × 24 px** | Bottom-center |
| `npc_guide.png` | 16 × 24 px | Bottom-center |
| `npc_instructor.png` | 16 × 24 px | Bottom-center |
| `npc_goblin.png` | 16 × 24 px | Bottom-center |

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
| `tree_maple.png` | 16 × 36 px | Bottom-center |
| `tree_yew.png` | 16 × 40 px | Bottom-center |
| `tree_magic.png` | 16 × 40 px | Bottom-center |
| `fishing_spot.png` | 16 × 16 px | Bottom-center |
| `fire.png` | 16 × 20 px | Bottom-center |
| `rock.png` | 16 × 12 px | Bottom-center |

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

The export script converts tags to numbered PNGs:
`player_walk_n_0.png`, `player_walk_n_1.png`, `player_walk_n_2.png`, `player_walk_n_3.png`

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

This runs LibGDX TexturePacker. It reads every `.png` in `art/sprites/` (ignoring `.placeholder` files) and writes:
- `client/src/main/resources/sprites.atlas`
- `client/src/main/resources/sprites.png`

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
| `npc_goblin.png` | `npc_goblin` | Goblin NPC |
| `npc_rat.png` | `npc_rat` | Rat (level 1) |
| `npc_giant_rat.png` | `npc_giant_rat` | Giant Rat (level 3) |
| `npc_chicken.png` | `npc_chicken` | Chicken |
| `npc_cow.png` | `npc_cow` | Cow |
| `tree.png` | `tree` | Standard tree |
| `tree_oak.png` | `tree_oak` | Oak tree |
| `tree_willow.png` | `tree_willow` | Willow tree |
| `tree_maple.png` | `tree_maple` | Maple tree |
| `tree_yew.png` | `tree_yew` | Yew tree |
| `tree_magic.png` | `tree_magic` | Magic tree (glowing blue) |
| `fishing_spot.png` | `fishing_spot` | Fishing spot |
| `fire.png` | `fire` | Cooking fire |
| `rock.png` | `rock` | Rock / ore node |

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
