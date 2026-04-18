#!/usr/bin/env python3
"""
convert-glb-to-player.py

Converts a character GLB (e.g. john.glb) → player_base.g3dj.
Preserves ALL original geometry — no decimation, no approximation.
The exact polygon faces of the source model appear in-game.

Usage:
    python3 scripts/convert-glb-to-player.py art/blender/john.glb [output_id]
"""

import sys, os, json, math, struct, shutil
import numpy as np

SCRIPT_DIR   = os.path.dirname(__file__)
OUT_DIR      = os.path.abspath(os.path.join(SCRIPT_DIR, "..", "art", "models"))
RESOURCE_DIR = os.path.abspath(os.path.join(SCRIPT_DIR, "..", "client", "src", "main", "resources", "models"))

# ─── COWBOY PALETTE ───────────────────────────────────────────────────────────
# (user confirmed colours are correct)

PAL = {
    "hat_top":  [0.22, 0.16, 0.08, 1.0],
    "hat":      [0.30, 0.22, 0.12, 1.0],
    "hair":     [0.25, 0.18, 0.10, 1.0],
    "skin":     [0.85, 0.60, 0.35, 1.0],
    "bandana":  [0.65, 0.15, 0.10, 1.0],
    "vest":     [0.28, 0.18, 0.09, 1.0],
    "shirt":    [0.55, 0.48, 0.36, 1.0],
    "belt":     [0.22, 0.14, 0.07, 1.0],
    "holster":  [0.25, 0.16, 0.08, 1.0],
    "pants":    [0.20, 0.25, 0.32, 1.0],
    "boots":    [0.22, 0.14, 0.08, 1.0],
    "spurs":    [0.65, 0.52, 0.22, 1.0],
}

# ─── GLB PARSING ──────────────────────────────────────────────────────────────

GLTF_COMP = {5120:('b',1), 5121:('B',1), 5122:('h',2), 5123:('H',2), 5125:('I',4), 5126:('f',4)}
GLTF_NCOMP = {'SCALAR':1,'VEC2':2,'VEC3':3,'VEC4':4,'MAT2':4,'MAT3':9,'MAT4':16}


def parse_glb(path):
    with open(path, 'rb') as f:
        raw = f.read()
    if struct.unpack_from('<I', raw, 0)[0] != 0x46546C67:
        raise ValueError("Not a GLB file")
    offset, gltf, bin_data = 12, None, b''
    while offset < len(raw):
        cl, ct = struct.unpack_from('<II', raw, offset)
        body = raw[offset+8:offset+8+cl]
        if ct == 0x4E4F534A: gltf = json.loads(body)
        elif ct in (0x4E4942, 0x004E4942): bin_data = body
        offset += 8 + cl
    return gltf, bin_data


def read_accessor(gltf, bin_data, idx):
    acc = gltf['accessors'][idx]
    bv  = gltf['bufferViews'][acc['bufferView']]
    dc, bs = GLTF_COMP[acc['componentType']]
    nc     = GLTF_NCOMP[acc['type']]
    count  = acc['count']
    base   = bv.get('byteOffset', 0) + acc.get('byteOffset', 0)
    stride = bv.get('byteStride', nc * bs)
    out = np.zeros((count, nc), dtype=np.float32)
    for i in range(count):
        b0 = base + i * stride
        for j in range(nc):
            out[i, j] = struct.unpack_from(f'<{dc}', bin_data, b0 + j * bs)[0]
    return out


def load_mesh(gltf, bin_data):
    """Return positions (N,3) float32 and triangles (M,3) int32 from first mesh."""
    prim  = gltf['meshes'][0]['primitives'][0]
    attrs = prim['attributes']
    if 'POSITION' not in attrs:
        raise ValueError("No POSITION attribute in GLB")
    pos = read_accessor(gltf, bin_data, attrs['POSITION'])
    if 'indices' in prim:
        idx = read_accessor(gltf, bin_data, prim['indices'])
        tri = idx.reshape(-1, 3).astype(np.int32)
    else:
        tri = np.arange(len(pos), dtype=np.int32).reshape(-1, 3)
    print(f"  Loaded: {len(pos):,} vertices  {len(tri):,} triangles")
    return pos.copy(), tri


