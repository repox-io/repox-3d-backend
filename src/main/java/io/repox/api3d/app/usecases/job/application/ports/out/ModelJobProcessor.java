package io.repox.api3d.app.usecases.job.application.ports.out;

import com.fasterxml.jackson.databind.JsonNode;
import io.repox.api3d.app.usecases.job.domain.ModelProcessingJobEntity;
import io.repox.api3d.app.usecases.job.domain.ModelProcessingJobType;
import java.io.File;

/**
 * SPI that performs the actual work for one {@link ModelProcessingJobType}. Implemented in the toolkit
 * (which owns the conversion/compression use-cases); the {@code ProcessingJobService} dispatches each job
 * to the processor whose {@link #type()} matches. Implementations resolve the input from the job's stored
 * {@code input} (download a URL or read the stored upload), run the work, and return the deliverable.
 */
public interface ModelJobProcessor {

    ModelProcessingJobType type();

    ProcessingOutput process(ModelProcessingJobEntity job) throws Exception;

    /**
     * Synchronously validates an uploaded input file at submit time, before the job is enqueued, so an input
     * that is certain to fail is rejected with a 4xx instead of producing a doomed job. Invoked only for
     * uploaded sources (the content is already on disk); URL sources are downloaded lazily by the worker and
     * validated there, so they are not pre-checked here. Implementations should throw
     * {@code KnownRuntimeError} to reject. Default: no-op.
     *
     * @param inputFile the stored upload to validate
     */
    default void validateUpload(File inputFile) {}

    /**
     * The deliverable produced by processing, in one of two forms: a file in the storage output directory
     * (downloaded from {@code GET /jobs/{id}/output}), or a structured {@code result} (returned inline by
     * {@code GET /jobs/{id}}). {@link #none()} for job types that yield neither.
     *
     * @param outputFile produced file, or null when the deliverable is not a file
     * @param downloadName suggested download filename, or null when there is no file
     * @param result structured result, or null when the deliverable is a file
     */
    record ProcessingOutput(File outputFile, String downloadName, JsonNode result) {

        public static ProcessingOutput of(File outputFile, String downloadName) {
            return new ProcessingOutput(outputFile, downloadName, null);
        }

        public static ProcessingOutput ofResult(JsonNode result) {
            return new ProcessingOutput(null, null, result);
        }

        public static ProcessingOutput none() {
            return new ProcessingOutput(null, null, null);
        }
    }
}
