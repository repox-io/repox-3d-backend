package io.repox.api3d.app;

import static org.assertj.core.api.Assertions.assertThat;

import io.repox.api3d.app.usecases.storage.application.TempFileStorageService;
import io.repox.api3d.app.usecases.storage.domain.FileStorageSettings;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TempFileStorageServiceTests {

    private TempFileStorageService service(Path dir) {
        return new TempFileStorageService(new FileStorageSettings(dir.toString()));
    }

    @Test
    void saveWritesToInputDirectoryWithUniqueSegment(@TempDir Path dir) throws IOException {
        File saved =
                service(dir).save(new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)), "model.glb", true);

        assertThat(saved.getName()).endsWith("_model.glb");
        assertThat(saved.getParentFile().getName()).isEqualTo("input");
        assertThat(saved).exists();
    }

    @Test
    void cleanupInputDirectoryEmptiesInputButPreservesOutput(@TempDir Path dir) throws IOException {
        TempFileStorageService service = service(dir);
        File input = service.save(new ByteArrayInputStream("a".getBytes(StandardCharsets.UTF_8)), "model.glb", true);
        File output = new File(service.getOutputDirectory(), "model_converted_20260706_143022.glb");
        Files.createFile(output.toPath());

        service.cleanupInputDirectory();

        assertThat(input).doesNotExist(); // input leftover -> swept
        assertThat(output).exists(); // produced output -> preserved
    }
}