# ─── GEOMETRY NORMALISATION ───────────────────────────────────────────────────

def normalise_to_180cm(pos):
    """Translate feet to Y=0, scale so character is 1.80m tall.
    Centre horizontally so spine is at X=0, Z=0."""
    pos = pos.copy()
    y_lo, y_hi = float(pos[:,1].min()), float(pos[:,1].max())
    scale = 1.80 / (y_hi - y_lo) if y_hi > y_lo else 1.0
    pos[:,1] = (pos[:,1] - y_lo) * scale
    pos[:,0] = (pos[:,0] - (pos[:,0].max() + pos[:,0].min()) / 2.0) * scale
    pos[:,2] = (pos[:,2] - (pos[:,2].max() + pos[:,2].min()) / 2.0) * scale
    return pos


# ─── SMOOTH NORMALS ───────────────────────────────────────────────────────────

def compute_normals(pos, tri):
    """Area-weighted smooth vertex normals."""
    n = np.zeros_like(pos)
    v0 = pos[tri[:,0]]; v1 = pos[tri[:,1]]; v2 = pos[tri[:,2]]
    fn = np.cross(v1 - v0, v2 - v0)   # area-weighted face normal
    np.add.at(n, tri[:,0], fn)
    np.add.at(n, tri[:,1], fn)
    np.add.at(n, tri[:,2], fn)
    nrm = np.linalg.norm(n, axis=1, keepdims=True)
    nrm[nrm < 1e-8] = 1.0
    return (n / nrm).astype(np.float32)


# ─── COLOUR ASSIGNMENT ────────────────────────────────────────────────────────

LIGHT = np.array([0.40, 0.80, 0.35], dtype=np.float32)
LIGHT /= np.linalg.norm(LIGHT)
AMBIENT = 0.42
DIFFUSE = 0.58


def vertex_color(yn, xn, x_raw):
    """Return palette key for a vertex at normalised Y (0=feet,1=head) and
    normalised |X| (0=spine, 1=outermost shoulder width)."""
    if yn > 0.92:
        return "hat_top"
    if yn > 0.86:
        return "hat"
    if yn > 0.81:
        return "hair"
    if yn > 0.74:
        return "skin"      # face / head sides
    if yn > 0.68:
        return "bandana"   # neck / bandana triangle
    if yn > 0.38:
        # Torso + arm zone — distinguish by X width
        if xn > 0.54:
            return "shirt"     # arm (shirt visible, vest open)
        return "vest"
    if yn > 0.35:
        # Belt + holster zone
        if xn > 0.35 and x_raw > 0:   # holster on right hip
            return "holster"
        return "belt"
    if yn > 0.11:
        return "pants"
    if yn > 0.04:
        return "boots"
    return "spurs"


def assign_colors(pos, normals):
    """Per-vertex RGBA with OSRS-style diffuse shading baked in."""
    y_lo = float(pos[:,1].min()); y_hi = float(pos[:,1].max())
    y_range = y_hi - y_lo if y_hi > y_lo else 1.0
    x_lo = float(pos[:,0].min()); x_hi = float(pos[:,0].max())
    x_half = (x_hi - x_lo) / 2.0 if x_hi > x_lo else 1.0

    yn = (pos[:,1] - y_lo) / y_range
    xn = np.abs(pos[:,0]) / x_half        # 0=spine, 1=edge

    dots = np.clip(normals @ LIGHT, 0, 1)
    brightness = AMBIENT + DIFFUSE * dots  # shape (N,)

    rgba = np.zeros((len(pos), 4), dtype=np.float32)
    for i in range(len(pos)):
        key = vertex_color(float(yn[i]), float(xn[i]), float(pos[i, 0]))
        c   = PAL[key]
        b   = float(brightness[i])
        rgba[i, 0] = min(1.0, c[0] * b)
        rgba[i, 1] = min(1.0, c[1] * b)
        rgba[i, 2] = min(1.0, c[2] * b)
        rgba[i, 3] = c[3]

    return rgba


