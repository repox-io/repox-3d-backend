package io.repox.api3d.app.usecases.integration.xrculture;

import io.repox.api3d.app.usecases.integration.xrculture.application.ports.in.XrCultureRegistrationUseCase;
import io.repox.api3d.app.usecases.integration.xrculture.domain.registration.RegistrationRequest;
import io.repox.api3d.app.usecases.integration.xrculture.domain.registration.RegistrationResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class XrCultureRegistrationService implements XrCultureRegistrationUseCase {

    @Override
    public RegistrationResponse fromRegistrationRequest(RegistrationRequest request) {
        return null;
    }

    @Override
    public String getRegistrationRequestSample() throws IOException {
        return new String(
                Objects.requireNonNull(XrCultureRegistrationService.class
                                .getClassLoader()
                                .getResourceAsStream("xrculture/RegistrationRequest_sample.xml"))
                        .readAllBytes(),
                StandardCharsets.UTF_8);
    }
}
