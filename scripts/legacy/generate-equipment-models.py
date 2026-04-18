"""
Generate G3DJ equipment models sized to fit the octagonal-frustum player model.

Player model reference (world space, Y=up, tile at Y=0):
  torso     : Y 0.54–1.00, hw 0.135 (top/shoulders), 0.090 (bottom/waist), hd 0.080/0.065
  head      : Y 1.06–1.28, hw 0.082–0.090, hd 0.080–0.085
  upper arm : pivot at (±0.155, 1.04), hw 0.050, extends down 0.22 → bottom Y 0.82
  lower arm : pivot at (±0.155, 0.82), hw 0.038, extends down 0.22 → bottom Y 0.60
  upper leg : pivot at (±0.055, 0.54), hw 0.060, extends down 0.24 → bottom Y 0.30
  lower leg : pivot at (±0.055, 0.30), hw 0.052, extends down 0.28 → bottom Y 0.02

Anchor world positions (used by renderer, from player_base.g3dj):
  head_anchor   : (0,    1.28, 0)
  body_anchor   : (0,    0.88, 0)
  legs_anchor   : (0,    0.44, 0)
  weapon_anchor : (0.24, 0.88, -0.02)
  shield_anchor : (-0.24,0.88, 0.02)
  feet_anchor   : (0,    0.08, 0)

Renderer DEFAULT anchor offsets (subtracted when anchor found):
  HEAD  Y=1.18   BODY  Y=0.84   LEGS  Y=0.38
  WEAPON X=0.24 Y=0.84 Z=-0.02
  SHIELD X=-0.24 Y=0.84 Z=0.02
  FEET  Y=0.08

Model origin world position = anchor_world + (manifest_offset - default_offset).

Equipment uses POSITION+NORMAL (lit by directional environment light).
Geometry is boxes (6-face quads) with per-face flat normals.
Outputs to art/models/.
"""

import json, math, os

OUT_DIR = os.path.join(os.path.dirname(__file__), "..", "art", "models")


def box_verts(x0, y0, z0, x1, y1, z1):
    """
    24 vertices × 6 floats [px,py,pz,nx,ny,nz] for an axis-aligned box.
    Face order: +X, -X, +Y, -Y, +Z, -Z.
    Winding is CCW when viewed from the outside (LibGDX convention).
    """
    v = []
    # +X face  (x=x1, normal +X)
    v += [x1,y0,z0, 1,0,0,  x1,y0,z1, 1,0,0,  x1,y1,z1, 1,0,0,  x1,y1,z0, 1,0,0]
    # -X face  (x=x0, normal -X)
    v += [x0,y0,z1, -1,0,0, x0,y0,z0, -1,0,0, x0,y1,z0, -1,0,0, x0,y1,z1, -1,0,0]
    # +Y face  (y=y1, normal +Y)
    v += [x0,y1,z0, 0,1,0,  x1,y1,z0, 0,1,0,  x1,y1,z1, 0,1,0,  x0,y1,z1, 0,1,0]
    # -Y face  (y=y0, normal -Y)
    v += [x0,y0,z1, 0,-1,0, x1,y0,z1, 0,-1,0, x1,y0,z0, 0,-1,0, x0,y0,z0, 0,-1,0]
    # +Z face  (z=z1, normal +Z)
    v += [x1,y0,z1, 0,0,1,  x0,y0,z1, 0,0,1,  x0,y1,z1, 0,0,1,  x1,y1,z1, 0,0,1]
    # -Z face  (z=z0, normal -Z)
    v += [x0,y0,z0, 0,0,-1, x1,y0,z0, 0,0,-1, x1,y1,z0, 0,0,-1, x0,y1,z0, 0,0,-1]
    return [round(f, 6) for f in v]


def box_indices(base=0):
    """36 indices for a 24-vertex box (6 faces × 2 tris)."""
    idx = []
    for f in range(6):
        b = base + f * 4
        idx += [b, b+1, b+2,  b+2, b+3, b]
    return idx


def mesh_from_boxes(mid, boxes):
    """
    Build a G3DJ mesh dict from a list of (id, verts) tuples.
    Each box contributes 24 vertices; indices are offset accordingly.
    """
    all_verts = []
    parts = []
    for pid, verts in boxes:
        base = len(all_verts) // 6
        all_verts.extend(verts)
        parts.append({"id": pid, "type": "TRIANGLES", "indices": box_indices(base)})
    return {
        "id": mid,
        "attributes": ["POSITION", "NORMAL"],
        "vertices": all_verts,
        "parts": parts,
    }


