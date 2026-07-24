import bpy
import sys

# ── parse args passed after "--" ──────────────────────────
# argv: <input_path> <output_path> <decimate_ratio|""> <texture_max_size|"">
argv = sys.argv
args = argv[argv.index("--") + 1:]
input_path = args[0]
output_path = args[1]
decimate_ratio = args[2] if len(args) > 2 else ""
texture_max = args[3] if len(args) > 3 else ""

# ── reset scene and import the usdz ───────────────────────
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.wm.usd_import(filepath=input_path)

# ── decimate meshes ───────────────────────────────────────
if decimate_ratio:
    ratio = float(decimate_ratio)
    if 0.0 < ratio < 1.0:
        for obj in [o for o in bpy.context.scene.objects if o.type == "MESH"]:
            mod = obj.modifiers.new(name="usdz_decimate", type="DECIMATE")
            mod.ratio = ratio
            with bpy.context.temp_override(object=obj, active_object=obj, selected_objects=[obj]):
                bpy.ops.object.modifier_apply(modifier=mod.name)

# ── downscale oversized textures ──────────────────────────
if texture_max:
    max_size = int(texture_max)
    for img in bpy.data.images:
        w, h = img.size
        if w <= 0 or h <= 0:
            continue
        longest = max(w, h)
        if longest > max_size:
            scale = max_size / longest
            img.scale(max(1, int(w * scale)), max(1, int(h * scale)))
            # Pack the scaled pixels so the USD export writes the reduced texture, not the original file.
            try:
                img.pack()
            except RuntimeError as e:
                print(f"[usdz_optimize] could not pack image {img.name}: {e}", file=sys.stderr)

# ── re-export as usdz (Blender packages by extension) ─────
bpy.ops.wm.usd_export(filepath=output_path)
print(f"[usdz_optimize] optimized {input_path} -> {output_path} (decimate={decimate_ratio}, texture_max={texture_max})")
