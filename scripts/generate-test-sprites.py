#!/usr/bin/env python3
"""
generate-test-sprites.py
Generates flat-colour test PNGs for every sprite slot in art/sprites/.
These are NOT final art — just placeholder colours to verify the pipeline works.
Replace each file with real art when the artist delivers it.

Usage:
    python3 scripts/generate-test-sprites.py

Requires: Pillow  (pip install Pillow)
"""

from PIL import Image, ImageDraw
import os

OUT = os.path.join(os.path.dirname(__file__), "..", "art", "sprites")
os.makedirs(OUT, exist_ok=True)

def save(name, img):
    path = os.path.join(OUT, name)
    img.save(path)
    print(f"  {name}  ({img.width}x{img.height})")

# ---------------------------------------------------------------------------
# Isometric diamond tile — 32x16, transparent corners
# Vertices: top(16,0) right(31,8) bottom(16,15) left(0,8)
# ---------------------------------------------------------------------------
def make_tile(color_rgb):
    img = Image.new("RGBA", (32, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    diamond = [(16, 0), (31, 8), (16, 15), (0, 8)]
    draw.polygon(diamond, fill=(*color_rgb, 255))
    return img

# ---------------------------------------------------------------------------
# Entity sprite — given size, flat colour, bottom-center anchor implied
# ---------------------------------------------------------------------------
def make_entity(w, h, color_rgb):
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    draw.rectangle([1, 1, w - 2, h - 2], fill=(*color_rgb, 255))
    return img

# ---------------------------------------------------------------------------
# Simple humanoid — head + body + legs in the right proportions
# ---------------------------------------------------------------------------
def make_humanoid(w, h, shirt_color, skin=(220, 180, 130)):
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    cx = w // 2
    leg_h = h // 3
    draw.rectangle([cx - 4, h - leg_h, cx - 2, h - 1], fill=(80, 50, 20, 255))
    draw.rectangle([cx + 1, h - leg_h, cx + 3, h - 1], fill=(80, 50, 20, 255))
    body_y = h - leg_h - (h // 3)
    draw.rectangle([cx - 4, body_y, cx + 3, h - leg_h - 1], fill=(*shirt_color, 255))
    head_h = h // 4
    head_y = body_y - head_h
    draw.rectangle([cx - 3, head_y, cx + 2, body_y - 1], fill=(*skin, 255))
    return img

# ---------------------------------------------------------------------------
# Simple tree silhouette
# ---------------------------------------------------------------------------
def make_tree(w, h, trunk_color, canopy_color):
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    cx = w // 2
    trunk_h = h // 4
    draw.rectangle([cx - 2, h - trunk_h, cx + 1, h - 1], fill=(*trunk_color, 255))
    canopy = [(cx, 0), (w - 2, h - trunk_h), (1, h - trunk_h)]
    draw.polygon(canopy, fill=(*canopy_color, 255))
    return img

# ---------------------------------------------------------------------------
# Rock with coloured ore vein
# ---------------------------------------------------------------------------
def make_rock(ore_color):
    w, h = 16, 12
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    # Grey boulder base
    draw.rectangle([1, 2, w - 2, h - 1], fill=(95, 90, 85, 255))
    # Lighter highlight top-left
    draw.rectangle([1, 2, w - 6, 4], fill=(135, 130, 125, 255))
    # Ore vein stripe
    draw.rectangle([3, 5, w - 4, 7], fill=(*ore_color, 255))
    # Shadow bottom
    draw.rectangle([2, h - 3, w - 3, h - 2], fill=(50, 46, 42, 255))
    return img

# ---------------------------------------------------------------------------
# Generate everything
# ---------------------------------------------------------------------------

print("Generating test sprites → art/sprites/")
print()

print("Ground tiles (32×16):")
save("tile_grass.png",  make_tile((60, 150, 40)))
save("tile_water.png",  make_tile((30, 110, 200)))
save("tile_path.png",   make_tile((150, 110, 60)))
save("tile_wall.png",   make_tile((130, 130, 130)))
save("tile_sand.png",   make_tile((210, 185, 110)))

print()
print("Player + humanoid NPCs (16×24):")
save("player.png",         make_humanoid(16, 24, (50, 100, 200)))         # blue shirt
save("npc_guide.png",      make_humanoid(16, 24, (50, 150, 60)))          # green robe
save("npc_instructor.png", make_humanoid(16, 24, (180, 40, 40)))          # red armour
save("npc_banker.png",     make_humanoid(16, 24, (36, 77, 184), skin=(220, 180, 130)))  # blue waistcoat
save("npc_goblin.png",     make_humanoid(16, 24, (80, 140, 60), skin=(100, 160, 80)))

print()
print("Small creatures (16×12):")
save("npc_rat.png",     make_entity(16, 12, (130, 110, 90)))
save("npc_chicken.png", make_entity(16, 12, (240, 230, 210)))

print()
print("Large creatures (24×20):")
save("npc_giant_rat.png", make_entity(24, 20, (100, 80, 60)))
save("npc_cow.png",       make_entity(24, 20, (230, 220, 200)))

print()
print("Trees (16×32 / 16×40):")
save("tree.png",          make_tree(16, 32, (100, 65, 30),  (40, 130, 40)))
save("tree_oak.png",      make_tree(16, 32, (90, 55, 20),   (55, 120, 35)))
save("tree_willow.png",   make_tree(16, 40, (80, 50, 15),   (60, 140, 50)))
save("tree_teak.png",     make_tree(16, 32, (140, 76, 26),  (55, 132, 50)))   # warm orange-brown trunk
save("tree_maple.png",    make_tree(16, 36, (95, 60, 20),   (180, 80, 30)))   # autumn orange
save("tree_mahogany.png", make_tree(16, 36, (97, 36, 20),   (40, 102, 36)))   # dark red-brown trunk
save("tree_yew.png",      make_tree(16, 40, (70, 45, 15),   (30, 100, 30)))
save("tree_magic.png",    make_tree(16, 40, (60, 40, 100),  (80, 40, 200)))   # purple/magic

print()
print("Mining rocks (16×12, with ore vein colour):")
save("rock_copper.png",     make_rock((204, 102, 38)))    # copper orange
save("rock_tin.png",        make_rock((191, 199, 204)))   # silver-grey
save("rock_iron.png",       make_rock((140, 77, 46)))     # rust brown
save("rock_silver.png",     make_rock((224, 224, 235)))   # bright silver
save("rock_coal.png",       make_rock((38, 38, 41)))      # near-black
save("rock_gold.png",       make_rock((242, 204, 26)))    # bright gold
save("rock_mithril.png",    make_rock((64, 107, 209)))    # blue
save("rock_adamantite.png", make_rock((31, 158, 77)))     # green
save("rock_runite.png",     make_rock((31, 184, 184)))    # teal

print()
print("Other objects:")
save("fishing_spot.png", make_entity(16, 16, (80, 160, 220)))
save("fire.png",         make_entity(16, 20, (220, 100, 30)))

print()
print("Done. Now run:")
print("  mvn generate-resources -pl client -am")
print("  GAME_SERVER_HOST=165.22.37.200 ./run-client.sh")
