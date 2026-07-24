import bpy
import sys
import os

# ── parse args passed after "--" ──────────────────────────
argv = sys.argv
args = argv[argv.index("--") + 1:]
input_path = args[0]

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
    ".usdz": lambda: bpy.ops.wm.usd_import(filepath=input_path),
}

if ext not in importers:
    print(f"[model_info] unsupported format: {ext}", file=sys.stderr)
    sys.exit(1)

importers[ext]()

# ── collect metrics from the imported scene ───────────────
meshes = bpy.data.meshes
armatures = bpy.data.armatures
actions = bpy.data.actions

vertices = sum(len(m.vertices) for m in meshes)
faces = sum(len(m.polygons) for m in meshes)
bones = sum(len(a.bones) for a in armatures)
animation_channels = sum(len(a.fcurves) for a in actions)

metrics = {
    "nodes": len(bpy.data.objects),
    "meshes": len(meshes),
    "animations": len(actions),
    "textures": len(bpy.data.images),
    "materials": len(bpy.data.materials),
    "cameras": len(bpy.data.cameras),
    "lights": len(bpy.data.lights),
    "vertices": vertices,
    "faces": faces,
    "bones": bones,
    "animationChannels": animation_channels,
}

# memoryConsumptionB has no Blender equivalent — intentionally omitted (stays null)
for key, value in metrics.items():
    print(f"MODELINFO:{key}={value}")