def single_box_model(model_id, x0, y0, z0, x1, y1, z1, color):
    """Single-box equipment model."""
    verts = box_verts(x0, y0, z0, x1, y1, z1)
    r, g, b, a = color
    meshes = [{
        "id": "mesh_0",
        "attributes": ["POSITION", "NORMAL"],
        "vertices": verts,
        "parts": [{"id": "part_0", "type": "TRIANGLES", "indices": box_indices()}],
    }]
    materials = [{"id": "mat_0", "diffuse": [r, g, b, a]}]
    nodes = [{"id": f"{model_id}_node", "parts": [{"meshpartid": "part_0", "materialid": "mat_0"}]}]
    return {"version": [0, 1], "id": model_id, "meshes": meshes,
            "materials": materials, "nodes": nodes, "animations": []}


def multi_box_model(model_id, box_defs):
    """
    Multi-box equipment model.
    box_defs: list of (x0,y0,z0,x1,y1,z1,color) tuples.
    Returns a G3DJ dict with one mesh per box, all under one node.
    """
    meshes = []
    materials = []
    node_parts = []
    for i, (x0, y0, z0, x1, y1, z1, color) in enumerate(box_defs):
        mid = f"mesh_{i}"
        pid = f"part_{i}"
        mat_id = f"mat_{i}"
        r, g, b, a = color
        meshes.append({
            "id": mid,
            "attributes": ["POSITION", "NORMAL"],
            "vertices": box_verts(x0, y0, z0, x1, y1, z1),
            "parts": [{"id": pid, "type": "TRIANGLES", "indices": box_indices()}],
        })
        materials.append({"id": mat_id, "diffuse": [r, g, b, a]})
        node_parts.append({"meshpartid": pid, "materialid": mat_id})
    nodes = [{"id": f"{model_id}_node", "parts": node_parts}]
    return {"version": [0, 1], "id": model_id, "meshes": meshes,
            "materials": materials, "nodes": nodes, "animations": []}


# ---------------------------------------------------------------------------
# Colour palettes
# ---------------------------------------------------------------------------
BRONZE = (0.62, 0.46, 0.30, 1.0)
BRONZE_DARK = (0.38, 0.26, 0.14, 1.0)
IRON = (0.50, 0.50, 0.52, 1.0)
IRON_DARK = (0.28, 0.28, 0.30, 1.0)
STEEL = (0.68, 0.70, 0.72, 1.0)
STEEL_DARK = (0.38, 0.38, 0.40, 1.0)
MITHRIL = (0.28, 0.30, 0.60, 1.0)
MITHRIL_DARK = (0.16, 0.18, 0.38, 1.0)
ADAMANT = (0.14, 0.46, 0.22, 1.0)
ADAMANT_DARK = (0.08, 0.28, 0.12, 1.0)
RUNE = (0.22, 0.50, 0.78, 1.0)
RUNE_DARK = (0.10, 0.28, 0.50, 1.0)
BLACK = (0.12, 0.12, 0.14, 1.0)
BLACK_DARK = (0.06, 0.06, 0.08, 1.0)
DRAGON = (0.72, 0.12, 0.08, 1.0)
DRAGON_DARK = (0.44, 0.06, 0.04, 1.0)
WOOD = (0.58, 0.38, 0.18, 1.0)
WOOD_DARK = (0.36, 0.22, 0.08, 1.0)

# ---------------------------------------------------------------------------
# Geometry constants derived from player model dimensions
#
# Equipment model local Y=0 is placed at (anchor_world + offset_delta).
# Helmet  : origin at world Y=1.28  (head top / head_anchor when offset_y=1.18)
# Platebody: origin at world Y=0.86  (body_anchor=0.88, offset_y=0.82, default=0.84)
# Platelegs: origin at world Y=0.425 (legs_anchor=0.44, offset_y=0.365, default=0.38)
# Sword    : origin at weapon_anchor world (0.24, 0.88, -0.02)
# Shield   : origin at shield_anchor world (-0.24, 0.88, 0.02)
# Boots    : origin at world Y=0.08  (feet_anchor, offset_y=0.08)
# ---------------------------------------------------------------------------

def make_helm(model_id, main_col, trim_col=None):
    """
    Full helm: covers world Y 1.02–1.32 (local -0.26..+0.04).
    Width/depth ±0.100 (head is ±0.090, slight clearance).
    Optional visor trim strip at front (Z=+0.095..+0.102, small).
    """
    hw, hd = 0.100, 0.100
    y0, y1 = -0.26, 0.04
    boxes = [(- hw, y0, -hd,  hw, y1,  hd, main_col)]
    if trim_col:
        # Small visor ridge on front face
        boxes.append((-0.045, y0 + 0.06, hd,  0.045, y0 + 0.16, hd + 0.008, trim_col))
    return multi_box_model(model_id, boxes)


