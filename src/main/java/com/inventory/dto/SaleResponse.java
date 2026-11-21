package com.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SaleResponse(Long id, String invoiceNumber, Long customerId, String customerName, LocalDateTime saleDate,
                           List<LineResponse> items, BigDecimal subtotal, BigDecimal discountPercent,
                           BigDecimal discountAmount, BigDecimal total, BigDecimal profit, String paymentMethod,
                           String status, String createdBy) {
}
