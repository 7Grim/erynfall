#!/usr/bin/env python3
"""
Convert G3DJ files to GLB format.

Handles all asset types:
  - Static props/resources: multi-mesh, multi-material, node scale
  - NPC _idle/_walk/_action snapshots: same as above
  - Humanoid NPC _base (nested node hierarchy): multi-node tree with COLOR vertex attrs
  - Animal NPC _base (real keyframe animations): GLTF animation clips (idle/walk/action)
  - Equipment G3DJ files: static meshes (empty animations list is ignored)

Usage:
    python3 scripts/convert_g3dj_to_glb.py [--dry-run] [--target key1,key2,...]
    python3 scripts/convert_g3dj_to_glb.py --all-static
    python3 scripts/convert_g3dj_to_glb.py --all

Output: art/models/<key>.glb alongside (then replacing) the original .g3dj files.
"""
import argparse
import json
import os
import struct
import sys
from pathlib import Path


MODELS_DIR = Path(__file__).parent.parent / "art" / "models"
MANIFEST_PATH = Path(__file__).parent.parent / "art" / "models" / "manifest.yaml"

STARTER_ZONE_STATIC = [
    "dock_pier_small", "dock_platform", "dock_stairs",
    "furnace", "cooking_range", "fishing_spot",
    "crate_small", "barrel_small", "sack_stack_small",
    "signpost_small", "bench_small", "cart_small", "table_small", "fence_post_small",
    "rock_copper", "rock_tin", "rock_iron", "rock_coal", "rock_gold",
    "rock_mithril", "rock_silver", "rock_adamantite", "rock_runite",
    "tree_willow", "tree_maple", "tree_yew", "tree_mahogany", "tree_magic",
]


# ---------------------------------------------------------------------------
# Binary helpers
# ---------------------------------------------------------------------------

def _align4(n):
    return (n + 3) & ~3


class BinBuf:
    """Accumulates binary data and tracks byte offsets."""

    def __init__(self):
        self._data = bytearray()

    def append(self, b: bytes) -> int:
        off = len(self._data)
        self._data += b
        pad = _align4(len(b)) - len(b)
        self._data += b"\x00" * pad
        return off

    def __len__(self):
        return len(self._data)

    def bytes(self):
        return bytes(self._data)


# ---------------------------------------------------------------------------
# GLB assembly
# ---------------------------------------------------------------------------

def assemble_glb(gltf_dict: dict, bin_buf: BinBuf) -> bytes:
    json_str = json.dumps(gltf_dict, separators=(",", ":"))
    json_bytes = json_str.encode("utf-8")
    json_pad = _align4(len(json_bytes)) - len(json_bytes)
    json_bytes += b" " * json_pad

    bin_data = bin_buf.bytes()
    bin_pad = _align4(len(bin_data)) - len(bin_data)
    bin_data += b"\x00" * bin_pad

    JSON_TYPE = 0x4E4F534A
    BIN_TYPE  = 0x004E4942
    json_chunk = struct.pack("<II", len(json_bytes), JSON_TYPE) + json_bytes
    bin_chunk  = struct.pack("<II", len(bin_data),  BIN_TYPE)  + bin_data

    total = 12 + len(json_chunk) + len(bin_chunk)
    header = struct.pack("<III", 0x46546C67, 2, total)
    return header + json_chunk + bin_chunk


# ---------------------------------------------------------------------------
# Accessor / buffer-view helpers
# ---------------------------------------------------------------------------

