# Terrain Palette Spec

**Read this before authoring or reviewing terrain visual regions in `scene.yaml`.**

This document defines the canonical colour vocabulary for Erynfall terrain.
The spec locks five tile types to OSRS-inspired base colours and governs how
`WorldTheme` tints may shift them per region.

Enforcement: `scripts/validate-scene.py` — run before committing scene changes.

---

## Tile Type Vocabulary

Five tile types are defined.  Every `type:` value in `terrain_visual.regions`
and `tile_overrides` **must** be one of these identifiers (name or numeric):

| Integer | Name string(s)   | Description                          |
|---------|------------------|--------------------------------------|
| 0       | `grass`          | Natural ground — dominant island/mainland tile |
| 1       | `water`          | Sea, river, inlet — impassable       |
| 2       | `path` / `dirt`  | Worn walkway, dock apron, arena floor |
| 3       | `wall` / `rock`  | Cliff face, rocky outcrop, rubble    |
| 4       | `sand`           | Beach, desert edge, shore transition |

Any other string value is **invalid** and will be caught by `validate-scene.py`
(`TERRAIN_INVALID_TYPE`).

---

## Base Colour Reference

These are the canonical base RGB values — the colour of each tile type before
any `WorldTheme.terrain_tint` multiplier is applied.  All values are within
the `WorldPalette` constraint envelope (saturation ≤ 0.72, value 0.18–0.88).

Programmatic constants: `WorldPalette.TERRAIN_*`.

| Tile type | R     | G     | B     | Character                              |
|-----------|-------|-------|-------|----------------------------------------|
| grass     | 0.42  | 0.52  | 0.24  | Muted lowland green, warm yellow bias  |
| water     | 0.22  | 0.44  | 0.62  | Cold coastal blue, shallow-inlet tone  |
| path      | 0.58  | 0.48  | 0.28  | Worn dusty brown, well-trodden earth   |
| wall/rock | 0.46  | 0.44  | 0.38  | Cool grey stone, cliff or rubble face  |
| sand      | 0.76  | 0.66  | 0.38  | Warm pale beige, beach or desert edge  |

---

## WorldTheme Tint Rules

`WorldTheme.terrain_tint` is a per-channel multiplier `[R, G, B]` applied to
the base colour.  Default (no theme) = `[1.0, 1.0, 1.0]`.

**Acceptable tint range:** each component must be in **[0.30, 1.50]**.
Outside this range the result drifts too far from the base vocabulary and
will be flagged as `TERRAIN_THEME_EXTREME_TINT` by `validate-scene.py`.

### Defined Themes

| Theme ID                  | Tint R | Tint G | Tint B | Effect                                 |
|---------------------------|--------|--------|--------|----------------------------------------|
| `frontier_settlement`     | 1.05   | 0.92   | 0.80   | Warm, sun-baked; dusty paths, dry grass |
| `hell_scarred_badlands`   | 1.20   | 0.65   | 0.45   | Orange-red char; strips blue/green out  |

### Resulting Colour Examples (base × tint)

| Tile   | frontier_settlement       | hell_scarred_badlands       |
|--------|---------------------------|-----------------------------|
| grass  | (0.44, 0.48, 0.19) — dry  | (0.50, 0.34, 0.11) — charred |
| path   | (0.61, 0.44, 0.22) — dust | (0.70, 0.31, 0.13) — cinder |
| sand   | (0.80, 0.61, 0.30) — warm | (0.91, 0.43, 0.17) — scorched |
| water  | (0.23, 0.40, 0.50) — murky| (0.26, 0.29, 0.28) — dead pool |

All resulting values remain within the OSRS muted-palette range — no neon,
no blown-out whites, no near-black voids.

---

## Authoring Rules

1. **No invented types.**  Only the five vocabulary types may appear in
   `terrain_visual`.  Do not use `lava`, `ash`, `dirt`, etc. as type values —
   these are achieved via a `hell_scarred_badlands` theme on `path` or `wall`.

2. **Theme before tint.**  If a region needs a visual shift, apply a theme —
   do not duplicate regions with slightly different colours.  One tint per
   theme; one theme per region.

3. **Theme IDs declared before use.**  Every `theme:` reference on a region
   must resolve to an entry in the `themes:` block of the same `scene.yaml`.
   Undeclared theme references are caught by `TERRAIN_UNKNOWN_THEME_REF`.

4. **default_type required.**  Every `terrain_visual` block must declare
   `default_type:` so the background tile type is unambiguous.

5. **No region type `wall` for walkable floors.**  Tile type `wall` / `rock`
   is for impassable cliff faces and rocky terrain.  For arena floors or
   rough indoor surfaces, use `path` with an appropriate theme.

---

## Validation

Run before any terrain commit:

```bash
python3 scripts/validate-scene.py
python3 scripts/validate-scene.py --world-id sandbox
python3 scripts/validate-scene.py --json
```

Codes:

| Code                        | Level    | Meaning                                        |
|-----------------------------|----------|------------------------------------------------|
| `TERRAIN_INVALID_TYPE`      | CRITICAL | Unrecognized type string in region or override  |
| `TERRAIN_UNKNOWN_THEME_REF` | WARNING  | `theme:` value not declared in `themes:` block |
| `TERRAIN_MISSING_DEFAULT`   | WARNING  | `terrain_visual` has no `default_type`          |
| `TERRAIN_THEME_MISSING_TINT`| WARNING  | Theme declared without `terrain_tint`          |
| `TERRAIN_THEME_EXTREME_TINT`| WARNING  | Tint component outside [0.30, 1.50]            |
