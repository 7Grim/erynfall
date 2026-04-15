"""
Generate G3DJ humanoid character models with multi-bone hierarchy for
programmatic OSRS-style walk animation.

Each limb is a separate node with bone-local vertex coordinates so
Renderer3DExperimental can rotate them independently.
Vertex COLOR (pre-baked per-face shading) replaces NORMAL — renderer uses
characterEnvironment (AmbientLight=white, no directional light).

Body parts use OCTAGONAL frustum prisms (8 side faces + 2 octagonal caps)
so the silhouette reads as cylindrical — matching OSRS style.

player_base  — John the cowboy. Includes all 8 keyframe animation clips
               (idle, walk, pickup, chop, mine, fish, sword, spear) plus
               cowboy-specific geometry nodes (hat, bandana, holster, spurs).
               When clips are present the renderer uses AnimationController
               instead of the procedural fallback.

NPC bases    — Same oct-frustum structure, empty animations array → procedural
               walk/idle.

Outputs to art/models/ (Maven copies to client/src/main/resources/models/).

Usage:  python3 scripts/generate-character-models.py
"""

import json, math, os

OUT_DIR      = os.path.join(os.path.dirname(__file__), "..", "art", "models")
RESOURCE_DIR = os.path.join(os.path.dirname(__file__), "..", "client", "src", "main", "resources", "models")


# ─── GEOMETRY HELPERS ─────────────────────────────────────────────────────────

def oct_ring(hw, hd, y):
    """
    8 CCW vertices (viewed from above) of an octagon at height y,
    inscribed within bounding rectangle hw × hd.
    """
    c = min(hw, hd) * (1.0 - 1.0 / math.sqrt(2.0))
    return [
        ( hw,     y,  hd - c),
        ( hw - c, y,  hd    ),
        (-hw + c, y,  hd    ),
        (-hw,     y,  hd - c),
        (-hw,     y, -(hd-c)),
        (-hw + c, y, -hd    ),
        ( hw - c, y, -hd    ),
        ( hw,     y, -(hd-c)),
    ]


# OSRS light from upper-front-left.
SIDE_SHADE   = [0.78, 0.88, 0.84, 0.78, 0.60, 0.50, 0.58, 0.68]
TOP_SHADE    = 0.92
BOTTOM_SHADE = 0.50


def _shade(r0, g0, b0, s):
    return round(r0 * s, 4), round(g0 * s, 4), round(b0 * s, 4)


def oct_frustum_verts(tw, td, bw, bd, y0, y1, rgba):
    """
    Octagonal frustum with pre-baked per-face shading.
      tw/td = top half-width/half-depth (at y1)
      bw/bd = bottom half-width/half-depth (at y0)
    Returns 48 vertices × 7 floats [px,py,pz,r,g,b,a].
    """
    r0, g0, b0, _ = rgba
    bot = oct_ring(bw, bd, y0)
    top = oct_ring(tw, td, y1)
    verts = []
    for i in range(8):
        s = SIDE_SHADE[i]
        r, g, b = _shade(r0, g0, b0, s)
        j = (i + 1) % 8
        for p in [bot[i], bot[j], top[j], top[i]]:
            verts.extend([round(p[0], 4), round(p[1], 4), round(p[2], 4), r, g, b, 1.0])
    r, g, b = _shade(r0, g0, b0, TOP_SHADE)
    for p in top:
        verts.extend([round(p[0], 4), round(p[1], 4), round(p[2], 4), r, g, b, 1.0])
    r, g, b = _shade(r0, g0, b0, BOTTOM_SHADE)
    for p in bot:
        verts.extend([round(p[0], 4), round(p[1], 4), round(p[2], 4), r, g, b, 1.0])
    return verts


