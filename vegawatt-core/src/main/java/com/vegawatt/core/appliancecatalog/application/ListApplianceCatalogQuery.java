package com.vegawatt.core.appliancecatalog.application;

import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogItem;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class ListApplianceCatalogQuery {

    private static final Locale TURKISH = Locale.forLanguageTag("tr");

    private final ApplianceCatalogRepository repository;

    ListApplianceCatalogQuery(ApplianceCatalogRepository repository) {
        this.repository = repository;
    }

    public List<ApplianceCatalogItem> execute(ApplianceCatalogFilter filter) {
        return repository.findAllEnabled().stream()
                .filter(item -> filter.category() == null || item.category() == filter.category())
                .filter(item -> filter.featured() == null || item.featured() == filter.featured())
                .filter(item -> matchesSearch(item, filter.search()))
                .sorted(Comparator.comparingInt(ApplianceCatalogItem::displayOrder))
                .toList();
    }

    private static boolean matchesSearch(ApplianceCatalogItem item, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String needle = search.trim().toLowerCase(TURKISH);
        boolean matchesName = item.displayName().toLowerCase(TURKISH).contains(needle);
        boolean matchesKeywords = item.searchKeywords() != null
                && item.searchKeywords().toLowerCase(TURKISH).contains(needle);
        return matchesName || matchesKeywords;
    }
}