# ─── BODY-PART SPLITTING ──────────────────────────────────────────────────────
#
# Assign each TRIANGLE to a body-part node by its centroid's (yn, xn, x_sign).
# ALL THREE VERTICES of every triangle are added to that node's mesh.
# Vertices on part-boundaries appear in both adjacent parts — the tiny duplication
# (~5% of total) is necessary to keep the surface watertight (no holes).

BODY_PARTS_ORDER = [
    "head", "hair", "neck",
    "upper_arm_l", "upper_arm_r",
    "lower_arm_l", "lower_arm_r",
    "torso",
    "upper_leg_l", "upper_leg_r",
    "lower_leg_l", "lower_leg_r",
]


def classify_triangle(cyn, cxn, cx_raw):
    """Return engine node name for a triangle centroid."""
    if cyn > 0.86:   return "head"
    if cyn > 0.79:   return "hair"
    if cyn > 0.68:   return "neck"
    if cyn > 0.38:
        # Torso + arm zone: wide X → arm, narrow X → torso
        if cxn > 0.52:
            return "upper_arm_l" if cx_raw < 0 else "upper_arm_r"
        return "torso"
    if cyn > 0.20:
        # Lower arm / upper leg zone
        if cxn > 0.46:
            return "lower_arm_l" if cx_raw < 0 else "lower_arm_r"
        return "upper_leg_l" if cx_raw < 0 else "upper_leg_r"
    if cyn > 0.05:
        return "lower_leg_l" if cx_raw < 0 else "lower_leg_r"
    # Boots / spurs → fold into lower_leg
    return "lower_leg_l" if cx_raw <= 0 else "lower_leg_r"


def split_mesh(pos, tri, normals, colors):
    """Split full mesh into body-part submeshes.
    Returns dict: node_id → {'pos', 'normals', 'colors', 'tri'}"""
    y_lo = float(pos[:,1].min()); y_range = float(pos[:,1].max()) - y_lo
    x_half = float(np.abs(pos[:,0]).max()) or 1.0

    # Centroid arrays
    c0 = pos[tri[:,0]]; c1 = pos[tri[:,1]]; c2 = pos[tri[:,2]]
    cen   = (c0 + c1 + c2) / 3.0
    cyn   = (cen[:,1] - y_lo) / (y_range if y_range > 1e-6 else 1.0)
    cxn   = np.abs(cen[:,0]) / x_half
    cx_raw = cen[:,0]

    # Map each triangle index → node name
    buckets = {k: [] for k in BODY_PARTS_ORDER}
    for t_idx in range(len(tri)):
        nid = classify_triangle(float(cyn[t_idx]), float(cxn[t_idx]), float(cx_raw[t_idx]))
        buckets[nid].append(t_idx)

    result = {}
    for nid in BODY_PARTS_ORDER:
        t_indices = buckets[nid]
        if not t_indices:
            continue
        node_tris = tri[t_indices]           # (K, 3) — original global vertex indices
        used_verts = np.unique(node_tris)    # sorted unique vertex indices

        # Remap global → local
        remap = np.empty(len(pos), dtype=np.int32)
        remap[used_verts] = np.arange(len(used_verts), dtype=np.int32)
        local_tri = remap[node_tris]

        result[nid] = {
            "pos":     pos[used_verts],
            "normals": normals[used_verts],
            "colors":  colors[used_verts],
            "tri":     local_tri,
        }
        print(f"    {nid:<16s}  {len(used_verts):6,}v  {len(local_tri):6,}t")

    return result


# ─── G3DJ CONSTRUCTION ────────────────────────────────────────────────────────

def r4(v): return round(float(v), 4)

# Limb segments must rotate around the TOP of the segment (the joint),
# not the bounding-box centre.  Without this, rotation pivots at mid-thigh
# instead of the hip, and mid-shin instead of the knee — body parts appear
# detached during animation.
_JOINT_TOP   = {"upper_leg_l","upper_leg_r","lower_leg_l","lower_leg_r",
                 "upper_arm_l","upper_arm_r","lower_arm_l","lower_arm_r"}
_JOINT_BOTT  = {"head"}   # head rotates at the neck (its bottom edge)


