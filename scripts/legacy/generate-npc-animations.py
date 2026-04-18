#!/usr/bin/env python3
"""
generate-npc-animations.py  —  Pack 6: remaining NPC animation frames

Humanoid NPCs (16×24, bottom-center):
  npc_guide        idle_0/1, walk_{n,s,e,w}_0..3  → 18 files
  npc_instructor   idle_0/1, walk_{n,s,e,w}_0..3  → 18 files
  npc_goblin       idle_0/1, walk_{n,s,e,w}_0..3  → 18 files

Animal NPCs:
  npc_rat      (16×12)  idle_0/1, walk_{n,s,e,w}_0..3  → 18 files
  npc_giant_rat(24×20)  idle_0/1, walk_{n,s,e,w}_0..3  → 18 files
  npc_chicken  (16×12)  idle_0/1, walk_{n,s,e,w}_0..3  → 18 files
  npc_cow      (24×20)  idle_0/1, walk_{n,s,e,w}_0..3  → 18 files

Total: 126 files

Humanoid layout (identical to Pack 5):
  boots fixed y=21-22, legs y=15-20, belt y=14, shirt y=8-13, neck y=7,
  head y=1-6. Upper body bobs +1 on stride frames 1 and 3.

Goblin variant: head at y=4 (squat), 4-row head, 5-row cloth torso.
  Boots still fixed at y=21-22 — same leg functions reused.

Animal west walk = img.transpose(FLIP_LEFT_RIGHT) of east walk.
  Guaranteed symmetry, no duplicate drawing code.

Usage:  python3 scripts/generate-npc-animations.py
Requires: Pillow
"""
from PIL import Image
import os

OUT = os.path.join(os.path.dirname(__file__), "..", "art", "sprites")
os.makedirs(OUT, exist_ok=True)

# ═══════════════════════════ Palette ═══════════════════════════════════════
T      = (0,   0,   0,   0)

# Shared humanoid
SKIN   = (208, 152, 104, 255)
SKIN_D = (160, 104,  64, 255)
HAIR   = ( 56,  36,  16, 255)
BOOT   = ( 36,  24,  12, 255)
BELT   = ( 88,  60,  24, 255)
EYE    = ( 40,  28,  20, 255)

# NPC Guide — olive-green travel robe
GD_ROB  = (100, 116,  64, 255)
GD_ROBD = ( 68,  80,  40, 255)

# NPC Instructor — red padded combat jacket
IN_ARM  = (140,  44,  32, 255)
IN_ARMD = ( 92,  28,  20, 255)

# NPC Goblin — green skin, brown rags (walk/idle frames)
GOB_SK = ( 88, 124,  48, 255)
GOB_SD = ( 60,  88,  28, 255)
GOB_RG = (104,  64,  24, 255)
GOB_RD = ( 72,  44,  16, 255)
GOB_EY = (220, 180,  20, 255)

# Rat
RAT_B  = (128,  96,  72, 255)   # body warm brown
RAT_D  = ( 88,  64,  48, 255)   # shadow / underbelly
RAT_L  = (168, 136, 112, 255)   # fur highlight
RAT_T  = (100,  72,  52, 255)   # tail
RAT_EY = ( 20,  12,   8, 255)
RAT_NS = (200, 140, 140, 255)   # pink nose

# Giant rat — darker, red eyes
GR_B   = (108,  76,  52, 255)
GR_D   = ( 72,  48,  32, 255)
GR_L   = (148, 112,  80, 255)
GR_T   = ( 84,  56,  36, 255)
GR_EY  = (200,  24,  24, 255)   # red eyes, menacing
GR_NS  = (180, 120, 120, 255)

# Chicken
CH_W   = (220, 208, 180, 255)   # cream-white feathers
CH_D   = (176, 164, 136, 255)   # shadow
CH_CRD = (208,  48,  48, 255)   # comb / wattle red
CH_BEK = (220, 160,  40, 255)   # beak / legs orange-yellow
CH_EY  = ( 24,  16,  12, 255)

# Cow
CW_W   = (232, 228, 220, 255)   # off-white
CW_P   = ( 44,  36,  28, 255)   # dark brown patches
CW_MZ  = (224, 192, 176, 255)   # pink muzzle / udder
CW_HF  = ( 40,  32,  20, 255)   # hooves
CW_HN  = (204, 188, 156, 255)   # horn
CW_EY  = ( 20,  12,   8, 255)

# ═══════════════════════════ Draw helpers ══════════════════════════════════
def new(w, h):
    return Image.new("RGBA", (w, h), T)

def save(name, img):
    img.save(os.path.join(OUT, name))
    print(f"  {name}")

def px(img, x, y, c):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), c)

def rect(img, x, y, w, h, c):
    for dy in range(h):
        for dx in range(w):
            px(img, x + dx, y + dy, c)

def hline(img, x1, x2, y, c):
    for x in range(x1, x2 + 1):
        px(img, x, y, c)

def vline(img, x, y1, y2, c):
    for y in range(y1, y2 + 1):
        px(img, x, y, c)

def flip_h(img):
    return img.transpose(Image.FLIP_LEFT_RIGHT)

# ═══════════════ Humanoid helpers (identical to Pack 5) ════════════════════

def head_south(img, hy, skin, skin_d, hair, eye):
    hline(img,  4, 11, hy,     hair)
    px(img,  3, hy + 1, hair);  px(img, 12, hy + 1, hair)
    rect(img, 4, hy + 1, 8, 4, skin)
    px(img,  5, hy + 3, eye);   px(img, 10, hy + 3, eye)
    hline(img,  5, 10, hy + 5, skin)
    px(img,  4, hy + 5, skin_d); px(img, 11, hy + 5, skin_d)

