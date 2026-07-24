package io.repox.api3d.app.usecases.toolkit.adapters.out.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.repox.api3d.app.usecases.job.domain.ModelProcessingJobType;
import io.repox.api3d.app.usecases.storage.application.ports.out.FileStoragePort;
import io.repox.api3d.app.usecases.toolkit.application.ports.in.SceneAndModelInfoUseCase;
import java.io.File;
import org.springframework.stereotype.Component;

@Component
class ModelDetailsJobProcessor extends AbstractModelJobProcessor {

    private final SceneAndModelInfoUseCase useCase;

    ModelDetailsJobProcessor(FileStoragePort fileStorage, ObjectMapper objectMapper, SceneAndModelInfoUseCase useCase) {
        super(fileStorage, objectMapper);
        this.useCase = useCase;
    }

    @Override
    public ModelProcessingJobType type() {
        return ModelProcessingJobType.MODEL_DETAILS;
    }

    @Override
    protected ProcessingOutput run(File input, JsonNode options) throws Exception {
        return ProcessingOutput.ofResult(objectMapper.valueToTree(useCase.getModelTechnicalDetailsFromFile(input)));
    }
}
