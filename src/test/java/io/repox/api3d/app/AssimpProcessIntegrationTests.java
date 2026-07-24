package io.repox.api3d.app;

import io.repox.api3d.app.usecases.storage.application.TempFileStorageService;
import io.repox.api3d.app.usecases.storage.domain.FileStorageSettings;
import io.repox.api3d.app.usecases.toolkit.adapters.out.AssimpProcessIntegration;
import io.repox.api3d.app.usecases.toolkit.application.ports.out.AssimpProcessPort;
import io.repox.api3d.app.usecases.toolkit.domain.AssimpExportFormatEnum;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class AssimpProcessIntegrationTests {
    AssimpProcessPort assimpProcessPort = new AssimpProcessIntegration(
            new TempFileStorageService(new FileStorageSettings(System.getProperty("java.io.tmpdir"))));

    @Test
    void infoTest() throws URISyntaxException, IOException, InterruptedException {
        assimpProcessPort.info(new File(AssimpProcessIntegrationTests.class
                .getClassLoader()
                .getResource("BabyElephant_GLB.glb")
                .toURI()));
    }

    @Test
    void exportTest() {
        Assertions.assertThatCode(() -> assimpProcessPort.export(
                        new File(AssimpProcessIntegrationTests.class
                                .getClassLoader()
                                .getResource("BabyElephant_GLB.glb")
                                .toURI()),
                        AssimpExportFormatEnum.FBX))
                .doesNotThrowAnyException();
    }

    @Test
    void listextTest() throws IOException {
        Assertions.assertThat(!assimpProcessPort.listext().isEmpty());
    }

    @Test
    void versionTest() throws IOException, InterruptedException {
        Assertions.assertThat(assimpProcessPort.version().contains("6.0"));
    }
}
