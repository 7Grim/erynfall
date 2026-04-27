"""Bootstrap `world.blend` from existing scene.yaml + manifest.yaml.

Produces `art/worlds/<world>/world.blend` containing:

- A `terrain_mesh` Object: subdivided plane covering world bounds, displaced in Z
  per terrain_height regions, vertex-colored per terrain_visual types.
- A `props` Collection: one Object per static_props entry, named
  `<manifest_key>.<instance_id>` and placed at the tile-space position.
- For manifest entries with `source_blend` that exist on disk, the Object is a
  linked collection instance of the source blend's top-level collection (WYSIWYG).
- For entries without `source_blend` (current 159-of-175 case), the Object is a
  category-colored proxy cube sized to a reasonable default, so it is visible in
  the Blender viewport.

Run headless:

    blender --background --python scripts/blender/generate_world_blend.py -- --world=main_world

Or, dry-run the plan without requiring Blender at all:

    python3 scripts/blender/generate_world_blend.py --world=main_world --dry-run

The dry-run path is useful for CI and for reviewing what the script *would* do
without a Blender install.
"""

from __future__ import annotations

import os
import sys
from pathlib import Path

# Allow `from _pipeline_common import ...` when run either from repo root or directly.
_HERE = Path(__file__).resolve().parent
if str(_HERE) not in sys.path:
    sys.path.insert(0, str(_HERE))

import _pipeline_common as pc  # noqa: E402


# Distinct colors per category so proxy cubes are visually distinguishable.
CATEGORY_COLORS = {
    "prop": (0.82, 0.65, 0.45, 1.0),       # tan
    "resource": (0.35, 0.60, 0.35, 1.0),   # green
    "shell": (0.55, 0.45, 0.35, 1.0),      # brown
    "actor": (0.85, 0.45, 0.35, 1.0),      # terracotta
    "equipment": (0.65, 0.65, 0.65, 1.0),  # grey
    "": (0.5, 0.5, 0.5, 1.0),
}

# Category default footprint (x, y, z) in world units, used for proxy cubes.
CATEGORY_FOOTPRINT = {
    "prop": (1.0, 1.0, 1.0),
    "resource": (1.0, 1.0, 2.5),
    "shell": (4.0, 4.0, 3.0),
    "actor": (0.8, 0.8, 1.8),
    "equipment": (0.6, 0.6, 0.6),
    "": (1.0, 1.0, 1.0),
}

# Terrain visual type → vertex color tint. Values chosen to match the runtime
# sprite palette approximately; will be overwritten by Phase 2 bake.
VISUAL_TYPE_COLORS = {
    "grass": (0.35, 0.55, 0.28, 1.0),
    "sand":  (0.85, 0.75, 0.55, 1.0),
    "path":  (0.65, 0.55, 0.40, 1.0),
    "water": (0.20, 0.35, 0.55, 1.0),
}


# --------------------------------------------------------------------------
# Pure planning (no Blender imports)
# --------------------------------------------------------------------------

def build_plan(repo_root: Path, world_id: str) -> dict:
    scene_path = pc.scene_yaml_path(repo_root, world_id)
    manifest = pc.load_manifest(pc.manifest_path(repo_root))
    scene = pc.load_scene(scene_path)

    # World bounds from terrain_height / terrain_visual regions.
    min_x = min_y = 10_000
    max_x = max_y = -10_000
    for r in scene.height_regions + [vr for vr in scene.visual_regions]:
        min_x = min(min_x, r.min_x)
        min_y = min(min_y, r.min_y)
        max_x = max(max_x, r.max_x)
        max_y = max(max_y, r.max_y)
    if min_x > max_x or min_y > max_y:
        # No regions — fall back to a small pad so the script still produces a file.
        min_x, min_y, max_x, max_y = 0, 0, 64, 64

    # Plan prop placements: group by manifest key, assign instance ids.
    placements = []
    per_key_count: dict[str, int] = {}
    missing_keys: set[str] = set()
    for p in scene.static_props:
        if p.key not in manifest:
            missing_keys.add(p.key)
            continue
        per_key_count[p.key] = per_key_count.get(p.key, 0) + 1
        instance_id = f"{per_key_count[p.key]:03d}"
        entry = manifest[p.key]
        source_blend_path = None
        if entry.source_blend:
            candidate = repo_root / "art" / "blender" / entry.source_blend
            if candidate.exists():
                source_blend_path = candidate
        placements.append({
            "name": f"{p.key}.{instance_id}",
            "key": p.key,
            "category": entry.category,
            "x": p.x,
            "y": p.y,
            "rotation_y_degrees": p.rotation_y_degrees,
            "scale": p.scale * entry.scale,
            "visibility_group": p.visibility_group,
            "source_blend": str(source_blend_path) if source_blend_path else None,
        })

    return {
        "world_id": world_id,
        "world_blend_path": str(pc.world_blend_path(repo_root, world_id)),
        "world_bounds": {"min_x": min_x, "min_y": min_y, "max_x": max_x, "max_y": max_y},
        "height_step": scene.height_step,
        "height_regions": [r.__dict__ for r in scene.height_regions],
        "visual_default": scene.visual_default,
        "visual_regions": [r.__dict__ for r in scene.visual_regions],
        "tile_overrides": [t.__dict__ for t in scene.tile_overrides],
        "placements": placements,
        "missing_keys": sorted(missing_keys),
    }


