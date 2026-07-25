package com.vegawatt.sensors.publishing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vegawatt.sensors.config.SensorsKafkaProperties;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class TelemetryEventPublisherTest {

    private static final String TELEMETRY_TOPIC = "vegawatt.telemetry.v1";

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    // A real mapper, so the assertions run against the JSON the broker would actually receive rather
    // than against a mock's idea of it.
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private TelemetryEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new TelemetryEventPublisher(kafkaTemplate, objectMapper,
                new SensorsKafkaProperties("vegawatt.asset-registration.v1", TELEMETRY_TOPIC));
        when(kafkaTemplate.send(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture((SendResult<String, String>) null));
    }

    @Test
    void numbersAnAppliancesReadingsWithAStrictlyIncreasingSequence() {
        UUID homeId = UUID.randomUUID();
        UUID applianceId = UUID.randomUUID();

        publish(homeId, applianceId);
        publish(homeId, applianceId);
        publish(homeId, applianceId);

        List<Long> sequences = capturedSequenceNumbers(3);
        assertThat(sequences.get(1)).isEqualTo(sequences.get(0) + 1);
        assertThat(sequences.get(2)).isEqualTo(sequences.get(1) + 1);
    }

    @Test
    void keepsASeparateSequenceForEachAppliance() {
        UUID homeId = UUID.randomUUID();
        UUID applianceA = UUID.randomUUID();
        UUID applianceB = UUID.randomUUID();

        publish(homeId, applianceA);
        publish(homeId, applianceB);
        publish(homeId, applianceA);

        List<Long> sequences = capturedSequenceNumbers(3);
        long firstA = sequences.get(0);
        long firstB = sequences.get(1);
        long secondA = sequences.get(2);

        // B's reading must not advance A's counter: A's second number follows its first by exactly one.
        assertThat(secondA).isEqualTo(firstA + 1);
        // Each appliance starts from the same base, so their first numbers match. The point of the test
        // is that the two streams are independent, not that they share one counter.
        assertThat(firstB).isEqualTo(firstA);
    }

    @Test
    void startsWellAboveZeroSoARestartedSensorDoesNotRewindTheSequence() {
        publish(UUID.randomUUID(), UUID.randomUUID());

        long first = capturedSequenceNumbers(1).get(0);
        // Seeded from the process start epoch (millis), not zero, so even a freshly started process
        // emits a number a prior run is overwhelmingly unlikely to have reached. 1e12 is comfortably
        // below the current epoch-millis value and far above any plain per-tick counter.
        assertThat(first).isGreaterThan(1_000_000_000_000L);
    }

    private void publish(UUID homeId, UUID applianceId) {
        publisher.publish(homeId, applianceId, new BigDecimal("640.25"), "ON", "NORMAL", 5);
    }

    private List<Long> capturedSequenceNumbers(int expectedSends) {
        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate, times(expectedSends)).send(any(), anyString(), json.capture());
        List<Long> sequences = new ArrayList<>();
        for (String value : json.getAllValues()) {
            sequences.add(readSequenceNumber(value));
        }
        return sequences;
    }

    private long readSequenceNumber(String json) {
        try {
            return objectMapper.readTree(json).get("sequenceNumber").asLong();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("published telemetry was not valid JSON", e);
        }
    }
}
