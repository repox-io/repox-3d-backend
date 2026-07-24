package io.repox.api3d.app.usecases.integration.xrculturehub.adapters.out;

import io.repox.api3d.app.usecases.integration.xrculturehub.domain.XrCultureHubRegistrationEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface XrCultureHubRegistrationRepository extends JpaRepository<XrCultureHubRegistrationEntity, UUID> {

    Optional<XrCultureHubRegistrationEntity> findFirstByServiceKey(String serviceKey);
}
