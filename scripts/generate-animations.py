#!/usr/bin/env python3
"""
generate-animations.py
Pack 5 — first animation batch for Erynfall.

Sprites generated:
  Player (16×24):   player_idle_0/1, player_walk_{n,s,e,w}_0..3, player_chop_0/1
  Ambient:          fire_idle_0/1 (16×20), fishing_spot_idle_0/1 (32×16)
  NPC banker(16×24):npc_banker_idle_0/1, npc_banker_walk_{n,s,e,w}_0..3
  NPC goblin(16×24):npc_goblin_action_0/1

Frame naming convention (critical):
  base_N.png  →  TexturePacker strips the _N suffix, packs as region base@N
  SpriteSheet.getAnimation("base") calls findRegions("base") → all N frames

Character layout (all humanoids, 16×24, boots anchored at y=21-22):
  y= 0        : clear (air above head)
  y= 1        : hair crown            ← head_y = 1
  y= 2..6     : face / hair sides
  y= 7        : neck
  y= 8..13    : shirt / torso
  y=14        : belt
  y=15..20    : legs
  y=21..22    : boots (always fixed — preserves bottom-center pivot)
  y=23        : clear (ground anchor row)

Walk bob: body_dy = +1 on stride frames (1 and 3) so upper body shifts
down 1px while boots stay fixed → weight-shift feel without pivot drift.

Usage:  python3 scripts/generate-animations.py
Requires: Pillow  (pip install Pillow)
"""
from PIL import Image
import os

OUT = os.path.join(os.path.dirname(__file__), "..", "art", "sprites")
os.makedirs(OUT, exist_ok=True)

# ═══════════════════════════ Palette ═══════════════════════════════════════
T      = (0,   0,   0,   0)       # transparent

# ── Player / generic humanoid ──
SKIN   = (208, 152, 104, 255)
SKIN_D = (160, 104,  64, 255)     # shadow / underside
HAIR   = ( 56,  36,  16, 255)
TUN    = ( 72,  96, 148, 255)     # tunic blue-grey
TUN_D  = ( 48,  64, 104, 255)
PNT    = ( 80,  64,  40, 255)     # pants brown
PNT_D  = ( 56,  44,  24, 255)
BOOT   = ( 36,  24,  12, 255)
BELT   = ( 88,  60,  24, 255)
EYE    = ( 40,  28,  20, 255)
AXE    = (160, 160, 168, 255)     # axe blade (grey metal)

# ── NPC Banker (gold shirt, dark-blue pants) ──
BNK_S  = (172, 136,  72, 255)
BNK_SD = (120,  92,  44, 255)
BNK_P  = ( 52,  44,  76, 255)
BNK_PD = ( 36,  28,  56, 255)

# ── NPC Goblin (green skin, brown rags) ──
GOB_SK = ( 88, 124,  48, 255)
GOB_SD = ( 60,  88,  28, 255)
GOB_RG = (104,  64,  24, 255)
GOB_RD = ( 72,  44,  16, 255)
GOB_EY = (220, 180,  20, 255)

# ── Fire ──
FR_TIP = (240, 220, 100, 255)
FR_MID = (240, 160,  32, 255)
FR_LO  = (220, 100,  20, 255)
FR_BSE = (180,  48,   8, 255)
FR_GLW = (240, 140,  16, 192)    # semi-transparent glow

# ── Water / fishing ──
WAT    = ( 80, 148, 192, 255)
WAT_D  = ( 48, 100, 148, 255)
WAT_L  = (128, 180, 220, 255)
WAT_F  = (188, 212, 228, 255)    # foam / ripple highlight
BOB_R  = (200,  40,  40, 255)    # bobber red
BOB_L  = ( 60,  60,  60, 255)    # fishing line dark

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
    """Draw filled rectangle. Clips to canvas bounds."""
    for dy in range(h):
        for dx in range(w):
            px(img, x + dx, y + dy, c)

def hline(img, x1, x2, y, c):
    for x in range(x1, x2 + 1):
        px(img, x, y, c)

def vline(img, x, y1, y2, c):
    for y in range(y1, y2 + 1):
        px(img, x, y, c)

