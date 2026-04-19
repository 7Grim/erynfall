#!/usr/bin/env python3
"""
gen_tutorial_map.py — Regenerate main_world map.yaml for server + client.

Paints a 320×192 walkability grid using rectangular paint regions applied in
order (later regions override earlier).  Writes identical output to both:
  server/src/main/resources/worlds/main_world/map.yaml
  client/src/main/resources/worlds/main_world/map.yaml

Tile IDs:
  0 = Grass  (walkable)
  1 = Water  (non-walkable)  ← default/ocean
  2 = Sand   (walkable)
  3 = Rock   (non-walkable)
  4 = Tree   (non-walkable)

Coordinate system: grid[y][x], row 0 = y=0 (north edge of map).
Tutorial island region in world: x 20-92, y 62-130.
Mainland scaffold region:        x 196-308, y 56-142.
"""

import math
import os
import pathlib

# ── Map dimensions ────────────────────────────────────────────────────────────
WIDTH  = 320
HEIGHT = 192

# ── Tile IDs ──────────────────────────────────────────────────────────────────
GRASS = 0
WATER = 1
SAND  = 2
ROCK  = 3
TREE  = 4


# ── Building model half-extents (model-space hw, hd before scale) ─────────────
_BUILDING_HALF_EXTENTS = {
    'small':   (2.10, 1.90),
    'service': (2.50, 2.10),
    'coastal': (2.10, 1.90),
}


def paint_building_walls(grid, cx, cy, model_type, scale, rot_deg=0, door_gaps=None):
    """
    Paint ROCK perimeter walls for a building shell prop, with optional door gaps.

    cx, cy      — tile-space placement coordinates matching scene.yaml x/y
    model_type  — 'small' | 'service' | 'coastal'
    scale       — placement scale (same as scene.yaml scale)
    rot_deg     — rotation in degrees (0/90/180/270)
    door_gaps   — dict mapping side ('N'/'S'/'E'/'W') → iterable of tile coords
                  along that wall that should remain walkable (door opening).
                  For N/S walls, coords are x values.
                  For E/W walls, coords are y values.
    """
    hw_m, hd_m = _BUILDING_HALF_EXTENTS[model_type]
    hw = hw_m * scale
    hd = hd_m * scale

    # 90°/270° rotation swaps width ↔ depth
    snap = round(rot_deg / 90) % 4
    if snap in (1, 3):
        hw, hd = hd, hw

    center_x = cx + 0.5
    center_y = cy + 0.5

    x_min = math.floor(center_x - hw)
    x_max = math.floor(center_x + hw - 0.001)
    y_min = math.floor(center_y - hd)
    y_max = math.floor(center_y + hd - 0.001)

    gaps = door_gaps or {}
    north_gaps = set(gaps.get('N', []))
    south_gaps = set(gaps.get('S', []))
    west_gaps  = set(gaps.get('W', []))
    east_gaps  = set(gaps.get('E', []))

    # North wall
    for x in range(x_min, x_max + 1):
        if x not in north_gaps:
            paint_point(grid, x, y_min, ROCK)
    # South wall
    for x in range(x_min, x_max + 1):
        if x not in south_gaps:
            paint_point(grid, x, y_max, ROCK)
    # West wall (skip corners — already painted above)
    for y in range(y_min + 1, y_max):
        if y not in west_gaps:
            paint_point(grid, x_min, y, ROCK)
    # East wall (skip corners)
    for y in range(y_min + 1, y_max):
        if y not in east_gaps:
            paint_point(grid, x_max, y, ROCK)

    # Interior guaranteed walkable (grass)
    paint_rect(grid, x_min + 1, y_min + 1, x_max - 1, y_max - 1, GRASS)


def paint_rect(grid, x0, y0, x1, y1, tile):
    """Paint all cells in [x0..x1] × [y0..y1] (inclusive) with tile."""
    for y in range(max(0, y0), min(HEIGHT, y1 + 1)):
        for x in range(max(0, x0), min(WIDTH, x1 + 1)):
            grid[y][x] = tile


def paint_point(grid, x, y, tile):
    if 0 <= x < WIDTH and 0 <= y < HEIGHT:
        grid[y][x] = tile