def oct_indices():
    """84 indices for a 48-vertex octagonal frustum."""
    idx = []
    for i in range(8):
        b = i * 4
        idx += [b, b+1, b+2,  b+2, b+3, b]
    base = 32
    for i in range(1, 7):
        idx += [base, base + i, base + i + 1]
    base = 40
    for i in range(1, 7):
        idx += [base, base + i + 1, base + i]
    return idx


def mesh(mid, pid, geom, color):
    return {
        "id": mid,
        "attributes": ["POSITION", "COLOR"],
        "vertices": oct_frustum_verts(*geom, color),
        "parts": [{"id": pid, "type": "TRIANGLES", "indices": oct_indices()}],
    }


def part(pid):
    return {"meshpartid": pid, "materialid": "mat_white"}


def node(nid, tx, ty, tz, pid=None, children=None):
    n = {"id": nid, "translation": [tx, ty, tz]}
    if pid:
        n["parts"] = [part(pid)]
    if children:
        n["children"] = children
    return n


def anchor(nid, tx, ty, tz):
    return {"id": nid, "translation": [tx, ty, tz]}


# ─── ANIMATION HELPERS ────────────────────────────────────────────────────────

def quat_x(deg):
    """Quaternion [x,y,z,w] for rotation about X axis by deg degrees."""
    r = math.radians(deg) / 2.0
    return [round(math.sin(r), 6), 0.0, 0.0, round(math.cos(r), 6)]


def bone_anim(bone_id, kf_list):
    """
    kf_list: list of (keytime_seconds, rotation_degrees_around_X).
    Returns a G3DJ bones entry with quaternion keyframes.
    """
    return {
        "boneId": bone_id,
        "keyframes": [
            {"keytime": round(t, 4), "rotation": quat_x(deg)}
            for t, deg in kf_list
        ],
    }


def anim_clip(clip_id, length_s, bone_list):
    """
    bone_list: list of (bone_id, [(keytime, deg_x), ...]).
    Returns a complete G3DJ animation dict.
    """
    return {
        "id": clip_id,
        "length": round(length_s, 4),
        "bones": [bone_anim(bid, kfs) for bid, kfs in bone_list],
    }


# ─── GEOMETRY DEFINITIONS ─────────────────────────────────────────────────────
#
#   (tw, td, bw, bd, y0, y1)  — all in G3DJ units (Y-up, 1 unit ≈ 1 game tile)
#
#   Limbs hang below pivot:  y0 = –length, y1 = 0
#   Upward parts:            y0 = 0,       y1 = height

GEOM = {
    # ── Core body (shared by all humanoid characters) ──────────────────────────
    "torso":       (0.135, 0.080, 0.090, 0.065,  0.00,  0.46),
    "neck":        (0.038, 0.034, 0.036, 0.032,  0.00,  0.06),
    "head":        (0.082, 0.080, 0.090, 0.085,  0.00,  0.22),
    "hair":        (0.092, 0.090, 0.090, 0.088,  0.00,  0.06),
    "upper_arm_l": (0.050, 0.048, 0.038, 0.036, -0.22,  0.00),
    "lower_arm_l": (0.038, 0.036, 0.030, 0.028, -0.22,  0.00),
    "upper_arm_r": (0.050, 0.048, 0.038, 0.036, -0.22,  0.00),
    "lower_arm_r": (0.038, 0.036, 0.030, 0.028, -0.22,  0.00),
    "upper_leg_l": (0.060, 0.058, 0.048, 0.046, -0.24,  0.00),
    "lower_leg_l": (0.052, 0.060, 0.040, 0.048, -0.28,  0.00),
    "upper_leg_r": (0.060, 0.058, 0.048, 0.046, -0.24,  0.00),
    "lower_leg_r": (0.052, 0.060, 0.040, 0.048, -0.28,  0.00),
    # ── Cowboy-specific geometry (player_base only) ────────────────────────────
    # Hat: brim is a wide flat disc; crown is a tapered tall cylinder;
    #      band is a thin ring at the crown base.  All are children of "head".
    "hat_brim":    (0.220, 0.200, 0.220, 0.200,  0.00,  0.040),
    "hat_crown":   (0.110, 0.100, 0.095, 0.086,  0.00,  0.260),
    "hat_band":    (0.116, 0.106, 0.116, 0.106,  0.00,  0.030),
    # Bandana: wide short tube at the neck, child of "neck".
    "bandana":     (0.058, 0.052, 0.048, 0.043,  0.00,  0.100),
    # Holster: small hanging pouch, child of "upper_leg_r" (offset to outer thigh).
    "holster":     (0.028, 0.048, 0.022, 0.038, -0.14,  0.000),
    # Spurs: thin gold disc on back of boot, children of lower_leg_l/r.
    "spur_l":      (0.016, 0.056, 0.016, 0.056,  0.00,  0.022),
    "spur_r":      (0.016, 0.056, 0.016, 0.056,  0.00,  0.022),
}

