package io.repox.api3d.app.usecases.harvest.adapters.ports.out;

import io.repox.api3d.app.usecases.harvest.domain.ModelRecordEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface ModelRecordPersistenceRepository extends JpaRepository<ModelRecordEntity, UUID> {
    List<ModelRecordEntity> findAllByExternalIdIn(List<String> externalIdList);
}
