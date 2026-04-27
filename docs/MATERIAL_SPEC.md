# Material Spec

**Read this before authoring or reviewing 3D materials in any GLB asset.**

This document defines the approved material approach for Erynfall.
All GLB exports must comply with these rules before they can ship.

Enforcement: `scripts/audit-assets.py` — run before committing model changes.

---

## Approved Material Approach

Erynfall uses **flat/matte vertex-colored materials** — the closest real-time
equivalent to the original OSRS flat-shaded, palette-driven look.

### What to use

| Property              | Target value          | Why                                             |
|-----------------------|-----------------------|-------------------------------------------------|
| Color source          | Vertex colors (COLOR_0) | Avoids texture overhead; matches OSRS flat fill |
| `metallicFactor`      | 0.0                   | LibGDX has no PBR shader — metallic renders broken |
| `roughnessFactor`     | ≥ 0.7 (prefer 1.0)    | Matte look; no specular highlights              |
| `alphaMode`           | `OPAQUE`              | No transparency artifacts; OSRS world is opaque |
| `emissiveFactor`      | `[0, 0, 0]`           | No glow/bloom — flat ambient-only lighting      |
| Textures              | None (preferred)      | Use vertex colors; textures only if unavoidable |

### What to avoid

| Property              | Reason to avoid                                               |
|-----------------------|---------------------------------------------------------------|
| `metallicFactor` > 0  | Renders as flat grey or broken in LibGDX — no PBR support    |
| `roughnessFactor` < 0.4 | Creates specular highlights — modern/realistic look         |
| `alphaMode: BLEND`    | Draw-order artifacts; OSRS world is fully opaque             |
| `normalTexture`       | Not supported by renderer — silently ignored                  |
| `occlusionTexture`    | Not supported — wasted data                                   |
| `emissiveFactor` > 0  | Causes additive glow — breaks OSRS aesthetic                 |
| Textures > 256×256    | Wastes GPU memory; pixel art style requires small maps only  |

---

## Material Count Limits

Simple assets should use the minimum number of material slots.
Each material slot = one colour zone, not one object part.

| Category   | Max materials | Typical breakdown                                   |
|------------|---------------|-----------------------------------------------------|
| `actor`    | 4             | skin, hair/head, torso, legs (or combined zones)    |
| `equipment`| 3             | primary colour, secondary accent, metal/edge        |
| `prop`     | 2             | main body, accent detail (e.g. stone + wood trim)   |
| `shell`    | 4             | walls, roof, floor, trim                            |
| `resource` | 2             | main body, highlight/vein                           |

Exceeding these limits is a WARNING (`MATERIAL_COUNT_HIGH`).
Merge materials using vertex colors where possible — assign different
`COLOR_0` values per face rather than using separate material slots.

---

## Workflow

### In Blender

1. Set every material's Principled BSDF:
   - **Metallic = 0**
   - **Roughness ≥ 0.7** (or 1.0 for fully matte)
   - **Emission Color** = black, **Emission Strength** = 0
2. Use vertex colors (`Object Data → Color Attributes`) not image textures.
3. Set Material `Alpha` to 1.0 and blend mode to `Opaque`.
4. Do not add Normal Map or Ambient Occlusion texture nodes.

### In glTF export settings

- `Lighting Mode`: **Raw (Unlit)** or **Standard** — both acceptable
- `Image Format`: PNG (if any textures), max 256×256
- `Skinning`: **OFF** (rigid parenting only)
- `Compression`: OFF (GLB binary already compact)

---

## Validation Codes

| Code                    | Level    | Meaning                                              |
|-------------------------|----------|------------------------------------------------------|
| `MATERIAL_COUNT_HIGH`   | WARNING  | Material count exceeds category limit                |
| `MATERIAL_EMISSIVE`     | CRITICAL | Non-zero emissiveFactor (glow) detected              |
| `MATERIAL_TRANSPARENT`  | WARNING  | alphaMode BLEND on non-equipment asset               |
| `MATERIAL_LOW_ROUGHNESS`| WARNING  | roughnessFactor < 0.4 (shiny/specular)               |
| `PBR_METALLIC`          | CRITICAL | metallicFactor > 0 (LibGDX renders broken)           |
| `NORMAL_MAP`            | CRITICAL | normalTexture present (renderer ignores it)          |
| `OCCLUSION_MAP`         | WARNING  | occlusionTexture present (not supported)             |
| `TEXTURE_PRESENT`       | WARNING  | Texture/image embedded (prefer vertex colors)        |
