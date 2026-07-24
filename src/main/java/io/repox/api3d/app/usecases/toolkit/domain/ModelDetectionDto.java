package io.repox.api3d.app.usecases.toolkit.domain;

import java.util.List;

/**
 * Result of a lightweight {@code /tools/detect} classification: the model {@link ModelKind kind}, the
 * detected source format (e.g. {@code "PLY"}, {@code "SPZ"}, {@code "GLB"}), and the formats the input
 * can be converted to for that kind ({@link SplatFormat} names for splats, {@link MeshExportFormat}
 * names for meshes).
 */
public record ModelDetectionDto(ModelKind kind, String sourceFormat, List<String> targetFormats) {}
