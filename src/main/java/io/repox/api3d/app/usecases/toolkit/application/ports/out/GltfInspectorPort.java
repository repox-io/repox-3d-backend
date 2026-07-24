package io.repox.api3d.app.usecases.toolkit.application.ports.out;

import java.io.File;

public interface GltfInspectorPort {

    /**
     * Determines whether a glTF/GLB file declares the {@code KHR_draco_mesh_compression} extension.
     * gltfpack cannot read Draco-compressed input, so callers use this to decide whether the file must
     * first be decoded (e.g. via Blender) before compression.
     *
     * @param file the {@code .glb}/{@code .gltf} file to inspect
     * @return {@code true} if the file uses Draco mesh compression; {@code false} for plain glTF,
     *     non-glTF input, or any read/format error (detection never throws)
     */
    boolean usesDraco(File file);
}
