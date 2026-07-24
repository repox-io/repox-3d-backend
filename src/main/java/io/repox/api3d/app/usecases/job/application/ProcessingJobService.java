package io.repox.api3d.app.usecases.job.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.repox.api3d.app.usecases.job.application.ports.in.ProcessingJobUseCase;
import io.repox.api3d.app.usecases.job.application.ports.out.ModelJobProcessor;
import io.repox.api3d.app.usecases.job.application.ports.out.ModelJobProcessor.ProcessingOutput;
import io.repox.api3d.app.usecases.job.application.ports.out.ProcessingJobRepositoryPort;
import io.repox.api3d.app.usecases.job.domain.JobInput;
import io.repox.api3d.app.usecases.job.domain.ModelProcessingJobEntity;
import io.repox.api3d.app.usecases.job.domain.ModelProcessingJobType;
import io.repox.api3d.app.usecases.storage.application.ports.out.FileStoragePort;
import io.repox.api3d.app.utils.ApplicationClock;
import io.repox.api3d.app.utils.RandomFilenamePrefix;
import io.repox.api3d.app.utils.UrlSourceUtils;
import io.repox.api3d.app.utils.exception.KnownRuntimeError;
import io.repox.api3d.appconfig.options.JobProperties;
import java.io.File;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Orchestrates asynchronous processing jobs: persists a {@code PENDING} job on submit, hands it to a bounded
 * background executor, and drives it through {@code RUNNING} to a terminal state via the type's
 * {@link ModelJobProcessor}. The produced output file is intentionally retained (the processor does not
 * delete it); its lifecycle is governed by the output retention sweep.
 */
@Slf4j
@Service
public class ProcessingJobService implements ProcessingJobUseCase {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;

    private final ProcessingJobRepositoryPort repository;
    private final FileStoragePort fileStorage;
    private final UrlSourceUtils urlSourceUtils;
    private final ObjectMapper objectMapper;
    private final ApplicationClock clock;
    private final JobProperties jobProperties;
    private final ExecutorService jobExecutor;
    private final Map<ModelProcessingJobType, ModelJobProcessor> processors;

    public ProcessingJobService(
            ProcessingJobRepositoryPort repository,
            FileStoragePort fileStorage,
            UrlSourceUtils urlSourceUtils,
            ObjectMapper objectMapper,
            ApplicationClock clock,
            JobProperties jobProperties,
            @Qualifier("jobExecutor") ExecutorService jobExecutor,
            List<ModelJobProcessor> processors) {
        this.repository = repository;
        this.fileStorage = fileStorage;
        this.urlSourceUtils = urlSourceUtils;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.jobProperties = jobProperties;
        this.jobExecutor = jobExecutor;
        this.processors = new EnumMap<>(ModelProcessingJobType.class);
        for (ModelJobProcessor processor : processors) {
            this.processors.put(processor.type(), processor);
        }
    }

    @Override
    public JobStatusDto submit(ModelProcessingJobType type, String url, Object options) {
        // Cheap submit-time guard: confirm the URL resolves (HEAD, no body download) so an obviously bad
        // link is rejected up front. The content is downloaded and validated lazily by the worker, so the
        // request thread does not wait for the (potentially large) download.
        if (!urlSourceUtils.remoteResourceExists(url)) {
            throw new KnownRuntimeError("Source URL is not reachable or the content does not exist: " + url);
        }
        return enqueue(type, JobInput.fromUrl(url, toJsonNode(options)));
    }

    @Override
    public JobStatusDto submitWithUpload(ModelProcessingJobType type, MultipartFile file, Object options) {
        File stored;
        try (InputStream inputStream = file.getInputStream()) {
            stored = fileStorage.save(inputStream, file.getOriginalFilename(), true);
        } catch (Exception e) {
            log.error("Could not store uploaded file for {} job: {}", type, e.getMessage(), e);
            throw new RuntimeException(e);
        }
        // Fail fast on an upload the processor already knows is doomed (e.g. a Draco input for meshopt
        // compression): reject before a job is created, cleaning up the stored file. URL sources are not
        // pre-validated here — the worker downloads and validates them when it runs.
        validateUploadOrCleanup(type, stored);
        return enqueue(type, JobInput.fromUpload(stored.getName(), toJsonNode(options)));
    }

    /**
     * Runs the type's upload guard before a job is created, so an uploaded input that is certain to fail
     * (e.g. a Draco-compressed model for meshopt compression) is rejected with a 4xx instead of producing a
     * doomed job. On rejection the stored file is deleted and the {@code KnownRuntimeError} rethrown; types
     * without a guard default to a no-op.
     */
    private void validateUploadOrCleanup(ModelProcessingJobType type, File stored) {
        ModelJobProcessor processor = processors.get(type);
        if (processor == null) {
            return;
        }
        try {
            processor.validateUpload(stored);
        } catch (RuntimeException e) {
            fileStorage.delete(stored);
            throw e;
        }
    }

