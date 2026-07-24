package io.repox.api3d.app.usecases.job.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A unit of asynchronous 3D-model processing. Created {@link ModelProcessingJobStatus#PENDING} on submit,
 * transitioned by the worker through {@code RUNNING} to a terminal {@code COMPLETED}/{@code FAILED}.
 *
 * <p>A completed job carries its deliverable in one of two ways: {@link #outputFile} (a filename in the
 * storage output directory) for jobs producing a file, or {@link #result} for jobs producing structured
 * data. {@link #expiresAt} tracks only the output file's retention window — the reason expiry exists is to
 * keep the storage sweep from filling up — so a result-only job has no expiry; its {@code result} lives in
 * the database and is returned for as long as the job row exists.
 */
@Getter
@Entity
@Table(name = "model_processing_job")
public class ModelProcessingJobEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    private ModelProcessingJobType type;

    @Enumerated(EnumType.STRING)
    private ModelProcessingJobStatus status;

    /** Serialized {@link JobInput} JSON (jsonb column). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String input;

    /** Filename of the produced deliverable within the storage output directory; null when no output. */
    private String outputFile;

    /** Serialized structured result JSON (jsonb column) for types that yield data rather than a file. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String result;

    private String errorMessage;

    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime expiresAt;

    protected ModelProcessingJobEntity() {}

    public static ModelProcessingJobEntity create(ModelProcessingJobType type, String input, LocalDateTime now) {
        ModelProcessingJobEntity job = new ModelProcessingJobEntity();
        job.id = UUID.randomUUID();
        job.type = type;
        job.status = ModelProcessingJobStatus.PENDING;
        job.input = input;
        job.createdAt = now;
        return job;
    }

    /** Marks the job as started (RUNNING). */
    public void markRunning(LocalDateTime now) {
        this.status = ModelProcessingJobStatus.RUNNING;
        this.startedAt = now;
    }

    /**
     * Marks the job as completed with its deliverable — an {@code outputFile}, a structured {@code result},
     * or neither. Only an output file expires ({@code retentionMinutes} after {@code now}), since expiry
     * governs the storage sweep; a result-only or empty completion records no expiry.
     */
    public void markCompleted(String outputFile, String result, LocalDateTime now, long retentionMinutes) {
        this.status = ModelProcessingJobStatus.COMPLETED;
        this.endedAt = now;
        this.outputFile = outputFile;
        this.result = result;
        this.expiresAt = outputFile == null ? null : now.plusMinutes(retentionMinutes);
    }

    /** Marks the job as failed with a short error message. */
    public void markFailed(String errorMessage, LocalDateTime now) {
        this.status = ModelProcessingJobStatus.FAILED;
        this.endedAt = now;
        this.errorMessage = errorMessage;
    }
}
