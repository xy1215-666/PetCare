package io.petcare.backend.common;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

public final class BusinessDate {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private BusinessDate() {
    }

    public static LocalDate today(Clock clock) {
        return LocalDate.now(clock.withZone(ZONE));
    }

    public static boolean isFuture(LocalDate date, Clock clock) {
        return date.isAfter(today(clock));
    }
}
