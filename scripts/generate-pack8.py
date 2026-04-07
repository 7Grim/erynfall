#!/usr/bin/env python3
"""
generate-pack8.py — Pack 8: NPC action animations + ambient resource idles

Part A — NPC actions (all 2-frame, strong readable key poses):
  npc_rat_action_0/1.png         (16×12, east-facing lunge/bite)
  npc_giant_rat_action_0/1.png   (24×20, east-facing lunge/bite)
  npc_chicken_action_0/1.png     (16×12, east-facing peck/flutter)
  npc_cow_action_0/1.png         (24×20, east-facing head-lower charge)
  npc_banker_action_0/1.png      (16×24, south-facing hand-raise service)
  npc_guide_action_0/1.png       (16×24, south-facing pointing gesture)
  npc_instructor_action_0/1.png  (16×24, south-facing combat-ready stance)

Part B — Resource idles (all 2-frame, near-static):
  tree_idle_0/1.png              (16×32, canopy tip sways 1px)
  tree_oak_idle_0/1.png          (16×32, same motion, oak palette)
  tree_willow_idle_0/1.png       (16×40, slightly more rows sway)
  rock_gold_idle_0/1.png         (16×12, bright ore glint shifts 1px right)
  rock_runite_idle_0/1.png       (16×12, same glint, teal ore)
  clutter_reeds_1_idle_0/1.png   (32×16, reed tips sway 2px total)

Total: 26 files.

Usage:  python3 scripts/generate-pack8.py
Requires: Pillow
"""
from PIL import Image
import os

OUT = os.path.join(os.path.dirname(__file__), "..", "art", "sprites")
os.makedirs(OUT, exist_ok=True)

# ═══════════════════════════ Palette ═══════════════════════════════════════
T = (0, 0, 0, 0)

# Shared humanoid
SKIN   = (208, 152, 104, 255)
SKIN_D = (160, 104,  64, 255)
HAIR   = ( 56,  36,  16, 255)
BOOT   = ( 36,  24,  12, 255)
BELT   = ( 88,  60,  24, 255)
EYE    = ( 40,  28,  20, 255)
AXE    = (160, 160, 168, 255)   # generic metal grey (reuse for any weapon hint)

# Banker (gold shirt, dark-blue pants)
BNK_S  = (172, 136,  72, 255)
BNK_SD = (120,  92,  44, 255)
BNK_P  = ( 52,  44,  76, 255)
BNK_PD = ( 36,  28,  56, 255)

# Guide (olive-green robe)
GD_ROB  = (100, 116,  64, 255)
GD_ROBD = ( 68,  80,  40, 255)

# Instructor (red padded jacket)
IN_ARM  = (140,  44,  32, 255)
IN_ARMD = ( 92,  28,  20, 255)

# Rat
RAT_B  = (128,  96,  72, 255)
RAT_D  = ( 88,  64,  48, 255)
RAT_L  = (168, 136, 112, 255)
RAT_T  = (100,  72,  52, 255)
RAT_EY = ( 20,  12,   8, 255)
RAT_NS = (200, 140, 140, 255)

# Giant rat
GR_B  = (108,  76,  52, 255)
GR_D  = ( 72,  48,  32, 255)
GR_L  = (148, 112,  80, 255)
GR_T  = ( 84,  56,  36, 255)
GR_EY = (200,  24,  24, 255)
GR_NS = (180, 120, 120, 255)

# Chicken
CH_W   = (220, 208, 180, 255)
CH_D   = (176, 164, 136, 255)
CH_CRD = (208,  48,  48, 255)
CH_BEK = (220, 160,  40, 255)
CH_EY  = ( 24,  16,  12, 255)

# Cow
CW_W  = (232, 228, 220, 255)
CW_P  = ( 44,  36,  28, 255)
CW_MZ = (224, 192, 176, 255)
CW_HF = ( 40,  32,  20, 255)
CW_HN = (204, 188, 156, 255)
CW_EY = ( 20,  12,   8, 255)
CW_SH = (196, 192, 184, 255)   # pulled-back leg shadow

# Trees — base
TR_H  = ( 62, 158,  52, 200)   # highlight (top canopy)
TR_L  = ( 38, 128,  38, 255)   # base leaf
TR_TD = ( 82,  46,  10, 255)   # trunk dark
TR_T  = (102,  66,  30, 255)   # trunk light

