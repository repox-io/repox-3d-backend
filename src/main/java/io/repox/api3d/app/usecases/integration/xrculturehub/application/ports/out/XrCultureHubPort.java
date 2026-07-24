package io.repox.api3d.app.usecases.integration.xrculturehub.application.ports.out;

import io.repox.api3d.app.usecases.integration.xrculturehub.domain.XrCultureHubRegistrationEntity;
import java.util.Optional;

public interface XrCultureHubPort {
    void save(XrCultureHubRegistrationEntity entity);

    Optional<XrCultureHubRegistrationEntity> findByServiceKey(String serviceKey);
}
