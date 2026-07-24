package io.repox.api3d.app.usecases.samples.adapters.ports.in;

import io.repox.api3d.app.usecases.samples.application.ports.in.FileSampleUseCase;
import io.repox.api3d.app.usecases.samples.domain.FileSampleDto;
import io.repox.api3d.app.usecases.samples.domain.ToolEnum;
import io.repox.api3d.appconfig.OpenApiTagConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = OpenApiTagConfig.API_SAMPLES)
@RestController
@RequiredArgsConstructor
class FileSampleController {
    private final FileSampleUseCase fileSampleUseCase;

    @Operation(summary = "List 3D model file samples, optionally filtered by applicable tool.")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/samples")
    List<FileSampleDto> getSamples(@RequestParam(required = false) ToolEnum tool) {
        return fileSampleUseCase.getSamples(tool);
    }
}
