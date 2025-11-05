package com.mk.contractservice.web.dto.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreatePersonRequest(
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

        @NotNull(message = "Birth date is required")
        @PastOrPresent(message = "Birth date must be in the past or present")
        LocalDate birthDate
) {
}

