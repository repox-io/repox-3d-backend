package io.repox.api3d.app.usecases.toolkit.adapters.out.analyzer;

import io.repox.api3d.app.usecases.toolkit.application.ports.out.ModelInfoAnalyzer;
import io.repox.api3d.app.usecases.toolkit.domain.ModelTechnicalDetailsDto;
import io.repox.api3d.app.utils.exception.KnownRuntimeError;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Reads the total splat count from a .ksplat (mkkellogg/GaussianSplats3D) file header. The 4096-byte
 * header stores the total {@code splatCount} as a little-endian {@code uint32} at byte offset 16.
 */
@Component
class KsplatModelInfoAnalyzer implements ModelInfoAnalyzer {

    private static final Set<String> EXTENSIONS = Set.of("ksplat");
    private static final int SPLAT_COUNT_OFFSET = 16;
    private static final int HEADER_READ_BYTES = 20;

    @Override
    public Set<String> supportedExtensions() {
        return EXTENSIONS;
    }

    @Override
    public ModelTechnicalDetailsDto analyze(File file) throws IOException {
        long splatCount;
        try (InputStream in = Files.newInputStream(file.toPath())) {
            byte[] header = in.readNBytes(HEADER_READ_BYTES);
            if (header.length < HEADER_READ_BYTES) {
                throw new KnownRuntimeError("ksplat file too short to contain a header: " + file.getName());
            }
            ByteBuffer buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
            splatCount = Integer.toUnsignedLong(buf.getInt(SPLAT_COUNT_OFFSET));
        }

        return new ModelTechnicalDetailsDto(
                null, // memoryConsumptionB
                null, // nodes
                null, // meshes
                null, // animations
                null, // textures
                null, // materials
                null, // cameras
                null, // lights
                null, // vertices
                null, // faces
                null, // bones
                null, // animationChannels
                splatCount, // pointCount
                splatCount); // gaussianCount
    }
}
