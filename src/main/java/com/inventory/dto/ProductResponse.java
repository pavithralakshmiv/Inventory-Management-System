package com.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductResponse(Long id, String productCode, String name, Long categoryId, String categoryName,
                              Long supplierId, String supplierName, String imagePath, BigDecimal purchasePrice,
                              BigDecimal sellingPrice, Integer quantity, String brand, LocalDate productDate,
                              String description, String status, String stockStatus) {
}
