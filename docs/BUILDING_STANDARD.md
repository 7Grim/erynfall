# Building Standard — Canonical Small Building

**Read this before authoring or reviewing any building shell asset.**

This document defines the expected proportions for a single-story building in Erynfall. All new buildings are evaluated against these targets. The in-engine building reference gizmo (F12 in artist mode) renders these dimensions at world origin for direct comparison.

---

## Reference Model: `building_shell_small`

Source script: `scripts/gen_osrs_buildings.py`, variant `small`.

### Model-Space Dimensions (before placement scale)

| Dimension | Value (model units) | Notes |
|---|---|---|
| Half-width (X) | 2.10 | Full footprint = 4.20 model units |
| Half-depth (Z) | 1.90 | Full footprint = 3.80 model units |
| Wall height (Y) | 1.50 | Base of roof begins here |
| Roof overhang | +0.22 per side | Eave extends hw/hd + 0.22 |
| Roof peak (Y) | 2.60 | Ridge beam at this height |
| Ridge half-length | 1.10 | Hip roof geometry |

### Placement Scales Used in `main_world/scene.yaml`

| Scale | Width (tiles) | Depth (tiles) | Wall height (WU) | Use case |
|---|---|---|---|---|
| 1.4× | 5.88 | 5.32 | 2.10 | Tight / small NPC structure |
| 1.5× | 6.30 | 5.70 | 2.25 | Standard small building |
| 1.8× | 7.56 | 6.84 | 2.70 | Medium building |
| 2.0× | 8.40 | 7.60 | 3.00 | Large structure |
| 2.5× | 10.5 | 9.50 | 3.75 | Major landmark / saloon |

### Player Height Relationship

| | Value |
|---|---|
| Canonical player height | 1.80 WU |
| Wall height at scale 1.5× | 2.25 WU — 1.25× player height |
| Wall height at scale 2.5× | 3.75 WU — 2.08× player height |

Walls must always be visibly taller than the player. At scale 1.5× the wall is 25% taller than the player — the minimum acceptable. Scale below 1.4× is not recommended for occupied buildings.

---

## Proportion Checklist

Use this for every building shell asset before committing.

### Footprint

- [ ] Width and depth within ±10% of reference (hw 2.10, hd 1.90)
- [ ] Origin at tile-center bottom `(0, 0, 0)` in Blender before export
- [ ] Footprint symmetric on both X and Z axes (no asymmetric overhangs)
- [ ] At intended placement scale, building footprint aligns to tile grid (no half-tile offsets)

### Wall Height

- [ ] Wall height 1.40–1.60 model units (target 1.50)
- [ ] At minimum placement scale (≥1.4×), walls taller than player (≥ 1.80 WU in-world)
- [ ] No visible geometry below Y = 0 (floor at ground, no basement)
- [ ] Walls form a closed box — no open gaps at corners

### Door Opening

- [ ] Present on at least one face (south face preferred as default)
- [ ] Width: 0.40–0.55 model units (target ~0.45)
- [ ] Height: 0.80–1.00 model units (target 0.90)
- [ ] Bottom of door frame at Y = 0 (flush with ground)
- [ ] Door centered on its wall face (symmetric left/right)

### Roof

- [ ] Eave edge extends ≥ 0.15 model units beyond wall base on all sides
- [ ] Eave starts at or slightly below wall top (overlap = 0.05 model units)
- [ ] Ridge runs along X axis (hip roof style)
- [ ] No gap between base top and roof eave in the _base + _roof combined view
- [ ] Roof geometry in separate GLB file (`*_roof.glb`) — never in `*_base.glb`

### Visual / Materials

- [ ] Walls: warm stone/tan color (COL_STONE = `#C4AD8A`)
- [ ] Timber trim cap at wall top
- [ ] Roof: thatched brown (COL_ROOF_THATCH = `#7A5832`)
- [ ] Ridge beam: dark timber (COL_ROOF_TIMBER = `#3C260E`)
- [ ] No PBR maps — diffuse color only
- [ ] No textures > 256×256 (prefer vertex colors)
- [ ] Flat shading — no smooth normals

### Manifest / Export

- [ ] Base registered in `art/models/manifest.yaml` with `visibility_group: base`, `category: shell`
- [ ] Roof registered with `visibility_group: roof`, `category: shell`
- [ ] Both entries share the same placement position in `scene.yaml`
- [ ] `source_blend` field set in both manifest entries
- [ ] `PropPlacementValidator` runs clean (no warnings for this building) on next launch

---

## In-Engine Reference Gizmo (F12)

Press **F12** in artist mode (any workbench mode, 3D renderer active) to toggle the building reference gizmo.

The gizmo draws at world origin:

| Color | What |
|---|---|
| Cyan wire box | Expected shell bounding box: hw 2.10, hd 1.90, wall 1.50 model units |
| Green stick + tick | Player height reference: 1.80 WU — walls must clear this |
| White rectangle | Door template: south face, 0.45 wide × 0.90 tall, at Y = 0 |

Load a building shell model in `MODEL_PREVIEW` mode, enable F12, and compare the loaded mesh against the cyan box. The mesh should roughly match the box — significant over/under-size indicates a scale problem.

> **Note:** The gizmo is in model space. It does not account for placement scale. A shell at scale 2.5× will appear much larger than the gizmo in the game world — this is expected.

---

## Two-Part Shell Rule

Every building must have exactly two GLB entries placed at the same position with the same scale and rotation:

- `*_base.glb` — walls + floor + trim (`visibility_group: base`)
- `*_roof.glb` — roof geometry only (`visibility_group: roof`)

The roof is hidden when the player enters the building interior. The base is always visible. Merging them into a single file breaks interior visibility.

`PropPlacementValidator` logs a warning if a base exists without a matching roof or vice versa.

---

## Deviations from this Standard

If an asset intentionally deviates (e.g., a ruin with missing walls, a tower with no roof), add a `notes` field to its manifest entry explaining why:

```yaml
notes: "Ruin variant — intentionally missing south wall and roof."
```

This prevents the deviation from being flagged as an error during review.