def head_north(img, hy, hair):
    hline(img,  4, 11, hy,     hair)
    rect(img,   3, hy + 1, 10, 5, hair)

def head_east(img, hy, skin, skin_d, hair, eye):
    hline(img,  6, 12, hy,     hair)
    rect(img,   6, hy + 1, 4, 4, skin)
    hline(img, 10, 13, hy + 1, hair);  hline(img, 10, 13, hy + 2, hair)
    hline(img, 10, 12, hy + 3, hair)
    px(img,  8, hy + 3, eye)
    hline(img,  6,  9, hy + 5, skin)

def head_west(img, hy, skin, skin_d, hair, eye):
    hline(img,  3,  9, hy,     hair)
    rect(img,   6, hy + 1, 4, 4, skin)
    hline(img,  2,  5, hy + 1, hair);  hline(img,  2,  5, hy + 2, hair)
    hline(img,  3,  5, hy + 3, hair)
    px(img,  7, hy + 3, eye)
    hline(img,  6,  9, hy + 5, skin)

def torso_south(img, ty, shirt, shirt_d, belt, skin):
    rect(img,  7, ty,     2, 1, skin)
    rect(img,  3, ty + 1, 10, 6, shirt)
    vline(img, 12, ty + 1, ty + 6, shirt_d)
    hline(img,  3, 12, ty + 7, belt)

def torso_north(img, ty, shirt, shirt_d, belt, skin):
    rect(img,  6, ty,     4, 1, skin)
    rect(img,  4, ty + 1, 8, 6, shirt)
    vline(img, 11, ty + 1, ty + 6, shirt_d)
    hline(img,  4, 11, ty + 7, belt)

def torso_east(img, ty, shirt, shirt_d, belt, skin):
    px(img,  6, ty, skin)
    rect(img,  6, ty + 1, 5, 6, shirt)
    vline(img, 11, ty + 1, ty + 3, shirt_d)
    hline(img,  6, 11, ty + 7, belt)

def torso_west(img, ty, shirt, shirt_d, belt, skin):
    px(img,  9, ty, skin)
    rect(img,  5, ty + 1, 5, 6, shirt)
    vline(img,  4, ty + 1, ty + 3, shirt_d)
    hline(img,  4,  9, ty + 7, belt)

def legs_south(img, frame, pnt, pnt_d, boot):
    if frame in (0, 2):
        rect(img,  3, 15, 4, 6, pnt);   rect(img,  9, 15, 4, 6, pnt)
        rect(img,  3, 21, 5, 2, boot);  rect(img,  8, 21, 5, 2, boot)
    elif frame == 1:
        rect(img,  2, 15, 4, 7, pnt);   vline(img, 5, 15, 21, pnt_d)
        rect(img,  9, 15, 4, 6, pnt);   vline(img, 12, 15, 20, pnt_d)
        rect(img,  2, 22, 5, 1, boot);  rect(img,  8, 21, 5, 2, boot)
    else:
        rect(img,  3, 15, 4, 6, pnt);   vline(img, 6, 15, 20, pnt_d)
        rect(img,  9, 15, 4, 7, pnt);   vline(img, 12, 15, 21, pnt_d)
        rect(img,  3, 21, 5, 2, boot);  rect(img,  9, 22, 5, 1, boot)

def legs_north(img, frame, pnt, pnt_d, boot):
    if frame in (0, 2):
        rect(img,  4, 15, 3, 6, pnt);   rect(img,  9, 15, 3, 6, pnt)
        rect(img,  3, 21, 5, 2, boot);  rect(img,  8, 21, 5, 2, boot)
    elif frame == 1:
        rect(img,  3, 15, 3, 7, pnt);   vline(img, 5, 15, 21, pnt_d)
        rect(img,  9, 15, 3, 6, pnt);   vline(img, 11, 15, 20, pnt_d)
        rect(img,  2, 22, 5, 1, boot);  rect(img,  8, 21, 5, 2, boot)
    else:
        rect(img,  4, 15, 3, 6, pnt);   vline(img, 6, 15, 20, pnt_d)
        rect(img,  9, 15, 3, 7, pnt);   vline(img, 11, 15, 21, pnt_d)
        rect(img,  3, 21, 5, 2, boot);  rect(img,  8, 22, 5, 1, boot)

def legs_east(img, frame, pnt, pnt_d, boot):
    if frame in (0, 2):
        rect(img,  7, 15, 4, 6, pnt);   rect(img,  6, 21, 5, 2, boot)
    elif frame == 1:
        rect(img,  6, 15, 3, 6, pnt_d); rect(img,  8, 15, 4, 7, pnt)
        rect(img,  6, 21, 4, 1, boot);  rect(img,  7, 22, 5, 1, boot)
    else:
        rect(img,  6, 15, 4, 7, pnt);   rect(img,  8, 15, 3, 6, pnt_d)
        rect(img,  6, 22, 5, 1, boot);  rect(img,  8, 21, 4, 1, boot)

def legs_west(img, frame, pnt, pnt_d, boot):
    if frame in (0, 2):
        rect(img,  5, 15, 4, 6, pnt);   rect(img,  5, 21, 5, 2, boot)
    elif frame == 1:
        rect(img,  4, 15, 4, 7, pnt);   rect(img,  7, 15, 3, 6, pnt_d)
        rect(img,  4, 22, 5, 1, boot);  rect(img,  6, 21, 4, 1, boot)
    else:
        rect(img,  7, 15, 3, 6, pnt_d); rect(img,  4, 15, 4, 7, pnt)
        rect(img,  5, 22, 5, 1, boot);  rect(img,  7, 21, 4, 1, boot)

