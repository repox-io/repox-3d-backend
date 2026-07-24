package io.repox.api3d.app.usecases.toolkit.application;

import io.repox.api3d.app.usecases.storage.application.ports.out.FileStoragePort;
import io.repox.api3d.app.usecases.toolkit.application.ports.in.ModelThumbnailUseCase;
import io.repox.api3d.app.usecases.toolkit.application.ports.out.BlenderProcessPort;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelThumbnailService implements ModelThumbnailUseCase {

    private final FileStoragePort fileStorage;
    private final BlenderProcessPort blenderProcessPort;

    @Override
    public ConversionResult generateThumbnailFromFile(File input) throws Exception {
        try {
            File output = blenderProcessPort.thumbnail(input);
            return ConversionResult.forOutput(output, () -> fileStorage.delete(output));
        } finally {
            fileStorage.delete(input);
        }
    }

    /**
     * Renders and reads the PNG back for the synchronous callers, closing the result so the output file is
     * deleted once its bytes are in hand — the job path instead retains it for the retention sweep.
     */
    private byte[] renderThumbnail(File modelFile) throws Exception {
        try (ConversionResult result = generateThumbnailFromFile(modelFile)) {
            return Files.readAllBytes(result.file().toPath());
        }
    }

    @Override
    public byte[] generateThumbnail(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            File modelFile = fileStorage.save(inputStream, file.getOriginalFilename(), true);
            return renderThumbnail(modelFile);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] generateThumbnailFromUri(String url) {
        try {
            File modelFile = fileStorage.saveFromUri(url, true);
            return renderThumbnail(modelFile);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