# ═══════════════════════════ Head drawing ══════════════════════════════════
#
# All head functions receive `head_y` = the y of the topmost hair/head row.
# Head occupies head_y..head_y+5 (6 rows total, chin on row head_y+5).
# Callers set head_y = 1 + body_dy  so upper body bobs without moving boots.

def head_south(img, hy, skin, skin_d, hair, eye):
    """South-facing (frontal). 10 px wide centred at x=3..12."""
    hline(img,  4, 11, hy,     hair)          # crown
    px(img,  3, hy + 1, hair)                 # left hair border
    px(img, 12, hy + 1, hair)                 # right hair border
    rect(img, 4, hy + 1, 8, 4, skin)          # face (y+1..y+4)
    px(img,  5, hy + 3, eye)                  # left eye
    px(img, 10, hy + 3, eye)                  # right eye
    hline(img,  5, 10, hy + 5, skin)          # chin centre
    px(img,  4, hy + 5, skin_d)               # chin corners (shadow)
    px(img, 11, hy + 5, skin_d)

def head_north(img, hy, hair):
    """North-facing (back of head). All hair, no visible face."""
    hline(img,  4, 11, hy,     hair)
    rect(img,   3, hy + 1, 10, 5, hair)

def head_east(img, hy, skin, skin_d, hair, eye):
    """East-facing right-profile. Face on left half (x=6-9), hair on right."""
    hline(img,  6, 12, hy,     hair)          # top hair
    rect(img,   6, hy + 1, 4, 4, skin)        # face
    hline(img, 10, 13, hy + 1, hair)          # back of head row 1
    hline(img, 10, 13, hy + 2, hair)
    hline(img, 10, 12, hy + 3, hair)
    px(img,  8, hy + 3, eye)                  # single eye
    hline(img,  6,  9, hy + 5, skin)          # chin

def head_west(img, hy, skin, skin_d, hair, eye):
    """West-facing left-profile. Mirror of east."""
    hline(img,  3,  9, hy,     hair)
    rect(img,   6, hy + 1, 4, 4, skin)
    hline(img,  2,  5, hy + 1, hair)
    hline(img,  2,  5, hy + 2, hair)
    hline(img,  3,  5, hy + 3, hair)
    px(img,  7, hy + 3, eye)
    hline(img,  6,  9, hy + 5, skin)

# ═══════════════════════════ Torso drawing ═════════════════════════════════
#
# Torso top (ty) = head_y + 6.
# Neck is at ty, shirt y=ty+1..ty+6, belt at ty+7.
# Callers pass ty = 7 + body_dy.

def torso_south(img, ty, shirt, shirt_d, belt, skin):
    rect(img,  7, ty,     2, 1, skin)          # neck visible
    rect(img,  3, ty + 1, 10, 6, shirt)        # shirt body
    vline(img, 12, ty + 1, ty + 6, shirt_d)    # right-side shadow
    hline(img,  3, 12, ty + 7, belt)

def torso_north(img, ty, shirt, shirt_d, belt, skin):
    rect(img,  6, ty,     4, 1, skin)           # back of neck
    rect(img,  4, ty + 1, 8, 6, shirt)
    vline(img, 11, ty + 1, ty + 6, shirt_d)
    hline(img,  4, 11, ty + 7, belt)

def torso_east(img, ty, shirt, shirt_d, belt, skin):
    px(img,    6, ty, skin)                     # neck side
    rect(img,  6, ty + 1, 5, 6, shirt)          # profile chest (5 px wide)
    vline(img, 11, ty + 1, ty + 3, shirt_d)     # right shadow
    hline(img,  6, 11, ty + 7, belt)

def torso_west(img, ty, shirt, shirt_d, belt, skin):
    px(img,    9, ty, skin)
    rect(img,  5, ty + 1, 5, 6, shirt)
    vline(img,  4, ty + 1, ty + 3, shirt_d)
    hline(img,  4,  9, ty + 7, belt)

# ═══════════════════════════ Leg/boot drawing ══════════════════════════════
#
# Legs occupy y=15..20 (6 rows).  Boots fixed at y=21..22 (2 rows).
# Stride frames shift one leg 1px laterally + 1 row longer to simulate
# ground contact; the other is pulled back 1 row shorter.
# NOTE: every leg block must meet its boot block without a gap.

