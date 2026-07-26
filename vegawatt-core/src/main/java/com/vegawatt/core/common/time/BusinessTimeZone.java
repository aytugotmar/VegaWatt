package com.vegawatt.core.common.time;

import java.time.ZoneId;

/**
 * The single source of truth for "whose calendar" business-facing time boundaries (billing
 * periods, daily history buckets, ...) follow. Existed only as a private constant duplicated
 * inside {@link BillingPeriodResolver} before — {@code GetHomeConsumptionHistoryQuery}'s daily
 * history bucketing used {@code ZoneOffset.UTC} instead, so a reading taken shortly after Turkish
 * local midnight (already the next day locally, still the previous day in UTC) landed in the
 * wrong day's bucket while billing correctly rolled over at the Istanbul boundary. Centralizing
 * the zone here means every business-time-boundary computation shares it by construction.
 */
public final class BusinessTimeZone {

    public static final ZoneId ZONE = ZoneId.of("Europe/Istanbul");

    private BusinessTimeZone() {
    }
}
