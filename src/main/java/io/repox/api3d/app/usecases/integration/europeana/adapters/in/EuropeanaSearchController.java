package io.repox.api3d.app.usecases.integration.europeana.adapters.in;

import io.repox.api3d.app.usecases.harvest.domain.ModelRecordDto;
import io.repox.api3d.app.usecases.integration.europeana.application.ports.in.EuropeanaRecordToModelRecordDtoMapper;
import io.repox.api3d.app.usecases.integration.europeana.application.ports.in.EuropeanaSearchUseCase;
import io.repox.api3d.app.usecases.integration.europeana.domain.EuropeanaSearchRequest;
import io.repox.api3d.app.usecases.integration.europeana.domain.EuropeanaSearchResponse;
import io.repox.api3d.appconfig.OpenApiTagConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = OpenApiTagConfig.API_INTEGRATIONS)
@RestController
@RequiredArgsConstructor
class EuropeanaSearchController {
    private final EuropeanaSearchUseCase europeanaSearchUseCase;
    private final EuropeanaRecordToModelRecordDtoMapper repox3dApiEuropeanaModelViewMapper;

    record EuropeanaModelRecordResponse(List<ModelRecordDto> content, String cursor, Long totalItems) {}

    @Operation(summary = "Search Europeana 3D content.")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/search/europeana")
    EuropeanaModelRecordResponse searchEuropeana(
            @RequestBody EuropeanaSearchRequest request, @RequestParam(required = false) String cursor) {
        EuropeanaSearchResponse res = europeanaSearchUseCase.search(request, cursor);
        return new EuropeanaModelRecordResponse(
                res.items().stream()
                        .map(repox3dApiEuropeanaModelViewMapper::mapFrom)
                        .toList(),
                res.nextCursor(),
                res.totalResults());
    }
}
