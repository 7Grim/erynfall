"""
Generate G3DJ humanoid character models with multi-bone hierarchy for
programmatic OSRS-style walk animation.

Each limb is a separate node with bone-local vertex coordinates so
Renderer3DExperimental.applyWalkAnimation() can rotate them independently.
Vertex COLOR (pre-baked per-face shading) replaces NORMAL — renderer uses
characterEnvironment (AmbientLight=white, no directional light).

Body parts use OCTAGONAL frustum prisms (8 side faces + 2 octagonal caps)
so the silhouette reads as cylindrical — matching OSRS style. The graduated
shading around 8 faces eliminates the harsh "inner shadow" from a single
dark face being visible through inter-part gaps.

Empty animations array → hasClips=false → programmatic code runs instead
of AnimationController.

Outputs to art/models/ (Maven copies to client/src/main/resources/models/).
"""

import json, math, os

OUT_DIR = os.path.join(os.path.dirname(__file__), "..", "art", "models")


def oct_ring(hw, hd, y):
    """
    8 CCW vertices (viewed from above) of an octagon at height y,
    inscribed within bounding rectangle hw × hd.
    Corner cut c keeps the octagon proportional to the shorter dimension.
    """
    c = min(hw, hd) * (1.0 - 1.0 / math.sqrt(2.0))
    # Vertex layout: (X, Y_height, Z_depth) — Y is up in LibGDX
    return [
        ( hw,     y,  hd - c),   # 0  right-side,  front edge
        ( hw - c, y,  hd    ),   # 1  front-side,  right edge
        (-hw + c, y,  hd    ),   # 2  front-side,  left edge
        (-hw,     y,  hd - c),   # 3  left-side,   front edge
        (-hw,     y, -(hd-c)),   # 4  left-side,   back edge
        (-hw + c, y, -hd    ),   # 5  back-side,   left edge
        ( hw - c, y, -hd    ),   # 6  back-side,   right edge
        ( hw,     y, -(hd-c)),   # 7  right-side,  back edge
    ]


# OSRS light from upper-front-left.
# Face i = the quad between oct vertices i and (i+1)%8.
# Vertex 0 is right-side/front-edge; face normals go around CCW from face 0:
#   face 0 (+X+Z front-right), 1 (+Z front), 2 (-X+Z front-left),
#   3 (-X left), 4 (-X-Z back-left), 5 (-Z back), 6 (+X-Z back-right), 7 (+X right)
SIDE_SHADE = [
    0.78,   # face 0: front-right diagonal
    0.88,   # face 1: front
    0.84,   # face 2: front-left diagonal
    0.78,   # face 3: left
    0.60,   # face 4: back-left diagonal
    0.50,   # face 5: back
    0.58,   # face 6: back-right diagonal
    0.68,   # face 7: right
]
TOP_SHADE    = 0.92
BOTTOM_SHADE = 0.50


def _shade(r0, g0, b0, s):
    return round(r0 * s, 4), round(g0 * s, 4), round(b0 * s, 4)


def oct_frustum_verts(tw, td, bw, bd, y0, y1, rgba):
    """
    Octagonal frustum with pre-baked per-face shading.
      tw/td = top half-width/half-depth (at y1)
      bw/bd = bottom half-width/half-depth (at y0)
    Returns 48 vertices × 7 floats [px,py,pz,r,g,b,a]:
      - verts  0..31: 8 side faces × 4 verts  (SIDE_SHADE[i])
      - verts 32..39: top cap ring              (TOP_SHADE)
      - verts 40..47: bottom cap ring           (BOTTOM_SHADE)
    Winding matches LibGDX G3D convention (matches box_verts).
    """
    r0, g0, b0, _ = rgba
    bot = oct_ring(bw, bd, y0)
    top = oct_ring(tw, td, y1)
    verts = []

    # 8 side faces.  Each quad: bot[i], bot[j], top[j], top[i]  (j=(i+1)%8)
    # This winding matches box_verts convention used by the existing renderer.
    for i in range(8):
        s = SIDE_SHADE[i]
        r, g, b = _shade(r0, g0, b0, s)
        j = (i + 1) % 8
        for p in [bot[i], bot[j], top[j], top[i]]:
            verts.extend([round(p[0], 4), round(p[1], 4), round(p[2], 4), r, g, b, 1.0])

    # Top cap — 8 verts at y1, CCW from above (matches box_verts +Y face winding)
    r, g, b = _shade(r0, g0, b0, TOP_SHADE)
    for p in top:
        verts.extend([round(p[0], 4), round(p[1], 4), round(p[2], 4), r, g, b, 1.0])

    # Bottom cap — 8 verts at y0, CW from above (matches box_verts -Y face winding)
    r, g, b = _shade(r0, g0, b0, BOTTOM_SHADE)
    for p in bot:
        verts.extend([round(p[0], 4), round(p[1], 4), round(p[2], 4), r, g, b, 1.0])

    return verts


