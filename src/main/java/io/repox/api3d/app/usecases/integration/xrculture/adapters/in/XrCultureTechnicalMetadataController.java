package io.repox.api3d.app.usecases.integration.xrculture.adapters.in;

import io.repox.api3d.app.usecases.integration.xrculture.application.ports.in.XrCultureTechnicalMetadataUseCase;
import io.repox.api3d.app.usecases.integration.xrculture.domain.technicalmetadata.TechnicalMetadataExtractionRequest;
import io.repox.api3d.app.usecases.integration.xrculture.domain.technicalmetadata.TechnicalMetadataExtractionResponse;
import io.repox.api3d.appconfig.OpenApiTagConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.xml.bind.JAXBException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = OpenApiTagConfig.API_XRCULTURE)
@RestController
@RequiredArgsConstructor
public class XrCultureTechnicalMetadataController {
    private final XrCultureTechnicalMetadataUseCase xrCultureTechnicalMetadataUseCase;

    @Operation(
            summary = "Extract technical metadata of a 3D model supplied inline as base64 or by URL.",
            description = "Takes a TechnicalMetadataExtractionRequest document and returns a"
                    + " TechnicalMetadataExtractionResponse. The Status element mirrors the HTTP status. Only"
                    + " non-null, non-zero metrics are reported, so the emitted elements describe the model type:"
                    + " meshes report vertex and polygon counts, gaussian splats report gaussian counts.")
    @PostMapping(
            value = "/xrculture/details",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    ResponseEntity<TechnicalMetadataExtractionResponse> extractTechnicalMetadata(
            HttpServletRequest ctx, @Valid @RequestBody TechnicalMetadataExtractionRequest request)
            throws JAXBException {
        TechnicalMetadataExtractionResponse response =
                xrCultureTechnicalMetadataUseCase.fromTechnicalMetadataExtractionRequest(request, ctx);
        return ResponseEntity.status(response.getStatus().intValue()).body(response);
    }
}
