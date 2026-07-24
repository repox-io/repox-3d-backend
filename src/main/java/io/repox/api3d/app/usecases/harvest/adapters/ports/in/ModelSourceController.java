package io.repox.api3d.app.usecases.harvest.adapters.ports.in;

import io.repox.api3d.app.usecases.harvest.application.ports.in.ModelSourceUseCase;
import io.repox.api3d.appconfig.OpenApiTagConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = OpenApiTagConfig.API_INTEGRATIONS)
@RestController
@RequiredArgsConstructor
public class ModelSourceController {
    private final ModelSourceUseCase modelSourceUseCase;

    @Operation(summary = "Distinct model records external uri list")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/list/externalUri")
    ResponseEntity<List<String>> getAllExternalUriList() {
        return new ResponseEntity<>(modelSourceUseCase.allModelRecordExternalUriList(), HttpStatus.OK);
    }
}
