package io.repox.api3d.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.repox.api3d.app.usecases.storage.application.ports.out.FileStoragePort;
import io.repox.api3d.app.usecases.toolkit.adapters.out.TripoSRUnavailable;
import io.repox.api3d.app.usecases.toolkit.application.ImageTo3DService;
import io.repox.api3d.app.utils.exception.KnownRuntimeError;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class ImageTo3DServiceTests {

    @TempDir
    Path storageDir;

    private final FileStoragePort fileStorage = mock(FileStoragePort.class);

    private final ImageTo3DService service = new ImageTo3DService(fileStorage, new TripoSRUnavailable());

    @Test
    void answersNotImplementedWithoutWrappingTheStatus() throws IOException {
        File staged = Files.createFile(storageDir.resolve("staged.png")).toFile();
        when(fileStorage.getStorageDirectory()).thenReturn(storageDir.toFile());
        when(fileStorage.save(any(InputStream.class), anyString())).thenReturn(staged);

        MockMultipartFile image = new MockMultipartFile("file", "photo.png", "image/png", new byte[] {1, 2, 3});

        assertThatThrownBy(() -> service.convert(image))
                .isInstanceOf(KnownRuntimeError.class)
                .satisfies(e -> assertThat(((KnownRuntimeError) e).httpStatus()).isEqualTo(501));

        verify(fileStorage).delete(staged);
        try (var leftovers = Files.list(storageDir)) {
            assertThat(leftovers.filter(Files::isDirectory)).isEmpty();
        }
    }
}
