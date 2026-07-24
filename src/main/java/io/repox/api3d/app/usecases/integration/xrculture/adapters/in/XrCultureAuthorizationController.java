package io.repox.api3d.app.usecases.integration.xrculture.adapters.in;

import io.repox.api3d.app.usecases.integration.xrculture.application.ports.in.XrCultureAuthorizationUseCase;
import io.repox.api3d.app.usecases.integration.xrculture.domain.authorization.AuthorizationRequest;
import io.repox.api3d.app.usecases.integration.xrculture.domain.authorization.AuthorizationResponse;
import io.repox.api3d.appconfig.OpenApiTagConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Tag(name = OpenApiTagConfig.API_XRCULTURE)
@RestController
@RequiredArgsConstructor
public class XrCultureAuthorizationController {
    private final XrCultureAuthorizationUseCase xrCultureAuthorizationUseCase;

    @Operation(summary = "Service Authorization Request.")
    @ResponseStatus(HttpStatus.OK)
    @PostMapping(
            value = "/xrculture/auth",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    AuthorizationResponse authorize(@Valid @RequestBody AuthorizationRequest request) {
        return xrCultureAuthorizationUseCase.fromAuthorizationRequest(request);
    }

    @Operation(summary = "Service Authorization sample.")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = "/xrculture/auth", produces = MediaType.APPLICATION_XML_VALUE)
    AuthorizationRequest authorizeSample() {
        return xrCultureAuthorizationUseCase.getAuthorizationRequestSample();
    }
}