def make_g3dj_part(node_id, d):
    """Build G3DJ mesh, node and material for one body part."""
    pos, normals, colors, tri = d["pos"], d["normals"], d["colors"], d["tri"]

    # Determine the joint pivot in world space
    x_ctr = float((pos[:,0].max() + pos[:,0].min()) / 2.0)
    y_min = float(pos[:,1].min())
    y_max = float(pos[:,1].max())
    y_ctr = (y_min + y_max) / 2.0
    z_ctr = float((pos[:,2].max() + pos[:,2].min()) / 2.0)

    if node_id in _JOINT_TOP:
        # Hip / knee / shoulder / elbow — rotate around the top of the segment
        pivot = np.array([x_ctr, y_max, z_ctr], dtype=np.float32)
    elif node_id in _JOINT_BOTT:
        # Head — rotate around the neck (bottom edge of the head mesh)
        pivot = np.array([x_ctr, y_min, z_ctr], dtype=np.float32)
    else:
        # Torso, neck, hair: bounding-box centre is fine
        pivot = np.array([x_ctr, y_ctr, z_ctr], dtype=np.float32)

    local_pos = pos - pivot

    # Build vertex buffer: x y z r g b a  (7 floats per vertex)
    verts = []
    for i in range(len(local_pos)):
        px, py, pz = local_pos[i]
        r, g, b, a = colors[i]
        verts.extend([r4(px), r4(py), r4(pz), r4(r), r4(g), r4(b), r4(a)])

    g3dj_mesh = {
        "id":         f"mesh_{node_id}",
        "attributes": ["POSITION", "COLOR"],
        "vertices":   verts,
        "parts":      [{"id": f"part_{node_id}", "type": "TRIANGLES",
                        "indices": tri.flatten().tolist()}]
    }

    g3dj_node = {
        "id":          node_id,
        "translation": [r4(pivot[0]), r4(pivot[1]), r4(pivot[2])],
        "parts":       [{"meshpartid": f"part_{node_id}",
                         "materialid": f"mat_{node_id}"}]
    }

    avg = colors.mean(axis=0)
    g3dj_mat = {
        "id":        f"mat_{node_id}",
        "diffuse":   [r4(avg[0]), r4(avg[1]), r4(avg[2])],
        "ambient":   [0.4, 0.4, 0.4],
        "emissive":  [0.0, 0.0, 0.0],
        "shininess": 0.0,
        "opacity":   1.0,
    }

    return g3dj_mesh, g3dj_node, g3dj_mat


# ─── EQUIPMENT ANCHOR NODES ───────────────────────────────────────────────────

ANCHORS = [
    ("head_anchor",    0.00, 1.18,  0.00),
    ("cape_anchor",    0.00, 1.10,  0.00),
    ("ammo_anchor",    0.00, 0.90,  0.00),
    ("body_anchor",    0.00, 0.84,  0.00),
    ("weapon_anchor",  0.24, 0.84,  0.00),
    ("shield_anchor", -0.24, 0.84,  0.00),
    ("legs_anchor",    0.00, 0.52,  0.00),
    ("hands_anchor",   0.00, 0.52,  0.00),
    ("feet_anchor",    0.00, 0.10,  0.00),
]


# ─── ANIMATION CLIPS ──────────────────────────────────────────────────────────

def qx(deg):
    r = math.radians(deg) / 2.0
    return [round(math.sin(r), 6), 0.0, 0.0, round(math.cos(r), 6)]


def kf(bone_id, pairs):
    return {"boneId": bone_id,
            "keyframes": [{"keytime": round(t, 4), "rotation": qx(d)} for t, d in pairs]}