def print_plan_summary(plan: dict) -> None:
    b = plan["world_bounds"]
    pc.log("generate_world_blend",
           f"world={plan['world_id']} bounds=({b['min_x']},{b['min_y']})..({b['max_x']},{b['max_y']})")
    pc.log("generate_world_blend",
           f"terrain: {len(plan['height_regions'])} height regions, "
           f"{len(plan['visual_regions'])} visual regions, "
           f"{len(plan['tile_overrides'])} per-tile overrides")
    pc.log("generate_world_blend",
           f"props: {len(plan['placements'])} placements planned")
    if plan["missing_keys"]:
        pc.log("generate_world_blend",
               f"WARN: {len(plan['missing_keys'])} scene.yaml keys not found in manifest — skipped: "
               + ", ".join(plan["missing_keys"][:10])
               + (" ..." if len(plan["missing_keys"]) > 10 else ""))
    linked = sum(1 for p in plan["placements"] if p["source_blend"])
    proxy = len(plan["placements"]) - linked
    pc.log("generate_world_blend",
           f"props with linked source .blend (WYSIWYG): {linked}, proxy-cube placements: {proxy}")


# --------------------------------------------------------------------------
# Blender-side execution
# --------------------------------------------------------------------------

def run_in_blender(plan: dict) -> None:
    """Build world.blend from the plan. Only imports bpy here."""
    import bpy  # type: ignore

    # Fresh file
    bpy.ops.wm.read_homefile(use_empty=True)

    scene = bpy.context.scene
    scene.name = f"world_{plan['world_id']}"

    # Top-level collections
    world_col = bpy.data.collections.new("world")
    scene.collection.children.link(world_col)
    terrain_col = bpy.data.collections.new("terrain")
    world_col.children.link(terrain_col)
    props_col = bpy.data.collections.new("props")
    world_col.children.link(props_col)

    # --- Terrain mesh ---
    b = plan["world_bounds"]
    width = b["max_x"] - b["min_x"]
    depth = b["max_y"] - b["min_y"]
    origin_x = (b["min_x"] + b["max_x"]) / 2.0
    origin_y = (b["min_y"] + b["max_y"]) / 2.0

    bpy.ops.mesh.primitive_plane_add(size=1, location=(origin_x, origin_y, 0))
    terrain_obj = bpy.context.object
    terrain_obj.name = "terrain_mesh"
    terrain_obj.scale = (width, depth, 1.0)
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)

    # Subdivide one quad per tile.
    bpy.ops.object.mode_set(mode="EDIT")
    bpy.ops.mesh.select_all(action="SELECT")
    # Subdivide so we get width*depth cells.
    cuts_x = max(1, width - 1)
    cuts_y = max(1, depth - 1)
    bpy.ops.mesh.subdivide(number_cuts=max(cuts_x, cuts_y))
    bpy.ops.object.mode_set(mode="OBJECT")

    # Move to terrain collection and remove from default.
    for c in terrain_obj.users_collection:
        c.objects.unlink(terrain_obj)
    terrain_col.objects.link(terrain_obj)

    _apply_terrain_heights(terrain_obj, plan)
    _apply_terrain_vertex_colors(terrain_obj, plan)

    # --- Props ---
    for entry in plan["placements"]:
        _create_placement_object(entry, props_col)

    # Save
    bpy.ops.wm.save_as_mainfile(filepath=plan["world_blend_path"])
    pc.log("generate_world_blend", f"saved {plan['world_blend_path']}")


def _apply_terrain_heights(obj, plan) -> None:
    import bpy  # type: ignore

    # Map (x, y) integer tile → level.
    level_by_tile = {}
    for r in plan["height_regions"]:
        for x in range(r["min_x"], r["max_x"]):
            for y in range(r["min_y"], r["max_y"]):
                level_by_tile[(x, y)] = max(level_by_tile.get((x, y), 0), r["level"])

    step = plan["height_step"]
    for v in obj.data.vertices:
        tx = int(round(v.co.x))
        ty = int(round(v.co.y))
        v.co.z = level_by_tile.get((tx, ty), 0) * step


