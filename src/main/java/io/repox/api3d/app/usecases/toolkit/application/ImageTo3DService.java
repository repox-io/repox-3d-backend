package io.repox.api3d.app.usecases.toolkit.application;

import io.repox.api3d.app.usecases.storage.application.ports.out.FileStoragePort;
import io.repox.api3d.app.usecases.toolkit.application.ports.in.ImageTo3DUseCase;
import io.repox.api3d.app.usecases.toolkit.application.ports.out.TripoSRProcessPort;
import io.repox.api3d.app.utils.exception.KnownRuntimeError;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageTo3DService implements ImageTo3DUseCase {

    private final FileStoragePort fileStorage;
    private final TripoSRProcessPort tripoSRProcessPort;

    @Override
    public byte[] convert(MultipartFile file) {
        String jobId = UUID.randomUUID().toString();
        File outputDir = Path.of(fileStorage.getStorageDirectory().getAbsolutePath(), jobId)
                .toFile();
        File imageFile = null;
        File glbFile = null;
        try (InputStream inputStream = file.getInputStream()) {
            imageFile = fileStorage.save(inputStream, jobId + "_" + file.getOriginalFilename());
            Files.createDirectories(outputDir.toPath());
            glbFile = tripoSRProcessPort.convert(imageFile, outputDir);
            return Files.readAllBytes(glbFile.toPath());
        } catch (KnownRuntimeError e) {
            // Keeps its status (e.g. 501 while TripoSR is unavailable) instead of collapsing to 500 below.
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            if (imageFile != null) fileStorage.delete(imageFile);
            deleteDirectoryQuietly(outputDir);
        }
    }

    private void deleteDirectoryQuietly(File dir) {
        if (dir == null || !dir.exists()) return;
        try (var stream = Files.walk(dir.toPath())) {
            stream.sorted(java.util.Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        } catch (Exception e) {
            log.warn("Failed to clean up TripoSR output directory: {}", dir.getAbsolutePath());
        }
    }
}
