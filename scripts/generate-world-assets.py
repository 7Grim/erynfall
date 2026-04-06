#!/usr/bin/env python3
"""
generate-world-assets.py
Generates Pack 1–4 world overlay and variant sprites for Erynfall.

Pack 1: Water / Coastline  (shimmer, sparkle, shore foam, shore wet)
Pack 2: Terrain base variants  (grass×3, path×2, sand×2, wall×2)
Pack 3: Transition overlays  (edge_shore, edge_path_grass, edge_wall_base)
Pack 4: Ground clutter  (grass tufts, path pebbles, sand grains, reeds)

All tiles: 32×16, isometric diamond, transparent outside diamond.
Pivot: tile-diamond (centre of 32×16 canvas).

Coordinate notes
----------------
  Top vertex    (16,  0)
  Right vertex  (31,  8)
  Bottom vertex (16, 15)
  Left vertex   ( 0,  8)

Direction mapping (matches renderer waterNorth/E/S/W checks):
  N = NE diamond face  y∈[0,8]   adjacent tile = (x, y-1) = upper-right in screen
  E = SE diamond face  y∈[8,15]  adjacent tile = (x+1, y) = lower-right
  S = SW diamond face  y∈[8,15]  adjacent tile = (x, y+1) = lower-left
  W = NW diamond face  y∈[0,8]   adjacent tile = (x-1, y) = upper-left

Usage:  python3 scripts/generate-world-assets.py
Requires: Pillow
"""
from PIL import Image, ImageDraw
import os

OUT = os.path.join(os.path.dirname(__file__), "..", "art", "sprites")
os.makedirs(OUT, exist_ok=True)

# ── Palette ──────────────────────────────────────────────────────────────────
SHIMMER     = (168, 208, 228)   # pale silver-blue shimmer
SHIMMER_DIM = (138, 182, 208)   # shimmer halo
SPARKLE     = (232, 246, 255)   # near-white sparkle point
FOAM        = (204, 216, 224)   # off-white grey-blue foam
WET         = (36,  50,  68)    # dark cool wet-edge overlay

SHORE_TINT  = (52, 100, 158)    # water-adjacent land tint
GRASS_FRINGE= (52, 126, 34)     # grass creeping onto path edge
WALL_SHADOW = (12,  10,   8)    # wall base shadow

CLT_GRASS_A = (26,  80,  14)    # dark grass tuft
CLT_GRASS_B = (38, 102,  22)    # lighter grass tip
CLT_PATH    = (118,  86,  50)   # path pebble
CLT_SAND    = (188, 160,  78)   # sand grain
CLT_REED    = (44,   66,  28)   # reed stem

# ── Core helpers ─────────────────────────────────────────────────────────────
def new():
    return Image.new("RGBA", (32, 16), (0, 0, 0, 0))

def save(name, img):
    path = os.path.join(OUT, name)
    img.save(path)
    print(f"  {name}")

def row_x_range(y):
    """Left / right x (inclusive) for row y of the 32×16 isometric diamond."""
    if y <= 8:
        lx = max(0,  round(16 - 2 * y))
        rx = min(31, round(16 + 15 * y / 8))
    else:
        lx = max(0,  round(16 * (y - 8) / 7))
        rx = min(31, round(31 - 15 * (y - 8) / 7))
    return lx, rx

def in_diamond(x, y):
    if y < 0 or y > 15 or x < 0 or x > 31:
        return False
    lx, rx = row_x_range(y)
    return lx <= x <= rx

def set_px(pixels, x, y, col, alpha):
    """Paint pixel only if inside diamond and current alpha < new alpha."""
    if not in_diamond(x, y):
        return
    if pixels[x, y][3] < alpha:
        pixels[x, y] = (*col, alpha)

def make_tile(base, highlight=None, shade=None):
    img = new()
    d = ImageDraw.Draw(img)
    d.polygon([(16,0),(31,8),(16,15),(0,8)], fill=(*base, 255))
    if highlight:
        d.polygon([(16,0),(31,8),(16,8),(0,8)], fill=(*highlight, 220))
    if shade:
        d.polygon([(16,8),(31,8),(16,15),(0,8)], fill=(*shade, 200))
    return img

