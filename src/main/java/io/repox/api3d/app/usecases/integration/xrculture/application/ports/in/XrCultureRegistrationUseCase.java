package io.repox.api3d.app.usecases.integration.xrculture.application.ports.in;

import io.repox.api3d.app.usecases.integration.xrculture.domain.registration.RegistrationRequest;
import io.repox.api3d.app.usecases.integration.xrculture.domain.registration.RegistrationResponse;
import java.io.IOException;
import java.net.URISyntaxException;

public interface XrCultureRegistrationUseCase {
    RegistrationResponse fromRegistrationRequest(RegistrationRequest request);

    String getRegistrationRequestSample() throws URISyntaxException, IOException;
}
