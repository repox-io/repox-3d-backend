package io.repox.api3d.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.repox.api3d.app.usecases.storage.application.ports.out.FileStoragePort;
import io.repox.api3d.app.usecases.toolkit.application.ConversionResult;
import io.repox.api3d.app.usecases.toolkit.application.MeshCompressionService;
import io.repox.api3d.app.usecases.toolkit.application.ports.out.GltfInspectorPort;
import io.repox.api3d.app.usecases.toolkit.application.ports.out.MeshCompressionPort;
import io.repox.api3d.app.usecases.toolkit.domain.MeshCompressionOptions;
import io.repox.api3d.app.utils.exception.KnownRuntimeError;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;

class MeshCompressionServiceTests {

    private final FileStoragePort fileStorage = mock(FileStoragePort.class);
    private final MeshCompressionPort meshCompressionPort = mock(MeshCompressionPort.class);
    private final GltfInspectorPort gltfInspector = mock(GltfInspectorPort.class);

    private final MeshCompressionService service =
            new MeshCompressionService(fileStorage, meshCompressionPort, gltfInspector);

    private File tempFile(String suffix) throws IOException {
        File f = Files.createTempFile("mesh-compress-test", suffix).toFile();
        f.deleteOnExit();
        return f;
    }

    @Test
    void plainInputIsCompressedWithGltfpack() throws Exception {
        File input = tempFile(".glb");
        File output = tempFile("_compressed.glb");
        when(gltfInspector.usesDraco(input)).thenReturn(false);
        when(meshCompressionPort.compress(input, MeshCompressionOptions.defaults()))
                .thenReturn(output);

        ConversionResult result = service.compressMeshFromFile(input, MeshCompressionOptions.defaults());

        assertThat(result.file()).isEqualTo(output);
        verify(meshCompressionPort).compress(input, MeshCompressionOptions.defaults());
        verify(fileStorage).delete(input);
    }

    @Test
    void dracoInputIsRejectedWithoutCompressing() throws Exception {
        File input = tempFile(".glb");
        when(gltfInspector.usesDraco(input)).thenReturn(true);

        assertThatThrownBy(() -> service.compressMeshFromFile(input, MeshCompressionOptions.defaults()))
                .isInstanceOf(KnownRuntimeError.class)
                .hasMessageContaining("Draco");

        // gltfpack is never invoked, but the input temp file is still cleaned up
        verify(meshCompressionPort, never()).compress(any(), any());
        verify(fileStorage).delete(input);
    }

    @Test
    void verifyCompressibleRejectsDracoAndPassesPlain() throws Exception {
        File draco = tempFile(".glb");
        File plain = tempFile(".glb");
        when(gltfInspector.usesDraco(draco)).thenReturn(true);
        when(gltfInspector.usesDraco(plain)).thenReturn(false);

        assertThatThrownBy(() -> service.verifyCompressible(draco)).isInstanceOf(KnownRuntimeError.class);
        service.verifyCompressible(plain); // does not throw
    }
}
