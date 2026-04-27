# Starter Area Visual Lock Checklist

**Read this before any art commit that affects the starter area (Tutorial Island).**

The "starter area" is defined as `x=20–95, y=62–130` in `main_world`.
This checklist must pass before the starter area can be considered visually shippable.

Automated check: `python3 scripts/check-starter-lock.py`

---

## Checklist

### Actor / NPC Quality

- [ ] No billboard actors in starter area — all NPCs at spawn positions within
  `x=20–95, y=62–130` must have a valid `model_key_3d` with a GLB on disk.
- [ ] No `ENTITY_MISSING_GLB` findings for any entity whose definition_id
  appears in `assets/data/npcs.yaml` spawns within the starter area.

### Asset Format

- [ ] No G3DJ assets in use by any prop, shell, or actor in the starter area.
  All GLB files referenced from `scene.yaml` or `entity_visuals.yaml` must
  be `.glb` format (not `.g3dj` or `.g3db`).

### Building Shells

- [ ] All building shells in `art/worlds/main_world/scene.yaml` within the
  starter area pass `validate-scene.py` — no `SHELL_SCALE_BELOW_MIN`,
  `SHELL_BASE_WITHOUT_ROOF`, or `SHELL_ROOF_WITHOUT_BASE` findings.

### Resources

- [ ] All resource props (trees, ore rocks, fishing spots) placed in the starter
  area have a valid `model_key_3d` in `entity_visuals.yaml` with a GLB on disk.
  No resource entity may fall back to billboard in the starter area.

### Player Base

- [ ] `player_base` has material zones defined — `applyPlayerMaterialZones()`
  must succeed without falling back to `WorldPalette` defaults for all zones:
  `skin`, `hair`, `shirt`, `pants`, `boots`, `gloves`.
- [ ] `player_base` passes `audit-assets.py` with no CRITICAL findings.

### Screenshot Set

- [ ] A screenshot set for `player_base` exists in `review/screenshots/actor/`.
  Four views required: `player_base_front.png`, `player_base_right.png`,
  `player_base_rear.png`, `player_base_iso.png`.
- [ ] A JSON sidecar `player_base.json` exists in `review/screenshots/actor/`.

### Visual Regression Scene

- [ ] `art/worlds/sandbox/scene.yaml` passes `validate-scene.py` with zero
  CRITICAL findings and zero `TERRAIN_MISSING_DEFAULT` warnings.
- [ ] Sandbox scene contains at least one entry for each prop category:
  tree, ore rock, building shell, crafting station.

---

## Definitions

| Term             | Value                                        |
|------------------|----------------------------------------------|
| Starter area     | `x=20–95, y=62–130` in `main_world`          |
| Billboard actor  | Entity with `model_key_3d` absent or GLB missing |
| G3DJ asset       | `format: g3dj` in `manifest.yaml`            |
| Material zones   | Player body zones: skin, hair, shirt, pants, boots, gloves |

---

## How to Run

```bash
# Full check — exits 1 on any failure
python3 scripts/check-starter-lock.py

# JSON output for CI
python3 scripts/check-starter-lock.py --json

# Non-blocking (always exit 0)
python3 scripts/check-starter-lock.py --exit-zero
```
