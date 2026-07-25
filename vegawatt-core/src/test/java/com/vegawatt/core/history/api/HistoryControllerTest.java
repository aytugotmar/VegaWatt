package com.vegawatt.core.history.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.vegawatt.core.access.domain.HomeAuthorizationService;
import com.vegawatt.core.common.config.HistoryProperties;
import com.vegawatt.core.common.security.CurrentUser;
import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.history.application.GetHomeConsumptionHistoryQuery;
import com.vegawatt.core.history.domain.InvalidHistoryRangeException;
import com.vegawatt.core.user.domain.UserRole;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HistoryControllerTest {

    private static final Instant NOW = Instant.parse("2026-07-25T12:00:00Z");
    private static final UUID HOME_ID = UUID.randomUUID();
    private static final CurrentUser CURRENT_USER = new CurrentUser(UUID.randomUUID(), UserRole.USER);
    private static final int MAX_RANGE_DAYS = 90;

    @Mock
    private GetHomeConsumptionHistoryQuery getHomeConsumptionHistoryQuery;
    @Mock
    private ClockProvider clockProvider;
    @Mock
    private HomeAuthorizationService homeAuthorizationService;

    private HistoryController controller() {
        return new HistoryController(getHomeConsumptionHistoryQuery, clockProvider, homeAuthorizationService,
                new HistoryProperties(MAX_RANGE_DAYS));
    }

    @Test
    void withinTheMaxRangeDelegatesToTheQuery() {
        Instant from = NOW.minus(30, ChronoUnit.DAYS);
        when(getHomeConsumptionHistoryQuery.execute(eq(HOME_ID), eq(from), eq(NOW), any()))
                .thenReturn(List.of());

        List<ConsumptionHistoryPointResponse> result =
                controller().history(HOME_ID, from, NOW, null, CURRENT_USER);

        assertThat(result).isEmpty();
    }

    @Test
    void rejectsARangeExceedingTheConfiguredMaximum() {
        Instant tooFarBack = NOW.minus(MAX_RANGE_DAYS + 1, ChronoUnit.DAYS);

        assertThatThrownBy(() -> controller().history(HOME_ID, tooFarBack, NOW, null, CURRENT_USER))
                .isInstanceOf(InvalidHistoryRangeException.class);
    }

    @Test
    void rejectsFromAfterTo() {
        Instant to = NOW.minus(1, ChronoUnit.DAYS);
        Instant from = NOW;

        assertThatThrownBy(() -> controller().history(HOME_ID, from, to, null, CURRENT_USER))
                .isInstanceOf(InvalidHistoryRangeException.class);
    }

    @Test
    void defaultRangeWhenNeitherFromNorToIsGivenIsWithinTheLimit() {
        when(clockProvider.now()).thenReturn(NOW);
        when(getHomeConsumptionHistoryQuery.execute(eq(HOME_ID), any(), eq(NOW), any()))
                .thenReturn(List.of());

        List<ConsumptionHistoryPointResponse> result = controller().history(HOME_ID, null, null, null, CURRENT_USER);

        assertThat(result).isEmpty();
    }

    @Test
    void rejectsARangeThatExceedsTheMaximumByLessThanADay() {
        // Duration.toDays() truncates, so 90 days + 23 hours would read as "90 days" under a
        // naive toDays() > maxRangeDays check and slip past the limit. Comparing Durations
        // directly must not have that blind spot.
        Instant justOverTheLimit = NOW.minus(MAX_RANGE_DAYS, ChronoUnit.DAYS).minus(23, ChronoUnit.HOURS);

        assertThatThrownBy(() -> controller().history(HOME_ID, justOverTheLimit, NOW, null, CURRENT_USER))
                .isInstanceOf(InvalidHistoryRangeException.class);
    }

    @Test
    void aRangeExactlyAtTheMaximumIsAllowed() {
        Instant from = NOW.minus(MAX_RANGE_DAYS, ChronoUnit.DAYS);
        when(getHomeConsumptionHistoryQuery.execute(eq(HOME_ID), eq(from), eq(NOW), any()))
                .thenReturn(List.of());

        List<ConsumptionHistoryPointResponse> result =
                controller().history(HOME_ID, from, NOW, null, CURRENT_USER);

        assertThat(result).isEmpty();
    }
}
