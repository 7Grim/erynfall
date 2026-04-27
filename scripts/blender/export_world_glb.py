"""Export `world.blend` to `world.glb` for runtime consumption.

The exported file is the authoritative scene the game client loads. It contains:

- the `terrain_mesh` object as a first-class mesh node;
- every object under the `props` collection as a named transform node
  (with geometry if it's a linked-instance placement, or as an Empty if it's a
  proxy cube we'd rather not ship — see `--proxy-mode` below);
- custom properties (`manifest_key`, `visibility_group`) are exported as
  glTF `extras` on each node, which gdx-gltf preserves.

Run headless:

    blender --background --python scripts/blender/export_world_glb.py -- --world=main_world

Options (after --):
    --world=<id>           World id. Default `main_world`.
    --proxy-mode=<mode>    How to export proxy-cube placements. Default `empty`.
                           `empty`   : export as Empty node (no geometry). Keeps the
                                       glb small. Runtime looks up geometry via
                                       manifest_key property.
                           `mesh`    : export the proxy cube mesh too. Useful for
                                       inspecting the glb content without a runtime.
"""

from __future__ import annotations

import argparse
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
    parser.add_argument("--proxy-mode", choices=["empty", "mesh"], default="empty")
    return parser.parse_args(pc.blender_argv_after_double_dash())


def _convert_proxies_to_empties() -> None:
    import bpy  # type: ignore

    props = bpy.data.collections.get("props")
    if props is None:
        return
    for obj in list(props.objects):
        # Linked-instance placements have instance_type == 'COLLECTION' and no mesh data.
        # Proxy cubes have mesh data and no instance_collection.
        is_linked = obj.instance_type == "COLLECTION" and obj.instance_collection is not None
        if is_linked or obj.type != "MESH":
            continue
        name = obj.name
        loc = tuple(obj.location)
        rot = tuple(obj.rotation_euler)
        sca = tuple(obj.scale)
        manifest_key = obj.get("manifest_key")
        vis_group = obj.get("visibility_group")
        # Delete the mesh object and replace with an Empty that preserves transform + extras.
        bpy.data.objects.remove(obj, do_unlink=True)
        empty = bpy.data.objects.new(name, None)
        empty.location = loc
        empty.rotation_euler = rot
        empty.scale = sca
        empty.empty_display_type = "PLAIN_AXES"
        empty.empty_display_size = 0.25
        if manifest_key:
            empty["manifest_key"] = manifest_key
        if vis_group:
            empty["visibility_group"] = vis_group
        props.objects.link(empty)


def _export(world_glb_path: str) -> None:
    import bpy  # type: ignore

    bpy.ops.export_scene.gltf(
        filepath=world_glb_path,
        export_format="GLB",
        export_yup=True,
        export_apply=True,
        export_animations=False,  # world scene is static
        export_extras=True,       # preserve custom properties as glTF extras
        export_lights=False,
        export_cameras=False,
        use_selection=False,
        use_visible=True,
        export_colors=True,
        export_normals=True,
    )


def main() -> int:
    args = _extra_args()
    repo = pc.repo_root_from_script(__file__)
    blend_path = pc.world_blend_path(repo, args.world)
    glb_path = pc.world_glb_path(repo, args.world)

    if not blend_path.exists():
        pc.log("export_world_glb", f"ERROR: {blend_path} does not exist. Run generate_world_blend.py first.")
        return 1

    pc.log("export_world_glb", f"world={args.world} proxy_mode={args.proxy_mode}")
    pc.log("export_world_glb", f"source: {blend_path}")
    pc.log("export_world_glb", f"target: {glb_path}")

    if args.dry_run:
        pc.log("export_world_glb", "dry-run requested; not invoking Blender")
        return 0

    try:
        import bpy  # type: ignore
    except ImportError:
        pc.log("export_world_glb",
               "ERROR: bpy not available. Run this via Blender: "
               "`blender --background --python scripts/blender/export_world_glb.py -- --world=<id>`")
        return 2

    bpy.ops.wm.open_mainfile(filepath=str(blend_path))
    if args.proxy_mode == "empty":
        _convert_proxies_to_empties()
    _export(str(glb_path))
    pc.log("export_world_glb", f"wrote {glb_path}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
