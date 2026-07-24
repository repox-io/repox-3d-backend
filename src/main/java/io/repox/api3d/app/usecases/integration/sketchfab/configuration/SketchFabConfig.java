package io.repox.api3d.app.usecases.integration.sketchfab.configuration;

import io.repox.api3d.appconfig.options.SketchFabApiProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SketchFabConfig {

    public record SketchFabClientSettings(String apiKey) {}

    @Bean
    SketchFabClientSettings sketchFabClientSettings(SketchFabApiProperties properties) {
        return new SketchFabClientSettings(properties.getApiKey());
    }
}
