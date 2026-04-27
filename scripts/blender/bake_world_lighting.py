"""Cycles bake of AO + directional sun shadow onto the terrain mesh's vertex colors.

Input:  `art/worlds/<world>/world.blend`
Output: same file, with the `terrain_mesh` object's `Col` vertex-color layer
        overwritten with baked `Combined` (direct + indirect + AO + shadow) light.

Why bake to vertex colors: the runtime's stock libGDX `DefaultShader` already
samples vertex colors — no shader changes required. Baking to a lightmap texture
is strictly better but requires a shader modification; use that in a future phase.

Run headless:

    blender --background --python scripts/blender/bake_world_lighting.py -- --world=main_world

Tunables (after --):
    --sun-elevation=<deg>   Angle above horizon. Default 50.
    --sun-azimuth=<deg>     Compass direction (0=+Y north, 90=+X east). Default 135.
    --sun-strength=<float>  Energy in watts/m^2. Default 2.0.
    --samples=<int>         Cycles bake samples. Default 64 (fast). 256 for final.
"""

from __future__ import annotations

import argparse
import math
import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
if str(_HERE) not in sys.path:
    sys.path.insert(0, str(_HERE))

import _pipeline_common as pc  # noqa: E402


def _extra_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--world", default="main_world")
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--sun-elevation", type=float, default=50.0)
    parser.add_argument("--sun-azimuth", type=float, default=135.0)
    parser.add_argument("--sun-strength", type=float, default=2.0)
    parser.add_argument("--samples", type=int, default=64)
    return parser.parse_args(pc.blender_argv_after_double_dash())


def _setup_sun(elev_deg: float, azim_deg: float, strength: float) -> None:
    import bpy  # type: ignore
    from mathutils import Euler  # type: ignore

    # Remove any existing Sun lamps so bakes are deterministic.
    for obj in list(bpy.data.objects):
        if obj.type == "LIGHT" and obj.data and obj.data.type == "SUN":
            bpy.data.objects.remove(obj, do_unlink=True)

    light_data = bpy.data.lights.new(name="world_sun", type="SUN")
    light_data.energy = strength
    obj = bpy.data.objects.new(name="world_sun", object_data=light_data)
    bpy.context.collection.objects.link(obj)

    # Convert elevation/azimuth to Euler. Blender default sun points down (-Z).
    elev = math.radians(elev_deg)
    azim = math.radians(azim_deg)
    # Rotate sun's -Z axis to match (elev, azim). We rotate around X (elevation)
    # then around Z (azimuth).
    obj.rotation_mode = "XYZ"
    obj.rotation_euler = Euler((math.radians(90.0) - elev, 0.0, azim), "XYZ")


def _bake_terrain(samples: int) -> None:
    import bpy  # type: ignore

    terrain = bpy.data.objects.get("terrain_mesh")
    if terrain is None:
        raise RuntimeError("terrain_mesh object not found — run generate_world_blend.py first")

    # Ensure Col vertex-color layer exists.
    if "Col" not in terrain.data.color_attributes:
        terrain.data.color_attributes.new(name="Col", type="BYTE_COLOR", domain="CORNER")
    terrain.data.color_attributes.active_color_index = list(terrain.data.color_attributes).index(
        terrain.data.color_attributes["Col"]
    )

    # Terrain needs a material whose shader reads vertex colors so we can bake
    # 'Combined' into them. A simple Diffuse BSDF fed by an Attribute(Col) node works.
    mat = bpy.data.materials.get("terrain_bake_mat")
    if mat is None:
        mat = bpy.data.materials.new("terrain_bake_mat")
    mat.use_nodes = True
    nt = mat.node_tree
    for n in list(nt.nodes):
        nt.nodes.remove(n)
    attr = nt.nodes.new("ShaderNodeAttribute")
    attr.attribute_name = "Col"
    bsdf = nt.nodes.new("ShaderNodeBsdfDiffuse")
    out = nt.nodes.new("ShaderNodeOutputMaterial")
    nt.links.new(attr.outputs["Color"], bsdf.inputs["Color"])
    nt.links.new(bsdf.outputs["BSDF"], out.inputs["Surface"])
    if not terrain.data.materials:
        terrain.data.materials.append(mat)
    else:
        terrain.data.materials[0] = mat

    # Configure Cycles for vertex-color bake.
    scene = bpy.context.scene
    scene.render.engine = "CYCLES"
    scene.cycles.samples = samples
    scene.cycles.use_denoising = True
    scene.cycles.bake_type = "COMBINED"
    scene.render.bake.target = "VERTEX_COLORS"
    scene.render.bake.use_pass_direct = True
    scene.render.bake.use_pass_indirect = True
    scene.render.bake.use_pass_diffuse = True
    scene.render.bake.use_pass_glossy = False
    scene.render.bake.use_pass_transmission = False
    scene.render.bake.use_pass_emit = False
    scene.render.bake.margin = 0

    # Select only the terrain as the bake target.
    bpy.ops.object.select_all(action="DESELECT")
    terrain.select_set(True)
    bpy.context.view_layer.objects.active = terrain
    bpy.ops.object.bake(type="COMBINED")


def main() -> int:
    args = _extra_args()
    repo = pc.repo_root_from_script(__file__)
    blend_path = pc.world_blend_path(repo, args.world)

    if not blend_path.exists():
        pc.log("bake_world_lighting", f"ERROR: {blend_path} does not exist. Run generate_world_blend.py first.")
        return 1

    pc.log("bake_world_lighting",
           f"world={args.world} elev={args.sun_elevation} azim={args.sun_azimuth} "
           f"strength={args.sun_strength} samples={args.samples}")

    if args.dry_run:
        pc.log("bake_world_lighting", "dry-run requested; not invoking Blender")
        return 0

    try:
        import bpy  # type: ignore
    except ImportError:
        pc.log("bake_world_lighting",
               "ERROR: bpy not available. Run this via Blender: "
               "`blender --background --python scripts/blender/bake_world_lighting.py -- --world=<id>`")
        return 2

    bpy.ops.wm.open_mainfile(filepath=str(blend_path))
    _setup_sun(args.sun_elevation, args.sun_azimuth, args.sun_strength)
    _bake_terrain(args.samples)
    bpy.ops.wm.save_mainfile()
    pc.log("bake_world_lighting", f"updated vertex colors in {blend_path}")
    pc.log("bake_world_lighting", "remember to re-run export_world_glb.py so world.glb picks this up")
    return 0


if __name__ == "__main__":
    sys.exit(main())
