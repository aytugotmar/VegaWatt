package com.vegawatt.core.appliancecatalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vegawatt.core.appliancecatalog.domain.ApplianceBehaviorProfile;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogCode;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogItem;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogRepository;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCategory;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListApplianceCatalogQueryTest {

    @Mock
    private ApplianceCatalogRepository repository;

    private ListApplianceCatalogQuery query;

    private static ApplianceCatalogItem item(String code, String displayName, ApplianceCategory category,
                                              boolean featured, String searchKeywords, int displayOrder) {
        return new ApplianceCatalogItem(UUID.randomUUID(), new ApplianceCatalogCode(code), displayName,
                "description", category, ApplianceBehaviorProfile.MANUAL_SWITCH, new BigDecimal("100"),
                new BigDecimal("10"), new BigDecimal("90"), null, null, false, false, false,
                "icon", searchKeywords, featured, true, displayOrder);
    }

    @Test
    void returnsAllItemsSortedByDisplayOrderWhenNoFilterApplied() {
        query = new ListApplianceCatalogQuery(repository);
        ApplianceCatalogItem coffee = item("COFFEE_MACHINE", "Kahve Makinesi", ApplianceCategory.KITCHEN, true,
                "kahve, espresso", 20);
        ApplianceCatalogItem fridge = item("REFRIGERATOR", "Buzdolabı", ApplianceCategory.KITCHEN, true,
                "buzdolabı", 10);
        when(repository.findAllEnabled()).thenReturn(List.of(coffee, fridge));

        List<ApplianceCatalogItem> result = query.execute(new ApplianceCatalogFilter(null, null, null));

        assertThat(result).extracting(i -> i.code().value()).containsExactly("REFRIGERATOR", "COFFEE_MACHINE");
    }

    @Test
    void filtersByCategory() {
        query = new ListApplianceCatalogQuery(repository);
        ApplianceCatalogItem fridge = item("REFRIGERATOR", "Buzdolabı", ApplianceCategory.KITCHEN, false, null, 10);
        ApplianceCatalogItem router = item("ROUTER", "Router", ApplianceCategory.NETWORK_AND_SMART_HOME, false,
                null, 20);
        when(repository.findAllEnabled()).thenReturn(List.of(fridge, router));

        List<ApplianceCatalogItem> result = query.execute(
                new ApplianceCatalogFilter(ApplianceCategory.KITCHEN, null, null));

        assertThat(result).extracting(i -> i.code().value()).containsExactly("REFRIGERATOR");
    }

    @Test
    void filtersByFeatured() {
        query = new ListApplianceCatalogQuery(repository);
        ApplianceCatalogItem featured = item("REFRIGERATOR", "Buzdolabı", ApplianceCategory.KITCHEN, true, null, 10);
        ApplianceCatalogItem notFeatured = item("FREEZER", "Derin Dondurucu", ApplianceCategory.KITCHEN, false,
                null, 20);
        when(repository.findAllEnabled()).thenReturn(List.of(featured, notFeatured));

        List<ApplianceCatalogItem> result = query.execute(new ApplianceCatalogFilter(null, null, true));

        assertThat(result).extracting(i -> i.code().value()).containsExactly("REFRIGERATOR");
    }

    @Test
    void searchMatchesDisplayNameCaseInsensitively() {
        query = new ListApplianceCatalogQuery(repository);
        ApplianceCatalogItem coffee = item("COFFEE_MACHINE", "Kahve Makinesi", ApplianceCategory.KITCHEN, false,
                null, 10);
        when(repository.findAllEnabled()).thenReturn(List.of(coffee));

        List<ApplianceCatalogItem> result = query.execute(new ApplianceCatalogFilter(null, "kahve", null));

        assertThat(result).extracting(i -> i.code().value()).containsExactly("COFFEE_MACHINE");
    }

    @Test
    void searchMatchesSearchKeywords() {
        query = new ListApplianceCatalogQuery(repository);
        ApplianceCatalogItem router = item("ROUTER", "Router", ApplianceCategory.NETWORK_AND_SMART_HOME, false,
                "router, modem, kablosuz, wifi", 10);
        when(repository.findAllEnabled()).thenReturn(List.of(router));

        List<ApplianceCatalogItem> result = query.execute(new ApplianceCatalogFilter(null, "wifi", null));

        assertThat(result).extracting(i -> i.code().value()).containsExactly("ROUTER");
    }

    @Test
    void searchIsTurkishCaseInsensitiveForDottedAndDotlessI() {
        query = new ListApplianceCatalogQuery(repository);
        ApplianceCatalogItem heater = item("FAN_HEATER", "Fanlı Isıtıcı", ApplianceCategory.CLIMATE, false,
                "fanlı ısıtıcı", 10);
        when(repository.findAllEnabled()).thenReturn(List.of(heater));

        List<ApplianceCatalogItem> result = query.execute(new ApplianceCatalogFilter(null, "ISITICI", null));

        assertThat(result).extracting(i -> i.code().value()).containsExactly("FAN_HEATER");
    }

    @Test
    void returnsEmptyListWhenSearchMatchesNothing() {
        query = new ListApplianceCatalogQuery(repository);
        ApplianceCatalogItem fridge = item("REFRIGERATOR", "Buzdolabı", ApplianceCategory.KITCHEN, false, null, 10);
        when(repository.findAllEnabled()).thenReturn(List.of(fridge));

        List<ApplianceCatalogItem> result = query.execute(new ApplianceCatalogFilter(null, "yok-boyle-bir-sey", null));

        assertThat(result).isEmpty();
    }
}