# Trees — oak
OAK_H  = ( 72, 145,  50, 200)
OAK_L  = ( 52, 118,  34, 255)
OAK_TD = ( 68,  35,   2, 255)
OAK_T  = ( 88,  55,  22, 255)

# Trees — willow
WIL_H  = ( 82, 162,  65, 200)
WIL_L  = ( 58, 138,  48, 255)
WIL_TD = ( 58,  30,   0, 255)
WIL_T  = ( 78,  50,  14, 255)

# Rocks
ROCK_L  = (135, 128, 122, 255)   # light grey
ROCK_M  = ( 98,  93,  88, 255)   # mid grey
ROCK_D  = ( 55,  50,  46, 255)   # dark grey

GOLD    = (240, 202,  25, 255)   # ore dim
GOLD2   = (255, 242,  65, 255)   # ore bright (glint)

RUN     = ( 28, 180, 182, 255)   # runite dim
RUN2    = ( 68, 220, 222, 255)   # runite bright

# Reeds
REED    = ( 44,  66,  28, 200)
REED_TIP= ( 44,  66,  28, 118)
REED_BSE= ( 44,  66,  28, 145)

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

# ═══════════════════════════ Humanoid helpers ══════════════════════════════
# Identical to Pack 5 / generate-animations.py

def head_south(img, hy, skin=SKIN, skin_d=SKIN_D, hair=HAIR, eye=EYE):
    hline(img,  4, 11, hy, hair)
    px(img,  3, hy+1, hair);  px(img, 12, hy+1, hair)
    rect(img, 4, hy+1, 8, 4, skin)
    px(img,  5, hy+3, eye);   px(img, 10, hy+3, eye)
    hline(img,  5, 10, hy+5, skin)
    px(img,  4, hy+5, skin_d); px(img, 11, hy+5, skin_d)

def torso_south(img, ty, shirt, shirt_d, belt=BELT, skin=SKIN):
    rect(img,  7, ty, 2, 1, skin)
    rect(img,  3, ty+1, 10, 6, shirt)
    vline(img, 12, ty+1, ty+6, shirt_d)
    hline(img,  3, 12, ty+7, belt)

def legs_south(img, pnt, pnt_d, boot=BOOT):
    """Neutral legs (frame=0)."""
    rect(img,  3, 15, 4, 6, pnt)
    rect(img,  9, 15, 4, 6, pnt)
    rect(img,  3, 21, 5, 2, boot)
    rect(img,  8, 21, 5, 2, boot)

# ═══════════════════════════ Rat body components ═══════════════════════════
# Reused from generate-npc-animations.py (east-facing reference).

def _rat_body_e(img):
    """Standard east-facing rat body (no legs)."""
    px(img, 0, 2, RAT_T);  px(img, 1, 2, RAT_T)
    px(img, 1, 3, RAT_T);  px(img, 2, 3, RAT_T)
    hline(img, 2,  8, 2, RAT_L)
    hline(img, 2, 11, 3, RAT_B)
    hline(img, 2, 11, 4, RAT_B)
    hline(img, 2, 10, 5, RAT_D)
    hline(img,  9, 13, 2, RAT_B)
    hline(img, 10, 14, 3, RAT_B)
    hline(img,  9, 13, 4, RAT_B)
    hline(img,  9, 12, 5, RAT_B)
    px(img, 12, 3, RAT_EY)
    px(img, 14, 3, RAT_NS)
    px(img, 13, 4, RAT_NS)
    px(img, 10, 1, RAT_D)

def _rat_body_e_lunge(img):
    """East-facing rat body with head 1px forward (lunge/bite frame)."""
    # Tail + back highlight — unchanged
    px(img, 0, 2, RAT_T);  px(img, 1, 2, RAT_T)
    px(img, 1, 3, RAT_T);  px(img, 2, 3, RAT_T)
    hline(img, 2,  8, 2, RAT_L)
    # Main body — unchanged
    hline(img, 2, 11, 3, RAT_B)
    hline(img, 2, 11, 4, RAT_B)
    hline(img, 2, 10, 5, RAT_D)
    # Head shifted +1 right: was x=9..14, now x=10..15
    hline(img, 10, 14, 2, RAT_B)
    hline(img, 11, 15, 3, RAT_B)
    hline(img, 10, 14, 4, RAT_B)
    hline(img, 10, 13, 5, RAT_B)
    px(img, 13, 3, RAT_EY)
    px(img, 15, 3, RAT_NS)
    px(img, 14, 4, RAT_NS)
    # Lower jaw open: dark pixel below nose = visible bite
    px(img, 14, 5, RAT_D)
    px(img, 10, 1, RAT_D)

