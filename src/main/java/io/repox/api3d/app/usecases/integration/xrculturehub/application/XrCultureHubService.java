package io.repox.api3d.app.usecases.integration.xrculturehub.application;

import io.repox.api3d.app.usecases.integration.xrculture.domain.authorization.AuthorizationRequest;
import io.repox.api3d.app.usecases.integration.xrculture.domain.authorization.AuthorizationResponse;
import io.repox.api3d.app.usecases.integration.xrculture.domain.registration.ObjectFactory;
import io.repox.api3d.app.usecases.integration.xrculture.domain.registration.RegistrationRequest;
import io.repox.api3d.app.usecases.integration.xrculture.domain.registration.RegistrationResponse;
import io.repox.api3d.app.usecases.integration.xrculturehub.application.ports.in.XrCultureHubUseCase;
import io.repox.api3d.app.usecases.integration.xrculturehub.application.ports.out.XrCultureHubPort;
import io.repox.api3d.app.usecases.integration.xrculturehub.domain.XrCultureHubRegistrationEntity;
import jakarta.xml.bind.JAXBException;
import java.math.BigInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class XrCultureHubService implements XrCultureHubUseCase {
    private final XrCultureHubPort xrCultureHubPort;

    @Override
    public RegistrationResponse register(RegistrationRequest registrationRequest) throws JAXBException {
        XrCultureHubRegistrationEntity registrationEntity = XrCultureHubRegistrationEntity.from(registrationRequest);
        xrCultureHubPort.save(registrationEntity);
        RegistrationResponse registrationResponse = new ObjectFactory().createRegistrationResponse();
        registrationResponse.setStatus(BigInteger.valueOf(200L));
        registrationResponse.setServiceKey(registrationEntity.getServiceKey());
        registrationResponse.setMessage("Registration successful");
        return registrationResponse;
    }

    @Override
    public AuthorizationResponse authorize(AuthorizationRequest authorizationRequest) {
        return xrCultureHubPort
                .findByServiceKey(authorizationRequest.getPublicKey())
                .map(r -> {
                    AuthorizationResponse authorizationResponse =
                            new io.repox.api3d.app.usecases.integration.xrculture.domain.authorization.ObjectFactory()
                                    .createAuthorizationResponse();
                    authorizationResponse.setStatus(BigInteger.valueOf(200L));
                    authorizationResponse.setMessage("Authorization successful");
                    authorizationResponse.setSessionToken("dummy todo");
                    return authorizationResponse;
                })
                .orElseThrow(() -> new SecurityException("Authorization failed"));
    }
}
