package com.vegawatt.core.appliancecatalog.domain;

public record ApplianceCatalogCode(String value) {

    public ApplianceCatalogCode {
        if (value == null || !value.matches("[A-Z0-9_]{2,64}")) {
            throw new IllegalArgumentException("Invalid appliance catalog code");
        }
    }
}