def _rat_legs(img, extended=False):
    """East-facing rat legs. extended=True = front legs reaching forward."""
    if not extended:
        rect(img, 4, 6, 2, 4, RAT_B)
        rect(img, 9, 6, 2, 4, RAT_B)
    else:
        rect(img, 3, 6, 2, 5, RAT_B)   # front legs stretched forward
        rect(img, 10, 6, 2, 5, RAT_B)

# ═══════════════════════════ Giant rat body components ═════════════════════

def _gr_body_e(img):
    """Standard east-facing giant rat body (no legs)."""
    px(img, 0, 3, GR_T);  px(img, 1, 3, GR_T)
    px(img, 1, 4, GR_T);  px(img, 2, 4, GR_T);  px(img, 3, 5, GR_T)
    hline(img,  3, 13, 3, GR_L)
    hline(img,  3, 17, 4, GR_B)
    hline(img,  3, 17, 5, GR_B)
    hline(img,  3, 17, 6, GR_B)
    hline(img,  3, 16, 7, GR_B)
    hline(img,  4, 15, 8, GR_D)
    hline(img,  5, 14, 9, GR_D)
    hline(img, 14, 21, 3, GR_B)
    hline(img, 15, 22, 4, GR_B)
    hline(img, 15, 22, 5, GR_B)
    hline(img, 14, 21, 6, GR_B)
    hline(img, 14, 20, 7, GR_B)
    px(img, 16, 2, GR_D);  px(img, 17, 2, GR_D)
    px(img, 16, 1, GR_B)
    px(img, 20, 4, GR_EY);  px(img, 21, 4, GR_EY)
    px(img, 22, 5, GR_NS);  px(img, 23, 5, GR_NS)
    px(img, 22, 6, GR_NS)
    px(img, 22, 7, (232, 224, 208, 255))

def _gr_body_e_lunge(img):
    """Giant rat east body with head lunged +1px right."""
    # Tail + back — unchanged
    px(img, 0, 3, GR_T);  px(img, 1, 3, GR_T)
    px(img, 1, 4, GR_T);  px(img, 2, 4, GR_T);  px(img, 3, 5, GR_T)
    hline(img,  3, 13, 3, GR_L)
    # Main body — unchanged
    hline(img,  3, 17, 4, GR_B)
    hline(img,  3, 17, 5, GR_B)
    hline(img,  3, 17, 6, GR_B)
    hline(img,  3, 16, 7, GR_B)
    hline(img,  4, 15, 8, GR_D)
    hline(img,  5, 14, 9, GR_D)
    # Head shifted +1 right
    hline(img, 15, 22, 3, GR_B)
    hline(img, 16, 23, 4, GR_B)
    hline(img, 16, 23, 5, GR_B)
    hline(img, 15, 22, 6, GR_B)
    hline(img, 15, 21, 7, GR_B)
    px(img, 17, 2, GR_D);  px(img, 18, 2, GR_D)   # ear
    px(img, 17, 1, GR_B)
    px(img, 21, 4, GR_EY);  px(img, 22, 4, GR_EY)
    px(img, 23, 5, GR_NS)    # nose at edge
    px(img, 23, 6, GR_NS)
    # Open jaw
    px(img, 22, 7, GR_D)

def _gr_legs(img, extended=False):
    if not extended:
        rect(img,  7, 10, 3, 8, GR_B)
        rect(img, 15, 10, 3, 8, GR_B)
    else:
        rect(img,  5, 10, 3, 9, GR_B)   # front stretched forward
        rect(img, 16, 10, 3, 9, GR_B)

# ═══════════════════════════ Chicken body components ═══════════════════════

