package com.vegawatt.sensors.registration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vegawatt.sensors.simulation.ApplianceSimulationScheduler;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.listener.ConsumerSeekAware.ConsumerSeekCallback;

@ExtendWith(MockitoExtension.class)
class RegistrationEventConsumerTest {

    @Mock
    private ApplianceSimulationScheduler simulationScheduler;

    private final HomeRegistry homeRegistry = new HomeRegistry();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private RegistrationEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new RegistrationEventConsumer(homeRegistry, simulationScheduler, objectMapper);
    }

    private static AssetRegistrationEventPayload.HomePayload homePayload(UUID homeId) {
        return new AssetRegistrationEventPayload.HomePayload(homeId, "Test Ev", "test@example.com",
                new BigDecimal("500"), new BigDecimal("2500"), new BigDecimal("2.10"), new BigDecimal("3.50"));
    }

    private ConsumerRecord<String, String> record(AssetRegistrationEventPayload payload) throws Exception {
        return new ConsumerRecord<>("vegawatt.asset-registration.v1", 0, 0L, payload.home().homeId().toString(),
                objectMapper.writeValueAsString(payload));
    }

    @Test
    void processesVersionTwoPayloadWithCatalogFields() throws Exception {
        UUID homeId = UUID.randomUUID();
        UUID applianceId = UUID.randomUUID();
        AssetRegistrationEventPayload.AppliancePayload appliance = new AssetRegistrationEventPayload.AppliancePayload(
                applianceId, "Mutfak Kahve Makinesi", "COFFEE_MACHINE", new BigDecimal("1500"),
                new BigDecimal("600"), new BigDecimal("1300"), "COFFEE_MACHINE", "SHORT_HIGH_POWER",
                new BigDecimal("0"), new BigDecimal("2"));
        AssetRegistrationEventPayload payload = new AssetRegistrationEventPayload(UUID.randomUUID(), 2,
                Instant.now(), homePayload(homeId), List.of(appliance));

        consumer.onMessage(record(payload));

        ApplianceConfig config = homeRegistry.find(applianceId).orElseThrow();
        assertThat(config.catalogCode()).isEqualTo("COFFEE_MACHINE");
        assertThat(config.behaviorProfile()).isEqualTo("SHORT_HIGH_POWER");
        assertThat(config.standbyMinWatt()).isEqualByComparingTo("0");
        assertThat(config.standbyMaxWatt()).isEqualByComparingTo("2");
        verify(simulationScheduler).ensureScheduled(applianceId);
    }

    @Test
    void processesVersionOnePayloadWithoutCatalogFields() throws Exception {
        UUID homeId = UUID.randomUUID();
        UUID applianceId = UUID.randomUUID();
        AssetRegistrationEventPayload.AppliancePayload appliance = new AssetRegistrationEventPayload.AppliancePayload(
                applianceId, "Buzdolabı", "REFRIGERATOR", new BigDecimal("220"), new BigDecimal("80"),
                new BigDecimal("180"), null, null, null, null);
        AssetRegistrationEventPayload payload = new AssetRegistrationEventPayload(UUID.randomUUID(), 1,
                Instant.now(), homePayload(homeId), List.of(appliance));

        consumer.onMessage(record(payload));

        ApplianceConfig config = homeRegistry.find(applianceId).orElseThrow();
        assertThat(config.type()).isEqualTo("REFRIGERATOR");
        assertThat(config.catalogCode()).isNull();
        verify(simulationScheduler).ensureScheduled(applianceId);
    }

    @Test
    void discardsUnsupportedEventVersion() throws Exception {
        UUID homeId = UUID.randomUUID();
        UUID applianceId = UUID.randomUUID();
        AssetRegistrationEventPayload.AppliancePayload appliance = new AssetRegistrationEventPayload.AppliancePayload(
                applianceId, "Buzdolabı", "REFRIGERATOR", new BigDecimal("220"), new BigDecimal("80"),
                new BigDecimal("180"), null, null, null, null);
        AssetRegistrationEventPayload payload = new AssetRegistrationEventPayload(UUID.randomUUID(), 99,
                Instant.now(), homePayload(homeId), List.of(appliance));

        consumer.onMessage(record(payload));

        assertThat(homeRegistry.find(applianceId)).isEmpty();
        verify(simulationScheduler, never()).ensureScheduled(any());
    }

    @Test
    void discardsMalformedJson() {
        ConsumerRecord<String, String> malformed = new ConsumerRecord<>("vegawatt.asset-registration.v1", 0, 0L,
                "key", "{not-valid-json");

        consumer.onMessage(malformed);

        assertThat(homeRegistry.registeredApplianceIds()).isEmpty();
        verify(simulationScheduler, never()).ensureScheduled(any());
    }

    @Test
    void rewindsEveryAssignedPartitionToTheBeginningOnAssignment() {
        RegistrationEventConsumer restartConsumer = new RegistrationEventConsumer(
                mock(HomeRegistry.class), mock(ApplianceSimulationScheduler.class), new ObjectMapper());
        ConsumerSeekCallback callback = mock(ConsumerSeekCallback.class);
        String topic = "vegawatt.asset-registration.v1";
        TopicPartition partitionZero = new TopicPartition(topic, 0);
        TopicPartition partitionOne = new TopicPartition(topic, 1);

        // The committed offsets (42, 7) are the trap. A registry with no persistence behind it must
        // rebuild from the whole log after a restart, not resume from where a prior run left off, so
        // the consumer ignores them and rewinds to the start of every partition it is handed.
        restartConsumer.onPartitionsAssigned(Map.of(partitionZero, 42L, partitionOne, 7L), callback);

        verify(callback).seekToBeginning(topic, 0);
        verify(callback).seekToBeginning(topic, 1);
    }
}
