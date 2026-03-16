# Artist Integration Guide - OSRS MMORP

**Welcome!** This guide explains how to contribute art to the OSRS MMORP project.

**You do NOT need to understand code.** Just art, file organization, and YAML config.

---

## Quick Start

1. **Clone the repo** (same as dev)
2. **Create art** in designated folders (see below)
3. **Update YAML config files** (simple text, no code)
4. **Create a PR** on GitHub (we'll handle the rest)

---

## File Organization

### Sprite Assets Go Here

```
assets/
├── sprites/
│   ├── player/              ← Character sprites (male, female)
│   │   ├── head_male.png
│   │   ├── body_male.png
│   │   ├── legs_male.png
│   │   ├── head_female.png
│   │   └── ...
│   ├── npc/                 ← NPC sprites (Tutorial Island)
│   │   ├── tutorial_guide.png
│   │   ├── combat_instructor.png
│   │   ├── rat.png
│   │   └── ...
│   ├── items/               ← Item sprites (weapons, armor, food)
│   │   ├── dagger.png
│   │   ├── iron_armor.png
│   │   ├── bread.png
│   │   └── ...
│   └── ui/                  ← UI elements (buttons, icons, inventory)
│       ├── button_attack.png
│       ├── icon_health.png
│       └── ...
├── tilesets/                ← Tile sprites (grass, water, rock, etc.)
│   └── tutorial_island.png  ← Single spritesheet with all tiles
└── data/
    └── sprites.yaml         ← Metadata (you'll update this)
```

---

## Sprite Specifications

### Isometric Perspective

All sprites are drawn in **isometric view** (diagonal angle):

```
Example tile (32×16 pixels):
  ╱╲  ← top point
 ╱  ╲
╱    ╲ ← sides slope at 45°
╲    ╱
 ╲  ╱
  ╲╱  ← bottom point

Width:  32 pixels
Height: 16 pixels
```

**Key**: The sprite occupies a tilted rectangle, not a square. This creates the classic isometric look.

### Dimensions

| Sprite Type | Width | Height | Notes |
|-------------|-------|--------|-------|
| **Tile (terrain)** | 32 | 16 | Single tile floor/wall |
| **NPC/Player (idle)** | 32 | 32 | Character standing still |
| **NPC/Player (moving)** | 32 | 32 | Each frame same size |
| **Item** | 16 | 16 | Small icon |
| **UI element** | Varies | Varies | Buttons, icons, etc. |

### Animation

**Each animation is a separate PNG file** (not spritesheet):

```
sprites/npc/tutorial_guide.png       ← Idle frame 1
sprites/npc/tutorial_guide_f2.png    ← Idle frame 2 (if animated)
```

**Naming convention:**
- Base: `name.png`
- Frame 2+: `name_f2.png`, `name_f3.png`, etc.

**Example (4-frame idle animation):**
```
tutorial_guide.png
tutorial_guide_f2.png
tutorial_guide_f3.png
tutorial_guide_f4.png
```

### Color

- **Format**: PNG (8-bit or 32-bit)
- **Transparency**: Alpha channel (PNG transparency)
- **Palette**: 256 colors max (8-bit) or full RGB (32-bit)
- **Background**: Transparent (not white or black)

---

## How to Register Your Sprites

Update `assets/data/sprites.yaml` (simple YAML file, no code required):

### Example 1: NPC Sprite

```yaml
sprites:
  - id: 1000
    name: "Tutorial Guide"
    category: "npc"
    file: "sprites/npc/tutorial_guide.png"
    
    # Animation (optional)
    frames: 4
    frame_duration: 200    # Milliseconds per frame
    loop: true             # Loop animation?
    
    # Display dimensions
    width: 32
    height: 32
    
    # Offset (if sprite needs positioning adjustment)
    offset_x: 0
    offset_y: 0
```

### Example 2: Tile Spritesheet

```yaml
tiles:
  - id: 0
    name: "Grass"
    file: "tilesets/tutorial_island.png"
    sprite_sheet: true        # This is a spritesheet
    columns: 10               # 10 tiles wide
    rows: 10                  # 10 tiles high
    tile_width: 32
    tile_height: 16
```

### Example 3: Item Sprite

```yaml
items:
  - id: 101
    name: "Logs"
    file: "sprites/items/logs.png"
    width: 16
    height: 16
    stack_limit: 64
```

---

## Workflow: Add a New NPC Sprite

### 1. Draw the Sprite
- Draw NPC in isometric view (32×32 pixels)
- Use transparent background (PNG alpha channel)
- Save as `sprites/npc/my_npc_name.png`

### 2. Add to sprites.yaml
Open `assets/data/sprites.yaml` and add:

```yaml
sprites:
  - id: 1010
    name: "My NPC Name"
    category: "npc"
    file: "sprites/npc/my_npc_name.png"
    frames: 1
    width: 32
    height: 32
```

### 3. Commit & Push
```bash
git checkout -b feature/art-my-npc
git add assets/sprites/npc/my_npc_name.png
git add assets/data/sprites.yaml
git commit -m "Add My NPC sprite"
git push origin feature/art-my-npc
```

### 4. Create PR on GitHub
1. Go to GitHub repo
2. Create Pull Request: `feature/art-my-npc` → `main`
3. Title: "Add My NPC sprite"
4. Description: "Idle animation, isometric view, 32×32 px"
5. Request review

**Dev will review + merge. Done!**

---

## File Naming Rules

### Sprites
- Use **lowercase** + underscores
- No spaces, no special characters
- Examples:
  - ✅ `tutorial_guide.png`
  - ✅ `tutorial_guide_f2.png`
  - ❌ `Tutorial Guide.png`
  - ❌ `tutorial-guide.png`

### YAML IDs
- Use **sequential integers** (1000, 1001, 1002, etc.)
- Keep ranges consistent:
  - `1000-1099` = NPCs
  - `1100-1199` = Players
  - `100-199` = Items
  - `0-99` = Terrain tiles

---

## Collaboration Tips

### Discord Channel
Use `#parsundra` for art discussion. Share progress, ask questions, get feedback.

### Iteration
- Post WIPs (work in progress) early
- Dev will test your sprites in the engine
- Adjust if rendering looks off
- Loop until perfect

### Attribution
You'll be credited in:
1. Game credits (in-game menu)
2. GitHub repository (CONTRIBUTORS.md)
3. Release notes

---

## Common Issues & Fixes

### "Sprite looks stretched/distorted"
- Isometric sprites are **not square**. They should look tall/narrow.
- Check dimensions: 32×32 for characters, 32×16 for tiles.
- Compare to examples in reference folder.

### "Transparent background shows as black/white"
- Use **PNG format** with alpha channel (transparency).
- Not JPG.
- Verify in image editor: background should show checkered pattern, not solid color.

### "Sprite is too big/small"
- Follow size specs above (32×32 for NPCs, 32×16 for tiles).
- If in doubt, ask in Discord before spending time.

### "Animation frames look choppy"
- Increase `frame_duration` in sprites.yaml (milliseconds per frame).
- Try 200-300ms for idle animations, 100-150ms for running.

---

## Tools You Might Use

### Free
- **Aseprite** alternative: Piskel.app (browser-based, free)
- **GIMP** (free, powerful)
- **Krita** (free, artist-friendly)

### Paid
- **Aseprite** ($20, excellent for pixel art)
- **Photoshop** (overkill for pixel art)

### Reference
- Use existing OSRS sprites as reference for style + proportions
- https://oldschool.runescape.wiki/ has sprite database
- Ask dev for sample sprites to use as template

---

## Sprite Templates

**Available in repo:** `assets/templates/` (check for pre-made templates)

These show:
- Exact dimensions
- Isometric angle
- Layer structure
- Export settings

Use as starting point for your own work.

---

## Testing Your Sprites

Once sprites are merged:
- Dev will integrate into game client
- You can test in-game by cloning main branch + running client
- Sprites will appear on NPCs, players, items, UI

### How to Test (if you're curious)
```bash
git checkout main
mvn clean install

cd client
mvn exec:java -Dexec.mainClass="com.osrs.client.Client"
```

Game window opens. Your sprites render in real-time.

---

## Collaboration Etiquette

### DO
- ✅ Post art frequently (even rough drafts)
- ✅ Ask questions in Discord
- ✅ Take feedback gracefully
- ✅ Update sprites if dev says "doesn't fit the style"
- ✅ Coordinate with other artists (don't duplicate work)

### DON'T
- ❌ Use copyrighted art (OSRS sprites, official art)
- ❌ Go dark for 2 weeks then dump 50 sprites
- ❌ Ignore technical specs (dimensions, format, etc.)
- ❌ Get offended by critique (it's about the game, not you)

---

## Sprint Schedule (Art Perspective)

| Sprint | What | Your Role |
|--------|------|-----------|
| **S1** (Weeks 1-4) | Foundation | None (dev building engine) |
| **S2** (Weeks 5-8) | Combat, XP | None (engine still being built) |
| **S3** (Weeks 9-14) | Polish | **START HERE** — Create sprites for Tutorial Island |
| **S4** (Weeks 15-24) | Release | Finalize art, replace placeholders |

**You join in S3.** Dev will have placeholder art by then; you replace it with quality custom sprites.

---

## Deliverables for S3-S4

| Asset | Qty | Notes |
|-------|-----|-------|
| **Tile sprites** | ~50 | Grass, water, sand, rock, buildings, etc. |
| **NPC sprites** | 15 | Tutorial Island NPCs (idle + walk animation) |
| **Player sprites** | 2 | Male + female (idle + walk animation) |
| **Item sprites** | 30 | Weapons, armor, food, quest items, etc. |
| **UI elements** | 20 | Buttons, icons, inventory slots, bars, etc. |

**Total:** ~130 sprites over 12 weeks (10-15 sprites/week)

---

## Questions?

Ask in Discord: `#parsundra`

Or check:
- **ARCHITECTURE.md** — System design (sprites section)
- **GitHub Issues** — Ask dev for sprite tasks

---

**Welcome to the team.** Your art makes this real. 🎨

---

**Next Steps:**
1. Clone repo + read this guide
2. Check `assets/templates/` for template files
3. Start sketching in S3 (when dev gives green light)
4. Post WIPs in Discord for feedback
5. Create PRs with your sprites

Let's build something beautiful.