def legs_south(img, frame, pnt, pnt_d, boot):
    if frame in (0, 2):                         # neutral / crossing
        rect(img,  3, 15, 4, 6, pnt)            # left leg  x=3-6
        rect(img,  9, 15, 4, 6, pnt)            # right leg x=9-12
        rect(img,  3, 21, 5, 2, boot)           # left boot x=3-7
        rect(img,  8, 21, 5, 2, boot)           # right boot x=8-12
    elif frame == 1:                            # left leg forward
        rect(img,  2, 15, 4, 7, pnt)            # left leg extended (x=2-5, y=15-21)
        vline(img, 5, 15, 21, pnt_d)            # left leg right-shadow
        rect(img,  9, 15, 4, 6, pnt)            # right leg pulled (y=15-20)
        vline(img, 12, 15, 20, pnt_d)
        rect(img,  2, 22, 5, 1, boot)           # left boot 1px lower
        rect(img,  8, 21, 5, 2, boot)           # right boot
    else:                                       # frame == 3: right leg forward
        rect(img,  3, 15, 4, 6, pnt)            # left leg pulled (y=15-20)
        vline(img, 6, 15, 20, pnt_d)
        rect(img,  9, 15, 4, 7, pnt)            # right leg extended (y=15-21)
        vline(img, 12, 15, 21, pnt_d)
        rect(img,  3, 21, 5, 2, boot)           # left boot
        rect(img,  9, 22, 5, 1, boot)           # right boot 1px lower

def legs_north(img, frame, pnt, pnt_d, boot):
    if frame in (0, 2):
        rect(img,  4, 15, 3, 6, pnt)
        rect(img,  9, 15, 3, 6, pnt)
        rect(img,  3, 21, 5, 2, boot)
        rect(img,  8, 21, 5, 2, boot)
    elif frame == 1:                            # left forward
        rect(img,  3, 15, 3, 7, pnt)            # y=15-21
        vline(img, 5, 15, 21, pnt_d)
        rect(img,  9, 15, 3, 6, pnt)            # y=15-20
        vline(img, 11, 15, 20, pnt_d)
        rect(img,  2, 22, 5, 1, boot)
        rect(img,  8, 21, 5, 2, boot)
    else:                                       # frame == 3: right forward
        rect(img,  4, 15, 3, 6, pnt)            # y=15-20
        vline(img, 6, 15, 20, pnt_d)
        rect(img,  9, 15, 3, 7, pnt)            # y=15-21
        vline(img, 11, 15, 21, pnt_d)
        rect(img,  3, 21, 5, 2, boot)
        rect(img,  8, 22, 5, 1, boot)

def legs_east(img, frame, pnt, pnt_d, boot):
    """Profile east. Front leg = right side of canvas (larger), back = behind."""
    if frame in (0, 2):                         # neutral — legs crossing
        rect(img,  7, 15, 4, 6, pnt)            # visible leg x=7-10
        rect(img,  6, 21, 5, 2, boot)
    elif frame == 1:                            # front leg strides forward
        rect(img,  6, 15, 3, 6, pnt_d)          # back leg (dark, y=15-20)
        rect(img,  8, 15, 4, 7, pnt)            # front leg (y=15-21)
        rect(img,  6, 21, 4, 1, boot)           # back boot (y=21 only)
        rect(img,  7, 22, 5, 1, boot)           # front boot (y=22)
    else:                                       # frame == 3
        rect(img,  6, 15, 4, 7, pnt)            # front leg (y=15-21)
        rect(img,  8, 15, 3, 6, pnt_d)          # back leg (dark, y=15-20)
        rect(img,  6, 22, 5, 1, boot)           # front boot
        rect(img,  8, 21, 4, 1, boot)           # back boot

