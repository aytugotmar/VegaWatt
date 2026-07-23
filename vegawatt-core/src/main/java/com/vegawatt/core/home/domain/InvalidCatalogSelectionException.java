package com.vegawatt.core.home.domain;

import com.vegawatt.core.common.BusinessRuleViolationException;

public class InvalidCatalogSelectionException extends BusinessRuleViolationException {

    public InvalidCatalogSelectionException(String message) {
        super(message);
    }
}
