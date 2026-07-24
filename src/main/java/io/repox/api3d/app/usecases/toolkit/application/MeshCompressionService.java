package io.repox.api3d.app.usecases.toolkit.application;

import io.repox.api3d.app.usecases.storage.application.ports.out.FileStoragePort;
import io.repox.api3d.app.usecases.toolkit.application.ports.in.MeshCompressionUseCase;
import io.repox.api3d.app.usecases.toolkit.application.ports.out.GltfInspectorPort;
import io.repox.api3d.app.usecases.toolkit.application.ports.out.MeshCompressionPort;
import io.repox.api3d.app.usecases.toolkit.domain.MeshCompressionOptions;
import io.repox.api3d.app.utils.exception.KnownRuntimeError;
import java.io.File;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeshCompressionService implements MeshCompressionUseCase {

    private final FileStoragePort fileStorage;
    private final MeshCompressionPort meshCompressionPort;
    private final GltfInspectorPort gltfInspector;

    @Override
    public ConversionResult compressMesh(MultipartFile file, MeshCompressionOptions options) {
        try (InputStream inputStream = file.getInputStream()) {
            return compressMeshFromFile(fileStorage.save(inputStream, file.getOriginalFilename(), true), options);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public ConversionResult compressMeshFromUri(String url, MeshCompressionOptions options) {
        try {
            return compressMeshFromFile(fileStorage.saveFromUri(url, true), options);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Compresses a glTF/GLB mesh with gltfpack, producing meshopt ({@code EXT_meshopt_compression}) output.
     *
     * <p>A <b>Draco-compressed input is rejected</b> with a {@link KnownRuntimeError} (→ HTTP 400): gltfpack
     * targets meshopt, and a Draco input is already geometry-compressed with a stronger scheme, so re-encoding
     * it to meshopt would only inflate the file. The input temp file is always cleaned up.
     */
    @Override
    public void verifyCompressible(File input) {
        // gltfpack targets meshopt (EXT_meshopt_compression). A Draco-compressed input is already
        // geometry-compressed with a stronger scheme; re-encoding it to meshopt only inflates it,
        // so we reject rather than produce a larger "compressed" file.
        if (gltfInspector.usesDraco(input)) {
            throw new KnownRuntimeError(DRACO_REJECTION_MESSAGE);
        }
    }

    @Override
    public ConversionResult compressMeshFromFile(File input, MeshCompressionOptions options) throws Exception {
        try {
            verifyCompressible(input);
            long inputBytes = input.length();
            File output = meshCompressionPort.compress(input, options);
            log.info(
                    "Compressed {} ({} B) -> {} ({} B), {}%",
                    input.getName(),
                    inputBytes,
                    output.getName(),
                    output.length(),
                    pctChange(inputBytes, output.length()));
            return ConversionResult.forOutput(output, () -> fileStorage.delete(output));
        } finally {
            fileStorage.delete(input);
        }
    }

    /**
     * Percent change from {@code inputBytes} to {@code outputBytes} (negative = smaller output), for logging.
     * Returns 0 when the input size is unknown/zero to avoid divide-by-zero.
     */
    private long pctChange(long inputBytes, long outputBytes) {
        if (inputBytes <= 0) {
            return 0;
        }
        return Math.round((outputBytes - inputBytes) * 100.0 / inputBytes);
    }
}
