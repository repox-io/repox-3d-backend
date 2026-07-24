package io.repox.api3d.appconfig.options;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("options.sketchfab")
public class SketchFabApiProperties {
    String apiKey;
}
