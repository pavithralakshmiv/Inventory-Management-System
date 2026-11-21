package com.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.util.List;

public record SaleRequest(
        Long customerId,
        @NotEmpty(message = "Add at least one product") List<@Valid LineItemRequest> items,
        BigDecimal discountPercent,
        @NotBlank(message = "Payment method is required") String paymentMethod) {
}