def build_grid():
    # Layer 1: base — all ocean water
    grid = [[WATER] * WIDTH for _ in range(HEIGHT)]

    # ── Tutorial Island ───────────────────────────────────────────────────────

    # Layer 2: Sandy beach perimeter (bounding box of island)
    paint_rect(grid, 20, 62, 92, 130, SAND)

    # Layer 3: Grass interior — main walkable surface
    paint_rect(grid, 25, 67, 87, 125, GRASS)

    # Layer 4: Fishing cove — narrow water core only.
    #   Fishing spot NPCs sit at (32,82) and (34,84) which must be walkable so
    #   the player can stand on them.  The cove is kept narrow (x:24-29) so
    #   the shore east of it (x:30+) stays as grass/sand.
    #   The visual scene.yaml paints a wider cove appearance independently.
    paint_rect(grid, 24, 77, 29, 84, WATER)

    # Layer 6: Non-walkable tree positions (NPC trees block movement)
    for (x, y) in [(46, 106), (50, 110), (40, 110), (38, 114)]:
        paint_point(grid, x, y, TREE)

    # Layer 7: Non-walkable ore rock positions
    for (x, y) in [(63, 84), (66, 83)]:
        paint_point(grid, x, y, ROCK)

    # ── Building wall collision ───────────────────────────────────────────────
    # Hub (small, x=58, y=97, scale=2.5): player spawns inside — no walls; it
    # sits on the main path hub so wall blocking here would break movement flow.

    # Forge / smithing hut (service, x=71, y=88, scale=2.0)
    # Walls: N(y=84) S(y=92) W(x=66) E(x=76). Door: west wall, y:87..89.
    # Anvil(67,86) and Furnace(70,86) are interior; rocks + instructor outdoor.
    # Players walk directly from mine area (x<66) through west door into forge.
    paint_building_walls(grid, 71, 88, 'service', 2.0, rot_deg=0,
                         door_gaps={'W': range(87, 90)})  # 3-tile west door

    # Armory (small, x=72, y=123, scale=1.5) — decorative; south of combat yard.
    # Walls: N(y=121) S(y=126) W(x=69) E(x=75). Door: north wall (facing yard).
    paint_building_walls(grid, 72, 123, 'small', 1.5, rot_deg=0,
                         door_gaps={'N': range(71, 74)})  # 3-tile north door

    # ── Mainland Scaffold ─────────────────────────────────────────────────────

    # Layer 8: Mainland sandy shore
    paint_rect(grid, 196, 56, 308, 142, SAND)

    # Layer 9: Mainland grass interior
    paint_rect(grid, 202, 62, 302, 136, GRASS)

    return grid


YAML_HEADER = """\
# Main World Map — generated by scripts/gen_tutorial_map.py
# Tutorial island: x 20-92, y 62-130  |  Mainland: x 196-308, y 56-142
# To regenerate: python3 scripts/gen_tutorial_map.py

width: {width}
height: {height}

tiles:
  - id: 0
    name: Grass
    walkable: true
    sprite_index: 0
  - id: 1
    name: Water
    walkable: false
    sprite_index: 1
  - id: 2
    name: Sand
    walkable: true
    sprite_index: 2
  - id: 3
    name: Rock
    walkable: false
    sprite_index: 3
  - id: 4
    name: Tree
    walkable: false
    sprite_index: 4

layout: |
""".format(width=WIDTH, height=HEIGHT)


def write_map_yaml(path, grid):
    pathlib.Path(path).parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(YAML_HEADER)
        for row in grid:
            f.write("  " + " ".join(str(t) for t in row) + "\n")
    print(f"  wrote {path}  ({os.path.getsize(path):,} bytes)")


def main():
    repo_root = pathlib.Path(__file__).parent.parent.resolve()

    targets = [
        repo_root / "server" / "src" / "main" / "resources" / "worlds" / "main_world" / "map.yaml",
        repo_root / "client" / "src" / "main" / "resources" / "worlds" / "main_world" / "map.yaml",
    ]

    print("Building map grid…")
    grid = build_grid()

    # Sanity checks
    spawn_x, spawn_y = 56, 96
    assert grid[spawn_y][spawn_x] == GRASS, f"Spawn tile ({spawn_x},{spawn_y}) is not grass!"
    assert grid[80][26] == WATER,            "Cove deep water (26,80) should be water"
    assert grid[82][32] == GRASS,            "Fishing spot (32,82) should be grass (walkable shore)"
    assert grid[84][34] == GRASS,            "Fishing spot (34,84) should be grass (walkable shore)"
    assert grid[106][46] == TREE,            "Tree tile (46,106) should be tree (non-walkable)"
    assert grid[84][63] == ROCK,             "Copper rock (63,84) should be rock (non-walkable)"
    assert grid[98][252] == GRASS,           "Mainland spawn (252,98) should be grass"
    # Building wall checks
    assert grid[84][66] == ROCK,             "Forge north wall (66,84) should be rock"
    assert grid[92][76] == ROCK,             "Forge east corner (76,92) should be rock"
    assert grid[88][71] == GRASS,            "Forge interior (71,88) should be grass"
    assert grid[88][66] == GRASS,            "Forge west door (66,88) should be grass (door gap)"
    assert grid[84][67] == ROCK,             "Forge north wall (67,84) should be rock"
    print("  All sanity checks passed.")

    for path in targets:
        write_map_yaml(str(path), grid)

    print("Done.")


if __name__ == "__main__":
    main()
