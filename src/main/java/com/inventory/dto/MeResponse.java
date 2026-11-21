package com.inventory.dto;

public record MeResponse(Long id, String name, String email, String mobile, String address, String role,
                         String businessName, String currency) {
}
