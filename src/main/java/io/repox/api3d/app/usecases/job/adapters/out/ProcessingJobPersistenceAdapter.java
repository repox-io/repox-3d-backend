package io.repox.api3d.app.usecases.job.adapters.out;

import io.repox.api3d.app.usecases.job.application.ports.out.ProcessingJobRepositoryPort;
import io.repox.api3d.app.usecases.job.domain.ModelProcessingJobEntity;
import io.repox.api3d.app.usecases.job.domain.ModelProcessingJobStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ProcessingJobPersistenceAdapter implements ProcessingJobRepositoryPort {

    private final ModelProcessingJobRepository repository;

    @Override
    public ModelProcessingJobEntity save(ModelProcessingJobEntity job) {
        return repository.save(job);
    }

    @Override
    public Optional<ModelProcessingJobEntity> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<ModelProcessingJobEntity> findByStatusIn(Collection<ModelProcessingJobStatus> statuses) {
        return statuses.isEmpty() ? List.of() : repository.findByStatusIn(statuses);
    }

    @Override
    public List<String> findKeepableOutputFiles(LocalDateTime now) {
        return repository.findOutputFilesByStatusAndNotExpired(ModelProcessingJobStatus.COMPLETED, now);
    }
}
