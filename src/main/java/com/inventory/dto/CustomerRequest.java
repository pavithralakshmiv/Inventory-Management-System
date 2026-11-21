package com.inventory.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank(message = "Customer name is required") @Size(max = 100) String name,
        @NotBlank(message = "Mobile number is required") @Pattern(regexp = "^[0-9+\\- ]{7,15}$", message = "Enter a valid mobile number") String mobile,
        @Email(message = "Enter a valid email address") @Size(max = 150) String email,
        @Size(max = 255) String address,
        @NotBlank(message = "Status is required") String status) {
}
