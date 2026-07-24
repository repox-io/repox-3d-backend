package io.repox.api3d.app.usecases.job.configuration;

import io.repox.api3d.app.usecases.job.application.ports.out.ProcessingJobRepositoryPort;
import io.repox.api3d.app.usecases.job.domain.ModelProcessingJobEntity;
import io.repox.api3d.app.usecases.job.domain.ModelProcessingJobStatus;
import io.repox.api3d.app.utils.ApplicationClock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Reconciles jobs left non-terminal by a previous run. A crash or restart leaves {@code PENDING}/
 * {@code RUNNING} jobs with no worker (and their input files wiped from the input directory on startup),
 * so they can never complete — mark them {@code FAILED} once the application is ready.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobStartupReconciler {

    private final ProcessingJobRepositoryPort repository;
    private final ApplicationClock clock;

    @EventListener(ApplicationReadyEvent.class)
    void failOrphanedJobsOnStartup() {
        List<ModelProcessingJobEntity> orphans =
                repository.findByStatusIn(List.of(ModelProcessingJobStatus.PENDING, ModelProcessingJobStatus.RUNNING));
        if (orphans.isEmpty()) {
            return;
        }
        for (ModelProcessingJobEntity job : orphans) {
            job.markFailed("Interrupted by application restart", clock.now());
            repository.save(job);
        }
        log.info("Reconciled {} orphaned job(s) to FAILED on startup", orphans.size());
    }
}
