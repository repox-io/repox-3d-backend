package io.repox.api3d.app.usecases.job.application.ports.in;

import io.repox.api3d.app.usecases.job.application.JobOutputResolution;
import io.repox.api3d.app.usecases.job.application.JobStatusDto;
import io.repox.api3d.app.usecases.job.domain.ModelProcessingJobType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

/**
 * Inbound port for asynchronous model-processing jobs. Submissions return immediately with a
 * {@link JobStatusDto}; work runs in the background. Callers poll {@link #getJob(UUID)} and fetch the
 * result via {@link #resolveOutput(UUID)}.
 */
public interface ProcessingJobUseCase {

    /** Submits a job whose source model is fetched from {@code url}. {@code options} is type-specific. */
    JobStatusDto submit(ModelProcessingJobType type, String url, Object options);

    /** Submits a job for an uploaded model; the bytes are stored synchronously before returning. */
    JobStatusDto submitWithUpload(ModelProcessingJobType type, MultipartFile file, Object options);

    Optional<JobStatusDto> getJob(UUID id);

    JobOutputResolution resolveOutput(UUID id);
}
