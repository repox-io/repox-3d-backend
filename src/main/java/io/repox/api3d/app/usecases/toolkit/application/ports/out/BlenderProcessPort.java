package io.repox.api3d.app.usecases.toolkit.application.ports.out;

import io.repox.api3d.app.usecases.toolkit.domain.BlenderExportFormat;
import io.repox.api3d.app.usecases.toolkit.domain.ModelTechnicalDetailsDto;
import io.repox.api3d.app.usecases.toolkit.domain.UsdzOptimizeOptions;
import java.io.File;
import java.io.IOException;

public interface BlenderProcessPort {
    /**
     * Renders a thumbnail PNG of the given 3D model file via a background Blender Python script. The
     * adapter picks the destination under the shared output directory, as {@link #convert} and
     * {@link #optimizeUsdz} do; the caller owns the returned file's lifecycle.
     *
     * @param modelFile the input 3D model
     * @return the rendered PNG output file
     */
    File thumbnail(File modelFile) throws Exception;

    /**
     * Extracts technical details (mesh stats) from the given 3D model file via a background Blender
     * Python script. {@code memoryConsumptionB}, {@code pointCount} and {@code gaussianCount} have no
     * Blender equivalent and remain null.
     *
     * @param modelFile the input 3D model
     * @return the extracted technical details
     */
    ModelTechnicalDetailsDto info(File modelFile) throws Exception;

    /**
     * Converts a 3D model file to the given target format via a background Blender Python script
     * (import → export). Used as the fallback for {@code /tools/convert/mesh} when Assimp cannot import
     * the source (e.g. Draco-compressed glTF/GLB, USDZ), and as the primary engine for USD targets which
     * Assimp cannot export.
     *
     * @param modelFile the input 3D model
     * @param target the target export format
     * @return the converted output file (written alongside the input)
     */
    File convert(File modelFile, BlenderExportFormat target) throws Exception;

    /**
     * Optimizes a USDZ file (mesh decimation + texture downscaling) via a background Blender Python
     * script and re-exports it as USDZ. The only self-hostable "compression" path for USDZ.
     *
     * @param modelFile the input USDZ file
     * @param options decimation ratio / texture cap
     * @return the optimized USDZ output file (written alongside the input)
     */
    File optimizeUsdz(File modelFile, UsdzOptimizeOptions options) throws Exception;

    /**
     * Returns the installed Blender version string.
     */
    String version() throws IOException;
}
