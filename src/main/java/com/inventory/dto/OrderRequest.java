package com.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record OrderRequest(
        @NotNull(message = "Customer is required") Long customerId,
        @NotEmpty(message = "Add at least one product") List<@Valid LineItemRequest> items,
        BigDecimal discountPercent,
        String paymentStatus) {
}
