package com.inventory.dto;

import java.time.LocalDateTime;

public record LogResponse(Long id, String username, String action, String description, LocalDateTime createdAt) {
}