# ═══════════════════════════ Guide NPC ═════════════════════════════════════
def guide_idle(frame):
    img = new(16, 24)
    dy = 1 if frame == 1 else 0
    head_south(img, 1 + dy, SKIN, SKIN_D, HAIR, EYE)
    torso_south(img, 7 + dy, GD_ROB, GD_ROBD, BELT, SKIN)
    legs_south(img, 0, GD_ROB, GD_ROBD, BOOT)
    return img

def guide_walk(frame, direction):
    img = new(16, 24)
    dy = 1 if frame in (1, 3) else 0
    if direction == 's':
        head_south(img, 1+dy, SKIN, SKIN_D, HAIR, EYE)
        torso_south(img, 7+dy, GD_ROB, GD_ROBD, BELT, SKIN)
        legs_south(img, frame, GD_ROB, GD_ROBD, BOOT)
    elif direction == 'n':
        head_north(img, 1+dy, HAIR)
        torso_north(img, 7+dy, GD_ROB, GD_ROBD, BELT, SKIN)
        legs_north(img, frame, GD_ROB, GD_ROBD, BOOT)
    elif direction == 'e':
        head_east(img, 1+dy, SKIN, SKIN_D, HAIR, EYE)
        torso_east(img, 7+dy, GD_ROB, GD_ROBD, BELT, SKIN)
        legs_east(img, frame, GD_ROB, GD_ROBD, BOOT)
    else:
        head_west(img, 1+dy, SKIN, SKIN_D, HAIR, EYE)
        torso_west(img, 7+dy, GD_ROB, GD_ROBD, BELT, SKIN)
        legs_west(img, frame, GD_ROB, GD_ROBD, BOOT)
    return img

# ═══════════════════════════ Instructor NPC ════════════════════════════════
def instructor_idle(frame):
    img = new(16, 24)
    dy = 1 if frame == 1 else 0
    head_south(img, 1+dy, SKIN, SKIN_D, HAIR, EYE)
    torso_south(img, 7+dy, IN_ARM, IN_ARMD, BELT, SKIN)
    legs_south(img, 0, IN_ARM, IN_ARMD, BOOT)
    return img

def instructor_walk(frame, direction):
    img = new(16, 24)
    dy = 1 if frame in (1, 3) else 0
    if direction == 's':
        head_south(img, 1+dy, SKIN, SKIN_D, HAIR, EYE)
        torso_south(img, 7+dy, IN_ARM, IN_ARMD, BELT, SKIN)
        legs_south(img, frame, IN_ARM, IN_ARMD, BOOT)
    elif direction == 'n':
        head_north(img, 1+dy, HAIR)
        torso_north(img, 7+dy, IN_ARM, IN_ARMD, BELT, SKIN)
        legs_north(img, frame, IN_ARM, IN_ARMD, BOOT)
    elif direction == 'e':
        head_east(img, 1+dy, SKIN, SKIN_D, HAIR, EYE)
        torso_east(img, 7+dy, IN_ARM, IN_ARMD, BELT, SKIN)
        legs_east(img, frame, IN_ARM, IN_ARMD, BOOT)
    else:
        head_west(img, 1+dy, SKIN, SKIN_D, HAIR, EYE)
        torso_west(img, 7+dy, IN_ARM, IN_ARMD, BELT, SKIN)
        legs_west(img, frame, IN_ARM, IN_ARMD, BOOT)
    return img

# ═══════════════════════════ Goblin NPC (idle + walk) ══════════════════════
#
# Squat layout: head at y=4 (3 clear rows above = shorter appearance).
# Head = 4 rows (y=4..7), neck y=8, cloth torso y=9..13, belt y=14,
# legs y=15..20, boots y=21..22.  Same leg functions as player.
#
# Goblin head functions (compact, bald, pointed ears, yellow eyes).

def gob_head_south(img, hy):
    hline(img,  5, 10, hy,     GOB_SK)
    hline(img,  4, 11, hy + 1, GOB_SK)
    hline(img,  4, 11, hy + 2, GOB_SK)
    px(img,  5, hy + 2, GOB_EY);  px(img, 10, hy + 2, GOB_EY)
    hline(img,  5, 10, hy + 3, GOB_SD)
    px(img,  3, hy + 1, GOB_SK)   # left pointed ear
    px(img, 12, hy + 1, GOB_SK)   # right pointed ear

def gob_head_north(img, hy):
    hline(img,  5, 10, hy,     GOB_SK)
    hline(img,  4, 11, hy + 1, GOB_SK)
    hline(img,  4, 11, hy + 2, GOB_SD)
    hline(img,  5, 10, hy + 3, GOB_SD)
    px(img,  3, hy + 1, GOB_SK);  px(img, 12, hy + 1, GOB_SK)

def gob_head_east(img, hy):
    # Profile: face at x=7-10, back/ear at x=11-13
    hline(img,  7, 12, hy,     GOB_SK)
    rect(img,   7, hy + 1, 4, 2, GOB_SK)
    hline(img, 11, 13, hy + 1, GOB_SK)
    px(img,  9, hy + 1, GOB_EY)
    hline(img,  7, 10, hy + 3, GOB_SD)
    px(img, 14, hy + 1, GOB_SK)   # ear tip