BASE_PARTS = [
    "torso", "neck", "head", "hair",
    "upper_arm_l", "lower_arm_l", "upper_arm_r", "lower_arm_r",
    "upper_leg_l", "lower_leg_l", "upper_leg_r", "lower_leg_r",
]

COWBOY_EXTRA_PARTS = [
    "hat_brim", "hat_crown", "hat_band", "bandana", "holster", "spur_l", "spur_r",
]


# ─── NPC CHARACTER BUILDER ────────────────────────────────────────────────────

def make_character(model_id, colors, include_anchors=False):
    """
    Generic humanoid character — no cowboy extras, empty animations array
    (engine uses procedural walk/idle).
    """
    skin     = colors["skin"]
    hair_c   = colors["hair"]
    shirt    = colors["shirt"]
    la_color = colors.get("lower_arm", skin)
    pants    = colors["pants"]
    boots    = colors["boots"]

    color_map = {
        "torso":       shirt,
        "neck":        skin,
        "head":        skin,
        "hair":        hair_c,
        "upper_arm_l": shirt,    "upper_arm_r": shirt,
        "lower_arm_l": la_color, "lower_arm_r": la_color,
        "upper_leg_l": pants,    "upper_leg_r": pants,
        "lower_leg_l": boots,    "lower_leg_r": boots,
    }

    meshes    = [mesh(f"mesh_{p}", f"part_{p}", GEOM[p], color_map[p]) for p in BASE_PARTS]
    materials = [{"id": "mat_white", "diffuse": [1.0, 1.0, 1.0, 1.0]}]

    lower_arm_l_ch = []
    lower_arm_r_ch = []
    if include_anchors:
        lower_arm_l_ch.append(anchor("shield_anchor", -0.085, 0.06,  0.02))
        lower_arm_r_ch.append(anchor("weapon_anchor",  0.085, 0.06, -0.02))

    root_children = [
        node("torso",        0.0,    0.54, 0.0, pid="part_torso"),
        node("neck",         0.0,    1.00, 0.0, pid="part_neck"),
        node("head",         0.0,    1.06, 0.0, pid="part_head",
             children=[node("hair", 0.0, 0.22, 0.0, pid="part_hair")]),
        node("upper_arm_l", -0.155,  1.04, 0.0, pid="part_upper_arm_l",
             children=[node("lower_arm_l", 0.0, -0.22, 0.0, pid="part_lower_arm_l",
                            children=lower_arm_l_ch)]),
        node("upper_arm_r",  0.155,  1.04, 0.0, pid="part_upper_arm_r",
             children=[node("lower_arm_r", 0.0, -0.22, 0.0, pid="part_lower_arm_r",
                            children=lower_arm_r_ch)]),
        node("upper_leg_l", -0.055,  0.54, 0.0, pid="part_upper_leg_l",
             children=[node("lower_leg_l", 0.0, -0.24, 0.0, pid="part_lower_leg_l")]),
        node("upper_leg_r",  0.055,  0.54, 0.0, pid="part_upper_leg_r",
             children=[node("lower_leg_r", 0.0, -0.24, 0.0, pid="part_lower_leg_r")]),
    ]

    if include_anchors:
        root_children += [
            anchor("head_anchor",    0.0,  1.28,  0.0),
            anchor("cape_anchor",    0.0,  1.00,  0.08),
            anchor("ammo_anchor",   -0.12, 1.00,  0.10),
            anchor("body_anchor",    0.0,  0.88,  0.0),
            anchor("legs_anchor",    0.0,  0.44,  0.0),
            anchor("hands_anchor",   0.0,  0.68,  0.0),
            anchor("feet_anchor",    0.0,  0.08,  0.0),
        ]

    return {
        "version":    [0, 1],
        "id":         model_id,
        "meshes":     meshes,
        "materials":  materials,
        "nodes":      [{"id": f"{model_id}_node", "children": root_children}],
        "animations": [],
    }


