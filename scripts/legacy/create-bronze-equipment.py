"""
Blender headless script — generates bronze full helm and platebody GLB files.

Run from repo root:
  /Applications/Blender.app/Contents/MacOS/blender --background --python scripts/create-bronze-equipment.py

Coordinate mapping (LibGDX → Blender):
  LibGDX X (right)  → Blender X
  LibGDX Y (up)     → Blender Z
  LibGDX Z (depth)  → Blender -Y
  (Blender's default GLB exporter converts Z-up → Y-up automatically)

Output:
  art/blender/equipment/equip_head_bronze_full_helm.blend
  art/blender/equipment/equip_body_bronze_platebody.blend
  art/models/equip_head_bronze_full_helm.glb
  art/models/equip_body_bronze_platebody.glb
"""

import bpy
import os
from pathlib import Path

REPO_ROOT    = Path(__file__).resolve().parent.parent
BLEND_DIR    = REPO_ROOT / "art" / "blender" / "equipment"
GLB_DIR      = REPO_ROOT / "art" / "models"

# Bronze colour palette — linear RGB (matches existing G3DJ + LibGDX diffuse values)
BRONZE_MAIN  = (0.62, 0.46, 0.30, 1.0)
BRONZE_DARK  = (0.38, 0.26, 0.14, 1.0)


# ---------------------------------------------------------------------------
# Scene / mesh helpers
# ---------------------------------------------------------------------------

def clear_scene():
    bpy.ops.wm.read_factory_settings(use_empty=True)


def make_mat(name: str, color: tuple, metallic: float = 0.78, roughness: float = 0.42) -> bpy.types.Material:
    mat = bpy.data.materials.new(name=name)
    mat.use_nodes = True
    nodes = mat.node_tree.nodes
    links = mat.node_tree.links
    nodes.clear()
    bsdf   = nodes.new("ShaderNodeBsdfPrincipled")
    output = nodes.new("ShaderNodeOutputMaterial")
    links.new(bsdf.outputs["BSDF"], output.inputs["Surface"])
    bsdf.inputs["Base Color"].default_value = color
    bsdf.inputs["Metallic"].default_value   = metallic
    bsdf.inputs["Roughness"].default_value  = roughness
    return mat


def add_box(name: str, lx0, ly0, lz0, lx1, ly1, lz1, mat):
    """
    Create a box object in LibGDX coordinates and assign mat.
    lx/ly/lz are LibGDX local-space bounds (Y=up, Z=depth).
    """
    # Centre in Blender space
    bx  = (lx0 + lx1) * 0.5
    by  = -(lz0 + lz1) * 0.5     # Blender Y = -LibGDX Z
    bz  = (ly0 + ly1) * 0.5      # Blender Z =  LibGDX Y
    # Half-extents
    hx = (lx1 - lx0) * 0.5
    hy = abs(lz1 - lz0) * 0.5
    hz = (ly1 - ly0) * 0.5

    bpy.ops.mesh.primitive_cube_add(size=2.0, location=(bx, by, bz))
    obj = bpy.context.active_object
    obj.name = name
    obj.scale = (hx, hy, hz)
    bpy.ops.object.transform_apply(scale=True)
    obj.data.materials.clear()
    obj.data.materials.append(mat)
    return obj


def join_all(final_name: str) -> bpy.types.Object:
    bpy.ops.object.select_all(action='SELECT')
    bpy.context.view_layer.objects.active = next(
        o for o in bpy.context.scene.objects if o.type == 'MESH')
    bpy.ops.object.join()
    obj = bpy.context.active_object
    obj.name = final_name
    return obj


def save_blend(path: Path):
    path.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=str(path))
    print(f"  saved  {path.relative_to(REPO_ROOT)}")


def export_glb(path: Path):
    path.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.export_scene.gltf(
        filepath=str(path),
        export_format="GLB",
        export_apply=True,
    )
    print(f"  export {path.relative_to(REPO_ROOT)}")


