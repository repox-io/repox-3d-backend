package io.repox.api3d.app.usecases.integration.xrculture.adapters.out;

import io.repox.api3d.app.usecases.integration.xrculture.domain.XrCultureLogEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface XrCultureLogRepository extends JpaRepository<XrCultureLogEntity, UUID> {}
