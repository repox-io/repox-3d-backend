package io.repox.api3d.appconfig;

import io.repox.api3d.app.utils.ApplicationClock;
import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class TimeConfiguration {
    private static final ZoneId DEFAULT = ZoneId.of("UTC");
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    static {
        TimeZone.setDefault(TimeZone.getTimeZone(DEFAULT));
    }

    @Bean
    ApplicationClock clock() {
        return new ApplicationClock(Clock.system(DEFAULT), DEFAULT);
    }
}