def make_clips(present):
    ULL,ULR = "upper_leg_l","upper_leg_r"
    LLL,LLR = "lower_leg_l","lower_leg_r"
    UAL,UAR = "upper_arm_l","upper_arm_r"
    LAL,LAR = "lower_arm_l","lower_arm_r"
    TRS,HEAD = "torso","head"

    def clip(cid, length, bones):
        used = [b for b in bones if b["boneId"] in present]
        return {"id": cid, "length": round(length, 4), "bones": used}

    return [
        # Idle: explicitly reset every animated bone so that switching from walk
        # never leaves legs or arms frozen mid-swing.
        clip("idle", 2.0, [
            kf(TRS,  [(0.0, 0  ),(1.0, +2  ),(2.0, 0  )]),
            kf(HEAD, [(0.0, 0  ),(1.0, -1  ),(2.0, 0  )]),
            kf(ULL,  [(0.0, 0  ),(2.0, 0   )]),
            kf(ULR,  [(0.0, 0  ),(2.0, 0   )]),
            kf(LLL,  [(0.0, 0  ),(2.0, 0   )]),
            kf(LLR,  [(0.0, 0  ),(2.0, 0   )]),
            kf(UAL,  [(0.0, -5 ),(2.0, -5  )]),
            kf(UAR,  [(0.0, -5 ),(2.0, -5  )]),
            kf(LAL,  [(0.0, 0  ),(2.0, 0   )]),
            kf(LAR,  [(0.0, 0  ),(2.0, 0   )]),
        ]),
        # Walk: 1.2 s per full cycle (= 2 OSRS game ticks).  Reduced swing
        # angles match OSRS's subtle, measured stride.
        clip("walk", 1.2, [
            kf(ULL, [(0.0,+22),(0.3,0),(0.6,-22),(0.9,0),(1.2,+22)]),
            kf(ULR, [(0.0,-22),(0.3,0),(0.6,+22),(0.9,0),(1.2,-22)]),
            kf(LLL, [(0.0, +8),(0.3,0),(0.6,  0),(0.9,0),(1.2, +8)]),
            kf(LLR, [(0.0,  0),(0.3,0),(0.6, +8),(0.9,0),(1.2,  0)]),
            kf(UAL, [(0.0,-14),(0.3,0),(0.6,+14),(0.9,0),(1.2,-14)]),
            kf(UAR, [(0.0,+14),(0.3,0),(0.6,-14),(0.9,0),(1.2,+14)]),
            kf(LAL, [(0.0,  0),(0.3,0),(0.6, +5),(0.9,0),(1.2,  0)]),
            kf(LAR, [(0.0, +5),(0.3,0),(0.6,  0),(0.9,0),(1.2, +5)]),
        ]),
        clip("pickup", 1.0, [
            kf(TRS, [(0.0,0),(0.35,+35),(0.7,0),(1.0,0)]),
            kf(UAL, [(0.0,0),(0.35,+60),(0.7,0),(1.0,0)]),
            kf(UAR, [(0.0,0),(0.35,+60),(0.7,0),(1.0,0)]),
            kf(LAL, [(0.0,0),(0.35,+30),(0.7,0),(1.0,0)]),
            kf(LAR, [(0.0,0),(0.35,+30),(0.7,0),(1.0,0)]),
        ]),
        clip("chop", 0.667, [
            kf(UAR, [(0.0,-70),(0.22,-70),(0.44,+30),(0.667,-70)]),
            kf(LAR, [(0.0,  0),(0.22,  0),(0.44,+45),(0.667,  0)]),
            kf(UAL, [(0.0,+15),(0.22,+15),(0.44,+15),(0.667,+15)]),
            kf(TRS, [(0.0,  0),(0.22,  0),(0.44,+10),(0.667,  0)]),
        ]),
        clip("mine", 0.667, [
            kf(UAR, [(0.0,-80),(0.22,-80),(0.44,+20),(0.667,-80)]),
            kf(UAL, [(0.0,-80),(0.22,-80),(0.44,+20),(0.667,-80)]),
            kf(LAR, [(0.0,+30),(0.22,+30),(0.44,+60),(0.667,+30)]),
            kf(LAL, [(0.0,+30),(0.22,+30),(0.44,+60),(0.667,+30)]),
            kf(TRS, [(0.0,  0),(0.22,  0),(0.44,+12),(0.667,  0)]),
        ]),
        clip("fish", 1.333, [
            kf(UAR, [(0.0,+25),(0.667,+30),(1.333,+25)]),
            kf(LAR, [(0.0,+40),(0.667,+50),(1.333,+40)]),
            kf(UAL, [(0.0,+10),(0.667,+12),(1.333,+10)]),
            kf(TRS, [(0.0,  0),(0.667, +3),(1.333,  0)]),
        ]),
        clip("sword", 0.6, [
            kf(UAR, [(0.0,+20),(0.15,-50),(0.3,+50),(0.5,+15),(0.6,+20)]),
            kf(LAR, [(0.0,+30),(0.15,+20),(0.3,+60),(0.5,+25),(0.6,+30)]),
            kf(UAL, [(0.0,+10),(0.15,+30),(0.3,-10),(0.5,+10),(0.6,+10)]),
            kf(TRS, [(0.0,  0),(0.15, +5),(0.3, -5),(0.5,  0),(0.6,  0)]),
        ]),
        clip("spear", 0.667, [
            kf(UAR, [(0.0,+30),(0.22,+30),(0.44,+15),(0.667,+30)]),
            kf(UAL, [(0.0,+30),(0.22,+30),(0.44,+15),(0.667,+30)]),
            kf(LAR, [(0.0,+10),(0.22,+10),(0.44,+10),(0.667,+10)]),
            kf(LAL, [(0.0,+10),(0.22,+10),(0.44,+10),(0.667,+10)]),
            kf(TRS, [(0.0,  0),(0.22,+10),(0.44, -5),(0.667,  0)]),
        ]),
    ]


