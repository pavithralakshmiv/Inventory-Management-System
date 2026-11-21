package com.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PurchaseItemRequest(
        @NotNull(message = "Product is required") Long productId,
        @NotNull(message = "Quantity is required") @Min(value = 1, message = "Quantity must be at least 1") Integer quantity,
        @NotNull(message = "Purchase price is required") @DecimalMin(value = "0.0", message = "Purchase price cannot be negative") BigDecimal price) {
}
