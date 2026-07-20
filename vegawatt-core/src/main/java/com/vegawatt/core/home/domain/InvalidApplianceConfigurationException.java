package com.vegawatt.core.home.domain;

import com.vegawatt.core.common.BusinessRuleViolationException;

public class InvalidApplianceConfigurationException extends BusinessRuleViolationException {

    public InvalidApplianceConfigurationException(String message) {
        super(message);
    }
}
