package io.repox.api3d.app.usecases.toolkit.adapters.out.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.repox.api3d.app.usecases.job.domain.ModelProcessingJobType;
import io.repox.api3d.app.usecases.storage.application.ports.out.FileStoragePort;
import io.repox.api3d.app.usecases.toolkit.application.ports.in.SplatConversionUseCase;
import io.repox.api3d.app.usecases.toolkit.domain.SplatFormat;
import java.io.File;
import org.springframework.stereotype.Component;

@Component
class SplatConvertJobProcessor extends AbstractModelJobProcessor {

    private final SplatConversionUseCase useCase;

    SplatConvertJobProcessor(FileStoragePort fileStorage, ObjectMapper objectMapper, SplatConversionUseCase useCase) {
        super(fileStorage, objectMapper);
        this.useCase = useCase;
    }

    @Override
    public ModelProcessingJobType type() {
        return ModelProcessingJobType.SPLAT_CONVERT;
    }

    @Override
    protected ProcessingOutput run(File input, JsonNode options) throws Exception {
        return output(useCase.convertSplatFromFile(input, options(options, SplatFormat.class)));
    }
}
