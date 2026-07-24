package io.repox.api3d.app.usecases.toolkit.adapters.in;

import io.repox.api3d.app.usecases.job.application.JobResponses;
import io.repox.api3d.app.usecases.job.application.JobStatusDto;
import io.repox.api3d.app.usecases.job.application.ports.in.ProcessingJobUseCase;
import io.repox.api3d.app.usecases.job.domain.ModelProcessingJobType;
import io.repox.api3d.app.usecases.toolkit.application.ports.in.ModelThumbnailUseCase;
import io.repox.api3d.app.utils.ModelSourceUtils;
import io.repox.api3d.appconfig.OpenApiTagConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = OpenApiTagConfig.API_3D_TOOLKIT)
@RestController
@RequiredArgsConstructor
public class ModelThumbnailController {

    private final ModelThumbnailUseCase modelThumbnailUseCase;
    private final ProcessingJobUseCase processingJobUseCase;

    @Operation(
            summary = "Submit a job to generate a PNG thumbnail of a 3D model supplied by file upload or URL.",
            description = "Asynchronous: returns 202 with a job to poll; the PNG is downloaded from the job's output"
                    + " once it completes. Use /tools/thumbnail/inline to block and get the PNG directly.")
    @PostMapping(value = "/tools/thumbnail")
    ResponseEntity<JobStatusDto> generateThumbnail(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "url", required = false) String url) {
        return JobResponses.accepted(
                ModelSourceUtils.isUpload(file, url)
                        ? processingJobUseCase.submitWithUpload(ModelProcessingJobType.MODEL_THUMBNAIL, file, null)
                        : processingJobUseCase.submit(ModelProcessingJobType.MODEL_THUMBNAIL, url, null));
    }

    @Operation(
            summary = "Generate a PNG thumbnail of a 3D model supplied by file upload or URL, inline.",
            description = "Synchronous: blocks until the render completes and returns the PNG in the response body."
                    + " For large models prefer /tools/thumbnail to avoid a long-held request.")
    @ResponseStatus(HttpStatus.OK)
    @PostMapping(value = "/tools/thumbnail/inline", produces = MediaType.IMAGE_PNG_VALUE)
    byte[] generateThumbnailInline(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "url", required = false) String url) {
        return ModelSourceUtils.isUpload(file, url)
                ? modelThumbnailUseCase.generateThumbnail(file)
                : modelThumbnailUseCase.generateThumbnailFromUri(url);
    }
}
