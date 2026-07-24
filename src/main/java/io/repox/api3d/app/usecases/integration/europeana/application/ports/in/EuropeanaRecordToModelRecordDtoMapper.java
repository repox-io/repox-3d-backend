package io.repox.api3d.app.usecases.integration.europeana.application.ports.in;

import io.repox.api3d.app.usecases.harvest.domain.ModelRecordDto;
import io.repox.api3d.app.usecases.integration.europeana.domain.EuropeanaRecord;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EuropeanaRecordToModelRecordDtoMapper {
    @Mapping(target = "title", source = "title")
    @Mapping(target = "description", source = "dcDescription")
    @Mapping(target = "externalId", source = "id")
    @Mapping(target = "externalUri", source = "guid")
    @Mapping(target = "embedUrl", source = "edmIsShownBy")
    @Mapping(target = "thumbnailUri", source = "edmPreview")
    ModelRecordDto mapFrom(EuropeanaRecord item);

    default String mapFrom(List<String> item) {
        return item.stream().findFirst().orElse(null);
    }
}
