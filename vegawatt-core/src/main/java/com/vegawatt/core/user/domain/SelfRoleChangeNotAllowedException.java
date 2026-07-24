package com.vegawatt.core.user.domain;

import com.vegawatt.core.common.BusinessRuleViolationException;

public class SelfRoleChangeNotAllowedException extends BusinessRuleViolationException {

    public SelfRoleChangeNotAllowedException() {
        super("Kendi rolünüzü değiştiremezsiniz.");
    }
}
