package io.repox.api3d.app.usecases.integration.europeana.application.ports.in;

import io.repox.api3d.app.usecases.integration.europeana.domain.EuropeanaSearchRequest;
import io.repox.api3d.app.usecases.integration.europeana.domain.EuropeanaSearchResponse;

public interface EuropeanaSearchUseCase {
    EuropeanaSearchResponse search(EuropeanaSearchRequest request, String cursor);
}