def _ch_body_e(img, head_dx=0, wings_spread=False):
    """East-facing chicken. wings_spread adds raised wing-tip pixels."""
    if wings_spread:
        # Wing tips raised above body
        px(img, 0, 1, CH_D);   px(img, 1, 0, CH_W)
        px(img, 0, 2, CH_W)
    # Tail feathers (standard)
    px(img, 0, 2, CH_D);  px(img, 1, 1, CH_W)
    px(img, 1, 2, CH_W);  px(img, 2, 2, CH_W)
    px(img, 0, 3, CH_W);  px(img, 1, 3, CH_W)
    # Body oval
    hline(img, 2,  9, 2, CH_W)
    hline(img, 2, 10, 3, CH_W)
    hline(img, 2, 10, 4, CH_W)
    hline(img, 2, 10, 5, CH_W)
    hline(img, 3,  9, 6, CH_D)
    hline(img, 3,  8, 4, CH_D)   # wing texture
    # Head (with optional shift)
    hx = 9 + head_dx
    hline(img, hx, hx+3, 1, CH_W)
    px(img, hx,   0, CH_CRD)
    px(img, hx+1, 0, CH_CRD)
    hline(img, hx, hx+4, 2, CH_W)
    hline(img, hx, hx+4, 3, CH_W)
    hline(img, hx, hx+3, 4, CH_W)
    px(img, hx+1, 5, CH_CRD)     # wattle
    px(img, hx+5, 3, CH_BEK)     # beak
    px(img, hx+2, 2, CH_EY)

def _ch_legs_e(img):
    rect(img, 6, 7, 1, 3, CH_BEK)
    rect(img, 8, 7, 1, 3, CH_BEK)
    px(img, 5, 10, CH_BEK);  px(img, 6, 10, CH_BEK);  px(img, 7, 10, CH_BEK)
    px(img, 7, 10, CH_BEK);  px(img, 8, 10, CH_BEK);  px(img, 9, 10, CH_BEK)

# ═══════════════════════════ Cow body components ═══════════════════════════

def _cow_body_e(img, head_dy=0):
    """East-facing cow body. head_dy shifts head elements down (for charge)."""
    # Tail
    vline(img,  1, 2, 7, CW_W)
    px(img, 0, 3, CW_W);  px(img, 0, 4, CW_W)
    # Main body (unchanged regardless of head_dy)
    hline(img,  2, 17, 3, CW_W)
    hline(img,  2, 17, 4, CW_W)
    hline(img,  2, 17, 5, CW_W)
    hline(img,  2, 17, 6, CW_W)
    hline(img,  2, 17, 7, CW_W)
    hline(img,  2, 16, 8, CW_W)
    hline(img,  3, 15, 9, CW_W)
    rect(img,  5, 4, 3, 3, CW_P)
    rect(img, 10, 5, 4, 2, CW_P)
    rect(img,  8, 8, 3, 2, CW_P)
    rect(img,  8, 10, 6, 3, CW_MZ)
    hline(img,  9, 12, 13, CW_MZ)
    hline(img,  4, 14, 9, (200, 196, 188, 255))
    # Head elements — shifted by head_dy
    hy = head_dy
    hline(img, 14, 21, 3 + hy, CW_W)
    hline(img, 14, 22, 4 + hy, CW_W)
    hline(img, 14, 23, 5 + hy, CW_W)
    hline(img, 14, 23, 6 + hy, CW_W)
    hline(img, 14, 22, 7 + hy, CW_W)
    hline(img, 15, 21, 8 + hy, CW_W)
    # Horns (shift with head; at head_dy=0 they're above head, at head_dy=2 they're mid-head)
    px(img, 17, 1 + hy, CW_HN);  px(img, 18, 1 + hy, CW_HN);  px(img, 19, 1 + hy, CW_HN)
    px(img, 17, 2 + hy, CW_HN);  px(img, 20, 2 + hy, CW_HN)
    px(img, 22, 3 + hy, CW_W);   px(img, 23, 4 + hy, CW_W)    # ear
    rect(img, 15, 6 + hy, 5, 3, CW_MZ)                          # muzzle
    px(img, 20, 4 + hy, CW_EY);  px(img, 21, 4 + hy, CW_EY)   # eye
    px(img, 16, 7 + hy, CW_P);   px(img, 18, 7 + hy, CW_P)    # nostrils

def _cow_legs_e(img, extended=False):
    """East-facing cow legs. extended=True = front pair reaches forward."""
    if not extended:
        rect(img,  6, 12, 3, 6, CW_W);   rect(img, 13, 12, 3, 6, CW_W)
        rect(img,  6, 18, 3, 2, CW_HF);  rect(img, 13, 18, 3, 2, CW_HF)
    else:
        rect(img,  4, 12, 3, 7, CW_W);   rect(img, 15, 12, 3, 7, CW_W)
        rect(img,  4, 19, 3, 1, CW_HF);  rect(img, 15, 19, 3, 1, CW_HF)
        rect(img,  8, 12, 2, 5, CW_SH);  rect(img, 11, 12, 2, 5, CW_SH)

