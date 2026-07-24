package io.repox.api3d.app.usecases.integration.europeana.application.ports.out;

import io.repox.api3d.app.usecases.integration.europeana.domain.EuropeanaSearchResponse;
import java.net.URI;

public interface EuropeanaApiPort {
    EuropeanaSearchResponse search(URI uriString);
}
