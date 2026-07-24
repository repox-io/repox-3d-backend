package io.repox.api3d.app.usecases.job.domain;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * The serialized request captured for a processing job, stored verbatim in the {@code input} jsonb column.
 * Holds where the source model comes from and the type-specific options; a {@code ModelJobProcessor}
 * resolves the actual input file (download the {@link #url()} or read the stored {@link #inputFile()}) and
 * interprets {@link #options()} for its type.
 *
 * @param source whether the source is fetched from a URL or was uploaded and pre-stored
 * @param url source URL, set when {@code source == URL}
 * @param inputFile filename under the storage input directory, set when {@code source == UPLOAD}
 * @param options type-specific options (e.g. compression flags, target format); may be null
 */
public record JobInput(Source source, String url, String inputFile, JsonNode options) {

    public enum Source {
        URL,
        UPLOAD
    }

    public static JobInput fromUrl(String url, JsonNode options) {
        return new JobInput(Source.URL, url, null, options);
    }

    public static JobInput fromUpload(String inputFile, JsonNode options) {
        return new JobInput(Source.UPLOAD, null, inputFile, options);
    }
}
