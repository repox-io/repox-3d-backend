package io.repox.api3d.testconfig;

import io.repox.api3d.app.utils.ApplicationClock;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles(value = "it")
@SpringBootTest(
        classes = {TestContainersDatabaseConfigurationForGenericIntegrationTest.class, FixTimeConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AbstractGenericIntegrationTest {

    @LocalServerPort
    public int port;

    @Autowired
    protected ApplicationClock clock;

    @AfterEach
    void tearDown() {
        clock.resetClock();
    }
}