# ── Edge pixel iterators ──────────────────────────────────────────────────────
def edge_pixels(direction):
    """
    Yields (edge_x, y, inward_dx) for each pixel along the specified face.
    inward_dx is +1 (right) or -1 (left) — direction towards diamond interior.
    """
    if direction == 'n':           # NE face:  (16,0) → (31,8)
        for y in range(0, 9):
            x = min(31, round(16 + 15 * y / 8))
            yield x, y, -1
    elif direction == 'e':         # SE face:  (31,8) → (16,15)
        for y in range(8, 16):
            x = min(31, round(31 - 15 * (y - 8) / 7))
            yield x, y, -1
    elif direction == 's':         # SW face:  (0,8)  → (16,15)
        for y in range(8, 16):
            x = max(0, round(16 * (y - 8) / 7))
            yield x, y, +1
    elif direction == 'w':         # NW face:  (16,0) → (0,8)
        for y in range(0, 9):
            x = max(0, round(16 - 2 * y))
            yield x, y, +1

# ═══════════════════════════════════════════════════════════════════════════════
# PACK 1 — Water / Coastline
# ═══════════════════════════════════════════════════════════════════════════════

# Shimmer blob centres — spread evenly inside the diamond.
_SHIMMER_SEEDS = [
    (7,  4), (14,  3), (21,  4), (27,  5),
    (9,  7), (17,  6), (24,  8), (29,  7),
    (8, 10), (16, 10), (23, 10),
    (11, 13), (20, 12),
]

def make_water_shimmer(frame):
    """
    4-frame water shimmer overlay.  Each frame shifts blob positions ≤2 px
    in a cyclic pattern and deactivates a rotating 1-in-5 subset.
    """
    img = new()
    pixels = img.load()
    shifts = [(0, 0), (1, 1), (2, 0), (1, -1)]
    ox, oy = shifts[frame]
    for i, (bx, by) in enumerate(_SHIMMER_SEEDS):
        if i % 5 == frame:         # ~20% of blobs off each frame (rotates)
            continue
        px, py = bx + ox, by + oy
        if not in_diamond(px, py):
            continue
        set_px(pixels, px, py, SHIMMER, 60)
        for dx, dy in [(1,0),(-1,0),(0,1),(0,-1)]:
            set_px(pixels, px+dx, py+dy, SHIMMER_DIM, 24)
    return img

_SPARKLE_FRAMES = [
    [(10, 4), (23, 7), (16, 12), (7,  9), (27, 5)],
    [(19, 3), ( 8, 8), (25, 11), (13, 13), (29, 7)],
]

def make_water_sparkle(frame):
    """2-frame sparse sparkle points — occasional sunlight catch."""
    img = new()
    pixels = img.load()
    for (px, py) in _SPARKLE_FRAMES[frame]:
        if not in_diamond(px, py):
            continue
        set_px(pixels, px, py, SPARKLE, 215)
        for dx, dy in [(1,0),(-1,0),(0,1),(0,-1)]:
            set_px(pixels, px+dx, py+dy, SPARKLE, 80)
    return img

def make_shore_foam(direction):
    """
    Broken 1-2 px foam accent along one diamond face.
    ~20% pixels skipped for a natural, non-solid look.
    """
    img = new()
    pixels = img.load()
    skip_offset = {'n': 0, 'e': 1, 's': 2, 'w': 3}[direction]
    for i, (ex, ey, inward) in enumerate(edge_pixels(direction)):
        if (i + skip_offset) % 5 == 0:    # broken gap
            continue
        set_px(pixels, ex,          ey, FOAM, 178)   # edge pixel
        set_px(pixels, ex + inward, ey, FOAM,  88)   # inner pixel
    return img

def make_shore_wet(direction):
    """
    Continuous 3 px darkening band — damp ground where water meets land.
    Alpha fades from 130 at edge to 38 two pixels in.
    """
    img = new()
    pixels = img.load()
    for (ex, ey, inward) in edge_pixels(direction):
        set_px(pixels, ex,              ey, WET, 130)
        set_px(pixels, ex + inward,     ey, WET,  78)
        set_px(pixels, ex + inward * 2, ey, WET,  36)
    return img

# ═══════════════════════════════════════════════════════════════════════════════
# PACK 2 — Terrain base variants
# ═══════════════════════════════════════════════════════════════════════════════

def _grass_variant(idx):
    palettes = [
        # base,             highlight,        shade,            dot
        ((60,150, 36), (82,178, 56), (46,122, 26), (26, 80, 14)),
        ((55,142, 42), (76,168, 62), (42,114, 32), (22, 72, 18)),
        ((52,135, 34), (72,160, 52), (40,108, 24), (20, 66, 12)),
    ]
    return palettes[idx]