def make_platebody(model_id, main_col, trim_col=None):
    """
    Platebody covering world Y 0.54–1.00 → local -0.32..+0.14.
    Origin at world Y=0.86.
    Main box: ±0.155 wide, ±0.095 deep (covers torso hw=0.135+margin).
    Shoulder pauldrons: extend from ±0.155 to ±0.215 at shoulder height (local 0..+0.14).
    """
    # Main torso slab
    boxes = [
        (-0.155, -0.32, -0.095,  0.155,  0.14, 0.095, main_col),
        # Left pauldron
        (-0.215, -0.02, -0.060, -0.155,  0.14, 0.060, main_col),
        # Right pauldron
        ( 0.155, -0.02,  -0.060,  0.215,  0.14, 0.060, main_col),
    ]
    if trim_col:
        # Belt trim strip at bottom
        boxes.append((-0.156, -0.33, -0.096,  0.156, -0.30, 0.096, trim_col))
    return multi_box_model(model_id, boxes)


def make_platelegs(model_id, main_col, trim_col=None):
    """
    Platelegs covering world Y 0.02–0.54 → local -0.405..+0.115.
    Origin at world Y=0.425.
    Two leg boxes plus a hip strap at top.
    Leg pivot at X=±0.055; leg hw≈0.065 (leg is 0.060).
    """
    lw = 0.068   # half-width of each leg box (slightly wider than upper_leg hw=0.060)
    ld = 0.068   # half-depth
    # Hip section (joins both legs at waist)
    boxes = [
        # Hip panel
        (-0.130, -0.02, -0.085,  0.130,  0.115, 0.085, main_col),
        # Left leg
        (-0.055 - lw, -0.405, -ld,  -0.055 + lw, -0.02,  ld, main_col),
        # Right leg
        ( 0.055 - lw, -0.405, -ld,   0.055 + lw, -0.02,  ld, main_col),
    ]
    if trim_col:
        boxes.append((-0.131, -0.03, -0.086,  0.131, -0.00, 0.086, trim_col))
    return multi_box_model(model_id, boxes)


def make_bronze_platelegs(model_id):
    """
    Bronze reference platelegs with clearer OSRS silhouette:
    - separate left/right cuisse + greave volumes
    - visible knee cops
    - short split fauld panel at the hips
    """
    boxes = [
        # Waist / fauld panel
        (-0.128, 0.000, -0.086, 0.128, 0.120, 0.082, BRONZE),
        # Left/right upper leg shells (cuisses)
        (-0.126, -0.190, -0.080, -0.010, 0.000, 0.072, BRONZE),
        (0.010, -0.190, -0.080, 0.126, 0.000, 0.072, BRONZE),
        # Left/right lower leg shells (greaves)
        (-0.114, -0.405, -0.074, -0.018, -0.190, 0.080, BRONZE),
        (0.018, -0.405, -0.074, 0.114, -0.190, 0.080, BRONZE),
        # Knee cops
        (-0.122, -0.225, 0.052, -0.010, -0.175, 0.092, BRONZE_DARK),
        (0.010, -0.225, 0.052, 0.122, -0.175, 0.092, BRONZE_DARK),
        # Inner split trim to avoid tube-like read
        (-0.012, -0.405, -0.082, 0.012, 0.090, -0.050, BRONZE_DARK),
    ]
    return multi_box_model(model_id, boxes)


def make_sq_shield(model_id, main_col, trim_col=None):
    """
    Sq shield: flat slab held in left hand.
    Local coords centred on shield face.
    Width 0.26, height 0.32, depth 0.04.
    No manifest anchor delta — shield is positioned by weapon_anchor logic.
    """
    hw, hh, hd = 0.130, 0.160, 0.020
    boxes = [(-hw, -hh, -hd,  hw, hh, hd, main_col)]
    if trim_col:
        # Rim trim
        inset = 0.015
        boxes.append((-hw, -hh, hd,  hw, hh, hd + 0.006, trim_col))
    return multi_box_model(model_id, boxes)


