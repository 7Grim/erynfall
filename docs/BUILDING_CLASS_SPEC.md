# Building Class Spec

**Read this before placing or scaling any building shell in `scene.yaml`.**

This document defines the four canonical building classes for Erynfall.
Every `building_shell_*` placement must map to one of these classes.
Classes set expected scale bands, footprint ranges, wall heights, and use cases.

Enforcement: `scripts/validate-scene.py` — run before committing scene changes.

---

## Shell Model Dimensions (model space, before placement scale)

| Shell key                   | Half-width (X) | Half-depth (Z) | Wall height (Y) | Notes                     |
|-----------------------------|---------------|---------------|----------------|---------------------------|
| `building_shell_small`      | 2.10          | 1.90          | 1.50           | General-purpose rural hut |
| `building_shell_service`    | 2.50          | 2.10          | 1.55           | Workshop / forge interior  |
| `building_shell_coastal`    | 2.10          | 1.90          | 1.25           | Lower eave, seaside style  |

Source: `scripts/gen_osrs_buildings.py`; visual reference gizmo: F12 in artist mode.

---

## Class Definitions

### Class 1 — Tiny Hut

| Attribute           | Value                          |
|---------------------|-------------------------------|
| **Scale band**      | 1.40 – 1.65                   |
| **Shell(s)**        | `small`, `coastal`            |
| **Footprint**       | 5.9 – 6.9 tiles W × 5.3 – 6.3 tiles D |
| **Wall height**     | 1.75 – 2.48 WU                |
| **Interior clear**  | Tight — single-room only      |
| **Use cases**       | Fishing hut, survival camp, guardpost, small storage shed |

> **Coastal shell warning:** at scale 1.40, coastal wall height = 1.75 WU,
> below player height (1.80 WU). Minimum recommended scale for coastal = **1.50**.

---

### Class 2 — Small Service Building

| Attribute           | Value                          |
|---------------------|-------------------------------|
| **Scale band**      | 1.65 – 1.95                   |
| **Shell(s)**        | `small`, `service`            |
| **Footprint**       | 6.9 – 8.2 tiles W × 6.3 – 7.4 tiles D |
| **Wall height**     | 2.48 – 3.02 WU                |
| **Interior clear**  | One counter / workstation      |
| **Use cases**       | General store, small workshop, inn room, apothecary |

---

### Class 3 — Medium Hub Building

| Attribute           | Value                          |
|---------------------|-------------------------------|
| **Scale band**      | 2.00 – 2.55                   |
| **Shell(s)**        | `small`, `service`            |
| **Footprint**       | 8.4 – 10.7 tiles W × 7.6 – 9.7 tiles D |
| **Wall height**     | 3.00 – 3.96 WU                |
| **Interior clear**  | Multi-NPC (bank, quest hub, market) |
| **Use cases**       | Bank, Tutorial Guide hall, town hall, training school, saloon |

---

### Class 4 — Forge / Workshop

| Attribute           | Value                          |
|---------------------|-------------------------------|
| **Scale band**      | 1.80 – 2.25                   |
| **Shell(s)**        | `service` (preferred), `small` |
| **Footprint**       | 9.0 – 11.3 tiles W × 7.6 – 9.5 tiles D (service) |
| **Wall height**     | 2.79 – 3.49 WU                |
| **Interior clear**  | Anvil + furnace, large floor area |
| **Use cases**       | Smithy, forge, armorer, carpentry, alchemy lab |

---

## Scale Band Reference

| Scale | Class               | Width (small, tiles) | Wall (small, WU) | Wall (service, WU) |
|-------|---------------------|----------------------|------------------|--------------------|
| 1.40  | tiny_hut            | 5.88                 | 2.10             | 2.17               |
| 1.50  | tiny_hut            | 6.30                 | 2.25             | 2.33               |
| 1.65  | small_service       | 6.93                 | 2.48             | 2.56               |
| 1.80  | forge / small_svc   | 7.56                 | 2.70             | 2.79               |
| 2.00  | medium_hub / forge  | 8.40                 | 3.00             | 3.10               |
| 2.25  | medium_hub / forge  | 9.45                 | 3.38             | 3.49               |
| 2.50  | medium_hub          | 10.50                | 3.75             | 3.88               |

Player height = 1.80 WU.  All wall heights at scale ≥ 1.40 clear the player
for `small` and `service` variants.  For `coastal`, minimum safe scale is **1.50**.

---

## Placement Rules

1. **Scale within [1.40, 2.50].**  Below 1.40 the coastal variant's walls may
   not clear the player.  Above 2.50 the building dominates more than a single
   OSRS screen zone and loses the compact feel.

2. **Base + roof always paired.**  Every `_base` entry must have a matching
   `_roof` at the same `(x, y, scale, rotation)`.  `PropPlacementValidator`
   and `validate-scene.py` both flag unpaired shells.

3. **Footprint aligns to tile grid.**  At the intended scale the outer walls
   should land on whole-tile boundaries (or half-tile at most).

4. **One class per building.**  Pick the class whose scale band contains your
   intended scale.  If two classes overlap (e.g. 2.00–2.25), prefer the
   class that matches the shell variant — `service` → `forge_workshop`,
   `small` → `medium_hub`.

5. **Deviations need notes.**  If a building intentionally exceeds class
   dimensions (ruin, tower, landmark), add a `notes:` field to its manifest
   entry explaining why, to suppress `validate-scene.py` warnings.

---

## Validation

Run before any building placement commit:

```bash
python3 scripts/validate-scene.py
```

Codes:

| Code                         | Level    | Meaning                                              |
|------------------------------|----------|------------------------------------------------------|
| `SHELL_SCALE_BELOW_MIN`      | CRITICAL | Scale < 1.40 (walls may not clear player)            |
| `SHELL_SCALE_ABOVE_MAX`      | WARNING  | Scale > 2.50 (over-large for OSRS feel)              |
| `SHELL_WALL_TOO_SHORT`       | CRITICAL | Computed wall height < 1.80 WU (player taller)       |
| `SHELL_BASE_WITHOUT_ROOF`    | WARNING  | `_base` placed with no matching `_roof`              |
| `SHELL_ROOF_WITHOUT_BASE`    | WARNING  | `_roof` placed with no matching `_base`              |
| `SHELL_UNKNOWN_CLASS_SCALE`  | WARNING  | Scale outside all defined class bands (1.40–2.50)    |
