package io.repox.api3d.app.usecases.toolkit.adapters.in;

import io.repox.api3d.app.usecases.toolkit.application.ports.in.ExternalModelStreamUseCase;
import io.repox.api3d.appconfig.OpenApiTagConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Tag(name = OpenApiTagConfig.API_3D_TOOLKIT)
@RestController
@RequiredArgsConstructor
public class ExternalModelStreamController {
    private final ExternalModelStreamUseCase externalModelStreamUseCase;

    @Operation(summary = "Return a external model from a URL.")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = "/tools/retrieve")
    ResponseEntity<StreamingResponseBody> streamExternalModel(
            HttpServletResponse response, @Valid @RequestParam URI url) {
        StreamingResponseBody responseBody =
                outputStream -> externalModelStreamUseCase.streamExternalModel(url, response, outputStream);
        return ResponseEntity.ok(responseBody);
    }
}