_GRASS_DOTS = [
    [(8,5),(19,4),(25,8),(11,11),(22,12)],
    [(6,6),(15,3),(23,6),( 9,10),(20,13)],
    [(12,4),(21,5),( 7,8),(17,11),(26, 9)],
]

def _path_variant(idx):
    palettes = [
        ((150,110, 60), (172,132, 78), (120, 88, 44), (105, 70, 34)),
        ((144,104, 54), (165,124, 70), (114, 82, 38), ( 98, 64, 28)),
    ]
    return palettes[idx]

_PATH_DOTS = [
    [(10,5),(22,7),( 7,10),(25, 9)],
    [(14,4),(20,8),( 9,11),(27, 6)],
]

def _sand_variant(idx):
    palettes = [
        ((210,185,112), (230,208,140), (180,158, 86), (165,138, 62)),
        ((205,178,106), (225,200,132), (175,152, 80), (158,132, 56)),
    ]
    return palettes[idx]

_SAND_DOTS = [
    [( 9,6),(18,4),(24,8),(12,12)],
    [( 7,7),(21,5),(15,11),(27, 9)],
]

def _wall_variant(idx):
    palettes = [
        ((130,130,128), (168,168,166), ( 97, 97, 96), (70, 68, 66)),
        ((126,128,132), (162,164,168), ( 94, 96,100), (66, 66, 72)),
    ]
    return palettes[idx]

_WALL_DOTS = [
    [(10,4),(20,6),( 8, 9),(23,10)],
    [(13,5),(25,7),( 7,11),(18, 9)],
]

def make_tile_variant(kind, idx):
    dispatch = {
        'grass': (_grass_variant, _GRASS_DOTS),
        'path':  (_path_variant,  _PATH_DOTS),
        'sand':  (_sand_variant,  _SAND_DOTS),
        'wall':  (_wall_variant,  _WALL_DOTS),
    }
    pal_fn, dot_sets = dispatch[kind]
    base, hi, sh, dot_col = pal_fn(idx)
    img = make_tile(base, hi, sh)
    pixels = img.load()
    for (x, y) in dot_sets[idx]:
        if in_diamond(x, y):
            pixels[x, y] = (*dot_col, 200)
    return img

# ═══════════════════════════════════════════════════════════════════════════════
# PACK 3 — Transition overlays
# ═══════════════════════════════════════════════════════════════════════════════

def make_edge_shore(direction):
    """
    Semi-transparent water-blue tint along shoreline edge — land tiles only.
    Stacks beneath shore_foam / shore_wet for depth.
    """
    img = new()
    pixels = img.load()
    for (ex, ey, inward) in edge_pixels(direction):
        set_px(pixels, ex,              ey, SHORE_TINT, 80)
        set_px(pixels, ex + inward,     ey, SHORE_TINT, 42)
        set_px(pixels, ex + inward * 2, ey, SHORE_TINT, 16)
    return img

def make_edge_path_grass(direction):
    """
    Grass fringe bleeding onto path edge — softens the hard path/grass border.
    Drawn on path tiles adjacent to grass.
    """
    img = new()
    pixels = img.load()
    for (ex, ey, inward) in edge_pixels(direction):
        set_px(pixels, ex,          ey, GRASS_FRINGE, 72)
        set_px(pixels, ex + inward, ey, GRASS_FRINGE, 34)
    return img

def make_edge_wall_base():
    """
    Gradient shadow across the bottom half of the wall diamond.
    Gives the flat wall tile the illusion of standing height.
    """
    img = new()
    pixels = img.load()
    for y in range(8, 16):
        lx, rx = row_x_range(y)
        alpha = int(115 * (y - 8) / 7)
        for x in range(lx, rx + 1):
            set_px(pixels, x, y, WALL_SHADOW, alpha)
    return img

# ═══════════════════════════════════════════════════════════════════════════════
# PACK 4 — Ground clutter
# ═══════════════════════════════════════════════════════════════════════════════

_GRASS_TUFTS = [
    # variant 1 — two tufts
    [[(9,6),(9,7)],           [(19,8),(19,9)]],
    # variant 2 — three tufts
    [[(7,5),(7,6)],           [(16,10),(16,11)],  [(23,7),(23,8)]],
    # variant 3 — two wider-spaced tufts
    [[(11,9),(11,10)],        [(25,6),(25,7)]],
]