def legs_west(img, frame, pnt, pnt_d, boot):
    """Profile west. Mirror of east."""
    if frame in (0, 2):
        rect(img,  5, 15, 4, 6, pnt)
        rect(img,  5, 21, 5, 2, boot)
    elif frame == 1:                            # front leg (left side) strides
        rect(img,  4, 15, 4, 7, pnt)            # front leg (y=15-21)
        rect(img,  7, 15, 3, 6, pnt_d)          # back leg
        rect(img,  4, 22, 5, 1, boot)
        rect(img,  6, 21, 4, 1, boot)
    else:                                       # frame == 3
        rect(img,  7, 15, 3, 6, pnt_d)          # back leg (y=15-20)
        rect(img,  4, 15, 4, 7, pnt)            # front leg (y=15-21)
        rect(img,  5, 22, 5, 1, boot)
        rect(img,  7, 21, 4, 1, boot)

# ═══════════════════════════ Player sprites ════════════════════════════════

def player_idle(frame):
    """16×24 south-facing idle. Frame 1 drops body 1px (gentle breath bob)."""
    img = new(16, 24)
    dy  = 1 if frame == 1 else 0
    head_south(img, 1 + dy, SKIN, SKIN_D, HAIR, EYE)
    torso_south(img, 7 + dy, TUN, TUN_D, BELT, SKIN)
    legs_south(img, 0, PNT, PNT_D, BOOT)
    return img

def player_walk_south(frame):
    img = new(16, 24)
    dy  = 1 if frame in (1, 3) else 0
    head_south(img, 1 + dy, SKIN, SKIN_D, HAIR, EYE)
    torso_south(img, 7 + dy, TUN, TUN_D, BELT, SKIN)
    legs_south(img, frame, PNT, PNT_D, BOOT)
    return img

def player_walk_north(frame):
    img = new(16, 24)
    dy  = 1 if frame in (1, 3) else 0
    head_north(img, 1 + dy, HAIR)
    torso_north(img, 7 + dy, TUN, TUN_D, BELT, SKIN)
    legs_north(img, frame, PNT, PNT_D, BOOT)
    return img

def player_walk_east(frame):
    img = new(16, 24)
    dy  = 1 if frame in (1, 3) else 0
    head_east(img, 1 + dy, SKIN, SKIN_D, HAIR, EYE)
    torso_east(img, 7 + dy, TUN, TUN_D, BELT, SKIN)
    legs_east(img, frame, PNT, PNT_D, BOOT)
    return img

def player_walk_west(frame):
    img = new(16, 24)
    dy  = 1 if frame in (1, 3) else 0
    head_west(img, 1 + dy, SKIN, SKIN_D, HAIR, EYE)
    torso_west(img, 7 + dy, TUN, TUN_D, BELT, SKIN)
    legs_west(img, frame, PNT, PNT_D, BOOT)
    return img

def player_chop(frame):
    """16×24, east-facing. Frame 0: axe raised, frame 1: mid-swing."""
    img = new(16, 24)
    head_east(img, 1, SKIN, SKIN_D, HAIR, EYE)
    torso_east(img, 7, TUN, TUN_D, BELT, SKIN)
    legs_east(img, 0, PNT, PNT_D, BOOT)
    # Axe head (2×2 grey) + handle (vline dark) on the right side
    if frame == 0:                              # raised high
        vline(img, 13,  4, 10, BOOT)           # handle
        rect(img, 14,  3, 2, 2, AXE)           # axe head
    else:                                       # swinging down to mid
        vline(img, 13,  8, 14, BOOT)
        rect(img, 14, 13, 2, 2, AXE)
    return img

# ═══════════════════════════ NPC Banker ════════════════════════════════════

def banker_idle(frame):
    img = new(16, 24)
    dy  = 1 if frame == 1 else 0
    head_south(img, 1 + dy, SKIN, SKIN_D, HAIR, EYE)
    torso_south(img, 7 + dy, BNK_S, BNK_SD, BELT, SKIN)
    legs_south(img, 0, BNK_P, BNK_PD, BOOT)
    return img