def gob_head_west(img, hy):
    hline(img,  3,  8, hy,     GOB_SK)
    rect(img,   5, hy + 1, 4, 2, GOB_SK)
    hline(img,  2,  4, hy + 1, GOB_SK)
    px(img,  6, hy + 1, GOB_EY)
    hline(img,  5,  8, hy + 3, GOB_SD)
    px(img,  1, hy + 1, GOB_SK)

def gob_torso_south(img, ty):
    rect(img,  7, ty,     2, 1, GOB_SK)           # neck
    rect(img,  4, ty + 1, 8, 5, GOB_RG)           # cloth body
    vline(img, 11, ty + 1, ty + 5, GOB_RD)
    hline(img,  4, 11, ty + 6, BELT)

def gob_torso_north(img, ty):
    rect(img,  6, ty,     4, 1, GOB_SK)
    rect(img,  5, ty + 1, 6, 5, GOB_RG)
    vline(img, 10, ty + 1, ty + 5, GOB_RD)
    hline(img,  5, 10, ty + 6, BELT)

def gob_torso_east(img, ty):
    px(img,  6, ty, GOB_SK)
    rect(img,  6, ty + 1, 5, 5, GOB_RG)
    vline(img, 10, ty + 1, ty + 3, GOB_RD)
    hline(img,  6, 10, ty + 6, BELT)

def gob_torso_west(img, ty):
    px(img,  9, ty, GOB_SK)
    rect(img,  5, ty + 1, 5, 5, GOB_RG)
    vline(img,  5, ty + 1, ty + 3, GOB_RD)
    hline(img,  5,  9, ty + 6, BELT)

def goblin_idle(frame):
    img = new(16, 24)
    hy = 4 + (1 if frame == 1 else 0)
    gob_head_south(img, hy)
    gob_torso_south(img, hy + 4)
    legs_south(img, 0, GOB_SK, GOB_SD, BOOT)
    return img

def goblin_walk(frame, direction):
    img = new(16, 24)
    dy = 1 if frame in (1, 3) else 0
    hy = 4 + dy
    ty = hy + 4
    if direction == 's':
        gob_head_south(img, hy)
        gob_torso_south(img, ty)
        legs_south(img, frame, GOB_SK, GOB_SD, BOOT)
    elif direction == 'n':
        gob_head_north(img, hy)
        gob_torso_north(img, ty)
        legs_north(img, frame, GOB_SK, GOB_SD, BOOT)
    elif direction == 'e':
        gob_head_east(img, hy)
        gob_torso_east(img, ty)
        legs_east(img, frame, GOB_SK, GOB_SD, BOOT)
    else:
        gob_head_west(img, hy)
        gob_torso_west(img, ty)
        legs_west(img, frame, GOB_SK, GOB_SD, BOOT)
    return img

# ═══════════════════════════ Rat (16×12) ═══════════════════════════════════
#
# East-facing: head at right (x=9-14), body x=2-11, tail at left x=0-2.
# Legs y=7-10, ground anchor y=11 (clear).
# South/north: round foreshortened blob with face or back.
# West walk = flip_h(east walk).

def _rat_body_e(img, B, D, L, T, EY, NS):
    # Tail curling at top-left
    px(img, 0, 2, T);  px(img, 1, 2, T)
    px(img, 1, 3, T);  px(img, 2, 3, T)
    # Back / rump highlight
    hline(img, 2,  8, 2, L)
    # Main body
    hline(img, 2, 11, 3, B)
    hline(img, 2, 11, 4, B)
    hline(img, 2, 10, 5, D)
    # Head (right side)
    hline(img,  9, 13, 2, B)
    hline(img, 10, 14, 3, B)
    hline(img,  9, 13, 4, B)
    hline(img,  9, 12, 5, B)
    px(img, 12, 3, EY)           # eye
    px(img, 14, 3, NS)           # nose tip
    px(img, 13, 4, NS)
    px(img, 10, 1, D)            # ear hint

def _rat_legs_e(img, frame, B):
    # front legs near head (x=9-10), back legs near tail (x=4-5), y=6-10
    if frame in (0, 2):
        rect(img, 4, 6, 2, 4, B);   rect(img, 9, 6, 2, 4, B)
    elif frame == 1:
        rect(img, 3, 6, 2, 5, B);   rect(img, 10, 6, 2, 5, B)
    else:
        rect(img, 5, 6, 2, 5, B);   rect(img, 8, 6, 2, 5, B)

def _rat_body_s(img, B, D, L, EY, NS):
    # Front-facing blob (foreshortened)
    hline(img, 5, 10, 1, B)     # ear hints at top
    px(img, 4, 1, D);  px(img, 11, 1, D)
    hline(img, 4, 11, 2, B)
    hline(img, 3, 12, 3, B)
    hline(img, 3, 12, 4, B)
    hline(img, 4, 11, 5, D)
    hline(img, 5, 10, 6, D)
    px(img, 5, 3, EY);  px(img, 10, 3, EY)   # two eyes
    px(img, 7, 4, NS);  px(img, 8, 4, NS)     # nose

def _rat_legs_s(img, frame, B):
    # Two front paws spread at bottom
    if frame in (0, 2):
        rect(img, 4, 7, 2, 3, B);  rect(img, 10, 7, 2, 3, B)
    elif frame == 1:
        rect(img, 3, 7, 2, 4, B);  rect(img, 11, 7, 2, 4, B)
    else:
        rect(img, 5, 7, 2, 4, B);  rect(img,  9, 7, 2, 4, B)

def _rat_body_n(img, B, D, L, T):
    # Back view: rump highlight, tail visible at top
    px(img, 7, 0, T);  px(img, 8, 0, T)       # tail curling up
    hline(img, 5, 10, 1, T)
    hline(img, 4, 11, 2, L)
    hline(img, 3, 12, 3, B)
    hline(img, 3, 12, 4, B)
    hline(img, 4, 11, 5, D)
    hline(img, 5, 10, 6, D)

