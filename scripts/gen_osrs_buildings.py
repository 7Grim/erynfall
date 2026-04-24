#!/usr/bin/env python3
"""
gen_osrs_buildings.py — Generate OSRS-style solid building shell models via Blender.

Usage:
    blender --background --python scripts/gen_osrs_buildings.py

Overwrites:
    art/models/building_shell_small_base.glb
    art/models/building_shell_small_roof.glb
    art/models/building_shell_service_base.glb
    art/models/building_shell_service_roof.glb
    art/models/building_shell_coastal_base.glb
    art/models/building_shell_coastal_roof.glb

Coordinate convention:
    Blender Z = up (height).  Exported with GLTF +Y-up (default).
    In GLTF: Y = height (was Blender Z), X = Blender X, Z = -Blender Y.

OSRS building proportions (per analysis of existing GLB mesh data):
    Canonical player height = 1.80 world units (WorldScale.PLAYER_HEIGHT in WorldScale.java).
    This script was originally calibrated assuming player height = 2.0 units — an 11% error.
    Walls are therefore ~11% taller relative to the player than the comments below imply.
    The proportions are visually acceptable; no rebuild is required. See docs/SCALE_SPEC.md.

    At placement scale 2.5 (scene.yaml), buildings are 2.5× model size.
    Target wall height = 1.5 model units → 3.75 units at scale 2.5 (~2.08× player at 1.80 WU).
    Building footprint: small ≈ 4.2 × 3.8 model units → ~10.5 × 9.5 tiles at scale 2.5.

Why previous models were broken:
    base GLB: hollow thin-wall frame → corners gap at scale 2.5.
    roof GLB: x only -0.64→+1.38 (asymmetric, half the base width) → visible roof hole.

This script produces solid-wall bases and symmetric hip roofs that always align.
"""

import bpy
import bmesh
import pathlib
import sys

REPO_ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT_DIR   = REPO_ROOT / "art" / "models"


# ── Colour helpers ────────────────────────────────────────────────────────────

def srgb(r, g, b, a=1.0):
    """Convert 0-255 sRGB to Blender linear-light RGBA."""
    def c(v): return (v / 255.0) ** 2.2
    return (c(r), c(g), c(b), a)


# OSRS palette: warm sandy stone walls + dark thatched roofs
COL_STONE        = srgb(196, 173, 138)   # main wall face — warm tan
COL_STONE_DARK   = srgb(148, 128, 100)   # shadow/recessed stone
COL_TIMBER       = srgb(90,  58,  28)    # timber frame trim
COL_ROOF_THATCH  = srgb(122, 88,  50)    # thatched roof surface
COL_ROOF_DARK    = srgb(88,  62,  35)    # underside/shadow roof
COL_ROOF_TIMBER  = srgb(60,  38,  14)    # ridge beam timber
COL_STONE_SVC    = srgb(160, 148, 126)   # service building (slightly darker)
COL_ROOF_SVC     = srgb(100, 72,  42)    # service roof


# ── Material factory ──────────────────────────────────────────────────────────

def make_mat(name, color, roughness=0.95):
    """Create or replace a Principled BSDF material."""
    if name in bpy.data.materials:
        bpy.data.materials.remove(bpy.data.materials[name])
    mat = bpy.data.materials.new(name)
    mat.use_nodes = True
    nodes = mat.node_tree.nodes
    bsdf = nodes.get("Principled BSDF")
    if bsdf:
        bsdf.inputs["Base Color"].default_value = color
        bsdf.inputs["Roughness"].default_value  = roughness
        bsdf.inputs["Metallic"].default_value   = 0.0
    return mat


# ── Scene utilities ───────────────────────────────────────────────────────────

def clear_scene():
    bpy.ops.object.select_all(action='SELECT')
    bpy.ops.object.delete(use_global=True)
    for mesh in list(bpy.data.meshes):
        bpy.data.meshes.remove(mesh)


def add_mesh_object(name, mesh):
    obj = bpy.data.objects.new(name, mesh)
    bpy.context.collection.objects.link(obj)
    bpy.context.view_layer.objects.active = obj
    obj.select_set(True)
    return obj


def flat_shade(obj):
    for poly in obj.data.polygons:
        poly.use_smooth = False


# ── Building geometry ─────────────────────────────────────────────────────────

