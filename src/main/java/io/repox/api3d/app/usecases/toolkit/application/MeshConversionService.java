package io.repox.api3d.app.usecases.toolkit.application;

import io.repox.api3d.app.usecases.storage.application.ports.out.FileStoragePort;
import io.repox.api3d.app.usecases.toolkit.application.ports.in.MeshConversionUseCase;
import io.repox.api3d.app.usecases.toolkit.application.ports.out.AssimpProcessPort;
import io.repox.api3d.app.usecases.toolkit.application.ports.out.BlenderProcessPort;
import io.repox.api3d.app.usecases.toolkit.domain.MeshExportFormat;
import io.repox.api3d.app.utils.exception.KnownRuntimeError;
import io.repox.api3d.appconfig.options.ConverterProperties;
import java.io.File;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Mesh format conversion routed by {@link MeshExportFormat}. Assimp-capable targets go through Assimp
 * first with an automatic Blender fallback ({@code assimp info} and {@code assimp export} share the same
 * importer, so files Assimp cannot read — e.g. Draco-compressed glTF/GLB, USDZ — also fail conversion;
 * Blender imports them fine). USD targets have no {@code assimpFormat} and go straight to Blender, which
 * Assimp cannot export.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeshConversionService implements MeshConversionUseCase {

    private final FileStoragePort fileStorage;
    private final AssimpProcessPort assimpProcessPort;
    private final BlenderProcessPort blenderProcessPort;
    private final ConverterProperties converterProperties;

    @Override
    public ConversionResult convertMesh(MultipartFile file, MeshExportFormat target) {
        try (InputStream inputStream = file.getInputStream()) {
            return convertMeshFromFile(fileStorage.save(inputStream, file.getOriginalFilename(), true), target);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public ConversionResult convertMeshFromUri(String url, MeshExportFormat target) {
        try {
            return convertMeshFromFile(fileStorage.saveFromUri(url, true), target);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public ConversionResult convertMeshFromFile(File input, MeshExportFormat target) throws Exception {
        try {
            File output = convertToTarget(input, target);
            return ConversionResult.forOutput(output, () -> fileStorage.delete(output));
        } finally {
            fileStorage.delete(input);
        }
    }

    private File convertToTarget(File input, MeshExportFormat target) throws Exception {
        // USD targets are Blender-only (Assimp cannot export USD).
        if (target.assimpFormat() == null) {
            return blenderProcessPort.convert(input, target.blenderFormat());
        }

        Exception failure;
        try {
            File output = assimpProcessPort.export(input, target.assimpFormat());
            if (isUsable(output)) {
                return output;
            }
            failure = new KnownRuntimeError("Assimp produced no usable output for: " + input.getName());
        } catch (Exception e) {
            failure = e;
        }

        log.warn("Assimp conversion unsuccessful for {} -> {}: {}", input.getName(), target, failure.getMessage());
        if (converterProperties.isMeshConvertBlenderFallback()) {
            log.info("Falling back to Blender for {} -> {}", input.getName(), target);
            return blenderProcessPort.convert(input, target.blenderFormat());
        }
        throw failure;
    }

    private boolean isUsable(File output) {
        return output != null && output.exists() && output.length() > 0;
    }
}
