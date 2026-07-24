package io.repox.api3d.app.usecases.integration.sketchfab.adapters.ports.out;

import io.repox.api3d.app.usecases.integration.sketchfab.application.ports.out.SketchFabDataApiPort;
import io.repox.api3d.app.usecases.integration.sketchfab.configuration.SketchFabConfig;
import io.repox.api3d.app.usecases.integration.sketchfab.domain.SketchFabModel;
import io.repox.api3d.app.usecases.integration.sketchfab.domain.SketchFabModelDownload;
import java.text.MessageFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
class SketchFabDataApiClient implements SketchFabDataApiPort {
    private final RestTemplate restTemplate;
    private final SketchFabConfig.SketchFabClientSettings sketchFabClientSettings;

    private final String SKETCHFAB_API_ENDPOINT = "https://api.sketchfab.com/v3/models";

    /**
     * Returns the detail of a model
     * @param uid The model UID
     * @return SketchFabModel
     * @throws HttpClientErrorException.Forbidden
     * @throws HttpClientErrorException.NotFound
     */
    @Override
    public SketchFabModel modelInfo(String uid)
            throws HttpClientErrorException.Forbidden, HttpClientErrorException.NotFound {
        return restTemplate
                .exchange(
                        UriComponentsBuilder.fromUriString(SKETCHFAB_API_ENDPOINT)
                                .path(uid)
                                .toUriString(),
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers()),
                        SketchFabModel.class)
                .getBody();
    }

    /**
     * Returns download information for downloadable models.
     * @param uid The model UID
     * @return Object
     * @throws HttpClientErrorException.BadRequest
     * @throws HttpClientErrorException.Unauthorized
     * @throws HttpClientErrorException.Forbidden
     * @throws HttpClientErrorException.NotFound
     * @throws HttpClientErrorException.TooManyRequests
     */
    @Override
    public SketchFabModelDownload modelDownload(String uid)
            throws HttpClientErrorException.BadRequest, HttpClientErrorException.Unauthorized,
                    HttpClientErrorException.Forbidden, HttpClientErrorException.NotFound,
                    HttpClientErrorException.TooManyRequests {
        return restTemplate
                .exchange(
                        UriComponentsBuilder.fromUriString(SKETCHFAB_API_ENDPOINT)
                                .pathSegment(uid, "download")
                                .toUriString(),
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers()),
                        SketchFabModelDownload.class)
                .getBody();
    }

    HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, MessageFormat.format("Token {0}", sketchFabClientSettings.apiKey()));
        return headers;
    }
}
