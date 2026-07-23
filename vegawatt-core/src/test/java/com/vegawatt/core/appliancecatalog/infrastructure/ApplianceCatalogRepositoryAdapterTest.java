package com.vegawatt.core.appliancecatalog.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplianceCatalogRepositoryAdapterTest {

    @Mock
    private ApplianceCatalogJpaRepository jpaRepository;

    private static ApplianceCatalogEntity coffeeMachineEntity() {
        Instant now = Instant.now();
        return new ApplianceCatalogEntity(UUID.randomUUID(), "COFFEE_MACHINE", "Kahve Makinesi",
                "description", "KITCHEN", "SHORT_HIGH_POWER", new BigDecimal("1500"), new BigDecimal("600"),
                new BigDecimal("1300"), BigDecimal.ZERO, new BigDecimal("2"), true, false, false,
                "coffee", "kahve, espresso", true, true, 60, 1, now, now);
    }

    @Test
    void findAllEnabledDelegatesToEnabledTrueQueryAndMapsToDomain() {
        ApplianceCatalogRepositoryAdapter adapter = new ApplianceCatalogRepositoryAdapter(jpaRepository);
        when(jpaRepository.findByEnabledTrue()).thenReturn(List.of(coffeeMachineEntity()));

        List<ApplianceCatalogItem> result = adapter.findAllEnabled();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code().value()).isEqualTo("COFFEE_MACHINE");
        verify(jpaRepository, never()).findAll();
    }

    @Test
    void findEnabledByCodeDelegatesToCodeAndEnabledTrueQuery() {
        ApplianceCatalogRepositoryAdapter adapter = new ApplianceCatalogRepositoryAdapter(jpaRepository);
        when(jpaRepository.findByCodeAndEnabledTrue("COFFEE_MACHINE")).thenReturn(Optional.of(coffeeMachineEntity()));

        Optional<ApplianceCatalogItem> result = adapter.findEnabledByCode("COFFEE_MACHINE");

        assertThat(result).isPresent();
        assertThat(result.get().displayName()).isEqualTo("Kahve Makinesi");
        verify(jpaRepository, never()).findById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void findEnabledByCodeReturnsEmptyWhenNotFound() {
        ApplianceCatalogRepositoryAdapter adapter = new ApplianceCatalogRepositoryAdapter(jpaRepository);
        when(jpaRepository.findByCodeAndEnabledTrue("UNKNOWN")).thenReturn(Optional.empty());

        assertThat(adapter.findEnabledByCode("UNKNOWN")).isEmpty();
    }

    @Test
    void findEnabledByIdDelegatesToIdAndEnabledTrueQuery() {
        ApplianceCatalogRepositoryAdapter adapter = new ApplianceCatalogRepositoryAdapter(jpaRepository);
        ApplianceCatalogEntity entity = coffeeMachineEntity();
        when(jpaRepository.findByIdAndEnabledTrue(entity.getId())).thenReturn(Optional.of(entity));

        Optional<ApplianceCatalogItem> result = adapter.findEnabledById(entity.getId());

        assertThat(result).isPresent();
        assertThat(result.get().code().value()).isEqualTo("COFFEE_MACHINE");
    }

    @Test
    void findEnabledByIdReturnsEmptyWhenNotFound() {
        ApplianceCatalogRepositoryAdapter adapter = new ApplianceCatalogRepositoryAdapter(jpaRepository);
        UUID id = UUID.randomUUID();
        when(jpaRepository.findByIdAndEnabledTrue(id)).thenReturn(Optional.empty());

        assertThat(adapter.findEnabledById(id)).isEmpty();
    }
}
