package io.repox.api3d.app.usecases.harvest.application.ports.out;

import io.repox.api3d.app.usecases.harvest.domain.ModelRecordEntity;
import java.util.List;

public interface ModelRecordSavePort {
    void delete(ModelRecordEntity entity);

    void save(ModelRecordEntity entity);

    void saveAll(List<ModelRecordEntity> entityList);
}
