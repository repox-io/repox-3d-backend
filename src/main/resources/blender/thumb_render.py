import bpy
import sys
import os
import math
import mathutils

# ── parse args passed after "--" ──────────────────────────
argv = sys.argv
args = argv[argv.index("--") + 1:]
input_path  = args[0]
output_path = args[1]
width       = int(args[2]) if len(args) > 2 else 512
height      = int(args[3]) if len(args) > 3 else 512

# ── reset to empty scene ──────────────────────────────────
bpy.ops.wm.read_factory_settings(use_empty=True)

# ── import based on extension ─────────────────────────────
ext = os.path.splitext(input_path)[1].lower()

importers = {
    ".glb":  lambda: bpy.ops.import_scene.gltf(filepath=input_path),
    ".gltf": lambda: bpy.ops.import_scene.gltf(filepath=input_path),
    ".obj":  lambda: bpy.ops.wm.obj_import(filepath=input_path),       # 4.x
    ".fbx":  lambda: bpy.ops.import_scene.fbx(filepath=input_path),
    ".dae":  lambda: bpy.ops.wm.collada_import(filepath=input_path),
    ".stl":  lambda: bpy.ops.wm.stl_import(filepath=input_path),       # 4.x
    ".ply":  lambda: bpy.ops.wm.ply_import(filepath=input_path),       # 4.x
    ".usd":  lambda: bpy.ops.wm.usd_import(filepath=input_path),
    ".usdc": lambda: bpy.ops.wm.usd_import(filepath=input_path),
    ".usda": lambda: bpy.ops.wm.usd_import(filepath=input_path),
}

if ext not in importers:
    print(f"[thumb_render] unsupported format: {ext}", file=sys.stderr)
    sys.exit(1)

importers[ext]()

# ── PLY fix: assign a default material if none exists ─────
# PLY files carry no material data — without this the mesh
# renders black / invisible in EEVEE
for obj in bpy.context.scene.objects:
    if obj.type == "MESH" and len(obj.data.materials) == 0:
        mat = bpy.data.materials.new(name="DefaultMat")
        mat.use_nodes = True
        bsdf = mat.node_tree.nodes.get("Principled BSDF")
        if bsdf:
            bsdf.inputs["Base Color"].default_value = (0.6, 0.6, 0.6, 1.0)
            bsdf.inputs["Roughness"].default_value  = 0.5
            bsdf.inputs["Metallic"].default_value   = 0.0
        obj.data.materials.append(mat)

# ── compute bounding box of all imported objects ──────────
def get_world_bbox(obj):
    return [obj.matrix_world @ mathutils.Vector(c) for c in obj.bound_box]

all_corners = []
for obj in bpy.context.scene.objects:
    if obj.type == "MESH":
        all_corners.extend(get_world_bbox(obj))

if not all_corners:
    print("[thumb_render] no meshes found", file=sys.stderr)
    sys.exit(1)

xs = [v.x for v in all_corners]
ys = [v.y for v in all_corners]
zs = [v.z for v in all_corners]

center = mathutils.Vector((
    (min(xs) + max(xs)) / 2,
    (min(ys) + max(ys)) / 2,
    (min(zs) + max(zs)) / 2,
))
max_dim = max(max(xs)-min(xs), max(ys)-min(ys), max(zs)-min(zs))

# ── camera ────────────────────────────────────────────────
bpy.ops.object.camera_add()
cam = bpy.context.object
bpy.context.scene.camera = cam

# 3/4 view angle — classic product thumbnail position
distance = max_dim * 2.0
cam.location = center + mathutils.Vector((distance, -distance * 1.2, distance * 0.8))

# Point camera at bounding box center
direction = center - cam.location
rot = direction.to_track_quat("-Z", "Y")
cam.rotation_euler = rot.to_euler()

cam.data.lens     = 50           # 50mm — minimal distortion
cam.data.clip_end = max_dim * 100

# ── 3-point lighting ──────────────────────────────────────
def add_light(name, light_type, location, energy, color=(1, 1, 1)):
    bpy.ops.object.light_add(type=light_type, location=location)
    light = bpy.context.object
    light.name = name
    light.data.energy = energy
    light.data.color  = color
    return light

r = max_dim * 3

# Key light — warm, main illumination
add_light("Key",  "SUN", center + mathutils.Vector(( r,  -r,  r * 1.5)), energy=3.0, color=(1.0, 0.95, 0.85))
# Fill light — cool, soft opposite side
add_light("Fill", "SUN", center + mathutils.Vector((-r,   r,  r * 0.5)), energy=1.2, color=(0.8, 0.9,  1.0))
# Rim light — bright backlight for silhouette separation
add_light("Rim",  "SUN", center + mathutils.Vector(( 0,   r, -r * 0.5)), energy=1.8, color=(1.0, 1.0,  1.0))

# ── world background ──────────────────────────────────────
world = bpy.data.worlds.new("World")
bpy.context.scene.world = world
world.use_nodes = True
bg = world.node_tree.nodes["Background"]
bg.inputs["Color"].default_value    = (0.12, 0.12, 0.15, 1.0)  # dark blue-grey
bg.inputs["Strength"].default_value = 0.5

# ── render settings — EEVEE, headless CPU ────────────────
scene = bpy.context.scene
scene.render.engine             = "BLENDER_EEVEE"  # reliable headless, no GPU needed
scene.render.resolution_x       = width
scene.render.resolution_y       = height
scene.render.resolution_percentage = 100
scene.render.image_settings.file_format = "PNG"
scene.render.filepath           = output_path

# EEVEE quality settings
eevee = scene.eevee
eevee.taa_render_samples        = 64    # anti-aliasing samples
eevee.use_ssr                   = False # screen-space reflections off (speed)
eevee.use_bloom                 = False

# ── render ────────────────────────────────────────────────
bpy.ops.render.render(write_still=True)
print(f"[thumb_render] written to {output_path}")