def banker_walk(frame, direction):
    img = new(16, 24)
    dy  = 1 if frame in (1, 3) else 0
    if direction == 's':
        head_south(img, 1 + dy, SKIN, SKIN_D, HAIR, EYE)
        torso_south(img, 7 + dy, BNK_S, BNK_SD, BELT, SKIN)
        legs_south(img, frame, BNK_P, BNK_PD, BOOT)
    elif direction == 'n':
        head_north(img, 1 + dy, HAIR)
        torso_north(img, 7 + dy, BNK_S, BNK_SD, BELT, SKIN)
        legs_north(img, frame, BNK_P, BNK_PD, BOOT)
    elif direction == 'e':
        head_east(img, 1 + dy, SKIN, SKIN_D, HAIR, EYE)
        torso_east(img, 7 + dy, BNK_S, BNK_SD, BELT, SKIN)
        legs_east(img, frame, BNK_P, BNK_PD, BOOT)
    else:   # 'w'
        head_west(img, 1 + dy, SKIN, SKIN_D, HAIR, EYE)
        torso_west(img, 7 + dy, BNK_S, BNK_SD, BELT, SKIN)
        legs_west(img, frame, BNK_P, BNK_PD, BOOT)
    return img

# ═══════════════════════════ NPC Goblin action ═════════════════════════════
#
# Goblin is squat: head starts at y=4 (1 extra clear row at top).
# Layout: head y=4-9, cloth y=10-15, loincloth y=16-17, legs y=18-20, boots y=21-22.

def goblin_action(frame):
    """16×24. Frame 0: dagger raised. Frame 1: striking forward."""
    img = new(16, 24)
    hy  = 4                                     # head top fixed for both frames

    # Head (green, no hair — bald goblin with pointed ears)
    hline(img,  5, 10, hy,     GOB_SK)          # skull top
    rect(img,   4, hy + 1, 8, 3, GOB_SK)        # face
    px(img,  3, hy + 1, GOB_SD)                 # ear hints / shadow
    px(img, 12, hy + 1, GOB_SD)
    px(img,  5, hy + 2, GOB_EY)                 # left eye
    px(img, 10, hy + 2, GOB_EY)
    hline(img,  5, 10, hy + 4, GOB_SK)          # chin

    # Ragged cloth body y=9-15
    rect(img,  7, hy + 5, 2, 1, GOB_SK)         # neck
    rect(img,  4, hy + 6, 8, 5, GOB_RG)         # cloth torso
    vline(img, 11, hy + 6, hy + 10, GOB_RD)

    # Loincloth / hip y=16-17
    hline(img,  5, 10, 16, GOB_RG)
    hline(img,  5, 10, 17, GOB_RD)

    # Legs (bare green, y=18-20)
    rect(img,  4, 18, 3, 3, GOB_SK)             # left leg
    vline(img, 6, 18, 20, GOB_SD)
    rect(img,  9, 18, 3, 3, GOB_SK)             # right leg
    vline(img, 11, 18, 20, GOB_SD)

    # Boots y=21-22
    rect(img,  3, 21, 5, 2, BOOT)
    rect(img,  8, 21, 5, 2, BOOT)

    # Weapon (short sword / dagger) — right-side
    if frame == 0:                              # blade raised
        vline(img, 13,  3,  9, BOOT)            # handle + pommel
        rect(img, 14,  2, 2, 2, AXE)            # blade tip
        hline(img, 12, 15,  9, AXE)             # crossguard
    else:                                       # striking — body leans
        vline(img, 13,  9, 15, BOOT)
        rect(img, 14, 14, 2, 2, AXE)
        hline(img, 12, 15, 15, AXE)
        # Forward lean: shift arm pixel
        px(img,  2, 10, GOB_RG)
        px(img,  2, 11, GOB_RG)
    return img

# ═══════════════════════════ Fire idle (16×20) ═════════════════════════════
#
# Bottom-center pivot. Bottom 2 rows = ember base (always fixed).
# Flame flickers by shifting tongue positions 1px between frames.

