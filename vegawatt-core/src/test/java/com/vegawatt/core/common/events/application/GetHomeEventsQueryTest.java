package com.vegawatt.core.common.events.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vegawatt.core.common.events.OperationalEvent;
import com.vegawatt.core.common.events.OperationalEventRepository;
import com.vegawatt.core.common.events.OperationalEventType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetHomeEventsQueryTest {

    @Mock
    private OperationalEventRepository operationalEventRepository;

    @Test
    void returnsEventsForHome() {
        GetHomeEventsQuery query = new GetHomeEventsQuery(operationalEventRepository);
        UUID homeId = UUID.randomUUID();
        OperationalEvent event = OperationalEvent.create(homeId, UUID.randomUUID(),
                OperationalEventType.APPLIANCE_ANOMALY_DETECTED, Instant.now(), "power exceeded safe limit");
        when(operationalEventRepository.findByHomeId(homeId)).thenReturn(List.of(event));

        List<OperationalEvent> result = query.execute(homeId);

        assertThat(result).containsExactly(event);
    }

    @Test
    void returnsEmptyListWhenHomeHasNoEvents() {
        GetHomeEventsQuery query = new GetHomeEventsQuery(operationalEventRepository);
        UUID homeId = UUID.randomUUID();
        when(operationalEventRepository.findByHomeId(homeId)).thenReturn(List.of());

        List<OperationalEvent> result = query.execute(homeId);

        assertThat(result).isEmpty();
    }
}
