package io.repox.api3d.app.usecases.toolkit.application.ports.in;

import io.repox.api3d.app.usecases.toolkit.application.ConversionResult;
import io.repox.api3d.app.usecases.toolkit.domain.MeshCompressionOptions;
import java.io.File;
import org.springframework.web.multipart.MultipartFile;

public interface MeshCompressionUseCase {

    /**
     * Rejection message for an input that gltfpack cannot meaningfully compress to meshopt (currently: an
     * already Draco-compressed model, which would only inflate). Shared so the pre-submit upload check and
     * the worker-side guard speak with one voice.
     */
    String DRACO_REJECTION_MESSAGE = "Input is already Draco-compressed (KHR_draco_mesh_compression); this"
            + " endpoint produces meshopt output, which would be larger. Submit an uncompressed or"
            + " meshopt-encoded glTF/GLB instead.";

    ConversionResult compressMesh(MultipartFile file, MeshCompressionOptions options);

    ConversionResult compressMeshFromUri(String url, MeshCompressionOptions options);

    /** Compresses an already-stored input file. The caller owns the returned output file's lifecycle. */
    ConversionResult compressMeshFromFile(File input, MeshCompressionOptions options) throws Exception;

    /**
     * Fails fast (throws {@code KnownRuntimeError} → HTTP 400) if the given file cannot be compressed to
     * meshopt — currently when it is already Draco-compressed. Lets callers reject a doomed request before
     * enqueuing an async job. Content-based, so it only applies to inputs already on disk (uploads); a
     * clean, cheap header read.
     */
    void verifyCompressible(File input);
}
