package io.repox.api3d.app.usecases.toolkit.adapters.out.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.repox.api3d.app.usecases.job.domain.ModelProcessingJobType;
import io.repox.api3d.app.usecases.storage.application.ports.out.FileStoragePort;
import io.repox.api3d.app.usecases.toolkit.application.ports.in.ModelThumbnailUseCase;
import java.io.File;
import org.springframework.stereotype.Component;

@Component
class ModelThumbnailJobProcessor extends AbstractModelJobProcessor {

    private final ModelThumbnailUseCase useCase;

    ModelThumbnailJobProcessor(FileStoragePort fileStorage, ObjectMapper objectMapper, ModelThumbnailUseCase useCase) {
        super(fileStorage, objectMapper);
        this.useCase = useCase;
    }

    @Override
    public ModelProcessingJobType type() {
        return ModelProcessingJobType.MODEL_THUMBNAIL;
    }

    @Override
    protected ProcessingOutput run(File input, JsonNode options) throws Exception {
        return output(useCase.generateThumbnailFromFile(input));
    }
}
