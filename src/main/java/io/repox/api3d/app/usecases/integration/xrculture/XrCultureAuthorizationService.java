package io.repox.api3d.app.usecases.integration.xrculture;

import io.repox.api3d.app.usecases.integration.xrculture.application.ports.in.XrCultureAuthorizationUseCase;
import io.repox.api3d.app.usecases.integration.xrculture.domain.authorization.AuthorizationRequest;
import io.repox.api3d.app.usecases.integration.xrculture.domain.authorization.AuthorizationResponse;
import io.repox.api3d.app.usecases.integration.xrculture.domain.authorization.ObjectFactory;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class XrCultureAuthorizationService implements XrCultureAuthorizationUseCase {
    @Override
    public AuthorizationResponse fromAuthorizationRequest(AuthorizationRequest request) {
        return null;
    }

    @Override
    public AuthorizationRequest getAuthorizationRequestSample() {
        AuthorizationRequest authorizationRequestSample = new ObjectFactory().createAuthorizationRequest();
        authorizationRequestSample.setProviderID("repox.io");
        authorizationRequestSample.setServiceName("Repox 3D Viewer");
        authorizationRequestSample.setEndpoint("https://3dapi.repox.io/");
        authorizationRequestSample.setPublicKey("MIIBIjANBgkqh...");
        authorizationRequestSample.setContactEmail("your_mail@host.com");
        authorizationRequestSample.setTimestamp(String.valueOf(LocalDateTime.now()));
        return authorizationRequestSample;
    }
}