def make_base(obj_name, hw, hd, h, mat_stone, mat_timber):
    """
    Solid-wall building base.

    hw        half-width  (Blender X)
    hd        half-depth  (Blender Y → GLTF -Z)
    h         wall height (Blender Z → GLTF Y)
    mat_stone main face material (slot 0)
    mat_timber top-trim material  (slot 1)

    Geometry: closed box (4 walls + floor + top cap).
    Top-cap strip uses mat_timber for a timber-frame crown finish.
    """
    bm = bmesh.new()

    # 8 corner verts — outer box
    v = [
        bm.verts.new((-hw, -hd, 0.0)),   # 0 SW-bottom
        bm.verts.new(( hw, -hd, 0.0)),   # 1 SE-bottom
        bm.verts.new(( hw,  hd, 0.0)),   # 2 NE-bottom
        bm.verts.new((-hw,  hd, 0.0)),   # 3 NW-bottom
        bm.verts.new((-hw, -hd, h   )),   # 4 SW-top
        bm.verts.new(( hw, -hd, h   )),   # 5 SE-top
        bm.verts.new(( hw,  hd, h   )),   # 6 NE-top
        bm.verts.new((-hw,  hd, h   )),   # 7 NW-top
    ]
    bm.verts.ensure_lookup_table()

    # 4 walls + floor
    wall_s = bm.faces.new([v[0], v[1], v[5], v[4]])   # south
    wall_e = bm.faces.new([v[1], v[2], v[6], v[5]])   # east
    wall_n = bm.faces.new([v[3], v[2], v[6], v[7]])   # north (flipped winding)
    wall_w = bm.faces.new([v[0], v[3], v[7], v[4]])   # west (flipped winding)
    floor  = bm.faces.new([v[0], v[3], v[2], v[1]])   # floor

    # Top cap (timber trim)
    top = bm.faces.new([v[4], v[5], v[6], v[7]])

    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)

    mesh = bpy.data.meshes.new(obj_name)
    bm.to_mesh(mesh)
    bm.free()

    mesh.materials.append(mat_stone)    # slot 0
    mesh.materials.append(mat_timber)   # slot 1

    # Assign: walls + floor → stone (0), top cap → timber (1)
    for i, poly in enumerate(mesh.polygons):
        poly.material_index = 0
    mesh.polygons[5].material_index = 1  # top cap is face index 5

    obj = add_mesh_object(obj_name, mesh)
    flat_shade(obj)
    return obj


def make_roof(obj_name, hw, hd, eave_z, peak_z, ridge_hw, mat_tile, mat_beam):
    """
    Hip roof: 4 sloped faces meeting at a central ridge running along Blender X.

    hw        half-width (Blender X) of eave — should be base hw + 0.2 overhang
    hd        half-depth (Blender Y) of eave — should be base hd + 0.2 overhang
    eave_z    Z level of eave (bottom edge of roof) — slightly below wall top
    peak_z    Z level of ridge
    ridge_hw  half-length of ridge (Blender X) — determines hip steepness
    mat_tile  roof surface material
    mat_beam  ridge-beam material
    """
    bm = bmesh.new()

    # 6 main verts: 4 eave corners + 2 ridge endpoints
    e = [
        bm.verts.new((-hw,  -hd, eave_z)),   # 0 SW eave
        bm.verts.new(( hw,  -hd, eave_z)),   # 1 SE eave
        bm.verts.new(( hw,   hd, eave_z)),   # 2 NE eave
        bm.verts.new((-hw,   hd, eave_z)),   # 3 NW eave
        bm.verts.new((-ridge_hw, 0.0, peak_z)),  # 4 west ridge
        bm.verts.new(( ridge_hw, 0.0, peak_z)),  # 5 east ridge
    ]
    bm.verts.ensure_lookup_table()

    # 4 sloped faces (2 quads, 2 triangles — hip geometry)
    f_s = bm.faces.new([e[0], e[1], e[5], e[4]])   # south slope (quad)
    f_n = bm.faces.new([e[2], e[3], e[4], e[5]])   # north slope (quad)
    f_e = bm.faces.new([e[1], e[2], e[5]])          # east hip  (triangle)
    f_w = bm.faces.new([e[3], e[0], e[4]])          # west hip  (triangle)

    # Ridge beam: thin box along ridge line
    beam_r = 0.07
    beam_hw = ridge_hw + 0.12
    beam_verts = [
        bm.verts.new((-beam_hw, -beam_r, peak_z - beam_r)),
        bm.verts.new(( beam_hw, -beam_r, peak_z - beam_r)),
        bm.verts.new(( beam_hw,  beam_r, peak_z - beam_r)),
        bm.verts.new((-beam_hw,  beam_r, peak_z - beam_r)),
        bm.verts.new((-beam_hw, -beam_r, peak_z + beam_r)),
        bm.verts.new(( beam_hw, -beam_r, peak_z + beam_r)),
        bm.verts.new(( beam_hw,  beam_r, peak_z + beam_r)),
        bm.verts.new((-beam_hw,  beam_r, peak_z + beam_r)),
    ]
    bm.verts.ensure_lookup_table()
    bv = beam_verts
    beam_faces = [
        bm.faces.new([bv[0], bv[1], bv[5], bv[4]]),
        bm.faces.new([bv[1], bv[2], bv[6], bv[5]]),
        bm.faces.new([bv[3], bv[2], bv[6], bv[7]]),
        bm.faces.new([bv[0], bv[3], bv[7], bv[4]]),
        bm.faces.new([bv[4], bv[5], bv[6], bv[7]]),
        bm.faces.new([bv[0], bv[3], bv[2], bv[1]]),
    ]

    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)

    mesh = bpy.data.meshes.new(obj_name)
    bm.to_mesh(mesh)
    bm.free()

    mesh.materials.append(mat_tile)   # slot 0
    mesh.materials.append(mat_beam)   # slot 1

    # Roof slope faces → tile (0), ridge beam faces → beam (1)
    n_slope = 4
    for i, poly in enumerate(mesh.polygons):
        poly.material_index = 0 if i < n_slope else 1

    obj = add_mesh_object(obj_name, mesh)
    flat_shade(obj)
    return obj


