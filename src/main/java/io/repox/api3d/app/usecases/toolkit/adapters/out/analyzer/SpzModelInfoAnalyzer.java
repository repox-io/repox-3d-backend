package io.repox.api3d.app.usecases.toolkit.adapters.out.analyzer;

import io.repox.api3d.app.usecases.toolkit.application.ports.out.ModelInfoAnalyzer;
import io.repox.api3d.app.usecases.toolkit.domain.ModelTechnicalDetailsDto;
import io.repox.api3d.app.utils.exception.KnownRuntimeError;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import org.springframework.stereotype.Component;

/**
 * Reads the gaussian count from a Niantic SPZ file header. In every header variant {@code numPoints}
 * is a little-endian {@code uint32} at byte offset 8.
 *
 * <ul>
 *   <li>Legacy v1–3: the whole file is gzip-compressed; decompress, then read the 16-byte header.
 *   <li>v4 "NGSP": a 32-byte plaintext header precedes the (zstd) payload; read it directly.
 * </ul>
 *
 * Both variants carry the {@code NGSP} magic ({@code 0x5053474E} little-endian) at offset 0.
 */
@Component
class SpzModelInfoAnalyzer implements ModelInfoAnalyzer {

    private static final Set<String> EXTENSIONS = Set.of("spz");
    private static final int NGSP_MAGIC = 0x5053474E;
    private static final int NUM_POINTS_OFFSET = 8;
    private static final int HEADER_READ_BYTES = 16;

    @Override
    public Set<String> supportedExtensions() {
        return EXTENSIONS;
    }

    @Override
    public ModelTechnicalDetailsDto analyze(File file) throws IOException {
        long numPoints;
        try (InputStream raw = Files.newInputStream(file.toPath());
                BufferedInputStream buffered = new BufferedInputStream(raw)) {
            buffered.mark(2);
            int b0 = buffered.read();
            int b1 = buffered.read();
            buffered.reset();

            boolean gzip = b0 == 0x1F && b1 == 0x8B;
            try (InputStream headerStream = gzip ? new GZIPInputStream(buffered) : buffered) {
                byte[] header = headerStream.readNBytes(HEADER_READ_BYTES);
                if (header.length < HEADER_READ_BYTES) {
                    throw new KnownRuntimeError("SPZ file too short to contain a header: " + file.getName());
                }
                ByteBuffer buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
                if (buf.getInt(0) != NGSP_MAGIC) {
                    throw new KnownRuntimeError("Not a valid SPZ file (bad magic): " + file.getName());
                }
                numPoints = Integer.toUnsignedLong(buf.getInt(NUM_POINTS_OFFSET));
            }
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
                numPoints, // pointCount
                numPoints); // gaussianCount
    }
}
