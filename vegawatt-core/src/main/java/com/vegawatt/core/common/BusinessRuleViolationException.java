package com.vegawatt.core.common;

public abstract class BusinessRuleViolationException extends RuntimeException {

    protected BusinessRuleViolationException(String message) {
        super(message);
    }
}
