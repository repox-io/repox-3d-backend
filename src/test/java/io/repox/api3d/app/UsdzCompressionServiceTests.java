package io.repox.api3d.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.repox.api3d.app.usecases.storage.application.ports.out.FileStoragePort;
import io.repox.api3d.app.usecases.toolkit.application.ConversionResult;
import io.repox.api3d.app.usecases.toolkit.application.UsdzCompressionService;
import io.repox.api3d.app.usecases.toolkit.application.ports.out.BlenderProcessPort;
import io.repox.api3d.app.usecases.toolkit.domain.UsdzOptimizeOptions;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;

class UsdzCompressionServiceTests {

    private final FileStoragePort fileStorage = mock(FileStoragePort.class);
    private final BlenderProcessPort blenderProcessPort = mock(BlenderProcessPort.class);

    private final UsdzCompressionService service = new UsdzCompressionService(fileStorage, blenderProcessPort);

    private File tempFile(String suffix) throws IOException {
        File f = Files.createTempFile("usdz-compress-test", suffix).toFile();
        f.deleteOnExit();
        return f;
    }

    @Test
    void invokesBlenderOptimizeAndReturnsOutput() throws Exception {
        File input = tempFile(".usdz");
        File output = tempFile(".usdz");
        UsdzOptimizeOptions options = new UsdzOptimizeOptions(0.5f, 1024);
        when(fileStorage.saveFromUri(anyString(), anyBoolean())).thenReturn(input);
        when(blenderProcessPort.optimizeUsdz(eq(input), any())).thenReturn(output);

        ConversionResult result = service.compressUsdzFromUri("http://example/model.usdz", options);

        assertThat(result.file()).isEqualTo(output);
        verify(blenderProcessPort).optimizeUsdz(input, options);
        // Input temp file is cleaned up after conversion.
        verify(fileStorage).delete(input);
    }
}
