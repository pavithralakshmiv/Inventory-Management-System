package com.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileRequest(
        @NotBlank(message = "Name is required") @Size(max = 100) String name,
        @Pattern(regexp = "^$|^[0-9+\\- ]{7,15}$", message = "Enter a valid mobile number") String mobile,
        @Size(max = 255) String address) {
}
