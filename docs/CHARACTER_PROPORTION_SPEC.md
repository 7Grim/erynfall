# Character Proportion Specification

**Read this before modelling any player or NPC character.**

This document defines the canonical proportions for all player and NPC character models in Erynfall. Proportions follow the OSRS low-poly visual style: slightly stocky, large head relative to body, exaggerated hands and boots for readability at small screen sizes.

All dimensions are in **world units (WU)**. 1 WU = 1 gameplay tile. All characters must stand upright with origin at ground level (Y = 0).

---

## Total Height

| Property | Value |
|---|---|
| Total height | **1.80 WU** |
| Origin | bottom-centre at Y = 0 |
| Top of head | Y = 1.80 WU |

This is enforced by `WorldScale.PLAYER_HEIGHT` and validated by `scripts/audit-assets.py`. Do not deviate more than ±0.05 WU from this target.

---

## Vertical Zones

The body is divided into four segments from ground up:

| Zone | Y range | Height |
|---|---|---|
| Shins / feet | 0.00 – 0.45 WU | 0.45 WU |
| Thighs | 0.45 – 0.90 WU | 0.45 WU |
| Torso | 0.90 – 1.44 WU | 0.54 WU |
| Head | 1.44 – 1.80 WU | 0.36 WU |

Zone boundary Y values (for use in Blender and as gizmo tick targets):

| Boundary | Y |
|---|---|
| Ground | 0.00 WU |
| Knee | 0.45 WU |
| Hip | 0.90 WU |
| Shoulder / neck | 1.44 WU |
| Top of head | 1.80 WU |

---

## Horizontal Widths

All widths are expressed as **total width** (not half-width) and measured at the widest point of that zone.

| Zone | Width | Notes |
|---|---|---|
| Shoulder (widest torso point) | **0.54 WU** | Slightly stocky; slightly more than half a tile |
| Hip | 0.44 WU | Narrower than shoulders |
| Head | 0.26 WU | Large relative to torso — OSRS cartoon proportion |
| Each arm tube | 0.08 WU | Measured across the arm cylinder |
| Each leg tube | 0.11 WU | Slightly wider than arm |

---

## Depth (Front-to-Back)

| Zone | Depth |
|---|---|
| Torso | 0.28 WU |
| Head | 0.22 WU |
| Arms | 0.08 WU |
| Legs | 0.11 WU |

---

## Exaggerations (OSRS Readability Rules)

These slight exaggerations are intentional and must be preserved:

| Feature | Exaggeration | Why |
|---|---|---|
| Boots / feet | +0.05 WU outward, +0.04 WU forward vs. shin tube | Silhouette readable at small sizes |
| Hands / knuckles | +0.02–0.04 WU wider at the knuckle vs. arm tube | Clickable affordance for equipment |
| Head | 1/5 of total height (0.36 WU) | OSRS standard — 5-head-tall proportions |

Do NOT scale these down to "realistic" proportions. Realistic proportions (7–8 heads tall) look wrong in the tile grid at OSRS camera distance.

---

## Arm Position

Arms hang from the shoulder (Y = 1.44) with the wrist at approximately Y = 0.72:

| Arm point | Y |
|---|---|
| Shoulder joint | 1.44 WU |
| Elbow | 1.08 WU |
| Wrist / hand | 0.72 WU |

Arms are positioned at X ≈ ±(shoulder_half_width + arm_half_width) = ±0.31 WU from centre.

---

## In-Engine Validation

The **character proportion reference gizmo** in MODEL_PREVIEW shows these zone boundaries as a wireframe overlay.

Toggle: **F4** (artist mode only)

What it draws:
- **White** outer silhouette box — shoulder_width × 1.80 height × torso_depth
- **Magenta** horizontal dividers at Y = 1.44 (shoulder), Y = 0.90 (hip), Y = 0.45 (knee)
- **Cyan** head zone box — head_width × 0.36 height, at Y 1.44–1.80

After pressing F4, orbit the camera to verify the model silhouette stays within (or intentionally fills) the reference box.

Runtime constants live in `Renderer3DExperimental.java` prefixed `CREF_`.

---

## Quick Reference Card

```
Y=1.80 ┬──────────────────────── top of head
       │        HEAD              W: 0.26, D: 0.22
Y=1.44 ┼════════════════════════ shoulder / neck
       │        TORSO             W: 0.54, D: 0.28
Y=0.90 ┼════════════════════════ hip
       │        THIGHS            W: 0.44, D: 0.11 each
Y=0.45 ┼════════════════════════ knee
       │        SHINS/FEET        boots: +0.05 outward
Y=0.00 ┴──────────────────────── ground (origin)

Arm: shoulder (1.44) → elbow (1.08) → wrist (0.72)
     X offset ≈ ±0.31 WU from centreline
```

---

## NPC Proportions

NPCs follow the same zone heights and material zones as the player. Variation between NPC types comes from:

- **Scale** — apply a uniform scale at the manifest level; do not reshape zone boundaries
- **Silhouette** — wider/taller body parts within a zone are fine; moving zone boundaries between NPC types is not
- **Head size** — may be slightly exaggerated further for named NPCs (quest givers, instructors); do not go below 1/5 of total height

Creatures (rats, chickens, cows) are exempt from humanoid zone rules but must still hit the 1.80 WU canonical height or declare an explicit scale in the manifest.

---

## Relation to Other Docs

| Doc | What it covers |
|---|---|
| `docs/SCALE_SPEC.md` | World unit definition, tile size, terrain heights |
| `docs/GRAPHICS_STYLE.md` | Poly budgets, colour palette, animation rhythm |
| `docs/VISUAL_PARITY_CHECKLIST.md` | Pass/fail review criteria |
| `art/models/manifest.yaml` | Per-asset scale, material_zones declaration |
