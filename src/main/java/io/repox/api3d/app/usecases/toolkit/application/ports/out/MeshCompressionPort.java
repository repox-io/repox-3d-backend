package io.repox.api3d.app.usecases.toolkit.application.ports.out;

import io.repox.api3d.app.usecases.toolkit.domain.MeshCompressionOptions;
import java.io.File;

public interface MeshCompressionPort {

    /**
     * Compresses a glTF/GLB mesh for delivery (meshopt geometry + optional KTX2 texture supercompression),
     * writing a {@code .glb} alongside the input and returning it. Implemented by the gltfpack subprocess
     * adapter.
     *
     * @param input the source {@code .glb}/{@code .gltf} file
     * @param options compression options
     * @return the compressed {@code .glb} output file
     * @throws Exception if the subprocess fails or times out
     */
    File compress(File input, MeshCompressionOptions options) throws Exception;
}
