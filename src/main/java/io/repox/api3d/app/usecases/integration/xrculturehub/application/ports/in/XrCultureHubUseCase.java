package io.repox.api3d.app.usecases.integration.xrculturehub.application.ports.in;

import io.repox.api3d.app.usecases.integration.xrculture.domain.authorization.AuthorizationRequest;
import io.repox.api3d.app.usecases.integration.xrculture.domain.authorization.AuthorizationResponse;
import io.repox.api3d.app.usecases.integration.xrculture.domain.registration.RegistrationRequest;
import io.repox.api3d.app.usecases.integration.xrculture.domain.registration.RegistrationResponse;
import jakarta.xml.bind.JAXBException;

public interface XrCultureHubUseCase {
    RegistrationResponse register(RegistrationRequest registrationRequest) throws JAXBException;

    AuthorizationResponse authorize(AuthorizationRequest authorizationRequest);
}
