"""
Generate G3DJ humanoid character models with single-bone world-space geometry.
All mesh vertices are in WORLD space — all parts hang off the root node.
This gives correct flat-shading under a directional light (OSRS style).

Outputs to art/models/ — the authoritative source that Maven copies to
client/src/main/resources/models/ during the generate-resources phase.

Part layout (world-space Y=0 at ground):
  torso:     Y 0.60-1.06  X ±0.11   Z ±0.07
  head:      Y 1.06-1.28  X ±0.09   Z ±0.08
  hair:      Y 1.28-1.34  X ±0.10   Z ±0.09
  upper_arm: Y 0.82-1.04  X ±0.11..0.20  Z ±0.045  (shirt sleeve)
  lower_arm: Y 0.60-0.82  X ±0.115..0.195 Z ±0.040 (skin / gauntlet)
  upper_leg: Y 0.36-0.60  X ±0.035..0.145 Z ±0.055 (pants)
  lower_leg: Y 0.08-0.36  X ±0.037..0.143 Z ±0.063 (boots)

Total height: Y 0.08 to 1.34 = 1.26 units
"""

import json, os

OUT_DIR = os.path.join(os.path.dirname(__file__), "..", "art", "models")


def box_verts(x0, y0, z0, x1, y1, z1):
    """World-space axis-aligned box. 6 faces × 4 vertices × 6 floats (pos+normal)."""
    faces = [
        [(x1,y0,z0), (x1,y0,z1), (x1,y1,z1), (x1,y1,z0)],   # +X
        [(x0,y0,z1), (x0,y0,z0), (x0,y1,z0), (x0,y1,z1)],   # -X
        [(x0,y1,z0), (x1,y1,z0), (x1,y1,z1), (x0,y1,z1)],   # +Y
        [(x0,y0,z1), (x1,y0,z1), (x1,y0,z0), (x0,y0,z0)],   # -Y
        [(x0,y0,z1), (x0,y1,z1), (x1,y1,z1), (x1,y0,z1)],   # +Z
        [(x1,y0,z0), (x1,y1,z0), (x0,y1,z0), (x0,y0,z0)],   # -Z
    ]
    normals = [(1,0,0), (-1,0,0), (0,1,0), (0,-1,0), (0,0,1), (0,0,-1)]
    out = []
    for face, n in zip(faces, normals):
        for p in face:
            out.extend([round(p[0],4), round(p[1],4), round(p[2],4),
                        float(n[0]),   float(n[1]),   float(n[2])])
    return out


def box_indices(base):
    idx = []
    for f in range(6):
        b = base + f * 4
        idx += [b, b+1, b+2, b+2, b+3, b]
    return idx


# (name, x0, y0, z0, x1, y1, z1)  — all world-space
PART_GEOM = [
    ("torso",  -0.11,  0.60, -0.07,   0.11,  1.06,  0.07),
    ("head",   -0.09,  1.06, -0.08,   0.09,  1.28,  0.08),
    ("hair",   -0.10,  1.28, -0.09,   0.10,  1.34,  0.09),
    ("ua_l",   -0.20,  0.82, -0.045, -0.11,  1.04,  0.045),
    ("la_l",   -0.195, 0.60, -0.040, -0.115, 0.82,  0.040),
    ("ua_r",    0.11,  0.82, -0.045,  0.20,  1.04,  0.045),
    ("la_r",    0.115, 0.60, -0.040,  0.195, 0.82,  0.040),
    ("ul_l",   -0.145, 0.36, -0.055, -0.035, 0.60,  0.055),
    ("ll_l",   -0.143, 0.08, -0.063, -0.037, 0.36,  0.063),
    ("ul_r",    0.035, 0.36, -0.055,  0.145, 0.60,  0.055),
    ("ll_r",    0.037, 0.08, -0.063,  0.143, 0.36,  0.063),
]


