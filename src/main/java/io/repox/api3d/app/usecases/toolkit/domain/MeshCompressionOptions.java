package io.repox.api3d.app.usecases.toolkit.domain;

/**
 * Options for gltfpack mesh compression.
 *
 * @param compressTextures convert textures to KTX2/Basis supercompression ({@code -tc})
 * @param aggressive use the more aggressive meshopt compression level ({@code -cc}) instead of {@code -c}
 * @param simplifyRatio when non-null, simplify the mesh targeting this triangle-count ratio in (0, 1]
 *     ({@code -si}); {@code null} disables simplification
 */
public record MeshCompressionOptions(boolean compressTextures, boolean aggressive, Float simplifyRatio) {

    public static MeshCompressionOptions defaults() {
        return new MeshCompressionOptions(true, false, null);
    }
}
