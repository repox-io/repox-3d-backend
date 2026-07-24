package io.repox.api3d.app.usecases.toolkit.domain;

/**
 * Formats Blender can export to (via {@code model_convert.py}). The {@code token} is passed to the
 * Python script to select the exporter; the {@code extension} names the output file (Blender packages
 * {@code .usdz} by extension).
 */
public enum BlenderExportFormat {
    GLB("glb", ".glb"),
    GLTF("gltf", ".gltf"),
    OBJ("obj", ".obj"),
    STL("stl", ".stl"),
    PLY("ply", ".ply"),
    FBX("fbx", ".fbx"),
    DAE("dae", ".dae"),
    USD("usd", ".usd"),
    USDA("usda", ".usda"),
    USDC("usdc", ".usdc"),
    USDZ("usdz", ".usdz");

    private final String token;
    private final String extension;

    BlenderExportFormat(String token, String extension) {
        this.token = token;
        this.extension = extension;
    }

    public String token() {
        return token;
    }

    public String extension() {
        return extension;
    }
}
