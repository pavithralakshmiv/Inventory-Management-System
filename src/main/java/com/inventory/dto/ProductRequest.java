package com.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductRequest(
        @NotBlank(message = "Product code is required") @Size(max = 50) String productCode,
        @NotBlank(message = "Product name is required") @Size(max = 150) String name,
        @NotNull(message = "Category is required") Long categoryId,
        Long supplierId,
        @Size(max = 255) String imagePath,
        @NotNull(message = "Purchase price is required") @DecimalMin(value = "0.0", message = "Purchase price cannot be negative") BigDecimal purchasePrice,
        @NotNull(message = "Selling price is required") @DecimalMin(value = "0.0", message = "Selling price cannot be negative") BigDecimal sellingPrice,
        @NotNull(message = "Quantity is required") @Min(value = 0, message = "Quantity cannot be negative") Integer quantity,
        @Size(max = 100) String brand,
        LocalDate productDate,
        @Size(max = 500) String description,
        String status) {
}
