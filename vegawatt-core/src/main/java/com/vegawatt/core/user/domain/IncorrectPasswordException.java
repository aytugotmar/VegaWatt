package com.vegawatt.core.user.domain;

import com.vegawatt.core.common.BusinessRuleViolationException;

public class IncorrectPasswordException extends BusinessRuleViolationException {

    public IncorrectPasswordException() {
        super("Mevcut şifreniz hatalı.");
    }
}
