"""
Erynfall — fully automated player rig builder.

Requires NO existing .blend file. Run from repo root:

  /Applications/Blender.app/Contents/MacOS/Blender \
      --background \
      --python scripts/blender_build_rig.py

Reads from:
  art/blender/mixamo/john_rigged.fbx      rigged T-pose character
  art/blender/mixamo/<clip_name>.fbx      one FBX per animation clip

Writes:
  art/models/player_base.glb              skinned animated GLB for engine
  art/blender/player_base.blend           saved source file

Re-runnable: safe to run again if Blender crashes — just re-run the command.
"""

import bpy
import os
import sys
import mathutils

# ── Paths ─────────────────────────────────────────────────────
SCRIPT_DIR  = os.path.dirname(os.path.abspath(__file__))
REPO_ROOT   = os.path.dirname(SCRIPT_DIR)
MIXAMO_DIR  = os.path.join(REPO_ROOT, "art", "blender", "mixamo")
CHAR_FBX    = os.path.join(MIXAMO_DIR, "john_rigged.fbx")
GLB_OUT     = os.path.join(REPO_ROOT, "art", "models", "player_base.glb")
BLEND_OUT   = os.path.join(REPO_ROOT, "art", "blender", "player_base.blend")

# Clip name → target OSRS frame count at 24 fps
# 1 OSRS tick = 600ms = 14.4 frames
CLIPS = {
    'idle':             58,   # 4 ticks
    'walk':             29,   # 2 ticks
    'run':              14,   # 1 tick
    'pickup':           29,
    'chop':             14,
    'mine':             14,
    'fish':             43,   # 3 ticks
    'smith':            14,
    'cook':             14,
    'attack_slash':     14,
    'attack_stab':      14,
    'attack_shoot':     14,
    'attack_shoot_bow': 14,
    'attack_throw':     14,
    'block':            14,
    'death':            43,
}

BONE_RENAMES = {
    'mixamorig:Hips':          'pelvis',
    'mixamorig:Spine':         'spine',
    'mixamorig:Spine1':        'chest',
    'mixamorig:Spine2':        'chest_upper',
    'mixamorig:Neck':          'neck',
    'mixamorig:Head':          'head',
    'mixamorig:LeftShoulder':  'shoulder_l',
    'mixamorig:RightShoulder': 'shoulder_r',
    'mixamorig:LeftArm':       'upper_arm_l',
    'mixamorig:RightArm':      'upper_arm_r',
    'mixamorig:LeftForeArm':   'lower_arm_l',
    'mixamorig:RightForeArm':  'lower_arm_r',
    'mixamorig:LeftHand':      'hand_l',
    'mixamorig:RightHand':     'hand_r',
    'mixamorig:LeftUpLeg':     'upper_leg_l',
    'mixamorig:RightUpLeg':    'upper_leg_r',
    'mixamorig:LeftLeg':       'lower_leg_l',
    'mixamorig:RightLeg':      'lower_leg_r',
    'mixamorig:LeftFoot':      'foot_l',
    'mixamorig:RightFoot':     'foot_r',
    'mixamorig:LeftToeBase':   'toe_l',
    'mixamorig:RightToeBase':  'toe_r',
    'mixamorig:LeftHandIndex1':  'finger_l_1',
    'mixamorig:LeftHandIndex2':  'finger_l_2',
    'mixamorig:LeftHandIndex3':  'finger_l_3',
    'mixamorig:RightHandIndex1': 'finger_r_1',
    'mixamorig:RightHandIndex2': 'finger_r_2',
    'mixamorig:RightHandIndex3': 'finger_r_3',
    'mixamorig:HeadTop_End':     'head_top',
    'mixamorig:LeftToeBase_end': 'toe_l_end',
    'mixamorig:RightToeBase_end':'toe_r_end',
}

