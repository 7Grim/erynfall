#!/usr/bin/env python3
"""
generate-pack7.py
Pack 7 — water base tiles + player action animations.

Sprites generated:
  Water tiles (32×16):  tile_water_0..3       (4-frame animated base)
  Player (16×24):       player_mine_0/1       (south-facing, pickaxe raise/strike)
                        player_fish_0/1       (east-facing, rod cast/wait)
                        player_pickup_0/1     (south-facing, crouch/reach)

Character layout (16×24, inherited from generate-animations.py):
  y= 1        : hair crown        (head_y = 1)
  y= 2..6     : face / hair sides
  y= 7        : neck
  y= 8..13    : shirt / torso    (ty = 7)
  y=14        : belt
  y=15..20    : legs
  y=21..22    : boots (fixed — preserves bottom-center pivot)
  y=23        : ground anchor row (clear)

Usage:  python3 scripts/generate-pack7.py
Requires: Pillow
"""
from PIL import Image
import os

OUT = os.path.join(os.path.dirname(__file__), "..", "art", "sprites")
os.makedirs(OUT, exist_ok=True)

# ═══════════════════════════ Palette ═══════════════════════════════════════
T       = (0,   0,   0,   0)       # transparent

# Player / humanoid (exact match to generate-animations.py)
SKIN    = (208, 152, 104, 255)
SKIN_D  = (160, 104,  64, 255)
HAIR    = ( 56,  36,  16, 255)
TUN     = ( 72,  96, 148, 255)     # tunic blue-grey
TUN_D   = ( 48,  64, 104, 255)
PNT     = ( 80,  64,  40, 255)     # pants brown
PNT_D   = ( 56,  44,  24, 255)
BOOT    = ( 36,  24,  12, 255)
BELT    = ( 88,  60,  24, 255)
EYE     = ( 40,  28,  20, 255)
AXE     = (160, 160, 168, 255)     # grey metal — reused for pick head

# Water (matching existing tile_water.png palette)
WAT_DARK = ( 18,  80, 160, 200)   # deep water base
WAT_LITE = ( 45, 135, 225, 220)   # lighter surface band

# ═══════════════════════════ Iso diamond mask (32×16) ══════════════════════
# Pixel extents per row, derived from actual tile_grass.png inspection.
DIAMOND_LEFT  = [16, 14, 12, 10,  8,  6,  4,  2,  0,  2,  5,  7,  9, 11, 14, 16]
DIAMOND_RIGHT = [16, 18, 20, 22, 23, 25, 27, 29, 31, 29, 27, 25, 22, 20, 18, 16]

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

# ═══════════════════════════ Head (copied from generate-animations.py) ════
def head_south(img, hy, skin, skin_d, hair, eye):
    """South-facing (frontal). Head occupies hy..hy+5."""
    hline(img,  4, 11, hy,     hair)
    px(img,  3, hy + 1, hair)
    px(img, 12, hy + 1, hair)
    rect(img, 4, hy + 1, 8, 4, skin)
    px(img,  5, hy + 3, eye)
    px(img, 10, hy + 3, eye)
    hline(img,  5, 10, hy + 5, skin)
    px(img,  4, hy + 5, skin_d)
    px(img, 11, hy + 5, skin_d)

def head_east(img, hy, skin, skin_d, hair, eye):
    """East-facing right-profile. Face on x=6-9, hair behind."""
    hline(img,  6, 12, hy,     hair)
    rect(img,   6, hy + 1, 4, 4, skin)
    hline(img, 10, 13, hy + 1, hair)
    hline(img, 10, 13, hy + 2, hair)
    hline(img, 10, 12, hy + 3, hair)
    px(img,  8, hy + 3, eye)
    hline(img,  6,  9, hy + 5, skin)

# ═══════════════════════════ Torso (copied from generate-animations.py) ════
def torso_south(img, ty, shirt, shirt_d, belt, skin):
    rect(img,  7, ty,     2, 1, skin)
    rect(img,  3, ty + 1, 10, 6, shirt)
    vline(img, 12, ty + 1, ty + 6, shirt_d)
    hline(img,  3, 12, ty + 7, belt)

def torso_east(img, ty, shirt, shirt_d, belt, skin):
    px(img,    6, ty, skin)
    rect(img,  6, ty + 1, 5, 6, shirt)
    vline(img, 11, ty + 1, ty + 3, shirt_d)
    hline(img,  6, 11, ty + 7, belt)

