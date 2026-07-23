package com.vegawatt.core.appliancecatalog.domain;

import com.vegawatt.core.common.ResourceNotFoundException;

public class ApplianceCatalogItemNotFoundException extends ResourceNotFoundException {

    public ApplianceCatalogItemNotFoundException(String code) {
        super("Appliance catalog item not found: " + code);
    }
}
