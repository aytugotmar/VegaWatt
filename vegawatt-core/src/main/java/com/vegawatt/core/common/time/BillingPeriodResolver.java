package com.vegawatt.core.common.time;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Resolves the "yyyy-MM" billing period a given instant falls into, in Turkey's local time zone
 * (a calendar month boundary should follow the customer's wall clock, not UTC).
 */
public final class BillingPeriodResolver {

    private static final DateTimeFormatter PERIOD_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM").withZone(BusinessTimeZone.ZONE);

    private BillingPeriodResolver() {
    }

    public static String currentPeriod(Instant now) {
        return PERIOD_FORMAT.format(now);
    }
}