    private JobStatusDto enqueue(ModelProcessingJobType type, JobInput input) {
        ModelProcessingJobEntity job = ModelProcessingJobEntity.create(type, serialize(input), clock.now());
        repository.save(job);
        UUID id = job.getId();
        jobExecutor.submit(() -> runJob(id));
        log.info("Submitted {} job {}", type, id);
        return toDto(job);
    }

    /** Executes one job on a worker thread; every status transition is persisted. */
    private void runJob(UUID id) {
        ModelProcessingJobEntity job = repository.findById(id).orElse(null);
        if (job == null) {
            log.error("Job {} vanished before it could be processed", id);
            return;
        }
        job.markRunning(clock.now());
        repository.save(job);
        try {
            ModelJobProcessor processor = processors.get(job.getType());
            if (processor == null) {
                throw new IllegalStateException("No processor registered for job type " + job.getType());
            }
            ProcessingOutput output = processor.process(job);
            String outputFile =
                    output.outputFile() == null ? null : output.outputFile().getName();
            String result = output.result() == null ? null : output.result().toString();
            job.markCompleted(outputFile, result, clock.now(), jobProperties.getOutputRetentionMinutes());
            repository.save(job);
            log.info("Job {} ({}) completed{}", id, job.getType(), outputFile == null ? "" : " -> " + outputFile);
        } catch (Exception e) {
            log.error("Job {} ({}) failed: {}", id, job.getType(), e.getMessage(), e);
            job.markFailed(shortMessage(e), clock.now());
            repository.save(job);
        }
    }

    @Override
    public Optional<JobStatusDto> getJob(UUID id) {
        return repository.findById(id).map(this::toDto);
    }

    /** Builds the client-facing view, deserializing the stored result so it renders as JSON, not a string. */
    private JobStatusDto toDto(ModelProcessingJobEntity job) {
        return JobStatusDto.from(job, clock.now(), deserializeResult(job));
    }

    private JsonNode deserializeResult(ModelProcessingJobEntity job) {
        if (job.getResult() == null) {
            return null;
        }
        try {
            return objectMapper.readTree(job.getResult());
        } catch (Exception e) {
            // The column is jsonb, so this should be unreachable; degrade to no result rather than
            // failing the whole status response.
            log.error("Could not read stored result for job {}: {}", job.getId(), e.getMessage(), e);
            return null;
        }
    }

    @Override
    public JobOutputResolution resolveOutput(UUID id) {
        Optional<ModelProcessingJobEntity> found = repository.findById(id);
        if (found.isEmpty()) {
            return JobOutputResolution.of(JobOutputResolution.Outcome.NOT_FOUND);
        }
        ModelProcessingJobEntity job = found.get();
        return switch (job.getStatus()) {
            case PENDING, RUNNING -> JobOutputResolution.of(JobOutputResolution.Outcome.NOT_FINISHED);
            case FAILED -> JobOutputResolution.of(JobOutputResolution.Outcome.FAILED);
            case COMPLETED -> resolveCompletedOutput(job);
        };
    }

    private JobOutputResolution resolveCompletedOutput(ModelProcessingJobEntity job) {
        if (!job.getType().producesOutput() || job.getOutputFile() == null) {
            return JobOutputResolution.of(JobOutputResolution.Outcome.NO_OUTPUT);
        }
        if (job.getExpiresAt() == null || !job.getExpiresAt().isAfter(clock.now())) {
            return JobOutputResolution.of(JobOutputResolution.Outcome.EXPIRED);
        }
        File file = new File(fileStorage.getOutputDirectory(), job.getOutputFile());
        if (!file.isFile()) {
            return JobOutputResolution.of(JobOutputResolution.Outcome.OUTPUT_MISSING);
        }
        return JobOutputResolution.available(file, RandomFilenamePrefix.strip(job.getOutputFile()));
    }

    private JsonNode toJsonNode(Object options) {
        return objectMapper.valueToTree(options);
    }

    private String serialize(JobInput input) {
        try {
            return objectMapper.writeValueAsString(input);
        } catch (Exception e) {
            throw new RuntimeException("Could not serialize job input", e);
        }
    }

    private String shortMessage(Exception e) {
        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return message.length() > MAX_ERROR_MESSAGE_LENGTH ? message.substring(0, MAX_ERROR_MESSAGE_LENGTH) : message;
    }
}
