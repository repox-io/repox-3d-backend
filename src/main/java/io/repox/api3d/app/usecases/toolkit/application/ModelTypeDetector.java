package io.repox.api3d.app.usecases.toolkit.application;

import io.repox.api3d.app.usecases.toolkit.domain.MeshExportFormat;
import io.repox.api3d.app.usecases.toolkit.domain.ModelDetectionDto;
import io.repox.api3d.app.usecases.toolkit.domain.ModelKind;
import io.repox.api3d.app.usecases.toolkit.domain.PlyHeader;
import io.repox.api3d.app.usecases.toolkit.domain.SplatFormat;
import io.repox.api3d.app.utils.exception.KnownRuntimeError;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Classifies a 3D model input as {@link ModelKind#MESH} or {@link ModelKind#SPLAT} as cheaply as
 * possible: the file extension decides every format except {@code .ply}, so unambiguous inputs need no
 * bytes at all. Only for an ambiguous ({@code .ply}) or missing/unknown extension are the header bytes
 * sniffed — a PLY gaussian signature, a glTF magic, or an SPZ magic — which the caller supplies lazily
 * so a URL is read (via a small ranged request) only when strictly necessary.
 *
 * <p>Deliberately independent of the {@code ModelInfoAnalyzer}s: mesh analysis shells out to Assimp/Blender
 * over the whole file, exactly what detection must avoid.
 */
@Slf4j
@Component
public class ModelTypeDetector {

    /** Extensions that unambiguously imply a mesh (Assimp/Blender inputs and the USD family). */
    private static final Set<String> MESH_EXTENSIONS =
            Set.of("glb", "gltf", "obj", "stl", "dae", "fbx", "usdz", "usd", "usda", "usdc");

    /** Extensions that unambiguously imply a gaussian splat. {@code .ply} is intentionally absent — it is ambiguous. */
    private static final Set<String> SPLAT_EXTENSIONS = Set.of("splat", "ksplat", "spz", "sog");

    private static final byte[] GLTF_MAGIC = {'g', 'l', 'T', 'F'};
    private static final byte[] NGSP_MAGIC = {'N', 'G', 'S', 'P'};

    private static final List<String> MESH_TARGETS =
            Arrays.stream(MeshExportFormat.values()).map(Enum::name).toList();
    private static final List<String> SPLAT_TARGETS =
            Arrays.stream(SplatFormat.values()).map(Enum::name).toList();

    /** Supplies the leading header bytes of the input, invoked lazily and only when a sniff is required. */
    @FunctionalInterface
    public interface HeaderBytesSupplier {
        byte[] get() throws IOException;
    }

    /**
     * Detects the kind of the input named {@code filename}, sniffing {@code headerBytes} only when the
     * extension cannot settle it. Throws {@link KnownRuntimeError} (mapped to 422) when the type cannot be
     * determined.
     */
    public ModelDetectionDto detect(String filename, HeaderBytesSupplier headerBytes) {
        String ext = extensionOf(filename);
        if (ext != null && MESH_EXTENSIONS.contains(ext)) {
            return meshResult(ext.toUpperCase(Locale.ROOT));
        }
        if (ext != null && SPLAT_EXTENSIONS.contains(ext)) {
            return splatResult(ext.toUpperCase(Locale.ROOT));
        }
        return sniff(filename, ext, headerBytes);
    }

    private ModelDetectionDto sniff(String filename, String ext, HeaderBytesSupplier headerBytes) {
        byte[] head;
        try {
            head = headerBytes.get();
        } catch (IOException e) {
            throw new KnownRuntimeError("Could not read the model header to determine its type for: " + filename, e);
        }

        Optional<PlyHeader> ply = parsePly(head);
        if (ply.isPresent()) {
            return ply.get().gaussianSplat() ? splatResult("PLY") : meshResult("PLY");
        }
        if (startsWith(head, GLTF_MAGIC)) {
            return meshResult(ext == null ? "GLB" : ext.toUpperCase(Locale.ROOT));
        }
        if (isSpz(head)) {
            return splatResult(ext == null ? "SPZ" : ext.toUpperCase(Locale.ROOT));
        }
        throw new KnownRuntimeError("Could not determine whether the input is a mesh or a splat for: " + filename);
    }

    private ModelDetectionDto meshResult(String sourceFormat) {
        return new ModelDetectionDto(ModelKind.MESH, sourceFormat, MESH_TARGETS);
    }

    private ModelDetectionDto splatResult(String sourceFormat) {
        return new ModelDetectionDto(ModelKind.SPLAT, sourceFormat, SPLAT_TARGETS);
    }

    private static String extensionOf(String filename) {
        String ext = StringUtils.getFilenameExtension(filename);
        return ext == null ? null : ext.toLowerCase(Locale.ROOT);
    }

    private static Optional<PlyHeader> parsePly(byte[] head) {
        try {
            return PlyHeader.parse(new ByteArrayInputStream(head));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /** SPZ carries the {@code NGSP} magic at offset 0, either in the clear (v4) or after gzip (legacy v1–3). */
    private static boolean isSpz(byte[] head) {
        if (startsWith(head, NGSP_MAGIC)) {
            return true;
        }
        if (head.length < 2 || (head[0] & 0xFF) != 0x1F || (head[1] & 0xFF) != 0x8B) {
            return false;
        }
        try (InputStream gunzip = new GZIPInputStream(new BufferedInputStream(new ByteArrayInputStream(head)))) {
            byte[] magic = gunzip.readNBytes(NGSP_MAGIC.length);
            return startsWith(magic, NGSP_MAGIC);
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
