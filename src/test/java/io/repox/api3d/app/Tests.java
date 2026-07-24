package io.repox.api3d.app;

import io.repox.api3d.app.usecases.harvest.application.ports.out.ModelRecordFindPort;
import io.repox.api3d.app.usecases.harvest.domain.ExternalSourceEnum;
import io.repox.api3d.app.usecases.harvest.domain.ModelRecordEntity;
import io.repox.api3d.app.usecases.integration.europeana.domain.EuropeanaSearchResponse;
import io.repox.api3d.app.usecases.integration.sketchfab.application.ports.out.SketchFabDataApiPort;
import io.repox.api3d.app.usecases.integration.sketchfab.domain.SketchFabModelDownload;
import io.repox.api3d.app.usecases.integration.sketchfab.domain.SketchFabSourceEnumRule;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class Tests {
    @LocalServerPort
    public int port;

    @Autowired
    ModelRecordFindPort modelRecordFindPort;

    @Autowired
    SketchFabDataApiPort sketchFabDataApiPort;

    @Autowired
    SketchFabSourceEnumRule.ModelRecordSketchFabUidMapper modelRecordSketchFabUidMapper;

    @SneakyThrows
    @Test
    void sketchfabTotalSize() {
        Long totalSketchfabBytes = modelRecordFindPort.findAllByExternalSource(ExternalSourceEnum.SKETCHFAB).stream()
                .map(ModelRecordEntity::getExternalUri)
                .map(modelRecordSketchFabUidMapper::map)
                .filter(Objects::nonNull)
                .map(sketchfabUid -> {
                    try {
                        return sketchFabDataApiPort.modelDownload(sketchfabUid);
                    } catch (Exception ignored) {
                        log.info(ignored.getMessage(), ignored);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .map(SketchFabModelDownload::source)
                .map(SketchFabModelDownload.Asset::size)
                .collect(Collectors.summarizingLong(Long::longValue))
                .getSum();
        Assertions.assertThat(totalSketchfabBytes > 0);
    }

    @Test
    void sketchFabDownloadInfoTest() {
        RestTemplate restTemplate = new RestTemplate();
        String uid = "8152a12db067492b8179f86e02cf2cb9";
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Token faa380cd0ea1475a98a42f038728232d");
        ResponseEntity<SketchFabModelDownload> res = restTemplate.exchange(
                UriComponentsBuilder.fromUriString("https://api.sketchfab.com/v3/models")
                        .pathSegment(uid, "download")
                        .toUriString(),
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                SketchFabModelDownload.class);
        Assertions.assertThat(res.getBody()).isNotNull();
    }

    @Test
    void restTemplateTest() {
        RestTemplate restTemplate = new RestTemplate();
        EuropeanaSearchResponse res = restTemplate
                .exchange(
                        UriComponentsBuilder.fromUriString("https://api.europeana.eu/record/v2/search.json")
                                .queryParam("cursor", encodeParam("*"))
                                //
                                // "AoREQ3+NvJPsiAM/RC85MTEvaHR0cHNfX193d3dfc2VhcmNoY3VsdHVyZV9ncl9hZ2dyZWdhdG9yX2VkbV9taW5nZWlfMDAwMTU1XzI4ZDE5MWFiXzk1YmFfNDJmNF85ZWRhX2NhMjQ2MDkyYWE4ZA=="))
                                .queryParam("wskey", encodeParam("nLbaXYaiH"))
                                .queryParam("query", encodeParam("*:*"))
                                .queryParam("qf", encodeParam("contentTier:(1 OR 2 OR 3 OR 4)"))
                                .queryParam("qf", encodeParam("TYPE:\"3D\""))
                                .queryParam("rows", encodeParam(String.valueOf(100)))
                                .build(true)
                                .toUri(),
                        HttpMethod.GET,
                        null,
                        EuropeanaSearchResponse.class)
                .getBody();
        Assertions.assertThat(res).isNotNull();
    }

    Object encodeParam(String o) {
        return URLEncoder.encode(o, StandardCharsets.UTF_8);
    }
}
