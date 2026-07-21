package com.vegawatt.core.common.time;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Resolves the "yyyy-MM" billing period a given instant falls into, in Turkey's local time zone
 * (a calendar month boundary should follow the customer's wall clock, not UTC).
 */
public final class BillingPeriodResolver {

    private static final ZoneId ZONE = ZoneId.of("Europe/Istanbul");
    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM").withZone(ZONE);

    private BillingPeriodResolver() {
    }

    public static String currentPeriod(Instant now) {
        return PERIOD_FORMAT.format(now);
    }
}
