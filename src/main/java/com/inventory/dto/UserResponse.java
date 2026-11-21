package com.inventory.dto;

public record UserResponse(Long id, String name, String mobile, String email, String address, String status, String role) {
}
