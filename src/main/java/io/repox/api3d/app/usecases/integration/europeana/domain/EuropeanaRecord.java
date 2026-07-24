package io.repox.api3d.app.usecases.integration.europeana.domain;

import java.util.List;
import java.util.Map;

public record EuropeanaRecord(
        List<String> dataProvider,
        List<String> dcCreator,
        Map<String, List<String>> dcCreatorLangAware,
        Map<String, List<String>> dcTitleLangAware,
        List<String> edmIsShownBy,
        List<String> edmPreview,
        List<String> edmPlaceLatitude,
        List<String> edmPlaceLongitude,
        int europeanaCompleteness,
        String guid,
        String id,
        List<String> language,
        String link,
        List<String> provider,
        List<String> rights,
        double score,
        List<String> dcDescription,
        List<String> title,
        String type,
        List<String> year) {}
