package com.vegawatt.core.appliancecatalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vegawatt.core.appliancecatalog.domain.ApplianceBehaviorProfile;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogCode;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogItem;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogItemNotFoundException;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogRepository;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCategory;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetApplianceCatalogItemQueryTest {

    @Mock
    private ApplianceCatalogRepository repository;

    @Test
    void returnsItemForKnownCode() {
        GetApplianceCatalogItemQuery query = new GetApplianceCatalogItemQuery(repository);
        ApplianceCatalogItem coffee = new ApplianceCatalogItem(UUID.randomUUID(),
                new ApplianceCatalogCode("COFFEE_MACHINE"), "Kahve Makinesi", "description", ApplianceCategory.KITCHEN,
                ApplianceBehaviorProfile.SHORT_HIGH_POWER, new BigDecimal("1500"), new BigDecimal("600"),
                new BigDecimal("1300"), null, null, false, false, false, "coffee", null, true, true, 60);
        when(repository.findEnabledByCode("COFFEE_MACHINE")).thenReturn(Optional.of(coffee));

        ApplianceCatalogItem result = query.execute("COFFEE_MACHINE");

        assertThat(result.code().value()).isEqualTo("COFFEE_MACHINE");
    }

    @Test
    void throwsNotFoundForUnknownCode() {
        GetApplianceCatalogItemQuery query = new GetApplianceCatalogItemQuery(repository);
        when(repository.findEnabledByCode("NOT_A_REAL_CODE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> query.execute("NOT_A_REAL_CODE"))
                .isInstanceOf(ApplianceCatalogItemNotFoundException.class);
    }
}
