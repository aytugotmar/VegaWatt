package com.vegawatt.core.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Money(BigDecimal amount) {

    private static final int SCALE = 6;
    private static final int DISPLAY_SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    public Money {
        amount = amount.setScale(SCALE, ROUNDING);
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO);
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    public Money plus(Money other) {
        return new Money(amount.add(other.amount));
    }

    public boolean isGreaterThanOrEqualTo(Money other) {
        return amount.compareTo(other.amount) >= 0;
    }

    public BigDecimal rounded() {
        return amount.setScale(DISPLAY_SCALE, ROUNDING);
    }
}