def make_bronze_sq_shield(model_id):
    """
    Bronze reference square shield with thickness and grip readability.
    Face (+Z) is outward; negative Z side is arm-side.
    """
    boxes = [
        # Main wooden/metal body with slight taper
        (-0.126, -0.168, -0.024, 0.126, 0.168, 0.018, BRONZE),
        # Raised rim frame
        (-0.136, -0.178, 0.014, 0.136, 0.178, 0.034, BRONZE_DARK),
        # Central boss
        (-0.034, -0.032, 0.030, 0.034, 0.036, 0.052, BRONZE_DARK),
        # Arm-side grip block / strap mount
        (-0.020, -0.050, -0.044, 0.020, 0.080, -0.020, BRONZE_DARK),
    ]
    return multi_box_model(model_id, boxes)


def make_scimitar(model_id, blade_col, handle_col):
    """
    Scimitar: blade (tall thin box, slightly angled via Z offset at top) + grip + pommel.
    Blade local Y: 0.04 (guard) to 0.50 (tip).
    Grip local Y: -0.14 to 0.04.
    """
    boxes = [
        # Blade
        (-0.012,  0.04, -0.006,  0.012,  0.50,  0.006, blade_col),
        # Guard (cross-guard)
        (-0.055,  0.02, -0.010,  0.055,  0.06,  0.010, blade_col),
        # Grip
        (-0.014, -0.12, -0.014,  0.014,  0.04,  0.014, handle_col),
        # Pommel
        (-0.022, -0.16, -0.022,  0.022, -0.10,  0.022, handle_col),
    ]
    return multi_box_model(model_id, boxes)


def make_boots(model_id, main_col):
    """
    Boots: cover world Y 0.02–0.30 (lower_leg bottom area).
    Local Y relative to feet_anchor at world Y=0.08:
      local Y: 0.02-0.08=-0.06 to 0.12-0.08=+0.04 per foot.
    Two side-by-side foot boxes.
    """
    bw, bh, bd = 0.060, 0.100, 0.068
    boxes = [
        # Left boot
        (-0.055 - bw, -0.06, -bd,  -0.055 + bw,  0.04,  bd, main_col),
        # Right boot
        ( 0.055 - bw, -0.06, -bd,   0.055 + bw,  0.04,  bd, main_col),
    ]
    return multi_box_model(model_id, boxes)


def make_gauntlets(model_id, main_col):
    """
    Gauntlets covering wrist/hand area.
    Placed at hands_anchor (world Y=0.68). Local Y small.
    Two small boxes symmetrical about X axis.
    """
    gw, gh, gd = 0.038, 0.060, 0.038
    boxes = [
        (-0.155 - gw, -gh / 2, -gd,  -0.155 + gw,  gh / 2, gd, main_col),
        ( 0.155 - gw, -gh / 2, -gd,   0.155 + gw,  gh / 2, gd, main_col),
    ]
    return multi_box_model(model_id, boxes)


