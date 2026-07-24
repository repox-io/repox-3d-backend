package io.repox.api3d.app.utils;

import io.repox.api3d.app.utils.exception.KnownRuntimeError;
import org.springframework.web.multipart.MultipartFile;

public class ModelSourceUtils {

    private ModelSourceUtils() {}

    /**
     * Validates that exactly one model source was supplied and reports which one. Returns {@code true} when the
     * uploaded {@code file} is the source, {@code false} when the {@code url} is; throws a 400 if both or neither
     * are provided. An empty {@code file} part is treated as absent so a stray part alongside a url is ignored.
     *
     * @param file the uploaded model, or {@code null}/empty when the caller supplied a url
     * @param url the model url, or {@code null}/blank when the caller supplied an upload
     * @return {@code true} when the upload is the source, {@code false} when the url is
     */
    public static boolean isUpload(MultipartFile file, String url) {
        boolean hasFile = file != null && !file.isEmpty();
        boolean hasUrl = url != null && !url.isBlank();
        if (hasFile == hasUrl) {
            throw new KnownRuntimeError("Provide either a file or a url, but not both.");
        }
        return hasFile;
    }
}
