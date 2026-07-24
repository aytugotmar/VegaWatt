package com.vegawatt.sensors.simulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DiurnalCurveTest {

    /** hashCode() == 0, so every hash-derived phase/offset below is exactly 0. */
    private static final UUID FIXED_ID = new UUID(0L, 0L);

    @Test
    void bumpPeaksExactlyAtItsCenterAndVanishesAtItsEdge() {
        assertThat(DiurnalCurve.bump(19.0, 19.0, 2.0, 1.35)).isCloseTo(0.35, within(1e-9));
        assertThat(DiurnalCurve.bump(21.0, 19.0, 2.0, 1.35)).isEqualTo(0.0);
        assertThat(DiurnalCurve.bump(17.0, 19.0, 2.0, 1.35)).isEqualTo(0.0);
    }

    @Test
    void bumpIsSymmetricAroundItsCenter() {
        double before = DiurnalCurve.bump(18.0, 19.0, 2.0, 1.35);
        double after = DiurnalCurve.bump(20.0, 19.0, 2.0, 1.35);

        assertThat(before).isCloseTo(after, within(1e-9));
        assertThat(before).isGreaterThan(0.0);
    }

    @Test
    void diurnalMultiplierIsAlwaysBaselineWithoutBumps() {
        assertThat(DiurnalCurve.diurnalMultiplier(3.0, List.of(), FIXED_ID)).isEqualTo(1.0);
        assertThat(DiurnalCurve.diurnalMultiplier(19.0, List.of(), FIXED_ID)).isEqualTo(1.0);
    }

    @Test
    void zeroHashApplianceGetsNoPersonalityJitter() {
        List<ApplianceProfile.Bump> bumps = List.of(new ApplianceProfile.Bump(19.0, 2.0, 1.35));

        double atCenter = DiurnalCurve.diurnalMultiplier(19.0, bumps, FIXED_ID);

        assertThat(atCenter).isCloseTo(1.35, within(1e-9));
    }

    @Test
    void differentAppliancesOfTheSameTypeDoNotPeakAtTheExactSameTimeOrDepth() {
        List<ApplianceProfile.Bump> bumps = List.of(new ApplianceProfile.Bump(19.0, 2.0, 1.35));
        UUID otherApplianceId = new UUID(0L, 1L);

        double zeroHashReading = DiurnalCurve.diurnalMultiplier(19.0, bumps, FIXED_ID);
        double otherApplianceReading = DiurnalCurve.diurnalMultiplier(19.0, bumps, otherApplianceId);

        assertThat(otherApplianceReading).isNotEqualTo(zeroHashReading);
    }

    @Test
    void identifiesWeekendDays() {
        ZonedDateTime saturday = ZonedDateTime.of(2026, 7, 25, 12, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime monday = ZonedDateTime.of(2026, 7, 20, 12, 0, 0, 0, ZoneOffset.UTC);

        assertThat(DiurnalCurve.isWeekend(saturday)).isTrue();
        assertThat(DiurnalCurve.isWeekend(monday)).isFalse();
    }

    @Test
    void dutyCycleIsOnAtTheStartOfEachPeriodAndOffLater() {
        ApplianceProfile.DutyCycle cycle = new ApplianceProfile.DutyCycle(1200, 0.4, 0.2);
        ZonedDateTime periodStart = Instant.ofEpochSecond(0).atZone(ZoneOffset.UTC);
        ZonedDateTime midOnWindow = Instant.ofEpochSecond(200).atZone(ZoneOffset.UTC);
        ZonedDateTime afterOnWindow = Instant.ofEpochSecond(600).atZone(ZoneOffset.UTC);
        ZonedDateTime nextPeriodStart = Instant.ofEpochSecond(1200).atZone(ZoneOffset.UTC);

        assertThat(DiurnalCurve.dutyCycleOn(periodStart, FIXED_ID, cycle)).isTrue();
        assertThat(DiurnalCurve.dutyCycleOn(midOnWindow, FIXED_ID, cycle)).isTrue();
        assertThat(DiurnalCurve.dutyCycleOn(afterOnWindow, FIXED_ID, cycle)).isFalse();
        assertThat(DiurnalCurve.dutyCycleOn(nextPeriodStart, FIXED_ID, cycle)).isTrue();
    }

    @Test
    void sessionStartsAtTheWindowStartWhenTheDaySeedOffsetIsZero() {
        ApplianceProfile.Session session = new ApplianceProfile.Session(19.0, 21.0, 1.0, 0.03);
        LocalDate weekday = LocalDate.ofEpochDay(0);
        ZonedDateTime beforeSession = ZonedDateTime.of(weekday, LocalTime.of(18, 59), ZoneOffset.UTC);
        ZonedDateTime duringSession = ZonedDateTime.of(weekday, LocalTime.of(19, 30), ZoneOffset.UTC);
        ZonedDateTime afterSession = ZonedDateTime.of(weekday, LocalTime.of(20, 1), ZoneOffset.UTC);

        assertThat(DiurnalCurve.inDailySession(beforeSession, FIXED_ID, session)).isFalse();
        assertThat(DiurnalCurve.inDailySession(duringSession, FIXED_ID, session)).isTrue();
        assertThat(DiurnalCurve.inDailySession(afterSession, FIXED_ID, session)).isFalse();
    }

    @Test
    void plannedSessionStartHourPlansExactlyMinCountSessionsWhenTheDaySeedOffsetIsZero() {
        ZonedDateTime day = ZonedDateTime.of(LocalDate.ofEpochDay(0), LocalTime.NOON, ZoneOffset.UTC);

        // FIXED_ID's hash is 0, and epoch day 0 makes the daySeed 0 too, so the planned count is
        // exactly minCount (2) with zero slot jitter on the very first slot.
        double first = DiurnalCurve.plannedSessionStartHour(day, FIXED_ID, 0, 6, 20, 2, 5);
        double second = DiurnalCurve.plannedSessionStartHour(day, FIXED_ID, 1, 6, 20, 2, 5);
        double third = DiurnalCurve.plannedSessionStartHour(day, FIXED_ID, 2, 6, 20, 2, 5);

        assertThat(first).isCloseTo(6.0, within(1e-9));
        assertThat(second).isGreaterThan(first).isLessThan(20.0);
        assertThat(third).isNaN(); // only 2 sessions planned this day, so index 2 doesn't exist
    }

    @Test
    void plannedSessionStartHourStaysWithinItsWindow() {
        UUID someApplianceId = new UUID(123L, 456L);
        for (int day = 0; day < 30; day++) {
            ZonedDateTime time = ZonedDateTime.of(LocalDate.ofEpochDay(day), LocalTime.NOON, ZoneOffset.UTC);
            for (int index = 0; index < 3; index++) {
                double start = DiurnalCurve.plannedSessionStartHour(time, someApplianceId, index, 7, 22, 0, 3);
                if (!Double.isNaN(start)) {
                    assertThat(start).isGreaterThanOrEqualTo(7.0).isLessThan(22.0);
                }
            }
        }
    }

    @Test
    void demandIntensityPeaksAtThePeakHourAndVanishesAtTheEdgeOfItsHalfWidth() {
        ZonedDateTime atPeak = ZonedDateTime.of(2026, 1, 1, 15, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime atEdge = ZonedDateTime.of(2026, 1, 1, 6, 0, 0, 0, ZoneOffset.UTC); // exactly 9h before the peak

        assertThat(DiurnalCurve.demandIntensity(atPeak, FIXED_ID, 9, 15.0)).isCloseTo(1.0, within(1e-9));
        assertThat(DiurnalCurve.demandIntensity(atEdge, FIXED_ID, 9, 15.0)).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void demandIntensityWithMultiplePeaksTakesTheHighestNearbyPeak() {
        ZonedDateTime morningPeak = ZonedDateTime.of(2026, 1, 1, 7, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime eveningPeak = ZonedDateTime.of(2026, 1, 1, 19, 30, 0, 0, ZoneOffset.UTC);
        ZonedDateTime midday = ZonedDateTime.of(2026, 1, 1, 13, 30, 0, 0, ZoneOffset.UTC);

        assertThat(DiurnalCurve.demandIntensity(morningPeak, FIXED_ID, 4, 7.0, 19.5)).isCloseTo(1.0, within(1e-9));
        assertThat(DiurnalCurve.demandIntensity(eveningPeak, FIXED_ID, 4, 7.0, 19.5)).isCloseTo(1.0, within(1e-9));
        assertThat(DiurnalCurve.demandIntensity(midday, FIXED_ID, 4, 7.0, 19.5)).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void smoothedNoiseStaysWithinUnitRangeAndVariesOverTime() {
        double atStart = DiurnalCurve.smoothedNoise01(Duration.ZERO, FIXED_ID);
        double atOneMinute = DiurnalCurve.smoothedNoise01(Duration.ofMinutes(1), FIXED_ID);
        double atFiveMinutes = DiurnalCurve.smoothedNoise01(Duration.ofMinutes(5), FIXED_ID);

        assertThat(atStart).isBetween(0.0, 1.0);
        assertThat(atOneMinute).isBetween(0.0, 1.0);
        assertThat(atFiveMinutes).isBetween(0.0, 1.0);
        assertThat(List.of(atStart, atOneMinute, atFiveMinutes)).doesNotHaveDuplicates();
    }

    @Test
    void smoothedNoiseIsDeterministicForTheSameInputs() {
        double first = DiurnalCurve.smoothedNoise01(Duration.ofSeconds(123), FIXED_ID);
        double second = DiurnalCurve.smoothedNoise01(Duration.ofSeconds(123), FIXED_ID);

        assertThat(first).isEqualTo(second);
    }
}