# ─── PLAYER BASE (COWBOY) BUILDER ─────────────────────────────────────────────

def make_player_base(model_id, colors):
    """
    Builds the complete animated player_base G3DJ for the John/cowboy character.

    Extra geometry vs NPC base:
      hat_brim / hat_crown / hat_band  — children of "head"
      bandana                          — child of "neck"
      holster                          — child of "upper_leg_r"
      spur_l / spur_r                  — children of lower_leg_l / lower_leg_r

    All 8 animation clips are embedded so Renderer3DExperimental uses
    AnimationController (keyframe interpolation) instead of the procedural
    fallback (which only handles idle / walk).
    """
    color_map = {
        "torso":       colors["vest"],
        "neck":        colors["skin"],
        "head":        colors["skin"],
        "hair":        colors["hair"],
        "upper_arm_l": colors["shirt"],   "upper_arm_r": colors["shirt"],
        "lower_arm_l": colors["skin"],    "lower_arm_r": colors["skin"],
        "upper_leg_l": colors["pants"],   "upper_leg_r": colors["pants"],
        "lower_leg_l": colors["boots"],   "lower_leg_r": colors["boots"],
        "hat_brim":    colors["hat"],
        "hat_crown":   colors["hat"],
        "hat_band":    colors["hat_band"],
        "bandana":     colors["bandana"],
        "holster":     colors["holster"],
        "spur_l":      colors["spurs"],
        "spur_r":      colors["spurs"],
    }

    all_parts = BASE_PARTS + COWBOY_EXTRA_PARTS
    meshes    = [mesh(f"mesh_{p}", f"part_{p}", GEOM[p], color_map[p]) for p in all_parts]
    materials = [{"id": "mat_white", "diffuse": [1.0, 1.0, 1.0, 1.0]}]

    # ── Node hierarchy ────────────────────────────────────────────────────────
    #
    # Hat nodes sit above the hair cap.  In head-local space:
    #   head geom  y1 = 0.22  (top of head relative to pivot)
    #   hair child y  = 0.22, hair geom y1 = 0.06  → hair top = 0.28
    #   hat_brim   y  = 0.28  (rests on hair top),  geom y1 = 0.04
    #   hat_crown  y  = 0.32  (sits on brim top),   geom y1 = 0.26
    #   hat_band   y  = 0.32  (same as crown base),  geom y1 = 0.03
    #
    # Bandana in neck-local space: at y=0 (neck base), slight Z offset (forward).
    # Holster in upper_leg_r-local space: offset to outer thigh (+X).
    # Spurs at bottom-back of lower_leg local space.

    head_children = [
        node("hair",      0.0, 0.22, 0.0,  pid="part_hair"),
        node("hat_brim",  0.0, 0.28, 0.0,  pid="part_hat_brim"),
        node("hat_crown", 0.0, 0.32, 0.0,  pid="part_hat_crown"),
        node("hat_band",  0.0, 0.32, 0.0,  pid="part_hat_band"),
    ]

    root_children = [
        node("torso",        0.0,    0.54, 0.0, pid="part_torso"),
        node("neck",         0.0,    1.00, 0.0, pid="part_neck",
             children=[node("bandana", 0.0, 0.0, 0.010, pid="part_bandana")]),
        node("head",         0.0,    1.06, 0.0, pid="part_head",
             children=head_children),
        node("upper_arm_l", -0.155,  1.04, 0.0, pid="part_upper_arm_l",
             children=[node("lower_arm_l", 0.0, -0.22, 0.0, pid="part_lower_arm_l",
                            children=[anchor("shield_anchor", -0.085, 0.06, 0.02)])]),
        node("upper_arm_r",  0.155,  1.04, 0.0, pid="part_upper_arm_r",
             children=[node("lower_arm_r", 0.0, -0.22, 0.0, pid="part_lower_arm_r",
                            children=[anchor("weapon_anchor",  0.085, 0.06, -0.02)])]),
        node("upper_leg_l", -0.055,  0.54, 0.0, pid="part_upper_leg_l",
             children=[node("lower_leg_l", 0.0, -0.24, 0.0, pid="part_lower_leg_l",
                            children=[node("spur_l", 0.0, -0.25, -0.052, pid="part_spur_l")])]),
        node("upper_leg_r",  0.055,  0.54, 0.0, pid="part_upper_leg_r",
             children=[
                 node("lower_leg_r", 0.0, -0.24, 0.0, pid="part_lower_leg_r",
                      children=[node("spur_r", 0.0, -0.25, -0.052, pid="part_spur_r")]),
                 node("holster", 0.100, -0.08, 0.0, pid="part_holster"),
             ]),
        anchor("head_anchor",    0.0,  1.28,  0.0),
        anchor("cape_anchor",    0.0,  1.00,  0.08),
        anchor("ammo_anchor",   -0.12, 1.00,  0.10),
        anchor("body_anchor",    0.0,  0.88,  0.0),
        anchor("legs_anchor",    0.0,  0.44,  0.0),
        anchor("hands_anchor",   0.0,  0.68,  0.0),
        anchor("feet_anchor",    0.0,  0.08,  0.0),
    ]

    return {
        "version":    [0, 1],
        "id":         model_id,
        "meshes":     meshes,
        "materials":  materials,
        "nodes":      [{"id": f"{model_id}_node", "children": root_children}],
        "animations": _make_player_animations(),
    }


