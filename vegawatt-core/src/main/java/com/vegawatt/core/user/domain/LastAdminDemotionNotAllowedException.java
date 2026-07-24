package com.vegawatt.core.user.domain;

import com.vegawatt.core.common.BusinessRuleViolationException;

public class LastAdminDemotionNotAllowedException extends BusinessRuleViolationException {

    public LastAdminDemotionNotAllowedException() {
        super("Sistemdeki son admin başka bir role düşürülemez.");
    }
}
