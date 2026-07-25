package com.vegawatt.core.history.domain;

import com.vegawatt.core.common.BusinessRuleViolationException;

public class InvalidHistoryRangeException extends BusinessRuleViolationException {

    public InvalidHistoryRangeException(String message) {
        super(message);
    }
}
