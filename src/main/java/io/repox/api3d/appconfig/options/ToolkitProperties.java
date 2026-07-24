package io.repox.api3d.appconfig.options;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("options.toolkit")
public class ToolkitProperties {

    /**
     * Selects which engine extracts technical details for mesh formats (glb, obj, stl, dae, usdz).
     * Splat/point formats (ply, spz, ksplat) always use their dedicated header parsers.
     */
    private ModelInfoProvider infoProvider = ModelInfoProvider.ASSIMP;

    public enum ModelInfoProvider {
        ASSIMP,
        BLENDER
    }
}
