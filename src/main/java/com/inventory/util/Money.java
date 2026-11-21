package com.inventory.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {

    private Money() {
    }

    public static BigDecimal round(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal percentOf(BigDecimal amount, BigDecimal percent) {
        return round(amount.multiply(percent).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
    }

    public static BigDecimal multiply(BigDecimal price, int quantity) {
        return round(price.multiply(BigDecimal.valueOf(quantity)));
    }
}
