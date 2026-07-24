package io.repox.api3d.app.usecases.integration.europeana.domain;

import java.util.List;

public record EuropeanaSearchResponse(
        String apikey,
        Boolean success,
        Long requestNumber,
        Integer itemsCount,
        Long totalResults,
        String nextCursor,
        List<EuropeanaRecord> items) {}