# ═══════════════════════════ Legs (copied from generate-animations.py) ═════
def legs_south(img, frame, pnt, pnt_d, boot):
    """Boots fixed at y=21-22 regardless of frame."""
    if frame in (0, 2):
        rect(img,  3, 15, 4, 6, pnt)
        rect(img,  9, 15, 4, 6, pnt)
        rect(img,  3, 21, 5, 2, boot)
        rect(img,  8, 21, 5, 2, boot)
    elif frame == 1:
        rect(img,  2, 15, 4, 7, pnt)
        vline(img, 5, 15, 21, pnt_d)
        rect(img,  9, 15, 4, 6, pnt)
        vline(img, 12, 15, 20, pnt_d)
        rect(img,  2, 22, 5, 1, boot)
        rect(img,  8, 21, 5, 2, boot)
    else:
        rect(img,  3, 15, 4, 6, pnt)
        vline(img, 6, 15, 20, pnt_d)
        rect(img,  9, 15, 4, 7, pnt)
        vline(img, 12, 15, 21, pnt_d)
        rect(img,  3, 21, 5, 2, boot)
        rect(img,  9, 22, 5, 1, boot)

def legs_east(img, frame, pnt, pnt_d, boot):
    """Profile east. Single visible leg in neutral (frame 0)."""
    if frame in (0, 2):
        rect(img,  7, 15, 4, 6, pnt)
        rect(img,  6, 21, 5, 2, boot)
    elif frame == 1:
        rect(img,  6, 15, 3, 6, pnt_d)
        rect(img,  8, 15, 4, 7, pnt)
        rect(img,  6, 21, 4, 1, boot)
        rect(img,  7, 22, 5, 1, boot)
    else:
        rect(img,  6, 15, 4, 7, pnt)
        rect(img,  8, 15, 3, 6, pnt_d)
        rect(img,  6, 22, 5, 1, boot)
        rect(img,  8, 21, 4, 1, boot)

# ═══════════════════════════ Water base tiles (32×16) ══════════════════════
#
# 4-frame animated iso tile.  A diagonal highlight band drifts across the
# surface each frame (shifts 4 px / frame; period = 16 px → loops in 4).
# The band axis is x - 2*y = const, which maps to E–W in world space.
# Two-tone only (OSRS flat-colour convention; shimmer/sparkle overlays handle
# fine detail).

def water_tile(frame):
    img = new(32, 16)
    offset = frame * 4          # 0, 4, 8, 12 across the 4 frames
    for y in range(16):
        xl = DIAMOND_LEFT[y]
        xr = DIAMOND_RIGHT[y]
        for x in range(xl, xr + 1):
            # Phase along the E-W iso diagonal shifted by frame offset
            phase = (x - 2 * y + offset) % 16
            c = WAT_LITE if phase < 5 else WAT_DARK
            px(img, x, y, c)
    return img

# ═══════════════════════════ Player mine (16×24) ════════════════════════════
#
# South-facing (non-directional first pass).
# Pickaxe: BOOT-coloured handle + AXE-grey horizontal head.
#
# Frame 0 — raised:
#   Body at natural dy=0. Handle rises from shoulder (y≈9) past the head to
#   y=3. Pick head bar at y=2 (above crown, x=10..15), with tip px at (10,3).
#
# Frame 1 — strike:
#   Body shifts dy=1 (lean into swing). Handle drops from shoulder to impact
#   at y=16 (just below belt). Pick head at y=16-17 with tip px below.

def player_mine(frame):
    img = new(16, 24)
    if frame == 0:
        # ── body (no bob) ──
        head_south(img, 1, SKIN, SKIN_D, HAIR, EYE)
        torso_south(img, 7, TUN, TUN_D, BELT, SKIN)
        legs_south(img, 0, PNT, PNT_D, BOOT)
        # ── pickaxe (raised over right shoulder) ──
        # Handle: right of shirt (x=13), from shoulder up past head
        vline(img, 13, 3, 9, BOOT)
        # Pick head: horizontal bar above crown
        hline(img, 10, 15, 2, AXE)
        px(img, 10, 3, AXE)             # pick tip curves down-left
        px(img, 15, 3, AXE)             # blunt end curves down-right
    else:
        # ── body (lean dy=1) ──
        head_south(img, 2, SKIN, SKIN_D, HAIR, EYE)
        torso_south(img, 8, TUN, TUN_D, BELT, SKIN)
        legs_south(img, 0, PNT, PNT_D, BOOT)
        # ── pickaxe (swinging to impact) ──
        # Handle drops from shoulder (y≈10) to just below belt
        vline(img, 13, 10, 16, BOOT)
        # Pick head at strike row
        hline(img, 10, 15, 16, AXE)
        px(img, 10, 17, AXE)            # pick tip embedded in ground
        px(img, 15, 17, AXE)
    return img

