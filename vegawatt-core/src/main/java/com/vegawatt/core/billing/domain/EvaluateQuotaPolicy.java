package com.vegawatt.core.billing.domain;

import java.math.BigDecimal;

public final class EvaluateQuotaPolicy {

    private static final BigDecimal EIGHTY_PERCENT = new BigDecimal("80");
    private static final BigDecimal HUNDRED_PERCENT = new BigDecimal("100");

    private EvaluateQuotaPolicy() {
    }

    public static QuotaTransition evaluate(BigDecimal previousPercentage, BigDecimal newPercentage) {
        boolean crossed80 = previousPercentage.compareTo(EIGHTY_PERCENT) < 0
                && newPercentage.compareTo(EIGHTY_PERCENT) >= 0;
        boolean crossed100 = previousPercentage.compareTo(HUNDRED_PERCENT) < 0
                && newPercentage.compareTo(HUNDRED_PERCENT) >= 0;
        return new QuotaTransition(crossed80, crossed100);
    }
}