# ── GLB export ────────────────────────────────────────────────────────────────

def export_glb(obj, path):
    """Export a single selected object to GLB with GLTF +Y-up convention."""
    bpy.ops.object.select_all(action='DESELECT')
    obj.select_set(True)
    bpy.context.view_layer.objects.active = obj
    bpy.ops.export_scene.gltf(
        filepath     = str(path),
        use_selection = True,
        export_format = 'GLB',
        export_apply  = True,     # apply modifiers
        export_yup    = True,     # Blender Z → GLTF Y (up in game)
        export_materials = 'EXPORT',
        export_normals = True,
    )
    size = path.stat().st_size
    print(f"  wrote {path.name}  ({size:,} bytes)")


# ── Variant definitions ───────────────────────────────────────────────────────
#
# All dimensions in Blender-space (Z = up).
# GLTF Y  = Blender Z  (height, 0 = ground)
# GLTF X  = Blender X  (east-west, ± = half-width)
# GLTF Z  = -Blender Y (north-south, ± = half-depth)
#
# Existing small_base GLB had:  GLTF X ±2.09, Y 0→1.46, Z ±1.91
# New targets match that footprint, fix roof symmetry & wall solidity.
#
VARIANTS = {
    # name:  (base_hw, base_hd, wall_h, roof_overhang, peak_z, ridge_hw)
    #         hw/hd = half extents; overhang added to hw+hd for roof eave
    'small':   (2.10, 1.90, 1.50, 0.22, 2.60, 1.10),
    'service': (2.50, 2.10, 1.55, 0.22, 2.70, 1.50),
    'coastal': (2.10, 1.90, 1.25, 0.20, 2.25, 1.10),
}


def main():
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    print("Building grid…")

    for vname, (hw, hd, wh, overhang, peak_z, ridge_hw) in VARIANTS.items():
        print(f"\n── {vname} ──────────────────────────────────")

        # Material names must match what model library / scene.yaml expect
        if vname == 'small':
            mat_wall  = make_mat(f'shell_small_stone',        COL_STONE,       0.95)
            mat_trim  = make_mat(f'shell_small_timber',       COL_TIMBER,      0.90)
            mat_tile  = make_mat(f'shell_small_roof_tile',    COL_ROOF_THATCH, 0.95)
            mat_beam  = make_mat(f'shell_small_roof_timber',  COL_ROOF_TIMBER, 0.88)
        elif vname == 'service':
            mat_wall  = make_mat(f'shell_service_stone',        COL_STONE_SVC,  0.95)
            mat_trim  = make_mat(f'shell_service_timber',       COL_TIMBER,     0.90)
            mat_tile  = make_mat(f'shell_service_roof_tile',    COL_ROOF_SVC,   0.95)
            mat_beam  = make_mat(f'shell_service_roof_timber',  COL_ROOF_TIMBER, 0.88)
        else:  # coastal
            mat_wall  = make_mat(f'shell_coastal_stone',        COL_STONE,       0.95)
            mat_trim  = make_mat(f'shell_coastal_timber',       COL_TIMBER,      0.90)
            mat_tile  = make_mat(f'shell_coastal_roof_tile',    COL_ROOF_THATCH, 0.95)
            mat_beam  = make_mat(f'shell_coastal_roof_timber',  COL_ROOF_TIMBER, 0.88)

        # BASE
        clear_scene()
        base_name = f'building_shell_{vname}_base'
        obj_base = make_base(base_name, hw, hd, wh, mat_wall, mat_trim)
        export_glb(obj_base, OUT_DIR / f"{base_name}.glb")

        # ROOF
        clear_scene()
        roof_name = f'building_shell_{vname}_roof'
        obj_roof = make_roof(
            roof_name,
            hw     + overhang,
            hd     + overhang,
            eave_z = wh - 0.05,   # eave slightly below wall top for overlap
            peak_z = peak_z,
            ridge_hw = ridge_hw,
            mat_tile = mat_tile,
            mat_beam = mat_beam,
        )
        export_glb(obj_roof, OUT_DIR / f"{roof_name}.glb")

    print("\nAll buildings done.")


if __name__ == "__main__":
    main()