# ═══════════════════════════ NPC actions ═══════════════════════════════════

def npc_rat_action(frame):
    """
    East-facing lunge/bite. Two key poses:
    0 = pre-lunge — front legs stretched out, body poised (same head as idle)
    1 = strike    — head surges 1px forward, jaws suggest bite
    """
    img = new(16, 12)
    if frame == 0:
        _rat_body_e(img)
        _rat_legs(img, extended=True)
    else:
        _rat_body_e_lunge(img)
        _rat_legs(img, extended=False)
    return img

def npc_giant_rat_action(frame):
    """
    East-facing heavy lunge. Same two-pose structure as rat, larger canvas.
    0 = coil/ready (legs extended forward)
    1 = strike (head +1px, open jaw)
    """
    img = new(24, 20)
    if frame == 0:
        _gr_body_e(img)
        _gr_legs(img, extended=True)
    else:
        _gr_body_e_lunge(img)
        _gr_legs(img, extended=False)
    return img

def npc_chicken_action(frame):
    """
    East-facing peck/flutter.
    0 = wings spread, head normal (startled)
    1 = head jabbed +1px forward, beak at canvas edge (peck strike)
    """
    img = new(16, 12)
    if frame == 0:
        _ch_body_e(img, head_dx=0, wings_spread=True)
        _ch_legs_e(img)
    else:
        _ch_body_e(img, head_dx=1, wings_spread=False)
        _ch_legs_e(img)
    return img

def npc_cow_action(frame):
    """
    East-facing head-lower horn charge.
    0 = normal pose (head at standard height, same as idle)
    1 = head shifted down 2px — horns now horizontal, muzzle lowered
    """
    img = new(24, 20)
    _cow_body_e(img, head_dy=0 if frame == 0 else 2)
    _cow_legs_e(img, extended=(frame == 0))
    return img

# ─── Humanoid actions ──────────────────────────────────────────────────────
#
# All south-facing (frontal). Arm gesture drawn as pixels OUTSIDE the shirt
# block (shirt ends at x=12; arm/hand at x=13-15 on right side).
# Belt = standard humanoid, legs neutral, boots fixed at y=21-22.
#
# Arm anatomy (right side of character = viewer's left):
#   x=13 → sleeve colour (shirt extending from shoulder)
#   x=14 → forearm / hand (SKIN)
#   x=15 → pointing fingertip (SKIN, optional)
# Vertical position tracks intent:
#   y≈9-11 → arm resting low / by-side
#   y≈7-9  → arm raised to mid-chest / pointing forward
#   y≈5-7  → arm raised to shoulder / waving

def npc_banker_action(frame):
    """
    Calm, non-combat service gesture. Right hand raised acknowledging.
    0 = hand at mid-chest, palm outward (gentle 'please approach')
    1 = hand slightly raised (friendly acknowledgement)
    """
    img = new(16, 24)
    dy = 0                  # no body bob — calm, stable
    head_south(img, 1, SKIN, SKIN_D, HAIR, EYE)
    torso_south(img, 7, BNK_S, BNK_SD)
    legs_south(img, BNK_P, BNK_PD)

    if frame == 0:
        # Right sleeve at shoulder level then dropping to mid-chest
        px(img, 13, 8,  BNK_S)
        px(img, 13, 9,  BNK_S)
        px(img, 14, 9,  SKIN)     # hand at mid-chest height
        px(img, 14, 10, SKIN)
    else:
        # Hand raised to just below shoulder
        px(img, 13, 7,  BNK_S)
        px(img, 13, 8,  BNK_S)
        px(img, 14, 7,  SKIN)     # hand raised
    return img

