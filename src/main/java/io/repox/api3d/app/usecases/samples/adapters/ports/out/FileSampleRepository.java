package io.repox.api3d.app.usecases.samples.adapters.ports.out;

import io.repox.api3d.app.usecases.samples.domain.FileSampleEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
interface FileSampleRepository extends JpaRepository<FileSampleEntity, UUID> {

    @Query(
            value = """
                    select * from file_samples where :tool = any(applicable_tools)""",
            nativeQuery = true)
    List<FileSampleEntity> findAllByToolContaining(@Param("tool") String tool);
}
