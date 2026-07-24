package io.repox.api3d.app.usecases.toolkit.application.ports.in;

import org.springframework.web.multipart.MultipartFile;

public interface ImageTo3DUseCase {
    /**
     * Converts an uploaded image to a 3D model (GLB) via TripoSR.
     *
     * @param file the uploaded image file
     * @return the raw bytes of the generated GLB file
     */
    byte[] convert(MultipartFile file);
}
