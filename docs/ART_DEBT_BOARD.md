# Art Debt Board

_Generated 2026-04-27 16:05 UTC by `scripts/gen-art-debt.py`._
_Do not edit by hand — re-run the script to refresh._

## Summary

| Source | Findings |
|--------|----------|
| audit-assets.py | 674 |
| validate-scene.py (main_world) | 1 |
| validate-scene.py (sandbox) | 1 |
| report-entity-visuals.py | 17 |
| **Total** | **693** |

> 🔴 44 critical issue(s) — fix before next screenshot.

---

## 🔴 Must fix before next screenshot  (44)

| Key | Code | Detail | Source |
|-----|------|--------|--------|
| `npc_banker_base` | `ACTOR_HEIGHT_WRONG` | Actor height = 1.280 WU (y_min=-0.280, y_max=1.000). Expected 1.8 WU (hard range 1.45–2.1 WU). Normalise height in Blender: scale mesh to... | audit-assets |
| `npc_banker_idle` | `ACTOR_HEIGHT_WRONG` | Actor height = 2.300 WU (y_min=-1.000, y_max=1.300). Expected 1.8 WU (hard range 1.45–2.1 WU). Normalise height in Blender: scale mesh to... | audit-assets |
| `npc_banker_walk` | `ACTOR_HEIGHT_WRONG` | Actor height = 2.300 WU (y_min=-1.000, y_max=1.300). Expected 1.8 WU (hard range 1.45–2.1 WU). Normalise height in Blender: scale mesh to... | audit-assets |
| `npc_goblin_action` | `ACTOR_HEIGHT_WRONG` | Actor height = 2.300 WU (y_min=-1.000, y_max=1.300). Expected 1.8 WU (hard range 1.45–2.1 WU). Normalise height in Blender: scale mesh to... | audit-assets |
| `npc_goblin_base` | `ACTOR_HEIGHT_WRONG` | Actor height = 1.280 WU (y_min=-0.280, y_max=1.000). Expected 1.8 WU (hard range 1.45–2.1 WU). Normalise height in Blender: scale mesh to... | audit-assets |
| `npc_goblin_idle` | `ACTOR_HEIGHT_WRONG` | Actor height = 2.300 WU (y_min=-1.000, y_max=1.300). Expected 1.8 WU (hard range 1.45–2.1 WU). Normalise height in Blender: scale mesh to... | audit-assets |
| `npc_goblin_walk` | `ACTOR_HEIGHT_WRONG` | Actor height = 2.300 WU (y_min=-1.000, y_max=1.300). Expected 1.8 WU (hard range 1.45–2.1 WU). Normalise height in Blender: scale mesh to... | audit-assets |
| `npc_guide_base` | `ACTOR_HEIGHT_WRONG` | Actor height = 1.280 WU (y_min=-0.280, y_max=1.000). Expected 1.8 WU (hard range 1.45–2.1 WU). Normalise height in Blender: scale mesh to... | audit-assets |
| `npc_guide_idle` | `ACTOR_HEIGHT_WRONG` | Actor height = 2.240 WU (y_min=-1.000, y_max=1.240). Expected 1.8 WU (hard range 1.45–2.1 WU). Normalise height in Blender: scale mesh to... | audit-assets |
| `npc_guide_walk` | `ACTOR_HEIGHT_WRONG` | Actor height = 2.240 WU (y_min=-1.000, y_max=1.240). Expected 1.8 WU (hard range 1.45–2.1 WU). Normalise height in Blender: scale mesh to... | audit-assets |
| `npc_instructor_base` | `ACTOR_HEIGHT_WRONG` | Actor height = 1.280 WU (y_min=-0.280, y_max=1.000). Expected 1.8 WU (hard range 1.45–2.1 WU). Normalise height in Blender: scale mesh to... | audit-assets |
| `npc_instructor_idle` | `ACTOR_HEIGHT_WRONG` | Actor height = 2.240 WU (y_min=-1.000, y_max=1.240). Expected 1.8 WU (hard range 1.45–2.1 WU). Normalise height in Blender: scale mesh to... | audit-assets |
| `npc_instructor_walk` | `ACTOR_HEIGHT_WRONG` | Actor height = 2.240 WU (y_min=-1.000, y_max=1.240). Expected 1.8 WU (hard range 1.45–2.1 WU). Normalise height in Blender: scale mesh to... | audit-assets |
| `player_chop` | `ACTOR_HEIGHT_WRONG` | Actor height = 2.300 WU (y_min=-1.000, y_max=1.300). Expected 1.8 WU (hard range 1.45–2.1 WU). Normalise height in Blender: scale mesh to... | audit-assets |
| `player_fish` | `ACTOR_HEIGHT_WRONG` | Actor height = 2.300 WU (y_min=-1.000, y_max=1.300). Expected 1.8 WU (hard range 1.45–2.1 WU). Normalise height in Blender: scale mesh to... | audit-assets |
| `player_idle` | `ACTOR_HEIGHT_WRONG` | Actor height = 2.300 WU (y_min=-1.000, y_max=1.300). Expected 1.8 WU (hard range 1.45–2.1 WU). Normalise height in Blender: scale mesh to... | audit-assets |
| `player_mine` | `ACTOR_HEIGHT_WRONG` | Actor height = 2.300 WU (y_min=-1.000, y_max=1.300). Expected 1.8 WU (hard range 1.45–2.1 WU). Normalise height in Blender: scale mesh to... | audit-assets |
| `player_pickup` | `ACTOR_HEIGHT_WRONG` | Actor height = 2.300 WU (y_min=-1.000, y_max=1.300). Expected 1.8 WU (hard range 1.45–2.1 WU). Normalise height in Blender: scale mesh to... | audit-assets |
| `player_spear` | `ACTOR_HEIGHT_WRONG` | Actor height = 2.300 WU (y_min=-1.000, y_max=1.300). Expected 1.8 WU (hard range 1.45–2.1 WU). Normalise height in Blender: scale mesh to... | audit-assets |
| `player_sword` | `ACTOR_HEIGHT_WRONG` | Actor height = 2.300 WU (y_min=-1.000, y_max=1.300). Expected 1.8 WU (hard range 1.45–2.1 WU). Normalise height in Blender: scale mesh to... | audit-assets |
| `player_walk` | `ACTOR_HEIGHT_WRONG` | Actor height = 2.300 WU (y_min=-1.000, y_max=1.300). Expected 1.8 WU (hard range 1.45–2.1 WU). Normalise height in Blender: scale mesh to... | audit-assets |
| `entity:1:Tutorial Guide` | `ENTITY_MISSING_GLB` | model_key_3d 'npc_guide' not in manifest and GLB not on disk | report-entity-visuals |
| `entity:2:Combat Instructor` | `ENTITY_MISSING_GLB` | model_key_3d 'npc_instructor' not in manifest and GLB not on disk | report-entity-visuals |
| `entity:3:Rat` | `ENTITY_MISSING_GLB` | model_key_3d 'npc_rat' not in manifest and GLB not on disk | report-entity-visuals |
| `entity:40:Banker` | `ENTITY_MISSING_GLB` | model_key_3d 'npc_banker' not in manifest and GLB not on disk | report-entity-visuals |
| `entity:4:Rat` | `ENTITY_MISSING_GLB` | model_key_3d 'npc_rat' not in manifest and GLB not on disk | report-entity-visuals |
| `entity:50:Survival Expert` | `ENTITY_MISSING_GLB` | model_key_3d 'npc_guide' not in manifest and GLB not on disk | report-entity-visuals |
| `entity:51:Mining Instructor` | `ENTITY_MISSING_GLB` | model_key_3d 'npc_guide' not in manifest and GLB not on disk | report-entity-visuals |
| `entity:52:Quest Guide` | `ENTITY_MISSING_GLB` | model_key_3d 'npc_guide' not in manifest and GLB not on disk | report-entity-visuals |
| `entity:53:Chicken` | `ENTITY_MISSING_GLB` | model_key_3d 'npc_chicken' not in manifest and GLB not on disk | report-entity-visuals |
| `entity:5:Rat` | `ENTITY_MISSING_GLB` | model_key_3d 'npc_rat' not in manifest and GLB not on disk | report-entity-visuals |
| `entity:60:Giant Rat` | `ENTITY_MISSING_GLB` | model_key_3d 'npc_giant_rat' not in manifest and GLB not on disk | report-entity-visuals |
| `entity:62:Goblin` | `ENTITY_MISSING_GLB` | model_key_3d 'npc_goblin' not in manifest and GLB not on disk | report-entity-visuals |
| `entity:63:Goblin Warchief` | `ENTITY_MISSING_GLB` | model_key_3d 'npc_goblin' not in manifest and GLB not on disk | report-entity-visuals |
| `entity:70:Cow` | `ENTITY_MISSING_GLB` | model_key_3d 'npc_cow' not in manifest and GLB not on disk | report-entity-visuals |
| `player_base` | `GLB_NON_CANONICAL_CLIPS` | GLB contains non-canonical animation names: 'Armature|mixamo.com|Layer0', 'attack_shoot_bow'. These clips will not be played by the runti... | audit-assets |
| `anvil` | `PBR_METALLIC` | One or more materials use metallicFactor > 0. LibGDX has no PBR shader — metallic shading renders as flat or broken. Set Metallic = 0 on ... | audit-assets |
| `equip_body_bronze_platebody` | `PBR_METALLIC` | One or more materials use metallicFactor > 0. LibGDX has no PBR shader — metallic shading renders as flat or broken. Set Metallic = 0 on ... | audit-assets |
| `equip_head_bronze_full_helm` | `PBR_METALLIC` | One or more materials use metallicFactor > 0. LibGDX has no PBR shader — metallic shading renders as flat or broken. Set Metallic = 0 on ... | audit-assets |
| `equip_legs_bronze_platelegs` | `PBR_METALLIC` | One or more materials use metallicFactor > 0. LibGDX has no PBR shader — metallic shading renders as flat or broken. Set Metallic = 0 on ... | audit-assets |
| `equip_shield_bronze_sq` | `PBR_METALLIC` | One or more materials use metallicFactor > 0. LibGDX has no PBR shader — metallic shading renders as flat or broken. Set Metallic = 0 on ... | audit-assets |
| `equip_weapon_bronze_sword` | `PBR_METALLIC` | One or more materials use metallicFactor > 0. LibGDX has no PBR shader — metallic shading renders as flat or broken. Set Metallic = 0 on ... | audit-assets |
| `equip_weapon_dragon_axe` | `PBR_METALLIC` | One or more materials use metallicFactor > 0. LibGDX has no PBR shader — metallic shading renders as flat or broken. Set Metallic = 0 on ... | audit-assets |
| `player_base` | `SKINNING_PRESENT` | GLB contains 1 skin(s). OSRS-style animation requires rigid bone-parented mesh objects, NOT vertex-weight skinning. In Blender: remove th... | audit-assets |

