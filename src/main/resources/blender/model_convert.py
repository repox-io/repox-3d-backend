import bpy
import sys
import os

# ── parse args passed after "--" ──────────────────────────
# argv: <input_path> <output_path> <target_token>
argv = sys.argv
args = argv[argv.index("--") + 1:]
input_path = args[0]
output_path = args[1]
target = args[2]

# ── reset to empty scene ──────────────────────────────────
bpy.ops.wm.read_factory_settings(use_empty=True)

# ── import based on extension (mirrors model_info.py) ─────
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
    print(f"[model_convert] unsupported input format: {ext}", file=sys.stderr)
    sys.exit(1)

importers[ext]()

# ── export based on the requested target token ────────────
exporters = {
    "glb":  lambda: bpy.ops.export_scene.gltf(filepath=output_path, export_format="GLB"),
    "gltf": lambda: bpy.ops.export_scene.gltf(filepath=output_path, export_format="GLTF_SEPARATE"),
    "obj":  lambda: bpy.ops.wm.obj_export(filepath=output_path),       # 4.x
    "stl":  lambda: bpy.ops.wm.stl_export(filepath=output_path),       # 4.x
    "ply":  lambda: bpy.ops.wm.ply_export(filepath=output_path),       # 4.x
    "fbx":  lambda: bpy.ops.export_scene.fbx(filepath=output_path),
    "dae":  lambda: bpy.ops.wm.collada_export(filepath=output_path),
    "usd":  lambda: bpy.ops.wm.usd_export(filepath=output_path),          # Blender packages by extension
    "usda": lambda: bpy.ops.wm.usd_export(filepath=output_path),
    "usdc": lambda: bpy.ops.wm.usd_export(filepath=output_path),
    "usdz": lambda: bpy.ops.wm.usd_export(filepath=output_path),
}

if target not in exporters:
    print(f"[model_convert] unsupported target format: {target}", file=sys.stderr)
    sys.exit(1)

exporters[target]()
print(f"[model_convert] exported {input_path} -> {output_path} ({target})")
