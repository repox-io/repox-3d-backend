package io.repox.api3d.app.usecases.harvest.adapters.ports.in;

import io.repox.api3d.app.usecases.harvest.application.ports.in.ModelHarvestUseCase;
import io.repox.api3d.app.usecases.harvest.domain.AggregatorEnum;
import io.repox.api3d.appconfig.OpenApiTagConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = OpenApiTagConfig.API_INTEGRATIONS)
@RestController
@RequiredArgsConstructor
public class ModelHarvestController {
    private final ModelHarvestUseCase modelHarvestUseCase;

    record ModelHarvestRequest(@NotNull AggregatorEnum aggregator) {}

    // todo jobs pinaka

    @Operation(summary = "Harvest Models from Aggregator")
    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/harvest")
    ResponseEntity<HttpStatus> harvest(
            @RequestBody @Valid ModelHarvestRequest modelHarvestRequest,
            @PageableDefault(size = 24) Pageable pageable) {
        modelHarvestUseCase.harvestModels(modelHarvestRequest.aggregator);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