def _make_player_animations():
    """
    All 8 keyframe animation clips required by Renderer3DExperimental.
    Only the 8 deforming limb nodes are keyed; hat/bandana/holster/spurs
    inherit their parent's transform automatically.

    Rotation convention (X-axis):
      Positive deg → limb bottom swings toward -Z (forward = toward viewer in
      isometric view), matching the procedural applyWalkAnimation().
    """
    UAL = "upper_arm_l"
    UAR = "upper_arm_r"
    LAL = "lower_arm_l"
    LAR = "lower_arm_r"
    ULL = "upper_leg_l"
    ULR = "upper_leg_r"
    LLL = "lower_leg_l"
    LLR = "lower_leg_r"

    def kf(bone_id, pairs):
        return (bone_id, pairs)

    def rest_bone(bone_id, length):
        return kf(bone_id, [(0.0, 0), (length, 0)])

    def all_rest(length):
        return [rest_bone(b, length) for b in (UAL, UAR, LAL, LAR, ULL, ULR, LLL, LLR)]

    # ── idle (1.0 s, loop) ────────────────────────────────────────────────────
    # Arms relaxed slightly forward; subtle 1° sway at mid-point.
    idle = anim_clip("idle", 1.0, [
        kf(UAL, [(0.0, -5), (0.5, -4), (1.0, -5)]),
        kf(UAR, [(0.0, -5), (0.5, -4), (1.0, -5)]),
        kf(LAL, [(0.0,  0), (1.0,  0)]),
        kf(LAR, [(0.0,  0), (1.0,  0)]),
        kf(ULL, [(0.0,  0), (1.0,  0)]),
        kf(ULR, [(0.0,  0), (1.0,  0)]),
        kf(LLL, [(0.0,  0), (1.0,  0)]),
        kf(LLR, [(0.0,  0), (1.0,  0)]),
    ])

    # ── walk (0.8 s, loop) ────────────────────────────────────────────────────
    # Cross-body swing: left leg forward ↔ right arm forward.
    # Knee bends on the leg that is currently swinging forward.
    # t=0.0 / t=0.8 : left leg fwd, right arm fwd (phase 0)
    # t=0.2        : mid-stride (all neutral)
    # t=0.4        : right leg fwd, left arm fwd (phase π)
    # t=0.6        : mid-stride
    walk = anim_clip("walk", 0.8, [
        kf(ULL, [(0.0, +30), (0.2,  0), (0.4, -30), (0.6,  0), (0.8, +30)]),
        kf(ULR, [(0.0, -30), (0.2,  0), (0.4, +30), (0.6,  0), (0.8, -30)]),
        kf(LLL, [(0.0, +12), (0.2,  0), (0.4,   0), (0.6,  0), (0.8, +12)]),
        kf(LLR, [(0.0,   0), (0.2,  0), (0.4, +12), (0.6,  0), (0.8,   0)]),
        kf(UAL, [(0.0, -22), (0.2,  0), (0.4, +22), (0.6,  0), (0.8, -22)]),
        kf(UAR, [(0.0, +22), (0.2,  0), (0.4, -22), (0.6,  0), (0.8, +22)]),
        kf(LAL, [(0.0,   0), (0.2,  0), (0.4,  +8), (0.6,  0), (0.8,   0)]),
        kf(LAR, [(0.0,  +8), (0.2,  0), (0.4,   0), (0.6,  0), (0.8,  +8)]),
    ])

    # ── pickup (0.7 s, one-shot) ──────────────────────────────────────────────
    # Both arms reach forward and down; slight knee bend.
    pickup = anim_clip("pickup", 0.7, [
        kf(UAL, [(0.0, -5), (0.25, +40), (0.45, +55), (0.70, -5)]),
        kf(UAR, [(0.0, -5), (0.25, +40), (0.45, +55), (0.70, -5)]),
        kf(LAL, [(0.0,  0), (0.25, +28), (0.45, +42), (0.70,  0)]),
        kf(LAR, [(0.0,  0), (0.25, +28), (0.45, +42), (0.70,  0)]),
        kf(ULL, [(0.0,  0), (0.25, -10), (0.45, -14), (0.70,  0)]),
        kf(ULR, [(0.0,  0), (0.25, -10), (0.45, -14), (0.70,  0)]),
        kf(LLL, [(0.0,  0), (0.70,   0)]),
        kf(LLR, [(0.0,  0), (0.70,   0)]),
    ])

    # ── chop (0.7 s, one-shot) ────────────────────────────────────────────────
    # Both arms raise high then swing down hard (axe chop).
    chop = anim_clip("chop", 0.7, [
        kf(UAL, [(0.0, -5), (0.20, -90), (0.38, +28), (0.70, -5)]),
        kf(UAR, [(0.0, -5), (0.20, -95), (0.38, +28), (0.70, -5)]),
        kf(LAL, [(0.0,  0), (0.20, -20), (0.38, +12), (0.70,  0)]),
        kf(LAR, [(0.0,  0), (0.20, -15), (0.38, +12), (0.70,  0)]),
        kf(ULL, [(0.0,  0), (0.70,   0)]),
        kf(ULR, [(0.0,  0), (0.70,   0)]),
        kf(LLL, [(0.0,  0), (0.70,   0)]),
        kf(LLR, [(0.0,  0), (0.70,   0)]),
    ])

    # ── mine (0.7 s, one-shot) ────────────────────────────────────────────────
    # Right arm dominant overhead strike (pickaxe); left supports.
    mine = anim_clip("mine", 0.7, [
        kf(UAR, [(0.0, -5), (0.20, -110), (0.38, +35), (0.70, -5)]),
        kf(UAL, [(0.0, -5), (0.20,  -70), (0.38, +18), (0.70, -5)]),
        kf(LAR, [(0.0,  0), (0.20,  -12), (0.38, +15), (0.70,  0)]),
        kf(LAL, [(0.0,  0), (0.20,   -8), (0.38,  +8), (0.70,  0)]),
        kf(ULL, [(0.0,  0), (0.70,    0)]),
        kf(ULR, [(0.0,  0), (0.70,    0)]),
        kf(LLL, [(0.0,  0), (0.70,    0)]),
        kf(LLR, [(0.0,  0), (0.70,    0)]),
    ])

    # ── fish (1.5 s, loop) ────────────────────────────────────────────────────
    # Right arm holds rod forward-up; subtle bob every half-cycle.
    fish = anim_clip("fish", 1.5, [
        kf(UAR, [(0.0, -50), (0.75, -46), (1.5, -50)]),
        kf(LAR, [(0.0, -60), (0.75, -55), (1.5, -60)]),
        kf(UAL, [(0.0, -12), (0.75, -10), (1.5, -12)]),
        kf(LAL, [(0.0,   0), (1.5,    0)]),
        kf(ULL, [(0.0,   0), (1.5,    0)]),
        kf(ULR, [(0.0,   0), (1.5,    0)]),
        kf(LLL, [(0.0,   0), (1.5,    0)]),
        kf(LLR, [(0.0,   0), (1.5,    0)]),
    ])

    # ── sword (0.7 s, one-shot) ───────────────────────────────────────────────
    # Right-arm horizontal slash: guard → windup → slash → follow → guard.
    sword = anim_clip("sword", 0.7, [
        kf(UAR, [(0.0, -22), (0.18, -60), (0.35, +18), (0.55, +12), (0.70, -22)]),
        kf(LAR, [(0.0, -15), (0.18, -10), (0.35, +12), (0.55,  +8), (0.70, -15)]),
        kf(UAL, [(0.0, -10), (0.18,  -5), (0.35,  +8), (0.55,  +5), (0.70, -10)]),
        kf(LAL, [(0.0,   0), (0.70,   0)]),
        kf(ULL, [(0.0,   0), (0.70,   0)]),
        kf(ULR, [(0.0,   0), (0.70,   0)]),
        kf(LLL, [(0.0,   0), (0.70,   0)]),
        kf(LLR, [(0.0,   0), (0.70,   0)]),
    ])

    # ── spear (0.7 s, one-shot) ───────────────────────────────────────────────
    # Two-handed forward thrust: hold → coil → thrust → hold → recover.
    spear = anim_clip("spear", 0.7, [
        kf(UAR, [(0.0, -22), (0.18, -18), (0.38, -48), (0.55, -46), (0.70, -22)]),
        kf(UAL, [(0.0, -20), (0.18, -16), (0.38, -44), (0.55, -42), (0.70, -20)]),
        kf(LAR, [(0.0, -28), (0.18, -22), (0.38, -65), (0.55, -62), (0.70, -28)]),
        kf(LAL, [(0.0, -24), (0.18, -18), (0.38, -60), (0.55, -58), (0.70, -24)]),
        kf(ULL, [(0.0,   0), (0.70,   0)]),
        kf(ULR, [(0.0,   0), (0.70,   0)]),
        kf(LLL, [(0.0,   0), (0.70,   0)]),
        kf(LLR, [(0.0,   0), (0.70,   0)]),
    ])

    return [idle, walk, pickup, chop, mine, fish, sword, spear]


