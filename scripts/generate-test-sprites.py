#!/usr/bin/env python3
"""
generate-test-sprites.py
Generates recognisable test sprites for every slot in art/sprites/.
NOT final art — replace each file with real artist work when ready.

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
    print(f"  {name}  ({img.width}×{img.height})")

def new(w, h):
    return Image.new("RGBA", (w, h), (0, 0, 0, 0))

# ---------------------------------------------------------------------------
# Isometric diamond tile — 32×16, transparent corners
# ---------------------------------------------------------------------------
def make_tile(base, highlight=None, shade=None):
    """Diamond tile with optional lighter highlight strip and darker shade."""
    img = new(32, 16)
    d = ImageDraw.Draw(img)
    d.polygon([(16,0),(31,8),(16,15),(0,8)], fill=(*base, 255))
    if highlight:
        # Top-left quadrant gets a slightly lighter strip for faked top-light
        d.polygon([(16,0),(31,8),(16,8),(0,8)], fill=(*highlight, 220))
    if shade:
        # Bottom half is subtly darker
        d.polygon([(16,8),(31,8),(16,15),(0,8)], fill=(*shade, 200))
    return img

# ---------------------------------------------------------------------------
# Humanoid — side-view, 16×24, bottom-center anchor
# ---------------------------------------------------------------------------
def make_humanoid(shirt, trousers=(80,50,20), skin=(220,180,130), hair=(60,38,14),
                  accessory=None, acc_color=None):
    img = new(16, 24)
    d = ImageDraw.Draw(img)
    cx = 8
    # Legs
    d.rectangle([cx-4, 16, cx-2, 23], fill=(*trousers, 255))
    d.rectangle([cx+1, 16, cx+3, 23], fill=(*trousers, 255))
    # Boot/feet
    d.rectangle([cx-5, 21, cx-1, 23], fill=(50,35,15,255))
    d.rectangle([cx+0, 21, cx+4, 23], fill=(50,35,15,255))
    # Body
    d.rectangle([cx-5, 9, cx+4, 16], fill=(*shirt, 255))
    # Arms
    d.rectangle([cx-7, 10, cx-5, 17], fill=(*skin, 255))
    d.rectangle([cx+4, 10, cx+6, 17], fill=(*skin, 255))
    # Neck
    d.rectangle([cx-1, 7, cx+1, 10], fill=(*skin, 255))
    # Head
    d.rectangle([cx-3, 2, cx+3, 9], fill=(*skin, 255))
    # Eyes
    d.rectangle([cx-2, 4, cx-1, 5], fill=(30,20,10,255))
    d.rectangle([cx+1, 4, cx+2, 5], fill=(30,20,10,255))
    # Hair
    d.rectangle([cx-3, 1, cx+3, 3], fill=(*hair, 255))
    if accessory and acc_color:
        d.rectangle(accessory, fill=(*acc_color, 255))
    return img

# ---------------------------------------------------------------------------
# Rat — side view facing right, 16×12, bottom-center anchor
# ---------------------------------------------------------------------------
def make_rat():
    img = new(16, 12)
    d = ImageDraw.Draw(img)
    body    = (132,110,88)
    dark    = (100, 82,66)
    pink    = (220,155,155)
    eye_col = (15, 10,10)

    # Tail (drawn first so body sits on top)
    d.line([(0,9),(0,11),(2,11)], fill=(190,160,135), width=1)
    # Body — elongated oval
    d.ellipse([1,4,11,10], fill=body)
    # Dark belly
    d.ellipse([2,6,9,10], fill=dark)
    # Head
    d.ellipse([8,2,15,9], fill=(148,124,100))
    # Snout
    d.rectangle([13,5,15,7], fill=(160,130,106))
    d.point((15,6), fill=(80,50,50))       # nostril
    # Ear
    d.ellipse([9,0,13,4], fill=pink)
    d.ellipse([10,1,12,3], fill=(240,190,190))  # inner ear
    # Eye
    d.rectangle([11,3,12,4], fill=eye_col)
    d.point((12,3), fill=(220,220,220))    # eye highlight
    # Legs
    d.rectangle([3,9,4,11], fill=dark)
    d.rectangle([7,9,8,11], fill=dark)
    return img

# ---------------------------------------------------------------------------
# Giant Rat — bigger, darker, 24×20
# ---------------------------------------------------------------------------
def make_giant_rat():
    img = new(24, 20)
    d = ImageDraw.Draw(img)
    body = (100,78,56)
    dark = ( 72,56,40)
    pink = (200,140,140)
    red  = (180,20,20)

    # Tail
    d.line([(0,15),(0,18),(3,19)], fill=(165,130,105), width=2)
    # Body
    d.ellipse([1,7,17,17], fill=body)
    # Darker back
    d.ellipse([1,7,9,14], fill=dark)
    # Head
    d.ellipse([13,4,23,15], fill=(115,90,66))
    # Snout
    d.rectangle([21,8,23,12], fill=(130,100,75))
    d.point((23,10), fill=(70,40,40))
    # Ear — large
    d.ellipse([14,0,20,7], fill=pink)
    d.ellipse([15,1,19,6], fill=(230,175,175))
    # Eyes — red, menacing
    d.rectangle([17,5,19,7], fill=red)
    d.point((18,5), fill=(255,180,180))
    # Legs
    d.rectangle([4,16,6,19], fill=dark)
    d.rectangle([9,16,11,19], fill=dark)
    return img

# ---------------------------------------------------------------------------
# Chicken — round, 16×12
# ---------------------------------------------------------------------------
def make_chicken():
    img = new(16, 12)
    d = ImageDraw.Draw(img)
    white  = (242,238,225)
    wing   = (200,195,180)
    orange = (220,130,30)
    red    = (200,30,30)
    eye    = (15,10,10)

    # Legs
    d.rectangle([5,9,6,11], fill=orange)
    d.rectangle([9,9,10,11], fill=orange)
    # Feet
    d.rectangle([3,11,7,11], fill=orange)
    d.rectangle([8,11,12,11], fill=orange)
    # Body
    d.ellipse([2,3,13,11], fill=white)
    # Wing accent
    d.ellipse([3,4,10,10], fill=wing)
    # Head
    d.ellipse([10,0,15,7], fill=white)
    # Comb (3 bumps)
    d.rectangle([11,0,12,2], fill=red)
    d.rectangle([12,0,13,1], fill=red)
    d.rectangle([13,0,14,2], fill=red)
    # Wattle (under beak)
    d.ellipse([13,4,15,7], fill=red)
    # Beak
    d.polygon([(14,3),(15,4),(14,5)], fill=orange)
    # Eye
    d.rectangle([11,2,12,3], fill=eye)
    d.point((12,2), fill=(230,230,230))
    return img

# ---------------------------------------------------------------------------
# Cow — large, 24×20, black-and-white patches
# ---------------------------------------------------------------------------
def make_cow():
    img = new(24, 20)
    d = ImageDraw.Draw(img)
    white  = (232,228,218)
    black  = ( 30, 28, 28)
    pink   = (220,170,160)
    ivory  = (200,190,150)
    eye    = ( 20, 15, 10)

    # Legs — thick, black
    d.rectangle([ 3,14, 6,19], fill=black)
    d.rectangle([ 8,14,11,19], fill=black)
    d.rectangle([14,14,17,19], fill=black)
    d.rectangle([19,14,22,19], fill=black)
    # Body
    d.ellipse([2,5,21,16], fill=white)
    # Black patches (Holstein markings)
    d.ellipse([2, 5, 9,11], fill=black)
    d.ellipse([13,9,20,15], fill=black)
    d.ellipse([6,10,12,15], fill=black)
    # Udder
    d.ellipse([8,14,15,17], fill=pink)
    # Head
    d.ellipse([18,3,24,13], fill=white)
    # Black patch on head
    d.ellipse([18,3,22,7], fill=black)
    # Nose
    d.ellipse([20,9,24,13], fill=pink)
    d.point((21,11), fill=black)
    d.point((23,11), fill=black)
    # Eye
    d.rectangle([19,5,20,6], fill=eye)
    d.point((20,5), fill=(230,230,230))
    # Horns
    d.line([(19,3),(17,0)], fill=ivory, width=1)
    d.line([(22,3),(23,0)], fill=ivory, width=1)
    # Ear
    d.ellipse([16,5,19,9], fill=pink)
    # Tail
    d.line([(2,12),(0,15)], fill=(180,170,150), width=1)
    d.ellipse([0,15,2,17], fill=(120,110,90))
    return img

# ---------------------------------------------------------------------------
# Goblin — hunched green creature, 16×24
# ---------------------------------------------------------------------------
def make_goblin():
    img = new(16, 24)
    d = ImageDraw.Draw(img)
    skin    = (100,160,80)
    dark    = ( 68,110,54)
    leather = ( 96, 60,22)
    eye_col = (220,200,10)
    weapon  = (150,150,155)

    # Legs (shorter, squatter than humanoid)
    d.rectangle([3,17,5,23], fill=leather)
    d.rectangle([9,17,11,23], fill=leather)
    # Crude leather armour body
    d.rectangle([3,10,12,18], fill=leather)
    # Green skin showing through
    d.rectangle([5,12,10,16], fill=skin)
    # Crude helmet
    d.rectangle([3, 6,12,10], fill=leather)
    # Head
    d.rectangle([4, 7,11,13], fill=skin)
    # Big ears
    d.ellipse([0, 8, 5,13], fill=dark)
    d.ellipse([10,8,15,13], fill=dark)
    d.ellipse([1, 9, 4,12], fill=(130,190,105))
    d.ellipse([11,9,14,12], fill=(130,190,105))
    # Yellow eyes
    pupil = (15, 10, 10)
    d.rectangle([5, 9, 6,10], fill=eye_col)
    d.rectangle([8, 9, 9,10], fill=eye_col)
    d.point((5,9), fill=pupil)
    d.point((8,9), fill=pupil)
    # Weapon (club, right side)
    d.rectangle([12,8,13,20], fill=weapon)
    d.rectangle([11,7,14,10], fill=weapon)
    return img

# ---------------------------------------------------------------------------
# Tree helpers
# ---------------------------------------------------------------------------
def make_tree(w, h, trunk, canopy, highlight=None):
    img = new(w, h)
    d = ImageDraw.Draw(img)
    cx = w // 2
    th = h // 4  # trunk height
    # Trunk with slight darker left edge for depth
    d.rectangle([cx-3, h-th, cx+2, h-1], fill=(*trunk,255))
    d.rectangle([cx-3, h-th, cx-2, h-1], fill=(max(0,trunk[0]-20), max(0,trunk[1]-20), max(0,trunk[2]-20),255))
    # Canopy layers (wide base, tapering to top)
    d.polygon([(cx,0),(w-2,h-th),(1,h-th)], fill=(*canopy,255))
    if highlight:
        # Upper canopy lighter highlight
        mid = (h-th)//2
        d.polygon([(cx,0),(w//2+4,mid),(w//2-4,mid)], fill=(*highlight,200))
    return img

# ---------------------------------------------------------------------------
# Rock with ore vein
# ---------------------------------------------------------------------------
def make_rock(ore):
    img = new(16, 12)
    d = ImageDraw.Draw(img)
    d.polygon([(8,0),(15,5),(12,11),(3,11),(0,5)], fill=(98,93,88,255))
    # Highlight (top-left face catches light)
    d.polygon([(8,0),(15,5),(8,5),(0,5)], fill=(135,128,122,255))
    # Shadow (bottom)
    d.polygon([(3,11),(12,11),(12,9),(3,9)], fill=(55,50,46,255))
    # Ore vein
    d.ellipse([4,4,11,8], fill=(*ore,255))
    d.ellipse([5,5,10,7], fill=(min(255,ore[0]+40),min(255,ore[1]+40),min(255,ore[2]+40),255))
    return img

# ---------------------------------------------------------------------------
# Fire — 16×20, bottom-center anchor
# ---------------------------------------------------------------------------
def make_fire():
    img = new(16, 20)
    d = ImageDraw.Draw(img)
    # Log base
    d.rectangle([2,16,13,19], fill=(88,50,18,255))
    d.rectangle([0,14,15,16], fill=(68,38,12,255))
    # Outer flame (orange)
    d.polygon([(8,0),(14,10),(12,18),(4,18),(2,10)], fill=(220,95,20,255))
    # Mid flame (yellow)
    d.polygon([(8,2),(13,10),(11,16),(5,16),(3,10)], fill=(240,185,30,255))
    # Core flame (bright yellow-white)
    d.polygon([(8,5),(11,11),(8,15),(5,11)], fill=(255,240,160,255))
    return img

# ---------------------------------------------------------------------------
# Generate everything
# ---------------------------------------------------------------------------
print("Generating test sprites → art/sprites/")
print()

print("Ground tiles (32×16):")
save("tile_grass.png",  make_tile((58,148,38),   highlight=(80,175,58),  shade=(44,118,28)))
save("tile_water.png",  make_tile((28,105,198),  highlight=(45,135,225), shade=(18, 80,160)))
save("tile_path.png",   make_tile((148,108,58),  highlight=(170,128,75), shade=(118, 85,42)))
save("tile_wall.png",   make_tile((128,128,128), highlight=(165,165,165),shade=( 95, 95, 95)))
save("tile_sand.png",   make_tile((208,182,108), highlight=(228,205,135),shade=(178,155, 82)))

print()
print("Player + humanoid NPCs (16×24):")
save("player.png",         make_humanoid(shirt=(48,98,195), trousers=(58,38,18)))
save("npc_guide.png",      make_humanoid(shirt=(45,145,55), trousers=(55,35,15),
                                         hair=(88,55,20),
                                         accessory=[4,0,11,2], acc_color=(88,55,20)))  # hood
save("npc_instructor.png", make_humanoid(shirt=(175,35,35), trousers=(88,88,92),
                                         hair=(0,0,0),
                                         accessory=[4,0,11,4], acc_color=(155,155,160)))  # helmet
save("npc_banker.png",     make_humanoid(shirt=(35,75,180), trousers=(48,42,72),
                                         hair=(88,62,28),
                                         accessory=[5,8,9,10], acc_color=(200,165,25)))   # waistcoat gold trim
save("npc_goblin.png",     make_goblin())

print()
print("Small creatures (16×12):")
save("npc_rat.png",     make_rat())
save("npc_chicken.png", make_chicken())

print()
print("Large creatures (24×20):")
save("npc_giant_rat.png", make_giant_rat())
save("npc_cow.png",       make_cow())

print()
print("Trees:")
save("tree.png",          make_tree(16,32, (102,66,30),  (38,128,38),  highlight=(62,158,52)))
save("tree_oak.png",      make_tree(16,32, (88,55,22),   (52,118,34),  highlight=(72,145,50)))
save("tree_willow.png",   make_tree(16,40, (78,50,14),   (58,138,48),  highlight=(82,162,65)))
save("tree_teak.png",     make_tree(16,32, (138,74,25),  (52,128,48),  highlight=(72,155,68)))
save("tree_maple.png",    make_tree(16,36, (92,58,18),   (178,78,28),  highlight=(210,105,45)))
save("tree_mahogany.png", make_tree(16,36, (95,34,18),   (38,98,34),   highlight=(52,125,48)))
save("tree_yew.png",      make_tree(16,40, (68,44,14),   (28,98,28),   highlight=(42,118,42)))
save("tree_magic.png",    make_tree(16,40, (58,38,98),   (78,38,195),  highlight=(115,75,230)))

print()
print("Mining rocks (16×12):")
save("rock_copper.png",     make_rock((205,100,38)))
save("rock_tin.png",        make_rock((188,198,205)))
save("rock_iron.png",       make_rock((138,75,45)))
save("rock_silver.png",     make_rock((222,222,232)))
save("rock_coal.png",       make_rock(( 35, 35, 40)))
save("rock_gold.png",       make_rock((240,202,25)))
save("rock_mithril.png",    make_rock(( 62,105,210)))
save("rock_adamantite.png", make_rock(( 28,155,75)))
save("rock_runite.png",     make_rock(( 28,180,182)))

print()
print("Other objects:")
save("fishing_spot.png", make_tile((28,105,198), highlight=(45,135,225), shade=(18,80,160)))
save("fire.png",         make_fire())

print()
print("Done. Now run:")
print("  mvn generate-resources -pl client -am")
print("  GAME_SERVER_HOST=165.22.37.200 ./run-client.sh")
