package io.repox.api3d.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.repox.api3d.app.usecases.job.application.JobOutputResolution;
import io.repox.api3d.app.usecases.job.application.JobStatusDto;
import io.repox.api3d.app.usecases.job.application.ProcessingJobService;
import io.repox.api3d.app.usecases.job.application.ports.out.ModelJobProcessor;
import io.repox.api3d.app.usecases.job.application.ports.out.ProcessingJobRepositoryPort;
import io.repox.api3d.app.usecases.job.domain.ModelProcessingJobEntity;
import io.repox.api3d.app.usecases.job.domain.ModelProcessingJobStatus;
import io.repox.api3d.app.usecases.job.domain.ModelProcessingJobType;
import io.repox.api3d.app.usecases.storage.application.ports.out.FileStoragePort;
import io.repox.api3d.app.utils.ApplicationClock;
import io.repox.api3d.app.utils.UrlSourceUtils;
import io.repox.api3d.app.utils.exception.KnownRuntimeError;
import io.repox.api3d.appconfig.options.JobProperties;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.multipart.MultipartFile;

class ProcessingJobServiceTests {

    private final InMemoryJobRepository repository = new InMemoryJobRepository();
    private final FileStoragePort fileStorage = mock(FileStoragePort.class);
    private final UrlSourceUtils urlSourceUtils = mock(UrlSourceUtils.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ApplicationClock clock =
            new ApplicationClock(Clock.fixed(Instant.parse("2026-07-07T10:00:00Z"), ZoneOffset.UTC), ZoneId.of("UTC"));
    private final JobProperties jobProperties = new JobProperties();
    private final ExecutorService directExecutor = new DirectExecutorService();

    private ProcessingJobService service(ModelJobProcessor processor) {
        return new ProcessingJobService(
                repository,
                fileStorage,
                urlSourceUtils,
                objectMapper,
                clock,
                jobProperties,
                directExecutor,
                List.of(processor));
    }

    /** URL submissions do a HEAD existence check at submit time; let it pass by default. */
    @BeforeEach
    void stubUrlExists() {
        when(urlSourceUtils.remoteResourceExists(any())).thenReturn(true);
    }

    @Test
    void urlJobRunsToCompletionWithOutputAndExpiry() {
        ModelJobProcessor processor = processor(
                ModelProcessingJobType.MESH_COMPRESS,
                job -> ModelJobProcessor.ProcessingOutput.of(new File("model_compressed.glb"), "model_compressed.glb"));

        JobStatusDto dto =
                service(processor).submit(ModelProcessingJobType.MESH_COMPRESS, "http://example/model.glb", "OPTS");

        ModelProcessingJobEntity job = repository.findById(dto.id()).orElseThrow();
        assertThat(job.getStatus()).isEqualTo(ModelProcessingJobStatus.COMPLETED);
        assertThat(job.getOutputFile()).isEqualTo("model_compressed.glb");
        assertThat(job.getEndedAt()).isEqualTo(LocalDateTime.of(2026, 7, 7, 10, 0));
        assertThat(job.getExpiresAt())
                .isEqualTo(job.getEndedAt().plusMinutes(jobProperties.getOutputRetentionMinutes()));
    }

    @Test
    void jobFailsWhenProcessorThrows() {
        ModelJobProcessor processor = processor(ModelProcessingJobType.MESH_COMPRESS, job -> {
            throw new IllegalStateException("gltfpack blew up");
        });

        JobStatusDto dto =
                service(processor).submit(ModelProcessingJobType.MESH_COMPRESS, "http://example/model.glb", "OPTS");

        ModelProcessingJobEntity job = repository.findById(dto.id()).orElseThrow();
        assertThat(job.getStatus()).isEqualTo(ModelProcessingJobStatus.FAILED);
        assertThat(job.getErrorMessage()).contains("gltfpack blew up");
        assertThat(job.getOutputFile()).isNull();
        assertThat(job.getExpiresAt()).isNull();
    }

    @Test
    void uploadRejectedByValidationIsNotEnqueuedAndStoredFileDeleted() throws Exception {
        File stored = Files.createTempFile("rejected-upload", ".glb").toFile();
        stored.deleteOnExit();
        when(fileStorage.save(any(), any(), anyBoolean())).thenReturn(stored);
        ModelJobProcessor rejecting = new ModelJobProcessor() {
            @Override
            public ModelProcessingJobType type() {
                return ModelProcessingJobType.MESH_COMPRESS;
            }

            @Override
            public ProcessingOutput process(ModelProcessingJobEntity job) {
                return ProcessingOutput.none();
            }

            @Override
            public void validateUpload(File inputFile) {
                throw new KnownRuntimeError("already Draco-compressed");
            }
        };

        assertThatThrownBy(() ->
                        service(rejecting).submitWithUpload(ModelProcessingJobType.MESH_COMPRESS, multipart(), "OPTS"))
                .isInstanceOf(KnownRuntimeError.class);

        verify(fileStorage).delete(stored); // the stored upload is cleaned up
        assertThat(repository.findByStatusIn(Arrays.asList(ModelProcessingJobStatus.values())))
                .isEmpty(); // no doomed job was persisted
    }

    @Test
    void urlThatDoesNotResolveIsRejectedAndNotEnqueued() {
        when(urlSourceUtils.remoteResourceExists(any())).thenReturn(false);

        assertThatThrownBy(() -> service(noopProcessor())
                        .submit(ModelProcessingJobType.MESH_COMPRESS, "http://example/missing.glb", "OPTS"))
                .isInstanceOf(KnownRuntimeError.class);

        assertThat(repository.findByStatusIn(Arrays.asList(ModelProcessingJobStatus.values())))
                .isEmpty(); // no job was persisted for an unreachable URL
    }

    @Test
    void uploadPassingValidationIsEnqueuedAndRuns() throws Exception {
        File stored = Files.createTempFile("ok-upload", ".glb").toFile();
        stored.deleteOnExit();
        when(fileStorage.save(any(), any(), anyBoolean())).thenReturn(stored);
        ModelJobProcessor processor = processor(
                ModelProcessingJobType.MESH_COMPRESS,
                job -> ModelJobProcessor.ProcessingOutput.of(new File("out.glb"), "out.glb"));

        JobStatusDto dto =
                service(processor).submitWithUpload(ModelProcessingJobType.MESH_COMPRESS, multipart(), "OPTS");

        ModelProcessingJobEntity job = repository.findById(dto.id()).orElseThrow();
        assertThat(job.getStatus()).isEqualTo(ModelProcessingJobStatus.COMPLETED);
    }

    @Test
    void resolveOutputReturnsAvailableForCompletedJob(@TempDir Path outputDir) throws Exception {
        File output =
                Files.createFile(outputDir.resolve("model_compressed.glb")).toFile();
        when(fileStorage.getOutputDirectory()).thenReturn(outputDir.toFile());
        ModelJobProcessor processor = processor(
                ModelProcessingJobType.MESH_COMPRESS,
                job -> ModelJobProcessor.ProcessingOutput.of(output, "model_compressed.glb"));
        ProcessingJobService service = service(processor);
        UUID id = service.submit(ModelProcessingJobType.MESH_COMPRESS, "http://example/model.glb", "OPTS")
                .id();

        JobOutputResolution resolution = service.resolveOutput(id);

        assertThat(resolution.outcome()).isEqualTo(JobOutputResolution.Outcome.AVAILABLE);
        assertThat(resolution.file()).isEqualTo(output);
        assertThat(resolution.downloadName()).isEqualTo("model_compressed.glb");
    }

    @Test
    void resultProducingJobStoresResultAndReportsNoDownloadableOutput() {
        ModelJobProcessor processor = processor(
                ModelProcessingJobType.MODEL_DETAILS,
                job -> ModelJobProcessor.ProcessingOutput.ofResult(objectMapper.valueToTree(Map.of("nodes", 5))));
        ProcessingJobService service = service(processor);

        UUID id = service.submit(ModelProcessingJobType.MODEL_DETAILS, "http://example/model.ply", null)
                .id();

        JobStatusDto dto = service.getJob(id).orElseThrow();
        assertThat(dto.status()).isEqualTo(ModelProcessingJobStatus.COMPLETED);
        assertThat(dto.result().get("nodes").asInt()).isEqualTo(5);
        assertThat(dto.outputAvailable()).isFalse();
        // A result-only type is not downloadable via the file endpoint.
        assertThat(service.resolveOutput(id).outcome()).isEqualTo(JobOutputResolution.Outcome.NO_OUTPUT);
    }

    @Test
    void resolveOutputReflectsJobStateForNonHappyCases() {
        assertThat(service(noopProcessor()).resolveOutput(UUID.randomUUID()).outcome())
                .isEqualTo(JobOutputResolution.Outcome.NOT_FOUND);

        // Failed job -> FAILED outcome
        ModelJobProcessor failing = processor(ModelProcessingJobType.MESH_COMPRESS, job -> {
            throw new IllegalStateException("boom");
        });
        UUID failedId = service(failing)
                .submit(ModelProcessingJobType.MESH_COMPRESS, "http://example/model.glb", "OPTS")
                .id();
        assertThat(service(failing).resolveOutput(failedId).outcome()).isEqualTo(JobOutputResolution.Outcome.FAILED);
    }

    private ModelJobProcessor processor(ModelProcessingJobType type, ProcessorFunction fn) {
        return new ModelJobProcessor() {
            @Override
            public ModelProcessingJobType type() {
                return type;
            }

            @Override
            public ProcessingOutput process(ModelProcessingJobEntity job) throws Exception {
                return fn.apply(job);
            }
        };
    }

    private MultipartFile multipart() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[] {1, 2, 3}));
        when(file.getOriginalFilename()).thenReturn("model.glb");
        return file;
    }

    private ModelJobProcessor noopProcessor() {
        return processor(ModelProcessingJobType.MESH_COMPRESS, job -> ModelJobProcessor.ProcessingOutput.none());
    }

    @FunctionalInterface
    private interface ProcessorFunction {
        ModelJobProcessor.ProcessingOutput apply(ModelProcessingJobEntity job) throws Exception;
    }

    /** Runs submitted tasks inline so job execution is deterministic within the test. */
    private static final class DirectExecutorService extends AbstractExecutorService {
        @Override
        public void execute(Runnable command) {
            command.run();
        }

        @Override
        public void shutdown() {}

        @Override
        public List<Runnable> shutdownNow() {
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }
    }

    private static final class InMemoryJobRepository implements ProcessingJobRepositoryPort {
        private final Map<UUID, ModelProcessingJobEntity> store = new HashMap<>();

        @Override
        public ModelProcessingJobEntity save(ModelProcessingJobEntity job) {
            store.put(job.getId(), job);
            return job;
        }

        @Override
        public Optional<ModelProcessingJobEntity> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<ModelProcessingJobEntity> findByStatusIn(Collection<ModelProcessingJobStatus> statuses) {
            List<ModelProcessingJobEntity> matches = new ArrayList<>();
            for (ModelProcessingJobEntity job : store.values()) {
                if (statuses.contains(job.getStatus())) {
                    matches.add(job);
                }
            }
            return matches;
        }

        @Override
        public List<String> findKeepableOutputFiles(LocalDateTime now) {
            List<String> keep = new ArrayList<>();
            for (ModelProcessingJobEntity job : store.values()) {
                if (job.getStatus() == ModelProcessingJobStatus.COMPLETED
                        && job.getOutputFile() != null
                        && job.getExpiresAt() != null
                        && job.getExpiresAt().isAfter(now)) {
                    keep.add(job.getOutputFile());
                }
            }
            return keep;
        }
    }
}
