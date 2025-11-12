package com.mk.contractservice.web.dto.client;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@JsonTypeName("PERSON")
@Schema(description = "Create person client request")
public record CreatePersonRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 200, message = "Name must not exceed 200 characters")
        @Schema(description = "Person name", example = "Alice Martin", maxLength = 200, requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @NotBlank(message = "Email is required")
        @jakarta.validation.constraints.Email(message = "Email must be a valid email address")
        @Size(max = 254, message = "Email must not exceed 254 characters")
        @Schema(description = "Person email address", example = "alice.martin@example.com", format = "email", maxLength = 254, requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "\\+?[0-9 .()/-]{7,20}", message = "Phone number must be valid (7-20 characters, allowed: digits, +, -, ., (), /, spaces)")
        @Schema(description = "Person phone number", example = "+41791234567", pattern = "\\+?[0-9 .()/-]{7,20}", minLength = 7, maxLength = 20, requiredMode = Schema.RequiredMode.REQUIRED)
        String phone,

        @NotNull(message = "Birth date is required")
        @PastOrPresent(message = "Birth date must be in the past or present")
        @Schema(description = "Person birth date (must be in the past or present)", example = "1990-05-15", format = "date", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate birthDate
) implements CreateClientRequest {
}