ANCHORS = [
    # (name,            parent,    head_offset,       tail_offset)
    ('weapon_anchor',  'hand_r',  (0,  0,    0   ),  (0,  0.08, 0   )),
    ('shield_anchor',  'hand_l',  (0,  0,    0   ),  (0,  0.08, 0   )),
    ('head_anchor',    'head',    (0,  0,    0   ),  (0,  0,    0.12)),
    ('cape_anchor',    'chest',   (0, -0.05, 0   ),  (0, -0.15, 0   )),
    ('ammo_anchor',    'pelvis',  (0.08, 0,  0   ),  (0.18, 0,  0   )),
]

# ─────────────────────────────────────────────────────────────
def log(msg): print(msg, flush=True)
def die(msg): sys.exit(f"\nERROR: {msg}")

# ─────────────────────────────────────────────────────────────
# VALIDATE INPUTS
# ─────────────────────────────────────────────────────────────
log("\n" + "="*60)
log("ERYNFALL PLAYER RIG BUILDER")
log("="*60)

if not os.path.exists(CHAR_FBX):
    die(f"Character FBX not found: {CHAR_FBX}\n"
        f"Put john_rigged.fbx in art/blender/mixamo/")

missing = []
for clip in CLIPS:
    fbx = os.path.join(MIXAMO_DIR, f"{clip}.fbx")
    if not os.path.exists(fbx):
        missing.append(f"  {clip}.fbx")
if missing:
    log("WARNING: Missing animation FBX files (will be skipped):")
    for m in missing:
        log(m)

# ─────────────────────────────────────────────────────────────
# CLEAN DEFAULT SCENE
# ─────────────────────────────────────────────────────────────
log("\n[1] Clearing default scene...")
bpy.ops.object.select_all(action='SELECT')
bpy.ops.object.delete()
for block in list(bpy.data.meshes) + list(bpy.data.materials) + list(bpy.data.cameras):
    try: block.user_clear(); bpy.data.batch_remove([block])
    except: pass

# ─────────────────────────────────────────────────────────────
# IMPORT RIGGED CHARACTER
# ─────────────────────────────────────────────────────────────
log(f"\n[2] Importing character: {CHAR_FBX}")
bpy.ops.import_scene.fbx(filepath=CHAR_FBX)

# Find the main armature (should be the only one after import)
main_arm = next(
    (o for o in bpy.data.objects if o.type == 'ARMATURE'), None
)
if not main_arm:
    die("No armature found after importing character FBX.")
log(f"    Armature: '{main_arm.name}'")

# Ensure armature has animation_data
if not main_arm.animation_data:
    main_arm.animation_data_create()

# ─────────────────────────────────────────────────────────────
# IMPORT ANIMATIONS + PUSH TO NLA
# ─────────────────────────────────────────────────────────────
log("\n[3] Importing animation clips...")

def get_all_actions():
    return set(a.name for a in bpy.data.actions)

def import_animation_clip(clip_name, fbx_path):
    """
    Import animation FBX, find the new action, rename it,
    assign to main armature, push to NLA, delete extra armature.
    """
    before = get_all_actions()
    before_arms = set(o.name for o in bpy.data.objects if o.type == 'ARMATURE')

    bpy.ops.import_scene.fbx(filepath=fbx_path)

    # Find new action(s)
    after = get_all_actions()
    new_action_names = after - before
    if not new_action_names:
        log(f"    WARNING: '{clip_name}' — no new action created, skipping")
        return False

    # Rename each new action to the clip name
    # (usually only one, but handle multiple just in case)
    action = None
    for aname in new_action_names:
        a = bpy.data.actions[aname]
        a.name = clip_name
        action = a
        break

    if not main_arm.animation_data:
        main_arm.animation_data_create()

    # Push directly to NLA using Python API — no editor context needed.
    # Each clip gets its own track so strips don't overlap.
    track = main_arm.animation_data.nla_tracks.new()
    track.name = clip_name
    strip = track.strips.new(clip_name, 1, action)
    strip.name = clip_name

    # Clear the active action slot so it doesn't conflict with NLA
    main_arm.animation_data.action = None

    # Clear active action slot
    main_arm.animation_data.action = None

    # Delete extra armatures that came in with this FBX
    after_arms = set(o.name for o in bpy.data.objects if o.type == 'ARMATURE')
    extra_arms = after_arms - before_arms
    for arm_name in extra_arms:
        obj = bpy.data.objects.get(arm_name)
        if obj:
            bpy.data.objects.remove(obj, do_unlink=True)

    log(f"    ✓ '{clip_name}' imported and pushed to NLA")
    return True

