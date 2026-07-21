package com.vegawatt.sensors.registration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vegawatt.sensors.simulation.ApplianceSimulationScheduler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class RegistrationEventConsumer {

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
