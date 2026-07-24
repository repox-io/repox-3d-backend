package io.repox.api3d.app.usecases.integration.xrculturehub.domain;

import io.repox.api3d.app.usecases.integration.xrculture.domain.registration.RegistrationRequest;
import io.repox.api3d.app.utils.XmlUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.Getter;
import org.apache.hc.client5.http.utils.Base64;

@Getter
@Entity
@Table(name = "xrculture_hub_registration")
public class XrCultureHubRegistrationEntity {
    @Id
    UUID id;

    @Column(name = "service_key")
    String serviceKey;

    String registration;
    LocalDateTime timestamp;

    public static XrCultureHubRegistrationEntity from(RegistrationRequest registrationRequest) throws JAXBException {
        XrCultureHubRegistrationEntity registrationEntity = new XrCultureHubRegistrationEntity();
        registrationEntity.id = UUID.randomUUID();
        registrationEntity.serviceKey = randomBase64String();
        registrationEntity.registration =
                XmlUtils.stringify(JAXBContext.newInstance(RegistrationRequest.class), registrationRequest);
        registrationEntity.timestamp = LocalDateTime.now();
        return registrationEntity;
    }

    private static String randomBase64String() {
        Random random = ThreadLocalRandom.current();
        byte[] r = new byte[256];
        random.nextBytes(r);
        return Base64.encodeBase64String(r);
    }
}
