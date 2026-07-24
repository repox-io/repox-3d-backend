package io.repox.api3d.app.usecases.toolkit.domain;

/**
 * Target formats offered by {@code /tools/convert/mesh}. Each value carries routing metadata:
 *
 * <ul>
 *   <li>{@code assimpFormat} — the Assimp export format to try first, or {@code null} for Blender-only
 *       targets (USD family, which Assimp cannot export).
 *   <li>{@code blenderFormat} — the Blender exporter used as the fallback (Assimp-capable formats) or the
 *       primary engine (USD family).
 * </ul>
 *
 * Deliberately curated to the mainstream mesh formats plus the USD family — Assimp's niche/debug export
 * tokens (assbin, assxml, assjson, pbrt, x, stp, 3ds, x3d, 3mf) are intentionally not exposed.
 */
public enum MeshExportFormat {
    GLB(AssimpExportFormatEnum.GLB2, BlenderExportFormat.GLB),
    GLTF(AssimpExportFormatEnum.GLTF2, BlenderExportFormat.GLTF),
    OBJ(AssimpExportFormatEnum.OBJ, BlenderExportFormat.OBJ),
    STL(AssimpExportFormatEnum.STL, BlenderExportFormat.STL),
    PLY(AssimpExportFormatEnum.PLY, BlenderExportFormat.PLY),
    FBX(AssimpExportFormatEnum.FBX, BlenderExportFormat.FBX),
    DAE(AssimpExportFormatEnum.COLLADA, BlenderExportFormat.DAE),
    USD(null, BlenderExportFormat.USD),
    USDA(null, BlenderExportFormat.USDA),
    USDC(null, BlenderExportFormat.USDC),
    USDZ(null, BlenderExportFormat.USDZ);

    private final AssimpExportFormatEnum assimpFormat;
    private final BlenderExportFormat blenderFormat;

    MeshExportFormat(AssimpExportFormatEnum assimpFormat, BlenderExportFormat blenderFormat) {
        this.assimpFormat = assimpFormat;
        this.blenderFormat = blenderFormat;
    }

    /** The Assimp export format to attempt first, or {@code null} for Blender-only (USD) targets. */
    public AssimpExportFormatEnum assimpFormat() {
        return assimpFormat;
    }

    /** The Blender exporter for this target (fallback for Assimp formats, primary for USD). */
    public BlenderExportFormat blenderFormat() {
        return blenderFormat;
    }
}