def npc_rat_idle(frame):
    img = new(16, 12)
    _rat_body_e(img, RAT_B, RAT_D, RAT_L, RAT_T, RAT_EY, RAT_NS)
    _rat_legs_e(img, 0 if frame == 0 else 2, RAT_B)
    return img

def npc_rat_walk(frame, direction):
    if direction == 'w':
        base = npc_rat_walk(frame, 'e')
        return flip_h(base)
    img = new(16, 12)
    if direction == 'e':
        _rat_body_e(img, RAT_B, RAT_D, RAT_L, RAT_T, RAT_EY, RAT_NS)
        _rat_legs_e(img, frame, RAT_B)
    elif direction == 's':
        _rat_body_s(img, RAT_B, RAT_D, RAT_L, RAT_EY, RAT_NS)
        _rat_legs_s(img, frame, RAT_B)
    else:  # 'n'
        _rat_body_n(img, RAT_B, RAT_D, RAT_L, RAT_T)
        _rat_legs_s(img, frame, RAT_B)   # same leg pattern from behind
    return img

# ═══════════════════════════ Giant Rat (24×20) ═════════════════════════════
#
# Same proportional design as rat, scaled to 24×20.
# Body center roughly x=5-19, head x=15-22, tail x=0-4.
# Legs y=12-18, ground y=19 (clear).

def _gr_body_e(img):
    # Tail (left, curling)
    px(img, 0, 3, GR_T);  px(img, 1, 3, GR_T)
    px(img, 1, 4, GR_T);  px(img, 2, 4, GR_T);  px(img, 3, 5, GR_T)
    # Back highlight
    hline(img,  3, 13, 3, GR_L)
    # Main body
    hline(img,  3, 17, 4, GR_B)
    hline(img,  3, 17, 5, GR_B)
    hline(img,  3, 17, 6, GR_B)
    hline(img,  3, 16, 7, GR_B)
    hline(img,  4, 15, 8, GR_D)    # underbelly
    hline(img,  5, 14, 9, GR_D)
    # Head (right)
    hline(img, 14, 21, 3, GR_B)
    hline(img, 15, 22, 4, GR_B)
    hline(img, 15, 22, 5, GR_B)
    hline(img, 14, 21, 6, GR_B)
    hline(img, 14, 20, 7, GR_B)
    # Ear (top of head)
    px(img, 16, 2, GR_D);  px(img, 17, 2, GR_D)
    px(img, 16, 1, GR_B)
    # Eye (red, menacing)
    px(img, 20, 4, GR_EY);  px(img, 21, 4, GR_EY)
    # Nose
    px(img, 22, 5, GR_NS);  px(img, 23, 5, GR_NS)
    px(img, 22, 6, GR_NS)
    # Teeth hint
    px(img, 22, 7, (232, 224, 208, 255))

def _gr_legs_e(img, frame):
    # Front legs x=15-17, back legs x=7-9, y=10-18
    if frame in (0, 2):
        rect(img,  7, 10, 3, 8, GR_B);  rect(img, 15, 10, 3, 8, GR_B)
    elif frame == 1:
        rect(img,  5, 10, 3, 9, GR_B);  rect(img, 16, 10, 3, 9, GR_B)
    else:
        rect(img,  9, 10, 3, 9, GR_B);  rect(img, 13, 10, 3, 9, GR_B)

def _gr_body_s(img):
    # Front blob view
    px(img,  7, 0, GR_D);  px(img, 16, 0, GR_D)   # ears
    hline(img,  6,  8, 1, GR_B);  hline(img, 15, 17, 1, GR_B)
    hline(img,  5, 18, 2, GR_B)
    hline(img,  4, 19, 3, GR_B)
    hline(img,  4, 19, 4, GR_B)
    hline(img,  4, 19, 5, GR_B)
    hline(img,  5, 18, 6, GR_D)
    hline(img,  6, 17, 7, GR_D)
    hline(img,  7, 16, 8, GR_D)
    # Eyes
    px(img,  7, 3, GR_EY);  px(img,  8, 3, GR_EY)
    px(img, 15, 3, GR_EY);  px(img, 16, 3, GR_EY)
    # Nose
    px(img, 11, 5, GR_NS);  px(img, 12, 5, GR_NS)

def _gr_legs_s(img, frame):
    if frame in (0, 2):
        rect(img,  6, 9, 3, 9, GR_B);  rect(img, 15, 9, 3, 9, GR_B)
    elif frame == 1:
        rect(img,  4, 9, 3, 10, GR_B); rect(img, 17, 9, 3, 10, GR_B)
    else:
        rect(img,  8, 9, 3, 10, GR_B); rect(img, 13, 9, 3, 10, GR_B)

def _gr_body_n(img):
    # Back view — rump highlight, tail at top
    hline(img,  9, 14, 0, GR_T)           # tail
    hline(img,  7, 16, 1, GR_T)
    hline(img,  6, 17, 2, GR_L)           # rump highlight
    hline(img,  4, 19, 3, GR_B)
    hline(img,  4, 19, 4, GR_B)
    hline(img,  4, 19, 5, GR_B)
    hline(img,  5, 18, 6, GR_D)
    hline(img,  6, 17, 7, GR_D)
    hline(img,  7, 16, 8, GR_D)

def npc_giant_rat_idle(frame):
    img = new(24, 20)
    _gr_body_e(img)
    _gr_legs_e(img, 0 if frame == 0 else 2)
    return img

