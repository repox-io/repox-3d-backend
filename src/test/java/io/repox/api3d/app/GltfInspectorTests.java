package io.repox.api3d.app;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import io.repox.api3d.app.usecases.toolkit.adapters.out.GltfInspector;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GltfInspectorTests {

    private static final String DRACO_JSON =
            "{\"asset\":{\"version\":\"2.0\"},\"extensionsRequired\":[\"KHR_draco_mesh_compression\"]}";
    private static final String PLAIN_JSON = "{\"asset\":{\"version\":\"2.0\"},\"meshes\":[]}";

    private final GltfInspector inspector = new GltfInspector();

    @TempDir
    Path tempDir;

    @Test
    void detectsDracoInGlb() throws IOException {
        File file = writeBytes("draco.glb", glb(DRACO_JSON));
        assertThat(inspector.usesDraco(file)).isTrue();
    }

    @Test
    void plainGlbIsNotDraco() throws IOException {
        File file = writeBytes("plain.glb", glb(PLAIN_JSON));
        assertThat(inspector.usesDraco(file)).isFalse();
    }

    @Test
    void detectsDracoInGltfText() throws IOException {
        File file = writeBytes("draco.gltf", DRACO_JSON.getBytes(UTF_8));
        assertThat(inspector.usesDraco(file)).isTrue();
    }

    @Test
    void plainGltfTextIsNotDraco() throws IOException {
        File file = writeBytes("plain.gltf", PLAIN_JSON.getBytes(UTF_8));
        assertThat(inspector.usesDraco(file)).isFalse();
    }

    @Test
    void truncatedGlbReturnsFalseWithoutThrowing() throws IOException {
        File file = writeBytes("garbage.glb", new byte[] {1, 2, 3, 4, 5});
        assertThat(inspector.usesDraco(file)).isFalse();
    }

    @Test
    void nonGltfExtensionIsIgnored() throws IOException {
        // Even though the bytes contain the marker, a non-glTF extension is never inspected.
        File file = writeBytes("model.obj", DRACO_JSON.getBytes(UTF_8));
        assertThat(inspector.usesDraco(file)).isFalse();
    }

    @Test
    void nullFileReturnsFalse() {
        assertThat(inspector.usesDraco(null)).isFalse();
    }

    private File writeBytes(String name, byte[] bytes) throws IOException {
        Path path = tempDir.resolve(name);
        Files.write(path, bytes);
        return path.toFile();
    }

    /** Builds a minimal GLB container (header + JSON chunk only) wrapping the given JSON. */
    private static byte[] glb(String json) {
        byte[] jsonBytes = json.getBytes(UTF_8);
        int pad = (4 - (jsonBytes.length % 4)) % 4; // JSON chunk must be 4-byte aligned (space padded)
        int jsonChunkLength = jsonBytes.length + pad;
        int total = 12 + 8 + jsonChunkLength;

        ByteBuffer buf = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN);
        buf.put("glTF".getBytes(US_ASCII)); // magic
        buf.putInt(2); // version
        buf.putInt(total); // total length
        buf.putInt(jsonChunkLength); // chunk 0 length
        buf.put("JSON".getBytes(US_ASCII)); // chunk 0 type
        buf.put(jsonBytes);
        for (int i = 0; i < pad; i++) {
            buf.put((byte) 0x20);
        }
        return buf.array();
    }
}
