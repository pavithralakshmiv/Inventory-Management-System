package com.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseResponse(Long id, String purchaseNumber, Long supplierId, String supplierName,
                               LocalDateTime purchaseDate, List<LineResponse> items, BigDecimal total,
                               String status, String createdBy) {
}