def npc_giant_rat_walk(frame, direction):
    if direction == 'w':
        return flip_h(npc_giant_rat_walk(frame, 'e'))
    img = new(24, 20)
    if direction == 'e':
        _gr_body_e(img);    _gr_legs_e(img, frame)
    elif direction == 's':
        _gr_body_s(img);    _gr_legs_s(img, frame)
    else:
        _gr_body_n(img);    _gr_legs_s(img, frame)
    return img

# ═══════════════════════════ Chicken (16×12) ═══════════════════════════════
#
# East-facing: head at right (x=9-15), body x=2-11, tail feathers at left.
# Biped: 2 thin yellow legs. Head bobs +1px right on stride frames.
# West = flip_h(east).

def _ch_body_e(img, head_dx=0):
    # Tail feathers (left, x=0-3)
    px(img, 0, 2, CH_D);  px(img, 1, 1, CH_W)
    px(img, 1, 2, CH_W);  px(img, 2, 2, CH_W)
    px(img, 0, 3, CH_W);  px(img, 1, 3, CH_W)
    # Body oval
    hline(img, 2,  9, 2, CH_W)
    hline(img, 2, 10, 3, CH_W)
    hline(img, 2, 10, 4, CH_W)
    hline(img, 2, 10, 5, CH_W)
    hline(img, 3,  9, 6, CH_D)
    # Wing texture (slightly darker band)
    hline(img, 3,  8, 4, CH_D)
    # Head (right side, with head_dx shift for bob)
    hx = 9 + head_dx
    hline(img, hx, hx+3, 1, CH_W)         # comb base
    px(img, hx, 0, CH_CRD)                # comb top
    px(img, hx+1, 0, CH_CRD)
    hline(img, hx, hx+4, 2, CH_W)         # head
    hline(img, hx, hx+4, 3, CH_W)
    hline(img, hx, hx+3, 4, CH_W)
    px(img, hx+1, 5, CH_CRD)              # wattle
    # Beak (pointing right)
    px(img, hx+5, 3, CH_BEK)
    # Eye
    px(img, hx+2, 2, CH_EY)

def _ch_legs_e(img):
    # Two thin yellow legs at x=6, x=8, y=7-10, feet spread at y=10
    rect(img, 6, 7, 1, 3, CH_BEK)
    rect(img, 8, 7, 1, 3, CH_BEK)
    px(img, 5, 10, CH_BEK);  px(img, 6, 10, CH_BEK);  px(img, 7, 10, CH_BEK)  # left foot
    px(img, 7, 10, CH_BEK);  px(img, 8, 10, CH_BEK);  px(img, 9, 10, CH_BEK)  # right foot

def _ch_legs_stride_e(img, frame):
    # Stride A (frame 1): left leg fwd, right back
    # Stride B (frame 3): right leg fwd, left back
    if frame in (0, 2):
        _ch_legs_e(img)
    elif frame == 1:
        rect(img, 5, 7, 1, 4, CH_BEK);  rect(img, 9, 7, 1, 4, CH_BEK)
        px(img, 4, 11, CH_BEK);  px(img, 5, 11, CH_BEK);  px(img, 6, 11, CH_BEK)
        px(img, 8, 11, CH_BEK);  px(img, 9, 11, CH_BEK);  px(img, 10, 11, CH_BEK)
    else:
        rect(img, 7, 7, 1, 4, CH_BEK);  rect(img, 7, 7, 1, 4, CH_BEK)
        rect(img, 5, 7, 1, 3, CH_BEK);  rect(img, 9, 7, 1, 3, CH_BEK)
        px(img, 6, 11, CH_BEK);  px(img, 7, 11, CH_BEK);  px(img, 8, 11, CH_BEK)

def _ch_body_s(img):
    # Front view — round fluffy blob with face
    hline(img, 5, 10, 0, CH_CRD)    # comb
    hline(img, 4, 11, 1, CH_W)
    hline(img, 3, 12, 2, CH_W)
    hline(img, 3, 12, 3, CH_W)
    hline(img, 3, 12, 4, CH_W)
    hline(img, 4, 11, 5, CH_D)
    hline(img, 5, 10, 6, CH_D)
    px(img, 5, 2, CH_EY);   px(img, 10, 2, CH_EY)
    hline(img, 7, 8, 4, CH_BEK)   # beak (forward)

def _ch_legs_s(img, frame):
    if frame in (0, 2):
        rect(img, 5, 7, 1, 3, CH_BEK);  rect(img, 10, 7, 1, 3, CH_BEK)
        hline(img, 4, 6, 10, CH_BEK);   hline(img, 9, 11, 10, CH_BEK)
    elif frame == 1:
        rect(img, 4, 7, 1, 4, CH_BEK);  rect(img, 11, 7, 1, 4, CH_BEK)
        hline(img, 3, 5, 11, CH_BEK);   hline(img, 10, 12, 11, CH_BEK)
    else:
        rect(img, 6, 7, 1, 4, CH_BEK);  rect(img, 9, 7, 1, 4, CH_BEK)
        hline(img, 5, 7, 11, CH_BEK);   hline(img, 8, 10, 11, CH_BEK)

def _ch_body_n(img):
    # Back view — round blob, no face, tail feathers at top
    px(img, 7, 0, CH_W);  px(img, 8, 0, CH_W)   # tail feather tips
    hline(img, 5, 10, 1, CH_W)
    hline(img, 4, 11, 2, CH_W)
    hline(img, 3, 12, 3, CH_W)
    hline(img, 3, 12, 4, CH_W)
    hline(img, 4, 11, 5, CH_D)
    hline(img, 5, 10, 6, CH_D)

