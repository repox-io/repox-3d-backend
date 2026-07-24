package io.repox.api3d.app.usecases.toolkit.adapters.out;

import io.repox.api3d.app.usecases.toolkit.application.ports.out.GltfInspectorPort;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Inspects a glTF/GLB file for the {@code KHR_draco_mesh_compression} extension. gltfpack cannot read
 * Draco-compressed input, so {@link io.repox.api3d.app.usecases.toolkit.application.MeshCompressionService}
 * uses this to decide whether an input must first be decoded via Blender.
 *
 * <p>Detection is a literal substring test against the glTF JSON: the extension name appears as
 * uncompressed ASCII in {@code extensionsUsed}/{@code extensionsRequired}, so no JSON parser is needed.
 * For {@code .glb} only the leading JSON chunk is read (bounded by its declared length); for {@code .gltf}
 * the whole text file is read. Any I/O or format problem yields {@code false} — detection must never
 * itself break an otherwise valid request; gltfpack will surface its own error if the file is unusable.
 */
@Slf4j
@Component
public class GltfInspector implements GltfInspectorPort {

    private static final String DRACO_EXTENSION = "KHR_draco_mesh_compression";

    /** GLB container magic ("glTF", little-endian uint32). */
    private static final int GLB_MAGIC = 0x46546C67;
    /** GLB chunk type for the JSON chunk ("JSON", little-endian uint32). */
    private static final int GLB_CHUNK_TYPE_JSON = 0x4E4F534A;
    /** 12-byte GLB header + 8-byte chunk header. */
    private static final int GLB_HEADER_LENGTH = 12;
    /** Upper bound on the JSON chunk we are willing to read into memory (guards against corrupt lengths). */
    private static final int MAX_JSON_CHUNK_BYTES = 64 * 1024 * 1024;

    @Override
    public boolean usesDraco(File file) {
        if (file == null) {
            return false;
        }
        String name = file.getName().toLowerCase();
        try {
            if (name.endsWith(".glb")) {
                return glbJsonChunk(file).contains(DRACO_EXTENSION);
            }
            if (name.endsWith(".gltf")) {
                return Files.readString(file.toPath(), StandardCharsets.UTF_8).contains(DRACO_EXTENSION);
            }
        } catch (Exception e) {
            log.debug("Could not inspect {} for Draco compression; assuming not Draco: {}", name, e.getMessage());
        }
        return false;
    }

    /** Reads and returns the UTF-8 JSON chunk of a binary glTF (GLB) container. */
    private String glbJsonChunk(File file) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            if (raf.length() < GLB_HEADER_LENGTH + 8) {
                throw new IOException("file too small to be a GLB");
            }
            byte[] header = new byte[GLB_HEADER_LENGTH + 8];
            raf.readFully(header);
            if (readLeUint32(header, 0) != GLB_MAGIC) {
                throw new IOException("not a GLB container (bad magic)");
            }
            long jsonLength = readLeUint32(header, GLB_HEADER_LENGTH);
            if (readLeUint32(header, GLB_HEADER_LENGTH + 4) != GLB_CHUNK_TYPE_JSON) {
                throw new IOException("first GLB chunk is not JSON");
            }
            if (jsonLength <= 0 || jsonLength > MAX_JSON_CHUNK_BYTES || jsonLength > raf.length() - header.length) {
                throw new IOException("implausible JSON chunk length: " + jsonLength);
            }
            byte[] json = new byte[(int) jsonLength];
            raf.readFully(json);
            return new String(json, StandardCharsets.UTF_8);
        }
    }

    /** Reads a little-endian unsigned 32-bit integer from {@code buf} at {@code offset}. */
    private long readLeUint32(byte[] buf, int offset) {
        return (buf[offset] & 0xFFL)
                | ((buf[offset + 1] & 0xFFL) << 8)
                | ((buf[offset + 2] & 0xFFL) << 16)
                | ((buf[offset + 3] & 0xFFL) << 24);
    }
}
