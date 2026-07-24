package io.repox.api3d.app.usecases.job.application;

import java.io.File;

/**
 * Outcome of resolving a job's downloadable output, mapped by the controller to HTTP status codes. Only
 * {@link Outcome#AVAILABLE} carries a {@link #file()} and {@link #downloadName()}.
 */
public record JobOutputResolution(Outcome outcome, File file, String downloadName) {

    public enum Outcome {
        /** Output is present and streamable. */
        AVAILABLE,
        /** No job exists for the given id. */
        NOT_FOUND,
        /** Job is still PENDING or RUNNING. */
        NOT_FINISHED,
        /** Job completed but this type yields no output (or none was recorded). */
        NO_OUTPUT,
        /** Job failed during processing. */
        FAILED,
        /** Output existed but its retention window has passed. */
        EXPIRED,
        /** Output should exist but the file is no longer on disk. */
        OUTPUT_MISSING
    }

    public static JobOutputResolution available(File file, String downloadName) {
        return new JobOutputResolution(Outcome.AVAILABLE, file, downloadName);
    }

    public static JobOutputResolution of(Outcome outcome) {
        return new JobOutputResolution(outcome, null, null);
    }
}