def npc_chicken_idle(frame):
    img = new(16, 12)
    _ch_body_e(img, head_dx=0)
    _ch_legs_e(img)
    if frame == 1:   # head dips to peck
        img = new(16, 12)
        _ch_body_e(img, head_dx=1)   # head 1px fwd
        _ch_legs_e(img)
    return img

def npc_chicken_walk(frame, direction):
    if direction == 'w':
        return flip_h(npc_chicken_walk(frame, 'e'))
    img = new(16, 12)
    head_dx = 1 if frame in (1, 3) else 0   # bob forward on stride
    if direction == 'e':
        _ch_body_e(img, head_dx=head_dx)
        _ch_legs_stride_e(img, frame)
    elif direction == 's':
        _ch_body_s(img)
        _ch_legs_s(img, frame)
    else:  # 'n'
        _ch_body_n(img)
        _ch_legs_s(img, frame)
    return img

# ═══════════════════════════ Cow (24×20) ════════════════════════════════════
#
# East-facing: head at right (x=14-23), body x=2-17, tail at left.
# 4 legs: front pair x=13-15, back pair x=6-8, y=13-18, hooves y=18-19.
# South/north: wide front/back views.
# West = flip_h(east).

def _cow_body_e(img):
    # Tail (left, curling up)
    vline(img,  1, 2, 7, CW_W)
    px(img, 0, 3, CW_W);  px(img, 0, 4, CW_W)
    # Main body (broad)
    hline(img,  2, 17, 3, CW_W)
    hline(img,  2, 17, 4, CW_W)
    hline(img,  2, 17, 5, CW_W)
    hline(img,  2, 17, 6, CW_W)
    hline(img,  2, 17, 7, CW_W)
    hline(img,  2, 16, 8, CW_W)
    hline(img,  3, 15, 9, CW_W)
    # Dark patches on body (characteristic cow markings)
    rect(img,  5, 4, 3, 3, CW_P)    # patch 1 (left-mid)
    rect(img, 10, 5, 4, 2, CW_P)    # patch 2 (mid)
    rect(img,  8, 8, 3, 2, CW_P)    # patch 3 (belly)
    # Udder (pink, underside right-center)
    rect(img,  8, 10, 6, 3, CW_MZ)
    hline(img,  9, 12, 13, CW_MZ)
    # Belly shadow
    hline(img,  4, 14, 9, (200, 196, 188, 255))
    # Head (right side)
    hline(img, 14, 21, 3, CW_W)
    hline(img, 14, 22, 4, CW_W)
    hline(img, 14, 23, 5, CW_W)
    hline(img, 14, 23, 6, CW_W)
    hline(img, 14, 22, 7, CW_W)
    hline(img, 15, 21, 8, CW_W)
    # Horns (above head)
    px(img, 17, 1, CW_HN);  px(img, 18, 1, CW_HN);  px(img, 19, 1, CW_HN)
    px(img, 17, 2, CW_HN);  px(img, 20, 2, CW_HN)
    # Ear
    px(img, 22, 3, CW_W);  px(img, 23, 4, CW_W)
    # Muzzle (pink, lower right of head)
    rect(img, 15, 6, 5, 3, CW_MZ)
    # Eye
    px(img, 20, 4, CW_EY);  px(img, 21, 4, CW_EY)
    # Nostrils
    px(img, 16, 7, CW_P);  px(img, 18, 7, CW_P)

def _cow_legs_e(img, frame):
    # Front pair x=13-15, back pair x=6-8, y=12-18, hooves y=18-19
    if frame in (0, 2):
        rect(img,  6, 12, 3, 6, CW_W);   rect(img, 13, 12, 3, 6, CW_W)
        rect(img,  6, 18, 3, 2, CW_HF);  rect(img, 13, 18, 3, 2, CW_HF)
    elif frame == 1:
        rect(img,  4, 12, 3, 7, CW_W);   rect(img, 15, 12, 3, 7, CW_W)
        rect(img,  4, 19, 3, 1, CW_HF);  rect(img, 15, 19, 3, 1, CW_HF)
        # Pulled-back legs (dark, behind front)
        rect(img,  8, 12, 2, 5, (196, 192, 184, 255))
        rect(img, 11, 12, 2, 5, (196, 192, 184, 255))
    else:
        rect(img,  8, 12, 3, 7, CW_W);   rect(img, 11, 12, 3, 7, CW_W)
        rect(img,  8, 19, 3, 1, CW_HF);  rect(img, 11, 19, 3, 1, CW_HF)
        rect(img,  4, 12, 2, 5, (196, 192, 184, 255))
        rect(img, 15, 12, 2, 5, (196, 192, 184, 255))

def _cow_body_s(img):
    # Front view — wide body, horns on sides of head
    # Horns
    px(img,  3, 1, CW_HN);  px(img,  2, 2, CW_HN)
    px(img, 20, 1, CW_HN);  px(img, 21, 2, CW_HN)
    # Head centered
    hline(img,  6, 17, 2, CW_W)
    hline(img,  5, 18, 3, CW_W)
    hline(img,  5, 18, 4, CW_W)
    hline(img,  5, 18, 5, CW_W)
    # Eyes
    px(img,  7, 3, CW_EY);  px(img,  8, 3, CW_EY)
    px(img, 15, 3, CW_EY);  px(img, 16, 3, CW_EY)
    # Muzzle
    rect(img,  8, 5, 8, 3, CW_MZ)
    px(img,  9, 6, CW_P);  px(img, 14, 6, CW_P)   # nostrils
    # Body (wide)
    hline(img,  3, 20, 6, CW_W)
    hline(img,  2, 21, 7, CW_W)
    hline(img,  2, 21, 8, CW_W)
    hline(img,  2, 21, 9, CW_W)
    hline(img,  3, 20, 10, CW_W)
    hline(img,  4, 19, 11, CW_W)
    # Patches
    rect(img,  5, 7, 4, 3, CW_P)
    rect(img, 14, 8, 4, 2, CW_P)
    # Udder
    hline(img,  9, 14, 12, CW_MZ)

