package io.repox.api3d.app.usecases.integration.europeana.adapters.out;

import io.repox.api3d.app.usecases.integration.europeana.application.ports.out.EuropeanaApiPort;
import io.repox.api3d.app.usecases.integration.europeana.domain.EuropeanaSearchResponse;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
class EuropeanaClient implements EuropeanaApiPort {
    private final RestTemplate restTemplate;

    /**
     * See https://europeana.atlassian.net/wiki/spaces/EF/pages/2385739812/Search+API+Documentation#Request
     * for request parameter options.
     */
    @Override
    public EuropeanaSearchResponse search(URI uri) {
        return restTemplate
                .exchange(uri, HttpMethod.GET, null, EuropeanaSearchResponse.class)
                .getBody();
    }
}
