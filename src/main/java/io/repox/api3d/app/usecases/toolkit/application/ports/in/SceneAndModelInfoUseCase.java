package io.repox.api3d.app.usecases.toolkit.application.ports.in;

import io.repox.api3d.app.usecases.toolkit.domain.ModelDetectionDto;
import io.repox.api3d.app.usecases.toolkit.domain.ModelTechnicalDetailsDto;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface SceneAndModelInfoUseCase {
    ModelTechnicalDetailsDto getModelTechnicalDetails(MultipartFile file);

    ModelTechnicalDetailsDto getModelTechnicalDetailsFromUri(String url);

    /** Extracts details from an already-stored input file, which is deleted once analysis finishes. */
    ModelTechnicalDetailsDto getModelTechnicalDetailsFromFile(File input) throws IOException;

    List<String> supportedFileExtensions() throws IOException;

    /** Classifies an uploaded model as mesh or splat by reading only its header (no full-file processing). */
    ModelDetectionDto detect(MultipartFile file);

    /** Classifies a model at the given URL as mesh or splat, reading as few bytes as possible. */
    ModelDetectionDto detectFromUri(String url);
}
