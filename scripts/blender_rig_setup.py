"""
Erynfall player rig setup script — run via Blender background mode:

  /Applications/Blender.app/Contents/MacOS/Blender \
      --background /path/to/player_base.blend \
      --python scripts/blender_rig_setup.py

Does everything in one pass:
  1. Finds the main armature (most NLA tracks)
  2. Renames wrong NLA strip names (fish_idle → fish, cooking → cook)
  3. Retimes all action keyframes to OSRS tick lengths at 24 fps
  4. Renames mixamorig: bones to engine names
  5. Adds non-deforming equipment anchor bones
  6. Exports player_base.glb to art/models/
  7. Saves the .blend file
"""

import bpy
import sys
import os
import mathutils

# ── Resolve output paths relative to this script ─────────────
SCRIPT_DIR  = os.path.dirname(os.path.abspath(__file__))
REPO_ROOT   = os.path.dirname(SCRIPT_DIR)
GLB_OUT     = os.path.join(REPO_ROOT, "art", "models", "player_base.glb")
BLEND_OUT   = bpy.data.filepath  # save back to same file

print("\n" + "="*60)
print("ERYNFALL PLAYER RIG SETUP")
print(f"  GLB output : {GLB_OUT}")
print(f"  Blend file : {BLEND_OUT}")
print("="*60)

# ─────────────────────────────────────────────────────────────
# 1. FIND MAIN ARMATURE
# ─────────────────────────────────────────────────────────────
def find_main_armature():
    candidates = [
        o for o in bpy.data.objects
        if o.type == 'ARMATURE'
        and o.animation_data
        and o.animation_data.nla_tracks
    ]
    if not candidates:
        sys.exit("ERROR: No armature with NLA tracks found. "
                 "Make sure you saved AFTER pushing all clips to NLA.")
    return max(candidates, key=lambda o: len(o.animation_data.nla_tracks))

arm = find_main_armature()
print(f"\n[1] Armature: '{arm.name}'  "
      f"nla_tracks={len(arm.animation_data.nla_tracks)}")

# ─────────────────────────────────────────────────────────────
# 2. RENAME WRONG NLA STRIP NAMES
# ─────────────────────────────────────────────────────────────
STRIP_RENAMES = {
    'fish_idle': 'fish',
    'cooking':   'cook',
}

print("\n[2] Renaming NLA strips...")
for track in arm.animation_data.nla_tracks:
    for strip in track.strips:
        if strip.name in STRIP_RENAMES:
            new = STRIP_RENAMES[strip.name]
            print(f"    '{strip.name}' → '{new}'")
            strip.name = new

