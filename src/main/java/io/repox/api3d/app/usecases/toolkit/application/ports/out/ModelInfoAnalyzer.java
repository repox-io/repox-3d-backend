package io.repox.api3d.app.usecases.toolkit.application.ports.out;

import io.repox.api3d.app.usecases.toolkit.domain.ModelTechnicalDetailsDto;
import java.io.File;
import java.util.Set;

/**
 * Strategy for extracting technical details from a 3D model file. Each implementation declares the
 * file extensions it handles; the toolkit service dispatches to the matching analyzer by extension.
 */
public interface ModelInfoAnalyzer {

    /**
     * Lowercase extensions (without the leading dot) this analyzer can process, e.g. {@code "glb"}.
     */
    Set<String> supportedExtensions();

    /**
     * Extracts technical details from the given model file.
     */
    ModelTechnicalDetailsDto analyze(File file) throws Exception;
}
