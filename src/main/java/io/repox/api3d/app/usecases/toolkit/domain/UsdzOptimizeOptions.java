package io.repox.api3d.app.usecases.toolkit.domain;

/**
 * Options for Blender-based USDZ optimization (the only self-hostable "compression" lever for USDZ —
 * there is no gltfpack-equivalent; gains come from mesh decimation and texture downscaling).
 *
 * @param decimateRatio target face-count ratio in (0, 1] applied via a Decimate modifier; {@code null}
 *     skips decimation
 * @param textureMaxSize max texture edge in pixels (e.g. 2048/1024); textures larger than this are
 *     downscaled; {@code null} leaves textures untouched
 */
public record UsdzOptimizeOptions(Float decimateRatio, Integer textureMaxSize) {}
