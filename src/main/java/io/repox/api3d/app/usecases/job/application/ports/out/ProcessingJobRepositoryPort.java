package io.repox.api3d.app.usecases.job.application.ports.out;

import io.repox.api3d.app.usecases.job.domain.ModelProcessingJobEntity;
import io.repox.api3d.app.usecases.job.domain.ModelProcessingJobStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcessingJobRepositoryPort {

    /** Inserts a new job or updates an existing (detached) one. */
    ModelProcessingJobEntity save(ModelProcessingJobEntity job);

    Optional<ModelProcessingJobEntity> findById(UUID id);

    List<ModelProcessingJobEntity> findByStatusIn(Collection<ModelProcessingJobStatus> statuses);

    /**
     * Returns the {@code output_file} names of jobs whose deliverable must be kept: completed, with an
     * output file, not yet expired at {@code now}. Used as the retention sweep's keep-list.
     */
    List<String> findKeepableOutputFiles(LocalDateTime now);
}
