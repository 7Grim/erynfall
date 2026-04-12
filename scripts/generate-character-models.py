"""
Generate G3DJ humanoid character models with multi-bone hierarchy for
programmatic OSRS-style walk animation.

Each limb is a separate node with bone-local vertex coordinates so
Renderer3DExperimental.applyWalkAnimation() can rotate them independently.
Vertex COLOR (pre-baked per-face shading) replaces NORMAL — renderer uses
characterEnvironment (AmbientLight=white, no directional light).

Empty animations array → hasClips=false → programmatic code runs instead
of AnimationController.

Outputs to art/models/ (Maven copies to client/src/main/resources/models/).
"""

import json, os

OUT_DIR = os.path.join(os.path.dirname(__file__), "..", "art", "models")

# Per-face brightness — OSRS light from upper-front-left.
# Order matches box_verts faces: +X, -X, +Y, -Y, +Z, -Z
FACE_SHADE = [0.70, 0.78, 0.92, 0.35, 0.85, 0.50]


def box_verts(x0, y0, z0, x1, y1, z1, rgba):
    """Bone-local box with pre-baked per-face shading → [px,py,pz,r,g,b,a] × 24."""
    r0, g0, b0, _ = rgba
    faces = [
        [(x1,y0,z0),(x1,y0,z1),(x1,y1,z1),(x1,y1,z0)],  # +X
        [(x0,y0,z1),(x0,y0,z0),(x0,y1,z0),(x0,y1,z1)],  # -X
        [(x0,y1,z0),(x1,y1,z0),(x1,y1,z1),(x0,y1,z1)],  # +Y
        [(x0,y0,z1),(x1,y0,z1),(x1,y0,z0),(x0,y0,z0)],  # -Y
        [(x0,y0,z1),(x0,y1,z1),(x1,y1,z1),(x1,y0,z1)],  # +Z
        [(x1,y0,z0),(x1,y1,z0),(x0,y1,z0),(x0,y0,z0)],  # -Z
    ]
    out = []
    for face, s in zip(faces, FACE_SHADE):
        r, g, b = round(r0*s,4), round(g0*s,4), round(b0*s,4)
        for p in face:
            out.extend([round(p[0],4), round(p[1],4), round(p[2],4), r, g, b, 1.0])
    return out


def box_indices(base):
    idx = []
    for f in range(6):
        b = base + f*4
        idx += [b, b+1, b+2, b+2, b+3, b]
    return idx


def mesh(mid, pid, bounds, color):
    return {
        "id": mid,
        "attributes": ["POSITION", "COLOR"],
        "vertices": box_verts(*bounds, color),
        "parts": [{"id": pid, "type": "TRIANGLES", "indices": box_indices(0)}]
    }


def part(pid): return {"meshpartid": pid, "materialid": "mat_white"}


def node(nid, tx, ty, tz, pid=None, children=None):
    n = {"id": nid, "translation": [tx, ty, tz]}
    if pid: n["parts"] = [part(pid)]
    if children: n["children"] = children
    return n


def anchor(nid, tx, ty, tz):
    return {"id": nid, "translation": [tx, ty, tz]}


# Bone-local box bounds: (x0, y0, z0, x1, y1, z1)
# Y=0 at pivot. Static parts grow UP (+Y). Limbs hang DOWN (-Y).
BOUNDS = {
    "torso":      (-0.11,  0.00, -0.07,  0.11,  0.46,  0.07),
    "head":       (-0.09,  0.00, -0.08,  0.09,  0.22,  0.08),
    "hair":       (-0.10,  0.00, -0.09,  0.10,  0.06,  0.09),
    "upper_arm_l":(-0.045,-0.22,-0.045,  0.045, 0.00,  0.045),
    "lower_arm_l":(-0.040,-0.22,-0.040,  0.040, 0.00,  0.040),
    "upper_arm_r":(-0.045,-0.22,-0.045,  0.045, 0.00,  0.045),
    "lower_arm_r":(-0.040,-0.22,-0.040,  0.040, 0.00,  0.040),
    "upper_leg_l":(-0.055,-0.24,-0.055,  0.055, 0.00,  0.055),
    "lower_leg_l":(-0.053,-0.28,-0.063,  0.053, 0.00,  0.063),
    "upper_leg_r":(-0.055,-0.24,-0.055,  0.055, 0.00,  0.055),
    "lower_leg_r":(-0.053,-0.28,-0.063,  0.053, 0.00,  0.063),
}

