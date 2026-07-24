package io.repox.api3d.app.usecases.toolkit.application.ports.out;

import io.repox.api3d.app.usecases.toolkit.domain.AssimpExportFormatEnum;
import io.repox.api3d.app.usecases.toolkit.domain.AssimpInfoTechnicalDetails;
import java.io.File;
import java.io.IOException;
import java.util.List;

public interface AssimpProcessPort {
    /**
     * Quick file stats.
     */
    AssimpInfoTechnicalDetails info(File file) throws IOException;

    /**
     * List all known file extensions available for import.
     */
    List<String> listext() throws IOException;

    /**
     * Check whether a file extension is recognized by Assimp.
     */
    boolean knownext(File file) throws IOException;

    /**
     * Export a file to one of the supported output formats. The output is written to the same directory
     * as the input (base name + the format extension) and returned. Throws if the underlying Assimp
     * subprocess fails or times out — the mesh-conversion service relies on this to trigger the Blender
     * fallback for files Assimp cannot import (e.g. Draco-compressed glTF/GLB, USDZ).
     *
     * @param imported the source file
     * @param exportFormat the target export format
     * @return the exported output file
     */
    File export(File imported, AssimpExportFormatEnum exportFormat) throws IOException;

    /**
     * List all supported export formats.
     */
    List<String> listexport();

    /**
     * Show basic information on a specific export format.
     */
    String exportinfo(String format);

    /**
     * Extract embedded texture images.
     */
    List<Object> extract(File file);

    /**
     * Convert models to a binary or textual dump (ASSBIN/ASSXML).
     */
    Object dump(File file);

    /**
     * Compare dumps created using 'assimp dump <file> -s ..'.
     */
    Object cmpdump(List<File> dumps);

    /**
     * Display Assimp version.
     */
    String version() throws IOException, InterruptedException;
}
