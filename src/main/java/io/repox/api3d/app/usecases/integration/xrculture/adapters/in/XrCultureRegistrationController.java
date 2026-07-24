package io.repox.api3d.app.usecases.integration.xrculture.adapters.in;

import io.repox.api3d.app.usecases.integration.xrculture.application.ports.in.XrCultureRegistrationUseCase;
import io.repox.api3d.app.usecases.integration.xrculture.domain.registration.RegistrationRequest;
import io.repox.api3d.app.usecases.integration.xrculture.domain.registration.RegistrationResponse;
import io.repox.api3d.appconfig.OpenApiTagConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URISyntaxException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Tag(name = OpenApiTagConfig.API_XRCULTURE)
@RestController
@RequiredArgsConstructor
public class XrCultureRegistrationController {
    private final XrCultureRegistrationUseCase xrCultureRegistrationUseCase;

    @Operation(summary = "Viewer registration request.")
    @ResponseStatus(HttpStatus.OK)
    @PostMapping(
            value = "/xrculture/register",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    RegistrationResponse register(@Valid @RequestBody RegistrationRequest request) {
        return xrCultureRegistrationUseCase.fromRegistrationRequest(request);
    }

    @Operation(summary = "Viewer registration request sample.")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = "/xrculture/register", produces = MediaType.APPLICATION_XML_VALUE)
    String registerSample() throws URISyntaxException, IOException {
        return xrCultureRegistrationUseCase.getRegistrationRequestSample();
    }
}
