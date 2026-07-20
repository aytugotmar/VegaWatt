package com.vegawatt.core.home.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vegawatt.core.common.outbox.OutboxEvent;
import com.vegawatt.core.common.outbox.OutboxRepository;
import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.home.domain.AssetRegistrationPublisher;
import com.vegawatt.core.home.domain.Home;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AssetRegistrationEventPublisher implements AssetRegistrationPublisher {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final ClockProvider clockProvider;

    public AssetRegistrationEventPublisher(OutboxRepository outboxRepository, ObjectMapper objectMapper,
                                            ClockProvider clockProvider) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.clockProvider = clockProvider;
    }

    @Override
    public void publish(Home home) {
        Instant occurredAt = clockProvider.now();
        AssetRegistrationEventPayload payload = new AssetRegistrationEventPayload(
                UUID.randomUUID(),
                1,
                occurredAt,
                new AssetRegistrationEventPayload.HomePayload(
                        home.id(), home.name(), home.contactEmail(), home.energyQuotaKwh(),
                        home.budgetQuotaTry(), home.baseTariffPerKwh(), home.penaltyTariffPerKwh()),
                home.appliances().stream()
                        .map(appliance -> new AssetRegistrationEventPayload.AppliancePayload(
                                appliance.id(), appliance.name(), appliance.type(),
                                appliance.safePowerLimitWatt(), appliance.simulationMinWatt(),
                                appliance.simulationMaxWatt()))
                        .toList());

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize asset registration event for home " + home.id(), e);
        }

        outboxRepository.save(OutboxEvent.create("HOME", home.id(), "ASSET_REGISTERED", json, occurredAt));
    }
}
