package io.repox.api3d.testconfig;

import io.repox.api3d.app.utils.ApplicationClock;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class FixTimeConfig {
    private static Clock clock = Clock.fixed(Instant.parse("2018-08-22T10:00:00Z"), ZoneOffset.UTC);

    @Primary
    @Bean
    ApplicationClock fixedClock() {
        return new ApplicationClock(clock, ZoneId.of("UTC"));
    }
}
