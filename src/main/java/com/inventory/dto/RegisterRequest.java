package com.inventory.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Name is required") @Size(max = 100, message = "Name is too long") String name,
        @NotBlank(message = "Email is required") @Email(message = "Enter a valid email address") @Size(max = 150) String email,
        @NotBlank(message = "Mobile number is required") @Pattern(regexp = "^[0-9+\\- ]{7,15}$", message = "Enter a valid mobile number") String mobile,
        @NotBlank(message = "Password is required") @Size(min = 6, max = 60, message = "Password must be 6 to 60 characters") String password,
        @NotBlank(message = "Confirm password is required") String confirmPassword,
        @Size(max = 255, message = "Address is too long") String address) {
}
