package io.repox.api3d.app.usecases.toolkit.application.ports.in;

import io.repox.api3d.app.usecases.toolkit.application.ConversionResult;
import java.io.File;
import org.springframework.web.multipart.MultipartFile;

public interface ModelThumbnailUseCase {
    /**
     * Renders a PNG thumbnail of an uploaded 3D model file.
     *
     * @param file the uploaded model file
     * @return PNG image bytes
     */
    byte[] generateThumbnail(MultipartFile file);

    /**
     * Renders a PNG thumbnail of a 3D model fetched from an external URL.
     *
     * @param url the URL of the model file
     * @return PNG image bytes
     */
    byte[] generateThumbnailFromUri(String url);

    /** Renders a thumbnail of an already-stored input file. The caller owns the returned output's lifecycle. */
    ConversionResult generateThumbnailFromFile(File input) throws Exception;
}
