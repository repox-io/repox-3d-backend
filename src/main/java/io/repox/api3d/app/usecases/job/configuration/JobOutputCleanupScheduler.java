package io.repox.api3d.app.usecases.job.configuration;

import io.repox.api3d.app.usecases.job.application.ports.out.ProcessingJobRepositoryPort;
import io.repox.api3d.app.usecases.storage.application.ports.out.FileStoragePort;
import io.repox.api3d.app.utils.ApplicationClock;
import io.repox.api3d.appconfig.options.JobProperties;
import java.io.File;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically removes stale files from the storage output directory. A file is deleted only when it is
 * <em>not</em> the live output of a completed, unexpired job <em>and</em> is older than the retention
 * window. The age guard protects the in-progress output of a RUNNING job (and a just-finished file racing
 * its DB commit) from being swept — safe because the retention window exceeds any single job's runtime.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobOutputCleanupScheduler {

    private final ProcessingJobRepositoryPort repository;
    private final FileStoragePort fileStorage;
    private final JobProperties jobProperties;
    private final ApplicationClock clock;

    @Scheduled(fixedDelayString = "${options.job.cleanup-interval-minutes}", timeUnit = TimeUnit.MINUTES)
    public void sweepExpiredOutputs() {
        File outputDir = fileStorage.getOutputDirectory();
        File[] files = outputDir.listFiles();
        if (files == null) {
            log.warn("Output directory not accessible for retention sweep: {}", outputDir.getAbsolutePath());
            return;
        }
        Set<String> keep = new HashSet<>(repository.findKeepableOutputFiles(clock.now()));
        long ageGuardMillis =
                Duration.ofMinutes(jobProperties.getOutputRetentionMinutes()).toMillis();
        long now = clock.instant().toEpochMilli();

        int deleted = 0;
        for (File file : files) {
            if (!file.isFile() || keep.contains(file.getName())) {
                continue;
            }
            if (now - file.lastModified() < ageGuardMillis) {
                continue; // too young to be an orphan — could belong to an in-flight job
            }
            if (file.delete()) {
                deleted++;
            } else {
                log.warn("Could not delete expired output file: {}", file.getAbsolutePath());
            }
        }
        if (deleted > 0) {
            log.info("Output retention sweep removed {} expired file(s) from {}", deleted, outputDir.getAbsolutePath());
        }
    }
}
