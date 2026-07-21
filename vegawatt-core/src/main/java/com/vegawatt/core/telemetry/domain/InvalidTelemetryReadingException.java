package com.vegawatt.core.telemetry.domain;

import com.vegawatt.core.common.BusinessRuleViolationException;

public class InvalidTelemetryReadingException extends BusinessRuleViolationException {

    public InvalidTelemetryReadingException(String message) {
        super(message);
    }
}