# ─────────────────────────────────────────────────────────────
# 3. RETIME ACTIONS TO OSRS TICK LENGTHS
#    1 OSRS tick = 600 ms = 14.4 frames at 24 fps
# ─────────────────────────────────────────────────────────────
OSRS_FRAMES = {
    'idle':             58,   # 4 ticks (57.6)
    'walk':             29,   # 2 ticks (28.8)
    'run':              14,   # 1 tick  (14.4)
    'pickup':           29,
    'chop':             14,
    'mine':             14,
    'fish':             43,   # 3 ticks (43.2)
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

def retime_action(action, target_frames):
    """Scale all keyframe times in an action to fit target_frames."""
    all_times = [
        kp.co[0]
        for fc in action.fcurves
        for kp in fc.keyframe_points
    ]
    if not all_times:
        print(f"    WARNING: '{action.name}' has no keyframes — skipped")
        return
    max_frame = max(all_times)
    if max_frame <= 0:
        return
    scale = target_frames / max_frame
    for fc in action.fcurves:
        for kp in fc.keyframe_points:
            kp.co[0]           = kp.co[0]          * scale
            kp.handle_left[0]  = kp.handle_left[0]  * scale
            kp.handle_right[0] = kp.handle_right[0] * scale
        fc.update()
    print(f"    '{action.name}': {max_frame:.1f} → {target_frames} frames  "
          f"(×{scale:.3f})")

print("\n[3] Retiming actions to OSRS tick lengths...")
retimed = set()
for track in arm.animation_data.nla_tracks:
    for strip in track.strips:
        clip = strip.name
        if clip not in OSRS_FRAMES:
            print(f"    WARNING: unknown clip '{clip}' — not retimed")
            continue
        if not strip.action:
            print(f"    WARNING: strip '{clip}' has no action — skipped")
            continue
        action_id = strip.action.name
        if action_id not in retimed:          # each action only scaled once
            retime_action(strip.action, OSRS_FRAMES[clip])
            retimed.add(action_id)
        tf = OSRS_FRAMES[clip]
        strip.action_frame_start = 0
        strip.action_frame_end   = tf
        strip.frame_end          = strip.frame_start + tf

# ─────────────────────────────────────────────────────────────
# 4. RENAME BONES  (mixamorig: → engine names)
#    Blender auto-updates vertex groups + action fcurve paths
# ─────────────────────────────────────────────────────────────
BONE_RENAMES = {
    'mixamorig:Hips':                'pelvis',
    'mixamorig:Spine':               'spine',
    'mixamorig:Spine1':              'chest',
    'mixamorig:Spine2':              'chest_upper',
    'mixamorig:Neck':                'neck',
    'mixamorig:Head':                'head',
    'mixamorig:LeftShoulder':        'shoulder_l',
    'mixamorig:RightShoulder':       'shoulder_r',
    'mixamorig:LeftArm':             'upper_arm_l',
    'mixamorig:RightArm':            'upper_arm_r',
    'mixamorig:LeftForeArm':         'lower_arm_l',
    'mixamorig:RightForeArm':        'lower_arm_r',
    'mixamorig:LeftHand':            'hand_l',
    'mixamorig:RightHand':           'hand_r',
    'mixamorig:LeftUpLeg':           'upper_leg_l',
    'mixamorig:RightUpLeg':          'upper_leg_r',
    'mixamorig:LeftLeg':             'lower_leg_l',
    'mixamorig:RightLeg':            'lower_leg_r',
    'mixamorig:LeftFoot':            'foot_l',
    'mixamorig:RightFoot':           'foot_r',
    'mixamorig:LeftToeBase':         'toe_l',
    'mixamorig:RightToeBase':        'toe_r',
    'mixamorig:LeftHandIndex1':      'finger_l_1',
    'mixamorig:LeftHandIndex2':      'finger_l_2',
    'mixamorig:LeftHandIndex3':      'finger_l_3',
    'mixamorig:RightHandIndex1':     'finger_r_1',
    'mixamorig:RightHandIndex2':     'finger_r_2',
    'mixamorig:RightHandIndex3':     'finger_r_3',
    # end/leaf bones
    'mixamorig:HeadTop_End':         'head_top',
    'mixamorig:LeftToeBase_end':     'toe_l_end',
    'mixamorig:RightToeBase_end':    'toe_r_end',
}

print("\n[4] Renaming bones...")
bpy.context.view_layer.objects.active = arm
bpy.ops.object.mode_set(mode='EDIT')
renamed = 0
skipped = 0
for old, new in BONE_RENAMES.items():
    if old in arm.data.edit_bones:
        arm.data.edit_bones[old].name = new
        renamed += 1
    else:
        skipped += 1
bpy.ops.object.mode_set(mode='OBJECT')
print(f"    Renamed {renamed} bones  ({skipped} not found — normal if rig varies)")

# ─────────────────────────────────────────────────────────────
# 5. ADD EQUIPMENT ANCHOR BONES  (non-deforming)
# ─────────────────────────────────────────────────────────────
ANCHORS = [
    # (anchor_name,    parent_name,  head_offset,          tail_offset)
    ('weapon_anchor',  'hand_r',     (0,  0,     0   ),    (0,  0.08,  0   )),
    ('shield_anchor',  'hand_l',     (0,  0,     0   ),    (0,  0.08,  0   )),
    ('head_anchor',    'head',       (0,  0,     0   ),    (0,  0,     0.12)),
    ('cape_anchor',    'chest',      (0, -0.05,  0   ),    (0, -0.15,  0   )),
    ('ammo_anchor',    'pelvis',     (0.08, 0,   0   ),    (0.18, 0,   0   )),
]

print("\n[5] Adding anchor bones...")
bpy.ops.object.mode_set(mode='EDIT')
eb = arm.data.edit_bones
for anchor_name, parent_name, h_off, t_off in ANCHORS:
    if anchor_name in eb:
        print(f"    '{anchor_name}' already exists — skipped")
        continue
    parent = eb.get(parent_name)
    if parent is None:
        print(f"    WARNING: parent '{parent_name}' not found for '{anchor_name}'")
        continue
    b        = eb.new(anchor_name)
    b.head   = parent.tail + mathutils.Vector(h_off)
    b.tail   = b.head      + mathutils.Vector(t_off)
    b.parent = parent
    b.use_deform = False
    print(f"    Added '{anchor_name}' → parent '{parent_name}'")
bpy.ops.object.mode_set(mode='OBJECT')

# ─────────────────────────────────────────────────────────────
# 6. EXPORT GLB
# ─────────────────────────────────────────────────────────────
print(f"\n[6] Exporting GLB → {GLB_OUT}")
os.makedirs(os.path.dirname(GLB_OUT), exist_ok=True)

bpy.ops.export_scene.gltf(
    filepath              = GLB_OUT,
    export_format         = 'GLB',
    use_selection         = False,
    export_apply          = True,           # apply modifiers
    export_animations     = True,
    export_nla_strips     = True,           # named NLA strips → named clips
    export_anim_slide     = False,
    export_bake_animation = False,
    export_skins          = True,           # skinned animation ON
    export_all_influences = False,          # 1 bone influence per vertex
    export_morph          = False,
    export_lights         = False,
    export_cameras        = False,
    export_extras         = False,
)
print("    GLB written.")

# ─────────────────────────────────────────────────────────────
# 7. SAVE .BLEND
# ─────────────────────────────────────────────────────────────
if BLEND_OUT:
    print(f"\n[7] Saving .blend → {BLEND_OUT}")
    bpy.ops.wm.save_mainfile()
else:
    print("\n[7] .blend not saved (file was Untitled — save manually with Ctrl+S)")

# ─────────────────────────────────────────────────────────────
# SUMMARY
# ─────────────────────────────────────────────────────────────
print("\n" + "="*60)
print("FINAL NLA STRIPS:")
for track in arm.animation_data.nla_tracks:
    for strip in track.strips:
        print(f"  '{strip.name}' [{strip.frame_start:.0f}..{strip.frame_end:.0f}]")
print("="*60)
print("DONE.")
