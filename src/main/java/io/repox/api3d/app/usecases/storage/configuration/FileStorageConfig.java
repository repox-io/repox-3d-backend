package io.repox.api3d.app.usecases.storage.configuration;

import io.repox.api3d.app.usecases.storage.domain.FileStorageSettings;
import io.repox.api3d.appconfig.options.FileStorageProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileStorageConfig {
    @Bean
    FileStorageSettings fileStorageProperties(FileStorageProperties fileStorageProperties) {
        return new FileStorageSettings(fileStorageProperties.getDirectory());
    }
}
