package io.repox.api3d.app.usecases.integration.xrculturehub.adapters.in;

import io.repox.api3d.app.usecases.integration.xrculture.domain.authorization.AuthorizationRequest;
import io.repox.api3d.app.usecases.integration.xrculture.domain.authorization.AuthorizationResponse;
import io.repox.api3d.app.usecases.integration.xrculture.domain.registration.RegistrationRequest;
import io.repox.api3d.app.usecases.integration.xrculture.domain.registration.RegistrationResponse;
import io.repox.api3d.app.usecases.integration.xrculturehub.application.ports.in.XrCultureHubUseCase;
import io.repox.api3d.appconfig.OpenApiTagConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.xml.bind.JAXBException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = OpenApiTagConfig.API_XRCULTURE_HUB)
@RestController
@RequiredArgsConstructor
public class XrCultureHubController {
    private final XrCultureHubUseCase xrCultureHubUseCase;

    @Operation(summary = "Hub Viewer Registration.")
    @ResponseStatus(HttpStatus.OK)
    @PostMapping(
            value = "/xrculturehub/register",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    RegistrationResponse register(@Valid @RequestBody RegistrationRequest registrationRequest) throws JAXBException {
        return xrCultureHubUseCase.register(registrationRequest);
    }

    @Operation(summary = "Hub Service Authorization.")
    @ResponseStatus(HttpStatus.OK)
    @PostMapping(
            value = "/xrculturehub/auth",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    AuthorizationResponse authorize(@Valid @RequestBody AuthorizationRequest request) {
        return xrCultureHubUseCase.authorize(request);
    }
}
