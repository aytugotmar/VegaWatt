package com.vegawatt.core.telemetry.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vegawatt.core.home.domain.ApplianceOperatingState;
import com.vegawatt.core.telemetry.application.ProcessTelemetryUseCase;
import com.vegawatt.core.telemetry.domain.TelemetryReading;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TelemetryEventKafkaConsumerTest {

    @Mock
    private ProcessTelemetryUseCase processTelemetryUseCase;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private TelemetryEventKafkaConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new TelemetryEventKafkaConsumer(processTelemetryUseCase, objectMapper);
    }

    @Test
    void mapsOperatingStateAndModeFromThePayload() throws Exception {
        UUID homeId = UUID.randomUUID();
        UUID applianceId = UUID.randomUUID();
        TelemetryEventPayload payload = new TelemetryEventPayload(UUID.randomUUID(), 2, Instant.now(), homeId,
                applianceId, 15297L, new BigDecimal("120.50"), ApplianceOperatingState.STANDBY, "STANDBY", 5);

        consumer.onMessage(new ConsumerRecord<>("vegawatt.telemetry.v1", 0, 0L, homeId.toString(),
                objectMapper.writeValueAsString(payload)));

        ArgumentCaptor<TelemetryReading> captor = ArgumentCaptor.forClass(TelemetryReading.class);
        verify(processTelemetryUseCase).execute(captor.capture());
        assertThat(captor.getValue().operatingState()).isEqualTo(ApplianceOperatingState.STANDBY);
        assertThat(captor.getValue().operatingMode()).isEqualTo("STANDBY");
        assertThat(captor.getValue().sequenceNumber()).isEqualTo(15297L);
    }

    @Test
    void tolerantOfOlderPayloadsMissingOperatingStateAndMode() {
        UUID homeId = UUID.randomUUID();
        UUID applianceId = UUID.randomUUID();
        String legacyJson = String.format(
                "{\"eventId\":\"%s\",\"eventVersion\":1,\"occurredAt\":\"2026-01-01T00:00:00Z\",\"homeId\":\"%s\","
                        + "\"applianceId\":\"%s\",\"powerWatt\":100.0,\"measurementIntervalSeconds\":5}",
                UUID.randomUUID(), homeId, applianceId);

        consumer.onMessage(new ConsumerRecord<>("vegawatt.telemetry.v1", 0, 0L, homeId.toString(), legacyJson));

        ArgumentCaptor<TelemetryReading> captor = ArgumentCaptor.forClass(TelemetryReading.class);
        verify(processTelemetryUseCase).execute(captor.capture());
        assertThat(captor.getValue().operatingState()).isNull();
        assertThat(captor.getValue().operatingMode()).isNull();
        // A payload without the field deserializes to sequence 0, which the guard treats as unguarded.
        assertThat(captor.getValue().sequenceNumber()).isZero();
    }

    @Test
    void rejectsAnUnsupportedEventVersionInsteadOfProcessingItAsIfItWereKnown() throws Exception {
        UUID homeId = UUID.randomUUID();
        UUID applianceId = UUID.randomUUID();
        TelemetryEventPayload payload = new TelemetryEventPayload(UUID.randomUUID(), 99, Instant.now(), homeId,
                applianceId, 1L, new BigDecimal("120.50"), ApplianceOperatingState.STANDBY, "STANDBY", 5);

        assertThatThrownBy(() -> consumer.onMessage(new ConsumerRecord<>("vegawatt.telemetry.v1", 0, 0L,
                homeId.toString(), objectMapper.writeValueAsString(payload))))
                .isInstanceOf(UnsupportedTelemetryEventVersionException.class);

        // Thrown, not swallowed — KafkaErrorHandlingConfig routes this straight to the DLT rather
        // than silently processing a payload shaped like a version this consumer never validated.
        verifyNoInteractions(processTelemetryUseCase);
    }
}
