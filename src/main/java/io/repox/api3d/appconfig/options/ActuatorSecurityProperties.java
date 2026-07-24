package io.repox.api3d.appconfig.options;

import jakarta.validation.constraints.AssertTrue;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@ConfigurationProperties("management.endpoints.web.security")
@Validated
public class ActuatorSecurityProperties {
    private boolean enabled = false;
    private String username;
    private String password;

    @AssertTrue
    public boolean isValid() {
        if (!enabled) {
            return true;
        }
        return isNotBlank(username) && isNotBlank(password);
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
