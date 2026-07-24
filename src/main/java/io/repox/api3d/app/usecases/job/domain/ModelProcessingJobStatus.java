package io.repox.api3d.app.usecases.job.domain;

/** Lifecycle state of a {@link ModelProcessingJobEntity}. */
public enum ModelProcessingJobStatus {
    /** Accepted and queued, not yet picked up by a worker. */
    PENDING,
    /** Currently being processed by a worker. */
    RUNNING,
    /** Finished successfully. If the type produces output, {@code output_file}/{@code expires_at} are set. */
    COMPLETED,
    /** Processing failed; see {@code error_message}. */
    FAILED
}
