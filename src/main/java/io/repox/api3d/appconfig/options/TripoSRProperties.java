package io.repox.api3d.appconfig.options;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("options.triposr")
public class TripoSRProperties {
    private String dir;
    private String python;
    private String device;
    private String saveFormat;
}
