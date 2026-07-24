package io.repox.api3d.app.usecases.toolkit.adapters.out.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.repox.api3d.app.usecases.job.domain.ModelProcessingJobType;
import io.repox.api3d.app.usecases.storage.application.ports.out.FileStoragePort;
import io.repox.api3d.app.usecases.toolkit.application.ports.in.MeshCompressionUseCase;
import io.repox.api3d.app.usecases.toolkit.domain.MeshCompressionOptions;
import java.io.File;
import org.springframework.stereotype.Component;

@Component
class MeshCompressJobProcessor extends AbstractModelJobProcessor {

    private final MeshCompressionUseCase useCase;

    MeshCompressJobProcessor(FileStoragePort fileStorage, ObjectMapper objectMapper, MeshCompressionUseCase useCase) {
        super(fileStorage, objectMapper);
        this.useCase = useCase;
    }

    @Override
    public ModelProcessingJobType type() {
        return ModelProcessingJobType.MESH_COMPRESS;
    }

    @Override
    public void validateUpload(File inputFile) {
        // Reject a Draco-compressed upload before a job is created; URL inputs are validated in the worker.
        useCase.verifyCompressible(inputFile);
    }

    @Override
    protected ProcessingOutput run(File input, JsonNode options) throws Exception {
        MeshCompressionOptions opts = options(options, MeshCompressionOptions.class);
        return output(useCase.compressMeshFromFile(input, opts == null ? MeshCompressionOptions.defaults() : opts));
    }
}
