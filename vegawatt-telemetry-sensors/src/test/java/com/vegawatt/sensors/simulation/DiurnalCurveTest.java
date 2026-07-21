package com.vegawatt.sensors.simulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

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
}
