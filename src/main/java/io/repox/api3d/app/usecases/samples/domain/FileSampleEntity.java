package io.repox.api3d.app.usecases.samples.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Getter
@Setter
@Entity
@Table(name = "file_samples")
public class FileSampleEntity {
    @Id
    UUID uuid;

    @Column(length = 32)
    String label;

    @Column(columnDefinition = "text")
    String description;

    @Column(name = "model_uri", length = 512)
    String modelUri;

    @Column(name = "thumbnail_uri", length = 512)
    String thumbnailUri;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "applicable_tools", columnDefinition = "text[]")
    List<ToolEnum> applicableTools;

    @Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
    public interface FileSampleEntityMapper {
        FileSampleDto mapFrom(FileSampleEntity entity);
    }
}
