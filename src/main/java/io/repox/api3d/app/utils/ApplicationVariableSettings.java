package io.repox.api3d.app.utils;

import io.repox.api3d.appconfig.options.ApplicationUrlProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public record ApplicationVariableSettings(String uiUrl, String apiUrl) {
    @Configuration
    public static class ApplicationVariableSettingsConfig {
        @Bean
        ApplicationVariableSettings applicationSettings(ApplicationUrlProperties properties) {
            return new ApplicationVariableSettings(properties.getUiUrl(), properties.getApiUrl());
        }
    }
}
