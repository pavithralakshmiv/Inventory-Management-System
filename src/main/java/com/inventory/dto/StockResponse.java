package com.inventory.dto;

import java.math.BigDecimal;

public record StockResponse(Long productId, String productCode, String productName, String categoryName,
                            Integer quantity, BigDecimal purchasePrice, BigDecimal sellingPrice, String stockStatus) {
}
