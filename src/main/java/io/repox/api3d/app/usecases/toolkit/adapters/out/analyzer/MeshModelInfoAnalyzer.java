package io.repox.api3d.app.usecases.toolkit.adapters.out.analyzer;

import io.repox.api3d.app.usecases.toolkit.application.ports.out.AssimpProcessPort;
import io.repox.api3d.app.usecases.toolkit.application.ports.out.BlenderProcessPort;
import io.repox.api3d.app.usecases.toolkit.application.ports.out.ModelInfoAnalyzer;
import io.repox.api3d.app.usecases.toolkit.domain.AssimpInfoTechnicalDetails;
import io.repox.api3d.app.usecases.toolkit.domain.ModelTechnicalDetailsDto;
import io.repox.api3d.appconfig.options.ToolkitProperties;
import java.io.File;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.stereotype.Component;

/**
 * Analyzes polygonal mesh formats. Delegates to Assimp or Blender based on the configured
 * {@link ToolkitProperties.ModelInfoProvider} toggle.
 */
@Component
@RequiredArgsConstructor
class MeshModelInfoAnalyzer implements ModelInfoAnalyzer {

    private static final Set<String> EXTENSIONS = Set.of("glb", "obj", "stl", "dae", "usdz");

    private final AssimpProcessPort assimpProcessPort;
    private final BlenderProcessPort blenderProcessPort;
    private final ToolkitProperties toolkitProperties;
    private final AssimpModelInfoMapper assimpModelInfoMapper;

    @Override
    public Set<String> supportedExtensions() {
        return EXTENSIONS;
    }

    @Override
    public ModelTechnicalDetailsDto analyze(File file) throws Exception {
        if (toolkitProperties.getInfoProvider() == ToolkitProperties.ModelInfoProvider.BLENDER) {
            return blenderProcessPort.info(file);
        }
        return assimpModelInfoMapper.map(assimpProcessPort.info(file));
    }

    @Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
    public interface AssimpModelInfoMapper {
        @Mapping(target = "pointCount", ignore = true)
        @Mapping(target = "gaussianCount", ignore = true)
        ModelTechnicalDetailsDto map(AssimpInfoTechnicalDetails assimpInfoTechnicalDetails);
    }
}
