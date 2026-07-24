package io.repox.api3d.appconfig.options;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("options.blender")
public class BlenderProperties {
    private String thumbnailScript;
    private String infoScript;
    private String convertScript;
    private String usdzOptimizeScript;
}
