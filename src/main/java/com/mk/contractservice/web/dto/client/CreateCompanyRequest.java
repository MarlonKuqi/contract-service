package com.mk.contractservice.web.dto.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCompanyRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 200, message = "Name must not exceed 200 characters")
        String name,

        @NotBlank(message = "Email is required")
        @jakarta.validation.constraints.Email(message = "Email must be a valid email address")
        @Size(max = 254, message = "Email must not exceed 254 characters")
        String email,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "\\+?[0-9 .()/-]{7,20}", message = "Phone number must be valid (7-20 characters, allowed: digits, +, -, ., (), /, spaces)")
        String phone,

        @NotBlank(message = "Company identifier is required")
        @Size(max = 64, message = "Company identifier must not exceed 64 characters")
        String companyIdentifier
) {
}
