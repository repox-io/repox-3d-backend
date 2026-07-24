package io.repox.api3d.app.usecases.toolkit.adapters.out.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.repox.api3d.app.usecases.job.domain.ModelProcessingJobType;
import io.repox.api3d.app.usecases.storage.application.ports.out.FileStoragePort;
import io.repox.api3d.app.usecases.toolkit.application.ports.in.MeshConversionUseCase;
import io.repox.api3d.app.usecases.toolkit.domain.MeshExportFormat;
import java.io.File;
import org.springframework.stereotype.Component;

@Component
class MeshConvertJobProcessor extends AbstractModelJobProcessor {

    private final MeshConversionUseCase useCase;

    MeshConvertJobProcessor(FileStoragePort fileStorage, ObjectMapper objectMapper, MeshConversionUseCase useCase) {
        super(fileStorage, objectMapper);
        this.useCase = useCase;
    }

    @Override
    public ModelProcessingJobType type() {
        return ModelProcessingJobType.MESH_CONVERT;
    }

    @Override
    protected ProcessingOutput run(File input, JsonNode options) throws Exception {
        return output(useCase.convertMeshFromFile(input, options(options, MeshExportFormat.class)));
    }
}