# ─── CHARACTER DEFINITIONS ────────────────────────────────────────────────────

# Common NPC skin/boot constants
_SKIN  = [0.84, 0.64, 0.44, 1.0]
_BOOTS = [0.30, 0.15, 0.06, 1.0]

# ── player_base: John the cowboy ──────────────────────────────────────────────
PLAYER_BASE_COLORS = {
    "skin":     [0.85, 0.60, 0.35, 1.0],   # sun-tanned
    "hair":     [0.25, 0.18, 0.10, 1.0],   # dark brown
    "vest":     [0.28, 0.18, 0.09, 1.0],   # dark leather vest (torso)
    "shirt":    [0.55, 0.48, 0.36, 1.0],   # dusty off-white (upper arms)
    "pants":    [0.20, 0.25, 0.32, 1.0],   # faded denim
    "boots":    [0.22, 0.14, 0.08, 1.0],   # dark brown leather
    "hat":      [0.30, 0.22, 0.12, 1.0],   # worn brown leather
    "hat_band": [0.65, 0.52, 0.22, 1.0],   # aged gold
    "bandana":  [0.65, 0.15, 0.10, 1.0],   # dusty red
    "holster":  [0.25, 0.16, 0.08, 1.0],   # dark leather
    "spurs":    [0.65, 0.52, 0.22, 1.0],   # aged gold
}

