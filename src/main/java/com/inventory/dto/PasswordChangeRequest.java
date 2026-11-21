package com.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
        @NotBlank(message = "Old password is required") String oldPassword,
        @NotBlank(message = "New password is required") @Size(min = 6, max = 60, message = "Password must be 6 to 60 characters") String newPassword,
        @NotBlank(message = "Confirm password is required") String confirmPassword) {
}
