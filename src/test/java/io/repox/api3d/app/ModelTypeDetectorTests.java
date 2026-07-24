package io.repox.api3d.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.repox.api3d.app.usecases.toolkit.application.ModelTypeDetector;
import io.repox.api3d.app.usecases.toolkit.domain.MeshExportFormat;
import io.repox.api3d.app.usecases.toolkit.domain.ModelDetectionDto;
import io.repox.api3d.app.usecases.toolkit.domain.ModelKind;
import io.repox.api3d.app.usecases.toolkit.domain.SplatFormat;
import io.repox.api3d.app.utils.exception.KnownRuntimeError;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ModelTypeDetectorTests {

    private final ModelTypeDetector detector = new ModelTypeDetector();

    private static final ModelTypeDetector.HeaderBytesSupplier MUST_NOT_READ = () -> {
        throw new AssertionError("header bytes were fetched for an unambiguous extension");
    };

    private static final List<String> MESH_TARGETS =
            Arrays.stream(MeshExportFormat.values()).map(Enum::name).toList();
    private static final List<String> SPLAT_TARGETS =
            Arrays.stream(SplatFormat.values()).map(Enum::name).toList();

    @ParameterizedTest
    @ValueSource(
            strings = {
                "model.glb",
                "model.gltf",
                "model.obj",
                "model.stl",
                "model.dae",
                "model.fbx",
                "model.usdz",
                "model.usd",
                "model.usda",
                "model.usdc"
            })
    void unambiguousMeshExtensionNeedsNoHeader(String filename) {
        ModelDetectionDto result = detector.detect(filename, MUST_NOT_READ);

        assertThat(result.kind()).isEqualTo(ModelKind.MESH);
        assertThat(result.targetFormats()).containsExactlyElementsOf(MESH_TARGETS);
    }

    @ParameterizedTest
    @ValueSource(strings = {"scene.spz", "scene.ksplat", "scene.splat", "scene.sog"})
    void unambiguousSplatExtensionNeedsNoHeader(String filename) {
        ModelDetectionDto result = detector.detect(filename, MUST_NOT_READ);

        assertThat(result.kind()).isEqualTo(ModelKind.SPLAT);
        assertThat(result.targetFormats()).containsExactlyElementsOf(SPLAT_TARGETS);
    }

    @Test
    void plyWithFacesAndNoGaussianPropsIsMesh() {
        String header = "ply\n"
                + "format ascii 1.0\n"
                + "element vertex 3\n"
                + "property float x\n"
                + "property float y\n"
                + "property float z\n"
                + "element face 1\n"
                + "property list uchar int vertex_indices\n"
                + "end_header\n";

        ModelDetectionDto result = detector.detect("model.ply", () -> header.getBytes(StandardCharsets.US_ASCII));

        assertThat(result.kind()).isEqualTo(ModelKind.MESH);
        assertThat(result.sourceFormat()).isEqualTo("PLY");
    }

    @Test
    void plyWithGaussianPropsIsSplat() {
        String header = "ply\n"
                + "format binary_little_endian 1.0\n"
                + "element vertex 100\n"
                + "property float x\n"
                + "property float y\n"
                + "property float z\n"
                + "property float f_dc_0\n"
                + "property float scale_0\n"
                + "property float rot_0\n"
                + "property float opacity\n"
                + "end_header\n";

        ModelDetectionDto result = detector.detect("cloud.ply", () -> header.getBytes(StandardCharsets.US_ASCII));

        assertThat(result.kind()).isEqualTo(ModelKind.SPLAT);
        assertThat(result.sourceFormat()).isEqualTo("PLY");
        assertThat(result.targetFormats()).containsExactlyElementsOf(SPLAT_TARGETS);
    }

    @Test
    void noExtensionFallsBackToGltfMagic() {
        byte[] glb = concat("glTF".getBytes(StandardCharsets.US_ASCII), new byte[] {0x02, 0, 0, 0});

        ModelDetectionDto result = detector.detect("https://host/asset/content", () -> glb);

        assertThat(result.kind()).isEqualTo(ModelKind.MESH);
        assertThat(result.sourceFormat()).isEqualTo("GLB");
    }

    @Test
    void noExtensionFallsBackToSpzMagic() {
        byte[] spz = concat("NGSP".getBytes(StandardCharsets.US_ASCII), new byte[] {0x02, 0, 0, 0});

        ModelDetectionDto result = detector.detect("download?id=42", () -> spz);

        assertThat(result.kind()).isEqualTo(ModelKind.SPLAT);
        assertThat(result.sourceFormat()).isEqualTo("SPZ");
    }

    @Test
    void unrecognisedHeaderIsRejected() {
        byte[] junk = "not a 3d model at all".getBytes(StandardCharsets.US_ASCII);

        assertThatThrownBy(() -> detector.detect("mystery.bin", () -> junk)).isInstanceOf(KnownRuntimeError.class);
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