# ─── MAIN ─────────────────────────────────────────────────────────────────────

def convert(glb_path, output_id="player_base"):
    print(f"\nConverting {glb_path} → {output_id}.g3dj")
    print("(no decimation — preserving all original geometry)\n")

    gltf, bin_data = parse_glb(glb_path)
    pos, tri = load_mesh(gltf, bin_data)

    print("  Normalising to 1.80m …")
    pos = normalise_to_180cm(pos)

    print("  Computing smooth normals …")
    normals = compute_normals(pos, tri)

    print("  Assigning cowboy colours …")
    colors = assign_colors(pos, normals)

    print("  Splitting into body parts …")
    parts = split_mesh(pos, tri, normals, colors)

    # Build G3DJ
    meshes, mats, children = [], [], []
    present = set(parts.keys())

    for node_id in BODY_PARTS_ORDER:
        if node_id not in parts:
            continue
        gm, gn, mat = make_g3dj_part(node_id, parts[node_id])
        meshes.append(gm)
        mats.append(mat)
        children.append(gn)

    for aid, ax, ay, az in ANCHORS:
        children.append({"id": aid, "translation": [ax, ay, az]})

    g3dj = {
        "version":    [0, 1],
        "id":         output_id,
        "meshes":     meshes,
        "materials":  mats,
        "nodes":      [{"id": "root", "translation": [0.0, 0.0, 0.0], "children": children}],
        "animations": make_clips(present),
    }

    os.makedirs(OUT_DIR, exist_ok=True)
    os.makedirs(RESOURCE_DIR, exist_ok=True)
    out_art = os.path.join(OUT_DIR, f"{output_id}.g3dj")
    out_res = os.path.join(RESOURCE_DIR, f"{output_id}.g3dj")

    print(f"\n  Writing {output_id}.g3dj …")
    with open(out_art, "w") as f:
        json.dump(g3dj, f, separators=(",", ":"))
    shutil.copy2(out_art, out_res)

    total_v = sum(len(d["pos"]) for d in parts.values())
    total_t = sum(len(d["tri"]) for d in parts.values())
    size_mb = os.path.getsize(out_art) / 1024 / 1024
    print(f"\n  Done.")
    print(f"  Parts:       {len(parts)} body nodes + 9 anchors")
    print(f"  Vertices:    {total_v:,} (includes seam duplication)")
    print(f"  Triangles:   {total_t:,}")
    print(f"  File size:   {size_mb:.1f} MB")
    print(f"  Clips:       {[a['id'] for a in g3dj['animations']]}")
    print(f"  → {out_res}")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(0)
    glb_path  = sys.argv[1]
    output_id = sys.argv[2] if len(sys.argv) > 2 else "player_base"
    if not os.path.exists(glb_path):
        print(f"ERROR: {glb_path} not found"); sys.exit(1)
    convert(glb_path, output_id)
