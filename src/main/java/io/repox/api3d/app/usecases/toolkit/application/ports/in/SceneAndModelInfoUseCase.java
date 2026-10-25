package io.repox.api3d.app.usecases.toolkit.application.ports.in;

import io.repox.api3d.app.usecases.toolkit.domain.ModelDetectionDto;
import io.repox.api3d.app.usecases.toolkit.domain.ModelSourceInfoDto;
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

    /** Reports the size, filename and content type of an upload, without storing it. */
    ModelSourceInfoDto sourceInfo(MultipartFile file);

    /** Reports the size, filename and content type of a remote source, without downloading its body. */
    ModelSourceInfoDto sourceInfoFromUri(String url);

    /**
     * Reads info from an already-stored input file, which is deleted once it has been read. The uploaded
     * part's content type is not carried through the job input, so it is inferred from the extension and is
     * null for the 3D formats no mime table knows (.glb, .ply, .splat, ...).
     */
    ModelSourceInfoDto sourceInfoFromFile(File input);
}
