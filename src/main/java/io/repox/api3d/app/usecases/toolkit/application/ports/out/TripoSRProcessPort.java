package io.repox.api3d.app.usecases.toolkit.application.ports.out;

import java.io.File;

public interface TripoSRProcessPort {
    /**
     * Converts an image file to a 3D model (GLB) using TripoSR.
     *
     * @param imageFile the input image file
     * @param outputDir the directory where TripoSR will write the output
     * @return the generated GLB file
     */
    File convert(File imageFile, File outputDir) throws Exception;
}
