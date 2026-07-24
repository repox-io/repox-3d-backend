package io.repox.api3d.app.usecases.integration.xrculturehub.adapters.out;

import io.repox.api3d.app.usecases.integration.xrculturehub.application.ports.out.XrCultureHubPort;
import io.repox.api3d.app.usecases.integration.xrculturehub.domain.XrCultureHubRegistrationEntity;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class XrCultureHubPersistenceAdapter implements XrCultureHubPort {
    private final XrCultureHubRegistrationRepository xrCultureHubRegistrationRepository;

    @Override
    public void save(XrCultureHubRegistrationEntity entity) {
        xrCultureHubRegistrationRepository.save(entity);
    }

    @Override
    public Optional<XrCultureHubRegistrationEntity> findByServiceKey(String serviceKey) {
        return xrCultureHubRegistrationRepository.findFirstByServiceKey(serviceKey);
    }
}
