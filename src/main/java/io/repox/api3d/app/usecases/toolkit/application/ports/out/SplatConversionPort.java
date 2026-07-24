package io.repox.api3d.app.usecases.toolkit.application.ports.out;

import io.repox.api3d.app.usecases.toolkit.domain.SplatFormat;
import java.io.File;

public interface SplatConversionPort {

    /**
     * Converts a Gaussian-splat file to the given target format, writing the output alongside the input
     * and returning it. Implemented by the GaussForge {@code gfconvert} subprocess adapter.
     *
     * @param input the source splat file (PLY/compressed-PLY/SPLAT/KSPLAT/SPZ/SOG)
     * @param target the desired output format
     * @return the converted output file
     * @throws Exception if the subprocess fails or times out
     */
    File convert(File input, SplatFormat target) throws Exception;
}
