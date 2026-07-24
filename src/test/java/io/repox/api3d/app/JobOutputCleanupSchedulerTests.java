package io.repox.api3d.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.repox.api3d.app.usecases.job.application.ports.out.ProcessingJobRepositoryPort;
import io.repox.api3d.app.usecases.job.configuration.JobOutputCleanupScheduler;
import io.repox.api3d.app.usecases.storage.application.ports.out.FileStoragePort;
import io.repox.api3d.app.utils.ApplicationClock;
import io.repox.api3d.appconfig.options.JobProperties;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JobOutputCleanupSchedulerTests {

    private final ProcessingJobRepositoryPort repository = mock(ProcessingJobRepositoryPort.class);
    private final FileStoragePort fileStorage = mock(FileStoragePort.class);
    private final JobProperties jobProperties = new JobProperties(); // retention 60 min
    private final Instant now = Instant.parse("2026-07-07T10:00:00Z");
    private final ApplicationClock clock = new ApplicationClock(Clock.fixed(now, ZoneOffset.UTC), ZoneId.of("UTC"));

    private final JobOutputCleanupScheduler scheduler =
            new JobOutputCleanupScheduler(repository, fileStorage, jobProperties, clock);

    @Test
    void deletesStaleOrphansButKeepsWhitelistedAndYoungFiles(@TempDir Path outputDir) throws Exception {
        File keep = Files.createFile(outputDir.resolve("keep_compressed.glb")).toFile();
        File staleOrphan =
                Files.createFile(outputDir.resolve("stale_orphan.glb")).toFile();
        File youngOrphan =
                Files.createFile(outputDir.resolve("young_orphan.glb")).toFile();

        keep.setLastModified(now.toEpochMilli() - Duration.ofHours(3).toMillis());
        staleOrphan.setLastModified(now.toEpochMilli() - Duration.ofHours(2).toMillis());
        youngOrphan.setLastModified(now.toEpochMilli()); // just produced -> protected by age guard

        when(fileStorage.getOutputDirectory()).thenReturn(outputDir.toFile());
        when(repository.findKeepableOutputFiles(any())).thenReturn(List.of("keep_compressed.glb"));

        scheduler.sweepExpiredOutputs();

        assertThat(keep).exists(); // whitelisted -> kept even though old
        assertThat(staleOrphan).doesNotExist(); // not whitelisted and older than retention -> swept
        assertThat(youngOrphan).exists(); // not whitelisted but too young -> protected
    }
}
