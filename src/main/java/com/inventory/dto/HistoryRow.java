package com.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HistoryRow(String type, String number, LocalDateTime date, BigDecimal total, String status) {
}
