package io.repox.api3d.app.utils;

import java.time.*;

public class ApplicationClock {
    private final Clock initialClock;
    private Clock clock;
    private final ZoneId zoneId;

    public ApplicationClock(Clock clock, ZoneId zoneId) {
        this.initialClock = clock;
        this.clock = clock;
        this.zoneId = zoneId;
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public long nowAsTimestamp() {
        return Instant.now(clock).toEpochMilli();
    }

    public ZonedDateTime nowWithZone() {
        return ZonedDateTime.now(clock);
    }

    public Clock javaClock() {
        return clock;
    }

    public void advanceBySeconds(int seconds) {
        final Instant instant = clock.instant();
        final Instant advancedInstance = instant.plusSeconds(seconds);
        clock = Clock.fixed(advancedInstance, ZoneOffset.UTC);
    }

    public void resetClock() {
        clock = initialClock;
    }

    public Instant instant() {
        return clock.instant();
    }
}
