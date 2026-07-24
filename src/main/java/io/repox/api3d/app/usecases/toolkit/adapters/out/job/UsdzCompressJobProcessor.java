package io.repox.api3d.app.usecases.toolkit.adapters.out.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.repox.api3d.app.usecases.job.domain.ModelProcessingJobType;
import io.repox.api3d.app.usecases.storage.application.ports.out.FileStoragePort;
import io.repox.api3d.app.usecases.toolkit.application.ports.in.UsdzCompressionUseCase;
import io.repox.api3d.app.usecases.toolkit.domain.UsdzOptimizeOptions;
import java.io.File;
import org.springframework.stereotype.Component;

@Component
class UsdzCompressJobProcessor extends AbstractModelJobProcessor {

    private final UsdzCompressionUseCase useCase;

    UsdzCompressJobProcessor(FileStoragePort fileStorage, ObjectMapper objectMapper, UsdzCompressionUseCase useCase) {
        super(fileStorage, objectMapper);
        this.useCase = useCase;
    }

    @Override
    public ModelProcessingJobType type() {
        return ModelProcessingJobType.USDZ_COMPRESS;
    }

    @Override
    protected ProcessingOutput run(File input, JsonNode options) throws Exception {
        UsdzOptimizeOptions opts = options(options, UsdzOptimizeOptions.class);
        return output(useCase.compressUsdzFromFile(input, opts == null ? new UsdzOptimizeOptions(null, null) : opts));
    }
}
