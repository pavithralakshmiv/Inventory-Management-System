package com.inventory.dto;

import java.math.BigDecimal;

public record LineResponse(Long productId, String productCode, String productName, Integer quantity,
                           BigDecimal unitPrice, BigDecimal lineTotal) {
}