for clip_name in CLIPS:
    fbx_path = os.path.join(MIXAMO_DIR, f"{clip_name}.fbx")
    if not os.path.exists(fbx_path):
        log(f"    — '{clip_name}' skipped (no FBX)")
        continue
    import_animation_clip(clip_name, fbx_path)

# ─────────────────────────────────────────────────────────────
# RETIME ACTIONS TO OSRS TICK LENGTHS
# ─────────────────────────────────────────────────────────────
log("\n[4] Retiming clips to OSRS tick lengths (24 fps)...")

def get_fcurves(action):
    """Return all fcurves from an action.
    Handles both Blender 4.x (action.fcurves) and
    Blender 5.x (action.layers → strips → channelbags → fcurves).
    """
    # Blender 4.x and earlier
    if hasattr(action, 'fcurves') and action.fcurves is not None:
        try:
            if len(action.fcurves) > 0:
                return list(action.fcurves)
        except Exception:
            pass
    # Blender 5.x layered action API
    result = []
    if hasattr(action, 'layers'):
        for layer in action.layers:
            strips = getattr(layer, 'strips', [])
            for strip in strips:
                channelbags = getattr(strip, 'channelbags', [])
                for cbag in channelbags:
                    fcs = getattr(cbag, 'fcurves', [])
                    result.extend(fcs)
    return result


# Clips that must be in-place (no forward root motion).
# If downloaded from Mixamo without "In Place" checked, the pelvis bone
# translates forward in X/Z.  Strip that translation here as a safeguard.
IN_PLACE_CLIPS = {'walk', 'run'}

def strip_root_motion(action, root_bone='pelvis'):
    """Zero out X and Z translation on the root bone — converts to in-place."""
    fcurves = get_fcurves(action)
    stripped = 0
    for fc in fcurves:
        if f'pose.bones["{root_bone}"].location' in fc.data_path and fc.array_index in (0, 2):
            for kp in fc.keyframe_points:
                kp.co[1]           = 0.0
                kp.handle_left[1]  = 0.0
                kp.handle_right[1] = 0.0
            fc.update()
            stripped += 1
    return stripped


def retime_action(action, target_frames):
    """Scale all keyframe times in an action to fit target_frames."""
    fcurves = get_fcurves(action)
    if not fcurves:
        log(f"    WARNING: '{action.name}' has no fcurves — skipped")
        return 1.0
    all_times = [kp.co[0] for fc in fcurves for kp in fc.keyframe_points]
    if not all_times:
        log(f"    WARNING: '{action.name}' has no keyframes — skipped")
        return 1.0
    max_frame = max(all_times)
    if max_frame <= 0:
        return 1.0
    scale = target_frames / max_frame
    for fc in fcurves:
        for kp in fc.keyframe_points:
            kp.co[0]           = kp.co[0]          * scale
            kp.handle_left[0]  = kp.handle_left[0]  * scale
            kp.handle_right[0] = kp.handle_right[0] * scale
        fc.update()
    log(f"    '{action.name}': {max_frame:.1f} → {target_frames} frames  (×{scale:.3f})")
    return scale

retimed = set()
for track in main_arm.animation_data.nla_tracks:
    for strip in track.strips:
        clip = strip.name
        if clip not in CLIPS:
            continue
        if not strip.action:
            continue
        action_id = id(strip.action)
        if action_id not in retimed:
            scale = retime_action(strip.action, CLIPS[clip])
            retimed.add(action_id)
            log(f"    '{clip}' → {CLIPS[clip]} frames")
        tf = CLIPS[clip]
        strip.action_frame_start = 0
        strip.action_frame_end   = tf
        strip.frame_end          = strip.frame_start + tf