def oct_indices():
    """
    84 indices for a 48-vertex octagonal frustum:
      48 for 8 side quads + 18 for top cap fan + 18 for bottom cap fan.
    """
    idx = []

    # 8 side quads (vertices 0..31)
    for i in range(8):
        b = i * 4
        idx += [b, b+1, b+2,  b+2, b+3, b]

    # Top cap fan from vertex 32 (CCW from above → [0,1,2],[0,2,3],...)
    base = 32
    for i in range(1, 7):
        idx += [base, base + i, base + i + 1]

    # Bottom cap fan from vertex 40 (CW from above → [0,2,1],[0,3,2],...)
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


# Octagonal frustum geometry params: (tw, td, bw, bd, y0, y1)
#   tw/td = bounding-rect half-width/half-depth at y1 (the top end)
#   bw/bd = bounding-rect half-width/half-depth at y0 (the bottom end)
# Y=0 is the bone pivot.
#   Upward parts (torso, head, neck): y0=0, y1=height; bottom=pivot=narrower.
#   Hanging limbs (arms, legs): y0=-length, y1=0; top/pivot=wider, tapers down.
GEOM = {
    "torso":       (0.135, 0.080, 0.090, 0.065,  0.00,  0.46),  # wide at shoulders (top)
    "neck":        (0.038, 0.034, 0.036, 0.032,  0.00,  0.06),  # very slight taper upward
    "head":        (0.082, 0.080, 0.090, 0.085,  0.00,  0.22),  # slightly narrower at crown
    "hair":        (0.092, 0.090, 0.090, 0.088,  0.00,  0.06),  # flat cap
    "upper_arm_l": (0.050, 0.048, 0.038, 0.036, -0.22,  0.00),  # wide at shoulder, taper to elbow
    "lower_arm_l": (0.038, 0.036, 0.030, 0.028, -0.22,  0.00),  # taper to wrist
    "upper_arm_r": (0.050, 0.048, 0.038, 0.036, -0.22,  0.00),
    "lower_arm_r": (0.038, 0.036, 0.030, 0.028, -0.22,  0.00),
    "upper_leg_l": (0.060, 0.058, 0.048, 0.046, -0.24,  0.00),  # wide at hip, taper to knee
    "lower_leg_l": (0.052, 0.060, 0.040, 0.048, -0.28,  0.00),  # boot flares in Z
    "upper_leg_r": (0.060, 0.058, 0.048, 0.046, -0.24,  0.00),
    "lower_leg_r": (0.052, 0.060, 0.040, 0.048, -0.28,  0.00),
}

PARTS = [
    "torso", "neck", "head", "hair",
    "upper_arm_l", "lower_arm_l", "upper_arm_r", "lower_arm_r",
    "upper_leg_l", "lower_leg_l", "upper_leg_r", "lower_leg_r",
]


