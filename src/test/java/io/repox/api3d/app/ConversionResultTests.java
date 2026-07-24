package io.repox.api3d.app;

import static org.assertj.core.api.Assertions.assertThat;

import io.repox.api3d.app.usecases.toolkit.application.ConversionResult;
import java.io.File;
import org.junit.jupiter.api.Test;

class ConversionResultTests {

    @Test
    void stripsUuidPrefixFromDownloadNameButKeepsFileOnDisk() {
        File output = new File("/tmp/3f2504e0-4f89-41d3-9a0c-0305e82c3301_model_converted_20260706_143022.glb");

        ConversionResult result = ConversionResult.forOutput(output, () -> {});

        assertThat(result.file()).isEqualTo(output); // on-disk file keeps its unique name
        assertThat(result.downloadName()).isEqualTo("model_converted_20260706_143022.glb");
    }

    @Test
    void leavesNameUnchangedWhenThereIsNoUuidPrefix() {
        File output = new File("/tmp/model_compressed_20260706_143022.glb");

        ConversionResult result = ConversionResult.forOutput(output, () -> {});

        assertThat(result.downloadName()).isEqualTo("model_compressed_20260706_143022.glb");
    }
}
