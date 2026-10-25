package io.repox.api3d.app.usecases.job.adapters.in;

import io.repox.api3d.app.usecases.job.application.JobOutputResolution;
import io.repox.api3d.app.usecases.job.application.JobOutputResolution.Outcome;
import io.repox.api3d.app.usecases.job.application.JobStatusDto;
import io.repox.api3d.app.usecases.job.application.ports.in.ProcessingJobUseCase;
import io.repox.api3d.appconfig.OpenApiTagConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.File;
import java.nio.file.Files;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Read side of the async processing jobs: poll a job's status and download its output. Download failures
 * are mapped to granular status codes, each carrying a machine-readable {@code reason} in the body.
 */
@Tag(name = OpenApiTagConfig.API_JOB)
@RestController
@RequiredArgsConstructor
public class ProcessingJobController {

    private final ProcessingJobUseCase processingJobUseCase;

    @Operation(summary = "Get the status of a processing job.")
    @GetMapping(value = "/jobs/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<JobStatusDto> getJob(@PathVariable UUID id) {
        return processingJobUseCase.getJob(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound()
                .build());
    }

    @Operation(summary = "Download the output file produced by a processing job.")
    @GetMapping(value = "/jobs/{id}/output")
    ResponseEntity<StreamingResponseBody> downloadOutput(@PathVariable UUID id) {
        JobOutputResolution resolution = processingJobUseCase.resolveOutput(id);
        if (resolution.outcome() != Outcome.AVAILABLE) {
            throw new OutputUnavailableException(resolution.outcome());
        }
        File file = resolution.file();
        StreamingResponseBody body = outputStream -> Files.copy(file.toPath(), outputStream);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resolution.downloadName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(body);
    }

    @ExceptionHandler(OutputUnavailableException.class)
    ResponseEntity<ProblemDetail> handleOutputUnavailable(OutputUnavailableException ex) {
        HttpStatus status = statusFor(ex.outcome);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, messageFor(ex.outcome));
        problemDetail.setProperty("reason", ex.outcome.name());
        return ResponseEntity.status(status).body(problemDetail);
    }

    private static HttpStatus statusFor(Outcome outcome) {
        return switch (outcome) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case NOT_FINISHED, NO_OUTPUT -> HttpStatus.CONFLICT;
            case FAILED -> HttpStatus.UNPROCESSABLE_ENTITY;
            case EXPIRED, OUTPUT_MISSING -> HttpStatus.GONE;
            case AVAILABLE -> HttpStatus.OK;
        };
    }

    private static String messageFor(Outcome outcome) {
        return switch (outcome) {
            case NOT_FOUND -> "No job found with the given id.";
            case NOT_FINISHED -> "The job has not finished processing yet.";
            case NO_OUTPUT -> "This job does not produce a downloadable output.";
            case FAILED -> "The job failed during processing.";
            case EXPIRED -> "The job output has expired and is no longer available.";
            case OUTPUT_MISSING -> "The job output is no longer available.";
            case AVAILABLE -> "";
        };
    }

    /** Signals a non-downloadable {@link Outcome}; translated to a status code by the local handler. */
    private static final class OutputUnavailableException extends RuntimeException {
        private final transient Outcome outcome;

        private OutputUnavailableException(Outcome outcome) {
            this.outcome = outcome;
        }
    }
}