def make_character(model_id, colors, include_anchors=False, player_anims=False):
    """
    colors keys: skin, hair, shirt, lower_arm (opt, defaults to skin), pants, boots
    include_anchors: add equipment anchor child nodes (player only)
    player_anims: use player animation set vs NPC set
    """
    skin      = colors["skin"]
    hair      = colors["hair"]
    shirt     = colors["shirt"]
    lower_arm = colors.get("lower_arm", skin)
    pants     = colors["pants"]
    boots     = colors["boots"]

    # One color per part, same order as PART_GEOM
    part_colors = [
        shirt,      # torso
        skin,       # head
        hair,       # hair
        shirt,      # ua_l  (shirt sleeve)
        lower_arm,  # la_l  (skin forearm or gauntlet)
        shirt,      # ua_r
        lower_arm,  # la_r
        pants,      # ul_l
        boots,      # ll_l
        pants,      # ul_r
        boots,      # ll_r
    ]

    meshes = []
    materials = []
    node_parts = []

    for i, (name, *bounds) in enumerate(PART_GEOM):
        mid = f"mesh_{i}"
        pid = f"part_{i}"
        mat = f"mat_{i}"
        meshes.append({
            "id": mid,
            "attributes": ["POSITION", "NORMAL"],
            "vertices": box_verts(*bounds),
            "parts": [{"id": pid, "type": "TRIANGLES", "indices": box_indices(0)}]
        })
        materials.append({"id": mat, "diffuse": part_colors[i]})
        node_parts.append({"meshpartid": pid, "materialid": mat})

    bone_id = f"{model_id}_node"
    node = {"id": bone_id, "parts": node_parts}

    if include_anchors:
        node["children"] = [
            {"id": "head_anchor",   "translation": [ 0.0,  1.28,  0.0 ]},
            {"id": "cape_anchor",   "translation": [ 0.0,  1.00,  0.08]},
            {"id": "weapon_anchor", "translation": [ 0.24, 0.88, -0.02]},
            {"id": "shield_anchor", "translation": [-0.24, 0.88,  0.02]},
            {"id": "ammo_anchor",   "translation": [-0.12, 1.00,  0.10]},
            {"id": "body_anchor",   "translation": [ 0.0,  0.88,  0.0 ]},
            {"id": "legs_anchor",   "translation": [ 0.0,  0.44,  0.0 ]},
            {"id": "hands_anchor",  "translation": [ 0.0,  0.68,  0.0 ]},
            {"id": "feet_anchor",   "translation": [ 0.0,  0.08,  0.0 ]},
        ]

    def kf(t, ty=0.0):
        return {"keytime": t, "translation": [0.0, ty, 0.0],
                "rotation": [0.0, 0.0, 0.0, 1.0], "scale": [1.0, 1.0, 1.0]}

    if player_anims:
        animations = [
            {"id": "idle",   "bones": [{"boneId": bone_id, "keyframes": [
                kf(0.0), kf(1000.0)]}]},
            {"id": "walk",   "bones": [{"boneId": bone_id, "keyframes": [
                kf(0.0), kf(350.0, 0.03), kf(700.0)]}]},
            {"id": "pickup", "bones": [{"boneId": bone_id, "keyframes": [
                kf(0.0), kf(250.0, -0.06), kf(500.0)]}]},
            {"id": "chop",   "bones": [{"boneId": bone_id, "keyframes": [
                kf(0.0), kf(220.0, 0.04), kf(440.0)]}]},
            {"id": "mine",   "bones": [{"boneId": bone_id, "keyframes": [
                kf(0.0), kf(220.0, 0.04), kf(440.0)]}]},
            {"id": "fish",   "bones": [{"boneId": bone_id, "keyframes": [
                kf(0.0), kf(280.0, 0.03), kf(560.0)]}]},
            {"id": "sword",  "bones": [{"boneId": bone_id, "keyframes": [
                kf(0.0), kf(180.0, 0.05), kf(360.0)]}]},
            {"id": "spear",  "bones": [{"boneId": bone_id, "keyframes": [
                kf(0.0), kf(180.0, 0.05), kf(360.0)]}]},
        ]
    else:
        animations = [
            {"id": "idle",   "bones": [{"boneId": bone_id, "keyframes": [
                kf(0.0), kf(500.0, 0.007), kf(1000.0)]}]},
            {"id": "walk",   "bones": [{"boneId": bone_id, "keyframes": [
                kf(0.0), kf(200.0, 0.025), kf(400.0),
                kf(600.0, 0.025), kf(800.0)]}]},
            {"id": "action", "bones": [{"boneId": bone_id, "keyframes": [
                kf(0.0), kf(180.0, 0.015), kf(360.0),
                kf(540.0, 0.01125), kf(720.0)]}]},
        ]

    return {
        "version": [0, 1],
        "id": model_id,
        "meshes": meshes,
        "materials": materials,
        "nodes": [node],
        "animations": animations,
    }


SKIN  = [0.84, 0.64, 0.44, 1.0]
BOOTS = [0.30, 0.15, 0.06, 1.0]

CHARACTERS = {
    "player_base": {
        "colors": {
            "skin":  SKIN,
            "hair":  [0.30, 0.18, 0.06, 1.0],   # dark brown
            "shirt": [0.40, 0.46, 0.18, 1.0],   # olive-green
            "pants": [0.22, 0.42, 0.16, 1.0],   # medium green
            "boots": BOOTS,
        },
        "include_anchors": True,
        "player_anims": True,
    },
    "npc_banker_base": {
        "colors": {
            "skin":  SKIN,
            "hair":  [0.22, 0.14, 0.05, 1.0],
            "shirt": [0.12, 0.22, 0.45, 1.0],   # dark navy vest
            "pants": [0.18, 0.18, 0.22, 1.0],   # dark pants
            "boots": BOOTS,
        },
    },
    "npc_guide_base": {
        "colors": {
            "skin":  SKIN,
            "hair":  [0.32, 0.20, 0.06, 1.0],
            "shirt": [0.25, 0.52, 0.28, 1.0],   # forest green robe
            "pants": [0.25, 0.52, 0.28, 1.0],   # same green
            "boots": BOOTS,
        },
    },
    "npc_instructor_base": {
        "colors": {
            "skin":      SKIN,
            "hair":      [0.20, 0.12, 0.04, 1.0],
            "shirt":     [0.65, 0.12, 0.12, 1.0],   # red plate
            "lower_arm": [0.52, 0.52, 0.55, 1.0],   # metal gauntlet
            "pants":     [0.32, 0.30, 0.28, 1.0],   # dark metal
            "boots":     [0.28, 0.26, 0.24, 1.0],   # metal boots
        },
    },
    "npc_goblin_base": {
        "colors": {
            "skin":  [0.54, 0.60, 0.28, 1.0],   # goblin green
            "hair":  [0.18, 0.10, 0.02, 1.0],
            "shirt": [0.48, 0.56, 0.24, 1.0],
            "pants": [0.40, 0.48, 0.20, 1.0],
            "boots": [0.28, 0.14, 0.06, 1.0],
        },
    },
}


os.makedirs(OUT_DIR, exist_ok=True)

for key, cfg in CHARACTERS.items():
    data = make_character(
        key,
        cfg["colors"],
        include_anchors=cfg.get("include_anchors", False),
        player_anims=cfg.get("player_anims", False),
    )
    path = os.path.join(OUT_DIR, f"{key}.g3dj")
    with open(path, "w") as f:
        json.dump(data, f, separators=(",", ":"))
    print(f"Generated {key}.g3dj")
