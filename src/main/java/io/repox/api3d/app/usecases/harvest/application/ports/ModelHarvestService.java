package io.repox.api3d.app.usecases.harvest.application.ports;

import io.repox.api3d.app.usecases.harvest.application.ports.in.ModelHarvestUseCase;
import io.repox.api3d.app.usecases.harvest.application.ports.out.ModelRecordFindPort;
import io.repox.api3d.app.usecases.harvest.application.ports.out.ModelRecordSavePort;
import io.repox.api3d.app.usecases.harvest.domain.AggregatorEnum;
import io.repox.api3d.app.usecases.harvest.domain.ModelRecordEntity;
import io.repox.api3d.app.usecases.integration.europeana.application.ports.in.EuropeanaSearchUseCase;
import io.repox.api3d.app.usecases.integration.europeana.domain.EuropeanaMediaTypeEnum;
import io.repox.api3d.app.usecases.integration.europeana.domain.EuropeanaRecord;
import io.repox.api3d.app.usecases.integration.europeana.domain.EuropeanaSearchRequest;
import io.repox.api3d.app.usecases.integration.europeana.domain.EuropeanaSearchResponse;
import io.repox.api3d.app.usecases.integration.sketchfab.domain.SketchFabSourceEnumRule;
import io.repox.api3d.app.usecases.integration.zenodo.domain.ZenodoSourceEnumRule;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelHarvestService implements ModelHarvestUseCase {
    private final EuropeanaSearchUseCase europeanaSearchUseCase;
    private final ModelRecordEntity.ModelRecordEntityMapper europeanaRecordToModelRecordEntityMapper;
    private final ModelRecordSavePort modelRecordSavePort;
    private final ModelRecordFindPort modelRecordFindPort;

    @Override
    public void harvestModels(AggregatorEnum aggregator) {
        if (AggregatorEnum.EUROPEANA == aggregator) {
            EuropeanaSearchRequest europeanaSearchRequest =
                    new EuropeanaSearchRequest("*:*", List.of(EuropeanaMediaTypeEnum._3D));
            EuropeanaSearchResponse res = europeanaSearchUseCase.search(europeanaSearchRequest, "*");
            mapAndSave(res.items());
            while (StringUtils.isNotBlank(res.nextCursor())) {
                res = europeanaSearchUseCase.search(europeanaSearchRequest, res.nextCursor());
                mapAndSave(res.items());
            }
        }
    }

    private void mapAndSave(List<EuropeanaRecord> europeanaRecords) {
        modelRecordFindPort
                .findAllByExternalIdIn(
                        europeanaRecords.stream().map(EuropeanaRecord::id).toList())
                .forEach(modelRecordSavePort::delete);
        modelRecordSavePort.saveAll(europeanaRecords.stream()
                .map(record -> europeanaRecordToModelRecordEntityMapper.mapFrom(
                        record, List.of(new SketchFabSourceEnumRule(), new ZenodoSourceEnumRule())))
                .toList());
    }
}
