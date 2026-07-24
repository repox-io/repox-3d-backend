package io.repox.api3d.appconfig.security;

import io.repox.api3d.appconfig.options.ActuatorSecurityProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Getter
public class SecurityConfigurationProperties {
    private final PasswordEncoder passwordEncoder;
    private final ActuatorSecurityProperties actuatorSecurityProperties;
}
