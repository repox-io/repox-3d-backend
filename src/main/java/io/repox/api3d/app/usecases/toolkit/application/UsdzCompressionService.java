package io.repox.api3d.app.usecases.toolkit.application;

import io.repox.api3d.app.usecases.storage.application.ports.out.FileStoragePort;
import io.repox.api3d.app.usecases.toolkit.application.ports.in.UsdzCompressionUseCase;
import io.repox.api3d.app.usecases.toolkit.application.ports.out.BlenderProcessPort;
import io.repox.api3d.app.usecases.toolkit.domain.UsdzOptimizeOptions;
import java.io.File;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * USDZ "compression" via Blender optimization (mesh decimation + texture downscaling → re-export usdz).
 * The only self-hostable lever for USDZ — there is no gltfpack-equivalent CLI for USD.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsdzCompressionService implements UsdzCompressionUseCase {

    private final FileStoragePort fileStorage;
    private final BlenderProcessPort blenderProcessPort;

    @Override
    public ConversionResult compressUsdz(MultipartFile file, UsdzOptimizeOptions options) {
        try (InputStream inputStream = file.getInputStream()) {
            return compressUsdzFromFile(fileStorage.save(inputStream, file.getOriginalFilename(), true), options);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public ConversionResult compressUsdzFromUri(String url, UsdzOptimizeOptions options) {
        try {
            return compressUsdzFromFile(fileStorage.saveFromUri(url, true), options);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public ConversionResult compressUsdzFromFile(File input, UsdzOptimizeOptions options) throws Exception {
        try {
            File output = blenderProcessPort.optimizeUsdz(input, options);
            return ConversionResult.forOutput(output, () -> fileStorage.delete(output));
        } finally {
            fileStorage.delete(input);
        }
    }
}
