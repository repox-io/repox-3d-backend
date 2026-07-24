package io.repox.api3d.app.usecases.job.adapters.out;

import io.repox.api3d.app.usecases.job.domain.ModelProcessingJobEntity;
import io.repox.api3d.app.usecases.job.domain.ModelProcessingJobStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
interface ModelProcessingJobRepository extends JpaRepository<ModelProcessingJobEntity, UUID> {

    List<ModelProcessingJobEntity> findByStatusIn(Collection<ModelProcessingJobStatus> statuses);

    @Query(
            """
            select j.outputFile from ModelProcessingJobEntity j
            where j.status = :status and j.outputFile is not null and j.expiresAt > :now""")
    List<String> findOutputFilesByStatusAndNotExpired(
            @Param("status") ModelProcessingJobStatus status, @Param("now") LocalDateTime now);
}
