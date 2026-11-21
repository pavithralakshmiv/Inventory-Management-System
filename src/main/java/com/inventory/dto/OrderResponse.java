package com.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(Long id, String orderNumber, Long customerId, String customerName, LocalDateTime orderDate,
                            List<LineResponse> items, Integer totalQuantity, BigDecimal subtotal,
                            BigDecimal discountPercent, BigDecimal discountAmount, BigDecimal total,
                            String paymentStatus, String status, String createdBy) {
}
