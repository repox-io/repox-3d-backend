package io.repox.api3d.app.usecases.integration.xrculture.adapters.out;

import io.repox.api3d.app.usecases.integration.xrculture.application.ports.out.XrCulturePort;
import io.repox.api3d.app.usecases.integration.xrculture.domain.XrCultureLogEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class XrCulturePersistenceAdapter implements XrCulturePort {
    private final XrCultureLogRepository xrCultureLogRepository;

    @Override
    public void save(XrCultureLogEntity entity) {
        xrCultureLogRepository.save(entity);
    }
}
