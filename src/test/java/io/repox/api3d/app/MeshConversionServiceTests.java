package io.repox.api3d.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.repox.api3d.app.usecases.storage.application.ports.out.FileStoragePort;
import io.repox.api3d.app.usecases.toolkit.application.ConversionResult;
import io.repox.api3d.app.usecases.toolkit.application.MeshConversionService;
import io.repox.api3d.app.usecases.toolkit.application.ports.out.AssimpProcessPort;
import io.repox.api3d.app.usecases.toolkit.application.ports.out.BlenderProcessPort;
import io.repox.api3d.app.usecases.toolkit.domain.AssimpExportFormatEnum;
import io.repox.api3d.app.usecases.toolkit.domain.BlenderExportFormat;
import io.repox.api3d.app.usecases.toolkit.domain.MeshExportFormat;
import io.repox.api3d.app.utils.exception.KnownRuntimeError;
import io.repox.api3d.appconfig.options.ConverterProperties;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;

class MeshConversionServiceTests {

    private final FileStoragePort fileStorage = mock(FileStoragePort.class);
    private final AssimpProcessPort assimpProcessPort = mock(AssimpProcessPort.class);
    private final BlenderProcessPort blenderProcessPort = mock(BlenderProcessPort.class);
    private final ConverterProperties converterProperties = new ConverterProperties();

    private final MeshConversionService service =
            new MeshConversionService(fileStorage, assimpProcessPort, blenderProcessPort, converterProperties);

    private File tempFile(String suffix) throws IOException {
        File f = Files.createTempFile("mesh-conv-test", suffix).toFile();
        f.deleteOnExit();
        return f;
    }

    @Test
    void fallsBackToBlenderWhenAssimpThrows() throws Exception {
        File input = tempFile(".glb");
        File blenderOutput = tempFile(".obj");
        when(fileStorage.saveFromUri(anyString(), anyBoolean())).thenReturn(input);
        when(assimpProcessPort.export(any(), eq(AssimpExportFormatEnum.OBJ)))
                .thenThrow(new KnownRuntimeError("assimp could not import draco glb"));
        when(blenderProcessPort.convert(input, BlenderExportFormat.OBJ)).thenReturn(blenderOutput);

        ConversionResult result = service.convertMeshFromUri("http://example/model.glb", MeshExportFormat.OBJ);

        assertThat(result.file()).isEqualTo(blenderOutput);
        verify(blenderProcessPort).convert(input, BlenderExportFormat.OBJ);
    }

    @Test
    void fallsBackToBlenderWhenAssimpOutputIsEmpty() throws Exception {
        File input = tempFile(".glb");
        File blenderOutput = tempFile(".obj");
        // Assimp "succeeds" but the output file is missing/empty — the Draco/empty-scene symptom.
        File emptyAssimpOutput = new File(input.getParentFile(), "does-not-exist.obj");
        when(fileStorage.saveFromUri(anyString(), anyBoolean())).thenReturn(input);
        when(assimpProcessPort.export(any(), eq(AssimpExportFormatEnum.OBJ))).thenReturn(emptyAssimpOutput);
        when(blenderProcessPort.convert(input, BlenderExportFormat.OBJ)).thenReturn(blenderOutput);

        ConversionResult result = service.convertMeshFromUri("http://example/model.glb", MeshExportFormat.OBJ);

        assertThat(result.file()).isEqualTo(blenderOutput);
    }

    @Test
    void rethrowsWhenFallbackDisabled() throws Exception {
        File input = tempFile(".glb");
        converterProperties.setMeshConvertBlenderFallback(false);
        when(fileStorage.saveFromUri(anyString(), anyBoolean())).thenReturn(input);
        when(assimpProcessPort.export(any(), eq(AssimpExportFormatEnum.OBJ)))
                .thenThrow(new KnownRuntimeError("assimp failed"));

        assertThatThrownBy(() -> service.convertMeshFromUri("http://example/model.glb", MeshExportFormat.OBJ))
                .isInstanceOf(RuntimeException.class);
        verify(blenderProcessPort, never()).convert(any(), any());
    }

    @Test
    void usdTargetGoesStraightToBlenderWithoutAssimp() throws Exception {
        File input = tempFile(".glb");
        File usdzOutput = tempFile(".usdz");
        when(fileStorage.saveFromUri(anyString(), anyBoolean())).thenReturn(input);
        when(blenderProcessPort.convert(input, BlenderExportFormat.USDZ)).thenReturn(usdzOutput);

        ConversionResult result = service.convertMeshFromUri("http://example/model.glb", MeshExportFormat.USDZ);

        assertThat(result.file()).isEqualTo(usdzOutput);
        verify(assimpProcessPort, never()).export(any(), any());
        verify(blenderProcessPort).convert(input, BlenderExportFormat.USDZ);
    }
}