## 🟡 Must fix before public alpha  (454)

| Key | Code | Detail | Source |
|-----|------|--------|--------|
| `npc_chicken_action` | `ACTOR_HEIGHT_OFF_TARGET` | Actor height = 2.000 WU (target 1.8 ± 0.08 WU). Minor deviation — verify against player_base reference in MODEL_PREVIEW (F7 overlay). | audit-assets |
| `npc_chicken_base` | `ACTOR_HEIGHT_OFF_TARGET` | Actor height = 2.000 WU (target 1.8 ± 0.08 WU). Minor deviation — verify against player_base reference in MODEL_PREVIEW (F7 overlay). | audit-assets |
| `npc_chicken_idle` | `ACTOR_HEIGHT_OFF_TARGET` | Actor height = 2.000 WU (target 1.8 ± 0.08 WU). Minor deviation — verify against player_base reference in MODEL_PREVIEW (F7 overlay). | audit-assets |
| `npc_chicken_walk` | `ACTOR_HEIGHT_OFF_TARGET` | Actor height = 2.000 WU (target 1.8 ± 0.08 WU). Minor deviation — verify against player_base reference in MODEL_PREVIEW (F7 overlay). | audit-assets |
| `npc_cow_action` | `ACTOR_HEIGHT_OFF_TARGET` | Actor height = 2.000 WU (target 1.8 ± 0.08 WU). Minor deviation — verify against player_base reference in MODEL_PREVIEW (F7 overlay). | audit-assets |
| `npc_cow_base` | `ACTOR_HEIGHT_OFF_TARGET` | Actor height = 2.000 WU (target 1.8 ± 0.08 WU). Minor deviation — verify against player_base reference in MODEL_PREVIEW (F7 overlay). | audit-assets |
| `npc_cow_idle` | `ACTOR_HEIGHT_OFF_TARGET` | Actor height = 2.000 WU (target 1.8 ± 0.08 WU). Minor deviation — verify against player_base reference in MODEL_PREVIEW (F7 overlay). | audit-assets |
| `npc_cow_walk` | `ACTOR_HEIGHT_OFF_TARGET` | Actor height = 2.000 WU (target 1.8 ± 0.08 WU). Minor deviation — verify against player_base reference in MODEL_PREVIEW (F7 overlay). | audit-assets |
| `npc_giant_rat_action` | `ACTOR_HEIGHT_OFF_TARGET` | Actor height = 2.000 WU (target 1.8 ± 0.08 WU). Minor deviation — verify against player_base reference in MODEL_PREVIEW (F7 overlay). | audit-assets |
| `npc_giant_rat_base` | `ACTOR_HEIGHT_OFF_TARGET` | Actor height = 2.000 WU (target 1.8 ± 0.08 WU). Minor deviation — verify against player_base reference in MODEL_PREVIEW (F7 overlay). | audit-assets |
| `npc_giant_rat_idle` | `ACTOR_HEIGHT_OFF_TARGET` | Actor height = 2.000 WU (target 1.8 ± 0.08 WU). Minor deviation — verify against player_base reference in MODEL_PREVIEW (F7 overlay). | audit-assets |
| `npc_giant_rat_walk` | `ACTOR_HEIGHT_OFF_TARGET` | Actor height = 2.000 WU (target 1.8 ± 0.08 WU). Minor deviation — verify against player_base reference in MODEL_PREVIEW (F7 overlay). | audit-assets |
| `npc_rat_action` | `ACTOR_HEIGHT_OFF_TARGET` | Actor height = 2.000 WU (target 1.8 ± 0.08 WU). Minor deviation — verify against player_base reference in MODEL_PREVIEW (F7 overlay). | audit-assets |
| `npc_rat_base` | `ACTOR_HEIGHT_OFF_TARGET` | Actor height = 2.000 WU (target 1.8 ± 0.08 WU). Minor deviation — verify against player_base reference in MODEL_PREVIEW (F7 overlay). | audit-assets |
| `npc_rat_idle` | `ACTOR_HEIGHT_OFF_TARGET` | Actor height = 2.000 WU (target 1.8 ± 0.08 WU). Minor deviation — verify against player_base reference in MODEL_PREVIEW (F7 overlay). | audit-assets |
| `npc_rat_walk` | `ACTOR_HEIGHT_OFF_TARGET` | Actor height = 2.000 WU (target 1.8 ± 0.08 WU). Minor deviation — verify against player_base reference in MODEL_PREVIEW (F7 overlay). | audit-assets |
| `player_base` | `ACTOR_HEIGHT_OFF_TARGET` | Actor height = 1.959 WU (target 1.8 ± 0.08 WU). Minor deviation — verify against player_base reference in MODEL_PREVIEW (F7 overlay). | audit-assets |
| `npc_banker_base` | `ACTOR_NOT_ANIMATED` | Actor GLB has animated: false (or field absent). If this model contains animation clips, set animated: true or the clips will be ignored. | audit-assets |
| `npc_banker_idle` | `ACTOR_NOT_ANIMATED` | Actor GLB has animated: false (or field absent). If this model contains animation clips, set animated: true or the clips will be ignored. | audit-assets |
| `npc_banker_walk` | `ACTOR_NOT_ANIMATED` | Actor GLB has animated: false (or field absent). If this model contains animation clips, set animated: true or the clips will be ignored. | audit-assets |
| `npc_chicken_action` | `ACTOR_NOT_ANIMATED` | Actor GLB has animated: false (or field absent). If this model contains animation clips, set animated: true or the clips will be ignored. | audit-assets |
| `npc_chicken_idle` | `ACTOR_NOT_ANIMATED` | Actor GLB has animated: false (or field absent). If this model contains animation clips, set animated: true or the clips will be ignored. | audit-assets |
| `npc_chicken_walk` | `ACTOR_NOT_ANIMATED` | Actor GLB has animated: false (or field absent). If this model contains animation clips, set animated: true or the clips will be ignored. | audit-assets |
| `npc_cow_action` | `ACTOR_NOT_ANIMATED` | Actor GLB has animated: false (or field absent). If this model contains animation clips, set animated: true or the clips will be ignored. | audit-assets |
| `npc_cow_idle` | `ACTOR_NOT_ANIMATED` | Actor GLB has animated: false (or field absent). If this model contains animation clips, set animated: true or the clips will be ignored. | audit-assets |
| `npc_cow_walk` | `ACTOR_NOT_ANIMATED` | Actor GLB has animated: false (or field absent). If this model contains animation clips, set animated: true or the clips will be ignored. | audit-assets |
| `npc_giant_rat_action` | `ACTOR_NOT_ANIMATED` | Actor GLB has animated: false (or field absent). If this model contains animation clips, set animated: true or the clips will be ignored. | audit-assets |
| `npc_giant_rat_idle` | `ACTOR_NOT_ANIMATED` | Actor GLB has animated: false (or field absent). If this model contains animation clips, set animated: true or the clips will be ignored. | audit-assets |
| `npc_giant_rat_walk` | `ACTOR_NOT_ANIMATED` | Actor GLB has animated: false (or field absent). If this model contains animation clips, set animated: true or the clips will be ignored. | audit-assets |
| `npc_goblin_action` | `ACTOR_NOT_ANIMATED` | Actor GLB has animated: false (or field absent). If this model contains animation clips, set animated: true or the clips will be ignored. | audit-assets |
| `npc_goblin_base` | `ACTOR_NOT_ANIMATED` | Actor GLB has animated: false (or field absent). If this model contains animation clips, set animated: true or the clips will be ignored. | audit-assets |
| `npc_goblin_idle` | `ACTOR_NOT_ANIMATED` | Actor GLB has animated: false (or field absent). If this model contains animation clips, set animated: true or the clips will be ignored. | audit-assets |
| `npc_goblin_walk` | `ACTOR_NOT_ANIMATED` | Actor GLB has animated: false (or field absent). If this model contains animation clips, set animated: true or the clips will be ignored. | audit-assets |
| `npc_guide_base` | `ACTOR_NOT_ANIMATED` | Actor GLB has animated: false (or field absent). If this model contains animation clips, set animated: true or the clips will be ignored. | audit-assets |
| `npc_guide_idle` | `ACTOR_NOT_ANIMATED` | Actor GLB has animated: false (or field absent). If this model contains animation clips, set animated: true or the clips will be ignored. | audit-assets |
| `npc_guide_walk` | `ACTOR_NOT_ANIMATED` | Actor GLB has animated: false (or field absent). If this model contains animation clips, set animated: true or the clips will be ignored. | audit-assets |
| `npc_instructor_base` | `ACTOR_NOT_ANIMATED` | Actor GLB has animated: false (or field absent). If this model contains animation clips, set animated: true or the clips will be ignored. | audit-assets |
| `npc_instructor_idle` | `ACTOR_NOT_ANIMATED` | Actor GLB has animated: false (or field absent). If this model contains animation clips, set animated: true or the clips will be ignored. | audit-assets |
| `npc_instructor_walk` | `ACTOR_NOT_ANIMATED` | Actor GLB has animated: false (or field absent). If this model contains animation clips, set animated: true or the clips will be ignored. | audit-assets |
| `npc_rat_action` | `ACTOR_NOT_ANIMATED` | Actor GLB has animated: false (or field absent). If this model contains animation clips, set animated: true or the clips will be ignored. | audit-assets |
| `npc_rat_idle` | `ACTOR_NOT_ANIMATED` | Actor GLB has animated: false (or field absent). If this model contains animation clips, set animated: true or the clips will be ignored. | audit-assets |
| `npc_rat_walk` | `ACTOR_NOT_ANIMATED` | Actor GLB has animated: false (or field absent). If this model contains animation clips, set animated: true or the clips will be ignored. | audit-assets |
| `player_chop` | `ACTOR_NOT_ANIMATED` | Actor GLB has animated: false (or field absent). If this model contains animation clips, set animated: true or the clips will be ignored. | audit-assets |
| `player_fish` | `ACTOR_NOT_ANIMATED` | Actor GLB has animated: false (or field absent). If this model contains animation clips, set animated: true or the clips will be ignored. | audit-assets |
| `player_idle` | `ACTOR_NOT_ANIMATED` | Actor GLB has animated: false (or field absent). If this model contains animation clips, set animated: true or the clips will be ignored. | audit-assets |
| `player_mine` | `ACTOR_NOT_ANIMATED` | Actor GLB has animated: false (or field absent). If this model contains animation clips, set animated: true or the clips will be ignored. | audit-assets |
| `player_pickup` | `ACTOR_NOT_ANIMATED` | Actor GLB has animated: false (or field absent). If this model contains animation clips, set animated: true or the clips will be ignored. | audit-assets |
| `player_spear` | `ACTOR_NOT_ANIMATED` | Actor GLB has animated: false (or field absent). If this model contains animation clips, set animated: true or the clips will be ignored. | audit-assets |
| `player_sword` | `ACTOR_NOT_ANIMATED` | Actor GLB has animated: false (or field absent). If this model contains animation clips, set animated: true or the clips will be ignored. | audit-assets |
| `player_walk` | `ACTOR_NOT_ANIMATED` | Actor GLB has animated: false (or field absent). If this model contains animation clips, set animated: true or the clips will be ignored. | audit-assets |
| `npc_banker_base` | `ACTOR_NO_CLIP_LIST` | No animation_clips list declared in manifest. Add the list so clip names can be validated. Player: idle walk run pickup chop mine fish sm... | audit-assets |
| `npc_banker_idle` | `ACTOR_NO_CLIP_LIST` | No animation_clips list declared in manifest. Add the list so clip names can be validated. Player: idle walk run pickup chop mine fish sm... | audit-assets |
| `npc_banker_walk` | `ACTOR_NO_CLIP_LIST` | No animation_clips list declared in manifest. Add the list so clip names can be validated. Player: idle walk run pickup chop mine fish sm... | audit-assets |
| `npc_chicken_action` | `ACTOR_NO_CLIP_LIST` | No animation_clips list declared in manifest. Add the list so clip names can be validated. Player: idle walk run pickup chop mine fish sm... | audit-assets |
| `npc_chicken_idle` | `ACTOR_NO_CLIP_LIST` | No animation_clips list declared in manifest. Add the list so clip names can be validated. Player: idle walk run pickup chop mine fish sm... | audit-assets |
| `npc_chicken_walk` | `ACTOR_NO_CLIP_LIST` | No animation_clips list declared in manifest. Add the list so clip names can be validated. Player: idle walk run pickup chop mine fish sm... | audit-assets |
| `npc_cow_action` | `ACTOR_NO_CLIP_LIST` | No animation_clips list declared in manifest. Add the list so clip names can be validated. Player: idle walk run pickup chop mine fish sm... | audit-assets |
| `npc_cow_idle` | `ACTOR_NO_CLIP_LIST` | No animation_clips list declared in manifest. Add the list so clip names can be validated. Player: idle walk run pickup chop mine fish sm... | audit-assets |
| `npc_cow_walk` | `ACTOR_NO_CLIP_LIST` | No animation_clips list declared in manifest. Add the list so clip names can be validated. Player: idle walk run pickup chop mine fish sm... | audit-assets |
| `npc_giant_rat_action` | `ACTOR_NO_CLIP_LIST` | No animation_clips list declared in manifest. Add the list so clip names can be validated. Player: idle walk run pickup chop mine fish sm... | audit-assets |
| `npc_giant_rat_idle` | `ACTOR_NO_CLIP_LIST` | No animation_clips list declared in manifest. Add the list so clip names can be validated. Player: idle walk run pickup chop mine fish sm... | audit-assets |
| `npc_giant_rat_walk` | `ACTOR_NO_CLIP_LIST` | No animation_clips list declared in manifest. Add the list so clip names can be validated. Player: idle walk run pickup chop mine fish sm... | audit-assets |
| `npc_goblin_action` | `ACTOR_NO_CLIP_LIST` | No animation_clips list declared in manifest. Add the list so clip names can be validated. Player: idle walk run pickup chop mine fish sm... | audit-assets |
| `npc_goblin_base` | `ACTOR_NO_CLIP_LIST` | No animation_clips list declared in manifest. Add the list so clip names can be validated. Player: idle walk run pickup chop mine fish sm... | audit-assets |
| `npc_goblin_idle` | `ACTOR_NO_CLIP_LIST` | No animation_clips list declared in manifest. Add the list so clip names can be validated. Player: idle walk run pickup chop mine fish sm... | audit-assets |
| `npc_goblin_walk` | `ACTOR_NO_CLIP_LIST` | No animation_clips list declared in manifest. Add the list so clip names can be validated. Player: idle walk run pickup chop mine fish sm... | audit-assets |
| `npc_guide_base` | `ACTOR_NO_CLIP_LIST` | No animation_clips list declared in manifest. Add the list so clip names can be validated. Player: idle walk run pickup chop mine fish sm... | audit-assets |
| `npc_guide_idle` | `ACTOR_NO_CLIP_LIST` | No animation_clips list declared in manifest. Add the list so clip names can be validated. Player: idle walk run pickup chop mine fish sm... | audit-assets |
| `npc_guide_walk` | `ACTOR_NO_CLIP_LIST` | No animation_clips list declared in manifest. Add the list so clip names can be validated. Player: idle walk run pickup chop mine fish sm... | audit-assets |
| `npc_instructor_base` | `ACTOR_NO_CLIP_LIST` | No animation_clips list declared in manifest. Add the list so clip names can be validated. Player: idle walk run pickup chop mine fish sm... | audit-assets |
| `npc_instructor_idle` | `ACTOR_NO_CLIP_LIST` | No animation_clips list declared in manifest. Add the list so clip names can be validated. Player: idle walk run pickup chop mine fish sm... | audit-assets |
| `npc_instructor_walk` | `ACTOR_NO_CLIP_LIST` | No animation_clips list declared in manifest. Add the list so clip names can be validated. Player: idle walk run pickup chop mine fish sm... | audit-assets |
| `npc_rat_action` | `ACTOR_NO_CLIP_LIST` | No animation_clips list declared in manifest. Add the list so clip names can be validated. Player: idle walk run pickup chop mine fish sm... | audit-assets |
| `npc_rat_idle` | `ACTOR_NO_CLIP_LIST` | No animation_clips list declared in manifest. Add the list so clip names can be validated. Player: idle walk run pickup chop mine fish sm... | audit-assets |
| `npc_rat_walk` | `ACTOR_NO_CLIP_LIST` | No animation_clips list declared in manifest. Add the list so clip names can be validated. Player: idle walk run pickup chop mine fish sm... | audit-assets |
| `player_chop` | `ACTOR_NO_CLIP_LIST` | No animation_clips list declared in manifest. Add the list so clip names can be validated. Player: idle walk run pickup chop mine fish sm... | audit-assets |
| `player_fish` | `ACTOR_NO_CLIP_LIST` | No animation_clips list declared in manifest. Add the list so clip names can be validated. Player: idle walk run pickup chop mine fish sm... | audit-assets |
| `player_idle` | `ACTOR_NO_CLIP_LIST` | No animation_clips list declared in manifest. Add the list so clip names can be validated. Player: idle walk run pickup chop mine fish sm... | audit-assets |
| `player_mine` | `ACTOR_NO_CLIP_LIST` | No animation_clips list declared in manifest. Add the list so clip names can be validated. Player: idle walk run pickup chop mine fish sm... | audit-assets |
| `player_pickup` | `ACTOR_NO_CLIP_LIST` | No animation_clips list declared in manifest. Add the list so clip names can be validated. Player: idle walk run pickup chop mine fish sm... | audit-assets |
| `player_spear` | `ACTOR_NO_CLIP_LIST` | No animation_clips list declared in manifest. Add the list so clip names can be validated. Player: idle walk run pickup chop mine fish sm... | audit-assets |
| `player_sword` | `ACTOR_NO_CLIP_LIST` | No animation_clips list declared in manifest. Add the list so clip names can be validated. Player: idle walk run pickup chop mine fish sm... | audit-assets |
| `player_walk` | `ACTOR_NO_CLIP_LIST` | No animation_clips list declared in manifest. Add the list so clip names can be validated. Player: idle walk run pickup chop mine fish sm... | audit-assets |
| `npc_banker_base` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -0.280 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `npc_banker_idle` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -1.000 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `npc_banker_walk` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -1.000 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `npc_chicken_action` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -1.000 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `npc_chicken_base` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -1.000 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `npc_chicken_idle` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -1.000 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `npc_chicken_walk` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -1.000 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `npc_cow_action` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -1.000 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `npc_cow_base` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -1.000 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `npc_cow_idle` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -1.000 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `npc_cow_walk` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -1.000 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `npc_giant_rat_action` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -1.000 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `npc_giant_rat_base` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -1.000 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `npc_giant_rat_idle` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -1.000 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `npc_giant_rat_walk` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -1.000 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `npc_goblin_action` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -1.000 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `npc_goblin_base` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -0.280 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `npc_goblin_idle` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -1.000 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `npc_goblin_walk` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -1.000 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `npc_guide_base` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -0.280 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `npc_guide_idle` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -1.000 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `npc_guide_walk` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -1.000 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `npc_instructor_base` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -0.280 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `npc_instructor_idle` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -1.000 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `npc_instructor_walk` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -1.000 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `npc_rat_action` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -1.000 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `npc_rat_base` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -1.000 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `npc_rat_idle` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -1.000 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `npc_rat_walk` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -1.000 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `player_base` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -0.998 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `player_chop` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -1.000 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `player_fish` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -1.000 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `player_idle` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -1.000 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `player_mine` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -1.000 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `player_pickup` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -1.000 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `player_spear` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -1.000 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `player_sword` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -1.000 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `player_walk` | `ACTOR_ORIGIN_CENTERED` | Actor y_min = -1.000 WU — origin appears to be at model centre rather than ground level. Move origin to bottom-centre in Blender so the c... | audit-assets |
| `npc_banker_idle` | `ACTOR_SINGLE_MESH` | Actor has only 1 mesh node. OSRS-style characters use separate mesh objects per body segment (head, torso, upper/lower arms, upper/lower ... | audit-assets |
| `npc_banker_walk` | `ACTOR_SINGLE_MESH` | Actor has only 1 mesh node. OSRS-style characters use separate mesh objects per body segment (head, torso, upper/lower arms, upper/lower ... | audit-assets |
| `npc_chicken_action` | `ACTOR_SINGLE_MESH` | Actor has only 1 mesh node. OSRS-style characters use separate mesh objects per body segment (head, torso, upper/lower arms, upper/lower ... | audit-assets |
| `npc_chicken_base` | `ACTOR_SINGLE_MESH` | Actor has only 1 mesh node. OSRS-style characters use separate mesh objects per body segment (head, torso, upper/lower arms, upper/lower ... | audit-assets |
| `npc_chicken_idle` | `ACTOR_SINGLE_MESH` | Actor has only 1 mesh node. OSRS-style characters use separate mesh objects per body segment (head, torso, upper/lower arms, upper/lower ... | audit-assets |
| `npc_chicken_walk` | `ACTOR_SINGLE_MESH` | Actor has only 1 mesh node. OSRS-style characters use separate mesh objects per body segment (head, torso, upper/lower arms, upper/lower ... | audit-assets |
| `npc_cow_action` | `ACTOR_SINGLE_MESH` | Actor has only 1 mesh node. OSRS-style characters use separate mesh objects per body segment (head, torso, upper/lower arms, upper/lower ... | audit-assets |
| `npc_cow_base` | `ACTOR_SINGLE_MESH` | Actor has only 1 mesh node. OSRS-style characters use separate mesh objects per body segment (head, torso, upper/lower arms, upper/lower ... | audit-assets |
| `npc_cow_idle` | `ACTOR_SINGLE_MESH` | Actor has only 1 mesh node. OSRS-style characters use separate mesh objects per body segment (head, torso, upper/lower arms, upper/lower ... | audit-assets |
| `npc_cow_walk` | `ACTOR_SINGLE_MESH` | Actor has only 1 mesh node. OSRS-style characters use separate mesh objects per body segment (head, torso, upper/lower arms, upper/lower ... | audit-assets |
| `npc_giant_rat_action` | `ACTOR_SINGLE_MESH` | Actor has only 1 mesh node. OSRS-style characters use separate mesh objects per body segment (head, torso, upper/lower arms, upper/lower ... | audit-assets |
| `npc_giant_rat_base` | `ACTOR_SINGLE_MESH` | Actor has only 1 mesh node. OSRS-style characters use separate mesh objects per body segment (head, torso, upper/lower arms, upper/lower ... | audit-assets |
| `npc_giant_rat_idle` | `ACTOR_SINGLE_MESH` | Actor has only 1 mesh node. OSRS-style characters use separate mesh objects per body segment (head, torso, upper/lower arms, upper/lower ... | audit-assets |
| `npc_giant_rat_walk` | `ACTOR_SINGLE_MESH` | Actor has only 1 mesh node. OSRS-style characters use separate mesh objects per body segment (head, torso, upper/lower arms, upper/lower ... | audit-assets |
| `npc_goblin_action` | `ACTOR_SINGLE_MESH` | Actor has only 1 mesh node. OSRS-style characters use separate mesh objects per body segment (head, torso, upper/lower arms, upper/lower ... | audit-assets |
| `npc_goblin_idle` | `ACTOR_SINGLE_MESH` | Actor has only 1 mesh node. OSRS-style characters use separate mesh objects per body segment (head, torso, upper/lower arms, upper/lower ... | audit-assets |
| `npc_goblin_walk` | `ACTOR_SINGLE_MESH` | Actor has only 1 mesh node. OSRS-style characters use separate mesh objects per body segment (head, torso, upper/lower arms, upper/lower ... | audit-assets |
| `npc_guide_idle` | `ACTOR_SINGLE_MESH` | Actor has only 1 mesh node. OSRS-style characters use separate mesh objects per body segment (head, torso, upper/lower arms, upper/lower ... | audit-assets |
| `npc_guide_walk` | `ACTOR_SINGLE_MESH` | Actor has only 1 mesh node. OSRS-style characters use separate mesh objects per body segment (head, torso, upper/lower arms, upper/lower ... | audit-assets |
| `npc_instructor_idle` | `ACTOR_SINGLE_MESH` | Actor has only 1 mesh node. OSRS-style characters use separate mesh objects per body segment (head, torso, upper/lower arms, upper/lower ... | audit-assets |
| `npc_instructor_walk` | `ACTOR_SINGLE_MESH` | Actor has only 1 mesh node. OSRS-style characters use separate mesh objects per body segment (head, torso, upper/lower arms, upper/lower ... | audit-assets |
| `npc_rat_action` | `ACTOR_SINGLE_MESH` | Actor has only 1 mesh node. OSRS-style characters use separate mesh objects per body segment (head, torso, upper/lower arms, upper/lower ... | audit-assets |
| `npc_rat_base` | `ACTOR_SINGLE_MESH` | Actor has only 1 mesh node. OSRS-style characters use separate mesh objects per body segment (head, torso, upper/lower arms, upper/lower ... | audit-assets |
| `npc_rat_idle` | `ACTOR_SINGLE_MESH` | Actor has only 1 mesh node. OSRS-style characters use separate mesh objects per body segment (head, torso, upper/lower arms, upper/lower ... | audit-assets |
| `npc_rat_walk` | `ACTOR_SINGLE_MESH` | Actor has only 1 mesh node. OSRS-style characters use separate mesh objects per body segment (head, torso, upper/lower arms, upper/lower ... | audit-assets |
| `player_base` | `ACTOR_SINGLE_MESH` | Actor has only 1 mesh node. OSRS-style characters use separate mesh objects per body segment (head, torso, upper/lower arms, upper/lower ... | audit-assets |
| `player_chop` | `ACTOR_SINGLE_MESH` | Actor has only 1 mesh node. OSRS-style characters use separate mesh objects per body segment (head, torso, upper/lower arms, upper/lower ... | audit-assets |
| `player_fish` | `ACTOR_SINGLE_MESH` | Actor has only 1 mesh node. OSRS-style characters use separate mesh objects per body segment (head, torso, upper/lower arms, upper/lower ... | audit-assets |
| `player_idle` | `ACTOR_SINGLE_MESH` | Actor has only 1 mesh node. OSRS-style characters use separate mesh objects per body segment (head, torso, upper/lower arms, upper/lower ... | audit-assets |
| `player_mine` | `ACTOR_SINGLE_MESH` | Actor has only 1 mesh node. OSRS-style characters use separate mesh objects per body segment (head, torso, upper/lower arms, upper/lower ... | audit-assets |
| `player_pickup` | `ACTOR_SINGLE_MESH` | Actor has only 1 mesh node. OSRS-style characters use separate mesh objects per body segment (head, torso, upper/lower arms, upper/lower ... | audit-assets |
| `player_spear` | `ACTOR_SINGLE_MESH` | Actor has only 1 mesh node. OSRS-style characters use separate mesh objects per body segment (head, torso, upper/lower arms, upper/lower ... | audit-assets |
| `player_sword` | `ACTOR_SINGLE_MESH` | Actor has only 1 mesh node. OSRS-style characters use separate mesh objects per body segment (head, torso, upper/lower arms, upper/lower ... | audit-assets |
| `player_walk` | `ACTOR_SINGLE_MESH` | Actor has only 1 mesh node. OSRS-style characters use separate mesh objects per body segment (head, torso, upper/lower arms, upper/lower ... | audit-assets |
| `npc_chicken_base` | `CLIP_NON_TICK_DURATION` | Clip 'idle': duration 1.0000s is not a multiple of 0.6s (nearest tick: 1.2s, off by 0.2000s). Adjust the NLA track length in Blender so i... | audit-assets |
| `npc_chicken_base` | `CLIP_NON_TICK_DURATION` | Clip 'walk': duration 0.8000s is not a multiple of 0.6s (nearest tick: 0.6s, off by 0.2000s). Adjust the NLA track length in Blender so i... | audit-assets |
| `npc_chicken_base` | `CLIP_NON_TICK_DURATION` | Clip 'action': duration 0.7200s is not a multiple of 0.6s (nearest tick: 0.6s, off by 0.1200s). Adjust the NLA track length in Blender so... | audit-assets |
| `npc_cow_base` | `CLIP_NON_TICK_DURATION` | Clip 'idle': duration 1.0000s is not a multiple of 0.6s (nearest tick: 1.2s, off by 0.2000s). Adjust the NLA track length in Blender so i... | audit-assets |
| `npc_cow_base` | `CLIP_NON_TICK_DURATION` | Clip 'walk': duration 0.8000s is not a multiple of 0.6s (nearest tick: 0.6s, off by 0.2000s). Adjust the NLA track length in Blender so i... | audit-assets |
| `npc_cow_base` | `CLIP_NON_TICK_DURATION` | Clip 'action': duration 0.7200s is not a multiple of 0.6s (nearest tick: 0.6s, off by 0.1200s). Adjust the NLA track length in Blender so... | audit-assets |
| `npc_giant_rat_base` | `CLIP_NON_TICK_DURATION` | Clip 'idle': duration 1.0000s is not a multiple of 0.6s (nearest tick: 1.2s, off by 0.2000s). Adjust the NLA track length in Blender so i... | audit-assets |
| `npc_giant_rat_base` | `CLIP_NON_TICK_DURATION` | Clip 'walk': duration 0.8000s is not a multiple of 0.6s (nearest tick: 0.6s, off by 0.2000s). Adjust the NLA track length in Blender so i... | audit-assets |
| `npc_giant_rat_base` | `CLIP_NON_TICK_DURATION` | Clip 'action': duration 0.7200s is not a multiple of 0.6s (nearest tick: 0.6s, off by 0.1200s). Adjust the NLA track length in Blender so... | audit-assets |
| `npc_rat_base` | `CLIP_NON_TICK_DURATION` | Clip 'idle': duration 1.0000s is not a multiple of 0.6s (nearest tick: 1.2s, off by 0.2000s). Adjust the NLA track length in Blender so i... | audit-assets |
| `npc_rat_base` | `CLIP_NON_TICK_DURATION` | Clip 'walk': duration 0.8000s is not a multiple of 0.6s (nearest tick: 0.6s, off by 0.2000s). Adjust the NLA track length in Blender so i... | audit-assets |
| `npc_rat_base` | `CLIP_NON_TICK_DURATION` | Clip 'action': duration 0.7200s is not a multiple of 0.6s (nearest tick: 0.6s, off by 0.1200s). Adjust the NLA track length in Blender so... | audit-assets |
| `player_base` | `CLIP_WRONG_DURATION` | Clip 'attack_shoot': duration 0.583s (1 tick(s)), expected 1.8s (3 tick(s)) per docs/GRAPHICS_STYLE.md. Wrong tick count changes animatio... | audit-assets |
| `player_base` | `CLIP_WRONG_DURATION` | Clip 'attack_slash': duration 0.583s (1 tick(s)), expected 1.8s (3 tick(s)) per docs/GRAPHICS_STYLE.md. Wrong tick count changes animatio... | audit-assets |
| `player_base` | `CLIP_WRONG_DURATION` | Clip 'attack_stab': duration 0.583s (1 tick(s)), expected 1.8s (3 tick(s)) per docs/GRAPHICS_STYLE.md. Wrong tick count changes animation... | audit-assets |
| `player_base` | `CLIP_WRONG_DURATION` | Clip 'attack_throw': duration 0.583s (1 tick(s)), expected 1.8s (3 tick(s)) per docs/GRAPHICS_STYLE.md. Wrong tick count changes animatio... | audit-assets |
| `player_base` | `CLIP_WRONG_DURATION` | Clip 'chop': duration 0.583s (1 tick(s)), expected 1.2s (2 tick(s)) per docs/GRAPHICS_STYLE.md. Wrong tick count changes animation rhythm... | audit-assets |
| `player_base` | `CLIP_WRONG_DURATION` | Clip 'cook': duration 0.583s (1 tick(s)), expected 1.8s (3 tick(s)) per docs/GRAPHICS_STYLE.md. Wrong tick count changes animation rhythm... | audit-assets |
| `player_base` | `CLIP_WRONG_DURATION` | Clip 'fish': duration 1.792s (3 tick(s)), expected 2.4s (4 tick(s)) per docs/GRAPHICS_STYLE.md. Wrong tick count changes animation rhythm... | audit-assets |
| `player_base` | `CLIP_WRONG_DURATION` | Clip 'mine': duration 0.583s (1 tick(s)), expected 2.4s (4 tick(s)) per docs/GRAPHICS_STYLE.md. Wrong tick count changes animation rhythm... | audit-assets |
| `player_base` | `CLIP_WRONG_DURATION` | Clip 'pickup': duration 1.208s (2 tick(s)), expected 0.6s (1 tick(s)) per docs/GRAPHICS_STYLE.md. Wrong tick count changes animation rhyt... | audit-assets |
| `player_base` | `CLIP_WRONG_DURATION` | Clip 'smith': duration 0.583s (1 tick(s)), expected 1.8s (3 tick(s)) per docs/GRAPHICS_STYLE.md. Wrong tick count changes animation rhyth... | audit-assets |
| `equip_body_adamant_platebody` | `EQUIP_NO_HIDE_NODES` | Equipment slot BODY (6) has empty hide_nodes. Body-covering equipment must hide the underlying player_base nodes (e.g. head, hair, torso)... | audit-assets |
| `equip_body_black_platebody` | `EQUIP_NO_HIDE_NODES` | Equipment slot BODY (6) has empty hide_nodes. Body-covering equipment must hide the underlying player_base nodes (e.g. head, hair, torso)... | audit-assets |
| `equip_body_blue_wizard_robe` | `EQUIP_NO_HIDE_NODES` | Equipment slot BODY (6) has empty hide_nodes. Body-covering equipment must hide the underlying player_base nodes (e.g. head, hair, torso)... | audit-assets |
| `equip_body_dragon_chainbody` | `EQUIP_NO_HIDE_NODES` | Equipment slot BODY (6) has empty hide_nodes. Body-covering equipment must hide the underlying player_base nodes (e.g. head, hair, torso)... | audit-assets |
| `equip_body_iron_platebody` | `EQUIP_NO_HIDE_NODES` | Equipment slot BODY (6) has empty hide_nodes. Body-covering equipment must hide the underlying player_base nodes (e.g. head, hair, torso)... | audit-assets |
| `equip_body_leather_body` | `EQUIP_NO_HIDE_NODES` | Equipment slot BODY (6) has empty hide_nodes. Body-covering equipment must hide the underlying player_base nodes (e.g. head, hair, torso)... | audit-assets |
| `equip_body_mithril_platebody` | `EQUIP_NO_HIDE_NODES` | Equipment slot BODY (6) has empty hide_nodes. Body-covering equipment must hide the underlying player_base nodes (e.g. head, hair, torso)... | audit-assets |
| `equip_body_rune_platebody` | `EQUIP_NO_HIDE_NODES` | Equipment slot BODY (6) has empty hide_nodes. Body-covering equipment must hide the underlying player_base nodes (e.g. head, hair, torso)... | audit-assets |
| `equip_body_steel_platebody` | `EQUIP_NO_HIDE_NODES` | Equipment slot BODY (6) has empty hide_nodes. Body-covering equipment must hide the underlying player_base nodes (e.g. head, hair, torso)... | audit-assets |
| `equip_feet_leather_boots` | `EQUIP_NO_HIDE_NODES` | Equipment slot FEET (9) has empty hide_nodes. Body-covering equipment must hide the underlying player_base nodes (e.g. head, hair, torso)... | audit-assets |
| `equip_hands_leather_gloves` | `EQUIP_NO_HIDE_NODES` | Equipment slot HANDS (8) has empty hide_nodes. Body-covering equipment must hide the underlying player_base nodes (e.g. head, hair, torso... | audit-assets |
| `equip_head_adamant_full_helm` | `EQUIP_NO_HIDE_NODES` | Equipment slot HEAD (0) has empty hide_nodes. Body-covering equipment must hide the underlying player_base nodes (e.g. head, hair, torso)... | audit-assets |
| `equip_head_black_full_helm` | `EQUIP_NO_HIDE_NODES` | Equipment slot HEAD (0) has empty hide_nodes. Body-covering equipment must hide the underlying player_base nodes (e.g. head, hair, torso)... | audit-assets |
| `equip_head_blue_wizard_hat` | `EQUIP_NO_HIDE_NODES` | Equipment slot HEAD (0) has empty hide_nodes. Body-covering equipment must hide the underlying player_base nodes (e.g. head, hair, torso)... | audit-assets |
| `equip_head_dragon_full_helm` | `EQUIP_NO_HIDE_NODES` | Equipment slot HEAD (0) has empty hide_nodes. Body-covering equipment must hide the underlying player_base nodes (e.g. head, hair, torso)... | audit-assets |
| `equip_head_iron_full_helm` | `EQUIP_NO_HIDE_NODES` | Equipment slot HEAD (0) has empty hide_nodes. Body-covering equipment must hide the underlying player_base nodes (e.g. head, hair, torso)... | audit-assets |
| `equip_head_leather_cowl` | `EQUIP_NO_HIDE_NODES` | Equipment slot HEAD (0) has empty hide_nodes. Body-covering equipment must hide the underlying player_base nodes (e.g. head, hair, torso)... | audit-assets |
| `equip_head_mithril_full_helm` | `EQUIP_NO_HIDE_NODES` | Equipment slot HEAD (0) has empty hide_nodes. Body-covering equipment must hide the underlying player_base nodes (e.g. head, hair, torso)... | audit-assets |
| `equip_head_rune_full_helm` | `EQUIP_NO_HIDE_NODES` | Equipment slot HEAD (0) has empty hide_nodes. Body-covering equipment must hide the underlying player_base nodes (e.g. head, hair, torso)... | audit-assets |
| `equip_head_steel_full_helm` | `EQUIP_NO_HIDE_NODES` | Equipment slot HEAD (0) has empty hide_nodes. Body-covering equipment must hide the underlying player_base nodes (e.g. head, hair, torso)... | audit-assets |
| `equip_legs_blue_wizard_skirt` | `EQUIP_NO_HIDE_NODES` | Equipment slot LEGS (7) has empty hide_nodes. Body-covering equipment must hide the underlying player_base nodes (e.g. head, hair, torso)... | audit-assets |
| `equip_legs_leather_chaps` | `EQUIP_NO_HIDE_NODES` | Equipment slot LEGS (7) has empty hide_nodes. Body-covering equipment must hide the underlying player_base nodes (e.g. head, hair, torso)... | audit-assets |
| `equip_body_adamant_platebody` | `MATERIAL_COUNT_HIGH` | Material count = 4 — exceeds limit (3) for category 'equipment'. OSRS-style assets use one material per colour zone. Merge materials in B... | audit-assets |
| `equip_body_black_platebody` | `MATERIAL_COUNT_HIGH` | Material count = 4 — exceeds limit (3) for category 'equipment'. OSRS-style assets use one material per colour zone. Merge materials in B... | audit-assets |
| `equip_body_dragon_chainbody` | `MATERIAL_COUNT_HIGH` | Material count = 4 — exceeds limit (3) for category 'equipment'. OSRS-style assets use one material per colour zone. Merge materials in B... | audit-assets |
| `equip_body_iron_platebody` | `MATERIAL_COUNT_HIGH` | Material count = 4 — exceeds limit (3) for category 'equipment'. OSRS-style assets use one material per colour zone. Merge materials in B... | audit-assets |
| `equip_body_mithril_platebody` | `MATERIAL_COUNT_HIGH` | Material count = 4 — exceeds limit (3) for category 'equipment'. OSRS-style assets use one material per colour zone. Merge materials in B... | audit-assets |
| `equip_body_rune_platebody` | `MATERIAL_COUNT_HIGH` | Material count = 4 — exceeds limit (3) for category 'equipment'. OSRS-style assets use one material per colour zone. Merge materials in B... | audit-assets |
| `equip_body_steel_platebody` | `MATERIAL_COUNT_HIGH` | Material count = 4 — exceeds limit (3) for category 'equipment'. OSRS-style assets use one material per colour zone. Merge materials in B... | audit-assets |
| `equip_legs_adamant_platelegs` | `MATERIAL_COUNT_HIGH` | Material count = 4 — exceeds limit (3) for category 'equipment'. OSRS-style assets use one material per colour zone. Merge materials in B... | audit-assets |
| `equip_legs_black_platelegs` | `MATERIAL_COUNT_HIGH` | Material count = 4 — exceeds limit (3) for category 'equipment'. OSRS-style assets use one material per colour zone. Merge materials in B... | audit-assets |
| `equip_legs_dragon_platelegs` | `MATERIAL_COUNT_HIGH` | Material count = 4 — exceeds limit (3) for category 'equipment'. OSRS-style assets use one material per colour zone. Merge materials in B... | audit-assets |
| `equip_legs_iron_platelegs` | `MATERIAL_COUNT_HIGH` | Material count = 4 — exceeds limit (3) for category 'equipment'. OSRS-style assets use one material per colour zone. Merge materials in B... | audit-assets |
| `equip_legs_mithril_platelegs` | `MATERIAL_COUNT_HIGH` | Material count = 4 — exceeds limit (3) for category 'equipment'. OSRS-style assets use one material per colour zone. Merge materials in B... | audit-assets |
| `equip_legs_rune_platelegs` | `MATERIAL_COUNT_HIGH` | Material count = 4 — exceeds limit (3) for category 'equipment'. OSRS-style assets use one material per colour zone. Merge materials in B... | audit-assets |
| `equip_legs_steel_platelegs` | `MATERIAL_COUNT_HIGH` | Material count = 4 — exceeds limit (3) for category 'equipment'. OSRS-style assets use one material per colour zone. Merge materials in B... | audit-assets |
| `equip_weapon_adamant_scimitar` | `MATERIAL_COUNT_HIGH` | Material count = 4 — exceeds limit (3) for category 'equipment'. OSRS-style assets use one material per colour zone. Merge materials in B... | audit-assets |
| `equip_weapon_black_scimitar` | `MATERIAL_COUNT_HIGH` | Material count = 4 — exceeds limit (3) for category 'equipment'. OSRS-style assets use one material per colour zone. Merge materials in B... | audit-assets |
| `equip_weapon_bronze_scimitar` | `MATERIAL_COUNT_HIGH` | Material count = 4 — exceeds limit (3) for category 'equipment'. OSRS-style assets use one material per colour zone. Merge materials in B... | audit-assets |
| `equip_weapon_dragon_scimitar` | `MATERIAL_COUNT_HIGH` | Material count = 4 — exceeds limit (3) for category 'equipment'. OSRS-style assets use one material per colour zone. Merge materials in B... | audit-assets |
| `equip_weapon_iron_scimitar` | `MATERIAL_COUNT_HIGH` | Material count = 4 — exceeds limit (3) for category 'equipment'. OSRS-style assets use one material per colour zone. Merge materials in B... | audit-assets |
| `equip_weapon_mithril_scimitar` | `MATERIAL_COUNT_HIGH` | Material count = 4 — exceeds limit (3) for category 'equipment'. OSRS-style assets use one material per colour zone. Merge materials in B... | audit-assets |
| `equip_weapon_rune_scimitar` | `MATERIAL_COUNT_HIGH` | Material count = 4 — exceeds limit (3) for category 'equipment'. OSRS-style assets use one material per colour zone. Merge materials in B... | audit-assets |
| `equip_weapon_steel_scimitar` | `MATERIAL_COUNT_HIGH` | Material count = 4 — exceeds limit (3) for category 'equipment'. OSRS-style assets use one material per colour zone. Merge materials in B... | audit-assets |
| `npc_banker_base` | `MATERIAL_COUNT_HIGH` | Material count = 12 — exceeds limit (4) for category 'actor'. OSRS-style assets use one material per colour zone. Merge materials in Blen... | audit-assets |
| `npc_banker_idle` | `MATERIAL_COUNT_HIGH` | Material count = 6 — exceeds limit (4) for category 'actor'. OSRS-style assets use one material per colour zone. Merge materials in Blend... | audit-assets |
| `npc_banker_walk` | `MATERIAL_COUNT_HIGH` | Material count = 6 — exceeds limit (4) for category 'actor'. OSRS-style assets use one material per colour zone. Merge materials in Blend... | audit-assets |
| `npc_chicken_action` | `MATERIAL_COUNT_HIGH` | Material count = 5 — exceeds limit (4) for category 'actor'. OSRS-style assets use one material per colour zone. Merge materials in Blend... | audit-assets |
| `npc_chicken_base` | `MATERIAL_COUNT_HIGH` | Material count = 5 — exceeds limit (4) for category 'actor'. OSRS-style assets use one material per colour zone. Merge materials in Blend... | audit-assets |
| `npc_chicken_idle` | `MATERIAL_COUNT_HIGH` | Material count = 5 — exceeds limit (4) for category 'actor'. OSRS-style assets use one material per colour zone. Merge materials in Blend... | audit-assets |
| `npc_chicken_walk` | `MATERIAL_COUNT_HIGH` | Material count = 5 — exceeds limit (4) for category 'actor'. OSRS-style assets use one material per colour zone. Merge materials in Blend... | audit-assets |
| `npc_goblin_action` | `MATERIAL_COUNT_HIGH` | Material count = 7 — exceeds limit (4) for category 'actor'. OSRS-style assets use one material per colour zone. Merge materials in Blend... | audit-assets |
| `npc_goblin_base` | `MATERIAL_COUNT_HIGH` | Material count = 12 — exceeds limit (4) for category 'actor'. OSRS-style assets use one material per colour zone. Merge materials in Blen... | audit-assets |
| `npc_goblin_idle` | `MATERIAL_COUNT_HIGH` | Material count = 6 — exceeds limit (4) for category 'actor'. OSRS-style assets use one material per colour zone. Merge materials in Blend... | audit-assets |
| `npc_goblin_walk` | `MATERIAL_COUNT_HIGH` | Material count = 6 — exceeds limit (4) for category 'actor'. OSRS-style assets use one material per colour zone. Merge materials in Blend... | audit-assets |
| `npc_guide_base` | `MATERIAL_COUNT_HIGH` | Material count = 12 — exceeds limit (4) for category 'actor'. OSRS-style assets use one material per colour zone. Merge materials in Blen... | audit-assets |
| `npc_instructor_base` | `MATERIAL_COUNT_HIGH` | Material count = 12 — exceeds limit (4) for category 'actor'. OSRS-style assets use one material per colour zone. Merge materials in Blen... | audit-assets |
| `player_chop` | `MATERIAL_COUNT_HIGH` | Material count = 7 — exceeds limit (4) for category 'actor'. OSRS-style assets use one material per colour zone. Merge materials in Blend... | audit-assets |
| `player_fish` | `MATERIAL_COUNT_HIGH` | Material count = 7 — exceeds limit (4) for category 'actor'. OSRS-style assets use one material per colour zone. Merge materials in Blend... | audit-assets |
| `player_idle` | `MATERIAL_COUNT_HIGH` | Material count = 6 — exceeds limit (4) for category 'actor'. OSRS-style assets use one material per colour zone. Merge materials in Blend... | audit-assets |
| `player_mine` | `MATERIAL_COUNT_HIGH` | Material count = 7 — exceeds limit (4) for category 'actor'. OSRS-style assets use one material per colour zone. Merge materials in Blend... | audit-assets |
| `player_pickup` | `MATERIAL_COUNT_HIGH` | Material count = 6 — exceeds limit (4) for category 'actor'. OSRS-style assets use one material per colour zone. Merge materials in Blend... | audit-assets |
| `player_spear` | `MATERIAL_COUNT_HIGH` | Material count = 7 — exceeds limit (4) for category 'actor'. OSRS-style assets use one material per colour zone. Merge materials in Blend... | audit-assets |
| `player_sword` | `MATERIAL_COUNT_HIGH` | Material count = 7 — exceeds limit (4) for category 'actor'. OSRS-style assets use one material per colour zone. Merge materials in Blend... | audit-assets |
| `player_walk` | `MATERIAL_COUNT_HIGH` | Material count = 6 — exceeds limit (4) for category 'actor'. OSRS-style assets use one material per colour zone. Merge materials in Blend... | audit-assets |
| `tree` | `MATERIAL_COUNT_HIGH` | Material count = 3 — exceeds limit (2) for category 'resource'. OSRS-style assets use one material per colour zone. Merge materials in Bl... | audit-assets |
| `tree_magic` | `MATERIAL_COUNT_HIGH` | Material count = 3 — exceeds limit (2) for category 'resource'. OSRS-style assets use one material per colour zone. Merge materials in Bl... | audit-assets |
| `anvil` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `barrel_small` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `bench_small` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `building_shell_coastal` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `building_shell_coastal_base` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `building_shell_coastal_roof` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `building_shell_service` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `building_shell_service_base` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `building_shell_service_roof` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `building_shell_small` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `building_shell_small_base` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `building_shell_small_roof` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `cart_small` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `cooking_range` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `crate_small` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `dock_pier_small` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `dock_platform` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `dock_stairs` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_ammo_quiver_adamant` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_ammo_quiver_bronze` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_ammo_quiver_iron` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_ammo_quiver_mithril` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_ammo_quiver_rune` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_ammo_quiver_steel` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_body_adamant_platebody` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_body_black_platebody` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_body_blue_wizard_robe` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_body_bronze_platebody` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_body_dragon_chainbody` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_body_iron_platebody` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_body_leather_body` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_body_mithril_platebody` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_body_rune_platebody` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_body_steel_platebody` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_cape_black_cape` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_feet_leather_boots` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_hands_leather_gloves` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_head_adamant_full_helm` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_head_black_full_helm` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_head_blue_wizard_hat` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_head_bronze_full_helm` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_head_dragon_full_helm` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_head_iron_full_helm` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_head_leather_cowl` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_head_mithril_full_helm` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_head_rune_full_helm` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_head_steel_full_helm` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_legs_adamant_platelegs` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_legs_black_platelegs` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_legs_blue_wizard_skirt` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_legs_bronze_platelegs` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_legs_dragon_platelegs` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_legs_iron_platelegs` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_legs_leather_chaps` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_legs_mithril_platelegs` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_legs_rune_platelegs` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_legs_steel_platelegs` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_shield_adamant_sq` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_shield_black_sq` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_shield_bronze_sq` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_shield_dragon_sq` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_shield_iron_sq` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_shield_mithril_sq` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_shield_rune_sq` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_shield_steel_sq` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_shield_wooden` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_adamant_axe` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_adamant_longsword` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_adamant_pickaxe` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_adamant_scimitar` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_air_staff` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_black_axe` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_black_longsword` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_black_pickaxe` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_black_scimitar` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_bronze_axe` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_bronze_longsword` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_bronze_pickaxe` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_bronze_scimitar` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_bronze_sword` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_dragon_axe` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_dragon_longsword` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_dragon_pickaxe` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_dragon_scimitar` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_earth_staff` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_fire_staff` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_iron_axe` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_iron_longsword` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_iron_pickaxe` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_iron_scimitar` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_longbow` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_magic_longbow` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_magic_shortbow` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_maple_longbow` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_maple_shortbow` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_mithril_axe` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_mithril_longsword` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_mithril_pickaxe` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_mithril_scimitar` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_oak_longbow` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_oak_shortbow` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_rune_axe` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_rune_longsword` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_rune_pickaxe` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_rune_scimitar` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_shortbow` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_steel_axe` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_steel_longsword` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_steel_pickaxe` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_steel_scimitar` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_water_staff` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_willow_longbow` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_willow_shortbow` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_yew_longbow` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `equip_weapon_yew_shortbow` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `fence_post_small` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `fishing_spot` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `furnace` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `npc_banker_idle` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `npc_banker_walk` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `npc_chicken_action` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `npc_chicken_base` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `npc_chicken_idle` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `npc_chicken_walk` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `npc_cow_action` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `npc_cow_base` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `npc_cow_idle` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `npc_cow_walk` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `npc_giant_rat_action` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `npc_giant_rat_base` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `npc_giant_rat_idle` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `npc_giant_rat_walk` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `npc_goblin_action` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `npc_goblin_idle` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `npc_goblin_walk` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `npc_guide_idle` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `npc_guide_walk` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `npc_instructor_idle` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `npc_instructor_walk` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `npc_rat_action` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `npc_rat_base` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `npc_rat_idle` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `npc_rat_walk` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `player_base` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `player_chop` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `player_fish` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `player_idle` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `player_mine` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `player_pickup` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `player_spear` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `player_sword` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `player_walk` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `rock_adamantite` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `rock_coal` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `rock_copper` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `rock_gold` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `rock_iron` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `rock_mithril` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `rock_runite` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `rock_silver` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `rock_tin` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `sack_stack_small` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `signpost_small` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `table_small` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `tree` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `tree_magic` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `tree_mahogany` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `tree_maple` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `tree_oak` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `tree_willow` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `tree_yew` | `NO_SURFACE_COLOR` | No COLOR_0 vertex colors and no textures — model renders as flat grey. Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → A... | audit-assets |
| `building_shell_coastal_roof` | `PROP_FLOATING` | y_min = 1.200 WU — bottom of model is above the ground plane. In Blender: move the mesh down and set origin to bottom-centre so it sits f... | audit-assets |
| `building_shell_service_roof` | `PROP_FLOATING` | y_min = 1.500 WU — bottom of model is above the ground plane. In Blender: move the mesh down and set origin to bottom-centre so it sits f... | audit-assets |
| `building_shell_small_roof` | `PROP_FLOATING` | y_min = 1.450 WU — bottom of model is above the ground plane. In Blender: move the mesh down and set origin to bottom-centre so it sits f... | audit-assets |
| `anvil` | `PROP_SUNK` | y_min = -0.270 WU — bottom of model extends below the ground plane. Geometry is buried underground. Move origin to bottom-centre in Blender. | audit-assets |
| `barrel_small` | `PROP_SUNK` | y_min = -1.000 WU — bottom of model extends below the ground plane. Geometry is buried underground. Move origin to bottom-centre in Blender. | audit-assets |
| `bench_small` | `PROP_SUNK` | y_min = -1.000 WU — bottom of model extends below the ground plane. Geometry is buried underground. Move origin to bottom-centre in Blender. | audit-assets |
| `building_shell_coastal` | `PROP_SUNK` | y_min = -1.000 WU — bottom of model extends below the ground plane. Geometry is buried underground. Move origin to bottom-centre in Blender. | audit-assets |
| `building_shell_service` | `PROP_SUNK` | y_min = -1.000 WU — bottom of model extends below the ground plane. Geometry is buried underground. Move origin to bottom-centre in Blender. | audit-assets |
| `building_shell_small` | `PROP_SUNK` | y_min = -1.000 WU — bottom of model extends below the ground plane. Geometry is buried underground. Move origin to bottom-centre in Blender. | audit-assets |
| `cart_small` | `PROP_SUNK` | y_min = -1.000 WU — bottom of model extends below the ground plane. Geometry is buried underground. Move origin to bottom-centre in Blender. | audit-assets |
| `cooking_range` | `PROP_SUNK` | y_min = -1.000 WU — bottom of model extends below the ground plane. Geometry is buried underground. Move origin to bottom-centre in Blender. | audit-assets |
| `crate_small` | `PROP_SUNK` | y_min = -1.000 WU — bottom of model extends below the ground plane. Geometry is buried underground. Move origin to bottom-centre in Blender. | audit-assets |
| `dock_pier_small` | `PROP_SUNK` | y_min = -1.000 WU — bottom of model extends below the ground plane. Geometry is buried underground. Move origin to bottom-centre in Blender. | audit-assets |
| `dock_platform` | `PROP_SUNK` | y_min = -1.000 WU — bottom of model extends below the ground plane. Geometry is buried underground. Move origin to bottom-centre in Blender. | audit-assets |
| `dock_stairs` | `PROP_SUNK` | y_min = -1.000 WU — bottom of model extends below the ground plane. Geometry is buried underground. Move origin to bottom-centre in Blender. | audit-assets |
| `fence_post_small` | `PROP_SUNK` | y_min = -1.000 WU — bottom of model extends below the ground plane. Geometry is buried underground. Move origin to bottom-centre in Blender. | audit-assets |
| `fishing_spot` | `PROP_SUNK` | y_min = -1.000 WU — bottom of model extends below the ground plane. Geometry is buried underground. Move origin to bottom-centre in Blender. | audit-assets |
| `furnace` | `PROP_SUNK` | y_min = -1.000 WU — bottom of model extends below the ground plane. Geometry is buried underground. Move origin to bottom-centre in Blender. | audit-assets |
| `rock_adamantite` | `PROP_SUNK` | y_min = -1.000 WU — bottom of model extends below the ground plane. Geometry is buried underground. Move origin to bottom-centre in Blender. | audit-assets |
| `rock_coal` | `PROP_SUNK` | y_min = -1.000 WU — bottom of model extends below the ground plane. Geometry is buried underground. Move origin to bottom-centre in Blender. | audit-assets |
| `rock_copper` | `PROP_SUNK` | y_min = -1.000 WU — bottom of model extends below the ground plane. Geometry is buried underground. Move origin to bottom-centre in Blender. | audit-assets |
| `rock_gold` | `PROP_SUNK` | y_min = -1.000 WU — bottom of model extends below the ground plane. Geometry is buried underground. Move origin to bottom-centre in Blender. | audit-assets |
| `rock_iron` | `PROP_SUNK` | y_min = -1.000 WU — bottom of model extends below the ground plane. Geometry is buried underground. Move origin to bottom-centre in Blender. | audit-assets |
| `rock_mithril` | `PROP_SUNK` | y_min = -1.000 WU — bottom of model extends below the ground plane. Geometry is buried underground. Move origin to bottom-centre in Blender. | audit-assets |
| `rock_runite` | `PROP_SUNK` | y_min = -1.000 WU — bottom of model extends below the ground plane. Geometry is buried underground. Move origin to bottom-centre in Blender. | audit-assets |
| `rock_silver` | `PROP_SUNK` | y_min = -1.000 WU — bottom of model extends below the ground plane. Geometry is buried underground. Move origin to bottom-centre in Blender. | audit-assets |
| `rock_tin` | `PROP_SUNK` | y_min = -1.000 WU — bottom of model extends below the ground plane. Geometry is buried underground. Move origin to bottom-centre in Blender. | audit-assets |
| `sack_stack_small` | `PROP_SUNK` | y_min = -1.000 WU — bottom of model extends below the ground plane. Geometry is buried underground. Move origin to bottom-centre in Blender. | audit-assets |
| `signpost_small` | `PROP_SUNK` | y_min = -1.000 WU — bottom of model extends below the ground plane. Geometry is buried underground. Move origin to bottom-centre in Blender. | audit-assets |
| `table_small` | `PROP_SUNK` | y_min = -1.000 WU — bottom of model extends below the ground plane. Geometry is buried underground. Move origin to bottom-centre in Blender. | audit-assets |
| `tree` | `PROP_SUNK` | y_min = -0.975 WU — bottom of model extends below the ground plane. Geometry is buried underground. Move origin to bottom-centre in Blender. | audit-assets |
| `tree_magic` | `PROP_SUNK` | y_min = -1.000 WU — bottom of model extends below the ground plane. Geometry is buried underground. Move origin to bottom-centre in Blender. | audit-assets |
| `tree_mahogany` | `PROP_SUNK` | y_min = -1.000 WU — bottom of model extends below the ground plane. Geometry is buried underground. Move origin to bottom-centre in Blender. | audit-assets |
| `tree_maple` | `PROP_SUNK` | y_min = -1.000 WU — bottom of model extends below the ground plane. Geometry is buried underground. Move origin to bottom-centre in Blender. | audit-assets |
| `tree_oak` | `PROP_SUNK` | y_min = -0.875 WU — bottom of model extends below the ground plane. Geometry is buried underground. Move origin to bottom-centre in Blender. | audit-assets |
| `tree_willow` | `PROP_SUNK` | y_min = -1.000 WU — bottom of model extends below the ground plane. Geometry is buried underground. Move origin to bottom-centre in Blender. | audit-assets |
| `tree_yew` | `PROP_SUNK` | y_min = -1.000 WU — bottom of model extends below the ground plane. Geometry is buried underground. Move origin to bottom-centre in Blender. | audit-assets |
| `` | `SHELL_WALL_TOO_SHORT` | Wall height at scale 1.50 = 1.875 WU — only 1.04× player height. Walls should be at least 1.25× player height to feel OSRS-scaled. Consid... | validate-scene[main_world] |
| `` | `SHELL_WALL_TOO_SHORT` | Wall height at scale 1.50 = 1.875 WU — only 1.04× player height. Walls should be at least 1.25× player height to feel OSRS-scaled. Consid... | validate-scene[sandbox] |

## 🔵 Nice to have  (3)

| Key | Code | Detail | Source |
|-----|------|--------|--------|
| `entity:37:Fishing Supplier` | `ENTITY_BILLBOARD` | Sprite-only fallback. Add model_key_3d when GLB is ready. | report-entity-visuals |
| `entity:39:Smithing Supplier` | `ENTITY_BILLBOARD` | Sprite-only fallback. Add model_key_3d when GLB is ready. | report-entity-visuals |
| `entity:400:Cooking Fire` | `ENTITY_BILLBOARD` | Known intentional: Cooking Fire — particle/world-object; no GLB planned | report-entity-visuals |

## ⚪ Deprecated / legacy  (192)

| Key | Code | Detail | Source |
|-----|------|--------|--------|
| `npc_banker_idle` | `FRAGMENTED_ACTOR` | Separate pose file. 'npc_banker_base' exists — consolidate all clips as NLA tracks in the _base GLB then remove this pose file and its ma... | audit-assets |
| `npc_banker_walk` | `FRAGMENTED_ACTOR` | Separate pose file. 'npc_banker_base' exists — consolidate all clips as NLA tracks in the _base GLB then remove this pose file and its ma... | audit-assets |
| `npc_chicken_action` | `FRAGMENTED_ACTOR` | Separate pose file. 'npc_chicken_base' exists — consolidate all clips as NLA tracks in the _base GLB then remove this pose file and its m... | audit-assets |
| `npc_chicken_idle` | `FRAGMENTED_ACTOR` | Separate pose file. 'npc_chicken_base' exists — consolidate all clips as NLA tracks in the _base GLB then remove this pose file and its m... | audit-assets |
| `npc_chicken_walk` | `FRAGMENTED_ACTOR` | Separate pose file. 'npc_chicken_base' exists — consolidate all clips as NLA tracks in the _base GLB then remove this pose file and its m... | audit-assets |
| `npc_cow_action` | `FRAGMENTED_ACTOR` | Separate pose file. 'npc_cow_base' exists — consolidate all clips as NLA tracks in the _base GLB then remove this pose file and its manif... | audit-assets |
| `npc_cow_idle` | `FRAGMENTED_ACTOR` | Separate pose file. 'npc_cow_base' exists — consolidate all clips as NLA tracks in the _base GLB then remove this pose file and its manif... | audit-assets |
| `npc_cow_walk` | `FRAGMENTED_ACTOR` | Separate pose file. 'npc_cow_base' exists — consolidate all clips as NLA tracks in the _base GLB then remove this pose file and its manif... | audit-assets |
| `npc_giant_rat_action` | `FRAGMENTED_ACTOR` | Separate pose file. 'npc_giant_rat_base' exists — consolidate all clips as NLA tracks in the _base GLB then remove this pose file and its... | audit-assets |
| `npc_giant_rat_idle` | `FRAGMENTED_ACTOR` | Separate pose file. 'npc_giant_rat_base' exists — consolidate all clips as NLA tracks in the _base GLB then remove this pose file and its... | audit-assets |
| `npc_giant_rat_walk` | `FRAGMENTED_ACTOR` | Separate pose file. 'npc_giant_rat_base' exists — consolidate all clips as NLA tracks in the _base GLB then remove this pose file and its... | audit-assets |
| `npc_goblin_action` | `FRAGMENTED_ACTOR` | Separate pose file. 'npc_goblin_base' exists — consolidate all clips as NLA tracks in the _base GLB then remove this pose file and its ma... | audit-assets |
| `npc_goblin_idle` | `FRAGMENTED_ACTOR` | Separate pose file. 'npc_goblin_base' exists — consolidate all clips as NLA tracks in the _base GLB then remove this pose file and its ma... | audit-assets |
| `npc_goblin_walk` | `FRAGMENTED_ACTOR` | Separate pose file. 'npc_goblin_base' exists — consolidate all clips as NLA tracks in the _base GLB then remove this pose file and its ma... | audit-assets |
| `npc_guide_idle` | `FRAGMENTED_ACTOR` | Separate pose file. 'npc_guide_base' exists — consolidate all clips as NLA tracks in the _base GLB then remove this pose file and its man... | audit-assets |
| `npc_guide_walk` | `FRAGMENTED_ACTOR` | Separate pose file. 'npc_guide_base' exists — consolidate all clips as NLA tracks in the _base GLB then remove this pose file and its man... | audit-assets |
| `npc_instructor_idle` | `FRAGMENTED_ACTOR` | Separate pose file. 'npc_instructor_base' exists — consolidate all clips as NLA tracks in the _base GLB then remove this pose file and it... | audit-assets |
| `npc_instructor_walk` | `FRAGMENTED_ACTOR` | Separate pose file. 'npc_instructor_base' exists — consolidate all clips as NLA tracks in the _base GLB then remove this pose file and it... | audit-assets |
| `npc_rat_action` | `FRAGMENTED_ACTOR` | Separate pose file. 'npc_rat_base' exists — consolidate all clips as NLA tracks in the _base GLB then remove this pose file and its manif... | audit-assets |
| `npc_rat_idle` | `FRAGMENTED_ACTOR` | Separate pose file. 'npc_rat_base' exists — consolidate all clips as NLA tracks in the _base GLB then remove this pose file and its manif... | audit-assets |
| `npc_rat_walk` | `FRAGMENTED_ACTOR` | Separate pose file. 'npc_rat_base' exists — consolidate all clips as NLA tracks in the _base GLB then remove this pose file and its manif... | audit-assets |
| `player_chop` | `FRAGMENTED_ACTOR` | Separate pose file. 'player_base' exists — consolidate all clips as NLA tracks in the _base GLB then remove this pose file and its manife... | audit-assets |
| `player_fish` | `FRAGMENTED_ACTOR` | Separate pose file. 'player_base' exists — consolidate all clips as NLA tracks in the _base GLB then remove this pose file and its manife... | audit-assets |
| `player_idle` | `FRAGMENTED_ACTOR` | Separate pose file. 'player_base' exists — consolidate all clips as NLA tracks in the _base GLB then remove this pose file and its manife... | audit-assets |
| `player_mine` | `FRAGMENTED_ACTOR` | Separate pose file. 'player_base' exists — consolidate all clips as NLA tracks in the _base GLB then remove this pose file and its manife... | audit-assets |
| `player_pickup` | `FRAGMENTED_ACTOR` | Separate pose file. 'player_base' exists — consolidate all clips as NLA tracks in the _base GLB then remove this pose file and its manife... | audit-assets |
| `player_spear` | `FRAGMENTED_ACTOR` | Separate pose file. 'player_base' exists — consolidate all clips as NLA tracks in the _base GLB then remove this pose file and its manife... | audit-assets |
| `player_sword` | `FRAGMENTED_ACTOR` | Separate pose file. 'player_base' exists — consolidate all clips as NLA tracks in the _base GLB then remove this pose file and its manife... | audit-assets |
| `player_walk` | `FRAGMENTED_ACTOR` | Separate pose file. 'player_base' exists — consolidate all clips as NLA tracks in the _base GLB then remove this pose file and its manife... | audit-assets |
| `barrel_small` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `bench_small` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `building_shell_coastal` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `building_shell_service` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `building_shell_small` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `cart_small` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `cooking_range` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `crate_small` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `dock_pier_small` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `dock_platform` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `dock_stairs` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_ammo_quiver_adamant` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_ammo_quiver_bronze` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_ammo_quiver_iron` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_ammo_quiver_mithril` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_ammo_quiver_rune` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_ammo_quiver_steel` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_body_adamant_platebody` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_body_black_platebody` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_body_blue_wizard_robe` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_body_dragon_chainbody` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_body_iron_platebody` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_body_leather_body` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_body_mithril_platebody` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_body_rune_platebody` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_body_steel_platebody` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_cape_black_cape` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_feet_leather_boots` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_hands_leather_gloves` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_head_adamant_full_helm` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_head_black_full_helm` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_head_blue_wizard_hat` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_head_dragon_full_helm` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_head_iron_full_helm` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_head_leather_cowl` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_head_mithril_full_helm` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_head_rune_full_helm` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_head_steel_full_helm` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_legs_adamant_platelegs` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_legs_black_platelegs` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_legs_blue_wizard_skirt` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_legs_dragon_platelegs` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_legs_iron_platelegs` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_legs_leather_chaps` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_legs_mithril_platelegs` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_legs_rune_platelegs` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_legs_steel_platelegs` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_shield_adamant_sq` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_shield_black_sq` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_shield_dragon_sq` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_shield_iron_sq` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_shield_mithril_sq` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_shield_rune_sq` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_shield_steel_sq` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_shield_wooden` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_adamant_axe` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_adamant_longsword` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_adamant_pickaxe` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_adamant_scimitar` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_air_staff` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_black_axe` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_black_longsword` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_black_pickaxe` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_black_scimitar` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_bronze_axe` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_bronze_longsword` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_bronze_pickaxe` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_bronze_scimitar` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_dragon_longsword` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_dragon_pickaxe` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_dragon_scimitar` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_earth_staff` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_fire_staff` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_iron_axe` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_iron_longsword` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_iron_pickaxe` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_iron_scimitar` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_longbow` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_magic_longbow` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_magic_shortbow` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_maple_longbow` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_maple_shortbow` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_mithril_axe` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_mithril_longsword` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_mithril_pickaxe` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_mithril_scimitar` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_oak_longbow` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_oak_shortbow` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_rune_axe` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_rune_longsword` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_rune_pickaxe` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_rune_scimitar` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_shortbow` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_steel_axe` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_steel_longsword` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_steel_pickaxe` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_steel_scimitar` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_water_staff` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_willow_longbow` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_willow_shortbow` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_yew_longbow` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `equip_weapon_yew_shortbow` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `fence_post_small` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `fishing_spot` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `furnace` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `npc_banker_base` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `npc_banker_idle` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `npc_banker_walk` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `npc_chicken_action` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `npc_chicken_base` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `npc_chicken_idle` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `npc_chicken_walk` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `npc_cow_action` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `npc_cow_base` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `npc_cow_idle` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `npc_cow_walk` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `npc_giant_rat_action` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `npc_giant_rat_base` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `npc_giant_rat_idle` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `npc_giant_rat_walk` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `npc_goblin_action` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `npc_goblin_base` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `npc_goblin_idle` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `npc_goblin_walk` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `npc_guide_base` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `npc_guide_idle` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `npc_guide_walk` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `npc_instructor_base` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `npc_instructor_idle` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `npc_instructor_walk` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `npc_rat_action` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `npc_rat_base` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `npc_rat_idle` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `npc_rat_walk` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `player_chop` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `player_fish` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `player_idle` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `player_mine` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `player_pickup` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `player_spear` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `player_sword` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `player_walk` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `rock_adamantite` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `rock_coal` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `rock_copper` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `rock_gold` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `rock_iron` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `rock_mithril` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `rock_runite` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `rock_silver` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `rock_tin` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `sack_stack_small` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `signpost_small` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `table_small` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `tree_magic` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `tree_mahogany` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `tree_maple` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `tree_willow` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `tree_yew` | `NO_SOURCE_BLEND` | No source_blend path declared. The Blender source file for this model cannot be located for future edits. Add: source_blend: <relative-pa... | audit-assets |
| `building_shell_coastal_base` | `SOURCE_BLEND_MISSING` | source_blend 'shells/building_shell_coastal_base.blend' not found on disk. Commit the .blend file to art/blender/ or correct the path. | audit-assets |
| `building_shell_coastal_roof` | `SOURCE_BLEND_MISSING` | source_blend 'shells/building_shell_coastal_roof.blend' not found on disk. Commit the .blend file to art/blender/ or correct the path. | audit-assets |
| `building_shell_service_base` | `SOURCE_BLEND_MISSING` | source_blend 'shells/building_shell_service_base.blend' not found on disk. Commit the .blend file to art/blender/ or correct the path. | audit-assets |
| `building_shell_service_roof` | `SOURCE_BLEND_MISSING` | source_blend 'shells/building_shell_service_roof.blend' not found on disk. Commit the .blend file to art/blender/ or correct the path. | audit-assets |