def npc_guide_action(frame):
    """
    Small instructive pointing gesture. Right arm extended, fingertip visible.
    0 = arm at mid-chest pointing diagonally forward
    1 = arm at slightly higher angle (pointing upward/forward)
    """
    img = new(16, 24)
    head_south(img, 1, SKIN, SKIN_D, HAIR, EYE)
    torso_south(img, 7, GD_ROB, GD_ROBD)
    legs_south(img, GD_ROB, GD_ROBD)

    if frame == 0:
        px(img, 13, 9,  GD_ROB)
        px(img, 14, 9,  SKIN)
        px(img, 14, 10, SKIN)    # arm angled slightly down = pointing out/at player
        px(img, 15, 10, SKIN)    # fingertip
    else:
        px(img, 13, 8,  GD_ROB)
        px(img, 14, 7,  SKIN)
        px(img, 14, 8,  SKIN)    # arm raised
        px(img, 15, 7,  SKIN)    # fingertip now pointing upward
    return img

def npc_instructor_action(frame):
    """
    Demonstrative combat pose. Slightly more assertive than guide/banker.
    0 = guard stance — both arms loosely raised, weight slightly forward
    1 = right arm extended forward (punch/demonstrate strike)
    """
    img = new(16, 24)
    head_south(img, 1, SKIN, SKIN_D, HAIR, EYE)
    torso_south(img, 7, IN_ARM, IN_ARMD)
    legs_south(img, IN_ARM, IN_ARMD)

    if frame == 0:
        # Both arms raised at guard level
        px(img,  2, 9,  SKIN)    # left arm (viewer right)
        px(img,  2, 10, SKIN)
        px(img, 13, 9,  IN_ARM)
        px(img, 13, 10, IN_ARM)
        px(img, 14, 9,  SKIN)    # right fist
    else:
        # Right arm drives forward — extend 2 pixels past shirt
        px(img,  2, 10, SKIN)    # left guard arm (lower)
        px(img, 13, 8,  IN_ARM)
        px(img, 14, 8,  SKIN)
        px(img, 15, 8,  SKIN)    # fully extended right arm punch
    return img

# ═══════════════════════════ Tree idle (sway) ══════════════════════════════
#
# Frame 0 = faithful reconstruction of static sprite.
# Frame 1 = top rows of canopy shifted 1px LEFT (western sway).
# Trunk rows: NEVER shifted (guarantees pivot stability).
#
# Sway boundary: rows y=0..highlight_end (the lighter, upper highlight region).
# Below that boundary, the denser base-leaf mass stays fixed — looks natural.

def _draw_tree_canopy(img, hi_color, lo_color, sway_rows=0, dx=-1):
    """
    Draw a 16px-wide conical canopy on `img`.

    hi_color applies to rows y=0..12 (upper highlight mass).
    lo_color applies to rows y=13..24 (lower base-leaf mass).
    sway_rows: how many rows from the top to offset by dx pixels.
    """
    # Upper highlight (y=0..12)
    hi_ranges = [
        (0,  8,  8),    # y=0:  single pixel at x=8
        (1,  8,  8),    # y=1
        (2,  7,  9),    # y=2..4
        (3,  7,  9),
        (4,  7,  9),
        (5,  6, 10),    # y=5..7
        (6,  6, 10),
        (7,  6, 10),
        (8,  5, 11),    # y=8..10
        (9,  5, 11),
        (10, 5, 11),
        (11, 4, 12),    # y=11..12
        (12, 4, 12),
    ]
    for (y, x1, x2) in hi_ranges:
        if y < sway_rows:
            hline(img, x1 + dx, x2 + dx, y, hi_color)
        else:
            hline(img, x1, x2, y, hi_color)

    # Lower leaf mass (y=13..24)
    lo_ranges = [
        (13, 4, 11),    # note: x2=11 not 12 (slightly tapered left)
        (14, 4, 11),
        (15, 4, 12),
        (16, 3, 12),
        (17, 3, 12),
        (18, 3, 12),
        (19, 2, 13),
        (20, 2, 13),
        (21, 2, 13),
        (22, 2, 13),
        (23, 1, 14),
        (24, 1, 14),
    ]
    for (y, x1, x2) in lo_ranges:
        hline(img, x1, x2, y, lo_color)

def _draw_trunk(img, trunk_y, td_color, t_color):
    """Draw 6px-wide trunk at x=5..10, from trunk_y to bottom of canvas."""
    for y in range(trunk_y, img.height):
        px(img, 5, y, td_color);  px(img, 6, y, td_color)
        px(img, 7, y, t_color);   px(img, 8, y, t_color)
        px(img, 9, y, t_color);   px(img, 10, y, t_color)

