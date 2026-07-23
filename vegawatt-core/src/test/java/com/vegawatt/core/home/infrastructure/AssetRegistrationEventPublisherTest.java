package com.vegawatt.core.home.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vegawatt.core.appliancecatalog.domain.ApplianceBehaviorProfile;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogCode;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogItem;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCategory;
import com.vegawatt.core.common.outbox.OutboxEvent;
import com.vegawatt.core.common.outbox.OutboxRepository;
import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.home.domain.Appliance;
import com.vegawatt.core.home.domain.Home;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssetRegistrationEventPublisherTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private ClockProvider clockProvider;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private static ApplianceCatalogItem coffeeMachineCatalogItem() {
        return new ApplianceCatalogItem(UUID.randomUUID(), new ApplianceCatalogCode("COFFEE_MACHINE"),
                "Kahve Makinesi", "description", ApplianceCategory.KITCHEN, ApplianceBehaviorProfile.SHORT_HIGH_POWER,
                new BigDecimal("1500"), new BigDecimal("600"), new BigDecimal("1300"), new BigDecimal("0"),
                new BigDecimal("2"), true, false, false, "coffee", null, true, true, 60);
    }

    private JsonNode publishAndCapture(Home home) throws Exception {
        when(clockProvider.now()).thenReturn(Instant.parse("2026-07-22T10:00:00Z"));
        when(outboxRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssetRegistrationEventPublisher publisher = new AssetRegistrationEventPublisher(outboxRepository,
                objectMapper, clockProvider);
        publisher.publish(home);

        var captor = org.mockito.ArgumentCaptor.forClass(OutboxEvent.class);
        org.mockito.Mockito.verify(outboxRepository).save(captor.capture());
        return objectMapper.readTree(captor.getValue().payload());
    }

    @Test
    void publishesEventVersionTwo() throws Exception {
        Home home = Home.register("Test Ev", "test@example.com", new BigDecimal("500"), new BigDecimal("2500"),
                new BigDecimal("2.10"), new BigDecimal("3.50"), Instant.parse("2026-07-22T10:00:00Z"));
        home.addAppliance(Appliance.create(home.id(), "Buzdolabı", "REFRIGERATOR", new BigDecimal("220"),
                new BigDecimal("80"), new BigDecimal("180")));

        JsonNode json = publishAndCapture(home);

        assertThat(json.get("eventVersion").asInt()).isEqualTo(2);
    }

    @Test
    void includesCatalogSnapshotFieldsForCatalogLinkedAppliance() throws Exception {
        Home home = Home.register("Test Ev", "test@example.com", new BigDecimal("500"), new BigDecimal("2500"),
                new BigDecimal("2.10"), new BigDecimal("3.50"), Instant.parse("2026-07-22T10:00:00Z"));
        home.addAppliance(Appliance.createFromCatalog(home.id(), "Mutfak Kahve Makinesi",
                coffeeMachineCatalogItem(), new BigDecimal("1450"), new BigDecimal("650"), new BigDecimal("1350")));

        JsonNode appliance = publishAndCapture(home).get("appliances").get(0);

        assertThat(appliance.get("catalogCode").asText()).isEqualTo("COFFEE_MACHINE");
        assertThat(appliance.get("behaviorProfile").asText()).isEqualTo("SHORT_HIGH_POWER");
        assertThat(appliance.get("standbyMinWatt").decimalValue()).isEqualByComparingTo("0");
        assertThat(appliance.get("standbyMaxWatt").decimalValue()).isEqualByComparingTo("2");
    }

    @Test
    void leavesCatalogSnapshotFieldsNullForCustomAppliance() throws Exception {
        Home home = Home.register("Test Ev", "test@example.com", new BigDecimal("500"), new BigDecimal("2500"),
                new BigDecimal("2.10"), new BigDecimal("3.50"), Instant.parse("2026-07-22T10:00:00Z"));
        home.addAppliance(Appliance.create(home.id(), "Buzdolabı", "REFRIGERATOR", new BigDecimal("220"),
                new BigDecimal("80"), new BigDecimal("180")));

        JsonNode appliance = publishAndCapture(home).get("appliances").get(0);

        assertThat(appliance.get("catalogCode").isNull()).isTrue();
        assertThat(appliance.get("behaviorProfile").isNull()).isTrue();
        assertThat(appliance.get("standbyMinWatt").isNull()).isTrue();
        assertThat(appliance.get("standbyMaxWatt").isNull()).isTrue();
    }
}
