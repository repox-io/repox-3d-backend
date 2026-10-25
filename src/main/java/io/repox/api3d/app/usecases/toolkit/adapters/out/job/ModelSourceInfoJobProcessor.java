package io.repox.api3d.app.usecases.toolkit.adapters.out.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.repox.api3d.app.usecases.job.application.ports.out.ModelJobProcessor;
import io.repox.api3d.app.usecases.job.domain.JobInput;
import io.repox.api3d.app.usecases.job.domain.ModelProcessingJobEntity;
import io.repox.api3d.app.usecases.job.domain.ModelProcessingJobType;
import io.repox.api3d.app.usecases.storage.application.ports.out.FileStoragePort;
import io.repox.api3d.app.usecases.toolkit.application.ports.in.SceneAndModelInfoUseCase;
import io.repox.api3d.app.usecases.toolkit.domain.ModelSourceInfoDto;
import java.io.File;
import org.springframework.stereotype.Component;

/**
 * Backs {@code POST /tools/info}. Deliberately implements {@link ModelJobProcessor} directly instead of
 * extending {@code AbstractModelJobProcessor}: that base resolves a URL source by downloading the whole body
 * into the input directory, which is exactly what this endpoint exists to avoid — its answer comes from a
 * {@code HEAD}. Moving it onto the base would silently turn a header probe into a multi-gigabyte transfer.
 */
@Component
class ModelSourceInfoJobProcessor implements ModelJobProcessor {

    private final FileStoragePort fileStorage;
    private final ObjectMapper objectMapper;
    private final SceneAndModelInfoUseCase useCase;

    ModelSourceInfoJobProcessor(
            FileStoragePort fileStorage, ObjectMapper objectMapper, SceneAndModelInfoUseCase useCase) {
        this.fileStorage = fileStorage;
        this.objectMapper = objectMapper;
        this.useCase = useCase;
    }

    @Override
    public ModelProcessingJobType type() {
        return ModelProcessingJobType.MODEL_INFO;
    }

    @Override
    public ProcessingOutput process(ModelProcessingJobEntity job) throws Exception {
        JobInput input = objectMapper.readValue(job.getInput(), JobInput.class);
        // The upload was already stored at submit time; sourceInfoFromFile measures and then deletes it.
        ModelSourceInfoDto info =
                switch (input.source()) {
                    case URL -> useCase.sourceInfoFromUri(input.url());
                    case UPLOAD -> useCase.sourceInfoFromFile(
                            new File(fileStorage.getInputDirectory(), input.inputFile()));
                };
        return ProcessingOutput.ofResult(objectMapper.valueToTree(info));
    }
}
