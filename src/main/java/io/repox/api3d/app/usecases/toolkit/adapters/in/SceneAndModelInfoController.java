package io.repox.api3d.app.usecases.toolkit.adapters.in;

import io.repox.api3d.app.usecases.job.application.JobResponses;
import io.repox.api3d.app.usecases.job.application.JobStatusDto;
import io.repox.api3d.app.usecases.job.application.ports.in.ProcessingJobUseCase;
import io.repox.api3d.app.usecases.job.domain.ModelProcessingJobType;
import io.repox.api3d.app.usecases.toolkit.application.ports.in.SceneAndModelInfoUseCase;
import io.repox.api3d.app.usecases.toolkit.domain.ModelDetectionDto;
import io.repox.api3d.app.usecases.toolkit.domain.ModelTechnicalDetailsDto;
import io.repox.api3d.app.utils.ModelSourceUtils;
import io.repox.api3d.appconfig.OpenApiTagConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = OpenApiTagConfig.API_3D_TOOLKIT)
@RestController
@RequiredArgsConstructor
public class SceneAndModelInfoController {
    private final SceneAndModelInfoUseCase sceneAndModelInfoUseCase;
    private final ProcessingJobUseCase processingJobUseCase;

    @Operation(
            summary = "Submit a job to retrieve technical info of a 3D model supplied by file upload or URL.",
            description = "Asynchronous: returns 202 with a job to poll; the technical details arrive in the job's"
                    + " result once it completes. Use /tools/details/inline to block and get the details directly.")
    @PostMapping(value = "/tools/details")
    ResponseEntity<JobStatusDto> generateModelInfo(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "url", required = false) String url) {
        return JobResponses.accepted(
                ModelSourceUtils.isUpload(file, url)
                        ? processingJobUseCase.submitWithUpload(ModelProcessingJobType.MODEL_DETAILS, file, null)
                        : processingJobUseCase.submit(ModelProcessingJobType.MODEL_DETAILS, url, null));
    }

    @Operation(
            summary = "Retrieve technical info of a 3D model supplied by file upload or URL, inline.",
            description = "Synchronous: blocks until analysis completes and returns the technical details in the"
                    + " response body. For large meshes prefer /tools/details to avoid a long-held request.")
    @ResponseStatus(HttpStatus.OK)
    @PostMapping(value = "/tools/details/inline", produces = MediaType.APPLICATION_JSON_VALUE)
    ModelTechnicalDetailsDto generateModelInfoInline(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "url", required = false) String url) {
        return ModelSourceUtils.isUpload(file, url)
                ? sceneAndModelInfoUseCase.getModelTechnicalDetails(file)
                : sceneAndModelInfoUseCase.getModelTechnicalDetailsFromUri(url);
    }

    @Operation(
            summary = "Detect whether a 3D model (upload or URL) is a mesh or a gaussian splat, and list its"
                    + " convertible target formats.",
            description = "Synchronous and lightweight: classification is decided by the file extension where"
                    + " possible, and otherwise by reading only the file header. For a URL the full model is never"
                    + " downloaded — an unambiguous extension needs no bytes, and anything else reads only the"
                    + " leading bytes via a ranged request.")
    @ResponseStatus(HttpStatus.OK)
    @PostMapping(value = "/tools/detect", produces = MediaType.APPLICATION_JSON_VALUE)
    ModelDetectionDto detect(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "url", required = false) String url) {
        return ModelSourceUtils.isUpload(file, url)
                ? sceneAndModelInfoUseCase.detect(file)
                : sceneAndModelInfoUseCase.detectFromUri(url);
    }

    @Operation(summary = "Supported 3D model file types.")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = "/tools/supported", produces = MediaType.APPLICATION_JSON_VALUE)
    List<String> supportedModelFileTypes() throws IOException {
        return sceneAndModelInfoUseCase.supportedFileExtensions();
    }
}