# ---------------------------------------------------------------------------
# Equipment definitions: (model_id → make_fn(*args))
# ---------------------------------------------------------------------------
EQUIPMENT = {
    # Helmets
    "equip_head_bronze_full_helm":  lambda: make_helm("equip_head_bronze_full_helm",  BRONZE, BRONZE_DARK),
    "equip_head_iron_full_helm":    lambda: make_helm("equip_head_iron_full_helm",    IRON,   IRON_DARK),
    "equip_head_steel_full_helm":   lambda: make_helm("equip_head_steel_full_helm",   STEEL,  STEEL_DARK),
    "equip_head_mithril_full_helm": lambda: make_helm("equip_head_mithril_full_helm", MITHRIL, MITHRIL_DARK),
    "equip_head_adamant_full_helm": lambda: make_helm("equip_head_adamant_full_helm", ADAMANT, ADAMANT_DARK),
    "equip_head_rune_full_helm":    lambda: make_helm("equip_head_rune_full_helm",    RUNE,   RUNE_DARK),
    "equip_head_black_full_helm":   lambda: make_helm("equip_head_black_full_helm",   BLACK,  BLACK_DARK),
    "equip_head_dragon_full_helm":  lambda: make_helm("equip_head_dragon_full_helm",  DRAGON, DRAGON_DARK),

    # Platebodies
    "equip_body_bronze_platebody":  lambda: make_platebody("equip_body_bronze_platebody",  BRONZE, BRONZE_DARK),
    "equip_body_iron_platebody":    lambda: make_platebody("equip_body_iron_platebody",    IRON,   IRON_DARK),
    "equip_body_steel_platebody":   lambda: make_platebody("equip_body_steel_platebody",   STEEL,  STEEL_DARK),
    "equip_body_mithril_platebody": lambda: make_platebody("equip_body_mithril_platebody", MITHRIL, MITHRIL_DARK),
    "equip_body_adamant_platebody": lambda: make_platebody("equip_body_adamant_platebody", ADAMANT, ADAMANT_DARK),
    "equip_body_rune_platebody":    lambda: make_platebody("equip_body_rune_platebody",    RUNE,   RUNE_DARK),
    "equip_body_black_platebody":   lambda: make_platebody("equip_body_black_platebody",   BLACK,  BLACK_DARK),
    "equip_body_dragon_chainbody":  lambda: make_platebody("equip_body_dragon_chainbody",  DRAGON, DRAGON_DARK),

    # Platelegs
    "equip_legs_bronze_platelegs":  lambda: make_bronze_platelegs("equip_legs_bronze_platelegs"),
    "equip_legs_iron_platelegs":    lambda: make_platelegs("equip_legs_iron_platelegs",    IRON,   IRON_DARK),
    "equip_legs_steel_platelegs":   lambda: make_platelegs("equip_legs_steel_platelegs",   STEEL,  STEEL_DARK),
    "equip_legs_mithril_platelegs": lambda: make_platelegs("equip_legs_mithril_platelegs", MITHRIL, MITHRIL_DARK),
    "equip_legs_adamant_platelegs": lambda: make_platelegs("equip_legs_adamant_platelegs", ADAMANT, ADAMANT_DARK),
    "equip_legs_rune_platelegs":    lambda: make_platelegs("equip_legs_rune_platelegs",    RUNE,   RUNE_DARK),
    "equip_legs_black_platelegs":   lambda: make_platelegs("equip_legs_black_platelegs",   BLACK,  BLACK_DARK),
    "equip_legs_dragon_platelegs":  lambda: make_platelegs("equip_legs_dragon_platelegs",  DRAGON, DRAGON_DARK),

    # Sq shields
    "equip_shield_bronze_sq":       lambda: make_bronze_sq_shield("equip_shield_bronze_sq"),
    "equip_shield_iron_sq":         lambda: make_sq_shield("equip_shield_iron_sq",      IRON,   IRON_DARK),
    "equip_shield_steel_sq":        lambda: make_sq_shield("equip_shield_steel_sq",     STEEL,  STEEL_DARK),
    "equip_shield_mithril_sq":      lambda: make_sq_shield("equip_shield_mithril_sq",   MITHRIL, MITHRIL_DARK),
    "equip_shield_adamant_sq":      lambda: make_sq_shield("equip_shield_adamant_sq",   ADAMANT, ADAMANT_DARK),
    "equip_shield_rune_sq":         lambda: make_sq_shield("equip_shield_rune_sq",      RUNE,   RUNE_DARK),
    "equip_shield_black_sq":        lambda: make_sq_shield("equip_shield_black_sq",     BLACK,  BLACK_DARK),
    "equip_shield_dragon_sq":       lambda: make_sq_shield("equip_shield_dragon_sq",    DRAGON, DRAGON_DARK),
    "equip_shield_wooden":          lambda: make_sq_shield("equip_shield_wooden",        WOOD,  WOOD_DARK),

    # Scimitars
    "equip_weapon_bronze_scimitar":  lambda: make_scimitar("equip_weapon_bronze_scimitar",  BRONZE, WOOD_DARK),
    "equip_weapon_iron_scimitar":    lambda: make_scimitar("equip_weapon_iron_scimitar",    IRON,   WOOD_DARK),
    "equip_weapon_steel_scimitar":   lambda: make_scimitar("equip_weapon_steel_scimitar",   STEEL,  WOOD_DARK),
    "equip_weapon_mithril_scimitar": lambda: make_scimitar("equip_weapon_mithril_scimitar", MITHRIL, WOOD_DARK),
    "equip_weapon_adamant_scimitar": lambda: make_scimitar("equip_weapon_adamant_scimitar", ADAMANT, WOOD_DARK),
    "equip_weapon_rune_scimitar":    lambda: make_scimitar("equip_weapon_rune_scimitar",    RUNE,   WOOD_DARK),
    "equip_weapon_black_scimitar":   lambda: make_scimitar("equip_weapon_black_scimitar",   BLACK,  WOOD_DARK),
    "equip_weapon_dragon_scimitar":  lambda: make_scimitar("equip_weapon_dragon_scimitar",  DRAGON, WOOD_DARK),
}

os.makedirs(OUT_DIR, exist_ok=True)
for key, factory in EQUIPMENT.items():
    data = factory()
    path = os.path.join(OUT_DIR, f"{key}.g3dj")
    with open(path, "w") as f:
        json.dump(data, f, separators=(",", ":"))
    print(f"Generated {key}.g3dj")