def _cow_legs_s(img, frame):
    # 4 legs visible from front, y=13-18, hooves y=18-19
    if frame in (0, 2):
        rect(img,  5, 13, 3, 5, CW_W);   rect(img, 16, 13, 3, 5, CW_W)   # outer
        rect(img,  9, 13, 3, 5, CW_W);   rect(img, 12, 13, 3, 5, CW_W)   # inner
        for lx in (5, 9, 12, 16): rect(img, lx, 18, 3, 2, CW_HF)
    elif frame == 1:
        rect(img,  4, 13, 3, 6, CW_W);   rect(img, 17, 13, 3, 6, CW_W)
        rect(img,  9, 13, 3, 5, CW_W);   rect(img, 12, 13, 3, 5, CW_W)
        rect(img,  4, 19, 3, 1, CW_HF);  rect(img, 17, 19, 3, 1, CW_HF)
        rect(img,  9, 18, 3, 2, CW_HF);  rect(img, 12, 18, 3, 2, CW_HF)
    else:
        rect(img,  5, 13, 3, 5, CW_W);   rect(img, 16, 13, 3, 5, CW_W)
        rect(img,  8, 13, 3, 6, CW_W);   rect(img, 13, 13, 3, 6, CW_W)
        rect(img,  5, 18, 3, 2, CW_HF);  rect(img, 16, 18, 3, 2, CW_HF)
        rect(img,  8, 19, 3, 1, CW_HF);  rect(img, 13, 19, 3, 1, CW_HF)

def _cow_body_n(img):
    # Back view — rump, tail, no face
    # Tail
    vline(img,  11, 0, 4, CW_W)
    vline(img,  12, 0, 4, CW_W)
    px(img, 11, 5, (160, 148, 128, 255))   # tail tuft
    px(img, 12, 5, (160, 148, 128, 255))
    # Rump / back
    hline(img,  5, 18, 2, CW_W)
    hline(img,  3, 20, 3, CW_W)
    hline(img,  2, 21, 4, CW_W)
    hline(img,  2, 21, 5, CW_W)
    hline(img,  2, 21, 6, CW_W)
    hline(img,  2, 21, 7, CW_W)
    hline(img,  3, 20, 8, CW_W)
    hline(img,  4, 19, 9, CW_W)
    hline(img,  5, 18, 10, CW_W)
    hline(img,  6, 17, 11, CW_W)
    # Patch on rump
    rect(img,  8, 4, 5, 4, CW_P)
    rect(img, 15, 6, 4, 3, CW_P)

def npc_cow_idle(frame):
    img = new(24, 20)
    _cow_body_e(img)
    _cow_legs_e(img, 0 if frame == 0 else 2)
    return img

def npc_cow_walk(frame, direction):
    if direction == 'w':
        return flip_h(npc_cow_walk(frame, 'e'))
    img = new(24, 20)
    if direction == 'e':
        _cow_body_e(img);   _cow_legs_e(img, frame)
    elif direction == 's':
        _cow_body_s(img);   _cow_legs_s(img, frame)
    else:  # 'n'
        _cow_body_n(img);   _cow_legs_s(img, frame)
    return img

# ═════════════════════════════ Main ════════════════════════════════════════
if __name__ == "__main__":
    print("Pack 6: remaining NPC animations")

    tasks = [
        ("npc_guide",    guide_idle,         guide_walk),
        ("npc_instructor", instructor_idle,  instructor_walk),
        ("npc_goblin",   goblin_idle,        goblin_walk),
    ]
    for prefix, idle_fn, walk_fn in tasks:
        for f in range(2):
            save(f"{prefix}_idle_{f}.png", idle_fn(f))
        for d in ('n', 's', 'e', 'w'):
            for f in range(4):
                save(f"{prefix}_walk_{d}_{f}.png", walk_fn(f, d))

    # Animal NPCs
    for f in range(2):
        save(f"npc_rat_idle_{f}.png", npc_rat_idle(f))
    for d in ('n', 's', 'e', 'w'):
        for f in range(4):
            save(f"npc_rat_walk_{d}_{f}.png", npc_rat_walk(f, d))

    for f in range(2):
        save(f"npc_giant_rat_idle_{f}.png", npc_giant_rat_idle(f))
    for d in ('n', 's', 'e', 'w'):
        for f in range(4):
            save(f"npc_giant_rat_walk_{d}_{f}.png", npc_giant_rat_walk(f, d))

    for f in range(2):
        save(f"npc_chicken_idle_{f}.png", npc_chicken_idle(f))
    for d in ('n', 's', 'e', 'w'):
        for f in range(4):
            save(f"npc_chicken_walk_{d}_{f}.png", npc_chicken_walk(f, d))

    for f in range(2):
        save(f"npc_cow_idle_{f}.png", npc_cow_idle(f))
    for d in ('n', 's', 'e', 'w'):
        for f in range(4):
            save(f"npc_cow_walk_{d}_{f}.png", npc_cow_walk(f, d))

    print(f"\nDone — 126 files written to {OUT}/")
