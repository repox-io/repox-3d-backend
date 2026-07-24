package io.repox.api3d.app.usecases.integration.europeana.configuration;

import io.repox.api3d.app.usecases.integration.europeana.domain.EuropeanaClientSettings;
import io.repox.api3d.appconfig.options.EuropeanaApiProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class EuropeanaConfig {
    @Bean
    RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Bean
    EuropeanaClientSettings europeanaClientSettings(EuropeanaApiProperties apiProperties) {
        return new EuropeanaClientSettings(apiProperties.getApiKey());
    }
}
