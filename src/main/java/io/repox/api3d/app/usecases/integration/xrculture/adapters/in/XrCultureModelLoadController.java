package io.repox.api3d.app.usecases.integration.xrculture.adapters.in;

import io.repox.api3d.app.usecases.integration.xrculture.application.ports.in.XrCultureModelLoadUseCase;
import io.repox.api3d.app.usecases.integration.xrculture.domain.loading.ModelLoadingRequest;
import io.repox.api3d.app.usecases.integration.xrculture.domain.loading.ModelLoadingResponse;
import io.repox.api3d.appconfig.OpenApiTagConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.xml.bind.JAXBException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = OpenApiTagConfig.API_XRCULTURE)
@RestController
@RequiredArgsConstructor
public class XrCultureModelLoadController {
    private final XrCultureModelLoadUseCase xrCultureModelLoadUseCase;

    @Operation(summary = "Request to load a model. Generate url for viewer.")
    @ResponseStatus(HttpStatus.OK)
    @PostMapping(
            value = "/xrculture/models",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    ModelLoadingResponse loadModels(HttpServletRequest ctx, @Valid @RequestBody ModelLoadingRequest request)
            throws JAXBException {
        return xrCultureModelLoadUseCase.fromModelLoadingRequest(request, ctx);
    }
}