def make_character(model_id, colors, include_anchors=False):
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

    meshes    = [mesh(f"mesh_{p}", f"part_{p}", GEOM[p], color_map[p]) for p in PARTS]
    materials = [{"id": "mat_white", "diffuse": [1.0, 1.0, 1.0, 1.0]}]

    # Torso pivot at Y=0.54 (leaves room for neck node 0..+0.06 above it at Y=1.00).
    # Legs at X=±0.055 (less splayed than previous ±0.090 — more OSRS proportioned).
    root_id = f"{model_id}_node"
    root_children = [
        node("torso",        0.0,    0.54, 0.0, pid="part_torso"),
        node("neck",         0.0,    1.00, 0.0, pid="part_neck"),
        node("head",         0.0,    1.06, 0.0, pid="part_head",
             children=[node("hair", 0.0,  0.22, 0.0, pid="part_hair")]),
        node("upper_arm_l", -0.155,  1.04, 0.0, pid="part_upper_arm_l",
             children=[node("lower_arm_l", 0.0, -0.22, 0.0, pid="part_lower_arm_l")]),
        node("upper_arm_r",  0.155,  1.04, 0.0, pid="part_upper_arm_r",
             children=[node("lower_arm_r", 0.0, -0.22, 0.0, pid="part_lower_arm_r")]),
        node("upper_leg_l", -0.055,  0.54, 0.0, pid="part_upper_leg_l",
             children=[node("lower_leg_l", 0.0, -0.24, 0.0, pid="part_lower_leg_l")]),
        node("upper_leg_r",  0.055,  0.54, 0.0, pid="part_upper_leg_r",
             children=[node("lower_leg_r", 0.0, -0.24, 0.0, pid="part_lower_leg_r")]),
    ]

    if include_anchors:
        root_children += [
            anchor("head_anchor",    0.0,  1.28,  0.0),
            anchor("cape_anchor",    0.0,  1.00,  0.08),
            anchor("weapon_anchor",  0.24, 0.88, -0.02),
            anchor("shield_anchor", -0.24, 0.88,  0.02),
            anchor("ammo_anchor",   -0.12, 1.00,  0.10),
            anchor("body_anchor",    0.0,  0.88,  0.0),
            anchor("legs_anchor",    0.0,  0.44,  0.0),
            anchor("hands_anchor",   0.0,  0.68,  0.0),
            anchor("feet_anchor",    0.0,  0.08,  0.0),
        ]

    nodes = [{"id": root_id, "children": root_children}]

    return {
        "version": [0, 1],
        "id": model_id,
        "meshes": meshes,
        "materials": materials,
        "nodes": nodes,
        "animations": [],   # empty → hasClips=false → programmatic animation runs
    }


SKIN  = [0.84, 0.64, 0.44, 1.0]
BOOTS = [0.30, 0.15, 0.06, 1.0]

CHARACTERS = {
    "player_base": {
        "colors": {"skin": SKIN, "hair": [0.30, 0.18, 0.06, 1.0],
                   "shirt": [0.40, 0.46, 0.18, 1.0], "pants": [0.22, 0.42, 0.16, 1.0],
                   "boots": BOOTS},
        "include_anchors": True,
    },
    "npc_banker_base": {
        "colors": {"skin": SKIN, "hair": [0.22, 0.14, 0.05, 1.0],
                   "shirt": [0.12, 0.22, 0.45, 1.0], "pants": [0.18, 0.18, 0.22, 1.0],
                   "boots": BOOTS},
    },
    "npc_guide_base": {
        "colors": {"skin": SKIN, "hair": [0.32, 0.20, 0.06, 1.0],
                   "shirt": [0.25, 0.52, 0.28, 1.0], "pants": [0.25, 0.52, 0.28, 1.0],
                   "boots": BOOTS},
    },
    "npc_instructor_base": {
        "colors": {"skin": SKIN, "hair": [0.20, 0.12, 0.04, 1.0],
                   "shirt": [0.65, 0.12, 0.12, 1.0], "lower_arm": [0.52, 0.52, 0.55, 1.0],
                   "pants": [0.32, 0.30, 0.28, 1.0], "boots": [0.28, 0.26, 0.24, 1.0]},
    },
    "npc_goblin_base": {
        "colors": {"skin": [0.54, 0.60, 0.28, 1.0], "hair": [0.18, 0.10, 0.02, 1.0],
                   "shirt": [0.48, 0.56, 0.24, 1.0], "pants": [0.40, 0.48, 0.20, 1.0],
                   "boots": [0.28, 0.14, 0.06, 1.0]},
    },
}

os.makedirs(OUT_DIR, exist_ok=True)
for key, cfg in CHARACTERS.items():
    data = make_character(key, cfg["colors"], cfg.get("include_anchors", False))
    path = os.path.join(OUT_DIR, f"{key}.g3dj")
    with open(path, "w") as f:
        json.dump(data, f, separators=(",", ":"))
    print(f"Generated {key}.g3dj")