# ── NPC variants (procedural animation, no cowboy extras) ─────────────────────
NPC_CHARACTERS = {
    "npc_banker_base": {
        "colors": {
            "skin": _SKIN, "hair": [0.22, 0.14, 0.05, 1.0],
            "shirt": [0.12, 0.22, 0.45, 1.0], "pants": [0.18, 0.18, 0.22, 1.0],
            "boots": _BOOTS,
        },
    },
    "npc_guide_base": {
        "colors": {
            "skin": _SKIN, "hair": [0.32, 0.20, 0.06, 1.0],
            "shirt": [0.25, 0.52, 0.28, 1.0], "pants": [0.25, 0.52, 0.28, 1.0],
            "boots": _BOOTS,
        },
    },
    "npc_instructor_base": {
        "colors": {
            "skin": _SKIN, "hair": [0.20, 0.12, 0.04, 1.0],
            "shirt": [0.65, 0.12, 0.12, 1.0], "lower_arm": [0.52, 0.52, 0.55, 1.0],
            "pants": [0.32, 0.30, 0.28, 1.0], "boots": [0.28, 0.26, 0.24, 1.0],
        },
    },
    "npc_goblin_base": {
        "colors": {
            "skin": [0.54, 0.60, 0.28, 1.0], "hair": [0.18, 0.10, 0.02, 1.0],
            "shirt": [0.48, 0.56, 0.24, 1.0], "pants": [0.40, 0.48, 0.20, 1.0],
            "boots": [0.28, 0.14, 0.06, 1.0],
        },
    },
}


# ─── MAIN ─────────────────────────────────────────────────────────────────────

os.makedirs(OUT_DIR, exist_ok=True)
os.makedirs(RESOURCE_DIR, exist_ok=True)


def write_model(filename, data):
    """Write G3DJ to art/models/ and sync to classpath resources."""
    import shutil
    art_path = os.path.join(OUT_DIR, filename)
    res_path = os.path.join(RESOURCE_DIR, filename)
    with open(art_path, "w") as f:
        json.dump(data, f, separators=(",", ":"))
    shutil.copy2(art_path, res_path)


# player_base — cowboy, full animation clips + extra geometry
data = make_player_base("player_base", PLAYER_BASE_COLORS)
write_model("player_base.g3dj", data)
print(f"Generated player_base.g3dj  "
      f"({len(data['meshes'])} meshes, {len(data['animations'])} clips: "
      f"{', '.join(a['id'] for a in data['animations'])})")

# NPC base characters — procedural animation
for key, cfg in NPC_CHARACTERS.items():
    data = make_character(key, cfg["colors"], include_anchors=False)
    write_model(f"{key}.g3dj", data)
    print(f"Generated {key}.g3dj")
