package com.vegawatt.sensors.registration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vegawatt.sensors.simulation.ApplianceSimulationScheduler;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.AbstractConsumerSeekAware;
import org.springframework.stereotype.Component;

@Component
class RegistrationEventConsumer extends AbstractConsumerSeekAware {

    private static final Logger log = LoggerFactory.getLogger(RegistrationEventConsumer.class);

    private final HomeRegistry homeRegistry;
    private final ApplianceSimulationScheduler simulationScheduler;
    private final ObjectMapper objectMapper;

    RegistrationEventConsumer(HomeRegistry homeRegistry, ApplianceSimulationScheduler simulationScheduler,
                               ObjectMapper objectMapper) {
        this.homeRegistry = homeRegistry;
        this.simulationScheduler = simulationScheduler;
        this.objectMapper = objectMapper;
    }

    // HomeRegistry is an in-memory projection of the registration log and nothing persists it.
    // If I resumed from the committed offset after a restart, every home registered before that
    // offset would be invisible, its appliances would never emit telemetry, and the container
    // would still report healthy: the demo would look alive while sending nothing. So on every
    // partition assignment I rewind to the start of the topic and rebuild the whole registry.
    // upsert and ensureScheduled are both idempotent, so replaying the log costs only a few reads.
    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        super.onPartitionsAssigned(assignments, callback);
        assignments.keySet().forEach(partition ->
                callback.seekToBeginning(partition.topic(), partition.partition()));
    }

    @KafkaListener(topics = "${vegawatt.kafka.registration-topic}", groupId = "vegawatt-sensors-registration")
    void onMessage(ConsumerRecord<String, String> record) {
        AssetRegistrationEventPayload payload;
        try {
            payload = objectMapper.readValue(record.value(), AssetRegistrationEventPayload.class);
        } catch (Exception e) {
            log.warn("Discarding malformed asset registration event from partition {} offset {}",
                    record.partition(), record.offset(), e);
            return;
        }

        for (AssetRegistrationEventPayload.AppliancePayload appliance : payload.appliances()) {
            ApplianceConfig config = new ApplianceConfig(appliance.applianceId(), payload.home().homeId(),
                    appliance.type(), appliance.safePowerLimitWatt(), appliance.simulationMinWatt(),
                    appliance.simulationMaxWatt());
            homeRegistry.upsert(config);
            simulationScheduler.ensureScheduled(config.applianceId());
        }

        log.info("Registered home {} with {} appliance(s)", payload.home().homeId(), payload.appliances().size());
    }
}