def _add_vec3_accessor(buf, accessors, buffer_views, floats, elem_type="VEC3",
                       target=34962, with_minmax=False):
    raw = struct.pack(f"<{len(floats)}f", *floats)
    off = buf.append(raw)
    bv = len(buffer_views)
    buffer_views.append({"buffer": 0, "byteOffset": off, "byteLength": len(raw), "target": target})
    acc = {"bufferView": bv, "byteOffset": 0, "componentType": 5126, "count": len(floats) // 3, "type": elem_type}
    if with_minmax:
        xs = floats[0::3]; ys = floats[1::3]; zs = floats[2::3]
        acc["min"] = [min(xs), min(ys), min(zs)]
        acc["max"] = [max(xs), max(ys), max(zs)]
    idx = len(accessors)
    accessors.append(acc)
    return idx


def _add_vec4_accessor(buf, accessors, buffer_views, floats, target=34962):
    raw = struct.pack(f"<{len(floats)}f", *floats)
    off = buf.append(raw)
    bv = len(buffer_views)
    buffer_views.append({"buffer": 0, "byteOffset": off, "byteLength": len(raw), "target": target})
    idx = len(accessors)
    accessors.append({"bufferView": bv, "byteOffset": 0, "componentType": 5126,
                      "count": len(floats) // 4, "type": "VEC4"})
    return idx


def _add_scalar_accessor(buf, accessors, buffer_views, floats):
    raw = struct.pack(f"<{len(floats)}f", *floats)
    off = buf.append(raw)
    bv = len(buffer_views)
    buffer_views.append({"buffer": 0, "byteOffset": off, "byteLength": len(raw)})
    idx = len(accessors)
    accessors.append({"bufferView": bv, "byteOffset": 0, "componentType": 5126,
                      "count": len(floats), "type": "SCALAR",
                      "min": [min(floats)], "max": [max(floats)]})
    return idx


def _add_index_accessor(buf, accessors, buffer_views, indices, n_verts):
    use_u32 = n_verts > 65535
    if use_u32:
        raw = struct.pack(f"<{len(indices)}I", *indices)
        comp = 5125
    else:
        raw = struct.pack(f"<{len(indices)}H", *indices)
        comp = 5123
    off = buf.append(raw)
    bv = len(buffer_views)
    buffer_views.append({"buffer": 0, "byteOffset": off, "byteLength": len(raw), "target": 34963})
    idx = len(accessors)
    accessors.append({"bufferView": bv, "byteOffset": 0, "componentType": comp,
                      "count": len(indices), "type": "SCALAR"})
    return idx


# ---------------------------------------------------------------------------
# Primitive builder (shared by static and animated paths)
# ---------------------------------------------------------------------------

def build_primitive(buf, accessors, buffer_views, materials,
                    raw_verts, fpv, attrs, indices, diffuse):
    """Build one GLTF primitive from G3DJ mesh data. Returns (primitive_dict, material_idx)."""
    n_verts = len(raw_verts) // fpv
    has_pos    = "POSITION" in attrs
    has_normal = "NORMAL" in attrs
    has_color  = "COLOR" in attrs

    pos_off    = attrs.index("POSITION") if has_pos else 0
    norm_off   = attrs.index("NORMAL")   if has_normal else -1
    color_off  = attrs.index("COLOR")    if has_color else -1

    positions = []
    normals   = []
    colors    = []

    for vi in range(n_verts):
        base = vi * fpv
        if has_pos:
            positions.extend(raw_verts[base + pos_off: base + pos_off + 3])
        if has_normal:
            normals.extend(raw_verts[base + norm_off: base + norm_off + 3])
        if has_color:
            colors.extend(raw_verts[base + color_off: base + color_off + 4])

    prim_attrs = {}

    if positions:
        prim_attrs["POSITION"] = _add_vec3_accessor(buf, accessors, buffer_views,
                                                    positions, with_minmax=True)
    if normals:
        prim_attrs["NORMAL"] = _add_vec3_accessor(buf, accessors, buffer_views, normals)

    if colors:
        prim_attrs["COLOR_0"] = _add_vec4_accessor(buf, accessors, buffer_views, colors)

    idx_acc = _add_index_accessor(buf, accessors, buffer_views, indices, n_verts)

    r, g, b, a = (list(diffuse) + [1.0, 1.0, 1.0, 1.0])[:4]
    mat_idx = len(materials)
    materials.append({
        "pbrMetallicRoughness": {
            "baseColorFactor": [r, g, b, a],
            "metallicFactor": 0.0,
            "roughnessFactor": 0.9,
        },
        "doubleSided": False,
    })

    prim = {"attributes": prim_attrs, "indices": idx_acc, "material": mat_idx, "mode": 4}
    return prim


# ---------------------------------------------------------------------------
# Node tree builder (for articulated NPC skeletons)
# ---------------------------------------------------------------------------

def flatten_nodes(g3dj_nodes):
    """DFS-flatten G3DJ node tree. Returns (flat_list, id_to_flat_idx)."""
    flat = []
    id_to_idx = {}

    def visit(n):
        idx = len(flat)
        id_to_idx[n["id"]] = idx
        flat.append(n)
        for child in n.get("children", []):
            visit(child)

    for n in g3dj_nodes:
        visit(n)

    return flat, id_to_idx


def build_gltf_nodes(flat_nodes, id_to_idx, gltf_meshes_for_node):
    """
    Build GLTF nodes list from flattened G3DJ nodes.
    gltf_meshes_for_node: dict(flat_idx → gltf_mesh_idx or None)
    Returns list of GLTF node dicts.
    """
    result = []
    for i, n in enumerate(flat_nodes):
        gn = {"name": n["id"]}
        t = n.get("translation")
        r = n.get("rotation")
        s = n.get("scale")
        if t and t != [0.0, 0.0, 0.0]:
            gn["translation"] = [float(v) for v in t]
        if r:
            gn["rotation"] = [float(v) for v in r]
        if s and s != [1.0, 1.0, 1.0]:
            gn["scale"] = [float(v) for v in s]
        children = [id_to_idx[c["id"]] for c in n.get("children", [])]
        if children:
            gn["children"] = children
        mesh_idx = gltf_meshes_for_node.get(i)
        if mesh_idx is not None:
            gn["mesh"] = mesh_idx
        result.append(gn)
    return result


# ---------------------------------------------------------------------------
# Animation converter
# ---------------------------------------------------------------------------

def build_gltf_animations(g3dj_anims, id_to_node_idx, buf, accessors, buffer_views):
    """
    Convert G3DJ animation list to GLTF animations.
    g3dj_anims: list of {id, bones: [{boneId, keyframes: [{keytime_ms, translation, rotation, scale}]}]}
    id_to_node_idx: maps G3DJ node id → GLTF node index
    Returns list of GLTF animation dicts.
    """
    gltf_anims = []

    for anim in g3dj_anims:
        channels = []
        samplers = []

        for bone in anim.get("bones", []):
            bone_id  = bone["boneId"]
            node_idx = id_to_node_idx.get(bone_id)
            if node_idx is None:
                continue

            kfs = bone.get("keyframes", [])
            if not kfs:
                continue

            times_s = [kf["keytime"] / 1000.0 for kf in kfs]

            # Only add channels where values actually change (avoids zero-size accessors).
            t_vals = []
            r_vals = []
            s_vals = []
            has_t = has_r = has_s = False

            for kf in kfs:
                t = kf.get("translation", [0.0, 0.0, 0.0])
                r = kf.get("rotation",    [0.0, 0.0, 0.0, 1.0])
                s = kf.get("scale",       [1.0, 1.0, 1.0])
                t_vals.extend(t)
                r_vals.extend(r)
                s_vals.extend(s)

            # Check if channel has any non-identity keyframes.
            identity_t = all(abs(v) < 1e-8 for v in t_vals)
            identity_r = all(abs(t_vals[i] - ([0,0,0,1]*len(kfs))[i]) < 1e-8
                             for i in range(len(r_vals))) if r_vals else True
            identity_s = all(abs(v - 1.0) < 1e-8 for v in s_vals)

            has_t = not identity_t
            has_r = not identity_r
            has_s = not identity_s

            for (active, path, vals, n_components) in [
                (has_t, "translation", t_vals, 3),
                (has_r, "rotation",    r_vals, 4),
                (has_s, "scale",       s_vals, 3),
            ]:
                if not active:
                    continue
                time_acc = _add_scalar_accessor(buf, accessors, buffer_views, times_s)
                if n_components == 3:
                    val_acc = _add_vec3_accessor(buf, accessors, buffer_views, vals, target=0)
                else:
                    val_acc = _add_vec4_accessor(buf, accessors, buffer_views, vals, target=0)

                sampler_idx = len(samplers)
                samplers.append({"input": time_acc, "interpolation": "LINEAR", "output": val_acc})
                channels.append({"sampler": sampler_idx, "target": {"node": node_idx, "path": path}})

        if channels:
            gltf_anims.append({"name": anim["id"], "channels": channels, "samplers": samplers})

    return gltf_anims


# ---------------------------------------------------------------------------
# Main converter
# ---------------------------------------------------------------------------

def g3dj_to_glb(g3dj_path: Path, out_path: Path, dry_run: bool = False) -> bool:
    with open(g3dj_path) as f:
        d = json.load(f)

    real_anims = [a for a in d.get("animations", []) if a.get("bones")]
    has_real_anims = len(real_anims) > 0

    meshes_by_id  = {m["id"]: m for m in d.get("meshes", [])}
    mats_by_id    = {m["id"]: m for m in d.get("materials", [])}
    g3dj_nodes    = d.get("nodes", [])
    has_hierarchy = any(n.get("children") for n in g3dj_nodes)

    if dry_run:
        kind = "animated" if has_real_anims else ("hierarchy" if has_hierarchy else "static")
        print(f"  DRY RUN ({kind}): {g3dj_path.name}")
        return True

    buf           = BinBuf()
    accessors     = []
    buffer_views  = []
    materials     = []
    gltf_meshes   = []
    gltf_nodes_list = []

    # ------------------------------------------------------------------
    # Helper: build a primitive from a (meshpartid → part) + materialid.
    # ------------------------------------------------------------------
    def _find_part(meshpart_id):
        for mesh in d["meshes"]:
            for mp in mesh.get("parts", []):
                if mp["id"] == meshpart_id:
                    return mesh, mp
        return None, None

    def _prim_from_ref(part_ref):
        owner_mesh, mesh_part = _find_part(part_ref["meshpartid"])
        if owner_mesh is None:
            return None
        attrs     = owner_mesh["attributes"]
        raw_verts = owner_mesh["vertices"]
        indices   = mesh_part["indices"]
        fpv       = len(attrs)
        mat       = mats_by_id.get(part_ref["materialid"], {})
        diffuse   = mat.get("diffuse", [0.8, 0.8, 0.8, 1.0])
        return build_primitive(buf, accessors, buffer_views, materials,
                               raw_verts, fpv, attrs, indices, diffuse)

    # ------------------------------------------------------------------
    # Path A: hierarchy (humanoid NPC base)
    # ------------------------------------------------------------------
    if has_hierarchy:
        flat_nodes, id_to_idx = flatten_nodes(g3dj_nodes)
        gltf_mesh_for_node = {}

        for flat_idx, node in enumerate(flat_nodes):
            parts = node.get("parts", [])
            if not parts:
                continue
            prims = [p for p in (_prim_from_ref(pr) for pr in parts) if p is not None]
            if not prims:
                continue
            mesh_idx = len(gltf_meshes)
            gltf_meshes.append({"primitives": prims, "name": node["id"]})
            gltf_mesh_for_node[flat_idx] = mesh_idx

        gltf_nodes_list = build_gltf_nodes(flat_nodes, id_to_idx, gltf_mesh_for_node)
        root_indices = [id_to_idx[n["id"]] for n in g3dj_nodes]

        gltf = {
            "asset": {"version": "2.0", "generator": "erynfall g3dj->glb"},
            "scene": 0,
            "scenes": [{"nodes": root_indices, "name": "Scene"}],
            "nodes": gltf_nodes_list,
            "meshes": gltf_meshes,
            "accessors": accessors,
            "bufferViews": buffer_views,
            "buffers": [{"byteLength": len(buf)}],
            "materials": materials,
        }

    # ------------------------------------------------------------------
    # Path B: animated (animal NPC base) — single-root with animation clips
    # ------------------------------------------------------------------
    elif has_real_anims:
        flat_nodes, id_to_idx = flatten_nodes(g3dj_nodes)

        # Build mesh for each node that has parts.
        for flat_idx, node in enumerate(flat_nodes):
            parts = node.get("parts", [])
            if not parts:
                continue
            prims = [p for p in (_prim_from_ref(pr) for pr in parts) if p is not None]
            if not prims:
                continue
            mesh_idx = len(gltf_meshes)
            gltf_meshes.append({"primitives": prims, "name": node["id"]})

        # Build GLTF node list.
        gltf_mesh_for_node = {i: i for i in range(len(flat_nodes)) if i < len(gltf_meshes)}
        gltf_nodes_list = build_gltf_nodes(flat_nodes, id_to_idx, gltf_mesh_for_node)

        # Build animations.
        gltf_anims = build_gltf_animations(real_anims, id_to_idx, buf, accessors, buffer_views)
        root_indices = [id_to_idx[n["id"]] for n in g3dj_nodes]

        gltf = {
            "asset": {"version": "2.0", "generator": "erynfall g3dj->glb"},
            "scene": 0,
            "scenes": [{"nodes": root_indices, "name": "Scene"}],
            "nodes": gltf_nodes_list,
            "meshes": gltf_meshes,
            "accessors": accessors,
            "bufferViews": buffer_views,
            "buffers": [{"byteLength": len(buf)}],
            "materials": materials,
            "animations": gltf_anims,
        }

    # ------------------------------------------------------------------
    # Path C: static (flat nodes, no hierarchy, no animations)
    # ------------------------------------------------------------------
    else:
        all_prims = []
        for node in g3dj_nodes:
            node_scale = node.get("scale")
            for part_ref in node.get("parts", []):
                prim = _prim_from_ref(part_ref)
                if prim:
                    all_prims.append((prim, node_scale))

        if not all_prims:
            print(f"  SKIP (no primitives): {g3dj_path.name}")
            return False

        prims_list = [p for p, _ in all_prims]
        gltf_meshes.append({"primitives": prims_list, "name": g3dj_path.stem})

        node_scale = all_prims[0][1] if all_prims else None
        gltf_node = {"mesh": 0, "name": g3dj_path.stem}
        if node_scale and node_scale != [1.0, 1.0, 1.0]:
            gltf_node["scale"] = [float(v) for v in node_scale]

        gltf = {
            "asset": {"version": "2.0", "generator": "erynfall g3dj->glb"},
            "scene": 0,
            "scenes": [{"nodes": [0], "name": "Scene"}],
            "nodes": [gltf_node],
            "meshes": gltf_meshes,
            "accessors": accessors,
            "bufferViews": buffer_views,
            "buffers": [{"byteLength": len(buf)}],
            "materials": materials,
        }

    if not accessors:
        print(f"  SKIP (no geometry): {g3dj_path.name}")
        return False

    gltf["buffers"][0]["byteLength"] = len(buf)
    glb = assemble_glb(gltf, buf)
    out_path.write_bytes(glb)

    anim_tag = f" +{len(real_anims)} anims" if has_real_anims else ""
    hier_tag = " +hierarchy" if has_hierarchy else ""
    print(f"  OK: {out_path.name}  ({len(gltf_meshes)} mesh{anim_tag}{hier_tag}, {len(glb)} bytes)")
    return True


# ---------------------------------------------------------------------------
# Manifest updater
# ---------------------------------------------------------------------------

def update_manifest_entry(manifest_path: Path, key: str, new_file: str) -> bool:
    text = manifest_path.read_text()
    lines = text.splitlines(keepends=True)
    changed = False
    in_key = False
    new_lines = []
    for line in lines:
        stripped = line.strip()
        # Exact key match only — avoid partial prefix matches (e.g. "tree" matching "tree_oak").
        if stripped == f"- key: {key}":
            in_key = True
        if in_key and stripped.startswith("file:"):
            line = line[:line.index("file:")] + f"file: {new_file}\n"
            changed = True
        if in_key and stripped.startswith("format:"):
            line = line[:line.index("format:")] + "format: glb\n"
        new_lines.append(line)
        # New entry starts — exit current key block.
        if in_key and stripped.startswith("- key:") and stripped != f"- key: {key}":
            in_key = False
    if changed:
        manifest_path.write_text("".join(new_lines))
    return changed


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--target", default="",
                        help="Comma-separated asset keys")
    parser.add_argument("--all-static", action="store_true",
                        help="Convert all non-animated G3DJ files")
    parser.add_argument("--all", action="store_true",
                        help="Convert ALL G3DJ files (including animated NPC bases)")
    parser.add_argument("--delete", action="store_true",
                        help="Delete source .g3dj after successful conversion")
    args = parser.parse_args()

    if args.target:
        targets = [t.strip() for t in args.target.split(",") if t.strip()]
        g3dj_files = [MODELS_DIR / f"{k}.g3dj" for k in targets]
    elif args.all or args.all_static:
        g3dj_files = sorted(MODELS_DIR.glob("*.g3dj"))
    else:
        g3dj_files = [MODELS_DIR / f"{k}.g3dj" for k in STARTER_ZONE_STATIC]

    converted = skipped = 0
    for g3dj_path in g3dj_files:
        if not g3dj_path.exists():
            print(f"  MISSING: {g3dj_path.name}")
            skipped += 1
            continue
        key = g3dj_path.stem
        out_path = MODELS_DIR / f"{key}.glb"
        if out_path.exists() and not args.dry_run:
            print(f"  EXISTS:  {out_path.name} (skipping)")
            skipped += 1
            continue
        ok = g3dj_to_glb(g3dj_path, out_path, dry_run=args.dry_run)
        if ok:
            converted += 1
            if not args.dry_run:
                update_manifest_entry(MANIFEST_PATH, key, f"{key}.glb")
                if args.delete:
                    g3dj_path.unlink()
        else:
            skipped += 1

    print(f"\nDone. converted={converted} skipped={skipped}")
    if args.dry_run:
        print("(dry run — no files written)")


if __name__ == "__main__":
    main()
