package io.repox.api3d.app.usecases.toolkit.adapters.out.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.repox.api3d.app.usecases.job.application.ports.out.ModelJobProcessor;
import io.repox.api3d.app.usecases.job.domain.JobInput;
import io.repox.api3d.app.usecases.job.domain.ModelProcessingJobEntity;
import io.repox.api3d.app.usecases.storage.application.ports.out.FileStoragePort;
import io.repox.api3d.app.usecases.toolkit.application.ConversionResult;
import java.io.File;

/**
 * Base for toolkit-backed job processors. Reads the job's stored {@link JobInput}, resolves the actual
 * input file (downloads a URL into the input directory, or reads the pre-stored upload), then delegates to
 * the type-specific {@link #run(File, JsonNode)}. A file deliverable is returned but intentionally not
 * closed/deleted — the {@code ProcessingJobService} and retention sweep own its lifecycle. The underlying
 * {@code ...FromFile} use-case still deletes the input file when it finishes.
 */
abstract class AbstractModelJobProcessor implements ModelJobProcessor {

    protected final FileStoragePort fileStorage;
    protected final ObjectMapper objectMapper;

    protected AbstractModelJobProcessor(FileStoragePort fileStorage, ObjectMapper objectMapper) {
        this.fileStorage = fileStorage;
        this.objectMapper = objectMapper;
    }

    @Override
    public final ProcessingOutput process(ModelProcessingJobEntity job) throws Exception {
        JobInput input = objectMapper.readValue(job.getInput(), JobInput.class);
        File inputFile = resolveInputFile(input);
        return run(inputFile, input.options());
    }

    private File resolveInputFile(JobInput input) throws Exception {
        return switch (input.source()) {
            case URL -> fileStorage.saveFromUri(input.url(), true);
            case UPLOAD -> new File(fileStorage.getInputDirectory(), input.inputFile());
        };
    }

    /** Runs the concrete work on the resolved input file and returns its deliverable. */
    protected abstract ProcessingOutput run(File input, JsonNode options) throws Exception;

    /** Adapts a file-producing use-case result to the job pipeline's output contract. */
    protected static ProcessingOutput output(ConversionResult result) {
        return ProcessingOutput.of(result.file(), result.downloadName());
    }

    /** Deserializes the stored options node into the concrete options type (null-safe). */
    protected <T> T options(JsonNode options, Class<T> type) throws Exception {
        return options == null || options.isNull() ? null : objectMapper.treeToValue(options, type);
    }
}
