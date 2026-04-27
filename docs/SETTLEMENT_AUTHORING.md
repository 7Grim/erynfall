# Settlement Authoring Guide

Practical workflow for placing clutter (crates, barrels, rocks, fences, signs)
so towns and roads feel intentional rather than sparse or randomly dotted.

## Prop taxonomy

| Category  | Manifest key examples                                  | Use                              |
|-----------|--------------------------------------------------------|----------------------------------|
| Clutter   | `crate_small`, `barrel_small`, `sack_stack_small`      | Storage piles, dock cargo        |
| Furniture | `bench_small`, `table_small`                           | Market seating, interiors        |
| Navigation| `signpost_small`, `cart_small`                         | Wayfinding, market stalls        |
| Structure | `fence_post_small`                                     | Enclosures, separating zones     |
| Resource  | `rock_copper`, `rock_tin`, `tree_oak`, …               | Placed by design, not narrative  |

All keys must appear in `art/models/manifest.yaml` before placement.

---

## Density intent system

A `density_zones:` block in `scene.yaml` lets you declare _authoring intent_ for
each area. Zones are never loaded in production — they exist only to guide
placement and power the in-editor overlay.

### YAML format

```yaml
density_zones:

  - name: "dock_storage"
    min_x: 34
    min_y: 73
    max_x: 42
    max_y: 82
    density: "dense"           # sparse | medium | dense
    note: "crates, barrels, sacks unloaded at the pier"

  - name: "fishing_shore"
    min_x: 24
    min_y: 76
    max_x: 38
    max_y: 92
    density: "sparse"
    note: "keep open — just a few props on the beach"
```

**Density targets** (approximate, computed from area):

| Level  | Target props per tile | Border color |
|--------|-----------------------|--------------|
| sparse | 1 per 8 tiles         | Green        |
| medium | 1 per 4 tiles         | Yellow       |
| dense  | 1 per 2 tiles         | Orange       |

These are guidelines, not hard limits. Use them to calibrate visual weight.

---

## In-editor overlay

In WORLD_PLACEMENT mode, press **Z** to toggle the density overlay.

What you see:
- **Colored border rectangles** on the ground — one per zone, colored by density level
- **Inner fill lines** at density-keyed spacing — more lines = denser intent
- **Top-left HUD panel** — zone name, current prop count vs target, fill status color

```
[DENSITY ZONES]
S dock_storage:    5 / 8       ← sparse = S, medium = M, dense = D
M market_row:      3 / 12
D mining_scatter:  0 / 5
```

Fill color in the HUD: green = at or over target, yellow = halfway, orange = under.

The overlay reads live placement state — counts update immediately as you place
or delete props.

**Z** toggles the overlay on/off. No other workflow changes; saving still uses **P**.

---

## Placement workflow

### Phase 1 — Define zones

Add `density_zones:` entries to `scene.yaml` before placing any clutter.
Define zone bounds loosely — they do not constrain placement, only inform it.

### Phase 2 — Block in with WORLD_PLACEMENT

1. Launch artist mode: `./scripts/run-artist-client.sh --world-id=main_world`
2. Open workbench: **F6** → WORLD_PLACEMENT
3. Toggle density overlay: **Z**
4. Select prop: **[** / **]** to cycle keys
5. Click tiles to place; use **, .** to rotate, **-** / **=** to scale
6. Watch the HUD count — fill each zone to at least yellow before moving on

### Phase 3 — Vary and refine

- Use **D** to duplicate a selected placement as preview, then place rotated variants nearby
- Hold a consistent scale per prop type within a zone (e.g. all crates at 1.4×)
- Avoid uniform 90° rotation — rotate clutter 15–45° off-cardinal for lived-in feel
- `crate_small` + `barrel_small` pair well; scatter a `sack_stack_small` offset by 1 tile

### Phase 4 — Save

Press **P** (or **Ctrl+P** for dry-run) when the zone HUD shows green across all zones.

---

## Clutter clusters by zone type

### Dock / storage
Dense: 3–5 props in a 2×2–3×3 footprint.
Stack pattern: crate at center, 2× barrel flanking at ±1 tile, sack slightly offset.
```
    barrel_small  (x-1, y,   rot 45)
    crate_small   (x,   y,   rot 0)
    sack_stack    (x+1, y-1, rot 0)
    barrel_small  (x,   y+1, rot 90)
```

### Market row
Medium: spaced 3–5 tiles apart along a path. Signposts every 6–8 tiles.
Mix bench + signpost + crate. Avoid regular grid — offset every other prop by 1 tile.

### Mining site
Medium-dense near the rock, sparse at the perimeter.
`crate_small` at 1–2 tiles from ore nodes; `sack_stack_small` at 2–3 tiles.

### Settlement interior
Medium overall; denser near building entries.
Benches 1–2 tiles from doorways; signposts at path junctions; fences to frame enclosures.

### Shoreline / wilderness
Sparse. One cluster per 10–15 tiles of open ground.
Use scale ≤ 1.4× to keep props visually small against open terrain.

---

## Rules of thumb

1. **Cluster, don't scatter.** Three props at one location read better than one prop
   at three locations. Group first, then thin out.

2. **Vary rotation, not scale.** Keep scale consistent within a prop type per zone.
   Rotation variation (15–45° off-cardinal) does the visual work.

3. **Frame negative space.** Every dense cluster should have open tiles around it.
   A dock with no walkable gap feels like a maze.

4. **Anchor clutter to context.** Barrels near water/dock, crates near buildings,
   rocks near mining outcrops. Unexplained props feel abandoned, not lived-in.

5. **Fences define, not fill.** Fence posts belong at zone edges or enclosures.
   Scattering them in the middle reads as rubble, not construction.
