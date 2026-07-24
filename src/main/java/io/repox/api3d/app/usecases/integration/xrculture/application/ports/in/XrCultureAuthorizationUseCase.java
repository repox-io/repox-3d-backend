package io.repox.api3d.app.usecases.integration.xrculture.application.ports.in;

import io.repox.api3d.app.usecases.integration.xrculture.domain.authorization.AuthorizationRequest;
import io.repox.api3d.app.usecases.integration.xrculture.domain.authorization.AuthorizationResponse;

public interface XrCultureAuthorizationUseCase {
    AuthorizationResponse fromAuthorizationRequest(AuthorizationRequest request);

    AuthorizationRequest getAuthorizationRequestSample();
}
