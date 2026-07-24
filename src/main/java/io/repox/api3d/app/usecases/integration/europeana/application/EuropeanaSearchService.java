package io.repox.api3d.app.usecases.integration.europeana.application;

import io.repox.api3d.app.usecases.integration.europeana.application.ports.in.EuropeanaSearchUseCase;
import io.repox.api3d.app.usecases.integration.europeana.application.ports.out.EuropeanaApiPort;
import io.repox.api3d.app.usecases.integration.europeana.domain.EuropeanaClientSettings;
import io.repox.api3d.app.usecases.integration.europeana.domain.EuropeanaSearchRequest;
import io.repox.api3d.app.usecases.integration.europeana.domain.EuropeanaSearchResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
class EuropeanaSearchService implements EuropeanaSearchUseCase {
    private final EuropeanaClientSettings settings;
    private final EuropeanaApiPort europeanaApiPort;

    public EuropeanaSearchResponse search(EuropeanaSearchRequest request, String cursor) {
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(
                        "https://api.europeana.eu/record/v2/search.json")
                .queryParam("cursor", encodeParam(cursor))
                .queryParam("wskey", encodeParam(settings.apiKey()))
                .queryParam("query", encodeParam(request.query()))
                .queryParam("qf", encodeParam("contentTier:(1 OR 2 OR 3 OR 4)"))
                .queryParam("rows", encodeParam(String.valueOf(100)));
        request.typeOfMedia()
                .forEach(type ->
                        uriComponentsBuilder.queryParam("qf", encodeParam(MessageFormat.format("TYPE:\"{0}\"", type))));
        return europeanaApiPort.search(uriComponentsBuilder.build(true).toUri());
    }

    Object encodeParam(String o) {
        return URLEncoder.encode(o, StandardCharsets.UTF_8);
    }
}
