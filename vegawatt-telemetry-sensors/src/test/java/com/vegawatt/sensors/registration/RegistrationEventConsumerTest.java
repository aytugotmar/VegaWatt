package com.vegawatt.sensors.registration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vegawatt.sensors.simulation.ApplianceSimulationScheduler;
import java.util.Map;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.listener.ConsumerSeekAware.ConsumerSeekCallback;

class RegistrationEventConsumerTest {

    private static final String TOPIC = "vegawatt.asset-registration.v1";

    @Test
    void rewindsEveryAssignedPartitionToTheBeginningOnAssignment() {
        RegistrationEventConsumer consumer = new RegistrationEventConsumer(
                mock(HomeRegistry.class), mock(ApplianceSimulationScheduler.class), new ObjectMapper());
        ConsumerSeekCallback callback = mock(ConsumerSeekCallback.class);
        TopicPartition partitionZero = new TopicPartition(TOPIC, 0);
        TopicPartition partitionOne = new TopicPartition(TOPIC, 1);

        // The committed offsets (42, 7) are the trap. A registry with no persistence behind it must
        // rebuild from the whole log after a restart, not resume from where a prior run left off, so
        // the consumer ignores them and rewinds to the start of every partition it is handed.
        consumer.onPartitionsAssigned(Map.of(partitionZero, 42L, partitionOne, 7L), callback);

        verify(callback).seekToBeginning(TOPIC, 0);
        verify(callback).seekToBeginning(TOPIC, 1);
    }
}
