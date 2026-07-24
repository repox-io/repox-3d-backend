package io.repox.api3d.app.usecases.harvest.domain;

import java.time.LocalDateTime;
import java.util.List;

public record ModelRecordDto(
        String title,
        String description,
        List<String> addSubjectTerms,
        String languageCode,
        ModelPlace place,
        String temporal,
        List<String> periods,
        String license,
        String copyright,
        String creator,
        Integer creationYear,
        String creationDescription,
        List<String> creationProcess,
        List<String> relations,
        List<String> relationsLinks,
        List<String> isPartOf,
        String externalId,
        String externalSource,
        String externalUri,
        List<String> tags,
        String thumbnailUri,
        String embedUrl,
        String status,
        String publishedEdm,
        LocalDateTime publishedAt,
        String publishedBy,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy) {}