PARTS = ["torso","head","hair",
         "upper_arm_l","lower_arm_l","upper_arm_r","lower_arm_r",
         "upper_leg_l","lower_leg_l","upper_leg_r","lower_leg_r"]


def make_character(model_id, colors, include_anchors=False):
    skin      = colors["skin"]
    hair_c    = colors["hair"]
    shirt     = colors["shirt"]
    la_color  = colors.get("lower_arm", skin)
    pants     = colors["pants"]
    boots     = colors["boots"]

    color_map = {
        "torso":       shirt,
        "head":        skin,
        "hair":        hair_c,
        "upper_arm_l": shirt,   "upper_arm_r": shirt,
        "lower_arm_l": la_color,"lower_arm_r": la_color,
        "upper_leg_l": pants,   "upper_leg_r": pants,
        "lower_leg_l": boots,   "lower_leg_r": boots,
    }

    meshes = [mesh(f"mesh_{p}", f"part_{p}", BOUNDS[p], color_map[p]) for p in PARTS]
    materials = [{"id": "mat_white", "diffuse": [1.0, 1.0, 1.0, 1.0]}]

    # Build node hierarchy
    root_id = f"{model_id}_node"
    root_children = [
        node("torso",        0.0,   0.60, 0.0, pid="part_torso"),
        node("head",         0.0,   1.06, 0.0, pid="part_head",
             children=[node("hair", 0.0, 0.22, 0.0, pid="part_hair")]),
        node("upper_arm_l", -0.155, 1.04, 0.0, pid="part_upper_arm_l",
             children=[node("lower_arm_l", 0.0, -0.22, 0.0, pid="part_lower_arm_l")]),
        node("upper_arm_r",  0.155, 1.04, 0.0, pid="part_upper_arm_r",
             children=[node("lower_arm_r", 0.0, -0.22, 0.0, pid="part_lower_arm_r")]),
        node("upper_leg_l", -0.090, 0.60, 0.0, pid="part_upper_leg_l",
             children=[node("lower_leg_l", 0.0, -0.24, 0.0, pid="part_lower_leg_l")]),
        node("upper_leg_r",  0.090, 0.60, 0.0, pid="part_upper_leg_r",
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
        "colors": {"skin": SKIN, "hair": [0.30,0.18,0.06,1.0],
                   "shirt": [0.40,0.46,0.18,1.0], "pants": [0.22,0.42,0.16,1.0], "boots": BOOTS},
        "include_anchors": True,
    },
    "npc_banker_base": {
        "colors": {"skin": SKIN, "hair": [0.22,0.14,0.05,1.0],
                   "shirt": [0.12,0.22,0.45,1.0], "pants": [0.18,0.18,0.22,1.0], "boots": BOOTS},
    },
    "npc_guide_base": {
        "colors": {"skin": SKIN, "hair": [0.32,0.20,0.06,1.0],
                   "shirt": [0.25,0.52,0.28,1.0], "pants": [0.25,0.52,0.28,1.0], "boots": BOOTS},
    },
    "npc_instructor_base": {
        "colors": {"skin": SKIN, "hair": [0.20,0.12,0.04,1.0],
                   "shirt": [0.65,0.12,0.12,1.0], "lower_arm": [0.52,0.52,0.55,1.0],
                   "pants": [0.32,0.30,0.28,1.0], "boots": [0.28,0.26,0.24,1.0]},
    },
    "npc_goblin_base": {
        "colors": {"skin": [0.54,0.60,0.28,1.0], "hair": [0.18,0.10,0.02,1.0],
                   "shirt": [0.48,0.56,0.24,1.0], "pants": [0.40,0.48,0.20,1.0],
                   "boots": [0.28,0.14,0.06,1.0]},
    },
}

os.makedirs(OUT_DIR, exist_ok=True)
for key, cfg in CHARACTERS.items():
    data = make_character(key, cfg["colors"], cfg.get("include_anchors", False))
    path = os.path.join(OUT_DIR, f"{key}.g3dj")
    with open(path, "w") as f:
        json.dump(data, f, separators=(",", ":"))
    print(f"Generated {key}.g3dj")
