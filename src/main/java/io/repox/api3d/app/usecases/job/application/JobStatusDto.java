package io.repox.api3d.app.usecases.job.application;

import com.fasterxml.jackson.databind.JsonNode;
import io.repox.api3d.app.usecases.job.domain.ModelProcessingJobEntity;
import io.repox.api3d.app.usecases.job.domain.ModelProcessingJobStatus;
import io.repox.api3d.app.usecases.job.domain.ModelProcessingJobType;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Client-facing view of a processing job. A completed job carries its deliverable in one of two ways:
 * {@link #outputAvailable()} is a convenience flag telling the caller a download is currently obtainable
 * (job completed, produced an output file, and it has not expired) so it does not have to infer this from
 * status alone; {@link #result()} carries the payload inline for job types that produce structured data
 * rather than a file, so polling alone is enough to collect it.
 */
public record JobStatusDto(
        UUID id,
        ModelProcessingJobType type,
        ModelProcessingJobStatus status,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        LocalDateTime expiresAt,
        boolean outputAvailable,
        JsonNode result,
        String error) {

    /**
     * @param result the job's deserialized structured result, or null when it has none
     */
    public static JobStatusDto from(ModelProcessingJobEntity job, LocalDateTime now, JsonNode result) {
        boolean completed = job.getStatus() == ModelProcessingJobStatus.COMPLETED;
        boolean outputAvailable = completed
                && job.getOutputFile() != null
                && job.getExpiresAt() != null
                && job.getExpiresAt().isAfter(now);
        return new JobStatusDto(
                job.getId(),
                job.getType(),
                job.getStatus(),
                job.getCreatedAt(),
                job.getStartedAt(),
                job.getEndedAt(),
                job.getExpiresAt(),
                outputAvailable,
                completed ? result : null,
                job.getErrorMessage());
    }
}
