package com.inventory.dto;

import java.time.LocalDateTime;

public record StockMovementResponse(Long id, String productName, String movementType, Integer quantityChange,
                                    Integer balanceAfter, String referenceType, Long referenceId, String note,
                                    String createdBy, LocalDateTime createdAt) {
}
