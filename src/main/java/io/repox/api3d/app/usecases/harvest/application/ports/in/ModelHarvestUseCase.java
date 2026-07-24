package io.repox.api3d.app.usecases.harvest.application.ports.in;

import io.repox.api3d.app.usecases.harvest.domain.AggregatorEnum;

public interface ModelHarvestUseCase {
    void harvestModels(AggregatorEnum aggregator);
}
