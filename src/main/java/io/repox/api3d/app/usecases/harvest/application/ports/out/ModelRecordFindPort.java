package io.repox.api3d.app.usecases.harvest.application.ports.out;

import io.repox.api3d.app.usecases.harvest.domain.ExternalSourceEnum;
import io.repox.api3d.app.usecases.harvest.domain.ModelRecordEntity;
import java.util.List;

public interface ModelRecordFindPort {
    List<ModelRecordEntity> findAllByExternalIdIn(List<String> externalIdList);

    List<String> findAllExternalUris();

    List<ModelRecordEntity> findAllByExternalSource(ExternalSourceEnum externalSource);
}
