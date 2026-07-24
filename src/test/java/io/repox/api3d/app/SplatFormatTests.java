package io.repox.api3d.app;

import static org.assertj.core.api.Assertions.assertThat;

import io.repox.api3d.app.usecases.toolkit.domain.SplatFormat;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class SplatFormatTests {

    @Test
    void compressingFormatsAreTheCompactContainers() {
        Set<SplatFormat> compressing = Arrays.stream(SplatFormat.values())
                .filter(SplatFormat::isCompressing)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(SplatFormat.class)));

        assertThat(compressing)
                .containsExactlyInAnyOrder(
                        SplatFormat.COMPRESSED_PLY, SplatFormat.KSPLAT, SplatFormat.SPZ, SplatFormat.SOG);
    }

    @Test
    void rawContainersAreNotCompressing() {
        assertThat(SplatFormat.PLY.isCompressing()).isFalse();
        assertThat(SplatFormat.SPLAT.isCompressing()).isFalse();
    }
}
