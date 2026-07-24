package io.repox.api3d.app.usecases.integration.xrculture.application.ports.out;

import io.repox.api3d.app.usecases.integration.xrculture.domain.XrCultureLogEntity;

public interface XrCulturePort {
    void save(XrCultureLogEntity entity);
}