def tree_idle(frame):
    """16×32 base tree. frame 1 sways top 13 rows left by 1px."""
    img = new(16, 32)
    sway = 13 if frame == 1 else 0
    _draw_tree_canopy(img, TR_H, TR_L, sway_rows=sway)
    _draw_trunk(img, 25, TR_TD, TR_T)
    return img

def tree_oak_idle(frame):
    """16×32 oak tree. Same motion profile as base tree."""
    img = new(16, 32)
    sway = 13 if frame == 1 else 0
    _draw_tree_canopy(img, OAK_H, OAK_L, sway_rows=sway)
    _draw_trunk(img, 25, OAK_TD, OAK_T)
    return img

def tree_willow_idle(frame):
    """
    16×40 willow. Canopy is 31 rows, trunk is 9 rows (y=31..39).
    Willow sways 20 rows (more than other trees — hanging fronds respond more).
    """
    img = new(16, 40)

    # Willow canopy pixel ranges (from sprite inspection)
    # Upper highlight (WIL_H, y=0..15)
    wil_hi = [
        (0,  8,  8),
        (1,  8,  8),
        (2,  7,  9),
        (3,  7,  9),
        (4,  7,  9),
        (5,  7,  9),
        (6,  6, 10),
        (7,  6, 10),
        (8,  6, 10),
        (9,  6, 10),
        (10, 5, 11),
        (11, 5, 11),
        (12, 5, 11),
        (13, 5, 11),
        (14, 4, 12),
        (15, 4, 12),
    ]
    # Lower leaf mass (WIL_L, y=16..30)
    wil_lo = [
        (16, 4, 11),
        (17, 4, 11),
        (18, 4, 12),
        (19, 4, 12),
        (20, 3, 12),
        (21, 3, 12),
        (22, 3, 12),
        (23, 3, 13),
        (24, 2, 13),
        (25, 2, 13),
        (26, 2, 13),
        (27, 2, 13),
        (28, 1, 14),
        (29, 1, 14),
        (30, 1, 14),
    ]

    sway_limit = 20 if frame == 1 else 0   # more rows sway for willow
    dx = -1

    for (y, x1, x2) in wil_hi:
        if y < sway_limit:
            hline(img, x1 + dx, x2 + dx, y, WIL_H)
        else:
            hline(img, x1, x2, y, WIL_H)

    for (y, x1, x2) in wil_lo:
        if y < sway_limit:
            hline(img, x1 + dx, x2 + dx, y, WIL_L)
        else:
            hline(img, x1, x2, y, WIL_L)

    # Trunk y=31..39
    _draw_trunk(img, 31, WIL_TD, WIL_T)
    return img

# ═══════════════════════════ Rock glint idles ══════════════════════════════
#
# Rock silhouette is identical in both frames — only the bright ore highlight
# shifts 1px right in frame 1. This reads as a mineral catch-light flicker.
#
# Ore layout (shared rock shape):
#   y=4: ORE_DIM at x=6..9
#   y=5: ORE_DIM at x=5,x=10 | ORE_BRIGHT at x=6..9
#   y=6: ORE_DIM at x=4,x=11 | ORE_BRIGHT at x=5..10
#   y=7: ORE_DIM at x=5,x=10 | ORE_BRIGHT at x=6..9
#   y=8: ORE_DIM at x=6..9

def _draw_rock_base(img):
    """Grey rock dome — identical for both frames."""
    # Top dome
    px(img,   8,  0, ROCK_L)
    hline(img, 6,  9,  1, ROCK_L)
    hline(img, 5, 11,  2, ROCK_L)
    hline(img, 3, 12,  3, ROCK_L)
    # Upper ore ring (just light grey outside ore)
    hline(img, 2, 5, 4, ROCK_L);  hline(img, 10, 14, 4, ROCK_L)
    hline(img, 0, 4, 5, ROCK_L);  hline(img, 11, 15, 5, ROCK_L)
    # Mid shadow ring
    hline(img, 1, 3, 6, ROCK_M);  hline(img, 12, 14, 6, ROCK_M)
    hline(img, 1, 4, 7, ROCK_M);  hline(img, 11, 14, 7, ROCK_M)
    hline(img, 2, 5, 8, ROCK_M);  hline(img, 10, 13, 8, ROCK_M)
    # Dark base
    px(img,   2,  9, ROCK_M)
    hline(img, 3, 12,  9, ROCK_D)
    px(img,  13,  9, ROCK_M)
    hline(img, 3, 12, 10, ROCK_D)
    hline(img, 3, 12, 11, ROCK_D)

