package io.repox.api3d.app.usecases.integration.europeana.domain;

import java.util.List;

public record EuropeanaSearchRequest(String query, List<EuropeanaMediaTypeEnum> typeOfMedia) {}