def fire_idle(frame):
    img = new(16, 20)

    # Ember base rows y=17-19 (always same)
    hline(img,  6, 10, 19, FR_BSE)
    hline(img,  5, 11, 18, FR_BSE)
    hline(img,  5, 10, 17, FR_LO)

    if frame == 0:
        # Lower body y=10-16
        rect(img,  6, 10, 4, 7, FR_LO)
        # Mid flame y=6-10
        rect(img,  7,  6, 2, 5, FR_MID)
        # Side tongues
        px(img,  5, 12, FR_LO);  px(img,  4, 13, FR_LO)
        px(img, 11, 12, FR_LO);  px(img, 12, 13, FR_LO)
        # Tip
        px(img,  7,  4, FR_TIP); px(img,  8,  4, FR_TIP)
        px(img,  8,  3, FR_TIP)
        # Inner glow points
        px(img,  6,  8, FR_GLW); px(img,  9,  8, FR_GLW)
    else:
        # Flame shifted 1px right, tip nudged
        rect(img,  7, 10, 4, 7, FR_LO)
        rect(img,  7,  6, 3, 5, FR_MID)
        # Side tongues (shifted)
        px(img,  6, 12, FR_LO);  px(img,  5, 13, FR_LO)
        px(img, 12, 12, FR_LO);  px(img, 11, 13, FR_LO)
        px(img,  8,  4, FR_TIP); px(img,  9,  4, FR_TIP)
        px(img,  8,  3, FR_TIP)
        px(img,  7,  8, FR_GLW); px(img, 10,  9, FR_GLW)
    return img

# ═══════════════ Fishing spot idle (32×16) ════════════════════════════════
#
# Tile-sized (32×16), bottom-center pivot.
# Shows a patch of water with a ripple ring and bobber.
# Two frames = ring expands / contracts to imply lapping water.

def fishing_spot_idle(frame):
    img = new(32, 16)

    # Water fill (lower half)
    rect(img,  6, 6, 20, 10, WAT)
    rect(img,  8, 8, 16,  7, WAT_L)     # brighter centre

    if frame == 0:
        # Ripple ring — wider
        hline(img,  9, 22,  6, WAT_F)   # top edge
        hline(img,  8, 23, 14, WAT_F)   # bottom edge
        vline(img,  7,  8, 12, WAT_F)   # left edge
        vline(img, 24,  8, 12, WAT_F)   # right edge
        # Inner dark ring
        hline(img, 12, 19,  8, WAT_D)
        hline(img, 12, 19, 13, WAT_D)
        # Bobber: floating at y=4-7 (above water surface)
        rect(img, 15,  4, 2, 4, BOB_R)
        vline(img, 16,  8, 10, BOB_L)   # line dipping in
    else:
        # Ripple ring — narrower (contracted)
        hline(img, 10, 21,  7, WAT_F)
        hline(img,  9, 22, 13, WAT_F)
        vline(img,  8,  9, 11, WAT_F)
        vline(img, 23,  9, 11, WAT_F)
        hline(img, 13, 18,  9, WAT_D)
        hline(img, 13, 18, 12, WAT_D)
        # Bobber dips 1px
        rect(img, 15,  5, 2, 4, BOB_R)
        vline(img, 16,  9, 10, BOB_L)
    return img

# ═════════════════════════════ Main ════════════════════════════════════════
if __name__ == "__main__":
    print("Pack 5: animation frames")

    # ── Player idle (2 frames)
    for f in range(2):
        save(f"player_idle_{f}.png", player_idle(f))

    # ── Player walk 4 directions × 4 frames = 16 files
    for d, fn in (('n', player_walk_north), ('s', player_walk_south),
                  ('e', player_walk_east),  ('w', player_walk_west)):
        for f in range(4):
            save(f"player_walk_{d}_{f}.png", fn(f))

    # ── Player chop (2 frames)
    for f in range(2):
        save(f"player_chop_{f}.png", player_chop(f))

    # ── Ambient: fire (2 frames)
    for f in range(2):
        save(f"fire_idle_{f}.png", fire_idle(f))

    # ── Ambient: fishing spot (2 frames)
    for f in range(2):
        save(f"fishing_spot_idle_{f}.png", fishing_spot_idle(f))

    # ── NPC Banker idle (2 frames)
    for f in range(2):
        save(f"npc_banker_idle_{f}.png", banker_idle(f))

    # ── NPC Banker walk 4 directions × 4 frames = 16 files
    for d in ('n', 's', 'e', 'w'):
        for f in range(4):
            save(f"npc_banker_walk_{d}_{f}.png", banker_walk(f, d))

    # ── NPC Goblin action (2 frames)
    for f in range(2):
        save(f"npc_goblin_action_{f}.png", goblin_action(f))

    total = 2 + 16 + 2 + 2 + 2 + 2 + 16 + 2
    print(f"\nDone — {total} files written to {OUT}/")
