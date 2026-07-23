package com.vegawatt.core.appliancecatalog.api;

import com.vegawatt.core.appliancecatalog.application.ApplianceCatalogFilter;
import com.vegawatt.core.appliancecatalog.application.GetApplianceCatalogItemQuery;
import com.vegawatt.core.appliancecatalog.application.ListApplianceCatalogQuery;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCategory;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/appliance-catalog")
class ApplianceCatalogController {

    private final ListApplianceCatalogQuery listApplianceCatalogQuery;
    private final GetApplianceCatalogItemQuery getApplianceCatalogItemQuery;

    ApplianceCatalogController(ListApplianceCatalogQuery listApplianceCatalogQuery,
                                GetApplianceCatalogItemQuery getApplianceCatalogItemQuery) {
        this.listApplianceCatalogQuery = listApplianceCatalogQuery;
        this.getApplianceCatalogItemQuery = getApplianceCatalogItemQuery;
    }

    @GetMapping
    List<ApplianceCatalogItemResponse> list(
            @RequestParam(required = false) ApplianceCategory category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean featured) {
        return listApplianceCatalogQuery.execute(new ApplianceCatalogFilter(category, search, featured)).stream()
                .map(ApplianceCatalogItemResponse::from)
                .toList();
    }

    @GetMapping("/{code}")
    ApplianceCatalogItemResponse detail(@PathVariable String code) {
        return ApplianceCatalogItemResponse.from(getApplianceCatalogItemQuery.execute(code));
    }
}