def _draw_ore(img, ore_dim, ore_bright, shift=0):
    """
    Draw ore highlights. shift=0 → centred, shift=1 → 1px right (glint frame).
    Clamped so no ore pixels exceed the surrounding grey ring.
    """
    s = shift
    # Row y=4: dim ring
    hline(img, 6 + s, 9 + s, 4, ore_dim)
    # Row y=5: dim edges + bright centre
    px(img, 5 + s, 5, ore_dim);   px(img, 10 + s, 5, ore_dim)
    hline(img, 6 + s,  9 + s, 5, ore_bright)
    # Row y=6: dim edges + bright centre (wider)
    px(img, 4 + s, 6, ore_dim);   px(img, 11 + s, 6, ore_dim)
    hline(img, 5 + s, 10 + s, 6, ore_bright)
    # Row y=7: same as y=5
    px(img, 5 + s, 7, ore_dim);   px(img, 10 + s, 7, ore_dim)
    hline(img, 6 + s,  9 + s, 7, ore_bright)
    # Row y=8: dim ring
    hline(img, 6 + s,  9 + s, 8, ore_dim)

def rock_gold_idle(frame):
    img = new(16, 12)
    _draw_rock_base(img)
    _draw_ore(img, GOLD, GOLD2, shift=(1 if frame == 1 else 0))
    return img

def rock_runite_idle(frame):
    img = new(16, 12)
    _draw_rock_base(img)
    _draw_ore(img, RUN, RUN2, shift=(1 if frame == 1 else 0))
    return img

# ═══════════════════════════ Reed idle (32×16 tile) ════════════════════════
#
# Two reeds on a 32×16 iso tile, anchored at y=6..8.
# Tip pixel is 1 column to the right of the body in frame 0 (sway right).
# In frame 1 the tip moves left of the body (sway left) — 2px total swing.
# Geometry never changes below y=6 (base is stable).
#
# Reed 1: body at x=8, base at y=6..8. Tip at x+1 (frame 0) or x-1 (frame 1).
# Reed 2: body at x=22, base at y=6..8. Same pattern.

def clutter_reeds_1_idle(frame):
    img = new(32, 16)

    tip_dx = 1 if frame == 0 else -1   # +1 = right sway; -1 = left sway

    for bx in (8, 22):
        # Tip — semi-transparent, 1 row above body, offset by tip_dx
        px(img, bx + tip_dx, 5, REED_TIP)
        # Body — two solid rows
        px(img, bx, 6, REED)
        px(img, bx, 7, REED)
        # Base — slightly transparent (anchored, never shifts)
        px(img, bx, 8, REED_BSE)

    return img

# ═════════════════════════════ Main ════════════════════════════════════════
if __name__ == "__main__":
    print("Pack 8: NPC actions + resource idle animations")

    print("\nNPC actions:")
    for f in range(2):
        save(f"npc_rat_action_{f}.png",       npc_rat_action(f))
    for f in range(2):
        save(f"npc_giant_rat_action_{f}.png", npc_giant_rat_action(f))
    for f in range(2):
        save(f"npc_chicken_action_{f}.png",   npc_chicken_action(f))
    for f in range(2):
        save(f"npc_cow_action_{f}.png",       npc_cow_action(f))
    for f in range(2):
        save(f"npc_banker_action_{f}.png",    npc_banker_action(f))
    for f in range(2):
        save(f"npc_guide_action_{f}.png",     npc_guide_action(f))
    for f in range(2):
        save(f"npc_instructor_action_{f}.png", npc_instructor_action(f))

    print("\nResource idles:")
    for f in range(2):
        save(f"tree_idle_{f}.png",               tree_idle(f))
    for f in range(2):
        save(f"tree_oak_idle_{f}.png",           tree_oak_idle(f))
    for f in range(2):
        save(f"tree_willow_idle_{f}.png",        tree_willow_idle(f))
    for f in range(2):
        save(f"rock_gold_idle_{f}.png",          rock_gold_idle(f))
    for f in range(2):
        save(f"rock_runite_idle_{f}.png",        rock_runite_idle(f))
    for f in range(2):
        save(f"clutter_reeds_1_idle_{f}.png",    clutter_reeds_1_idle(f))

    print(f"\nDone — 26 files written to {OUT}/")
