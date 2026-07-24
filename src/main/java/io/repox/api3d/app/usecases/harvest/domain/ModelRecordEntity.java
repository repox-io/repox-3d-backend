package io.repox.api3d.app.usecases.harvest.domain;

import io.repox.api3d.app.usecases.integration.europeana.domain.EuropeanaRecord;
import io.repox.api3d.app.utils.SelfValidating;
import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.util.CollectionUtils;

@Getter
@Setter
@Entity
@Table(name = "model_record")
public class ModelRecordEntity implements SelfValidating<ModelRecordEntity> {
    @Id
    UUID id;

    String title;
    String description;
    String provider;
    String place;
    Double latitude;
    Double longitude;
    String license;
    String creator;

    @Column(name = "creation_year")
    Integer creationYear;

    @Enumerated(EnumType.STRING)
    AggregatorEnum aggregator;

    @Column(name = "external_id")
    String externalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "external_source")
    ExternalSourceEnum externalSource;

    @Column(name = "external_uri")
    String externalUri;

    @Column(name = "thumbnail_uri")
    String thumbnailUri;

    @Mapper(
            componentModel = "spring",
            unmappedTargetPolicy = ReportingPolicy.IGNORE,
            imports = {AggregatorEnum.class, UUID.class, ExternalSourceEnumRule.class})
    public interface ModelRecordEntityMapper {
        @Mapping(target = "id", expression = "java(UUID.randomUUID())")
        @Mapping(target = "title", source = "item.title")
        @Mapping(target = "description", source = "item.dcDescription")
        @Mapping(target = "provider", source = "item.provider")
        @Mapping(
                target = "latitude",
                expression =
                        "java(mapFrom(item.edmPlaceLatitude()) != null ? Double.valueOf(mapFrom(item.edmPlaceLatitude())) : null)")
        @Mapping(
                target = "longitude",
                expression =
                        "java(mapFrom(item.edmPlaceLongitude()) != null ? Double.valueOf(mapFrom(item.edmPlaceLongitude())) : null)")
        @Mapping(target = "license", source = "item.rights")
        @Mapping(target = "creator", source = "item.dcCreator")
        @Mapping(target = "aggregator", expression = "java(AggregatorEnum.EUROPEANA)")
        @Mapping(target = "externalId", source = "item.id")
        @Mapping(target = "externalSource", expression = "java(mapExternalSource(item.edmIsShownBy(), rules))")
        @Mapping(target = "externalUri", source = "item.edmIsShownBy")
        @Mapping(target = "thumbnailUri", source = "item.edmPreview")
        ModelRecordEntity mapFrom(EuropeanaRecord item, List<ExternalSourceEnumRule> rules);

        default String mapFrom(List<String> item) {
            return !CollectionUtils.isEmpty(item) ? item.stream().findFirst().orElse(null) : null;
        }

        default ExternalSourceEnum mapExternalSource(List<String> item, List<ExternalSourceEnumRule> rules) {
            if (CollectionUtils.isEmpty(item)) {
                return null;
            }
            String edmIsShownBy = item.stream().findFirst().orElse("");
            return rules.stream()
                    .filter(externalSourceEnumRule ->
                            externalSourceEnumRule.getExternalSourceEnum(edmIsShownBy) != null)
                    .map(externalSourceEnumRule -> externalSourceEnumRule.getExternalSourceEnum(edmIsShownBy))
                    .findFirst()
                    .orElse(null);
        }
    }
}
