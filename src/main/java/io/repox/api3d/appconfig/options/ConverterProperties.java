package io.repox.api3d.appconfig.options;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("options.converter")
public class ConverterProperties {

    /**
     * When {@code true} (default), {@code /tools/convert/mesh} falls back to Blender for files Assimp
     * cannot import (e.g. Draco-compressed glTF/GLB, USDZ). Set {@code false} to fail fast on Assimp
     * instead.
     */
    private boolean meshConvertBlenderFallback = true;
}