def make_clutter_grass(variant):
    img = new()
    pixels = img.load()
    for stem in _GRASS_TUFTS[variant - 1]:
        for (x, y) in stem:
            if in_diamond(x, y):
                pixels[x, y] = (*CLT_GRASS_A, 200)
        # Lighter tip one pixel above base of stem
        if stem:
            tx, ty = stem[0]
            if ty > 0 and in_diamond(tx, ty - 1):
                pixels[tx, ty - 1] = (*CLT_GRASS_B, 150)
    return img

_PATH_PEBBLES = [
    [(10,6), (18,8), (24,10)],
    [( 8,7), (15,5), (22,9), (27,7)],
]

def make_clutter_path(variant):
    img = new()
    pixels = img.load()
    for (x, y) in _PATH_PEBBLES[variant - 1]:
        if in_diamond(x, y):
            pixels[x, y] = (*CLT_PATH, 190)
            # Small adjacent pixel for larger pebble (deterministic)
            if (x + y) % 2 == 0 and in_diamond(x + 1, y):
                pixels[x + 1, y] = (*CLT_PATH, 108)
    return img

_SAND_GRAINS = [
    [( 9,5), (17,7), (23,9), (12,12)],
    [( 7,8), (14,6), (21,11), (26,8)],
]

def make_clutter_sand(variant):
    img = new()
    pixels = img.load()
    for (x, y) in _SAND_GRAINS[variant - 1]:
        if in_diamond(x, y):
            pixels[x, y] = (*CLT_SAND, 182)
    return img

def make_clutter_reeds():
    """Two sparse 3-px vertical reed stems near the diamond edge."""
    img = new()
    pixels = img.load()
    stems = [
        [(8, 8), (8, 7), (8, 6)],
        [(22, 8), (22, 7), (22, 6)],
    ]
    for stem in stems:
        for i, (x, y) in enumerate(stem):
            if in_diamond(x, y):
                alpha = 145 if i == 0 else 200
                pixels[x, y] = (*CLT_REED, alpha)
        if stem:
            tx, ty = stem[-1]
            if in_diamond(tx + 1, ty - 1):
                pixels[tx + 1, ty - 1] = (*CLT_REED, 118)
    return img

# ═══════════════════════════════════════════════════════════════════════════════
# Main
# ═══════════════════════════════════════════════════════════════════════════════
total = 0

print("Pack 1 — Water / Coastline")
for f in range(4):
    save(f"water_shimmer_{f}.png", make_water_shimmer(f)); total += 1
for f in range(2):
    save(f"water_sparkle_{f}.png", make_water_sparkle(f)); total += 1
for d in ['n','e','s','w']:
    save(f"shore_foam_{d}.png", make_shore_foam(d)); total += 1
for d in ['n','e','s','w']:
    save(f"shore_wet_{d}.png",  make_shore_wet(d));  total += 1

print()
print("Pack 2 — Terrain base variants")
for i in range(3):
    save(f"tile_grass_{i}.png", make_tile_variant('grass', i)); total += 1
for i in range(2):
    save(f"tile_path_{i}.png",  make_tile_variant('path',  i)); total += 1
for i in range(2):
    save(f"tile_sand_{i}.png",  make_tile_variant('sand',  i)); total += 1
for i in range(2):
    save(f"tile_wall_{i}.png",  make_tile_variant('wall',  i)); total += 1

print()
print("Pack 3 — Transition overlays")
for d in ['n','e','s','w']:
    save(f"edge_shore_{d}.png",       make_edge_shore(d));      total += 1
for d in ['n','e','s','w']:
    save(f"edge_path_grass_{d}.png",  make_edge_path_grass(d)); total += 1
save("edge_wall_base.png", make_edge_wall_base()); total += 1

print()
print("Pack 4 — Ground clutter")
for v in range(1, 4):
    save(f"clutter_grass_{v}.png", make_clutter_grass(v)); total += 1
for v in range(1, 3):
    save(f"clutter_path_{v}.png",  make_clutter_path(v));  total += 1
for v in range(1, 3):
    save(f"clutter_sand_{v}.png",  make_clutter_sand(v));  total += 1
save("clutter_reeds_1.png", make_clutter_reeds()); total += 1

print()
print(f"Done — {total} assets written to art/sprites/")
print("Next: mvn generate-resources -pl client -am  (runs TexturePacker)")