# Strip root motion from locomotion clips (in case downloaded without "In Place")
log("\n[4b] Stripping root motion from locomotion clips...")
for track in main_arm.animation_data.nla_tracks:
    for strip in track.strips:
        if strip.name in IN_PLACE_CLIPS and strip.action:
            n = strip_root_motion(strip.action)
            log(f"    '{strip.name}': stripped {n} root-motion fcurve(s)")

# ─────────────────────────────────────────────────────────────
# RENAME BONES
# ─────────────────────────────────────────────────────────────
log("\n[5] Renaming bones (mixamorig: → engine names)...")
bpy.context.view_layer.objects.active = main_arm
bpy.ops.object.mode_set(mode='EDIT')
renamed = 0
for old, new in BONE_RENAMES.items():
    if old in main_arm.data.edit_bones:
        main_arm.data.edit_bones[old].name = new
        renamed += 1
bpy.ops.object.mode_set(mode='OBJECT')
log(f"    Renamed {renamed} bones")

# Patch fcurve data_path strings in all actions so NLA clips still
# reference the correct (renamed) bones.  Blender does NOT auto-update
# actions that are only in the NLA stack (not the active action).
log("    Patching action fcurve paths...")
patched_paths = 0
for action in bpy.data.actions:
    fcurves = get_fcurves(action)
    for fc in fcurves:
        new_path = fc.data_path
        for old_name, new_name in BONE_RENAMES.items():
            old_token = f'pose.bones["{old_name}"]'
            new_token = f'pose.bones["{new_name}"]'
            if old_token in new_path:
                new_path = new_path.replace(old_token, new_token)
        if new_path != fc.data_path:
            fc.data_path = new_path
            patched_paths += 1
log(f"    Patched {patched_paths} fcurve paths")

# ─────────────────────────────────────────────────────────────
# ADD ANCHOR BONES
# ─────────────────────────────────────────────────────────────
log("\n[6] Adding equipment anchor bones...")
bpy.ops.object.mode_set(mode='EDIT')
eb = main_arm.data.edit_bones
for anchor_name, parent_name, h_off, t_off in ANCHORS:
    if anchor_name in eb:
        log(f"    '{anchor_name}' already exists — skipped")
        continue
    parent = eb.get(parent_name)
    if not parent:
        log(f"    WARNING: parent '{parent_name}' not found for '{anchor_name}'")
        continue
    b            = eb.new(anchor_name)
    b.head       = parent.tail + mathutils.Vector(h_off)
    b.tail       = b.head      + mathutils.Vector(t_off)
    b.parent     = parent
    b.use_deform = False
    log(f"    Added '{anchor_name}'")
bpy.ops.object.mode_set(mode='OBJECT')

# ─────────────────────────────────────────────────────────────
# EXPORT GLB
# ─────────────────────────────────────────────────────────────
log(f"\n[7] Exporting GLB → {GLB_OUT}")
os.makedirs(os.path.dirname(GLB_OUT), exist_ok=True)

bpy.ops.export_scene.gltf(
    filepath              = GLB_OUT,
    export_format         = 'GLB',
    use_selection         = False,
    export_apply          = True,
    export_animations     = True,
    export_nla_strips     = True,
    export_bake_animation = False,
    export_skins          = True,
    export_all_influences = False,
    export_morph          = False,
    export_lights         = False,
    export_cameras        = False,
)
log("    GLB written.")

# ─────────────────────────────────────────────────────────────
# SAVE .BLEND
# ─────────────────────────────────────────────────────────────
log(f"\n[8] Saving .blend → {BLEND_OUT}")
os.makedirs(os.path.dirname(BLEND_OUT), exist_ok=True)
bpy.ops.wm.save_as_mainfile(filepath=BLEND_OUT)
log("    .blend saved.")

# ─────────────────────────────────────────────────────────────
# SUMMARY
# ─────────────────────────────────────────────────────────────
log("\n" + "="*60)
log("FINAL NLA STRIPS:")
for track in main_arm.animation_data.nla_tracks:
    for strip in track.strips:
        log(f"  '{strip.name}' [{strip.frame_start:.0f}..{strip.frame_end:.0f}]")
log("="*60)
log("DONE — player_base.glb ready.")
