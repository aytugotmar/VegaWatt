package com.vegawatt.core.home.domain;

import com.vegawatt.core.common.BusinessRuleViolationException;

public class InvalidHomeConfigurationException extends BusinessRuleViolationException {

    public InvalidHomeConfigurationException(String message) {
        super(message);
    }
}