# ═══════════════════════════ Player fish (16×24) ════════════════════════════
#
# East-facing (rod extends clearly into forward space on the right).
# Readable across all fishing methods (net/bait/lure/cage/harpoon).
# Fishing rod: BOOT dark handle pixels.  Fishing line: EYE dark grey pixels.
#
# Frame 0 — cast/extended:
#   Arms pushed forward (arm extension px at x=12); rod angled ~35° upward.
#   Rod body from (12,8) diagonalling to tip at (15,3). Line hangs from tip.
#
# Frame 1 — engaged/waiting:
#   Arms at lower angle; rod nearly horizontal at shoulder level.
#   Rod from (12,10) to tip at (15,7). Shorter line drop.

def player_fish(frame):
    img = new(16, 24)
    head_east(img, 1, SKIN, SKIN_D, HAIR, EYE)
    torso_east(img, 7, TUN, TUN_D, BELT, SKIN)
    legs_east(img, 0, PNT, PNT_D, BOOT)

    if frame == 0:
        # Arm extension
        px(img, 12, 9, SKIN)
        # Rod: diagonal from (12,8) up-right to tip at (15,3)
        px(img, 12, 8, BOOT)
        px(img, 13, 7, BOOT)
        px(img, 13, 6, BOOT)
        px(img, 14, 5, BOOT)
        px(img, 14, 4, BOOT)
        px(img, 15, 3, BOOT)            # rod tip
        # Line hanging from tip (dark, subtle)
        px(img, 15, 4, EYE)
        px(img, 15, 5, EYE)
        px(img, 15, 6, EYE)
    else:
        # Arm at slightly lower position
        px(img, 12, 10, SKIN)
        # Rod: shallower angle from (12,10) to tip at (15,7)
        px(img, 12,  9, BOOT)
        px(img, 13,  9, BOOT)
        px(img, 14,  8, BOOT)
        px(img, 14,  7, BOOT)
        px(img, 15,  7, BOOT)           # rod tip
        # Line (shorter drop — waiting/engaged)
        px(img, 15,  8, EYE)
        px(img, 15,  9, EYE)
    return img

# ═══════════════════════════ Player pickup (16×24) ══════════════════════════
#
# South-facing crouch-and-reach.
# Head + torso shift DOWN by dy while boots stay fixed, producing a clear
# crouch silhouette. Arm reach shown as SKIN pixels just outside the
# torso/leg bounds at x=2 (left) and x=13 (right).
#
# Frame 0: dy=1 — slight lean, arms at belt level.
# Frame 1: dy=2 — deeper crouch, arms reaching toward ground.

def player_pickup(frame):
    img = new(16, 24)
    dy = frame + 1                      # dy=1 for frame 0, dy=2 for frame 1

    head_south(img, 1 + dy, SKIN, SKIN_D, HAIR, EYE)
    torso_south(img, 7 + dy, TUN, TUN_D, BELT, SKIN)
    legs_south(img, 0, PNT, PNT_D, BOOT)

    # Arms reaching down — SKIN pixels flanking the torso/leg column
    arm_top = 7 + dy + 6                # bottom of shirt (ty+6) = reach start
    reach   = 2 + frame                 # 2 px frame 0, 3 px frame 1
    for i in range(reach):
        px(img,  2, arm_top + i, SKIN)  # left arm
        px(img, 13, arm_top + i, SKIN)  # right arm
    return img

# ═════════════════════════════ Main ════════════════════════════════════════
if __name__ == "__main__":
    print("Pack 7: water base tiles + player action animations")
    print()

    print("Water base tiles:")
    for f in range(4):
        save(f"tile_water_{f}.png", water_tile(f))

    print("Player mine:")
    for f in range(2):
        save(f"player_mine_{f}.png", player_mine(f))

    print("Player fish:")
    for f in range(2):
        save(f"player_fish_{f}.png", player_fish(f))

    print("Player pickup:")
    for f in range(2):
        save(f"player_pickup_{f}.png", player_pickup(f))

    print(f"\nDone — 10 files written to {OUT}/")