# ---------------------------------------------------------------------------
# Bronze full helm
# ---------------------------------------------------------------------------
# Anchor: head_anchor world Y=1.28; offset_y=1.18 (=default) → model origin at Y=1.28.
# Head occupies world Y 1.06–1.28  →  local Y −0.22 .. 0.00 at anchor.
# Helm should enclose head with small clearance and add visor/nape.
#
# Key geometry targets (local LibGDX coords, origin=head_anchor):
#   dome       : ±0.106 wide, Y −0.20 .. +0.04, ±0.106 deep   (main skull cap)
#   face plate : ±0.055 wide, Y −0.26 .. −0.04, Z +0.088..+0.110  (visor plate)
#   cheek L    : X −0.110..−0.094, Y −0.26..−0.06, ±0.090 deep
#   cheek R    : X +0.094..+0.110, Y −0.26..−0.06, ±0.090 deep
#   nape       : ±0.090 wide, Y −0.22..−0.04, Z −0.112..−0.090  (back neck guard)
#   brow ridge : ±0.100 wide, Y −0.04..+0.00, Z +0.094..+0.112  (dark trim)
#   visor slit : ±0.052 wide, Y −0.09..−0.04, Z +0.098..+0.114  (dark detail)
# ---------------------------------------------------------------------------

def create_helm():
    print("Building equip_head_bronze_full_helm …")
    clear_scene()
    m  = make_mat("bronze_main", BRONZE_MAIN)
    md = make_mat("bronze_dark", BRONZE_DARK)

    add_box("dome",       -0.106, -0.20, -0.106,  0.106,  0.04,  0.106, m)
    add_box("face_plate", -0.055, -0.26,  0.088,  0.055, -0.04,  0.110, m)
    add_box("cheek_l",   -0.110, -0.26, -0.090, -0.094, -0.06,  0.090, m)
    add_box("cheek_r",    0.094, -0.26, -0.090,  0.110, -0.06,  0.090, m)
    add_box("nape",      -0.090, -0.22, -0.112,  0.090, -0.04, -0.090, m)
    add_box("brow",      -0.100, -0.04,  0.094,  0.100,  0.00,  0.112, md)
    add_box("visor_slit",-0.052, -0.09,  0.098,  0.052, -0.04,  0.114, md)

    join_all("equip_head_bronze_full_helm")
    save_blend(BLEND_DIR / "equip_head_bronze_full_helm.blend")
    export_glb(GLB_DIR   / "equip_head_bronze_full_helm.glb")


# ---------------------------------------------------------------------------
# Bronze platebody
# ---------------------------------------------------------------------------
# Anchor: body_anchor world Y=0.88; offset_y=0.82, default=0.84 → delta=−0.02
#   model origin at world Y = 0.88 + (0.82 − 0.84) = 0.86
# Torso occupies world Y 0.54–1.00 → local Y −0.32 .. +0.14
#
# Key geometry targets (local LibGDX):
#   main torso  : ±0.150 wide, Y −0.32..+0.14, ±0.090 deep
#   pauldron L  : X −0.215..−0.150, Y −0.02..+0.14, ±0.060 deep
#   pauldron R  : X +0.150..+0.215, Y −0.02..+0.14, ±0.060 deep
#   fauld       : ±0.152 wide, Y −0.34..−0.30, ±0.092 deep   (dark belt trim)
#   collar      : ±0.082 wide, Y +0.10..+0.16, ±0.077 deep   (dark gorget)
#   gusset L    : X −0.182..−0.150, Y −0.18..−0.02, ±0.046 deep
#   gusset R    : X +0.150..+0.182, Y −0.18..−0.02, ±0.046 deep
# ---------------------------------------------------------------------------

def create_platebody():
    print("Building equip_body_bronze_platebody …")
    clear_scene()
    m  = make_mat("bronze_main", BRONZE_MAIN)
    md = make_mat("bronze_dark", BRONZE_DARK)

    add_box("torso",     -0.150, -0.32, -0.090,  0.150,  0.14,  0.090, m)
    add_box("pauldron_l",-0.215, -0.02, -0.060, -0.150,  0.14,  0.060, m)
    add_box("pauldron_r", 0.150, -0.02, -0.060,  0.215,  0.14,  0.060, m)
    add_box("fauld",     -0.152, -0.34, -0.092,  0.152, -0.30,  0.092, md)
    add_box("collar",    -0.082,  0.10, -0.077,  0.082,  0.16,  0.077, md)
    add_box("gusset_l",  -0.182, -0.18, -0.046, -0.150, -0.02,  0.046, m)
    add_box("gusset_r",   0.150, -0.18, -0.046,  0.182, -0.02,  0.046, m)

    join_all("equip_body_bronze_platebody")
    save_blend(BLEND_DIR / "equip_body_bronze_platebody.blend")
    export_glb(GLB_DIR   / "equip_body_bronze_platebody.glb")


# ---------------------------------------------------------------------------

create_helm()
create_platebody()
print("Done.")
