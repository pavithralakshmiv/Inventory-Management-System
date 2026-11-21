package com.inventory.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank(message = "Name is required") @Size(max = 100) String name,
        @Pattern(regexp = "^$|^[0-9+\\- ]{7,15}$", message = "Enter a valid mobile number") String mobile,
        @NotBlank(message = "Email is required") @Email(message = "Enter a valid email address") @Size(max = 150) String email,
        @Size(max = 60, message = "Password is too long") String password,
        @Size(max = 255) String address,
        @NotBlank(message = "Status is required") String status,
        @NotBlank(message = "Role is required") String role) {
}