def _apply_terrain_vertex_colors(obj, plan) -> None:
    import bpy  # type: ignore

    mesh = obj.data
    layer = mesh.color_attributes.new(name="Col", type="BYTE_COLOR", domain="CORNER")

    # Build tile → color LUT.
    tile_color = {}
    for r in plan["visual_regions"]:
        col = VISUAL_TYPE_COLORS.get(r["type"], VISUAL_TYPE_COLORS["grass"])
        for x in range(r["min_x"], r["max_x"]):
            for y in range(r["min_y"], r["max_y"]):
                tile_color[(x, y)] = col
    for o in plan["tile_overrides"]:
        tile_color[(o["x"], o["y"])] = VISUAL_TYPE_COLORS.get(o["type"], VISUAL_TYPE_COLORS["grass"])
    default_col = VISUAL_TYPE_COLORS.get(plan["visual_default"], VISUAL_TYPE_COLORS["grass"])

    # For each loop (face corner), look up color by nearest tile of its vertex.
    for loop in mesh.loops:
        v = mesh.vertices[loop.vertex_index]
        tx = int(round(v.co.x))
        ty = int(round(v.co.y))
        col = tile_color.get((tx, ty), default_col)
        layer.data[loop.index].color = col


def _create_placement_object(entry: dict, props_col) -> None:
    import bpy  # type: ignore
    from mathutils import Vector  # type: ignore
    import math  # local import to keep top of file import-clean

    name = entry["name"]
    if entry["source_blend"]:
        obj = _create_linked_instance(entry, props_col, name)
    if not entry["source_blend"] or obj is None:
        obj = _create_proxy_cube(entry, props_col, name)

    obj.location = (float(entry["x"]), float(entry["y"]), 0.0)
    obj.rotation_euler = (0.0, 0.0, math.radians(float(entry["rotation_y_degrees"])))
    s = float(entry["scale"])
    obj.scale = (s, s, s)

    # Custom properties consumed by the runtime.
    obj["manifest_key"] = entry["key"]
    obj["visibility_group"] = entry["visibility_group"]


def _create_linked_instance(entry: dict, props_col, name: str):
    """Return an Object that is an empty instancing the source .blend's top collection,
    or None if the link fails."""
    import bpy  # type: ignore

    src = entry["source_blend"]
    # Try to link the top-level collection from the source blend.
    try:
        with bpy.data.libraries.load(src, link=True) as (data_from, data_to):
            if data_from.collections:
                data_to.collections = [data_from.collections[0]]
            else:
                return None
        linked_col = bpy.data.collections[data_to.collections[0]]
    except Exception as e:
        pc.log("generate_world_blend", f"WARN: link failed for {src}: {e}")
        return None

    inst = bpy.data.objects.new(name, None)
    inst.instance_type = "COLLECTION"
    inst.instance_collection = linked_col
    inst.empty_display_size = 0.5
    props_col.objects.link(inst)
    return inst


def _create_proxy_cube(entry: dict, props_col, name: str):
    import bpy  # type: ignore

    fp = CATEGORY_FOOTPRINT.get(entry["category"], CATEGORY_FOOTPRINT[""])
    col = CATEGORY_COLORS.get(entry["category"], CATEGORY_COLORS[""])

    mesh = bpy.data.meshes.new(name + "_proxy_mesh")
    # Single-box mesh built from bmesh for cleanliness.
    import bmesh  # type: ignore
    bm = bmesh.new()
    bmesh.ops.create_cube(bm, size=1.0)
    bm.to_mesh(mesh)
    bm.free()
    mesh.validate()

    obj = bpy.data.objects.new(name, mesh)
    obj.scale = fp
    obj.show_wire = True
    # Give it a flat-color material so the cube is readable in the viewport.
    mat = bpy.data.materials.new(name + "_proxy_mat")
    mat.diffuse_color = col
    mat.use_nodes = False
    obj.data.materials.append(mat)
    props_col.objects.link(obj)
    return obj


# --------------------------------------------------------------------------
# Entry
# --------------------------------------------------------------------------

def main() -> int:
    args = pc.parse_world_arg()
    repo = pc.repo_root_from_script(__file__)
    plan = build_plan(repo, args.world)
    print_plan_summary(plan)

    if args.dry_run:
        pc.log("generate_world_blend", "dry-run requested; not invoking Blender")
        return 0

    try:
        import bpy  # noqa: F401
    except ImportError:
        pc.log("generate_world_blend",
               "ERROR: bpy not available. Run this via Blender: "
               "`blender --background --python scripts/blender/generate_world_blend.py -- --world=<id>`")
        return 2

    # Ensure parent dir exists.
    Path(plan["world_blend_path"]).parent.mkdir(parents=True, exist_ok=True)
    run_in_blender(plan)
    return 0


if __name__ == "__main__":
    sys.exit(main())
