package io.repox.api3d.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import io.repox.api3d.app.usecases.job.application.JobStatusDto;
import io.repox.api3d.app.usecases.job.domain.ModelProcessingJobEntity;
import io.repox.api3d.app.usecases.job.domain.ModelProcessingJobStatus;
import io.repox.api3d.app.usecases.job.domain.ModelProcessingJobType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** Unit coverage for the JSON result channel added to the job pipeline. */
class JobResultChannelTests {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 7, 10, 0);
    private static final long RETENTION = 60;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void markCompletedWithResultOnlyHasNoExpiry() {
        ModelProcessingJobEntity job = ModelProcessingJobEntity.create(ModelProcessingJobType.MODEL_DETAILS, "{}", NOW);

        job.markCompleted(null, "{\"nodes\":5}", NOW, RETENTION);

        assertThat(job.getStatus()).isEqualTo(ModelProcessingJobStatus.COMPLETED);
        assertThat(job.getOutputFile()).isNull();
        assertThat(job.getResult()).isEqualTo("{\"nodes\":5}");
        // Expiry governs the storage sweep; a result-only job holds no file, so it never expires.
        assertThat(job.getExpiresAt()).isNull();
    }

    @Test
    void markCompletedWithOutputFileSetsExpiry() {
        ModelProcessingJobEntity job =
                ModelProcessingJobEntity.create(ModelProcessingJobType.MODEL_THUMBNAIL, "{}", NOW);

        job.markCompleted("thumb.png", null, NOW, RETENTION);

        assertThat(job.getExpiresAt()).isEqualTo(NOW.plusMinutes(RETENTION));
    }

    @Test
    void markCompletedWithNeitherFileNorResultHasNoExpiry() {
        ModelProcessingJobEntity job = ModelProcessingJobEntity.create(ModelProcessingJobType.MODEL_DETAILS, "{}", NOW);

        job.markCompleted(null, null, NOW, RETENTION);

        assertThat(job.getExpiresAt()).isNull();
    }

    @Test
    void jobStatusDtoSurfacesResultOnlyWhenCompleted() {
        ModelProcessingJobEntity job = ModelProcessingJobEntity.create(ModelProcessingJobType.MODEL_DETAILS, "{}", NOW);

        // Still running: the result must not leak even if one were somehow present.
        JobStatusDto running = JobStatusDto.from(job, NOW, IntNode.valueOf(5));
        assertThat(running.result()).isNull();
        assertThat(running.outputAvailable()).isFalse();

        job.markCompleted(null, "{\"nodes\":5}", NOW, RETENTION);
        JobStatusDto completed = JobStatusDto.from(job, NOW, IntNode.valueOf(5));
        assertThat(completed.result()).isEqualTo(IntNode.valueOf(5));
        // A result-only job produces no downloadable file.
        assertThat(completed.outputAvailable()).isFalse();
    }

    @Test
    void resultRoundTripsThroughJsonbStringForm() throws Exception {
        // Mirrors ProcessingJobService: the processor's JsonNode is stored as its string form, then re-read.
        String stored = objectMapper.valueToTree(new int[] {1, 2, 3}).toString();
        assertThat(objectMapper.readTree(stored)).isEqualTo(objectMapper.valueToTree(new int[] {1, 2, 3}));
    }
}
