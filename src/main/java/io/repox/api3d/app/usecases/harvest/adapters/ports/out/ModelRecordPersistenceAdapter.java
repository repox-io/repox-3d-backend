package io.repox.api3d.app.usecases.harvest.adapters.ports.out;

import io.repox.api3d.app.usecases.harvest.application.ports.out.ModelRecordFindPort;
import io.repox.api3d.app.usecases.harvest.application.ports.out.ModelRecordSavePort;
import io.repox.api3d.app.usecases.harvest.domain.ExternalSourceEnum;
import io.repox.api3d.app.usecases.harvest.domain.ModelRecordEntity;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Transactional
@Slf4j
@Component
@RequiredArgsConstructor
class ModelRecordPersistenceAdapter implements ModelRecordSavePort, ModelRecordFindPort {
    private final EntityManager em;
    private final ModelRecordPersistenceRepository modelRecordPersistenceRepository;

    @Override
    public void delete(ModelRecordEntity entity) {
        modelRecordPersistenceRepository.delete(entity);
    }

    @Transactional
    @Override
    public void save(ModelRecordEntity modelRecordEntity) {
        modelRecordPersistenceRepository.save(modelRecordEntity);
    }

    @Transactional
    @Override
    public void saveAll(List<ModelRecordEntity> entityList) {
        modelRecordPersistenceRepository.saveAll(entityList);
    }

    @Transactional
    @Override
    public List<ModelRecordEntity> findAllByExternalIdIn(List<String> externalIdList) {
        return modelRecordPersistenceRepository.findAllByExternalIdIn(externalIdList);
    }

    @Override
    public List<String> findAllExternalUris() {
        return em.createQuery(
                        """
                        select m.externalUri from ModelRecordEntity m""", String.class)
                .getResultList();
    }

    @Override
    public List<ModelRecordEntity> findAllByExternalSource(ExternalSourceEnum externalSource) {
        return em.createQuery(
                        """
                        from ModelRecordEntity m where m.externalSource = :externalSource""",
                        ModelRecordEntity.class)
                .setParameter("externalSource", externalSource)
                .getResultList();
    }
}
