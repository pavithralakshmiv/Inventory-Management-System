package com.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PurchaseRequest(
        @NotNull(message = "Supplier is required") Long supplierId,
        @NotEmpty(message = "Add at least one product") List<@Valid PurchaseItemRequest> items) {
}
